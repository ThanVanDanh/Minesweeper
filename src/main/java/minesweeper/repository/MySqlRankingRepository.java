package minesweeper.repository;

import minesweeper.dto.RankingDTO;
import minesweeper.repository.connection.ConnectionFactory;
import minesweeper.repository.connection.ConnectionFactoryProvider;
import minesweeper.repository.connection.HikariConnectionFactory;
import minesweeper.repository.config.MySqlConnectionConfig;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.repository.pagination.Page;
import minesweeper.repository.pagination.PagedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MySqlRankingRepository implements RankingRepository {
    private static final Logger LOG = LoggerFactory.getLogger(MySqlRankingRepository.class);

    private static final String SELECT_LEVELS_SQL = """
            SELECT id, level_name
            FROM game_levels
            ORDER BY id ASC
            """;

    private static final String SELECT_LEADERBOARD_BY_LEVEL_SQL = """
            SELECT
                player_name,
                total_games,
                wins,
                best_score,
                best_time_ms,
                DENSE_RANK() OVER (
                    ORDER BY best_score DESC,
                             COALESCE(best_time_ms, 999999999) ASC,
                             wins DESC,
                             player_name ASC
                ) AS rank
            FROM (
                SELECT
                    u.username AS player_name,
                    COUNT(gs.id) AS total_games,
                    SUM(CASE WHEN UPPER(gs.result) = 'WIN' THEN 1 ELSE 0 END) AS wins,
                    MAX(gs.score) AS best_score,
                    MIN(CASE WHEN UPPER(gs.result) = 'WIN' THEN gs.completion_time END) AS best_time_ms
                FROM game_sessions gs
                JOIN users u ON u.id = gs.user_id
                WHERE gs.level_id = ?
                GROUP BY u.id, u.username
            ) AS aggregated
            ORDER BY rank ASC, player_name ASC
            """;


    private static final String COUNT_LEADERBOARD_SQL = """
            SELECT COUNT(DISTINCT u.id) as total
            FROM game_sessions gs
            JOIN users u ON u.id = gs.user_id
            WHERE gs.level_id = ?
            """;

    private final ConnectionFactory connectionFactory;

    public MySqlRankingRepository() {
        this(ConnectionFactoryProvider.get());
    }

    public MySqlRankingRepository(MySqlConnectionConfig config) {
        Objects.requireNonNull(config, "config cannot be null");
        this.connectionFactory = new HikariConnectionFactory(config);
    }

    public MySqlRankingRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory cannot be null");
    }

    @Override
    public List<RankingDTO> getLeaderboardByLevel(int levelId) throws Exception {
        return getLeaderboardByLevelPaginated(levelId, new Page(0, 1000000, 0)).getContent();
    }

    public PagedResult<RankingDTO> getLeaderboardByLevelPaginated(int levelId, Page page) throws DataAccessException {
        Objects.requireNonNull(page, "page cannot be null");
        
        try (Connection connection = connectionFactory.getConnection()) {
            // Get total count
            long totalCount = getLeaderboardCount(connection, levelId);
            Page pageInfo = new Page(page.getPageNumber(), page.getPageSize(), totalCount);

            List<RankingDTO> rankings = new ArrayList<>();
            String sql = SELECT_LEADERBOARD_BY_LEVEL_SQL + pageInfo.getLimitClause();

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, levelId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        // P8 fix: rank is now computed by DENSE_RANK() in SQL
                        int sqlRank = rs.getInt("rank");
                        rankings.add(new RankingDTO(
                                sqlRank,
                                rs.getString("player_name"),
                                rs.getInt("total_games"),
                                rs.getInt("wins"),
                                rs.getInt("best_score"),
                                rs.getLong("best_time_ms")
                        ));
                    }
                }
            }

            LOG.info("Fetched {} rankings for level {}, page {}/{}", 
                    rankings.size(), levelId, page.getPageNumber() + 1, pageInfo.getTotalPages());
            return new PagedResult<>(rankings, pageInfo);
        } catch (Exception e) {
            LOG.error("Error fetching leaderboard for level {}", levelId, e);
            throw new DataAccessException("Failed to fetch leaderboard", e);
        }
    }

    private long getLeaderboardCount(Connection connection, int levelId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(COUNT_LEADERBOARD_SQL)) {
            statement.setInt(1, levelId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total");
                }
            }
        }
        return 0;
    }

    @Override
    public List<LevelInfo> getLevels() throws DataAccessException {
        try (Connection connection = connectionFactory.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(SELECT_LEVELS_SQL)) {

            List<LevelInfo> levels = new ArrayList<>();
            while (rs.next()) {
                levels.add(new LevelInfo(rs.getInt("id"), rs.getString("level_name")));
            }
            LOG.info("Fetched {} game levels", levels.size());
            return levels;
        } catch (Exception e) {
            LOG.error("Error fetching game levels", e);
            throw new DataAccessException("Failed to fetch levels", e);
        }
    }

    public void close() {
        connectionFactory.close();
        LOG.info("MySqlRankingRepository closed");
    }
}

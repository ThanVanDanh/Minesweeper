package minesweeper.persistence;

import minesweeper.dto.RankingDTO;
import minesweeper.repository.config.MySqlConnectionConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MySqlRankingDAO implements RankingDAO {
    private static final String SELECT_LEVELS_SQL = """
            SELECT id, level_name
            FROM game_levels
            ORDER BY id ASC
            """;

    private static final String SELECT_LEADERBOARD_BY_LEVEL_SQL = """
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
            ORDER BY best_score DESC,
                     COALESCE(best_time_ms, 999999999) ASC,
                     wins DESC,
                     u.username ASC
            """;

    private final MySqlConnectionConfig config;

    public MySqlRankingDAO() {
        this(MySqlConnectionConfig.fromResources());
    }

    public MySqlRankingDAO(MySqlConnectionConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public List<RankingDTO> getLeaderboardByLevel(int levelId) throws Exception {
        List<RankingDTO> rankings = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LEADERBOARD_BY_LEVEL_SQL)) {
            statement.setInt(1, levelId);
            try (ResultSet rs = statement.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    rankings.add(new RankingDTO(
                            rank++,
                            rs.getString("player_name"),
                            rs.getInt("total_games"),
                            rs.getInt("wins"),
                            rs.getInt("best_score"),
                            rs.getLong("best_time_ms")
                    ));
                }
            }
        }
        return rankings;
    }

    @Override
    public List<LevelInfo> getLevels() throws Exception {
        List<LevelInfo> levels = new ArrayList<>();
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(SELECT_LEVELS_SQL)) {
            while (rs.next()) {
                levels.add(new LevelInfo(rs.getInt("id"), rs.getString("level_name")));
            }
        }
        return levels;
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword());
    }
}


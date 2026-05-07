package minesweeper.repository;

import minesweeper.model.Difficulty;
import minesweeper.model.GameResult;
import minesweeper.repository.config.MySqlConnectionConfig;
import minesweeper.repository.connection.ConnectionFactory;
import minesweeper.repository.connection.HikariConnectionFactory;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.repository.pagination.Page;
import minesweeper.repository.pagination.PagedResult;
import minesweeper.service.LevelService;
import minesweeper.service.MySqlLevelService;
import minesweeper.service.MySqlUserService;
import minesweeper.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * MySQL implementation of GameResultRepository with connection pooling
 */
public class MySqlGameResultRepository implements GameResultRepository {
    private static final Logger LOG = LoggerFactory.getLogger(MySqlGameResultRepository.class);

    // SQL Queries
    private static final String INSERT_SESSION_SQL = """
            INSERT INTO game_sessions (user_id, level_id, result, completion_time, score, opened_cells, flagged_cells, started_at, ended_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String COUNT_ALL_SQL = "SELECT COUNT(*) AS total FROM game_sessions";
    
    private static final String SELECT_ALL_SQL = """
            SELECT gs.id AS session_id, u.username AS player_name, COALESCE(gl.level_name, '') AS level_name,
                   gs.result, gs.completion_time, gs.flagged_cells, gs.opened_cells, gs.ended_at, gs.created_at, gs.score
            FROM game_sessions gs
            JOIN users u ON gs.user_id = u.id
            LEFT JOIN game_levels gl ON gs.level_id = gl.id
            ORDER BY gs.created_at DESC, gs.id DESC
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_BY_PLAYER_SQL = """
            SELECT COUNT(*) AS total FROM game_sessions gs
            JOIN users u ON gs.user_id = u.id
            WHERE LOWER(u.username) = LOWER(?)
            """;
    
    private static final String SELECT_BY_PLAYER_SQL = """
            SELECT gs.id AS session_id, u.username AS player_name, COALESCE(gl.level_name, '') AS level_name,
                   gs.result, gs.completion_time, gs.flagged_cells, gs.opened_cells, gs.ended_at, gs.created_at, gs.score
            FROM game_sessions gs
            JOIN users u ON gs.user_id = u.id
            LEFT JOIN game_levels gl ON gs.level_id = gl.id
            WHERE LOWER(u.username) = LOWER(?)
            ORDER BY gs.created_at DESC, gs.id DESC
            LIMIT ? OFFSET ?
            """;

    private static final String DELETE_ALL_SQL = "DELETE FROM game_sessions";

    private static final String DELETE_BY_IDS_SQL = "DELETE FROM game_sessions WHERE id = ?";

    private final ConnectionFactory connectionFactory;
    private final UserService userService;
    private final LevelService levelService;

    public MySqlGameResultRepository() {
        MySqlConnectionConfig config = MySqlConnectionConfig.fromResources();
        this.connectionFactory = new HikariConnectionFactory(config);
        this.userService = new MySqlUserService(connectionFactory);
        this.levelService = new MySqlLevelService(connectionFactory);
    }

    public MySqlGameResultRepository(MySqlConnectionConfig config) {
        Objects.requireNonNull(config, "config");
        this.connectionFactory = new HikariConnectionFactory(config);
        this.userService = new MySqlUserService(connectionFactory);
        this.levelService = new MySqlLevelService(connectionFactory);
    }

    public MySqlGameResultRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
        this.userService = new MySqlUserService(connectionFactory);
        this.levelService = new MySqlLevelService(connectionFactory);
    }

    public MySqlGameResultRepository(ConnectionFactory connectionFactory, UserService userService, LevelService levelService) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
        this.userService = Objects.requireNonNull(userService, "userService");
        this.levelService = Objects.requireNonNull(levelService, "levelService");
    }

    @Override
    public void saveGameResult(GameResult result) throws DataAccessException {
        validateResult(result);
        
        try (Connection connection = connectionFactory.getConnection()) {
            // Use manual transaction for consistency
            connection.setAutoCommit(false);
            try {
                long userId = userService.getOrCreateUser(result.getPlayerName());
                Integer levelId = levelService.getLevelIdByDifficulty(result.getDifficulty());
                insertGameSession(connection, userId, levelId, result);
                
                connection.commit();
                LOG.info("Game result saved for player: {}", result.getPlayerName());
            } catch (SQLException e) {
                connection.rollback();
                LOG.error("Error saving game result, transaction rolled back", e);
                throw new DataAccessException("Failed to save game result", e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOG.error("Database error while saving game result", e);
            throw new DataAccessException("Database connection error", e);
        }
    }

    /**
     * Inserts game session into database
     */
    private void insertGameSession(Connection connection, long userId, Integer levelId, GameResult result) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_SESSION_SQL)) {
            ps.setLong(1, userId);
            
            if (levelId == null) {
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, levelId);
            }
            
            ps.setString(3, result.isWon() ? "WIN" : "LOSE");
            ps.setLong(4, result.getElapsedTimeMs());
            ps.setInt(5, result.getScore());
            ps.setInt(6, 0);  // opened_cells unknown
            ps.setInt(7, result.getFlagsUsed());
            ps.setTimestamp(8, java.sql.Timestamp.valueOf(result.getPlayedAt()));
            ps.setTimestamp(9, java.sql.Timestamp.valueOf(result.getPlayedAt()));
            
            ps.executeUpdate();
        }
    }

    @Override
    public List<GameResult> getPlayerHistory(String playerName) throws DataAccessException {
        if (playerName == null || playerName.isBlank()) {
            return new ArrayList<>();
        }
        
        try {
            PagedResult<GameResult> result = getPlayerHistory(playerName, 0, 1000);
            return result.getContent();
        } catch (DataAccessException e) {
            LOG.error("Error fetching player history for: {}", playerName, e);
            throw e;
        }
    }
    
    /**
     * Get player history with pagination
     */
    public PagedResult<GameResult> getPlayerHistory(String playerName, int pageNumber, int pageSize) throws DataAccessException {
        if (playerName == null || playerName.isBlank()) {
            Page emptyPage = new Page(pageNumber, pageSize, 0);
            return new PagedResult<>(new ArrayList<>(), emptyPage);
        }
        
        String trimmedName = playerName.trim();
        
        try {
            // Get total count
            long total = getPlayerGameCount(trimmedName);
            
            // Get paged results
            List<GameResult> results = new ArrayList<>();
            try (Connection connection = connectionFactory.getConnection();
                 PreparedStatement statement = connection.prepareStatement(SELECT_BY_PLAYER_SQL)) {
                statement.setString(1, trimmedName);
                statement.setInt(2, pageSize);
                statement.setInt(3, (int) ((long) pageNumber * pageSize));
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        results.add(mapSessionResult(resultSet));
                    }
                }
            }
            
            Page page = new Page(pageNumber, pageSize, total);
            LOG.info("Fetched {} of {} results for player: {} (page {}/{})", 
                    results.size(), total, playerName, pageNumber + 1, page.getTotalPages());
            return new PagedResult<>(results, page);
        } catch (SQLException e) {
            LOG.error("Error fetching player history for: {}", playerName, e);
            throw new DataAccessException("Failed to fetch player history", e);
        }
    }
    
    private long getPlayerGameCount(String playerName) throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement ps = connection.prepareStatement(COUNT_BY_PLAYER_SQL)) {
            ps.setString(1, playerName);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total");
                }
            }
        }
        return 0;
    }

    @Override
    public List<GameResult> getAllResults() throws DataAccessException {
        try {
            PagedResult<GameResult> result = getAllResults(0, 1000);
            return result.getContent();
        } catch (DataAccessException e) {
            LOG.error("Error fetching all results", e);
            throw e;
        }
    }
    
    /**
     * Get all results with pagination
     */
    public PagedResult<GameResult> getAllResults(int pageNumber, int pageSize) throws DataAccessException {
        try {
            // Get total count
            long total = getTotalGameCount();
            
            // Get paged results
            List<GameResult> results = new ArrayList<>();
            try (Connection connection = connectionFactory.getConnection();
                 PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL)) {
                statement.setInt(1, pageSize);
                statement.setInt(2, (int) ((long) pageNumber * pageSize));
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        results.add(mapSessionResult(resultSet));
                    }
                }
            }
            
            Page page = new Page(pageNumber, pageSize, total);
            LOG.info("Fetched {} of {} total results (page {}/{})", 
                    results.size(), total, pageNumber + 1, page.getTotalPages());
            return new PagedResult<>(results, page);
        } catch (SQLException e) {
            LOG.error("Error fetching all results", e);
            throw new DataAccessException("Failed to fetch all results", e);
        }
    }
    
    private long getTotalGameCount() throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(COUNT_ALL_SQL)) {
            if (rs.next()) {
                return rs.getLong("total");
            }
        }
        return 0;
    }

    @Override
    public void clearAllResults() throws DataAccessException {
        try (Connection connection = connectionFactory.getConnection();
             Statement statement = connection.createStatement()) {
            int deleted = statement.executeUpdate(DELETE_ALL_SQL);
            LOG.warn("Cleared {} game sessions", deleted);
        } catch (SQLException e) {
            LOG.error("Error clearing results", e);
            throw new DataAccessException("Failed to clear results", e);
        }
    }

    /**
     * Maps ResultSet to GameResult object
     */
    private GameResult mapSessionResult(ResultSet rs) throws SQLException {
        long sessionId = rs.getLong("session_id");
        String gameId = "session-" + sessionId;
        String playerName = rs.getString("player_name");
        String levelName = rs.getString("level_name");
        int openedCells = rs.getInt("opened_cells");
        int score = rs.getInt("score");

        Difficulty difficulty = null;
        if (levelName != null && !levelName.isBlank()) {
            try {
                difficulty = Difficulty.valueOf(levelName.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Difficulty not found, leave as null
            }
        }
        
        boolean isWon = "WIN".equalsIgnoreCase(rs.getString("result"));
        long elapsed = rs.getLong("completion_time");
        int flagsUsed = rs.getInt("flagged_cells");
        int minesTotal = 0;
        
        java.sql.Timestamp ended = rs.getTimestamp("ended_at");
        java.sql.Timestamp created = rs.getTimestamp("created_at");
        
        LocalDateTime playedAt;
        if (ended != null) {
            playedAt = ended.toLocalDateTime();
        } else if (created != null) {
            playedAt = created.toLocalDateTime();
        } else {
            playedAt = LocalDateTime.now();
        }

        GameResult gr = new GameResult(gameId, playerName, difficulty, isWon, elapsed, flagsUsed, minesTotal, playedAt);
        gr.setOpenedCells(openedCells);
        gr.setScore(score);
        return gr;
    }

    /**
     * Validates game result before persistence
     */
    private void validateResult(GameResult result) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(result.getPlayerName(), "result.playerName");
        Objects.requireNonNull(result.getPlayedAt(), "result.playedAt");
    }

    public void close() {
        connectionFactory.close();
        LOG.info("MySqlGameResultRepository closed");
    }

    @Override
    public void deleteByGameIds(List<String> gameIds) throws DataAccessException {
        if (gameIds == null || gameIds.isEmpty()) return;

        List<Long> sessionIds = gameIds.stream()
                .filter(id -> id != null && id.startsWith("session-"))
                .map(id -> {
                    try {
                        return Long.parseLong(id.substring("session-".length()));
                    } catch (NumberFormatException e) {
                        LOG.warn("gameId không hợp lệ, bỏ qua: {}", id);
                        return -1L;
                    }
                })
                .filter(id -> id > 0)
                .collect(java.util.stream.Collectors.toList());

        if (sessionIds.isEmpty()) return;

        try (Connection connection = connectionFactory.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(DELETE_BY_IDS_SQL)) {
                for (long sessionId : sessionIds) {
                    ps.setLong(1, sessionId);
                    ps.addBatch();
                    LOG.warn("[FRAUD-DELETE] Xoá game_session id={}", sessionId);
                }
                ps.executeBatch();
                connection.commit();
                LOG.warn("Đã xoá {} kết quả gian lận khỏi DB.", sessionIds.size());
            } catch (SQLException e) {
                connection.rollback();
                throw new DataAccessException("Lỗi khi xoá kết quả gian lận", e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi kết nối DB khi xoá gian lận", e);
        }}
}

package minesweeper.repository;

import minesweeper.repository.connection.ConnectionFactory;
import minesweeper.repository.connection.ConnectionFactoryProvider;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.service.LevelService;
import minesweeper.service.MySqlLevelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Repository lưu kết quả ván chơi theo đúng 3 bước có transaction:
 * <ol>
 *   <li>INSERT game_sessions → lấy generated sessionId</li>
 *   <li>Upsert player_best_scores (chỉ khi WIN)</li>
 *   <li>INSERT ranking_snapshots (chỉ khi có kỷ lục mới)</li>
 * </ol>
 *
 * <p>Toàn bộ 3 bước nằm trong một Transaction. Nếu bất kỳ bước nào lỗi thì rollback toàn bộ.
 */
public class GameSessionRepository {

    private static final Logger LOG = LoggerFactory.getLogger(GameSessionRepository.class);

    // ── SQL constants ─────────────────────────────────────────────────────────

    /** Bước 1: Lưu ván chơi */
    private static final String SQL_INSERT_SESSION = """
            INSERT INTO game_sessions
                (user_id, level_id, result, completion_time, score,
                 opened_cells, flagged_cells, started_at, first_click_at, ended_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    /** Bước 2a: Kiểm tra đã có bản ghi best score chưa */
    private static final String SQL_SELECT_BEST = """
            SELECT best_time, best_score
            FROM player_best_scores
            WHERE user_id = ? AND level_id = ?
            """;

    /** Bước 2b: Chưa có → INSERT mới */
    private static final String SQL_INSERT_BEST = """
            INSERT INTO player_best_scores
                (user_id, level_id, best_time, best_score, game_session_id, achieved_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

    /** Bước 2c: Đã có nhưng record mới tốt hơn → UPDATE */
    private static final String SQL_UPDATE_BEST = """
            UPDATE player_best_scores
            SET best_time       = ?,
                best_score      = ?,
                game_session_id = ?,
                achieved_at     = CURRENT_TIMESTAMP
            WHERE user_id = ? AND level_id = ?
            """;

    /**
     * Bước 3a: Tính rank của user trong level (best_time ASC, best_score DESC, achieved_at ASC).
     * Đếm số người có best_time nhỏ hơn, hoặc bằng nhưng best_score lớn hơn,
     * hoặc bằng cả hai nhưng achieved_at sớm hơn → rank = COUNT + 1.
     */
    private static final String SQL_CALC_RANK = """
            SELECT COUNT(*) + 1 AS rank_position
            FROM player_best_scores
            WHERE level_id = ?
              AND (
                   best_time < ?
                OR (best_time = ? AND best_score > ?)
                OR (best_time = ? AND best_score = ? AND achieved_at < CURRENT_TIMESTAMP)
              )
            """;

    /** Bước 3b: Lưu snapshot thứ hạng */
    private static final String SQL_INSERT_SNAPSHOT = """
            INSERT INTO ranking_snapshots
                (user_id, level_id, rank_position, best_score, best_time_ms, snapshot_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

    // ── Fields ────────────────────────────────────────────────────────────────

    private final ConnectionFactory connectionFactory;
    private final LevelService levelService;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Dùng shared connection pool mặc định (HikariCP). */
    public GameSessionRepository() {
        ConnectionFactory shared = ConnectionFactoryProvider.get();
        this.connectionFactory = shared;
        this.levelService      = new MySqlLevelService(shared);
    }

    /** Inject ConnectionFactory tùy ý (hữu ích cho testing). */
    public GameSessionRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
        this.levelService      = new MySqlLevelService(connectionFactory);
    }

    /** Inject đầy đủ (dùng cho unit test / DI). */
    public GameSessionRepository(ConnectionFactory connectionFactory, LevelService levelService) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
        this.levelService      = Objects.requireNonNull(levelService, "levelService");
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Lưu kết quả ván chơi theo 3 bước trong một Transaction.
     *
     * @param data dữ liệu ván chơi vừa kết thúc
     * @return {@link GameSessionResult} chứa sessionId, isNewRecord, newRank
     * @throws DataAccessException nếu có lỗi DB (transaction đã được rollback)
     */
    public GameSessionResult saveGameResult(GameSessionData data) throws DataAccessException {
        Objects.requireNonNull(data, "GameSessionData không được null");

        // Tra level_id từ Difficulty (có thể throw DataAccessException)
        Integer levelId = levelService.getLevelIdByDifficulty(data.getDifficulty());
        if (levelId == null) {
            throw new DataAccessException(
                    "Không tìm thấy level_id cho difficulty: " + data.getDifficulty(), null);
        }

        try (Connection conn = connectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // ── Bước 1 ────────────────────────────────────────────────
                long sessionId = insertGameSession(conn, data, levelId);
                LOG.info("[Step 1] game_sessions INSERT OK → sessionId={}", sessionId);

                boolean newRecord = false;
                Integer newRank   = null;

                if (data.isWon()) {
                    // ── Bước 2 ────────────────────────────────────────────
                    newRecord = upsertBestScore(conn, data, levelId, sessionId);
                    LOG.info("[Step 2] player_best_scores upsert → newRecord={}", newRecord);

                    // ── Bước 3 ────────────────────────────────────────────
                    if (newRecord) {
                        newRank = insertRankingSnapshot(conn, data, levelId);
                        LOG.info("[Step 3] ranking_snapshots INSERT OK → newRank={}", newRank);
                    }
                }

                conn.commit();
                LOG.info("Transaction committed. sessionId={}, newRecord={}, newRank={}",
                         sessionId, newRecord, newRank);
                return new GameSessionResult(sessionId, newRecord, newRank);

            } catch (SQLException e) {
                safeRollback(conn);
                LOG.error("Transaction rolled back do lỗi SQL", e);
                throw new DataAccessException("Lưu kết quả ván chơi thất bại, đã rollback", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOG.error("Không lấy được connection từ pool", e);
            throw new DataAccessException("Lỗi kết nối cơ sở dữ liệu", e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Bước 1: INSERT game_sessions, trả về generated key.
     */
    private long insertGameSession(Connection conn, GameSessionData data, int levelId)
            throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(
                SQL_INSERT_SESSION, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, data.getUserId());
            ps.setInt(2, levelId);
            ps.setString(3, data.isWon() ? "WIN" : "LOSE");
            ps.setLong(4, data.getCompletionTimeMs());
            ps.setInt(5, data.getScore());
            ps.setInt(6, data.getOpenedCells());
            ps.setInt(7, data.getFlaggedCells());
            setTimestampOrNull(ps, 8, data.getStartedAt());
            setTimestampOrNull(ps, 9, data.getFirstClickAt());
            setTimestampOrNull(ps, 10, data.getEndedAt());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("INSERT game_sessions không tạo được bản ghi nào");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new SQLException("Không lấy được generated key sau INSERT game_sessions");
        }
    }

    /**
     * Bước 2: Kiểm tra player_best_scores và upsert nếu cần.
     *
     * @return true nếu đã INSERT hoặc UPDATE (tức là có kỷ lục mới/cải thiện)
     */
    private boolean upsertBestScore(Connection conn, GameSessionData data,
                                    int levelId, long sessionId) throws SQLException {
        long newTime  = data.getCompletionTimeMs();
        int  newScore = data.getScore();
        long userId   = data.getUserId();

        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BEST)) {
            ps.setLong(1, userId);
            ps.setInt(2, levelId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // Chưa có bản ghi → INSERT mới
                    insertBestScore(conn, userId, levelId, newTime, newScore, sessionId);
                    return true;
                }

                long currentBestTime  = rs.getLong("best_time");
                int  currentBestScore = rs.getInt("best_score");

                // Cải thiện khi: thời gian mới ngắn hơn
                // (Theo spec: completion_time ASC → nhỏ hơn là tốt hơn)
                if (newTime < currentBestTime) {
                    updateBestScore(conn, userId, levelId, newTime, newScore, sessionId);
                    return true;
                }

                // Không cải thiện → không làm gì
                LOG.debug("Không có kỷ lục mới (newTime={} >= currentBest={})", newTime, currentBestTime);
                return false;
            }
        }
    }

    /** INSERT bản ghi player_best_scores mới. */
    private void insertBestScore(Connection conn, long userId, int levelId,
                                 long bestTime, int bestScore, long sessionId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_BEST)) {
            ps.setLong(1, userId);
            ps.setInt(2, levelId);
            ps.setLong(3, bestTime);
            ps.setInt(4, bestScore);
            ps.setLong(5, sessionId);
            ps.executeUpdate();
            LOG.debug("[Step 2] INSERT player_best_scores: userId={}, levelId={}, bestTime={}", userId, levelId, bestTime);
        }
    }

    /** UPDATE player_best_scores khi có kỷ lục tốt hơn. */
    private void updateBestScore(Connection conn, long userId, int levelId,
                                 long bestTime, int bestScore, long sessionId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_BEST)) {
            ps.setLong(1, bestTime);
            ps.setInt(2, bestScore);
            ps.setLong(3, sessionId);
            ps.setLong(4, userId);
            ps.setInt(5, levelId);
            ps.executeUpdate();
            LOG.debug("[Step 2] UPDATE player_best_scores: userId={}, levelId={}, newBestTime={}", userId, levelId, bestTime);
        }
    }

    /**
     * Bước 3: Tính rank và INSERT ranking_snapshots.
     *
     * @return vị trí rank (1-based)
     */
    private int insertRankingSnapshot(Connection conn, GameSessionData data, int levelId)
            throws SQLException {

        long userId   = data.getUserId();
        long bestTime = data.getCompletionTimeMs();
        int  bestScore = data.getScore();

        // Tính rank_position
        int rankPosition;
        try (PreparedStatement ps = conn.prepareStatement(SQL_CALC_RANK)) {
            ps.setInt(1, levelId);
            // best_time < ?
            ps.setLong(2, bestTime);
            // best_time = ? AND best_score > ?
            ps.setLong(3, bestTime);
            ps.setInt(4, bestScore);
            // best_time = ? AND best_score = ? AND achieved_at < CURRENT_TIMESTAMP
            ps.setLong(5, bestTime);
            ps.setInt(6, bestScore);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    rankPosition = rs.getInt("rank_position");
                } else {
                    rankPosition = 1; // Không có ai → rank 1
                }
            }
        }

        // INSERT snapshot
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_SNAPSHOT)) {
            ps.setLong(1, userId);
            ps.setInt(2, levelId);
            ps.setInt(3, rankPosition);
            ps.setInt(4, bestScore);
            ps.setLong(5, bestTime);
            ps.executeUpdate();
            LOG.debug("[Step 3] INSERT ranking_snapshots: userId={}, levelId={}, rank={}", userId, levelId, rankPosition);
        }

        return rankPosition;
    }

    /** Đặt Timestamp hoặc NULL nếu LocalDateTime là null. */
    private void setTimestampOrNull(PreparedStatement ps, int paramIndex,
                                    java.time.LocalDateTime ldt) throws SQLException {
        if (ldt != null) {
            ps.setTimestamp(paramIndex, Timestamp.valueOf(ldt));
        } else {
            ps.setNull(paramIndex, java.sql.Types.TIMESTAMP);
        }
    }

    /** Rollback an toàn, không để lỗi rollback che khuất lỗi gốc. */
    private void safeRollback(Connection conn) {
        try {
            conn.rollback();
            LOG.warn("Rollback thành công.");
        } catch (SQLException ex) {
            LOG.error("Rollback thất bại!", ex);
        }
    }
}

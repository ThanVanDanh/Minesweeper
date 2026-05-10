package minesweeper.repository;

/**
 * Kết quả trả về sau khi gọi {@link GameSessionRepository#saveGameResult(GameSessionData)}.
 * <p>
 * Chứa đủ thông tin để UI hiển thị kết quả: có kỷ lục mới không, xếp hạng bao nhiêu.
 */
public class GameSessionResult {

    /** ID của bản ghi game_sessions vừa INSERT */
    private final long sessionId;

    /**
     * true nếu Bước 2 đã INSERT hoặc UPDATE player_best_scores
     * (tức là người chơi thiết lập hoặc cải thiện kỷ lục cá nhân).
     */
    private final boolean newRecord;

    /**
     * Vị trí xếp hạng sau khi cập nhật kỷ lục.
     * null nếu không có kỷ lục mới (newRecord == false).
     */
    private final Integer newRank;

    public GameSessionResult(long sessionId, boolean newRecord, Integer newRank) {
        this.sessionId = sessionId;
        this.newRecord = newRecord;
        this.newRank   = newRank;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    /** ID của game_sessions vừa lưu. */
    public long getSessionId() { return sessionId; }

    /** Có kỷ lục mới (cá nhân) không? */
    public boolean isNewRecord() { return newRecord; }

    /**
     * Xếp hạng toàn server sau khi cập nhật kỷ lục.
     * Trả về {@code null} nếu không có kỷ lục mới.
     */
    public Integer getNewRank() { return newRank; }

    @Override
    public String toString() {
        return "GameSessionResult{sessionId=" + sessionId
                + ", newRecord=" + newRecord
                + ", newRank=" + newRank + '}';
    }
}

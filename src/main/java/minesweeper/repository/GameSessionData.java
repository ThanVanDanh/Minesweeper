package minesweeper.repository;

import minesweeper.model.Difficulty;

import java.time.LocalDateTime;

/**
 * Input data transfer object chứa tất cả thông tin cần lưu sau một ván chơi.
 * Được truyền vào {@link GameSessionRepository#saveGameResult(GameSessionData)}.
 */
public class GameSessionData {

    /** ID của user đang đăng nhập (từ SessionManager.getCurrentUser().getId()) */
    private final long userId;

    /** Cấp độ đang chơi, dùng để tra level_id trong DB */
    private final Difficulty difficulty;

    /** true = WIN, false = LOSE */
    private final boolean won;

    /** Thời gian hoàn thành (ms): từ first_click_at đến ended_at */
    private final long completionTimeMs;

    /** Điểm số đã tính sẵn */
    private final int score;

    /** Số ô đã mở */
    private final int openedCells;

    /** Số cờ đã cắm */
    private final int flaggedCells;

    /** Thời điểm ván bắt đầu (khi màn hình hiển thị) */
    private final LocalDateTime startedAt;

    /** Thời điểm click đầu tiên */
    private final LocalDateTime firstClickAt;

    /** Thời điểm ván kết thúc */
    private final LocalDateTime endedAt;

    private GameSessionData(Builder builder) {
        this.userId          = builder.userId;
        this.difficulty      = builder.difficulty;
        this.won             = builder.won;
        this.completionTimeMs = builder.completionTimeMs;
        this.score           = builder.score;
        this.openedCells     = builder.openedCells;
        this.flaggedCells    = builder.flaggedCells;
        this.startedAt       = builder.startedAt;
        this.firstClickAt    = builder.firstClickAt;
        this.endedAt         = builder.endedAt;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public long getUserId()            { return userId; }
    public Difficulty getDifficulty()  { return difficulty; }
    public boolean isWon()             { return won; }
    public long getCompletionTimeMs()  { return completionTimeMs; }
    public int getScore()              { return score; }
    public int getOpenedCells()        { return openedCells; }
    public int getFlaggedCells()       { return flaggedCells; }
    public LocalDateTime getStartedAt()    { return startedAt; }
    public LocalDateTime getFirstClickAt() { return firstClickAt; }
    public LocalDateTime getEndedAt()      { return endedAt; }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long userId;
        private Difficulty difficulty;
        private boolean won;
        private long completionTimeMs;
        private int score;
        private int openedCells;
        private int flaggedCells;
        private LocalDateTime startedAt;
        private LocalDateTime firstClickAt;
        private LocalDateTime endedAt;

        private Builder() {}

        public Builder userId(long userId)                   { this.userId = userId;               return this; }
        public Builder difficulty(Difficulty difficulty)     { this.difficulty = difficulty;       return this; }
        public Builder won(boolean won)                      { this.won = won;                     return this; }
        public Builder completionTimeMs(long ms)             { this.completionTimeMs = ms;         return this; }
        public Builder score(int score)                      { this.score = score;                 return this; }
        public Builder openedCells(int openedCells)          { this.openedCells = openedCells;     return this; }
        public Builder flaggedCells(int flaggedCells)        { this.flaggedCells = flaggedCells;   return this; }
        public Builder startedAt(LocalDateTime startedAt)    { this.startedAt = startedAt;         return this; }
        public Builder firstClickAt(LocalDateTime firstClickAt) { this.firstClickAt = firstClickAt; return this; }
        public Builder endedAt(LocalDateTime endedAt)        { this.endedAt = endedAt;             return this; }

        public GameSessionData build() {
            if (userId <= 0)           throw new IllegalStateException("userId phải > 0");
            if (difficulty == null)    throw new IllegalStateException("difficulty không được null");
            if (endedAt == null)       throw new IllegalStateException("endedAt không được null");
            if (completionTimeMs < 0)  throw new IllegalStateException("completionTimeMs không được âm");
            return new GameSessionData(this);
        }
    }
}

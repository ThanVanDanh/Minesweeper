package minesweeper.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class GameResult {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int id;
    private String username;
    private int score;
    private long timeSeconds;   // thời gian hoàn thành (giây)
    private Difficulty difficulty;
    private LocalDateTime playedAt;
    private boolean flaggedAsFraud;

    // ── Constructors ──────────────────────────────────────────────
    public GameResult() {}

    public GameResult(int id, String username, int score, long timeSeconds,
                      Difficulty difficulty, LocalDateTime playedAt) {
        this.id = id;
        this.username = username;
        this.score = score;
        this.timeSeconds = timeSeconds;
        this.difficulty = difficulty;
        this.playedAt = playedAt;
        this.flaggedAsFraud = false;
    }

    // ── Getters / Setters ─────────────────────────────────────────
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public long getTimeSeconds() { return timeSeconds; }
    public void setTimeSeconds(long timeSeconds) { this.timeSeconds = timeSeconds; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public LocalDateTime getPlayedAt() { return playedAt; }
    public void setPlayedAt(LocalDateTime playedAt) { this.playedAt = playedAt; }

    public boolean isFlaggedAsFraud() { return flaggedAsFraud; }
    public void setFlaggedAsFraud(boolean flaggedAsFraud) { this.flaggedAsFraud = flaggedAsFraud; }

    // ──  display ────────────────────────────────────────────
    public String getTimeFormatted() {
        return String.format("%02d:%02d", timeSeconds / 60, timeSeconds % 60);
    }

    public String getPlayedAtFormatted() {
        return playedAt != null ? playedAt.format(FMT) : "--";
    }

    @Override
    public String toString() {
        return String.format("GameResult{id=%d, user=%s, score=%d, time=%s, diff=%s}",
                id, username, score, getTimeFormatted(), difficulty);
    }
}
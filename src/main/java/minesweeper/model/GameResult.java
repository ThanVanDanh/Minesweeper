package minesweeper.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GameResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String gameId;
    private String playerName;
    private Difficulty difficulty;
    private boolean isWon;
    private long elapsedTimeMs;      // milliseconds
    private int flagsUsed;
    private int minesTotal;
    private LocalDateTime playedAt;
    private int score;
    private int openedCells;

    public GameResult() {}

    public GameResult(String gameId, String playerName, Difficulty difficulty,
                      boolean isWon, long elapsedTimeMs, int flagsUsed, 
                      int minesTotal, LocalDateTime playedAt) {
        this.gameId = gameId;
        this.playerName = playerName;
        this.difficulty = difficulty;
        this.isWon = isWon;
        this.elapsedTimeMs = elapsedTimeMs;
        this.flagsUsed = flagsUsed;
        this.minesTotal = minesTotal;
        this.playedAt = playedAt;
        this.score = calculateScore();
    }

    // Calculate score based on win + time + difficulty
    private int calculateScore() {
        if (!isWon) return 0;

        int baseScore = 1000;
        int difficultyBonus = 0;
        if (difficulty != null) {
            difficultyBonus = switch (difficulty) {
                case EASY -> 100;
                case MEDIUM -> 300;
                case HARD -> 500;
                default -> 0;
            };
        }

        // Time bonus using logarithmic decay (rewards speed with diminishing returns)
        // Fast games get high bonus, slow games still get some bonus
        long elapsedSeconds = elapsedTimeMs / 1000;
        int timeBonus = calculateTimeBonus(elapsedSeconds);

        return baseScore + difficultyBonus + timeBonus;
    }

    /**
     * Calculate time bonus with logarithmic decay
     * - Fast games (< 2 min): up to 300 bonus
     * - Moderate games (5-10 min): 50-150 bonus
     * - Slow games (> 15 min): 10-20 bonus
     * Games always get some bonus for winning, even if very slow
     */
    private int calculateTimeBonus(long elapsedSeconds) {
        if (elapsedSeconds <= 0) {
            return 300; // Max bonus for instant completion
        }
        
        // Logarithmic formula: 300 * ln(1 + 600/seconds)
        // This ensures diminishing returns and never hits 0
        double timeBonus = 300.0 * Math.log(1.0 + 600.0 / elapsedSeconds);
        return Math.max(10, (int) timeBonus); // Minimum 10 bonus even for very slow wins
    }

    // Getters
    public String getGameId() { return gameId; }
    public String getPlayerName() { return playerName; }
    public Difficulty getDifficulty() { return difficulty; }
    public boolean isWon() { return isWon; }
    public long getElapsedTimeMs() { return elapsedTimeMs; }
    public long getElapsedSeconds() { return elapsedTimeMs / 1000; }
    public int getFlagsUsed() { return flagsUsed; }
    public int getMinesTotal() { return minesTotal; }
    public LocalDateTime getPlayedAt() { return playedAt; }
    public int getScore() { return score; }
    public String getResult() {
        return isWon ? "Thắng" : "Thua";
    }

    // Setter
    public void setOpenedCells(int openedCells) { this.openedCells = openedCells; }
    public void setScore(int score) { this.score = score; }
    public String getDifficultyLabel() {
        return difficulty == null ? "N/A" : difficulty.getLabel();
    }
    public String getTimeFormatted() {
        long seconds = elapsedTimeMs / 1000;
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
    public String getPlayedAtFormatted() {
        if (playedAt == null) {
            return "N/A";
        }
        return playedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s (%s) - %s",
                getPlayedAtFormatted(), playerName, getDifficultyLabel(),
                getResult(), getTimeFormatted());
    }
}


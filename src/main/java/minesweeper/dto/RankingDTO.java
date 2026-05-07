package minesweeper.dto;

/**
 * Immutable DTO for ranking data
 * No setters - rank is assigned at creation time
 */
public final class RankingDTO {
    private final int rank;
    private final String playerName;
    private final int totalGames;
    private final int wins;
    private final int bestScore;
    private final long bestTimeMs;

    public RankingDTO(int rank, String playerName, int totalGames, int wins, int bestScore, long bestTimeMs) {
        this.rank = rank;
        this.playerName = playerName;
        this.totalGames = totalGames;
        this.wins = wins;
        this.bestScore = bestScore;
        this.bestTimeMs = bestTimeMs;
    }

    public int getRank() {
        return rank;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public int getWins() {
        return wins;
    }

    public int getBestScore() {
        return bestScore;
    }

    public long getBestTimeMs() {
        return bestTimeMs;
    }

    @Override
    public String toString() {
        return String.format("#%d %s: %d wins/%d games, best score: %d", 
                rank, playerName, wins, totalGames, bestScore);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RankingDTO)) return false;
        RankingDTO that = (RankingDTO) o;
        return rank == that.rank && playerName != null && playerName.equals(that.playerName);
    }

    @Override
    public int hashCode() {
        return 31 * rank + (playerName != null ? playerName.hashCode() : 0);
    }
}


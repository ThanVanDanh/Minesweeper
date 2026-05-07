package minesweeper.service;

import minesweeper.model.GameResult;
import minesweeper.repository.GameResultRepository;
import minesweeper.repository.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages ranking calculation and statistics
 */
public class RankingManager {
    private static final Logger LOG = LoggerFactory.getLogger(RankingManager.class);
    
    private final GameResultRepository gameResultRepository;

    public RankingManager(GameResultRepository gameResultRepository) {
        this.gameResultRepository = gameResultRepository;
    }

    /**
     * Get global ranking (all players, sorted by total score)
     */
    public List<PlayerRanking> getGlobalRanking() throws DataAccessException {
        try {
            List<GameResult> allResults = gameResultRepository.getAllResults();

            // Group by player + aggregate stats
            Map<String, PlayerStats> playerStats = new HashMap<>();

            for (GameResult result : allResults) {
                String playerName = normalizePlayerName(result.getPlayerName());
                if (playerName == null) {
                    continue;
                }
                playerStats.putIfAbsent(playerName, new PlayerStats(result.getPlayerName().trim()));
                playerStats.get(playerName).addResult(result);
            }

            // Convert to ranking list
            List<PlayerRanking> rankings = playerStats.values().stream()
                    .map(PlayerRanking::fromStats)
                    .sorted(Comparator
                            .comparingInt(PlayerRanking::getTotalScore).reversed()
                            .thenComparing(Comparator.comparingInt(PlayerRanking::getWins).reversed())
                            .thenComparing(PlayerRanking::getPlayerName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            // Add rank number
            for (int i = 0; i < rankings.size(); i++) {
                rankings.get(i).setRank(i + 1);
            }

            LOG.info("Global ranking calculated for {} players", rankings.size());
            return rankings;
        } catch (DataAccessException e) {
            LOG.error("Error calculating global ranking", e);
            throw e;
        }
    }

    /**
     * Get player's ranking position
     */
    public int getPlayerRankPosition(String playerName) throws DataAccessException {
        String normalizedName = normalizePlayerName(playerName);
        if (normalizedName == null) {
            LOG.warn("Invalid player name: {}", playerName);
            return -1;
        }
        try {
            List<PlayerRanking> rankings = getGlobalRanking();
            for (PlayerRanking ranking : rankings) {
                String rankingName = normalizePlayerName(ranking.getPlayerName());
                if (rankingName != null && rankingName.equals(normalizedName)) {
                    LOG.debug("Player {} found at rank {}", playerName, ranking.getRank());
                    return ranking.getRank();
                }
            }
            LOG.debug("Player not found in rankings: {}", playerName);
            return -1; // Not found
        } catch (DataAccessException e) {
            LOG.error("Error getting rank position for player: {}", playerName, e);
            throw e;
        }
    }

    /**
     * Get top N players
     */
    public List<PlayerRanking> getTopPlayers(int limit) throws DataAccessException {
        try {
            List<PlayerRanking> rankings = getGlobalRanking();
            List<PlayerRanking> topPlayers = rankings.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
            LOG.info("Retrieved top {} players", topPlayers.size());
            return topPlayers;
        } catch (DataAccessException e) {
            LOG.error("Error getting top {} players", limit, e);
            throw e;
        }
    }

    /**
     * Get player's statistics
     */
    public PlayerStats getPlayerStats(String playerName) throws DataAccessException {
        try {
            List<GameResult> playerResults = gameResultRepository.getPlayerHistory(playerName);
            PlayerStats stats = new PlayerStats(playerName == null ? "" : playerName.trim());

            for (GameResult result : playerResults) {
                stats.addResult(result);
            }

            LOG.info("Player stats calculated for {}: {} games, {} wins", 
                    playerName, stats.getTotalGames(), stats.getWins());
            return stats;
        } catch (DataAccessException e) {
            LOG.error("Error getting player stats for: {}", playerName, e);
            throw e;
        }
    }

    private String normalizePlayerName(String playerName) {
        if (playerName == null) {
            return null;
        }

        String normalized = playerName.trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * Player statistics
     */
    public static class PlayerStats {
        private final String playerName;
        private int totalGames = 0;
        private int wins = 0;
        private int losses = 0;
        private long totalTimeMs = 0;
        private int totalScore = 0;
        private int bestScore = 0;
        private int bestTimeSeconds = Integer.MAX_VALUE;

        public PlayerStats(String playerName) {
            this.playerName = playerName;
        }

        public void addResult(GameResult result) {
            totalGames++;
            if (result.isWon()) {
                wins++;
            } else {
                losses++;
            }

            totalTimeMs += result.getElapsedTimeMs();
            int resultScore = result.getScore();
            totalScore += resultScore;
            bestScore = Math.max(bestScore, resultScore);

            if (result.isWon()) {
                int seconds = (int) result.getElapsedSeconds();
                bestTimeSeconds = Math.min(bestTimeSeconds, seconds);
            }
        }

        // Getters
        public String getPlayerName() { return playerName; }
        public int getTotalGames() { return totalGames; }
        public int getWins() { return wins; }
        public int getLosses() { return losses; }
        public int getTotalScore() { return totalScore; }
        public int getBestScore() { return bestScore; }
        public int getBestTimeSeconds() {
            return bestTimeSeconds == Integer.MAX_VALUE ? 0 : bestTimeSeconds;
        }
        public double getWinRate() {
            return totalGames == 0 ? 0 : (double) wins / totalGames * 100;
        }
        public String getWinRateFormatted() {
            return String.format("%.1f%%", getWinRate());
        }
        public double getAverageTimeSeconds() {
            return totalGames == 0 ? 0 : (double) totalTimeMs / totalGames / 1000;
        }
        public String getAverageTimeFormatted() {
            double avgSeconds = getAverageTimeSeconds();
            return String.format("%.0f giây", avgSeconds);
        }
    }

    /**
     * Player ranking (with rank position)
     */
    public static class PlayerRanking {
        private int rank;
        private final String playerName;
        private final int totalGames;
        private final int wins;
        private final int totalScore;
        private final double winRate;

        public PlayerRanking(int rank, String playerName, int totalGames,
                           int wins, int totalScore, double winRate) {
            this.rank = rank;
            this.playerName = playerName;
            this.totalGames = totalGames;
            this.wins = wins;
            this.totalScore = totalScore;
            this.winRate = winRate;
        }

        public static PlayerRanking fromStats(PlayerStats stats) {
            return new PlayerRanking(
                    0,
                    stats.getPlayerName(),
                    stats.getTotalGames(),
                    stats.getWins(),
                    stats.getTotalScore(),
                    stats.getWinRate()
            );
        }

        public void setRank(int rank) { this.rank = rank; }
        public int getRank() { return rank; }
        public String getPlayerName() { return playerName; }
        public int getTotalGames() { return totalGames; }
        public int getWins() { return wins; }
        public int getTotalScore() { return totalScore; }
        public double getWinRate() { return winRate; }
        public String getWinRateFormatted() { return String.format("%.1f%%", winRate); }
    }
}

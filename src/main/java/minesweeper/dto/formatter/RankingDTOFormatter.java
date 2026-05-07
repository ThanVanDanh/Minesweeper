package minesweeper.dto.formatter;

import minesweeper.dto.RankingDTO;

/**
 * Formatter for RankingDTO - Separates presentation logic from data
 */
public class RankingDTOFormatter {
    private final RankingDTO dto;

    public RankingDTOFormatter(RankingDTO dto) {
        this.dto = dto;
    }

    public String getBestTimeFormatted() {
        long timeMs = dto.getBestTimeMs();
        if (timeMs <= 0) {
            return "--";
        }
        long seconds = timeMs / 1000;
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    public double getWinRate() {
        int totalGames = dto.getTotalGames();
        return totalGames == 0 ? 0.0 : ((double) dto.getWins() / totalGames) * 100.0;
    }

    public String getWinRateFormatted() {
        return String.format("%.1f%%", getWinRate());
    }

    public String getFormattedString() {
        return String.format("#%d %s: %d wins/%d games (%.1f%%), best score: %d, best time: %s",
                dto.getRank(),
                dto.getPlayerName(),
                dto.getWins(),
                dto.getTotalGames(),
                getWinRate(),
                dto.getBestScore(),
                getBestTimeFormatted());
    }
}

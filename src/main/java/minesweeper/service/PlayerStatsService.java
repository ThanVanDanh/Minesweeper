package minesweeper.service;

import minesweeper.model.GameResult;
import minesweeper.model.enums.Difficulty;

import java.util.*;
import java.util.stream.Collectors;

/**
 * UC05.11 – Thống kê tổng hợp theo người chơi [MỚI v1.2 – E1]
 *
 * <p>Business logic thuần (không phụ thuộc JavaFX) để tính toán thống kê
 * cho một người chơi cụ thể dựa trên danh sách kết quả game.
 *
 * <p>Bao gồm:
 * <ul>
 *   <li>Tổng ván chơi, tổng thắng/thua, tỉ lệ thắng</li>
 *   <li>Best score / best time theo từng độ khó</li>
 *   <li>Điểm trung bình</li>
 * </ul>
 */
public class PlayerStatsService {

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * 05.11.3 Hệ thống truy vấn toàn bộ game_sessions của user đó
     *          và tính toán các chỉ số thống kê tổng hợp.
     *
     * @param results danh sách kết quả game của một người chơi
     * @return DTO chứa toàn bộ thống kê tổng hợp
     */
    public PlayerStats computeStats(List<GameResult> results) {
        if (results == null || results.isEmpty()) {
            // 05.11-A1 User không có kết quả nào → trả về stats rỗng
            return PlayerStats.empty();
        }

        // 05.11.3a Tính tổng ván, thắng, thua
        int totalGames = results.size();
        long totalWins  = results.stream().filter(GameResult::isWon).count();
        long totalLoses = totalGames - totalWins;

        // 05.11.3b Tính tỉ lệ thắng (%)
        double winRate = (double) totalWins / totalGames * 100.0;

        // 05.11.3c Tính điểm trung bình (chỉ tính các ván WIN có score > 0)
        double avgScore = results.stream()
                .filter(GameResult::isWon)
                .mapToInt(GameResult::getScore)
                .average()
                .orElse(0.0);

        // 05.11.3d Tính best score và best time theo từng độ khó
        Map<Difficulty, DifficultyStats> perDifficulty = computePerDifficulty(results);

        return new PlayerStats(
                totalGames, totalWins, totalLoses,
                winRate, avgScore, perDifficulty
        );
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * 05.11.3d Nhóm kết quả theo từng Difficulty, tính best score,
     *           best time, tổng ván và tỉ lệ thắng cho mỗi nhóm.
     */
    private Map<Difficulty, DifficultyStats> computePerDifficulty(List<GameResult> results) {
        Map<Difficulty, List<GameResult>> grouped = results.stream()
                .filter(r -> r.getDifficulty() != null)
                .collect(Collectors.groupingBy(GameResult::getDifficulty));

        Map<Difficulty, DifficultyStats> map = new EnumMap<>(Difficulty.class);
        for (var entry : grouped.entrySet()) {
            Difficulty d       = entry.getKey();
            List<GameResult> g = entry.getValue();

            int total = g.size();
            long wins = g.stream().filter(GameResult::isWon).count();

            // Best score = max score trong tất cả các ván WIN
            int bestScore = g.stream()
                    .filter(GameResult::isWon)
                    .mapToInt(GameResult::getScore)
                    .max()
                    .orElse(0);

            // Best time = min elapsed trong tất cả các ván WIN (ms)
            long bestTimeMs = g.stream()
                    .filter(GameResult::isWon)
                    .mapToLong(GameResult::getElapsedTimeMs)
                    .filter(t -> t > 0)
                    .min()
                    .orElse(0L);

            double winRate = total > 0 ? (double) wins / total * 100.0 : 0.0;

            map.put(d, new DifficultyStats(total, wins, winRate, bestScore, bestTimeMs));
        }
        return map;
    }

    // =========================================================================
    // DTOs
    // =========================================================================

    /**
     * 05.11.4 Thông tin thống kê tổng hợp của một người chơi.
     */
    public record PlayerStats(
            int totalGames,
            long totalWins,
            long totalLoses,
            double winRate,
            double avgScore,
            Map<Difficulty, DifficultyStats> perDifficulty
    ) {
        /** Trả về stats rỗng khi user chưa có dữ liệu (05.11-A1). */
        public static PlayerStats empty() {
            return new PlayerStats(0, 0, 0, 0.0, 0.0, Map.of());
        }
    }

    /**
     * Thống kê theo từng độ khó.
     */
    public record DifficultyStats(
            int totalGames,
            long totalWins,
            double winRate,
            int bestScore,
            long bestTimeMs
    ) {
        /** Format best time thành mm:ss. */
        public String bestTimeFormatted() {
            if (bestTimeMs <= 0) return "--:--";
            long seconds = bestTimeMs / 1000;
            return String.format("%02d:%02d", seconds / 60, seconds % 60);
        }
    }
}

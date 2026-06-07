package minesweeper.service;

import minesweeper.model.GameResult;
import minesweeper.model.enums.Difficulty;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UC05.10 – Phát hiện gian lận tự động [MỚI v1.2 – B5]
 *
 * <p>Business logic thuần (không phụ thuộc JavaFX) để phát hiện các kết quả
 * game nghi vấn dựa trên ngưỡng thời gian tối thiểu theo từng độ khó.
 *
 * <p>Ngưỡng mặc định (thời gian hoàn thành < ngưỡng → nghi vấn gian lận):
 * <ul>
 *   <li>EASY   (9×9,   10 mìn) : &lt; 5 000 ms  (&lt; 5  giây)</li>
 *   <li>MEDIUM (16×16, 40 mìn) : &lt; 20 000 ms (&lt; 20 giây)</li>
 *   <li>HARD   (16×30, 99 mìn) : &lt; 60 000 ms (&lt; 1  phút)</li>
 *   <li>EXPERT (20×30,145 mìn) : &lt; 120 000 ms(&lt; 2  phút)</li>
 * </ul>
 */
public class FraudDetectionService {

    /** Ngưỡng thời gian tối thiểu (ms) theo độ khó. Dưới ngưỡng → nghi vấn. */
    private final Map<Difficulty, Long> thresholdsMs;

    // =========================================================================
    // Constructors
    // =========================================================================

    /**
     * 05.10.2 Hệ thống đọc ngưỡng thời gian hợp lý (dùng ngưỡng mặc định).
     * Các giá trị này dựa trên dữ liệu thống kê và cấu hình game_levels:
     *   EASY < 5s | MEDIUM < 20s | HARD < 60s | EXPERT < 120s
     */
    public FraudDetectionService() {
        Map<Difficulty, Long> defaults = new EnumMap<>(Difficulty.class);
        defaults.put(Difficulty.EASY,   5_000L);   // < 5 giây
        defaults.put(Difficulty.MEDIUM, 20_000L);  // < 20 giây
        defaults.put(Difficulty.HARD,   60_000L);  // < 60 giây
        defaults.put(Difficulty.EXPERT, 120_000L); // < 120 giây
        this.thresholdsMs = defaults;
    }

    /**
     * Constructor cho test / custom ngưỡng.
     *
     * @param thresholdsMs map từ Difficulty → ngưỡng ms (completion_time < threshold = nghi vấn)
     */
    public FraudDetectionService(Map<Difficulty, Long> thresholdsMs) {
        this.thresholdsMs = new EnumMap<>(thresholdsMs);
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * 05.10.3 Hệ thống quét tất cả kết quả WIN trong danh sách, so sánh
     *          completion_time với ngưỡng tương ứng của từng level.
     *
     * <p>Chỉ kết quả WIN mới được kiểm tra (thua thì không có nghĩa là gian lận).
     * Kết quả có difficulty null hoặc không có ngưỡng được bỏ qua.
     *
     * @param results danh sách kết quả đang hiển thị trên bảng (trang hiện tại)
     * @return danh sách kết quả nghi vấn (có thể rỗng – ứng với 05.10-A1)
     */
    public List<GameResult> detectSuspicious(List<GameResult> results) {
        if (results == null) return List.of();
        return results.stream()
                .filter(r -> r != null && r.isWon())    // chỉ WIN
                .filter(this::isSuspicious)             // so sánh ngưỡng
                .collect(Collectors.toList());
    }

    /**
     * 05.10.2 Kiểm tra một kết quả có vượt ngưỡng nghi vấn hay không.
     *
     * @param result kết quả cần kiểm tra
     * @return {@code true} nếu completion_time hợp lệ (> 0) và nhỏ hơn ngưỡng của level đó
     */
    public boolean isSuspicious(GameResult result) {
        if (result == null || !result.isWon()) return false;

        Difficulty d = result.getDifficulty();
        if (d == null) return false;

        Long threshold = thresholdsMs.get(d);
        if (threshold == null) return false;   // độ khó không có ngưỡng → bỏ qua

        long elapsed = result.getElapsedTimeMs();
        return elapsed > 0 && elapsed < threshold;
    }

    /**
     * Trả về ngưỡng (ms) cho một độ khó cụ thể.
     * Dùng trong test để verify cấu hình.
     */
    public Long getThreshold(Difficulty difficulty) {
        return thresholdsMs.get(difficulty);
    }

    /** Trả về toàn bộ bảng ngưỡng (unmodifiable). */
    public Map<Difficulty, Long> getThresholdsMs() {
        return Map.copyOf(thresholdsMs);
    }
}

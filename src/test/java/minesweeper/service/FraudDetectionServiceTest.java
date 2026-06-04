package minesweeper.service;

import minesweeper.model.GameResult;
import minesweeper.model.enums.Difficulty;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho FraudDetectionService – UC05.10 [MỚI v1.2 – B5]
 *
 * Phạm vi kiểm thử theo đặc tả:
 *  - 05.10.2 : Ngưỡng thời gian hợp lý theo từng độ khó
 *  - 05.10.3 : Chỉ quét kết quả WIN; so sánh completion_time với ngưỡng
 *  - 05.10-A1: Không tìm thấy kết quả nghi vấn → danh sách rỗng
 */
class FraudDetectionServiceTest {

    private FraudDetectionService service;

    // ── Ngưỡng mặc định (ms) theo spec 05.10.2 ─────────────────────────────
    private static final long EASY_THRESHOLD   =   5_000L;
    private static final long MEDIUM_THRESHOLD =  20_000L;
    private static final long HARD_THRESHOLD   =  60_000L;
    private static final long EXPERT_THRESHOLD = 120_000L;

    @BeforeEach
    void setUp() {
        service = new FraudDetectionService();
    }

    // =========================================================================
    // 05.10.2 – Kiểm tra cấu hình ngưỡng mặc định
    // =========================================================================

    @Nested
    @DisplayName("05.10.2 – Cấu hình ngưỡng thời gian theo độ khó")
    class ThresholdConfigTests {

        @Test
        @DisplayName("05.10.2: EASY threshold = 5 000 ms (< 5 giây là nghi vấn)")
        void defaultThreshold_Easy_ShouldBe5000ms() {
            assertEquals(EASY_THRESHOLD, service.getThreshold(Difficulty.EASY));
        }

        @Test
        @DisplayName("05.10.2: MEDIUM threshold = 20 000 ms (< 20 giây là nghi vấn)")
        void defaultThreshold_Medium_ShouldBe20000ms() {
            assertEquals(MEDIUM_THRESHOLD, service.getThreshold(Difficulty.MEDIUM));
        }

        @Test
        @DisplayName("05.10.2: HARD threshold = 60 000 ms (< 1 phút là nghi vấn)")
        void defaultThreshold_Hard_ShouldBe60000ms() {
            assertEquals(HARD_THRESHOLD, service.getThreshold(Difficulty.HARD));
        }

        @Test
        @DisplayName("05.10.2: EXPERT threshold = 120 000 ms (< 2 phút là nghi vấn)")
        void defaultThreshold_Expert_ShouldBe120000ms() {
            assertEquals(EXPERT_THRESHOLD, service.getThreshold(Difficulty.EXPERT));
        }

        @Test
        @DisplayName("05.10.2: Tất cả 4 độ khó đều có ngưỡng (không null)")
        void defaultThresholds_AllDifficulties_ShouldExist() {
            for (Difficulty d : Difficulty.values()) {
                assertNotNull(service.getThreshold(d),
                        "Ngưỡng không nên null cho difficulty: " + d);
            }
        }

        @Test
        @DisplayName("05.10.2: Custom threshold được sử dụng đúng")
        void customThresholds_ShouldOverrideDefaults() {
            Map<Difficulty, Long> custom = new EnumMap<>(Difficulty.class);
            custom.put(Difficulty.EASY,   3_000L);
            custom.put(Difficulty.MEDIUM, 10_000L);
            custom.put(Difficulty.HARD,   30_000L);
            custom.put(Difficulty.EXPERT, 90_000L);

            FraudDetectionService customService = new FraudDetectionService(custom);

            assertEquals(3_000L,  customService.getThreshold(Difficulty.EASY));
            assertEquals(10_000L, customService.getThreshold(Difficulty.MEDIUM));
            assertEquals(30_000L, customService.getThreshold(Difficulty.HARD));
            assertEquals(90_000L, customService.getThreshold(Difficulty.EXPERT));
        }
    }

    // =========================================================================
    // 05.10.3 – isSuspicious: Logic phát hiện từng kết quả
    // =========================================================================

    @Nested
    @DisplayName("05.10.3 – isSuspicious: So sánh completion_time với ngưỡng")
    class IsSuspiciousTests {

        // ── Kết quả WIN dưới ngưỡng → nghi vấn ─────────────────────────────

        @Test
        @DisplayName("05.10.3: WIN EASY 1 000ms < 5 000ms → nghi vấn")
        void easy_Win_BelowThreshold_ShouldBeSuspicious() {
            GameResult r = buildResult(Difficulty.EASY, true, 1_000L);
            assertTrue(service.isSuspicious(r));
        }

        @Test
        @DisplayName("05.10.3: WIN MEDIUM 5 000ms < 20 000ms → nghi vấn")
        void medium_Win_BelowThreshold_ShouldBeSuspicious() {
            GameResult r = buildResult(Difficulty.MEDIUM, true, 5_000L);
            assertTrue(service.isSuspicious(r));
        }

        @Test
        @DisplayName("05.10.3: WIN HARD 30 000ms < 60 000ms → nghi vấn")
        void hard_Win_BelowThreshold_ShouldBeSuspicious() {
            GameResult r = buildResult(Difficulty.HARD, true, 30_000L);
            assertTrue(service.isSuspicious(r));
        }

        @Test
        @DisplayName("05.10.3: WIN EXPERT 60 000ms < 120 000ms → nghi vấn")
        void expert_Win_BelowThreshold_ShouldBeSuspicious() {
            GameResult r = buildResult(Difficulty.EXPERT, true, 60_000L);
            assertTrue(service.isSuspicious(r));
        }

        // ── Kết quả WIN trên ngưỡng → không nghi vấn ───────────────────────

        @Test
        @DisplayName("05.10.3: WIN EASY 10 000ms ≥ 5 000ms → bình thường")
        void easy_Win_AboveThreshold_ShouldNotBeSuspicious() {
            GameResult r = buildResult(Difficulty.EASY, true, 10_000L);
            assertFalse(service.isSuspicious(r));
        }

        @Test
        @DisplayName("05.10.3: WIN MEDIUM 25 000ms ≥ 20 000ms → bình thường")
        void medium_Win_AboveThreshold_ShouldNotBeSuspicious() {
            GameResult r = buildResult(Difficulty.MEDIUM, true, 25_000L);
            assertFalse(service.isSuspicious(r));
        }

        @Test
        @DisplayName("05.10.3: WIN EXPERT 180 000ms ≥ 120 000ms → bình thường")
        void expert_Win_AboveThreshold_ShouldNotBeSuspicious() {
            GameResult r = buildResult(Difficulty.EXPERT, true, 180_000L);
            assertFalse(service.isSuspicious(r));
        }

        // ── Biên chính xác bằng ngưỡng → KHÔNG nghi vấn (< threshold, không phải ≤) ──

        @Test
        @DisplayName("05.10.3 [Biên]: WIN EASY elapsed = 5 000ms = threshold → bình thường")
        void easy_Win_ExactlyAtThreshold_ShouldNotBeSuspicious() {
            GameResult r = buildResult(Difficulty.EASY, true, EASY_THRESHOLD);
            assertFalse(service.isSuspicious(r),
                    "Biên: thời gian bằng đúng ngưỡng không tính là gian lận");
        }

        @Test
        @DisplayName("05.10.3 [Biên]: WIN EASY elapsed = 4 999ms = threshold-1 → nghi vấn")
        void easy_Win_OneBelowThreshold_ShouldBeSuspicious() {
            GameResult r = buildResult(Difficulty.EASY, true, EASY_THRESHOLD - 1);
            assertTrue(service.isSuspicious(r));
        }

        // ── Kết quả THUA → không nghi vấn (chỉ WIN mới kiểm tra) ───────────

        @Test
        @DisplayName("05.10.3: LOSE EASY 500ms → bình thường (thua không gian lận)")
        void easy_Loss_BelowThreshold_ShouldNotBeSuspicious() {
            GameResult r = buildResult(Difficulty.EASY, false, 500L);
            assertFalse(service.isSuspicious(r));
        }

        @Test
        @DisplayName("05.10.3: LOSE EXPERT 1ms → bình thường (thua không gian lận)")
        void expert_Loss_VeryFast_ShouldNotBeSuspicious() {
            GameResult r = buildResult(Difficulty.EXPERT, false, 1L);
            assertFalse(service.isSuspicious(r));
        }

        // ── Edge cases ──────────────────────────────────────────────────────

        @Test
        @DisplayName("05.10.3: elapsed = 0ms → bình thường (dữ liệu lỗi, elapsed = 0 bị bỏ qua)")
        void win_ZeroElapsed_ShouldNotBeSuspicious() {
            GameResult r = buildResult(Difficulty.EASY, true, 0L);
            assertFalse(service.isSuspicious(r),
                    "Elapsed = 0 là dữ liệu thiếu/lỗi, không tính là gian lận");
        }

        @Test
        @DisplayName("05.10.3: result null → false (không ném exception)")
        void nullResult_ShouldReturnFalse() {
            assertFalse(service.isSuspicious(null));
        }
    }

    // =========================================================================
    // 05.10.3 + 05.10-A1 – detectSuspicious trên danh sách
    // =========================================================================

    @Nested
    @DisplayName("05.10.3 / 05.10-A1 – detectSuspicious(List<GameResult>)")
    class DetectSuspiciousListTests {

        @Test
        @DisplayName("05.10-A1: Danh sách rỗng → kết quả rỗng")
        void emptyList_ShouldReturnEmpty() {
            List<GameResult> result = service.detectSuspicious(List.of());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("05.10-A1: Null input → kết quả rỗng (không throw exception)")
        void nullList_ShouldReturnEmpty() {
            List<GameResult> result = service.detectSuspicious(null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("05.10-A1: Tất cả kết quả bình thường → trả về danh sách rỗng")
        void allNormal_ShouldReturnEmpty() {
            List<GameResult> input = List.of(
                    buildResult(Difficulty.EASY,   true,  10_000L),  // ≥ 5s
                    buildResult(Difficulty.MEDIUM, true,  30_000L),  // ≥ 20s
                    buildResult(Difficulty.HARD,   true,  90_000L)   // ≥ 60s
            );
            List<GameResult> suspicious = service.detectSuspicious(input);
            assertTrue(suspicious.isEmpty());
        }

        @Test
        @DisplayName("05.10.3: Tất cả là THUA → không phát hiện nghi vấn nào")
        void allLosses_ShouldReturnEmpty() {
            List<GameResult> input = List.of(
                    buildResult(Difficulty.EASY,   false, 500L),
                    buildResult(Difficulty.MEDIUM, false, 1_000L),
                    buildResult(Difficulty.EXPERT, false, 1L)
            );
            List<GameResult> suspicious = service.detectSuspicious(input);
            assertTrue(suspicious.isEmpty());
        }

        @Test
        @DisplayName("05.10.3: Hỗn hợp WIN/LOSE, chỉ WIN dưới ngưỡng được phát hiện")
        void mixedWinLose_OnlySuspiciousWinDetected() {
            GameResult suspiciousWin  = buildResult(Difficulty.EASY, true,  1_000L); // nghi vấn
            GameResult normalWin      = buildResult(Difficulty.EASY, true, 10_000L); // bình thường
            GameResult lossUnderLimit = buildResult(Difficulty.EASY, false,  500L); // thua → bỏ qua

            List<GameResult> suspicious = service.detectSuspicious(
                    List.of(suspiciousWin, normalWin, lossUnderLimit));

            assertEquals(1, suspicious.size());
            assertTrue(suspicious.contains(suspiciousWin));
        }

        @Test
        @DisplayName("05.10.3: Nhiều độ khó, phát hiện đúng số lượng nghi vấn")
        void multiDifficulty_DetectsCorrectCount() {
            List<GameResult> input = List.of(
                    buildResult(Difficulty.EASY,   true,  1_000L),  // nghi vấn ✓
                    buildResult(Difficulty.EASY,   true, 10_000L),  // bình thường
                    buildResult(Difficulty.MEDIUM, true,  5_000L),  // nghi vấn ✓
                    buildResult(Difficulty.MEDIUM, true, 25_000L),  // bình thường
                    buildResult(Difficulty.HARD,   true, 30_000L),  // nghi vấn ✓
                    buildResult(Difficulty.EXPERT, true, 60_000L)   // nghi vấn ✓
            );

            List<GameResult> suspicious = service.detectSuspicious(input);

            assertEquals(4, suspicious.size(),
                    "Phải phát hiện đúng 4 kết quả nghi vấn trong 6 kết quả");
        }

        @Test
        @DisplayName("05.10.3: Tất cả đều nghi vấn → trả về toàn bộ danh sách WIN")
        void allSuspicious_ShouldReturnAll() {
            List<GameResult> input = List.of(
                    buildResult(Difficulty.EASY,   true,   500L),
                    buildResult(Difficulty.MEDIUM, true,  1_000L),
                    buildResult(Difficulty.HARD,   true,  5_000L),
                    buildResult(Difficulty.EXPERT, true, 10_000L)
            );

            List<GameResult> suspicious = service.detectSuspicious(input);

            assertEquals(4, suspicious.size());
        }

        @Test
        @DisplayName("05.10.3: Bảo toàn thứ tự phát hiện giống thứ tự input")
        void detectedOrder_ShouldMatchInputOrder() {
            GameResult first  = buildResult(Difficulty.EASY,   true, 500L);
            GameResult second = buildResult(Difficulty.MEDIUM, true, 500L);
            GameResult third  = buildResult(Difficulty.HARD,   true, 500L);

            List<GameResult> suspicious = service.detectSuspicious(
                    List.of(first, second, third));

            assertEquals(List.of(first, second, third), suspicious);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Tạo GameResult giả cho test.
     *
     * @param difficulty  độ khó
     * @param won         true = WIN, false = LOSE
     * @param elapsedMs   thời gian hoàn thành (ms)
     */
    private GameResult buildResult(Difficulty difficulty, boolean won, long elapsedMs) {
        return new GameResult(
                "G" + System.nanoTime(),
                "player1",
                difficulty,
                won,
                elapsedMs,
                won ? 10 : 0,
                difficulty.getMines(),
                LocalDateTime.now()
        );
    }
}

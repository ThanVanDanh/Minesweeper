package minesweeper.service;

import minesweeper.model.GameResult;
import minesweeper.model.enums.Difficulty;
import minesweeper.service.PlayerStatsService.DifficultyStats;
import minesweeper.service.PlayerStatsService.PlayerStats;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho PlayerStatsService – UC05.11 [MỚI v1.2 – E1]
 *
 * Phạm vi kiểm thử theo đặc tả:
 *  - 05.11.3a : Tổng ván, thắng, thua
 *  - 05.11.3b : Tỉ lệ thắng (%)
 *  - 05.11.3c : Điểm trung bình (chỉ WIN)
 *  - 05.11.3d : Best score / best time theo từng độ khó
 *  - 05.11-A1 : User không có kết quả nào → stats rỗng
 */
class PlayerStatsServiceTest {

    private PlayerStatsService service;

    @BeforeEach
    void setUp() {
        service = new PlayerStatsService();
    }

    // =========================================================================
    // 05.11-A1 – User không có kết quả nào → stats rỗng
    // =========================================================================

    @Nested
    @DisplayName("05.11-A1 – Không có kết quả")
    class EmptyStatsTests {

        @Test
        @DisplayName("05.11-A1: Null input → PlayerStats.empty()")
        void computeStats_NullInput_ShouldReturnEmpty() {
            PlayerStats stats = service.computeStats(null);
            assertEquals(0, stats.totalGames());
            assertEquals(0, stats.totalWins());
            assertEquals(0, stats.totalLoses());
            assertEquals(0.0, stats.winRate());
            assertEquals(0.0, stats.avgScore());
            assertTrue(stats.perDifficulty().isEmpty());
        }

        @Test
        @DisplayName("05.11-A1: Danh sách rỗng → PlayerStats.empty()")
        void computeStats_EmptyList_ShouldReturnEmpty() {
            PlayerStats stats = service.computeStats(List.of());
            assertEquals(0, stats.totalGames());
            assertTrue(stats.perDifficulty().isEmpty());
        }

        @Test
        @DisplayName("05.11-A1: PlayerStats.empty() factory method đúng giá trị")
        void empty_ShouldReturnZeroStats() {
            PlayerStats empty = PlayerStats.empty();
            assertEquals(0, empty.totalGames());
            assertEquals(0, empty.totalWins());
            assertEquals(0, empty.totalLoses());
            assertEquals(0.0, empty.winRate(), 0.001);
            assertEquals(0.0, empty.avgScore(), 0.001);
            assertTrue(empty.perDifficulty().isEmpty());
        }
    }

    // =========================================================================
    // 05.11.3a – Tổng ván, thắng, thua
    // =========================================================================

    @Nested
    @DisplayName("05.11.3a – Tổng ván chơi, thắng, thua")
    class TotalGamesTests {

        @Test
        @DisplayName("05.11.3a: 1 ván WIN → totalGames=1, wins=1, loses=0")
        void singleWin_ShouldCountCorrectly() {
            List<GameResult> results = List.of(
                    buildResult(Difficulty.EASY, true, 10_000L, 1200)
            );
            PlayerStats stats = service.computeStats(results);
            assertEquals(1, stats.totalGames());
            assertEquals(1, stats.totalWins());
            assertEquals(0, stats.totalLoses());
        }

        @Test
        @DisplayName("05.11.3a: 1 ván LOSE → totalGames=1, wins=0, loses=1")
        void singleLoss_ShouldCountCorrectly() {
            List<GameResult> results = List.of(
                    buildResult(Difficulty.EASY, false, 5_000L, 0)
            );
            PlayerStats stats = service.computeStats(results);
            assertEquals(1, stats.totalGames());
            assertEquals(0, stats.totalWins());
            assertEquals(1, stats.totalLoses());
        }

        @Test
        @DisplayName("05.11.3a: 5 ván (3 WIN + 2 LOSE) → đếm đúng")
        void mixedResults_ShouldCountCorrectly() {
            List<GameResult> results = List.of(
                    buildResult(Difficulty.EASY, true,  10_000L, 1500),
                    buildResult(Difficulty.EASY, true,  15_000L, 1300),
                    buildResult(Difficulty.EASY, false,  5_000L, 0),
                    buildResult(Difficulty.MEDIUM, true, 30_000L, 1800),
                    buildResult(Difficulty.MEDIUM, false, 8_000L, 0)
            );
            PlayerStats stats = service.computeStats(results);
            assertEquals(5, stats.totalGames());
            assertEquals(3, stats.totalWins());
            assertEquals(2, stats.totalLoses());
        }
    }

    // =========================================================================
    // 05.11.3b – Tỉ lệ thắng (%)
    // =========================================================================

    @Nested
    @DisplayName("05.11.3b – Tỉ lệ thắng")
    class WinRateTests {

        @Test
        @DisplayName("05.11.3b: 3 WIN / 5 total → 60.0%")
        void winRate_3of5_ShouldBe60Percent() {
            List<GameResult> results = List.of(
                    buildResult(Difficulty.EASY, true,  10_000L, 1500),
                    buildResult(Difficulty.EASY, true,  15_000L, 1300),
                    buildResult(Difficulty.EASY, false,  5_000L, 0),
                    buildResult(Difficulty.EASY, true,  20_000L, 1100),
                    buildResult(Difficulty.EASY, false,  3_000L, 0)
            );
            PlayerStats stats = service.computeStats(results);
            assertEquals(60.0, stats.winRate(), 0.01);
        }

        @Test
        @DisplayName("05.11.3b: 0 WIN / 3 total → 0.0%")
        void winRate_AllLosses_ShouldBe0() {
            List<GameResult> results = List.of(
                    buildResult(Difficulty.EASY, false, 1_000L, 0),
                    buildResult(Difficulty.EASY, false, 2_000L, 0),
                    buildResult(Difficulty.EASY, false, 3_000L, 0)
            );
            PlayerStats stats = service.computeStats(results);
            assertEquals(0.0, stats.winRate(), 0.01);
        }

        @Test
        @DisplayName("05.11.3b: 4 WIN / 4 total → 100.0%")
        void winRate_AllWins_ShouldBe100() {
            List<GameResult> results = List.of(
                    buildResult(Difficulty.EASY, true, 10_000L, 1500),
                    buildResult(Difficulty.EASY, true, 12_000L, 1400),
                    buildResult(Difficulty.EASY, true, 14_000L, 1300),
                    buildResult(Difficulty.EASY, true, 16_000L, 1200)
            );
            PlayerStats stats = service.computeStats(results);
            assertEquals(100.0, stats.winRate(), 0.01);
        }
    }

    // =========================================================================
    // 05.11.3c – Điểm trung bình (chỉ tính WIN)
    // =========================================================================

    @Nested
    @DisplayName("05.11.3c – Điểm trung bình (chỉ WIN)")
    class AvgScoreTests {

        @Test
        @DisplayName("05.11.3c: 2 WIN (1500 + 1300) → avg = 1400.0")
        void avgScore_TwoWins_ShouldBeCorrect() {
            List<GameResult> results = List.of(
                    buildResult(Difficulty.EASY, true, 10_000L, 1500),
                    buildResult(Difficulty.EASY, true, 15_000L, 1300),
                    buildResult(Difficulty.EASY, false, 5_000L, 0)
            );
            PlayerStats stats = service.computeStats(results);
            assertEquals(1400.0, stats.avgScore(), 0.01);
        }

        @Test
        @DisplayName("05.11.3c: Chỉ LOSE → avg = 0.0")
        void avgScore_AllLosses_ShouldBe0() {
            List<GameResult> results = List.of(
                    buildResult(Difficulty.EASY, false, 5_000L, 0),
                    buildResult(Difficulty.EASY, false, 3_000L, 0)
            );
            PlayerStats stats = service.computeStats(results);
            assertEquals(0.0, stats.avgScore(), 0.01);
        }

        @Test
        @DisplayName("05.11.3c: 1 WIN (2000) → avg = 2000.0")
        void avgScore_SingleWin_ShouldBeExactScore() {
            List<GameResult> results = List.of(
                    buildResult(Difficulty.EXPERT, true, 180_000L, 2000)
            );
            PlayerStats stats = service.computeStats(results);
            assertEquals(2000.0, stats.avgScore(), 0.01);
        }
    }

    // =========================================================================
    // 05.11.3d – Best score / best time theo từng độ khó
    // =========================================================================

    @Nested
    @DisplayName("05.11.3d – Thống kê theo từng độ khó")
    class PerDifficultyTests {

        @Test
        @DisplayName("05.11.3d: Kết quả nhiều độ khó → nhóm đúng số lượng")
        void perDifficulty_GroupedCorrectly() {
            List<GameResult> results = List.of(
                    buildResult(Difficulty.EASY,   true,  10_000L, 1500),
                    buildResult(Difficulty.EASY,   false,  5_000L, 0),
                    buildResult(Difficulty.MEDIUM, true,  30_000L, 1800),
                    buildResult(Difficulty.HARD,   true,  90_000L, 2100)
            );
            PlayerStats stats = service.computeStats(results);
            Map<Difficulty, DifficultyStats> pd = stats.perDifficulty();

            assertEquals(3, pd.size(), "Phải có 3 nhóm độ khó");
            assertTrue(pd.containsKey(Difficulty.EASY));
            assertTrue(pd.containsKey(Difficulty.MEDIUM));
            assertTrue(pd.containsKey(Difficulty.HARD));
        }

        @Test
        @DisplayName("05.11.3d: EASY 2 ván (1 WIN 1500, 1 LOSE) → bestScore=1500, winRate=50%")
        void perDifficulty_Easy_StatsCorrect() {
            List<GameResult> results = List.of(
                    buildResult(Difficulty.EASY, true,  10_000L, 1500),
                    buildResult(Difficulty.EASY, false,  5_000L, 0)
            );
            PlayerStats stats = service.computeStats(results);
            DifficultyStats easy = stats.perDifficulty().get(Difficulty.EASY);

            assertNotNull(easy);
            assertEquals(2, easy.totalGames());
            assertEquals(1, easy.totalWins());
            assertEquals(50.0, easy.winRate(), 0.01);
            assertEquals(1500, easy.bestScore());
            assertEquals(10_000L, easy.bestTimeMs());
        }

        @Test
        @DisplayName("05.11.3d: Best score = max score trong các ván WIN")
        void perDifficulty_BestScore_ShouldBeMaxWinScore() {
            List<GameResult> results = List.of(
                    buildResult(Difficulty.MEDIUM, true, 30_000L, 1200),
                    buildResult(Difficulty.MEDIUM, true, 25_000L, 1800),
                    buildResult(Difficulty.MEDIUM, true, 35_000L, 1500)
            );
            PlayerStats stats = service.computeStats(results);
            DifficultyStats medium = stats.perDifficulty().get(Difficulty.MEDIUM);

            assertEquals(1800, medium.bestScore());
        }

        @Test
        @DisplayName("05.11.3d: Best time = min elapsed trong các ván WIN")
        void perDifficulty_BestTime_ShouldBeMinWinElapsed() {
            List<GameResult> results = List.of(
                    buildResult(Difficulty.HARD, true, 90_000L, 2000),
                    buildResult(Difficulty.HARD, true, 70_000L, 2200),
                    buildResult(Difficulty.HARD, true, 85_000L, 2100)
            );
            PlayerStats stats = service.computeStats(results);
            DifficultyStats hard = stats.perDifficulty().get(Difficulty.HARD);

            assertEquals(70_000L, hard.bestTimeMs());
        }

        @Test
        @DisplayName("05.11.3d: Chỉ LOSE trong một độ khó → bestScore=0, bestTime=0")
        void perDifficulty_OnlyLosses_ShouldHaveZeroBests() {
            List<GameResult> results = List.of(
                    buildResult(Difficulty.EXPERT, false, 30_000L, 0),
                    buildResult(Difficulty.EXPERT, false, 60_000L, 0)
            );
            PlayerStats stats = service.computeStats(results);
            DifficultyStats expert = stats.perDifficulty().get(Difficulty.EXPERT);

            assertNotNull(expert);
            assertEquals(2, expert.totalGames());
            assertEquals(0, expert.totalWins());
            assertEquals(0, expert.bestScore());
            assertEquals(0L, expert.bestTimeMs());
        }

        @Test
        @DisplayName("05.11.3d: Kết quả null difficulty → bỏ qua trong perDifficulty")
        void perDifficulty_NullDifficulty_ShouldBeIgnored() {
            List<GameResult> results = new ArrayList<>();
            results.add(buildResult(null, true, 10_000L, 1000));
            results.add(buildResult(Difficulty.EASY, true, 10_000L, 1500));

            PlayerStats stats = service.computeStats(results);
            // Tổng vẫn đếm cả 2, nhưng perDifficulty chỉ có EASY
            assertEquals(2, stats.totalGames());
            assertEquals(1, stats.perDifficulty().size());
            assertTrue(stats.perDifficulty().containsKey(Difficulty.EASY));
        }
    }

    // =========================================================================
    // DifficultyStats.bestTimeFormatted()
    // =========================================================================

    @Nested
    @DisplayName("DifficultyStats.bestTimeFormatted()")
    class BestTimeFormattedTests {

        @Test
        @DisplayName("bestTimeMs = 0 → \"--:--\"")
        void zeroTime_ShouldShowDashes() {
            DifficultyStats ds = new DifficultyStats(1, 0, 0.0, 0, 0L);
            assertEquals("--:--", ds.bestTimeFormatted());
        }

        @Test
        @DisplayName("bestTimeMs = 65000 (1m05s) → \"01:05\"")
        void oneMinuteFiveSeconds_ShouldFormat() {
            DifficultyStats ds = new DifficultyStats(1, 1, 100.0, 1500, 65_000L);
            assertEquals("01:05", ds.bestTimeFormatted());
        }

        @Test
        @DisplayName("bestTimeMs = 30000 (30s) → \"00:30\"")
        void thirtySeconds_ShouldFormat() {
            DifficultyStats ds = new DifficultyStats(1, 1, 100.0, 1500, 30_000L);
            assertEquals("00:30", ds.bestTimeFormatted());
        }

        @Test
        @DisplayName("bestTimeMs = 600000 (10m) → \"10:00\"")
        void tenMinutes_ShouldFormat() {
            DifficultyStats ds = new DifficultyStats(1, 1, 100.0, 1500, 600_000L);
            assertEquals("10:00", ds.bestTimeFormatted());
        }
    }

    // =========================================================================
    // Tích hợp – Kịch bản thực tế
    // =========================================================================

    @Nested
    @DisplayName("Kịch bản tích hợp thực tế")
    class IntegrationTests {

        @Test
        @DisplayName("Player có 10 ván, 4 độ khó → tính toán đầy đủ")
        void fullScenario_ShouldComputeAllMetrics() {
            List<GameResult> results = List.of(
                    buildResult(Difficulty.EASY,   true,  8_000L,  1500),
                    buildResult(Difficulty.EASY,   true,  12_000L, 1300),
                    buildResult(Difficulty.EASY,   false, 3_000L,  0),
                    buildResult(Difficulty.MEDIUM, true,  25_000L, 1800),
                    buildResult(Difficulty.MEDIUM, true,  35_000L, 1600),
                    buildResult(Difficulty.MEDIUM, false, 10_000L, 0),
                    buildResult(Difficulty.HARD,   true,  80_000L, 2100),
                    buildResult(Difficulty.HARD,   false, 40_000L, 0),
                    buildResult(Difficulty.EXPERT, true,  150_000L, 2500),
                    buildResult(Difficulty.EXPERT, false, 60_000L,  0)
            );

            PlayerStats stats = service.computeStats(results);

            // 05.11.3a
            assertEquals(10, stats.totalGames());
            assertEquals(6, stats.totalWins());
            assertEquals(4, stats.totalLoses());

            // 05.11.3b
            assertEquals(60.0, stats.winRate(), 0.01);

            // 05.11.3c: avg of WIN scores = (1500+1300+1800+1600+2100+2500)/6
            double expectedAvg = (1500 + 1300 + 1800 + 1600 + 2100 + 2500) / 6.0;
            assertEquals(expectedAvg, stats.avgScore(), 0.01);

            // 05.11.3d
            Map<Difficulty, DifficultyStats> pd = stats.perDifficulty();
            assertEquals(4, pd.size());

            // EASY: best score = 1500, best time = 8000
            assertEquals(1500, pd.get(Difficulty.EASY).bestScore());
            assertEquals(8_000L, pd.get(Difficulty.EASY).bestTimeMs());

            // MEDIUM: best score = 1800, best time = 25000
            assertEquals(1800, pd.get(Difficulty.MEDIUM).bestScore());
            assertEquals(25_000L, pd.get(Difficulty.MEDIUM).bestTimeMs());

            // HARD: best score = 2100, best time = 80000
            assertEquals(2100, pd.get(Difficulty.HARD).bestScore());
            assertEquals(80_000L, pd.get(Difficulty.HARD).bestTimeMs());

            // EXPERT: best score = 2500, best time = 150000
            assertEquals(2500, pd.get(Difficulty.EXPERT).bestScore());
            assertEquals(150_000L, pd.get(Difficulty.EXPERT).bestTimeMs());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private GameResult buildResult(Difficulty difficulty, boolean won,
                                   long elapsedMs, int score) {
        GameResult r = new GameResult(
                "G" + System.nanoTime(),
                "player1",
                difficulty,
                won,
                elapsedMs,
                won ? 5 : 0,
                difficulty != null ? difficulty.getMines() : 0,
                LocalDateTime.now()
        );
        r.setScore(score);
        return r;
    }
}

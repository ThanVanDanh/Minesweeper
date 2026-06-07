package minesweeper.rankingV2;

import minesweeper.model.Achievement;
import minesweeper.model.GameResult;
import minesweeper.model.enums.Difficulty;
import minesweeper.repository.GameResultRepository;
import minesweeper.service.AchievementService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử UC04.3 / UC04.5 – Thống kê & Thành tựu V2
 * Nguồn: RankingHistoryController, AchievementService, Achievement, GameResult
 *
 * Ba nhóm:
 *   1. Win Streak hiện tại (ST_WS_*)
 *   2. Win Streak tối đa   (ST_MW_*)
 *   3. Streak ↔ Achievement (ST_AC_*)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC04.3/04.5 – Thống kê & Thành tựu V2")
public class StatisticsV2 {

    @Mock
    private GameResultRepository gameResultRepository;

    private AchievementService achievementService;

    @BeforeEach
    void setUp() {
        achievementService = new AchievementService();
    }

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private GameResult makeResult(boolean isWon, int minutesOffset) {
        GameResult r = new GameResult(
                "game-" + minutesOffset, "player01", Difficulty.EASY,
                isWon, 60_000L, 5, 10,
                BASE_TIME.plusMinutes(minutesOffset)
        );
        r.computeScore();
        return r;
    }

    /**
     * Tạo lịch sử theo thứ tự DESC (index 0 = ván MỚI NHẤT).
     * Khớp với cách DB trả về trong MySqlGameResultRepository.getPlayerHistory().
     */
    private List<GameResult> makeHistory(boolean... winsDesc) {
        List<GameResult> list = new ArrayList<>();
        for (int i = 0; i < winsDesc.length; i++) {
            list.add(makeResult(winsDesc[i], (winsDesc.length - i) * 5));
        }
        return list;
    }

    // ── Logic ánh xạ từ RankingHistoryController.loadPlayerStats() ───────────

    /**
     * Ánh xạ vòng lặp tính currentStreak (bước 04.3.7):
     * duyệt từ index 0 (mới nhất), break khi gặp thua.
     */
    private int computeCurrentStreak(List<GameResult> history) {
        int streak = 0;
        for (GameResult r : history) {
            if (r.isWon()) streak++;
            else break;
        }
        return streak;
    }

    /**
     * Ánh xạ vòng lặp tính maxStreak (bước 04.3.6):
     * duyệt ngược (ASC) để tìm chuỗi thắng dài nhất.
     */
    private int computeMaxStreak(List<GameResult> history) {
        int maxStreak = 0, tempStreak = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).isWon()) {
                tempStreak++;
                if (tempStreak > maxStreak) maxStreak = tempStreak;
            } else {
                tempStreak = 0;
            }
        }
        return maxStreak;
    }

    /** Ánh xạ statsWinStreak.setText(...) trong controller. */
    private String streakDisplay(int currentStreak) {
        return currentStreak > 0 ? currentStreak + " 🔥" : String.valueOf(currentStreak);
    }

    /** Ánh xạ statsMaxWinStreak.setText(...) trong controller. */
    private String maxStreakDisplay(int maxStreak) {
        return maxStreak > 0 ? maxStreak + " ⭐" : String.valueOf(maxStreak);
    }

    // =========================================================================
    // NHÓM 1 – Win Streak hiện tại (ST_WS)
    // =========================================================================

    @Test
    @DisplayName("ST_WS_01 – currentStreak: ván mới nhất thua → 0; chuỗi thắng đầu → đếm đúng")
    void stWs01_currentStreak_basicCases() {
        // Ván mới nhất thua → currentStreak = 0
        assertEquals(0, computeCurrentStreak(makeHistory(false, true, true)),
                "Ván mới nhất thua → currentStreak phải là 0");

        // 3 ván đầu thắng liên tiếp rồi thua → currentStreak = 3
        assertEquals(3, computeCurrentStreak(makeHistory(true, true, true, false, true)),
                "3 ván mới nhất thắng liên tiếp → currentStreak = 3");

        // Tất cả thắng → currentStreak = tổng ván
        assertEquals(5, computeCurrentStreak(makeHistory(true, true, true, true, true)),
                "Toàn thắng → currentStreak = tổng số ván");
    }

    @Test
    @DisplayName("ST_WS_02 – currentStreak: lịch sử rỗng, 1 ván, toàn thua → luôn = 0")
    void stWs02_currentStreak_zeroEdgeCases() {
        assertAll(
                () -> assertEquals(0, computeCurrentStreak(Collections.emptyList()),
                        "Lịch sử rỗng → 0"),
                () -> assertEquals(0, computeCurrentStreak(makeHistory(false)),
                        "1 ván thua → 0"),
                () -> assertEquals(0, computeCurrentStreak(makeHistory(false, false, false)),
                        "Toàn thua → 0")
        );
    }

    @Test
    @DisplayName("ST_WS_03 – Nhãn streakDisplay: > 0 kèm 🔥, = 0 hiện '0'")
    void stWs03_streakDisplay_labels() {
        assertEquals("3 🔥", streakDisplay(3), "streak=3 → '3 🔥'");
        assertEquals("0",    streakDisplay(0), "streak=0 → '0' (không có 🔥)");
    }

    // =========================================================================
    // NHÓM 2 – Win Streak tối đa (ST_MW)
    // =========================================================================

    @Test
    @DisplayName("ST_MW_01 – maxStreak: lịch sử rỗng / toàn thua → 0; toàn thắng → tổng ván")
    void stMw01_maxStreak_edgeAndAllWins() {
        assertAll(
                () -> assertEquals(0, computeMaxStreak(Collections.emptyList()),
                        "Rỗng → maxStreak = 0"),
                () -> assertEquals(0, computeMaxStreak(makeHistory(false, false, false)),
                        "Toàn thua → maxStreak = 0"),
                () -> assertEquals(4, computeMaxStreak(makeHistory(true, true, true, true)),
                        "Toàn thắng → maxStreak = tổng ván")
        );
    }

    @Test
    @DisplayName("ST_MW_02 – maxStreak chọn chuỗi dài nhất; có thể > currentStreak")
    void stMw02_maxStreak_picksLongestAndComparedToCurrent() {
        // DESC: W, L, W, W, W, L, W, W  →  ASC: W,W,L,W,W,W,L,W → max=3
        List<GameResult> multiStreak = makeHistory(true, false, true, true, true, false, true, true);
        assertEquals(3, computeMaxStreak(multiStreak), "Chuỗi dài nhất phải là 3");

        // DESC: W, L, W, W, W → currentStreak=1, maxStreak=3
        List<GameResult> h = makeHistory(true, false, true, true, true);
        int current = computeCurrentStreak(h);
        int max     = computeMaxStreak(h);
        assertTrue(max > current, "maxStreak phải lớn hơn currentStreak khi chuỗi cũ dài hơn");
        assertEquals(1, current);
        assertEquals(3, max);
    }

    @Test
    @DisplayName("ST_MW_03 – maxStreak = currentStreak khi chuỗi hiện tại là dài nhất")
    void stMw03_maxStreakEqualsCurrent_whenCurrentIsLongest() {
        // DESC: W, W, W, L, W → currentStreak=3, maxStreak=3
        List<GameResult> history = makeHistory(true, true, true, false, true);
        int current = computeCurrentStreak(history);
        int max     = computeMaxStreak(history);
        assertEquals(3, current, "currentStreak phải là 3");
        assertEquals(current, max, "maxStreak = currentStreak khi chuỗi hiện tại là dài nhất");
    }

    @Test
    @DisplayName("ST_MW_04 – Nhãn maxStreakDisplay: > 0 kèm ⭐, = 0 hiện '0'")
    void stMw04_maxStreakDisplay_labels() {
        assertEquals("5 ⭐", maxStreakDisplay(5), "maxStreak=5 → '5 ⭐'");
        assertEquals("1 ⭐", maxStreakDisplay(1), "maxStreak=1 → '1 ⭐' (biên dưới có ⭐)");
        assertEquals("0",    maxStreakDisplay(0), "maxStreak=0 → '0' (không có ⭐)");
    }

    // =========================================================================
    // NHÓM 3 – Streak ↔ Achievement (ST_AC)
    // =========================================================================

    @Test
    @DisplayName("ST_AC_01 – CO_DIEN: ≥2 thắng liên tiếp bất kỳ → mở; < 2 → chưa mở")
    void stAc01_coDien_unlockAndLock() {
        // Mở: 2 thắng liên tiếp ở giữa lịch sử
        List<GameResult> unlock = makeHistory(false, false, true, true, false);
        assertTrue(achievementService.evaluate(unlock).get(Achievement.CO_DIEN),
                "Chuỗi thắng 2 (ở giữa) → CO_DIEN phải mở");

        // Chưa mở: thắng rải rác không liên tiếp
        List<GameResult> locked = makeHistory(false, true);
        assertFalse(achievementService.evaluate(locked).get(Achievement.CO_DIEN),
                "Thắng 1 ván rồi thua → CO_DIEN chưa mở");
    }

    @Test
    @DisplayName("ST_AC_02 – TON_TRONG: ≥5 liên tiếp → mở; =4 hoặc rải rác 5 wins → chưa mở")
    void stAc02_tonTrong_unlockAndLock() {
        // Mở: đúng 5 thắng liên tiếp
        List<GameResult> unlock = makeHistory(true, true, true, true, true);
        Map<Achievement, Boolean> r1 = achievementService.evaluate(unlock);
        assertTrue(r1.get(Achievement.TON_TRONG), "5 thắng liên tiếp → TON_TRONG mở");
        // Kèm theo: CO_DIEN cũng phải mở
        assertTrue(r1.get(Achievement.CO_DIEN), "5 thắng → CO_DIEN cũng phải mở");

        // Chưa mở: chỉ 4 thắng liên tiếp
        List<GameResult> lock4 = makeHistory(true, true, true, true, false);
        assertFalse(achievementService.evaluate(lock4).get(Achievement.TON_TRONG),
                "Chuỗi chỉ 4 → TON_TRONG chưa mở");

        // Chưa mở: 5 wins rải rác không liên tiếp
        List<GameResult> lockScattered = makeHistory(true, false, true, false, true, false, true, false, true);
        assertFalse(achievementService.evaluate(lockScattered).get(Achievement.TON_TRONG),
                "5 wins rải rác → TON_TRONG chưa mở");
    }

    @Test
    @DisplayName("ST_AC_03 – NGU_LOM và lịch sử rỗng: thua ≥2 liên tiếp → mở; <2 → chưa mở; rỗng → tất cả khóa")
    void stAc03_nguLom_andEmptyHistory() {
        // Mở: thua 2 liên tiếp
        assertTrue(achievementService.evaluate(makeHistory(false, false)).get(Achievement.NGU_LOM),
                "Thua 2 liên tiếp → NGU_LOM mở");

        // Chưa mở: chỉ thua 1 ván
        assertFalse(achievementService.evaluate(makeHistory(true, false)).get(Achievement.NGU_LOM),
                "Chỉ thua 1 ván → NGU_LOM chưa mở");

        // Lịch sử rỗng → tất cả thành tựu chưa mở
        Map<Achievement, Boolean> empty = achievementService.evaluate(Collections.emptyList());
        assertAll(
                () -> assertFalse(empty.get(Achievement.CO_DIEN),   "CO_DIEN chưa mở"),
                () -> assertFalse(empty.get(Achievement.TON_TRONG), "TON_TRONG chưa mở"),
                () -> assertFalse(empty.get(Achievement.NGU_LOM),   "NGU_LOM chưa mở")
        );
    }
}
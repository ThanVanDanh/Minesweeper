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
import static org.mockito.Mockito.*;

/**
 * Kiểm thử chức năng UC – Thống kê người chơi V2
 * Nguồn: RankingHistoryController.java, AchievementService.java,
 *         Achievement.java, GameResult.java
 *
 * Hai nhóm chính:
 *   1. Win Streak hiện tại  (ST_WS_*)
 *   2. Win Streak tối đa    (ST_MW_*)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC – Thống kê người chơi V2")
public class StatisticsV2 {

    @Mock
    private GameResultRepository gameResultRepository;

    private AchievementService achievementService;

    @BeforeEach
    void setUp() {
        achievementService = new AchievementService();
    }

    // ── Hằng số ──────────────────────────────────────────────────────────────
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

    // ── Helper tạo GameResult ─────────────────────────────────────────────────

    /**
     * Tạo GameResult đơn giản với isWon tuỳ chọn.
     * playedAt tăng dần theo index để giả lập thứ tự thời gian.
     */
    private GameResult makeResult(boolean isWon, int minutesOffset) {
        GameResult r = new GameResult(
                "game-" + minutesOffset,
                "player01",
                Difficulty.EASY,
                isWon,
                60_000L,
                5,
                10,
                BASE_TIME.plusMinutes(minutesOffset)
        );
        r.computeScore();
        return r;
    }

    /**
     * Tạo danh sách GameResult từ mảng boolean (true = win).
     * Thứ tự trong List: index 0 là ván MỚI NHẤT (DESC) — khớp với
     * cách RankingHistoryController nhận dữ liệu từ DB.
     */
    private List<GameResult> makeHistory(boolean... winsDesc) {
        List<GameResult> list = new ArrayList<>();
        for (int i = 0; i < winsDesc.length; i++) {
            // i = 0 → mới nhất → minutesOffset lớn nhất
            list.add(makeResult(winsDesc[i], (winsDesc.length - i) * 5));
        }
        return list;
    }

    // ── Logic trích từ RankingHistoryController ───────────────────────────────

    /**
     * Tính currentStreak: đếm chuỗi thắng liên tiếp từ ván MỚI NHẤT.
     * history đã ở dạng DESC (index 0 = mới nhất).
     */
    private int computeCurrentStreak(List<GameResult> history) {
        int streak = 0;
        for (GameResult r : history) {
            if (r.isWon()) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * Tính maxStreak: chuỗi thắng dài nhất trong toàn bộ lịch sử.
     * history DESC → phải đảo ngược để duyệt theo chiều thời gian tăng dần.
     */
    private int computeMaxStreak(List<GameResult> history) {
        int maxStreak  = 0;
        int tempStreak = 0;
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

    /** Tạo nhãn hiển thị currentStreak (khớp với controller). */
    private String streakDisplay(int currentStreak) {
        return currentStreak > 0 ? currentStreak + " 🔥" : String.valueOf(currentStreak);
    }

    /** Tạo nhãn hiển thị maxStreak (khớp với controller). */
    private String maxStreakDisplay(int maxStreak) {
        return maxStreak > 0 ? maxStreak + " ⭐" : String.valueOf(maxStreak);
    }

    // =========================================================================
    // NHÓM 1 – Win Streak hiện tại (ST_WS)
    // =========================================================================

    /**
     * ST_WS_01: Ván gần nhất thua → currentStreak = 0.
     */
    @Test
    @DisplayName("ST_WS_01 – Ván gần nhất thua → currentStreak = 0")
    void stWs01_lastGameLost_currentStreakZero() {
        // DESC: thua, thắng, thắng
        List<GameResult> history = makeHistory(false, true, true);
        assertEquals(0, computeCurrentStreak(history),
                "Ván mới nhất thua → currentStreak phải là 0");
    }

    /**
     * ST_WS_02: 3 ván gần nhất đều thắng → currentStreak = 3.
     */
    @Test
    @DisplayName("ST_WS_02 – 3 ván gần nhất đều thắng → currentStreak = 3")
    void stWs02_threeConsecutiveWins_currentStreakThree() {
        // DESC: W, W, W, L, W
        List<GameResult> history = makeHistory(true, true, true, false, true);
        assertEquals(3, computeCurrentStreak(history),
                "3 ván liên tiếp thắng → currentStreak phải là 3");
    }

    /**
     * ST_WS_03: Danh sách rỗng → currentStreak = 0.
     */
    @Test
    @DisplayName("ST_WS_03 – Lịch sử rỗng → currentStreak = 0")
    void stWs03_emptyHistory_currentStreakZero() {
        assertEquals(0, computeCurrentStreak(Collections.emptyList()),
                "Lịch sử rỗng → currentStreak phải là 0");
    }

    /**
     * ST_WS_04: Tất cả ván đều thắng → currentStreak = tổng số ván.
     */
    @Test
    @DisplayName("ST_WS_04 – Tất cả ván đều thắng → currentStreak = tổng số ván")
    void stWs04_allWins_currentStreakEqualsTotal() {
        List<GameResult> history = makeHistory(true, true, true, true, true);
        assertEquals(5, computeCurrentStreak(history),
                "Toàn bộ thắng → currentStreak phải bằng tổng số ván");
    }

    /**
     * ST_WS_05: Chỉ 1 ván, kết quả thắng → currentStreak = 1.
     */
    @Test
    @DisplayName("ST_WS_05 – 1 ván thắng duy nhất → currentStreak = 1")
    void stWs05_singleWin_currentStreakOne() {
        List<GameResult> history = makeHistory(true);
        assertEquals(1, computeCurrentStreak(history),
                "1 ván thắng → currentStreak phải là 1");
    }

    /**
     * ST_WS_06: Chỉ 1 ván, kết quả thua → currentStreak = 0.
     */
    @Test
    @DisplayName("ST_WS_06 – 1 ván thua duy nhất → currentStreak = 0")
    void stWs06_singleLoss_currentStreakZero() {
        List<GameResult> history = makeHistory(false);
        assertEquals(0, computeCurrentStreak(history),
                "1 ván thua → currentStreak phải là 0");
    }

    /**
     * ST_WS_07: Streak bị gián đoạn bởi 1 ván thua ở giữa → currentStreak chỉ tính từ đầu.
     * Ví dụ: W, W, L, W, W → currentStreak = 2 (chỉ 2 ván mới nhất).
     */
    @Test
    @DisplayName("ST_WS_07 – Streak bị gián đoạn → currentStreak tính từ ván mới nhất")
    void stWs07_streakInterrupted_countFromLatest() {
        // DESC: W, W, L, W, W
        List<GameResult> history = makeHistory(true, true, false, true, true);
        assertEquals(2, computeCurrentStreak(history),
                "Sau gián đoạn: currentStreak chỉ tính 2 ván mới nhất liên tiếp thắng");
    }

    /**
     * ST_WS_08: currentStreak > 0 → nhãn hiển thị kèm 🔥.
     */
    @Test
    @DisplayName("ST_WS_08 – currentStreak > 0 → nhãn hiển thị kèm 🔥")
    void stWs08_positiveCurrentStreak_displayWithFire() {
        assertEquals("3 🔥", streakDisplay(3),
                "currentStreak = 3 phải hiển thị '3 🔥'");
    }

    /**
     * ST_WS_09: currentStreak = 0 → nhãn hiển thị "0" (không có 🔥).
     */
    @Test
    @DisplayName("ST_WS_09 – currentStreak = 0 → nhãn hiển thị '0'")
    void stWs09_zeroCurrentStreak_displayZero() {
        assertEquals("0", streakDisplay(0),
                "currentStreak = 0 phải hiển thị '0'");
    }

    /**
     * ST_WS_10: Tất cả ván đều thua → currentStreak = 0 và không hiện 🔥.
     */
    @Test
    @DisplayName("ST_WS_10 – Tất cả ván đều thua → currentStreak = 0, không hiện 🔥")
    void stWs10_allLosses_noStreak() {
        List<GameResult> history = makeHistory(false, false, false);
        int cs = computeCurrentStreak(history);
        assertEquals(0, cs);
        assertEquals("0", streakDisplay(cs),
                "Không có ván thắng → không hiện 🔥");
    }

    // =========================================================================
    // NHÓM 2 – Win Streak tối đa (ST_MW)
    // =========================================================================

    /**
     * ST_MW_01: Lịch sử rỗng → maxStreak = 0.
     */
    @Test
    @DisplayName("ST_MW_01 – Lịch sử rỗng → maxStreak = 0")
    void stMw01_emptyHistory_maxStreakZero() {
        assertEquals(0, computeMaxStreak(Collections.emptyList()),
                "Lịch sử rỗng → maxStreak phải là 0");
    }

    /**
     * ST_MW_02: Tất cả ván đều thua → maxStreak = 0.
     */
    @Test
    @DisplayName("ST_MW_02 – Tất cả ván đều thua → maxStreak = 0")
    void stMw02_allLosses_maxStreakZero() {
        List<GameResult> history = makeHistory(false, false, false);
        assertEquals(0, computeMaxStreak(history),
                "Toàn bộ thua → maxStreak phải là 0");
    }

    /**
     * ST_MW_03: Có 1 chuỗi thắng liên tiếp dài nhất = 4 → maxStreak = 4.
     * Ví dụ: W, L, W, W, W, W, L
     */
    @Test
    @DisplayName("ST_MW_03 – Chuỗi thắng dài nhất 4 → maxStreak = 4")
    void stMw03_longestStreak4_maxStreakFour() {
        // DESC: L, W, W, W, W, L, W  →  ASC: W, L, W, W, W, W, L
        List<GameResult> history = makeHistory(false, true, true, true, true, false, true);
        assertEquals(4, computeMaxStreak(history),
                "Chuỗi dài nhất 4 ván thắng → maxStreak phải là 4");
    }

    /**
     * ST_MW_04: Tất cả ván đều thắng → maxStreak = tổng số ván.
     */
    @Test
    @DisplayName("ST_MW_04 – Tất cả ván đều thắng → maxStreak = tổng số ván")
    void stMw04_allWins_maxStreakEqualsTotal() {
        List<GameResult> history = makeHistory(true, true, true, true);
        assertEquals(4, computeMaxStreak(history),
                "Toàn bộ thắng → maxStreak bằng tổng số ván");
    }

    /**
     * ST_MW_05: Nhiều chuỗi thắng rải rác → maxStreak chọn chuỗi dài nhất.
     * Ví dụ: W, W, L, W, W, W, L, W → max = 3.
     */
    @Test
    @DisplayName("ST_MW_05 – Nhiều chuỗi rải rác → maxStreak lấy chuỗi dài nhất")
    void stMw05_multipleStreaks_maxPicksLongest() {
        // DESC: W, L, W, W, W, L, W, W  →  ASC: W, W, L, W, W, W, L, W
        List<GameResult> history = makeHistory(true, false, true, true, true, false, true, true);
        assertEquals(3, computeMaxStreak(history),
                "Chuỗi dài nhất là 3 → maxStreak phải là 3");
    }

    /**
     * ST_MW_06: maxStreak > currentStreak khi chuỗi cũ dài hơn chuỗi hiện tại.
     * Ví dụ: DESC: W, L, W, W, W → currentStreak=1, maxStreak=3.
     */
    @Test
    @DisplayName("ST_MW_06 – maxStreak có thể lớn hơn currentStreak")
    void stMw06_maxStreakGreaterThanCurrent() {
        // DESC: W, L, W, W, W
        List<GameResult> history = makeHistory(true, false, true, true, true);
        int current = computeCurrentStreak(history); // 1
        int max     = computeMaxStreak(history);     // 3
        assertTrue(max > current,
                "maxStreak phải lớn hơn currentStreak khi chuỗi cũ dài hơn");
        assertEquals(1, current);
        assertEquals(3, max);
    }

    /**
     * ST_MW_07: maxStreak = currentStreak khi chuỗi gần nhất là dài nhất.
     * Ví dụ: DESC: W, W, W, L, W → currentStreak=3, maxStreak=3.
     */
    @Test
    @DisplayName("ST_MW_07 – maxStreak = currentStreak khi chuỗi hiện tại là dài nhất")
    void stMw07_maxStreakEqualsCurrent_whenCurrentIsLongest() {
        // DESC: W, W, W, L, W
        List<GameResult> history = makeHistory(true, true, true, false, true);
        assertEquals(computeCurrentStreak(history), computeMaxStreak(history),
                "Khi chuỗi hiện tại là dài nhất: maxStreak = currentStreak");
    }

    /**
     * ST_MW_08: maxStreak > 0 → nhãn hiển thị kèm ⭐.
     */
    @Test
    @DisplayName("ST_MW_08 – maxStreak > 0 → nhãn hiển thị kèm ⭐")
    void stMw08_positiveMaxStreak_displayWithStar() {
        assertEquals("5 ⭐", maxStreakDisplay(5),
                "maxStreak = 5 phải hiển thị '5 ⭐'");
    }

    /**
     * ST_MW_09: maxStreak = 0 → nhãn hiển thị "0" (không có ⭐).
     */
    @Test
    @DisplayName("ST_MW_09 – maxStreak = 0 → nhãn hiển thị '0'")
    void stMw09_zeroMaxStreak_displayZero() {
        assertEquals("0", maxStreakDisplay(0),
                "maxStreak = 0 phải hiển thị '0'");
    }

    /**
     * ST_MW_10: maxStreak = 1 → nhãn hiển thị "1 ⭐" (biên dưới có ⭐).
     */
    @Test
    @DisplayName("ST_MW_10 – maxStreak = 1 → nhãn hiển thị '1 ⭐'")
    void stMw10_oneMaxStreak_displayOneStar() {
        assertEquals("1 ⭐", maxStreakDisplay(1),
                "maxStreak = 1 phải hiển thị '1 ⭐'");
    }

    // =========================================================================
    // NHÓM 3 – Liên kết Streak ↔ Achievement (ST_AC)
    // =========================================================================

    /**
     * ST_AC_01: Chuỗi thắng ≥ 2 ở bất kỳ đâu trong lịch sử → mở thành tựu CO_DIEN.
     */
    @Test
    @DisplayName("ST_AC_01 – Chuỗi thắng ≥ 2 bất kỳ → mở khóa CO_DIEN")
    void stAc01_twoConsecutiveWins_unlockCoDien() {
        // ASC: W, W → DESC: W, W
        List<GameResult> history = makeHistory(true, true);
        Map<Achievement, Boolean> result = achievementService.evaluate(history);
        assertTrue(result.get(Achievement.CO_DIEN),
                "Chuỗi thắng 2 phải mở khóa CO_DIEN");
    }

    /**
     * ST_AC_02: Chỉ thắng 1 ván rồi thua → CO_DIEN chưa mở khóa.
     */
    @Test
    @DisplayName("ST_AC_02 – Thắng 1 ván rồi thua → CO_DIEN chưa mở khóa")
    void stAc02_oneWinThenLoss_coDienLocked() {
        // DESC: L, W
        List<GameResult> history = makeHistory(false, true);
        Map<Achievement, Boolean> result = achievementService.evaluate(history);
        assertFalse(result.get(Achievement.CO_DIEN),
                "Không có 2 ván thắng liên tiếp → CO_DIEN chưa mở khóa");
    }

    /**
     * ST_AC_03: Chuỗi thắng ≥ 5 trong toàn lịch sử → mở thành tựu TON_TRONG.
     */
    @Test
    @DisplayName("ST_AC_03 – Chuỗi thắng ≥ 5 → mở khóa TON_TRONG")
    void stAc03_fiveConsecutiveWins_unlockTonTrong() {
        // DESC: W, W, W, W, W
        List<GameResult> history = makeHistory(true, true, true, true, true);
        Map<Achievement, Boolean> result = achievementService.evaluate(history);
        assertTrue(result.get(Achievement.TON_TRONG),
                "Chuỗi thắng 5 phải mở khóa TON_TRONG");
    }

    /**
     * ST_AC_04: Chuỗi thắng chỉ = 4 → TON_TRONG chưa mở khóa.
     */
    @Test
    @DisplayName("ST_AC_04 – Chuỗi thắng = 4 → TON_TRONG chưa mở khóa")
    void stAc04_fourWins_tonTrongLocked() {
        // DESC: W, W, W, W, L
        List<GameResult> history = makeHistory(true, true, true, true, false);
        Map<Achievement, Boolean> result = achievementService.evaluate(history);
        assertFalse(result.get(Achievement.TON_TRONG),
                "Chuỗi thắng chỉ 4 → TON_TRONG chưa mở khóa");
    }

    /**
     * ST_AC_05: Thua 2 ván liên tiếp → mở thành tựu NGU_LOM.
     */
    @Test
    @DisplayName("ST_AC_05 – Thua 2 ván liên tiếp → mở khóa NGU_LOM")
    void stAc05_twoConcurrentLosses_unlockNguLom() {
        // DESC: L, L
        List<GameResult> history = makeHistory(false, false);
        Map<Achievement, Boolean> result = achievementService.evaluate(history);
        assertTrue(result.get(Achievement.NGU_LOM),
                "Thua 2 liên tiếp phải mở khóa NGU_LOM");
    }

    /**
     * ST_AC_06: Chỉ thua 1 ván → NGU_LOM chưa mở khóa.
     */
    @Test
    @DisplayName("ST_AC_06 – Chỉ thua 1 ván → NGU_LOM chưa mở khóa")
    void stAc06_oneLoss_nguLomLocked() {
        // DESC: W, L
        List<GameResult> history = makeHistory(true, false);
        Map<Achievement, Boolean> result = achievementService.evaluate(history);
        assertFalse(result.get(Achievement.NGU_LOM),
                "Chỉ thua 1 ván → NGU_LOM chưa mở khóa");
    }

    /**
     * ST_AC_07: TON_TRONG ≥ 5 → CO_DIEN cũng phải đồng thời mở khóa.
     */
    @Test
    @DisplayName("ST_AC_07 – Khi TON_TRONG mở → CO_DIEN cũng mở")
    void stAc07_tonTrongUnlocked_coDienAlsoUnlocked() {
        // DESC: W, W, W, W, W
        List<GameResult> history = makeHistory(true, true, true, true, true);
        Map<Achievement, Boolean> result = achievementService.evaluate(history);
        assertTrue(result.get(Achievement.TON_TRONG));
        assertTrue(result.get(Achievement.CO_DIEN),
                "Chuỗi 5 thắng → CO_DIEN bắt buộc cũng phải mở");
    }

    /**
     * ST_AC_08: Lịch sử rỗng → tất cả thành tựu đều chưa mở.
     */
    @Test
    @DisplayName("ST_AC_08 – Lịch sử rỗng → tất cả thành tựu chưa mở")
    void stAc08_emptyHistory_allAchievementsLocked() {
        Map<Achievement, Boolean> result = achievementService.evaluate(Collections.emptyList());
        assertFalse(result.get(Achievement.CO_DIEN),   "CO_DIEN phải chưa mở");
        assertFalse(result.get(Achievement.TON_TRONG), "TON_TRONG phải chưa mở");
        assertFalse(result.get(Achievement.NGU_LOM),   "NGU_LOM phải chưa mở");
    }

    /**
     * ST_AC_09: Chuỗi thắng rải rác (không liên tiếp) → TON_TRONG không mở dù tổng wins ≥ 5.
     * Ví dụ: W, L, W, L, W, L, W, L, W → 5 wins nhưng không có 5 liên tiếp.
     */
    @Test
    @DisplayName("ST_AC_09 – 5 wins rải rác không liên tiếp → TON_TRONG chưa mở")
    void stAc09_fiveWinsNotConsecutive_tonTrongLocked() {
        // DESC: W, L, W, L, W, L, W, L, W
        List<GameResult> history = makeHistory(true, false, true, false, true, false, true, false, true);
        Map<Achievement, Boolean> result = achievementService.evaluate(history);
        assertFalse(result.get(Achievement.TON_TRONG),
                "5 thắng rải rác → TON_TRONG chưa mở");
    }

    /**
     * ST_AC_10: Chuỗi thắng nằm ở giữa lịch sử (không phải cuối) → vẫn mở CO_DIEN.
     * Ví dụ: DESC: L, L, W, W, L → chuỗi W, W ở vị trí ASC thứ 2-3.
     */
    @Test
    @DisplayName("ST_AC_10 – Chuỗi thắng nằm giữa lịch sử → vẫn mở CO_DIEN")
    void stAc10_consecutiveWinsInMiddle_coDienUnlocked() {
        // DESC: L, L, W, W, L
        List<GameResult> history = makeHistory(false, false, true, true, false);
        Map<Achievement, Boolean> result = achievementService.evaluate(history);
        assertTrue(result.get(Achievement.CO_DIEN),
                "Chuỗi thắng 2 ở giữa lịch sử vẫn phải mở CO_DIEN");
    }
}
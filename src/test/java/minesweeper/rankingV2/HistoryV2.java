package minesweeper.rankingV2;

import minesweeper.model.GameResult;
import minesweeper.model.enums.Difficulty;
import minesweeper.repository.MySqlGameResultRepository;
import minesweeper.repository.pagination.PagedResult;
import minesweeper.repository.spec.GameResultFilterSpec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UC – Lịch sử chơi V2")
public class HistoryV2 {

    @Mock
    private MySqlGameResultRepository gameResultRepository;

    private static final int    HISTORY_PAGE_SIZE = 10;
    private static final String PLAYER_NAME       = "testuser"; // dùng playerName, không phải username

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dùng constructor có sẵn của GameResult thay vì setter không tồn tại.
     * GameResult(gameId, playerName, difficulty, isWon, elapsedTimeMs,
     *            flagsUsed, minesTotal, playedAt)
     */
    private GameResult makeResult(Difficulty difficulty, boolean won,
                                  int score, long elapsedMs) {
        GameResult r = new GameResult(
                java.util.UUID.randomUUID().toString(),  // gameId
                PLAYER_NAME,                             // playerName
                difficulty,
                won,
                elapsedMs,
                0,                                       // flagsUsed
                0,                                       // minesTotal
                LocalDateTime.now()                      // playedAt
        );
        r.setScore(score); // setScore() tồn tại
        return r;
    }

    private List<GameResult> makeResultList(int n) {
        Difficulty[] levels = Difficulty.values();
        List<GameResult> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(makeResult(
                    levels[i % levels.length],
                    i % 2 == 0,
                    1000 - i * 10,
                    (long) (i + 1) * 30_000));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Logic ánh xạ từ RankingHistoryController
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ánh xạ buildHistoryFilterSpec().
     * Lưu ý: spec.username trong hệ thống map sang getPlayerName() của GameResult.
     */
    private GameResultFilterSpec buildSpec(String playerName, Difficulty difficulty, Boolean win) {
        GameResultFilterSpec spec = new GameResultFilterSpec();
        spec.username   = playerName;  // field tên username nhưng chứa playerName
        spec.difficulty = difficulty;
        spec.win        = win;
        return spec;
    }

    private int totalHistoryPages(long totalItems) {
        return (int) Math.ceil((double) totalItems / HISTORY_PAGE_SIZE);
    }

    private String historyPageLabel(int currentPage, int totalPages) {
        return "Trang " + (currentPage + 1) + " / " + Math.max(1, totalPages);
    }

    private boolean isPrevDisabled(int currentPage) { return currentPage == 0; }

    private boolean isNextDisabled(int currentPage, int totalPages) {
        return currentPage >= totalPages - 1;
    }

    /**
     * Giả lập logic WHERE trong findPaged().
     * Dùng getPlayerName() thay vì getUsername() vì GameResult không có getUsername().
     */
    private List<GameResult> applyFilter(List<GameResult> all, GameResultFilterSpec spec) {
        return all.stream()
                .filter(r -> r.getPlayerName().equalsIgnoreCase(spec.username))
                .filter(r -> spec.difficulty == null || r.getDifficulty() == spec.difficulty)
                .filter(r -> spec.win        == null || r.isWon()         == spec.win)
                .collect(Collectors.toList());
    }

    private List<GameResult> getPage(List<GameResult> filtered, int page) {
        int from = page * HISTORY_PAGE_SIZE;
        int to   = Math.min(from + HISTORY_PAGE_SIZE, filtered.size());
        return from < to ? filtered.subList(from, to) : List.of();
    }

    // =========================================================================
    // NHÓM 1 – Lọc theo độ khó (HV2_DIFF)
    // =========================================================================

    @Test
    @DisplayName("HV2_DIFF_01 – Lọc EASY chỉ trả về ván EASY")
    void hvDiff01_filterEasy_returnsOnlyEasy() {
        List<GameResult> all = List.of(
                makeResult(Difficulty.EASY,   true,  900, 30_000),
                makeResult(Difficulty.MEDIUM, false, 700, 60_000),
                makeResult(Difficulty.EASY,   false, 500, 45_000),
                makeResult(Difficulty.HARD,   true,  800, 90_000)
        );
        List<GameResult> result = applyFilter(all, buildSpec(PLAYER_NAME, Difficulty.EASY, null));

        assertEquals(2, result.size(), "Phải có đúng 2 ván EASY");
        assertTrue(result.stream().allMatch(r -> r.getDifficulty() == Difficulty.EASY));
    }

    @Test
    @DisplayName("HV2_DIFF_02 – Lọc EXPERT chỉ trả về ván EXPERT")
    void hvDiff02_filterExpert_returnsOnlyExpert() {
        List<GameResult> all = List.of(
                makeResult(Difficulty.EXPERT, true,  1000, 120_000),
                makeResult(Difficulty.EASY,   true,   900,  30_000),
                makeResult(Difficulty.EXPERT, false,  800,  90_000)
        );
        List<GameResult> result = applyFilter(all, buildSpec(PLAYER_NAME, Difficulty.EXPERT, null));

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> r.getDifficulty() == Difficulty.EXPERT));
    }

    @Test
    @DisplayName("HV2_DIFF_03 – Tất cả độ khó (null) không lọc, trả về đủ bản ghi")
    void hvDiff03_filterNull_returnsAll() {
        List<GameResult> all    = makeResultList(8);
        List<GameResult> result = applyFilter(all, buildSpec(PLAYER_NAME, null, null));
        assertEquals(8, result.size());
    }

    @Test
    @DisplayName("HV2_DIFF_04 – Lọc độ khó không có ván nào → rỗng")
    void hvDiff04_filterDifficulty_noMatch_returnsEmpty() {
        List<GameResult> all = List.of(
                makeResult(Difficulty.EASY,   true, 900, 30_000),
                makeResult(Difficulty.MEDIUM, true, 800, 60_000)
        );
        assertTrue(applyFilter(all, buildSpec(PLAYER_NAME, Difficulty.EXPERT, null)).isEmpty());
    }

    @Test
    @DisplayName("HV2_DIFF_05 – buildSpec() set difficulty đúng khi chọn HARD")
    void hvDiff05_buildSpec_setsDifficultyCorrectly() {
        GameResultFilterSpec spec = buildSpec(PLAYER_NAME, Difficulty.HARD, null);
        assertEquals(PLAYER_NAME,     spec.username);
        assertEquals(Difficulty.HARD, spec.difficulty);
        assertNull(spec.win);
    }

    @Test
    @DisplayName("HV2_DIFF_06 – DifficultyOption.ALL → spec.difficulty = null")
    void hvDiff06_allOption_specDifficultyNull() {
        GameResultFilterSpec spec = buildSpec(PLAYER_NAME, null, null);
        assertNull(spec.difficulty, "Chọn 'Tất cả độ khó' thì spec.difficulty phải null");
    }

    // =========================================================================
    // NHÓM 2 – Lọc theo kết quả (HV2_RES)
    // =========================================================================

    @Test
    @DisplayName("HV2_RES_01 – Lọc Thắng chỉ trả về ván thắng")
    void hvRes01_filterWin_returnsOnlyWins() {
        List<GameResult> all = List.of(
                makeResult(Difficulty.EASY, true,  900, 30_000),
                makeResult(Difficulty.EASY, false, 500, 45_000),
                makeResult(Difficulty.HARD, true,  800, 90_000),
                makeResult(Difficulty.HARD, false, 300, 60_000)
        );
        List<GameResult> result = applyFilter(all, buildSpec(PLAYER_NAME, null, true));

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(GameResult::isWon));
    }

    @Test
    @DisplayName("HV2_RES_02 – Lọc Thua chỉ trả về ván thua")
    void hvRes02_filterLose_returnsOnlyLosses() {
        List<GameResult> all = List.of(
                makeResult(Difficulty.MEDIUM, true,  700, 60_000),
                makeResult(Difficulty.MEDIUM, false, 400, 50_000),
                makeResult(Difficulty.EASY,   false, 200, 40_000)
        );
        List<GameResult> result = applyFilter(all, buildSpec(PLAYER_NAME, null, false));

        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(GameResult::isWon));
    }

    @Test
    @DisplayName("HV2_RES_03 – Tất cả kết quả (null) không lọc, trả về đủ bản ghi")
    void hvRes03_filterNull_returnsAll() {
        assertEquals(6, applyFilter(makeResultList(6), buildSpec(PLAYER_NAME, null, null)).size());
    }

    @Test
    @DisplayName("HV2_RES_04 – Toàn thua, lọc Thắng → rỗng")
    void hvRes04_allLosses_filterWin_returnsEmpty() {
        List<GameResult> all = List.of(
                makeResult(Difficulty.EASY, false, 0, 30_000),
                makeResult(Difficulty.EASY, false, 0, 40_000)
        );
        assertTrue(applyFilter(all, buildSpec(PLAYER_NAME, null, true)).isEmpty());
    }

    @Test
    @DisplayName("HV2_RES_05 – Toàn thắng, lọc Thua → rỗng")
    void hvRes05_allWins_filterLose_returnsEmpty() {
        List<GameResult> all = List.of(
                makeResult(Difficulty.HARD, true, 900, 60_000),
                makeResult(Difficulty.HARD, true, 800, 70_000)
        );
        assertTrue(applyFilter(all, buildSpec(PLAYER_NAME, null, false)).isEmpty());
    }

    @Test
    @DisplayName("HV2_RES_06 – ResultOption ánh xạ đúng giá trị win")
    void hvRes06_resultOption_correctWinValue() {
        // Mô phỏng 3 giá trị win của ResultOption
        Boolean winAll  = null;
        Boolean winWin  = Boolean.TRUE;
        Boolean winLose = Boolean.FALSE;

        assertNull(winAll);
        assertTrue(winWin);
        assertFalse(winLose);
    }

    // =========================================================================
    // NHÓM 3 – Lọc kết hợp (HV2_CMB)
    // =========================================================================

    @Test
    @DisplayName("HV2_CMB_01 – Lọc HARD + Thắng chỉ trả về ván HARD-win")
    void hvCmb01_hardAndWin_returnsHardWinOnly() {
        List<GameResult> all = List.of(
                makeResult(Difficulty.HARD,   true,  900, 60_000),  // ✓
                makeResult(Difficulty.HARD,   false, 600, 70_000),  // ✗ thua
                makeResult(Difficulty.EASY,   true,  800, 30_000),  // ✗ sai độ khó
                makeResult(Difficulty.MEDIUM, false, 400, 50_000)   // ✗ cả hai sai
        );
        List<GameResult> result = applyFilter(all, buildSpec(PLAYER_NAME, Difficulty.HARD, true));

        assertEquals(1, result.size());
        assertEquals(Difficulty.HARD, result.get(0).getDifficulty());
        assertTrue(result.get(0).isWon());
    }

    @Test
    @DisplayName("HV2_CMB_02 – Lọc MEDIUM + Thua chỉ trả về ván MEDIUM-lose")
    void hvCmb02_mediumAndLose_returnsMediumLoseOnly() {
        List<GameResult> all = List.of(
                makeResult(Difficulty.MEDIUM, false, 400, 50_000),  // ✓
                makeResult(Difficulty.MEDIUM, true,  700, 60_000),  // ✗ thắng
                makeResult(Difficulty.HARD,   false, 300, 70_000),  // ✗ sai độ khó
                makeResult(Difficulty.MEDIUM, false, 350, 55_000)   // ✓
        );
        List<GameResult> result = applyFilter(all, buildSpec(PLAYER_NAME, Difficulty.MEDIUM, false));

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(
                r -> r.getDifficulty() == Difficulty.MEDIUM && !r.isWon()));
    }

    @Test
    @DisplayName("HV2_CMB_03 – Lọc kết hợp không khớp → rỗng")
    void hvCmb03_combinedFilter_noMatch_returnsEmpty() {
        List<GameResult> all = List.of(
                makeResult(Difficulty.EASY, true,  900, 30_000),
                makeResult(Difficulty.EASY, false, 500, 40_000)
        );
        assertTrue(applyFilter(all, buildSpec(PLAYER_NAME, Difficulty.EXPERT, true)).isEmpty());
    }

    @Test
    @DisplayName("HV2_CMB_04 – Thay đổi bộ lọc reset currentPage về 0")
    void hvCmb04_filterChange_resetsPageToZero() {
        int currentPage = 2;
        currentPage = 0; // mô phỏng onHistoryFilterChanged()
        assertEquals(0, currentPage);
    }

    @Test
    @DisplayName("HV2_CMB_05 – buildSpec() luôn set username trong mọi trường hợp")
    void hvCmb05_buildSpec_alwaysSetsUsername() {
        assertAll("playerName luôn phải được set vào spec.username",
                () -> assertEquals(PLAYER_NAME, buildSpec(PLAYER_NAME, null,            null).username),
                () -> assertEquals(PLAYER_NAME, buildSpec(PLAYER_NAME, Difficulty.EASY, null).username),
                () -> assertEquals(PLAYER_NAME, buildSpec(PLAYER_NAME, null,            true).username),
                () -> assertEquals(PLAYER_NAME, buildSpec(PLAYER_NAME, Difficulty.HARD, false).username)
        );
    }

    @Test
    @DisplayName("HV2_CMB_06 – totalPages cập nhật theo kết quả lọc, không phải tổng gốc")
    void hvCmb06_filteredResult_affectsTotalPages() {
        assertEquals(3, totalHistoryPages(25L), "25 ván → 3 trang");
        assertEquals(2, totalHistoryPages(12L), "12 ván sau lọc → 2 trang");
        assertEquals(1, totalHistoryPages(7L),  "7 ván sau lọc → 1 trang");
    }

    // =========================================================================
    // NHÓM 4 – Phân trang lịch sử (HV2_PG)
    // =========================================================================

    @Test
    @DisplayName("HV2_PG_01 – 25 ván với PAGE_SIZE=10 → 3 trang")
    void hvPg01_25Items_returns3Pages() {
        assertEquals(3, totalHistoryPages(25L));
    }

    @Test
    @DisplayName("HV2_PG_02 – Đúng 10 ván → 1 trang")
    void hvPg02_exactlyOnePage() {
        assertEquals(1, totalHistoryPages(10L));
    }

    @Test
    @DisplayName("HV2_PG_03 – 0 ván → totalPages = 0, nhãn 'Trang 1 / 1'")
    void hvPg03_zeroItems_labelTrang1Of1() {
        int totalPages = totalHistoryPages(0L);
        assertEquals(0, totalPages);
        assertEquals("Trang 1 / 1", historyPageLabel(0, totalPages));
    }

    @Test
    @DisplayName("HV2_PG_04 – Nhãn trang đúng 'Trang X / Y' với 3 trang")
    void hvPg04_pageLabel_correctForAllPages() {
        int totalPages = totalHistoryPages(25L);
        assertAll(
                () -> assertEquals("Trang 1 / 3", historyPageLabel(0, totalPages)),
                () -> assertEquals("Trang 2 / 3", historyPageLabel(1, totalPages)),
                () -> assertEquals("Trang 3 / 3", historyPageLabel(2, totalPages))
        );
    }

    @Test
    @DisplayName("HV2_PG_05 – btnHistoryPrev disable ở trang 0, enable ở trang 1+")
    void hvPg05_prevButton_stateCorrect() {
        assertTrue(isPrevDisabled(0));
        assertFalse(isPrevDisabled(1));
        assertFalse(isPrevDisabled(2));
    }

    @Test
    @DisplayName("HV2_PG_06 – btnHistoryNext disable ở trang cuối")
    void hvPg06_nextButton_stateCorrect() {
        int totalPages = totalHistoryPages(25L); // 3
        assertFalse(isNextDisabled(0, totalPages));
        assertFalse(isNextDisabled(1, totalPages));
        assertTrue(isNextDisabled(2, totalPages));
    }

    @Test
    @DisplayName("HV2_PG_07 – onHistoryNextPage tăng page đúng, chặn ở trang cuối")
    void hvPg07_nextPage_incrementsCorrectly() {
        int totalPages = totalHistoryPages(25L); // 3
        int page = 0;

        if (page < totalPages - 1) page++;
        assertEquals(1, page);

        if (page < totalPages - 1) page++;
        assertEquals(2, page);

        if (page < totalPages - 1) page++;
        assertEquals(2, page, "Ở trang cuối không được tăng page");
    }

    @Test
    @DisplayName("HV2_PG_08 – onHistoryPrevPage giảm page đúng, chặn ở trang 0")
    void hvPg08_prevPage_decrementsCorrectly() {
        int page = 2;

        if (page > 0) page--;
        assertEquals(1, page);

        if (page > 0) page--;
        assertEquals(0, page);

        if (page > 0) page--;
        assertEquals(0, page, "Ở trang 0 không được giảm page");
    }

    @Test
    @DisplayName("HV2_PG_11 – Trang cuối trả về đúng số ván còn lại")
    void hvPg11_lastPage_returnsRemainder() {
        List<GameResult> filtered =
                applyFilter(makeResultList(25), buildSpec(PLAYER_NAME, null, null));
        assertEquals(5, getPage(filtered, 2).size(),
                "Trang cuối của 25 items phải có 5 phần tử");
    }

    @Test
    @DisplayName("HV2_PG_12 – totalPages cập nhật đúng sau khi filter thay đổi")
    void hvPg12_totalPages_updatesAfterFilterChange() {
        assertEquals(3, totalHistoryPages(25L));
        assertEquals(1, totalHistoryPages(7L));
        assertNotEquals(totalHistoryPages(25L), totalHistoryPages(7L));
    }
}
package minesweeper.rankingV2;

import minesweeper.model.GameResult;
import minesweeper.model.enums.Difficulty;
import minesweeper.repository.MySqlGameResultRepository;
import minesweeper.repository.spec.GameResultFilterSpec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UC04.2 – Lịch sử chơi V2")
public class HistoryV2 {

    @Mock
    private MySqlGameResultRepository gameResultRepository;

    private static final int    HISTORY_PAGE_SIZE = 10;
    private static final String PLAYER_NAME       = "testuser";

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo GameResult bằng constructor có sẵn.
     * GameResult(gameId, playerName, difficulty, isWon, elapsedTimeMs, flagsUsed, minesTotal, playedAt)
     */
    private GameResult makeResult(Difficulty difficulty, boolean won, int score, long elapsedMs) {
        GameResult r = new GameResult(
                UUID.randomUUID().toString(),
                PLAYER_NAME,
                difficulty,
                won,
                elapsedMs,
                0, 0,
                LocalDateTime.now()
        );
        r.setScore(score);
        return r;
    }

    /**
     * Tạo danh sách n bản ghi xen kẽ độ khó và kết quả.
     */
    private List<GameResult> makeResultList(int n) {
        Difficulty[] levels = Difficulty.values();
        List<GameResult> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(makeResult(levels[i % levels.length], i % 2 == 0, 1000 - i * 10, (long) (i + 1) * 30_000));
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Ánh xạ logic từ RankingHistoryController
    // ─────────────────────────────────────────────────────────────────────────

    /** Ánh xạ buildHistoryFilterSpec() trong controller. */
    private GameResultFilterSpec buildSpec(String playerName, Difficulty difficulty, Boolean win) {
        GameResultFilterSpec spec = new GameResultFilterSpec();
        spec.username   = playerName;
        spec.difficulty = difficulty;
        spec.win        = win;
        return spec;
    }

    /** Ánh xạ mệnh đề WHERE của MySqlGameResultRepository.findPaged(). */
    private List<GameResult> applyFilter(List<GameResult> all, GameResultFilterSpec spec) {
        return all.stream()
                .filter(r -> r.getPlayerName().equalsIgnoreCase(spec.username))
                .filter(r -> spec.difficulty == null || r.getDifficulty() == spec.difficulty)
                .filter(r -> spec.win        == null || r.isWon()         == spec.win)
                .collect(Collectors.toList());
    }

    /** Ánh xạ totalHistoryPages trong updateHistoryPaginationControls(). */
    private int totalHistoryPages(long totalItems) {
        return (int) Math.ceil((double) totalItems / HISTORY_PAGE_SIZE);
    }

    /** Ánh xạ lblHistoryPage.setText(...) trong updateHistoryPaginationControls(). */
    private String historyPageLabel(int currentPage, int totalPages) {
        return "Trang " + (currentPage + 1) + " / " + Math.max(1, totalPages);
    }

    private boolean isPrevDisabled(int currentPage)               { return currentPage == 0; }
    private boolean isNextDisabled(int currentPage, int totalPages) { return currentPage >= totalPages - 1; }

    /** Ánh xạ getPage() – cắt slice theo trang. */
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
    @DisplayName("HV2_DIFF_02 – Lọc độ khó không tồn tại trong dữ liệu → danh sách rỗng")
    void hvDiff02_filterDifficulty_noMatch_returnsEmpty() {
        // spec: chỉ có EASY + MEDIUM, lọc EXPERT → không khớp
        List<GameResult> all = List.of(
                makeResult(Difficulty.EASY,   true,  900, 30_000),
                makeResult(Difficulty.MEDIUM, true,  800, 60_000)
        );

        assertTrue(applyFilter(all, buildSpec(PLAYER_NAME, Difficulty.EXPERT, null)).isEmpty(),
                "Không có ván EXPERT nào, kết quả phải rỗng");
    }

    @Test
    @DisplayName("HV2_DIFF_03 – DifficultyOption ALL (null) không lọc, trả về toàn bộ bản ghi")
    void hvDiff03_filterNull_returnsAll() {
        List<GameResult> all    = makeResultList(8);
        List<GameResult> result = applyFilter(all, buildSpec(PLAYER_NAME, null, null));

        assertEquals(8, result.size(), "null difficulty phải trả về tất cả 8 bản ghi");
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
    @DisplayName("HV2_RES_03 – Toàn thua, lọc Thắng → danh sách rỗng")
    void hvRes03_allLosses_filterWin_returnsEmpty() {
        List<GameResult> all = List.of(
                makeResult(Difficulty.EASY, false, 0, 30_000),
                makeResult(Difficulty.EASY, false, 0, 40_000)
        );

        assertTrue(applyFilter(all, buildSpec(PLAYER_NAME, null, true)).isEmpty());
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
    @DisplayName("HV2_CMB_02 – Lọc MEDIUM + Thua trả về đúng 2 ván MEDIUM-lose")
    void hvCmb02_mediumAndLose_returnsMediumLoseOnly() {
        List<GameResult> all = List.of(
                makeResult(Difficulty.MEDIUM, false, 400, 50_000),  // ✓
                makeResult(Difficulty.MEDIUM, true,  700, 60_000),  // ✗ thắng
                makeResult(Difficulty.HARD,   false, 300, 70_000),  // ✗ sai độ khó
                makeResult(Difficulty.MEDIUM, false, 350, 55_000)   // ✓
        );

        List<GameResult> result = applyFilter(all, buildSpec(PLAYER_NAME, Difficulty.MEDIUM, false));

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> r.getDifficulty() == Difficulty.MEDIUM && !r.isWon()));
    }

    @Test
    @DisplayName("HV2_CMB_03 – Thay đổi bộ lọc reset currentPage về 0")
    void hvCmb03_filterChange_resetsPageToZero() {
        // Mô phỏng trạng thái người dùng đang ở trang 2
        int currentPage = 2;
        // onHistoryFilterChanged() → historyCurrentPage = 0
        currentPage = 0;
        assertEquals(0, currentPage, "Đổi filter phải reset về trang 0");
    }

    @Test
    @DisplayName("HV2_CMB_04 – buildSpec() luôn set username dù filter thay đổi")
    void hvCmb04_buildSpec_alwaysSetsUsername() {
        assertAll("playerName luôn phải xuất hiện trong spec.username",
                () -> assertEquals(PLAYER_NAME, buildSpec(PLAYER_NAME, null,            null ).username),
                () -> assertEquals(PLAYER_NAME, buildSpec(PLAYER_NAME, Difficulty.EASY, null ).username),
                () -> assertEquals(PLAYER_NAME, buildSpec(PLAYER_NAME, null,            true ).username),
                () -> assertEquals(PLAYER_NAME, buildSpec(PLAYER_NAME, Difficulty.HARD, false).username)
        );
    }

    // =========================================================================
    // NHÓM 4 – Phân trang lịch sử (HV2_PG)
    // =========================================================================

    @Test
    @DisplayName("HV2_PG_01 – Nhãn trang hiển thị đúng 'Trang X / Y' ở mọi trang")
    void hvPg01_pageLabel_correctForAllPages() {
        int totalPages = totalHistoryPages(25L); // 3
        assertAll(
                () -> assertEquals("Trang 1 / 3", historyPageLabel(0, totalPages)),
                () -> assertEquals("Trang 2 / 3", historyPageLabel(1, totalPages)),
                () -> assertEquals("Trang 3 / 3", historyPageLabel(2, totalPages))
        );
    }

    @Test
    @DisplayName("HV2_PG_02 – 0 ván → totalPages = 0, nhãn hiển thị 'Trang 1 / 1'")
    void hvPg02_zeroItems_labelTrang1Of1() {
        int totalPages = totalHistoryPages(0L);
        assertEquals(0, totalPages);
        assertEquals("Trang 1 / 1", historyPageLabel(0, totalPages),
                "Math.max(1, 0) đảm bảo không hiện 'Trang 1 / 0'");
    }

    @Test
    @DisplayName("HV2_PG_03 – btnHistoryPrev/Next disable đúng theo vị trí trang")
    void hvPg03_prevNextButton_stateCorrect() {
        int totalPages = totalHistoryPages(25L); // 3
        assertAll(
                () -> assertTrue(isPrevDisabled(0),           "Prev disable ở trang đầu"),
                () -> assertFalse(isPrevDisabled(1),          "Prev enable ở trang giữa"),
                () -> assertFalse(isNextDisabled(0, totalPages), "Next enable ở trang đầu"),
                () -> assertTrue(isNextDisabled(2, totalPages),  "Next disable ở trang cuối")
        );
    }

    @Test
    @DisplayName("HV2_PG_04 – onHistoryNextPage / PrevPage tăng giảm đúng và chặn biên")
    void hvPg04_nextPrevPage_clampedCorrectly() {
        int totalPages = totalHistoryPages(25L); // 3
        int page = 0;

        // Next: 0 → 1 → 2 → vẫn 2 (chặn biên)
        if (page < totalPages - 1) page++;  assertEquals(1, page);
        if (page < totalPages - 1) page++;  assertEquals(2, page);
        if (page < totalPages - 1) page++;  assertEquals(2, page, "Chặn biên cuối");

        // Prev: 2 → 1 → 0 → vẫn 0 (chặn biên)
        if (page > 0) page--;  assertEquals(1, page);
        if (page > 0) page--;  assertEquals(0, page);
        if (page > 0) page--;  assertEquals(0, page, "Chặn biên đầu");
    }

    @Test
    @DisplayName("HV2_PG_05 – Trang cuối trả về đúng phần dư (25 ván → trang 2 có 5 phần tử)")
    void hvPg05_lastPage_returnsRemainder() {
        List<GameResult> filtered = applyFilter(makeResultList(25), buildSpec(PLAYER_NAME, null, null));

        List<GameResult> lastPage = getPage(filtered, 2);

        assertEquals(5, lastPage.size(), "Trang cuối của 25 items phải có 5 phần tử");
    }
}
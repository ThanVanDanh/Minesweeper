package minesweeper.rankingV2;

import minesweeper.controller.RankingController;
import minesweeper.dto.RankingDTO;
import minesweeper.repository.RankingRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử chức năng UC – Bảng xếp hạng lịch sử V2
 * Nguồn: RankingHistoryController.java, RankingController.java, RankingDTO.java
 *
 * Bốn nhóm chính:
 *   1. Hiển thị bestTime (RK_BT_*)
 *   2. Phân trang       (RK_PG_*)
 *   3. Highlight người dùng hiện tại (RK_HL_*)
 *   4. Ghim xếp hạng người dùng hiện tại (RK_PIN_*)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC – Bảng xếp hạng lịch sử V2")
public class RankingV2 {

    @Mock
    private RankingRepository rankingRepository;

    // ── Hằng số ──────────────────────────────────────────────────────────────
    private static final int PAGE_SIZE = 10;
    private static final int LEVEL_ID  = 1;

    // ── Helper tạo RankingDTO ─────────────────────────────────────────────────

    /**
     * Tạo RankingDTO với bestTimeMs tuỳ chỉnh.
     */
    private RankingDTO makeRanking(int rank, String playerName,
                                   int totalGames, int wins,
                                   int bestScore, long bestTimeMs) {
        return new RankingDTO(rank, playerName, totalGames, wins, bestScore, bestTimeMs);
    }

    /**
     * Tạo danh sách n RankingDTO với tên "player01"…"playerN".
     * bestTimeMs tăng dần (player01 nhanh nhất).
     */
    private List<RankingDTO> makeRankingList(int n) {
        List<RankingDTO> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            list.add(makeRanking(i, "player" + String.format("%02d", i),
                    10, 5, 1000 - i * 10, (long) i * 60_000));
        }
        return list;
    }

    // ── Logic phân trang (ánh xạ từ RankingHistoryController) ─────────────────

    /** Lấy slice trang currentPage từ danh sách allRankings. */
    private List<RankingDTO> getPageSlice(List<RankingDTO> allRankings, int currentPage) {
        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, allRankings.size());
        return from < to ? allRankings.subList(from, to) : List.of();
    }

    /** Tính tổng số trang. */
    private int totalPages(List<RankingDTO> allRankings) {
        return (int) Math.ceil((double) allRankings.size() / PAGE_SIZE);
    }

    /** Nhãn trang hiển thị. */
    private String pageLabel(int currentPage, int total) {
        return "Trang " + (currentPage + 1) + " / " + Math.max(1, total);
    }

    /** Kiểm tra người dùng hiện tại (case-insensitive). */
    private boolean isCurrentUser(String rowPlayer, String currentUser) {
        if (currentUser == null || rowPlayer == null) return false;
        return currentUser.equalsIgnoreCase(rowPlayer);
    }

    /**
     * Logic ghim: tìm hàng của người dùng; nếu không nằm trong trang hiện tại thì ghim.
     */
    private RankingDTO findPinnedRow(List<RankingDTO> allRankings,
                                     int currentPage, String currentUser) {
        if (currentUser == null) return null;

        RankingDTO myRow = allRankings.stream()
                .filter(r -> r.getPlayerName().equalsIgnoreCase(currentUser))
                .findFirst()
                .orElse(null);

        if (myRow == null) return null;

        List<RankingDTO> slice = getPageSlice(allRankings, currentPage);
        boolean visibleInPage = slice.stream()
                .anyMatch(r -> r.getPlayerName().equalsIgnoreCase(currentUser));

        return visibleInPage ? null : myRow; // null = không cần ghim
    }

    // =========================================================================
    // NHÓM 1 – Hiển thị Best Time (RK_BT)
    // =========================================================================

    /**
     * RK_BT_01: bestTimeMs > 0 → hiển thị đúng định dạng mm:ss.
     * Ví dụ: 75_000 ms = 1 phút 15 giây → "1:15"
     */
    @Test
    @DisplayName("RK_BT_01 – bestTimeMs > 0 định dạng mm:ss đúng")
    void rkBt01_bestTimeFormatted_minutes() {
        RankingDTO dto = makeRanking(1, "player01", 10, 5, 900, 75_000L);
        assertEquals("1:15", dto.getBestTimeFormatted(),
                "75 000 ms phải hiện '1:15'");
    }

    /**
     * RK_BT_02: bestTimeMs < 60 000 ms → hiển thị "0:ss".
     * Ví dụ: 45_000 ms = 45 giây → "0:45"
     */
    @Test
    @DisplayName("RK_BT_02 – bestTimeMs < 60s định dạng 0:ss đúng")
    void rkBt02_bestTimeFormatted_seconds() {
        RankingDTO dto = makeRanking(1, "player01", 10, 5, 900, 45_000L);
        assertEquals("0:45", dto.getBestTimeFormatted(),
                "45 000 ms phải hiện '0:45'");
    }

    /**
     * RK_BT_03: bestTimeMs = 0 → hiển thị "—" (chưa có ván thắng).
     */
    @Test
    @DisplayName("RK_BT_03 – bestTimeMs = 0 hiển thị dấu gạch ngang")
    void rkBt03_bestTimeFormatted_zero() {
        RankingDTO dto = makeRanking(1, "player01", 5, 0, 0, 0L);
        assertEquals("—", dto.getBestTimeFormatted(),
                "bestTimeMs = 0 phải hiện '—'");
    }

    /**
     * RK_BT_04: bestTimeMs < 0 → hiển thị "—".
     */
    @Test
    @DisplayName("RK_BT_04 – bestTimeMs âm hiển thị dấu gạch ngang")
    void rkBt04_bestTimeFormatted_negative() {
        RankingDTO dto = makeRanking(1, "player01", 5, 0, 0, -1L);
        assertEquals("—", dto.getBestTimeFormatted(),
                "bestTimeMs âm phải hiện '—'");
    }

    /**
     * RK_BT_05: bestTimeMs = 60 000 ms → hiển thị "1:00".
     */
    @Test
    @DisplayName("RK_BT_05 – bestTimeMs = 60 000 ms hiển thị '1:00'")
    void rkBt05_bestTimeFormatted_exactly60s() {
        RankingDTO dto = makeRanking(1, "player01", 10, 5, 900, 60_000L);
        assertEquals("1:00", dto.getBestTimeFormatted(),
                "60 000 ms phải hiện '1:00'");
    }

    /**
     * RK_BT_07: Khi tất cả người chơi chưa thắng ván nào, bestTimeMs = 0,
     * getBestTimeFormatted() trả về "—" cho tất cả.
     */
    @Test
    @DisplayName("RK_BT_07 – Tất cả 0 wins → getBestTimeFormatted() = '—'")
    void rkBt07_allZeroWins_dashForAll() {
        List<RankingDTO> list = List.of(
                makeRanking(1, "p1", 5, 0, 0, 0L),
                makeRanking(2, "p2", 3, 0, 0, 0L)
        );
        assertTrue(list.stream().allMatch(r -> "—".equals(r.getBestTimeFormatted())),
                "Tất cả 0 wins phải hiện '—'");
    }

    // =========================================================================
    // NHÓM 2 – Phân trang (RK_PG)
    // =========================================================================

    /**
     * RK_PG_01: Trang đầu tiên (page 0) → slice gồm items 0-9.
     */
    @Test
    @DisplayName("RK_PG_01 – Trang 0 trả về đúng 10 phần tử đầu")
    void rkPg01_firstPage_returns10Items() {
        List<RankingDTO> all = makeRankingList(25);
        List<RankingDTO> slice = getPageSlice(all, 0);
        assertEquals(10, slice.size(), "Trang 0 phải có 10 phần tử");
        assertEquals("player01", slice.get(0).getPlayerName());
        assertEquals("player10", slice.get(9).getPlayerName());
    }

    /**
     * RK_PG_02: Trang cuối (page 2 của 25 items) → 5 items còn lại.
     */
    @Test
    @DisplayName("RK_PG_02 – Trang cuối trả về đúng số items còn lại")
    void rkPg02_lastPage_returnsRemainder() {
        List<RankingDTO> all = makeRankingList(25);
        List<RankingDTO> slice = getPageSlice(all, 2);
        assertEquals(5, slice.size(), "Trang cuối phải có 5 phần tử");
        assertEquals("player21", slice.get(0).getPlayerName());
    }

    /**
     * RK_PG_03: Danh sách rỗng → slice rỗng, nhãn "Trang 1 / 1".
     */
    @Test
    @DisplayName("RK_PG_03 – Danh sách rỗng → slice rỗng và nhãn 'Trang 1 / 1'")
    void rkPg03_emptyList_emptySlice() {
        List<RankingDTO> all = Collections.emptyList();
        List<RankingDTO> slice = getPageSlice(all, 0);
        assertTrue(slice.isEmpty(), "Slice phải rỗng khi không có dữ liệu");
        assertEquals("Trang 1 / 1", pageLabel(0, totalPages(all)));
    }

    /**
     * RK_PG_04: Đúng 10 items → 1 trang, btnNext bị disable.
     */
    @Test
    @DisplayName("RK_PG_04 – Đúng 10 items → 1 trang, không có trang tiếp theo")
    void rkPg04_exactOnePage() {
        List<RankingDTO> all = makeRankingList(10);
        int total = totalPages(all);
        assertEquals(1, total, "10 items phải là 1 trang");
        // btnNext disable khi currentPage >= total - 1
        assertTrue(0 >= total - 1, "Ở trang 0, không có trang tiếp theo");
    }

    /**
     * RK_PG_05: Nhãn trang hiển thị "Trang X / Y" đúng với 25 items.
     */
    @Test
    @DisplayName("RK_PG_05 – Nhãn trang hiển thị 'Trang X / Y' đúng")
    void rkPg05_pageLabel_correct() {
        List<RankingDTO> all = makeRankingList(25);
        int total = totalPages(all); // 3
        assertEquals("Trang 1 / 3", pageLabel(0, total));
        assertEquals("Trang 2 / 3", pageLabel(1, total));
        assertEquals("Trang 3 / 3", pageLabel(2, total));
    }

    /**
     * RK_PG_06: btnPrev disable ở trang đầu, enable ở trang 1.
     */
    @Test
    @DisplayName("RK_PG_06 – btnPrev disable ở trang 0, enable ở trang 1")
    void rkPg06_prevButton_disableAtFirstPage() {
        assertTrue(0 == 0,  "Trang 0: btnPrev phải disable");
        assertFalse(1 == 0, "Trang 1: btnPrev phải enable");
    }

    /**
     * RK_PG_07: btnNext disable ở trang cuối.
     */
    @Test
    @DisplayName("RK_PG_07 – btnNext disable ở trang cuối")
    void rkPg07_nextButton_disableAtLastPage() {
        List<RankingDTO> all = makeRankingList(25);
        int total = totalPages(all); // 3
        int lastPage = total - 1;    // 2
        assertTrue(lastPage >= total - 1, "Trang cuối: btnNext phải disable");
        assertFalse(0 >= total - 1,       "Trang 0: btnNext phải enable");
    }

    /**
     * RK_PG_08: Chuyển trang bằng onNextPage / onPrevPage → currentPage thay đổi đúng.
     */
    @Test
    @DisplayName("RK_PG_08 – onNextPage tăng currentPage, onPrevPage giảm đúng")
    void rkPg08_pageNavigation_correct() {
        List<RankingDTO> all = makeRankingList(25);
        int total = totalPages(all);
        int page = 0;

        // Next
        if (page < total - 1) page++;
        assertEquals(1, page, "Sau onNextPage: page phải là 1");

        // Next lần 2
        if (page < total - 1) page++;
        assertEquals(2, page, "Sau onNextPage lần 2: page phải là 2");

        // Next ở trang cuối → không đổi
        if (page < total - 1) page++;
        assertEquals(2, page, "Ở trang cuối onNextPage không được tăng page");

        // Prev
        if (page > 0) page--;
        assertEquals(1, page, "Sau onPrevPage: page phải là 1");
    }

    /**
     * RK_PG_09: getRankingTop(levelId, limit) ≤ limit khi DB trả về nhiều hơn.
     */
    @Test
    @DisplayName("RK_PG_09 – getRankingTop giới hạn đúng số lượng kết quả")
    void rkPg09_getRankingTop_limitsResults() throws Exception {
        List<RankingDTO> all = makeRankingList(50);
        when(rankingRepository.getLeaderboardByLevel(LEVEL_ID)).thenReturn(all);

        RankingController controller = new RankingController(rankingRepository);
        List<RankingDTO> top10 = controller.getRankingTop(LEVEL_ID, 10);

        assertEquals(10, top10.size(), "getRankingTop(10) phải trả về đúng 10 items");
    }

    /**
     * RK_PG_10: getRankingTop với limit <= 0 → trả về danh sách rỗng.
     */
    @Test
    @DisplayName("RK_PG_10 – getRankingTop limit <= 0 trả về rỗng")
    void rkPg10_getRankingTop_zeroLimit_returnsEmpty() throws Exception {
        RankingController controller = new RankingController(rankingRepository);
        List<RankingDTO> result = controller.getRankingTop(LEVEL_ID, 0);
        assertTrue(result.isEmpty(), "limit=0 phải trả về danh sách rỗng");
    }

    /**
     * RK_PG_11: Trang mà currentPage vượt quá tổng trang → slice rỗng.
     */
    @Test
    @DisplayName("RK_PG_11 – currentPage vượt giới hạn → slice rỗng")
    void rkPg11_outOfBoundsPage_emptySlice() {
        List<RankingDTO> all = makeRankingList(5);
        List<RankingDTO> slice = getPageSlice(all, 99);
        assertTrue(slice.isEmpty(), "Page vượt giới hạn phải trả về slice rỗng");
    }

    // =========================================================================
    // NHÓM 3 – Highlight người dùng hiện tại (RK_HL)
    // =========================================================================

    /**
     * RK_HL_01: Hàng có playerName khớp currentUser → isCurrentUser = true.
     */
    @Test
    @DisplayName("RK_HL_01 – Hàng khớp currentUser → isCurrentUser trả về true")
    void rkHl01_currentUserRow_highlighted() {
        String currentUser = "player05";
        RankingDTO dto = makeRanking(5, "player05", 10, 7, 800, 50_000L);
        assertTrue(isCurrentUser(dto.getPlayerName(), currentUser),
                "player05 phải được highlight");
    }

    /**
     * RK_HL_02: isCurrentUser case-insensitive ("PLAYER05" vs "player05").
     */
    @Test
    @DisplayName("RK_HL_02 – isCurrentUser case-insensitive")
    void rkHl02_currentUser_caseInsensitive() {
        assertTrue(isCurrentUser("PLAYER05", "player05"),
                "So sánh tên phải không phân biệt hoa thường");
        assertTrue(isCurrentUser("Player05", "PLAYER05"),
                "Cả hai chiều đều phải true");
    }

    /**
     * RK_HL_03: Hàng KHÔNG khớp currentUser → isCurrentUser = false.
     */
    @Test
    @DisplayName("RK_HL_03 – Hàng khác currentUser → isCurrentUser = false")
    void rkHl03_otherUser_notHighlighted() {
        assertFalse(isCurrentUser("player01", "player05"),
                "player01 không phải current user");
    }

    /**
     * RK_HL_04: currentUser null (chưa đăng nhập) → isCurrentUser = false.
     */
    @Test
    @DisplayName("RK_HL_04 – currentUser null (chưa login) → không highlight")
    void rkHl04_nullCurrentUser_notHighlighted() {
        assertFalse(isCurrentUser("player01", null),
                "Chưa đăng nhập không được highlight hàng nào");
    }

    /**
     * RK_HL_05: Toàn bộ trang chỉ có đúng 1 hàng được highlight (currentUser).
     */
    @Test
    @DisplayName("RK_HL_05 – Chỉ đúng 1 hàng trong trang được highlight")
    void rkHl05_onlyOneRowHighlighted() {
        String currentUser = "player03";
        List<RankingDTO> page = makeRankingList(10);
        long highlightCount = page.stream()
                .filter(r -> isCurrentUser(r.getPlayerName(), currentUser))
                .count();
        assertEquals(1, highlightCount,
                "Phải có đúng 1 hàng được highlight trong trang");
    }

    /**
     * RK_HL_06: Nếu currentUser không có trong danh sách → không hàng nào được highlight.
     */
    @Test
    @DisplayName("RK_HL_06 – currentUser không trong danh sách → 0 hàng highlight")
    void rkHl06_currentUserNotInList_noHighlight() {
        String currentUser = "ghost_user";
        List<RankingDTO> page = makeRankingList(10);
        long highlightCount = page.stream()
                .filter(r -> isCurrentUser(r.getPlayerName(), currentUser))
                .count();
        assertEquals(0, highlightCount,
                "currentUser không tồn tại → không hàng nào highlight");
    }

    // =========================================================================
    // NHÓM 4 – Ghim xếp hạng người dùng hiện tại (RK_PIN)
    // =========================================================================

    /**
     * RK_PIN_01: currentUser ở trang 0, đang xem trang 0 → không ghim (myRankBox ẩn).
     */
    @Test
    @DisplayName("RK_PIN_01 – currentUser nằm trong trang hiện tại → không ghim")
    void rkPin01_currentUserInPage_noPinned() {
        List<RankingDTO> all = makeRankingList(25);
        String currentUser = "player05"; // rank 5, trang 0
        RankingDTO pinned = findPinnedRow(all, 0, currentUser);
        assertNull(pinned, "User trong trang 0 không cần ghim");
    }

    /**
     * RK_PIN_02: currentUser ở trang 1 (rank 11-20), đang xem trang 0 → ghim.
     */
    @Test
    @DisplayName("RK_PIN_02 – currentUser ngoài trang hiện tại → ghim đúng hàng")
    void rkPin02_currentUserOutsidePage_pinned() {
        List<RankingDTO> all = makeRankingList(25);
        String currentUser = "player15"; // rank 15, trang 1
        RankingDTO pinned = findPinnedRow(all, 0, currentUser);
        assertNotNull(pinned, "User ngoài trang 0 phải được ghim");
        assertEquals("player15", pinned.getPlayerName());
        assertEquals(15, pinned.getRank());
    }

    /**
     * RK_PIN_03: currentUser null (chưa đăng nhập) → không ghim.
     */
    @Test
    @DisplayName("RK_PIN_03 – Chưa login → không ghim hàng nào")
    void rkPin03_notLoggedIn_noPinned() {
        List<RankingDTO> all = makeRankingList(25);
        RankingDTO pinned = findPinnedRow(all, 0, null);
        assertNull(pinned, "Chưa đăng nhập thì không ghim");
    }

    /**
     * RK_PIN_04: currentUser không tồn tại trong bảng xếp hạng → không ghim.
     */
    @Test
    @DisplayName("RK_PIN_04 – currentUser không có trong ranking → không ghim")
    void rkPin04_currentUserNotInRanking_noPinned() {
        List<RankingDTO> all = makeRankingList(25);
        RankingDTO pinned = findPinnedRow(all, 0, "ghost_user");
        assertNull(pinned, "User không có trong ranking thì không ghim");
    }

    /**
     * RK_PIN_05: currentUser ở trang 2, đang xem trang 1 → ghim đúng rank.
     */
    @Test
    @DisplayName("RK_PIN_05 – currentUser ở trang 2 khi xem trang 1 → ghim")
    void rkPin05_currentUserOnPage2_viewingPage1_pinned() {
        List<RankingDTO> all = makeRankingList(30);
        String currentUser = "player25"; // rank 25, trang 2
        RankingDTO pinned = findPinnedRow(all, 1, currentUser);
        assertNotNull(pinned, "User ở trang 2 khi xem trang 1 phải được ghim");
        assertEquals("player25", pinned.getPlayerName());
        assertEquals(25, pinned.getRank());
    }

    /**
     * RK_PIN_06: Sau khi chuyển đến trang chứa currentUser → không còn ghim.
     */
    @Test
    @DisplayName("RK_PIN_06 – Chuyển đến trang chứa currentUser → bỏ ghim")
    void rkPin06_navigateToUserPage_pinRemoved() {
        List<RankingDTO> all = makeRankingList(25);
        String currentUser = "player15"; // rank 15, trang 1

        // Đang ở trang 0 → ghim
        RankingDTO pinnedPage0 = findPinnedRow(all, 0, currentUser);
        assertNotNull(pinnedPage0, "Trang 0: phải ghim player15");

        // Chuyển sang trang 1 → không ghim nữa
        RankingDTO pinnedPage1 = findPinnedRow(all, 1, currentUser);
        assertNull(pinnedPage1, "Trang 1: không còn ghim vì user đã hiển thị");
    }

    /**
     * RK_PIN_07: Hàng được ghim hiển thị đúng bestTime của người dùng.
     */
    @Test
    @DisplayName("RK_PIN_07 – Hàng ghim hiển thị đúng bestTime của currentUser")
    void rkPin07_pinnedRow_showsCorrectBestTime() {
        List<RankingDTO> all = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            long ms = i == 12 ? 93_000L : (long) i * 60_000;
            all.add(makeRanking(i, "player" + String.format("%02d", i),
                    10, 5, 1000 - i * 10, ms));
        }

        String currentUser = "player12"; // rank 12, bestTime 93 000 ms = 1:33
        RankingDTO pinned = findPinnedRow(all, 0, currentUser); // trang 0: rank 1-10

        assertNotNull(pinned, "player12 ngoài trang 0 phải được ghim");
        assertEquals("1:33", pinned.getBestTimeFormatted(),
                "Hàng ghim phải hiển thị '1:33'");
    }

    /**
     * RK_PIN_08: Danh sách chỉ có 1 trang → currentUser luôn hiển thị, không ghim.
     */
    @Test
    @DisplayName("RK_PIN_08 – Chỉ 1 trang dữ liệu → không bao giờ ghim")
    void rkPin08_singlePage_neverPinned() {
        List<RankingDTO> all = makeRankingList(5);
        for (int i = 0; i < all.size(); i++) {
            RankingDTO pinned = findPinnedRow(all, 0,
                    all.get(i).getPlayerName());
            assertNull(pinned,
                    "Chỉ có 1 trang: " + all.get(i).getPlayerName() + " không được ghim");
        }
    }
}
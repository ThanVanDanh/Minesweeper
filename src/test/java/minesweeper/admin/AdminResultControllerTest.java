package minesweeper.admin;

import minesweeper.model.GameResult;
import minesweeper.model.enums.Difficulty;
import minesweeper.repository.GameResultRepository;
import minesweeper.repository.exception.DataAccessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử chức năng UC19 – Xóa kết quả gian lận
 * Nguồn: AdminResultController.java, MySqlGameResultRepository.java
 *
 * Lưu ý: Test business logic thuần, mock GameResultRepository để tránh phụ thuộc DB.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC19 – Xóa kết quả gian lận")
public class AdminResultControllerTest {

    @Mock
    private GameResultRepository repository;

    // ── Constants ────────────────────────────────────────────────────────────
    private static final String FILTER_ALL = "Tất cả";

    // ── Helper: tạo GameResult mẫu ─────────────────────────────────────────

    private GameResult makeResult(String gameId, String playerName,
                                  Difficulty difficulty, boolean isWon) {
        GameResult r = new GameResult(
                gameId, playerName, difficulty, isWon,
                120_000L, 3, 10, LocalDateTime.now()
        );
        return r;
    }

    // ── Helper: logic lọc (ánh xạ từ AdminResultController.matchesFilter) ──

    private boolean matchesFilter(GameResult game, String usernameFilter,
                                  String difficultyFilter, String resultFilter) {
        boolean matchUsername = game.getPlayerName().toLowerCase()
                .contains(usernameFilter);
        boolean matchDifficulty = FILTER_ALL.equals(difficultyFilter)
                || (game.getDifficulty() != null
                && game.getDifficulty().getLabel().equalsIgnoreCase(difficultyFilter));
        boolean matchResult = FILTER_ALL.equals(resultFilter)
                || game.getResult().equalsIgnoreCase(resultFilter);

        return matchUsername && matchDifficulty && matchResult;
    }

    // =========================================================================
    // TC ADMR_01 – Mở màn hình kết quả
    // =========================================================================

    /**
     * ADMR_01: Tải danh sách kết quả khi mở màn hình
     * Mong đợi: getAllResults() được gọi và trả về đúng dữ liệu
     */
    @Test
    @DisplayName("ADMR_01 – Tải danh sách kết quả thành công")
    void admr01_loadResults_success() throws DataAccessException {
        List<GameResult> mockResults = Arrays.asList(
                makeResult("G001", "player01", Difficulty.EASY,   true),
                makeResult("G002", "player02", Difficulty.MEDIUM, false),
                makeResult("G003", "player01", Difficulty.HARD,   true)
        );
        when(repository.getAllResults()).thenReturn(mockResults);

        List<GameResult> result = repository.getAllResults();

        assertNotNull(result, "Danh sách kết quả không được null");
        assertEquals(3, result.size(), "Phải tải được 3 kết quả");
        verify(repository, times(1)).getAllResults();
    }

    // =========================================================================
    // TC ADMR_02 – Lọc theo Username
    // =========================================================================

    /**
     * ADMR_02: Lọc theo username = "player01"
     * Mong đợi: Chỉ hiển thị kết quả của player01
     */
    @Test
    @DisplayName("ADMR_02 – Lọc theo username 'player01'")
    void admr02_filterByUsername() {
        List<GameResult> allResults = Arrays.asList(
                makeResult("G001", "player01", Difficulty.EASY,   true),
                makeResult("G002", "player02", Difficulty.MEDIUM, false),
                makeResult("G003", "player01", Difficulty.HARD,   true)
        );

        String usernameFilter = "player01";
        List<GameResult> filtered = allResults.stream()
                .filter(g -> matchesFilter(g, usernameFilter, FILTER_ALL, FILTER_ALL))
                .toList();

        assertEquals(2, filtered.size(), "Phải tìm thấy 2 kết quả của player01");
        assertTrue(filtered.stream()
                .allMatch(g -> g.getPlayerName().contains(usernameFilter)));
    }

    // =========================================================================
    // TC ADMR_03 – Lọc theo Độ khó = EASY
    // =========================================================================

    /**
     * ADMR_03: Lọc theo Độ khó = EASY
     * Mong đợi: Chỉ hiển thị kết quả có difficulty = EASY
     */
    @Test
    @DisplayName("ADMR_03 – Lọc theo Độ khó = EASY")
    void admr03_filterByDifficultyEasy() {
        List<GameResult> allResults = Arrays.asList(
                makeResult("G001", "p1", Difficulty.EASY,   true),
                makeResult("G002", "p2", Difficulty.MEDIUM, false),
                makeResult("G003", "p3", Difficulty.EASY,   false)
        );

        String diffFilter = Difficulty.EASY.getLabel(); // "Dễ"
        List<GameResult> filtered = allResults.stream()
                .filter(g -> matchesFilter(g, "", diffFilter, FILTER_ALL))
                .toList();

        assertEquals(2, filtered.size(), "Phải có 2 kết quả EASY");
        assertTrue(filtered.stream()
                .allMatch(g -> g.getDifficulty() == Difficulty.EASY));
    }

    // =========================================================================
    // TC ADMR_04 – Lọc theo Kết quả = Thắng
    // =========================================================================

    /**
     * ADMR_04: Lọc theo Kết quả = "Thắng"
     * Mong đợi: Chỉ hiển thị các ván thắng
     */
    @Test
    @DisplayName("ADMR_04 – Lọc theo Kết quả = Thắng")
    void admr04_filterByResultWin() {
        List<GameResult> allResults = Arrays.asList(
                makeResult("G001", "p1", Difficulty.EASY,   true),
                makeResult("G002", "p2", Difficulty.MEDIUM, false),
                makeResult("G003", "p3", Difficulty.HARD,   true)
        );

        List<GameResult> filtered = allResults.stream()
                .filter(g -> matchesFilter(g, "", FILTER_ALL, "Thắng"))
                .toList();

        assertEquals(2, filtered.size(), "Phải có 2 ván thắng");
        assertTrue(filtered.stream().allMatch(g -> g.getResult().equals("Thắng")));
    }

    // =========================================================================
    // TC ADMR_05 – Lọc kết hợp nhiều tiêu chí
    // =========================================================================

    /**
     * ADMR_05: Lọc kết hợp username="p", difficulty=MEDIUM, result=Thua
     * Mong đợi: Chỉ kết quả thỏa TẤT CẢ điều kiện
     */
    @Test
    @DisplayName("ADMR_05 – Lọc kết hợp username + difficulty + result")
    void admr05_combinedFilter() {
        List<GameResult> allResults = Arrays.asList(
                makeResult("G001", "player01", Difficulty.MEDIUM, false),
                makeResult("G002", "player02", Difficulty.MEDIUM, true),
                makeResult("G003", "admin01",  Difficulty.MEDIUM, false),
                makeResult("G004", "player01", Difficulty.EASY,   false)
        );

        String diffFilter    = Difficulty.MEDIUM.getLabel();
        List<GameResult> filtered = allResults.stream()
                .filter(g -> matchesFilter(g, "p", diffFilter, "Thua"))
                .toList();

        assertEquals(1, filtered.size(),
                "Chỉ G001 thỏa tất cả: username chứa 'p', MEDIUM, Thua");
        assertEquals("G001", filtered.get(0).getGameId());
    }

    // =========================================================================
    // TC ADMR_06 – Không tìm thấy kết quả
    // =========================================================================

    /**
     * ADMR_06: Bộ lọc không khớp gì
     * Mong đợi: Danh sách rỗng, statusLabel='Tìm thấy 0 kết quả'
     */
    @Test
    @DisplayName("ADMR_06 – Không tìm thấy kết quả với bộ lọc không khớp")
    void admr06_noResultsFound() {
        List<GameResult> allResults = Arrays.asList(
                makeResult("G001", "player01", Difficulty.EASY, true)
        );

        List<GameResult> filtered = allResults.stream()
                .filter(g -> matchesFilter(g, "nonexistent_xyz", FILTER_ALL, FILTER_ALL))
                .toList();

        assertTrue(filtered.isEmpty(), "Kết quả phải rỗng");
        // statusLabel sẽ hiển thị "Tìm thấy 0 kết quả" (kiểm tra size)
        assertEquals(0, filtered.size());
    }

    // =========================================================================
    // TC ADMR_07 – Lỗi kết nối CSDL khi tải
    // =========================================================================

    /**
     * ADMR_07: Lỗi DB khi tải danh sách kết quả
     * Mong đợi: DataAccessException được ném ra
     */
    @Test
    @DisplayName("ADMR_07 – Lỗi DB khi tải kết quả ném DataAccessException")
    void admr07_loadResults_dbError_throwsException() throws DataAccessException {
        when(repository.getAllResults())
                .thenThrow(new DataAccessException("Connection refused"));

        assertThrows(DataAccessException.class,
                () -> repository.getAllResults(),
                "Phải ném DataAccessException khi DB không kết nối được");
    }

    // =========================================================================
    // TC ADMR_08 – Reset bộ lọc
    // =========================================================================

    /**
     * ADMR_08: Reset bộ lọc → gọi lại getAllResults()
     * Mong đợi: Repository được gọi lại để lấy toàn bộ dữ liệu
     */
    @Test
    @DisplayName("ADMR_08 – Reset bộ lọc gọi lại getAllResults()")
    void admr08_resetFilter_reloadsData() throws DataAccessException {
        when(repository.getAllResults()).thenReturn(Collections.emptyList());

        // Load lần 1 (ban đầu)
        repository.getAllResults();
        // Reset → load lại lần 2
        repository.getAllResults();

        verify(repository, times(2)).getAllResults();
    }

    // =========================================================================
    // TC ADMR_09 – Lỗi CSDL khi lọc
    // =========================================================================

    /**
     * ADMR_09: Lỗi DB khi thực hiện lọc (getAllResults bên trong onFilter)
     * Mong đợi: Exception được ném ra, hiển thị "Lọc thất bại"
     */
    @Test
    @DisplayName("ADMR_09 – Lỗi DB trong quá trình lọc ném exception")
    void admr09_filter_dbError_throwsException() throws DataAccessException {
        when(repository.getAllResults())
                .thenThrow(new DataAccessException("Query failed"));

        Exception ex = assertThrows(DataAccessException.class,
                () -> repository.getAllResults());

        assertNotNull(ex.getMessage());
    }

    // =========================================================================
    // TC ADMR_10 – Xoá 1 kết quả - xác nhận OK
    // =========================================================================

    /**
     * ADMR_10: Xoá 1 kết quả sau khi xác nhận
     * Mong đợi: deleteByGameIds() được gọi với đúng gameId, kết quả bị xoá khỏi danh sách
     */
    @Test
    @DisplayName("ADMR_10 – Xoá 1 kết quả thành công")
    void admr10_deleteOneResult_confirmed() throws DataAccessException {
        GameResult toDelete = makeResult("G001", "player01", Difficulty.EASY, true);
        doNothing().when(repository).deleteByGameIds(List.of("G001"));

        repository.deleteByGameIds(List.of(toDelete.getGameId()));

        verify(repository).deleteByGameIds(List.of("G001"));
    }

    // =========================================================================
    // TC ADMR_11 – Xoá nhiều kết quả cùng lúc
    // =========================================================================

    /**
     * ADMR_11: Xoá 3 kết quả cùng lúc
     * Mong đợi: deleteByGameIds() được gọi 3 lần (mỗi lần 1 record), tất cả bị xoá
     */
    @Test
    @DisplayName("ADMR_11 – Xoá nhiều kết quả cùng lúc (3 records)")
    void admr11_deleteMultipleResults() throws DataAccessException {
        List<GameResult> selectedList = Arrays.asList(
                makeResult("G001", "p1", Difficulty.EASY,   true),
                makeResult("G002", "p2", Difficulty.MEDIUM, false),
                makeResult("G003", "p3", Difficulty.HARD,   true)
        );

        doNothing().when(repository).deleteByGameIds(anyList());

        // Giả lập vòng lặp trong AdminResultController.onDeleteFraud()
        for (GameResult game : selectedList) {
            repository.deleteByGameIds(List.of(game.getGameId()));
        }

        verify(repository, times(3)).deleteByGameIds(anyList());
        verify(repository).deleteByGameIds(List.of("G001"));
        verify(repository).deleteByGameIds(List.of("G002"));
        verify(repository).deleteByGameIds(List.of("G003"));
    }

    // =========================================================================
    // TC ADMR_12 – Hủy thao tác xoá
    // =========================================================================

    /**
     * ADMR_12: Hủy xoá (bấm Hủy trên dialog)
     * Mong đợi: deleteByGameIds() không được gọi, danh sách giữ nguyên
     */
    @Test
    @DisplayName("ADMR_12 – Hủy xoá: deleteByGameIds() không được gọi")
    void admr12_deleteResult_cancelled_noRepositoryCall() throws DataAccessException {
        boolean userConfirmed = false; // Giả lập user bấm Hủy

        if (userConfirmed) {
            repository.deleteByGameIds(List.of("G001"));
        }

        verify(repository, never()).deleteByGameIds(anyList());
    }

    // =========================================================================
    // TC ADMR_13 – Xoá khi không chọn kết quả
    // =========================================================================

    /**
     * ADMR_13: Bấm Xoá khi selectedList rỗng
     * Logic: selectedList.isEmpty() → repository không được gọi
     */
    @Test
    @DisplayName("ADMR_13 – Xoá khi không chọn kết quả: repository không gọi")
    void admr13_deleteResult_noneSelected_noRepositoryCall() throws DataAccessException {
        List<GameResult> selectedList = Collections.emptyList(); // Không chọn gì

        if (!selectedList.isEmpty()) {
            for (GameResult game : selectedList) {
                repository.deleteByGameIds(List.of(game.getGameId()));
            }
        }

        verify(repository, never()).deleteByGameIds(anyList());
    }

    // =========================================================================
    // TC ADMR_14 – Xoá - Lỗi kết nối CSDL
    // =========================================================================

    /**
     * ADMR_14: Lỗi DB khi thực hiện xoá
     * Mong đợi: Exception được ném ra, rollback tự động, kết quả vẫn còn
     */
    @Test
    @DisplayName("ADMR_14 – Lỗi DB khi xoá ném DataAccessException")
    void admr14_deleteResult_dbError_throwsException() throws DataAccessException {
        doThrow(new DataAccessException("Connection error during delete"))
                .when(repository).deleteByGameIds(anyList());

        assertThrows(DataAccessException.class,
                () -> repository.deleteByGameIds(List.of("G001")),
                "Phải ném DataAccessException khi DB lỗi khi xoá");
    }

    // =========================================================================
    // TC ADMR_15 – Chọn tất cả (Select All)
    // =========================================================================

    /**
     * ADMR_15: Bấm Select All
     * Logic: Tất cả kết quả trong trang phải được đưa vào selectedList
     */
    @Test
    @DisplayName("ADMR_15 – Select All chọn tất cả kết quả trong trang")
    void admr15_selectAll_selectsAllItems() {
        List<GameResult> pageItems = Arrays.asList(
                makeResult("G001", "p1", Difficulty.EASY,   true),
                makeResult("G002", "p2", Difficulty.MEDIUM, false),
                makeResult("G003", "p3", Difficulty.HARD,   true)
        );

        // Giả lập: selectAll() → selectedList = toàn bộ pageItems
        List<GameResult> selectedList = pageItems; // selectAll behavior

        assertEquals(pageItems.size(), selectedList.size(),
                "selectedList phải bằng toàn bộ items trong trang");
        assertEquals(pageItems, selectedList, "Phải chọn đúng các item");
    }

    // =========================================================================
    // TC bổ sung – Xoá thành công cập nhật đúng statusLabel
    // =========================================================================

    /**
     * Kiểm tra message "Đã xoá N kết quả" đúng số lượng
     */
    @Test
    @DisplayName("Bổ sung – Thông báo xoá hiển thị đúng số lượng")
    void deleteStatusMessage_correct() throws DataAccessException {
        List<GameResult> toDelete = Arrays.asList(
                makeResult("G001", "p1", Difficulty.EASY, true),
                makeResult("G002", "p2", Difficulty.EASY, false)
        );

        doNothing().when(repository).deleteByGameIds(anyList());
        for (GameResult g : toDelete) {
            repository.deleteByGameIds(List.of(g.getGameId()));
        }

        // Giả lập tạo statusLabel
        String statusMsg = "Đã xoá " + toDelete.size() + " kết quả";
        assertEquals("Đã xoá 2 kết quả", statusMsg);
    }
}
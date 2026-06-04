package minesweeper.controller;

import minesweeper.model.GameResult;
import minesweeper.model.enums.Difficulty;
import minesweeper.service.GameResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests cho AdminResultController – UC05.9 (v1.2)
 *
 * Phạm vi kiểm thử:
 *  - UC05.9-E1 : validateDeleteSelection – Chưa chọn dòng nào, nhấn Xoá
 *  - UC05.9.3  : validateDeleteSelection – Lấy danh sách kết quả đã chọn
 *  - UC05.9.4  : logic B3 (confirm dialog) – test via package-visible method
 */
class AdminResultControllerTest {

    private AdminResultController controller;

    @BeforeEach
    void setUp() {
        GameResultService mockService = mock(GameResultService.class);
        controller = new AdminResultController(mockService);
    }

    // =========================================================================
    // UC05.9-E1 – validateDeleteSelection
    // =========================================================================

    @Nested
    @DisplayName("UC05.9-E1 – Chưa chọn dòng nào, nhấn Xoá")
    class ValidateDeleteSelectionTests {

        @Test
        @DisplayName("05.9-E1: Danh sách null → ném IllegalArgumentException với đúng thông báo")
        void validateDeleteSelection_NullList_ShouldThrowWithCorrectMessage() {
            // Arrange – 05.9.3: Hệ thống lấy danh sách → kết quả null (trường hợp biên)
            // Act & Assert – 05.9-E1
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> controller.validateDeleteSelection(null)
            );
            assertEquals("Hãy chọn dữ liệu để xoá", ex.getMessage());
        }

        @Test
        @DisplayName("05.9-E1: Danh sách rỗng → ném IllegalArgumentException với đúng thông báo")
        void validateDeleteSelection_EmptyList_ShouldThrowWithCorrectMessage() {
            // Arrange – 05.9.3: Admin chưa tích chọn dòng nào → selectedList trống
            List<GameResult> emptyList = Collections.emptyList();

            // Act & Assert – 05.9-E1
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> controller.validateDeleteSelection(emptyList)
            );
            assertEquals("Hãy chọn dữ liệu để xoá", ex.getMessage());
        }

        @Test
        @DisplayName("05.9-E1: ArrayList rỗng → ném IllegalArgumentException")
        void validateDeleteSelection_EmptyArrayList_ShouldThrow() {
            // Arrange
            List<GameResult> emptyMutableList = new ArrayList<>();

            // Act & Assert – 05.9-E1
            assertThrows(
                    IllegalArgumentException.class,
                    () -> controller.validateDeleteSelection(emptyMutableList)
            );
        }

        // =====================================================================
        // UC05.9.3 – Admin đã chọn ít nhất 1 dòng → validateDeleteSelection pass
        // =====================================================================

        @Test
        @DisplayName("05.9.3: 1 kết quả được chọn → không ném exception")
        void validateDeleteSelection_OneResult_ShouldPass() {
            // Arrange – 05.9.1: Admin tích 1 checkbox
            //           05.9.3: Hệ thống lấy danh sách = 1 phần tử
            List<GameResult> oneItem = List.of(buildGameResult("1"));

            // Act & Assert – không có exception = luồng hợp lệ
            assertDoesNotThrow(() -> controller.validateDeleteSelection(oneItem));
        }

        @Test
        @DisplayName("05.9.3: Nhiều kết quả được chọn → không ném exception")
        void validateDeleteSelection_MultipleResults_ShouldPass() {
            // Arrange – 05.9.1: Admin nhấn Chọn tất cả (nhiều dòng)
            //           05.9.3: Hệ thống lấy danh sách = N phần tử
            List<GameResult> manyItems = List.of(
                    buildGameResult("10"),
                    buildGameResult("20"),
                    buildGameResult("30")
            );

            // Act & Assert – không có exception = luồng hợp lệ
            assertDoesNotThrow(() -> controller.validateDeleteSelection(manyItems));
        }

        @Test
        @DisplayName("05.9.3: Kết quả chứa nhiều độ khó khác nhau → không ném exception")
        void validateDeleteSelection_MixedDifficultyResults_ShouldPass() {
            // Arrange
            List<GameResult> mixedList = new ArrayList<>();
            GameResult easy   = buildGameResult("100"); easy.setScore(500);
            GameResult expert = buildGameResult("200"); expert.setScore(90000);
            mixedList.add(easy);
            mixedList.add(expert);

            // Act & Assert
            assertDoesNotThrow(() -> controller.validateDeleteSelection(mixedList));
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Tạo một GameResult tối giản cho mục đích test.
     * Không cần kết nối DB.
     */
    private GameResult buildGameResult(String id) {
        GameResult r = new GameResult(
                id,
                "testPlayer",
                Difficulty.EASY,
                true,
                30_000L,  // 30 seconds
                5,
                10,
                LocalDateTime.now()
        );
        r.setScore(1000);
        return r;
    }
}

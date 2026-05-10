package minesweeper.playGame;

import minesweeper.model.*;
import minesweeper.controller.GameController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PLAY_02: Cắm cờ - Toggle Flag
 * User click chuột phải để cắm/gỡ cờ trên ô chưa mở.
 * Mỗi ô chỉ có thể cắm cờ 1 lần, không vượt quá số mìn.
 */
@DisplayName("PLAY_02: Cắm cờ")
class PLAY_02_ToggleFlagTest {

    private Board board;
    private GameController gameController;

    @BeforeEach
    void setUp() {
        gameController = new GameController();
        gameController.startNewGame(Difficulty.EASY);
        board = gameController.getBoard();
    }

    @Test
    @DisplayName("PLAY_02.1: Cắm cờ trên ô chưa mở")
    void testToggleFlagOnUnrevealedCell() {
        // Dữ liệu: Mở ô đầu tiên để game bắt đầu
        gameController.reveal(0, 0);

        Cell target = findUnrevealedUnflaggedCell();
        int row = target.getRow();
        int col = target.getCol();

        assertFalse(board.getCell(row, col).isRevealed(),
                "Ô được chọn phải chưa mở");
        assertFalse(board.getCell(row, col).isFlagged(),
                "Ô ban đầu không có cờ");

        // Thực hiện: Cắm cờ
        gameController.toggleFlag(row, col);

        // Kết quả: Ô được cắm cờ
        assertTrue(board.getCell(row, col).isFlagged(),
                "Ô phải được cắm cờ");
    }

    @Test
    @DisplayName("PLAY_02.2: Gỡ cờ khi click lần 2")
    void testToggleFlagRemovesFlag() {
        // Dữ liệu: Mở ô đầu tiên để game bắt đầu
        gameController.reveal(0, 0);

        Cell target = findUnrevealedUnflaggedCell();
        int row = target.getRow();
        int col = target.getCol();

        gameController.toggleFlag(row, col);
        assertTrue(board.getCell(row, col).isFlagged(),
                "Lần 1 cắm cờ");

        // Thực hiện: Click lần 2
        gameController.toggleFlag(row, col);

        // Kết quả: Cờ bị gỡ
        assertFalse(board.getCell(row, col).isFlagged(),
                "Lần 2 gỡ cờ");
    }

    @Test
    @DisplayName("PLAY_02.3: Số cờ không vượt quá số mìn (10)")
    void testFlagCountNotExceedsTotal() {
        // Dữ liệu: Mở ô đầu tiên để game bắt đầu
        gameController.reveal(0, 0);

        assertEquals(10, board.getTotalMines());
        assertEquals(0, board.getFlagsPlaced());

        List<Cell> targets = findUnrevealedUnflaggedCells(board.getTotalMines() + 1);
        assertTrue(targets.size() >= board.getTotalMines(),
                "Cần có đủ ô chưa mở để cắm tối đa số cờ bằng số mìn");

        // Thực hiện: Cố gắng cắm nhiều hơn số mìn
        for (Cell cell : targets) {
            gameController.toggleFlag(cell.getRow(), cell.getCol());
        }

        // Kết quả: Số cờ không vượt quá số mìn
        assertEquals(10, board.getFlagsPlaced(),
                "Tối đa 10 cờ được cắm");
    }

    @Test
    @DisplayName("PLAY_02.4: Counter cờ cập nhật chính xác")
    void testFlagCounterUpdates() {
        // Dữ liệu: Mở ô đầu tiên để game bắt đầu
        gameController.reveal(0, 0);

        assertEquals(0, board.getFlagsPlaced());
        assertEquals(10, board.getRemainingMines());

        List<Cell> targets = findUnrevealedUnflaggedCells(3);
        assertEquals(3, targets.size(),
                "Cần có 3 ô chưa mở để kiểm tra counter cờ");

        // Thực hiện: Cắm 3 cờ trên 3 ô chưa mở
        for (Cell cell : targets) {
            gameController.toggleFlag(cell.getRow(), cell.getCol());
        }

        // Kết quả: Counter cập nhật
        assertEquals(3, board.getFlagsPlaced(),
                "Đã cắm 3 cờ");
        assertEquals(7, board.getRemainingMines(),
                "Cờ còn lại = 7");
    }

    @Test
    @DisplayName("PLAY_02.5: Không thể cắm cờ trên ô đã mở")
    void testCannotFlagRevealedCell() {
        // Dữ liệu: Mở ô đầu tiên
        int row = 0, col = 0;
        gameController.reveal(row, col);
        assertTrue(board.getCell(row, col).isRevealed());

        int flagsBefore = board.getFlagsPlaced();

        // Thực hiện: Cố gắng cắm cờ trên ô đã mở
        gameController.toggleFlag(row, col);

        // Kết quả: Không cắm được
        assertFalse(board.getCell(row, col).isFlagged(),
                "Ô mở không thể cắm cờ");
        assertEquals(flagsBefore, board.getFlagsPlaced(),
                "Số cờ không thay đổi");
    }

    private Cell findUnrevealedUnflaggedCell() {
        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                Cell cell = board.getCell(row, col);
                if (!cell.isRevealed() && !cell.isFlagged()) {
                    return cell;
                }
            }
        }

        fail("Không tìm thấy ô chưa mở và chưa cắm cờ");
        return null;
    }

    private List<Cell> findUnrevealedUnflaggedCells(int limit) {
        List<Cell> cells = new ArrayList<>();

        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                Cell cell = board.getCell(row, col);
                if (!cell.isRevealed() && !cell.isFlagged()) {
                    cells.add(cell);

                    if (cells.size() == limit) {
                        return cells;
                    }
                }
            }
        }

        return cells;
    }
}
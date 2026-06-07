package minesweeper.TestUC02;

import minesweeper.model.Board;
import minesweeper.model.enums.GameState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;

public class BoardUC02UnitTest {

    private void runTestCase(String testId, String testName, Executable executable) throws Throwable {
        try {
            executable.execute();
            System.out.println("[PASS] " + testId + " - " + testName);
        } catch (Throwable e) {
            System.out.println("[FAIL] " + testId + " - " + testName);
            System.out.println("       Lỗi: " + e.getMessage());
            throw e;
        }
    }

    @Test
    void UC02_UT01_shouldCreateBoardWhenConfigIsValid() throws Throwable {
        runTestCase("UC02-UT01", "Khởi tạo Board hợp lệ", () -> {
            Board board = new Board(9, 9, 10, 1);

            assertEquals(9, board.getRows());
            assertEquals(9, board.getCols());
            assertEquals(10, board.getTotalMines());
            assertEquals(1, board.getPlayerCount());
            assertEquals(0, board.getFlagsPlaced());
            assertEquals(10, board.getRemainingMines());
            assertEquals(GameState.IDLE, board.getGameState());
        });
    }

    @Test
    void UC02_UT02_shouldThrowExceptionWhenRowsLessThanTwo() throws Throwable {
        runTestCase("UC02-UT02", "Khởi tạo Board với số hàng nhỏ hơn 2", () -> {
            assertThrows(IllegalArgumentException.class, () -> new Board(1, 9, 10, 1));
        });
    }

    @Test
    void UC02_UT03_shouldThrowExceptionWhenColsLessThanTwo() throws Throwable {
        runTestCase("UC02-UT03", "Khởi tạo Board với số cột nhỏ hơn 2", () -> {
            assertThrows(IllegalArgumentException.class, () -> new Board(9, 1, 10, 1));
        });
    }

    @Test
    void UC02_UT04_shouldThrowExceptionWhenMineCountIsZero() throws Throwable {
        runTestCase("UC02-UT04", "Khởi tạo Board với số mìn bằng 0", () -> {
            assertThrows(IllegalArgumentException.class, () -> new Board(9, 9, 0, 1));
        });
    }

    @Test
    void UC02_UT05_shouldThrowExceptionWhenMineCountGreaterOrEqualTotalCells() throws Throwable {
        runTestCase("UC02-UT05", "Khởi tạo Board với số mìn lớn hơn hoặc bằng tổng số ô", () -> {
            assertThrows(IllegalArgumentException.class, () -> new Board(3, 3, 9, 1));
        });
    }

    @Test
    void UC02_UT06_shouldThrowExceptionWhenPlayerCountLessThanOne() throws Throwable {
        runTestCase("UC02-UT06", "Khởi tạo Board với số người chơi nhỏ hơn 1", () -> {
            assertThrows(IllegalArgumentException.class, () -> new Board(9, 9, 10, 0));
        });
    }

    @Test
    void UC02_UT07_shouldThrowExceptionWhenPlayerCountGreaterThanFour() throws Throwable {
        runTestCase("UC02-UT07", "Khởi tạo Board với số người chơi lớn hơn 4", () -> {
            assertThrows(IllegalArgumentException.class, () -> new Board(9, 9, 10, 5));
        });
    }

    @Test
    void UC02_UT08_shouldReturnCorrectRemainingMinesAtStart() throws Throwable {
        runTestCase("UC02-UT08", "Kiểm tra số mìn còn lại ban đầu", () -> {
            Board board = new Board(10, 10, 20, 1);

            assertEquals(20, board.getRemainingMines());
        });
    }

    @Test
    void UC02_UT09_shouldThrowExceptionWhenGetCellOutOfBounds() throws Throwable {
        runTestCase("UC02-UT09", "Lấy ô ngoài phạm vi bàn cờ", () -> {
            Board board = new Board(9, 9, 10, 1);

            assertThrows(IllegalArgumentException.class, () -> board.getCell(9, 0));
        });
    }
}
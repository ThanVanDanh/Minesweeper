package minesweeper.controller;

import minesweeper.model.Board;
import minesweeper.model.Difficulty;
import minesweeper.model.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameControllerTest {

    private GameController gameController;

    @BeforeEach
    public void setUp() {
        // Khởi tạo lại GameController trước mỗi test case
        gameController = new GameController();
    }

    /**
     * Mã TC: GAME_01
     * Tên Test Case: Khởi tạo mức Dễ
     * Mục tiêu: Đảm bảo khi chọn mức EASY, Board được tạo với đúng số hàng, cột, mìn và trạng thái ban đầu.
     */
    @Test
    public void testGame01_InitEasyDifficulty() {
        // Các bước thực hiện
        gameController.startNewGame(Difficulty.EASY);
        Board board = gameController.getBoard();

        // Kết quả mong đợi
        assertNotNull(board, "Board không được null");
        assertEquals(Difficulty.EASY, gameController.getDifficulty(), "Độ khó phải là EASY");
        assertEquals(9, board.getRows(), "Số hàng mức Dễ phải là 9");
        assertEquals(9, board.getCols(), "Số cột mức Dễ phải là 9");
        assertEquals(10, board.getTotalMines(), "Số mìn mức Dễ phải là 10");
        assertEquals(GameState.IDLE, gameController.getGameState(), "Trạng thái ban đầu phải là IDLE (chưa click)");
        assertFalse(gameController.isPaused(), "Game không được ở trạng thái tạm dừng khi mới bắt đầu");
    }
    /**
     * Khởi tạo mức Trung Bình (MEDIUM)
     * Mục tiêu: Đảm bảo Board được tạo với 16 hàng, 16 cột và 40 mìn.
     */
    @Test
    public void testGame01_InitMediumDifficulty() {
        gameController.startNewGame(Difficulty.MEDIUM);
        Board board = gameController.getBoard();

        assertNotNull(board, "Board không được null");
        assertEquals(Difficulty.MEDIUM, gameController.getDifficulty(), "Độ khó phải là MEDIUM");
        assertEquals(16, board.getRows(), "Số hàng mức Trung Bình phải là 16");
        assertEquals(16, board.getCols(), "Số cột mức Trung Bình phải là 16");
        assertEquals(40, board.getTotalMines(), "Số mìn mức Trung Bình phải là 40");
        assertEquals(GameState.IDLE, gameController.getGameState(), "Trạng thái ban đầu phải là IDLE");
    }

    /**
     * Khởi tạo mức Khó (HARD)
     * Mục tiêu: Đảm bảo Board được tạo với 16 hàng, 30 cột và 99 mìn.
     */
    @Test
    public void testGame01_InitHardDifficulty() {
        gameController.startNewGame(Difficulty.HARD);
        Board board = gameController.getBoard();

        assertNotNull(board, "Board không được null");
        assertEquals(Difficulty.HARD, gameController.getDifficulty(), "Độ khó phải là HARD");
        assertEquals(16, board.getRows(), "Số hàng mức Khó phải là 16");
        assertEquals(30, board.getCols(), "Số cột mức Khó phải là 30");
        assertEquals(99, board.getTotalMines(), "Số mìn mức Khó phải là 99");
        assertEquals(GameState.IDLE, gameController.getGameState(), "Trạng thái ban đầu phải là IDLE");
    }

    /**
     * Khởi tạo mức Chuyên Gia (EXPERT)
     * Mục tiêu: Đảm bảo Board được tạo với 20 hàng, 30 cột và 145 mìn.
     */
    @Test
    public void testGame01_InitExpertDifficulty() {
        gameController.startNewGame(Difficulty.EXPERT);
        Board board = gameController.getBoard();

        assertNotNull(board, "Board không được null");
        assertEquals(Difficulty.EXPERT, gameController.getDifficulty(), "Độ khó phải là EXPERT");
        assertEquals(20, board.getRows(), "Số hàng mức Chuyên Gia phải là 20");
        assertEquals(30, board.getCols(), "Số cột mức Chuyên Gia phải là 30");
        assertEquals(145, board.getTotalMines(), "Số mìn mức Chuyên Gia phải là 145");
        assertEquals(GameState.IDLE, gameController.getGameState(), "Trạng thái ban đầu phải là IDLE");
    }

    /**
     * Mã TC: GAME_02
     * Tên Test Case: Bắt đầu tính giờ (và bắt đầu game)
     * Mục tiêu: Đồng hồ trong UI bắt đầu đếm khi game bắt đầu.
     * Ở tầng logic, game chính thức chuyển sang trạng thái PLAYING và sinh mìn khi người chơi click ô đầu tiên.
     */
    @Test
    public void testGame02_StartGameOnFirstClick() {
        // Khởi tạo game
        gameController.startNewGame(Difficulty.EASY);
        assertEquals(GameState.IDLE, gameController.getGameState(), "Trạng thái trước khi click phải là IDLE");

        // Các bước thực hiện: Mô phỏng người chơi click mở ô đầu tiên tại tọa độ (0, 0)
        gameController.reveal(0, 0);

        // Kết quả mong đợi: Hệ thống sinh mìn và đổi trạng thái sang PLAYING (đồng nghĩa thời gian đang chạy)
        assertEquals(GameState.PLAYING, gameController.getGameState(), "Trạng thái game phải chuyển sang PLAYING sau click đầu tiên");
    }

    /**
     * Mã TC: GAME_03
     * Tên Test Case: Tạm dừng game
     * Mục tiêu: Kiểm tra cờ trạng thái isPaused được bật thành true.
     */
    @Test
    public void testGame03_PauseGame() {
        // Khởi tạo game
        gameController.startNewGame(Difficulty.EASY);

        // Đảm bảo game đang không tạm dừng
        assertFalse(gameController.isPaused());

        // Các bước thực hiện: Gọi hàm tạm dừng
        gameController.setPaused(true);

        // Kết quả mong đợi
        assertTrue(gameController.isPaused(), "Trạng thái isPaused phải là true sau khi gọi hàm tạm dừng");
    }

    /**
     * Mã TC: GAME_04
     * Tên Test Case: Tiếp tục game
     * Mục tiêu: Kiểm tra cờ trạng thái isPaused được tắt thành false sau khi đang tạm dừng.
     */
    @Test
    public void testGame04_ResumeGame() {
        // Khởi tạo game
        gameController.startNewGame(Difficulty.EASY);

        // Giả lập người chơi bấm tạm dừng trước
        gameController.setPaused(true);
        assertTrue(gameController.isPaused(), "Game phải đang ở trạng thái tạm dừng");

        // Các bước thực hiện: Người chơi bấm tiếp tục
        gameController.setPaused(false);

        // Kết quả mong đợi
        assertFalse(gameController.isPaused(), "Trạng thái isPaused phải trở về false sau khi tiếp tục game");
    }
}
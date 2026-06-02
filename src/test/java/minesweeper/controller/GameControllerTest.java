package minesweeper.controller;

import minesweeper.model.Board;
import minesweeper.model.enums.Difficulty;
import minesweeper.model.enums.GameState;
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

    @Test
    public void testGame01_InitCustomBoard() {
        gameController.startCustomGame(12, 14, 25, 3);
        Board board = gameController.getBoard();

        assertNotNull(board, "Board tùy chỉnh không được null");
        assertNull(gameController.getDifficulty(), "Bàn tùy chỉnh không gắn với độ khó cố định");
        assertTrue(gameController.isCustomGame(), "Game phải được đánh dấu là bàn tùy chỉnh");
        assertEquals(12, board.getRows(), "Số hàng tùy chỉnh phải đúng");
        assertEquals(14, board.getCols(), "Số cột tùy chỉnh phải đúng");
        assertEquals(25, board.getTotalMines(), "Số mìn tùy chỉnh phải đúng");
        assertEquals(3, board.getPlayerCount(), "Số người chơi tùy chỉnh phải đúng");
        assertEquals(GameState.IDLE, gameController.getGameState(), "Trạng thái ban đầu phải là IDLE");
    }

    @Test
    public void testGame01_CustomBoardRejectsTooManyMines() {
        assertThrows(IllegalArgumentException.class,
                () -> gameController.startCustomGame(3, 3, 9, 2),
                "Số mìn phải nhỏ hơn tổng số ô để luôn còn ô an toàn");
    }

    @Test
    public void testGame01_CustomBoardRejectsMoreThanFourPlayers() {
        assertThrows(IllegalArgumentException.class,
                () -> gameController.startCustomGame(9, 9, 10, 5),
                "Số người chơi tối đa phải là 4");
    }

    @Test
    public void testGame01_DifficultyBoardCanUseMultiplePlayers() {
        gameController.startNewGame(Difficulty.HARD, 4);

        assertEquals(Difficulty.HARD, gameController.getDifficulty(), "Độ khó phải là HARD");
        assertEquals(4, gameController.getPlayerCount(), "Bàn theo độ khó cũng phải nhận số người chơi tùy chọn");
        assertEquals(4, gameController.getPlayerScores().length, "Phải có điểm riêng cho 4 người chơi");
    }

    @Test
    public void testGame02_MultiplayerScoreAndTurnAfterReveal() {
        gameController.startCustomGame(2, 2, 1, 2);

        int openedCells = gameController.reveal(0, 0);

        assertEquals(1, openedCells, "Bàn 2x2 với 1 mìn chỉ mở 1 ô ở lượt đầu");
        assertEquals(10, gameController.getPlayerScore(1), "Mỗi ô mở được cộng 10 điểm");
        assertEquals(0, gameController.getPlayerScore(2), "Người chơi chưa tới lượt chưa có điểm");
        assertEquals(2, gameController.getCurrentPlayerNumber(), "Sau lượt mở ô an toàn phải chuyển sang P2");
    }

    @Test
    public void testGame02_MultiplayerFlagUsesTurnWithoutScoring() {
        gameController.startCustomGame(2, 2, 1, 2);
        gameController.reveal(0, 0);

        boolean changed = gameController.toggleFlag(0, 1);

        assertTrue(changed, "Cắm cờ hợp lệ phải được tính là một lượt");
        assertEquals(10, gameController.getPlayerScore(1), "P1 giữ nguyên điểm sau lượt P2 cắm cờ");
        assertEquals(0, gameController.getPlayerScore(2), "Cắm cờ không cộng điểm");
        assertEquals(1, gameController.getCurrentPlayerNumber(), "Sau P2 cắm cờ phải quay lại P1");
    }

    @Test
    public void testGame02_MultiplayerTimeoutSkipsTurn() {
        gameController.startCustomGame(9, 9, 10, 3);

        boolean skipped = gameController.skipCurrentTurn();

        assertTrue(skipped, "Hết giờ phải làm người chơi hiện tại mất lượt");
        assertEquals(2, gameController.getCurrentPlayerNumber(), "Sau khi P1 hết giờ phải chuyển sang P2");
        assertEquals(-50, gameController.getPlayerScore(1), "Hết giờ phải trừ 50 điểm người chơi hiện tại");
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

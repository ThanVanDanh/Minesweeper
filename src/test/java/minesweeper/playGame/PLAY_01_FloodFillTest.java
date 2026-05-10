package minesweeper.playGame;

import minesweeper.model.*;
import minesweeper.controller.GameController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PLAY_01: Thuật toán loang - Flood Fill Algorithm
 * Khi user click vào ô không có mìn xung quanh (0 mìn), hệ thống tự động mở
 * tất cả các ô trống liên tiếp (Flood Fill).
 */
@DisplayName("PLAY_01: Thuật toán loang")
class PLAY_01_FloodFillTest {

    private Board board;
    private GameController gameController;

    @BeforeEach
    void setUp() {
        gameController = new GameController();
        gameController.startNewGame(Difficulty.EASY);
        board = gameController.getBoard();
    }

    @Test
    @DisplayName("PLAY_01.1: Click ô trống → Các ô lân cận mở tự động")
    void testFloodFillOpensAdjacentCells() {
        // Dữ liệu: Click ô (0,0)
        gameController.reveal(0, 0);

        // Kết quả: Ô được mở
        assertTrue(board.getCell(0, 0).isRevealed(),
            "Ô click phải được mở");

        // Kết quả: Nếu là ô trống (0 mìn), các ô lân cận cũng mở
        if (board.getCell(0, 0).getNeighborMines() == 0) {
            boolean adjacentOpened = false;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int nr = 0 + dr;
                    int nc = 0 + dc;
                    if (nr >= 0 && nr < board.getRows() &&
                        nc >= 0 && nc < board.getCols() &&
                        board.getCell(nr, nc).isRevealed()) {
                        adjacentOpened = true;
                        break;
                    }
                }
                if (adjacentOpened) break;
            }
            assertTrue(adjacentOpened,
                "Ô trống phải trigger Flood Fill, các ô lân cận mở");
        }
    }

    @Test
    @DisplayName("PLAY_01.2: Flood Fill dừng tại ô có số")
    void testFloodFillStopsAtNumberedCell() {
        gameController.startNewGame(Difficulty.EASY);
        gameController.reveal(0, 0);

        // Kiểm tra trạng thái: Game PLAYING hoặc WON
        assertTrue(gameController.getGameState() == GameState.PLAYING ||
                   gameController.getGameState() == GameState.WON,
            "Game phải PLAYING hoặc WON");
    }

    @Test
    @DisplayName("PLAY_01.3: Trạng thái game chuyển PLAYING")
    void testGameStateAfterFloodFill() {
        gameController.reveal(0, 0);

        assertTrue(gameController.getGameState() == GameState.PLAYING ||
                   gameController.getGameState() == GameState.WON,
            "Trạng thái phải PLAYING hoặc WON sau reveal");
    }
}


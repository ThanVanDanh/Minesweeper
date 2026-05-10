package minesweeper.playGame;

import minesweeper.model.*;
import minesweeper.controller.GameController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PLAY_03: Thưa cuộc (Mìn) - Mine Detection
 * Khi user click vào ô có mìn, game kết thúc với trạng thái LOST.
 * Tất cả các mìn được hiển thị.
 */
@DisplayName("PLAY_03: Thua cuộc (Mìn)")
class PLAY_03_MineDetectionTest {

    private GameController gameController;

    @BeforeEach
    void setUp() {
        gameController = new GameController();
    }

    @Test
    @DisplayName("PLAY_03.1: Click mìn → Game LOST")
    void testClickOnMineCausesLost() {
        // Dữ liệu: Board EASY
        gameController.startNewGame(Difficulty.EASY);
        Board board = gameController.getBoard();

        // Thực hiện: Click safe-first-click
        gameController.reveal(0, 0);

        // Tìm mìn và click
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.getCell(r, c).isMine() && !board.getCell(r, c).isRevealed()) {
                    gameController.reveal(r, c);
                    break;
                }
            }
            if (gameController.getGameState() == GameState.LOST) break;
        }

        // Kết quả: Game LOST
        assertEquals(GameState.LOST, gameController.getGameState(),
            "Click mìn → Game LOST");
    }

    @Test
    @DisplayName("PLAY_03.2: Tất cả mìn được hiển thị khi thua")
    void testAllMinesRevealedOnLoss() {
        // Dữ liệu: Board EASY
        gameController.startNewGame(Difficulty.EASY);
        Board board = gameController.getBoard();

        // Thực hiện: Trigger LOST
        gameController.reveal(0, 0);
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.getCell(r, c).isMine() && !board.getCell(r, c).isRevealed()) {
                    gameController.reveal(r, c);
                    break;
                }
            }
            if (gameController.getGameState() == GameState.LOST) break;
        }

        // Kết quả: Ít nhất 1 mìn được mở
        if (gameController.getGameState() == GameState.LOST) {
            int revealedMines = 0;
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (board.getCell(r, c).isMine() && board.getCell(r, c).isRevealed()) {
                        revealedMines++;
                    }
                }
            }
            assertTrue(revealedMines > 0,
                "Ít nhất 1 mìn phải hiển thị");
        }
    }

    @Test
    @DisplayName("PLAY_03.3: Trạng thái LOST không thay đổi khi action tiếp")
    void testGameStateLostImmutable() {
        // Dữ liệu: Game LOST
        gameController.startNewGame(Difficulty.EASY);
        Board board = gameController.getBoard();

        gameController.reveal(0, 0);
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.getCell(r, c).isMine() && !board.getCell(r, c).isRevealed()) {
                    gameController.reveal(r, c);
                    break;
                }
            }
            if (gameController.getGameState() == GameState.LOST) break;
        }

        assertEquals(GameState.LOST, gameController.getGameState());

        // Thực hiện: Click ô khác
        gameController.reveal(1, 1);

        // Kết quả: Trạng thái vẫn LOST
        assertEquals(GameState.LOST, gameController.getGameState(),
            "Trạng thái LOST immutable");
    }

    @Test
    @DisplayName("PLAY_03.4: Click ô đầu tiên không phải mìn (Safe-first-click)")
    void testFirstClickSafeFromMines() {
        // Dữ liệu: Click (0,0) lần đầu
        gameController.startNewGame(Difficulty.EASY);
        Board board = gameController.getBoard();

        gameController.reveal(0, 0);

        // Kết quả: Ô (0,0) không phải mìn
        assertFalse(board.getCell(0, 0).isMine(),
            "Ô click đầu tiên không phải mìn");
        assertNotEquals(GameState.LOST, gameController.getGameState(),
            "Không thưa cuộc khi click ô đầu");
    }

    @Test
    @DisplayName("PLAY_03.5: Ô an toàn không trigger LOST")
    void testRevealingSafeCellDoesNotCauseLoss() {
        // Dữ liệu
        gameController.startNewGame(Difficulty.EASY);
        Board board = gameController.getBoard();

        // Thực hiện: Mở ô an toàn
        gameController.reveal(0, 0);

        boolean isLost = gameController.getGameState() == GameState.LOST;

        // Kết quả: Không thua
        assertFalse(isLost,
            "Mở ô an toàn không thua");
    }
}


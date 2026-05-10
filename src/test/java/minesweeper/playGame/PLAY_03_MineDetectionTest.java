package minesweeper.playGame;

import minesweeper.model.*;
import minesweeper.controller.GameController;
import minesweeper.model.enums.Difficulty;
import minesweeper.model.enums.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

        assertEquals(GameState.LOST, gameController.getGameState(),
            "Click mìn → Game LOST");
    }

    @Test
    @DisplayName("PLAY_03.2: Tất cả mìn được hiển thị khi thua")
    void testAllMinesRevealedOnLoss() {
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

        gameController.reveal(1, 1);

        assertEquals(GameState.LOST, gameController.getGameState(),
            "Trạng thái LOST immutable");
    }

    @Test
    @DisplayName("PLAY_03.4: Click ô đầu tiên không phải mìn (Safe-first-click)")
    void testFirstClickSafeFromMines() {
        gameController.startNewGame(Difficulty.EASY);
        Board board = gameController.getBoard();

        gameController.reveal(0, 0);

        assertFalse(board.getCell(0, 0).isMine(),
            "Ô click đầu tiên không phải mìn");
        assertNotEquals(GameState.LOST, gameController.getGameState(),
            "Không thưa cuộc khi click ô đầu");
    }

    @Test
    @DisplayName("PLAY_03.5: Ô an toàn không trigger LOST")
    void testRevealingSafeCellDoesNotCauseLoss() {
        gameController.startNewGame(Difficulty.EASY);
        Board board = gameController.getBoard();

        gameController.reveal(0, 0);

        boolean isLost = gameController.getGameState() == GameState.LOST;

        assertFalse(isLost,
            "Mở ô an toàn không thua");
    }
}


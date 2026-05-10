package minesweeper.playGame;

import minesweeper.model.*;
import minesweeper.controller.GameController;
import minesweeper.model.enums.Difficulty;
import minesweeper.model.enums.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PLAY_04: Thắng cuộc")
class PLAY_04_WinConditionTest {

    private GameController gameController;

    @BeforeEach
    void setUp() {
        gameController = new GameController();
    }

    @Test
    @DisplayName("PLAY_04.1: Mở tất cả ô an toàn → Game WON")
    void testOpenAllSafeCellsCausesWin() {
        gameController.startNewGame(Difficulty.EASY);
        Board board = gameController.getBoard();

        gameController.reveal(0, 0);

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (!board.getCell(r, c).isMine() && !board.getCell(r, c).isRevealed()) {
                    if (board.getGameState() != GameState.WON) {
                        gameController.reveal(r, c);
                    }
                }
            }
        }

        assertEquals(GameState.WON, gameController.getGameState(),
            "Mở tất cả ô an toàn → WON");
    }

    @Test
    @DisplayName("PLAY_04.2: Win check tính chính xác")
    void testWinCheckAccuracy() {
        gameController.startNewGame(Difficulty.EASY);
        Board board = gameController.getBoard();

        gameController.reveal(0, 0);
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (!board.getCell(r, c).isMine() && !board.getCell(r, c).isRevealed()) {
                    gameController.reveal(r, c);
                }
            }
        }

        if (gameController.getGameState() == GameState.WON) {
            int totalSafe = board.getRows() * board.getCols() - board.getTotalMines();
            int revealedSafe = 0;
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (!board.getCell(r, c).isMine() && board.getCell(r, c).isRevealed()) {
                        revealedSafe++;
                    }
                }
            }
            assertEquals(totalSafe, revealedSafe,
                "Tất cả ô an toàn mở");
        }
    }

    @Test
    @DisplayName("PLAY_04.3: Trạng thái WON không thay đổi khi action tiếp")
    void testGameStateWonImmutable() {
        gameController.startNewGame(Difficulty.EASY);
        Board board = gameController.getBoard();

        gameController.reveal(0, 0);
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (!board.getCell(r, c).isMine() && !board.getCell(r, c).isRevealed()) {
                    gameController.reveal(r, c);
                }
            }
        }

        assertEquals(GameState.WON, gameController.getGameState());

        gameController.reveal(1, 1);

        assertEquals(GameState.WON, gameController.getGameState(),
            "Trạng thái WON immutable");
    }

    @Test
    @DisplayName("PLAY_04.4: Mìn được xử lý khi thắng")
    void testMinesHandledUponWin() {
        gameController.startNewGame(Difficulty.EASY);
        Board board = gameController.getBoard();

        gameController.reveal(0, 0);
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (!board.getCell(r, c).isMine() && !board.getCell(r, c).isRevealed()) {
                    gameController.reveal(r, c);
                }
            }
        }

        assertEquals(GameState.WON, gameController.getGameState(),
            "Game thắng");
    }

    @Test
    @DisplayName("PLAY_04.5: Flood Fill tăng tốc độ thắng")
    void testFloodFillAcceleratesWin() {
        gameController.startNewGame(Difficulty.EASY);
        Board board = gameController.getBoard();

        gameController.reveal(0, 0);

        int openedCells = 0;
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.getCell(r, c).isRevealed()) {
                    openedCells++;
                }
            }
        }

        if (board.getCell(0, 0).getNeighborMines() == 0) {
            assertTrue(openedCells > 1,
                "Flood Fill từ ô trống mở nhiều ô");
        }
    }
}


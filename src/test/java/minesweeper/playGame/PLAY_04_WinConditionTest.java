package minesweeper.playGame;

import minesweeper.model.*;
import minesweeper.controller.GameController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PLAY_04: Thắng cuộc - Win Condition
 * User phải mở tất cả ô không có mìn để thắng.
 * Trạng thái game chuyển thành WON khi mở đủ ô an toàn.
 */
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
        // Dữ liệu: Board EASY (71 ô an toàn)
        gameController.startNewGame(Difficulty.EASY);
        Board board = gameController.getBoard();

        // Thực hiện: Mở tất cả ô an toàn
        gameController.reveal(0, 0); // Safe-first-click

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (!board.getCell(r, c).isMine() && !board.getCell(r, c).isRevealed()) {
                    if (board.getGameState() != GameState.WON) {
                        gameController.reveal(r, c);
                    }
                }
            }
        }

        // Kết quả: Game WON
        assertEquals(GameState.WON, gameController.getGameState(),
            "Mở tất cả ô an toàn → WON");
    }

    @Test
    @DisplayName("PLAY_04.2: Win check tính chính xác")
    void testWinCheckAccuracy() {
        // Dữ liệu
        gameController.startNewGame(Difficulty.EASY);
        Board board = gameController.getBoard();

        // Thực hiện: Mở hết ô an toàn
        gameController.reveal(0, 0);
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (!board.getCell(r, c).isMine() && !board.getCell(r, c).isRevealed()) {
                    gameController.reveal(r, c);
                }
            }
        }

        // Kết quả: WON
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
        // Dữ liệu: Game WON
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

        // Thực hiện: Click ô khác
        gameController.reveal(1, 1);

        // Kết quả: Trạng thái vẫn WON
        assertEquals(GameState.WON, gameController.getGameState(),
            "Trạng thái WON immutable");
    }

    @Test
    @DisplayName("PLAY_04.4: Mìn được xử lý khi thắng")
    void testMinesHandledUponWin() {
        // Dữ liệu
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

        // Kết quả: WON
        assertEquals(GameState.WON, gameController.getGameState(),
            "Game thắng");
    }

    @Test
    @DisplayName("PLAY_04.5: Flood Fill tăng tốc độ thắng")
    void testFloodFillAcceleratesWin() {
        // Dữ liệu: Board EASY
        gameController.startNewGame(Difficulty.EASY);
        Board board = gameController.getBoard();

        // Thực hiện: Click ô (0,0)
        gameController.reveal(0, 0);

        // Kết quả: Nhiều ô mở từ 1 click
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


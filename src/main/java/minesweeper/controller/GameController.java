package minesweeper.controller;

import minesweeper.model.Board;
import minesweeper.model.enums.Difficulty;
import minesweeper.model.enums.GameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameController {
    private static final Logger LOG = LoggerFactory.getLogger(GameController.class);

    private Board board;
    private Difficulty difficulty;
    private boolean isPaused;

    public void startNewGame(Difficulty difficulty) {
        this.difficulty = difficulty;
        this.board = new Board(difficulty);
        this.isPaused = false;
        LOG.info("New game started with difficulty: {}", difficulty);
    }

    public boolean hasGame() {
        return board != null;
    }

    public Board getBoard() {
        return board;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public GameState getGameState() {
        return board == null ? GameState.IDLE : board.getGameState();
    }

    // 3.1 MỞ Ô: tầng điều phối nhận yêu cầu từ BoardGameController
    public void reveal(int row, int col) {
        if (board == null) {
            LOG.warn("Attempted to reveal cell ({}, {}) with no active game", row, col);
            return;
        }
        // Nếu GameState == IDLE (Chưa rải mìn), tiến hành rải mìn tránh ô click đầu
        if (board.getGameState() == GameState.IDLE) {
            board.placeMines(row, col);
        }
        LOG.debug("Revealing cell: ({}, {})", row, col);

        // Gọi lệnh reveal() của Board
        board.reveal(row, col);
    }

    // 03.2.1 UC03.1 - CẮM / GỠ CỜ
    public void toggleFlag(int row, int col) {
        if (board == null) {
            LOG.warn("Attempted to toggle flag at ({}, {}) with no active game", row, col);
            return;
        }
        LOG.debug("Toggling flag at cell: ({}, {})", row, col);
        // 03.2.1.2: Chuyển tiếp lệnh gọi hàm
        board.toggleFlag(row, col);
    }

    // 03.2.2 UC03.2 - MỞ NHANH (Fast Reveal / Chording)
    public void fastReveal(int row, int col) {
        // 03.2.2.2: Chuyển tiếp lệnh gọi hàm
        if (board != null) board.fastReveal(row, col);
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }
}
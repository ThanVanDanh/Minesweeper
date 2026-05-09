package minesweeper.controller;

import minesweeper.model.Board;
import minesweeper.model.Difficulty;
import minesweeper.model.GameState;
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
        this.board.placeMinesNow();
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

    public void reveal(int row, int col) {
        if (board == null) {
            LOG.warn("Attempted to reveal cell ({}, {}) with no active game", row, col);
            return;
        }
        LOG.debug("Revealing cell: ({}, {})", row, col);
        board.reveal(row, col);

}

    public void toggleFlag(int row, int col) {
        if (board == null) {
            LOG.warn("Attempted to toggle flag at ({}, {}) with no active game", row, col);
            return;
        }
        LOG.debug("Toggling flag at cell: ({}, {})", row, col);

            board.toggleFlag(row, col); // UC10
        }

    public void fastReveal(int row, int col) {
        if (board != null) board.fastReveal(row, col); // UC13
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }
}


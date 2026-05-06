package minesweeper.controller;

import minesweeper.model.Board;
import minesweeper.model.Difficulty;
import minesweeper.model.GameState;

public class GameController {
    private Board board;
    private Difficulty difficulty;

    public void startNewGame(Difficulty difficulty) {
        this.difficulty = difficulty;
        this.board = new Board(difficulty);
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
            return;
        }
        board.reveal(row, col);
    }

    public void toggleFlag(int row, int col) {
        if (board == null) {
            return;
        }
        board.toggleFlag(row, col);
    }
}


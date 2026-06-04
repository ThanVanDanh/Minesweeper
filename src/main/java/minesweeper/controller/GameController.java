package minesweeper.controller;

import minesweeper.model.Board;
import minesweeper.model.enums.Difficulty;
import minesweeper.model.enums.GameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class GameController {
    private static final Logger LOG = LoggerFactory.getLogger(GameController.class);
    private static final int SCORE_PER_OPENED_CELL = 10;
    private static final int TIMEOUT_PENALTY_POINTS = 50;

    private Board board;
    private Difficulty difficulty;
    private boolean isPaused;
    private boolean customGame;
    private int currentPlayerIndex;
    private int[] playerScores = {0};

    //UC04 - Bắt đầu ván mới.
    public void startNewGame(Difficulty difficulty) {
        startNewGame(difficulty, Board.MIN_PLAYER_COUNT);
    }

    public void startNewGame(Difficulty difficulty, int playerCount) {
        // UC03 - Chọn độ khó
        this.difficulty = difficulty;
        // UC04 - Bắt đầu ván mới
        this.board = new Board(difficulty, playerCount);
        this.customGame = false;
        resetPlayerState(board.getPlayerCount());
        // UC05/UC06 - Tạm dừng / Tiếp tục ván game
        this.isPaused = false;
        LOG.info("New game started with difficulty: {}, players={}", difficulty, playerCount);
    }

    public void startCustomGame(int rows, int cols, int mines, int playerCount) {
        this.difficulty = null;
        this.board = new Board(rows, cols, mines, playerCount);
        this.customGame = true;
        resetPlayerState(board.getPlayerCount());
        this.isPaused = false;
        LOG.info("New custom game started: {}x{}, mines={}, players={}", rows, cols, mines, playerCount);
    }

    public void restartCurrentGame() {
        if (board == null) {
            return;
        }
        if (customGame) {
            startCustomGame(board.getRows(), board.getCols(), board.getTotalMines(), board.getPlayerCount());
        } else if (difficulty != null) {
            startNewGame(difficulty, board.getPlayerCount());
        }
    }

    public boolean hasGame() {
        return board != null;
    }

    public Board getBoard() {
        return board;
    }

    //UC03 - Chọn độ khó.
    public Difficulty getDifficulty() {
        return difficulty;
    }

    public boolean isCustomGame() {
        return customGame;
    }

    public int getPlayerCount() {
        return board == null ? 1 : board.getPlayerCount();
    }

    public int getCurrentPlayerNumber() {
        return currentPlayerIndex + 1;
    }

    public int[] getPlayerScores() {
        return Arrays.copyOf(playerScores, playerScores.length);
    }

    public int getPlayerScore(int playerNumber) {
        if (playerNumber < 1 || playerNumber > playerScores.length) {
            throw new IllegalArgumentException("Invalid player number: " + playerNumber);
        }
        return playerScores[playerNumber - 1];
    }

    public GameState getGameState() {
        return board == null ? GameState.IDLE : board.getGameState();
    }

    // 3.1 MỞ Ô: tầng điều phối nhận yêu cầu từ BoardGameController
    public int reveal(int row, int col) {
        if (board == null) {
            LOG.warn("Attempted to reveal cell ({}, {}) with no active game", row, col);
            return 0;
        }
        // Nếu GameState == IDLE (Chưa rải mìn), tiến hành rải mìn tránh ô click đầu
        if (board.getGameState() == GameState.IDLE) {
            board.placeMines(row, col);
        }
        LOG.debug("Revealing cell: ({}, {})", row, col);

        int openedBefore = countRevealedSafeCells();
        // Gọi lệnh reveal() của Board
        board.reveal(row, col);
        int newlyOpened = countRevealedSafeCells() - openedBefore;
        applyScoreAndAdvanceTurn(newlyOpened);
        return newlyOpened;
    }

    // 03.2.1 UC03.1 - CẮM / GỠ CỜ
    public boolean toggleFlag(int row, int col) {
        if (board == null) {
            LOG.warn("Attempted to toggle flag at ({}, {}) with no active game", row, col);
            return false;
        }
        if (!isInBounds(row, col) || board.getGameState() != GameState.PLAYING) {
            return false;
        }

        minesweeper.model.Cell cell = board.getCell(row, col);
        if (cell.isRevealed()) {
            return false;
        }
        LOG.debug("Toggling flag at cell: ({}, {})", row, col);
        // 03.2.1.2: Chuyển tiếp lệnh gọi hàm
        boolean wasFlagged = cell.isFlagged();
        int flagsBefore = board.getFlagsPlaced();
        board.toggleFlag(row, col);
        boolean changed = wasFlagged != cell.isFlagged() || flagsBefore != board.getFlagsPlaced();
        if (changed) {
            advanceTurn();
        }
        return changed;
    }

    // 03.2.2 UC03.2 - MỞ NHANH (Fast Reveal / Chording)
    public int fastReveal(int row, int col) {
        // 03.2.2.2: Chuyển tiếp lệnh gọi hàm
        if (board == null) {
            return 0;
        }
        int openedBefore = countRevealedSafeCells();
        board.fastReveal(row, col);
        int newlyOpened = countRevealedSafeCells() - openedBefore;
        applyScoreAndAdvanceTurn(newlyOpened);
        return newlyOpened;
    }

    public boolean isPaused() {
        return isPaused;
    }
    // UC05/UC06 - Tạm dừng/ tiếp tục ván game
    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    public boolean skipCurrentTurn() {
        if (board == null || board.getGameState() == GameState.LOST || board.getGameState() == GameState.WON) {
            return false;
        }
        playerScores[currentPlayerIndex] -= TIMEOUT_PENALTY_POINTS;
        advanceTurn();
        return true;
    }

    private void resetPlayerState(int playerCount) {
        this.currentPlayerIndex = 0;
        this.playerScores = new int[playerCount];
    }

    private int countRevealedSafeCells() {
        if (board == null) {
            return 0;
        }
        int count = 0;
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                minesweeper.model.Cell cell = board.getCell(r, c);
                if (cell.isRevealed() && !cell.isMine()) {
                    count++;
                }
            }
        }
        return count;
    }

    private void applyScoreAndAdvanceTurn(int newlyOpenedCells) {
        if (newlyOpenedCells <= 0) {
            return;
        }
        playerScores[currentPlayerIndex] += newlyOpenedCells * SCORE_PER_OPENED_CELL;
        if (board.getGameState() == GameState.PLAYING && playerScores.length > 1) {
            advanceTurn();
        }
    }

    private void advanceTurn() {
        if (playerScores.length > 1) {
            currentPlayerIndex = (currentPlayerIndex + 1) % playerScores.length;
        }
    }

    private boolean isInBounds(int row, int col) {
        return row >= 0 && board != null && row < board.getRows()
                && col >= 0 && col < board.getCols();
    }
    public void deductCurrentPlayerScore(int points) {
        if (playerScores != null && currentPlayerIndex >= 0 && currentPlayerIndex < playerScores.length) {
            playerScores[currentPlayerIndex] = Math.max(0, playerScores[currentPlayerIndex] - points);
        }
    }
}

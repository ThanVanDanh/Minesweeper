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

    //UC04 - Bắt đầu ván mới.
    public void startNewGame(Difficulty difficulty) {
        // UC03 - Chọn độ khó
        this.difficulty = difficulty;
        // UC04 - Bắt đầu ván mới
        this.board = new Board(difficulty);
        // UC05/UC06 - Tạm dừng / Tiếp tục ván game
        this.isPaused = false;
        LOG.info("New game started with difficulty: {}", difficulty);
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

    public GameState getGameState() {
        return board == null ? GameState.IDLE : board.getGameState();
    }
    // UC09 - Mở ô: tầng điều phối nhận yêu cầu từ BoardGameController,
    // đặt mìn sau click đầu tiên nếu ván còn IDLE, rồi chuyển xử lý cho Board.reveal().
    public void reveal(int row, int col) {
        if (board == null) {
            LOG.warn("Attempted to reveal cell ({}, {}) with no active game", row, col);
            return;
        }
        // UC09.4: Nếu ván đấu mới khởi động (IDLE), rải mìn ngẫu nhiên tránh ô click đầu tiên
        if (board.getGameState() == GameState.IDLE) {
            board.placeMines(row, col);
        }
        LOG.debug("Revealing cell: ({}, {})", row, col);
        // UC09.5: Chuyển tiếp tham số xử lý logic cốt lõi xuống phương thức của đối tượng Board
        board.reveal(row, col);
    }

    // UC10 - Tầng điều phối tiếp nhận yêu cầu cắm cờ
    public void toggleFlag(int row, int col) {
        if (board == null) {
            LOG.warn("Attempted to toggle flag at ({}, {}) with no active game", row, col);
            return;
        }
        LOG.debug("Toggling flag at cell: ({}, {})", row, col);
        // UC10.3: Chuyển giao nhiệm vụ đánh dấu ô sang phương thức xử lý logic của Board
        board.toggleFlag(row, col);
    }

    // UC13 - Tầng điều phối tiếp nhận yêu cầu mở ô hàng loạt
    public void fastReveal(int row, int col) {
        // UC13.3: Chuyển giao xử lý mở nhanh xung quanh ô số cho phương thức fastReveal() của Board
        if (board != null) board.fastReveal(row, col);
    }
    public boolean isPaused() {
        return isPaused;
    }
    // UC05/UC06 - Tạm dừng/ tiếp tục ván game
    public void setPaused(boolean paused) {
        isPaused = paused;
    }
}


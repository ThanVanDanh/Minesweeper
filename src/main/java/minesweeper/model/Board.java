package minesweeper.model;

import minesweeper.model.enums.Difficulty;
import minesweeper.model.enums.GameState;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

public class Board {
    public static final int MIN_PLAYER_COUNT = 1;
    public static final int MAX_PLAYER_COUNT = 4;

    private final Cell[][] grid;
    private final int rows;
    private final int cols;
    private final int totalMines;
    private final int playerCount;
    private int flagsPlaced;
    private GameState gameState;
    private boolean minesPlaced;
    private int safeCellsRemaining;
    private static final int[] DR = {-1, -1, 0, 1, 1, 1, 0, -1};
    private static final int[] DC = {0, 1, 1, 1, 0, -1, -1, -1};

    public Board(Difficulty difficulty) {
        this(difficulty, MIN_PLAYER_COUNT);
    }

    public Board(Difficulty difficulty, int playerCount) {
        this(difficulty.getRows(), difficulty.getCols(), difficulty.getMines(), playerCount);
    }

    public Board(int rows, int cols, int totalMines, int playerCount) {
        validateBoardConfig(rows, cols, totalMines, playerCount);
        this.rows = rows;
        this.cols = cols;
        this.totalMines = totalMines;
        this.playerCount = playerCount;
        this.flagsPlaced = 0;
        this.gameState = GameState.IDLE;
        this.minesPlaced = false;
        this.grid = new Cell[rows][cols];
        this.safeCellsRemaining = (this.rows * this.cols) - this.totalMines;
        initGrid();
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getTotalMines() {
        return totalMines;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public int getFlagsPlaced() {
        return flagsPlaced;
    }

    public int getRemainingMines() {
        return Math.max(0, totalMines - flagsPlaced);
    }

    public GameState getGameState() {
        return gameState;
    }

    public Cell getCell(int row, int col) {
        if (!isInBounds(row, col)) {
            throw new IllegalArgumentException("Cell out of bounds: " + row + "," + col);
        }
        return grid[row][col];
    }

    // 3.1 MỞ Ô (Basic Flow) - Phương thức thực thi logic lật mở ô cốt lõi
    public void reveal(int row, int col) {
        if (!isInBounds(row, col) || gameState != GameState.PLAYING) {
            return;
        }
        Cell cell = grid[row][col];

        if (cell.isRevealed() || cell.isFlagged()) {
            return;
        }

        boolean isMine = cell.isMine();

        if (isMine) {
            // 03.2.3 PHÁT HIỆN MÌN (Game Over - Thua cuộc)
            // 03.2.3.2: Kiểm tra isMine() == true và đổi trạng thái sang LOST
            gameState = GameState.LOST;
            // 03.2.3.3: Lộ diện toàn bộ mìn
            revealAllMines();
        } else {
            // 3.1 & 03.2.4.2: Ô an toàn tuyệt đối, lật mở ô (reveal) và giảm số ô an toàn còn lại
            cell.reveal();
            safeCellsRemaining--;

            // 03.2.4.2: Gọi hàm kiểm tra checkWinCondition()
            checkWinCondition();

            int count = cell.getNeighborMines();

            // 3.1: Nếu ô có 0 mìn lân cận (neighborMines == 0), kích hoạt thuật toán loang BFS mở vùng trống
            if (count == 0) {
                floodFill(row, col);
            }
        }
    }

    // 3.1 MỞ Ô - Thuật toán loang lan tỏa mở rộng ô trống tự động (BFS)
    private void floodFill(int startRow, int startCol) {
        Queue<Cell> queue = new ArrayDeque<>();
        queue.add(grid[startRow][startCol]);

        while (!queue.isEmpty()) {
            Cell current = queue.poll();

            for (int i = 0; i < 8; i++) {
                int nr = current.getRow() + DR[i];
                int nc = current.getCol() + DC[i];

                if (isInBounds(nr, nc)) {
                    Cell neighbor = grid[nr][nc];
                    if (!neighbor.isRevealed() && !neighbor.isFlagged() && !neighbor.isMine()) {
                        neighbor.reveal();
                        safeCellsRemaining--;
                        checkWinCondition(); // 03.2.4.2: Check lại đk thắng trong quá trình loang

                        int n = neighbor.getNeighborMines();
                        if (n == 0) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
    }

    // 03.2.2 UC03.2 - MỞ NHANH (Fast Reveal / Chording)
    public void fastReveal(int row, int col) {
        if (!isInBounds(row, col) || gameState != GameState.PLAYING) return;

        Cell cell = grid[row][col];
        if (!cell.isRevealed() || cell.getNeighborMines() == 0 || cell.isMine()) {
            return;
        }

        // 03.2.2.3: Đếm số cờ xung quanh ô đó
        int flagCount = 0;
        for (int i = 0; i < 8; i++) {
            int nr = row + DR[i];
            int nc = col + DC[i];
            if (isInBounds(nr, nc) && grid[nr][nc].isFlagged()) {
                flagCount++;
            }
        }
        int cellValue = cell.getNeighborMines();

        // 03.2.2.3: Nếu số cờ bằng đúng con số hiển thị trên ô
        if (flagCount == cellValue) {
            // Tự động mở tất cả các ô xung quanh chưa cắm cờ
            for (int i = 0; i < 8; i++) {
                int nr = row + DR[i];
                int nc = col + DC[i];
                if (isInBounds(nr, nc)) {
                    Cell neighbor = grid[nr][nc];
                    if (!neighbor.isRevealed() && !neighbor.isFlagged()) {
                        // Tái sử dụng logic Mở ô cơ bản
                        reveal(nr, nc);
                    }
                }
            }
        }
    }

    // 03.2.1 UC03.1 - CẮM / GỠ CỜ (Toggle Flag)
    public void toggleFlag(int row, int col) {
        if (!isInBounds(row, col) || gameState != GameState.PLAYING) return;

        Cell cell = grid[row][col];
        if (cell.isRevealed()) return;

        // 03.2.1.3: Board kiểm tra trạng thái ô
        if (cell.isFlagged()) {
            // Nếu ô đã cắm cờ: Gỡ cờ và tăng lại số cờ còn lại
            cell.setFlagged(false);
            flagsPlaced = Math.max(0, flagsPlaced - 1);
        } else {
            // Nếu ô chưa cắm cờ và số cờ đã đặt chưa vượt quá tổng số mìn: Đặt cờ cho ô
            if (flagsPlaced < totalMines) {
                cell.setFlagged(true);
                flagsPlaced++;
            }
        }
    }

    // 03.2.3.3: Hệ thống tự động lộ diện toàn bộ mìn trên bàn cờ
    private void revealAllMines() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = grid[r][c];
                if (cell.isMine()) {
                    cell.reveal();
                }
            }
        }
    }

    private void initGrid() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = new Cell(r, c);
            }
        }
    }

    // 3.1 MỞ Ô: Rải mìn tránh ô click đầu tiên
    public void placeMines(int safeRow, int safeCol) {
        Random random = new Random();
        int placed = 0;
        while (placed < totalMines) {
            int row = random.nextInt(rows);
            int col = random.nextInt(cols);
            if ((row == safeRow && col == safeCol) || grid[row][col].isMine()) {
                continue;
            }
            grid[row][col].setMine(true);
            placed++;
        }
        calculateNeighborMines(); // Tính toán NeighborMines
        minesPlaced = true;
        this.gameState = GameState.PLAYING;
    }

    // Tính toán số lượng mìn xung quanh (NeighborMines) cho từng ô
    private void calculateNeighborMines() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c].isMine()) {
                    grid[r][c].setNeighborMines(0);
                    continue;
                }
                int count = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) {
                            continue;
                        }
                        int nr = r + dr;
                        int nc = c + dc;
                        if (isInBounds(nr, nc) && grid[nr][nc].isMine()) {
                            count++;
                        }
                    }
                }
                grid[r][c].setNeighborMines(count);
            }
        }
    }

    // 03.2.4 ĐIỀU KIỆN THẮNG CUỘC
    private void checkWinCondition() {
        // 03.2.4.3: Nhận thấy số ô an toàn còn lại bằng 0, chuyển trạng thái sang WON
        if (safeCellsRemaining == 0 && gameState == GameState.PLAYING) {
            gameState = GameState.WON;
        }
    }

    private boolean isInBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    private static void validateBoardConfig(int rows, int cols, int totalMines, int playerCount) {
        if (rows < 2 || cols < 2) {
            throw new IllegalArgumentException("Board must have at least 2 rows and 2 columns");
        }
        if (totalMines <= 0) {
            throw new IllegalArgumentException("Board must contain at least 1 mine");
        }
        if (totalMines >= rows * cols) {
            throw new IllegalArgumentException("Mine count must be smaller than total cells");
        }
        if (playerCount < MIN_PLAYER_COUNT || playerCount > MAX_PLAYER_COUNT) {
            throw new IllegalArgumentException("Player count must be between "
                    + MIN_PLAYER_COUNT + " and " + MAX_PLAYER_COUNT);
        }
    }
}

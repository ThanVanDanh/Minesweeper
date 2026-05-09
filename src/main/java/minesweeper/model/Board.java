package minesweeper.model;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

public class Board {
    private final Cell[][] grid;
    private final int rows;
    private final int cols;
    private final int totalMines;
    private int flagsPlaced;
    private GameState gameState;
    private boolean minesPlaced;
    private int safeCellsRemaining;
    private static final int[] DR = {-1, -1, 0, 1, 1, 1, 0, -1};
    private static final int[] DC = {0, 1, 1, 1, 0, -1, -1, -1};

    public Board(Difficulty difficulty) {
        this.rows = difficulty.getRows();
        this.cols = difficulty.getCols();
        this.totalMines = difficulty.getMines();
        this.flagsPlaced = 0;
        this.gameState = GameState.IDLE;
        this.minesPlaced = false;
        this.grid = new Cell[rows][cols];
        this.safeCellsRemaining = (this.rows * this.cols) - this.totalMines;
        initGrid();
    }

//    public Board(int rows, int cols, int mines) {
//        this.rows = rows;
//        this.cols = cols;
//        this.totalMines = mines;
//        this.flagsPlaced = 0;
//        this.gameState = GameState.IDLE;
//        this.minesPlaced = false;
//        this.grid = new Cell[rows][cols];
//        this.safeCellsRemaining = (this.rows * this.cols) - this.totalMines;
//        initGrid();
//    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getTotalMines() {
        return totalMines;
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
//   UC09- Mở ô & UC14- Phát hiện mìn
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
        // Có mìn
        gameState = GameState.LOST;
        revealAllMines();
    } else {
        // Không có mìn
        cell.reveal();
        safeCellsRemaining--;
        checkWinCondition();
        int count = cell.getNeighborMines(); // UC12: Lấy số mìn xung quanh

        if (count == 0) {
            // Kích hoạt UC11 (Loang)
            floodFill(row, col);
        }
    }
}
// UC11-Flood Fill
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
                        neighbor.reveal(); // 7: Hiển thị ô đã mở
                        safeCellsRemaining--;
                        checkWinCondition();
                        int n = neighbor.getNeighborMines();
                        if (n == 0) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
    }
    // UC13 - Mở nhanh quanh ô
    public void fastReveal(int row, int col) {
        if (!isInBounds(row, col) || gameState != GameState.PLAYING) return;

        Cell cell = grid[row][col];

        if (!cell.isRevealed() || cell.getNeighborMines() == 0 || cell.isMine()) {
            return;
        }

        int flagCount = 0;
        for (int i = 0; i < 8; i++) {
            int nr = row + DR[i];
            int nc = col + DC[i];
            if (isInBounds(nr, nc) && grid[nr][nc].isFlagged()) {
                flagCount++;
            }
        }
        int cellValue = cell.getNeighborMines();

        if (flagCount == cellValue) {
            for (int i = 0; i < 8; i++) {
                int nr = row + DR[i];
                int nc = col + DC[i];
                if (isInBounds(nr, nc)) {
                    Cell neighbor = grid[nr][nc];
                    if (!neighbor.isRevealed() && !neighbor.isFlagged()) {
                        reveal(nr, nc); // Gọi lại UC09
                    }
                }
            }
        }
    }
    // UC10- Cắm cờ
    public void toggleFlag(int row, int col) {
        if (!isInBounds(row, col) || gameState != GameState.PLAYING) return;

        Cell cell = grid[row][col];
        if (cell.isRevealed()) return;

        if (cell.isFlagged()) {
            // [Đã có cờ] -> Gỡ cờ
            cell.setFlagged(false);
            flagsPlaced = Math.max(0, flagsPlaced - 1);
        } else {
            // [Chưa có cờ] -> Cắm cờ
            if (flagsPlaced < totalMines) {
                cell.setFlagged(true);
                flagsPlaced++;
            }
        }
    }
// UC15 - Hiển thị toàn bộ mìn khi thua
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
        calculateNeighborMines();
        minesPlaced = true;
        this.gameState = GameState.PLAYING;
    }

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


private void checkWinCondition() {
    if (safeCellsRemaining == 0 && gameState == GameState.PLAYING) {
        gameState = GameState.WON;
    }
}

    private boolean isInBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }
}


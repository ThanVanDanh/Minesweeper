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

    public Board(Difficulty difficulty) {
        this.rows = difficulty.getRows();
        this.cols = difficulty.getCols();
        this.totalMines = difficulty.getMines();
        this.flagsPlaced = 0;
        this.gameState = GameState.IDLE;
        this.minesPlaced = false;
        this.grid = new Cell[rows][cols];
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

    public void reveal(int row, int col) {
        if (!isInBounds(row, col) || gameState == GameState.WON || gameState == GameState.LOST) {
            return;
        }

        if (!minesPlaced) {
            placeMines(row, col);
            gameState = GameState.PLAYING;
        }

        Cell cell = grid[row][col];
        if (cell.isFlagged() || cell.isRevealed()) {
            return;
        }

        cell.reveal();
        if (cell.isMine()) {
            gameState = GameState.LOST;
            revealAllMines();
            return;
        }

        if (cell.getNeighborMines() == 0) {
            floodReveal(row, col);
        }

        if (checkWin()) {
            gameState = GameState.WON;
        }
    }

    public void toggleFlag(int row, int col) {
        if (!isInBounds(row, col) || gameState == GameState.WON || gameState == GameState.LOST) {
            return;
        }

        Cell cell = grid[row][col];
        if (cell.isRevealed()) {
            return;
        }

        if (cell.isFlagged()) {
            cell.setFlagged(false);
            flagsPlaced = Math.max(0, flagsPlaced - 1);
        } else if (flagsPlaced < totalMines) {
            cell.setFlagged(true);
            flagsPlaced++;
        }
    }

    private void initGrid() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = new Cell(r, c);
            }
        }
    }

    private void placeMines(int safeRow, int safeCol) {
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

    private void floodReveal(int startRow, int startCol) {
        Queue<Cell> queue = new ArrayDeque<>();
        queue.add(grid[startRow][startCol]);

        while (!queue.isEmpty()) {
            Cell current = queue.remove();
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) {
                        continue;
                    }
                    int nr = current.getRow() + dr;
                    int nc = current.getCol() + dc;
                    if (!isInBounds(nr, nc)) {
                        continue;
                    }
                    Cell neighbor = grid[nr][nc];
                    if (neighbor.isRevealed() || neighbor.isFlagged() || neighbor.isMine()) {
                        continue;
                    }
                    neighbor.reveal();
                    if (neighbor.getNeighborMines() == 0) {
                        queue.add(neighbor);
                    }
                }
            }
        }
    }

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

    private boolean checkWin() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = grid[r][c];
                if (!cell.isMine() && !cell.isRevealed()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isInBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }
}


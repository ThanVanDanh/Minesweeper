package minesweeper;

public class Cell {
    private final int row;
    private final int col;
    private boolean isMine;
    private boolean isRevealed;
    private boolean isFlagged;
    private int neighborMines; // Số mìn xung quanh (0–8)

    public Cell(int row, int col) {
        this.row          = row;
        this.col          = col;
        this.isMine       = false;
        this.isRevealed   = false;
        this.isFlagged    = false;
        this.neighborMines = 0;
    }
}

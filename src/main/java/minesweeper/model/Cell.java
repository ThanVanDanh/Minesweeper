package minesweeper.model;

public class Cell {
    private final int row;
    private final int col;
    private boolean mine;
    private boolean revealed;
    private boolean flagged;
    private int neighborMines;

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.mine = false;
        this.revealed = false;
        this.flagged = false;
        this.neighborMines = 0;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean isMine() {
        return mine;
    }

    public void setMine(boolean mine) {
        this.mine = mine;
    }

    public boolean isRevealed() {
        return revealed;
    }

    public void reveal() {
        this.revealed = true;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public int getNeighborMines() {
        return neighborMines;
    }

    public void setNeighborMines(int neighborMines) {
        this.neighborMines = neighborMines;
    }
}


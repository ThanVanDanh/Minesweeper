package minesweeper;
//chinh sua cho phu hop - Hoa

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
    }
}

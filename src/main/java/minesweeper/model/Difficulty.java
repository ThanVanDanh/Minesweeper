package minesweeper.model;

public enum Difficulty {
    EASY(9, 9, 10, "Dễ"),
    MEDIUM(16, 16, 40, "Trung Bình"),
    HARD(16, 30, 99, "Khó"),
    EXPERT(20, 30, 145, "Chuyên Gia"),
    CUSTOM(10, 10, 10, "Tùy Chỉnh");

    private final int rows;
    private final int cols;
    private final int mines;
    private final String label;

    Difficulty(int rows, int cols, int mines, String label) {
        this.rows = rows;
        this.cols = cols;
        this.mines = mines;
        this.label = label;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getMines() {
        return mines;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}


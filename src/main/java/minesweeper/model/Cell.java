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

    public void setMine(boolean mine) {
        this.mine = mine;
    }

    public boolean isRevealed() {
        return revealed;
    }


    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }


    public void setNeighborMines(int neighborMines) {
        this.neighborMines = neighborMines;
    }
    // Các hàm cốt lõi cung cấp Get/Set trạng thái ô được hệ thống gọi điều phối
    public boolean isMine() {
        return mine; // Phục vụ UC09.7 / UC11.7 / UC12.7 / UC13.4 / UC15.3 để thẩm định tính chất mìn ẩn của ô vuông
    }

    public void reveal() {
        // Phương thức gán đổi trạng thái dữ liệu ô phục vụ lật mở cho UC09.8 / UC11.8 / UC13.9 / UC15.3
        this.revealed = true;
    }

    public boolean isFlagged() {
        return flagged; // Phục vụ UC09.6 / UC10.5 / UC11.7 / UC13.5 để kiểm tra trạng thái cắm cờ đánh dấu của ô vuông
    }

    public int getNeighborMines() {
        return neighborMines; // Chứa kết quả do UC12.9 tính toán, phục vụ hiển thị nhãn số và thuật toán loang UC11.10/mở nhanh UC13.6
    }
}


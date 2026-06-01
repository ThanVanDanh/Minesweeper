package minesweeper.model;

public class Cell {
    // Trạng thái lõi của từng ô, phục vụ luồng UC03 - Chơi game (3.1, 03.2.1, 03.2.2, 03.2.3, 03.2.4)
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

    public boolean isMine() {
        //  03.2.3.2: Thẩm định tính chất mìn ẩn của ô vuông khi lật
        return mine;
    }

    public void reveal() {
        //  3.1 và 03.2.4.2: Gán đổi trạng thái dữ liệu ô thành đã lật mở (reveal)
        this.revealed = true;
    }

    public boolean isFlagged() {
        //  03.2.1.3: Kiểm tra trạng thái cắm cờ đánh dấu của ô vuông
        return flagged;
    }

    public int getNeighborMines() {
        // Chứa kết quả tính toán số mìn xung quanh, phục vụ 3.1 (kiểm tra == 0 để loang) và 03.2.2.3 (so sánh để mở nhanh)
        return neighborMines;
    }
}
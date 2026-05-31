package minesweeper.model;

import minesweeper.model.enums.Difficulty;
import minesweeper.model.enums.GameState;

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
    // UC09 & UC14 - Phương thức thực thi logic lật mở ô cốt lõi
    public void reveal(int row, int col) {
        // UC09.6: Kiểm tra các điều kiện an toàn biên tọa độ và trạng thái ván đấu đang diễn ra
        if (!isInBounds(row, col) || gameState != GameState.PLAYING) {
            return;
        }
        Cell cell = grid[row][col];
        // UC09.6: Chặn thao tác nếu thực thể ô đã được lật mở từ trước hoặc đang có cờ đánh dấu
        if (cell.isRevealed() || cell.isFlagged()) {
            return;
        }
        // UC09.7: Gọi phương thức dữ liệu cell.isMine() để thẩm định thuộc tính ẩn của ô hiện hành
        boolean isMine = cell.isMine();

        if (isMine) {
            // UC14 - PHÁT HIỆN MÌN
            // UC14.2: Thay đổi trạng thái bao quát của ván đấu sang thất bại GameState.LOST
            gameState = GameState.LOST;
            // UC14.3: Tự động kích hoạt gọi hàm thành phần nội bộ để xử lý lật tung vị trí mìn ẩn
            revealAllMines();
        } else {
            // UC09.8: Ô an toàn tuyệt đối, gọi phương thức nội bộ cell.reveal() đổi trạng thái dữ liệu ô
            cell.reveal();
            // UC09.9: Thực hiện giảm trừ số ô an toàn còn lại trên bàn cờ thông qua biến tích lũy
            safeCellsRemaining--;
            // UC09.10: Kiểm tra điều kiện hoàn tất ván đấu thắng cuộc
            checkWinCondition();
            int count = cell.getNeighborMines();

            // UC09.11: Nếu giá trị mìn bao quanh bằng 0, kích hoạt chạy thuật toán loang tự động mở vùng trống
            if (count == 0) {
                floodFill(row, col);
            }
        }
    }
    // UC11 - Thuật toán loang lan tỏa mở rộng ô trống tự động (BFS)
    private void floodFill(int startRow, int startCol) {
        // UC11.2: Tạo một kiến trúc hàng đợi Queue và nạp thực thể ô trống gốc ban đầu vào làm điểm tựa loang
        Queue<Cell> queue = new ArrayDeque<>();
        queue.add(grid[startRow][startCol]);

        // UC11.3: Duy trì chu kỳ vòng lặp kiểm tra liên tục trạng thái hàng đợi cho đến khi trống rỗng
        while (!queue.isEmpty()) {
            // UC11.4: Lấy ô vuông dữ liệu nằm ở đầu hàng đợi ra làm trung tâm xử lý thông qua lệnh queue.poll()
            Cell current = queue.poll();

            // UC11.5: Duyệt qua đồ thị ma trận 8 hướng lân cận bao quanh ô vuông trung tâm
            for (int i = 0; i < 8; i++) {
                int nr = current.getRow() + DR[i];
                int nc = current.getCol() + DC[i];

                // UC11.6: Kiểm tra biên tọa độ ô hàng xóm nằm hợp lệ trong phạm vi kích thước của bàn cờ
                if (isInBounds(nr, nc)) {
                    Cell neighbor = grid[nr][nc];
                    // UC11.7: Kiểm tra điều kiện an toàn: ô hàng xóm phải chưa lật mở, chưa cắm cờ và không có mìn ẩn
                    if (!neighbor.isRevealed() && !neighbor.isFlagged() && !neighbor.isMine()) {
                        // UC11.8: Ô lân cận an toàn, ra lệnh lật mở dữ liệu thông qua phương thức neighbor.reveal()
                        neighbor.reveal();
                        // UC11.9: Khấu trừ số ô an toàn còn lại trên hệ thống và cập nhật kiểm tra thắng cuộc
                        safeCellsRemaining--;
                        checkWinCondition();

                        int n = neighbor.getNeighborMines();
                        // UC11.10: Lấy số mìn lân cận, nếu ô hàng xóm cũng trống (n == 0) thì đẩy tiếp vào Queue để loang
                        if (n == 0) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
    }


    // UC13 - Phương thức logic mở nhanh ô hàng loạt (Chording)
    public void fastReveal(int row, int col) {
        // UC13.4: Thẩm định điều kiện chặn tiên quyết (biên tọa độ, game đang chơi, ô đã mở, có số lân cận và không chứa mìn)
        if (!isInBounds(row, col) || gameState != GameState.PLAYING) return;

        Cell cell = grid[row][col];
        if (!cell.isRevealed() || cell.getNeighborMines() == 0 || cell.isMine()) {
            return;
        }

        // UC13.5: Vòng lặp thứ nhất thực hiện quét qua ma trận 8 hướng xung quanh để thu thập tổng số lượng ô cắm cờ thực tế
        int flagCount = 0;
        for (int i = 0; i < 8; i++) {
            int nr = row + DR[i];
            int nc = col + DC[i];
            if (isInBounds(nr, nc) && grid[nr][nc].isFlagged()) {
                flagCount++;
            }
        }
        int cellValue = cell.getNeighborMines();

        // UC13.6: So sánh số cờ thực tế người chơi đã đánh dấu với giá trị chữ số cố định của ô vuông hiện tại
        if (flagCount == cellValue) {
            // UC13.7: Giá trị trùng khớp, chạy vòng lặp thứ hai quét lại hệ thống 8 hướng xung quanh ô số
            for (int i = 0; i < 8; i++) {
                int nr = row + DR[i];
                int nc = col + DC[i];
                if (isInBounds(nr, nc)) {
                    Cell neighbor = grid[nr][nc];
                    // UC13.8: Lọc tách ra những ô hàng xóm có trạng thái an toàn: chưa lật mở và chưa cắm cờ đánh dấu
                    if (!neighbor.isRevealed() && !neighbor.isFlagged()) {
                        // UC13.9: Gọi phương thức cốt lõi reveal(nr, nc), tái sử dụng luồng kiểm tra biên và loang mìn của UC09
                        reveal(nr, nc);
                    }
                }
            }
        }
    }
    // UC10 - Phương thức thay đổi logic cắm hoặc gỡ cờ đánh dấu ô dữ liệu
    public void toggleFlag(int row, int col) {
        // UC10.4: Kiểm tra các điều kiện an toàn biên (tọa độ hợp lệ, game đang chơi, ô phải chưa lật mở)
        if (!isInBounds(row, col) || gameState != GameState.PLAYING) return;

        Cell cell = grid[row][col];
        if (cell.isRevealed()) return;

        // UC10.5: Thẩm định trạng thái đánh dấu hiện tại của ô thông qua phương thức logic cell.isFlagged()
        if (cell.isFlagged()) {
            // UC10.6: Ô dữ liệu đã chứa cờ, thực hiện gỡ cờ bằng lệnh gán cell.setFlagged(false) và khấu trừ bộ đếm cờ
            cell.setFlagged(false);
            flagsPlaced = Math.max(0, flagsPlaced - 1);
        } else {
            // UC10.7: Ô chưa có cờ, kiểm tra số cờ đã đặt chưa vượt quá tổng số mìn của màn chơi hiện tại
            if (flagsPlaced < totalMines) {
                // Đạt điều kiện, tiến hành đặt cờ bằng lệnh cell.setFlagged(true) và cộng dồn tích lũy số cờ hệ thống
                cell.setFlagged(true);
                flagsPlaced++;
            }
        }
    }
    // UC15 - Phương thức tự động lật mở lộ diện toàn bộ mìn giấu kín khi người chơi thua cuộc
    private void revealAllMines() {
        // UC15.2: Triển khai vòng lặp ma trận kép càn quét qua toàn diện hệ thống ô vuông dữ liệu trên bàn cờ
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = grid[r][c];
                // UC15.3: Thẩm định thuộc tính ẩn, nếu ô vuông chứa mìn, kích hoạt hàm cell.reveal() chuyển đổi trạng thái dữ liệu sang lật mở công khai
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

    // UC12 - Thuật toán tự động chạy ngầm quét đếm tính toán số mìn xung quanh ma trận bàn cờ
    private void calculateNeighborMines() {
        // UC12.2: Chạy vòng lặp ma trận kép càn quét qua mọi vị trí tọa độ hàng r và cột c trên lưới bàn cờ dữ liệu
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // UC12.3: Thẩm định thuộc tính, nếu trúng mìn ẩn lập tức gán giá trị lân cận về 0 và nhảy bước bằng lệnh continue
                if (grid[r][c].isMine()) {
                    grid[r][c].setNeighborMines(0);
                    continue;
                }
                // UC12.4: Ô hiện tại là ô an toàn, khởi tạo biến bộ đếm tích lũy số lượng quả mìn lân cận nội bộ count = 0
                int count = 0;
                // UC12.5: Triển khai vòng lặp duyệt dịch chuyển vị trí biên ma trận 8 hướng lân cận từ -1 đến 1
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        // UC12.6: Vòng lặp tự động dùng lệnh continue bỏ qua chu kỳ nếu tọa độ dr, dc trùng khít với chính ô trung tâm
                        if (dr == 0 && dc == 0) {
                            continue;
                        }
                        int nr = r + dr;
                        int nc = c + dc;
                        // UC12.7: Tính toán tọa độ lân cận, kiểm tra biên hợp lệ và xác thực xem ô hàng xóm đó có chứa mìn ẩn hay không
                        if (isInBounds(nr, nc) && grid[nr][nc].isMine()) {
                            // UC12.8: Phát hiện ô hàng xóm chứa mìn ẩn, thực hiện tăng biến tích lũy count lên thêm 1 đơn vị
                            count++;
                        }
                    }
                }
                // UC12.9: Kết thúc chu trình đếm 8 hướng, lưu tổng số tích lũy đếm được vào thuộc tính neighborMines của ô hiện tại
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


package minesweeper.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import minesweeper.model.AuditLog;
import minesweeper.model.GameResult;
import minesweeper.model.enums.Difficulty;
import minesweeper.repository.MySqlGameResultRepository;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.repository.log.MySqlAuditLogRepository;
import minesweeper.repository.pagination.PagedResult;
import minesweeper.repository.spec.GameResultFilterSpec;
import minesweeper.service.FraudDetectionService;
import minesweeper.service.GameResultService;
import minesweeper.service.PlayerStatsService;
import minesweeper.service.PlayerStatsService.DifficultyStats;
import minesweeper.service.PlayerStatsService.PlayerStats;
import minesweeper.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminResultController {

    // ── FXML Controls ────────────────────────────────────────────────────────
    @FXML private ComboBox<String> cbDifficulty;
    @FXML private ComboBox<String> cbResult;
    @FXML private TextField        tfUsername;
    @FXML private Label            statusLabel;
    @FXML private Label            selectedCountLabel;

    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Label  pageLabel;

    @FXML private TableView<GameResult>            resultTable;
    @FXML private TableColumn<GameResult, Boolean> colSelect;
    @FXML private TableColumn<GameResult, String>  colGameId;
    @FXML private TableColumn<GameResult, String>  colUsername;
    @FXML private TableColumn<GameResult, Integer> colScore;
    @FXML private TableColumn<GameResult, String>  colDifficulty;
    @FXML private TableColumn<GameResult, String>  colTime;
    @FXML private TableColumn<GameResult, String>  colResult;
    @FXML private TableColumn<GameResult, String>  colPlayedAt;
    @FXML private TableColumn<GameResult, Integer> colOpenedCells;

    // ── Constants ────────────────────────────────────────────────────────────
    private static final int    PAGE_SIZE  = 20;
    private static final String FILTER_ALL = "Tất cả";

    // ── State ────────────────────────────────────────────────────────────────
    private int                  currentPage = 0;
    private int                  totalPages  = 1;
    private GameResultFilterSpec activeSpec  = new GameResultFilterSpec();

    private final ObservableList<GameResult> pageItems = FXCollections.observableArrayList();

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final GameResultService       gameResultService;
    private final MySqlAuditLogRepository auditLogRepository;
    private final FraudDetectionService   fraudDetectionService;
    private final PlayerStatsService      playerStatsService;
    private static final Logger LOG = LoggerFactory.getLogger(AdminResultController.class);

    /** Production constructor */
    public AdminResultController() {
        this.gameResultService     = new GameResultService(new MySqlGameResultRepository());
        this.auditLogRepository    = new MySqlAuditLogRepository();
        this.fraudDetectionService = new FraudDetectionService();
        this.playerStatsService    = new PlayerStatsService();
    }

    /** Test constructor (inject mock) */
    public AdminResultController(GameResultService gameResultService) {
        this.gameResultService     = gameResultService;
        this.auditLogRepository    = new MySqlAuditLogRepository();
        this.fraudDetectionService = new FraudDetectionService();
        this.playerStatsService    = new PlayerStatsService();
    }

    /** Test constructor with full injection */
    public AdminResultController(GameResultService gameResultService,
                                 FraudDetectionService fraudDetectionService) {
        this.gameResultService     = gameResultService;
        this.auditLogRepository    = new MySqlAuditLogRepository();
        this.fraudDetectionService = fraudDetectionService;
        this.playerStatsService    = new PlayerStatsService();
    }

    // =========================================================================
    // Basic Flow – UC-05.7 Xem danh sách kết quả
    // =========================================================================

    @FXML
    public void initialize() {
        // 05.7.1 Admin nhấn chọn mục "Quản lý kết quả" từ thanh điều hướng
        // 05.7.2 Hệ thống khởi tạo các bộ lọc mặc định:
        //          Độ khó và Kết quả đều ở trạng thái "Tất cả"
        setupTable();
        setupFilterComboBoxes();

        // 05.7.3 Hệ thống truy vấn cơ sở dữ liệu và lấy danh sách kết quả,
        //          hiển thị tối đa 20 bản ghi mỗi trang, bắt đầu từ trang đầu tiên
        loadPage();
    }

    private void setupFilterComboBoxes() {
        cbDifficulty.setItems(FXCollections.observableArrayList(
                FILTER_ALL,
                Difficulty.EASY.getLabel(),
                Difficulty.MEDIUM.getLabel(),
                Difficulty.HARD.getLabel(),
                Difficulty.EXPERT.getLabel()
        ));
        cbDifficulty.getSelectionModel().selectFirst();

        cbResult.setItems(FXCollections.observableArrayList(FILTER_ALL, "Thắng", "Thua"));
        cbResult.getSelectionModel().selectFirst();
    }

    private void setupTable() {
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        resultTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setupColumns();

        // 05.7.5 Lắng nghe sự kiện click tiêu đề cột để sắp xếp (server-side)
        resultTable.setOnSort(event -> {
            var sortOrder = resultTable.getSortOrder();
            if (sortOrder.isEmpty()) {
                activeSpec.sortBy = null;
                activeSpec.sortDir = null;
            } else {
                TableColumn<GameResult, ?> col = sortOrder.get(0);
                if (col == colScore) {
                    activeSpec.sortBy = "score";
                } else if (col == colTime) {
                    activeSpec.sortBy = "time";
                } else {
                    activeSpec.sortBy = null;
                }
                
                if (activeSpec.sortBy != null) {
                    activeSpec.sortDir = col.getSortType() == TableColumn.SortType.ASCENDING ? "ASC" : "DESC";
                } else {
                    activeSpec.sortDir = null;
                }
            }
            
            currentPage = 0;
            loadPage();
            event.consume(); // Chặn client-side sorting của TableView
        });
    }

    /**
     * 05.7.4 Hệ thống hiển thị danh sách lên bảng kèm thông tin số trang hiện tại,
     *          tổng số trang và tổng số kết quả tìm được.
     * 05-E1 CSDL không thể kết nối → hiển thị hộp thoại lỗi, bảng để trống
     */
    private void loadPage() {
        try {
            PagedResult<GameResult> result =
                    gameResultService.findPaged(activeSpec, currentPage, PAGE_SIZE);

            totalPages  = Math.max(1, result.getTotalPages());
            currentPage = Math.min(currentPage, totalPages - 1);

            pageItems.setAll(result.getContent());
            resultTable.setItems(pageItems);

            pageLabel.setText("Trang " + (currentPage + 1) + " / " + totalPages);
            btnPrevPage.setDisable(currentPage == 0);
            btnNextPage.setDisable(currentPage >= totalPages - 1);
            statusLabel.setText("Tìm thấy " + result.getTotalElements() + " kết quả");

        } catch (DataAccessException e) {
            // 05-E1 CSDL không thể kết nối → hiển thị hộp thoại lỗi, bảng để trống
            showError("Không thể tải dữ liệu");
        }
    }

    // =========================================================================
    // Alternative Flow – UC-05.8 Lọc kết quả
    // =========================================================================

    @FXML
    public void onFilter() {
        // 05.8.1 Admin nhập tên người chơi và/hoặc chọn bộ lọc Độ khó, Kết quả
        //          rồi nhấn nút Lọc
        String usernameFilter   = tfUsername.getText().trim();
        String difficultyFilter = cbDifficulty.getValue();
        String resultFilter     = cbResult.getValue();

        boolean noFilter = usernameFilter.isEmpty()
                && FILTER_ALL.equals(difficultyFilter)
                && FILTER_ALL.equals(resultFilter);

        if (noFilter) {
            // 05.8-A1 Admin xóa hết điều kiện lọc rồi nhấn Lọc
            //           → Hệ thống nhận diện không có bộ lọc nào,
            //             tải lại toàn bộ danh sách ban đầu từ cơ sở dữ liệu
            activeSpec = new GameResultFilterSpec();
        } else {
            // 05.8.2 Hệ thống xác định điều kiện lọc dựa trên thông tin Admin vừa nhập
            activeSpec = buildFilterSpec(usernameFilter, difficultyFilter, resultFilter);
        }

        // 05.8.3 Hệ thống truy vấn cơ sở dữ liệu theo điều kiện lọc,
        //          bắt đầu hiển thị lại từ trang đầu tiên
        currentPage = 0;

        // 05.8.4 Hệ thống tải lại bảng với danh sách kết quả phù hợp
        loadPage();
    }

    @FXML
    public void onReset() {
        // 05.8-A2 Admin nhấn Làm mới
        //           → Hệ thống xoá ô Username, reset ComboBox về 'Tất cả',
        //             tải lại toàn bộ danh sách từ CSDL
        tfUsername.clear();
        cbDifficulty.getSelectionModel().selectFirst();
        cbResult.getSelectionModel().selectFirst();
        activeSpec  = new GameResultFilterSpec();
        currentPage = 0;
        loadPage();
        statusLabel.setText("Đã reset");
    }

    /** Dựng GameResultFilterSpec từ các giá trị bộ lọc trên UI. */
    private GameResultFilterSpec buildFilterSpec(String username,
                                                 String difficultyFilter,
                                                 String resultFilter) {
        GameResultFilterSpec spec = new GameResultFilterSpec();

        if (!username.isEmpty()) spec.username = username;

        if (!FILTER_ALL.equals(difficultyFilter)) {
            for (Difficulty d : Difficulty.values()) {
                if (d.getLabel().equalsIgnoreCase(difficultyFilter)) {
                    spec.difficulty = d;
                    break;
                }
            }
        }

        if ("Thắng".equals(resultFilter))     spec.win = true;
        else if ("Thua".equals(resultFilter)) spec.win = false;

        return spec;
    }

    // =========================================================================
    // Alternative Flow – UC-05.9 Xóa kết quả gian lận
    // =========================================================================

    @FXML
    public void onDeleteFraud() {
        // 05.9.1 Admin tích checkbox trên từng dòng hoặc nhấn Chọn tất cả
        //          để chọn các kết quả đang hiển thị trên bảng
        // 05.9.2 Admin nhấn nút Xoá kết quả gian lận

        // 05.9.3 Hệ thống lấy danh sách kết quả mà Admin đã chọn
        List<GameResult> selectedList =
                new ArrayList<>(resultTable.getSelectionModel().getSelectedItems());

        // 05.9-E1 Chưa chọn dòng nào, nhấn Xoá
        //           → Hiển thị thông báo 'Hãy chọn dữ liệu để xoá', không thực hiện xoá
        try {
            validateDeleteSelection(selectedList);
        } catch (IllegalArgumentException e) {
            showInfo(e.getMessage());
            return;
        }

        // 05.9.4 [CẢI TIẾN v1.2 – B3] Hệ thống hiển thị dialog xác nhận:
        //          "Bạn có chắc muốn xóa N kết quả đã chọn? Hành động này không thể hoàn tác."
        if (!confirmFraudDelete(selectedList.size())) {
            // 05.9-A1 Admin nhấn Huỷ tại dialog xác nhận → Đóng dialog, không thực hiện xóa
            return;
        }

        try {
            // 05.9.5 Hệ thống thực hiện xóa toàn bộ các kết quả đã chọn khỏi cơ sở dữ liệu
            List<String> ids = selectedList.stream()
                    .map(GameResult::getGameId)
                    .collect(Collectors.toList());
            gameResultService.deleteByGameIds(ids);

            // 05.9.5 Hệ thống tải lại bảng dữ liệu và hiển thị thông báo xóa thành công
            //          kèm số lượng bản ghi đã xóa
            loadPage();
            statusLabel.setText("Đã xoá " + selectedList.size() + " kết quả");
            showInfo("Đã xoá thành công " + selectedList.size() + " kết quả gian lận.");

            // 05.9.6 Hệ thống ghi nhận hành động xóa vào nhật ký, bao gồm thông tin
            //          Admin thực hiện, danh sách mã kết quả bị xóa và tổng số lượng bản ghi
            writeAuditLog(selectedList);

        } catch (Exception e) {
            // 05-E1 CSDL lỗi
            showError("Xoá thất bại");
        }
    }

    @FXML
    public void onSelectAll() {
        if (resultTable.getSelectionModel().getSelectedItems().size()
                == resultTable.getItems().size()) {
            resultTable.getSelectionModel().clearSelection();
        } else {
            resultTable.getSelectionModel().selectAll();
        }
        resultTable.refresh();
        selectedCountLabel.setText(
                resultTable.getSelectionModel().getSelectedItems().size() + " đã chọn");
    }

    // =========================================================================
    // Alternative Flow – UC-05.10 Phát hiện gian lận tự động [MỚI v1.2 – B5]
    // =========================================================================

    @FXML
    public void onDetectFraud() {
        // 05.10.1 Admin nhấn nút "Phát hiện gian lận" trên màn hình Quản lý kết quả

        // 05.10.2 Hệ thống đọc ngưỡng thời gian hợp lý từ cấu hình FraudDetectionService
        //          (EASY < 5s | MEDIUM < 20s | HARD < 60s | EXPERT < 120s)

        // 05.10.3 Hệ thống quét tất cả kết quả WIN đang hiển thị trong bảng,
        //          so sánh completion_time với ngưỡng tương ứng của từng level
        List<GameResult> currentItems = new ArrayList<>(pageItems);
        List<GameResult> suspicious   = fraudDetectionService.detectSuspicious(currentItems);

        // 05.10-A1 Không tìm thấy kết quả ngị vấn
        //           → Hệ thống hiển thị thông báo "Không phát hiện kết quả bất thường nào."
        if (suspicious.isEmpty()) {
            statusLabel.setText("Không phát hiện kết quả ngị vấn");
            showInfo("Không phát hiện kết quả bất thường nào.");
            return;
        }

        // 05.10.4 Các dòng ngị vấn được tự động highlight màu đỏ nhạt
        //          và tích chọn checkbox
        highlightSuspiciousRows(suspicious);

        // 05.10.5 Nhãn trạng thái hiển thị: "Phát hiện N kết quả ngị vấn"
        int count = suspicious.size();
        statusLabel.setText("Phát hiện " + count + " kết quả ngị vấn");
        selectedCountLabel.setText(count + " đã chọn");
    }

    /**
     * 05.10.4 Tự động chọn (ticked checkbox) các dòng nghi vấn trong bảng.
     * Admin vẫn có thể bỏ chọn thủ công trước khi xóa (tiếp tục UC05.9).
     */
    private void highlightSuspiciousRows(List<GameResult> suspicious) {
        resultTable.getSelectionModel().clearSelection();
        for (GameResult r : suspicious) {
            int idx = pageItems.indexOf(r);
            if (idx >= 0) {
                resultTable.getSelectionModel().select(idx);
            }
        }
        resultTable.refresh();
    }

    // =========================================================================
    // Alternative Flow – UC-05.11 Thống kê theo người chơi [MỚI v1.2 – E1]
    // =========================================================================

    @FXML
    public void onViewPlayerStats() {
        // 05.11.1 Admin chọn một dòng kết quả trong bảng và nhấn nút
        //          "Thống kê người chơi"
        GameResult selected = resultTable.getSelectionModel().getSelectedItem();

        // 05.11-E1 Chưa chọn dòng nào → hiển thị thông báo
        if (selected == null) {
            showInfo("Hãy chọn một dòng kết quả để xem thống kê người chơi.");
            return;
        }

        // 05.11.2 Hệ thống lấy username từ dòng được chọn
        String username = selected.getPlayerName();

        try {
            // 05.11.3 Hệ thống truy vấn toàn bộ game_sessions của user đó
            //          và tính toán các chỉ số thống kê tổng hợp
            GameResultFilterSpec spec = GameResultFilterSpec.withUsername(username);
            // Lấy tất cả kết quả (tối đa 10000 để tính thống kê đầy đủ)
            PagedResult<GameResult> allResults =
                    gameResultService.findPaged(spec, 0, 10_000);

            PlayerStats stats = playerStatsService.computeStats(allResults.getContent());

            // 05.11-A1 User không có kết quả nào
            if (stats.totalGames() == 0) {
                showInfo("Người chơi '" + username + "' chưa có kết quả nào.");
                return;
            }

            // 05.11.4 Hệ thống hiển thị popup thống kê chi tiết
            showPlayerStatsDialog(username, stats);

        } catch (DataAccessException e) {
            // 05-E1 CSDL lỗi
            LOG.error("Failed to load player stats for: {}", username, e);
            showError("Không thể tải thống kê cho người chơi: " + username);
        }
    }

    /**
     * 05.11.4 Hiển thị popup thống kê chi tiết cho người chơi.
     *          Gồm: tổng ván, thắng/thua, tỉ lệ thắng, điểm TB,
     *          và bảng best score / best time theo từng độ khó.
     */
    private void showPlayerStatsDialog(String username, PlayerStats stats) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Thống kê người chơi: " + username);
        dialog.setHeaderText("Dashboard — " + username);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: linear-gradient(to bottom right, "
                + "rgba(5,20,28,0.95), rgba(5,10,18,0.95));");

        // ── Tổng quan ──
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(8);
        grid.setStyle("-fx-padding: 10;");
        String labelStyle = "-fx-text-fill: rgba(45,212,240,0.8); -fx-font-weight: bold;";
        String valueStyle = "-fx-text-fill: white; -fx-font-size: 14;";

        int row = 0;
        grid.add(styledLabel("Tổng ván chơi:", labelStyle), 0, row);
        grid.add(styledLabel(String.valueOf(stats.totalGames()), valueStyle), 1, row++);

        grid.add(styledLabel("Thắng / Thua:", labelStyle), 0, row);
        grid.add(styledLabel(stats.totalWins() + " / " + stats.totalLoses(), valueStyle), 1, row++);

        grid.add(styledLabel("Tỉ lệ thắng:", labelStyle), 0, row);
        grid.add(styledLabel(String.format("%.1f%%", stats.winRate()), valueStyle), 1, row++);

        grid.add(styledLabel("Điểm trung bình (WIN):", labelStyle), 0, row);
        grid.add(styledLabel(String.format("%.0f", stats.avgScore()), valueStyle), 1, row);

        content.getChildren().add(grid);

        // ── Bảng theo độ khó ──
        Label sectionLabel = new Label("Thành tích theo độ khó");
        sectionLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold; -fx-font-size: 13; -fx-padding: 8 0 0 0;");
        content.getChildren().add(sectionLabel);

        TableView<Map.Entry<Difficulty, DifficultyStats>> diffTable = new TableView<>();
        diffTable.setStyle("-fx-control-inner-background: rgba(10,20,30,0.8); -fx-text-fill: white;");
        diffTable.setPrefHeight(200);
        diffTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Map.Entry<Difficulty, DifficultyStats>, String> colDiff = new TableColumn<>("Độ khó");
        colDiff.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getKey().getLabel()));

        TableColumn<Map.Entry<Difficulty, DifficultyStats>, String> colTotal = new TableColumn<>("Tổng ván");
        colTotal.setCellValueFactory(d -> new SimpleStringProperty(
                String.valueOf(d.getValue().getValue().totalGames())));

        TableColumn<Map.Entry<Difficulty, DifficultyStats>, String> colWR = new TableColumn<>("Tỉ lệ thắng");
        colWR.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.1f%%", d.getValue().getValue().winRate())));

        TableColumn<Map.Entry<Difficulty, DifficultyStats>, String> colBest = new TableColumn<>("Best Score");
        colBest.setCellValueFactory(d -> new SimpleStringProperty(
                String.valueOf(d.getValue().getValue().bestScore())));

        TableColumn<Map.Entry<Difficulty, DifficultyStats>, String> colTime = new TableColumn<>("Best Time");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getValue().bestTimeFormatted()));

        diffTable.getColumns().addAll(colDiff, colTotal, colWR, colBest, colTime);
        diffTable.setItems(FXCollections.observableArrayList(stats.perDifficulty().entrySet()));

        content.getChildren().add(diffTable);

        dialog.getDialogPane().setContent(content);
        dialog.setWidth(650);
        dialog.setHeight(500);
        dialog.showAndWait();
    }

    /** Tạo Label có style inline (dùng cho dialog thống kê). */
    private Label styledLabel(String text, String style) {
        Label l = new Label(text);
        l.setStyle(style);
        return l;
    }

    // =========================================================================
    // Phân trang
    // =========================================================================

    @FXML
    public void onPrevPage() {
        if (currentPage > 0) {
            currentPage--;
            loadPage();
        }
    }

    @FXML
    public void onNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadPage();
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Xác thực danh sách kết quả được chọn trước khi xóa.
     * 05.9-E1 Chưa chọn dòng nào → ném IllegalArgumentException.
     */
    void validateDeleteSelection(List<GameResult> selected) {
        if (selected == null || selected.isEmpty()) {
            throw new IllegalArgumentException("Hãy chọn dữ liệu để xoá");
        }
    }

    /**
     * 05.9.4 [CẢI TIẾN v1.2 – B3] Hiển thị dialog xác nhận trước khi xóa hàng loạt.
     * 05.9-A1 Admin nhấn Huỷ → trả về false, không thực hiện xoá.
     */
    private boolean confirmFraudDelete(int count) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Xoá " + count + " kết quả gian lận?");
        alert.setContentText("Bạn có chắc muốn xóa " + count
                + " kết quả đã chọn? Hành động này không thể hoàn tác.");
        Optional<ButtonType> btn = alert.showAndWait();
        return btn.isPresent() && btn.get() == ButtonType.OK;
    }

    /**
     * 05.9.7 Ghi nhận hành động xóa vào nhật ký, bao gồm thông tin Admin thực hiện,
     *          danh sách mã kết quả bị xóa và tổng số lượng bản ghi.
     * 05-E1 Ghi log vào CSDL thất bại
     */
    private void writeAuditLog(List<GameResult> deleted) {
        try {
            Long adminId = SessionManager.isLoggedIn()
                    ? SessionManager.getCurrentUser().getId()
                    : null;

            String target = deleted.stream()
                    .map(GameResult::getGameId)
                    .collect(Collectors.joining(","));

            String details = "Deleted " + deleted.size()
                    + " fraudulent game results; GameIds: " + target;

            auditLogRepository.insert(new AuditLog(adminId, "DELETE_SESSION", target, details));
            LOG.info("[AUDIT] Admin {} deleted {} fraudulent results: {}",
                    adminId, deleted.size(), target);

        } catch (Exception e) {
            // 05-E1 Ghi log vào CSDL thất bại
            LOG.warn("Failed to write audit log for fraud deletion", e);
        }
    }

    private void setupColumns() {
        colGameId.setCellValueFactory(new PropertyValueFactory<>("gameId"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colDifficulty.setCellValueFactory(new PropertyValueFactory<>("difficultyLabel"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timeFormatted"));
        colResult.setCellValueFactory(new PropertyValueFactory<>("result"));
        colPlayedAt.setCellValueFactory(new PropertyValueFactory<>("playedAtFormatted"));
        colOpenedCells.setCellValueFactory(new PropertyValueFactory<>("openedCells"));

        // Chỉ cho phép sort cột Score và Thời gian
        colGameId.setSortable(false);
        colUsername.setSortable(false);
        colDifficulty.setSortable(false);
        colResult.setSortable(false);
        colPlayedAt.setSortable(false);
        colOpenedCells.setSortable(false);
        colSelect.setSortable(false);

        colSelect.setCellValueFactory(param -> new SimpleBooleanProperty(false));
        colSelect.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(e -> {
                    if (checkBox.isSelected()) {
                        resultTable.getSelectionModel().select(getIndex());
                    } else {
                        resultTable.getSelectionModel().clearSelection(getIndex());
                    }
                    selectedCountLabel.setText(
                            resultTable.getSelectionModel().getSelectedItems().size() + " đã chọn");
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                checkBox.setSelected(
                        resultTable.getSelectionModel().getSelectedIndices().contains(getIndex()));
                setGraphic(checkBox);
            }
        });
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void closePopup() {
        ((Stage) resultTable.getScene().getWindow()).close();
    }
}
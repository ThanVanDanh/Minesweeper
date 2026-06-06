package minesweeper.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
import minesweeper.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
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
    private static final Logger LOG = LoggerFactory.getLogger(AdminResultController.class);

    /** Production constructor */
    public AdminResultController() {
        this.gameResultService    = new GameResultService(new MySqlGameResultRepository());
        this.auditLogRepository   = new MySqlAuditLogRepository();
        this.fraudDetectionService = new FraudDetectionService();
    }

    /** Test constructor (inject mock) */
    public AdminResultController(GameResultService gameResultService) {
        this.gameResultService    = gameResultService;
        this.auditLogRepository   = new MySqlAuditLogRepository();
        this.fraudDetectionService = new FraudDetectionService();
    }

    /** Test constructor with full injection */
    public AdminResultController(GameResultService gameResultService,
                                 FraudDetectionService fraudDetectionService) {
        this.gameResultService    = gameResultService;
        this.auditLogRepository   = new MySqlAuditLogRepository();
        this.fraudDetectionService = fraudDetectionService;
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
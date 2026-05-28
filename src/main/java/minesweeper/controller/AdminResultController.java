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
import minesweeper.service.GameResultService;
import minesweeper.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
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
    private static final Logger LOG = LoggerFactory.getLogger(AdminResultController.class);

    /** Production constructor */
    public AdminResultController() {
        this.gameResultService  = new GameResultService(new MySqlGameResultRepository());
        this.auditLogRepository = new MySqlAuditLogRepository();
    }

    /** Test constructor (inject mock) */
    public AdminResultController(GameResultService gameResultService) {
        this.gameResultService  = gameResultService;
        this.auditLogRepository = new MySqlAuditLogRepository();
    }

    // =========================================================================
    // Basic Flow – UC-19.1 Xem danh sách kết quả
    // =========================================================================

    /**
     * 19.1.1 Admin mở màn hình Quản lý kết quả trò chơi
     */
    @FXML
    public void initialize() {
        setupTable();
        setupFilterComboBoxes();
        loadPage(); // 19.1.2 Hệ thống truy vấn CSDL
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
    }

    /**
     * 19.1.2 Hệ thống truy vấn CSDL và hiển thị theo activeSpec + currentPage
     * 19.1-E1 CSDL không thể kết nối → hiển thị hộp thoại lỗi, bảng để trống
     */
    private void loadPage() {
        try {
            PagedResult<GameResult> result =
                    gameResultService.findPaged(activeSpec, currentPage, PAGE_SIZE);

            totalPages  = Math.max(1, result.getTotalPages());
            currentPage = Math.min(currentPage, totalPages - 1);

            // 19.1.3 Hệ thống hiển thị danh sách lên bảng
            pageItems.setAll(result.getContent());
            resultTable.setItems(pageItems);

            pageLabel.setText("Trang " + (currentPage + 1) + " / " + totalPages);
            btnPrevPage.setDisable(currentPage == 0);
            btnNextPage.setDisable(currentPage >= totalPages - 1);
            statusLabel.setText("Tìm thấy " + result.getTotalElements() + " kết quả");

        } catch (DataAccessException e) {
            // 19.1-E1 CSDL không thể kết nối → hiển thị hộp thoại lỗi, bảng để trống
            showError("Không thể tải dữ liệu");
        }
    }

    // =========================================================================
    // Alternative Flow – UC-19.2 Lọc kết quả
    // =========================================================================

    /**
     * 19.2.1 Admin nhập username và/hoặc chọn bộ lọc (Độ khó / Kết quả) rồi nhấn Lọc
     * 19.2-A1 Admin xoá hết điều kiện rồi nhấn Lọc → khôi phục danh sách ban đầu
     * 19.2-E1 CSDL lỗi khi truy vấn → hiển thị hộp thoại lỗi
     */
    @FXML
    public void onFilter() {
        String usernameFilter   = tfUsername.getText().trim();
        String difficultyFilter = cbDifficulty.getValue();
        String resultFilter     = cbResult.getValue();

        boolean noFilter = usernameFilter.isEmpty()
                && FILTER_ALL.equals(difficultyFilter)
                && FILTER_ALL.equals(resultFilter);

        // 19.2-A1 Admin xoá hết điều kiện → spec trống, tải lại toàn bộ
        activeSpec  = noFilter ? new GameResultFilterSpec()
                : buildFilterSpec(usernameFilter, difficultyFilter, resultFilter);
        currentPage = 0;

        // 19.2.2 + 19.2.3 Hệ thống truy vấn và lọc server-side
        // 19.2.4 Hệ thống tải lại danh sách
        loadPage();
    }

    /**
     * 19.2-A2 Admin nhấn Làm mới
     * Xoá ô Username, reset ComboBox về 'Tất cả', tải lại toàn bộ danh sách
     */
    @FXML
    public void onReset() {
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
    // Alternative Flow – UC-19.3 Xóa kết quả gian lận
    // =========================================================================

    /**
     * 19.3.1 Admin tích checkbox trên từng dòng hoặc nhấn 'Chọn tất cả'
     * 19.3.2 Admin nhấn nút Xoá kết quả gian lận
     * 19.3-E1 Chưa chọn dòng nào, nhấn Xoá
     * 19.3-E2 CSDL lỗi khi xoá
     * 19.3-E3 Ghi log vào CSDL thất bại
     */
    @FXML
    public void onDeleteFraud() {
        // 19.3.3 Lấy danh sách mà Admin chọn
        List<GameResult> selectedList =
                new ArrayList<>(resultTable.getSelectionModel().getSelectedItems());

        // 19.3-E1 Chưa chọn dòng nào → hiển thị thông báo
        if (selectedList.isEmpty()) {
            showInfo("Hãy chọn dữ liệu để xoá");
            return;
        }

        try {
            // 19.3.4 Hệ thống xóa kết quả khỏi CSDL
            List<String> ids = selectedList.stream()
                    .map(GameResult::getGameId)
                    .collect(Collectors.toList());
            gameResultService.deleteByGameIds(ids);

            // 19.3.5 Hệ thống tải lại bảng và thông báo thành công
            loadPage();
            statusLabel.setText("Đã xoá " + selectedList.size() + " kết quả");
            showInfo("Đã xoá thành công " + selectedList.size() + " kết quả gian lận.");

            // 19.3.6 Hệ thống ghi nhận Log vào CSDL
            writeAuditLog(selectedList);

        } catch (Exception e) {
            // 19.3-E2 CSDL lỗi khi xoá → hiển thị hộp thoại lỗi
            showError("Xoá thất bại");
        }
    }

    /**
     * 19.3-A1 Admin nhấn 'Chọn tất cả'
     * Nếu tất cả dòng đang được chọn → bỏ chọn tất cả; ngược lại → chọn tất cả.
     */
    @FXML
    public void onSelectAll() {
        if (resultTable.getSelectionModel().getSelectedItems().size()
                == resultTable.getItems().size()) {
            // 19.3-A1 Tất cả đã chọn → bỏ chọn tất cả
            resultTable.getSelectionModel().clearSelection();
        } else {
            resultTable.getSelectionModel().selectAll();
        }
        resultTable.refresh();
        selectedCountLabel.setText(
                resultTable.getSelectionModel().getSelectedItems().size() + " đã chọn");
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
     * Ghi một bản ghi audit log. Lỗi chỉ được log warn, không ném lên UI.
     * 19.3-E3 Ghi log vào CSDL thất bại
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
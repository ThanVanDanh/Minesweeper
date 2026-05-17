package minesweeper.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import minesweeper.model.enums.Difficulty;
import minesweeper.model.GameResult;
import minesweeper.repository.GameResultRepository;
import minesweeper.repository.MySqlGameResultRepository;

import java.util.ArrayList;
import java.util.List;

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

    @FXML private TableView<GameResult>              resultTable;
    @FXML private TableColumn<GameResult, Boolean>   colSelect;
    @FXML private TableColumn<GameResult, String>    colGameId;
    @FXML private TableColumn<GameResult, String>    colUsername;
    @FXML private TableColumn<GameResult, Integer>   colScore;
    @FXML private TableColumn<GameResult, String>    colDifficulty;
    @FXML private TableColumn<GameResult, String>    colTime;
    @FXML private TableColumn<GameResult, String>    colResult;
    @FXML private TableColumn<GameResult, String>    colPlayedAt;
    @FXML private TableColumn<GameResult, Integer>   colOpenedCells;

    // ── Constants ────────────────────────────────────────────────────────────
    private static final int    PAGE_SIZE  = 20;
    private static final String FILTER_ALL = "Tất cả";

    // ── State ────────────────────────────────────────────────────────────────
    private int     currentPage  = 0;
    private int     totalPages   = 1;
    private boolean isFiltering  = false;

    private final ObservableList<GameResult> masterList   = FXCollections.observableArrayList();
    private final ObservableList<GameResult> filteredList = FXCollections.observableArrayList();

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final GameResultRepository       repository;
    private final MySqlGameResultRepository  pagedRepository;

    public AdminResultController() {
        repository       = new MySqlGameResultRepository();
        pagedRepository  = (MySqlGameResultRepository) repository;
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
        loadResults();         // 19.1.2 Hệ thống truy vấn CSLD
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
     * 19.1.2 Hệ thống truy vấn CSLD
     * 19.1.3 Hệ Thống hiển thị danh sách lên bảng
     * 19.1-E1 CSDL không thể kết nối
     */
    private void loadResults() {
        try {
            // 19.1.2 Hệ thống truy vấn CSLD
            var paged = pagedRepository.getAllResults(currentPage, PAGE_SIZE);
            //19.1.3 Hệ Thống hiển thị danh sách lên bảng
            masterList.setAll(paged.getContent());
            resultTable.setItems(masterList);
            totalPages = Math.max(1, paged.getTotalPages());
            updatePageControls();
            statusLabel.setText("Trang " + (currentPage + 1) + ": " + masterList.size() + " kết quả");
        } catch (Exception e) {
            // 19.1-E1 CSDL không thể kết nối
            showError("Không thể tải dữ liệu");
        }
    }

    // =========================================================================
    // Alternative Flow – UC-19.2 Lọc kết quả
    // =========================================================================

    /**
     * 19.2.1 Admin nhập username và/hoặc chọn bộ lọc (Độ khó / Kết quả) rồi nhấn Lọc
     * 19.2.2 Hệ thống truy vấn danh sách từ CSLD
     * 19.2.3 Hệ thống lọc danh sách theo yêu cầu
     * 19.2.4 Hệ thống tải lại danh sách
     * 19.2-E1 CSDL lỗi khi tải toàn bộ dữ liệu để lọc
     */
    @FXML
    public void onFilter() {
        // 19.2.1 Admin nhập username và/hoặc chọn bộ lọc (Độ khó / Kết quả) rồi nhấn Lọc
        String usernameFilter   = tfUsername.getText().toLowerCase().trim();
        String difficultyFilter = cbDifficulty.getValue();
        String resultFilter     = cbResult.getValue();

        try {
            // 19.2.2 Hệ thống truy vấn danh sách từ CSLD
            ObservableList<GameResult> allResults =
                    FXCollections.observableArrayList(repository.getAllResults());

            // 19.2.3 Hệ thống lọc danh sách theo yêu cầu
            filteredList.clear();
            for (GameResult game : allResults) {
                if (matchesFilter(game, usernameFilter, difficultyFilter, resultFilter)) {
                    filteredList.add(game);
                }
            }
            isFiltering = true;
            totalPages  = Math.max(1, (int) Math.ceil((double) filteredList.size() / PAGE_SIZE));
            currentPage = 0;

            // 19.2.4 Hệ thống tải lại danh sách
            showFilteredPage();

            // 23.2.6
            statusLabel.setText("Tìm thấy " + filteredList.size() + " kết quả");
        } catch (Exception e) {
            // 19.2-E1 CSDL lỗi khi tải toàn bộ dữ liệu để lọc
            e.printStackTrace();
            showError("Lọc thất bại");
        }
    }

    /**
     * 19.2-A2 Admin nhấn Làm mới
     * Xoá ô Username, reset ComboBox về 'Tất cả', tải lại theo server-side paging
     */
    @FXML
    public void onReset() {
        tfUsername.clear();
        cbDifficulty.getSelectionModel().selectFirst();
        cbResult.getSelectionModel().selectFirst();

        isFiltering = false;
        filteredList.clear();
        currentPage = 0;
        loadResults();
        statusLabel.setText("Đã reset");
    }

    /**
     * Kiểm tra một GameResult có khớp điều kiện lọc không.
     */
    private boolean matchesFilter(GameResult game, String usernameFilter,
                                  String difficultyFilter, String resultFilter) {
        boolean matchUsername   = game.getPlayerName().toLowerCase().contains(usernameFilter);
        boolean matchDifficulty = FILTER_ALL.equals(difficultyFilter)
                || (game.getDifficulty() != null
                && game.getDifficulty().getLabel().equalsIgnoreCase(difficultyFilter));
        boolean matchResult     = FILTER_ALL.equals(resultFilter)
                || game.getResult().equalsIgnoreCase(resultFilter);

        return matchUsername && matchDifficulty && matchResult;
    }

    /**
     * Hiển thị một trang của filteredList lên bảng.
     */
    private void showFilteredPage() {
        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filteredList.size());
        masterList.setAll(filteredList.subList(from, to));
        resultTable.setItems(masterList);
        updatePageControls();
        statusLabel.setText("Trang " + (currentPage + 1) + ": " + masterList.size() + " kết quả");
    }

    // =========================================================================
    // Alternative Flow – UC-19.3 Xóa kết quả gian lận
    // =========================================================================

    /**
     * 19.3.1 Admin tích checkbox trên từng dòng hoặc nhấn 'Chọn tất cả'
     * 19.3.2 Admin nhấn nút Xoá kết quả gian lận
     * 19.3.3 Láy danh sách mà Admin chọn
     * 19.3.4 Hệ thống xóa kết quả khỏi CSDL
     * 19.3.5 Hệ thống cập nhập lại bảng và thông báo thành công
     * 19.3-E1 Chưa chọn dòng nào, nhấn Xoá
     * 19.3-E2 CSDL lỗi khi xoá
     */
    @FXML
    public void onDeleteFraud() {
        // 19.3.3 Láy danh sách mà Admin chọn
        List<GameResult> selectedList =
                new ArrayList<>(resultTable.getSelectionModel().getSelectedItems());

        // 19.3-E1 Chưa chọn dòng nào, nhấn Xoá
        if (selectedList.isEmpty()) {
            showInfo("Hãy chọn dữ liệu để xoá");
            return;
        }

        try {
            // 19.3.4 Hệ thống xóa kết quả khỏi CSDL
            for (GameResult game : selectedList) {
                repository.deleteByGameIds(List.of(game.getGameId()));
                masterList.remove(game);
                if (isFiltering) filteredList.remove(game);
            }

            // 19.3.5 Hệ thống cập nhập lại bảng và thông báo thành công
            resultTable.setItems(masterList);
            statusLabel.setText("Đã xoá " + selectedList.size() + " kết quả");
            showInfo("Đã xoá thành công " + selectedList.size() + " kết quả gian lận.");
        } catch (Exception e) {
            // 19.3-E2 CSDL lỗi khi xoá
            showError("Xoá thất bại");
        }
    }

    /**
     * 19.3-A1 Admin nhấn 'Chọn tất cả' khi tất cả dòng đã được chọn
     * Nếu tất cả dòng đang được chọn → bỏ chọn tất cả; ngược lại → chọn tất cả.
     */
    @FXML
    public void onSelectAll() {
        if (resultTable.getSelectionModel().getSelectedItems().size()
                == resultTable.getItems().size()) {
            // 19.3-A1: Tất cả đã chọn → bỏ chọn tất cả
            resultTable.getSelectionModel().clearSelection();
        } else {
            resultTable.getSelectionModel().selectAll();
        }
        resultTable.refresh();
        selectedCountLabel.setText(
                resultTable.getSelectionModel().getSelectedItems().size() + " đã chọn");
    }

    // =========================================================================
    // Private – Helpers
    // =========================================================================

    /** Chuyển về trang trước; gọi loadResults() hoặc showFilteredPage() tuỳ trạng thái lọc. */
    @FXML
    public void onPrevPage() {
        if (currentPage > 0) {
            currentPage--;
            if (isFiltering) showFilteredPage();
            else             loadResults();
        }
    }

    /** Chuyển sang trang tiếp theo; gọi loadResults() hoặc showFilteredPage() tuỳ trạng thái lọc. */
    @FXML
    public void onNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            if (isFiltering) showFilteredPage();
            else             loadResults();
        }
    }

    /** Cập nhật nhãn trang và trạng thái nút Prev/Next. */
    private void updatePageControls() {
        pageLabel.setText("Trang " + (currentPage + 1) + " / " + totalPages);
        btnPrevPage.setDisable(currentPage == 0);
        btnNextPage.setDisable(currentPage >= totalPages - 1);
    }

    // ── Cột bảng ─────────────────────────────────────────────────────────────

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
                } else {
                    boolean selected = resultTable.getSelectionModel()
                            .getSelectedIndices().contains(getIndex());
                    checkBox.setSelected(selected);
                    setGraphic(checkBox);
                }
            }
        });
    }

    // ── Thông báo ─────────────────────────────────────────────────────────────

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

    // ── Đóng màn hình ─────────────────────────────────────────────────────────

    @FXML
    private void closePopup() {
        ((Stage) resultTable.getScene().getWindow()).close();
    }
}
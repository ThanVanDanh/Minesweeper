package minesweeper.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import minesweeper.model.GameResult;
import minesweeper.service.ResultService;

import java.util.ArrayList;
import java.util.List;

public class AdminResultController {

    @FXML private ComboBox<String> cbDifficulty;
    @FXML private TextField tfUsername;

    @FXML private TableView<GameResult>            resultTable;
    @FXML private TableColumn<GameResult, Boolean> colSelect;      // checkbox chọn để xóa
    @FXML private TableColumn<GameResult, Integer> colId;
    @FXML private TableColumn<GameResult, String>  colUsername;
    @FXML private TableColumn<GameResult, Integer> colScore;
    @FXML private TableColumn<GameResult, String>  colDifficulty;
    @FXML private TableColumn<GameResult, String>  colTime;
    @FXML private TableColumn<GameResult, String>  colPlayedAt;

    private final ResultService resultService = new ResultService();
    private final ObservableList<GameResult> resultList = FXCollections.observableArrayList();

    // ── Khởi tạo ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        cbDifficulty.setItems(FXCollections.observableArrayList(
                "Tất cả", "Dễ", "Trung Bình", "Khó", "Chuyên Gia"
        ));
        cbDifficulty.getSelectionModel().selectFirst();

        setupColumns();
        loadResults();
    }

    // ── Setup cột ────────────────────────────────────────────────────────────

    private void setupColumns() {
        // Cột checkbox — click để đánh dấu gian lận
        colSelect.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();

            {
                cb.setOnAction(e -> {
                    GameResult r = getTableView().getItems().get(getIndex());
                    r.setFlaggedAsFraud(cb.isSelected());
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                GameResult r = getTableView().getItems().get(getIndex());
                cb.setSelected(r.isFlaggedAsFraud());
                setGraphic(cb);
            }
        });

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));

        colDifficulty.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getDifficulty().getLabel()));

        colTime.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getTimeFormatted()));

        colPlayedAt.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getPlayedAtFormatted()));

        resultTable.setEditable(true);
    }

    // ── UC18: Load & Lọc ────────────────────────────────────────────────────

    private void loadResults() {
        resultList.setAll(resultService.filterResults(null, null, null, null));
        resultTable.setItems(resultList);
    }

    @FXML
    public void onFilter() {
        String username   = tfUsername.getText().trim().toLowerCase();
        String difficulty = cbDifficulty.getValue();

        ObservableList<GameResult> filtered = FXCollections.observableArrayList();

        for (GameResult r : resultService.filterResults(null, null, null, null)) {
            boolean matchUser = r.getUsername().toLowerCase().contains(username);
            boolean matchDiff = difficulty.equals("Tất cả")
                    || r.getDifficulty().getLabel().equals(difficulty);

            if (matchUser && matchDiff) {
                filtered.add(r);
            }
        }

        resultTable.setItems(filtered);

        if (filtered.isEmpty()) {
            showMessage("Không tìm thấy dữ liệu phù hợp.");
        } else {
            showMessage("Tìm thấy " + filtered.size() + " kết quả.");
        }
    }

    @FXML
    public void onReset() {
        tfUsername.clear();
        cbDifficulty.getSelectionModel().selectFirst();
        loadResults();
    }

    // ── UC19: Xóa gian lận ──────────────────────────────────────────────────

    @FXML
    public void onSelectAll() {
        boolean anyUnchecked = resultList.stream().anyMatch(r -> !r.isFlaggedAsFraud());
        resultList.forEach(r -> r.setFlaggedAsFraud(anyUnchecked));
        resultTable.refresh();
    }

    @FXML
    public void onDeleteFraud() {
        List<GameResult> deleteList = new ArrayList<>();

        for (GameResult r : resultTable.getItems()) {
            if (r.isFlaggedAsFraud()) {
                deleteList.add(r);
            }
        }

        if (deleteList.isEmpty()) {
            showMessage("Hãy tick checkbox vào kết quả cần xóa.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Sắp xóa " + deleteList.size() + " kết quả gian lận.");
        confirm.setContentText("Hành động này không thể hoàn tác. Bạn có chắc chắn?");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // UC16: ghi log trước khi xóa (bên trong service)
                resultService.deleteFraudResults(
                        deleteList.stream().map(GameResult::getId).toList()
                );
                resultTable.getItems().removeAll(deleteList);
                resultList.removeAll(deleteList);
                showMessage("Đã xóa " + deleteList.size() + " kết quả gian lận.");
            }
        });
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
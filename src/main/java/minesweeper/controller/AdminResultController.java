package minesweeper.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import minesweeper.model.Difficulty;
import minesweeper.model.GameResult;
import minesweeper.repository.GameResultRepository;
import minesweeper.repository.MySqlGameResultRepository;
import minesweeper.repository.exception.DataAccessException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminResultController {

    @FXML private ComboBox<String> cbDifficulty;
    @FXML private ComboBox<String> cbResult;
    @FXML private TextField        tfUsername;

    @FXML private Label statusLabel;
    @FXML private Label selectedCountLabel;

    @FXML private TableView<GameResult>            resultTable;
    @FXML private TableColumn<GameResult, Boolean> colSelect;
    @FXML private TableColumn<GameResult, String>  colGameId;
    @FXML private TableColumn<GameResult, String>  colUsername;
    @FXML private TableColumn<GameResult, Integer> colScore;
    @FXML private TableColumn<GameResult, String>  colDifficulty;
    @FXML private TableColumn<GameResult, String>  colTime;
    @FXML private TableColumn<GameResult, String>  colResult;
    @FXML private TableColumn<GameResult, String>  colPlayedAt;

    private final Set<String> fraudIds = new HashSet<>();
    private final ObservableList<GameResult> masterList = FXCollections.observableArrayList();
    private final GameResultRepository repository;

    public AdminResultController() {
        this(new MySqlGameResultRepository());
    }
    public AdminResultController(GameResultRepository repository) {
        this.repository = repository;
    }

    @FXML
    public void initialize() {
        cbDifficulty.setItems(FXCollections.observableArrayList(
                "Tất cả",
                Difficulty.EASY.getLabel(),
                Difficulty.MEDIUM.getLabel(),
                Difficulty.HARD.getLabel(),
                Difficulty.EXPERT.getLabel()
        ));
        cbDifficulty.getSelectionModel().selectFirst();

        cbResult.setItems(FXCollections.observableArrayList("Tất cả", "Thắng", "Thua"));
        cbResult.getSelectionModel().selectFirst();

        setupColumns();
        loadResults();
    }

    private void setupColumns() {
        // Checkbox chọn
        colSelect.setCellValueFactory(data ->
                new SimpleBooleanProperty(fraudIds.contains(data.getValue().getGameId())));
        colSelect.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                cb.setStyle("-fx-cursor:hand;");
                cb.setOnAction(e -> {
                    GameResult r = getTableView().getItems().get(getIndex());
                    if (cb.isSelected()) fraudIds.add(r.getGameId());
                    else                 fraudIds.remove(r.getGameId());
                    updateSelectedCount();
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                GameResult r = getTableView().getItems().get(getIndex());
                cb.setSelected(fraudIds.contains(r.getGameId()));
                setGraphic(cb);
            }
        });

        colGameId.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getGameId()));

        colUsername.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPlayerName()));
        colUsername.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-text-fill:#2dd4f0;-fx-font-weight:700;");
            }
        });

        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colScore.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(String.valueOf(item));
                setStyle("-fx-text-fill:#39ff8f;-fx-font-weight:700;-fx-alignment:CENTER_RIGHT;");
            }
        });

        colDifficulty.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDifficultyLabel()));

        colTime.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTimeFormatted()));

        colResult.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getResult()));
        colResult.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("Thắng".equals(item)
                        ? "-fx-text-fill:#39ff8f;-fx-font-weight:900;"
                        : "-fx-text-fill:#ef5350;-fx-font-weight:900;");
            }
        });

        colPlayedAt.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPlayedAtFormatted()));

        resultTable.setEditable(true);
    }

    private void loadResults() {
        try {
            List<GameResult> all = repository.getAllResults();
            masterList.setAll(all);
            fraudIds.clear();
            resultTable.setItems(masterList);
            setStatus("Tải " + all.size() + " kết quả.");
            updateSelectedCount();
        } catch (DataAccessException e) {
            showError("Không thể tải dữ liệu:\n" + e.getMessage());
        }
    }

    @FXML
    public void onFilter() {
        String username  = tfUsername.getText().trim().toLowerCase();
        String diffLabel = cbDifficulty.getValue();
        String result    = cbResult.getValue();

        List<GameResult> filtered = masterList.stream()
                .filter(r -> username.isBlank()
                        || r.getPlayerName().toLowerCase().contains(username))
                .filter(r -> "Tất cả".equals(diffLabel)
                        || diffLabel.equals(r.getDifficultyLabel()))
                .filter(r -> "Tất cả".equals(result)
                        || result.equals(r.getResult()))
                .collect(Collectors.toList());

        resultTable.setItems(FXCollections.observableArrayList(filtered));
        setStatus("Tìm thấy " + filtered.size() + " kết quả.");
        updateSelectedCount();
    }

    @FXML
    public void onReset() {
        tfUsername.clear();
        cbDifficulty.getSelectionModel().selectFirst();
        cbResult.getSelectionModel().selectFirst();
        fraudIds.clear();
        loadResults();
    }

    @FXML
    public void onSelectAll() {
        ObservableList<GameResult> current = resultTable.getItems();
        boolean anyUnchecked = current.stream()
                .anyMatch(r -> !fraudIds.contains(r.getGameId()));

        if (anyUnchecked) current.forEach(r -> fraudIds.add(r.getGameId()));
        else              current.forEach(r -> fraudIds.remove(r.getGameId()));

        resultTable.refresh();
        updateSelectedCount();
    }

    @FXML
    public void onDeleteFraud() {
        List<GameResult> toDelete = resultTable.getItems().stream()
                .filter(r -> fraudIds.contains(r.getGameId()))
                .collect(Collectors.toList());

        if (toDelete.isEmpty()) {
            showInfo("Hãy tick checkbox vào kết quả cần xoá.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xoá");
        confirm.setHeaderText("Sắp xoá " + toDelete.size() + " kết quả gian lận.");
        confirm.setContentText("Hành động này không thể hoàn tác. Bạn có chắc chắn?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            List<String> gameIds = toDelete.stream()
                    .map(GameResult::getGameId)
                    .collect(Collectors.toList());

            try {
                repository.deleteByGameIds(gameIds);
                masterList.removeAll(toDelete);
                resultTable.getItems().removeAll(toDelete);
                fraudIds.removeAll(gameIds);
                setStatus("Đã xoá " + toDelete.size() + " kết quả gian lận.");
                updateSelectedCount();
            } catch (DataAccessException e) {
                showError("Xoá thất bại:\n" + e.getMessage());
            }
        });
    }

    private void updateSelectedCount() {
        if (selectedCountLabel != null)
            selectedCountLabel.setText(fraudIds.size() + " đã chọn");
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION) {{ setContentText(message); }}.showAndWait();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR) {{ setContentText(message); }}.showAndWait();
    }
}
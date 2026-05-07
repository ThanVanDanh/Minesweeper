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
    @FXML private TextField        tfUsername;

    @FXML private TableView<GameResult>                 resultTable;
    @FXML private TableColumn<GameResult, Boolean>      colSelect;
    @FXML private TableColumn<GameResult, String>       colGameId;
    @FXML private TableColumn<GameResult, String>       colUsername;
    @FXML private TableColumn<GameResult, Integer>      colScore;
    @FXML private TableColumn<GameResult, String>       colDifficulty;
    @FXML private TableColumn<GameResult, String>       colTime;
    @FXML private TableColumn<GameResult, String>       colResult;
    @FXML private TableColumn<GameResult, String>       colPlayedAt;


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

        setupColumns();
        loadResults();
    }


    private void setupColumns() {

        colSelect.setCellValueFactory(data ->
                new SimpleBooleanProperty(fraudIds.contains(data.getValue().getGameId())));

        colSelect.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();

            {
                cb.setOnAction(e -> {
                    GameResult r = getTableView().getItems().get(getIndex());
                    if (cb.isSelected()) {
                        fraudIds.add(r.getGameId());
                    } else {
                        fraudIds.remove(r.getGameId());
                    }
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

        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));

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
                setStyle(item.equals("Thắng")
                        ? "-fx-text-fill: #2a9d2a;"
                        : "-fx-text-fill: #c0392b;");
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
        } catch (DataAccessException e) {
            showError("Không thể tải dữ liệu:\n" + e.getMessage());
        }
    }

    @FXML
    public void onFilter() {
        String username   = tfUsername.getText().trim().toLowerCase();
        String diffLabel  = cbDifficulty.getValue();

        List<GameResult> filtered = masterList.stream()
                .filter(r -> username.isBlank()
                        || r.getPlayerName().toLowerCase().contains(username))
                .filter(r -> "Tất cả".equals(diffLabel)
                        || diffLabel.equals(r.getDifficultyLabel()))
                .collect(Collectors.toList());

        resultTable.setItems(FXCollections.observableArrayList(filtered));

        if (filtered.isEmpty()) {
            showInfo("Không tìm thấy dữ liệu phù hợp.");
        } else {
            showInfo("Tìm thấy " + filtered.size() + " kết quả.");
        }
    }

    @FXML
    public void onReset() {
        tfUsername.clear();
        cbDifficulty.getSelectionModel().selectFirst();
        fraudIds.clear();
        loadResults();
    }


    @FXML
    public void onSelectAll() {
        ObservableList<GameResult> current = resultTable.getItems();
        boolean anyUnchecked = current.stream()
                .anyMatch(r -> !fraudIds.contains(r.getGameId()));

        if (anyUnchecked) {
            current.forEach(r -> fraudIds.add(r.getGameId()));
        } else {
            current.forEach(r -> fraudIds.remove(r.getGameId()));
        }
        resultTable.refresh();
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

                showInfo("Đã xoá " + toDelete.size() + " kết quả gian lận.");
            } catch (DataAccessException e) {
                showError("Xoá thất bại, dữ liệu không thay đổi:\n" + e.getMessage());
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
}
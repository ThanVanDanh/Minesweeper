package minesweeper.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import minesweeper.model.GameResult;
import minesweeper.repository.GameResultRepository;
import minesweeper.repository.MySqlGameResultRepository;

public class AdminResultController {

    @FXML private ComboBox<String> cbDifficulty;
    @FXML private ComboBox<String> cbResult;
    @FXML private TextField tfUsername;
    @FXML private Label statusLabel;
    @FXML private Label selectedCountLabel;

    @FXML private TableView<GameResult> resultTable;
    @FXML private TableColumn<GameResult, String> colGameId;
    @FXML private TableColumn<GameResult, String> colUsername;
    @FXML private TableColumn<GameResult, Integer> colScore;
    @FXML private TableColumn<GameResult, String> colDifficulty;
    @FXML private TableColumn<GameResult, String> colTime;
    @FXML private TableColumn<GameResult, String> colResult;
    @FXML private TableColumn<GameResult, String> colPlayedAt;
    @FXML private TableColumn<GameResult, Integer> colOpenedCells;

    private ObservableList<GameResult> masterList = FXCollections.observableArrayList();
    private GameResultRepository repository;

    public AdminResultController() {
        repository = new MySqlGameResultRepository();
    }

    @FXML
    public void initialize() {

        cbDifficulty.setItems(FXCollections.observableArrayList("Tất cả","Easy","Medium","Hard","Expert"));
        cbDifficulty.getSelectionModel().selectFirst();

        cbResult.setItems(FXCollections.observableArrayList("Tất cả",  "Thắng", "Thua"));
        cbResult.getSelectionModel().selectFirst();

        setupColumns();
        loadResults();
    }

    private void setupColumns() {
        colGameId.setCellValueFactory( new PropertyValueFactory<>("gameId"));
        colUsername.setCellValueFactory( new PropertyValueFactory<>("playerName"));
        colScore.setCellValueFactory( new PropertyValueFactory<>("score"));
        colDifficulty.setCellValueFactory( new PropertyValueFactory<>("difficultyLabel"));
        colTime.setCellValueFactory( new PropertyValueFactory<>("timeFormatted"));
        colResult.setCellValueFactory( new PropertyValueFactory<>("result"));
        colPlayedAt.setCellValueFactory( new PropertyValueFactory<>("playedAtFormatted"));
        colOpenedCells.setCellValueFactory( new PropertyValueFactory<>("openedCells"));
    }

    private void loadResults() {

        try {
            masterList.clear();
            masterList.addAll( repository.getAllResults() );
            resultTable.setItems(masterList);
            statusLabel.setText("Đã tải "+ masterList.size()+ " kết quả");
        } catch (Exception e) {
            showError("Không thể tải dữ liệu");
        }
    }

    @FXML
    public void onFilter() {
        String username = tfUsername.getText().toLowerCase();
        String difficulty = cbDifficulty.getValue();
        String result = cbResult.getValue();

        ObservableList<GameResult> filteredList = FXCollections.observableArrayList();

        for (GameResult game : masterList) {
            boolean matchUsername =game.getPlayerName().toLowerCase().contains(username);
            boolean matchDifficulty =difficulty.equals("Tất cả")|| game.getDifficultyLabel().equals(difficulty);
            boolean matchResult =result.equals("Tất cả") || game.getResult().equals(result);

            if (matchUsername && matchDifficulty && matchResult) {
                filteredList.add(game);
            }
        }

        resultTable.setItems(filteredList);
        statusLabel.setText("Tìm thấy " + filteredList.size() + " kết quả" );
    }

    @FXML
    public void onReset() {
        tfUsername.clear();
        cbDifficulty.getSelectionModel().selectFirst();
        cbResult.getSelectionModel() .selectFirst();
        resultTable.setItems(masterList);
        statusLabel.setText("Đã reset");
    }

    @FXML
    public void onDeleteFraud() {
        GameResult selected =resultTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showInfo("Hãy chọn 1 dòng để xoá");
            return;
        }
        try {
            masterList.remove(selected);
            resultTable.setItems(masterList);
            statusLabel.setText("Đã xoá kết quả");
        } catch (Exception e) {
            showError("Xoá thất bại");
        }
    }

    @FXML
    public void onSelectAll() {
        resultTable.getSelectionModel().selectAll();
        selectedCountLabel.setText(
                resultTable.getSelectionModel().getSelectedItems().size()+ " đã chọn");
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void showError(String message) {
        Alert alert =new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
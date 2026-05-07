package minesweeper.controller;

import javafx.beans.property.SimpleBooleanProperty;
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

    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Label pageLabel;
    private static final int PAGE_SIZE = 20;
    private int currentPage = 0;
    private int totalPages = 1;
    private MySqlGameResultRepository pagedRepository;

    @FXML private TableView<GameResult> resultTable;
    @FXML private TableColumn<GameResult, Boolean> colSelect;
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
        pagedRepository = (MySqlGameResultRepository) repository;
    }

    @FXML
    public void initialize() {
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        cbDifficulty.setItems(FXCollections.observableArrayList("Tất cả","Easy","Medium","Hard","Expert"));
        cbDifficulty.getSelectionModel().selectFirst();

        cbResult.setItems(FXCollections.observableArrayList("Tất cả",  "Thắng", "Thua"));
        cbResult.getSelectionModel().selectFirst();

        resultTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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
                setGraphic(empty ? null : checkBox);
                if (!empty) {
                    checkBox.setSelected(
                            resultTable.getSelectionModel().getSelectedIndices().contains(getIndex()));
                }
            }
        });
    }

    private void loadResults() {

        try {
            var paged = pagedRepository.getAllResults(currentPage, PAGE_SIZE);
            masterList.setAll(paged.getContent());
            resultTable.setItems(masterList);
            totalPages = Math.max(1, paged.getTotalPages());
            pageLabel.setText("Trang " + (currentPage + 1) + " / " + totalPages);
            btnPrevPage.setDisable(currentPage == 0);
            btnNextPage.setDisable(currentPage >= totalPages - 1);
            statusLabel.setText("Trang " + (currentPage+1) + ": " + masterList.size() + " kết quả");
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
        ObservableList<GameResult> selectedList = resultTable.getSelectionModel().getSelectedItems();

        if (selectedList.isEmpty()) {
            showInfo("Hãy chọn dữ liệu để xoá");
            return;
        }
        try {
            for (GameResult game : selectedList) {
                repository.deleteByGameIds(
                        java.util.List.of(game.getGameId())
                );
                masterList.remove(game);
            }
            resultTable.setItems(masterList);
            statusLabel.setText("Đã xoá "+ selectedList.size()+ " kết quả");
            showInfo("Đã xoá thành công " + selectedList.size() + " kết quả gian lận.");
        } catch (Exception e) {
            showError("Xoá thất bại");
        }
    }

    @FXML
    public void onSelectAll() {
        if (resultTable.getSelectionModel().getSelectedItems().size() == resultTable.getItems().size()) {
            resultTable.getSelectionModel().clearSelection();
        } else {
            resultTable.getSelectionModel().selectAll();
        }
        resultTable.refresh();
        selectedCountLabel.setText(
                resultTable.getSelectionModel().getSelectedItems().size() + " đã chọn");
    }

    @FXML
    public void onPrevPage() {
        if (currentPage > 0) { currentPage--; loadResults(); }
    }

    @FXML
    public void onNextPage() {
        if (currentPage < totalPages - 1) { currentPage++; loadResults(); }
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
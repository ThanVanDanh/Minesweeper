package minesweeper.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import minesweeper.dto.RankingDTO;

import java.util.List;
import javafx.stage.Stage;
import minesweeper.model.GameResult;
import minesweeper.model.User;
import minesweeper.repository.MySqlGameResultRepository;
import minesweeper.service.SessionManager;

public class RankingHistoryController {

    @FXML
    private javafx.scene.control.TabPane tabPane;

    @FXML
    private ComboBox<RankingController.LevelOption> levelCombo;

    @FXML
    private TableView<RankingDTO> rankingTable;

    @FXML
    private TableColumn<RankingDTO, Integer> colRank;

    @FXML
    private TableColumn<RankingDTO, String> colPlayerName;

    @FXML
    private TableColumn<RankingDTO, Integer> colTotalGames;

    @FXML
    private TableColumn<RankingDTO, Integer> colWins;

    @FXML
    private TableColumn<RankingDTO, Integer> colScore;

    // --- Tab Lịch sử ---
    @FXML
    private ComboBox<String> playerCombo;

    @FXML
    private TableView<GameResult> historyTable;

    @FXML
    private TableColumn<GameResult, String> colDate;

    @FXML
    private TableColumn<GameResult, String> colDifficulty;

    @FXML
    private TableColumn<GameResult, String> colResult;

    @FXML
    private TableColumn<GameResult, String> colTime;

    @FXML
    private TableColumn<GameResult, Integer> colHistoryScore;

    // --- Tab Thống kê ---
    @FXML
    private ComboBox<String> statsPlayerCombo;

    @FXML
    private Label statsTotalGames;

    @FXML
    private Label statsWins;

    @FXML
    private Label statsWinRate;

    @FXML
    private Label statsBestScore;

    @FXML
    private Label statsAvgTime;

    private final RankingController rankingController = new RankingController();
    private final MySqlGameResultRepository gameResultRepository = new MySqlGameResultRepository();

    // GD2 -- Phân trang
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label  lblPage;
    @FXML private TableColumn<RankingDTO, String> colBestTime;

    private static final int PAGE_SIZE = 10;
    private int currentPage = 0;
    private List<RankingDTO> allRankings = List.of();

    // Ranking User Alone
    @FXML private VBox myRankBox;
    @FXML private TableView<RankingDTO> myRankTable;
    @FXML private TableColumn<RankingDTO, Integer> colMyRank;
    @FXML private TableColumn<RankingDTO, String>  colMyPlayerName;
    @FXML private TableColumn<RankingDTO, Integer> colMyTotalGames;
    @FXML private TableColumn<RankingDTO, Integer> colMyWins;
    @FXML private TableColumn<RankingDTO, Integer> colMyScore;
    @FXML private TableColumn<RankingDTO, String>  colMyBestTime;

    @FXML
    private void initialize() {
        setupRankingTable();
        setupExpertOnlyLevelFilter();
        
        setupHistoryAndStats();
    }

    private void setupRankingTable() {
        colRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colPlayerName.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        colTotalGames.setCellValueFactory(new PropertyValueFactory<>("totalGames"));
        colWins.setCellValueFactory(new PropertyValueFactory<>("wins"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("bestScore"));

        // GD2
        colBestTime.setCellValueFactory(new PropertyValueFactory<>("bestTimeFormatted"));
        // Ranking User Alone
        colMyRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colMyPlayerName.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        colMyTotalGames.setCellValueFactory(new PropertyValueFactory<>("totalGames"));
        colMyWins.setCellValueFactory(new PropertyValueFactory<>("wins"));
        colMyScore.setCellValueFactory(new PropertyValueFactory<>("bestScore"));
        colMyBestTime.setCellValueFactory(new PropertyValueFactory<>("bestTimeFormatted"));
        // HighLight
        rankingTable.setRowFactory(tv -> buildHighlightRow());
        myRankTable.setRowFactory(tv -> buildHighlightRow());
        // Ẩn header row
        myRankTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                javafx.application.Platform.runLater(() -> {
                    // Ẩn header
                    var header = myRankTable.lookup("TableHeaderRow");
                    if (header != null) {
                        header.setVisible(false);
                        header.setManaged(false);
                    }
                    // Tắt scrollbar dọc
                    myRankTable.lookupAll(".scroll-bar").forEach(node -> {
                        if (node instanceof javafx.scene.control.ScrollBar sb) {
                            sb.setVisible(false);
                            sb.setManaged(false);
                        }
                    });
                });
            }
        });

    }
    private TableRow<RankingDTO> buildHighlightRow() {
        return new TableRow<>() {
            @Override
            protected void updateItem(RankingDTO item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("current-user-row");
                if (!empty && item != null && isCurrentUser(item.getPlayerName())) {
                    getStyleClass().add("current-user-row");
                }
            }
        };
    }

    private boolean isCurrentUser(String playerName) {
        if (!SessionManager.isLoggedIn()) return false;
        String username = SessionManager.getCurrentUser().getUsername();
        return username != null && username.equalsIgnoreCase(playerName);
    }

    private void setupExpertOnlyLevelFilter() {
        List<RankingController.LevelOption> allLevels;
        try {
            allLevels = rankingController.getAllLevels();
        } catch (Exception e) {
            allLevels = List.of();
        }

        levelCombo.setItems(FXCollections.observableArrayList(allLevels));
        if (!allLevels.isEmpty()) {
            levelCombo.getSelectionModel().selectFirst();
            // Load ranking khi chọn level
            levelCombo.setOnAction(event -> loadRankingBySelectedLevel());
            loadRankingBySelectedLevel();
        }
    }

    private void loadRankingBySelectedLevel() {
        RankingController.LevelOption selected = levelCombo.getSelectionModel().getSelectedItem();
        if (selected == null) {
            rankingTable.setItems(FXCollections.observableArrayList());
            return;
        }

        //GD2
        try {
            allRankings = rankingController.getRankingTop(selected.getId(), 50);
            System.out.println("Ranking size = " + allRankings.size());
            currentPage = 0;
            if (SessionManager.isLoggedIn()) {
                String me = SessionManager.getCurrentUser().getUsername();
                for (int i = 0; i < allRankings.size(); i++) {
                    if (allRankings.get(i).getPlayerName().equalsIgnoreCase(me)) {
                        currentPage = i / PAGE_SIZE;
                        break;
                    }
                }
            }
            renderPage();
        } catch (Exception e) {
            allRankings = List.of();
            renderPage();
            e.printStackTrace();
        }
    }

    private void setupHistoryAndStats() {
        // Cấu hình cột bảng lịch sử
        colDate.setCellValueFactory(new PropertyValueFactory<>("playedAtFormatted"));
        colDifficulty.setCellValueFactory(new PropertyValueFactory<>("difficultyLabel"));
        colResult.setCellValueFactory(new PropertyValueFactory<>("result"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timeFormatted"));
        colHistoryScore.setCellValueFactory(new PropertyValueFactory<>("score"));

        if (!SessionManager.isLoggedIn()) {
            playerCombo.setItems(FXCollections.observableArrayList("Vui lòng đăng nhập"));
            statsPlayerCombo.setItems(FXCollections.observableArrayList("Vui lòng đăng nhập"));
            playerCombo.getSelectionModel().selectFirst();
            statsPlayerCombo.getSelectionModel().selectFirst();
            return;
        }

        User currentUser = SessionManager.getCurrentUser();
        String username = currentUser.getUsername();

        playerCombo.setItems(FXCollections.observableArrayList(username));
        playerCombo.getSelectionModel().selectFirst();
        playerCombo.setDisable(true); // Khoá combobox

        statsPlayerCombo.setItems(FXCollections.observableArrayList(username));
        statsPlayerCombo.getSelectionModel().selectFirst();
        statsPlayerCombo.setDisable(true); // Khoá combobox

        loadPlayerHistoryAndStats(username);
    }

    private void loadPlayerHistoryAndStats(String username) {
        try {
            List<GameResult> history = gameResultRepository.getPlayerHistory(username);
            historyTable.setItems(FXCollections.observableArrayList(history));

            // Tính toán thống kê
            int totalGames = history.size();
            int wins = 0;
            int bestScore = 0;
            long totalWinTimeMs = 0;

            for (GameResult r : history) {
                if (r.isWon()) {
                    wins++;
                    totalWinTimeMs += r.getElapsedTimeMs();
                }
                if (r.getScore() > bestScore) {
                    bestScore = r.getScore();
                }
            }

            statsTotalGames.setText(String.valueOf(totalGames));
            statsWins.setText(wins + " / " + (totalGames - wins));
            
            double winRate = totalGames > 0 ? (double) wins / totalGames * 100 : 0;
            statsWinRate.setText(String.format("%.1f%%", winRate));
            
            statsBestScore.setText(String.format("%,d", bestScore));

            if (wins > 0) {
                long avgMs = totalWinTimeMs / wins;
                statsAvgTime.setText((avgMs / 1000) + " giây");
            } else {
                statsAvgTime.setText("0 giây");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void closePopup() {
        if (levelCombo != null && levelCombo.getScene() != null) {
            Stage stage = (Stage) levelCombo.getScene().getWindow();
            stage.close();
        }
    }

    /**
     * Chuyển TabPane sang Tab Lịch sử (index 1)
     */
    public void selectHistoryTab() {
        if (tabPane != null) {
            tabPane.getSelectionModel().select(1);
        }
    }

    // GD2
    private void renderPage() {
        int from  = currentPage * PAGE_SIZE;
        int to    = Math.min(from + PAGE_SIZE, allRankings.size());
        List<RankingDTO> slice = from < to ? allRankings.subList(from, to) : List.of();
        rankingTable.setItems(FXCollections.observableArrayList(slice));
        updatePaginationControls();
        renderMyRank();
    }
    private void renderMyRank() {
        if (myRankBox == null) return;
        myRankBox.setVisible(false);
        myRankBox.setManaged(false);

        if (!SessionManager.isLoggedIn()) return;

        String me = SessionManager.getCurrentUser().getUsername();
        RankingDTO myRow = allRankings.stream()
                .filter(r -> r.getPlayerName().equalsIgnoreCase(me))
                .findFirst()
                .orElse(null);

        if (myRow == null) return;

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, allRankings.size());
        boolean visibleInPage = allRankings.subList(from, to).stream()
                .anyMatch(r -> r.getPlayerName().equalsIgnoreCase(me));

        if (visibleInPage) return;

        myRankTable.setItems(FXCollections.observableArrayList(myRow));
        myRankBox.setVisible(true);
        myRankBox.setManaged(true);
    }

    private void updatePaginationControls() {
        int total = (int) Math.ceil((double) allRankings.size() / PAGE_SIZE);
        int display = allRankings.isEmpty() ? 0 : total;
        lblPage.setText("Trang " + (currentPage + 1) + " / " + Math.max(1, display));
        btnPrev.setDisable(currentPage == 0);
        btnNext.setDisable(currentPage >= display - 1);
    }

    @FXML private void onPrevPage() { if (currentPage > 0) { currentPage--; renderPage(); } }
    @FXML private void onNextPage() {
        int total = (int) Math.ceil((double) allRankings.size() / PAGE_SIZE);
        if (currentPage < total - 1) { currentPage++; renderPage(); }
    }
}


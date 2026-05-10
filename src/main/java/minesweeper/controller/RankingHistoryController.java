package minesweeper.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import minesweeper.dto.RankingDTO;

import java.util.List;
import javafx.stage.Stage;
import minesweeper.model.GameResult;
import minesweeper.model.User;
import minesweeper.repository.MySqlGameResultRepository;
import minesweeper.service.SessionManager;
import javafx.scene.control.Label;

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

        try {
            List<RankingDTO> rankings = rankingController.getRankingTop(selected.getId(), 50);
            rankingTable.setItems(FXCollections.observableArrayList(rankings));
        } catch (Exception e) {
            rankingTable.setItems(FXCollections.observableArrayList());
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
}


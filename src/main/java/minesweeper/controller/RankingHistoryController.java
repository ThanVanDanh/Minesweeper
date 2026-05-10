package minesweeper.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import minesweeper.dto.RankingDTO;

import java.util.List;

public class RankingHistoryController {

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


    private final RankingController rankingController = new RankingController();

    @FXML
    private void initialize() {
        setupRankingTable();
        setupExpertOnlyLevelFilter();
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
            System.out.println("INFO: Loaded ranking for level " + selected.getDisplayName() + " with " + rankings.size() + " items");
        } catch (Exception e) {
            rankingTable.setItems(FXCollections.observableArrayList());
            System.err.println("❌ FAIL: Không thể tải bảng xếp hạng cho level " + selected.getDisplayName());
            e.printStackTrace();
        }
    }
}


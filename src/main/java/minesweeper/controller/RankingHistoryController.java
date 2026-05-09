package minesweeper.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import minesweeper.dto.RankingDTO;

import java.util.List;
import java.util.Locale;

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

    @FXML
    private TableColumn<RankingDTO, String> colWinRate;

    private final RankingController rankingController = new RankingController();

    @FXML
    private void initialize() {
        setupRankingTable();
        setupExpertOnlyLevelFilter();
        loadExpertTop50();
    }

    private void setupRankingTable() {
        colRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colPlayerName.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        colTotalGames.setCellValueFactory(new PropertyValueFactory<>("totalGames"));
        colWins.setCellValueFactory(new PropertyValueFactory<>("wins"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("bestScore"));
        colWinRate.setCellValueFactory(cellData -> {
            RankingDTO row = cellData.getValue();
            double winRate = row.getTotalGames() <= 0 ? 0.0 : (row.getWins() * 100.0 / row.getTotalGames());
            return new ReadOnlyStringWrapper(String.format(Locale.ROOT, "%.1f%%", winRate));
        });
    }

    private void setupExpertOnlyLevelFilter() {
        List<RankingController.LevelOption> expertLevels;
        try {
            expertLevels = rankingController.getAvailableLevels();
        } catch (Exception e) {
            expertLevels = List.of();
        }

        levelCombo.setItems(FXCollections.observableArrayList(expertLevels));
        levelCombo.setDisable(true);
        if (!expertLevels.isEmpty()) {
            levelCombo.getSelectionModel().selectFirst();
        }
    }

    private void loadExpertTop50() {
        try {
            List<RankingDTO> rankings = rankingController.getExpertRankingTop(50);
            rankingTable.setItems(FXCollections.observableArrayList(rankings));
        } catch (Exception e) {
            rankingTable.setItems(FXCollections.observableArrayList());
            System.err.println("Không thể tải bảng xếp hạng Expert Top 50: " + e.getMessage());
        }
    }
}


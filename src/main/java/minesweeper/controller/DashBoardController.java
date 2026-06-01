package minesweeper.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import minesweeper.model.Board;
import minesweeper.model.enums.Difficulty;
import minesweeper.model.User;
import minesweeper.service.SessionManager;
import utils.AdminPopupHelper;
import utils.AuthPopupHelper;

import java.io.IOException;
import java.util.Objects;
import java.util.List;

import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;

import minesweeper.dto.RankingDTO;

public class DashBoardController {
    private static DashBoardController instance;

    @FXML
    private ToggleButton easyButton;
    @FXML
    private ToggleButton mediumButton;
    @FXML
    private ToggleButton hardButton;
    @FXML
    private ToggleButton expertButton;
    @FXML
    private ToggleButton customButton;
    @FXML
    private TextField customRowsField;
    @FXML
    private TextField customColsField;
    @FXML
    private TextField customMinesField;
    @FXML
    private TextField customPlayersField;

    @FXML
    private Button openPopupLogin;
    @FXML
    private Label selectedModeLabel;
    @FXML
    private Parent rootPane;
    @FXML
    private VBox rankingContainer;

    private static final int MIN_BOARD_SIZE = 2;
    private static final int MAX_ROWS = 30;
    private static final int MAX_COLS = 40;
    private static final int MIN_PLAYERS = Board.MIN_PLAYER_COUNT;
    private static final int MAX_PLAYERS = Board.MAX_PLAYER_COUNT;
    private final ToggleGroup difficultyGroup = new ToggleGroup();

    @FXML
    private void initialize() {
        instance = this;
        easyButton.setToggleGroup(difficultyGroup);
        mediumButton.setToggleGroup(difficultyGroup);
        hardButton.setToggleGroup(difficultyGroup);
        expertButton.setToggleGroup(difficultyGroup);
        customButton.setToggleGroup(difficultyGroup);

        easyButton.setSelected(true);
        applySelectedDifficultyDefaults();
        setCustomInputsDisabled(true);
        setupCustomInputListeners();
        updateSelectedModeForCurrentSelection();
//        mediumButton.setSelected(true);
//        updateSelectedMode("TRUNG BÌNH", "16×16 | 40 Min");

        difficultyGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true);
                return;
            }

            applySelectedDifficultyDefaults();
            setCustomInputsDisabled(newToggle != customButton);
            updateSelectedModeForCurrentSelection();
        });
        updateUiBasedOnSession();
        loadTopRanking();
    }

    private void updateUiBasedOnSession() {
        boolean isLoggedIn = SessionManager.isLoggedIn();
        User currentUser = SessionManager.getCurrentUser();

        if (!isLoggedIn || currentUser == null) {
            // Not logged in: show login button, hide player label
            openPopupLogin.setVisible(true);
            openPopupLogin.setManaged(true);
        } else {
            // Logged in: hide login button, show player label
            openPopupLogin.setVisible(false);
            openPopupLogin.setManaged(false);
        }
    }

    public void refreshUI() {
        updateUiBasedOnSession();
        loadTopRanking();
    }

    public static void refreshAllInstances() {
        if (instance != null) {
            instance.refreshUI();
        }
    }

    @FXML
    private void onStartBattle() {
        CustomBoardSelection customSelection = null;
        int playerCount;

        try {
            playerCount = getSelectedPlayerCount();
        } catch (IllegalArgumentException e) {
            selectedModeLabel.setText("Cấu hình số người chơi chưa hợp lệ: " + e.getMessage());
            return;
        }

        if (customButton.isSelected()) {
            try {
                customSelection = getCustomBoardSelection();
                selectedModeLabel.setText("Đang chuẩn bị bàn chơi: TÙY CHỈNH - " + customSelection.meta());
            } catch (IllegalArgumentException e) {
                selectedModeLabel.setText("Cấu hình tùy chỉnh chưa hợp lệ: " + e.getMessage());
                return;
            }
        } else {
            selectedModeLabel.setText("Đang chuẩn bị bàn chơi: " + selectedModeLabel.getText());
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/boardgame.fxml"));
            Parent root = loader.load();

            BoardGameController controller = loader.getController();

            // Truyền thông tin user từ session vào controller
            minesweeper.model.User currentUser = minesweeper.service.SessionManager.getCurrentUser();
            if (currentUser != null) {
                controller.setCurrentUser(currentUser.getId(), currentUser.getUsername());
            } else {
                controller.setCurrentUser(-1, "Khách (Guest)");
            }

            if (customSelection != null) {
                controller.setInitialCustomBoard(
                        customSelection.rows(),
                        customSelection.cols(),
                        customSelection.mines(),
                        customSelection.players()
                );
            } else {
                controller.setInitialDifficulty(getSelectedDifficulty(), playerCount);
            }

            Stage stage = (Stage) selectedModeLabel.getScene().getWindow();
            Scene gameScene = new Scene(root);
            gameScene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/css/styles.css")
            ).toExternalForm());

            stage.setScene(gameScene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi: Không thể tải file boardgame.fxml. Hãy kiểm tra lại đường dẫn!");
        }

    }
    @FXML
    private void openSettingsPage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/settings.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) rootPane.getScene().getWindow();
            double currentWidth = stage.getScene().getWidth();
            double currentHeight = stage.getScene().getHeight();
            Scene scene = new Scene(root, currentWidth, currentHeight);

            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Admin navigation ──────────────────────────────────────────────────────

    public void openAdminUser() {
        try {
            AdminPopupHelper.openAdminUserPopup(rootPane != null ? rootPane : selectedModeLabel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void openAdminResult() {
        try {
            AdminPopupHelper.openAdminResultPopup(rootPane != null ? rootPane : selectedModeLabel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void openAdminWindow(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1100, 700);
            if (rootPane != null && rootPane.getScene() != null) {
                scene.getStylesheets().addAll(rootPane.getScene().getStylesheets());
            }

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.initModality(Modality.APPLICATION_MODAL);

            if (rootPane != null && rootPane.getScene() != null) {
                stage.initOwner(rootPane.getScene().getWindow());
            }

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @FXML
    private void onLogin() {
        openLoginPopup();
    }

    @FXML
    private void onRegister() {
        openRegisterPopup();
    }



    private Difficulty getSelectedDifficulty() {
        if (easyButton.isSelected()) return Difficulty.EASY;
        if (mediumButton.isSelected()) return Difficulty.MEDIUM;
        if (hardButton.isSelected()) return Difficulty.HARD;
        if (expertButton.isSelected()) return Difficulty.EXPERT;
        return Difficulty.MEDIUM;
    }

    private Difficulty getSelectedDifficultyOrNull() {
        if (easyButton.isSelected()) return Difficulty.EASY;
        if (mediumButton.isSelected()) return Difficulty.MEDIUM;
        if (hardButton.isSelected()) return Difficulty.HARD;
        if (expertButton.isSelected()) return Difficulty.EXPERT;
        return null;
    }

    private void updateSelectedMode(String title, String meta) {
        selectedModeLabel.setText("Chế độ đã chọn: " + title + " - " + meta + " | " + getPlayerMeta());
    }

    private void setupCustomInputListeners() {
        List.of(customRowsField, customColsField, customMinesField)
                .forEach(field -> field.textProperty().addListener((obs, oldValue, newValue) -> {
                    if (customButton.isSelected()) {
                        updateCustomModeLabel();
                    }
                }));
        customPlayersField.textProperty().addListener((obs, oldValue, newValue) -> updateSelectedModeForCurrentSelection());
    }

    private void setCustomInputsDisabled(boolean disabled) {
        List.of(customRowsField, customColsField, customMinesField)
                .forEach(field -> field.setDisable(disabled));
    }

    private void applySelectedDifficultyDefaults() {
        Difficulty difficulty = getSelectedDifficultyOrNull();
        if (difficulty == null) {
            return;
        }
        customRowsField.setText(String.valueOf(difficulty.getRows()));
        customColsField.setText(String.valueOf(difficulty.getCols()));
        customMinesField.setText(String.valueOf(difficulty.getMines()));
    }

    private void updateSelectedModeForCurrentSelection() {
        if (customButton.isSelected()) {
            updateCustomModeLabel();
        } else if (easyButton.isSelected()) {
            updateSelectedMode("DỄ", "9×9 | 10 mìn");
        } else if (mediumButton.isSelected()) {
            updateSelectedMode("TRUNG BÌNH", "16×16 | 40 mìn");
        } else if (hardButton.isSelected()) {
            updateSelectedMode("KHÓ", "16×30 | 99 mìn");
        } else if (expertButton.isSelected()) {
            updateSelectedMode("CHUYÊN GIA", "20×30 | 145 mìn");
        }
    }

    private void updateCustomModeLabel() {
        try {
            CustomBoardSelection selection = getCustomBoardSelection();
            selectedModeLabel.setText("Chế độ đã chọn: TÙY CHỈNH - " + selection.meta());
        } catch (IllegalArgumentException e) {
            selectedModeLabel.setText("Chế độ đã chọn: TÙY CHỈNH - nhập hàng, cột, mìn và người chơi");
        }
    }

    private CustomBoardSelection getCustomBoardSelection() {
        int rows = parseCustomNumber(customRowsField, "Số hàng");
        int cols = parseCustomNumber(customColsField, "Số cột");
        int mines = parseCustomNumber(customMinesField, "Số mìn");
        int players = getSelectedPlayerCount();

        if (rows < MIN_BOARD_SIZE || rows > MAX_ROWS) {
            throw new IllegalArgumentException("Số hàng phải từ " + MIN_BOARD_SIZE + " đến " + MAX_ROWS + ".");
        }
        if (cols < MIN_BOARD_SIZE || cols > MAX_COLS) {
            throw new IllegalArgumentException("Số cột phải từ " + MIN_BOARD_SIZE + " đến " + MAX_COLS + ".");
        }
        int maxMines = rows * cols - 1;
        if (mines < 1 || mines > maxMines) {
            throw new IllegalArgumentException("Số mìn phải từ 1 đến " + maxMines + ".");
        }
        if (players < MIN_PLAYERS || players > MAX_PLAYERS) {
            throw new IllegalArgumentException("Số người chơi phải từ " + MIN_PLAYERS + " đến " + MAX_PLAYERS + ".");
        }
        return new CustomBoardSelection(rows, cols, mines, players);
    }

    private int getSelectedPlayerCount() {
        int players = parseCustomNumber(customPlayersField, "Số người chơi");
        if (players < MIN_PLAYERS || players > MAX_PLAYERS) {
            throw new IllegalArgumentException("Số người chơi phải từ " + MIN_PLAYERS + " đến " + MAX_PLAYERS + ".");
        }
        return players;
    }

    private String getPlayerMeta() {
        try {
            return getSelectedPlayerCount() + " người chơi";
        } catch (IllegalArgumentException e) {
            return "số người chơi 1-4";
        }
    }

    private int parseCustomNumber(TextField field, String label) {
        String raw = field.getText();
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " không được để trống.");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " phải là số nguyên.");
        }
    }

    private record CustomBoardSelection(int rows, int cols, int mines, int players) {
        private String meta() {
            return rows + "×" + cols + " | " + mines + " mìn | " + players + " người chơi";
        }
    }

    @FXML
    private void openLoginPopup() {
        openAuthPopup(false);
    }

    @FXML
    private void openRegisterPopup() {
        openAuthPopup(true);
    }

    @FXML
    private void openRankingHistoryPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/ranking-history.fxml")

            );
            System.out.println(getClass().getResource("/app/ranking-history.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1100, 720);
            scene.setFill(Color.TRANSPARENT);

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);
            popupStage.initModality(Modality.APPLICATION_MODAL);

            Stage owner = null;
            if (rootPane != null && rootPane.getScene() != null) {
                owner = (Stage) rootPane.getScene().getWindow();
            } else if (selectedModeLabel != null && selectedModeLabel.getScene() != null) {
                owner = (Stage) selectedModeLabel.getScene().getWindow();
            }
            if (owner != null) {
                popupStage.initOwner(owner);
            }

            popupStage.setTitle("Bảng xếp hạng & lịch sử chơi");
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openAuthPopup(boolean registerMode) {
        try {
            Object ownerNode = rootPane != null ? rootPane : selectedModeLabel;
            AuthPopupHelper.openAuthPopup(ownerNode, registerMode, this::updateUiBasedOnSession);
            updateUiBasedOnSession();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTopRanking() {
        if (rankingContainer == null) return;
        rankingContainer.getChildren().clear();
        try {
            RankingController rankingController = new RankingController();
            // Lấy top 5 ranking của level Expert
            List<RankingDTO> topRanks = rankingController.getExpertRankingTop(5);

            if (topRanks.isEmpty()) {
                Label emptyLabel = new Label("Chưa có dữ liệu xếp hạng.");
                emptyLabel.setStyle("-fx-text-fill: gray;");
                rankingContainer.getChildren().add(emptyLabel);
                return;
            }

            for (int i = 0; i < topRanks.size(); i++) {
                RankingDTO rank = topRanks.get(i);

                HBox row = new HBox();
                row.setAlignment(Pos.CENTER_LEFT);
                row.setSpacing(12);
                row.getStyleClass().add("rank-row");
                if (i == 0) row.getStyleClass().add("rank-row-first");

                Label badge = new Label(String.valueOf(rank.getRank()));
                if (rank.getRank() == 1) badge.getStyleClass().add("rank-badge-gold");
                else if (rank.getRank() == 2) badge.getStyleClass().add("rank-badge-silver");
                else if (rank.getRank() == 3) badge.getStyleClass().add("rank-badge-bronze");
                else badge.getStyleClass().add("rank-badge-blue");

                VBox nameBox = new VBox();
                nameBox.setSpacing(1);
                Label nameLabel = new Label(rank.getPlayerName());
                if (rank.getRank() == 1) nameLabel.getStyleClass().add("rank-name-gold");
                else nameLabel.getStyleClass().add("rank-name");

                Label metaLabel = new Label("Trận: " + rank.getTotalGames() + " | Thắng: " + rank.getWins());
                metaLabel.getStyleClass().add("rank-meta");
                nameBox.getChildren().addAll(nameLabel, metaLabel);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                VBox scoreBox = new VBox();
                scoreBox.setAlignment(Pos.CENTER_RIGHT);
                scoreBox.setSpacing(1);

                long timeSeconds = rank.getBestTimeMs() / 1000;
                Label timeLabel = new Label(timeSeconds + "s");
                timeLabel.getStyleClass().add("rank-score-green");

                Label scoreLabel = new Label(String.format("%,d", rank.getBestScore()));
                scoreLabel.getStyleClass().add("rank-meta");

                scoreBox.getChildren().addAll(timeLabel, scoreLabel);

                row.getChildren().addAll(badge, nameBox, spacer, scoreBox);
                rankingContainer.getChildren().add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Label errorLabel = new Label("Lỗi tải xếp hạng.");
            errorLabel.setStyle("-fx-text-fill: red;");
            rankingContainer.getChildren().add(errorLabel);
        }
    }
}

package minesweeper.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import minesweeper.dto.RankingDTO;
import minesweeper.model.Board;
import minesweeper.model.User;
import minesweeper.model.enums.Difficulty;
import minesweeper.service.SessionManager;
import utils.AdminPopupHelper;
import utils.AuthPopupHelper;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

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

    // Popup nhập tên người chơi trong dashboard.fxml
    @FXML
    private StackPane playerNameOverlay;

    @FXML
    private VBox playerRow1;

    @FXML
    private VBox playerRow2;

    @FXML
    private VBox playerRow3;

    @FXML
    private VBox playerRow4;

    @FXML
    private TextField playerNameField1;

    @FXML
    private TextField playerNameField2;

    @FXML
    private TextField playerNameField3;
    @FXML
    private StackPane mineDensityWarningOverlay;

    @FXML
    private Label mineDensityWarningMessage;

    @FXML
    private TextField playerNameField4;

    private static final int MIN_BOARD_SIZE = 2;
    private static final int MAX_ROWS = 30;
    private static final int MAX_COLS = 40;
    private static final int MIN_PLAYERS = Board.MIN_PLAYER_COUNT;
    private static final int MAX_PLAYERS = Board.MAX_PLAYER_COUNT;
    private static final double HIGH_MINE_DENSITY_THRESHOLD = 0.5;

    private CustomBoardSelection pendingWarningCustomSelection;
    private int pendingWarningPlayerCount;
    private User pendingWarningCurrentUser;

    private final ToggleGroup difficultyGroup = new ToggleGroup();

    private int pendingPlayerCount;
    private CustomBoardSelection pendingCustomSelection;
    private User pendingCurrentUser;

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
            openPopupLogin.setVisible(true);
            openPopupLogin.setManaged(true);
        } else {
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
            // BF-2.1.8 Người chơi nhập số người chơi, mặc định là 1 [UC02]
            // BF-2.1.10 Hệ thống kiểm tra cấu hình ván đấu và số lượng người chơi là hợp lệ [UC02]
            playerCount = getSelectedPlayerCount();
        } catch (IllegalArgumentException e) {
            // AF-2.2.4.4 Hệ thống không chuyển sang màn hình chơi game. Use Case tạm dừng [UC02]
            // AF-2.2.4.3 Hệ thống hiển thị thông báo yêu cầu nhập số nguyên từ 1 đến 4 [UC02]
            selectedModeLabel.setText("Cấu hình số người chơi chưa hợp lệ: " + e.getMessage());
            return;
        }

        if (customButton.isSelected()) {
            try {
                // AF-2.2.1.1 Người chơi chọn chế độ Tùy chỉnh [UC02]
                // AF-2.2.1.3 Người chơi nhập số hàng, số cột và số mìn [UC02]
                customSelection = getCustomBoardSelection();

                // AF-2.2.3.1 Hệ thống tính toán mật độ mìn theo công thức: (Số mìn / Tổng số ô) * 100 [UC02]
                if (isHighMineDensity(customSelection)) {
                    User currentUser = SessionManager.getCurrentUser();

                    // AF-2.2.3.3 Hệ thống hiển thị hộp thoại cảnh báo mật độ mìn cao [UC02]
                    showMineDensityWarning(customSelection, playerCount, currentUser);
                    return;
                }

                // AF-2.2.1.4 Hệ thống cập nhật nhãn chế độ chơi theo thông tin tùy chỉnh [UC02]
                selectedModeLabel.setText("Đang chuẩn bị bàn chơi: TÙY CHỈNH - " + customSelection.meta());

            } catch (IllegalArgumentException e) {
                // AF-2.2.2.2 Hệ thống phát hiện dữ liệu không hợp lệ như bị trống, chứa chữ cái, số hàng/cột ngoài giới hạn hoặc số mìn lớn hơn/bằng tổng số ô [UC02]
                // AF-2.2.2.3 Hệ thống ngừng thao tác chuyển màn hình và không khởi tạo bàn cờ [UC02]
                // AF-2.2.2.4 Hệ thống hiển thị thông báo lỗi tại Dashboard để cảnh báo người chơi [UC02]
                selectedModeLabel.setText("Cấu hình tùy chỉnh chưa hợp lệ: " + e.getMessage());
                return;
            }
        } else {
            // BF-2.1.6 Hệ thống ghi nhận chế độ chơi được chọn [UC02]
            selectedModeLabel.setText("Đang chuẩn bị bàn chơi: " + selectedModeLabel.getText());
        }

        User currentUser = SessionManager.getCurrentUser();

        // BF-2.1.9 Người chơi nhấn nút Bắt đầu [UC02]
        continueOpenGameAfterConfigAccepted(
                customSelection,
                playerCount,
                currentUser
        );
    }

    private void showPlayerNamePopup(
            int playerCount,
            CustomBoardSelection customSelection,
            User currentUser
    ) {
        pendingPlayerCount = playerCount;
        pendingCustomSelection = customSelection;
        pendingCurrentUser = currentUser;

        VBox[] rows = {
                playerRow1,
                playerRow2,
                playerRow3,
                playerRow4
        };

        TextField[] fields = {
                playerNameField1,
                playerNameField2,
                playerNameField3,
                playerNameField4
        };

        // AF-2.2.5.1 Hệ thống hiển thị popup yêu cầu nhập tên tương ứng với số người chơi [UC02]
        // AF-2.2.5.2 Hệ thống điền sẵn tên mặc định như Player 01, Player 02,... [UC02]
        for (int i = 0; i < fields.length; i++) {
            boolean active = i < playerCount;

            rows[i].setVisible(active);
            rows[i].setManaged(active);

            if (active) {
                if (i == 0 && currentUser != null) {
                    fields[i].setText(currentUser.getUsername());
                } else {
                    fields[i].setText("Player " + String.format("%02d", i + 1));
                }
            } else {
                fields[i].clear();
            }
        }

        // AF-2.2.5.1 Hiển thị popup nhập tên người chơi [UC02]
        playerNameOverlay.setVisible(true);
        playerNameOverlay.setManaged(true);
    }

    @FXML
    private void onCancelPlayerNamePopup() {
        // AF-2.2.6.1 Người chơi nhấn "Hủy" tại popup nhập tên [UC02]
        // AF-2.2.6.2 Hệ thống đóng popup nhập tên [UC02]
        // AF-2.2.6.3 Hệ thống không khởi tạo bàn cờ [UC02]
        // AF-2.2.6.4 Use Case kết thúc [UC02]
        hidePlayerNamePopup();

        pendingPlayerCount = 0;
        pendingCustomSelection = null;
        pendingCurrentUser = null;
    }

    @FXML
    private void onConfirmPlayerNamePopup() {
        TextField[] fields = {
                playerNameField1,
                playerNameField2,
                playerNameField3,
                playerNameField4
        };

        // AF-2.2.5.3 Người chơi nhập hoặc chỉnh sửa tên người chơi [UC02]
        // AF-2.2.5.4 Người chơi nhấn "Xác nhận" [UC02]
        String[] playerNames = new String[pendingPlayerCount];

        for (int i = 0; i < pendingPlayerCount; i++) {
            String text = fields[i].getText().trim();
            playerNames[i] = text.isEmpty() ? "Player " + (i + 1) : text;
        }

        // AF-2.2.5.5 Hệ thống ghi nhận danh sách tên người chơi và đóng popup [UC02]
        hidePlayerNamePopup();

        // AF-2.2.5.6 Luồng quay lại BF-2.1.11 [UC02]
        openBoardGame(
                pendingCustomSelection,
                pendingPlayerCount,
                pendingCurrentUser,
                playerNames
        );
    }

    private void hidePlayerNamePopup() {
        playerNameOverlay.setVisible(false);
        playerNameOverlay.setManaged(false);
    }

    private void openBoardGame(
            CustomBoardSelection customSelection,
            int playerCount,
            User currentUser,
            String[] playerNames
    ) {
        try {
            // BF-2.1.11 Hệ thống tải tệp giao diện boardgame.fxml [UC02]
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/boardgame.fxml"));
            Parent root = loader.load();

            BoardGameController controller = loader.getController();

            // BF-2.1.11 Hệ thống truyền dữ liệu cài đặt sang màn chơi [UC02]

            if (currentUser != null) {
                controller.setCurrentUser(currentUser.getId(), currentUser.getUsername());
            } else {
                controller.setCurrentUser(-1, "Khách (Guest)");
            }

            controller.setPlayerNames(playerNames);

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

            // BF-2.1.12 Hệ thống chuyển sang màn hình chơi game [UC02]
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

        customPlayersField.textProperty().addListener(
                (obs, oldValue, newValue) -> updateSelectedModeForCurrentSelection()
        );
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

            AuthPopupHelper.openAuthPopup(
                    ownerNode,
                    registerMode,
                    this::updateUiBasedOnSession
            );

            updateUiBasedOnSession();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTopRanking() {
        if (rankingContainer == null) {
            return;
        }

        rankingContainer.getChildren().clear();

        try {
            RankingController rankingController = new RankingController();

            List<RankingDTO> topRanks = rankingController.getExpertRankingTop(5);

            if (topRanks.isEmpty()) {
                Label emptyLabel = new Label("Chưa có dữ liệu xếp hạng.");
                emptyLabel.getStyleClass().add("ranking-empty-label");
                rankingContainer.getChildren().add(emptyLabel);
                return;
            }

            for (int i = 0; i < topRanks.size(); i++) {
                RankingDTO rank = topRanks.get(i);

                HBox row = new HBox();
                row.setAlignment(Pos.CENTER_LEFT);
                row.setSpacing(12);
                row.getStyleClass().add("rank-row");

                if (i == 0) {
                    row.getStyleClass().add("rank-row-first");
                }

                Label badge = new Label(String.valueOf(rank.getRank()));

                if (rank.getRank() == 1) {
                    badge.getStyleClass().add("rank-badge-gold");
                } else if (rank.getRank() == 2) {
                    badge.getStyleClass().add("rank-badge-silver");
                } else if (rank.getRank() == 3) {
                    badge.getStyleClass().add("rank-badge-bronze");
                } else {
                    badge.getStyleClass().add("rank-badge-blue");
                }

                VBox nameBox = new VBox();
                nameBox.setSpacing(1);

                Label nameLabel = new Label(rank.getPlayerName());

                if (rank.getRank() == 1) {
                    nameLabel.getStyleClass().add("rank-name-gold");
                } else {
                    nameLabel.getStyleClass().add("rank-name");
                }

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
            errorLabel.getStyleClass().add("ranking-error-label");
            rankingContainer.getChildren().add(errorLabel);
        }
    }
    private boolean isHighMineDensity(CustomBoardSelection selection) {
        // AF-2.2.3.1 Hệ thống tính toán mật độ mìn theo công thức: (Số mìn / Tổng số ô) * 100 [UC02]
        int totalCells = selection.rows() * selection.cols();
        double density = (double) selection.mines() / totalCells;

        // AF-2.2.3.2 Hệ thống phát hiện số lượng mìn quá cao so với kích thước bàn cờ [UC02]
        return density > HIGH_MINE_DENSITY_THRESHOLD;
    }

    private void showMineDensityWarning(
            CustomBoardSelection customSelection,
            int playerCount,
            User currentUser
    ) {
        pendingWarningCustomSelection = customSelection;
        pendingWarningPlayerCount = playerCount;
        pendingWarningCurrentUser = currentUser;

        int totalCells = customSelection.rows() * customSelection.cols();
        double densityPercent = ((double) customSelection.mines() / totalCells) * 100;

        // AF-2.2.3.3 Hệ thống hiển thị hộp thoại cảnh báo mật độ mìn cao [UC02]
        mineDensityWarningMessage.setText(
                "Số lượng mìn quá dày đặc.\n\n" +
                        "Bàn chơi: " + customSelection.rows() + "×" + customSelection.cols() + "\n" +
                        "Số mìn: " + customSelection.mines() + "\n" +
                        "Mật độ mìn: " + String.format("%.1f", densityPercent) + "%\n\n" +
                        "Bạn có chắc chắn muốn bắt đầu không?"
        );

        mineDensityWarningOverlay.setVisible(true);
        mineDensityWarningOverlay.setManaged(true);
    }

    @FXML
    private void onContinueMineDensityWarning() {
        // AF-2.2.3.4 Nếu người chơi chọn "Tiếp tục", hệ thống đóng hộp thoại và đi tiếp tới BF-2.1.11 [UC02]
        hideMineDensityWarning();

        selectedModeLabel.setText(
                "Đang chuẩn bị bàn chơi: TÙY CHỈNH - " + pendingWarningCustomSelection.meta()
        );

        continueOpenGameAfterConfigAccepted(
                pendingWarningCustomSelection,
                pendingWarningPlayerCount,
                pendingWarningCurrentUser
        );

        clearMineDensityWarningData();
    }

    @FXML
    private void onChangeMineDensityWarning() {
        // AF-2.2.3.5 Nếu người chơi chọn "Thay đổi", hệ thống đóng hộp thoại và cho phép chỉnh lại số mìn [UC02]
        hideMineDensityWarning();
        clearMineDensityWarningData();

        selectedModeLabel.setText("Hãy điều chỉnh lại số mìn trước khi bắt đầu.");

        customMinesField.requestFocus();
        customMinesField.selectAll();
    }

    private void hideMineDensityWarning() {
        mineDensityWarningOverlay.setVisible(false);
        mineDensityWarningOverlay.setManaged(false);
    }

    private void clearMineDensityWarningData() {
        pendingWarningCustomSelection = null;
        pendingWarningPlayerCount = 0;
        pendingWarningCurrentUser = null;
    }
    private void continueOpenGameAfterConfigAccepted(
            CustomBoardSelection customSelection,
            int playerCount,
            User currentUser
    ) {
        // BF-2.1.10 Hệ thống kiểm tra số lượng người chơi trước khi mở màn chơi [UC02]
        if (playerCount == 1) {
            String[] playerNames = new String[playerCount];
            playerNames[0] = currentUser != null ? currentUser.getUsername() : "Player 01";

            openBoardGame(
                    customSelection,
                    playerCount,
                    currentUser,
                    playerNames
            );

            return;
        }

        // AF-2.2.5 Ván đấu có nhiều người chơi, mở rộng tại BF-2.1.10 [UC02]
        showPlayerNamePopup(
                playerCount,
                customSelection,
                currentUser
        );
    }
}
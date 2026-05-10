package minesweeper.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import minesweeper.model.Difficulty;
import minesweeper.model.User;
import minesweeper.service.SessionManager;
import utils.AuthPopupHelper;

import java.io.IOException;
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
    private Button openPopupLogin;
    @FXML
    private Label selectedModeLabel;
    @FXML
    private Parent rootPane;
    private final ToggleGroup difficultyGroup = new ToggleGroup();

    @FXML
    private void initialize() {
        instance = this;
        easyButton.setToggleGroup(difficultyGroup);
        mediumButton.setToggleGroup(difficultyGroup);
        hardButton.setToggleGroup(difficultyGroup);
        expertButton.setToggleGroup(difficultyGroup);
//        customButton.setToggleGroup(difficultyGroup);

        easyButton.setSelected(true);
        updateSelectedMode("DỄ", "9×9 | 10 Min");
//        mediumButton.setSelected(true);
//        updateSelectedMode("TRUNG BÌNH", "16×16 | 40 Min");

        difficultyGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true);
                return;
            }

            if (newToggle == easyButton) updateSelectedMode("DỄ", "9×9 | 10 Min");
            if (newToggle == mediumButton) updateSelectedMode("TRUNG BÌNH", "16×16 | 40 Min");
            if (newToggle == hardButton) updateSelectedMode("KHÓ", "16×30 | 99 Min");
            if (newToggle == expertButton) updateSelectedMode("CHUYÊN GIA", "20×30 | 145 Min");
//            if (newToggle == customButton) updateSelectedMode("TÙY CHỈNH", "Tự thiết lập");
        });
        updateUiBasedOnSession();

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
    }

    public static void refreshAllInstances() {
        if (instance != null) {
            instance.refreshUI();
        }
    }

    @FXML
    private void onStartBattle() {
        selectedModeLabel.setText("Đang chuẩn bị bàn chơi: " + selectedModeLabel.getText());

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/boardgame.fxml"));
            Parent root = loader.load();

            BoardGameController controller = loader.getController();
            controller.setInitialDifficulty(getSelectedDifficulty());

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

    @FXML
    public void openAdminUser() {
        openAdminWindow("/app/adminUser.fxml", "Quản lý Người dùng – Admin");
    }

    @FXML
    public void openAdminResult() {
        openAdminWindow("/app/adminResult.fxml", "Quản lý Kết quả – Admin");
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

    @FXML
    private void onJoinChallenge() {
        selectedModeLabel.setText("Đã chọn thử thách hằng ngày: Expert mode");
    }

    private Difficulty getSelectedDifficulty() {
        if (easyButton.isSelected()) return Difficulty.EASY;
        if (mediumButton.isSelected()) return Difficulty.MEDIUM;
        if (hardButton.isSelected()) return Difficulty.HARD;
        if (expertButton.isSelected()) return Difficulty.EXPERT;
        return Difficulty.MEDIUM;
    }

    private void updateSelectedMode(String title, String meta) {
        selectedModeLabel.setText("Chế độ đã chọn: " + title + " - " + meta);
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

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.DECORATED);
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


}

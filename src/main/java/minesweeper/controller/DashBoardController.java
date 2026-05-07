package minesweeper.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class DashBoardController {
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
    private Label selectedModeLabel;
    @FXML
    private Parent rootPane;
    private final ToggleGroup difficultyGroup = new ToggleGroup();

    @FXML
    private void initialize() {
        easyButton.setToggleGroup(difficultyGroup);
        mediumButton.setToggleGroup(difficultyGroup);
        hardButton.setToggleGroup(difficultyGroup);
        expertButton.setToggleGroup(difficultyGroup);
        customButton.setToggleGroup(difficultyGroup);

        mediumButton.setSelected(true);
        updateSelectedMode("TRUNG BÌNH", "16×16 | 40 Min");

        difficultyGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true);
                return;
            }

            if (newToggle == easyButton) updateSelectedMode("DỄ", "9×9 | 10 Min");
            if (newToggle == mediumButton) updateSelectedMode("TRUNG BÌNH", "16×16 | 40 Min");
            if (newToggle == hardButton) updateSelectedMode("KHÓ", "16×30 | 99 Min");
            if (newToggle == expertButton) updateSelectedMode("CHUYÊN GIA", "20×30 | 145 Min");
            if (newToggle == customButton) updateSelectedMode("TÙY CHỈNH", "Tự thiết lập");
        });
    }

    @FXML
    private void onStartBattle() {
        selectedModeLabel.setText("Đang chuẩn bị bàn chơi: " + selectedModeLabel.getText());
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/ranking-history.fxml"));
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
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/login.fxml")
            );

            Parent root = loader.load();

            LoginController controller = loader.getController();

            if (registerMode) {
                controller.openAsRegister();
            } else {
                controller.openAsLogin();
            }

            Scene scene = new Scene(root);
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

            popupStage.setScene(scene);
            popupStage.setResizable(false);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

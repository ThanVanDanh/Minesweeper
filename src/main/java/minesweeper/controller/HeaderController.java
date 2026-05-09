package minesweeper.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class HeaderController {

    @FXML
    private VBox headerRoot;

    @FXML
    private void openSettingsPage() {
        if (headerRoot == null || headerRoot.getScene() == null) {
            return;
        }

        if (headerRoot.getScene().getRoot().getStyleClass().contains("settings-root")) {
            return;
        }

        swapScene("/app/settings.fxml");
    }

    @FXML
    public void openAdminUser() {
        openAdminWindow("/app/adminUser.fxml", "Quản lý Người dùng – Admin");
    }

    @FXML
    public void openAdminResult() {
        openAdminWindow("/app/adminResult.fxml", "Quản lý Kết quả – Admin");
    }

    private void swapScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) headerRoot.getScene().getWindow();
            double currentWidth = stage.getScene().getWidth();
            double currentHeight = stage.getScene().getHeight();
            Scene scene = new Scene(root, currentWidth, currentHeight);

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openAdminWindow(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1100, 700);
            String sharedStyles = Objects.requireNonNull(
                    getClass().getResource("/css/styles.css")
            ).toExternalForm();
            scene.getStylesheets().add(sharedStyles);

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.initModality(Modality.APPLICATION_MODAL);

            if (headerRoot != null && headerRoot.getScene() != null) {
                stage.initOwner(headerRoot.getScene().getWindow());
            }

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


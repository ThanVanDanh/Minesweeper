package minesweeper.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import minesweeper.model.Role;
import minesweeper.model.User;
import minesweeper.service.SessionManager;
import utils.AdminPopupHelper;
import utils.AuthPopupHelper;

import java.io.IOException;
import java.util.Objects;

public class HeaderController {

    private static HeaderController instance;

    @FXML
    private VBox headerRoot;

    @FXML
    private Button dashboardButton;

    @FXML
    private Button historyButton;

    @FXML
    private Button settingsButton;

    @FXML
    private Button adminUserButton;

    @FXML
    private Button adminResultButton;

    @FXML
    private Button loginButton;

    @FXML
    private Label playerNameLabel;

    @FXML
    private void initialize() {
        HeaderController.instance = this;
        updateUiBasedOnSession();
    }

    private void updateUiBasedOnSession() {
        boolean isLoggedIn = SessionManager.isLoggedIn();
        User currentUser = SessionManager.getCurrentUser();

        if (!isLoggedIn || currentUser == null) {
            // Not logged in: show login button, hide player label
            loginButton.setVisible(true);
            loginButton.setManaged(true);
            playerNameLabel.setVisible(false);
            playerNameLabel.setManaged(false);

            // Show only Dashboard and Settings; hide other buttons
            dashboardButton.setVisible(true);
            dashboardButton.setManaged(true);
            settingsButton.setVisible(true);
            settingsButton.setManaged(true);
            historyButton.setVisible(false);
            historyButton.setManaged(false);
            adminUserButton.setVisible(false);
            adminUserButton.setManaged(false);
            adminResultButton.setVisible(false);
            adminResultButton.setManaged(false);
        } else {
            // Logged in: show player label, hide login button
            loginButton.setVisible(false);
            loginButton.setManaged(false);
            playerNameLabel.setVisible(true);
            playerNameLabel.setManaged(true);
            playerNameLabel.setText("👤 " + currentUser.getDisplayName());

            // Always show Dashboard, Settings, History
            dashboardButton.setVisible(true);
            dashboardButton.setManaged(true);
            settingsButton.setVisible(true);
            settingsButton.setManaged(true);
            historyButton.setVisible(true);
            historyButton.setManaged(true);

            // Show admin buttons only if role is ADMIN
            if (currentUser.getRole() == Role.ADMIN) {
                adminUserButton.setVisible(true);
                adminUserButton.setManaged(true);
                adminResultButton.setVisible(true);
                adminResultButton.setManaged(true);
            } else {
                adminUserButton.setVisible(false);
                adminUserButton.setManaged(false);
                adminResultButton.setVisible(false);
                adminResultButton.setManaged(false);
            }
        }
    }

    @FXML
    private void handleLoginClick() {
        openAuthPopup(false);
    }

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
    private void openDashboardPage() {
        if (headerRoot == null || headerRoot.getScene() == null) {
            return;
        }

        if (headerRoot.getScene().getRoot().getStyleClass().contains("root-bg")) {
            return; // Đang ở dashboard rồi thì không load lại
        }

        swapScene("/app/dashboard.fxml");
    }

    @FXML
    public void openAdminUser() {
        try {
            AdminPopupHelper.openAdminUserPopup(headerRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void openAdminResult() {
        try {
            AdminPopupHelper.openAdminResultPopup(headerRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void swapScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) headerRoot.getScene().getWindow();
            double currentWidth = stage.getScene().getWidth();
            double currentHeight = stage.getScene().getHeight();
            Scene scene = new Scene(root, currentWidth, currentHeight);
            String sharedStyles = Objects.requireNonNull(
                    getClass().getResource("/css/styles.css")
            ).toExternalForm();
            scene.getStylesheets().add(sharedStyles);

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

    private void openAuthPopup(boolean registerMode) {
        try {
            AuthPopupHelper.openAuthPopup(headerRoot, registerMode, () -> {
                refreshUI();
                DashBoardController.refreshAllInstances();
            });
        } catch (IOException e) {
            e.printStackTrace();
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
}


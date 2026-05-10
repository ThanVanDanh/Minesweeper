package minesweeper.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.service.AuthService;

public class LoginController {
    @FXML
    private Label formTitle;

    @FXML
    private Label formSubtitle;

    @FXML
    private VBox loginForm;

    @FXML
    private VBox registerForm;

    @FXML
    private Button loginTabButton;

    @FXML
    private Button registerTabButton;

    @FXML
    private TextField loginUsernameField;

    @FXML
    private PasswordField loginPasswordField;

    @FXML
    private TextField registerUsernameField;

    @FXML
    private TextField registerDisplayNameField;

    @FXML
    private PasswordField registerPasswordField;

    @FXML
    private PasswordField registerConfirmPasswordField;

    private final AuthService authService = new AuthService();
    private Runnable onLoginSuccess;

    public void showLoginForm() {
        formTitle.setText("ĐĂNG NHẬP");
        formSubtitle.setText("Truy cập hồ sơ chiến thuật của bạn");

        loginForm.setVisible(true);
        loginForm.setManaged(true);

        registerForm.setVisible(false);
        registerForm.setManaged(false);

        loginTabButton.getStyleClass().removeAll("switch-button", "switch-button-active");
        registerTabButton.getStyleClass().removeAll("switch-button", "switch-button-active");

        loginTabButton.getStyleClass().add("switch-button-active");
        registerTabButton.getStyleClass().add("switch-button");

        reCenterStage();
    }

    public void showRegisterForm() {
        formTitle.setText("ĐĂNG KÝ");
        formSubtitle.setText("Khởi tạo tài khoản chiến binh mới");

        loginForm.setVisible(false);
        loginForm.setManaged(false);

        registerForm.setVisible(true);
        registerForm.setManaged(true);

        loginTabButton.getStyleClass().removeAll("switch-button", "switch-button-active");
        registerTabButton.getStyleClass().removeAll("switch-button", "switch-button-active");

        loginTabButton.getStyleClass().add("switch-button");
        registerTabButton.getStyleClass().add("switch-button-active");

        reCenterStage();
    }

    public void openAsLogin() {
        showLoginForm();
    }

    public void openAsRegister() {
        showRegisterForm();
    }

    public void setOnLoginSuccess(Runnable callback) {
        this.onLoginSuccess = callback;
    }

    @FXML
    private void closePopup() {
        Stage stage = (Stage) formTitle.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleLogin() {
        String username = loginUsernameField != null ? loginUsernameField.getText() : null;
        String password = loginPasswordField != null ? loginPasswordField.getText() : null;

        try {
            authService.login(username, password);
            clearLoginFields();
            if (onLoginSuccess != null) {
                onLoginSuccess.run();
            }
            HeaderController.refreshAllInstances();
            DashBoardController.refreshAllInstances();
            closePopup();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (DataAccessException e) {
            showError("Không thể đăng nhập. Vui lòng thử lại.");
        }
    }

    @FXML
    private void handleRegister() {
        String username = registerUsernameField != null ? registerUsernameField.getText() : null;
        String password = registerPasswordField != null ? registerPasswordField.getText() : null;
        String confirmPassword = registerConfirmPasswordField != null ? registerConfirmPasswordField.getText() : null;

        if (password == null || confirmPassword == null || !password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp.");
            return;
        }

        String displayName = null;
        if (registerDisplayNameField != null) {
            displayName = registerDisplayNameField.getText();
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = username; // fallback to username when display name not provided
        }

        try {
            authService.register(username, displayName, password);
            clearRegisterFields();
            showInfo("Đăng ký thành công. Vui lòng đăng nhập.");
            showLoginForm();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (DataAccessException e) {
            showError("Không thể đăng ký. Vui lòng thử lại.");
        }
    }

    private void clearLoginFields() {
        if (loginUsernameField != null) loginUsernameField.clear();
        if (loginPasswordField != null) loginPasswordField.clear();
    }

    private void clearRegisterFields() {
        if (registerUsernameField != null) registerUsernameField.clear();
        if(registerDisplayNameField != null) registerDisplayNameField.clear();
        if (registerPasswordField != null) registerPasswordField.clear();
        if (registerConfirmPasswordField != null) registerConfirmPasswordField.clear();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Thông báo");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Thông báo");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void reCenterStage() {
        if (formTitle != null && formTitle.getScene() != null) {
            Stage stage = (Stage) formTitle.getScene().getWindow();
            // Đợi 1 nhịp UI để JavaFX tính toán lại kích thước mới của các thành phần bên trong
            javafx.application.Platform.runLater(() -> {
                stage.sizeToScene();
                stage.centerOnScreen();
            });
        }
    }
}

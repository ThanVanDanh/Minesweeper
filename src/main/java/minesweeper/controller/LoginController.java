package minesweeper.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Modality;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.service.AuthService;
import minesweeper.service.ForgotPasswordService;
import minesweeper.model.User;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.Optional;

public class LoginController {
    @FXML private Label  formTitle;
    @FXML private Label  formSubtitle;
    @FXML private VBox   loginForm;
    @FXML private VBox   registerForm;
    @FXML private Button loginTabButton;
    @FXML private Button registerTabButton;
    @FXML private TextField     loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private CheckBox      rememberMeCheckBox;
    @FXML private TextField     registerUsernameField;
    @FXML private TextField     registerDisplayNameField;
    @FXML private TextField     registerEmailField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField registerConfirmPasswordField;
    private final AuthService authService = new AuthService();
    private Runnable onLoginSuccess;
    private long pendingVerifyUserId = -1;
    public void setOnLoginSuccess(Runnable callback) {
        this.onLoginSuccess = callback;
    }
    public void showLoginForm() {
        formTitle.setText("ĐĂNG NHẬP");
        formSubtitle.setText("Truy cập hồ sơ chiến thuật của bạn");
        loginForm.setVisible(true);
        loginForm.setManaged(true);
        registerForm.setVisible(false);
        registerForm.setManaged(false);
        setActiveTab(loginTabButton, registerTabButton);
        reCenterStage();
    }

    /**
     * 1.1.1 Người dùng chọn "Đăng ký" trên màn hình chào.
     * showRegisterForm(): hiển thị form đăng ký.
     */
    public void showRegisterForm() {
        formTitle.setText("ĐĂNG KÝ");
        formSubtitle.setText("Khởi tạo tài khoản chiến binh mới");
        loginForm.setVisible(false);
        loginForm.setManaged(false);
        registerForm.setVisible(true);
        registerForm.setManaged(true);
        setActiveTab(registerTabButton, loginTabButton);
        reCenterStage();
    }
    public void openAsLogin() {
        showLoginForm();
    }

    /**
     * 1.1.1 Người dùng chọn "Đăng ký" trên màn hình chào.
     */
    public void openAsRegister() {
        showRegisterForm();
    }

    @FXML
    private void closePopup() {
        getStage().close();
    }

    /**
     * UC01 - AF01.2: Đăng nhập (phía UI).
     * Đảm nhiệm: 1.2.2, 1.2.10..1.2.14 và xử lý lỗi 1.2.E1, 1.2.E2.
     */
    @FXML
    private void handleLogin() {
        // 1.2.2 Người dùng nhập username, password và nhấn "Đăng nhập".
        String username = text(loginUsernameField);
        String password = text(loginPasswordField);
        boolean remember = rememberMeCheckBox != null && rememberMeCheckBox.isSelected();

        try {
            // 1.2.3 AuthService.login(username, password, remember): validate, truy vấn user, verify password, tạo session.
            authService.login(username, password, remember);

            // 1.2.10 clearLoginFields(): xóa dữ liệu form đăng nhập.
            clearLoginFields();

            if (onLoginSuccess != null) {
                // 1.2.11 onLoginSuccess.run(): thực thi callback sau đăng nhập.
                onLoginSuccess.run();
            }

            // 1.2.12 HeaderController.refreshAllInstances(): cập nhật Header theo trạng thái đã đăng nhập.
            HeaderController.refreshAllInstances();
            // 1.2.13 DashBoardController.refreshAllInstances(): cập nhật Dashboard theo trạng thái đã đăng nhập.
            DashBoardController.refreshAllInstances();
            // 1.2.14 closePopup(): đóng popup đăng nhập.
            closePopup();
        } catch (IllegalArgumentException e) {
            // 1.2.E1 Dữ liệu đăng nhập không hợp lệ hoặc xác thực thất bại.
            showError(e.getMessage());
        } catch (DataAccessException e) {
            // 1.2.E2 Lỗi truy cập CSDL khi đăng nhập.
            showError("Không thể đăng nhập. Vui lòng thử lại.");
        }
    }

    /**
     * UC01 - AF01.1: Đăng ký tài khoản (phía UI).
     * Đảm nhiệm: 1.1.2..1.1.5 (thu thập/kiểm tra input) và xử lý lỗi 1.1.E1..1.1.E3.
     * Lưu ý: flow hiện tại có bước xác nhận email (OTP) sau khi tạo tài khoản.
     */
    @FXML
    private void handleRegister() {
        // 1.1.2 Người dùng nhập thông tin và nhấn "Tạo tài khoản".
        String username = text(registerUsernameField);
        String displayName = text(registerDisplayNameField);
        String email = text(registerEmailField);
        String password = text(registerPasswordField);
        String confirmPassword = text(registerConfirmPasswordField);

        // 1.1.3 Kiểm tra confirmPassword khớp password.
        if (password == null || confirmPassword == null || !password.equals(confirmPassword)) {
            // 1.1.E1 Mật khẩu xác nhận không khớp.
            showError("Mật khẩu xác nhận không khớp.");
            return;
        }

        // 1.1.4 Chuẩn hóa displayName, nếu rỗng thì dùng username.
        if (displayName == null || displayName.isBlank()) {
            displayName = username;
        }

        try {
            // 1.1.5 AuthService.register(...): validate, kiểm tra trùng, hash mật khẩu, lưu DB (inactive) và gửi OTP.
            User newUser = authService.register(username, displayName, email, password);
            pendingVerifyUserId = newUser.getId();

            // 1.1.9 clearRegisterFields(): xóa dữ liệu form đăng ký.
            clearRegisterFields();

            // Hiển thị dialog xác nhận email/OTP.
            showOtpDialog(email);
        } catch (IllegalArgumentException e) {
            // 1.1.E2 Dữ liệu đăng ký không hợp lệ hoặc username/email đã tồn tại.
            showError(e.getMessage());
        } catch (DataAccessException e) {
            // 1.1.E3 Lỗi truy cập CSDL khi đăng ký.
            showError("Không thể đăng ký. Vui lòng thử lại.");
        } catch (MessagingException e) {
            showError("Đăng ký thành công nhưng không gửi được email xác nhận.\n" +
                    "Vui lòng liên hệ admin.");
        }
    }
    private void showOtpDialog(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/otp-verification.fxml"));
            VBox root = loader.load();
            OtpVerificationController controller = loader.getController();
            controller.setup(pendingVerifyUserId, email);
            controller.setOnSuccess(() -> {
                clearRegisterFields();
                showLoginForm();
            });
            controller.setOnCancel(() -> {
                showInfo("Bạn có thể xác nhận email sau khi đăng nhập.");
            });
            Stage stage = new Stage();
            stage.setTitle("Xác nhận Email");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 450, 400));
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.showAndWait();

        } catch (IOException e) {
            showError("Lỗi tải giao diện: " + e.getMessage());
        }
    }
    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/forgot-password.fxml"));
            VBox root = loader.load();
            ForgotPasswordController controller = loader.getController();
            controller.setOnSuccess(() -> {
                showLoginForm();
            });
            Stage stage = new Stage();
            stage.setTitle("Quên mật khẩu");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 450, 300));
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.showAndWait();

        } catch (IOException e) {
            showError("Lỗi tải giao diện: " + e.getMessage());
        }
    }
    private void clearLoginFields() {
        if (loginUsernameField  != null) loginUsernameField.clear();
        if (loginPasswordField  != null) loginPasswordField.clear();
        if (rememberMeCheckBox  != null) rememberMeCheckBox.setSelected(false);
    }
    private void clearRegisterFields() {
        if (registerUsernameField     != null) registerUsernameField.clear();
        if (registerDisplayNameField  != null) registerDisplayNameField.clear();
        if (registerEmailField        != null) registerEmailField.clear();
        if (registerPasswordField     != null) registerPasswordField.clear();
        if (registerConfirmPasswordField != null) registerConfirmPasswordField.clear();
    }

    private void setActiveTab(Button active, Button inactive) {
        active.getStyleClass().removeAll("switch-button", "switch-button-active");
        inactive.getStyleClass().removeAll("switch-button", "switch-button-active");
        active.getStyleClass().add("switch-button-active");
        inactive.getStyleClass().add("switch-button");
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

    private String text(TextField field) {
        return field != null ? field.getText() : null;
    }

    private Stage getStage() {
        return (Stage) formTitle.getScene().getWindow();
    }

    private void reCenterStage() {
        if (formTitle != null && formTitle.getScene() != null) {
            Platform.runLater(() -> {
                Stage stage = getStage();
                stage.sizeToScene();
                stage.centerOnScreen();
            });
        }
    }
}
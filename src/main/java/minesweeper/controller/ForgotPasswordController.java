package minesweeper.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import minesweeper.service.ForgotPasswordService;
import minesweeper.repository.exception.DataAccessException;
import jakarta.mail.MessagingException;

public class ForgotPasswordController {

    @FXML private VBox step1Box;
    @FXML private VBox step2Box;
    @FXML private VBox loadingOverlay;
    @FXML private javafx.scene.control.Label loadingLabel;

    @FXML private TextField emailField;
    @FXML private TextField otpField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Button sendButton;
    @FXML private Button resetButton;

    private Runnable onSuccess;
    private final ForgotPasswordService forgotService = new ForgotPasswordService();

    @FXML
    private void initialize() {
        // Khởi tạo: hiện bước 1, ẩn bước 2
        step1Box.setVisible(true);
        step1Box.setManaged(true);
        step2Box.setVisible(false);
        step2Box.setManaged(false);
    }

    @FXML
    private void handleSendOtp() {
        String email = emailField.getText().trim();

        // Validation
        if (email.isEmpty()) {
            showError("Vui lòng nhập email.");
            return;
        }

        if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError("Email không hợp lệ.");
            return;
        }

        setLoadingState(sendButton, true, "Đang gửi...");

        Task<Void> sendTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                forgotService.sendPasswordResetOtp(email);
                return null;
            }
        };

        sendTask.setOnSucceeded(e -> {
            setLoadingState(sendButton, false, "📧 GỬI OTP");
            showInfo("✓ Mã OTP đã được gửi đến:\n\n" + email +
                    "\n\nVui lòng kiểm tra hộp thư (kể cả thư rác). Mã có hiệu lực trong 30 phút.");
            
            // Chuyển sang bước 2
            step1Box.setVisible(false);
            step1Box.setManaged(false);
            step2Box.setVisible(true);
            step2Box.setManaged(true);
            
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.sizeToScene();
        });

        sendTask.setOnFailed(e -> {
            setLoadingState(sendButton, false, "📧 GỬI OTP");
            Throwable error = sendTask.getException();
            if (error instanceof IllegalArgumentException) {
                showError(error.getMessage());
            } else {
                showError("Không thể gửi mã OTP. Vui lòng thử lại sau.");
                error.printStackTrace();
            }
        });

        new Thread(sendTask).start();
    }

    @FXML
    private void handleResetPassword() {
        String otp = otpField.getText().trim();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (otp.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showError("Vui lòng nhập đầy đủ OTP và mật khẩu mới.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showError("Mật khẩu xác nhận không khớp.");
            return;
        }

        setLoadingState(resetButton, true, "Đang xử lý...");

        Task<Void> resetTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                forgotService.resetPassword(otp, newPass);
                return null;
            }
        };

        resetTask.setOnSucceeded(e -> {
            setLoadingState(resetButton, false, "💾 ĐỔI MẬT KHẨU");
            showInfo("Đổi mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.");
            if (onSuccess != null) onSuccess.run();
            closeDialog();
        });

        resetTask.setOnFailed(e -> {
            setLoadingState(resetButton, false, "💾 ĐỔI MẬT KHẨU");
            Throwable error = resetTask.getException();
            if (error instanceof IllegalArgumentException) {
                showError(error.getMessage());
            } else {
                showError("Đã xảy ra lỗi hệ thống. Không thể đổi mật khẩu.");
                error.printStackTrace();
            }
        });

        new Thread(resetTask).start();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    public void setOnSuccess(Runnable callback) {
        this.onSuccess = callback;
    }

    private void closeDialog() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
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

    private void setLoadingState(Button button, boolean isLoading, String text) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(isLoading);
            if (loadingLabel != null) {
                loadingLabel.setText(text);
            }
        }
        button.setDisable(isLoading);
    }
}

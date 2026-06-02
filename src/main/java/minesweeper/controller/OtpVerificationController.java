package minesweeper.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.service.AuthService;
import minesweeper.service.MySqlUserService;
import minesweeper.model.User;
import java.util.Optional;
public class OtpVerificationController {

    @FXML private Label emailLabel;
    @FXML private TextField otpField;
    @FXML private Button resendButton;
    @FXML private Button okButton;
    @FXML private Button cancelButton;

    private long pendingUserId = -1;
    private String pendingEmail;
    private AuthService authService = new AuthService();
    private Runnable onSuccess;
    private Runnable onCancel;

    public void initialize() {
        otpField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                otpField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (otpField.getText().length() > 6) {
                otpField.setText(otpField.getText().substring(0, 6));
            }
        });
    }

    public void setup(long userId, String email) {
        this.pendingUserId = userId;
        this.pendingEmail = email;
        emailLabel.setText("Đến: " + email);
    }

    public void setOnSuccess(Runnable callback) {
        this.onSuccess = callback;
    }
    public void setOnCancel(Runnable callback) {
        this.onCancel = callback;
    }

    @FXML
    private void handleVerify() {
        String otp = otpField.getText().trim();
        if (otp.isEmpty() || otp.length() != 6 || !otp.matches("\\d{6}")) {
            showError("Mã OTP phải có đúng 6 chữ số.");
            return;
        }
        if (pendingUserId < 0) return;

        try {
            authService.verifyEmail(pendingUserId, otp);
            showInfo("Xác nhận email thành công! Vui lòng đăng nhập.");
            if (onSuccess != null) onSuccess.run();
            closeDialog();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (DataAccessException e) {
            showError("Không thể xác nhận. Vui lòng thử lại.");
        }
    }

    @FXML
    private void handleResendOtp() {
        if (pendingUserId < 0) return;
        try {
            authService.resendOtp(pendingUserId);
            showInfo("Đã gửi lại OTP đến " + pendingEmail);
            otpField.clear();
            otpField.requestFocus();
        } catch (Exception e) {
            showError("Không thể gửi lại OTP: " + e.getMessage());
        }
    }
    @FXML
    private void handleCancel() {
        if (onCancel != null) onCancel.run();
        closeDialog();
    }
    private void closeDialog() {
        Stage stage = (Stage) okButton.getScene().getWindow();
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

}



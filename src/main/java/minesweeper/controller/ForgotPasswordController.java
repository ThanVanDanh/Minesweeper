package minesweeper.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import minesweeper.service.ForgotPasswordService;
import jakarta.mail.MessagingException;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Button sendButton;
    @FXML private Button cancelButton;

    private Runnable onSuccess;

    @FXML
    private void handleSendLink() {
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
        try {
            ForgotPasswordService forgotService = new ForgotPasswordService();
            forgotService.sendPasswordResetLink(email);

            showInfo("✓ Link đặt lại mật khẩu đã được gửi đến:\n\n" + email +
                    "\n\nVui lòng kiểm tra hộp thư (kể cả thư rác). Link có hiệu lực trong 30 phút.");

            if (onSuccess != null) onSuccess.run();
            closeDialog();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Không thể gửi link reset. Vui lòng thử lại sau.");
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    public void setOnSuccess(Runnable callback) {
        this.onSuccess = callback;
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
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

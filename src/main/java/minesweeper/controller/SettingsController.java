package minesweeper.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import minesweeper.model.User;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.service.AuthService;
import minesweeper.service.MySqlUserService;
import minesweeper.service.SessionManager;

import java.io.IOException;
import java.util.Objects;

public class SettingsController {

    @FXML private BorderPane settingsRoot;

    @FXML private Button visualSoundTab;
    @FXML private Button gameplayTab;
    @FXML private Button controlTab;
    @FXML private Button accountTab;

    @FXML private VBox visualSoundPane;
    @FXML private VBox gameplayPane;
    @FXML private VBox controlPane;
    @FXML private VBox accountPane;

    @FXML private ComboBox<String> resolutionCombo;
    @FXML private ComboBox<String> themeCombo;
    @FXML private ComboBox<String> difficultyCombo;
    @FXML private ComboBox<String> clickModeCombo;

    @FXML private ToggleButton gridEffectToggle;
    @FXML private ToggleButton particleToggle;
    @FXML private ToggleButton soundToggle;
    @FXML private ToggleButton musicToggle;
    @FXML private ToggleButton notificationToggle;
    @FXML private ToggleButton safeFirstClickToggle;
    @FXML private ToggleButton autoFlagToggle;
    @FXML private ToggleButton questionMarkToggle;
    @FXML private ToggleButton doubleClickToggle;
    @FXML private ToggleButton shortcutToggle;

    @FXML private Slider brightnessSlider;
    @FXML private Slider mainVolumeSlider;
    @FXML private Slider sfxVolumeSlider;
    @FXML private Slider musicVolumeSlider;
    @FXML private Slider sensitivitySlider;

    @FXML private Label brightnessValue;
    @FXML private Label mainVolumeValue;
    @FXML private Label sfxVolumeValue;
    @FXML private Label musicVolumeValue;
    @FXML private Label sensitivityValue;

    @FXML private TextField playerNameField;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label accountMessageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        setupComboBoxes();
        setupSliders();
        setupDefaultValues();
        setupAccountTab();
        showVisualSound();
    }

    private void setupAccountTab() {
        User currentUser = SessionManager.getCurrentUser();
        boolean isLoggedIn = SessionManager.isLoggedIn();

        if (!isLoggedIn || currentUser == null) {
            // Not logged in: hide account tab
            accountTab.setVisible(false);
            accountTab.setManaged(false);
            accountPane.setVisible(false);
            accountPane.setManaged(false);
        } else {
            // Logged in: show account tab and load user info
            accountTab.setVisible(true);
            accountTab.setManaged(true);

            // Load user info into fields
            if (playerNameField != null) {
                playerNameField.setText(currentUser.getDisplayName());
            }
        }
    }

    private void setupComboBoxes() {
        resolutionCombo.getItems().setAll(
                "1280 x 720",
                "1366 x 768",
                "1600 x 900",
                "1920 x 1080",
                "2560 x 1440"
        );

        themeCombo.getItems().setAll(
                "DarkTech Neon",
                "Cyber Blue",
                "Emerald Grid",
                "Classic Minesweeper"
        );

        difficultyCombo.getItems().setAll(
                "Dễ - 9x9 | 10 Min",
                "Trung bình - 16x16 | 40 Min",
                "Khó - 16x30 | 99 Min",
                "Chuyên gia - 20x30 | 145 Min",
                "Tùy chỉnh"
        );

        clickModeCombo.getItems().setAll(
                "Chuột trái mở ô / Chuột phải cắm cờ",
                "Chuột trái cắm cờ / Chuột phải mở ô",
                "Giữ Shift để cắm cờ",
                "Chế độ một tay"
        );
    }

    private void setupSliders() {
        bindSlider(brightnessSlider, brightnessValue);
        bindSlider(mainVolumeSlider, mainVolumeValue);
        bindSlider(sfxVolumeSlider, sfxVolumeValue);
        bindSlider(musicVolumeSlider, musicVolumeValue);
        bindSlider(sensitivitySlider, sensitivityValue);
    }

    private void bindSlider(Slider slider, Label label) {
        label.setText((int) slider.getValue() + "%");

        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            label.setText(newValue.intValue() + "%");
        });
    }

    private void setupDefaultValues() {
        resolutionCombo.setValue("1920 x 1080");
        themeCombo.setValue("DarkTech Neon");
        difficultyCombo.setValue("Trung bình - 16x16 | 40 Min");
        clickModeCombo.setValue("Chuột trái mở ô / Chuột phải cắm cờ");

        gridEffectToggle.setSelected(true);
        particleToggle.setSelected(true);
        soundToggle.setSelected(true);
        musicToggle.setSelected(true);
        notificationToggle.setSelected(true);

        safeFirstClickToggle.setSelected(true);
        autoFlagToggle.setSelected(false);
        questionMarkToggle.setSelected(true);

        doubleClickToggle.setSelected(true);
        shortcutToggle.setSelected(true);

        brightnessSlider.setValue(80);
        mainVolumeSlider.setValue(70);
        sfxVolumeSlider.setValue(85);
        musicVolumeSlider.setValue(40);
        sensitivitySlider.setValue(60);
    }

    @FXML
    private void showVisualSound() {
        showPane(visualSoundPane, visualSoundTab);
    }

    @FXML
    private void showGameplay() {
        showPane(gameplayPane, gameplayTab);
    }

    @FXML
    private void showControl() {
        showPane(controlPane, controlTab);
    }

    @FXML
    private void showAccount() {
        showPane(accountPane, accountTab);
    }

    private void showPane(VBox activePane, Button activeTab) {
        visualSoundPane.setVisible(false);
        visualSoundPane.setManaged(false);

        gameplayPane.setVisible(false);
        gameplayPane.setManaged(false);

        controlPane.setVisible(false);
        controlPane.setManaged(false);

        accountPane.setVisible(false);
        accountPane.setManaged(false);

        visualSoundTab.getStyleClass().remove("setting-tab-active");
        gameplayTab.getStyleClass().remove("setting-tab-active");
        controlTab.getStyleClass().remove("setting-tab-active");
        accountTab.getStyleClass().remove("setting-tab-active");

        activePane.setVisible(true);
        activePane.setManaged(true);

        if (!activeTab.getStyleClass().contains("setting-tab-active")) {
            activeTab.getStyleClass().add("setting-tab-active");
        }

        if (accountMessageLabel != null) {
            accountMessageLabel.setText("");
        }
    }

    @FXML
    private void saveSettings() {
        if (accountMessageLabel != null) {
            accountMessageLabel.setText("Đã lưu cài đặt hệ thống.");
        }

        System.out.println("Saved settings:");
        System.out.println("Resolution: " + resolutionCombo.getValue());
        System.out.println("Theme: " + themeCombo.getValue());
        System.out.println("Brightness: " + (int) brightnessSlider.getValue());
        System.out.println("Main Volume: " + (int) mainVolumeSlider.getValue());
    }

    @FXML
    private void resetSettings() {
        setupDefaultValues();

        if (currentPasswordField != null) {
            currentPasswordField.clear();
        }

        if (newPasswordField != null) {
            newPasswordField.clear();
        }

        if (confirmPasswordField != null) {
            confirmPasswordField.clear();
        }

        if (accountMessageLabel != null) {
            accountMessageLabel.setText("Đã đặt lại cài đặt mặc định.");
        }
    }

    @FXML
    private void updatePlayerName() {
        String newName = playerNameField.getText();

        if (newName == null || newName.trim().isEmpty()) {
            accountMessageLabel.setText("Tên người chơi không được để trống.");
            return;
        }

        if (newName.trim().length() < 3) {
            accountMessageLabel.setText("Tên người chơi cần ít nhất 3 ký tự.");
            return;
        }

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            accountMessageLabel.setText("Vui lòng đăng nhập để cập nhật tên.");
            return;
        }

        try {
            // Update in database via authService (which uses MySqlUserService)
            MySqlUserService userService = new MySqlUserService();
            userService.updateDisplayName(currentUser.getId(), newName.trim());
            
            // Update current session
            currentUser.setDisplayName(newName.trim());
            SessionManager.createSession(currentUser);
            
            accountMessageLabel.setText("Đã cập nhật tên người chơi thành: " + newName.trim());
        } catch (Exception e) {
            accountMessageLabel.setText("Không thể cập nhật tên. Vui lòng thử lại.");
            e.printStackTrace();
        }
    }

    /**
     * UC01.4 - Đổi mật khẩu
     */
    @FXML
    private void changePassword() {
        // Basic flow 01.4.1 Người dùng nhập mật khẩu hiện tại, mật khẩu mới, xác nhận mật khẩu mới và nhấn ĐỔI MẬT KHẨU.
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Basic flow 01.4.2 Hệ thống kiểm tra các trường không rỗng.
        if (currentPassword == null || currentPassword.isEmpty()
                || newPassword == null || newPassword.isEmpty()
                || confirmPassword == null || confirmPassword.isEmpty()) {
            // Exception flow E01.1 Thiếu thông tin mật khẩu.
            accountMessageLabel.setText("Vui lòng nhập đầy đủ thông tin mật khẩu.");
            return;
        }

        // Basic flow 01.4.3 Hệ thống kiểm tra mật khẩu mới >= 6 ký tự và khớp xác nhận. (Khớp xác nhận)
        if (!newPassword.equals(confirmPassword)) {
            // Exception flow E01.5 Mật khẩu mới không khớp xác nhận.
            accountMessageLabel.setText("Mật khẩu xác nhận không khớp.");
            return;
        }

        // 1.4.5 SessionManager.getCurrentUser(): kiểm tra người dùng đang đăng nhập.
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            // 1.4.E3 Người dùng chưa đăng nhập.
            accountMessageLabel.setText("Vui lòng đăng nhập để đổi mật khẩu.");
            return;
        }

        try {
            // AuthService sẽ xử lý: 01.4.4 Lấy hash, 01.4.5 Xác minh, 01.4.6 Hash mới, 01.4.7 Cập nhật hash
            authService.changePassword(currentUser.getId(), currentPassword, newPassword);
            
            // Basic flow 01.4.8 Hệ thống xóa các ô nhập và hiển thị thông báo thành công.
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            accountMessageLabel.setText("Đổi mật khẩu thành công.");
        } catch (IllegalArgumentException e) {
            // Bắt Exception flow E01.8 Mật khẩu hiện tại sai, hoặc các lỗi do validator ném ra.
            accountMessageLabel.setText(e.getMessage());
        } catch (DataAccessException e) {
            // 1.4.E5 Lỗi truy cập CSDL khi đổi mật khẩu.
            accountMessageLabel.setText("Không thể đổi mật khẩu. Vui lòng thử lại.");
        }
    }

    /**
     * UC01.3 - Đăng xuất
     */
    @FXML
    private void logout() {
        // Basic flow 01.3.1 Người dùng chọn Đăng xuất từ Header hoặc Cài đặt.
        
        // (Xử lý Basic flow 01.3.2 và 01.3.3 bên trong authService.logout)
        authService.logout();
        
        // Basic flow 01.3.4 Hệ thống xóa dữ liệu form tài khoản.
        clearAccountFields();
        
        // Basic flow 01.3.5 Hệ thống điều hướng về Dashboard.
        navigateToDashboard();
    }

    private void clearAccountFields() {
        if (playerNameField != null) playerNameField.clear();
        if (currentPasswordField != null) currentPasswordField.clear();
        if (newPasswordField != null) newPasswordField.clear();
        if (confirmPasswordField != null) confirmPasswordField.clear();
        if (accountMessageLabel != null) accountMessageLabel.setText("");
    }

    private void navigateToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/dashboard.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) settingsRoot.getScene().getWindow();
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

    @FXML
    private void openSettingsPage() {
        // Already on Settings; no-op to avoid FXML handler errors.
    }

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

            if (settingsRoot != null && settingsRoot.getScene() != null) {
                stage.initOwner(settingsRoot.getScene().getWindow());
            }

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
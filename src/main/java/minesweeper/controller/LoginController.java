package minesweeper.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
    }

    public void openAsLogin() {
        showLoginForm();
    }

    public void openAsRegister() {
        showRegisterForm();
    }

    @FXML
    private void closePopup() {
        Stage stage = (Stage) formTitle.getScene().getWindow();
        stage.close();
    }
}


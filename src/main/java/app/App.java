package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import minesweeper.model.User;
import minesweeper.service.AuthService;
import minesweeper.service.SessionManager;
import utils.AuthPopupHelper;

import java.io.IOException;
import java.util.Objects;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Thử auto-login bằng Remember Me token trước khi hiện UI
        AuthService authService = new AuthService();
        User autoUser = authService.tryAutoLogin();

        Parent root = FXMLLoader.load(Objects.requireNonNull(
                App.class.getResource("/app/dashboard.fxml")
        ));
        Scene scene = new Scene(root, 1280, 760);
        scene.getStylesheets().add(Objects.requireNonNull(
                App.class.getResource("/css/styles.css")
        ).toExternalForm());

        stage.setTitle("Minesweeper Tactical - Dashboard");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();

        if (autoUser == null && SessionManager.getCurrentUser() == null) {
            AuthPopupHelper.openAuthPopup(root, false, null);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
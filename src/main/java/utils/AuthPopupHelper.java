package utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import minesweeper.controller.LoginController;

import java.io.IOException;

public final class AuthPopupHelper {
    private AuthPopupHelper() {
    }

    public static void openAuthPopup(Object ownerNode, boolean registerMode, Runnable onLoginSuccess) throws IOException {
        FXMLLoader loader = new FXMLLoader(AuthPopupHelper.class.getResource("/app/login.fxml"));
        Parent root = loader.load();

        LoginController controller = loader.getController();
        controller.setOnLoginSuccess(onLoginSuccess);
        if (registerMode) {
            controller.openAsRegister();
        } else {
            controller.openAsLogin();
        }

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        Stage popupStage = new Stage();
        popupStage.initStyle(StageStyle.TRANSPARENT);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        if (ownerNode instanceof javafx.scene.Node node && node.getScene() != null) {
            popupStage.initOwner(node.getScene().getWindow());
        }

        popupStage.setScene(scene);
        popupStage.setResizable(false);
        popupStage.centerOnScreen();
        popupStage.showAndWait();
    }
}
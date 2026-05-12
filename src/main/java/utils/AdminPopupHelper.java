package utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public final class AdminPopupHelper {
    private AdminPopupHelper() {}

    public static void openAdminResultPopup(Object ownerNode) throws IOException {
        openPopup(ownerNode, "/app/adminResult.fxml");
    }

    public static void openAdminUserPopup(Object ownerNode) throws IOException {
        openPopup(ownerNode, "/app/adminUser.fxml");
    }

    private static void openPopup(Object ownerNode, String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(AdminPopupHelper.class.getResource(fxmlPath));
        Parent root = loader.load();

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
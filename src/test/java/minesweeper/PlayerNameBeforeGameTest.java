package minesweeper;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import minesweeper.controller.BoardGameController;
import minesweeper.controller.DashBoardController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PlayerNameBeforeGameTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX đã được khởi động ở test khác thì bỏ qua
        }
    }

    @Test
    void showPlayerNamePopup_WithThreePlayers_ShouldShowThreeRowsAndDefaultNames() throws Exception {
        DashBoardController controller = new DashBoardController();

        StackPane overlay = new StackPane();

        VBox row1 = new VBox();
        VBox row2 = new VBox();
        VBox row3 = new VBox();
        VBox row4 = new VBox();

        TextField name1 = new TextField();
        TextField name2 = new TextField();
        TextField name3 = new TextField();
        TextField name4 = new TextField();

        setPrivateField(controller, "playerNameOverlay", overlay);

        setPrivateField(controller, "playerRow1", row1);
        setPrivateField(controller, "playerRow2", row2);
        setPrivateField(controller, "playerRow3", row3);
        setPrivateField(controller, "playerRow4", row4);

        setPrivateField(controller, "playerNameField1", name1);
        setPrivateField(controller, "playerNameField2", name2);
        setPrivateField(controller, "playerNameField3", name3);
        setPrivateField(controller, "playerNameField4", name4);

        invokePrivateMethod(
                controller,
                "showPlayerNamePopup",
                new Class[]{int.class, getCustomBoardSelectionClass(), getUserClass()},
                new Object[]{3, null, null}
        );

        assertTrue(overlay.isVisible());
        assertTrue(overlay.isManaged());

        assertTrue(row1.isVisible());
        assertTrue(row2.isVisible());
        assertTrue(row3.isVisible());
        assertFalse(row4.isVisible());

        assertTrue(row1.isManaged());
        assertTrue(row2.isManaged());
        assertTrue(row3.isManaged());
        assertFalse(row4.isManaged());

        assertEquals("Player 01", name1.getText());
        assertEquals("Player 02", name2.getText());
        assertEquals("Player 03", name3.getText());
        assertEquals("", name4.getText());
    }

    @Test
    void playerCanChangeNameInPopup_BeforeEnteringGame() throws Exception {
        DashBoardController controller = new DashBoardController();

        StackPane overlay = new StackPane();

        VBox row1 = new VBox();
        VBox row2 = new VBox();
        VBox row3 = new VBox();
        VBox row4 = new VBox();

        TextField name1 = new TextField();
        TextField name2 = new TextField();
        TextField name3 = new TextField();
        TextField name4 = new TextField();

        setPrivateField(controller, "playerNameOverlay", overlay);

        setPrivateField(controller, "playerRow1", row1);
        setPrivateField(controller, "playerRow2", row2);
        setPrivateField(controller, "playerRow3", row3);
        setPrivateField(controller, "playerRow4", row4);

        setPrivateField(controller, "playerNameField1", name1);
        setPrivateField(controller, "playerNameField2", name2);
        setPrivateField(controller, "playerNameField3", name3);
        setPrivateField(controller, "playerNameField4", name4);

        invokePrivateMethod(
                controller,
                "showPlayerNamePopup",
                new Class[]{int.class, getCustomBoardSelectionClass(), getUserClass()},
                new Object[]{2, null, null}
        );

        name1.setText("An");
        name2.setText("Bình");

        assertEquals("An", name1.getText());
        assertEquals("Bình", name2.getText());
    }

    @Test
    void cancelPlayerNamePopup_ShouldHidePopupAndClearPendingData() throws Exception {
        DashBoardController controller = new DashBoardController();

        StackPane overlay = new StackPane();

        VBox row1 = new VBox();
        VBox row2 = new VBox();
        VBox row3 = new VBox();
        VBox row4 = new VBox();

        TextField name1 = new TextField();
        TextField name2 = new TextField();
        TextField name3 = new TextField();
        TextField name4 = new TextField();

        setPrivateField(controller, "playerNameOverlay", overlay);

        setPrivateField(controller, "playerRow1", row1);
        setPrivateField(controller, "playerRow2", row2);
        setPrivateField(controller, "playerRow3", row3);
        setPrivateField(controller, "playerRow4", row4);

        setPrivateField(controller, "playerNameField1", name1);
        setPrivateField(controller, "playerNameField2", name2);
        setPrivateField(controller, "playerNameField3", name3);
        setPrivateField(controller, "playerNameField4", name4);

        invokePrivateMethod(
                controller,
                "showPlayerNamePopup",
                new Class[]{int.class, getCustomBoardSelectionClass(), getUserClass()},
                new Object[]{2, null, null}
        );

        assertTrue(overlay.isVisible());

        invokePrivateMethod(
                controller,
                "onCancelPlayerNamePopup",
                new Class[]{},
                new Object[]{}
        );

        assertFalse(overlay.isVisible());
        assertFalse(overlay.isManaged());

        assertEquals(0, getPrivateField(controller, "pendingPlayerCount"));
        assertNull(getPrivateField(controller, "pendingCustomSelection"));
        assertNull(getPrivateField(controller, "pendingCurrentUser"));
    }

    @Test
    void boardGameController_SetPlayerNames_ShouldDisplayChangedNames() throws Exception {
        BoardGameController controller = new BoardGameController();

        TextField playerName1 = new TextField();
        TextField playerName2 = new TextField();
        TextField playerName3 = new TextField();
        TextField playerName4 = new TextField();

        setPrivateField(controller, "playerName1", playerName1);
        setPrivateField(controller, "playerName2", playerName2);
        setPrivateField(controller, "playerName3", playerName3);
        setPrivateField(controller, "playerName4", playerName4);

        controller.setPlayerNames(new String[]{
                "An",
                "Bình",
                "Cường",
                "Dung"
        });

        assertEquals("An", playerName1.getText());
        assertEquals("Bình", playerName2.getText());
        assertEquals("Cường", playerName3.getText());
        assertEquals("Dung", playerName4.getText());
    }

    @Test
    void boardGameController_SetPlayerNames_ShouldTrimName() throws Exception {
        BoardGameController controller = new BoardGameController();

        TextField playerName1 = new TextField();
        TextField playerName2 = new TextField();
        TextField playerName3 = new TextField();
        TextField playerName4 = new TextField();

        setPrivateField(controller, "playerName1", playerName1);
        setPrivateField(controller, "playerName2", playerName2);
        setPrivateField(controller, "playerName3", playerName3);
        setPrivateField(controller, "playerName4", playerName4);

        controller.setPlayerNames(new String[]{
                "  An  ",
                "  Bình  "
        });

        assertEquals("An", playerName1.getText());
        assertEquals("Bình", playerName2.getText());
        assertEquals("Player 03", playerName3.getText());
        assertEquals("Player 04", playerName4.getText());
    }

    @Test
    void boardGameController_SetPlayerNames_WithBlankName_ShouldUseDefaultName() throws Exception {
        BoardGameController controller = new BoardGameController();

        TextField playerName1 = new TextField();
        TextField playerName2 = new TextField();
        TextField playerName3 = new TextField();
        TextField playerName4 = new TextField();

        setPrivateField(controller, "playerName1", playerName1);
        setPrivateField(controller, "playerName2", playerName2);
        setPrivateField(controller, "playerName3", playerName3);
        setPrivateField(controller, "playerName4", playerName4);

        controller.setPlayerNames(new String[]{
                "",
                "   ",
                null
        });

        assertEquals("Player 01", playerName1.getText());
        assertEquals("Player 02", playerName2.getText());
        assertEquals("Player 03", playerName3.getText());
        assertEquals("Player 04", playerName4.getText());
    }

    @Test
    void getPlayerDisplayName_ShouldReturnChangedName() throws Exception {
        BoardGameController controller = new BoardGameController();

        TextField playerName1 = new TextField("An");
        TextField playerName2 = new TextField("Bình");
        TextField playerName3 = new TextField("");
        TextField playerName4 = new TextField("");

        setPrivateField(controller, "playerName1", playerName1);
        setPrivateField(controller, "playerName2", playerName2);
        setPrivateField(controller, "playerName3", playerName3);
        setPrivateField(controller, "playerName4", playerName4);

        String name1 = (String) invokePrivateMethod(
                controller,
                "getPlayerDisplayName",
                new Class[]{int.class},
                new Object[]{0}
        );

        String name2 = (String) invokePrivateMethod(
                controller,
                "getPlayerDisplayName",
                new Class[]{int.class},
                new Object[]{1}
        );

        String name3 = (String) invokePrivateMethod(
                controller,
                "getPlayerDisplayName",
                new Class[]{int.class},
                new Object[]{2}
        );

        assertEquals("An", name1);
        assertEquals("Bình", name2);
        assertEquals("Player 03", name3);
    }

    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object invokePrivateMethod(
            Object target,
            String methodName,
            Class<?>[] parameterTypes,
            Object[] args
    ) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Class<?> getCustomBoardSelectionClass() throws ClassNotFoundException {
        return Class.forName("minesweeper.controller.DashBoardController$CustomBoardSelection");
    }

    private static Class<?> getUserClass() throws ClassNotFoundException {
        return Class.forName("minesweeper.model.User");
    }
}
package minesweeper;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import minesweeper.controller.DashBoardController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class DashBoardControllerValidationTest {

    private DashBoardController controller;

    private static boolean javaFxStarted = false;

    @BeforeAll
    static void initJavaFxToolkit() {
        if (!javaFxStarted) {
            Platform.startup(() -> {});
            javaFxStarted = true;
        }
    }
    @BeforeEach
    void setUp() throws Exception {
        controller = new DashBoardController();

        setPrivateField("customRowsField", new TextField());
        setPrivateField("customColsField", new TextField());
        setPrivateField("customMinesField", new TextField());
        setPrivateField("customPlayersField", new TextField());
    }

    @Test
    void getSelectedPlayerCount_WithValidOnePlayer_ShouldReturnOne() {
        setText("customPlayersField", "1");

        int result = (int) invokePrivateMethod("getSelectedPlayerCount");

        assertEquals(1, result);
    }

    @Test
    void getSelectedPlayerCount_WithValidFourPlayers_ShouldReturnFour() {
        setText("customPlayersField", "4");

        int result = (int) invokePrivateMethod("getSelectedPlayerCount");

        assertEquals(4, result);
    }

    @Test
    void getSelectedPlayerCount_WithEmptyValue_ShouldThrowException() {
        setText("customPlayersField", "");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invokePrivateMethod("getSelectedPlayerCount")
        );

        assertTrue(exception.getMessage().contains("không được để trống"));
    }

    @Test
    void getSelectedPlayerCount_WithTextValue_ShouldThrowException() {
        setText("customPlayersField", "abc");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invokePrivateMethod("getSelectedPlayerCount")
        );

        assertTrue(exception.getMessage().contains("phải là số nguyên"));
    }

    @Test
    void getSelectedPlayerCount_WithZeroPlayer_ShouldThrowException() {
        setText("customPlayersField", "0");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invokePrivateMethod("getSelectedPlayerCount")
        );

        assertTrue(exception.getMessage().contains("Số người chơi phải từ 1 đến 4"));
    }

    @Test
    void getSelectedPlayerCount_WithFivePlayers_ShouldThrowException() {
        setText("customPlayersField", "5");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invokePrivateMethod("getSelectedPlayerCount")
        );

        assertTrue(exception.getMessage().contains("Số người chơi phải từ 1 đến 4"));
    }

    @Test
    void getCustomBoardSelection_WithValidData_ShouldReturnSelection() throws Exception {
        setText("customRowsField", "10");
        setText("customColsField", "12");
        setText("customMinesField", "20");
        setText("customPlayersField", "3");

        Object selection = invokePrivateMethod("getCustomBoardSelection");

        assertEquals(10, getRecordValue(selection, "rows"));
        assertEquals(12, getRecordValue(selection, "cols"));
        assertEquals(20, getRecordValue(selection, "mines"));
        assertEquals(3, getRecordValue(selection, "players"));
    }

    @Test
    void getCustomBoardSelection_WithRowsLessThanMin_ShouldThrowException() {
        setText("customRowsField", "1");
        setText("customColsField", "10");
        setText("customMinesField", "5");
        setText("customPlayersField", "1");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invokePrivateMethod("getCustomBoardSelection")
        );

        assertTrue(exception.getMessage().contains("Số hàng phải từ 2 đến 30"));
    }

    @Test
    void getCustomBoardSelection_WithRowsGreaterThanMax_ShouldThrowException() {
        setText("customRowsField", "31");
        setText("customColsField", "10");
        setText("customMinesField", "5");
        setText("customPlayersField", "1");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invokePrivateMethod("getCustomBoardSelection")
        );

        assertTrue(exception.getMessage().contains("Số hàng phải từ 2 đến 30"));
    }

    @Test
    void getCustomBoardSelection_WithColsLessThanMin_ShouldThrowException() {
        setText("customRowsField", "10");
        setText("customColsField", "1");
        setText("customMinesField", "5");
        setText("customPlayersField", "1");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invokePrivateMethod("getCustomBoardSelection")
        );

        assertTrue(exception.getMessage().contains("Số cột phải từ 2 đến 40"));
    }

    @Test
    void getCustomBoardSelection_WithColsGreaterThanMax_ShouldThrowException() {
        setText("customRowsField", "10");
        setText("customColsField", "41");
        setText("customMinesField", "5");
        setText("customPlayersField", "1");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invokePrivateMethod("getCustomBoardSelection")
        );

        assertTrue(exception.getMessage().contains("Số cột phải từ 2 đến 40"));
    }

    @Test
    void getCustomBoardSelection_WithMineZero_ShouldThrowException() {
        setText("customRowsField", "10");
        setText("customColsField", "10");
        setText("customMinesField", "0");
        setText("customPlayersField", "1");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invokePrivateMethod("getCustomBoardSelection")
        );

        assertTrue(exception.getMessage().contains("Số mìn phải từ 1"));
    }

    @Test
    void getCustomBoardSelection_WithMineEqualTotalCells_ShouldThrowException() {
        setText("customRowsField", "2");
        setText("customColsField", "2");
        setText("customMinesField", "4");
        setText("customPlayersField", "1");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invokePrivateMethod("getCustomBoardSelection")
        );

        assertTrue(exception.getMessage().contains("Số mìn phải từ 1 đến 3"));
    }

    private void setText(String fieldName, String value) {
        TextField field = (TextField) getPrivateField(fieldName);
        field.setText(value);
    }

    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = DashBoardController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    private Object getPrivateField(String fieldName) {
        try {
            Field field = DashBoardController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(controller);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object invokePrivateMethod(String methodName) {
        try {
            Method method = DashBoardController.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(controller);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            if (cause instanceof Error error) {
                throw error;
            }

            throw new RuntimeException(cause);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object getRecordValue(Object record, String methodName) throws Exception {
        Method method = record.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(record);
    }
}
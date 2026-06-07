package minesweeper.TestUC02;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import minesweeper.controller.DashBoardController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class DashBoardControllerValidationUC02Test {
    private static boolean javaFxStarted = false;

    private DashBoardController controller;
    private TextField rowsField;
    private TextField colsField;
    private TextField minesField;
    private TextField playersField;

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

        rowsField = new TextField();
        colsField = new TextField();
        minesField = new TextField();
        playersField = new TextField();

        setField(controller, "customRowsField", rowsField);
        setField(controller, "customColsField", colsField);
        setField(controller, "customMinesField", minesField);
        setField(controller, "customPlayersField", playersField);
    }

    private void runTestCase(String testId, String testName, Executable executable) throws Throwable {
        try {
            executable.execute();
            System.out.println("[PASS] " + testId + " - " + testName);
        } catch (Throwable e) {
            System.out.println("[FAIL] " + testId + " - " + testName);
            System.out.println("       Lỗi: " + e.getMessage());
            throw e;
        }
    }

    @Test
    void UC02_UT10_shouldReturnValidPlayerCount() throws Throwable {
        runTestCase("UC02-UT10", "Kiểm tra số người chơi hợp lệ", () -> {
            playersField.setText("4");

            int result = (int) invoke(controller, "getSelectedPlayerCount");

            assertEquals(4, result);
        });
    }

    @Test
    void UC02_UT11_shouldThrowExceptionWhenPlayerCountBlank() throws Throwable {
        runTestCase("UC02-UT11", "Số người chơi bị bỏ trống", () -> {
            playersField.setText("");

            assertThrows(IllegalArgumentException.class, () -> {
                invoke(controller, "getSelectedPlayerCount");
            });
        });
    }

    @Test
    void UC02_UT12_shouldThrowExceptionWhenPlayerCountIsText() throws Throwable {
        runTestCase("UC02-UT12", "Số người chơi chứa chữ", () -> {
            playersField.setText("abc");

            assertThrows(IllegalArgumentException.class, () -> {
                invoke(controller, "getSelectedPlayerCount");
            });
        });
    }

    @Test
    void UC02_UT13_shouldThrowExceptionWhenPlayerCountOutOfRange() throws Throwable {
        runTestCase("UC02-UT13", "Số người chơi nằm ngoài khoảng 1 đến 4", () -> {
            playersField.setText("5");

            assertThrows(IllegalArgumentException.class, () -> {
                invoke(controller, "getSelectedPlayerCount");
            });
        });
    }

    @Test
    void UC02_UT14_shouldCreateValidCustomBoardSelection() throws Throwable {
        runTestCase("UC02-UT14", "Tạo cấu hình bàn chơi tùy chỉnh hợp lệ", () -> {
            rowsField.setText("10");
            colsField.setText("10");
            minesField.setText("20");
            playersField.setText("1");

            Object selection = invoke(controller, "getCustomBoardSelection");

            assertEquals(10, invokeRecordAccessor(selection, "rows"));
            assertEquals(10, invokeRecordAccessor(selection, "cols"));
            assertEquals(20, invokeRecordAccessor(selection, "mines"));
            assertEquals(1, invokeRecordAccessor(selection, "players"));
        });
    }

    @Test
    void UC02_UT15_shouldThrowExceptionWhenCustomRowsBlank() throws Throwable {
        runTestCase("UC02-UT15", "Số hàng tùy chỉnh bị bỏ trống", () -> {
            rowsField.setText("");
            colsField.setText("10");
            minesField.setText("20");
            playersField.setText("1");

            assertThrows(IllegalArgumentException.class, () -> {
                invoke(controller, "getCustomBoardSelection");
            });
        });
    }

    @Test
    void UC02_UT16_shouldThrowExceptionWhenCustomRowsIsText() throws Throwable {
        runTestCase("UC02-UT16", "Số hàng tùy chỉnh chứa chữ", () -> {
            rowsField.setText("abc");
            colsField.setText("10");
            minesField.setText("20");
            playersField.setText("1");

            assertThrows(IllegalArgumentException.class, () -> {
                invoke(controller, "getCustomBoardSelection");
            });
        });
    }

    @Test
    void UC02_UT17_shouldThrowExceptionWhenRowsOrColsOutOfRange() throws Throwable {
        runTestCase("UC02-UT17", "Số hàng hoặc số cột nằm ngoài giới hạn", () -> {
            rowsField.setText("1");
            colsField.setText("10");
            minesField.setText("20");
            playersField.setText("1");

            assertThrows(IllegalArgumentException.class, () -> {
                invoke(controller, "getCustomBoardSelection");
            });
        });
    }

    @Test
    void UC02_UT18_shouldThrowExceptionWhenMinesGreaterOrEqualTotalCells() throws Throwable {
        runTestCase("UC02-UT18", "Số mìn lớn hơn hoặc bằng tổng số ô", () -> {
            rowsField.setText("5");
            colsField.setText("5");
            minesField.setText("25");
            playersField.setText("1");

            assertThrows(IllegalArgumentException.class, () -> {
                invoke(controller, "getCustomBoardSelection");
            });
        });
    }

    @Test
    void UC02_UT19_shouldReturnFalseWhenMineDensityEqualsThreshold() throws Throwable {
        runTestCase("UC02-UT19", "Mật độ mìn bằng ngưỡng 50% thì không cảnh báo", () -> {
            rowsField.setText("10");
            colsField.setText("10");
            minesField.setText("50");
            playersField.setText("1");

            Object selection = invoke(controller, "getCustomBoardSelection");
            boolean highDensity = (boolean) invoke(controller, "isHighMineDensity", selection);

            assertFalse(highDensity);
        });
    }

    @Test
    void UC02_UT20_shouldReturnTrueWhenMineDensityGreaterThanThreshold() throws Throwable {
        runTestCase("UC02-UT20", "Mật độ mìn lớn hơn 50% thì cảnh báo", () -> {
            rowsField.setText("10");
            colsField.setText("10");
            minesField.setText("51");
            playersField.setText("1");

            Object selection = invoke(controller, "getCustomBoardSelection");
            boolean highDensity = (boolean) invoke(controller, "isHighMineDensity", selection);

            assertTrue(highDensity);
        });
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object invoke(Object target, String methodName, Object... args) throws Exception {
        Method method = null;

        for (Method m : target.getClass().getDeclaredMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                method = m;
                break;
            }
        }

        if (method == null) {
            throw new NoSuchMethodException(methodName);
        }

        method.setAccessible(true);

        try {
            return method.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();

            if (cause instanceof Exception) {
                throw (Exception) cause;
            }

            throw new RuntimeException(cause);
        }
    }

    private static int invokeRecordAccessor(Object record, String accessor) throws Exception {
        Method method = record.getClass().getDeclaredMethod(accessor);
        method.setAccessible(true);
        return (int) method.invoke(record);
    }
}
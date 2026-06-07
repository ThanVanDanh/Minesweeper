package minesweeper.TestUC02;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import minesweeper.controller.DashBoardController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class DashBoardControllerComponentUC02Test {

    private DashBoardController controller;
    private static boolean javaFxStarted = false;


    private ToggleButton easyButton, mediumButton, hardButton, expertButton, customButton;
    private TextField rowsField, colsField, minesField, playersField;
    private Label selectedModeLabel, mineDensityWarningMessage;
    private StackPane mineDensityWarningOverlay, playerNameOverlay;
    private VBox playerRow1, playerRow2, playerRow3, playerRow4;
    private TextField playerNameField1, playerNameField2, playerNameField3, playerNameField4;

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

        easyButton = new ToggleButton("Dễ");
        mediumButton = new ToggleButton("Trung bình");
        hardButton = new ToggleButton("Khó");
        expertButton = new ToggleButton("Chuyên gia");
        customButton = new ToggleButton("Tùy chỉnh");

        rowsField = new TextField();
        colsField = new TextField();
        minesField = new TextField();
        playersField = new TextField("1");
        selectedModeLabel = new Label();

        mineDensityWarningOverlay = new StackPane();
        mineDensityWarningMessage = new Label();

        playerNameOverlay = new StackPane();
        playerRow1 = new VBox();
        playerRow2 = new VBox();
        playerRow3 = new VBox();
        playerRow4 = new VBox();

        playerNameField1 = new TextField();
        playerNameField2 = new TextField();
        playerNameField3 = new TextField();
        playerNameField4 = new TextField();

        setField("easyButton", easyButton);
        setField("mediumButton", mediumButton);
        setField("hardButton", hardButton);
        setField("expertButton", expertButton);
        setField("customButton", customButton);

        setField("customRowsField", rowsField);
        setField("customColsField", colsField);
        setField("customMinesField", minesField);
        setField("customPlayersField", playersField);
        setField("selectedModeLabel", selectedModeLabel);

        setField("mineDensityWarningOverlay", mineDensityWarningOverlay);
        setField("mineDensityWarningMessage", mineDensityWarningMessage);

        setField("playerNameOverlay", playerNameOverlay);
        setField("playerRow1", playerRow1);
        setField("playerRow2", playerRow2);
        setField("playerRow3", playerRow3);
        setField("playerRow4", playerRow4);

        setField("playerNameField1", playerNameField1);
        setField("playerNameField2", playerNameField2);
        setField("playerNameField3", playerNameField3);
        setField("playerNameField4", playerNameField4);
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
    void UC02_CT05_shouldEnableCustomInputsWhenCustomModeSelected() throws Throwable {
        runTestCase("UC02-CT05", "Chọn Tùy chỉnh thì mở khóa ô hàng, cột, mìn", () -> {
            customButton.setSelected(true);

            invoke("setCustomInputsDisabled", false);

            assertFalse(rowsField.isDisabled());
            assertFalse(colsField.isDisabled());
            assertFalse(minesField.isDisabled());
        });
    }

    @Test
    void UC02_CT07_shouldShowErrorAndStopWhenCustomConfigInvalid() throws Throwable {
        runTestCase("UC02-CT07", "Cấu hình tùy chỉnh không hợp lệ thì hiển thị lỗi và dừng chuyển màn hình", () -> {
            customButton.setSelected(true);

            rowsField.setText("1");
            colsField.setText("10");
            minesField.setText("20");
            playersField.setText("1");

            invoke("onStartBattle");

            assertTrue(selectedModeLabel.getText().contains("Cấu hình tùy chỉnh chưa hợp lệ"));
        });
    }

    @Test
    void UC02_CT08_shouldShowMineDensityWarningWhenMineDensityTooHigh() throws Throwable {
        runTestCase("UC02-CT08", "Mật độ mìn quá cao thì hiển thị cảnh báo", () -> {
            customButton.setSelected(true);

            rowsField.setText("10");
            colsField.setText("10");
            minesField.setText("60");
            playersField.setText("1");

            invoke("onStartBattle");

            assertTrue(mineDensityWarningOverlay.isVisible());
            assertTrue(mineDensityWarningMessage.getText().contains("Số lượng mìn quá dày đặc"));
            assertTrue(mineDensityWarningMessage.getText().contains("60.0%"));
        });
    }

    @Test
    void UC02_CT10_shouldCancelGameCreationWhenUserChoosesChangeInMineWarning() throws Throwable {
        runTestCase("UC02-CT10", "Chọn Thay đổi trong cảnh báo mật độ mìn thì hủy tạo ván đấu", () -> {
            customButton.setSelected(true);

            rowsField.setText("10");
            colsField.setText("10");
            minesField.setText("60");
            playersField.setText("1");

            invoke("onStartBattle");
            invoke("onChangeMineDensityWarning");

            assertFalse(mineDensityWarningOverlay.isVisible());
            assertTrue(selectedModeLabel.getText().contains("Hãy điều chỉnh lại số mìn"));
        });
    }

    @Test
    void UC02_CT11_shouldShowErrorWhenPlayerCountInvalid() throws Throwable {
        runTestCase("UC02-CT11", "Số người chơi không hợp lệ thì hiển thị lỗi và không chuyển màn hình", () -> {
            customButton.setSelected(false);
            easyButton.setSelected(true);
            playersField.setText("5");

            invoke("onStartBattle");

            assertTrue(selectedModeLabel.getText().contains("Cấu hình số người chơi chưa hợp lệ"));
        });
    }

    @Test
    void UC02_CT12_shouldShowPlayerNamePopupWhenPlayerCountGreaterThanOne() throws Throwable {
        runTestCase("UC02-CT12", "Số người chơi lớn hơn 1 thì hiển thị popup nhập tên người chơi", () -> {
            playersField.setText("3");

            invoke("showPlayerNamePopup", 3, null, null);

            assertTrue(playerNameOverlay.isVisible());

            assertTrue(playerRow1.isVisible());
            assertTrue(playerRow2.isVisible());
            assertTrue(playerRow3.isVisible());
            assertFalse(playerRow4.isVisible());

            assertEquals("Player 02", playerNameField2.getText());
            assertEquals("Player 03", playerNameField3.getText());
        });
    }

    @Test
    void UC02_CT15_shouldClosePopupWhenCancelPlayerNamePopup() throws Throwable {
        runTestCase("UC02-CT15", "Nhấn Hủy popup nhập tên thì đóng popup và kết thúc use case", () -> {
            invoke("showPlayerNamePopup", 2, null, null);

            assertTrue(playerNameOverlay.isVisible());

            invoke("onCancelPlayerNamePopup");

            assertFalse(playerNameOverlay.isVisible());
        });
    }

    private void setField(String name, Object value) throws Exception {
        Field field = DashBoardController.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(controller, value);
    }

    private Object invoke(String name, Object... args) throws Exception {
        Method method = null;

        for (Method m : DashBoardController.class.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == args.length) {
                method = m;
                break;
            }
        }

        if (method == null) {
            throw new NoSuchMethodException(name);
        }

        method.setAccessible(true);

        try {
            return method.invoke(controller, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();

            if (cause instanceof Exception) {
                throw (Exception) cause;
            }

            throw new RuntimeException(cause);
        }
    }
}
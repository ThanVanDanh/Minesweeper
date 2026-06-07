package minesweeper.TestUC02;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.service.query.EmptyNodeQueryException;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.*;

public class UC02SystemFlowTest extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/app/dashboard.fxml"));
        stage.setScene(new Scene(root, 1280, 760));
        stage.show();
        stage.toFront();
    }

    private void printPass(String testId, String testName) {
        System.out.println("[PASS] " + testId + " - " + testName);
    }

    private void printFail(String testId, String testName, Throwable e) {
        System.out.println("[FAIL] " + testId + " - " + testName);
        System.out.println("       Lỗi: " + e.getMessage());
    }

    @Test
    void UC02_ST01_dashboardShouldShowDifficultyButtons() {
        String testId = "UC02-ST01";
        String testName = "Dashboard hiển thị danh sách chế độ chơi";

        try {
            WaitForAsyncUtils.waitForFxEvents();

            assertNotNull(lookup("#easyButton").query());
            assertNotNull(lookup("#mediumButton").query());
            assertNotNull(lookup("#hardButton").query());
            assertNotNull(lookup("#expertButton").query());
            assertNotNull(lookup("#customButton").query());

            printPass(testId, testName);
        } catch (Throwable e) {
            printFail(testId, testName, e);
            throw e;
        }
    }

    @Test
    void UC02_ST02_dashboardShouldSelectEasyByDefaultAndLockCustomInputs() {
        String testId = "UC02-ST02";
        String testName = "Dashboard mặc định chọn Dễ và khóa ô hàng, cột, mìn";

        try {
            WaitForAsyncUtils.waitForFxEvents();

            ToggleButton easyButton = lookup("#easyButton").queryAs(ToggleButton.class);
            TextField rowsField = lookup("#customRowsField").queryAs(TextField.class);
            TextField colsField = lookup("#customColsField").queryAs(TextField.class);
            TextField minesField = lookup("#customMinesField").queryAs(TextField.class);

            assertTrue(easyButton.isSelected());
            assertTrue(rowsField.isDisabled());
            assertTrue(colsField.isDisabled());
            assertTrue(minesField.isDisabled());

            printPass(testId, testName);
        } catch (Throwable e) {
            printFail(testId, testName, e);
            throw e;
        }
    }

    @Test
    void UC02_ST03_customModeShouldEnableCustomInputs() {
        String testId = "UC02-ST03";
        String testName = "Chọn Tùy chỉnh thì mở khóa ô hàng, cột, mìn";

        try {
            WaitForAsyncUtils.waitForFxEvents();

            clickOn("#customButton");
            WaitForAsyncUtils.waitForFxEvents();

            TextField rowsField = lookup("#customRowsField").queryAs(TextField.class);
            TextField colsField = lookup("#customColsField").queryAs(TextField.class);
            TextField minesField = lookup("#customMinesField").queryAs(TextField.class);

            assertFalse(rowsField.isDisabled());
            assertFalse(colsField.isDisabled());
            assertFalse(minesField.isDisabled());

            printPass(testId, testName);
        } catch (Throwable e) {
            printFail(testId, testName, e);
            throw e;
        }
    }

    @Test
    void UC02_ST04_customValidConfigShouldGoToBoardGameScreen() {
        String testId = "UC02-ST04";
        String testName = "Cấu hình tùy chỉnh hợp lệ thì chuyển sang màn hình chơi game";

        try {
            WaitForAsyncUtils.waitForFxEvents();

            clickOn("#customButton");
            WaitForAsyncUtils.waitForFxEvents();

            eraseAndWrite("#customRowsField", "10");
            eraseAndWrite("#customColsField", "10");
            eraseAndWrite("#customMinesField", "20");
            eraseAndWrite("#customPlayersField", "1");

            clickStartBattleButton();
            WaitForAsyncUtils.waitForFxEvents();

            assertNotNull(lookup("#minesweeperGrid").query());

            printPass(testId, testName);
        } catch (Throwable e) {
            printFail(testId, testName, e);
            throw e;
        }
    }

    @Test
    void UC02_ST05_invalidCustomConfigShouldStayAtDashboardAndShowError() {
        String testId = "UC02-ST05";
        String testName = "Cấu hình tùy chỉnh không hợp lệ thì báo lỗi và không chuyển màn hình";

        try {
            WaitForAsyncUtils.waitForFxEvents();

            clickOn("#customButton");
            WaitForAsyncUtils.waitForFxEvents();

            eraseAndWrite("#customRowsField", "1");
            eraseAndWrite("#customColsField", "10");
            eraseAndWrite("#customMinesField", "20");
            eraseAndWrite("#customPlayersField", "1");

            clickStartBattleButton();
            WaitForAsyncUtils.waitForFxEvents();

            String labelText = lookup("#selectedModeLabel")
                    .queryAs(javafx.scene.control.Label.class)
                    .getText();

            assertTrue(labelText.contains("Cấu hình tùy chỉnh chưa hợp lệ"));
            assertThrows(EmptyNodeQueryException.class, () -> lookup("#minesweeperGrid").query());
            printPass(testId, testName);
        } catch (Throwable e) {
            printFail(testId, testName, e);
            throw e;
        }
    }

    @Test
    void UC02_ST06_highMineDensityShouldShowWarning() {
        String testId = "UC02-ST06";
        String testName = "Mật độ mìn quá cao thì hiển thị cảnh báo";

        try {
            WaitForAsyncUtils.waitForFxEvents();

            clickOn("#customButton");
            WaitForAsyncUtils.waitForFxEvents();

            eraseAndWrite("#customRowsField", "10");
            eraseAndWrite("#customColsField", "10");
            eraseAndWrite("#customMinesField", "60");
            eraseAndWrite("#customPlayersField", "1");

            clickStartBattleButton();
            WaitForAsyncUtils.waitForFxEvents();

            assertTrue(lookup("#mineDensityWarningOverlay").query().isVisible());

            String warningText = lookup("#mineDensityWarningMessage")
                    .queryAs(javafx.scene.control.Label.class)
                    .getText();

            assertTrue(warningText.contains("Số lượng mìn quá dày đặc"));
            assertTrue(warningText.contains("60.0%"));

            printPass(testId, testName);
        } catch (Throwable e) {
            printFail(testId, testName, e);
            throw e;
        }
    }

    @Test
    void UC02_ST07_chooseChangeInMineWarningShouldStayAtDashboard() {
        String testId = "UC02-ST07";
        String testName = "Chọn Thay đổi trong cảnh báo mật độ mìn thì hủy tạo ván đấu";

        try {
            WaitForAsyncUtils.waitForFxEvents();

            clickOn("#customButton");
            WaitForAsyncUtils.waitForFxEvents();

            eraseAndWrite("#customRowsField", "10");
            eraseAndWrite("#customColsField", "10");
            eraseAndWrite("#customMinesField", "60");
            eraseAndWrite("#customPlayersField", "1");

            clickStartBattleButton();
            WaitForAsyncUtils.waitForFxEvents();

            assertTrue(lookup("#mineDensityWarningOverlay").query().isVisible());

            clickOn("THAY ĐỔI");
            WaitForAsyncUtils.waitForFxEvents();

            assertFalse(lookup("#mineDensityWarningOverlay").query().isVisible());

            String labelText = lookup("#selectedModeLabel")
                    .queryAs(javafx.scene.control.Label.class)
                    .getText();

            assertTrue(labelText.contains("Hãy điều chỉnh lại số mìn"));

            printPass(testId, testName);
        } catch (Throwable e) {
            printFail(testId, testName, e);
            throw e;
        }
    }

    @Test
    void UC02_ST08_invalidPlayerCountShouldShowErrorAndNotGoToGameScreen() {
        String testId = "UC02-ST08";
        String testName = "Số người chơi không hợp lệ thì báo lỗi và không chuyển màn hình";

        try {
            WaitForAsyncUtils.waitForFxEvents();

            eraseAndWrite("#customPlayersField", "5");

            clickStartBattleButton();
            WaitForAsyncUtils.waitForFxEvents();

            String labelText = lookup("#selectedModeLabel")
                    .queryAs(javafx.scene.control.Label.class)
                    .getText();

            assertTrue(labelText.contains("Cấu hình số người chơi chưa hợp lệ"));
            assertThrows(EmptyNodeQueryException.class, () -> lookup("#minesweeperGrid").query());
            printPass(testId, testName);
        } catch (Throwable e) {
            printFail(testId, testName, e);
            throw e;
        }
    }

    @Test
    void UC02_ST09_multiplePlayersShouldShowPlayerNamePopup() {
        String testId = "UC02-ST09";
        String testName = "Số người chơi từ 2 đến 4 thì hiển thị popup nhập tên";

        try {
            WaitForAsyncUtils.waitForFxEvents();

            eraseAndWrite("#customPlayersField", "3");

            clickStartBattleButton();
            WaitForAsyncUtils.waitForFxEvents();

            assertTrue(lookup("#playerNameOverlay").query().isVisible());
            assertTrue(lookup("#playerRow1").query().isVisible());
            assertTrue(lookup("#playerRow2").query().isVisible());
            assertTrue(lookup("#playerRow3").query().isVisible());
            assertFalse(lookup("#playerRow4").query().isVisible());

            TextField player2 = lookup("#playerNameField2").queryAs(TextField.class);
            TextField player3 = lookup("#playerNameField3").queryAs(TextField.class);

            assertEquals("Player 02", player2.getText());
            assertEquals("Player 03", player3.getText());

            printPass(testId, testName);
        } catch (Throwable e) {
            printFail(testId, testName, e);
            throw e;
        }
    }

    @Test
    void UC02_ST10_cancelPlayerNamePopupShouldClosePopup() {
        String testId = "UC02-ST10";
        String testName = "Nhấn Hủy popup nhập tên thì đóng popup và kết thúc use case";

        try {
            WaitForAsyncUtils.waitForFxEvents();

            eraseAndWrite("#customPlayersField", "2");

            clickStartBattleButton();
            WaitForAsyncUtils.waitForFxEvents();

            assertTrue(lookup("#playerNameOverlay").query().isVisible());

            clickOn("#cancelPlayerNameButton");
            WaitForAsyncUtils.waitForFxEvents();

            assertFalse(lookup("#playerNameOverlay").query().isVisible());

            printPass(testId, testName);
        } catch (Throwable e) {
            printFail(testId, testName, e);
            throw e;
        }
    }

    private void clickStartBattleButton() {
        /*
         * Trong dashboard.fxml hiện tại, nút bắt đầu không có fx:id.
         * Vì vậy click theo text thật của Button:
         *
         * <Button text="▷ BẮT ĐẦU CHIẾN ĐẤU"
         *         onAction="#onStartBattle"
         *         styleClass="start-button"/>
         */
        clickOn("▷ BẮT ĐẦU CHIẾN ĐẤU");
    }

    private void eraseAndWrite(String selector, String value) {
        clickOn(selector);

        press(KeyCode.SHORTCUT).press(KeyCode.A);
        release(KeyCode.A).release(KeyCode.SHORTCUT);

        write(value);
    }
}
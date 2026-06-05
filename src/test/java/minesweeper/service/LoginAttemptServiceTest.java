package minesweeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    private LoginAttemptService attemptService;
    private final String TEST_USER = "test_sapper";

    @BeforeEach
    void setUp() {
        attemptService = new LoginAttemptService();
        // Reset bộ đếm trước mỗi test case
        attemptService.loginSucceeded(TEST_USER);
    }

    @Test
    void whenLoginFailsLessThanFiveTimes_thenAccountIsNotLocked() {
        // Giả lập đăng nhập sai 4 lần
        for (int i = 0; i < 4; i++) {
            attemptService.loginFailed(TEST_USER);
            assertFalse(attemptService.isLocked(TEST_USER), "Tài khoản không được khóa ở lần sai thứ " + (i + 1));
        }
    }

    @Test
    void whenLoginFailsFiveTimes_thenAccountIsLocked() {
        // Giả lập đăng nhập sai 5 lần
        for (int i = 0; i < 5; i++) {
            attemptService.loginFailed(TEST_USER);
        }

        assertTrue(attemptService.isLocked(TEST_USER), "Tài khoản phải bị khóa sau 5 lần nhập sai liên tiếp");
        assertTrue(attemptService.getRemainingLockTimeMinutes(TEST_USER) > 0, "Thời gian khóa còn lại phải lớn hơn 0");
    }

    @Test
    void whenLoginSucceeds_thenFailedAttemptsAreReset() {
        // Đăng nhập sai 3 lần
        attemptService.loginFailed(TEST_USER);
        attemptService.loginFailed(TEST_USER);
        attemptService.loginFailed(TEST_USER);

        // Đăng nhập đúng thành công
        attemptService.loginSucceeded(TEST_USER);
        assertFalse(attemptService.isLocked(TEST_USER), "Tài khoản không được khóa");

        // Nhập sai thêm 2 lần (nếu không reset thì cộng dồn 3+2=5 và bị khóa)
        attemptService.loginFailed(TEST_USER);
        attemptService.loginFailed(TEST_USER);

        assertFalse(attemptService.isLocked(TEST_USER), "Tài khoản không bị khóa vì bộ đếm đã được reset về 0");
    }
}

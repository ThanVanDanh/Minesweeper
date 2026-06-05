package minesweeper.service;

import minesweeper.model.User;
import minesweeper.model.enums.Role;
import minesweeper.repository.EmailVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private MySqlUserService mockUserService;
    private EmailVerificationRepository mockEmailVerifRepo;
    private EmailService mockEmailService;
    private RememberMeService mockRememberMeService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        mockUserService = mock(MySqlUserService.class);
        mockEmailVerifRepo = mock(EmailVerificationRepository.class);
        mockEmailService = mock(EmailService.class);
        mockRememberMeService = mock(RememberMeService.class);

        authService = new AuthService(
                mockUserService,
                mockEmailVerifRepo,
                mockEmailService,
                mockRememberMeService
        );
    }

    // =========================================================================
    // 1. Kiểm thử định dạng Username
    // =========================================================================

    @Test
    void register_withValidUsername_shouldNotThrowValidationException() throws Exception {
        String username = "valid_user_123";
        
        // Cấu hình mock để không bị lỗi trùng lặp khi chạy qua bước validate
        when(mockUserService.getUserByUsername(username)).thenReturn(null);
        when(mockUserService.getUserByEmail(anyString())).thenReturn(null);

        // Chúng ta mong đợi code chạy qua validate và gọi đến check trùng, ném lỗi NPE hoặc NullPointer ở các bước sau 
        // hoặc bắt lỗi do mock chưa cấu hình hết. Nhưng quan trọng là KHÔNG ném validate error về định dạng.
        try {
            authService.register(username, "Display", "test@example.com", "password123");
        } catch (NullPointerException e) {
            // Đạt yêu cầu: Không ném IllegalArgumentException về định dạng username
        } catch (IllegalArgumentException e) {
            // Không được có thông báo lỗi định dạng ký tự
            assertFalse(e.getMessage().contains("chỉ được chứa chữ cái"));
        }
    }

    @Test
    void register_withUsernameContainingSpecialChar_shouldThrowException() {
        String invalidUsername = "neo@sapper";
        
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(invalidUsername, "Display", "test@example.com", "password123")
        );
        assertTrue(exception.getMessage().contains("chỉ được chứa chữ cái, chữ số và dấu gạch dưới"));
    }

    @Test
    void register_withUsernameContainingSpace_shouldThrowException() {
        String invalidUsername = "neo sapper";
        
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(invalidUsername, "Display", "test@example.com", "password123")
        );
        assertTrue(exception.getMessage().contains("không được chứa khoảng trắng"));
    }

    // =========================================================================
    // 2. Kiểm thử mã hóa BCrypt
    // =========================================================================

    @Test
    void register_shouldHashPasswordWithBCrypt() throws Exception {
        String username = "new_sapper";
        String rawPassword = "mypassword123";

        when(mockUserService.getUserByUsername(username)).thenReturn(null);
        when(mockUserService.getUserByEmail(anyString())).thenReturn(null);

        // Tạo một User giả để trả về sau khi tạo
        User dummyUser = new User();
        dummyUser.setId(999);
        dummyUser.setUsername(username);
        dummyUser.setEmail("new@example.com");
        when(mockUserService.getAuthUserByUsername(username)).thenReturn(dummyUser);

        authService.register(username, "New Sapper", "new@example.com", rawPassword);

        // Capture mật khẩu đã được truyền vào createUserWithEmail
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockUserService).createUserWithEmail(
                eq(username),
                anyString(),
                anyString(),
                passwordCaptor.capture(),
                eq(Role.PLAYER),
                eq(false)
        );

        String hashedPassword = passwordCaptor.getValue();
        assertNotNull(hashedPassword);
        // BCrypt hash luôn bắt đầu bằng "$2a$" hoặc "$2b$" hoặc "$2y$"
        assertTrue(hashedPassword.startsWith("$2a$") || hashedPassword.startsWith("$2b$") || hashedPassword.startsWith("$2y$"),
                "Mật khẩu lưu trong DB phải ở định dạng BCrypt hash (bắt đầu bằng $2)");
    }

    // =========================================================================
    // 3. Kiểm thử Rate Limiting
    // =========================================================================

    @Test
    void login_whenAccountIsLockedAfterFiveFailures_shouldThrowException() throws Exception {
        String username = "brute_force_user";
        User mockUser = new User();
        mockUser.setId(101);
        mockUser.setUsername(username);
        mockUser.setActive(true);
        mockUser.setPasswordHash("$2a$12$DummyHashForTestCodeMustMatchBCryptStructure"); // BCrypt hash giả hợp lệ

        // Mock trả về user tồn tại
        when(mockUserService.getAuthUserByUsername(username)).thenReturn(mockUser);

        // Đăng nhập sai 5 lần để kích hoạt khóa
        for (int i = 0; i < 5; i++) {
            try {
                authService.login(username, "wrong_password", false);
            } catch (IllegalArgumentException e) {
                // Mong đợi báo sai mật khẩu
                assertEquals("Sai tên đăng nhập hoặc mật khẩu.", e.getMessage());
            }
        }

        // Lần thứ 6: Đăng nhập tiếp -> Phải bị chặn ngay lập tức do Rate Limit
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(username, "any_password", false)
        );
        assertTrue(exception.getMessage().contains("Tài khoản đã bị tạm khóa"),
                "Thông báo phải thông báo tài khoản bị khóa tạm thời");
    }
}

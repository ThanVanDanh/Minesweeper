package minesweeper.service;

import jakarta.mail.MessagingException;
import minesweeper.model.EmailVerificationToken;
import minesweeper.model.User;
import minesweeper.model.enums.Role;
import minesweeper.repository.EmailVerificationRepository;
import minesweeper.repository.MySqlEmailVerificationRepository;
import minesweeper.repository.exception.DataAccessException;
import utils.CryptUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * AuthService - Lớp xử lý nghiệp vụ xác thực và quản lý phiên đăng nhập.
 * Đảm nhiệm các use case UC01: Đăng ký/Đăng nhập.
 *
 * Trách nhiệm:
 * - Đăng ký tài khoản mới (UC01 - AF01.1) + gửi OTP xác nhận email
 * - Đăng nhập (UC01 - AF01.2) + tùy chọn remember-me
 * - Đăng xuất (UC01 - AF01.3)
 * - Đổi mật khẩu (UC01 - AF01.4)
 * - Validate dữ liệu đầu vào, mã hóa và xác minh mật khẩu
 */
public class AuthService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRE_MINS = 10;

    private final MySqlUserService userService;
    private final EmailVerificationRepository emailVerifRepo;
    private final EmailService emailService;
    private final RememberMeService rememberMeService;
    private final LoginAttemptService loginAttemptService = new LoginAttemptService();

    public AuthService() {
        this(new MySqlUserService(),
                new MySqlEmailVerificationRepository(),
                new EmailService(),
                new RememberMeService());
    }

    public AuthService(MySqlUserService userService,
                       EmailVerificationRepository emailVerifRepo,
                       EmailService emailService,
                       RememberMeService rememberMeService) {
        this.userService = userService;
        this.emailVerifRepo = emailVerifRepo;
        this.emailService = emailService;
        this.rememberMeService = rememberMeService;
    }

    /**
     * UC01.1 - Đăng ký tài khoản
     */
    public User register(String username, String displayName, String email, String password)
            throws DataAccessException, MessagingException {
        // Basic flow 01.1.2 Hệ thống kiểm tra dữ liệu: username không rỗng, >= 3 ký tự, chỉ [a-zA-Z0-9_].
        validateUsername(username);

        // Basic flow 01.1.3 Hệ thống kiểm tra mật khẩu >= 6 ký tự (phần check length).
        validatePassword(password);

        validateEmail(email);

        // Basic flow 01.1.4 Hệ thống kiểm tra username và email chưa tồn tại trong DB.
        if (userService.getUserByUsername(username) != null) {
            // Exception flow E01.2 Username đã tồn tại.
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }

        if (userService.getUserByEmail(email) != null) {
            // Exception flow E01.3 Email đã được sử dụng.
            throw new IllegalArgumentException("Email này đã được sử dụng.");
        }

        // Basic flow 01.1.5 Hệ thống mã hóa mật khẩu bằng BCrypt (cost 12).
        String passwordHash = CryptUtils.hashPassword(password);

        // Basic flow 01.1.6 Hệ thống lưu user mới (is_active = false, role = PLAYER).
        userService.createUserWithEmail(username,
                displayName != null && !displayName.isBlank() ? displayName : username,
                email,
                passwordHash,
                Role.PLAYER,
                false);

        User newUser = userService.getAuthUserByUsername(username);

        try {
            sendOtp(newUser);
        } catch (Exception e) {
            // Nếu gửi OTP thất bại, rollback user vừa tạo để tránh user "mồ côi".
            try {
                userService.deleteUser(newUser.getId());
            } catch (Exception ignored) {
            }
            if (e instanceof MessagingException me) throw me;
            if (e instanceof DataAccessException dae) throw dae;
            throw new DataAccessException("Failed to send OTP", e);
        }

        return newUser;
    }

    public void verifyEmail(long userId, String otp) throws DataAccessException {
        if (otp == null || otp.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mã OTP.");
        }

        EmailVerificationToken token = emailVerifRepo.findActiveByUserId(userId);
        if (token == null) {
            throw new IllegalArgumentException("Mã OTP không còn hiệu lực. Vui lòng yêu cầu gửi lại.");
        }
        if (!token.isValid()) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại.");
        }

        if (!CryptUtils.verifyPassword(otp.trim(), token.getOtp())) {
            throw new IllegalArgumentException("Mã OTP không đúng.");
        }

        userService.setActive(userId, true);
        emailVerifRepo.markUsed(token.getId());
        emailVerifRepo.deleteByUserId(userId);
    }

    public void resendOtp(long userId) throws DataAccessException, MessagingException {
        User user = userService.getAuthUserByUsername(
                userService.getUserById(userId).getUsername());
        if (user == null) {
            throw new IllegalArgumentException("Tài khoản không tồn tại.");
        }
        if (user.isActive()) {
            throw new IllegalArgumentException("Tài khoản đã được xác nhận.");
        }
        sendOtp(user);
    }

    /**
     * UC01.2 - Đăng nhập
     */
    public User login(String username, String password, boolean rememberMe) throws DataAccessException {
        // Basic flow 01.2.2 Hệ thống kiểm tra username không rỗng, >= 3 ký tự, không khoảng trắng.
        validateUsername(username);

        // Basic flow 01.2.3 Hệ thống kiểm tra tài khoản không đang bị Rate Limit.
        if (loginAttemptService.isLocked(username)) {
            // Exception flow E01.7 Rate Limit: bị khóa tạm thời 5 phút.
            long remaining = loginAttemptService.getRemainingLockTimeMinutes(username);
            throw new IllegalArgumentException("Tài khoản đã bị tạm khóa do nhập sai mật khẩu quá 5 lần. Vui lòng thử lại sau " + remaining + " phút.");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu.");
        }

        // Basic flow 01.2.4 Hệ thống truy vấn user theo username.
        User user = userService.getAuthUserByUsername(username);
        if (user == null) {
            // Exception flow E01.3 Sai tài khoản hoặc mật khẩu.
            throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu.");
        }

        // Basic flow 01.2.5 Hệ thống kiểm tra is_active của tài khoản.
        if (!user.isActive()) {
            boolean hasPendingOtp = emailVerifRepo.findActiveByUserId(user.getId()) != null;
            if (hasPendingOtp) {
                // Exception flow E01.4a Tài khoản chưa xác nhận email.
                throw new IllegalArgumentException("Tài khoản chưa xác nhận email. Vui lòng kiểm tra hộp thư.");
            }
            // Exception flow E01.4b Tài khoản bị Admin khóa.
            throw new IllegalArgumentException("Tài khoản đã bị khóa. Vui lòng liên hệ admin.");
        }

        // Basic flow 01.2.6 Hệ thống xác minh mật khẩu bằng BCrypt.checkpw().
        if (!CryptUtils.verifyPassword(password, user.getPasswordHash())) {
            // Exception flow E01.3 Sai tài khoản hoặc mật khẩu → tăng bộ đếm sai.
            loginAttemptService.loginFailed(username);
            throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu.");
        }

        // Basic flow 01.2.7 Hệ thống reset bộ đếm sai mật khẩu.
        loginAttemptService.loginSucceeded(username);

        // Basic flow 01.2.8 Hệ thống cập nhật last_login_at.
        userService.updateLastLogin(user.getId());

        // Basic flow 01.2.9 SessionManager.createSession(user).
        SessionManager.createSession(user);

        // Basic flow 01.2.10 Nếu rememberMe: RememberMeService.save(user).
        // (Và Alternative flow 01.2-A1 Remember Me: hệ thống lưu token để tự đăng nhập)
        if (rememberMe) {
            rememberMeService.save(user);
        } else {
            rememberMeService.clear();
        }

        // 1.2.10 Trả về User cho Controller để cập nhật giao diện.
        return user;
    }

    public User login(String username, String password) throws DataAccessException {
        return login(username, password, false);
    }

    /**
     * UC01.3 - Đăng xuất
     */
    public void logout() {
        // Basic flow 01.3.2 Hệ thống gọi RememberMeService.clear() xóa token.
        rememberMeService.clear();
        // Basic flow 01.3.3 Hệ thống gọi SessionManager.clearSession().
        SessionManager.clearSession();
    }

    /**
     * UC01.4 - Đổi mật khẩu
     */
    public void changePassword(long userId, String currentPassword, String newPassword) throws DataAccessException {
        // Basic flow 01.4.2 Hệ thống kiểm tra các trường không rỗng.
        if (currentPassword == null || currentPassword.isBlank()) {
            // Exception flow E01.1 Thiếu thông tin mật khẩu.
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại.");
        }

        // Basic flow 01.4.3 Hệ thống kiểm tra mật khẩu mới >= 6 ký tự.
        validatePassword(newPassword);

        // Basic flow 01.4.4 Hệ thống lấy password_hash hiện tại từ DB.
        String storedHash = userService.getPasswordHashById(userId);

        // Basic flow 01.4.5 Hệ thống xác minh bằng CryptUtils.verifyPassword() (BCrypt).
        if (storedHash == null || !CryptUtils.verifyPassword(currentPassword, storedHash)) {
            // Exception flow E01.8 Mật khẩu hiện tại sai.
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        }

        // Basic flow 01.4.6 Hệ thống mã hóa mật khẩu mới bằng CryptUtils.hashPassword() (BCrypt).
        String newHash = CryptUtils.hashPassword(newPassword);

        // Basic flow 01.4.7 Hệ thống gọi updatePasswordHash(userId, newHash).
        userService.updatePasswordHash(userId, newHash);
    }

    public User tryAutoLogin() {
        return rememberMeService.tryAutoLogin();
    }

    private void sendOtp(User user) throws DataAccessException, MessagingException {
        // Basic flow 01.1.7 Hệ thống tạo OTP 6 chữ số, mã hóa Bcrypt, lưu vào bảng email_verification_tokens (hết hạn 10 phút).
        String plainOtp = generateOtp();
        String hashedOtp = CryptUtils.hashPassword(plainOtp);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(user.getId());
        token.setEmail(user.getEmail());
        token.setOtp(hashedOtp);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRE_MINS));
        emailVerifRepo.save(token);

        // Basic flow 01.1.8 Hệ thống gửi email chứa OTP tới địa chỉ vừa nhập.
        // Nếu lỗi xảy ra, catch ở register() sẽ rollback xóa user theo Alternative flow 01.1-A2.
        emailService.sendOtpEmail(user.getEmail(), plainOtp);
    }

    private String generateOtp() {
        // OTP_LENGTH hiện được đảm bảo bằng format 6 chữ số.
        SecureRandom rng = new SecureRandom();
        int code = 100_000 + rng.nextInt(900_000);
        return String.valueOf(code);
    }

    /**
     * Validate username: không rỗng, >= 3 ký tự, không chứa khoảng trắng.
     * Sử dụng trong: 1.1.3, 1.2.3.
     */
    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập tên đăng nhập.");
        }
        if (username.contains(" ")) {
            throw new IllegalArgumentException("Tên đăng nhập không được chứa khoảng trắng.");
        }
        if (username.trim().length() < 3) {
            throw new IllegalArgumentException("Tên đăng nhập cần ít nhất 3 ký tự.");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Tên đăng nhập chỉ được chứa chữ cái, chữ số và dấu gạch dưới (_).");
        }
    }

    /**
     * Validate password: không rỗng, >= 6 ký tự.
     * Sử dụng trong: 1.1.4, 1.4.4.
     */
    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu.");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu cần ít nhất 6 ký tự.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập email.");
        }
        if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Địa chỉ email không hợp lệ.");
        }
    }
}

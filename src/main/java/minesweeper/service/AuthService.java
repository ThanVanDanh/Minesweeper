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
     * UC01 - AF01.1: Đăng ký tài khoản (phía Service).
     * Luồng hiện tại: tạo user (inactive) + phát OTP qua email để xác nhận.
     */
    public User register(String username, String displayName, String email, String password)
            throws DataAccessException, MessagingException {
        // 1.1.3 validateUsername(username): kiểm tra username không rỗng, >= 3 ký tự, không chứa khoảng trắng.
        validateUsername(username);

        // 1.1.4 validatePassword(password): kiểm tra mật khẩu không rỗng và >= 6 ký tự.
        validatePassword(password);

        validateEmail(email);

        // 1.1.5 getUserByUsername(username): kiểm tra username đã tồn tại chưa.
        if (userService.getUserByUsername(username) != null) {
            // 1.1.E1 Username đã tồn tại.
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }

        if (userService.getUserByEmail(email) != null) {
            throw new IllegalArgumentException("Email này đã được sử dụng.");
        }

        // 1.1.6 CryptUtils.md5(password): mã hóa mật khẩu trước khi lưu.
        String passwordHash = CryptUtils.md5(password);

        // Lưu user mới (inactive) và chờ xác nhận OTP.
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

        if (!CryptUtils.matchesMd5(otp.trim(), token.getOtp())) {
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
     * UC01 - AF01.2: Đăng nhập (phía Service).
     * Đảm nhiệm: 1.2.3..1.2.9 (+ remember-me nếu được chọn).
     */
    public User login(String username, String password, boolean rememberMe) throws DataAccessException {
        // 1.2.3 validateUsername(username): kiểm tra username không rỗng, >= 3 ký tự, không chứa khoảng trắng.
        validateUsername(username);

        // Kiểm tra xem tài khoản có đang bị khóa tạm thời do nhập sai quá nhiều lần không
        if (loginAttemptService.isLocked(username)) {
            long remaining = loginAttemptService.getRemainingLockTimeMinutes(username);
            throw new IllegalArgumentException("Tài khoản đã bị tạm khóa do nhập sai mật khẩu quá 5 lần. Vui lòng thử lại sau " + remaining + " phút.");
        }

        // 1.2.4 Kiểm tra password không rỗng.
        if (password == null || password.isBlank()) {
            // 1.2.E1 Người dùng chưa nhập mật khẩu.
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu.");
        }

        // 1.2.5 getAuthUserByUsername(username): truy vấn tài khoản theo username.
        User user = userService.getAuthUserByUsername(username);
        if (user == null) {
            // 1.2.E2 Username không tồn tại.
            throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu.");
        }

        // 1.2.6 isActive(): kiểm tra trạng thái tài khoản.
        if (!user.isActive()) {
            boolean hasPendingOtp = emailVerifRepo.findActiveByUserId(user.getId()) != null;
            if (hasPendingOtp) {
                throw new IllegalArgumentException("Tài khoản chưa xác nhận email. Vui lòng kiểm tra hộp thư.");
            }
            throw new IllegalArgumentException("Tài khoản đã bị khóa. Vui lòng liên hệ admin.");
        }

        // 1.2.7 CryptUtils.matchesMd5(password, passwordHash): xác minh mật khẩu.
        if (!CryptUtils.matchesMd5(password, user.getPasswordHash())) {
            // Tăng số lần đăng nhập sai
            loginAttemptService.loginFailed(username);
            // 1.2.E4 Mật khẩu không khớp.
            throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu.");
        }

        // Đăng nhập thành công -> Reset bộ đếm lần sai
        loginAttemptService.loginSucceeded(username);

        // 1.2.8 updateLastLogin(userId): cập nhật thời gian đăng nhập cuối.
        userService.updateLastLogin(user.getId());

        // 1.2.9 SessionManager.createSession(user): tạo phiên đăng nhập.
        SessionManager.createSession(user);

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
     * UC01 - AF01.3: Đăng xuất (phía Service).
     */
    public void logout() {
        rememberMeService.clear();
        // 1.3.2 SessionManager.clearSession(): xóa phiên đăng nhập hiện tại.
        SessionManager.clearSession();
    }

    /**
     * UC01 - AF01.4: Đổi mật khẩu (phía Service).
     * Đảm nhiệm: 1.4.3..1.4.8.
     */
    public void changePassword(long userId, String currentPassword, String newPassword) throws DataAccessException {
        // 1.4.3 Kiểm tra mật khẩu hiện tại không rỗng.
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại.");
        }

        // 1.4.4 validatePassword(newPassword): kiểm tra mật khẩu mới không rỗng và >= 6 ký tự.
        validatePassword(newPassword);

        // 1.4.5 getPasswordHashById(userId): lấy password_hash hiện tại từ DB.
        String storedHash = userService.getPasswordHashById(userId);

        // 1.4.6 CryptUtils.matchesMd5(currentPassword, storedHash): xác minh mật khẩu hiện tại.
        if (storedHash == null || !CryptUtils.matchesMd5(currentPassword, storedHash)) {
            // 1.4.E1 Mật khẩu hiện tại không đúng hoặc không tìm thấy hash trong DB.
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        }

        // 1.4.7 CryptUtils.md5(newPassword): mã hóa mật khẩu mới.
        String newHash = CryptUtils.md5(newPassword);

        // 1.4.8 updatePasswordHash(userId, newHash): cập nhật mật khẩu mới vào DB.
        userService.updatePasswordHash(userId, newHash);
    }

    public User tryAutoLogin() {
        return rememberMeService.tryAutoLogin();
    }

    private void sendOtp(User user) throws DataAccessException, MessagingException {
        String plainOtp = generateOtp();
        String hashedOtp = CryptUtils.md5(plainOtp);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(user.getId());
        token.setEmail(user.getEmail());
        token.setOtp(hashedOtp);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRE_MINS));
        emailVerifRepo.save(token);

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

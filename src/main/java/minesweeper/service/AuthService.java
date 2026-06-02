package minesweeper.service;

import minesweeper.model.EmailVerificationToken;
import minesweeper.model.enums.Role;
import minesweeper.model.User;
import minesweeper.repository.EmailVerificationRepository;
import minesweeper.repository.MySqlEmailVerificationRepository;
import minesweeper.repository.exception.DataAccessException;
import utils.CryptUtils;

import jakarta.mail.MessagingException;
import java.security.SecureRandom;
import java.time.LocalDateTime;

public class AuthService {

    private static final int OTP_LENGTH      = 6;
    private static final int OTP_EXPIRE_MINS = 10;

    private final MySqlUserService            userService;
    private final EmailVerificationRepository emailVerifRepo;
    private final EmailService                emailService;
    private final RememberMeService           rememberMeService;

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
        this.userService       = userService;
        this.emailVerifRepo    = emailVerifRepo;
        this.emailService      = emailService;
        this.rememberMeService = rememberMeService;
    }
    public User register(String username, String displayName, String email, String password)
            throws DataAccessException, MessagingException {
        validateUsername(username);
        validatePassword(password);
        validateEmail(email);
        if (userService.getUserByUsername(username) != null) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }
        if (userService.getUserByEmail(email) != null) {
            throw new IllegalArgumentException("Email này đã được sử dụng.");
        }
        String passwordHash = CryptUtils.md5(password);
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
            try { userService.deleteUser(newUser.getId()); } catch (Exception ignored) {}
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

    public User login(String username, String password, boolean rememberMe)
            throws DataAccessException {

        validateUsername(username);
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu.");
        }

        User user = userService.getAuthUserByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu.");
        }
        if (!user.isActive()) {
            boolean hasPendingOtp = emailVerifRepo.findActiveByUserId(user.getId()) != null;
            if (hasPendingOtp) {
                throw new IllegalArgumentException("Tài khoản chưa xác nhận email. Vui lòng kiểm tra hộp thư.");
            }
            throw new IllegalArgumentException("Tài khoản đã bị khóa. Vui lòng liên hệ admin.");
        }
        if (!CryptUtils.matchesMd5(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu.");
        }

        userService.updateLastLogin(user.getId());
        SessionManager.createSession(user);

        if (rememberMe) {
            rememberMeService.save(user);
        } else {
            rememberMeService.clear();
        }

        return user;
    }

    public User login(String username, String password) throws DataAccessException {
        return login(username, password, false);
    }


    public void logout() {
        rememberMeService.clear();
        SessionManager.clearSession();
    }


    public void changePassword(long userId, String currentPassword, String newPassword)
            throws DataAccessException {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại.");
        }
        validatePassword(newPassword);

        String storedHash = userService.getPasswordHashById(userId);
        if (storedHash == null || !CryptUtils.matchesMd5(currentPassword, storedHash)) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        }

        userService.updatePasswordHash(userId, CryptUtils.md5(newPassword));
    }
    public User tryAutoLogin() {
        return rememberMeService.tryAutoLogin();
    }

    private void sendOtp(User user) throws DataAccessException, MessagingException {
        String plainOtp  = generateOtp();
        String hashedOtp = CryptUtils.md5(plainOtp);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId   (user.getId());
        token.setEmail    (user.getEmail());
        token.setOtp      (hashedOtp);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRE_MINS));
        emailVerifRepo.save(token);

        emailService.sendOtpEmail(user.getEmail(), plainOtp);
    }

    private String generateOtp() {
        SecureRandom rng = new SecureRandom();
        int code = 100_000 + rng.nextInt(900_000);
        return String.valueOf(code);
    }

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
    }

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
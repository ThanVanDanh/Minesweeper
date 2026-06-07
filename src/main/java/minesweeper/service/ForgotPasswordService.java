package minesweeper.service;

import minesweeper.model.PasswordResetToken;
import minesweeper.model.User;
import minesweeper.repository.PasswordResetRepository;
import minesweeper.repository.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.CryptUtils;

import jakarta.mail.MessagingException;
import java.time.LocalDateTime;
import java.util.Random;

public class ForgotPasswordService {
    private static final Logger LOG = LoggerFactory.getLogger(ForgotPasswordService.class);
    private static final int RESET_TOKEN_EXPIRE_MINS = 30;

    private final MySqlUserService userService;
    private final PasswordResetRepository resetTokenRepo;
    private final EmailService emailService;

    public ForgotPasswordService() {
        this(new MySqlUserService(),
             new PasswordResetRepository(),
             new EmailService());
    }

    public ForgotPasswordService(MySqlUserService userService,
                                  PasswordResetRepository resetTokenRepo,
                                  EmailService emailService) {
        this.userService = userService;
        this.resetTokenRepo = resetTokenRepo;
        this.emailService = emailService;
    }

    public void sendPasswordResetOtp(String email) throws DataAccessException, MessagingException {
        User user = userService.getUserByEmail(email);
        if (user == null) {
            LOG.warn("Reset password requested for non-existent email: {}", email);
            throw new IllegalArgumentException("Email này không tồn tại trong hệ thống.");
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("Tài khoản chưa được kích hoạt. Vui lòng xác nhận email đăng ký.");
        }

        String otp = String.format("%06d", new Random().nextInt(1000000));
        PasswordResetToken resetToken = new PasswordResetToken(
            user.getId(),
            otp,
            email,
            LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRE_MINS)
        );
        resetTokenRepo.save(resetToken);

        emailService.sendPasswordResetOtpEmail(email, otp);
        LOG.info("Password reset OTP sent to email: {}", email);
    }

    public void resetPassword(String token, String newPassword) throws DataAccessException {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mã OTP.");
        }

        PasswordResetToken resetToken = resetTokenRepo.findByToken(token);
        if (resetToken == null) {
            throw new IllegalArgumentException("Mã OTP không hợp lệ hoặc đã hết hạn.");
        }

        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn (30 phút). Vui lòng yêu cầu cấp lại.");
        }

        // Validate new password
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu mới.");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu cần ít nhất 6 ký tự.");
        }
        String newPasswordHash = CryptUtils.hashPassword(newPassword);
        userService.updatePasswordHash(resetToken.getUserId(), newPasswordHash);
        resetTokenRepo.markUsed(resetToken.getId());

        resetTokenRepo.deleteExpired();

        LOG.info("Password reset successful for user id: {}", resetToken.getUserId());
    }
}


package minesweeper.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailService.class);
    private static final String CONFIG_FILE = "/mail.properties";

    private final Session mailSession;
    private final String  fromAddress;

    public EmailService() {
        Properties cfg = loadConfig();
        this.fromAddress = cfg.getProperty("mail.from", "");
        String password  = cfg.getProperty("mail.password", "");

        Properties sessionProps = new Properties();
        sessionProps.put("mail.smtp.host",            cfg.getProperty("mail.smtp.host", "smtp.gmail.com"));
        sessionProps.put("mail.smtp.port",            cfg.getProperty("mail.smtp.port", "587"));
        sessionProps.put("mail.smtp.auth",            "true");
        sessionProps.put("mail.smtp.starttls.enable", "true");

        this.mailSession = Session.getInstance(sessionProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromAddress, password);
            }
        });
    }

    public void sendOtpEmail(String toEmail, String otp) throws MessagingException {
        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(fromAddress));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        message.setSubject("Mã xác nhận Minesweeper của bạn");
        message.setText(buildEmailBody(otp), "UTF-8", "html");

        Transport.send(message);
        LOG.info("OTP email sent to {}", toEmail);
    }
    public void sendPasswordResetEmail(String toEmail, String resetToken) throws MessagingException {
        String resetLink = "https://your-app-domain.com/reset-password?token=" + resetToken;
        String subject = "Đặt lại mật khẩu Minesweeper";
        String body = buildPasswordResetEmailBody(resetLink);

        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(fromAddress));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        message.setSubject(subject);
        message.setText(body, "UTF-8", "html");

        Transport.send(message);
        LOG.info("Password reset email sent to {}", toEmail);
    }
    private String buildEmailBody(String otp) {
        return "<div style='font-family:monospace;background:#0d1117;color:#c9d1d9;padding:32px;border-radius:8px'>" +
                "<h2 style='color:#58a6ff;letter-spacing:4px'>MINESWEEPER</h2>" +
                "<p>Mã xác nhận email của bạn:</p>" +
                "<div style='font-size:36px;font-weight:bold;letter-spacing:12px;color:#ffffff;" +
                "background:#161b22;padding:20px 32px;border-radius:6px;display:inline-block'>" +
                otp +
                "</div>" +
                "<p style='color:#8b949e;font-size:13px;margin-top:24px'>Mã có hiệu lực trong <b>10 phút</b>. " +
                "Không chia sẻ mã này cho bất kỳ ai.</p>" +
                "</div>";
    }

    private String buildPasswordResetEmailBody(String resetLink) {
        return "<div style='font-family:Arial,sans-serif;background:#f5f5f5;padding:32px;'>" +
                "<div style='background:#ffffff;padding:24px;border-radius:8px;max-width:600px;margin:0 auto;'>" +
                "<h2 style='color:#2196F3;margin-top:0'>Đặt lại mật khẩu Minesweeper</h2>" +
                "<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>" +
                "<p style='margin:24px 0;'>" +
                "<a href='" + resetLink + "' style='background-color:#2196F3;color:white;padding:12px 24px;" +
                "text-decoration:none;border-radius:4px;display:inline-block;font-weight:bold;'>" +
                "Đặt lại mật khẩu" +
                "</a>" +
                "</p>" +
                "<p style='color:#666;font-size:13px;'>" +
                "Link này sẽ hết hạn trong <b>30 phút</b>." +
                "</p>" +
                "<p style='color:#999;font-size:12px;'>" +
                "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này." +
                "</p>" +
                "</div>" +
                "</div>";
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) props.load(is);
        } catch (IOException e) {
            LOG.warn("mail.properties not found, email sending will fail");
        }
        return props;
    }
}
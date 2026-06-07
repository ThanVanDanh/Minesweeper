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
    public void sendPasswordResetOtpEmail(String toEmail, String otp) throws MessagingException {
        String subject = "Đặt lại mật khẩu Minesweeper";
        String body = buildPasswordResetEmailBody(otp);

        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(fromAddress));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        message.setSubject(subject);
        message.setText(body, "UTF-8", "html");

        Transport.send(message);
        LOG.info("Password reset OTP email sent to {}", toEmail);
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

    private String buildPasswordResetEmailBody(String otp) {
        return "<div style='font-family:monospace;background:#0d1117;color:#c9d1d9;padding:32px;border-radius:8px'>" +
                "<h2 style='color:#58a6ff;letter-spacing:4px'>MINESWEEPER</h2>" +
                "<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>" +
                "<p>Mã xác nhận (OTP) để đổi mật khẩu của bạn là:</p>" +
                "<div style='font-size:36px;font-weight:bold;letter-spacing:12px;color:#ffffff;" +
                "background:#161b22;padding:20px 32px;border-radius:6px;display:inline-block'>" +
                otp +
                "</div>" +
                "<p style='color:#8b949e;font-size:13px;margin-top:24px'>Mã có hiệu lực trong <b>30 phút</b>. " +
                "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>" +
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
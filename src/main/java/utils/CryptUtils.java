package utils;

import org.mindrot.jbcrypt.BCrypt;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * CryptUtils - Lớp tiện ích mã hóa mật khẩu và OTP.
 * Sử dụng BCrypt cho tất cả hash (mật khẩu và OTP).
 *
 * Sử dụng trong:
 * - Đăng ký: mã hóa mật khẩu trước khi lưu vào DB (1.1.6)
 * - Đăng nhập: xác minh mật khẩu nhập vào với hash trong DB (1.2.7)
 * - Đổi mật khẩu: xác minh mật khẩu hiện tại (1.4.6) và mã hóa mật khẩu mới (1.4.7)
 * - OTP: mã hóa mã OTP trước khi lưu vào DB và xác minh khi người dùng nhập
 */
public final class CryptUtils {
    private static final byte[] AES_KEY = "MinesweeperApp12".getBytes(StandardCharsets.UTF_8);
    private static final String AES_ALGORITHM = "AES";

    private CryptUtils() {
    }

    /**
     * Mã hóa chuỗi plain-text sử dụng BCrypt (dùng cho cả mật khẩu và OTP).
     *
     * @param value chuỗi plain-text cần mã hóa
     * @return chuỗi BCrypt hash, hoặc null nếu value là null
     */
    public static String hashPassword(String value) {
        if (value == null) {
            return null;
        }
        return BCrypt.hashpw(value, BCrypt.gensalt(12));
    }

    /**
     * Xác minh chuỗi plain-text với BCrypt hash đã lưu (dùng cho cả mật khẩu và OTP).
     *
     * @param rawValue chuỗi plain-text người dùng nhập vào
     * @param hashed   BCrypt hash đã lưu trong DB
     * @return true nếu khớp, false nếu không khớp hoặc hash không hợp lệ
     */
    public static boolean verifyPassword(String rawValue, String hashed) {
        if (rawValue == null || hashed == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawValue, hashed);
        } catch (Exception e) {
            // Hash không hợp lệ (ví dụ: dữ liệu cũ hoặc corrupted) → trả về false, không crash app.
            return false;
        }
    }

    /**
     * Mã hóa chuỗi văn bản bằng AES-128 (dùng cho file token).
     */
    public static String encryptAES(String plainText) {
        if (plainText == null) return null;
        try {
            SecretKeySpec secretKey = new SecretKeySpec(AES_KEY, AES_ALGORITHM);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi mã hóa dữ liệu", e);
        }
    }

    /**
     * Giải mã chuỗi đã mã hóa bằng AES-128.
     */
    public static String decryptAES(String encryptedText) {
        if (encryptedText == null) return null;
        try {
            SecretKeySpec secretKey = new SecretKeySpec(AES_KEY, AES_ALGORITHM);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Lỗi giải mã (file bị sửa tay, token plain-text cũ, v.v...)
            return null;
        }
    }
}

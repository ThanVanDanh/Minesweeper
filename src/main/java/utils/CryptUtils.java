package utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * CryptUtils - Lớp tiện ích mã hóa mật khẩu.
 * Đảm nhiệm mã hóa và xác minh mật khẩu cho UC01 - Đăng ký/Đăng nhập.
 * 
 * Sử dụng trong:
 * - Đăng ký: mã hóa mật khẩu trước khi lưu vào DB (1.1.6)
 * - Đăng nhập: xác minh mật khẩu nhập vào với hash trong DB (1.2.7)
 * - Đổi mật khẩu: xác minh mật khẩu hiện tại (1.4.6) và mã hóa mật khẩu mới (1.4.7)
 */
public final class CryptUtils {
    private CryptUtils() {
    }

    /**
     * Mã hóa chuỗi plain-text thành MD5 hash.
     * Sử dụng trong: 1.1.6 (đăng ký), 1.4.7 (đổi mật khẩu).
     * 
     * @param value chuỗi plain-text cần mã hóa (thường là mật khẩu)
     * @return chuỗi MD5 hash dạng hex, hoặc null nếu value là null
     */
    public static String md5(String value) {
        // 1.6.1 md5(value): nhận mật khẩu plain-text.
        if (value == null) {
            return null;
        }
        try {
            // 1.6.2 MessageDigest.getInstance("MD5"): khởi tạo thuật toán hash.
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            // 1.6.3 digest(...): tạo mảng byte hash từ chuỗi đầu vào.
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            
            // 1.6.4 toHex(...): chuyển hash bytes sang chuỗi hex.
            // 1.6.5 Trả về passwordHash để lưu vào cột password_hash.
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    /**
     * So sánh mật khẩu plain-text với MD5 hash đã lưu.
     * Sử dụng trong: 1.2.7 (đăng nhập), 1.4.6 (đổi mật khẩu).
     * 
     * @param rawValue mật khẩu plain-text người dùng nhập vào
     * @param expectedHash MD5 hash đã lưu trong DB
     * @return true nếu khớp, false nếu không khớp
     */
    public static boolean matchesMd5(String rawValue, String expectedHash) {
        // 1.6.6 matchesMd5(rawValue, expectedHash): nhận mật khẩu nhập vào và hash đã lưu trong DB.
        if (rawValue == null || expectedHash == null) {
            return false;
        }
        
        // 1.6.7 md5(rawValue): hash lại mật khẩu người dùng nhập.
        String actual = md5(rawValue);
        
        // 1.6.8 So sánh hash vừa tạo với expectedHash.
        // 1.6.9 Trả về true nếu khớp, false nếu không khớp.
        return actual != null && actual.equalsIgnoreCase(expectedHash);
    }

    /**
     * Chuyển đổi mảng byte thành chuỗi hex.
     * 
     * @param bytes mảng byte cần chuyển đổi
     * @return chuỗi hex
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

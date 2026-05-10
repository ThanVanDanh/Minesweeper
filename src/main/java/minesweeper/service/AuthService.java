package minesweeper.service;

import minesweeper.model.enums.Role;
import minesweeper.model.User;
import minesweeper.repository.exception.DataAccessException;
import utils.CryptUtils;

public class AuthService {
    private final MySqlUserService userService;

    public AuthService() {
        this(new MySqlUserService());
    }

    public AuthService(MySqlUserService userService) {
        this.userService = userService;
    }

    public User register(String username, String displayName, String password) throws DataAccessException {
        validateUsername(username);
        validatePassword(password);

        if (userService.getUserByUsername(username) != null) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }

        String passwordHash = CryptUtils.md5(password);
        userService.createUserWithPassword(username, displayName, passwordHash, Role.PLAYER);
        return userService.getAuthUserByUsername(username);
    }

    public User login(String username, String password) throws DataAccessException {
        validateUsername(username);
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu.");
        }

        User user = userService.getAuthUserByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu.");
        }
        if (!user.isActive()) {
            throw new IllegalArgumentException("Tài khoản đã bị khóa.");
        }
        if (!CryptUtils.matchesMd5(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu.");
        }

        userService.updateLastLogin(user.getId());
        SessionManager.createSession(user);
        return user;
    }

    public void logout() {
        SessionManager.clearSession();
    }

    public void changePassword(long userId, String currentPassword, String newPassword) throws DataAccessException {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại.");
        }
        validatePassword(newPassword);

        String storedHash = userService.getPasswordHashById(userId);
        if (storedHash == null || !CryptUtils.matchesMd5(currentPassword, storedHash)) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        }

        String newHash = CryptUtils.md5(newPassword);
        userService.updatePasswordHash(userId, newHash);
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
}


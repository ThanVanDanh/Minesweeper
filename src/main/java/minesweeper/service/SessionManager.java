package minesweeper.service;

import minesweeper.model.User;

public final class SessionManager {
    private static User currentUser;

    private SessionManager() {
    }

    public static void createSession(User user) {
        // 1.2.9 SessionManager.createSession(user): tạo phiên đăng nhập.
        currentUser = user;
    }

    public static void clearSession() {
        // 1.3.2 clearSession(): xóa currentUser khỏi bộ nhớ ứng dụng.
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}


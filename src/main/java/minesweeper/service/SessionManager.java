package minesweeper.service;

import minesweeper.model.User;

public final class SessionManager {
    private static User currentUser;

    private SessionManager() {
    }

    public static void createSession(User user) {
        currentUser = user;
    }

    public static void clearSession() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}


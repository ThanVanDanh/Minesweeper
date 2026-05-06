package minesweeper.model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String username;
    private String passwordHash; // BCrypt hash
    private String displayName;
    private String email;
    private Role role;
    private boolean isActive;    // Admin có thể khóa tài khoản
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    // ── Constructor ───────────────────────────────────────
    public User() {}

    public User(String username, String passwordHash, String displayName,
                String email, Role role) {
        this.username     = username;
        this.passwordHash = passwordHash;
        this.displayName  = displayName;
        this.email        = email;
        this.role         = role;
        this.isActive     = true;
        this.createdAt    = LocalDateTime.now();
    }
}


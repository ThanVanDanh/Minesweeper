package minesweeper.model;

import java.time.LocalDateTime;

public class User {
    private long id;
    private String username;
    private String passwordHash; // MD5 hash
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

    // ── Getters ────────────────────────────────────────
    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public boolean isActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }

    // ── Setters ────────────────────────────────────────
    public void setId(long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(Role role) { this.role = role; }
    public void setActive(boolean active) { isActive = active; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
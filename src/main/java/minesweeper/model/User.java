package minesweeper.model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String username;
    private String passwordHash; // BCrypt hash
    private String displayName;
    private Role role;
    private boolean isActive;    // Admin có thể khóa tài khoản
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    // ── Constructor ───────────────────────────────────────
    public User() {}

    public User(String username, String passwordHash, String displayName, Role role) {
        this.username     = username;
        this.passwordHash = passwordHash;
        this.displayName  = displayName;
        this.role         = role;
        this.isActive     = true;
        this.createdAt    = LocalDateTime.now();
    }

    // ── Getters / Setters ─────────────────────────────────────────
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getUsername()                 { return username; }
    public void setUsername(String username)    { this.username = username; }

    public String getPasswordHash()             { return passwordHash; }
    public void setPasswordHash(String h)       { this.passwordHash = h; }

    public String getDisplayName()              { return displayName; }
    public void setDisplayName(String name)     { this.displayName = name; }

    public Role getRole()                       { return role; }
    public void setRole(Role role)              { this.role = role; }

    public boolean isActive()                   { return isActive; }
    public void setActive(boolean active)       { this.isActive = active; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime t)   { this.createdAt = t; }

    public LocalDateTime getLastLoginAt()       { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime t) { this.lastLoginAt = t; }

    @Override
    public String toString() {
        return String.format("User{id=%d, username=%s, role=%s, active=%b}",
                id, username, role, isActive);
    }
}


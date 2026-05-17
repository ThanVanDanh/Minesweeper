package minesweeper.model;

import java.time.LocalDateTime;

public class AuditLog {
    private long id;
    private Long adminId;
    private String action;
    private String target;
    private String details;
    private LocalDateTime createdAt;

    public AuditLog() {}

    public AuditLog(Long adminId, String action, String target, String details) {
        this.adminId = adminId;
        this.action = action;
        this.target = target;
        this.details = details;
        this.createdAt = LocalDateTime.now();
    }


    // GETTER - SETTER
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

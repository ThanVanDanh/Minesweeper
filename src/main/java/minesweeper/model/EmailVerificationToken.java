package minesweeper.model;
import java.time.LocalDateTime;

public class EmailVerificationToken {

    private long   id;
    private long   userId;
    private String email;       // email cần xác nhận
    private String otp;         // mã 6 chữ số (plain, lưu hash trong DB)
    private LocalDateTime expiresAt;
    private boolean used;
    private LocalDateTime createdAt;

    public EmailVerificationToken() {}


    public long getId()                      { return id; }
    public void setId(long id)               { this.id = id; }

    public long getUserId()                  { return userId; }
    public void setUserId(long userId)       { this.userId = userId; }

    public String getEmail()                 { return email; }
    public void setEmail(String email)       { this.email = email; }

    public String getOtp()                   { return otp; }
    public void setOtp(String otp)           { this.otp = otp; }

    public LocalDateTime getExpiresAt()              { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt){ this.expiresAt = expiresAt; }

    public boolean isUsed()                  { return used; }
    public void setUsed(boolean used)        { this.used = used; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }
    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }
}
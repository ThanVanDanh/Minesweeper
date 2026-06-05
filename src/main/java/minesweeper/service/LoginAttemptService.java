package minesweeper.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service quản lý việc kiểm tra và giới hạn số lần đăng nhập sai (Rate Limiting).
 * Tạm khóa đăng nhập của tài khoản nếu nhập sai mật khẩu quá 5 lần trong vòng 5 phút.
 */
public class LoginAttemptService {
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION_MINUTES = 5;

    private static class AttemptInfo {
        int count;
        Instant lockedUntil;

        AttemptInfo() {
            this.count = 0;
            this.lockedUntil = null;
        }
    }

    // Cache dùng chung dạng static để lưu trạng thái đăng nhập sai của các tài khoản
    private static final Map<String, AttemptInfo> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String username) {
        if (username != null) {
            attemptsCache.remove(username.toLowerCase());
        }
    }

    public void loginFailed(String username) {
        if (username == null) return;
        attemptsCache.compute(username.toLowerCase(), (key, info) -> {
            if (info == null) {
                info = new AttemptInfo();
            }
            info.count++;
            if (info.count >= MAX_ATTEMPTS) {
                info.lockedUntil = Instant.now().plusSeconds(LOCK_TIME_DURATION_MINUTES * 60);
            }
            return info;
        });
    }

    public boolean isLocked(String username) {
        if (username == null) return false;
        AttemptInfo info = attemptsCache.get(username.toLowerCase());
        if (info == null) {
            return false;
        }
        if (info.lockedUntil != null) {
            if (Instant.now().isBefore(info.lockedUntil)) {
                return true;
            } else {
                // Hết thời gian khóa, tự động reset
                attemptsCache.remove(username.toLowerCase());
            }
        }
        return false;
    }

    public long getRemainingLockTimeMinutes(String username) {
        if (username == null) return 0;
        AttemptInfo info = attemptsCache.get(username.toLowerCase());
        if (info != null && info.lockedUntil != null) {
            long diffSeconds = info.lockedUntil.getEpochSecond() - Instant.now().getEpochSecond();
            long minutes = (diffSeconds + 59) / 60; // Làm tròn lên phút
            return Math.max(0, minutes);
        }
        return 0;
    }
}

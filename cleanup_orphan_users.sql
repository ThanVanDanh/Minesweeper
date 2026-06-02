-- Xóa các user đã đăng ký nhưng chưa xác nhận email (is_active = false)
-- và KHÔNG có token xác nhận còn hiệu lực
-- Chạy file này 1 lần trong MySQL Workbench để dọn dẹp "rác"

USE minesweeper_db;

-- Xem trước các user bị stuck (optional, để kiểm tra)
SELECT id, username, email, is_active, created_at
FROM users
WHERE is_active = FALSE
  AND role = 'PLAYER';

-- Xóa game sessions của các user đó trước (do FK constraint)
DELETE gs FROM game_sessions gs
    JOIN users u ON gs.user_id = u.id
WHERE u.is_active = FALSE AND u.role = 'PLAYER';

-- Xóa email verification tokens
DELETE evt FROM email_verification_tokens evt
    JOIN users u ON evt.user_id = u.id
WHERE u.is_active = FALSE AND u.role = 'PLAYER';

-- Xóa user rác
DELETE FROM users
WHERE is_active = FALSE AND role = 'PLAYER';

SELECT ROW_COUNT() AS deleted_orphan_users;

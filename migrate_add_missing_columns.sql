-- ============================================================
-- Migration: Thêm các cột/bảng còn thiếu cho tính năng
--            "Ghi nhớ đăng nhập" và "Xác nhận email OTP"
-- Chạy file này 1 lần trong MySQL Workbench hoặc CLI
-- ============================================================

USE minesweeper_db;

-- 1. Thêm cột remember_token vào bảng users (nếu chưa có)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS remember_token VARCHAR(100) NULL;

-- 2. Tạo bảng email_verification_tokens (nếu chưa có)
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT UNSIGNED NOT NULL,
    email      VARCHAR(255)    NOT NULL,
    otp_hash   VARCHAR(255)    NOT NULL,
    expires_at TIMESTAMP       NOT NULL,
    is_used    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_evt_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE CASCADE,

    KEY idx_evt_user (user_id)
) ENGINE=InnoDB;

SELECT 'Migration completed successfully!' AS status;

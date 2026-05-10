CREATE DATABASE IF NOT EXISTS minesweeper_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE minesweeper_db;

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    display_name  VARCHAR(100) NULL,
    password_hash VARCHAR(255) NULL,
    email         VARCHAR(100) NULL,
    role          ENUM('PLAYER', 'ADMIN') NOT NULL DEFAULT 'PLAYER',
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,

    KEY idx_username (username)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS game_levels (
    id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    level_name  VARCHAR(50) NOT NULL UNIQUE,
    rows_count  INT UNSIGNED NOT NULL,
    cols_count  INT UNSIGNED NOT NULL,
    mines_count INT UNSIGNED NOT NULL,
    sort_order  TINYINT UNSIGNED NOT NULL DEFAULT 0,
    level_type  ENUM('PRESET', 'CUSTOM') NOT NULL DEFAULT 'PRESET',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    KEY idx_level_name (level_name),

    CONSTRAINT chk_game_levels_board_size
    CHECK (rows_count > 0 AND cols_count > 0),
    CONSTRAINT chk_game_levels_mines_count
    CHECK (mines_count > 0 AND mines_count < rows_count * cols_count)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS game_sessions (
                                             id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                             user_id         BIGINT UNSIGNED NOT NULL,
                                             level_id        INT UNSIGNED NOT NULL,
                                             result          ENUM('WIN', 'LOSE') NOT NULL,
    completion_time BIGINT UNSIGNED NOT NULL DEFAULT 0,
    score           INT NOT NULL DEFAULT 0,
    opened_cells    INT UNSIGNED NOT NULL DEFAULT 0,
    flagged_cells   INT UNSIGNED NOT NULL DEFAULT 0,
    started_at      TIMESTAMP NULL,
    first_click_at  TIMESTAMP NULL,
    ended_at        TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_game_sessions_user
    FOREIGN KEY (user_id) REFERENCES users(id)
                                                                 ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_game_sessions_level
    FOREIGN KEY (level_id) REFERENCES game_levels(id)
                                                                 ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_game_sessions_time
    CHECK (completion_time >= 0),
    CONSTRAINT chk_game_sessions_cells
    CHECK (opened_cells >= 0 AND flagged_cells >= 0),
    CONSTRAINT chk_game_sessions_datetime
    CHECK (
              first_click_at IS NULL OR ended_at IS NULL
              OR ended_at >= first_click_at
          ),

    KEY idx_user_result (user_id, result),
    KEY idx_user_level_created (user_id, level_id, created_at DESC),
    KEY idx_level_result_time (level_id, result, completion_time ASC),
    KEY idx_level_score (level_id, score DESC),
    KEY idx_created (created_at DESC)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS player_best_scores (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED NOT NULL,
    level_id        INT UNSIGNED NOT NULL,
    best_time       BIGINT UNSIGNED NOT NULL,
    best_score      INT NOT NULL DEFAULT 0,
    game_session_id BIGINT UNSIGNED NOT NULL,
    achieved_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_player_best_scores_user_level
    UNIQUE (user_id, level_id),
    CONSTRAINT fk_player_best_scores_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_player_best_scores_level
    FOREIGN KEY (level_id) REFERENCES game_levels(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_player_best_scores_session
    FOREIGN KEY (game_session_id) REFERENCES game_sessions(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_player_best_scores_time
    CHECK (best_time >= 0),

    KEY idx_level_rank (level_id, best_time ASC, best_score DESC, achieved_at ASC)
    ) ENGINE=InnoDB;

INSERT INTO game_levels (level_name, rows_count, cols_count, mines_count, sort_order, level_type)
VALUES
    ('EASY',   9,  9,  10,  1, 'PRESET'),
    ('MEDIUM', 16, 16,  40, 2, 'PRESET'),
    ('HARD',   16, 30,  99, 3, 'PRESET'),
    ('EXPERT', 20, 30, 145, 4, 'PRESET')
    ON DUPLICATE KEY UPDATE
                         rows_count  = VALUES(rows_count),
                         cols_count  = VALUES(cols_count),
                         mines_count = VALUES(mines_count),
                         sort_order  = VALUES(sort_order);

-- P15: ranking snapshot table for time-series rank history queries
CREATE TABLE IF NOT EXISTS ranking_snapshots (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT UNSIGNED NOT NULL,
    level_id      INT UNSIGNED NULL,          -- NULL = global (cross-level) snapshot
    rank_position INT UNSIGNED NOT NULL,
    best_score    INT NOT NULL DEFAULT 0,
    best_time_ms  BIGINT UNSIGNED NOT NULL DEFAULT 0,
    snapshot_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ranking_snapshots_user
    FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_ranking_snapshots_level
    FOREIGN KEY (level_id) REFERENCES game_levels(id)
        ON UPDATE CASCADE ON DELETE SET NULL,

    KEY idx_snapshot_user_level (user_id, level_id, snapshot_at DESC),
    KEY idx_snapshot_level_at   (level_id, snapshot_at DESC)
) ENGINE=InnoDB;

-- P16: audit log for admin actions (deletions, locks, role changes)
CREATE TABLE IF NOT EXISTS audit_log (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    admin_id   BIGINT UNSIGNED NULL,          -- NULL if system-generated
    action     VARCHAR(50)  NOT NULL,         -- e.g. 'DELETE_SESSION', 'LOCK_USER'
    target     VARCHAR(255) NOT NULL,         -- e.g. 'game_sessions:42'
    details    TEXT         NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_log_admin
    FOREIGN KEY (admin_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE SET NULL,

    KEY idx_audit_created (created_at DESC),
    KEY idx_audit_action  (action)
) ENGINE=InnoDB;
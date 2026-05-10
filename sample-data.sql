-- Sample data for Minesweeper ranking testing
-- First, verify EXPERT level exists
SELECT "=== Step 1: Check game_levels ===" AS step;
SELECT * FROM game_levels WHERE level_name = 'EXPERT';

-- Insert test users (DELETE old ones first to reset)
DELETE FROM users WHERE username LIKE 'user%';
INSERT INTO users (username, display_name, password_hash, email, role, is_active)
VALUES
    ('user1', 'NightSapper', 'hash1', 'user1@test.com', 'PLAYER', TRUE),
    ('user2', 'BombDisposer', 'hash2', 'user2@test.com', 'PLAYER', TRUE),
    ('user3', 'GridMaster_VN', 'hash3', 'user3@test.com', 'PLAYER', TRUE),
    ('user4', 'QuantumSweep', 'hash4', 'user4@test.com', 'PLAYER', TRUE),
    ('user5', 'ShadowMiner', 'hash5', 'user5@test.com', 'PLAYER', TRUE),
    ('user6', 'MineHunter', 'hash6', 'user6@test.com', 'PLAYER', TRUE),
    ('user7', 'VictorySeeker', 'hash7', 'user7@test.com', 'PLAYER', TRUE),
    ('user8', 'RiskTaker', 'hash8', 'user8@test.com', 'PLAYER', TRUE),
    ('user9', 'PrecisionMaster', 'hash9', 'user9@test.com', 'PLAYER', TRUE),
    ('user10', 'SpeedRunner', 'hash10', 'user10@test.com', 'PLAYER', TRUE);

SELECT "=== Step 2: Check users after insert ===" AS step;
SELECT id, username FROM users WHERE username LIKE 'user%' LIMIT 5;

-- Insert sample game sessions for EXPERT level (level_id = 4)
-- EXPERT: 20×30 | 145 mines

-- User 1: High score, fast time (Rank 1)
INSERT INTO game_sessions (user_id, level_id, result, completion_time, score, opened_cells, flagged_cells, started_at, first_click_at, ended_at)
VALUES
    (1, 4, 'WIN', 32000, 98420, 450, 145, NOW(), NOW(), NOW()),
    (1, 4, 'WIN', 35000, 97100, 448, 145, NOW(), NOW(), NOW()),
    (1, 4, 'LOSE', 45000, 95000, 400, 140, NOW(), NOW(), NOW());

-- User 2: Good score, slower time (Rank 2)
INSERT INTO game_sessions (user_id, level_id, result, completion_time, score, opened_cells, flagged_cells, started_at, first_click_at, ended_at)
VALUES
    (2, 4, 'WIN', 38000, 87310, 440, 145, NOW(), NOW(), NOW()),
    (2, 4, 'WIN', 40000, 86500, 438, 145, NOW(), NOW(), NOW()),
    (2, 4, 'LOSE', 50000, 82000, 380, 135, NOW(), NOW(), NOW());

-- User 3: Medium score (Rank 3)
INSERT INTO game_sessions (user_id, level_id, result, completion_time, score, opened_cells, flagged_cells, started_at, first_click_at, ended_at)
VALUES
    (3, 4, 'WIN', 41000, 76100, 430, 144, NOW(), NOW(), NOW()),
    (3, 4, 'LOSE', 55000, 70000, 350, 130, NOW(), NOW(), NOW());

-- User 4: Lower score (Rank 4)
INSERT INTO game_sessions (user_id, level_id, result, completion_time, score, opened_cells, flagged_cells, started_at, first_click_at, ended_at)
VALUES
    (4, 4, 'WIN', 45000, 65200, 420, 143, NOW(), NOW(), NOW()),
    (4, 4, 'LOSE', 60000, 60000, 340, 125, NOW(), NOW(), NOW());

-- User 5: Multiple games (Rank 5)
INSERT INTO game_sessions (user_id, level_id, result, completion_time, score, opened_cells, flagged_cells, started_at, first_click_at, ended_at)
VALUES
    (5, 4, 'WIN', 48000, 54800, 410, 142, NOW(), NOW(), NOW()),
    (5, 4, 'LOSE', 65000, 50000, 330, 120, NOW(), NOW(), NOW());

-- User 6: More games
INSERT INTO game_sessions (user_id, level_id, result, completion_time, score, opened_cells, flagged_cells, started_at, first_click_at, ended_at)
VALUES
    (6, 4, 'WIN', 50000, 44500, 400, 140, NOW(), NOW(), NOW()),
    (6, 4, 'LOSE', 70000, 40000, 320, 115, NOW(), NOW(), NOW());

-- User 7
INSERT INTO game_sessions (user_id, level_id, result, completion_time, score, opened_cells, flagged_cells, started_at, first_click_at, ended_at)
VALUES
    (7, 4, 'WIN', 52000, 35600, 390, 138, NOW(), NOW(), NOW()),
    (7, 4, 'LOSE', 75000, 30000, 310, 110, NOW(), NOW(), NOW());

-- User 8
INSERT INTO game_sessions (user_id, level_id, result, completion_time, score, opened_cells, flagged_cells, started_at, first_click_at, ended_at)
VALUES
    (8, 4, 'WIN', 55000, 28700, 380, 136, NOW(), NOW(), NOW()),
    (8, 4, 'LOSE', 80000, 25000, 300, 105, NOW(), NOW(), NOW());

-- User 9
INSERT INTO game_sessions (user_id, level_id, result, completion_time, score, opened_cells, flagged_cells, started_at, first_click_at, ended_at)
VALUES
    (9, 4, 'WIN', 58000, 22900, 370, 134, NOW(), NOW(), NOW()),
    (9, 4, 'LOSE', 85000, 20000, 290, 100, NOW(), NOW(), NOW());

-- User 10
INSERT INTO game_sessions (user_id, level_id, result, completion_time, score, opened_cells, flagged_cells, started_at, first_click_at, ended_at)
VALUES
    (10, 4, 'WIN', 60000, 18500, 360, 132, NOW(), NOW(), NOW()),
    (10, 4, 'LOSE', 90000, 15000, 280, 95, NOW(), NOW(), NOW());

-- Verify data
SELECT "=== Ranking dữ liệu mẫu ===" AS info;
SELECT
    u.username,
    COUNT(gs.id) AS total_games,
    SUM(CASE WHEN UPPER(gs.result) = 'WIN' THEN 1 ELSE 0 END) AS wins,
    MAX(gs.score) AS best_score,
    MIN(CASE WHEN UPPER(gs.result) = 'WIN' THEN gs.completion_time END) AS best_time_ms
FROM game_sessions gs
JOIN users u ON u.id = gs.user_id
WHERE gs.level_id = 4
GROUP BY u.id, u.username
ORDER BY best_score DESC, best_time_ms ASC;


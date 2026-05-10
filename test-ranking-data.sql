-- Fix: Simple insert for ranking test data
-- Author: Debug session

-- Step 1: Clear old data
DELETE FROM game_sessions WHERE level_id = 4;
DELETE FROM users WHERE username LIKE 'user%';

-- Step 2: Insert fresh users
INSERT INTO users (username, display_name, password_hash, email, role, is_active)
VALUES
    ('user1', 'NightSapper', 'hash1', 'user1@test.com', 'PLAYER', TRUE),
    ('user2', 'BombDisposer', 'hash2', 'user2@test.com', 'PLAYER', TRUE),
    ('user3', 'GridMaster_VN', 'hash3', 'user3@test.com', 'PLAYER', TRUE),
    ('user4', 'QuantumSweep', 'hash4', 'user4@test.com', 'PLAYER', TRUE),
    ('user5', 'ShadowMiner', 'hash5', 'user5@test.com', 'PLAYER', TRUE);

-- Step 3: Verify users inserted
SELECT "=== Users created ===" AS status;
SELECT id, username FROM users WHERE username LIKE 'user%' ORDER BY id;

-- Step 4: Get user IDs for reference
-- Assuming IDs are auto-increment, they should be consecutive

-- Step 5: Insert game sessions with correct user IDs
-- Using direct IDs: assume they are 1,2,3,4,5 or find them first
SET @user1_id = (SELECT id FROM users WHERE username = 'user1');
SET @user2_id = (SELECT id FROM users WHERE username = 'user2');
SET @user3_id = (SELECT id FROM users WHERE username = 'user3');
SET @user4_id = (SELECT id FROM users WHERE username = 'user4');
SET @user5_id = (SELECT id FROM users WHERE username = 'user5');

SELECT "=== User IDs ===" AS status, @user1_id, @user2_id, @user3_id, @user4_id, @user5_id;

-- Insert game sessions for EXPERT level (level_id = 4)
INSERT INTO game_sessions (user_id, level_id, result, completion_time, score, opened_cells, flagged_cells, started_at, first_click_at, ended_at)
VALUES
    (@user1_id, 4, 'WIN', 32000, 98420, 450, 145, NOW(), NOW(), NOW()),
    (@user1_id, 4, 'WIN', 35000, 97100, 448, 145, NOW(), NOW(), NOW()),
    (@user1_id, 4, 'LOSE', 45000, 95000, 400, 140, NOW(), NOW(), NOW()),
    (@user2_id, 4, 'WIN', 38000, 87310, 440, 145, NOW(), NOW(), NOW()),
    (@user2_id, 4, 'WIN', 40000, 86500, 438, 145, NOW(), NOW(), NOW()),
    (@user3_id, 4, 'WIN', 41000, 76100, 430, 144, NOW(), NOW(), NOW()),
    (@user3_id, 4, 'LOSE', 55000, 70000, 350, 130, NOW(), NOW(), NOW()),
    (@user4_id, 4, 'WIN', 45000, 65200, 420, 143, NOW(), NOW(), NOW()),
    (@user4_id, 4, 'LOSE', 60000, 60000, 340, 125, NOW(), NOW(), NOW()),
    (@user5_id, 4, 'WIN', 48000, 54800, 410, 142, NOW(), NOW(), NOW()),
    (@user5_id, 4, 'LOSE', 65000, 50000, 330, 120, NOW(), NOW(), NOW());

-- Step 6: Verify game sessions inserted
SELECT "=== Game sessions created ===" AS status;
SELECT COUNT(*) AS total_sessions FROM game_sessions WHERE level_id = 4;

-- Step 7: Show ranking data
SELECT "=== Ranking Test ===" AS status;
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


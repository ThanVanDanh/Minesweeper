package minesweeper.service;

import minesweeper.model.Achievement;
import minesweeper.model.GameResult;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Service tính toán thành tựu từ lịch sử ván đấu.
 */
public class AchievementService {

    public Map<Achievement, Boolean> evaluate(List<GameResult> history) {
        Map<Achievement, Boolean> result = new EnumMap<>(Achievement.class);

        result.put(Achievement.CO_DIEN,    hasConsecutiveWins(history, 2));
        result.put(Achievement.TON_TRONG, hasConsecutiveWins(history, 5));
        result.put(Achievement.NGU_LOM,    hasConsecutiveLosses(history, 2));

        return result;
    }

    private boolean hasConsecutiveWins(List<GameResult> history, int n) {
        return hasConsecutiveStreak(history, true, n);
    }

    private boolean hasConsecutiveLosses(List<GameResult> history, int n) {
        return hasConsecutiveStreak(history, false, n);
    }

    private boolean hasConsecutiveStreak(List<GameResult> history, boolean wonTarget, int n) {
        int streak = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).isWon() == wonTarget) {
                streak++;
                if (streak >= n) return true;
            } else {
                streak = 0;
            }
        }
        return false;
    }
}
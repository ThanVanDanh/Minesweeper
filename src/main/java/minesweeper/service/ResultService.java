package minesweeper.service;

import minesweeper.model.Difficulty;
import minesweeper.model.GameResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ResultService {

    private List<GameResult> results = new ArrayList<>();

    public ResultService() {

        addSample(1, "nightsapper", 98420, 32, Difficulty.EASY, -1);

        addSample(2, "bombdisposer", 87310, 38, Difficulty.EASY, -2);

        addSample(3, "gridmaster_vn", 76100, 41, Difficulty.HARD, -3);

        addSample(4, "cheater_x", 9999, 1, Difficulty.MEDIUM, -1);
    }

    private void addSample(int id, String username, int score, long time, Difficulty difficulty, int daysAgo) {

        GameResult r = new GameResult(id, username, score, time, difficulty, LocalDateTime.now().plusDays(daysAgo));

        results.add(r);
    }

    public List<GameResult> getAllResults() {
        return results;
    }

    public List<GameResult> filterResults(Difficulty difficulty, String username, LocalDate from, LocalDate to) {

        List<GameResult> filtered = new ArrayList<>();
        for (GameResult r : results) {
            boolean match = true;
            if (difficulty != null && r.getDifficulty() != difficulty) {
                match = false;
            }
            if (username != null && !username.isBlank()) {
                if (!r.getUsername().toLowerCase().contains(username.toLowerCase())) {
                    match = false;
                }
            }
            if (from != null) {
                if (r.getPlayedAt().toLocalDate().isBefore(from)) {
                    match = false;
                }
            }
            if (to != null) {
                if (r.getPlayedAt().toLocalDate().isAfter(to)) {
                    match = false;
                }
            }
            if (match) {
                filtered.add(r);
            }
        }
        return filtered;
    }

    public void deleteFraudResults(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (int i = 0; i < results.size(); i++) {
            GameResult r = results.get(i);
            if (ids.contains(r.getId())) {
                System.out.println("Xoá kết quả của user: " + r.getUsername());
                results.remove(i);
                i--;
            }
        }
    }
}
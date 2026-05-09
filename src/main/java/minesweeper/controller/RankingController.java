package minesweeper.controller;

import minesweeper.dto.RankingDTO;
import minesweeper.repository.MySqlRankingRepository;
import minesweeper.repository.RankingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RankingController {
    private final RankingRepository rankingRepository;

    public RankingController() {
        this(new MySqlRankingRepository());
    }

    public RankingController(RankingRepository rankingRepository) {
        this.rankingRepository = Objects.requireNonNull(rankingRepository, "rankingRepository");
    }

    public List<RankingDTO> getRanking(int levelId) throws Exception {
        List<RankingDTO> raw = rankingRepository.getLeaderboardByLevel(levelId);
        List<RankingDTO> formatted = new ArrayList<>(raw.size());

        for (RankingDTO row : raw) {
            String playerName = row.getPlayerName() == null ? "Unknown" : row.getPlayerName().trim();
            // P8 fix: preserve rank computed by DENSE_RANK() in SQL — do NOT overwrite with rank++
            formatted.add(new RankingDTO(
                    row.getRank(),
                    playerName,
                    Math.max(0, row.getTotalGames()),
                    Math.max(0, row.getWins()),
                    Math.max(0, row.getBestScore()),
                    Math.max(0L, row.getBestTimeMs())
            ));
        }

        return formatted;
    }

    public List<LevelOption> getAvailableLevels() throws Exception {
        List<RankingRepository.LevelInfo> levels = rankingRepository.getLevels();
        List<LevelOption> options = new ArrayList<>(levels.size());
        for (RankingRepository.LevelInfo level : levels) {
            options.add(new LevelOption(level.getId(), level.getName()));
        }
        return options;
    }

    public static class LevelOption {
        private final int id;
        private final String displayName;

        public LevelOption(int id, String displayName) {
            this.id = id;
            this.displayName = displayName == null || displayName.isBlank() ? "Level " + id : displayName;
        }

        public int getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}

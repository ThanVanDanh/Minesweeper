package minesweeper.controller;

import minesweeper.dto.RankingDTO;
import minesweeper.repository.MySqlRankingRepository;
import minesweeper.repository.RankingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RankingController {
    private static final String EXPERT_LEVEL_NAME = "EXPERT";
    private static final int TOP_RANK_LIMIT = 50;

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

    public List<RankingDTO> getRankingTop(int levelId, int limit) throws Exception {
        if (limit <= 0) {
            return List.of();
        }

        List<RankingDTO> ranking = getRanking(levelId);
        if (ranking.size() <= limit) {
            return ranking;
        }
        return new ArrayList<>(ranking.subList(0, limit));
    }

    public List<RankingDTO> getExpertRankingTop50() throws Exception {
        return getExpertRankingTop(TOP_RANK_LIMIT);
    }

    public List<RankingDTO> getExpertRankingTop(int limit) throws Exception {
        int expertLevelId = findExpertLevelId();
        if (expertLevelId < 0) {
            System.err.println("ERROR: Expert level không tìm được trong DB!");
            return List.of();
        }
        System.out.println("DEBUG: Expert level ID = " + expertLevelId + ", fetching top " + limit);
        List<RankingDTO> result = getRankingTop(expertLevelId, limit);
        System.out.println("DEBUG: Lấy được " + result.size() + " ranking");
        return result;
    }

    public List<LevelOption> getAvailableLevels() throws Exception {
        List<RankingRepository.LevelInfo> levels = rankingRepository.getLevels();
        List<LevelOption> options = new ArrayList<>(levels.size());
        for (RankingRepository.LevelInfo level : levels) {
            if (isExpertLevel(level.getName())) {
                options.add(new LevelOption(level.getId(), level.getName()));
            }
        }
        return options;
    }

    public List<LevelOption> getAllLevels() throws Exception {
        List<RankingRepository.LevelInfo> levels = rankingRepository.getLevels();
        List<LevelOption> options = new ArrayList<>(levels.size());
        for (RankingRepository.LevelInfo level : levels) {
            options.add(new LevelOption(level.getId(), level.getName()));
        }
        return options;
    }

    private int findExpertLevelId() throws Exception {
        List<RankingRepository.LevelInfo> levels = rankingRepository.getLevels();
        for (RankingRepository.LevelInfo level : levels) {
            if (isExpertLevel(level.getName())) {
                return level.getId();
            }
        }
        return -1;
    }

    private boolean isExpertLevel(String levelName) {
        return levelName != null && EXPERT_LEVEL_NAME.equalsIgnoreCase(levelName.trim());
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

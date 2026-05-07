package minesweeper.repository;

import java.util.List;

import minesweeper.dto.RankingDTO;

public interface RankingRepository {
    List<RankingDTO> getLeaderboardByLevel(int levelId) throws Exception;

    List<LevelInfo> getLevels() throws Exception;

    class LevelInfo {
        private final int id;
        private final String name;

        public LevelInfo(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}

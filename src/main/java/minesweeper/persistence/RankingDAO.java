package minesweeper.persistence;

import minesweeper.dto.RankingDTO;

import java.util.List;

public interface RankingDAO {
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


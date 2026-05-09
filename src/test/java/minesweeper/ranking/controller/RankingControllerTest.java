package minesweeper.ranking.controller;

import minesweeper.controller.RankingController;
import minesweeper.dto.RankingDTO;
import minesweeper.repository.RankingRepository;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankingControllerTest {

    @Test
    void getRankingFormatsRowsAndAssignsRank() throws Exception {
        RankingRepository fakeRepository = new RankingRepository() {
            @Override
            public List<RankingDTO> getLeaderboardByLevel(int levelId) {
                return Arrays.asList(
                        new RankingDTO(1, "  Alice  ", 5, 4, 2500, 45000),
                        new RankingDTO(2, null, 3, 1, 1200, -1)
                );
            }

            @Override
            public List<LevelInfo> getLevels() {
                return List.of(new LevelInfo(1, "Easy"));
            }
        };

        RankingController controller = new RankingController(fakeRepository);
        List<RankingDTO> ranking = controller.getRanking(1);

        assertEquals(2, ranking.size());
        assertEquals(1, ranking.get(0).getRank());
        assertEquals("Alice", ranking.get(0).getPlayerName());
        assertEquals(2, ranking.get(1).getRank());
        assertEquals("Unknown", ranking.get(1).getPlayerName());
        assertEquals(0, ranking.get(1).getBestTimeMs());
    }
}



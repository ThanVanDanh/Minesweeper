package minesweeper.controller;

import minesweeper.dto.RankingDTO;
import minesweeper.repository.RankingRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankingControllerTest {

	@Test
	void getAvailableLevelsReturnsOnlyExpertLevels() throws Exception {
		RankingRepository fakeRepository = new RankingRepository() {
			@Override
			public List<RankingDTO> getLeaderboardByLevel(int levelId) {
				return List.of();
			}

			@Override
			public List<LevelInfo> getLevels() {
				return List.of(
						new LevelInfo(1, "Easy"),
						new LevelInfo(4, "EXPERT"),
						new LevelInfo(5, "Custom")
				);
			}
		};

		RankingController controller = new RankingController(fakeRepository);
		List<RankingController.LevelOption> options = controller.getAvailableLevels();

		assertEquals(1, options.size());
		assertEquals(4, options.getFirst().getId());
		assertEquals("EXPERT", options.getFirst().getDisplayName());
	}

	@Test
	void getExpertRankingTop50LimitsTheListToFiftyRows() throws Exception {
		RankingRepository fakeRepository = new RankingRepository() {
			@Override
			public List<RankingDTO> getLeaderboardByLevel(int levelId) {
				List<RankingDTO> rows = new ArrayList<>();
				for (int i = 1; i <= 60; i++) {
					rows.add(new RankingDTO(i, "Player" + i, 10 + i, 5 + i, 1000 + i, 10000L + i));
				}
				return rows;
			}

			@Override
			public List<LevelInfo> getLevels() {
				return List.of(new LevelInfo(4, "Expert"));
			}
		};

		RankingController controller = new RankingController(fakeRepository);
		List<RankingDTO> ranking = controller.getExpertRankingTop50();

		assertEquals(50, ranking.size());
		assertEquals(1, ranking.getFirst().getRank());
		assertEquals(50, ranking.getLast().getRank());
		assertEquals("Player1", ranking.getFirst().getPlayerName());
		assertEquals("Player50", ranking.getLast().getPlayerName());
	}
}



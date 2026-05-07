package minesweeper.ranking.service;

import minesweeper.model.Difficulty;
import minesweeper.model.GameResult;
import minesweeper.repository.GameResultRepository;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.service.RankingManager;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
class RankingManagerRepositoryTest {
    @Test
    void rankingManagerAggregatesResultsFromRepository() throws Exception {
        InMemoryGameResultRepository repository = new InMemoryGameResultRepository();
        repository.saveGameResult(sampleResult("Alice", Difficulty.EASY, true, 60_000));
        repository.saveGameResult(sampleResult("Bob", Difficulty.MEDIUM, true, 180_000));
        repository.saveGameResult(sampleResult("Alice", Difficulty.HARD, false, 200_000));
        repository.saveGameResult(sampleResult("Alice", Difficulty.EASY, true, 45_000));
        RankingManager manager = new RankingManager(repository);
        List<RankingManager.PlayerRanking> rankings = manager.getGlobalRanking();
        assertEquals(2, rankings.size());
        assertEquals("Alice", rankings.get(0).getPlayerName());
        assertEquals(2, rankings.get(0).getWins());
        assertEquals(1, rankings.get(1).getWins());
        List<GameResult> aliceHistory = repository.getPlayerHistory("alice");
        assertEquals(3, aliceHistory.size());
        assertEquals("Alice", aliceHistory.get(0).getPlayerName());
    }
    private GameResult sampleResult(String playerName, Difficulty difficulty, boolean isWon, long elapsedTimeMs) {
        return new GameResult(
                UUID.randomUUID().toString(),
                playerName,
                difficulty,
                isWon,
                elapsedTimeMs,
                3,
                10,
                LocalDateTime.now()
        );
    }
    private static final class InMemoryGameResultRepository implements GameResultRepository {
        private final List<GameResult> results = new ArrayList<>();
        @Override
        public void saveGameResult(GameResult result) {
            results.add(result);
        }
        @Override
        public List<GameResult> getPlayerHistory(String playerName) {
            List<GameResult> filtered = new ArrayList<>();
            for (GameResult result : results) {
                if (result.getPlayerName() != null && result.getPlayerName().equalsIgnoreCase(playerName)) {
                    filtered.add(result);
                }
            }
            return filtered;
        }
        @Override
        public List<GameResult> getAllResults() {
            return new ArrayList<>(results);
        }
        @Override
        public void clearAllResults() {
            results.clear();
        }
        @Override
        public void deleteByGameIds(List<String> gameIds) throws DataAccessException {}
    }
}


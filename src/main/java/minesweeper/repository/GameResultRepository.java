package minesweeper.repository;

import minesweeper.model.GameResult;
import minesweeper.repository.exception.DataAccessException;

import java.util.List;

/**
 * Repository contract for saving and loading game results.
 *
 * This replaces the old ScoreStorage-based flow.
 */
public interface GameResultRepository {
    void saveGameResult(GameResult result) throws DataAccessException;

    List<GameResult> getPlayerHistory(String playerName) throws DataAccessException;

    List<GameResult> getAllResults() throws DataAccessException;

    void clearAllResults() throws DataAccessException;

    void deleteByGameIds(List<String> gameIds) throws DataAccessException;
}


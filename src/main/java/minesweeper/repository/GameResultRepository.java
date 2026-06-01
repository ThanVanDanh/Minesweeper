package minesweeper.repository;

import minesweeper.model.GameResult;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.repository.pagination.PagedResult;
import minesweeper.repository.spec.GameResultFilterSpec;

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

    PagedResult<GameResult> findPaged(GameResultFilterSpec spec, int pageNumber, int pageSize)
            throws DataAccessException;

    long count(GameResultFilterSpec spec) throws DataAccessException;
}


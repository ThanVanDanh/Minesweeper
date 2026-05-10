package minesweeper.service;

import minesweeper.model.enums.Difficulty;
import minesweeper.repository.exception.DataAccessException;

/**
 * Service interface for game level management
 */
public interface LevelService {
    /**
     * Gets level ID by difficulty
     * @param difficulty the game difficulty
     * @return level ID, or null if not found
     * @throws DataAccessException if database error occurs
     */
    Integer getLevelIdByDifficulty(Difficulty difficulty) throws DataAccessException;
    
    /**
     * Gets difficulty by level ID
     * @param levelId the level ID
     * @return the difficulty, or null if not found
     * @throws DataAccessException if database error occurs
     */
    Difficulty getDifficultyByLevelId(int levelId) throws DataAccessException;
}

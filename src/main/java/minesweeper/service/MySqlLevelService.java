package minesweeper.service;

import minesweeper.model.Difficulty;
import minesweeper.repository.connection.ConnectionFactory;
import minesweeper.repository.connection.HikariConnectionFactory;
import minesweeper.repository.config.MySqlConnectionConfig;
import minesweeper.repository.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * MySQL implementation of LevelService
 */
public class MySqlLevelService implements LevelService {
    private static final Logger LOG = LoggerFactory.getLogger(MySqlLevelService.class);
    
    private static final String SELECT_LEVEL_BY_NAME_SQL = "SELECT id, level_name FROM game_levels WHERE LOWER(level_name) = LOWER(?) LIMIT 1";
    private static final String SELECT_LEVEL_BY_ID_SQL = "SELECT id, level_name FROM game_levels WHERE id = ? LIMIT 1";
    
    private final ConnectionFactory connectionFactory;
    private final Map<String, Integer> levelCache = new HashMap<>();
    
    public MySqlLevelService() {
        this(MySqlConnectionConfig.fromResources());
    }
    
    public MySqlLevelService(MySqlConnectionConfig config) {
        Objects.requireNonNull(config, "config");
        this.connectionFactory = new HikariConnectionFactory(config);
    }
    
    public MySqlLevelService(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
    }
    
    @Override
    public Integer getLevelIdByDifficulty(Difficulty difficulty) throws DataAccessException {
        if (difficulty == null) {
            return null;
        }
        
        String diffName = difficulty.name();
        
        // Check cache first
        if (levelCache.containsKey(diffName)) {
            Integer cached = levelCache.get(diffName);
            LOG.debug("Level cache hit for: {}", diffName);
            return cached;
        }
        
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_LEVEL_BY_NAME_SQL)) {
            ps.setString(1, diffName);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Integer levelId = rs.getInt("id");
                    levelCache.put(diffName, levelId);
                    LOG.debug("Level found for difficulty {}: id={}", diffName, levelId);
                    return levelId;
                }
            }
            
            LOG.debug("Level not found for difficulty: {}", diffName);
            levelCache.put(diffName, null);
            return null;
        } catch (SQLException e) {
            LOG.error("Error fetching level for difficulty: {}", diffName, e);
            throw new DataAccessException("Failed to fetch level by difficulty", e);
        }
    }
    
    @Override
    public Difficulty getDifficultyByLevelId(int levelId) throws DataAccessException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_LEVEL_BY_ID_SQL)) {
            ps.setInt(1, levelId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String levelName = rs.getString("level_name");
                    try {
                        Difficulty difficulty = Difficulty.valueOf(levelName.trim().toUpperCase());
                        LOG.debug("Difficulty found for level {}: {}", levelId, difficulty);
                        return difficulty;
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Invalid difficulty name for level {}: {}", levelId, levelName);
                        return null;
                    }
                }
            }
            
            LOG.debug("Level not found with id: {}", levelId);
            return null;
        } catch (SQLException e) {
            LOG.error("Error fetching difficulty for level id: {}", levelId, e);
            throw new DataAccessException("Failed to fetch difficulty by level id", e);
        }
    }
    
    public void clearCache() {
        levelCache.clear();
        LOG.debug("Level cache cleared");
    }
    
    public void close() {
        connectionFactory.close();
        LOG.info("MySqlLevelService closed");
    }
}

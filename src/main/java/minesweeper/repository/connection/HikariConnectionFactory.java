package minesweeper.repository.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import minesweeper.repository.config.MySqlConnectionConfig;
import minesweeper.repository.exception.DatabaseConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class HikariConnectionFactory implements ConnectionFactory {
    private static final Logger logger = LoggerFactory.getLogger(HikariConnectionFactory.class);
    private final HikariDataSource dataSource;

    public HikariConnectionFactory(MySqlConnectionConfig config) {
        this.dataSource = createDataSource(config);
    }

    private HikariDataSource createDataSource(MySqlConnectionConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setUsername(config.getUser());
        hikariConfig.setPassword(config.getPassword());
        
        // Pool settings
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setConnectionTimeout(30000);  // 30 seconds
        hikariConfig.setIdleTimeout(600000);       // 10 minutes
        hikariConfig.setMaxLifetime(1800000);      // 30 minutes
        hikariConfig.setAutoCommit(true);
        
        hikariConfig.setPoolName("MinesweeperPool");
        hikariConfig.setLeakDetectionThreshold(15000);  // 15 seconds leak detection
        
        logger.info("Creating HikariCP connection pool: {}", config.getUrl());
        return new HikariDataSource(hikariConfig);
    }

    @Override
    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            logger.error("Failed to get connection from pool", e);
            throw new DatabaseConnectionException("Failed to get database connection", e);
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing HikariCP connection pool");
            dataSource.close();
        }
    }

    @Override
    public boolean isAlive() {
        try {
            if (dataSource == null || dataSource.isClosed()) {
                return false;
            }
            try (Connection connection = dataSource.getConnection()) {
                return connection.isValid(2);
            }
        } catch (SQLException e) {
            logger.warn("Connection pool health check failed", e);
            return false;
        }
    }
}

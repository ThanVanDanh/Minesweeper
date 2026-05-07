package minesweeper.repository.connection;

import java.sql.Connection;

public interface ConnectionFactory {
    /**
     * Get a connection from the pool
     * @return A database connection
     */
    Connection getConnection();

    /**
     * Close all connections in the pool
     */
    void close();

    /**
     * Check if connection pool is alive
     */
    boolean isAlive();
}

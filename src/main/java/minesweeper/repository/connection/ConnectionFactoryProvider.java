package minesweeper.repository.connection;

import minesweeper.repository.config.MySqlConnectionConfig;

/**
 * Singleton provider for the shared HikariCP connection pool.
 * All services and repositories should use this instead of creating
 * their own HikariConnectionFactory instances (P22 fix).
 */
public final class ConnectionFactoryProvider {

    private static volatile ConnectionFactory instance;

    private ConnectionFactoryProvider() {}

    public static ConnectionFactory get() {
        if (instance == null) {
            synchronized (ConnectionFactoryProvider.class) {
                if (instance == null) {
                    instance = new HikariConnectionFactory(MySqlConnectionConfig.fromResources());
                }
            }
        }
        return instance;
    }

    /**
     * Close the shared pool on application shutdown.
     */
    public static void shutdown() {
        if (instance != null) {
            synchronized (ConnectionFactoryProvider.class) {
                if (instance != null) {
                    instance.close();
                    instance = null;
                }
            }
        }
    }
}

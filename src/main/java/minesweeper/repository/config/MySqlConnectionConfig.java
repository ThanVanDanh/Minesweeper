package minesweeper.repository.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public final class MySqlConnectionConfig {
    private static final String RESOURCE_NAME = "/mysql.properties";
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/minesweeper?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "22072005";

    private final String url;
    private final String user;
    private final String password;

    private MySqlConnectionConfig(String url, String user, String password) {
        this.url = Objects.requireNonNull(url, "url");
        this.user = Objects.requireNonNull(user, "user");
        this.password = password == null ? "" : password;
    }

    public static MySqlConnectionConfig fromResources() {
        Properties properties = new Properties();
        try (InputStream inputStream = MySqlConnectionConfig.class.getResourceAsStream(RESOURCE_NAME)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException ignored) {
            // Fall back to defaults below.
        }

        return new MySqlConnectionConfig(
                override("minesweeper.db.url", "MINE_SWEEPER_DB_URL", properties.getProperty("mysql.db.url", DEFAULT_URL)),
                override("minesweeper.db.user", "MINE_SWEEPER_DB_USER", properties.getProperty("mysql.db.user", DEFAULT_USER)),
                override("minesweeper.db.password", "MINE_SWEEPER_DB_PASSWORD", properties.getProperty("mysql.db.password", DEFAULT_PASSWORD))
        );
    }

    private static String override(String systemProperty, String envVar, String fallback) {
        String systemValue = System.getProperty(systemProperty);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }

        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        return fallback;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
}

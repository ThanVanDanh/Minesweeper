package minesweeper.service;

import minesweeper.model.Role;
import minesweeper.model.User;
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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * MySQL implementation of UserService
 */
public class MySqlUserService implements UserService {
    private static final Logger LOG = LoggerFactory.getLogger(MySqlUserService.class);

    private static final String SELECT_USER_BY_ID_SQL = "SELECT id, username, display_name FROM users WHERE id = ? LIMIT 1";
    private static final String SELECT_USER_BY_USERNAME_SQL = "SELECT id, username, display_name FROM users WHERE username = ? LIMIT 1";
    private static final String INSERT_USER_SQL = "INSERT INTO users (username, display_name) VALUES (?, ?)";
    private static final String INSERT_USER_SQL_F = "INSERT INTO users (username, display_name, role) VALUES (?, ?, ?)";
    private static final String SELECT_ALL_SQL =
            "SELECT id, username, display_name, role, is_active FROM users ORDER BY id";

    private static final String UPDATE_DISPLAY_NAME_SQL = "UPDATE users SET display_name = ? WHERE id = ?";
    private static final String UPDATE_ACTIVE_SQL = "UPDATE users SET is_active = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM users WHERE id = ?";
    private static final String UPDATE_ROLE_SQL = "UPDATE users SET role = ? WHERE id = ?";

    private final ConnectionFactory connectionFactory;

    public MySqlUserService() {
        this(MySqlConnectionConfig.fromResources());
    }

    public MySqlUserService(MySqlConnectionConfig config) {
        Objects.requireNonNull(config, "config");
        this.connectionFactory = new HikariConnectionFactory(config);
    }

    public MySqlUserService(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
    }

    @Override
    public long getOrCreateUser(String username) throws DataAccessException {
        validateUsername(username);
        String trimmed = username.trim();

        try {
            User existing = getUserByUsername(trimmed);
            if (existing != null) {
                LOG.debug("User found: {} (id={})", trimmed, existing.getId());
                return existing.getId();
            }

            // Create new user if not found
            return createUser(trimmed, trimmed);
        } catch (DataAccessException e) {
            LOG.error("Error getting or creating user: {}", username, e);
            throw e;
        }
    }

    @Override
    public User getUserById(long userId) throws DataAccessException {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_USER_BY_ID_SQL)) {
            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }

            LOG.debug("User not found with id: {}", userId);
            return null;
        } catch (SQLException e) {
            LOG.error("Error fetching user by id: {}", userId, e);
            throw new DataAccessException("Failed to fetch user by id", e);
        }
    }

    @Override
    public User getUserByUsername(String username) throws DataAccessException {
        if (username == null || username.isBlank()) {
            return null;
        }

        String trimmed = username.trim();
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_USER_BY_USERNAME_SQL)) {
            ps.setString(1, trimmed);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapUser(rs);
                    LOG.debug("User found: {} (id={})", trimmed, user.getId());
                    return user;
                }
            }

            LOG.debug("User not found: {}", trimmed);
            return null;
        } catch (SQLException e) {
            LOG.error("Error fetching user by username: {}", username, e);
            throw new DataAccessException("Failed to fetch user by username", e);
        }
    }

    @Override
    public long createUser(String username, String displayName) throws DataAccessException {
        validateUsername(username);

        String trimmedUsername = username.trim();
        String trimmedDisplayName = displayName != null ? displayName.trim() : trimmedUsername;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement ps = connection.prepareStatement(INSERT_USER_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, trimmedUsername);
            ps.setString(2, trimmedDisplayName);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long userId = keys.getLong(1);
                    LOG.info("User created: {} (id={})", trimmedUsername, userId);
                    return userId;
                }
            }

            throw new DataAccessException("Failed to create user: " + username);
        } catch (SQLException e) {
            LOG.error("Error creating user: {}", username, e);
            throw new DataAccessException("Failed to create user", e);
        }
    }

    @Override
    public long createUserFull(String username, String displayName, Role role) throws DataAccessException {
        validateUsername(username);

        String trimmedUsername = username.trim();
        String trimmedDisplayName = displayName != null ? displayName.trim() : trimmedUsername;
        String roleStr = (role != null ? role.name() : Role.PLAYER.name());

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement ps = connection.prepareStatement(INSERT_USER_SQL_F, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, trimmedUsername);
            ps.setString(2, trimmedDisplayName);
            ps.setString(3, roleStr);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long userId = keys.getLong(1);
                    LOG.info("User created: {} (id={})", trimmedUsername, userId);
                    return userId;
                }
            }

            throw new DataAccessException("Failed to create user: " + username);
        } catch (SQLException e) {
            LOG.error("Error creating user: {}", username, e);
            throw new DataAccessException("Failed to create user", e);
        }
    }

    @Override
    public List<User> getAllUsers() throws DataAccessException {
        List<User> users = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) users.add(mapUser(rs));
            return users;

        } catch (SQLException e) {
            LOG.error("Error fetching all users", e);
            throw new DataAccessException("Failed to fetch all users", e);
        }
    }

    // ── Admin: Update / Lock / Delete ─────────────────────────────────────────

    @Override
    public void updateDisplayName(int userId, String newDisplayName) throws DataAccessException {
        if (newDisplayName == null || newDisplayName.isBlank())
            throw new DataAccessException("Display name cannot be blank");

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_DISPLAY_NAME_SQL)) {
            ps.setString(1, newDisplayName.trim());
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DataAccessException("User not found: id=" + userId);
            LOG.info("Updated display name for user id={}", userId);
        } catch (SQLException e) {
            LOG.error("Error updating display name for user id={}", userId, e);
            throw new DataAccessException("Failed to update display name", e);
        }
    }

    @Override
    public void setActive(int userId, boolean active) throws DataAccessException {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_ACTIVE_SQL)) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DataAccessException("User not found: id=" + userId);
            LOG.info("Set active={} for user id={}", active, userId);
        } catch (SQLException e) {
            LOG.error("Error updating is_active for user id={}", userId, e);
            throw new DataAccessException("Failed to update user status", e);
        }
    }

    @Override
    public void deleteUser(int userId) throws DataAccessException {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            ps.setInt(1, userId);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DataAccessException("User not found: id=" + userId);
            LOG.info("Deleted user id={}", userId);
        } catch (SQLException e) {
            LOG.error("Error deleting user id={}", userId, e);
            throw new DataAccessException("Failed to delete user", e);
        }
    }

    @Override
    public void updateRole(int userId, Role newRole) throws DataAccessException {
        if (newRole == null) return;

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_ROLE_SQL)) {
            ps.setString(1, newRole.name()); // Lưu tên Enum (ADMIN/PLAYER)
            ps.setInt(2, userId);

            int rows = ps.executeUpdate();
            if (rows == 0) throw new DataAccessException("User not found: id=" + userId);
            LOG.info("Updated role to {} for user id={}", newRole, userId);
        } catch (SQLException e) {
            LOG.error("Error updating role for user id={}", userId, e);
            throw new DataAccessException("Failed to update role", e);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId((int) rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setDisplayName(rs.getString("display_name"));
        user.setActive(rs.getBoolean("is_active"));
        String role = rs.getString("role");
        if (role != null) {
            user.setRole(Role.valueOf(role));
        }



        return user;
    }

    private void validateUsername(String username) throws DataAccessException {
        if (username == null || username.isBlank()) {
            throw new DataAccessException("Username cannot be null or blank");
        }
    }

    public void close() {
        connectionFactory.close();
        LOG.info("MySqlUserService closed");
    }
}

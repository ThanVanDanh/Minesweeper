package minesweeper.repository;

import minesweeper.model.User;
import minesweeper.model.enums.Role;
import minesweeper.repository.connection.ConnectionFactory;
import minesweeper.repository.connection.ConnectionFactoryProvider;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.repository.pagination.Page;
import minesweeper.repository.pagination.PagedResult;
import minesweeper.repository.spec.UserFilterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MySqlUserRepository implements UserRepository {

    private static final Logger LOG = LoggerFactory.getLogger(MySqlUserRepository.class);

    // ── SQL constants ──────────────────────────────────────────────────────────

    private static final String BASE_SELECT =
            "SELECT id, username, display_name, role, is_active FROM users";

    private static final String FIND_BY_ID =
            BASE_SELECT + " WHERE id = ? LIMIT 1";

    private static final String FIND_BY_USERNAME =
            BASE_SELECT + " WHERE username = ? LIMIT 1";

    private static final String FIND_ALL =
            BASE_SELECT + " ORDER BY id";

    private static final String INSERT =
            "INSERT INTO users (username, display_name, role, password_hash) VALUES (?, ?, ?, ?)";

    private static final String UPDATE_DISPLAY_NAME =
            "UPDATE users SET display_name = ? WHERE id = ?";

    private static final String UPDATE_ROLE =
            "UPDATE users SET role = ? WHERE id = ?";

    private static final String UPDATE_ACTIVE =
            "UPDATE users SET is_active = ? WHERE id = ?";

    private static final String DELETE_SESSIONS =
            "DELETE FROM game_sessions WHERE user_id = ?";

    private static final String DELETE_USER =
            "DELETE FROM users WHERE id = ?";

    // ── Constructor ────────────────────────────────────────────────────────────

    private final ConnectionFactory connectionFactory;

    public MySqlUserRepository() {
        this(ConnectionFactoryProvider.get());
    }

    public MySqlUserRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
    }

    // ── Read ───────────────────────────────────────────────────────────────────

    @Override
    public User findById(long id) throws DataAccessException {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find user by id=" + id, e);
        }
    }

    @Override
    public User findByUsername(String username) throws DataAccessException {
        if (username == null || username.isBlank()) return null;
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_USERNAME)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find user by username=" + username, e);
        }
    }

    @Override
    public List<User> findAll() throws DataAccessException {
        List<User> users = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) users.add(mapUser(rs));
            return users;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch all users", e);
        }
    }

    @Override
    public PagedResult<User> findPaged(UserFilterSpec spec, int pageNumber, int pageSize)
            throws DataAccessException {

        WhereClause where = buildWhere(spec);

        long total = executeCount(where);

        Page page = new Page(pageNumber, pageSize, total);

        String dataSql = BASE_SELECT + where.sql + " ORDER BY id" + page.getLimitClause();
        List<User> users = new ArrayList<>();

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(dataSql)) {
            setParams(ps, where.params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch paged users", e);
        }

        return new PagedResult<>(users, page);
    }

    @Override
    public long count(UserFilterSpec spec) throws DataAccessException {
        return executeCount(buildWhere(spec));
    }

    // ── Write ──────────────────────────────────────────────────────────────────

    @Override
    public long save(String username, String displayName, Role role, String passwordHash)
            throws DataAccessException {
        String trimUser = username.trim();
        String trimName = (displayName != null && !displayName.isBlank())
                ? displayName.trim() : trimUser;
        String roleStr  = (role != null ? role : Role.PLAYER).name();

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, trimUser);
            ps.setString(2, trimName);
            ps.setString(3, roleStr);
            ps.setString(4, passwordHash != null ? passwordHash : "");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    LOG.info("User created: {} (id={})", trimUser, id);
                    return id;
                }
            }
            throw new DataAccessException("Insert returned no generated key");
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save user: " + username, e);
        }
    }

    @Override
    public void updateDisplayName(long id, String displayName) throws DataAccessException {
        if (displayName == null || displayName.isBlank())
            throw new DataAccessException("Display name cannot be blank");
        executeUpdate(UPDATE_DISPLAY_NAME, id, displayName.trim());
    }

    @Override
    public void updateRole(long id, Role role) throws DataAccessException {
        if (role == null) return;
        executeUpdate(UPDATE_ROLE, id, role.name());
    }

    @Override
    public void setActive(long id, boolean active) throws DataAccessException {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_ACTIVE)) {
            ps.setBoolean(1, active);
            ps.setLong(2, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DataAccessException("User not found: id=" + id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to set active for id=" + id, e);
        }
    }

    @Override
    public void delete(long id) throws DataAccessException {
        try (Connection conn = connectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps1 = conn.prepareStatement(DELETE_SESSIONS)) {
                    ps1.setLong(1, id);
                    ps1.executeUpdate();
                }
                try (PreparedStatement ps2 = conn.prepareStatement(DELETE_USER)) {
                    ps2.setLong(1, id);
                    int rows = ps2.executeUpdate();
                    if (rows == 0) throw new DataAccessException("User not found: id=" + id);
                }
                conn.commit();
                LOG.info("Deleted user id={} and their sessions", id);
            } catch (Exception e) {
                conn.rollback();
                throw (e instanceof DataAccessException de) ? de
                        : new DataAccessException("Failed to delete user id=" + id, e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Connection error while deleting user id=" + id, e);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Build mệnh đề WHERE động từ spec.
     * Trường nào null → bỏ qua điều kiện đó.
     */
    private WhereClause buildWhere(UserFilterSpec spec) {
        StringBuilder sql    = new StringBuilder(" WHERE 1=1");
        List<Object>  params = new ArrayList<>();

        if (spec.keyword != null && !spec.keyword.isBlank()) {
            sql.append(" AND (username LIKE ? OR display_name LIKE ?)");
            String like = "%" + spec.keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (spec.role != null) {
            sql.append(" AND role = ?");
            params.add(spec.role.name());
        }
        if (spec.active != null) {
            sql.append(" AND is_active = ?");
            params.add(spec.active ? 1 : 0);
        }

        return new WhereClause(sql.toString(), params);
    }

    private long executeCount(WhereClause where) throws DataAccessException {
        String countSql = "SELECT COUNT(*) FROM users" + where.sql;
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            setParams(ps, where.params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count users", e);
        }
    }

    /**
     * Helper cho UPDATE đơn giản: UPDATE ... SET col=? WHERE id=?
     * param1 = giá trị mới, param2 = id
     */
    private void executeUpdate(String sql, long id, Object value) throws DataAccessException {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, value);
            ps.setLong(2, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new DataAccessException("User not found: id=" + id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute update for id=" + id, e);
        }
    }

    private void setParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setDisplayName(rs.getString("display_name"));
        user.setActive(rs.getBoolean("is_active"));
        String roleStr = rs.getString("role");
        if (roleStr != null) user.setRole(Role.valueOf(roleStr));
        return user;
    }

    // ── Inner class ────────────────────────────────────────────────────────────

    /** Giữ chuỗi WHERE và danh sách params tương ứng. */
    private record WhereClause(String sql, List<Object> params) {}
}
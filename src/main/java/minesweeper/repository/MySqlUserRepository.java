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

    private static final String BASE_SELECT = """
            SELECT u.id, u.username, u.display_name, u.email, u.role, u.is_active,
                   u.created_at, u.last_login_at, COUNT(gs.id) AS game_count
            FROM users u
            LEFT JOIN game_sessions gs ON gs.user_id = u.id
            """;

    private static final String GROUP_BY_USER = " GROUP BY u.id";

    private static final String FIND_BY_ID =
            BASE_SELECT + " WHERE u.id = ?" + GROUP_BY_USER + " LIMIT 1";

    private static final String FIND_BY_USERNAME =
            BASE_SELECT + " WHERE u.username = ?" + GROUP_BY_USER + " LIMIT 1";

    private static final String FIND_ALL =
            BASE_SELECT + GROUP_BY_USER + " ORDER BY u.id";

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

        String dataSql = BASE_SELECT + where.whereSql + GROUP_BY_USER
                + where.havingSql + " ORDER BY u.id" + page.getLimitClause();
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
        StringBuilder whereSql  = new StringBuilder(" WHERE 1=1");
        StringBuilder havingSql = new StringBuilder();
        List<Object>  params    = new ArrayList<>();

        if (spec.keyword != null && !spec.keyword.isBlank()) {
            whereSql.append(" AND (u.username LIKE ? OR u.display_name LIKE ?)");
            String like = "%" + spec.keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (spec.email != null && !spec.email.isBlank()) {
            whereSql.append(" AND u.email LIKE ?");
            params.add("%" + spec.email.trim() + "%");
        }
        if (spec.role != null) {
            whereSql.append(" AND u.role = ?");
            params.add(spec.role.name());
        }
        if (spec.active != null) {
            whereSql.append(" AND u.is_active = ?");
            params.add(spec.active ? 1 : 0);
        }
        if (spec.createdFrom != null) {
            whereSql.append(" AND u.created_at >= ?");
            params.add(Timestamp.valueOf(spec.createdFrom.atStartOfDay()));
        }
        if (spec.createdTo != null) {
            whereSql.append(" AND u.created_at < ?");
            params.add(Timestamp.valueOf(spec.createdTo.plusDays(1).atStartOfDay()));
        }
        if (spec.lastLoginFrom != null) {
            whereSql.append(" AND u.last_login_at >= ?");
            params.add(Timestamp.valueOf(spec.lastLoginFrom.atStartOfDay()));
        }
        if (spec.lastLoginTo != null) {
            whereSql.append(" AND u.last_login_at < ?");
            params.add(Timestamp.valueOf(spec.lastLoginTo.plusDays(1).atStartOfDay()));
        }

        List<String> havingParts = new ArrayList<>();
        if (spec.minGames != null) {
            havingParts.add("COUNT(gs.id) >= ?");
            params.add(spec.minGames);
        }
        if (spec.maxGames != null) {
            havingParts.add("COUNT(gs.id) <= ?");
            params.add(spec.maxGames);
        }
        if (spec.hasGameResults != null) {
            havingParts.add(spec.hasGameResults ? "COUNT(gs.id) > 0" : "COUNT(gs.id) = 0");
        }
        if (!havingParts.isEmpty()) {
            havingSql.append(" HAVING ").append(String.join(" AND ", havingParts));
        }

        return new WhereClause(whereSql.toString(), havingSql.toString(), params);
    }

    private long executeCount(WhereClause where) throws DataAccessException {
        String countSql = "SELECT COUNT(*) FROM ("
                + "SELECT u.id FROM users u LEFT JOIN game_sessions gs ON gs.user_id = u.id"
                + where.whereSql + GROUP_BY_USER + where.havingSql
                + ") filtered_users";
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
        user.setEmail(rs.getString("email"));
        user.setActive(rs.getBoolean("is_active"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) user.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp lastLoginAt = rs.getTimestamp("last_login_at");
        if (lastLoginAt != null) user.setLastLoginAt(lastLoginAt.toLocalDateTime());
        user.setGameCount(rs.getInt("game_count"));
        String roleStr = rs.getString("role");
        if (roleStr != null) user.setRole(Role.valueOf(roleStr));
        return user;
    }

    // ── Inner class ────────────────────────────────────────────────────────────

    /** Giữ chuỗi WHERE và danh sách params tương ứng. */
    private record WhereClause(String whereSql, String havingSql, List<Object> params) {}
}

package minesweeper.repository.log;

import minesweeper.model.AuditLog;
import minesweeper.repository.connection.ConnectionFactory;
import minesweeper.repository.connection.ConnectionFactoryProvider;
import minesweeper.repository.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MySqlAuditLogRepository implements AuditLogRepository {
    private static final Logger LOG = LoggerFactory.getLogger(MySqlAuditLogRepository.class);
    private static final String INSERT_SQL = "INSERT INTO audit_log (admin_id, action, target, details) VALUES (?, ?, ?, ?)";
    private static final String SELECT_RECENT = "SELECT id, admin_id, action, target, details, created_at FROM audit_log ORDER BY created_at DESC LIMIT ?";

    private final ConnectionFactory connectionFactory;

    public MySqlAuditLogRepository() {
        this(ConnectionFactoryProvider.get());
    }
    public MySqlAuditLogRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void insert(AuditLog log) throws DataAccessException {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            if (log.getAdminId() == null) ps.setNull(1, java.sql.Types.BIGINT);
            else ps.setLong(1, log.getAdminId());
            ps.setString(2, log.getAction());
            ps.setString(3, log.getTarget());
            ps.setString(4, log.getDetails());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Error inserting audit_log", e);
            throw new DataAccessException("Failed to insert audit log", e);
        }
    }

    @Override
    public List<AuditLog> findRecent(int limit) throws DataAccessException {
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_RECENT)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditLog a = new AuditLog();
                    a.setId(rs.getLong("id"));
                    long aid = rs.getLong("admin_id");
                    a.setAdminId(rs.wasNull() ? null : aid);
                    a.setAction(rs.getString("action"));
                    a.setTarget(rs.getString("target"));
                    a.setDetails(rs.getString("details"));
                    java.sql.Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) a.setCreatedAt(ts.toLocalDateTime());
                    list.add(a);
                }
            }
            return list;
        } catch (SQLException e) {
            LOG.error("Error reading audit_log", e);
            throw new DataAccessException("Failed to read audit log", e);
        }
    }
}

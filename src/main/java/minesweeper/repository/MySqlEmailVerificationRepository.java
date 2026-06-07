package minesweeper.repository;

import minesweeper.model.EmailVerificationToken;
import minesweeper.repository.connection.ConnectionFactory;
import minesweeper.repository.connection.ConnectionFactoryProvider;
import minesweeper.repository.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Objects;

public class MySqlEmailVerificationRepository implements EmailVerificationRepository {

    private static final Logger LOG = LoggerFactory.getLogger(MySqlEmailVerificationRepository.class);

    private static final String DELETE_OLD =
            "DELETE FROM email_verification_tokens WHERE user_id = ?";

    private static final String INSERT =
            "INSERT INTO email_verification_tokens (user_id, email, otp_hash, expires_at) VALUES (?, ?, ?, ?)";

    private static final String FIND_ACTIVE =
            "SELECT id, user_id, email, otp_hash, expires_at, is_used, created_at " +
                    "FROM email_verification_tokens " +
                    "WHERE user_id = ? AND is_used = FALSE " +
                    "ORDER BY created_at DESC LIMIT 1";

    private static final String MARK_USED =
            "UPDATE email_verification_tokens SET is_used = TRUE WHERE id = ?";

    private static final String DELETE_BY_USER =
            "DELETE FROM email_verification_tokens WHERE user_id = ?";

    private final ConnectionFactory connectionFactory;

    public MySqlEmailVerificationRepository() {
        this(ConnectionFactoryProvider.get());
    }

    public MySqlEmailVerificationRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    @Override
    public void save(EmailVerificationToken token) throws DataAccessException {
        try (Connection conn = connectionFactory.getConnection()) {
            try (PreparedStatement del = conn.prepareStatement(DELETE_OLD)) {
                del.setLong(1, token.getUserId());
                del.executeUpdate();
            }
            try (PreparedStatement ins = conn.prepareStatement(INSERT)) {
                ins.setLong  (1, token.getUserId());
                ins.setString(2, token.getEmail());
                ins.setString(3, token.getOtp());          // caller đã hash trước khi lưu
                ins.setTimestamp(4, Timestamp.valueOf(token.getExpiresAt()));
                ins.executeUpdate();
            }
        } catch (SQLException e) {
            LOG.error("Failed to save email verification token for userId={}", token.getUserId(), e);
            throw new DataAccessException("Failed to save email verification token", e);
        }
    }

    @Override
    public EmailVerificationToken findActiveByUserId(long userId) throws DataAccessException {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_ACTIVE)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        } catch (SQLException e) {
            LOG.error("Failed to find active token for userId={}", userId, e);
            throw new DataAccessException("Failed to find email verification token", e);
        }
    }

    @Override
    public void markUsed(long tokenId) throws DataAccessException {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(MARK_USED)) {
            ps.setLong(1, tokenId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Failed to mark token id={} as used", tokenId, e);
            throw new DataAccessException("Failed to mark token as used", e);
        }
    }

    @Override
    public void deleteByUserId(long userId) throws DataAccessException {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_BY_USER)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Failed to delete tokens for userId={}", userId, e);
            throw new DataAccessException("Failed to delete email verification tokens", e);
        }
    }
    private EmailVerificationToken mapRow(ResultSet rs) throws SQLException {
        EmailVerificationToken t = new EmailVerificationToken();
        t.setId       (rs.getLong     ("id"));
        t.setUserId   (rs.getLong     ("user_id"));
        t.setEmail    (rs.getString   ("email"));
        t.setOtp      (rs.getString   ("otp_hash"));
        t.setUsed     (rs.getBoolean  ("is_used"));
        Timestamp exp = rs.getTimestamp("expires_at");
        if (exp != null) t.setExpiresAt(exp.toLocalDateTime());
        Timestamp cre = rs.getTimestamp("created_at");
        if (cre != null) t.setCreatedAt(cre.toLocalDateTime());
        return t;
    }
}
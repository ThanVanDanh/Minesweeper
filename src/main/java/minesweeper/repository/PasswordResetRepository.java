package minesweeper.repository;

import minesweeper.model.PasswordResetToken;
import minesweeper.repository.connection.ConnectionFactory;
import minesweeper.repository.connection.ConnectionFactoryProvider;
import minesweeper.repository.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Objects;

public class PasswordResetRepository {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetRepository.class);

    private static final String SAVE_TOKEN_SQL =
            "INSERT INTO password_reset_tokens (user_id, token, email, expires_at) VALUES (?, ?, ?, ?)";
    private static final String FIND_TOKEN_SQL =
            "SELECT id, user_id, token, email, expires_at, used FROM password_reset_tokens WHERE token = ? AND used = FALSE";
    private static final String MARK_USED_SQL =
            "UPDATE password_reset_tokens SET used = TRUE WHERE id = ?";
    private static final String DELETE_EXPIRED_SQL =
            "DELETE FROM password_reset_tokens WHERE expires_at < ? OR used = TRUE";

    private final ConnectionFactory connectionFactory;

    public PasswordResetRepository() {
        this(ConnectionFactoryProvider.get());
    }

    public PasswordResetRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
    }

    public void save(PasswordResetToken token) throws DataAccessException {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(SAVE_TOKEN_SQL)) {

            ps.setLong(1, token.getUserId());
            ps.setString(2, token.getToken());
            ps.setString(3, token.getEmail());
            ps.setTimestamp(4, Timestamp.valueOf(token.getExpiresAt()));

            ps.executeUpdate();

            LOG.info("Đã lưu mã đặt lại mật khẩu cho người dùng có ID={}", token.getUserId());

        } catch (SQLException e) {
            LOG.error("Lỗi khi lưu mã đặt lại mật khẩu", e);
            throw new DataAccessException("Lỗi khi lưu mã đặt lại mật khẩu", e);
        }
    }

    public PasswordResetToken findByToken(String token) throws DataAccessException {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_TOKEN_SQL)) {

            ps.setString(1, token);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PasswordResetToken t = new PasswordResetToken();

                    t.setId(rs.getLong("id"));
                    t.setUserId(rs.getLong("user_id"));
                    t.setToken(rs.getString("token"));
                    t.setEmail(rs.getString("email"));
                    t.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
                    t.setUsed(rs.getBoolean("used"));

                    LOG.debug("Đã tìm thấy mã đặt lại mật khẩu hợp lệ");

                    return t;
                }
            }

        } catch (SQLException e) {
            LOG.error("Lỗi khi tìm mã đặt lại mật khẩu", e);
            throw new DataAccessException("Lỗi khi tìm mã đặt lại mật khẩu", e);
        }

        return null;
    }

    public void markUsed(long tokenId) throws DataAccessException {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(MARK_USED_SQL)) {

            ps.setLong(1, tokenId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                LOG.info("Đã đánh dấu mã đặt lại mật khẩu là đã sử dụng: tokenId={}", tokenId);
            }

        } catch (SQLException e) {
            LOG.error("Lỗi khi cập nhật trạng thái sử dụng của mã đặt lại mật khẩu", e);
            throw new DataAccessException("Lỗi khi cập nhật trạng thái sử dụng của mã đặt lại mật khẩu", e);
        }
    }

    public void deleteExpired() throws DataAccessException {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_EXPIRED_SQL)) {

            ps.setTimestamp(1, Timestamp.valueOf(java.time.LocalDateTime.now()));
            int rows = ps.executeUpdate();

            LOG.debug("Đã xóa {} mã đặt lại mật khẩu hết hạn hoặc đã được sử dụng", rows);

        } catch (SQLException e) {
            LOG.error("Lỗi khi xóa các mã đặt lại mật khẩu hết hạn", e);
            throw new DataAccessException("Lỗi khi xóa các mã đặt lại mật khẩu hết hạn", e);
        }
    }
}


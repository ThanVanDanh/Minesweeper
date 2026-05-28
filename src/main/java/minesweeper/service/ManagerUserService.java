package minesweeper.service;

import minesweeper.model.User;
import minesweeper.model.enums.Role;
import minesweeper.repository.UserRepository;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.repository.pagination.PagedResult;
import minesweeper.repository.spec.UserFilterSpec;

import java.util.Objects;

public class ManagerUserService {

    private final UserRepository userRepository;

    public ManagerUserService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Lấy danh sách user phân trang theo điều kiện lọc.
     *
     * @param spec       điều kiện lọc (keyword, role, active). Trường null = không lọc.
     * @param pageNumber trang hiện tại (0-indexed)
     * @param pageSize   số dòng mỗi trang
     */
    public PagedResult<User> findPaged(UserFilterSpec spec, int pageNumber, int pageSize)
            throws DataAccessException {
        UserFilterSpec safeSpec = spec != null ? spec : new UserFilterSpec();
        return userRepository.findPaged(safeSpec, pageNumber, pageSize);
    }

    /**
     * Thống kê tổng quan: tổng user, đang hoạt động, bị khoá, số admin.
     * Dùng 4 lần count thay vì tải toàn bộ danh sách vào bộ nhớ.
     */
    public UserStats getStats() throws DataAccessException {
        long total  = userRepository.count(new UserFilterSpec());
        long active = userRepository.count(UserFilterSpec.withActive(true));
        long locked = userRepository.count(UserFilterSpec.withActive(false));
        long admins = userRepository.count(UserFilterSpec.withRole(Role.ADMIN));
        return new UserStats(total, active, locked, admins);
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Tạo user mới; trả về id được sinh ra.
     */
    public long createUser(String username, String displayName, Role role, String passwordHash)
            throws DataAccessException {
        if (username == null || username.isBlank())
            throw new DataAccessException("Username không được để trống");
        return userRepository.save(username.trim(), displayName, role, passwordHash);
    }

    public void updateDisplayName(long userId, String displayName) throws DataAccessException {
        userRepository.updateDisplayName(userId, displayName);
    }

    public void updateRole(long userId, Role role) throws DataAccessException {
        userRepository.updateRole(userId, role);
    }

    public void setActive(long userId, boolean active) throws DataAccessException {
        userRepository.setActive(userId, active);
    }

    public void deleteUser(long userId) throws DataAccessException {
        userRepository.delete(userId);
    }

    // ── Inner DTO ─────────────────────────────────────────────────────────────

    public record UserStats(long total, long active, long locked, long adminCount) {}
}
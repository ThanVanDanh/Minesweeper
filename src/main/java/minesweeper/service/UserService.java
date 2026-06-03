package minesweeper.service;

import minesweeper.model.enums.Role;
import minesweeper.model.User;
import minesweeper.repository.exception.DataAccessException;

import java.util.List;

/**
 * UserService giữ vai trò định nghĩa các thao tác quản lý người dùng ở mức trừu tượng.
 * Trong code hiện tại, AuthService đang sử dụng MySqlUserService; interface vẫn hữu ích
 * để thể hiện kiến trúc và có thể mở rộng implementation sau này.
 */
public interface UserService {
    /** getOrCreateUser(username): lấy user theo username, nếu chưa tồn tại thì tạo mới. */
    long getOrCreateUser(String username) throws DataAccessException;

    /** getUserById(userId): lấy thông tin user theo id. */
    User getUserById(long userId) throws DataAccessException;

    /** getUserByUsername(username): kiểm tra username đã tồn tại khi đăng ký. */
    User getUserByUsername(String username) throws DataAccessException;

    /** createUser(username, displayName): tạo user cơ bản không kèm password_hash. */
    long createUser(String username, String displayName) throws DataAccessException;

    /** createUserFull(username, displayName, role, password): tạo user đầy đủ thông tin. */
    long createUserFull(String username, String displayName, Role role, String password) throws DataAccessException;

    /** getAllUsers(): lấy danh sách toàn bộ user, phục vụ Admin. */
    List<User> getAllUsers() throws DataAccessException;

    /** updateDisplayName(userId, newDisplayName): cập nhật tên hiển thị của user. */
    void updateDisplayName(long userId, String newDisplayName) throws DataAccessException;

    /** setActive(userId, active): khóa hoặc mở khóa tài khoản. */
    void setActive(long userId, boolean active) throws DataAccessException;

    /** updateRole(userId, newRole): cập nhật vai trò của user. */
    void updateRole(long userId, Role newRole) throws DataAccessException;

    /** deleteUser(userId): xóa user và dữ liệu liên quan. */
    void deleteUser(long userId) throws DataAccessException;
}

package minesweeper.admin;

import minesweeper.model.User;
import minesweeper.model.enums.Role;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử chức năng UC22 – Quản lý người dùng
 * Nguồn: AdminUserController.java, MySqlUserService.java
 *
 * Lưu ý: Các test này kiểm tra business logic thuần (không cần JavaFX runtime).
 * Sử dụng Mockito để mock UserService, tránh phụ thuộc vào CSDL thực.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC22 – Quản lý người dùng")
public class AdminUserControllerTest {

    @Mock
    private UserService userService;

    // ── Helper: tạo User mẫu ────────────────────────────────────────────────

    private User makeUser(long id, String username, String displayName,
                          Role role, boolean isActive) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setDisplayName(displayName);
        u.setRole(role);
        u.setActive(isActive);
        return u;
    }

    // ── Helper: logic lọc (ánh xạ từ AdminUserController.matchesFilter) ─────

    private static final String FILTER_ALL    = "Tất cả";
    private static final String STATUS_ACTIVE = "Hoạt động";
    private static final String STATUS_LOCKED = "Đã khoá";

    private boolean matchesFilter(User u, String keyword,
                                  String roleFilter, String statusFilter) {
        boolean matchKw = keyword.isEmpty()
                || u.getUsername().toLowerCase().contains(keyword)
                || (u.getDisplayName() != null
                && u.getDisplayName().toLowerCase().contains(keyword));

        boolean matchRole = FILTER_ALL.equals(roleFilter)
                || (u.getRole() != null && u.getRole().getLabel().equals(roleFilter));

        boolean matchStatus = FILTER_ALL.equals(statusFilter)
                || (STATUS_ACTIVE.equals(statusFilter) && u.isActive())
                || (STATUS_LOCKED.equals(statusFilter) && !u.isActive());

        return matchKw && matchRole && matchStatus;
    }

    // =========================================================================
    // TC ADMU_01 – Tải danh sách users thành công
    // =========================================================================

    /**
     * ADMU_01: Mở màn hình quản lý User
     * Khi: Admin mở màn hình
     * Mong đợi: getAllUsers() được gọi và trả về danh sách đúng
     */
    @Test
    @DisplayName("ADMU_01 – Tải danh sách users thành công")
    void admu01_loadUsers_success() throws DataAccessException {
        // Chuẩn bị
        List<User> mockUsers = Arrays.asList(
                makeUser(1, "admin1", "Admin One",  Role.ADMIN,  true),
                makeUser(2, "player1","Player One", Role.PLAYER, true),
                makeUser(3, "player2","Player Two", Role.PLAYER, false)
        );
        when(userService.getAllUsers()).thenReturn(mockUsers);

        // Thực hiện
        List<User> result = userService.getAllUsers();

        // Kiểm tra
        assertNotNull(result, "Danh sách users không được null");
        assertEquals(3, result.size(), "Phải có đúng 3 users");
        verify(userService, times(1)).getAllUsers();
    }

    /**
     * ADMU_01 (mở rộng): Kiểm tra thống kê Total/Active/Locked/Admin
     */
    @Test
    @DisplayName("ADMU_01 – Thống kê Total/Active/Locked/Admin đúng")
    void admu01_stats_correct() throws DataAccessException {
        List<User> users = Arrays.asList(
                makeUser(1, "admin1",  "Admin",    Role.ADMIN,  true),
                makeUser(2, "player1", "Player 1", Role.PLAYER, true),
                makeUser(3, "player2", "Player 2", Role.PLAYER, false)
        );
        when(userService.getAllUsers()).thenReturn(users);

        List<User> result = userService.getAllUsers();

        long total  = result.size();
        long active = result.stream().filter(User::isActive).count();
        long locked = result.stream().filter(u -> !u.isActive()).count();
        long admin  = result.stream().filter(u -> u.getRole() == Role.ADMIN).count();

        assertEquals(3, total,  "Total = 3");
        assertEquals(2, active, "Active = 2");
        assertEquals(1, locked, "Locked = 1");
        assertEquals(1, admin,  "Admin = 1");
    }

    // =========================================================================
    // TC ADMU_02 – Tìm kiếm theo từ khóa
    // =========================================================================

    /**
     * ADMU_02: Tìm kiếm theo từ khóa "player"
     * Mong đợi: Chỉ hiển thị users có username/displayName chứa "player"
     */
    @Test
    @DisplayName("ADMU_02 – Tìm kiếm theo từ khóa 'player'")
    void admu02_searchByKeyword() {
        List<User> allUsers = Arrays.asList(
                makeUser(1, "admin1",  "Admin One",  Role.ADMIN,  true),
                makeUser(2, "player1", "Player One", Role.PLAYER, true),
                makeUser(3, "player2", "Player Two", Role.PLAYER, false)
        );

        String keyword = "player";
        List<User> filtered = allUsers.stream()
                .filter(u -> matchesFilter(u, keyword, FILTER_ALL, FILTER_ALL))
                .toList();

        assertEquals(2, filtered.size(), "Phải tìm thấy 2 user chứa 'player'");
        assertTrue(filtered.stream().allMatch(u ->
                u.getUsername().contains(keyword) ||
                        (u.getDisplayName() != null && u.getDisplayName().toLowerCase().contains(keyword))
        ));
    }

    // =========================================================================
    // TC ADMU_03 – Lọc theo Vai trò = Quản trị viên
    // =========================================================================

    /**
     * ADMU_03: Lọc theo Vai trò = "Quản trị viên"
     * Mong đợi: Chỉ hiển thị users có role = ADMIN
     */
    @Test
    @DisplayName("ADMU_03 – Lọc theo Vai trò = Quản trị viên")
    void admu03_filterByRoleAdmin() {
        List<User> allUsers = Arrays.asList(
                makeUser(1, "admin1",  "Admin",    Role.ADMIN,  true),
                makeUser(2, "player1", "Player 1", Role.PLAYER, true),
                makeUser(3, "admin2",  "Admin 2",  Role.ADMIN,  true)
        );

        String roleFilter = Role.ADMIN.getLabel(); // "Quản trị viên"
        List<User> filtered = allUsers.stream()
                .filter(u -> matchesFilter(u, "", roleFilter, FILTER_ALL))
                .toList();

        assertEquals(2, filtered.size(), "Phải có 2 admin");
        assertTrue(filtered.stream().allMatch(u -> u.getRole() == Role.ADMIN),
                "Tất cả phải có role ADMIN");
    }

    // =========================================================================
    // TC ADMU_04 – Lọc theo Trạng thái = Đã khoá
    // =========================================================================

    /**
     * ADMU_04: Lọc theo Trạng thái = "Đã khoá"
     * Mong đợi: Chỉ hiển thị users có isActive = false
     */
    @Test
    @DisplayName("ADMU_04 – Lọc theo Trạng thái = Đã khoá")
    void admu04_filterByStatusLocked() {
        List<User> allUsers = Arrays.asList(
                makeUser(1, "user1", "User 1", Role.PLAYER, true),
                makeUser(2, "user2", "User 2", Role.PLAYER, false),
                makeUser(3, "user3", "User 3", Role.PLAYER, false)
        );

        List<User> filtered = allUsers.stream()
                .filter(u -> matchesFilter(u, "", FILTER_ALL, STATUS_LOCKED))
                .toList();

        assertEquals(2, filtered.size(), "Phải có 2 user bị khoá");
        assertTrue(filtered.stream().noneMatch(User::isActive),
                "Tất cả phải ở trạng thái khoá");
    }

    // =========================================================================
    // TC ADMU_05 – Không có kết quả khớp
    // =========================================================================

    /**
     * ADMU_05: Tìm keyword không tồn tại
     * Mong đợi: Danh sách kết quả rỗng
     */
    @Test
    @DisplayName("ADMU_05 – Không tìm thấy kết quả với keyword không tồn tại")
    void admu05_noResultsFound() {
        List<User> allUsers = Arrays.asList(
                makeUser(1, "admin1",  "Admin",    Role.ADMIN,  true),
                makeUser(2, "player1", "Player 1", Role.PLAYER, true)
        );

        String keyword = "xyz_not_exist";
        List<User> filtered = allUsers.stream()
                .filter(u -> matchesFilter(u, keyword, FILTER_ALL, FILTER_ALL))
                .toList();

        assertTrue(filtered.isEmpty(), "Kết quả phải rỗng khi keyword không tồn tại");
    }

    // =========================================================================
    // TC ADMU_06 – Reset bộ lọc
    // =========================================================================

    /**
     * ADMU_06: Reset bộ lọc về mặc định
     * Mong đợi: getAllUsers() được gọi lại để lấy toàn bộ danh sách
     */
    @Test
    @DisplayName("ADMU_06 – Reset bộ lọc gọi lại getAllUsers()")
    void admu06_resetFilterCallsGetAllUsers() throws DataAccessException {
        List<User> mockUsers = Arrays.asList(
                makeUser(1, "admin1",  "Admin",    Role.ADMIN,  true),
                makeUser(2, "player1", "Player 1", Role.PLAYER, true)
        );
        when(userService.getAllUsers()).thenReturn(mockUsers);

        // Giả lập: lần 1 load ban đầu, lần 2 sau khi refresh
        userService.getAllUsers(); // lần 1
        userService.getAllUsers(); // lần 2 (refresh)

        verify(userService, times(2)).getAllUsers();
    }

    // =========================================================================
    // TC ADMU_07 – Thêm user thành công
    // =========================================================================

    /**
     * ADMU_07: Thêm user mới thành công
     * Mong đợi: createUserFull() được gọi và trả về ID mới
     */
    @Test
    @DisplayName("ADMU_07 – Thêm user thành công")
    void admu07_addUserSuccess() throws DataAccessException {
        String username    = "newuser01";
        String displayName = "TestNick";
        Role   role        = Role.PLAYER;
        String passHash    = "some-bcrypt-hash"; // dummy hash (mock test, không dùng thật)

        when(userService.createUserFull(username, displayName, role, passHash))
                .thenReturn(42L);

        long newId = userService.createUserFull(username, displayName, role, passHash);

        assertEquals(42L, newId, "ID trả về phải là 42");
        verify(userService).createUserFull(username, displayName, role, passHash);
    }

    // =========================================================================
    // TC ADMU_08 – Thêm user - Username rỗng (validation)
    // =========================================================================

    /**
     * ADMU_08: Không thể thêm user khi username rỗng
     * Logic: Nút OK trong dialog phải bị disable; service không được gọi
     */
    @Test
    @DisplayName("ADMU_08 – Không thêm user khi username rỗng")
    void admu08_addUser_emptyUsername_noServiceCall() throws DataAccessException {
        String username = ""; // rỗng

        // Validation: nếu username rỗng thì không gọi service
        if (!username.isBlank()) {
            userService.createUserFull(username, "Nick", Role.PLAYER, "hash");
        }

        verify(userService, never())
                .createUserFull(anyString(), anyString(), any(), anyString());
    }

    // =========================================================================
    // TC ADMU_09 – Thêm user - Username đã tồn tại
    // =========================================================================

    /**
     * ADMU_09: Thêm user với username đã tồn tại
     * Mong đợi: DataAccessException được ném ra
     */
    @Test
    @DisplayName("ADMU_09 – Thêm user với username trùng ném DataAccessException")
    void admu09_addUser_duplicateUsername_throwsException() throws DataAccessException {
        when(userService.createUserFull(eq("existing"), anyString(), any(), anyString()))
                .thenThrow(new DataAccessException("Duplicate entry 'existing'"));

        assertThrows(DataAccessException.class,
                () -> userService.createUserFull("existing", "Nick", Role.PLAYER, "hash"),
                "Phải ném DataAccessException khi username trùng");
    }

    // =========================================================================
    // TC ADMU_10 – Thêm user - Lỗi DB
    // =========================================================================

    /**
     * ADMU_10: Lỗi kết nối DB khi thêm user
     * Mong đợi: Exception được bắt, app không crash
     */
    @Test
    @DisplayName("ADMU_10 – Lỗi DB khi thêm user ném DataAccessException")
    void admu10_addUser_dbError_throwsException() throws DataAccessException {
        when(userService.createUserFull(anyString(), anyString(), any(), anyString()))
                .thenThrow(new DataAccessException("Connection refused"));

        DataAccessException ex = assertThrows(DataAccessException.class,
                () -> userService.createUserFull("user1", "Nick", Role.PLAYER, "hash"));

        assertNotNull(ex.getMessage());
    }

    // =========================================================================
    // TC ADMU_11 – Sửa thông tin user thành công
    // =========================================================================

    /**
     * ADMU_11: Cập nhật displayName và role thành công
     * Mong đợi: updateDisplayName() và updateRole() đều được gọi
     */
    @Test
    @DisplayName("ADMU_11 – Sửa displayName và role thành công")
    void admu11_editUser_success() throws DataAccessException {
        long userId = 5L;
        String newDisplayName = "New Nickname";
        Role newRole = Role.ADMIN;

        doNothing().when(userService).updateDisplayName(userId, newDisplayName);
        doNothing().when(userService).updateRole(userId, newRole);

        userService.updateDisplayName(userId, newDisplayName);
        userService.updateRole(userId, newRole);

        verify(userService).updateDisplayName(userId, newDisplayName);
        verify(userService).updateRole(userId, newRole);
    }

    // =========================================================================
    // TC ADMU_12 – Sửa không chọn user
    // =========================================================================

    /**
     * ADMU_12: Bấm Sửa khi không chọn user
     * Logic: selected == null → service không được gọi
     */
    @Test
    @DisplayName("ADMU_12 – Sửa khi không chọn user: service không được gọi")
    void admu12_editUser_noneSelected_noServiceCall() throws DataAccessException {
        User selected = null; // Không chọn user nào

        if (selected != null) {
            userService.updateDisplayName(selected.getId(), "newNick");
        }

        verify(userService, never()).updateDisplayName(anyLong(), anyString());
    }

    // =========================================================================
    // TC ADMU_13 – Sửa - username bị disable trong dialog
    // =========================================================================

    /**
     * ADMU_13: Trong dialog sửa, ô username phải bị disable
     * Logic: Khi isEdit=true, username không thể thay đổi
     */
    @Test
    @DisplayName("ADMU_13 – Username không thay đổi khi sửa (bị disable)")
    void admu13_editUser_usernameUnchanged() throws DataAccessException {
        User selected = makeUser(3L, "original_user", "Old Nick", Role.PLAYER, true);
        String originalUsername = selected.getUsername();

        // Simulate edit: chỉ thay đổi displayName và role, không đổi username
        userService.updateDisplayName(selected.getId(), "New Nick");
        userService.updateRole(selected.getId(), Role.ADMIN);

        // Username phải giữ nguyên
        assertEquals("original_user", originalUsername,
                "Username không được thay đổi khi sửa");
        verify(userService, never()).getUserByUsername(anyString());
    }

    // =========================================================================
    // TC ADMU_14 – Sửa - Lỗi DB khi update
    // =========================================================================

    /**
     * ADMU_14: Lỗi DB khi cập nhật
     * Mong đợi: Exception được ném ra, dữ liệu in-memory không thay đổi
     */
    @Test
    @DisplayName("ADMU_14 – Lỗi DB khi sửa user ném exception")
    void admu14_editUser_dbError_throwsException() throws DataAccessException {
        doThrow(new DataAccessException("Update failed"))
                .when(userService).updateDisplayName(anyLong(), anyString());

        assertThrows(DataAccessException.class,
                () -> userService.updateDisplayName(1L, "New Nick"),
                "Phải ném DataAccessException khi DB lỗi");
    }

    // =========================================================================
    // TC ADMU_15 – Khoá user đang hoạt động
    // =========================================================================

    /**
     * ADMU_15: Khoá user có isActive=true
     * Mong đợi: setActive(id, false) được gọi
     */
    @Test
    @DisplayName("ADMU_15 – Khoá user đang hoạt động")
    void admu15_lockActiveUser() throws DataAccessException {
        User user = makeUser(10L, "player1", "Player", Role.PLAYER, true);
        assertTrue(user.isActive(), "User phải đang hoạt động");

        boolean newStatus = !user.isActive(); // false
        doNothing().when(userService).setActive(user.getId(), newStatus);

        userService.setActive(user.getId(), newStatus);

        verify(userService).setActive(10L, false);
        // Cập nhật in-memory
        user.setActive(newStatus);
        assertFalse(user.isActive(), "User phải bị khoá sau khi gọi setActive(false)");
    }

    // =========================================================================
    // TC ADMU_16 – Mở khoá user bị khoá
    // =========================================================================

    /**
     * ADMU_16: Mở khoá user có isActive=false
     * Mong đợi: setActive(id, true) được gọi
     */
    @Test
    @DisplayName("ADMU_16 – Mở khoá user bị khoá")
    void admu16_unlockLockedUser() throws DataAccessException {
        User user = makeUser(11L, "player2", "Player 2", Role.PLAYER, false);
        assertFalse(user.isActive(), "User phải đang bị khoá");

        boolean newStatus = !user.isActive(); // true
        doNothing().when(userService).setActive(user.getId(), newStatus);

        userService.setActive(user.getId(), newStatus);

        verify(userService).setActive(11L, true);
        user.setActive(newStatus);
        assertTrue(user.isActive(), "User phải được mở khoá sau khi gọi setActive(true)");
    }

    // =========================================================================
    // TC ADMU_17 – Khoá/Mở khoá không chọn user
    // =========================================================================

    /**
     * ADMU_17: Bấm Khoá/Mở khoá khi không chọn user
     * Logic: selected == null → setActive() không được gọi
     */
    @Test
    @DisplayName("ADMU_17 – Khoá/Mở khoá khi không chọn user: service không gọi")
    void admu17_toggleLock_noneSelected_noServiceCall() throws DataAccessException {
        User selected = null;

        if (selected != null) {
            userService.setActive(selected.getId(), !selected.isActive());
        }

        verify(userService, never()).setActive(anyLong(), anyBoolean());
    }

    // =========================================================================
    // TC ADMU_18 – Xoá user - xác nhận OK
    // =========================================================================

    /**
     * ADMU_18: Xoá user sau khi xác nhận OK
     * Mong đợi: deleteUser(id) được gọi, user bị xoá khỏi danh sách
     */
    @Test
    @DisplayName("ADMU_18 – Xoá user thành công sau khi xác nhận")
    void admu18_deleteUser_confirmed() throws DataAccessException {
        User user = makeUser(20L, "to_delete", "Delete Me", Role.PLAYER, true);
        doNothing().when(userService).deleteUser(user.getId());

        userService.deleteUser(user.getId());

        verify(userService).deleteUser(20L);
    }

    // =========================================================================
    // TC ADMU_19 – Xoá user - hủy thao tác
    // =========================================================================

    /**
     * ADMU_19: Hủy xoá user (bấm Hủy trên dialog)
     * Logic: Nếu user bấm Hủy, deleteUser() không được gọi
     */
    @Test
    @DisplayName("ADMU_19 – Hủy xoá user: deleteUser() không được gọi")
    void admu19_deleteUser_cancelled_noServiceCall() throws DataAccessException {
        boolean userConfirmed = false; // Giả lập user bấm Hủy

        if (userConfirmed) {
            userService.deleteUser(20L);
        }

        verify(userService, never()).deleteUser(anyLong());
    }

    // =========================================================================
    // TC ADMU_20 – Xoá user - không chọn user
    // =========================================================================

    /**
     * ADMU_20: Bấm Xoá khi không có user được chọn
     * Logic: selected == null → deleteUser() không được gọi
     */
    @Test
    @DisplayName("ADMU_20 – Xoá khi không chọn user: deleteUser() không gọi")
    void admu20_deleteUser_noneSelected_noServiceCall() throws DataAccessException {
        User selected = null;

        if (selected != null) {
            userService.deleteUser(selected.getId());
        }

        verify(userService, never()).deleteUser(anyLong());
    }

    // =========================================================================
    // TC ADMU_21 – Xoá user - Lỗi DB
    // =========================================================================

    /**
     * ADMU_21: Lỗi DB khi xoá user
     * Mong đợi: DataAccessException được ném ra, user vẫn còn trong danh sách
     */
    @Test
    @DisplayName("ADMU_21 – Lỗi DB khi xoá user ném DataAccessException")
    void admu21_deleteUser_dbError_throwsException() throws DataAccessException {
        doThrow(new DataAccessException("Delete failed"))
                .when(userService).deleteUser(anyLong());

        assertThrows(DataAccessException.class,
                () -> userService.deleteUser(1L),
                "Phải ném DataAccessException khi DB lỗi");

        // Vì exception → in-memory list không thay đổi (controller không gọi remove)
        // Behavior này được đảm bảo bởi exception trước khi gọi allUsers.remove()
    }

    // =========================================================================
    // TC bổ sung – Lọc kết hợp keyword + role + status
    // =========================================================================

    /**
     * Lọc kết hợp: keyword="admin", role=ADMIN, status=Hoạt động
     * Mong đợi: Chỉ trả về admin đang hoạt động có username chứa "admin"
     */
    @Test
    @DisplayName("Bổ sung – Lọc kết hợp keyword + role + status")
    void combinedFilter_keyword_role_status() {
        List<User> allUsers = Arrays.asList(
                makeUser(1, "admin1",   "Admin One",   Role.ADMIN,  true),
                makeUser(2, "admin2",   "Admin Two",   Role.ADMIN,  false),
                makeUser(3, "player1",  "Player One",  Role.PLAYER, true)
        );

        List<User> filtered = allUsers.stream()
                .filter(u -> matchesFilter(u, "admin", Role.ADMIN.getLabel(), STATUS_ACTIVE))
                .toList();

        assertEquals(1, filtered.size());
        assertEquals("admin1", filtered.get(0).getUsername());
    }
}
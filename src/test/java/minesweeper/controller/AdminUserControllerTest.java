package minesweeper.controller;

import minesweeper.model.User;
import minesweeper.model.enums.Role;
import minesweeper.service.ManagerUserService;
import minesweeper.service.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class AdminUserControllerTest {

    private AdminUserController controller;

    @BeforeEach
    void setUp() {
        // Mock the service to avoid real DB connection attempts during instantiation
        ManagerUserService mockService = mock(ManagerUserService.class);
        controller = new AdminUserController(mockService);
        SessionManager.clearSession(); // Đảm bảo state độc lập
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
    }

    @Test
    void validateNotSelfAction_NullUser_ShouldThrowException() {
        // Kiểm tra khóa
        IllegalArgumentException lockException = assertThrows(
                IllegalArgumentException.class,
                () -> controller.validateNotSelfAction(null, "khóa")
        );
        assertEquals("Hãy chọn user", lockException.getMessage());

        // Kiểm tra xóa
        IllegalArgumentException deleteException = assertThrows(
                IllegalArgumentException.class,
                () -> controller.validateNotSelfAction(null, "xóa")
        );
        assertEquals("Hãy chọn user cần xoá", deleteException.getMessage());
    }

    @Test
    void validateNotSelfAction_AdminSelfLocking_ShouldThrowException() {
        // Arrange
        User currentAdmin = new User();
        currentAdmin.setId(1);
        currentAdmin.setUsername("admin1");
        currentAdmin.setRole(Role.ADMIN);
        
        SessionManager.createSession(currentAdmin);

        User selectedUser = new User();
        selectedUser.setId(1); // Cùng ID với admin hiện tại

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.validateNotSelfAction(selectedUser, "khóa")
        );
        assertEquals("Bạn không thể khóa tài khoản của chính mình!", exception.getMessage());
    }

    @Test
    void validateNotSelfAction_AdminSelfDeleting_ShouldThrowException() {
        // Arrange
        User currentAdmin = new User();
        currentAdmin.setId(1);
        currentAdmin.setUsername("admin1");
        currentAdmin.setRole(Role.ADMIN);
        
        SessionManager.createSession(currentAdmin);

        User selectedUser = new User();
        selectedUser.setId(1); // Cùng ID với admin hiện tại

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.validateNotSelfAction(selectedUser, "xóa")
        );
        assertEquals("Bạn không thể xóa tài khoản của chính mình!", exception.getMessage());
    }

    @Test
    void validateNotSelfAction_AdminLockingOtherUser_ShouldPass() {
        // Arrange
        User currentAdmin = new User();
        currentAdmin.setId(1);
        SessionManager.createSession(currentAdmin);

        User otherUser = new User();
        otherUser.setId(2); // Khác ID

        // Act & Assert
        // Không nên throw bất kì exception nào
        controller.validateNotSelfAction(otherUser, "khóa");
    }

    @Test
    void validateNotSelfAction_NotLoggedIn_ShouldPass() {
        // Arrange
        SessionManager.clearSession();
        User otherUser = new User();
        otherUser.setId(2);

        // Act & Assert
        controller.validateNotSelfAction(otherUser, "khóa");
    }
}

package minesweeper.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import minesweeper.model.User;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.service.MySqlUserService;
import minesweeper.service.UserService;

import java.util.List;

public class AdminUserController {

    // ── FXML ─────────────────────────────────────────────────────────────────

    @FXML private TextField searchField;

    @FXML private TableView<User>              userTable;
    @FXML private TableColumn<User, Integer>   colId;
    @FXML private TableColumn<User, String>    colUsername;
    @FXML private TableColumn<User, String>    colEmail;
    @FXML private TableColumn<User, String>    colDisplayName;
    @FXML private TableColumn<User, Boolean>   colStatus;

    // ── Service ──────────────────────────────────────────────────────────────

    private final UserService userService;

    private final ObservableList<User> masterList = FXCollections.observableArrayList();

    public AdminUserController() {
        this(new MySqlUserService());
    }

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    // ── Khởi tạo ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupColumns();
        loadUsers();
    }


    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colDisplayName.setCellValueFactory(new PropertyValueFactory<>("displayName"));

        colEmail.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getEmail() != null ? data.getValue().getEmail() : ""));

        colStatus.setCellValueFactory(data ->
                new SimpleBooleanProperty(data.getValue().isActive()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                if (item) {
                    setText("✔ Hoạt động");
                    setStyle("-fx-text-fill: #2a9d2a;");
                } else {
                    setText("✖ Đã khoá");
                    setStyle("-fx-text-fill: #c0392b;");
                }
            }
        });
    }


    private void loadUsers() {
        try {
            List<User> users = userService.getAllUsers();
            masterList.setAll(users);
            userTable.setItems(masterList);
        } catch (DataAccessException e) {
            showError("Không thể tải danh sách người dùng:\n" + e.getMessage());
        }
    }

    @FXML
    public void onRefresh() {
        searchField.clear();
        loadUsers();
    }

    // ── Tìm kiếm ─────────────────────────────────────────────────────────────

    @FXML
    public void onSearch() {
        String keyword = searchField.getText().trim().toLowerCase();

        if (keyword.isBlank()) {
            userTable.setItems(masterList);
            return;
        }

        ObservableList<User> filtered = FXCollections.observableArrayList();
        for (User u : masterList) {
            boolean matchUsername = u.getUsername() != null
                    && u.getUsername().toLowerCase().contains(keyword);
            boolean matchDisplay  = u.getDisplayName() != null
                    && u.getDisplayName().toLowerCase().contains(keyword);
            boolean matchEmail    = u.getEmail() != null
                    && u.getEmail().toLowerCase().contains(keyword);

            if (matchUsername || matchDisplay || matchEmail) {
                filtered.add(u);
            }
        }

        userTable.setItems(filtered);

        if (filtered.isEmpty()) {
            showInfo("Không tìm thấy user nào khớp với \"" + keyword + "\".");
        }
    }

    // ── Sửa tên hiển thị ─────────────────────────────────────────────────────

    @FXML
    public void onEditUser() {
        User user = getSelectedUser();
        if (user == null) return;

        TextInputDialog dialog = new TextInputDialog(
                user.getDisplayName() != null ? user.getDisplayName() : "");
        dialog.setTitle("Sửa tên hiển thị");
        dialog.setHeaderText("Nhập tên mới cho: " + user.getUsername());
        dialog.setContentText("Tên hiển thị:");

        dialog.showAndWait().ifPresent(newName -> {
            if (newName.isBlank()) {
                showError("Tên hiển thị không được để trống.");
                return;
            }
            user.setDisplayName(newName.trim());
            // TODO: userService.updateDisplayName(user.getId(), newName) khi có method
            userTable.refresh();
            showInfo("Đã cập nhật tên hiển thị thành công.");
        });
    }

    // ── Khoá / Mở khoá ──────────────────────────────────────────────────────

    @FXML
    public void onLockUser() {
        User user = getSelectedUser();
        if (user == null) return;

        boolean willLock = user.isActive(); // đang active → sắp bị khoá
        String action = willLock ? "khoá" : "mở khoá";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận " + action);
        confirm.setHeaderText("Bạn muốn " + action + " tài khoản: " + user.getUsername() + "?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                user.setActive(!willLock);
                // TODO: userService.setActive(user.getId(), !willLock) khi có method
                userTable.refresh();
                showInfo("Đã " + action + " tài khoản " + user.getUsername() + ".");
            }
        });
    }

    // ── Xoá user ─────────────────────────────────────────────────────────────

    @FXML
    public void onDeleteUser() {
        User user = getSelectedUser();
        if (user == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xoá");
        confirm.setHeaderText("Xoá tài khoản: " + user.getUsername());
        confirm.setContentText("Hành động này không thể hoàn tác. Bạn có chắc chắn?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                // TODO: userService.deleteUser(user.getId()) khi có method
                masterList.remove(user);
                // nếu đang hiển thị filtered list thì xoá luôn ở đó
                if (userTable.getItems() != masterList) {
                    userTable.getItems().remove(user);
                }
                showInfo("Đã xoá tài khoản " + user.getUsername() + ".");
            }
        });
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    /** Lấy user đang được chọn, hiển thị cảnh báo nếu chưa chọn. */
    private User getSelectedUser() {
        User user = userTable.getSelectionModel().getSelectedItem();
        if (user == null) {
            showInfo("Hãy chọn một user trong bảng trước.");
        }
        return user;
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
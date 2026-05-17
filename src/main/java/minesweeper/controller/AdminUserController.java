package minesweeper.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import minesweeper.model.enums.Role;
import minesweeper.model.User;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.service.MySqlUserService;
import minesweeper.service.UserService;
import utils.CryptUtils;
import minesweeper.model.AuditLog;
import minesweeper.repository.log.MySqlAuditLogRepository;
import minesweeper.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

import java.util.List;
import java.util.Optional;

public class AdminUserController {

    // ── FXML Controls ────────────────────────────────────────────────────────
    @FXML private TextField       searchField;
    @FXML private ComboBox<String> cbRoleFilter;
    @FXML private ComboBox<String> cbStatusFilter;

    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label lockedUsersLabel;
    @FXML private Label adminCountLabel;
    @FXML private Label statusLabel;

    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Label  pageLabel;

    @FXML private TableView<User>              userTable;
    @FXML private TableColumn<User, Boolean>   colSelect;
    @FXML private TableColumn<User, Integer>   colId;
    @FXML private TableColumn<User, String>    colUsername;
    @FXML private TableColumn<User, String>    colDisplayName;
    @FXML private TableColumn<User, String>    colRole;
    @FXML private TableColumn<User, String>    colStatus;

    // ── Constants ────────────────────────────────────────────────────────────
    private static final int    PAGE_SIZE        = 20;
    private static final String FILTER_ALL       = "Tất cả";
    private static final String STATUS_ACTIVE    = "Hoạt động";
    private static final String STATUS_LOCKED    = "Đã khoá";
    private static final String DEFAULT_PASSWORD = "123456";

    // ── State ────────────────────────────────────────────────────────────────
    private int     currentPage  = 0;
    private int     totalPages   = 1;
    private boolean isFiltering  = false;

    private final ObservableList<User> allUsers  = FXCollections.observableArrayList();
    private final ObservableList<User> filtered  = FXCollections.observableArrayList();
    private final ObservableList<User> pageItems = FXCollections.observableArrayList();

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final UserService userService;
    private static final Logger LOG = LoggerFactory.getLogger(AdminUserController.class);
    private final MySqlAuditLogRepository auditLogRepository;

    public AdminUserController() {
        this.userService = new MySqlUserService();
        this.auditLogRepository = new MySqlAuditLogRepository();
    }

    // =========================================================================
    // Basic Flow – UC-22
    // =========================================================================

    /**
     * 22.1.1 Admin mở màn hình quản lí người dùng
     */
    @FXML
    public void initialize() {
        setupFilterComboBoxes();
        setupTable();
        loadUsers(); // 22.1.2 Hệ thống tải và hiển thị danh sách người dùng
    }

    private void setupFilterComboBoxes() {
        cbRoleFilter.setItems(FXCollections.observableArrayList(FILTER_ALL, "Người chơi", "Quản trị viên"));
        cbRoleFilter.getSelectionModel().selectFirst();

        cbStatusFilter.setItems(FXCollections.observableArrayList(FILTER_ALL, STATUS_ACTIVE, STATUS_LOCKED));
        cbStatusFilter.getSelectionModel().selectFirst();
    }

    private void setupTable() {
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        userTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        setupColumns();
    }

    /**
     * 22.1.2 Hệ thống tải và hiển thị danh sách người dùng
     * 22.1-E1 CSDL không thể kết nối
     */
    private void loadUsers() {
        try {
            List<User> users = userService.getAllUsers();
            allUsers.setAll(users);
            isFiltering = false;
            filtered.clear();
            currentPage = 0;
            totalPages = calcTotalPages(allUsers.size());
            showPage();
            updateStats(allUsers);
            statusLabel.setText("Đã tải " + allUsers.size() + " users");
        } catch (DataAccessException e) {
            // 22.1-E1 CSDL không thể kết nối → hiển thị hộp thoại lỗi, bảng để trống
            showError("Không thể tải dữ liệu");
        }
    }

    // =========================================================================
    // Alternative Flow – UC-22.2 Tìm kiếm người dùng
    // =========================================================================

    /**
     * 22.2.1 Admin nhập từ khóa và chọn bộ lọc (Vai trò/Trạng thái) rồi nhấn Tìm kiếm
     * 22.2.2 Hệ thống lọc danh sách và làm mới danh sách
     * 22.2-A1 Admin xoá hết điều kiện rồi nhấn Tìm kiếm
     */
    @FXML
    public void onSearch() {
        // 22.2.1 Admin nhập từ khóa và chọn bộ lọc rồi nhấn Tìm kiếm
        String keyword      = searchField.getText().toLowerCase().trim();
        String roleFilter   = cbRoleFilter.getValue();
        String statusFilter = cbStatusFilter.getValue();

        boolean noFilter = keyword.isEmpty()
                && FILTER_ALL.equals(roleFilter)
                && FILTER_ALL.equals(statusFilter);

        if (noFilter) {
            // 22.2-A1 Admin xoá hết điều kiện → khôi phục toàn bộ danh sách ban đầu
            isFiltering = false;
            filtered.clear();
        } else {
            // 22.2.2 Hệ thống lọc danh sách và làm mới danh sách
            filtered.clear();
            for (User u : allUsers) {
                if (matchesFilter(u, keyword, roleFilter, statusFilter)) {
                    filtered.add(u);
                }
            }
            isFiltering = true;
        }

        currentPage = 0;
        showPage();

        int resultCount = isFiltering ? filtered.size() : allUsers.size();
        statusLabel.setText("Tìm thấy " + resultCount + " kết quả");
    }

    /**
     * 22.2-A2 Admin nhấn Làm mới
     * Hệ thống xóa ô tìm kiếm, đưa các bộ lọc về mặc định và tải lại danh sách mới
     */
    @FXML
    public void onRefresh() {
        searchField.clear();
        cbRoleFilter.getSelectionModel().selectFirst();
        cbStatusFilter.getSelectionModel().selectFirst();
        loadUsers();
    }

    /**
     * Kiểm tra user có khớp điều kiện lọc không.
     */
    private boolean matchesFilter(User u, String keyword, String roleFilter, String statusFilter) {
        boolean matchKw = keyword.isEmpty()
                || u.getUsername().toLowerCase().contains(keyword)
                || (u.getDisplayName() != null && u.getDisplayName().toLowerCase().contains(keyword));

        boolean matchRole = FILTER_ALL.equals(roleFilter)
                || (u.getRole() != null && u.getRole().getLabel().equals(roleFilter));

        boolean matchStatus = FILTER_ALL.equals(statusFilter)
                || (STATUS_ACTIVE.equals(statusFilter) && u.isActive())
                || (STATUS_LOCKED.equals(statusFilter) && !u.isActive());

        return matchKw && matchRole && matchStatus;
    }

    // =========================================================================
    // Alternative Flow – UC-22.3 Thêm người dùng
    // =========================================================================

    /**
     * 22.3.1 Admin nhấn nút thêm người dùng
     * 22.3-A1 Admin nhấn Huỷ
     */
    @FXML
    public void onAddUser() {
        // 22.3.1 Admin nhấn nút thêm người dùng
        // 22.3.2 Hệ thống hiển thị dialog nhập thông tin người dùng
        Dialog<User> dialog = buildUserDialog(null);
        Optional<User> result = dialog.showAndWait(); // 22.3-A1 Admin nhấn Huỷ → đóng, không lưu

        // 22.3.3 Admin nhập thông tin và nhấn nút thêm
        result.ifPresent(newUser -> {
            try {
                // 22.3.4 Hệ thống hash mật khẩu và lưu trữ người dùng vào CSDL
                String passwordHash = CryptUtils.md5(newUser.getPasswordHash());
                long generatedId = userService.createUserFull(
                        newUser.getUsername(),
                        newUser.getDisplayName(),
                        newUser.getRole(),
                        passwordHash
                );

                // 22.3.5 Hệ thống gán Id trả về cho User mới và cập nhật danh sách
                newUser.setId((int) generatedId);
                allUsers.add(newUser);
                if (isFiltering) filtered.add(newUser);

                currentPage = calcTotalPages(isFiltering ? filtered.size() : allUsers.size()) - 1;
                showPage();
                updateStats(allUsers);
                statusLabel.setText("Đã thêm user: " + newUser.getUsername());
                // Log audit: thêm ghi chép vào bảng audit_log /////////////////////////////
                try {
                    Long adminId = SessionManager.isLoggedIn()
                            ? SessionManager.getCurrentUser().getId()
                            : null;

                    String target = "user:" + newUser.getUsername();
                    String details = "Created new user; Role: " +
                            (newUser.getRole() != null ? newUser.getRole().getLabel() : "N/A");

                    AuditLog log = new AuditLog(adminId, "CREATE_USER", target, details);
                    auditLogRepository.insert(log);

                    LOG.info("[AUDIT] Admin {} created user: {}", adminId, newUser.getUsername());
                } catch (Exception e) {
                    LOG.warn("Failed to log audit for user creation", e);
                }

            } catch (Exception e) {
                // 22.3-E1 (username trùng, mất kết nối...) → hiển thị hộp thoại lỗi
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                showError("Thêm user thất bại!\n"
                        + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + (cause != e ? "\nCause: " + cause.getMessage() : ""));
            }
        });
    }

    // =========================================================================
    // Alternative Flow – UC-22.4 Chỉnh sửa thông tin người dùng
    // =========================================================================

    /**
     * 22.4.1 Admin chọn user trong bảng
     * 22.4.2 Admin nhấn nút chỉnh sửa
     * 22.4-A1 Admin nhấn hủy
     * 22.4-E1 Chưa chọn User, nhấn chỉnh sửa
     * 22.4-E2 Lỗi CSDL
     */
    @FXML
    public void onEditUser() {
        // 22.4.1 Admin chọn user trong bảng
        // 22.4.2 Admin nhấn nút chỉnh sửa
        User selected = userTable.getSelectionModel().getSelectedItem();

        // 22.4-E1 Chưa chọn User → hiển thị thông báo chọn User
        if (selected == null) {
            showInfo("Hãy chọn user cần sửa");
            return;
        }

        // 22.4.3 Hệ thống hiển thị dialog sửa người dùng
        Dialog<User> dialog = buildUserDialog(selected);
        Optional<User> result = dialog.showAndWait(); // 22.4-A1 Admin nhấn hủy → đóng, không thay đổi

        // 22.4.4 Admin cập nhật nickname hoặc vai trò và nhấn cập nhật
        result.ifPresent(updated -> {
            try {
                // 22.4.5 Hệ thống cập nhật thông tin vào CSDL
                userService.updateDisplayName(selected.getId(), updated.getDisplayName());
                userService.updateRole(selected.getId(), updated.getRole());

                // 22.4.6 Hệ thống load lại danh sách
                selected.setDisplayName(updated.getDisplayName());
                selected.setRole(updated.getRole());
                userTable.refresh();
                updateStats(allUsers);
                statusLabel.setText("Đã cập nhật: " + selected.getUsername());
                // Log audit: thêm ghi chép vào bảng audit_log /////////////////////////////
                try {
                    Long adminId = SessionManager.isLoggedIn()
                            ? SessionManager.getCurrentUser().getId()
                            : null;

                    String target = "user:" + selected.getUsername();
                    String changes = "DisplayName: " + selected.getDisplayName() + " → " + updated.getDisplayName() +
                            "; Role: " + (selected.getRole() != null ? selected.getRole().getLabel() : "N/A") +
                            " → " + (updated.getRole() != null ? updated.getRole().getLabel() : "N/A");

                    AuditLog log = new AuditLog(adminId, "UPDATE_USER", target, changes);
                    auditLogRepository.insert(log);

                    LOG.info("[AUDIT] Admin {} updated user: {} | {}", adminId, selected.getUsername(), changes);
                } catch (Exception e) {
                    LOG.warn("Failed to log audit for user update", e);
                }
            } catch (Exception e) {
                // 22.4-E2 Lỗi CSDL → hiển thị hộp thoại lỗi, bộ nhớ không thay đổi
                showError("Sửa thất bại: " + e.getMessage());
            }
        });
    }

    // =========================================================================
    // Alternative Flow – UC-22.5 Khoá / Mở khoá tài khoản
    // =========================================================================

    /**
     * 22.5.1 Admin chọn User trong bảng và nhấn nút khóa/ mở khóa
     * 22.5-E1 Chưa chọn User, nhấn khóa/ mở khóa
     * 22.5-E2 Lỗi CSDL
     */
    @FXML
    public void onLockUser() {
        // 22.5.1 Admin chọn User trong bảng và nhấn nút khóa/ mở khóa
        User selected = userTable.getSelectionModel().getSelectedItem();

        // 22.5-E1 Chưa chọn User → hiển thị thông báo chọn User
        if (selected == null) {
            showInfo("Hãy chọn user");
            return;
        }

        try {
            // 22.5.2 Hệ thống cập nhật vào CSDL
            boolean newStatus = !selected.isActive();
            userService.setActive(selected.getId(), newStatus);

            // 22.5.3 Hệ thống load lại danh sách
            selected.setActive(newStatus);
            userTable.refresh();
            updateStats(allUsers);
            String msg = newStatus
                    ? "Đã mở khoá: " + selected.getUsername()
                    : "Đã khoá: " + selected.getUsername();
            statusLabel.setText(msg);

            // Log audit: thêm ghi chép vào bảng audit_log /////////////////////////////
            try {
                Long adminId = SessionManager.isLoggedIn()
                        ? SessionManager.getCurrentUser().getId()
                        : null;

                String target = "user:" + selected.getUsername();
                String action = newStatus ? "UNLOCK_USER" : "LOCK_USER";
                String details = newStatus ? "Unlocked user account" : "Locked user account";

                AuditLog log = new AuditLog(adminId, action, target, details);
                auditLogRepository.insert(log);

                LOG.info("[AUDIT] Admin {} {} user: {}", adminId, action, selected.getUsername());
            } catch (Exception e) {
                LOG.warn("Failed to log audit for lock/unlock", e);
            }
        } catch (Exception e) {
            // 22.5-E2 Lỗi CSDL → hiển thị hộp thoại lỗi, bộ nhớ không thay đổi
            showError("Khoá/mở khoá thất bại");
        }
    }

    // =========================================================================
    // Alternative Flow – UC-22.6 Xoá người dùng
    // =========================================================================

    /**
     * 22.6.1 Admin chọn User trong bảng và nhấn nút xóa
     * 22.6-A1 Admin nhấn hủy
     * 22.6-E1 Chưa chọn User, nhấn xóa
     * 22.6-E2 Lỗi CSDL
     */
    @FXML
    public void onDeleteUser() {
        // 22.6.1 Admin chọn User trong bảng và nhấn nút xóa
        User selected = userTable.getSelectionModel().getSelectedItem();

        // 22.6-E1 Chưa chọn User → hiển thị thông báo chọn User
        if (selected == null) {
            showInfo("Hãy chọn user cần xoá");
            return;
        }

        // 22.6.2 Hệ thống hiển thị hộp thoại xác nhận
        if (!confirmDelete(selected.getUsername())) {
            // 22.6-A1 Admin nhấn hủy → đóng dialog, không thay đổi
            return;
        }

        try {
            // 22.6.3 Admin xác nhận OK
            // 22.6.4 Hệ thống xóa User khỏi CSDL
            userService.deleteUser(selected.getId());
            allUsers.remove(selected);
            if (isFiltering) filtered.remove(selected);
            int newTotalPages = calcTotalPages(isFiltering ? filtered.size() : allUsers.size());
            if (currentPage >= newTotalPages) {
                currentPage = Math.max(0, currentPage - 1);
            }

            // 22.6.5 Hệ thống load lại danh sách
            showPage();
            updateStats(allUsers);
            statusLabel.setText("Đã xoá user: " + selected.getUsername());
            // Log audit: thêm ghi chép vào bảng audit_log /////////////////////////////
            try {
                Long adminId = SessionManager.isLoggedIn()
                        ? SessionManager.getCurrentUser().getId()
                        : null;

                String target = "user:" + selected.getUsername();
                String details = "Deleted user; Original Role: " +
                        (selected.getRole() != null ? selected.getRole().getLabel() : "N/A") +
                        "; Status: " + (selected.isActive() ? "Active" : "Locked");

                AuditLog log = new AuditLog(adminId, "DELETE_USER", target, details);
                auditLogRepository.insert(log);

                LOG.info("[AUDIT] Admin {} deleted user: {}", adminId, selected.getUsername());
            } catch (Exception e) {
                LOG.warn("Failed to log audit for user deletion", e);
            }
        } catch (Exception e) {
            // 22.6-E2 Lỗi CSDL → hiển thị hộp thoại lỗi, bộ nhớ không thay đổi
            showError("Xoá thất bại");
        }
    }

    // =========================================================================
    // Private – Giao diện & Helpers
    // =========================================================================

    @FXML
    public void onPrevPage() {
        if (currentPage > 0) {
            currentPage--;
            showPage();
        }
    }

    @FXML
    public void onNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            showPage();
        }
    }

    /**
     * Hiển thị một trang dữ liệu lên bảng.
     * Dùng allUsers hoặc filtered tuỳ trạng thái isFiltering.
     */
    private void showPage() {
        ObservableList<User> source = isFiltering ? filtered : allUsers;
        totalPages = calcTotalPages(source.size());
        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, source.size());
        pageItems.setAll(source.subList(from, to));
        userTable.setItems(pageItems);
        pageLabel.setText("Trang " + (currentPage + 1) + " / " + totalPages);
        btnPrevPage.setDisable(currentPage == 0);
        btnNextPage.setDisable(currentPage >= totalPages - 1);
    }

    /** Cập nhật 4 nhãn thống kê phía trên bảng. */
    private void updateStats(List<User> users) {
        int total = users.size(), active = 0, locked = 0, admin = 0;
        for (User u : users) {
            if (u.isActive()) active++; else locked++;
            if (u.getRole() == Role.ADMIN) admin++;
        }
        totalUsersLabel.setText(String.valueOf(total));
        activeUsersLabel.setText(String.valueOf(active));
        lockedUsersLabel.setText(String.valueOf(locked));
        adminCountLabel.setText(String.valueOf(admin));
    }

    private int calcTotalPages(int size) {
        return Math.max(1, (int) Math.ceil((double) size / PAGE_SIZE));
    }

    /**
     * 22.6.2 Hệ thống hiển thị hộp thoại xác nhận
     * @return true nếu Admin nhấn OK (22.6.3)
     */
    private boolean confirmDelete(String username) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Xoá user \"" + username + "\"?");
        alert.setContentText("Hành động này không thể hoàn tác.");
        Optional<ButtonType> btn = alert.showAndWait();
        return btn.isPresent() && btn.get() == ButtonType.OK;
    }

    /**
     * Dialog dùng chung cho Thêm (existing=null) và Sửa (existing≠null)
     * 22.3-E1 Username bị bỏ trống → nút Thêm bị vô hiệu hoá
     * 22.3-A2 Biệt danh bị để trống → dùng username làm tên hiển thị
     */
    private Dialog<User> buildUserDialog(User existing) {
        boolean isEdit = existing != null;

        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Sửa người dùng" : "Thêm người dùng");
        dialog.setHeaderText(isEdit
                ? "Cập nhật thông tin: " + existing.getUsername()
                : "Nhập thông tin người dùng mới");

        ButtonType okBtn = new ButtonType(
                isEdit ? "Cập nhật" : "Thêm",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField      tfUsername    = new TextField(isEdit ? existing.getUsername() : "");
        TextField      tfDisplayName = new TextField(isEdit && existing.getDisplayName() != null
                ? existing.getDisplayName() : "");
        TextField      tfPassword    = new TextField(DEFAULT_PASSWORD);
        ComboBox<Role> cbRole        = new ComboBox<>(FXCollections.observableArrayList(Role.values()));

        tfUsername.setPromptText("username");
        tfDisplayName.setPromptText("Tên hiển thị (nickname)");
        tfPassword.setPromptText("Mật khẩu (mặc định: " + DEFAULT_PASSWORD + ")");

        cbRole.getSelectionModel().select(
                isEdit && existing.getRole() != null ? existing.getRole() : Role.PLAYER);

        if (isEdit) {
            tfUsername.setDisable(true);
            tfPassword.setDisable(true);
        }

        GridPane.setHgrow(tfUsername,    Priority.ALWAYS);
        GridPane.setHgrow(tfDisplayName, Priority.ALWAYS);
        GridPane.setHgrow(cbRole,        Priority.ALWAYS);

        int row = 0;
        grid.add(new Label("Username:"), 0, row); grid.add(tfUsername,    1, row++);
        grid.add(new Label("Nickname:"), 0, row); grid.add(tfDisplayName, 1, row++);
        if (!isEdit) {
            grid.add(new Label("Mật khẩu:"), 0, row); grid.add(tfPassword, 1, row++);
        }
        grid.add(new Label("Vai trò:"),  0, row); grid.add(cbRole,        1, row++);

        dialog.getDialogPane().setContent(grid);

        // 22.3-E1 Username bị bỏ trống → vô hiệu hoá nút Thêm
        javafx.scene.Node okNode = dialog.getDialogPane().lookupButton(okBtn);
        okNode.setDisable(!isEdit);
        if (!isEdit) {
            tfUsername.textProperty().addListener((obs, oldVal, newVal) ->
                    okNode.setDisable(newVal.trim().isEmpty()));
        }

        final TextField finalTfPassword = tfPassword;
        dialog.setResultConverter(btnType -> {
            if (btnType != okBtn) return null; // 22.3-A1 / 22.4-A1

            User result = isEdit ? existing : new User();
            if (!isEdit) result.setUsername(tfUsername.getText().trim());

            // 22.3-A2 Biệt danh bị để trống → lấy username làm tên hiển thị
            String nick = tfDisplayName.getText().trim();
            result.setDisplayName(nick.isEmpty() ? tfUsername.getText().trim() : nick);

            result.setRole(cbRole.getValue() != null ? cbRole.getValue() : Role.PLAYER);
            if (!isEdit) result.setPasswordHash(finalTfPassword.getText().trim());
            result.setActive(true);
            return result;
        });

        return dialog;
    }

    // ── Cột bảng ─────────────────────────────────────────────────────────────

    private void setupColumns() {
        colSelect.setCellValueFactory(param -> new SimpleBooleanProperty(false));
        colSelect.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(e -> {
                    if (checkBox.isSelected()) {
                        userTable.getSelectionModel().select(getIndex());
                    } else {
                        userTable.getSelectionModel().clearSelection();
                    }
                    userTable.refresh();
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(userTable.getSelectionModel().getSelectedIndex() == getIndex());
                    setGraphic(checkBox);
                }
            }
        });

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colDisplayName.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        colRole.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getRole() != null ? data.getValue().getRole().getLabel() : "—"));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isActive() ? STATUS_ACTIVE : STATUS_LOCKED));
    }

    // ── Thông báo ─────────────────────────────────────────────────────────────

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

    // ── Đóng màn hình ─────────────────────────────────────────────────────────

    @FXML
    private void closePopup() {
        ((Stage) userTable.getScene().getWindow()).close();
    }
}
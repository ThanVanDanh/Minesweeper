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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import minesweeper.model.AuditLog;
import minesweeper.model.User;
import minesweeper.model.enums.Role;
import minesweeper.repository.MySqlUserRepository;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.repository.log.MySqlAuditLogRepository;
import minesweeper.repository.pagination.PagedResult;
import minesweeper.repository.spec.UserFilterSpec;
import minesweeper.service.ManagerUserService;
import minesweeper.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.CryptUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class AdminUserController {

    // ── FXML Controls ────────────────────────────────────────────────────────
    @FXML private TextField        searchField;
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

    @FXML private TableView<User>            userTable;
    @FXML private TableColumn<User, Boolean> colSelect;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String>  colUsername;
    @FXML private TableColumn<User, String>  colDisplayName;
    @FXML private TableColumn<User, String>  colRole;
    @FXML private TableColumn<User, String>  colStatus;

    // ── Constants ────────────────────────────────────────────────────────────
    private static final int    PAGE_SIZE        = 20;
    private static final String FILTER_ALL       = "Tất cả";
    private static final String STATUS_ACTIVE    = "Hoạt động";
    private static final String STATUS_LOCKED    = "Đã khoá";
    private static final String DEFAULT_PASSWORD = "123456";

    // ── State ────────────────────────────────────────────────────────────────
    private int            currentPage = 0;
    private int            totalPages  = 1;
    private UserFilterSpec activeSpec  = new UserFilterSpec();   // spec hiện tại

    private final ObservableList<User> pageItems = FXCollections.observableArrayList();

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final ManagerUserService      managerUserService;
    private final MySqlAuditLogRepository auditLogRepository;
    private static final Logger LOG = LoggerFactory.getLogger(AdminUserController.class);

    /** Constructor mặc định dùng trong production. */
    public AdminUserController() {
        this.managerUserService = new ManagerUserService(new MySqlUserRepository());
        this.auditLogRepository = new MySqlAuditLogRepository();
    }

    /** Constructor cho unit test (inject mock). */
    public AdminUserController(ManagerUserService managerUserService) {
        this.managerUserService = managerUserService;
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
        loadPage(); // 22.1.2 Hệ thống tải và hiển thị danh sách người dùng
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
     * Gọi server để lấy trang hiện tại theo activeSpec.
     * 22.1-E1 CSDL không thể kết nối → hiển thị hộp thoại lỗi, bảng để trống
     */
    private void loadPage() {
        try {
            PagedResult<User> result = managerUserService.findPaged(activeSpec, currentPage, PAGE_SIZE);

            totalPages = result.getTotalPages();
            currentPage = Math.min(currentPage, Math.max(0, totalPages - 1));

            pageItems.setAll(result.getContent());
            userTable.setItems(pageItems);

            pageLabel.setText("Trang " + (currentPage + 1) + " / " + totalPages);
            btnPrevPage.setDisable(currentPage == 0);
            btnNextPage.setDisable(currentPage >= totalPages - 1);

            refreshStats();

            statusLabel.setText("Tìm thấy " + result.getTotalElements() + " kết quả");

        } catch (DataAccessException e) {
            // 22.1-E1 CSDL không thể kết nối → hiển thị hộp thoại lỗi, bảng để trống
            showError("Không thể tải dữ liệu");
        }
    }

    /** Cập nhật 4 nhãn thống kê bằng server-side count (không load toàn bộ list). */
    private void refreshStats() {
        try {
            ManagerUserService.UserStats stats = managerUserService.getStats();
            totalUsersLabel.setText(String.valueOf(stats.total()));
            activeUsersLabel.setText(String.valueOf(stats.active()));
            lockedUsersLabel.setText(String.valueOf(stats.locked()));
            adminCountLabel.setText(String.valueOf(stats.adminCount()));
        } catch (DataAccessException e) {
            LOG.warn("Failed to refresh stats", e);
        }
    }

    // =========================================================================
    // Alternative Flow – UC-22.2 Tìm kiếm người dùng
    // =========================================================================

    /**
     * 22.2.1 Admin nhập từ khóa và chọn bộ lọc (Vai trò/Trạng thái) rồi nhấn Tìm kiếm
     * 22.2-A1 Admin xoá hết điều kiện rồi nhấn Tìm kiếm → khôi phục danh sách ban đầu
     */
    @FXML
    public void onSearch() {
        String keyword      = searchField.getText().trim();
        String roleFilter   = cbRoleFilter.getValue();
        String statusFilter = cbStatusFilter.getValue();

        boolean noFilter = keyword.isEmpty()
                && FILTER_ALL.equals(roleFilter)
                && FILTER_ALL.equals(statusFilter);

        if (noFilter) {
            // 22.2-A1 Admin xoá hết điều kiện → spec trống, tải lại toàn bộ
            activeSpec = new UserFilterSpec();
        } else {
            // 22.2.2 Hệ thống lọc danh sách server-side
            activeSpec = buildFilterSpec(keyword, roleFilter, statusFilter);
        }

        currentPage = 0;
        loadPage();
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
        activeSpec  = new UserFilterSpec();
        currentPage = 0;
        loadPage();
    }

    /** Dựng UserFilterSpec từ các giá trị bộ lọc trên UI. */
    private UserFilterSpec buildFilterSpec(String keyword, String roleFilter, String statusFilter) {
        UserFilterSpec spec = new UserFilterSpec();

        if (!keyword.isEmpty())              spec.keyword = keyword;

        if ("Người chơi".equals(roleFilter))      spec.role = Role.PLAYER;
        else if ("Quản trị viên".equals(roleFilter)) spec.role = Role.ADMIN;

        if (STATUS_ACTIVE.equals(statusFilter))  spec.active = true;
        else if (STATUS_LOCKED.equals(statusFilter)) spec.active = false;

        return spec;
    }

    // =========================================================================
    // Alternative Flow – UC-22.3 Thêm người dùng
    // =========================================================================

    /**
     * 22.3.1 Admin nhấn nút thêm người dùng
     * 22.3-A1 Admin nhấn Huỷ
     * 22.3-E  Ghi log vào CSDL thất bại
     * 22.3-E1 (username trùng, mất kết nối...) → hiển thị hộp thoại lỗi
     */
    @FXML
    public void onAddUser() {
        // 22.3.2 Hệ thống hiển thị dialog nhập thông tin người dùng
        Dialog<User> dialog = buildUserDialog(null);
        Optional<User> result = dialog.showAndWait(); // 22.3-A1 Admin nhấn Huỷ → đóng, không lưu

        // 22.3.3 Admin nhập thông tin và nhấn nút thêm
        result.ifPresent(newUser -> {
            try {
                // 22.3.4 Hệ thống hash mật khẩu và lưu trữ người dùng vào CSDL
                String passwordHash = CryptUtils.md5(newUser.getPasswordHash());
                long generatedId = managerUserService.createUser(
                        newUser.getUsername(),
                        newUser.getDisplayName(),
                        newUser.getRole(),
                        passwordHash
                );

                // 22.3.5 Hệ thống gán Id trả về rồi tải lại trang cuối để hiện user mới
                newUser.setId((int) generatedId);
                loadPage();
                currentPage = totalPages - 1;
                loadPage();
                statusLabel.setText("Đã thêm user: " + newUser.getUsername());

                // 22.3.6 Hệ thống ghi nhận Log vào CSDL
                writeAuditLog("CREATE_USER",
                        "user:" + newUser.getUsername(),
                        "Created new user; Role: " + labelOf(newUser.getRole()));

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
     * 22.4-E  Ghi log vào CSDL thất bại
     */
    @FXML
    public void onEditUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();

        // 22.4-E1 Chưa chọn User → hiển thị thông báo chọn User
        if (selected == null) {
            showInfo("Hãy chọn user cần sửa");
            return;
        }

        // BACKUP giá trị cho logging
        String oldDisplayName = selected.getDisplayName();
        Role   oldRole        = selected.getRole();

        // 22.4.3 Hệ thống hiển thị dialog sửa người dùng
        Dialog<User> dialog = buildUserDialog(selected);
        Optional<User> result = dialog.showAndWait(); // 22.4-A1 Admin nhấn hủy → đóng, không thay đổi

        // 22.4.4 Admin cập nhật nickname hoặc vai trò và nhấn cập nhật
        result.ifPresent(updated -> {
            try {
                // 22.4.5 Hệ thống cập nhật thông tin vào CSDL
                managerUserService.updateDisplayName(selected.getId(), updated.getDisplayName());
                managerUserService.updateRole(selected.getId(), updated.getRole());

                // 22.4.6 Hệ thống load lại trang hiện tại
                loadPage();
                statusLabel.setText("Đã cập nhật: " + selected.getUsername());

                // 22.4.7 Hệ thống ghi nhận Log vào CSDL
                String changes = "DisplayName: " + oldDisplayName + " → " + updated.getDisplayName()
                        + "; Role: " + labelOf(oldRole) + " → " + labelOf(updated.getRole());
                writeAuditLog("UPDATE_USER", "user:" + selected.getUsername(), changes);

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
     * 22.5.1 Admin chọn User trong bảng và nhấn nút khóa/mở khóa
     * 22.5-E  Ghi log vào CSDL thất bại
     * 22.5-E1 Chưa chọn User, nhấn khóa/mở khóa
     * 22.5-E2 Lỗi CSDL
     */
    @FXML
    public void onLockUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();

        // 22.5-E1 Chưa chọn User → hiển thị thông báo chọn User
        if (selected == null) {
            showInfo("Hãy chọn user");
            return;
        }

        try {
            // 22.5.2 Hệ thống cập nhật vào CSDL
            boolean newStatus = !selected.isActive();
            managerUserService.setActive(selected.getId(), newStatus);

            // 22.5.3 Hệ thống load lại trang hiện tại
            loadPage();
            statusLabel.setText(newStatus
                    ? "Đã mở khoá: " + selected.getUsername()
                    : "Đã khoá: "    + selected.getUsername());

            // 22.5.4 Hệ thống ghi nhận Log vào CSDL
            String action  = newStatus ? "UNLOCK_USER" : "LOCK_USER";
            String details = newStatus ? "Unlocked user account" : "Locked user account";
            writeAuditLog(action, "user:" + selected.getUsername(), details);

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
     * 22.6-E  Ghi log vào CSDL thất bại
     */
    @FXML
    public void onDeleteUser() {
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
            // 22.6.3 Admin xác nhận OK → 22.6.4 Hệ thống xóa User khỏi CSDL
            managerUserService.deleteUser(selected.getId());

            // 22.6.5 Hệ thống load lại trang (lùi 1 trang nếu trang hiện tại đã trống)
            loadPage();
            if (pageItems.isEmpty() && currentPage > 0) {
                currentPage--;
                loadPage();
            }
            statusLabel.setText("Đã xoá user: " + selected.getUsername());

            // 22.6.6 Hệ thống ghi nhận Log vào CSDL
            String details = "Deleted user; Original Role: " + labelOf(selected.getRole())
                    + "; Status: " + (selected.isActive() ? "Active" : "Locked");
            writeAuditLog("DELETE_USER", "user:" + selected.getUsername(), details);

        } catch (Exception e) {
            // 22.6-E2 Lỗi CSDL → hiển thị hộp thoại lỗi, bộ nhớ không thay đổi
            showError("Xoá thất bại");
        }
    }

    // =========================================================================
    // Phân trang
    // =========================================================================

    @FXML
    public void onPrevPage() {
        if (currentPage > 0) {
            currentPage--;
            loadPage();
        }
    }

    @FXML
    public void onNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadPage();
        }
    }

    // =========================================================================
    // Audit Log Dialog
    // =========================================================================

    @FXML
    public void onViewAuditLog() {
        try {
            List<AuditLog> logs = auditLogRepository.findRecent(100);
            showAuditLogDialog(logs);
        } catch (Exception e) {
            LOG.error("Failed to load audit logs", e);
            showError("Không thể tải log: " + e.getMessage());
        }
    }

    private void showAuditLogDialog(List<AuditLog> logs) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Audit Log - Nhật ký hành động");
        dialog.setHeaderText("Lịch sử hành động của admin (" + logs.size() + " entries)");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox vbox = new VBox();
        vbox.setSpacing(10);
        vbox.setPadding(new Insets(15));
        vbox.setStyle("-fx-background-color: linear-gradient(to bottom right, "
                + "rgba(5,20,28,0.95), rgba(5,10,18,0.95));");

        TableView<AuditLog> logTable = new TableView<>();
        logTable.setStyle("-fx-control-inner-background: rgba(10,20,30,0.8); -fx-text-fill: white;");
        logTable.setPrefHeight(500);
        logTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<AuditLog, Long> colLogId = new TableColumn<>("ID");
        colLogId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colLogId.setPrefWidth(60);

        TableColumn<AuditLog, Long> colAdminId = new TableColumn<>("Admin ID");
        colAdminId.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getAdminId()));
        colAdminId.setPrefWidth(80);

        TableColumn<AuditLog, String> colAction = new TableColumn<>("Action");
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colAction.setPrefWidth(140);
        colAction.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if      (item.contains("DELETE")) setStyle("-fx-text-fill: #ef5350; -fx-font-weight: bold;");
                else if (item.contains("LOCK"))   setStyle("-fx-text-fill: #ff9d2e; -fx-font-weight: bold;");
                else if (item.contains("CREATE")) setStyle("-fx-text-fill: #39ff8f; -fx-font-weight: bold;");
                else                              setStyle("-fx-text-fill: #2dd4f0; -fx-font-weight: bold;");
            }
        });

        TableColumn<AuditLog, String> colTarget = new TableColumn<>("Target");
        colTarget.setCellValueFactory(new PropertyValueFactory<>("target"));
        colTarget.setPrefWidth(150);

        TableColumn<AuditLog, String> colDetails = new TableColumn<>("Details");
        colDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        colDetails.setPrefWidth(300);
        colDetails.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setWrapText(!empty && item != null);
            }
        });

        TableColumn<AuditLog, String> colCreatedAt = new TableColumn<>("Thời gian");
        colCreatedAt.setCellValueFactory(data -> {
            LocalDateTime dt = data.getValue().getCreatedAt();
            String fmt = dt != null
                    ? dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                    : "N/A";
            return new SimpleStringProperty(fmt);
        });
        colCreatedAt.setPrefWidth(150);

        logTable.getColumns().addAll(colLogId, colAdminId, colAction, colTarget, colDetails, colCreatedAt);
        logTable.setItems(FXCollections.observableArrayList(logs));

        vbox.getChildren().add(logTable);
        dialog.getDialogPane().setContent(vbox);
        dialog.setWidth(1200);
        dialog.setHeight(600);
        dialog.showAndWait();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Dialog dùng chung cho Thêm (existing=null) và Sửa (existing≠null).
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
                isEdit ? "Cập nhật" : "Thêm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField      tfUsername    = new TextField(isEdit ? existing.getUsername() : "");
        TextField      tfDisplayName = new TextField(
                isEdit && existing.getDisplayName() != null ? existing.getDisplayName() : "");
        TextField      tfPassword    = new TextField(DEFAULT_PASSWORD);
        ComboBox<Role> cbRole        = new ComboBox<>(
                FXCollections.observableArrayList(Role.values()));

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
        grid.add(new Label("Vai trò:"),  0, row); grid.add(cbRole, 1, row);

        dialog.getDialogPane().setContent(grid);

        // 22.3-E1 Username bị bỏ trống → vô hiệu hoá nút Thêm
        javafx.scene.Node okNode = dialog.getDialogPane().lookupButton(okBtn);
        okNode.setDisable(!isEdit);
        if (!isEdit) {
            tfUsername.textProperty().addListener((obs, oldVal, newVal) ->
                    okNode.setDisable(newVal.trim().isEmpty()));
        }

        dialog.setResultConverter(btnType -> {
            if (btnType != okBtn) return null; // 22.3-A1 / 22.4-A1

            User resultUser = isEdit ? existing : new User();
            if (!isEdit) resultUser.setUsername(tfUsername.getText().trim());

            // 22.3-A2 Biệt danh bị để trống → lấy username làm tên hiển thị
            String nick = tfDisplayName.getText().trim();
            resultUser.setDisplayName(nick.isEmpty() ? tfUsername.getText().trim() : nick);

            resultUser.setRole(cbRole.getValue() != null ? cbRole.getValue() : Role.PLAYER);
            if (!isEdit) resultUser.setPasswordHash(tfPassword.getText().trim());
            resultUser.setActive(true);
            return resultUser;
        });

        return dialog;
    }

    private void setupColumns() {
        colSelect.setCellValueFactory(param -> new SimpleBooleanProperty(false));
        colSelect.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(e -> {
                    if (checkBox.isSelected()) userTable.getSelectionModel().select(getIndex());
                    else                       userTable.getSelectionModel().clearSelection();
                    userTable.refresh();
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                checkBox.setSelected(userTable.getSelectionModel().getSelectedIndex() == getIndex());
                setGraphic(checkBox);
            }
        });

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colDisplayName.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        colRole.setCellValueFactory(data -> new SimpleStringProperty(labelOf(data.getValue().getRole())));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isActive() ? STATUS_ACTIVE : STATUS_LOCKED));
    }

    /**
     * Ghi một bản ghi audit log. Lỗi chỉ được log warn, không ném lên UI.
     * 22.x-E Ghi log vào CSDL thất bại
     */
    private void writeAuditLog(String action, String target, String details) {
        try {
            Long adminId = SessionManager.isLoggedIn()
                    ? SessionManager.getCurrentUser().getId()
                    : null;
            auditLogRepository.insert(new AuditLog(adminId, action, target, details));
            LOG.info("[AUDIT] Admin {} {} → {}", adminId, action, target);
        } catch (Exception e) {
            LOG.warn("Failed to write audit log: action={}, target={}", action, target, e);
        }
    }

    private String labelOf(Role role) {
        return role != null ? role.getLabel() : "N/A";
    }

    private boolean confirmDelete(String username) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Xoá user \"" + username + "\"?");
        alert.setContentText("Hành động này không thể hoàn tác.");
        Optional<ButtonType> btn = alert.showAndWait();
        return btn.isPresent() && btn.get() == ButtonType.OK;
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

    @FXML
    private void closePopup() {
        ((Stage) userTable.getScene().getWindow()).close();
    }
}
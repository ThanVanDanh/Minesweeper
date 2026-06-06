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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class AdminUserController {

    // ── FXML Controls ────────────────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private TextField        emailField;
    @FXML private ComboBox<String> cbRoleFilter;
    @FXML private ComboBox<String> cbStatusFilter;
    @FXML private DatePicker       createdFromPicker;
    @FXML private DatePicker       createdToPicker;
    @FXML private DatePicker       lastLoginFromPicker;
    @FXML private DatePicker       lastLoginToPicker;
    @FXML private TextField        minGamesField;
    @FXML private TextField        maxGamesField;
    @FXML private ComboBox<String> cbGameHistoryFilter;

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
    @FXML private TableColumn<User, String>  colEmail;
    @FXML private TableColumn<User, String>  colRole;
    @FXML private TableColumn<User, String>  colStatus;
    @FXML private TableColumn<User, String>  colCreatedAt;
    @FXML private TableColumn<User, String>  colLastLoginAt;
    @FXML private TableColumn<User, Integer> colGameCount;

    // ── Constants ────────────────────────────────────────────────────────────
    private static final int    PAGE_SIZE        = 20;
    private static final String FILTER_ALL       = "Tất cả";
    private static final String STATUS_ACTIVE    = "Hoạt động";
    private static final String STATUS_LOCKED    = "Đã khoá";
    private static final String GAME_FILTER_HAS  = "Có kết quả";
    private static final String GAME_FILTER_NONE = "Chưa có kết quả";
    private static final String DEFAULT_PASSWORD = "123456";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── State ────────────────────────────────────────────────────────────────
    private int            currentPage = 0;
    private int            totalPages  = 1;
    private UserFilterSpec activeSpec  = new UserFilterSpec();

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
    // Basic Flow – UC-05.1 Xem danh sách người chơi
    // =========================================================================

    @FXML
    public void initialize() {
        // 05.1.1 Admin nhấn chọn mục "Quản lý người chơi" từ thanh điều hướng
        // 05.1.2 Hệ thống hiển thị màn hình quản lý người chơi và các nút chức năng
        setupFilterComboBoxes();
        setupTable();

        // 05.1.3 Hệ thống tự động tải danh sách người chơi,
        //          hiển thị tối đa 20 người mỗi trang cùng thanh điều hướng phân trang
        loadPage();
    }

    private void setupFilterComboBoxes() {
        cbRoleFilter.setItems(FXCollections.observableArrayList(FILTER_ALL, "Người chơi", "Quản trị viên"));
        cbRoleFilter.getSelectionModel().selectFirst();

        cbStatusFilter.setItems(FXCollections.observableArrayList(FILTER_ALL, STATUS_ACTIVE, STATUS_LOCKED));
        cbStatusFilter.getSelectionModel().selectFirst();

        cbGameHistoryFilter.setItems(FXCollections.observableArrayList(
                FILTER_ALL, GAME_FILTER_HAS, GAME_FILTER_NONE));
        cbGameHistoryFilter.getSelectionModel().selectFirst();
    }

    private void setupTable() {
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        userTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        setupColumns();
    }

    /**
     * Tải trang hiện tại theo activeSpec.
     * 05.1-E1 CSDL không thể kết nối → hiển thị hộp thoại lỗi, bảng để trống
     */
    private void loadPage() {
        try {
            PagedResult<User> result = managerUserService.findPaged(activeSpec, currentPage, PAGE_SIZE);

            totalPages  = result.getTotalPages();
            currentPage = Math.min(currentPage, Math.max(0, totalPages - 1));

            pageItems.setAll(result.getContent());
            userTable.setItems(pageItems);

            pageLabel.setText("Trang " + (currentPage + 1) + " / " + totalPages);
            btnPrevPage.setDisable(currentPage == 0);
            btnNextPage.setDisable(currentPage >= totalPages - 1);

            refreshStats();

            statusLabel.setText("Tìm thấy " + result.getTotalElements() + " kết quả");

        } catch (DataAccessException e) {
            // 05-E1 CSDL không thể kết nối
            showError("Không thể tải dữ liệu");
        }
    }

    /** Cập nhật 4 nhãn thống kê bằng server-side count. */
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
    // Alternative Flow – UC-05.2 Tìm kiếm người dùng
    // =========================================================================

    @FXML
    public void onSearch() {
        // 05.2.1 Admin nhập từ khoá vào ô tìm kiếm và/hoặc chọn bộ lọc Vai trò,
        //          Trạng thái rồi nhấn Tìm kiếm
        String keyword      = searchField.getText().trim();
        String email        = emailField.getText().trim();
        String roleFilter   = cbRoleFilter.getValue();
        String statusFilter = cbStatusFilter.getValue();
        String gameFilter   = cbGameHistoryFilter.getValue();

        boolean noFilter = keyword.isEmpty()
                && email.isEmpty()
                && FILTER_ALL.equals(roleFilter)
                && FILTER_ALL.equals(statusFilter)
                && createdFromPicker.getValue() == null
                && createdToPicker.getValue() == null
                && lastLoginFromPicker.getValue() == null
                && lastLoginToPicker.getValue() == null
                && minGamesField.getText().trim().isEmpty()
                && maxGamesField.getText().trim().isEmpty()
                && FILTER_ALL.equals(gameFilter);

        if (noFilter) {
            // 05.2-A1 Admin xoá hết điều kiện rồi nhấn Tìm kiếm
            //           → Hệ thống truy vấn lại toàn bộ danh sách không kèm bộ lọc,
            //             hiển thị từ trang đầu tiên
            activeSpec = new UserFilterSpec();
        } else {
            // 05.2.2 Hệ thống gửi điều kiện tìm kiếm xuống CSDL, lấy về danh sách phù hợp
            try {
                activeSpec = buildFilterSpec(
                        keyword,
                        email,
                        roleFilter,
                        statusFilter,
                        gameFilter,
                        createdFromPicker.getValue(),
                        createdToPicker.getValue(),
                        lastLoginFromPicker.getValue(),
                        lastLoginToPicker.getValue(),
                        minGamesField.getText(),
                        maxGamesField.getText()
                );
            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
                return;
            }
        }

        // 05.2.3 Hệ thống hiển thị từ trang đầu tiên và cập nhật số lượng kết quả tìm được
        currentPage = 0;
        loadPage();
    }

    @FXML
    public void onRefresh() {
        // 05.2-A2 Admin nhấn Làm mới
        //           → Hệ thống xoá ô tìm kiếm, đưa tất cả bộ lọc về mặc định "Tất cả",
        //             truy vấn lại CSDL và hiển thị toàn bộ danh sách từ trang đầu tiên
        searchField.clear();
        emailField.clear();
        cbRoleFilter.getSelectionModel().selectFirst();
        cbStatusFilter.getSelectionModel().selectFirst();
        cbGameHistoryFilter.getSelectionModel().selectFirst();
        createdFromPicker.setValue(null);
        createdToPicker.setValue(null);
        lastLoginFromPicker.setValue(null);
        lastLoginToPicker.setValue(null);
        minGamesField.clear();
        maxGamesField.clear();
        activeSpec  = new UserFilterSpec();
        currentPage = 0;
        loadPage();
    }

    /**
     * Dựng UserFilterSpec từ các giá trị bộ lọc trên UI.
     * Throw IllegalArgumentException để hiển thị lỗi validate (không gọi xuống DB).
     */
    private UserFilterSpec buildFilterSpec(
            String keyword,
            String email,
            String roleFilter,
            String statusFilter,
            String gameFilter,
            LocalDate createdFrom,
            LocalDate createdTo,
            LocalDate lastLoginFrom,
            LocalDate lastLoginTo,
            String minGamesText,
            String maxGamesText
    ) {
        UserFilterSpec spec = new UserFilterSpec();

        String kw = keyword != null ? keyword.trim() : "";
        if (!kw.isEmpty()) spec.keyword = kw;

        String em = email != null ? email.trim() : "";
        if (!em.isEmpty()) spec.email = em;

        if ("Người chơi".equals(roleFilter))         spec.role = Role.PLAYER;
        else if ("Quản trị viên".equals(roleFilter)) spec.role = Role.ADMIN;

        if (STATUS_ACTIVE.equals(statusFilter))      spec.active = true;
        else if (STATUS_LOCKED.equals(statusFilter)) spec.active = false;

        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new IllegalArgumentException("Khoảng ngày tạo không hợp lệ (Từ > Đến). ");
        }
        spec.createdFrom = createdFrom;
        spec.createdTo   = createdTo;

        if (lastLoginFrom != null && lastLoginTo != null && lastLoginFrom.isAfter(lastLoginTo)) {
            throw new IllegalArgumentException("Khoảng đăng nhập gần nhất không hợp lệ (Từ > Đến). ");
        }
        spec.lastLoginFrom = lastLoginFrom;
        spec.lastLoginTo   = lastLoginTo;

        Integer minGames = parseNonNegativeInt(minGamesText, "Số ván (min)");
        Integer maxGames = parseNonNegativeInt(maxGamesText, "Số ván (max)");
        if (minGames != null && maxGames != null && minGames > maxGames) {
            throw new IllegalArgumentException("Số ván không hợp lệ (min > max). ");
        }
        spec.minGames = minGames;
        spec.maxGames = maxGames;

        if (GAME_FILTER_HAS.equals(gameFilter)) {
            spec.hasGameResults = true;
        } else if (GAME_FILTER_NONE.equals(gameFilter)) {
            spec.hasGameResults = false;
        } else {
            spec.hasGameResults = null;
        }

        return spec;
    }

    private Integer parseNonNegativeInt(String text, String fieldLabel) {
        if (text == null) return null;
        String s = text.trim();
        if (s.isEmpty()) return null;
        try {
            int v = Integer.parseInt(s);
            if (v < 0) throw new IllegalArgumentException();
            return v;
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldLabel + " phải là số nguyên không âm.");
        }
    }

    // =========================================================================
    // Alternative Flow – UC-05.3 Thêm người dùng
    // =========================================================================

    @FXML
    public void onAddUser() {
        // 05.3.1 Admin nhấn nút Thêm người chơi
        // 05.3.2 Hệ thống hiển thị dialog nhập thông tin người dùng
        Dialog<User> dialog = buildUserDialog(null);

        // 05.3-A1 Admin nhấn Huỷ → cửa sổ đóng lại, không lưu thay đổi
        Optional<User> result = dialog.showAndWait();

        // 05.3.3 Admin nhập thông tin và nhấn nút thêm
        result.ifPresent(newUser -> {
            try {
                // 05.3.4 Hệ thống hash mật khẩu và lưu trữ người dùng vào CSDL
                String passwordHash = CryptUtils.hashPassword(newUser.getPasswordHash());
                long generatedId = managerUserService.createUser(
                        newUser.getUsername(),
                        newUser.getDisplayName(),
                        newUser.getRole(),
                        passwordHash
                );

                // 05.3.5 Hệ thống gán Id trả về cho User mới và cập nhật danh sách
                newUser.setId((int) generatedId);
                loadPage();
                currentPage = totalPages - 1;
                loadPage();
                statusLabel.setText("Đã thêm user: " + newUser.getUsername());

                // 05.3.6 Hệ thống ghi nhận Log vào CSDL
                writeAuditLog("CREATE_USER",
                        "user:" + newUser.getUsername(),
                        "Created new user; Role: " + labelOf(newUser.getRole()));

            } catch (Exception e) {
                // 05.3-E1 Username trùng hoặc lỗi CSDL → hiển thị hộp thoại lỗi
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                showError("Thêm user thất bại!\n"
                        + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + (cause != e ? "\nCause: " + cause.getMessage() : ""));
            }
        });
    }

    // =========================================================================
    // Alternative Flow – UC-05.4 Chỉnh sửa thông tin người dùng
    // =========================================================================

    @FXML
    public void onEditUser() {
        // 05.4.1 Admin chọn user trong bảng
        // 05.4.2 Admin nhấn nút chỉnh sửa
        User selected = userTable.getSelectionModel().getSelectedItem();

        // 05.4-E1 Chưa chọn User, nhấn chỉnh sửa → hiển thị thông báo chọn User
        if (selected == null) {
            showInfo("Hãy chọn user cần sửa");
            return;
        }

        String oldDisplayName = selected.getDisplayName();
        Role   oldRole        = selected.getRole();

        // 05.4.3 Hệ thống hiển thị dialog sửa người dùng
        Dialog<User> dialog = buildUserDialog(selected);

        // 05.4-A1 Admin nhấn hủy → đóng dialog, không có thay đổi
        Optional<User> result = dialog.showAndWait();

        // 05.4.4 Admin cập nhật nickname hoặc vai trò và nhấn cập nhật
        result.ifPresent(updated -> {
            try {
                // 05.4.5 Hệ thống cập nhật thông tin vào CSDL
                managerUserService.updateDisplayName(selected.getId(), updated.getDisplayName());
                managerUserService.updateRole(selected.getId(), updated.getRole());

                // 05.4.6 Hệ thống load lại danh sách
                loadPage();
                statusLabel.setText("Đã cập nhật: " + selected.getUsername());

                // 05.4.7 Hệ thống ghi nhận Log vào CSDL
                String changes = "DisplayName: " + oldDisplayName + " → " + updated.getDisplayName()
                        + "; Role: " + labelOf(oldRole) + " → " + labelOf(updated.getRole());
                writeAuditLog("UPDATE_USER", "user:" + selected.getUsername(), changes);

            } catch (Exception e) {
                // 05-E1 CSDL không thể kết nối
                showError("Sửa thất bại: " + e.getMessage());
            }
        });
    }

    // =========================================================================
    // Alternative Flow – UC-05.5 Khoá / Mở khoá tài khoản
    // =========================================================================

    @FXML
    public void onLockUser() {
        // 05.5.1 Admin chọn User trong bảng và nhấn nút Khoá / Mở khoá
        User selected = userTable.getSelectionModel().getSelectedItem();

        // 05.5-E1 Chưa chọn User, nhấn khoá → hiển thị thông báo chọn User
        // 05.5.2 [CẢI TIẾN v1.1 – A4] Kiểm tra: nếu User được chọn chính là Admin đang
        //          đăng nhập, hiển thị lỗi và dừng.
        // 05.5-E2 Admin tự khoá mình → hiển thị lỗi
        try {
            validateNotSelfAction(selected, "khoá");
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Hãy chọn")) {
                showInfo(e.getMessage());
            } else {
                showError(e.getMessage());
            }
            return;
        }

        // 05.5.3 [CẢI TIẾN v1.1 – A3] Hệ thống hiển thị dialog xác nhận
        //          Khoá: "• Tài khoản bị khoá sẽ không thể đăng nhập."
        //          Mở khoá: "• Tài khoản sẽ được phép đăng nhập trở lại."
        // 05.5-A1 Admin nhấn Huỷ → Đóng dialog, không thay đổi trạng thái
        boolean isLocking = selected.isActive();
        if (!confirmLock(selected.getUsername(), isLocking)) {
            return;
        }

        try {
            // 05.5.4 Admin xác nhận OK → Hệ thống cập nhật trạng thái is_active vào CSDL
            boolean newStatus = !isLocking;
            managerUserService.setActive(selected.getId(), newStatus);

            // 05.5.5 Hệ thống load lại danh sách, cột Trạng thái cập nhật badge màu tương ứng
            loadPage();
            statusLabel.setText(newStatus
                    ? "Đã mở khoá: " + selected.getUsername()
                    : "Đã khoá: "    + selected.getUsername());

            // 05.5.6 Hệ thống ghi nhận Log vào CSDL (action: LOCK_USER / UNLOCK_USER)
            String action  = newStatus ? "UNLOCK_USER" : "LOCK_USER";
            String details = newStatus ? "Unlocked user account" : "Locked user account";
            writeAuditLog(action, "user:" + selected.getUsername(), details);

        } catch (Exception e) {
            // 05-E1 CSDL không thể kết nối
            showError("Khoá/mở khoá thất bại");
        }
    }

    // =========================================================================
    // Alternative Flow – UC-05.6 Xoá người dùng
    // =========================================================================

    @FXML
    public void onDeleteUser() {
        // 05.6.1 Admin chọn User trong bảng và nhấn nút Xóa
        User selected = userTable.getSelectionModel().getSelectedItem();

        // 05.6-E1 Chưa chọn User, nhấn xóa → hiển thị thông báo chọn User
        // 05.6.2 [CẢI TIẾN v1.1 – A4] Kiểm tra: nếu User được chọn chính là Admin đang
        //          đăng nhập, hiển thị lỗi và dừng.
        // 05.6-E2 Admin tự xóa mình → hiển thị lỗi
        try {
            validateNotSelfAction(selected, "xóa");
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Hãy chọn")) {
                showInfo(e.getMessage());
            } else {
                showError(e.getMessage());
            }
            return;
        }

        // 05.6.3 Hệ thống hiển thị hộp thoại xác nhận:
        //          "Xoá user [tên]? Hành động này không thể hoàn tác."
        // 05.6-A1 Admin nhấn hủy → đóng dialog, không có thay đổi
        if (!confirmDelete(selected.getUsername())) {
            return;
        }

        try {
            // 05.6.4 Admin xác nhận OK → Hệ thống xóa User khỏi CSDL
            //          (CASCADE xóa game_sessions liên quan)
            managerUserService.deleteUser(selected.getId());

            // 05.6.5 Hệ thống load lại danh sách
            //          (lùi 1 trang nếu trang hiện tại đã trống sau khi xóa)
            loadPage();
            if (pageItems.isEmpty() && currentPage > 0) {
                currentPage--;
                loadPage();
            }
            statusLabel.setText("Đã xoá user: " + selected.getUsername());

            // 05.6.6 Hệ thống ghi nhận Log vào CSDL với vai trò và trạng thái gốc
            //          (action: DELETE_USER)
            String details = "Deleted user; Original Role: " + labelOf(selected.getRole())
                    + "; Status: " + (selected.isActive() ? "Active" : "Locked");
            writeAuditLog("DELETE_USER", "user:" + selected.getUsername(), details);

        } catch (Exception e) {
            // 05-E1 CSDL không thể kết nối
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
    // Xem lịch sử thao tác (Audit Log)
    // =========================================================================

    @FXML
    public void onViewAuditLog() {
        // 05.1.1.4 Admin chọn chức năng: Xem lịch sử thao tác
        try {
            List<AuditLog> logs = auditLogRepository.findRecent(100);
            showAuditLogDialog(logs);
        } catch (Exception e) {
            // 05-E1 CSDL không thể kết nối
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
     * Dialog dùng chung cho Thêm (existing = null) và Sửa (existing ≠ null).
     *
     * 05.3-A2 Biệt danh bị để trống → Hệ thống tự động lấy Tên đăng nhập làm tên hiển thị
     * 05.3-E1 Username bị bỏ trống   → nút Thêm bị vô hiệu hoá
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

        // 05.3-E1 Username bị bỏ trống → vô hiệu hoá nút Thêm
        javafx.scene.Node okNode = dialog.getDialogPane().lookupButton(okBtn);
        okNode.setDisable(!isEdit);
        if (!isEdit) {
            tfUsername.textProperty().addListener((obs, oldVal, newVal) ->
                    okNode.setDisable(newVal.trim().isEmpty()));
        }

        dialog.setResultConverter(btnType -> {
            // 05.3-A1 / 05.4-A1 Admin nhấn Huỷ → trả về null, đóng dialog
            if (btnType != okBtn) return null;

            User resultUser = isEdit ? existing : new User();
            if (!isEdit) resultUser.setUsername(tfUsername.getText().trim());

            // 05.3-A2 Biệt danh bị để trống → lấy username làm tên hiển thị
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
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        colCreatedAt.setCellValueFactory(data -> new SimpleStringProperty(
                formatDateTime(data.getValue().getCreatedAt())));
        colLastLoginAt.setCellValueFactory(data -> new SimpleStringProperty(
                formatDateTime(data.getValue().getLastLoginAt())));
        colGameCount.setCellValueFactory(new PropertyValueFactory<>("gameCount"));

        colRole.setCellValueFactory(data -> new SimpleStringProperty(labelOf(data.getValue().getRole())));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isActive() ? STATUS_ACTIVE : STATUS_LOCKED));
    }

    /**
     * Ghi một bản ghi audit log vào CSDL.
     * Lỗi chỉ được ghi warn nội bộ, không hiển thị lên UI và không ném ngoại lệ.
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

    private String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(DATE_TIME_FORMATTER) : "N/A";
    }

    void validateNotSelfAction(User selected, String actionName) {
        if (selected == null) {
            throw new IllegalArgumentException("Hãy chọn user" + (actionName.equals("xóa") ? " cần xoá" : ""));
        }
        if (SessionManager.isLoggedIn() && SessionManager.getCurrentUser() != null
                && SessionManager.getCurrentUser().getId() == selected.getId()) {
            throw new IllegalArgumentException("Bạn không thể " + actionName + " tài khoản của chính mình!");
        }
    }

    private boolean confirmDelete(String username) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Xoá user \"" + username + "\"?");
        alert.setContentText("Hành động này không thể hoàn tác.");
        Optional<ButtonType> btn = alert.showAndWait();
        return btn.isPresent() && btn.get() == ButtonType.OK;
    }

    private boolean confirmLock(String username, boolean isLocking) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText((isLocking ? "Khóa" : "Mở khóa") + " user \"" + username + "\"?");
        alert.setContentText(isLocking ? "Tài khoản bị khóa sẽ không thể đăng nhập." : "Tài khoản sẽ được phép đăng nhập trở lại.");
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

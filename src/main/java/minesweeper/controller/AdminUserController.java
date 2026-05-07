package minesweeper.controller;

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
import minesweeper.model.Role;
import minesweeper.model.User;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.service.MySqlUserService;
import minesweeper.service.UserService;

import java.util.List;
import java.util.Optional;

public class AdminUserController {

    @FXML private TextField searchField;
    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label lockedUsersLabel;
    @FXML private Label adminCountLabel;
    @FXML private Label statusLabel;

    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Label pageLabel;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colDisplayName;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;

    private static final int PAGE_SIZE = 20;
    private int currentPage = 0;
    private int totalPages = 1;

    private boolean isFiltering = false;
    private final ObservableList<User> allUsers   = FXCollections.observableArrayList();
    private final ObservableList<User> filtered   = FXCollections.observableArrayList();
    private final ObservableList<User> pageItems  = FXCollections.observableArrayList();

    private UserService userService;

    public AdminUserController() {
        userService = new MySqlUserService();
    }

    @FXML
    public void initialize() {
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        setupColumns();
        loadUsers();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colDisplayName.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        colRole.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRole() != null
                        ? data.getValue().getRole().getLabel() : "—"));
        colStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isActive() ? "Hoạt động" : "Đã khoá"));
    }

    // ── Load ─────────────────────────────────────────────────────────────────

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
            showError("Không thể tải dữ liệu");
        }
    }

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

    private int calcTotalPages(int size) {
        return Math.max(1, (int) Math.ceil((double) size / PAGE_SIZE));
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

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

    // ── Search ────────────────────────────────────────────────────────────────

    @FXML
    public void onSearch() {
        String kw = searchField.getText().toLowerCase().trim();
        if (kw.isEmpty()) {
            isFiltering = false;
            filtered.clear();
        } else {
            filtered.clear();
            for (User u : allUsers) {
                boolean matchUser = u.getUsername().toLowerCase().contains(kw);
                boolean matchName = u.getDisplayName() != null
                        && u.getDisplayName().toLowerCase().contains(kw);
                if (matchUser || matchName) filtered.add(u);
            }
            isFiltering = true;
        }
        currentPage = 0;
        showPage();
        ObservableList<User> source = isFiltering ? filtered : allUsers;
        statusLabel.setText("Tìm thấy " + source.size() + " kết quả");
    }

    @FXML
    public void onRefresh() {
        searchField.clear();
        loadUsers();
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    @FXML
    public void onPrevPage() {
        if (currentPage > 0) { currentPage--; showPage(); }
    }

    @FXML
    public void onNextPage() {
        if (currentPage < totalPages - 1) { currentPage++; showPage(); }
    }

    // ── Add ───────────────────────────────────────────────────────────────────

    @FXML
    public void onAddUser() {
        Dialog<User> dialog = buildUserDialog(null);
        Optional<User> result = dialog.showAndWait();
        result.ifPresent(u -> {
            try {
                long id = userService.createUser(u.getUsername(), u.getDisplayName());
                u.setId((int) id);
                allUsers.add(u);
                updateStats(allUsers);
                currentPage = calcTotalPages(
                        isFiltering ? filtered.size() : allUsers.size()) - 1;
                showPage();
                statusLabel.setText("Đã thêm user: " + u.getUsername());
            } catch (Exception e) {
                showError("Thêm user thất bại: " + e.getMessage());
            }
        });
    }

    // ── Edit ──────────────────────────────────────────────────────────────────

    @FXML
    public void onEditUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Hãy chọn user cần sửa"); return; }

        Dialog<User> dialog = buildUserDialog(selected);
        Optional<User> result = dialog.showAndWait();
        result.ifPresent(u -> {
            try {
                userService.updateDisplayName(selected.getId(), u.getDisplayName());
                selected.setDisplayName(u.getDisplayName());
                userTable.refresh();
                statusLabel.setText("Đã cập nhật: " + selected.getUsername());
            } catch (Exception e) {
                showError("Sửa thất bại: " + e.getMessage());
            }
        });
    }

    // ── Lock / Unlock ─────────────────────────────────────────────────────────

    @FXML
    public void onLockUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Hãy chọn user"); return; }
        try {
            boolean newStatus = !selected.isActive();
            userService.setActive(selected.getId(), newStatus);
            selected.setActive(newStatus);
            userTable.refresh();
            updateStats(allUsers);
            statusLabel.setText(newStatus ? "Đã mở khoá: " + selected.getUsername()
                    : "Đã khoá: "    + selected.getUsername());
        } catch (Exception e) {
            showError("Khoá/mở khoá thất bại");
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @FXML
    public void onDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Hãy chọn user cần xoá"); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Xoá user \"" + selected.getUsername() + "\"?");
        confirm.setContentText("Hành động này không thể hoàn tác.");
        Optional<ButtonType> btn = confirm.showAndWait();
        if (btn.isEmpty() || btn.get() != ButtonType.OK) return;

        try {
            userService.deleteUser(selected.getId());
            allUsers.remove(selected);
            if (isFiltering) filtered.remove(selected);
            // Adjust page nếu trang hiện tại vượt quá tổng
            if (currentPage >= calcTotalPages(isFiltering ? filtered.size() : allUsers.size())) {
                currentPage = Math.max(0, currentPage - 1);
            }
            showPage();
            updateStats(allUsers);
            statusLabel.setText("Đã xoá user: " + selected.getUsername());
        } catch (Exception e) {
            showError("Xoá thất bại");
        }
    }

    // ── Dialog helper ─────────────────────────────────────────────────────────

    private Dialog<User> buildUserDialog(User existing) {
        boolean isEdit = existing != null;
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Sửa người dùng" : "Thêm người dùng");
        dialog.setHeaderText(isEdit ? "Cập nhật thông tin: " + existing.getUsername()
                : "Nhập thông tin người dùng mới");

        ButtonType okBtn = new ButtonType(isEdit ? "Cập nhật" : "Thêm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField tfUsername    = new TextField(isEdit ? existing.getUsername() : "");
        TextField tfDisplayName = new TextField(isEdit && existing.getDisplayName() != null
                ? existing.getDisplayName() : "");

        tfUsername.setPromptText("username");
        tfDisplayName.setPromptText("Tên hiển thị");
        GridPane.setHgrow(tfUsername, Priority.ALWAYS);
        GridPane.setHgrow(tfDisplayName, Priority.ALWAYS);

        if (isEdit) tfUsername.setDisable(true);

        grid.add(new Label("Username:"),    0, 0); grid.add(tfUsername,    1, 0);
        grid.add(new Label("Tên hiển thị:"), 0, 1); grid.add(tfDisplayName, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Validate
        javafx.scene.Node okNode = dialog.getDialogPane().lookupButton(okBtn);
        okNode.setDisable(!isEdit);
        tfUsername.textProperty().addListener((obs, o, n) ->
                okNode.setDisable(n.trim().isEmpty()));
        if (!isEdit) okNode.setDisable(true);

        dialog.setResultConverter(bt -> {
            if (bt == okBtn) {
                User u = isEdit ? existing : new User();
                if (!isEdit) u.setUsername(tfUsername.getText().trim());
                u.setDisplayName(tfDisplayName.getText().trim().isEmpty()
                        ? tfUsername.getText().trim()
                        : tfDisplayName.getText().trim());
                u.setActive(true);
                u.setRole(Role.PLAYER);
                return u;
            }
            return null;
        });

        return dialog;
    }

    // ── Alert helpers ─────────────────────────────────────────────────────────

    private void showInfo(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setContentText(message);
        a.showAndWait();
    }

    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setContentText(message);
        a.showAndWait();
    }
}
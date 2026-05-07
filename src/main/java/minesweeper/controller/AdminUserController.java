package minesweeper.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import minesweeper.model.Role;
import minesweeper.model.User;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.service.MySqlUserService;
import minesweeper.service.UserService;

import java.util.List;

public class AdminUserController {

    // ── FXML ─────────────────────────────────────────────────────────────────

    @FXML private TextField searchField;

    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label lockedUsersLabel;
    @FXML private Label adminCountLabel;
    @FXML private Label statusLabel;

    @FXML private TableView<User>            userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String>  colUsername;
    @FXML private TableColumn<User, String>  colDisplayName;
    @FXML private TableColumn<User, String>  colRole;
    @FXML private TableColumn<User, String>  colStatus;
    @FXML private TableColumn<User, Void>    colActions;

    // ── Data ─────────────────────────────────────────────────────────────────

    private final UserService userService;
    private final ObservableList<User> masterList = FXCollections.observableArrayList();

    // ── Constructor ───────────────────────────────────────────────────────────

    public AdminUserController() { this(new MySqlUserService()); }
    public AdminUserController(UserService userService) { this.userService = userService; }

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupColumns();
        loadUsers();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUsername.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle("-fx-text-fill:#2dd4f0;-fx-font-weight:900;");
            }
        });

        colDisplayName.setCellValueFactory(new PropertyValueFactory<>("displayName"));

        colRole.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getRole() != null
                                ? data.getValue().getRole().getLabel() : "—"));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle(v.equals(Role.ADMIN.getLabel())
                        ? "-fx-text-fill:#ff9d2e;-fx-font-weight:900;"
                        : "-fx-text-fill:rgba(45,212,240,0.75);");
            }
        });

        colStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isActive() ? "active" : "locked"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                if ("active".equals(v)) {
                    setText("✔ Hoạt động");
                    setStyle("-fx-text-fill:#39ff8f;-fx-font-weight:700;");
                } else {
                    setText("✖ Đã khoá");
                    setStyle("-fx-text-fill:#ef5350;-fx-font-weight:700;");
                }
            }
        });

        // Inline action buttons per row
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = styledBtn("✏ Sửa tên",
                    "-fx-background-color:rgba(45,212,240,0.12);-fx-border-color:rgba(45,212,240,0.5);-fx-border-radius:4;-fx-background-radius:4;-fx-text-fill:#2dd4f0;-fx-font-size:11;-fx-padding:5 9;-fx-cursor:hand;");
            private final Button btnLock   = styledBtn("🔒 Khoá",
                    "-fx-background-color:rgba(255,157,46,0.12);-fx-border-color:rgba(255,157,46,0.5);-fx-border-radius:4;-fx-background-radius:4;-fx-text-fill:#ff9d2e;-fx-font-size:11;-fx-padding:5 9;-fx-cursor:hand;");
            private final Button btnDelete = styledBtn("🗑 Xoá",
                    "-fx-background-color:rgba(198,40,40,0.12);-fx-border-color:rgba(198,40,40,0.55);-fx-border-radius:4;-fx-background-radius:4;-fx-text-fill:#ef5350;-fx-font-size:11;-fx-padding:5 9;-fx-cursor:hand;");
            private final HBox box = new HBox(6, btnEdit, btnLock, btnDelete);
            {
                box.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e   -> doEdit(getUser()));
                btnLock.setOnAction(e   -> doLock(getUser()));
                btnDelete.setOnAction(e -> doDelete(getUser()));
            }

            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                refreshLockBtn(getUser());
                setGraphic(box);
            }

            private User getUser() {
                return getTableView().getItems().get(getIndex());
            }

            private void refreshLockBtn(User u) {
                if (u.isActive()) {
                    btnLock.setText("🔒 Khoá");
                    btnLock.setStyle("-fx-background-color:rgba(255,157,46,0.12);-fx-border-color:rgba(255,157,46,0.5);-fx-border-radius:4;-fx-background-radius:4;-fx-text-fill:#ff9d2e;-fx-font-size:11;-fx-padding:5 9;-fx-cursor:hand;");
                } else {
                    btnLock.setText("🔓 Mở khoá");
                    btnLock.setStyle("-fx-background-color:rgba(57,255,143,0.10);-fx-border-color:rgba(57,255,143,0.45);-fx-border-radius:4;-fx-background-radius:4;-fx-text-fill:#39ff8f;-fx-font-size:11;-fx-padding:5 9;-fx-cursor:hand;");
                }
            }

            private Button styledBtn(String label, String style) {
                Button b = new Button(label); b.setStyle(style); return b;
            }
        });
    }

    // ── Load ─────────────────────────────────────────────────────────────────

    private void loadUsers() {
        try {
            List<User> users = userService.getAllUsers();
            masterList.setAll(users);
            userTable.setItems(masterList);
            updateStats();
            setStatus("Tải " + users.size() + " người dùng thành công.");
        } catch (DataAccessException e) {
            showError("Không thể tải danh sách người dùng:\n" + e.getMessage());
        }
    }

    private void updateStats() {
        long total  = masterList.size();
        long active = masterList.stream().filter(User::isActive).count();
        long locked = total - active;
        long admins = masterList.stream().filter(u -> u.getRole() == Role.ADMIN).count();

        if (totalUsersLabel  != null) totalUsersLabel.setText(String.valueOf(total));
        if (activeUsersLabel != null) activeUsersLabel.setText(String.valueOf(active));
        if (lockedUsersLabel != null) lockedUsersLabel.setText(String.valueOf(locked));
        if (adminCountLabel  != null) adminCountLabel.setText(String.valueOf(admins));
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @FXML public void onSearch() {
        String kw = searchField.getText().trim().toLowerCase();
        if (kw.isBlank()) {
            userTable.setItems(masterList);
            setStatus("");
            return;
        }
        ObservableList<User> filtered = FXCollections.observableArrayList();
        for (User u : masterList) {
            boolean m1 = u.getUsername()    != null && u.getUsername().toLowerCase().contains(kw);
            boolean m2 = u.getDisplayName() != null && u.getDisplayName().toLowerCase().contains(kw);
            boolean m3 = u.getEmail()       != null && u.getEmail().toLowerCase().contains(kw);
            if (m1 || m2 || m3) filtered.add(u);
        }
        userTable.setItems(filtered);
        setStatus("Tìm thấy " + filtered.size() + " kết quả.");
    }

    @FXML public void onRefresh() {
        searchField.clear();
        loadUsers();
    }

    // ── Toolbar actions ───────────────────────────────────────────────────────

    @FXML public void onEditUser()   { User u = getSelectedUser(); if (u != null) doEdit(u); }
    @FXML public void onLockUser()   { User u = getSelectedUser(); if (u != null) doLock(u); }
    @FXML public void onDeleteUser() { User u = getSelectedUser(); if (u != null) doDelete(u); }

    // ── Core operations (gọi DB thật) ─────────────────────────────────────────

    private void doEdit(User user) {
        TextInputDialog dlg = new TextInputDialog(
                user.getDisplayName() != null ? user.getDisplayName() : "");
        dlg.setTitle("Sửa tên hiển thị");
        dlg.setHeaderText("Tài khoản: " + user.getUsername());
        dlg.setContentText("Tên hiển thị mới:");

        dlg.showAndWait().ifPresent(newName -> {
            if (newName.isBlank()) { showError("Tên hiển thị không được để trống."); return; }
            try {
                userService.updateDisplayName(user.getId(), newName.trim()); // ← gọi DB
                user.setDisplayName(newName.trim());
                userTable.refresh();
                setStatus("✔ Đã cập nhật tên hiển thị của " + user.getUsername() + ".");
            } catch (DataAccessException e) {
                showError("Cập nhật thất bại:\n" + e.getMessage());
            }
        });
    }

    private void doLock(User user) {
        boolean willLock = user.isActive();
        String action = willLock ? "khoá" : "mở khoá";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận " + action);
        confirm.setHeaderText("Bạn muốn " + action + " tài khoản: " + user.getUsername() + "?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            try {
                userService.setActive(user.getId(), !willLock);
                user.setActive(!willLock);
                userTable.refresh();
                updateStats();
                setStatus("✔ Đã " + action + " tài khoản " + user.getUsername() + ".");
            } catch (DataAccessException e) {
                showError("Thao tác thất bại:\n" + e.getMessage());
            }
        });
    }

    private void doDelete(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xoá");
        confirm.setHeaderText("Xoá tài khoản: " + user.getUsername());
        confirm.setContentText("Hành động này không thể hoàn tác. Bạn có chắc chắn?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            try {
                userService.deleteUser(user.getId()); // ← gọi DB
                masterList.remove(user);
                if (userTable.getItems() != masterList) userTable.getItems().remove(user);
                updateStats();
                setStatus("✔ Đã xoá tài khoản " + user.getUsername() + ".");
            } catch (DataAccessException e) {
                showError("Xoá thất bại:\n" + e.getMessage());
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User getSelectedUser() {
        User u = userTable.getSelectionModel().getSelectedItem();
        if (u == null) showInfo("Hãy chọn một user trong bảng trước.");
        return u;
    }

    private void setStatus(String msg) { if (statusLabel != null) statusLabel.setText(msg); }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION) {{ setContentText(msg); }}.showAndWait();
    }
    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR) {{ setContentText(msg); }}.showAndWait();
    }
}
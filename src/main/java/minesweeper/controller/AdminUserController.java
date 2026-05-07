package minesweeper.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import minesweeper.model.Role;
import minesweeper.model.User;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.service.MySqlUserService;
import minesweeper.service.UserService;

public class AdminUserController {

    @FXML private TextField searchField;
    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label lockedUsersLabel;
    @FXML private Label adminCountLabel;
    @FXML private Label statusLabel;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colDisplayName;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;

    private final ObservableList<User> masterList = FXCollections.observableArrayList();
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
        colId.setCellValueFactory( new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory( new PropertyValueFactory<>("username"));
        colDisplayName.setCellValueFactory( new PropertyValueFactory<>("displayName"));
        colRole.setCellValueFactory(data ->  new SimpleStringProperty( data.getValue().getRole().getLabel()));
        colStatus.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().isActive()
                                ? "Hoạt động"
                                : "Đã khoá"
                )
        );
    }

    private void loadUsers() {
        try {
            masterList.clear();
            masterList.addAll(userService.getAllUsers());
            userTable.setItems(masterList);
            updateStats();
            statusLabel.setText(
                    "Đã tải "
                            + masterList.size()
                            + " users"
            );
        } catch (DataAccessException e) {
            showError("Không thể tải dữ liệu");
        }
    }

    private void updateStats() {
        int total = masterList.size();
        int active = 0;
        int locked = 0;
        int admin = 0;
        for (User user : masterList) {

            if (user.isActive()) {
                active++;
            } else {
                locked++;
            }

            if (user.getRole() == Role.ADMIN) {
                admin++;
            }
        }

        totalUsersLabel.setText(String.valueOf(total));
        activeUsersLabel.setText(String.valueOf(active));
        lockedUsersLabel.setText(String.valueOf(locked));
        adminCountLabel.setText(String.valueOf(admin));
    }

    @FXML
    public void onSearch() {

        String keyword =
                searchField.getText().toLowerCase();

        ObservableList<User> filteredList =
                FXCollections.observableArrayList();

        for (User user : masterList) {

            boolean matchUsername =
                    user.getUsername()
                            .toLowerCase()
                            .contains(keyword);

            boolean matchDisplayName =
                    user.getDisplayName() != null
                            &&
                            user.getDisplayName()
                                    .toLowerCase()
                                    .contains(keyword);

            if (matchUsername || matchDisplayName) {
                filteredList.add(user);
            }
        }

        userTable.setItems(filteredList);

        statusLabel.setText(
                "Tìm thấy "
                        + filteredList.size()
                        + " kết quả"
        );
    }

    @FXML
    public void onRefresh() {

        searchField.clear();

        userTable.setItems(masterList);

        statusLabel.setText("Đã reset");
    }

    @FXML
    public void onLockUser() {

        User selected =
                userTable.getSelectionModel()
                        .getSelectedItem();

        if (selected == null) {

            showInfo("Hãy chọn user");

            return;
        }

        try {

            boolean newStatus =
                    !selected.isActive();

            userService.setActive(
                    selected.getId(),
                    newStatus
            );

            selected.setActive(newStatus);

            userTable.refresh();

            updateStats();

            statusLabel.setText(
                    "Đã cập nhật trạng thái"
            );

        } catch (Exception e) {

            showError("Khoá user thất bại");
        }
    }

    @FXML
    public void onDeleteUser() {

        User selected =
                userTable.getSelectionModel()
                        .getSelectedItem();

        if (selected == null) {

            showInfo("Hãy chọn user");

            return;
        }

        try {

            userService.deleteUser(
                    selected.getId()
            );

            masterList.remove(selected);

            userTable.setItems(masterList);

            updateStats();

            statusLabel.setText(
                    "Đã xoá user"
            );

        } catch (Exception e) {

            showError("Xoá thất bại");
        }
    }

    private void showInfo(String message) {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setContentText(message);

        alert.showAndWait();
    }

    private void showError(String message) {

        Alert alert =
                new Alert(Alert.AlertType.ERROR);

        alert.setContentText(message);

        alert.showAndWait();
    }
}
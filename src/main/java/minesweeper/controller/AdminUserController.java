package minesweeper.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import minesweeper.model.User;
import minesweeper.service.UserService;

public class AdminUserController {

    @FXML
    private TextField searchField;

    // Bảng user
    @FXML
    private TableView<User> userTable;

    // Các cột
    @FXML
    private TableColumn<User, Integer> colId;

    @FXML
    private TableColumn<User, String> colUsername;

    @FXML
    private TableColumn<User, String> colEmail;

    @FXML
    private TableColumn<User, String> colDisplayName;

    @FXML
    private TableColumn<User, Boolean> colStatus;

    // ───────────────────────────────────────Service───────────────────────────────────────
    private UserService userService = new UserService();

    private ObservableList<User> userList =
            FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        colId.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        colUsername.setCellValueFactory(
                new PropertyValueFactory<>("username")
        );

        colEmail.setCellValueFactory(
                new PropertyValueFactory<>("email")
        );

        colDisplayName.setCellValueFactory(
                new PropertyValueFactory<>("displayName")
        );

        colStatus.setCellValueFactory(
                new PropertyValueFactory<>("active")
        );

        loadUsers();
    }

    private void loadUsers() {

        userList.clear();

        userList.addAll(
                userService.getAllUsers()
        );

        userTable.setItems(userList);
    }

    @FXML
    public void onRefresh() {

        loadUsers();
    }

    @FXML
    public void onSearch() {

        String keyword =
                searchField.getText().toLowerCase();

        ObservableList<User> result =
                FXCollections.observableArrayList();

        for (User u : userService.getAllUsers()) {

            if (u.getUsername().toLowerCase().contains(keyword)) {

                result.add(u);
            }
        }

        userTable.setItems(result);
    }

    @FXML
    public void onEditUser() {

        User user =
                userTable.getSelectionModel().getSelectedItem();

        if (user == null) {

            showMessage("Hãy chọn user");

            return;
        }

        TextInputDialog dialog =
                new TextInputDialog(user.getDisplayName());

        dialog.setTitle("Sửa Tên");

        dialog.setHeaderText(
                "Nhập tên mới cho "
                        + user.getUsername()
        );

        dialog.showAndWait().ifPresent(email -> {

            user.setDisplayName(email);

            userTable.refresh();

            showMessage("Cập nhật thành công");
        });
    }

    @FXML
    public void onLockUser() {

        User user =
                userTable.getSelectionModel().getSelectedItem();

        if (user == null) {

            showMessage("Hãy chọn user");

            return;
        }

        Alert alert =
                new Alert(Alert.AlertType.CONFIRMATION);

        alert.setContentText(
                "Khoá user "
                        + user.getUsername()
                        + " ?"
        );

        alert.showAndWait().ifPresent(result -> {

            if (result == ButtonType.OK) {

                user.setActive(false);

                userTable.refresh();

                showMessage("Đã khoá user");
            }
        });
    }

    @FXML
    public void onDeleteUser() {

        User user =
                userTable.getSelectionModel().getSelectedItem();

        if (user == null) {

            showMessage("Hãy chọn user");

            return;
        }

        Alert alert =
                new Alert(Alert.AlertType.CONFIRMATION);

        alert.setContentText(
                "Xoá user "
                        + user.getUsername()
                        + " ?"
        );

        alert.showAndWait().ifPresent(result -> {

            if (result == ButtonType.OK) {

                userList.remove(user);

                showMessage("Đã xoá user");
            }
        });
    }


    private void showMessage(String message) {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setContentText(message);

        alert.showAndWait();
    }
}
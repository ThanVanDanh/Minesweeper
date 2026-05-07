package minesweeper.service;

import minesweeper.model.Role;
import minesweeper.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserService {

    private List<User> users = new ArrayList<>();
    private int nextId = 1;

    public UserService() {

        // Dữ liệu mẫu
        User u1 = new User("nightsapper", "123", "NightSapper", Role.PLAYER);
        u1.setId(nextId++);
        users.add(u1);

        User u2 = new User("admin", "123", "Admin", Role.ADMIN);
        u2.setId(nextId++);
        users.add(u2);
    }

    public List<User> getAllUsers() {
        return users;
    }

    public List<User> searchUsers(String keyword) {

        List<User> result = new ArrayList<>();

        for (User u : users) {

            if (u.getUsername().toLowerCase().contains(keyword.toLowerCase())) {

                result.add(u);
            }
        }

        return result;
    }

    public void updateUser(int id, String displayName) {

        for (User u : users) {

            if (u.getId() == id) {

                u.setDisplayName(displayName);

                return;
            }
        }

        System.out.println("Không tìm thấy user");
    }

    public void lockUser(int id) {

        for (User u : users) {

            if (u.getId() == id) {

                u.setActive(false);

                return;
            }
        }

        System.out.println("Không tìm thấy user");
    }

    public void deleteUser(int id) {

        for (int i = 0; i < users.size(); i++) {

            if (users.get(i).getId() == id) {

                users.remove(i);

                return;
            }
        }

        System.out.println("Không tìm thấy user");
    }
}
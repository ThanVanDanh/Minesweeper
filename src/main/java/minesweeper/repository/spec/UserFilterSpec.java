package minesweeper.repository.spec;

import minesweeper.model.enums.Role;

/**
 * Điều kiện lọc user.
 * Trường nào null → không áp dụng điều kiện đó.
 */
public class UserFilterSpec {

    public String  keyword;
    public Role    role;
    public Boolean active;

    public UserFilterSpec() {}


    public static UserFilterSpec withActive(boolean active) {
        UserFilterSpec s = new UserFilterSpec();
        s.active = active;
        return s;
    }

    public static UserFilterSpec withRole(Role role) {
        UserFilterSpec s = new UserFilterSpec();
        s.role = role;
        return s;
    }
}
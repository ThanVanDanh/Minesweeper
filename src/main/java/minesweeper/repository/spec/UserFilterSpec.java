package minesweeper.repository.spec;

import minesweeper.model.enums.Role;

import java.time.LocalDate;

/**
 * Điều kiện lọc user.
 * Trường nào null → không áp dụng điều kiện đó.
 */
public class UserFilterSpec {

    public String  keyword;
    public String  email;
    public Role    role;
    public Boolean active;
    public LocalDate createdFrom;
    public LocalDate createdTo;
    public LocalDate lastLoginFrom;
    public LocalDate lastLoginTo;
    public Integer minGames;
    public Integer maxGames;
    public Boolean hasGameResults;

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

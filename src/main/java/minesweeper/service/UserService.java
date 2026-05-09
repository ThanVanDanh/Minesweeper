package minesweeper.service;

import minesweeper.model.Role;
import minesweeper.model.User;
import minesweeper.repository.exception.DataAccessException;

import java.util.List;

/**
 * Service interface for user management
 */
public interface UserService {
    /**
     * Gets user by username, creating if not exists
     * @param username the username
     * @return user ID
     * @throws DataAccessException if database error occurs
     */
    long getOrCreateUser(String username) throws DataAccessException;

    User getUserById(long userId) throws DataAccessException;

    User getUserByUsername(String username) throws DataAccessException;

    long createUser(String username, String displayName) throws DataAccessException;

    long createUserFull(String username, String displayName, Role role, String password) throws DataAccessException;

    List<User> getAllUsers() throws DataAccessException;

    void updateDisplayName(long userId, String newDisplayName) throws DataAccessException;

    void setActive(long userId, boolean active) throws DataAccessException;

    void updateRole(long userId, Role newRole) throws DataAccessException;

    void deleteUser(long  userId) throws DataAccessException;
}

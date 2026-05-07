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
    
    /**
     * Gets user by ID
     * @param userId the user ID
     * @return the user, or null if not found
     * @throws DataAccessException if database error occurs
     */
    User getUserById(long userId) throws DataAccessException;
    
    /**
     * Gets user by username
     * @param username the username
     * @return the user, or null if not found
     * @throws DataAccessException if database error occurs
     */
    User getUserByUsername(String username) throws DataAccessException;
    
    /**
     * Creates a new user
     * @param username the username
     * @param displayName the display name
     * @return user ID
     * @throws DataAccessException if database error occurs or user already exists
     */
    long createUser(String username, String displayName) throws DataAccessException;

    long createUserFull(String username, String displayName, Role role) throws DataAccessException;

    List<User> getAllUsers() throws DataAccessException;

    void updateDisplayName(int userId, String newDisplayName) throws DataAccessException;

    void setActive(int userId, boolean active) throws DataAccessException;

    void updateRole(int userId, Role newRole) throws DataAccessException;

    void deleteUser(int userId) throws DataAccessException;
}

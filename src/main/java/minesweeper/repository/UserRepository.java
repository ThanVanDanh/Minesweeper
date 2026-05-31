package minesweeper.repository;

import minesweeper.model.User;
import minesweeper.model.enums.Role;
import minesweeper.repository.pagination.PagedResult;
import minesweeper.repository.spec.UserFilterSpec;
import minesweeper.repository.exception.DataAccessException;

import java.util.List;

public interface UserRepository {

    // ── Read ──────────────────────────────────────────────────────────────────

    User findById(long id) throws DataAccessException;

    User findByUsername(String username) throws DataAccessException;

    List<User> findAll() throws DataAccessException;

    PagedResult<User> findPaged(UserFilterSpec spec, int pageNumber, int pageSize)
            throws DataAccessException;

    long count(UserFilterSpec spec) throws DataAccessException;

    // ── Write ─────────────────────────────────────────────────────────────────

    long save(String username, String displayName, Role role, String passwordHash)
            throws DataAccessException;

    void updateDisplayName(long id, String displayName) throws DataAccessException;

    void updateRole(long id, Role role) throws DataAccessException;

    void setActive(long id, boolean active) throws DataAccessException;


    void delete(long id) throws DataAccessException;
}
package minesweeper.repository.log;

import minesweeper.model.AuditLog;
import minesweeper.repository.exception.DataAccessException;

import java.util.List;

public interface AuditLogRepository {
    void insert(AuditLog log) throws DataAccessException;
    List<AuditLog> findRecent(int limit) throws DataAccessException;

}

package minesweeper.service;
import minesweeper.model.GameResult;
import minesweeper.repository.GameResultRepository;
import minesweeper.repository.exception.DataAccessException;
import minesweeper.repository.pagination.PagedResult;
import minesweeper.repository.spec.GameResultFilterSpec;

import java.util.List;
import java.util.Objects;

public class GameResultService {

    private final GameResultRepository repository;

    public GameResultService(GameResultRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public PagedResult<GameResult> findPaged(GameResultFilterSpec spec,
                                             int pageNumber, int pageSize)
            throws DataAccessException {
        GameResultFilterSpec safeSpec = spec != null ? spec : new GameResultFilterSpec();
        return repository.findPaged(safeSpec, pageNumber, pageSize);
    }

    public GameResultStats getStats() throws DataAccessException {
        long total = repository.count(new GameResultFilterSpec());
        long wins  = repository.count(GameResultFilterSpec.withWin(true));
        long loses = repository.count(GameResultFilterSpec.withWin(false));
        return new GameResultStats(total, wins, loses);
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    public void deleteByGameIds(List<String> gameIds) throws DataAccessException {
        repository.deleteByGameIds(gameIds);
    }

    public void clearAll() throws DataAccessException {
        repository.clearAllResults();
    }

    // ── Inner DTO ─────────────────────────────────────────────────────────────

    public record GameResultStats(long total, long wins, long loses) {}
}

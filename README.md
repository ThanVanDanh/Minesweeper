# Minesweeper (JavaFX)

This project uses a simple MVC split:
- Model: `minesweeper.model`
- Controller: `minesweeper.controller` and `minesweeper.ranking.controller`
- View: `src/main/resources/app/hello-view.fxml`

## Ranking flow (MySQL)

The leaderboard tab now follows this structure:

- `minesweeper.controller.RankingHistoryController` (RankingScreen)
- `minesweeper.ranking.controller.RankingController` (application/controller layer)
- `minesweeper.ranking.repository.RankingDAO` + `minesweeper.ranking.repository.MySqlRankingDAO` (data access layer)
- MySQL tables: `game_sessions`, `users`, `game_levels`

Main call path:

`RankingHistoryController.loadRankingByLevel()` -> `RankingController.getRanking(levelId)` -> `RankingDAO.getLeaderboardByLevel(levelId)`

SQL behavior for leaderboard:

- filter by `level_id`
- aggregate per player (`total_games`, `wins`, `best_score`, `best_time_ms`)
- sort by `best_score DESC`, `best_time_ms ASC`, then username

## Run

Use the Maven wrapper if `mvn` is not on PATH:

```powershell
./mvnw -q -DskipTests package
./mvnw javafx:run
```

If you have Maven installed:

```powershell
mvn -q -DskipTests package
mvn javafx:run
```


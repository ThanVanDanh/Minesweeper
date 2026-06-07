package minesweeper.repository.spec;

import minesweeper.model.enums.Difficulty;

public class GameResultFilterSpec {
    public String     username;
    public Difficulty difficulty;
    public Boolean    win;
    public String     sortBy;  // "score" or "time"
    public String     sortDir; // "ASC" or "DESC"

    public GameResultFilterSpec() {}

    public static GameResultFilterSpec withUsername(String username) {
        GameResultFilterSpec s = new GameResultFilterSpec();
        s.username = username;
        return s;
    }

    public static GameResultFilterSpec withDifficulty(Difficulty difficulty) {
        GameResultFilterSpec s = new GameResultFilterSpec();
        s.difficulty = difficulty;
        return s;
    }

    public static GameResultFilterSpec withWin(boolean win) {
        GameResultFilterSpec s = new GameResultFilterSpec();
        s.win = win;
        return s;
    }
}

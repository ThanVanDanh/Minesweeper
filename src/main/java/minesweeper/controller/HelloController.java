package minesweeper.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import minesweeper.model.Board;
import minesweeper.model.Difficulty;

public class HelloController {
    private final GameController gameController = new GameController();

    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        if (!gameController.hasGame()) {
            gameController.startNewGame(Difficulty.EASY);
        }
        gameController.reveal(0, 0);
        Board board = gameController.getBoard();
        if (board == null) {
            welcomeText.setText("No active game");
            return;
        }
        welcomeText.setText("State: " + gameController.getGameState()
                + " | Mines left: " + board.getRemainingMines());
    }
}


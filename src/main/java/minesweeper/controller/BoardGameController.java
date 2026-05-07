package minesweeper.controller;

import app.App;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import minesweeper.model.Difficulty;
import minesweeper.model.GameState;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class BoardGameController implements Initializable {
    @FXML private ToggleButton btnEasy;
    @FXML private ToggleButton btnMedium;
    @FXML private ToggleButton btnHard;
    @FXML private Label lblFlags;
    @FXML private Label lblTime;
    @FXML private GridPane minesweeperGrid;
    @FXML private VBox pauseOverlay;
    @FXML private Button btnPause;

    private ToggleGroup difficultyGroup;
    private GameController gameLogic;
    private Timeline timer;
    private int secondsElapsed = 0;
    private final int BUTTON_SIZE = 35;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        gameLogic = new GameController();

        difficultyGroup = new ToggleGroup();
        btnEasy.setToggleGroup(difficultyGroup);
        btnMedium.setToggleGroup(difficultyGroup);
        btnHard.setToggleGroup(difficultyGroup);

        difficultyGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                if (oldVal != null) oldVal.setSelected(true);
            } else if (newVal == btnEasy) {
                startGame(Difficulty.EASY);
            } else if (newVal == btnMedium) {
                startGame(Difficulty.MEDIUM);
            } else if (newVal == btnHard) {
                startGame(Difficulty.HARD);
            }
        });

        setupTimer();
    }

    public void setInitialDifficulty(Difficulty selectedDifficulty) {
        if (selectedDifficulty == null) return;

        if (selectedDifficulty == Difficulty.EASY) {
            btnEasy.setSelected(true);
        } else if (selectedDifficulty == Difficulty.MEDIUM) {
            btnMedium.setSelected(true);
        } else if (selectedDifficulty == Difficulty.HARD) {
            btnHard.setSelected(true);
        }
    }

    private void startGame(Difficulty diff) {
        gameLogic.startNewGame(diff);
        stopTimer();
        secondsElapsed = 0;
        updateStatus();
        pauseOverlay.setVisible(false);
        if (btnPause != null) btnPause.setText("Tạm dừng");
        renderBoard();
    }

    private void renderBoard() {
        minesweeperGrid.getChildren().clear();
        int rows = gameLogic.getBoard().getRows();
        int cols = gameLogic.getBoard().getCols();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Button btnCell = new Button();
                btnCell.setPrefSize(BUTTON_SIZE, BUTTON_SIZE);
                btnCell.getStyleClass().add("mine-cell-covered");

                final int finalR = r;
                final int finalC = c;

                btnCell.setOnMouseClicked(e -> {
                    if (gameLogic.isPaused()) return;

//                    gameLogic.reveal(finalR, finalC);
                    updateBoardUI();
                });

                minesweeperGrid.add(btnCell, c, r);
            }
        }
        updateBoardUI();
    }

    private void updateBoardUI() {
        int rows = gameLogic.getBoard().getRows();
        int cols = gameLogic.getBoard().getCols();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                Button btnCell = (Button) minesweeperGrid.getChildren().get(index);
                minesweeper.model.Cell cell = gameLogic.getBoard().getCell(r, c);

                btnCell.setText("");
                btnCell.setStyle("");

                if (cell.isRevealed()) {
                    btnCell.setDisable(true);
                    if (cell.isMine()) {
                        btnCell.setText("💣");
                        btnCell.setStyle("-fx-background-color: #ff4a69; -fx-opacity: 1;");
                    } else if (cell.getNeighborMines() > 0) {
                        btnCell.setText(String.valueOf(cell.getNeighborMines()));
                        btnCell.setStyle("-fx-text-fill: #39ff8f; -fx-font-weight: bold; -fx-opacity: 1;");
                    } else {
                        btnCell.setStyle("-fx-background-color: #031522; -fx-opacity: 1;");
                    }
                } else if (cell.isFlagged()) {
                    btnCell.setText("⚑");
                    btnCell.setStyle("-fx-text-fill: #ff4a69;");
                } else {
                    btnCell.getStyleClass().clear();
                    btnCell.getStyleClass().add("mine-cell-covered");
                }
            }
        }
        updateStatus();
    }

    private void updateStatus() {
        int remaining = gameLogic.getBoard() != null ? gameLogic.getBoard().getRemainingMines() : 0;
        lblFlags.setText(String.format("Cờ %03d", remaining));

        int mins = secondsElapsed / 60;
        int secs = secondsElapsed % 60;
        lblTime.setText(String.format("Time %02d:%02d", mins, secs));
    }

    @FXML
    private void togglePause() {
        if (gameLogic.getGameState() != GameState.PLAYING && !gameLogic.isPaused()) return;

        boolean isNowPaused = !gameLogic.isPaused();
        gameLogic.setPaused(isNowPaused);
        pauseOverlay.setVisible(isNowPaused);

        if (isNowPaused) {
            stopTimer();
            btnPause.setText("Tiếp tục");
        } else {
            startTimer();
            btnPause.setText("Tạm dừng");
        }
    }

    @FXML
    private void backToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/app/dashboard.fxml"));
            Scene scene = new Scene(root, 1280, 760);
            scene.getStylesheets().add(Objects.requireNonNull(
                    App.class.getResource("/css/styles.css")
            ).toExternalForm());

            Stage stage = (Stage) minesweeperGrid.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Minesweeper Tactical - Dashboard");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void btn(ActionEvent actionEvent) {
        if (gameLogic.hasGame()) {
            startGame(gameLogic.getDifficulty());
        }
    }

    private void setupTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            updateStatus();
        }));
        timer.setCycleCount(Animation.INDEFINITE);
    }

    private void startTimer() {
        if (timer != null) timer.play();
    }

    private void stopTimer() {
        if (timer != null) timer.pause();
    }
}
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
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
    @FXML private Button btnPause, btnFlat;
    @FXML private Label lblOverlay;

    private ToggleGroup difficultyGroup;
    private GameController gameLogic;
    private Timeline timer;
    private int secondsElapsed = 0;
    private final int BUTTON_SIZE = 35;
    private boolean isFlagMode = false;

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
        startTimer();
    }

    private void renderBoard() {
        minesweeperGrid.getChildren().clear();
        int rows = gameLogic.getBoard().getRows();
        int cols = gameLogic.getBoard().getCols();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Button btnCell = new Button();

                btnCell.setPrefSize(BUTTON_SIZE, BUTTON_SIZE);
                btnCell.setMinSize(BUTTON_SIZE, BUTTON_SIZE);
                btnCell.setMaxSize(BUTTON_SIZE, BUTTON_SIZE);

                btnCell.getStyleClass().add("mine-cell-covered");

                final int finalR = r;
                final int finalC = c;

                btnCell.setOnAction(e -> {
                    if (gameLogic.isPaused()) return;
                    if (gameLogic.getGameState() == GameState.LOST || gameLogic.getGameState() == GameState.WON) return;
                    minesweeper.model.Cell currentCell = gameLogic.getBoard().getCell(finalR, finalC);

                    if (currentCell.isRevealed()) {
                        gameLogic.fastReveal(finalR, finalC);
                    } else {
                        if (isFlagMode) {
                            gameLogic.toggleFlag(finalR, finalC);
                        } else {
                            gameLogic.reveal(finalR, finalC);
                        }
                    }

                    if (gameLogic.getGameState() == GameState.LOST) {
                        playExplosionSound();
                        showGameOver("BẠN ĐÃ THUA!", "#ff4a69");
                    } else if (gameLogic.getGameState() == GameState.WON) {
                        showGameOver("BẠN ĐÃ THẮNG!", "#39ff8f");
                    }

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
                btnCell.setGraphic(null);
                btnCell.setStyle("");
                btnCell.getStyleClass().removeAll(
                        "mine-cell-covered",
                        "mine-cell-flaggedd",
                        "mine-cell-revealed",
                        "mine-cell-bomb",
                        "mine-cell-number",
                        "mine-cell-empty"
                );

                if (cell.isRevealed()) {
                    btnCell.getStyleClass().add("mine-cell-revealed");

                    if (cell.isMine()) {
                        ImageView bombIcon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/bomb-icon.png"))));
                        bombIcon.setFitWidth(22);
                        bombIcon.setFitHeight(22);
                        btnCell.setGraphic(bombIcon);
                        btnCell.getStyleClass().add("mine-cell-bomb");
                    } else if (cell.getNeighborMines() > 0) {
                        btnCell.setText(String.valueOf(cell.getNeighborMines()));
                        btnCell.getStyleClass().add("mine-cell-number");
                    } else {
                        btnCell.getStyleClass().add("mine-cell-empty");
                    }
                } else if (cell.isFlagged() || (gameLogic.getGameState() == GameState.WON && cell.isMine())) {
                    btnCell.getStyleClass().add("mine-cell-flaggedd");
                } else {
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

    @FXML
    public void btnFlat(ActionEvent actionEvent) {
        ImageView icon = (ImageView) btnFlat.getGraphic();
        isFlagMode = !isFlagMode;

        btnFlat.setStyle("");
        btnFlat.getStyleClass().removeAll("btn-flat-normal", "btn-flat-flag-active");

        if (isFlagMode) {
            btnFlat.setText("Cắm cờ");
            btnFlat.getStyleClass().add("btn-flat-flag-active");
            icon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/flag-icon.png"))));
        } else {
            btnFlat.setText("Mở ô");
            btnFlat.getStyleClass().add("btn-flat-normal");
            icon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/bomb-icon.png"))));
        }
    }

    private void playExplosionSound() {
        try {
            URL soundUrl = getClass().getResource("/sound/explosion.mp3");
            if (soundUrl != null) {
                AudioClip explosionSound = new AudioClip(soundUrl.toExternalForm());
                explosionSound.play();
            } else {
                System.err.println("Lỗi: Không tìm thấy file âm thanh tại /sound/explosion.mp3");
            }
        } catch (Exception ex) {
            System.err.println("Lỗi khi phát âm thanh: " + ex.getMessage());
        }
    }
    private void showGameOver(String message, String hexColor) {
        stopTimer();
        lblOverlay.setText(message);
        lblOverlay.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-size: 40; -fx-font-weight: bold;");
        pauseOverlay.setVisible(true);
    }

    @FXML
    public void restartGame(ActionEvent actionEvent) {
        Difficulty currentDiff = Difficulty.EASY; // Mặc định là Dễ
        if (btnMedium.isSelected()) {
            currentDiff = Difficulty.MEDIUM;
        } else if (btnHard.isSelected()) {
            currentDiff = Difficulty.HARD;
        }

        if (isFlagMode) {
            isFlagMode = false;
            btnFlat.setText("Mở ô");
            btnFlat.getStyleClass().removeAll("btn-flat-normal", "btn-flat-flag-active");
            btnFlat.getStyleClass().add("btn-flat-normal");

            ImageView icon = (ImageView) btnFlat.getGraphic();
            if (icon != null) {
                icon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/bomb-icon.png"))));
            }
        }

        startGame(currentDiff);
    }
}
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
import minesweeper.model.Board;
import minesweeper.model.Difficulty;
import minesweeper.model.GameState;
import minesweeper.repository.GameSessionData;
import minesweeper.repository.GameSessionRepository;
import minesweeper.repository.GameSessionResult;
import javafx.scene.input.MouseButton;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.ResourceBundle;

public class BoardGameController implements Initializable {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(BoardGameController.class);
    @FXML private Label lblFlags;
    @FXML private Label lblTime;
    @FXML private GridPane minesweeperGrid;
    @FXML private VBox pauseOverlay;
    @FXML private Button btnPause, btnFlat;
    @FXML private Label lblOverlay;
    @FXML private VBox gameOverOverlay;
    @FXML private Label lblGameOver;

    private ToggleGroup difficultyGroup;
    private GameController gameLogic;
    private Timeline timer;
    private int secondsElapsed = 0;
    private final int BUTTON_SIZE = 35;
    private boolean isFlagMode = false;
    private GameSessionRepository gameSessionRepository;
    private long currentUserId;
    private String currentUsername = "Player";
    private LocalDateTime gameStartedAt;
    private LocalDateTime firstClickAt;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        gameLogic = new GameController();
        gameSessionRepository = new GameSessionRepository();
        setupTimer();
    }

    // UC04 - Bắt đầu ván mới
    public void setInitialDifficulty(Difficulty selectedDifficulty) {
        if (selectedDifficulty == null) return;
        startGame(selectedDifficulty);
    }

    // UC04 - Bắt đầu ván mới
    private void startGame(Difficulty diff) {
        gameLogic.startNewGame(diff);
        stopTimer();
        secondsElapsed = 0;
        updateStatus();

        // GIẤU CẢ 2 MÀN HÌNH ĐEN KHI BẮT ĐẦU VÁN MỚI
        if (pauseOverlay != null) pauseOverlay.setVisible(false);
        if (gameOverOverlay != null) gameOverOverlay.setVisible(false);

        if (btnPause != null) btnPause.setText("Tạm dừng");
        gameStartedAt = LocalDateTime.now();
        firstClickAt = null;
        renderBoard();
//        startTimer();
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

                btnCell.setOnMouseClicked(e -> {
                    if (gameLogic.isPaused()) return;
                    if (gameLogic.getGameState() == GameState.LOST || gameLogic.getGameState() == GameState.WON) return;

                    if (firstClickAt == null) {
                        firstClickAt = LocalDateTime.now();
                        startTimer();
                    }

                    minesweeper.model.Cell currentCell = gameLogic.getBoard().getCell(finalR, finalC);


                    // UC10 - CẮM/GỠ CỜ
                    // UC10.1: Người chơi click chuột phải vào ô vuông
                    if (e.getButton() == MouseButton.SECONDARY) {
                        // UC10.2: BoardGameController chuyển tiếp tọa độ sang GameController
                        gameLogic.toggleFlag(finalR, finalC);
                    }

                    // UC09 & UC13 - MỞ Ô / MỞ NHANH
                    else if (e.getButton() == MouseButton.PRIMARY) {
                        // UC13.1: Người chơi click chuột trái vào ô số hiển thị đã mở từ trước
                        if (currentCell.isRevealed()) {
                            // UC13.2: BoardGameController gọi hàm mở nhanh fastReveal
                            gameLogic.fastReveal(finalR, finalC);
                        } else {
                            // UC10.1: Hoặc click chuột trái khi nút công tắc btnFlat đang bật chế độ cắm cờ
                            if (isFlagMode) {
                                // UC10.2: Chuyển tiếp tọa độ sang GameController để cắm cờ
                                gameLogic.toggleFlag(finalR, finalC);
                            } else {
                                // UC09.1: Người chơi click chuột trái vào một ô vuông chưa mở thông thường
                                // UC09.3: BoardGameController truyền yêu cầu điều phối sang GameController
                                gameLogic.reveal(finalR, finalC);
                            }
                        }
                    }
                    // UC14 - PHÁT HIỆN MÌN
                    // UC14.4: Nhận diện trạng thái GameState là LOST sau tính toán của Board
                    if (gameLogic.getGameState() == GameState.LOST) {
                        playExplosionSound();
                        // UC14.5: Khóa luồng xử lý, dừng thời gian, hiện lớp phủ thông báo và lưu kết quả
                        showGameOver("BẠN ĐÃ THUA!", "#ff4a69");
                    } else if (gameLogic.getGameState() == GameState.WON) {
                        showGameOver("BẠN ĐÃ THẮNG!", "#39ff8f");
                    }

                    // UC09.12 / UC10.8 / UC13.10: Đồng bộ hóa toàn bộ trạng thái lên màn hình hiển thị UI
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

                // UC09/UC10/UC12/UC15 - Đồng bộ trạng thái Cell lên UI:
                // ô mở, số mìn lân cận, cờ, mìn lộ và cờ sai khi thua.
                btnCell.setText("");
                btnCell.setGraphic(null);
                btnCell.setStyle("");
                btnCell.getStyleClass().removeAll(
                        "mine-cell-covered",
                        "mine-cell-flaggedd",
                        "mine-cell-revealed",
                        "mine-cell-bomb",
                        "mine-cell-number",
                        "mine-cell-empty",
                        "mine-cell-wrong-flag"
                );

                if (cell.isRevealed()) {
                    btnCell.getStyleClass().add("mine-cell-revealed");

                    // UC15 - LỘ TOÀN BỘ MÌN
                    if (cell.isMine()) {
                        // UC15.5: Ô chứa mìn đã lật, nạp biểu tượng quả bom (bomb-icon.png) lên nút bấm
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
                }
                // UC15.6: Trạng thái LOST, ô chưa lật bị cắm cờ sai vị trí mìn nhận ký hiệu dấu X (x-icon.png)
                else if (gameLogic.getGameState() == GameState.LOST && cell.isFlagged() && !cell.isMine()) {
                    ImageView wrongIcon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/x-icon.png"))));
                    wrongIcon.setFitWidth(22);
                    wrongIcon.setFitHeight(22);
                    btnCell.setGraphic(wrongIcon);
                    btnCell.getStyleClass().add("mine-cell-wrong-flag");
                }
                else if (cell.isFlagged() || (gameLogic.getGameState() == GameState.WON && cell.isMine())) {
                    btnCell.getStyleClass().add("mine-cell-flaggedd");
                }
                else {
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

    // UC05/UC06 - Tạm dừng / Tiếp tục ván game
    @FXML
    private void togglePause() {
        if (gameLogic.getGameState() != GameState.PLAYING && !gameLogic.isPaused()) return;

        boolean isNowPaused = !gameLogic.isPaused();
        gameLogic.setPaused(isNowPaused);
        if (pauseOverlay != null) pauseOverlay.setVisible(isNowPaused);

        if (isNowPaused) {
            // UC05 - Tạm dừng
            stopTimer();
            if (btnPause != null) btnPause.setText("Tiếp tục");
        } else {
            // UC06 - Tiếp tục ván game
            startTimer();
            if (btnPause != null) btnPause.setText("Tạm dừng");
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

    // UC21 - Đếm thời gian
    private void setupTimer() {
        if (timer != null) timer.stop(); // Dọn dẹp timer cũ trước khi setup
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            updateStatus();
        }));
        timer.setCycleCount(Animation.INDEFINITE);
    }

    // UC21 - Đếm thời gian
    private void startTimer() {
        if (timer != null) timer.play();
    }

    // UC21 - Đếm thời gian
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
        if (lblGameOver != null) {
            lblGameOver.setText(message);
            lblGameOver.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-size: 40; -fx-font-weight: bold;");
        }

        if (gameOverOverlay != null) gameOverOverlay.setVisible(true);
        if (pauseOverlay != null) pauseOverlay.setVisible(false);
        saveGameResult();
    }

    /**
     * Nhận thông tin user từ SessionManager (được gọi bởi DashBoardController trước khi start game).
     */
    public void setCurrentUser(long userId, String username) {
        this.currentUserId   = userId;
        this.currentUsername = (username != null && !username.isBlank()) ? username.trim() : "Player";
    }

    private void saveGameResult() {
        if (currentUserId <= 0) {
            LOG.warn("Bỏ qua lưu kết quả: chưa đăng nhập (userId={})", currentUserId);
            return;
        }

        try {
            boolean isWon = gameLogic.getGameState() == GameState.WON;
            Board board   = gameLogic.getBoard();
            LocalDateTime endedAt = LocalDateTime.now();

            // Đếm số ô đã mở (không tính ô mìn)
            int openedCells = 0;
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (board.getCell(r, c).isRevealed() && !board.getCell(r, c).isMine()) {
                        openedCells++;
                    }
                }
            }

            // completionTime = từ first_click_at đến ended_at (ms)
            long completionTimeMs = (long) secondsElapsed * 1000;

            // Tính score tạm bằng GameResult để tái dụng logic
            minesweeper.model.GameResult tmp = new minesweeper.model.GameResult(
                    java.util.UUID.randomUUID().toString(),
                    currentUsername,
                    gameLogic.getDifficulty(),
                    isWon,
                    completionTimeMs,
                    board.getFlagsPlaced(),
                    board.getTotalMines(),
                    endedAt
            );
            tmp.computeScore();

            GameSessionData data = GameSessionData.builder()
                    .userId(currentUserId)
                    .difficulty(gameLogic.getDifficulty())
                    .won(isWon)
                    .completionTimeMs(completionTimeMs)
                    .score(tmp.getScore())
                    .openedCells(openedCells)
                    .flaggedCells(board.getFlagsPlaced())
                    .startedAt(gameStartedAt)
                    .firstClickAt(firstClickAt)
                    .endedAt(endedAt)
                    .build();

            GameSessionResult saved = gameSessionRepository.saveGameResult(data);
            LOG.info("Đã lưu kết quả: user={}, sessionId={}, newRecord={}, rank={}",
                     currentUsername, saved.getSessionId(), saved.isNewRecord(), saved.getNewRank());


        } catch (Exception e) {
            LOG.error("Lỗi khi lưu kết quả game", e);
        }
    }

    // UC04 - Bắt đầu ván mới
    @FXML
    public void restartGame(ActionEvent actionEvent) {
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

        if (gameLogic != null && gameLogic.getDifficulty() != null) {
            startGame(gameLogic.getDifficulty());
        }
    }
}
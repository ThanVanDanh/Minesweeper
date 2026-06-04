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
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;
import javafx.util.Duration;
import minesweeper.model.Board;
import minesweeper.model.enums.Difficulty;
import minesweeper.model.enums.GameState;
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
    private static final int TURN_DURATION_SECONDS = 10;
    @FXML private Label lblFlags;
    @FXML private Label lblTime;
    @FXML private GridPane minesweeperGrid;
    @FXML private VBox pauseOverlay;
    @FXML private Button btnPause, btnFlat;
    @FXML private Label lblOverlay;
    @FXML private VBox gameOverOverlay;
    @FXML private Label lblGameOver;
    @FXML private Label lblPlayers;
    @FXML private Label lblScores;
    @FXML private VBox playerCard1, playerCard2, playerCard3, playerCard4;
    @FXML private TextField playerName1, playerName2, playerName3, playerName4;
    @FXML private Label playerScore1, playerScore2, playerScore3, playerScore4;

    private GameController gameLogic;
    private Timeline timer;
    private Timeline turnTimer;
    private int secondsElapsed = 0;
    private int turnSecondsRemaining = TURN_DURATION_SECONDS;
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
        setupTurnTimer();
    }

    // UC04 - Bắt đầu ván mới
    public void setInitialDifficulty(Difficulty selectedDifficulty) {
        setInitialDifficulty(selectedDifficulty, Board.MIN_PLAYER_COUNT);
    }

    public void setInitialDifficulty(Difficulty selectedDifficulty, int playerCount) {
        if (selectedDifficulty == null) return;
        startGame(selectedDifficulty, playerCount);
    }

    public void setInitialCustomBoard(int rows, int cols, int mines, int playerCount) {
        startCustomGame(rows, cols, mines, playerCount);
    }

    // UC04 - Bắt đầu ván mới
    private void startGame(Difficulty diff, int playerCount) {
        gameLogic.startNewGame(diff, playerCount);
        resetGameStartState();
        renderBoard();
//        startTimer();
    }

    private void startCustomGame(int rows, int cols, int mines, int playerCount) {
        gameLogic.startCustomGame(rows, cols, mines, playerCount);
        resetGameStartState();
        renderBoard();
//        startTimer();
    }

    private void resetGameStartState() {
        stopTimer();
        stopTurnTimer();
        secondsElapsed = 0;
        turnSecondsRemaining = TURN_DURATION_SECONDS;
        updateStatus();

        // GIẤU CẢ 2 MÀN HÌNH ĐEN KHI BẮT ĐẦU VÁN MỚI
        if (pauseOverlay != null) pauseOverlay.setVisible(false);
        if (gameOverOverlay != null) gameOverOverlay.setVisible(false);

        if (btnPause != null) btnPause.setText("Tạm dừng");
        gameStartedAt = LocalDateTime.now();
        firstClickAt = null;

        isBlindBombActive = false;
        isBlindBombPending = false;
        if (btnBlindBomb != null) {
            btnBlindBomb.getStyleClass().remove("item-button-active");
            btnBlindBomb.setDisable(false);
        }
        startTurnTimer();
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
                    boolean completedMove = false;

                    // 03.2.1 UC03.1 - CẮM / GỠ CỜ
                    if (e.getButton() == MouseButton.SECONDARY) {
                        // 03.2.1.1 & 03.2.1.2: Người chơi nhấp chuột phải -> Chuyển tiếp lệnh
                        completedMove = gameLogic.toggleFlag(finalR, finalC);
                    }
                    // Mở ô bằng chuột trái
                    else if (e.getButton() == MouseButton.PRIMARY) {
                        GameState stateBeforeMove = gameLogic.getGameState();
                        // 03.2.2 UC03.2 - MỞ NHANH (Fast Reveal)
                        if (currentCell.isRevealed()) {
                            // 03.2.2.1 & 03.2.2.2: Người chơi nhấp chuột trái vào ô số đã mở
                            int openedCells = gameLogic.fastReveal(finalR, finalC);
                            completedMove = openedCells > 0 || gameLogic.getGameState() != stateBeforeMove;
                        } else {
                            // 03.2.1.1: Hoặc click chuột trái khi nút btnFlat đang bật chế độ cắm cờ
                            if (isFlagMode) {
                                completedMove = gameLogic.toggleFlag(finalR, finalC);
                            } else {
                                // 3.1 MỞ Ô (Basic Flow)
                                int openedCells = gameLogic.reveal(finalR, finalC);
                                completedMove = openedCells > 0 || gameLogic.getGameState() != stateBeforeMove;
                            }
                        }
                    }

                    // Xử lý sau khi tương tác: Kiểm tra điều kiện thắng/thua

                    // 03.2.3 PHÁT HIỆN MÌN (Game Over - Thua cuộc)
                    if (gameLogic.getGameState() == GameState.LOST) {
                        // 03.2.3.4: Phát âm thanh nổ
                        playExplosionSound();
                        // 03.2.3.5 & 03.2.3.6: Hiển thị lớp phủ BẠN ĐÃ THUA, dừng thời gian và lưu KQ
                        showGameOver(buildLostMessage(), "#ff4a69");
                    }
                    // 03.2.4 ĐIỀU KIỆN THẮNG CUỘC
                    else if (gameLogic.getGameState() == GameState.WON) {
                        // 03.2.4.4 & 03.2.4.5: Hiển thị BẠN ĐÃ THẮNG, dừng đồng hồ và lưu KQ
                        showGameOver(buildWinMessage(), "#39ff8f");
                    }
                    else if (completedMove) {
                        restartTurnTimer();
                    }

                    // 03.2.1.4, 03.2.2.4, 03.2.4.5: Đồng bộ hóa toàn bộ trạng thái lên UI
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

                // Reset style
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

                    // 03.2.3.3 LỘ TOÀN BỘ MÌN
                    if (cell.isMine()) {
                        ImageView bombIcon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/bomb-icon.png"))));
                        bombIcon.setFitWidth(22);
                        bombIcon.setFitHeight(22);
                        btnCell.setGraphic(bombIcon);
                        btnCell.getStyleClass().add("mine-cell-bomb");
                    } else if (cell.getNeighborMines() > 0) {
                        if (isBlindBombActive) {
                            btnCell.setText("?");
                            btnCell.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.3);");
                        } else {
                            btnCell.setText(String.valueOf(cell.getNeighborMines()));
                        }
                        btnCell.getStyleClass().add("mine-cell-number");
                    } else {
                        btnCell.getStyleClass().add("mine-cell-empty");
                    }
                }
                // 03.2.3.3: Trạng thái LOST, ô chưa lật bị cắm cờ sai vị trí mìn nhận ký hiệu dấu X
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
        // 03.2.1.4: Cập nhật số lượng cờ còn lại
        int remaining = gameLogic.getBoard() != null ? gameLogic.getBoard().getRemainingMines() : 0;
        lblFlags.setText(String.format("Cờ %03d", remaining));

        if (lblPlayers != null) {
            int players = gameLogic.getBoard() != null ? gameLogic.getPlayerCount() : 1;
            if (players > 1) {
                lblPlayers.setText(String.format("Lượt %s | %02ds",
                        getCurrentPlayerDisplayName(), turnSecondsRemaining));
            } else {
                lblPlayers.setText(String.format("%s | %02ds",
                        getCurrentPlayerDisplayName(), turnSecondsRemaining));
            }
        }

        if (lblScores != null) {
            lblScores.setText(buildScoreText());
        }
        updatePlayerScorePanel();

        int mins = secondsElapsed / 60;
        int secs = secondsElapsed % 60;
        lblTime.setText(String.format("Time %02d:%02d", mins, secs));

        updateItemUI();
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
            stopTurnTimer();
            if (btnPause != null) btnPause.setText("Tiếp tục");
        } else {
            // UC06 - Tiếp tục ván game
            startTimer();
            startTurnTimer();
            if (btnPause != null) btnPause.setText("Tạm dừng");
        }
    }

    @FXML
    private void backToDashboard() {
        try {
            stopTimer();
            stopTurnTimer();
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

    private void setupTurnTimer() {
        if (turnTimer != null) turnTimer.stop();
        turnTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (gameLogic == null || gameLogic.isPaused()
                    || gameLogic.getGameState() == GameState.LOST
                    || gameLogic.getGameState() == GameState.WON) {
                return;
            }

            turnSecondsRemaining--;
            if (turnSecondsRemaining <= 0) {
                handleTurnTimeout();
            } else {
                updateStatus();
            }
        }));
        turnTimer.setCycleCount(Animation.INDEFINITE);
    }

    private void startTurnTimer() {
        if (turnTimer != null) turnTimer.play();
    }

    private void stopTurnTimer() {
        if (turnTimer != null) turnTimer.pause();
    }

    private void restartTurnTimer() {
        processBlindBombTurnTransition();

        turnSecondsRemaining = TURN_DURATION_SECONDS;
        startTurnTimer();
        updateStatus();
    }

    private void handleTurnTimeout() {
        gameLogic.skipCurrentTurn();
        processBlindBombTurnTransition();
        turnSecondsRemaining = TURN_DURATION_SECONDS;
        updateStatus();
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
        stopTurnTimer();
        if (lblGameOver != null) {
            lblGameOver.setText(message);
            lblGameOver.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-size: 40; -fx-font-weight: bold;");
        }

        if (gameOverOverlay != null) gameOverOverlay.setVisible(true);
        if (pauseOverlay != null) pauseOverlay.setVisible(false);
        // 03.2.3.5 & 03.2.4.4 Gọi luồng lưu dữ liệu ván đấu
        saveGameResult();
    }

    private String buildScoreText() {
        int[] scores = gameLogic.getPlayerScores();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < scores.length; i++) {
            if (i > 0) builder.append("   ");
            builder.append(getPlayerDisplayName(i))
                    .append(": ")
                    .append(String.format("%04d", scores[i]));
        }
        return builder.toString();
    }

    private String buildLostMessage() {
        if (gameLogic.getPlayerCount() <= 1) {
            return "BẠN ĐÃ THUA!";
        }
        return getCurrentPlayerDisplayName().toUpperCase() + " TRÚNG MÌN!";
    }

    private String buildWinMessage() {
        if (gameLogic.getPlayerCount() <= 1) {
            return "BẠN ĐÃ THẮNG!";
        }

        int[] scores = gameLogic.getPlayerScores();
        int bestScore = Integer.MIN_VALUE;
        int winnerIndex = -1;
        boolean tied = false;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > bestScore) {
                bestScore = scores[i];
                winnerIndex = i;
                tied = false;
            } else if (scores[i] == bestScore) {
                tied = true;
            }
        }
        return tied ? "HÒA ĐIỂM!" : getPlayerDisplayName(winnerIndex).toUpperCase() + " THẮNG!";
    }

    private void updatePlayerScorePanel() {
        if (playerCard1 == null) {
            return;
        }

        VBox[] cards = {playerCard1, playerCard2, playerCard3, playerCard4};
        TextField[] names = {playerName1, playerName2, playerName3, playerName4};
        Label[] scores = {playerScore1, playerScore2, playerScore3, playerScore4};
        int[] playerScores = gameLogic.getPlayerScores();
        int activePlayerIndex = gameLogic.getCurrentPlayerNumber() - 1;

        for (int i = 0; i < cards.length; i++) {
            boolean visible = i < playerScores.length;
            cards[i].setVisible(visible);
            cards[i].setManaged(visible);
            if (!visible) {
                continue;
            }

            boolean active = i == activePlayerIndex && gameLogic.getGameState() != GameState.WON;
            setPlayerStateClasses(cards[i], active, "player-card-active", "player-card-inactive");

            String currentName = names[i].getText();
            if (!names[i].isFocused() && (currentName == null || currentName.isBlank())) {
                names[i].setText("Player " + String.format("%02d", i + 1));
            }

            setPlayerStateClasses(names[i], active, "player-name-active", "player-name-inactive");
            scores[i].setText("Điểm: " + playerScores[i]);
            setPlayerStateClasses(scores[i], active, "player-score-active", "player-score-inactive");
        }
    }

    private void setPlayerStateClasses(javafx.scene.Node node, boolean active, String activeClass, String inactiveClass) {
        node.getStyleClass().removeAll(activeClass, inactiveClass);
        node.getStyleClass().add(active ? activeClass : inactiveClass);
    }

    private String getPlayerDisplayName(int playerIndex) {
        TextField[] fields = {playerName1, playerName2, playerName3, playerName4};

        if (playerIndex >= 0 && playerIndex < fields.length && fields[playerIndex] != null) {
            String text = fields[playerIndex].getText();

            if (text != null && !text.trim().isEmpty()) {
                return text.trim();
            }
        }

        return "Player " + String.format("%02d", playerIndex + 1);
    }

    private String getCurrentPlayerDisplayName() {
        int currentIndex = gameLogic.getCurrentPlayerNumber() - 1;
        return getPlayerDisplayName(currentIndex);
    }

    /**
     * Nhận thông tin user từ SessionManager (được gọi bởi DashBoardController trước khi start game).
     */
    public void setCurrentUser(long userId, String username) {
        this.currentUserId   = userId;
        this.currentUsername = (username != null && !username.isBlank()) ? username.trim() : "Player";
    }

    public void setPlayerNames(String[] names) {
        if (names == null) return;

        TextField[] fields = {playerName1, playerName2, playerName3, playerName4};

        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == null) {
                continue;
            }

            if (i < names.length && names[i] != null && !names[i].trim().isEmpty()) {
                fields[i].setText(names[i].trim());
            } else {
                fields[i].setText("Player " + String.format("%02d", i + 1));
            }
        }
    }

    private void saveGameResult() {
        if (currentUserId <= 0) {
            LOG.warn("Bỏ qua lưu kết quả: chưa đăng nhập (userId={})", currentUserId);
            return;
        }
        if (gameLogic.isCustomGame()) {
            LOG.info("Bỏ qua lưu bảng xếp hạng cho bàn tùy chỉnh");
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

        if (gameLogic != null && gameLogic.hasGame()) {
            gameLogic.restartCurrentGame();
            resetGameStartState();
            renderBoard();
        }
    }
    @FXML
    private Button btnBlindBomb;
    private boolean isBlindBombActive = false;
    private boolean isBlindBombPending = false;

    @FXML
    private void useBlindBomb(ActionEvent event) {
        if (gameLogic == null || gameLogic.getGameState() != GameState.PLAYING) return;

        int currentScore = gameLogic.getPlayerScores()[gameLogic.getCurrentPlayerNumber() - 1];

        if (currentScore >= 100 && !isBlindBombPending && !isBlindBombActive) {
            gameLogic.deductCurrentPlayerScore(100);
            isBlindBombPending = true;
            if (btnBlindBomb != null) {
                btnBlindBomb.getStyleClass().add("item-button-active");
            }
            System.out.println("Bom mù ĐÃ GÀI! Bị trừ 100 điểm.");
            updateStatus();
        }
    }
    private void processBlindBombTurnTransition() {
        if (isBlindBombActive) {
            isBlindBombActive = false;
            System.out.println("Lượt đã kết thúc. Màn hình trở lại bình thường.");
        }

        if (isBlindBombPending) {
            isBlindBombPending = false;
            isBlindBombActive = true;

            if (btnBlindBomb != null) {
                btnBlindBomb.getStyleClass().remove("item-button-active");
                // btnBlindBomb.setDisable(true);
            }
            System.out.println("ĐỐI THỦ DÍNH BOM MÙ! Bàn cờ bị ẩn số.");
        }
        updateBoardUI();
    }
    private void updateItemUI() {
        if (btnBlindBomb == null) return;

        if (gameLogic == null || gameLogic.getGameState() != GameState.PLAYING) {
            btnBlindBomb.setDisable(true);
            btnBlindBomb.setOpacity(0.4);
            return;
        }

        if (isBlindBombPending) {
            btnBlindBomb.setDisable(true);
            btnBlindBomb.setOpacity(1.0);
            return;
        }

        int currentScore = gameLogic.getPlayerScores()[gameLogic.getCurrentPlayerNumber() - 1];
        if (currentScore < 100) {
            btnBlindBomb.setDisable(true);
            btnBlindBomb.setOpacity(0.4);
        } else {
            btnBlindBomb.setDisable(false);
            btnBlindBomb.setOpacity(1.0);
        }
    }
}
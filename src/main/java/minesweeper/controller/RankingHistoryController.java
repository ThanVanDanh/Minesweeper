package minesweeper.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import minesweeper.dto.RankingDTO;

import java.util.List;
import java.util.Map;

import javafx.stage.Stage;
import minesweeper.model.Achievement;
import minesweeper.model.GameResult;
import minesweeper.model.User;
import minesweeper.model.enums.Difficulty;
import minesweeper.repository.MySqlGameResultRepository;
import minesweeper.repository.pagination.PagedResult;
import minesweeper.repository.spec.GameResultFilterSpec;
import minesweeper.service.AchievementService;
import minesweeper.service.SessionManager;

public class RankingHistoryController {

    @FXML private javafx.scene.control.TabPane tabPane;

    // ── Tab Xếp hạng ─────────────────────────────────────────────────────────
    @FXML private ComboBox<RankingController.LevelOption> levelCombo;
    @FXML private TableView<RankingDTO>                   rankingTable;
    @FXML private TableColumn<RankingDTO, Integer>        colRank;
    @FXML private TableColumn<RankingDTO, String>         colPlayerName;
    @FXML private TableColumn<RankingDTO, Integer>        colTotalGames;
    @FXML private TableColumn<RankingDTO, Integer>        colWins;
    @FXML private TableColumn<RankingDTO, Integer>        colScore;
    @FXML private TableColumn<RankingDTO, String>         colBestTime;

    // Phân trang – tab Xếp hạng
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label  lblPage;

    // Ranking của người dùng hiện tại (hiển thị khi ra ngoài trang)
    @FXML private VBox                                    myRankBox;
    @FXML private TableView<RankingDTO>                   myRankTable;
    @FXML private TableColumn<RankingDTO, Integer>        colMyRank;
    @FXML private TableColumn<RankingDTO, String>         colMyPlayerName;
    @FXML private TableColumn<RankingDTO, Integer>        colMyTotalGames;
    @FXML private TableColumn<RankingDTO, Integer>        colMyWins;
    @FXML private TableColumn<RankingDTO, Integer>        colMyScore;
    @FXML private TableColumn<RankingDTO, String>         colMyBestTime;

    // ── Tab Lịch sử ──────────────────────────────────────────────────────────
    @FXML private ComboBox<String>                        playerCombo;

    // Bộ lọc lịch sử
    @FXML private ComboBox<DifficultyOption>              historyDifficultyCombo;
    @FXML private ComboBox<ResultOption>                  historyResultCombo;

    @FXML private TableView<GameResult>                   historyTable;
    @FXML private TableColumn<GameResult, String>         colDate;
    @FXML private TableColumn<GameResult, String>         colDifficulty;
    @FXML private TableColumn<GameResult, String>         colResult;
    @FXML private TableColumn<GameResult, String>         colTime;
    @FXML private TableColumn<GameResult, Integer>        colHistoryScore;

    // Phân trang – tab Lịch sử
    @FXML private Button                                  btnHistoryPrev;
    @FXML private Button                                  btnHistoryNext;
    @FXML private Label                                   lblHistoryPage;

    // ── Tab Thống kê ─────────────────────────────────────────────────────────
    @FXML private ComboBox<String> statsPlayerCombo;
    @FXML private Label statsTotalGames;
    @FXML private Label statsWins;
    @FXML private Label statsWinRate;
    @FXML private Label statsBestScore;
    @FXML private Label statsAvgTime;
    @FXML private Label statsWinStreak;
    @FXML private Label statsMaxWinStreak;

    // ── Tab Thành tựu ────────────────────────────────────────────────────────
    @FXML private VBox achievementList;

    // ── Services / Repositories ──────────────────────────────────────────────
    private final RankingController         rankingController    = new RankingController();
    private final MySqlGameResultRepository gameResultRepository = new MySqlGameResultRepository();
    private final AchievementService        achievementService   = new AchievementService();

    // ── Trạng thái phân trang – Xếp hạng ────────────────────────────────────
    private static final int RANKING_PAGE_SIZE = 10;
    private int              rankingCurrentPage = 0;
    private List<RankingDTO> allRankings        = List.of();

    // ── Trạng thái phân trang – Lịch sử ─────────────────────────────────────
    private static final int HISTORY_PAGE_SIZE = 10;
    private int              historyCurrentPage = 0;
    private long             historyTotalItems  = 0;

    /**
     * UC04.1 – Điểm khởi tạo controller sau khi FXMLLoader nạp FXML xong.
     * Thứ tự gọi tương ứng với luồng cơ bản:
     *   Bước 04.1.2 → setupRankingTable()
     *   Bước 04.1.3 → setupExpertOnlyLevelFilter()
     *   UC04.2 – Bước 04.2.2:
     */
    @FXML
    private void initialize() {
        // 04.1.2 → setupRankingTable()
        setupRankingTable();
        // 04.1.3 → setupExpertOnlyLevelFilter()
        setupExpertOnlyLevelFilter();
        // 04.2.2 → setupHistoryAndStats()
        setupHistoryAndStats();
    }

    //  Tab Xếp hạng
    // 04.1.2 → setupRankingTable()
    private void setupRankingTable() {
        colRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colPlayerName.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        colTotalGames.setCellValueFactory(new PropertyValueFactory<>("totalGames"));
        colWins.setCellValueFactory(new PropertyValueFactory<>("wins"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("bestScore"));
        colBestTime.setCellValueFactory(new PropertyValueFactory<>("bestTimeFormatted"));

        colMyRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colMyPlayerName.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        colMyTotalGames.setCellValueFactory(new PropertyValueFactory<>("totalGames"));
        colMyWins.setCellValueFactory(new PropertyValueFactory<>("wins"));
        colMyScore.setCellValueFactory(new PropertyValueFactory<>("bestScore"));
        colMyBestTime.setCellValueFactory(new PropertyValueFactory<>("bestTimeFormatted"));

        rankingTable.setRowFactory(tv -> buildHighlightRow());
        myRankTable.setRowFactory(tv -> buildHighlightRow());

        // Ẩn header của bảng "hạng của bạn"
        myRankTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                javafx.application.Platform.runLater(() -> {
                    var header = myRankTable.lookup("TableHeaderRow");
                    if (header != null) {
                        header.setVisible(false);
                        header.setManaged(false);
                    }
                    myRankTable.lookupAll(".scroll-bar").forEach(node -> {
                        if (node instanceof javafx.scene.control.ScrollBar sb) {
                            sb.setVisible(false);
                            sb.setManaged(false);
                        }
                    });
                });
            }
        });
    }

    private TableRow<RankingDTO> buildHighlightRow() {
        return new TableRow<>() {
            @Override
            protected void updateItem(RankingDTO item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("current-user-row");
                if (!empty && item != null && isCurrentUser(item.getPlayerName())) {
                    getStyleClass().add("current-user-row");
                }
            }
        };
    }

    private boolean isCurrentUser(String playerName) {
        if (!SessionManager.isLoggedIn()) return false;
        String username = SessionManager.getCurrentUser().getUsername();
        return username != null && username.equalsIgnoreCase(playerName);
    }

    // 04.1.3 → setupExpertOnlyLevelFilter()
    private void setupExpertOnlyLevelFilter() {
        List<RankingController.LevelOption> allLevels;
        try {
            allLevels = rankingController.getAllLevels();
        } catch (Exception e) {
            allLevels = List.of();
        }

        levelCombo.setItems(FXCollections.observableArrayList(allLevels));
        if (!allLevels.isEmpty()) {
            levelCombo.getSelectionModel().selectFirst();
            levelCombo.setOnAction(event -> loadRankingBySelectedLevel());
            loadRankingBySelectedLevel();
        }
    }

    // 04.1.4 Hệ thống kích hoạt hàm loadRankingBySelectedLevel()
    private void loadRankingBySelectedLevel() {
        RankingController.LevelOption selected = levelCombo.getSelectionModel().getSelectedItem();
        if (selected == null) {
            // UC04.1-A1: không có cấp độ được chọn → set bảng rỗng
            rankingTable.setItems(FXCollections.observableArrayList());
            return;
        }

        try {
            // Bước 04.1.4: lấy top 50 theo cấp độ được chọn
            // Bước 04.1.5: chuẩn hoá DTO
            allRankings = rankingController.getRankingTop(selected.getId(), 50);
            rankingCurrentPage = 0;

            // Bước 04.1.6: tự động nhảy đến trang chứa người dùng hiện tại
            if (SessionManager.isLoggedIn()) {
                String me = SessionManager.getCurrentUser().getUsername();
                for (int i = 0; i < allRankings.size(); i++) {
                    if (allRankings.get(i).getPlayerName().equalsIgnoreCase(me)) {
                        rankingCurrentPage = i / RANKING_PAGE_SIZE;
                        break;
                    }
                }
            }
            renderRankingPage();
        } catch (Exception e) {
            // UC04.1-E1: lỗi CSDL → set bảng rỗng, ghi log, không crash UI
            allRankings = List.of();
            renderRankingPage();
            e.printStackTrace();
        }
    }
    // 04.1.7 Hệ thống gọi renderPage(), cắt danh sách theo trang hiện tại
    private void renderRankingPage() {
        int from  = rankingCurrentPage * RANKING_PAGE_SIZE;
        int to    = Math.min(from + RANKING_PAGE_SIZE, allRankings.size());
        List<RankingDTO> slice = from < to ? allRankings.subList(from, to) : List.of();
        rankingTable.setItems(FXCollections.observableArrayList(slice));
        // Bước 04.1.8: cập nhật điều khiển phân trang
        updateRankingPaginationControls();
        // Bước 04.1.9: hiển thị / ẩn hàng ghim hạng
        renderMyRank();
    }

    private void renderMyRank() {
        if (myRankBox == null) return;
        myRankBox.setVisible(false);
        myRankBox.setManaged(false);

        if (!SessionManager.isLoggedIn()) return;

        String me = SessionManager.getCurrentUser().getUsername();
        RankingDTO myRow = allRankings.stream()
                .filter(r -> r.getPlayerName().equalsIgnoreCase(me))
                .findFirst().orElse(null);
        if (myRow == null) return;

        int from = rankingCurrentPage * RANKING_PAGE_SIZE;
        int to   = Math.min(from + RANKING_PAGE_SIZE, allRankings.size());
        boolean visibleInPage = allRankings.subList(from, to).stream()
                .anyMatch(r -> r.getPlayerName().equalsIgnoreCase(me));
        if (visibleInPage) return;

        myRankTable.setItems(FXCollections.observableArrayList(myRow));
        myRankBox.setVisible(true);
        myRankBox.setManaged(true);
    }

    private void updateRankingPaginationControls() {
        int total   = (int) Math.ceil((double) allRankings.size() / RANKING_PAGE_SIZE);
        int display = allRankings.isEmpty() ? 0 : total;
        lblPage.setText("Trang " + (rankingCurrentPage + 1) + " / " + Math.max(1, display));
        btnPrev.setDisable(rankingCurrentPage == 0);
        btnNext.setDisable(rankingCurrentPage >= display - 1);
    }

    @FXML private void onPrevPage() {
        if (rankingCurrentPage > 0) { rankingCurrentPage--; renderRankingPage(); }
    }
    @FXML private void onNextPage() {
        int total = (int) Math.ceil((double) allRankings.size() / RANKING_PAGE_SIZE);
        if (rankingCurrentPage < total - 1) { rankingCurrentPage++; renderRankingPage(); }
    }

    //  Tab Lịch sử
    // UC04.2 – Bước 04.2.2 / 04.2.3 / 04.2.4
    // Khởi tạo toàn bộ tab Lịch sử và tab Thống kê:

    private void setupHistoryAndStats() {
        // Cột bảng lịch sử
        colDate.setCellValueFactory(new PropertyValueFactory<>("playedAtFormatted"));
        colDifficulty.setCellValueFactory(new PropertyValueFactory<>("difficultyLabel"));
        colResult.setCellValueFactory(new PropertyValueFactory<>("result"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timeFormatted"));
        colHistoryScore.setCellValueFactory(new PropertyValueFactory<>("score"));

        // 04.2.3 Hệ thống khởi tạo bộ lọc
        historyDifficultyCombo.setItems(FXCollections.observableArrayList(
                DifficultyOption.ALL,
                new DifficultyOption(Difficulty.EASY),
                new DifficultyOption(Difficulty.MEDIUM),
                new DifficultyOption(Difficulty.HARD),
                new DifficultyOption(Difficulty.EXPERT)
        ));
        historyDifficultyCombo.getSelectionModel().selectFirst();

        // Khởi tạo ComboBox lọc kết quả: "Tất cả" + Thắng + Thua
        historyResultCombo.setItems(FXCollections.observableArrayList(
                ResultOption.ALL,
                ResultOption.WIN,
                ResultOption.LOSE
        ));
        historyResultCombo.getSelectionModel().selectFirst();

        // Khi thay đổi bộ lọc → reset trang 0 và tải lại (UC04.2-A2)
        historyDifficultyCombo.setOnAction(e -> onHistoryFilterChanged());
        historyResultCombo.setOnAction(e -> onHistoryFilterChanged());

        // Bước 04.2.2: kiểm tra trạng thái đăng nhập
        if (!SessionManager.isLoggedIn()) {
            playerCombo.setItems(FXCollections.observableArrayList("Vui lòng đăng nhập"));
            statsPlayerCombo.setItems(FXCollections.observableArrayList("Vui lòng đăng nhập"));
            playerCombo.getSelectionModel().selectFirst();
            statsPlayerCombo.getSelectionModel().selectFirst();
            return;
        }

        // 04.2.4 Hệ thống lấy username từ SessionManager.getCurrentUser().
        User currentUser = SessionManager.getCurrentUser();
        String username  = currentUser.getUsername();
        //04.2.5 Hệ thống nạp username vào ComboBox playerCombo và khóa ComboBox này lại (setDisable(true)).
        playerCombo.setItems(FXCollections.observableArrayList(username));
        playerCombo.getSelectionModel().selectFirst();
        playerCombo.setDisable(true);

        statsPlayerCombo.setItems(FXCollections.observableArrayList(username));
        statsPlayerCombo.getSelectionModel().selectFirst();
        statsPlayerCombo.setDisable(true);

        // 04.2.6 Hệ thống gọi hàm loadPlayerHistoryAndStats(username).
        loadPlayerHistoryPaged(username, 0);
        loadPlayerStats(username);
    }

    //  Tab Lịch sử – lọc & phân trang

    /**
     * UC04.2 – Alternative Flow A2: Người dùng áp dụng bộ lọc.
     */
    private void onHistoryFilterChanged() {
        historyCurrentPage = 0;
        if (!SessionManager.isLoggedIn()) return;
        loadPlayerHistoryPaged(SessionManager.getCurrentUser().getUsername(), 0);
    }

    /**
     * Xây dựng GameResultFilterSpec từ trạng thái combobox hiện tại.
     */
    private GameResultFilterSpec buildHistoryFilterSpec(String username) {
        GameResultFilterSpec spec = new GameResultFilterSpec();
        spec.username = username;

        DifficultyOption diffOpt = historyDifficultyCombo.getSelectionModel().getSelectedItem();
        if (diffOpt != null && diffOpt.difficulty != null) {
            spec.difficulty = diffOpt.difficulty;
        }

        ResultOption resultOpt = historyResultCombo.getSelectionModel().getSelectedItem();
        if (resultOpt != null && resultOpt.win != null) {
            spec.win = resultOpt.win;
        }

        return spec;
    }

    /**
     * Tải một trang lịch sử từ DB theo filter và số trang.
     */
    private void loadPlayerHistoryPaged(String username, int page) {
        historyCurrentPage = page;
        try {
            // 04.2.7 MySqlGameResultRepository truy vấn và trả về danh sách lịch sử.
            GameResultFilterSpec spec = buildHistoryFilterSpec(username);
            PagedResult<GameResult> result =
                    gameResultRepository.findPaged(spec, page, HISTORY_PAGE_SIZE);

            historyTotalItems = result.getTotalElements();
            historyTable.setItems(FXCollections.observableArrayList(result.getContent()));
            updateHistoryPaginationControls();
        } catch (Exception e) {
            e.printStackTrace();
            //04.2.8 Hệ thống đổ dữ liệu lên historyTable qua các cột: Thời gian, Độ khó, Kết quả, TG chơi, Điểm số.
            historyTable.setItems(FXCollections.observableArrayList());
            historyTotalItems = 0;
            updateHistoryPaginationControls();
        }
    }

    private void updateHistoryPaginationControls() {
        int totalPages = (int) Math.ceil((double) historyTotalItems / HISTORY_PAGE_SIZE);
        int display    = historyTotalItems == 0 ? 0 : totalPages;
        lblHistoryPage.setText("Trang " + (historyCurrentPage + 1) + " / " + Math.max(1, display));
        btnHistoryPrev.setDisable(historyCurrentPage == 0);
        btnHistoryNext.setDisable(historyCurrentPage >= display - 1);
    }

    @FXML private void onHistoryPrevPage() {
        if (historyCurrentPage > 0 && SessionManager.isLoggedIn()) {
            loadPlayerHistoryPaged(
                    SessionManager.getCurrentUser().getUsername(), historyCurrentPage - 1);
        }
    }

    @FXML private void onHistoryNextPage() {
        int totalPages = (int) Math.ceil((double) historyTotalItems / HISTORY_PAGE_SIZE);
        if (historyCurrentPage < totalPages - 1 && SessionManager.isLoggedIn()) {
            loadPlayerHistoryPaged(
                    SessionManager.getCurrentUser().getUsername(), historyCurrentPage + 1);
        }
    }

    //  Tab Thống kê (tính trên toàn bộ lịch sử, không bị ảnh hưởng bởi filter)
    private void loadPlayerStats(String username) {
        try {
            // 04.3.1 Người dùng mở tab Thống kê.
            List<GameResult> history = gameResultRepository.getPlayerHistory(username);

            // 04.3.2 Hệ thống khởi tạo các biến đếm: totalGames, wins, bestScore, totalWinTimeMs.
            int  totalGames    = history.size();
            int  wins          = 0;
            int  bestScore     = 0;
            long totalWinTimeMs = 0;

            // 04.3.3 Hệ thống duyệt qua danh sách history.
            for (GameResult r : history) {
                if (r.isWon()) {
                    wins++;
                    totalWinTimeMs += r.getElapsedTimeMs();
                }
                if (r.getScore() > bestScore) bestScore = r.getScore();
            }

            // 04.3.4 Hệ thống tính Tỉ lệ thắng
            statsTotalGames.setText(String.valueOf(totalGames));
            statsWins.setText(wins + " / " + (totalGames - wins));

            double winRate = totalGames > 0 ? (double) wins / totalGames * 100 : 0;
            statsWinRate.setText(String.format("%.1f%%", winRate));
            statsBestScore.setText(String.format("%,d", bestScore));

            // 04.3.5 Hệ thống tính Thời gian trung bình
            // 04.3-A1 Người chơi chưa thắng ván nào → hiển thị "0 giây" hoặc "N/A"
            if (wins > 0) {
                statsAvgTime.setText((totalWinTimeMs / wins / 1000) + " giây");
            } else {
                statsAvgTime.setText("0 giây");
            }

            // 04.3.6 Hệ thống tính Win Streak dài nhất (maxStreak)
            int currentStreak = 0, maxStreak = 0, tempStreak = 0;
            for (int i = history.size() - 1; i >= 0; i--) {
                if (history.get(i).isWon()) {
                    tempStreak++;
                    if (tempStreak > maxStreak) maxStreak = tempStreak;
                } else {
                    tempStreak = 0;
                }
            }

            // 04.3.7 Hệ thống tính Win Streak hiện tại (currentStreak)
            for (GameResult r : history) {
                if (r.isWon()) currentStreak++;
                else break;
            }

            // 04.3.8 Hệ thống cập nhật số liệu lên các Label giao diện tương ứng.
            statsWinStreak.setText(currentStreak > 0 ? currentStreak + " 🔥" : "0");
            statsMaxWinStreak.setText(maxStreak > 0 ? maxStreak + " ⭐" : "0");

            // Render achievement cards dựa trên lịch sử
            renderAchievements(history);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * UC04.5 - Xem thành tựu cá nhân
     */
    private void renderAchievements(List<GameResult> history) {
        // Pre-condition: người chơi đã đăng nhập và `history` đã được lấy (có thể là danh sách rỗng).
        if (achievementList == null) return;
        achievementList.getChildren().clear();

        try {
            // 04.5.2: Gọi service để đánh giá các achievement dựa trên lịch sử.
            // 04.5.3 — Duyệt enum Achievement & gọi hàm kiểm tra
            // 04.5.4 — Hàm kiểm chuỗi liên tiế
            // 04.5.5 evaluate() trả về Map<Achievement, Boolean> — mỗi thành tựu ánh xạ tới kết quả true/false.
            Map<Achievement, Boolean> results = achievementService.evaluate(history);

            // 04.5.6: Duyệt qua toàn bộ enum Achievement và render một card cho mỗi thành tựu.
            for (Achievement achievement : Achievement.values()) {
                boolean unlocked = Boolean.TRUE.equals(results.get(achievement));

                javafx.scene.layout.HBox card = new javafx.scene.layout.HBox(16);
                card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                card.setPadding(new Insets(14, 18, 14, 18));

                // 04.5.7: Card thành tựu đã đạt được render
                card.setStyle(unlocked
                        ? "-fx-background-color: rgba(0,255,180,0.10); -fx-background-radius: 10;"
                        + " -fx-border-color: #00ffcc; -fx-border-radius: 10; -fx-border-width: 1.5;"
                        : "-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 10;"
                        + " -fx-border-color: #444; -fx-border-radius: 10; -fx-border-width: 1;");

                Label iconLabel = new Label(unlocked ? achievement.getIcon() : "🔒");
                iconLabel.setStyle("-fx-font-size: 28px;");

                VBox textBlock = new VBox(4);
                Label nameLabel = new Label(achievement.getDisplayName());
                nameLabel.setStyle(unlocked
                        ? "-fx-text-fill: #00ffcc; -fx-font-size: 14px; -fx-font-weight: bold;"
                        : "-fx-text-fill: #888888; -fx-font-size: 14px; -fx-font-weight: bold;");
                Label descLabel = new Label(achievement.getDescription());
                descLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");
                textBlock.getChildren().addAll(nameLabel, descLabel);

                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                Label badge = new Label(unlocked ? "✔ Đã đạt" : "Chưa đạt");
                badge.setStyle(unlocked
                        ? "-fx-text-fill: #00ffcc; -fx-font-size: 12px; -fx-font-weight: bold;"
                        : "-fx-text-fill: #666666; -fx-font-size: 12px;");

                card.getChildren().addAll(iconLabel, textBlock, spacer, badge);
                achievementList.getChildren().add(card);
            }
        } catch (Exception e) {
            // 04.5-E1: Bắt lỗi truy xuất / đánh giá, in ra console (không cho crash UI).
            e.printStackTrace();
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void closePopup() {
        if (levelCombo != null && levelCombo.getScene() != null) {
            ((Stage) levelCombo.getScene().getWindow()).close();
        }
    }

    /** Chuyển sang Tab Lịch sử (index 1) từ màn hình ngoài. */
    public void selectHistoryTab() {
        if (tabPane != null) tabPane.getSelectionModel().select(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Inner types – wrapper cho ComboBox lọc
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Item hiển thị trong ComboBox lọc độ khó.
     * difficulty == null  →  "Tất cả độ khó" (không lọc).
     */
    public static class DifficultyOption {
        static final DifficultyOption ALL = new DifficultyOption(null);
        final Difficulty difficulty;

        DifficultyOption(Difficulty difficulty) { this.difficulty = difficulty; }

        @Override
        public String toString() {
            return difficulty == null ? "Tất cả độ khó" : difficulty.getLabel();
        }
    }

    /**
     * Item hiển thị trong ComboBox lọc kết quả.
     * win == null  →  "Tất cả kết quả" (không lọc).
     */
    public static class ResultOption {
        static final ResultOption ALL  = new ResultOption(null,  "Tất cả kết quả");
        static final ResultOption WIN  = new ResultOption(true,  "Thắng");
        static final ResultOption LOSE = new ResultOption(false, "Thua");

        final Boolean win;
        final String  label;

        ResultOption(Boolean win, String label) { this.win = win; this.label = label; }

        @Override public String toString() { return label; }
    }
}
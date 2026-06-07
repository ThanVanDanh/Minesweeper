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

/**
 * Controller chính quản lý màn hình Xếp hạng / Lịch sử / Thống kê / Thành tựu.
 */
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

    /**
     * [BỔ SUNG – UC04.1] Cột Best Time hiển thị thời gian hoàn thành nhanh nhất
     * của người chơi theo định dạng "m:ss". Hiển thị "—" nếu chưa có dữ liệu.
     */
    @FXML private TableColumn<RankingDTO, String>         colBestTime;

    // Phân trang – tab Xếp hạng
    /**
     * [BỔ SUNG – UC04.1] Nút điều hướng và nhãn phân trang cho bảng xếp hạng.
     * Mỗi trang hiển thị tối đa RANKING_PAGE_SIZE = 10 bản ghi.
     * Nút bị vô hiệu hoá khi đã ở trang đầu/cuối.
     */
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label  lblPage;

    /**
     * [BỔ SUNG – UC04.1] Khu vực ghim hạng của người dùng hiện tại.
     * Chỉ hiển thị khi hàng của người dùng KHÔNG nằm trong trang đang xem,
     * giúp người dùng luôn biết vị trí xếp hạng của mình dù đang ở trang nào.
     */
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

    /**
     * [BỔ SUNG – UC04.2] Bộ lọc lịch sử theo độ khó và kết quả ván chơi.
     * Khi người dùng thay đổi bộ lọc, hệ thống reset về trang 0 và truy vấn
     * lại DB với điều kiện mới (Alternative Flow A2).
     */
    @FXML private ComboBox<DifficultyOption>              historyDifficultyCombo;
    @FXML private ComboBox<ResultOption>                  historyResultCombo;

    @FXML private TableView<GameResult>                   historyTable;
    @FXML private TableColumn<GameResult, String>         colDate;
    @FXML private TableColumn<GameResult, String>         colDifficulty;
    @FXML private TableColumn<GameResult, String>         colResult;
    @FXML private TableColumn<GameResult, String>         colTime;
    @FXML private TableColumn<GameResult, Integer>        colHistoryScore;

    /**
     * [BỔ SUNG – UC04.2] Nút điều hướng và nhãn phân trang cho tab Lịch sử.
     * Khác với tab Xếp hạng (phân trang in-memory), tab Lịch sử truy vấn DB
     * theo từng trang thông qua GameResultFilterSpec + PagedResult.
     */
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

    /**
     * [BỔ SUNG – UC04.3] Hai chỉ số Win Streak mới được thêm vào tab Thống kê
     */
    @FXML private Label statsWinStreak;
    @FXML private Label statsMaxWinStreak;

    // ── Tab Thành tựu ────────────────────────────────────────────────────────
    /**
     * [BỔ SUNG – UC04.5] VBox chứa danh sách card thành tựu được render động.
     */
    @FXML private VBox achievementList;

    // ── Services / Repositories ──────────────────────────────────────────────
    private final RankingController         rankingController    = new RankingController();
    private final MySqlGameResultRepository gameResultRepository = new MySqlGameResultRepository();
    private final AchievementService        achievementService   = new AchievementService();

    // ── Trạng thái phân trang – Xếp hạng ────────────────────────────────────
    /**
     * [BỔ SUNG – UC04.1] Hằng số và biến trạng thái phân trang cho bảng xếp hạng.
     */
    private static final int RANKING_PAGE_SIZE = 10;
    private int              rankingCurrentPage = 0;
    private List<RankingDTO> allRankings        = List.of();

    // ── Trạng thái phân trang – Lịch sử ─────────────────────────────────────
    /**
     * [BỔ SUNG – UC04.2] Hằng số và biến trạng thái phân trang cho tab Lịch sử.
     */
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

    // =========================================================================
    //  Tab Xếp hạng
    // =========================================================================

    /**
     * [UC04.1 – Bước 04.1.2] Cấu hình các cột cho bảng xếp hạng chính và
     * bảng mini ghim hạng (myRankTable).
     *
     * <p>[BỔ SUNG] colBestTime và colMyBestTime bind với thuộc tính
     * "bestTimeFormatted" của RankingDTO — được định dạng sẵn dạng "m:ss"
     * hoặc "—" bên trong DTO, controller không cần xử lý thêm.
     *
     * <p>[BỔ SUNG] rowFactory được gán để highlight hàng của người dùng
     * hiện tại bằng CSS class "current-user-row" (nền vàng mờ, chữ #ffe066).
     *
     * <p>[BỔ SUNG] Header và scrollbar của myRankTable bị ẩn để giao diện
     * ghim hạng trông gọn gàng, chỉ hiện nội dung dữ liệu.
     */
    private void setupRankingTable() {
        colRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colPlayerName.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        colTotalGames.setCellValueFactory(new PropertyValueFactory<>("totalGames"));
        colWins.setCellValueFactory(new PropertyValueFactory<>("wins"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("bestScore"));
        // [BỔ SUNG – UC04.1] Bind cột Best Time
        colBestTime.setCellValueFactory(new PropertyValueFactory<>("bestTimeFormatted"));

        colMyRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colMyPlayerName.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        colMyTotalGames.setCellValueFactory(new PropertyValueFactory<>("totalGames"));
        colMyWins.setCellValueFactory(new PropertyValueFactory<>("wins"));
        colMyScore.setCellValueFactory(new PropertyValueFactory<>("bestScore"));
        // [BỔ SUNG – UC04.1] Bind cột Best Time cho bảng ghim
        colMyBestTime.setCellValueFactory(new PropertyValueFactory<>("bestTimeFormatted"));

        // [BỔ SUNG – UC04.1] Gán rowFactory highlight người dùng hiện tại
        rankingTable.setRowFactory(tv -> buildHighlightRow());
        myRankTable.setRowFactory(tv -> buildHighlightRow());

        // [BỔ SUNG – UC04.1] Ẩn header và scrollbar của bảng ghim hạng
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

    /**
     * [BỔ SUNG – UC04.1] Tạo TableRow có khả năng highlight hàng của người
     * dùng đang đăng nhập bằng cách thêm/xoá CSS class "current-user-row".
     * Được dùng cho cả rankingTable và myRankTable.
     */
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

    /**
     * [BỔ SUNG – UC04.1] Kiểm tra xem tên người chơi có khớp với tài khoản
     */
    private boolean isCurrentUser(String playerName) {
        if (!SessionManager.isLoggedIn()) return false;
        String username = SessionManager.getCurrentUser().getUsername();
        return username != null && username.equalsIgnoreCase(playerName);
    }

    /**
     * [UC04.1 – Bước 04.1.3] Khởi tạo ComboBox lọc theo cấp độ và tải
     * bảng xếp hạng lần đầu khi mở màn hình.
     * Nếu không lấy được danh sách cấp độ, bảng sẽ hiển thị rỗng (không crash UI).
     */
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

    /**
     * [UC04.1 – Bước 04.1.4] Tải dữ liệu xếp hạng từ DB theo cấp độ được chọn.
     *
     * <p>[BỔ SUNG – UC04.1] Sau khi tải xong, tự động nhảy đến trang chứa
     * người dùng hiện tại (nếu đã đăng nhập) thay vì luôn về trang 1.
     */
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

            // [BỔ SUNG – UC04.1] Tự động nhảy đến trang chứa người dùng hiện tại
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

    /**
     * [UC04.1 – Bước 04.1.7] Cắt danh sách allRankings theo trang hiện tại
     * và đổ lên rankingTable.
     *
     * <p>[BỔ SUNG – UC04.1] Sau khi render dữ liệu trang, gọi thêm:
     * <ul>
     *   <li>updateRankingPaginationControls() – cập nhật nhãn trang và
     *       trạng thái nút Trước/Sau (Bước 04.1.8)</li>
     *   <li>renderMyRank() – hiển thị hoặc ẩn khu vực ghim hạng (Bước 04.1.9)</li>
     * </ul>
     */
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

    /**
     * [BỔ SUNG – UC04.1] Hiển thị khu vực ghim hạng (myRankBox) bên dưới
     * thanh phân trang nếu hàng của người dùng KHÔNG xuất hiện trong trang
     * hiện tại. Ngược lại, ẩn myRankBox để tránh hiển thị trùng lặp.
     */
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

    /**
     * [BỔ SUNG – UC04.1 – Bước 04.1.8] Cập nhật nhãn "Trang X / Y" và
     * vô hiệu hoá nút Trước/Sau khi đã ở trang đầu hoặc trang cuối.
     * Nếu danh sách rỗng, hiển thị "Trang 1 / 1" (không hiển thị "Trang 1 / 0").
     */
    private void updateRankingPaginationControls() {
        int total   = (int) Math.ceil((double) allRankings.size() / RANKING_PAGE_SIZE);
        int display = allRankings.isEmpty() ? 0 : total;
        lblPage.setText("Trang " + (rankingCurrentPage + 1) + " / " + Math.max(1, display));
        btnPrev.setDisable(rankingCurrentPage == 0);
        btnNext.setDisable(rankingCurrentPage >= display - 1);
    }

    /** [BỔ SUNG – UC04.1] Xử lý sự kiện nút "‹ Trước" — lùi một trang. */
    @FXML private void onPrevPage() {
        if (rankingCurrentPage > 0) { rankingCurrentPage--; renderRankingPage(); }
    }

    /** [BỔ SUNG – UC04.1] Xử lý sự kiện nút "Sau ›" — tiến một trang. */
    @FXML private void onNextPage() {
        int total = (int) Math.ceil((double) allRankings.size() / RANKING_PAGE_SIZE);
        if (rankingCurrentPage < total - 1) { rankingCurrentPage++; renderRankingPage(); }
    }

    // =========================================================================
    //  Tab Lịch sử
    // =========================================================================

    /**
     * [UC04.2 – Bước 04.2.2 / 04.2.3 / 04.2.4] Khởi tạo toàn bộ tab Lịch sử
     * và tab Thống kê khi controller được nạp
     */
    private void setupHistoryAndStats() {
        // Cột bảng lịch sử
        colDate.setCellValueFactory(new PropertyValueFactory<>("playedAtFormatted"));
        colDifficulty.setCellValueFactory(new PropertyValueFactory<>("difficultyLabel"));
        colResult.setCellValueFactory(new PropertyValueFactory<>("result"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timeFormatted"));
        colHistoryScore.setCellValueFactory(new PropertyValueFactory<>("score"));

        // [BỔ SUNG – UC04.2 – Bước 04.2.3] Khởi tạo ComboBox lọc độ khó
        historyDifficultyCombo.setItems(FXCollections.observableArrayList(
                DifficultyOption.ALL,
                new DifficultyOption(Difficulty.EASY),
                new DifficultyOption(Difficulty.MEDIUM),
                new DifficultyOption(Difficulty.HARD),
                new DifficultyOption(Difficulty.EXPERT)
        ));
        historyDifficultyCombo.getSelectionModel().selectFirst();

        // [BỔ SUNG – UC04.2] Khởi tạo ComboBox lọc kết quả: Tất cả / Thắng / Thua
        historyResultCombo.setItems(FXCollections.observableArrayList(
                ResultOption.ALL,
                ResultOption.WIN,
                ResultOption.LOSE
        ));
        historyResultCombo.getSelectionModel().selectFirst();

        // [BỔ SUNG – UC04.2] Listener: thay đổi bộ lọc → reset trang 0 và tải lại (Alternative Flow A2)
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

        // 04.2.4: Lấy username từ SessionManager
        User currentUser = SessionManager.getCurrentUser();
        String username  = currentUser.getUsername();

        // 04.2.5: Nạp username vào ComboBox playerCombo và khoá lại
        playerCombo.setItems(FXCollections.observableArrayList(username));
        playerCombo.getSelectionModel().selectFirst();
        playerCombo.setDisable(true);

        statsPlayerCombo.setItems(FXCollections.observableArrayList(username));
        statsPlayerCombo.getSelectionModel().selectFirst();
        statsPlayerCombo.setDisable(true);

        // 04.2.6: Tải trang đầu và thống kê
        loadPlayerHistoryPaged(username, 0);
        loadPlayerStats(username);
    }

    /**
     * [BỔ SUNG – UC04.2 – Alternative Flow A2] Xử lý khi người dùng thay đổi
     * bộ lọc (độ khó hoặc kết quả). Reset về trang 0 và tải lại dữ liệu
     * với bộ lọc mới.
     */
    private void onHistoryFilterChanged() {
        historyCurrentPage = 0;
        if (!SessionManager.isLoggedIn()) return;
        loadPlayerHistoryPaged(SessionManager.getCurrentUser().getUsername(), 0);
    }

    /**
     * [BỔ SUNG – UC04.2] Xây dựng GameResultFilterSpec từ trạng thái hiện tại
     * của hai ComboBox bộ lọc. Nếu chọn "Tất cả" thì trường tương ứng trong
     * spec sẽ là null (không áp dụng điều kiện lọc cho trường đó).
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
     * [BỔ SUNG – UC04.2 – Bước 04.2.7] Tải một trang lịch sử từ DB theo
     * bộ lọc hiện tại và số trang được yêu cầu. Dữ liệu được truy vấn bằng
     * LIMIT/OFFSET thông qua gameResultRepository.findPaged().
     */
    private void loadPlayerHistoryPaged(String username, int page) {
        historyCurrentPage = page;
        try {
            // 04.2.7: Truy vấn DB theo filter và số trang
            GameResultFilterSpec spec = buildHistoryFilterSpec(username);
            PagedResult<GameResult> result =
                    gameResultRepository.findPaged(spec, page, HISTORY_PAGE_SIZE);

            historyTotalItems = result.getTotalElements();
            // 04.2.8: Đổ dữ liệu lên historyTable
            historyTable.setItems(FXCollections.observableArrayList(result.getContent()));
            updateHistoryPaginationControls();
        } catch (Exception e) {
            e.printStackTrace();
            historyTable.setItems(FXCollections.observableArrayList());
            historyTotalItems = 0;
            updateHistoryPaginationControls();
        }
    }

    /**
     * [BỔ SUNG – UC04.2] Cập nhật nhãn trang và trạng thái nút Trước/Sau
     * cho tab Lịch sử dựa trên historyTotalItems và historyCurrentPage.
     */
    private void updateHistoryPaginationControls() {
        int totalPages = (int) Math.ceil((double) historyTotalItems / HISTORY_PAGE_SIZE);
        int display    = historyTotalItems == 0 ? 0 : totalPages;
        lblHistoryPage.setText("Trang " + (historyCurrentPage + 1) + " / " + Math.max(1, display));
        btnHistoryPrev.setDisable(historyCurrentPage == 0);
        btnHistoryNext.setDisable(historyCurrentPage >= display - 1);
    }

    /** [BỔ SUNG – UC04.2] Xử lý sự kiện nút "‹ Trước" cho tab Lịch sử. */
    @FXML private void onHistoryPrevPage() {
        if (historyCurrentPage > 0 && SessionManager.isLoggedIn()) {
            loadPlayerHistoryPaged(
                    SessionManager.getCurrentUser().getUsername(), historyCurrentPage - 1);
        }
    }

    /** [BỔ SUNG – UC04.2] Xử lý sự kiện nút "Sau ›" cho tab Lịch sử. */
    @FXML private void onHistoryNextPage() {
        int totalPages = (int) Math.ceil((double) historyTotalItems / HISTORY_PAGE_SIZE);
        if (historyCurrentPage < totalPages - 1 && SessionManager.isLoggedIn()) {
            loadPlayerHistoryPaged(
                    SessionManager.getCurrentUser().getUsername(), historyCurrentPage + 1);
        }
    }

    // =========================================================================
    //  Tab Thống kê
    // =========================================================================

    /**
     * [UC04.3] Tải và tính toán toàn bộ thống kê cá nhân từ lịch sử đầy đủ
     * của người chơi (không bị ảnh hưởng bởi bộ lọc của tab Lịch sử).
     */
    private void loadPlayerStats(String username) {
        try {
            // 04.3.1: Lấy toàn bộ lịch sử (history sắp xếp DESC – ván mới nhất ở đầu)
            List<GameResult> history = gameResultRepository.getPlayerHistory(username);

            // 04.3.2: Khởi tạo các biến đếm
            int  totalGames    = history.size();
            int  wins          = 0;
            int  bestScore     = 0;
            long totalWinTimeMs = 0;

            // 04.3.3: Duyệt qua danh sách lịch sử để tích luỹ các chỉ số
            for (GameResult r : history) {
                if (r.isWon()) {
                    wins++;
                    totalWinTimeMs += r.getElapsedTimeMs();
                }
                if (r.getScore() > bestScore) bestScore = r.getScore();
            }

            // 04.3.4: Tính và hiển thị tỉ lệ thắng
            statsTotalGames.setText(String.valueOf(totalGames));
            statsWins.setText(wins + " / " + (totalGames - wins));

            double winRate = totalGames > 0 ? (double) wins / totalGames * 100 : 0;
            statsWinRate.setText(String.format("%.1f%%", winRate));
            statsBestScore.setText(String.format("%,d", bestScore));

            // 04.3.5: Tính thời gian trung bình (chỉ tính trên ván thắng)
            // 04.3-A1: Chưa có ván thắng → hiển thị "0 giây"
            if (wins > 0) {
                statsAvgTime.setText((totalWinTimeMs / wins / 1000) + " giây");
            } else {
                statsAvgTime.setText("0 giây");
            }

            // [BỔ SUNG – UC04.3 – Bước 04.3.6] Tính Win Streak DÀI NHẤT
            // Duyệt ngược từ ván cũ nhất đến mới nhất để đếm chuỗi thắng liên tiếp
            int currentStreak = 0, maxStreak = 0, tempStreak = 0;
            for (int i = history.size() - 1; i >= 0; i--) {
                if (history.get(i).isWon()) {
                    tempStreak++;
                    if (tempStreak > maxStreak) maxStreak = tempStreak;
                } else {
                    tempStreak = 0;
                }
            }

            // [BỔ SUNG – UC04.3 – Bước 04.3.7] Tính Win Streak HIỆN TẠI
            // Duyệt từ ván mới nhất (đầu list) đến khi gặp ván thua đầu tiên
            for (GameResult r : history) {
                if (r.isWon()) currentStreak++;
                else break;
            }

            // [BỔ SUNG – UC04.3 – Bước 04.3.8] Hiển thị Win Streak với emoji
            statsWinStreak.setText(currentStreak > 0 ? currentStreak + " 🔥" : "0");
            statsMaxWinStreak.setText(maxStreak > 0 ? maxStreak + " ⭐" : "0");

            // Gọi render thành tựu sau khi đã có lịch sử (UC04.5)
            renderAchievements(history);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    //  Tab Thành tựu (UC04.5)
    // =========================================================================

    /**
     * [BỔ SUNG – UC04.5] Render danh sách card thành tựu vào achievementList.
     */
    private void renderAchievements(List<GameResult> history) {
        if (achievementList == null) return;
        achievementList.getChildren().clear();

        try {
            // Bước 04.5.2 – 04.5.5: Hệ thống đánh giá từng thành tựu dựa trên lịch sử.
            // Với mỗi thành tựu, duyệt toàn bộ lịch sử từ ván cũ nhất đến mới nhất,
            // đếm chuỗi thắng hoặc thua liên tiếp; đánh dấu đã đạt nếu chuỗi đủ ngưỡng.
            Map<Achievement, Boolean> results = achievementService.evaluate(history);

            // Bước 04.5.6: Hệ thống tạo một thẻ hiển thị cho từng thành tựu theo thứ tự danh sách.
            for (Achievement achievement : Achievement.values()) {
                boolean unlocked = Boolean.TRUE.equals(results.get(achievement));

                javafx.scene.layout.HBox card = new javafx.scene.layout.HBox(16);
                card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                card.setPadding(new Insets(14, 18, 14, 18));

                // Bước 04.5.7 – 04.5.8: Hệ thống áp dụng style khác nhau cho thẻ
                // tuỳ theo trạng thái Đã đạt (nền xanh mờ, viền sáng) hoặc Chưa đạt (nền tối, viền xám).
                card.setStyle(unlocked
                        ? "-fx-background-color: rgba(0,255,180,0.10); -fx-background-radius: 10;"
                        + " -fx-border-color: #00ffcc; -fx-border-radius: 10; -fx-border-width: 1.5;"
                        : "-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 10;"
                        + " -fx-border-color: #444; -fx-border-radius: 10; -fx-border-width: 1;");

                // Đã đạt: hiện icon thật của thành tựu. Chưa đạt: hiện icon khóa 🔒.
                Label iconLabel = new Label(unlocked ? achievement.getIcon() : "🔒");
                iconLabel.setStyle("-fx-font-size: 28px;");

                // Khối văn bản gồm tên thành tựu (in đậm) và mô tả điều kiện (nhỏ hơn, mờ hơn).
                VBox textBlock = new VBox(4);
                Label nameLabel = new Label(achievement.getDisplayName());
                nameLabel.setStyle(unlocked
                        ? "-fx-text-fill: #00ffcc; -fx-font-size: 14px; -fx-font-weight: bold;"
                        : "-fx-text-fill: #888888; -fx-font-size: 14px; -fx-font-weight: bold;");
                Label descLabel = new Label(achievement.getDescription());
                descLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");
                textBlock.getChildren().addAll(nameLabel, descLabel);

                // Khoảng trống co giãn để đẩy badge trạng thái sang sát mép phải thẻ.
                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                // Badge trạng thái: "✔ Đã đạt" (xanh sáng) hoặc "Chưa đạt" (xám tối).
                Label badge = new Label(unlocked ? "✔ Đã đạt" : "Chưa đạt");
                badge.setStyle(unlocked
                        ? "-fx-text-fill: #00ffcc; -fx-font-size: 12px; -fx-font-weight: bold;"
                        : "-fx-text-fill: #666666; -fx-font-size: 12px;");

                card.getChildren().addAll(iconLabel, textBlock, spacer, badge);

                // Bước 04.5.9: Hệ thống xếp các thẻ theo chiều dọc và hiển thị lên màn hình.
                achievementList.getChildren().add(card);
            }
        } catch (Exception e) {
            // E1: Lỗi xảy ra trong quá trình đánh giá hoặc hiển thị.
            // Hệ thống ghi nhật ký lỗi; tab hiển thị rỗng, các tab khác không bị ảnh hưởng.
            e.printStackTrace();
        }
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

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

    // =========================================================================
    //  Inner types – wrapper cho ComboBox lọc
    // =========================================================================

    /**
     * [BỔ SUNG – UC04.2] Wrapper cho ComboBox lọc độ khó.
     * difficulty == null → "Tất cả độ khó" (không áp dụng điều kiện lọc).
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
     * [BỔ SUNG – UC04.2] Wrapper cho ComboBox lọc kết quả ván chơi.
     * win == null → "Tất cả kết quả" (không áp dụng điều kiện lọc).
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
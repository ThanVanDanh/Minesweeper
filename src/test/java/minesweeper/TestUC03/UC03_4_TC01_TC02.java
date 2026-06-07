package minesweeper.TestUC03;

import minesweeper.controller.GameController;
import minesweeper.model.enums.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UC03_4_TC01_TC02 {
    private GameController game;

    @BeforeEach
    void setUp() {
        game = new GameController();
        game.startCustomGame(2, 2, 1, 2);
    }

    @Test
    void UC03_TC01_moOAnToanThiCongDiemVaChuyenLuot() {
        int openedCells = game.reveal(0, 0);

        assertTrue(openedCells > 0, "Mở ô hợp lệ phải mở được ít nhất một ô an toàn");
        assertEquals(10, game.getPlayerScore(1),
                "Người chơi 1 phải được cộng 10 điểm cho ô an toàn đã mở");
        assertEquals(0, game.getPlayerScore(2), "Người chơi 2 chưa tới lượt nên chưa có điểm");
        assertEquals(2, game.getCurrentPlayerNumber(),
                "Sau khi người chơi 1 mở ô an toàn, lượt phải chuyển sang người chơi 2");
        assertEquals(GameState.PLAYING, game.getGameState(), "Ván chơi vẫn phải đang tiếp diễn");
    }

    @Test
    void UC03_TC02_hetThoiGianThiTruNamMuoiDiemVaChuyenLuot() {
        game.reveal(0, 0);
        assertEquals(2, game.getCurrentPlayerNumber(), "Người chơi 2 phải đang có lượt sau khi người chơi 1 mở ô");

        int playerTwoScoreBeforeTimeout = game.getPlayerScore(2);
        boolean skipped = game.skipCurrentTurn();

        assertTrue(skipped, "Hết thời gian phải được xử lý khi ván chơi đang diễn ra");
        assertEquals(playerTwoScoreBeforeTimeout - 50, game.getPlayerScore(2),
                "Người chơi bị hết thời gian phải bị trừ 50 điểm");
        assertEquals(1, game.getCurrentPlayerNumber(),
                "Sau khi người chơi 2 hết thời gian, lượt phải quay lại người chơi 1");
    }
}

package minesweeper.TestUC03;

import minesweeper.controller.GameController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UC03_6_TC03_TC04 {
    private GameController game;

    @BeforeEach
    void setUp() {
        game = new GameController();
        game.startCustomGame(2, 2, 1, 2);
        game.reveal(0, 0);
        assertEquals(2, game.getCurrentPlayerNumber(), "Người chơi 2 phải là người đang có lượt để kiểm thử vật phẩm");
    }

    @Test
    void UC03_TC03_dungBomMuThiTruMotTramDiemNguoiChoiHienTai() throws Exception {
        setCurrentPlayerScore(250);

        game.deductCurrentPlayerScore(100);

        assertEquals(150, game.getPlayerScore(2), "Dùng bom mù phải trừ 100 điểm của người chơi hiện tại");
        assertEquals(10, game.getPlayerScore(1), "Điểm của người chơi khác không được thay đổi");
    }

    @Test
    void UC03_TC04_truChiPhiBomMuKhongLamDiemBiAm() throws Exception {
        setCurrentPlayerScore(50);

        game.deductCurrentPlayerScore(100);

        assertEquals(0, game.getPlayerScore(2), "Điểm phải được giữ ở mức 0 khi số điểm bị trừ lớn hơn điểm hiện có");
        assertEquals(10, game.getPlayerScore(1), "Điểm của người chơi khác không được thay đổi");
    }

    private void setCurrentPlayerScore(int score) throws Exception {
        Field playerScoresField = GameController.class.getDeclaredField("playerScores");
        playerScoresField.setAccessible(true);
        int[] playerScores = (int[]) playerScoresField.get(game);
        playerScores[game.getCurrentPlayerNumber() - 1] = score;
    }
}

package minesweeper.TestUC03;

import minesweeper.controller.GameController;
import minesweeper.model.enums.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UC03_2_TC05 {
    private GameController game;

    @BeforeEach
    void setUp() {
        game = new GameController();
        game.startCustomGame(2, 2, 1, 1);
    }

    @Test
    void UC03_TC05_moOAnToanThiCongMuoiDiemChoMoiO() {
        int openedCells = game.reveal(0, 0);

        assertTrue(openedCells > 0, "Mở ô an toàn đầu tiên phải mở được ít nhất một ô");
        assertEquals(openedCells * 10, game.getPlayerScore(1),
                "Điểm người chơi phải tăng 10 điểm cho mỗi ô an toàn vừa được mở");
        assertEquals(GameState.PLAYING, game.getGameState(),
                "Ván chơi phải tiếp tục sau khi mở ô an toàn");
    }

    @Test
    void UC03_TC05_moLaiOCuKhongDuocCongDiemLanNua() {
        int firstOpenedCells = game.reveal(0, 0);
        int scoreAfterFirstReveal = game.getPlayerScore(1);

        int secondOpenedCells = game.reveal(0, 0);

        assertTrue(firstOpenedCells > 0, "Lần mở đầu tiên phải mở được ít nhất một ô");
        assertEquals(0, secondOpenedCells, "Mở lại ô đã mở không được mở thêm ô mới");
        assertEquals(scoreAfterFirstReveal, game.getPlayerScore(1),
                "Điểm không được thay đổi khi không có ô an toàn mới được mở");
    }
}

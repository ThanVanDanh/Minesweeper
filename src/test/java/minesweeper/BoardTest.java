package minesweeper;

import minesweeper.model.Board;
import minesweeper.model.enums.Difficulty;
import minesweeper.model.enums.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    @Test
    void createCustomBoard_WithValidConfig_ShouldSuccess() {
        Board board = new Board(10, 10, 15, 3);

        assertEquals(10, board.getRows());
        assertEquals(10, board.getCols());
        assertEquals(15, board.getTotalMines());
        assertEquals(3, board.getPlayerCount());
        assertEquals(GameState.IDLE, board.getGameState());
        assertEquals(15, board.getRemainingMines());
    }

    @Test
    void createBoard_WithMinPlayerCount_ShouldSuccess() {
        Board board = new Board(9, 9, 10, Board.MIN_PLAYER_COUNT);

        assertEquals(1, board.getPlayerCount());
    }

    @Test
    void createBoard_WithMaxPlayerCount_ShouldSuccess() {
        Board board = new Board(9, 9, 10, Board.MAX_PLAYER_COUNT);

        assertEquals(4, board.getPlayerCount());
    }

    @Test
    void createBoard_WithPlayerCountLessThanMin_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Board(9, 9, 10, 0)
        );

        assertTrue(exception.getMessage().contains("Player count"));
    }

    @Test
    void createBoard_WithPlayerCountGreaterThanMax_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Board(9, 9, 10, 5)
        );

        assertTrue(exception.getMessage().contains("Player count"));
    }

    @Test
    void createBoard_WithRowsLessThanTwo_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Board(1, 9, 10, 1)
        );

        assertTrue(exception.getMessage().contains("at least 2 rows"));
    }

    @Test
    void createBoard_WithColsLessThanTwo_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Board(9, 1, 10, 1)
        );

        assertTrue(exception.getMessage().contains("at least 2"));
    }

    @Test
    void createBoard_WithMineCountZero_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Board(9, 9, 0, 1)
        );

        assertTrue(exception.getMessage().contains("at least 1 mine"));
    }

    @Test
    void createBoard_WithMineCountEqualTotalCells_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Board(2, 2, 4, 1)
        );

        assertTrue(exception.getMessage().contains("Mine count"));
    }

    @Test
    void createBoard_WithDifficultyAndPlayerCount_ShouldUseDifficultyConfig() {
        Board board = new Board(Difficulty.EASY, 2);

        assertEquals(Difficulty.EASY.getRows(), board.getRows());
        assertEquals(Difficulty.EASY.getCols(), board.getCols());
        assertEquals(Difficulty.EASY.getMines(), board.getTotalMines());
        assertEquals(2, board.getPlayerCount());
    }

    @Test
    void placeMines_ShouldChangeGameStateToPlaying() {
        Board board = new Board(9, 9, 10, 1);

        board.placeMines(0, 0);

        assertEquals(GameState.PLAYING, board.getGameState());
    }

    @Test
    void placeMines_ShouldNotPlaceMineAtFirstClickedCell() {
        Board board = new Board(9, 9, 10, 1);

        board.placeMines(0, 0);

        assertFalse(board.getCell(0, 0).isMine());
    }
}
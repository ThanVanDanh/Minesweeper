package minesweeper.controller;

import minesweeper.model.Board;
import minesweeper.model.Difficulty;
import minesweeper.model.GameResult;
import minesweeper.model.GameState;
import minesweeper.repository.GameResultRepository;
import minesweeper.repository.MySqlGameResultRepository;
import minesweeper.service.RankingManager;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Example: How to integrate ranking system with GameController
 *
 * This shows where to add code to save game results when a game ends.
 */
public class GameControllerIntegrationExample {

    private Board board;
    private Difficulty difficulty;
    private long gameStartTimeMs;  // ← ADD THIS (track when game started)
    private String currentPlayerName = "TestPlayer";  // ← From login system

    private GameResultRepository gameResultRepository;
    private RankingManager rankingManager;

    public void initialize() {
        gameResultRepository = new MySqlGameResultRepository();
        rankingManager = new RankingManager(gameResultRepository);
    }

    public void startNewGame(Difficulty difficulty) {
        this.difficulty = difficulty;
        this.board = new Board(difficulty);
        this.gameStartTimeMs = System.currentTimeMillis();  // ← TRACK TIME START
    }

    public void reveal(int row, int col) {
        if (board == null) {
            return;
        }

        board.reveal(row, col);

        // ✅ NEW: Check if game just ended
        if (board.getGameState() == GameState.WON ||
            board.getGameState() == GameState.LOST) {
            onGameEnd();
        }
    }

    public void toggleFlag(int row, int col) {
        if (board == null) {
            return;
        }
        board.toggleFlag(row, col);
    }

    /**
     * ✅ NEW METHOD: Called when game ends (win or loss)
     */
    private void onGameEnd() {
        try {
            // Create game result
            long elapsedTimeMs = System.currentTimeMillis() - gameStartTimeMs;

            GameResult result = new GameResult(
                UUID.randomUUID().toString(),
                currentPlayerName,
                difficulty,
                board.getGameState() == GameState.WON,
                elapsedTimeMs,
                board.getFlagsPlaced(),
                board.getTotalMines(),
                LocalDateTime.now()
            );

            // Save to storage
            gameResultRepository.saveGameResult(result);

            // Update ranking
            int playerRank = rankingManager.getPlayerRankPosition(currentPlayerName);

            System.out.println("Game Result:");
            System.out.println("  Player: " + currentPlayerName);
            System.out.println("  Result: " + (result.isWon() ? "WIN" : "LOSS"));
            System.out.println("  Score: " + result.getScore());
            System.out.println("  Time: " + result.getTimeFormatted());
            System.out.println("  Rank: #" + playerRank);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Getters
    public Board getBoard() { return board; }
    public Difficulty getDifficulty() { return difficulty; }
    public GameState getGameState() {
        return board == null ? GameState.IDLE : board.getGameState();
    }
}


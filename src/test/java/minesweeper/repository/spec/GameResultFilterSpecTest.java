package minesweeper.repository.spec;

import minesweeper.model.enums.Difficulty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case cho GameResultFilterSpec (Hỗ trợ D3 - Sorting)
 */
class GameResultFilterSpecTest {

    @Test
    @DisplayName("Khởi tạo mặc định không có điều kiện sort")
    void defaultConstructor_ShouldHaveNullSort() {
        GameResultFilterSpec spec = new GameResultFilterSpec();
        assertNull(spec.sortBy);
        assertNull(spec.sortDir);
        assertNull(spec.username);
        assertNull(spec.difficulty);
        assertNull(spec.win);
    }

    @Test
    @DisplayName("Kiểm tra factory method withUsername không ảnh hưởng đến sort")
    void withUsername_ShouldNotAffectSort() {
        GameResultFilterSpec spec = GameResultFilterSpec.withUsername("player1");
        assertEquals("player1", spec.username);
        assertNull(spec.sortBy);
        assertNull(spec.sortDir);
    }

    @Test
    @DisplayName("Kiểm tra việc thiết lập thủ công các thuộc tính sort theo Score")
    void setSortByScore_ShouldBeCorrect() {
        GameResultFilterSpec spec = new GameResultFilterSpec();
        spec.sortBy = "score";
        spec.sortDir = "DESC";
        
        assertEquals("score", spec.sortBy);
        assertEquals("DESC", spec.sortDir);
    }

    @Test
    @DisplayName("Kiểm tra việc thiết lập thủ công các thuộc tính sort theo Time")
    void setSortByTime_ShouldBeCorrect() {
        GameResultFilterSpec spec = new GameResultFilterSpec();
        spec.sortBy = "time";
        spec.sortDir = "ASC";
        
        assertEquals("time", spec.sortBy);
        assertEquals("ASC", spec.sortDir);
    }
}

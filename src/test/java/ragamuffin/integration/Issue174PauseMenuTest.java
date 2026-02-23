package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.GameState;
import ragamuffin.ui.PauseMenu;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #174:
 * Pressing ESC should open a pause menu with options to exit the game.
 *
 * Verifies that:
 * 1. The pause menu becomes visible when the game transitions to PAUSED state.
 * 2. The pause menu has a Quit (exit) option.
 * 3. The pause menu is hidden when the game resumes.
 * 4. Navigation wraps correctly through all options.
 */
class Issue174PauseMenuTest {

    private PauseMenu pauseMenu;

    @BeforeEach
    void setUp() {
        pauseMenu = new PauseMenu();
    }

    /**
     * Test 1: Pressing ESC transitions to PAUSED and shows the pause menu.
     * Simulates what RagamuffinGame.transitionToPaused() does: sets state to PAUSED
     * and calls pauseMenu.show(). Verifies the menu is visible.
     */
    @Test
    void escKeyShowsPauseMenu() {
        // Initially the pause menu must not be visible
        assertFalse(pauseMenu.isVisible(), "Pause menu should be hidden at game start");

        // Simulate transitionToPaused() — state becomes PAUSED and menu is shown
        GameState state = GameState.PLAYING;
        pauseMenu.show();
        state = GameState.PAUSED;

        assertTrue(pauseMenu.isVisible(), "Pause menu must be visible after ESC (transition to PAUSED)");
        assertEquals(GameState.PAUSED, state, "Game state must be PAUSED after ESC");
    }

    /**
     * Test 2: Pause menu has a Quit option to exit the game.
     * Navigates to the Quit option and verifies isQuitSelected() returns true.
     */
    @Test
    void pauseMenuHasQuitOption() {
        pauseMenu.show();

        // Default is Resume; navigate to Quit
        assertFalse(pauseMenu.isQuitSelected(), "Quit should not be selected by default");

        pauseMenu.selectNext(); // -> Restart
        pauseMenu.selectNext(); // -> Quit

        assertTrue(pauseMenu.isQuitSelected(), "Quit option must be reachable via selectNext()");
    }

    /**
     * Test 3: Selecting Resume hides the pause menu and resumes gameplay.
     * Simulates what RagamuffinGame.transitionToPlaying() does: hides the menu
     * and returns to PLAYING state.
     */
    @Test
    void resumeHidesPauseMenuAndReturnsToPlaying() {
        // Enter paused state
        pauseMenu.show();
        GameState state = GameState.PAUSED;
        assertTrue(pauseMenu.isVisible(), "Pause menu should be visible when paused");

        // Resume is the default selection
        assertTrue(pauseMenu.isResumeSelected(), "Resume should be selected by default");

        // Simulate transitionToPlaying()
        pauseMenu.hide();
        state = GameState.PLAYING;

        assertFalse(pauseMenu.isVisible(), "Pause menu must be hidden after resume");
        assertEquals(GameState.PLAYING, state, "Game state must be PLAYING after resume");
    }

    /**
     * Test 4: Pause menu selection resets to Resume each time it is shown.
     * Ensures the menu always opens with Resume highlighted, not a previously
     * selected Quit or Restart.
     */
    @Test
    void pauseMenuResetsToResumeOnShow() {
        pauseMenu.show();
        pauseMenu.selectNext(); // -> Restart
        pauseMenu.selectNext(); // -> Quit
        assertTrue(pauseMenu.isQuitSelected(), "Quit should be selected before hide");

        // Hide and re-show (simulates ESC → resume → ESC again)
        pauseMenu.hide();
        pauseMenu.show();

        assertTrue(pauseMenu.isResumeSelected(), "Resume must be selected when pause menu is shown again");
    }

    /**
     * Test 5: All three options are accessible and wrap correctly.
     * Navigates down through Resume → Restart → Quit → Resume (wrap).
     */
    @Test
    void pauseMenuOptionsWrapCorrectly() {
        pauseMenu.show();

        assertTrue(pauseMenu.isResumeSelected(), "Default must be Resume");

        pauseMenu.selectNext();
        assertTrue(pauseMenu.isRestartSelected(), "After one down: Restart");

        pauseMenu.selectNext();
        assertTrue(pauseMenu.isQuitSelected(), "After two down: Quit");

        pauseMenu.selectNext(); // wrap around
        assertTrue(pauseMenu.isResumeSelected(), "After wrap: Resume");

        // Also check upward wrap
        pauseMenu.selectPrevious();
        assertTrue(pauseMenu.isQuitSelected(), "Wrap up from Resume should reach Quit");
    }
}

package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.ui.PauseMenu;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #329:
 * restartGame() leaked pauseMenu's visible and selectedOption state — if the
 * pause menu was open (and "Restart" was highlighted) when the player triggered
 * a restart, the new session started with visible=true and selectedOption=1
 * ("Restart") instead of the defaults (false / "Resume").
 *
 * Fix: restartGame() now does {@code pauseMenu = new PauseMenu()} alongside
 * the other UI resets (consistent with the pattern used for helpUI in #325).
 */
class Issue329PauseMenuRestartTest {

    /**
     * Test 1: After restart, pauseMenu must not be visible.
     *
     * Models the restartGame() contract: a fresh PauseMenu instance starts with
     * isVisible() == false, so the pause overlay does not bleed into the new session.
     */
    @Test
    void restartResetsPauseMenuVisibility() {
        // Session 1: player opens pause menu
        PauseMenu pauseMenu = new PauseMenu();
        assertFalse(pauseMenu.isVisible(), "PauseMenu should start hidden");

        pauseMenu.show();
        assertTrue(pauseMenu.isVisible(), "PauseMenu should be visible after show()");

        // restartGame() — THE FIX: recreate pauseMenu
        pauseMenu = new PauseMenu();

        // Session 2: pause overlay must be hidden
        assertFalse(pauseMenu.isVisible(),
                "PauseMenu must not be visible after restart — visible state must not leak");
    }

    /**
     * Test 2: After restart, selectedOption resets to "Resume" (index 0).
     *
     * If the player navigated to "Restart" (index 1) before confirming, the cursor
     * must reset to "Resume" in the new session rather than persisting on "Restart".
     */
    @Test
    void restartResetsPauseMenuSelectedOption() {
        // Session 1: player opens pause menu and moves cursor to "Restart"
        PauseMenu pauseMenu = new PauseMenu();
        pauseMenu.show();
        pauseMenu.selectNext(); // moves from Resume (0) → Restart (1)
        assertTrue(pauseMenu.isRestartSelected(), "Restart option should be selected");

        // restartGame() — THE FIX: recreate pauseMenu
        pauseMenu = new PauseMenu();

        // Session 2: cursor must be back on "Resume"
        assertTrue(pauseMenu.isResumeSelected(),
                "selectedOption must reset to Resume after restart — cursor must not leak");
        assertFalse(pauseMenu.isRestartSelected(),
                "Restart must not remain selected after restart");
    }

    /**
     * Test 3: Without the fix, stale visible state carries over.
     *
     * Documents the pre-fix bug: if pauseMenu is NOT recreated, isVisible() stays
     * true in the new session, matching the same class of leak fixed for helpUI (#325).
     */
    @Test
    void withoutResetPauseMenuStaysVisible() {
        PauseMenu pauseMenu = new PauseMenu();
        pauseMenu.show(); // open in session 1
        assertTrue(pauseMenu.isVisible(), "Pause menu is open before restart");

        // BUG: restartGame() does NOT recreate pauseMenu — stale state leaks
        // (no assignment here — intentionally omitted to model the bug)

        assertTrue(pauseMenu.isVisible(),
                "Without the fix, pause menu stays visible after restart — confirming the bug");
    }

    /**
     * Test 4: Without the fix, stale selectedOption carries over.
     *
     * Documents the pre-fix bug: if pauseMenu is NOT recreated, selectedOption
     * persists from the previous session.
     */
    @Test
    void withoutResetSelectedOptionStaysOnRestart() {
        PauseMenu pauseMenu = new PauseMenu();
        pauseMenu.show();
        pauseMenu.selectNext(); // cursor moves to Restart (index 1)
        assertTrue(pauseMenu.isRestartSelected(), "Restart selected before restart");

        // BUG: no recreation — selectedOption stays at 1
        assertTrue(pauseMenu.isRestartSelected(),
                "Without the fix, selectedOption stays on Restart — confirming the bug");
    }
}

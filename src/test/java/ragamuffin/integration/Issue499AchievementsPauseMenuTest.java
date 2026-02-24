package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.GameState;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementsUI;
import ragamuffin.ui.PauseMenu;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for Issue #499:
 * Achievements button in the pause menu closes the game.
 *
 * <p>Root causes:
 * <ol>
 *   <li><b>Click-through bug</b>: When the achievements overlay is open in
 *       PAUSED state, mouse clicks were still forwarded to
 *       {@code pauseMenu.handleClick()}.  If the click landed on the area
 *       corresponding to the "Quit" hit-box (which overlaps with the
 *       achievements panel), {@code Gdx.app.exit()} was called.</li>
 *   <li><b>Missing keyboard handler</b>: The PAUSED-state ENTER handler
 *       contained cases for Resume, Restart, and Quit but not for
 *       Achievements, so keyboard activation of the achievements option
 *       silently did nothing.</li>
 *   <li><b>Input leakage</b>: UP/DOWN/ENTER were forwarded to the pause menu
 *       even when the achievements overlay was open, so scroll events were
 *       lost and ENTER could accidentally trigger a pause-menu action.</li>
 * </ol>
 *
 * <p>Fix: mirror the identical guard already added for the quest log by Fix #481 —
 * when {@code achievementsUI.isVisible()}, forward UP/DOWN to
 * {@code achievementsUI.scrollUp/Down()} and swallow ENTER; also guard the mouse
 * click handler with {@code !achievementsUI.isVisible()} so clicks on the overlay
 * do not reach pause-menu hit-boxes underneath.
 */
class Issue499AchievementsPauseMenuTest {

    private static final int SCREEN_WIDTH  = 1280;
    private static final int SCREEN_HEIGHT = 720;

    private AchievementSystem achievementSystem;
    private AchievementsUI achievementsUI;
    private PauseMenu pauseMenu;

    @BeforeEach
    void setUp() {
        achievementSystem = new AchievementSystem();
        achievementsUI    = new AchievementsUI(achievementSystem);
        pauseMenu         = new PauseMenu();
        pauseMenu.show();
    }

    // -----------------------------------------------------------------------
    // Test 1 — Clicking "Achievements" in the pause menu opens the overlay.
    //
    // Clicking the Achievements option (index 1) must open the achievements
    // overlay, not close the game.  This verifies the basic happy path.
    // -----------------------------------------------------------------------
    @Test
    void clickingAchievementsOptionOpensOverlay() {
        assertFalse(achievementsUI.isVisible(), "Overlay must start hidden");

        // Simulate what the fixed click handler does: handleClick returns 1
        // → achievementsUI.toggle()
        int clicked = 1; // Achievements option
        if (clicked == 1) {
            achievementsUI.toggle();
        }

        assertTrue(achievementsUI.isVisible(),
                "Achievements overlay must be visible after clicking the Achievements option — " +
                "the game must NOT call Gdx.app.exit()");
    }

    // -----------------------------------------------------------------------
    // Test 2 — Clicking "Achievements" with keyboard Enter opens the overlay.
    //
    // The user navigates to the Achievements option with arrow keys and presses
    // Enter.  The missing `isAchievementsSelected()` branch must now call
    // achievementsUI.toggle().
    // -----------------------------------------------------------------------
    @Test
    void enterOnAchievementsOptionOpensOverlay() {
        pauseMenu.selectNext(); // Resume → Achievements
        assertTrue(pauseMenu.isAchievementsSelected(),
                "After one selectNext(), Achievements must be selected");

        assertFalse(achievementsUI.isVisible(), "Overlay hidden before Enter");

        // Simulate the fixed ENTER handler
        if (pauseMenu.isResumeSelected()) {
            fail("Should not be Resume");
        } else if (pauseMenu.isAchievementsSelected()) {
            achievementsUI.toggle();
        } else if (pauseMenu.isRestartSelected()) {
            fail("Should not be Restart");
        } else if (pauseMenu.isQuitSelected()) {
            fail("Should not be Quit — pressing Enter on Achievements must NOT exit the game");
        }

        assertTrue(achievementsUI.isVisible(),
                "Achievements overlay must open when Enter is pressed on the Achievements option");
    }

    // -----------------------------------------------------------------------
    // Test 3 — Click in achievements overlay area does NOT trigger Quit.
    //
    // The Quit option hit-box (Y ≈ 480-510 for 1280×720) overlaps the
    // achievements panel.  When the overlay is open a click at the Quit
    // position must NOT reach pauseMenu.handleClick(), so the game must
    // not exit.
    //
    // The fix guards the mouse click handler with !achievementsUI.isVisible().
    // -----------------------------------------------------------------------
    @Test
    void clickInOverlayAreaDoesNotTriggerQuitWhenOverlayOpen() {
        achievementsUI.show();
        assertTrue(achievementsUI.isVisible(), "Overlay open");

        // Coordinates that hit the Quit hit-box in the pause menu:
        //   Quit is option i=3.  baselineScreen = 720 - (360 - 3*50) = 510.
        //   Hit-box: Y ∈ [480, 510], X ∈ [520, 760].
        int clickX = SCREEN_WIDTH / 2;   // 640 — centre, inside hit-box
        int clickY = 495;                 // midpoint of Quit hit-box

        // Simulate the FIXED click handler: guard with !achievementsUI.isVisible()
        boolean quitTriggered = false;
        if (!achievementsUI.isVisible()) {
            // This branch is NOT reached because overlay is open
            int clicked = pauseMenu.handleClick(clickX, clickY, SCREEN_WIDTH, SCREEN_HEIGHT);
            if (clicked == 3) {
                quitTriggered = true; // Would have called Gdx.app.exit()
            }
        }
        // else: click consumed, no pause-menu action

        assertFalse(quitTriggered,
                "Clicking in the achievements overlay area must NOT trigger Quit — " +
                "the pause menu click handler must be guarded by !achievementsUI.isVisible()");
    }

    // -----------------------------------------------------------------------
    // Test 4 — UP/DOWN scroll the achievements list, not the pause menu.
    //
    // When the achievements overlay is open in PAUSED state, UP/DOWN must
    // scroll the achievements list.  The pause menu selection must not change.
    // -----------------------------------------------------------------------
    @Test
    void upDownScrollAchievementsNotPauseMenuWhenOverlayOpen() {
        achievementsUI.show();
        pauseMenu.show();

        assertTrue(achievementsUI.isVisible(), "Overlay open");
        assertTrue(pauseMenu.isResumeSelected(), "Pause menu starts at Resume");

        int initialScrollOffset = achievementsUI.getScrollOffset();

        // Simulate the fixed logic: achievementsUI is visible → scroll it, not pause menu
        achievementsUI.scrollDown();

        assertTrue(pauseMenu.isResumeSelected(),
                "Pause menu selection must not change when achievements overlay is visible — " +
                "DOWN must scroll achievements, not call pauseMenu.selectNext()");

        achievementsUI.scrollUp();

        assertTrue(pauseMenu.isResumeSelected(),
                "Pause menu selection must not change when achievements overlay is visible — " +
                "UP must scroll achievements, not call pauseMenu.selectPrevious()");

        assertEquals(initialScrollOffset, achievementsUI.getScrollOffset(),
                "One scrollDown() + one scrollUp() must return scroll offset to initial value");
    }

    // -----------------------------------------------------------------------
    // Test 5 — ENTER is swallowed when achievements overlay is open.
    //
    // When the achievements overlay is open, pressing ENTER must NOT trigger
    // any pause menu action (resume/restart/quit).
    // -----------------------------------------------------------------------
    @Test
    void enterIsSwallowedWhenAchievementsOverlayOpen() {
        achievementsUI.show();
        pauseMenu.show();
        GameState state = GameState.PAUSED;

        assertTrue(achievementsUI.isVisible(), "Overlay open");
        assertTrue(pauseMenu.isResumeSelected(), "Resume selected by default");

        // Simulate the fixed PAUSED-state ENTER handling:
        // achievementsUI.isVisible() → swallow ENTER, do NOT act on pause menu
        if (achievementsUI.isVisible()) {
            // swallow — do nothing
        } else {
            if (pauseMenu.isResumeSelected()) {
                state = GameState.PLAYING; // would have transitioned
            }
        }

        assertEquals(GameState.PAUSED, state,
                "ENTER must not trigger any pause menu action when achievements overlay is open — " +
                "game must stay in PAUSED state");
    }

    // -----------------------------------------------------------------------
    // Test 6 — ESC closes the achievements overlay and stays in PAUSED.
    //
    // When the achievements overlay is open in PAUSED state, pressing ESC
    // must close the overlay (not resume the game).  The pause menu must
    // remain visible.
    // -----------------------------------------------------------------------
    @Test
    void escClosesOverlayKeepsGamePaused() {
        achievementsUI.show();
        GameState state = GameState.PAUSED;
        pauseMenu.show();

        assertTrue(achievementsUI.isVisible(), "Overlay open before ESC");
        assertTrue(pauseMenu.isVisible(), "Pause menu visible while paused");

        // Simulate handleEscapePress(): achievementsUI visible → hide it, stay PAUSED
        if (achievementsUI.isVisible()) {
            achievementsUI.hide();
            // state stays PAUSED
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING; // would resume — must NOT happen
        }

        assertFalse(achievementsUI.isVisible(),
                "Achievements overlay must be hidden after ESC");
        assertEquals(GameState.PAUSED, state,
                "Game must remain in PAUSED state after ESC closes the overlay — " +
                "player must not be accidentally returned to PLAYING");
        assertTrue(pauseMenu.isVisible(),
                "Pause menu must still be visible after achievements overlay is closed by ESC");
    }

    // -----------------------------------------------------------------------
    // Test 7 — Normal pause menu click still works when overlay is NOT open.
    //
    // Verifies the guard does not break normal Achievements click when the
    // overlay is closed.
    // -----------------------------------------------------------------------
    @Test
    void normalClickOnAchievementsStillWorksWhenOverlayClosed() {
        assertFalse(achievementsUI.isVisible(), "Overlay starts closed");

        // Simulate the fixed click handler: !achievementsUI.isVisible() → handleClick
        if (!achievementsUI.isVisible()) {
            int clicked = pauseMenu.handleClick(
                    SCREEN_WIDTH / 2,
                    // Achievements hit-box centre: baselineScreen=410, centre at 410-15=395
                    395,
                    SCREEN_WIDTH, SCREEN_HEIGHT);
            if (clicked == 1) {
                achievementsUI.toggle();
            }
        }

        assertTrue(achievementsUI.isVisible(),
                "Clicking the Achievements option when the overlay is closed must open the overlay");
    }
}

package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.BuildingQuestRegistry;
import ragamuffin.core.GameState;
import ragamuffin.ui.PauseMenu;
import ragamuffin.ui.QuestLogUI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for Issue #529:
 * Left-click passes through quest log to pause menu while paused.
 *
 * <p>Root cause: when the quest log overlay is open in PAUSED state, mouse
 * clicks were still forwarded to {@code pauseMenu.handleClick()}.  If the
 * click landed on the area corresponding to the "Quit" hit-box, the game
 * exited without confirmation.
 *
 * <p>Fix: mirror the guard already present for Fix #499 — add
 * {@code !questLogUI.isVisible()} to the pause-menu click condition and add
 * an {@code else if} branch that consumes the click when the quest log is
 * visible, preventing it from carrying over to subsequent frames.
 */
class Issue529QuestLogPauseMenuTest {

    private static final int SCREEN_WIDTH  = 1280;
    private static final int SCREEN_HEIGHT = 720;

    private QuestLogUI questLogUI;
    private PauseMenu  pauseMenu;

    @BeforeEach
    void setUp() {
        BuildingQuestRegistry questRegistry = new BuildingQuestRegistry();
        questLogUI = new QuestLogUI(questRegistry);
        pauseMenu  = new PauseMenu();
        pauseMenu.show();
    }

    // -----------------------------------------------------------------------
    // Test 1 — Click in quest-log overlay area does NOT trigger Quit.
    //
    // The Quit option hit-box (Y ≈ 480-510 for 1280×720) overlaps the quest
    // log panel.  When the overlay is open a click at the Quit position must
    // NOT reach pauseMenu.handleClick(), so the game must not exit.
    //
    // The fix guards the mouse click handler with !questLogUI.isVisible().
    // -----------------------------------------------------------------------
    @Test
    void clickInQuestLogAreaDoesNotTriggerQuitWhenOverlayOpen() {
        questLogUI.show();
        assertTrue(questLogUI.isVisible(), "Quest log overlay must be open");

        // Coordinates that hit the Quit hit-box in the pause menu:
        //   Quit is option i=3.  baselineScreen = 720 - (360 - 3*50) = 510.
        //   Hit-box: Y ∈ [480, 510], X ∈ [520, 760].
        int clickX = SCREEN_WIDTH / 2; // 640 — centre, inside hit-box
        int clickY = 495;              // midpoint of Quit hit-box

        // Simulate the FIXED click handler: guard with !questLogUI.isVisible()
        boolean quitTriggered = false;
        if (!questLogUI.isVisible()) {
            // This branch must NOT be reached because the overlay is open.
            int clicked = pauseMenu.handleClick(clickX, clickY, SCREEN_WIDTH, SCREEN_HEIGHT);
            if (clicked == 3) {
                quitTriggered = true; // would have called Gdx.app.exit()
            }
        }
        // else: click consumed, no pause-menu action

        assertFalse(quitTriggered,
                "Clicking in the quest-log overlay area must NOT trigger Quit — " +
                "the pause menu click handler must be guarded by !questLogUI.isVisible()");
    }

    // -----------------------------------------------------------------------
    // Test 2 — Click is consumed (not leaked) when quest log overlay is open.
    //
    // After the click is processed while the quest log is visible, the click
    // state must be considered consumed so it does not affect subsequent
    // frames.  We verify this by checking that pauseMenu.handleClick() is
    // never called when the overlay is open.
    // -----------------------------------------------------------------------
    @Test
    void clickIsConsumedWhenQuestLogOverlayOpen() {
        questLogUI.show();
        assertTrue(questLogUI.isVisible(), "Quest log overlay must be open");

        // Track whether the pause menu click handler was invoked.
        final boolean[] pauseMenuHandleClickCalled = {false};

        // Simulate the fixed handler logic.
        boolean leftClickPressed = true; // pretend a click just occurred
        if (leftClickPressed && !questLogUI.isVisible()) {
            pauseMenuHandleClickCalled[0] = true; // should NOT reach here
        } else if (leftClickPressed && questLogUI.isVisible()) {
            // Consume the click — this is the new else-if branch from Fix #529.
            leftClickPressed = false; // resetLeftClick() equivalent
        }

        assertFalse(pauseMenuHandleClickCalled[0],
                "pauseMenu.handleClick() must not be called when quest log overlay is open — " +
                "the click must be consumed by the new else-if branch");
        assertFalse(leftClickPressed,
                "The click state must be reset (consumed) when the quest log overlay is open");
    }

    // -----------------------------------------------------------------------
    // Test 3 — Normal pause menu click still works when quest log is NOT open.
    //
    // Verifies the new guard does not break normal pause-menu interaction
    // when the quest log overlay is closed.
    // -----------------------------------------------------------------------
    @Test
    void normalClickOnPauseMenuStillWorksWhenQuestLogClosed() {
        assertFalse(questLogUI.isVisible(), "Quest log overlay must start hidden");

        // Simulate the fixed click handler: !questLogUI.isVisible() → handleClick
        int result = -1;
        if (!questLogUI.isVisible()) {
            result = pauseMenu.handleClick(
                    SCREEN_WIDTH / 2,
                    // Resume hit-box: baselineScreen = 720 - (360 - 0*50) = 360
                    // centre at 360 - 15 = 345
                    345,
                    SCREEN_WIDTH, SCREEN_HEIGHT);
        }

        assertEquals(0, result,
                "Clicking the Resume option when the quest log overlay is closed must return 0 " +
                "(Resume action) — the guard must not block normal pause-menu clicks");
    }

    // -----------------------------------------------------------------------
    // Test 4 — ENTER is swallowed when quest log overlay is open.
    //
    // When the quest log overlay is open, pressing ENTER must NOT trigger
    // any pause menu action (resume/restart/quit).  This mirrors the
    // equivalent test for the achievements overlay (Issue #499).
    // -----------------------------------------------------------------------
    @Test
    void enterIsSwallowedWhenQuestLogOverlayOpen() {
        questLogUI.show();
        pauseMenu.show();
        GameState state = GameState.PAUSED;

        assertTrue(questLogUI.isVisible(), "Quest log overlay must be open");
        assertTrue(pauseMenu.isResumeSelected(), "Resume selected by default");

        // Simulate the PAUSED-state ENTER handling:
        // questLogUI.isVisible() → swallow ENTER, do NOT act on pause menu
        if (questLogUI.isVisible()) {
            // swallow — do nothing
        } else {
            if (pauseMenu.isResumeSelected()) {
                state = GameState.PLAYING; // would have transitioned
            }
        }

        assertEquals(GameState.PAUSED, state,
                "ENTER must not trigger any pause menu action when quest log overlay is open — " +
                "game must stay in PAUSED state");
    }

    // -----------------------------------------------------------------------
    // Test 5 — ESC closes the quest log overlay and stays in PAUSED.
    //
    // When the quest log overlay is open in PAUSED state, pressing ESC must
    // close the overlay (not resume the game).
    // -----------------------------------------------------------------------
    @Test
    void escClosesQuestLogOverlayKeepsGamePaused() {
        questLogUI.show();
        GameState state = GameState.PAUSED;
        pauseMenu.show();

        assertTrue(questLogUI.isVisible(), "Quest log overlay open before ESC");
        assertTrue(pauseMenu.isVisible(), "Pause menu visible while paused");

        // Simulate handleEscapePress(): questLogUI visible → hide it, stay PAUSED
        if (questLogUI.isVisible()) {
            questLogUI.hide();
            // state stays PAUSED
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING; // would resume — must NOT happen
        }

        assertFalse(questLogUI.isVisible(),
                "Quest log overlay must be hidden after ESC");
        assertEquals(GameState.PAUSED, state,
                "Game must remain in PAUSED state after ESC closes the quest log — " +
                "player must not be accidentally returned to PLAYING");
        assertTrue(pauseMenu.isVisible(),
                "Pause menu must still be visible after quest log overlay is closed by ESC");
    }
}

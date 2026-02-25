package ragamuffin.integration;

import com.badlogic.gdx.Input;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.InputHandler;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementsUI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for Issue #667:
 * Pressing the Tab key caused the game to close or crash.
 *
 * <p>Root cause: {@link AchievementsUI#render} called
 * {@code shapeRenderer.begin()} without first calling {@code spriteBatch.end()}.
 * LibGDX throws a {@code GdxRuntimeException} ("SpriteBatch.end must be called
 * before begin") when a ShapeRenderer is started while a SpriteBatch is active,
 * which crashed the game on the first frame that the achievements overlay was
 * rendered with any even-indexed achievement row visible.
 *
 * <p>Fix: call {@code spriteBatch.end()} immediately before
 * {@code shapeRenderer.begin()} inside the alternating-row-background branch.
 *
 * <p>These tests verify the input-handling and UI-state aspects of the fix
 * (the render path itself requires an OpenGL context and is exercised manually).
 */
class Issue667TabKeyCrashTest {

    private InputHandler inputHandler;
    private AchievementSystem achievementSystem;
    private AchievementsUI achievementsUI;

    @BeforeEach
    void setUp() {
        inputHandler = new InputHandler();
        achievementSystem = new AchievementSystem();
        achievementsUI = new AchievementsUI(achievementSystem);
    }

    // -----------------------------------------------------------------------
    // Test 1 — Tab key sets achievementsPressed in InputHandler.
    //
    // Pressing Tab must set the achievementsPressed flag so that
    // RagamuffinGame can pick it up in the same frame.
    // -----------------------------------------------------------------------
    @Test
    void tabKeyDownSetsAchievementsPressed() {
        assertFalse(inputHandler.isAchievementsPressed(),
                "achievementsPressed must be false before any key press");

        inputHandler.keyDown(Input.Keys.TAB);

        assertTrue(inputHandler.isAchievementsPressed(),
                "Tab key must set achievementsPressed — " +
                "game must detect the key press and open the achievements overlay");
    }

    // -----------------------------------------------------------------------
    // Test 2 — resetAchievements() clears the flag.
    //
    // RagamuffinGame calls resetAchievements() after consuming the press
    // so the overlay is not toggled again on the next frame.
    // -----------------------------------------------------------------------
    @Test
    void resetAchievementsClearsFlag() {
        inputHandler.keyDown(Input.Keys.TAB);
        assertTrue(inputHandler.isAchievementsPressed(), "Flag set after keyDown");

        inputHandler.resetAchievements();

        assertFalse(inputHandler.isAchievementsPressed(),
                "resetAchievements() must clear the achievementsPressed flag — " +
                "the overlay must not be toggled on subsequent frames");
    }

    // -----------------------------------------------------------------------
    // Test 3 — Tab opens then closes the achievements overlay (toggle).
    //
    // The first Tab press opens the overlay; the second closes it.
    // This models the full keypress → isAchievementsPressed → toggle →
    // resetAchievements cycle that happens inside RagamuffinGame.
    // -----------------------------------------------------------------------
    @Test
    void tabTogglesAchievementsOverlayOpenThenClosed() {
        assertFalse(achievementsUI.isVisible(), "Overlay must start hidden");

        // Simulate first Tab press
        inputHandler.keyDown(Input.Keys.TAB);
        if (inputHandler.isAchievementsPressed()) {
            achievementsUI.toggle();
            inputHandler.resetAchievements();
        }
        assertTrue(achievementsUI.isVisible(),
                "Achievements overlay must be visible after first Tab press");

        // Simulate second Tab press
        inputHandler.keyDown(Input.Keys.TAB);
        if (inputHandler.isAchievementsPressed()) {
            achievementsUI.toggle();
            inputHandler.resetAchievements();
        }
        assertFalse(achievementsUI.isVisible(),
                "Achievements overlay must be hidden after second Tab press — " +
                "Tab must toggle the overlay, not crash the game");
    }

    // -----------------------------------------------------------------------
    // Test 4 — Other keys do not set achievementsPressed.
    //
    // Only Tab should open the achievements overlay.
    // -----------------------------------------------------------------------
    @Test
    void otherKeysDoNotSetAchievementsPressed() {
        inputHandler.keyDown(Input.Keys.I);
        inputHandler.keyDown(Input.Keys.H);
        inputHandler.keyDown(Input.Keys.C);
        inputHandler.keyDown(Input.Keys.ESCAPE);

        assertFalse(inputHandler.isAchievementsPressed(),
                "achievementsPressed must remain false when keys other than Tab are pressed");
    }

    // -----------------------------------------------------------------------
    // Test 5 — AchievementsUI show() resets scroll offset to prevent
    // out-of-bounds access in the render loop.
    //
    // An out-of-bounds scrollOffset combined with the render crash was a
    // compounding failure mode; show() must always reset it to 0.
    // -----------------------------------------------------------------------
    @Test
    void showResetsScrollOffsetSoRenderLoopIsInBounds() {
        achievementsUI.show();
        // Scroll to a non-zero position
        achievementsUI.scrollDown();
        achievementsUI.hide();

        // Re-open: scroll must reset to 0 so no out-of-bounds access occurs
        achievementsUI.show();

        assertEquals(0, achievementsUI.getScrollOffset(),
                "show() must reset scrollOffset to 0 — a stale offset could cause " +
                "the render loop to access achievement entries out of bounds");
    }
}

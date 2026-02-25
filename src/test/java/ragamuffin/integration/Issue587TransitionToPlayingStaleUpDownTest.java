package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.InputHandler;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #587:
 * transitionToPlaying() does not reset upPressed or downPressed,
 * causing stale arrow-key input on first PLAYING frame after resume.
 *
 * <p>The fix adds the following calls to transitionToPlaying():
 * <ul>
 *   <li>{@code inputHandler.resetUp()} — clears stale UP arrow flag</li>
 *   <li>{@code inputHandler.resetDown()} — clears stale DOWN arrow flag</li>
 * </ul>
 *
 * <p>Tests mirror the pattern established by fixes #583 (transitionToPaused()),
 * which already resets both flags.
 */
class Issue587TransitionToPlayingStaleUpDownTest {

    private InputHandler inputHandler;

    @BeforeEach
    void setUp() {
        inputHandler = new InputHandler();
    }

    /**
     * Test 1: resetUp() clears stale upPressed set by the UP arrow key
     * pressed on the same frame as the ESC/resume transition.
     *
     * Without the fix, upPressed == true on the first PLAYING frame causes
     * achievementsUI or questLogUI to fire a spurious scroll event if either
     * overlay becomes visible on that frame.
     */
    @Test
    void transitionToPlaying_resetsUpPressed() {
        // Simulate pressing UP arrow (sets upPressed = true)
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.UP);

        assertTrue(inputHandler.isUpPressed(),
            "Precondition: pressing UP must set upPressed to true");

        // Simulate what transitionToPlaying() now does: resetUp()
        inputHandler.resetUp();

        assertFalse(inputHandler.isUpPressed(),
            "After transitionToPlaying(), isUpPressed() must be false — " +
            "stale UP key from the same-frame press must not fire a spurious " +
            "scroll event on the first PLAYING frame after resume");
    }

    /**
     * Test 2: resetUp() is idempotent — calling it when upPressed is already
     * false must not throw or change any state.
     */
    @Test
    void resetUp_isIdempotent() {
        assertFalse(inputHandler.isUpPressed(),
            "Precondition: upPressed must be false on fresh InputHandler");

        inputHandler.resetUp();
        inputHandler.resetUp();

        assertFalse(inputHandler.isUpPressed(),
            "upPressed must remain false after repeated resetUp() calls");
    }

    /**
     * Test 3: resetDown() clears stale downPressed set by the DOWN arrow key
     * pressed on the same frame as the ESC/resume transition.
     *
     * Without the fix, downPressed == true on the first PLAYING frame causes
     * achievementsUI or questLogUI to fire a spurious scroll event if either
     * overlay becomes visible on that frame.
     */
    @Test
    void transitionToPlaying_resetsDownPressed() {
        // Simulate pressing DOWN arrow (sets downPressed = true)
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.DOWN);

        assertTrue(inputHandler.isDownPressed(),
            "Precondition: pressing DOWN must set downPressed to true");

        // Simulate what transitionToPlaying() now does: resetDown()
        inputHandler.resetDown();

        assertFalse(inputHandler.isDownPressed(),
            "After transitionToPlaying(), isDownPressed() must be false — " +
            "stale DOWN key from the same-frame press must not fire a spurious " +
            "scroll event on the first PLAYING frame after resume");
    }

    /**
     * Test 4: resetDown() is idempotent — calling it when downPressed is
     * already false must not throw or change any state.
     */
    @Test
    void resetDown_isIdempotent() {
        assertFalse(inputHandler.isDownPressed(),
            "Precondition: downPressed must be false on fresh InputHandler");

        inputHandler.resetDown();
        inputHandler.resetDown();

        assertFalse(inputHandler.isDownPressed(),
            "downPressed must remain false after repeated resetDown() calls");
    }

    /**
     * Test 5: Both resets are independent — resetting one does not affect the other.
     */
    @Test
    void transitionToPlaying_resetsAreIndependent() {
        // Set both upPressed and downPressed via keyDown
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.UP);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.DOWN);

        assertTrue(inputHandler.isUpPressed(), "UP pressed");
        assertTrue(inputHandler.isDownPressed(), "DOWN pressed");

        // Reset only up — down must be unaffected
        inputHandler.resetUp();

        assertFalse(inputHandler.isUpPressed(),
            "upPressed must be false after resetUp()");
        assertTrue(inputHandler.isDownPressed(),
            "downPressed must be unaffected by resetUp()");

        // Reset down — both now cleared
        inputHandler.resetDown();

        assertFalse(inputHandler.isDownPressed(),
            "downPressed must be false after resetDown()");
        assertFalse(inputHandler.isUpPressed(),
            "upPressed must still be false after resetDown()");
    }
}

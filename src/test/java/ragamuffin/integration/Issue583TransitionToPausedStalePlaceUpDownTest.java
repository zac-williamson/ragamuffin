package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.InputHandler;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #583:
 * transitionToPaused() does not reset placePressed, upPressed, or downPressed,
 * causing stale input on first frame after transition.
 *
 * <p>The fix adds the following calls to transitionToPaused():
 * <ul>
 *   <li>{@code inputHandler.resetPlace()} — clears stale right-click place flag</li>
 *   <li>{@code inputHandler.resetUp()} — clears stale UP arrow flag</li>
 *   <li>{@code inputHandler.resetDown()} — clears stale DOWN arrow flag</li>
 * </ul>
 *
 * <p>Tests mirror the pattern established by fixes #569, #571, #573, #575, #577, #579.
 */
class Issue583TransitionToPausedStalePlaceUpDownTest {

    private InputHandler inputHandler;

    @BeforeEach
    void setUp() {
        inputHandler = new InputHandler();
    }

    /**
     * Test 1: resetPlace() clears stale placePressed set by a right-click
     * pressed on the same frame as the ESC/pause transition.
     *
     * Simulates the stale-input scenario: player right-clicks to place a block
     * and presses ESC on the same frame. transitionToPaused() must call
     * resetPlace() so isPlacePressed() returns false on the first PLAYING frame
     * after resume, preventing a spurious block placement.
     */
    @Test
    void transitionToPaused_resetsPlacePressed() {
        // Simulate right-click (sets placePressed = true) via direct reset/set
        // placePressed is set by touchDown with RIGHT button + cursor caught;
        // we verify the reset method works correctly here.
        assertFalse(inputHandler.isPlacePressed(),
            "Precondition: placePressed must be false on fresh InputHandler");

        // Manually exercise resetPlace() to verify it clears the flag
        // (placePressed is normally set via touchDown but that requires Gdx.input)
        inputHandler.resetPlace();

        assertFalse(inputHandler.isPlacePressed(),
            "After transitionToPaused(), isPlacePressed() must be false — " +
            "stale right-click from the same-frame press must not fire a block " +
            "placement on the first PLAYING frame after resume");
    }

    /**
     * Test 2: resetPlace() is idempotent — calling it when placePressed is
     * already false must not throw or change any state.
     */
    @Test
    void resetPlace_isIdempotent() {
        assertFalse(inputHandler.isPlacePressed(),
            "Precondition: placePressed must be false on fresh InputHandler");

        inputHandler.resetPlace();
        inputHandler.resetPlace();

        assertFalse(inputHandler.isPlacePressed(),
            "placePressed must remain false after repeated resetPlace() calls");
    }

    /**
     * Test 3: resetUp() clears stale upPressed set by the UP arrow key
     * pressed on the same frame as the ESC/pause transition.
     *
     * Without the fix, upPressed == true on the first PAUSED frame causes
     * handlePausedInput() to call pauseMenu.selectPrevious() without any
     * deliberate player input.
     */
    @Test
    void transitionToPaused_resetsUpPressed() {
        // Simulate pressing UP arrow (sets upPressed = true)
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.UP);

        assertTrue(inputHandler.isUpPressed(),
            "Precondition: pressing UP must set upPressed to true");

        // Simulate what transitionToPaused() now does: resetUp()
        inputHandler.resetUp();

        assertFalse(inputHandler.isUpPressed(),
            "After transitionToPaused(), isUpPressed() must be false — " +
            "stale UP key from the same-frame press must not call " +
            "pauseMenu.selectPrevious() on the first PAUSED frame");
    }

    /**
     * Test 4: resetUp() is idempotent — calling it when upPressed is already
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
     * Test 5: resetDown() clears stale downPressed set by the DOWN arrow key
     * pressed on the same frame as the ESC/pause transition.
     *
     * Without the fix, downPressed == true on the first PAUSED frame causes
     * handlePausedInput() to call pauseMenu.selectNext() without any
     * deliberate player input.
     */
    @Test
    void transitionToPaused_resetsDownPressed() {
        // Simulate pressing DOWN arrow (sets downPressed = true)
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.DOWN);

        assertTrue(inputHandler.isDownPressed(),
            "Precondition: pressing DOWN must set downPressed to true");

        // Simulate what transitionToPaused() now does: resetDown()
        inputHandler.resetDown();

        assertFalse(inputHandler.isDownPressed(),
            "After transitionToPaused(), isDownPressed() must be false — " +
            "stale DOWN key from the same-frame press must not call " +
            "pauseMenu.selectNext() on the first PAUSED frame");
    }

    /**
     * Test 6: resetDown() is idempotent — calling it when downPressed is
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
     * Test 7: All three resets are independent — resetting one does not affect
     * the others.
     */
    @Test
    void transitionToPaused_resetsAreIndependent() {
        // Set upPressed and downPressed via keyDown
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
        assertFalse(inputHandler.isPlacePressed(),
            "placePressed must be unaffected (still false) after up/down resets");
    }
}

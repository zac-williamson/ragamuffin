package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.InputHandler;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #589:
 * transitionToPlaying() and transitionToPaused() do not reset leftClickPressed
 * or leftClickReleased, causing stale mouse-click events on the first frame
 * after transition.
 *
 * <p>The fix adds the following calls to both transition methods:
 * <ul>
 *   <li>{@code inputHandler.resetLeftClick()} — clears stale leftClickPressed</li>
 *   <li>{@code inputHandler.resetLeftClickReleased()} — clears stale leftClickReleased</li>
 * </ul>
 *
 * <p>Tests mirror the pattern established by fixes #583 and #587.
 */
class Issue589TransitionStaleLeftClickTest {

    private InputHandler inputHandler;

    @BeforeEach
    void setUp() {
        inputHandler = new InputHandler();
    }

    /**
     * Test 1: resetLeftClick() clears stale leftClickPressed set by a mouse-click
     * on the same frame as the ESC-to-pause transition.
     *
     * Without the fix, leftClickPressed == true on the first PAUSED frame causes
     * pauseMenu.handleClick() to activate a pause-menu option the player did not intend.
     */
    @Test
    void transitionToPaused_resetsLeftClickPressed() {
        // Simulate a left mouse button press (sets leftClickPressed = true)
        inputHandler.touchDown(0, 0, 0, com.badlogic.gdx.Input.Buttons.LEFT);

        assertTrue(inputHandler.isLeftClickPressed(),
            "Precondition: touchDown with LEFT button must set leftClickPressed to true");

        // Simulate what transitionToPaused() now does: resetLeftClick()
        inputHandler.resetLeftClick();

        assertFalse(inputHandler.isLeftClickPressed(),
            "After transitionToPaused(), isLeftClickPressed() must be false — " +
            "stale left-click from the same-frame press must not activate a " +
            "pause-menu option on the first PAUSED frame");
    }

    /**
     * Test 2: resetLeftClickReleased() clears stale leftClickReleased set when
     * the player releases the mouse button on the same frame as ESC-to-pause.
     *
     * Without the fix, leftClickReleased == true on the first PAUSED frame causes
     * spurious drag-drop release handling in inventoryUI.
     */
    @Test
    void transitionToPaused_resetsLeftClickReleased() {
        // Simulate a left mouse press+release (sets leftClickReleased = true)
        inputHandler.touchDown(0, 0, 0, com.badlogic.gdx.Input.Buttons.LEFT);
        inputHandler.touchUp(0, 0, 0, com.badlogic.gdx.Input.Buttons.LEFT);

        assertTrue(inputHandler.isLeftClickReleased(),
            "Precondition: touchUp with LEFT button must set leftClickReleased to true");

        // Simulate what transitionToPaused() now does: resetLeftClickReleased()
        inputHandler.resetLeftClickReleased();

        assertFalse(inputHandler.isLeftClickReleased(),
            "After transitionToPaused(), isLeftClickReleased() must be false — " +
            "stale release flag must not trigger a phantom drag-drop release on the " +
            "first PAUSED frame");
    }

    /**
     * Test 3: resetLeftClick() clears stale leftClickPressed after the "Resume"
     * button click triggers transitionToPlaying().
     *
     * Without the fix, leftClickPressed == true on the first PLAYING frame may
     * register a spurious inventory slot click or other UI action.
     */
    @Test
    void transitionToPlaying_resetsLeftClickPressed() {
        // Simulate the Resume button click (sets leftClickPressed = true)
        inputHandler.touchDown(0, 0, 0, com.badlogic.gdx.Input.Buttons.LEFT);

        assertTrue(inputHandler.isLeftClickPressed(),
            "Precondition: pressing Resume (touchDown LEFT) must set leftClickPressed to true");

        // Simulate what transitionToPlaying() now does: resetLeftClick()
        inputHandler.resetLeftClick();

        assertFalse(inputHandler.isLeftClickPressed(),
            "After transitionToPlaying(), isLeftClickPressed() must be false — " +
            "the Resume button click must not fire a spurious UI action on the " +
            "first PLAYING frame after resume");
    }

    /**
     * Test 4: resetLeftClickReleased() clears stale leftClickReleased after the
     * "Resume" button click triggers transitionToPlaying().
     *
     * Without the fix, leftClickReleased == true on the first PLAYING frame may
     * trigger a phantom drag-drop release in inventoryUI, corrupting item drag state.
     */
    @Test
    void transitionToPlaying_resetsLeftClickReleased() {
        // Simulate the Resume button press+release (sets leftClickReleased = true)
        inputHandler.touchDown(0, 0, 0, com.badlogic.gdx.Input.Buttons.LEFT);
        inputHandler.touchUp(0, 0, 0, com.badlogic.gdx.Input.Buttons.LEFT);

        assertTrue(inputHandler.isLeftClickReleased(),
            "Precondition: touchUp with LEFT button must set leftClickReleased to true");

        // Simulate what transitionToPlaying() now does: resetLeftClickReleased()
        inputHandler.resetLeftClickReleased();

        assertFalse(inputHandler.isLeftClickReleased(),
            "After transitionToPlaying(), isLeftClickReleased() must be false — " +
            "stale release flag from the Resume click must not trigger a phantom " +
            "drag-drop release on the first PLAYING frame after resume");
    }

    /**
     * Test 5: resetLeftClick() is idempotent — calling it when leftClickPressed
     * is already false must not throw or change any state.
     */
    @Test
    void resetLeftClick_isIdempotent() {
        assertFalse(inputHandler.isLeftClickPressed(),
            "Precondition: leftClickPressed must be false on fresh InputHandler");

        inputHandler.resetLeftClick();
        inputHandler.resetLeftClick();

        assertFalse(inputHandler.isLeftClickPressed(),
            "leftClickPressed must remain false after repeated resetLeftClick() calls");
    }

    /**
     * Test 6: resetLeftClickReleased() is idempotent — calling it when
     * leftClickReleased is already false must not throw or change any state.
     */
    @Test
    void resetLeftClickReleased_isIdempotent() {
        assertFalse(inputHandler.isLeftClickReleased(),
            "Precondition: leftClickReleased must be false on fresh InputHandler");

        inputHandler.resetLeftClickReleased();
        inputHandler.resetLeftClickReleased();

        assertFalse(inputHandler.isLeftClickReleased(),
            "leftClickReleased must remain false after repeated resetLeftClickReleased() calls");
    }

    /**
     * Test 7: The two resets are independent — resetting one does not affect the other.
     */
    @Test
    void transitionResets_areIndependent() {
        // Set both flags via press+release
        inputHandler.touchDown(0, 0, 0, com.badlogic.gdx.Input.Buttons.LEFT);
        inputHandler.touchUp(0, 0, 0, com.badlogic.gdx.Input.Buttons.LEFT);

        assertTrue(inputHandler.isLeftClickPressed() || inputHandler.isLeftClickReleased(),
            "Precondition: at least one of leftClickPressed/leftClickReleased must be true " +
            "after press+release sequence");

        // Reset only leftClickReleased
        inputHandler.resetLeftClickReleased();

        assertFalse(inputHandler.isLeftClickReleased(),
            "leftClickReleased must be false after resetLeftClickReleased()");

        // Reset leftClickPressed
        inputHandler.resetLeftClick();

        assertFalse(inputHandler.isLeftClickPressed(),
            "leftClickPressed must be false after resetLeftClick()");
        assertFalse(inputHandler.isLeftClickReleased(),
            "leftClickReleased must still be false");
    }
}

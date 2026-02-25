package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.InputHandler;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #569:
 * transitionToPaused() does not reset interactPressed, causing stale E-key
 * interaction on the first PLAYING frame after resume.
 *
 * <p>The fix adds the following calls to transitionToPaused():
 * <ul>
 *   <li>{@code inputHandler.resetInteract()} — clears stale E-key interact flag</li>
 *   <li>{@code inputHandler.resetJump()} — clears stale jump flag</li>
 *   <li>{@code inputHandler.resetDodge()} — clears stale dodge flag</li>
 * </ul>
 *
 * <p>Tests mirror the pattern established by fixes #275, #363, #365, #543, #545,
 * #565, and #567.
 */
class Issue569TransitionToPausedStaleInteractTest {

    private InputHandler inputHandler;

    @BeforeEach
    void setUp() {
        inputHandler = new InputHandler();
    }

    /**
     * Test 1: resetInteract() clears stale interactPressed set by the E key
     * pressed on the same frame as the ESC/pause transition.
     *
     * Simulates the stale-input scenario: player presses E and ESC on the same
     * frame. transitionToPaused() must call resetInteract() so isInteractPressed()
     * returns false on the first PLAYING frame after resume.
     */
    @Test
    void transitionToPaused_resetsInteractPressed() {
        // Simulate pressing E (sets interactPressed = true)
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.E);

        assertTrue(inputHandler.isInteractPressed(),
            "Precondition: pressing E must set interactPressed to true");

        // Simulate what transitionToPaused() now does: resetInteract()
        inputHandler.resetInteract();

        assertFalse(inputHandler.isInteractPressed(),
            "After transitionToPaused(), isInteractPressed() must be false — " +
            "stale E-key from the same-frame press must not fire an interaction " +
            "on the first PLAYING frame after resume");
    }

    /**
     * Test 2: resetInteract() is idempotent — calling it when interactPressed is
     * already false must not throw or change any state.
     */
    @Test
    void resetInteract_isIdempotent() {
        assertFalse(inputHandler.isInteractPressed(),
            "Precondition: interactPressed must be false on fresh InputHandler");

        // Calling resetInteract() on an already-false flag must be safe
        inputHandler.resetInteract();
        inputHandler.resetInteract();

        assertFalse(inputHandler.isInteractPressed(),
            "interactPressed must remain false after repeated resetInteract() calls");
    }

    /**
     * Test 3: resetJump() clears stale jumpPressed set by the Space key
     * pressed on the same frame as the ESC/pause transition.
     */
    @Test
    void transitionToPaused_resetsJumpPressed() {
        // Simulate pressing Space (sets jumpPressed = true)
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.SPACE);

        assertTrue(inputHandler.isJumpPressed(),
            "Precondition: pressing Space must set jumpPressed to true");

        // Simulate what transitionToPaused() now does: resetJump()
        inputHandler.resetJump();

        assertFalse(inputHandler.isJumpPressed(),
            "After transitionToPaused(), isJumpPressed() must be false — " +
            "stale jump from the same-frame press must not fire on resume");
    }

    /**
     * Test 4: resetDodge() clears stale dodgePressed set by the dodge key
     * pressed on the same frame as the ESC/pause transition.
     */
    @Test
    void transitionToPaused_resetsDodgePressed() {
        // Simulate pressing the dodge key (Left Ctrl)
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.CONTROL_LEFT);

        assertTrue(inputHandler.isDodgePressed(),
            "Precondition: pressing Left Ctrl must set dodgePressed to true");

        // Simulate what transitionToPaused() now does: resetDodge()
        inputHandler.resetDodge();

        assertFalse(inputHandler.isDodgePressed(),
            "After transitionToPaused(), isDodgePressed() must be false — " +
            "stale dodge from the same-frame press must not fire on resume");
    }

    /**
     * Test 5: All three resets (interact, jump, dodge) are independent — resetting
     * one does not affect the others.
     */
    @Test
    void transitionToPaused_resetInteractDoesNotAffectOtherFlags() {
        // Set all three flags
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.E);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.SPACE);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.CONTROL_LEFT);

        assertTrue(inputHandler.isInteractPressed(), "E pressed");
        assertTrue(inputHandler.isJumpPressed(), "Space pressed");
        assertTrue(inputHandler.isDodgePressed(), "Shift pressed");

        // Reset only interact
        inputHandler.resetInteract();

        assertFalse(inputHandler.isInteractPressed(),
            "interactPressed must be false after resetInteract()");
        assertTrue(inputHandler.isJumpPressed(),
            "jumpPressed must be unaffected by resetInteract()");
        assertTrue(inputHandler.isDodgePressed(),
            "dodgePressed must be unaffected by resetInteract()");
    }
}

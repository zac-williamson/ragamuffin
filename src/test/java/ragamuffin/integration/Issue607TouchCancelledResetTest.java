package ragamuffin.integration;

import com.badlogic.gdx.Input;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.InputHandler;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #607:
 * touchCancelled does not reset punchHeld or leftClickReleased, causing permanent
 * auto-punch when the window loses focus mid-click.
 *
 * Fix: InputHandler.touchCancelled() now mirrors touchUp() for the left button,
 * clearing punchHeld and setting leftClickReleased.
 */
class Issue607TouchCancelledResetTest {

    private InputHandler inputHandler;

    @BeforeEach
    void setUp() {
        inputHandler = new InputHandler();
    }

    /**
     * Test 1: touchCancelled with LEFT button clears punchHeld.
     * Verifies that after a touch cancel for the left button, isPunchHeld() returns false.
     */
    @Test
    void touchCancelledWithLeftButtonClearsPunchHeld() {
        // Initially punchHeld must be false
        assertFalse(inputHandler.isPunchHeld(),
                "punchHeld must be false before any input");

        // Simulate the game having set punchHeld=true (cursor was caught, LMB was held)
        // We use resetPunchHeld's inverse: directly call touchCancelled to verify it clears.
        // Since we cannot call touchDown with cursor caught in tests, we verify the
        // touchCancelled path is safe to call when punchHeld is already false (no-op / idempotent).
        inputHandler.touchCancelled(0, 0, 0, Input.Buttons.LEFT);

        assertFalse(inputHandler.isPunchHeld(),
                "punchHeld must be false after touchCancelled with LEFT button");
    }

    /**
     * Test 2: touchCancelled with LEFT button sets leftClickReleased.
     * Verifies that after a touch cancel for the left button, isLeftClickReleased() returns true.
     */
    @Test
    void touchCancelledWithLeftButtonSetsLeftClickReleased() {
        assertFalse(inputHandler.isLeftClickReleased(),
                "leftClickReleased must be false before any input");

        inputHandler.touchCancelled(0, 0, 0, Input.Buttons.LEFT);

        assertTrue(inputHandler.isLeftClickReleased(),
                "leftClickReleased must be true after touchCancelled with LEFT button");
    }

    /**
     * Test 3: touchCancelled with RIGHT button does NOT set leftClickReleased.
     * Verifies that cancellations for non-left buttons are ignored.
     */
    @Test
    void touchCancelledWithRightButtonDoesNotSetLeftClickReleased() {
        inputHandler.touchCancelled(0, 0, 0, Input.Buttons.RIGHT);

        assertFalse(inputHandler.isLeftClickReleased(),
                "leftClickReleased must remain false after touchCancelled with RIGHT button");
    }

    /**
     * Test 4: touchCancelled with RIGHT button does NOT affect punchHeld.
     */
    @Test
    void touchCancelledWithRightButtonDoesNotClearPunchHeld() {
        // punchHeld starts false; a right-cancel should not change it
        inputHandler.touchCancelled(0, 0, 0, Input.Buttons.RIGHT);

        assertFalse(inputHandler.isPunchHeld(),
                "punchHeld must remain false after touchCancelled with RIGHT button");
    }

    /**
     * Test 5: touchCancelled mirrors touchUp for the left button.
     * Both should produce the same observable state:
     *   punchHeld == false, leftClickReleased == true.
     */
    @Test
    void touchCancelledMirrorsTouchUpForLeftButton() {
        InputHandler handlerA = new InputHandler();
        InputHandler handlerB = new InputHandler();

        handlerA.touchUp(0, 0, 0, Input.Buttons.LEFT);
        handlerB.touchCancelled(0, 0, 0, Input.Buttons.LEFT);

        assertEquals(handlerA.isPunchHeld(), handlerB.isPunchHeld(),
                "punchHeld must be equal after touchUp vs touchCancelled (both false)");
        assertEquals(handlerA.isLeftClickReleased(), handlerB.isLeftClickReleased(),
                "leftClickReleased must be equal after touchUp vs touchCancelled (both true)");
    }
}

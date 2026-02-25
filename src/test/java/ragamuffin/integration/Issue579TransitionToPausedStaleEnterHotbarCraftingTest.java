package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.InputHandler;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #579:
 * transitionToPaused() does not reset enterPressed, hotbarSlotPressed, or
 * craftingSlotPressed, causing stale key actions on first PAUSED frame.
 *
 * <p>The fix adds the following calls to transitionToPaused():
 * <ul>
 *   <li>{@code inputHandler.resetEnter()} — clears stale ENTER key flag</li>
 *   <li>{@code inputHandler.resetHotbarSlot()} — clears stale hotbar slot selection</li>
 *   <li>{@code inputHandler.resetCraftingSlot()} — clears stale crafting slot selection</li>
 * </ul>
 *
 * <p>Tests mirror the pattern established by fixes #569, #571, #573, #575, #577.
 */
class Issue579TransitionToPausedStaleEnterHotbarCraftingTest {

    private InputHandler inputHandler;

    @BeforeEach
    void setUp() {
        inputHandler = new InputHandler();
    }

    /**
     * Test 1: resetEnter() clears stale enterPressed set by the ENTER key
     * pressed on the same frame as the ESC/pause transition.
     *
     * Simulates the stale-input scenario: player presses ENTER and ESC on the
     * same frame. transitionToPaused() must call resetEnter() so isEnterPressed()
     * returns false on the first PAUSED frame, preventing an immediate pause-menu
     * action (Resume/Restart/Quit) from firing without deliberate player input.
     */
    @Test
    void transitionToPaused_resetsEnterPressed() {
        // Simulate pressing ENTER (sets enterPressed = true)
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.ENTER);

        assertTrue(inputHandler.isEnterPressed(),
            "Precondition: pressing ENTER must set enterPressed to true");

        // Simulate what transitionToPaused() now does: resetEnter()
        inputHandler.resetEnter();

        assertFalse(inputHandler.isEnterPressed(),
            "After transitionToPaused(), isEnterPressed() must be false — " +
            "stale ENTER from the same-frame press must not fire a pause-menu " +
            "action on the first PAUSED frame");
    }

    /**
     * Test 2: resetEnter() is idempotent — calling it when enterPressed is
     * already false must not throw or change any state.
     */
    @Test
    void resetEnter_isIdempotent() {
        assertFalse(inputHandler.isEnterPressed(),
            "Precondition: enterPressed must be false on fresh InputHandler");

        inputHandler.resetEnter();
        inputHandler.resetEnter();

        assertFalse(inputHandler.isEnterPressed(),
            "enterPressed must remain false after repeated resetEnter() calls");
    }

    /**
     * Test 3: resetHotbarSlot() clears stale hotbarSlotPressed set by a number
     * key (1-9) pressed on the same frame as the ESC/pause transition.
     */
    @Test
    void transitionToPaused_resetsHotbarSlotPressed() {
        // Simulate pressing key '1' (sets hotbarSlotPressed = 0)
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.NUM_1);

        assertEquals(0, inputHandler.getHotbarSlotPressed(),
            "Precondition: pressing '1' must set hotbarSlotPressed to 0");

        // Simulate what transitionToPaused() now does: resetHotbarSlot()
        inputHandler.resetHotbarSlot();

        assertEquals(-1, inputHandler.getHotbarSlotPressed(),
            "After transitionToPaused(), getHotbarSlotPressed() must be -1 — " +
            "stale hotbar slot from the same-frame press must not persist into PAUSED state");
    }

    /**
     * Test 4: resetHotbarSlot() is idempotent — calling it when hotbarSlotPressed
     * is already -1 must not throw or change any state.
     */
    @Test
    void resetHotbarSlot_isIdempotent() {
        assertEquals(-1, inputHandler.getHotbarSlotPressed(),
            "Precondition: hotbarSlotPressed must be -1 on fresh InputHandler");

        inputHandler.resetHotbarSlot();
        inputHandler.resetHotbarSlot();

        assertEquals(-1, inputHandler.getHotbarSlotPressed(),
            "hotbarSlotPressed must remain -1 after repeated resetHotbarSlot() calls");
    }

    /**
     * Test 5: resetCraftingSlot() clears stale craftingSlotPressed set by a number
     * key (1-9) pressed on the same frame as the ESC/pause transition.
     */
    @Test
    void transitionToPaused_resetsCraftingSlotPressed() {
        // Simulate pressing key '5' (sets craftingSlotPressed = 4)
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.NUM_5);

        assertEquals(4, inputHandler.getCraftingSlotPressed(),
            "Precondition: pressing '5' must set craftingSlotPressed to 4");

        // Simulate what transitionToPaused() now does: resetCraftingSlot()
        inputHandler.resetCraftingSlot();

        assertEquals(-1, inputHandler.getCraftingSlotPressed(),
            "After transitionToPaused(), getCraftingSlotPressed() must be -1 — " +
            "stale crafting slot from the same-frame press must not persist into PAUSED state");
    }

    /**
     * Test 6: resetCraftingSlot() is idempotent — calling it when craftingSlotPressed
     * is already -1 must not throw or change any state.
     */
    @Test
    void resetCraftingSlot_isIdempotent() {
        assertEquals(-1, inputHandler.getCraftingSlotPressed(),
            "Precondition: craftingSlotPressed must be -1 on fresh InputHandler");

        inputHandler.resetCraftingSlot();
        inputHandler.resetCraftingSlot();

        assertEquals(-1, inputHandler.getCraftingSlotPressed(),
            "craftingSlotPressed must remain -1 after repeated resetCraftingSlot() calls");
    }

    /**
     * Test 7: All three resets are independent — resetting one does not affect the others.
     */
    @Test
    void transitionToPaused_resetsAreIndependent() {
        // Set all three flags
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.ENTER);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.NUM_3);

        assertTrue(inputHandler.isEnterPressed(), "ENTER pressed");
        assertEquals(2, inputHandler.getHotbarSlotPressed(), "hotbarSlot 3 pressed");
        assertEquals(2, inputHandler.getCraftingSlotPressed(), "craftingSlot 3 pressed");

        // Reset only enter — others must be unaffected
        inputHandler.resetEnter();

        assertFalse(inputHandler.isEnterPressed(),
            "enterPressed must be false after resetEnter()");
        assertEquals(2, inputHandler.getHotbarSlotPressed(),
            "hotbarSlotPressed must be unaffected by resetEnter()");
        assertEquals(2, inputHandler.getCraftingSlotPressed(),
            "craftingSlotPressed must be unaffected by resetEnter()");

        // Reset hotbar — enter already cleared, crafting still set
        inputHandler.resetHotbarSlot();

        assertEquals(-1, inputHandler.getHotbarSlotPressed(),
            "hotbarSlotPressed must be -1 after resetHotbarSlot()");
        assertEquals(2, inputHandler.getCraftingSlotPressed(),
            "craftingSlotPressed must be unaffected by resetHotbarSlot()");

        // Reset crafting — all now cleared
        inputHandler.resetCraftingSlot();

        assertEquals(-1, inputHandler.getCraftingSlotPressed(),
            "craftingSlotPressed must be -1 after resetCraftingSlot()");
    }
}

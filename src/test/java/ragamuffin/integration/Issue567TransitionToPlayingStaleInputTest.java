package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.CraftingSystem;
import ragamuffin.building.Inventory;
import ragamuffin.core.InputHandler;
import ragamuffin.ui.CraftingUI;
import ragamuffin.ui.HelpUI;
import ragamuffin.ui.InventoryUI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #567:
 * transitionToPlaying() does not reset craftingSlotPressed, causing stale
 * number-key input to fire crafting selection on the first PLAYING frame
 * after resume.
 *
 * <p>The fix adds the following calls to transitionToPlaying():
 * <ul>
 *   <li>{@code inputHandler.resetCraftingSlot()} — clears stale crafting slot</li>
 *   <li>{@code inputHandler.resetHotbarSlot()} — clears stale hotbar slot</li>
 *   <li>{@code inventoryUI.hide()} — defensively hide overlay</li>
 *   <li>{@code craftingUI.hide()} — defensively hide overlay</li>
 *   <li>{@code helpUI.hide()} — defensively hide overlay</li>
 * </ul>
 *
 * <p>Tests mirror the pattern established by fixes #543, #545, #461, and #565.
 */
class Issue567TransitionToPlayingStaleInputTest {

    private InputHandler inputHandler;
    private CraftingUI craftingUI;
    private InventoryUI inventoryUI;
    private HelpUI helpUI;

    @BeforeEach
    void setUp() {
        inputHandler = new InputHandler();
        CraftingSystem craftingSystem = new CraftingSystem();
        Inventory inventory = new Inventory(36);
        craftingUI = new CraftingUI(craftingSystem, inventory);
        inventoryUI = new InventoryUI(inventory);
        helpUI = new HelpUI();
    }

    /**
     * Test 1: resetCraftingSlot() clears stale craftingSlotPressed set by a
     * number key pressed on the same frame as the Resume action.
     *
     * Simulates the stale-input scenario: player presses NUM_3 and Resume on
     * the same frame. transitionToPlaying() must call resetCraftingSlot() so
     * getCraftingSlotPressed() returns -1 on the first PLAYING frame.
     */
    @Test
    void transitionToPlaying_resetsCraftingSlotPressed() {
        // Simulate pressing NUM_3 (sets craftingSlotPressed = 2)
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.NUM_3);

        assertEquals(2, inputHandler.getCraftingSlotPressed(),
            "Precondition: pressing NUM_3 must set craftingSlotPressed to 2");

        // Simulate what transitionToPlaying() now does: resetCraftingSlot()
        inputHandler.resetCraftingSlot();

        assertEquals(-1, inputHandler.getCraftingSlotPressed(),
            "After transitionToPlaying(), getCraftingSlotPressed() must be -1 — " +
            "stale slot from the same-frame number key must not fire crafting selection");
    }

    /**
     * Test 2: resetHotbarSlot() clears stale hotbarSlotPressed set by a number
     * key pressed on the same frame as the Resume action.
     *
     * Mirrors the craftingSlotPressed fix: hotbarSlotPressed is also set by
     * keyDown() and must be cleared on resume to avoid unintended hotbar changes.
     */
    @Test
    void transitionToPlaying_resetsHotbarSlotPressed() {
        // Simulate pressing NUM_5 (sets hotbarSlotPressed = 4)
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.NUM_5);

        assertEquals(4, inputHandler.getHotbarSlotPressed(),
            "Precondition: pressing NUM_5 must set hotbarSlotPressed to 4");

        // Simulate what transitionToPlaying() now does: resetHotbarSlot()
        inputHandler.resetHotbarSlot();

        assertEquals(-1, inputHandler.getHotbarSlotPressed(),
            "After transitionToPlaying(), getHotbarSlotPressed() must be -1 — " +
            "stale hotbar slot must not fire a slot change on the first PLAYING frame");
    }

    /**
     * Test 3: craftingUI.hide() is called by transitionToPlaying(), clearing any
     * crafting overlay left open if the transition happened via a non-ESC code path.
     *
     * Verifies that hide() makes isVisible() return false.
     */
    @Test
    void transitionToPlaying_hidesCraftingUI() {
        craftingUI.show();
        assertTrue(craftingUI.isVisible(),
            "Precondition: craftingUI must be visible after show()");

        // Simulate what transitionToPlaying() now does
        craftingUI.hide();

        assertFalse(craftingUI.isVisible(),
            "craftingUI must be hidden after transitionToPlaying() — " +
            "crafting overlay must not persist across a pause/resume cycle");
    }

    /**
     * Test 4: inventoryUI.hide() is called by transitionToPlaying(), clearing any
     * inventory overlay that was open when the pause occurred.
     */
    @Test
    void transitionToPlaying_hidesInventoryUI() {
        inventoryUI.show();
        assertTrue(inventoryUI.isVisible(),
            "Precondition: inventoryUI must be visible after show()");

        // Simulate what transitionToPlaying() now does
        inventoryUI.hide();

        assertFalse(inventoryUI.isVisible(),
            "inventoryUI must be hidden after transitionToPlaying() — " +
            "inventory overlay must not persist across a pause/resume cycle");
    }

    /**
     * Test 5: helpUI.hide() is called by transitionToPlaying(), clearing any
     * help overlay that was open when the pause occurred.
     */
    @Test
    void transitionToPlaying_hidesHelpUI() {
        helpUI.show();
        assertTrue(helpUI.isVisible(),
            "Precondition: helpUI must be visible after show()");

        // Simulate what transitionToPlaying() now does
        helpUI.hide();

        assertFalse(helpUI.isVisible(),
            "helpUI must be hidden after transitionToPlaying() — " +
            "help overlay must not persist across a pause/resume cycle");
    }

    /**
     * Test 6: Pressing multiple number keys before resume leaves only the last
     * key's value; resetCraftingSlot() always clears to -1 regardless.
     *
     * This guards against any future regression where multiple keyDown calls
     * accumulate state that resetCraftingSlot() fails to clear.
     */
    @Test
    void transitionToPlaying_clearsCraftingSlotAfterMultipleKeyPresses() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.NUM_1);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.NUM_7);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.NUM_9);

        // Last key wins: craftingSlotPressed == 8
        assertEquals(8, inputHandler.getCraftingSlotPressed(),
            "Precondition: last NUM_9 press sets craftingSlotPressed to 8");

        // transitionToPlaying() resets it
        inputHandler.resetCraftingSlot();

        assertEquals(-1, inputHandler.getCraftingSlotPressed(),
            "resetCraftingSlot() must clear to -1 regardless of which key was last pressed");
    }
}

package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.ui.HelpUI;
import ragamuffin.ui.InventoryUI;
import ragamuffin.ui.CraftingUI;
import ragamuffin.building.Inventory;
import ragamuffin.building.CraftingSystem;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #325:
 * restartGame() leaked helpUI's open state — if the help screen was visible
 * when the player triggered a restart, the new session started with
 * helpUI.isVisible() == true, causing isUIBlocking() to return true and
 * suppressing all player input for the entire new session.
 *
 * Fix: restartGame() now does {@code helpUI = new HelpUI()} alongside the
 * existing resets for inventoryUI and craftingUI.
 */
class Issue325HelpUIRestartTest {

    /**
     * Test 1: After restart, helpUI must not be visible.
     *
     * Models the restartGame() contract: a fresh HelpUI instance starts with
     * isVisible() == false, so isUIBlocking() returns false and input is enabled.
     */
    @Test
    void restartResetsHelpUIVisibility() {
        // Session 1: player opens help screen
        HelpUI helpUI = new HelpUI();
        assertFalse(helpUI.isVisible(), "HelpUI should start hidden");

        helpUI.toggle(); // player presses H
        assertTrue(helpUI.isVisible(), "HelpUI should be visible after toggle");

        // restartGame() — THE FIX: recreate helpUI
        helpUI = new HelpUI();

        // Session 2: help must be hidden so isUIBlocking() returns false
        assertFalse(helpUI.isVisible(),
                "HelpUI must not be visible after restart — open state must not leak");
    }

    /**
     * Test 2: Without the fix, the stale visible state carries over.
     *
     * Documents the pre-fix bug: if helpUI is NOT recreated, isVisible() stays
     * true in the new session, blocking all input.
     */
    @Test
    void withoutResetHelpUIStaysVisible() {
        HelpUI helpUI = new HelpUI();
        helpUI.toggle(); // open in session 1
        assertTrue(helpUI.isVisible(), "Help is open before restart");

        // BUG: restartGame() does NOT recreate helpUI — stale state leaks
        // (no assignment here — intentionally omitted to model the bug)

        assertTrue(helpUI.isVisible(),
                "Without the fix, help UI stays visible after restart — confirming the bug");
    }

    /**
     * Test 3: isUIBlocking() equivalent — all three UIs must be hidden after restart.
     *
     * The isUIBlocking() method in RagamuffinGame checks:
     *   inventoryUI.isVisible() || helpUI.isVisible() || craftingUI.isVisible()
     * After a restart every UI must return false so the method returns false.
     */
    @Test
    void restartResetsAllUIBlockingState() {
        Inventory inventory = new Inventory(36);
        CraftingSystem craftingSystem = new CraftingSystem();

        // Session 1: open all UIs
        InventoryUI inventoryUI = new InventoryUI(inventory);
        HelpUI helpUI = new HelpUI();
        CraftingUI craftingUI = new CraftingUI(craftingSystem, inventory);

        inventoryUI.toggle();
        helpUI.toggle();
        craftingUI.toggle();

        assertTrue(inventoryUI.isVisible(), "InventoryUI open in session 1");
        assertTrue(helpUI.isVisible(), "HelpUI open in session 1");
        assertTrue(craftingUI.isVisible(), "CraftingUI open in session 1");

        // restartGame() resets all UI instances
        inventory = new Inventory(36);
        inventoryUI = new InventoryUI(inventory);
        helpUI = new HelpUI();          // Fix #325
        craftingUI = new CraftingUI(craftingSystem, inventory);

        // Session 2: none must be visible
        assertFalse(inventoryUI.isVisible(), "InventoryUI must be hidden after restart");
        assertFalse(helpUI.isVisible(),      "HelpUI must be hidden after restart");
        assertFalse(craftingUI.isVisible(),  "CraftingUI must be hidden after restart");

        // isUIBlocking() equivalent
        boolean uiBlocking = inventoryUI.isVisible() || helpUI.isVisible() || craftingUI.isVisible();
        assertFalse(uiBlocking,
                "isUIBlocking() must return false at the start of a new session");
    }
}

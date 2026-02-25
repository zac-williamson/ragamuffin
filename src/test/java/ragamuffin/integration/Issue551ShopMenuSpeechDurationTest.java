package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.InteractionSystem;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #551 — shop menu speech bubble expires too quickly,
 * leaving stale activeShopkeeperNPC state.
 *
 * Fix #551 has two parts:
 *   1. The shop menu speech bubble duration is increased from 3 s to 10 s so that
 *      the player has time to read, select an item with 1/2/3, and confirm with E.
 *   2. When the NPC's own speech timer expires and closes the menu, the game loop
 *      must clear activeShopkeeperNPC so the stale reference does not persist.
 *
 * These tests validate the logic that mirrors updatePlayingSimulation():
 *
 *   // stale-state guard (Fix #551)
 *   if (activeShopkeeperNPC != null && !activeShopkeeperNPC.isShopMenuOpen()) {
 *       activeShopkeeperNPC = null;
 *   }
 */
class Issue551ShopMenuSpeechDurationTest {

    /**
     * Simulates the stale-state guard added in RagamuffinGame.updatePlayingSimulation()
     * by Fix #551:
     *
     *   if (activeShopkeeperNPC != null && !activeShopkeeperNPC.isShopMenuOpen()) {
     *       activeShopkeeperNPC = null;
     *   }
     */
    private static NPC runStaleStateGuard(NPC activeShopkeeperNPC) {
        if (activeShopkeeperNPC != null && !activeShopkeeperNPC.isShopMenuOpen()) {
            activeShopkeeperNPC = null;
        }
        return activeShopkeeperNPC;
    }

    // --- Speech bubble duration is at least 10 seconds ---

    @Test
    void shopMenuSpeechBubble_remainsOpen_after3Seconds() {
        // Before Fix #551, the duration was 3 s so the menu would close after 3 s.
        // After the fix, the duration is 10 s, so 3 s should NOT expire the menu.
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 2);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open menu

        assertTrue(shopkeeper.isShopMenuOpen(), "Menu must be open after first E-press");

        // Simulate 3 seconds passing — with the old 3-second duration the menu would
        // have just expired; with the new 10-second duration it must remain open.
        shopkeeper.update(3f);

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Shop menu must still be open after 3 seconds (duration is now 10 s, not 3 s)");
        assertNotNull(shopkeeper.getSpeechText(),
            "Speech bubble must still be visible after 3 seconds");
    }

    @Test
    void shopMenuSpeechBubble_closesAfter10Seconds() {
        // After Fix #551, the menu should close after 10 seconds.
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 2);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open menu

        assertTrue(shopkeeper.isShopMenuOpen(), "Menu must be open after first E-press");

        // Simulate 10+ seconds passing to expire the timer
        shopkeeper.update(10f);

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Shop menu must be closed after 10 seconds (speech timer expired)");
    }

    // --- Stale activeShopkeeperNPC is cleared when timer expires ---

    @Test
    void whenSpeechTimerExpires_staleGuard_clearsActiveShopkeeperNPC() {
        // Scenario from the issue: player opens menu, waits > 3 s (now > 10 s),
        // speech bubble disappears, NPC closes menu internally.
        // The stale-state guard must detect isShopMenuOpen() == false and clear the ref.
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 2);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open menu
        NPC activeShopkeeperNPC = shopkeeper;    // game sets this after first E-press

        assertTrue(shopkeeper.isShopMenuOpen(), "Precondition: menu must be open");
        assertNotNull(activeShopkeeperNPC, "Precondition: activeShopkeeperNPC must be set");

        // Simulate 10+ seconds passing — NPC's own timer closes the menu
        shopkeeper.update(10f);

        assertFalse(shopkeeper.isShopMenuOpen(),
            "After 10 s, NPC must have closed the shop menu internally via speech timer");

        // Now run the stale-state guard (mirrors updatePlayingSimulation() logic)
        activeShopkeeperNPC = runStaleStateGuard(activeShopkeeperNPC);

        assertNull(activeShopkeeperNPC,
            "activeShopkeeperNPC must be cleared to null when NPC's speech timer closed the menu");
    }

    @Test
    void whenMenuStillOpen_staleGuard_doesNotClearActiveShopkeeperNPC() {
        // Guard must be a no-op when the menu is still legitimately open.
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 2);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open menu
        NPC activeShopkeeperNPC = shopkeeper;

        // Simulate only 1 second — menu is still open (duration is 10 s)
        shopkeeper.update(1f);

        assertTrue(shopkeeper.isShopMenuOpen(), "Menu must still be open after 1 s");

        // Guard should not fire
        activeShopkeeperNPC = runStaleStateGuard(activeShopkeeperNPC);

        assertNotNull(activeShopkeeperNPC,
            "activeShopkeeperNPC must NOT be cleared when the menu is still open");
    }

    @Test
    void staleGuard_isNoOp_whenActiveShopkeeperIsNull() {
        // Guard must handle null safely.
        NPC result = runStaleStateGuard(null);
        assertNull(result, "Guard must return null unchanged when input is already null");
    }

    // --- After timer-based close, next E-press reopens the menu (not a purchase) ---

    @Test
    void afterSpeechExpires_nextEPress_reopensMenu_notPurchase() {
        // The core bug from Issue #551:
        // Before the fix, after the speech timer expires the stale activeShopkeeperNPC
        // is NOT cleared, so the next E-press sees isShopMenuOpen() == false and
        // treats it as a "second press" executing a purchase.
        //
        // After the fix, the stale guard clears activeShopkeeperNPC. The next E-press
        // (simulated here by calling interactWithNPC again) is therefore a first press
        // and opens the menu — it does NOT deduct currency.
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // first E-press: menu opens
        NPC activeShopkeeperNPC = shopkeeper;

        // Simulate timer expiry
        shopkeeper.update(10f);
        assertFalse(shopkeeper.isShopMenuOpen(), "Precondition: menu closed by timer");

        // Stale-state guard fires (Fix #551 — clears the ref)
        activeShopkeeperNPC = runStaleStateGuard(activeShopkeeperNPC);
        assertNull(activeShopkeeperNPC, "Precondition: activeShopkeeperNPC must be null after guard");

        // Player presses E again — this is now a first press since menu is closed
        // interactWithNPC with a closed menu and currency → opens the menu again
        int shillingsBefore = inv.getItemCount(Material.SHILLING);
        String dialogue = system.interactWithNPC(shopkeeper, inv);

        assertTrue(shopkeeper.isShopMenuOpen(),
            "After timer-expired menu, next E-press must reopen the menu (not execute a purchase)");
        assertEquals(shillingsBefore, inv.getItemCount(Material.SHILLING),
            "Next E-press after timer expiry must NOT deduct currency (it is a first press, not a purchase)");
        assertNotNull(dialogue, "Next E-press must return menu dialogue, not null");
        assertTrue(
            dialogue.contains("sausage roll") || dialogue.contains("energy drink") || dialogue.contains("crisps"),
            "Dialogue after reopening must list shop items, got: " + dialogue
        );
    }
}

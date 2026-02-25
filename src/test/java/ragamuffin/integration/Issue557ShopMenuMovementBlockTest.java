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
 * Integration tests for Issue #557 — player can move while shop menu is open.
 *
 * Fix: add {@code (activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen())}
 * to the {@code isUIBlocking()} method in {@code RagamuffinGame} so that WASD movement
 * is suppressed during a shop transaction, consistent with all other UI overlays.
 *
 * The E key is still allowed through when the shop menu is open so the purchase
 * confirmation flow (second E-press) continues to work.
 *
 * These tests validate the condition logic that governs movement blocking. They mirror
 * the {@code isUIBlocking()} check in {@code RagamuffinGame}:
 *
 * <pre>
 *   private boolean isUIBlocking() {
 *       return inventoryUI.isVisible() || helpUI.isVisible() || craftingUI.isVisible()
 *               || achievementsUI.isVisible() || questLogUI.isVisible()
 *               || (activeShopkeeperNPC != null &amp;&amp; activeShopkeeperNPC.isShopMenuOpen());
 *   }
 * </pre>
 */
class Issue557ShopMenuMovementBlockTest {

    /**
     * Mirrors the shop-menu portion of {@code isUIBlocking()} added by Fix #557.
     * Returns true when movement should be blocked (shop menu is open).
     */
    private static boolean shopMenuBlocksMovement(NPC activeShopkeeperNPC) {
        return activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen();
    }

    // --- Movement blocked when shop menu opens ---

    @Test
    void whenShopMenuIsOpen_shopMenuBlocksMovement_isTrue() {
        // After first E-press, the shop menu is open.
        // isUIBlocking() must return true → updatePlayingInput() is skipped → no WASD movement.
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // first E-press: menu opens

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: isShopMenuOpen() must be true after first E-press");

        boolean blocked = shopMenuBlocksMovement(shopkeeper);

        assertTrue(blocked,
            "Fix #557: when shop menu is open, isUIBlocking() must return true " +
            "so that updatePlayingInput() is skipped and the player cannot move with WASD");
    }

    // --- Movement unblocked when shop menu closes ---

    @Test
    void whenShopMenuCloses_afterPurchase_shopMenuBlocksMovement_isFalse() {
        // After second E-press (purchase confirmed), the shop menu closes.
        // isUIBlocking() must return false → movement is restored.
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open menu
        system.interactWithNPC(shopkeeper, inv); // confirm purchase → menu closes

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Precondition: isShopMenuOpen() must be false after second E-press (purchase)");

        boolean blocked = shopMenuBlocksMovement(shopkeeper);

        assertFalse(blocked,
            "Fix #557: after shop menu closes, isUIBlocking() must return false " +
            "so that normal WASD movement is restored");
    }

    @Test
    void whenNoActiveShopkeeper_shopMenuBlocksMovement_isFalse() {
        // When activeShopkeeperNPC is null, the condition must not block movement.
        boolean blocked = shopMenuBlocksMovement(null);

        assertFalse(blocked,
            "Fix #557: when activeShopkeeperNPC is null, the shop-menu condition must be false " +
            "so that movement is not spuriously blocked");
    }

    // --- Movement blocked for the full duration the menu is visible ---

    @Test
    void whileShopMenuIsOpen_movementRemainsBlocked_untilMenuCloses() {
        // Movement must stay blocked throughout the entire shop interaction:
        // open → (blocked) → select item → (still blocked) → confirm → (unblocked)
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();

        // Step 1: open menu
        system.interactWithNPC(shopkeeper, inv);
        assertTrue(shopMenuBlocksMovement(shopkeeper),
            "Movement must be blocked immediately after menu opens");

        // Step 2: select a different item (menu remains open)
        system.selectShopItem(shopkeeper, 2, inv); // press 2 → energy drink
        assertTrue(shopkeeper.isShopMenuOpen(),
            "Menu must remain open after selecting an item (2nd E not yet pressed)");
        assertTrue(shopMenuBlocksMovement(shopkeeper),
            "Movement must remain blocked after selecting an item (menu still open)");

        // Step 3: confirm purchase (menu closes)
        system.interactWithNPC(shopkeeper, inv);
        assertFalse(shopMenuBlocksMovement(shopkeeper),
            "Movement must be unblocked once the menu closes after purchase confirmation");
    }

    // --- Movement blocked while menu open, unblocked when speech timer expires ---

    @Test
    void whenSpeechTimerExpires_shopMenuCloses_movementUnblocked() {
        // If the player does not respond within 10 s, the NPC's speech timer closes the menu.
        // Movement must be unblocked once the menu closes.
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open menu

        assertTrue(shopMenuBlocksMovement(shopkeeper),
            "Movement must be blocked while menu is open");

        // Simulate 10+ seconds: speech timer expires, NPC closes menu internally
        shopkeeper.update(10f);

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Precondition: speech timer must have closed the menu after 10 s");
        assertFalse(shopMenuBlocksMovement(shopkeeper),
            "After speech timer closes the menu, movement must be unblocked");
    }

    // --- Purchase confirmation (second E-press) still works ---

    @Test
    void whenShopMenuIsOpen_secondEPress_confirmsPurchaseAndClosesMenu() {
        // The E key is allowed through even when isUIBlocking() returns true (shop menu open),
        // so the second E-press can confirm the purchase and close the menu.
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // first E: open menu

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: menu must be open after first E-press");

        // Simulate the second E-press: allowed through by the special-case gate in
        // RagamuffinGame.handleUIInput() — "allow E while shop menu is open"
        system.interactWithNPC(shopkeeper, inv); // second E: confirm purchase

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Fix #557: second E-press must still be able to confirm the purchase " +
            "and close the shop menu even though isUIBlocking() now returns true during the transaction");
    }
}

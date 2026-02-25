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
 * Integration tests for Issue #555 — shop menu does not release mouse cursor while open.
 *
 * Fix: include {@code (activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen())}
 * in the {@code uiOpen} boolean in {@code RagamuffinGame.handleUIInput()} so that
 * {@code Gdx.input.setCursorCatched(!uiOpen)} releases the cursor during a shop transaction.
 *
 * These tests validate the condition logic that governs cursor release. They mirror
 * the {@code uiOpen} check in {@code RagamuffinGame.handleUIInput()}:
 *
 * <pre>
 *   boolean uiOpen = inventoryUI.isVisible() || helpUI.isVisible() || craftingUI.isVisible()
 *           || achievementsUI.isVisible() || questLogUI.isVisible()
 *           || (activeShopkeeperNPC != null &amp;&amp; activeShopkeeperNPC.isShopMenuOpen());
 *   Gdx.input.setCursorCatched(!uiOpen);
 * </pre>
 */
class Issue555ShopMenuCursorReleaseTest {

    /**
     * Mirrors the shop-menu portion of the {@code uiOpen} condition added by Fix #555.
     * Returns true when the cursor should be released (menu is open).
     */
    private static boolean shopMenuContributesToUiOpen(NPC activeShopkeeperNPC) {
        return activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen();
    }

    // --- Cursor released when shop menu opens ---

    @Test
    void whenShopMenuIsOpen_shopMenuContributesToUiOpen_isTrue() {
        // After first E-press, the shop menu is open.
        // The cursor should be released (uiOpen == true → setCursorCatched(false)).
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // first E-press: menu opens

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: isShopMenuOpen() must be true after first E-press");

        // Simulate the uiOpen condition from RagamuffinGame.handleUIInput()
        boolean shopContributes = shopMenuContributesToUiOpen(shopkeeper);

        assertTrue(shopContributes,
            "Fix #555: when shop menu is open, the shop-menu condition must contribute true " +
            "to uiOpen so that setCursorCatched(false) releases the cursor");
    }

    // --- Cursor recaptured when shop menu closes ---

    @Test
    void whenShopMenuIsClosed_afterPurchase_shopMenuContributesToUiOpen_isFalse() {
        // After second E-press (purchase confirmed), the shop menu closes.
        // The cursor should be recaptured (uiOpen == false → setCursorCatched(true)).
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open menu
        system.interactWithNPC(shopkeeper, inv); // confirm purchase → menu closes

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Precondition: isShopMenuOpen() must be false after second E-press (purchase)");

        boolean shopContributes = shopMenuContributesToUiOpen(shopkeeper);

        assertFalse(shopContributes,
            "Fix #555: after shop menu closes, the shop-menu condition must contribute false " +
            "to uiOpen so that setCursorCatched(true) recaptures the cursor");
    }

    @Test
    void whenNoActiveShopkeeper_shopMenuContributesToUiOpen_isFalse() {
        // When activeShopkeeperNPC is null (no shop interaction in progress),
        // the condition must not contribute to uiOpen.
        boolean shopContributes = shopMenuContributesToUiOpen(null);

        assertFalse(shopContributes,
            "Fix #555: when activeShopkeeperNPC is null, the shop-menu condition must be false " +
            "so that the cursor is not spuriously released");
    }

    // --- Cursor released for the full duration the menu is visible ---

    @Test
    void whileShopMenuIsOpen_cursorRemainsReleased_untilMenuCloses() {
        // The cursor should stay released throughout the shop interaction:
        // open → (cursor released) → select item → (cursor still released) → confirm
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();

        // Step 1: open menu
        system.interactWithNPC(shopkeeper, inv);
        assertTrue(shopMenuContributesToUiOpen(shopkeeper),
            "Cursor must be released immediately after menu opens");

        // Step 2: select a different item (menu remains open)
        system.selectShopItem(shopkeeper, 2, inv); // press 2 → energy drink
        assertTrue(shopkeeper.isShopMenuOpen(),
            "Menu must remain open after selecting an item (2nd E not yet pressed)");
        assertTrue(shopMenuContributesToUiOpen(shopkeeper),
            "Cursor must remain released after selecting an item (menu still open)");

        // Step 3: confirm purchase (menu closes)
        system.interactWithNPC(shopkeeper, inv);
        assertFalse(shopMenuContributesToUiOpen(shopkeeper),
            "Cursor must be recaptured once the menu closes after purchase confirmation");
    }

    // --- Timer-based menu expiry also releases cursor then recaptures it ---

    @Test
    void whenSpeechTimerExpires_shopMenuCloses_shopMenuContributesToUiOpen_becomesFalse() {
        // If the player does not respond within 10 s, the NPC's speech timer closes the menu.
        // The cursor condition must reflect the closed state.
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open menu

        assertTrue(shopMenuContributesToUiOpen(shopkeeper),
            "Cursor must be released while menu is open");

        // Simulate 10+ seconds: speech timer expires, NPC closes menu internally
        shopkeeper.update(10f);

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Precondition: speech timer must have closed the menu after 10 s");
        assertFalse(shopMenuContributesToUiOpen(shopkeeper),
            "After speech timer closes the menu, shopMenuContributesToUiOpen must be false " +
            "so the cursor is recaptured");
    }
}

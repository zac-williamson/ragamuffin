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
 * Integration tests for Issue #541 — shop menu 1/2/3 key selection wiring.
 *
 * The game loop in RagamuffinGame must intercept hotbar key presses 1/2/3
 * (slots 0/1/2) when a shopkeeper's shop menu is open and route them to
 * InteractionSystem.selectShopItem() instead of changing the hotbar.
 *
 * These tests validate the routing logic independently of the full game loop:
 * - activeShopkeeperNPC field is set after a shop menu opens
 * - selectShopItem(npc, index, inv) correctly updates selected item
 * - Hotbar keys 1/2/3 map to shop indices 1/2/3 (slot+1)
 * - After the menu closes, the NPC's isShopMenuOpen() returns false
 */
class Issue541ShopMenuKeyRoutingTest {

    // --- activeShopkeeperNPC tracking ---

    @Test
    void afterFirstEPress_shopMenuIsOpen_trackingIsRequired() {
        // Simulate what RagamuffinGame.handleInteraction() does after first E-press:
        // it checks targetNPC.isShopMenuOpen() to decide whether to set activeShopkeeperNPC.
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        system.interactWithNPC(shopkeeper, inv); // first E-press opens menu

        // After first E-press, the NPC's shop menu must be open so that
        // RagamuffinGame can store it as activeShopkeeperNPC.
        assertTrue(shopkeeper.isShopMenuOpen(),
            "After first E-press, isShopMenuOpen() must be true so the game loop can set activeShopkeeperNPC");
    }

    @Test
    void afterSecondEPress_shopMenuIsClosed_trackingClears() {
        // Simulate what RagamuffinGame.handleInteraction() does after second E-press:
        // isShopMenuOpen() returns false, so activeShopkeeperNPC is cleared to null.
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        system.interactWithNPC(shopkeeper, inv); // open menu
        system.interactWithNPC(shopkeeper, inv); // confirm purchase (closes menu)

        // After second E-press, isShopMenuOpen() must be false so the game loop
        // can clear activeShopkeeperNPC, restoring normal hotbar behaviour.
        assertFalse(shopkeeper.isShopMenuOpen(),
            "After second E-press, isShopMenuOpen() must be false so the game loop clears activeShopkeeperNPC");
    }

    // --- Key routing: pressing 1/2/3 while menu is open ---

    @Test
    void hotbarSlot0_mapsToShopIndex1_sausageRoll() {
        // In RagamuffinGame: hotbarSlot 0 (key "1") → selectShopItem(npc, 1, inv)
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        system.interactWithNPC(shopkeeper, inv); // open menu (default = 1)
        system.selectShopItem(shopkeeper, 2, inv); // change to 2 first
        system.selectShopItem(shopkeeper, 1, inv); // press "1" → slot 0+1 = index 1

        assertEquals(1, shopkeeper.getSelectedShopItem(),
            "Pressing key 1 (hotbar slot 0) should select shop item index 1 (sausage roll)");
    }

    @Test
    void hotbarSlot1_mapsToShopIndex2_energyDrink() {
        // In RagamuffinGame: hotbarSlot 1 (key "2") → selectShopItem(npc, 2, inv)
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        system.interactWithNPC(shopkeeper, inv); // open menu
        system.selectShopItem(shopkeeper, 1 + 1, inv); // slot 1 → index 2

        assertEquals(2, shopkeeper.getSelectedShopItem(),
            "Pressing key 2 (hotbar slot 1) should select shop item index 2 (energy drink)");
    }

    @Test
    void hotbarSlot2_mapsToShopIndex3_crisps() {
        // In RagamuffinGame: hotbarSlot 2 (key "3") → selectShopItem(npc, 3, inv)
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        system.interactWithNPC(shopkeeper, inv); // open menu
        system.selectShopItem(shopkeeper, 2 + 1, inv); // slot 2 → index 3

        assertEquals(3, shopkeeper.getSelectedShopItem(),
            "Pressing key 3 (hotbar slot 2) should select shop item index 3 (crisps)");
    }

    // --- Pressing E after selection buys the correct item ---

    @Test
    void selectEnergyDrink_thenE_buysEnergyDrinkNotSausageRoll() {
        // Full flow: open menu, press 2 (slot 1 → index 2), press E to confirm
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 3);

        system.interactWithNPC(shopkeeper, inv); // open menu
        system.selectShopItem(shopkeeper, 2, inv); // press 2 → energy drink
        system.interactWithNPC(shopkeeper, inv);   // E to buy

        assertEquals(1, inv.getItemCount(Material.ENERGY_DRINK),
            "After selecting item 2 and pressing E, player should receive energy drink");
        assertEquals(0, inv.getItemCount(Material.SAUSAGE_ROLL),
            "Sausage roll must NOT be purchased when energy drink (item 2) is selected");
    }

    @Test
    void selectCrisps_thenE_buysCrispsNotSausageRoll() {
        // Full flow: open menu, press 3 (slot 2 → index 3), press E to confirm
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 2);
        inv.addItem(Material.PENNY, 10);

        system.interactWithNPC(shopkeeper, inv); // open menu
        system.selectShopItem(shopkeeper, 3, inv); // press 3 → crisps
        system.interactWithNPC(shopkeeper, inv);   // E to buy

        assertEquals(1, inv.getItemCount(Material.CRISPS),
            "After selecting item 3 and pressing E, player should receive crisps");
        assertEquals(0, inv.getItemCount(Material.SAUSAGE_ROLL),
            "Sausage roll must NOT be purchased when crisps (item 3) are selected");
    }

    // --- Guard: selectShopItem is a no-op when menu is not open ---

    @Test
    void selectShopItem_whenMenuNotOpen_doesNothing() {
        // The guard in RagamuffinGame: only intercept if activeShopkeeperNPC.isShopMenuOpen()
        // This mirrors InteractionSystem.selectShopItem() returning null when menu is closed.
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);

        // Don't open menu — simulate pressing 2 while no shop menu is active
        String result = system.selectShopItem(shopkeeper, 2, inv);

        assertNull(result,
            "selectShopItem should return null when shop menu is not open (menu not active)");
        assertEquals(1, shopkeeper.getSelectedShopItem(),
            "Selected item should remain at default (1) when menu is not open");
    }
}

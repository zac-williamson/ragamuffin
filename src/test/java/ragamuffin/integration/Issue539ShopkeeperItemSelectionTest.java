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
 * Integration tests for Issue #539 — shopkeeper item selection.
 * After the first E-press opens the shop menu, the player should be able to press
 * 1/2/3 to highlight an item, then E to confirm the purchase of THAT item.
 * The second E-press must NOT auto-select the most-expensive affordable item.
 */
class Issue539ShopkeeperItemSelectionTest {

    // --- Default selection ---

    @Test
    void defaultSelection_isSausageRoll() {
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);

        // Before the menu opens the field is irrelevant, but after opening it should be 1
        InteractionSystem system = new InteractionSystem();
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        system.interactWithNPC(shopkeeper, inv); // open menu

        assertEquals(1, shopkeeper.getSelectedShopItem(),
            "Default selection after opening menu should be 1 (sausage roll)");
    }

    @Test
    void defaultSelection_buysExpensiveItem_whenAffordable() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 3);

        system.interactWithNPC(shopkeeper, inv); // open menu (default = sausage roll)
        system.interactWithNPC(shopkeeper, inv); // buy

        assertEquals(1, inv.getItemCount(Material.SAUSAGE_ROLL),
            "Default selection (1) should buy a sausage roll");
        assertEquals(0, inv.getItemCount(Material.ENERGY_DRINK),
            "Should not buy energy drink when sausage roll is selected");
    }

    // --- Player selects item 2 (energy drink) ---

    @Test
    void pressingTwo_selectsEnergyDrink() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        system.interactWithNPC(shopkeeper, inv); // open menu
        system.selectShopItem(shopkeeper, 2, inv); // press 2

        assertEquals(2, shopkeeper.getSelectedShopItem(),
            "Pressing 2 should select energy drink (index 2)");
    }

    @Test
    void pressingTwo_thenE_buysEnergyDrink() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 3); // can afford both sausage roll and energy drink

        system.interactWithNPC(shopkeeper, inv); // open menu
        system.selectShopItem(shopkeeper, 2, inv); // select energy drink
        system.interactWithNPC(shopkeeper, inv); // confirm purchase

        assertEquals(0, inv.getItemCount(Material.SAUSAGE_ROLL),
            "Should NOT buy sausage roll when energy drink is selected");
        assertEquals(1, inv.getItemCount(Material.ENERGY_DRINK),
            "Should buy energy drink (item 2) when it is selected");
        assertEquals(2, inv.getItemCount(Material.SHILLING),
            "Should deduct 1 shilling for energy drink (3 - 1 = 2)");
    }

    // --- Player selects item 3 (crisps) ---

    @Test
    void pressingThree_selectsCrisps() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 2);

        system.interactWithNPC(shopkeeper, inv); // open menu
        system.selectShopItem(shopkeeper, 3, inv); // press 3

        assertEquals(3, shopkeeper.getSelectedShopItem(),
            "Pressing 3 should select crisps (index 3)");
    }

    @Test
    void pressingThree_thenE_buysCrisps() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 2);
        inv.addItem(Material.PENNY, 10);

        system.interactWithNPC(shopkeeper, inv); // open menu
        system.selectShopItem(shopkeeper, 3, inv); // select crisps
        system.interactWithNPC(shopkeeper, inv); // confirm purchase

        assertEquals(1, inv.getItemCount(Material.CRISPS),
            "Should buy crisps when item 3 is selected");
        assertEquals(0, inv.getItemCount(Material.SAUSAGE_ROLL),
            "Should NOT buy sausage roll when crisps are selected");
        assertEquals(4, inv.getItemCount(Material.PENNY),
            "Should deduct 6 pennies for crisps (10 - 6 = 4)");
    }

    // --- Player selects item 1 explicitly (overriding a previous selection) ---

    @Test
    void canChangSelection_fromTwoToOne() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        system.interactWithNPC(shopkeeper, inv); // open menu
        system.selectShopItem(shopkeeper, 2, inv); // select energy drink
        system.selectShopItem(shopkeeper, 1, inv); // change back to sausage roll
        system.interactWithNPC(shopkeeper, inv);   // confirm

        assertEquals(1, inv.getItemCount(Material.SAUSAGE_ROLL),
            "Should buy sausage roll after switching selection back to 1");
        assertEquals(0, inv.getItemCount(Material.ENERGY_DRINK));
    }

    // --- Speech bubble reflects current selection ---

    @Test
    void selectShopItem_returnsSpeechWithSelectedItem() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 3);

        system.interactWithNPC(shopkeeper, inv); // open menu
        String dialogue = system.selectShopItem(shopkeeper, 2, inv);

        assertNotNull(dialogue,
            "selectShopItem should return updated dialogue");
        assertTrue(dialogue.contains("> ") && dialogue.contains("energy drink"),
            "Updated dialogue should mark energy drink as selected, got: " + dialogue);
    }

    @Test
    void selectShopItem_updatesNpcSpeechBubble() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 3);

        system.interactWithNPC(shopkeeper, inv); // open menu
        system.selectShopItem(shopkeeper, 3, inv);

        String bubble = shopkeeper.getSpeechText();
        assertNotNull(bubble, "NPC speech bubble should be updated after selectShopItem");
        assertTrue(bubble.contains("crisps"),
            "Updated speech bubble should mention crisps, got: " + bubble);
    }

    // --- selectShopItem is a no-op when menu is closed ---

    @Test
    void selectShopItem_whenMenuClosed_returnsNull() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 3);

        // Menu is not open yet
        String result = system.selectShopItem(shopkeeper, 2, inv);

        assertNull(result,
            "selectShopItem should return null when shop menu is not open");
    }

    // --- Selection resets when menu re-opens ---

    @Test
    void selectionResets_whenMenuReopens() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 6);

        // Cycle 1: open, select 2, buy energy drink
        system.interactWithNPC(shopkeeper, inv); // open menu
        system.selectShopItem(shopkeeper, 2, inv);
        system.interactWithNPC(shopkeeper, inv); // buy energy drink (1s)

        // Cycle 2: reopen — selection should default back to 1
        system.interactWithNPC(shopkeeper, inv); // open menu again
        assertEquals(1, shopkeeper.getSelectedShopItem(),
            "Selection should reset to 1 when the shop menu reopens");
    }

    // --- Menu dialogue lists items with numbered keys ---

    @Test
    void firstEPress_dialogueContainsNumberedItems() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 3);

        String dialogue = system.interactWithNPC(shopkeeper, inv);

        assertNotNull(dialogue);
        assertTrue(dialogue.contains("1."), "Menu dialogue should have item 1");
        assertTrue(dialogue.contains("2."), "Menu dialogue should have item 2");
        assertTrue(dialogue.contains("3."), "Menu dialogue should have item 3");
        assertTrue(dialogue.contains("sausage roll"), "Menu should list sausage roll");
        assertTrue(dialogue.contains("energy drink"), "Menu should list energy drink");
        assertTrue(dialogue.contains("crisps"), "Menu should list crisps");
    }

    // --- Cannot afford selected item: shows error message, no purchase ---

    @Test
    void cannotAffordSelectedItem_showsErrorMessage() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.PENNY, 3); // only 3 pennies, crisps cost 6

        system.interactWithNPC(shopkeeper, inv); // open menu (has pennies)
        system.selectShopItem(shopkeeper, 3, inv); // select crisps
        String dialogue = system.interactWithNPC(shopkeeper, inv); // try to buy

        assertNotNull(dialogue, "Should return a dialogue even on failure");
        assertEquals(0, inv.getItemCount(Material.CRISPS),
            "Should not receive crisps with insufficient pennies");
        assertEquals(3, inv.getItemCount(Material.PENNY),
            "Pennies should be unchanged on failed purchase");
    }
}

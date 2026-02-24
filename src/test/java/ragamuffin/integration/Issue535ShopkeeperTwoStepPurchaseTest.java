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
 * Integration tests for Issue #535 — shopkeeper purchase must be two-step:
 * first E-press opens the shop menu, second E-press executes the purchase.
 * No currency should be deducted without explicit player confirmation.
 */
class Issue535ShopkeeperTwoStepPurchaseTest {

    // --- First E-press: menu opens, no purchase ---

    @Test
    void firstEPress_doesNotDeductCurrency() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        system.interactWithNPC(shopkeeper, inv);

        assertEquals(5, inv.getItemCount(Material.SHILLING),
            "First E-press must NOT deduct currency — player has not chosen an item yet");
        assertEquals(0, inv.getItemCount(Material.SAUSAGE_ROLL),
            "First E-press must NOT add any item to inventory");
    }

    @Test
    void firstEPress_opensShopMenu() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 2);

        system.interactWithNPC(shopkeeper, inv);

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Shop menu should be open after first E-press");
    }

    @Test
    void firstEPress_returnsMenuDialogue_listingItems() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 3);

        String dialogue = system.interactWithNPC(shopkeeper, inv);

        assertNotNull(dialogue, "First E-press must return a dialogue line (the shop menu)");
        // Menu dialogue should mention at least one purchasable item
        assertTrue(
            dialogue.contains("sausage roll") || dialogue.contains("energy drink") || dialogue.contains("crisps"),
            "Shop menu dialogue should list available items, got: " + dialogue
        );
    }

    // --- Second E-press: purchase executes ---

    @Test
    void secondEPress_deductsCurrencyAndAddsItem() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        system.interactWithNPC(shopkeeper, inv); // first press — opens menu
        system.interactWithNPC(shopkeeper, inv); // second press — executes purchase

        assertEquals(1, inv.getItemCount(Material.SAUSAGE_ROLL),
            "Sausage roll should be purchased on second E-press");
        assertEquals(3, inv.getItemCount(Material.SHILLING),
            "2 shillings should be deducted for sausage roll");
    }

    @Test
    void secondEPress_closesShopMenu() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 3);

        system.interactWithNPC(shopkeeper, inv); // open
        system.interactWithNPC(shopkeeper, inv); // purchase

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Shop menu should be closed after completing a purchase");
    }

    @Test
    void secondEPress_returnsPurchaseConfirmationDialogue() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 3);

        system.interactWithNPC(shopkeeper, inv); // open menu
        String dialogue = system.interactWithNPC(shopkeeper, inv); // purchase

        assertNotNull(dialogue, "Second E-press should return a purchase dialogue");
        assertFalse(dialogue.isEmpty());
    }

    // --- No currency: generic dialogue, no menu state ---

    @Test
    void noCurrency_firstEPress_doesNotOpenShopMenu() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);

        system.interactWithNPC(shopkeeper, inv);

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Shop menu should NOT open when player has no currency");
    }

    @Test
    void noCurrency_firstEPress_returnsGenericDialogue() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);

        String dialogue = system.interactWithNPC(shopkeeper, inv);

        assertNotNull(dialogue, "Shopkeeper should give generic dialogue when player has no currency");
    }

    // --- Shop menu expires when speech bubble times out ---

    @Test
    void shopMenuClosesWhenSpeechExpires() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 2);

        system.interactWithNPC(shopkeeper, inv); // open menu
        assertTrue(shopkeeper.isShopMenuOpen(), "Menu should be open after first E-press");

        // Simulate enough time passing for the speech bubble to expire
        shopkeeper.update(10f);

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Shop menu should close when speech bubble expires (player walked away)");
    }

    // --- Repeated presses cycle correctly: open → purchase → open → purchase ---

    @Test
    void repeatedPresses_cycleMenuAndPurchase() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 6);

        // Cycle 1
        system.interactWithNPC(shopkeeper, inv); // open
        system.interactWithNPC(shopkeeper, inv); // buy sausage roll (2s)
        assertEquals(1, inv.getItemCount(Material.SAUSAGE_ROLL));
        assertEquals(4, inv.getItemCount(Material.SHILLING));

        // Cycle 2
        system.interactWithNPC(shopkeeper, inv); // open again
        system.interactWithNPC(shopkeeper, inv); // buy second sausage roll (2s)
        assertEquals(2, inv.getItemCount(Material.SAUSAGE_ROLL));
        assertEquals(2, inv.getItemCount(Material.SHILLING));
    }
}

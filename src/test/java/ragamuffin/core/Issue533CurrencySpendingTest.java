package ragamuffin.core;

import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.BlockDropTable;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.world.BlockType;
import ragamuffin.world.LandmarkType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #533 — currency (shillings/pennies) spending mechanic.
 */
class Issue533CurrencySpendingTest {

    // --- canUseItem() ---

    @Test
    void shilling_canBeUsed() {
        InteractionSystem system = new InteractionSystem();
        assertTrue(system.canUseItem(Material.SHILLING),
            "SHILLING should be usable (right-click)");
    }

    @Test
    void penny_canBeUsed() {
        InteractionSystem system = new InteractionSystem();
        assertTrue(system.canUseItem(Material.PENNY),
            "PENNY should be usable (right-click)");
    }

    // --- useItem() tooltip messages ---

    @Test
    void shilling_useItem_returnsTooltip() {
        InteractionSystem system = new InteractionSystem();
        String msg = system.useItem(Material.SHILLING, null, null);
        assertNotNull(msg, "useItem(SHILLING) should return a tooltip message");
        assertFalse(msg.isEmpty());
    }

    @Test
    void penny_useItem_returnsTooltip() {
        InteractionSystem system = new InteractionSystem();
        String msg = system.useItem(Material.PENNY, null, null);
        assertNotNull(msg, "useItem(PENNY) should return a tooltip message");
        assertFalse(msg.isEmpty());
    }

    // --- Merchant purchase: buySausageRoll ---

    @Test
    void buySausageRoll_withEnoughShillings_succeeds() {
        InteractionSystem system = new InteractionSystem();
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 3);

        InteractionSystem.PurchaseResult result = system.buySausageRoll(inv);

        assertEquals(InteractionSystem.PurchaseResult.SUCCESS, result);
        assertEquals(1, inv.getItemCount(Material.SAUSAGE_ROLL),
            "Should have 1 sausage roll after purchase");
        assertEquals(1, inv.getItemCount(Material.SHILLING),
            "Should have 1 shilling remaining (3 - 2)");
    }

    @Test
    void buySausageRoll_insufficientShillings_fails() {
        InteractionSystem system = new InteractionSystem();
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 1); // Only 1, need 2

        InteractionSystem.PurchaseResult result = system.buySausageRoll(inv);

        assertEquals(InteractionSystem.PurchaseResult.INSUFFICIENT_FUNDS, result);
        assertEquals(0, inv.getItemCount(Material.SAUSAGE_ROLL),
            "Should not receive sausage roll with insufficient shillings");
        assertEquals(1, inv.getItemCount(Material.SHILLING),
            "Shillings should be unchanged on failed purchase");
    }

    @Test
    void buySausageRoll_noShillings_fails() {
        InteractionSystem system = new InteractionSystem();
        Inventory inv = new Inventory(36);

        InteractionSystem.PurchaseResult result = system.buySausageRoll(inv);

        assertEquals(InteractionSystem.PurchaseResult.INSUFFICIENT_FUNDS, result);
    }

    // --- Merchant purchase: buyEnergyDrink ---

    @Test
    void buyEnergyDrink_withEnoughShillings_succeeds() {
        InteractionSystem system = new InteractionSystem();
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 2);

        InteractionSystem.PurchaseResult result = system.buyEnergyDrink(inv);

        assertEquals(InteractionSystem.PurchaseResult.SUCCESS, result);
        assertEquals(1, inv.getItemCount(Material.ENERGY_DRINK));
        assertEquals(1, inv.getItemCount(Material.SHILLING), "Should have 1 shilling left");
    }

    @Test
    void buyEnergyDrink_noShillings_fails() {
        InteractionSystem system = new InteractionSystem();
        Inventory inv = new Inventory(36);

        InteractionSystem.PurchaseResult result = system.buyEnergyDrink(inv);

        assertEquals(InteractionSystem.PurchaseResult.INSUFFICIENT_FUNDS, result);
        assertEquals(0, inv.getItemCount(Material.ENERGY_DRINK));
    }

    // --- Merchant purchase: buyCrisps ---

    @Test
    void buyCrisps_withEnoughPennies_succeeds() {
        InteractionSystem system = new InteractionSystem();
        Inventory inv = new Inventory(36);
        inv.addItem(Material.PENNY, 10);

        InteractionSystem.PurchaseResult result = system.buyCrisps(inv);

        assertEquals(InteractionSystem.PurchaseResult.SUCCESS, result);
        assertEquals(1, inv.getItemCount(Material.CRISPS));
        assertEquals(4, inv.getItemCount(Material.PENNY), "Should have 4 pennies left");
    }

    @Test
    void buyCrisps_insufficientPennies_fails() {
        InteractionSystem system = new InteractionSystem();
        Inventory inv = new Inventory(36);
        inv.addItem(Material.PENNY, 3); // Only 3, need 6

        InteractionSystem.PurchaseResult result = system.buyCrisps(inv);

        assertEquals(InteractionSystem.PurchaseResult.INSUFFICIENT_FUNDS, result);
        assertEquals(0, inv.getItemCount(Material.CRISPS));
        assertEquals(3, inv.getItemCount(Material.PENNY));
    }

    // --- Shopkeeper NPC interaction with currency ---

    @Test
    void shopkeeperInteraction_withShillings_firstPressSHowsMenu_secondPressBuys() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);

        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        // First E-press: should open the shop menu, NOT purchase anything
        String firstDialogue = system.interactWithNPC(shopkeeper, inv);
        assertNotNull(firstDialogue, "First E-press should return shop menu dialogue");
        assertEquals(0, inv.getItemCount(Material.SAUSAGE_ROLL),
            "No purchase should happen on first E-press");
        assertEquals(5, inv.getItemCount(Material.SHILLING),
            "Shillings should be unchanged after first E-press");
        assertTrue(shopkeeper.isShopMenuOpen(), "Shop menu should be open after first E-press");

        // Second E-press: should complete the purchase
        String secondDialogue = system.interactWithNPC(shopkeeper, inv);
        assertNotNull(secondDialogue, "Second E-press should return purchase dialogue");
        assertEquals(1, inv.getItemCount(Material.SAUSAGE_ROLL),
            "Player should receive a sausage roll after second E-press");
        assertEquals(3, inv.getItemCount(Material.SHILLING),
            "Player should have 3 shillings remaining after purchase");
        assertFalse(shopkeeper.isShopMenuOpen(), "Shop menu should be closed after purchase");
    }

    @Test
    void shopkeeperInteraction_withPenniesOnly_twoStepBuysCrisps() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);

        Inventory inv = new Inventory(36);
        inv.addItem(Material.PENNY, 8);

        // First E-press: opens shop menu
        String firstDialogue = system.interactWithNPC(shopkeeper, inv);
        assertNotNull(firstDialogue);
        assertEquals(0, inv.getItemCount(Material.CRISPS),
            "No purchase on first E-press");
        assertEquals(8, inv.getItemCount(Material.PENNY),
            "Pennies unchanged after first E-press");

        // Player selects crisps (item 3) since they only have pennies
        system.selectShopItem(shopkeeper, 3, inv);
        assertEquals(3, shopkeeper.getSelectedShopItem(),
            "Selected item should be 3 (crisps) after pressing 3");

        // Second E-press: completes purchase for the selected item (crisps)
        String secondDialogue = system.interactWithNPC(shopkeeper, inv);
        assertNotNull(secondDialogue);
        assertEquals(1, inv.getItemCount(Material.CRISPS),
            "Player should receive crisps for pennies on second E-press");
        assertEquals(2, inv.getItemCount(Material.PENNY),
            "Player should have 2 pennies remaining");
    }

    @Test
    void shopkeeperInteraction_noCurrency_givesGenericDialogue() {
        InteractionSystem system = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);

        Inventory inv = new Inventory(36);

        String dialogue = system.interactWithNPC(shopkeeper, inv);

        assertNotNull(dialogue, "Shopkeeper should still give dialogue with no currency");
        // Should be generic shopkeeper dialogue — no purchase
        assertEquals(0, inv.getItemCount(Material.SAUSAGE_ROLL));
        assertEquals(0, inv.getItemCount(Material.ENERGY_DRINK));
        assertEquals(0, inv.getItemCount(Material.CRISPS));
    }

    // --- BlockDropTable: currency drops ---

    @Test
    void cashConverter_brickBlock_canDropCurrency() {
        BlockDropTable table = new BlockDropTable();
        boolean foundShilling = false;
        boolean foundPenny = false;
        // Run enough iterations to see currency drop (33% chance per block break)
        for (int i = 0; i < 500; i++) {
            Material drop = table.getDrop(BlockType.BRICK, LandmarkType.CASH_CONVERTER);
            if (drop == Material.SHILLING) foundShilling = true;
            if (drop == Material.PENNY) foundPenny = true;
        }
        assertTrue(foundShilling || foundPenny,
            "CASH_CONVERTER should drop SHILLING or PENNY from BRICK blocks");
    }

    @Test
    void pawnShop_brickBlock_canDropCurrency() {
        BlockDropTable table = new BlockDropTable();
        boolean foundCurrency = false;
        for (int i = 0; i < 500; i++) {
            Material drop = table.getDrop(BlockType.BRICK, LandmarkType.PAWN_SHOP);
            if (drop == Material.SHILLING || drop == Material.PENNY) {
                foundCurrency = true;
                break;
            }
        }
        assertTrue(foundCurrency,
            "PAWN_SHOP should sometimes drop SHILLING or PENNY from BRICK blocks");
    }

    @Test
    void cashConverter_glassBlock_canDropCurrency() {
        BlockDropTable table = new BlockDropTable();
        boolean foundCurrency = false;
        for (int i = 0; i < 500; i++) {
            Material drop = table.getDrop(BlockType.GLASS, LandmarkType.CASH_CONVERTER);
            if (drop == Material.SHILLING || drop == Material.PENNY) {
                foundCurrency = true;
                break;
            }
        }
        assertTrue(foundCurrency,
            "CASH_CONVERTER should sometimes drop currency from GLASS blocks");
    }

    // --- Purchase message polling ---

    @Test
    void purchaseMessage_isSetOnSuccess() {
        InteractionSystem system = new InteractionSystem();
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 2);

        system.buySausageRoll(inv);
        String msg = system.pollLastPurchaseMessage();

        assertNotNull(msg, "A purchase message should be set after a successful buy");
        assertFalse(msg.isEmpty());
    }

    @Test
    void purchaseMessage_isSetOnFailure() {
        InteractionSystem system = new InteractionSystem();
        Inventory inv = new Inventory(36);

        system.buySausageRoll(inv);
        String msg = system.pollLastPurchaseMessage();

        assertNotNull(msg, "A purchase message should be set even on failed buy");
        assertFalse(msg.isEmpty());
    }

    @Test
    void purchaseMessage_isNullAfterPoll() {
        InteractionSystem system = new InteractionSystem();
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 2);

        system.buySausageRoll(inv);
        system.pollLastPurchaseMessage(); // consume it
        String msg = system.pollLastPurchaseMessage();

        assertNull(msg, "pollLastPurchaseMessage() should return null after message consumed");
    }
}

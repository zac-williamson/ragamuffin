package ragamuffin.core;

import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.world.LandmarkType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #501 — currency system using shillings and pennies.
 */
class Issue501CurrencySystemTest {

    // --- Material enum ---

    @Test
    void shilling_existsInMaterialEnum() {
        assertNotNull(Material.SHILLING);
        assertEquals("Shilling", Material.SHILLING.getDisplayName());
    }

    @Test
    void penny_existsInMaterialEnum() {
        assertNotNull(Material.PENNY);
        assertEquals("Penny", Material.PENNY.getDisplayName());
    }

    @Test
    void shilling_isNotABlockItem() {
        assertFalse(Material.SHILLING.isBlockItem());
    }

    @Test
    void penny_isNotABlockItem() {
        assertFalse(Material.PENNY.isBlockItem());
    }

    @Test
    void shilling_hasIconColors() {
        assertNotNull(Material.SHILLING.getIconColors());
        assertTrue(Material.SHILLING.getIconColors().length >= 1);
    }

    @Test
    void penny_hasIconColors() {
        assertNotNull(Material.PENNY.getIconColors());
        assertTrue(Material.PENNY.getIconColors().length >= 1);
    }

    @Test
    void shilling_hasIconShape() {
        assertNotNull(Material.SHILLING.getIconShape());
    }

    @Test
    void penny_hasIconShape() {
        assertNotNull(Material.PENNY.getIconShape());
    }

    // --- Inventory operations ---

    @Test
    void shillings_canBeAddedToInventory() {
        Inventory inv = new Inventory(36);
        assertTrue(inv.addItem(Material.SHILLING, 3));
        assertEquals(3, inv.getItemCount(Material.SHILLING));
    }

    @Test
    void pennies_canBeAddedToInventory() {
        Inventory inv = new Inventory(36);
        assertTrue(inv.addItem(Material.PENNY, 10));
        assertEquals(10, inv.getItemCount(Material.PENNY));
    }

    @Test
    void shillings_canBeRemovedFromInventory() {
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);
        assertTrue(inv.removeItem(Material.SHILLING, 2));
        assertEquals(3, inv.getItemCount(Material.SHILLING));
    }

    @Test
    void pennies_stackInInventory() {
        Inventory inv = new Inventory(36);
        inv.addItem(Material.PENNY, 6);
        inv.addItem(Material.PENNY, 4);
        assertEquals(10, inv.getItemCount(Material.PENNY));
    }

    // --- Quest rewards ---

    @Test
    void jeweller_questRewardsShillings() {
        BuildingQuestRegistry registry = new BuildingQuestRegistry();
        Quest quest = registry.getQuest(LandmarkType.JEWELLER);
        assertNotNull(quest);
        assertEquals(Material.SHILLING, quest.getReward());
        assertTrue(quest.getRewardCount() > 0);
    }

    @Test
    void pawnShop_questRewardsShillings() {
        BuildingQuestRegistry registry = new BuildingQuestRegistry();
        Quest quest = registry.getQuest(LandmarkType.PAWN_SHOP);
        assertNotNull(quest);
        assertEquals(Material.SHILLING, quest.getReward());
        assertTrue(quest.getRewardCount() > 0);
    }

    @Test
    void newsagent_questRewardsPennies() {
        BuildingQuestRegistry registry = new BuildingQuestRegistry();
        Quest quest = registry.getQuest(LandmarkType.NEWSAGENT);
        assertNotNull(quest);
        assertEquals(Material.PENNY, quest.getReward());
        assertTrue(quest.getRewardCount() > 0);
    }

    @Test
    void cashConverter_questRewardsPennies() {
        BuildingQuestRegistry registry = new BuildingQuestRegistry();
        Quest quest = registry.getQuest(LandmarkType.CASH_CONVERTER);
        assertNotNull(quest);
        assertEquals(Material.PENNY, quest.getReward());
        assertTrue(quest.getRewardCount() > 0);
    }

    // --- Quest completion grants currency ---

    @Test
    void completingJewellerQuest_grantsShillingsToInventory() {
        BuildingQuestRegistry registry = new BuildingQuestRegistry();
        Quest quest = registry.getQuest(LandmarkType.JEWELLER);
        assertNotNull(quest);

        Inventory inv = new Inventory(36);
        inv.addItem(Material.DIAMOND, quest.getRequiredCount());

        int shillingsBefore = inv.getItemCount(Material.SHILLING);
        assertTrue(quest.complete(inv));
        int shillingsAfter = inv.getItemCount(Material.SHILLING);

        assertTrue(shillingsAfter > shillingsBefore,
            "Completing jeweller quest should add shillings to inventory");
        assertEquals(0, inv.getItemCount(Material.DIAMOND),
            "Diamond should be consumed on quest completion");
    }

    @Test
    void completingCashConverterQuest_grantsPenniesToInventory() {
        BuildingQuestRegistry registry = new BuildingQuestRegistry();
        Quest quest = registry.getQuest(LandmarkType.CASH_CONVERTER);
        assertNotNull(quest);

        Inventory inv = new Inventory(36);
        inv.addItem(Material.DODGY_DVD, quest.getRequiredCount());

        int penniesBefore = inv.getItemCount(Material.PENNY);
        assertTrue(quest.complete(inv));
        int penniesAfter = inv.getItemCount(Material.PENNY);

        assertTrue(penniesAfter > penniesBefore,
            "Completing cash converter quest should add pennies to inventory");
    }
}

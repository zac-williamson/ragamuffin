package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.InteractionSystem;
import ragamuffin.entity.Player;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #257: Non-consumable loot items silently fail when right-clicked.
 * Verifies that all non-food, non-placeable items now have a meaningful right-click action
 * (useItem) instead of silently failing.
 */
class Issue257NonConsumableLootTest {

    private Player player;
    private Inventory inventory;
    private InteractionSystem interactionSystem;

    @BeforeEach
    void setUp() {
        player = new Player(0, 1, 0);
        inventory = new Inventory(36);
        interactionSystem = new InteractionSystem();
    }

    // === canUseItem() coverage ===

    @Test
    void testNewspaperCanBeUsed() {
        assertTrue(interactionSystem.canUseItem(Material.NEWSPAPER),
            "NEWSPAPER should have a use action");
    }

    @Test
    void testTextbookCanBeUsed() {
        assertTrue(interactionSystem.canUseItem(Material.TEXTBOOK),
            "TEXTBOOK should have a use action");
    }

    @Test
    void testHymnBookCanBeUsed() {
        assertTrue(interactionSystem.canUseItem(Material.HYMN_BOOK),
            "HYMN_BOOK should have a use action");
    }

    @Test
    void testHairClippersCanBeUsed() {
        assertTrue(interactionSystem.canUseItem(Material.HAIR_CLIPPERS),
            "HAIR_CLIPPERS should have a use action");
    }

    @Test
    void testNailPolishCanBeUsed() {
        assertTrue(interactionSystem.canUseItem(Material.NAIL_POLISH),
            "NAIL_POLISH should have a use action");
    }

    @Test
    void testBrokenPhoneCanBeUsed() {
        assertTrue(interactionSystem.canUseItem(Material.BROKEN_PHONE),
            "BROKEN_PHONE should have a use action");
    }

    @Test
    void testDodgyDvdCanBeUsed() {
        assertTrue(interactionSystem.canUseItem(Material.DODGY_DVD),
            "DODGY_DVD should have a use action");
    }

    @Test
    void testPlywoodCanBeUsed() {
        assertTrue(interactionSystem.canUseItem(Material.PLYWOOD),
            "PLYWOOD should have a use action");
    }

    @Test
    void testPipeCanBeUsed() {
        assertTrue(interactionSystem.canUseItem(Material.PIPE),
            "PIPE should have a use action");
    }

    @Test
    void testComputerCanBeUsed() {
        assertTrue(interactionSystem.canUseItem(Material.COMPUTER),
            "COMPUTER should have a use action");
    }

    @Test
    void testOfficeChairCanBeUsed() {
        assertTrue(interactionSystem.canUseItem(Material.OFFICE_CHAIR),
            "OFFICE_CHAIR should have a use action");
    }

    @Test
    void testStaplerCanBeUsed() {
        assertTrue(interactionSystem.canUseItem(Material.STAPLER),
            "STAPLER should have a use action");
    }

    @Test
    void testPetrolCanCanBeUsed() {
        assertTrue(interactionSystem.canUseItem(Material.PETROL_CAN),
            "PETROL_CAN should have a use action");
    }

    @Test
    void testDiamondCanBeUsed() {
        assertTrue(interactionSystem.canUseItem(Material.DIAMOND),
            "DIAMOND should have a use action");
    }

    // === canUseItem() negative cases — block/food items should NOT match ===

    @Test
    void testBlockItemsCannotBeUsedViaUseItem() {
        assertFalse(interactionSystem.canUseItem(Material.WOOD),
            "WOOD (block item) should not have a use action — it is placed");
        assertFalse(interactionSystem.canUseItem(Material.BRICK),
            "BRICK (block item) should not have a use action — it is placed");
        assertFalse(interactionSystem.canUseItem(Material.STONE),
            "STONE (block item) should not have a use action — it is placed");
    }

    @Test
    void testFoodItemsCannotBeUsedViaUseItem() {
        assertFalse(interactionSystem.canUseItem(Material.SAUSAGE_ROLL),
            "SAUSAGE_ROLL (food) should not have a use action — it is consumed via isFood()");
        assertFalse(interactionSystem.canUseItem(Material.ENERGY_DRINK),
            "ENERGY_DRINK (food) should not have a use action — it is consumed via isFood()");
    }

    // === useItem() effects ===

    @Test
    void testReadingNewspaperRestoresEnergy() {
        player.setEnergy(50);
        float energyBefore = player.getEnergy();
        inventory.addItem(Material.NEWSPAPER, 1);

        String message = interactionSystem.useItem(Material.NEWSPAPER, player, inventory);

        assertNotNull(message, "NEWSPAPER use should return a tooltip message");
        assertFalse(message.isEmpty(), "NEWSPAPER tooltip message should not be empty");
        assertTrue(player.getEnergy() > energyBefore,
            "Reading NEWSPAPER should restore some energy");
        // Item IS consumed by useItem (fix #291)
        assertEquals(0, inventory.getItemCount(Material.NEWSPAPER),
            "NEWSPAPER should be removed from inventory by useItem()");
    }

    @Test
    void testReadingTextbookRestoresEnergy() {
        player.setEnergy(50);
        float energyBefore = player.getEnergy();
        inventory.addItem(Material.TEXTBOOK, 1);

        String message = interactionSystem.useItem(Material.TEXTBOOK, player, inventory);

        assertNotNull(message, "TEXTBOOK use should return a tooltip message");
        assertTrue(player.getEnergy() > energyBefore,
            "Reading TEXTBOOK should restore some energy");
    }

    @Test
    void testReadingHymnBookRestoresEnergy() {
        player.setEnergy(50);
        float energyBefore = player.getEnergy();
        inventory.addItem(Material.HYMN_BOOK, 1);

        String message = interactionSystem.useItem(Material.HYMN_BOOK, player, inventory);

        assertNotNull(message, "HYMN_BOOK use should return a tooltip message");
        assertTrue(player.getEnergy() > energyBefore,
            "Reading HYMN_BOOK should restore some energy");
    }

    @Test
    void testDiamondUseShowsTooltip() {
        String message = interactionSystem.useItem(Material.DIAMOND, player, inventory);
        assertNotNull(message, "DIAMOND use should return a tooltip message");
        assertFalse(message.isEmpty(), "DIAMOND tooltip message should not be empty");
    }

    @Test
    void testPetrolCanUseShowsTooltip() {
        String message = interactionSystem.useItem(Material.PETROL_CAN, player, inventory);
        assertNotNull(message, "PETROL_CAN use should return a tooltip message");
        assertFalse(message.isEmpty(), "PETROL_CAN tooltip message should not be empty");
    }

    @Test
    void testBrokenPhoneUseShowsTooltip() {
        String message = interactionSystem.useItem(Material.BROKEN_PHONE, player, inventory);
        assertNotNull(message, "BROKEN_PHONE use should return a tooltip message");
        assertFalse(message.isEmpty(), "BROKEN_PHONE tooltip message should not be empty");
    }

    @Test
    void testComputerUseShowsTooltip() {
        String message = interactionSystem.useItem(Material.COMPUTER, player, inventory);
        assertNotNull(message, "COMPUTER use should return a tooltip message");
        assertFalse(message.isEmpty(), "COMPUTER tooltip message should not be empty");
    }

    @Test
    void testAllUsableItemsReturnNonNullMessage() {
        // Exhaustive: every item declared as canUseItem() must return a non-null message
        Material[] usableItems = {
            Material.NEWSPAPER, Material.TEXTBOOK, Material.HYMN_BOOK,
            Material.HAIR_CLIPPERS, Material.NAIL_POLISH, Material.BROKEN_PHONE,
            Material.DODGY_DVD, Material.PLYWOOD, Material.PIPE,
            Material.COMPUTER, Material.OFFICE_CHAIR, Material.STAPLER,
            Material.PETROL_CAN, Material.DIAMOND
        };

        for (Material m : usableItems) {
            assertTrue(interactionSystem.canUseItem(m),
                m + " should be recognised by canUseItem()");
            String msg = interactionSystem.useItem(m, player, inventory);
            assertNotNull(msg, m + " should return a non-null tooltip from useItem()");
            assertFalse(msg.isEmpty(), m + " tooltip message should not be empty");
        }
    }

    @Test
    void testNonUsableItemsReturnNullMessage() {
        // Items that are block-placeable or food should return null from useItem()
        assertNull(interactionSystem.useItem(Material.WOOD, player, inventory));
        assertNull(interactionSystem.useItem(Material.BRICK, player, inventory));
        assertNull(interactionSystem.useItem(Material.SAUSAGE_ROLL, player, inventory));
    }
}

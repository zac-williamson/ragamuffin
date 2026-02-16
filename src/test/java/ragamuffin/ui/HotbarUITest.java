package ragamuffin.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;

import static org.junit.jupiter.api.Assertions.*;

class HotbarUITest {

    private HotbarUI hotbarUI;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new Inventory(36);
        hotbarUI = new HotbarUI(inventory);
    }

    @Test
    void testInitialSelection() {
        assertEquals(0, hotbarUI.getSelectedSlot());
    }

    @Test
    void testSelectSlot() {
        hotbarUI.selectSlot(3);
        assertEquals(3, hotbarUI.getSelectedSlot());
    }

    @Test
    void testSelectSlot_Clamped() {
        hotbarUI.selectSlot(15); // Out of bounds
        assertEquals(8, hotbarUI.getSelectedSlot()); // Should clamp to max

        hotbarUI.selectSlot(-5);
        assertEquals(0, hotbarUI.getSelectedSlot()); // Should clamp to min
    }

    @Test
    void testGetSelectedItem_Empty() {
        assertNull(hotbarUI.getSelectedItem());
    }

    @Test
    void testGetSelectedItem_WithItem() {
        inventory.addItem(Material.WOOD, 5); // Goes to slot 0
        assertEquals(Material.WOOD, hotbarUI.getSelectedItem());
    }

    @Test
    void testGetSelectedItem_DifferentSlot() {
        inventory.addItem(Material.WOOD, 1); // Slot 0
        inventory.addItem(Material.BRICK, 1); // Slot 1

        hotbarUI.selectSlot(0);
        assertEquals(Material.WOOD, hotbarUI.getSelectedItem());

        hotbarUI.selectSlot(1);
        assertEquals(Material.BRICK, hotbarUI.getSelectedItem());
    }

    @Test
    void testGetInventory() {
        assertSame(inventory, hotbarUI.getInventory());
    }

    @Test
    void testHotbarSize() {
        assertEquals(9, HotbarUI.HOTBAR_SLOTS);
    }

    @Test
    void testSlotNavigation() {
        // Test navigating through all slots
        for (int i = 0; i < 9; i++) {
            hotbarUI.selectSlot(i);
            assertEquals(i, hotbarUI.getSelectedSlot());
        }
    }
}

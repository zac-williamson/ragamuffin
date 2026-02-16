package ragamuffin.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;

import static org.junit.jupiter.api.Assertions.*;

class InventoryUITest {

    private InventoryUI inventoryUI;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new Inventory(36);
        inventoryUI = new InventoryUI(inventory);
    }

    @Test
    void testInitiallyNotVisible() {
        assertFalse(inventoryUI.isVisible());
    }

    @Test
    void testToggleVisibility() {
        inventoryUI.toggle();
        assertTrue(inventoryUI.isVisible());

        inventoryUI.toggle();
        assertFalse(inventoryUI.isVisible());
    }

    @Test
    void testShow() {
        inventoryUI.show();
        assertTrue(inventoryUI.isVisible());
    }

    @Test
    void testHide() {
        inventoryUI.show();
        inventoryUI.hide();
        assertFalse(inventoryUI.isVisible());
    }

    @Test
    void testGetInventory() {
        assertSame(inventory, inventoryUI.getInventory());
    }

    @Test
    void testInventoryReflectsChanges() {
        inventory.addItem(Material.WOOD, 5);
        inventory.addItem(Material.BRICK, 3);

        assertEquals(5, inventoryUI.getInventory().getItemCount(Material.WOOD));
        assertEquals(3, inventoryUI.getInventory().getItemCount(Material.BRICK));
    }
}

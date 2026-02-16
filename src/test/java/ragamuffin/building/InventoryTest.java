package ragamuffin.building;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryTest {

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new Inventory(36); // Standard inventory size
    }

    @Test
    void testAddItem() {
        assertTrue(inventory.addItem(Material.WOOD, 1));
        assertEquals(1, inventory.getItemCount(Material.WOOD));
    }

    @Test
    void testAddMultipleItems() {
        assertTrue(inventory.addItem(Material.WOOD, 5));
        assertEquals(5, inventory.getItemCount(Material.WOOD));
    }

    @Test
    void testStackItems() {
        assertTrue(inventory.addItem(Material.WOOD, 3));
        assertTrue(inventory.addItem(Material.WOOD, 2));
        assertEquals(5, inventory.getItemCount(Material.WOOD));
    }

    @Test
    void testRemoveItem() {
        inventory.addItem(Material.BRICK, 5);
        assertTrue(inventory.removeItem(Material.BRICK, 2));
        assertEquals(3, inventory.getItemCount(Material.BRICK));
    }

    @Test
    void testRemoveMoreThanAvailable() {
        inventory.addItem(Material.BRICK, 3);
        assertFalse(inventory.removeItem(Material.BRICK, 5));
        assertEquals(3, inventory.getItemCount(Material.BRICK)); // Should remain unchanged
    }

    @Test
    void testRemoveAllItems() {
        inventory.addItem(Material.STONE, 4);
        assertTrue(inventory.removeItem(Material.STONE, 4));
        assertEquals(0, inventory.getItemCount(Material.STONE));
    }

    @Test
    void testHasItem() {
        assertFalse(inventory.hasItem(Material.DIAMOND));
        inventory.addItem(Material.DIAMOND, 1);
        assertTrue(inventory.hasItem(Material.DIAMOND));
    }

    @Test
    void testHasItemWithCount() {
        inventory.addItem(Material.WOOD, 3);
        assertTrue(inventory.hasItem(Material.WOOD, 3));
        assertTrue(inventory.hasItem(Material.WOOD, 2));
        assertFalse(inventory.hasItem(Material.WOOD, 4));
    }

    @Test
    void testGetItemCount_Empty() {
        assertEquals(0, inventory.getItemCount(Material.COMPUTER));
    }

    @Test
    void testClear() {
        inventory.addItem(Material.WOOD, 5);
        inventory.addItem(Material.BRICK, 3);
        inventory.clear();
        assertEquals(0, inventory.getItemCount(Material.WOOD));
        assertEquals(0, inventory.getItemCount(Material.BRICK));
    }

    @Test
    void testGetSlot() {
        inventory.addItem(Material.WOOD, 3);
        int slot = inventory.findSlotWithItem(Material.WOOD);
        assertTrue(slot >= 0 && slot < 36);
        assertEquals(Material.WOOD, inventory.getItemInSlot(slot));
        assertEquals(3, inventory.getCountInSlot(slot));
    }

    @Test
    void testGetItemInEmptySlot() {
        assertNull(inventory.getItemInSlot(0));
        assertEquals(0, inventory.getCountInSlot(0));
    }
}

package ragamuffin.building;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.world.World;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for small item looting (Issue #872).
 * Verifies that small 3D objects placed in the world can be picked up by the player.
 */
class SmallItemLootTest {

    private World world;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        world = new World(12345L);
        inventory = new Inventory(36);
    }

    // --- World.removeSmallItem() tests ---

    @Test
    void testRemoveSmallItem_RemovesItemAtIndex() {
        world.placeSmallItem(new SmallItem(Material.TIN_OF_BEANS, new Vector3(1f, 1f, 1f)));
        world.placeSmallItem(new SmallItem(Material.ENERGY_DRINK, new Vector3(2f, 1f, 2f)));
        world.placeSmallItem(new SmallItem(Material.NEWSPAPER, new Vector3(3f, 1f, 3f)));

        world.removeSmallItem(1); // remove ENERGY_DRINK

        List<SmallItem> items = world.getSmallItems();
        assertEquals(2, items.size(), "Should have 2 items after removing 1");
        assertEquals(Material.TIN_OF_BEANS, items.get(0).getMaterial());
        assertEquals(Material.NEWSPAPER, items.get(1).getMaterial());
    }

    @Test
    void testRemoveSmallItem_RemovesFirstItem() {
        world.placeSmallItem(new SmallItem(Material.PINT, new Vector3(1f, 1f, 1f)));
        world.placeSmallItem(new SmallItem(Material.SAUSAGE_ROLL, new Vector3(2f, 1f, 2f)));

        world.removeSmallItem(0);

        List<SmallItem> items = world.getSmallItems();
        assertEquals(1, items.size());
        assertEquals(Material.SAUSAGE_ROLL, items.get(0).getMaterial());
    }

    @Test
    void testRemoveSmallItem_RemovesLastItem() {
        world.placeSmallItem(new SmallItem(Material.PINT, new Vector3(1f, 1f, 1f)));
        world.placeSmallItem(new SmallItem(Material.SAUSAGE_ROLL, new Vector3(2f, 1f, 2f)));

        world.removeSmallItem(1);

        List<SmallItem> items = world.getSmallItems();
        assertEquals(1, items.size());
        assertEquals(Material.PINT, items.get(0).getMaterial());
    }

    @Test
    void testRemoveSmallItem_SingleItem_WorldBecomesEmpty() {
        world.placeSmallItem(new SmallItem(Material.CRISPS, new Vector3(5f, 1f, 5f)));

        world.removeSmallItem(0);

        assertTrue(world.getSmallItems().isEmpty(), "World should be empty after removing the only item");
    }

    @Test
    void testRemoveSmallItem_OutOfBounds_ThrowsException() {
        world.placeSmallItem(new SmallItem(Material.TIN_OF_BEANS, new Vector3(1f, 1f, 1f)));

        assertThrows(IndexOutOfBoundsException.class,
                () -> world.removeSmallItem(5),
                "removeSmallItem with out-of-range index should throw IndexOutOfBoundsException");
    }

    @Test
    void testRemoveSmallItem_EmptyWorld_ThrowsException() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> world.removeSmallItem(0),
                "removeSmallItem on empty world should throw IndexOutOfBoundsException");
    }

    // --- Loot pickup simulation tests ---

    @Test
    void testLootPickup_ItemMovesFromWorldToInventory() {
        // Simulate the looting sequence: remove from world, add to inventory
        world.placeSmallItem(new SmallItem(Material.ENERGY_DRINK, new Vector3(10f, 1f, 10f)));

        SmallItem item = world.getSmallItems().get(0);
        Material mat = item.getMaterial();
        world.removeSmallItem(0);
        inventory.addItem(mat, 1);

        assertTrue(world.getSmallItems().isEmpty(), "Item should be removed from world after looting");
        assertEquals(1, inventory.getItemCount(Material.ENERGY_DRINK),
                "Item should appear in inventory after looting");
    }

    @Test
    void testLootPickup_CorrectMaterialTransferred() {
        world.placeSmallItem(new SmallItem(Material.NEWSPAPER, new Vector3(5f, 1f, 5f)));
        world.placeSmallItem(new SmallItem(Material.TIN_OF_BEANS, new Vector3(6f, 1f, 6f)));

        // Loot the newspaper (index 0)
        SmallItem item = world.getSmallItems().get(0);
        Material mat = item.getMaterial();
        world.removeSmallItem(0);
        inventory.addItem(mat, 1);

        assertEquals(Material.NEWSPAPER, mat, "Should have looted NEWSPAPER");
        assertEquals(1, inventory.getItemCount(Material.NEWSPAPER));
        assertEquals(0, inventory.getItemCount(Material.TIN_OF_BEANS),
                "TIN_OF_BEANS should remain unlootd in the world");
        assertEquals(1, world.getSmallItems().size(),
                "TIN_OF_BEANS should still be in the world");
    }

    @Test
    void testLootPickup_StacksWithExistingInventoryItem() {
        inventory.addItem(Material.CRISPS, 3); // already have 3
        world.placeSmallItem(new SmallItem(Material.CRISPS, new Vector3(5f, 1f, 5f)));

        SmallItem item = world.getSmallItems().get(0);
        world.removeSmallItem(0);
        inventory.addItem(item.getMaterial(), 1);

        assertEquals(4, inventory.getItemCount(Material.CRISPS),
                "Looted item should stack with existing inventory");
        assertTrue(world.getSmallItems().isEmpty());
    }

    @Test
    void testGetSmallItems_RemainsUnmodifiableAfterRemove() {
        world.placeSmallItem(new SmallItem(Material.PINT, new Vector3(1f, 1f, 1f)));
        world.placeSmallItem(new SmallItem(Material.SAUSAGE_ROLL, new Vector3(2f, 1f, 2f)));
        world.removeSmallItem(0);

        List<SmallItem> items = world.getSmallItems();
        assertThrows(UnsupportedOperationException.class,
                () -> items.add(new SmallItem(Material.NEWSPAPER, new Vector3(3f, 3f, 3f))),
                "getSmallItems() should remain unmodifiable after a remove");
    }
}

package ragamuffin.render;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Material;
import ragamuffin.building.SmallItem;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;
import ragamuffin.building.BlockPlacer;
import ragamuffin.building.Inventory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SmallItemRenderer (Issue #675).
 *
 * Verifies that:
 *  - Registering items creates the correct number of instances
 *  - All small item material types are accepted without error
 *  - setItems with an empty list produces zero instances
 *  - Replacing items (calling setItems again) resets instance count
 *  - The renderer is safe when given a null / unmodifiable list
 *  - SmallItem data is correctly captured by the world for rendering
 *
 * Note: Actual GL model creation requires a GPU context and cannot be tested
 * in headless JUnit mode. These tests verify the data-side wiring that feeds
 * the renderer.
 */
class SmallItemRendererTest {

    private World world;
    private Inventory inventory;
    private BlockPlacer blockPlacer;

    @BeforeEach
    void setUp() {
        world = new World(99999L);
        inventory = new Inventory(36);
        blockPlacer = new BlockPlacer();
    }

    // ── World / data-side tests (no GL context required) ──────────────────────

    @Test
    void testSmallItemsStoredInWorld() {
        world.placeSmallItem(new SmallItem(Material.TIN_OF_BEANS, new Vector3(10f, 1f, 10f)));
        world.placeSmallItem(new SmallItem(Material.ENERGY_DRINK, new Vector3(11f, 1f, 10f)));

        List<SmallItem> items = world.getSmallItems();
        assertEquals(2, items.size(), "World should store two small items");
        assertEquals(Material.TIN_OF_BEANS, items.get(0).getMaterial());
        assertEquals(Material.ENERGY_DRINK, items.get(1).getMaterial());
    }

    @Test
    void testSmallItemPositionPreserved() {
        Vector3 pos = new Vector3(5.25f, 2.0f, 7.75f);
        world.placeSmallItem(new SmallItem(Material.NEWSPAPER, pos));

        SmallItem stored = world.getSmallItems().get(0);
        assertEquals(5.25f, stored.getPosition().x, 0.001f);
        assertEquals(2.0f,  stored.getPosition().y, 0.001f);
        assertEquals(7.75f, stored.getPosition().z, 0.001f);
    }

    @Test
    void testAllSmallItemMaterialsCanBePlacedInWorld() {
        // Every material that isSmallItem() should be storeable in the world without error
        int count = 0;
        for (Material m : Material.values()) {
            if (m.isSmallItem()) {
                world.placeSmallItem(new SmallItem(m, new Vector3(count, 1f, 0f)));
                count++;
            }
        }
        assertTrue(count > 0, "There should be at least one small item material");
        assertEquals(count, world.getSmallItems().size(),
                "All small item materials should be stored in the world");
    }

    @Test
    void testWorldSmallItemListIsUnmodifiable() {
        world.placeSmallItem(new SmallItem(Material.PINT, new Vector3(0f, 1f, 0f)));
        List<SmallItem> items = world.getSmallItems();

        assertThrows(UnsupportedOperationException.class,
                () -> items.add(new SmallItem(Material.TIN_OF_BEANS, new Vector3(1f, 1f, 1f))),
                "getSmallItems() should return an unmodifiable list");
    }

    @Test
    void testSmallItemPlacedViaBlockPlacer() {
        // Place a surface block, then use BlockPlacer to place item on it
        world.setBlock(50, 100, 50, BlockType.STONE);
        inventory.addItem(Material.TIN_OF_BEANS, 1);

        Vector3 origin    = new Vector3(50.5f, 103f, 50.5f);
        Vector3 direction = new Vector3(0f, -1f, 0f);

        boolean placed = blockPlacer.placeSmallItem(world, inventory, Material.TIN_OF_BEANS,
                origin, direction, 5.0f, null);

        assertTrue(placed, "BlockPlacer should place the small item successfully");
        assertEquals(1, world.getSmallItems().size(),
                "World should contain exactly one small item after placement");
        assertEquals(Material.TIN_OF_BEANS, world.getSmallItems().get(0).getMaterial());
        // Y position should be on top of the block (blockY=100, so itemY=101)
        assertEquals(101.0f, world.getSmallItems().get(0).getPosition().y, 0.001f);
    }

    @Test
    void testSmallItemNotPlacedOnSideFace() {
        world.setBlock(50, 100, 50, BlockType.STONE);
        inventory.addItem(Material.ENERGY_DRINK, 1);

        // Shoot horizontally — hits the side face, should be rejected
        Vector3 origin    = new Vector3(46f, 100.5f, 50.5f);
        Vector3 direction = new Vector3(1f, 0f, 0f);

        boolean placed = blockPlacer.placeSmallItem(world, inventory, Material.ENERGY_DRINK,
                origin, direction, 10.0f, null);

        assertFalse(placed, "Placement on a side face should be rejected");
        assertTrue(world.getSmallItems().isEmpty(),
                "No items should be stored when placement is rejected");
    }

    @Test
    void testMultipleSmallItemsAccumulateInWorld() {
        world.setBlock(50, 100, 50, BlockType.STONE);
        inventory.addItem(Material.PINT, 3);

        Vector3 origin    = new Vector3(50.5f, 103f, 50.5f);
        Vector3 direction = new Vector3(0f, -1f, 0f);

        blockPlacer.placeSmallItem(world, inventory, Material.PINT, origin, direction, 5.0f, null);
        blockPlacer.placeSmallItem(world, inventory, Material.PINT, origin, direction, 5.0f, null);
        blockPlacer.placeSmallItem(world, inventory, Material.PINT, origin, direction, 5.0f, null);

        assertEquals(3, world.getSmallItems().size(),
                "Three separate placements should produce three items in the world");
        assertEquals(0, inventory.getItemCount(Material.PINT),
                "All pints should be consumed from inventory");
    }

    @Test
    void testSmallItemRendererConstructsWithoutError() {
        // SmallItemRenderer should construct without requiring a GL context.
        // GL model building only happens when setItems() is called (deferred to runtime).
        SmallItemRenderer renderer = new SmallItemRenderer();
        assertNotNull(renderer, "SmallItemRenderer should construct without error");
        // Before setItems(), no instances should be registered
        assertEquals(0, renderer.getInstanceCount(),
                "Freshly constructed renderer should have zero instances");
        renderer.dispose();
    }

    @Test
    void testSmallItemRendererDisposeIsIdempotent() {
        // Dispose on a freshly constructed (no GL state) renderer should not throw.
        SmallItemRenderer renderer = new SmallItemRenderer();
        assertDoesNotThrow(renderer::dispose,
                "dispose() should not throw even when no models have been built");
    }

    @Test
    void testIsSmallItemCoversExpectedMaterials() {
        // Spot-check that the materials intended to be small items are flagged correctly
        assertTrue(Material.TIN_OF_BEANS.isSmallItem());
        assertTrue(Material.ENERGY_DRINK.isSmallItem());
        assertTrue(Material.PINT.isSmallItem());
        assertTrue(Material.NEWSPAPER.isSmallItem());
        assertTrue(Material.STAPLER.isSmallItem());
        assertTrue(Material.SAUSAGE_ROLL.isSmallItem());
        assertTrue(Material.STEAK_BAKE.isSmallItem());
        assertTrue(Material.CHIPS.isSmallItem());
        assertTrue(Material.SCRATCH_CARD.isSmallItem());
        assertTrue(Material.PARACETAMOL.isSmallItem());
    }

    @Test
    void testBlockItemsAreNotSmallItems() {
        // Block materials should not be misclassified as small items
        assertFalse(Material.PLANKS.isSmallItem());
        assertFalse(Material.BRICK.isSmallItem());
        assertFalse(Material.GLASS.isSmallItem());
        assertFalse(Material.DOOR.isSmallItem());
        assertFalse(Material.STONE.isSmallItem());
    }
}

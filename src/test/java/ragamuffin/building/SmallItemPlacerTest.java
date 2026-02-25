package ragamuffin.building;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.entity.AABB;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for small item placement (Issue #645).
 * Small items are placed on block surfaces without grid snapping.
 */
class SmallItemPlacerTest {

    private BlockPlacer blockPlacer;
    private World world;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        blockPlacer = new BlockPlacer();
        world = new World(12345L);
        inventory = new Inventory(36);
    }

    // --- isSmallItem() tests ---

    @Test
    void testIsSmallItem_SmallItemMaterials() {
        assertTrue(Material.TIN_OF_BEANS.isSmallItem(), "TIN_OF_BEANS should be a small item");
        assertTrue(Material.ENERGY_DRINK.isSmallItem(), "ENERGY_DRINK should be a small item");
        assertTrue(Material.NEWSPAPER.isSmallItem(), "NEWSPAPER should be a small item");
        assertTrue(Material.STAPLER.isSmallItem(), "STAPLER should be a small item");
        assertTrue(Material.PINT.isSmallItem(), "PINT should be a small item");
        assertTrue(Material.SAUSAGE_ROLL.isSmallItem(), "SAUSAGE_ROLL should be a small item");
    }

    @Test
    void testIsSmallItem_NonSmallItemMaterials() {
        assertFalse(Material.PLANKS.isSmallItem(), "PLANKS should not be a small item");
        assertFalse(Material.BRICK.isSmallItem(), "BRICK should not be a small item");
        assertFalse(Material.GLASS.isSmallItem(), "GLASS should not be a small item");
        assertFalse(Material.DOOR.isSmallItem(), "DOOR should not be a small item");
        assertFalse(Material.IMPROVISED_TOOL.isSmallItem(), "IMPROVISED_TOOL should not be a small item");
        assertFalse(Material.DIAMOND.isSmallItem(), "DIAMOND should not be a small item");
    }

    // --- placeSmallItem() tests ---

    @Test
    void testPlaceSmallItem_SuccessOnTopFace() {
        // Block at high altitude to avoid generated terrain
        world.setBlock(50, 100, 50, BlockType.GRASS);
        inventory.addItem(Material.TIN_OF_BEANS, 1);

        // Look straight down from above — hits the top face of the block
        Vector3 origin = new Vector3(50.5f, 103f, 50.5f);
        Vector3 direction = new Vector3(0, -1, 0);

        boolean placed = blockPlacer.placeSmallItem(world, inventory, Material.TIN_OF_BEANS,
                origin, direction, 5.0f, null);

        assertTrue(placed, "Small item should be placed successfully on top face");
        assertEquals(0, inventory.getItemCount(Material.TIN_OF_BEANS),
                "Item should be consumed from inventory");

        List<SmallItem> items = world.getSmallItems();
        assertEquals(1, items.size(), "World should contain exactly one small item");

        SmallItem item = items.get(0);
        assertEquals(Material.TIN_OF_BEANS, item.getMaterial());
        // Y position should be exactly on top of the block (blockY + 1 = 101)
        assertEquals(101.0f, item.getPosition().y, 0.001f,
                "Small item Y should be at block top surface");
        // X and Z should be within the block's footprint
        assertTrue(item.getPosition().x >= 50.05f && item.getPosition().x <= 50.95f,
                "Small item X should be within block bounds");
        assertTrue(item.getPosition().z >= 50.05f && item.getPosition().z <= 50.95f,
                "Small item Z should be within block bounds");
    }

    @Test
    void testPlaceSmallItem_NoGridSnapping() {
        // Verify the placed position is NOT grid-snapped by looking at an offset position
        world.setBlock(50, 100, 50, BlockType.STONE);
        inventory.addItem(Material.NEWSPAPER, 1);

        // Look at the block from a slight angle so the hit X/Z isn't exactly 50.5
        Vector3 origin = new Vector3(50.3f, 103f, 50.7f);
        Vector3 direction = new Vector3(0, -1, 0);

        boolean placed = blockPlacer.placeSmallItem(world, inventory, Material.NEWSPAPER,
                origin, direction, 5.0f, null);

        assertTrue(placed);

        SmallItem item = world.getSmallItems().get(0);
        // The X coordinate should reflect the actual hit X (not snapped to 50 or 51)
        assertNotEquals(50.0f, item.getPosition().x, 0.01f,
                "Small item X should not be grid-snapped to block origin");
        assertNotEquals(51.0f, item.getPosition().x, 0.01f,
                "Small item X should not be grid-snapped to next block boundary");
    }

    @Test
    void testPlaceSmallItem_NoItemInInventory() {
        world.setBlock(50, 100, 50, BlockType.GRASS);
        // Do NOT add item to inventory

        Vector3 origin = new Vector3(50.5f, 103f, 50.5f);
        Vector3 direction = new Vector3(0, -1, 0);

        boolean placed = blockPlacer.placeSmallItem(world, inventory, Material.TIN_OF_BEANS,
                origin, direction, 5.0f, null);

        assertFalse(placed, "Placement should fail when item is not in inventory");
        assertTrue(world.getSmallItems().isEmpty(), "No items should be placed");
    }

    @Test
    void testPlaceSmallItem_NoBlockInRange() {
        // No block below — ray hits nothing
        inventory.addItem(Material.TIN_OF_BEANS, 1);

        Vector3 origin = new Vector3(50.5f, 103f, 50.5f);
        Vector3 direction = new Vector3(0, 1, 0); // Looking up — no block

        boolean placed = blockPlacer.placeSmallItem(world, inventory, Material.TIN_OF_BEANS,
                origin, direction, 5.0f, null);

        assertFalse(placed, "Placement should fail when no block is in range");
        assertTrue(world.getSmallItems().isEmpty());
        assertEquals(1, inventory.getItemCount(Material.TIN_OF_BEANS),
                "Item should not be consumed when placement fails");
    }

    @Test
    void testPlaceSmallItem_SideFaceRejected() {
        // Block at high altitude; hit from the side
        world.setBlock(50, 100, 50, BlockType.STONE);
        inventory.addItem(Material.ENERGY_DRINK, 1);

        // Shoot horizontally at the side face of the block
        Vector3 origin = new Vector3(46f, 100.5f, 50.5f);
        Vector3 direction = new Vector3(1, 0, 0); // Side face

        boolean placed = blockPlacer.placeSmallItem(world, inventory, Material.ENERGY_DRINK,
                origin, direction, 10.0f, null);

        assertFalse(placed, "Small items should only be placeable on the top face, not side faces");
        assertTrue(world.getSmallItems().isEmpty());
    }

    @Test
    void testPlaceSmallItem_NonSmallItemMaterialRejected() {
        world.setBlock(50, 100, 50, BlockType.GRASS);
        inventory.addItem(Material.PLANKS, 1);

        Vector3 origin = new Vector3(50.5f, 103f, 50.5f);
        Vector3 direction = new Vector3(0, -1, 0);

        boolean placed = blockPlacer.placeSmallItem(world, inventory, Material.PLANKS,
                origin, direction, 5.0f, null);

        assertFalse(placed, "PLANKS is not a small item and should be rejected by placeSmallItem");
        assertTrue(world.getSmallItems().isEmpty());
    }

    @Test
    void testPlaceSmallItem_MultipleItemsAccumulate() {
        world.setBlock(50, 100, 50, BlockType.STONE);
        inventory.addItem(Material.TIN_OF_BEANS, 2);

        Vector3 origin = new Vector3(50.5f, 103f, 50.5f);
        Vector3 direction = new Vector3(0, -1, 0);

        blockPlacer.placeSmallItem(world, inventory, Material.TIN_OF_BEANS, origin, direction, 5.0f, null);
        blockPlacer.placeSmallItem(world, inventory, Material.TIN_OF_BEANS, origin, direction, 5.0f, null);

        assertEquals(2, world.getSmallItems().size(),
                "Multiple small items can be placed in the same world");
        assertEquals(0, inventory.getItemCount(Material.TIN_OF_BEANS));
    }

    @Test
    void testPlaceSmallItem_NullMaterialRejected() {
        world.setBlock(50, 100, 50, BlockType.GRASS);

        Vector3 origin = new Vector3(50.5f, 103f, 50.5f);
        Vector3 direction = new Vector3(0, -1, 0);

        boolean placed = blockPlacer.placeSmallItem(world, inventory, null,
                origin, direction, 5.0f, null);

        assertFalse(placed, "null material should be rejected");
        assertTrue(world.getSmallItems().isEmpty());
    }

    // --- SmallItem class tests ---

    @Test
    void testSmallItem_StoresPositionAndMaterial() {
        Vector3 pos = new Vector3(10.3f, 5.0f, 7.8f);
        SmallItem item = new SmallItem(Material.STAPLER, pos);

        assertEquals(Material.STAPLER, item.getMaterial());
        assertEquals(10.3f, item.getPosition().x, 0.001f);
        assertEquals(5.0f, item.getPosition().y, 0.001f);
        assertEquals(7.8f, item.getPosition().z, 0.001f);
    }

    @Test
    void testSmallItem_PositionIsCopied() {
        Vector3 pos = new Vector3(1f, 2f, 3f);
        SmallItem item = new SmallItem(Material.PINT, pos);

        // Mutating the original vector should not affect the stored position
        pos.set(99f, 99f, 99f);
        assertEquals(1f, item.getPosition().x, 0.001f, "SmallItem should store a copy of the position");
    }

    // --- World small item storage tests ---

    @Test
    void testWorld_GetSmallItemsIsUnmodifiable() {
        world.placeSmallItem(new SmallItem(Material.NEWSPAPER, new Vector3(1f, 1f, 1f)));
        List<SmallItem> items = world.getSmallItems();

        assertThrows(UnsupportedOperationException.class, () -> items.add(
                new SmallItem(Material.NEWSPAPER, new Vector3(2f, 2f, 2f))),
                "getSmallItems() should return an unmodifiable list");
    }
}

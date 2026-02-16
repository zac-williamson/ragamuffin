package ragamuffin.building;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;
import ragamuffin.world.RaycastResult;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BlockPlacer.
 */
class BlockPlacerTest {

    private BlockPlacer blockPlacer;
    private World world;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        blockPlacer = new BlockPlacer();
        world = new World(12345L);
        inventory = new Inventory(36);
    }

    @Test
    void testGetPlacementPosition_ValidAdjacent() {
        // Place a ground block
        world.setBlock(10, 0, 10, BlockType.GRASS);

        Vector3 cameraPos = new Vector3(10, 2, 12);
        Vector3 direction = new Vector3(0, -1, -1).nor();

        Vector3 placement = blockPlacer.getPlacementPosition(world, cameraPos, direction, 5.0f);
        assertNotNull(placement);

        // Should place on top of the ground block or adjacent
        assertTrue(placement.y >= 0);
    }

    @Test
    void testGetPlacementPosition_NoBlockInRange() {
        Vector3 cameraPos = new Vector3(10, 2, 12);
        Vector3 direction = new Vector3(0, 1, 0); // Looking up at empty space

        Vector3 placement = blockPlacer.getPlacementPosition(world, cameraPos, direction, 5.0f);
        assertNull(placement);
    }

    @Test
    void testPlaceBlock_Success() {
        world.setBlock(10, 0, 10, BlockType.GRASS);
        inventory.addItem(Material.PLANKS, 1);

        Vector3 cameraPos = new Vector3(10, 2, 12);
        Vector3 direction = new Vector3(0, -1, -1).nor();

        boolean placed = blockPlacer.placeBlock(world, inventory, Material.PLANKS, cameraPos, direction, 5.0f);
        assertTrue(placed);

        assertEquals(0, inventory.getItemCount(Material.PLANKS));
    }

    @Test
    void testPlaceBlock_NoMaterialInInventory() {
        world.setBlock(10, 0, 10, BlockType.GRASS);

        Vector3 cameraPos = new Vector3(10, 2, 12);
        Vector3 direction = new Vector3(0, -1, -1).nor();

        boolean placed = blockPlacer.placeBlock(world, inventory, Material.PLANKS, cameraPos, direction, 5.0f);
        assertFalse(placed);
    }

    @Test
    void testPlaceBlock_InvalidPlacement() {
        inventory.addItem(Material.PLANKS, 1);

        Vector3 cameraPos = new Vector3(10, 2, 12);
        Vector3 direction = new Vector3(0, 1, 0); // Looking at empty space

        boolean placed = blockPlacer.placeBlock(world, inventory, Material.PLANKS, cameraPos, direction, 5.0f);
        assertFalse(placed);

        assertEquals(1, inventory.getItemCount(Material.PLANKS));
    }

    @Test
    void testMaterialToBlockType() {
        assertEquals(BlockType.WOOD, blockPlacer.materialToBlockType(Material.PLANKS));
        assertEquals(BlockType.BRICK, blockPlacer.materialToBlockType(Material.SHELTER_WALL));
        assertEquals(BlockType.BRICK, blockPlacer.materialToBlockType(Material.BRICK_WALL));
        assertEquals(BlockType.GLASS, blockPlacer.materialToBlockType(Material.WINDOW));
    }
}

package ragamuffin.building;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.entity.AABB;
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
    void testPlaceBlock_BlockedByPlayerAABB() {
        // Use a high-altitude block to avoid generated terrain interference
        // Place a ground block at y=60 (above terrain)
        world.setBlock(50, 60, 50, BlockType.GRASS);
        inventory.addItem(Material.PLANKS, 1);

        // Player standing on the block at (50, 61, 50)
        // Player dimensions: width=0.6, height=1.8, depth=0.6
        Vector3 playerPos = new Vector3(50, 61, 50);
        AABB playerAABB = new AABB(playerPos, 0.6f, 1.8f, 0.6f);

        // Look straight down — raycast hits the grass block at y=60,
        // placement goes on top at y=61 which overlaps the player
        Vector3 cameraPos = new Vector3(50, 62.62f, 50); // eye height above feet
        Vector3 direction = new Vector3(0, -1, 0);

        boolean placed = blockPlacer.placeBlock(world, inventory, Material.PLANKS,
                cameraPos, direction, 5.0f, playerAABB);
        assertFalse(placed, "Should not place block inside player AABB");

        // Inventory should not have been consumed
        assertEquals(1, inventory.getItemCount(Material.PLANKS));
    }

    @Test
    void testPlaceBlock_AllowedAwayFromPlayer() {
        // Use high-altitude block to avoid generated terrain interference
        world.setBlock(50, 60, 55, BlockType.GRASS);
        inventory.addItem(Material.PLANKS, 1);

        // Player far from target block
        Vector3 playerPos = new Vector3(50, 61, 50);
        AABB playerAABB = new AABB(playerPos, 0.6f, 1.8f, 0.6f);

        // Look towards the block at z=55, place on top at y=61
        // Player is at z=50, target is at z=55 — well outside AABB
        Vector3 cameraPos = new Vector3(50, 62.62f, 50);
        Vector3 direction = new Vector3(0, -0.3f, 1).nor();

        boolean placed = blockPlacer.placeBlock(world, inventory, Material.PLANKS,
                cameraPos, direction, 5.0f, playerAABB);
        assertTrue(placed, "Should allow placement away from player");
    }

    @Test
    void testMaterialToBlockType() {
        assertEquals(BlockType.WOOD, blockPlacer.materialToBlockType(Material.PLANKS));
        assertEquals(BlockType.BRICK, blockPlacer.materialToBlockType(Material.SHELTER_WALL));
        assertEquals(BlockType.BRICK, blockPlacer.materialToBlockType(Material.BRICK_WALL));
        assertEquals(BlockType.GLASS, blockPlacer.materialToBlockType(Material.WINDOW));
    }
}

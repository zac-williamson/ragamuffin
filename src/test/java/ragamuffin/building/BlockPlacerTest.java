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
        // Use very high altitude to avoid generated terrain interference
        world.setBlock(50, 500, 50, BlockType.GRASS);
        inventory.addItem(Material.PLANKS, 1);

        // Player standing on the block at (50, 501, 50)
        Vector3 playerPos = new Vector3(50, 501, 50);
        AABB playerAABB = new AABB(playerPos, 0.6f, 1.8f, 0.6f);

        // Look slightly down at the block — hit top face, placement at y=501
        // Camera offset slightly so raycast hits near centre of top face
        Vector3 cameraPos = new Vector3(50.5f, 502.62f, 50.5f);
        Vector3 direction = new Vector3(0, -1, 0);

        boolean placed = blockPlacer.placeBlock(world, inventory, Material.PLANKS,
                cameraPos, direction, 5.0f, playerAABB);
        assertFalse(placed, "Should not place block inside player AABB");

        // Inventory should not have been consumed
        assertEquals(1, inventory.getItemCount(Material.PLANKS));
    }

    @Test
    void testPlaceBlock_AllowedAwayFromPlayer() {
        // Use very high altitude to avoid generated terrain interference
        world.setBlock(50, 500, 53, BlockType.GRASS);
        inventory.addItem(Material.PLANKS, 1);

        // Player far from target block
        Vector3 playerPos = new Vector3(50, 501, 50);
        AABB playerAABB = new AABB(playerPos, 0.6f, 1.8f, 0.6f);

        // Look towards the block at z=53, place on top at y=501
        // Player is at z=50, target is at z=53 — well outside AABB
        Vector3 cameraPos = new Vector3(50, 502.62f, 50);
        Vector3 direction = new Vector3(0, -0.5f, 1).nor();

        boolean placed = blockPlacer.placeBlock(world, inventory, Material.PLANKS,
                cameraPos, direction, 6.0f, playerAABB);
        assertTrue(placed, "Should allow placement away from player");
    }

    @Test
    void testMaterialToBlockType() {
        // PLANKS places as WOOD_PLANKS (distinct from WOOD) to prevent infinite resource loop
        assertEquals(BlockType.WOOD_PLANKS, blockPlacer.materialToBlockType(Material.PLANKS));
        assertEquals(BlockType.BRICK, blockPlacer.materialToBlockType(Material.SHELTER_WALL));
        assertEquals(BlockType.BRICK, blockPlacer.materialToBlockType(Material.BRICK_WALL));
        assertEquals(BlockType.GLASS, blockPlacer.materialToBlockType(Material.WINDOW));
        // Fix #886: LADDER material must map to LADDER block so right-click placement works
        assertEquals(BlockType.LADDER, blockPlacer.materialToBlockType(Material.LADDER),
            "Material.LADDER must map to BlockType.LADDER so players can place ladders");
    }

    @Test
    void testPlaceDoor_PlacesTwoBlockDoor() {
        // Use high altitude to avoid generated terrain interference
        world.setBlock(50, 500, 50, BlockType.GRASS);
        // Target position for door is (50, 501, 50); (50, 502, 50) must be AIR
        inventory.addItem(Material.DOOR, 1);

        // Look straight down at the top of the ground block — places at y=501
        Vector3 cameraPos = new Vector3(50.5f, 503f, 50.5f);
        Vector3 direction = new Vector3(0, -1, 0);

        boolean placed = blockPlacer.placeBlock(world, inventory, Material.DOOR, cameraPos, direction, 5.0f);
        assertTrue(placed, "DOOR placement should succeed when both target and above are AIR");

        assertEquals(BlockType.DOOR_LOWER, world.getBlock(50, 501, 50), "Lower block should be DOOR_LOWER");
        assertEquals(BlockType.DOOR_UPPER, world.getBlock(50, 502, 50), "Upper block should be DOOR_UPPER");
        assertEquals(0, inventory.getItemCount(Material.DOOR), "DOOR should be consumed from inventory");
    }

    @Test
    void testPlaceDoor_FailsWhenAboveBlocked() {
        // Use high altitude to avoid generated terrain interference.
        // Place a wall block at (50, 500, 53) and aim at its side face so the
        // placement target lands at (50, 500, 52). Then put STONE at (50, 501, 52)
        // so the DOOR_UPPER slot is occupied.
        world.setBlock(50, 500, 53, BlockType.GRASS);
        world.setBlock(50, 501, 52, BlockType.STONE);
        inventory.addItem(Material.DOOR, 1);

        // Look from z=50 toward z=53, slightly downward, hit the front face of
        // the block and place at (50, 500, 52)
        Vector3 cameraPos = new Vector3(50.5f, 500.5f, 50.0f);
        Vector3 direction = new Vector3(0, 0, 1);

        boolean placed = blockPlacer.placeBlock(world, inventory, Material.DOOR, cameraPos, direction, 5.0f);
        assertFalse(placed, "DOOR placement should fail when block above target is not AIR");

        assertEquals(BlockType.AIR, world.getBlock(50, 500, 52), "Lower slot should remain AIR");
        assertEquals(1, inventory.getItemCount(Material.DOOR), "DOOR should not be consumed when placement fails");
    }
}

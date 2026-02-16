package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.*;
import ragamuffin.entity.Player;
import ragamuffin.ui.*;
import ragamuffin.world.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug Fix Integration Test - verifies gravity, terrain, and harvesting work together.
 */
class BugFixIntegrationTest {

    private World world;
    private Player player;
    private Inventory inventory;
    private BlockBreaker blockBreaker;
    private BlockDropTable dropTable;

    @BeforeEach
    void setUp() {
        world = new World(99999);
        inventory = new Inventory(36);
        blockBreaker = new BlockBreaker();
        dropTable = new BlockDropTable();
    }

    /**
     * Integration Test: playerSpawnsNearTreeAndHarvests
     *
     * Setup: Create a flat world (fill ground at y=0 with GRASS, y=-1 with DIRT).
     * Place a tree (TREE_TRUNK at (5,1,5), (5,2,5), (5,3,5) with LEAVES around top)
     * exactly 5 blocks in front of player spawn at (0,1,0).
     *
     * Action:
     * 1. Player spawns. Verify player Y position is at ground level (Y approximately 1.0,
     *    standing on the GRASS at y=0). NOT floating at y=5.
     * 2. Move player forward ~4 blocks toward the tree (simulate W key presses).
     *    Verify player is adjacent to tree (within 2 blocks of x=5,z=5).
     * 3. Player punches the tree trunk block at (5,1,5) five times.
     *
     * Expected:
     * - Player Y ~= 1.0 (gravity worked, landed on ground)
     * - After moving, player is near the tree
     * - After 5 punches, the TREE_TRUNK at (5,1,5) is replaced with AIR
     * - Player inventory contains 1 WOOD item
     */
    @Test
    void playerSpawnsNearTreeAndHarvests() {
        // Setup: Create flat world with ground layer
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                world.setBlock(x, -1, z, BlockType.DIRT);  // Bedrock
                world.setBlock(x, 0, z, BlockType.GRASS);   // Surface
            }
        }

        // Place tree at (5, 1, 5) - trunk blocks
        world.setBlock(5, 1, 5, BlockType.TREE_TRUNK);
        world.setBlock(5, 2, 5, BlockType.TREE_TRUNK);
        world.setBlock(5, 3, 5, BlockType.TREE_TRUNK);

        // Leaves around the top
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 3; dy <= 4; dy++) {
                    if (!(dx == 0 && dz == 0 && dy == 3)) { // Not the trunk itself
                        world.setBlock(5 + dx, dy, 5 + dz, BlockType.LEAVES);
                    }
                }
            }
        }

        // Action 1: Spawn player at (0, 1, 0) on the ground
        // Calculate spawn height - should be y=1 (standing on GRASS at y=0)
        float spawnY = calculateSpawnHeight(world, 0, 0);
        player = new Player(0, spawnY, 0);

        // Verify player spawns at ground level (y ~= 1.0), NOT floating at y=5
        assertEquals(1.0f, player.getPosition().y, 0.1f,
            "Player should spawn at y=1.0 (on ground), not floating");

        // Apply gravity for several frames to verify player stays on ground
        for (int frame = 0; frame < 60; frame++) {
            float delta = 1.0f / 60.0f; // 60 FPS
            world.moveWithCollision(player, 0, 0, 0, delta);
        }

        // Player should still be at ground level after gravity
        assertEquals(1.0f, player.getPosition().y, 0.1f,
            "After gravity, player should remain at y=1.0 (landed on ground)");

        // Action 2: Move player toward the tree at (5, 0, 5)
        // Move in +X direction (toward x=5)
        for (int step = 0; step < 60; step++) {
            float delta = 1.0f / 60.0f;
            world.moveWithCollision(player, 1, 0, 0, delta); // Move right (+X)
        }

        // Verify player moved toward the tree (X coordinate should be close to 5)
        assertTrue(player.getPosition().x >= 3.0f,
            "Player should have moved toward tree (x >= 3.0)");

        // Now move in +Z direction to get to z=5
        for (int step = 0; step < 60; step++) {
            float delta = 1.0f / 60.0f;
            world.moveWithCollision(player, 0, 0, 1, delta); // Move forward (+Z)
        }

        // Verify player is adjacent to tree (within 2 blocks of (5, 5))
        float distanceToTree = new Vector3(player.getPosition().x - 5, 0,
            player.getPosition().z - 5).len();
        assertTrue(distanceToTree <= 2.5f,
            "Player should be within 2.5 blocks of tree at (5,5)");

        // Action 3: Punch tree trunk at (5, 1, 5) five times
        for (int punch = 0; punch < 5; punch++) {
            boolean broken = blockBreaker.punchBlock(world, 5, 1, 5);
            if (punch < 4) {
                assertFalse(broken, "Block should not break on punch " + (punch + 1));
            } else {
                assertTrue(broken, "Block should break on punch 5");
            }
        }

        // Expected: Block at (5, 1, 5) is now AIR
        assertEquals(BlockType.AIR, world.getBlock(5, 1, 5),
            "Tree trunk at (5,1,5) should be AIR after 5 punches");

        // Add dropped WOOD to inventory (simulating the game loop)
        Material drop = dropTable.getDrop(BlockType.TREE_TRUNK, null);
        assertNotNull(drop, "Tree trunk should drop a material");
        assertEquals(Material.WOOD, drop, "Tree trunk should drop WOOD");
        inventory.addItem(drop, 1);

        // Expected: Inventory contains 1 WOOD
        assertEquals(1, inventory.getItemCount(Material.WOOD),
            "Inventory should contain 1 WOOD after harvesting tree");

        // Final verification: Player Y is still at ground level
        assertEquals(1.0f, player.getPosition().y, 0.1f,
            "Player Y should still be ~1.0 (on ground) after all actions");
    }

    /**
     * Helper method to calculate spawn height by finding highest solid block.
     */
    private float calculateSpawnHeight(World world, int x, int z) {
        for (int y = 64; y >= -10; y--) {
            BlockType block = world.getBlock(x, y, z);
            if (block.isSolid()) {
                return y + 1.0f; // Spawn one block above solid block
            }
        }
        return 1.0f; // Default
    }
}

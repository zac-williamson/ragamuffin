package ragamuffin.building;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

class BlockBreakerTest {

    private BlockBreaker blockBreaker;
    private World world;

    @BeforeEach
    void setUp() {
        blockBreaker = new BlockBreaker();
        world = new World(12345);
    }

    @Test
    void testPunchBlock_NotBrokenAfter4Hits() {
        world.setBlock(0, 1, 0, BlockType.TREE_TRUNK);

        for (int i = 0; i < 4; i++) {
            boolean broken = blockBreaker.punchBlock(world, 0, 1, 0);
            assertFalse(broken);
        }

        assertEquals(BlockType.TREE_TRUNK, world.getBlock(0, 1, 0));
        assertEquals(4, blockBreaker.getHitCount(0, 1, 0));
    }

    @Test
    void testPunchBlock_BrokenAfter5Hits() {
        world.setBlock(0, 1, 0, BlockType.TREE_TRUNK);

        for (int i = 0; i < 4; i++) {
            assertFalse(blockBreaker.punchBlock(world, 0, 1, 0));
        }

        boolean broken = blockBreaker.punchBlock(world, 0, 1, 0);
        assertTrue(broken);

        assertEquals(BlockType.AIR, world.getBlock(0, 1, 0));
        assertEquals(0, blockBreaker.getHitCount(0, 1, 0)); // Should reset
    }

    @Test
    void testPunchDifferentBlocks() {
        world.setBlock(0, 1, 0, BlockType.TREE_TRUNK);
        world.setBlock(1, 1, 0, BlockType.BRICK);

        blockBreaker.punchBlock(world, 0, 1, 0);
        blockBreaker.punchBlock(world, 0, 1, 0);
        blockBreaker.punchBlock(world, 1, 1, 0);

        assertEquals(2, blockBreaker.getHitCount(0, 1, 0));
        assertEquals(1, blockBreaker.getHitCount(1, 1, 0));
    }

    @Test
    void testPunchAir_DoesNothing() {
        boolean broken = blockBreaker.punchBlock(world, 0, 1, 0);
        assertFalse(broken);
        assertEquals(0, blockBreaker.getHitCount(0, 1, 0));
    }

    @Test
    void testResetHits() {
        world.setBlock(0, 1, 0, BlockType.STONE);

        blockBreaker.punchBlock(world, 0, 1, 0);
        blockBreaker.punchBlock(world, 0, 1, 0);
        assertEquals(2, blockBreaker.getHitCount(0, 1, 0));

        blockBreaker.resetHits();
        assertEquals(0, blockBreaker.getHitCount(0, 1, 0));
    }

    @Test
    void testDifferentBlockTypesRequire5Hits() {
        // Soft blocks require 5 hits
        BlockType[] softTypes = {BlockType.TREE_TRUNK, BlockType.GRASS};

        for (BlockType type : softTypes) {
            world.setBlock(0, 1, 0, type);
            blockBreaker.resetHits();

            for (int i = 0; i < 4; i++) {
                assertFalse(blockBreaker.punchBlock(world, 0, 1, 0));
            }

            assertTrue(blockBreaker.punchBlock(world, 0, 1, 0));
            assertEquals(BlockType.AIR, world.getBlock(0, 1, 0));
        }

        // Hard blocks require 8 hits
        BlockType[] hardTypes = {BlockType.BRICK, BlockType.STONE};

        for (BlockType type : hardTypes) {
            world.setBlock(0, 1, 0, type);
            blockBreaker.resetHits();

            for (int i = 0; i < 7; i++) {
                assertFalse(blockBreaker.punchBlock(world, 0, 1, 0));
            }

            assertTrue(blockBreaker.punchBlock(world, 0, 1, 0));
            assertEquals(BlockType.AIR, world.getBlock(0, 1, 0));
        }
    }

    @Test
    void testGetTargetBlock_ReturnsNull_WhenNoBlock() {
        Vector3 origin = new Vector3(0, 5, 0);
        Vector3 direction = new Vector3(0, -1, 0);

        assertNull(blockBreaker.getTargetBlock(world, origin, direction, 5.0f));
    }

    @Test
    void testGetTargetBlock_ReturnsBlock_WhenLookingAt() {
        world.setBlock(0, 1, 0, BlockType.BRICK);

        Vector3 origin = new Vector3(0.5f, 5f, 0.5f);
        Vector3 direction = new Vector3(0, -1, 0);

        var result = blockBreaker.getTargetBlock(world, origin, direction, 10.0f);
        assertNotNull(result);
        assertEquals(0, result.getBlockX());
        assertEquals(1, result.getBlockY());
        assertEquals(0, result.getBlockZ());
        assertEquals(BlockType.BRICK, result.getBlockType());
    }

    @Test
    void testPunchingDifferentBlockResetsCounter() {
        world.setBlock(0, 1, 0, BlockType.TREE_TRUNK);
        world.setBlock(1, 1, 0, BlockType.BRICK);

        // Hit first block 3 times
        blockBreaker.punchBlock(world, 0, 1, 0);
        blockBreaker.punchBlock(world, 0, 1, 0);
        blockBreaker.punchBlock(world, 0, 1, 0);

        // Now punch a different block
        blockBreaker.punchBlock(world, 1, 1, 0);

        // Original block should still have 3 hits
        assertEquals(3, blockBreaker.getHitCount(0, 1, 0));
        assertEquals(1, blockBreaker.getHitCount(1, 1, 0));
    }

    // --- Issue #182: Block hit decay tests ---

    @Test
    void testTickDecay_RetainsHitsBeforeTimeout() {
        world.setBlock(0, 1, 0, BlockType.TREE_TRUNK);

        blockBreaker.punchBlock(world, 0, 1, 0);
        blockBreaker.punchBlock(world, 0, 1, 0);
        assertEquals(2, blockBreaker.getHitCount(0, 1, 0));

        // Tick with a very small delta — hits should still be present
        blockBreaker.tickDecay(0.016f);

        assertEquals(2, blockBreaker.getHitCount(0, 1, 0));
        // Block must still be intact
        assertEquals(BlockType.TREE_TRUNK, world.getBlock(0, 1, 0));
    }

    @Test
    void testTickDecay_ClearsHitsAfterTimeout() throws Exception {
        // Use a subclass that reports the current time so we can simulate the passage
        // of BLOCK_REGEN_SECONDS without actually sleeping.
        //
        // We achieve this by punching a block and then manually invoking tickDecay
        // via a reflective timestamp override is not possible with the private inner
        // class, so instead we use a concrete trick: call punchBlock, then call
        // clearHits (which exists) and verify it, then separately verify that
        // tickDecay removes entries whose timestamp is old enough.
        //
        // The cleanest test-friendly approach: expose a package-private helper that
        // back-dates the timestamp of a hit record, then call tickDecay.

        world.setBlock(0, 1, 0, BlockType.TREE_TRUNK);
        blockBreaker.punchBlock(world, 0, 1, 0);
        assertEquals(1, blockBreaker.getHitCount(0, 1, 0));

        // Back-date the timestamp via the package-private helper
        blockBreaker.backdateHitsForTesting(0, 1, 0,
                (long) (BlockBreaker.BLOCK_REGEN_SECONDS * 1000L) + 1L);

        blockBreaker.tickDecay(0.016f);

        // Hits should have been cleared
        assertEquals(0, blockBreaker.getHitCount(0, 1, 0));
        // Block must still exist — tickDecay only removes hit records, not blocks
        assertEquals(BlockType.TREE_TRUNK, world.getBlock(0, 1, 0));
    }

    @Test
    void testTickDecay_DoesNotClearFreshHits() {
        world.setBlock(0, 1, 0, BlockType.TREE_TRUNK);
        blockBreaker.punchBlock(world, 0, 1, 0);

        // Tick immediately — timestamp is fresh, nothing should be removed
        blockBreaker.tickDecay(0.016f);

        assertEquals(1, blockBreaker.getHitCount(0, 1, 0));
    }

    // --- Issue #255: Door block breaking tests ---

    @Test
    void testPunchDoorLower_BreaksAfter2HitsAndDropsDoor() {
        world.setBlock(0, 1, 0, BlockType.DOOR_LOWER);
        BlockDropTable dropTable = new BlockDropTable();

        // First punch — should not break
        boolean broken = blockBreaker.punchBlock(world, 0, 1, 0);
        assertFalse(broken, "DOOR_LOWER should not break on first punch");
        assertEquals(BlockType.DOOR_LOWER, world.getBlock(0, 1, 0));
        assertEquals(1, blockBreaker.getHitCount(0, 1, 0));

        // Second punch — should break
        broken = blockBreaker.punchBlock(world, 0, 1, 0);
        assertTrue(broken, "DOOR_LOWER should break on second punch");
        assertEquals(BlockType.AIR, world.getBlock(0, 1, 0));
        assertEquals(0, blockBreaker.getHitCount(0, 1, 0));

        // Verify drop
        Material drop = dropTable.getDrop(BlockType.DOOR_LOWER, null);
        assertEquals(Material.DOOR, drop, "Breaking DOOR_LOWER should yield Material.DOOR");
    }

    @Test
    void testPunchDoorUpper_BreaksAfter2Hits() {
        world.setBlock(0, 2, 0, BlockType.DOOR_UPPER);

        assertFalse(blockBreaker.punchBlock(world, 0, 2, 0));
        assertTrue(blockBreaker.punchBlock(world, 0, 2, 0));
        assertEquals(BlockType.AIR, world.getBlock(0, 2, 0));
    }

    @Test
    void testDoorUpper_DropsDoor() {
        BlockDropTable dropTable = new BlockDropTable();
        Material drop = dropTable.getDrop(BlockType.DOOR_UPPER, null);
        assertEquals(Material.DOOR, drop, "Breaking DOOR_UPPER should yield Material.DOOR");
    }

    @Test
    void testTickDecay_OnlyRemovesStaleEntries() throws Exception {
        world.setBlock(0, 1, 0, BlockType.TREE_TRUNK);
        world.setBlock(1, 1, 0, BlockType.BRICK);

        blockBreaker.punchBlock(world, 0, 1, 0);  // will be back-dated (stale)
        blockBreaker.punchBlock(world, 1, 1, 0);  // fresh — should survive

        // Back-date only the first block
        blockBreaker.backdateHitsForTesting(0, 1, 0,
                (long) (BlockBreaker.BLOCK_REGEN_SECONDS * 1000L) + 1L);

        blockBreaker.tickDecay(0.016f);

        assertEquals(0, blockBreaker.getHitCount(0, 1, 0), "Stale block should have 0 hits after decay");
        assertEquals(1, blockBreaker.getHitCount(1, 1, 0), "Fresh block should retain its hit count");
    }
}

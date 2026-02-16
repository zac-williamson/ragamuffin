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
}

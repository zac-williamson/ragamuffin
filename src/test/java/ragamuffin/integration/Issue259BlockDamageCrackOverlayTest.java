package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.BlockBreaker;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #259: block damage crack/overlay visual feedback.
 *
 * Verifies that partially-damaged blocks are exposed via the BlockBreaker API
 * so the renderer can draw crack overlays on them.  The rendering itself uses
 * LibGDX ShapeRenderer and cannot be exercised in headless unit tests, but all
 * the data-layer contracts that drive the rendering are fully testable here.
 */
class Issue259BlockDamageCrackOverlayTest {

    private BlockBreaker blockBreaker;
    private World world;

    @BeforeEach
    void setUp() {
        blockBreaker = new BlockBreaker();
        world = new World(12345);
    }

    /**
     * Test 1: A freshly-placed block that has never been hit must NOT appear in
     * the damaged-blocks set (no overlay should be rendered).
     */
    @Test
    void test1_IntactBlockNotInDamagedSet() {
        world.setBlock(5, 5, 5, BlockType.TREE_TRUNK);

        Set<String> damaged = blockBreaker.getDamagedBlockKeys();
        assertTrue(damaged.isEmpty(),
                "No blocks should be damaged before any punching");
    }

    /**
     * Test 2: After one punch on a TREE_TRUNK (which requires 5 hits to break),
     * that block's key must appear in getDamagedBlockKeys() so the renderer knows
     * to draw a crack overlay on it.
     */
    @Test
    void test2_OnePunchAddsDamagedEntry() {
        world.setBlock(3, 3, 3, BlockType.TREE_TRUNK);
        blockBreaker.punchBlock(world, 3, 3, 3);

        Set<String> damaged = blockBreaker.getDamagedBlockKeys();
        assertEquals(1, damaged.size(),
                "Exactly one block should be in the damaged set after one punch");

        int[] coords = BlockBreaker.parseBlockKey(damaged.iterator().next());
        assertArrayEquals(new int[]{3, 3, 3}, coords,
                "Damaged block key should round-trip to coordinates (3,3,3)");
    }

    /**
     * Test 3: Damage progress for each stage maps to the correct alpha tier.
     * Stage thresholds: <25% → tier 1, 25–50% → tier 2, 50–75% → tier 3, >=75% → tier 4.
     * TREE_TRUNK requires 5 hits: 1 hit = 20%, 2 hits = 40%, 3 hits = 60%, 4 hits = 80%.
     */
    @Test
    void test3_DamageProgressStageMapping() {
        world.setBlock(0, 1, 0, BlockType.TREE_TRUNK); // 5 hits to break

        // Stage 1: 1/5 = 20% → < 25%
        blockBreaker.punchBlock(world, 0, 1, 0);
        float p1 = blockBreaker.getBreakProgress(world, 0, 1, 0, null);
        assertEquals(0.20f, p1, 0.001f, "1 hit should give 20% progress");
        assertTrue(p1 > 0f && p1 < 0.25f, "1 hit should be in stage-1 range (<25%)");

        // Stage 2: 2/5 = 40% → 25–50%
        blockBreaker.punchBlock(world, 0, 1, 0);
        float p2 = blockBreaker.getBreakProgress(world, 0, 1, 0, null);
        assertEquals(0.40f, p2, 0.001f, "2 hits should give 40% progress");
        assertTrue(p2 >= 0.25f && p2 < 0.50f, "2 hits should be in stage-2 range (25–50%)");

        // Stage 3: 3/5 = 60% → 50–75%
        blockBreaker.punchBlock(world, 0, 1, 0);
        float p3 = blockBreaker.getBreakProgress(world, 0, 1, 0, null);
        assertEquals(0.60f, p3, 0.001f, "3 hits should give 60% progress");
        assertTrue(p3 >= 0.50f && p3 < 0.75f, "3 hits should be in stage-3 range (50–75%)");

        // Stage 4: 4/5 = 80% → >= 75%
        blockBreaker.punchBlock(world, 0, 1, 0);
        float p4 = blockBreaker.getBreakProgress(world, 0, 1, 0, null);
        assertEquals(0.80f, p4, 0.001f, "4 hits should give 80% progress");
        assertTrue(p4 >= 0.75f, "4 hits should be in stage-4 range (>=75%)");
    }

    /**
     * Test 4: When a block is fully broken it must be removed from the damaged set
     * so the renderer stops drawing a crack overlay at that position.
     */
    @Test
    void test4_BrokenBlockRemovedFromDamagedSet() {
        world.setBlock(1, 1, 1, BlockType.TREE_TRUNK);

        // Hit 4 times (not yet broken)
        for (int i = 0; i < 4; i++) {
            blockBreaker.punchBlock(world, 1, 1, 1);
        }
        assertFalse(blockBreaker.getDamagedBlockKeys().isEmpty(),
                "Damaged set should be non-empty before the block breaks");

        // 5th hit — breaks the block
        boolean broken = blockBreaker.punchBlock(world, 1, 1, 1);
        assertTrue(broken, "Block should break on the 5th hit");

        assertTrue(blockBreaker.getDamagedBlockKeys().isEmpty(),
                "Damaged set must be empty after the block is fully broken");
        assertEquals(BlockType.AIR, world.getBlock(1, 1, 1),
                "Block should be AIR after breaking");
    }

    /**
     * Test 5: After clearHits() removes the hit record, the block disappears from
     * the damaged set — confirming that crack overlays clear when blocks regenerate
     * (tickDecay calls clearHits internally; this tests the same contract).
     */
    @Test
    void test5_OverlayClearsWhenHitsCleared() {
        world.setBlock(2, 2, 2, BlockType.BRICK);
        blockBreaker.punchBlock(world, 2, 2, 2);

        assertFalse(blockBreaker.getDamagedBlockKeys().isEmpty(),
                "Block should appear damaged after one punch");

        // Clear hits (equivalent to tickDecay removing a stale record)
        blockBreaker.clearHits(2, 2, 2);

        assertTrue(blockBreaker.getDamagedBlockKeys().isEmpty(),
                "Damaged set must be empty after hit record is cleared (block healed)");
        // The block itself should still be in the world
        assertEquals(BlockType.BRICK, world.getBlock(2, 2, 2),
                "Brick block itself must remain — only the hit record is cleared");
    }

    /**
     * Test 6: Multiple partially-damaged blocks all appear in the damaged set,
     * so the renderer draws overlays on each of them independently.
     */
    @Test
    void test6_MultipleDamagedBlocksTrackedIndependently() {
        world.setBlock(0, 1, 0, BlockType.TREE_TRUNK);
        world.setBlock(5, 1, 5, BlockType.BRICK);
        world.setBlock(10, 1, 10, BlockType.GLASS);

        blockBreaker.punchBlock(world, 0, 1, 0);      // 1/5 hits
        blockBreaker.punchBlock(world, 5, 1, 5);      // 1/8 hits
        blockBreaker.punchBlock(world, 10, 1, 10);    // 1/2 hits — breaks GLASS!

        Set<String> damaged = blockBreaker.getDamagedBlockKeys();

        // GLASS (2 hits required) broke on the 1st hit? No — 1 hit < 2, so not broken.
        // Actually GLASS requires 2 hits, so 1 hit is partial.
        // Let's verify counts:
        assertEquals(1, blockBreaker.getHitCount(0, 1, 0), "TREE_TRUNK should have 1 hit");
        assertEquals(1, blockBreaker.getHitCount(5, 1, 5), "BRICK should have 1 hit");
        assertEquals(1, blockBreaker.getHitCount(10, 1, 10), "GLASS should have 1 hit (not yet broken)");
        assertEquals(BlockType.GLASS, world.getBlock(10, 1, 10), "GLASS should not be broken yet");

        assertEquals(3, damaged.size(),
                "All three partially-damaged blocks should appear in the damaged set");

        // Progress checks for the renderer's alpha calculations
        assertTrue(blockBreaker.getBreakProgress(world, 0, 1, 0, null) > 0f);
        assertTrue(blockBreaker.getBreakProgress(world, 5, 1, 5, null) > 0f);
        assertTrue(blockBreaker.getBreakProgress(world, 10, 1, 10, null) > 0f);
    }

    /**
     * Test 7: parseBlockKey correctly round-trips coordinate encoding,
     * including negative coordinates which occur in world chunks near the origin.
     */
    @Test
    void test7_ParseBlockKeyRoundTrip() {
        int[][] cases = {
                {0, 0, 0},
                {15, 63, 15},
                {-5, 2, -10},
                {100, 5, 200},
                {-100, 0, -200},
        };
        for (int[] c : cases) {
            // We need to access the private key format; use punchBlock as a proxy.
            // Instead, directly test parseBlockKey with manually formatted keys.
            String key = c[0] + "," + c[1] + "," + c[2];
            int[] parsed = BlockBreaker.parseBlockKey(key);
            assertArrayEquals(c, parsed,
                    "parseBlockKey should round-trip " + key);
        }
    }

    /**
     * Test 8: getDamagedBlockKeys() returns a snapshot copy — mutating the
     * returned set does not affect the internal state of BlockBreaker.
     */
    @Test
    void test8_GetDamagedBlockKeysReturnsDefensiveCopy() {
        world.setBlock(1, 1, 1, BlockType.TREE_TRUNK);
        blockBreaker.punchBlock(world, 1, 1, 1);

        Set<String> snapshot = blockBreaker.getDamagedBlockKeys();
        assertEquals(1, snapshot.size());

        // Mutate the returned set
        snapshot.clear();

        // Internal state must be unaffected
        assertEquals(1, blockBreaker.getDamagedBlockKeys().size(),
                "Clearing the returned snapshot must not affect BlockBreaker's internal state");
    }
}

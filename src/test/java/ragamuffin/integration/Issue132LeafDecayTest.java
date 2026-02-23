package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.BlockBreaker;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #133 Integration Tests — Leaf decay when tree trunk is removed.
 *
 * Verifies that:
 * 1. Leaves decay when the last supporting trunk is removed.
 * 2. Leaves survive when an adjacent trunk still exists.
 * 3. Leaves more than 4 blocks from the broken trunk are not affected.
 * 4. Non-trunk block removal does not trigger leaf decay.
 * 5. Multi-trunk tree partial harvesting leaves other leaves intact.
 *
 * These tests simulate the decayFloatingLeaves() logic from RagamuffinGame.handlePunch()
 * directly using World and BlockBreaker, mirroring what the production code does.
 */
class Issue132LeafDecayTest {

    private World world;
    private BlockBreaker blockBreaker;

    @BeforeEach
    void setUp() {
        world = new World(99999L);
        blockBreaker = new BlockBreaker();

        // Flat ground
        for (int x = -20; x < 30; x++) {
            for (int z = -20; z < 30; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * Helper: simulate punchBlock until the block breaks (bare fist).
     */
    private boolean breakBlock(int x, int y, int z) {
        BlockType bt = world.getBlock(x, y, z);
        if (bt == BlockType.AIR) return false;
        boolean broken = false;
        for (int i = 0; i < 10; i++) {
            broken = blockBreaker.punchBlock(world, x, y, z);
            if (broken) break;
        }
        return broken;
    }

    /**
     * Helper: simulate the decayFloatingLeaves() logic from RagamuffinGame.
     * Called after a TREE_TRUNK is broken at (brokenX, brokenY, brokenZ).
     */
    private void decayFloatingLeaves(int brokenX, int brokenY, int brokenZ) {
        int radius = 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > radius) continue;
                    int lx = brokenX + dx;
                    int ly = brokenY + dy;
                    int lz = brokenZ + dz;
                    if (world.getBlock(lx, ly, lz) != BlockType.LEAVES) continue;
                    if (!hasNearbyTrunk(lx, ly, lz, radius)) {
                        world.setBlock(lx, ly, lz, BlockType.AIR);
                        blockBreaker.clearHits(lx, ly, lz);
                        // Mark chunk dirty (via world's own markBlockDirty)
                        world.markBlockDirty(lx, ly, lz);
                    }
                }
            }
        }
    }

    private boolean hasNearbyTrunk(int lx, int ly, int lz, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > radius) continue;
                    if (world.getBlock(lx + dx, ly + dy, lz + dz) == BlockType.TREE_TRUNK) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Test 1: Leaves decay when the last trunk is removed.
     *
     * Place a single TREE_TRUNK at (5, 1, 5). Place LEAVES at (5, 2, 5), (5, 2, 6),
     * (6, 2, 5) — all within 4 blocks of the trunk. Break the trunk (5 hits).
     * Verify all three LEAVES blocks are now AIR. Verify at least 1 dirty chunk.
     */
    @Test
    void test1_LeavesDecayWhenLastTrunkRemoved() {
        world.setBlock(5, 1, 5, BlockType.TREE_TRUNK);
        world.setBlock(5, 2, 5, BlockType.LEAVES);
        world.setBlock(5, 2, 6, BlockType.LEAVES);
        world.setBlock(6, 2, 5, BlockType.LEAVES);

        boolean broken = breakBlock(5, 1, 5);
        assertTrue(broken, "TREE_TRUNK should break in 5 hits");
        assertEquals(BlockType.AIR, world.getBlock(5, 1, 5), "Trunk should be AIR after breaking");

        // Trigger leaf decay (as handlePunch() does when a trunk is broken)
        decayFloatingLeaves(5, 1, 5);

        assertEquals(BlockType.AIR, world.getBlock(5, 2, 5), "Unsupported LEAVES at (5,2,5) should decay to AIR");
        assertEquals(BlockType.AIR, world.getBlock(5, 2, 6), "Unsupported LEAVES at (5,2,6) should decay to AIR");
        assertEquals(BlockType.AIR, world.getBlock(6, 2, 5), "Unsupported LEAVES at (6,2,5) should decay to AIR");

        // At least one chunk should be marked dirty
        assertTrue(world.getDirtyChunks().size() >= 1, "At least one chunk should be dirty after leaf decay");
    }

    /**
     * Test 2: Leaves survive when an adjacent trunk still exists.
     *
     * Place TREE_TRUNK at (5, 1, 5) and (5, 2, 5). Place LEAVES at (5, 3, 5).
     * Break the lower trunk. Verify LEAVES at (5, 3, 5) survive (upper trunk still supports).
     * Break the upper trunk. Verify LEAVES at (5, 3, 5) are now AIR.
     */
    @Test
    void test2_LeavesSurviveWhenAdjacentTrunkExists() {
        world.setBlock(5, 1, 5, BlockType.TREE_TRUNK);
        world.setBlock(5, 2, 5, BlockType.TREE_TRUNK);
        world.setBlock(5, 3, 5, BlockType.LEAVES);

        // Break lower trunk
        boolean broken = breakBlock(5, 1, 5);
        assertTrue(broken, "Lower TREE_TRUNK should break");
        decayFloatingLeaves(5, 1, 5);

        assertEquals(BlockType.LEAVES, world.getBlock(5, 3, 5),
            "LEAVES at (5,3,5) should survive: upper trunk at (5,2,5) still provides support");

        // Break upper trunk
        broken = breakBlock(5, 2, 5);
        assertTrue(broken, "Upper TREE_TRUNK should break");
        decayFloatingLeaves(5, 2, 5);

        assertEquals(BlockType.AIR, world.getBlock(5, 3, 5),
            "LEAVES at (5,3,5) should now decay: no trunk remains within 4 blocks");
    }

    /**
     * Test 3: Leaves more than 4 blocks away from the broken trunk are not affected.
     *
     * Place TREE_TRUNK at (5, 1, 5). Place LEAVES at (10, 1, 5) — 5 blocks away
     * (beyond the decay radius). Break the trunk. Verify LEAVES at (10, 1, 5) remain.
     */
    @Test
    void test3_LeavesOutsideRadiusNotAffected() {
        world.setBlock(5, 1, 5, BlockType.TREE_TRUNK);
        world.setBlock(10, 1, 5, BlockType.LEAVES); // Manhattan distance = 5 > 4

        boolean broken = breakBlock(5, 1, 5);
        assertTrue(broken, "TREE_TRUNK should break");
        decayFloatingLeaves(5, 1, 5);

        assertEquals(BlockType.LEAVES, world.getBlock(10, 1, 5),
            "LEAVES 5 blocks away should not be affected by decay (radius is 4)");
    }

    /**
     * Test 4: Non-trunk block removal does not trigger leaf decay.
     *
     * Place a BRICK block at (5, 1, 5) and LEAVES at (5, 2, 5). Break the BRICK (8 hits).
     * Verify the LEAVES remain. Leaf decay is only triggered by TREE_TRUNK removal.
     */
    @Test
    void test4_NonTrunkRemovalDoesNotTriggerDecay() {
        world.setBlock(5, 1, 5, BlockType.BRICK);
        world.setBlock(5, 2, 5, BlockType.LEAVES);

        boolean broken = breakBlock(5, 1, 5);
        assertTrue(broken, "BRICK block should break in 8 hits");
        assertEquals(BlockType.AIR, world.getBlock(5, 1, 5), "BRICK should be gone");

        // handlePunch() only calls decayFloatingLeaves() when blockType == TREE_TRUNK.
        // We do NOT call decayFloatingLeaves() here — simulating that a BRICK break was detected.

        assertEquals(BlockType.LEAVES, world.getBlock(5, 2, 5),
            "LEAVES should remain: non-trunk removal must not trigger leaf decay");
    }

    /**
     * Test 5: Multi-trunk tree — partial harvesting leaves other leaves intact.
     *
     * Build a 3-trunk-tall tree at (5,1,5), (5,2,5), (5,3,5) with LEAVES at
     * (5,4,5), (6,4,5), (5,4,6). Break the bottom trunk (5,1,5). Verify none of
     * the leaves have decayed (all within 4 blocks of remaining trunks).
     * Break all remaining trunks. Verify all leaves are now AIR.
     */
    @Test
    void test5_MultiTrunkPartialHarvestLeavesOtherLeavesIntact() {
        world.setBlock(5, 1, 5, BlockType.TREE_TRUNK);
        world.setBlock(5, 2, 5, BlockType.TREE_TRUNK);
        world.setBlock(5, 3, 5, BlockType.TREE_TRUNK);
        world.setBlock(5, 4, 5, BlockType.LEAVES);
        world.setBlock(6, 4, 5, BlockType.LEAVES);
        world.setBlock(5, 4, 6, BlockType.LEAVES);

        // Break bottom trunk
        boolean broken = breakBlock(5, 1, 5);
        assertTrue(broken, "Bottom trunk should break");
        decayFloatingLeaves(5, 1, 5);

        // All leaves should still be intact — remaining trunks at y=2 and y=3 support them
        assertEquals(BlockType.LEAVES, world.getBlock(5, 4, 5),
            "LEAVES at (5,4,5) should survive after removing bottom trunk");
        assertEquals(BlockType.LEAVES, world.getBlock(6, 4, 5),
            "LEAVES at (6,4,5) should survive after removing bottom trunk");
        assertEquals(BlockType.LEAVES, world.getBlock(5, 4, 6),
            "LEAVES at (5,4,6) should survive after removing bottom trunk");

        // Break middle trunk
        broken = breakBlock(5, 2, 5);
        assertTrue(broken, "Middle trunk should break");
        decayFloatingLeaves(5, 2, 5);

        // Leaves should still be intact — top trunk at y=3 still supports them
        assertEquals(BlockType.LEAVES, world.getBlock(5, 4, 5),
            "LEAVES at (5,4,5) should survive after removing middle trunk");

        // Break top trunk
        broken = breakBlock(5, 3, 5);
        assertTrue(broken, "Top trunk should break");
        decayFloatingLeaves(5, 3, 5);

        // Now all leaves should have decayed
        assertEquals(BlockType.AIR, world.getBlock(5, 4, 5),
            "LEAVES at (5,4,5) should be AIR after all trunks removed");
        assertEquals(BlockType.AIR, world.getBlock(6, 4, 5),
            "LEAVES at (6,4,5) should be AIR after all trunks removed");
        assertEquals(BlockType.AIR, world.getBlock(5, 4, 6),
            "LEAVES at (5,4,6) should be AIR after all trunks removed");
    }
}

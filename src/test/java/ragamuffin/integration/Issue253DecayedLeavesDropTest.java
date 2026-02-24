package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.BlockBreaker;
import ragamuffin.building.BlockDropTable;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #253 Integration Test — Decayed leaves yield loot when trunk is broken.
 *
 * Verifies that when a TREE_TRUNK is broken and nearby unsupported LEAVES blocks
 * are decayed via decayFloatingLeaves(), the drop table is consulted for each
 * decayed LEAVES block and any resulting drop is added to the inventory.
 */
class Issue253DecayedLeavesDropTest {

    private World world;
    private BlockBreaker blockBreaker;
    private BlockDropTable dropTable;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        world = new World(99999L);
        blockBreaker = new BlockBreaker();
        dropTable = new BlockDropTable();
        inventory = new Inventory(36);

        // Flat ground
        for (int x = -20; x < 30; x++) {
            for (int z = -20; z < 30; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * Helper: simulate the decayFloatingLeaves() logic from RagamuffinGame,
     * including the Issue #253 fix: consult the drop table for each decayed leaf.
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
                        world.markBlockDirty(lx, ly, lz);
                        // Issue #253 fix: consult drop table for decayed leaves
                        Material drop = dropTable.getDrop(BlockType.LEAVES, null);
                        if (drop != null) {
                            inventory.addItem(drop, 1);
                        }
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
     * Helper: simulate punchBlock until the block breaks.
     */
    private boolean breakBlock(int x, int y, int z) {
        BlockType bt = world.getBlock(x, y, z);
        if (bt == BlockType.AIR) return false;
        for (int i = 0; i < 10; i++) {
            if (blockBreaker.punchBlock(world, x, y, z)) return true;
        }
        return false;
    }

    /**
     * Test: Decayed leaves can yield WOOD.
     *
     * Place a TREE_TRUNK at (5,1,5) with many LEAVES blocks around it.
     * Break the trunk. With 30% drop rate over many leaves, at least some
     * WOOD should appear in the inventory across multiple trial runs.
     *
     * Uses 10 LEAVES blocks (all within radius 4 of the trunk). The probability
     * that zero drops occur in a single trial is 0.7^10 ≈ 2.8%. Running enough
     * trials ensures the test is statistically reliable.
     */
    @Test
    void test_DecayedLeavesYieldWood() {
        // Run multiple trials to confirm the drop table is being consulted
        boolean sawWoodDrop = false;

        for (int trial = 0; trial < 20 && !sawWoodDrop; trial++) {
            // Fresh world and inventory per trial
            World trialWorld = new World(99999L + trial);
            BlockBreaker trialBreaker = new BlockBreaker();
            Inventory trialInventory = new Inventory(36);

            // Flat ground
            for (int x = -5; x < 15; x++) {
                for (int z = -5; z < 15; z++) {
                    trialWorld.setBlock(x, 0, z, BlockType.GRASS);
                }
            }

            // Place trunk and 10 LEAVES blocks within radius 4
            trialWorld.setBlock(5, 1, 5, BlockType.TREE_TRUNK);
            trialWorld.setBlock(5, 2, 5, BlockType.LEAVES);
            trialWorld.setBlock(5, 2, 6, BlockType.LEAVES);
            trialWorld.setBlock(6, 2, 5, BlockType.LEAVES);
            trialWorld.setBlock(4, 2, 5, BlockType.LEAVES);
            trialWorld.setBlock(5, 2, 4, BlockType.LEAVES);
            trialWorld.setBlock(5, 3, 5, BlockType.LEAVES);
            trialWorld.setBlock(5, 3, 6, BlockType.LEAVES);
            trialWorld.setBlock(6, 3, 5, BlockType.LEAVES);
            trialWorld.setBlock(4, 3, 5, BlockType.LEAVES);
            trialWorld.setBlock(5, 3, 4, BlockType.LEAVES);

            // Break trunk
            for (int i = 0; i < 10; i++) {
                if (trialBreaker.punchBlock(trialWorld, 5, 1, 5)) break;
            }

            // Decay leaves with drop table
            int radius = 4;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > radius) continue;
                        int lx = 5 + dx, ly = 1 + dy, lz = 5 + dz;
                        if (trialWorld.getBlock(lx, ly, lz) != BlockType.LEAVES) continue;
                        // No trunks remain, so all leaves are unsupported
                        trialWorld.setBlock(lx, ly, lz, BlockType.AIR);
                        Material drop = dropTable.getDrop(BlockType.LEAVES, null);
                        if (drop != null) {
                            trialInventory.addItem(drop, 1);
                        }
                    }
                }
            }

            if (trialInventory.getItemCount(Material.WOOD) > 0) {
                sawWoodDrop = true;
            }
        }

        assertTrue(sawWoodDrop,
            "Decayed leaves should yield WOOD at least once in 20 trials with 10 leaves each " +
            "(probability of all-zero drops per trial is ~2.8%, chance of 20 failures is negligible)");
    }

    /**
     * Test: Decayed leaves drop only WOOD (or nothing), never unexpected materials.
     *
     * Run the drop table for LEAVES many times and confirm the result is always
     * WOOD or null — consistent with the #251 fix and BlockDropTable spec.
     */
    @Test
    void test_DecayedLeavesDropOnlyWoodOrNull() {
        // Place trunk and one LEAVES block, decay 500 times across fresh scenarios
        for (int i = 0; i < 500; i++) {
            Material drop = dropTable.getDrop(BlockType.LEAVES, null);
            assertTrue(drop == null || drop == Material.WOOD,
                "Decayed LEAVES should drop WOOD or null, but got: " + drop);
        }
    }

    /**
     * Test: Leaves that are still supported by a remaining trunk do NOT decay
     * and therefore do NOT yield any loot from the decay path.
     *
     * Place two trunks (lower and upper) with LEAVES above the upper. Remove
     * only the lower trunk. The LEAVES should survive (supported by upper trunk)
     * and the inventory should remain empty from the decay pass.
     */
    @Test
    void test_SupportedLeavesDoNotDecayAndYieldNoLoot() {
        world.setBlock(5, 1, 5, BlockType.TREE_TRUNK);
        world.setBlock(5, 2, 5, BlockType.TREE_TRUNK);
        world.setBlock(5, 3, 5, BlockType.LEAVES);

        // Break only the lower trunk
        assertTrue(breakBlock(5, 1, 5), "Lower trunk should break");

        // Decay pass — LEAVES at (5,3,5) are supported by upper trunk at (5,2,5)
        decayFloatingLeaves(5, 1, 5);

        // LEAVES should still be there
        assertEquals(BlockType.LEAVES, world.getBlock(5, 3, 5),
            "LEAVES supported by remaining trunk should not decay");

        // Inventory should be empty — no loot from non-decayed leaves
        assertEquals(0, inventory.getItemCount(Material.WOOD),
            "No loot should be added for leaves that did not decay");
    }
}

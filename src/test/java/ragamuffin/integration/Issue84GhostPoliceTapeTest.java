package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.BlockBreaker;
import ragamuffin.building.Inventory;
import ragamuffin.ai.NPCManager;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #84 Integration Tests — demolishBlock() leaves ghost police-tape protection.
 *
 * Verifies that:
 * 1. A block position that was police-taped and then cleared by removePoliceTape() is
 *    no longer protected — a new block placed there can be punched.
 * 2. A police-taped block that is NOT cleared remains protected (regression guard).
 * 3. The full scenario: tape a block, demolish it (via world.removePoliceTape()), place a
 *    new block at the same position, confirm it breaks normally.
 */
class Issue84GhostPoliceTapeTest {

    private World world;
    private BlockBreaker blockBreaker;

    private static final int X = 5, Y = 1, Z = 5;

    @BeforeEach
    void setUp() {
        world = new World(42L);
        blockBreaker = new BlockBreaker();

        // Flat ground so world is consistent
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                world.setBlock(x, 0, z, BlockType.PAVEMENT);
            }
        }
    }

    /**
     * Test 1: After removePoliceTape(), a new block at that position is breakable.
     *
     * Place a WOOD block at (5,1,5). Add police tape. Confirm block is protected.
     * Call removePoliceTape(). Set a new WOOD block at the same position.
     * Punch it 5 times — it must break (not be silently rejected by isProtected()).
     */
    @Test
    void test1_BlockIsBreakableAfterPoliceTapeRemoved() {
        // Place a block and police-tape it (simulating police visit)
        world.setBlock(X, Y, Z, BlockType.WOOD);
        world.addPoliceTape(X, Y, Z);

        assertTrue(world.isProtected(X, Y, Z),
            "Block at (" + X + "," + Y + "," + Z + ") should be protected after addPoliceTape()");

        // Simulate demolition: clear the block and remove the tape
        world.setBlock(X, Y, Z, BlockType.AIR);
        world.removePoliceTape(X, Y, Z);

        assertFalse(world.isProtected(X, Y, Z),
            "Position should no longer be protected after removePoliceTape()");

        // Player places a new block at the demolished position
        world.setBlock(X, Y, Z, BlockType.WOOD);

        // Punch it 5 times — WOOD requires 5 hits with bare fist
        boolean broken = false;
        for (int i = 0; i < 5; i++) {
            broken = blockBreaker.punchBlock(world, X, Y, Z);
        }

        assertTrue(broken,
            "New WOOD block placed at a previously police-taped position should be breakable " +
            "after police tape is removed. Before fix #84, ghost protection in protectedBlocks " +
            "caused punchBlock() to silently return false on every punch.");
        assertEquals(BlockType.AIR, world.getBlock(X, Y, Z),
            "Block should be AIR after being broken");
    }

    /**
     * Test 2: A police-taped block that is NOT cleared remains unbreakable (regression guard).
     *
     * Place a WOOD block, add police tape, do NOT call removePoliceTape().
     * Punch it 10 times — it must remain unbroken (police tape protection should work).
     */
    @Test
    void test2_PoliceTapedBlockRemainsProtected() {
        world.setBlock(X, Y, Z, BlockType.WOOD);
        world.addPoliceTape(X, Y, Z);

        // Punch 10 times without removing tape
        for (int i = 0; i < 10; i++) {
            blockBreaker.punchBlock(world, X, Y, Z);
        }

        assertNotEquals(BlockType.AIR, world.getBlock(X, Y, Z),
            "A police-taped block should remain unbreakable — tape protection must still work.");
        assertTrue(world.isProtected(X, Y, Z),
            "Block should still be protected after punching without tape removal.");
    }

    /**
     * Test 3: Full ghost-protection scenario from Issue #84.
     *
     * 1. Place WOOD block — police tape it (simulates police visit at night).
     * 2. Demolish: set to AIR and call removePoliceTape() (what the fix adds to demolishBlock()).
     * 3. Player places a new WOOD block at the same position.
     * 4. Punch 5 times — must break normally.
     * 5. Verify position is no longer protected.
     *
     * Before the fix, step 2 omitted removePoliceTape(), so the position stayed in
     * protectedBlocks forever. Step 4 would silently fail on every punch.
     */
    @Test
    void test3_FullGhostProtectionScenario() {
        // Step 1: player builds, police tape appears
        world.setBlock(X, Y, Z, BlockType.WOOD);
        world.addPoliceTape(X, Y, Z);
        assertTrue(world.isProtected(X, Y, Z), "Pre-condition: block must be protected");

        // Step 2: council builder demolishes the block
        // (demolishBlock() calls world.setBlock AIR, then world.removePoliceTape — the fix)
        world.setBlock(X, Y, Z, BlockType.AIR);
        world.removePoliceTape(X, Y, Z);  // This is the line the fix adds

        assertFalse(world.isProtected(X, Y, Z),
            "After demolition + removePoliceTape(), position must not be protected");
        assertEquals(BlockType.AIR, world.getBlock(X, Y, Z),
            "Block must be AIR after demolition");

        // Step 3: player places a new block at the same position
        world.setBlock(X, Y, Z, BlockType.WOOD);
        assertEquals(BlockType.WOOD, world.getBlock(X, Y, Z),
            "New WOOD block should be present at demolished position");

        // Step 4: punch the new block 5 times — must break
        boolean broken = false;
        for (int punch = 1; punch <= 5; punch++) {
            boolean result = blockBreaker.punchBlock(world, X, Y, Z);
            if (result) {
                broken = true;
                break;
            }
            // Verify hit is registering (block is not being silently rejected)
            if (punch < 5) {
                int hits = blockBreaker.getHitCount(X, Y, Z);
                assertEquals(punch, hits,
                    "Hit counter should be " + punch + " after " + punch + " punches, " +
                    "but was " + hits + ". If this is 0, punchBlock() is returning false " +
                    "due to ghost police-tape protection.");
            }
        }

        assertTrue(broken,
            "New block at previously demolish+taped position must break after 5 punches. " +
            "Ghost protection from Issue #84 would cause all punches to silently fail.");

        // Step 5: confirm position is clean
        assertEquals(BlockType.AIR, world.getBlock(X, Y, Z),
            "Position should be AIR after block is broken");
        assertFalse(world.isProtected(X, Y, Z),
            "Position should not be protected after block is broken");
    }
}

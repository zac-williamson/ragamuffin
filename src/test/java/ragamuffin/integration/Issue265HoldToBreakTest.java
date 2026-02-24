package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.BlockBreaker;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #265: hold-to-break auto-punch while left mouse
 * button is held.
 *
 * Verifies the repeat-fire timer logic: 8 punch events fired at 0.25 s intervals
 * (simulating 2 seconds of holding) must break a BRICK block (which requires
 * 8 hits) without the player needing to click 8 separate times.
 *
 * The timer logic lives in RagamuffinGame and depends on LibGDX, so these tests
 * exercise the BlockBreaker layer directly — simulating the sequence of punch
 * events that the timer would fire — and confirm the end-to-end break behaviour.
 */
class Issue265HoldToBreakTest {

    private static final float PUNCH_REPEAT_INTERVAL = 0.25f; // must match RagamuffinGame

    private BlockBreaker blockBreaker;
    private World world;

    @BeforeEach
    void setUp() {
        blockBreaker = new BlockBreaker();
        world = new World(42);
    }

    /**
     * Test 1: Holding left mouse for 2 seconds fires 8 punch events at 0.25 s
     * intervals and breaks a BRICK block (8 hits required).
     *
     * Scenario: the hold-repeat timer starts at 0 on initial click (tick 0),
     * then accumulates delta per frame.  After each PUNCH_REPEAT_INTERVAL seconds
     * it fires one punch.  Across 2 s at 0.25 s intervals that is exactly 8 punches
     * (at t = 0.25, 0.50, 0.75, 1.00, 1.25, 1.50, 1.75, 2.00).
     */
    @Test
    void test1_HoldingForTwoSecondsBreaksBrickBlock() {
        world.setBlock(5, 1, 5, BlockType.BRICK);

        // Simulate the 8 punch events that the repeat timer fires while held
        int punchsFired = 0;
        float heldTimer = 0f;
        float totalTime = 2.0f;
        float delta = PUNCH_REPEAT_INTERVAL;
        float elapsed = 0f;
        boolean broken = false;

        while (elapsed < totalTime - 0.001f) {
            elapsed += delta;
            heldTimer += delta;
            if (heldTimer >= PUNCH_REPEAT_INTERVAL) {
                heldTimer -= PUNCH_REPEAT_INTERVAL;
                broken = blockBreaker.punchBlock(world, 5, 1, 5);
                punchsFired++;
            }
        }

        assertEquals(8, punchsFired,
                "Exactly 8 punch events should fire in 2 seconds at 0.25 s intervals");
        assertTrue(broken,
                "The 8th punch must break the BRICK block");
        assertEquals(BlockType.AIR, world.getBlock(5, 1, 5),
                "BRICK block must be replaced with AIR after 8 hits");
    }

    /**
     * Test 2: Holding for 1.75 seconds (7 punches) does NOT break a BRICK block —
     * confirming that the block requires all 8 hits and does not break prematurely.
     */
    @Test
    void test2_SevenPunchesDoNotBreakBrickBlock() {
        world.setBlock(3, 1, 3, BlockType.BRICK);

        boolean broken = false;
        for (int i = 0; i < 7; i++) {
            broken = blockBreaker.punchBlock(world, 3, 1, 3);
        }

        assertFalse(broken,
                "7 punches must NOT break a BRICK block (requires 8 hits)");
        assertEquals(BlockType.BRICK, world.getBlock(3, 1, 3),
                "BRICK block must still be present after only 7 hits");
        float progress = blockBreaker.getBreakProgress(world, 3, 1, 3, null);
        assertEquals(7f / 8f, progress, 0.001f,
                "Break progress should be 87.5% after 7 of 8 hits");
    }

    /**
     * Test 3: Holding for 1.25 seconds (5 punches at 0.25 s intervals) breaks a
     * TREE_TRUNK block (requires 5 hits) — shorter hold than BRICK.
     */
    @Test
    void test3_HoldingFiveIntervalsBreaксTreeTrunk() {
        world.setBlock(7, 1, 7, BlockType.TREE_TRUNK);

        boolean broken = false;
        for (int i = 0; i < 5; i++) {
            broken = blockBreaker.punchBlock(world, 7, 1, 7);
        }

        assertTrue(broken,
                "5 punches must break a TREE_TRUNK block");
        assertEquals(BlockType.AIR, world.getBlock(7, 1, 7),
                "TREE_TRUNK must be AIR after 5 hits");
    }

    /**
     * Test 4: When aiming at a new block the timer resets, so punches from the
     * old target don't carry over.  Switching mid-hold to a fresh BRICK block
     * requires another 8 hits on the new block.
     */
    @Test
    void test4_SwitchingTargetResetsProgress() {
        world.setBlock(1, 1, 1, BlockType.BRICK);
        world.setBlock(2, 1, 1, BlockType.BRICK);

        // Hit first block 4 times
        for (int i = 0; i < 4; i++) {
            blockBreaker.punchBlock(world, 1, 1, 1);
        }

        // "Switch target" — start punching second block; first block must survive
        boolean secondBroken = false;
        for (int i = 0; i < 8; i++) {
            secondBroken = blockBreaker.punchBlock(world, 2, 1, 1);
        }

        assertTrue(secondBroken, "Second BRICK block should break after 8 hits");
        assertEquals(BlockType.BRICK, world.getBlock(1, 1, 1),
                "First BRICK block must still exist — its hits are independent");
        float p1 = blockBreaker.getBreakProgress(world, 1, 1, 1, null);
        assertEquals(4f / 8f, p1, 0.001f,
                "First block's progress must remain at 50% (4/8 hits)");
    }

    /**
     * Test 5: GLASS (2 hits required) breaks after just 2 punch events,
     * so a very brief hold (0.5 s) is sufficient.
     */
    @Test
    void test5_TwoPunchesBreakGlass() {
        world.setBlock(9, 1, 9, BlockType.GLASS);

        blockBreaker.punchBlock(world, 9, 1, 9); // 1st — should not break
        assertFalse(blockBreaker.getDamagedBlockKeys().isEmpty(),
                "GLASS should be in damaged set after first punch");

        boolean broken = blockBreaker.punchBlock(world, 9, 1, 9); // 2nd — should break
        assertTrue(broken, "GLASS must break on the 2nd punch");
        assertEquals(BlockType.AIR, world.getBlock(9, 1, 9),
                "GLASS block must be AIR after 2 hits");
    }
}

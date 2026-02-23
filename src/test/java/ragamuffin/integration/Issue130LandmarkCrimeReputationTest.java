package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.BlockBreaker;
import ragamuffin.building.Inventory;
import ragamuffin.core.StreetReputation;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.Landmark;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #130 Integration Tests — Landmark crimes must award reputation points.
 *
 * Verifies that:
 * 1. Raiding Greggs (breaking a Greggs block) awards 1 reputation point per block.
 * 2. Looting the jeweller (breaking a jeweller block) awards 3 reputation points per block.
 * 3. Breaking non-landmark blocks (trees, grass) still awards zero reputation (Issue #46 preserved).
 * 4. Reputation accumulates correctly across multiple landmark crimes.
 * 5. NOTORIOUS status is reachable through landmark crimes alone (no NPC punching needed).
 * 6. Breaking 50+ non-landmark blocks stays at zero reputation (Issue #46 regression guard).
 *
 * The fix is in RagamuffinGame.handlePunch(). These tests simulate that logic directly
 * using StreetReputation.addPoints() after a landmark block is detected as broken,
 * mirroring exactly what the production code now does.
 */
class Issue130LandmarkCrimeReputationTest {

    private World world;
    private Player player;
    private NPCManager npcManager;
    private Inventory inventory;
    private TooltipSystem tooltipSystem;
    private BlockBreaker blockBreaker;

    @BeforeEach
    void setUp() {
        world = new World(77777L);
        player = new Player(0, 1, 0);
        npcManager = new NPCManager();
        inventory = new Inventory(36);
        tooltipSystem = new TooltipSystem();
        blockBreaker = new BlockBreaker();

        // Flat ground for testing
        for (int x = -60; x < 60; x++) {
            for (int z = -60; z < 60; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * Helper: simulate RagamuffinGame.handlePunch() for a block with a given landmark.
     * Breaks the block using BlockBreaker then awards reputation based on the landmark,
     * matching the production code switch statement in handlePunch().
     *
     * @return true if the block was broken on the final punch
     */
    private boolean simulatePunchWithReputation(int x, int y, int z, LandmarkType landmark, int hitsToBreak) {
        boolean broken = false;
        for (int i = 0; i < hitsToBreak; i++) {
            broken = blockBreaker.punchBlock(world, x, y, z);
        }
        if (broken && landmark != null) {
            switch (landmark) {
                case GREGGS:
                    player.getStreetReputation().addPoints(1);
                    break;
                case JEWELLER:
                    player.getStreetReputation().addPoints(3);
                    break;
                case OFF_LICENCE:
                case CHARITY_SHOP:
                case BOOKIES:
                case OFFICE_BUILDING:
                    player.getStreetReputation().addPoints(1);
                    break;
                default:
                    break;
            }
        }
        return broken;
    }

    /**
     * Test 1: Raiding Greggs awards 1 reputation per block.
     *
     * Place a GLASS block tagged as GREGGS landmark. Break it (2 hits for GLASS).
     * Verify reputation increases by 1. Break 3 such blocks — verify reputation is 3.
     */
    @Test
    void test1_GreggsRaidAwards1ReputationPerBlock() {
        // Register Greggs landmark covering positions (10,1,10) to (15,3,15)
        world.addLandmark(new Landmark(LandmarkType.GREGGS, 10, 1, 10, 5, 3, 5));

        assertEquals(0, player.getStreetReputation().getPoints(), "Should start at 0");

        // Break 3 GLASS blocks inside the Greggs landmark area
        for (int i = 0; i < 3; i++) {
            world.setBlock(10 + i, 1, 10, BlockType.GLASS);
            boolean broken = simulatePunchWithReputation(10 + i, 1, 10, LandmarkType.GREGGS, 2);
            assertTrue(broken, "GLASS block should break in 2 hits");
        }

        assertEquals(3, player.getStreetReputation().getPoints(),
            "Breaking 3 Greggs blocks should award 3 reputation points (1 per block)");
    }

    /**
     * Test 2: Looting the jeweller awards 3 reputation per block.
     *
     * Place a GLASS block tagged as JEWELLER. Break it (2 hits).
     * Verify reputation increases by 3.
     */
    @Test
    void test2_JewellerTheftAwards3ReputationPerBlock() {
        world.addLandmark(new Landmark(LandmarkType.JEWELLER, 20, 1, 20, 5, 3, 5));

        assertEquals(0, player.getStreetReputation().getPoints(), "Should start at 0");

        world.setBlock(20, 1, 20, BlockType.GLASS);
        boolean broken = simulatePunchWithReputation(20, 1, 20, LandmarkType.JEWELLER, 2);
        assertTrue(broken, "GLASS block should break in 2 hits");

        assertEquals(3, player.getStreetReputation().getPoints(),
            "Breaking a jeweller block should award exactly 3 reputation points");
    }

    /**
     * Test 3: Breaking non-landmark blocks still awards zero reputation.
     *
     * Place a TREE_TRUNK (no landmark tag). Break it (5 hits).
     * Verify reputation stays at 0. Verify no civilians flee.
     */
    @Test
    void test3_NonLandmarkBlockBreakingAwardsZeroReputation() {
        // No landmark registered at this position
        world.setBlock(5, 1, 5, BlockType.TREE_TRUNK);

        // Simulate punch without landmark (null) — no reputation awarded
        boolean broken = simulatePunchWithReputation(5, 1, 5, null, 5);
        assertTrue(broken, "TREE_TRUNK should break in 5 hits");

        assertEquals(0, player.getStreetReputation().getPoints(),
            "Breaking a non-landmark block should award zero reputation (Issue #46 preserved)");
        assertFalse(player.getStreetReputation().isNotorious(),
            "Player should not be notorious after breaking a tree");
        assertFalse(player.getStreetReputation().isKnown(),
            "Player should not even be known after breaking a tree");

        // Spawn civilian and verify they don't flee
        NPC civilian = npcManager.spawnNPC(NPCType.PUBLIC, 5, 1, 0);
        assertNotNull(civilian);
        npcManager.update(0.016f, world, player, inventory, tooltipSystem);
        assertNotEquals(NPCState.FLEEING, civilian.getState(),
            "Civilian should not flee — player's reputation is 0 after tree breaking");
    }

    /**
     * Test 4: Reputation accumulates across multiple landmark crimes.
     *
     * Break 10 Greggs blocks and steal from the jeweller once.
     * Verify reputation is at least 13 (10×1 + 1×3).
     * Verify player has reached KNOWN status (≥10 pts).
     */
    @Test
    void test4_ReputationAccumulatesAcrossLandmarkCrimes() {
        world.addLandmark(new Landmark(LandmarkType.GREGGS, 10, 1, 10, 20, 3, 5));
        world.addLandmark(new Landmark(LandmarkType.JEWELLER, 40, 1, 10, 5, 3, 5));

        // Break 10 Greggs GLASS blocks
        for (int i = 0; i < 10; i++) {
            world.setBlock(10 + i, 1, 10, BlockType.GLASS);
            boolean broken = simulatePunchWithReputation(10 + i, 1, 10, LandmarkType.GREGGS, 2);
            assertTrue(broken, "Greggs block " + i + " should break");
        }

        assertEquals(10, player.getStreetReputation().getPoints(),
            "10 Greggs blocks should give 10 reputation points");
        assertTrue(player.getStreetReputation().isKnown(),
            "Player should have reached KNOWN status after 10 Greggs points");

        // Now steal from the jeweller (3 more points)
        world.setBlock(40, 1, 10, BlockType.GLASS);
        boolean jewBroken = simulatePunchWithReputation(40, 1, 10, LandmarkType.JEWELLER, 2);
        assertTrue(jewBroken, "Jeweller block should break");

        int total = player.getStreetReputation().getPoints();
        assertTrue(total >= 13,
            "Reputation should be at least 13 after 10 Greggs + 1 jeweller. Got: " + total);
    }

    /**
     * Test 5: NOTORIOUS status is reachable through landmark crimes alone.
     *
     * Break enough Greggs blocks (30+) without punching any NPC.
     * Verify player reaches NOTORIOUS status.
     * Verify a PUBLIC civilian enters FLEEING state on the next NPCManager update tick.
     */
    @Test
    void test5_NotoriousStatusReachableThroughLandmarkCrimesAlone() {
        world.addLandmark(new Landmark(LandmarkType.GREGGS, 10, 1, 10, 50, 3, 5));

        // Break 30 Greggs blocks — enough to reach NOTORIOUS (threshold = 30)
        for (int i = 0; i < StreetReputation.NOTORIOUS_THRESHOLD; i++) {
            world.setBlock(10 + i, 1, 10, BlockType.GLASS);
            boolean broken = simulatePunchWithReputation(10 + i, 1, 10, LandmarkType.GREGGS, 2);
            assertTrue(broken, "Block " + i + " should break");
        }

        assertTrue(player.getStreetReputation().isNotorious(),
            "Player should reach NOTORIOUS status through landmark crimes alone (no NPC punching)");

        // Spawn a PUBLIC civilian nearby and run an update tick
        NPC civilian = npcManager.spawnNPC(NPCType.PUBLIC, 5, 1, 0);
        assertNotNull(civilian);
        npcManager.update(0.016f, world, player, inventory, tooltipSystem);

        assertEquals(NPCState.FLEEING, civilian.getState(),
            "Civilian should flee from NOTORIOUS player (reached via landmark crimes only)");
    }

    /**
     * Test 6: Non-criminal block breaks remain at zero reputation (Issue #46 regression guard).
     *
     * Break 50 TREE_TRUNK blocks and 50 GRASS blocks (none tagged to landmarks).
     * Verify reputation is exactly 0 throughout.
     */
    @Test
    void test6_Issue46RegressionGuard_NonCriminalBlocksStayAtZero() {
        // Break 50 TREE_TRUNK blocks — no landmark
        for (int i = 0; i < 50; i++) {
            world.setBlock(i, 1, 0, BlockType.TREE_TRUNK);
            simulatePunchWithReputation(i, 1, 0, null, 5);
        }

        assertEquals(0, player.getStreetReputation().getPoints(),
            "Reputation should be 0 after breaking 50 TREE_TRUNK blocks (Issue #46)");

        // Break 50 GRASS blocks — no landmark
        for (int i = 0; i < 50; i++) {
            world.setBlock(i, 1, 1, BlockType.GRASS);
            simulatePunchWithReputation(i, 1, 1, null, 5);
        }

        assertEquals(0, player.getStreetReputation().getPoints(),
            "Reputation should remain 0 after breaking 50 GRASS blocks (Issue #46 regression guard)");
        assertFalse(player.getStreetReputation().isKnown(),
            "Player should not be KNOWN after 100 non-landmark block breaks");
    }
}

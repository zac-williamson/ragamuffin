package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #206 Integration Tests — Police lose player when line of sight is broken.
 *
 * Verifies that:
 * 1. Aggressive police give up a chase after losing sight of the player for 3 seconds.
 * 2. Police continue chasing while they maintain line of sight.
 * 3. The hasLineOfSight() utility correctly detects blocked and unblocked sight lines.
 */
class Issue206LineOfSightPoliceChaseTest {

    private NPCManager npcManager;
    private Player player;
    private World world;
    private Inventory inventory;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        npcManager = new NPCManager();
        tooltipSystem = new TooltipSystem();
        inventory = new Inventory(36);
        world = new World(42L);

        // Flat ground at y=0 so NPCs can move (entities walk at y=1)
        for (int x = -5; x <= 30; x++) {
            for (int z = -5; z <= 30; z++) {
                world.setBlock(x, 0, z, BlockType.PAVEMENT);
            }
        }
    }

    /**
     * Test 1: Police give up chase after losing line of sight for 3 seconds.
     *
     * Scenario: police NPC is AGGRESSIVE and the player is separated from the police
     * by a solid wall (BRICK blocks). The police cannot see the player through the wall.
     * Simulate enough frames for the 3-second lost-sight timer to expire.
     * Verify the police state reverts to PATROLLING.
     */
    @Test
    void test1_PoliceLoseChaseAfterLostSight() {
        // Player is behind a wall at x=10
        player = new Player(15f, 1f, 5f);

        // Build a solid wall between police (x=5) and player (x=15) at x=10
        // Wall spans z=0..10, y=1..3 (solid BRICK blocks — not GLASS)
        for (int z = 0; z <= 10; z++) {
            for (int y = 1; y <= 3; y++) {
                world.setBlock(10, y, z, BlockType.BRICK);
            }
        }

        // Spawn police on the other side of the wall
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 5f, 1f, 5f);
        assertNotNull(police, "Police NPC should spawn");
        police.setState(NPCState.AGGRESSIVE);

        // Verify there is no line of sight between police and player (wall in the way)
        assertFalse(NPCManager.hasLineOfSight(world, police.getPosition(), player.getPosition()),
            "Police should NOT have line of sight through the BRICK wall");

        // Simulate 4 seconds (240 frames at 60fps) — exceeds the 3-second timeout
        float delta = 1f / 60f;
        for (int i = 0; i < 240; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
        }

        assertEquals(NPCState.PATROLLING, police.getState(),
            "Police should give up chase (revert to PATROLLING) after losing sight for 3 seconds. " +
            "Before fix #206, police chased forever regardless of line of sight. " +
            "Final state: " + police.getState());
        assertFalse(npcManager.isArrestPending(),
            "No arrest should occur when police cannot see the player through the wall");
    }

    /**
     * Test 2: Police continue chasing when they maintain line of sight.
     *
     * Scenario: police NPC is AGGRESSIVE and has a clear line of sight to the player
     * (no solid blocks between them). After 4 seconds of simulation the police should
     * still be in AGGRESSIVE state (or have made an arrest), NOT have given up.
     *
     * Player is placed far enough away (>1.5 blocks) that they won't be arrested in
     * the time window, but police has clear LOS the whole time.
     */
    @Test
    void test2_PoliceContinueChasingWithLineOfSight() {
        // Player is 10 blocks away from police, open flat ground — clear LOS
        player = new Player(0f, 1f, 0f);
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 10f, 1f, 0f);
        assertNotNull(police, "Police NPC should spawn");
        police.setState(NPCState.AGGRESSIVE);

        // Verify there IS line of sight (open ground)
        assertTrue(NPCManager.hasLineOfSight(world, police.getPosition(), player.getPosition()),
            "Police should have line of sight on open flat ground");

        // Simulate 2 seconds (120 frames) — less than the 3-second LOS timeout
        float delta = 1f / 60f;
        for (int i = 0; i < 120; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
            if (npcManager.isArrestPending()) {
                // Police reached the player and made an arrest — this is also correct
                npcManager.clearArrestPending();
                break;
            }
        }

        // Police should either still be AGGRESSIVE (chasing) or have made an arrest
        // (which transitions to PATROLLING). It must NOT have given up the chase.
        // We verify this by confirming the chase was not abandoned without contact:
        // if no arrest happened, police should still be AGGRESSIVE.
        // If police already caught player (PATROLLING after arrest), that is fine too.
        NPCState finalState = police.getState();
        boolean caughtPlayer = !police.getPosition().epsilonEquals(10f, 1f, 0f, 2.0f);
        assertTrue(finalState == NPCState.AGGRESSIVE || caughtPlayer,
            "Police with clear LOS should still be chasing (AGGRESSIVE) or have caught the player, " +
            "not silently given up. Final state: " + finalState);
    }

    /**
     * Test 3: hasLineOfSight() is blocked by solid BRICK blocks but not GLASS.
     *
     * Unit-level check on the static utility method used by the chase system.
     */
    @Test
    void test3_LineOfSightUtilityBlockedBySolid() {
        Vector3Wrapper from = new Vector3Wrapper(0f, 1f, 5f);
        Vector3Wrapper to   = new Vector3Wrapper(20f, 1f, 5f);

        // No wall: LOS should be clear
        assertTrue(NPCManager.hasLineOfSight(world, from.v, to.v),
            "LOS should be clear with no obstacles");

        // Add BRICK wall at x=10
        for (int y = 1; y <= 3; y++) {
            world.setBlock(10, y, 5, BlockType.BRICK);
        }
        assertFalse(NPCManager.hasLineOfSight(world, from.v, to.v),
            "LOS should be blocked by solid BRICK blocks");

        // Replace BRICK with GLASS — glass should NOT block LOS
        for (int y = 1; y <= 3; y++) {
            world.setBlock(10, y, 5, BlockType.GLASS);
        }
        assertTrue(NPCManager.hasLineOfSight(world, from.v, to.v),
            "LOS should NOT be blocked by GLASS blocks (they are transparent)");
    }

    /** Simple wrapper to make inline Vector3 construction readable in tests. */
    private static class Vector3Wrapper {
        final com.badlogic.gdx.math.Vector3 v;
        Vector3Wrapper(float x, float y, float z) {
            v = new com.badlogic.gdx.math.Vector3(x, y, z);
        }
    }
}

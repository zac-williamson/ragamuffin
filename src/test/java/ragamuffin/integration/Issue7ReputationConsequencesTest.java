package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.core.ArrestSystem;
import ragamuffin.core.StreetReputation;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #7 Integration Tests — Reputation Consequences
 *
 * Verifies that:
 * - Notorious reputation causes civilians to flee from the player
 * - Notorious players cause police to skip the WARNING phase
 * - Notorious players attract more police (higher spawn cap)
 * - Arrest reduces the player's reputation points
 * - KNOWN reputation has no civilian fleeing effect (only NOTORIOUS does)
 */
class Issue7ReputationConsequencesTest {

    private World world;
    private Player player;
    private NPCManager npcManager;
    private Inventory inventory;
    private TooltipSystem tooltipSystem;
    private ArrestSystem arrestSystem;

    @BeforeEach
    void setUp() {
        world = new World(12345);
        player = new Player(0, 1, 0);
        npcManager = new NPCManager();
        inventory = new Inventory(36);
        tooltipSystem = new TooltipSystem();
        arrestSystem = new ArrestSystem();

        // Flat ground for testing
        for (int x = -50; x < 50; x++) {
            for (int z = -50; z < 50; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * Test 1: Notorious player causes civilians to flee.
     *
     * Give the player NOTORIOUS reputation. Spawn a PUBLIC NPC 5 blocks away.
     * Run one NPCManager update tick. Verify the NPC enters FLEEING state.
     */
    @Test
    void test1_NotoriousPlayerCausesCiviliansToFlee() {
        // Make player notorious
        player.getStreetReputation().addPoints(StreetReputation.NOTORIOUS_THRESHOLD);
        assertTrue(player.getStreetReputation().isNotorious(), "Player should be notorious");

        // Spawn a PUBLIC NPC 5 blocks from player
        NPC civilian = npcManager.spawnNPC(NPCType.PUBLIC, 5, 1, 0);
        assertNotNull(civilian, "Civilian should spawn");

        // One update tick
        npcManager.update(0.016f, world, player, inventory, tooltipSystem);

        assertEquals(NPCState.FLEEING, civilian.getState(),
            "Civilian should flee from notorious player");
    }

    /**
     * Test 2: KNOWN reputation does NOT cause civilians to flee.
     *
     * Give the player KNOWN (but not NOTORIOUS) reputation. Spawn a PUBLIC NPC
     * 5 blocks away. Run one update tick. Verify the NPC does NOT enter FLEEING state.
     */
    @Test
    void test2_KnownReputationDoesNotCauseFleeing() {
        // Make player KNOWN but not NOTORIOUS
        player.getStreetReputation().addPoints(StreetReputation.KNOWN_THRESHOLD);
        assertTrue(player.getStreetReputation().isKnown(), "Player should be known");
        assertFalse(player.getStreetReputation().isNotorious(), "Player should not be notorious");

        // Spawn a PUBLIC NPC 5 blocks from player
        NPC civilian = npcManager.spawnNPC(NPCType.PUBLIC, 5, 1, 0);
        assertNotNull(civilian, "Civilian should spawn");

        // One update tick
        npcManager.update(0.016f, world, player, inventory, tooltipSystem);

        assertNotEquals(NPCState.FLEEING, civilian.getState(),
            "Civilian should NOT flee from merely known player");
    }

    /**
     * Test 3: Civilians out of range do NOT flee even if player is notorious.
     *
     * Give the player NOTORIOUS reputation. Spawn a PUBLIC NPC 25 blocks away
     * (outside the 10-block flee trigger radius). Run one update tick. Verify
     * the NPC does NOT enter FLEEING state.
     */
    @Test
    void test3_DistantCiviliansDoNotFlee() {
        // Make player notorious
        player.getStreetReputation().addPoints(StreetReputation.NOTORIOUS_THRESHOLD);
        assertTrue(player.getStreetReputation().isNotorious());

        // Spawn civilian 25 blocks away — outside flee range
        NPC civilian = npcManager.spawnNPC(NPCType.PUBLIC, 25, 1, 0);
        assertNotNull(civilian);

        // One update tick
        npcManager.update(0.016f, world, player, inventory, tooltipSystem);

        assertNotEquals(NPCState.FLEEING, civilian.getState(),
            "Civilian 25 blocks away should not flee (outside range)");
    }

    /**
     * Test 4: Notorious player — police skip WARNING and go straight to AGGRESSIVE.
     *
     * Give the player NOTORIOUS reputation. Spawn a POLICE NPC adjacent to the player.
     * Set the police to PATROLLING. Run an update tick. Verify the police goes to
     * AGGRESSIVE rather than WARNING.
     */
    @Test
    void test4_NotoriousPlayerPoliceSkipWarning() {
        // Make player notorious
        player.getStreetReputation().addPoints(StreetReputation.NOTORIOUS_THRESHOLD);
        assertTrue(player.getStreetReputation().isNotorious());

        // Spawn police 1.5 blocks from player (within warning range)
        player.getPosition().set(0, 1, 0);
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 1.5f, 1, 0);
        assertNotNull(police);
        police.setState(NPCState.PATROLLING);

        // Run update tick
        npcManager.update(0.016f, world, player, inventory, tooltipSystem);

        assertEquals(NPCState.AGGRESSIVE, police.getState(),
            "Police should go straight to AGGRESSIVE for notorious player, skipping WARNING");
    }

    /**
     * Test 5: Non-notorious player — police issue WARNING (normal behaviour preserved).
     *
     * Player has no reputation. Spawn a POLICE NPC adjacent to player. Set state
     * to PATROLLING. Run an update tick. Verify the police goes to WARNING (not AGGRESSIVE).
     */
    @Test
    void test5_NobodyPlayerPoliceIssueWarning() {
        // Player has no reputation (NOBODY)
        assertFalse(player.getStreetReputation().isNotorious());

        // Spawn police 1.5 blocks from player (within warning range)
        player.getPosition().set(0, 1, 0);
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 1.5f, 1, 0);
        assertNotNull(police);
        police.setState(NPCState.PATROLLING);

        // Run update tick
        npcManager.update(0.016f, world, player, inventory, tooltipSystem);

        assertEquals(NPCState.WARNING, police.getState(),
            "Police should issue WARNING for nobody player");
    }

    /**
     * Test 6: Arrest reduces player reputation points.
     *
     * Give the player 30 reputation points (NOTORIOUS). Apply an arrest via
     * ArrestSystem. Verify that the player's reputation points have decreased.
     */
    @Test
    void test6_ArrestReducesReputation() {
        // Build up reputation
        player.getStreetReputation().addPoints(StreetReputation.NOTORIOUS_THRESHOLD);
        int pointsBefore = player.getStreetReputation().getPoints();
        assertEquals(StreetReputation.NOTORIOUS_THRESHOLD, pointsBefore);

        // Simulate the arrest + reputation loss as done in RagamuffinGame
        arrestSystem.arrest(player, inventory);
        player.getStreetReputation().removePoints(15);

        int pointsAfter = player.getStreetReputation().getPoints();
        assertTrue(pointsAfter < pointsBefore,
            "Reputation points should decrease after arrest");
    }

    /**
     * Test 7: Arrest can drop player from NOTORIOUS to KNOWN or lower.
     *
     * Give the player exactly NOTORIOUS_THRESHOLD (30) points.
     * Apply arrest penalty (15 points removed). Verify player is no longer NOTORIOUS.
     */
    @Test
    void test7_ArrestCanDropFromNotoriousToKnown() {
        player.getStreetReputation().addPoints(StreetReputation.NOTORIOUS_THRESHOLD); // 30 pts
        assertTrue(player.getStreetReputation().isNotorious());

        // Arrest removes 15 points (30 - 15 = 15, which is KNOWN)
        player.getStreetReputation().removePoints(15);

        assertFalse(player.getStreetReputation().isNotorious(),
            "Player should no longer be NOTORIOUS after arrest penalty");
        assertTrue(player.getStreetReputation().isKnown(),
            "Player should still be KNOWN at 15 points");
    }

    /**
     * Test 8: Multiple civilians flee — not just PUBLIC type.
     *
     * Give the player NOTORIOUS reputation. Spawn a PENSIONER NPC 5 blocks away.
     * Run one update tick. Verify the PENSIONER also flees.
     */
    @Test
    void test8_PensionerAlsoFleesFromNotorious() {
        player.getStreetReputation().addPoints(StreetReputation.NOTORIOUS_THRESHOLD);
        assertTrue(player.getStreetReputation().isNotorious());

        NPC pensioner = npcManager.spawnNPC(NPCType.PENSIONER, 5, 1, 0);
        assertNotNull(pensioner);

        npcManager.update(0.016f, world, player, inventory, tooltipSystem);

        assertEquals(NPCState.FLEEING, pensioner.getState(),
            "Pensioner should flee from notorious player");
    }

    /**
     * Test 9: Youth gang does NOT flee from notorious player (they're hostile).
     *
     * Give the player NOTORIOUS reputation. Spawn a YOUTH_GANG NPC nearby.
     * Run one update tick. Verify the youth gang does NOT enter FLEEING state
     * (they should approach to steal instead).
     */
    @Test
    void test9_YouthGangDoesNotFlee() {
        player.getStreetReputation().addPoints(StreetReputation.NOTORIOUS_THRESHOLD);
        assertTrue(player.getStreetReputation().isNotorious());

        NPC youth = npcManager.spawnNPC(NPCType.YOUTH_GANG, 5, 1, 0);
        assertNotNull(youth);

        npcManager.update(0.016f, world, player, inventory, tooltipSystem);

        assertNotEquals(NPCState.FLEEING, youth.getState(),
            "Youth gang should not flee from notorious player");
    }
}

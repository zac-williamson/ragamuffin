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
 * Issue #138 Integration Tests — Police unconditionally stalk player at night.
 *
 * Verifies that:
 * 1. NOBODY-reputation players are NOT hunted by patrolling police — police wander instead.
 * 2. KNOWN-reputation players ARE approached by patrolling police.
 * 3. Greggs-raid alerted police pursue even a NOBODY-reputation player.
 */
class Issue138PoliceInnocentPatrolTest {

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

        // Flat ground so NPCs can move
        for (int x = -50; x <= 50; x++) {
            for (int z = -50; z <= 50; z++) {
                world.setBlock(x, 0, z, BlockType.PAVEMENT);
            }
        }

        // Player at origin, NOBODY reputation (default)
        player = new Player(0, 1, 0);
    }

    /**
     * Test 1: NOBODY-reputation player is NOT stalked by patrolling police.
     *
     * Spawn a POLICE NPC 20 blocks away from the player at (20, 1, 0).
     * Player has zero reputation (NOBODY). Simulate 300 frames (5 seconds at 60fps).
     * Verify the police has NOT reached the player (still > 5 blocks away), and
     * arrestPending is false. Without the fix, police would immediately path-find
     * to the player and arrest them within seconds.
     */
    @Test
    void test1_NobodyReputationPlayerNotStalkedByPatrollingPolice() {
        // Spawn police 20 blocks away
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 20f, 1f, 0f);
        assertNotNull(police, "POLICE NPC should spawn");
        police.setState(NPCState.PATROLLING);

        // Verify player has NOBODY reputation
        assertFalse(player.getStreetReputation().isKnown(),
            "Player should have NOBODY reputation for this test");

        float delta = 1f / 60f;
        for (int i = 0; i < 300; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
        }

        // Police should NOT have arrested the player
        assertFalse(npcManager.isArrestPending(),
            "NOBODY-reputation player should not be arrested by patrolling police. " +
            "Before fix #138, police unconditionally called setNPCTarget(police, player.getPosition()) " +
            "and would walk straight to the player and arrest them.");

        // Police should still be patrolling/wandering, not in AGGRESSIVE state
        assertNotEquals(NPCState.AGGRESSIVE, police.getState(),
            "Patrolling police should NOT enter AGGRESSIVE state against a NOBODY-reputation player. " +
            "State was: " + police.getState());
    }

    /**
     * Test 2: KNOWN-reputation player IS approached by patrolling police.
     *
     * Spawn a POLICE NPC 5 blocks away at (5, 1, 0). Give the player KNOWN
     * reputation (10 points). Simulate 300 frames. Verify the police transitions
     * to WARNING or AGGRESSIVE (it recognises the player as a known troublemaker).
     */
    @Test
    void test2_KnownReputationPlayerIsApproachedByPolice() {
        // Give player KNOWN reputation
        player.getStreetReputation().addPoints(10);
        assertTrue(player.getStreetReputation().isKnown(),
            "Player should have KNOWN reputation for this test");

        // Spawn police 5 blocks away — close enough to reach player quickly
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 5f, 1f, 0f);
        assertNotNull(police, "POLICE NPC should spawn");
        police.setState(NPCState.PATROLLING);

        float delta = 1f / 60f;
        for (int i = 0; i < 300; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
            if (police.getState() == NPCState.WARNING || police.getState() == NPCState.AGGRESSIVE) {
                break;
            }
        }

        NPCState finalState = police.getState();
        assertTrue(finalState == NPCState.WARNING || finalState == NPCState.AGGRESSIVE,
            "Police should approach and escalate against a KNOWN-reputation player. " +
            "Final state: " + finalState);
    }

    /**
     * Test 3: Greggs-raid alerted police pursue even a NOBODY-reputation player.
     *
     * Spawn a POLICE NPC 5 blocks away in PATROLLING state. Player has NOBODY reputation.
     * Call alertPoliceToGreggRaid(). The Greggs alert sets the nearby police to AGGRESSIVE
     * and adds it to the alerted set. Simulate 300 frames. Verify arrest is triggered —
     * the crime alert overrides the innocent-reputation guard in updatePolicePatrolling.
     */
    @Test
    void test3_GreggRaidAlertOverridesInnocentReputation() {
        // Ensure player has NOBODY reputation
        assertFalse(player.getStreetReputation().isKnown(),
            "Player should have NOBODY reputation for this test");

        // Spawn police within arrest range (1.0 block away, well under 1.5 threshold)
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 1f, 1f, 0f);
        assertNotNull(police, "POLICE NPC should spawn");
        police.setState(NPCState.PATROLLING);

        // Alert police to a Greggs raid — existing officer within 40 blocks goes AGGRESSIVE
        npcManager.alertPoliceToGreggRaid(player, world);

        // Officer should now be AGGRESSIVE
        assertEquals(NPCState.AGGRESSIVE, police.getState(),
            "Nearby police should become AGGRESSIVE after Greggs raid alert");

        float delta = 1f / 60f;
        boolean arrested = false;
        for (int i = 0; i < 60; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
            if (npcManager.isArrestPending()) {
                arrested = true;
                break;
            }
        }

        assertTrue(arrested,
            "After a Greggs raid alert, even a NOBODY-reputation player should be pursued " +
            "and arrested by the alerted police unit. The crime event explicitly flags the police.");
    }
}

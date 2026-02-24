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

import com.badlogic.gdx.math.Vector3;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #463 Integration Tests — Police should attack/arrest player when attacked.
 *
 * Verifies that:
 * 1. Punching a PATROLLING police officer causes them to turn AGGRESSIVE.
 * 2. Punching a WARNING police officer causes them to turn AGGRESSIVE.
 * 3. Already AGGRESSIVE police remain AGGRESSIVE after being punched.
 * 4. A knocked-out police officer does not re-aggro after being punched.
 */
class Issue463PoliceAttackWhenPunchedTest {

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
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                world.setBlock(x, 0, z, BlockType.PAVEMENT);
            }
        }

        player = new Player(0, 1, 0);
    }

    /**
     * Test 1: Punching a PATROLLING police officer makes them AGGRESSIVE.
     *
     * Spawn a POLICE NPC in PATROLLING state. Punch it once.
     * Verify the police transitions to AGGRESSIVE state.
     */
    @Test
    void test1_PunchingPatrollingPoliceMakesThemAggressive() {
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 1.5f, 1f, 0f);
        assertNotNull(police, "POLICE NPC should spawn");
        police.setState(NPCState.PATROLLING);
        assertEquals(NPCState.PATROLLING, police.getState(), "Police should start PATROLLING");

        // Punch toward the police (direction from player toward police)
        Vector3 punchDir = new Vector3(1f, 0f, 0f).nor();
        npcManager.punchNPC(police, punchDir, inventory, tooltipSystem);

        assertEquals(NPCState.AGGRESSIVE, police.getState(),
            "Police should become AGGRESSIVE after being punched by the player. " +
            "Before fix, punching police had no effect on their state.");
    }

    /**
     * Test 2: Punching a WARNING police officer makes them AGGRESSIVE immediately.
     *
     * Spawn a POLICE NPC in WARNING state. Punch it once.
     * Verify the police transitions to AGGRESSIVE state (skipping the normal 2-second escalation).
     */
    @Test
    void test2_PunchingWarningPoliceMakesThemAggressive() {
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 1.5f, 1f, 0f);
        assertNotNull(police, "POLICE NPC should spawn");
        police.setState(NPCState.WARNING);
        assertEquals(NPCState.WARNING, police.getState(), "Police should start in WARNING state");

        Vector3 punchDir = new Vector3(1f, 0f, 0f).nor();
        npcManager.punchNPC(police, punchDir, inventory, tooltipSystem);

        assertEquals(NPCState.AGGRESSIVE, police.getState(),
            "Police should become AGGRESSIVE immediately when punched, skipping warning escalation timer.");
    }

    /**
     * Test 3: AGGRESSIVE police remain AGGRESSIVE after being punched.
     *
     * Spawn a POLICE NPC already in AGGRESSIVE state. Punch it.
     * Verify the police stays in AGGRESSIVE state (doesn't regress).
     */
    @Test
    void test3_PunchingAlreadyAggressivePolicekeepsThemAggressive() {
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 1.5f, 1f, 0f);
        assertNotNull(police, "POLICE NPC should spawn");
        police.setState(NPCState.AGGRESSIVE);

        Vector3 punchDir = new Vector3(1f, 0f, 0f).nor();
        npcManager.punchNPC(police, punchDir, inventory, tooltipSystem);

        assertEquals(NPCState.AGGRESSIVE, police.getState(),
            "Already AGGRESSIVE police should remain AGGRESSIVE after being punched.");
    }

    /**
     * Test 4: Punching AGGRESSIVE police leads to arrest within simulation.
     *
     * Spawn a POLICE NPC in PATROLLING state near the player. Punch it to trigger
     * AGGRESSIVE state. Simulate frames to allow the police to close in and arrest.
     * Verify that arrestPending is set within a reasonable time.
     */
    @Test
    void test4_AggressivePoliceFromPunchEventuallyArrestsPlayer() {
        // Place police adjacent to player
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 1.0f, 1f, 0f);
        assertNotNull(police, "POLICE NPC should spawn");
        police.setState(NPCState.PATROLLING);

        // Punch the police — this should make them AGGRESSIVE
        Vector3 punchDir = new Vector3(1f, 0f, 0f).nor();
        npcManager.punchNPC(police, punchDir, inventory, tooltipSystem);

        assertEquals(NPCState.AGGRESSIVE, police.getState(),
            "Police should become AGGRESSIVE after punch");

        // Simulate frames — aggressive police should close in and arrest
        float delta = 1f / 60f;
        boolean arrested = false;
        for (int i = 0; i < 120; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
            if (npcManager.isArrestPending()) {
                arrested = true;
                npcManager.clearArrestPending();
                break;
            }
        }

        assertTrue(arrested,
            "Police made AGGRESSIVE by player punch should eventually arrest the player.");
    }

    /**
     * Test 5: Fix #487 — Punched police NPC is added to alertedPoliceNPCs.
     *
     * Before the fix, the punched officer was set AGGRESSIVE but never added to
     * alertedPoliceNPCs, so a NOBODY-reputation player would not be pursued.
     * This test verifies the officer is in alertedPoliceNPCs after the punch.
     */
    @Test
    void test5_PunchedPoliceAddedToAlertedSet() {
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 1.5f, 1f, 0f);
        assertNotNull(police, "POLICE NPC should spawn");
        police.setState(NPCState.PATROLLING);

        Vector3 punchDir = new Vector3(1f, 0f, 0f).nor();
        npcManager.punchNPC(police, punchDir, inventory, tooltipSystem, player.getPosition(), world);

        assertEquals(NPCState.AGGRESSIVE, police.getState(),
            "Punched police should be AGGRESSIVE");
        assertTrue(npcManager.isAlertedPolice(police),
            "Punched police NPC must be added to alertedPoliceNPCs so they pursue " +
            "NOBODY-reputation players (Fix #487).");
    }

    /**
     * Test 6: Fix #487 — Nearby police officers are alerted when one colleague is punched.
     *
     * Spawn a second POLICE officer within 20 blocks of the attacked officer.
     * After punching the first officer, the nearby backup officer should also become
     * AGGRESSIVE and be added to alertedPoliceNPCs.
     */
    @Test
    void test6_NearbyPoliceAlertedWhenColleaguePunched() {
        // First officer — will be punched
        NPC officer1 = npcManager.spawnNPC(NPCType.POLICE, 1.5f, 1f, 0f);
        assertNotNull(officer1, "Officer 1 should spawn");
        officer1.setState(NPCState.PATROLLING);

        // Second officer nearby (within 20 blocks) — should be alerted as backup
        NPC officer2 = npcManager.spawnNPC(NPCType.POLICE, 10f, 1f, 0f);
        assertNotNull(officer2, "Officer 2 should spawn");
        officer2.setState(NPCState.PATROLLING);

        Vector3 punchDir = new Vector3(1f, 0f, 0f).nor();
        npcManager.punchNPC(officer1, punchDir, inventory, tooltipSystem, player.getPosition(), world);

        assertEquals(NPCState.AGGRESSIVE, officer2.getState(),
            "Nearby police officer should become AGGRESSIVE when colleague is attacked (backup alert).");
        assertTrue(npcManager.isAlertedPolice(officer2),
            "Nearby backup officer should be added to alertedPoliceNPCs.");
    }
}

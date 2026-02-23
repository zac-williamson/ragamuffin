package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.GangTerritorySystem;
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
 * Issue #72 Integration Tests — AGGRESSIVE NPCs chase player.
 *
 * Verifies that:
 * 1. An AGGRESSIVE YOUTH_GANG NPC moves toward the player (not randomly).
 * 2. Territory-triggered hostility causes gang members to chase the player.
 * 3. An AGGRESSIVE NPC de-escalates to WANDERING when the player is >40 blocks away.
 */
class Issue72AggressiveNPCChaseTest {

    private NPCManager npcManager;
    private GangTerritorySystem gangTerritorySystem;
    private TooltipSystem tooltipSystem;
    private Player player;
    private World world;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        npcManager = new NPCManager();
        gangTerritorySystem = new GangTerritorySystem();
        tooltipSystem = new TooltipSystem();
        inventory = new Inventory(36);
        world = new World(42L);

        // Flat ground so pathfinding works
        for (int x = -80; x <= 20; x++) {
            for (int z = -80; z <= 20; z++) {
                world.setBlock(x, 0, z, BlockType.PAVEMENT);
            }
        }

        // Player starts outside any territory
        player = new Player(0, 1, 0);
    }

    /**
     * Test 1: AGGRESSIVE gang member moves toward player, not randomly.
     *
     * Spawn a YOUTH_GANG NPC at (−50, 1, −30). Place the player at (−48, 1, −30)
     * (2 blocks away). Set the NPC state to AGGRESSIVE. Simulate 60 frames
     * (1 second at 60fps). Verify the NPC's distance to the player has decreased
     * compared to its starting distance.
     */
    @Test
    void test1_AggressiveNPCMovesCloserToPlayer() {
        NPC gangMember = npcManager.spawnNPC(NPCType.YOUTH_GANG, -50f, 1f, -30f);
        assertNotNull(gangMember, "YOUTH_GANG NPC should spawn");
        gangMember.setState(NPCState.AGGRESSIVE);

        player.getPosition().set(-45f, 1f, -30f); // 5 blocks away

        float initialDist = gangMember.getPosition().dst(player.getPosition());

        // Simulate 300 frames (5 seconds) — allows for pathfinding throttle
        float delta = 1f / 60f;
        float closestDist = initialDist;
        for (int i = 0; i < 300; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
            float d = gangMember.getPosition().dst(player.getPosition());
            if (d < closestDist) closestDist = d;
        }

        assertTrue(closestDist < initialDist,
            "AGGRESSIVE YOUTH_GANG NPC should move closer to the player. " +
            "Initial dist: " + initialDist + ", closest dist: " + closestDist +
            ". Before fix, NPCs wandered randomly (defaulted to updateWandering).");
    }

    /**
     * Test 2: Territory-triggered hostility causes gangs to chase the player.
     *
     * Register "Bricky Estate" territory centred at (−50, −30). Spawn a YOUTH_GANG
     * NPC at (−55, 1, −30) inside the territory. Place the player at (−48, 1, −30)
     * (7 blocks from the NPC). Simulate LINGER_THRESHOLD_SECONDS + 2 seconds so the
     * territory turns HOSTILE and gangs are set AGGRESSIVE. Verify the NPC is AGGRESSIVE.
     * Then simulate an additional 60 frames and verify the NPC has moved closer to the player.
     */
    @Test
    void test2_TerritoryHostilityMakesGangChasePlayer() {
        gangTerritorySystem.addTerritory("Bricky Estate", -50f, -30f, 20f);

        // NPC spawns 7 blocks from the player so there is a non-zero starting distance
        NPC gangMember = npcManager.spawnNPC(NPCType.YOUTH_GANG, -55f, 1f, -30f);
        assertNotNull(gangMember, "YOUTH_GANG NPC should spawn inside territory");
        gangMember.setState(NPCState.WANDERING);

        // Player inside the territory but not at the NPC's position
        player.getPosition().set(-48f, 1f, -30f);

        // Simulate enough time for territory to turn HOSTILE and gangs to become AGGRESSIVE
        float lingerDelta = GangTerritorySystem.LINGER_THRESHOLD_SECONDS + 2.0f;
        gangTerritorySystem.update(0.1f, player, tooltipSystem, npcManager, world);
        gangTerritorySystem.update(lingerDelta, player, tooltipSystem, npcManager, world);

        assertEquals(NPCState.AGGRESSIVE, gangMember.getState(),
            "YOUTH_GANG NPC should be AGGRESSIVE after territory turns hostile");

        // Record distance immediately after going AGGRESSIVE
        float distAfterAggressive = gangMember.getPosition().dst(player.getPosition());
        assertTrue(distAfterAggressive > 0f, "NPC and player must be at different positions for this test");

        // Simulate 60 more frames — gang should now chase, not wander
        float delta = 1f / 60f;
        for (int i = 0; i < 60; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
        }

        float distAfterChase = gangMember.getPosition().dst(player.getPosition());

        assertTrue(distAfterChase < distAfterAggressive,
            "AGGRESSIVE gang NPC should move closer to the player after territory turns hostile. " +
            "Dist before chase: " + distAfterAggressive + ", dist after chase: " + distAfterChase);
    }

    /**
     * Test 3: De-escalation when player escapes beyond 40 blocks.
     *
     * Set a YOUTH_GANG NPC to AGGRESSIVE at (−50, 1, −30). Move the player to
     * (10, 1, 10) — well beyond 40 blocks. Simulate 60 frames. Verify the NPC
     * reverts to WANDERING.
     */
    @Test
    void test3_DeEscalatesWhenPlayerEscapes() {
        NPC gangMember = npcManager.spawnNPC(NPCType.YOUTH_GANG, -50f, 1f, -30f);
        assertNotNull(gangMember, "YOUTH_GANG NPC should spawn");
        gangMember.setState(NPCState.AGGRESSIVE);

        // Player is far away (>40 blocks)
        player.getPosition().set(10f, 1f, 10f);

        float dist = gangMember.getPosition().dst(player.getPosition());
        assertTrue(dist > 40f, "Player should be more than 40 blocks from NPC. Dist: " + dist);

        // Simulate 60 frames
        float delta = 1f / 60f;
        for (int i = 0; i < 60; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
        }

        assertEquals(NPCState.WANDERING, gangMember.getState(),
            "AGGRESSIVE NPC should de-escalate to WANDERING when player is >40 blocks away. " +
            "Before fix, the NPC had no AGGRESSIVE handler so this path was never reached.");
    }
}

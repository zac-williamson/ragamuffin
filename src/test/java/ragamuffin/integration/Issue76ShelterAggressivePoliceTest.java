package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.BlockPlacer;
import ragamuffin.building.Inventory;
import ragamuffin.core.ShelterDetector;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #76 Integration Tests — Shelter provides no protection against already-aggressive police.
 *
 * Verifies that:
 * 1. Aggressive police abandon chase when player enters a cardboard shelter.
 * 2. Police alerted by a Greggs raid cannot arrest a player already inside a shelter.
 * 3. Aggressive police resume chasing when the player leaves the shelter.
 */
class Issue76ShelterAggressivePoliceTest {

    private NPCManager npcManager;
    private TooltipSystem tooltipSystem;
    private Player player;
    private World world;
    private Inventory inventory;
    private BlockPlacer blockPlacer;

    // Shelter origin — ground level y=0, entities walk at y=1, shelter floor at y=1
    private static final int OX = 20, OY = 1, OZ = 20;

    @BeforeEach
    void setUp() {
        npcManager = new NPCManager();
        tooltipSystem = new TooltipSystem();
        inventory = new Inventory(36);
        blockPlacer = new BlockPlacer();
        world = new World(42L);

        // Flat ground at y=0 so pathfinding works (entities walk at y=1)
        for (int x = 0; x <= 50; x++) {
            for (int z = 0; z <= 50; z++) {
                world.setBlock(x, 0, z, BlockType.PAVEMENT);
            }
        }

        // Clear the shelter area (y=1..6, dx=-1..4, dz=-1..6 relative to OX,OY,OZ)
        for (int dx = -1; dx <= 4; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                for (int dz = -1; dz <= 7; dz++) {
                    world.setBlock(OX + dx, OY + dy, OZ + dz, BlockType.AIR);
                }
            }
        }

        // Build cardboard shelter at (OX, OY, OZ)
        blockPlacer.buildCardboardShelter(world, OX, OY, OZ);

        // Player starts at shelter interior position (centred in the 1-block gap)
        // Interior is at approximately (OX+1, OY+1, OZ+1)
        player = new Player(OX + 1.5f, OY + 1, OZ + 1.0f);
    }

    /**
     * Test 1: Aggressive police abandon chase when player enters shelter.
     *
     * Build a cardboard shelter. Spawn a police NPC in AGGRESSIVE state with player
     * inside the shelter. Simulate 120 frames. Verify the police NPC's state has
     * reverted to PATROLLING and arrestPending is false.
     */
    @Test
    void test1_AggressivePoliceLosesTargetWhenPlayerEntersShelter() {
        // Verify player position is actually sheltered before starting
        assertTrue(ShelterDetector.isSheltered(world, player.getPosition()),
            "Player should be inside shelter before test begins");

        // Spawn police 6 blocks away from shelter entrance, in AGGRESSIVE state
        NPC police = npcManager.spawnNPC(NPCType.POLICE, OX + 1.5f, OY + 1, OZ + 7f);
        assertNotNull(police, "Police NPC should spawn");
        police.setState(NPCState.AGGRESSIVE);

        // Simulate 120 frames (2 seconds at 60fps)
        float delta = 1f / 60f;
        for (int i = 0; i < 120; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
        }

        assertEquals(NPCState.PATROLLING, police.getState(),
            "AGGRESSIVE police should revert to PATROLLING when player is inside shelter. " +
            "Before fix, updatePoliceAggressive() had no shelter check so police continued chasing.");
        assertFalse(npcManager.isArrestPending(),
            "arrestPending should be false — sheltered player cannot be arrested.");
    }

    /**
     * Test 2: Greggs-raid police cannot arrest sheltered player.
     *
     * Set up a shelter. Place player inside. Call alertPoliceToGreggRaid() to spawn
     * an AGGRESSIVE police unit. Simulate 600 frames. Verify arrestPending remains false.
     */
    @Test
    void test2_GreggRaidPoliceCannotArrestSheltered() {
        // Verify player is sheltered before alerting police
        assertTrue(ShelterDetector.isSheltered(world, player.getPosition()),
            "Player should be inside shelter before alerting police");

        // Alert police as if player smashed a Greggs block
        npcManager.alertPoliceToGreggRaid(player, world);

        // Simulate 600 frames (10 seconds at 60fps)
        float delta = 1f / 60f;
        for (int i = 0; i < 600; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
        }

        assertFalse(npcManager.isArrestPending(),
            "arrestPending should remain false — shelter must protect against Greggs-raid police. " +
            "Before fix, alertPoliceToGreggRaid set AGGRESSIVE state with no shelter check, " +
            "allowing police to arrest the player regardless of shelter.");
    }

    /**
     * Test 3: Aggressive police resume chase when player leaves shelter.
     *
     * Spawn AGGRESSIVE police with player inside shelter — police reverts to PATROLLING.
     * Player then steps outside (unsheltered). Verify ShelterDetector confirms player is
     * now unsheltered, and that after advancing frames, police is no longer ignoring the player.
     */
    @Test
    void test3_AggressivePoliceResumeChaseWhenPlayerLeavesShelter() {
        // Spawn police outside the shelter in AGGRESSIVE state
        NPC police = npcManager.spawnNPC(NPCType.POLICE, OX + 1.5f, OY + 1, OZ + 6f);
        assertNotNull(police, "Police NPC should spawn");
        police.setState(NPCState.AGGRESSIVE);

        // Phase 1: player inside shelter — police reverts to PATROLLING
        float delta = 1f / 60f;
        for (int i = 0; i < 60; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
        }
        assertEquals(NPCState.PATROLLING, police.getState(),
            "Police should be PATROLLING while player is sheltered (pre-condition for test 3)");

        // Phase 2: player steps outside the shelter entrance.
        // The entrance is on the +Z face at approximately oz+2. Place player clearly outside.
        player.getPosition().set(OX + 1.5f, OY + 1, OZ + 4f);

        // Verify player is now unsheltered
        assertFalse(ShelterDetector.isSheltered(world, player.getPosition()),
            "Player outside shelter should NOT be detected as sheltered");

        // Place police close to the player (within detection range, 2 blocks away)
        police.getPosition().set(OX + 1.5f, OY + 1, OZ + 6f);

        // Simulate 180 frames (3 seconds) — police should detect unsheltered player
        // and approach within 2.0f, transitioning to WARNING or AGGRESSIVE
        for (int i = 0; i < 180; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
        }

        // The key behaviour: police is no longer treating the player as sheltered.
        // When PATROLLING, police approaches player and transitions to WARNING/AGGRESSIVE
        // once within 2.0f blocks.
        NPCState finalState = police.getState();
        assertNotEquals(NPCState.PATROLLING, finalState,
            "Police should no longer be PATROLLING after player leaves shelter — " +
            "it should have detected the unsheltered player and escalated. " +
            "Final state: " + finalState);
    }
}

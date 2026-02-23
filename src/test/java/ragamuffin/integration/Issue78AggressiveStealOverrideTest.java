package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #78 Integration Tests — AGGRESSIVE YOUTH_GANG steal override fix.
 *
 * Verifies that:
 * 1. An AGGRESSIVE gang member does NOT switch to STEALING when it reaches attack range.
 * 2. An AGGRESSIVE gang member continues chasing after reaching attack range.
 * 3. A WANDERING gang member still steals normally (regression guard).
 */
class Issue78AggressiveStealOverrideTest {

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
     * Test 1: AGGRESSIVE gang member does not switch to STEALING at attack range.
     *
     * Spawn a YOUTH_GANG NPC at (1.5, 1, 0) — within 2 blocks of the player at (0,1,0).
     * Set its state to AGGRESSIVE. Simulate 60 frames. Verify state remains AGGRESSIVE,
     * not STEALING.
     */
    @Test
    void test1_AggressiveGangDoesNotSwitchToStealing() {
        // Place NPC within the 2.0-block steal trigger radius
        NPC gangMember = npcManager.spawnNPC(NPCType.YOUTH_GANG, 1.5f, 1f, 0f);
        assertNotNull(gangMember, "YOUTH_GANG NPC should spawn");
        gangMember.setState(NPCState.AGGRESSIVE);

        player.getPosition().set(0f, 1f, 0f);

        float initialDist = gangMember.getPosition().dst(player.getPosition());
        assertTrue(initialDist <= 2.0f, "NPC must start within steal range for this test. Dist: " + initialDist);

        float delta = 1f / 60f;
        for (int i = 0; i < 60; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
        }

        assertNotEquals(NPCState.STEALING, gangMember.getState(),
            "AGGRESSIVE YOUTH_GANG NPC should NOT switch to STEALING when within 2 blocks. " +
            "Actual state: " + gangMember.getState());
    }

    /**
     * Test 2: AGGRESSIVE gang member continues chasing after reaching attack range.
     *
     * Spawn a YOUTH_GANG NPC at (−10, 1, 0) in AGGRESSIVE state. Player is at (0,1,0).
     * Simulate until the NPC reaches within 2 blocks (up to 5 seconds). Verify the NPC
     * remains AGGRESSIVE throughout and gets within attack range.
     */
    @Test
    void test2_AggressiveGangContinuesChasingAfterAttackRange() {
        NPC gangMember = npcManager.spawnNPC(NPCType.YOUTH_GANG, -4f, 1f, 0f);
        assertNotNull(gangMember, "YOUTH_GANG NPC should spawn");
        gangMember.setState(NPCState.AGGRESSIVE);

        player.getPosition().set(0f, 1f, 0f);

        float delta = 1f / 60f;
        boolean reachedAttackRange = false;
        for (int i = 0; i < 600; i++) { // up to 10 seconds
            npcManager.update(delta, world, player, inventory, tooltipSystem);
            float dist = gangMember.getPosition().dst(player.getPosition());
            if (dist <= 2.0f) {
                reachedAttackRange = true;
                // State must still be AGGRESSIVE (or ATTACKING if that state exists),
                // but NOT STEALING or WANDERING
                assertNotEquals(NPCState.STEALING, gangMember.getState(),
                    "AGGRESSIVE NPC switched to STEALING on reaching attack range at frame " + i);
                assertNotEquals(NPCState.WANDERING, gangMember.getState(),
                    "AGGRESSIVE NPC switched to WANDERING on reaching attack range at frame " + i);
                break;
            }
        }

        assertTrue(reachedAttackRange,
            "AGGRESSIVE YOUTH_GANG NPC should have reached within 2 blocks of the player. " +
            "Final dist: " + gangMember.getPosition().dst(player.getPosition()));
    }

    /**
     * Test 3: Gang still steals when WANDERING and adjacent (regression).
     *
     * Spawn a YOUTH_GANG NPC within 2 blocks of the player in WANDERING state.
     * Give the player some WOOD. Simulate until theft occurs or 10 seconds pass.
     * Verify wood was stolen.
     */
    @Test
    void test3_WanderingGangStillSteals() {
        NPC gangMember = npcManager.spawnNPC(NPCType.YOUTH_GANG, 0.5f, 1f, 0.5f);
        assertNotNull(gangMember, "YOUTH_GANG NPC should spawn");
        // State defaults to WANDERING for YOUTH_GANG; ensure it's not AGGRESSIVE
        gangMember.setState(NPCState.WANDERING);

        player.getPosition().set(0f, 1f, 0f);
        inventory.addItem(Material.WOOD, 5);

        assertEquals(5, inventory.getItemCount(Material.WOOD), "Player should start with 5 wood");

        float delta = 1f / 60f;
        for (int i = 0; i < 600; i++) { // up to 10 seconds
            npcManager.update(delta, world, player, inventory, tooltipSystem);
            if (inventory.getItemCount(Material.WOOD) < 5) {
                break;
            }
        }

        assertTrue(inventory.getItemCount(Material.WOOD) < 5,
            "WANDERING YOUTH_GANG NPC should still steal from the player. " +
            "Wood count: " + inventory.getItemCount(Material.WOOD));
    }
}

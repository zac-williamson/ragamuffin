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
 * Issue #80 Integration Tests — Patrolling police deal damage unconditionally.
 *
 * Verifies that:
 * 1. Patrolling police does NOT damage the player within 1.8 blocks.
 * 2. AGGRESSIVE police DOES attack the player in range.
 * 3. WARNING police does NOT attack the player.
 */
class Issue80PatrollingPoliceDamageTest {

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
     * Test 1: Patrolling police does not damage player within 1.8 blocks.
     *
     * Spawn a POLICE NPC at (1.0, 1, 0) — within 1.8 blocks of the player at (0,1,0).
     * Set its state to PATROLLING. Simulate 60 frames (1 second at 60fps).
     * Verify the player's HP is still 100 (unchanged).
     */
    @Test
    void test1_PatrollingPoliceDoesNotDamagePlayer() {
        // Place police within 1.8-block attack range
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 1.0f, 1f, 0f);
        assertNotNull(police, "POLICE NPC should spawn");
        police.setState(NPCState.PATROLLING);

        float initialDist = police.getPosition().dst(player.getPosition());
        assertTrue(initialDist <= 1.8f, "Police must start within 1.8 blocks for this test. Dist: " + initialDist);

        float initialHealth = player.getHealth();
        assertEquals(100f, initialHealth, "Player should start with 100 HP");

        float delta = 1f / 60f;
        for (int i = 0; i < 60; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
        }

        assertEquals(100f, player.getHealth(),
            "PATROLLING police should NOT damage the player. " +
            "Before fix, isHostile() caused police to attack unconditionally regardless of state. " +
            "Player HP: " + player.getHealth());
    }

    /**
     * Test 2: AGGRESSIVE police does attack player in range.
     *
     * Spawn a POLICE NPC at (1.0, 1, 0) — within 1.8 blocks of the player.
     * Set its state to AGGRESSIVE. Simulate 60 frames (1 second).
     * Verify the player's HP has decreased (police attacked).
     */
    @Test
    void test2_AggressivePoliceAttacksPlayerInRange() {
        // Place police within 1.8-block attack range
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 1.0f, 1f, 0f);
        assertNotNull(police, "POLICE NPC should spawn");
        police.setState(NPCState.AGGRESSIVE);

        float initialDist = police.getPosition().dst(player.getPosition());
        assertTrue(initialDist <= 1.8f, "Police must start within 1.8 blocks for this test. Dist: " + initialDist);

        assertEquals(100f, player.getHealth(), "Player should start with 100 HP");

        float delta = 1f / 60f;
        for (int i = 0; i < 60; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
        }

        assertTrue(player.getHealth() < 100f,
            "AGGRESSIVE police should attack and damage the player. " +
            "Player HP: " + player.getHealth());
    }

    /**
     * Test 3: WARNING police does not attack player.
     *
     * Spawn a POLICE NPC at (1.0, 1, 0) — within 1.8 blocks of the player.
     * Set its state to WARNING. Simulate 60 frames (1 second).
     * Verify the player's HP is unchanged (still 100).
     */
    @Test
    void test3_WarningPoliceDoesNotAttackPlayer() {
        // Place police within 1.8-block attack range
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 1.0f, 1f, 0f);
        assertNotNull(police, "POLICE NPC should spawn");
        police.setState(NPCState.WARNING);

        float initialDist = police.getPosition().dst(player.getPosition());
        assertTrue(initialDist <= 1.8f, "Police must start within 1.8 blocks for this test. Dist: " + initialDist);

        assertEquals(100f, player.getHealth(), "Player should start with 100 HP");

        float delta = 1f / 60f;
        for (int i = 0; i < 60; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
        }

        assertEquals(100f, player.getHealth(),
            "WARNING police should NOT attack the player. " +
            "Police in WARNING state should use the escalation system, not deal damage. " +
            "Player HP: " + player.getHealth());
    }
}

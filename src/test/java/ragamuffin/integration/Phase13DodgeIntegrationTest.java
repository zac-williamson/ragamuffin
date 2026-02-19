package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 13 Integration Tests: Dodge/Roll Mechanic
 * Tests dodge initiation, invincibility frames, energy cost, cooldown, and NPC interaction.
 */
class Phase13DodgeIntegrationTest {

    private World world;
    private Player player;
    private Inventory inventory;
    private NPCManager npcManager;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        world = new World(12345);
        world.generate();
        player = new Player(0, 5, 0);
        inventory = new Inventory(36);
        npcManager = new NPCManager();
        tooltipSystem = new TooltipSystem();
    }

    /**
     * Test 1: Dodge initiates correctly while moving.
     * Player has full energy, dodge in +X direction.
     * Verify isDodging() returns true and energy was consumed.
     */
    @Test
    void test1_DodgeInitiatesWhileMoving() {
        float energyBefore = player.getEnergy();
        boolean dodged = player.dodge(1, 0);

        assertTrue(dodged, "Dodge should initiate");
        assertTrue(player.isDodging(), "Player should be dodging");
        assertEquals(energyBefore - Player.DODGE_ENERGY_COST, player.getEnergy(), 0.01f);
    }

    /**
     * Test 2: Dodge grants invincibility frames — NPC attack misses during dodge.
     * Spawn hostile NPC adjacent to player. Start dodge. Trigger NPC attack.
     * Verify player takes no damage.
     */
    @Test
    void test2_DodgeGrantsInvincibility() {
        // Spawn hostile youth gang right next to player
        NPC hostile = npcManager.spawnNPC(NPCType.YOUTH_GANG, 0.5f, 5, 0.5f);
        hostile.setState(NPCState.AGGRESSIVE);

        float healthBefore = player.getHealth();

        // Player starts dodging
        player.dodge(1, 0);
        assertTrue(player.isDodging());

        // Run NPC update — the NPC should try to attack but miss due to i-frames
        npcManager.update(0.016f, world, player, inventory, tooltipSystem);

        assertEquals(healthBefore, player.getHealth(), 0.01f,
            "Player should take no damage during dodge i-frames");
    }

    /**
     * Test 3: Without dodging, NPC attack hits normally.
     * Same setup as test 2 but without dodging — verify damage is taken.
     */
    @Test
    void test3_NpcAttackHitsWithoutDodge() {
        // Spawn hostile NPC right on top of player
        NPC hostile = npcManager.spawnNPC(NPCType.YOUTH_GANG, 0.1f, 5, 0.1f);
        hostile.setState(NPCState.AGGRESSIVE);

        float healthBefore = player.getHealth();
        assertFalse(player.isDodging(), "Player should not be dodging");

        // Run multiple updates to let NPC attack
        for (int i = 0; i < 10; i++) {
            npcManager.update(0.1f, world, player, inventory, tooltipSystem);
        }

        assertTrue(player.getHealth() < healthBefore,
            "Player should take damage when not dodging");
    }

    /**
     * Test 4: Dodge cooldown prevents rapid dodging.
     * Dodge once, advance past dodge duration but not cooldown.
     * Attempt second dodge — should fail.
     */
    @Test
    void test4_DodgeCooldownPreventsRepeat() {
        player.dodge(1, 0);

        // End the dodge but not the cooldown
        player.updateDodge(Player.DODGE_DURATION + 0.01f);
        assertFalse(player.isDodging());
        assertFalse(player.canDodge(), "Should not be able to dodge during cooldown");

        boolean secondDodge = player.dodge(0, 1);
        assertFalse(secondDodge, "Second dodge should fail during cooldown");
    }

    /**
     * Test 5: Dodge fails with low energy.
     * Set energy below dodge cost and attempt dodge.
     */
    @Test
    void test5_DodgeFailsLowEnergy() {
        player.setEnergy(Player.DODGE_ENERGY_COST - 1);

        boolean dodged = player.dodge(1, 0);
        assertFalse(dodged, "Dodge should fail with insufficient energy");
        assertFalse(player.isDodging());
    }

    /**
     * Test 6: Dodge direction is preserved during the roll.
     * Dodge in a diagonal direction, check getDodgeDirX/Z are normalised.
     */
    @Test
    void test6_DodgeDirectionPreserved() {
        player.dodge(1, 1); // Diagonal

        float dirX = player.getDodgeDirX();
        float dirZ = player.getDodgeDirZ();
        float len = (float) Math.sqrt(dirX * dirX + dirZ * dirZ);

        assertEquals(1.0f, len, 0.01f, "Dodge direction should be normalised");
        assertTrue(dirX > 0, "X component should be positive");
        assertTrue(dirZ > 0, "Z component should be positive");
    }

    /**
     * Test 7: Dodge becomes available again after cooldown expires.
     */
    @Test
    void test7_DodgeAvailableAfterCooldown() {
        player.dodge(0, -1);

        // Wait out full cooldown + dodge duration
        player.updateDodge(Player.DODGE_DURATION + Player.DODGE_COOLDOWN + 0.1f);

        assertTrue(player.canDodge(), "Dodge should be available after cooldown");
        boolean secondDodge = player.dodge(1, 0);
        assertTrue(secondDodge, "Should be able to dodge again");
    }
}

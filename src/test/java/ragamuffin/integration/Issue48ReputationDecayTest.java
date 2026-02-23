package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
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
 * Issue #48 Integration Tests — StreetReputation Passive Decay
 *
 * Verifies that:
 * - After lying low for enough time, NOTORIOUS players can recover to a lower level
 * - Civilian NPCs stop fleeing once the player is no longer NOTORIOUS
 */
class Issue48ReputationDecayTest {

    private World world;
    private Player player;
    private NPCManager npcManager;
    private Inventory inventory;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        world = new World(12345);
        player = new Player(0, 1, 0);
        npcManager = new NPCManager();
        inventory = new Inventory(36);
        tooltipSystem = new TooltipSystem();

        // Flat ground for testing
        for (int x = -50; x < 50; x++) {
            for (int z = -50; z < 50; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * Test 1: Player can recover from NOTORIOUS by lying low — civilians no longer flee.
     *
     * Set reputation to 30 (NOTORIOUS). Verify civilians flee.
     * Advance DECAY_INTERVAL_SECONDS * 21 + 1 seconds with no crimes.
     * Verify reputation is now 9. Verify isNotorious() is false.
     * Verify that in the next simulation frame, civilian NPCs do NOT enter FLEEING state.
     */
    @Test
    void test1_PlayerCanRecoverFromNotoriousByLyingLow() {
        // Set up notorious reputation
        player.getStreetReputation().addPoints(StreetReputation.NOTORIOUS_THRESHOLD); // 30 pts
        assertTrue(player.getStreetReputation().isNotorious(), "Should start NOTORIOUS");

        // Verify civilians flee at this point
        NPC civilian = npcManager.spawnNPC(NPCType.PUBLIC, 5, 1, 0);
        assertNotNull(civilian);
        npcManager.update(0.016f, world, player, inventory, tooltipSystem);
        assertEquals(NPCState.FLEEING, civilian.getState(),
            "Civilian should flee from notorious player initially");

        // Now advance time to decay reputation: 21 full decay intervals + 1 second
        float totalTime = StreetReputation.DECAY_INTERVAL_SECONDS * 21 + 1f;
        for (float elapsed = 0; elapsed < totalTime; elapsed += 1f) {
            player.getStreetReputation().update(1f);
        }

        // Verify reputation has decayed below NOTORIOUS threshold
        assertEquals(9, player.getStreetReputation().getPoints(),
            "Reputation should be 9 after 21 decay ticks (30 - 21 = 9)");
        assertFalse(player.getStreetReputation().isNotorious(),
            "Player should no longer be NOTORIOUS after lying low");

        // Spawn a fresh civilian near the player
        NPC freshCivilian = npcManager.spawnNPC(NPCType.PUBLIC, 5, 1, 1);
        assertNotNull(freshCivilian);

        // Run one NPC update tick — civilian should NOT flee
        npcManager.update(0.016f, world, player, inventory, tooltipSystem);

        assertNotEquals(NPCState.FLEEING, freshCivilian.getState(),
            "Civilian should NOT flee from player who is no longer NOTORIOUS");
    }
}

package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.core.ArrestSystem;
import ragamuffin.core.HealingSystem;
import ragamuffin.core.RespawnSystem;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #166:
 * HealingSystem.restingTime resets after every respawn or arrest teleport.
 *
 * Root cause: after a teleport, lastPosition held the old position. The next
 * update() computed distance / delta which was enormous, exceeding
 * MOVEMENT_THRESHOLD and resetting restingTime to 0.
 *
 * Fix: after each teleport (respawn / arrest), call healingSystem.resetPosition()
 * so that lastPosition is synced to the new position before the first update().
 */
class Issue166HealingSystemTeleportTest {

    private Player player;
    private HealingSystem healingSystem;
    private RespawnSystem respawnSystem;
    private ArrestSystem arrestSystem;
    private TooltipSystem tooltipSystem;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        player = new Player(0, 1, 0);
        healingSystem = new HealingSystem();
        respawnSystem = new RespawnSystem();
        arrestSystem = new ArrestSystem();
        tooltipSystem = new TooltipSystem();
        inventory = new Inventory(36);
    }

    /**
     * Test 1: resetPosition() zeroes restingTime and syncs lastPosition.
     * After calling resetPosition with a position, a subsequent update() with the
     * player at that same position must NOT reset restingTime (speed == 0, player
     * is stationary).
     */
    @Test
    void resetPosition_syncsPosAndZerosRestingTime() {
        Vector3 parkPos = new Vector3(0, 2, 0);
        healingSystem.resetPosition(parkPos);

        assertEquals(0f, healingSystem.getRestingTime(), 0.001f,
                "resetPosition() should zero restingTime");

        // Place player at the synced position
        player.getPosition().set(parkPos);
        player.setHealth(50f);
        player.setHunger(100f);

        float delta = 1.0f / 60.0f;
        healingSystem.update(delta, player);

        // restingTime should have increased (player is stationary), not reset back to 0
        assertTrue(healingSystem.getRestingTime() > 0f,
                "restingTime should accumulate when player is stationary after resetPosition()");
    }

    /**
     * Test 2: Without resetPosition(), a large teleport causes a spurious speed
     * spike that resets restingTime. This verifies the old bug is detectable.
     */
    @Test
    void withoutResetPosition_teleportCausesSpuriousSpeedReset() {
        // Warm up lastPosition at a distant location
        player.getPosition().set(500f, 1f, 500f);
        healingSystem.update(1.0f / 60.0f, player);

        // Teleport player to park WITHOUT calling resetPosition
        player.getPosition().set(0f, 2f, 0f);
        player.setHunger(100f);

        float delta = 1.0f / 60.0f;
        healingSystem.update(delta, player);

        // restingTime should be 0 because the spurious speed exceeded MOVEMENT_THRESHOLD
        assertEquals(0f, healingSystem.getRestingTime(), 0.001f,
                "Without resetPosition(), a teleport should cause restingTime to reset to 0 (spurious speed bug)");
    }

    /**
     * Test 3: After resetPosition(), standing still for 5 seconds starts healing.
     * Simulates the correct post-respawn behaviour: player is placed at park,
     * resetPosition() is called, then player stands still for 5+ seconds and heals.
     */
    @Test
    void afterResetPosition_restingForFiveSecondsStartsHealing() {
        player.setHealth(60f);   // injured but not dead
        player.setHunger(100f);  // well-fed — healing condition met

        // Simulate respawn: place at park, reset HealingSystem
        Vector3 parkPos = new Vector3(0f, 2f, 0f);
        player.getPosition().set(parkPos);
        healingSystem.resetPosition(parkPos);

        float delta = 1.0f / 60.0f;
        int framesFor6Seconds = (int) (6.0f / delta); // 360 frames

        for (int i = 0; i < framesFor6Seconds; i++) {
            healingSystem.update(delta, player);
        }

        assertTrue(healingSystem.getRestingTime() >= HealingSystem.RESTING_DURATION_REQUIRED,
                "restingTime should reach 5s after standing still following resetPosition()");
        assertTrue(player.getHealth() > 60f,
                "Player should have gained health after resting 5+ seconds with enough hunger");
    }

    /**
     * Test 4: Simulates the respawn scenario end-to-end.
     * Kill the player, run through the respawn countdown, then call resetPosition()
     * (as RagamuffinGame does after the respawn completes). Verify that the first
     * update() after resetPosition() accumulates restingTime rather than resetting it.
     */
    @Test
    void respawnScenario_firstUpdateAfterResetPositionAccumulatesRestingTime() {
        // Kill the player
        player.setHealth(1f);
        player.damage(1f);
        assertTrue(player.isDead());

        // Begin respawn sequence
        respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
        assertTrue(respawnSystem.isRespawning());

        // Fast-forward through countdown
        boolean wasRespawning = respawnSystem.isRespawning();
        float bigDelta = RespawnSystem.RESPAWN_MESSAGE_DURATION + 0.1f;
        respawnSystem.update(bigDelta, player);
        boolean nowRespawning = respawnSystem.isRespawning();

        // Simulate what RagamuffinGame does in the post-respawn block
        if (wasRespawning && !nowRespawning) {
            healingSystem.resetPosition(player.getPosition());
        }

        assertFalse(nowRespawning, "Respawn should be complete");
        assertFalse(player.isDead(), "Player should be alive after respawn");

        // Now simulate one frame — player stands still at respawn point
        player.setHunger(100f);
        float delta = 1.0f / 60.0f;
        healingSystem.update(delta, player);

        assertTrue(healingSystem.getRestingTime() > 0f,
                "restingTime should accumulate on first update after resetPosition(), not reset to 0");
    }

    /**
     * Test 5: Simulates the arrest scenario end-to-end.
     * Place player far away, arrest them (teleport to park), call resetPosition(),
     * then verify the next update() accumulates restingTime.
     */
    @Test
    void arrestScenario_firstUpdateAfterResetPositionAccumulatesRestingTime() {
        // Warm up HealingSystem at a distant position
        player.getPosition().set(500f, 1f, 500f);
        healingSystem.update(1.0f / 60.0f, player);

        // Arrest teleports player to park
        player.setHealth(100f);
        player.setHunger(100f);
        arrestSystem.arrest(player, inventory);

        // Simulate what RagamuffinGame does after arrest
        healingSystem.resetPosition(player.getPosition());

        // First frame after arrest — player is stationary at park
        float delta = 1.0f / 60.0f;
        healingSystem.update(delta, player);

        assertTrue(healingSystem.getRestingTime() > 0f,
                "restingTime should accumulate on first update after arrest resetPosition(), not reset to 0");
    }
}

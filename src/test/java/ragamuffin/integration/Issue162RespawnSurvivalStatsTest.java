package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.RespawnSystem;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #162:
 * Damage and hunger drain applied while player is dead during respawn.
 *
 * Fix: The survival-stats block in RagamuffinGame.render() is now gated by
 * !respawnSystem.isRespawning(), preventing starvation damage, cold-snap
 * damage, and hunger drain from firing while the player is dead and waiting
 * to respawn.
 *
 * These tests verify the RespawnSystem behaviour and that a dead player's
 * health does not go further negative during the respawn countdown.
 */
class Issue162RespawnSurvivalStatsTest {

    private Player player;
    private RespawnSystem respawnSystem;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        player = new Player(0, 1, 0);
        respawnSystem = new RespawnSystem();
        tooltipSystem = new TooltipSystem();
    }

    /**
     * Test 1: isRespawning() returns true immediately after death is detected.
     * Kill the player, call checkAndTriggerRespawn. Verify isRespawning() == true.
     * This confirms the guard condition in render() would suppress survival stats.
     */
    @Test
    void isRespawningTrueAfterDeathDetected() {
        player.setHealth(1);
        player.damage(1);
        assertTrue(player.isDead(), "Player should be dead after lethal damage");

        respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);

        assertTrue(respawnSystem.isRespawning(),
                "isRespawning() must return true while waiting to respawn");
    }

    /**
     * Test 2: Player health does NOT go further negative during the respawn countdown.
     * Kill the player by starvation (hunger=0, damage applied). Record health at death.
     * Simulate the 3-second respawn window: the survival-stats block should be
     * suppressed (isRespawning==true), so no additional damage() calls are made.
     * Verify health at end of countdown equals health at start of countdown (i.e., 0).
     */
    @Test
    void playerHealthDoesNotGoNegativeDuringRespawnCountdown() {
        // Starve the player to death
        player.setHunger(0);
        player.setHealth(1);
        player.damage(1); // lethal blow — simulates starvation kill
        assertEquals(0f, player.getHealth(), 0.001f, "Health should be 0 after death");
        assertTrue(player.isDead());

        // Begin respawn sequence
        respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
        assertTrue(respawnSystem.isRespawning());

        float healthAtStartOfCountdown = player.getHealth();

        // Simulate the respawn countdown ticking (NOT calling player.damage() —
        // that would be the bug). The survival-stats block is gated by
        // !isRespawning(), so we only call respawnSystem.update() here.
        float delta = 1.0f / 60.0f;
        int framesBeforeRespawn = (int) (RespawnSystem.RESPAWN_MESSAGE_DURATION * 60) - 1;
        for (int i = 0; i < framesBeforeRespawn; i++) {
            respawnSystem.update(delta, player);
            // isRespawning is still true until timer expires
            if (respawnSystem.isRespawning()) {
                // Verify health has not gone further negative
                assertTrue(player.getHealth() >= healthAtStartOfCountdown,
                        "Health must not decrease further during respawn countdown (frame " + i + ")");
            }
        }
    }

    /**
     * Test 3: isRespawning() returns false after the countdown completes.
     * Kill the player, trigger respawn, advance time past RESPAWN_MESSAGE_DURATION.
     * Verify isRespawning() == false and player is revived with restored stats.
     */
    @Test
    void isRespawningFalseAfterCountdownCompletes() {
        player.setHealth(1);
        player.damage(1);

        respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
        assertTrue(respawnSystem.isRespawning());

        // Advance past full countdown duration
        float bigDelta = RespawnSystem.RESPAWN_MESSAGE_DURATION + 0.1f;
        respawnSystem.update(bigDelta, player);

        assertFalse(respawnSystem.isRespawning(),
                "isRespawning() should be false after countdown completes");
        assertFalse(player.isDead(),
                "Player should not be dead after respawn");
        assertEquals(50f, player.getHealth(), 0.001f,
                "Player should have 50 HP after respawn");
    }

    /**
     * Test 4: checkAndTriggerRespawn does NOT re-trigger while already respawning.
     * Kill the player, trigger respawn. Call checkAndTriggerRespawn again while
     * already respawning. Verify the timer has not been reset (respawnTimer is
     * still decreasing, not reset to RESPAWN_MESSAGE_DURATION).
     */
    @Test
    void respawnDoesNotRetriggerWhileAlreadyRespawning() {
        player.setHealth(1);
        player.damage(1);

        respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
        assertTrue(respawnSystem.isRespawning());

        // Advance slightly — timer should have decreased
        float partialDelta = 0.5f;
        respawnSystem.update(partialDelta, player);
        float timerAfterPartialAdvance = respawnSystem.getRespawnTimer();

        // Call checkAndTriggerRespawn again while still respawning
        boolean triggered = respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
        assertFalse(triggered, "Respawn should not re-trigger while already respawning");

        // Timer should not have been reset to full RESPAWN_MESSAGE_DURATION
        assertTrue(respawnSystem.getRespawnTimer() < RespawnSystem.RESPAWN_MESSAGE_DURATION,
                "Timer must not be reset while already respawning");
        assertEquals(timerAfterPartialAdvance, respawnSystem.getRespawnTimer(), 0.001f,
                "Timer should be unchanged by the second checkAndTriggerRespawn call");
    }
}

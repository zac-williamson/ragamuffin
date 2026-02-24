package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.RespawnSystem;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #269:
 * Player input not blocked during death/respawn screen.
 *
 * Fix: The input-gate condition in RagamuffinGame.render() now includes
 * !respawnSystem.isRespawning() && !player.isDead() so that updatePlayingInput()
 * is suppressed while the player is dead and during the 3-second respawn countdown.
 *
 * These tests verify that the guard conditions (isDead and isRespawning) correctly
 * identify when input should be suppressed.
 */
class Issue269DeadPlayerInputBlockTest {

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
     * Test 1: Input gate condition is false when player is dead (before respawn triggers).
     * Kill the player. Verify player.isDead() == true, confirming the input gate
     * (!player.isDead()) would suppress updatePlayingInput().
     */
    @Test
    void inputGateSuppressedWhenPlayerDead() {
        assertFalse(player.isDead(), "Player should be alive initially");

        player.setHealth(1);
        player.damage(1);

        assertTrue(player.isDead(),
                "player.isDead() must be true after lethal damage — input gate must suppress updatePlayingInput()");
    }

    /**
     * Test 2: Input gate condition is false during the full respawn countdown.
     * Kill the player, trigger respawn. For every frame during the countdown,
     * verify that isRespawning() == true, confirming input would be suppressed
     * throughout the entire death screen.
     */
    @Test
    void inputGateSuppressedThroughoutRespawnCountdown() {
        player.setHealth(1);
        player.damage(1);
        assertTrue(player.isDead());

        respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
        assertTrue(respawnSystem.isRespawning(),
                "isRespawning() must be true immediately after death is detected");

        // Simulate the full respawn countdown minus the last frame
        float delta = 1.0f / 60.0f;
        int framesBeforeRespawn = (int) (RespawnSystem.RESPAWN_MESSAGE_DURATION * 60) - 1;
        for (int i = 0; i < framesBeforeRespawn; i++) {
            assertTrue(respawnSystem.isRespawning() || player.isDead(),
                    "Input must be blocked (isRespawning or isDead) on frame " + i);
            respawnSystem.update(delta, player);
        }
    }

    /**
     * Test 3: Input gate is restored after respawn completes.
     * Kill the player, trigger and complete respawn. Verify that after the countdown
     * both isRespawning() == false and isDead() == false, allowing input to resume.
     */
    @Test
    void inputGateRestoredAfterRespawnCompletes() {
        player.setHealth(1);
        player.damage(1);
        assertTrue(player.isDead());

        respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
        assertTrue(respawnSystem.isRespawning());

        // Advance past the full countdown
        respawnSystem.update(RespawnSystem.RESPAWN_MESSAGE_DURATION + 0.1f, player);

        assertFalse(respawnSystem.isRespawning(),
                "isRespawning() must be false after countdown completes — input gate should open");
        assertFalse(player.isDead(),
                "player.isDead() must be false after respawn — input gate should open");
    }

    /**
     * Test 4: A healthy (alive, not respawning) player does NOT have the input gate blocked.
     * Verify the positive case: normal gameplay should not be incorrectly suppressed.
     */
    @Test
    void inputGateOpenForAlivePlayer() {
        assertFalse(player.isDead(), "Alive player: isDead() must be false");
        assertFalse(respawnSystem.isRespawning(), "No respawn in progress: isRespawning() must be false");

        // The input gate condition: !isRespawning() && !isDead() must be true for a normal player
        boolean inputAllowed = !respawnSystem.isRespawning() && !player.isDead();
        assertTrue(inputAllowed,
                "Input must be allowed for a healthy, non-respawning player");
    }
}

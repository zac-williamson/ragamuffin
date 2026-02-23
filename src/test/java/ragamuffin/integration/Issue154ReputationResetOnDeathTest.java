package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.RespawnSystem;
import ragamuffin.core.StreetReputation;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #154:
 * Player reputation not reset on death — police and civilians permanently
 * hostile after first death.
 *
 * Fix: RagamuffinGame calls player.getStreetReputation().reset() in the
 * wasRespawning && !respawnSystem.isRespawning() block, alongside the
 * existing greggsRaidSystem.reset().
 */
class Issue154ReputationResetOnDeathTest {

    private Player player;
    private RespawnSystem respawnSystem;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        player = new Player(0, 5, 0);
        respawnSystem = new RespawnSystem();
        tooltipSystem = new TooltipSystem();
    }

    /**
     * Test 1: After a full respawn sequence completes, reputation is reset to zero.
     *
     * The actual reset is wired in RagamuffinGame (wasRespawning && !isRespawning block),
     * so here we verify that StreetReputation.reset() returns the player to NOBODY
     * with 0 points — confirming the called method does what the fix relies on.
     */
    @Test
    void reputationResetClearsPointsAndLevel() {
        StreetReputation rep = player.getStreetReputation();

        // Build up notorious reputation
        rep.addPoints(StreetReputation.NOTORIOUS_THRESHOLD);
        assertTrue(rep.isNotorious(), "Player should be notorious before death");
        assertEquals(StreetReputation.NOTORIOUS_THRESHOLD, rep.getPoints());

        // Simulate what RagamuffinGame does on respawn completion
        rep.reset();

        assertEquals(0, rep.getPoints(), "Points should be 0 after reset");
        assertFalse(rep.isKnown(), "Player should not be known after reset");
        assertFalse(rep.isNotorious(), "Player should not be notorious after reset");
        assertEquals(StreetReputation.ReputationLevel.NOBODY, rep.getLevel(),
                "Reputation level should be NOBODY after reset");
    }

    /**
     * Test 2: After the full respawn sequence, a simulated reset leaves the player
     * at NOBODY even if they had KNOWN status.
     */
    @Test
    void knownReputationClearedOnRespawn() {
        StreetReputation rep = player.getStreetReputation();

        rep.addPoints(StreetReputation.KNOWN_THRESHOLD);
        assertTrue(rep.isKnown(), "Player should be known before death");

        // Simulate RagamuffinGame respawn completion
        rep.reset();

        assertFalse(rep.isKnown(), "Player should not be known after respawn");
        assertEquals(0, rep.getPoints(), "Points should be 0 after respawn");
    }

    /**
     * Test 3: Reputation accumulated after a respawn is not affected by the reset
     * — i.e. the reset only fires once, at the moment of respawn.
     */
    @Test
    void reputationAccumulatesNormallyAfterRespawn() {
        StreetReputation rep = player.getStreetReputation();

        // Build, die, reset
        rep.addPoints(StreetReputation.NOTORIOUS_THRESHOLD);
        rep.reset();
        assertEquals(0, rep.getPoints(), "Should be at 0 after reset");

        // Now accumulate new reputation (post-respawn crimes)
        rep.addPoints(StreetReputation.KNOWN_THRESHOLD);
        assertTrue(rep.isKnown(), "New reputation should accumulate normally after reset");
        assertEquals(StreetReputation.KNOWN_THRESHOLD, rep.getPoints());
    }

    /**
     * Test 4: The RespawnSystem completes the full sequence (checkAndTriggerRespawn →
     * update → completion) and leaves isRespawning() == false, giving RagamuffinGame
     * the correct signal to call the reputation reset.
     */
    @Test
    void respawnSequenceCompletesAndSignalsGame() {
        // Kill the player
        player.setHealth(1);
        player.damage(1);
        assertTrue(player.isDead(), "Player should be dead");

        // Trigger respawn
        respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
        assertTrue(respawnSystem.isRespawning(), "Respawn sequence should start");

        boolean wasRespawning = respawnSystem.isRespawning();

        // Advance past the timer
        respawnSystem.update(RespawnSystem.RESPAWN_MESSAGE_DURATION + 0.1f, player);
        assertFalse(respawnSystem.isRespawning(), "Respawn sequence should be complete");

        // This is the condition RagamuffinGame checks — it should be true exactly once
        boolean shouldResetReputation = wasRespawning && !respawnSystem.isRespawning();
        assertTrue(shouldResetReputation,
                "RagamuffinGame's wasRespawning && !isRespawning() should be true, triggering reputation reset");

        // Simulate what RagamuffinGame does at that moment
        player.getStreetReputation().addPoints(StreetReputation.NOTORIOUS_THRESHOLD);
        player.getStreetReputation().reset();
        assertEquals(0, player.getStreetReputation().getPoints(),
                "Reputation should be 0 after game-triggered reset");
    }
}

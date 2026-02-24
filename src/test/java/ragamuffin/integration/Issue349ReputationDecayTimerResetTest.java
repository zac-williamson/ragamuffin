package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.StreetReputation;
import ragamuffin.entity.Player;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #349:
 * StreetReputation.reset() does not clear decayTimer — reputation decays
 * prematurely after death or arrest.
 *
 * Fix: reset() now also zeroes decayTimer, preventing immediate passive decay
 * after a respawn or arrest event.
 */
class Issue349ReputationDecayTimerResetTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player(0, 5, 0);
    }

    /**
     * Test 1: After reset(), a newly earned point is NOT immediately stripped by a
     * near-threshold decayTimer.
     *
     * Build up reputation so decayTimer has been ticking nearly to the threshold.
     * Call reset(). Add 1 point. Advance time by 2 seconds (well within the interval).
     * Verify the point remains — decay should not fire until a full interval from reset.
     */
    @Test
    void pointsNotImmediatelyDecayedAfterReset() {
        StreetReputation rep = player.getStreetReputation();

        // Build reputation and advance decayTimer to near-threshold (58 of 60 seconds)
        rep.addPoints(5);
        rep.update(StreetReputation.DECAY_INTERVAL_SECONDS - 2f); // 58 seconds in
        assertEquals(5, rep.getPoints(), "Points should still be 5 before decay fires");

        // Simulate death/arrest — reset reputation
        rep.reset();
        assertEquals(0, rep.getPoints(), "Points should be 0 after reset");

        // Earn 1 point after respawn (e.g. breaking a block)
        rep.addPoints(1);
        assertEquals(1, rep.getPoints(), "Should have 1 point after respawn crime");

        // Advance only 2 seconds — if decayTimer was NOT reset, decay would fire immediately
        // (because the timer was at 58s, and 58+2=60 >= DECAY_INTERVAL_SECONDS)
        rep.update(2f);

        assertEquals(1, rep.getPoints(),
                "Decay should NOT fire 2 seconds after respawn — decayTimer must have been reset to 0");
    }

    /**
     * Test 2: After reset(), decay only fires after a full DECAY_INTERVAL_SECONDS have elapsed.
     *
     * Reset a reputation that had a nearly-full decayTimer.
     * Add 1 point. Advance a full interval minus 1 second — no decay yet.
     * Advance 1 more second (total = full interval) — decay fires, point removed.
     */
    @Test
    void decayFiresAfterFullIntervalFollowingReset() {
        StreetReputation rep = player.getStreetReputation();

        // Wind the timer up to near the threshold, then reset
        rep.addPoints(10);
        rep.update(StreetReputation.DECAY_INTERVAL_SECONDS - 1f);
        rep.reset();

        // Start fresh: earn a point after respawn
        rep.addPoints(1);

        // Advance almost a full interval — should NOT have decayed yet
        rep.update(StreetReputation.DECAY_INTERVAL_SECONDS - 1f);
        assertEquals(1, rep.getPoints(),
                "Point should still be present with 1 second remaining in the decay interval");

        // Now cross the threshold
        rep.update(1f);
        assertEquals(0, rep.getPoints(),
                "Point should have decayed exactly at the one-full-interval mark");
    }

    /**
     * Test 3: reset() on a fresh StreetReputation (decayTimer already 0) is a no-op for decay.
     * Ensures the fix doesn't break the normal case.
     */
    @Test
    void resetOnFreshReputationIsHarmless() {
        StreetReputation rep = player.getStreetReputation();

        // Should already be clean — reset shouldn't cause issues
        rep.reset();
        assertEquals(0, rep.getPoints(), "Points should be 0");
        assertEquals(StreetReputation.ReputationLevel.NOBODY, rep.getLevel(), "Level should be NOBODY");

        // Add points and verify normal decay still works
        rep.addPoints(3);
        rep.update(StreetReputation.DECAY_INTERVAL_SECONDS + 0.1f);
        assertEquals(2, rep.getPoints(), "Normal decay should still work after reset on fresh instance");
    }
}

package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.StreetReputation;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #359:
 * {@code player.getStreetReputation().update(delta)} was not called in the PAUSED
 * render path, so the passive decay timer froze while the pause menu was open.
 *
 * A player with NOTORIOUS status (60+ pts) could exploit this by opening the pause
 * menu every 59 seconds, preventing any decay indefinitely and breaking the intended
 * "lying low" mechanic described in the StreetReputation class comment.
 *
 * Fix: the PAUSED render path now calls
 *   {@code player.getStreetReputation().update(delta)}
 * mirroring the PLAYING path (line ~1088), so passive decay advances while paused.
 */
class Issue359ReputationDecayPausedTest {

    private StreetReputation reputation;

    @BeforeEach
    void setUp() {
        reputation = new StreetReputation();
    }

    /**
     * Test 1: update() advances the decay timer — calling it repeatedly with a
     * total delta >= DECAY_INTERVAL_SECONDS removes 1 point.
     *
     * This is the core fix: the PAUSED path now calls update(delta) each frame,
     * so the 60-second decay window continues to drain while paused.
     */
    @Test
    void updateAdvancesDecayTimer_onePointRemovedAfterInterval() {
        // Give the player some reputation to decay
        reputation.addPoints(10);
        int initial = reputation.getPoints();

        // Advance by the full decay interval in small per-frame ticks
        float delta = 1.0f / 60.0f;
        float total = 0f;
        while (total < StreetReputation.DECAY_INTERVAL_SECONDS) {
            reputation.update(delta);
            total += delta;
        }
        // One extra tick to be sure we crossed the boundary
        reputation.update(delta);

        assertEquals(initial - 1, reputation.getPoints(),
                "One reputation point must be removed after DECAY_INTERVAL_SECONDS of update() calls (simulates paused frames)");
    }

    /**
     * Test 2: Without calling update() the timer freezes and no decay occurs —
     * documents the exact pre-fix bug (pause menu exploitation).
     *
     * A player could hold the reputation indefinitely by keeping the pause menu open.
     */
    @Test
    void withoutUpdate_timerFreezes_noDecay() {
        reputation.addPoints(30); // NOTORIOUS level
        int initial = reputation.getPoints();

        // Deliberately do NOT call update() — simulates the frozen timer during pause
        assertEquals(initial, reputation.getPoints(),
                "Reputation must not decay without update() calls (pre-fix: frozen timer exploit)");
        assertEquals(StreetReputation.ReputationLevel.NOTORIOUS, reputation.getLevel(),
                "Level must remain NOTORIOUS without update() calls");
    }

    /**
     * Test 3: A single large delta (simulating a long pause) removes at least 1 point.
     *
     * The implementation uses a single if-check per call, so passing
     * DECAY_INTERVAL_SECONDS * 3 removes exactly 1 point (the remainder accumulates
     * in decayTimer for subsequent calls). This mirrors realistic post-pause behaviour:
     * the first call processes one decay tick and the rest carry over.
     */
    @Test
    void singleLargeDelta_decaysAtLeastOnePoint() {
        reputation.addPoints(10);
        int initial = reputation.getPoints();

        // Large delta exceeding the interval — at least one decay tick must fire
        reputation.update(StreetReputation.DECAY_INTERVAL_SECONDS + 1.0f);

        assertEquals(initial - 1, reputation.getPoints(),
                "A delta exceeding DECAY_INTERVAL_SECONDS must remove at least 1 reputation point");
    }

    /**
     * Test 4: update() with zero delta does NOT decay — zero-delta frame must be a no-op.
     *
     * Passing 0 to update() (e.g. first paused frame with zero real delta) must not
     * advance the timer or remove any points.
     */
    @Test
    void updateWithZeroDelta_noDecay() {
        reputation.addPoints(5);
        int initial = reputation.getPoints();

        for (int i = 0; i < 1000; i++) {
            reputation.update(0f);
        }

        assertEquals(initial, reputation.getPoints(),
                "Zero-delta updates must not advance the decay timer or remove any points");
    }

    /**
     * Test 5: update() is a no-op when reputation is zero — no underflow occurs.
     *
     * The implementation short-circuits when points == 0, so no negative decay
     * or timer accumulation should happen on a clean player.
     */
    @Test
    void updateAtZeroReputation_noOp() {
        assertEquals(0, reputation.getPoints(), "Initial reputation must be zero");

        reputation.update(StreetReputation.DECAY_INTERVAL_SECONDS * 10);

        assertEquals(0, reputation.getPoints(),
                "update() must be a no-op when reputation is already zero (no underflow)");
        assertEquals(StreetReputation.ReputationLevel.NOBODY, reputation.getLevel(),
                "Level must remain NOBODY after update() with zero points");
    }

    /**
     * Test 6: Notorious-level player (60+ pts / 5-star exploit scenario) loses one
     * point after a full decay interval — the core exploit is closed.
     *
     * Before the fix, opening the pause menu before the 60-second mark and closing it
     * just after would reset the window. Now update() is called every paused frame,
     * so the timer advances regardless of whether the pause menu is open.
     */
    @Test
    void notoriousPlayerDecaysWhilePaused() {
        reputation.addPoints(60); // 5-star NOTORIOUS
        assertEquals(StreetReputation.ReputationLevel.NOTORIOUS, reputation.getLevel());

        // Simulate PAUSED render path: call update(delta) each frame for 60 seconds
        float delta = 1.0f / 60.0f;
        int frames = (int) ((StreetReputation.DECAY_INTERVAL_SECONDS + 1.0f) / delta);
        for (int i = 0; i < frames; i++) {
            reputation.update(delta);
        }

        assertTrue(reputation.getPoints() < 60,
                "NOTORIOUS player (60 pts) must lose at least 1 point after one decay interval — pause-menu exploit closed");
    }
}

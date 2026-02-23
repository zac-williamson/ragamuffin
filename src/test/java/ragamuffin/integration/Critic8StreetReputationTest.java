package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.StreetReputation;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Critic 8 Integration Tests — Street Reputation System
 *
 * Verifies that:
 * - StreetReputation starts at NOBODY level with zero points
 * - Adding points advances through KNOWN and NOTORIOUS levels
 * - Removing points can drop reputation level back down
 * - isKnown and isNotorious reflect the correct state
 * - reset() returns the system to initial state
 * - Thresholds are honored precisely (edge cases)
 */
class Critic8StreetReputationTest {

    private StreetReputation reputation;

    @BeforeEach
    void setUp() {
        reputation = new StreetReputation();
    }

    /**
     * Test 1: Initial state is NOBODY with zero points.
     */
    @Test
    void test1_InitialStateIsNobody() {
        assertEquals(StreetReputation.ReputationLevel.NOBODY, reputation.getLevel(),
            "Reputation should start at NOBODY");
        assertEquals(0, reputation.getPoints(), "Should start with zero points");
        assertFalse(reputation.isKnown(), "Should not be known initially");
        assertFalse(reputation.isNotorious(), "Should not be notorious initially");
    }

    /**
     * Test 2: Adding points below KNOWN threshold keeps level at NOBODY.
     */
    @Test
    void test2_BelowKnownThresholdStaysNobody() {
        reputation.addPoints(StreetReputation.KNOWN_THRESHOLD - 1);

        assertEquals(StreetReputation.ReputationLevel.NOBODY, reputation.getLevel(),
            "Should remain NOBODY below KNOWN threshold");
        assertEquals(StreetReputation.KNOWN_THRESHOLD - 1, reputation.getPoints());
        assertFalse(reputation.isKnown(), "Should not be known below threshold");
    }

    /**
     * Test 3: Reaching KNOWN threshold advances to KNOWN level.
     */
    @Test
    void test3_ReachingKnownThresholdAdvancesToKnown() {
        reputation.addPoints(StreetReputation.KNOWN_THRESHOLD);

        assertEquals(StreetReputation.ReputationLevel.KNOWN, reputation.getLevel(),
            "Should advance to KNOWN at threshold");
        assertTrue(reputation.isKnown(), "isKnown should return true");
        assertFalse(reputation.isNotorious(), "Should not be notorious at KNOWN level");
    }

    /**
     * Test 4: Adding points above KNOWN but below NOTORIOUS keeps level at KNOWN.
     */
    @Test
    void test4_BetweenKnownAndNotoriousStaysKnown() {
        int midpoint = (StreetReputation.KNOWN_THRESHOLD + StreetReputation.NOTORIOUS_THRESHOLD) / 2;
        reputation.addPoints(midpoint);

        assertEquals(StreetReputation.ReputationLevel.KNOWN, reputation.getLevel(),
            "Should be KNOWN between thresholds");
        assertTrue(reputation.isKnown());
        assertFalse(reputation.isNotorious());
    }

    /**
     * Test 5: Reaching NOTORIOUS threshold advances to NOTORIOUS level.
     */
    @Test
    void test5_ReachingNotoriousThresholdAdvancesToNotorious() {
        reputation.addPoints(StreetReputation.NOTORIOUS_THRESHOLD);

        assertEquals(StreetReputation.ReputationLevel.NOTORIOUS, reputation.getLevel(),
            "Should advance to NOTORIOUS at threshold");
        assertTrue(reputation.isKnown(), "isKnown should return true at NOTORIOUS");
        assertTrue(reputation.isNotorious(), "isNotorious should return true");
    }

    /**
     * Test 6: Points above NOTORIOUS threshold keep level at NOTORIOUS.
     */
    @Test
    void test6_AboveNotoriousThresholdStaysNotorious() {
        reputation.addPoints(StreetReputation.NOTORIOUS_THRESHOLD + 100);

        assertEquals(StreetReputation.ReputationLevel.NOTORIOUS, reputation.getLevel(),
            "Should stay NOTORIOUS above threshold");
        assertEquals(StreetReputation.NOTORIOUS_THRESHOLD + 100, reputation.getPoints(),
            "Points should accumulate");
    }

    /**
     * Test 7: Removing points can drop level from NOTORIOUS to KNOWN.
     */
    @Test
    void test7_RemovingPointsDropsFromNotoriousToKnown() {
        reputation.addPoints(StreetReputation.NOTORIOUS_THRESHOLD + 5);
        assertEquals(StreetReputation.ReputationLevel.NOTORIOUS, reputation.getLevel());

        reputation.removePoints(10); // Drop below NOTORIOUS threshold

        assertEquals(StreetReputation.ReputationLevel.KNOWN, reputation.getLevel(),
            "Should drop to KNOWN when points fall below NOTORIOUS threshold");
        assertFalse(reputation.isNotorious(), "Should not be notorious after drop");
        assertTrue(reputation.isKnown(), "Should still be known");
    }

    /**
     * Test 8: Removing points can drop level from KNOWN to NOBODY.
     */
    @Test
    void test8_RemovingPointsDropsFromKnownToNobody() {
        reputation.addPoints(StreetReputation.KNOWN_THRESHOLD + 3);
        assertEquals(StreetReputation.ReputationLevel.KNOWN, reputation.getLevel());

        reputation.removePoints(5); // Drop below KNOWN threshold

        assertEquals(StreetReputation.ReputationLevel.NOBODY, reputation.getLevel(),
            "Should drop to NOBODY when points fall below KNOWN threshold");
        assertFalse(reputation.isKnown(), "Should not be known after drop");
    }

    /**
     * Test 9: Removing more points than available floors at zero (no negative).
     */
    @Test
    void test9_RemovingPointsFloorsAtZero() {
        reputation.addPoints(5);
        reputation.removePoints(100);

        assertEquals(0, reputation.getPoints(), "Points should floor at zero");
        assertEquals(StreetReputation.ReputationLevel.NOBODY, reputation.getLevel(),
            "Level should be NOBODY at zero points");
    }

    /**
     * Test 10: reset() returns system to initial NOBODY state.
     */
    @Test
    void test10_ResetClearsState() {
        reputation.addPoints(StreetReputation.NOTORIOUS_THRESHOLD + 50);
        assertTrue(reputation.isNotorious(), "Should be notorious before reset");

        reputation.reset();

        assertEquals(StreetReputation.ReputationLevel.NOBODY, reputation.getLevel(),
            "Level should be NOBODY after reset");
        assertEquals(0, reputation.getPoints(), "Points should be 0 after reset");
        assertFalse(reputation.isKnown(), "Should not be known after reset");
        assertFalse(reputation.isNotorious(), "Should not be notorious after reset");
    }

    /**
     * Test 11: Multiple small additions accumulate correctly.
     */
    @Test
    void test11_MultipleSmallAdditionsAccumulate() {
        for (int i = 0; i < 15; i++) {
            reputation.addPoints(2); // 15 * 2 = 30 points
        }

        assertEquals(30, reputation.getPoints(), "Points should accumulate to 30");
        assertEquals(StreetReputation.ReputationLevel.NOTORIOUS, reputation.getLevel(),
            "Should reach NOTORIOUS at exactly 30 points");
    }

    /**
     * Test 12: Edge case — one point below each threshold stays at lower level.
     */
    @Test
    void test12_OnePointBelowThresholdsStaysAtLowerLevel() {
        // One below KNOWN
        reputation.addPoints(StreetReputation.KNOWN_THRESHOLD - 1);
        assertEquals(StreetReputation.ReputationLevel.NOBODY, reputation.getLevel(),
            "Should be NOBODY at KNOWN threshold - 1");

        // Advance to one below NOTORIOUS
        reputation.addPoints(StreetReputation.NOTORIOUS_THRESHOLD - StreetReputation.KNOWN_THRESHOLD);
        assertEquals(StreetReputation.ReputationLevel.KNOWN, reputation.getLevel(),
            "Should be KNOWN at NOTORIOUS threshold - 1");
    }

    // ====== Star count tests (Issue #8: GTA-style star display) ======

    /**
     * Test 13: Zero points yields zero stars.
     */
    @Test
    void test13_ZeroPointsYieldsZeroStars() {
        assertEquals(0, reputation.getStarCount(), "No points should give 0 stars");
    }

    /**
     * Test 14: Just below KNOWN threshold still gives zero stars.
     */
    @Test
    void test14_BelowKnownThresholdYieldsZeroStars() {
        reputation.addPoints(StreetReputation.KNOWN_THRESHOLD - 1);
        assertEquals(0, reputation.getStarCount(), "Below KNOWN threshold should still be 0 stars");
    }

    /**
     * Test 15: Exactly at KNOWN threshold gives 1 star.
     */
    @Test
    void test15_AtKnownThresholdYieldsOneStar() {
        reputation.addPoints(StreetReputation.KNOWN_THRESHOLD); // 10 pts
        assertEquals(1, reputation.getStarCount(), "10 points should give 1 star");
    }

    /**
     * Test 16: 20 points gives 2 stars.
     */
    @Test
    void test16_TwentyPointsYieldsTwoStars() {
        reputation.addPoints(20);
        assertEquals(2, reputation.getStarCount(), "20 points should give 2 stars");
    }

    /**
     * Test 17: At NOTORIOUS threshold (30 pts) gives 3 stars.
     */
    @Test
    void test17_AtNotoriousThresholdYieldsThreeStars() {
        reputation.addPoints(StreetReputation.NOTORIOUS_THRESHOLD); // 30 pts
        assertEquals(3, reputation.getStarCount(), "30 points should give 3 stars");
    }

    /**
     * Test 18: 45 points gives 4 stars.
     */
    @Test
    void test18_FortyFivePointsYieldsFourStars() {
        reputation.addPoints(45);
        assertEquals(4, reputation.getStarCount(), "45 points should give 4 stars");
    }

    /**
     * Test 19: 60 points gives 5 stars (maximum).
     */
    @Test
    void test19_SixtyPointsYieldsFiveStars() {
        reputation.addPoints(60);
        assertEquals(5, reputation.getStarCount(), "60 points should give 5 stars");
    }

    /**
     * Test 20: Far above 60 still caps at 5 stars.
     */
    @Test
    void test20_MassivePointsCapAtFiveStars() {
        reputation.addPoints(1000);
        assertEquals(5, reputation.getStarCount(), "Star count should never exceed 5");
    }

    /**
     * Test 21: Star count decreases when points are removed.
     */
    @Test
    void test21_StarCountDecreasesWhenPointsRemoved() {
        reputation.addPoints(45);
        assertEquals(4, reputation.getStarCount());

        reputation.removePoints(20); // Drop to 25 pts — still KNOWN, 2 stars
        assertEquals(2, reputation.getStarCount(),
            "Star count should decrease when reputation points are lost");
    }

    // ====== Issue #48: Passive decay tests ======

    /**
     * Test 22: Reputation decays passively over time.
     *
     * Give the player 30 reputation points (NOTORIOUS). Advance the simulation for
     * DECAY_INTERVAL_SECONDS * 30 + 1 seconds without any criminal actions.
     * Verify points are 0 and isNotorious() is false.
     */
    @Test
    void test22_ReputationDecaysPassivelyOverTime() {
        reputation.addPoints(StreetReputation.NOTORIOUS_THRESHOLD); // 30 pts
        assertTrue(reputation.isNotorious(), "Should start NOTORIOUS");

        // Simulate DECAY_INTERVAL_SECONDS * 30 + 1 seconds of non-criminal time
        float totalTime = StreetReputation.DECAY_INTERVAL_SECONDS * 30 + 1f;
        // Advance in 1-second steps
        for (float elapsed = 0; elapsed < totalTime; elapsed += 1f) {
            reputation.update(1f);
        }

        assertEquals(0, reputation.getPoints(),
            "Reputation should have decayed to 0 after 30 minutes of non-criminal play");
        assertFalse(reputation.isNotorious(),
            "Player should no longer be NOTORIOUS after decay");
    }

    /**
     * Test 23: Decay does not go below zero.
     *
     * Give the player 5 reputation points. Advance for DECAY_INTERVAL_SECONDS * 10 seconds.
     * Verify points are clamped at 0 (not negative).
     */
    @Test
    void test23_DecayDoesNotGoBelowZero() {
        reputation.addPoints(5);
        assertEquals(5, reputation.getPoints());

        // Advance far more time than needed to decay 5 points
        float totalTime = StreetReputation.DECAY_INTERVAL_SECONDS * 10;
        for (float elapsed = 0; elapsed < totalTime; elapsed += 1f) {
            reputation.update(1f);
        }

        assertEquals(0, reputation.getPoints(),
            "Reputation points should be clamped at 0, never go negative");
        assertFalse(reputation.isKnown(), "Should be NOBODY at 0 points");
    }

    /**
     * Test 24: Criminal activity does not reset decay timer — timer continues running.
     *
     * Give the player 10 reputation points. Advance for DECAY_INTERVAL_SECONDS * 0.9
     * seconds (just below one decay tick). Add 2 more points (simulating punching an NPC).
     * Verify points are now 12. Advance another DECAY_INTERVAL_SECONDS * 0.9 seconds.
     * After a full interval total of DECAY_INTERVAL_SECONDS * 1.8 from the start,
     * verify points have decreased by 1 from peak (now 11).
     */
    @Test
    void test24_CriminalActivityDoesNotResetDecayTimer() {
        reputation.addPoints(10);
        assertEquals(10, reputation.getPoints());

        // Advance 0.9 intervals — just under one decay tick
        float interval = StreetReputation.DECAY_INTERVAL_SECONDS;
        float step = interval * 0.9f;

        // Advance in small increments
        float elapsed = 0;
        while (elapsed < step) {
            float tick = Math.min(1f, step - elapsed);
            reputation.update(tick);
            elapsed += tick;
        }

        // Verify no decay yet
        assertEquals(10, reputation.getPoints(), "No decay tick should have fired yet");

        // Commit a crime: punch an NPC (+2 pts)
        reputation.addPoints(2);
        assertEquals(12, reputation.getPoints(), "Points should now be 12 after crime");

        // Advance another 0.9 intervals — the cumulative timer now exceeds one interval
        elapsed = 0;
        while (elapsed < step) {
            float tick = Math.min(1f, step - elapsed);
            reputation.update(tick);
            elapsed += tick;
        }

        // The timer has now accumulated 1.8 intervals total — one decay tick fired
        assertEquals(11, reputation.getPoints(),
            "One decay tick should have fired (12 - 1 = 11)");
    }

    /**
     * Test 25: Player can recover from NOTORIOUS by lying low — points reach KNOWN level.
     *
     * Set reputation to 30 (NOTORIOUS). Advance DECAY_INTERVAL_SECONDS * 21 + 1 seconds
     * with no crimes. Verify reputation is now 9 (KNOWN, not NOTORIOUS).
     * Verify isNotorious() is false.
     */
    @Test
    void test25_PlayerCanRecoverFromNotoriousByLyingLow() {
        reputation.addPoints(StreetReputation.NOTORIOUS_THRESHOLD); // 30 pts
        assertTrue(reputation.isNotorious(), "Should start NOTORIOUS");

        // Advance 21 full decay intervals + 1 second
        float totalTime = StreetReputation.DECAY_INTERVAL_SECONDS * 21 + 1f;
        for (float elapsed = 0; elapsed < totalTime; elapsed += 1f) {
            reputation.update(1f);
        }

        assertEquals(9, reputation.getPoints(),
            "Reputation should be 9 after 21 decay ticks (30 - 21 = 9)");
        assertFalse(reputation.isNotorious(),
            "Player should no longer be NOTORIOUS");
        // 9 pts is below KNOWN_THRESHOLD (10), so level is NOBODY
        assertFalse(reputation.isKnown(),
            "Player at 9 pts is below KNOWN threshold — should be NOBODY");
    }
}

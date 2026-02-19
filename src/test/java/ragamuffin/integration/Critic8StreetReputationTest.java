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
}

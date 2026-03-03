package ragamuffin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.LocalElectionSystem.Candidate;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LocalElectionSystem} (Issue #1414).
 */
class LocalElectionSystemTest {

    private LocalElectionSystem system;
    private AchievementSystem achievementSystem;
    private NotorietySystem.AchievementCallback achievementCallback;
    private CriminalRecord criminalRecord;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        system = new LocalElectionSystem(new Random(42));
        achievementSystem = new AchievementSystem();
        achievementCallback = type -> achievementSystem.unlock(type);
        criminalRecord = new CriminalRecord();
        notorietySystem = new NotorietySystem();
        wantedSystem = new WantedSystem();
        inventory = new Inventory(36);
        inventory.addItem(Material.POSTAL_VOTE_BUNDLE, 5);
    }

    // ── Calendar query tests ───────────────────────────────────────────────────

    @Test
    void testElectionActiveOnDay90() {
        assertTrue(system.isPollingDay(90),
                "isPollingDay() should return true for day 90");
    }

    @Test
    void testElectionInactiveOnDay89() {
        assertFalse(system.isPollingDay(89),
                "isPollingDay() should return false for day 89");
    }

    @Test
    void testCanvassingWeekDays83To89() {
        assertTrue(system.isCanvassingWeek(83),
                "isCanvassingWeek() should be true on day 83");
        assertTrue(system.isCanvassingWeek(86),
                "isCanvassingWeek() should be true mid-week");
        assertTrue(system.isCanvassingWeek(89),
                "isCanvassingWeek() should be true on day 89");
        assertFalse(system.isCanvassingWeek(90),
                "isCanvassingWeek() should be false on day 90 (Polling Day)");
        assertFalse(system.isCanvassingWeek(82),
                "isCanvassingWeek() should be false before canvassing starts");
    }

    @Test
    void testPollingStationOpenHours() {
        assertTrue(system.isPollingStationOpen(90, 10.0f),
                "Polling station should be open at 10:00 on day 90");
        assertFalse(system.isPollingStationOpen(90, 6.0f),
                "Polling station should be closed before 07:00");
        assertFalse(system.isPollingStationOpen(90, 22.5f),
                "Polling station should be closed after 22:00");
        assertFalse(system.isPollingStationOpen(89, 10.0f),
                "Polling station should be closed on day 89");
    }

    // ── Canvassing mechanic tests ──────────────────────────────────────────────

    @Test
    void testTacticalVoterAwardedAfterAllThreePledges() {
        assertFalse(achievementSystem.isUnlocked(AchievementType.TACTICAL_VOTER));

        system.pledgeSupport(Candidate.HOLT, achievementCallback);
        assertFalse(achievementSystem.isUnlocked(AchievementType.TACTICAL_VOTER),
                "TACTICAL_VOTER should not be awarded after only one pledge");

        system.pledgeSupport(Candidate.BRANNIGAN, achievementCallback);
        assertFalse(achievementSystem.isUnlocked(AchievementType.TACTICAL_VOTER),
                "TACTICAL_VOTER should not be awarded after two pledges");

        system.pledgeSupport(Candidate.PATEL, achievementCallback);
        assertTrue(achievementSystem.isUnlocked(AchievementType.TACTICAL_VOTER),
                "TACTICAL_VOTER should be awarded after pledging to all three");
    }

    @Test
    void testTacticalVoterNotDuplicated() {
        system.pledgeSupport(Candidate.HOLT, achievementCallback);
        system.pledgeSupport(Candidate.BRANNIGAN, achievementCallback);
        system.pledgeSupport(Candidate.PATEL, achievementCallback);
        system.pledgeSupport(Candidate.PATEL, achievementCallback); // again
        assertTrue(system.isTacticalVoterAwarded(), "Should only be awarded once");
    }

    // ── Postal vote fraud tests ────────────────────────────────────────────────

    @Test
    void testPostalVoteFraudUndetected() {
        // Force no detection by overriding detection risk to 0
        system.setDetectionRiskForTesting(0.0f);
        int before = system.getVotes(Candidate.HOLT);

        boolean success = system.submitPostalVote(inventory, Candidate.HOLT,
                criminalRecord, notorietySystem, wantedSystem, null, achievementCallback, null);

        assertTrue(success, "Postal vote should succeed with 0% detection risk");
        assertEquals(before + LocalElectionSystem.POSTAL_VOTES_ADDED, system.getVotes(Candidate.HOLT),
                "Holt's votes should increase by POSTAL_VOTES_ADDED");
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.ELECTORAL_FRAUD),
                "No ELECTORAL_FRAUD crime should be recorded on success");
        assertEquals(0, notorietySystem.getNotoriety(),
                "No Notoriety should be added on success");
    }

    @Test
    void testPostalVoteFraudCaught() {
        // Force detection by overriding to 100%
        system.setDetectionRiskForTesting(1.0f);
        int before = system.getVotes(Candidate.HOLT);

        boolean success = system.submitPostalVote(inventory, Candidate.HOLT,
                criminalRecord, notorietySystem, wantedSystem, null, achievementCallback, null);

        assertFalse(success, "Postal vote should fail (detected) with 100% detection risk");
        assertEquals(before, system.getVotes(Candidate.HOLT),
                "Holt's votes should not change on detection");
        assertTrue(criminalRecord.getCount(CriminalRecord.CrimeType.ELECTORAL_FRAUD) > 0,
                "ELECTORAL_FRAUD should be recorded on detection");
        assertTrue(notorietySystem.getNotoriety() >= LocalElectionSystem.POSTAL_FRAUD_NOTORIETY,
                "Notoriety should increase by POSTAL_FRAUD_NOTORIETY on detection");
    }

    @Test
    void testBallotStufferAwardedOnSuccess() {
        system.setDetectionRiskForTesting(0.0f);
        assertFalse(achievementSystem.isUnlocked(AchievementType.BALLOT_STUFFER));

        system.submitPostalVote(inventory, Candidate.PATEL,
                criminalRecord, notorietySystem, wantedSystem, null, achievementCallback, null);

        assertTrue(achievementSystem.isUnlocked(AchievementType.BALLOT_STUFFER),
                "BALLOT_STUFFER should be awarded on successful postal fraud");
    }

    @Test
    void testBallotStufferNotAwardedOnDetection() {
        system.setDetectionRiskForTesting(1.0f);

        system.submitPostalVote(inventory, Candidate.PATEL,
                criminalRecord, notorietySystem, wantedSystem, null, achievementCallback, null);

        assertFalse(achievementSystem.isUnlocked(AchievementType.BALLOT_STUFFER),
                "BALLOT_STUFFER should NOT be awarded when caught");
    }

    // ── Polling Day mechanic tests ─────────────────────────────────────────────

    @Test
    void testFirstVoterAwardedOnFirstVote() {
        assertFalse(achievementSystem.isUnlocked(AchievementType.FIRST_VOTER));
        assertFalse(system.hasVoted());

        boolean result = system.castVote(inventory, Candidate.HOLT, achievementCallback);

        assertTrue(result, "First vote should be accepted");
        assertTrue(system.hasVoted(), "hasVoted should be true after voting");
        assertEquals(Candidate.HOLT, system.getVotedFor());
        assertTrue(achievementSystem.isUnlocked(AchievementType.FIRST_VOTER),
                "FIRST_VOTER should be awarded on first vote");
    }

    @Test
    void testDoubleVotingPrevented() {
        system.castVote(inventory, Candidate.HOLT, achievementCallback);
        int votesAfterFirst = system.getVotes(Candidate.HOLT);

        boolean secondResult = system.castVote(inventory, Candidate.BRANNIGAN, achievementCallback);

        assertFalse(secondResult, "Second vote should be rejected");
        assertEquals(votesAfterFirst, system.getVotes(Candidate.HOLT),
                "Holt's vote count should not change after blocked second vote");
        assertEquals(Candidate.HOLT, system.getVotedFor(),
                "votedFor should remain HOLT after second vote attempt");
    }

    @Test
    void testFirstVoterNotDuplicated() {
        system.castVote(inventory, Candidate.HOLT, achievementCallback);
        system.castVote(inventory, Candidate.BRANNIGAN, achievementCallback); // blocked
        assertTrue(system.isFirstVoterAwarded(), "Should only be awarded once");
    }

    // ── Ballot box theft tests ─────────────────────────────────────────────────

    @Test
    void testStealBallotBoxVoidsResult() {
        assertFalse(system.isResultVoided());
        system.stealBallotBox(inventory, criminalRecord, notorietySystem, wantedSystem, achievementCallback);
        assertTrue(system.isResultVoided(), "Stealing ballot box should void the result");
        assertTrue(inventory.hasItem(Material.BALLOT_BOX), "BALLOT_BOX should be in inventory");
        assertTrue(notorietySystem.getNotoriety() >= LocalElectionSystem.BALLOT_BOX_NOTORIETY,
                "Notoriety should increase by BALLOT_BOX_NOTORIETY");
        assertTrue(criminalRecord.getCount(CriminalRecord.CrimeType.BREACH_OF_POLLING_STATION_EXCLUSION) > 0,
                "BREACH_OF_POLLING_STATION_EXCLUSION should be recorded");
    }

    @Test
    void testGetWinnerReturnsNullWhenVoided() {
        system.stealBallotBox(inventory, criminalRecord, notorietySystem, wantedSystem, achievementCallback);
        assertNull(system.getWinner(), "getWinner() should return null when result is voided");
    }

    // ── Count Night tests ──────────────────────────────────────────────────────

    @Test
    void testGrassUpBoxStufferAwardsStreetSmart() {
        assertFalse(achievementSystem.isUnlocked(AchievementType.STREET_SMART));
        system.grassUpBoxStuffer(achievementCallback);
        assertTrue(achievementSystem.isUnlocked(AchievementType.STREET_SMART),
                "STREET_SMART should be awarded for grassing up the box-stuffer");
    }

    @Test
    void testStealCountSheet() {
        assertFalse(inventory.hasItem(Material.COUNT_SHEET));
        system.stealCountSheet(inventory, notorietySystem, achievementCallback);
        assertTrue(inventory.hasItem(Material.COUNT_SHEET), "COUNT_SHEET should be added to inventory");
        assertTrue(notorietySystem.getNotoriety() >= LocalElectionSystem.COUNT_SHEET_NOTORIETY,
                "Notoriety should increase after stealing count sheet");
    }

    // ── Winner resolution tests ────────────────────────────────────────────────

    @Test
    void testGetWinnerHighestVotes() {
        system.setVotes(Candidate.HOLT, 200);
        system.setVotes(Candidate.BRANNIGAN, 150);
        system.setVotes(Candidate.PATEL, 100);
        assertEquals(Candidate.HOLT, system.getWinner(),
                "Holt should win with the most votes");
    }

    @Test
    void testPostElectionEffectsBrannigan() {
        DWPSystem dwpSystem = new DWPSystem(new Random());
        assertEquals(1.0f, dwpSystem.getPaymentMultiplier(), 0.001f);

        system.setVotes(Candidate.BRANNIGAN, 999);
        system.setVotes(Candidate.HOLT, 50);
        system.setVotes(Candidate.PATEL, 50);
        system.applyPostElectionEffects(dwpSystem, null, null, null, LocalElectionSystem.POLLING_DAY, null);

        assertTrue(dwpSystem.getPaymentMultiplier() > 1.0f,
                "DWP payment multiplier should be above 1.0 after Brannigan win");
    }

    @Test
    void testPostElectionEffectsPatel() {
        NeighbourhoodSystem neighbourhoodSystem = new NeighbourhoodSystem();
        int vibesBefore = neighbourhoodSystem.getVibes();

        system.setVotes(Candidate.PATEL, 999);
        system.setVotes(Candidate.HOLT, 50);
        system.setVotes(Candidate.BRANNIGAN, 50);
        system.applyPostElectionEffects(null, neighbourhoodSystem, null, null,
                LocalElectionSystem.POLLING_DAY, null);

        assertEquals(vibesBefore + LocalElectionSystem.PATEL_VIBES_BONUS,
                neighbourhoodSystem.getVibes(),
                "NeighbourhoodSystem Vibes should increase by PATEL_VIBES_BONUS");
    }

    @Test
    void testKingmakerAchievement() {
        // Player voted for BRANNIGAN, and BRANNIGAN wins
        system.castVote(inventory, Candidate.BRANNIGAN, achievementCallback);
        system.setVotes(Candidate.BRANNIGAN, 999);
        system.setVotes(Candidate.HOLT, 50);
        system.setVotes(Candidate.PATEL, 50);
        system.applyPostElectionEffects(null, null, null, null, LocalElectionSystem.POLLING_DAY, achievementCallback);
        assertTrue(achievementSystem.isUnlocked(AchievementType.KINGMAKER),
                "KINGMAKER should be awarded when player's chosen candidate wins");
    }

    @Test
    void testKingmakerNotAwardedWhenLoser() {
        // Player voted for PATEL, but HOLT wins
        system.castVote(inventory, Candidate.PATEL, achievementCallback);
        system.setVotes(Candidate.HOLT, 999);
        system.setVotes(Candidate.BRANNIGAN, 50);
        system.setVotes(Candidate.PATEL, 50);
        system.applyPostElectionEffects(null, null, null, null, LocalElectionSystem.POLLING_DAY, achievementCallback);
        assertFalse(achievementSystem.isUnlocked(AchievementType.KINGMAKER),
                "KINGMAKER should NOT be awarded when player's candidate loses");
    }

    // ── Constants sanity checks ────────────────────────────────────────────────

    @Test
    void testConstants() {
        assertEquals(83, LocalElectionSystem.CANVASSING_START_DAY);
        assertEquals(90, LocalElectionSystem.POLLING_DAY);
        assertEquals(7.0f, LocalElectionSystem.POLLING_STATION_OPEN_HOUR, 0.001f);
        assertEquals(22.0f, LocalElectionSystem.POLLING_STATION_CLOSE_HOUR, 0.001f);
        assertEquals(22.5f, LocalElectionSystem.COUNT_START_HOUR, 0.001f);
        assertEquals(0.15f, LocalElectionSystem.POSTAL_VOTE_DETECTION_BASE, 0.001f);
        assertEquals(0.05f, LocalElectionSystem.POSTAL_VOTE_DETECTION_SLEIGHT, 0.001f);
        assertEquals(3.0f, LocalElectionSystem.BALLOT_BOX_STEAL_SECONDS, 0.001f);
        assertEquals(15, LocalElectionSystem.BALLOT_BOX_NOTORIETY);
        assertEquals(2.0f, LocalElectionSystem.LEAFLET_STEAL_SECONDS, 0.001f);
        assertEquals(7, LocalElectionSystem.POST_ELECTION_EFFECT_DAYS);
        assertEquals(2, LocalElectionSystem.BRIBE_CANVASSER_COST);
        assertEquals(3, LocalElectionSystem.DEFACED_POSTER_NOTORIETY);
        assertEquals(1, LocalElectionSystem.POSTAL_VOTE_BUNDLE_COST);
        assertEquals(5, LocalElectionSystem.POSTAL_VOTES_ADDED);
        assertEquals(10, LocalElectionSystem.POSTAL_FRAUD_NOTORIETY);
    }
}

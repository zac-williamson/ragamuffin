package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.*;
import ragamuffin.core.LocalElectionSystem.Candidate;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #1414: Integration tests for LocalElectionSystem.
 *
 * <p>Six scenarios as specified in SPEC.md:
 * <ol>
 *   <li>Canvassing spawns candidate NPCs days 83–89.</li>
 *   <li>TACTICAL_VOTER awarded after pledging to all three candidates.</li>
 *   <li>Polling station active day 90, closed before and after.</li>
 *   <li>FIRST_VOTER awarded on first vote; no duplicate award.</li>
 *   <li>BALLOT_STUFFER awarded on successful postal fraud (mocked 0% detection).</li>
 *   <li>Post-election Brannigan win raises DWP payment.</li>
 * </ol>
 */
class Issue1414LocalElectionIntegrationTest {

    private LocalElectionSystem electionSystem;
    private AchievementSystem achievementSystem;
    private NotorietySystem.AchievementCallback achievementCallback;
    private CriminalRecord criminalRecord;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private RumourNetwork rumourNetwork;
    private Inventory playerInventory;
    private List<NPC> npcs;
    private TimeSystem timeSystem;

    @BeforeEach
    void setUp() {
        electionSystem = new LocalElectionSystem(new Random(42));
        achievementSystem = new AchievementSystem();
        achievementCallback = type -> achievementSystem.unlock(type);
        criminalRecord = new CriminalRecord();
        notorietySystem = new NotorietySystem();
        wantedSystem = new WantedSystem();
        rumourNetwork = new RumourNetwork(new Random(77));
        playerInventory = new Inventory(36);
        npcs = new ArrayList<>();
        // TimeSystem: dayCount=1 → getDayOfYear() = (152 + 1 - 1) % 365 = 152
        timeSystem = new TimeSystem(8.0f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Canvassing spawns candidate NPCs on days 83–89
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: Advance time to day 83, hour 08:00. Call update(). Verify
     * three CANDIDATE_NPC instances exist. Advance past day 90; verify NPCs
     * are despawned.
     */
    @Test
    void testCanvassingSpawnsCandidateNpcsOnDay83() {
        // Day 83 of year — trigger canvassing
        electionSystem.update(83, npcs);

        long candidateCount = npcs.stream()
                .filter(n -> n.getType() == NPCType.CANDIDATE_NPC)
                .count();
        assertEquals(3, candidateCount,
                "Three CANDIDATE_NPC should spawn on day 83");

        long canvasserCount = npcs.stream()
                .filter(n -> n.getType() == NPCType.CANVASSER_NPC)
                .count();
        assertTrue(canvasserCount >= 2,
                "At least two CANVASSER_NPC should spawn during canvassing week");

        // Post-election: NPCs should despawn
        electionSystem.update(91, npcs);
        long remainingCandidates = npcs.stream()
                .filter(n -> n.getType() == NPCType.CANDIDATE_NPC)
                .count();
        assertEquals(0, remainingCandidates,
                "CANDIDATE_NPC should despawn after day 90");
    }

    @Test
    void testCanvassingNpcsNotSpawnedBeforeDay83() {
        electionSystem.update(82, npcs);
        long candidateCount = npcs.stream()
                .filter(n -> n.getType() == NPCType.CANDIDATE_NPC)
                .count();
        assertEquals(0, candidateCount,
                "No CANDIDATE_NPC should spawn before canvassing week");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: TACTICAL_VOTER awarded after pledging to all three candidates
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: Call pledgeSupport(HOLT), pledgeSupport(BRANNIGAN),
     * pledgeSupport(PATEL). Verify TACTICAL_VOTER achievement is awarded on
     * the third pledge.
     */
    @Test
    void testTacticalVoterAwardedAfterPledgingToAllThree() {
        assertFalse(achievementSystem.isUnlocked(AchievementType.TACTICAL_VOTER),
                "TACTICAL_VOTER should not be awarded before any pledges");

        electionSystem.pledgeSupport(Candidate.HOLT, achievementCallback);
        assertFalse(achievementSystem.isUnlocked(AchievementType.TACTICAL_VOTER),
                "TACTICAL_VOTER should not be awarded after 1 pledge");

        electionSystem.pledgeSupport(Candidate.BRANNIGAN, achievementCallback);
        assertFalse(achievementSystem.isUnlocked(AchievementType.TACTICAL_VOTER),
                "TACTICAL_VOTER should not be awarded after 2 pledges");

        electionSystem.pledgeSupport(Candidate.PATEL, achievementCallback);
        assertTrue(achievementSystem.isUnlocked(AchievementType.TACTICAL_VOTER),
                "TACTICAL_VOTER should be awarded on the third (final) pledge");
    }

    @Test
    void testPledgingOrderDoesNotMatter() {
        electionSystem.pledgeSupport(Candidate.PATEL, achievementCallback);
        electionSystem.pledgeSupport(Candidate.HOLT, achievementCallback);
        electionSystem.pledgeSupport(Candidate.BRANNIGAN, achievementCallback);
        assertTrue(achievementSystem.isUnlocked(AchievementType.TACTICAL_VOTER),
                "TACTICAL_VOTER should be awarded regardless of pledge order");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Polling station active on day 90, closed before and after
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: Advance to day 90 10:00; verify isPollingStationOpen() is true.
     * Advance to day 91; verify it returns false.
     */
    @Test
    void testPollingStationActiveOnDay90() {
        assertTrue(electionSystem.isPollingStationOpen(90, 10.0f),
                "Polling station should be open at 10:00 on day 90");
    }

    @Test
    void testPollingStationClosedBeforeDay90() {
        assertFalse(electionSystem.isPollingStationOpen(89, 10.0f),
                "Polling station should be closed on day 89");
    }

    @Test
    void testPollingStationClosedAfterDay90() {
        assertFalse(electionSystem.isPollingStationOpen(91, 10.0f),
                "Polling station should be closed on day 91");
    }

    @Test
    void testPollingStationClosedAfter2200() {
        assertFalse(electionSystem.isPollingStationOpen(90, 22.1f),
                "Polling station should be closed after 22:00 on day 90");
    }

    @Test
    void testPollingStationClosedBefore0700() {
        assertFalse(electionSystem.isPollingStationOpen(90, 6.5f),
                "Polling station should be closed before 07:00 on day 90");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: FIRST_VOTER awarded on first vote; no duplicate award
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Call castVote(player, HOLT); verify FIRST_VOTER is awarded.
     * Call again; verify no duplicate award and second call returns false.
     */
    @Test
    void testFirstVoterAwardedOnFirstVote() {
        assertFalse(achievementSystem.isUnlocked(AchievementType.FIRST_VOTER),
                "FIRST_VOTER should not be awarded before voting");

        boolean firstVote = electionSystem.castVote(playerInventory, Candidate.HOLT, achievementCallback);
        assertTrue(firstVote, "First castVote() should return true");
        assertTrue(achievementSystem.isUnlocked(AchievementType.FIRST_VOTER),
                "FIRST_VOTER should be awarded on first vote");
        assertTrue(electionSystem.hasVoted(), "hasVoted() should be true");
    }

    @Test
    void testNoDuplicateFirstVoterAward() {
        electionSystem.castVote(playerInventory, Candidate.HOLT, achievementCallback);

        // Attempt second vote — should be rejected
        boolean secondVote = electionSystem.castVote(playerInventory, Candidate.BRANNIGAN, achievementCallback);
        assertFalse(secondVote, "Second castVote() should return false (already voted)");

        // FIRST_VOTER should only be awarded once (not thrown into an error by double-unlock)
        assertTrue(achievementSystem.isUnlocked(AchievementType.FIRST_VOTER),
                "FIRST_VOTER should remain unlocked");
        assertEquals(Candidate.HOLT, electionSystem.getVotedFor(),
                "votedFor should remain HOLT after rejected second vote");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: BALLOT_STUFFER awarded on successful postal fraud (0% detection)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5: Set detection to 0 (mocked), call submitPostalVote(player, PATEL).
     * Verify BALLOT_STUFFER awarded and Patel vote tally incremented by POSTAL_VOTES_ADDED.
     */
    @Test
    void testBallotStufferAwardedOnSuccessfulPostalFraud() {
        // Give player a postal vote bundle
        playerInventory.addItem(Material.POSTAL_VOTE_BUNDLE, 1);
        int votesBefore = electionSystem.getVotes(Candidate.PATEL);

        // Override detection to 0% so fraud always succeeds
        electionSystem.setDetectionRiskForTesting(0.0f);

        boolean success = electionSystem.submitPostalVote(playerInventory, Candidate.PATEL,
                criminalRecord, notorietySystem, wantedSystem, rumourNetwork,
                achievementCallback, null);

        assertTrue(success, "Postal vote should succeed with 0% detection risk");
        assertTrue(achievementSystem.isUnlocked(AchievementType.BALLOT_STUFFER),
                "BALLOT_STUFFER should be awarded on successful postal fraud");
        assertEquals(votesBefore + LocalElectionSystem.POSTAL_VOTES_ADDED,
                electionSystem.getVotes(Candidate.PATEL),
                "Patel vote tally should increase by POSTAL_VOTES_ADDED");
    }

    @Test
    void testBallotStufferNotAwardedWhenCaught() {
        playerInventory.addItem(Material.POSTAL_VOTE_BUNDLE, 1);

        // Override detection to 100% so fraud always fails
        electionSystem.setDetectionRiskForTesting(1.0f);

        boolean success = electionSystem.submitPostalVote(playerInventory, Candidate.PATEL,
                criminalRecord, notorietySystem, wantedSystem, rumourNetwork,
                achievementCallback, null);

        assertFalse(success, "Postal vote should fail with 100% detection");
        assertFalse(achievementSystem.isUnlocked(AchievementType.BALLOT_STUFFER),
                "BALLOT_STUFFER should NOT be awarded when caught");
        assertTrue(criminalRecord.getCount(CriminalRecord.CrimeType.ELECTORAL_FRAUD) > 0,
                "ELECTORAL_FRAUD should be recorded in CriminalRecord");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 6: Post-election Brannigan win raises DWP payment
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 6: Simulate Brannigan winning; call applyPostElectionEffects();
     * verify DWPSystem.getPaymentMultiplier() > 1.0f.
     */
    @Test
    void testPostElectionBranniganWinRaisesDWPPayment() {
        DWPSystem dwpSystem = new DWPSystem(new Random());
        assertEquals(1.0f, dwpSystem.getPaymentMultiplier(), 0.001f,
                "DWP payment multiplier should start at 1.0");

        // Simulate Brannigan winning using the simplified overload
        electionSystem.applyPostElectionEffects(dwpSystem);

        assertTrue(dwpSystem.getPaymentMultiplier() > 1.0f,
                "DWP payment multiplier should be above 1.0 after Brannigan wins");
    }

    @Test
    void testPostElectionEffectsDecayAfter7Days() {
        DWPSystem dwpSystem = new DWPSystem(new Random());
        electionSystem.applyPostElectionEffects(dwpSystem);

        assertTrue(dwpSystem.getPaymentMultiplier() > 1.0f, "Multiplier should be raised");

        // Advance past the effect window
        int decayDay = LocalElectionSystem.POLLING_DAY + LocalElectionSystem.POST_ELECTION_EFFECT_DAYS;
        electionSystem.tickPostElectionDecay(decayDay, dwpSystem);

        assertEquals(1.0f, dwpSystem.getPaymentMultiplier(), 0.001f,
                "DWP payment multiplier should return to 1.0 after the effect window expires");
    }
}

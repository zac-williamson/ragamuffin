package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord;
import ragamuffin.core.Faction;
import ragamuffin.core.FactionSystem;
import ragamuffin.core.JobCentreRecord;
import ragamuffin.core.JobCentreRecord.JobSearchMissionType;
import ragamuffin.core.JobCentreSystem;
import ragamuffin.core.JobCentreSystem.SignOnResult;
import ragamuffin.core.NotorietySystem;
import ragamuffin.core.RumourNetwork;
import ragamuffin.core.StreetSkillSystem;
import ragamuffin.core.StreetSkillSystem.Skill;
import ragamuffin.core.TimeSystem;
import ragamuffin.core.TurfMap;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #795: Integration tests for The JobCentre Gauntlet system.
 *
 * <h3>Test coverage</h3>
 * <ol>
 *   <li>UC payment calculation: correct amount at each sanction level</li>
 *   <li>Sign-on cycle: 3-cycle arc — two normal sign-ons then a missed one</li>
 *   <li>Debt collector spawns at sanction level 3 and is removed on payment</li>
 *   <li>Criminal record complications: suspicious case worker at 3–5 offences</li>
 *   <li>Notoriety Tier 3 auto-pay, Tier 5 claim closure</li>
 *   <li>BUREAUCRACY skill perks: extended window, appeal, payment bonus</li>
 *   <li>Faction cross-pollination: Street Lads respect on 3 missed sign-ons</li>
 *   <li>Marchetti item confiscation</li>
 *   <li>Debt collector punch: notoriety gain</li>
 * </ol>
 */
class Issue795JobCentreGauntletTest {

    private TimeSystem timeSystem;
    private CriminalRecord criminalRecord;
    private NotorietySystem notorietySystem;
    private FactionSystem factionSystem;
    private RumourNetwork rumourNetwork;
    private StreetSkillSystem streetSkillSystem;
    private JobCentreSystem jobCentreSystem;
    private Player player;
    private Inventory inventory;
    private List<NPC> allNpcs;

    @BeforeEach
    void setUp() {
        timeSystem       = new TimeSystem(9.0f); // Start at 09:00
        criminalRecord   = new CriminalRecord();
        notorietySystem  = new NotorietySystem(new Random(42));
        factionSystem    = new FactionSystem(new TurfMap(), new RumourNetwork(new Random(42)), new Random(42));
        rumourNetwork    = new RumourNetwork(new Random(42));
        streetSkillSystem = new StreetSkillSystem(new Random(42));
        player           = new Player(100, 1, 100);
        inventory        = new Inventory(36);
        allNpcs          = new ArrayList<>();

        jobCentreSystem = new JobCentreSystem(
                timeSystem,
                criminalRecord,
                notorietySystem,
                factionSystem,
                rumourNetwork,
                null, // NewspaperSystem not needed for most tests
                streetSkillSystem,
                null, // WantedSystem not needed for most tests
                null, // NPCManager not needed for most tests
                new Random(42));

        // Set day to the first sign-on day (day 3) so the window is open
        advanceToSignOnDay(3);
    }

    // ── Helper: advance time to a sign-on window ──────────────────────────────

    /**
     * Advance the time system to day {@code day} at 09:30 (inside the sign-on window).
     */
    private void advanceToSignOnDay(int day) {
        // TimeSystem doesn't have setDay; simulate by using setNextSignOnDayForTesting
        // and setting the time to 09:30 (sign-on window is 09:00–10:00).
        jobCentreSystem.setNextSignOnDayForTesting(timeSystem.getDayCount());
        timeSystem.setTime(9.5f); // 09:30
    }

    /**
     * Advance the time system past the sign-on window (to 11:00) to simulate a miss.
     */
    private void advancePastSignOnWindow() {
        timeSystem.setTime(11.0f); // past 10:00
    }

    // ── Test 1: UC payment at each sanction level ─────────────────────────────

    /**
     * UC payment is 8 COIN at sanction 0, reduced by 3 per level.
     * At level 3 (max) it should be 8 − 9 = −1, floored to 0.
     */
    @Test
    void ucPayment_correctAtEachSanctionLevel() {
        JobCentreRecord record = jobCentreSystem.getRecord();

        // Sanction 0 → 8
        record.setSanctionLevelForTesting(0);
        assertEquals(8, jobCentreSystem.computeUCPayment(),
                "At sanction 0 payment should be 8 COIN");

        // Sanction 1 → 5
        record.setSanctionLevelForTesting(1);
        assertEquals(5, jobCentreSystem.computeUCPayment(),
                "At sanction 1 payment should be 5 COIN");

        // Sanction 2 → 2
        record.setSanctionLevelForTesting(2);
        assertEquals(2, jobCentreSystem.computeUCPayment(),
                "At sanction 2 payment should be 2 COIN");

        // Sanction 3 → 0 (floored)
        record.setSanctionLevelForTesting(3);
        assertEquals(0, jobCentreSystem.computeUCPayment(),
                "At sanction 3 payment should be 0 COIN (fully sanctioned)");
    }

    // ── Test 2: Full 3-cycle benefit arc ─────────────────────────────────────

    /**
     * Complete 3-cycle arc:
     * Cycle 1: sign on → receive 8 COIN, mission assigned.
     * Cycle 2: sign on → receive 8 COIN, mission assigned.
     * Cycle 3: miss sign-on → sanction level increments to 1.
     * Next sign-on: receive 5 COIN (8 − 3).
     */
    @Test
    void threeCycleBenefitArc_sanctionIncreasesOnMiss() {
        // ── Cycle 1: sign on ──────────────────────────────────────────────────
        SignOnResult result1 = jobCentreSystem.trySignOn(player, inventory);
        assertEquals(SignOnResult.SUCCESS, result1, "Cycle 1 sign-on should succeed");
        assertTrue(inventory.getItemCount(Material.COIN) >= 8,
                "Should receive at least 8 COIN on first sign-on");
        assertNotNull(jobCentreSystem.getRecord().getCurrentMission(),
                "A mission should be assigned after sign-on");
        assertEquals(1, jobCentreSystem.getRecord().getTotalSignOns(),
                "Total sign-ons should be 1");

        // ── Cycle 2: advance 3 days, sign on ──────────────────────────────────
        inventory.removeItem(Material.COIN, 8); // clear coins
        jobCentreSystem.setNextSignOnDayForTesting(timeSystem.getDayCount());
        timeSystem.setTime(9.5f);

        SignOnResult result2 = jobCentreSystem.trySignOn(player, inventory);
        assertEquals(SignOnResult.SUCCESS, result2, "Cycle 2 sign-on should succeed");
        assertEquals(2, jobCentreSystem.getRecord().getTotalSignOns(),
                "Total sign-ons should be 2");

        // ── Cycle 3: advance 3 days, miss sign-on ─────────────────────────────
        inventory.removeItem(Material.COIN, 8);
        // Advance to a new day so lastSignOnDay (from cycle 2) < nextSignOnDay
        timeSystem.advanceTime(24.0f);
        jobCentreSystem.setNextSignOnDayForTesting(timeSystem.getDayCount());
        timeSystem.setTime(9.5f);
        // Simulate the window opening (windowWasOpen = true), then closing
        jobCentreSystem.update(0.1f, player, allNpcs); // window open
        advancePastSignOnWindow();
        jobCentreSystem.update(0.1f, player, allNpcs); // window closed → missed

        assertEquals(1, jobCentreSystem.getRecord().getMissedSignOns(),
                "Should have 1 missed sign-on after skipping cycle 3");
        assertEquals(1, jobCentreSystem.getRecord().getSanctionLevel(),
                "Sanction level should be 1 after first miss");

        // ── Cycle 4: sign on with sanction 1 → 5 COIN ────────────────────────
        inventory.removeItem(Material.COIN, inventory.getItemCount(Material.COIN));
        jobCentreSystem.setNextSignOnDayForTesting(timeSystem.getDayCount());
        timeSystem.setTime(9.5f);

        SignOnResult result4 = jobCentreSystem.trySignOn(player, inventory);
        assertEquals(SignOnResult.SUCCESS, result4, "Cycle 4 sign-on should succeed");
        assertEquals(5, inventory.getItemCount(Material.COIN),
                "With sanction 1, should receive 5 COIN (8 − 3)");
    }

    // ── Test 3: Debt collector at sanction level 3 ───────────────────────────

    /**
     * When sanction level reaches 3 (after 3 missed sign-ons), the debt
     * collector flag is set. Paying 10 COIN clears it.
     */
    @Test
    void debtCollector_activatesAtMaxSanction_clearedOnPayment() {
        JobCentreRecord record = jobCentreSystem.getRecord();

        // Simulate 3 missed sign-ons
        for (int i = 0; i < 3; i++) {
            jobCentreSystem.setNextSignOnDayForTesting(timeSystem.getDayCount());
            timeSystem.setTime(9.5f);
            jobCentreSystem.update(0.1f, player, allNpcs); // window open
            advancePastSignOnWindow();
            jobCentreSystem.update(0.1f, player, allNpcs); // missed
        }

        assertEquals(3, record.getSanctionLevel(),
                "Sanction level should be 3 after 3 misses");
        assertTrue(record.isDebtCollectorActive(),
                "Debt collector should be active at sanction level 3");

        // Pay 10 COIN
        inventory.addItem(Material.COIN, 10);
        boolean paid = jobCentreSystem.payDebt(inventory);
        assertTrue(paid, "payDebt should return true when player has 10 COIN");
        assertFalse(record.isDebtCollectorActive(),
                "Debt collector should be inactive after payment");
        assertEquals(0, inventory.getItemCount(Material.COIN),
                "10 COIN should be deducted after payment");
    }

    // ── Test 4: Criminal record complications ────────────────────────────────

    /**
     * 3–5 total offences → case worker is suspicious (SUSPICIOUS result).
     * 6+ total offences → POLICE_ESCORT result.
     */
    @Test
    void criminalRecord_triggersComplicationsAtThresholds() {
        // ── 3 offences → SUSPICIOUS ───────────────────────────────────────────
        for (int i = 0; i < 3; i++) {
            criminalRecord.record(CriminalRecord.CrimeType.BLOCKS_DESTROYED);
        }

        jobCentreSystem.setNextSignOnDayForTesting(timeSystem.getDayCount());
        timeSystem.setTime(9.5f);
        SignOnResult result = jobCentreSystem.trySignOn(player, inventory);
        assertEquals(SignOnResult.SUSPICIOUS, result,
                "3 offences should trigger SUSPICIOUS result");

        // ── 6+ offences → POLICE_ESCORT ───────────────────────────────────────
        for (int i = 0; i < 4; i++) {
            criminalRecord.record(CriminalRecord.CrimeType.BLOCKS_DESTROYED);
        }
        // total = 7 offences

        inventory.removeItem(Material.COIN, inventory.getItemCount(Material.COIN));
        jobCentreSystem.setNextSignOnDayForTesting(timeSystem.getDayCount());
        timeSystem.setTime(9.5f);
        SignOnResult result2 = jobCentreSystem.trySignOn(player, inventory);
        assertEquals(SignOnResult.POLICE_ESCORT, result2,
                "7 offences should trigger POLICE_ESCORT result");
    }

    // ── Test 5: Notoriety tier complications ─────────────────────────────────

    /**
     * Notoriety Tier 3 → case worker pays but auto-fails mission (NOTORIETY_SCARED).
     * Notoriety Tier 5 → case worker flees; claim closed permanently.
     */
    @Test
    void notorietyTier_triggersComplicationsAtTierThresholds() {
        // ── Tier 3: scared but pays ────────────────────────────────────────────
        notorietySystem.setNotorietyForTesting(500); // Tier 3

        jobCentreSystem.setNextSignOnDayForTesting(timeSystem.getDayCount());
        timeSystem.setTime(9.5f);
        SignOnResult result = jobCentreSystem.trySignOn(player, inventory);
        assertEquals(SignOnResult.NOTORIETY_SCARED, result,
                "Tier 3 notoriety should trigger NOTORIETY_SCARED result");
        // Still gets paid
        assertTrue(inventory.getItemCount(Material.COIN) > 0,
                "Player should still receive UC payment even when case worker is scared");

        // ── Tier 5: claim closed ───────────────────────────────────────────────
        notorietySystem.setNotorietyForTesting(1000); // Tier 5
        inventory.removeItem(Material.COIN, inventory.getItemCount(Material.COIN));

        jobCentreSystem.setNextSignOnDayForTesting(timeSystem.getDayCount());
        timeSystem.setTime(9.5f);
        SignOnResult result2 = jobCentreSystem.trySignOn(player, inventory);
        assertEquals(SignOnResult.NOTORIETY_FLEE, result2,
                "Tier 5 notoriety should trigger NOTORIETY_FLEE result");
        assertTrue(jobCentreSystem.getRecord().isClaimClosed(),
                "Claim should be permanently closed after NOTORIETY_FLEE");

        // Subsequent sign-on attempt should return CLAIM_CLOSED
        SignOnResult result3 = jobCentreSystem.trySignOn(player, inventory);
        assertEquals(SignOnResult.CLAIM_CLOSED, result3,
                "Any further sign-on should return CLAIM_CLOSED");
    }

    // ── Test 6: BUREAUCRACY skill perks ──────────────────────────────────────

    /**
     * BUREAUCRACY Apprentice (tier 1): extended sign-on window.
     * BUREAUCRACY Journeyman (tier 2): +2 COIN UC payment bonus.
     */
    @Test
    void bureaucracySkill_grantsPerks() {
        // ── Apprentice: extended sign-on window ───────────────────────────────
        streetSkillSystem.setXP(Skill.BUREAUCRACY, 100); // Apprentice threshold

        jobCentreSystem.setNextSignOnDayForTesting(timeSystem.getDayCount());
        // Sign-on window is 09:00–11:00 with Apprentice (2 hours)
        timeSystem.setTime(10.5f); // 10:30 — within extended window
        assertTrue(jobCentreSystem.isSignOnWindowOpen(),
                "With BUREAUCRACY Apprentice, sign-on window should be extended to 2 hours");

        // Without Apprentice, 10:30 is outside the normal 1-hour window
        StreetSkillSystem baseSkill = new StreetSkillSystem(new Random(42));
        JobCentreSystem baseSystem = new JobCentreSystem(
                timeSystem, criminalRecord, notorietySystem, factionSystem,
                rumourNetwork, null, baseSkill, null, null, new Random(42));
        baseSystem.setNextSignOnDayForTesting(timeSystem.getDayCount());
        assertFalse(baseSystem.isSignOnWindowOpen(),
                "Without BUREAUCRACY skill, 10:30 should be outside the normal 1-hour window");

        // ── Journeyman: higher UC payment ────────────────────────────────────
        streetSkillSystem.setXP(Skill.BUREAUCRACY, 300); // Journeyman threshold

        timeSystem.setTime(9.5f);
        inventory.removeItem(Material.COIN, inventory.getItemCount(Material.COIN));
        jobCentreSystem.setNextSignOnDayForTesting(timeSystem.getDayCount());
        SignOnResult result = jobCentreSystem.trySignOn(player, inventory);
        assertEquals(SignOnResult.SUCCESS, result, "Sign-on should succeed");
        int coinsReceived = inventory.getItemCount(Material.COIN);
        assertEquals(10, coinsReceived,
                "With BUREAUCRACY Journeyman, should receive 8 + 2 = 10 COIN");
    }

    // ── Test 7: Street Lads faction cross-pollination ───────────────────────

    /**
     * After 3 missed sign-ons, Street Lads respect increases by 8.
     */
    @Test
    void missedSignOns_grantsStreetLadsRespect() {
        int initialRespect = factionSystem.getRespect(Faction.STREET_LADS);

        // Miss 3 sign-ons
        for (int i = 0; i < 3; i++) {
            jobCentreSystem.setNextSignOnDayForTesting(timeSystem.getDayCount());
            timeSystem.setTime(9.5f);
            jobCentreSystem.update(0.1f, player, allNpcs); // window open
            advancePastSignOnWindow();
            jobCentreSystem.update(0.1f, player, allNpcs); // missed
        }

        int newRespect = factionSystem.getRespect(Faction.STREET_LADS);
        assertEquals(initialRespect + JobCentreSystem.STREET_LADS_MISSED_SIGN_ON_RESPECT,
                newRespect,
                "Street Lads respect should increase by "
                        + JobCentreSystem.STREET_LADS_MISSED_SIGN_ON_RESPECT
                        + " after 3 missed sign-ons");
        assertTrue(jobCentreSystem.getRecord().isStreetLadsNoticed(),
                "streetLadsNoticed flag should be set");
    }

    // ── Test 8: Marchetti item confiscation ──────────────────────────────────

    /**
     * If the player carries a Marchetti mission item (CROWBAR, BOLT_CUTTERS,
     * BALACLAVA) during sign-on, it is confiscated.
     */
    @Test
    void marchettItemConfiscation_removesItemFromInventory() {
        inventory.addItem(Material.CROWBAR, 1);

        jobCentreSystem.setNextSignOnDayForTesting(timeSystem.getDayCount());
        timeSystem.setTime(9.5f);
        SignOnResult result = jobCentreSystem.trySignOn(player, inventory);

        assertEquals(SignOnResult.MARCHETTI_CONFISCATION, result,
                "Carrying a CROWBAR should trigger confiscation");
        assertEquals(0, inventory.getItemCount(Material.CROWBAR),
                "CROWBAR should be removed from inventory after confiscation");
    }

    // ── Test 9: Debt collector punch notoriety ───────────────────────────────

    /**
     * Punching the debt collector adds 15 notoriety.
     */
    @Test
    void punchingDebtCollector_addsNotoriety() {
        int initialNotoriety = notorietySystem.getNotoriety();

        jobCentreSystem.onDebtCollectorPunched(null);

        assertEquals(initialNotoriety + JobCentreSystem.DEBT_COLLECTOR_PUNCH_NOTORIETY,
                notorietySystem.getNotoriety(),
                "Punching debt collector should add "
                        + JobCentreSystem.DEBT_COLLECTOR_PUNCH_NOTORIETY + " notoriety");
    }

    // ── Test 10: Mission assignment and completion ────────────────────────────

    /**
     * After signing on, a mission is assigned. Completing it awards BUREAUCRACY XP.
     */
    @Test
    void missionAssignment_andCompletion_awardsBureaucracyXP() {
        jobCentreSystem.setNextSignOnDayForTesting(timeSystem.getDayCount());
        timeSystem.setTime(9.5f);

        SignOnResult result = jobCentreSystem.trySignOn(player, inventory);
        assertEquals(SignOnResult.SUCCESS, result);

        // Mission should be assigned
        assertNotNull(jobCentreSystem.getRecord().getCurrentMission(),
                "A mission should be assigned after sign-on");
        assertFalse(jobCentreSystem.getRecord().isMissionCompletedThisCycle(),
                "Mission should not be completed yet");

        int xpBefore = streetSkillSystem.getXP(Skill.BUREAUCRACY);
        boolean completed = jobCentreSystem.completeMission();
        assertTrue(completed, "completeMission should return true when a mission is active");
        assertTrue(jobCentreSystem.getRecord().isMissionCompletedThisCycle(),
                "Mission should be marked completed");
        assertTrue(streetSkillSystem.getXP(Skill.BUREAUCRACY) > xpBefore,
                "BUREAUCRACY XP should increase after completing a mission");
    }

    // ── Test 11: Sign-on window timing ───────────────────────────────────────

    /**
     * The sign-on window is open at 09:30 but closed at 08:59 and 10:01 (base window).
     */
    @Test
    void signOnWindow_openDuringCorrectHours() {
        jobCentreSystem.setNextSignOnDayForTesting(timeSystem.getDayCount());

        // Before window
        timeSystem.setTime(8.5f);
        assertFalse(jobCentreSystem.isSignOnWindowOpen(),
                "Sign-on window should not be open at 08:30");

        // During window
        timeSystem.setTime(9.5f);
        assertTrue(jobCentreSystem.isSignOnWindowOpen(),
                "Sign-on window should be open at 09:30");

        // After window (base: 1 hour, closes at 10:00)
        timeSystem.setTime(10.5f);
        assertFalse(jobCentreSystem.isSignOnWindowOpen(),
                "Sign-on window should be closed at 10:30 (base window is 1 hour)");
    }

    // ── Test 12: Case worker dialogue ────────────────────────────────────────

    /**
     * Case worker dialogue varies based on sign-on result.
     */
    @Test
    void caseWorkerDialogue_reflectsSignOnResult() {
        String successDialogue = jobCentreSystem.getCaseWorkerDialogue(SignOnResult.SUCCESS, 0);
        assertNotNull(successDialogue);
        assertFalse(successDialogue.isEmpty(), "Success dialogue should not be empty");

        String suspiciousDialogue = jobCentreSystem.getCaseWorkerDialogue(SignOnResult.SUSPICIOUS, 0);
        assertTrue(suspiciousDialogue.contains("LIE") || suspiciousDialogue.contains("ADMIT"),
                "Suspicious dialogue should mention LIE / ADMIT IT");

        String fleeDialogue = jobCentreSystem.getCaseWorkerDialogue(SignOnResult.NOTORIETY_FLEE, 5);
        assertTrue(fleeDialogue.contains("HELP") || fleeDialogue.toLowerCase().contains("run"),
                "Flee dialogue should express panic");

        String closedDialogue = jobCentreSystem.getCaseWorkerDialogue(SignOnResult.CLAIM_CLOSED, 0);
        assertFalse(closedDialogue.isEmpty(), "Closed-claim dialogue should not be empty");
    }
}

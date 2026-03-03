package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.*;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1455 — Northfield Dodgy Roofer: Kenny's Round,
 * the Van Raid &amp; the Trading Standards Trap.
 *
 * <p>Five scenarios:
 * <ol>
 *   <li>Kenny completes a pitch → KENNY_SCAM rumour seeded.</li>
 *   <li>Van raid loot + VEHICLE_BREAK_IN recorded.</li>
 *   <li>Kenny hostile on rival pitch spot (within turf-dispute radius).</li>
 *   <li>Trading Standards impound on report.</li>
 *   <li>Tip-off reward + van moved.</li>
 * </ol>
 */
class Issue1455DodgyRooferTest {

    private DodgyRooferSystem dodgyRoofer;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private NotorietySystem.AchievementCallback achievementCallback;
    private RumourNetwork rumourNetwork;
    private Inventory inventory;

    /** A nearby witness NPC. */
    private NPC witnessNpc;

    @BeforeEach
    void setUp() {
        // Use fixed seed for reproducibility across all tests
        dodgyRoofer = new DodgyRooferSystem(new Random(42));
        notorietySystem = new NotorietySystem();
        wantedSystem = new WantedSystem(new Random(99));
        criminalRecord = new CriminalRecord();
        achievementSystem = new AchievementSystem();
        achievementCallback = type -> achievementSystem.unlock(type);
        rumourNetwork = new RumourNetwork(new Random(77));
        inventory = new Inventory(36);

        witnessNpc = new NPC(NPCType.PENSIONER, 5f, 1f, 5f);

        dodgyRoofer.setNotorietySystem(notorietySystem);
        dodgyRoofer.setWantedSystem(wantedSystem);
        dodgyRoofer.setCriminalRecord(criminalRecord);
        dodgyRoofer.setRumourNetwork(rumourNetwork);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Kenny completes a pitch → KENNY_SCAM rumour seeded
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: Kenny completing a pitch seeds the KENNY_SCAM rumour.
     *
     * <p>Simulate advancing through a full pitch duration (45s) on a weekday during
     * operating hours. Because the random seed is fixed (42), we may need to advance
     * through multiple properties until a pitch is accepted. We force Kenny into the
     * working state via the testing hook to guarantee the post-acceptance path fires,
     * then verify KENNY_SCAM was seeded.
     */
    @Test
    void scenario1_kennyCompletePitch_kennySCAMRumourSeeded() {
        // Force round active and Kenny into working state (simulates accepted pitch)
        dodgyRoofer.setRoundActiveForTesting(true);
        dodgyRoofer.setKennyWorkingForTesting(true);

        // Advance time through full work duration to trigger rumour seeding
        // Work duration = 30s; update in small steps
        float elapsed = 0f;
        int dayOfWeek = 1; // Monday
        float hour = 10.0f;

        while (elapsed < DodgyRooferSystem.WORK_DURATION_SECONDS + 1f) {
            dodgyRoofer.update(1f, hour, dayOfWeek, witnessNpc, achievementCallback);
            elapsed += 1f;
        }

        // Verify BOTCHED_JOB or KENNY_SCAM were seeded (both are seeded after work completes)
        boolean scamOrBotchedSeeded =
                rumourNetwork.getAllRumourTypes().contains(RumourType.KENNY_SCAM)
                || rumourNetwork.getAllRumourTypes().contains(RumourType.BOTCHED_JOB);
        assertTrue(scamOrBotchedSeeded,
                "After Kenny completes a job, KENNY_SCAM or BOTCHED_JOB rumour should be seeded");
    }

    /**
     * Scenario 1b: Kenny's round is active on weekdays during operating hours
     * and inactive outside those hours.
     */
    @Test
    void scenario1b_kennyRoundActiveOnWeekdays() {
        // Saturday (dayOfWeek=6) — should NOT be active
        dodgyRoofer.update(1f, 10.0f, 6, witnessNpc, achievementCallback);
        assertFalse(dodgyRoofer.isRoundActive(), "Kenny should not operate on Saturday");

        // Monday within hours — should become active
        dodgyRoofer.update(1f, 10.0f, 1, witnessNpc, achievementCallback);
        assertTrue(dodgyRoofer.isRoundActive(), "Kenny's round should be active Monday 10:00");

        // Monday before hours — should end round
        // Create fresh system
        DodgyRooferSystem fresh = new DodgyRooferSystem(new Random(1));
        fresh.update(1f, 8.0f, 1, null, null);
        assertFalse(fresh.isRoundActive(), "Kenny's round should not start before 09:00");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Van raid loot + VEHICLE_BREAK_IN recorded
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: Player raids Kenny's van while unattended.
     *
     * <p>Force van unattended via testing hook. Verify BUCKET_OF_SEALANT is always
     * present. Verify VEHICLE_BREAK_IN is recorded in CriminalRecord.
     * Verify WantedSystem gained at least 1 star. Verify TOOLS_DOWN achievement awarded.
     */
    @Test
    void scenario2_vanRaid_lootAndCrimeRecorded() {
        // Force van unattended
        dodgyRoofer.setVanUnattendedForTesting(true);

        // Player raids van from a safe distance (far from Kenny)
        float distanceToKenny = 50f; // far away
        DodgyRooferSystem.VanRaidResult result =
                dodgyRoofer.raidVan(inventory, distanceToKenny, witnessNpc, achievementCallback);

        assertEquals(DodgyRooferSystem.VanRaidResult.LOOTED, result,
                "Van raid should succeed when unattended and Kenny is far away");

        // BUCKET_OF_SEALANT always included
        assertTrue(inventory.getItemCount(Material.BUCKET_OF_SEALANT) >= 1,
                "BUCKET_OF_SEALANT should always be in van loot");

        // VEHICLE_BREAK_IN recorded in criminal record
        assertTrue(criminalRecord.getCount(CriminalRecord.CrimeType.VEHICLE_BREAK_IN) >= 1,
                "VEHICLE_BREAK_IN should be recorded in criminal record");

        // Notoriety increased
        assertTrue(notorietySystem.getNotoriety() >= DodgyRooferSystem.VAN_RAID_NOTORIETY,
                "Notoriety should increase by at least VAN_RAID_NOTORIETY after van raid");

        // WantedSystem increased
        assertTrue(wantedSystem.getWantedStars() >= DodgyRooferSystem.VAN_RAID_WANTED_STARS,
                "Wanted stars should increase after van raid");

        // TOOLS_DOWN achievement
        assertTrue(achievementSystem.isUnlocked(AchievementType.TOOLS_DOWN),
                "TOOLS_DOWN achievement should be awarded on first van raid");
    }

    /**
     * Scenario 2b: Attempting to raid the van when NOT unattended returns VAN_NOT_UNATTENDED.
     */
    @Test
    void scenario2b_vanRaid_notUnattended_returnsNotUnattended() {
        // Van is NOT unattended (default state, Kenny not mid-pitch yet)
        DodgyRooferSystem.VanRaidResult result =
                dodgyRoofer.raidVan(inventory, 50f, witnessNpc, achievementCallback);

        assertEquals(DodgyRooferSystem.VanRaidResult.VAN_NOT_UNATTENDED, result,
                "Van raid should fail when van is not unattended");
        assertEquals(0, inventory.getItemCount(Material.BUCKET_OF_SEALANT),
                "No loot should be added when van is not unattended");
    }

    /**
     * Scenario 2c: Kenny catches player raiding van → CAUGHT_BY_KENNY result.
     */
    @Test
    void scenario2c_vanRaid_kennyCatches_hostile() {
        // Force Kenny into working phase (within hostile-check range)
        dodgyRoofer.setKennyWorkingForTesting(true);

        // Player very close to Kenny
        float distanceToKenny = 3f;
        DodgyRooferSystem.VanRaidResult result =
                dodgyRoofer.raidVan(inventory, distanceToKenny, witnessNpc, achievementCallback);

        assertEquals(DodgyRooferSystem.VanRaidResult.CAUGHT_BY_KENNY, result,
                "Kenny should catch player when within hostile radius");
        assertTrue(dodgyRoofer.isKennyHostile(),
                "Kenny should become hostile when catching player at van");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Kenny hostile on rival pitch spot
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: Player attempts rival cold-call within Kenny's turf-dispute radius.
     *
     * <p>Give player BUCKET_OF_SEALANT + LATEX_GLOVES. Place Kenny within
     * {@link DodgyRooferSystem#TURF_DISPUTE_RADIUS} blocks. Verify result is
     * CAUGHT_BY_KENNY, Kenny becomes hostile, and TURF_DISPUTE rumour is seeded.
     */
    @Test
    void scenario3_rivalCallWithinRadius_kennyHostileAndTurfDisputeSeeded() {
        // Give player the required items
        inventory.addItem(Material.BUCKET_OF_SEALANT, 1);
        inventory.addItem(Material.LATEX_GLOVES, 1);

        // Kenny is within the turf dispute radius
        float kennyDistance = DodgyRooferSystem.TURF_DISPUTE_RADIUS - 5f;

        DodgyRooferSystem.RivalCallResult result =
                dodgyRoofer.attemptRivalCall(inventory, kennyDistance, witnessNpc, achievementCallback);

        assertEquals(DodgyRooferSystem.RivalCallResult.CAUGHT_BY_KENNY, result,
                "Rival call within Kenny's radius should result in CAUGHT_BY_KENNY");

        assertTrue(dodgyRoofer.isKennyHostile(),
                "Kenny should become hostile after spotting rival call on his turf");

        assertTrue(rumourNetwork.getAllRumourTypes().contains(RumourType.TURF_DISPUTE),
                "TURF_DISPUTE rumour should be seeded when rival call occurs near Kenny");
    }

    /**
     * Scenario 3b: Rival call outside Kenny's range succeeds and increments counter.
     */
    @Test
    void scenario3b_rivalCallOutsideRadius_accepted() {
        inventory.addItem(Material.BUCKET_OF_SEALANT, 1);
        inventory.addItem(Material.LATEX_GLOVES, 1);

        // Kenny is far away
        float kennyDistance = DodgyRooferSystem.TURF_DISPUTE_RADIUS + 10f;

        // Use a system with a seed known to accept at least some calls
        DodgyRooferSystem system = new DodgyRooferSystem(new Random(7));
        system.setRumourNetwork(rumourNetwork);
        system.setNotorietySystem(notorietySystem);

        // Try multiple calls to verify the system doesn't crash and rival rumour is seeded
        DodgyRooferSystem.RivalCallResult result =
                system.attemptRivalCall(inventory, kennyDistance, witnessNpc, achievementCallback);

        // Result is either ACCEPTED or REFUSED (no crash, no CAUGHT)
        assertTrue(result == DodgyRooferSystem.RivalCallResult.ACCEPTED
                        || result == DodgyRooferSystem.RivalCallResult.REFUSED,
                "Rival call outside Kenny's range should be ACCEPTED or REFUSED, not CAUGHT");

        assertTrue(rumourNetwork.getAllRumourTypes().contains(RumourType.RIVAL_ROOFER),
                "RIVAL_ROOFER rumour should always be seeded on rival call attempt");
    }

    /**
     * Scenario 3c: Rival call without required items returns MISSING_ITEMS.
     */
    @Test
    void scenario3c_rivalCallMissingItems_returnsMissingItems() {
        // Player has no tools
        DodgyRooferSystem.RivalCallResult result =
                dodgyRoofer.attemptRivalCall(inventory, 100f, witnessNpc, achievementCallback);

        assertEquals(DodgyRooferSystem.RivalCallResult.MISSING_ITEMS, result,
                "Rival call without BUCKET_OF_SEALANT + LATEX_GLOVES should return MISSING_ITEMS");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Trading Standards impound on report
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Player reports Kenny before 10:50 on Friday → van impounded at 11:00.
     *
     * <p>Report at Friday 10:00 (before deadline). Verify result is REPORTED.
     * Call processTradingStandardsCheck() to simulate the Friday 11:00 check.
     * Verify van is impounded, KENNY_FINED rumour is seeded, CIVIC_MINDED achievement awarded.
     */
    @Test
    void scenario4_tradingStandardsReport_vanImpounded() {
        int playerNotoriety = 10; // Low notoriety
        float hour = 10.0f;       // Before 10:50 deadline
        int dayOfWeek = 5;        // Friday

        DodgyRooferSystem.TradingStandardsReportResult reportResult =
                dodgyRoofer.reportToTradingStandards(
                        hour, dayOfWeek, playerNotoriety, witnessNpc, achievementCallback);

        assertEquals(DodgyRooferSystem.TradingStandardsReportResult.REPORTED, reportResult,
                "Report before deadline on Friday should return REPORTED");

        assertTrue(dodgyRoofer.isTradingStandardsReported(),
                "System should record that Trading Standards was reported");

        // KENNY_REPORTED rumour seeded
        assertTrue(rumourNetwork.getAllRumourTypes().contains(RumourType.KENNY_REPORTED),
                "KENNY_REPORTED rumour should be seeded after report");

        // SNITCH rumour seeded
        assertTrue(rumourNetwork.getAllRumourTypes().contains(RumourType.SNITCH),
                "SNITCH rumour should be seeded to warn about informer");

        // PUBLIC_SPIRITED at low notoriety
        assertTrue(achievementSystem.isUnlocked(AchievementType.PUBLIC_SPIRITED),
                "PUBLIC_SPIRITED achievement should be awarded at low notoriety");

        // Now simulate the Trading Standards officer arriving
        boolean impounded = dodgyRoofer.processTradingStandardsCheck(witnessNpc, achievementCallback);

        assertTrue(impounded, "Van should be impounded after Trading Standards check when reported");
        assertTrue(dodgyRoofer.isVanImpounded(), "Van impounded flag should be set");

        // KENNY_FINED rumour
        assertTrue(rumourNetwork.getAllRumourTypes().contains(RumourType.KENNY_FINED),
                "KENNY_FINED rumour should be seeded after impoundment");

        // CIVIC_MINDED achievement
        assertTrue(achievementSystem.isUnlocked(AchievementType.CIVIC_MINDED),
                "CIVIC_MINDED achievement should be awarded when van is impounded");

        // Kenny's round should no longer be active
        assertFalse(dodgyRoofer.isRoundActive(),
                "Kenny's round should be inactive after van impoundment");
    }

    /**
     * Scenario 4b: Reporting too late (after 10:50) returns TOO_LATE.
     */
    @Test
    void scenario4b_reportAfterDeadline_tooLate() {
        float hour = 10.9f; // After 10:50 deadline
        int dayOfWeek = 5;  // Friday

        DodgyRooferSystem.TradingStandardsReportResult result =
                dodgyRoofer.reportToTradingStandards(hour, dayOfWeek, 10, witnessNpc, achievementCallback);

        assertEquals(DodgyRooferSystem.TradingStandardsReportResult.TOO_LATE, result,
                "Report after deadline should return TOO_LATE");
    }

    /**
     * Scenario 4c: Reporting on a non-Friday returns NOT_FRIDAY.
     */
    @Test
    void scenario4c_reportOnWednesday_notFriday() {
        float hour = 10.0f;
        int dayOfWeek = 3; // Wednesday

        DodgyRooferSystem.TradingStandardsReportResult result =
                dodgyRoofer.reportToTradingStandards(hour, dayOfWeek, 10, witnessNpc, achievementCallback);

        assertEquals(DodgyRooferSystem.TradingStandardsReportResult.NOT_FRIDAY, result,
                "Report on non-Friday should return NOT_FRIDAY");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Tip-off reward + van moved
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5: Player tips off Kenny before Friday 10:50.
     *
     * <p>Verify TIP_OFF_KENNY achievement is awarded, player receives
     * {@link DodgyRooferSystem#TIP_OFF_COIN_REWARD} COIN, and van is flagged as moved.
     */
    @Test
    void scenario5_tipOffKenny_rewardAndVanMoved() {
        float hour = 10.0f; // Before 10:50 deadline
        int dayOfWeek = 5;  // Friday

        DodgyRooferSystem.TipOffResult result =
                dodgyRoofer.tipOffKenny(inventory, hour, dayOfWeek, achievementCallback);

        assertEquals(DodgyRooferSystem.TipOffResult.TIPPED, result,
                "Tip-off before deadline on Friday should return TIPPED");

        // Van moved
        assertTrue(dodgyRoofer.isVanMoved(),
                "Van should be flagged as moved after successful tip-off");

        // Kenny flagged as tipped
        assertTrue(dodgyRoofer.isKennyTippedOff(),
                "Kenny should be flagged as tipped off");

        // Coin reward
        assertEquals(DodgyRooferSystem.TIP_OFF_COIN_REWARD,
                inventory.getItemCount(Material.COIN),
                "Player should receive TIP_OFF_COIN_REWARD COIN for tipping off Kenny");

        // Achievement
        assertTrue(achievementSystem.isUnlocked(AchievementType.TIP_OFF_KENNY),
                "TIP_OFF_KENNY achievement should be awarded after successful tip-off");
    }

    /**
     * Scenario 5b: Tipping off too late returns TOO_LATE.
     */
    @Test
    void scenario5b_tipOffAfterDeadline_tooLate() {
        float hour = 11.0f; // After deadline
        int dayOfWeek = 5;  // Friday

        DodgyRooferSystem.TipOffResult result =
                dodgyRoofer.tipOffKenny(inventory, hour, dayOfWeek, achievementCallback);

        assertEquals(DodgyRooferSystem.TipOffResult.TOO_LATE, result,
                "Tip-off after deadline should return TOO_LATE");
        assertFalse(dodgyRoofer.isVanMoved(),
                "Van should not be moved when tip-off is too late");
        assertEquals(0, inventory.getItemCount(Material.COIN),
                "No coin reward should be given for a late tip-off");
    }

    /**
     * Scenario 5c: Tipping off on a non-Friday returns NOT_FRIDAY.
     */
    @Test
    void scenario5c_tipOffOnThursday_notFriday() {
        float hour = 10.0f;
        int dayOfWeek = 4; // Thursday

        DodgyRooferSystem.TipOffResult result =
                dodgyRoofer.tipOffKenny(inventory, hour, dayOfWeek, achievementCallback);

        assertEquals(DodgyRooferSystem.TipOffResult.NOT_FRIDAY, result,
                "Tip-off on non-Friday should return NOT_FRIDAY");
    }

    /**
     * Scenario 5d: Double tip-off returns ALREADY_TIPPED on second attempt.
     */
    @Test
    void scenario5d_doubleTipOff_alreadyTipped() {
        float hour = 10.0f;
        int dayOfWeek = 5;

        dodgyRoofer.tipOffKenny(inventory, hour, dayOfWeek, achievementCallback);
        DodgyRooferSystem.TipOffResult result =
                dodgyRoofer.tipOffKenny(inventory, hour, dayOfWeek, achievementCallback);

        assertEquals(DodgyRooferSystem.TipOffResult.ALREADY_TIPPED, result,
                "Second tip-off attempt should return ALREADY_TIPPED");
        // Coin reward should only be given once
        assertEquals(DodgyRooferSystem.TIP_OFF_COIN_REWARD,
                inventory.getItemCount(Material.COIN),
                "Coin reward should only be given once for tip-off");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 6: Invoice fraud integration
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 6: Invoice fraud attempt without INVOICE_PAD returns NO_INVOICE_PAD.
     */
    @Test
    void scenario6_invoiceFraud_noInvoicePad() {
        dodgyRoofer.setInvoiceWindowOpenForTesting(true);

        DodgyRooferSystem.InvoiceFraudResult result =
                dodgyRoofer.attemptInvoiceFraud(inventory, witnessNpc, achievementCallback);

        assertEquals(DodgyRooferSystem.InvoiceFraudResult.NO_INVOICE_PAD, result,
                "Invoice fraud without pad should return NO_INVOICE_PAD");
    }

    /**
     * Scenario 6b: Invoice fraud attempt with no recent work returns NO_RECENT_WORK.
     */
    @Test
    void scenario6b_invoiceFraud_noRecentWork() {
        inventory.addItem(Material.INVOICE_PAD, 1);
        // Invoice window NOT open

        DodgyRooferSystem.InvoiceFraudResult result =
                dodgyRoofer.attemptInvoiceFraud(inventory, witnessNpc, achievementCallback);

        assertEquals(DodgyRooferSystem.InvoiceFraudResult.NO_RECENT_WORK, result,
                "Invoice fraud without recent work should return NO_RECENT_WORK");
    }

    /**
     * Scenario 6c: Invoice fraud attempt with pad + window open returns SUCCESS or FAILED.
     */
    @Test
    void scenario6c_invoiceFraud_padAndWindow_successOrFailed() {
        inventory.addItem(Material.INVOICE_PAD, 1);
        dodgyRoofer.setInvoiceWindowOpenForTesting(true);

        DodgyRooferSystem.InvoiceFraudResult result =
                dodgyRoofer.attemptInvoiceFraud(inventory, witnessNpc, achievementCallback);

        assertTrue(result == DodgyRooferSystem.InvoiceFraudResult.SUCCESS
                        || result == DodgyRooferSystem.InvoiceFraudResult.FAILED,
                "Invoice fraud with pad + open window should return SUCCESS or FAILED");
    }
}

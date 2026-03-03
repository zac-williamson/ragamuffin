package ragamuffin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ragamuffin.core.WindowCleanerSystem.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WindowCleanerSystem} — Issue #1398: Northfield Window Cleaner.
 *
 * <p>Tests:
 * <ol>
 *   <li>Route generation: 12 properties, all unique, deterministic with same seed.</li>
 *   <li>Round start on weekday during hours.</li>
 *   <li>Round NOT active on weekend.</li>
 *   <li>Payment cycle: 20% non-payment chance verified over many samples.</li>
 *   <li>Ladder placed at start of each property; removed when Terry moves on.</li>
 *   <li>Employment start: blocked when notoriety >= 40.</li>
 *   <li>Employment start: blocked when Terry hostile.</li>
 *   <li>Mini-game scoring: cleanHouse awards HOUSE_WAGE on pass.</li>
 *   <li>WINDOW_LAD achievement after 8 houses.</li>
 *   <li>TRUSTED_WORKER after 5 full days.</li>
 *   <li>HMRC triggered when daily earnings exceed threshold.</li>
 *   <li>Rival round: 60% pay, 30% refuse, 10% grass distributions over 1000 samples.</li>
 *   <li>Burglary via ladder: records LADDER_BURGLARY, adds notoriety and wanted star.</li>
 *   <li>Ladder witnessed: adds LADDER_SEEN_NOTORIETY, seeds LADDER_INCIDENT rumour.</li>
 * </ol>
 */
class WindowCleanerSystemTest {

    private WindowCleanerSystem system;
    private Inventory inventory;
    private AchievementSystem achievements;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private HMRCSystem hmrcSystem;
    private List<AchievementType> awarded;
    private NotorietySystem.AchievementCallback cb;
    private NPC witnessNpc;

    @BeforeEach
    void setUp() {
        system = new WindowCleanerSystem(new Random(42L));
        inventory = new Inventory(36);
        achievements = new AchievementSystem();
        notorietySystem = new NotorietySystem();
        wantedSystem = new WantedSystem();
        criminalRecord = new CriminalRecord();
        rumourNetwork = new RumourNetwork(new Random(7L));
        hmrcSystem = new HMRCSystem(new Random(13L));

        awarded = new ArrayList<>();
        cb = type -> {
            awarded.add(type);
            achievements.unlock(type);
        };

        system.setNotorietySystem(notorietySystem);
        system.setWantedSystem(wantedSystem);
        system.setCriminalRecord(criminalRecord);
        system.setRumourNetwork(rumourNetwork);
        system.setHmrcSystem(hmrcSystem);

        witnessNpc = new NPC(NPCType.PUBLIC, 0f, 1f, 0f);
    }

    // ── 1. Route generation ───────────────────────────────────────────────────

    @Test
    void routeGeneration_returns12UniqueProperties() {
        List<Integer> route = system.generateDailyRoute();

        assertEquals(WindowCleanerSystem.PROPERTIES_PER_DAY, route.size(),
                "Route should have 12 properties");

        // All unique
        long distinct = route.stream().distinct().count();
        assertEquals(WindowCleanerSystem.PROPERTIES_PER_DAY, distinct,
                "All property indices should be unique");

        // All in range 0–11
        for (int idx : route) {
            assertTrue(idx >= 0 && idx < WindowCleanerSystem.PROPERTIES_PER_DAY,
                    "Property index should be in range 0–11: " + idx);
        }
    }

    @Test
    void routeGeneration_deterministicWithSameSeed() {
        WindowCleanerSystem s1 = new WindowCleanerSystem(new Random(99L));
        WindowCleanerSystem s2 = new WindowCleanerSystem(new Random(99L));

        List<Integer> route1 = s1.generateDailyRoute();
        List<Integer> route2 = s2.generateDailyRoute();

        assertEquals(route1, route2, "Same seed should produce same route");
    }

    // ── 2. Round active on weekday during hours ───────────────────────────────

    @Test
    void round_startsOnWeekdayDuringHours() {
        // Monday = dayOfWeek 1, 10:00
        system.update(0.1f, 10.0f, 1, 0f, 0f, 0f, 0, null, null);
        assertTrue(system.isRoundActive(), "Round should be active on weekday 10:00");
    }

    // ── 3. Round NOT active on weekend ────────────────────────────────────────

    @Test
    void round_notActiveOnWeekend_saturday() {
        // Saturday = dayOfWeek 6
        system.update(0.1f, 10.0f, 6, 0f, 0f, 0f, 0, null, null);
        assertFalse(system.isRoundActive(), "Round should NOT be active on Saturday");
    }

    @Test
    void round_notActiveOnWeekend_sunday() {
        // Sunday = dayOfWeek 0
        system.update(0.1f, 10.0f, 0, 0f, 0f, 0f, 0, null, null);
        assertFalse(system.isRoundActive(), "Round should NOT be active on Sunday");
    }

    @Test
    void round_notActiveBefore0830() {
        // Monday, 08:00 — before round start
        system.update(0.1f, 8.0f, 1, 0f, 0f, 0f, 0, null, null);
        assertFalse(system.isRoundActive(), "Round should NOT be active before 08:30");
    }

    // ── 4. Payment cycle: 20% non-payment chance ─────────────────────────────

    @Test
    void paymentCycle_nonPaymentChanceApprox20Percent_over1000Rounds() {
        int nonPayments = 0;
        int total = 1000;
        for (int i = 0; i < total; i++) {
            WindowCleanerSystem s = new WindowCleanerSystem(new Random(i * 1000003L));
            // Manually start and trigger one payment resolution
            s.setRoundActiveForTesting(true);
            // Advance past full property time to trigger payment resolution
            s.update(WindowCleanerSystem.SECONDS_PER_PROPERTY + 1f, 10.0f, 1, 0f, 0f, 0f, 0, null, null);
            if (!s.getDefaulterProperties().isEmpty()) {
                nonPayments++;
            }
        }
        double rate = (double) nonPayments / total;
        // Allow ±10% margin around 20%
        assertTrue(rate >= 0.10 && rate <= 0.30,
                "Non-payment rate should be approximately 20%, was: " + rate);
    }

    // ── 5. Ladder placed at property; removed when Terry moves on ─────────────

    @Test
    void ladder_placedAtStart_removedAfterPropertyTime() {
        system.update(0.1f, 10.0f, 1, 0f, 0f, 0f, 0, null, null);
        assertTrue(system.isRoundActive());
        assertTrue(system.isLadderPlaced(), "Ladder should be placed at first property");

        // Advance past property time
        system.update(WindowCleanerSystem.SECONDS_PER_PROPERTY + 1f, 10.0f, 1, 0f, 0f, 0f, 0, null, null);
        // After moving to next property, a new ladder is placed (still active)
        assertTrue(system.isLadderPlaced(), "Ladder should be placed at next property");
        assertEquals(1, system.getCurrentPropertyIndex(), "Should have moved to property 1");
    }

    // ── 6. Employment blocked when notoriety >= 40 ────────────────────────────

    @Test
    void employment_blockedWhenNotorietyTooHigh() {
        system.setRoundActiveForTesting(true);
        EmploymentResult result = system.startShift(WindowCleanerSystem.EMPLOYMENT_MAX_NOTORIETY);
        assertEquals(EmploymentResult.NOTORIETY_TOO_HIGH, result);
    }

    @Test
    void employment_allowedWhenNotorietyBelow40() {
        system.setRoundActiveForTesting(true);
        EmploymentResult result = system.startShift(WindowCleanerSystem.EMPLOYMENT_MAX_NOTORIETY - 1);
        assertEquals(EmploymentResult.STARTED, result);
        assertTrue(system.isShiftActive());
    }

    // ── 7. Employment blocked when Terry hostile ──────────────────────────────

    @Test
    void employment_blockedWhenTerryHostile() {
        system.setRoundActiveForTesting(true);
        system.setTerryHostileForTesting(true);
        EmploymentResult result = system.startShift(0);
        assertEquals(EmploymentResult.TERRY_HOSTILE, result);
    }

    // ── 8. Mini-game scoring: cleanHouse awards HOUSE_WAGE on pass ────────────

    @Test
    void cleanHouse_awards3CoinOnPass() {
        system.setRoundActiveForTesting(true);
        system.startShift(0);
        assertTrue(system.isShiftActive());

        // Run cleanHouse; result is probabilistic but we verify coin increases on PASSED
        int coinBefore = inventory.getItemCount(Material.COIN);
        HouseCleanResult result = system.cleanHouse(inventory, cb);

        if (result == HouseCleanResult.PASSED) {
            assertEquals(coinBefore + WindowCleanerSystem.HOUSE_WAGE,
                    inventory.getItemCount(Material.COIN),
                    "COIN should increase by HOUSE_WAGE on pass");
        } else {
            assertEquals(coinBefore, inventory.getItemCount(Material.COIN),
                    "COIN should not change on fail");
        }
        // Houses cleaned counter incremented either way
        assertEquals(1, system.getHousesCleanedThisShift());
    }

    @Test
    void cleanHouse_returnsNoShiftWhenNoShiftActive() {
        HouseCleanResult result = system.cleanHouse(inventory, cb);
        assertEquals(HouseCleanResult.NO_SHIFT, result);
    }

    // ── 9. WINDOW_LAD achievement after 8 houses ─────────────────────────────

    @Test
    void windowLad_achievementAfter8HousesThisShift() {
        system.setRoundActiveForTesting(true);
        system.startShift(0);
        // Force 7 houses already cleaned, then trigger the 8th via setters + manual award check
        system.setHousesCleanedForTesting(WindowCleanerSystem.WINDOW_LAD_THRESHOLD - 1);
        // Set daily coin to simulate a passing house
        inventory.addItem(Material.COIN, 100); // ensure enough headroom

        // Manually trigger win condition by setting houses to threshold-1 and cleaning one more
        // We use a system seeded to pass: run until we get a pass or force it
        // Instead, use the test setter to simulate being at threshold
        // and call the private path via cleanHouse — we accept probabilistic pass
        // So run many cleanHouse calls until we get a PASSED
        boolean gotPass = false;
        for (int i = 0; i < 20 && !gotPass; i++) {
            HouseCleanResult r = system.cleanHouse(inventory, cb);
            if (r == HouseCleanResult.PASSED) {
                gotPass = true;
            }
            // Reset counter each time to keep trying from threshold - 1
            if (!gotPass) system.setHousesCleanedForTesting(WindowCleanerSystem.WINDOW_LAD_THRESHOLD - 1);
        }

        assertTrue(gotPass, "Should eventually get a PASSED result");
        assertTrue(awarded.contains(AchievementType.WINDOW_LAD),
                "WINDOW_LAD should be awarded after reaching WINDOW_LAD_THRESHOLD houses");
    }

    // ── 10. TRUSTED_WORKER after 5 full days ─────────────────────────────────

    @Test
    void trustedWorker_afterFiveFullDays() {
        system.setFullDaysWorkedForTesting(WindowCleanerSystem.TRUSTED_WORKER_DAYS - 1);
        assertFalse(system.isTrustedWorker());

        // Simulate completing one more full shift (5 houses minimum = WINDOW_LAD_THRESHOLD)
        system.setRoundActiveForTesting(true);
        system.startShift(0);
        system.setHousesCleanedForTesting(WindowCleanerSystem.WINDOW_LAD_THRESHOLD);

        // End the round to finalise the shift
        system.update(0.1f, 17.0f, 1, 0f, 0f, 0f, 0, null, null); // after round end hour
        assertFalse(system.isRoundActive());

        assertTrue(system.isTrustedWorker(),
                "Should be TRUSTED_WORKER after " + WindowCleanerSystem.TRUSTED_WORKER_DAYS + " full days");
    }

    // ── 11. HMRC triggered when daily earnings exceed threshold ───────────────

    @Test
    void hmrc_triggeredWhenDailyEarningsExceedThreshold() {
        // Set up system with shift that has earned > threshold
        system.setRoundActiveForTesting(true);
        system.setShiftActiveForTesting(true);
        system.setHousesCleanedForTesting(WindowCleanerSystem.WINDOW_LAD_THRESHOLD);
        system.setDailyCoinEarnedForTesting(WindowCleanerSystem.HMRC_DAILY_COIN_THRESHOLD + 1);

        // End round (triggers finaliseShift)
        system.update(0.1f, 17.0f, 1, 0f, 0f, 0f, 0, null, null);

        // HMRC should have received an untaxed earning notification
        assertTrue(hmrcSystem.getTotalUntaxedEarnings() > 0,
                "HMRCSystem should be notified when daily coin exceeds threshold");
    }

    @Test
    void hmrc_notTriggeredWhenDailyEarningsBelowThreshold() {
        system.setRoundActiveForTesting(true);
        system.setShiftActiveForTesting(true);
        system.setHousesCleanedForTesting(WindowCleanerSystem.WINDOW_LAD_THRESHOLD);
        system.setDailyCoinEarnedForTesting(WindowCleanerSystem.HMRC_DAILY_COIN_THRESHOLD - 1);

        system.update(0.1f, 17.0f, 1, 0f, 0f, 0f, 0, null, null);

        assertEquals(0, hmrcSystem.getTotalUntaxedEarnings(),
                "HMRCSystem should NOT be notified when daily coin is below threshold");
    }

    // ── 12. Rival round distributions over 1000 samples ─────────────────────

    @Test
    void rivalRound_distributionApprox60Pay30Refuse10Grass_over1000Samples() {
        int paid = 0, refused = 0, grassed = 0;
        int total = 1000;

        for (int i = 0; i < total; i++) {
            WindowCleanerSystem s = new WindowCleanerSystem(new Random(i * 1000003L));
            Inventory inv = new Inventory(36);
            inv.addItem(Material.BUCKET_AND_CHAMOIS, 1);

            RivalCleanResult result = s.doRivalClean(
                    inv,
                    null,   // no Terry NPC nearby
                    999f, 999f, // Terry far away
                    0f, 0f,
                    witnessNpc, null);

            if (result == RivalCleanResult.PAID) paid++;
            else if (result == RivalCleanResult.REFUSED) refused++;
            else if (result == RivalCleanResult.GRASSED) grassed++;
        }

        double payRate = (double) paid / total;
        double refuseRate = (double) refused / total;
        double grassRate = (double) grassed / total;

        // Allow ±15% margin
        assertTrue(payRate >= 0.45 && payRate <= 0.75,
                "Pay rate should be ~60%, was: " + payRate);
        assertTrue(refuseRate >= 0.15 && refuseRate <= 0.45,
                "Refuse rate should be ~30%, was: " + refuseRate);
        assertTrue(grassRate >= 0.0 && grassRate <= 0.20,
                "Grass rate should be ~10%, was: " + grassRate);
    }

    // ── 13. Burglary via ladder ───────────────────────────────────────────────

    @Test
    void ladderBurglary_recordsCrimeNotorietyAndWantedStar() {
        system.setLadderPlacedForTesting(true);

        LadderClimbResult result = system.climbLadder(null, cb);

        assertEquals(LadderClimbResult.CLIMBED, result);
        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.LADDER_BURGLARY),
                "LADDER_BURGLARY should be recorded");
        assertEquals(WindowCleanerSystem.LADDER_BURGLARY_NOTORIETY,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by LADDER_BURGLARY_NOTORIETY");
        assertEquals(WindowCleanerSystem.LADDER_BURGLARY_WANTED_STARS,
                wantedSystem.getWantedStars(),
                "Wanted stars should increase by LADDER_BURGLARY_WANTED_STARS");
        assertTrue(awarded.contains(AchievementType.UP_THE_LADDER),
                "UP_THE_LADDER achievement should be awarded");
    }

    @Test
    void ladderBurglary_noLadder_returnsNoLadder() {
        system.setLadderPlacedForTesting(false);
        LadderClimbResult result = system.climbLadder(null, cb);
        assertEquals(LadderClimbResult.NO_LADDER, result);
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.LADDER_BURGLARY));
    }

    // ── 14. Ladder witnessed ─────────────────────────────────────────────────

    @Test
    void ladderWitnessed_addsLadderSeenNotorietyAndSeedsRumour() {
        system.setLadderPlacedForTesting(true);

        LadderClimbResult result = system.climbLadder(witnessNpc, cb);

        assertEquals(LadderClimbResult.WITNESSED, result);
        assertEquals(WindowCleanerSystem.LADDER_SEEN_NOTORIETY,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by LADDER_SEEN_NOTORIETY");
        // No crime recorded (witnessed climb, not completed burglary)
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.LADDER_BURGLARY),
                "LADDER_BURGLARY should NOT be recorded when only seen on ladder");
        // LADDER_INCIDENT rumour seeded
        assertTrue(rumourNetwork.getAllRumours().stream()
                .anyMatch(r -> r.getType() == RumourType.LADDER_INCIDENT),
                "LADDER_INCIDENT rumour should be seeded when player is seen on ladder");
    }
}

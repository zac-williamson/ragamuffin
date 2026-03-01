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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #967 — Northfield Taxi Rank: A1 Taxis, Dodgy Minicabs
 * &amp; Cash-Only Night Rides.
 *
 * <p>Eight scenarios:
 * <ol>
 *   <li>Rank accessibility — Mick is open 08:00–02:00; Dave operates 22:00–04:00</li>
 *   <li>Day fare deduction and fast-travel to destination</li>
 *   <li>Night fare calculation — fares increase after 22:00</li>
 *   <li>Tier 5 notoriety refusal</li>
 *   <li>TAXI_PASS ride counting — decrements per journey, falls back on exhaustion</li>
 *   <li>Dave spawning window — available 22:00–04:00 and refusing BALACLAVA wearers</li>
 *   <li>Mick rumour sharing — player can ask "Hear anything?" free of charge</li>
 *   <li>FARE_EVASION dialogue cross-reference — Mick demands cash upfront</li>
 * </ol>
 */
class Issue967TaxiSystemTest {

    private TaxiSystem taxiSystem;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        taxiSystem = new TaxiSystem(new Random(42));
        notorietySystem = new NotorietySystem();
        criminalRecord = new CriminalRecord();
        achievementSystem = new AchievementSystem();

        taxiSystem.setNotorietySystem(notorietySystem);
        taxiSystem.setCriminalRecord(criminalRecord);
        taxiSystem.setAchievementSystem(achievementSystem);

        inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 50);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Rank accessibility — Mick open 08:00–02:00; Dave open 22:00–04:00
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1a: Mick is open at 08:00 (opening time), 14:00 (midday), and 01:00 (after midnight).
     * Mick is closed at 03:00 and 06:00.
     */
    @Test
    void rankAccessibility_mickOpenHoursCorrect() {
        // Mick should be open at opening hour (08:00)
        assertTrue(taxiSystem.isMickOpen(TaxiSystem.MICK_OPEN_HOUR),
                "Mick should be open at exactly opening hour (08:00)");

        // Open in the middle of the day
        assertTrue(taxiSystem.isMickOpen(14.0f),
                "Mick should be open at 14:00");

        // Open past midnight (01:00)
        assertTrue(taxiSystem.isMickOpen(1.0f),
                "Mick should be open at 01:00 (past midnight, within service hours)");

        // Closed at 03:00 (after 02:00 close)
        assertFalse(taxiSystem.isMickOpen(3.0f),
                "Mick should be closed at 03:00 (outside service hours)");

        // Closed at 06:00 (before 08:00 open)
        assertFalse(taxiSystem.isMickOpen(6.0f),
                "Mick should be closed at 06:00 (before opening time)");
    }

    /**
     * Scenario 1b: Dave is available at 22:00 (opening), 23:00, and 03:00.
     * Dave is not available at 05:00 (after close) and 12:00 (daytime).
     */
    @Test
    void rankAccessibility_daveSpawnWindowCorrect() {
        // Dave open at 22:00 (opening time)
        assertTrue(taxiSystem.isDaveOpen(TaxiSystem.DAVE_OPEN_HOUR),
                "Dave should be available at exactly 22:00 (opening time)");

        // Open at 23:00
        assertTrue(taxiSystem.isDaveOpen(23.0f),
                "Dave should be available at 23:00");

        // Open past midnight at 03:00
        assertTrue(taxiSystem.isDaveOpen(3.0f),
                "Dave should be available at 03:00 (within night window)");

        // Closed at 05:00 (after 04:00 close)
        assertFalse(taxiSystem.isDaveOpen(5.0f),
                "Dave should not be available at 05:00 (after closing)");

        // Closed during the day at 12:00
        assertFalse(taxiSystem.isDaveOpen(12.0f),
                "Dave should not be available at 12:00 (daytime)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Day fare deduction and fast-travel to destination
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: Request a ride to the Park at 10:00 (daytime).
     * Verify the correct day fare (4 COIN) is deducted from the player's inventory.
     * Verify the result is SUCCESS. Verify lastDestination is DEST_PARK.
     */
    @Test
    void dayFare_parkFareDeductedAndDestinationRecorded() {
        float hour = 10.0f; // Daytime
        taxiSystem.forceSpawnMick();

        int coinsBefore = inventory.getItemCount(Material.COIN);
        TaxiSystem.MickResult result = taxiSystem.requestRide(hour, TaxiSystem.DEST_PARK, inventory);

        assertEquals(TaxiSystem.MickResult.SUCCESS, result,
                "Should succeed for a player with sufficient funds during daytime");

        int coinsAfter = inventory.getItemCount(Material.COIN);
        assertEquals(coinsBefore - TaxiSystem.FARE_PARK_DAY, coinsAfter,
                "Should deduct exactly " + TaxiSystem.FARE_PARK_DAY + " COIN for daytime Park fare");

        assertEquals(TaxiSystem.DEST_PARK, taxiSystem.getLastDestination(),
                "Last destination should be recorded as DEST_PARK");
    }

    /**
     * Scenario 2b: Request a ride to the Industrial Estate at 14:00 (daytime).
     * Verify 6 COIN deducted and result is SUCCESS.
     */
    @Test
    void dayFare_industrialFareDeducted() {
        float hour = 14.0f;
        taxiSystem.forceSpawnMick();

        int coinsBefore = inventory.getItemCount(Material.COIN);
        TaxiSystem.MickResult result = taxiSystem.requestRide(hour, TaxiSystem.DEST_INDUSTRIAL, inventory);

        assertEquals(TaxiSystem.MickResult.SUCCESS, result,
                "Should succeed for Industrial Estate during daytime");
        assertEquals(coinsBefore - TaxiSystem.FARE_INDUSTRIAL_DAY,
                inventory.getItemCount(Material.COIN),
                "Should deduct exactly " + TaxiSystem.FARE_INDUSTRIAL_DAY + " COIN for daytime Industrial fare");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Night fare calculation — fares increase after 22:00
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3a: computeFare() at 10:00 (day) returns FARE_PARK_DAY (4).
     * computeFare() at 23:00 (night) returns FARE_PARK_NIGHT (7).
     * Verify night fare is strictly higher than day fare for all destinations.
     */
    @Test
    void nightFare_allDestinationsHigherAtNight() {
        // Park: day 4, night 7
        assertEquals(TaxiSystem.FARE_PARK_DAY, taxiSystem.computeFare(10.0f, TaxiSystem.DEST_PARK),
                "Park day fare should be " + TaxiSystem.FARE_PARK_DAY);
        assertEquals(TaxiSystem.FARE_PARK_NIGHT, taxiSystem.computeFare(23.0f, TaxiSystem.DEST_PARK),
                "Park night fare should be " + TaxiSystem.FARE_PARK_NIGHT);
        assertTrue(TaxiSystem.FARE_PARK_NIGHT > TaxiSystem.FARE_PARK_DAY,
                "Night fare for Park must exceed day fare");

        // Industrial: day 6, night 10
        assertEquals(TaxiSystem.FARE_INDUSTRIAL_DAY, taxiSystem.computeFare(10.0f, TaxiSystem.DEST_INDUSTRIAL),
                "Industrial day fare should be " + TaxiSystem.FARE_INDUSTRIAL_DAY);
        assertEquals(TaxiSystem.FARE_INDUSTRIAL_NIGHT, taxiSystem.computeFare(23.0f, TaxiSystem.DEST_INDUSTRIAL),
                "Industrial night fare should be " + TaxiSystem.FARE_INDUSTRIAL_NIGHT);
        assertTrue(TaxiSystem.FARE_INDUSTRIAL_NIGHT > TaxiSystem.FARE_INDUSTRIAL_DAY,
                "Night fare for Industrial must exceed day fare");

        // High Street: day 3, night 5
        assertEquals(TaxiSystem.FARE_HIGH_STREET_DAY, taxiSystem.computeFare(10.0f, TaxiSystem.DEST_HIGH_STREET),
                "High Street day fare should be " + TaxiSystem.FARE_HIGH_STREET_DAY);
        assertEquals(TaxiSystem.FARE_HIGH_STREET_NIGHT, taxiSystem.computeFare(23.0f, TaxiSystem.DEST_HIGH_STREET),
                "High Street night fare should be " + TaxiSystem.FARE_HIGH_STREET_NIGHT);
        assertTrue(TaxiSystem.FARE_HIGH_STREET_NIGHT > TaxiSystem.FARE_HIGH_STREET_DAY,
                "Night fare for High Street must exceed day fare");
    }

    /**
     * Scenario 3b: Request a ride to the Park at 23:00 (nighttime).
     * Verify night fare (7 COIN) is deducted and result is SUCCESS.
     */
    @Test
    void nightFare_correctNightFareDeducted() {
        float hour = 23.0f; // Night
        taxiSystem.forceSpawnMick();

        int coinsBefore = inventory.getItemCount(Material.COIN);
        TaxiSystem.MickResult result = taxiSystem.requestRide(hour, TaxiSystem.DEST_PARK, inventory);

        assertEquals(TaxiSystem.MickResult.SUCCESS, result,
                "Should succeed for night Park journey");
        assertEquals(coinsBefore - TaxiSystem.FARE_PARK_NIGHT,
                inventory.getItemCount(Material.COIN),
                "Should deduct night Park fare of " + TaxiSystem.FARE_PARK_NIGHT + " COIN");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Tier 5 notoriety refusal
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Raise player notoriety to Tier 5. Request a ride from Mick.
     * Verify result is REFUSED. Verify inventory is unchanged (no coins deducted).
     */
    @Test
    void notorietyRefusal_tier5PlayerRefused() {
        float hour = 10.0f;
        taxiSystem.forceSpawnMick();

        // Push notoriety to Tier 5 (1000+)
        for (int i = 0; i < 1000; i++) {
            notorietySystem.addNotoriety(1, null);
        }
        assertEquals(5, notorietySystem.getTier(), "Notoriety should be Tier 5 after 1000 points");

        int coinsBefore = inventory.getItemCount(Material.COIN);
        TaxiSystem.MickResult result = taxiSystem.requestRide(hour, TaxiSystem.DEST_PARK, inventory);

        assertEquals(TaxiSystem.MickResult.REFUSED, result,
                "Mick should refuse a Tier 5 notoriety player");
        assertEquals(coinsBefore, inventory.getItemCount(Material.COIN),
                "Inventory should be unchanged when Mick refuses service");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: TAXI_PASS ride counting — decrements per journey, falls back
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5a: Buy a TAXI_PASS (5 journeys). Take 5 journeys — verify ride counter
     * decrements from 5 to 0, and that each journey returns SUCCESS_PASS (no coins deducted).
     */
    @Test
    void taxiPass_rideCounterDecrementsToZero() {
        float hour = 10.0f;
        taxiSystem.forceSpawnMick();

        // Buy a TAXI_PASS
        TaxiSystem.PassPurchaseResult purchaseResult = taxiSystem.buyTaxiPass(hour, inventory);
        assertEquals(TaxiSystem.PassPurchaseResult.SUCCESS, purchaseResult,
                "Should be able to buy TAXI_PASS when Mick is open and player has " + TaxiSystem.TAXI_PASS_PRICE + " COIN");
        assertEquals(TaxiSystem.TAXI_PASS_JOURNEYS, taxiSystem.getTaxiPassRidesRemaining(),
                "TAXI_PASS should start with " + TaxiSystem.TAXI_PASS_JOURNEYS + " rides");

        // Take 5 journeys
        for (int i = TaxiSystem.TAXI_PASS_JOURNEYS; i >= 1; i--) {
            int coinsBefore = inventory.getItemCount(Material.COIN);
            TaxiSystem.MickResult result = taxiSystem.requestRide(hour, TaxiSystem.DEST_PARK, inventory);
            assertEquals(TaxiSystem.MickResult.SUCCESS_PASS, result,
                    "Journey " + (TaxiSystem.TAXI_PASS_JOURNEYS - i + 1) + " should use TAXI_PASS (no coin cost)");
            assertEquals(coinsBefore, inventory.getItemCount(Material.COIN),
                    "No coins should be deducted when using TAXI_PASS");

            int expectedRides = i - 1;
            assertEquals(expectedRides, taxiSystem.getTaxiPassRidesRemaining(),
                    "Rides remaining should be " + expectedRides + " after journey " + (TaxiSystem.TAXI_PASS_JOURNEYS - i + 1));
        }

        assertEquals(0, taxiSystem.getTaxiPassRidesRemaining(),
                "TAXI_PASS should be exhausted (0 rides remaining) after " + TaxiSystem.TAXI_PASS_JOURNEYS + " journeys");
    }

    /**
     * Scenario 5b: TAXI_PASS is exhausted (0 rides). Next journey falls back to coin payment.
     * Verify result is SUCCESS (not SUCCESS_PASS) and coins are deducted.
     */
    @Test
    void taxiPass_exhaustedPassFallsBackToCoinPayment() {
        float hour = 10.0f;
        taxiSystem.forceSpawnMick();

        // Exhaust the pass manually
        taxiSystem.setTaxiPassRidesForTesting(0);

        int coinsBefore = inventory.getItemCount(Material.COIN);
        TaxiSystem.MickResult result = taxiSystem.requestRide(hour, TaxiSystem.DEST_PARK, inventory);

        assertEquals(TaxiSystem.MickResult.SUCCESS, result,
                "Journey should succeed with coin payment when TAXI_PASS is exhausted");
        assertEquals(coinsBefore - TaxiSystem.FARE_PARK_DAY,
                inventory.getItemCount(Material.COIN),
                "Day Park fare should be deducted when TAXI_PASS is exhausted");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 6: Dave spawning window and BALACLAVA refusal
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 6a: Request Dave's minicab at 22:30 (within his window).
     * Verify result is SUCCESS (or a valid variant), and coins are deducted.
     * Dave charges 1 COIN less than A1 Taxis.
     */
    @Test
    void daveSpawnWindow_availableAtNightAndFareCheaper() {
        float hour = 22.5f; // 22:30
        taxiSystem.forceSpawnDave();

        // Use a deterministic TaxiSystem with no detour and no package for simplicity
        TaxiSystem deterministicTaxi = new TaxiSystem(new Random(0) {
            @Override
            public float nextFloat() {
                return 0.99f; // above both thresholds — no detour, no package
            }
        });
        deterministicTaxi.setNotorietySystem(notorietySystem);
        deterministicTaxi.setCriminalRecord(criminalRecord);
        deterministicTaxi.setAchievementSystem(achievementSystem);
        deterministicTaxi.forceSpawnDave();

        int coinsBefore = inventory.getItemCount(Material.COIN);
        int expectedFare = taxiSystem.computeFare(hour, TaxiSystem.DEST_PARK) - TaxiSystem.DAVE_DISCOUNT;
        expectedFare = Math.max(1, expectedFare);

        TaxiSystem.DaveResult result = deterministicTaxi.requestDaveRide(hour, TaxiSystem.DEST_PARK, inventory);

        assertTrue(result == TaxiSystem.DaveResult.SUCCESS
                || result == TaxiSystem.DaveResult.DETOURED
                || result == TaxiSystem.DaveResult.SUCCESS_WITH_PACKAGE
                || result == TaxiSystem.DaveResult.DETOURED_WITH_PACKAGE,
                "Dave's ride should result in a journey (SUCCESS or variant), was: " + result);

        assertEquals(coinsBefore - expectedFare, inventory.getItemCount(Material.COIN),
                "Dave should charge " + expectedFare + " COIN (A1 fare - " + TaxiSystem.DAVE_DISCOUNT + " discount)");
    }

    /**
     * Scenario 6b: Request Dave's minicab while player holds a BALACLAVA.
     * Verify result is BALACLAVA_REFUSED and no coins are deducted.
     */
    @Test
    void daveSpawnWindow_refusesBalaclavasWearers() {
        float hour = 23.0f;
        taxiSystem.forceSpawnDave();

        inventory.addItem(Material.BALACLAVA, 1);
        int coinsBefore = inventory.getItemCount(Material.COIN);

        TaxiSystem.DaveResult result = taxiSystem.requestDaveRide(hour, TaxiSystem.DEST_PARK, inventory);

        assertEquals(TaxiSystem.DaveResult.BALACLAVA_REFUSED, result,
                "Dave should refuse a BALACLAVA-wearing player");
        assertEquals(coinsBefore, inventory.getItemCount(Material.COIN),
                "No coins should be deducted when Dave refuses service");
    }

    /**
     * Scenario 6c: Request Dave's minicab at 05:00 (outside his window).
     * Verify result is CLOSED.
     */
    @Test
    void daveSpawnWindow_closedOutsideWindow() {
        float hour = 5.0f; // After 04:00 close
        TaxiSystem.DaveResult result = taxiSystem.requestDaveRide(hour, TaxiSystem.DEST_PARK, inventory);

        assertEquals(TaxiSystem.DaveResult.CLOSED, result,
                "Dave should return CLOSED at 05:00 (outside 22:00–04:00 window)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 7: Mick rumour sharing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 7: Inject 2 rumours into Mick's queue via testing helper.
     * Verify getMickRumourCount() == 2. Call askMickForRumour() twice.
     * Verify each call returns a non-empty string (the rumour). Verify queue is now empty.
     * Call askMickForRumour() a third time — verify MICK_NO_RUMOURS is returned.
     */
    @Test
    void mickRumours_playerCanAskForRumoursForFree() {
        taxiSystem.forceSpawnMick();

        // Inject two rumours
        String rumour1 = "Something's going on near the estate tonight.";
        String rumour2 = "Council's been sniffing around the park.";
        taxiSystem.addRumourForTesting(rumour1);
        taxiSystem.addRumourForTesting(rumour2);

        assertEquals(2, taxiSystem.getMickRumourCount(),
                "Mick should have 2 rumours queued");

        // Ask for first rumour
        String heard1 = taxiSystem.askMickForRumour();
        assertEquals(rumour1, heard1,
                "First rumour retrieved should match injected rumour 1");
        assertEquals(1, taxiSystem.getMickRumourCount(),
                "Mick should have 1 rumour remaining after first ask");

        // Ask for second rumour
        String heard2 = taxiSystem.askMickForRumour();
        assertEquals(rumour2, heard2,
                "Second rumour retrieved should match injected rumour 2");
        assertEquals(0, taxiSystem.getMickRumourCount(),
                "Mick should have 0 rumours remaining after second ask");

        // Ask again — no rumours left
        String noRumour = taxiSystem.askMickForRumour();
        assertEquals(TaxiSystem.MICK_NO_RUMOURS, noRumour,
                "Mick should return MICK_NO_RUMOURS when queue is empty");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 8: FARE_EVASION dialogue cross-reference
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 8: Record a FARE_EVASION entry in the player's criminal record.
     * Request a ride from Mick. Verify result is CASH_UPFRONT (not SUCCESS_PASS
     * even if a TAXI_PASS is held). Verify coins are deducted (cash payment).
     * Verify a TAXI_PASS would not be used in this scenario.
     */
    @Test
    void fareEvasionCrossRef_mickDemandsCashUpfront() {
        float hour = 10.0f;
        taxiSystem.forceSpawnMick();

        // Record fare evasion on player's criminal record
        criminalRecord.record(CriminalRecord.CrimeType.FARE_EVASION);
        assertTrue(criminalRecord.getCount(CriminalRecord.CrimeType.FARE_EVASION) > 0,
                "Criminal record should show FARE_EVASION");

        // Give the player a TAXI_PASS — it should NOT be used
        inventory.addItem(Material.TAXI_PASS, 1);
        taxiSystem.setTaxiPassRidesForTesting(TaxiSystem.TAXI_PASS_JOURNEYS);

        int coinsBefore = inventory.getItemCount(Material.COIN);
        TaxiSystem.MickResult result = taxiSystem.requestRide(hour, TaxiSystem.DEST_PARK, inventory);

        // Mick demands cash, so TAXI_PASS is bypassed and coins are deducted
        assertEquals(TaxiSystem.MickResult.CASH_UPFRONT, result,
                "Mick should return CASH_UPFRONT for a player with FARE_EVASION on record");
        assertEquals(coinsBefore - TaxiSystem.FARE_PARK_DAY,
                inventory.getItemCount(Material.COIN),
                "Cash fare should be deducted (not TAXI_PASS) when FARE_EVASION is on record");

        // TAXI_PASS rides should be unchanged (not used)
        assertEquals(TaxiSystem.TAXI_PASS_JOURNEYS, taxiSystem.getTaxiPassRidesRemaining(),
                "TAXI_PASS rides should be unchanged when Mick demands cash upfront");
    }
}

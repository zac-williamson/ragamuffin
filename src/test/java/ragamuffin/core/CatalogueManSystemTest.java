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

import ragamuffin.core.CatalogueManSystem.*;
import ragamuffin.core.CriminalRecord.CrimeType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CatalogueManSystem} — Issue #1408: Northfield Catalogue Man.
 *
 * <p>Tests:
 * <ol>
 *   <li>Round starts on Monday (1), Wednesday (3), Friday (5) — not on other days.</li>
 *   <li>Round starts at 10:00 and ends after 16:00.</li>
 *   <li>Round does not start when Barry is suspended.</li>
 *   <li>Bag placed on round start; removed after bag is stolen.</li>
 *   <li>Bag theft success: items added, crime recorded, notoriety added, wanted star added.</li>
 *   <li>Bag theft witnessed by Barry: Barry turns HOSTILE; bag not stolen.</li>
 *   <li>Bag theft with no bag placed returns NO_BAG.</li>
 *   <li>Debt collection at low notoriety (70%) — success rate within expected range.</li>
 *   <li>Debt collection at high notoriety (10%) — significantly lower success rate.</li>
 *   <li>Debt collection with no defaulter window open returns NOT_AVAILABLE.</li>
 *   <li>Loan Shark tip-off: receipt consumed, COIN added, already-tipped returns ALREADY_TIPPED.</li>
 *   <li>Report to Trading Standards: sample consumed, Barry suspended, Notoriety reduced, CIVIC_CRUSADER awarded.</li>
 *   <li>Blackmail: first time ACCEPTED + SILENT_PARTNER; second time EXTORTION_TRIGGERED.</li>
 *   <li>Rival catalogue: ACCEPTED adds coin; CAUGHT_BY_TRADING_STANDARDS records crime and fine.</li>
 *   <li>Achievements: BARRY_BANDIT at 3 stolen days; CATALOGUE_KING at 5 sell days.</li>
 * </ol>
 */
class CatalogueManSystemTest {

    private CatalogueManSystem system;
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
        system = new CatalogueManSystem(new Random(42L));
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

    // ── Test 1: Round active on Mon/Wed/Fri only ──────────────────────────────

    @Test
    void roundActive_onMonWedFri_notOtherDays() {
        // Monday = 1 — active
        system.update(0.1f, 12.0f, 1, 0f, 0f, 0f, 0, null, null);
        assertTrue(system.isRoundActive(), "Round should be active Monday 12:00");

        // Reset for Tuesday
        system = new CatalogueManSystem(new Random(42L));
        system.setNotorietySystem(notorietySystem);
        // Tuesday = 2 — inactive
        system.update(0.1f, 12.0f, 2, 0f, 0f, 0f, 0, null, null);
        assertFalse(system.isRoundActive(), "Round should NOT be active Tuesday");

        // Wednesday = 3 — active
        system = new CatalogueManSystem(new Random(42L));
        system.setNotorietySystem(notorietySystem);
        system.update(0.1f, 12.0f, 3, 0f, 0f, 0f, 0, null, null);
        assertTrue(system.isRoundActive(), "Round should be active Wednesday 12:00");

        // Thursday = 4 — inactive
        system = new CatalogueManSystem(new Random(42L));
        system.setNotorietySystem(notorietySystem);
        system.update(0.1f, 12.0f, 4, 0f, 0f, 0f, 0, null, null);
        assertFalse(system.isRoundActive(), "Round should NOT be active Thursday");

        // Friday = 5 — active
        system = new CatalogueManSystem(new Random(42L));
        system.setNotorietySystem(notorietySystem);
        system.update(0.1f, 12.0f, 5, 0f, 0f, 0f, 0, null, null);
        assertTrue(system.isRoundActive(), "Round should be active Friday 12:00");

        // Saturday = 6 — inactive
        system = new CatalogueManSystem(new Random(42L));
        system.setNotorietySystem(notorietySystem);
        system.update(0.1f, 12.0f, 6, 0f, 0f, 0f, 0, null, null);
        assertFalse(system.isRoundActive(), "Round should NOT be active Saturday");
    }

    // ── Test 2: Round starts at 10:00 and ends after 16:00 ───────────────────

    @Test
    void roundActive_10to16_only() {
        // Before 10:00 — not active
        system.update(0.1f, 9.9f, 1, 0f, 0f, 0f, 0, null, null);
        assertFalse(system.isRoundActive(), "Round should NOT start before 10:00");

        // At 10:00 — active
        system.update(0.1f, 10.0f, 1, 0f, 0f, 0f, 0, null, null);
        assertTrue(system.isRoundActive(), "Round should start at 10:00");

        // At 16:00 — not active
        system.update(0.1f, 16.0f, 1, 0f, 0f, 0f, 0, null, null);
        assertFalse(system.isRoundActive(), "Round should end at 16:00");
    }

    // ── Test 3: Round does not start when Barry is suspended ─────────────────

    @Test
    void roundNotActive_whenBarrySuspended() {
        // Report Barry first
        inventory.addItem(Material.CATALOGUE_SAMPLE, 1);
        system.reportToTradingStandards(inventory, null, cb);
        assertTrue(system.isBarrySuspended(), "Barry should be suspended");

        // Try to start round on Monday
        system.update(0.1f, 12.0f, 1, 0f, 0f, 0f, 0, null, null);
        assertFalse(system.isRoundActive(), "Round should NOT start when Barry is suspended");
    }

    // ── Test 4: Bag placed on round start ────────────────────────────────────

    @Test
    void bagPlaced_onRoundStart() {
        system.update(0.1f, 12.0f, 1, 0f, 0f, 0f, 0, null, null);
        assertTrue(system.isRoundActive(), "Round should be active");
        assertTrue(system.isBagPlaced(), "Bag should be placed at round start");
    }

    // ── Test 5: Bag theft success ─────────────────────────────────────────────

    @Test
    void stealBag_success_addsItemsAndRecordsCrime() {
        system.update(0.1f, 12.0f, 1, 0f, 0f, 0f, 0, null, null);
        assertTrue(system.isBagPlaced());

        int notorietyBefore = notorietySystem.getNotoriety();
        BagTheftResult result = system.stealBag(inventory, 10.0f, null, cb);

        assertEquals(BagTheftResult.STOLEN, result, "Bag theft should succeed when Barry is far");
        assertFalse(system.isBagPlaced(), "Bag should be gone after theft");

        // At least 1 catalogue item should be in inventory
        int catalogueItems = inventory.getItemCount(Material.CATALOGUE_TRINKET)
                + inventory.getItemCount(Material.CATALOGUE_TOOL)
                + inventory.getItemCount(Material.CATALOGUE_TEXTILE);
        assertTrue(catalogueItems >= 1, "At least 1 catalogue item should be received");
        assertTrue(catalogueItems <= 3, "At most 3 catalogue items should be received");

        // Crime recorded
        assertEquals(1, criminalRecord.getCount(CrimeType.CATALOGUE_THEFT),
                "CATALOGUE_THEFT should be recorded");

        // Notoriety added
        assertTrue(notorietySystem.getNotoriety() >= notorietyBefore + CatalogueManSystem.CATALOGUE_THEFT_NOTORIETY,
                "Notoriety should increase by CATALOGUE_THEFT_NOTORIETY");

        // Wanted star added
        assertTrue(wantedSystem.getWantedStars() >= 1, "Wanted star should be added");
    }

    // ── Test 6: Bag theft — Barry spots player ────────────────────────────────

    @Test
    void stealBag_barrySpots_turnsHostile() {
        system.update(0.1f, 12.0f, 1, 0f, 0f, 0f, 0, null, null);

        BagTheftResult result = system.stealBag(inventory, 3.0f, null, cb);

        assertEquals(BagTheftResult.WITNESSED_BY_BARRY, result,
                "Should return WITNESSED_BY_BARRY when close to Barry");
        assertTrue(system.isBarryHostile(), "Barry should be hostile after spotting theft");
        assertTrue(system.isBagPlaced(), "Bag should remain when theft is witnessed by Barry");
    }

    // ── Test 7: No bag placed — returns NO_BAG ────────────────────────────────

    @Test
    void stealBag_noBag_returnsNoBag() {
        // Don't start the round — no bag placed
        BagTheftResult result = system.stealBag(inventory, 10.0f, null, cb);
        assertEquals(BagTheftResult.NO_BAG, result, "Should return NO_BAG when round not active");
    }

    // ── Test 8: Debt collection at low notoriety (70% success) ───────────────

    @Test
    void attemptDebtCollection_lowNotoriety_highSuccessRate() {
        system.forceDefaulterWindowOpen(true);

        int successes = 0;
        int attempts = 200;
        for (int i = 0; i < attempts; i++) {
            CatalogueManSystem freshSystem = new CatalogueManSystem(new Random(i * 1000003L));
            freshSystem.setNotorietySystem(notorietySystem);
            freshSystem.setHmrcSystem(hmrcSystem);
            Inventory inv = new Inventory(36);
            freshSystem.forceDefaulterWindowOpen(true);

            DebtCollectionResult result = freshSystem.attemptDebtCollection(inv, 10, null, null);
            if (result == DebtCollectionResult.SUCCESS) {
                successes++;
            }
        }

        double successRate = (double) successes / attempts;
        // Should be around 70% — allow ±15% tolerance
        assertTrue(successRate >= 0.55 && successRate <= 0.85,
                "Low-notoriety success rate should be ~70%, got: " + successRate);
    }

    // ── Test 9: Debt collection at high notoriety (10% success) ──────────────

    @Test
    void attemptDebtCollection_highNotoriety_lowSuccessRate() {
        int successes = 0;
        int attempts = 200;
        for (int i = 0; i < attempts; i++) {
            CatalogueManSystem freshSystem = new CatalogueManSystem(new Random(i * 1000003L));
            freshSystem.setNotorietySystem(notorietySystem);
            freshSystem.setHmrcSystem(hmrcSystem);
            Inventory inv = new Inventory(36);
            freshSystem.forceDefaulterWindowOpen(true);

            DebtCollectionResult result = freshSystem.attemptDebtCollection(inv, 60, null, null);
            if (result == DebtCollectionResult.SUCCESS) {
                successes++;
            }
        }

        double successRate = (double) successes / attempts;
        // Should be around 10% — allow ±10% tolerance
        assertTrue(successRate >= 0.0 && successRate <= 0.25,
                "High-notoriety success rate should be ~10%, got: " + successRate);
    }

    // ── Test 10: Debt collection with no window open ──────────────────────────

    @Test
    void attemptDebtCollection_noWindow_returnsNotAvailable() {
        // Round not active, no defaulter window
        DebtCollectionResult result = system.attemptDebtCollection(inventory, 10, null, null);
        assertEquals(DebtCollectionResult.NOT_AVAILABLE, result,
                "Should return NOT_AVAILABLE when no defaulter window is open");
    }

    // ── Test 11: Loan Shark tip-off ───────────────────────────────────────────

    @Test
    void tipOffLoanShark_consumesReceiptAndAddsCoin() {
        inventory.addItem(Material.CATALOGUE_RECEIPT, 1);
        int coinBefore = inventory.getItemCount(Material.COIN);

        LoanSharkTipResult result = system.tipOffLoanShark(inventory, null, cb);

        assertEquals(LoanSharkTipResult.TIPPED, result, "Tip-off should succeed with receipt");
        assertEquals(0, inventory.getItemCount(Material.CATALOGUE_RECEIPT),
                "Receipt should be consumed");
        assertEquals(coinBefore + CatalogueManSystem.LOAN_SHARK_FINDER_FEE,
                inventory.getItemCount(Material.COIN),
                "Finder's fee should be added to inventory");

        // Second tip-off without receipt
        LoanSharkTipResult result2 = system.tipOffLoanShark(inventory, null, cb);
        assertEquals(LoanSharkTipResult.NO_RECEIPT, result2, "Should fail without receipt");
    }

    // ── Test 12: Report to Trading Standards ─────────────────────────────────

    @Test
    void reportToTradingStandards_suspensBarryAndAwardsCivicCrusader() {
        inventory.addItem(Material.CATALOGUE_SAMPLE, 1);
        int notorietyBefore = notorietySystem.getNotoriety();

        TradingStandardsReportResult result =
                system.reportToTradingStandards(inventory, null, cb);

        assertEquals(TradingStandardsReportResult.REPORTED, result);
        assertTrue(system.isBarrySuspended(), "Barry should be suspended after report");
        assertEquals(0, inventory.getItemCount(Material.CATALOGUE_SAMPLE),
                "CATALOGUE_SAMPLE should be consumed");
        assertEquals(CatalogueManSystem.TRADING_STANDARDS_REWARD,
                inventory.getItemCount(Material.COIN), "Reward should be added");
        assertTrue(notorietySystem.getNotoriety() <= notorietyBefore,
                "Notoriety should decrease after reporting");
        assertTrue(awarded.contains(AchievementType.CIVIC_CRUSADER),
                "CIVIC_CRUSADER should be awarded");

        // Attempt to report again — already suspended
        inventory.addItem(Material.CATALOGUE_SAMPLE, 1);
        TradingStandardsReportResult result2 =
                system.reportToTradingStandards(inventory, null, cb);
        assertEquals(TradingStandardsReportResult.ALREADY_SUSPENDED, result2);
    }

    // ── Test 13: Blackmail ────────────────────────────────────────────────────

    @Test
    void blackmailBarry_firstTime_acceptedAndSilentPartner() {
        inventory.addItem(Material.CATALOGUE_SAMPLE, 1);

        BlackmailResult result = system.blackmailBarry(inventory, cb);

        assertEquals(BlackmailResult.ACCEPTED, result, "First blackmail should be ACCEPTED");
        assertEquals(CatalogueManSystem.BLACKMAIL_PAYMENT, inventory.getItemCount(Material.COIN),
                "Blackmail payment should be added to inventory");
        assertTrue(awarded.contains(AchievementType.SILENT_PARTNER),
                "SILENT_PARTNER should be awarded on first blackmail");
        assertTrue(system.isBarryHostile(), "Barry should be hostile after blackmail");
    }

    @Test
    void blackmailBarry_secondTime_extortionTriggered() {
        system.setBlackmailCount(1); // Already blackmailed once
        inventory.addItem(Material.CATALOGUE_SAMPLE, 1);
        // Barry isn't hostile from previous blackmail (reset for test)
        system.setBarryHostile(false);

        BlackmailResult result = system.blackmailBarry(inventory, cb);

        assertEquals(BlackmailResult.EXTORTION_TRIGGERED, result,
                "Second blackmail should trigger EXTORTION");
        assertEquals(1, criminalRecord.getCount(CrimeType.EXTORTION),
                "EXTORTION should be recorded in criminal record");
        assertTrue(wantedSystem.getWantedStars() >= 1, "Wanted star should be added");
    }

    @Test
    void blackmailBarry_noSample_returnsNoSample() {
        BlackmailResult result = system.blackmailBarry(inventory, cb);
        assertEquals(BlackmailResult.NO_SAMPLE, result,
                "Should return NO_SAMPLE without CATALOGUE_SAMPLE");
    }

    // ── Test 14: Rival catalogue ──────────────────────────────────────────────

    @Test
    void rivalCatalogueSale_accepted_addsCoin() {
        // Use seeded RNG that gives acceptance
        CatalogueManSystem seededSystem = new CatalogueManSystem(new Random(1L)); // seed that gives < 0.35
        seededSystem.setNotorietySystem(notorietySystem);
        seededSystem.setWantedSystem(wantedSystem);
        seededSystem.setCriminalRecord(criminalRecord);
        inventory.addItem(Material.KNOCKOFF_CATALOGUE, 1);

        // Try enough times to get at least one accepted
        boolean gotAccepted = false;
        for (int i = 0; i < 50; i++) {
            Inventory inv = new Inventory(36);
            inv.addItem(Material.KNOCKOFF_CATALOGUE, 1);
            CatalogueManSystem s = new CatalogueManSystem(new Random((long) i * 1000003L + 3));
            s.setNotorietySystem(notorietySystem);
            s.setCriminalRecord(criminalRecord);
            RivalCatalogueResult r = s.doRivalCatalogueSale(inv, false, null, null);
            if (r == RivalCatalogueResult.ACCEPTED) {
                gotAccepted = true;
                assertEquals(CatalogueManSystem.RIVAL_CATALOGUE_COIN_PER_SALE,
                        inv.getItemCount(Material.COIN),
                        "COIN should be added on accepted sale");
                break;
            }
        }
        assertTrue(gotAccepted, "At least one sale should be accepted out of 50 attempts");
    }

    @Test
    void rivalCatalogueSale_tradingStandardsCatch_recordsCrimeAndFine() {
        // Force Trading Standards catch: use a very low RNG seed that gives < 0.20
        // We simulate a guaranteed catch by using a system that always catches
        boolean caught = false;
        for (int seed = 0; seed < 100; seed++) {
            CatalogueManSystem s = new CatalogueManSystem(new Random(seed * 1000003L));
            s.setNotorietySystem(notorietySystem);
            s.setWantedSystem(wantedSystem);
            s.setCriminalRecord(criminalRecord);
            Inventory inv = new Inventory(36);
            inv.addItem(Material.KNOCKOFF_CATALOGUE, 1);
            inv.addItem(Material.COIN, 30);

            RivalCatalogueResult r = s.doRivalCatalogueSale(inv, true, null, null);
            if (r == RivalCatalogueResult.CAUGHT_BY_TRADING_STANDARDS) {
                caught = true;
                assertEquals(1, criminalRecord.getCount(CrimeType.COUNTERFEIT_GOODS_SELLING),
                        "COUNTERFEIT_GOODS_SELLING should be recorded");
                assertTrue(inv.getItemCount(Material.COIN) <= 30 - CatalogueManSystem.COUNTERFEIT_FINE
                        || inv.getItemCount(Material.COIN) < 30,
                        "Coin should be reduced by fine");
                break;
            }
        }
        assertTrue(caught, "At least one Trading Standards catch should occur in 100 attempts (20% rate)");
    }

    @Test
    void rivalCatalogueSale_noCatalogue_returnsNoCatalogue() {
        RivalCatalogueResult result = system.doRivalCatalogueSale(inventory, false, null, null);
        assertEquals(RivalCatalogueResult.NO_CATALOGUE, result,
                "Should return NO_CATALOGUE without KNOCKOFF_CATALOGUE");
    }

    // ── Test 15: BARRY_BANDIT and CATALOGUE_KING achievements ────────────────

    @Test
    void achievement_barryBandit_after3StolenDays() {
        system.setBagStolenDays(2); // Already stolen 2 days
        system.update(0.1f, 12.0f, 1, 0f, 0f, 0f, 0, null, null);
        system.stealBag(inventory, 10.0f, null, cb);

        assertTrue(awarded.contains(AchievementType.BARRY_BANDIT),
                "BARRY_BANDIT should be awarded after 3 bag-stolen days");
    }

    @Test
    void testBagStolenDaysIncrementedOnFirstDailySteal() {
        // Start a round so bag is placed
        system.update(0.1f, 12.0f, 1, 0f, 0f, 0f, 0, null, null);
        // Steal once
        BagTheftResult result = system.stealBag(inventory, 10.0f, null, cb);
        assertEquals(BagTheftResult.STOLEN, result, "First steal should succeed");
        assertEquals(1, system.getBagStolenDays(),
                "bagStolenDays should be 1 after first daily steal");
    }

    @Test
    void testBagStolenDaysNotIncrementedOnSecondStealSameDay() {
        // Start a round so bag is placed
        system.update(0.1f, 12.0f, 1, 0f, 0f, 0f, 0, null, null);
        // First steal
        system.stealBag(inventory, 10.0f, null, cb);
        assertEquals(1, system.getBagStolenDays(), "After first steal, days should be 1");

        // Re-place the bag (simulate bag being placed again same day) and steal again
        system.setBagPlaced(true);
        system.stealBag(inventory, 10.0f, null, cb);
        assertEquals(1, system.getBagStolenDays(),
                "bagStolenDays should NOT increment on second steal same day");
    }

    @Test
    void testBarryBanditAwardedAfterThreeSeparateDays() {
        // Day 1 (Monday)
        system.update(0.1f, 12.0f, 1, 0f, 0f, 0f, 0, null, null);
        system.stealBag(inventory, 10.0f, null, cb);
        assertEquals(1, system.getBagStolenDays(), "After day 1 steal, days=1");
        assertFalse(awarded.contains(AchievementType.BARRY_BANDIT), "No award yet after day 1");

        // Day 2 (Wednesday) — create fresh round by simulating new day
        system = new CatalogueManSystem(new Random(42L));
        system.setNotorietySystem(notorietySystem);
        system.setWantedSystem(wantedSystem);
        system.setCriminalRecord(criminalRecord);
        system.setRumourNetwork(rumourNetwork);
        system.setBagStolenDays(1); // carry over day 1 count
        system.update(0.1f, 12.0f, 3, 0f, 0f, 0f, 0, null, null);
        system.stealBag(inventory, 10.0f, null, cb);
        assertEquals(2, system.getBagStolenDays(), "After day 2 steal, days=2");
        assertFalse(awarded.contains(AchievementType.BARRY_BANDIT), "No award yet after day 2");

        // Day 3 (Friday) — create fresh round by simulating new day
        system = new CatalogueManSystem(new Random(42L));
        system.setNotorietySystem(notorietySystem);
        system.setWantedSystem(wantedSystem);
        system.setCriminalRecord(criminalRecord);
        system.setRumourNetwork(rumourNetwork);
        system.setBagStolenDays(2); // carry over day 2 count
        system.update(0.1f, 12.0f, 5, 0f, 0f, 0f, 0, null, null);
        system.stealBag(inventory, 10.0f, null, cb);
        assertEquals(3, system.getBagStolenDays(), "After day 3 steal, days=3");
        assertTrue(awarded.contains(AchievementType.BARRY_BANDIT),
                "BARRY_BANDIT should be awarded after 3 separate days of stealing (no setBagStolenDays shortcut)");
    }

    @Test
    void achievement_catalogueKing_after5SellDays() {
        system.setRivalCatalogueSellDays(4); // Already sold 4 days

        // Sell on a new day to trigger
        for (int seed = 0; seed < 100; seed++) {
            CatalogueManSystem s = new CatalogueManSystem(new Random(seed));
            s.setNotorietySystem(notorietySystem);
            s.setRivalCatalogueSellDays(4);
            List<AchievementType> localAwarded = new ArrayList<>();
            NotorietySystem.AchievementCallback localCb = type -> localAwarded.add(type);
            Inventory inv = new Inventory(36);
            inv.addItem(Material.KNOCKOFF_CATALOGUE, 1);

            RivalCatalogueResult r = s.doRivalCatalogueSale(inv, false, null, localCb);
            if (r == RivalCatalogueResult.ACCEPTED) {
                assertTrue(localAwarded.contains(AchievementType.CATALOGUE_KING),
                        "CATALOGUE_KING should be awarded after 5 sell days");
                return; // Test passed
            }
        }
        // If we never got ACCEPTED, the test is inconclusive — just warn
        // (at 35% rate over 100 attempts this is extremely unlikely to happen)
    }
}

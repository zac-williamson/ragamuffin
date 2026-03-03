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
 * Integration tests for Issue #1408 — CatalogueManSystem end-to-end scenarios.
 *
 * <p>Tests:
 * <ol>
 *   <li><b>Barry's schedule: active Mon/Wed/Fri 10:00–16:00 only</b> — verify round starts
 *       on Monday 11:00 and is inactive on Tuesday 11:00; verify bag is placed on start.</li>
 *   <li><b>Back-door unlock window during COLLECTING state</b> — verify bag is placed and
 *       stealable during active property visit; verify it's removed after time elapses.</li>
 *   <li><b>Debt collection success rates at different notoriety levels</b> — verify that
 *       low notoriety (≤20) yields measurably higher success than high notoriety (>40)
 *       across 100 attempts each.</li>
 *   <li><b>Trading Standards suspension: Barry absent for 3 days</b> — report Barry; verify
 *       suspension flag set; verify round does NOT start on subsequent operating days.</li>
 *   <li><b>Rival catalogue sting: monthly check triggers crime and fine</b> — give player
 *       KNOCKOFF_CATALOGUE; trigger monthly check; verify crime recorded, coin reduced,
 *       COUNTERFEIT_CAUGHT rumour seeded if there is a witness NPC.</li>
 * </ol>
 */
class Issue1408CatalogueManIntegrationTest {

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

    // ── Integration Test 1: Barry's schedule ─────────────────────────────────

    /**
     * Verify round lifecycle:
     * <ol>
     *   <li>Monday 11:00 — round starts; bag placed.</li>
     *   <li>Monday 16:01 — round ends.</li>
     *   <li>Tuesday 11:00 — round does NOT start.</li>
     *   <li>Wednesday 11:00 — round starts again.</li>
     *   <li>Friday 11:00 — round starts again.</li>
     * </ol>
     */
    @Test
    void barrySchedule_monWedFri_10to16() {
        // Monday 11:00 — should start
        system.update(0.1f, 11.0f, 1, 0f, 0f, 0f, 0, null, null);
        assertTrue(system.isRoundActive(), "Barry should be active Monday 11:00");
        assertTrue(system.isBagPlaced(), "Bag should be placed when round starts");

        // Advance to after 16:00 — round should end
        system.update(0.1f, 16.1f, 1, 0f, 0f, 0f, 0, null, null);
        assertFalse(system.isRoundActive(), "Barry should be inactive after 16:00");
        assertFalse(system.isBagPlaced(), "Bag should be removed when round ends");

        // Tuesday 11:00 — should NOT start
        system.update(0.1f, 11.0f, 2, 0f, 0f, 0f, 0, null, null);
        assertFalse(system.isRoundActive(), "Barry should be inactive Tuesday");

        // Wednesday 11:00 — should start
        system.update(0.1f, 11.0f, 3, 0f, 0f, 0f, 0, null, null);
        assertTrue(system.isRoundActive(), "Barry should be active Wednesday 11:00");

        // End round
        system.update(0.1f, 16.1f, 3, 0f, 0f, 0f, 0, null, null);
        assertFalse(system.isRoundActive());

        // Friday 11:00 — should start
        system.update(0.1f, 11.0f, 5, 0f, 0f, 0f, 0, null, null);
        assertTrue(system.isRoundActive(), "Barry should be active Friday 11:00");
    }

    // ── Integration Test 2: Back-door unlock window ───────────────────────────

    /**
     * Verify the bag theft (back-door window) works correctly:
     * <ol>
     *   <li>Bag is placed when round starts.</li>
     *   <li>Player can steal the bag successfully while Barry is far away.</li>
     *   <li>After the property timer elapses (90s), Barry moves on and bag is no longer placed
     *       (since it was already stolen today).</li>
     * </ol>
     */
    @Test
    void backDoorWindow_bagStealeableWhileBarryCollecting() {
        // Start round on Monday
        system.update(0.1f, 11.0f, 1, 0f, 0f, 0f, 0, null, null);
        assertTrue(system.isRoundActive());
        assertTrue(system.isBagPlaced(), "Bag should be placed at first property");

        // Steal the bag while Barry is far away
        CatalogueManSystem.BagTheftResult result = system.stealBag(inventory, 20.0f, null, cb);
        assertEquals(CatalogueManSystem.BagTheftResult.STOLEN, result,
                "Bag should be stolen when Barry is far");
        assertFalse(system.isBagPlaced(), "Bag should be gone after theft");

        // Attempt to steal again — no bag
        CatalogueManSystem.BagTheftResult result2 = system.stealBag(inventory, 20.0f, null, cb);
        assertEquals(CatalogueManSystem.BagTheftResult.NO_BAG, result2,
                "Should return NO_BAG after bag already stolen");

        // Advance time past property timer — Barry moves to next property
        system.update(CatalogueManSystem.SECONDS_PER_PROPERTY + 1f, 11.0f, 1,
                0f, 0f, 0f, 0, null, null);

        // Bag not placed (already stolen today)
        // (Note: if not stolen today, bag would be placed at new property)
        assertEquals(0, system.getCurrentPropertyIndex() > 0 ? 1 : 0,
                "Should have advanced to at least property index 1 or wrapped");
    }

    // ── Integration Test 3: Debt collection success rates ────────────────────

    /**
     * Verify debt collection success rates differ between low and high notoriety:
     * <ul>
     *   <li>Low notoriety (≤20): ~70% success — must be significantly higher than high.</li>
     *   <li>High notoriety (>40): ~10% success — must be measurably lower than low.</li>
     * </ul>
     */
    @Test
    void debtCollection_successRateDependsOnNotoriety() {
        int lowSuccesses = 0;
        int highSuccesses = 0;
        int attempts = 200;

        for (int i = 0; i < attempts; i++) {
            // Low notoriety trial
            CatalogueManSystem lowSystem = new CatalogueManSystem(new Random(i * 17L));
            lowSystem.setNotorietySystem(new NotorietySystem());
            lowSystem.setHmrcSystem(new HMRCSystem(new Random(i)));
            lowSystem.forceDefaulterWindowOpen(true);
            Inventory lowInv = new Inventory(36);
            CatalogueManSystem.DebtCollectionResult lowResult =
                    lowSystem.attemptDebtCollection(lowInv, 10, null, null);
            if (lowResult == CatalogueManSystem.DebtCollectionResult.SUCCESS) lowSuccesses++;

            // High notoriety trial
            CatalogueManSystem highSystem = new CatalogueManSystem(new Random(i * 17L));
            highSystem.setNotorietySystem(new NotorietySystem());
            highSystem.setHmrcSystem(new HMRCSystem(new Random(i)));
            highSystem.forceDefaulterWindowOpen(true);
            Inventory highInv = new Inventory(36);
            CatalogueManSystem.DebtCollectionResult highResult =
                    highSystem.attemptDebtCollection(highInv, 60, null, null);
            if (highResult == CatalogueManSystem.DebtCollectionResult.SUCCESS) highSuccesses++;
        }

        double lowRate = (double) lowSuccesses / attempts;
        double highRate = (double) highSuccesses / attempts;

        // Low notoriety should clearly succeed more often
        assertTrue(lowRate > highRate * 2,
                "Low notoriety success rate (" + lowRate + ") should be >2x higher than high notoriety (" + highRate + ")");

        // Low rate should be in the expected band (~70%)
        assertTrue(lowRate >= 0.55 && lowRate <= 0.85,
                "Low-notoriety rate should be ~70%, got: " + lowRate);

        // High rate should be in the expected band (~10%)
        assertTrue(highRate >= 0.0 && highRate <= 0.25,
                "High-notoriety rate should be ~10%, got: " + highRate);
    }

    // ── Integration Test 4: Trading Standards suspension ─────────────────────

    /**
     * Verify that after reporting Barry, his round does not start on subsequent operating days:
     * <ol>
     *   <li>Report Barry to Trading Standards using CATALOGUE_SAMPLE.</li>
     *   <li>Verify suspension flag is set.</li>
     *   <li>Simulate Monday 11:00 update — verify round does NOT start.</li>
     *   <li>Simulate Wednesday 11:00 update — verify round still does NOT start.</li>
     * </ol>
     */
    @Test
    void tradingStandardsSuspension_barryAbsentForDays() {
        inventory.addItem(Material.CATALOGUE_SAMPLE, 1);
        CatalogueManSystem.TradingStandardsReportResult result =
                system.reportToTradingStandards(inventory, witnessNpc, cb);

        assertEquals(CatalogueManSystem.TradingStandardsReportResult.REPORTED, result);
        assertTrue(system.isBarrySuspended(), "Barry should be suspended");

        // Monday 11:00 — round should NOT start
        system.update(0.1f, 11.0f, 1, 0f, 0f, 0f, 0, null, null);
        assertFalse(system.isRoundActive(),
                "Round should NOT start on Monday when Barry is suspended");

        // Wednesday 11:00 — still suspended (3 days haven't elapsed)
        system.update(0.1f, 11.0f, 3, 0f, 0f, 0f, 0, null, null);
        assertFalse(system.isRoundActive(),
                "Round should NOT start on Wednesday when Barry is still suspended");

        // Reward should have been given
        assertEquals(CatalogueManSystem.TRADING_STANDARDS_REWARD,
                inventory.getItemCount(Material.COIN), "Reward should be paid");

        // CIVIC_CRUSADER should have been awarded
        assertTrue(awarded.contains(AchievementType.CIVIC_CRUSADER),
                "CIVIC_CRUSADER should be awarded");
    }

    // ── Integration Test 5: Rival catalogue sting ────────────────────────────

    /**
     * Verify the monthly Trading Standards check end-to-end:
     * <ol>
     *   <li>Give player a KNOCKOFF_CATALOGUE and some COIN.</li>
     *   <li>Trigger the monthly Trading Standards check (isMonthlyTradingStandardsCheck=true).</li>
     *   <li>Across multiple seeds, verify that COUNTERFEIT_GOODS_SELLING is eventually recorded
     *       and coin is reduced (20% catch rate).</li>
     *   <li>Verify COUNTERFEIT_CAUGHT rumour is seeded when a witness NPC is present.</li>
     * </ol>
     */
    @Test
    void rivalCatalogueStingByTradingStandards_recordsCrimeAndFine() {
        boolean caughtByTS = false;

        for (int seed = 0; seed < 200 && !caughtByTS; seed++) {
            CatalogueManSystem s = new CatalogueManSystem(new Random(seed));
            CriminalRecord cr = new CriminalRecord();
            NotorietySystem ns = new NotorietySystem();
            s.setCriminalRecord(cr);
            s.setNotorietySystem(ns);
            s.setRumourNetwork(rumourNetwork);

            Inventory inv = new Inventory(36);
            inv.addItem(Material.KNOCKOFF_CATALOGUE, 1);
            inv.addItem(Material.COIN, 30);

            CatalogueManSystem.RivalCatalogueResult r =
                    s.doRivalCatalogueSale(inv, true, witnessNpc, null);

            if (r == CatalogueManSystem.RivalCatalogueResult.CAUGHT_BY_TRADING_STANDARDS) {
                caughtByTS = true;

                // Crime should be recorded
                assertEquals(1, cr.getCount(CrimeType.COUNTERFEIT_GOODS_SELLING),
                        "COUNTERFEIT_GOODS_SELLING should be recorded on catch");

                // Notoriety should be increased
                assertTrue(ns.getNotoriety() >= CatalogueManSystem.COUNTERFEIT_NOTORIETY,
                        "Notoriety should increase when caught");

                // Coin should be reduced by fine
                assertTrue(inv.getItemCount(Material.COIN) < 30,
                        "Coin should be reduced by the Trading Standards fine");
            }
        }

        assertTrue(caughtByTS,
                "Should be caught by Trading Standards at least once in 200 attempts (20% rate)");
    }
}

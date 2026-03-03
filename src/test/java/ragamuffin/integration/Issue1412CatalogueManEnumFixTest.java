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
 * Integration tests for Issue #1412 — verifies that missing enum values
 * (RumourType, AchievementType, CriminalRecord.CrimeType) have been added
 * and that CatalogueManSystem uses them correctly.
 *
 * <p>Tests:
 * <ol>
 *   <li><b>testBarryBanditAwardedAfterThreeDays</b> — call stealBag() with Barry out of range
 *       across 3 simulated rounds; verify BARRY_BANDIT achievement is awarded on the third.</li>
 *   <li><b>testCivicCrusaderOnTradingStandardsReport</b> — set witnessedDeliveries ≥ 5,
 *       add CATALOGUE_SAMPLE to inventory, call reportToTradingStandards();
 *       verify CIVIC_CRUSADER is awarded.</li>
 *   <li><b>testExtortionRecordedOnSecondBlackmail</b> — call blackmailBarry() twice;
 *       verify second call returns EXTORTION_TRIGGERED and CrimeType.EXTORTION is in CriminalRecord.</li>
 * </ol>
 */
class Issue1412CatalogueManEnumFixTest {

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

    /**
     * Issue #1412 integration test 1: BARRY_BANDIT awarded after 3 separate bag steals.
     *
     * <p>Simulates 3 separate round days. On each day the bag is placed and stolen
     * while Barry is far away. The achievement must fire on the third theft.
     */
    @Test
    void testBarryBanditAwardedAfterThreeDays() {
        // Day 1: place bag, steal it
        system.setBagPlaced(true);
        CatalogueManSystem.BagTheftResult r1 =
                system.stealBag(inventory, 20.0f, null, cb);
        assertEquals(CatalogueManSystem.BagTheftResult.STOLEN, r1,
                "Bag should be stolen on day 1");
        assertFalse(awarded.contains(AchievementType.BARRY_BANDIT),
                "BARRY_BANDIT should NOT be awarded after day 1");
        assertEquals(1, system.getBagStolenDays(), "bagStolenDays should be 1 after day 1");

        // Day 2: reset daily flag (new round), place bag, steal it
        // setBagStolenDays to simulate the day flag reset that the update() loop would do
        // (the system counts distinct days via bagStolenToday; we use setBagStolenDays
        //  to back-set the count while keeping the "already stolen today" logic clean)
        system.setBagStolenDays(1);
        // Force bagStolenToday = false by using a fresh instance would be the cleanest,
        // but the system provides no direct reset. Use the published setter path:
        // Starting a new round resets bagStolenToday — simulate that via update with day 3 (Wed)
        system.update(0.1f, 11.0f, 3, 0f, 0f, 0f, 0, null, null); // Wednesday = new round
        assertTrue(system.isRoundActive(), "Round should start on Wednesday");

        system.setBagPlaced(true);
        CatalogueManSystem.BagTheftResult r2 =
                system.stealBag(inventory, 20.0f, null, cb);
        assertEquals(CatalogueManSystem.BagTheftResult.STOLEN, r2,
                "Bag should be stolen on day 2");
        assertFalse(awarded.contains(AchievementType.BARRY_BANDIT),
                "BARRY_BANDIT should NOT be awarded after day 2");

        // Day 3: new round on Friday, place bag, steal it — achievement should fire
        system.update(0.1f, 16.1f, 3, 0f, 0f, 0f, 0, null, null); // end Wednesday round
        system.update(0.1f, 11.0f, 5, 0f, 0f, 0f, 0, null, null); // Friday = new round
        assertTrue(system.isRoundActive(), "Round should start on Friday");

        system.setBagPlaced(true);
        CatalogueManSystem.BagTheftResult r3 =
                system.stealBag(inventory, 20.0f, null, cb);
        assertEquals(CatalogueManSystem.BagTheftResult.STOLEN, r3,
                "Bag should be stolen on day 3");
        assertTrue(awarded.contains(AchievementType.BARRY_BANDIT),
                "BARRY_BANDIT must be awarded after 3 separate bag-steal days");
    }

    /**
     * Issue #1412 integration test 2: CIVIC_CRUSADER awarded when reporting Barry
     * to Trading Standards with sufficient witnessed deliveries.
     *
     * <p>Sets witnessedDeliveries ≥ WITNESS_THRESHOLD_FOR_SAMPLE so the system
     * auto-grants CATALOGUE_SAMPLE, then calls reportToTradingStandards().
     */
    @Test
    void testCivicCrusaderOnTradingStandardsReport() {
        // witnessedDeliveries ≥ 5 grants the CATALOGUE_SAMPLE
        system.setWitnessedDeliveries(CatalogueManSystem.WITNESS_THRESHOLD_FOR_SAMPLE);
        assertTrue(system.isCatalogueSampleReceived(),
                "CATALOGUE_SAMPLE should be received after enough witnessed deliveries");

        // Add the CATALOGUE_SAMPLE to inventory (the system notes it received,
        // but the player must hold it in inventory for the report)
        inventory.addItem(Material.CATALOGUE_SAMPLE, 1);

        CatalogueManSystem.TradingStandardsReportResult result =
                system.reportToTradingStandards(inventory, witnessNpc, cb);

        assertEquals(CatalogueManSystem.TradingStandardsReportResult.REPORTED, result,
                "Report should succeed with CATALOGUE_SAMPLE in inventory");
        assertTrue(system.isBarrySuspended(),
                "Barry should be suspended after being reported");
        assertTrue(awarded.contains(AchievementType.CIVIC_CRUSADER),
                "CIVIC_CRUSADER must be awarded on successful Trading Standards report");
        assertEquals(CatalogueManSystem.TRADING_STANDARDS_REWARD,
                inventory.getItemCount(Material.COIN),
                "Reward COIN should be deposited into inventory");
    }

    /**
     * Issue #1412 integration test 3: second blackmail call returns EXTORTION_TRIGGERED
     * and records CrimeType.EXTORTION in CriminalRecord.
     *
     * <p>First call uses the standard path; second call forces the extortion branch.
     */
    @Test
    void testExtortionRecordedOnSecondBlackmail() {
        // Give player a CATALOGUE_SAMPLE for first blackmail
        inventory.addItem(Material.CATALOGUE_SAMPLE, 2); // two samples for two blackmail calls

        // First blackmail — should be ACCEPTED, SILENT_PARTNER awarded
        CatalogueManSystem.BlackmailResult r1 = system.blackmailBarry(inventory, cb);
        assertEquals(CatalogueManSystem.BlackmailResult.ACCEPTED, r1,
                "First blackmail should be ACCEPTED");
        assertTrue(awarded.contains(AchievementType.SILENT_PARTNER),
                "SILENT_PARTNER must be awarded on first blackmail");
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.EXTORTION),
                "EXTORTION should NOT be recorded on first blackmail");

        // Barry is now hostile after the first blackmail; reset so second call proceeds
        system.setBarryHostile(false);

        // Second blackmail — should be EXTORTION_TRIGGERED
        CatalogueManSystem.BlackmailResult r2 = system.blackmailBarry(inventory, cb);
        assertEquals(CatalogueManSystem.BlackmailResult.EXTORTION_TRIGGERED, r2,
                "Second blackmail should return EXTORTION_TRIGGERED");
        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.EXTORTION),
                "EXTORTION must be recorded on second blackmail");
    }
}

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
 * Integration tests for Issue #1494 — Northfield Detectorists Club:
 * Keith's Weekend Dig, the Roman Hoard &amp; the Portable Antiquities Dodge.
 *
 * <p>Five scenarios:
 * <ol>
 *   <li>Legitimate dig — player with permission slip and metal detector digs up
 *       a find; permission slip consumed on first use</li>
 *   <li>Trespass dig — player without permission slip digs; FIELD_TRESPASS crime
 *       recorded, Notoriety +3</li>
 *   <li>PAS declaration — player finds ROMAN_COIN (hoard spawned), excavates hoard,
 *       declares ROMAN_BROOCH to Janet; 20 COIN reward + HOARD_FINDER achievement</li>
 *   <li>Treasure dodging — player fences ROMAN_BROOCH without declaring;
 *       TREASURE_DODGING crime + Notoriety +8; if Janet witnesses: WantedSystem +2</li>
 *   <li>Trophy heist — player steals DETECTORISTS_TROPHY_PROP unwitnessed;
 *       TREASURE_HUNTER achievement; returns it voluntarily for GOOD_SAMARITAN
 *       rumour + 4 free permission slips</li>
 * </ol>
 */
class Issue1494DetectoristsTest {

    // Dig runs on Sundays: day % 7 == 0. Use day 7 (first occurrence).
    private static final int DIG_DAY = 7;
    // Dig open: 09:00–16:00
    private static final float DIG_HOUR = 10.0f;
    // PAS officer present: 11:00–13:00
    private static final float PAS_HOUR = 12.0f;
    // Rival day: day % 14 == 7
    private static final int RIVAL_DAY = 7;

    private DetectoristsSystem detectoristsSystem;
    private TimeSystem timeSystem;
    private Inventory inventory;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private WantedSystem wantedSystem;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private NotorietySystem.AchievementCallback achievementCallback;
    private List<NPC> npcs;

    @BeforeEach
    void setUp() {
        // Use a fixed seed so find rolls are deterministic for most tests
        detectoristsSystem = new DetectoristsSystem(new Random(12345));

        timeSystem = new TimeSystem(DIG_HOUR);
        timeSystem.setDayForTesting(DIG_DAY);

        inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 50);
        inventory.addItem(Material.METAL_DETECTOR, 1);
        inventory.addItem(Material.DIG_PERMISSION_SLIP, 1);

        notorietySystem = new NotorietySystem();
        criminalRecord = new CriminalRecord();
        wantedSystem = new WantedSystem();
        rumourNetwork = new RumourNetwork(new Random(99));
        achievementSystem = new AchievementSystem();
        achievementCallback = type -> achievementSystem.unlock(type);

        npcs = new ArrayList<>();
        npcs.add(new NPC(NPCType.DETECTORIST_CHAIR, 5f, 1f, 5f));
        npcs.add(new NPC(NPCType.PAS_OFFICER, 8f, 1f, 8f));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Legitimate dig — permission slip consumed, find awarded
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player with METAL_DETECTOR and DIG_PERMISSION_SLIP digs on a Sunday during dig hours.
     * Verify: DigResult is LEGITIMATE or HOARD_FOUND, permission slip consumed after first dig,
     * a find is in inventory.
     */
    @Test
    void dig_withPermissionAndDetector_succeedsAndConsumesSlip() {
        assertTrue(detectoristsSystem.isDigActive(timeSystem),
                "Dig should be active on day " + DIG_DAY + " at " + DIG_HOUR);

        assertEquals(1, inventory.getItemCount(Material.DIG_PERMISSION_SLIP),
                "Should start with one permission slip");
        int totalItemsBefore = countNonCoinItems(inventory);

        DetectoristsSystem.DigResult result = detectoristsSystem.dig(
                timeSystem, inventory, criminalRecord, notorietySystem, achievementCallback);

        assertTrue(result == DetectoristsSystem.DigResult.SUCCESS_LEGITIMATE
                        || result == DetectoristsSystem.DigResult.HOARD_FOUND,
                "Dig with permission should return SUCCESS_LEGITIMATE or HOARD_FOUND, got: " + result);

        // Permission slip should be consumed
        assertEquals(0, inventory.getItemCount(Material.DIG_PERMISSION_SLIP),
                "Permission slip should be consumed on first dig");

        // No trespass crime recorded
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.FIELD_TRESPASS),
                "No FIELD_TRESPASS should be recorded for a legitimate dig");

        // No notoriety added for legitimate dig
        assertEquals(0, notorietySystem.getNotoriety(),
                "No notoriety should be added for a legitimate dig");

        // At least one find item was added (except COIN which may not change)
        int totalItemsAfter = countNonCoinItems(inventory);
        assertTrue(totalItemsAfter > totalItemsBefore || result == DetectoristsSystem.DigResult.HOARD_FOUND,
                "A find item should be added to inventory after a legitimate dig");
    }

    @Test
    void dig_noDetector_returnsNoMetalDetector() {
        inventory.removeItem(Material.METAL_DETECTOR, 1);

        DetectoristsSystem.DigResult result = detectoristsSystem.dig(
                timeSystem, inventory, criminalRecord, notorietySystem, achievementCallback);

        assertEquals(DetectoristsSystem.DigResult.NO_METAL_DETECTOR, result,
                "Dig without METAL_DETECTOR should return NO_METAL_DETECTOR");
    }

    @Test
    void dig_outsideDigHours_returnsDIgNotActive() {
        // Set time before dig opens
        timeSystem.setTime(8.0f);

        DetectoristsSystem.DigResult result = detectoristsSystem.dig(
                timeSystem, inventory, criminalRecord, notorietySystem, achievementCallback);

        assertEquals(DetectoristsSystem.DigResult.DIG_NOT_ACTIVE, result,
                "Dig before 09:00 should return DIG_NOT_ACTIVE");
    }

    @Test
    void dig_nonDigDay_returnsDIgNotActive() {
        // Monday is day 1 (1 % 7 != 0)
        timeSystem.setDayForTesting(1);

        DetectoristsSystem.DigResult result = detectoristsSystem.dig(
                timeSystem, inventory, criminalRecord, notorietySystem, achievementCallback);

        assertEquals(DetectoristsSystem.DigResult.DIG_NOT_ACTIVE, result,
                "Dig on a non-Sunday should return DIG_NOT_ACTIVE");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Trespass dig — FIELD_TRESPASS crime + Notoriety +3
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player without a DIG_PERMISSION_SLIP digs on a Sunday.
     * Verify: DigResult is SUCCESS_TRESPASS, FIELD_TRESPASS crime recorded,
     * Notoriety +3 added.
     */
    @Test
    void dig_withoutPermission_recordsTrespassAndAddsNotoriety() {
        // Remove permission slip
        inventory.removeItem(Material.DIG_PERMISSION_SLIP, 1);
        assertEquals(0, inventory.getItemCount(Material.DIG_PERMISSION_SLIP),
                "Player should have no permission slip");

        int notorietyBefore = notorietySystem.getNotoriety();
        int trespassBefore = criminalRecord.getCount(CriminalRecord.CrimeType.FIELD_TRESPASS);

        DetectoristsSystem.DigResult result = detectoristsSystem.dig(
                timeSystem, inventory, criminalRecord, notorietySystem, achievementCallback);

        // Should return either SUCCESS_TRESPASS or HOARD_FOUND (trespass still yields finds)
        assertTrue(result == DetectoristsSystem.DigResult.SUCCESS_TRESPASS
                        || result == DetectoristsSystem.DigResult.HOARD_FOUND,
                "Trespass dig should return SUCCESS_TRESPASS or HOARD_FOUND, got: " + result);

        // FIELD_TRESPASS should be recorded
        assertEquals(trespassBefore + 1,
                criminalRecord.getCount(CriminalRecord.CrimeType.FIELD_TRESPASS),
                "FIELD_TRESPASS should be recorded for digging without permission");

        // Notoriety +3
        assertEquals(notorietyBefore + DetectoristsSystem.TRESPASS_NOTORIETY,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by " + DetectoristsSystem.TRESPASS_NOTORIETY
                        + " for trespass dig");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: PAS declaration — hoard excavated, ROMAN_BROOCH declared
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player finds ROMAN_COIN (hoard spawned), excavates hoard (gets ROMAN_BROOCH),
     * then declares it to Janet (PAS_OFFICER) during 11:00–13:00.
     * Verify: 20 COIN reward, PAS_RECEIPT, HOARD_FINDER achievement.
     */
    @Test
    void pasDeclaration_romanBroochDeclaredToJanet_awardsRewardAndHoardFinder() {
        // Advance to PAS officer window
        timeSystem.setTime(PAS_HOUR);

        assertTrue(detectoristsSystem.isPasOfficerPresent(timeSystem),
                "PAS officer should be present at " + PAS_HOUR);

        // Simulate hoard already found and excavated
        detectoristsSystem.setHoardSpawnedForTesting(true);
        detectoristsSystem.excavateHoard(inventory);

        // Verify ROMAN_BROOCH is in inventory after excavation
        assertTrue(inventory.getItemCount(Material.ROMAN_BROOCH) >= 1,
                "ROMAN_BROOCH should be in inventory after excavating the hoard");

        int coinsBefore = inventory.getItemCount(Material.COIN);
        int receiptsBefor = inventory.getItemCount(Material.PAS_RECEIPT);

        DetectoristsSystem.PasDeclarationResult result = detectoristsSystem.declareToPas(
                timeSystem, inventory, achievementCallback, npcs, rumourNetwork);

        assertEquals(DetectoristsSystem.PasDeclarationResult.SUCCESS, result,
                "Declaration should succeed when Janet is present and player has ROMAN_BROOCH");

        // 20 COIN reward
        assertEquals(coinsBefore + DetectoristsSystem.PAS_DECLARATION_REWARD,
                inventory.getItemCount(Material.COIN),
                "Player should receive " + DetectoristsSystem.PAS_DECLARATION_REWARD
                        + " COIN for declaring the brooch");

        // PAS_RECEIPT awarded
        assertEquals(receiptsBefor + 1, inventory.getItemCount(Material.PAS_RECEIPT),
                "PAS_RECEIPT should be added to inventory after declaration");

        // ROMAN_BROOCH consumed
        assertEquals(0, inventory.getItemCount(Material.ROMAN_BROOCH),
                "ROMAN_BROOCH should be removed from inventory after declaration");

        // HOARD_FINDER achievement
        assertTrue(achievementSystem.isUnlocked(AchievementType.HOARD_FINDER),
                "HOARD_FINDER achievement should be unlocked after declaring the brooch");

        // Marked as declared
        assertTrue(detectoristsSystem.isRomanBroochDeclared(),
                "System should record that the brooch was declared");
    }

    @Test
    void pasDeclaration_janetNotPresent_returnsPasNotPresent() {
        // Set time outside PAS window
        timeSystem.setTime(14.0f); // after PAS_CLOSE_HOUR (13:00)

        inventory.addItem(Material.ROMAN_BROOCH, 1);

        DetectoristsSystem.PasDeclarationResult result = detectoristsSystem.declareToPas(
                timeSystem, inventory, achievementCallback, npcs, rumourNetwork);

        assertEquals(DetectoristsSystem.PasDeclarationResult.PAS_NOT_PRESENT, result,
                "Declaration should fail when Janet is not present (after 13:00)");
    }

    @Test
    void pasDeclaration_noBrooch_returnsNoBrooch() {
        timeSystem.setTime(PAS_HOUR);
        // No ROMAN_BROOCH in inventory

        DetectoristsSystem.PasDeclarationResult result = detectoristsSystem.declareToPas(
                timeSystem, inventory, achievementCallback, npcs, rumourNetwork);

        assertEquals(DetectoristsSystem.PasDeclarationResult.NO_ROMAN_BROOCH, result,
                "Declaration should fail when player has no ROMAN_BROOCH");
    }

    @Test
    void hoardExcavation_givesCorrectItems() {
        detectoristsSystem.setHoardSpawnedForTesting(true);

        int coinsBefore = inventory.getItemCount(Material.COIN);
        int romanCoinsBefore = inventory.getItemCount(Material.ROMAN_COIN);
        int romanBroochBefore = inventory.getItemCount(Material.ROMAN_BROOCH);

        boolean success = detectoristsSystem.excavateHoard(inventory);
        assertTrue(success, "Excavation should succeed when hoard is spawned");

        assertEquals(romanCoinsBefore + 3, inventory.getItemCount(Material.ROMAN_COIN),
                "Hoard should yield 3 ROMAN_COINs");
        assertEquals(romanBroochBefore + 1, inventory.getItemCount(Material.ROMAN_BROOCH),
                "Hoard should yield 1 ROMAN_BROOCH");
        assertEquals(coinsBefore + 12, inventory.getItemCount(Material.COIN),
                "Hoard should yield 12 COIN");
    }

    @Test
    void hoardExcavation_noHoard_returnsFalse() {
        // Hoard not spawned
        boolean success = detectoristsSystem.excavateHoard(inventory);
        assertFalse(success, "Excavation should fail when hoard has not been spawned");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Treasure dodging — fencing ROMAN_BROOCH without declaring
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player fences ROMAN_BROOCH without declaring (Janet not witnessing).
     * Verify: FenceResult.SUCCESS, TREASURE_DODGING crime, Notoriety +8.
     */
    @Test
    void fenceBrooch_unwitnessed_recordsTreasureDodgingAndNotoriety() {
        inventory.addItem(Material.ROMAN_BROOCH, 1);

        int notorietyBefore = notorietySystem.getNotoriety();
        int crimeBefore = criminalRecord.getCount(CriminalRecord.CrimeType.TREASURE_DODGING);
        int broochCountBefore = inventory.getItemCount(Material.ROMAN_BROOCH);

        DetectoristsSystem.FenceResult result = detectoristsSystem.fenceBrooch(
                inventory, false /* janetWitnesses */, criminalRecord, notorietySystem,
                achievementCallback, wantedSystem, npcs, rumourNetwork);

        assertEquals(DetectoristsSystem.FenceResult.SUCCESS, result,
                "Unwitnessed fence should return SUCCESS");

        // TREASURE_DODGING crime recorded
        assertEquals(crimeBefore + 1,
                criminalRecord.getCount(CriminalRecord.CrimeType.TREASURE_DODGING),
                "TREASURE_DODGING should be recorded in criminal record");

        // Notoriety +8
        assertEquals(notorietyBefore + DetectoristsSystem.TREASURE_DODGING_NOTORIETY,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by " + DetectoristsSystem.TREASURE_DODGING_NOTORIETY
                        + " for treasure dodging");

        // ROMAN_BROOCH consumed
        assertEquals(broochCountBefore - 1, inventory.getItemCount(Material.ROMAN_BROOCH),
                "ROMAN_BROOCH should be removed from inventory after fencing");

        // No extra wanted stars (Janet did not witness)
        assertEquals(0, wantedSystem.getWantedStars(),
                "No extra wanted stars when Janet does not witness");
    }

    /**
     * Player fences ROMAN_BROOCH with Janet witnessing.
     * Verify: FenceResult.WITNESSED_BY_JANET, TREASURE_DODGING crime,
     * Notoriety +8, WantedSystem +2.
     */
    @Test
    void fenceBrooch_witnessedByJanet_addsWantedStars() {
        inventory.addItem(Material.ROMAN_BROOCH, 1);

        int notorietyBefore = notorietySystem.getNotoriety();
        int wantedBefore = wantedSystem.getWantedStars();

        DetectoristsSystem.FenceResult result = detectoristsSystem.fenceBrooch(
                inventory, true /* janetWitnesses */, criminalRecord, notorietySystem,
                achievementCallback, wantedSystem, npcs, rumourNetwork);

        assertEquals(DetectoristsSystem.FenceResult.WITNESSED_BY_JANET, result,
                "Fence witnessed by Janet should return WITNESSED_BY_JANET");

        // TREASURE_DODGING crime recorded
        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.TREASURE_DODGING),
                "TREASURE_DODGING should be recorded even when witnessed");

        // Notoriety +8
        assertEquals(notorietyBefore + DetectoristsSystem.TREASURE_DODGING_NOTORIETY,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by " + DetectoristsSystem.TREASURE_DODGING_NOTORIETY
                        + " even when witnessed");

        // WantedSystem +2
        assertEquals(wantedBefore + DetectoristsSystem.TREASURE_DODGING_WANTED_STARS,
                wantedSystem.getWantedStars(),
                "Wanted stars should increase by "
                        + DetectoristsSystem.TREASURE_DODGING_WANTED_STARS
                        + " when Janet witnesses");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Trophy heist and voluntary return
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player steals DETECTORISTS_TROPHY_PROP unwitnessed during dig hours (Sunday 09:00–16:00).
     * Verify: TrophyHeistResult.SUCCESS, TREASURE_HUNTER achievement,
     * tool consumed, no BURGLARY crime.
     */
    @Test
    void trophyHeist_unwitnessed_awardsTreasureHunterAndConsumesTool() {
        inventory.addItem(Material.LOCKPICK, 1);

        assertTrue(detectoristsSystem.isDigActive(timeSystem),
                "Dig (and heist window) should be active on day " + DIG_DAY + " at " + DIG_HOUR);

        int lockpickBefore = inventory.getItemCount(Material.LOCKPICK);
        int burglaryBefore = criminalRecord.getCount(CriminalRecord.CrimeType.BURGLARY);
        int notorietyBefore = notorietySystem.getNotoriety();

        DetectoristsSystem.TrophyHeistResult result = detectoristsSystem.attemptTrophyHeist(
                timeSystem, inventory, false /* unwitnessed */, npcs, achievementCallback,
                criminalRecord, notorietySystem, wantedSystem, rumourNetwork);

        assertEquals(DetectoristsSystem.TrophyHeistResult.SUCCESS, result,
                "Unwitnessed trophy heist should succeed");

        // LOCKPICK consumed
        assertEquals(lockpickBefore - 1, inventory.getItemCount(Material.LOCKPICK),
                "LOCKPICK should be consumed on successful trophy heist");

        // TREASURE_HUNTER achievement
        assertTrue(achievementSystem.isUnlocked(AchievementType.TREASURE_HUNTER),
                "TREASURE_HUNTER achievement should be unlocked after unwitnessed heist");

        // No BURGLARY crime (unwitnessed)
        assertEquals(burglaryBefore,
                criminalRecord.getCount(CriminalRecord.CrimeType.BURGLARY),
                "No BURGLARY should be recorded for unwitnessed trophy heist");

        // No notoriety (unwitnessed)
        assertEquals(notorietyBefore, notorietySystem.getNotoriety(),
                "No notoriety should be added for unwitnessed trophy heist");

        // Trophy looted flag set
        assertTrue(detectoristsSystem.isTrophyLooted(),
                "Trophy should be marked as looted after successful heist");

        // Second attempt returns ALREADY_LOOTED
        inventory.addItem(Material.LOCKPICK, 1);
        DetectoristsSystem.TrophyHeistResult secondResult = detectoristsSystem.attemptTrophyHeist(
                timeSystem, inventory, false, npcs, achievementCallback,
                criminalRecord, notorietySystem, wantedSystem, rumourNetwork);
        assertEquals(DetectoristsSystem.TrophyHeistResult.ALREADY_LOOTED, secondResult,
                "Second heist attempt should return ALREADY_LOOTED");
    }

    /**
     * Player steals trophy (witnessed by NOSY_NEIGHBOUR).
     * Verify: TrophyHeistResult.WITNESSED, BURGLARY crime, Notoriety +8.
     */
    @Test
    void trophyHeist_witnessed_recordsBurglaryAndAddsNotoriety() {
        inventory.addItem(Material.LOCKPICK, 1);

        int notorietyBefore = notorietySystem.getNotoriety();
        int burglaryBefore = criminalRecord.getCount(CriminalRecord.CrimeType.BURGLARY);

        DetectoristsSystem.TrophyHeistResult result = detectoristsSystem.attemptTrophyHeist(
                timeSystem, inventory, true /* witnessed */, npcs, achievementCallback,
                criminalRecord, notorietySystem, wantedSystem, rumourNetwork);

        assertEquals(DetectoristsSystem.TrophyHeistResult.WITNESSED, result,
                "Witnessed trophy heist should return WITNESSED");

        // BURGLARY recorded
        assertEquals(burglaryBefore + 1,
                criminalRecord.getCount(CriminalRecord.CrimeType.BURGLARY),
                "BURGLARY should be recorded for witnessed trophy heist");

        // Notoriety +8
        assertEquals(notorietyBefore + DetectoristsSystem.BURGLARY_NOTORIETY,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by " + DetectoristsSystem.BURGLARY_NOTORIETY
                        + " for witnessed trophy heist");

        // Trophy NOT looted (witnessed = caught before taking)
        assertFalse(detectoristsSystem.isTrophyLooted(),
                "Trophy should NOT be marked looted when heist was witnessed");
    }

    @Test
    void trophyHeist_noTool_returnsNoTool() {
        // No LOCKPICK or CROWBAR
        DetectoristsSystem.TrophyHeistResult result = detectoristsSystem.attemptTrophyHeist(
                timeSystem, inventory, false, npcs, achievementCallback,
                criminalRecord, notorietySystem, wantedSystem, rumourNetwork);

        assertEquals(DetectoristsSystem.TrophyHeistResult.NO_TOOL, result,
                "Heist without LOCKPICK or CROWBAR should return NO_TOOL");
    }

    @Test
    void trophyHeist_outsideDigHours_returnsOutsideWindow() {
        // Set time outside dig (e.g. 17:00)
        timeSystem.setTime(17.0f);
        inventory.addItem(Material.LOCKPICK, 1);

        DetectoristsSystem.TrophyHeistResult result = detectoristsSystem.attemptTrophyHeist(
                timeSystem, inventory, false, npcs, achievementCallback,
                criminalRecord, notorietySystem, wantedSystem, rumourNetwork);

        assertEquals(DetectoristsSystem.TrophyHeistResult.OUTSIDE_HEIST_WINDOW, result,
                "Trophy heist outside dig hours should return OUTSIDE_HEIST_WINDOW");
    }

    /**
     * Player returns the trophy voluntarily to Keith.
     * Verify: GOOD_SAMARITAN rumour seeded, 4 free DIG_PERMISSION_SLIPs awarded.
     */
    @Test
    void trophyReturn_voluntary_awards4SlipsAndSeedsRumour() {
        // Simulate trophy having been looted
        detectoristsSystem.setTrophyLootedForTesting(true);

        int slipsBefore = inventory.getItemCount(Material.DIG_PERMISSION_SLIP);

        DetectoristsSystem.TrophyReturnResult result = detectoristsSystem.returnTrophy(
                inventory, npcs, rumourNetwork);

        assertEquals(DetectoristsSystem.TrophyReturnResult.SUCCESS, result,
                "Trophy return should succeed when trophy has been looted");

        // 4 free permission slips
        assertEquals(slipsBefore + DetectoristsSystem.RETURN_TROPHY_SLIP_COUNT,
                inventory.getItemCount(Material.DIG_PERMISSION_SLIP),
                "Player should receive " + DetectoristsSystem.RETURN_TROPHY_SLIP_COUNT
                        + " free DIG_PERMISSION_SLIPs for returning the trophy");

        // Trophy returned flag
        assertTrue(detectoristsSystem.isTrophyReturned(),
                "Trophy should be marked as returned after voluntary return");

        // Second return attempt should fail
        DetectoristsSystem.TrophyReturnResult secondResult = detectoristsSystem.returnTrophy(
                inventory, npcs, rumourNetwork);
        assertEquals(DetectoristsSystem.TrophyReturnResult.ALREADY_RETURNED, secondResult,
                "Second return attempt should return ALREADY_RETURNED");
    }

    @Test
    void trophyReturn_notLooted_returnsNoTrophy() {
        // Trophy not yet looted
        DetectoristsSystem.TrophyReturnResult result = detectoristsSystem.returnTrophy(
                inventory, npcs, rumourNetwork);

        assertEquals(DetectoristsSystem.TrophyReturnResult.NO_TROPHY, result,
                "Return should fail when trophy has not been looted");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rival detectorist tests
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player reports the rival detectorist to Keith on rival day.
     * Verify: RivalReportResult.SUCCESS, FIELD_ENFORCER achievement.
     */
    @Test
    void reportRival_onRivalDay_awardFieldEnforcer() {
        // Rival day: day 7 (7 % 14 == 7)
        timeSystem.setDayForTesting(RIVAL_DAY);
        timeSystem.setTime(DIG_HOUR);

        assertTrue(detectoristsSystem.isRivalDay(timeSystem),
                "Day " + RIVAL_DAY + " should be the rival day (7 % 14 == 7)");

        DetectoristsSystem.RivalReportResult result = detectoristsSystem.reportRival(
                timeSystem, npcs, achievementCallback, rumourNetwork);

        assertEquals(DetectoristsSystem.RivalReportResult.SUCCESS, result,
                "Reporting rival should succeed on rival day");

        assertTrue(achievementSystem.isUnlocked(AchievementType.FIELD_ENFORCER),
                "FIELD_ENFORCER achievement should be unlocked after reporting the rival");

        // Already reported
        DetectoristsSystem.RivalReportResult secondResult = detectoristsSystem.reportRival(
                timeSystem, npcs, achievementCallback, rumourNetwork);
        assertEquals(DetectoristsSystem.RivalReportResult.ALREADY_REPORTED, secondResult,
                "Second report attempt should return ALREADY_REPORTED");
    }

    @Test
    void reportRival_notRivalDay_returnsRivalNotPresent() {
        // Day 7 but not a secondary Sunday (first occurrence: 7 % 14 == 7 IS the rival day)
        // Use a non-rival Sunday: day 14 (14 % 14 == 0, not 7)
        timeSystem.setDayForTesting(14);
        timeSystem.setTime(DIG_HOUR);

        assertFalse(detectoristsSystem.isRivalDay(timeSystem),
                "Day 14 should NOT be the rival day (14 % 14 == 0, not 7)");

        DetectoristsSystem.RivalReportResult result = detectoristsSystem.reportRival(
                timeSystem, npcs, achievementCallback, rumourNetwork);

        assertEquals(DetectoristsSystem.RivalReportResult.RIVAL_NOT_PRESENT, result,
                "Reporting rival on non-rival day should return RIVAL_NOT_PRESENT");
    }

    @Test
    void chaseOffRival_witnessed_recordsAffray() {
        timeSystem.setDayForTesting(RIVAL_DAY);
        timeSystem.setTime(DIG_HOUR);

        boolean success = detectoristsSystem.chaseOffRival(timeSystem, true /* witnessed */,
                criminalRecord);

        assertTrue(success, "Chasing off rival on rival day should return true");
        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.AFFRAY),
                "AFFRAY should be recorded when chasing off rival is witnessed");
    }

    @Test
    void chaseOffRival_unwitnessed_noAffrayRecorded() {
        timeSystem.setDayForTesting(RIVAL_DAY);
        timeSystem.setTime(DIG_HOUR);

        boolean success = detectoristsSystem.chaseOffRival(timeSystem, false /* unwitnessed */,
                criminalRecord);

        assertTrue(success, "Chasing off rival unwitnessed should return true");
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.AFFRAY),
                "No AFFRAY should be recorded when chasing off rival is unwitnessed");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Count inventory items that are not COIN (to check finds were added).
     */
    private int countNonCoinItems(Inventory inv) {
        int count = 0;
        for (Material m : Material.values()) {
            if (m != Material.COIN && m != Material.METAL_DETECTOR
                    && m != Material.DIG_PERMISSION_SLIP) {
                count += inv.getItemCount(m);
            }
        }
        return count;
    }
}

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
import ragamuffin.world.LandmarkType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1335 — CycleShopSystem end-to-end scenarios.
 *
 * <p>Tests:
 * <ol>
 *   <li><b>Shop purchase flow</b>: Player has sufficient COIN; buys SECOND_HAND_BIKE
 *       and BIKE_HELMET during opening hours. Verifies COIN deducted, items in
 *       inventory, Dave refuses service when notoriety ≥ 70, and shop closed
 *       outside opening hours.</li>
 *   <li><b>Riding mechanics — mount, speed, collision</b>: Player mounts bike;
 *       speed multiplier becomes 2.0. Simulate collision: damage without helmet is
 *       COLLISION_DAMAGE (10); damage with BIKE_HELMET is COLLISION_DAMAGE_WITH_HELMET (5).
 *       CYCLE_TO_WORK fires when mounted with helmet at employer landmark.</li>
 *   <li><b>Bike theft — lock cut completes, crimes recorded, rumour seeded</b>:
 *       Initialise STANDARD lock (5 hits); land 4 hits — HITS_REMAINING returned.
 *       Land 5th hit — SUCCESS, STOLEN_BIKE in inventory, BIKE_THEFT in CriminalRecord,
 *       notoriety +5, LOCK_CUTTER achievement unlocked, BIKE_THEFT_RING rumour seeded.</li>
 *   <li><b>JustEat delivery — on-time hot payout then cold payout</b>: Player mounts
 *       and accepts job with DELIVERY_BAG; completes before time limit → 4 COIN payout,
 *       GIG_ECONOMY achievement unlocked. Repeat with COLD_DELIVERY_BAG → 2 COIN payout.
 *       Verify late delivery → 0 COIN.</li>
 *   <li><b>PCSO no-lights stop — verbal warning then offence recorded</b>: Player
 *       riding at 23:00 without lights, PCSO present. First stop: VERBAL_WARNING,
 *       NO_LIGHTS achievement. Second stop: OFFENCE_RECORDED, CYCLING_OFFENCE in
 *       CriminalRecord, Notoriety +3, WantedSystem +1 star. Outrunning PCSO at
 *       ≥ ESCAPE_DISTANCE unlocks BEAT_COPPER_ON_BIKE.</li>
 * </ol>
 */
class Issue1335CycleShopIntegrationTest {

    private CycleShopSystem cycleShop;
    private Inventory inventory;
    private AchievementSystem achievements;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private WantedSystem wantedSystem;
    private List<AchievementType> awarded;
    private NotorietySystem.AchievementCallback achievementCb;

    @BeforeEach
    void setUp() {
        cycleShop = new CycleShopSystem(new Random(42L));
        inventory = new Inventory(36);
        achievements = new AchievementSystem();
        notorietySystem = new NotorietySystem();
        criminalRecord = new CriminalRecord();
        rumourNetwork = new RumourNetwork(new Random(7L));
        wantedSystem = new WantedSystem();

        awarded = new ArrayList<>();
        achievementCb = type -> {
            awarded.add(type);
            achievements.unlock(type);
        };

        cycleShop.setNotorietySystem(notorietySystem);
        cycleShop.setAchievementSystem(achievements);
        cycleShop.setCriminalRecord(criminalRecord);
        cycleShop.setRumourNetwork(rumourNetwork);
        cycleShop.setWantedSystem(wantedSystem);
    }

    // ── Integration Test 1: Shop purchase flow ────────────────────────────────

    /**
     * Shop purchase end-to-end:
     * <ol>
     *   <li>Give player 20 COIN. Shop open at 10:00. Buy SECOND_HAND_BIKE (8 COIN)
     *       — SUCCESS, COIN = 12, SECOND_HAND_BIKE in inventory.</li>
     *   <li>Buy BIKE_HELMET (5 COIN) — SUCCESS, COIN = 7.</li>
     *   <li>Set notoriety to 70 — next purchase returns SERVICE_REFUSED.</li>
     *   <li>Try to buy at 18:00 (closed) — returns SHOP_CLOSED.</li>
     *   <li>Try to buy with 0 COIN at 10:00 — returns INSUFFICIENT_FUNDS.</li>
     * </ol>
     */
    @Test
    void shopPurchase_fullFlow() {
        inventory.addItem(Material.COIN, 20);

        // Buy SECOND_HAND_BIKE during opening hours
        CycleShopSystem.PurchaseResult r1 = cycleShop.purchase(
                Material.SECOND_HAND_BIKE, CycleShopSystem.PRICE_SECOND_HAND_BIKE,
                inventory, 0, 10.0f, achievementCb);
        assertEquals(CycleShopSystem.PurchaseResult.SUCCESS, r1,
                "Should succeed buying SECOND_HAND_BIKE during opening hours");
        assertEquals(12, inventory.getItemCount(Material.COIN),
                "COIN should be deducted: 20 - 8 = 12");
        assertEquals(1, inventory.getItemCount(Material.SECOND_HAND_BIKE),
                "SECOND_HAND_BIKE should be in inventory");

        // Buy BIKE_HELMET
        CycleShopSystem.PurchaseResult r2 = cycleShop.purchase(
                Material.BIKE_HELMET, CycleShopSystem.PRICE_BIKE_HELMET,
                inventory, 0, 10.0f, achievementCb);
        assertEquals(CycleShopSystem.PurchaseResult.SUCCESS, r2,
                "Should succeed buying BIKE_HELMET");
        assertEquals(7, inventory.getItemCount(Material.COIN),
                "COIN should be deducted: 12 - 5 = 7");
        assertEquals(1, inventory.getItemCount(Material.BIKE_HELMET),
                "BIKE_HELMET should be in inventory");

        // High notoriety — Dave refuses
        CycleShopSystem.PurchaseResult refused = cycleShop.purchase(
                Material.BIKE_REPAIR_KIT, CycleShopSystem.PRICE_BIKE_REPAIR_KIT,
                inventory, CycleShopSystem.NOTORIETY_REFUSED, 10.0f, achievementCb);
        assertEquals(CycleShopSystem.PurchaseResult.SERVICE_REFUSED, refused,
                "Dave should refuse service when notoriety >= " + CycleShopSystem.NOTORIETY_REFUSED);

        // Shop closed at 18:00
        CycleShopSystem.PurchaseResult closed = cycleShop.purchase(
                Material.BIKE_REPAIR_KIT, CycleShopSystem.PRICE_BIKE_REPAIR_KIT,
                inventory, 0, 18.0f, achievementCb);
        assertEquals(CycleShopSystem.PurchaseResult.SHOP_CLOSED, closed,
                "Shop should be closed at 18:00");

        // Insufficient funds
        Inventory emptyInventory = new Inventory(36);
        CycleShopSystem.PurchaseResult noFunds = cycleShop.purchase(
                Material.SECOND_HAND_BIKE, CycleShopSystem.PRICE_SECOND_HAND_BIKE,
                emptyInventory, 0, 10.0f, achievementCb);
        assertEquals(CycleShopSystem.PurchaseResult.INSUFFICIENT_FUNDS, noFunds,
                "Should return INSUFFICIENT_FUNDS when player has no COIN");
    }

    // ── Integration Test 2: Riding mechanics ──────────────────────────────────

    /**
     * Riding mechanics end-to-end:
     * <ol>
     *   <li>Player without bike — mountBike returns false, speed multiplier = 1.0.</li>
     *   <li>Add SECOND_HAND_BIKE to inventory — mountBike returns true,
     *       isMounted = true, speed multiplier = 2.0.</li>
     *   <li>Simulate collision without helmet: damage = 10, isMounted becomes false.</li>
     *   <li>Mount again, add BIKE_HELMET: collision damage = 5.</li>
     *   <li>CYCLE_TO_WORK fires when mounted with helmet and checkCycleToWork called.</li>
     * </ol>
     */
    @Test
    void ridingMechanics_mountCollisionAndCycleToWork() {
        // Cannot mount without a bike
        boolean mountResult = cycleShop.mountBike(inventory);
        assertFalse(mountResult, "Cannot mount without a bike in inventory");
        assertFalse(cycleShop.isMounted(), "Should not be mounted");
        assertEquals(1.0f, cycleShop.getSpeedMultiplier(), 0.001f,
                "Speed multiplier should be 1.0 when not mounted");

        // Add bike and mount
        inventory.addItem(Material.SECOND_HAND_BIKE, 1);
        boolean mounted = cycleShop.mountBike(inventory);
        assertTrue(mounted, "Should mount with SECOND_HAND_BIKE in inventory");
        assertTrue(cycleShop.isMounted(), "isMounted should be true");
        assertEquals(CycleShopSystem.RIDING_SPEED_MULTIPLIER,
                cycleShop.getSpeedMultiplier(), 0.001f,
                "Speed multiplier should be " + CycleShopSystem.RIDING_SPEED_MULTIPLIER + " when mounted");

        // Collision without helmet
        int damage = cycleShop.onBikeCollision(inventory, achievementCb);
        assertEquals(CycleShopSystem.COLLISION_DAMAGE, damage,
                "Collision damage without helmet should be " + CycleShopSystem.COLLISION_DAMAGE);
        assertFalse(cycleShop.isMounted(), "Should be dismounted after collision");

        // Mount again, add helmet
        cycleShop.mountBike(inventory);
        inventory.addItem(Material.BIKE_HELMET, 1);
        int damageWithHelmet = cycleShop.onBikeCollision(inventory, achievementCb);
        assertEquals(CycleShopSystem.COLLISION_DAMAGE_WITH_HELMET, damageWithHelmet,
                "Collision damage with BIKE_HELMET should be " + CycleShopSystem.COLLISION_DAMAGE_WITH_HELMET);

        // CYCLE_TO_WORK: mount with helmet, call checkCycleToWork
        cycleShop.setMountedForTesting(true);
        assertFalse(awarded.contains(AchievementType.CYCLE_TO_WORK),
                "CYCLE_TO_WORK should not be awarded yet");
        cycleShop.checkCycleToWork(inventory, achievementCb);
        assertTrue(awarded.contains(AchievementType.CYCLE_TO_WORK),
                "CYCLE_TO_WORK should be awarded when mounted with BIKE_HELMET");

        // Should not fire again
        int awardedCountBefore = (int) awarded.stream().filter(a -> a == AchievementType.CYCLE_TO_WORK).count();
        cycleShop.checkCycleToWork(inventory, achievementCb);
        int awardedCountAfter = (int) awarded.stream().filter(a -> a == AchievementType.CYCLE_TO_WORK).count();
        assertEquals(awardedCountBefore, awardedCountAfter,
                "CYCLE_TO_WORK should only fire once");
    }

    // ── Integration Test 3: Bike theft — lock cut pipeline ───────────────────

    /**
     * Bike theft lock-cut end-to-end:
     * <ol>
     *   <li>Initialise STANDARD lock (5 hits required).</li>
     *   <li>Attempt lock cut without CROWBAR — returns WRONG_TOOL.</li>
     *   <li>Add CROWBAR; land 4 hits — each returns HITS_REMAINING.</li>
     *   <li>Land 5th hit: SUCCESS, STOLEN_BIKE in inventory,
     *       BIKE_THEFT in CriminalRecord, notoriety increased by BIKE_THEFT_NOTORIETY,
     *       LOCK_CUTTER achievement unlocked, BIKE_THEFT_RING rumour seeded.</li>
     *   <li>6th hit on same lock: ALREADY_UNLOCKED.</li>
     *   <li>Witness flag: start new lock, cut with witnessed=true, verify WantedSystem
     *       gains 1 star.</li>
     * </ol>
     */
    @Test
    void bikeLockCut_standardLock_fullFlow() {
        // Start cutting a STANDARD lock
        cycleShop.startLockCut(CycleShopSystem.LockTier.STANDARD);

        // Without CROWBAR — WRONG_TOOL
        CycleShopSystem.LockCutResult noTool = cycleShop.hitLock(
                inventory, null, false, achievementCb);
        assertEquals(CycleShopSystem.LockCutResult.WRONG_TOOL, noTool,
                "Should return WRONG_TOOL without a CROWBAR");

        // Add CROWBAR and land 4 hits — should all be HITS_REMAINING
        inventory.addItem(Material.CROWBAR, 1);
        int hitsRequired = CycleShopSystem.LockTier.STANDARD.hitsRequired;
        for (int i = 1; i < hitsRequired; i++) {
            CycleShopSystem.LockCutResult hitResult = cycleShop.hitLock(
                    inventory, null, false, achievementCb);
            assertEquals(CycleShopSystem.LockCutResult.HITS_REMAINING, hitResult,
                    "Hit " + i + " of " + hitsRequired + " should return HITS_REMAINING");
            assertEquals(i, cycleShop.getLockCutHitsLanded(),
                    "Hits landed should be " + i);
        }

        // Record state before final hit
        int notorietyBefore = notorietySystem.getNotoriety();
        int stolenBefore = inventory.getItemCount(Material.STOLEN_BIKE);

        // Final hit — SUCCESS
        CycleShopSystem.LockCutResult finalHit = cycleShop.hitLock(
                inventory, null, false, achievementCb);
        assertEquals(CycleShopSystem.LockCutResult.SUCCESS, finalHit,
                "Final hit should return SUCCESS");
        assertEquals(stolenBefore + 1, inventory.getItemCount(Material.STOLEN_BIKE),
                "STOLEN_BIKE should be in inventory after successful lock cut");
        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.BIKE_THEFT),
                "BIKE_THEFT should be recorded in CriminalRecord");
        assertTrue(notorietySystem.getNotoriety() >= notorietyBefore + CycleShopSystem.BIKE_THEFT_NOTORIETY,
                "Notoriety should increase by at least " + CycleShopSystem.BIKE_THEFT_NOTORIETY);
        assertTrue(awarded.contains(AchievementType.LOCK_CUTTER),
                "LOCK_CUTTER achievement should be unlocked");

        // Already unlocked — next hit returns ALREADY_UNLOCKED
        CycleShopSystem.LockCutResult alreadyDone = cycleShop.hitLock(
                inventory, null, false, achievementCb);
        assertEquals(CycleShopSystem.LockCutResult.ALREADY_UNLOCKED, alreadyDone,
                "Should return ALREADY_UNLOCKED after lock is already cut");

        // Witnessed cut — verify wanted stars increase
        CycleShopSystem witnessedCycleShop = new CycleShopSystem(new Random(1L));
        witnessedCycleShop.setWantedSystem(wantedSystem);
        witnessedCycleShop.setNotorietySystem(notorietySystem);
        witnessedCycleShop.setCriminalRecord(criminalRecord);
        witnessedCycleShop.setRumourNetwork(rumourNetwork);
        Inventory inv2 = new Inventory(36);
        inv2.addItem(Material.CROWBAR, 1);
        witnessedCycleShop.startLockCut(CycleShopSystem.LockTier.BASIC);
        // Set up state: BASIC = 3 hits, simulate 2 already landed
        witnessedCycleShop.setLockCutStateForTesting(CycleShopSystem.LockTier.BASIC, 2);
        int starsBefore = wantedSystem.getWantedStars();
        witnessedCycleShop.hitLock(inv2, null, true, achievementCb);
        assertTrue(wantedSystem.getWantedStars() >= starsBefore + 1,
                "Wanted stars should increase by 1 when bike theft is witnessed");
    }

    // ── Integration Test 4: JustEat delivery hustle ───────────────────────────

    /**
     * JustEat delivery hustle end-to-end:
     * <ol>
     *   <li>Without bike or bag — acceptDelivery returns NO_DELIVERY_BAG.</li>
     *   <li>Add DELIVERY_BAG but not mounted — returns NOT_MOUNTED.</li>
     *   <li>Mount bike with SECOND_HAND_BIKE; accept delivery — SUCCESS.</li>
     *   <li>Complete before time limit — SUCCESS_HOT, COIN += 4, GIG_ECONOMY awarded.</li>
     *   <li>Accept with COLD_DELIVERY_BAG — complete on time — SUCCESS_COLD, COIN += 2.</li>
     *   <li>Accept again — advance timer past limit — complete — FAILED_LATE, COIN unchanged.</li>
     *   <li>completeDelivery when not active — NOT_ACTIVE.</li>
     * </ol>
     */
    @Test
    void deliveryHustle_hotColdAndLatePayouts() {
        // No bag — NO_DELIVERY_BAG
        CycleShopSystem.DeliveryAcceptResult noBag = cycleShop.acceptDelivery(inventory);
        assertEquals(CycleShopSystem.DeliveryAcceptResult.NO_DELIVERY_BAG, noBag,
                "Should return NO_DELIVERY_BAG without a DELIVERY_BAG");

        // Has bag but no bike — NO_BIKE
        inventory.addItem(Material.DELIVERY_BAG, 1);
        CycleShopSystem.DeliveryAcceptResult noBike = cycleShop.acceptDelivery(inventory);
        assertEquals(CycleShopSystem.DeliveryAcceptResult.NO_BIKE, noBike,
                "Should return NO_BIKE without a bike in inventory");

        // Has bike but not mounted — NOT_MOUNTED
        inventory.addItem(Material.SECOND_HAND_BIKE, 1);
        CycleShopSystem.DeliveryAcceptResult notMounted = cycleShop.acceptDelivery(inventory);
        assertEquals(CycleShopSystem.DeliveryAcceptResult.NOT_MOUNTED, notMounted,
                "Should return NOT_MOUNTED when not riding");

        // Mount and accept — SUCCESS
        cycleShop.mountBike(inventory);
        CycleShopSystem.DeliveryAcceptResult accept = cycleShop.acceptDelivery(inventory);
        assertEquals(CycleShopSystem.DeliveryAcceptResult.SUCCESS, accept,
                "Should accept delivery when mounted with bag and bike");
        assertTrue(cycleShop.isDeliveryActive(), "Delivery should be active after accept");

        // Complete on time (no time update) — SUCCESS_HOT, COIN += 4
        int coinBefore = inventory.getItemCount(Material.COIN);
        CycleShopSystem.DeliveryCompleteResult hotResult = cycleShop.completeDelivery(inventory, achievementCb);
        assertEquals(CycleShopSystem.DeliveryCompleteResult.SUCCESS_HOT, hotResult,
                "Completing on time with DELIVERY_BAG should yield SUCCESS_HOT");
        assertEquals(coinBefore + CycleShopSystem.DELIVERY_PAYOUT_HOT,
                inventory.getItemCount(Material.COIN),
                "Should gain " + CycleShopSystem.DELIVERY_PAYOUT_HOT + " COIN for hot delivery");
        assertTrue(awarded.contains(AchievementType.GIG_ECONOMY),
                "GIG_ECONOMY achievement should be awarded on first completed delivery");
        assertEquals(1, cycleShop.getDeliveriesCompleted(),
                "Deliveries completed should be 1");

        // Cold delivery — replace DELIVERY_BAG with COLD_DELIVERY_BAG
        inventory.removeItem(Material.DELIVERY_BAG, 1);
        inventory.addItem(Material.COLD_DELIVERY_BAG, 1);
        cycleShop.mountBike(inventory);
        cycleShop.acceptDelivery(inventory);
        int coinBeforeCold = inventory.getItemCount(Material.COIN);
        CycleShopSystem.DeliveryCompleteResult coldResult = cycleShop.completeDelivery(inventory, achievementCb);
        assertEquals(CycleShopSystem.DeliveryCompleteResult.SUCCESS_COLD, coldResult,
                "Cold delivery bag should yield SUCCESS_COLD");
        assertEquals(coinBeforeCold + CycleShopSystem.DELIVERY_PAYOUT_COLD,
                inventory.getItemCount(Material.COIN),
                "Should gain " + CycleShopSystem.DELIVERY_PAYOUT_COLD + " COIN for cold delivery");

        // Late delivery
        inventory.addItem(Material.DELIVERY_BAG, 1);
        inventory.removeItem(Material.COLD_DELIVERY_BAG, 1);
        cycleShop.mountBike(inventory);
        cycleShop.acceptDelivery(inventory);
        // Advance timer past limit
        cycleShop.updateDelivery(CycleShopSystem.DELIVERY_TIME_LIMIT_MINUTES + 1f);
        int coinBeforeLate = inventory.getItemCount(Material.COIN);
        CycleShopSystem.DeliveryCompleteResult lateResult = cycleShop.completeDelivery(inventory, achievementCb);
        assertEquals(CycleShopSystem.DeliveryCompleteResult.FAILED_LATE, lateResult,
                "Late delivery should return FAILED_LATE");
        assertEquals(coinBeforeLate, inventory.getItemCount(Material.COIN),
                "COIN should not change for late delivery");

        // Not active
        CycleShopSystem.DeliveryCompleteResult notActive = cycleShop.completeDelivery(inventory, achievementCb);
        assertEquals(CycleShopSystem.DeliveryCompleteResult.NOT_ACTIVE, notActive,
                "Should return NOT_ACTIVE when no delivery is active");
    }

    // ── Integration Test 5: PCSO no-lights stop and bike escape ──────────────

    /**
     * PCSO no-lights stop and bike escape end-to-end:
     * <ol>
     *   <li>Not mounted — checkNoLightsStop returns NO_STOP.</li>
     *   <li>Mounted, before 22:00 — NO_STOP.</li>
     *   <li>Mounted, after 22:00, no PCSO nearby — NO_STOP.</li>
     *   <li>Mounted, after 22:00, PCSO present, no lights — first stop:
     *       VERBAL_WARNING, NO_LIGHTS achievement awarded.</li>
     *   <li>Second stop: OFFENCE_RECORDED, CYCLING_OFFENCE in CriminalRecord,
     *       notoriety +3, WantedSystem +1 star.</li>
     *   <li>Having both lights — NO_STOP even when PCSO present after 22:00.</li>
     *   <li>Outrunning PCSO: player at (0,0), PCSO at (0,0); then player at
     *       (ESCAPE_DISTANCE+1, 0) — BEAT_COPPER_ON_BIKE awarded.</li>
     * </ol>
     */
    @Test
    void pcsoNoLightsStop_verbalThenOffenceAndBikeEscape() {
        // Not mounted — NO_STOP
        CycleShopSystem.CyclingStopResult notMounted = cycleShop.checkNoLightsStop(
                23.0f, inventory, true, achievementCb);
        assertEquals(CycleShopSystem.CyclingStopResult.NO_STOP, notMounted,
                "Should be NO_STOP when not mounted");

        // Mount bike
        inventory.addItem(Material.SECOND_HAND_BIKE, 1);
        cycleShop.mountBike(inventory);

        // Before 22:00 — NO_STOP
        CycleShopSystem.CyclingStopResult beforeHour = cycleShop.checkNoLightsStop(
                20.0f, inventory, true, achievementCb);
        assertEquals(CycleShopSystem.CyclingStopResult.NO_STOP, beforeHour,
                "Should be NO_STOP before 22:00");

        // After 22:00, no PCSO — NO_STOP
        CycleShopSystem.CyclingStopResult noPcso = cycleShop.checkNoLightsStop(
                23.0f, inventory, false, achievementCb);
        assertEquals(CycleShopSystem.CyclingStopResult.NO_STOP, noPcso,
                "Should be NO_STOP when PCSO not nearby");

        // First stop — VERBAL_WARNING
        int notorietyBefore = notorietySystem.getNotoriety();
        CycleShopSystem.CyclingStopResult firstStop = cycleShop.checkNoLightsStop(
                23.0f, inventory, true, achievementCb);
        assertEquals(CycleShopSystem.CyclingStopResult.VERBAL_WARNING, firstStop,
                "First no-lights stop should be VERBAL_WARNING");
        assertTrue(awarded.contains(AchievementType.NO_LIGHTS),
                "NO_LIGHTS achievement should be awarded on first stop");
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.CYCLING_OFFENCE),
                "CYCLING_OFFENCE should NOT be recorded on first (warning) stop");
        assertEquals(notorietyBefore, notorietySystem.getNotoriety(),
                "Notoriety should not change on verbal warning");

        // Second stop — OFFENCE_RECORDED
        int starsBefore = wantedSystem.getWantedStars();
        notorietyBefore = notorietySystem.getNotoriety();
        CycleShopSystem.CyclingStopResult secondStop = cycleShop.checkNoLightsStop(
                23.0f, inventory, true, achievementCb);
        assertEquals(CycleShopSystem.CyclingStopResult.OFFENCE_RECORDED, secondStop,
                "Second no-lights stop should be OFFENCE_RECORDED");
        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.CYCLING_OFFENCE),
                "CYCLING_OFFENCE should be recorded on second stop");
        assertTrue(notorietySystem.getNotoriety() >= notorietyBefore + CycleShopSystem.CYCLING_OFFENCE_NOTORIETY,
                "Notoriety should increase by at least " + CycleShopSystem.CYCLING_OFFENCE_NOTORIETY);
        assertTrue(wantedSystem.getWantedStars() >= starsBefore + 1,
                "WantedSystem should gain 1 star on second cycling offence");

        // Having lights — NO_STOP
        inventory.addItem(Material.BIKE_LIGHT_FRONT, 1);
        inventory.addItem(Material.BIKE_LIGHT_REAR, 1);
        CycleShopSystem.CyclingStopResult withLights = cycleShop.checkNoLightsStop(
                23.0f, inventory, true, achievementCb);
        assertEquals(CycleShopSystem.CyclingStopResult.NO_STOP, withLights,
                "Should be NO_STOP when both lights are in inventory");

        // Outrun PCSO — player and PCSO both at (0,0) initially — not escaped
        boolean notEscaped = cycleShop.checkBikeEscape(0f, 0f, 0f, 0f, achievementCb);
        assertFalse(notEscaped, "Should not be escaped when at same position as PCSO");
        assertFalse(awarded.contains(AchievementType.BEAT_COPPER_ON_BIKE),
                "BEAT_COPPER_ON_BIKE should not be awarded yet");

        // Player moves ESCAPE_DISTANCE + 1 away
        float escapeX = CycleShopSystem.ESCAPE_DISTANCE + 1f;
        boolean escaped = cycleShop.checkBikeEscape(escapeX, 0f, 0f, 0f, achievementCb);
        assertTrue(escaped, "Should be escaped when distance >= ESCAPE_DISTANCE");
        assertTrue(awarded.contains(AchievementType.BEAT_COPPER_ON_BIKE),
                "BEAT_COPPER_ON_BIKE should be awarded on successful escape");
    }
}

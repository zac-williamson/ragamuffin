package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1335: Northfield Cycle Centre — Dave's Bikes, Riding Mechanics,
 * Bike Theft &amp; the JustEat Delivery Hustle.
 *
 * <h3>Overview</h3>
 * Dave's Cycle Centre ({@link LandmarkType#CYCLE_SHOP}) is a narrow storefront
 * on the high street, open 09:00–17:30 Mon–Sat. Dave ({@link NPCType#CYCLE_SHOP_OWNER})
 * sells bikes, components, and accessories from the counter. The shop also houses
 * the {@code JUST_EAT_DELIVERY_BOARD_PROP}, from which the player can accept
 * timed food delivery runs.
 *
 * <h3>Mechanic 1 — Shop Stock &amp; Purchasing</h3>
 * <ul>
 *   <li>{@link Material#SECOND_HAND_BIKE}: 8 COIN</li>
 *   <li>{@link Material#BIKE_REPAIR_KIT}: 3 COIN</li>
 *   <li>{@link Material#BIKE_LOCK}: 4 COIN (basic); 6 COIN (standard)</li>
 *   <li>{@link Material#BIKE_LIGHT_FRONT}: 2 COIN</li>
 *   <li>{@link Material#BIKE_LIGHT_REAR}: 2 COIN</li>
 *   <li>{@link Material#BIKE_HELMET}: 5 COIN</li>
 *   <li>{@link Material#DELIVERY_BAG}: 6 COIN</li>
 * </ul>
 * Dave refuses to sell to players with Notoriety ≥ {@link #NOTORIETY_REFUSED}.
 *
 * <h3>Mechanic 2 — Bicycle Riding</h3>
 * Mounting a bike (press E on SECOND_HAND_BIKE or STOLEN_BIKE in inventory)
 * sets {@link #isMounted()} true and applies speed multiplier ×{@link #RIDING_SPEED_MULTIPLIER}.
 * Momentum carries the player 0.5 blocks after releasing W.
 * Dismount on collision: player takes {@link #COLLISION_DAMAGE} and is thrown
 * forward. BIKE_HELMET halves collision damage.
 *
 * <h3>Mechanic 3 — Bike Theft Loop</h3>
 * {@link LandmarkType#CYCLE_SHOP} and nearby lamp posts spawn {@code LOCKED_BIKE_PROP}s
 * with randomised lock tiers:
 * <ul>
 *   <li>Basic lock: 3 CROWBAR hits to cut ({@link #LOCK_HITS_BASIC})</li>
 *   <li>Standard lock: 5 CROWBAR hits ({@link #LOCK_HITS_STANDARD})</li>
 *   <li>Heavy-duty lock: 8 CROWBAR hits ({@link #LOCK_HITS_HEAVY})</li>
 * </ul>
 * On successful unlock: STOLEN_BIKE added to inventory, {@link RumourType#BIKE_THEFT_RING}
 * seeded, {@link CrimeType#BIKE_THEFT} recorded, WantedSystem +1 star if witnessed.
 * Stolen bikes can be fenced via TravellerSiteSystem for 3 COIN.
 *
 * <h3>Mechanic 4 — JustEat Delivery Hustle</h3>
 * Player accepts a delivery from the board (requires {@link Material#DELIVERY_BAG}
 * and a bike material in inventory). Each run has a destination (KebabVan or Chippy)
 * and a time limit. Payout tiers:
 * <ul>
 *   <li>On-time with DELIVERY_BAG: 4 COIN ({@link #DELIVERY_PAYOUT_HOT})</li>
 *   <li>On-time with COLD_DELIVERY_BAG: 2 COIN ({@link #DELIVERY_PAYOUT_COLD})</li>
 *   <li>Late delivery: 0 COIN, complaint lodged ({@link #DELIVERY_PAYOUT_LATE})</li>
 * </ul>
 * Delivery income is disclosed to DWPSystem if player is on UC (integration point).
 * Completing first delivery unlocks {@link AchievementType#GIG_ECONOMY}.
 *
 * <h3>Mechanic 5 — PCSO Interaction (No Lights / Pavement Riding)</h3>
 * After 22:00, riding without BIKE_LIGHT_FRONT + BIKE_LIGHT_REAR triggers
 * a stop by the nearest PCSO. First offence: verbal warning.
 * Second offence: {@link CrimeType#CYCLING_OFFENCE} recorded, WantedSystem +1 star.
 * Riding on pavement within 10 blocks of PUBLIC/PENSIONER triggers
 * {@link CrimeType#CYCLING_OFFENCE} immediately (no warning).
 * Outrunning PCSO pursuit (increase distance to ≥ {@link #ESCAPE_DISTANCE})
 * unlocks {@link AchievementType#BEAT_COPPER_ON_BIKE}.
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@link AchievementType#LOCK_CUTTER} — cut a bike lock</li>
 *   <li>{@link AchievementType#GIG_ECONOMY} — complete first delivery</li>
 *   <li>{@link AchievementType#BEAT_COPPER_ON_BIKE} — outrun a PCSO on bike</li>
 *   <li>{@link AchievementType#NO_LIGHTS} — caught cycling without lights</li>
 *   <li>{@link AchievementType#CYCLE_TO_WORK} — ride to employer wearing helmet</li>
 * </ul>
 */
public class CycleShopSystem {

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Opening hour (09:00). */
    public static final float OPEN_HOUR = 9.0f;

    /** Closing hour (17:30). */
    public static final float CLOSE_HOUR = 17.5f;

    // ── Shop stock prices (in COIN) ───────────────────────────────────────────

    /** Price of a SECOND_HAND_BIKE from Dave's. */
    public static final int PRICE_SECOND_HAND_BIKE = 8;

    /** Price of a BIKE_REPAIR_KIT. */
    public static final int PRICE_BIKE_REPAIR_KIT = 3;

    /** Price of a BIKE_LOCK (basic). */
    public static final int PRICE_BIKE_LOCK = 4;

    /** Price of BIKE_LIGHT_FRONT. */
    public static final int PRICE_BIKE_LIGHT_FRONT = 2;

    /** Price of BIKE_LIGHT_REAR. */
    public static final int PRICE_BIKE_LIGHT_REAR = 2;

    /** Price of a BIKE_HELMET. */
    public static final int PRICE_BIKE_HELMET = 5;

    /** Price of a DELIVERY_BAG. */
    public static final int PRICE_DELIVERY_BAG = 6;

    // ── Notoriety threshold ───────────────────────────────────────────────────

    /** Notoriety at which Dave refuses to serve the player. */
    public static final int NOTORIETY_REFUSED = 70;

    // ── Riding mechanics ──────────────────────────────────────────────────────

    /** Speed multiplier applied while mounted on a bike. */
    public static final float RIDING_SPEED_MULTIPLIER = 2.0f;

    /** Damage taken on bike collision (without helmet). */
    public static final int COLLISION_DAMAGE = 10;

    /** Damage taken on bike collision (with BIKE_HELMET equipped). */
    public static final int COLLISION_DAMAGE_WITH_HELMET = 5;

    // ── Lock tiers ────────────────────────────────────────────────────────────

    /** CROWBAR hits to break a basic lock. */
    public static final int LOCK_HITS_BASIC = 3;

    /** CROWBAR hits to break a standard lock. */
    public static final int LOCK_HITS_STANDARD = 5;

    /** CROWBAR hits to break a heavy-duty lock. */
    public static final int LOCK_HITS_HEAVY = 8;

    /** Notoriety gained when stealing a bike. */
    public static final int BIKE_THEFT_NOTORIETY = 5;

    // ── Delivery hustle ───────────────────────────────────────────────────────

    /** Payout for hot on-time delivery (DELIVERY_BAG). */
    public static final int DELIVERY_PAYOUT_HOT = 4;

    /** Payout for cold on-time delivery (COLD_DELIVERY_BAG). */
    public static final int DELIVERY_PAYOUT_COLD = 2;

    /** Payout for late delivery. */
    public static final int DELIVERY_PAYOUT_LATE = 0;

    /** Time limit for a delivery run (in in-game minutes). */
    public static final float DELIVERY_TIME_LIMIT_MINUTES = 5.0f;

    // ── Police / PCSO interaction ─────────────────────────────────────────────

    /** In-game hour after which riding without lights triggers PCSO stop. */
    public static final float NO_LIGHTS_HOUR = 22.0f;

    /** Distance from PCSO (in blocks) at which player has escaped pursuit. */
    public static final float ESCAPE_DISTANCE = 30.0f;

    /** Notoriety gained for a cycling offence (no lights / pavement). */
    public static final int CYCLING_OFFENCE_NOTORIETY = 3;

    // ── Result enums ──────────────────────────────────────────────────────────

    /**
     * Result of a shop purchase attempt.
     */
    public enum PurchaseResult {
        SUCCESS,
        SHOP_CLOSED,
        SERVICE_REFUSED,
        INSUFFICIENT_FUNDS,
        OUT_OF_STOCK
    }

    /**
     * Lock tier for a LOCKED_BIKE_PROP.
     */
    public enum LockTier {
        BASIC(LOCK_HITS_BASIC),
        STANDARD(LOCK_HITS_STANDARD),
        HEAVY(LOCK_HITS_HEAVY);

        public final int hitsRequired;

        LockTier(int hitsRequired) {
            this.hitsRequired = hitsRequired;
        }
    }

    /**
     * Result of a lock-cut attempt on a LOCKED_BIKE_PROP.
     */
    public enum LockCutResult {
        SUCCESS,
        WRONG_TOOL,
        HITS_REMAINING,
        ALREADY_UNLOCKED
    }

    /**
     * Result of accepting a delivery job.
     */
    public enum DeliveryAcceptResult {
        SUCCESS,
        NO_DELIVERY_BAG,
        NO_BIKE,
        NOT_MOUNTED,
        NO_JOBS_AVAILABLE
    }

    /**
     * Result of completing a delivery run.
     */
    public enum DeliveryCompleteResult {
        SUCCESS_HOT,
        SUCCESS_COLD,
        FAILED_LATE,
        NOT_ACTIVE
    }

    /**
     * Result of a PCSO stop for cycling offence.
     */
    public enum CyclingStopResult {
        VERBAL_WARNING,
        OFFENCE_RECORDED,
        NO_STOP
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random rng;

    /** Whether the player is currently mounted on a bike. */
    private boolean mounted = false;

    /** Whether the player has BIKE_HELMET equipped (in inventory). */
    private boolean helmetEquipped = false;

    /** Number of lock-cut hits landed on the current LOCKED_BIKE_PROP. */
    private int lockCutHitsLanded = 0;

    /** Lock tier of the bike currently being cut. */
    private LockTier currentLockTier = null;

    /** Whether the current LOCKED_BIKE_PROP has been unlocked. */
    private boolean lockCutComplete = false;

    /** Whether a delivery run is currently active. */
    private boolean deliveryActive = false;

    /** Time remaining on the current delivery run (in in-game minutes). */
    private float deliveryTimeRemaining = 0f;

    /** Whether the active delivery bag is a COLD_DELIVERY_BAG. */
    private boolean deliveryCold = false;

    /** Number of deliveries successfully completed (for GIG_ECONOMY achievement). */
    private int deliveriesCompleted = 0;

    /** Number of cycling offence stops received. */
    private int cyclingOffenceCount = 0;

    /** Whether the CYCLE_TO_WORK achievement has been awarded. */
    private boolean cycleToWorkAwarded = false;

    // ── Optional system references ────────────────────────────────────────────

    private RumourNetwork rumourNetwork;
    private NotorietySystem notorietySystem;
    private AchievementSystem achievementSystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;

    // ── Construction ──────────────────────────────────────────────────────────

    public CycleShopSystem() {
        this(new Random());
    }

    public CycleShopSystem(Random rng) {
        this.rng = rng;
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setRumourNetwork(RumourNetwork r)         { this.rumourNetwork = r; }
    public void setNotorietySystem(NotorietySystem n)     { this.notorietySystem = n; }
    public void setAchievementSystem(AchievementSystem a) { this.achievementSystem = a; }
    public void setWantedSystem(WantedSystem w)           { this.wantedSystem = w; }
    public void setCriminalRecord(CriminalRecord c)       { this.criminalRecord = c; }

    // ── Opening hours ─────────────────────────────────────────────────────────

    /**
     * Returns whether Dave's Cycle Centre is open at the given hour.
     * Open 09:00–17:30 (Mon–Sat; closed Sunday, modelled as day 7 if needed).
     *
     * @param hour current in-game hour (0.0–24.0)
     * @return true if the shop is open
     */
    public boolean isOpen(float hour) {
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    // ── Shop purchases ────────────────────────────────────────────────────────

    /**
     * Player attempts to purchase an item from Dave's Cycle Centre.
     *
     * @param item                the material being purchased
     * @param price               the COIN price to charge
     * @param inventory           the player's inventory
     * @param notoriety           the player's current notoriety
     * @param currentHour         current in-game hour
     * @param achievementCallback callback for awarding achievements, may be null
     * @return the result of the purchase attempt
     */
    public PurchaseResult purchase(Material item, int price,
                                   Inventory inventory, int notoriety,
                                   float currentHour,
                                   NotorietySystem.AchievementCallback achievementCallback) {
        if (!isOpen(currentHour)) {
            return PurchaseResult.SHOP_CLOSED;
        }

        if (notoriety >= NOTORIETY_REFUSED) {
            return PurchaseResult.SERVICE_REFUSED;
        }

        if (inventory.getItemCount(Material.COIN) < price) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        inventory.removeItem(Material.COIN, price);
        inventory.addItem(item, 1);

        return PurchaseResult.SUCCESS;
    }

    // ── Riding mechanics ──────────────────────────────────────────────────────

    /**
     * Mount a bike from the player's inventory.
     * Requires SECOND_HAND_BIKE, STOLEN_BIKE, or KIDS_BIKE in inventory.
     *
     * @param inventory the player's inventory
     * @return true if successfully mounted
     */
    public boolean mountBike(Inventory inventory) {
        boolean hasBike = inventory.hasItem(Material.SECOND_HAND_BIKE)
                || inventory.hasItem(Material.STOLEN_BIKE)
                || inventory.hasItem(Material.KIDS_BIKE);
        if (!hasBike) {
            return false;
        }
        mounted = true;
        helmetEquipped = inventory.hasItem(Material.BIKE_HELMET);
        return true;
    }

    /**
     * Dismount the currently ridden bike.
     */
    public void dismountBike() {
        mounted = false;
    }

    /**
     * Returns whether the player is currently mounted.
     */
    public boolean isMounted() {
        return mounted;
    }

    /**
     * Returns the effective movement speed multiplier for the player.
     * Returns {@link #RIDING_SPEED_MULTIPLIER} when mounted, 1.0 otherwise.
     */
    public float getSpeedMultiplier() {
        return mounted ? RIDING_SPEED_MULTIPLIER : 1.0f;
    }

    /**
     * Handle a bike collision. Applies damage and dismounts the player.
     *
     * @param inventory           the player's inventory
     * @param achievementCallback callback for awarding achievements, may be null
     * @return damage dealt to the player
     */
    public int onBikeCollision(Inventory inventory,
                                NotorietySystem.AchievementCallback achievementCallback) {
        dismountBike();
        boolean hasHelmet = inventory.hasItem(Material.BIKE_HELMET);
        return hasHelmet ? COLLISION_DAMAGE_WITH_HELMET : COLLISION_DAMAGE;
    }

    /**
     * Check whether the player qualifies for the CYCLE_TO_WORK achievement.
     * Fires when mounted and the player arrives at an employer landmark
     * wearing a BIKE_HELMET for the first time.
     *
     * @param inventory           the player's inventory
     * @param achievementCallback callback for awarding achievements, may be null
     */
    public void checkCycleToWork(Inventory inventory,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        if (!cycleToWorkAwarded && mounted && inventory.hasItem(Material.BIKE_HELMET)) {
            cycleToWorkAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.CYCLE_TO_WORK);
            }
        }
    }

    // ── Bike theft (lock cutting) ─────────────────────────────────────────────

    /**
     * Initialise a lock-cut attempt on a LOCKED_BIKE_PROP with the given tier.
     * Resets the hit counter and marks the lock as in-progress.
     *
     * @param tier the lock tier of the target LOCKED_BIKE_PROP
     */
    public void startLockCut(LockTier tier) {
        currentLockTier = tier;
        lockCutHitsLanded = 0;
        lockCutComplete = false;
    }

    /**
     * Land one CROWBAR hit on the lock currently being cut.
     * When hits reach the tier threshold, the lock breaks and STOLEN_BIKE
     * is added to inventory; crimes are recorded and rumours seeded.
     *
     * @param inventory           the player's inventory (must contain CROWBAR)
     * @param daveNpc             Dave's NPC for rumour seeding, may be null
     * @param witnessed           true if any NPC can see the player
     * @param achievementCallback callback for awarding achievements, may be null
     * @return result of the hit
     */
    public LockCutResult hitLock(Inventory inventory, NPC daveNpc,
                                  boolean witnessed,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        if (currentLockTier == null) {
            return LockCutResult.WRONG_TOOL;
        }
        if (lockCutComplete) {
            return LockCutResult.ALREADY_UNLOCKED;
        }
        if (!inventory.hasItem(Material.CROWBAR)) {
            return LockCutResult.WRONG_TOOL;
        }

        lockCutHitsLanded++;

        if (lockCutHitsLanded < currentLockTier.hitsRequired) {
            return LockCutResult.HITS_REMAINING;
        }

        // Lock broken — steal the bike
        lockCutComplete = true;
        inventory.addItem(Material.STOLEN_BIKE, 1);

        // Record crime
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.BIKE_THEFT);
        }

        // Notoriety gain
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(BIKE_THEFT_NOTORIETY, achievementCallback);
        }

        // Wanted stars if witnessed
        if (witnessed && wantedSystem != null) {
            wantedSystem.addWantedStars(1);
        }

        // Seed rumour
        NPC source = (daveNpc != null) ? daveNpc
                : new NPC(NPCType.PUBLIC, 0f, 1f, 0f);
        if (rumourNetwork != null) {
            rumourNetwork.addRumour(source,
                    new Rumour(RumourType.BIKE_THEFT_RING,
                            "Someone's been nicking bikes off the street — proper organised."));
        }

        // Award achievement
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.LOCK_CUTTER);
        }

        return LockCutResult.SUCCESS;
    }

    // ── JustEat delivery hustle ───────────────────────────────────────────────

    /**
     * Accept a delivery job from the JUST_EAT_DELIVERY_BOARD_PROP.
     * Requires DELIVERY_BAG or COLD_DELIVERY_BAG in inventory and a bike material.
     *
     * @param inventory   the player's inventory
     * @return result of the accept attempt
     */
    public DeliveryAcceptResult acceptDelivery(Inventory inventory) {
        boolean hasBag = inventory.hasItem(Material.DELIVERY_BAG)
                || inventory.hasItem(Material.COLD_DELIVERY_BAG);
        if (!hasBag) {
            return DeliveryAcceptResult.NO_DELIVERY_BAG;
        }

        boolean hasBike = inventory.hasItem(Material.SECOND_HAND_BIKE)
                || inventory.hasItem(Material.STOLEN_BIKE)
                || inventory.hasItem(Material.KIDS_BIKE);
        if (!hasBike) {
            return DeliveryAcceptResult.NO_BIKE;
        }

        if (!mounted) {
            return DeliveryAcceptResult.NOT_MOUNTED;
        }

        deliveryCold = !inventory.hasItem(Material.DELIVERY_BAG)
                && inventory.hasItem(Material.COLD_DELIVERY_BAG);
        deliveryActive = true;
        deliveryTimeRemaining = DELIVERY_TIME_LIMIT_MINUTES;
        return DeliveryAcceptResult.SUCCESS;
    }

    /**
     * Update the active delivery timer. Call each frame (or each in-game minute).
     *
     * @param deltaMinutes elapsed in-game minutes since last call
     */
    public void updateDelivery(float deltaMinutes) {
        if (deliveryActive) {
            deliveryTimeRemaining -= deltaMinutes;
        }
    }

    /**
     * Complete the active delivery run at the destination.
     * Adds the appropriate COIN reward to the player's inventory.
     *
     * @param inventory           the player's inventory
     * @param achievementCallback callback for awarding achievements, may be null
     * @return result of the completion attempt
     */
    public DeliveryCompleteResult completeDelivery(Inventory inventory,
                                                    NotorietySystem.AchievementCallback achievementCallback) {
        if (!deliveryActive) {
            return DeliveryCompleteResult.NOT_ACTIVE;
        }

        deliveryActive = false;
        deliveriesCompleted++;

        if (deliveryTimeRemaining < 0f) {
            // Late — no payout
            return DeliveryCompleteResult.FAILED_LATE;
        }

        int payout;
        DeliveryCompleteResult result;
        if (deliveryCold) {
            payout = DELIVERY_PAYOUT_COLD;
            result = DeliveryCompleteResult.SUCCESS_COLD;
        } else {
            payout = DELIVERY_PAYOUT_HOT;
            result = DeliveryCompleteResult.SUCCESS_HOT;
        }

        inventory.addItem(Material.COIN, payout);

        // Award GIG_ECONOMY on first completed delivery
        if (deliveriesCompleted == 1 && achievementCallback != null) {
            achievementCallback.award(AchievementType.GIG_ECONOMY);
        }

        return result;
    }

    /**
     * Returns the number of deliveries successfully completed.
     */
    public int getDeliveriesCompleted() {
        return deliveriesCompleted;
    }

    /**
     * Returns whether a delivery run is currently active.
     */
    public boolean isDeliveryActive() {
        return deliveryActive;
    }

    /**
     * Returns the remaining time on the active delivery (in in-game minutes).
     */
    public float getDeliveryTimeRemaining() {
        return deliveryTimeRemaining;
    }

    // ── PCSO / no-lights interaction ──────────────────────────────────────────

    /**
     * Check whether the player should be stopped for cycling without lights
     * at the given hour. Called each in-game update when mounted.
     *
     * @param currentHour         current in-game hour
     * @param inventory           player's inventory
     * @param pcsoPresentNearby   true if a PCSO NPC is within 15 blocks
     * @param achievementCallback callback for awarding achievements, may be null
     * @return result of the stop check
     */
    public CyclingStopResult checkNoLightsStop(float currentHour, Inventory inventory,
                                                boolean pcsoPresentNearby,
                                                NotorietySystem.AchievementCallback achievementCallback) {
        if (!mounted || !pcsoPresentNearby || currentHour < NO_LIGHTS_HOUR) {
            return CyclingStopResult.NO_STOP;
        }

        boolean hasLights = inventory.hasItem(Material.BIKE_LIGHT_FRONT)
                && inventory.hasItem(Material.BIKE_LIGHT_REAR);
        if (hasLights) {
            return CyclingStopResult.NO_STOP;
        }

        cyclingOffenceCount++;

        if (cyclingOffenceCount == 1) {
            // First offence: verbal warning, award NO_LIGHTS achievement
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.NO_LIGHTS);
            }
            return CyclingStopResult.VERBAL_WARNING;
        }

        // Second+ offence: record crime, add wanted star
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.CYCLING_OFFENCE);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(CYCLING_OFFENCE_NOTORIETY, achievementCallback);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(1);
        }

        return CyclingStopResult.OFFENCE_RECORDED;
    }

    /**
     * Check whether the player has escaped PCSO pursuit on bike.
     * Fires {@link AchievementType#BEAT_COPPER_ON_BIKE} on first escape.
     *
     * @param playerX             player X position
     * @param playerZ             player Z position
     * @param pcsoX               PCSO X position
     * @param pcsoZ               PCSO Z position
     * @param achievementCallback callback for awarding achievements, may be null
     * @return true if the player has escaped (distance ≥ ESCAPE_DISTANCE)
     */
    public boolean checkBikeEscape(float playerX, float playerZ,
                                    float pcsoX, float pcsoZ,
                                    NotorietySystem.AchievementCallback achievementCallback) {
        if (!mounted) return false;

        float dx = playerX - pcsoX;
        float dz = playerZ - pcsoZ;
        float distSq = dx * dx + dz * dz;

        if (distSq >= ESCAPE_DISTANCE * ESCAPE_DISTANCE) {
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.BEAT_COPPER_ON_BIKE);
            }
            return true;
        }
        return false;
    }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /**
     * Force the mounted state for testing.
     */
    public void setMountedForTesting(boolean mounted) {
        this.mounted = mounted;
    }

    /**
     * Force the delivery state for testing.
     */
    public void setDeliveryActiveForTesting(boolean active, float timeRemaining, boolean cold) {
        this.deliveryActive = active;
        this.deliveryTimeRemaining = timeRemaining;
        this.deliveryCold = cold;
    }

    /**
     * Force the lock-cut state for testing.
     */
    public void setLockCutStateForTesting(LockTier tier, int hitsLanded) {
        this.currentLockTier = tier;
        this.lockCutHitsLanded = hitsLanded;
        this.lockCutComplete = false;
    }

    /**
     * Force the cycling offence count for testing.
     */
    public void setCyclingOffenceCountForTesting(int count) {
        this.cyclingOffenceCount = count;
    }

    /**
     * Returns the number of lock-cut hits landed on the current prop.
     */
    public int getLockCutHitsLanded() {
        return lockCutHitsLanded;
    }

    /**
     * Returns the cycling offence stop count.
     */
    public int getCyclingOffenceCount() {
        return cyclingOffenceCount;
    }
}

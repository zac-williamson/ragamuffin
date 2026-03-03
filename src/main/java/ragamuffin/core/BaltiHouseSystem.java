package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1451: Northfield Balti House — The Raj Mahal, Bashir's Lock-In &amp; the Catering Tin Heist.
 *
 * <h3>Mechanic 1 — Dine In</h3>
 * <ul>
 *   <li>Press E on {@code BALTI_HOUSE_COUNTER_PROP} during Mon–Sat 17:00–23:00.</li>
 *   <li>Menu: Lamb Balti (8 COIN), Chicken Tikka Masala (7 COIN), Vegetable Balti (5 COIN),
 *       Naan (2 COIN), Curry &amp; Rice (4 COIN).</li>
 *   <li>Eating restores Hunger +20, Warmth +10.</li>
 *   <li>Curry Club Thursday: all mains −2 COIN; first visit awards {@link AchievementType#CURRY_CLUB}.</li>
 * </ul>
 *
 * <h3>Mechanic 2 — Lock-In (Fri/Sat 23:00–01:00)</h3>
 * <ul>
 *   <li>Bashir locks the front door ({@code BALTI_HOUSE_DOOR_PROP}, HARD 8 hits to break).</li>
 *   <li>Drinks served during lock-in.</li>
 *   <li>3+ drinks triggers {@code CURRY_LOCK_IN_BRAWL} event.</li>
 *   <li>{@link RumourType#BALTI_LOCK_IN} rumour seeded Friday 22:00.</li>
 *   <li>Surviving until 01:00 awards {@link AchievementType#LOCK_IN_LEGEND}.</li>
 * </ul>
 *
 * <h3>Mechanic 3 — Takeaway Hatch</h3>
 * <ul>
 *   <li>{@code BALTI_HOUSE_HATCH_PROP} on the alley side. Order, wait 3 in-game minutes,
 *       collect {@link Material#BALTI_BOX}.</li>
 *   <li>Peak-hours queue (18:00–20:00): jumping queue has {@link #QUEUE_JUMP_DETECTION_CHANCE}
 *       detection chance → Notoriety +1; 60% success.</li>
 * </ul>
 *
 * <h3>Mechanic 4 — Catering Tin Heist</h3>
 * <ul>
 *   <li>Kitchen accessible during closed hours (23:00–17:00) via {@code BALTI_BACK_DOOR_PROP}
 *       (HARD 8 hits or lockpick 4s).</li>
 *   <li>{@link Material#BALTI_CATERING_TIN} on shelf: fence value 6 COIN, pawn 4 COIN.</li>
 *   <li>If Bashir has line-of-sight: {@link CrimeType#RESTAURANT_THEFT} crime, Notoriety +3,
 *       Bashir chases and calls police after 10s.</li>
 *   <li>Undetected theft awards {@link AchievementType#SECRET_MASALA}.</li>
 * </ul>
 */
public class BaltiHouseSystem {

    // ── Opening hours ───────────────────────────────────────────────────────

    /** Hour The Raj Mahal opens (17:00). */
    public static final float OPEN_HOUR = 17.0f;

    /** Hour The Raj Mahal closes (23:00). */
    public static final float CLOSE_HOUR = 23.0f;

    /** Hour the lock-in begins on Fri/Sat nights. */
    public static final float LOCK_IN_START_HOUR = 23.0f;

    /** Hour the lock-in ends on Fri/Sat nights. */
    public static final float LOCK_IN_END_HOUR = 1.0f;

    /** Hour to seed the BALTI_LOCK_IN rumour on Fridays. */
    public static final float LOCK_IN_RUMOUR_HOUR = 22.0f;

    /** Peak takeaway hours start (18:00). */
    public static final float PEAK_HOURS_START = 18.0f;

    /** Peak takeaway hours end (20:00). */
    public static final float PEAK_HOURS_END = 20.0f;

    /** Time in in-game minutes to prepare a takeaway order. */
    public static final float TAKEAWAY_WAIT_MINUTES = 3.0f;

    // ── Menu prices ─────────────────────────────────────────────────────────

    /** Price of Lamb Balti (COIN). */
    public static final int PRICE_LAMB_BALTI = 8;

    /** Price of Chicken Tikka Masala (COIN). */
    public static final int PRICE_CHICKEN_TIKKA_MASALA = 7;

    /** Price of Vegetable Balti (COIN). */
    public static final int PRICE_VEGETABLE_BALTI = 5;

    /** Price of Naan Bread (COIN). */
    public static final int PRICE_NAAN = 2;

    /** Price of Curry &amp; Rice (COIN). */
    public static final int PRICE_CURRY_AND_RICE = 4;

    /** Curry Club Thursday discount applied to all mains (COIN). */
    public static final int CURRY_CLUB_DISCOUNT = 2;

    // ── Nourishment values ──────────────────────────────────────────────────

    /** Hunger restored by eating any dish. */
    public static final int HUNGER_RESTORE = 20;

    /** Warmth restored by eating any dish. */
    public static final int WARMTH_RESTORE = 10;

    // ── Queue-jump ──────────────────────────────────────────────────────────

    /** Detection probability (0–1) when jumping the takeaway queue during peak hours. */
    public static final float QUEUE_JUMP_DETECTION_CHANCE = 0.40f;

    /** Notoriety added when queue-jump is detected. */
    public static final int QUEUE_JUMP_NOTORIETY = 1;

    // ── Catering tin heist ──────────────────────────────────────────────────

    /** Fence value (COIN) for the BALTI_CATERING_TIN. */
    public static final int CATERING_TIN_FENCE_VALUE = 6;

    /** Pawn shop value (COIN) for the BALTI_CATERING_TIN. */
    public static final int CATERING_TIN_PAWN_VALUE = 4;

    /** Notoriety added if Bashir catches the player stealing. */
    public static final int RESTAURANT_THEFT_NOTORIETY = 3;

    /** In-game seconds before Bashir calls the police when he has line-of-sight on the thief. */
    public static final float POLICE_CALL_DELAY_SECONDS = 10.0f;

    // ── Lock-in ─────────────────────────────────────────────────────────────

    /** Number of drinks to trigger the lock-in brawl event. */
    public static final int BRAWL_DRINK_THRESHOLD = 3;

    // ── Day-of-week helpers (dayCount % 7) ──────────────────────────────────

    /** Value of {@code dayCount % 7} that corresponds to Thursday. */
    public static final int DAY_THURSDAY = 4;

    /** Value of {@code dayCount % 7} that corresponds to Friday. */
    public static final int DAY_FRIDAY = 5;

    /** Value of {@code dayCount % 7} that corresponds to Saturday (Sunday = 0, Mon=1…). */
    public static final int DAY_SATURDAY = 6;

    // ── Speech lines ─────────────────────────────────────────────────────────

    public static final String BASHIR_GREETING       = "Evening. What can I get you?";
    public static final String BASHIR_CLOSED         = "Sorry, we're closed. Come back after five.";
    public static final String BASHIR_NO_COIN        = "You haven't got enough, mate. Prices are on the board.";
    public static final String BASHIR_ORDER_OK       = "Coming right up. Take a seat, won't be long.";
    public static final String BASHIR_CURRY_CLUB     = "Thursday special — two coin off everything. You're a regular now.";
    public static final String BASHIR_LOCK_IN_INVITE = "Right, door's going on the latch. You staying?";
    public static final String BASHIR_LOCK_IN_DRINK  = "On the house. Don't tell anyone.";
    public static final String BASHIR_BRAWL          = "Oi! Not in here! Take it outside!";
    public static final String BASHIR_HATCH_ORDERED  = "Three minutes. I'll shout you when it's ready.";
    public static final String BASHIR_HATCH_READY    = "There you go, mate. Enjoy.";
    public static final String BASHIR_QUEUE_CAUGHT   = "Oi, back of the queue! I saw that.";
    public static final String BASHIR_CAUGHT_THIEF   = "Oi! What are you doing in my kitchen?! GET OUT!";

    // ── State ────────────────────────────────────────────────────────────────

    private final Random random;

    /** Bashir NPC (null if not yet spawned). */
    private NPC bashir = null;

    /** Whether the first Curry Club visit achievement has been awarded. */
    private boolean curryClubbedFirstVisit = false;

    /** Whether the player is currently in a lock-in session. */
    private boolean lockInActive = false;

    /** Number of drinks consumed during the current lock-in. */
    private int lockInDrinkCount = 0;

    /** Whether the lock-in legend achievement has been awarded this lock-in. */
    private boolean lockInLegendAwarded = false;

    /** Whether a takeaway order is pending. */
    private boolean takeawayOrderPending = false;

    /** In-game time (hours) when the takeaway order was placed. */
    private float takeawayOrderTime = 0f;

    /** Whether the BALTI_LOCK_IN rumour has been seeded this Friday. */
    private boolean lockInRumourSeededThisWeek = false;

    /** Day count when the lock-in rumour was last seeded. */
    private int lastLockInRumourDay = -1;

    // ── Optional system references ────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;

    // ── Construction ─────────────────────────────────────────────────────────

    public BaltiHouseSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection setters ──────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    // ── NPC management ────────────────────────────────────────────────────────

    /** Force-spawn Bashir for testing purposes. */
    public void forceSpawnBashir() {
        bashir = new NPC(NPCType.CURRY_HOUSE_OWNER, 0f, 0f, 0f);
        bashir.setName("Bashir");
    }

    /** Returns the Bashir NPC (may be null if not spawned). */
    public NPC getBashir() {
        return bashir;
    }

    // ── Opening hours logic ───────────────────────────────────────────────────

    /**
     * Returns true if The Raj Mahal is open for dine-in service.
     * Open Mon–Sat 17:00–23:00; closed Sundays.
     *
     * @param hour       current in-game hour (0–24)
     * @param dayOfWeek  computed as {@code timeSystem.getDayCount() % 7} where 0=Sunday
     */
    public boolean isOpen(float hour, int dayOfWeek) {
        if (dayOfWeek == 0) return false; // Closed Sundays
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Returns true if it's currently Curry Club Thursday.
     *
     * @param dayOfWeek  computed as {@code timeSystem.getDayCount() % 7}
     */
    public boolean isCurryClubThursday(int dayOfWeek) {
        return dayOfWeek == DAY_THURSDAY;
    }

    /**
     * Returns true if the lock-in is currently active (Fri/Sat 23:00–01:00).
     *
     * @param hour       current in-game hour
     * @param dayOfWeek  computed as {@code timeSystem.getDayCount() % 7}
     */
    public boolean isLockInTime(float hour, int dayOfWeek) {
        boolean isFriOrSat = (dayOfWeek == DAY_FRIDAY || dayOfWeek == DAY_SATURDAY);
        // Lock-in: 23:00 onwards OR before 01:00 (wraps past midnight)
        boolean isAfterClose = hour >= LOCK_IN_START_HOUR;
        boolean isEarlyMorning = hour < LOCK_IN_END_HOUR;
        return isFriOrSat && (isAfterClose || isEarlyMorning);
    }

    /**
     * Returns true if peak takeaway hours are active (18:00–20:00).
     *
     * @param hour current in-game hour
     */
    public boolean isPeakHours(float hour) {
        return hour >= PEAK_HOURS_START && hour < PEAK_HOURS_END;
    }

    // ── Mechanic 1: Dine In ───────────────────────────────────────────────────

    /**
     * Menu item identifiers for the dine-in counter.
     */
    public enum MenuItem {
        LAMB_BALTI,
        CHICKEN_TIKKA_MASALA,
        VEGETABLE_BALTI,
        NAAN,
        CURRY_AND_RICE
    }

    /**
     * Result of ordering food at the counter.
     */
    public enum OrderResult {
        /** Food ordered and paid for successfully. */
        SUCCESS,
        /** The restaurant is closed. */
        CLOSED,
        /** Player does not have enough COIN. */
        NO_COIN
    }

    /**
     * Returns the price of a menu item, accounting for the Curry Club Thursday discount.
     *
     * @param item       the menu item
     * @param dayOfWeek  computed as {@code timeSystem.getDayCount() % 7}
     */
    public int getPrice(MenuItem item, int dayOfWeek) {
        int basePrice;
        boolean isMain;
        switch (item) {
            case LAMB_BALTI:           basePrice = PRICE_LAMB_BALTI; isMain = true; break;
            case CHICKEN_TIKKA_MASALA: basePrice = PRICE_CHICKEN_TIKKA_MASALA; isMain = true; break;
            case VEGETABLE_BALTI:      basePrice = PRICE_VEGETABLE_BALTI; isMain = true; break;
            case CURRY_AND_RICE:       basePrice = PRICE_CURRY_AND_RICE; isMain = true; break;
            case NAAN:                 basePrice = PRICE_NAAN; isMain = false; break;
            default:                   basePrice = PRICE_CURRY_AND_RICE; isMain = true; break;
        }
        if (isMain && isCurryClubThursday(dayOfWeek)) {
            basePrice = Math.max(0, basePrice - CURRY_CLUB_DISCOUNT);
        }
        return basePrice;
    }

    /**
     * Player presses E on the BALTI_HOUSE_COUNTER_PROP to order a meal.
     *
     * @param inventory  player's inventory
     * @param item       the menu item chosen
     * @param hour       current in-game hour
     * @param dayOfWeek  computed as {@code timeSystem.getDayCount() % 7}
     */
    public OrderResult orderDineIn(Inventory inventory, MenuItem item, float hour, int dayOfWeek) {
        if (!isOpen(hour, dayOfWeek)) {
            return OrderResult.CLOSED;
        }

        int price = getPrice(item, dayOfWeek);
        if (inventory.getItemCount(Material.COIN) < price) {
            return OrderResult.NO_COIN;
        }

        inventory.removeItem(Material.COIN, price);

        // Add the food item
        Material food = menuItemToMaterial(item);
        inventory.addItem(food, 1);

        // Award Curry Club achievement on first Thursday visit (mains only)
        boolean isMain = (item != MenuItem.NAAN);
        if (isMain && isCurryClubThursday(dayOfWeek) && !curryClubbedFirstVisit) {
            curryClubbedFirstVisit = true;
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.CURRY_CLUB);
            }
        }

        return OrderResult.SUCCESS;
    }

    private Material menuItemToMaterial(MenuItem item) {
        switch (item) {
            case LAMB_BALTI:           return Material.LAMB_BALTI;
            case CHICKEN_TIKKA_MASALA: return Material.CHICKEN_TIKKA_MASALA;
            case VEGETABLE_BALTI:      return Material.VEGETABLE_BALTI;
            case NAAN:                 return Material.NAAN_BREAD;
            case CURRY_AND_RICE:       return Material.CURRY_CLUB_SPECIAL;
            default:                   return Material.CURRY_CLUB_SPECIAL;
        }
    }

    // ── Mechanic 2: Lock-In ───────────────────────────────────────────────────

    /**
     * Result of requesting a lock-in drink.
     */
    public enum LockInDrinkResult {
        /** Drink received. */
        SUCCESS,
        /** Lock-in is not currently active. */
        NOT_LOCK_IN,
        /** Brawl has been triggered (3+ drinks). */
        BRAWL_TRIGGERED
    }

    /**
     * Seed the BALTI_LOCK_IN rumour on Friday at 22:00. Should be called from
     * the per-frame update.
     *
     * @param hour       current in-game hour
     * @param dayOfWeek  day of week (dayCount % 7)
     * @param currentDay the current day count
     * @param npcs       active NPCs for rumour seeding
     */
    public void updateLockInRumour(float hour, int dayOfWeek, int currentDay, List<NPC> npcs) {
        if (dayOfWeek == DAY_FRIDAY
                && hour >= LOCK_IN_RUMOUR_HOUR
                && currentDay != lastLockInRumourDay
                && rumourNetwork != null
                && npcs != null
                && !npcs.isEmpty()) {
            NPC seedTarget = findAnyNPC(npcs);
            if (seedTarget != null) {
                rumourNetwork.addRumour(seedTarget,
                        new Rumour(RumourType.BALTI_LOCK_IN,
                                "Bashir's doing a lock-in tonight — if you know, you know."));
                lastLockInRumourDay = currentDay;
            }
        }
    }

    /**
     * Start the lock-in session.
     *
     * @param hour       current in-game hour
     * @param dayOfWeek  day of week
     */
    public void startLockIn(float hour, int dayOfWeek) {
        if (isLockInTime(hour, dayOfWeek) && !lockInActive) {
            lockInActive = true;
            lockInDrinkCount = 0;
            lockInLegendAwarded = false;
        }
    }

    /**
     * Player requests a lock-in drink from Bashir.
     *
     * @param inventory player's inventory
     */
    public LockInDrinkResult requestLockInDrink(Inventory inventory) {
        if (!lockInActive) {
            return LockInDrinkResult.NOT_LOCK_IN;
        }

        lockInDrinkCount++;
        // Drinks are on the house during lock-in
        inventory.addItem(Material.CAN_OF_LAGER, 1);

        if (lockInDrinkCount >= BRAWL_DRINK_THRESHOLD) {
            return LockInDrinkResult.BRAWL_TRIGGERED;
        }

        return LockInDrinkResult.SUCCESS;
    }

    /**
     * End the lock-in at 01:00. Awards LOCK_IN_LEGEND if the player survived.
     *
     * @param playerPresent whether the player is still inside
     */
    public void endLockIn(boolean playerPresent) {
        if (lockInActive) {
            lockInActive = false;
            if (playerPresent && !lockInLegendAwarded && achievementSystem != null) {
                lockInLegendAwarded = true;
                achievementSystem.unlock(AchievementType.LOCK_IN_LEGEND);
            }
        }
    }

    // ── Mechanic 3: Takeaway Hatch ────────────────────────────────────────────

    /**
     * Result of attempting to order from the takeaway hatch.
     */
    public enum TakeawayOrderResult {
        /** Order placed. Collect BALTI_BOX after 3 in-game minutes. */
        ORDER_PLACED,
        /** An order is already pending. */
        ORDER_ALREADY_PENDING,
        /** Player has no COIN. */
        NO_COIN,
        /** Queue jump detected — Notoriety +1, but order proceeds. */
        QUEUE_JUMPED_DETECTED,
        /** Queue jump undetected — order proceeds silently. */
        QUEUE_JUMPED_UNDETECTED
    }

    /**
     * Price for a BALTI_BOX from the hatch.
     */
    public static final int PRICE_BALTI_BOX = 4;

    /**
     * Player orders from the takeaway hatch.
     *
     * @param inventory      player's inventory
     * @param hour           current in-game hour
     * @param currentTimeHours absolute game time in hours (for 3-min wait tracking)
     * @param jumpingQueue   whether the player is jumping the queue
     * @param npcs           nearby NPCs for rumour seeding
     */
    public TakeawayOrderResult orderFromHatch(
            Inventory inventory,
            float hour,
            float currentTimeHours,
            boolean jumpingQueue,
            List<NPC> npcs) {

        if (takeawayOrderPending) {
            return TakeawayOrderResult.ORDER_ALREADY_PENDING;
        }
        if (inventory.getItemCount(Material.COIN) < PRICE_BALTI_BOX) {
            return TakeawayOrderResult.NO_COIN;
        }

        inventory.removeItem(Material.COIN, PRICE_BALTI_BOX);
        takeawayOrderPending = true;
        takeawayOrderTime = currentTimeHours;

        if (jumpingQueue && isPeakHours(hour)) {
            if (random.nextFloat() < QUEUE_JUMP_DETECTION_CHANCE) {
                // Detected
                if (notorietySystem != null) {
                    notorietySystem.addNotoriety(QUEUE_JUMP_NOTORIETY, null);
                }
                if (rumourNetwork != null && npcs != null && !npcs.isEmpty()) {
                    NPC witness = findAnyNPC(npcs);
                    if (witness != null) {
                        rumourNetwork.addRumour(witness,
                                new Rumour(RumourType.QUEUE_JUMPER,
                                        "Someone jumped the queue at the Raj Mahal hatch — Bashir noticed."));
                    }
                }
                return TakeawayOrderResult.QUEUE_JUMPED_DETECTED;
            } else {
                return TakeawayOrderResult.QUEUE_JUMPED_UNDETECTED;
            }
        }

        return TakeawayOrderResult.ORDER_PLACED;
    }

    /**
     * Result of attempting to collect a pending takeaway order.
     */
    public enum CollectOrderResult {
        /** BALTI_BOX added to inventory. */
        COLLECTED,
        /** No order is pending. */
        NO_ORDER,
        /** Order not ready yet (less than 3 in-game minutes elapsed). */
        NOT_READY
    }

    /**
     * Player attempts to collect their pending BALTI_BOX.
     *
     * @param inventory        player's inventory
     * @param currentTimeHours absolute game time in hours
     */
    public CollectOrderResult collectTakeaway(Inventory inventory, float currentTimeHours) {
        if (!takeawayOrderPending) {
            return CollectOrderResult.NO_ORDER;
        }
        float elapsedMinutes = (currentTimeHours - takeawayOrderTime) * 60f;
        if (elapsedMinutes < TAKEAWAY_WAIT_MINUTES) {
            return CollectOrderResult.NOT_READY;
        }
        takeawayOrderPending = false;
        inventory.addItem(Material.BALTI_BOX, 1);
        return CollectOrderResult.COLLECTED;
    }

    // ── Mechanic 4: Catering Tin Heist ────────────────────────────────────────

    /**
     * Result of stealing the BALTI_CATERING_TIN from the kitchen.
     */
    public enum CateringTinHeistResult {
        /** Tin stolen undetected. {@link AchievementType#SECRET_MASALA} awarded. */
        STOLEN_UNDETECTED,
        /** Bashir caught the player — {@link CrimeType#RESTAURANT_THEFT} recorded, chase triggered. */
        CAUGHT_BY_BASHIR,
        /** Kitchen is not accessible right now (open hours). */
        KITCHEN_INACCESSIBLE
    }

    /**
     * Returns true if the kitchen is accessible (during closed hours: 23:00–17:00).
     *
     * @param hour current in-game hour
     */
    public boolean isKitchenAccessible(float hour) {
        // Closed hours: 23:00 onwards OR before 17:00
        return hour >= CLOSE_HOUR || hour < OPEN_HOUR;
    }

    /**
     * Player attempts to steal the BALTI_CATERING_TIN from the kitchen.
     *
     * @param inventory        player's inventory
     * @param hour             current in-game hour
     * @param bashirHasLoS     whether Bashir has line-of-sight on the player
     * @param npcs             active NPCs for rumour seeding
     */
    public CateringTinHeistResult stealCateringTin(
            Inventory inventory,
            float hour,
            boolean bashirHasLoS,
            List<NPC> npcs) {

        if (!isKitchenAccessible(hour)) {
            return CateringTinHeistResult.KITCHEN_INACCESSIBLE;
        }

        if (bashirHasLoS) {
            // Caught — crime, notoriety, chase
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.RESTAURANT_THEFT);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(RESTAURANT_THEFT_NOTORIETY, null);
            }
            if (bashir != null) {
                bashir.setState(NPCState.CHASING);
            }
            // Seed caught-thief rumour
            if (rumourNetwork != null && npcs != null && !npcs.isEmpty()) {
                NPC seed = findAnyNPC(npcs);
                if (seed != null) {
                    rumourNetwork.addRumour(seed,
                            new Rumour(RumourType.BASHIR_CAUGHT_THIEF,
                                    "Bashir caught someone nicking a catering tin from the kitchen. Chased them down the alley."));
                }
            }
            return CateringTinHeistResult.CAUGHT_BY_BASHIR;
        }

        // Undetected theft
        inventory.addItem(Material.BALTI_CATERING_TIN, 1);

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.SECRET_MASALA);
        }

        // Seed unsolved theft rumour
        if (rumourNetwork != null && npcs != null && !npcs.isEmpty()) {
            NPC seed = findAnyNPC(npcs);
            if (seed != null) {
                rumourNetwork.addRumour(seed,
                        new Rumour(RumourType.UNSOLVED_THEFT,
                                "Someone got into the Raj Mahal kitchen last night — catering tin's gone. Bashir's fuming."));
            }
        }

        return CateringTinHeistResult.STOLEN_UNDETECTED;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update. Handles lock-in rumour seeding and lock-in lifecycle.
     *
     * @param delta      time since last frame (seconds)
     * @param timeSystem the current time system
     * @param npcs       active NPCs for rumour seeding
     */
    public void update(float delta, TimeSystem timeSystem, List<NPC> npcs) {
        float hour = timeSystem.getTime();
        int dayOfWeek = timeSystem.getDayCount() % 7;
        int currentDay = timeSystem.getDayCount();

        // Seed lock-in rumour on Fridays at 22:00
        updateLockInRumour(hour, dayOfWeek, currentDay, npcs);

        // Auto-start lock-in
        if (!lockInActive && isLockInTime(hour, dayOfWeek)) {
            startLockIn(hour, dayOfWeek);
        }

        // Auto-end lock-in when time has moved past 01:00
        if (lockInActive && !isLockInTime(hour, dayOfWeek)) {
            endLockIn(true); // in update context assume player present
        }
    }

    // ── Utility helpers ───────────────────────────────────────────────────────

    private NPC findAnyNPC(List<NPC> npcs) {
        if (npcs == null || npcs.isEmpty()) return null;
        for (NPC npc : npcs) {
            if (npc != null) return npc;
        }
        return null;
    }

    // ── Accessors for testing ─────────────────────────────────────────────────

    /** Returns whether the Curry Club first-visit achievement has been awarded. */
    public boolean isCurryClubbedFirstVisit() {
        return curryClubbedFirstVisit;
    }

    /** Force-set curry club first visit flag for testing. */
    public void setCurryClubbedFirstVisitForTesting(boolean value) {
        this.curryClubbedFirstVisit = value;
    }

    /** Returns whether a lock-in is currently active. */
    public boolean isLockInActive() {
        return lockInActive;
    }

    /** Force-set lock-in active for testing. */
    public void setLockInActiveForTesting(boolean active) {
        this.lockInActive = active;
        if (active) {
            this.lockInDrinkCount = 0;
        }
    }

    /** Returns the number of drinks consumed in the current lock-in. */
    public int getLockInDrinkCount() {
        return lockInDrinkCount;
    }

    /** Returns whether a takeaway order is currently pending. */
    public boolean isTakeawayOrderPending() {
        return takeawayOrderPending;
    }

    /** Returns the day on which the lock-in rumour was last seeded. */
    public int getLastLockInRumourDay() {
        return lastLockInRumourDay;
    }

    /** Force-set last lock-in rumour day for testing. */
    public void setLastLockInRumourDayForTesting(int day) {
        this.lastLockInRumourDay = day;
    }
}

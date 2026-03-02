package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Issue #1179: Tony's Chip Shop — Late-Night Queue, Biscuit the Cat &amp; the Frying-Pan Heist.
 *
 * <p>Tony's Chip Shop ({@code LandmarkType.CHIPPY}) is open 11:00–00:00 daily.
 * Tony ({@code NPCType.CHIPPY_OWNER}) stands behind the {@code PropType.CHIPPY_COUNTER}
 * and serves food via {@link ragamuffin.ui.ChippyOrderUI}.
 *
 * <h3>Opening Hours</h3>
 * <ul>
 *   <li>Normal: 11:00–00:00</li>
 *   <li>FROST: delayed open to 11:30</li>
 * </ul>
 *
 * <h3>Fish Supper Availability</h3>
 * Available 2 out of every 3 days: {@code dayNumber % 3 != 0}.
 *
 * <h3>CHIP_BUTTY prerequisite</h3>
 * Requires {@code Material.BREAD} in the player's inventory.
 *
 * <h3>Queue System</h3>
 * <ul>
 *   <li>1–4 NPCs normally; 3–8 during post-pub rush 23:00–00:00</li>
 *   <li>Queue advances every 90 seconds</li>
 *   <li>Drunk NPCs from {@link PubLockInSystem} auto-join</li>
 *   <li>Player must be at front; queue-jumping seeds {@link RumourType#QUEUE_JUMP}
 *       and unlocks {@link AchievementType#QUEUE_JUMPER}</li>
 * </ul>
 *
 * <h3>Last Orders</h3>
 * Served 23:50–00:00 → unlocks {@link AchievementType#LAST_ORDERS_CHIPPY}.
 *
 * <h3>CHIPPY_REGULAR</h3>
 * 5 separate purchase days unlocks {@link AchievementType#CHIPPY_REGULAR}.
 *
 * <h3>Biscuit the Cat ({@code NPCType.STRAY_CAT})</h3>
 * <ul>
 *   <li>Flees on punch → Tony goes {@code NPCState.AGGRESSIVE}, seeds {@link RumourType#CAT_PUNCH}</li>
 *   <li>FRIENDLY if fed {@code CHIPS} or {@code FISH_SUPPER} → {@link AchievementType#CAT_FEEDER} at 3 feeds</li>
 * </ul>
 *
 * <h3>Frying-Pan Heist</h3>
 * 11:00 opening window when Tony is in back room. Player steals 1–2 items.
 * Detection: 35% base + per-queue-NPC modifier − disguise modifier.
 *
 * <h3>Weather Modifiers</h3>
 * <ul>
 *   <li>FROST: delayed open to 11:30</li>
 *   <li>RAIN: thins queue (minimum 1 NPC)</li>
 *   <li>HEATWAVE: all prices +1 COIN</li>
 * </ul>
 *
 * <h3>Service Refusal</h3>
 * Notoriety ≥ 60 → Tony refuses service.
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@link AchievementType#LAST_ORDERS_CHIPPY} — served 23:50–00:00</li>
 *   <li>{@link AchievementType#QUEUE_JUMPER} — jumped the queue</li>
 *   <li>{@link AchievementType#CHIPPY_REGULAR} — 5 separate purchase days</li>
 *   <li>{@link AchievementType#CAT_FEEDER} — fed Biscuit 3 times</li>
 * </ul>
 */
public class ChippySystem {

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Normal opening hour (11:00). */
    public static final float OPEN_HOUR = 11.0f;

    /** Delayed opening hour during FROST (11:30). */
    public static final float FROST_OPEN_HOUR = 11.5f;

    /** Closing hour (midnight = 0.0 the next day, stored as 24.0 for comparison). */
    public static final float CLOSE_HOUR = 24.0f;

    // ── Queue constants ───────────────────────────────────────────────────────

    /** Normal minimum queue size. */
    public static final int QUEUE_MIN_NORMAL = 1;

    /** Normal maximum queue size. */
    public static final int QUEUE_MAX_NORMAL = 4;

    /** Post-pub rush minimum queue size (23:00–00:00). */
    public static final int QUEUE_MIN_POSTPUB = 3;

    /** Post-pub rush maximum queue size (23:00–00:00). */
    public static final int QUEUE_MAX_POSTPUB = 8;

    /** Seconds between queue advances. */
    public static final float QUEUE_ADVANCE_INTERVAL = 90.0f;

    /** In-game hour at which post-pub rush begins. */
    public static final float POST_PUB_RUSH_HOUR = 23.0f;

    // ── Last orders window ────────────────────────────────────────────────────

    /** Hour at which last-orders window begins (23:50 = 23 + 50/60). */
    public static final float LAST_ORDERS_START = 23.0f + 50.0f / 60.0f;

    // ── Notoriety threshold ───────────────────────────────────────────────────

    /** Notoriety level at which Tony refuses service. */
    public static final int SERVICE_REFUSED_NOTORIETY = 60;

    // ── Frying-pan heist ─────────────────────────────────────────────────────

    /** Base detection probability for the frying-pan heist (0–1). */
    public static final float HEIST_BASE_DETECTION = 0.35f;

    /** Detection modifier added per queue NPC present during heist. */
    public static final float HEIST_DETECTION_PER_QUEUE_NPC = 0.05f;

    /** Minimum items stolen in a successful heist. */
    public static final int HEIST_LOOT_MIN = 1;

    /** Maximum items stolen in a successful heist. */
    public static final int HEIST_LOOT_MAX = 2;

    /** In-game hour during which the heist window is open (Tony in back room). */
    public static final float HEIST_WINDOW_HOUR = 11.0f;

    // ── Weather ───────────────────────────────────────────────────────────────

    /** Price surcharge during HEATWAVE (in COIN). */
    public static final int HEATWAVE_PRICE_SURCHARGE = 1;

    // ── Notoriety for punching Biscuit ────────────────────────────────────────

    /** Notoriety gained for punching Biscuit the cat. */
    public static final int BISCUIT_PUNCH_NOTORIETY = 3;

    // ── Result enums ──────────────────────────────────────────────────────────

    /**
     * Result of an order attempt via the CHIPPY_COUNTER.
     */
    public enum OrderResult {
        SUCCESS,
        SHOP_CLOSED,
        SERVICE_REFUSED,
        INSUFFICIENT_FUNDS,
        ITEM_UNAVAILABLE,
        MISSING_BREAD,
        QUEUE_JUMP_REFUSED
    }

    /**
     * Result of a frying-pan heist attempt.
     */
    public enum HeistResult {
        SUCCESS,
        DETECTED,
        WINDOW_NOT_OPEN,
        ALREADY_LOOTED
    }

    /**
     * Result of feeding Biscuit the cat.
     */
    public enum FeedBiscuitResult {
        SUCCESS,
        WRONG_FOOD,
        BISCUIT_NOT_PRESENT
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random rng;

    /** Queue of NPC types waiting outside (index 0 = front of queue). */
    private final List<NPCType> queue = new ArrayList<>();

    /** Timer counting seconds until next queue advance. */
    private float queueAdvanceTimer = QUEUE_ADVANCE_INTERVAL;

    /** Days on which the player has made at least one purchase (for CHIPPY_REGULAR). */
    private final Set<Integer> purchaseDays = new HashSet<>();

    /** Whether the frying-pan heist has been completed this session. */
    private boolean heistLooted = false;

    /** Number of times Biscuit has been fed (for CAT_FEEDER achievement). */
    private int biscuitFeedCount = 0;

    /** Whether the shop is currently open. */
    private boolean shopOpen = false;

    // ── Optional system references ────────────────────────────────────────────

    private RumourNetwork rumourNetwork;
    private NotorietySystem notorietySystem;
    private AchievementSystem achievementSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public ChippySystem() {
        this(new Random());
    }

    public ChippySystem(Random rng) {
        this.rng = rng;
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setRumourNetwork(RumourNetwork r)         { this.rumourNetwork = r; }
    public void setNotorietySystem(NotorietySystem n)     { this.notorietySystem = n; }
    public void setAchievementSystem(AchievementSystem a) { this.achievementSystem = a; }

    // ── Opening hours ─────────────────────────────────────────────────────────

    /**
     * Returns whether Tony's Chip Shop is open at the given hour and weather.
     *
     * @param hour    current in-game hour (0.0–24.0)
     * @param weather current weather condition
     * @return true if the shop is open
     */
    public boolean isOpen(float hour, Weather weather) {
        float openHour = (weather == Weather.FROST) ? FROST_OPEN_HOUR : OPEN_HOUR;
        // Open from openHour until midnight (24.0)
        return hour >= openHour && hour < CLOSE_HOUR;
    }

    /**
     * Returns whether the hour is within the post-pub rush window (23:00–00:00).
     *
     * @param hour current in-game hour
     * @return true if post-pub rush is active
     */
    public boolean isPostPubRush(float hour) {
        return hour >= POST_PUB_RUSH_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Returns whether the hour is within the last-orders window (23:50–00:00).
     *
     * @param hour current in-game hour
     * @return true if within last-orders window
     */
    public boolean isLastOrders(float hour) {
        return hour >= LAST_ORDERS_START && hour < CLOSE_HOUR;
    }

    /**
     * Returns whether Fish Supper is available today.
     * Available when {@code dayNumber % 3 != 0}.
     *
     * @param dayNumber in-game day number (1-based)
     * @return true if fish supper is on the menu
     */
    public boolean isFishSupperAvailable(int dayNumber) {
        return dayNumber % 3 != 0;
    }

    // ── Queue management ──────────────────────────────────────────────────────

    /**
     * Initialise the outdoor queue based on hour and weather.
     * Called when the shop opens.
     *
     * @param hour    current in-game hour
     * @param weather current weather condition
     */
    public void initQueue(float hour, Weather weather) {
        queue.clear();
        int min, max;
        if (isPostPubRush(hour)) {
            min = QUEUE_MIN_POSTPUB;
            max = QUEUE_MAX_POSTPUB;
        } else {
            min = QUEUE_MIN_NORMAL;
            max = QUEUE_MAX_NORMAL;
        }
        // RAIN thins the queue: halve the range, minimum 1
        if (weather == Weather.RAIN) {
            min = 1;
            max = Math.max(1, max / 2);
        }
        int size = min + rng.nextInt(max - min + 1);
        for (int i = 0; i < size; i++) {
            queue.add(NPCType.PUBLIC);
        }
    }

    /**
     * Add a drunk NPC from the pub lock-in to the front of the queue
     * (they push through). Used by {@link PubLockInSystem} integration.
     */
    public void joinQueueDrunk() {
        queue.add(0, NPCType.DRUNK);
    }

    /**
     * Returns an unmodifiable view of the current queue.
     */
    public List<NPCType> getQueue() {
        return java.util.Collections.unmodifiableList(queue);
    }

    /**
     * Returns the current queue size.
     */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * Update queue advance timer. Called each frame.
     *
     * @param delta seconds since last frame
     */
    public void updateQueue(float delta) {
        if (queue.isEmpty()) return;
        queueAdvanceTimer -= delta;
        if (queueAdvanceTimer <= 0f) {
            // Advance: remove front NPC (served)
            queue.remove(0);
            queueAdvanceTimer = QUEUE_ADVANCE_INTERVAL;
        }
    }

    /**
     * Returns whether the player is at the front of the queue.
     * The player is considered at the front when the queue is empty
     * (no one in front) or when they are next in line.
     *
     * @return true if the player can be served without queue-jumping
     */
    public boolean isPlayerAtFront() {
        return queue.isEmpty();
    }

    // ── Ordering ──────────────────────────────────────────────────────────────

    /**
     * Player places an order at the CHIPPY_COUNTER.
     *
     * @param material    the menu item being ordered
     * @param basePrice   the base price in COIN
     * @param inventory   the player's inventory
     * @param notoriety   the player's current notoriety score
     * @param currentHour current in-game hour
     * @param dayNumber   current in-game day number
     * @param weather     current weather condition
     * @param tonyNpc     Tony's NPC instance (for rumour seeding), may be null
     * @param achievementCallback callback for awarding achievements, may be null
     * @return the result of the order attempt
     */
    public OrderResult placeOrder(Material material, int basePrice,
                                  Inventory inventory, int notoriety,
                                  float currentHour, int dayNumber, Weather weather,
                                  NPC tonyNpc,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        // Opening hours check
        if (!isOpen(currentHour, weather)) {
            return OrderResult.SHOP_CLOSED;
        }

        // Notoriety refusal
        if (notoriety >= SERVICE_REFUSED_NOTORIETY) {
            return OrderResult.SERVICE_REFUSED;
        }

        // Queue jump check
        if (!isPlayerAtFront() && queue.size() > 0) {
            // Player is jumping the queue
            seedQueueJumpRumour(tonyNpc);
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.QUEUE_JUMPER);
            }
            return OrderResult.QUEUE_JUMP_REFUSED;
        }

        // Fish supper availability
        if (material == Material.FISH_SUPPER && !isFishSupperAvailable(dayNumber)) {
            return OrderResult.ITEM_UNAVAILABLE;
        }

        // CHIP_BUTTY requires BREAD
        if (material == Material.CHIP_BUTTY && !inventory.hasItem(Material.BREAD)) {
            return OrderResult.MISSING_BREAD;
        }

        // Apply heatwave surcharge
        int finalPrice = basePrice + (weather == Weather.HEATWAVE ? HEATWAVE_PRICE_SURCHARGE : 0);

        // Funds check
        if (inventory.getItemCount(Material.COIN) < finalPrice) {
            return OrderResult.INSUFFICIENT_FUNDS;
        }

        // Deduct coins and add item
        inventory.removeItem(Material.COIN, finalPrice);
        inventory.addItem(material, 1);

        // Remove BREAD if CHIP_BUTTY
        if (material == Material.CHIP_BUTTY) {
            inventory.removeItem(Material.BREAD, 1);
        }

        // Track purchase day for CHIPPY_REGULAR achievement
        purchaseDays.add(dayNumber);
        if (purchaseDays.size() >= 5 && achievementCallback != null) {
            achievementCallback.award(AchievementType.CHIPPY_REGULAR);
        }

        // Last orders achievement
        if (isLastOrders(currentHour) && achievementCallback != null) {
            achievementCallback.award(AchievementType.LAST_ORDERS_CHIPPY);
        }

        // Tony shares a rumour from the network
        if (tonyNpc != null && rumourNetwork != null) {
            List<Rumour> tonyRumours = tonyNpc.getRumours();
            if (!tonyRumours.isEmpty()) {
                // Share the first rumour (most recent) — caller handles player notification
                rumourNetwork.addRumour(tonyNpc, tonyRumours.get(0).spread());
            }
        }

        return OrderResult.SUCCESS;
    }

    /**
     * Returns the effective price of a menu item, accounting for HEATWAVE surcharge.
     *
     * @param basePrice base price in COIN
     * @param weather   current weather
     * @return effective price
     */
    public int getEffectivePrice(int basePrice, Weather weather) {
        return basePrice + (weather == Weather.HEATWAVE ? HEATWAVE_PRICE_SURCHARGE : 0);
    }

    // ── Biscuit the cat ───────────────────────────────────────────────────────

    /**
     * Player punches Biscuit the STRAY_CAT NPC.
     * Biscuit flees, Tony goes AGGRESSIVE, rumour is seeded town-wide.
     *
     * @param biscuit             Biscuit's NPC instance
     * @param tonyNpc             Tony's NPC instance
     * @param achievementCallback callback for awarding achievements, may be null
     */
    public void onBiscuitPunched(NPC biscuit, NPC tonyNpc,
                                 NotorietySystem.AchievementCallback achievementCallback) {
        // Biscuit flees
        if (biscuit != null) {
            biscuit.setState(NPCState.FLEEING);
        }

        // Tony goes aggressive
        if (tonyNpc != null) {
            tonyNpc.setState(NPCState.AGGRESSIVE);
        }

        // Notoriety gain
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(BISCUIT_PUNCH_NOTORIETY, achievementCallback);
        }

        // Seed CAT_PUNCH rumour town-wide via Tony
        NPC source = (tonyNpc != null) ? tonyNpc : new NPC(NPCType.CHIPPY_OWNER, 0f, 1f, 0f);
        if (rumourNetwork != null) {
            rumourNetwork.addRumour(source,
                    new Rumour(RumourType.CAT_PUNCH,
                            "Someone punched Biscuit outside Tony's — Tony's furious"));
        }
    }

    /**
     * Player feeds Biscuit with CHIPS or FISH_SUPPER.
     * On 3rd feed, awards CAT_FEEDER achievement.
     *
     * @param foodMaterial        the food being offered
     * @param inventory           player inventory
     * @param biscuit             Biscuit's NPC instance, may be null if not present
     * @param achievementCallback callback for awarding achievements, may be null
     * @return the result of the feed attempt
     */
    public FeedBiscuitResult feedBiscuit(Material foodMaterial, Inventory inventory,
                                         NPC biscuit, NotorietySystem.AchievementCallback achievementCallback) {
        if (biscuit == null) {
            return FeedBiscuitResult.BISCUIT_NOT_PRESENT;
        }

        if (foodMaterial != Material.CHIPS && foodMaterial != Material.FISH_SUPPER) {
            return FeedBiscuitResult.WRONG_FOOD;
        }

        // Consume the food item
        if (!inventory.hasItem(foodMaterial)) {
            return FeedBiscuitResult.WRONG_FOOD;
        }
        inventory.removeItem(foodMaterial, 1);

        // Biscuit becomes friendly
        biscuit.setState(NPCState.IDLE); // friendly idle

        biscuitFeedCount++;
        if (biscuitFeedCount >= 3 && achievementCallback != null) {
            achievementCallback.award(AchievementType.FED_THE_CAT);
        }

        return FeedBiscuitResult.SUCCESS;
    }

    /**
     * Returns the number of times Biscuit has been fed.
     */
    public int getBiscuitFeedCount() {
        return biscuitFeedCount;
    }

    // ── Frying-pan heist ─────────────────────────────────────────────────────

    /**
     * Player attempts the frying-pan heist during the 11:00 opening window.
     * Tony is in the back room for a short period; detection depends on queue size
     * and disguise.
     *
     * @param currentHour     current in-game hour
     * @param weather         current weather (affects open hour)
     * @param inventory       player inventory
     * @param disguiseModifier disguise detection reduction (0.0–1.0)
     * @return the result of the heist attempt
     */
    public HeistResult attemptFryingPanHeist(float currentHour, Weather weather,
                                              Inventory inventory, float disguiseModifier) {
        if (heistLooted) {
            return HeistResult.ALREADY_LOOTED;
        }

        // Heist window: exactly at opening time
        float openHour = (weather == Weather.FROST) ? FROST_OPEN_HOUR : HEIST_WINDOW_HOUR;
        if (currentHour < openHour || currentHour >= openHour + 0.5f) {
            return HeistResult.WINDOW_NOT_OPEN;
        }

        // Detection calculation
        float detectionChance = HEIST_BASE_DETECTION
                + (queue.size() * HEIST_DETECTION_PER_QUEUE_NPC)
                - disguiseModifier;
        detectionChance = Math.max(0f, Math.min(1f, detectionChance));

        if (rng.nextFloat() < detectionChance) {
            return HeistResult.DETECTED;
        }

        // Success: steal 1–2 items from a pool of chippy food
        int lootCount = HEIST_LOOT_MIN + rng.nextInt(HEIST_LOOT_MAX - HEIST_LOOT_MIN + 1);
        Material[] lootPool = {Material.CHIPS, Material.BATTERED_SAUSAGE, Material.MUSHY_PEAS};
        for (int i = 0; i < lootCount; i++) {
            Material loot = lootPool[rng.nextInt(lootPool.length)];
            inventory.addItem(loot, 1);
        }

        heistLooted = true;
        return HeistResult.SUCCESS;
    }

    /**
     * Returns whether the frying-pan heist has already been completed this session.
     */
    public boolean isHeistLooted() {
        return heistLooted;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void seedQueueJumpRumour(NPC source) {
        if (rumourNetwork == null) return;
        NPC rumourSource = (source != null) ? source
                : new NPC(NPCType.PUBLIC, 0f, 1f, 0f);
        rumourNetwork.addRumour(rumourSource,
                new Rumour(RumourType.QUEUE_JUMP,
                        "Someone jumped the queue outside Tony's — reckons they're above waiting"));
    }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /**
     * Force a specific queue size for testing purposes.
     */
    public void setQueueSizeForTesting(int size) {
        queue.clear();
        for (int i = 0; i < size; i++) {
            queue.add(NPCType.PUBLIC);
        }
    }

    /**
     * Set the heist looted flag for testing purposes.
     */
    public void setHeistLootedForTesting(boolean looted) {
        this.heistLooted = looted;
    }

    /**
     * Set the biscuit feed count for testing purposes.
     */
    public void setBiscuitFeedCountForTesting(int count) {
        this.biscuitFeedCount = count;
    }

    /**
     * Set the purchase days for testing purposes.
     */
    public void setPurchaseDaysForTesting(Set<Integer> days) {
        this.purchaseDays.clear();
        this.purchaseDays.addAll(days);
    }

    /**
     * Returns the number of distinct purchase days (for CHIPPY_REGULAR progress).
     */
    public int getPurchaseDayCount() {
        return purchaseDays.size();
    }
}

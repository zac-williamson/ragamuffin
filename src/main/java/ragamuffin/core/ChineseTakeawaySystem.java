package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Issue #1077: Northfield Chinese Takeaway — Golden Palace, Late-Night Grub &amp; the Prawn Cracker Economy.
 *
 * <p>The Golden Palace ({@code LandmarkType.CHINESE_TAKEAWAY}) is a narrow shopfront open 16:00–23:30
 * daily, run by Mr. Chen ({@code SHOPKEEPER} NPC). It serves a post-pub crowd with classic Chinese
 * takeaway food and a fortune cookie mechanic.
 *
 * <h3>Opening Hours</h3>
 * <ul>
 *   <li>Open: 16:00–23:30 daily.</li>
 * </ul>
 *
 * <h3>Menu</h3>
 * <ul>
 *   <li>{@link Material#PRAWN_CRACKERS} — 1 COIN. Hunger −10.</li>
 *   <li>{@link Material#SPRING_ROLLS} — 2 COIN. Hunger −20.</li>
 *   <li>{@link Material#CHICKEN_CHOW_MEIN} — 4 COIN. Hunger −50, Warmth +10. Wait 20–40s.</li>
 *   <li>{@link Material#EGG_FRIED_RICE} — 2 COIN. Hunger −30.</li>
 *   <li>{@link Material#SWEET_AND_SOUR_CHICKEN} — 4 COIN. Hunger −50. Wait 20–40s.</li>
 *   <li>{@link Material#CRISPY_DUCK} — 6 COIN. Hunger −60, Warmth +15. Wait 20–40s, no refund.</li>
 *   <li>{@link Material#FORTUNE_COOKIE} — 1 COIN. Random British fortune string on use.</li>
 *   <li>{@link Material#TAKEAWAY_BAG} — 0 COIN, free with orders ≥ 3 COIN.</li>
 * </ul>
 *
 * <h3>Price Modifiers</h3>
 * <ul>
 *   <li>COLD_SNAP weather raises all prices by +1 COIN.</li>
 *   <li>Free {@link Material#PRAWN_CRACKERS} with orders ≥ 5 COIN when Notoriety ≤ 30.</li>
 * </ul>
 *
 * <h3>Ordering Wait Mechanic</h3>
 * Non-instant items ({@code CHICKEN_CHOW_MEIN}, {@code SWEET_AND_SOUR_CHICKEN}, {@code CRISPY_DUCK})
 * trigger a 20–40 second wait. Leaving cancels the order; {@code CRISPY_DUCK} is not refunded.
 *
 * <h3>Delivery</h3>
 * Player can call from their squat's {@code TELEPHONE_PROP}. A {@code DELIVERY_DRIVER} NPC arrives
 * in 3 in-game minutes. Blocked at Notoriety ≥ {@link #DELIVERY_BLOCKED_NOTORIETY}.
 *
 * <h3>Prawn Cracker Economy</h3>
 * Litter bags spawn near the shop 22:00–00:00 (1 per 8 in-game minutes, cap 6). Player can:
 * feed to {@code BIRD} NPCs (seeds {@code PIGEON_CHAOS} rumour), or throw at NPCs (+1 Notoriety).
 *
 * <h3>Integration</h3>
 * <ul>
 *   <li>{@link WarmthSystem} — shop interior counts as shelter (+3 Warmth/min).</li>
 *   <li>{@link WeatherSystem} — rain adds extra NPCs; COLD_SNAP raises prices by +1.</li>
 *   <li>{@link RumourNetwork} — seeds {@code PIGEON_CHAOS}, {@code LOCAL_EVENT}, {@code NEIGHBOURHOOD}.</li>
 *   <li>{@link NotorietySystem} — +1 from food throw; PCSO event adds star.</li>
 *   <li>{@link AchievementSystem} — 5 achievements: LATE_NIGHT_REGULAR, FORTUNE_SEEKER,
 *       PRAWN_CRACKER_PIGEON_FEEDER, CRISPY_DUCK_CONNOISSEUR, PHONE_CHAOS.</li>
 *   <li>{@link WantedSystem} — delivery blocked at Notoriety ≥ 50.</li>
 * </ul>
 */
public class ChineseTakeawaySystem {

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Hour the shop opens. */
    public static final float OPEN_HOUR = 16.0f;

    /** Hour the shop closes (23:30 expressed as 23.5). */
    public static final float CLOSE_HOUR = 23.5f;

    // ── Delivery ──────────────────────────────────────────────────────────────

    /** Notoriety threshold above which delivery is refused. */
    public static final int DELIVERY_BLOCKED_NOTORIETY = 50;

    /** In-game minutes for delivery to arrive. */
    public static final float DELIVERY_ARRIVE_MINUTES = 3.0f;

    /** Extra COIN delivery charge on top of menu price. */
    public static final int DELIVERY_SURCHARGE = 1;

    // ── Free prawn crackers ───────────────────────────────────────────────────

    /** Order total above which free prawn crackers are given (Notoriety ≤ 30). */
    public static final int FREE_PRAWN_CRACKER_ORDER_MIN = 5;

    /** Max Notoriety for free prawn cracker bonus. */
    public static final int FREE_PRAWN_CRACKER_NOTORIETY_MAX = 30;

    /** Minimum order total for a free TAKEAWAY_BAG. */
    public static final int FREE_BAG_ORDER_MIN = 3;

    // ── Wait mechanic ─────────────────────────────────────────────────────────

    /** Minimum wait time in seconds for slow-cook items. */
    public static final float WAIT_SECONDS_MIN = 20.0f;

    /** Maximum wait time in seconds for slow-cook items. */
    public static final float WAIT_SECONDS_MAX = 40.0f;

    // ── Prawn cracker litter ──────────────────────────────────────────────────

    /** In-game minutes between litter spawns (22:00–00:00). */
    public static final float LITTER_SPAWN_INTERVAL_MINUTES = 8.0f;

    /** Maximum number of litter props that can exist at once. */
    public static final int LITTER_CAP = 6;

    /** Start hour for litter spawning. */
    public static final float LITTER_START_HOUR = 22.0f;

    /** End hour for litter spawning (wraps past midnight). */
    public static final float LITTER_END_HOUR = 0.0f;

    // ── Pigeon feeding ────────────────────────────────────────────────────────

    /** Duration in seconds a pigeon stays attracted after being fed. */
    public static final float PIGEON_ATTRACT_SECONDS = 15.0f;

    /** Notoriety gained by throwing food at an NPC. */
    public static final int FOOD_THROW_NOTORIETY = 1;

    // ── Notoriety service refusal ─────────────────────────────────────────────

    /** Notoriety threshold above which Mr. Chen refuses in-person service. */
    public static final int SERVICE_REFUSED_NOTORIETY = 70;

    // ── Fortune cookie pool ───────────────────────────────────────────────────

    private static final List<String> FORTUNE_POOL = Collections.unmodifiableList(Arrays.asList(
        "Hard work is its own reward.",
        "Beware the man who offers easy coin.",
        "The pigeon sees everything.",
        "A kebab deferred is a kebab forgotten.",
        "Someone nearby owes you money.",
        "Your notoriety precedes you.",
        "The allotment grows best when unwatched.",
        "A change in weather brings a change in price.",
        "Opportunity knocks. So does the bailiff.",
        "The council has been watching. They always are.",
        "Never trust a scratch card that winks.",
        "Three prawn crackers bring luck. Four bring pigeons.",
        "The night bus waits for no one.",
        "Wisdom is knowing when the off-licence closes.",
        "Your stars say: wear a coat.",
        "What is borrowed is never truly returned.",
        "The crispy duck does not regret the waiting.",
        "A steady hand wins at darts. A shaky one wins at dominoes.",
        "Northfield remembers. Northfield always remembers.",
        "Fortune favours the well-fed."
    ));

    // ── Menu item definition ──────────────────────────────────────────────────

    /**
     * Represents a single item on the Golden Palace menu.
     */
    public static class MenuItem {
        private final Material material;
        private final int basePrice;
        private final boolean requiresWait;

        public MenuItem(Material material, int basePrice, boolean requiresWait) {
            this.material = material;
            this.basePrice = basePrice;
            this.requiresWait = requiresWait;
        }

        public Material getMaterial() { return material; }
        public int getBasePrice()     { return basePrice; }
        public boolean isRequiresWait() { return requiresWait; }
    }

    // ── Static menu ───────────────────────────────────────────────────────────

    private static final List<MenuItem> MENU = Collections.unmodifiableList(Arrays.asList(
        new MenuItem(Material.PRAWN_CRACKERS,        1, false),
        new MenuItem(Material.SPRING_ROLLS,          2, false),
        new MenuItem(Material.CHICKEN_CHOW_MEIN,     4, true),
        new MenuItem(Material.EGG_FRIED_RICE,        2, false),
        new MenuItem(Material.SWEET_AND_SOUR_CHICKEN,4, true),
        new MenuItem(Material.CRISPY_DUCK,           6, true),
        new MenuItem(Material.FORTUNE_COOKIE,        1, false)
    ));

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random rng;

    /** Whether the shop is currently open. */
    private boolean shopOpen = false;

    /** Mr. Chen NPC (null when shop is closed). */
    private NPC mrChen = null;

    /** Active wait timer in seconds (>0 means an order is being prepared). */
    private float waitTimer = 0f;

    /** Total wait seconds for the current order. */
    private float waitTotal = 0f;

    /** The item currently being prepared (null if no active wait). */
    private Material pendingItem = null;

    /** Whether the pending item is Crispy Duck (no refund if cancelled). */
    private boolean pendingIsCrispyDuck = false;

    /** Current number of litter props spawned. */
    private int litterCount = 0;

    /** Elapsed minutes since last litter spawn. */
    private float litterSpawnTimer = 0f;

    /** Delivery driver NPC (null when no active delivery). */
    private NPC deliveryDriver = null;

    /** Remaining delivery time in seconds. */
    private float deliveryTimer = 0f;

    /** Inventory to deliver the item into on delivery completion. */
    private Inventory deliveryInventory = null;

    /** Item being delivered. */
    private Material deliveryItem = null;

    // ── Optional system references ────────────────────────────────────────────

    private RumourNetwork rumourNetwork;
    private NotorietySystem notorietySystem;
    private AchievementSystem achievementSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public ChineseTakeawaySystem() {
        this(new Random());
    }

    public ChineseTakeawaySystem(Random rng) {
        this.rng = rng;
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setRumourNetwork(RumourNetwork r)       { this.rumourNetwork = r; }
    public void setNotorietySystem(NotorietySystem n)   { this.notorietySystem = n; }
    public void setAchievementSystem(AchievementSystem a) { this.achievementSystem = a; }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns whether the shop is open at the given hour of day.
     *
     * @param hour current in-game hour (0.0–24.0)
     * @return true if the shop is open
     */
    public boolean isOpen(float hour) {
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Finds a menu item by material type.
     *
     * @param material the menu item's material
     * @return the matching {@link MenuItem}, or {@code null} if not on the menu
     */
    public MenuItem findMenuItem(Material material) {
        for (MenuItem item : MENU) {
            if (item.getMaterial() == material) {
                return item;
            }
        }
        return null;
    }

    /**
     * Calculates the actual price for an item given the current weather.
     * COLD_SNAP weather adds +1 COIN to all items.
     *
     * @param material     the item to price
     * @param coldSnap     whether COLD_SNAP weather is active
     * @return the final price in COIN, or -1 if the item is not on the menu
     */
    public int calculateOrderPrice(Material material, boolean coldSnap) {
        MenuItem item = findMenuItem(material);
        if (item == null) return -1;
        return item.getBasePrice() + (coldSnap ? 1 : 0);
    }

    /**
     * Returns whether delivery is blocked for the given notoriety level.
     *
     * @param notoriety the player's current notoriety score
     * @return true if delivery is refused
     */
    public boolean isDeliveryBlocked(int notoriety) {
        return notoriety >= DELIVERY_BLOCKED_NOTORIETY;
    }

    /**
     * Returns a fortune string deterministically for the given seed, or randomly if seed is 0.
     *
     * @param seed RNG seed (use 0 for random, or a positive value for deterministic selection)
     * @return a non-null, non-empty fortune string from the pool
     */
    public String getFortune(long seed) {
        int index;
        if (seed != 0) {
            index = (int) (Math.abs(seed) % FORTUNE_POOL.size());
        } else {
            index = rng.nextInt(FORTUNE_POOL.size());
        }
        return FORTUNE_POOL.get(index);
    }

    /**
     * Returns the full fortune pool (for validation).
     */
    public List<String> getFortunePool() {
        return FORTUNE_POOL;
    }

    /**
     * Returns the full menu.
     */
    public List<MenuItem> getMenu() {
        return MENU;
    }

    // ── Ordering ──────────────────────────────────────────────────────────────

    /**
     * Player places an order for the given item.
     * Deducts COIN from inventory, starts wait timer if item requires waiting,
     * or delivers instantly otherwise. Handles free prawn crackers and bag bonuses.
     *
     * @param material        the item being ordered
     * @param inventory       player inventory (used for COIN deduction and item delivery)
     * @param notoriety       player's current notoriety (for free prawn cracker check)
     * @param coldSnap        whether COLD_SNAP weather is active (price surcharge)
     * @param currentHour     current in-game hour (for LATE_NIGHT_REGULAR achievement)
     * @return true if the order was accepted, false if refused (insufficient COIN, shop closed,
     *         or service refused due to notoriety)
     */
    public boolean placeOrder(Material material, Inventory inventory, int notoriety,
                              boolean coldSnap, float currentHour) {
        if (!isOpen(currentHour)) return false;
        if (notoriety >= SERVICE_REFUSED_NOTORIETY) return false;

        int price = calculateOrderPrice(material, coldSnap);
        if (price < 0) return false;

        // Check player has enough COIN
        int coinCount = inventory.getCount(Material.COIN);
        if (coinCount < price) return false;

        // Deduct COIN
        for (int i = 0; i < price; i++) {
            inventory.removeItem(Material.COIN, 1);
        }

        // Late-night achievement
        if (currentHour >= 23.0f && currentHour < 24.0f && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.LATE_NIGHT_REGULAR);
        }

        // Crispy Duck connoisseur progress
        if (material == Material.CRISPY_DUCK && achievementSystem != null) {
            achievementSystem.recordProgress(AchievementType.CRISPY_DUCK_CONNOISSEUR);
        }

        // Free prawn crackers bonus (order >= 5 COIN, notoriety <= 30)
        if (price >= FREE_PRAWN_CRACKER_ORDER_MIN && notoriety <= FREE_PRAWN_CRACKER_NOTORIETY_MAX) {
            inventory.addItem(Material.PRAWN_CRACKERS, 1);
        }

        // Free takeaway bag (order >= 3 COIN)
        if (price >= FREE_BAG_ORDER_MIN) {
            inventory.addItem(Material.TAKEAWAY_BAG, 1);
        }

        MenuItem item = findMenuItem(material);
        if (item != null && item.isRequiresWait()) {
            // Start wait timer
            pendingItem = material;
            pendingIsCrispyDuck = (material == Material.CRISPY_DUCK);
            waitTotal = WAIT_SECONDS_MIN + rng.nextFloat() * (WAIT_SECONDS_MAX - WAIT_SECONDS_MIN);
            waitTimer = 0f;
        } else {
            // Instant delivery
            inventory.addItem(material, 1);
            pendingItem = null;
            waitTimer = 0f;
            waitTotal = 0f;
        }

        return true;
    }

    /**
     * Returns the wait time in seconds for the most recently placed slow-cook order.
     * Call after {@link #placeOrder} returns true for a wait-item.
     *
     * @return wait seconds in [{@link #WAIT_SECONDS_MIN}, {@link #WAIT_SECONDS_MAX}],
     *         or 0 if no pending order
     */
    public float pollWaitSeconds() {
        return waitTotal;
    }

    /**
     * Player cancels the current pending order.
     * No refund for Crispy Duck.
     *
     * @param inventory player inventory (COIN refund deposited here unless Crispy Duck)
     * @param coldSnap  whether COLD_SNAP weather is active (to calculate refund amount)
     */
    public void cancelOrder(Inventory inventory, boolean coldSnap) {
        if (pendingItem == null) return;
        if (!pendingIsCrispyDuck) {
            int refund = calculateOrderPrice(pendingItem, coldSnap);
            for (int i = 0; i < refund; i++) {
                inventory.addItem(Material.COIN, 1);
            }
        }
        pendingItem = null;
        pendingIsCrispyDuck = false;
        waitTimer = 0f;
        waitTotal = 0f;
    }

    // ── Fortune cookie ────────────────────────────────────────────────────────

    /**
     * Player uses a fortune cookie from their inventory.
     * Removes one from inventory; returns the fortune string; tracks achievement progress.
     *
     * @param inventory player inventory
     * @return the fortune string, or null if no FORTUNE_COOKIE in inventory
     */
    public String useFortuneCookie(Inventory inventory) {
        if (inventory.getCount(Material.FORTUNE_COOKIE) <= 0) return null;
        inventory.removeItem(Material.FORTUNE_COOKIE, 1);
        if (achievementSystem != null) {
            achievementSystem.recordProgress(AchievementType.FORTUNE_SEEKER);
        }
        return getFortune(0L);
    }

    // ── Prawn cracker feeding ─────────────────────────────────────────────────

    /**
     * Player feeds prawn crackers to nearby BIRD NPCs.
     * Removes one PRAWN_CRACKERS from inventory, sets BIRD NPCs to attracted state,
     * seeds PIGEON_CHAOS rumour, and tracks achievement progress.
     *
     * @param player        the player (used as rumour source)
     * @param birds         list of nearby BIRD NPCs (all are attracted)
     * @param rumourNetwork the rumour network (PIGEON_CHAOS seeded here)
     * @return true if feeding occurred (inventory had prawn crackers), false otherwise
     */
    public boolean onFeedPigeons(Player player, List<NPC> birds, RumourNetwork rumourNetwork) {
        if (birds == null || birds.isEmpty()) return false;

        // Set attracted state on all BIRD NPCs
        for (NPC bird : birds) {
            if (bird.getType() == NPCType.BIRD) {
                bird.setState(NPCState.ATTRACTED);
            }
        }

        // Seed PIGEON_CHAOS rumour on the first bird as the originating NPC
        if (rumourNetwork != null && !birds.isEmpty()) {
            NPC source = birds.get(0);
            rumourNetwork.addRumour(source, new Rumour(
                RumourType.PIGEON_CHAOS,
                "Someone's feeding prawn crackers to the pigeons outside the Golden Palace — they've gone absolutely mental."
            ));
        }

        // Achievement progress
        if (achievementSystem != null) {
            achievementSystem.recordProgress(AchievementType.PRAWN_CRACKER_PIGEON_FEEDER);
        }

        return true;
    }

    // ── Delivery ──────────────────────────────────────────────────────────────

    /**
     * Player initiates a delivery order from the squat TELEPHONE_PROP.
     * Deducts COIN (price + 1 surcharge), spawns DELIVERY_DRIVER NPC.
     *
     * @param material    the item being ordered for delivery
     * @param inventory   player inventory
     * @param notoriety   player notoriety (delivery blocked if ≥ 50)
     * @param coldSnap    COLD_SNAP weather surcharge flag
     * @param currentHour current in-game hour
     * @return true if delivery was started, false if blocked or unaffordable
     */
    public boolean startDelivery(Material material, Inventory inventory, int notoriety,
                                 boolean coldSnap, float currentHour) {
        if (isDeliveryBlocked(notoriety)) return false;

        int price = calculateOrderPrice(material, coldSnap) + DELIVERY_SURCHARGE;
        int coinCount = inventory.getCount(Material.COIN);
        if (coinCount < price) return false;

        for (int i = 0; i < price; i++) {
            inventory.removeItem(Material.COIN, 1);
        }

        // Spawn delivery driver
        deliveryDriver = new NPC(NPCType.DELIVERY_DRIVER, 0f, 1f, 0f);
        deliveryItem = material;
        // 3 in-game minutes = 180 seconds
        deliveryTimer = DELIVERY_ARRIVE_MINUTES * 60f;
        deliveryInventory = inventory;

        return true;
    }

    // ── Main update ───────────────────────────────────────────────────────────

    /**
     * Update the Chinese takeaway system each frame.
     *
     * @param delta       seconds since last frame
     * @param hour        current in-game hour
     * @param inventory   player inventory (for delivering pending orders)
     */
    public void update(float delta, float hour, Inventory inventory) {
        boolean shouldBeOpen = isOpen(hour);

        if (shouldBeOpen && !shopOpen) {
            shopOpen = true;
            mrChen = new NPC(NPCType.SHOPKEEPER, 0f, 1f, 0f);
        } else if (!shouldBeOpen && shopOpen) {
            shopOpen = false;
            if (mrChen != null) {
                mrChen.kill();
                mrChen = null;
            }
        }

        // Advance wait timer for pending orders
        if (pendingItem != null && inventory != null) {
            waitTimer += delta;
            if (waitTimer >= waitTotal) {
                inventory.addItem(pendingItem, 1);
                pendingItem = null;
                pendingIsCrispyDuck = false;
                waitTimer = 0f;
                waitTotal = 0f;
            }
        }

        // Advance delivery timer
        if (deliveryDriver != null) {
            deliveryTimer -= delta;
            if (deliveryTimer <= 0f && deliveryInventory != null && deliveryItem != null) {
                deliveryInventory.addItem(deliveryItem, 1);
                deliveryDriver.kill();
                deliveryDriver = null;
                deliveryItem = null;
                deliveryInventory = null;
                deliveryTimer = 0f;
            }
        }

        // Litter spawning 22:00–00:00
        boolean litterHour = (hour >= LITTER_START_HOUR || hour < LITTER_END_HOUR);
        if (litterHour && litterCount < LITTER_CAP) {
            litterSpawnTimer += delta;
            float intervalSeconds = LITTER_SPAWN_INTERVAL_MINUTES * 60f;
            if (litterSpawnTimer >= intervalSeconds) {
                litterSpawnTimer = 0f;
                litterCount++;
            }
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Whether the shop is currently open. */
    public boolean isShopOpen()          { return shopOpen; }

    /** Mr. Chen NPC (null when shop is closed). */
    public NPC getMrChen()               { return mrChen; }

    /** Whether there is a pending order being prepared. */
    public boolean hasPendingOrder()     { return pendingItem != null; }

    /** The material currently being prepared, or null. */
    public Material getPendingItem()     { return pendingItem; }

    /** Current litter prop count. */
    public int getLitterCount()          { return litterCount; }

    /** The delivery driver NPC, or null. */
    public NPC getDeliveryDriver()       { return deliveryDriver; }

    /** Whether a delivery is currently in progress. */
    public boolean isDeliveryInProgress() { return deliveryDriver != null; }

    // ── Litter pickup ─────────────────────────────────────────────────────────

    /**
     * Player picks up a litter bag (PRAWN_CRACKERS) from the ground.
     * Reduces litter count by 1 and adds to player inventory.
     *
     * @param inventory player inventory
     * @return true if a litter item was available to pick up
     */
    public boolean pickupLitter(Inventory inventory) {
        if (litterCount <= 0) return false;
        litterCount--;
        inventory.addItem(Material.PRAWN_CRACKERS, 1);
        return true;
    }

    // ── Phone chaos event ─────────────────────────────────────────────────────

    /**
     * Player answers the Golden Palace phone during the Phone Order Chaos event.
     * Seeds a LOCAL_EVENT rumour and unlocks the PHONE_CHAOS achievement.
     *
     * @param mrChen        Mr. Chen NPC (used as rumour source)
     * @param rumourNetwork the rumour network
     */
    public void onAnswerPhone(NPC mrChen, RumourNetwork rumourNetwork) {
        if (rumourNetwork != null && mrChen != null) {
            rumourNetwork.addRumour(mrChen, new Rumour(
                RumourType.LOCAL_EVENT,
                "Someone answered the Golden Palace phone and gave a fake address. Mr. Chen is fuming."
            ));
        }
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.PHONE_CHAOS);
        }
    }
}

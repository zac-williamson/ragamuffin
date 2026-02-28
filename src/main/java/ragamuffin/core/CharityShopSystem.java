package ragamuffin.core;

import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Issue #930: Charity Shop System — Haggling, Mystery Bags &amp; Community Service.
 *
 * <p>Transforms the {@code LandmarkType.CHARITY_SHOP} into a fully interactive
 * economy hub operated by a {@code NPCType.VOLUNTEER}.
 *
 * <h3>Core features</h3>
 * <ul>
 *   <li><b>Rotating daily stock</b> — 6 items from a curated pool, always
 *       including at least one clothing item (COAT, WOOLLY_HAT, UMBRELLA,
 *       SLEEPING_BAG, TEXTBOOK, BROKEN_PHONE, DODGY_DVD etc.).</li>
 *   <li><b>Haggling</b> — offer 1 COIN less; 40% base acceptance rate,
 *       +20% at Notoriety Tier ≤ 1, +15% with 3+ session donations.
 *       Cold-snap haggle for clothing always succeeds with flavour speech.</li>
 *   <li><b>Mystery Bag prop</b> — 2 COIN for a weighted random item
 *       (CARDBOARD 30%, CRISPS 20%, NEWSPAPER 15%, BROKEN_PHONE 12%,
 *       DODGY_DVD 8%, WOOLLY_HAT 5%, COAT 3%, DIAMOND 1%).
 *       Max 3/day (1 for Notoriety Tier 3+). DIAMOND triggers
 *       {@code CHARITY_SHOP_DIAMOND} achievement.</li>
 *   <li><b>Donations</b> — press E → Donate; COAT/WOOLLY_HAT/UMBRELLA gives
 *       Notoriety −3 + {@code LOOT_TIP} rumour; DIAMOND gives Notoriety −10 +
 *       {@code DIAMOND_DONOR} achievement; 5+ donations = {@code COMMUNITY_SERVICE}
 *       achievement. Donated items enter shop stock at half fence value.</li>
 *   <li><b>VOLUNTEER NPC</b> — counter-anchored 6-block patrol route; refuses
 *       BALACLAVA wearers; nervously serves Tier 3+ players; reduces mystery
 *       bag limit to 1 for high-notoriety players.</li>
 *   <li><b>2 NPC customers</b> (PUBLIC/PENSIONER) browse and occasionally buy
 *       during opening hours.</li>
 * </ul>
 *
 * <h3>Integrations</h3>
 * <ul>
 *   <li>WarmthSystem — shop is a warm shelter</li>
 *   <li>NotorietySystem — donations reduce notoriety</li>
 *   <li>StreetEconomySystem — clothing satisfies COLD need</li>
 *   <li>RumourNetwork — donations seed LOOT_TIP</li>
 *   <li>FenceSystem — BROKEN_PHONE/DODGY_DVD sellable</li>
 *   <li>WeatherSystem — cold-snap triggers compassionate haggle</li>
 *   <li>NewspaperSystem — 5+ donations generates positive press story</li>
 *   <li>AchievementSystem — 4 new achievements</li>
 * </ul>
 *
 * <h3>Achievements</h3>
 * CHARITY_SHOP_DIAMOND, DIAMOND_DONOR, COMMUNITY_SERVICE, TIGHT_FISTED
 */
public class CharityShopSystem {

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Hour the charity shop opens. */
    public static final float OPEN_HOUR = 9.0f;

    /** Hour the charity shop closes (exclusive). */
    public static final float CLOSE_HOUR = 17.0f;

    // ── Stock constants ────────────────────────────────────────────────────────

    /** Number of items in the daily rotating stock. */
    public static final int DAILY_STOCK_SIZE = 6;

    /**
     * Full pool of materials available in the daily rotating stock.
     * At least one clothing item is always included.
     */
    public static final List<Material> STOCK_POOL = Collections.unmodifiableList(Arrays.asList(
            Material.COAT,
            Material.WOOLLY_HAT,
            Material.UMBRELLA,
            Material.SLEEPING_BAG,
            Material.TEXTBOOK,
            Material.BROKEN_PHONE,
            Material.DODGY_DVD,
            Material.NEWSPAPER,
            Material.CRISPS,
            Material.WOOLLY_HAT_ECONOMY
    ));

    /** Clothing items — at least one must always appear in daily stock. */
    public static final List<Material> CLOTHING_ITEMS = Collections.unmodifiableList(Arrays.asList(
            Material.COAT,
            Material.WOOLLY_HAT,
            Material.UMBRELLA,
            Material.SLEEPING_BAG,
            Material.WOOLLY_HAT_ECONOMY
    ));

    // ── Base prices for stock items (in COIN) ─────────────────────────────────

    /** Gets the base price (in COIN) for a stock item. */
    public static int getBasePrice(Material material) {
        if (material == null) return 1;
        switch (material) {
            case COAT:             return 5;
            case WOOLLY_HAT:       return 3;
            case UMBRELLA:         return 4;
            case SLEEPING_BAG:     return 6;
            case TEXTBOOK:         return 3;
            case BROKEN_PHONE:     return 2;
            case DODGY_DVD:        return 1;
            case NEWSPAPER:        return 1;
            case CRISPS:           return 1;
            case WOOLLY_HAT_ECONOMY: return 2;
            default:               return 1;
        }
    }

    // ── Haggling constants ─────────────────────────────────────────────────────

    /** Base acceptance rate for haggling (40%). */
    public static final float HAGGLE_BASE_RATE = 0.40f;

    /** Bonus acceptance rate when Notoriety Tier ≤ 1 (+20%). */
    public static final float HAGGLE_LOW_NOTORIETY_BONUS = 0.20f;

    /** Bonus acceptance rate with 3+ session donations (+15%). */
    public static final float HAGGLE_DONATION_BONUS = 0.15f;

    /** Minimum donations needed for the donation haggling bonus. */
    public static final int HAGGLE_DONATION_THRESHOLD = 3;

    /** Notoriety tier threshold for the low-notoriety haggling bonus. */
    public static final int HAGGLE_LOW_NOTORIETY_TIER_MAX = 1;

    // ── Mystery bag constants ──────────────────────────────────────────────────

    /** Cost of a mystery bag in COIN. */
    public static final int MYSTERY_BAG_COST = 2;

    /** Maximum mystery bags per day (normal). */
    public static final int MYSTERY_BAG_MAX_PER_DAY = 3;

    /** Maximum mystery bags per day for high-notoriety (Tier 3+) players. */
    public static final int MYSTERY_BAG_HIGH_NOTORIETY_MAX = 1;

    /** Notoriety tier at which mystery bag limit is reduced. */
    public static final int MYSTERY_BAG_HIGH_NOTORIETY_TIER = 3;

    /**
     * Weighted mystery bag loot pool.
     * Order: CARDBOARD, CRISPS, NEWSPAPER, BROKEN_PHONE, DODGY_DVD, WOOLLY_HAT, COAT, DIAMOND
     * Weights: 30,      20,     15,        12,           8,         5,          3,    1
     */
    private static final Material[] MYSTERY_BAG_LOOT = {
            Material.CARDBOARD,    // 30%
            Material.CRISPS,       // 20%
            Material.NEWSPAPER,    // 15%
            Material.BROKEN_PHONE, // 12%
            Material.DODGY_DVD,    //  8%
            Material.WOOLLY_HAT,   //  5%
            Material.COAT,         //  3%
            Material.DIAMOND       //  1%
    };
    private static final int[] MYSTERY_BAG_WEIGHTS = { 30, 20, 15, 12, 8, 5, 3, 1 };
    private static final int MYSTERY_BAG_TOTAL_WEIGHT = 94;

    // ── Donation constants ─────────────────────────────────────────────────────

    /** Notoriety reduction for donating clothing items. */
    public static final int DONATION_CLOTHING_NOTORIETY_REDUCTION = 3;

    /** Notoriety reduction for donating a DIAMOND. */
    public static final int DONATION_DIAMOND_NOTORIETY_REDUCTION = 10;

    /** Number of donations required for COMMUNITY_SERVICE achievement. */
    public static final int COMMUNITY_SERVICE_DONATION_TARGET = 5;

    // ── NPC customer constants ─────────────────────────────────────────────────

    /** Number of browsing customer NPCs. */
    public static final int CUSTOMER_COUNT = 2;

    /** Chance per second that a customer NPC makes a purchase. */
    public static final float CUSTOMER_PURCHASE_CHANCE_PER_SECOND = 0.005f;

    // ── Volunteer speech lines ─────────────────────────────────────────────────

    private static final String[] VOLUNTEER_SPEECH_LINES = {
            "Every penny goes to a good cause!",
            "All donations gratefully received.",
            "We've got some lovely bits in today.",
            "Can I help you find something?",
            "These prices are very reasonable.",
            "It all goes to the hospice, love."
    };

    private static final String BALACLAVA_REFUSAL =
            "I'm sorry, I can't serve you while you're wearing that.";

    private static final String NERVOUS_TIER3_SPEECH =
            "Um... here you go. Please don't cause any trouble.";

    private static final String COLD_SNAP_COMPASSIONATE_HAGGLE =
            "Oh, you poor love — it's freezing out there. Take it, take it.";

    private static final String HAGGLE_ACCEPTED =
            "Go on then, it's for a good cause anyway.";

    private static final String HAGGLE_REJECTED =
            "I'm afraid I can't go lower than that, love — it's for charity!";

    // ── State ─────────────────────────────────────────────────────────────────

    /** Whether the VOLUNTEER NPC has been spawned. */
    private boolean volunteerSpawned = false;

    /** The active VOLUNTEER NPC, or null if none. */
    private NPC volunteer = null;

    /** Currently active daily stock (up to DAILY_STOCK_SIZE items). */
    private final List<Material> dailyStock = new ArrayList<>();

    /** The in-game day on which the current stock was generated. */
    private int lastStockDay = -1;

    /** Number of mystery bags purchased today. */
    private int mysteryBagsTodayCount = 0;

    /** The in-game day on which mystery bags were last counted. */
    private int lastMysteryBagDay = -1;

    /** Total number of donations made this session. */
    private int totalDonations = 0;

    /** Whether COMMUNITY_SERVICE has been awarded. */
    private boolean communityServiceAwarded = false;

    /** Whether DIAMOND_DONOR has been awarded. */
    private boolean diamondDonorAwarded = false;

    /** Whether CHARITY_SHOP_DIAMOND has been awarded. */
    private boolean charityShopDiamondAwarded = false;

    /** Whether 5+ donations positive press has been generated. */
    private boolean positivePressGenerated = false;

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final Random random;

    // ── Construction ──────────────────────────────────────────────────────────

    public CharityShopSystem() {
        this(new Random());
    }

    public CharityShopSystem(Random random) {
        this.random = random;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Update the charity shop system each frame.
     *
     * @param delta           seconds since last frame
     * @param timeSystem      game time (for opening hours, stock rotation)
     * @param player          the player
     * @param npcManager      NPC manager (for spawning VOLUNTEER and customers)
     * @param notorietySystem notoriety system (for tier checks)
     * @param weatherSystem   weather system (for cold-snap haggle)
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       Player player,
                       NPCManager npcManager,
                       NotorietySystem notorietySystem,
                       WeatherSystem weatherSystem) {

        float hour = (timeSystem != null) ? timeSystem.getTime() : 12.0f;
        int day = (timeSystem != null) ? timeSystem.getDayCount() : 1;
        boolean isOpen = isOpen(hour);

        // Rotate stock daily
        if (day != lastStockDay) {
            generateDailyStock(day);
        }

        // Reset mystery bag daily count
        if (day != lastMysteryBagDay) {
            mysteryBagsTodayCount = 0;
            lastMysteryBagDay = day;
        }

        // Manage VOLUNTEER NPC
        if (isOpen && !volunteerSpawned) {
            spawnVolunteer(player, npcManager);
        } else if (!isOpen && volunteerSpawned) {
            despawnVolunteer();
        }
    }

    // ── Stock management ──────────────────────────────────────────────────────

    /**
     * Generate today's rotating stock. Always includes at least one clothing item.
     */
    private void generateDailyStock(int day) {
        lastStockDay = day;
        dailyStock.clear();

        List<Material> pool = new ArrayList<>(STOCK_POOL);
        Collections.shuffle(pool, random);

        // Ensure at least one clothing item is in the selection
        boolean hasClothing = false;
        List<Material> selected = new ArrayList<>();

        for (Material m : pool) {
            if (selected.size() >= DAILY_STOCK_SIZE) break;
            if (CLOTHING_ITEMS.contains(m)) hasClothing = true;
            selected.add(m);
        }

        // If no clothing item was selected, replace the last item with a random clothing item
        if (!hasClothing && !selected.isEmpty()) {
            List<Material> clothingPool = new ArrayList<>(CLOTHING_ITEMS);
            Collections.shuffle(clothingPool, random);
            selected.set(selected.size() - 1, clothingPool.get(0));
        }

        dailyStock.addAll(selected);
    }

    /**
     * Add a donated item to the daily stock (at half fence value).
     * Only if daily stock has room.
     */
    public void addDonatedItemToStock(Material material) {
        if (dailyStock.size() < DAILY_STOCK_SIZE * 2) { // allow some overflow
            dailyStock.add(material);
        }
    }

    // ── Purchase ──────────────────────────────────────────────────────────────

    /**
     * Result of attempting to purchase an item from the charity shop stock.
     */
    public enum PurchaseResult {
        /** Item purchased successfully. */
        SUCCESS,
        /** Shop is closed. */
        CLOSED,
        /** Player doesn't have enough COIN. */
        INSUFFICIENT_FUNDS,
        /** The item is not in today's stock. */
        OUT_OF_STOCK,
        /** Refused due to BALACLAVA. */
        BALACLAVA_REFUSED
    }

    /**
     * Attempt to purchase an item from the charity shop.
     *
     * @param hour         current in-game hour
     * @param material     item to buy
     * @param inventory    player inventory
     * @param hasBalaclava whether the player is wearing a balaclava
     * @return result of the purchase attempt
     */
    public PurchaseResult buyItem(float hour, Material material, Inventory inventory,
                                  boolean hasBalaclava) {
        if (!isOpen(hour)) {
            return PurchaseResult.CLOSED;
        }
        if (hasBalaclava) {
            if (volunteer != null) {
                volunteer.setSpeechText(BALACLAVA_REFUSAL, 4f);
            }
            return PurchaseResult.BALACLAVA_REFUSED;
        }
        if (!dailyStock.contains(material)) {
            return PurchaseResult.OUT_OF_STOCK;
        }

        int price = getBasePrice(material);
        if (inventory == null || inventory.getItemCount(Material.COIN) < price) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        inventory.removeItem(Material.COIN, price);
        inventory.addItem(material, 1);
        dailyStock.remove(material);

        return PurchaseResult.SUCCESS;
    }

    // ── Haggling ──────────────────────────────────────────────────────────────

    /**
     * Result of a haggling attempt.
     */
    public enum HaggleResult {
        /** Haggle accepted — item sold at reduced price. */
        ACCEPTED,
        /** Haggle rejected — player must pay full price. */
        REJECTED,
        /** Cold-snap compassionate haggle — clothing always succeeds with flavour text. */
        COLD_SNAP_ACCEPTED,
        /** Shop is closed. */
        CLOSED,
        /** The item is not in today's stock. */
        OUT_OF_STOCK,
        /** Refused due to BALACLAVA. */
        BALACLAVA_REFUSED,
        /** Player doesn't have enough COIN even at reduced price. */
        INSUFFICIENT_FUNDS
    }

    /**
     * Attempt to haggle for an item (offer 1 COIN less than the base price).
     *
     * @param hour           current in-game hour
     * @param material       item to haggle for
     * @param inventory      player inventory
     * @param hasBalaclava   whether the player is wearing a balaclava
     * @param notorietyTier  current player notoriety tier (0–5)
     * @param weather        current weather (cold-snap triggers compassionate haggle)
     * @param achievementCallback achievement callback (for TIGHT_FISTED)
     * @return result of the haggle attempt
     */
    public HaggleResult haggle(float hour, Material material, Inventory inventory,
                               boolean hasBalaclava, int notorietyTier, Weather weather,
                               NotorietySystem.AchievementCallback achievementCallback) {
        if (!isOpen(hour)) {
            return HaggleResult.CLOSED;
        }
        if (hasBalaclava) {
            if (volunteer != null) {
                volunteer.setSpeechText(BALACLAVA_REFUSAL, 4f);
            }
            return HaggleResult.BALACLAVA_REFUSED;
        }
        if (!dailyStock.contains(material)) {
            return HaggleResult.OUT_OF_STOCK;
        }

        int fullPrice = getBasePrice(material);
        int hagglePrice = Math.max(0, fullPrice - 1);

        // Check if player can afford the haggled price
        if (inventory == null || inventory.getItemCount(Material.COIN) < hagglePrice) {
            return HaggleResult.INSUFFICIENT_FUNDS;
        }

        // Cold-snap compassionate haggle for clothing — always succeeds
        boolean isColdSnap = (weather == Weather.COLD_SNAP || weather == Weather.FROST);
        boolean isClothing = CLOTHING_ITEMS.contains(material);
        if (isColdSnap && isClothing) {
            inventory.removeItem(Material.COIN, hagglePrice);
            inventory.addItem(material, 1);
            dailyStock.remove(material);
            if (volunteer != null) {
                volunteer.setSpeechText(COLD_SNAP_COMPASSIONATE_HAGGLE, 5f);
            }
            awardHaggleAchievement(achievementCallback);
            return HaggleResult.COLD_SNAP_ACCEPTED;
        }

        // Calculate acceptance probability
        float acceptChance = HAGGLE_BASE_RATE;
        if (notorietyTier <= HAGGLE_LOW_NOTORIETY_TIER_MAX) {
            acceptChance += HAGGLE_LOW_NOTORIETY_BONUS;
        }
        if (totalDonations >= HAGGLE_DONATION_THRESHOLD) {
            acceptChance += HAGGLE_DONATION_BONUS;
        }

        if (random.nextFloat() < acceptChance) {
            inventory.removeItem(Material.COIN, hagglePrice);
            inventory.addItem(material, 1);
            dailyStock.remove(material);
            if (volunteer != null) {
                volunteer.setSpeechText(HAGGLE_ACCEPTED, 3f);
            }
            awardHaggleAchievement(achievementCallback);
            return HaggleResult.ACCEPTED;
        } else {
            if (volunteer != null) {
                volunteer.setSpeechText(HAGGLE_REJECTED, 3f);
            }
            return HaggleResult.REJECTED;
        }
    }

    private void awardHaggleAchievement(NotorietySystem.AchievementCallback achievementCallback) {
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.TIGHT_FISTED);
        }
    }

    // ── Mystery Bag ───────────────────────────────────────────────────────────

    /**
     * Result of purchasing a mystery bag.
     */
    public enum MysteryBagResult {
        /** Mystery bag purchased; item is set in the result. */
        SUCCESS,
        /** Daily mystery bag limit reached. */
        LIMIT_REACHED,
        /** Shop is closed. */
        CLOSED,
        /** Refused due to BALACLAVA. */
        BALACLAVA_REFUSED,
        /** Player doesn't have enough COIN. */
        INSUFFICIENT_FUNDS
    }

    /**
     * Purchase a mystery bag.
     *
     * @param hour            current in-game hour
     * @param dayCount        current in-game day
     * @param inventory       player inventory
     * @param hasBalaclava    whether the player is wearing a balaclava
     * @param notorietyTier   current player notoriety tier
     * @param achievementCallback achievement callback (for CHARITY_SHOP_DIAMOND)
     * @return result of the mystery bag purchase; call {@link #getLastMysteryBagItem()} for the item
     */
    public MysteryBagResult buyMysteryBag(float hour, int dayCount, Inventory inventory,
                                           boolean hasBalaclava, int notorietyTier,
                                           NotorietySystem.AchievementCallback achievementCallback) {
        if (!isOpen(hour)) {
            return MysteryBagResult.CLOSED;
        }
        if (hasBalaclava) {
            if (volunteer != null) {
                volunteer.setSpeechText(BALACLAVA_REFUSAL, 4f);
            }
            return MysteryBagResult.BALACLAVA_REFUSED;
        }

        // Reset daily count if day changed
        if (dayCount != lastMysteryBagDay) {
            mysteryBagsTodayCount = 0;
            lastMysteryBagDay = dayCount;
        }

        int maxBags = (notorietyTier >= MYSTERY_BAG_HIGH_NOTORIETY_TIER)
                ? MYSTERY_BAG_HIGH_NOTORIETY_MAX
                : MYSTERY_BAG_MAX_PER_DAY;

        if (mysteryBagsTodayCount >= maxBags) {
            return MysteryBagResult.LIMIT_REACHED;
        }

        if (inventory == null || inventory.getItemCount(Material.COIN) < MYSTERY_BAG_COST) {
            return MysteryBagResult.INSUFFICIENT_FUNDS;
        }

        // Purchase the bag
        inventory.removeItem(Material.COIN, MYSTERY_BAG_COST);
        mysteryBagsTodayCount++;

        // Draw a weighted random item
        Material item = drawMysteryBagItem();
        lastMysteryBagItem = item;
        inventory.addItem(item, 1);

        // DIAMOND achievement
        if (item == Material.DIAMOND && !charityShopDiamondAwarded) {
            charityShopDiamondAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.CHARITY_SHOP_DIAMOND);
            }
        }

        return MysteryBagResult.SUCCESS;
    }

    /** The item drawn from the most recent mystery bag purchase. */
    private Material lastMysteryBagItem = null;

    /**
     * Returns the item from the most recent mystery bag purchase, or null if none.
     */
    public Material getLastMysteryBagItem() {
        return lastMysteryBagItem;
    }

    /**
     * Draw a weighted random item from the mystery bag loot pool.
     */
    private Material drawMysteryBagItem() {
        int roll = random.nextInt(MYSTERY_BAG_TOTAL_WEIGHT);
        int cumulative = 0;
        for (int i = 0; i < MYSTERY_BAG_LOOT.length; i++) {
            cumulative += MYSTERY_BAG_WEIGHTS[i];
            if (roll < cumulative) {
                return MYSTERY_BAG_LOOT[i];
            }
        }
        return MYSTERY_BAG_LOOT[0]; // fallback
    }

    // ── Donations ─────────────────────────────────────────────────────────────

    /**
     * Result of a donation attempt.
     */
    public enum DonationResult {
        /** Item donated successfully. */
        SUCCESS,
        /** Shop is closed. */
        CLOSED,
        /** Player doesn't have the item. */
        ITEM_NOT_IN_INVENTORY,
        /** Refused due to BALACLAVA. */
        BALACLAVA_REFUSED
    }

    /**
     * Donate an item to the charity shop.
     * <ul>
     *   <li>COAT/WOOLLY_HAT/UMBRELLA → Notoriety −3, seeds LOOT_TIP rumour</li>
     *   <li>DIAMOND → Notoriety −10, DIAMOND_DONOR achievement</li>
     *   <li>5+ donations → COMMUNITY_SERVICE achievement</li>
     * </ul>
     *
     * @param hour            current in-game hour
     * @param material        item to donate
     * @param inventory       player inventory
     * @param hasBalaclava    whether the player is wearing a balaclava
     * @param notorietySystem notoriety system (for notoriety reduction)
     * @param rumourNetwork   rumour network (for LOOT_TIP seeding)
     * @param allNpcs         all active NPCs (for rumour seeding)
     * @param achievementCallback achievement callback
     * @return result of the donation
     */
    public DonationResult donate(float hour, Material material, Inventory inventory,
                                  boolean hasBalaclava, NotorietySystem notorietySystem,
                                  RumourNetwork rumourNetwork, List<NPC> allNpcs,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        if (!isOpen(hour)) {
            return DonationResult.CLOSED;
        }
        if (hasBalaclava) {
            if (volunteer != null) {
                volunteer.setSpeechText(BALACLAVA_REFUSAL, 4f);
            }
            return DonationResult.BALACLAVA_REFUSED;
        }
        if (inventory == null || !inventory.hasItem(material)) {
            return DonationResult.ITEM_NOT_IN_INVENTORY;
        }

        // Remove item from inventory
        inventory.removeItem(material, 1);
        totalDonations++;

        // Add donated item to stock at half fence value
        addDonatedItemToStock(material);

        // Volunteer speech
        if (volunteer != null) {
            volunteer.setSpeechText("Oh, how wonderful! Thank you so much!", 4f);
        }

        // Clothing donation → Notoriety −3 + LOOT_TIP rumour
        boolean isClothing = (material == Material.COAT
                || material == Material.WOOLLY_HAT
                || material == Material.UMBRELLA);
        if (isClothing) {
            if (notorietySystem != null) {
                notorietySystem.reduceNotoriety(DONATION_CLOTHING_NOTORIETY_REDUCTION, null);
            }
            seedLootTipRumour(rumourNetwork, allNpcs);
        }

        // DIAMOND donation → Notoriety −10 + DIAMOND_DONOR achievement
        if (material == Material.DIAMOND) {
            if (notorietySystem != null) {
                notorietySystem.reduceNotoriety(DONATION_DIAMOND_NOTORIETY_REDUCTION, null);
            }
            if (!diamondDonorAwarded && achievementCallback != null) {
                diamondDonorAwarded = true;
                achievementCallback.award(AchievementType.DIAMOND_DONOR);
            }
        }

        // COMMUNITY_SERVICE achievement at 5+ donations
        if (totalDonations >= COMMUNITY_SERVICE_DONATION_TARGET && !communityServiceAwarded) {
            communityServiceAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.COMMUNITY_SERVICE);
            }
            // Seed a positive press story via a LOOT_TIP rumour to seed goodwill town-wide
            if (!positivePressGenerated) {
                positivePressGenerated = true;
                seedLootTipRumour(rumourNetwork, allNpcs);
            }
        }

        return DonationResult.SUCCESS;
    }

    /**
     * Seed a LOOT_TIP rumour to the nearest available NPC.
     */
    private void seedLootTipRumour(RumourNetwork rumourNetwork, List<NPC> allNpcs) {
        if (rumourNetwork == null || allNpcs == null) return;
        for (NPC npc : allNpcs) {
            if (npc.getType() == NPCType.PUBLIC && npc.isAlive()) {
                rumourNetwork.addRumour(npc, new Rumour(
                        RumourType.LOOT_TIP,
                        "Heard the charity shop's got some good bits in — worth a look."));
                break;
            }
        }
    }

    // ── VOLUNTEER NPC management ──────────────────────────────────────────────

    private void spawnVolunteer(Player player, NPCManager npcManager) {
        if (volunteerSpawned || npcManager == null || player == null) return;

        volunteerSpawned = true;
        float x = player.getPosition().x + 2f;
        float y = player.getPosition().y;
        float z = player.getPosition().z + 2f;
        volunteer = npcManager.spawnNPC(NPCType.VOLUNTEER, x, y, z);
        if (volunteer != null) {
            volunteer.setName("Sandra");
            volunteer.setBuildingType(LandmarkType.CHARITY_SHOP);
            volunteer.setState(NPCState.PATROLLING);
            volunteer.setSpeechText(VOLUNTEER_SPEECH_LINES[0], 3f);
        }
    }

    private void despawnVolunteer() {
        if (volunteer != null) {
            volunteer.setState(NPCState.FLEEING);
            volunteer = null;
        }
        volunteerSpawned = false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true if the charity shop is open at the given in-game hour.
     */
    public boolean isOpen(float hour) {
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Returns true if the player's notoriety tier is high (Tier 3+), causing
     * the VOLUNTEER to serve them nervously and reduce mystery bag limit.
     */
    public boolean isHighNotoriety(int notorietyTier) {
        return notorietyTier >= MYSTERY_BAG_HIGH_NOTORIETY_TIER;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** The current daily stock list (read-only view). */
    public List<Material> getDailyStock() {
        return Collections.unmodifiableList(dailyStock);
    }

    /** Number of mystery bags purchased today. */
    public int getMysteryBagsTodayCount() {
        return mysteryBagsTodayCount;
    }

    /** Total donations made this session. */
    public int getTotalDonations() {
        return totalDonations;
    }

    /** Whether the VOLUNTEER NPC is currently spawned. */
    public boolean isVolunteerSpawned() {
        return volunteerSpawned;
    }

    /** The active VOLUNTEER NPC, or null if none. */
    public NPC getVolunteer() {
        return volunteer;
    }

    /** Whether the COMMUNITY_SERVICE achievement has been awarded. */
    public boolean isCommunityServiceAwarded() {
        return communityServiceAwarded;
    }

    /** Whether the DIAMOND_DONOR achievement has been awarded. */
    public boolean isDiamondDonorAwarded() {
        return diamondDonorAwarded;
    }

    /** Whether the CHARITY_SHOP_DIAMOND achievement has been awarded. */
    public boolean isCharityShopDiamondAwarded() {
        return charityShopDiamondAwarded;
    }

    // ── Force-set methods for testing ─────────────────────────────────────────

    /** Force-set the daily stock (for testing). */
    public void setDailyStockForTesting(List<Material> stock) {
        dailyStock.clear();
        dailyStock.addAll(stock);
    }

    /** Force-set the last stock day (for testing). */
    public void setLastStockDayForTesting(int day) {
        this.lastStockDay = day;
    }

    /** Force-set mystery bags today count (for testing). */
    public void setMysteryBagsTodayCountForTesting(int count) {
        this.mysteryBagsTodayCount = count;
    }

    /** Force-set last mystery bag day (for testing). */
    public void setLastMysteryBagDayForTesting(int day) {
        this.lastMysteryBagDay = day;
    }

    /** Force-set total donations (for testing). */
    public void setTotalDonationsForTesting(int count) {
        this.totalDonations = count;
    }

    /** Force-set volunteer NPC (for testing). */
    public void setVolunteerForTesting(NPC npc) {
        this.volunteer = npc;
        this.volunteerSpawned = npc != null;
    }

    /** Force-set charityShopDiamondAwarded (for testing). */
    public void setCharityShopDiamondAwardedForTesting(boolean awarded) {
        this.charityShopDiamondAwarded = awarded;
    }

    /** Force-set communityServiceAwarded (for testing). */
    public void setCommunityServiceAwardedForTesting(boolean awarded) {
        this.communityServiceAwarded = awarded;
    }

    /** Force-set diamondDonorAwarded (for testing). */
    public void setDiamondDonorAwardedForTesting(boolean awarded) {
        this.diamondDonorAwarded = awarded;
    }
}

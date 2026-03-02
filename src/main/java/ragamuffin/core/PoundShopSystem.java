package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1161: Northfield Poundstretcher — Own-Brand Bargains, Shoplifting Security
 * &amp; the Bulk-Buy Hustle.
 *
 * <h3>Opening Hours</h3>
 * <ul>
 *   <li>Monday–Saturday: 08:30–18:30.</li>
 *   <li>Sunday: 10:00–16:00.</li>
 * </ul>
 *
 * <h3>Shop Stock</h3>
 * <ul>
 *   <li>7 own-brand items at 1 {@code COIN} each:
 *       {@link Material#OWN_BRAND_CRISPS}, {@link Material#OWN_BRAND_COLA},
 *       {@link Material#WASHING_POWDER}, {@link Material#PARACETAMOL},
 *       {@link Material#TIN_OF_BEANS}, {@link Material#DODGY_PASTY},
 *       {@link Material#BARGAIN_BUCKET_CRISPS}.</li>
 *   <li>Bulk buy: 5 {@code COIN} for 6 random items from the stock —
 *       Sharon's contractual "deal". Awards {@link AchievementType#BULK_BUYER}
 *       on first use.</li>
 * </ul>
 *
 * <h3>Shoplifting — Shelf CRATE_PROPs</h3>
 * <ul>
 *   <li>Base catch chance via CCTV: {@link #CCTV_CATCH_CHANCE} (30%).</li>
 *   <li>Sharon has line of sight (LOS): {@link #SHARON_LOS_CATCH_CHANCE} (75%).</li>
 *   <li>Wearing a disguise ({@link Material#GREGGS_APRON} or
 *       {@link Material#HIGH_VIS_JACKET}): catch chance halved.</li>
 *   <li>One random CCTV blind-spot shelf per day — CCTV catch chance drops to 0%
 *       at that shelf.</li>
 *   <li>On catch: 2-day ban ({@link #BAN_DURATION_DAYS}), {@link CrimeType#SHOPLIFTING}
 *       recorded, {@link RumourType#SHOPLIFTING_BAN} seeded,
 *       {@link AchievementType#EVERY_POUNDS_A_PRISONER} awarded.</li>
 *   <li>Sharon catching: additionally awards {@link AchievementType#SHARON_KNOWS}.</li>
 *   <li>Successful theft: awards {@link AchievementType#POUNDSTRETCHER_FIVE_FINGER}.</li>
 * </ul>
 *
 * <h3>Ban System &amp; Proxy Street Lad</h3>
 * <ul>
 *   <li>Banned players cannot buy or shoplift directly.</li>
 *   <li>Proxy Street Lad buys items with 50% markup and a
 *       {@link #PROXY_SCAM_CHANCE} (30%) chance of simply taking the coin without
 *       delivering. Awards {@link AchievementType#MIDDLE_MAN} on first successful proxy use.</li>
 * </ul>
 *
 * <h3>Sharon's Speech Lines</h3>
 * <ul>
 *   <li>8 rotating lines delivered while on duty (see {@link #SHARON_SPEECH_LINES}).</li>
 * </ul>
 *
 * <h3>Delivery Drop Hustle</h3>
 * <ul>
 *   <li>Daily at 08:00 a {@link NPCType#DELIVERY_DRIVER} arrives at the loading bay
 *       with a {@code PALLET_PROP}.</li>
 *   <li>Player has {@link #PALLET_LOOT_WINDOW_SECONDS} (120 seconds / 2 minutes)
 *       to press E and loot one item before the pallet is brought inside.</li>
 *   <li>{@link #WHOLESALE_SPIRITS_CHANCE} (10%) chance of
 *       {@link Material#WHOLESALE_SPIRITS} (fence value 6 COIN); otherwise a
 *       random stock item.</li>
 *   <li>Successful loot awards {@link AchievementType#PALLET_PIRATE}.</li>
 * </ul>
 *
 * <h3>Notoriety Integration</h3>
 * <ul>
 *   <li>Tier 2+ triggers a pre-emptive police call on player entry with a
 *       {@link #POLICE_ESCAPE_WINDOW_SECONDS} (20-second) escape window.</li>
 * </ul>
 *
 * <h3>NewspaperSystem Integration</h3>
 * <ul>
 *   <li>3 thefts from Poundstretcher in one in-game day triggers a newspaper headline.</li>
 * </ul>
 *
 * <h3>Integrations</h3>
 * <ul>
 *   <li>{@link FenceSystem} — {@link Material#WHOLESALE_SPIRITS} is fenceable.</li>
 *   <li>{@link WitnessSystem} — police nearby triggers auto-arrest.</li>
 *   <li>{@link DisguiseSystem} — GREGGS_APRON / HIGH_VIS_JACKET halve catch chance.</li>
 *   <li>{@link NotorietySystem} — Tier 2+ pre-emptive police call on entry.</li>
 *   <li>{@link RumourNetwork} — ban rumour seeded on catch.</li>
 *   <li>{@link NewspaperSystem} — 3 thefts → headline.</li>
 *   <li>{@link BusSystem} — DELIVERY_DRIVER arrives by bus at 07:55.</li>
 * </ul>
 */
public class PoundShopSystem {

    // ── Day of week constants ──────────────────────────────────────────────────

    public static final int SUNDAY    = 0;
    public static final int MONDAY    = 1;
    public static final int TUESDAY   = 2;
    public static final int WEDNESDAY = 3;
    public static final int THURSDAY  = 4;
    public static final int FRIDAY    = 5;
    public static final int SATURDAY  = 6;

    // ── Opening hours ──────────────────────────────────────────────────────────

    /** Hour Poundstretcher opens Monday–Saturday (08:30). */
    public static final float OPEN_HOUR_WEEKDAY = 8.5f;

    /** Hour Poundstretcher closes Monday–Saturday (18:30). */
    public static final float CLOSE_HOUR_WEEKDAY = 18.5f;

    /** Hour Poundstretcher opens on Sunday (10:00). */
    public static final float OPEN_HOUR_SUNDAY = 10.0f;

    /** Hour Poundstretcher closes on Sunday (16:00). */
    public static final float CLOSE_HOUR_SUNDAY = 16.0f;

    // ── Stock ──────────────────────────────────────────────────────────────────

    /** Price of each individual own-brand item. */
    public static final int ITEM_PRICE = 1;

    /** Items sold individually by Sharon. */
    public static final Material[] STOCK_ITEMS = {
        Material.OWN_BRAND_CRISPS,
        Material.OWN_BRAND_COLA,
        Material.WASHING_POWDER,
        Material.PARACETAMOL,
        Material.TIN_OF_BEANS,
        Material.DODGY_PASTY,
        Material.BARGAIN_BUCKET_CRISPS
    };

    // ── Bulk buy ───────────────────────────────────────────────────────────────

    /** Cost of the bulk-buy deal. */
    public static final int BULK_BUY_PRICE = 5;

    /** Number of items in the bulk-buy deal. */
    public static final int BULK_BUY_COUNT = 6;

    // ── Shoplifting catch chances ──────────────────────────────────────────────

    /** Base catch chance via CCTV (0–1). */
    public static final float CCTV_CATCH_CHANCE = 0.30f;

    /** Catch chance when Sharon has line of sight (0–1). */
    public static final float SHARON_LOS_CATCH_CHANCE = 0.75f;

    /** Multiplier applied to catch chance when the player wears a disguise. */
    public static final float DISGUISE_CATCH_MULTIPLIER = 0.5f;

    // ── Ban system ─────────────────────────────────────────────────────────────

    /** Number of in-game days the player is banned after being caught. */
    public static final int BAN_DURATION_DAYS = 2;

    /** Number of seconds per in-game day (used for ban timer). */
    public static final float IN_GAME_DAY_SECONDS = 1200f;

    // ── Proxy Street Lad ──────────────────────────────────────────────────────

    /** Price markup multiplier when using the Street Lad proxy (50% markup = 1.5×). */
    public static final float PROXY_MARKUP = 1.5f;

    /** Chance the Street Lad scams the player (takes coin without delivering). */
    public static final float PROXY_SCAM_CHANCE = 0.30f;

    // ── Delivery pallet ───────────────────────────────────────────────────────

    /** In-game hour the delivery driver arrives at the loading bay. */
    public static final float DELIVERY_ARRIVAL_HOUR = 8.0f;

    /** In-game hour the bus carrying the delivery driver departs. */
    public static final float DELIVERY_BUS_HOUR = 7.916667f; // 07:55

    /** Seconds the player has to loot the pallet before it is brought inside. */
    public static final float PALLET_LOOT_WINDOW_SECONDS = 120.0f;

    /** Chance the delivery pallet contains WHOLESALE_SPIRITS (0–1). */
    public static final float WHOLESALE_SPIRITS_CHANCE = 0.10f;

    // ── Notoriety / police ─────────────────────────────────────────────────────

    /** Notoriety tier at which Sharon pre-emptively calls police on player entry. */
    public static final int POLICE_CALL_NOTORIETY_TIER = 2;

    /** Seconds the player has to leave before police arrive (escape window). */
    public static final float POLICE_ESCAPE_WINDOW_SECONDS = 20.0f;

    /** Notoriety added when caught shoplifting. */
    public static final int SHOPLIFTING_NOTORIETY = 5;

    // ── Newspaper ─────────────────────────────────────────────────────────────

    /** Number of thefts in one day that triggers a NewspaperSystem headline. */
    public static final int NEWSPAPER_THEFT_THRESHOLD = 3;

    /** Newspaper headline for repeated theft. */
    public static final String NEWSPAPER_HEADLINE =
            "POUNDSTRETCHER STRIPPED — SHARON FURIOUS AS OWN-BRAND THIEF STRIKES AGAIN";

    // ── Sharon's speech lines ─────────────────────────────────────────────────

    public static final String[] SHARON_SPEECH_LINES = {
        "Mind how you go.",
        "Wednesday's the best day for deals.",
        "Don't make a mess.",
        "Everything's a pound or less, love.",
        "If you're not buying, you're browsing.",
        "We've got a special on washing powder today.",
        "I've got eyes everywhere, just so you know.",
        "Don't touch what you're not buying."
    };

    // ── State ──────────────────────────────────────────────────────────────────

    private final Random random;

    /** Whether the player is currently banned from the shop. */
    private boolean banned = false;

    /** Remaining ban time in in-game seconds. */
    private float banTimer = 0f;

    /** Index of the current CCTV blind-spot shelf (0–3); reset each day. */
    private int blindSpotShelfIndex = 0;

    /** Current day index (for detecting day rollover). */
    private int currentDay = -1;

    /** Whether the delivery pallet is currently accessible at the loading bay. */
    private boolean palletAvailable = false;

    /** Remaining time (in-game seconds) before the pallet is brought inside. */
    private float palletTimer = 0f;

    /** Whether the BULK_BUYER achievement has been awarded. */
    private boolean bulkBuyerAwarded = false;

    /** Whether the MIDDLE_MAN achievement has been awarded. */
    private boolean middleManAwarded = false;

    /** Whether the PALLET_PIRATE achievement has been awarded. */
    private boolean palletPirateAwarded = false;

    /** Whether the POUNDSTRETCHER_FIVE_FINGER achievement has been awarded. */
    private boolean poundstretcnerFiveFingerAwarded = false;

    /** Number of thefts on the current in-game day. */
    private int theftsToday = 0;

    /** Whether the newspaper headline has been triggered today. */
    private boolean headlineTriggeredToday = false;

    /** Sharon's current speech line index. */
    private int speechLineIndex = 0;

    /** Whether the police escape window is active. */
    private boolean policeEscapeWindowActive = false;

    /** Remaining time on the police escape window (in-game seconds). */
    private float policeEscapeTimer = 0f;

    // ── Optional system references ─────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WitnessSystem witnessSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private WantedSystem wantedSystem;
    private NewspaperSystem newspaperSystem;
    private DisguiseSystem disguiseSystem;

    // ── Construction ────────────────────────────────────────────────────────────

    public PoundShopSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection setters ────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWitnessSystem(WitnessSystem witnessSystem) {
        this.witnessSystem = witnessSystem;
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

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setNewspaperSystem(NewspaperSystem newspaperSystem) {
        this.newspaperSystem = newspaperSystem;
    }

    public void setDisguiseSystem(DisguiseSystem disguiseSystem) {
        this.disguiseSystem = disguiseSystem;
    }

    // ── Opening hours logic ─────────────────────────────────────────────────────

    /**
     * Returns true if Poundstretcher is open at the given hour on the given day-of-week.
     *
     * @param hour       current in-game hour (0–24)
     * @param dayOfWeek  0=Sunday … 6=Saturday
     */
    public boolean isOpen(float hour, int dayOfWeek) {
        if (dayOfWeek == SUNDAY) {
            return hour >= OPEN_HOUR_SUNDAY && hour < CLOSE_HOUR_SUNDAY;
        }
        return hour >= OPEN_HOUR_WEEKDAY && hour < CLOSE_HOUR_WEEKDAY;
    }

    // ── Per-frame update ────────────────────────────────────────────────────────

    /**
     * Per-frame update. Ticks down ban timer, pallet timer, police escape window,
     * and handles day rollover (resetting daily blind spot and theft count).
     *
     * @param delta     in-game seconds since last frame
     * @param dayOfYear current in-game day-of-year (used for day rollover detection)
     */
    public void update(float delta, int dayOfYear) {
        // Day rollover
        if (dayOfYear != currentDay) {
            currentDay = dayOfYear;
            blindSpotShelfIndex = random.nextInt(4); // 4 shelf crates per side
            theftsToday = 0;
            headlineTriggeredToday = false;
        }

        // Ban timer countdown
        if (banned && banTimer > 0f) {
            banTimer -= delta;
            if (banTimer <= 0f) {
                banned = false;
                banTimer = 0f;
            }
        }

        // Pallet timer countdown
        if (palletAvailable && palletTimer > 0f) {
            palletTimer -= delta;
            if (palletTimer <= 0f) {
                palletAvailable = false;
                palletTimer = 0f;
            }
        }

        // Police escape window countdown
        if (policeEscapeWindowActive && policeEscapeTimer > 0f) {
            policeEscapeTimer -= delta;
            if (policeEscapeTimer <= 0f) {
                policeEscapeWindowActive = false;
                policeEscapeTimer = 0f;
            }
        }
    }

    // ── Shop purchase ─────────────────────────────────────────────────────────

    /**
     * Result of attempting to buy an individual item from Sharon.
     */
    public enum PurchaseResult {
        /** Item purchased successfully. */
        SUCCESS,
        /** Shop is closed right now. */
        CLOSED,
        /** Player is banned from the shop. */
        BANNED,
        /** Player does not have enough COIN. */
        NO_COIN
    }

    /**
     * Player presses E on Sharon to buy one own-brand item for 1 COIN.
     *
     * @param item      the item to purchase (must be in {@link #STOCK_ITEMS})
     * @param inventory player's inventory
     * @param hour      current in-game hour
     * @param dayOfWeek 0=Sunday … 6=Saturday
     * @return purchase result
     */
    public PurchaseResult buyItem(Material item, Inventory inventory, float hour, int dayOfWeek) {
        if (!isOpen(hour, dayOfWeek)) {
            return PurchaseResult.CLOSED;
        }
        if (banned) {
            return PurchaseResult.BANNED;
        }
        if (inventory.getItemCount(Material.COIN) < ITEM_PRICE) {
            return PurchaseResult.NO_COIN;
        }

        inventory.removeItem(Material.COIN, ITEM_PRICE);
        inventory.addItem(item, 1);
        return PurchaseResult.SUCCESS;
    }

    // ── Bulk buy ───────────────────────────────────────────────────────────────

    /**
     * Result of attempting the bulk-buy deal.
     */
    public enum BulkBuyResult {
        /** Deal completed — 6 items added to inventory. */
        SUCCESS,
        /** Shop is closed. */
        CLOSED,
        /** Player is banned. */
        BANNED,
        /** Player does not have 5 COIN. */
        NO_COIN
    }

    /**
     * Player presses E on Sharon to trigger the bulk-buy deal: 5 COIN for 6 random items.
     * Awards {@link AchievementType#BULK_BUYER} on first use.
     *
     * @param inventory      player's inventory
     * @param hour           current in-game hour
     * @param dayOfWeek      0=Sunday … 6=Saturday
     * @param achievementCb  callback for awarding achievements (may be null)
     * @return bulk-buy result
     */
    public BulkBuyResult buyBulkDeal(Inventory inventory, float hour, int dayOfWeek,
                                      NotorietySystem.AchievementCallback achievementCb) {
        if (!isOpen(hour, dayOfWeek)) {
            return BulkBuyResult.CLOSED;
        }
        if (banned) {
            return BulkBuyResult.BANNED;
        }
        if (inventory.getItemCount(Material.COIN) < BULK_BUY_PRICE) {
            return BulkBuyResult.NO_COIN;
        }

        inventory.removeItem(Material.COIN, BULK_BUY_PRICE);
        for (int i = 0; i < BULK_BUY_COUNT; i++) {
            inventory.addItem(STOCK_ITEMS[random.nextInt(STOCK_ITEMS.length)], 1);
        }

        if (!bulkBuyerAwarded) {
            bulkBuyerAwarded = true;
            if (achievementCb != null) {
                achievementCb.award(AchievementType.BULK_BUYER);
            }
        }

        return BulkBuyResult.SUCCESS;
    }

    // ── Shoplifting ───────────────────────────────────────────────────────────

    /**
     * Result of attempting to shoplift from a shelf CRATE_PROP.
     */
    public enum ShopliftResult {
        /** Item taken successfully — not caught. */
        SUCCESS,
        /** Caught by CCTV / Sharon — banned, crime recorded. */
        CAUGHT,
        /** Caught specifically because Sharon had line of sight. */
        CAUGHT_BY_SHARON,
        /** Shop is closed. */
        CLOSED,
        /** Player is already banned. */
        BANNED
    }

    /**
     * Player attempts to steal one item from a shelf CRATE_PROP.
     *
     * <p>Catch chance logic:
     * <ul>
     *   <li>If Sharon has LOS: {@link #SHARON_LOS_CATCH_CHANCE} (75%).</li>
     *   <li>Otherwise CCTV base: {@link #CCTV_CATCH_CHANCE} (30%).</li>
     *   <li>At the blind-spot shelf ({@code shelfIndex == blindSpotShelfIndex}):
     *       CCTV chance drops to 0% (Sharon LOS still applies).</li>
     *   <li>Wearing GREGGS_APRON or HIGH_VIS_JACKET: catch chance halved.</li>
     * </ul>
     *
     * @param inventory      player's inventory
     * @param hour           current in-game hour
     * @param dayOfWeek      0=Sunday … 6=Saturday
     * @param sharonHasLOS   true if Sharon can see the player
     * @param shelfIndex     which shelf (0–3) the player is stealing from
     * @param npcs           nearby NPCs (for rumour seeding)
     * @param achievementCb  callback for achievements (may be null)
     * @return shoplift result
     */
    public ShopliftResult attemptShoplift(
            Inventory inventory, float hour, int dayOfWeek,
            boolean sharonHasLOS, int shelfIndex,
            List<NPC> npcs,
            NotorietySystem.AchievementCallback achievementCb) {

        if (!isOpen(hour, dayOfWeek)) {
            return ShopliftResult.CLOSED;
        }
        if (banned) {
            return ShopliftResult.BANNED;
        }

        // Determine catch chance
        float catchChance;
        if (sharonHasLOS) {
            catchChance = SHARON_LOS_CATCH_CHANCE;
        } else {
            // CCTV blind spot: no CCTV catch chance
            catchChance = (shelfIndex == blindSpotShelfIndex) ? 0f : CCTV_CATCH_CHANCE;
        }

        // Disguise halves catch chance
        if (disguiseSystem != null && disguiseSystem.isDisguised()) {
            catchChance *= DISGUISE_CATCH_MULTIPLIER;
        }

        if (random.nextFloat() < catchChance) {
            // Caught
            applyBan(npcs);
            if (achievementCb != null) {
                achievementCb.award(AchievementType.EVERY_POUNDS_A_PRISONER);
                if (sharonHasLOS) {
                    achievementCb.award(AchievementType.SHARON_KNOWS);
                }
            }
            if (sharonHasLOS) {
                return ShopliftResult.CAUGHT_BY_SHARON;
            }
            return ShopliftResult.CAUGHT;
        }

        // Success — give a random stock item
        inventory.addItem(STOCK_ITEMS[random.nextInt(STOCK_ITEMS.length)], 1);
        theftsToday++;

        if (achievementCb != null && !poundstretcnerFiveFingerAwarded) {
            poundstretcnerFiveFingerAwarded = true;
            achievementCb.award(AchievementType.POUNDSTRETCHER_FIVE_FINGER);
        }

        // Check newspaper threshold
        if (!headlineTriggeredToday && theftsToday >= NEWSPAPER_THEFT_THRESHOLD) {
            headlineTriggeredToday = true;
            if (newspaperSystem != null) {
                newspaperSystem.recordEvent(
                        new NewspaperSystem.InfamyEvent(
                                "POUND_SHOP_THEFT",
                                "Poundstretcher",
                                Material.OWN_BRAND_CRISPS,
                                "Sharon",
                                0,
                                null,
                                null,
                                5
                        )
                );
            }
        }

        return ShopliftResult.SUCCESS;
    }

    // ── Ban system ────────────────────────────────────────────────────────────

    /** Applies a 2-day ban and records the shoplifting crime. */
    private void applyBan(List<NPC> npcs) {
        banned = true;
        banTimer = BAN_DURATION_DAYS * IN_GAME_DAY_SECONDS;

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.SHOPLIFTING);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(SHOPLIFTING_NOTORIETY, null);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(1, 0f, 0f, 0f, type -> {});
        }

        // Seed ban rumour
        if (rumourNetwork != null && npcs != null) {
            NPC target = findPublicNPC(npcs);
            if (target != null) {
                rumourNetwork.addRumour(target, new Rumour(
                        RumourType.SHOPLIFTING_BAN,
                        "Sharon's banned someone from the Poundstretcher — saw it happen myself."
                ));
            }
        }
    }

    // ── Proxy Street Lad purchase ─────────────────────────────────────────────

    /**
     * Result of using a Street Lad proxy while banned.
     */
    public enum ProxyResult {
        /** Street Lad delivered the item. */
        SUCCESS,
        /** Street Lad scammed the player — took coin but no delivery. */
        SCAMMED,
        /** Player is not banned (proxy not needed). */
        NOT_BANNED,
        /** Player does not have enough COIN (item price × 1.5, rounded up). */
        NO_COIN
    }

    /**
     * While banned, the player pays a Street Lad proxy to buy one item at 50% markup.
     * 30% chance the Street Lad scams them (takes coin, delivers nothing).
     * Awards {@link AchievementType#MIDDLE_MAN} on first successful proxy use.
     *
     * @param item          the item to obtain via proxy
     * @param inventory     player's inventory
     * @param achievementCb callback for achievements (may be null)
     * @return proxy result
     */
    public ProxyResult useStreetLadProxy(Material item, Inventory inventory,
                                          NotorietySystem.AchievementCallback achievementCb) {
        if (!banned) {
            return ProxyResult.NOT_BANNED;
        }

        int proxyCost = (int) Math.ceil(ITEM_PRICE * PROXY_MARKUP);
        if (inventory.getItemCount(Material.COIN) < proxyCost) {
            return ProxyResult.NO_COIN;
        }

        inventory.removeItem(Material.COIN, proxyCost);

        if (random.nextFloat() < PROXY_SCAM_CHANCE) {
            return ProxyResult.SCAMMED;
        }

        inventory.addItem(item, 1);

        if (!middleManAwarded) {
            middleManAwarded = true;
            if (achievementCb != null) {
                achievementCb.award(AchievementType.MIDDLE_MAN);
            }
        }

        return ProxyResult.SUCCESS;
    }

    // ── Delivery pallet ───────────────────────────────────────────────────────

    /**
     * Called when the daily delivery driver arrives at 08:00 with the pallet.
     * Spawns the pallet and starts the loot window timer.
     */
    public void onDeliveryArrival() {
        palletAvailable = true;
        palletTimer = PALLET_LOOT_WINDOW_SECONDS;
    }

    /**
     * Result of attempting to loot the delivery pallet.
     */
    public enum PalletLootResult {
        /** Item looted from the pallet. */
        SUCCESS,
        /** Looted WHOLESALE_SPIRITS (special prize). */
        SUCCESS_SPIRITS,
        /** No pallet available (not delivery time or window expired). */
        NO_PALLET
    }

    /**
     * Player presses E on the {@code PALLET_PROP} to loot one item.
     * {@link #WHOLESALE_SPIRITS_CHANCE} (10%) chance of {@link Material#WHOLESALE_SPIRITS}.
     * Awards {@link AchievementType#PALLET_PIRATE} on first successful loot.
     *
     * @param inventory     player's inventory
     * @param achievementCb callback for achievements (may be null)
     * @return pallet loot result
     */
    public PalletLootResult lootPallet(Inventory inventory,
                                        NotorietySystem.AchievementCallback achievementCb) {
        if (!palletAvailable) {
            return PalletLootResult.NO_PALLET;
        }

        palletAvailable = false;
        palletTimer = 0f;

        if (!palletPirateAwarded) {
            palletPirateAwarded = true;
            if (achievementCb != null) {
                achievementCb.award(AchievementType.PALLET_PIRATE);
            }
        }

        if (random.nextFloat() < WHOLESALE_SPIRITS_CHANCE) {
            inventory.addItem(Material.WHOLESALE_SPIRITS, 1);
            return PalletLootResult.SUCCESS_SPIRITS;
        }

        inventory.addItem(STOCK_ITEMS[random.nextInt(STOCK_ITEMS.length)], 1);
        return PalletLootResult.SUCCESS;
    }

    // ── Notoriety / police pre-emption ─────────────────────────────────────────

    /**
     * Called when the player enters the shop. At Notoriety Tier 2+, Sharon
     * pre-emptively calls the police. The player has a
     * {@link #POLICE_ESCAPE_WINDOW_SECONDS}-second escape window.
     *
     * @return true if the police have been called (escape window started)
     */
    public boolean onPlayerEntry() {
        if (notorietySystem != null && notorietySystem.getTier() >= POLICE_CALL_NOTORIETY_TIER) {
            policeEscapeWindowActive = true;
            policeEscapeTimer = POLICE_ESCAPE_WINDOW_SECONDS;
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, 0f, 0f, 0f, type -> {});
            }
            return true;
        }
        return false;
    }

    // ── Sharon speech ─────────────────────────────────────────────────────────

    /**
     * Returns Sharon's next rotating speech line.
     *
     * @return speech line string
     */
    public String nextSpeechLine() {
        String line = SHARON_SPEECH_LINES[speechLineIndex];
        speechLineIndex = (speechLineIndex + 1) % SHARON_SPEECH_LINES.length;
        return line;
    }

    // ── CCTV blind spot ───────────────────────────────────────────────────────

    /**
     * Returns the current day's CCTV blind-spot shelf index (0–3).
     *
     * @return blind spot shelf index
     */
    public int getBlindSpotShelfIndex() {
        return blindSpotShelfIndex;
    }

    /**
     * Computes the effective catch chance for shoplifting at a given shelf.
     *
     * @param sharonHasLOS  true if Sharon can see the player
     * @param shelfIndex    the shelf being targeted (0–3)
     * @param isDisguised   true if the player is wearing a disguise
     * @return effective catch chance (0.0–1.0)
     */
    public float computeCatchChance(boolean sharonHasLOS, int shelfIndex, boolean isDisguised) {
        float catchChance;
        if (sharonHasLOS) {
            catchChance = SHARON_LOS_CATCH_CHANCE;
        } else {
            catchChance = (shelfIndex == blindSpotShelfIndex) ? 0f : CCTV_CATCH_CHANCE;
        }
        if (isDisguised) {
            catchChance *= DISGUISE_CATCH_MULTIPLIER;
        }
        return catchChance;
    }

    // ── Utility helpers ────────────────────────────────────────────────────────

    private NPC findPublicNPC(List<NPC> npcs) {
        for (NPC npc : npcs) {
            if (npc != null && (npc.getType() == NPCType.PUBLIC
                    || npc.getType() == NPCType.PENSIONER)) {
                return npc;
            }
        }
        return npcs.isEmpty() ? null : npcs.get(0);
    }

    // ── Accessors for testing ──────────────────────────────────────────────────

    /** Returns whether the player is currently banned. */
    public boolean isBanned() {
        return banned;
    }

    /** Returns the remaining ban time in in-game seconds. */
    public float getBanTimer() {
        return banTimer;
    }

    /** Returns whether the delivery pallet is currently available to loot. */
    public boolean isPalletAvailable() {
        return palletAvailable;
    }

    /** Returns the remaining pallet loot window in in-game seconds. */
    public float getPalletTimer() {
        return palletTimer;
    }

    /** Returns whether the police escape window is currently active. */
    public boolean isPoliceEscapeWindowActive() {
        return policeEscapeWindowActive;
    }

    /** Returns the number of thefts recorded today. */
    public int getTheftsToday() {
        return theftsToday;
    }

    /** Returns whether the newspaper headline has been triggered today. */
    public boolean isHeadlineTriggeredToday() {
        return headlineTriggeredToday;
    }

    // ── Test helpers ──────────────────────────────────────────────────────────

    /** Force-set ban state for testing. */
    public void setBannedForTesting(boolean banned, float banTimerSeconds) {
        this.banned = banned;
        this.banTimer = banTimerSeconds;
    }

    /** Force-set blind spot shelf index for testing. */
    public void setBlindSpotShelfIndexForTesting(int index) {
        this.blindSpotShelfIndex = index;
    }

    /** Force-set pallet availability for testing. */
    public void setPalletAvailableForTesting(boolean available, float timerSeconds) {
        this.palletAvailable = available;
        this.palletTimer = timerSeconds;
    }

    /** Force-set thefts today for testing. */
    public void setTheftsTodayForTesting(int count) {
        this.theftsToday = count;
    }

    /** Force-set bulk-buyer awarded state for testing. */
    public void setBulkBuyerAwardedForTesting(boolean awarded) {
        this.bulkBuyerAwarded = awarded;
    }
}

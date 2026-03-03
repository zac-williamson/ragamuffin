package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.core.NotorietySystem.AchievementCallback;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1363: Northfield Sunday Car Boot Sale — The 6am Rush, the Dodgy
 * Vendor &amp; the Stolen Goods Sting.
 *
 * <h3>Overview</h3>
 * Every Sunday morning (06:00–12:00) the council car park transforms into a
 * car boot sale. Barry ({@link NPCType#BOOT_SALE_ORGANISER}) collects pitch
 * fees (3 COIN) from vendors before 06:15. 4–8
 * {@link NPCType#BOOT_SALE_VENDOR} NPCs sell junk from folding tables and open
 * car boots. 6–12 {@link NPCType#BOOT_SALE_PUNTER} NPCs compete with the
 * player for items.
 *
 * <h3>Mechanic 1 — Buying</h3>
 * Browse tables (press E on {@code BOOT_SALE_TABLE_PROP}), haggle for 70%
 * price ({@link #HAGGLE_PRICE_RATIO}) with 50% acceptance
 * ({@link #HAGGLE_ACCEPT_CHANCE}). Vendor becomes VENDOR_DISPUTE hostile after
 * 3 failed haggles ({@link #HAGGLE_FAIL_LIMIT}). Time-of-day price decay:
 * −10%/hr after 07:00 ({@link #PRICE_DECAY_PER_HOUR}); −40% in wind-down
 * (10:00–12:00, {@link #WIND_DOWN_DISCOUNT}). 5% gem-find chance
 * ({@link #GEM_FIND_CHANCE}) for BETAMAX_PLAYER/FONDUE_SET:
 * {@link AchievementType#BOOT_SALE_TREASURE}.
 *
 * <h3>Mechanic 2 — Selling</h3>
 * Pay 3 COIN pitch fee ({@link #PITCH_FEE}) to Barry before 06:15. Place up
 * to 8 items ({@link #MAX_PITCH_ITEMS}) on pitch. Punters auto-buy every
 * 2 in-game minutes ({@link #PUNTER_BUY_INTERVAL_MINUTES}). Stolen goods on
 * display risk Trading Standards detection. Selling all 8 items unlocks
 * {@link AchievementType#BOOT_SALE_SELLER}.
 *
 * <h3>Mechanic 3 — Derek's back-stock</h3>
 * {@link NPCType#DODGY_VENDOR} Derek offers stolen goods at Street Rep ≥ 30
 * ({@link #DEREK_REP_GATE}). Buy/sell at 50% fence value
 * ({@link #DEREK_FENCE_RATIO}) with no notoriety unless
 * {@code NEIGHBOURHOOD_WATCH} NPC is nearby.
 *
 * <h3>Mechanic 4 — Early-bird rush</h3>
 * Sprint to a table within 30 seconds of 06:00
 * ({@link #EARLY_BIRD_WINDOW_SECONDS}) for −15% first-pick discount
 * ({@link #EARLY_BIRD_DISCOUNT}). Unlocks {@link AchievementType#FIRST_PICK}.
 *
 * <h3>Events</h3>
 * <ul>
 *   <li>{@link EventType#TRADING_STANDARDS_STING} — 3+ stolen items sold →
 *       {@link CrimeType#TRADING_STANDARDS_BUST} + Notoriety +10 +
 *       NewspaperSystem headline.</li>
 *   <li>{@link EventType#VENDOR_DISPUTE} — 3 failed haggles → potential brawl.</li>
 *   <li>{@link EventType#RAIN_STOPS_PLAY} — HEAVY_RAIN/THUNDERSTORM cancels event.</li>
 *   <li>{@link EventType#DOG_LEADS_CHAOS} — dog companion causes stall chaos.</li>
 *   <li>{@link EventType#CLIVE_CLEARS_PITCH} — TrafficWarden evicts after noon.</li>
 * </ul>
 *
 * <h3>Weather</h3>
 * THUNDERSTORM or HEAVY_RAIN cancels the event. FROST reduces vendor count to 4
 * ({@link #FROST_VENDOR_COUNT}).
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@link AchievementType#BOOT_SALE_TREASURE} — gem-find on BETAMAX or FONDUE_SET</li>
 *   <li>{@link AchievementType#BOOT_SALE_SELLER} — sell all 8 pitch items</li>
 *   <li>{@link AchievementType#FIRST_PICK} — sprint to table in early-bird window</li>
 *   <li>{@link AchievementType#BOOT_SALE_HUSTLER} — 5 successful haggles (tracked)</li>
 * </ul>
 */
public class CarBootSaleSystem {

    // ── Day-of-week constant ──────────────────────────────────────────────────

    /** Sunday day-of-week value (dayCount % 7 == 6). */
    public static final int SUNDAY = 6;

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Event start hour (06:00). */
    public static final float OPEN_HOUR = 6.0f;

    /** Event end hour (12:00). */
    public static final float CLOSE_HOUR = 12.0f;

    /** Last time to pay pitch fee and register as vendor (06:15). */
    public static final float PITCH_FEE_DEADLINE = 6.25f;  // 06:15

    /** Wind-down discount start hour (10:00). */
    public static final float WIND_DOWN_HOUR = 10.0f;

    // ── Vendor / punter counts ────────────────────────────────────────────────

    /** Minimum number of BOOT_SALE_VENDOR NPCs spawned normally. */
    public static final int MIN_VENDORS = 4;

    /** Maximum number of BOOT_SALE_VENDOR NPCs spawned normally. */
    public static final int MAX_VENDORS = 8;

    /** Number of vendors spawned when weather is FROST. */
    public static final int FROST_VENDOR_COUNT = 4;

    /** Minimum number of BOOT_SALE_PUNTER NPCs spawned. */
    public static final int MIN_PUNTERS = 6;

    /** Maximum number of BOOT_SALE_PUNTER NPCs spawned. */
    public static final int MAX_PUNTERS = 12;

    // ── Pitch selling ─────────────────────────────────────────────────────────

    /** Pitch fee paid to Barry (in COIN). */
    public static final int PITCH_FEE = 3;

    /** Maximum number of items player can display on their pitch. */
    public static final int MAX_PITCH_ITEMS = 8;

    /** Interval (in-game minutes) between punter auto-buy ticks on player pitch. */
    public static final float PUNTER_BUY_INTERVAL_MINUTES = 2.0f;

    /** Number of openly displayed stolen items that triggers TRADING_STANDARDS_STING. */
    public static final int STOLEN_GOODS_STING_THRESHOLD = 3;

    /** Notoriety added when TRADING_STANDARDS_STING fires. */
    public static final int STING_NOTORIETY = 10;

    // ── Buying / haggling ─────────────────────────────────────────────────────

    /** Haggle price as a fraction of base price (0.70 = 70%). */
    public static final float HAGGLE_PRICE_RATIO = 0.70f;

    /** Probability vendor accepts a haggle attempt (0.50 = 50%). */
    public static final float HAGGLE_ACCEPT_CHANCE = 0.50f;

    /** Number of failed haggles before VENDOR_DISPUTE hostile state. */
    public static final int HAGGLE_FAIL_LIMIT = 3;

    /** Price decay rate per in-game hour after 07:00 (0.10 = 10%/hr). */
    public static final float PRICE_DECAY_PER_HOUR = 0.10f;

    /** Flat discount applied during wind-down phase 10:00–12:00 (0.40 = 40%). */
    public static final float WIND_DOWN_DISCOUNT = 0.40f;

    // ── Early-bird rush ───────────────────────────────────────────────────────

    /** Window (real seconds) after 06:00 in which early-bird discount applies. */
    public static final float EARLY_BIRD_WINDOW_SECONDS = 30.0f;

    /** Price discount for early-bird first-pick purchase (0.15 = 15% off). */
    public static final float EARLY_BIRD_DISCOUNT = 0.15f;

    // ── Gem-find ─────────────────────────────────────────────────────────────

    /** Probability of finding a hidden gem inside BETAMAX_PLAYER or FONDUE_SET. */
    public static final float GEM_FIND_CHANCE = 0.05f;

    // ── Derek's back-stock ────────────────────────────────────────────────────

    /** Minimum Street Reputation required to access Derek's stolen goods. */
    public static final int DEREK_REP_GATE = 30;

    /** Derek's buy/sell ratio relative to full fence value (0.50 = 50%). */
    public static final float DEREK_FENCE_RATIO = 0.50f;

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Result of attempting to pay a pitch fee. */
    public enum PitchFeeResult {
        /** Fee paid successfully; player registered as vendor. */
        PAID,
        /** Player does not have enough COIN. */
        INSUFFICIENT_FUNDS,
        /** Deadline has passed (after 06:15). */
        TOO_LATE,
        /** Event is not active (not Sunday or wrong hour). */
        EVENT_NOT_ACTIVE
    }

    /** Result of a haggle attempt. */
    public enum HaggleResult {
        /** Vendor accepted the haggle; item purchased at 70% price. */
        ACCEPTED,
        /** Vendor refused the haggle; item still available at full price. */
        REFUSED,
        /** Player has hit the fail limit; vendor is now hostile (VENDOR_DISPUTE). */
        VENDOR_HOSTILE,
        /** Event is not active or no item to haggle. */
        NOT_AVAILABLE
    }

    /** Result of placing an item on the player's pitch. */
    public enum PlaceItemResult {
        /** Item placed successfully. */
        PLACED,
        /** Pitch is full (8 items maximum). */
        PITCH_FULL,
        /** Player has not paid the pitch fee. */
        NO_PITCH,
        /** Player does not have the item in inventory. */
        NOT_IN_INVENTORY
    }

    /** Result of buying from Derek's dodgy back-stock. */
    public enum DerekBuyResult {
        /** Item purchased at 50% fence value. */
        PURCHASED,
        /** Player's Street Rep is below 30. */
        REP_TOO_LOW,
        /** Neighbourhood Watch is nearby; Derek won't deal. */
        WATCH_NEARBY,
        /** Player has insufficient COIN. */
        INSUFFICIENT_FUNDS,
        /** Event not active or Derek not present. */
        NOT_AVAILABLE
    }

    /** Car boot sale active events. */
    public enum EventType {
        /** Trading Standards Officer busts player for selling 3+ stolen goods. */
        TRADING_STANDARDS_STING,
        /** Vendor dispute after 3+ failed haggles escalates to brawl. */
        VENDOR_DISPUTE,
        /** Heavy rain or thunderstorm cancels the event. */
        RAIN_STOPS_PLAY,
        /** Dog companion knocks over stall causing chaos. */
        DOG_LEADS_CHAOS,
        /** Traffic warden Clive evicts remaining vendors after noon. */
        CLIVE_CLEARS_PITCH
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Whether the event is currently active (Sunday 06:00–12:00). */
    private boolean eventActive = false;

    /** Whether the player has paid the pitch fee this session. */
    private boolean pitchFeePaid = false;

    /** Number of items currently on the player's pitch. */
    private int pitchItemCount = 0;

    /** Number of items the player has sold from their pitch this session. */
    private int pitchItemsSold = 0;

    /** Number of openly displayed stolen goods on the player's pitch. */
    private int stolenGoodsOnDisplay = 0;

    /** Whether the TRADING_STANDARDS_STING has already fired this session. */
    private boolean stingFired = false;

    /** Cumulative failed haggles at the current vendor interaction. */
    private int haggleFailCount = 0;

    /** Cumulative successful haggles (across sessions, for BOOT_SALE_HUSTLER). */
    private int hagglesSucceeded = 0;

    /** Whether the early-bird discount has been applied this session. */
    private boolean earlyBirdApplied = false;

    /** Whether the BOOT_SALE_TREASURE achievement has already been awarded. */
    private boolean treasureAwarded = false;

    /** Whether the FIRST_PICK achievement has already been awarded. */
    private boolean firstPickAwarded = false;

    /** Whether the BOOT_SALE_SELLER achievement has already been awarded. */
    private boolean sellerAwarded = false;

    /** Elapsed real seconds since event start (for early-bird window). */
    private float secondsSinceOpen = 0.0f;

    /** Accumulated minutes since last punter auto-buy tick. */
    private float minutesSincePunterBuy = 0.0f;

    // ── Integrated systems (optional wiring) ─────────────────────────────────

    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;

    // ── Construction ──────────────────────────────────────────────────────────

    public CarBootSaleSystem() {
        this(new Random());
    }

    public CarBootSaleSystem(Random random) {
        this.random = random;
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem n) {
        this.notorietySystem = n;
    }

    public void setCriminalRecord(CriminalRecord c) {
        this.criminalRecord = c;
    }

    public void setRumourNetwork(RumourNetwork r) {
        this.rumourNetwork = r;
    }

    // ── Event lifecycle ───────────────────────────────────────────────────────

    /**
     * Returns whether the car boot sale is active given the current day-of-week
     * and in-game hour.
     *
     * @param dayOfWeek  dayCount % 7 (6 = Sunday)
     * @param hour       current in-game hour (e.g. 6.0 = 06:00)
     * @return true if the event is/should be active
     */
    public boolean isEventDay(int dayOfWeek, float hour) {
        return dayOfWeek == SUNDAY && hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Open the event for this Sunday session. Call when Sunday 06:00 is reached
     * and weather is acceptable. Resets all session state.
     *
     * @param weatherCancels true if THUNDERSTORM or HEAVY_RAIN is present
     *                       (cancels event, fires RAIN_STOPS_PLAY)
     * @return the event that fired, or null if no notable event
     */
    public EventType openEvent(boolean weatherCancels) {
        if (weatherCancels) {
            eventActive = false;
            return EventType.RAIN_STOPS_PLAY;
        }
        eventActive = true;
        pitchFeePaid = false;
        pitchItemCount = 0;
        pitchItemsSold = 0;
        stolenGoodsOnDisplay = 0;
        stingFired = false;
        haggleFailCount = 0;
        earlyBirdApplied = false;
        secondsSinceOpen = 0.0f;
        minutesSincePunterBuy = 0.0f;
        return null;
    }

    /**
     * Close the event (noon reached or forced closure). Fires CLIVE_CLEARS_PITCH.
     *
     * @return EventType.CLIVE_CLEARS_PITCH
     */
    public EventType closeEvent() {
        eventActive = false;
        return EventType.CLIVE_CLEARS_PITCH;
    }

    /**
     * Per-frame update. Advances early-bird window timer and punter auto-buy
     * timer. Call each frame while event is active.
     *
     * @param deltaSec   elapsed real seconds since last call
     * @param deltaMinutes elapsed in-game minutes since last call
     * @param pitchItemValues base COIN values of items on player pitch (may be null)
     * @param playerInventory player inventory (receives COIN on punter buy)
     * @return COIN earned from punter auto-buy this tick (0 if no purchase)
     */
    public int update(float deltaSec, float deltaMinutes,
                      int[] pitchItemValues, Inventory playerInventory) {
        if (!eventActive) return 0;

        secondsSinceOpen += deltaSec;
        minutesSincePunterBuy += deltaMinutes;

        int coinEarned = 0;
        if (minutesSincePunterBuy >= PUNTER_BUY_INTERVAL_MINUTES && pitchItemCount > 0) {
            minutesSincePunterBuy = 0f;
            // A punter buys one item from the pitch
            int value = (pitchItemValues != null && pitchItemValues.length > 0)
                    ? pitchItemValues[random.nextInt(Math.min(pitchItemCount, pitchItemValues.length))]
                    : 1;
            coinEarned = Math.max(1, value);
            if (playerInventory != null) {
                playerInventory.addItem(Material.COIN, coinEarned);
            }
            pitchItemCount = Math.max(0, pitchItemCount - 1);
            pitchItemsSold++;
        }
        return coinEarned;
    }

    // ── Pitch fee payment ─────────────────────────────────────────────────────

    /**
     * Player pays the 3 COIN pitch fee to Barry before 06:15.
     *
     * @param inventory  player's inventory
     * @param currentHour current in-game hour
     * @return result of the fee payment attempt
     */
    public PitchFeeResult payPitchFee(Inventory inventory, float currentHour) {
        if (!eventActive) return PitchFeeResult.EVENT_NOT_ACTIVE;
        if (currentHour >= PITCH_FEE_DEADLINE) return PitchFeeResult.TOO_LATE;
        if (inventory.getItemCount(Material.COIN) < PITCH_FEE) {
            return PitchFeeResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, PITCH_FEE);
        pitchFeePaid = true;
        return PitchFeeResult.PAID;
    }

    // ── Selling mechanics ─────────────────────────────────────────────────────

    /**
     * Place one item from the player's inventory onto their pitch.
     *
     * @param material       the material to place
     * @param isStolen       whether this item is stolen (adds to stolenGoodsOnDisplay)
     * @param inventory      player's inventory
     * @param callback       achievement callback
     * @return result of the placement attempt
     */
    public PlaceItemResult placeItemOnPitch(Material material, boolean isStolen,
                                            Inventory inventory,
                                            AchievementCallback callback) {
        if (!pitchFeePaid) return PlaceItemResult.NO_PITCH;
        if (pitchItemCount >= MAX_PITCH_ITEMS) return PlaceItemResult.PITCH_FULL;
        if (!inventory.hasItem(material)) return PlaceItemResult.NOT_IN_INVENTORY;

        inventory.removeItem(material, 1);
        pitchItemCount++;
        if (isStolen) {
            stolenGoodsOnDisplay++;
        }
        return PlaceItemResult.PLACED;
    }

    /**
     * Check whether the Trading Standards Sting event should fire. Call after
     * each stolen item is placed on the pitch.
     *
     * @param witnessNpc     an NPC near the player (for rumour seeding; may be null)
     * @param callback       achievement callback
     * @return EventType.TRADING_STANDARDS_STING if the sting fires, null otherwise
     */
    public EventType checkTradingStandardsSting(NPC witnessNpc,
                                                 AchievementCallback callback) {
        if (stingFired || stolenGoodsOnDisplay < STOLEN_GOODS_STING_THRESHOLD) {
            return null;
        }
        stingFired = true;

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.TRADING_STANDARDS_BUST);
        }
        if (notorietySystem != null && callback != null) {
            notorietySystem.addNotoriety(STING_NOTORIETY, callback);
        }
        if (rumourNetwork != null && witnessNpc != null) {
            rumourNetwork.addRumour(witnessNpc,
                new Rumour(RumourType.STOLEN_GOODS_MARKET,
                    "Word is someone's been flogging knocked-off gear down the " +
                    "car boot — Trading Standards were sniffing round on Sunday."));
        }
        return EventType.TRADING_STANDARDS_STING;
    }

    /**
     * Check whether the BOOT_SALE_SELLER achievement should fire (all 8 pitch
     * items sold). Returns true and awards the achievement on first trigger.
     *
     * @param callback achievement callback
     * @return true if the achievement was newly awarded
     */
    public boolean checkSellerAchievement(AchievementCallback callback) {
        if (!sellerAwarded && pitchItemsSold >= MAX_PITCH_ITEMS) {
            sellerAwarded = true;
            if (callback != null) {
                callback.award(AchievementType.BOOT_SALE_SELLER);
            }
            return true;
        }
        return false;
    }

    // ── Buying mechanics ──────────────────────────────────────────────────────

    /**
     * Calculate the effective price of an item at the current hour, accounting
     * for time-of-day decay and early-bird discount.
     *
     * @param basePrice   base COIN value of the item
     * @param currentHour current in-game hour
     * @param earlyBird   true if the player qualifies for the early-bird discount
     * @return effective COIN price (minimum 1)
     */
    public int calculatePrice(int basePrice, float currentHour, boolean earlyBird) {
        float price = basePrice;

        if (earlyBird) {
            price *= (1.0f - EARLY_BIRD_DISCOUNT);
        } else if (currentHour >= WIND_DOWN_HOUR) {
            price *= (1.0f - WIND_DOWN_DISCOUNT);
        } else if (currentHour > 7.0f) {
            float hoursAfterSeven = currentHour - 7.0f;
            float decay = Math.min(hoursAfterSeven * PRICE_DECAY_PER_HOUR, 0.30f);
            price *= (1.0f - decay);
        }

        return Math.max(1, Math.round(price));
    }

    /**
     * Returns whether the player qualifies for the early-bird discount.
     * True if within {@link #EARLY_BIRD_WINDOW_SECONDS} of event open time.
     *
     * @return true if early-bird window is still active
     */
    public boolean isEarlyBirdWindowOpen() {
        return eventActive && secondsSinceOpen <= EARLY_BIRD_WINDOW_SECONDS;
    }

    /**
     * Attempt to buy an item from a BOOT_SALE_VENDOR at the calculated price.
     * Applies early-bird discount if within the 30-second window.
     *
     * @param material      the material to buy
     * @param basePrice     base COIN value
     * @param currentHour   current in-game hour
     * @param isGemFind     true if this item has gem-find chance (BETAMAX/FONDUE_SET)
     * @param inventory     player's inventory
     * @param callback      achievement callback
     * @return COIN spent (positive), or -1 if purchase failed (insufficient funds)
     */
    public int buyFromVendor(Material material, int basePrice, float currentHour,
                             boolean isGemFind, Inventory inventory,
                             AchievementCallback callback) {
        if (!eventActive) return -1;

        boolean earlyBird = isEarlyBirdWindowOpen() && !earlyBirdApplied;
        int price = calculatePrice(basePrice, currentHour, earlyBird);

        if (inventory.getItemCount(Material.COIN) < price) return -1;

        inventory.removeItem(Material.COIN, price);
        inventory.addItem(material, 1);

        // Award FIRST_PICK on first early-bird purchase
        if (earlyBird) {
            earlyBirdApplied = true;
            if (!firstPickAwarded && callback != null) {
                firstPickAwarded = true;
                callback.award(AchievementType.FIRST_PICK);
            }
        }

        // Gem-find check for BETAMAX_PLAYER / FONDUE_SET
        if (isGemFind && random.nextFloat() < GEM_FIND_CHANCE) {
            inventory.addItem(Material.COIN, 2); // hidden cash bonus
            if (!treasureAwarded && callback != null) {
                treasureAwarded = true;
                callback.award(AchievementType.BOOT_SALE_TREASURE);
            }
        }

        return price;
    }

    // ── Haggling mechanics ────────────────────────────────────────────────────

    /**
     * Attempt to haggle an item down to 70% price.
     *
     * @param callback achievement callback
     * @return result of the haggle attempt
     */
    public HaggleResult haggle(AchievementCallback callback) {
        if (!eventActive) return HaggleResult.NOT_AVAILABLE;

        if (haggleFailCount >= HAGGLE_FAIL_LIMIT) {
            return HaggleResult.VENDOR_HOSTILE;
        }

        if (random.nextFloat() < HAGGLE_ACCEPT_CHANCE) {
            hagglesSucceeded++;
            haggleFailCount = 0; // reset on success

            // BOOT_SALE_HUSTLER: 5 successful haggles
            if (hagglesSucceeded >= AchievementType.BOOT_SALE_HUSTLER.getProgressTarget()
                    && callback != null) {
                callback.award(AchievementType.BOOT_SALE_HUSTLER);
            }
            return HaggleResult.ACCEPTED;
        } else {
            haggleFailCount++;
            if (haggleFailCount >= HAGGLE_FAIL_LIMIT) {
                // Seed VENDOR_DISPUTE rumour / event
                return HaggleResult.VENDOR_HOSTILE;
            }
            return HaggleResult.REFUSED;
        }
    }

    /**
     * Reset the haggle fail count for a new vendor interaction.
     * Call when the player moves to a different table.
     */
    public void resetHaggleCount() {
        haggleFailCount = 0;
    }

    // ── Derek's dodgy back-stock ──────────────────────────────────────────────

    /**
     * Attempt to buy a stolen item from Derek ({@link NPCType#DODGY_VENDOR})
     * at 50% of the given fence value.
     *
     * @param fenceValue          full fence value of the item
     * @param material            the material to buy
     * @param playerRep           player's current Street Reputation
     * @param watchNearby         true if a NEIGHBOURHOOD_WATCH NPC is within range
     * @param inventory           player's inventory
     * @return result of the buy attempt
     */
    public DerekBuyResult buyFromDerek(int fenceValue, Material material,
                                       int playerRep, boolean watchNearby,
                                       Inventory inventory) {
        if (!eventActive) return DerekBuyResult.NOT_AVAILABLE;
        if (playerRep < DEREK_REP_GATE) return DerekBuyResult.REP_TOO_LOW;
        if (watchNearby) return DerekBuyResult.WATCH_NEARBY;

        int price = Math.max(1, Math.round(fenceValue * DEREK_FENCE_RATIO));
        if (inventory.getItemCount(Material.COIN) < price) {
            return DerekBuyResult.INSUFFICIENT_FUNDS;
        }

        inventory.removeItem(Material.COIN, price);
        inventory.addItem(material, 1);
        return DerekBuyResult.PURCHASED;
    }

    /**
     * Sell a stolen item to Derek at 50% of the given fence value. No notoriety
     * added unless a NEIGHBOURHOOD_WATCH NPC is nearby.
     *
     * @param fenceValue          full fence value of the item
     * @param material            the material to sell
     * @param playerRep           player's current Street Reputation
     * @param watchNearby         true if a NEIGHBOURHOOD_WATCH NPC is within range
     * @param inventory           player's inventory
     * @param callback            achievement callback (used if notoriety triggers)
     * @return COIN received, or -1 if the sell was refused
     */
    public int sellToDerek(int fenceValue, Material material, int playerRep,
                           boolean watchNearby, Inventory inventory,
                           AchievementCallback callback) {
        if (!eventActive) return -1;
        if (playerRep < DEREK_REP_GATE) return -1;
        if (watchNearby) {
            // Watch nearby — dealing exposed; add notoriety
            if (notorietySystem != null && callback != null) {
                notorietySystem.addNotoriety(5, callback);
            }
            return -1;
        }
        if (!inventory.hasItem(material)) return -1;

        int payment = Math.max(1, Math.round(fenceValue * DEREK_FENCE_RATIO));
        inventory.removeItem(material, 1);
        inventory.addItem(Material.COIN, payment);
        return payment;
    }

    // ── Dog chaos event ───────────────────────────────────────────────────────

    /**
     * Trigger the DOG_LEADS_CHAOS event if a dog companion is present at the
     * event. Seeds a rumour and causes a stall knock-over (pitchItemCount −1).
     *
     * @param dogPresent  true if the player has a dog companion
     * @param witnessNpc  an NPC near the player (for rumour seeding; may be null)
     * @return EventType.DOG_LEADS_CHAOS if triggered, null otherwise
     */
    public EventType triggerDogChaos(boolean dogPresent, NPC witnessNpc) {
        if (!eventActive || !dogPresent) return null;
        if (pitchItemCount > 0) {
            pitchItemCount--;
        }
        if (rumourNetwork != null && witnessNpc != null) {
            rumourNetwork.addRumour(witnessNpc,
                new Rumour(RumourType.PENSIONER_SHOVED,
                    "Did you hear? Some pensioner got shoved out the way at the " +
                    "car boot — everyone was after the same box of VHS tapes at 6am."));
        }
        return EventType.DOG_LEADS_CHAOS;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return true if the event is currently active */
    public boolean isEventActive() { return eventActive; }

    /** @return true if the player has paid the pitch fee this session */
    public boolean isPitchFeePaid() { return pitchFeePaid; }

    /** @return number of items currently on the player's pitch */
    public int getPitchItemCount() { return pitchItemCount; }

    /** @return number of items sold from the player's pitch this session */
    public int getPitchItemsSold() { return pitchItemsSold; }

    /** @return number of stolen goods openly displayed on player's pitch */
    public int getStolenGoodsOnDisplay() { return stolenGoodsOnDisplay; }

    /** @return whether the TRADING_STANDARDS_STING has fired this session */
    public boolean isStingFired() { return stingFired; }

    /** @return number of failed haggles in current vendor interaction */
    public int getHaggleFailCount() { return haggleFailCount; }

    /** @return cumulative successful haggles (for BOOT_SALE_HUSTLER tracking) */
    public int getHagglesSucceeded() { return hagglesSucceeded; }

    /** @return whether the early-bird discount has been applied this session */
    public boolean isEarlyBirdApplied() { return earlyBirdApplied; }

    /** @return elapsed real seconds since event opened this session */
    public float getSecondsSinceOpen() { return secondsSinceOpen; }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /** Force event active state for testing. */
    public void setEventActiveForTesting(boolean active) {
        this.eventActive = active;
    }

    /** Force pitch fee paid state for testing. */
    public void setPitchFeePaidForTesting(boolean paid) {
        this.pitchFeePaid = paid;
    }

    /** Force pitch item count for testing. */
    public void setPitchItemCountForTesting(int count) {
        this.pitchItemCount = count;
    }

    /** Force stolen goods on display count for testing. */
    public void setStolenGoodsOnDisplayForTesting(int count) {
        this.stolenGoodsOnDisplay = count;
    }

    /** Force haggles succeeded count for testing. */
    public void setHagglesSucceededForTesting(int count) {
        this.hagglesSucceeded = count;
    }

    /** Force haggle fail count for testing. */
    public void setHaggleFailCountForTesting(int count) {
        this.haggleFailCount = count;
    }

    /** Force seconds since open for testing (early-bird window control). */
    public void setSecondsSinceOpenForTesting(float seconds) {
        this.secondsSinceOpen = seconds;
    }

    /** Force pitch items sold count for testing. */
    public void setPitchItemsSoldForTesting(int sold) {
        this.pitchItemsSold = sold;
    }
}

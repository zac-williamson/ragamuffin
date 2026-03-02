package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #952 / #1255: Clucky's Fried Chicken — Wing Tax &amp; the Late-Night Lockout Hustle.
 *
 * <p>Clucky's Fried Chicken ({@code LandmarkType.FRIED_CHICKEN_SHOP}) occupies a narrow
 * high-street unit (8×6×4 blocks). DEVRAJ ({@code NPCType.DEVRAJ}) runs the counter
 * 10:00–02:00 and refuses service to BALACLAVA wearers.
 *
 * <h3>Opening Hours</h3>
 * <ul>
 *   <li>Normal: 10:00–02:00 (stored as 10.0–26.0 with 24-hour wrap)</li>
 *   <li>Security grille drops at 02:00</li>
 * </ul>
 *
 * <h3>Menu</h3>
 * <ul>
 *   <li>CHICKEN_WINGS — 2 COIN, +35 hunger</li>
 *   <li>CHICKEN_BOX — 4 COIN, +50 hunger</li>
 *   <li>CHIPS_AND_GRAVY — 1 COIN, +30 hunger (after 20:00 only)</li>
 *   <li>FLAT_COLA — 1 COIN, +10 thirst</li>
 * </ul>
 *
 * <h3>Wing Tax Mechanic</h3>
 * YOUTH_GANG within 4 blocks has 40 % chance/minute to demand food while player holds
 * chicken. Player chooses Give/Refuse/Run/Stare-down (BRAWLING tier ≥ EXPERT).
 * Refusing: 60 % fight chance → FIGHT_NOISE + FIGHT_NEARBY rumour.
 * {@link AchievementType#WING_DEFENDER} awarded at 5 successful refusals.
 *
 * <h3>Late-Night Lockout Hustle</h3>
 * At 01:45 a DRUNK is refused entry. Player can fetch a CHICKEN_BOX for 3 COIN, or
 * mark it up to 6 COIN (TRADING/INFLUENCE tier ≥ JOURNEYMAN). Achievement
 * {@link AchievementType#LATE_NIGHT_ENTREPRENEUR} after 3 mark-ups.
 *
 * <h3>Police / Notoriety</h3>
 * <ul>
 *   <li>Balaclava → refused + police in 5 seconds (+1 wanted star)</li>
 *   <li>FIGHT_NEARBY rumour when 2+ YOUTH_GANG fight outside</li>
 *   <li>Smashing FRYER_PROP → ARSON + Notoriety +15</li>
 *   <li>Robbing till → 8–15 COIN + THEFT + police</li>
 * </ul>
 *
 * <h3>Litter</h3>
 * Spawns CHICKEN_BONE / EMPTY_CHICKEN_BOX every 15 in-game minutes; cap 8.
 *
 * <h3>Midnight Rumour</h3>
 * DEVRAJ seeds a local gossip rumour at 00:00.
 */
public class FriedChickenShopSystem {

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Normal opening hour (10:00). */
    public static final float OPEN_HOUR = 10.0f;

    /**
     * Closing hour expressed as hours-past-midnight on the 24-h scale.
     * The shop closes at 02:00, represented as 26.0 on a continuous clock
     * (i.e. hour values wrap: 24.0 = 00:00, 25.0 = 01:00, 26.0 = 02:00).
     */
    public static final float CLOSE_HOUR = 26.0f;

    /** Hour at which the Late-Night Lockout Hustle begins (01:45 = 25.75). */
    public static final float LOCKOUT_HUSTLE_HOUR = 25.75f;

    /** Hour at which CHIPS_AND_GRAVY becomes available. */
    public static final float CHIPS_GRAVY_HOUR = 20.0f;

    /** Hour at which DEVRAJ seeds a midnight rumour (00:00 = 24.0). */
    public static final float MIDNIGHT_RUMOUR_HOUR = 24.0f;

    // ── YOUTH_GANG spawn / Wing Tax ───────────────────────────────────────────

    /** Hour at which YOUTH_GANG NPCs begin spawning (20:00). */
    public static final float YOUTH_SPAWN_HOUR = 20.0f;

    /** Range in blocks within which YOUTH_GANG triggers Wing Tax demand. */
    public static final float WING_TAX_RANGE = 4.0f;

    /** Probability per minute that a YOUTH_GANG demands food (40 %). */
    public static final float WING_TAX_CHANCE_PER_MINUTE = 0.40f;

    /** Probability that a refusal triggers a fight (60 %). */
    public static final float WING_TAX_FIGHT_CHANCE = 0.60f;

    /** BRAWLING tier level required for a successful stare-down (EXPERT = 3). */
    public static final int INTIMIDATE_TIER_REQUIRED = 3;

    /** TRADING/INFLUENCE tier level required to mark up the Lockout Hustle price (JOURNEYMAN = 2). */
    public static final int NEGOTIATE_TIER_REQUIRED = 2;

    /** Number of successful Wing Tax refusals needed for WING_DEFENDER achievement. */
    public static final int WING_DEFENDER_TARGET = 5;

    /** 30 % chance per hour that a YOUTH_GANG enters FIGHTING_EACH_OTHER state. */
    public static final float YOUTH_FIGHT_CHANCE_PER_HOUR = 0.30f;

    // ── Lockout Hustle prices ─────────────────────────────────────────────────

    /** Standard CHICKEN_BOX sell price for the Lockout Hustle (3 COIN). */
    public static final int HUSTLE_STANDARD_PRICE = 3;

    /** Marked-up CHICKEN_BOX price when NEGOTIATE skill is sufficient (6 COIN). */
    public static final int HUSTLE_MARKUP_PRICE = 6;

    /** Number of mark-ups needed for LATE_NIGHT_ENTREPRENEUR achievement. */
    public static final int LATE_NIGHT_ENTREPRENEUR_TARGET = 3;

    // ── Litter spawning ───────────────────────────────────────────────────────

    /** In-game minutes between litter spawns (15 minutes). */
    public static final float LITTER_SPAWN_INTERVAL_MINUTES = 15.0f;

    /** Maximum litter items outside the shop at any one time. */
    public static final int LITTER_CAP = 8;

    // ── Till robbery ──────────────────────────────────────────────────────────

    /** Minimum COIN from robbing the till. */
    public static final int TILL_LOOT_MIN = 8;

    /** Maximum COIN from robbing the till. */
    public static final int TILL_LOOT_MAX = 15;

    // ── Notoriety for Fryer smash ─────────────────────────────────────────────

    /** Notoriety gained for smashing the fryer (ARSON). */
    public static final int FRYER_SMASH_NOTORIETY = 15;

    // ── Balaclava police timer ────────────────────────────────────────────────

    /** Seconds until police are called after refusing a BALACLAVA wearer. */
    public static final float BALACLAVA_POLICE_DELAY = 5.0f;

    // ── Result enums ──────────────────────────────────────────────────────────

    /**
     * Result of an order attempt at the CHIPPY_COUNTER (Clucky's counter).
     */
    public enum OrderResult {
        SUCCESS,
        SHOP_CLOSED,
        BALACLAVA_REFUSED,
        INSUFFICIENT_FUNDS,
        ITEM_UNAVAILABLE
    }

    /**
     * Result of a Wing Tax interaction.
     */
    public enum WingTaxResult {
        /** Player gave away their chicken — no confrontation. */
        GAVE,
        /** Player refused and escaped (ran). */
        RAN,
        /** Player stared down the youth (requires INTIMIDATE tier). */
        STARED_DOWN,
        /** Player refused, fight was triggered. */
        FIGHT_TRIGGERED,
        /** Player refused, fight was not triggered (60% dice roll passed). */
        REFUSED_NO_FIGHT,
        /** YOUTH_GANG not in range or player has no chicken. */
        NOT_APPLICABLE
    }

    /**
     * Result of the Late-Night Lockout Hustle offer.
     */
    public enum LockoutHustleResult {
        /** Standard price (3 COIN) — CHICKEN_BOX sold to drunk. */
        SOLD_STANDARD,
        /** Marked-up price (6 COIN) — NEGOTIATE skill used. */
        SOLD_MARKUP,
        /** Player does not have CHICKEN_BOX in inventory. */
        NO_CHICKEN_BOX,
        /** Player does not have enough COIN for purchase (for STANDARD, cost to get box). */
        HUSTLE_NOT_ACTIVE
    }

    /**
     * Result of robbing the till.
     */
    public enum TillRobResult {
        SUCCESS,
        SHOP_CLOSED,
        ALREADY_ROBBED
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random rng;

    /** Whether a midnight rumour has been seeded this in-game day. */
    private boolean midnightRumourSeeded = false;

    /** Count of successful Wing Tax refusals this session. */
    private int wingTaxRefusals = 0;

    /** Count of Lockout Hustle mark-ups completed. */
    private int lockoutMarkupCount = 0;

    /** Whether the till has been robbed today. */
    private boolean tillRobbed = false;

    /** Current litter count outside the shop. */
    private int litterCount = 0;

    /** Timer accumulating real seconds toward the next litter spawn event. */
    private float litterTimer = 0f;

    /** Number of seconds per litter spawn (converted from in-game minutes at construction). */
    private final float litterSpawnSeconds;

    // ── Optional system references ────────────────────────────────────────────

    private RumourNetwork rumourNetwork;
    private NotorietySystem notorietySystem;
    private AchievementSystem achievementSystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private NoiseSystem noiseSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    /**
     * Create a FriedChickenShopSystem with the given in-game minutes-per-real-second ratio
     * (so tests can control litter timing).
     *
     * @param rng                   random number generator
     * @param inGameMinutesPerSecond how many in-game minutes pass per real second
     */
    public FriedChickenShopSystem(Random rng, float inGameMinutesPerSecond) {
        this.rng = rng;
        // Convert in-game minute interval to real seconds
        this.litterSpawnSeconds = LITTER_SPAWN_INTERVAL_MINUTES / inGameMinutesPerSecond;
    }

    public FriedChickenShopSystem(Random rng) {
        this(rng, 1.0f);
    }

    public FriedChickenShopSystem() {
        this(new Random(), 1.0f);
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setRumourNetwork(RumourNetwork r)         { this.rumourNetwork = r; }
    public void setNotorietySystem(NotorietySystem n)     { this.notorietySystem = n; }
    public void setAchievementSystem(AchievementSystem a) { this.achievementSystem = a; }
    public void setWantedSystem(WantedSystem w)           { this.wantedSystem = w; }
    public void setCriminalRecord(CriminalRecord c)       { this.criminalRecord = c; }
    public void setNoiseSystem(NoiseSystem n)             { this.noiseSystem = n; }

    // ── Opening hours ─────────────────────────────────────────────────────────

    /**
     * Returns whether Clucky's is open at the given hour.
     *
     * <p>The shop is open 10:00–02:00. Hours after midnight are represented
     * as values ≥ 24.0 (e.g. 01:00 = 25.0, 02:00 = 26.0).
     *
     * @param hour current in-game hour on a continuous scale (0.0–48.0 range supported)
     * @return true if the shop is open
     */
    public boolean isOpen(float hour) {
        // Normalise hour to the range used within a single "day window"
        // The shop spans from 10:00 to 02:00 the next morning.
        // On a 0–24 scale, hours 0–2 are "late night" from the previous opening.
        // We represent those as 24–26 by adding 24 to hours < OPEN_HOUR.
        float h = (hour < OPEN_HOUR) ? hour + 24.0f : hour;
        return h >= OPEN_HOUR && h < CLOSE_HOUR;
    }

    /**
     * Returns whether CHIPS_AND_GRAVY is available at the given hour (after 20:00).
     *
     * @param hour current in-game hour (0–24)
     * @return true if chips and gravy is on the menu
     */
    public boolean isChipsAndGravyAvailable(float hour) {
        return hour >= CHIPS_GRAVY_HOUR || hour < OPEN_HOUR;
    }

    /**
     * Returns whether the Late-Night Lockout Hustle is active (01:45–02:00).
     *
     * @param hour current in-game hour on the continuous scale
     * @return true if the lockout hustle window is open
     */
    public boolean isLockoutHustleActive(float hour) {
        float h = (hour < OPEN_HOUR) ? hour + 24.0f : hour;
        return h >= LOCKOUT_HUSTLE_HOUR && h < CLOSE_HOUR;
    }

    /**
     * Returns whether the YOUTH_GANG spawn window is active (20:00–02:00).
     *
     * @param hour current in-game hour
     * @return true if youth gang spawning is permitted
     */
    public boolean isYouthSpawnActive(float hour) {
        return hour >= YOUTH_SPAWN_HOUR || hour < OPEN_HOUR;
    }

    // ── Ordering ──────────────────────────────────────────────────────────────

    /**
     * Player places an order at the counter.
     *
     * @param material          the menu item being ordered
     * @param basePrice         the base price in COIN
     * @param inventory         the player's inventory
     * @param currentHour       current in-game hour (0–24)
     * @param wearingBalaclava  true if the player is wearing a BALACLAVA
     * @param devrajNpc         DEVRAJ's NPC instance (for rumour seeding), may be null
     * @param achievementCallback callback for awarding achievements, may be null
     * @return the result of the order attempt
     */
    public OrderResult placeOrder(Material material, int basePrice,
                                  Inventory inventory, float currentHour,
                                  boolean wearingBalaclava,
                                  NPC devrajNpc,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        // Opening hours check
        if (!isOpen(currentHour)) {
            return OrderResult.SHOP_CLOSED;
        }

        // BALACLAVA refusal
        if (wearingBalaclava) {
            // Schedule police call via wanted system (+1 star after delay)
            // The game loop handles the 5-second timer; we flag the event here.
            if (wantedSystem != null) {
                WantedSystem.AchievementCallback wantedCb =
                        achievementCallback != null ? achievementCallback::award : null;
                wantedSystem.addWantedStars(1, 0f, 0f, 0f, wantedCb);
            }
            return OrderResult.BALACLAVA_REFUSED;
        }

        // CHIPS_AND_GRAVY only available after 20:00
        if (material == Material.CHIPS_AND_GRAVY && !isChipsAndGravyAvailable(currentHour)) {
            return OrderResult.ITEM_UNAVAILABLE;
        }

        // Funds check
        if (inventory.getItemCount(Material.COIN) < basePrice) {
            return OrderResult.INSUFFICIENT_FUNDS;
        }

        // Deduct coins and add item
        inventory.removeItem(Material.COIN, basePrice);
        inventory.addItem(material, 1);

        // DEVRAJ shares a rumour if he has one
        if (devrajNpc != null && rumourNetwork != null) {
            List<Rumour> devRumours = devrajNpc.getRumours();
            if (!devRumours.isEmpty()) {
                rumourNetwork.addRumour(devrajNpc, devRumours.get(0).spread());
            }
        }

        return OrderResult.SUCCESS;
    }

    // ── Midnight rumour ───────────────────────────────────────────────────────

    /**
     * Called each frame to check whether DEVRAJ should seed the midnight rumour.
     * Seeds exactly once per in-game day at 00:00.
     *
     * @param hour      current in-game hour (0–24, where 0.0 = midnight)
     * @param devrajNpc DEVRAJ's NPC instance, may be null
     */
    public void updateMidnightRumour(float hour, NPC devrajNpc) {
        if (!midnightRumourSeeded && hour < 0.1f && devrajNpc != null && rumourNetwork != null) {
            midnightRumourSeeded = true;
            rumourNetwork.addRumour(devrajNpc,
                    new Rumour(RumourType.FIGHT_NEARBY,
                            "Devraj reckons there's always something going on out there — "
                            + "he sees it all from behind that counter"));
        }
        // Reset flag when day rolls over (past midnight window)
        if (hour > 1.0f) {
            midnightRumourSeeded = false;
        }
    }

    // ── Wing Tax mechanic ─────────────────────────────────────────────────────

    /**
     * Resolves a Wing Tax demand from the YOUTH_GANG.
     *
     * <p>Should be called when the random demand trigger fires (40 % chance/minute
     * while YOUTH_GANG is within {@link #WING_TAX_RANGE} blocks and player holds
     * CHICKEN_WINGS or CHICKEN_BOX).
     *
     * @param action               player's chosen response
     * @param inventory            player's inventory
     * @param brawlingTierLevel    player's BRAWLING skill tier (0–4)
     * @param achievementCallback  callback for awarding achievements, may be null
     * @param gangNpc              one YOUTH_GANG NPC instance (for rumour seeding), may be null
     * @return the result of the interaction
     */
    public WingTaxResult resolveWingTax(WingTaxAction action, Inventory inventory,
                                        int brawlingTierLevel,
                                        NotorietySystem.AchievementCallback achievementCallback,
                                        NPC gangNpc) {
        // Check player actually holds chicken
        boolean hasChicken = inventory.hasItem(Material.CHICKEN_WINGS)
                || inventory.hasItem(Material.CHICKEN_BOX);
        if (!hasChicken) {
            return WingTaxResult.NOT_APPLICABLE;
        }

        switch (action) {
            case GIVE: {
                // Surrender the most valuable chicken item
                if (inventory.hasItem(Material.CHICKEN_BOX)) {
                    inventory.removeItem(Material.CHICKEN_BOX, 1);
                } else {
                    inventory.removeItem(Material.CHICKEN_WINGS, 1);
                }
                return WingTaxResult.GAVE;
            }

            case RUN: {
                // Running away always succeeds; no fight
                recordRefusal(achievementCallback);
                return WingTaxResult.RAN;
            }

            case STARE_DOWN: {
                if (brawlingTierLevel >= INTIMIDATE_TIER_REQUIRED) {
                    // Successful intimidation — gang backs off
                    recordRefusal(achievementCallback);
                    return WingTaxResult.STARED_DOWN;
                }
                // Fall through to REFUSE logic if stare-down fails
                return resolveRefuse(achievementCallback, gangNpc);
            }

            case REFUSE:
            default: {
                return resolveRefuse(achievementCallback, gangNpc);
            }
        }
    }

    private WingTaxResult resolveRefuse(NotorietySystem.AchievementCallback achievementCallback,
                                        NPC gangNpc) {
        recordRefusal(achievementCallback);
        // 60% chance of fight
        if (rng.nextFloat() < WING_TAX_FIGHT_CHANCE) {
            // Seed FIGHT_NEARBY rumour
            if (rumourNetwork != null) {
                NPC source = (gangNpc != null) ? gangNpc
                        : new NPC(NPCType.YOUTH_GANG, 0f, 1f, 0f);
                rumourNetwork.addRumour(source,
                        new Rumour(RumourType.FIGHT_NEARBY,
                                "There's a scrap kicking off outside Clucky's — someone's getting battered"));
            }
            // Add noise for the fight
            if (noiseSystem != null) {
                noiseSystem.addNoise(0.8f);
            }
            return WingTaxResult.FIGHT_TRIGGERED;
        }
        return WingTaxResult.REFUSED_NO_FIGHT;
    }

    private void recordRefusal(NotorietySystem.AchievementCallback achievementCallback) {
        wingTaxRefusals++;
        if (wingTaxRefusals >= WING_DEFENDER_TARGET && achievementCallback != null) {
            achievementCallback.award(AchievementType.WING_DEFENDER);
        }
    }

    /**
     * Returns the current Wing Tax refusal count (for WING_DEFENDER progress).
     */
    public int getWingTaxRefusalCount() {
        return wingTaxRefusals;
    }

    /**
     * Player choices in a Wing Tax confrontation.
     */
    public enum WingTaxAction {
        GIVE, REFUSE, RUN, STARE_DOWN
    }

    // ── Late-Night Lockout Hustle ──────────────────────────────────────────────

    /**
     * Resolves the Late-Night Lockout Hustle at 01:45.
     * A DRUNK NPC is refused entry; player can sell them a CHICKEN_BOX.
     *
     * @param useMarkup            true if player attempts the mark-up price
     * @param inventory            player's inventory
     * @param tradingTierLevel     player's TRADING or INFLUENCE skill tier (0–4)
     * @param achievementCallback  callback for awarding achievements, may be null
     * @return the result of the hustle
     */
    public LockoutHustleResult resolveLockoutHustle(boolean useMarkup, Inventory inventory,
                                                    int tradingTierLevel,
                                                    NotorietySystem.AchievementCallback achievementCallback) {
        if (!inventory.hasItem(Material.CHICKEN_BOX)) {
            return LockoutHustleResult.NO_CHICKEN_BOX;
        }

        int salePrice;
        boolean isMarkup = false;

        if (useMarkup && tradingTierLevel >= NEGOTIATE_TIER_REQUIRED) {
            salePrice = HUSTLE_MARKUP_PRICE;
            isMarkup = true;
        } else {
            salePrice = HUSTLE_STANDARD_PRICE;
        }

        // Remove chicken box from inventory and add coins
        inventory.removeItem(Material.CHICKEN_BOX, 1);
        inventory.addItem(Material.COIN, salePrice);

        if (isMarkup) {
            lockoutMarkupCount++;
            if (lockoutMarkupCount >= LATE_NIGHT_ENTREPRENEUR_TARGET && achievementCallback != null) {
                achievementCallback.award(AchievementType.LATE_NIGHT_ENTREPRENEUR);
            }
            return LockoutHustleResult.SOLD_MARKUP;
        }

        return LockoutHustleResult.SOLD_STANDARD;
    }

    /**
     * Returns the current Lockout Hustle mark-up count (for LATE_NIGHT_ENTREPRENEUR progress).
     */
    public int getLockoutMarkupCount() {
        return lockoutMarkupCount;
    }

    // ── YOUTH_GANG fighting ───────────────────────────────────────────────────

    /**
     * Checks whether the YOUTH_GANG enters FIGHTING_EACH_OTHER state this hour.
     * Should be called once per in-game hour during 20:00–02:00.
     *
     * @param gangNpcs   list of YOUTH_GANG NPC instances outside the shop
     * @param sourceNpc  NPC instance for rumour seeding (first gang member), may be null
     * @return true if the gang started fighting
     */
    public boolean checkYouthGangFight(List<NPC> gangNpcs, NPC sourceNpc) {
        if (gangNpcs == null || gangNpcs.size() < 2) return false;
        if (rng.nextFloat() >= YOUTH_FIGHT_CHANCE_PER_HOUR) return false;

        // Set all gang NPCs to fighting state
        for (NPC npc : gangNpcs) {
            npc.setState(NPCState.AGGRESSIVE);
        }

        // Seed FIGHT_NEARBY rumour if 2+ are fighting outside
        if (gangNpcs.size() >= 2 && rumourNetwork != null) {
            NPC source = (sourceNpc != null) ? sourceNpc : gangNpcs.get(0);
            rumourNetwork.addRumour(source,
                    new Rumour(RumourType.FIGHT_NEARBY,
                            "There's a scrap kicking off outside Clucky's — someone's getting battered"));
        }

        if (noiseSystem != null) {
            noiseSystem.addNoise(0.8f);
        }

        return true;
    }

    // ── Fryer smash ───────────────────────────────────────────────────────────

    /**
     * Player smashes the FRYER_PROP — triggers ARSON crime and Notoriety +15.
     *
     * @param achievementCallback callback for awarding achievements, may be null
     */
    public void onFryerSmashed(NotorietySystem.AchievementCallback achievementCallback) {
        // Record ARSON in criminal record
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.ARSON);
        }

        // Add notoriety
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(FRYER_SMASH_NOTORIETY, achievementCallback);
        }

        // Add noise
        if (noiseSystem != null) {
            noiseSystem.addNoise(1.0f);
        }
    }

    // ── Till robbery ──────────────────────────────────────────────────────────

    /**
     * Player robs the till.
     *
     * @param inventory           player's inventory
     * @param currentHour         current in-game hour
     * @param achievementCallback callback for awarding achievements, may be null
     * @return the result of the robbery attempt
     */
    public TillRobResult robTill(Inventory inventory, float currentHour,
                                 NotorietySystem.AchievementCallback achievementCallback) {
        if (!isOpen(currentHour)) {
            return TillRobResult.SHOP_CLOSED;
        }
        if (tillRobbed) {
            return TillRobResult.ALREADY_ROBBED;
        }

        int coins = TILL_LOOT_MIN + rng.nextInt(TILL_LOOT_MAX - TILL_LOOT_MIN + 1);
        inventory.addItem(Material.COIN, coins);
        tillRobbed = true;

        // Record THEFT
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.THEFT);
        }

        // Wanted star
        if (wantedSystem != null) {
            WantedSystem.AchievementCallback wantedCb =
                    achievementCallback != null ? achievementCallback::award : null;
            wantedSystem.addWantedStars(1, 0f, 0f, 0f, wantedCb);
        }

        return TillRobResult.SUCCESS;
    }

    /**
     * Returns whether the till has been robbed this session.
     */
    public boolean isTillRobbed() {
        return tillRobbed;
    }

    // ── Litter spawning ───────────────────────────────────────────────────────

    /**
     * Updates the litter spawn timer. Called each frame.
     * When the timer fires and the litter cap has not been reached, increments
     * the litter count (the game world places the prop; this system tracks the count).
     *
     * @param delta real seconds since last frame
     * @return true if a new litter item should be spawned this frame
     */
    public boolean updateLitter(float delta) {
        if (litterCount >= LITTER_CAP) return false;
        litterTimer += delta;
        if (litterTimer >= litterSpawnSeconds) {
            litterTimer -= litterSpawnSeconds;
            litterCount++;
            return true;
        }
        return false;
    }

    /**
     * Called by the game world when a litter item is picked up (e.g. binned by player).
     */
    public void onLitterPickedUp() {
        if (litterCount > 0) litterCount--;
    }

    /**
     * Returns the current number of litter items outside the shop.
     */
    public int getLitterCount() {
        return litterCount;
    }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /**
     * Force the wing tax refusal count for testing.
     */
    public void setWingTaxRefusalsForTesting(int count) {
        this.wingTaxRefusals = count;
    }

    /**
     * Force the lockout markup count for testing.
     */
    public void setLockoutMarkupCountForTesting(int count) {
        this.lockoutMarkupCount = count;
    }

    /**
     * Force the till robbed flag for testing.
     */
    public void setTillRobbedForTesting(boolean robbed) {
        this.tillRobbed = robbed;
    }

    /**
     * Force the litter count for testing.
     */
    public void setLitterCountForTesting(int count) {
        this.litterCount = count;
    }
}

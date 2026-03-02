package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1257: Northfield Rag-and-Bone Man — Barry's Round &amp; the Horsebox Hustle.
 *
 * <p>Barry Dodd ({@code NPCType.RAG_AND_BONE_MAN}) drives his battered flatbed Transit
 * ({@code PropType.RAG_AND_BONE_VAN}) Mon–Sat 07:30–13:00 on a fixed 6-stop route,
 * calling out "Any old iroooon!" every 30 in-game seconds (range 20 blocks).
 * He won't run on Sundays or in rain.
 *
 * <h3>Route</h3>
 * Barry visits 6 {@code PropType.RAGBONE_STOP} locations in sequence, spending
 * {@value #STOP_DURATION_SECONDS} real seconds at each stop. Up to
 * {@value #MAX_QUEUE_SIZE} PUBLIC NPCs may queue to sell {@code Material.JUNK_ITEM}.
 *
 * <h3>Player Scrap Prices (no questions asked)</h3>
 * <ul>
 *   <li>JUNK_ITEM — 1 COIN</li>
 *   <li>SCRAP_METAL — 2 COIN</li>
 *   <li>COPPER_WIRE — 4 COIN (3+ items triggers suspicion dialogue + Notoriety +5)</li>
 *   <li>LEAD_FLASHING — 5 COIN</li>
 *   <li>GARDEN_ORNAMENT — 2 COIN</li>
 * </ul>
 * Barry refuses {@code Material.EVIDENCE_ITEM} types. Selling with Notoriety ≥ 60
 * triggers WantedSystem +1 star.
 *
 * <h3>Three Hustles</h3>
 * <ol>
 *   <li><b>Garden Ornament Pinch</b> — steal GARDEN_ORNAMENT props from front gardens
 *       at night (CriminalRecord THEFT, WitnessSystem), sell to Barry next morning.</li>
 *   <li><b>Rival Route Sabotage</b> — slash Barry's tyres 02:00–06:00 with PENKNIFE;
 *       RIVAL_RAGBONE_MAN (Terry) takes over; repair van with RUBBER_TYRE for 10 COIN
 *       + {@link AchievementType#BARRY_S_MATE}.</li>
 *   <li><b>Door-Knock Pre-buy</b> — knock on residential doors 07:00–09:00 before Barry
 *       arrives, buy JUNK_ITEM cheaply (or free via charity appeal), resell to Barry.
 *       {@link AchievementType#KNOCKER_BOY} after 5 completions.</li>
 * </ol>
 *
 * <h3>Council Enforcement</h3>
 * {@code NPCType.COUNCIL_ENFORCEMENT} patrols Fridays (55% chance). If Barry lacks
 * BARRY_LICENCE_STATUS, the van is impounded for 48 in-game hours, Barry flees, and
 * {@link RumourType#BARRY_NICKED} spreads. Player can warn Barry (3 COIN reward),
 * bribe the officer with TIN_OF_BEANS (60% success, Notoriety +3 on failure), or
 * present a {@link Material#FORGED_LICENCE} from the InternetCafeSystem (5 COIN to craft).
 */
public class RagAndBoneSystem {

    // ── Day-of-week constants (dayCount % 7; ChurchSystem convention) ─────────
    // day 1 = game start (Wednesday=2); Sunday=6, Saturday=5, Friday=4
    private static final int SUNDAY   = 6;
    private static final int FRIDAY   = 4;

    // ── Operating hours ───────────────────────────────────────────────────────

    /** Barry starts his round at 07:30. */
    public static final float OPEN_HOUR = 7.5f;

    /** Barry's round ends at 13:00. */
    public static final float CLOSE_HOUR = 13.0f;

    /** Window for tyre-slashing sabotage (02:00–06:00). */
    public static final float SLASH_START_HOUR = 2.0f;
    /** End of tyre-slashing window (06:00). */
    public static final float SLASH_END_HOUR = 6.0f;

    /** Door-knock pre-buy window start (07:00). */
    public static final float DOORKNOCK_START_HOUR = 7.0f;
    /** Door-knock pre-buy window end (09:00). */
    public static final float DOORKNOCK_END_HOUR = 9.0f;

    // ── Route / stop mechanics ────────────────────────────────────────────────

    /** Number of stops on Barry's route. */
    public static final int STOP_COUNT = 6;

    /** Real seconds Barry spends at each stop. */
    public static final float STOP_DURATION_SECONDS = 90.0f;

    /** Maximum PUBLIC NPCs queuing to sell at a single stop. */
    public static final int MAX_QUEUE_SIZE = 3;

    // ── Call-out ("Any old iroooon!") ─────────────────────────────────────────

    /** In-game seconds between Barry's calls. */
    public static final float CALLOUT_INTERVAL_SECONDS = 30.0f;

    /** Range in blocks within which Barry's call is audible. */
    public static final float CALLOUT_RANGE_BLOCKS = 20.0f;

    // ── Scrap prices ──────────────────────────────────────────────────────────

    /** COIN paid by Barry for a JUNK_ITEM. */
    public static final int PRICE_JUNK_ITEM = 1;

    /** COIN paid by Barry for SCRAP_METAL. */
    public static final int PRICE_SCRAP_METAL = 2;

    /** COIN paid by Barry for COPPER_WIRE. */
    public static final int PRICE_COPPER_WIRE = 4;

    /** COIN paid by Barry for LEAD_FLASHING. */
    public static final int PRICE_LEAD_FLASHING = 5;

    /** COIN paid by Barry for GARDEN_ORNAMENT. */
    public static final int PRICE_GARDEN_ORNAMENT = 2;

    /**
     * Number of COPPER_WIRE sold in a single visit before suspicion dialogue fires
     * and Notoriety is increased.
     */
    public static final int COPPER_WIRE_SUSPICION_THRESHOLD = 3;

    /** Notoriety added when suspicion triggers on COPPER_WIRE sale. */
    public static final int COPPER_WIRE_NOTORIETY = 5;

    /**
     * Minimum Notoriety level at which selling stolen goods triggers WantedSystem +1 star.
     */
    public static final int NOTORIETY_FENCE_THRESHOLD = 60;

    // ── Council Enforcement ───────────────────────────────────────────────────

    /** Probability that COUNCIL_ENFORCEMENT patrols on a Friday (55%). */
    public static final float ENFORCEMENT_PATROL_CHANCE = 0.55f;

    /** In-game hours Barry's van is impounded if the council catches him unlicensed. */
    public static final float IMPOUND_DURATION_HOURS = 48.0f;

    /** COIN reward to the player for warning Barry before the council arrives. */
    public static final int WARN_BARRY_REWARD = 3;

    /** Probability that bribing the officer with TIN_OF_BEANS succeeds (60%). */
    public static final float BRIBE_SUCCESS_CHANCE = 0.60f;

    /** Notoriety added on a failed bribe attempt. */
    public static final int BRIBE_FAIL_NOTORIETY = 3;

    // ── Rival (Terry) ─────────────────────────────────────────────────────────

    /** Probability that Terry steals an item from the player per pass (25%). */
    public static final float RIVAL_STEAL_CHANCE = 0.25f;

    /** Terry's price multiplier relative to Barry's prices (90%). */
    public static final float RIVAL_PRICE_MULTIPLIER = 0.90f;

    // ── Repair reward ─────────────────────────────────────────────────────────

    /** COIN Barry pays the player for repairing his slashed van with a RUBBER_TYRE. */
    public static final int REPAIR_VAN_REWARD = 10;

    // ── Result enums ──────────────────────────────────────────────────────────

    /**
     * Result of the player attempting to sell scrap to Barry.
     */
    public enum SellResult {
        /** Sale completed successfully; coins added to inventory. */
        SUCCESS,
        /** Barry is not currently on his round (wrong hours/day/weather). */
        BARRY_UNAVAILABLE,
        /** Barry refuses this item type (e.g., EVIDENCE_ITEM). */
        ITEM_REFUSED,
        /** Player does not have the item in their inventory. */
        ITEM_NOT_IN_INVENTORY,
        /**
         * Sale completed, but COPPER_WIRE threshold triggered suspicion dialogue
         * and Notoriety increase.
         */
        SUCCESS_WITH_SUSPICION
    }

    /**
     * Result of the player attempting to slash Barry's van tyres.
     */
    public enum SlashTyresResult {
        /** Tyres successfully slashed; Terry takes over. */
        SUCCESS,
        /** Outside the valid tyre-slash window (02:00–06:00). */
        WRONG_TIME,
        /** Player does not have a PENKNIFE. */
        NO_PENKNIFE,
        /** Barry's van is not present (Barry not operating). */
        VAN_NOT_PRESENT,
        /** Van tyres are already slashed. */
        ALREADY_SLASHED
    }

    /**
     * Result of repairing Barry's slashed van.
     */
    public enum RepairVanResult {
        /** Van repaired; Barry resumes his round; player receives reward. */
        SUCCESS,
        /** Player does not have a RUBBER_TYRE. */
        NO_RUBBER_TYRE,
        /** The van is not currently slashed. */
        NOT_SLASHED,
        /** Barry is not present; van can't be repaired right now. */
        BARRY_UNAVAILABLE
    }

    /**
     * Result of the Council Enforcement bribe attempt.
     */
    public enum BribeResult {
        /** Bribe accepted; officer leaves; BARRY_LICENCE_STATUS assumed for the day. */
        SUCCESS,
        /** Bribe failed; Notoriety +3; van still at risk of impounding. */
        FAILED,
        /** Player does not have TIN_OF_BEANS. */
        NO_BEANS,
        /** No enforcement officer is currently active. */
        NO_OFFICER
    }

    /**
     * Result of the player warning Barry about incoming enforcement.
     */
    public enum WarnBarryResult {
        /** Warning delivered; Barry moves on; player receives 3 COIN. */
        SUCCESS,
        /** No enforcement threat is active this day. */
        NO_THREAT,
        /** Barry is not currently reachable. */
        BARRY_UNAVAILABLE
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random rng;

    /** Whether Barry is currently operating (on his round). */
    private boolean barryActive = false;

    /** Whether Barry's van tyres have been slashed by the player. */
    private boolean tyresSlashed = false;

    /** Whether the rival (Terry) is currently operating in Barry's place. */
    private boolean rivalActive = false;

    /** Current stop index (0–5). */
    private int currentStop = 0;

    /** Seconds spent at the current stop. */
    private float stopTimer = 0f;

    /** Seconds since last callout. */
    private float calloutTimer = 0f;

    /** Whether Barry has been impounded today (resets after IMPOUND_DURATION_HOURS). */
    private boolean barryImpounded = false;

    /** Remaining impound hours. */
    private float impoundHoursRemaining = 0f;

    /** Whether Barry has his licence today (player may have provided FORGED_LICENCE). */
    private boolean barryLicenced = false;

    /** Whether the Council Enforcement officer is active this Friday. */
    private boolean enforcementActive = false;

    /** Whether enforcement officer has been rolled for today. */
    private boolean enforcementRolledToday = false;

    /** Number of COPPER_WIRE sold in the current visit. */
    private int copperWireSoldThisVisit = 0;

    /** Number of door-knock pre-buys completed (for KNOCKER_BOY progress). */
    private int doorKnockCount = 0;

    /** Number of total scrap transactions with Barry (for HORSEBOX_HUSTLER progress). */
    private int totalScrapsCount = 0;

    // ── Optional system references ────────────────────────────────────────────

    private RumourNetwork rumourNetwork;
    private NotorietySystem notorietySystem;
    private AchievementSystem achievementSystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private NoiseSystem noiseSystem;
    private WeatherSystem weatherSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    /**
     * Create a RagAndBoneSystem.
     *
     * @param rng random number generator
     */
    public RagAndBoneSystem(Random rng) {
        this.rng = rng;
    }

    public RagAndBoneSystem() {
        this(new Random());
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setRumourNetwork(RumourNetwork r)         { this.rumourNetwork = r; }
    public void setNotorietySystem(NotorietySystem n)     { this.notorietySystem = n; }
    public void setAchievementSystem(AchievementSystem a) { this.achievementSystem = a; }
    public void setWantedSystem(WantedSystem w)           { this.wantedSystem = w; }
    public void setCriminalRecord(CriminalRecord c)       { this.criminalRecord = c; }
    public void setNoiseSystem(NoiseSystem n)             { this.noiseSystem = n; }
    public void setWeatherSystem(WeatherSystem w)         { this.weatherSystem = w; }

    // ── Operating hours / availability ────────────────────────────────────────

    /**
     * Returns whether Barry operates on the given day-of-week index.
     * Barry does not operate on Sundays ({@code dayCount % 7 == 6}).
     *
     * @param dayCount current in-game day count (from TimeSystem.getDayCount())
     * @return true if today is a working day for Barry
     */
    public boolean isWorkingDay(int dayCount) {
        return (dayCount % 7) != SUNDAY;
    }

    /**
     * Returns whether Barry is in his operating hours window (07:30–13:00).
     *
     * @param hour current in-game hour (0–24)
     * @return true if within Barry's operating window
     */
    public boolean isOperatingHour(float hour) {
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Returns whether it is currently raining (which prevents Barry's round).
     * Checks DRIZZLE, RAIN, and THUNDERSTORM.
     *
     * @return true if rain weather is active
     */
    public boolean isRaining() {
        if (weatherSystem == null) return false;
        return weatherSystem.getCurrentWeather().isRaining();
    }

    /**
     * Returns whether Barry is currently available to trade (not impounded, not rained off,
     * working day, and within operating hours).
     *
     * @param hour     current in-game hour
     * @param dayCount current in-game day count
     * @return true if Barry is active and accepting scrap
     */
    public boolean isBarryAvailable(float hour, int dayCount) {
        if (barryImpounded) return false;
        if (tyresSlashed) return false;
        if (!isWorkingDay(dayCount)) return false;
        if (!isOperatingHour(hour)) return false;
        if (isRaining()) return false;
        return true;
    }

    // ── Update (per-frame) ────────────────────────────────────────────────────

    /**
     * Per-frame update. Advances stop timers, triggers callouts, manages impound
     * countdown, and rolls Friday enforcement.
     *
     * @param delta      real seconds since last frame
     * @param timeSystem current TimeSystem instance
     * @param barryNpc   Barry's NPC instance for rumour seeding, may be null
     */
    public void update(float delta, TimeSystem timeSystem, NPC barryNpc) {
        float hour = timeSystem.getTime();
        int dayCount = timeSystem.getDayCount();

        // Roll enforcement officer on Fridays (once per day)
        int dow = dayCount % 7;
        if (dow == FRIDAY && !enforcementRolledToday) {
            enforcementRolledToday = true;
            enforcementActive = rng.nextFloat() < ENFORCEMENT_PATROL_CHANCE;
        }
        // Reset daily roll when day changes (non-Friday)
        if (dow != FRIDAY) {
            enforcementRolledToday = false;
        }

        // Count down impound
        if (barryImpounded && impoundHoursRemaining > 0f) {
            impoundHoursRemaining -= timeSystem.getTimeSpeed() * delta;
            if (impoundHoursRemaining <= 0f) {
                barryImpounded = false;
                impoundHoursRemaining = 0f;
                tyresSlashed = false;
                rivalActive = false;
                barryLicenced = false;
            }
        }

        boolean available = isBarryAvailable(hour, dayCount);

        // Reset copper wire count at start / end of Barry's operating window
        if (!available && barryActive) {
            // Barry just went off duty — reset per-visit count
            copperWireSoldThisVisit = 0;
            currentStop = 0;
            stopTimer = 0f;
        }

        barryActive = available;

        if (!barryActive) return;

        // Advance stop timer
        stopTimer += delta;
        if (stopTimer >= STOP_DURATION_SECONDS) {
            stopTimer -= STOP_DURATION_SECONDS;
            currentStop = (currentStop + 1) % STOP_COUNT;
        }

        // Callout timer
        calloutTimer += delta;
        if (calloutTimer >= CALLOUT_INTERVAL_SECONDS) {
            calloutTimer -= CALLOUT_INTERVAL_SECONDS;
            if (noiseSystem != null) {
                noiseSystem.addNoise(0.3f);
            }
        }
    }

    // ── Selling scrap ─────────────────────────────────────────────────────────

    /**
     * Returns the COIN price Barry will pay for the given material, or -1 if he
     * refuses it.
     *
     * @param material the material being offered
     * @return COIN value, or -1 if refused
     */
    public int getPriceFor(Material material) {
        switch (material) {
            case JUNK_ITEM:        return PRICE_JUNK_ITEM;
            case SCRAP_METAL:      return PRICE_SCRAP_METAL;
            case COPPER_WIRE:      return PRICE_COPPER_WIRE;
            case LEAD_FLASHING:    return PRICE_LEAD_FLASHING;
            case GARDEN_ORNAMENT:  return PRICE_GARDEN_ORNAMENT;
            default:               return -1;
        }
    }

    /**
     * Player attempts to sell one unit of {@code material} to Barry.
     *
     * @param material            the item being sold
     * @param inventory           the player's inventory
     * @param hour                current in-game hour
     * @param dayCount            current in-game day count
     * @param achievementCallback callback for awarding achievements, may be null
     * @return the result of the sell attempt
     */
    public SellResult sellToBarry(Material material, Inventory inventory,
                                  float hour, int dayCount,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        if (!isBarryAvailable(hour, dayCount)) {
            return SellResult.BARRY_UNAVAILABLE;
        }

        if (!inventory.hasItem(material)) {
            return SellResult.ITEM_NOT_IN_INVENTORY;
        }

        int price = getPriceFor(material);
        if (price < 0) {
            return SellResult.ITEM_REFUSED;
        }

        // High-notoriety stolen goods check
        if (notorietySystem != null && notorietySystem.getNotoriety() >= NOTORIETY_FENCE_THRESHOLD) {
            if (wantedSystem != null) {
                WantedSystem.AchievementCallback wantedCb =
                        achievementCallback != null ? achievementCallback::award : null;
                wantedSystem.addWantedStars(1, 0f, 0f, 0f, wantedCb);
            }
        }

        // Remove item and pay
        inventory.removeItem(material, 1);
        inventory.addItem(Material.COIN, price);

        // Track copper wire suspicion
        boolean suspicion = false;
        if (material == Material.COPPER_WIRE) {
            copperWireSoldThisVisit++;
            if (copperWireSoldThisVisit >= COPPER_WIRE_SUSPICION_THRESHOLD) {
                suspicion = true;
                if (notorietySystem != null) {
                    notorietySystem.addNotoriety(COPPER_WIRE_NOTORIETY, achievementCallback);
                }
            }
        }

        // Track total transactions for HORSEBOX_HUSTLER
        totalScrapsCount++;
        if (achievementCallback != null) {
            if (totalScrapsCount >= AchievementType.HORSEBOX_HUSTLER.getProgressTarget()) {
                achievementCallback.award(AchievementType.HORSEBOX_HUSTLER);
            }
        }

        return suspicion ? SellResult.SUCCESS_WITH_SUSPICION : SellResult.SUCCESS;
    }

    // ── Rival (Terry) ─────────────────────────────────────────────────────────

    /**
     * Returns whether Terry (the rival) is currently operating.
     */
    public boolean isRivalActive() {
        return rivalActive;
    }

    /**
     * Returns the price Terry pays for a given material (90% of Barry's prices).
     *
     * @param material the material being offered
     * @return COIN value (floor), or -1 if refused
     */
    public int getRivalPriceFor(Material material) {
        int barryPrice = getPriceFor(material);
        if (barryPrice < 0) return -1;
        return Math.max(0, (int) (barryPrice * RIVAL_PRICE_MULTIPLIER));
    }

    /**
     * Attempt to sell to Terry. Has a 25% chance that Terry steals an item from the
     * player's inventory as well.
     *
     * @param material            the item being sold
     * @param inventory           the player's inventory
     * @param hour                current in-game hour
     * @param dayCount            current in-game day count
     * @param achievementCallback callback for awarding achievements, may be null
     * @return the result (uses SellResult enum; SUCCESS/SUCCESS_WITH_SUSPICION/etc.)
     */
    public SellResult sellToRival(Material material, Inventory inventory,
                                  float hour, int dayCount,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        if (!rivalActive) return SellResult.BARRY_UNAVAILABLE;
        if (!isWorkingDay(dayCount)) return SellResult.BARRY_UNAVAILABLE;
        if (!isOperatingHour(hour)) return SellResult.BARRY_UNAVAILABLE;
        if (!inventory.hasItem(material)) return SellResult.ITEM_NOT_IN_INVENTORY;

        int price = getRivalPriceFor(material);
        if (price < 0) return SellResult.ITEM_REFUSED;

        inventory.removeItem(material, 1);
        inventory.addItem(Material.COIN, price);

        // 25% chance Terry steals an item
        if (rng.nextFloat() < RIVAL_STEAL_CHANCE) {
            // Steal one COIN from the player if available
            if (inventory.hasItem(Material.COIN)) {
                inventory.removeItem(Material.COIN, 1);
            }
        }

        return SellResult.SUCCESS;
    }

    // ── Tyre Slashing ─────────────────────────────────────────────────────────

    /**
     * Returns whether the current hour is within the tyre-slash window (02:00–06:00).
     *
     * @param hour current in-game hour
     * @return true if valid slash window
     */
    public boolean isSlashWindow(float hour) {
        return hour >= SLASH_START_HOUR && hour < SLASH_END_HOUR;
    }

    /**
     * Player attempts to slash Barry's van tyres with a PENKNIFE.
     *
     * @param inventory           the player's inventory
     * @param hour                current in-game hour
     * @param dayCount            current in-game day count
     * @param achievementCallback callback for awarding achievements, may be null
     * @param barryNpc            Barry's NPC instance (for rumour seeding), may be null
     * @return the result of the slash attempt
     */
    public SlashTyresResult slashTyres(Inventory inventory, float hour, int dayCount,
                                       NotorietySystem.AchievementCallback achievementCallback,
                                       NPC barryNpc) {
        if (tyresSlashed) return SlashTyresResult.ALREADY_SLASHED;
        if (!isSlashWindow(hour)) return SlashTyresResult.WRONG_TIME;
        if (!inventory.hasItem(Material.PENKNIFE)) return SlashTyresResult.NO_PENKNIFE;
        if (!isWorkingDay(dayCount)) return SlashTyresResult.VAN_NOT_PRESENT;

        // Consume penknife
        inventory.removeItem(Material.PENKNIFE, 1);
        tyresSlashed = true;
        rivalActive = true;
        barryActive = false;

        // Record vandalism
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.CRIMINAL_DAMAGE);
        }

        return SlashTyresResult.SUCCESS;
    }

    // ── Van repair ────────────────────────────────────────────────────────────

    /**
     * Player repairs Barry's slashed van using a RUBBER_TYRE. Awards BARRY_S_MATE
     * achievement and a 10 COIN reward from Barry.
     *
     * @param inventory           the player's inventory
     * @param hour                current in-game hour
     * @param dayCount            current in-game day count
     * @param achievementCallback callback for awarding achievements, may be null
     * @return the result of the repair attempt
     */
    public RepairVanResult repairVan(Inventory inventory, float hour, int dayCount,
                                     NotorietySystem.AchievementCallback achievementCallback) {
        if (!tyresSlashed) return RepairVanResult.NOT_SLASHED;
        if (!inventory.hasItem(Material.RUBBER_TYRE)) return RepairVanResult.NO_RUBBER_TYRE;

        // Consume tyre and pay the player
        inventory.removeItem(Material.RUBBER_TYRE, 1);
        inventory.addItem(Material.COIN, REPAIR_VAN_REWARD);

        // Restore Barry
        tyresSlashed = false;
        rivalActive = false;

        // Unlock BARRY_S_MATE
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.BARRY_S_MATE);
        }

        return RepairVanResult.SUCCESS;
    }

    // ── Council Enforcement ───────────────────────────────────────────────────

    /**
     * Returns whether a COUNCIL_ENFORCEMENT officer is patrolling today.
     */
    public boolean isEnforcementActive() {
        return enforcementActive;
    }

    /**
     * Player warns Barry about incoming enforcement, causing him to move on before
     * being caught. Barry rewards the player with 3 COIN.
     *
     * @param inventory  the player's inventory
     * @param hour       current in-game hour
     * @param dayCount   current in-game day count
     * @return the result of the warning
     */
    public WarnBarryResult warnBarry(Inventory inventory, float hour, int dayCount) {
        if (!enforcementActive) return WarnBarryResult.NO_THREAT;
        if (!isBarryAvailable(hour, dayCount)) return WarnBarryResult.BARRY_UNAVAILABLE;

        inventory.addItem(Material.COIN, WARN_BARRY_REWARD);
        enforcementActive = false;

        return WarnBarryResult.SUCCESS;
    }

    /**
     * Player bribes the COUNCIL_ENFORCEMENT officer with a TIN_OF_BEANS.
     * 60% success rate; failure adds Notoriety +3.
     *
     * @param inventory           the player's inventory
     * @param achievementCallback callback for awarding achievements, may be null
     * @return the result of the bribe attempt
     */
    public BribeResult bribeOfficer(Inventory inventory,
                                    NotorietySystem.AchievementCallback achievementCallback) {
        if (!enforcementActive) return BribeResult.NO_OFFICER;
        if (!inventory.hasItem(Material.TIN_OF_BEANS)) return BribeResult.NO_BEANS;

        inventory.removeItem(Material.TIN_OF_BEANS, 1);

        if (rng.nextFloat() < BRIBE_SUCCESS_CHANCE) {
            enforcementActive = false;
            barryLicenced = true;
            return BribeResult.SUCCESS;
        } else {
            // Failed bribe — notoriety increase
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(BRIBE_FAIL_NOTORIETY, achievementCallback);
            }
            return BribeResult.FAILED;
        }
    }

    /**
     * Player presents a FORGED_LICENCE to the COUNCIL_ENFORCEMENT officer.
     * Consumes the licence and clears the enforcement threat.
     *
     * @param inventory  the player's inventory
     * @return true if the forged licence was accepted (always accepted)
     */
    public boolean presentForgedLicence(Inventory inventory) {
        if (!enforcementActive) return false;
        if (!inventory.hasItem(Material.FORGED_LICENCE)) return false;

        inventory.removeItem(Material.FORGED_LICENCE, 1);
        enforcementActive = false;
        barryLicenced = true;
        return true;
    }

    /**
     * Impounds Barry's van when enforcement succeeds. Seeds BARRY_NICKED rumour.
     *
     * @param barryNpc            Barry's NPC instance, may be null
     * @param achievementCallback callback for awarding achievements, may be null
     */
    public void impoundVan(NPC barryNpc, NotorietySystem.AchievementCallback achievementCallback) {
        barryImpounded = true;
        impoundHoursRemaining = IMPOUND_DURATION_HOURS;
        barryActive = false;
        rivalActive = false;
        enforcementActive = false;

        // Seed BARRY_NICKED rumour
        if (rumourNetwork != null) {
            NPC source = (barryNpc != null) ? barryNpc
                    : new NPC(NPCType.RAG_AND_BONE_MAN, 0f, 1f, 0f);
            rumourNetwork.addRumour(source,
                    new Rumour(RumourType.BARRY_NICKED,
                            "Barry got nicked — council took his van. No rag-and-bone round this week."));
        }
    }

    // ── Door-Knock Pre-buy ────────────────────────────────────────────────────

    /**
     * Returns whether the door-knock pre-buy window is active (07:00–09:00).
     *
     * @param hour current in-game hour
     * @return true if within the pre-buy window
     */
    public boolean isDoorKnockWindow(float hour) {
        return hour >= DOORKNOCK_START_HOUR && hour < DOORKNOCK_END_HOUR;
    }

    /**
     * Completes a door-knock pre-buy transaction. The player adds a JUNK_ITEM to
     * inventory; tracks progress toward {@link AchievementType#KNOCKER_BOY}.
     *
     * @param inventory           the player's inventory
     * @param hour                current in-game hour
     * @param achievementCallback callback for awarding achievements, may be null
     * @return true if the door-knock window was active and the item was added
     */
    public boolean doorKnock(Inventory inventory, float hour,
                             NotorietySystem.AchievementCallback achievementCallback) {
        if (!isDoorKnockWindow(hour)) return false;

        inventory.addItem(Material.JUNK_ITEM, 1);
        doorKnockCount++;

        if (achievementCallback != null
                && doorKnockCount >= AchievementType.KNOCKER_BOY.getProgressTarget()) {
            achievementCallback.award(AchievementType.KNOCKER_BOY);
        }

        return true;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns whether Barry is currently active on his round. */
    public boolean isBarryActive() { return barryActive; }

    /** Returns whether Barry's van tyres have been slashed. */
    public boolean isTyresSlashed() { return tyresSlashed; }

    /** Returns whether Barry's van is currently impounded. */
    public boolean isBarryImpounded() { return barryImpounded; }

    /** Returns remaining impound time in hours. */
    public float getImpoundHoursRemaining() { return impoundHoursRemaining; }

    /** Returns whether Barry has a licence for today. */
    public boolean isBarryLicenced() { return barryLicenced; }

    /** Returns the current stop index (0–5). */
    public int getCurrentStop() { return currentStop; }

    /** Returns the number of COPPER_WIRE sold in the current visit. */
    public int getCopperWireSoldThisVisit() { return copperWireSoldThisVisit; }

    /** Returns total scrap transactions with Barry. */
    public int getTotalScrapsCount() { return totalScrapsCount; }

    /** Returns total door-knock pre-buy completions. */
    public int getDoorKnockCount() { return doorKnockCount; }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /** Force Barry's active state for testing. */
    public void setBarryActiveForTesting(boolean active) { this.barryActive = active; }

    /** Force tyres-slashed state for testing. */
    public void setTyresSlashedForTesting(boolean slashed) { this.tyresSlashed = slashed; }

    /** Force rival-active state for testing. */
    public void setRivalActiveForTesting(boolean active) { this.rivalActive = active; }

    /** Force barry-impounded state for testing. */
    public void setBarryImpoundedForTesting(boolean impounded, float hours) {
        this.barryImpounded = impounded;
        this.impoundHoursRemaining = hours;
    }

    /** Force enforcement-active state for testing. */
    public void setEnforcementActiveForTesting(boolean active) { this.enforcementActive = active; }

    /** Force barry-licenced state for testing. */
    public void setBarryLicencedForTesting(boolean licenced) { this.barryLicenced = licenced; }

    /** Force copper wire count for testing. */
    public void setCopperWireSoldForTesting(int count) { this.copperWireSoldThisVisit = count; }

    /** Force total scraps count for testing. */
    public void setTotalScrapsCountForTesting(int count) { this.totalScrapsCount = count; }

    /** Force door-knock count for testing. */
    public void setDoorKnockCountForTesting(int count) { this.doorKnockCount = count; }
}

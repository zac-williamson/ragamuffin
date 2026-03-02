package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;

import java.util.Random;

/**
 * Issue #1122: Northfield Tanning &amp; Massage — Sun Kissed Studio, the Happy Ending
 * Economy &amp; the Marchetti Money Laundering Front.
 *
 * <p>Sun Kissed Studio ({@link ragamuffin.world.LandmarkType#SUN_KISSED_STUDIO}) is a
 * narrow 6×8×3 shopfront on the high-street parade between the nail salon and the
 * payday loan shop.  Run by Tracey ({@link NPCType#SALON_OWNER}), with massage
 * therapists Jade and Tanya ({@link NPCType#MASSAGE_THERAPIST}).
 *
 * <h3>Opening Hours</h3>
 * <ul>
 *   <li>Mon–Sat 09:00–21:00.</li>
 *   <li>Sun 11:00–18:00.</li>
 * </ul>
 *
 * <h3>Services</h3>
 * <ul>
 *   <li>6-min sunbed — 2 COIN (1 COIN on sunny days).  Grants {@code TANNED} buff:
 *       −2 effective Notoriety display for 1 in-game day.</li>
 *   <li>12-min sunbed — 3 COIN (2 COIN on sunny days).  Grants {@code DEEPLY_TANNED} buff:
 *       −3 effective Notoriety display for 2 in-game days.</li>
 *   <li>Swedish massage — 5 COIN.  Restores +30 Health.</li>
 *   <li>Hot stone massage — 8 COIN.  Restores +50 Health + {@code RELAXED} noise-reduction
 *       buff (NoiseSystem integration).</li>
 *   <li>Special Services — 10 COIN at Street Rep ≥ {@link #SPECIAL_SERVICES_REP_THRESHOLD}.
 *       Removes 10 Notoriety; disables WitnessSystem for
 *       {@link #SPECIAL_SERVICES_WITNESS_SUPPRESS_MINUTES} in-game minutes.</li>
 * </ul>
 *
 * <h3>Marchetti Front</h3>
 * <ul>
 *   <li>Tracey accepts cash drops at 11:00 and 18:00 daily.</li>
 *   <li>Player can <b>intercept</b> a {@link Material#BROWN_ENVELOPE} drop:
 *       −{@link #INTERCEPT_MARCHETTI_RESPECT_PENALTY} Marchetti Respect,
 *       +{@link #INTERCEPT_COIN_MIN}–{@link #INTERCEPT_COIN_MAX} COIN.</li>
 *   <li>Player can <b>deliver</b> the envelope to Tracey:
 *       +{@link #DELIVER_MARCHETTI_RESPECT_GAIN} Marchetti Respect,
 *       +{@link #DELIVER_COIN_REWARD} COIN.</li>
 *   <li>The {@link Material#MARCHETTI_LEDGER} in the back-room safe can be stolen and
 *       delivered to the police station to trigger a 24-hour raid closure — but marks
 *       the player as GRASS (−{@link #GRASS_MARCHETTI_RESPECT_PENALTY} Marchetti
 *       Respect).</li>
 * </ul>
 *
 * <h3>DisguiseSystem integration</h3>
 * The {@code TANNED} buff stacks with a new hairstyle for a combined −20% police
 * recognition window when both effects are active simultaneously.
 *
 * <h3>Weather integration</h3>
 * <ul>
 *   <li>COLD_SNAP: best warmth shelter on high street (+{@link #COLD_SNAP_WARMTH_RATE}/min
 *       above the base indoor rate).</li>
 *   <li>Sunny days (CLEAR or HEATWAVE): sunbed price reduced by 1 COIN.</li>
 *   <li>Rainy days (RAIN or DRIZZLE): Jade gossips and seeds
 *       {@link RumourType#LOCAL_EVENT} rumours.</li>
 * </ul>
 *
 * <h3>Integrations</h3>
 * <ul>
 *   <li>{@link FactionSystem} — Marchetti Crew cash drops; Respect gates special
 *       services refusal and GRASS flag.</li>
 *   <li>{@link NotorietySystem} — TANNED/DEEPLY_TANNED notoriety display reductions;
 *       Special Services removes 10 Notoriety.</li>
 *   <li>{@link WitnessSystem} — Special Services suppresses witness reporting for a
 *       timed window (tracked internally as {@link #witnessSuppressionRemaining}).</li>
 *   <li>{@link RumourNetwork} — Jade seeds LOCAL_EVENT on rainy days; intercept and
 *       GRASS events seed GRASSED_UP.</li>
 *   <li>{@link AchievementSystem} — 5 new achievements.</li>
 *   <li>{@link NeighbourhoodSystem} — shop boards up when vibes &lt;
 *       {@link #BOARD_UP_VIBES_THRESHOLD}.</li>
 *   <li>{@link DisguiseSystem} — TANNED buff contributes −20% police recognition
 *       when stacked with hairstyle.</li>
 * </ul>
 */
public class TanningSalonSystem {

    // ── Day-of-week constants (dayCount % 7; 0=Mon … 5=Sat, 6=Sun) ───────────
    private static final int SUNDAY = 6;

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Opening hour Mon–Sat. */
    public static final float WEEKDAY_OPEN_HOUR  = 9.0f;
    /** Closing hour Mon–Sat. */
    public static final float WEEKDAY_CLOSE_HOUR = 21.0f;

    /** Opening hour on Sunday. */
    public static final float SUNDAY_OPEN_HOUR  = 11.0f;
    /** Closing hour on Sunday. */
    public static final float SUNDAY_CLOSE_HOUR = 18.0f;

    // ── Marchetti cash-drop hours ─────────────────────────────────────────────

    /** First daily cash-drop hour (11:00). */
    public static final float CASH_DROP_HOUR_1 = 11.0f;
    /** Second daily cash-drop hour (18:00). */
    public static final float CASH_DROP_HOUR_2 = 18.0f;

    // ── Service prices ────────────────────────────────────────────────────────

    /** Base price for a 6-minute sunbed session. */
    public static final int PRICE_SUNBED_6MIN = 2;
    /** Base price for a 12-minute sunbed session. */
    public static final int PRICE_SUNBED_12MIN = 3;
    /** Price for a Swedish massage. */
    public static final int PRICE_SWEDISH_MASSAGE = 5;
    /** Price for a hot stone massage. */
    public static final int PRICE_HOT_STONE_MASSAGE = 8;
    /** Price for Special Services (Street Rep ≥ {@link #SPECIAL_SERVICES_REP_THRESHOLD} required). */
    public static final int PRICE_SPECIAL_SERVICES = 10;

    /** Sunny-day discount on sunbed prices (reduces price by this amount, minimum 1 COIN). */
    public static final int SUNNY_DAY_DISCOUNT = 1;

    // ── Buff durations (in-game days) ─────────────────────────────────────────

    /** Duration of the TANNED notoriety display reduction, in in-game days. */
    public static final float TANNED_BUFF_DAYS = 1.0f;
    /** Duration of the DEEPLY_TANNED notoriety display reduction, in in-game days. */
    public static final float DEEPLY_TANNED_BUFF_DAYS = 2.0f;

    // ── Buff effects ─────────────────────────────────────────────────────────

    /** Notoriety display reduction while TANNED buff is active. */
    public static final int TANNED_NOTORIETY_REDUCTION = 2;
    /** Notoriety display reduction while DEEPLY_TANNED buff is active. */
    public static final int DEEPLY_TANNED_NOTORIETY_REDUCTION = 3;

    /** Health restored by Swedish massage. */
    public static final float SWEDISH_MASSAGE_HEALTH = 30f;
    /** Health restored by hot stone massage. */
    public static final float HOT_STONE_MASSAGE_HEALTH = 50f;

    /** Notoriety removed by Special Services. */
    public static final int SPECIAL_SERVICES_NOTORIETY_REMOVAL = 10;

    /** In-game minutes WitnessSystem is suppressed after Special Services. */
    public static final float SPECIAL_SERVICES_WITNESS_SUPPRESS_MINUTES = 60f;

    // ── DisguiseSystem stacking ────────────────────────────────────────────────

    /**
     * Police-recognition reduction fraction contributed by the TANNED buff when
     * stacked with an active hairstyle recognition window (e.g. from BarberSystem).
     * Combined effect is: base hairstyle reduction + this value.
     */
    public static final float TANNED_DISGUISE_STACK_REDUCTION = 0.20f;

    // ── Street Rep threshold ───────────────────────────────────────────────────

    /** Minimum StreetReputation points required to access Special Services. */
    public static final int SPECIAL_SERVICES_REP_THRESHOLD = 40;

    // ── Marchetti interaction constants ──────────────────────────────────────

    /** Marchetti Respect penalty for intercepting a cash drop. */
    public static final int INTERCEPT_MARCHETTI_RESPECT_PENALTY = 20;
    /** Minimum COIN gained from intercepting a cash drop. */
    public static final int INTERCEPT_COIN_MIN = 15;
    /** Maximum COIN gained from intercepting a cash drop. */
    public static final int INTERCEPT_COIN_MAX = 25;

    /** Marchetti Respect gained for delivering a cash drop to Tracey. */
    public static final int DELIVER_MARCHETTI_RESPECT_GAIN = 3;
    /** COIN reward for delivering a cash drop. */
    public static final int DELIVER_COIN_REWARD = 5;

    /** Marchetti Respect penalty for delivering the MARCHETTI_LEDGER to police (GRASS). */
    public static final int GRASS_MARCHETTI_RESPECT_PENALTY = 40;

    // ── WarmthSystem: COLD_SNAP bonus ─────────────────────────────────────────

    /**
     * Additional warmth restoration per real second when sheltering inside the salon
     * during COLD_SNAP weather (on top of {@link WarmthSystem#INDOOR_WARMTH_RATE}).
     */
    public static final float COLD_SNAP_WARMTH_RATE = 8.0f;

    // ── NeighbourhoodSystem ────────────────────────────────────────────────────

    /** Neighbourhood vibes threshold below which the shop boards up its window. */
    public static final int BOARD_UP_VIBES_THRESHOLD = 30;

    // ── Visit tracking (for SUN_KISSED achievement) ────────────────────────────

    /** Number of sunbed visits required to unlock the SUN_KISSED achievement. */
    public static final int SUN_KISSED_VISIT_COUNT = 5;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Remaining in-game minutes for the TANNED buff (6-min sunbed). */
    private float tannedBuffRemaining = 0f;

    /** Remaining in-game minutes for the DEEPLY_TANNED buff (12-min sunbed). */
    private float deeplyTannedBuffRemaining = 0f;

    /** Remaining in-game minutes for the RELAXED buff (hot stone massage). */
    private float relaxedBuffRemaining = 0f;

    /** Remaining in-game minutes for the WitnessSystem suppression window. */
    private float witnessSuppressionRemaining = 0f;

    /** Total number of sunbed visits by the player (for SUN_KISSED achievement). */
    private int totalSunbedVisits = 0;

    /** Whether a BROWN_ENVELOPE drop is currently available for interaction. */
    private boolean dropPending = false;

    /** Whether the MARCHETTI_LEDGER has already been extracted from the back-room safe. */
    private boolean ledgerRemoved = false;

    /** Whether the salon is currently under 24-hour raid closure. */
    private boolean raidClosed = false;

    /** Remaining real-seconds for the raid closure. */
    private float raidClosureRemaining = 0f;

    /** In-game seconds for a full raid closure duration. */
    public static final float RAID_CLOSURE_SECONDS = 86400f; // 24 in-game hours × 3600

    /** Random instance for COIN intercept range. */
    private final Random random;

    // ── Construction ──────────────────────────────────────────────────────────

    public TanningSalonSystem() {
        this.random = new Random();
    }

    public TanningSalonSystem(Random random) {
        this.random = random;
    }

    // ── Opening hours ─────────────────────────────────────────────────────────

    /**
     * Returns true if the salon is open at the given hour and day.
     *
     * @param hour      current in-game hour (0–24)
     * @param dayOfWeek derived from {@code timeSystem.getDayCount() % 7}
     *                  (0=Mon … 5=Sat, 6=Sun)
     * @return true if open for business
     */
    public boolean isOpen(float hour, int dayOfWeek) {
        if (raidClosed) return false;
        if (dayOfWeek == SUNDAY) {
            return hour >= SUNDAY_OPEN_HOUR && hour < SUNDAY_CLOSE_HOUR;
        }
        return hour >= WEEKDAY_OPEN_HOUR && hour < WEEKDAY_CLOSE_HOUR;
    }

    // ── Service results ───────────────────────────────────────────────────────

    /**
     * Result of a service purchase attempt.
     */
    public enum ServiceResult {
        /** Service completed successfully. */
        SUCCESS,
        /** Player cannot afford the service. */
        INSUFFICIENT_FUNDS,
        /** Shop is closed at this time. */
        SHOP_CLOSED,
        /** Street Rep too low for Special Services. */
        INSUFFICIENT_REP,
        /** Player is marked as GRASS — Tracey refuses service. */
        REFUSED_GRASS
    }

    /**
     * The types of service available at the salon.
     */
    public enum ServiceType {
        SUNBED_6MIN(PRICE_SUNBED_6MIN),
        SUNBED_12MIN(PRICE_SUNBED_12MIN),
        SWEDISH_MASSAGE(PRICE_SWEDISH_MASSAGE),
        HOT_STONE_MASSAGE(PRICE_HOT_STONE_MASSAGE),
        SPECIAL_SERVICES(PRICE_SPECIAL_SERVICES);

        private final int baseCost;

        ServiceType(int baseCost) {
            this.baseCost = baseCost;
        }

        public int getBaseCost() {
            return baseCost;
        }
    }

    /**
     * Calculate the actual price for the given service, accounting for weather discounts.
     *
     * @param service the service type
     * @param weather the current weather
     * @return COIN cost
     */
    public int getPrice(ServiceType service, Weather weather) {
        int base = service.getBaseCost();
        if ((service == ServiceType.SUNBED_6MIN || service == ServiceType.SUNBED_12MIN)
                && (weather == Weather.CLEAR || weather == Weather.HEATWAVE)) {
            return Math.max(1, base - SUNNY_DAY_DISCOUNT);
        }
        return base;
    }

    /**
     * Perform a service purchase for the player.
     *
     * <p>On SUCCESS:
     * <ol>
     *   <li>Deducts COIN from inventory.</li>
     *   <li>Applies buff/health effect to player.</li>
     *   <li>Awards relevant achievements.</li>
     *   <li>Seeds rumours if applicable.</li>
     * </ol>
     *
     * @param service         the service requested
     * @param player          the player
     * @param inventory       the player's inventory (must contain enough COIN)
     * @param hour            current in-game hour
     * @param dayOfWeek       0=Mon … 5=Sat, 6=Sun
     * @param weather         current weather
     * @param notorietySystem for applying notoriety reductions
     * @param achievementCb   callback for awarding achievements (may be null)
     * @param jadeNpc         Jade's NPC for rainy-day gossip (may be null)
     * @param rumourNetwork   rumour network (may be null)
     * @param isGrass         whether the player is currently marked as GRASS
     * @param streetRepPoints player's current StreetReputation point score
     * @return the result of the service attempt
     */
    public ServiceResult purchaseService(ServiceType service,
                                         Player player,
                                         Inventory inventory,
                                         float hour,
                                         int dayOfWeek,
                                         Weather weather,
                                         NotorietySystem notorietySystem,
                                         NotorietySystem.AchievementCallback achievementCb,
                                         NPC jadeNpc,
                                         RumourNetwork rumourNetwork,
                                         boolean isGrass,
                                         int streetRepPoints) {
        if (!isOpen(hour, dayOfWeek)) {
            return ServiceResult.SHOP_CLOSED;
        }

        // Tracey refuses service to a GRASS player
        if (isGrass) {
            return ServiceResult.REFUSED_GRASS;
        }

        // Special Services require sufficient Street Rep
        if (service == ServiceType.SPECIAL_SERVICES
                && streetRepPoints < SPECIAL_SERVICES_REP_THRESHOLD) {
            return ServiceResult.INSUFFICIENT_REP;
        }

        int cost = getPrice(service, weather);
        if (!inventory.hasItem(Material.COIN, cost)) {
            return ServiceResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, cost);

        // Apply effects
        switch (service) {
            case SUNBED_6MIN:
                applyTannedBuff();
                totalSunbedVisits++;
                awardSunbedAchievements(achievementCb);
                seedRainyGossip(weather, jadeNpc, rumourNetwork);
                break;

            case SUNBED_12MIN:
                applyDeeplyTannedBuff();
                totalSunbedVisits++;
                awardSunbedAchievements(achievementCb);
                seedRainyGossip(weather, jadeNpc, rumourNetwork);
                break;

            case SWEDISH_MASSAGE:
                player.heal(SWEDISH_MASSAGE_HEALTH);
                break;

            case HOT_STONE_MASSAGE:
                player.heal(HOT_STONE_MASSAGE_HEALTH);
                applyRelaxedBuff();
                break;

            case SPECIAL_SERVICES:
                if (notorietySystem != null && achievementCb != null) {
                    notorietySystem.reduceNotoriety(SPECIAL_SERVICES_NOTORIETY_REMOVAL, achievementCb);
                } else if (notorietySystem != null) {
                    notorietySystem.reduceNotoriety(SPECIAL_SERVICES_NOTORIETY_REMOVAL, null);
                }
                witnessSuppressionRemaining = SPECIAL_SERVICES_WITNESS_SUPPRESS_MINUTES;
                if (achievementCb != null) {
                    achievementCb.award(AchievementType.SPECIAL_APPOINTMENT);
                }
                break;
        }

        return ServiceResult.SUCCESS;
    }

    // ── Buff application ──────────────────────────────────────────────────────

    private void applyTannedBuff() {
        // TANNED lasts 1 in-game day; expressed in in-game minutes (1 day = 24h × 60 = 1440 min)
        tannedBuffRemaining = TANNED_BUFF_DAYS * 24f * 60f;
        // DEEPLY_TANNED is cleared when a shorter session is used
        deeplyTannedBuffRemaining = 0f;
    }

    private void applyDeeplyTannedBuff() {
        deeplyTannedBuffRemaining = DEEPLY_TANNED_BUFF_DAYS * 24f * 60f;
        // Upgrade replaces the shorter buff
        tannedBuffRemaining = 0f;
    }

    private void applyRelaxedBuff() {
        // RELAXED lasts 60 in-game minutes
        relaxedBuffRemaining = 60f;
    }

    private void awardSunbedAchievements(NotorietySystem.AchievementCallback achievementCb) {
        if (achievementCb == null) return;
        achievementCb.award(AchievementType.BRONZED);
        if (totalSunbedVisits >= SUN_KISSED_VISIT_COUNT) {
            achievementCb.award(AchievementType.SUN_KISSED);
        }
    }

    private void seedRainyGossip(Weather weather, NPC jadeNpc, RumourNetwork rumourNetwork) {
        if ((weather == Weather.RAIN || weather == Weather.DRIZZLE)
                && jadeNpc != null && rumourNetwork != null) {
            rumourNetwork.addRumour(jadeNpc,
                    new Rumour(RumourType.LOCAL_EVENT,
                            "Jade from Sun Kissed Studio was saying it's been dead quiet — weather keeping everyone indoors"));
        }
    }

    // ── Marchetti cash-drop mechanics ─────────────────────────────────────────

    /**
     * Called by the time system each in-game hour tick.  Spawns a BROWN_ENVELOPE
     * drop at 11:00 and 18:00 when the salon is open and no drop is already pending.
     *
     * @param hour current in-game hour (integer, 0–23)
     */
    public void onHourTick(int hour) {
        if (!dropPending && (hour == (int) CASH_DROP_HOUR_1 || hour == (int) CASH_DROP_HOUR_2)) {
            dropPending = true;
        }
    }

    /**
     * Intercept the pending cash drop.  Player claims the BROWN_ENVELOPE for coin
     * at the cost of Marchetti Respect.
     *
     * @param inventory     the player's inventory
     * @param factionSystem for applying the Respect penalty
     * @return true if a drop was available and intercepted
     */
    public boolean interceptDrop(Inventory inventory, FactionSystem factionSystem) {
        if (!dropPending) return false;
        dropPending = false;
        int coin = INTERCEPT_COIN_MIN + random.nextInt(INTERCEPT_COIN_MAX - INTERCEPT_COIN_MIN + 1);
        inventory.addItem(Material.COIN, coin);
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, -INTERCEPT_MARCHETTI_RESPECT_PENALTY);
        }
        return true;
    }

    /**
     * Deliver the pending cash drop to Tracey.  Grants Respect and COIN reward.
     *
     * @param inventory     the player's inventory
     * @param factionSystem for applying the Respect gain
     * @param achievementCb callback for the CLEAN_MONEY achievement (may be null)
     * @return true if the delivery was accepted
     */
    public boolean deliverDrop(Inventory inventory,
                                FactionSystem factionSystem,
                                NotorietySystem.AchievementCallback achievementCb) {
        if (!dropPending) return false;
        dropPending = false;
        inventory.addItem(Material.COIN, DELIVER_COIN_REWARD);
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, DELIVER_MARCHETTI_RESPECT_GAIN);
        }
        if (achievementCb != null) {
            achievementCb.award(AchievementType.CLEAN_MONEY);
        }
        return true;
    }

    /**
     * Steal the MARCHETTI_LEDGER from the back-room safe.
     * The ledger item is added to the player's inventory.
     *
     * @param inventory the player's inventory
     * @return true if the ledger was available (not already removed)
     */
    public boolean stealLedger(Inventory inventory) {
        if (ledgerRemoved) return false;
        ledgerRemoved = true;
        inventory.addItem(Material.MARCHETTI_LEDGER, 1);
        return true;
    }

    /**
     * Deliver the MARCHETTI_LEDGER to the police station.
     * Triggers a 24-hour raid closure, marks player as GRASS, and penalises
     * Marchetti Respect.
     *
     * @param inventory       the player's inventory (ledger is consumed)
     * @param factionSystem   for applying the GRASS Respect penalty
     * @param rumourNetwork   for seeding a GRASSED_UP rumour (may be null)
     * @param npc             the police NPC receiving the ledger (for rumour origin; may be null)
     * @param achievementCb   callback for the LAUNDERED achievement (may be null)
     * @return true if the ledger was present in inventory and delivered
     */
    public boolean deliverLedgerToPolice(Inventory inventory,
                                          FactionSystem factionSystem,
                                          RumourNetwork rumourNetwork,
                                          NPC npc,
                                          NotorietySystem.AchievementCallback achievementCb) {
        if (!inventory.hasItem(Material.MARCHETTI_LEDGER, 1)) return false;
        inventory.removeItem(Material.MARCHETTI_LEDGER, 1);
        // Trigger 24-hour raid closure
        raidClosed = true;
        raidClosureRemaining = RAID_CLOSURE_SECONDS;
        // Penalise Marchetti Respect
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, -GRASS_MARCHETTI_RESPECT_PENALTY);
        }
        // Seed GRASSED_UP rumour
        if (rumourNetwork != null && npc != null) {
            rumourNetwork.addRumour(npc,
                    new Rumour(RumourType.GRASSED_UP,
                            "Someone handed over the books on Sun Kissed Studio — Marchetti lot are furious"));
        }
        if (achievementCb != null) {
            achievementCb.award(AchievementType.LAUNDERED);
        }
        return true;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Update timed buffs and closures each frame.
     *
     * @param delta      seconds since last frame (real time)
     * @param timeSystem used to convert real seconds to in-game minutes
     */
    public void update(float delta, TimeSystem timeSystem) {
        float inGameMinutesPerRealSecond = timeSystem.getTimeSpeed() * 60f;
        float inGameMinutes = delta * inGameMinutesPerRealSecond;

        if (tannedBuffRemaining > 0f) {
            tannedBuffRemaining -= inGameMinutes;
            if (tannedBuffRemaining < 0f) tannedBuffRemaining = 0f;
        }
        if (deeplyTannedBuffRemaining > 0f) {
            deeplyTannedBuffRemaining -= inGameMinutes;
            if (deeplyTannedBuffRemaining < 0f) deeplyTannedBuffRemaining = 0f;
        }
        if (relaxedBuffRemaining > 0f) {
            relaxedBuffRemaining -= inGameMinutes;
            if (relaxedBuffRemaining < 0f) relaxedBuffRemaining = 0f;
        }
        if (witnessSuppressionRemaining > 0f) {
            witnessSuppressionRemaining -= inGameMinutes;
            if (witnessSuppressionRemaining < 0f) witnessSuppressionRemaining = 0f;
        }
        // Raid closure ticks in real seconds (it's a long-duration effect)
        if (raidClosed && raidClosureRemaining > 0f) {
            raidClosureRemaining -= delta;
            if (raidClosureRemaining <= 0f) {
                raidClosureRemaining = 0f;
                raidClosed = false;
            }
        }
    }

    // ── Buff queries ─────────────────────────────────────────────────────────

    /** Returns true if the TANNED buff is currently active. */
    public boolean isTanned() {
        return tannedBuffRemaining > 0f;
    }

    /** Returns true if the DEEPLY_TANNED buff is currently active. */
    public boolean isDeeplyTanned() {
        return deeplyTannedBuffRemaining > 0f;
    }

    /** Returns true if the RELAXED buff is currently active. */
    public boolean isRelaxed() {
        return relaxedBuffRemaining > 0f;
    }

    /** Returns true if WitnessSystem should be suppressed (Special Services window active). */
    public boolean isWitnessSuppressed() {
        return witnessSuppressionRemaining > 0f;
    }

    /**
     * Returns the effective Notoriety display reduction from active tanning buffs.
     * Only one buff is active at a time; DEEPLY_TANNED takes precedence.
     */
    public int getNotorietyDisplayReduction() {
        if (isDeeplyTanned()) return DEEPLY_TANNED_NOTORIETY_REDUCTION;
        if (isTanned()) return TANNED_NOTORIETY_REDUCTION;
        return 0;
    }

    /**
     * Returns the police-recognition reduction fraction when the TANNED buff is active
     * and a hairstyle recognition window is also active (stacking bonus).
     * Returns 0 if TANNED is not active.
     */
    public float getTannedDisguiseStackReduction() {
        return (isTanned() || isDeeplyTanned()) ? TANNED_DISGUISE_STACK_REDUCTION : 0f;
    }

    /**
     * Returns the additional warmth rate bonus from sheltering inside the salon
     * during COLD_SNAP weather.
     *
     * @param weather the current weather
     * @return additional warmth rate in units/second; 0 if not applicable
     */
    public float getColdSnapWarmthBonus(Weather weather) {
        return weather == Weather.COLD_SNAP ? COLD_SNAP_WARMTH_RATE : 0f;
    }

    // ── State queries ─────────────────────────────────────────────────────────

    /** Returns true if a BROWN_ENVELOPE drop is currently pending interaction. */
    public boolean isDropPending() {
        return dropPending;
    }

    /** Returns true if the MARCHETTI_LEDGER has been removed from the back-room safe. */
    public boolean isLedgerRemoved() {
        return ledgerRemoved;
    }

    /** Returns true if the salon is currently under raid closure. */
    public boolean isRaidClosed() {
        return raidClosed;
    }

    /** Returns the total number of sunbed sessions purchased. */
    public int getTotalSunbedVisits() {
        return totalSunbedVisits;
    }

    /** Returns the remaining in-game minutes for the TANNED buff. */
    public float getTannedBuffRemaining() {
        return tannedBuffRemaining;
    }

    /** Returns the remaining in-game minutes for the DEEPLY_TANNED buff. */
    public float getDeeplyTannedBuffRemaining() {
        return deeplyTannedBuffRemaining;
    }

    /** Returns the remaining in-game minutes for the WitnessSystem suppression window. */
    public float getWitnessSuppressionRemaining() {
        return witnessSuppressionRemaining;
    }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /** Directly set the TANNED buff remaining (for testing). */
    public void setTannedBuffForTesting(float minutes) {
        this.tannedBuffRemaining = minutes;
    }

    /** Directly set the DEEPLY_TANNED buff remaining (for testing). */
    public void setDeeplyTannedBuffForTesting(float minutes) {
        this.deeplyTannedBuffRemaining = minutes;
    }

    /** Directly set the total sunbed visit count (for testing SUN_KISSED achievement). */
    public void setTotalSunbedVisitsForTesting(int count) {
        this.totalSunbedVisits = count;
    }

    /** Directly set the drop-pending flag (for testing). */
    public void setDropPendingForTesting(boolean pending) {
        this.dropPending = pending;
    }

    /** Directly set the witness suppression remaining (for testing). */
    public void setWitnessSuppressionForTesting(float minutes) {
        this.witnessSuppressionRemaining = minutes;
    }

    /** Directly set the raid-closed state (for testing). */
    public void setRaidClosedForTesting(boolean closed, float remainingSeconds) {
        this.raidClosed = closed;
        this.raidClosureRemaining = remainingSeconds;
    }
}

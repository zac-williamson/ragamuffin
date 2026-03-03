package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.core.NotorietySystem.AchievementCallback;
import ragamuffin.core.StreetSkillSystem.Skill;
import ragamuffin.core.StreetSkillSystem.Tier;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1365: Northfield Bonfire Night — The Park Bonfire, Penny for the Guy
 * &amp; the Tesco Car Park Display Sabotage.
 *
 * <h3>Overview</h3>
 * On day-of-year 308 (5 November), 18:00–23:00, Northfield comes alive with
 * fireworks, a communal park bonfire, a penny-for-the-guy effigy, Darren's
 * dodgy firework holdall, and the official Tesco car park display presided over
 * by compère Keith. THUNDERSTORM cancels the event. The event may shift to the
 * nearest Saturday if one falls within ±3 days of November 5th.
 *
 * <h3>Mechanic 1 — Park Bonfire</h3>
 * {@link ragamuffin.world.PropType#BONFIRE_PROP} spawns in the park at 18:30.
 * Provides a 10-block warmth radius. {@link NPCType#BONFIRE_WARDEN} Gary
 * patrols until 21:00, then gets distracted with his flask (detection radius
 * −50%). Replaced by {@link ragamuffin.world.PropType#COLD_EMBERS_PROP}
 * at 22:00.
 *
 * <h3>Mechanic 2 — Penny for the Guy</h3>
 * Player crafts {@link ragamuffin.world.PropType#GUY_PROP} from NEWSPAPER +
 * OLD_CLOTHES + WOOLLY_HAT and places it in the park between 17:00–19:30.
 * PUBLIC and PENSIONER NPCs donate 1 COIN every 2 in-game minutes.
 * YOUTH_GANG NPCs have a 20% chance per minute to kick it over
 * ({@link AchievementType#PARTY_POOPER} if destroyed).
 * Collecting at least 1 donation unlocks {@link AchievementType#PENNY_FOR_THE_GUY}.
 *
 * <h3>Mechanic 3 — Darren the Firework Dealer</h3>
 * {@link NPCType#FIREWORK_DEALER_NPC} Darren spawns at 17:00 behind the
 * off-licence. Sells ROCKET_FIREWORK, BANGER_FIREWORK, and ROMAN_CANDLE at
 * inflated prices. His holdall is stealable with STEALTH ≥ 2; if stolen it
 * fences for 25 COIN and seeds {@link RumourType#FIREWORK_THEFT}. Darren
 * becomes HOSTILE if robbed or tipped off.
 *
 * <h3>Mechanic 4 — Personal Fireworks</h3>
 * Right-click firework items to launch. Noise 6.0–9.0 depending on type.
 * Misfire chance varies by type; RAIN doubles misfire chance. First police
 * sighting is a warning only. Second sighting records
 * {@link CrimeType#FIREWORK_OFFENCE} (+1 Wanted star, Notoriety +3).
 * Launching ≥3 fireworks without a FIREWORK_OFFENCE unlocks
 * {@link AchievementType#PYRO_NIGHT}.
 *
 * <h3>Mechanic 5 — Tesco Car Park Display Sabotage</h3>
 * {@link ragamuffin.world.PropType#FIREWORK_MORTAR_PROP} is active 19:45–23:00
 * with {@link NPCType#EVENT_COMPERE} Keith overseeing it.
 * Option A — early launch (before 20:00): Notoriety +5,
 * {@link AchievementType#SABOTEUR} achievement.
 * Option B — plant BANGER_FIREWORK (at 20:00 fires catastrophically):
 * Notoriety +8, CRIMINAL_DAMAGE, FIRE_ENGINE response,
 * {@link RumourType#FIREWORK_PRANK} seeded.
 *
 * <h3>Weather</h3>
 * THUNDERSTORM cancels the event entirely (fires
 * {@link EventType#THUNDERSTORM_CANCELS}).
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@link AchievementType#PENNY_FOR_THE_GUY} — collect ≥1 donation</li>
 *   <li>{@link AchievementType#PARTY_POOPER} — YOUTH_GANG destroys GUY_PROP</li>
 *   <li>{@link AchievementType#SABOTEUR} — plant BANGER in FIREWORK_MORTAR_PROP</li>
 *   <li>{@link AchievementType#PYRO_NIGHT} — 3 fireworks, no FIREWORK_OFFENCE</li>
 * </ul>
 */
public class BonfireNightSystem {

    // ── Day-of-year constant ──────────────────────────────────────────────────

    /** Bonfire Night day-of-year (5 November = day 308 in a non-leap year). */
    public static final int BONFIRE_NIGHT_DAY = 308;

    /** Maximum days to shift to a nearby Saturday (±3 days). */
    public static final int SATURDAY_SHIFT_MAX = 3;

    // ── Event hours ───────────────────────────────────────────────────────────

    /** Darren the firework dealer spawns at 17:00. */
    public static final float DARREN_SPAWN_HOUR = 17.0f;

    /** GUY_PROP placement window opens at 17:00. */
    public static final float GUY_PLACE_OPEN_HOUR = 17.0f;

    /** GUY_PROP placement window closes at 19:30. */
    public static final float GUY_PLACE_CLOSE_HOUR = 19.5f;

    /** Event window opens at 18:00. */
    public static final float EVENT_OPEN_HOUR = 18.0f;

    /** BONFIRE_PROP spawns at 18:30. */
    public static final float BONFIRE_SPAWN_HOUR = 18.5f;

    /** FIREWORK_MORTAR_PROP active from 19:45. */
    public static final float MORTAR_ACTIVE_HOUR = 19.75f;

    /** Planned official display start at 20:00. */
    public static final float DISPLAY_START_HOUR = 20.0f;

    /** BONFIRE_WARDEN Gary gets distracted with his flask at 21:00. */
    public static final float WARDEN_DISTRACTED_HOUR = 21.0f;

    /** BONFIRE_PROP replaced by COLD_EMBERS_PROP at 22:00. */
    public static final float BONFIRE_EMBERS_HOUR = 22.0f;

    /** Event window closes at 23:00. */
    public static final float EVENT_CLOSE_HOUR = 23.0f;

    // ── Warmth radius ─────────────────────────────────────────────────────────

    /** Warmth radius of the park bonfire (blocks). */
    public static final float BONFIRE_WARMTH_RADIUS = 10.0f;

    // ── Warden detection ──────────────────────────────────────────────────────

    /** Normal detection radius for BONFIRE_WARDEN Gary (blocks). */
    public static final float WARDEN_DETECTION_RADIUS = 8.0f;

    /** Detection radius after Gary gets distracted with his flask (−50%). */
    public static final float WARDEN_DISTRACTED_RADIUS = 4.0f;

    // ── Guy donations ─────────────────────────────────────────────────────────

    /** COIN donated per trigger by PUBLIC/PENSIONER NPCs (in-game minutes). */
    public static final int GUY_DONATION_AMOUNT = 1;

    /** Interval (in-game minutes) between donations from spectators. */
    public static final float GUY_DONATION_INTERVAL_MINUTES = 2.0f;

    /** Chance per in-game minute that a YOUTH_GANG NPC kicks over GUY_PROP. */
    public static final float GUY_KICK_CHANCE_PER_MINUTE = 0.20f;

    // ── Darren's holdall ──────────────────────────────────────────────────────

    /** Minimum STEALTH tier level required to steal Darren's holdall. */
    public static final int HOLDALL_STEALTH_MIN = 2;

    /** COIN earned by fencing Darren's holdall. */
    public static final int HOLDALL_FENCE_VALUE = 25;

    // ── Firework noise levels ─────────────────────────────────────────────────

    /** Noise level for ROMAN_CANDLE launch (blocks). */
    public static final float NOISE_ROMAN_CANDLE = 6.0f;

    /** Noise level for BANGER_FIREWORK launch (blocks). */
    public static final float NOISE_BANGER = 8.0f;

    /** Noise level for ROCKET_FIREWORK launch (blocks). */
    public static final float NOISE_ROCKET = 9.0f;

    // ── Misfire chances ───────────────────────────────────────────────────────

    /** Base misfire chance for ROCKET_FIREWORK (10%). */
    public static final float MISFIRE_ROCKET_BASE = 0.10f;

    /** Base misfire chance for BANGER_FIREWORK (20%). */
    public static final float MISFIRE_BANGER_BASE = 0.20f;

    /** Base misfire chance for ROMAN_CANDLE (5%). */
    public static final float MISFIRE_ROMAN_CANDLE_BASE = 0.05f;

    /** Rain multiplier for misfire chance (×2). */
    public static final float MISFIRE_RAIN_MULTIPLIER = 2.0f;

    // ── Police offence ────────────────────────────────────────────────────────

    /** Notoriety added on second police firework sighting. */
    public static final int FIREWORK_OFFENCE_NOTORIETY = 3;

    /** Wanted stars added on second police firework sighting. */
    public static final int FIREWORK_OFFENCE_WANTED_STARS = 1;

    // ── Sabotage penalties ────────────────────────────────────────────────────

    /** Notoriety added for Option A — early launch of the official display. */
    public static final int EARLY_LAUNCH_NOTORIETY = 5;

    /** Notoriety added for Option B — planting BANGER in the mortar. */
    public static final int BANGER_SABOTAGE_NOTORIETY = 8;

    // ── PYRO_NIGHT achievement ────────────────────────────────────────────────

    /** Number of fireworks needed for {@link AchievementType#PYRO_NIGHT}. */
    public static final int PYRO_NIGHT_TARGET = 3;

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Results of attempting to craft and place GUY_PROP. */
    public enum GuyPlacementResult {
        /** GUY_PROP successfully placed in the park. */
        PLACED,
        /** Placement window is not open (outside 17:00–19:30). */
        WRONG_TIME,
        /** Player is not in the park area. */
        WRONG_LOCATION,
        /** Player lacks crafting ingredients (NEWSPAPER + OLD_CLOTHES + WOOLLY_HAT). */
        MISSING_INGREDIENTS,
        /** Event not active (wrong day or thunderstorm). */
        EVENT_NOT_ACTIVE
    }

    /** Results of attempting to purchase from Darren's holdall. */
    public enum DarrenBuyResult {
        /** Item purchased successfully. */
        PURCHASED,
        /** Darren is not present (wrong time). */
        DARREN_NOT_PRESENT,
        /** Darren is hostile (robbed or tipped off). */
        DARREN_HOSTILE,
        /** Player has insufficient COIN. */
        INSUFFICIENT_FUNDS,
        /** Event not active. */
        EVENT_NOT_ACTIVE
    }

    /** Results of attempting to steal Darren's holdall. */
    public enum HoldallStealResult {
        /** Holdall stolen successfully. */
        STOLEN,
        /** Player's STEALTH tier is insufficient (< 2). */
        STEALTH_TOO_LOW,
        /** Darren is already hostile or holdall already stolen. */
        DARREN_HOSTILE,
        /** Darren is not present. */
        DARREN_NOT_PRESENT,
        /** Event not active. */
        EVENT_NOT_ACTIVE
    }

    /** Results of launching a personal firework. */
    public enum FireworkLaunchResult {
        /** Firework launched successfully. */
        LAUNCHED,
        /** Firework misfired (no noise spike, item consumed). */
        MISFIRE,
        /** Not during event window. */
        WRONG_TIME,
        /** Player does not have the firework item. */
        NOT_IN_INVENTORY,
        /** Event not active. */
        EVENT_NOT_ACTIVE
    }

    /** Results of interacting with the FIREWORK_MORTAR_PROP. */
    public enum MortarInteractResult {
        /** Option A: early launch triggered (Notoriety +5, SABOTEUR). */
        EARLY_LAUNCH,
        /** Option B: BANGER planted; scheduled for 20:00 catastrophic misfire. */
        BANGER_PLANTED,
        /** Mortar not yet active (before 19:45). */
        MORTAR_NOT_ACTIVE,
        /** Display already triggered. */
        ALREADY_TRIGGERED,
        /** Player does not have a BANGER_FIREWORK for Option B. */
        NO_BANGER,
        /** Event not active. */
        EVENT_NOT_ACTIVE
    }

    /** Broad event outcomes that callers may want to react to. */
    public enum EventType {
        /** THUNDERSTORM cancels the event. */
        THUNDERSTORM_CANCELS,
        /** BONFIRE_PROP spawned in the park. */
        BONFIRE_SPAWNED,
        /** BONFIRE_PROP replaced by COLD_EMBERS_PROP at 22:00. */
        BONFIRE_EMBERS,
        /** GUY_PROP kicked over by YOUTH_GANG. */
        GUY_KICKED_OVER,
        /** Display sabotaged — BANGER catastrophic misfire at 20:00. */
        BANGER_CATASTROPHE,
        /** Event window closes at 23:00. */
        EVENT_CLOSED
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Whether the event is currently active. */
    private boolean eventActive = false;

    /** Whether the BONFIRE_PROP has been spawned this session. */
    private boolean bonfireSpawned = false;

    /** Whether the BONFIRE_PROP has been replaced by COLD_EMBERS this session. */
    private boolean bonfireEmbers = false;

    /** Whether GUY_PROP has been placed by the player this session. */
    private boolean guyPlaced = false;

    /** Whether GUY_PROP has been destroyed (kicked over) this session. */
    private boolean guyDestroyed = false;

    /** Total COIN donated to the Guy this session. */
    private int guyCoinDonated = 0;

    /** Whether the PENNY_FOR_THE_GUY achievement has been awarded. */
    private boolean pennyAchievementAwarded = false;

    /** Whether the PARTY_POOPER achievement has been awarded. */
    private boolean partyPooberAchievementAwarded = false;

    /** Whether Darren's holdall has been stolen. */
    private boolean holdallStolen = false;

    /** Whether Darren is hostile (robbed or tipped off). */
    private boolean darrenHostile = false;

    /** Police firework sighting count for FIREWORK_OFFENCE tracking (0, 1, or 2+). */
    private int fireworkPoliceSightings = 0;

    /** Total fireworks launched this session (for PYRO_NIGHT). */
    private int fireworksLaunched = 0;

    /** Whether PYRO_NIGHT achievement has been awarded. */
    private boolean pyroNightAwarded = false;

    /** Whether the FIREWORK_MORTAR_PROP has been interacted with. */
    private boolean mortarTriggered = false;

    /** Whether a BANGER_FIREWORK has been planted in the mortar. */
    private boolean bangerPlanted = false;

    /** Whether the SABOTEUR achievement has been awarded. */
    private boolean saboteurAwarded = false;

    /** Whether the banger catastrophe has already fired. */
    private boolean bangerCatastropheFired = false;

    /** Accumulated in-game minutes for donation tick. */
    private float minutesSinceLastDonation = 0.0f;

    /** Accumulated in-game minutes for YOUTH_GANG kick chance. */
    private float minutesSinceLastKickCheck = 0.0f;

    // ── Integrated systems (optional wiring) ─────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private StreetSkillSystem streetSkillSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public BonfireNightSystem() {
        this(new Random());
    }

    public BonfireNightSystem(Random random) {
        this.random = random;
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem n) {
        this.notorietySystem = n;
    }

    public void setWantedSystem(WantedSystem w) {
        this.wantedSystem = w;
    }

    public void setCriminalRecord(CriminalRecord c) {
        this.criminalRecord = c;
    }

    public void setRumourNetwork(RumourNetwork r) {
        this.rumourNetwork = r;
    }

    public void setStreetSkillSystem(StreetSkillSystem s) {
        this.streetSkillSystem = s;
    }

    // ── Event day detection ───────────────────────────────────────────────────

    /**
     * Returns whether today is Bonfire Night (day-of-year 308), or the nearest
     * Saturday within ±3 days if one is available.
     *
     * @param dayOfYear    current day of year (1-based, e.g. 308 = 5 November)
     * @param dayOfWeek    current day of week (0=Mon … 6=Sun; 5=Saturday)
     * @return true if tonight is the Bonfire Night event
     */
    public boolean isBonfireNight(int dayOfYear, int dayOfWeek) {
        // Direct match
        if (dayOfYear == BONFIRE_NIGHT_DAY) return true;
        // Nearest Saturday within ±3 days
        if (dayOfWeek == 5) { // Saturday
            int diff = dayOfYear - BONFIRE_NIGHT_DAY;
            return Math.abs(diff) <= SATURDAY_SHIFT_MAX;
        }
        return false;
    }

    /**
     * Returns whether the event window is open for the given hour.
     *
     * @param hour current in-game hour
     * @return true if within the event window 18:00–23:00
     */
    public boolean isEventHour(float hour) {
        return hour >= EVENT_OPEN_HOUR && hour < EVENT_CLOSE_HOUR;
    }

    // ── Event lifecycle ───────────────────────────────────────────────────────

    /**
     * Open the event for this session. Resets all session state.
     * Must be called when 18:00 is reached on Bonfire Night.
     *
     * @param weatherCancels true if THUNDERSTORM cancels the event
     * @return EventType.THUNDERSTORM_CANCELS if cancelled, null otherwise
     */
    public EventType openEvent(boolean weatherCancels) {
        if (weatherCancels) {
            eventActive = false;
            return EventType.THUNDERSTORM_CANCELS;
        }
        eventActive = true;
        bonfireSpawned = false;
        bonfireEmbers = false;
        guyPlaced = false;
        guyDestroyed = false;
        guyCoinDonated = 0;
        pennyAchievementAwarded = false;
        partyPooberAchievementAwarded = false;
        holdallStolen = false;
        darrenHostile = false;
        fireworkPoliceSightings = 0;
        fireworksLaunched = 0;
        pyroNightAwarded = false;
        mortarTriggered = false;
        bangerPlanted = false;
        saboteurAwarded = false;
        bangerCatastropheFired = false;
        minutesSinceLastDonation = 0.0f;
        minutesSinceLastKickCheck = 0.0f;
        return null;
    }

    /**
     * Close the event at 23:00.
     *
     * @return EventType.EVENT_CLOSED
     */
    public EventType closeEvent() {
        eventActive = false;
        return EventType.EVENT_CLOSED;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update. Advances donation timers, YOUTH_GANG kick checks,
     * and the scheduled banger catastrophe at 20:00.
     *
     * @param deltaMinutes     elapsed in-game minutes since last call
     * @param currentHour      current in-game hour
     * @param playerInventory  player inventory (receives donations; may be null)
     * @param witnessNpc       an NPC near the player for rumour seeding (may be null)
     * @param callback         achievement callback (may be null)
     * @return event that fired this tick, or null
     */
    public EventType update(float deltaMinutes, float currentHour,
                            Inventory playerInventory, NPC witnessNpc,
                            AchievementCallback callback) {
        if (!eventActive) return null;

        // ── Bonfire spawn at 18:30 ────────────────────────────────────────────
        if (!bonfireSpawned && currentHour >= BONFIRE_SPAWN_HOUR) {
            bonfireSpawned = true;
            return EventType.BONFIRE_SPAWNED;
        }

        // ── Bonfire → embers at 22:00 ─────────────────────────────────────────
        if (bonfireSpawned && !bonfireEmbers && currentHour >= BONFIRE_EMBERS_HOUR) {
            bonfireEmbers = true;
            return EventType.BONFIRE_EMBERS;
        }

        // ── Banger catastrophe at 20:00 ───────────────────────────────────────
        if (bangerPlanted && !bangerCatastropheFired && currentHour >= DISPLAY_START_HOUR) {
            bangerCatastropheFired = true;
            triggerBangerCatastrophe(witnessNpc, callback);
            return EventType.BANGER_CATASTROPHE;
        }

        // ── Guy donation tick ──────────────────────────────────────────────────
        if (guyPlaced && !guyDestroyed) {
            minutesSinceLastDonation += deltaMinutes;
            if (minutesSinceLastDonation >= GUY_DONATION_INTERVAL_MINUTES) {
                minutesSinceLastDonation = 0.0f;
                guyCoinDonated += GUY_DONATION_AMOUNT;
                if (playerInventory != null) {
                    playerInventory.addItem(Material.COIN, GUY_DONATION_AMOUNT);
                }
                if (!pennyAchievementAwarded && guyCoinDonated >= 1) {
                    pennyAchievementAwarded = true;
                    if (callback != null) {
                        callback.award(AchievementType.PENNY_FOR_THE_GUY);
                    }
                }
            }

            // ── YOUTH_GANG kick check ──────────────────────────────────────────
            minutesSinceLastKickCheck += deltaMinutes;
            if (minutesSinceLastKickCheck >= 1.0f) {
                minutesSinceLastKickCheck = 0.0f;
                if (random.nextFloat() < GUY_KICK_CHANCE_PER_MINUTE) {
                    guyDestroyed = true;
                    if (!partyPooberAchievementAwarded) {
                        partyPooberAchievementAwarded = true;
                        if (callback != null) {
                            callback.award(AchievementType.PARTY_POOPER);
                        }
                    }
                    return EventType.GUY_KICKED_OVER;
                }
            }
        }

        return null;
    }

    // ── Mechanic 2 — Penny for the Guy ────────────────────────────────────────

    /**
     * Attempt to craft and place GUY_PROP in the park.
     * Requires NEWSPAPER + OLD_CLOTHES + WOOLLY_HAT in player inventory.
     * Placement window: 17:00–19:30.
     *
     * @param inventory    player inventory
     * @param currentHour  current in-game hour
     * @param inPark       true if the player is in the park area
     * @return result of the placement attempt
     */
    public GuyPlacementResult placeGuy(Inventory inventory, float currentHour,
                                       boolean inPark) {
        if (!eventActive) return GuyPlacementResult.EVENT_NOT_ACTIVE;
        if (currentHour < GUY_PLACE_OPEN_HOUR || currentHour > GUY_PLACE_CLOSE_HOUR) {
            return GuyPlacementResult.WRONG_TIME;
        }
        if (!inPark) return GuyPlacementResult.WRONG_LOCATION;
        if (!inventory.hasItem(Material.NEWSPAPER)
                || !inventory.hasItem(Material.OLD_CLOTHES)
                || !inventory.hasItem(Material.WOOLLY_HAT)) {
            return GuyPlacementResult.MISSING_INGREDIENTS;
        }

        inventory.removeItem(Material.NEWSPAPER, 1);
        inventory.removeItem(Material.OLD_CLOTHES, 1);
        inventory.removeItem(Material.WOOLLY_HAT, 1);
        guyPlaced = true;
        minutesSinceLastDonation = 0.0f;
        minutesSinceLastKickCheck = 0.0f;
        return GuyPlacementResult.PLACED;
    }

    // ── Mechanic 3 — Darren the firework dealer ────────────────────────────────

    /**
     * Returns whether Darren is currently present (between 17:00 and event close,
     * provided his holdall has not been stolen).
     *
     * @param currentHour current in-game hour
     * @return true if Darren is present and available
     */
    public boolean isDarrenPresent(float currentHour) {
        return eventActive
                && currentHour >= DARREN_SPAWN_HOUR
                && currentHour < EVENT_CLOSE_HOUR
                && !holdallStolen;
    }

    /**
     * Attempt to buy a firework item from Darren.
     *
     * @param item         the firework material to buy
     * @param price        COIN price for this item
     * @param inventory    player inventory
     * @param currentHour  current in-game hour
     * @return result of the purchase attempt
     */
    public DarrenBuyResult buyFromDarren(Material item, int price,
                                          Inventory inventory, float currentHour) {
        if (!eventActive) return DarrenBuyResult.EVENT_NOT_ACTIVE;
        if (!isDarrenPresent(currentHour)) return DarrenBuyResult.DARREN_NOT_PRESENT;
        if (darrenHostile) return DarrenBuyResult.DARREN_HOSTILE;
        if (inventory.getItemCount(Material.COIN) < price) {
            return DarrenBuyResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, price);
        inventory.addItem(item, 1);
        return DarrenBuyResult.PURCHASED;
    }

    /**
     * Attempt to steal Darren's holdall. Requires STEALTH tier ≥ 2.
     * On success, seeds {@link RumourType#FIREWORK_THEFT}, marks Darren hostile,
     * and adds HOLDALL item to player inventory (fenceable for 25 COIN).
     *
     * @param inventory    player inventory
     * @param currentHour  current in-game hour
     * @param witnessNpc   nearby NPC for rumour seeding (may be null)
     * @param callback     achievement callback (may be null)
     * @return result of the steal attempt
     */
    public HoldallStealResult stealHoldall(Inventory inventory, float currentHour,
                                            NPC witnessNpc, AchievementCallback callback) {
        if (!eventActive) return HoldallStealResult.EVENT_NOT_ACTIVE;
        if (!isDarrenPresent(currentHour)) return HoldallStealResult.DARREN_NOT_PRESENT;
        if (darrenHostile || holdallStolen) return HoldallStealResult.DARREN_HOSTILE;

        int stealthTier = (streetSkillSystem != null)
                ? streetSkillSystem.getTierLevel(Skill.STEALTH)
                : 0;
        if (stealthTier < HOLDALL_STEALTH_MIN) {
            return HoldallStealResult.STEALTH_TOO_LOW;
        }

        holdallStolen = true;
        darrenHostile = true;

        // Give the player the holdall (valuable fenceable item — represented as COIN)
        inventory.addItem(Material.COIN, HOLDALL_FENCE_VALUE);

        // Seed rumour
        if (rumourNetwork != null && witnessNpc != null) {
            rumourNetwork.addRumour(witnessNpc,
                new Rumour(RumourType.FIREWORK_THEFT,
                    "Some muppet nicked Darren's whole holdall — his full stock of " +
                    "fireworks, gone. He's out here with nothing to sell."));
        }

        return HoldallStealResult.STOLEN;
    }

    // ── Mechanic 4 — Personal fireworks ───────────────────────────────────────

    /**
     * Returns the base misfire chance for the given firework material.
     *
     * @param material the firework to query
     * @param isRaining whether it is currently raining (doubles misfire chance)
     * @return misfire probability (0.0–1.0)
     */
    public float getMisfireChance(Material material, boolean isRaining) {
        float base;
        switch (material) {
            case ROCKET_FIREWORK:   base = MISFIRE_ROCKET_BASE; break;
            case BANGER_FIREWORK:   base = MISFIRE_BANGER_BASE; break;
            case ROMAN_CANDLE:      base = MISFIRE_ROMAN_CANDLE_BASE; break;
            default:                base = MISFIRE_ROCKET_BASE; break;
        }
        return isRaining ? base * MISFIRE_RAIN_MULTIPLIER : base;
    }

    /**
     * Returns the noise level for the given firework material.
     *
     * @param material the firework to query
     * @return noise level (blocks radius)
     */
    public float getFireworkNoise(Material material) {
        switch (material) {
            case ROCKET_FIREWORK:   return NOISE_ROCKET;
            case BANGER_FIREWORK:   return NOISE_BANGER;
            case ROMAN_CANDLE:      return NOISE_ROMAN_CANDLE;
            default:                return NOISE_ROCKET;
        }
    }

    /**
     * Attempt to launch a personal firework.
     *
     * @param material     the firework material to launch
     * @param inventory    player inventory
     * @param currentHour  current in-game hour
     * @param isRaining    whether it is currently raining
     * @return result of the launch attempt
     */
    public FireworkLaunchResult launchFirework(Material material, Inventory inventory,
                                                float currentHour, boolean isRaining) {
        if (!eventActive) return FireworkLaunchResult.EVENT_NOT_ACTIVE;
        if (currentHour < EVENT_OPEN_HOUR || currentHour >= EVENT_CLOSE_HOUR) {
            return FireworkLaunchResult.WRONG_TIME;
        }
        if (!inventory.hasItem(material)) return FireworkLaunchResult.NOT_IN_INVENTORY;

        inventory.removeItem(material, 1);

        float misfireChance = getMisfireChance(material, isRaining);
        if (random.nextFloat() < misfireChance) {
            return FireworkLaunchResult.MISFIRE;
        }

        fireworksLaunched++;
        return FireworkLaunchResult.LAUNCHED;
    }

    /**
     * Records a police sighting of the player using fireworks. First sighting
     * is a warning; second sighting records FIREWORK_OFFENCE.
     *
     * @param playerX   player X position (for WantedSystem LKP)
     * @param playerY   player Y position
     * @param playerZ   player Z position
     * @param callback  achievement callback (may be null)
     * @return true if FIREWORK_OFFENCE was recorded (second sighting)
     */
    public boolean recordFireworkPoliceSighting(float playerX, float playerY,
                                                 float playerZ,
                                                 AchievementCallback callback) {
        fireworkPoliceSightings++;
        if (fireworkPoliceSightings < 2) {
            // First sighting: warning only
            return false;
        }
        // Second+ sighting: record offence
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.FIREWORK_OFFENCE);
        }
        if (notorietySystem != null && callback != null) {
            notorietySystem.addNotoriety(FIREWORK_OFFENCE_NOTORIETY, callback);
        }
        if (wantedSystem != null && callback != null) {
            wantedSystem.addWantedStars(FIREWORK_OFFENCE_WANTED_STARS,
                    playerX, playerY, playerZ, callback);
        }
        return true;
    }

    /**
     * Check whether PYRO_NIGHT achievement should be awarded (≥3 fireworks
     * launched this event with no FIREWORK_OFFENCE recorded).
     *
     * @param callback achievement callback (may be null)
     * @return true if the achievement was newly awarded
     */
    public boolean checkPyroNight(AchievementCallback callback) {
        if (!pyroNightAwarded
                && fireworksLaunched >= PYRO_NIGHT_TARGET
                && fireworkPoliceSightings < 2) {
            pyroNightAwarded = true;
            if (callback != null) {
                callback.award(AchievementType.PYRO_NIGHT);
            }
            return true;
        }
        return false;
    }

    // ── Mechanic 5 — Tesco car park display sabotage ──────────────────────────

    /**
     * Player interacts with the FIREWORK_MORTAR_PROP at the Tesco car park.
     * Option A (no BANGER in inventory): triggers early launch (Notoriety +5).
     * Option B (BANGER in inventory, before 20:00): plants the banger for
     * catastrophic misfire at 20:00.
     *
     * @param inventory    player inventory
     * @param currentHour  current in-game hour
     * @param witnessNpc   nearby NPC for rumour seeding (may be null)
     * @param callback     achievement callback (may be null)
     * @return result of the interaction
     */
    public MortarInteractResult interactWithMortar(Inventory inventory,
                                                    float currentHour,
                                                    NPC witnessNpc,
                                                    AchievementCallback callback) {
        if (!eventActive) return MortarInteractResult.EVENT_NOT_ACTIVE;
        if (currentHour < MORTAR_ACTIVE_HOUR) return MortarInteractResult.MORTAR_NOT_ACTIVE;
        if (mortarTriggered || bangerCatastropheFired) {
            return MortarInteractResult.ALREADY_TRIGGERED;
        }

        // Option B: plant BANGER_FIREWORK (if available and before 20:00)
        if (inventory.hasItem(Material.BANGER_FIREWORK)
                && currentHour < DISPLAY_START_HOUR) {
            inventory.removeItem(Material.BANGER_FIREWORK, 1);
            bangerPlanted = true;
            mortarTriggered = true;
            return MortarInteractResult.BANGER_PLANTED;
        }

        // Option A: early launch
        mortarTriggered = true;
        if (notorietySystem != null && callback != null) {
            notorietySystem.addNotoriety(EARLY_LAUNCH_NOTORIETY, callback);
        }
        if (!saboteurAwarded) {
            saboteurAwarded = true;
            if (callback != null) {
                callback.award(AchievementType.SABOTEUR);
            }
        }
        return MortarInteractResult.EARLY_LAUNCH;
    }

    /**
     * Internal: trigger the banger catastrophe (Option B at 20:00).
     * Notoriety +8, CRIMINAL_DAMAGE, FIRE_ENGINE callout, FIREWORK_PRANK rumour.
     */
    private void triggerBangerCatastrophe(NPC witnessNpc, AchievementCallback callback) {
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.CRIMINAL_DAMAGE);
        }
        if (notorietySystem != null && callback != null) {
            notorietySystem.addNotoriety(BANGER_SABOTAGE_NOTORIETY, callback);
        }
        if (rumourNetwork != null && witnessNpc != null) {
            rumourNetwork.addRumour(witnessNpc,
                new Rumour(RumourType.FIREWORK_PRANK,
                    "Someone shoved a banger in the display mortar at the Tesco " +
                    "car park — went off early and nearly flattened the compère."));
        }
        if (!saboteurAwarded) {
            saboteurAwarded = true;
            if (callback != null) {
                callback.award(AchievementType.SABOTEUR);
            }
        }
    }

    // ── Warden detection radius ───────────────────────────────────────────────

    /**
     * Returns the current detection radius for BONFIRE_WARDEN Gary.
     * After 21:00 Gary gets distracted with his flask and detection is halved.
     *
     * @param currentHour current in-game hour
     * @return detection radius in blocks
     */
    public float getWardenDetectionRadius(float currentHour) {
        return currentHour >= WARDEN_DISTRACTED_HOUR
                ? WARDEN_DISTRACTED_RADIUS
                : WARDEN_DETECTION_RADIUS;
    }

    // ── Spawning helpers ──────────────────────────────────────────────────────

    /**
     * Spawn the event NPCs for Bonfire Night: BONFIRE_WARDEN Gary and
     * EVENT_COMPERE Keith.
     *
     * @return list of spawned NPCs (caller adds them to the world)
     */
    public List<NPC> spawnEventNPCs() {
        List<NPC> npcs = new ArrayList<>();
        if (!eventActive) return npcs;

        NPC gary = new NPC(NPCType.BONFIRE_WARDEN, 0f, 0f, 0f);
        npcs.add(gary);

        NPC keith = new NPC(NPCType.EVENT_COMPERE, 0f, 0f, 0f);
        npcs.add(keith);

        return npcs;
    }

    /**
     * Spawn Darren the firework dealer (behind the off-licence).
     *
     * @return the Darren NPC (caller adds to world), or null if wrong time
     */
    public NPC spawnDarren(float currentHour) {
        if (!eventActive || currentHour < DARREN_SPAWN_HOUR) return null;
        return new NPC(NPCType.FIREWORK_DEALER_NPC, 0f, 0f, 0f);
    }

    // ── State accessors (for testing) ─────────────────────────────────────────

    public boolean isEventActive() { return eventActive; }
    public boolean isBonfireSpawned() { return bonfireSpawned; }
    public boolean isBonfireEmbers() { return bonfireEmbers; }
    public boolean isGuyPlaced() { return guyPlaced; }
    public boolean isGuyDestroyed() { return guyDestroyed; }
    public int getGuyCoinDonated() { return guyCoinDonated; }
    public boolean isHoldallStolen() { return holdallStolen; }
    public boolean isDarrenHostile() { return darrenHostile; }
    public int getFireworkPoliceSightings() { return fireworkPoliceSightings; }
    public int getFireworksLaunched() { return fireworksLaunched; }
    public boolean isMortarTriggered() { return mortarTriggered; }
    public boolean isBangerPlanted() { return bangerPlanted; }
    public boolean isBangerCatastropheFired() { return bangerCatastropheFired; }

    // ── Test helpers ──────────────────────────────────────────────────────────

    public void setEventActiveForTesting(boolean active) { this.eventActive = active; }
    public void setGuyPlacedForTesting(boolean placed) { this.guyPlaced = placed; }
    public void setGuyDestroyedForTesting(boolean destroyed) { this.guyDestroyed = destroyed; }
    public void setBonfireSpawnedForTesting(boolean spawned) { this.bonfireSpawned = spawned; }
    public void setMortarTriggeredForTesting(boolean triggered) { this.mortarTriggered = triggered; }
    public void setBangerPlantedForTesting(boolean planted) { this.bangerPlanted = planted; }
    public void setDarrenHostileForTesting(boolean hostile) { this.darrenHostile = hostile; }
    public void setHoldallStolenForTesting(boolean stolen) { this.holdallStolen = stolen; }
    public void setFireworkPoliceSightingsForTesting(int n) { this.fireworkPoliceSightings = n; }
    public void setFireworksLaunchedForTesting(int n) { this.fireworksLaunched = n; }
}

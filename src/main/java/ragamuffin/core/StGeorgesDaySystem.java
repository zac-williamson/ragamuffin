package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.core.NotorietySystem.AchievementCallback;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1386: Northfield St George's Day — The Morris Dancers Nobody Asked For,
 * the Wetherspoons Lock-In &amp; the Flag Heist.
 *
 * <h3>Overview</h3>
 * On day-of-year 113 (23 April, {@code dayCount % 365 == 113}), Northfield celebrates
 * St George's Day with five interlinked mechanics.
 *
 * <h3>Mechanic 1 — Morris Dancers (11:00–15:00, Park)</h3>
 * Six {@link NPCType#MORRIS_DANCER} NPCs with {@link Material#MORRIS_STICK_PROP}s perform
 * in the park. Player options:
 * <ul>
 *   <li>Watch — grants {@code MORRIS_APPRECIATION} flag + VIBES +1</li>
 *   <li>Join in — {@link AchievementType#NORTHERN_SPIRIT} achievement</li>
 *   <li>Steal a stick — all 6 dancers pursue; WantedSystem +1; {@link AchievementType#STOLE_THE_STICK}</li>
 *   <li>Sabotage with {@link Material#BANANA_SKIN} — NoiseSystem level 7</li>
 * </ul>
 *
 * <h3>Mechanic 2 — Wetherspoons Lock-In (11:00–02:00)</h3>
 * Terry ({@link NPCType#TERRY_BARMAN}) sells {@link Material#DOOM_BAR_PINT} (3 COIN each).
 * 3 pints → {@code DRUNK_STATE}. Lock-in at midnight for Notoriety Tier ≤ 2.
 * Police check at 00:30. Survive to 02:00 → {@link AchievementType#LOCK_IN_LEGEND}.
 *
 * <h3>Mechanic 3 — Flag Heist (Inside Bar)</h3>
 * Climb {@link PropType#BAR_STOOL_PROP}, press E on {@link Material#ST_GEORGE_FLAG_PROP}
 * above bar. Results: ejection + 3-day ban + {@link AchievementType#TOOK_THE_FLAG}.
 * Fence to Mirek ({@link NPCType#MIREK_FENCE}) for 5 COIN.
 *
 * <h3>Mechanic 4 — Flag Heist (Rooftop)</h3>
 * Climb {@link PropType#DRAINPIPE_PROP} in back alley (3-second hold-E). CCTV must be
 * disabled. Take {@link Material#ROOF_FLAG_PROP}. Fence to Mirek for 12 COIN.
 * {@link AchievementType#OFF_THE_ROOF} achievement.
 *
 * <h3>Mechanic 5 — Nationalist Tension + Crowd Scuffle</h3>
 * {@link NPCType#ENGLAND_FLAG_NPC} and {@link NPCType#COUNTER_PROTEST_NPC} groups converge
 * at 14:00. Within 5 blocks → {@code CROWD_SCUFFLE} event (NoiseSystem level 8, POLICE
 * in 30 seconds). Player can film with a phone ({@link Material#STOLEN_PHONE}) to seed
 * {@link RumourType#COMMUNITY_OUTRAGE} and trigger NewspaperSystem headline.
 *
 * <h3>Bonus — Best England Shirt Competition (15:00)</h3>
 * Buy {@link Material#ENGLAND_SHIRT_PROP} at PoundShop (3 COIN), keep condition ≥ 85,
 * stay sober → win 15 COIN. Awards {@link AchievementType#BEST_DRESSED_PATRIOT}.
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@link AchievementType#NORTHERN_SPIRIT} — join the Morris Dancers</li>
 *   <li>{@link AchievementType#STOLE_THE_STICK} — steal a Morris stick</li>
 *   <li>{@link AchievementType#TOOK_THE_FLAG} — take the bar flag</li>
 *   <li>{@link AchievementType#OFF_THE_ROOF} — take the roof flag</li>
 *   <li>{@link AchievementType#BEST_DRESSED_PATRIOT} — win the shirt competition</li>
 *   <li>{@link AchievementType#LOCK_IN_LEGEND} — survive the lock-in to 02:00</li>
 * </ul>
 */
public class StGeorgesDaySystem {

    // ── Day constant ──────────────────────────────────────────────────────────

    /** St George's Day falls on day-of-year 113 (23 April, non-leap year). */
    public static final int ST_GEORGES_DAY_DOY = 113;

    /** Trigger condition: {@code dayCount % 365 == ST_GEORGES_DAY_DOY}. */
    public static final int ST_GEORGES_DAY_MODULO = 365;

    // ── Event hours ───────────────────────────────────────────────────────────

    /** Morris Dancers & Wetherspoons open at 11:00. */
    public static final float EVENT_OPEN_HOUR = 11.0f;

    /** Morris Dancers perform until 15:00. */
    public static final float MORRIS_CLOSE_HOUR = 15.0f;

    /** England Flag NPC and Counter Protest NPC converge from 14:00. */
    public static final float TENSION_START_HOUR = 14.0f;

    /** Best England Shirt competition judged at 15:00. */
    public static final float SHIRT_COMPETITION_HOUR = 15.0f;

    /** Wetherspoons lock-in starts at midnight (24:00 = 0:00 next day, stored as 0.0f).
     *  Internally we detect midnight as hour wrapping past 24.0f or == 0.0f after event open. */
    public static final float LOCK_IN_HOUR = 24.0f;

    /** Police check during lock-in at 00:30. */
    public static final float POLICE_CHECK_HOUR = 0.5f;

    /** Lock-in ends at 02:00; LOCK_IN_LEGEND awarded if player survives. */
    public static final float LOCK_IN_END_HOUR = 2.0f;

    /** Wetherspoons open all day until lock-in ends at 02:00. */
    public static final float WETHERSPOONS_CLOSE_HOUR = 2.0f;

    // ── Costs & rewards ───────────────────────────────────────────────────────

    /** Cost of one DOOM_BAR_PINT from Terry. */
    public static final int DOOM_BAR_PINT_COST = 3;

    /** Number of pints that triggers DRUNK_STATE. */
    public static final int DRUNK_STATE_PINTS = 3;

    /** Coin received when fencing ST_GEORGE_FLAG_PROP to Mirek. */
    public static final int BAR_FLAG_FENCE_VALUE = 5;

    /** Coin received when fencing ROOF_FLAG_PROP to Mirek. */
    public static final int ROOF_FLAG_FENCE_VALUE = 12;

    /** Cost of ENGLAND_SHIRT_PROP at PoundShop. */
    public static final int SHIRT_COST = 3;

    /** Minimum shirt condition (0–100) required to win the competition. */
    public static final int SHIRT_MIN_CONDITION = 85;

    /** Coin won by winning the Best England Shirt competition. */
    public static final int SHIRT_WIN_PRIZE = 15;

    // ── Penalties ─────────────────────────────────────────────────────────────

    /** Notoriety added when player takes the bar flag. */
    public static final int BAR_FLAG_NOTORIETY = 3;

    /** Notoriety added when player takes the roof flag. */
    public static final int ROOF_FLAG_NOTORIETY = 5;

    /** Notoriety added when player steals a Morris stick. */
    public static final int MORRIS_STICK_NOTORIETY = 1;

    /** Wanted stars added for stealing a Morris stick. */
    public static final int MORRIS_STICK_WANTED = 1;

    /** Wanted stars added for rooftop flag theft. */
    public static final int ROOF_FLAG_WANTED = 1;

    /** Notoriety Tier maximum to qualify for the Wetherspoons lock-in. */
    public static final int LOCK_IN_MAX_TIER = 2;

    // ── Game mechanics ────────────────────────────────────────────────────────

    /** Distance (blocks) at which ENGLAND_FLAG_NPC and COUNTER_PROTEST_NPC trigger scuffle. */
    public static final float SCUFFLE_TRIGGER_DISTANCE = 5.0f;

    /** NoiseSystem level for BANANA_SKIN_PROP sabotage of Morris Dancers. */
    public static final float BANANA_SKIN_NOISE = 7.0f;

    /** NoiseSystem level for CROWD_SCUFFLE event. */
    public static final float CROWD_SCUFFLE_NOISE = 8.0f;

    /** Seconds until POLICE respond after CROWD_SCUFFLE event fires. */
    public static final float CROWD_SCUFFLE_POLICE_SECONDS = 30.0f;

    /** Number of MORRIS_DANCER NPCs spawned in the park. */
    public static final int MORRIS_DANCER_COUNT = 6;

    /** Drainpipe climb time in seconds (hold-E). */
    public static final float DRAINPIPE_CLIMB_SECONDS = 3.0f;

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Results of the player watching the Morris Dancers. */
    public enum WatchMorrisResult {
        /** Player watched successfully; MORRIS_APPRECIATION flag granted + VIBES +1. */
        WATCHED,
        /** Morris Dancers are not active (outside 11:00–15:00). */
        WRONG_TIME,
        /** Event not active. */
        EVENT_NOT_ACTIVE
    }

    /** Results of the player joining the Morris Dancers. */
    public enum JoinMorrisResult {
        /** Player joined; NORTHERN_SPIRIT achievement awarded. */
        JOINED,
        /** Morris Dancers are not active. */
        WRONG_TIME,
        /** Player already joined this session. */
        ALREADY_JOINED,
        /** Event not active. */
        EVENT_NOT_ACTIVE
    }

    /** Results of attempting to steal a Morris stick. */
    public enum StealStickResult {
        /** Stick stolen; all 6 dancers give chase; STOLE_THE_STICK awarded. */
        STOLEN,
        /** Morris Dancers are not active. */
        WRONG_TIME,
        /** Stick already stolen this session. */
        ALREADY_STOLEN,
        /** Event not active. */
        EVENT_NOT_ACTIVE
    }

    /** Results of sabotaging the Morris Dancers with a BANANA_SKIN_PROP. */
    public enum SabotageMorrisResult {
        /** Banana skin placed; NoiseSystem level 7 triggered. */
        SABOTAGED,
        /** Player does not have BANANA_SKIN_PROP. */
        NO_BANANA_SKIN,
        /** Morris Dancers not active. */
        WRONG_TIME,
        /** Event not active. */
        EVENT_NOT_ACTIVE
    }

    /** Results of buying a DOOM_BAR_PINT from Terry. */
    public enum BuyPintResult {
        /** Pint purchased successfully. */
        PURCHASED,
        /** Terry is not serving (outside 11:00–02:00). */
        WRONG_TIME,
        /** Player lacks funds. */
        INSUFFICIENT_FUNDS,
        /** Event not active. */
        EVENT_NOT_ACTIVE
    }

    /** Results of attempting the bar flag heist. */
    public enum BarFlagHeistResult {
        /** Flag taken; player ejected; 3-day ban; TOOK_THE_FLAG awarded. */
        FLAG_TAKEN,
        /** Player is not standing on the BAR_STOOL_PROP. */
        NOT_ON_STOOL,
        /** Flag already taken this session. */
        ALREADY_TAKEN,
        /** Event not active. */
        EVENT_NOT_ACTIVE
    }

    /** Results of attempting the rooftop flag heist. */
    public enum RoofFlagHeistResult {
        /** Flag taken; OFF_THE_ROOF awarded. */
        FLAG_TAKEN,
        /** Player has not climbed to the roof via DRAINPIPE_PROP. */
        NOT_ON_ROOF,
        /** CCTV is still active (not disabled). */
        CCTV_ACTIVE,
        /** Flag already taken this session. */
        ALREADY_TAKEN,
        /** Event not active. */
        EVENT_NOT_ACTIVE
    }

    /** Results of fencing a flag to Mirek. */
    public enum FenceFlagResult {
        /** Flag fenced; COIN added to inventory. */
        FENCED,
        /** Player does not have the specified flag item. */
        NO_FLAG,
        /** Mirek is not present. */
        MIREK_NOT_PRESENT,
        /** Event not active. */
        EVENT_NOT_ACTIVE
    }

    /** Results of the Best England Shirt competition. */
    public enum ShirtCompResult {
        /** Player wins; 15 COIN awarded; BEST_DRESSED_PATRIOT achievement. */
        WON,
        /** Competition not yet open (before 15:00). */
        WRONG_TIME,
        /** Player does not have ENGLAND_SHIRT_PROP. */
        NO_SHIRT,
        /** Shirt condition is below 85. */
        SHIRT_TOO_WORN,
        /** Player is in DRUNK_STATE. */
        PLAYER_DRUNK,
        /** Competition already resolved this session. */
        ALREADY_RESOLVED,
        /** Event not active. */
        EVENT_NOT_ACTIVE
    }

    /** Results of the lock-in check. */
    public enum LockInResult {
        /** Player qualifies for lock-in. */
        ADMITTED,
        /** Player's Notoriety Tier exceeds LOCK_IN_MAX_TIER. */
        TIER_TOO_HIGH,
        /** Lock-in not yet started (before midnight). */
        TOO_EARLY,
        /** Event not active. */
        EVENT_NOT_ACTIVE
    }

    /** Broad event outcomes returned by {@link #update}. */
    public enum EventType {
        /** Morris Dancers spawned at 11:00. */
        MORRIS_SPAWNED,
        /** Morris Dancers despawned at 15:00. */
        MORRIS_DESPAWNED,
        /** Nationalist tension groups begin converging at 14:00. */
        TENSION_STARTED,
        /** CROWD_SCUFFLE fires when groups come within 5 blocks. */
        CROWD_SCUFFLE,
        /** Best England Shirt competition opens at 15:00. */
        SHIRT_COMPETITION_OPEN,
        /** Wetherspoons lock-in starts at midnight. */
        LOCK_IN_STARTED,
        /** Police check during lock-in at 00:30. */
        LOCK_IN_POLICE_CHECK,
        /** Lock-in ends at 02:00. */
        LOCK_IN_ENDED,
        /** Event window closes (end of day). */
        EVENT_CLOSED
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    private boolean eventActive = false;

    // Morris Dancers
    private boolean morrisDancersSpawned = false;
    private boolean morrisDancersDespawned = false;
    private boolean playerWatchedMorris = false;
    private boolean playerJoinedMorris = false;
    private boolean morrisStickStolen = false;
    private boolean morrisSabotaged = false;
    private boolean northernSpiritAwarded = false;
    private boolean stoleTheStickAwarded = false;

    // Wetherspoons / lock-in
    private int pintsConsumed = 0;
    private boolean playerDrunk = false;
    private boolean lockInStarted = false;
    private boolean lockInPoliceCheckFired = false;
    private boolean lockInEnded = false;
    private boolean lockInLegendAwarded = false;
    private boolean playerInLockIn = false;

    // Flag heist — bar
    private boolean barFlagTaken = false;
    private boolean tookTheFlagAwarded = false;
    private boolean playerEjectedFromPub = false;

    // Flag heist — roof
    private boolean playerOnRoof = false;
    private boolean roofFlagTaken = false;
    private boolean offTheRoofAwarded = false;

    // Tension / scuffle
    private boolean tensionStarted = false;
    private boolean crowdScuffleFired = false;
    private boolean communityOutrageSeeded = false;

    // Shirt competition
    private boolean shirtCompOpen = false;
    private boolean shirtCompResolved = false;
    private boolean bestDressedAwarded = false;

    // ── Integrated systems (optional wiring) ─────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;

    // ── Construction ──────────────────────────────────────────────────────────

    public StGeorgesDaySystem() {
        this(new Random());
    }

    public StGeorgesDaySystem(Random random) {
        this.random = random;
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem n) { this.notorietySystem = n; }
    public void setWantedSystem(WantedSystem w) { this.wantedSystem = w; }
    public void setCriminalRecord(CriminalRecord c) { this.criminalRecord = c; }
    public void setRumourNetwork(RumourNetwork r) { this.rumourNetwork = r; }

    // ── Event day detection ───────────────────────────────────────────────────

    /**
     * Returns true if today is St George's Day (23 April).
     *
     * @param dayCount running day counter from game start
     * @return true if {@code dayCount % 365 == 113}
     */
    public boolean isStGeorgesDay(int dayCount) {
        return (dayCount % ST_GEORGES_DAY_MODULO) == ST_GEORGES_DAY_DOY;
    }

    // ── Event lifecycle ───────────────────────────────────────────────────────

    /**
     * Open the St George's Day event. Resets all session state.
     * Call when 11:00 is reached on the event day.
     */
    public void openEvent() {
        eventActive = true;
        morrisDancersSpawned = false;
        morrisDancersDespawned = false;
        playerWatchedMorris = false;
        playerJoinedMorris = false;
        morrisStickStolen = false;
        morrisSabotaged = false;
        northernSpiritAwarded = false;
        stoleTheStickAwarded = false;
        pintsConsumed = 0;
        playerDrunk = false;
        lockInStarted = false;
        lockInPoliceCheckFired = false;
        lockInEnded = false;
        lockInLegendAwarded = false;
        playerInLockIn = false;
        barFlagTaken = false;
        tookTheFlagAwarded = false;
        playerEjectedFromPub = false;
        playerOnRoof = false;
        roofFlagTaken = false;
        offTheRoofAwarded = false;
        tensionStarted = false;
        crowdScuffleFired = false;
        communityOutrageSeeded = false;
        shirtCompOpen = false;
        shirtCompResolved = false;
        bestDressedAwarded = false;
    }

    /**
     * Close the event.
     *
     * @return {@link EventType#EVENT_CLOSED}
     */
    public EventType closeEvent() {
        eventActive = false;
        return EventType.EVENT_CLOSED;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update. Fires timed events (morris spawn/despawn, tension, shirt comp,
     * lock-in start, police check, lock-in end).
     *
     * @param currentHour  current in-game hour (11.0–26.0, where 24.0+ = past midnight)
     * @param callback     achievement callback (may be null)
     * @return event that fired this tick, or null
     */
    public EventType update(float currentHour, AchievementCallback callback) {
        if (!eventActive) return null;

        // Morris Dancers spawn at 11:00
        if (!morrisDancersSpawned && currentHour >= EVENT_OPEN_HOUR) {
            morrisDancersSpawned = true;
            return EventType.MORRIS_SPAWNED;
        }

        // Morris Dancers despawn at 15:00
        if (morrisDancersSpawned && !morrisDancersDespawned && currentHour >= MORRIS_CLOSE_HOUR) {
            morrisDancersDespawned = true;
            return EventType.MORRIS_DESPAWNED;
        }

        // Tension groups start converging at 14:00
        if (!tensionStarted && currentHour >= TENSION_START_HOUR) {
            tensionStarted = true;
            return EventType.TENSION_STARTED;
        }

        // Shirt competition opens at 15:00
        if (!shirtCompOpen && currentHour >= SHIRT_COMPETITION_HOUR) {
            shirtCompOpen = true;
            return EventType.SHIRT_COMPETITION_OPEN;
        }

        // Lock-in starts at midnight (hour >= 24.0)
        if (!lockInStarted && currentHour >= LOCK_IN_HOUR) {
            lockInStarted = true;
            return EventType.LOCK_IN_STARTED;
        }

        // Police check at 00:30 (hour >= 24.5)
        if (lockInStarted && !lockInPoliceCheckFired && currentHour >= LOCK_IN_HOUR + POLICE_CHECK_HOUR) {
            lockInPoliceCheckFired = true;
            return EventType.LOCK_IN_POLICE_CHECK;
        }

        // Lock-in ends at 02:00 (hour >= 26.0)
        if (lockInStarted && !lockInEnded && currentHour >= LOCK_IN_HOUR + LOCK_IN_END_HOUR) {
            lockInEnded = true;
            if (playerInLockIn && !lockInLegendAwarded) {
                lockInLegendAwarded = true;
                if (callback != null) {
                    callback.award(AchievementType.LOCK_IN_LEGEND);
                }
            }
            return EventType.LOCK_IN_ENDED;
        }

        return null;
    }

    // ── Mechanic 1 — Morris Dancers ───────────────────────────────────────────

    /**
     * Player watches the Morris Dancers (awards MORRIS_APPRECIATION flag + VIBES +1).
     *
     * @param currentHour current in-game hour
     * @return result of the watch attempt
     */
    public WatchMorrisResult watchMorris(float currentHour) {
        if (!eventActive) return WatchMorrisResult.EVENT_NOT_ACTIVE;
        if (!isMorrisActive(currentHour)) return WatchMorrisResult.WRONG_TIME;
        playerWatchedMorris = true;
        return WatchMorrisResult.WATCHED;
    }

    /**
     * Player joins the Morris Dancers. Awards {@link AchievementType#NORTHERN_SPIRIT}.
     *
     * @param currentHour current in-game hour
     * @param callback    achievement callback (may be null)
     * @return result of the join attempt
     */
    public JoinMorrisResult joinMorris(float currentHour, AchievementCallback callback) {
        if (!eventActive) return JoinMorrisResult.EVENT_NOT_ACTIVE;
        if (!isMorrisActive(currentHour)) return JoinMorrisResult.WRONG_TIME;
        if (playerJoinedMorris) return JoinMorrisResult.ALREADY_JOINED;
        playerJoinedMorris = true;
        if (!northernSpiritAwarded) {
            northernSpiritAwarded = true;
            if (callback != null) {
                callback.award(AchievementType.NORTHERN_SPIRIT);
            }
        }
        return JoinMorrisResult.JOINED;
    }

    /**
     * Player steals a MORRIS_STICK_PROP from a dancer. All 6 dancers give chase.
     * Adds stick to inventory, records crime, adds Notoriety +1, WantedSystem +1,
     * seeds {@link RumourType#MORRIS_STICK_THEFT}, awards {@link AchievementType#STOLE_THE_STICK}.
     *
     * @param inventory   player inventory
     * @param currentHour current in-game hour
     * @param witness     nearby NPC for rumour seeding (may be null)
     * @param playerX     player X position
     * @param playerY     player Y position
     * @param playerZ     player Z position
     * @param callback    achievement callback (may be null)
     * @return result of the steal attempt
     */
    public StealStickResult stealMorrisStick(Inventory inventory, float currentHour,
                                              NPC witness,
                                              float playerX, float playerY, float playerZ,
                                              AchievementCallback callback) {
        if (!eventActive) return StealStickResult.EVENT_NOT_ACTIVE;
        if (!isMorrisActive(currentHour)) return StealStickResult.WRONG_TIME;
        if (morrisStickStolen) return StealStickResult.ALREADY_STOLEN;

        morrisStickStolen = true;
        inventory.addItem(Material.MORRIS_STICK_PROP, 1);

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.MORRIS_STICK_THEFT);
        }
        if (notorietySystem != null && callback != null) {
            notorietySystem.addNotoriety(MORRIS_STICK_NOTORIETY, callback);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(MORRIS_STICK_WANTED, playerX, playerY, playerZ, null);
        }
        if (rumourNetwork != null && witness != null) {
            rumourNetwork.addRumour(witness,
                new Rumour(RumourType.MORRIS_STICK_THEFT,
                    "Morris dancer got his stick nicked by some tourist-looking bloke. Absolute scenes."));
        }
        if (!stoleTheStickAwarded) {
            stoleTheStickAwarded = true;
            if (callback != null) {
                callback.award(AchievementType.STOLE_THE_STICK);
            }
        }
        return StealStickResult.STOLEN;
    }

    /**
     * Player sabotages the Morris Dancers with a BANANA_SKIN_PROP.
     * Triggers NoiseSystem level 7. Consumes the banana skin.
     *
     * @param inventory   player inventory
     * @param currentHour current in-game hour
     * @return result of the sabotage attempt
     */
    public SabotageMorrisResult sabotageMorris(Inventory inventory, float currentHour) {
        if (!eventActive) return SabotageMorrisResult.EVENT_NOT_ACTIVE;
        if (!isMorrisActive(currentHour)) return SabotageMorrisResult.WRONG_TIME;
        if (!inventory.hasItem(Material.BANANA_SKIN)) return SabotageMorrisResult.NO_BANANA_SKIN;

        inventory.removeItem(Material.BANANA_SKIN, 1);
        morrisSabotaged = true;
        return SabotageMorrisResult.SABOTAGED;
    }

    /**
     * Returns whether Morris Dancers are currently active.
     *
     * @param currentHour current in-game hour
     * @return true if within 11:00–15:00
     */
    public boolean isMorrisActive(float currentHour) {
        return currentHour >= EVENT_OPEN_HOUR && currentHour < MORRIS_CLOSE_HOUR;
    }

    /**
     * Spawns the six Morris Dancer NPCs.
     *
     * @return list of spawned NPCs
     */
    public List<NPC> spawnMorrisDancers() {
        List<NPC> npcs = new ArrayList<>();
        if (!eventActive) return npcs;
        for (int i = 0; i < MORRIS_DANCER_COUNT; i++) {
            npcs.add(new NPC(NPCType.MORRIS_DANCER, 0f, 0f, 0f));
        }
        return npcs;
    }

    // ── Mechanic 2 — Wetherspoons Lock-In ────────────────────────────────────

    /**
     * Buy a DOOM_BAR_PINT from Terry. 3 pints triggers DRUNK_STATE.
     *
     * @param inventory   player inventory
     * @param currentHour current in-game hour (11.0–26.0)
     * @return result of the purchase attempt
     */
    public BuyPintResult buyPint(Inventory inventory, float currentHour) {
        if (!eventActive) return BuyPintResult.EVENT_NOT_ACTIVE;
        if (!isWetherspoonsOpen(currentHour)) return BuyPintResult.WRONG_TIME;
        if (inventory.getItemCount(Material.COIN) < DOOM_BAR_PINT_COST) {
            return BuyPintResult.INSUFFICIENT_FUNDS;
        }

        inventory.removeItem(Material.COIN, DOOM_BAR_PINT_COST);
        inventory.addItem(Material.DOOM_BAR_PINT, 1);
        pintsConsumed++;
        if (pintsConsumed >= DRUNK_STATE_PINTS) {
            playerDrunk = true;
        }
        return BuyPintResult.PURCHASED;
    }

    /**
     * Attempt to enter the Wetherspoons lock-in (midnight).
     * Requires Notoriety Tier ≤ {@link #LOCK_IN_MAX_TIER}.
     *
     * @param currentHour    current in-game hour (≥ 24.0 for midnight)
     * @param notorietyTier  player's current Notoriety Tier (0–5)
     * @return result of the lock-in admission attempt
     */
    public LockInResult enterLockIn(float currentHour, int notorietyTier) {
        if (!eventActive) return LockInResult.EVENT_NOT_ACTIVE;
        if (currentHour < LOCK_IN_HOUR) return LockInResult.TOO_EARLY;
        if (notorietyTier > LOCK_IN_MAX_TIER) return LockInResult.TIER_TOO_HIGH;
        playerInLockIn = true;
        return LockInResult.ADMITTED;
    }

    /**
     * Returns whether the Wetherspoons is open (11:00–26:00, i.e. until 02:00 next day).
     *
     * @param currentHour current in-game hour
     * @return true if within opening hours
     */
    public boolean isWetherspoonsOpen(float currentHour) {
        return currentHour >= EVENT_OPEN_HOUR && currentHour < (LOCK_IN_HOUR + LOCK_IN_END_HOUR);
    }

    // ── Mechanic 3 — Bar Flag Heist ───────────────────────────────────────────

    /**
     * Player climbs the BAR_STOOL_PROP and takes the ST_GEORGE_FLAG_PROP.
     * Results: flag added to inventory, player ejected, 3-day pub ban,
     * Notoriety +3, {@link CrimeType#FLAG_THEFT} recorded,
     * {@link RumourType#FLAG_HEIST} seeded, {@link AchievementType#TOOK_THE_FLAG} awarded.
     *
     * @param inventory  player inventory
     * @param onStool    true if player is standing on BAR_STOOL_PROP
     * @param witness    nearby NPC for rumour seeding (may be null)
     * @param callback   achievement callback (may be null)
     * @return result of the heist attempt
     */
    public BarFlagHeistResult takeBarFlag(Inventory inventory, boolean onStool,
                                           NPC witness, AchievementCallback callback) {
        if (!eventActive) return BarFlagHeistResult.EVENT_NOT_ACTIVE;
        if (barFlagTaken) return BarFlagHeistResult.ALREADY_TAKEN;
        if (!onStool) return BarFlagHeistResult.NOT_ON_STOOL;

        barFlagTaken = true;
        playerEjectedFromPub = true;
        inventory.addItem(Material.ST_GEORGE_FLAG_PROP, 1);

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.FLAG_THEFT);
        }
        if (notorietySystem != null && callback != null) {
            notorietySystem.addNotoriety(BAR_FLAG_NOTORIETY, callback);
        }
        if (rumourNetwork != null && witness != null) {
            rumourNetwork.addRumour(witness,
                new Rumour(RumourType.FLAG_HEIST,
                    "Someone nicked the flag off the Wetherspoons wall. In broad daylight."));
        }
        if (!tookTheFlagAwarded) {
            tookTheFlagAwarded = true;
            if (callback != null) {
                callback.award(AchievementType.TOOK_THE_FLAG);
            }
        }
        return BarFlagHeistResult.FLAG_TAKEN;
    }

    // ── Mechanic 4 — Rooftop Flag Heist ──────────────────────────────────────

    /**
     * Player climbs the DRAINPIPE_PROP. Call when the player has held E for
     * {@link #DRAINPIPE_CLIMB_SECONDS} seconds on the drainpipe.
     */
    public void climbDrainpipe() {
        playerOnRoof = true;
    }

    /**
     * Player takes the ROOF_FLAG_PROP from the roof mount.
     * Requires CCTV disabled and player on roof.
     * Notoriety +5, WantedSystem +1, {@link CrimeType#ROOFTOP_FLAG_THEFT},
     * {@link RumourType#ROOF_FLAG_TAKEN} seeded, {@link AchievementType#OFF_THE_ROOF} awarded.
     *
     * @param inventory    player inventory
     * @param cctvDisabled true if CCTV has been disabled (spray-paint, power cut, or distraction)
     * @param witness      nearby NPC for rumour seeding (may be null)
     * @param playerX      player X position
     * @param playerY      player Y position
     * @param playerZ      player Z position
     * @param callback     achievement callback (may be null)
     * @return result of the heist attempt
     */
    public RoofFlagHeistResult takeRoofFlag(Inventory inventory, boolean cctvDisabled,
                                             NPC witness,
                                             float playerX, float playerY, float playerZ,
                                             AchievementCallback callback) {
        if (!eventActive) return RoofFlagHeistResult.EVENT_NOT_ACTIVE;
        if (roofFlagTaken) return RoofFlagHeistResult.ALREADY_TAKEN;
        if (!playerOnRoof) return RoofFlagHeistResult.NOT_ON_ROOF;
        if (!cctvDisabled) return RoofFlagHeistResult.CCTV_ACTIVE;

        roofFlagTaken = true;
        inventory.addItem(Material.ROOF_FLAG_PROP, 1);

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.ROOFTOP_FLAG_THEFT);
        }
        if (notorietySystem != null && callback != null) {
            notorietySystem.addNotoriety(ROOF_FLAG_NOTORIETY, callback);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(ROOF_FLAG_WANTED, playerX, playerY, playerZ, null);
        }
        if (rumourNetwork != null && witness != null) {
            rumourNetwork.addRumour(witness,
                new Rumour(RumourType.ROOF_FLAG_TAKEN,
                    "Some lad climbed up the drainpipe and had the roof flag away. On St George's Day of all days."));
        }
        if (!offTheRoofAwarded) {
            offTheRoofAwarded = true;
            if (callback != null) {
                callback.award(AchievementType.OFF_THE_ROOF);
            }
        }
        return RoofFlagHeistResult.FLAG_TAKEN;
    }

    // ── Mechanic 3 & 4 — Fencing flags to Mirek ──────────────────────────────

    /**
     * Fence a flag item to Mirek for COIN.
     * ST_GEORGE_FLAG_PROP → 5 COIN; ROOF_FLAG_PROP → 12 COIN.
     *
     * @param flagItem      the flag material to fence (ST_GEORGE_FLAG_PROP or ROOF_FLAG_PROP)
     * @param inventory     player inventory
     * @param mirekPresent  true if MIREK_FENCE NPC is present
     * @return result of the fence attempt
     */
    public FenceFlagResult fenceFlag(Material flagItem, Inventory inventory, boolean mirekPresent) {
        if (!eventActive) return FenceFlagResult.EVENT_NOT_ACTIVE;
        if (!mirekPresent) return FenceFlagResult.MIREK_NOT_PRESENT;
        if (!inventory.hasItem(flagItem)) return FenceFlagResult.NO_FLAG;

        inventory.removeItem(flagItem, 1);
        int value = (flagItem == Material.ROOF_FLAG_PROP)
                ? ROOF_FLAG_FENCE_VALUE
                : BAR_FLAG_FENCE_VALUE;
        inventory.addItem(Material.COIN, value);
        return FenceFlagResult.FENCED;
    }

    // ── Mechanic 5 — Crowd Scuffle ────────────────────────────────────────────

    /**
     * Called when an ENGLAND_FLAG_NPC and a COUNTER_PROTEST_NPC come within
     * {@link #SCUFFLE_TRIGGER_DISTANCE} blocks of each other at 14:00+.
     * Fires {@link EventType#CROWD_SCUFFLE}, NoiseSystem level 8.
     * Police respond in 30 seconds.
     *
     * @param playerHasMobilePhone true if player has MOBILE_PHONE in inventory
     * @param witness              nearby NPC for rumour seeding (may be null)
     * @param callback             achievement callback (may be null)
     * @return EventType.CROWD_SCUFFLE, or null if already fired
     */
    public EventType triggerCrowdScuffle(boolean playerHasMobilePhone,
                                          NPC witness,
                                          AchievementCallback callback) {
        if (!eventActive) return null;
        if (crowdScuffleFired) return null;
        crowdScuffleFired = true;

        if (playerHasMobilePhone && !communityOutrageSeeded) {
            communityOutrageSeeded = true;
            if (rumourNetwork != null && witness != null) {
                rumourNetwork.addRumour(witness,
                    new Rumour(RumourType.COMMUNITY_OUTRAGE,
                        "It kicked off in the park — English lads and the counter-protesters going at each other."));
            }
        }
        return EventType.CROWD_SCUFFLE;
    }

    // ── Bonus — Best England Shirt Competition ────────────────────────────────

    /**
     * Resolve the Best England Shirt competition at 15:00.
     * Player must have ENGLAND_SHIRT_PROP with condition ≥ 85 and not be drunk.
     *
     * @param inventory       player inventory
     * @param currentHour     current in-game hour
     * @param shirtCondition  shirt condition (0–100)
     * @param callback        achievement callback (may be null)
     * @return result of the competition
     */
    public ShirtCompResult resolveShirtComp(Inventory inventory, float currentHour,
                                             int shirtCondition, AchievementCallback callback) {
        if (!eventActive) return ShirtCompResult.EVENT_NOT_ACTIVE;
        if (currentHour < SHIRT_COMPETITION_HOUR) return ShirtCompResult.WRONG_TIME;
        if (shirtCompResolved) return ShirtCompResult.ALREADY_RESOLVED;
        if (!inventory.hasItem(Material.ENGLAND_SHIRT_PROP)) return ShirtCompResult.NO_SHIRT;
        if (shirtCondition < SHIRT_MIN_CONDITION) return ShirtCompResult.SHIRT_TOO_WORN;
        if (playerDrunk) return ShirtCompResult.PLAYER_DRUNK;

        shirtCompResolved = true;
        inventory.addItem(Material.COIN, SHIRT_WIN_PRIZE);
        if (!bestDressedAwarded) {
            bestDressedAwarded = true;
            if (callback != null) {
                callback.award(AchievementType.BEST_DRESSED_PATRIOT);
            }
        }
        return ShirtCompResult.WON;
    }

    // ── State accessors ───────────────────────────────────────────────────────

    public boolean isEventActive()            { return eventActive; }
    public boolean isMorrisDancersSpawned()   { return morrisDancersSpawned; }
    public boolean isMorrisDancersDespawned() { return morrisDancersDespawned; }
    public boolean isPlayerWatchedMorris()    { return playerWatchedMorris; }
    public boolean isPlayerJoinedMorris()     { return playerJoinedMorris; }
    public boolean isMorrisStickStolen()      { return morrisStickStolen; }
    public boolean isMorrisSabotaged()        { return morrisSabotaged; }
    public int getPintsConsumed()             { return pintsConsumed; }
    public boolean isPlayerDrunk()            { return playerDrunk; }
    public boolean isLockInStarted()          { return lockInStarted; }
    public boolean isLockInPoliceCheckFired() { return lockInPoliceCheckFired; }
    public boolean isLockInEnded()            { return lockInEnded; }
    public boolean isPlayerInLockIn()         { return playerInLockIn; }
    public boolean isBarFlagTaken()           { return barFlagTaken; }
    public boolean isPlayerEjectedFromPub()   { return playerEjectedFromPub; }
    public boolean isPlayerOnRoof()           { return playerOnRoof; }
    public boolean isRoofFlagTaken()          { return roofFlagTaken; }
    public boolean isTensionStarted()         { return tensionStarted; }
    public boolean isCrowdScuffleFired()      { return crowdScuffleFired; }
    public boolean isCommunityOutrageSeeded() { return communityOutrageSeeded; }
    public boolean isShirtCompOpen()          { return shirtCompOpen; }
    public boolean isShirtCompResolved()      { return shirtCompResolved; }

    // ── Test helpers ──────────────────────────────────────────────────────────

    public void setEventActiveForTesting(boolean active)          { this.eventActive = active; }
    public void setMorrisDancersSpawnedForTesting(boolean v)      { this.morrisDancersSpawned = v; }
    public void setMorrisStickStolenForTesting(boolean v)         { this.morrisStickStolen = v; }
    public void setPlayerJoinedMorrisForTesting(boolean v)        { this.playerJoinedMorris = v; }
    public void setPintsConsumedForTesting(int n)                 { this.pintsConsumed = n; }
    public void setPlayerDrunkForTesting(boolean v)               { this.playerDrunk = v; }
    public void setPlayerOnRoofForTesting(boolean v)              { this.playerOnRoof = v; }
    public void setRoofFlagTakenForTesting(boolean v)             { this.roofFlagTaken = v; }
    public void setBarFlagTakenForTesting(boolean v)              { this.barFlagTaken = v; }
    public void setLockInStartedForTesting(boolean v)             { this.lockInStarted = v; }
    public void setPlayerInLockInForTesting(boolean v)            { this.playerInLockIn = v; }
    public void setTensionStartedForTesting(boolean v)            { this.tensionStarted = v; }
    public void setCrowdScuffleFiredForTesting(boolean v)         { this.crowdScuffleFired = v; }
    public void setShirtCompOpenForTesting(boolean v)             { this.shirtCompOpen = v; }
    public void setShirtCompResolvedForTesting(boolean v)         { this.shirtCompResolved = v; }
    public void setLockInPoliceCheckFiredForTesting(boolean v)    { this.lockInPoliceCheckFired = v; }
    public void setLockInEndedForTesting(boolean v)               { this.lockInEnded = v; }
}

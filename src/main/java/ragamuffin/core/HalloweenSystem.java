package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.core.NotorietySystem.AchievementCallback;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1430: Northfield Halloween — Trick-or-Treat, the Egg Run &amp; the
 * Estate Pumpkin Carver.
 *
 * <h3>Overview</h3>
 * On day-of-year 304 (31 October), 18:00–23:59, Northfield's estate transforms
 * for Halloween. SCHOOL_KID NPCs in costumes follow a trick-or-treat route,
 * eggs are thrown at doors and cars, pumpkins are carved and placed as
 * JACK_O_LANTERN_PROPs, the charity shop pops up a costume stall, and the
 * midnight wind-down descends into ROWDY chaos managed by the Neighbourhood
 * Watch.
 *
 * <h3>Mechanic 1 — Trick-or-Treat Route</h3>
 * From 18:00–21:30, up to 8 {@link NPCType#SCHOOL_KID} NPCs in costume
 * (WITCH_COSTUME or PUMPKIN_HEAD_MASK) follow a 6-door route within 50 blocks
 * of the park. Player can join with a costume for {@link Material#TRICK_OR_TREAT_BAG}
 * rewards. Intercepting bags triggers PETTY_THEFT + Notoriety +3. SCHOOL_KIDs
 * enter {@link NPCState#ANGRY} when intercepted.
 * Achievement: {@link AchievementType#TRICK_OR_TREATER} on completing all 6 doors.
 *
 * <h3>Mechanic 2 — Egg Run</h3>
 * {@link Material#RAW_EGG} items are available from Iceland and behind Greggs.
 * Right-click throwing applies {@link PropType#EGGED_DOOR_PROP} decal on hit.
 * Triggers {@link CrimeType#CRIMINAL_DAMAGE} + Notoriety +2. Hitting a
 * {@link NPCType#NEIGHBOURHOOD_WATCH} NPC triggers WantedSystem +1.
 * Achievement: {@link AchievementType#HALLOWEEN_VANDAL} for 5 hits.
 *
 * <h3>Mechanic 3 — Pumpkin Carving &amp; Placement</h3>
 * {@link Material#RAW_PUMPKIN} from the corner shop (2 COIN). Carving yields
 * {@link Material#CARVED_PUMPKIN} + {@link Material#PUMPKIN_INNARDS}.
 * Placing a CARVED_PUMPKIN creates {@link PropType#JACK_O_LANTERN_PROP} with
 * a 3-block light radius.
 * Achievement: {@link AchievementType#PUMPKIN_KING} for 3 placements.
 *
 * <h3>Mechanic 4 — Costume Shop Pop-up</h3>
 * CharityShopSystem Halloween sub-mode 15:00–20:00: WITCH_COSTUME (2 COIN)
 * and PUMPKIN_HEAD_MASK (2 COIN) on a trestle table. Wearing costume triggers
 * DisguiseSystem (−40% recognition).
 * Achievement: {@link AchievementType#COSTUME_CRIME} for committing crime in costume.
 *
 * <h3>Mechanic 5 — Midnight Wind-down &amp; Estate Chaos</h3>
 * At 22:00: 3–5 {@link NPCType#YOUTH_GANG} NPCs enter {@link NPCState#ROWDY},
 * {@link NeighbourhoodWatchSystem#addAnger(int)} +15, NoiseSystem level 6.0.
 * At 23:00: 2 {@link NPCType#POLICE} NPCs begin estate sweep.
 * Achievement: {@link AchievementType#NIGHT_OWL} for surviving past midnight.
 * WantedSystem ≥ 2 stars → {@link RumourType#HALLOWEEN_CHAOS} rumour +
 * NewspaperSystem headline + NeighbourhoodSystem VIBES −3.
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@link AchievementType#TRICK_OR_TREATER} — complete full 6-door route</li>
 *   <li>{@link AchievementType#HALLOWEEN_VANDAL} — egg 5 targets</li>
 *   <li>{@link AchievementType#COSTUME_CRIME} — commit crime while in costume</li>
 *   <li>{@link AchievementType#PUMPKIN_KING} — place 3 JACK_O_LANTERN_PROPs</li>
 *   <li>{@link AchievementType#NIGHT_OWL} — still on estate after midnight</li>
 * </ul>
 */
public class HalloweenSystem {

    // ── Day-of-year constant ──────────────────────────────────────────────────

    /** Halloween day-of-year (31 October = day 304 in a non-leap year). */
    public static final int HALLOWEEN_DAY = 304;

    // ── Event hours ───────────────────────────────────────────────────────────

    /** Costume shop pop-up opens at 15:00. */
    public static final float COSTUME_SHOP_OPEN_HOUR = 15.0f;

    /** Costume shop pop-up closes at 20:00. */
    public static final float COSTUME_SHOP_CLOSE_HOUR = 20.0f;

    /** Trick-or-treat route opens at 18:00. */
    public static final float TRICK_OR_TREAT_OPEN_HOUR = 18.0f;

    /** Trick-or-treat route closes at 21:30. */
    public static final float TRICK_OR_TREAT_CLOSE_HOUR = 21.5f;

    /** Event window opens at 18:00. */
    public static final float EVENT_OPEN_HOUR = 18.0f;

    /** Midnight wind-down begins at 22:00 (ROWDY YOUTH_GANG spawns). */
    public static final float WIND_DOWN_HOUR = 22.0f;

    /** Police sweep begins at 23:00. */
    public static final float POLICE_SWEEP_HOUR = 23.0f;

    /** Event window ends at 23:59. */
    public static final float EVENT_CLOSE_HOUR = 23.99f;

    /** NIGHT_OWL achievement check: still on estate after midnight (00:00 next day). */
    public static final float NIGHT_OWL_HOUR = 0.0f;

    // ── Trick-or-treat constants ──────────────────────────────────────────────

    /** Number of SCHOOL_KID NPCs in the trick-or-treat group. */
    public static final int TRICK_OR_TREAT_NPC_COUNT = 8;

    /** Number of trick-or-treat doors in the route. */
    public static final int TRICK_OR_TREAT_DOOR_COUNT = 6;

    /** If THUNDERSTORM: number of trick-or-treaters is halved. */
    public static final int TRICK_OR_TREAT_STORM_COUNT = 4;

    /** Notoriety gained when player intercepts trick-or-treat bags. */
    public static final int INTERCEPT_NOTORIETY = 3;

    // ── Egg run constants ─────────────────────────────────────────────────────

    /** Number of RAW_EGG items spawned in Iceland. */
    public static final int EGG_SPAWN_ICELAND_COUNT = 6;

    /** Number of RAW_EGG items spawned behind Greggs. */
    public static final int EGG_SPAWN_GREGGS_COUNT = 3;

    /** Noise level when an egg is thrown. */
    public static final float EGG_THROW_NOISE = 3.0f;

    /** Notoriety gained when an egg hits a door/car/prop. */
    public static final int EGG_HIT_NOTORIETY = 2;

    /** Wanted stars gained when NEIGHBOURHOOD_WATCH witnesses an egg throw. */
    public static final int EGG_WATCH_WANTED_STARS = 1;

    /** Number of egged surfaces required before VIBES penalty applies. */
    public static final int EGGED_SURFACE_VIBES_THRESHOLD = 3;

    /** VIBES penalty per EGGED_SURFACE_VIBES_THRESHOLD egged surfaces. */
    public static final int EGGED_SURFACE_VIBES_PENALTY = 1;

    /** Number of egg hits needed for HALLOWEEN_VANDAL achievement. */
    public static final int HALLOWEEN_VANDAL_TARGET = 5;

    // ── Pumpkin constants ─────────────────────────────────────────────────────

    /** Cost of RAW_PUMPKIN at the corner shop (in COIN). */
    public static final int PUMPKIN_CORNER_SHOP_COST = 2;

    /** Corner shop max pumpkin stock on Halloween. */
    public static final int PUMPKIN_CORNER_SHOP_STOCK = 4;

    /** Number of JACK_O_LANTERN_PROP placements needed for PUMPKIN_KING. */
    public static final int PUMPKIN_KING_TARGET = 3;

    // ── Costume constants ─────────────────────────────────────────────────────

    /** Cost of WITCH_COSTUME at the charity shop pop-up (in COIN). */
    public static final int WITCH_COSTUME_COST = 2;

    /** Cost of PUMPKIN_HEAD_MASK at the charity shop pop-up (in COIN). */
    public static final int PUMPKIN_HEAD_MASK_COST = 2;

    /** Stock of WITCH_COSTUME at the charity shop pop-up. */
    public static final int WITCH_COSTUME_STOCK = 3;

    /** Stock of PUMPKIN_HEAD_MASK at the charity shop pop-up. */
    public static final int PUMPKIN_HEAD_MASK_STOCK = 2;

    // ── Midnight wind-down constants ──────────────────────────────────────────

    /** Minimum number of YOUTH_GANG NPCs spawned at 22:00. */
    public static final int ROWDY_GANG_MIN = 3;

    /** Maximum number of YOUTH_GANG NPCs spawned at 22:00. */
    public static final int ROWDY_GANG_MAX = 5;

    /** Anger added to NeighbourhoodWatchSystem at 22:00. */
    public static final int WIND_DOWN_WATCH_ANGER = 15;

    /** NoiseSystem level set in estate zone at 22:00. */
    public static final float WIND_DOWN_NOISE_LEVEL = 6.0f;

    /** Number of POLICE NPCs spawned for the estate sweep at 23:00. */
    public static final int POLICE_SWEEP_COUNT = 2;

    /** Wanted stars required to trigger HALLOWEEN_CHAOS rumour. */
    public static final int CHAOS_WANTED_THRESHOLD = 2;

    /** VIBES penalty when HALLOWEEN_CHAOS headline is published. */
    public static final int CHAOS_VIBES_PENALTY = 3;

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Results of joining the trick-or-treat route at a door. */
    public enum TrickOrTreatResult {
        /** Player received a TRICK_OR_TREAT_BAG reward. */
        REWARDED,
        /** Player is Wanted — resident slammed door. */
        DOOR_SLAMMED,
        /** Player is not in costume. */
        NO_COSTUME,
        /** This door is dark (no answer). */
        DARK_DOOR,
        /** Trick-or-treat route not active (wrong time or day). */
        NOT_ACTIVE,
        /** Player already completed this door. */
        ALREADY_DONE
    }

    /** Results of intercepting a trick-or-treat bag. */
    public enum InterceptResult {
        /** Bag successfully intercepted. PETTY_THEFT + Notoriety +3. */
        INTERCEPTED,
        /** No bag at this door to intercept. */
        NO_BAG,
        /** Trick-or-treat route not active. */
        NOT_ACTIVE
    }

    /** Results of throwing a RAW_EGG. */
    public enum EggThrowResult {
        /** Egg hit a door/wall/car — EGGED_DOOR_PROP placed, CRIMINAL_DAMAGE. */
        HIT_SURFACE,
        /** Egg hit an NPC — NPC enters ANGRY state. */
        HIT_NPC,
        /** Egg hit a NEIGHBOURHOOD_WATCH NPC — WantedSystem +1. */
        HIT_WATCH_NPC,
        /** Player does not have RAW_EGG in inventory. */
        NO_EGG,
        /** Event not active. */
        NOT_ACTIVE
    }

    /** Results of carving a pumpkin. */
    public enum CarvePumpkinResult {
        /** Pumpkin carved successfully — CARVED_PUMPKIN + PUMPKIN_INNARDS yielded. */
        CARVED,
        /** Player does not have RAW_PUMPKIN in inventory. */
        NO_PUMPKIN,
        /** Event not active. */
        NOT_ACTIVE
    }

    /** Results of placing a JACK_O_LANTERN_PROP. */
    public enum PlaceJackOLanternResult {
        /** JACK_O_LANTERN_PROP placed successfully. */
        PLACED,
        /** Player does not have CARVED_PUMPKIN in inventory. */
        NO_CARVED_PUMPKIN,
        /** Event not active. */
        NOT_ACTIVE
    }

    /** Broad event outcomes that callers may want to react to. */
    public enum EventType {
        /** Trick-or-treat route opens at 18:00. */
        TRICK_OR_TREAT_OPENED,
        /** Trick-or-treat route closes at 21:30. */
        TRICK_OR_TREAT_CLOSED,
        /** ROWDY YOUTH_GANG spawned at 22:00. */
        WIND_DOWN_STARTED,
        /** Police sweep begun at 23:00. */
        POLICE_SWEEP_STARTED,
        /** Event window closed at 23:59. */
        EVENT_CLOSED,
        /** HALLOWEEN_CHAOS rumour threshold reached. */
        HALLOWEEN_CHAOS_TRIGGERED
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Whether the event is currently active. */
    private boolean eventActive = false;

    /** Whether the trick-or-treat route has been opened this session. */
    private boolean trickOrTreatOpened = false;

    /** Whether the trick-or-treat route has been closed this session. */
    private boolean trickOrTreatClosed = false;

    /** Whether the ROWDY wind-down phase has started (22:00). */
    private boolean windDownStarted = false;

    /** Whether the police sweep has started (23:00). */
    private boolean policeSweepStarted = false;

    /** Number of doors completed in the trick-or-treat route. */
    private int doorsCompleted = 0;

    /** Whether TRICK_OR_TREATER achievement has been awarded. */
    private boolean trickOrTreaterAwarded = false;

    /** Total egg hits (surfaces + NPCs) this session. */
    private int eggHitsTotal = 0;

    /** Number of egged surfaces (doors/cars) for VIBES tracking. */
    private int eggedSurfaces = 0;

    /** Whether HALLOWEEN_VANDAL achievement has been awarded. */
    private boolean halloweenVandalAwarded = false;

    /** Whether HALLOWEEN_CHAOS has been triggered this session. */
    private boolean halloweenChaosTriggered = false;

    /** Number of JACK_O_LANTERN_PROPs placed this session. */
    private int jackOLanternsPlaced = 0;

    /** Whether PUMPKIN_KING achievement has been awarded. */
    private boolean pumpkinKingAwarded = false;

    /** Whether the player is currently wearing a Halloween costume. */
    private boolean playerInCostume = false;

    /** Whether COSTUME_CRIME achievement has been awarded. */
    private boolean costumeCrimeAwarded = false;

    /** Whether NIGHT_OWL achievement has been awarded. */
    private boolean nightOwlAwarded = false;

    /** Whether a thunderstorm is active (halves trick-or-treater count). */
    private boolean thunderstormActive = false;

    // ── Integrated systems (optional wiring) ─────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private NeighbourhoodWatchSystem neighbourhoodWatchSystem;
    private NeighbourhoodSystem neighbourhoodSystem;
    private NewspaperSystem newspaperSystem;
    private NoiseSystem noiseSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public HalloweenSystem() {
        this(new Random());
    }

    public HalloweenSystem(Random random) {
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

    public void setNeighbourhoodWatchSystem(NeighbourhoodWatchSystem n) {
        this.neighbourhoodWatchSystem = n;
    }

    public void setNeighbourhoodSystem(NeighbourhoodSystem n) {
        this.neighbourhoodSystem = n;
    }

    public void setNewspaperSystem(NewspaperSystem n) {
        this.newspaperSystem = n;
    }

    public void setNoiseSystem(NoiseSystem n) {
        this.noiseSystem = n;
    }

    // ── Event day detection ───────────────────────────────────────────────────

    /**
     * Returns whether today is Halloween (day-of-year 304).
     *
     * @param dayOfYear current day of year (1-based, e.g. 304 = 31 October)
     * @return true if today is Halloween
     */
    public boolean isHalloweenDay(int dayOfYear) {
        return dayOfYear == HALLOWEEN_DAY;
    }

    /**
     * Returns whether the main event window is open for the given hour.
     *
     * @param hour current in-game hour
     * @return true if within 18:00–23:59
     */
    public boolean isEventHour(float hour) {
        return hour >= EVENT_OPEN_HOUR && hour <= EVENT_CLOSE_HOUR;
    }

    /**
     * Returns whether the trick-or-treat window is open for the given hour.
     *
     * @param hour current in-game hour
     * @return true if within 18:00–21:30
     */
    public boolean isTrickOrTreatHour(float hour) {
        return hour >= TRICK_OR_TREAT_OPEN_HOUR && hour < TRICK_OR_TREAT_CLOSE_HOUR;
    }

    /**
     * Returns whether the costume shop pop-up is open.
     *
     * @param hour current in-game hour
     * @return true if within 15:00–20:00
     */
    public boolean isCostumeShopOpen(float hour) {
        return hour >= COSTUME_SHOP_OPEN_HOUR && hour < COSTUME_SHOP_CLOSE_HOUR;
    }

    /**
     * Returns whether the Halloween costume disguise bonus is active.
     * Returns true during the event window on Halloween day.
     *
     * @param dayOfYear current day of year
     * @param hour      current in-game hour
     * @return true if Halloween costume bonus is active
     */
    public boolean isHalloweenBonusActive(int dayOfYear, float hour) {
        return isHalloweenDay(dayOfYear) && isEventHour(hour);
    }

    // ── Event lifecycle ───────────────────────────────────────────────────────

    /**
     * Open the Halloween event for this session. Resets all session state.
     * Call when 18:00 is reached on day 304.
     *
     * @param isThunderstorm true if a thunderstorm is active (halves trick-or-treaters)
     */
    public void openEvent(boolean isThunderstorm) {
        eventActive = true;
        thunderstormActive = isThunderstorm;
        trickOrTreatOpened = false;
        trickOrTreatClosed = false;
        windDownStarted = false;
        policeSweepStarted = false;
        doorsCompleted = 0;
        trickOrTreaterAwarded = false;
        eggHitsTotal = 0;
        eggedSurfaces = 0;
        halloweenVandalAwarded = false;
        halloweenChaosTriggered = false;
        jackOLanternsPlaced = 0;
        pumpkinKingAwarded = false;
        playerInCostume = false;
        costumeCrimeAwarded = false;
        nightOwlAwarded = false;
    }

    /**
     * Close the event.
     *
     * @return EventType.EVENT_CLOSED
     */
    public EventType closeEvent() {
        eventActive = false;
        return EventType.EVENT_CLOSED;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update. Advances the Halloween event through its phases:
     * trick-or-treat open/close, wind-down at 22:00, police sweep at 23:00.
     *
     * @param currentHour  current in-game hour
     * @param witnessNpc   a nearby NPC for rumour seeding (may be null)
     * @param callback     achievement callback (may be null)
     * @return event that fired this tick, or null
     */
    public EventType update(float currentHour, NPC witnessNpc,
                            AchievementCallback callback) {
        if (!eventActive) return null;

        // ── Trick-or-treat opens at 18:00 ─────────────────────────────────────
        if (!trickOrTreatOpened && currentHour >= TRICK_OR_TREAT_OPEN_HOUR) {
            trickOrTreatOpened = true;
            return EventType.TRICK_OR_TREAT_OPENED;
        }

        // ── Trick-or-treat closes at 21:30 ────────────────────────────────────
        if (trickOrTreatOpened && !trickOrTreatClosed
                && currentHour >= TRICK_OR_TREAT_CLOSE_HOUR) {
            trickOrTreatClosed = true;
            return EventType.TRICK_OR_TREAT_CLOSED;
        }

        // ── Wind-down at 22:00: ROWDY YOUTH_GANG + Watch anger ────────────────
        if (!windDownStarted && currentHour >= WIND_DOWN_HOUR) {
            windDownStarted = true;
            if (neighbourhoodWatchSystem != null) {
                neighbourhoodWatchSystem.addAnger(WIND_DOWN_WATCH_ANGER);
            }
            if (noiseSystem != null) {
                noiseSystem.addNoise(WIND_DOWN_NOISE_LEVEL);
            }
            return EventType.WIND_DOWN_STARTED;
        }

        // ── Police sweep at 23:00 ─────────────────────────────────────────────
        if (!policeSweepStarted && currentHour >= POLICE_SWEEP_HOUR) {
            policeSweepStarted = true;
            return EventType.POLICE_SWEEP_STARTED;
        }

        return null;
    }

    // ── Mechanic 1 — Trick-or-treat route ─────────────────────────────────────

    /**
     * Player attempts to join the trick-or-treat route at a door.
     * Requires WITCH_COSTUME or PUMPKIN_HEAD_MASK equipped, and no Wanted stars.
     *
     * @param inventory    player inventory
     * @param currentHour  current in-game hour
     * @param wantedStars  player's current wanted stars
     * @param doorIndex    index of the door (0–5)
     * @param isDark       true if this door has no answer (30% of doors)
     * @param callback     achievement callback (may be null)
     * @return result of the trick-or-treat attempt
     */
    public TrickOrTreatResult joinTrickOrTreat(Inventory inventory, float currentHour,
                                                int wantedStars, int doorIndex,
                                                boolean isDark,
                                                AchievementCallback callback) {
        if (!eventActive) return TrickOrTreatResult.NOT_ACTIVE;
        if (!isTrickOrTreatHour(currentHour)) return TrickOrTreatResult.NOT_ACTIVE;
        if (doorIndex < 0 || doorIndex >= TRICK_OR_TREAT_DOOR_COUNT) {
            return TrickOrTreatResult.NOT_ACTIVE;
        }
        if (isDark) return TrickOrTreatResult.DARK_DOOR;
        if (wantedStars > 0) return TrickOrTreatResult.DOOR_SLAMMED;
        if (!isPlayerInHalloweenCostume(inventory)) return TrickOrTreatResult.NO_COSTUME;

        // Reward the player
        inventory.addItem(Material.TRICK_OR_TREAT_BAG, 1);
        doorsCompleted = Math.min(doorsCompleted + 1, TRICK_OR_TREAT_DOOR_COUNT);

        // Check TRICK_OR_TREATER achievement
        if (!trickOrTreaterAwarded && doorsCompleted >= TRICK_OR_TREAT_DOOR_COUNT) {
            trickOrTreaterAwarded = true;
            if (callback != null) {
                callback.award(AchievementType.TRICK_OR_TREATER);
            }
        }

        return TrickOrTreatResult.REWARDED;
    }

    /**
     * Complete the full trick-or-treat route for a player (used in integration tests).
     * Calls {@link #joinTrickOrTreat} for each of the 6 doors.
     *
     * @param inventory    player inventory (must have costume equipped)
     * @param currentHour  current in-game hour
     * @param wantedStars  player's current wanted stars
     * @param callback     achievement callback (may be null)
     * @return number of doors successfully completed
     */
    public int completeTrickOrTreatRoute(Inventory inventory, float currentHour,
                                          int wantedStars, AchievementCallback callback) {
        int completed = 0;
        for (int i = 0; i < TRICK_OR_TREAT_DOOR_COUNT; i++) {
            TrickOrTreatResult result = joinTrickOrTreat(
                    inventory, currentHour, wantedStars, i, false, callback);
            if (result == TrickOrTreatResult.REWARDED) {
                completed++;
            }
        }
        return completed;
    }

    /**
     * Player intercepts a trick-or-treat bag from the doorstep.
     * Triggers PETTY_THEFT + Notoriety +3. SCHOOL_KID NPCs become ANGRY.
     *
     * @param inventory    player inventory (receives the bag)
     * @param currentHour  current in-game hour
     * @param hasBagAtDoor true if a bag is currently on the doorstep
     * @param witnessNpc   nearby NPC for rumour seeding (may be null)
     * @param callback     achievement callback (may be null)
     * @return result of the intercept attempt
     */
    public InterceptResult interceptTrickOrTreatBag(Inventory inventory,
                                                     float currentHour,
                                                     boolean hasBagAtDoor,
                                                     NPC witnessNpc,
                                                     AchievementCallback callback) {
        if (!eventActive || !isTrickOrTreatHour(currentHour)) {
            return InterceptResult.NOT_ACTIVE;
        }
        if (!hasBagAtDoor) return InterceptResult.NO_BAG;

        inventory.addItem(Material.TRICK_OR_TREAT_BAG, 1);

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.PETTY_THEFT);
        }
        if (notorietySystem != null && callback != null) {
            notorietySystem.addNotoriety(INTERCEPT_NOTORIETY, callback);
        }
        if (rumourNetwork != null && witnessNpc != null) {
            rumourNetwork.addRumour(witnessNpc,
                new Rumour(RumourType.ANTI_SOCIAL_BEHAVIOUR,
                    "Some scumbag nicked the trick-or-treat bag right off the " +
                    "doorstep. Kids were gutted."));
        }

        // Check COSTUME_CRIME
        if (playerInCostume) {
            checkCostumeCrime(callback);
        }

        return InterceptResult.INTERCEPTED;
    }

    // ── Mechanic 2 — Egg Run ──────────────────────────────────────────────────

    /**
     * Player throws a RAW_EGG at a target door/wall/car position.
     * On hit: EGGED_DOOR_PROP placed, CRIMINAL_DAMAGE recorded, Notoriety +2,
     * NoiseSystem spike. If NEIGHBOURHOOD_WATCH within range: WantedSystem +1.
     *
     * @param inventory         player inventory (must have RAW_EGG)
     * @param targetPos         world position of the target (for tracking)
     * @param watchNpcNearby    true if a NEIGHBOURHOOD_WATCH NPC is within 10 blocks
     * @param witnessNpc        nearby NPC for rumour seeding (may be null)
     * @param callback          achievement callback (may be null)
     * @return result of the egg throw
     */
    public EggThrowResult throwEgg(Inventory inventory, boolean watchNpcNearby,
                                    NPC witnessNpc, AchievementCallback callback) {
        if (!eventActive) return EggThrowResult.NOT_ACTIVE;
        if (!inventory.hasItem(Material.RAW_EGG)) return EggThrowResult.NO_EGG;

        inventory.removeItem(Material.RAW_EGG, 1);

        // Apply criminal damage
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.CRIMINAL_DAMAGE);
        }
        if (notorietySystem != null && callback != null) {
            notorietySystem.addNotoriety(EGG_HIT_NOTORIETY, callback);
        }
        if (noiseSystem != null) {
            noiseSystem.addNoise(EGG_THROW_NOISE);
        }

        eggedSurfaces++;
        eggHitsTotal++;

        // VIBES penalty per threshold
        if (neighbourhoodSystem != null && eggedSurfaces % EGGED_SURFACE_VIBES_THRESHOLD == 0) {
            int currentVibes = neighbourhoodSystem.getVibes();
            neighbourhoodSystem.setVibes(currentVibes - EGGED_SURFACE_VIBES_PENALTY);
        }

        // Seed rumour
        if (rumourNetwork != null && witnessNpc != null) {
            rumourNetwork.addRumour(witnessNpc,
                new Rumour(RumourType.ANTI_SOCIAL_BEHAVIOUR,
                    "Someone was egging houses on the terrace again. Classic."));
        }

        // Check if NEIGHBOURHOOD_WATCH witnessed
        if (watchNpcNearby) {
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(EGG_WATCH_WANTED_STARS, 0f, 0f, 0f, null);
            }
        }

        // Check HALLOWEEN_VANDAL achievement
        checkHalloweenVandal(callback);

        // Check HALLOWEEN_CHAOS
        checkHalloweenChaos(witnessNpc, callback);

        if (watchNpcNearby) {
            return EggThrowResult.HIT_WATCH_NPC;
        }
        return EggThrowResult.HIT_SURFACE;
    }

    /**
     * Player throws a RAW_EGG at an NPC.
     * NPC enters ANGRY state; if NEIGHBOURHOOD_WATCH, WantedSystem +1.
     *
     * @param inventory   player inventory (must have RAW_EGG)
     * @param targetNpc   the NPC hit
     * @param witnessNpc  a different nearby NPC for rumour seeding (may be null)
     * @param callback    achievement callback (may be null)
     * @return result of the egg throw
     */
    public EggThrowResult throwEggAtNpc(Inventory inventory, NPC targetNpc,
                                         NPC witnessNpc, AchievementCallback callback) {
        if (!eventActive) return EggThrowResult.NOT_ACTIVE;
        if (!inventory.hasItem(Material.RAW_EGG)) return EggThrowResult.NO_EGG;

        inventory.removeItem(Material.RAW_EGG, 1);
        eggHitsTotal++;

        if (noiseSystem != null) {
            noiseSystem.addNoise(EGG_THROW_NOISE);
        }

        boolean isWatchNpc = (targetNpc != null
                && targetNpc.getType() == NPCType.NEIGHBOURHOOD_WATCH);

        if (targetNpc != null) {
            targetNpc.setState(NPCState.ANGRY);
        }

        if (isWatchNpc) {
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(EGG_WATCH_WANTED_STARS, 0f, 0f, 0f, null);
            }
        }

        if (notorietySystem != null && callback != null) {
            notorietySystem.addNotoriety(EGG_HIT_NOTORIETY, callback);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.CRIMINAL_DAMAGE);
        }

        // Seed rumour if watched
        if (rumourNetwork != null && witnessNpc != null) {
            rumourNetwork.addRumour(witnessNpc,
                new Rumour(RumourType.ANTI_SOCIAL_BEHAVIOUR,
                    "Some muppet egged someone on the street. Happy Halloween."));
        }

        checkHalloweenVandal(callback);
        checkHalloweenChaos(witnessNpc, callback);

        if (isWatchNpc) {
            return EggThrowResult.HIT_WATCH_NPC;
        }
        return EggThrowResult.HIT_NPC;
    }

    // ── Mechanic 3 — Pumpkin carving ──────────────────────────────────────────

    /**
     * Player carves a RAW_PUMPKIN into CARVED_PUMPKIN + PUMPKIN_INNARDS.
     * No tool required. Player must have RAW_PUMPKIN in inventory.
     *
     * @param inventory player inventory
     * @return result of the carving attempt
     */
    public CarvePumpkinResult carvePumpkin(Inventory inventory) {
        if (!eventActive) return CarvePumpkinResult.NOT_ACTIVE;
        if (!inventory.hasItem(Material.RAW_PUMPKIN)) return CarvePumpkinResult.NO_PUMPKIN;

        inventory.removeItem(Material.RAW_PUMPKIN, 1);
        inventory.addItem(Material.CARVED_PUMPKIN, 1);
        inventory.addItem(Material.PUMPKIN_INNARDS, 1);

        return CarvePumpkinResult.CARVED;
    }

    /**
     * Player places a CARVED_PUMPKIN as a JACK_O_LANTERN_PROP (right-click on
     * a block face). Emits warm orange light (3-block radius).
     *
     * @param inventory  player inventory (must have CARVED_PUMPKIN)
     * @param callback   achievement callback (may be null)
     * @return result of the placement attempt
     */
    public PlaceJackOLanternResult placeJackOLantern(Inventory inventory,
                                                      AchievementCallback callback) {
        if (!eventActive) return PlaceJackOLanternResult.NOT_ACTIVE;
        if (!inventory.hasItem(Material.CARVED_PUMPKIN)) {
            return PlaceJackOLanternResult.NO_CARVED_PUMPKIN;
        }

        inventory.removeItem(Material.CARVED_PUMPKIN, 1);
        jackOLanternsPlaced++;

        // Check PUMPKIN_KING achievement
        if (!pumpkinKingAwarded && jackOLanternsPlaced >= PUMPKIN_KING_TARGET) {
            pumpkinKingAwarded = true;
            if (callback != null) {
                callback.award(AchievementType.PUMPKIN_KING);
            }
        }

        return PlaceJackOLanternResult.PLACED;
    }

    // ── Mechanic 4 — Costume pop-up ───────────────────────────────────────────

    /**
     * Returns whether the player's inventory contains a Halloween costume
     * (WITCH_COSTUME or PUMPKIN_HEAD_MASK).
     *
     * @param inventory player inventory
     * @return true if player has a Halloween costume
     */
    public boolean isPlayerInHalloweenCostume(Inventory inventory) {
        return inventory.hasItem(Material.WITCH_COSTUME)
                || inventory.hasItem(Material.PUMPKIN_HEAD_MASK);
    }

    /**
     * Notifies the system that the player has equipped a Halloween costume.
     *
     * @param equipped true if player has put on a Halloween costume
     */
    public void setPlayerInCostume(boolean equipped) {
        this.playerInCostume = equipped;
    }

    /**
     * Call this when the player commits a crime (called by game logic when any
     * crime is recorded). If the player is in a Halloween costume during the
     * event, awards COSTUME_CRIME achievement.
     *
     * @param callback achievement callback (may be null)
     */
    public void onPlayerCommittedCrime(AchievementCallback callback) {
        if (eventActive && playerInCostume) {
            checkCostumeCrime(callback);
        }
    }

    /**
     * Returns the disguise bonus for a Halloween costume.
     * During the event (day 304, 18:00–23:59): 40% recognition reduction bonus (20 points).
     * Outside event: 5 points (barely works — "it's not Halloween, you div").
     *
     * @param dayOfYear current day of year
     * @param hour      current in-game hour
     * @return disguise bonus integer
     */
    public int getHalloweenDisguiseBonus(int dayOfYear, float hour) {
        return isHalloweenBonusActive(dayOfYear, hour) ? 20 : 5;
    }

    // ── Mechanic 5 — Midnight wind-down ───────────────────────────────────────

    /**
     * Spawn the ROWDY YOUTH_GANG NPCs for the 22:00 wind-down.
     * Returns 3–5 YOUTH_GANG NPCs in NPCState.ROWDY.
     *
     * @return list of spawned YOUTH_GANG NPCs
     */
    public List<NPC> spawnRowdyGang() {
        List<NPC> gang = new ArrayList<>();
        if (!windDownStarted) return gang;

        int count = ROWDY_GANG_MIN + random.nextInt(ROWDY_GANG_MAX - ROWDY_GANG_MIN + 1);
        for (int i = 0; i < count; i++) {
            NPC npc = new NPC(NPCType.YOUTH_GANG, 0f, 0f, 0f);
            npc.setState(NPCState.ROWDY);
            gang.add(npc);
        }
        return gang;
    }

    /**
     * Spawn SCHOOL_KID NPCs for the trick-or-treat route.
     * Returns up to 8 SCHOOL_KID NPCs (4 if thunderstorm is active).
     *
     * @return list of spawned SCHOOL_KID NPCs
     */
    public List<NPC> spawnTrickOrTreaters() {
        List<NPC> kids = new ArrayList<>();
        if (!eventActive) return kids;

        int count = thunderstormActive ? TRICK_OR_TREAT_STORM_COUNT : TRICK_OR_TREAT_NPC_COUNT;
        for (int i = 0; i < count; i++) {
            NPC npc = new NPC(NPCType.SCHOOL_KID, 0f, 0f, 0f);
            kids.add(npc);
        }
        return kids;
    }

    /**
     * Spawn POLICE NPCs for the 23:00 estate sweep.
     *
     * @return list of spawned POLICE NPCs
     */
    public List<NPC> spawnPoliceSweep() {
        List<NPC> police = new ArrayList<>();
        if (!policeSweepStarted) return police;

        for (int i = 0; i < POLICE_SWEEP_COUNT; i++) {
            NPC npc = new NPC(NPCType.POLICE, 0f, 0f, 0f);
            police.add(npc);
        }
        return police;
    }

    /**
     * Check NIGHT_OWL achievement — player still on estate after midnight.
     * Call this when the game clock reaches 00:00 on 1 November.
     *
     * @param playerWantedStars current player wanted stars (must be 0)
     * @param callback          achievement callback (may be null)
     * @return true if NIGHT_OWL was newly awarded
     */
    public boolean checkNightOwl(int playerWantedStars, AchievementCallback callback) {
        if (!nightOwlAwarded && playerWantedStars == 0) {
            nightOwlAwarded = true;
            if (callback != null) {
                callback.award(AchievementType.NIGHT_OWL);
            }
            return true;
        }
        return false;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Check and award HALLOWEEN_VANDAL achievement if threshold reached.
     */
    private void checkHalloweenVandal(AchievementCallback callback) {
        if (!halloweenVandalAwarded && eggHitsTotal >= HALLOWEEN_VANDAL_TARGET) {
            halloweenVandalAwarded = true;
            if (callback != null) {
                callback.award(AchievementType.HALLOWEEN_VANDAL);
            }
        }
    }

    /**
     * Check and trigger HALLOWEEN_CHAOS if WantedSystem ≥ 2 stars.
     * Seeds HALLOWEEN_CHAOS rumour, triggers newspaper headline, VIBES −3.
     */
    private void checkHalloweenChaos(NPC witnessNpc, AchievementCallback callback) {
        if (halloweenChaosTriggered) return;
        int currentStars = (wantedSystem != null) ? wantedSystem.getWantedStars() : 0;
        if (currentStars >= CHAOS_WANTED_THRESHOLD) {
            halloweenChaosTriggered = true;

            if (rumourNetwork != null && witnessNpc != null) {
                rumourNetwork.addRumour(witnessNpc,
                    new Rumour(RumourType.HALLOWEEN_CHAOS,
                        "Halloween was mental this year. Someone went proper mad — " +
                        "police were called three times. Classic Northfield."));
            }

            if (newspaperSystem != null) {
                newspaperSystem.recordEvent(new NewspaperSystem.InfamyEvent(
                    "HALLOWEEN_CHAOS", "Northfield Estate", null, null, 2, null, null, 6));
            }

            if (neighbourhoodSystem != null) {
                int currentVibes = neighbourhoodSystem.getVibes();
                neighbourhoodSystem.setVibes(currentVibes - CHAOS_VIBES_PENALTY);
            }
        }
    }

    /**
     * Check and award COSTUME_CRIME achievement (internal).
     */
    private void checkCostumeCrime(AchievementCallback callback) {
        if (!costumeCrimeAwarded && playerInCostume) {
            costumeCrimeAwarded = true;
            if (callback != null) {
                callback.award(AchievementType.COSTUME_CRIME);
            }
        }
    }

    // ── State accessors (for testing) ─────────────────────────────────────────

    public boolean isEventActive()           { return eventActive; }
    public boolean isTrickOrTreatOpened()    { return trickOrTreatOpened; }
    public boolean isTrickOrTreatClosed()    { return trickOrTreatClosed; }
    public boolean isWindDownStarted()       { return windDownStarted; }
    public boolean isPoliceSweepStarted()    { return policeSweepStarted; }
    public int getDoorsCompleted()           { return doorsCompleted; }
    public int getEggHitsTotal()             { return eggHitsTotal; }
    public int getEggedSurfaces()            { return eggedSurfaces; }
    public int getJackOLanternsPlaced()      { return jackOLanternsPlaced; }
    public boolean isPlayerInCostume()       { return playerInCostume; }
    public boolean isHalloweenChaosTriggered() { return halloweenChaosTriggered; }
    public boolean isTrickOrTreaterAwarded() { return trickOrTreaterAwarded; }
    public boolean isHalloweenVandalAwarded() { return halloweenVandalAwarded; }
    public boolean isPumpkinKingAwarded()    { return pumpkinKingAwarded; }
    public boolean isCostumeCrimeAwarded()   { return costumeCrimeAwarded; }
    public boolean isNightOwlAwarded()       { return nightOwlAwarded; }
    public boolean isThunderstormActive()    { return thunderstormActive; }

    // ── Test helpers ──────────────────────────────────────────────────────────

    public void setEventActiveForTesting(boolean active)      { this.eventActive = active; }
    public void setWindDownStartedForTesting(boolean started) { this.windDownStarted = started; }
    public void setPoliceSweepStartedForTesting(boolean s)    { this.policeSweepStarted = s; }
    public void setPlayerInCostumeForTesting(boolean c)       { this.playerInCostume = c; }
    public void setDoorsCompletedForTesting(int n)            { this.doorsCompleted = n; }
    public void setEggHitsTotalForTesting(int n)              { this.eggHitsTotal = n; }
    public void setJackOLanternsPlacedForTesting(int n)       { this.jackOLanternsPlaced = n; }
}

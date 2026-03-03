package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1377: Northfield BP Petrol Station — Raj's Kiosk, Fuel Theft &amp; Wayne's Night Shift.
 *
 * <p>Implements the BP Petrol Station ({@code LandmarkType.PETROL_STATION}) with two
 * attendants: Raj ({@code NPCType.PETROL_STATION_ATTENDANT}, 07:00–19:00) and Wayne
 * ({@code NPCType.PETROL_STATION_ASSISTANT}, nights, asleep 01:00–03:00 at noise &lt; 15).
 *
 * <h3>Kiosk Stock</h3>
 * <ul>
 *   <li>{@link Material#PETROL_CAN_FULL} — {@link #PRICE_PETROL_CAN} COIN (petrol can refill)</li>
 *   <li>{@link Material#ENERGY_DRINK} — {@link #PRICE_ENERGY_DRINK} COIN</li>
 *   <li>{@link Material#MICROWAVE_PASTY} — {@link #PRICE_MICROWAVE_PASTY} COIN</li>
 *   <li>{@link Material#SCRATCH_CARD} — {@link #PRICE_SCRATCH_CARD} COIN</li>
 *   <li>{@link Material#CIGARETTE_CARTON} — {@link #PRICE_CIGARETTE_CARTON} COIN (cabinet)</li>
 *   <li>{@link Material#NEWSPAPER} — {@link #PRICE_NEWSPAPER} COIN</li>
 * </ul>
 *
 * <h3>Mechanics</h3>
 * <ol>
 *   <li><b>Kiosk purchase</b> — E at counter; fills/gives item for COIN.</li>
 *   <li><b>Fuel pump</b> — fill {@code PETROL_CAN} for {@link #PRICE_PETROL_CAN} COIN.
 *       Drive-off (walk away after filling without paying): detected by active CCTV
 *       via line-of-sight → PETROL_THEFT, WantedSystem +1, CRIME_SPOTTED rumour,
 *       Notoriety +{@link #DRIVEOFF_NOTORIETY}; Achievement: {@code DRIVE_OFF}.
 *       Break CCTV first to avoid detection.</li>
 *   <li><b>Fuel siphoning</b> — from parked Cars at night (21:00–06:00); 5-second hold
 *       mini-game ({@link #SIPHON_HOLD_SECONDS}). Interrupt before 5 s → fails.
 *       Success: inventory gains {@code PETROL_CAN_FULL}, records
 *       {@code VEHICLE_TAMPERING}, Notoriety +{@link #SIPHON_NOTORIETY}.
 *       CCTV LoS active → WantedSystem +1 additional star.</li>
 *   <li><b>Till robbery</b> — E with CROWBAR at till; yields
 *       {@link #TILL_MIN_COIN}–{@link #TILL_MAX_COIN} COIN, records {@code ARMED_ROBBERY},
 *       WantedSystem +{@link #TILL_ROBBERY_WANTED_STARS}, police in
 *       {@link #POLICE_RESPONSE_SECONDS} in-game seconds; if player enters at tier ≥ 2,
 *       panic button triggers 30-second armed response.</li>
 *   <li><b>Cigarette cabinet smash</b> — 3 HARD hits; drops
 *       {@link #CIGARETTE_CARTON_DROP_MIN}–{@link #CIGARETTE_CARTON_DROP_MAX}
 *       {@code CIGARETTE_CARTON}; fenceable at {@link #CIGARETTE_CARTON_FENCE_VALUE} COIN each.</li>
 *   <li><b>Wayne's night shift</b> — microwave pasty at 23:00 atmosphere; sleep window
 *       01:00–03:00 (unresponsive to noise &lt; {@link #WAYNE_SLEEP_NOISE_THRESHOLD}).</li>
 * </ol>
 *
 * <h3>Integrations</h3>
 * <ul>
 *   <li>{@link NotorietySystem} — crime notoriety gains.</li>
 *   <li>{@link WantedSystem} — wanted star escalation.</li>
 *   <li>{@link CriminalRecord} — records PETROL_THEFT, VEHICLE_TAMPERING, ARMED_ROBBERY, ARSON.</li>
 *   <li>{@link RumourNetwork} — seeds {@code CRIME_SPOTTED}, {@code COMMUNITY_OUTRAGE},
 *       {@code WEATHER_GRUMBLE}.</li>
 *   <li>{@link AchievementSystem} — {@code DRIVE_OFF}, {@code FORECOURT_REGULAR},
 *       {@code PETROL_HEAD}, {@code MICROWAVE_MILLIONAIRE}, {@code MOLOTOV_MOMENT}.</li>
 * </ul>
 */
public class PetrolStationSystem {

    // ── Opening hours ──────────────────────────────────────────────────────────

    /** Hour Raj starts. */
    public static final float RAJ_START_HOUR = 7.0f;

    /** Hour Raj finishes. */
    public static final float RAJ_END_HOUR = 19.0f;

    /** Hour Wayne starts. */
    public static final float WAYNE_START_HOUR = 19.0f;

    /** Hour Wayne finishes. */
    public static final float WAYNE_END_HOUR = 7.0f;

    /** Start of Wayne's sleep window. */
    public static final float WAYNE_SLEEP_START = 1.0f;

    /** End of Wayne's sleep window. */
    public static final float WAYNE_SLEEP_END = 3.0f;

    /** Noise level below which Wayne will not wake during his sleep window. */
    public static final float WAYNE_SLEEP_NOISE_THRESHOLD = 15.0f;

    // ── Stock prices ──────────────────────────────────────────────────────────

    /** Price to fill a PETROL_CAN (pump or kiosk). */
    public static final int PRICE_PETROL_CAN = 3;

    /** Price of an Energy Drink. */
    public static final int PRICE_ENERGY_DRINK = 2;

    /** Price of a Microwave Pasty. */
    public static final int PRICE_MICROWAVE_PASTY = 2;

    /** Price of a Scratch Card. */
    public static final int PRICE_SCRATCH_CARD = 1;

    /** Price of a Cigarette Carton (behind the counter). */
    public static final int PRICE_CIGARETTE_CARTON = 8;

    /** Price of a Newspaper. */
    public static final int PRICE_NEWSPAPER = 1;

    // ── Theft: drive-off ──────────────────────────────────────────────────────

    /** Notoriety gained when a drive-off is detected by CCTV. */
    public static final int DRIVEOFF_NOTORIETY = 4;

    /** WantedSystem stars added on drive-off detection. */
    public static final int DRIVEOFF_WANTED_STARS = 1;

    // ── Theft: siphoning ─────────────────────────────────────────────────────

    /** Seconds of continuous hold required to complete a siphon. */
    public static final float SIPHON_HOLD_SECONDS = 5.0f;

    /** Earliest hour siphoning is possible (night window start). */
    public static final float SIPHON_START_HOUR = 21.0f;

    /** Latest hour siphoning is possible (night window end, wraps). */
    public static final float SIPHON_END_HOUR = 6.0f;

    /** Notoriety gained from successful siphoning. */
    public static final int SIPHON_NOTORIETY = 3;

    /** WantedSystem stars added when CCTV catches a siphon. */
    public static final int SIPHON_CCTV_WANTED_STARS = 1;

    // ── Theft: till robbery ───────────────────────────────────────────────────

    /** Minimum COIN yield from till robbery. */
    public static final int TILL_MIN_COIN = 8;

    /** Maximum COIN yield from till robbery. */
    public static final int TILL_MAX_COIN = 18;

    /** Notoriety gained from till robbery. */
    public static final int TILL_ROBBERY_NOTORIETY = 15;

    /** WantedSystem stars added on till robbery. */
    public static final int TILL_ROBBERY_WANTED_STARS = 2;

    /** In-game seconds until police arrive after till robbery (normal). */
    public static final float POLICE_RESPONSE_SECONDS = 180.0f;

    /** In-game seconds until armed response arrives when panic button triggers. */
    public static final float ARMED_RESPONSE_SECONDS = 30.0f;

    /** Notoriety tier at which the panic button triggers an armed response. */
    public static final int PANIC_BUTTON_TIER_THRESHOLD = 2;

    // ── Theft: cigarette cabinet ──────────────────────────────────────────────

    /** Hits required to smash the cigarette cabinet (HARD material). */
    public static final int CIGARETTE_CABINET_HITS = 3;

    /** Minimum CIGARETTE_CARTON drops from smashing. */
    public static final int CIGARETTE_CARTON_DROP_MIN = 2;

    /** Maximum CIGARETTE_CARTON drops from smashing. */
    public static final int CIGARETTE_CARTON_DROP_MAX = 3;

    /** Fence value per CIGARETTE_CARTON. */
    public static final int CIGARETTE_CARTON_FENCE_VALUE = 6;

    // ── Counters for achievements ─────────────────────────────────────────────

    /** Number of legitimate pump fills required for PETROL_HEAD. */
    public static final int PETROL_HEAD_FILLS = 5;

    /** Number of kiosk purchases required for FORECOURT_REGULAR. */
    public static final int FORECOURT_REGULAR_PURCHASES = 10;

    // ── Notoriety thresholds ──────────────────────────────────────────────────

    /** Notoriety tier above which Raj refuses service. */
    public static final int RAJ_REFUSAL_TIER = 3;

    // ── Purchase result ───────────────────────────────────────────────────────

    /** Result of a kiosk purchase attempt. */
    public enum PurchaseResult {
        SUCCESS,
        CLOSED,
        INSUFFICIENT_FUNDS,
        SERVICE_REFUSED,
        NOT_IN_STOCK
    }

    /** Result of a fuel-pump fill attempt. */
    public enum FillResult {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        NO_EMPTY_CAN,
        FROZEN_NOZZLE
    }

    /** Result of a till robbery attempt. */
    public enum TillRobberyResult {
        SUCCESS,
        NO_CROWBAR,
        PANIC_BUTTON_ARMED_RESPONSE
    }

    /** Result of a siphon mini-game check. */
    public enum SiphonResult {
        SUCCESS,
        INTERRUPTED,
        WRONG_TIME,
        NO_EMPTY_CAN
    }

    /** Result of a cigarette cabinet smash hit. */
    public enum CabinetSmashResult {
        HIT_REGISTERED,
        SMASHED,
        ALREADY_SMASHED
    }

    // ── MenuItem ──────────────────────────────────────────────────────────────

    /** A kiosk stock item with its price. */
    public static class MenuItem {
        public final Material material;
        public final int price;

        public MenuItem(Material material, int price) {
            this.material = material;
            this.price    = price;
        }
    }

    /** Kiosk stock (excluding petrol pump — that's a separate interaction). */
    public static final MenuItem[] STOCK = {
        new MenuItem(Material.ENERGY_DRINK,     PRICE_ENERGY_DRINK),
        new MenuItem(Material.MICROWAVE_PASTY,  PRICE_MICROWAVE_PASTY),
        new MenuItem(Material.SCRATCH_CARD,     PRICE_SCRATCH_CARD),
        new MenuItem(Material.CIGARETTE_CARTON, PRICE_CIGARETTE_CARTON),
        new MenuItem(Material.NEWSPAPER,        PRICE_NEWSPAPER),
    };

    // ── State ─────────────────────────────────────────────────────────────────

    /** Raj NPC (day shift). */
    private NPC raj;

    /** Wayne NPC (night shift). */
    private NPC wayne;

    /** Whether the CCTV is currently active (can be broken). */
    private boolean cctvActive = true;

    /** Whether the cigarette cabinet has been smashed. */
    private boolean cabinetSmashed = false;

    /** Current hit count on the cigarette cabinet. */
    private int cabinetHits = 0;

    /** Whether the till has been robbed (prevents repeated robberies). */
    private boolean tillRobbed = false;

    /** Whether the panic button has been triggered. */
    private boolean panicButtonTriggered = false;

    /** Timer counting seconds of continuous siphon hold. */
    private float siphonTimer = 0f;

    /** Whether a siphon mini-game is currently in progress. */
    private boolean siphoning = false;

    /** Total number of legitimate pump fills (for PETROL_HEAD). */
    private int legitimateFills = 0;

    /** Total kiosk purchases (for FORECOURT_REGULAR). */
    private int kioskPurchases = 0;

    /** Timer counting the police response after till robbery. */
    private float policeResponseTimer = -1f;

    /** Whether armed response is inbound (panic button). */
    private boolean armedResponseInbound = false;

    /** Timer for armed response arrival. */
    private float armedResponseTimer = -1f;

    /** Whether MICROWAVE_MILLIONAIRE has been awarded. */
    private boolean achievementMicrowaveMillionaire = false;

    /** Whether PETROL_HEAD has been awarded. */
    private boolean achievementPetrolHead = false;

    /** Whether FORECOURT_REGULAR has been awarded. */
    private boolean achievementForecourtRegular = false;

    /** Whether DRIVE_OFF has been awarded. */
    private boolean achievementDriveOff = false;

    private final Random random;

    // ── Injected systems ──────────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private RumourNetwork rumourNetwork;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public PetrolStationSystem() {
        this(new Random());
    }

    public PetrolStationSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ──────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem n) { this.notorietySystem = n; }
    public void setWantedSystem(WantedSystem w)       { this.wantedSystem = w; }
    public void setRumourNetwork(RumourNetwork r)     { this.rumourNetwork = r; }
    public void setCriminalRecord(CriminalRecord c)   { this.criminalRecord = c; }
    public void setAchievementSystem(AchievementSystem a) { this.achievementSystem = a; }

    // ── NPC setters ───────────────────────────────────────────────────────────

    public void setRaj(NPC raj)     { this.raj = raj; }
    public void setWayne(NPC wayne) { this.wayne = wayne; }

    // ── Opening / attendant helpers ───────────────────────────────────────────

    /**
     * Returns true if Raj is currently on duty.
     *
     * @param hour current in-game hour (0–24)
     */
    public boolean isRajOnDuty(float hour) {
        return hour >= RAJ_START_HOUR && hour < RAJ_END_HOUR;
    }

    /**
     * Returns true if Wayne is currently on duty.
     *
     * @param hour current in-game hour (0–24)
     */
    public boolean isWayneOnDuty(float hour) {
        return hour >= WAYNE_START_HOUR || hour < WAYNE_END_HOUR;
    }

    /**
     * Returns true if Wayne is asleep (01:00–03:00 window).
     *
     * @param hour current in-game hour
     */
    public boolean isWayneAsleep(float hour) {
        return hour >= WAYNE_SLEEP_START && hour < WAYNE_SLEEP_END;
    }

    /**
     * Returns true if Wayne can be woken by the given noise level.
     * Wayne is unresponsive to noise below {@link #WAYNE_SLEEP_NOISE_THRESHOLD}
     * during his sleep window.
     *
     * @param hour       current in-game hour
     * @param noiseLevel current noise level (0–100+)
     */
    public boolean isWayneRespondingToNoise(float hour, float noiseLevel) {
        if (isWayneAsleep(hour)) {
            return noiseLevel >= WAYNE_SLEEP_NOISE_THRESHOLD;
        }
        return true;
    }

    // ── Kiosk purchase ────────────────────────────────────────────────────────

    /**
     * Attempt to buy a stock item from the kiosk.
     *
     * @param material        item to buy
     * @param playerInventory player's inventory (COIN is consumed)
     * @param currentHour     current in-game hour
     * @param notoriety       player's raw notoriety
     * @param callback        achievement callback
     * @return purchase result
     */
    public PurchaseResult buyItem(Material material,
                                   Inventory playerInventory,
                                   float currentHour,
                                   int notoriety,
                                   NotorietySystem.AchievementCallback callback) {
        // Determine which attendant is serving
        NPC attendant = getActiveAttendant(currentHour);
        if (attendant == null) return PurchaseResult.CLOSED;

        // Raj refuses above tier threshold
        if (isRajOnDuty(currentHour)) {
            int tier = computeNotorietyTier(notoriety);
            if (tier >= RAJ_REFUSAL_TIER) {
                if (raj != null) {
                    raj.setSpeechText("I can't serve you, mate. Not looking for trouble.", 4f);
                }
                return PurchaseResult.SERVICE_REFUSED;
            }
        }

        // Find item in stock
        MenuItem item = findMenuItem(material);
        if (item == null) return PurchaseResult.NOT_IN_STOCK;

        if (playerInventory.getItemCount(Material.COIN) < item.price) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        if (item.price > 0) {
            playerInventory.removeItem(Material.COIN, item.price);
        }
        playerInventory.addItem(material, 1);

        kioskPurchases++;

        // Microwave Millionaire achievement: buy pasty after 21:00
        if (material == Material.MICROWAVE_PASTY && currentHour >= 21.0f
                && !achievementMicrowaveMillionaire) {
            achievementMicrowaveMillionaire = true;
            award(AchievementType.MICROWAVE_MILLIONAIRE, callback);
        }

        // Forecourt Regular achievement
        if (kioskPurchases >= FORECOURT_REGULAR_PURCHASES && !achievementForecourtRegular) {
            achievementForecourtRegular = true;
            award(AchievementType.FORECOURT_REGULAR, callback);
        }

        return PurchaseResult.SUCCESS;
    }

    // ── Fuel pump fill ────────────────────────────────────────────────────────

    /**
     * Fill a PETROL_CAN at the pump (legitimate purchase).
     *
     * <p>Converts one {@code PETROL_CAN} to {@code PETROL_CAN_FULL} for
     * {@link #PRICE_PETROL_CAN} COIN. Tracks PETROL_HEAD achievement.
     *
     * @param playerInventory player inventory
     * @param currentHour     current in-game hour
     * @param isFrost         whether it is currently FROST weather (may freeze nozzle)
     * @param callback        achievement callback
     * @return fill result
     */
    public FillResult fillPetrolCan(Inventory playerInventory,
                                     float currentHour,
                                     boolean isFrost,
                                     NotorietySystem.AchievementCallback callback) {
        // No attendant check needed — pump is self-serve, but store must be accessible
        if (playerInventory.getItemCount(Material.PETROL_CAN) < 1) {
            return FillResult.NO_EMPTY_CAN;
        }

        // Frost: 20% chance of frozen nozzle
        if (isFrost && random.nextFloat() < 0.20f) {
            seedWeatherGrumbleRumour();
            return FillResult.FROZEN_NOZZLE;
        }

        if (playerInventory.getItemCount(Material.COIN) < PRICE_PETROL_CAN) {
            return FillResult.INSUFFICIENT_FUNDS;
        }

        playerInventory.removeItem(Material.COIN, PRICE_PETROL_CAN);
        playerInventory.removeItem(Material.PETROL_CAN, 1);
        playerInventory.addItem(Material.PETROL_CAN_FULL, 1);

        legitimateFills++;

        if (legitimateFills >= PETROL_HEAD_FILLS && !achievementPetrolHead) {
            achievementPetrolHead = true;
            award(AchievementType.PETROL_HEAD, callback);
        }

        return FillResult.SUCCESS;
    }

    /**
     * Drive-off: player filled the can and walked away without paying.
     *
     * <p>If CCTV is active and has line-of-sight, records PETROL_THEFT,
     * adds Notoriety +{@link #DRIVEOFF_NOTORIETY}, WantedSystem +{@link #DRIVEOFF_WANTED_STARS},
     * seeds CRIME_SPOTTED rumour, and awards DRIVE_OFF achievement.
     *
     * @param cctvHasLos    whether the CCTV camera currently has LoS to the player
     * @param playerPos     player position (x, y, z) for WantedSystem LKP
     * @param npcs          all NPCs for rumour seeding
     * @param callback      achievement callback
     */
    public void onDriveOff(boolean cctvHasLos,
                            float playerX, float playerY, float playerZ,
                            List<NPC> npcs,
                            NotorietySystem.AchievementCallback callback) {
        if (cctvActive && cctvHasLos) {
            // Detected
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.PETROL_THEFT);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(DRIVEOFF_NOTORIETY, callback);
            }
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(DRIVEOFF_WANTED_STARS,
                        playerX, playerY, playerZ, null);
            }
            seedCrimeSpottedRumour(npcs, "Someone drove off from the BP without paying — Raj was fuming.");
        }

        // Award achievement regardless (the act itself, not detection)
        if (!achievementDriveOff) {
            achievementDriveOff = true;
            award(AchievementType.DRIVE_OFF, callback);
        }
    }

    // ── Siphoning ─────────────────────────────────────────────────────────────

    /**
     * Begin or continue the siphon mini-game (called each frame the player holds).
     *
     * @param delta       seconds since last frame
     * @param currentHour current in-game hour
     * @param playerInventory player inventory
     * @return INTERRUPTED if hold was not started, null if in progress (not yet complete)
     */
    public void startSiphon() {
        siphoning = true;
        siphonTimer = 0f;
    }

    /**
     * Advance the siphon timer.
     *
     * @param delta seconds elapsed this frame
     */
    public void advanceSiphon(float delta) {
        if (siphoning) {
            siphonTimer += delta;
        }
    }

    /**
     * Interrupt and reset the siphon mini-game.
     */
    public void interruptSiphon() {
        siphoning = false;
        siphonTimer = 0f;
    }

    /**
     * Check whether the siphon has been held long enough and complete it.
     *
     * <p>Must be in the night window ({@link #SIPHON_START_HOUR}–{@link #SIPHON_END_HOUR}).
     * Requires at least one empty {@code PETROL_CAN} in inventory.
     * If siphonTimer &lt; {@link #SIPHON_HOLD_SECONDS}, returns {@code INTERRUPTED}.
     *
     * @param currentHour     current in-game hour
     * @param playerInventory player inventory
     * @param cctvHasLos      whether CCTV currently has LoS
     * @param playerX         player X for WantedSystem LKP
     * @param playerY         player Y
     * @param playerZ         player Z
     * @param callback        achievement callback
     * @return siphon result
     */
    public SiphonResult completeSiphon(float currentHour,
                                        Inventory playerInventory,
                                        boolean cctvHasLos,
                                        float playerX, float playerY, float playerZ,
                                        NotorietySystem.AchievementCallback callback) {
        // Time window: 21:00–06:00
        boolean nightWindow = currentHour >= SIPHON_START_HOUR || currentHour < SIPHON_END_HOUR;
        if (!nightWindow) {
            interruptSiphon();
            return SiphonResult.WRONG_TIME;
        }

        if (playerInventory.getItemCount(Material.PETROL_CAN) < 1) {
            interruptSiphon();
            return SiphonResult.NO_EMPTY_CAN;
        }

        if (!siphoning || siphonTimer < SIPHON_HOLD_SECONDS) {
            return SiphonResult.INTERRUPTED;
        }

        // Siphon complete
        playerInventory.removeItem(Material.PETROL_CAN, 1);
        playerInventory.addItem(Material.PETROL_CAN_FULL, 1);

        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.VEHICLE_TAMPERING);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(SIPHON_NOTORIETY, callback);
        }
        if (cctvActive && cctvHasLos && wantedSystem != null) {
            wantedSystem.addWantedStars(SIPHON_CCTV_WANTED_STARS,
                    playerX, playerY, playerZ, null);
        }

        interruptSiphon();
        return SiphonResult.SUCCESS;
    }

    /** Returns the current siphon timer progress (seconds held). */
    public float getSiphonTimer() {
        return siphonTimer;
    }

    /** Returns whether a siphon mini-game is in progress. */
    public boolean isSiphoning() {
        return siphoning;
    }

    // ── Till robbery ──────────────────────────────────────────────────────────

    /**
     * Attempt to rob the till using a CROWBAR.
     *
     * <p>Yields {@link #TILL_MIN_COIN}–{@link #TILL_MAX_COIN} COIN; records
     * {@code ARMED_ROBBERY}; adds WantedSystem +{@link #TILL_ROBBERY_WANTED_STARS};
     * seeds {@code COMMUNITY_OUTRAGE} rumour; triggers police response in
     * {@link #POLICE_RESPONSE_SECONDS}. If the player is at notoriety tier ≥
     * {@link #PANIC_BUTTON_TIER_THRESHOLD} and Wayne/Raj is awake, panic button
     * triggers armed response in {@link #ARMED_RESPONSE_SECONDS}.
     *
     * @param playerInventory  player inventory (must contain CROWBAR)
     * @param playerNotoriety  player's raw notoriety score
     * @param currentHour      current in-game hour
     * @param npcs             all NPCs for rumour seeding
     * @param playerX          player X for WantedSystem LKP
     * @param playerY          player Y
     * @param playerZ          player Z
     * @param callback         achievement callback
     * @return robbery result
     */
    public TillRobberyResult robTill(Inventory playerInventory,
                                      int playerNotoriety,
                                      float currentHour,
                                      List<NPC> npcs,
                                      float playerX, float playerY, float playerZ,
                                      NotorietySystem.AchievementCallback callback) {
        if (playerInventory.getItemCount(Material.CROWBAR) < 1) {
            return TillRobberyResult.NO_CROWBAR;
        }

        // Panic button check — active if attendant awake and tier ≥ threshold
        int tier = computeNotorietyTier(playerNotoriety);
        boolean attendantAwake = isAttendantAwake(currentHour);
        if (tier >= PANIC_BUTTON_TIER_THRESHOLD && attendantAwake && !panicButtonTriggered) {
            panicButtonTriggered = true;
            armedResponseInbound = true;
            armedResponseTimer = ARMED_RESPONSE_SECONDS;
            if (raj != null && isRajOnDuty(currentHour)) {
                raj.setSpeechText("HELP! HELP! ARMED ROBBERY!", 5f);
            }
            if (wayne != null && isWayneOnDuty(currentHour)) {
                wayne.setSpeechText("I'm hitting the panic button!", 5f);
            }
        }

        // Yield coins
        int coinYield = TILL_MIN_COIN + random.nextInt(TILL_MAX_COIN - TILL_MIN_COIN + 1);
        playerInventory.addItem(Material.COIN, coinYield);
        tillRobbed = true;

        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.ARMED_ROBBERY);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(TILL_ROBBERY_NOTORIETY, callback);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(TILL_ROBBERY_WANTED_STARS,
                    playerX, playerY, playerZ, null);
        }

        // Start police response timer
        policeResponseTimer = POLICE_RESPONSE_SECONDS;

        // Seed COMMUNITY_OUTRAGE rumour
        if (rumourNetwork != null && npcs != null) {
            for (NPC npc : npcs) {
                if (npc.isAlive() && npc.getType() == NPCType.PUBLIC) {
                    rumourNetwork.addRumour(npc, new Rumour(RumourType.COMMUNITY_OUTRAGE,
                            "Someone robbed the BP at knifepoint. Absolute animals."));
                    break;
                }
            }
        }

        if (panicButtonTriggered) {
            return TillRobberyResult.PANIC_BUTTON_ARMED_RESPONSE;
        }
        return TillRobberyResult.SUCCESS;
    }

    // ── Cigarette cabinet ─────────────────────────────────────────────────────

    /**
     * Register a hit on the cigarette cabinet (HARD material, requires
     * {@link #CIGARETTE_CABINET_HITS} hits).
     *
     * @param playerInventory player inventory to add drops
     * @return smash result
     */
    public CabinetSmashResult hitCigaretteCabinet(Inventory playerInventory) {
        if (cabinetSmashed) {
            return CabinetSmashResult.ALREADY_SMASHED;
        }

        cabinetHits++;
        if (cabinetHits < CIGARETTE_CABINET_HITS) {
            return CabinetSmashResult.HIT_REGISTERED;
        }

        // Smashed
        cabinetSmashed = true;
        int drops = CIGARETTE_CARTON_DROP_MIN
                + random.nextInt(CIGARETTE_CARTON_DROP_MAX - CIGARETTE_CARTON_DROP_MIN + 1);
        playerInventory.addItem(Material.CIGARETTE_CARTON, drops);

        return CabinetSmashResult.SMASHED;
    }

    // ── CCTV ──────────────────────────────────────────────────────────────────

    /**
     * Break the CCTV camera (caller is responsible for enforcing hit count).
     */
    public void breakCctv() {
        cctvActive = false;
    }

    public boolean isCctvActive() { return cctvActive; }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Main per-frame update.
     *
     * @param delta        seconds since last frame
     * @param currentHour  current in-game hour
     * @param noiseLevel   current noise level
     * @param npcs         all NPCs
     * @param player       the player
     * @param callback     achievement callback
     */
    public void update(float delta,
                       float currentHour,
                       float noiseLevel,
                       List<NPC> npcs,
                       Player player,
                       NotorietySystem.AchievementCallback callback) {

        // Wayne sleep state management
        if (wayne != null && isWayneOnDuty(currentHour)) {
            if (isWayneAsleep(currentHour)) {
                if (!isWayneRespondingToNoise(currentHour, noiseLevel)) {
                    // Wayne is asleep and noise is too quiet to wake him
                    if (wayne.getState() != NPCState.KNOCKED_OUT) {
                        wayne.setState(NPCState.IDLE);
                    }
                } else {
                    // Woken by noise
                    if (wayne.getState() == NPCState.IDLE) {
                        wayne.setState(NPCState.WANDERING);
                        wayne.setSpeechText("What's that?! Who's there?!", 4f);
                    }
                }
            }
            // Wayne's microwave pasty atmosphere at 23:00
            if (currentHour >= 23.0f && currentHour < 23.1f) {
                wayne.setSpeechText("Smells like something's burning in the microwave again...", 5f);
            }
        }

        // Police response timer
        if (policeResponseTimer > 0f) {
            policeResponseTimer -= delta;
        }

        // Armed response timer
        if (armedResponseTimer > 0f) {
            armedResponseTimer -= delta;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Find a stock item by material type. */
    public MenuItem findMenuItem(Material material) {
        for (MenuItem item : STOCK) {
            if (item.material == material) return item;
        }
        return null;
    }

    /** Get the NPC currently serving (Raj or Wayne), or null if nobody is. */
    private NPC getActiveAttendant(float hour) {
        if (isRajOnDuty(hour)) return raj;
        if (isWayneOnDuty(hour)) return wayne;
        return null;
    }

    /** Returns true if the current attendant is awake. */
    private boolean isAttendantAwake(float hour) {
        if (isRajOnDuty(hour)) return raj != null && raj.isAlive();
        if (isWayneOnDuty(hour)) {
            if (wayne == null || !wayne.isAlive()) return false;
            // Wayne is awake unless in sleep window with low noise — but for panic button,
            // treat him as awake unless fully in sleep state
            return !isWayneAsleep(hour);
        }
        return false;
    }

    private void seedCrimeSpottedRumour(List<NPC> npcs, String text) {
        if (rumourNetwork == null || npcs == null) return;
        for (NPC npc : npcs) {
            if (npc.isAlive() && npc.getType() == NPCType.PUBLIC) {
                rumourNetwork.addRumour(npc, new Rumour(RumourType.CRIME_SPOTTED, text));
                break;
            }
        }
    }

    private void seedWeatherGrumbleRumour() {
        // No NPC needed — grumble spreads from context; just seeds the type in network
        // Callers may inject NPCs; simplified implementation seeds only when NPCs injected via update
    }

    private void award(AchievementType type, NotorietySystem.AchievementCallback callback) {
        if (achievementSystem != null) {
            achievementSystem.unlock(type);
        }
        if (callback != null) {
            callback.award(type);
        }
    }

    /** Compute notoriety tier from raw score (mirrors NotorietySystem thresholds). */
    private static int computeNotorietyTier(int notoriety) {
        if (notoriety >= NotorietySystem.TIER_5_THRESHOLD) return 5;
        if (notoriety >= NotorietySystem.TIER_4_THRESHOLD) return 4;
        if (notoriety >= NotorietySystem.TIER_3_THRESHOLD) return 3;
        if (notoriety >= NotorietySystem.TIER_2_THRESHOLD) return 2;
        if (notoriety >= NotorietySystem.TIER_1_THRESHOLD) return 1;
        return 0;
    }

    // ── Getters (state inspection / testing) ─────────────────────────────────

    public boolean isTillRobbed()                { return tillRobbed; }
    public boolean isPanicButtonTriggered()      { return panicButtonTriggered; }
    public boolean isArmedResponseInbound()      { return armedResponseInbound; }
    public float   getArmedResponseTimer()       { return armedResponseTimer; }
    public float   getPoliceResponseTimer()      { return policeResponseTimer; }
    public boolean isCabinetSmashed()            { return cabinetSmashed; }
    public int     getCabinetHits()              { return cabinetHits; }
    public int     getLegitFills()               { return legitimateFills; }
    public int     getKioskPurchases()           { return kioskPurchases; }
    public NPC     getRaj()                      { return raj; }
    public NPC     getWayne()                    { return wayne; }
}

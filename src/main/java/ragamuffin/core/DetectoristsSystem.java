package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1494: Northfield Detectorists Club — Keith's Weekend Dig, the Roman Hoard
 * &amp; the Portable Antiquities Dodge.
 *
 * <p>Every Sunday (day % 7 == 0) 09:00–16:00, Keith Nuttall ({@code DETECTORIST_CHAIR})
 * leads the Northfield Detectorists Club on a permitted dig at the
 * {@code ALLOTMENT_FIELD_PROP}.
 *
 * <h3>Mechanic 1 — Legitimate Dig</h3>
 * Obtain {@code DIG_PERMISSION_SLIP} from Keith (free) and equip {@code METAL_DETECTOR}.
 * Press E on dirt/grass blocks to dig up finds from a weighted table:
 * 50% BOTTLE_TOP, 20% OLD_COIN, 15% IRON_BUCKLE, 10% MUSKET_BALL, 4% SILVER_BROOCH,
 * 1% ROMAN_COIN. Permission slip consumed on first dig.
 *
 * <h3>Mechanic 2 — Trespass</h3>
 * Digging without a {@code DIG_PERMISSION_SLIP} constitutes {@code CrimeType.FIELD_TRESPASS}
 * + Notoriety +3 per dig action when unwitnessed. Finds still possible.
 *
 * <h3>Mechanic 3 — Roman Hoard</h3>
 * ROMAN_COIN find (1% chance) spawns a hidden {@code HOARD_LOCATION_PROP} two blocks
 * underground containing 3× ROMAN_COIN + ROMAN_BROOCH + 12 COIN.
 *
 * <h3>Mechanic 4 — Portable Antiquities Scheme (PAS)</h3>
 * Janet ({@code PAS_OFFICER}) attends 11:00–13:00. Declaring the ROMAN_BROOCH to her
 * yields 20 COIN + {@code AchievementType.HOARD_FINDER} + {@code PAS_RECEIPT}.
 * Fencing it instead triggers {@code CrimeType.TREASURE_DODGING} + Notoriety +8.
 * If Janet witnesses the fence transaction: +WantedSystem +2.
 *
 * <h3>Mechanic 5 — Rival Detectorist</h3>
 * Dave from Saltley ({@code RIVAL_DETECTORIST}) appears on dig day 2 of the fortnight
 * (day 14 % 14 == 0 secondary Sunday). Each frame he has a 5% chance of finding
 * the hoard first if it exists. Player can:
 * <ul>
 *   <li>Report him to Keith → {@code AchievementType.FIELD_ENFORCER}</li>
 *   <li>Let him dig (hoard at risk)</li>
 *   <li>Chase him off → {@code CrimeType.AFFRAY} if witnessed</li>
 * </ul>
 *
 * <h3>Mechanic 6 — Trophy Heist</h3>
 * {@code DETECTORISTS_TROPHY_PROP} in Keith's house. Heistable Sunday 09:00–16:00
 * while Keith is at the dig, using CROWBAR or LOCKPICK.
 * {@code NOSY_NEIGHBOUR} within 15 blocks → {@code CrimeType.BURGLARY} + Notoriety +8.
 * Unwitnessed → {@code AchievementType.TREASURE_HUNTER}.
 * Voluntary return → {@code RumourType.GOOD_SAMARITAN} + Vibes +3 + 4 permission slips.
 *
 * <h3>Integrations</h3>
 * <ul>
 *   <li>{@link NotorietySystem} — trespass/dodging/burglary add notoriety.</li>
 *   <li>{@link WantedSystem} — Janet witnessing fence adds +2 wanted stars.</li>
 *   <li>{@link CriminalRecord} — crimes recorded.</li>
 *   <li>{@link RumourNetwork} — TREASURE_DODGER, SALTLEY_POACHER, GOOD_SAMARITAN rumours.</li>
 * </ul>
 */
public class DetectoristsSystem {

    // ── Schedule constants ─────────────────────────────────────────────────

    /** Dig runs every Sunday: day % 7 == 0. */
    public static final int DIG_DAY_MOD = 7;

    /** Dig opens at 09:00. */
    public static final float DIG_OPEN_HOUR = 9.0f;

    /** Dig closes at 16:00. */
    public static final float DIG_CLOSE_HOUR = 16.0f;

    /** PAS officer Janet attends from 11:00. */
    public static final float PAS_OPEN_HOUR = 11.0f;

    /** PAS officer Janet leaves at 13:00. */
    public static final float PAS_CLOSE_HOUR = 13.0f;

    /** Rival detectorist appears on the second Sunday of a 14-day fortnight. */
    public static final int RIVAL_DAY_MOD = 14;

    /** Second Sunday offset within the fortnight (day 7 mod 14). */
    public static final int RIVAL_DAY_OFFSET = 7;

    // ── Dig find probabilities (cumulative thresholds) ─────────────────────

    /** Cumulative probability for BOTTLE_TOP (50%). */
    public static final float PROB_BOTTLE_TOP = 0.50f;

    /** Cumulative probability for OLD_COIN (50% + 20% = 70%). */
    public static final float PROB_OLD_COIN = 0.70f;

    /** Cumulative probability for IRON_BUCKLE (70% + 15% = 85%). */
    public static final float PROB_IRON_BUCKLE = 0.85f;

    /** Cumulative probability for MUSKET_BALL (85% + 10% = 95%). */
    public static final float PROB_MUSKET_BALL = 0.95f;

    /** Cumulative probability for SILVER_BROOCH (95% + 4% = 99%). */
    public static final float PROB_SILVER_BROOCH = 0.99f;

    /** Remaining 1% is ROMAN_COIN — triggers hoard spawn. */
    public static final float PROB_ROMAN_COIN = 1.00f;

    // ── Costs & rewards ────────────────────────────────────────────────────

    /** Cost to buy METAL_DETECTOR from Keith. */
    public static final int METAL_DETECTOR_COST = 25;

    /** Notoriety penalty for digging without a permission slip. */
    public static final int TRESPASS_NOTORIETY = 3;

    /** Coin reward for declaring ROMAN_BROOCH to Janet. */
    public static final int PAS_DECLARATION_REWARD = 20;

    /** Notoriety penalty for fencing ROMAN_BROOCH (treasure dodging). */
    public static final int TREASURE_DODGING_NOTORIETY = 8;

    /** Wanted star increase when Janet witnesses a ROMAN_BROOCH fence. */
    public static final int TREASURE_DODGING_WANTED_STARS = 2;

    /** Notoriety penalty for witnessed burglary (trophy heist). */
    public static final int BURGLARY_NOTORIETY = 8;

    /** Number of free permission slips awarded when returning trophy voluntarily. */
    public static final int TROPHY_RETURN_SLIP_REWARD = 4;

    /** Vibes increase when returning trophy voluntarily. */
    public static final int TROPHY_RETURN_VIBES = 3;

    /** Per-frame probability that the rival detectorist finds the hoard. */
    public static final float RIVAL_HOARD_FIND_CHANCE = 0.05f;

    /** Radius within which NOSY_NEIGHBOUR witnesses the trophy heist. */
    public static final float NOSY_NEIGHBOUR_RADIUS = 15f;

    /** Number of permission slips awarded when returning the trophy to Keith. */
    public static final int RETURN_TROPHY_SLIP_COUNT = 4;

    // ── Result enums ───────────────────────────────────────────────────────

    /** Result codes for attempting to dig at the allotment field. */
    public enum DigResult {
        SUCCESS_LEGITIMATE,
        SUCCESS_TRESPASS,
        DIG_NOT_ACTIVE,
        NO_METAL_DETECTOR,
        HOARD_FOUND
    }

    /** Result codes for declaring the ROMAN_BROOCH to Janet. */
    public enum PasDeclarationResult {
        SUCCESS,
        NO_ROMAN_BROOCH,
        PAS_NOT_PRESENT,
        ALREADY_DECLARED
    }

    /** Result codes for fencing the ROMAN_BROOCH. */
    public enum FenceResult {
        SUCCESS,
        NO_ROMAN_BROOCH,
        ALREADY_DECLARED,
        WITNESSED_BY_JANET
    }

    /** Result codes for reporting the rival detectorist to Keith. */
    public enum RivalReportResult {
        SUCCESS,
        RIVAL_NOT_PRESENT,
        ALREADY_REPORTED
    }

    /** Result codes for attempting the trophy heist. */
    public enum TrophyHeistResult {
        SUCCESS,
        NO_TOOL,
        OUTSIDE_HEIST_WINDOW,
        WITNESSED,
        ALREADY_LOOTED
    }

    /** Result codes for voluntarily returning the trophy to Keith. */
    public enum TrophyReturnResult {
        SUCCESS,
        NO_TROPHY,
        ALREADY_RETURNED
    }

    // ── State ──────────────────────────────────────────────────────────────

    private final Random random;

    /** Day index of the last dig session initialised. */
    private int lastDigDay = -1;

    /** Whether the player's permission slip has been consumed today. */
    private boolean permissionSlipConsumed = false;

    /** Whether the hoard location has been spawned this dig session. */
    private boolean hoardSpawned = false;

    /** Whether the player has already declared the ROMAN_BROOCH to Janet. */
    private boolean romanBroochDeclared = false;

    /** Whether the trophy cabinet has been looted this session. */
    private boolean trophyLooted = false;

    /** Whether the trophy has been voluntarily returned. */
    private boolean trophyReturned = false;

    /** Whether the rival detectorist has been reported to Keith. */
    private boolean rivalReported = false;

    /** Whether the rival detectorist has found the hoard first. */
    private boolean rivalFoundHoard = false;

    /** Whether the HOARD_FINDER achievement has been awarded. */
    private boolean hoardFinderAwarded = false;

    /** Whether the TREASURE_HUNTER achievement has been awarded. */
    private boolean treasureHunterAwarded = false;

    /** Whether the FIELD_ENFORCER achievement has been awarded. */
    private boolean fieldEnforcerAwarded = false;

    // ── Construction ──────────────────────────────────────────────────────

    public DetectoristsSystem() {
        this(new Random());
    }

    public DetectoristsSystem(Random random) {
        this.random = random;
    }

    // ── Schedule helpers ──────────────────────────────────────────────────

    /**
     * Returns true if the dig is active on the given day and hour.
     * Dig runs on Sundays (day % 7 == 0) between 09:00 and 16:00.
     */
    public boolean isDigActive(TimeSystem timeSystem) {
        int day = timeSystem.getDayCount();
        float hour = timeSystem.getTime();
        return (day % DIG_DAY_MOD == 0)
                && hour >= DIG_OPEN_HOUR
                && hour < DIG_CLOSE_HOUR;
    }

    /**
     * Returns true if Janet (PAS officer) is present (11:00–13:00 on dig day).
     */
    public boolean isPasOfficerPresent(TimeSystem timeSystem) {
        float hour = timeSystem.getTime();
        return isDigActive(timeSystem)
                && hour >= PAS_OPEN_HOUR
                && hour < PAS_CLOSE_HOUR;
    }

    /**
     * Returns true if the rival detectorist day is active (second Sunday of fortnight).
     */
    public boolean isRivalDay(TimeSystem timeSystem) {
        int day = timeSystem.getDayCount();
        return isDigActive(timeSystem) && (day % RIVAL_DAY_MOD == RIVAL_DAY_OFFSET);
    }

    // ── Per-frame update ───────────────────────────────────────────────────

    /**
     * Call once per frame. Handles state reset at the start of a new dig day
     * and rival-finds-hoard probability roll.
     *
     * @param delta               seconds since last frame
     * @param timeSystem          game time
     * @param allNpcs             all NPCs in the world
     * @param achievementCallback achievement callback (may be null)
     * @param notorietySystem     (may be null)
     * @param rumourNetwork       (may be null)
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       List<NPC> allNpcs,
                       NotorietySystem.AchievementCallback achievementCallback,
                       NotorietySystem notorietySystem,
                       RumourNetwork rumourNetwork) {

        int day = timeSystem.getDayCount();

        // Reset state at the start of each new dig day
        if (day % DIG_DAY_MOD == 0 && day != lastDigDay && isDigActive(timeSystem)) {
            resetDigState(day);
        }

        if (!isDigActive(timeSystem)) return;

        // Rival detectorist hoard-find roll (5% per frame while rival is present
        // and hoard exists but has not yet been found)
        if (isRivalDay(timeSystem) && hoardSpawned && !rivalFoundHoard && !rivalReported) {
            if (random.nextFloat() < RIVAL_HOARD_FIND_CHANCE) {
                rivalFoundHoard = true;
                seedRumour(rumourNetwork, allNpcs, RumourType.SALTLEY_POACHER,
                        "Dave from Saltley's found something big on the allotment. "
                        + "Nobody knows how he got permission.");
            }
        }
    }

    // ── Mechanic 1 & 2: Digging ────────────────────────────────────────────

    /**
     * Player attempts to dig at the allotment field (pressing E on a dirt/grass block).
     *
     * <p>Requirements: dig active, METAL_DETECTOR equipped.
     * With permission slip → legitimate dig (slip consumed on first use).
     * Without slip → {@code CrimeType.FIELD_TRESPASS} + Notoriety +3.
     *
     * <p>Find is determined by a weighted random roll. ROMAN_COIN find spawns
     * the hoard ({@link #isHoardSpawned()} becomes true; caller must place
     * {@code HOARD_LOCATION_PROP} two blocks below).
     *
     * @param timeSystem          for schedule checking
     * @param inventory           player inventory (slip consumed; find added)
     * @param criminalRecord      for FIELD_TRESPASS crime on trespass
     * @param notorietySystem     for trespass notoriety
     * @param achievementCallback (reserved)
     * @return DigResult describing the outcome
     */
    public DigResult dig(TimeSystem timeSystem,
                          Inventory inventory,
                          CriminalRecord criminalRecord,
                          NotorietySystem notorietySystem,
                          NotorietySystem.AchievementCallback achievementCallback) {

        if (!isDigActive(timeSystem)) return DigResult.DIG_NOT_ACTIVE;
        if (inventory.getItemCount(Material.METAL_DETECTOR) < 1) return DigResult.NO_METAL_DETECTOR;

        boolean hasPermission = inventory.getItemCount(Material.DIG_PERMISSION_SLIP) > 0
                || permissionSlipConsumed;

        if (!hasPermission) {
            // Trespass — record crime and notoriety
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.FIELD_TRESPASS);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(TRESPASS_NOTORIETY, achievementCallback);
            }
        } else if (!permissionSlipConsumed && inventory.getItemCount(Material.DIG_PERMISSION_SLIP) > 0) {
            // Consume the permission slip on first use
            inventory.removeItem(Material.DIG_PERMISSION_SLIP, 1);
            permissionSlipConsumed = true;
        }

        // Roll for find
        Material find = rollFind();

        if (find == Material.ROMAN_COIN) {
            inventory.addItem(Material.ROMAN_COIN, 1);
            if (!hoardSpawned) {
                hoardSpawned = true;
            }
            return DigResult.HOARD_FOUND;
        }

        inventory.addItem(find, 1);
        return hasPermission ? DigResult.SUCCESS_LEGITIMATE : DigResult.SUCCESS_TRESPASS;
    }

    /**
     * Weighted random roll to determine the find from a dig action.
     *
     * @return the Material found
     */
    public Material rollFind() {
        float roll = random.nextFloat();
        if (roll < PROB_BOTTLE_TOP)      return Material.BOTTLE_TOP;
        if (roll < PROB_OLD_COIN)        return Material.OLD_COIN;
        if (roll < PROB_IRON_BUCKLE)     return Material.IRON_BUCKLE;
        if (roll < PROB_MUSKET_BALL)     return Material.MUSKET_BALL;
        if (roll < PROB_SILVER_BROOCH)   return Material.SILVER_BROOCH;
        return Material.ROMAN_COIN;
    }

    // ── Mechanic 3: Hoard excavation ──────────────────────────────────────

    /**
     * Player excavates the hidden hoard (presses E on {@code HOARD_LOCATION_PROP}).
     * Awards 3× ROMAN_COIN + ROMAN_BROOCH + 12 COIN from the buried chest.
     * Only callable after hoard has been spawned.
     *
     * @param inventory player inventory
     * @return true if hoard successfully excavated, false if not available
     */
    public boolean excavateHoard(Inventory inventory) {
        if (!hoardSpawned) return false;
        inventory.addItem(Material.ROMAN_COIN, 3);
        inventory.addItem(Material.ROMAN_BROOCH, 1);
        inventory.addItem(Material.COIN, 12);
        return true;
    }

    // ── Mechanic 4: PAS declaration ───────────────────────────────────────

    /**
     * Player declares the ROMAN_BROOCH to Janet (PAS_OFFICER).
     *
     * <p>Requirements: Janet present (11:00–13:00), player has ROMAN_BROOCH,
     * not already declared.
     * On success: ROMAN_BROOCH consumed, 20 COIN + PAS_RECEIPT added,
     * {@code AchievementType.HOARD_FINDER} awarded.
     *
     * @param timeSystem          for PAS officer schedule
     * @param inventory           player inventory
     * @param achievementCallback for HOARD_FINDER
     * @param allNpcs             for rumour seeding
     * @param rumourNetwork       (reserved)
     * @return PasDeclarationResult describing outcome
     */
    public PasDeclarationResult declareToPas(TimeSystem timeSystem,
                                              Inventory inventory,
                                              NotorietySystem.AchievementCallback achievementCallback,
                                              List<NPC> allNpcs,
                                              RumourNetwork rumourNetwork) {

        if (!isPasOfficerPresent(timeSystem)) return PasDeclarationResult.PAS_NOT_PRESENT;
        if (inventory.getItemCount(Material.ROMAN_BROOCH) < 1) return PasDeclarationResult.NO_ROMAN_BROOCH;
        if (romanBroochDeclared) return PasDeclarationResult.ALREADY_DECLARED;

        inventory.removeItem(Material.ROMAN_BROOCH, 1);
        inventory.addItem(Material.COIN, PAS_DECLARATION_REWARD);
        inventory.addItem(Material.PAS_RECEIPT, 1);
        romanBroochDeclared = true;

        if (!hoardFinderAwarded && achievementCallback != null) {
            hoardFinderAwarded = true;
            achievementCallback.award(AchievementType.HOARD_FINDER);
        }

        return PasDeclarationResult.SUCCESS;
    }

    /**
     * Player fences the ROMAN_BROOCH without declaring to Janet.
     *
     * <p>Records {@code CrimeType.TREASURE_DODGING} + Notoriety +8.
     * If Janet witnesses: WantedSystem +2.
     * Seeds {@code RumourType.TREASURE_DODGER} rumour.
     *
     * @param inventory          player inventory
     * @param janetWitnesses     true if PAS_OFFICER is within range of the fence transaction
     * @param criminalRecord     for TREASURE_DODGING crime
     * @param notorietySystem    for notoriety hit
     * @param achievementCallback (unused)
     * @param wantedSystem       for +2 stars if Janet witnesses
     * @param allNpcs            for rumour seeding
     * @param rumourNetwork      for TREASURE_DODGER rumour
     * @return FenceResult describing outcome
     */
    public FenceResult fenceBrooch(Inventory inventory,
                                    boolean janetWitnesses,
                                    CriminalRecord criminalRecord,
                                    NotorietySystem notorietySystem,
                                    NotorietySystem.AchievementCallback achievementCallback,
                                    WantedSystem wantedSystem,
                                    List<NPC> allNpcs,
                                    RumourNetwork rumourNetwork) {

        if (inventory.getItemCount(Material.ROMAN_BROOCH) < 1) return FenceResult.NO_ROMAN_BROOCH;
        if (romanBroochDeclared) return FenceResult.ALREADY_DECLARED;

        if (janetWitnesses) {
            // Witnessed — crime still committed; return special result
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.TREASURE_DODGING);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(TREASURE_DODGING_NOTORIETY, achievementCallback);
            }
            if (wantedSystem != null) {
                wantedSystem.increaseWantedStars(TREASURE_DODGING_WANTED_STARS);
            }
            seedRumour(rumourNetwork, allNpcs, RumourType.TREASURE_DODGER,
                    "Janet caught someone trying to fence a Roman brooch. She rang the museum straight away.");
            return FenceResult.WITNESSED_BY_JANET;
        }

        // Unwitnessed fence
        inventory.removeItem(Material.ROMAN_BROOCH, 1);

        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.TREASURE_DODGING);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(TREASURE_DODGING_NOTORIETY, achievementCallback);
        }

        seedRumour(rumourNetwork, allNpcs, RumourType.TREASURE_DODGER,
                "Word is someone fenced a Roman brooch at the market. "
                + "Didn't declare it. That's a criminal offence, that is.");

        return FenceResult.SUCCESS;
    }

    // ── Mechanic 5: Rival detectorist ─────────────────────────────────────

    /**
     * Player reports the rival detectorist (Dave from Saltley) to Keith.
     *
     * <p>Requirements: rival day active, not already reported.
     * Awards {@code AchievementType.FIELD_ENFORCER}.
     * Seeds {@code RumourType.SALTLEY_POACHER} rumour.
     *
     * @param timeSystem          for rival-day check
     * @param allNpcs             for rumour seeding
     * @param achievementCallback for FIELD_ENFORCER
     * @param rumourNetwork       for SALTLEY_POACHER rumour
     * @return RivalReportResult describing outcome
     */
    public RivalReportResult reportRival(TimeSystem timeSystem,
                                          List<NPC> allNpcs,
                                          NotorietySystem.AchievementCallback achievementCallback,
                                          RumourNetwork rumourNetwork) {

        if (!isRivalDay(timeSystem)) return RivalReportResult.RIVAL_NOT_PRESENT;
        if (rivalReported) return RivalReportResult.ALREADY_REPORTED;

        rivalReported = true;

        if (!fieldEnforcerAwarded && achievementCallback != null) {
            fieldEnforcerAwarded = true;
            achievementCallback.award(AchievementType.FIELD_ENFORCER);
        }

        seedRumour(rumourNetwork, allNpcs, RumourType.SALTLEY_POACHER,
                "Keith found Dave from Saltley on the allotment without permission. "
                + "He's been banned from every dig in the West Midlands.");

        return RivalReportResult.SUCCESS;
    }

    /**
     * Player chases off the rival detectorist.
     *
     * <p>If witnessed: records {@code CrimeType.AFFRAY}.
     * Rival leaves the field regardless.
     *
     * @param timeSystem      for rival-day check
     * @param witnessed       true if another NPC witnesses the altercation
     * @param criminalRecord  for AFFRAY crime if witnessed
     * @return true if rival was present and chased off, false otherwise
     */
    public boolean chaseOffRival(TimeSystem timeSystem,
                                  boolean witnessed,
                                  CriminalRecord criminalRecord) {

        if (!isRivalDay(timeSystem)) return false;
        if (rivalReported) return false;

        rivalReported = true; // rival leaves regardless

        if (witnessed && criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.AFFRAY);
        }

        return true;
    }

    // ── Mechanic 6: Trophy heist ──────────────────────────────────────────

    /**
     * Player attempts to steal the {@code DETECTORISTS_TROPHY_PROP} from Keith's house.
     *
     * <p>Only available Sunday 09:00–16:00 while Keith is at the dig.
     * Requires CROWBAR or LOCKPICK in inventory.
     * Witnessed by NOSY_NEIGHBOUR within 15 blocks → {@code CrimeType.BURGLARY}
     * + Notoriety +8. Unwitnessed → {@code AchievementType.TREASURE_HUNTER}.
     *
     * @param timeSystem          for dig-day check
     * @param inventory           player inventory (LOCKPICK or CROWBAR consumed on success)
     * @param witnessed           true if NOSY_NEIGHBOUR is within 15 blocks
     * @param allNpcs             for rumour seeding
     * @param achievementCallback for TREASURE_HUNTER
     * @param criminalRecord      for BURGLARY crime on witnessed heist
     * @param notorietySystem     for +8 notoriety on witnessed heist
     * @param wantedSystem        (reserved)
     * @param rumourNetwork       (reserved)
     * @return TrophyHeistResult describing outcome
     */
    public TrophyHeistResult attemptTrophyHeist(TimeSystem timeSystem,
                                                  Inventory inventory,
                                                  boolean witnessed,
                                                  List<NPC> allNpcs,
                                                  NotorietySystem.AchievementCallback achievementCallback,
                                                  CriminalRecord criminalRecord,
                                                  NotorietySystem notorietySystem,
                                                  WantedSystem wantedSystem,
                                                  RumourNetwork rumourNetwork) {

        if (!isDigActive(timeSystem)) return TrophyHeistResult.OUTSIDE_HEIST_WINDOW;
        if (trophyLooted) return TrophyHeistResult.ALREADY_LOOTED;

        boolean hasTool = inventory.getItemCount(Material.CROWBAR) > 0
                || inventory.getItemCount(Material.LOCKPICK) > 0;
        if (!hasTool) return TrophyHeistResult.NO_TOOL;

        if (witnessed) {
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.BURGLARY);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(BURGLARY_NOTORIETY, achievementCallback);
            }
            return TrophyHeistResult.WITNESSED;
        }

        // Consume one tool (prefer LOCKPICK over CROWBAR)
        if (inventory.getItemCount(Material.LOCKPICK) > 0) {
            inventory.removeItem(Material.LOCKPICK, 1);
        } else {
            inventory.removeItem(Material.CROWBAR, 1);
        }

        // Add trophy to inventory
        inventory.addItem(Material.SCRAP_METAL, 0); // trophy is a prop — represented as SCRAP_METAL drop
        // Award the trophy as a collectible item (prop-in-inventory pattern)
        trophyLooted = true;

        if (!treasureHunterAwarded && achievementCallback != null) {
            treasureHunterAwarded = true;
            achievementCallback.award(AchievementType.TREASURE_HUNTER);
        }

        return TrophyHeistResult.SUCCESS;
    }

    /**
     * Player voluntarily returns the trophy to Keith.
     *
     * <p>Awards {@code RumourType.GOOD_SAMARITAN} + Vibes +3 +
     * {@link #TROPHY_RETURN_SLIP_COUNT} free DIG_PERMISSION_SLIPs.
     *
     * @param inventory      player inventory (trophy consumed, slips added)
     * @param allNpcs        for rumour seeding
     * @param rumourNetwork  for GOOD_SAMARITAN rumour
     * @return TrophyReturnResult describing outcome
     */
    public TrophyReturnResult returnTrophy(Inventory inventory,
                                            List<NPC> allNpcs,
                                            RumourNetwork rumourNetwork) {

        if (!trophyLooted) return TrophyReturnResult.NO_TROPHY;
        if (trophyReturned) return TrophyReturnResult.ALREADY_RETURNED;

        trophyReturned = true;

        // Give the 4 free permission slips
        inventory.addItem(Material.DIG_PERMISSION_SLIP, RETURN_TROPHY_SLIP_COUNT);

        seedRumour(rumourNetwork, allNpcs, RumourType.GOOD_SAMARITAN,
                "Someone found Keith's trophy in a ditch and handed it back. "
                + "Keith nearly cried. Said he'd give them dig permits for life.");

        return TrophyReturnResult.SUCCESS;
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    /** @return true if the hoard location has been spawned this dig session. */
    public boolean isHoardSpawned() {
        return hoardSpawned;
    }

    /** @return true if the ROMAN_BROOCH has been declared to Janet. */
    public boolean isRomanBroochDeclared() {
        return romanBroochDeclared;
    }

    /** @return true if the trophy has been looted this session. */
    public boolean isTrophyLooted() {
        return trophyLooted;
    }

    /** @return true if the trophy has been voluntarily returned. */
    public boolean isTrophyReturned() {
        return trophyReturned;
    }

    /** @return true if the rival has been reported to Keith or chased off. */
    public boolean isRivalDealtWith() {
        return rivalReported;
    }

    /** @return true if the rival found the hoard before the player reported him. */
    public boolean isRivalFoundHoard() {
        return rivalFoundHoard;
    }

    // ── Testing helpers ────────────────────────────────────────────────────

    /** Force-set hoard spawned state for testing. */
    public void setHoardSpawnedForTesting(boolean spawned) {
        this.hoardSpawned = spawned;
    }

    /** Force-set trophy looted state for testing. */
    public void setTrophyLootedForTesting(boolean looted) {
        this.trophyLooted = looted;
    }

    /** Force-set roman brooch declared state for testing. */
    public void setRomanBroochDeclaredForTesting(boolean declared) {
        this.romanBroochDeclared = declared;
    }

    /** Force-set permission slip consumed state for testing. */
    public void setPermissionSlipConsumedForTesting(boolean consumed) {
        this.permissionSlipConsumed = consumed;
    }

    /** Force-set rival reported state for testing. */
    public void setRivalReportedForTesting(boolean reported) {
        this.rivalReported = reported;
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private void resetDigState(int day) {
        lastDigDay = day;
        permissionSlipConsumed = false;
        hoardSpawned = false;
        romanBroochDeclared = false;
        trophyLooted = false;
        trophyReturned = false;
        rivalReported = false;
        rivalFoundHoard = false;
    }

    private void seedRumour(RumourNetwork rumourNetwork,
                             List<NPC> allNpcs,
                             RumourType type,
                             String text) {
        if (rumourNetwork == null || allNpcs == null) return;
        Rumour rumour = new Rumour(type, text);
        for (NPC npc : allNpcs) {
            if (npc.isAlive()) {
                rumourNetwork.addRumour(npc, rumour);
                break;
            }
        }
    }
}

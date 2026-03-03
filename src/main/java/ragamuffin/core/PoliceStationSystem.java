package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Issue #1337: Northfield Police Station — The Nick, Desk Sergeant Geoff,
 * Voluntary Surrender &amp; the Evidence Locker Heist.
 *
 * <h3>Overview</h3>
 * <p>B Division nick on the Northfield high street. Staffed by DESK_SERGEANT Geoff
 * (08:00–20:00) and CUSTODY_SERGEANT (20:00–08:00) at the ENQUIRY_COUNTER_PROP.</p>
 *
 * <h3>Mechanic 1 — Enquiry Counter (Desk Sergeant Geoff)</h3>
 * <ul>
 *   <li>Geoff is bribeable: 25 COIN, Notoriety ≤ 400 → reduces WantedSystem −1 star,
 *       seeds {@link RumourType#POLICE_CORRUPTION}, records
 *       {@link CriminalRecord.CrimeType#BRIBERY_OF_OFFICER}, awards
 *       {@link AchievementType#BENT_COPPER} on first bribe.</li>
 *   <li>Geoff can be intimidated via INTIMIDATION (StreetSkillSystem BRAWLING Expert+):
 *       same effect as bribe but free; 40% chance Geoff calls backup anyway.</li>
 *   <li>Night Custody Officer bribeable at 40 COIN (Notoriety ≤ 300 only).</li>
 * </ul>
 *
 * <h3>Mechanic 2 — Voluntary Surrender</h3>
 * <ul>
 *   <li>Player presses E on ENQUIRY_COUNTER_PROP with WantedSystem stars ≥ 1.</li>
 *   <li>Reduces next MagistratesCourtSystem sentence tier by one step.</li>
 *   <li>Player is held for {@link #VOLUNTARY_CUSTODY_HOURS} in-game hours.</li>
 *   <li>Notoriety −{@value #VOLUNTARY_SURRENDER_NOTORIETY_REDUCTION}.</li>
 *   <li>Seeds {@link RumourType#TURNED_YOURSELF_IN} and awards
 *       {@link AchievementType#CAME_IN_QUIETLY}.</li>
 * </ul>
 *
 * <h3>Mechanic 3 — Evidence Locker Heist</h3>
 * Three routes into the EVIDENCE_LOCKER_PROP:
 * <ol>
 *   <li><b>Key Card</b> — use POLICE_KEY_CARD on CUSTODY_DOOR_PROP; no alarm,
 *       max {@link #EVIDENCE_LOCKER_MAX_CARRY} items recoverable.</li>
 *   <li><b>Back Window</b> — use ROPE_AND_HOOK on BACK_WINDOW_PROP then enter;
 *       silent entry, same carry limit.</li>
 *   <li><b>Fire Alarm Distraction</b> — press E on FIRE_ALARM_PROP; all NPCs
 *       evacuate for {@link #FIRE_ALARM_EVACUATION_SECONDS}s; enter locker freely.</li>
 * </ol>
 * Success: seeds {@link RumourType#STATION_BREAK_IN}, awards
 * {@link AchievementType#EVIDENCE_GONE} (any route) or
 * {@link AchievementType#INSIDE_JOB} (fire alarm route).
 * Caught: records {@link CriminalRecord.CrimeType#EVIDENCE_TAMPERING},
 * WantedSystem +2 stars, station-wide hostile alert.
 *
 * <h3>Mechanic 4 — Vehicle Impound Recovery</h3>
 * <ul>
 *   <li>During 08:00–18:00: pay 20 COIN + present DRIVING_LICENCE at enquiry counter
 *       → vehicle returned, awards {@link AchievementType#GOT_ME_MOTOR_BACK}.</li>
 *   <li>At night (20:00–08:00): break POLICE_GARAGE_PROP with CROWBAR (8 hits)
 *       → vehicle returned, records
 *       {@link CriminalRecord.CrimeType#VEHICLE_RECOVERY_OFFENCE},
 *       awards {@link AchievementType#GOT_ME_MOTOR_BACK}.</li>
 * </ul>
 *
 * <h3>Mechanic 5 — Gang Safe-Zone</h3>
 * Non-POLICE NPCs in {@link NPCState#CHASING_PLAYER} halt at the station door
 * (within {@link #GANG_SAFE_ZONE_RADIUS} blocks of the station entrance).
 *
 * <h3>Disguise</h3>
 * A DisguiseSystem score ≥ 3 bypasses Geoff's recognition check at the enquiry
 * counter. Awards {@link AchievementType#HIDING_IN_PLAIN_SIGHT} on successful exit.
 */
public class PoliceStationSystem {

    // ── Staffing hours ─────────────────────────────────────────────────────────

    /** Hour Geoff (DESK_SERGEANT) arrives at the enquiry counter. */
    public static final float DESK_SERGEANT_START_HOUR = 8.0f;

    /** Hour Geoff leaves and CUSTODY_SERGEANT takes over. */
    public static final float DESK_SERGEANT_END_HOUR = 20.0f;

    // ── Bribery constants ─────────────────────────────────────────────────────

    /** COIN cost to bribe Geoff (DESK_SERGEANT). */
    public static final int BRIBE_DESK_SERGEANT_COST = 25;

    /** COIN cost to bribe the CUSTODY_SERGEANT. */
    public static final int BRIBE_CUSTODY_SERGEANT_COST = 40;

    /** Maximum Notoriety that allows bribing the DESK_SERGEANT. */
    public static final int BRIBE_DESK_MAX_NOTORIETY = 400;

    /** Maximum Notoriety that allows bribing the CUSTODY_SERGEANT. */
    public static final int BRIBE_CUSTODY_MAX_NOTORIETY = 300;

    /** Notoriety gained when a bribe is recorded. */
    public static final int BRIBE_NOTORIETY_PENALTY = 10;

    /** Probability (0–1) that Geoff calls backup even after a successful intimidation. */
    public static final float INTIMIDATION_BACKUP_CHANCE = 0.40f;

    // ── Voluntary surrender constants ─────────────────────────────────────────

    /** In-game hours the player is held after voluntary surrender. */
    public static final float VOLUNTARY_CUSTODY_HOURS = 2.0f;

    /** Notoriety reduction for voluntary surrender. */
    public static final int VOLUNTARY_SURRENDER_NOTORIETY_REDUCTION = 10;

    // ── Evidence locker constants ─────────────────────────────────────────────

    /** Minimum number of confiscated items in the evidence locker. */
    public static final int EVIDENCE_LOCKER_MIN_ITEMS = 3;

    /** Maximum number of confiscated items in the evidence locker. */
    public static final int EVIDENCE_LOCKER_MAX_ITEMS = 6;

    /** Maximum items the player can carry out of the evidence locker per raid. */
    public static final int EVIDENCE_LOCKER_MAX_CARRY = 4;

    /** Notoriety gain per item taken from the evidence locker if caught. */
    public static final int EVIDENCE_TAMPERING_NOTORIETY_PER_ITEM = 20;

    /** WantedSystem stars added if the player is caught in the evidence room. */
    public static final int EVIDENCE_CAUGHT_WANTED_STARS = 2;

    // ── Fire alarm constants ──────────────────────────────────────────────────

    /** Seconds the station is evacuated after the fire alarm is triggered. */
    public static final float FIRE_ALARM_EVACUATION_SECONDS = 90.0f;

    // ── Vehicle impound constants ─────────────────────────────────────────────

    /** COIN cost for official vehicle recovery at the enquiry counter. */
    public static final int VEHICLE_RECOVERY_COIN_COST = 20;

    /** Hour impound recovery service opens. */
    public static final float IMPOUND_OPEN_HOUR = 8.0f;

    /** Hour impound recovery service closes. */
    public static final float IMPOUND_CLOSE_HOUR = 18.0f;

    // ── Gang safe-zone constant ───────────────────────────────────────────────

    /** Distance from the station entrance within which chasing non-police NPCs stop. */
    public static final float GANG_SAFE_ZONE_RADIUS = 8.0f;

    // ── Station alert constants ───────────────────────────────────────────────

    /** Seconds the station-wide hostile alert persists after a break-in is detected. */
    public static final float STATION_ALERT_SECONDS = 120.0f;

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Outcomes for a bribe attempt at the enquiry counter. */
    public enum BribeResult {
        /** Bribe accepted — WantedSystem −1 star, BRIBERY_OF_OFFICER recorded. */
        SUCCESS,
        /** Player does not have enough COIN. */
        INSUFFICIENT_FUNDS,
        /** Notoriety too high for this NPC to accept a bribe. */
        NOTORIETY_TOO_HIGH,
        /** DESK_SERGEANT is not currently on duty (wrong time of day). */
        NOT_ON_DUTY,
        /** Player has already bribed this NPC in the current game session. */
        ALREADY_BRIBED
    }

    /** Outcomes for a voluntary surrender attempt. */
    public enum SurrenderResult {
        /** Surrender accepted — player processed and sentence tier reduced. */
        ACCEPTED,
        /** No wanted level — there is nothing to surrender for. */
        NO_WANTED_LEVEL,
        /** DESK_SERGEANT is not on duty (must surrender to Geoff, not night custody). */
        DESK_SERGEANT_NOT_ON_DUTY
    }

    /** Routes for accessing the evidence locker. */
    public enum EvidenceLockerRoute {
        /** Used POLICE_KEY_CARD on the CUSTODY_DOOR_PROP. */
        KEY_CARD,
        /** Used ROPE_AND_HOOK on the BACK_WINDOW_PROP. */
        BACK_WINDOW,
        /** Triggered FIRE_ALARM_PROP to evacuate the station. */
        FIRE_ALARM
    }

    /** Outcome of an evidence locker raid attempt. */
    public enum EvidenceLockerResult {
        /** Successfully raided — items added to inventory. */
        SUCCESS,
        /** Player was caught — crime recorded, wanted stars added. */
        CAUGHT,
        /** Locker is empty (already raided this session). */
        EMPTY,
        /** Fire alarm not triggered — route unavailable. */
        FIRE_ALARM_NOT_TRIGGERED
    }

    /** Outcome of a vehicle impound recovery attempt. */
    public enum VehicleRecoveryResult {
        /** Vehicle successfully recovered via official payment. */
        RECOVERED_OFFICIAL,
        /** Vehicle successfully recovered via night-time break-in. */
        RECOVERED_UNOFFICIAL,
        /** No impounded vehicle registered to the player. */
        NO_VEHICLE_IMPOUNDED,
        /** Counter is closed (outside 08:00–18:00) for official route. */
        COUNTER_CLOSED,
        /** Player lacks DRIVING_LICENCE for official route. */
        NO_LICENCE,
        /** Player lacks sufficient COIN for official route. */
        INSUFFICIENT_FUNDS,
        /** Break-in failed — station alerted. */
        CAUGHT
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** True when the fire alarm has been triggered this session. */
    private boolean fireAlarmTriggered;

    /** Remaining evacuation time in seconds after fire alarm; 0 when inactive. */
    private float evacuationTimer;

    /** True if the evidence locker has been successfully raided this session. */
    private boolean evidenceLockerRaided;

    /** Items currently held in the evidence locker (populated at game start). */
    private final List<Material> lockerContents;

    /** True if the player has a vehicle in the impound. */
    private boolean vehicleImpounded;

    /** True while the station-wide hostile alert is active. */
    private boolean stationAlertActive;

    /** Remaining seconds on the station-wide hostile alert. */
    private float stationAlertTimer;

    /** True once the player has bribed the DESK_SERGEANT this session. */
    private boolean deskSergeantBribed;

    /** True once the player has bribed the CUSTODY_SERGEANT this session. */
    private boolean custodySergeantBribed;

    /** True if the player has performed a voluntary surrender this session. */
    private boolean hasVoluntarilySurrendered;

    /**
     * True if the player successfully entered the station with a disguise score ≥ 3
     * and has not yet been detected or exited. Used for HIDING_IN_PLAIN_SIGHT.
     */
    private boolean disguisedEntryActive;

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final Random random;

    private NotorietySystem   notorietySystem;
    private WantedSystem      wantedSystem;
    private CriminalRecord    criminalRecord;
    private RumourNetwork     rumourNetwork;
    private MagistratesCourtSystem magistratesCourtSystem;
    private ArrestSystem      arrestSystem;

    // ── Constructor ───────────────────────────────────────────────────────────

    public PoliceStationSystem() {
        this(new Random());
    }

    public PoliceStationSystem(Random random) {
        this.random                  = random;
        this.fireAlarmTriggered      = false;
        this.evacuationTimer         = 0f;
        this.evidenceLockerRaided    = false;
        this.lockerContents          = new ArrayList<>();
        this.vehicleImpounded        = false;
        this.stationAlertActive      = false;
        this.stationAlertTimer       = 0f;
        this.deskSergeantBribed      = false;
        this.custodySergeantBribed   = false;
        this.hasVoluntarilySurrendered = false;
        this.disguisedEntryActive    = false;
    }

    // ── Dependency setters ────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem s)         { this.notorietySystem         = s; }
    public void setWantedSystem(WantedSystem s)               { this.wantedSystem             = s; }
    public void setCriminalRecord(CriminalRecord s)           { this.criminalRecord           = s; }
    public void setRumourNetwork(RumourNetwork s)             { this.rumourNetwork            = s; }
    public void setMagistratesCourtSystem(MagistratesCourtSystem s) { this.magistratesCourtSystem = s; }
    public void setArrestSystem(ArrestSystem s)               { this.arrestSystem             = s; }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update. Advances evacuation timer, station alert timer, and
     * applies the gang safe-zone mechanic to any CHASING_PLAYER non-police NPCs.
     *
     * @param delta  frame delta in seconds
     * @param npcs   all active NPCs in the world (may be null/empty)
     * @param stationEntranceX  X coordinate of the station front door
     * @param stationEntranceZ  Z coordinate of the station front door
     */
    public void update(float delta, List<NPC> npcs, float stationEntranceX, float stationEntranceZ) {
        // ── Fire alarm evacuation countdown ──────────────────────────────────
        if (fireAlarmTriggered && evacuationTimer > 0f) {
            evacuationTimer -= delta;
            if (evacuationTimer <= 0f) {
                evacuationTimer      = 0f;
                fireAlarmTriggered   = false;
            }
        }

        // ── Station alert countdown ───────────────────────────────────────────
        if (stationAlertActive) {
            stationAlertTimer -= delta;
            if (stationAlertTimer <= 0f) {
                stationAlertActive = false;
                stationAlertTimer  = 0f;
            }
        }

        // ── Gang safe-zone: halt non-police CHASING_PLAYER NPCs near entrance ─
        if (npcs != null) {
            for (NPC npc : npcs) {
                if (npc.getState() == NPCState.CHASING_PLAYER
                        && npc.getType() != NPCType.POLICE
                        && npc.getType() != NPCType.ARMED_RESPONSE
                        && npc.getType() != NPCType.PCSO
                        && npc.getType() != NPCType.DESK_SERGEANT
                        && npc.getType() != NPCType.CUSTODY_SERGEANT) {
                    float dx = npc.getPosition().x - stationEntranceX;
                    float dz = npc.getPosition().z - stationEntranceZ;
                    float distSq = dx * dx + dz * dz;
                    if (distSq <= GANG_SAFE_ZONE_RADIUS * GANG_SAFE_ZONE_RADIUS) {
                        npc.setState(NPCState.IDLE);
                    }
                }
            }
        }
    }

    // ── Mechanic 1: Bribery ───────────────────────────────────────────────────

    /**
     * Attempt to bribe Geoff (DESK_SERGEANT) at the enquiry counter.
     *
     * <p>Costs {@link #BRIBE_DESK_SERGEANT_COST} COIN. Requires Notoriety ≤
     * {@link #BRIBE_DESK_MAX_NOTORIETY}. Geoff must be on duty (08:00–20:00).
     * Can only bribe once per session.</p>
     *
     * @param inventory        the player's inventory
     * @param notoriety        the player's current notoriety score
     * @param currentHour      the current in-game hour (0.0–24.0)
     * @param achievementCallback  callback for unlocking achievements
     * @return the result of the bribe attempt
     */
    public BribeResult attemptBribeDeskSergeant(Inventory inventory, int notoriety,
                                                 float currentHour,
                                                 NotorietySystem.AchievementCallback achievementCallback) {
        if (!isDeskSergeantOnDuty(currentHour)) {
            return BribeResult.NOT_ON_DUTY;
        }
        if (deskSergeantBribed) {
            return BribeResult.ALREADY_BRIBED;
        }
        if (notoriety > BRIBE_DESK_MAX_NOTORIETY) {
            return BribeResult.NOTORIETY_TOO_HIGH;
        }
        if (inventory.getItemCount(Material.COIN) < BRIBE_DESK_SERGEANT_COST) {
            return BribeResult.INSUFFICIENT_FUNDS;
        }

        // Accept bribe
        inventory.removeItem(Material.COIN, BRIBE_DESK_SERGEANT_COST);
        deskSergeantBribed = true;

        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.BRIBERY_OF_OFFICER);
        }
        if (wantedSystem != null) {
            wantedSystem.reduceWantedStars(1);
        }
        if (notorietySystem != null && achievementCallback != null) {
            notorietySystem.addNotoriety(BRIBE_NOTORIETY_PENALTY, achievementCallback);
        }
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.BENT_COPPER);
        }

        return BribeResult.SUCCESS;
    }

    /**
     * Attempt to bribe the CUSTODY_SERGEANT (night custody, 20:00–08:00).
     *
     * <p>Costs {@link #BRIBE_CUSTODY_SERGEANT_COST} COIN. Requires Notoriety ≤
     * {@link #BRIBE_CUSTODY_MAX_NOTORIETY}. Can only bribe once per session.</p>
     *
     * @param inventory        the player's inventory
     * @param notoriety        the player's current notoriety score
     * @param currentHour      the current in-game hour (0.0–24.0)
     * @param achievementCallback  callback for unlocking achievements
     * @return the result of the bribe attempt
     */
    public BribeResult attemptBribeCustodySergeant(Inventory inventory, int notoriety,
                                                    float currentHour,
                                                    NotorietySystem.AchievementCallback achievementCallback) {
        if (isDeskSergeantOnDuty(currentHour)) {
            // CUSTODY_SERGEANT not yet on duty
            return BribeResult.NOT_ON_DUTY;
        }
        if (custodySergeantBribed) {
            return BribeResult.ALREADY_BRIBED;
        }
        if (notoriety > BRIBE_CUSTODY_MAX_NOTORIETY) {
            return BribeResult.NOTORIETY_TOO_HIGH;
        }
        if (inventory.getItemCount(Material.COIN) < BRIBE_CUSTODY_SERGEANT_COST) {
            return BribeResult.INSUFFICIENT_FUNDS;
        }

        // Accept bribe
        inventory.removeItem(Material.COIN, BRIBE_CUSTODY_SERGEANT_COST);
        custodySergeantBribed = true;

        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.BRIBERY_OF_OFFICER);
        }
        if (wantedSystem != null) {
            wantedSystem.reduceWantedStars(1);
        }
        if (notorietySystem != null && achievementCallback != null) {
            notorietySystem.addNotoriety(BRIBE_NOTORIETY_PENALTY, achievementCallback);
        }

        return BribeResult.SUCCESS;
    }

    /**
     * Attempt to intimidate Geoff (DESK_SERGEANT) using BRAWLING Expert+.
     * Free version of bribery — same effect but {@link #INTIMIDATION_BACKUP_CHANCE}
     * chance Geoff calls backup anyway.
     *
     * @param currentHour     the current in-game hour
     * @param notoriety       the player's current notoriety score
     * @param achievementCallback  callback for unlocking achievements
     * @return true if intimidation succeeded (Geoff backed down); false if failed
     *         or Geoff called backup (still reduces wanted but alert triggered)
     */
    public boolean attemptIntimidateDeskSergeant(float currentHour, int notoriety,
                                                  NotorietySystem.AchievementCallback achievementCallback) {
        if (!isDeskSergeantOnDuty(currentHour)) {
            return false;
        }
        if (notoriety > BRIBE_DESK_MAX_NOTORIETY) {
            return false;
        }

        // Intimidation succeeds; reduce wanted
        deskSergeantBribed = true;
        if (wantedSystem != null) {
            wantedSystem.reduceWantedStars(1);
        }

        // Random chance Geoff calls backup anyway
        if (random.nextFloat() < INTIMIDATION_BACKUP_CHANCE) {
            triggerStationAlert();
            return true; // "Succeeded" but backup called
        }
        return true;
    }

    // ── Mechanic 2: Voluntary Surrender ───────────────────────────────────────

    /**
     * Player voluntarily surrenders at the enquiry counter.
     *
     * <p>Requires DESK_SERGEANT to be on duty and WantedSystem stars ≥ 1.
     * Marks {@link #hasVoluntarilySurrendered()} true, applies Notoriety reduction,
     * seeds {@link RumourType#TURNED_YOURSELF_IN} rumour, and awards
     * {@link AchievementType#CAME_IN_QUIETLY}.</p>
     *
     * @param wantedStars     current wanted stars
     * @param currentHour     current in-game hour
     * @param npcs            active NPCs (for rumour seeding)
     * @param achievementCallback  callback for unlocking achievements
     * @return result of the surrender attempt
     */
    public SurrenderResult voluntarilySurrender(int wantedStars, float currentHour,
                                                 List<NPC> npcs,
                                                 NotorietySystem.AchievementCallback achievementCallback) {
        if (wantedStars < 1) {
            return SurrenderResult.NO_WANTED_LEVEL;
        }
        if (!isDeskSergeantOnDuty(currentHour)) {
            return SurrenderResult.DESK_SERGEANT_NOT_ON_DUTY;
        }

        hasVoluntarilySurrendered = true;

        // Notoriety reduction
        if (notorietySystem != null && achievementCallback != null) {
            notorietySystem.reduceNotoriety(VOLUNTARY_SURRENDER_NOTORIETY_REDUCTION, achievementCallback);
        }

        // Clear wanted level
        if (wantedSystem != null) {
            wantedSystem.clearWantedLevel();
        }

        // Mark sentence reduction in court system
        if (magistratesCourtSystem != null) {
            magistratesCourtSystem.markVoluntarySurrender();
        }

        // Seed TURNED_YOURSELF_IN rumour via nearest NPC
        if (rumourNetwork != null && npcs != null && !npcs.isEmpty()) {
            NPC seed = npcs.get(0);
            rumourNetwork.addRumour(seed,
                    new Rumour(RumourType.TURNED_YOURSELF_IN,
                            "Heard someone walked into the nick and handed themselves in."));
        }

        // Award achievement
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.CAME_IN_QUIETLY);
        }

        return SurrenderResult.ACCEPTED;
    }

    // ── Mechanic 3: Evidence Locker Heist ────────────────────────────────────

    /**
     * Trigger the FIRE_ALARM_PROP distraction route.
     * Starts a {@link #FIRE_ALARM_EVACUATION_SECONDS}-second evacuation window.
     * Records {@link CriminalRecord.CrimeType#FALSE_ALARM} in CriminalRecord.
     *
     * @param npcs  active NPCs (set to IDLE / fleeing during evacuation)
     */
    public void triggerFireAlarm(List<NPC> npcs) {
        if (fireAlarmTriggered) {
            return; // already active
        }
        fireAlarmTriggered = true;
        evacuationTimer    = FIRE_ALARM_EVACUATION_SECONDS;

        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.FALSE_ALARM);
        }

        // All station NPCs flee / evacuate
        if (npcs != null) {
            for (NPC npc : npcs) {
                if (npc.getType() == NPCType.DESK_SERGEANT
                        || npc.getType() == NPCType.CUSTODY_SERGEANT
                        || npc.getType() == NPCType.POLICE) {
                    npc.setState(NPCState.FLEEING);
                }
            }
        }
    }

    /**
     * Attempt to raid the evidence locker using the specified route.
     *
     * @param route            which entry route the player used
     * @param inventory        player's inventory (looted items added here)
     * @param disguiseScore    player's current disguise score (0–5)
     * @param npcs             active NPCs (for caught detection)
     * @param achievementCallback  callback for achievements
     * @return result of the raid attempt
     */
    public EvidenceLockerResult raidEvidenceLocker(EvidenceLockerRoute route,
                                                    Inventory inventory,
                                                    int disguiseScore,
                                                    List<NPC> npcs,
                                                    NotorietySystem.AchievementCallback achievementCallback) {
        if (evidenceLockerRaided) {
            return EvidenceLockerResult.EMPTY;
        }

        // Fire alarm route requires the alarm to be active
        if (route == EvidenceLockerRoute.FIRE_ALARM && !fireAlarmTriggered) {
            return EvidenceLockerResult.FIRE_ALARM_NOT_TRIGGERED;
        }

        // Check if a station NPC catches the player (non-fire-alarm routes only)
        if (route != EvidenceLockerRoute.FIRE_ALARM) {
            boolean caught = isPlayerCaughtInEvidenceRoom(npcs, disguiseScore);
            if (caught) {
                return handleCaughtInEvidenceRoom(inventory, achievementCallback);
            }
        }

        // Populate locker if empty (first-time lazy init)
        if (lockerContents.isEmpty()) {
            populateLockerContents();
        }

        if (lockerContents.isEmpty()) {
            evidenceLockerRaided = true;
            return EvidenceLockerResult.EMPTY;
        }

        // Transfer up to MAX_CARRY items to player inventory
        Collections.shuffle(lockerContents, random);
        int taken = Math.min(EVIDENCE_LOCKER_MAX_CARRY, lockerContents.size());
        for (int i = 0; i < taken; i++) {
            inventory.addItem(lockerContents.get(i), 1);
        }
        lockerContents.clear();
        evidenceLockerRaided = true;

        // Seed station break-in rumour
        if (rumourNetwork != null && npcs != null && !npcs.isEmpty()) {
            rumourNetwork.addRumour(npcs.get(0),
                    new Rumour(RumourType.STATION_BREAK_IN,
                            "Someone only went and turned over the nick — " +
                            "walked straight into the evidence locker."));
        }

        // Award achievements
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.EVIDENCE_GONE);
            if (route == EvidenceLockerRoute.FIRE_ALARM) {
                achievementCallback.award(AchievementType.INSIDE_JOB);
            }
        }

        return EvidenceLockerResult.SUCCESS;
    }

    /**
     * Called when the player enters the station lobby in an active disguise.
     * If DisguiseSystem score ≥ 3 and Geoff does not recognise the player,
     * sets disguisedEntryActive flag. Call {@link #onDisguisedPlayerExits()} when
     * the player leaves to award HIDING_IN_PLAIN_SIGHT.
     *
     * @param disguiseScore  current DisguiseSystem score
     */
    public void onDisguisedPlayerEnters(int disguiseScore) {
        if (disguiseScore >= 3) {
            disguisedEntryActive = true;
        }
    }

    /**
     * Called when the player exits the station after a disguised entry.
     * Awards {@link AchievementType#HIDING_IN_PLAIN_SIGHT} if the entry was
     * successful and the player was not detected.
     *
     * @param achievementCallback  callback for unlocking achievements
     */
    public void onDisguisedPlayerExits(NotorietySystem.AchievementCallback achievementCallback) {
        if (disguisedEntryActive) {
            disguisedEntryActive = false;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.HIDING_IN_PLAIN_SIGHT);
            }
        }
    }

    // ── Mechanic 4: Vehicle Impound Recovery ─────────────────────────────────

    /**
     * Mark a vehicle as impounded (called by TrafficWardenSystem or police arrest logic).
     */
    public void impoundVehicle() {
        this.vehicleImpounded = true;
    }

    /**
     * Attempt to recover an impounded vehicle via the official enquiry counter route.
     * Requires 08:00–18:00, 20 COIN, and a DRIVING_LICENCE in inventory.
     *
     * @param inventory    player's inventory
     * @param currentHour  current in-game hour
     * @param achievementCallback  callback for achievements
     * @return result of the recovery attempt
     */
    public VehicleRecoveryResult recoverVehicleOfficial(Inventory inventory,
                                                         float currentHour,
                                                         NotorietySystem.AchievementCallback achievementCallback) {
        if (!vehicleImpounded) {
            return VehicleRecoveryResult.NO_VEHICLE_IMPOUNDED;
        }
        if (currentHour < IMPOUND_OPEN_HOUR || currentHour >= IMPOUND_CLOSE_HOUR) {
            return VehicleRecoveryResult.COUNTER_CLOSED;
        }
        if (inventory.getItemCount(Material.DRIVING_LICENCE) < 1) {
            return VehicleRecoveryResult.NO_LICENCE;
        }
        if (inventory.getItemCount(Material.COIN) < VEHICLE_RECOVERY_COIN_COST) {
            return VehicleRecoveryResult.INSUFFICIENT_FUNDS;
        }

        inventory.removeItem(Material.COIN, VEHICLE_RECOVERY_COIN_COST);
        vehicleImpounded = false;

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.GOT_ME_MOTOR_BACK);
        }

        return VehicleRecoveryResult.RECOVERED_OFFICIAL;
    }

    /**
     * Attempt to recover an impounded vehicle by breaking the garage door at night
     * (POLICE_GARAGE_PROP broken by CROWBAR — 8 hits required, enforced by caller).
     *
     * @param currentHour  current in-game hour
     * @param achievementCallback  callback for achievements
     * @return result of the recovery attempt
     */
    public VehicleRecoveryResult recoverVehicleUnofficial(float currentHour,
                                                           NotorietySystem.AchievementCallback achievementCallback) {
        if (!vehicleImpounded) {
            return VehicleRecoveryResult.NO_VEHICLE_IMPOUNDED;
        }

        vehicleImpounded = false;

        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.VEHICLE_RECOVERY_OFFENCE);
        }
        if (wantedSystem != null) {
            wantedSystem.increaseWantedStars(2);
        }
        if (notorietySystem != null && achievementCallback != null) {
            notorietySystem.addNotoriety(8, achievementCallback);
        }
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.GOT_ME_MOTOR_BACK);
        }

        triggerStationAlert();

        return VehicleRecoveryResult.RECOVERED_UNOFFICIAL;
    }

    // ── Helper: station alert ─────────────────────────────────────────────────

    /**
     * Trigger a station-wide hostile alert for {@link #STATION_ALERT_SECONDS} seconds.
     * Sets all station NPCs to AGGRESSIVE.
     */
    public void triggerStationAlert() {
        stationAlertActive = true;
        stationAlertTimer  = STATION_ALERT_SECONDS;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Returns true if the DESK_SERGEANT (Geoff) is on duty at the given hour.
     * Geoff works 08:00–20:00.
     */
    public boolean isDeskSergeantOnDuty(float currentHour) {
        return currentHour >= DESK_SERGEANT_START_HOUR && currentHour < DESK_SERGEANT_END_HOUR;
    }

    /** Populate the evidence locker with 3–6 randomly chosen confiscated item types. */
    private void populateLockerContents() {
        // Representative confiscatable materials (mirror ArrestSystem confiscation pool)
        Material[] pool = {
            Material.DIAMOND, Material.COIN, Material.LOCKPICK, Material.CROWBAR,
            Material.BOLT_CUTTERS, Material.BALACLAVA, Material.GLASS_CUTTER,
            Material.STOLEN_PHONE, Material.CCTV_TAPE, Material.FAKE_ID,
            Material.POLICE_KEY_CARD, Material.DRIVING_LICENCE
        };

        int count = EVIDENCE_LOCKER_MIN_ITEMS
                + random.nextInt(EVIDENCE_LOCKER_MAX_ITEMS - EVIDENCE_LOCKER_MIN_ITEMS + 1);

        List<Material> shuffled = new ArrayList<>();
        for (Material m : pool) {
            shuffled.add(m);
        }
        Collections.shuffle(shuffled, random);

        for (int i = 0; i < Math.min(count, shuffled.size()); i++) {
            lockerContents.add(shuffled.get(i));
        }
    }

    /**
     * Check whether a POLICE or DESK_SERGEANT NPC catches the player in the
     * evidence room. Returns true if caught.
     */
    private boolean isPlayerCaughtInEvidenceRoom(List<NPC> npcs, int disguiseScore) {
        if (npcs == null) return false;
        // Disguise score ≥ 4 bypasses detection completely
        if (disguiseScore >= 4) return false;

        for (NPC npc : npcs) {
            if (npc.getType() == NPCType.POLICE
                    || npc.getType() == NPCType.DESK_SERGEANT
                    || npc.getType() == NPCType.CUSTODY_SERGEANT) {
                if (npc.getState() != NPCState.FLEEING && npc.getState() != NPCState.KNOCKED_OUT) {
                    // 60% base catch rate, reduced by disguise score
                    float catchChance = 0.60f - (disguiseScore * 0.10f);
                    if (random.nextFloat() < Math.max(0.10f, catchChance)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Handle the player being caught in the evidence room. */
    private EvidenceLockerResult handleCaughtInEvidenceRoom(Inventory inventory,
                                                             NotorietySystem.AchievementCallback achievementCallback) {
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.EVIDENCE_TAMPERING);
            criminalRecord.record(CriminalRecord.CrimeType.BREAKING_AND_ENTERING_POLICE_STATION);
        }
        if (wantedSystem != null) {
            wantedSystem.increaseWantedStars(EVIDENCE_CAUGHT_WANTED_STARS);
        }
        if (notorietySystem != null && achievementCallback != null) {
            notorietySystem.addNotoriety(EVIDENCE_TAMPERING_NOTORIETY_PER_ITEM, achievementCallback);
        }
        triggerStationAlert();
        return EvidenceLockerResult.CAUGHT;
    }

    // ── State accessors ───────────────────────────────────────────────────────

    /** Returns true if the fire alarm evacuation is currently active. */
    public boolean isFireAlarmActive() {
        return fireAlarmTriggered && evacuationTimer > 0f;
    }

    /** Remaining evacuation time in seconds (0 if not active). */
    public float getEvacuationTimer() {
        return evacuationTimer;
    }

    /** Returns true if the evidence locker has been successfully raided this session. */
    public boolean isEvidenceLockerRaided() {
        return evidenceLockerRaided;
    }

    /** Returns true if a vehicle is currently impounded. */
    public boolean isVehicleImpounded() {
        return vehicleImpounded;
    }

    /** Returns true while the station-wide alert is active. */
    public boolean isStationAlertActive() {
        return stationAlertActive;
    }

    /** Returns true if Geoff has been bribed this session. */
    public boolean isDeskSergeantBribed() {
        return deskSergeantBribed;
    }

    /** Returns true if the player has voluntarily surrendered this session. */
    public boolean hasVoluntarilySurrendered() {
        return hasVoluntarilySurrendered;
    }

    /** Returns an unmodifiable view of the current locker contents (for testing). */
    public List<Material> getLockerContents() {
        return Collections.unmodifiableList(lockerContents);
    }
}

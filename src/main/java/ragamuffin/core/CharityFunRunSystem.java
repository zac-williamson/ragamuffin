package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1487 / #1357: Northfield Charity Fun Run — Janet's Bibs, the Course Shortcut
 * &amp; the Registration Pot Pinch.
 *
 * <p>The Northfield Community Centre Charity Fun Run runs every 28 days starting on
 * day 21 (08:30–10:30). Janet ({@code NPCType.FUN_RUN_MARSHAL}) manages registration
 * from 08:30–09:00, the race starts at 09:00, and the event closes at 10:30.
 *
 * <h3>Schedule</h3>
 * <ul>
 *   <li>08:30 — Janet spawns at {@code START_FINISH_ARCH_PROP}; registration opens.
 *               Rain check: if {@code Weather.RAIN} or worse, event may be cancelled.</li>
 *   <li>09:00 — Race gun fires; timer starts for registered players.</li>
 *   <li>10:30 — Event closes.</li>
 * </ul>
 *
 * <h3>Registration (08:30–09:00)</h3>
 * Player pays {@code ENTRY_FEE} = 2 COIN and receives {@code RACE_NUMBER_BIB} +
 * {@code SPONSOR_SHEET}. Up to {@code MAX_SPONSOR_NPCS} = 8 NPCs can be solicited for
 * pledges: PENSIONER 80% / 2 COIN, PUBLIC 50% / 1 COIN, CHUGGER always refuses.
 *
 * <h3>The Race (09:00–10:30)</h3>
 * Player passes all 8 {@code FUN_RUN_CHECKPOINT_PROP} in order within
 * {@code WINNER_TIME_SECONDS} = 1500 in-game seconds for {@code WINNERS_MEDAL} +
 * {@code COMMUNITY_RUNNER_ELITE}. Dog companion present: {@code WALKIES_WINNER}.
 * {@code WATER_STATION_PROP} at checkpoint 4 grants {@code WATER_CUP}.
 *
 * <h3>Course Cutting</h3>
 * Skipping checkpoints and finishing: if a JOGGER NPC is within
 * {@code WITNESS_RADIUS_BLOCKS}, seeds {@code COURSE_CUTTING} rumour, Notoriety +2.
 * Otherwise: {@code SHAMELESS_SHORTCUT} achievement.
 *
 * <h3>Registration Pot Pinch</h3>
 * Pickpocket Janet: up to {@code MAX_POT_STEAL} = 40 COIN, {@code THEFT_FROM_PERSON},
 * Notoriety +5. Volunteer with {@code HIGH_VIS_JACKET}: keep pot — {@code CHARITY_FRAUD},
 * {@code VOLUNTEER_SUSPICION_DELAY_SECONDS} = 30 head start.
 */
public class CharityFunRunSystem {

    // ── Schedule constants ───────────────────────────────────────────────────

    /** Day-of-cycle on which the first event falls. */
    public static final int FIRST_EVENT_DAY = 21;

    /** Number of days between events. */
    public static final int RACE_INTERVAL_DAYS = 28;

    /** Hour at which registration opens and Janet spawns. */
    public static final float REGISTRATION_OPEN_HOUR = 8.5f;  // 08:30

    /** Hour at which the race gun fires. */
    public static final float RACE_START_HOUR = 9.0f;

    /** Hour at which the event closes. */
    public static final float RACE_END_HOUR = 10.5f;  // 10:30

    // ── Race mechanics constants ─────────────────────────────────────────────

    /** Entry fee in COIN. */
    public static final int ENTRY_FEE = 2;

    /** Maximum number of sponsor NPCs a player can solicit. */
    public static final int MAX_SPONSOR_NPCS = 8;

    /** Maximum sponsor pledge total in COIN. */
    public static final int MAX_SPONSOR_COIN = 20;

    /** Number of checkpoints on the course. */
    public static final int CHECKPOINT_COUNT = 8;

    /** Block radius within which a player counts as having passed a checkpoint. */
    public static final float CHECKPOINT_RADIUS = 2.0f;

    /** Time limit (in-game seconds) for medal and elite achievement. */
    public static final float WINNER_TIME_SECONDS = 1500.0f;  // 25 minutes

    /** Minimum NPC running speed (blocks/second). */
    public static final float NPC_SLOW_SPEED = 3.0f;

    /** Maximum NPC running speed (blocks/second). */
    public static final float NPC_FAST_SPEED = 6.0f;

    /** Block radius within which a JOGGER witnesses course-cutting. */
    public static final float WITNESS_RADIUS_BLOCKS = 15.0f;

    /** Maximum number of registered participants. */
    public static final int MAX_PARTICIPANTS = 20;

    /** Maximum COIN that can be stolen from the registration pot. */
    public static final int MAX_POT_STEAL = 40;

    /** Seconds before Janet notices the volunteer embezzlement. */
    public static final float VOLUNTEER_SUSPICION_DELAY_SECONDS = 30.0f;

    // ── Penalty constants ────────────────────────────────────────────────────

    /** Notoriety gain for pickpocketing Janet. */
    public static final int PICKPOCKET_NOTORIETY = 5;

    /** Notoriety gain for witnessed course-cutting. */
    public static final int COURSE_CUT_NOTORIETY = 2;

    /** Notoriety gain for charity fraud (sponsor sheet fraud). */
    public static final int FRAUD_NOTORIETY = 4;

    // ── Pledge probabilities ─────────────────────────────────────────────────

    /** Probability (0–1) that a PENSIONER pledges. */
    public static final float PENSIONER_PLEDGE_CHANCE = 0.80f;

    /** COIN pledged by a PENSIONER. */
    public static final int PENSIONER_PLEDGE_AMOUNT = 2;

    /** Probability (0–1) that a PUBLIC NPC pledges. */
    public static final float PUBLIC_PLEDGE_CHANCE = 0.50f;

    /** COIN pledged by a PUBLIC NPC. */
    public static final int PUBLIC_PLEDGE_AMOUNT = 1;

    // ── State ────────────────────────────────────────────────────────────────

    private final Random random;

    /** True once registration has opened this cycle. */
    private boolean registrationOpen = false;

    /** True once the race gun has fired. */
    private boolean raceStarted = false;

    /** True once the event has been cancelled by rain. */
    private boolean eventCancelled = false;

    /** True once the rain check has been performed at 08:30. */
    private boolean rainChecked = false;

    /** True if the player is registered for the current event. */
    private boolean playerRegistered = false;

    /** Accumulated pledge total on the player's sponsor sheet. */
    private int pledgeTotal = 0;

    /** Number of NPCs already solicited this event. */
    private int sponsorSolicitations = 0;

    /** COIN accumulated in Janet's registration pot. */
    private int potTotal = 0;

    /** True if the player has already been given a water cup this event. */
    private boolean waterCupGiven = false;

    /** True once the RAINED_OFF achievement has been awarded. */
    private boolean rainedOffAwarded = false;

    /** True once the FUN_RUN_CANCELLED rumour has been seeded. */
    private boolean cancelledRumourSeeded = false;

    /** True if the player volunteered as assistant (HIGH_VIS_JACKET embezzlement path). */
    private boolean playerVolunteered = false;

    /** Timer for volunteer suspicion delay after embezzlement. */
    private float volunteerSuspicionTimer = 0f;

    /** True once Janet has noticed the embezzlement. */
    private boolean janetNoticed = false;

    /** Last day count for which this event was run (to detect new cycles). */
    private int lastEventDayCount = -1;

    /** The day count on which the current event is scheduled. */
    private int currentEventDayCount = -1;

    /** Managed NPC list (Janet and any spawned participants). */
    private final List<NPC> eventNpcs = new ArrayList<>();

    // ── Constructor ──────────────────────────────────────────────────────────

    /** Create the system with the default random source. */
    public CharityFunRunSystem() {
        this(new Random());
    }

    /** Create the system with a seeded random source (for tests). */
    public CharityFunRunSystem(Random random) {
        this.random = random;
    }

    // ── Public query methods ──────────────────────────────────────────────────

    /**
     * Returns true if today is an event day (day 21 of any 28-day cycle).
     *
     * @param dayCount    in-game day count (1-based)
     * @param hour        current in-game hour
     */
    public boolean isEventDay(int dayCount, float hour) {
        // Event fires on day 21 and every 28 days after: (dayCount - 21) % 28 == 0
        if (dayCount < FIRST_EVENT_DAY) return false;
        int offset = (dayCount - FIRST_EVENT_DAY) % RACE_INTERVAL_DAYS;
        if (offset != 0) return false;
        return hour >= REGISTRATION_OPEN_HOUR && hour <= RACE_END_HOUR;
    }

    /**
     * Returns true if registration is currently open.
     */
    public boolean isRegistrationOpen() {
        return registrationOpen && !raceStarted && !eventCancelled;
    }

    /**
     * Returns true if the race is in progress.
     */
    public boolean isRaceActive() {
        return raceStarted && !eventCancelled;
    }

    /**
     * Returns true if the event was cancelled by rain.
     */
    public boolean isCancelled() {
        return eventCancelled;
    }

    /**
     * Returns true if the player is registered for the current event.
     */
    public boolean isPlayerRegistered() {
        return playerRegistered;
    }

    /**
     * Returns true if the event is currently active (registration or race running).
     */
    public boolean isActive() {
        return (registrationOpen || raceStarted) && !eventCancelled;
    }

    /**
     * Returns the current pledge total on the player's sponsor sheet.
     */
    public int getPledgeTotal() {
        return pledgeTotal;
    }

    /**
     * Returns the number of NPCs already solicited this event.
     */
    public int getSponsorSolicitations() {
        return sponsorSolicitations;
    }

    /**
     * Returns the COIN currently in Janet's registration pot.
     */
    public int getPotTotal() {
        return potTotal;
    }

    /**
     * Returns the list of NPCs managed by this system.
     */
    public List<NPC> getEventNpcs() {
        return eventNpcs;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update. Call every game tick.
     *
     * @param delta          seconds elapsed since last frame
     * @param timeSystem     current time/date source
     * @param npcs           global NPC list (for spawning / despawning)
     * @param weatherSystem  weather system (may be null in tests)
     * @param rumourNetwork  rumour network (may be null in tests)
     * @param achievement    achievement callback (may be null in tests)
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       List<NPC> npcs,
                       WeatherSystem weatherSystem,
                       RumourNetwork rumourNetwork,
                       AchievementCallback achievement) {

        int dayCount = timeSystem.getDayCount();
        float hour = timeSystem.getTime();

        // Check if today is an event day (and the event window is open or about to open)
        boolean onEventDay = isOnEventCycle(dayCount, hour);

        if (!onEventDay) {
            // If we've moved off the event day, reset for next cycle
            if (currentEventDayCount >= 0 && currentEventDayCount != dayCount) {
                resetForNewCycle(npcs);
            }
            return;
        }

        // Mark the current event day
        if (currentEventDayCount != dayCount) {
            currentEventDayCount = dayCount;
        }

        // ── Rain check at 08:30 ───────────────────────────────────────────────
        if (!rainChecked && hour >= REGISTRATION_OPEN_HOUR) {
            rainChecked = true;
            if (weatherSystem != null
                    && (weatherSystem.getCurrentWeather() == Weather.RAIN
                            || weatherSystem.getCurrentWeather() == Weather.THUNDERSTORM)) {
                // Cancel the event
                eventCancelled = true;
                // Seed FUN_RUN_CANCELLED rumour
                if (!cancelledRumourSeeded && rumourNetwork != null && npcs != null) {
                    seedCancelledRumour(rumourNetwork, npcs);
                    cancelledRumourSeeded = true;
                }
                // Award RAINED_OFF to registered player
                if (playerRegistered && !rainedOffAwarded && achievement != null) {
                    achievement.award(AchievementType.RAINED_OFF);
                    rainedOffAwarded = true;
                }
                return;
            }
        }

        if (eventCancelled) return;

        // ── Registration opens at 08:30 ───────────────────────────────────────
        if (!registrationOpen && hour >= REGISTRATION_OPEN_HOUR) {
            registrationOpen = true;
            spawnJanet(npcs);
        }

        // ── Race gun at 09:00 ─────────────────────────────────────────────────
        if (registrationOpen && !raceStarted && hour >= RACE_START_HOUR) {
            raceStarted = true;
        }

        // ── Volunteer suspicion timer ─────────────────────────────────────────
        if (playerVolunteered && !janetNoticed) {
            volunteerSuspicionTimer += delta;
            if (volunteerSuspicionTimer >= VOLUNTEER_SUSPICION_DELAY_SECONDS) {
                janetNoticed = true;
            }
        }

        // ── Event closes at 10:30 ─────────────────────────────────────────────
        if (raceStarted && hour >= RACE_END_HOUR) {
            closeEvent(npcs);
        }
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Player pays the entry fee and registers for the fun run.
     *
     * @param inventory  player inventory (must contain >= 2 COIN)
     * @return {@link RegistrationResult} indicating outcome
     */
    public RegistrationResult register(Inventory inventory) {
        if (!registrationOpen || raceStarted || eventCancelled) {
            return RegistrationResult.NOT_OPEN;
        }
        if (playerRegistered) {
            return RegistrationResult.ALREADY_REGISTERED;
        }
        if (inventory.getItemCount(Material.COIN) < ENTRY_FEE) {
            return RegistrationResult.INSUFFICIENT_FUNDS;
        }

        inventory.removeItem(Material.COIN, ENTRY_FEE);
        inventory.addItem(Material.RACE_NUMBER_BIB, 1);
        inventory.addItem(Material.SPONSOR_SHEET, 1);
        playerRegistered = true;
        potTotal = Math.min(potTotal + ENTRY_FEE, MAX_POT_STEAL);

        return RegistrationResult.SUCCESS;
    }

    /** Result of a registration attempt. */
    public enum RegistrationResult {
        /** Registration successful. BIB and SPONSOR_SHEET added to inventory. */
        SUCCESS,
        /** Registration not open (wrong time or event cancelled). */
        NOT_OPEN,
        /** Player already registered. */
        ALREADY_REGISTERED,
        /** Insufficient COIN. */
        INSUFFICIENT_FUNDS
    }

    // ── Sponsor solicitation ──────────────────────────────────────────────────

    /**
     * Approach an NPC with the sponsor sheet to solicit a pledge.
     *
     * @param npc       the NPC being approached
     * @param inventory player inventory (must contain SPONSOR_SHEET)
     * @return {@link PledgeResult} indicating outcome
     */
    public PledgeResult solicitPledge(NPC npc, Inventory inventory) {
        if (!playerRegistered) {
            return PledgeResult.NOT_REGISTERED;
        }
        if (!inventory.hasItem(Material.SPONSOR_SHEET)) {
            return PledgeResult.NO_SHEET;
        }
        if (sponsorSolicitations >= MAX_SPONSOR_NPCS) {
            return PledgeResult.MAX_REACHED;
        }
        if (pledgeTotal >= MAX_SPONSOR_COIN) {
            return PledgeResult.MAX_COIN_REACHED;
        }

        NPCType type = npc.getType();

        if (type == NPCType.CHUGGER) {
            return PledgeResult.REFUSED;
        }

        float roll = random.nextFloat();
        int pledge = 0;

        if (type == NPCType.PENSIONER) {
            if (roll < PENSIONER_PLEDGE_CHANCE) {
                pledge = PENSIONER_PLEDGE_AMOUNT;
            }
        } else if (type == NPCType.PUBLIC) {
            if (roll < PUBLIC_PLEDGE_CHANCE) {
                pledge = PUBLIC_PLEDGE_AMOUNT;
            }
        } else {
            // Other NPC types: 50% / 1 COIN
            if (roll < PUBLIC_PLEDGE_CHANCE) {
                pledge = PUBLIC_PLEDGE_AMOUNT;
            }
        }

        sponsorSolicitations++;

        if (pledge <= 0) {
            return PledgeResult.DECLINED;
        }

        pledgeTotal = Math.min(pledgeTotal + pledge, MAX_SPONSOR_COIN);
        return PledgeResult.SUCCESS;
    }

    /** Result of a sponsor solicitation attempt. */
    public enum PledgeResult {
        /** Pledge recorded successfully. */
        SUCCESS,
        /** NPC declined (probability roll failed). */
        DECLINED,
        /** NPC refuses (CHUGGER). */
        REFUSED,
        /** Player is not registered. */
        NOT_REGISTERED,
        /** Player does not have SPONSOR_SHEET. */
        NO_SHEET,
        /** Maximum NPC solicitations reached. */
        MAX_REACHED,
        /** Maximum pledge coin reached. */
        MAX_COIN_REACHED
    }

    // ── Race completion ───────────────────────────────────────────────────────

    /**
     * Called when the player crosses the finish arch after completing all checkpoints.
     *
     * @param inventory           player inventory
     * @param timeElapsedSeconds  in-game seconds since the race started
     * @param hasActiveDog        whether the player has an active dog companion
     * @param achievement         achievement callback
     * @return {@link FinishResult} indicating outcome
     */
    public FinishResult finishRun(Inventory inventory,
                                  float timeElapsedSeconds,
                                  boolean hasActiveDog,
                                  AchievementCallback achievement) {
        if (!playerRegistered) {
            return FinishResult.NOT_REGISTERED;
        }

        boolean won = timeElapsedSeconds <= WINNER_TIME_SECONDS;

        if (won) {
            inventory.addItem(Material.WINNERS_MEDAL, 1);
            if (achievement != null) {
                achievement.award(AchievementType.COMMUNITY_RUNNER_ELITE);
            }
        }

        // Pay out sponsor pledges
        if (pledgeTotal > 0) {
            inventory.addItem(Material.COIN, pledgeTotal);
        }

        // Dog companion bonus
        if (hasActiveDog && achievement != null) {
            achievement.award(AchievementType.WALKIES_WINNER);
        }

        return won ? FinishResult.WON : FinishResult.FINISHED;
    }

    /** Result of a run finish. */
    public enum FinishResult {
        /** Finished under the time limit — medal and elite achievement awarded. */
        WON,
        /** Finished but over the time limit. */
        FINISHED,
        /** Player not registered. */
        NOT_REGISTERED
    }

    // ── Course completion check ───────────────────────────────────────────────

    /**
     * Check whether all checkpoints have been visited in order.
     *
     * @param checkpointsVisited number of checkpoints visited in order
     * @return {@link CourseStatus}
     */
    public CourseStatus checkCourseCompletion(int checkpointsVisited) {
        if (checkpointsVisited >= CHECKPOINT_COUNT) {
            return CourseStatus.COMPLETE;
        }
        return CourseStatus.INCOMPLETE;
    }

    /** Status of the player's course progress. */
    public enum CourseStatus {
        /** All 8 checkpoints visited in order. */
        COMPLETE,
        /** One or more checkpoints not yet visited. */
        INCOMPLETE
    }

    // ── Course cutting detection ──────────────────────────────────────────────

    /**
     * Detect whether the player has cut the course. Called when the player finishes
     * without having visited all checkpoints.
     *
     * <p>If a JOGGER NPC is within {@code WITNESS_RADIUS_BLOCKS} of the player:
     * seeds {@code COURSE_CUTTING} rumour, adds Notoriety +2.
     * If unwitnessed: awards {@code SHAMELESS_SHORTCUT}.
     *
     * @param checkpointsSkipped  number of skipped checkpoints
     * @param witnessNpc          a JOGGER NPC in witness range, or null if none
     * @param rumourNetwork       rumour network (may be null)
     * @param notorietySystem     notoriety system (may be null)
     * @param npcs                global NPC list (for rumour seeding)
     * @param achievement         achievement callback (may be null)
     * @return {@link CourseCutResult}
     */
    public CourseCutResult detectCourseCut(int checkpointsSkipped,
                                           NPC witnessNpc,
                                           RumourNetwork rumourNetwork,
                                           NotorietySystem notorietySystem,
                                           List<NPC> npcs,
                                           AchievementCallback achievement) {
        if (checkpointsSkipped <= 0) {
            return CourseCutResult.NO_CUT;
        }

        if (witnessNpc != null && witnessNpc.getType() == NPCType.JOGGER) {
            // Witnessed: seed rumour, add notoriety
            if (rumourNetwork != null) {
                rumourNetwork.addRumour(witnessNpc, new Rumour(
                        RumourType.COURSE_CUTTING,
                        "Someone cut the corner on the charity run — "
                        + "a jogger saw the whole thing."));
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(COURSE_CUT_NOTORIETY, null);
            }
            return CourseCutResult.WITNESSED;
        } else {
            // Unwitnessed: award achievement
            if (achievement != null) {
                achievement.award(AchievementType.SHAMELESS_SHORTCUT);
            }
            return CourseCutResult.UNWITNESSED;
        }
    }

    /** Result of a course cut detection. */
    public enum CourseCutResult {
        /** No checkpoints were skipped. */
        NO_CUT,
        /** Course cut witnessed — rumour seeded and notoriety added. */
        WITNESSED,
        /** Course cut unwitnessed — SHAMELESS_SHORTCUT achievement awarded. */
        UNWITNESSED
    }

    // ── Rain cancellation ─────────────────────────────────────────────────────

    /**
     * Check whether the event should be cancelled due to heavy rain.
     *
     * @param weather current weather
     * @return {@link EventStatus}
     */
    public EventStatus checkRainCancellation(Weather weather) {
        if (weather == Weather.RAIN || weather == Weather.THUNDERSTORM) {
            eventCancelled = true;
            return EventStatus.CANCELLED;
        }
        return EventStatus.RUNNING;
    }

    /** Status of the event after a rain check. */
    public enum EventStatus {
        /** Event cancelled due to heavy rain. */
        CANCELLED,
        /** Event proceeding. */
        RUNNING
    }

    // ── Sponsor sheet collection ──────────────────────────────────────────────

    /**
     * Player presents sponsor sheet to Janet for payout.
     *
     * <p>If the player has finished the race (hasFinished=true): pledges paid out.
     * If not finished and Notoriety &lt; 5: Janet trusts them, pledges paid.
     * If not finished and Notoriety &ge; 5 and a JOGGER is within
     * {@code WITNESS_RADIUS_BLOCKS}: {@code CHARITY_FRAUD} recorded, Notoriety +4,
     * sheet confiscated.
     *
     * @param inventory       player inventory
     * @param hasFinished     whether the player legitimately finished the run
     * @param notorietySystem notoriety system
     * @param criminalRecord  criminal record
     * @param witnessNpc      a JOGGER NPC within witness radius, or null
     * @param achievement     achievement callback (may be null)
     * @return {@link CollectPledgesResult}
     */
    public CollectPledgesResult collectPledges(Inventory inventory,
                                               boolean hasFinished,
                                               NotorietySystem notorietySystem,
                                               CriminalRecord criminalRecord,
                                               NPC witnessNpc,
                                               AchievementCallback achievement) {
        if (!inventory.hasItem(Material.SPONSOR_SHEET)) {
            return CollectPledgesResult.NO_SHEET;
        }

        int currentNotoriety = (notorietySystem != null) ? notorietySystem.getNotoriety() : 0;

        if (!hasFinished && currentNotoriety >= 5 && witnessNpc != null) {
            // Fraud detected
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.CHARITY_FRAUD);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(FRAUD_NOTORIETY, null);
            }
            inventory.removeItem(Material.SPONSOR_SHEET, 1);
            return CollectPledgesResult.FRAUD_CAUGHT;
        }

        // Pay out pledges
        if (pledgeTotal > 0) {
            inventory.addItem(Material.COIN, pledgeTotal);
        }
        inventory.removeItem(Material.SPONSOR_SHEET, 1);
        return CollectPledgesResult.SUCCESS;
    }

    /** Result of collecting sponsor pledges. */
    public enum CollectPledgesResult {
        /** Pledges paid out successfully. */
        SUCCESS,
        /** Fraud detected — sheet confiscated, crime recorded. */
        FRAUD_CAUGHT,
        /** Player does not have SPONSOR_SHEET. */
        NO_SHEET
    }

    // ── Pickpocketing Janet ───────────────────────────────────────────────────

    /**
     * Attempt to pickpocket Janet's bum bag.
     *
     * @param inventory       player inventory
     * @param notorietySystem notoriety system
     * @param criminalRecord  criminal record
     * @param wantedSystem    wanted system (may be null; +1 star if witnessed)
     * @param witnessed       true if a witness NPC saw the theft
     * @return {@link PickpocketResult}
     */
    public PickpocketResult pickpocketMarshal(Inventory inventory,
                                              NotorietySystem notorietySystem,
                                              CriminalRecord criminalRecord,
                                              WantedSystem wantedSystem,
                                              boolean witnessed) {
        if (potTotal <= 0) {
            return PickpocketResult.EMPTY;
        }

        int stolen = Math.min(potTotal, MAX_POT_STEAL);
        potTotal -= stolen;
        inventory.addItem(Material.COIN, stolen);

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.THEFT_FROM_PERSON);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(PICKPOCKET_NOTORIETY, null);
        }
        if (witnessed && wantedSystem != null) {
            wantedSystem.addWantedStars(1, 0f, 0f, 0f, null);
        }

        return PickpocketResult.SUCCESS;
    }

    /** Result of a pickpocket attempt on Janet. */
    public enum PickpocketResult {
        /** Pickpocket succeeded. COIN transferred, crime recorded. */
        SUCCESS,
        /** Registration pot is empty. */
        EMPTY,
        /** Caught in the act. */
        CAUGHT
    }

    // ── Volunteer embezzlement ────────────────────────────────────────────────

    /**
     * Player volunteers as registration assistant (must have {@code HIGH_VIS_JACKET}).
     * Gives them access to the pot for embezzlement with a 30-second head start.
     *
     * @param inventory      player inventory (must contain HIGH_VIS_JACKET)
     * @param notorietySystem notoriety for eligibility check (must be < 10)
     * @return {@link VolunteerResult}
     */
    public VolunteerResult volunteer(Inventory inventory, NotorietySystem notorietySystem) {
        if (!registrationOpen || raceStarted) {
            return VolunteerResult.NOT_AVAILABLE;
        }
        if (!inventory.hasItem(Material.HIGH_VIS_JACKET)) {
            return VolunteerResult.NO_JACKET;
        }
        int notoriety = (notorietySystem != null) ? notorietySystem.getNotoriety() : 0;
        if (notoriety >= 10) {
            return VolunteerResult.TOO_NOTORIOUS;
        }
        if (playerVolunteered) {
            return VolunteerResult.ALREADY_VOLUNTEERED;
        }
        playerVolunteered = true;
        volunteerSuspicionTimer = 0f;
        return VolunteerResult.SUCCESS;
    }

    /**
     * Embezzle the registration pot after volunteering (before Janet notices).
     *
     * @param inventory      player inventory
     * @param criminalRecord criminal record
     * @return COIN taken, or 0 if embezzlement not possible
     */
    public int embezzlePot(Inventory inventory, CriminalRecord criminalRecord) {
        if (!playerVolunteered || janetNoticed || potTotal <= 0) {
            return 0;
        }

        int taken = potTotal;
        potTotal = 0;
        inventory.addItem(Material.COIN, taken);

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.CHARITY_FRAUD);
        }

        return taken;
    }

    /** Result of a volunteer attempt. */
    public enum VolunteerResult {
        /** Volunteering accepted. */
        SUCCESS,
        /** Not available (wrong time). */
        NOT_AVAILABLE,
        /** Player doesn't have HIGH_VIS_JACKET. */
        NO_JACKET,
        /** Notoriety too high (>= 10). */
        TOO_NOTORIOUS,
        /** Already volunteered this event. */
        ALREADY_VOLUNTEERED
    }

    // ── Water station ─────────────────────────────────────────────────────────

    /**
     * Player presses E at the WATER_STATION_PROP (at checkpoint 4).
     *
     * @param inventory player inventory
     * @return true if a water cup was given
     */
    public boolean takeWaterCup(Inventory inventory) {
        if (waterCupGiven) return false;
        inventory.addItem(Material.WATER_CUP, 1);
        waterCupGiven = true;
        return true;
    }

    // ── Achievement callback interface ────────────────────────────────────────

    /** Callback interface for awarding achievements, matching the project convention. */
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Returns true if the given day and hour are within any event day cycle
     * (including the pre-registration window at 08:30).
     */
    private boolean isOnEventCycle(int dayCount, float hour) {
        if (dayCount < FIRST_EVENT_DAY) return false;
        int offset = (dayCount - FIRST_EVENT_DAY) % RACE_INTERVAL_DAYS;
        if (offset != 0) return false;
        return hour >= REGISTRATION_OPEN_HOUR && hour <= RACE_END_HOUR;
    }

    private void spawnJanet(List<NPC> npcs) {
        NPC janet = new NPC(NPCType.FUN_RUN_MARSHAL, 0f, 0f, 0f);
        eventNpcs.add(janet);
        if (npcs != null) npcs.add(janet);
    }

    private void seedCancelledRumour(RumourNetwork rumourNetwork, List<NPC> npcs) {
        NPC seed = null;
        for (NPC npc : npcs) {
            if (npc.getType() == NPCType.PUBLIC || npc.getType() == NPCType.PENSIONER) {
                seed = npc;
                break;
            }
        }
        if (seed != null) {
            rumourNetwork.addRumour(seed, new Rumour(
                    RumourType.FUN_RUN_CANCELLED,
                    "That charity fun run got rained off this morning — Janet was gutted, apparently."));
        }
    }

    private void closeEvent(List<NPC> npcs) {
        if (npcs != null) {
            npcs.removeAll(eventNpcs);
        }
        eventNpcs.clear();
        raceStarted = false;
        registrationOpen = false;
    }

    /** Testing hook: reset rain-checked flag so rain check can fire again. */
    public void resetRainCheckedForTesting() {
        this.rainChecked = false;
    }

    private void resetForNewCycle(List<NPC> npcs) {
        if (npcs != null) {
            npcs.removeAll(eventNpcs);
        }
        eventNpcs.clear();
        registrationOpen = false;
        raceStarted = false;
        eventCancelled = false;
        rainChecked = false;
        playerRegistered = false;
        pledgeTotal = 0;
        sponsorSolicitations = 0;
        potTotal = 0;
        waterCupGiven = false;
        rainedOffAwarded = false;
        cancelledRumourSeeded = false;
        playerVolunteered = false;
        volunteerSuspicionTimer = 0f;
        janetNoticed = false;
        lastEventDayCount = currentEventDayCount;
        currentEventDayCount = -1;
    }
}

package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1493: Northfield Amateur Photography Club — Norman's Darkroom,
 * the Wildlife Competition &amp; the Planning Application Heist.
 *
 * <p>Norman Briggs ({@code NPCType.PHOTO_CLUB_CHAIR}) runs the Northfield Amateur
 * Photography Club from the Community Centre every fortnight (day 12 of a 14-day
 * cycle), 19:00–21:00.
 *
 * <h3>Mechanic 1 — Photography &amp; Entry</h3>
 * Player buys/finds a {@code DISPOSABLE_CAMERA} (3 charges), photographs animal NPCs
 * or scenic props within 4 blocks. Quality score 0–100 based on subject, time-of-day,
 * weather, and development method. Develop at the chemist (2 COIN, 1 hour) or
 * self-develop in Norman's locked darkroom (+10 quality bonus).
 * Enter the competition before 19:00 by handing the print to Norman.
 *
 * <h3>Mechanic 2 — Judging (19:30–20:00)</h3>
 * Derek (Councillor's nephew) has a fixed score of 72. Beat it legitimately →
 * {@code CANDID_WINNER} achievement + 8 COIN + {@code WINNERS_CERTIFICATE_PROP}.
 * Score 50–72 → runner-up. Derek wins by default if player doesn't enter.
 *
 * <h3>Mechanic 3 — Rigging</h3>
 * (a) Swap Derek's submission in the {@code SUBMISSION_BOX_PROP} before members arrive —
 * unwitnessed = no crime + {@code DARKROOM_SABOTEUR} achievement;
 * witnessed = {@code PETTY_THEFT} + Notoriety +4.
 * (b) Bribe Norman for 12 COIN pre-judging — 50% success adds +25 to player score;
 * failure = {@code BRIBERY} crime + Notoriety +5 + WantedLevel +1 +
 * {@code PHOTO_CLUB_RIGGED} rumour. Win via bribe = {@code BENT_LENS} achievement.
 *
 * <h3>Mechanic 4 — Darkroom Break-In &amp; Planning Application Heist (20:30–21:00)</h3>
 * Norman's darkroom ({@code DARKROOM_DOOR_PROP}) can be opened by lockpick or by
 * pickpocketing Norman's {@code DARKROOM_KEY}. Inside: {@code PHOTO_DEVELOPER_PROP}
 * for self-development, and {@code PLANNING_APPLICATION_FOLDER_PROP} (council's secret
 * plan to demolish the park bandstand for a car park). Steal the
 * {@code PLANNING_APPLICATION_DOCUMENT} — sell to Fence (15 COIN), tip to newspaper
 * (Council Respect −20, Vibes +10, {@code CIVIC_HERO} achievement), or give to Street
 * Lads (Respect +15).
 *
 * <h3>Integrations</h3>
 * <ul>
 *   <li>{@link WeatherSystem} — photo quality modifier.</li>
 *   <li>{@link TimeSystem} — 14-day cycle, golden hour bonus.</li>
 *   <li>{@link NotorietySystem} — bribery failure/witnessed swap/witnessed heist.</li>
 *   <li>{@link WantedSystem} — bribery failure adds wanted star.</li>
 *   <li>{@link CriminalRecord} — BRIBERY, PETTY_THEFT, PLANNING_DOCUMENT_THEFT.</li>
 *   <li>{@link RumourNetwork} — PHOTO_CLUB_RIGGED, BANDSTAND_UNDER_THREAT rumours.</li>
 *   <li>{@link FactionSystem} — THE_COUNCIL −20 on exposé; STREET_LADS +15 on document gift.</li>
 *   <li>{@link NeighbourhoodSystem} — Vibes +10 on exposé.</li>
 * </ul>
 */
public class AmateurPhotographySystem {

    // ── Schedule constants ─────────────────────────────────────────────────

    /** Day within a 14-day cycle on which the meeting is held (day 12). */
    public static final int MEETING_DAY_OF_CYCLE = 12;

    /** Length of the repeating meeting cycle in days. */
    public static final int MEETING_CYCLE_DAYS = 14;

    /** Meeting opens (19:00). */
    public static final float MEETING_OPEN_HOUR = 19.0f;

    /** Meeting closes (21:00). */
    public static final float MEETING_CLOSE_HOUR = 21.0f;

    /** Entry deadline — must hand photo to Norman before this hour (19:00). */
    public static final float ENTRY_DEADLINE_HOUR = 19.0f;

    /** Judging starts (19:30). */
    public static final float JUDGING_START_HOUR = 19.5f;

    /** Judging ends (20:00). */
    public static final float JUDGING_END_HOUR = 20.0f;

    /** Darkroom heist window opens (20:30). */
    public static final float HEIST_WINDOW_OPEN_HOUR = 20.5f;

    // ── Photography quality constants ──────────────────────────────────────

    /** Number of charges on a fresh DISPOSABLE_CAMERA. */
    public static final int CAMERA_CHARGES = 3;

    /** Quality bonus for photographing a BIRD NPC. */
    public static final int SUBJECT_BONUS_BIRD = 30;

    /** Quality bonus for photographing a DOG NPC. */
    public static final int SUBJECT_BONUS_DOG = 25;

    /** Quality bonus during golden hour (07:00–09:00). */
    public static final int GOLDEN_HOUR_BONUS = 20;

    /** Golden hour start time. */
    public static final float GOLDEN_HOUR_START = 7.0f;

    /** Golden hour end time. */
    public static final float GOLDEN_HOUR_END = 9.0f;

    /** Quality bonus for DRIZZLE weather. */
    public static final int WEATHER_DRIZZLE_BONUS = 10;

    /** Quality penalty for THUNDERSTORM weather. */
    public static final int WEATHER_THUNDERSTORM_PENALTY = -10;

    /** Quality bonus for self-developing in Norman's darkroom. */
    public static final int SELF_DEVELOP_BONUS = 10;

    /** Base photo quality before modifiers. */
    public static final int BASE_PHOTO_QUALITY = 20;

    /** Maximum photo quality score. */
    public static final int MAX_PHOTO_QUALITY = 100;

    // ── Competition constants ──────────────────────────────────────────────

    /** Derek's fixed competition score — always 72 (Councillor's nephew). */
    public static final int DEREK_FIXED_SCORE = 72;

    /** Prize money for winning the competition legitimately. */
    public static final int WIN_PRIZE_COIN = 8;

    /** Runner-up threshold (50–72 = runner-up). */
    public static final int RUNNER_UP_MIN_SCORE = 50;

    // ── Rigging constants ──────────────────────────────────────────────────

    /** Cost to bribe Norman pre-judging. */
    public static final int BRIBE_COST = 12;

    /** Probability (0–1) that a bribery attempt succeeds. */
    public static final float BRIBE_SUCCESS_PROBABILITY = 0.50f;

    /** Score bonus added to the player's total on successful bribe of Norman. */
    public static final int BRIBE_SCORE_BONUS = 25;

    /** Notoriety gain for a failed bribe. */
    public static final int BRIBE_FAIL_NOTORIETY = 5;

    /** Notoriety gain for a witnessed submission swap. */
    public static final int SWAP_WITNESSED_NOTORIETY = 4;

    /** Witness radius for detecting submission swap (blocks). */
    public static final float SWAP_WITNESS_RADIUS = 5.0f;

    // ── Planning document heist constants ─────────────────────────────────

    /** Fence value for the PLANNING_APPLICATION_DOCUMENT. */
    public static final int DOCUMENT_FENCE_VALUE = 15;

    /** Council respect delta when tipping off newspaper. */
    public static final int DOCUMENT_COUNCIL_RESPECT_HIT = -20;

    /** Neighbourhood Vibes bonus when tipping off newspaper. */
    public static final int DOCUMENT_VIBES_BONUS = 10;

    /** Street Lads respect bonus for giving document to Street Lads. */
    public static final int DOCUMENT_STREET_LADS_RESPECT = 15;

    // ── Result enums ───────────────────────────────────────────────────────

    /** Result codes for taking a photograph. */
    public enum PhotoResult {
        SUCCESS,
        NO_CAMERA,
        NO_CAMERA_CHARGES,
        NO_SUBJECT_IN_RANGE
    }

    /** Result codes for developing a photograph at the chemist. */
    public enum DevelopResult {
        SUCCESS,
        NO_CAMERA,
        INSUFFICIENT_FUNDS,
        ALREADY_DEVELOPING
    }

    /** Result codes for entering the competition. */
    public enum EntryResult {
        SUCCESS,
        MEETING_NOT_ACTIVE,
        ENTRY_WINDOW_CLOSED,
        NO_PHOTOGRAPH,
        ALREADY_ENTERED,
        NORMAN_NOT_PRESENT
    }

    /** Judging outcome for the player's photograph. */
    public enum JudgingPlacement {
        WINNER,
        RUNNER_UP,
        UNPLACED,
        DID_NOT_ENTER
    }

    /** Result codes for bribing Norman. */
    public enum BribeResult {
        SUCCESS,
        FAILED_CAUGHT,
        INSUFFICIENT_FUNDS,
        MEETING_NOT_ACTIVE,
        JUDGING_ALREADY_STARTED,
        NORMAN_NOT_PRESENT
    }

    /** Result codes for swapping Derek's submission. */
    public enum SwapResult {
        SUCCESS_UNWITNESSED,
        SUCCESS_WITNESSED,
        JUDGING_ALREADY_STARTED,
        MEETING_NOT_ACTIVE,
        ALREADY_SWAPPED
    }

    /** Result codes for the darkroom heist (stealing the planning document). */
    public enum HeistResult {
        SUCCESS,
        OUTSIDE_HEIST_WINDOW,
        DARKROOM_LOCKED,
        ALREADY_LOOTED,
        WITNESSED
    }

    /** How the player disposed of the planning document. */
    public enum DocumentDisposal {
        SELL_TO_FENCE,
        TIP_TO_NEWSPAPER,
        GIVE_TO_STREET_LADS
    }

    // ── State ──────────────────────────────────────────────────────────────

    private final Random random;

    /** Day of the last meeting we initialised state for. */
    private int lastMeetingDay = -1;

    /** Current charges remaining on the player's camera (-1 = no camera). */
    private int cameraCharges = -1;

    /** The quality score of the player's developed photograph (0 = no photo). */
    private int playerPhotoQuality = 0;

    /** Whether the player has entered the competition this meeting. */
    private boolean playerEntered = false;

    /** Whether judging has been resolved this meeting. */
    private boolean judgingComplete = false;

    /** Whether the planning document has been stolen this meeting. */
    private boolean documentLooted = false;

    /** Whether Derek's submission has been swapped this meeting. */
    private boolean derekSwapped = false;

    /** Bribe bonus applied to the player's score (0 if not bribed). */
    private int bribeBonus = 0;

    /** Whether the player bribed Norman (for achievement tracking). */
    private boolean playerBribed = false;

    /** Whether a CANDID_WINNER achievement has been awarded this meeting. */
    private boolean candidWinnerAwarded = false;

    /** Whether DARKROOM_SABOTEUR achievement has been awarded this meeting. */
    private boolean darkroomSaboteurAwarded = false;

    /** Whether BENT_LENS achievement has been awarded this meeting. */
    private boolean bentLensAwarded = false;

    /** Whether CIVIC_HERO achievement has been awarded this session. */
    private boolean civicHeroAwarded = false;

    /** Whether the darkroom has been unlocked (player has opened the door). */
    private boolean darkroomUnlocked = false;

    // ── Construction ──────────────────────────────────────────────────────

    public AmateurPhotographySystem() {
        this(new Random());
    }

    public AmateurPhotographySystem(Random random) {
        this.random = random;
    }

    // ── Schedule ──────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the photography club meeting is active on the given
     * day and time (day 12 of 14-day cycle, 19:00–21:00).
     */
    public boolean isMeetingActive(TimeSystem timeSystem) {
        int day = timeSystem.getDayCount();
        float hour = timeSystem.getTime();
        return (day % MEETING_CYCLE_DAYS == MEETING_DAY_OF_CYCLE)
                && hour >= MEETING_OPEN_HOUR
                && hour < MEETING_CLOSE_HOUR;
    }

    /**
     * Returns {@code true} if the entry window is open (before 19:00 on meeting day,
     * before judging begins).
     */
    public boolean isEntryWindowOpen(TimeSystem timeSystem) {
        int day = timeSystem.getDayCount();
        float hour = timeSystem.getTime();
        return (day % MEETING_CYCLE_DAYS == MEETING_DAY_OF_CYCLE)
                && hour < ENTRY_DEADLINE_HOUR;
    }

    /**
     * Returns {@code true} if judging is currently in progress (19:30–20:00).
     */
    public boolean isJudgingInProgress(TimeSystem timeSystem) {
        float hour = timeSystem.getTime();
        return isMeetingActive(timeSystem)
                && hour >= JUDGING_START_HOUR
                && hour < JUDGING_END_HOUR;
    }

    /**
     * Returns {@code true} if the heist window is open (20:30–21:00).
     */
    public boolean isHeistWindowOpen(TimeSystem timeSystem) {
        float hour = timeSystem.getTime();
        return isMeetingActive(timeSystem)
                && hour >= HEIST_WINDOW_OPEN_HOUR
                && hour < MEETING_CLOSE_HOUR;
    }

    // ── Per-frame update ───────────────────────────────────────────────────

    /**
     * Call once per frame to advance the photography club simulation.
     *
     * <p>Handles automatic judging when the judging window starts (if player entered)
     * and resets state at the start of each new meeting day.
     *
     * @param delta               seconds since last frame
     * @param timeSystem          game time
     * @param inventory           player inventory
     * @param allNpcs             all NPCs in the world
     * @param achievementCallback achievement callback (may be null)
     * @param notorietySystem     for notoriety tracking (may be null)
     * @param rumourNetwork       for seeding rumours (may be null)
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       Inventory inventory,
                       List<NPC> allNpcs,
                       NotorietySystem.AchievementCallback achievementCallback,
                       NotorietySystem notorietySystem,
                       RumourNetwork rumourNetwork) {

        int day = timeSystem.getDayCount();

        // Reset state when a new meeting day begins
        if (day % MEETING_CYCLE_DAYS == MEETING_DAY_OF_CYCLE && day != lastMeetingDay) {
            resetMeetingState(day);
        }

        // Trigger judging automatically when judging window opens
        if (!judgingComplete && playerEntered && isJudgingInProgress(timeSystem)) {
            resolveJudging(inventory, allNpcs, achievementCallback, notorietySystem, rumourNetwork);
        }
    }

    // ── Mechanic 1: Photography ───────────────────────────────────────────

    /**
     * Give the player a disposable camera (or refill charges if they already have one).
     * Call this when purchasing from the newsagent or finding one via skip diving.
     *
     * @param inventory player inventory (DISPOSABLE_CAMERA will be added)
     */
    public void giveCameraToPlayer(Inventory inventory) {
        inventory.addItem(Material.DISPOSABLE_CAMERA, 1);
        cameraCharges = CAMERA_CHARGES;
    }

    /**
     * Calculate the photo quality score for a given subject, time, and weather.
     *
     * <p>Quality breakdown:
     * <ul>
     *   <li>Base: {@link #BASE_PHOTO_QUALITY}</li>
     *   <li>Subject: +30 for BIRD, +25 for DOG</li>
     *   <li>Golden hour (07:00–09:00): +20</li>
     *   <li>Weather: +10 DRIZZLE, −10 THUNDERSTORM</li>
     *   <li>Self-development (Norman's darkroom): +10</li>
     * </ul>
     *
     * @param subjectType  type of the NPC being photographed, or null for scenic prop
     * @param timeSystem   for golden hour check
     * @param weatherSystem for weather modifier
     * @param selfDeveloped true if developed in Norman's darkroom
     * @return quality score clamped to [0, MAX_PHOTO_QUALITY]
     */
    public int calculatePhotoQuality(NPCType subjectType,
                                      TimeSystem timeSystem,
                                      WeatherSystem weatherSystem,
                                      boolean selfDeveloped) {
        int quality = BASE_PHOTO_QUALITY;

        // Subject bonus
        if (subjectType == NPCType.BIRD) {
            quality += SUBJECT_BONUS_BIRD;
        } else if (subjectType == NPCType.DOG) {
            quality += SUBJECT_BONUS_DOG;
        }

        // Golden hour bonus (07:00–09:00)
        if (timeSystem != null) {
            float hour = timeSystem.getTime();
            if (hour >= GOLDEN_HOUR_START && hour < GOLDEN_HOUR_END) {
                quality += GOLDEN_HOUR_BONUS;
            }
        }

        // Weather modifier
        if (weatherSystem != null) {
            Weather weather = weatherSystem.getCurrentWeather();
            if (weather == Weather.DRIZZLE) {
                quality += WEATHER_DRIZZLE_BONUS;
            } else if (weather == Weather.THUNDERSTORM) {
                quality += WEATHER_THUNDERSTORM_PENALTY;
            }
        }

        // Self-development bonus
        if (selfDeveloped) {
            quality += SELF_DEVELOP_BONUS;
        }

        return Math.max(0, Math.min(MAX_PHOTO_QUALITY, quality));
    }

    /**
     * Take a photograph using the player's DISPOSABLE_CAMERA.
     *
     * <p>Consumes one camera charge. The photograph is not yet developed; call
     * {@link #developAtChemist} or open Norman's darkroom to develop.
     *
     * @param subjectType  type of NPC being photographed (or null for prop)
     * @param timeSystem   for golden hour calculation
     * @param weatherSystem for weather bonus
     * @param inventory    player inventory (camera must be present)
     * @return PhotoResult describing outcome
     */
    public PhotoResult takePhotograph(NPCType subjectType,
                                       TimeSystem timeSystem,
                                       WeatherSystem weatherSystem,
                                       Inventory inventory) {
        if (inventory.getItemCount(Material.DISPOSABLE_CAMERA) < 1) {
            return PhotoResult.NO_CAMERA;
        }
        if (cameraCharges <= 0) {
            return PhotoResult.NO_CAMERA_CHARGES;
        }
        if (subjectType == null) {
            return PhotoResult.NO_SUBJECT_IN_RANGE;
        }

        // Consume one charge
        cameraCharges--;
        if (cameraCharges <= 0) {
            // Camera exhausted — remove it
            inventory.removeItem(Material.DISPOSABLE_CAMERA, 1);
            cameraCharges = 0;
        }

        // Store quality for later development (chemist default = no self-develop bonus)
        playerPhotoQuality = calculatePhotoQuality(subjectType, timeSystem, weatherSystem, false);

        return PhotoResult.SUCCESS;
    }

    /**
     * Develop the photograph at the chemist for 2 COIN (takes 1 in-game hour).
     *
     * <p>On success, adds {@code DEVELOPED_PHOTOGRAPH} to inventory and deducts 2 COIN.
     * Does not apply the self-development bonus.
     *
     * @param inventory player inventory (2 COIN deducted, DEVELOPED_PHOTOGRAPH added)
     * @return DevelopResult describing outcome
     */
    public DevelopResult developAtChemist(Inventory inventory) {
        if (playerPhotoQuality <= 0) {
            return DevelopResult.NO_CAMERA;
        }
        if (inventory.getItemCount(Material.COIN) < CHEMIST_DEVELOP_COST) {
            return DevelopResult.INSUFFICIENT_FUNDS;
        }

        inventory.removeItem(Material.COIN, CHEMIST_DEVELOP_COST);
        inventory.addItem(Material.DEVELOPED_PHOTOGRAPH, 1);

        return DevelopResult.SUCCESS;
    }

    /** Cost to develop at the chemist (2 COIN). */
    public static final int CHEMIST_DEVELOP_COST = 2;

    /**
     * Self-develop in Norman's darkroom (adds +10 quality bonus).
     *
     * <p>On success, adds {@code DEVELOPED_PHOTOGRAPH} with the enhanced quality score
     * to inventory.
     *
     * @param timeSystem for bonus calculation
     * @param weatherSystem for bonus calculation
     * @param originalSubjectType subject NPCType from when the photo was taken
     * @param inventory player inventory (DEVELOPED_PHOTOGRAPH added)
     * @return true if development succeeded; false if darkroom is locked or no pending photo
     */
    public boolean selfDevelopInDarkroom(NPCType originalSubjectType,
                                          TimeSystem timeSystem,
                                          WeatherSystem weatherSystem,
                                          Inventory inventory) {
        if (!darkroomUnlocked) return false;
        if (playerPhotoQuality <= 0) return false;

        // Recalculate with self-develop bonus
        int enhanced = calculatePhotoQuality(originalSubjectType, timeSystem, weatherSystem, true);
        playerPhotoQuality = enhanced;
        inventory.addItem(Material.DEVELOPED_PHOTOGRAPH, 1);
        return true;
    }

    // ── Mechanic 2: Competition entry ────────────────────────────────────

    /**
     * Enter the photography competition by handing the print to Norman.
     *
     * <p>Requirements: entry window open (before 19:00 on meeting day), player has a
     * {@code DEVELOPED_PHOTOGRAPH}, Norman is present, and player hasn't already entered.
     *
     * @param timeSystem   for schedule check
     * @param normanNearby true if the PHOTO_CLUB_CHAIR NPC is within interaction range
     * @param inventory    player inventory (DEVELOPED_PHOTOGRAPH consumed on success)
     * @return EntryResult describing outcome
     */
    public EntryResult enterCompetition(TimeSystem timeSystem,
                                         boolean normanNearby,
                                         Inventory inventory) {
        if (!isEntryWindowOpen(timeSystem)) {
            if (isMeetingActive(timeSystem)) {
                return EntryResult.ENTRY_WINDOW_CLOSED;
            }
            return EntryResult.MEETING_NOT_ACTIVE;
        }
        if (!normanNearby) return EntryResult.NORMAN_NOT_PRESENT;
        if (playerEntered) return EntryResult.ALREADY_ENTERED;
        if (inventory.getItemCount(Material.DEVELOPED_PHOTOGRAPH) < 1) {
            return EntryResult.NO_PHOTOGRAPH;
        }

        inventory.removeItem(Material.DEVELOPED_PHOTOGRAPH, 1);
        playerEntered = true;
        return EntryResult.SUCCESS;
    }

    // ── Mechanic 2: Judging ───────────────────────────────────────────────

    /**
     * Resolve the competition judging.
     *
     * <p>Player's total score = {@link #playerPhotoQuality} + {@link #bribeBonus}.
     * <ul>
     *   <li>Score &gt; {@link #DEREK_FIXED_SCORE} (72): {@code WINNER} — 8 COIN +
     *       {@code WINNERS_CERTIFICATE_PROP}; {@code CANDID_WINNER} if not bribed.</li>
     *   <li>Score 50–72: {@code RUNNER_UP}.</li>
     *   <li>Score &lt; 50: {@code UNPLACED}.</li>
     * </ul>
     *
     * @param inventory           player inventory (prize added on win)
     * @param allNpcs             for rumour seeding
     * @param achievementCallback for CANDID_WINNER / BENT_LENS
     * @param notorietySystem     (reserved)
     * @param rumourNetwork       for seeding post-judging rumours
     * @return JudgingPlacement
     */
    public JudgingPlacement resolveJudging(Inventory inventory,
                                            List<NPC> allNpcs,
                                            NotorietySystem.AchievementCallback achievementCallback,
                                            NotorietySystem notorietySystem,
                                            RumourNetwork rumourNetwork) {
        if (judgingComplete) return JudgingPlacement.DID_NOT_ENTER;
        judgingComplete = true;

        if (!playerEntered) {
            return JudgingPlacement.DID_NOT_ENTER;
        }

        // Derek's submission is zeroed if swapped; otherwise fixed 72
        int derekScore = derekSwapped ? 0 : DEREK_FIXED_SCORE;
        int playerTotal = playerPhotoQuality + bribeBonus;

        if (playerTotal > derekScore) {
            // WINNER
            if (inventory != null) {
                inventory.addItem(Material.COIN, WIN_PRIZE_COIN);
                inventory.addItem(Material.WINNERS_CERTIFICATE_PROP, 1);
            }
            // CANDID_WINNER only if no bribery involved
            if (!playerBribed && !candidWinnerAwarded && achievementCallback != null) {
                candidWinnerAwarded = true;
                achievementCallback.award(AchievementType.CANDID_WINNER);
            }
            // BENT_LENS if won via bribe
            if (playerBribed && !bentLensAwarded && achievementCallback != null) {
                bentLensAwarded = true;
                achievementCallback.award(AchievementType.BENT_LENS);
            }
            seedRumour(rumourNetwork, allNpcs, RumourType.COMMUNITY_WIN,
                    "Someone finally beat Derek at the photo club. Norman looked mortified.");
            return JudgingPlacement.WINNER;
        } else if (playerTotal >= RUNNER_UP_MIN_SCORE) {
            return JudgingPlacement.RUNNER_UP;
        } else {
            return JudgingPlacement.UNPLACED;
        }
    }

    // ── Mechanic 3a: Swap Derek's submission ──────────────────────────────

    /**
     * Attempt to swap Derek's submission in the {@code SUBMISSION_BOX_PROP} before
     * members arrive.
     *
     * <p>Unwitnessed: no crime, {@code DARKROOM_SABOTEUR} achievement, Derek's score → 0.
     * Witnessed: {@code PETTY_THEFT} crime + Notoriety +4 + WantedLevel +1.
     * Either way, Derek is effectively eliminated from judging.
     *
     * @param timeSystem          for schedule check (must be before judging starts)
     * @param witnessed           true if any NPC within {@link #SWAP_WITNESS_RADIUS} blocks
     * @param achievementCallback for DARKROOM_SABOTEUR
     * @param criminalRecord      for PETTY_THEFT on witnessed swap
     * @param notorietySystem     for +4 notoriety on witnessed swap
     * @param wantedSystem        for +1 wanted star on witnessed swap
     * @param achievementCb       same as achievementCallback (reused)
     * @return SwapResult describing outcome
     */
    public SwapResult swapDerekSubmission(TimeSystem timeSystem,
                                           boolean witnessed,
                                           NotorietySystem.AchievementCallback achievementCallback,
                                           CriminalRecord criminalRecord,
                                           NotorietySystem notorietySystem,
                                           WantedSystem wantedSystem) {
        // Meeting must be on this day; submission box accessible before judging
        int day = timeSystem.getDayCount();
        if (day % MEETING_CYCLE_DAYS != MEETING_DAY_OF_CYCLE) {
            return SwapResult.MEETING_NOT_ACTIVE;
        }
        if (isJudgingInProgress(timeSystem) || judgingComplete) {
            return SwapResult.JUDGING_ALREADY_STARTED;
        }
        if (derekSwapped) return SwapResult.ALREADY_SWAPPED;

        derekSwapped = true;

        if (witnessed) {
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.PETTY_THEFT);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(SWAP_WITNESSED_NOTORIETY, achievementCallback);
            }
            if (wantedSystem != null) {
                wantedSystem.increaseWantedStars(1);
            }
            return SwapResult.SUCCESS_WITNESSED;
        }

        // Unwitnessed — no crime
        if (!darkroomSaboteurAwarded && achievementCallback != null) {
            darkroomSaboteurAwarded = true;
            achievementCallback.award(AchievementType.DARKROOM_SABOTEUR);
        }
        return SwapResult.SUCCESS_UNWITNESSED;
    }

    // ── Mechanic 3b: Bribe Norman ─────────────────────────────────────────

    /**
     * Attempt to bribe Norman pre-judging to add +25 to the player's photo score.
     *
     * <p>50% chance of success. On failure: {@code BRIBERY} crime, WantedSystem +1,
     * Notoriety +5, {@code PHOTO_CLUB_RIGGED} rumour seeded.
     * On success: bribe bonus +25, player flagged as bribed (BENT_LENS eligible).
     *
     * @param timeSystem          for schedule check
     * @param inventory           12 COIN deducted on attempt
     * @param normanNearby        true if PHOTO_CLUB_CHAIR within interaction range
     * @param allNpcs             for rumour seeding
     * @param achievementCallback (reserved; BENT_LENS awarded at judging)
     * @param criminalRecord      for BRIBERY on failure
     * @param wantedSystem        for +1 wanted star on failure
     * @param notorietySystem     for +5 notoriety on failure
     * @param rumourNetwork       for PHOTO_CLUB_RIGGED on failure
     * @return BribeResult describing outcome
     */
    public BribeResult bribeNorman(TimeSystem timeSystem,
                                    Inventory inventory,
                                    boolean normanNearby,
                                    List<NPC> allNpcs,
                                    NotorietySystem.AchievementCallback achievementCallback,
                                    CriminalRecord criminalRecord,
                                    WantedSystem wantedSystem,
                                    NotorietySystem notorietySystem,
                                    RumourNetwork rumourNetwork) {

        if (!isMeetingActive(timeSystem) && !isEntryWindowOpen(timeSystem)) {
            return BribeResult.MEETING_NOT_ACTIVE;
        }
        if (isJudgingInProgress(timeSystem) || judgingComplete) {
            return BribeResult.JUDGING_ALREADY_STARTED;
        }
        if (!normanNearby) return BribeResult.NORMAN_NOT_PRESENT;
        if (inventory.getItemCount(Material.COIN) < BRIBE_COST) {
            return BribeResult.INSUFFICIENT_FUNDS;
        }

        inventory.removeItem(Material.COIN, BRIBE_COST);

        boolean success = random.nextFloat() < BRIBE_SUCCESS_PROBABILITY;

        if (success) {
            bribeBonus = BRIBE_SCORE_BONUS;
            playerBribed = true;
            return BribeResult.SUCCESS;
        } else {
            // Caught — record crime, notoriety, wanted star, seed rumour
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.BRIBERY);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(BRIBE_FAIL_NOTORIETY, achievementCallback);
            }
            if (wantedSystem != null) {
                wantedSystem.increaseWantedStars(1);
            }
            seedRumour(rumourNetwork, allNpcs, RumourType.PHOTO_CLUB_RIGGED,
                    "Derek won again. Norman's in the council's pocket. That competition's been rigged for years.");
            return BribeResult.FAILED_CAUGHT;
        }
    }

    // ── Mechanic 4: Darkroom break-in ────────────────────────────────────

    /**
     * Unlock the darkroom using a LOCKPICK (8-second hold-E) or the DARKROOM_KEY.
     *
     * @param inventory player inventory (LOCKPICK consumed if used; DARKROOM_KEY preferred)
     * @return true if darkroom is now unlocked; false if no key/lockpick available
     */
    public boolean unlockDarkroom(Inventory inventory) {
        if (darkroomUnlocked) return true;

        if (inventory.getItemCount(Material.DARKROOM_KEY) > 0) {
            darkroomUnlocked = true;
            return true;
        }
        if (inventory.getItemCount(Material.LOCKPICK) > 0) {
            inventory.removeItem(Material.LOCKPICK, 1);
            darkroomUnlocked = true;
            return true;
        }
        return false;
    }

    /**
     * Steal the {@code PLANNING_APPLICATION_DOCUMENT} from Norman's darkroom during
     * the heist window (20:30–21:00).
     *
     * <p>Unwitnessed = no crime, document added to inventory.
     * Witnessed = {@code PLANNING_DOCUMENT_THEFT} crime + Notoriety +5 + WantedSystem +1.
     *
     * @param timeSystem          for heist window check
     * @param witnessed           true if an NPC witnesses the theft
     * @param inventory           document added on success
     * @param achievementCallback (reserved)
     * @param criminalRecord      for PLANNING_DOCUMENT_THEFT on witnessed theft
     * @param notorietySystem     for +5 notoriety on witnessed theft
     * @param wantedSystem        for +1 wanted star on witnessed theft
     * @return HeistResult describing outcome
     */
    public HeistResult stealPlanningDocument(TimeSystem timeSystem,
                                              boolean witnessed,
                                              Inventory inventory,
                                              NotorietySystem.AchievementCallback achievementCallback,
                                              CriminalRecord criminalRecord,
                                              NotorietySystem notorietySystem,
                                              WantedSystem wantedSystem) {
        if (!isHeistWindowOpen(timeSystem)) return HeistResult.OUTSIDE_HEIST_WINDOW;
        if (!darkroomUnlocked) return HeistResult.DARKROOM_LOCKED;
        if (documentLooted) return HeistResult.ALREADY_LOOTED;

        if (witnessed) {
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.PLANNING_DOCUMENT_THEFT);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(5, achievementCallback);
            }
            if (wantedSystem != null) {
                wantedSystem.increaseWantedStars(1);
            }
            return HeistResult.WITNESSED;
        }

        // Unwitnessed — no crime
        documentLooted = true;
        inventory.addItem(Material.PLANNING_APPLICATION_DOCUMENT, 1);
        return HeistResult.SUCCESS;
    }

    /**
     * Dispose of the planning document.
     *
     * <p>Effects by disposal path:
     * <ul>
     *   <li>{@code SELL_TO_FENCE}: +15 COIN, document removed.</li>
     *   <li>{@code TIP_TO_NEWSPAPER}: Council Respect −20, Vibes +10,
     *       {@code BANDSTAND_UNDER_THREAT} rumour, {@code CIVIC_HERO} achievement.</li>
     *   <li>{@code GIVE_TO_STREET_LADS}: Street Lads Respect +15.</li>
     * </ul>
     *
     * @param disposal            how to dispose of the document
     * @param inventory           PLANNING_APPLICATION_DOCUMENT removed + COIN added on fence sale
     * @param allNpcs             for rumour seeding
     * @param achievementCallback for CIVIC_HERO
     * @param factionSystem       for Council/Street Lads respect changes
     * @param neighbourhoodSystem for Vibes bonus
     * @param rumourNetwork       for BANDSTAND_UNDER_THREAT
     * @return true if disposal succeeded; false if player doesn't have the document
     */
    public boolean disposeDocument(DocumentDisposal disposal,
                                    Inventory inventory,
                                    List<NPC> allNpcs,
                                    NotorietySystem.AchievementCallback achievementCallback,
                                    FactionSystem factionSystem,
                                    NeighbourhoodSystem neighbourhoodSystem,
                                    RumourNetwork rumourNetwork) {
        if (inventory.getItemCount(Material.PLANNING_APPLICATION_DOCUMENT) < 1) return false;

        inventory.removeItem(Material.PLANNING_APPLICATION_DOCUMENT, 1);

        switch (disposal) {
            case SELL_TO_FENCE:
                inventory.addItem(Material.COIN, DOCUMENT_FENCE_VALUE);
                break;

            case TIP_TO_NEWSPAPER:
                if (factionSystem != null) {
                    factionSystem.applyRespectDelta(Faction.THE_COUNCIL, DOCUMENT_COUNCIL_RESPECT_HIT);
                }
                if (neighbourhoodSystem != null) {
                    neighbourhoodSystem.addVibes(DOCUMENT_VIBES_BONUS);
                }
                seedRumour(rumourNetwork, allNpcs, RumourType.BANDSTAND_UNDER_THREAT,
                        "Council want to knock down the bandstand for a car park. "
                        + "Got the paperwork to prove it.");
                if (!civicHeroAwarded && achievementCallback != null) {
                    civicHeroAwarded = true;
                    achievementCallback.award(AchievementType.CIVIC_HERO);
                }
                break;

            case GIVE_TO_STREET_LADS:
                if (factionSystem != null) {
                    factionSystem.applyRespectDelta(Faction.STREET_LADS, DOCUMENT_STREET_LADS_RESPECT);
                }
                break;

            default:
                break;
        }

        return true;
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    /** @return true if the player has entered the current meeting's competition. */
    public boolean isPlayerEntered() { return playerEntered; }

    /** @return true if judging has been resolved for the current meeting. */
    public boolean isJudgingComplete() { return judgingComplete; }

    /** @return true if the planning document has been looted this meeting. */
    public boolean isDocumentLooted() { return documentLooted; }

    /** @return true if Derek's submission has been swapped this meeting. */
    public boolean isDerekSwapped() { return derekSwapped; }

    /** @return the player's current photo quality score. */
    public int getPlayerPhotoQuality() { return playerPhotoQuality; }

    /** @return the bribe bonus applied to the player's score. */
    public int getBribeBonus() { return bribeBonus; }

    /** @return the player's total competition score (photo quality + bribe bonus). */
    public int getPlayerTotalScore() { return playerPhotoQuality + bribeBonus; }

    /** @return true if the player bribed Norman this meeting. */
    public boolean isPlayerBribed() { return playerBribed; }

    /** @return true if the darkroom has been unlocked. */
    public boolean isDarkroomUnlocked() { return darkroomUnlocked; }

    /** @return the remaining charges on the player's camera. */
    public int getCameraCharges() { return cameraCharges; }

    // ── Testing helpers ────────────────────────────────────────────────────

    /** Force-set player entered status for testing. */
    public void setPlayerEnteredForTesting(boolean entered) {
        this.playerEntered = entered;
    }

    /** Force-set the player photo quality for testing. */
    public void setPlayerPhotoQualityForTesting(int quality) {
        this.playerPhotoQuality = quality;
    }

    /** Force-set the bribe bonus for testing. */
    public void setBribeBonusForTesting(int bonus) {
        this.bribeBonus = bonus;
    }

    /** Force-set darkroom unlocked state for testing. */
    public void setDarkroomUnlockedForTesting(boolean unlocked) {
        this.darkroomUnlocked = unlocked;
    }

    /** Force-set camera charges for testing. */
    public void setCameraChargesForTesting(int charges) {
        this.cameraCharges = charges;
    }

    /** Force-set Derek swapped state for testing. */
    public void setDerekSwappedForTesting(boolean swapped) {
        this.derekSwapped = swapped;
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private void resetMeetingState(int day) {
        lastMeetingDay = day;
        playerEntered = false;
        judgingComplete = false;
        documentLooted = false;
        derekSwapped = false;
        playerPhotoQuality = 0;
        bribeBonus = 0;
        playerBribed = false;
        darkroomUnlocked = false;
        candidWinnerAwarded = false;
        darkroomSaboteurAwarded = false;
        bentLensAwarded = false;
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

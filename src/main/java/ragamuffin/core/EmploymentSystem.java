package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Issue #1333: Northfield Employment System — Job Interviews, Wages,
 * Getting the Sack &amp; the Forged Reference.
 *
 * <h3>Overview</h3>
 * Provides a legitimate employment loop connecting to five local employers
 * (Greggs, Iceland, Corner Shop, Greasy Spoon, Charity Shop).  The player
 * applies for a job, attends a short multiple-choice interview with the
 * manager NPC, then works 3 shifts per in-game week.  Employment suspends UC
 * payments (via {@link DWPSystem}); failing to disclose to the DWP earns a
 * {@link CrimeType#BENEFIT_FRAUD} entry after 2 sign-on cycles.
 *
 * <h3>Mechanic 1 — Applying</h3>
 * Player presses E on a {@code JOB_VACANCY_BOARD_PROP} to receive a
 * {@link Material#JOB_APPLICATION_FORM}.  They then present it to the
 * employer's interview NPC during business hours.
 *
 * <h3>Mechanic 2 — Interview</h3>
 * 3-question dialogue (experience, reliability, criminal record).  Score 0–3
 * maps to rejection / 50% chance / offer.  A {@link Material#FORGED_REFERENCE}
 * adds +2 but carries a 30% detection risk (−10% per FORGERY level above
 * Apprentice): detected → {@link CrimeType#FRAUD} + Notoriety +5 + permanent
 * blacklist.
 *
 * <h3>Mechanic 3 — Shifts</h3>
 * 3 shifts/week (Mon/Wed/Fri) at employer-specific hours.  Player wears
 * {@link Material#STAFF_ID_BADGE}, clocks in at {@code STAFF_CLOCK_IN_PROP},
 * presses E on {@code STOCK_CRATE_PROP} every 2 in-game minutes, then clocks
 * out.  Wages paid immediately as COIN.  Late (&gt;15 min) = verbal warning;
 * 3 warnings = {@link DismissalReason#TOO_MANY_WARNINGS}.
 *
 * <h3>Mechanic 4 — Getting the Sack</h3>
 * Instant dismissal for theft during shift ({@link CrimeType#THEFT_FROM_EMPLOYER},
 * Notoriety +4), 3 late arrivals, 2 missed shifts, or notoriety exceeding the
 * employer threshold mid-employment.
 *
 * <h3>Mechanic 5 — DWP Integration</h3>
 * While employed, {@link DWPSystem#setUCClaimActive(boolean)} is set false.
 * Claiming UC without disclosure → {@link CrimeType#BENEFIT_FRAUD} after 2 cycles.
 *
 * <h3>Mechanic 6 — Perks</h3>
 * Greggs: 50% discount + sausage-roll temptation.
 * Iceland: 1 free FROZEN_PIZZA/shift; power cut cancels with half wages.
 * Charity Shop: no wages, Notoriety −1/shift, Community Respect +2,
 * {@link RumourType#COMMUNITY_SPIRIT} seeded.
 */
public class EmploymentSystem {

    // ── Employer definitions ──────────────────────────────────────────────────

    /**
     * The five employers available in Northfield.
     */
    public enum Employer {
        GREGGS(LandmarkType.GREGGS,       4,  20, NPCType.GREGGS_MANAGER,         6.0f,  9.0f),
        ICELAND(LandmarkType.ICELAND,     5,  30, NPCType.ICELAND_MANAGER,        9.0f,  13.0f),
        CORNER_SHOP(LandmarkType.CORNER_SHOP, 3, 100, NPCType.CORNER_SHOP_OWNER,  14.0f, 18.0f),
        GREASY_SPOON(LandmarkType.GREASY_SPOON, 3, 100, NPCType.GREASY_SPOON_OWNER, 7.0f, 11.0f),
        CHARITY_SHOP(LandmarkType.CHARITY_SHOP, 0, 0, NPCType.CHARITY_VOLUNTEER_LEADER, 10.0f, 14.0f);

        /** Landmark this employer occupies. */
        public final LandmarkType landmark;
        /** Coin paid per completed shift (0 for Charity Shop). */
        public final int wagePerShift;
        /** Maximum Notoriety allowed for application/employment (100 = no limit). */
        public final int notorietyThreshold;
        /** NPC type for the interview and shift supervision. */
        public final NPCType interviewNpc;
        /** Shift start time (in-game hour, e.g. 6.0 = 06:00). */
        public final float shiftStartHour;
        /** Shift end time (in-game hour). */
        public final float shiftEndHour;

        Employer(LandmarkType landmark, int wagePerShift, int notorietyThreshold,
                 NPCType interviewNpc, float shiftStartHour, float shiftEndHour) {
            this.landmark           = landmark;
            this.wagePerShift       = wagePerShift;
            this.notorietyThreshold = notorietyThreshold;
            this.interviewNpc       = interviewNpc;
            this.shiftStartHour     = shiftStartHour;
            this.shiftEndHour       = shiftEndHour;
        }
    }

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Result of {@link #applyForJob}. */
    public enum ApplicationResult {
        APPLICATION_FORM_ISSUED,
        ALREADY_EMPLOYED,
        NO_VACANCY,
        BLACKLISTED,
        NOTORIETY_TOO_HIGH
    }

    /** Result of {@link #conductInterview}. */
    public enum InterviewResult {
        JOB_OFFERED,
        REJECTED,
        FRAUD_DETECTED
    }

    /** Result of {@link #clockIn}. */
    public enum ClockInResult {
        CLOCKED_IN,
        LATE_WARNING,
        NO_BADGE,
        NO_ACTIVE_SHIFT,
        ALREADY_CLOCKED_IN
    }

    /** Result of {@link #clockOut}. */
    public enum ClockOutResult {
        SHIFT_COMPLETE,
        SKIVING_WARNING,
        WALKED_OUT,
        NOT_CLOCKED_IN
    }

    /** Reason a player was dismissed. */
    public enum DismissalReason {
        THEFT_DURING_SHIFT,
        TOO_MANY_WARNINGS,
        TOO_MANY_MISSED_SHIFTS,
        NOTORIETY_EXCEEDED,
        FIGHTING_ON_PREMISES
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Minutes past shift start after which arrival counts as late. */
    public static final float LATE_ARRIVAL_THRESHOLD_MINUTES = 15f;

    /** Number of verbal/written warnings before dismissal. */
    public static final int MAX_WARNINGS_BEFORE_DISMISSAL = 3;

    /** Number of missed shifts before dismissal. */
    public static final int MAX_MISSED_SHIFTS_BEFORE_DISMISSAL = 2;

    /** Number of tasks (E-presses on STOCK_CRATE_PROP) required per shift. */
    public static final int TASKS_REQUIRED_PER_SHIFT = 3;

    /** Missing this many tasks in a shift earns a SKIVING warning. */
    public static final int TASKS_SKIVING_THRESHOLD = 3;

    /** Notoriety added when dismissed for theft during shift. */
    public static final int THEFT_DURING_SHIFT_NOTORIETY = 4;

    /** Score bonus granted by FORGED_REFERENCE in interview. */
    public static final int FORGED_REFERENCE_SCORE_BONUS = 2;

    /** Base detection chance (0.0–1.0) for a FORGED_REFERENCE. */
    public static final float FORGED_REFERENCE_BASE_DETECTION = 0.30f;

    /** Reduction in detection chance per FORGERY level above Apprentice (0.0–1.0). */
    public static final float FORGED_REFERENCE_DETECTION_REDUCTION_PER_LEVEL = 0.10f;

    /** Notoriety added when FORGED_REFERENCE is detected. */
    public static final int FORGED_REFERENCE_CAUGHT_NOTORIETY = 5;

    /** In-game days before a non-theft blacklist entry expires. */
    public static final int BLACKLIST_COOLDOWN_DAYS = 7;

    /**
     * Permanent blacklist sentinel (theft/fraud — never expires).
     * Stored as cooldown days; Integer.MAX_VALUE means permanent.
     */
    public static final int BLACKLIST_PERMANENT = Integer.MAX_VALUE;

    /** In-game days after hire that DWP non-disclosure triggers BENEFIT_FRAUD. */
    public static final int DWP_DISCLOSURE_GRACE_CYCLES = 2;

    /** Notoriety reduction per completed Charity Shop volunteer shift. */
    public static final int CHARITY_SHIFT_NOTORIETY_REDUCTION = 1;

    /** Community Respect awarded per completed Charity Shop volunteer shift. */
    public static final int CHARITY_SHIFT_COMMUNITY_RESPECT = 2;

    /** Greggs discount factor for items purchased during a shift (50% off = 0.5). */
    public static final float GREGGS_SHIFT_DISCOUNT = 0.5f;

    /** Probability (0.0–1.0) that a Greggs shift tempts the player with a free sausage roll. */
    public static final float GREGGS_SAUSAGE_ROLL_TEMPTATION_CHANCE = 0.20f;

    /** Q3 criminal record entries: at this count or above, a lie is caught. */
    public static final int CRIMINAL_RECORD_SUSPICIOUS_THRESHOLD = 3;

    /** Q3 criminal record entries: at this count or above, rejected on the spot. */
    public static final int CRIMINAL_RECORD_REJECTION_THRESHOLD = 5;

    /** In-game days before a dismissed job board re-posts its vacancy. */
    public static final int VACANCY_REPOST_DAYS = 2;

    // ── Interview answer enums ────────────────────────────────────────────────

    /**
     * Player's answer to Q1 (experience).
     */
    public enum Q1Answer {
        YES_LOADS,      // lie: 30% catch chance
        NO_BUT_KEEN,    // honest, always passes
        SAY_NOTHING     // instant Q1 fail
    }

    /**
     * Player's answer to Q2 (reliability).
     */
    public enum Q2Answer {
        ABSOLUTELY,             // passes always
        DEPENDS_ON_MORNING,     // fails for strict employers (Greggs, Iceland)
        YAWN                    // always fails
    }

    /**
     * Player's answer to Q3 (criminal record).
     */
    public enum Q3Answer {
        NO,                     // lie: caught if CriminalRecord entries ≥ 3
        YES,                    // honest: rejection unless Corner Shop
        NONE_OF_YOUR_BUSINESS   // always rejection + Notoriety +1
    }

    // ── Internal state ────────────────────────────────────────────────────────

    private final Random random;

    /** Current employer, or null if unemployed. */
    private Employer currentEmployer;

    /** Day the player was hired (for DWP disclosure tracking). */
    private int hireDayCount;

    /** Whether player has disclosed employment to DWP. */
    private boolean dwpDisclosed;

    /**
     * Number of sign-on cycles that have elapsed since hire without DWP disclosure.
     * Accumulated by {@link #onSignOnCycle}.
     */
    private int undisclosedSignOnCycles;

    /** Number of verbal warnings at current employer. */
    private int warningCount;

    /** Number of missed shifts at current employer. */
    private int missedShiftCount;

    /** Number of consecutive shifts completed without any warning (for MODEL_EMPLOYEE). */
    private int consecutiveCleanShifts;

    /** Total dismissals across all employers (for HIRED_AND_FIRED achievement). */
    private int totalDismissals;

    /** Whether player is currently clocked in to a shift. */
    private boolean clockedIn;

    /** In-game hour when the player clocked in to the current shift. */
    private float clockInHour;

    /** Number of productivity tasks completed in the current shift. */
    private int tasksCompletedThisShift;

    /** Blacklist: employer → remaining blacklist cooldown days (Integer.MAX_VALUE = permanent). */
    private final Map<Employer, Integer> blacklist;

    /** Vacancy repost timer: employer → remaining in-game days until vacancy re-opens. */
    private final Map<Employer, Integer> vacancyRepostTimers;

    /** Whether a FIRST_DAY achievement has already been fired. */
    private boolean firstDayAchievementFired;

    // ── Constructor ───────────────────────────────────────────────────────────

    public EmploymentSystem(Random random) {
        this.random               = random;
        this.blacklist            = new EnumMap<>(Employer.class);
        this.vacancyRepostTimers  = new EnumMap<>(Employer.class);
    }

    // ── Mechanic 1: Applying ──────────────────────────────────────────────────

    /**
     * Player presses E on a {@code JOB_VACANCY_BOARD_PROP} outside {@code employer}.
     *
     * @param employer         the employer whose board was pressed
     * @param inventory        player's inventory (receives JOB_APPLICATION_FORM)
     * @param playerNotoriety  current player notoriety score
     * @param currentDay       current in-game day count
     * @return application result
     */
    public ApplicationResult applyForJob(Employer employer, Inventory inventory,
                                         int playerNotoriety, int currentDay) {
        // Already employed
        if (currentEmployer != null) {
            return ApplicationResult.ALREADY_EMPLOYED;
        }

        // Blacklisted
        if (isBlacklisted(employer, currentDay)) {
            return ApplicationResult.BLACKLISTED;
        }

        // Notoriety too high
        if (playerNotoriety >= employer.notorietyThreshold) {
            return ApplicationResult.NOTORIETY_TOO_HIGH;
        }

        // No vacancy (repost timer active)
        if (vacancyRepostTimers.containsKey(employer)) {
            return ApplicationResult.NO_VACANCY;
        }

        inventory.addItem(Material.JOB_APPLICATION_FORM, 1);
        return ApplicationResult.APPLICATION_FORM_ISSUED;
    }

    // ── Mechanic 2: Interview ─────────────────────────────────────────────────

    /**
     * Conducts the 3-question interview for the given employer.
     *
     * @param employer         employer being applied to
     * @param q1               player's Q1 answer (experience)
     * @param q2               player's Q2 answer (reliability)
     * @param q3               player's Q3 answer (criminal record)
     * @param criminalRecord   player's criminal record
     * @param hasForgedRef     whether player is presenting a FORGED_REFERENCE
     * @param forgeryLevel     player's FORGERY skill level (0 = Novice, 1 = Apprentice …)
     * @param inventory        player's inventory (JOB_APPLICATION_FORM consumed; STAFF_ID_BADGE added on offer)
     * @param notorietySystem  for applying caught-reference notoriety penalty
     * @param achievementCallback for unlocking achievements
     * @param currentDay       current in-game day count
     * @return interview result
     */
    public InterviewResult conductInterview(
            Employer employer,
            Q1Answer q1,
            Q2Answer q2,
            Q3Answer q3,
            CriminalRecord criminalRecord,
            boolean hasForgedRef,
            int forgeryLevel,
            Inventory inventory,
            NotorietySystem notorietySystem,
            NotorietySystem.AchievementCallback achievementCallback,
            int currentDay) {

        // Consume application form
        inventory.removeItem(Material.JOB_APPLICATION_FORM, 1);

        // ── Forged reference detection ────────────────────────────────────────
        if (hasForgedRef) {
            inventory.removeItem(Material.FORGED_REFERENCE, 1);
            int levelsAboveApprentice = Math.max(0, forgeryLevel - 1); // 1 = Apprentice
            float detectionChance = FORGED_REFERENCE_BASE_DETECTION
                    - levelsAboveApprentice * FORGED_REFERENCE_DETECTION_REDUCTION_PER_LEVEL;
            detectionChance = Math.max(0f, detectionChance);

            if (random.nextFloat() < detectionChance) {
                // Caught — fraud, permanent blacklist, notoriety
                criminalRecord.record(CrimeType.FRAUD);
                notorietySystem.addNotoriety(FORGED_REFERENCE_CAUGHT_NOTORIETY, achievementCallback);
                addToBlacklist(employer, BLACKLIST_PERMANENT);
                return InterviewResult.FRAUD_DETECTED;
            }
        }

        // ── Score the interview ───────────────────────────────────────────────
        int score = 0;

        // Q1 — Experience
        switch (q1) {
            case YES_LOADS:
                // 30% chance of being caught in the lie → fail
                if (random.nextFloat() >= 0.30f) {
                    score++; // lie passed
                }
                break;
            case NO_BUT_KEEN:
                score++; // honest, always passes
                break;
            case SAY_NOTHING:
                // 0 points
                break;
        }

        // Q2 — Reliability
        boolean strictEmployer = (employer == Employer.GREGGS || employer == Employer.ICELAND);
        switch (q2) {
            case ABSOLUTELY:
                score++;
                break;
            case DEPENDS_ON_MORNING:
                if (!strictEmployer) {
                    score++; // lenient employers accept it
                }
                break;
            case YAWN:
                // 0 points
                break;
        }

        // Q3 — Criminal record
        int totalCrimes = criminalRecord.getTotalCrimes();
        switch (q3) {
            case NO:
                if (totalCrimes >= CRIMINAL_RECORD_REJECTION_THRESHOLD) {
                    // Rejected on the spot; score stays 0 for Q3 (no blacklist, but result = reject)
                    addToBlacklist(employer, BLACKLIST_COOLDOWN_DAYS);
                    return InterviewResult.REJECTED;
                } else if (totalCrimes >= CRIMINAL_RECORD_SUSPICIOUS_THRESHOLD) {
                    // Caught in the lie — 0 points
                } else {
                    score++; // lie passes clean record
                }
                break;
            case YES:
                if (employer == Employer.CORNER_SHOP) {
                    score++; // corner shop is understanding
                }
                // All other employers: polite rejection (0 points)
                break;
            case NONE_OF_YOUR_BUSINESS:
                notorietySystem.addNotoriety(1, achievementCallback);
                // 0 points
                break;
        }

        // ── Apply forged reference bonus ──────────────────────────────────────
        if (hasForgedRef) {
            score += FORGED_REFERENCE_SCORE_BONUS;
        }

        // ── Determine outcome ─────────────────────────────────────────────────
        boolean offered;
        if (score >= 3) {
            offered = true;
        } else if (score == 2) {
            offered = (random.nextFloat() < 0.50f);
        } else {
            offered = false;
        }

        if (!offered) {
            if (score <= 1) {
                // Score 0 or 1 → blacklisted (unless forged ref bypassed)
                addToBlacklist(employer, BLACKLIST_COOLDOWN_DAYS);
            }
            return InterviewResult.REJECTED;
        }

        // ── Hire the player ───────────────────────────────────────────────────
        currentEmployer        = employer;
        hireDayCount           = currentDay;
        dwpDisclosed           = false;
        undisclosedSignOnCycles = 0;
        warningCount           = 0;
        missedShiftCount       = 0;
        consecutiveCleanShifts = 0;
        clockedIn              = false;
        tasksCompletedThisShift = 0;

        inventory.addItem(Material.STAFF_ID_BADGE, 1);
        return InterviewResult.JOB_OFFERED;
    }

    // ── Mechanic 3: Clock In ──────────────────────────────────────────────────

    /**
     * Player presses E on {@code STAFF_CLOCK_IN_PROP} to begin a shift.
     *
     * @param currentHour current in-game hour
     * @param inventory   player's inventory (must contain STAFF_ID_BADGE)
     * @return clock-in result
     */
    public ClockInResult clockIn(float currentHour, Inventory inventory) {
        if (currentEmployer == null) {
            return ClockInResult.NO_ACTIVE_SHIFT;
        }
        if (!inventory.hasItem(Material.STAFF_ID_BADGE)) {
            return ClockInResult.NO_BADGE;
        }
        if (clockedIn) {
            return ClockInResult.ALREADY_CLOCKED_IN;
        }

        float scheduledStart = currentEmployer.shiftStartHour;
        float minutesLate    = (currentHour - scheduledStart) * 60f;

        clockedIn              = true;
        clockInHour            = currentHour;
        tasksCompletedThisShift = 0;

        if (minutesLate > LATE_ARRIVAL_THRESHOLD_MINUTES) {
            warningCount++;
            return ClockInResult.LATE_WARNING;
        }
        return ClockInResult.CLOCKED_IN;
    }

    /**
     * Player presses E on {@code STOCK_CRATE_PROP} or {@code SERVING_COUNTER_PROP}
     * during a shift to log a productivity task.
     *
     * @return true if task was logged, false if not currently on shift
     */
    public boolean logTask() {
        if (!clockedIn) return false;
        tasksCompletedThisShift++;
        return true;
    }

    // ── Mechanic 3: Clock Out ─────────────────────────────────────────────────

    /**
     * Player presses E on {@code STAFF_CLOCK_IN_PROP} to end the shift.
     *
     * @param currentHour         current in-game hour
     * @param inventory           player's inventory (wages added as COIN)
     * @param notorietySystem     for Charity Shop notoriety reduction
     * @param achievementCallback for unlocking achievements
     * @param rumourNetwork       for seeding COMMUNITY_SPIRIT rumour
     * @param randomForRumour     seeded random for rumour NPC selection
     * @return clock-out result
     */
    public ClockOutResult clockOut(float currentHour,
                                   Inventory inventory,
                                   NotorietySystem notorietySystem,
                                   NotorietySystem.AchievementCallback achievementCallback,
                                   RumourNetwork rumourNetwork,
                                   Random randomForRumour) {
        if (!clockedIn) {
            return ClockOutResult.NOT_CLOCKED_IN;
        }

        clockedIn = false;
        float scheduledEnd = currentEmployer.shiftEndHour;
        boolean earlyExit  = currentHour < scheduledEnd - 0.05f; // 3-minute grace

        ClockOutResult result;

        if (tasksCompletedThisShift < TASKS_REQUIRED_PER_SHIFT) {
            warningCount++;
            consecutiveCleanShifts = 0;
            result = ClockOutResult.SKIVING_WARNING;
        } else if (earlyExit) {
            consecutiveCleanShifts = 0;
            achievementCallback.award(AchievementType.WALKED_OUT);
            result = ClockOutResult.WALKED_OUT;
        } else {
            // Successful shift
            consecutiveCleanShifts++;
            result = ClockOutResult.SHIFT_COMPLETE;
        }

        // Pay wages for completed or early shifts
        int wages = currentEmployer.wagePerShift;
        if (result == ClockOutResult.SHIFT_COMPLETE || result == ClockOutResult.WALKED_OUT) {
            inventory.addItem(Material.COIN, wages);
        }

        // Employer-specific perks on SHIFT_COMPLETE
        if (result == ClockOutResult.SHIFT_COMPLETE) {
            applyShiftPerks(currentEmployer, inventory, notorietySystem, achievementCallback,
                            rumourNetwork, randomForRumour);

            // FIRST_DAY achievement
            if (!firstDayAchievementFired) {
                firstDayAchievementFired = true;
                achievementCallback.award(AchievementType.FIRST_DAY);
            }

            // MODEL_EMPLOYEE after 10 consecutive clean shifts
            if (consecutiveCleanShifts >= 10) {
                achievementCallback.award(AchievementType.MODEL_EMPLOYEE);
            }
        }

        return result;
    }

    /**
     * Apply employer-specific shift completion perks.
     */
    public void applyShiftPerks(Employer employer, Inventory inventory,
                                 NotorietySystem notorietySystem,
                                 NotorietySystem.AchievementCallback achievementCallback,
                                 RumourNetwork rumourNetwork,
                                 Random rng) {
        switch (employer) {
            case GREGGS:
                // 20% chance of free sausage roll temptation (manager may catch the player)
                if (rng.nextFloat() < GREGGS_SAUSAGE_ROLL_TEMPTATION_CHANCE) {
                    inventory.addItem(Material.SAUSAGE_ROLL, 1);
                    // Whether manager catches player is resolved externally via
                    // onTheftDuringShift() when the player is observed taking it.
                }
                break;

            case ICELAND:
                // 1 free FROZEN_PIZZA per shift
                inventory.addItem(Material.FROZEN_PIZZA, 1);
                break;

            case CHARITY_SHOP:
                // Notoriety reduction and community respect
                notorietySystem.reduceNotoriety(CHARITY_SHIFT_NOTORIETY_REDUCTION, achievementCallback);
                // Seed COMMUNITY_SPIRIT rumour (NPC selection is caller's responsibility)
                rumourNetwork.addRumour(null,
                        new Rumour(RumourType.COMMUNITY_SPIRIT,
                                   "They've been up at the charity shop every week — proper legend."));
                achievementCallback.award(AchievementType.FIRST_DAY); // already guarded above
                break;

            default:
                break;
        }
    }

    // ── Mechanic 4: Getting the Sack ──────────────────────────────────────────

    /**
     * Called when the player steals from their employer during an active shift.
     * Triggers instant dismissal with {@link DismissalReason#THEFT_DURING_SHIFT}.
     *
     * @param inventory           player's inventory
     * @param criminalRecord      player's criminal record
     * @param notorietySystem     for Notoriety +4
     * @param achievementCallback for achievement tracking
     * @param dwpSystem           notified of employment end
     * @param currentDay          for blacklist timestamp
     * @return true if dismissal was triggered (was on shift), false if not on shift
     */
    public boolean onTheftDuringShift(Inventory inventory,
                                       CriminalRecord criminalRecord,
                                       NotorietySystem notorietySystem,
                                       NotorietySystem.AchievementCallback achievementCallback,
                                       DWPSystem dwpSystem,
                                       int currentDay) {
        if (currentEmployer == null) return false;

        criminalRecord.record(CrimeType.THEFT_FROM_EMPLOYER);
        notorietySystem.addNotoriety(THEFT_DURING_SHIFT_NOTORIETY, achievementCallback);
        dismiss(DismissalReason.THEFT_DURING_SHIFT, inventory, dwpSystem,
                achievementCallback, currentDay);
        return true;
    }

    /**
     * Checks mid-employment whether the player's Notoriety now exceeds the
     * employer's threshold and dismisses them if so.
     *
     * @return true if dismissed, false if still within tolerance
     */
    public boolean checkNotorietyDismissal(int playerNotoriety, Inventory inventory,
                                            DWPSystem dwpSystem,
                                            NotorietySystem.AchievementCallback achievementCallback,
                                            int currentDay) {
        if (currentEmployer == null) return false;
        if (playerNotoriety >= currentEmployer.notorietyThreshold) {
            dismiss(DismissalReason.NOTORIETY_EXCEEDED, inventory, dwpSystem,
                    achievementCallback, currentDay);
            return true;
        }
        return false;
    }

    /**
     * Checks whether late-arrival or missed-shift warnings have reached dismissal thresholds.
     *
     * @return true if dismissed
     */
    public boolean checkWarningDismissal(Inventory inventory,
                                          DWPSystem dwpSystem,
                                          NotorietySystem.AchievementCallback achievementCallback,
                                          int currentDay) {
        if (currentEmployer == null) return false;
        if (warningCount >= MAX_WARNINGS_BEFORE_DISMISSAL) {
            dismiss(DismissalReason.TOO_MANY_WARNINGS, inventory, dwpSystem,
                    achievementCallback, currentDay);
            return true;
        }
        if (missedShiftCount >= MAX_MISSED_SHIFTS_BEFORE_DISMISSAL) {
            dismiss(DismissalReason.TOO_MANY_MISSED_SHIFTS, inventory, dwpSystem,
                    achievementCallback, currentDay);
            return true;
        }
        return false;
    }

    /**
     * Records a missed shift (player did not clock in during their shift window).
     * Adds a warning and checks for dismissal threshold.
     *
     * @return true if this missed shift caused dismissal
     */
    public boolean onMissedShift(Inventory inventory,
                                  DWPSystem dwpSystem,
                                  NotorietySystem.AchievementCallback achievementCallback,
                                  int currentDay) {
        if (currentEmployer == null) return false;
        missedShiftCount++;
        consecutiveCleanShifts = 0;
        return checkWarningDismissal(inventory, dwpSystem, achievementCallback, currentDay);
    }

    /**
     * Dismisses the player from their current employment.
     *
     * @param reason              why the player is being dismissed
     * @param inventory           STAFF_ID_BADGE removed
     * @param dwpSystem           UC re-enabled (non-theft reasons allow immediate re-claim)
     * @param achievementCallback for HIRED_AND_FIRED achievement
     * @param currentDay          for blacklist cooldown calculation
     */
    public void dismiss(DismissalReason reason, Inventory inventory,
                         DWPSystem dwpSystem,
                         NotorietySystem.AchievementCallback achievementCallback,
                         int currentDay) {
        if (currentEmployer == null) return;

        Employer dismissed = currentEmployer;
        clockedIn   = false;
        currentEmployer = null;
        warningCount    = 0;
        missedShiftCount = 0;
        consecutiveCleanShifts = 0;

        // Remove badge
        inventory.removeItem(Material.STAFF_ID_BADGE, 1);

        // Blacklist — permanent for theft/fraud, timed otherwise
        boolean permanent = (reason == DismissalReason.THEFT_DURING_SHIFT);
        addToBlacklist(dismissed, permanent ? BLACKLIST_PERMANENT : BLACKLIST_COOLDOWN_DAYS);

        // Schedule vacancy repost
        vacancyRepostTimers.put(dismissed, VACANCY_REPOST_DAYS);

        // Notify DWP
        dwpSystem.setUCClaimActive(false); // re-enable UC eligibility

        // Achievement tracking
        totalDismissals++;
        achievementCallback.award(AchievementType.HIRED_AND_FIRED);
    }

    // ── Mechanic 5: DWP Integration ───────────────────────────────────────────

    /**
     * Player presses E on {@code COUNCIL_RECEPTIONIST} at {@code COUNCIL_OFFICE}
     * to disclose employment to the DWP.
     *
     * @param dwpSystem the DWP system to suspend UC payments
     */
    public void discloseToDwp(DWPSystem dwpSystem) {
        dwpDisclosed            = true;
        undisclosedSignOnCycles = 0;
        dwpSystem.setUCClaimActive(false); // suspend UC while employed
    }

    /**
     * Called each time the player signs on at JobCentre while employed.
     * If employment has not been disclosed, accumulates sign-on cycles.
     * After {@link #DWP_DISCLOSURE_GRACE_CYCLES} undisclosed cycles, records
     * {@link CrimeType#BENEFIT_FRAUD} and fires the {@code ON_THE_FIDDLE} achievement.
     *
     * @param criminalRecord      player's criminal record
     * @param achievementCallback for ON_THE_FIDDLE achievement
     * @return true if BENEFIT_FRAUD was recorded this cycle
     */
    public boolean onSignOnCycle(CriminalRecord criminalRecord,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        if (currentEmployer == null || dwpDisclosed) return false;

        undisclosedSignOnCycles++;
        if (undisclosedSignOnCycles >= DWP_DISCLOSURE_GRACE_CYCLES) {
            criminalRecord.record(CrimeType.BENEFIT_FRAUD);
            achievementCallback.award(AchievementType.ON_THE_FIDDLE);
            return true;
        }
        return false;
    }

    // ── Day tick ──────────────────────────────────────────────────────────────

    /**
     * Called once per in-game day to tick down blacklist and vacancy timers.
     *
     * @param currentDay current in-game day (unused — timers are countdown-based)
     */
    public void onDayTick(int currentDay) {
        // Tick down temporary blacklist entries
        Set<Employer> toRemove = EnumSet.noneOf(Employer.class);
        for (Map.Entry<Employer, Integer> entry : blacklist.entrySet()) {
            if (entry.getValue() == BLACKLIST_PERMANENT) continue;
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                toRemove.add(entry.getKey());
            } else {
                blacklist.put(entry.getKey(), remaining);
            }
        }
        toRemove.forEach(blacklist::remove);

        // Tick down vacancy repost timers
        Set<Employer> toRepost = EnumSet.noneOf(Employer.class);
        for (Map.Entry<Employer, Integer> entry : vacancyRepostTimers.entrySet()) {
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                toRepost.add(entry.getKey());
            } else {
                vacancyRepostTimers.put(entry.getKey(), remaining);
            }
        }
        toRepost.forEach(vacancyRepostTimers::remove);
    }

    // ── Voluntary resignation ─────────────────────────────────────────────────

    /**
     * Player presses E on manager NPC and selects "I quit".
     * Notifies DWP; UC re-claimable after 7-day waiting period
     * (3 days if BUREAUCRACY ≥ Apprentice — handled externally).
     *
     * @param inventory    STAFF_ID_BADGE removed
     * @param dwpSystem    UC eligibility managed
     * @param currentDay   unused (timers handled externally)
     */
    public void resign(Inventory inventory, DWPSystem dwpSystem, int currentDay) {
        if (currentEmployer == null) return;
        clockedIn       = false;
        currentEmployer = null;
        warningCount    = 0;
        missedShiftCount = 0;
        consecutiveCleanShifts = 0;
        inventory.removeItem(Material.STAFF_ID_BADGE, 1);
        // DWP notified — player must wait the statutory period before re-claiming
        dwpSystem.setUCClaimActive(false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addToBlacklist(Employer employer, int cooldownDays) {
        // Never downgrade a permanent blacklist entry
        if (BLACKLIST_PERMANENT == blacklist.getOrDefault(employer, 0)) return;
        blacklist.put(employer, cooldownDays);
    }

    /**
     * Returns true if the player is currently blacklisted from {@code employer}.
     *
     * @param employer   employer to check
     * @param currentDay unused — blacklist uses countdown days
     */
    public boolean isBlacklisted(Employer employer, int currentDay) {
        return blacklist.containsKey(employer);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns the player's current employer, or {@code null} if unemployed. */
    public Employer getCurrentEmployer() { return currentEmployer; }

    /** Returns true if the player is currently employed. */
    public boolean isEmployed() { return currentEmployer != null; }

    /** Returns true if the player is currently clocked in to a shift. */
    public boolean isClockedIn() { return clockedIn; }

    /** Returns the number of productivity tasks completed in the current shift. */
    public int getTasksCompletedThisShift() { return tasksCompletedThisShift; }

    /** Returns the number of current verbal/written warnings. */
    public int getWarningCount() { return warningCount; }

    /** Returns the number of missed shifts at the current employer. */
    public int getMissedShiftCount() { return missedShiftCount; }

    /** Returns the number of consecutive clean shifts (for MODEL_EMPLOYEE tracking). */
    public int getConsecutiveCleanShifts() { return consecutiveCleanShifts; }

    /** Returns the total number of dismissals across all employers. */
    public int getTotalDismissals() { return totalDismissals; }

    /** Returns the undisclosed-sign-on-cycle count (for DWP fraud tracking). */
    public int getUndisclosedSignOnCycles() { return undisclosedSignOnCycles; }

    /** Returns whether employment has been disclosed to DWP. */
    public boolean isDwpDisclosed() { return dwpDisclosed; }

    /** Returns the blacklist cooldown days remaining, or 0 if not blacklisted. */
    public int getBlacklistDays(Employer employer) {
        return blacklist.getOrDefault(employer, 0);
    }

    /** Returns true if the blacklist entry for this employer is permanent. */
    public boolean isPermanentlyBlacklisted(Employer employer) {
        return BLACKLIST_PERMANENT == blacklist.getOrDefault(employer, 0);
    }
}

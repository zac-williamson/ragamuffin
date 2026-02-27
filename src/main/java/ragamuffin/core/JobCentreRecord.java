package ragamuffin.core;

/**
 * Issue #795: The JobCentre Gauntlet — Universal Credit, Sanctions &amp; Bureaucratic Torment.
 *
 * <p>Tracks the player's Universal Credit claim status across the session.
 * Stores sanction level, sign-on history, assigned job search missions, and
 * whether the debt collector is currently active.
 */
public class JobCentreRecord {

    // ── Sanction constants ─────────────────────────────────────────────────────

    /** Base UC payment per sign-on cycle (8 COIN). */
    public static final int BASE_UC_PAYMENT = 8;

    /** Reduction per sanction level (3 COIN per level). */
    public static final int SANCTION_REDUCTION_PER_LEVEL = 3;

    /** Maximum sanction level before debt collector spawns. */
    public static final int MAX_SANCTION_LEVEL = 3;

    /** COIN owed to DWP when debt collector is active. */
    public static final int DEBT_AMOUNT = 10;

    /** Number of missed sign-ons before Street Lads notice. */
    public static final int STREET_LADS_NOTICE_MISSED = 3;

    /** Faction respect bonus from Street Lads for 3 missed sign-ons. */
    public static final int STREET_LADS_RESPECT_BONUS = 8;

    /** BUREAUCRACY XP awarded for successfully signing on. */
    public static final int XP_SIGN_ON = 10;

    /** BUREAUCRACY XP awarded for completing a job search mission. */
    public static final int XP_MISSION_COMPLETE = 20;

    /** BUREAUCRACY XP for successfully appealing a sanction. */
    public static final int XP_SANCTION_APPEAL = 15;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Current sanction level (0–3). */
    private int sanctionLevel = 0;

    /** Total number of successful sign-ons. */
    private int totalSignOns = 0;

    /** Total number of missed sign-on windows. */
    private int missedSignOns = 0;

    /** Whether a debt collector is currently active. */
    private boolean debtCollectorActive = false;

    /** Whether the UC claim has been permanently closed (Notoriety Tier 5). */
    private boolean claimClosed = false;

    /** The last in-game day number the player signed on. */
    private int lastSignOnDay = -1;

    /** The current assigned job search mission type, or null if none. */
    private JobSearchMissionType currentMission = null;

    /** Whether the current mission has been completed this cycle. */
    private boolean missionCompletedThisCycle = false;

    /** Whether Street Lads have already noticed the missed sign-ons. */
    private boolean streetLadsNoticed = false;

    /** Whether the BUREAUCRACY Level 3 Council Respect bonus has been awarded. */
    private boolean councilRespectAwarded = false;

    // ── Job search mission types ───────────────────────────────────────────────

    /**
     * The five satirical job search missions assigned by the JobCentre.
     */
    public enum JobSearchMissionType {

        /** Stand in front of a MOTIVATIONAL_POSTER for 3 seconds. */
        CV_WORKSHOP("CV Workshop",
            "Attend a 'CV Workshop' — stand in front of the poster for 3 seconds."),

        /** Hear the "We'll keep your CV on file" response from 3 shops. */
        APPLY_FOR_3_JOBS("Apply for 3 Jobs",
            "Apply for 3 jobs — visit any 3 shops and listen to their rejection."),

        /** Break 3 LITTER props within 30 seconds (picking up community rubbish). */
        MANDATORY_WORK_PLACEMENT("Mandatory Work Placement",
            "Attend your Mandatory Work Placement — pick up 3 pieces of litter."),

        /** Interact with the COMMUNITY_NOTICE_BOARD. */
        UNIVERSAL_JOBMATCH_PROFILE("Universal Jobmatch Profile",
            "Update your Universal Jobmatch profile — use the community notice board."),

        /** Speak to the ASSESSOR NPC. */
        WORK_CAPABILITY_ASSESSMENT("Work Capability Assessment",
            "Attend your Work Capability Assessment — speak to the assessor.");

        private final String displayName;
        private final String description;

        JobSearchMissionType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    // ── Construction ──────────────────────────────────────────────────────────

    public JobCentreRecord() {
        // defaults set by field initialisers
    }

    // ── UC payment calculation ─────────────────────────────────────────────────

    /**
     * Compute the UC payment for this cycle, reduced by sanction level.
     *
     * @return COIN amount (may be 0 if fully sanctioned)
     */
    public int getCurrentUCPayment() {
        int payment = BASE_UC_PAYMENT - (sanctionLevel * SANCTION_REDUCTION_PER_LEVEL);
        return Math.max(0, payment);
    }

    // ── Sign-on ────────────────────────────────────────────────────────────────

    /**
     * Record a successful sign-on.
     *
     * @param day the current in-game day number
     */
    public void recordSignOn(int day) {
        lastSignOnDay = day;
        totalSignOns++;
        missionCompletedThisCycle = false;
        currentMission = null;
    }

    /**
     * Record a missed sign-on window.
     * Increments sanction level (up to MAX_SANCTION_LEVEL).
     * At MAX_SANCTION_LEVEL, activates the debt collector.
     */
    public void recordMissedSignOn() {
        missedSignOns++;
        if (sanctionLevel < MAX_SANCTION_LEVEL) {
            sanctionLevel++;
        }
        if (sanctionLevel >= MAX_SANCTION_LEVEL) {
            debtCollectorActive = true;
        }
    }

    // ── Sanction management ────────────────────────────────────────────────────

    /**
     * Reduce sanction level by 1 (minimum 0). Used by the BUREAUCRACY Apprentice
     * automatic sanction appeal perk.
     */
    public void appealSanction() {
        if (sanctionLevel > 0) {
            sanctionLevel--;
        }
    }

    // ── Debt collector ─────────────────────────────────────────────────────────

    /**
     * Clear the debt (player paid 10 COIN) and deactivate the debt collector.
     */
    public void clearDebt() {
        debtCollectorActive = false;
    }

    // ── Claim closure ──────────────────────────────────────────────────────────

    /**
     * Permanently close the UC claim (triggered when case worker flees at Tier 5).
     */
    public void closeClaim() {
        claimClosed = true;
        debtCollectorActive = false;
    }

    // ── Mission management ────────────────────────────────────────────────────

    /**
     * Assign a job search mission for this cycle.
     *
     * @param mission the mission type to assign
     */
    public void assignMission(JobSearchMissionType mission) {
        this.currentMission = mission;
        this.missionCompletedThisCycle = false;
    }

    /**
     * Mark the current mission as completed.
     */
    public void completeMission() {
        this.missionCompletedThisCycle = true;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public int getSanctionLevel() { return sanctionLevel; }
    public int getTotalSignOns() { return totalSignOns; }
    public int getMissedSignOns() { return missedSignOns; }
    public boolean isDebtCollectorActive() { return debtCollectorActive; }
    public boolean isClaimClosed() { return claimClosed; }
    public int getLastSignOnDay() { return lastSignOnDay; }
    public JobSearchMissionType getCurrentMission() { return currentMission; }
    public boolean isMissionCompletedThisCycle() { return missionCompletedThisCycle; }
    public boolean isStreetLadsNoticed() { return streetLadsNoticed; }
    public boolean isCouncilRespectAwarded() { return councilRespectAwarded; }

    /** Mark that Street Lads have been notified of missed sign-ons. */
    public void setStreetLadsNoticed(boolean noticed) { this.streetLadsNoticed = noticed; }

    /** Mark that Council Respect has been awarded for BUREAUCRACY Level 3. */
    public void setCouncilRespectAwarded(boolean awarded) { this.councilRespectAwarded = awarded; }

    /** Force-set sanction level (for testing). */
    public void setSanctionLevelForTesting(int level) {
        this.sanctionLevel = Math.max(0, Math.min(MAX_SANCTION_LEVEL, level));
    }

    /** Force-set debt collector active state (for testing). */
    public void setDebtCollectorActiveForTesting(boolean active) {
        this.debtCollectorActive = active;
    }
}

package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.Random;

/**
 * Issue #1233: Northfield Probation Service — Sign-In Schedule, Community Service
 * &amp; the Ankle Tag Hustle.
 *
 * <p>The Probation Service operates Mon–Fri 09:00–17:00 in the beige pebble-dash
 * building between the Magistrates' Court and the JobCentre. Carol the
 * {@code PROBATION_RECEPTIONIST} manages sign-in appointments; Dave and Linda
 * ({@code PROBATION_OFFICER}) conduct the fortnightly consultations.
 *
 * <h3>Probation Order Types</h3>
 * <ul>
 *   <li><b>Conditional</b> (Notoriety &lt; {@value #NOTORIETY_STANDARD_THRESHOLD}):
 *       sign-ins only, no tag.</li>
 *   <li><b>Standard</b> ({@value #NOTORIETY_STANDARD_THRESHOLD}–{@value #NOTORIETY_ENHANCED_THRESHOLD}−1):
 *       fortnightly sign-ins + community service hours.</li>
 *   <li><b>Enhanced</b> (Notoriety ≥ {@value #NOTORIETY_ENHANCED_THRESHOLD}):
 *       sign-ins + community service + {@link Material#ANKLE_TAG} curfew 21:00–07:00.</li>
 * </ul>
 *
 * <h3>Fortnightly Sign-In</h3>
 * Press E on Carol ({@code PROBATION_RECEPTIONIST}) every 14 in-game days.
 * A 3-day window is allowed (sign-in due on day N can be completed N–N+3).
 * Bank Holidays (any day where {@code day % 7 == 0}) auto-reschedule to the next day.
 * <ul>
 *   <li>1 miss → {@link CriminalRecord.CrimeType#PROBATION_BREACH} + warning letter via PropertySystem.</li>
 *   <li>2 misses → {@link #issueRecallWarrant()} → {@link WantedSystem} +3 stars
 *       + {@link CriminalRecord.CrimeType#RECALL_WARRANT}.</li>
 * </ul>
 *
 * <h3>Community Service Postings</h3>
 * Three options offered by Carol (action-locked during session; leaving resets hours):
 * <ol>
 *   <li>Park Litter Pick (2 h, Notoriety −5).</li>
 *   <li>Food Bank Sorting (2 h, +3 COIN goodwill).</li>
 *   <li>Community Centre Painting (2 h, wall → MAGNOLIA).</li>
 * </ol>
 * {@link #logServiceHours(int)} accumulates minutes. Reaching
 * {@value #TOTAL_SERVICE_MINUTES_REQUIRED} total minutes triggers discharge if sign-ins also complete.
 *
 * <h3>Ankle Tag Curfew</h3>
 * ShelterDetector checks player within {@value #CURFEW_HOME_RADIUS} blocks of squat every 30 s.
 * Breach during curfew (21:00–07:00) → {@link CriminalRecord.CrimeType#CURFEW_BREACH}
 * + {@link WantedSystem} +2 stars.
 *
 * <h3>The Ankle Tag Hustle</h3>
 * FenceSystem (rep ≥ 40, cost {@value #TAG_CUT_COST} COIN) cuts tag; curfew lifted; 1 day later
 * {@link CriminalRecord.CrimeType#TAG_TAMPER} fires (+3 stars, new magistrate hearing,
 * {@link AchievementType#DONT_KNOW_YOU}).
 * {@link DisguiseSystem} score ≥ 3 gives 40% patrol miss rate.
 *
 * <h3>Legitimate Discharge</h3>
 * All sign-ins + hours completed → {@link #dischargeOrder()} → ANKLE_TAG auto-removed,
 * Notoriety −{@value #DISCHARGE_NOTORIETY_REDUCTION}, {@link AchievementType#DONE_MY_TIME}.
 */
public class ProbationSystem {

    // ── Order type thresholds ─────────────────────────────────────────────────

    /** Notoriety below this → Conditional order (sign-ins only, no tag). */
    public static final int NOTORIETY_STANDARD_THRESHOLD = 30;

    /** Notoriety at or above this → Enhanced order (sign-ins + tag). */
    public static final int NOTORIETY_ENHANCED_THRESHOLD = 60;

    // ── Sign-in scheduling ────────────────────────────────────────────────────

    /** Days between required sign-in appointments. */
    public static final int SIGN_IN_INTERVAL_DAYS = 14;

    /** Grace window in days after the due day to complete a sign-in. */
    public static final int SIGN_IN_GRACE_DAYS = 3;

    /** Day modulus value used to detect Bank Holidays. */
    public static final int BANK_HOLIDAY_MODULUS = 7;

    // ── Breach escalation ─────────────────────────────────────────────────────

    /** Wanted stars added on first sign-in miss (warning letter only). */
    public static final int FIRST_MISS_WANTED_STARS = 0;

    /** Wanted stars added when a recall warrant is issued (second miss). */
    public static final int RECALL_WARRANT_WANTED_STARS = 3;

    /** Notoriety added on recall warrant. */
    public static final int RECALL_WARRANT_NOTORIETY = 10;

    // ── Curfew ────────────────────────────────────────────────────────────────

    /** Curfew start hour (21:00). */
    public static final int CURFEW_START_HOUR = 21;

    /** Curfew end hour (07:00). */
    public static final int CURFEW_END_HOUR = 7;

    /** Distance from squat home beyond which a curfew breach is detected. */
    public static final float CURFEW_HOME_RADIUS = 20f;

    /** Seconds between curfew location checks (every 30 s). */
    public static final float CURFEW_CHECK_INTERVAL = 30f;

    /** Wanted stars added on a curfew breach. */
    public static final int CURFEW_BREACH_WANTED_STARS = 2;

    /** Notoriety added on a curfew breach. */
    public static final int CURFEW_BREACH_NOTORIETY = 5;

    // ── Ankle tag hustle ──────────────────────────────────────────────────────

    /** Minimum FenceSystem reputation required to cut the tag. */
    public static final int TAG_CUT_MIN_FENCE_REP = 40;

    /** COIN cost to have the Fence cut the tag. */
    public static final int TAG_CUT_COST = 15;

    /** Wanted stars added when TAG_TAMPER fires. */
    public static final int TAG_TAMPER_WANTED_STARS = 3;

    /** Notoriety added when TAG_TAMPER fires. */
    public static final int TAG_TAMPER_NOTORIETY = 20;

    /** In-game days after tag cut before TAG_TAMPER fires. */
    public static final int TAG_TAMPER_DELAY_DAYS = 1;

    /** DisguiseSystem score threshold for patrol miss rate reduction. */
    public static final int DISGUISE_PATROL_MISS_THRESHOLD = 3;

    /** Probability (0–1) that patrol misses player when disguise score ≥ threshold. */
    public static final float DISGUISE_PATROL_MISS_RATE = 0.40f;

    // ── Community service ─────────────────────────────────────────────────────

    /** Total community service minutes required for discharge. */
    public static final int TOTAL_SERVICE_MINUTES_REQUIRED = 480; // 8 hours

    /** Minutes per standard service posting session. */
    public static final int SERVICE_POSTING_MINUTES = 120; // 2 hours

    /** Notoriety reduction on discharge. */
    public static final int DISCHARGE_NOTORIETY_REDUCTION = 10;

    // ── Order type enum ───────────────────────────────────────────────────────

    /**
     * The tier of a probation order, derived from Notoriety at the time of sentencing.
     */
    public enum OrderType {
        /** No tag; sign-ins only. Notoriety &lt; 30. */
        CONDITIONAL,
        /** Sign-ins + community service. Notoriety 30–59. */
        STANDARD,
        /** Sign-ins + community service + ANKLE_TAG curfew. Notoriety ≥ 60. */
        ENHANCED
    }

    // ── Community service posting enum ────────────────────────────────────────

    /**
     * Available community service task assignments.
     */
    public enum ServicePosting {
        PARK_LITTER_PICK,
        FOOD_BANK_SORTING,
        COMMUNITY_CENTRE_PAINTING
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Whether a probation order is currently active. */
    private boolean orderActive = false;

    /** The current order type (CONDITIONAL/STANDARD/ENHANCED). */
    private OrderType orderType = OrderType.CONDITIONAL;

    /** In-game day on which the next sign-in is due. */
    private int nextSignInDay = -1;

    /** Number of sign-ins successfully completed. */
    private int signInsCompleted = 0;

    /** Total sign-ins required for the full order. */
    private int signInsRequired = 0;

    /** Number of consecutive missed sign-in appointments. */
    private int breachCount = 0;

    /** Whether the player currently has an ankle tag equipped. */
    private boolean tagged = false;

    /** Whether the tag has been cut (tamper pending). */
    private boolean tagCut = false;

    /** In-game day on which TAG_TAMPER should fire (after tag cut). */
    private int tagTamperDay = -1;

    /** Community service minutes logged so far. */
    private int serviceMinutesLogged = 0;

    /** Community service minutes required (set when order activated). */
    private int serviceMinutesRequired = 0;

    /** Whether the player is currently serving a community service session. */
    private boolean serviceSessionActive = false;

    /** Active posting during a community service session. */
    private ServicePosting activePosting = null;

    /** Whether a recall warrant has been issued. */
    private boolean recallWarrantIssued = false;

    /** Seconds since last curfew check. */
    private float curfewCheckTimer = 0f;

    /** The squat home X position (for curfew radius check). */
    private float homeX = 0f;

    /** The squat home Z position (for curfew radius check). */
    private float homeZ = 0f;

    /** Whether home position has been set. */
    private boolean homeSet = false;

    // ── Dependencies ──────────────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;

    private final Random random;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ProbationSystem() {
        this(new Random());
    }

    public ProbationSystem(Random random) {
        this.random = random;
    }

    // ── Dependency setters ────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    // ── Order activation ──────────────────────────────────────────────────────

    /**
     * Activate a probation order for the player. Called by MagistratesCourtSystem
     * on conviction with a community order.
     *
     * @param currentDay           current in-game day number
     * @param notoriety            player's current notoriety (determines order type)
     * @param durationWeeks        order duration in in-game weeks
     * @param communityServiceHours community service hours required
     * @param forceAnkleTag        if true, always apply ankle tag regardless of notoriety
     * @param playerInventory      player's inventory (tag added here if applicable)
     */
    public void activateOrder(int currentDay, int notoriety, int durationWeeks,
                              int communityServiceHours, boolean forceAnkleTag,
                              Inventory playerInventory) {
        orderType = determineOrderType(notoriety);
        orderActive = true;
        signInsRequired = durationWeeks / 2; // one sign-in per 2 weeks
        if (signInsRequired < 1) signInsRequired = 1;
        signInsCompleted = 0;
        breachCount = 0;
        recallWarrantIssued = false;
        serviceMinutesRequired = communityServiceHours * 60;
        serviceMinutesLogged = 0;
        serviceSessionActive = false;
        activePosting = null;
        tagCut = false;
        tagTamperDay = -1;

        int firstDue = currentDay + SIGN_IN_INTERVAL_DAYS;
        nextSignInDay = adjustForBankHoliday(firstDue);

        boolean needsTag = forceAnkleTag || orderType == OrderType.ENHANCED;
        if (needsTag && !tagged) {
            tagged = true;
            if (playerInventory != null) {
                playerInventory.addItem(Material.ANKLE_TAG, 1);
            }
        }
    }

    /**
     * Determine the order type from the player's notoriety score.
     *
     * @param notoriety player notoriety (0–1000)
     * @return the appropriate {@link OrderType}
     */
    public static OrderType determineOrderType(int notoriety) {
        if (notoriety >= NOTORIETY_ENHANCED_THRESHOLD) {
            return OrderType.ENHANCED;
        } else if (notoriety >= NOTORIETY_STANDARD_THRESHOLD) {
            return OrderType.STANDARD;
        } else {
            return OrderType.CONDITIONAL;
        }
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Update the probation system. Call once per frame.
     *
     * @param delta       seconds since last frame
     * @param timeSystem  the game time system
     * @param currentDay  current in-game day number
     * @param playerX     player X world position
     * @param playerZ     player Z world position
     */
    public void update(float delta, TimeSystem timeSystem, int currentDay,
                       float playerX, float playerZ) {
        if (!orderActive) return;

        // Check for missed sign-in (past grace window)
        checkMissedSignIn(currentDay);

        // Check for TAG_TAMPER firing
        checkTagTamper(currentDay);

        // Curfew check (Enhanced orders with active tag)
        if (tagged && !tagCut) {
            curfewCheckTimer += delta;
            if (curfewCheckTimer >= CURFEW_CHECK_INTERVAL) {
                curfewCheckTimer = 0f;
                if (isCurfewActive(timeSystem)) {
                    checkCurfewBreach(playerX, playerZ);
                }
            }
        }
    }

    // ── Sign-in mechanics ─────────────────────────────────────────────────────

    /**
     * Record a completed sign-in with Carol. Call this when the player presses E
     * on the PROBATION_RECEPTIONIST during a valid appointment window.
     *
     * @param currentDay current in-game day number
     * @return true if the sign-in was accepted (within the valid window)
     */
    public boolean recordSignIn(int currentDay) {
        if (!orderActive) return false;

        // Valid window: nextSignInDay to nextSignInDay + SIGN_IN_GRACE_DAYS
        if (currentDay >= nextSignInDay && currentDay <= nextSignInDay + SIGN_IN_GRACE_DAYS) {
            signInsCompleted++;
            int nextDue = currentDay + SIGN_IN_INTERVAL_DAYS;
            nextSignInDay = adjustForBankHoliday(nextDue);
            checkForDischarge(null);
            return true;
        }
        return false;
    }

    /**
     * Calculate the next sign-in due day from a starting day.
     * Adjusts for Bank Holidays (day % 7 == 0).
     *
     * @param currentDay the day from which to schedule the next sign-in
     * @return the day number the next sign-in is due
     */
    public int calculateNextSignInDay(int currentDay) {
        int due = currentDay + SIGN_IN_INTERVAL_DAYS;
        return adjustForBankHoliday(due);
    }

    /**
     * Adjust a day number forward if it falls on a Bank Holiday.
     * Bank Holidays are any day where {@code day % BANK_HOLIDAY_MODULUS == 0}.
     *
     * @param day the candidate day
     * @return the adjusted day (day + 1 if bank holiday, else unchanged)
     */
    public static int adjustForBankHoliday(int day) {
        if (day % BANK_HOLIDAY_MODULUS == 0) {
            return day + 1;
        }
        return day;
    }

    /**
     * Check whether the player has missed a sign-in appointment (past the grace window).
     * Called during per-frame update.
     */
    private void checkMissedSignIn(int currentDay) {
        if (!orderActive || recallWarrantIssued) return;
        if (nextSignInDay < 0) return;

        // Missed if we're past the grace window
        if (currentDay > nextSignInDay + SIGN_IN_GRACE_DAYS) {
            onMissedSignIn(currentDay);
        }
    }

    /**
     * Handle a missed sign-in appointment.
     */
    private void onMissedSignIn(int currentDay) {
        breachCount++;

        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.PROBATION_BREACH);
        }

        if (breachCount >= 2) {
            issueRecallWarrant(currentDay);
        }

        // Advance next sign-in to avoid repeat triggers
        int nextDue = nextSignInDay + SIGN_IN_INTERVAL_DAYS;
        nextSignInDay = adjustForBankHoliday(nextDue);
    }

    /**
     * Issue a recall warrant after 2 missed sign-ins.
     * Adds RECALL_WARRANT to CriminalRecord and escalates WantedSystem.
     */
    public void issueRecallWarrant(int currentDay) {
        if (recallWarrantIssued) return;
        recallWarrantIssued = true;

        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.RECALL_WARRANT);
        }

        if (wantedSystem != null) {
            wantedSystem.addWantedStars(RECALL_WARRANT_WANTED_STARS, 0f, 0f, 0f, null);
        }

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(RECALL_WARRANT_NOTORIETY, null);
        }
    }

    // ── Curfew mechanics ──────────────────────────────────────────────────────

    /**
     * Set the player's squat home position (used for curfew radius checks).
     *
     * @param x world X coordinate of the squat
     * @param z world Z coordinate of the squat
     */
    public void setHomePosition(float x, float z) {
        this.homeX = x;
        this.homeZ = z;
        this.homeSet = true;
    }

    /**
     * Whether curfew is currently active based on the current game time.
     * Curfew runs 21:00–07:00.
     *
     * @param timeSystem the game time system
     * @return true if curfew is active
     */
    public boolean isCurfewActive(TimeSystem timeSystem) {
        int hour = timeSystem.getHours();
        return hour >= CURFEW_START_HOUR || hour < CURFEW_END_HOUR;
    }

    /**
     * Check whether the player is in breach of curfew (outside the home radius).
     * Fires CURFEW_BREACH and adds wanted stars if breached.
     *
     * @param playerX player world X position
     * @param playerZ player world Z position
     */
    public void checkCurfewBreach(float playerX, float playerZ) {
        if (!homeSet) return;

        float dx = playerX - homeX;
        float dz = playerZ - homeZ;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        if (dist > CURFEW_HOME_RADIUS) {
            onCurfewBreach();
        }
    }

    private void onCurfewBreach() {
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.CURFEW_BREACH);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(CURFEW_BREACH_WANTED_STARS, 0f, 0f, 0f, null);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(CURFEW_BREACH_NOTORIETY, null);
        }
    }

    // ── Ankle tag hustle ──────────────────────────────────────────────────────

    /**
     * Cut the ankle tag via the FenceSystem. Removes tag from inventory, lifts curfew,
     * and schedules TAG_TAMPER to fire after {@value #TAG_TAMPER_DELAY_DAYS} day(s).
     *
     * @param inventory  the player's inventory
     * @param currentDay current in-game day number
     * @param fenceRep   the player's current FenceSystem reputation
     * @return true if the tag was successfully cut
     */
    public boolean cutTag(Inventory inventory, int currentDay, int fenceRep) {
        if (!tagged || tagCut) return false;
        if (fenceRep < TAG_CUT_MIN_FENCE_REP) return false;
        if (inventory.getItemCount(Material.COIN) < TAG_CUT_COST) return false;

        inventory.removeItem(Material.COIN, TAG_CUT_COST);
        inventory.removeItem(Material.ANKLE_TAG, 1);
        tagCut = true;
        tagged = false;
        tagTamperDay = currentDay + TAG_TAMPER_DELAY_DAYS;

        // Unlock DONT_KNOW_YOU achievement immediately
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.DONT_KNOW_YOU);
        }

        return true;
    }

    /**
     * Check whether TAG_TAMPER should fire (1 day after tag cut).
     */
    private void checkTagTamper(int currentDay) {
        if (!tagCut || tagTamperDay < 0) return;
        if (currentDay >= tagTamperDay) {
            onTagTamper();
            tagTamperDay = -1;
        }
    }

    private void onTagTamper() {
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.TAG_TAMPER);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(TAG_TAMPER_WANTED_STARS, 0f, 0f, 0f, null);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(TAG_TAMPER_NOTORIETY, null);
        }
    }

    // ── Community service ─────────────────────────────────────────────────────

    /**
     * Log community service minutes. Called by each posting's system.
     * Triggers discharge check and awards {@link AchievementType#COMMUNITY_SPIRIT}
     * when total reaches {@value #TOTAL_SERVICE_MINUTES_REQUIRED}.
     *
     * @param minutes minutes of service completed this session
     */
    public void logServiceHours(int minutes) {
        if (!orderActive || minutes <= 0) return;
        serviceMinutesLogged += minutes;

        if (achievementSystem != null && serviceMinutesLogged >= TOTAL_SERVICE_MINUTES_REQUIRED) {
            achievementSystem.unlock(AchievementType.COMMUNITY_SPIRIT);
        }

        checkForDischarge(null);
    }

    /**
     * Start a community service posting session. Action-locks the player.
     *
     * @param posting the selected posting
     * @return true if the session started successfully
     */
    public boolean startServiceSession(ServicePosting posting) {
        if (serviceSessionActive) return false;
        serviceSessionActive = true;
        activePosting = posting;
        return true;
    }

    /**
     * End the current community service session (e.g. player left the area).
     * Hours logged during the session are lost (leaving resets session progress).
     */
    public void endServiceSession() {
        serviceSessionActive = false;
        activePosting = null;
    }

    /**
     * Get remaining community service minutes required.
     *
     * @return minutes remaining (0 if complete or not on service order)
     */
    public int getServiceHoursRemaining() {
        if (!orderActive) return 0;
        return Math.max(0, serviceMinutesRequired - serviceMinutesLogged);
    }

    // ── Discharge ─────────────────────────────────────────────────────────────

    /**
     * Check whether discharge conditions are met and, if so, discharge the order.
     *
     * @param playerInventory player inventory (for tag removal); may be null
     */
    private void checkForDischarge(Inventory playerInventory) {
        if (!orderActive) return;
        boolean signInsOk = signInsCompleted >= signInsRequired;
        boolean serviceOk = serviceMinutesRequired == 0
                || serviceMinutesLogged >= serviceMinutesRequired;
        if (signInsOk && serviceOk) {
            dischargeOrder(playerInventory);
        }
    }

    /**
     * Legitimately discharge the probation order.
     * Removes ankle tag, reduces notoriety, awards {@link AchievementType#DONE_MY_TIME}.
     * Also awards {@link AchievementType#STRAIGHT_AND_NARROW} if zero breaches.
     *
     * @param playerInventory player inventory (tag removal); may be null
     */
    public void dischargeOrder(Inventory playerInventory) {
        if (!orderActive) return;
        orderActive = false;

        // Remove ankle tag from inventory
        if (tagged) {
            tagged = false;
            if (playerInventory != null) {
                playerInventory.removeItem(Material.ANKLE_TAG, 1);
            }
        }

        // Notoriety reduction
        if (notorietySystem != null) {
            notorietySystem.reduceNotoriety(DISCHARGE_NOTORIETY_REDUCTION, null);
        }

        // Achievements
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.DONE_MY_TIME);
            if (breachCount == 0) {
                achievementSystem.unlock(AchievementType.STRAIGHT_AND_NARROW);
            }
        }
    }

    // ── Public queries ────────────────────────────────────────────────────────

    /**
     * Whether a probation order is currently active for the player.
     */
    public boolean hasProbationOrder() {
        return orderActive;
    }

    /**
     * @deprecated Use {@link #hasProbationOrder()} — kept for compatibility.
     */
    @Deprecated
    public boolean hasProbationOrder(Player player) {
        return orderActive;
    }

    /**
     * The current order type.
     */
    public OrderType getOrderType() {
        return orderType;
    }

    /**
     * In-game day on which the next sign-in is due.
     */
    public int getNextSignInDue() {
        return nextSignInDay;
    }

    /**
     * Number of sign-ins successfully completed so far.
     */
    public int getSignInsCompleted() {
        return signInsCompleted;
    }

    /**
     * Number of sign-ins required for the full order.
     */
    public int getSignInsRequired() {
        return signInsRequired;
    }

    /**
     * Number of consecutive missed sign-in appointments (breaches).
     */
    public int getBreachCount() {
        return breachCount;
    }

    /**
     * Whether the player is currently wearing an ankle tag.
     */
    public boolean isTagged() {
        return tagged;
    }

    /**
     * @deprecated Use {@link #isTagged()} — kept for compatibility.
     */
    @Deprecated
    public boolean isTagged(Player player) {
        return tagged;
    }

    /**
     * Whether the tag has been cut (tamper pending).
     */
    public boolean isTagCut() {
        return tagCut;
    }

    /**
     * Whether a recall warrant has been issued.
     */
    public boolean isRecallWarrantIssued() {
        return recallWarrantIssued;
    }

    /**
     * Community service minutes logged so far.
     */
    public int getServiceMinutesLogged() {
        return serviceMinutesLogged;
    }

    /**
     * Whether a community service session is currently active.
     */
    public boolean isServiceSessionActive() {
        return serviceSessionActive;
    }

    /**
     * The active service posting, or null if none.
     */
    public ServicePosting getActivePosting() {
        return activePosting;
    }

    // ── Force-setters for testing ─────────────────────────────────────────────

    /**
     * Force-set a specific next sign-in day (for tests).
     */
    public void setNextSignInDayForTesting(int day) {
        this.nextSignInDay = day;
    }

    /**
     * Force-set tagged state (for tests).
     */
    public void setTaggedForTesting(boolean tagged) {
        this.tagged = tagged;
    }

    /**
     * Force-set breach count (for tests).
     */
    public void setBreachCountForTesting(int count) {
        this.breachCount = count;
    }

    /**
     * Force-set signInsCompleted (for tests).
     */
    public void setSignInsCompletedForTesting(int count) {
        this.signInsCompleted = count;
    }

    /**
     * Force-set serviceMinutesLogged (for tests).
     */
    public void setServiceMinutesLoggedForTesting(int minutes) {
        this.serviceMinutesLogged = minutes;
    }
}

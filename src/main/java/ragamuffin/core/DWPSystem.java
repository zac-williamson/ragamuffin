package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.DWPRecord.AppealStatus;
import ragamuffin.core.DWPRecord.SanctionTier;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1188: Northfield DWP Home Visit — Benefit Fraud Snooper, Universal
 * Credit Trap &amp; the Undeclared Earnings Hustle.
 *
 * <h3>Overview</h3>
 * When the player claims Universal Credit (via {@link JobCentreSystem}) while
 * simultaneously earning coin through street deals, fencing, or boot sales, a
 * suspicion score (0–100) accumulates. At threshold 60+, a compliance notice is
 * issued and a home visit is scheduled within 1–3 in-game days. On the visit,
 * Compliance Officer Brenda arrives at the squat; the player can:
 * <ul>
 *   <li><b>Cooperate</b> — declare earnings; UC adjusted; suspicion reset to 0.</li>
 *   <li><b>Bluff</b> — dice roll {@code rng.nextInt(100) < (100 − suspicionScore)};
 *       success clears visit; failure logs BENEFIT_FRAUD.</li>
 *   <li><b>Obstruct</b> — +15 suspicion; 30% police arrival chance.</li>
 * </ul>
 *
 * <h3>Sanction tiers</h3>
 * <ul>
 *   <li>WARNING (≥60): UC −20% for 4 weeks.</li>
 *   <li>SUSPENSION (≥70): UC suspended 8 weeks.</li>
 *   <li>CRIMINAL_REFERRAL (≥90 + evidence): prosecution + fine = 3× undeclared.</li>
 * </ul>
 *
 * <h3>Appeal</h3>
 * Craft APPEAL_LETTER_PROP (NEWSPAPER + MARKER_PEN). Press E on JOB_CENTRE_CLERK.
 * 2-week resolution. 50% base success, +20% if Notoriety &lt; 20, −20% if
 * CRIMINAL_REFERRAL active.
 */
public class DWPSystem {

    // ── Suspicion score constants ─────────────────────────────────────────────

    /** Suspicion gained per street deal while UC is active. */
    public static final int SUSPICION_STREET_DEAL     = 3;
    /** Suspicion gained per fence sale while UC is active. */
    public static final int SUSPICION_FENCE_SALE      = 5;
    /** Suspicion gained per boot sale win while UC is active. */
    public static final int SUSPICION_BOOT_SALE       = 4;
    /** Suspicion gained from a neighbour tip-off (NeighbourhoodWatch active). */
    public static final int SUSPICION_NEIGHBOUR_TIP   = 15;
    /** Suspicion gained when Notoriety crosses a new tier while UC is active. */
    public static final int SUSPICION_NOTORIETY_TIER  = 10;
    /** Suspicion gained at fortnightly sign-on with unreported earnings ≥ 20 COIN. */
    public static final int SUSPICION_SIGN_ON_UNREPORTED = 12;
    /** Suspicion decay per idle day (no earnings). */
    public static final int SUSPICION_DECAY_IDLE_DAY  = 2;
    /** Suspicion gained when player evades the home visit (no answer in 60s). */
    public static final int SUSPICION_EVASION         = 10;
    /** Suspicion gained on Obstruct stance. */
    public static final int SUSPICION_OBSTRUCT        = 15;
    /** Suspicion per contraband prop found during evidence search. */
    public static final int SUSPICION_CONTRABAND_FOUND = 20;
    /** Suspicion threshold at which a home visit is scheduled. */
    public static final int SUSPICION_VISIT_THRESHOLD = 60;
    /** Suspicion threshold at which Keith accompanies Brenda. */
    public static final int SUSPICION_KEITH_THRESHOLD = 80;
    /** Suspicion reduction on successful Bluff. */
    public static final int SUSPICION_BLUFF_SUCCESS_REDUCTION = 20;

    /** Minimum COIN earnings since last sign-on to trigger sign-on suspicion. */
    public static final int SIGN_ON_UNREPORTED_THRESHOLD = 20;

    /** Search radius (blocks) for Brenda's evidence search around squat door. */
    public static final float SEARCH_RADIUS = 8f;

    /** Probability of police arriving after Obstruct stance. */
    public static final float OBSTRUCT_POLICE_CHANCE = 0.30f;

    /** Base appeal success probability (0.0–1.0). */
    public static final float APPEAL_BASE_SUCCESS = 0.50f;
    /** Appeal success bonus if Notoriety &lt; 20. */
    public static final float APPEAL_LOW_NOTORIETY_BONUS = 0.20f;
    /** Appeal success penalty if CRIMINAL_REFERRAL active. */
    public static final float APPEAL_CRIMINAL_REFERRAL_PENALTY = 0.20f;

    /** Notoriety threshold for appeal bonus. */
    public static final int APPEAL_LOW_NOTORIETY_THRESHOLD = 20;

    /** Home visit door-answer timeout in real seconds. */
    public static final float VISIT_ANSWER_TIMEOUT = 60f;

    /** Coin cost to bribe the nosy neighbour for 7 in-game days. */
    public static final int NEIGHBOUR_BRIBE_COST = 5;

    /** In-game days a neighbour bribe suppresses tip-offs. */
    public static final int NEIGHBOUR_BRIBE_DURATION_DAYS = 7;

    /** Consecutive zero-declaration sign-ons needed for NOTHING_TO_DECLARE achievement. */
    public static final int NOTHING_TO_DECLARE_TARGET = 5;

    /** Consecutive weeks of both UC and street earning for BENEFIT_STREET achievement. */
    public static final int BENEFIT_STREET_WEEKS_TARGET = 10;

    // ── Contraband materials that raise suspicion in evidence search ──────────

    private static final Material[] CONTRABAND_MATERIALS = {
        Material.STOLEN_PHONE,
        Material.COUNTERFEIT_NOTE,
        Material.BOLT_CUTTERS,
        Material.BALACLAVA,
        Material.CROWBAR,
        Material.CASH_IN_HAND_LEDGER
    };

    // ── Interview stance ──────────────────────────────────────────────────────

    /** The player's chosen response when Brenda arrives. */
    public enum InterviewStance {
        COOPERATE,
        BLUFF,
        OBSTRUCT
    }

    /** Result of the home visit interview. */
    public enum VisitResult {
        /** Cooperated; UC adjusted; suspicion reset. */
        COOPERATED,
        /** Bluff succeeded; visit closed; suspicion reduced. */
        BLUFFED_SUCCESS,
        /** Bluff failed; BENEFIT_FRAUD logged; sanction applied. */
        BLUFFED_FAILURE,
        /** Player obstructed; suspicion increased; possible police. */
        OBSTRUCTED,
        /** Player did not answer in time; evasion logged. */
        EVADED,
        /** No active home visit to respond to. */
        NO_VISIT
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;
    private final DWPRecord record;

    /** Whether a UC claim is currently active. */
    private boolean ucClaimActive = false;

    /** Current suspicion score (0–100). */
    private int suspicionScore = 0;

    /** Whether a home visit has been scheduled. */
    private boolean homeVisitScheduled = false;

    /** In-game day on which the home visit should occur. */
    private int homeVisitDay = -1;

    /** In-game day the current visit started (for timeout calculation). */
    private float visitStartTime = -1f;

    /** Whether Brenda has knocked and the player has not yet responded. */
    private boolean visitKnocking = false;

    /** Elapsed real seconds since Brenda knocked (for timeout). */
    private float visitElapsedSeconds = 0f;

    /** In-game day the last nosy-neighbour tip-off was suppressed (bribed). */
    private int neighbourBribeExpiryDay = -1;

    /** In-game day of the last nosy-neighbour tip-off. */
    private int lastNeighbourTipDay = -1;

    /** Consecutive idle days (no earnings). */
    private int idleDayStreak = 0;

    /** In-game day of the most recent earnings notification. */
    private int lastEarningDay = -1;

    /** Cumulative coin earned since last fortnightly sign-on. */
    private int earningsSinceSignOn = 0;

    /** Consecutive zero-declaration sign-ons (for NOTHING_TO_DECLARE). */
    private int consecutiveZeroDeclarations = 0;

    /** Consecutive weeks claiming UC + street earning (for BENEFIT_STREET). */
    private int benefitStreetWeeks = 0;

    /** Whether BENEFIT_STREET achievement has been unlocked. */
    private boolean benefitStreetUnlocked = false;

    /** Whether BROWN_ENVELOPE achievement has been unlocked. */
    private boolean brownEnvelopeUnlocked = false;

    /** Whether NOSY_NEIGHBOUR achievement has been unlocked. */
    private boolean nosyNeighbourUnlocked = false;

    /** Whether NOTHING_TO_DECLARE achievement has been unlocked. */
    private boolean nothingToDeclareUnlocked = false;

    /** Post-election payment multiplier (applied by LocalElectionSystem when Brannigan wins). */
    private float paymentMultiplier = 1.0f;

    // ── Construction ──────────────────────────────────────────────────────────

    public DWPSystem(Random random) {
        this.random = random;
        this.record = new DWPRecord();
    }

    // ── UC claim activation ───────────────────────────────────────────────────

    /**
     * Enable or disable the UC claim for the player. Called by JobCentreSystem
     * when the claim is opened or permanently closed.
     *
     * @param active true to enable; false to disable
     */
    public void setUCClaimActive(boolean active) {
        this.ucClaimActive = active;
        if (!active) {
            suspicionScore = 0;
            homeVisitScheduled = false;
            visitKnocking = false;
            earningsSinceSignOn = 0;
        }
    }

    /** Returns true if the player currently has an active UC claim. */
    public boolean isUCClaimActive() {
        return ucClaimActive;
    }

    // ── Earnings notification ─────────────────────────────────────────────────

    /**
     * Notify the DWP system that the player has earned coin from any source.
     * Only accumulates suspicion when the UC claim is active.
     *
     * <p>Called by StreetEconomySystem (street deal), FenceSystem (item sale),
     * and BootSaleSystem (auction win). The {@code earningType} parameter
     * determines how much suspicion is added:
     * <ul>
     *   <li>{@code "STREET_DEAL"} → +3 suspicion</li>
     *   <li>{@code "FENCE_SALE"} → +5 suspicion</li>
     *   <li>{@code "BOOT_SALE"}  → +4 suspicion</li>
     * </ul>
     *
     * @param player      the player (for notoriety checks — currently unused)
     * @param coin        amount earned
     * @param earningType one of "STREET_DEAL", "FENCE_SALE", "BOOT_SALE"
     * @param currentDay  current in-game day
     */
    public void onEarning(Player player, int coin, String earningType, int currentDay) {
        earningsSinceSignOn += coin;
        lastEarningDay = currentDay;
        idleDayStreak = 0;

        if (!ucClaimActive) return;

        int gain;
        switch (earningType) {
            case "FENCE_SALE":  gain = SUSPICION_FENCE_SALE;  break;
            case "BOOT_SALE":   gain = SUSPICION_BOOT_SALE;   break;
            default:            gain = SUSPICION_STREET_DEAL; break; // STREET_DEAL
        }
        addSuspicion(gain);
        record.addUndeclaredEarnings(coin);

        // Track benefit-street weeks
        if (ucClaimActive) {
            benefitStreetWeeks++;
        }
    }

    /**
     * Convenience overload: {@code earningType} defaults to "STREET_DEAL".
     * This is the method called directly by the integration tests per the spec.
     */
    public void onEarning(Player player, int coin) {
        onEarning(player, coin, "STREET_DEAL", 0);
    }

    /**
     * Notify the DWP system that the player's Notoriety crossed a new tier
     * while the UC claim is active.
     */
    public void onNotorietyTierUp() {
        if (!ucClaimActive) return;
        addSuspicion(SUSPICION_NOTORIETY_TIER);
    }

    // ── Idle day decay ────────────────────────────────────────────────────────

    /**
     * Called once per in-game day to apply suspicion decay and check
     * nosy-neighbour tip-off.
     *
     * @param currentDay                 current in-game day
     * @param neighbourhoodWatchActive   whether NeighbourhoodWatchSystem is active
     * @param achievementCallback        callback to unlock achievements
     */
    public void onDayTick(int currentDay, boolean neighbourhoodWatchActive,
                          NotorietySystem.AchievementCallback achievementCallback) {
        if (!ucClaimActive) return;

        // Idle decay: if no earning today, reduce suspicion
        if (lastEarningDay < currentDay) {
            idleDayStreak++;
            suspicionScore = Math.max(0, suspicionScore - SUSPICION_DECAY_IDLE_DAY);
        } else {
            idleDayStreak = 0;
        }

        // Nosy neighbour tip-off: once per 7 in-game days if not bribed
        if (neighbourhoodWatchActive) {
            boolean bribeActive = (neighbourBribeExpiryDay >= 0 && currentDay < neighbourBribeExpiryDay);
            boolean alreadyTippedThisWeek = (lastNeighbourTipDay >= 0
                    && currentDay - lastNeighbourTipDay < 7);

            if (!bribeActive && !alreadyTippedThisWeek) {
                lastNeighbourTipDay = currentDay;
                addSuspicion(SUSPICION_NEIGHBOUR_TIP);

                if (!nosyNeighbourUnlocked) {
                    nosyNeighbourUnlocked = true;
                    achievementCallback.award(AchievementType.DWP_TIPOFF);
                }
            }
        }
    }

    // ── Fortnightly sign-on ───────────────────────────────────────────────────

    /**
     * Called by JobCentreSystem at the player's fortnightly sign-on.
     * The player declares how many COIN they earned; under-reporting increases
     * suspicion.
     *
     * @param actualEarned   total coin earned since last sign-on
     * @param declared       coin declared to the DWP (0 to actualEarned)
     * @param notoriety      current notoriety score (for tier checks)
     * @param achievementCb  callback to unlock achievements
     * @return the UC payment the player receives this fortnight
     */
    public int reportEarnings(int actualEarned, int declared,
                              int notoriety,
                              NotorietySystem.AchievementCallback achievementCb) {
        earningsSinceSignOn = 0;

        if (actualEarned <= 0) {
            // Clean fortnight — decay suspicion
            suspicionScore = Math.max(0, suspicionScore - SUSPICION_DECAY_IDLE_DAY * 14);
            consecutiveZeroDeclarations++;
            checkNothingToDeclare(achievementCb);
            return record.computeUCPayment();
        }

        int undeclared = actualEarned - declared;
        if (undeclared > 0) {
            record.addUndeclaredEarnings(undeclared);
        }

        if (declared == 0) {
            // Under-declared everything
            consecutiveZeroDeclarations++;
            addSuspicion(SUSPICION_SIGN_ON_UNREPORTED);
            checkNothingToDeclare(achievementCb);
        } else if (declared < actualEarned) {
            // Partial declaration
            consecutiveZeroDeclarations = 0;
            // Suspicion increase proportional to under-declaration
            addSuspicion(SUSPICION_SIGN_ON_UNREPORTED / 4);
        } else {
            // Full declaration
            consecutiveZeroDeclarations = 0;
            record.clearUndeclaredEarnings();
            suspicionScore = Math.max(0, suspicionScore - SUSPICION_DECAY_IDLE_DAY * 7);
        }

        // UC payment: proportional to declared fraction of actual
        float declarationFraction = actualEarned > 0
                ? (float) declared / actualEarned
                : 1.0f;
        int basePayment = record.computeUCPayment();
        // Retain UC proportional to declared earnings
        // If declared == 0: full UC (DWP doesn't know about income)
        // If declared == actual: reduced UC (fully above threshold)
        if (declared == 0) {
            return basePayment;
        } else if (declared >= actualEarned) {
            // Full declaration reduces UC proportionally (earnings taper off benefit)
            return Math.max(0, Math.round(basePayment * (1f - declarationFraction * 0.5f)));
        } else {
            // Partial: scale proportionally
            return Math.round(basePayment * (1f - declarationFraction * 0.5f));
        }
    }

    /**
     * Get the pending UC payment amount for this player.
     * Returns the payment from the DWP record based on active sanctions.
     */
    public int getPendingUCPayment() {
        return record.computeUCPayment();
    }

    // ── Home visit scheduling ─────────────────────────────────────────────────

    /**
     * Add to the suspicion score and schedule a home visit if the threshold is
     * reached.
     *
     * @param amount amount to add
     */
    private void addSuspicion(int amount) {
        suspicionScore = Math.min(100, suspicionScore + amount);
        if (suspicionScore >= SUSPICION_VISIT_THRESHOLD && !homeVisitScheduled) {
            scheduleHomeVisit();
        }
    }

    private void scheduleHomeVisit() {
        homeVisitScheduled = true;
        // Visit scheduled 1–3 in-game days from now (random)
        homeVisitDay = random.nextInt(3) + 1;
    }

    // ── Home visit response ───────────────────────────────────────────────────

    /**
     * Simulate Brenda knocking. Call this when she arrives.
     * Starts the 60-second answer timer.
     *
     * @param noiseSystem if non-null, triggers a level-3 knock event
     */
    public void brendaKnocks(NoiseSystem noiseSystem) {
        visitKnocking = true;
        visitElapsedSeconds = 0f;
        if (noiseSystem != null) {
            noiseSystem.addNoise(0.9f); // level-3 knock — loud but not quite block-break
        }
    }

    /**
     * Update the home visit timer. Call each frame while a visit is in progress.
     *
     * @param delta          seconds since last frame
     * @param criminalRecord to log evasion crime
     * @param achievementCb  callback for achievements
     * @return EVADED if the timer expired and the player was marked as evading;
     *         null otherwise
     */
    public VisitResult updateVisitTimer(float delta,
                                        CriminalRecord criminalRecord,
                                        NotorietySystem.AchievementCallback achievementCb) {
        if (!visitKnocking) return null;
        visitElapsedSeconds += delta;
        if (visitElapsedSeconds >= VISIT_ANSWER_TIMEOUT) {
            // No answer — log evasion
            visitKnocking = false;
            homeVisitScheduled = false;
            criminalRecord.record(CriminalRecord.CrimeType.BENEFIT_FRAUD_EVASION);
            addSuspicion(SUSPICION_EVASION);
            return VisitResult.EVADED;
        }
        return null;
    }

    /**
     * The player responds to Brenda's knock.
     *
     * @param stance               the chosen response
     * @param inventory            player's inventory (for contraband check)
     * @param squatFurnishings     furnishing tracker (for evidence search)
     * @param criminalRecord       to log crimes
     * @param wantedSystem         for police arrival chance
     * @param achievementCallback  callback for achievements
     * @return the result of the interview
     */
    public VisitResult respondToVisit(
            InterviewStance stance,
            Inventory inventory,
            SquatFurnishingTracker squatFurnishings,
            CriminalRecord criminalRecord,
            WantedSystem wantedSystem,
            NotorietySystem.AchievementCallback achievementCallback) {

        if (!visitKnocking && !homeVisitScheduled) {
            return VisitResult.NO_VISIT;
        }

        visitKnocking = false;
        homeVisitScheduled = false;

        switch (stance) {
            case COOPERATE:
                return handleCooperate(achievementCallback);

            case BLUFF:
                return handleBluff(criminalRecord, wantedSystem, achievementCallback);

            case OBSTRUCT:
                return handleObstruct(wantedSystem, achievementCallback);

            default:
                return VisitResult.NO_VISIT;
        }
    }

    private VisitResult handleCooperate(NotorietySystem.AchievementCallback achievementCallback) {
        // Declare all earnings; UC adjusted; suspicion reset
        record.clearUndeclaredEarnings();
        suspicionScore = 0;
        earningsSinceSignOn = 0;
        // UC payment is reduced proportionally (handled by DWPRecord/computeUCPayment)
        return VisitResult.COOPERATED;
    }

    private VisitResult handleBluff(
            CriminalRecord criminalRecord,
            WantedSystem wantedSystem,
            NotorietySystem.AchievementCallback achievementCallback) {

        // Bluff roll: rng.nextInt(100) < (100 - suspicionScore) → success
        int roll = random.nextInt(100);
        boolean success = roll < (100 - suspicionScore);

        if (success) {
            // Bluff succeeds: suspicion reduced
            suspicionScore = Math.max(0, suspicionScore - SUSPICION_BLUFF_SUCCESS_REDUCTION);
            achievementCallback.award(AchievementType.TALKED_MY_WAY_OUT);
            return VisitResult.BLUFFED_SUCCESS;
        } else {
            // Bluff fails: log BENEFIT_FRAUD and apply sanction
            criminalRecord.record(CriminalRecord.CrimeType.BENEFIT_FRAUD);
            applySanction(wantedSystem, achievementCallback);
            return VisitResult.BLUFFED_FAILURE;
        }
    }

    private VisitResult handleObstruct(
            WantedSystem wantedSystem,
            NotorietySystem.AchievementCallback achievementCallback) {

        addSuspicion(SUSPICION_OBSTRUCT);

        // 30% chance police arrive
        if (random.nextFloat() < OBSTRUCT_POLICE_CHANCE && wantedSystem != null) {
            wantedSystem.addWantedStars(1, 0, 0, 0, null);
        }

        return VisitResult.OBSTRUCTED;
    }

    /**
     * Perform an evidence search (called when suspicion ≥ 80 and player grants access).
     * Checks inventory for contraband items.
     *
     * @param inventory           player's inventory
     * @param criminalRecord      to log crimes
     * @param wantedSystem        for wanted stars on CRIMINAL_REFERRAL
     * @param achievementCallback callback for achievements
     */
    public void performEvidenceSearch(
            Inventory inventory,
            CriminalRecord criminalRecord,
            WantedSystem wantedSystem,
            NotorietySystem.AchievementCallback achievementCallback) {

        for (Material contraband : CONTRABAND_MATERIALS) {
            if (inventory.getItemCount(contraband) > 0) {
                if (contraband == Material.CASH_IN_HAND_LEDGER) {
                    // Instant CRIMINAL_REFERRAL
                    criminalRecord.record(CriminalRecord.CrimeType.BENEFIT_FRAUD);
                    record.applySanction(SanctionTier.CRIMINAL_REFERRAL);
                    if (wantedSystem != null) {
                        wantedSystem.addWantedStars(2, 0, 0, 0, null);
                    }
                    achievementCallback.award(AchievementType.BENEFIT_FRAUDSTER);
                    return;
                }
                addSuspicion(SUSPICION_CONTRABAND_FOUND);
            }
        }

        // After contraband search, apply sanction if threshold crossed
        if (suspicionScore >= 90) {
            criminalRecord.record(CriminalRecord.CrimeType.BENEFIT_FRAUD);
            applySanction(wantedSystem, achievementCallback);
        }
    }

    private void applySanction(WantedSystem wantedSystem,
                                NotorietySystem.AchievementCallback achievementCallback) {
        SanctionTier tier;
        if (suspicionScore >= 90) {
            tier = SanctionTier.CRIMINAL_REFERRAL;
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(2, 0, 0, 0, null);
            }
            achievementCallback.award(AchievementType.BENEFIT_FRAUDSTER);
        } else if (suspicionScore >= 70) {
            tier = SanctionTier.SUSPENSION;
        } else {
            tier = SanctionTier.WARNING;
        }
        record.applySanction(tier);
    }

    // ── Appeal mechanic ───────────────────────────────────────────────────────

    /**
     * Submit an appeal at the JobCentre. Requires APPEAL_LETTER_PROP in inventory.
     *
     * @param inventory    player's inventory
     * @param currentDay   current in-game day
     * @return true if the appeal was submitted successfully
     */
    public boolean submitAppeal(Inventory inventory, int currentDay) {
        if (inventory.getItemCount(Material.APPEAL_LETTER_PROP) <= 0) return false;
        if (!record.hasSanction()) return false;

        boolean submitted = record.submitAppeal(currentDay);
        if (submitted) {
            inventory.removeItem(Material.APPEAL_LETTER_PROP, 1);
        }
        return submitted;
    }

    /**
     * Attempt to resolve the appeal (called each day tick after submission).
     * Appeal resolves after 14 in-game days.
     *
     * @param currentDay    current in-game day
     * @param notoriety     player's current notoriety score
     * @param achievementCb callback for achievements
     * @return true if the appeal was resolved this tick
     */
    public boolean tryResolveAppeal(int currentDay, int notoriety,
                                    NotorietySystem.AchievementCallback achievementCb) {
        if (!record.isAppealPending()) return false;
        if (currentDay < record.getAppealSubmittedDay() + 14) return false;

        float successChance = APPEAL_BASE_SUCCESS;
        if (notoriety < APPEAL_LOW_NOTORIETY_THRESHOLD) {
            successChance += APPEAL_LOW_NOTORIETY_BONUS;
        }
        if (record.getSanctionTier() == SanctionTier.CRIMINAL_REFERRAL) {
            successChance -= APPEAL_CRIMINAL_REFERRAL_PENALTY;
        }
        successChance = Math.max(0f, Math.min(1f, successChance));

        boolean success = random.nextFloat() < successChance;
        record.resolveAppeal(currentDay, success);

        if (success) {
            achievementCb.award(AchievementType.APPEAL_UPHELD);
        }
        return true;
    }

    // ── Nosy neighbour bribe ──────────────────────────────────────────────────

    /**
     * Bribe the nosy neighbour to suppress DWP tip-offs for 7 in-game days.
     *
     * @param inventory   player's inventory
     * @param currentDay  current in-game day
     * @return true if the bribe was paid
     */
    public boolean brideNeighbour(Inventory inventory, int currentDay) {
        if (inventory.getItemCount(Material.COIN) < NEIGHBOUR_BRIBE_COST) return false;
        inventory.removeItem(Material.COIN, NEIGHBOUR_BRIBE_COST);
        neighbourBribeExpiryDay = currentDay + NEIGHBOUR_BRIBE_DURATION_DAYS;
        return true;
    }

    // ── NOTHING_TO_DECLARE achievement helper ─────────────────────────────────

    private void checkNothingToDeclare(NotorietySystem.AchievementCallback achievementCallback) {
        if (!nothingToDeclareUnlocked
                && consecutiveZeroDeclarations >= NOTHING_TO_DECLARE_TARGET
                && !homeVisitScheduled) {
            nothingToDeclareUnlocked = true;
            achievementCallback.award(AchievementType.NOTHING_TO_DECLARE);
        }
    }

    // ── DWP_LETTER_PROP placement ─────────────────────────────────────────────

    /**
     * Notify that a DWP_LETTER_PROP has been placed (for achievement tracking).
     *
     * @param achievementCallback callback to unlock BROWN_ENVELOPE achievement
     */
    public void onDWPLetterPlaced(NotorietySystem.AchievementCallback achievementCallback) {
        if (!brownEnvelopeUnlocked) {
            brownEnvelopeUnlocked = true;
            achievementCallback.award(AchievementType.BROWN_ENVELOPE);
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /**
     * Returns the current suspicion score (0–100).
     *
     * @param player ignored (for API compatibility with spec)
     */
    public int getSuspicionScore(Player player) {
        return suspicionScore;
    }

    /** Returns the current suspicion score. */
    public int getSuspicionScore() {
        return suspicionScore;
    }

    /**
     * Returns true if a home visit has been scheduled (suspicion ≥ 60).
     *
     * @param player ignored (for API compatibility with spec)
     */
    public boolean isHomeVisitScheduled(Player player) {
        return homeVisitScheduled;
    }

    /** Returns true if a home visit is scheduled. */
    public boolean isHomeVisitScheduled() {
        return homeVisitScheduled;
    }

    /**
     * Returns the current sanction tier.
     *
     * @param player ignored (for API compatibility with spec)
     */
    public SanctionTier getSanctionTier(Player player) {
        return record.getSanctionTier();
    }

    /** Returns the current sanction tier. */
    public SanctionTier getSanctionTier() {
        return record.getSanctionTier();
    }

    /** Returns true if any sanction is active. */
    public boolean hasSanction(Player player) {
        return record.hasSanction();
    }

    /** Returns the DWP data record. */
    public DWPRecord getRecord() {
        return record;
    }

    /** Returns true if Brenda has knocked and is waiting for the player to respond. */
    public boolean isVisitKnocking() {
        return visitKnocking;
    }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /** Force-set the suspicion score (for testing). */
    public void setSuspicionScoreForTesting(int score) {
        this.suspicionScore = Math.max(0, Math.min(100, score));
    }

    /** Force-schedule a home visit (for testing). */
    public void scheduleHomeVisitForTesting() {
        homeVisitScheduled = true;
        visitKnocking = true;
        visitElapsedSeconds = 0f;
    }

    /** Force-set the neighbour bribe expiry (for testing). */
    public void setNeighbourBribeExpiryForTesting(int day) {
        this.neighbourBribeExpiryDay = day;
    }

    /** Force-set the last neighbour tip day (for testing). */
    public void setLastNeighbourTipDayForTesting(int day) {
        this.lastNeighbourTipDay = day;
    }

    /** Force-set consecutive zero declarations (for testing). */
    public void setConsecutiveZeroDeclarationsForTesting(int count) {
        this.consecutiveZeroDeclarations = count;
    }

    /** Force-set the earnings since last sign-on (for testing). */
    public void setEarningsSinceSignOnForTesting(int amount) {
        this.earningsSinceSignOn = amount;
    }

    /** Set visit knocking state (for testing). */
    public void setVisitKnockingForTesting(boolean knocking) {
        this.visitKnocking = knocking;
        if (knocking) {
            homeVisitScheduled = true;
            visitElapsedSeconds = 0f;
        }
    }

    // ── Post-election multiplier (Issue #1414: LocalElectionSystem) ───────────

    /**
     * Get the current UC payment multiplier.  Normally 1.0f; set above 1.0f by
     * {@link ragamuffin.core.LocalElectionSystem} when Brannigan (Red) wins the election.
     */
    public float getPaymentMultiplier() {
        return paymentMultiplier;
    }

    /**
     * Set the UC payment multiplier (called by LocalElectionSystem post-election).
     * @param multiplier new multiplier value (e.g. 1.10f for +10%)
     */
    public void setPaymentMultiplier(float multiplier) {
        this.paymentMultiplier = multiplier;
    }
}

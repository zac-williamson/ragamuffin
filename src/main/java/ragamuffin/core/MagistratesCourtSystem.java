package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.Random;

/**
 * Issue #1079: Northfield Magistrates' Court — Your Day in Dock, the Duty Solicitor
 * &amp; the Community Service Economy.
 *
 * <h3>Court Summons</h3>
 * After any arrest with a non-minor {@link CriminalRecord.CrimeType}, a court date is
 * scheduled {@value #SUMMONS_DAYS_AHEAD} in-game days ahead. A {@link Material#COURT_SUMMONS}
 * item appears in the player's inventory. Failure to appear triggers Notoriety
 * +{@value #FAILURE_TO_APPEAR_NOTORIETY}, a {@link CriminalRecord.CrimeType#FAILURE_TO_APPEAR}
 * record entry, and increased police patrols.
 *
 * <h3>NPCs</h3>
 * <ul>
 *   <li><b>Magistrate Sandra Pemberton</b> ({@link ragamuffin.entity.NPCType#MAGISTRATE}) —
 *       severe, hates time-wasters; presides over all hearings.</li>
 *   <li><b>CPS Prosecutor Martin Gale</b> ({@link ragamuffin.entity.NPCType#CPS_PROSECUTOR}) —
 *       reads charges; bribeable ({@value #BRIBE_PROSECUTOR_COST} COIN) to drop a charge.</li>
 *   <li><b>Duty Solicitor Donna</b> ({@link ragamuffin.entity.NPCType#DUTY_SOLICITOR}) —
 *       {@value #DUTY_SOLICITOR_COST} COIN; reduces sentence tier by one if engaged.</li>
 *   <li><b>Court Usher Trevor</b> ({@link ragamuffin.entity.NPCType#COURT_USHER}) —
 *       bribeable ({@value #BRIBE_USHER_COST} COIN) to delay the session.</li>
 * </ul>
 *
 * <h3>Hearing Flow</h3>
 * Charges read → Guilty/Not Guilty plea → evidence check via WitnessSystem → sentence.
 * Outcomes: CONDITIONAL_CAUTION, FINE, COMMUNITY_SERVICE, SUSPENDED_SENTENCE, CUSTODIAL.
 *
 * <h3>Community Service Economy</h3>
 * A {@link Material#COMMUNITY_SERVICE_SLIP} sends the player to work shifts.
 * Each completed {@value #COMMUNITY_SERVICE_SHIFT_SECONDS}-second shift grants
 * Notoriety −{@value #COMMUNITY_SERVICE_NOTORIETY_REDUCTION}.
 * Skipping a shift: Notoriety +{@value #SKIP_SHIFT_NOTORIETY} and an immediate warrant.
 *
 * <h3>Bribery &amp; Manipulation</h3>
 * Bribe the prosecutor, intimidate witnesses via WitnessSystem, or use a
 * {@link Material#FORGED_DOCUMENT} from FenceSystem to swap a serious charge for a minor
 * public order offence.
 */
public class MagistratesCourtSystem {

    // ── Scheduling constants ──────────────────────────────────────────────────

    /** In-game days from arrest to scheduled court date. */
    public static final int SUMMONS_DAYS_AHEAD = 3;

    /** Hour that court sessions begin. */
    public static final float COURT_OPEN_HOUR = 10.0f;

    /** Hour that court sessions close. */
    public static final float COURT_CLOSE_HOUR = 17.0f;

    // ── Notoriety / penalty constants ─────────────────────────────────────────

    /** Notoriety added when the player fails to appear. */
    public static final int FAILURE_TO_APPEAR_NOTORIETY = 10;

    /** Notoriety reduction per completed community service shift. */
    public static final int COMMUNITY_SERVICE_NOTORIETY_REDUCTION = 5;

    /** Notoriety added when the player skips a community service shift. */
    public static final int SKIP_SHIFT_NOTORIETY = 8;

    /** Notoriety added for contempt of court. */
    public static final int CONTEMPT_NOTORIETY = 15;

    // ── Bribery constants ─────────────────────────────────────────────────────

    /** COIN cost to bribe the CPS Prosecutor to drop a charge. */
    public static final int BRIBE_PROSECUTOR_COST = 20;

    /** COIN cost to engage the Duty Solicitor for a one-tier sentence reduction. */
    public static final int DUTY_SOLICITOR_COST = 5;

    /** COIN cost to bribe the Court Usher for a one-hour delay. */
    public static final int BRIBE_USHER_COST = 3;

    /** Probability (0–1) that a FORGED_DOCUMENT is detected. */
    public static final float FORGED_DOCUMENT_DETECTION_CHANCE = 0.15f;

    // ── Fine constants ────────────────────────────────────────────────────────

    /** Base fine (COIN) for a FINE sentence. Scaled by criminal record total. */
    public static final int BASE_FINE = 10;

    /** Scaling factor: +1 COIN per this many total crimes on record. */
    public static final int FINE_CRIME_SCALING = 5;

    // ── Community Service constants ───────────────────────────────────────────

    /** Number of community service shifts assigned per CS sentence. */
    public static final int COMMUNITY_SERVICE_SHIFTS = 4;

    /** Real-time seconds per community service shift (10 minutes). */
    public static final float COMMUNITY_SERVICE_SHIFT_SECONDS = 600f;

    // ── Sentence tiers ────────────────────────────────────────────────────────

    /**
     * Possible outcomes from a magistrates' court hearing, in order of severity.
     */
    public enum SentenceTier {
        /** Absolute Discharge — charges considered but no further action. */
        CONDITIONAL_CAUTION,
        /** Financial penalty; amount scales with criminal record. */
        FINE,
        /** Community service order. */
        COMMUNITY_SERVICE,
        /** Sentence suspended — next offence automatically triggers custodial. */
        SUSPENDED_SENTENCE,
        /** Custodial sentence: 24-hour lock-out, empty inventory on release. */
        CUSTODIAL
    }

    /**
     * Possible plea options for the player at the hearing.
     */
    public enum Plea {
        GUILTY,
        NOT_GUILTY
    }

    /**
     * Result of a complete court hearing.
     */
    public enum HearingResult {
        DISCHARGED,
        FINED,
        COMMUNITY_SERVICE_ORDERED,
        SUSPENDED_SENTENCE_GIVEN,
        CUSTODIAL_SENTENCED,
        /** Hearing dismissed — prosecution offered no evidence. */
        NO_CASE_TO_ANSWER,
        /** Usher-bribed delay applied; hearing rescheduled. */
        HEARING_DELAYED,
        /** Charge dropped after successful prosecutor bribery. */
        CHARGE_DROPPED,
        /** Player failed to appear for their scheduled hearing. */
        FAILED_TO_APPEAR
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Whether a summons is currently active (player has pending court date). */
    private boolean summonsActive = false;

    /** Day number on which the court hearing is scheduled. */
    private int scheduledCourtDay = -1;

    /** Whether the Duty Solicitor has been engaged for the current case. */
    private boolean dutySolicitorEngaged = false;

    /** Whether the Prosecutor has been bribed to drop the current charge. */
    private boolean prosecutorBribed = false;

    /** Whether the Usher has been bribed to delay the current session. */
    private boolean usherBribed = false;

    /** Whether a forged document has been presented for the current hearing. */
    private boolean forgedDocumentPresented = false;

    /** Number of community service shifts remaining on the current order. */
    private int communityServiceShiftsRemaining = 0;

    /** Accumulated elapsed seconds for the current community service shift. */
    private float currentShiftElapsed = 0f;

    /** Whether the player is currently serving a community service shift. */
    private boolean shiftInProgress = false;

    /** The CrimeType that triggered the current summons (may be null). */
    private CriminalRecord.CrimeType pendingCharge = null;

    // ── Dependencies ──────────────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private WitnessSystem witnessSystem;
    private RumourNetwork rumourNetwork;

    private final Random random;

    // ── Constructor ───────────────────────────────────────────────────────────

    public MagistratesCourtSystem(Random random) {
        this.random = random;
    }

    // ── Dependency setters ────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setWitnessSystem(WitnessSystem witnessSystem) {
        this.witnessSystem = witnessSystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    // ── Summons issuance ──────────────────────────────────────────────────────

    /**
     * Issue a court summons to the player after a qualifying arrest.
     * Adds a {@link Material#COURT_SUMMONS} to the inventory and schedules
     * a court date {@value #SUMMONS_DAYS_AHEAD} days ahead.
     *
     * @param inventory   the player's inventory
     * @param currentDay  the current in-game day number
     * @param charge      the CrimeType that triggered the summons (may be null)
     * @return {@code true} if the summons was issued; {@code false} if one is already active
     */
    public boolean issueSummons(Inventory inventory, int currentDay, CriminalRecord.CrimeType charge) {
        if (summonsActive) {
            return false;
        }
        summonsActive = true;
        scheduledCourtDay = currentDay + SUMMONS_DAYS_AHEAD;
        pendingCharge = charge;
        dutySolicitorEngaged = false;
        prosecutorBribed = false;
        usherBribed = false;
        forgedDocumentPresented = false;
        inventory.addItem(Material.COURT_SUMMONS, 1);
        return true;
    }

    /**
     * Determine whether a given CrimeType qualifies for a court summons
     * (i.e. is non-minor). Minor offences receive only a caution on arrest.
     *
     * @param crimeType the crime to evaluate
     * @return {@code true} if the crime warrants a summons
     */
    public static boolean requiresSummons(CriminalRecord.CrimeType crimeType) {
        if (crimeType == null) return false;
        switch (crimeType) {
            case UNLICENSED_BUSKING:
            case TRESPASSING:
            case FARE_EVASION:
            case ANTISOCIAL_BEHAVIOUR:
            case DISTURBING_THE_PEACE:
                return false;
            default:
                return true;
        }
    }

    // ── Bribery actions ───────────────────────────────────────────────────────

    /**
     * Attempt to bribe the CPS Prosecutor (Martin Gale) to drop the current charge.
     * Costs {@value #BRIBE_PROSECUTOR_COST} COIN. Succeeds only if a summons is active.
     *
     * @param inventory  the player's inventory
     * @return {@code true} if the bribe was accepted
     */
    public boolean bribeProsecutor(Inventory inventory) {
        if (!summonsActive) return false;
        if (inventory.getItemCount(Material.COIN) < BRIBE_PROSECUTOR_COST) return false;
        inventory.removeItem(Material.COIN, BRIBE_PROSECUTOR_COST);
        prosecutorBribed = true;
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.BENT_BRIEF);
        }
        return true;
    }

    /**
     * Engage the Duty Solicitor (Donna) to reduce the sentence tier by one.
     * Costs {@value #DUTY_SOLICITOR_COST} COIN. Succeeds only if a summons is active.
     *
     * @param inventory  the player's inventory
     * @return {@code true} if the solicitor was engaged
     */
    public boolean engageDutySolicitor(Inventory inventory) {
        if (!summonsActive) return false;
        if (dutySolicitorEngaged) return false;
        if (inventory.getItemCount(Material.COIN) < DUTY_SOLICITOR_COST) return false;
        inventory.removeItem(Material.COIN, DUTY_SOLICITOR_COST);
        dutySolicitorEngaged = true;
        return true;
    }

    /**
     * Bribe the Court Usher (Trevor) to delay the session by one in-game hour.
     * Costs {@value #BRIBE_USHER_COST} COIN. Succeeds only if a summons is active.
     *
     * @param inventory  the player's inventory
     * @return {@code true} if the delay was accepted
     */
    public boolean bribeUsher(Inventory inventory) {
        if (!summonsActive) return false;
        if (usherBribed) return false;
        if (inventory.getItemCount(Material.COIN) < BRIBE_USHER_COST) return false;
        inventory.removeItem(Material.COIN, BRIBE_USHER_COST);
        usherBribed = true;
        return true;
    }

    /**
     * Present a {@link Material#FORGED_DOCUMENT} at the hearing to swap a serious charge
     * for a minor public order offence. Consumes the item.
     * {@value #FORGED_DOCUMENT_DETECTION_CHANCE} chance of detection →
     * {@link CriminalRecord.CrimeType#PERVERTING_COURSE_OF_JUSTICE} added.
     *
     * @param inventory  the player's inventory
     * @return {@code true} if the document was accepted (not detected)
     */
    public boolean presentForgedDocument(Inventory inventory) {
        if (!summonsActive) return false;
        if (inventory.getItemCount(Material.FORGED_DOCUMENT) < 1) return false;
        inventory.removeItem(Material.FORGED_DOCUMENT, 1);

        if (random.nextFloat() < FORGED_DOCUMENT_DETECTION_CHANCE) {
            // Caught — add perverting charge and notoriety penalty
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.PERVERTING_COURSE_OF_JUSTICE);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(15, null);
            }
            return false;
        }
        forgedDocumentPresented = true;
        return true;
    }

    // ── Hearing resolution ────────────────────────────────────────────────────

    /**
     * Resolve a court hearing given the player's plea.
     *
     * <p>Flow:
     * <ol>
     *   <li>If prosecutor was bribed → {@link HearingResult#CHARGE_DROPPED}.</li>
     *   <li>If usher-bribed delay pending → {@link HearingResult#HEARING_DELAYED}.</li>
     *   <li>Determine base sentence tier from criminal record severity.</li>
     *   <li>If forged document presented → downgrade charge to minor.</li>
     *   <li>NOT_GUILTY plea + WitnessSystem shows no evidence → {@link HearingResult#NO_CASE_TO_ANSWER}.</li>
     *   <li>Duty Solicitor engaged → reduce sentence tier by one.</li>
     *   <li>Apply sentence: update inventory, notoriety, criminal record, achievements.</li>
     * </ol>
     *
     * @param player    the player
     * @param inventory the player's inventory
     * @param plea      the player's plea
     * @return the hearing result
     */
    public HearingResult resolveHearing(Player player, Inventory inventory, Plea plea) {
        if (!summonsActive) return HearingResult.NO_CASE_TO_ANSWER;

        // Achievement: first time in the dock
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.FIRST_OFFENCE);
        }

        // Charge dropped via bribery
        if (prosecutorBribed) {
            clearSummons(inventory);
            return HearingResult.CHARGE_DROPPED;
        }

        // Usher delay — reschedule for next day
        if (usherBribed) {
            usherBribed = false;
            scheduledCourtDay++;
            return HearingResult.HEARING_DELAYED;
        }

        // Determine base sentence tier
        SentenceTier tier = computeBaseSentenceTier();

        // Forged document downgrades charge to minor
        if (forgedDocumentPresented) {
            tier = SentenceTier.CONDITIONAL_CAUTION;
        }

        // NOT_GUILTY plea — check for evidence (any active evidence props or CCTV)
        if (plea == Plea.NOT_GUILTY) {
            boolean evidenceExists = witnessSystem != null && witnessSystem.getActiveEvidenceCount() > 0;
            if (!evidenceExists) {
                clearSummons(inventory);
                if (achievementSystem != null) {
                    achievementSystem.unlock(AchievementType.NOT_GUILTY);
                }
                return HearingResult.NO_CASE_TO_ANSWER;
            }
            // Evidence exists — guilty verdict, downgrade tier by one (benefit of doubt for contesting)
            tier = downgradeTier(tier);
        }

        // Duty solicitor reduces by one further tier
        if (dutySolicitorEngaged) {
            tier = downgradeTier(tier);
        }

        // Apply sentence
        HearingResult result = applySentence(player, inventory, tier);
        clearSummons(inventory);
        return result;
    }

    /**
     * Handle the case where the player did not attend their scheduled hearing.
     * Adds Notoriety +{@value #FAILURE_TO_APPEAR_NOTORIETY} and a
     * {@link CriminalRecord.CrimeType#FAILURE_TO_APPEAR} record entry.
     *
     * @param inventory the player's inventory
     */
    public void handleFailureToAppear(Inventory inventory) {
        if (!summonsActive) return;
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(FAILURE_TO_APPEAR_NOTORIETY, null);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.FAILURE_TO_APPEAR);
        }
        clearSummons(inventory);
    }

    /**
     * Check whether the player has missed their court date and apply consequences.
     * Should be called during the daily time update.
     *
     * @param inventory  the player's inventory
     * @param currentDay the current in-game day number
     * @return {@code true} if a failure-to-appear was processed
     */
    public boolean checkFailureToAppear(Inventory inventory, int currentDay) {
        if (!summonsActive) return false;
        if (currentDay > scheduledCourtDay) {
            handleFailureToAppear(inventory);
            return true;
        }
        return false;
    }

    // ── Community Service ─────────────────────────────────────────────────────

    /**
     * Begin a community service shift. The player must hold a
     * {@link Material#COMMUNITY_SERVICE_SLIP} and have shifts remaining.
     *
     * @param inventory the player's inventory
     * @return {@code true} if the shift started successfully
     */
    public boolean startCommunityServiceShift(Inventory inventory) {
        if (communityServiceShiftsRemaining <= 0) return false;
        if (inventory.getItemCount(Material.COMMUNITY_SERVICE_SLIP) < 1) return false;
        shiftInProgress = true;
        currentShiftElapsed = 0f;
        return true;
    }

    /**
     * Update the active community service shift. Call each frame with delta time
     * while a shift is in progress.
     *
     * @param delta   seconds since last frame
     * @return {@code true} if the shift just completed this frame
     */
    public boolean updateCommunityServiceShift(float delta) {
        if (!shiftInProgress) return false;
        currentShiftElapsed += delta;
        if (currentShiftElapsed >= COMMUNITY_SERVICE_SHIFT_SECONDS) {
            completeShift();
            return true;
        }
        return false;
    }

    /**
     * Apply consequences for skipping a community service shift.
     * Adds Notoriety +{@value #SKIP_SHIFT_NOTORIETY} and records
     * {@link CriminalRecord.CrimeType#FAILURE_TO_APPEAR}.
     */
    public void skipCommunityServiceShift() {
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(SKIP_SHIFT_NOTORIETY, null);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.FAILURE_TO_APPEAR);
        }
        shiftInProgress = false;
        currentShiftElapsed = 0f;
    }

    /**
     * Handle contempt of court (e.g. player shouts in the courtroom or attacks an NPC).
     * Adds Notoriety +{@value #CONTEMPT_NOTORIETY} and unlocks the achievement.
     */
    public void contemptOfCourt() {
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(CONTEMPT_NOTORIETY, null);
        }
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.CONTEMPT_OF_COURT);
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public boolean isSummonsActive() {
        return summonsActive;
    }

    public int getScheduledCourtDay() {
        return scheduledCourtDay;
    }

    public boolean isDutySolicitorEngaged() {
        return dutySolicitorEngaged;
    }

    public boolean isProsecutorBribed() {
        return prosecutorBribed;
    }

    public boolean isUsherBribed() {
        return usherBribed;
    }

    public boolean isForgedDocumentPresented() {
        return forgedDocumentPresented;
    }

    public int getCommunityServiceShiftsRemaining() {
        return communityServiceShiftsRemaining;
    }

    public boolean isShiftInProgress() {
        return shiftInProgress;
    }

    public float getCurrentShiftElapsed() {
        return currentShiftElapsed;
    }

    public CriminalRecord.CrimeType getPendingCharge() {
        return pendingCharge;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Compute the base sentence tier based on the current criminal record.
     * Heavier records → higher starting tier.
     */
    private SentenceTier computeBaseSentenceTier() {
        if (criminalRecord == null) return SentenceTier.FINE;
        int total = criminalRecord.getTotalCrimes();
        if (total <= 2)  return SentenceTier.CONDITIONAL_CAUTION;
        if (total <= 5)  return SentenceTier.FINE;
        if (total <= 10) return SentenceTier.COMMUNITY_SERVICE;
        if (total <= 20) return SentenceTier.SUSPENDED_SENTENCE;
        return SentenceTier.CUSTODIAL;
    }

    /**
     * Reduce a sentence tier by one step (minimum CONDITIONAL_CAUTION).
     */
    private SentenceTier downgradeTier(SentenceTier tier) {
        switch (tier) {
            case CUSTODIAL:           return SentenceTier.SUSPENDED_SENTENCE;
            case SUSPENDED_SENTENCE:  return SentenceTier.COMMUNITY_SERVICE;
            case COMMUNITY_SERVICE:   return SentenceTier.FINE;
            case FINE:                return SentenceTier.CONDITIONAL_CAUTION;
            default:                  return SentenceTier.CONDITIONAL_CAUTION;
        }
    }

    /**
     * Apply the chosen sentence tier to the player/inventory and return the
     * corresponding {@link HearingResult}.
     */
    private HearingResult applySentence(Player player, Inventory inventory, SentenceTier tier) {
        switch (tier) {
            case CONDITIONAL_CAUTION:
                return HearingResult.DISCHARGED;

            case FINE: {
                int fine = computeFine();
                int available = inventory.getItemCount(Material.COIN);
                int deducted = Math.min(fine, available);
                if (deducted > 0) {
                    inventory.removeItem(Material.COIN, deducted);
                }
                return HearingResult.FINED;
            }

            case COMMUNITY_SERVICE: {
                communityServiceShiftsRemaining = COMMUNITY_SERVICE_SHIFTS;
                inventory.addItem(Material.COMMUNITY_SERVICE_SLIP, 1);
                return HearingResult.COMMUNITY_SERVICE_ORDERED;
            }

            case SUSPENDED_SENTENCE:
                return HearingResult.SUSPENDED_SENTENCE_GIVEN;

            case CUSTODIAL: {
                // 24-hour lock-out: confiscate entire inventory
                clearInventory(inventory);
                if (achievementSystem != null) {
                    achievementSystem.unlock(AchievementType.CUSTODIAL);
                }
                return HearingResult.CUSTODIAL_SENTENCED;
            }

            default:
                return HearingResult.DISCHARGED;
        }
    }

    /** Calculate the fine amount based on the criminal record total. */
    private int computeFine() {
        int total = (criminalRecord != null) ? criminalRecord.getTotalCrimes() : 0;
        return BASE_FINE + (total / FINE_CRIME_SCALING);
    }

    /** Complete the current community service shift: reduce counter, apply notoriety reduction. */
    private void completeShift() {
        shiftInProgress = false;
        currentShiftElapsed = 0f;
        communityServiceShiftsRemaining = Math.max(0, communityServiceShiftsRemaining - 1);
        if (notorietySystem != null) {
            notorietySystem.reduceNotoriety(COMMUNITY_SERVICE_NOTORIETY_REDUCTION, null);
        }
        if (achievementSystem != null) {
            achievementSystem.increment(AchievementType.COMMUNITY_SERVICE_HERO);
        }
    }

    /** Clear all summons state and remove the COURT_SUMMONS item from inventory. */
    private void clearSummons(Inventory inventory) {
        summonsActive = false;
        scheduledCourtDay = -1;
        pendingCharge = null;
        dutySolicitorEngaged = false;
        prosecutorBribed = false;
        usherBribed = false;
        forgedDocumentPresented = false;
        if (inventory.getItemCount(Material.COURT_SUMMONS) > 0) {
            inventory.removeItem(Material.COURT_SUMMONS, 1);
        }
    }

    /** Remove all items from the inventory (custodial sentence). */
    private void clearInventory(Inventory inventory) {
        for (Material m : Material.values()) {
            int count = inventory.getItemCount(m);
            if (count > 0) {
                inventory.removeItem(m, count);
            }
        }
    }
}

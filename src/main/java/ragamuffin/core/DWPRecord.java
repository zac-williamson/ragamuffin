package ragamuffin.core;

/**
 * Issue #1188: Data class tracking the player's DWP compliance state.
 *
 * <p>Holds the active sanction tier, how many in-game weeks remain on the
 * sanction, the running estimate of undeclared earnings (used for the fine
 * calculation at CRIMINAL_REFERRAL), and appeal status.
 *
 * <p>A single instance is owned by {@link DWPSystem}.
 */
public class DWPRecord {

    /** Default UC payment per fortnight when no sanction is active. */
    public static final int DEFAULT_UC_PAYMENT = 20;

    /** UC reduction percentage for WARNING tier (20%). */
    public static final float WARNING_UC_REDUCTION = 0.20f;

    /**
     * Sanction tiers applied by the DWP compliance system.
     */
    public enum SanctionTier {
        /** No active sanction. UC paid at full rate. */
        NONE,
        /** First suspected fraud, low evidence. UC reduced 20% for 4 in-game weeks. */
        WARNING,
        /** Suspicion ≥ 70 at sanction. UC suspended for 8 in-game weeks. */
        SUSPENSION,
        /**
         * Suspicion ≥ 90 + evidence found. BENEFIT_FRAUD CrimeType logged.
         * MagistratesCourtSystem prosecution; fine = 3× undeclared earnings.
         * Permanent record entry.
         */
        CRIMINAL_REFERRAL
    }

    /** Appeal status for an in-progress or resolved appeal. */
    public enum AppealStatus {
        /** No appeal in progress. */
        NONE,
        /** Appeal submitted; awaiting 2-week resolution. */
        PENDING,
        /** Appeal was successful; sanction cleared. */
        UPHELD,
        /** Appeal failed; sanction escalated. */
        REJECTED
    }

    // ── State fields ──────────────────────────────────────────────────────────

    private SanctionTier sanctionTier = SanctionTier.NONE;

    /** In-game weeks remaining on the current sanction. 0 if NONE. */
    private int weeksRemaining = 0;

    /**
     * Running estimate of undeclared earnings (COIN) since last sign-on with
     * full declaration. Used to compute the fine at CRIMINAL_REFERRAL tier
     * (fine = 3 × undeclaredEarningsEstimate).
     */
    private int undeclaredEarningsEstimate = 0;

    private AppealStatus appealStatus = AppealStatus.NONE;

    /** In-game day the appeal was submitted (used for 2-week resolution). */
    private int appealSubmittedDay = -1;

    /** Whether an appeal letter has been submitted (in progress). */
    private boolean appealPending = false;

    // ── Sanction management ───────────────────────────────────────────────────

    /**
     * Apply or escalate the sanction tier.
     *
     * <p>If the current tier is lower than the requested tier, the tier is
     * upgraded and the appropriate duration is set. If already at
     * CRIMINAL_REFERRAL, this is a no-op.
     *
     * @param tier the tier to apply
     */
    public void applySanction(SanctionTier tier) {
        if (tier.ordinal() > sanctionTier.ordinal()) {
            sanctionTier = tier;
            switch (tier) {
                case WARNING:           weeksRemaining = 4;  break;
                case SUSPENSION:        weeksRemaining = 8;  break;
                case CRIMINAL_REFERRAL: weeksRemaining = 0;  break; // permanent
                default: break;
            }
        }
    }

    /**
     * Tick down the sanction duration by one in-game week.
     * If weeksRemaining reaches 0 and sanction is not CRIMINAL_REFERRAL,
     * the tier is reset to NONE.
     */
    public void tickWeek() {
        if (sanctionTier == SanctionTier.NONE || sanctionTier == SanctionTier.CRIMINAL_REFERRAL) {
            return;
        }
        if (weeksRemaining > 0) {
            weeksRemaining--;
        }
        if (weeksRemaining <= 0) {
            sanctionTier = SanctionTier.NONE;
        }
    }

    /**
     * Clear the active sanction (used after a successful appeal).
     */
    public void clearSanction() {
        sanctionTier = SanctionTier.NONE;
        weeksRemaining = 0;
    }

    /**
     * Compute the current UC payment based on the active sanction tier.
     *
     * @return COIN per fortnight
     */
    public int computeUCPayment() {
        switch (sanctionTier) {
            case WARNING:
                return Math.round(DEFAULT_UC_PAYMENT * (1f - WARNING_UC_REDUCTION));
            case SUSPENSION:
            case CRIMINAL_REFERRAL:
                return 0;
            default:
                return DEFAULT_UC_PAYMENT;
        }
    }

    // ── Undeclared earnings ───────────────────────────────────────────────────

    /**
     * Accumulate undeclared earnings (i.e. income not reported to the DWP).
     *
     * @param coin the amount earned without declaring
     */
    public void addUndeclaredEarnings(int coin) {
        if (coin > 0) {
            undeclaredEarningsEstimate += coin;
        }
    }

    /** Reset the undeclared earnings estimate (e.g. after full declaration). */
    public void clearUndeclaredEarnings() {
        undeclaredEarningsEstimate = 0;
    }

    /**
     * Compute the criminal fine (3× undeclared earnings, minimum 0).
     */
    public int computeCriminalFine() {
        return undeclaredEarningsEstimate * 3;
    }

    // ── Appeal management ─────────────────────────────────────────────────────

    /**
     * Submit an appeal. No-op if no sanction is active or an appeal is already pending.
     *
     * @param currentDay the in-game day of submission
     * @return true if the appeal was accepted for processing
     */
    public boolean submitAppeal(int currentDay) {
        if (sanctionTier == SanctionTier.NONE) return false;
        if (appealPending) return false;
        appealPending = true;
        appealStatus = AppealStatus.PENDING;
        appealSubmittedDay = currentDay;
        return true;
    }

    /**
     * Resolve the appeal after 2 in-game weeks (14 days).
     *
     * @param currentDay the current in-game day
     * @param success    true if the appeal succeeds
     */
    public void resolveAppeal(int currentDay, boolean success) {
        if (!appealPending) return;
        if (currentDay < appealSubmittedDay + 14) return; // not yet resolved

        appealPending = false;
        if (success) {
            appealStatus = AppealStatus.UPHELD;
            clearSanction();
        } else {
            appealStatus = AppealStatus.REJECTED;
            // Escalate: WARNING → SUSPENSION, SUSPENSION → CRIMINAL_REFERRAL
            if (sanctionTier == SanctionTier.WARNING) {
                applySanction(SanctionTier.SUSPENSION);
            } else if (sanctionTier == SanctionTier.SUSPENSION) {
                applySanction(SanctionTier.CRIMINAL_REFERRAL);
            }
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public SanctionTier getSanctionTier() { return sanctionTier; }

    public int getWeeksRemaining() { return weeksRemaining; }

    public int getUndeclaredEarningsEstimate() { return undeclaredEarningsEstimate; }

    public AppealStatus getAppealStatus() { return appealStatus; }

    public boolean isAppealPending() { return appealPending; }

    public int getAppealSubmittedDay() { return appealSubmittedDay; }

    public boolean hasSanction() { return sanctionTier != SanctionTier.NONE; }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /** Force-set sanction tier (for testing). */
    public void setSanctionTierForTesting(SanctionTier tier) {
        this.sanctionTier = tier;
    }

    /** Force-set weeks remaining (for testing). */
    public void setWeeksRemainingForTesting(int weeks) {
        this.weeksRemaining = weeks;
    }

    /** Force-set undeclared earnings (for testing). */
    public void setUndeclaredEarningsForTesting(int amount) {
        this.undeclaredEarningsEstimate = amount;
    }

    /** Force-set appeal pending (for testing). */
    public void setAppealPendingForTesting(boolean pending, int submittedDay) {
        this.appealPending = pending;
        if (pending) {
            this.appealStatus = AppealStatus.PENDING;
            this.appealSubmittedDay = submittedDay;
        }
    }
}

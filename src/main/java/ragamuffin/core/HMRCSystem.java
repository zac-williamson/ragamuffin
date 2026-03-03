package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.core.NotorietySystem.AchievementCallback;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.Random;

/**
 * Issue #1359: Northfield HMRC Tax Investigation — The Cash-in-Hand Reckoning,
 * the Dawn Raid &amp; the Offshore Escape.
 *
 * <h3>Overview</h3>
 * HMRC Inspector Sandra Watts investigates the player's cash-in-hand economy.
 * When {@code totalUntaxedEarnings >= EARNINGS_THRESHOLD} (150 COIN), Sandra
 * spawns at the player's address at 09:00 on a weekday and attempts to serve a
 * {@link Material#TAX_DEMAND_LETTER} (30% of untaxed earnings, capped at
 * {@link #DEMAND_CAP}).
 *
 * <h3>Resolution paths</h3>
 * <ul>
 *   <li><b>Pay up</b> — COIN deducted, {@link Material#CLEAN_BILL_OF_HEALTH}
 *       issued, 7-day immunity, {@link AchievementType#TAX_COMPLIANT}.</li>
 *   <li><b>CitizensAdvice letter</b> — demand reduced 40%,
 *       {@link AchievementType#KNOWS_HIS_RIGHTS}.</li>
 *   <li><b>Bribe Sandra</b> — 60% success seeds {@code BENT_OFFICIAL} rumour;
 *       40% failure logs {@link CrimeType#BRIBERY_OF_PUBLIC_OFFICIAL} + Wanted +2.</li>
 *   <li><b>Hide COIN in lockup safe</b> — excluded from demand calculation.</li>
 *   <li><b>Ignore</b> — Derek (DISTRAINT_OFFICER) seizes goods after 2 days;
 *       dawn raid on day 5 at 06:30 with 2× inspectors + enforcement officer.</li>
 * </ul>
 *
 * <h3>Loan shark tip-off</h3>
 * The {@code LOAN_SHARK_CLERK} can tip off HMRC below the 150 COIN threshold
 * if the player owes debt, seeding a {@link RumourType#HMRC_TIPPED_OFF} rumour.
 */
public class HMRCSystem {

    // ── Thresholds and demand constants ──────────────────────────────────────

    /** Untaxed earnings (in COIN) required to trigger Sandra's visit. */
    public static final int EARNINGS_THRESHOLD = 150;

    /** Tax demand rate: 30% of untaxed earnings. */
    public static final float DEMAND_RATE = 0.30f;

    /** Maximum tax demand amount (cap), in COIN. */
    public static final int DEMAND_CAP = 80;

    /** CitizensAdvice discount: reduces demand by this fraction. */
    public static final float CITIZENS_ADVICE_DISCOUNT = 0.40f;

    /** Probability that bribing Sandra succeeds (0.0–1.0). */
    public static final float BRIBE_SUCCESS_CHANCE = 0.60f;

    /** In-game days after serving the demand before Derek (distraint) appears. */
    public static final int DISTRAINT_DAYS = 2;

    /** In-game days after serving the demand before the dawn raid. */
    public static final int DAWN_RAID_DAYS = 5;

    /** In-game hour at which Sandra spawns on a weekday (09:00). */
    public static final float SANDRA_SPAWN_HOUR = 9.0f;

    /** In-game hour at which the dawn raid begins (06:30). */
    public static final float DAWN_RAID_HOUR = 6.5f;

    /** In-game days of immunity after paying the full demand. */
    public static final int IMMUNITY_DAYS = 7;

    /** Notoriety gained when bribe fails. */
    public static final int BRIBE_FAIL_NOTORIETY = 10;

    /** WantedSystem stars added when bribe fails. */
    public static final int BRIBE_FAIL_WANTED_STARS = 2;

    /** Notoriety added to the neighbourhood on TAX_EVASION crime. */
    public static final int EVASION_NOTORIETY = 8;

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Result of attempting to pay the tax demand. */
    public enum PaymentResult {
        /** Demand paid in full; CLEAN_BILL_OF_HEALTH issued; 7-day immunity. */
        PAID,
        /** Player does not have enough COIN to pay. */
        INSUFFICIENT_FUNDS,
        /** No active demand to pay. */
        NO_DEMAND
    }

    /** Result of using a CitizensAdvice letter against the demand. */
    public enum AdviceResult {
        /** Demand successfully reduced by 40%. */
        REDUCED,
        /** No active demand to challenge. */
        NO_DEMAND,
        /** Player has no DEBT_ADVICE_LETTER in inventory. */
        NO_LETTER
    }

    /** Result of attempting to bribe Sandra. */
    public enum BribeResult {
        /** Bribe accepted; BENT_OFFICIAL rumour seeded; demand dropped. */
        SUCCESS,
        /** Bribe rejected; BRIBERY_OF_PUBLIC_OFFICIAL logged; Wanted +2. */
        FAILURE,
        /** No active demand / Sandra is not present. */
        NO_DEMAND,
        /** Player has no CASH_BRIBE_ENVELOPE in inventory. */
        NO_ENVELOPE
    }

    /** State of the HMRC investigation. */
    public enum InvestigationState {
        /** No active investigation. */
        INACTIVE,
        /** Sandra has been spawned; demand not yet served. */
        SANDRA_SPAWNED,
        /** Demand has been served; player must respond. */
        DEMAND_SERVED,
        /** Distraint officer Derek is active (day 2+). */
        DISTRAINT_ACTIVE,
        /** Dawn raid in progress (day 5+). */
        DAWN_RAID_ACTIVE,
        /** Investigation resolved (paid, bribed, or advised). */
        RESOLVED
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Total untaxed earnings accumulated from all sources. */
    private int totalUntaxedEarnings = 0;

    /** COIN hidden in the lockup safe (excluded from demand calculation). */
    private int lockupSafeCoin = 0;

    /** Current investigation state. */
    private InvestigationState state = InvestigationState.INACTIVE;

    /** The calculated tax demand amount (in COIN). */
    private int demandAmount = 0;

    /** In-game day on which the demand was served. */
    private int demandServedDay = -1;

    /** In-game day on which immunity expires (−1 = no immunity). */
    private int immunityExpiryDay = -1;

    /** Whether the CLEAN_BILL_OF_HEALTH has been issued this investigation. */
    private boolean cleanBillIssued = false;

    /** Whether a dawn raid escape achievement has been awarded already. */
    private boolean dawnRaidEscapeAwarded = false;

    // ── Construction ──────────────────────────────────────────────────────────

    public HMRCSystem() {
        this(new Random());
    }

    public HMRCSystem(Random random) {
        this.random = random;
    }

    // ── Earnings tracking ─────────────────────────────────────────────────────

    /**
     * Notify the HMRC system that the player has earned untaxed coin from a
     * cash-in-hand source (FenceSystem, StallSystem, BuskingSystem,
     * BootSaleSystem, or dodgy EmploymentSystem jobs).
     *
     * <p>If the total untaxed earnings reach or exceed {@link #EARNINGS_THRESHOLD}
     * and the investigation is currently inactive, the state transitions to
     * {@link InvestigationState#SANDRA_SPAWNED} so the caller can schedule
     * Sandra's visit at 09:00 on the next weekday.
     *
     * @param amount  coin earned (must be &gt; 0)
     * @param currentDay current in-game day
     */
    public void onUntaxedEarning(int amount, int currentDay) {
        if (amount <= 0) return;
        totalUntaxedEarnings += amount;

        if (state == InvestigationState.INACTIVE
                && totalUntaxedEarnings >= EARNINGS_THRESHOLD
                && !isImmune(currentDay)) {
            state = InvestigationState.SANDRA_SPAWNED;
        }
    }

    /**
     * Notify the HMRC system that the loan shark (LOAN_SHARK_CLERK) has tipped
     * off HMRC below the earnings threshold. Seeds a
     * {@link RumourType#HMRC_TIPPED_OFF} rumour and triggers Sandra early.
     *
     * @param sourceNpc  the NPC who made the tip (may be null)
     * @param rumourNetwork  the rumour network (may be null)
     * @param currentDay current in-game day
     */
    public void onLoanSharkTipOff(NPC sourceNpc, RumourNetwork rumourNetwork,
                                  int currentDay) {
        if (rumourNetwork != null && sourceNpc != null) {
            rumourNetwork.addRumour(sourceNpc,
                new Rumour(RumourType.HMRC_TIPPED_OFF,
                    "Heard someone tipped off the taxman about a mate's dealings " +
                    "— that loan shark's been busy."));
        }
        if (state == InvestigationState.INACTIVE && !isImmune(currentDay)) {
            state = InvestigationState.SANDRA_SPAWNED;
        }
    }

    // ── Day tick ──────────────────────────────────────────────────────────────

    /**
     * Per-day tick. Checks whether distraint or dawn-raid conditions have been
     * met; escalates state accordingly.
     *
     * <p>Call this once per in-game day from the game loop.
     *
     * @param currentDay          current in-game day
     * @param inventory           player's inventory (for distraint seizure)
     * @param criminalRecord      for logging TAX_EVASION
     * @param notorietySystem     for applying notoriety penalty
     * @param achievementCallback for achievement unlocks
     * @param rumourNetwork       for seeding TAX_TROUBLES rumour
     * @param witnessNpc          an NPC near the player (for rumour seeding; may be null)
     */
    public void onDayTick(int currentDay,
                          Inventory inventory,
                          CriminalRecord criminalRecord,
                          NotorietySystem notorietySystem,
                          AchievementCallback achievementCallback,
                          RumourNetwork rumourNetwork,
                          NPC witnessNpc) {
        if (state == InvestigationState.DEMAND_SERVED && demandServedDay >= 0) {
            int daysSinceDemand = currentDay - demandServedDay;

            if (daysSinceDemand >= DAWN_RAID_DAYS
                    && state != InvestigationState.DAWN_RAID_ACTIVE) {
                state = InvestigationState.DAWN_RAID_ACTIVE;
                // Log TAX_EVASION crime
                if (criminalRecord != null) {
                    criminalRecord.record(CrimeType.TAX_EVASION);
                }
                if (notorietySystem != null && achievementCallback != null) {
                    notorietySystem.addNotoriety(EVASION_NOTORIETY, achievementCallback);
                }
            } else if (daysSinceDemand >= DISTRAINT_DAYS
                    && state == InvestigationState.DEMAND_SERVED) {
                state = InvestigationState.DISTRAINT_ACTIVE;
                performDistraint(inventory, achievementCallback);
            }
        }

        // Seed TAX_TROUBLES rumour when Sandra first spawns
        if (state == InvestigationState.SANDRA_SPAWNED
                && rumourNetwork != null && witnessNpc != null) {
            rumourNetwork.addRumour(witnessNpc,
                new Rumour(RumourType.TAX_TROUBLES,
                    "Word is someone round here's had a visit from the taxman " +
                    "— paying cash in hand catches up with you eventually."));
        }
    }

    // ── Sandra visit ──────────────────────────────────────────────────────────

    /**
     * Sandra arrives at the player's address and serves the tax demand.
     * Transitions state to {@link InvestigationState#DEMAND_SERVED} and
     * calculates the demand amount (30% of taxable untaxed earnings, capped
     * at {@link #DEMAND_CAP}).
     *
     * <p>Taxable earnings = totalUntaxedEarnings − lockupSafeCoin (floored at 0).
     *
     * @param inventory  player's inventory (receives TAX_DEMAND_LETTER)
     * @param currentDay current in-game day
     * @return the demand amount in COIN, or 0 if Sandra cannot serve the demand
     */
    public int serveDemand(Inventory inventory, int currentDay) {
        if (state != InvestigationState.SANDRA_SPAWNED) return 0;

        int taxableEarnings = Math.max(0, totalUntaxedEarnings - lockupSafeCoin);
        demandAmount = Math.min(DEMAND_CAP,
                Math.round(taxableEarnings * DEMAND_RATE));

        if (demandAmount <= 0) {
            // No taxable earnings; close without demand
            state = InvestigationState.RESOLVED;
            return 0;
        }

        state = InvestigationState.DEMAND_SERVED;
        demandServedDay = currentDay;

        if (inventory != null) {
            inventory.addItem(Material.TAX_DEMAND_LETTER, 1);
        }

        return demandAmount;
    }

    // ── Player responses ──────────────────────────────────────────────────────

    /**
     * Pay the tax demand in full.
     *
     * <p>On success: deducts {@link #demandAmount} COIN from inventory, removes
     * the TAX_DEMAND_LETTER, adds a {@link Material#CLEAN_BILL_OF_HEALTH}, resets
     * untaxed earnings, sets 7-day immunity, and unlocks
     * {@link AchievementType#TAX_COMPLIANT}.
     *
     * @param inventory           player's inventory
     * @param currentDay          current in-game day
     * @param achievementCallback callback for achievement unlocks
     * @return result of the payment attempt
     */
    public PaymentResult payDemand(Inventory inventory, int currentDay,
                                   AchievementCallback achievementCallback) {
        if (state != InvestigationState.DEMAND_SERVED
                && state != InvestigationState.DISTRAINT_ACTIVE) {
            return PaymentResult.NO_DEMAND;
        }
        if (inventory.getItemCount(Material.COIN) < demandAmount) {
            return PaymentResult.INSUFFICIENT_FUNDS;
        }

        inventory.removeItem(Material.COIN, demandAmount);
        inventory.removeItem(Material.TAX_DEMAND_LETTER, 1);
        inventory.addItem(Material.CLEAN_BILL_OF_HEALTH, 1);

        demandAmount = 0;
        totalUntaxedEarnings = 0;
        lockupSafeCoin = 0;
        immunityExpiryDay = currentDay + IMMUNITY_DAYS;
        cleanBillIssued = true;
        state = InvestigationState.RESOLVED;

        if (!cleanBillIssued || achievementCallback != null) {
            achievementCallback.award(AchievementType.TAX_COMPLIANT);
        }

        return PaymentResult.PAID;
    }

    /**
     * Challenge the demand using a CitizensAdvice DEBT_ADVICE_LETTER.
     *
     * <p>On success: removes the DEBT_ADVICE_LETTER, reduces
     * {@link #demandAmount} by {@link #CITIZENS_ADVICE_DISCOUNT} (40%), and
     * unlocks {@link AchievementType#KNOWS_HIS_RIGHTS}.
     *
     * @param inventory           player's inventory
     * @param achievementCallback callback for achievement unlocks
     * @return result of the advice challenge
     */
    public AdviceResult challengeWithAdvice(Inventory inventory,
                                            AchievementCallback achievementCallback) {
        if (state != InvestigationState.DEMAND_SERVED) {
            return AdviceResult.NO_DEMAND;
        }
        if (inventory.getItemCount(Material.DEBT_ADVICE_LETTER) <= 0) {
            return AdviceResult.NO_LETTER;
        }

        inventory.removeItem(Material.DEBT_ADVICE_LETTER, 1);
        int reduction = Math.round(demandAmount * CITIZENS_ADVICE_DISCOUNT);
        demandAmount = Math.max(0, demandAmount - reduction);

        achievementCallback.award(AchievementType.KNOWS_HIS_RIGHTS);
        return AdviceResult.REDUCED;
    }

    /**
     * Attempt to bribe Sandra with a {@link Material#CASH_BRIBE_ENVELOPE}.
     *
     * <p>60% success: consumes the envelope, seeds {@link RumourType#BENT_OFFICIAL}
     * rumour, resolves the investigation, unlocks {@link AchievementType#GREASED_PALM}.
     * <p>40% failure: consumes the envelope, logs
     * {@link CrimeType#BRIBERY_OF_PUBLIC_OFFICIAL}, adds Wanted +2 stars,
     * Notoriety +{@value #BRIBE_FAIL_NOTORIETY}.
     *
     * @param inventory           player's inventory
     * @param criminalRecord      for logging crimes
     * @param wantedSystem        for adding wanted stars on failure
     * @param notorietySystem     for adding notoriety on failure
     * @param achievementCallback callback for achievement unlocks
     * @param rumourNetwork       for seeding rumours
     * @param witnessNpc          an NPC near the player for rumour seeding (may be null)
     * @return result of the bribe attempt
     */
    public BribeResult bribeSandra(Inventory inventory,
                                   CriminalRecord criminalRecord,
                                   WantedSystem wantedSystem,
                                   NotorietySystem notorietySystem,
                                   AchievementCallback achievementCallback,
                                   RumourNetwork rumourNetwork,
                                   NPC witnessNpc) {
        if (state != InvestigationState.DEMAND_SERVED) {
            return BribeResult.NO_DEMAND;
        }
        if (inventory.getItemCount(Material.CASH_BRIBE_ENVELOPE) <= 0) {
            return BribeResult.NO_ENVELOPE;
        }

        inventory.removeItem(Material.CASH_BRIBE_ENVELOPE, 1);

        boolean success = random.nextFloat() < BRIBE_SUCCESS_CHANCE;
        if (success) {
            demandAmount = 0;
            totalUntaxedEarnings = 0;
            state = InvestigationState.RESOLVED;

            // Seed BENT_OFFICIAL rumour if we have a witness NPC
            if (rumourNetwork != null && witnessNpc != null) {
                rumourNetwork.addRumour(witnessNpc,
                    new Rumour(RumourType.BENT_OFFICIAL,
                        "Sandra from the taxman took a brown envelope off someone " +
                        "on Northfield Road. Seen it with my own eyes."));
            }

            achievementCallback.award(AchievementType.GREASED_PALM);
            return BribeResult.SUCCESS;
        } else {
            // Bribe failed
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.BRIBERY_OF_PUBLIC_OFFICIAL);
            }
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(BRIBE_FAIL_WANTED_STARS, 0, 0, 0, null);
            }
            if (notorietySystem != null && achievementCallback != null) {
                notorietySystem.addNotoriety(BRIBE_FAIL_NOTORIETY, achievementCallback);
            }
            return BribeResult.FAILURE;
        }
    }

    // ── Lockup safe ───────────────────────────────────────────────────────────

    /**
     * Hide COIN in the lockup safe. Coin stashed here is excluded from the
     * tax demand calculation.
     *
     * @param amount amount of COIN to hide (must be &gt; 0)
     * @param inventory player's inventory (COIN will be removed)
     * @return true if successfully stashed; false if insufficient COIN
     */
    public boolean hideCoinInLockup(int amount, Inventory inventory) {
        if (amount <= 0) return false;
        if (inventory.getItemCount(Material.COIN) < amount) return false;
        inventory.removeItem(Material.COIN, amount);
        lockupSafeCoin += amount;
        return true;
    }

    /**
     * Retrieve COIN from the lockup safe back into inventory.
     *
     * @param amount amount to retrieve
     * @param inventory player's inventory (COIN will be added)
     * @return amount actually retrieved (may be less if safe has fewer coins)
     */
    public int retrieveCoinFromLockup(int amount, Inventory inventory) {
        int retrieved = Math.min(amount, lockupSafeCoin);
        if (retrieved <= 0) return 0;
        lockupSafeCoin -= retrieved;
        inventory.addItem(Material.COIN, retrieved);
        return retrieved;
    }

    // ── Distraint ─────────────────────────────────────────────────────────────

    /**
     * Derek seizes goods from the player's inventory up to the value of the
     * demand. Removes COIN first, then random items. Awards
     * {@link AchievementType#BAILED_ON}.
     *
     * @param inventory           player's inventory
     * @param achievementCallback callback for achievement unlocks
     */
    private void performDistraint(Inventory inventory,
                                  AchievementCallback achievementCallback) {
        if (inventory == null) {
            achievementCallback.award(AchievementType.BAILED_ON);
            return;
        }

        int remaining = demandAmount;
        // First seize COIN
        int coinAvailable = inventory.getItemCount(Material.COIN);
        int coinToSeize = Math.min(coinAvailable, remaining);
        if (coinToSeize > 0) {
            inventory.removeItem(Material.COIN, coinToSeize);
            remaining -= coinToSeize;
        }

        // Remove TAX_DEMAND_LETTER (it's been acted on)
        inventory.removeItem(Material.TAX_DEMAND_LETTER, 1);

        demandAmount = Math.max(0, remaining);
        achievementCallback.award(AchievementType.BAILED_ON);
    }

    // ── Dawn raid escape ──────────────────────────────────────────────────────

    /**
     * Called when the player successfully escapes the dawn raid via the
     * {@link ragamuffin.world.PropType#BACK_WINDOW_PROP}. Awards
     * {@link AchievementType#DAWN_RAID_SURVIVOR} and resolves the investigation.
     *
     * @param achievementCallback callback for achievement unlocks
     */
    public void onDawnRaidEscape(AchievementCallback achievementCallback) {
        if (!dawnRaidEscapeAwarded) {
            dawnRaidEscapeAwarded = true;
            achievementCallback.award(AchievementType.DAWN_RAID_SURVIVOR);
        }
        // Resolve the investigation after escape
        state = InvestigationState.RESOLVED;
        demandAmount = 0;
        totalUntaxedEarnings = 0;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Returns the current investigation state. */
    public InvestigationState getState() {
        return state;
    }

    /** Returns the current outstanding tax demand amount. */
    public int getDemandAmount() {
        return demandAmount;
    }

    /** Returns the total accumulated untaxed earnings. */
    public int getTotalUntaxedEarnings() {
        return totalUntaxedEarnings;
    }

    /** Returns the amount of COIN hidden in the lockup safe. */
    public int getLockupSafeCoin() {
        return lockupSafeCoin;
    }

    /**
     * Returns true if the player is currently immune from Sandra's visit
     * (7-day post-payment immunity window).
     *
     * @param currentDay current in-game day
     */
    public boolean isImmune(int currentDay) {
        return immunityExpiryDay >= 0 && currentDay < immunityExpiryDay;
    }

    /** Returns true if the investigation is currently inactive. */
    public boolean isInactive() {
        return state == InvestigationState.INACTIVE;
    }

    /** Returns true if Sandra has spawned but the demand has not yet been served. */
    public boolean isSandraSpawned() {
        return state == InvestigationState.SANDRA_SPAWNED;
    }

    /** Returns true if a demand has been served and is pending response. */
    public boolean isDemandPending() {
        return state == InvestigationState.DEMAND_SERVED
                || state == InvestigationState.DISTRAINT_ACTIVE;
    }

    /** Returns true if the dawn raid is currently active. */
    public boolean isDawnRaidActive() {
        return state == InvestigationState.DAWN_RAID_ACTIVE;
    }

    /**
     * Returns true if Sandra should spawn today.
     * Sandra visits at 09:00 on weekdays (Monday–Friday, days 0–4 mod 7).
     *
     * @param hour       current in-game hour (e.g. 9.0 = 09:00)
     * @param dayOfWeek  current day-of-week (0 = Monday … 6 = Sunday)
     */
    public boolean isSandraVisitTime(float hour, int dayOfWeek) {
        return state == InvestigationState.SANDRA_SPAWNED
                && dayOfWeek < 5
                && hour >= SANDRA_SPAWN_HOUR;
    }

    /**
     * Returns true if the dawn raid should begin today.
     * Dawn raid begins at 06:30 on day {@code demandServedDay + DAWN_RAID_DAYS}.
     *
     * @param hour       current in-game hour
     * @param currentDay current in-game day
     */
    public boolean isDawnRaidTime(float hour, int currentDay) {
        return state == InvestigationState.DAWN_RAID_ACTIVE
                && hour >= DAWN_RAID_HOUR
                && currentDay >= (demandServedDay + DAWN_RAID_DAYS);
    }
}

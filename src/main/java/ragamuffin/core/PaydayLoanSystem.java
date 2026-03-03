package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1418: Northfield QuickFix Loans — The 2000% APR Trap, the Debt Spiral &amp;
 * the Bailiff Run.
 *
 * <h3>Mechanic 1 — Taking Out a Loan</h3>
 * Three tiers (10 / 20 / 40 COIN). Base 50% interest, repayable within 3 in-game days.
 * Discounts applied at application time:
 * <ul>
 *   <li>Employment discount: −40% interest.</li>
 *   <li>DEBT_ADVICE_LETTER (from {@link CitizensAdviceSystem}): −20% interest (consumed).</li>
 *   <li>Marchetti FRIENDLY respect (≥ 75): loan cap doubled to 80 COIN.</li>
 * </ul>
 * One active loan at a time. {@link Material#LOAN_AGREEMENT} added to inventory.
 * Achievement: {@link AchievementType#FIRST_LOAN}.
 *
 * <h3>Mechanic 2 — Repayment</h3>
 * Press E on Darren ({@code LOAN_SHARK_CLERK}) while holding COIN. Repay principal + interest.
 * Early repayment (within 1 in-game day) grants 1 COIN cashback and seeds
 * {@link RumourType#LOAN_REPAID_RUMOUR}. Achievement: {@link AchievementType#DEBT_FREE}.
 *
 * <h3>Mechanic 3 — Debt Spiral</h3>
 * <ul>
 *   <li>Day 1 overdue: Darren becomes HOSTILE, Notoriety +2, {@link RumourType#LOAN_OVERDUE_RUMOUR}.</li>
 *   <li>Day 2 overdue: BAILIFF Terry spawns and pathfinds to the squat. Player options:
 *       pay, bribe (5 COIN → {@link RumourType#BRIBED_BAILIFF_RUMOUR}),
 *       attack ({@link WantedSystem} +2, {@link AchievementType#BOTTLED_THE_BAILIFF}),
 *       or scarper (loan doubles, {@link RumourType#SCARPERED_BAILIFF_RUMOUR}).</li>
 *   <li>Day 3 overdue: {@link CriminalRecord.CrimeType#LOAN_DEFAULTED}, WantedSystem +1,
 *       permanent ban. Achievement: {@link AchievementType#DEBT_SPIRAL}.</li>
 * </ul>
 *
 * <h3>Mechanic 4 — Back-Room Heist</h3>
 * After 3 loans Darren's barman seeds {@link RumourType#BARMAN_RUMOUR_LOAN_OFFICE}.
 * During Darren's lunch (12:30–13:00):
 * <ul>
 *   <li>CROWBAR + 5-second hold-E on {@link PropType#CASH_DRAWER_PROP} → 30–50 COIN.
 *       Achievement: {@link AchievementType#LOAN_RANGER}.</li>
 *   <li>LIGHTER on {@link PropType#FILING_CABINET_PROP} → loan wiped (Arson crime).
 *       Achievement: {@link AchievementType#BURNING_DEBT}.</li>
 *   <li>STOLEN_PHONE photo of loan terms + sell to journalist → headline + 15 COIN.
 *       Achievement: {@link AchievementType#LOAN_RANGER}.</li>
 * </ul>
 *
 * <h3>Mechanic 5 — FAKE_ID Loan</h3>
 * Use {@link Material#FAKE_ID} to bypass a ban. 15% chance Darren recognises the player
 * ({@link WantedSystem} +1, {@link CriminalRecord.CrimeType#IDENTITY_FRAUD}).
 * Achievement: {@link AchievementType#ANOTHER_IDENTITY}.
 */
public class PaydayLoanSystem {

    // ── Loan tier amounts ─────────────────────────────────────────────────────

    /** Small loan: 10 COIN principal. */
    public static final int LOAN_TIER_SMALL  = 10;

    /** Medium loan: 20 COIN principal. */
    public static final int LOAN_TIER_MEDIUM = 20;

    /** Large loan: 40 COIN principal. */
    public static final int LOAN_TIER_LARGE  = 40;

    /** Maximum loan tier when Marchetti respect ≥ FRIENDLY_THRESHOLD. */
    public static final int LOAN_TIER_MARCHETTI_MAX = 80;

    // ── Interest rates ────────────────────────────────────────────────────────

    /** Base interest rate (50%). */
    public static final float BASE_INTEREST_RATE = 0.50f;

    /** Discount applied when the player is employed (−40%). */
    public static final float EMPLOYMENT_DISCOUNT = 0.40f;

    /**
     * Discount applied when the player has a {@link Material#DEBT_ADVICE_LETTER} (−20%).
     * Mirrors {@link CitizensAdviceSystem#INTEREST_REDUCTION}.
     */
    public static final float ADVICE_LETTER_DISCOUNT = CitizensAdviceSystem.INTEREST_REDUCTION;

    // ── Timing ───────────────────────────────────────────────────────────────

    /** Repayment deadline in in-game days (3 days). */
    public static final int REPAYMENT_DEADLINE_DAYS = 3;

    /** Early-repayment window in in-game days (within 1 day). */
    public static final int EARLY_REPAYMENT_DAYS = 1;

    /** Day overdue at which Darren turns HOSTILE. */
    public static final int OVERDUE_HOSTILE_DAY = 1;

    /** Day overdue at which the bailiff spawns. */
    public static final int OVERDUE_BAILIFF_DAY = 2;

    /** Day overdue at which the loan is defaulted. */
    public static final int OVERDUE_DEFAULT_DAY = 3;

    /** Hour at which Darren's lunch break starts (12:30). */
    public static final float LUNCH_START_HOUR = 12.5f;

    /** Hour at which Darren's lunch break ends (13:00). */
    public static final float LUNCH_END_HOUR = 13.0f;

    /** Number of loans required before the barman seeds the back-office rumour. */
    public static final int LOANS_BEFORE_BARMAN_RUMOUR = 3;

    // ── Bribe & heist amounts ─────────────────────────────────────────────────

    /** Cost to bribe bailiff Terry. */
    public static final int BAILIFF_BRIBE_COST = 5;

    /** Minimum COIN looted from CASH_DRAWER_PROP. */
    public static final int CASH_DRAWER_LOOT_MIN = 30;

    /** Maximum COIN looted from CASH_DRAWER_PROP. */
    public static final int CASH_DRAWER_LOOT_MAX = 50;

    /** COIN received from selling loan-terms photograph to journalist. */
    public static final int JOURNALIST_TIP_REWARD = 15;

    /** Early repayment cashback amount. */
    public static final int EARLY_REPAYMENT_CASHBACK = 1;

    /** Notoriety added when loan becomes overdue (Day 1). */
    public static final int OVERDUE_NOTORIETY = 2;

    /** Probability (0–1) Darren recognises player when FAKE_ID is used. */
    public static final float FAKE_ID_RECOGNITION_CHANCE = 0.15f;

    // ── FactionSystem FRIENDLY threshold ─────────────────────────────────────

    /** Marchetti Crew respect required for double loan cap. */
    public static final int MARCHETTI_FRIENDLY_THRESHOLD = FactionSystem.FRIENDLY_THRESHOLD;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Whether the player currently has an active loan. */
    private boolean hasActiveLoan = false;

    /** Principal amount of the current loan in COIN. */
    private int loanPrincipal = 0;

    /** Interest amount in COIN (calculated at loan time). */
    private int loanInterest = 0;

    /** Total amount owed (principal + interest). */
    private int loanTotal = 0;

    /** The in-game day on which the loan was taken out. */
    private int loanIssuedDay = 0;

    /** Number of in-game days the loan is currently overdue (0 = not overdue). */
    private int daysOverdue = 0;

    /** Total number of loans taken out (ever). */
    private int totalLoansEver = 0;

    /** Whether the player is permanently banned from QuickFix Loans. */
    private boolean banned = false;

    /** Whether the BAILIFF NPC has been spawned for the current debt. */
    private boolean bailiffSpawned = false;

    /** Whether the barman back-office rumour has been seeded. */
    private boolean barmanRumourSeeded = false;

    /** Whether the FIRST_LOAN achievement has been awarded. */
    private boolean firstLoanAchievementAwarded = false;

    /** Whether the DEBT_SPIRAL achievement has been awarded for the current loan. */
    private boolean debtSpiralAwarded = false;

    /** Whether the BURNING_DEBT achievement has been awarded. */
    private boolean burningDebtAwarded = false;

    /** Whether the LOAN_RANGER achievement has been awarded. */
    private boolean loanRangerAwarded = false;

    /** Whether the ANOTHER_IDENTITY achievement has been awarded. */
    private boolean anotherIdentityAwarded = false;

    /** Whether the BOTTLED_THE_BAILIFF achievement has been awarded. */
    private boolean bottledTheBailiffAwarded = false;

    private final Random random;

    // ── Callback interface ────────────────────────────────────────────────────

    /** Callback for awarding achievements. */
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    public PaydayLoanSystem() {
        this(new Random());
    }

    public PaydayLoanSystem(Random random) {
        this.random = random;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Call once per in-game day tick to progress the debt spiral.
     *
     * @param currentDay        current in-game day index
     * @param darrenNpc         LOAN_SHARK_CLERK NPC (Darren), may be null
     * @param npcs              all living NPCs (for bailiff spawn check)
     * @param rumourNetwork     rumour network (may be null)
     * @param notorietySystem   notoriety system (may be null)
     * @param wantedSystem      wanted system (may be null)
     * @param criminalRecord    criminal record (may be null)
     * @param achievementCallback callback for achievements (may be null)
     */
    public void updateDailyTick(int currentDay,
                                NPC darrenNpc,
                                List<NPC> npcs,
                                RumourNetwork rumourNetwork,
                                NotorietySystem notorietySystem,
                                NotorietySystem.AchievementCallback notorietyCallback,
                                WantedSystem wantedSystem,
                                WantedSystem.AchievementCallback wantedCallback,
                                CriminalRecord criminalRecord,
                                AchievementCallback achievementCallback) {
        if (!hasActiveLoan) return;

        int daysSinceIssue = currentDay - loanIssuedDay;
        if (daysSinceIssue <= REPAYMENT_DEADLINE_DAYS) return; // still within deadline

        daysOverdue = daysSinceIssue - REPAYMENT_DEADLINE_DAYS;

        if (daysOverdue == OVERDUE_HOSTILE_DAY) {
            // Darren turns hostile
            if (darrenNpc != null) {
                darrenNpc.setState(NPCState.AGGRESSIVE);
                darrenNpc.setSpeechText("You owe me money, mate. Don't think I've forgotten.", 5f);
            }
            // Notoriety gain
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(OVERDUE_NOTORIETY, notorietyCallback);
            }
            // Seed overdue rumour
            if (rumourNetwork != null && darrenNpc != null) {
                rumourNetwork.addRumour(darrenNpc, new Rumour(RumourType.LOAN_OVERDUE_RUMOUR,
                        "Darren from QuickFix is fuming — someone's missed a payment. He's not happy."));
            }
        }

        if (daysOverdue == OVERDUE_BAILIFF_DAY && !bailiffSpawned) {
            bailiffSpawned = true;
            // Bailiff spawn is handled externally via isBailiffSpawnDue(); seed debt trouble rumour
            if (rumourNetwork != null && darrenNpc != null) {
                rumourNetwork.addRumour(darrenNpc, new Rumour(RumourType.DEBT_TROUBLE,
                        "Bailiff's been spotted on the estate. Someone owes serious money."));
            }
        }

        if (daysOverdue >= OVERDUE_DEFAULT_DAY && !debtSpiralAwarded) {
            debtSpiralAwarded = true;
            // Record crime
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.LOAN_DEFAULTED);
            }
            // Wanted +1
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, 0f, 0f, 0f, wantedCallback);
            }
            // Achievement
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.DEBT_SPIRAL);
            }
            // Permanent ban
            banned = true;
            // Clear the loan (debt considered written off as a crime)
            clearLoan();
        }
    }

    // ── Mechanic 1: Taking out a loan ─────────────────────────────────────────

    /**
     * Result of a loan application.
     */
    public enum LoanResult {
        /** Loan successfully issued; LOAN_AGREEMENT added to inventory. */
        SUCCESS,
        /** Player already has an active loan. */
        ALREADY_HAS_LOAN,
        /** Player is permanently banned. */
        BANNED,
        /** Loan shop is closed (outside Mon–Sat 09:00–17:00). */
        CLOSED,
        /** Invalid tier supplied. */
        INVALID_TIER
    }

    /**
     * Attempt to take out a loan from Darren at QuickFix Loans.
     *
     * @param tier             loan amount (10, 20, or 40 COIN; up to 80 if Marchetti FRIENDLY)
     * @param inventory        player inventory (LOAN_AGREEMENT added on success)
     * @param currentDay       current in-game day index
     * @param isShopOpen       whether the shop is open (Mon–Sat 09:00–17:00)
     * @param isEmployed       whether the player is currently employed
     * @param marchettiRespect Marchetti Crew respect score (0–100)
     * @param achievementCallback callback for achievements (may be null)
     * @return loan result
     */
    public LoanResult applyForLoan(int tier, Inventory inventory, int currentDay,
                                   boolean isShopOpen,
                                   boolean isEmployed,
                                   int marchettiRespect,
                                   AchievementCallback achievementCallback) {
        if (banned) return LoanResult.BANNED;
        if (hasActiveLoan) return LoanResult.ALREADY_HAS_LOAN;
        if (!isShopOpen) return LoanResult.CLOSED;

        // Determine max allowed tier
        int maxTier = (marchettiRespect >= MARCHETTI_FRIENDLY_THRESHOLD)
                ? LOAN_TIER_MARCHETTI_MAX : LOAN_TIER_LARGE;

        if (tier != LOAN_TIER_SMALL && tier != LOAN_TIER_MEDIUM
                && tier != LOAN_TIER_LARGE
                && !(marchettiRespect >= MARCHETTI_FRIENDLY_THRESHOLD && tier == LOAN_TIER_MARCHETTI_MAX)) {
            return LoanResult.INVALID_TIER;
        }
        if (tier > maxTier) return LoanResult.INVALID_TIER;

        // Calculate interest
        float interestRate = BASE_INTEREST_RATE;
        if (isEmployed) {
            interestRate -= EMPLOYMENT_DISCOUNT;
        }
        boolean usedAdviceLetter = inventory.getItemCount(Material.DEBT_ADVICE_LETTER) > 0;
        if (usedAdviceLetter) {
            interestRate -= ADVICE_LETTER_DISCOUNT;
            inventory.removeItem(Material.DEBT_ADVICE_LETTER, 1);
        }
        interestRate = Math.max(0f, interestRate);

        int interest = (int) Math.ceil(tier * interestRate);

        // Issue loan
        loanPrincipal  = tier;
        loanInterest   = interest;
        loanTotal      = tier + interest;
        loanIssuedDay  = currentDay;
        hasActiveLoan  = true;
        daysOverdue    = 0;
        bailiffSpawned = false;
        debtSpiralAwarded = false;
        totalLoansEver++;

        // Add LOAN_AGREEMENT to inventory
        inventory.addItem(Material.LOAN_AGREEMENT, 1);

        // Add the loan amount to the player's wallet
        inventory.addItem(Material.COIN, tier);

        // First loan achievement
        if (!firstLoanAchievementAwarded) {
            firstLoanAchievementAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.FIRST_LOAN);
            }
        }

        return LoanResult.SUCCESS;
    }

    // ── Mechanic 2: Repayment ─────────────────────────────────────────────────

    /**
     * Result of a repayment attempt.
     */
    public enum RepayResult {
        /** Repayment successful. */
        SUCCESS,
        /** Early repayment (within 1 in-game day): SUCCESS + 1 COIN cashback. */
        SUCCESS_EARLY,
        /** No active loan to repay. */
        NO_LOAN,
        /** Insufficient COIN in inventory. */
        INSUFFICIENT_FUNDS
    }

    /**
     * Attempt to repay the active loan. Called when player presses E on Darren with COIN.
     *
     * @param inventory           player inventory
     * @param currentDay          current in-game day index
     * @param rumourNetwork       rumour network (may be null)
     * @param darrenNpc           LOAN_SHARK_CLERK NPC (may be null)
     * @param achievementCallback callback for achievements (may be null)
     * @return repayment result
     */
    public RepayResult repayLoan(Inventory inventory, int currentDay,
                                 RumourNetwork rumourNetwork, NPC darrenNpc,
                                 AchievementCallback achievementCallback) {
        if (!hasActiveLoan) return RepayResult.NO_LOAN;
        if (inventory.getItemCount(Material.COIN) < loanTotal) {
            return RepayResult.INSUFFICIENT_FUNDS;
        }

        boolean isEarly = (currentDay - loanIssuedDay) <= EARLY_REPAYMENT_DAYS;

        inventory.removeItem(Material.COIN, loanTotal);
        inventory.removeItem(Material.LOAN_AGREEMENT, 1);

        // Restore Darren to friendly state if he was hostile
        if (darrenNpc != null && darrenNpc.getState() == NPCState.AGGRESSIVE) {
            darrenNpc.setState(NPCState.IDLE);
            darrenNpc.setSpeechText("Cheers. Always knew you were good for it.", 4f);
        }

        clearLoan();

        // Achievement
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.DEBT_FREE);
        }

        if (isEarly) {
            // Cashback
            inventory.addItem(Material.COIN, EARLY_REPAYMENT_CASHBACK);
            // Seed repaid rumour
            if (rumourNetwork != null && darrenNpc != null) {
                rumourNetwork.addRumour(darrenNpc, new Rumour(RumourType.LOAN_REPAID_RUMOUR,
                        "Someone at QuickFix paid back early. Got a coin back an' all."));
            }
            return RepayResult.SUCCESS_EARLY;
        }

        return RepayResult.SUCCESS;
    }

    // ── Mechanic 3: Bailiff interaction ───────────────────────────────────────

    /**
     * Returns true if the bailiff should be spawned (day 2+ overdue, not yet spawned).
     */
    public boolean isBailiffSpawnDue() {
        return hasActiveLoan && daysOverdue >= OVERDUE_BAILIFF_DAY && bailiffSpawned;
    }

    /**
     * Result of a bailiff interaction.
     */
    public enum BailiffInteractionResult {
        /** Player paid the debt — Terry leaves. */
        PAID,
        /** Player bribed Terry (5 COIN) — {@link RumourType#BRIBED_BAILIFF_RUMOUR} seeded. */
        BRIBED,
        /** Player attacked Terry — WantedSystem +2, achievement awarded. */
        ATTACKED,
        /** Player scarpered and barricaded — loan doubled, rumour seeded. */
        SCARPERED,
        /** Not enough COIN to pay or bribe. */
        INSUFFICIENT_FUNDS,
        /** No bailiff currently active. */
        NO_BAILIFF
    }

    /**
     * Handle player interaction with the BAILIFF NPC (Terry).
     *
     * @param action              one of: PAY, BRIBE, ATTACK, SCARPER
     * @param inventory           player inventory
     * @param terryNpc            the BAILIFF NPC
     * @param rumourNetwork       rumour network (may be null)
     * @param wantedSystem        wanted system (may be null)
     * @param wantedCallback      wanted achievement callback (may be null)
     * @param criminalRecord      criminal record (may be null)
     * @param achievementCallback callback for achievements (may be null)
     * @return result of the interaction
     */
    public BailiffInteractionResult interactWithBailiff(
            BailiffAction action,
            Inventory inventory,
            NPC terryNpc,
            RumourNetwork rumourNetwork,
            WantedSystem wantedSystem,
            WantedSystem.AchievementCallback wantedCallback,
            CriminalRecord criminalRecord,
            AchievementCallback achievementCallback) {

        if (!hasActiveLoan || !bailiffSpawned) return BailiffInteractionResult.NO_BAILIFF;

        switch (action) {
            case PAY:
                if (inventory.getItemCount(Material.COIN) < loanTotal) {
                    return BailiffInteractionResult.INSUFFICIENT_FUNDS;
                }
                inventory.removeItem(Material.COIN, loanTotal);
                inventory.removeItem(Material.LOAN_AGREEMENT, 1);
                if (terryNpc != null) {
                    terryNpc.setState(NPCState.IDLE);
                    terryNpc.setSpeechText("Cheers mate. I'll tell Darren.", 3f);
                }
                if (achievementCallback != null) {
                    achievementCallback.award(AchievementType.DEBT_FREE);
                }
                clearLoan();
                return BailiffInteractionResult.PAID;

            case BRIBE:
                if (inventory.getItemCount(Material.COIN) < BAILIFF_BRIBE_COST) {
                    return BailiffInteractionResult.INSUFFICIENT_FUNDS;
                }
                inventory.removeItem(Material.COIN, BAILIFF_BRIBE_COST);
                if (terryNpc != null) {
                    terryNpc.setState(NPCState.LEAVING);
                    terryNpc.setSpeechText("Go on then. I didn't see nothing.", 3f);
                }
                if (rumourNetwork != null && terryNpc != null) {
                    rumourNetwork.addRumour(terryNpc, new Rumour(RumourType.BRIBED_BAILIFF_RUMOUR,
                            "Someone slipped the bailiff a fiver. Bold as brass."));
                }
                // Note: loan remains active — just bought more time
                return BailiffInteractionResult.BRIBED;

            case ATTACK:
                if (terryNpc != null) {
                    terryNpc.setState(NPCState.FLEEING);
                    terryNpc.setSpeechText("Oi! You can't do that!", 3f);
                }
                // WantedSystem escalation
                if (wantedSystem != null) {
                    wantedSystem.addWantedStars(2, 0f, 0f, 0f, wantedCallback);
                }
                // Criminal record
                if (criminalRecord != null) {
                    criminalRecord.record(CriminalRecord.CrimeType.BAILIFF_ASSAULT);
                }
                // Achievement
                if (!bottledTheBailiffAwarded) {
                    bottledTheBailiffAwarded = true;
                    if (achievementCallback != null) {
                        achievementCallback.award(AchievementType.BOTTLED_THE_BAILIFF);
                    }
                }
                return BailiffInteractionResult.ATTACKED;

            case SCARPER:
                // Loan doubles
                loanTotal     = loanTotal * 2;
                loanInterest  = loanTotal - loanPrincipal;
                if (terryNpc != null) {
                    terryNpc.setState(NPCState.LEAVING);
                    terryNpc.setSpeechText("You can't hide forever, mate.", 3f);
                }
                if (rumourNetwork != null && terryNpc != null) {
                    rumourNetwork.addRumour(terryNpc, new Rumour(RumourType.SCARPERED_BAILIFF_RUMOUR,
                            "Someone barricaded their door against the bailiff. Loan's doubled now apparently."));
                }
                bailiffSpawned = false; // reset so he can come again
                return BailiffInteractionResult.SCARPERED;

            default:
                return BailiffInteractionResult.NO_BAILIFF;
        }
    }

    /** Enum of possible player responses to the bailiff. */
    public enum BailiffAction {
        PAY, BRIBE, ATTACK, SCARPER
    }

    // ── Mechanic 4: Back-room heist ───────────────────────────────────────────

    /**
     * Returns true if it is currently Darren's lunch break (12:30–13:00).
     *
     * @param currentHour the current fractional in-game hour (e.g. 12.5 = 12:30)
     */
    public boolean isDarrenOnLunch(float currentHour) {
        return currentHour >= LUNCH_START_HOUR && currentHour < LUNCH_END_HOUR;
    }

    /**
     * Attempt to loot the CASH_DRAWER_PROP with a CROWBAR during Darren's lunch.
     *
     * @param inventory           player inventory
     * @param currentHour         current in-game fractional hour
     * @param achievementCallback callback for achievements (may be null)
     * @return LootResult describing the outcome
     */
    public LootResult lootCashDrawer(Inventory inventory, float currentHour,
                                     AchievementCallback achievementCallback) {
        if (!inventory.getItemCount(Material.CROWBAR) > 0) {
            return LootResult.NO_TOOL;
        }
        if (!isDarrenOnLunch(currentHour)) {
            return LootResult.DARREN_PRESENT;
        }
        int loot = CASH_DRAWER_LOOT_MIN
                + random.nextInt(CASH_DRAWER_LOOT_MAX - CASH_DRAWER_LOOT_MIN + 1);
        inventory.addItem(Material.COIN, loot);

        if (!loanRangerAwarded) {
            loanRangerAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.LOAN_RANGER);
            }
        }
        return new LootResult(true, loot, null);
    }

    /**
     * Use a LIGHTER on the FILING_CABINET_PROP to destroy loan records (wipes active loan).
     *
     * @param inventory           player inventory
     * @param criminalRecord      criminal record (ARSON logged)
     * @param achievementCallback callback for achievements (may be null)
     * @return true if arson succeeded and loan was wiped
     */
    public boolean burnFilingCabinet(Inventory inventory, CriminalRecord criminalRecord,
                                     AchievementCallback achievementCallback) {
        if (inventory.getItemCount(Material.LIGHTER) <= 0
                && inventory.getItemCount(Material.DISPOSABLE_LIGHTER) <= 0) {
            return false;
        }
        // Record arson
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.ARSON);
        }
        // Wipe loan
        if (hasActiveLoan) {
            inventory.removeItem(Material.LOAN_AGREEMENT, 1);
            clearLoan();
        }
        if (!burningDebtAwarded) {
            burningDebtAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.BURNING_DEBT);
            }
        }
        return true;
    }

    /**
     * Photograph loan terms and sell to journalist via phone box.
     *
     * @param inventory           player inventory (STOLEN_PHONE required; 15 COIN added)
     * @param achievementCallback callback for achievements (may be null)
     * @return true if photo sell succeeded
     */
    public boolean photographAndSellLoanTerms(Inventory inventory,
                                               AchievementCallback achievementCallback) {
        if (inventory.getItemCount(Material.STOLEN_PHONE) <= 0) {
            return false;
        }
        inventory.addItem(Material.COIN, JOURNALIST_TIP_REWARD);
        if (!loanRangerAwarded) {
            loanRangerAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.LOAN_RANGER);
            }
        }
        return true;
    }

    /**
     * Check whether the barman should seed the back-office rumour (after 3 loans).
     * Call once after each successful loan issuance. Seeds the rumour and marks it done.
     *
     * @param barmanNpc     BARMAN NPC to seed the rumour from (may be null)
     * @param rumourNetwork rumour network (may be null)
     */
    public void checkBarmanRumour(NPC barmanNpc, RumourNetwork rumourNetwork) {
        if (barmanRumourSeeded) return;
        if (totalLoansEver < LOANS_BEFORE_BARMAN_RUMOUR) return;
        barmanRumourSeeded = true;
        if (rumourNetwork != null && barmanNpc != null) {
            rumourNetwork.addRumour(barmanNpc, new Rumour(RumourType.BARMAN_RUMOUR_LOAN_OFFICE,
                    "Darren nips off for lunch at half twelve — back office at QuickFix is unattended."));
        }
    }

    // ── Mechanic 5: FAKE_ID loan ──────────────────────────────────────────────

    /**
     * Attempt a loan using a FAKE_ID while banned.
     *
     * @param tier                loan amount
     * @param inventory           player inventory
     * @param currentDay          current in-game day
     * @param isShopOpen          whether the shop is open
     * @param isEmployed          whether the player is employed
     * @param marchettiRespect    Marchetti Crew respect (0–100)
     * @param wantedSystem        wanted system (may be null)
     * @param wantedCallback      wanted achievement callback (may be null)
     * @param criminalRecord      criminal record (may be null)
     * @param achievementCallback callback for achievements (may be null)
     * @return LoanResult (SUCCESS if recognised = false; or BANNED if Darren recognises)
     */
    public LoanResult applyForLoanWithFakeId(int tier, Inventory inventory, int currentDay,
                                              boolean isShopOpen, boolean isEmployed,
                                              int marchettiRespect,
                                              WantedSystem wantedSystem,
                                              WantedSystem.AchievementCallback wantedCallback,
                                              CriminalRecord criminalRecord,
                                              AchievementCallback achievementCallback) {
        if (!banned) {
            // Not banned — can use normal route
            return applyForLoan(tier, inventory, currentDay, isShopOpen, isEmployed,
                    marchettiRespect, achievementCallback);
        }
        if (inventory.getItemCount(Material.FAKE_ID) <= 0) {
            return LoanResult.BANNED;
        }
        if (!isShopOpen) return LoanResult.CLOSED;

        // Award ANOTHER_IDENTITY on first use
        if (!anotherIdentityAwarded) {
            anotherIdentityAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.ANOTHER_IDENTITY);
            }
        }

        // 15% chance Darren recognises player
        boolean recognised = random.nextFloat() < FAKE_ID_RECOGNITION_CHANCE;
        if (recognised) {
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, 0f, 0f, 0f, wantedCallback);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.IDENTITY_FRAUD);
            }
            return LoanResult.BANNED;
        }

        // Temporarily lift ban for this transaction
        banned = false;
        LoanResult result = applyForLoan(tier, inventory, currentDay, isShopOpen,
                isEmployed, marchettiRespect, achievementCallback);
        // Do NOT restore banned — FAKE_ID lifts the ban successfully
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void clearLoan() {
        hasActiveLoan  = false;
        loanPrincipal  = 0;
        loanInterest   = 0;
        loanTotal      = 0;
        loanIssuedDay  = 0;
        daysOverdue    = 0;
        bailiffSpawned = false;
        debtSpiralAwarded = false;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Whether the player currently has an active loan. */
    public boolean hasActiveLoan() { return hasActiveLoan; }

    /** Principal amount of the current loan in COIN (0 if no loan). */
    public int getLoanPrincipal() { return loanPrincipal; }

    /** Interest amount of the current loan in COIN (0 if no loan). */
    public int getLoanInterest() { return loanInterest; }

    /** Total amount owed (principal + interest). 0 if no loan. */
    public int getLoanTotal() { return loanTotal; }

    /** In-game day on which the current loan was issued (0 if no loan). */
    public int getLoanIssuedDay() { return loanIssuedDay; }

    /** Number of in-game days overdue (0 = not overdue). */
    public int getDaysOverdue() { return daysOverdue; }

    /** Whether the player is permanently banned from QuickFix Loans. */
    public boolean isBanned() { return banned; }

    /** Total loans ever taken out. */
    public int getTotalLoansEver() { return totalLoansEver; }

    /** Whether the back-room barman rumour has been seeded. */
    public boolean isBarmanRumourSeeded() { return barmanRumourSeeded; }

    // ── Force-set for testing ─────────────────────────────────────────────────

    /** Force-set days overdue (for testing). */
    public void setDaysOverdueForTesting(int days) { this.daysOverdue = days; }

    /** Force-set loan issued day (for testing). */
    public void setLoanIssuedDayForTesting(int day) { this.loanIssuedDay = day; }

    /** Force-set banned state (for testing). */
    public void setBannedForTesting(boolean banned) { this.banned = banned; }

    /** Force-set total loans ever (for testing). */
    public void setTotalLoansEverForTesting(int count) { this.totalLoansEver = count; }

    /** Force-set bailiff spawned (for testing). */
    public void setBailiffSpawnedForTesting(boolean spawned) { this.bailiffSpawned = spawned; }

    // ── Nested result type ────────────────────────────────────────────────────

    /**
     * Result of looting the cash drawer.
     */
    public static class LootResult {
        /** Whether the loot succeeded. */
        public final boolean success;
        /** Amount of COIN looted (0 if not successful). */
        public final int coinLooted;
        /** Optional failure reason string (null on success). */
        public final String failReason;

        /** Sentinel: no CROWBAR in inventory. */
        public static final LootResult NO_TOOL    = new LootResult(false, 0, "Need a crowbar.");
        /** Sentinel: Darren is present (not on lunch break). */
        public static final LootResult DARREN_PRESENT = new LootResult(false, 0, "Darren's watching.");

        public LootResult(boolean success, int coinLooted, String failReason) {
            this.success    = success;
            this.coinLooted = coinLooted;
            this.failReason = failReason;
        }
    }
}

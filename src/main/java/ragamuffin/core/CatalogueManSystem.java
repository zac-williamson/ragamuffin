package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.core.NotorietySystem.AchievementCallback;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1408: Northfield Catalogue Man — Barry's Round, the Owed Debt
 * &amp; the Trading Standards Trap.
 *
 * <h3>Mechanic 1 — Barry's Round (Mon/Wed/Fri 10:00–16:00)</h3>
 * <ul>
 *   <li>Barry ({@link NPCType#CATALOGUE_MAN}) visits 10 residential properties/day.</li>
 *   <li>Spends {@link #SECONDS_PER_PROPERTY} (90s) at each doorstep in COLLECTING state.</li>
 *   <li>Back door is briefly unlocked during COLLECTING — a burglary window for the player.</li>
 *   <li>His {@link PropType#CATALOGUE_BAG_PROP} can be stolen with a 2-second hold-E for
 *       1–3 catalogue items (CATALOGUE_TRINKET, CATALOGUE_TOOL, CATALOGUE_TEXTILE).
 *       Fence value 2–6 COIN.</li>
 *   <li>Barry turns HOSTILE if he spots the thief within {@link #BARRY_HOSTILE_SPOT_RADIUS} blocks.</li>
 *   <li>Achievement: {@link AchievementType#BARRY_BANDIT} (steal bag 3 separate days).</li>
 * </ul>
 *
 * <h3>Mechanic 2 — The Owed Debt</h3>
 * <ul>
 *   <li>3 of 10 stops are defaulting households — Barry argues and leaves DEJECTED.</li>
 *   <li>Player can impersonate a debt collector (E on door after Barry leaves):</li>
 *   <li>70% success at low notoriety (≤ {@link #DEBT_LOW_NOTORIETY_THRESHOLD}),
 *       10% at high (gt; {@link #DEBT_HIGH_NOTORIETY_THRESHOLD}).</li>
 *   <li>Success yields {@link #DEBT_MIN_REWARD}–{@link #DEBT_MAX_REWARD} COIN (HMRC-logged).</li>
 *   <li>Failure adds Notoriety +{@link #DEBT_FAIL_NOTORIETY}.</li>
 *   <li>Tip off Loan Shark NPC with {@link Material#CATALOGUE_RECEIPT} for
 *       {@link #LOAN_SHARK_FINDER_FEE} COIN finder's fee.</li>
 *   <li>Achievements: {@link AchievementType#DEBT_DODGER},
 *       {@link AchievementType#LOAN_SHARK_INFORMANT}.</li>
 * </ul>
 *
 * <h3>Mechanic 3 — The Trading Standards Trap</h3>
 * <ul>
 *   <li>After witnessing {@link #WITNESS_THRESHOLD_FOR_SAMPLE} undetected deliveries,
 *       player receives {@link Material#CATALOGUE_SAMPLE}.</li>
 *   <li>(a) Report to Trading Standards — Notoriety −{@link #TRADING_STANDARDS_NOTORIETY_REDUCTION},
 *       {@link #TRADING_STANDARDS_REWARD} COIN, Barry suspended {@link #TRADING_STANDARDS_SUSPENSION_DAYS}
 *       days. Achievement: {@link AchievementType#CIVIC_CRUSADER}.</li>
 *   <li>(b) Blackmail Barry — {@link #BLACKMAIL_PAYMENT} COIN, Barry hostile if approached within 1 day.
 *       Blackmail twice → {@link CrimeType#EXTORTION} crime + Wanted +1.
 *       Achievement: {@link AchievementType#SILENT_PARTNER}.</li>
 *   <li>(c) Craft {@link Material#KNOCKOFF_CATALOGUE} at InternetCafe and run rival round —
 *       {@link #RIVAL_CATALOGUE_ACCEPT_RATE} accept rate, {@link #RIVAL_CATALOGUE_COIN_PER_SALE} COIN/sale.
 *       Monthly Trading Standards check (last Friday 14:00) has {@link #TRADING_STANDARDS_CATCH_CHANCE}
 *       catch chance for {@link CrimeType#COUNTERFEIT_GOODS_SELLING} + {@link #COUNTERFEIT_FINE} COIN fine.
 *       Achievement: {@link AchievementType#CATALOGUE_KING} (sell 5 separate days).</li>
 * </ul>
 */
public class CatalogueManSystem {

    // ── Round schedule ────────────────────────────────────────────────────────

    /** Hour Barry starts his round (10:00). */
    public static final float ROUND_START_HOUR = 10.0f;

    /** Hour Barry ends his round (16:00). */
    public static final float ROUND_END_HOUR = 16.0f;

    /** Number of properties Barry visits per day. */
    public static final int PROPERTIES_PER_DAY = 10;

    /** In-game seconds Barry spends at each property in COLLECTING state. */
    public static final float SECONDS_PER_PROPERTY = 90.0f;

    /** Number of properties per day that are defaulting households. */
    public static final int DEFAULTERS_PER_DAY = 3;

    // ── Mechanic 1 — Bag theft ───────────────────────────────────────────────

    /** Hold-E duration (seconds) required to steal the CATALOGUE_BAG_PROP. */
    public static final float STEAL_HOLD_DURATION = 2.0f;

    /** Notoriety added when the player steals the catalogue bag. */
    public static final int CATALOGUE_THEFT_NOTORIETY = 4;

    /** WantedSystem stars added on catalogue bag theft. */
    public static final int CATALOGUE_THEFT_WANTED_STARS = 1;

    /** Radius (blocks) within which Barry spots the thief and turns HOSTILE. */
    public static final float BARRY_HOSTILE_SPOT_RADIUS = 5.0f;

    /** Steal bag this many separate days for BARRY_BANDIT achievement. */
    public static final int BARRY_BANDIT_THRESHOLD = 3;

    // ── Mechanic 2 — Debt collection ─────────────────────────────────────────

    /** Notoriety at or below which the 70% success chance applies. */
    public static final int DEBT_LOW_NOTORIETY_THRESHOLD = 20;

    /** Notoriety above which the 10% success chance applies. */
    public static final int DEBT_HIGH_NOTORIETY_THRESHOLD = 40;

    /** Success chance (0–1) at low notoriety. */
    public static final float DEBT_SUCCESS_CHANCE_LOW = 0.70f;

    /** Success chance (0–1) at mid notoriety. */
    public static final float DEBT_SUCCESS_CHANCE_MID = 0.40f;

    /** Success chance (0–1) at high notoriety. */
    public static final float DEBT_SUCCESS_CHANCE_HIGH = 0.10f;

    /** Minimum COIN reward from a successful debt collection. */
    public static final int DEBT_MIN_REWARD = 4;

    /** Maximum COIN reward from a successful debt collection (inclusive). */
    public static final int DEBT_MAX_REWARD = 8;

    /** Notoriety penalty on failed debt collection attempt. */
    public static final int DEBT_FAIL_NOTORIETY = 1;

    /** COIN finder's fee paid by Loan Shark for a CATALOGUE_RECEIPT tip-off. */
    public static final int LOAN_SHARK_FINDER_FEE = 3;

    /** Debt collection successes needed for DEBT_DODGER achievement. */
    public static final int DEBT_DODGER_THRESHOLD = 5;

    /** Loan Shark tip-offs needed for LOAN_SHARK_INFORMANT achievement. */
    public static final int LOAN_SHARK_INFORMANT_THRESHOLD = 3;

    // ── Mechanic 3 — Trading Standards Trap ─────────────────────────────────

    /** Undetected deliveries witnessed before CATALOGUE_SAMPLE is received. */
    public static final int WITNESS_THRESHOLD_FOR_SAMPLE = 5;

    /** COIN reward for reporting Barry to Trading Standards. */
    public static final int TRADING_STANDARDS_REWARD = 10;

    /** Notoriety reduction for reporting Barry to Trading Standards. */
    public static final int TRADING_STANDARDS_NOTORIETY_REDUCTION = 3;

    /** In-game days Barry is suspended after being reported. */
    public static final int TRADING_STANDARDS_SUSPENSION_DAYS = 3;

    /** COIN paid by Barry when blackmailed. */
    public static final int BLACKMAIL_PAYMENT = 8;

    /** Rival catalogue accept rate (0–1). */
    public static final float RIVAL_CATALOGUE_ACCEPT_RATE = 0.35f;

    /** COIN per rival catalogue sale. */
    public static final int RIVAL_CATALOGUE_COIN_PER_SALE = 4;

    /** Chance (0–1) Trading Standards catches rival catalogue round on monthly check. */
    public static final float TRADING_STANDARDS_CATCH_CHANCE = 0.20f;

    /** COIN fine when caught by Trading Standards. */
    public static final int COUNTERFEIT_FINE = 20;

    /** Notoriety added when caught with counterfeit catalogue. */
    public static final int COUNTERFEIT_NOTORIETY = 5;

    /** Rival catalogue selling days needed for CATALOGUE_KING achievement. */
    public static final int CATALOGUE_KING_THRESHOLD = 5;

    // ── Speech lines ──────────────────────────────────────────────────────────

    public static final String BARRY_GREETING     = "All right, love. I'll be with you in a minute.";
    public static final String BARRY_HOSTILE      = "Oi! That's my stuff! Get away from it!";
    public static final String BARRY_DEJECTED     = "Waste of time. They never pay.";
    public static final String BARRY_BLACKMAIL    = "...Fine. Just keep your mouth shut.";
    public static final String BARRY_SUSPENDED    = "I've been suspended. Trading Standards.";

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Result of attempting to steal Barry's CATALOGUE_BAG_PROP. */
    public enum BagTheftResult {
        /** Bag successfully stolen; items added to inventory. */
        STOLEN,
        /** Barry spotted the player; bag not stolen; Barry HOSTILE. */
        WITNESSED_BY_BARRY,
        /** No bag is currently placed (Barry not in COLLECTING state). */
        NO_BAG
    }

    /** Result of attempting to impersonate a debt collector. */
    public enum DebtCollectionResult {
        /** Success — COIN awarded. */
        SUCCESS,
        /** Failure — Notoriety penalty applied. */
        FAILED,
        /** No defaulter at current position (property is not a defaulter). */
        NO_DEFAULTER,
        /** Barry has not visited this stop yet / stop not a defaulter. */
        NOT_AVAILABLE
    }

    /** Result of tipping off the Loan Shark. */
    public enum LoanSharkTipResult {
        /** Tip-off accepted; finder's fee paid; DEBT_COLLECTOR rumour seeded. */
        TIPPED,
        /** Player does not have a CATALOGUE_RECEIPT. */
        NO_RECEIPT,
        /** Loan Shark already tipped for this property. */
        ALREADY_TIPPED
    }

    /** Result of reporting Barry to Trading Standards. */
    public enum TradingStandardsReportResult {
        /** Reported; Barry suspended, reward paid, Notoriety reduced. */
        REPORTED,
        /** Player does not have a CATALOGUE_SAMPLE. */
        NO_SAMPLE,
        /** Barry is already suspended. */
        ALREADY_SUSPENDED
    }

    /** Result of blackmailing Barry. */
    public enum BlackmailResult {
        /** First blackmail — Barry pays; SILENT_PARTNER awarded on first. */
        ACCEPTED,
        /** Second blackmail — EXTORTION crime recorded + Wanted +1. */
        EXTORTION_TRIGGERED,
        /** Player does not have a CATALOGUE_SAMPLE. */
        NO_SAMPLE,
        /** Barry is hostile/suspended and refuses interaction. */
        BARRY_UNAVAILABLE
    }

    /** Result of a rival catalogue sale attempt. */
    public enum RivalCatalogueResult {
        /** Householder accepts — COIN awarded. */
        ACCEPTED,
        /** Householder refuses. */
        REFUSED,
        /** Trading Standards monthly check caught the player mid-sale. */
        CAUGHT_BY_TRADING_STANDARDS,
        /** Player does not have a KNOCKOFF_CATALOGUE. */
        NO_CATALOGUE
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Whether Barry's round is currently active (Mon/Wed/Fri, 10:00–16:00). */
    private boolean roundActive = false;

    /** Current property index in the route (0–9). */
    private int currentPropertyIndex = 0;

    /** Seconds Barry has spent at the current property. */
    private float propertyTimer = 0f;

    /** Whether the CATALOGUE_BAG_PROP is currently placed at the active property. */
    private boolean bagPlaced = false;

    /** Whether the current property is a defaulter (Barry will leave DEJECTED). */
    private boolean currentPropertyIsDefaulter = false;

    /** Whether Barry has left the current defaulter property (player can now impersonate). */
    private boolean defaulterWindowOpen = false;

    /** Set of property indices that are defaulters this round. */
    private final List<Integer> defaulterIndices = new ArrayList<>();

    /** Set of defaulter property indices already tipped to Loan Shark this round. */
    private final List<Integer> loanSharkTippedIndices = new ArrayList<>();

    /** Number of undetected deliveries witnessed by the player this session. */
    private int witnessedDeliveries = 0;

    /** Whether the player has received the CATALOGUE_SAMPLE this session. */
    private boolean catalogueSampleReceived = false;

    /** Whether Barry has been reported to Trading Standards. */
    private boolean barrySuspended = false;

    /** Days remaining on Barry's suspension. */
    private float suspensionDaysRemaining = 0f;

    /** Whether Barry is currently hostile. */
    private boolean barryHostile = false;

    /** Timer (in-game hours) remaining on Barry's hostile state. */
    private float barryHostileHoursRemaining = 0f;

    /** Number of days the player has stolen the bag (for BARRY_BANDIT). */
    private int bagStolenDays = 0;

    /** Whether the bag was already stolen today. */
    private boolean bagStolenToday = false;

    /** Number of successful debt collection impersonations. */
    private int debtCollectionSuccesses = 0;

    /** Number of Loan Shark tip-offs. */
    private int loanSharkTipOffs = 0;

    /** Number of blackmail attempts this session. */
    private int blackmailCount = 0;

    /** Number of days the player has sold from rival catalogue. */
    private int rivalCatalogueSellDays = 0;

    /** Whether the player has sold from rival catalogue today. */
    private boolean rivalCatalogueSoldToday = false;

    // ── Achievement flags ─────────────────────────────────────────────────────

    private boolean barryBanditAwarded = false;
    private boolean debtDodgerAwarded = false;
    private boolean loanSharkInformantAwarded = false;
    private boolean civicCrusaderAwarded = false;
    private boolean silentPartnerAwarded = false;
    private boolean catalogueKingAwarded = false;

    // ── Optional integrations ─────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private HMRCSystem hmrcSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public CatalogueManSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ──────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setHmrcSystem(HMRCSystem hmrcSystem) {
        this.hmrcSystem = hmrcSystem;
    }

    // ── Round management ──────────────────────────────────────────────────────

    /**
     * Update the catalogue man system each frame.
     *
     * @param delta          seconds since last frame
     * @param hour           current in-game hour
     * @param dayOfWeek      0=Sunday, 1=Monday … 6=Saturday
     * @param playerX        player X position
     * @param playerY        player Y position
     * @param playerZ        player Z position
     * @param playerNotoriety raw player notoriety score
     * @param nearbyNpc      nearest NPC witness (or null)
     * @param cb             achievement callback (may be null)
     */
    public void update(float delta, float hour, int dayOfWeek,
                       float playerX, float playerY, float playerZ,
                       int playerNotoriety,
                       NPC nearbyNpc, AchievementCallback cb) {

        // Manage hostile timer (in-game hours)
        if (barryHostile && barryHostileHoursRemaining > 0f) {
            // Convert delta (seconds) to hours: assume 1 real second = some game speed
            // Use a simple countdown; hostile timer counts in game-seconds (same unit as delta)
            barryHostileHoursRemaining -= delta;
            if (barryHostileHoursRemaining <= 0f) {
                barryHostileHoursRemaining = 0f;
                barryHostile = false;
            }
        }

        // Manage suspension countdown (in days; advance by delta seconds relative to round length)
        if (barrySuspended && suspensionDaysRemaining > 0f) {
            // Decrement suspension only when not in an active round period
            suspensionDaysRemaining -= delta / (6.0f * 3600f); // 6-hour round = 1 day (simplified)
            if (suspensionDaysRemaining <= 0f) {
                suspensionDaysRemaining = 0f;
                barrySuspended = false;
            }
        }

        // Check if round should be active (Mon=1, Wed=3, Fri=5)
        boolean shouldBeActive = isOperatingDay(dayOfWeek)
                && hour >= ROUND_START_HOUR
                && hour < ROUND_END_HOUR
                && !barrySuspended;

        if (!shouldBeActive) {
            if (roundActive) {
                endRound();
            }
            return;
        }

        if (!roundActive) {
            startRound();
        }

        // Advance property timer
        propertyTimer += delta;
        if (propertyTimer >= SECONDS_PER_PROPERTY) {
            propertyTimer = 0f;
            advanceToNextProperty(nearbyNpc, cb);
        }
    }

    private boolean isOperatingDay(int dayOfWeek) {
        return dayOfWeek == 1 || dayOfWeek == 3 || dayOfWeek == 5; // Mon, Wed, Fri
    }

    private void startRound() {
        roundActive = true;
        currentPropertyIndex = 0;
        propertyTimer = 0f;
        bagPlaced = true;
        bagStolenToday = false;
        rivalCatalogueSoldToday = false;
        defaulterWindowOpen = false;
        defaulterIndices.clear();
        loanSharkTippedIndices.clear();
        resolveDefaulters();
        currentPropertyIsDefaulter = defaulterIndices.contains(0);
    }

    private void endRound() {
        roundActive = false;
        bagPlaced = false;
        defaulterWindowOpen = false;
    }

    private void advanceToNextProperty(NPC nearbyNpc, AchievementCallback cb) {
        // Check if this was a non-defaulter delivery the player witnessed
        if (!currentPropertyIsDefaulter && nearbyNpc != null) {
            recordWitnessedDelivery(nearbyNpc, cb);
        }

        // Close defaulter window from previous property
        defaulterWindowOpen = false;

        // If current property was a defaulter, open the window for impersonation
        if (currentPropertyIsDefaulter) {
            defaulterWindowOpen = true;
        }

        currentPropertyIndex++;
        if (currentPropertyIndex >= PROPERTIES_PER_DAY) {
            endRound();
            return;
        }

        bagPlaced = !bagStolenToday; // Only place if not already stolen today
        currentPropertyIsDefaulter = defaulterIndices.contains(currentPropertyIndex);
    }

    private void resolveDefaulters() {
        // Deterministically pick DEFAULTERS_PER_DAY indices from the 10 properties
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < PROPERTIES_PER_DAY; i++) {
            indices.add(i);
        }
        java.util.Collections.shuffle(indices, random);
        for (int i = 0; i < DEFAULTERS_PER_DAY; i++) {
            defaulterIndices.add(indices.get(i));
        }
    }

    private void recordWitnessedDelivery(NPC nearbyNpc, AchievementCallback cb) {
        witnessedDeliveries++;
        if (!catalogueSampleReceived && witnessedDeliveries >= WITNESS_THRESHOLD_FOR_SAMPLE) {
            catalogueSampleReceived = true;
            // Sample flagged — player needs to pick it up in the UI
        }
    }

    // ── Mechanic 1 — Bag theft ────────────────────────────────────────────────

    /**
     * Player attempts to steal Barry's CATALOGUE_BAG_PROP.
     *
     * <p>Called after the player has held E for {@link #STEAL_HOLD_DURATION} seconds
     * next to the bag prop. Barry is considered within spotting range if
     * {@code barryDistToPlayer} &le; {@link #BARRY_HOSTILE_SPOT_RADIUS}.
     *
     * @param inventory          player's inventory (items added on success)
     * @param barryDistToPlayer  distance from Barry to the player (blocks)
     * @param witnessNpc         NPC witness (or null); used for rumour seeding
     * @param cb                 achievement callback
     * @return result of the theft attempt
     */
    public BagTheftResult stealBag(Inventory inventory, float barryDistToPlayer,
                                   NPC witnessNpc, AchievementCallback cb) {
        if (!bagPlaced) return BagTheftResult.NO_BAG;

        // Check if Barry spots the player
        if (barryDistToPlayer <= BARRY_HOSTILE_SPOT_RADIUS) {
            barryHostile = true;
            barryHostileHoursRemaining = 24 * 3600f; // Hostile for remainder of day (24h in seconds)
            if (rumourNetwork != null && witnessNpc != null) {
                rumourNetwork.addRumour(witnessNpc,
                        new Rumour(RumourType.BARRY_SUSPICIOUS,
                                "Someone's been nicking stuff out of the catalogue bloke's bag on his round."));
            }
            return BagTheftResult.WITNESSED_BY_BARRY;
        }

        // Successful theft
        bagPlaced = false;

        // Award 1–3 random catalogue items
        int itemCount = 1 + random.nextInt(3);
        Material[] catalogueItems = {
            Material.CATALOGUE_TRINKET,
            Material.CATALOGUE_TOOL,
            Material.CATALOGUE_TEXTILE
        };
        for (int i = 0; i < itemCount; i++) {
            inventory.addItem(catalogueItems[random.nextInt(catalogueItems.length)], 1);
        }

        // Crime recording
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.CATALOGUE_THEFT);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(CATALOGUE_THEFT_NOTORIETY, cb);
        }
        if (wantedSystem != null) {
            wantedSystem.increaseWantedStars(CATALOGUE_THEFT_WANTED_STARS);
        }

        // Seed rumour if witnessed by a bystander
        if (witnessNpc != null && rumourNetwork != null) {
            rumourNetwork.addRumour(witnessNpc,
                    new Rumour(RumourType.BARRY_SUSPICIOUS,
                            "Someone's been nicking stuff out of the catalogue bloke's bag on his round."));
        }

        // Track bag-steal days for BARRY_BANDIT (check BEFORE flagging today)
        if (!bagStolenToday) {
            bagStolenDays++;
        }
        bagStolenToday = true;

        checkBarryBandit(cb);
        return BagTheftResult.STOLEN;
    }

    private void checkBarryBandit(AchievementCallback cb) {
        if (!barryBanditAwarded && bagStolenDays >= BARRY_BANDIT_THRESHOLD && cb != null) {
            barryBanditAwarded = true;
            cb.award(AchievementType.BARRY_BANDIT);
        }
    }

    // ── Mechanic 2 — Debt collection ─────────────────────────────────────────

    /**
     * Player presses E on a defaulting household door to impersonate a debt collector.
     *
     * @param inventory       player's inventory (COIN added on success, CATALOGUE_RECEIPT given)
     * @param playerNotoriety raw player notoriety score
     * @param witnessNpc      NPC witness (or null); for rumour seeding on success
     * @param cb              achievement callback
     * @return result of the collection attempt
     */
    public DebtCollectionResult attemptDebtCollection(Inventory inventory,
                                                       int playerNotoriety,
                                                       NPC witnessNpc,
                                                       AchievementCallback cb) {
        if (!defaulterWindowOpen) return DebtCollectionResult.NOT_AVAILABLE;
        if (!defaulterIndices.contains(currentPropertyIndex > 0
                ? currentPropertyIndex - 1 : currentPropertyIndex)) {
            return DebtCollectionResult.NO_DEFAULTER;
        }

        float successChance = getDebtSuccessChance(playerNotoriety);
        if (random.nextFloat() < successChance) {
            int reward = DEBT_MIN_REWARD + random.nextInt(DEBT_MAX_REWARD - DEBT_MIN_REWARD + 1);
            inventory.addItem(Material.COIN, reward);
            inventory.addItem(Material.CATALOGUE_RECEIPT, 1);

            // HMRC logging
            if (hmrcSystem != null) {
                hmrcSystem.onUntaxedEarning(reward, 0);
            }

            debtCollectionSuccesses++;
            checkDebtDodger(cb);

            if (rumourNetwork != null && witnessNpc != null) {
                rumourNetwork.addRumour(witnessNpc,
                        new Rumour(RumourType.DEBT_COLLECTOR,
                                "Loan shark's been calling at doors. Someone grassed up people behind on their catalogue."));
            }

            return DebtCollectionResult.SUCCESS;
        } else {
            // Failed — notoriety penalty
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(DEBT_FAIL_NOTORIETY, cb);
            }
            return DebtCollectionResult.FAILED;
        }
    }

    private float getDebtSuccessChance(int notoriety) {
        if (notoriety <= DEBT_LOW_NOTORIETY_THRESHOLD) {
            return DEBT_SUCCESS_CHANCE_LOW;
        } else if (notoriety <= DEBT_HIGH_NOTORIETY_THRESHOLD) {
            return DEBT_SUCCESS_CHANCE_MID;
        } else {
            return DEBT_SUCCESS_CHANCE_HIGH;
        }
    }

    private void checkDebtDodger(AchievementCallback cb) {
        if (!debtDodgerAwarded && debtCollectionSuccesses >= DEBT_DODGER_THRESHOLD && cb != null) {
            debtDodgerAwarded = true;
            cb.award(AchievementType.DEBT_DODGER);
        }
    }

    /**
     * Player tips off the Loan Shark NPC using a CATALOGUE_RECEIPT.
     *
     * @param inventory  player's inventory (CATALOGUE_RECEIPT consumed; COIN added)
     * @param witnessNpc NPC witness (or null); for rumour seeding
     * @param cb         achievement callback
     * @return result of the tip-off
     */
    public LoanSharkTipResult tipOffLoanShark(Inventory inventory,
                                               NPC witnessNpc,
                                               AchievementCallback cb) {
        if (!inventory.hasItem(Material.CATALOGUE_RECEIPT)) {
            return LoanSharkTipResult.NO_RECEIPT;
        }

        int lastDefaulterIndex = currentPropertyIndex > 0 ? currentPropertyIndex - 1 : 0;
        if (loanSharkTippedIndices.contains(lastDefaulterIndex)) {
            return LoanSharkTipResult.ALREADY_TIPPED;
        }

        inventory.removeItem(Material.CATALOGUE_RECEIPT, 1);
        inventory.addItem(Material.COIN, LOAN_SHARK_FINDER_FEE);
        loanSharkTippedIndices.add(lastDefaulterIndex);

        loanSharkTipOffs++;

        if (rumourNetwork != null && witnessNpc != null) {
            rumourNetwork.addRumour(witnessNpc,
                    new Rumour(RumourType.DEBT_COLLECTOR,
                            "Loan shark's been calling at doors. Someone grassed up people behind on their catalogue."));
        }

        checkLoanSharkInformant(cb);
        return LoanSharkTipResult.TIPPED;
    }

    private void checkLoanSharkInformant(AchievementCallback cb) {
        if (!loanSharkInformantAwarded && loanSharkTipOffs >= LOAN_SHARK_INFORMANT_THRESHOLD && cb != null) {
            loanSharkInformantAwarded = true;
            cb.award(AchievementType.LOAN_SHARK_INFORMANT);
        }
    }

    // ── Mechanic 3 — Trading Standards Trap ──────────────────────────────────

    /**
     * Player reports Barry to Trading Standards using their CATALOGUE_SAMPLE.
     *
     * @param inventory  player's inventory (CATALOGUE_SAMPLE consumed; COIN added)
     * @param witnessNpc NPC witness (or null); for rumour seeding
     * @param cb         achievement callback
     * @return result of the report
     */
    public TradingStandardsReportResult reportToTradingStandards(Inventory inventory,
                                                                   NPC witnessNpc,
                                                                   AchievementCallback cb) {
        if (!inventory.hasItem(Material.CATALOGUE_SAMPLE)) {
            return TradingStandardsReportResult.NO_SAMPLE;
        }
        if (barrySuspended) {
            return TradingStandardsReportResult.ALREADY_SUSPENDED;
        }

        inventory.removeItem(Material.CATALOGUE_SAMPLE, 1);
        inventory.addItem(Material.COIN, TRADING_STANDARDS_REWARD);
        barrySuspended = true;
        suspensionDaysRemaining = TRADING_STANDARDS_SUSPENSION_DAYS;

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(-TRADING_STANDARDS_NOTORIETY_REDUCTION, cb);
        }

        if (rumourNetwork != null && witnessNpc != null) {
            rumourNetwork.addRumour(witnessNpc,
                    new Rumour(RumourType.BARRY_REPORTED,
                            "They've suspended that catalogue man. Stuff was all knocked off apparently."));
        }

        if (!civicCrusaderAwarded && cb != null) {
            civicCrusaderAwarded = true;
            cb.award(AchievementType.CIVIC_CRUSADER);
        }

        return TradingStandardsReportResult.REPORTED;
    }

    /**
     * Player attempts to blackmail Barry using their CATALOGUE_SAMPLE.
     *
     * <p>First blackmail: Barry pays {@link #BLACKMAIL_PAYMENT} COIN.
     * Second blackmail: {@link CrimeType#EXTORTION} recorded and Wanted +1.
     *
     * @param inventory  player's inventory (CATALOGUE_SAMPLE consumed; COIN added on success)
     * @param cb         achievement callback
     * @return result of the blackmail attempt
     */
    public BlackmailResult blackmailBarry(Inventory inventory, AchievementCallback cb) {
        if (!inventory.hasItem(Material.CATALOGUE_SAMPLE)) {
            return BlackmailResult.NO_SAMPLE;
        }
        if (barryHostile || barrySuspended) {
            return BlackmailResult.BARRY_UNAVAILABLE;
        }

        blackmailCount++;

        if (blackmailCount == 1) {
            inventory.removeItem(Material.CATALOGUE_SAMPLE, 1);
            inventory.addItem(Material.COIN, BLACKMAIL_PAYMENT);
            barryHostile = true;
            barryHostileHoursRemaining = 24 * 3600f; // Hostile for 1 day

            if (!silentPartnerAwarded && cb != null) {
                silentPartnerAwarded = true;
                cb.award(AchievementType.SILENT_PARTNER);
            }
            return BlackmailResult.ACCEPTED;
        } else {
            // Second blackmail — extortion
            inventory.removeItem(Material.CATALOGUE_SAMPLE, 1);
            inventory.addItem(Material.COIN, BLACKMAIL_PAYMENT);

            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.EXTORTION);
            }
            if (wantedSystem != null) {
                wantedSystem.increaseWantedStars(1);
            }

            return BlackmailResult.EXTORTION_TRIGGERED;
        }
    }

    /**
     * Player attempts a rival catalogue sale at a residential door.
     *
     * <p>Requires {@link Material#KNOCKOFF_CATALOGUE} in inventory.
     * Trading Standards check fires on the last Friday of the month at 14:00:
     * caller should pass {@code isMonthlyTradingStandardsCheck=true} at that time.
     *
     * @param inventory                     player's inventory (KNOCKOFF_CATALOGUE required)
     * @param isMonthlyTradingStandardsCheck true if this is the monthly TS check window
     * @param witnessNpc                     NPC witness (or null)
     * @param cb                             achievement callback
     * @return result of the sale attempt
     */
    public RivalCatalogueResult doRivalCatalogueSale(Inventory inventory,
                                                       boolean isMonthlyTradingStandardsCheck,
                                                       NPC witnessNpc,
                                                       AchievementCallback cb) {
        if (!inventory.hasItem(Material.KNOCKOFF_CATALOGUE)) {
            return RivalCatalogueResult.NO_CATALOGUE;
        }

        // Monthly Trading Standards check
        if (isMonthlyTradingStandardsCheck && random.nextFloat() < TRADING_STANDARDS_CATCH_CHANCE) {
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.COUNTERFEIT_GOODS_SELLING);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(COUNTERFEIT_NOTORIETY, cb);
            }
            // Fine — remove COIN from inventory (capped at available)
            int currentCoin = inventory.getItemCount(Material.COIN);
            int fine = Math.min(COUNTERFEIT_FINE, currentCoin);
            if (fine > 0) {
                inventory.removeItem(Material.COIN, fine);
            }

            if (rumourNetwork != null && witnessNpc != null) {
                rumourNetwork.addRumour(witnessNpc,
                        new Rumour(RumourType.COUNTERFEIT_CAUGHT,
                                "Trading Standards had someone away for selling dodgy catalogues. Broad daylight."));
            }
            return RivalCatalogueResult.CAUGHT_BY_TRADING_STANDARDS;
        }

        // Normal sale attempt
        if (random.nextFloat() < RIVAL_CATALOGUE_ACCEPT_RATE) {
            inventory.addItem(Material.COIN, RIVAL_CATALOGUE_COIN_PER_SALE);

            if (!rivalCatalogueSoldToday) {
                rivalCatalogueSoldToday = true;
                rivalCatalogueSellDays++;
            }

            if (rumourNetwork != null && witnessNpc != null) {
                rumourNetwork.addRumour(witnessNpc,
                        new Rumour(RumourType.RIVAL_CATALOGUE,
                                "There's two catalogue men doing the same street now. One of them's definitely dodgy."));
            }

            checkCatalogueKing(cb);
            return RivalCatalogueResult.ACCEPTED;
        } else {
            return RivalCatalogueResult.REFUSED;
        }
    }

    private void checkCatalogueKing(AchievementCallback cb) {
        if (!catalogueKingAwarded && rivalCatalogueSellDays >= CATALOGUE_KING_THRESHOLD && cb != null) {
            catalogueKingAwarded = true;
            cb.award(AchievementType.CATALOGUE_KING);
        }
    }

    // ── Getters for test visibility ───────────────────────────────────────────

    /** Whether Barry's round is currently active. */
    public boolean isRoundActive() {
        return roundActive;
    }

    /** Whether the CATALOGUE_BAG_PROP is currently placed. */
    public boolean isBagPlaced() {
        return bagPlaced;
    }

    /** Whether the defaulter window is open (player can impersonate debt collector). */
    public boolean isDefaulterWindowOpen() {
        return defaulterWindowOpen;
    }

    /** Whether Barry is currently hostile. */
    public boolean isBarryHostile() {
        return barryHostile;
    }

    /** Whether Barry is currently suspended (reported to Trading Standards). */
    public boolean isBarrySuspended() {
        return barrySuspended;
    }

    /** Whether the player has received the CATALOGUE_SAMPLE this session. */
    public boolean isCatalogueSampleReceived() {
        return catalogueSampleReceived;
    }

    /** Number of undetected deliveries witnessed. */
    public int getWitnessedDeliveries() {
        return witnessedDeliveries;
    }

    /** Current property index in the round (0–9). */
    public int getCurrentPropertyIndex() {
        return currentPropertyIndex;
    }

    /** Number of days the bag has been stolen. */
    public int getBagStolenDays() {
        return bagStolenDays;
    }

    /** Number of successful debt collection impersonations. */
    public int getDebtCollectionSuccesses() {
        return debtCollectionSuccesses;
    }

    /** Number of Loan Shark tip-offs. */
    public int getLoanSharkTipOffs() {
        return loanSharkTipOffs;
    }

    /** Number of rival catalogue selling days. */
    public int getRivalCatalogueSellDays() {
        return rivalCatalogueSellDays;
    }

    /**
     * Directly set the number of witnessed deliveries — for testing / CATALOGUE_SAMPLE grant.
     */
    public void setWitnessedDeliveries(int count) {
        this.witnessedDeliveries = count;
        if (count >= WITNESS_THRESHOLD_FOR_SAMPLE) {
            this.catalogueSampleReceived = true;
        }
    }

    /**
     * Directly set bag-stolen-days count — for testing BARRY_BANDIT threshold.
     */
    public void setBagStolenDays(int days) {
        this.bagStolenDays = days;
    }

    /**
     * Directly set debt collection success count — for testing DEBT_DODGER threshold.
     */
    public void setDebtCollectionSuccesses(int count) {
        this.debtCollectionSuccesses = count;
    }

    /**
     * Directly set loan shark tip-off count — for testing LOAN_SHARK_INFORMANT threshold.
     */
    public void setLoanSharkTipOffs(int count) {
        this.loanSharkTipOffs = count;
    }

    /**
     * Directly set rival catalogue sell days — for testing CATALOGUE_KING threshold.
     */
    public void setRivalCatalogueSellDays(int days) {
        this.rivalCatalogueSellDays = days;
    }

    /**
     * Expose the defaulter window open state for testing.
     * Also exposes a way to force open the window.
     */
    public void forceDefaulterWindowOpen(boolean open) {
        this.defaulterWindowOpen = open;
        if (open && !defaulterIndices.contains(currentPropertyIndex)) {
            defaulterIndices.add(currentPropertyIndex);
        }
    }

    /**
     * Directly mark Barry as hostile — used in tests.
     */
    public void setBarryHostile(boolean hostile) {
        this.barryHostile = hostile;
        this.barryHostileHoursRemaining = hostile ? 24 * 3600f : 0f;
    }

    /**
     * Place/remove the bag prop directly — used in tests.
     */
    public void setBagPlaced(boolean placed) {
        this.bagPlaced = placed;
    }

    /**
     * Force the blackmail count — for testing second-blackmail extortion path.
     */
    public void setBlackmailCount(int count) {
        this.blackmailCount = count;
    }
}

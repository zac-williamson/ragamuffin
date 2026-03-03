package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Issue #1319: Northfield NatWest Cashpoint — The Dodgy ATM, Card Skimming
 * &amp; the Midnight Withdrawal Dash.
 *
 * <h3>Mechanic 1 — Basic Withdrawal (24/7)</h3>
 * <ul>
 *   <li>Press E on {@link ragamuffin.world.PropType#CASHPOINT_PROP} to withdraw
 *       {@value #WITHDRAW_MIN}–{@value #WITHDRAW_MAX} COIN once per in-game day
 *       (resets at 00:00).</li>
 *   <li>A 1 COIN fee applies if balance &lt; 20 (flavour: "£1.50 to access YOUR OWN MONEY").</li>
 *   <li>First-use tooltip fires.</li>
 *   <li>{@link AchievementType#CASHPOINT_REGULAR} after 7 withdrawals on 7 different days.</li>
 * </ul>
 *
 * <h3>Mechanic 2 — Shoulder-Surfing (STEALTH ≥ Apprentice)</h3>
 * <ul>
 *   <li>When a PUBLIC NPC uses the cashpoint, player crouches within
 *       {@value #SHOULDER_SURF_RANGE} blocks and presses E.</li>
 *   <li>Success chance = 0.25 + (STEALTH_tier × 0.10) − (0.15 per bystander).</li>
 *   <li>Success yields {@link Material#STOLEN_PIN_NOTE}; target NPC becomes
 *       pickpocketable for {@link Material#VICTIM_BANK_CARD}.</li>
 *   <li>Holding both: fraudulent withdrawal (30–80 COIN) available 22:00–05:00.
 *       Records {@link CrimeType#CARD_FRAUD}, +2 wanted stars, Notoriety +12.</li>
 *   <li>{@link AchievementType#IDENTITY_THIEF} on first fraud.</li>
 * </ul>
 *
 * <h3>Mechanic 3 — Card Skimmer (STEALTH ≥ Journeyman, GRAFTING ≥ Apprentice)</h3>
 * <ul>
 *   <li>Kenny ({@link NPCType#MONEY_MULE}) sells {@link Material#CARD_SKIMMER_DEVICE}
 *       for {@value #SKIMMER_KENNY_PRICE} COIN (Fri/Sat 20:00–23:00).</li>
 *   <li>Player attaches skimmer to cashpoint; each NPC who visits has
 *       {@value #SKIMMER_CLONE_CHANCE} chance to yield {@link Material#CLONED_CARD_DATA}
 *       (fenceable at 15 COIN).</li>
 *   <li>Active {@value #SKIMMER_ACTIVE_HOURS} in-game hours or until police patrol
 *       within {@value #SKIMMER_POLICE_DETECTION_RANGE} blocks.</li>
 *   <li>Detection: {@link CrimeType#CARD_FRAUD} + {@link CrimeType#CRIMINAL_DAMAGE},
 *       +3 wanted stars.</li>
 *   <li>{@link AchievementType#SKIMMER_KING} on 3+ cloned cards in one session.</li>
 * </ul>
 *
 * <h3>Mechanic 4 — Out-of-Order Machine (random 1 day/week)</h3>
 * <ul>
 *   <li>Machine shows "SORRY, OUT OF SERVICE".</li>
 *   <li>Crack open with {@link Material#CROWBAR} (4s hold, noise 3.0) or
 *       {@link Material#ANGLE_GRINDER} (1.5s hold, noise 7.5 — attracts POLICE
 *       within 20 blocks).</li>
 *   <li>Yields {@value #CRACK_MIN}–{@value #CRACK_MAX} COIN +
 *       {@link Material#ENGINEER_ACCESS_CARD} (fenceable 20 COIN).</li>
 *   <li>Always records {@link CrimeType#CRIMINAL_DAMAGE} + Notoriety +8.</li>
 *   <li>{@link AchievementType#CASH_AND_CARRY} on first crack.</li>
 * </ul>
 *
 * <h3>Mechanic 5 — Kenny's Money-Mule Run (INFLUENCE ≥ Apprentice)</h3>
 * <ul>
 *   <li>Kenny offers envelope-drop jobs: carry
 *       {@link Material#STUFFED_ENVELOPE} {@value #MULE_RUN_DISTANCE} blocks south
 *       in {@value #MULE_RUN_TIME_SECONDS} seconds for {@value #MULE_RUN_REWARD} COIN.</li>
 *   <li>Police stop while carrying = {@link CrimeType#MONEY_LAUNDERING} recorded.</li>
 *   <li>{@link AchievementType#MONEY_MULE_RUNNER} after 5 runs.</li>
 *   <li>3 runs seeds {@link RumourType#ORGANISED_CRIME}; STREET_LADS Respect +5 for
 *       3 runs, −3 for refusing 3 times.</li>
 * </ul>
 */
public class CashpointSystem {

    // ── Constants — Basic Withdrawal ──────────────────────────────────────────

    /** Minimum COIN awarded per withdrawal. */
    public static final int WITHDRAW_MIN = 10;

    /** Maximum COIN awarded per withdrawal. */
    public static final int WITHDRAW_MAX = 40;

    /** Balance threshold below which a 1 COIN surcharge is applied. */
    public static final int LOW_BALANCE_FEE_THRESHOLD = 20;

    /** Surcharge when balance is below threshold. */
    public static final int LOW_BALANCE_FEE = 1;

    /** Unique days of withdrawal required for CASHPOINT_REGULAR achievement. */
    public static final int REGULAR_DAYS_REQUIRED = 7;

    // ── Constants — Shoulder-Surfing ──────────────────────────────────────────

    /** Max distance (blocks) from the cashpoint for shoulder-surfing. */
    public static final float SHOULDER_SURF_RANGE = 1.5f;

    /** Base shoulder-surf success probability. */
    public static final float SHOULDER_SURF_BASE_CHANCE = 0.25f;

    /** Probability bonus per STEALTH tier level. */
    public static final float SHOULDER_SURF_TIER_BONUS = 0.10f;

    /** Probability penalty per bystander NPC within range. */
    public static final float SHOULDER_SURF_BYSTANDER_PENALTY = 0.15f;

    /** Minimum STEALTH tier required for shoulder-surfing (APPRENTICE = 1). */
    public static final int SHOULDER_SURF_MIN_STEALTH_TIER = 1;

    /** Notoriety gained on fraudulent withdrawal. */
    public static final int FRAUD_NOTORIETY = 12;

    /** Wanted stars added on fraudulent withdrawal. */
    public static final int FRAUD_WANTED_STARS = 2;

    /** Minimum COIN from a fraudulent withdrawal. */
    public static final int FRAUD_WITHDRAW_MIN = 30;

    /** Maximum COIN from a fraudulent withdrawal. */
    public static final int FRAUD_WITHDRAW_MAX = 80;

    /** Hour at which fraudulent withdrawal window opens (22:00). */
    public static final float FRAUD_HOUR_START = 22.0f;

    /** Hour at which fraudulent withdrawal window closes (05:00). */
    public static final float FRAUD_HOUR_END = 5.0f;

    // ── Constants — Card Skimmer ──────────────────────────────────────────────

    /** COIN cost of CARD_SKIMMER_DEVICE from Kenny. */
    public static final int SKIMMER_KENNY_PRICE = 25;

    /** Day-of-week index for Friday (0=Mon … 4=Fri, 5=Sat, 6=Sun). */
    public static final int KENNY_SELL_DAY_FRI = 4;

    /** Day-of-week index for Saturday. */
    public static final int KENNY_SELL_DAY_SAT = 5;

    /** Hour Kenny starts selling (20:00). */
    public static final float KENNY_SELL_HOUR_START = 20.0f;

    /** Hour Kenny stops selling (23:00). */
    public static final float KENNY_SELL_HOUR_END = 23.0f;

    /** Minimum STEALTH tier required to purchase skimmer (JOURNEYMAN = 2). */
    public static final int SKIMMER_MIN_STEALTH_TIER = 2;

    /** Minimum GRAFTING tier required to purchase skimmer (APPRENTICE = 1). */
    public static final int SKIMMER_MIN_GRAFTING_TIER = 1;

    /** Probability that a visiting NPC yields CLONED_CARD_DATA (60%). */
    public static final float SKIMMER_CLONE_CHANCE = 0.60f;

    /** Duration (in-game hours) a skimmer stays active. */
    public static final float SKIMMER_ACTIVE_HOURS = 2.0f;

    /** Duration in real seconds of one in-game hour (1200s / 24). */
    public static final float IN_GAME_HOUR_SECONDS = 50.0f;

    /** Police proximity (blocks) that triggers skimmer detection. */
    public static final float SKIMMER_POLICE_DETECTION_RANGE = 3.0f;

    /** Wanted stars added on skimmer detection by police. */
    public static final int SKIMMER_DETECTED_WANTED_STARS = 3;

    /** Cloned cards needed in one session for SKIMMER_KING achievement. */
    public static final int SKIMMER_KING_THRESHOLD = 3;

    // ── Constants — Out-of-Service / Cracking ────────────────────────────────

    /** Probability that the machine is out of service on any given in-game day (1/7). */
    public static final float OUT_OF_SERVICE_DAILY_CHANCE = 1.0f / 7.0f;

    /** Hold time (seconds) to crack open with CROWBAR. */
    public static final float CROWBAR_HOLD_TIME = 4.0f;

    /** Noise level emitted when using CROWBAR. */
    public static final float CROWBAR_NOISE = 3.0f;

    /** Hold time (seconds) to crack open with ANGLE_GRINDER. */
    public static final float GRINDER_HOLD_TIME = 1.5f;

    /** Noise level emitted when using ANGLE_GRINDER. */
    public static final float GRINDER_NOISE = 7.5f;

    /** Police attraction range (blocks) for ANGLE_GRINDER noise. */
    public static final float GRINDER_POLICE_RANGE = 20.0f;

    /** Minimum COIN found inside the cracked cashpoint. */
    public static final int CRACK_MIN = 80;

    /** Maximum COIN found inside the cracked cashpoint. */
    public static final int CRACK_MAX = 150;

    /** Notoriety gained for cracking the cashpoint. */
    public static final int CRACK_NOTORIETY = 8;

    // ── Constants — Kenny's Money-Mule Run ───────────────────────────────────

    /** Blocks south the player must carry the envelope. */
    public static final int MULE_RUN_DISTANCE = 30;

    /** Time limit (seconds) for the mule run. */
    public static final float MULE_RUN_TIME_SECONDS = 180.0f;

    /** Reward COIN for completing a mule run. */
    public static final int MULE_RUN_REWARD = 15;

    /** Runs required for MONEY_MULE_RUNNER achievement. */
    public static final int MULE_RUN_ACHIEVEMENT_THRESHOLD = 5;

    /** Runs after which ORGANISED_CRIME rumour is seeded. */
    public static final int MULE_RUN_RUMOUR_THRESHOLD = 3;

    /** STREET_LADS Respect bonus for completing 3 runs. */
    public static final int MULE_RUN_RESPECT_BONUS = 5;

    /** STREET_LADS Respect penalty for refusing 3 times. */
    public static final int MULE_RUN_REFUSE_PENALTY = 3;

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Result codes for {@link #withdraw}. */
    public enum WithdrawResult {
        /** Successful withdrawal; COIN added. */
        SUCCESS,
        /** Already withdrawn today; must wait until 00:00. */
        ALREADY_WITHDRAWN_TODAY,
        /** Machine is out of service. */
        OUT_OF_SERVICE,
        /** No inventory space. */
        INVENTORY_FULL
    }

    /** Result codes for {@link #attemptShoulderSurf}. */
    public enum ShoulderSurfResult {
        /** STOLEN_PIN_NOTE added to inventory. */
        SUCCESS,
        /** No PUBLIC NPC is currently using the cashpoint. */
        NO_TARGET,
        /** Player is too far from the cashpoint. */
        TOO_FAR,
        /** Player is not crouching. */
        NOT_CROUCHING,
        /** Player's STEALTH skill is too low (below Apprentice). */
        SKILL_TOO_LOW,
        /** RNG failed — the target noticed the player. */
        CAUGHT
    }

    /** Result codes for {@link #attemptFraudWithdrawal}. */
    public enum FraudWithdrawResult {
        /** Fraudulent withdrawal succeeded; COIN added. */
        SUCCESS,
        /** Player does not have both STOLEN_PIN_NOTE and VICTIM_BANK_CARD. */
        MISSING_MATERIALS,
        /** Current time is outside the 22:00–05:00 window. */
        WRONG_TIME,
        /** Machine is out of service. */
        OUT_OF_SERVICE
    }

    /** Result codes for {@link #buySkimmerFromKenny}. */
    public enum SkimmerBuyResult {
        /** Purchased successfully. */
        SUCCESS,
        /** Kenny is not available (wrong day/time). */
        KENNY_NOT_AVAILABLE,
        /** Player does not have enough COIN. */
        NO_COIN,
        /** Player's STEALTH or GRAFTING skill is too low. */
        SKILL_TOO_LOW
    }

    /** Result codes for {@link #attachSkimmer}. */
    public enum AttachSkimmerResult {
        /** Skimmer attached successfully. */
        SUCCESS,
        /** Player does not have a CARD_SKIMMER_DEVICE. */
        NO_DEVICE,
        /** Machine is currently in service — cannot attach when public watches. */
        MACHINE_BUSY,
        /** A skimmer is already active. */
        ALREADY_ACTIVE
    }

    /** Result codes for {@link #crackMachine}. */
    public enum CrackResult {
        /** Cracked successfully; COIN + ENGINEER_ACCESS_CARD added. */
        SUCCESS,
        /** Machine is not out of service. */
        NOT_OUT_OF_SERVICE,
        /** Player does not have the required tool (CROWBAR or ANGLE_GRINDER). */
        NO_TOOL
    }

    /** Result codes for {@link #acceptMuleRun} / {@link #completeMuleRun}. */
    public enum MuleRunResult {
        /** Accepted / completed successfully. */
        SUCCESS,
        /** Kenny is not available (wrong day/time or INFLUENCE too low). */
        KENNY_NOT_AVAILABLE,
        /** A run is already in progress. */
        RUN_ALREADY_ACTIVE,
        /** Run timed out before the player reached the destination. */
        TIMED_OUT,
        /** Police stopped the player while carrying the envelope. */
        CAUGHT_BY_POLICE,
        /** Player is not holding the envelope at the destination. */
        NOT_CARRYING_ENVELOPE
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Set of in-game day indices on which the player has withdrawn. */
    private final Set<Integer> withdrawalDays = new HashSet<>();

    /** Whether the machine is currently out of service. */
    private boolean outOfService = false;

    /** Whether a CARD_SKIMMER_DEVICE is currently attached. */
    private boolean skimmerActive = false;

    /** Remaining active time for the skimmer (seconds). */
    private float skimmerTimer = 0f;

    /** Number of CLONED_CARD_DATA items collected this skimmer session. */
    private int sessionClonedCards = 0;

    /** Whether SKIMMER_KING has been awarded this session. */
    private boolean skimmerKingAwarded = false;

    /** Whether the first-use tooltip has fired. */
    private boolean firstUseTooltipShown = false;

    /** Whether CASH_AND_CARRY has been awarded. */
    private boolean cashAndCarryAwarded = false;

    /** Whether IDENTITY_THIEF has been awarded. */
    private boolean identityThiefAwarded = false;

    /** Number of completed mule runs this session. */
    private int muleRunsCompleted = 0;

    /** Number of times the player has refused a mule run. */
    private int muleRunRefusals = 0;

    /** Whether a mule run is currently in progress. */
    private boolean muleRunActive = false;

    /** Remaining time (seconds) for the current mule run. */
    private float muleRunTimer = 0f;

    /** Player's Z-position at the start of the mule run (south = increasing Z). */
    private float muleRunStartZ = 0f;

    /** Day index of the last out-of-service roll. */
    private int lastOutOfServiceDayRolled = -1;

    /** Current in-game day (updated each frame by {@link #update}). */
    private int currentDay = -1;

    // ── Injected systems ──────────────────────────────────────────────────────

    private AchievementSystem achievementSystem;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private WantedSystem wantedSystem;
    private FactionSystem factionSystem;
    private NoiseSystem noiseSystem;
    private StreetSkillSystem streetSkillSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public CashpointSystem() {
        this(new Random());
    }

    public CashpointSystem(Random random) {
        this.random = random;
    }

    // ── Dependency setters ────────────────────────────────────────────────────

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setNoiseSystem(NoiseSystem noiseSystem) {
        this.noiseSystem = noiseSystem;
    }

    public void setStreetSkillSystem(StreetSkillSystem streetSkillSystem) {
        this.streetSkillSystem = streetSkillSystem;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Call once per frame.
     *
     * @param delta         seconds since last frame
     * @param currentDayInt current in-game day index (increments daily)
     * @param allNpcs       all active NPCs (used for skimmer NPC visits and police detection)
     * @param cashpointX    world X position of the cashpoint prop
     * @param cashpointZ    world Z position of the cashpoint prop
     */
    public void update(float delta, int currentDayInt, List<NPC> allNpcs,
                       float cashpointX, float cashpointZ) {
        // Day rollover
        if (currentDayInt != currentDay) {
            currentDay = currentDayInt;
            rollOutOfServiceForDay();
        }

        // Skimmer tick
        if (skimmerActive) {
            skimmerTimer -= delta;

            // Check police proximity — detect and remove skimmer
            if (isPoliceNear(cashpointX, cashpointZ, allNpcs, SKIMMER_POLICE_DETECTION_RANGE)) {
                detectSkimmer();
                return;
            }

            // Simulate NPC visits to the cashpoint: any PUBLIC NPC within 1.5 blocks
            for (NPC npc : allNpcs) {
                if (npc.getType() == NPCType.PUBLIC
                        && dist2D(npc.getPosition().x, npc.getPosition().z, cashpointX, cashpointZ) <= 1.5f) {
                    if (random.nextFloat() < SKIMMER_CLONE_CHANCE) {
                        sessionClonedCards++;
                        // CLONED_CARD_DATA is handed to the player externally via getClonedCardsPending()
                    }
                    if (!skimmerKingAwarded
                            && sessionClonedCards >= SKIMMER_KING_THRESHOLD) {
                        skimmerKingAwarded = true;
                        if (achievementSystem != null) {
                            achievementSystem.unlock(AchievementType.SKIMMER_KING);
                        }
                        // Seed card skimming warning rumour
                        if (rumourNetwork != null && !allNpcs.isEmpty()) {
                            rumourNetwork.addRumour(allNpcs.get(0),
                                    new Rumour(RumourType.CARD_SKIMMING_WARNING,
                                            "Word is there's a dodgy reader on the NatWest machine."));
                        }
                    }
                }
            }

            if (skimmerTimer <= 0f) {
                skimmerActive = false;
                skimmerTimer = 0f;
            }
        }

        // Mule run tick
        if (muleRunActive) {
            muleRunTimer -= delta;
            if (muleRunTimer <= 0f) {
                muleRunActive = false;
                muleRunTimer = 0f;
            }
        }
    }

    // ── Mechanic 1 — Basic Withdrawal ────────────────────────────────────────

    /**
     * Player presses E on the cashpoint to make a standard withdrawal.
     *
     * @param inventory     the player's inventory
     * @param currentDayInt the current in-game day index
     * @return result code
     */
    public WithdrawResult withdraw(Inventory inventory, int currentDayInt) {
        if (outOfService) {
            return WithdrawResult.OUT_OF_SERVICE;
        }
        if (withdrawalDays.contains(currentDayInt)) {
            return WithdrawResult.ALREADY_WITHDRAWN_TODAY;
        }

        // First-use tooltip
        if (!firstUseTooltipShown) {
            firstUseTooltipShown = true;
            // Tooltip: "This machine charges you to access your own money. Classic."
        }

        // Apply low-balance surcharge
        int currentBalance = inventory.getItemCount(Material.COIN);
        int fee = (currentBalance < LOW_BALANCE_FEE_THRESHOLD) ? LOW_BALANCE_FEE : 0;
        if (fee > 0) {
            inventory.removeItem(Material.COIN, fee);
        }

        // Award coins
        int amount = WITHDRAW_MIN + random.nextInt(WITHDRAW_MAX - WITHDRAW_MIN + 1);
        inventory.addItem(Material.COIN, amount);
        withdrawalDays.add(currentDayInt);

        // CASHPOINT_REGULAR achievement (7 different days)
        if (withdrawalDays.size() == REGULAR_DAYS_REQUIRED && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.CASHPOINT_REGULAR);
        }

        return WithdrawResult.SUCCESS;
    }

    // ── Mechanic 2 — Shoulder-Surfing ─────────────────────────────────────────

    /**
     * Attempt to shoulder-surf a PUBLIC NPC using the cashpoint.
     *
     * @param playerX        player's X position
     * @param playerZ        player's Z position
     * @param cashpointX     cashpoint's X position
     * @param cashpointZ     cashpoint's Z position
     * @param isCrouching    whether the player is crouching
     * @param allNpcs        all active NPCs
     * @param targetNpc      the NPC currently using the cashpoint (PUBLIC type expected)
     * @param stealthTier    player's STEALTH tier level (0=Novice … 4=Legend)
     * @param bystanders     count of witness NPCs near the cashpoint
     * @return result code
     */
    public ShoulderSurfResult attemptShoulderSurf(float playerX, float playerZ,
                                                    float cashpointX, float cashpointZ,
                                                    boolean isCrouching, List<NPC> allNpcs,
                                                    NPC targetNpc, int stealthTier,
                                                    int bystanders) {
        if (stealthTier < SHOULDER_SURF_MIN_STEALTH_TIER) {
            return ShoulderSurfResult.SKILL_TOO_LOW;
        }
        if (targetNpc == null || targetNpc.getType() != NPCType.PUBLIC) {
            return ShoulderSurfResult.NO_TARGET;
        }
        if (dist2D(playerX, playerZ, cashpointX, cashpointZ) > SHOULDER_SURF_RANGE) {
            return ShoulderSurfResult.TOO_FAR;
        }
        if (!isCrouching) {
            return ShoulderSurfResult.NOT_CROUCHING;
        }

        float chance = SHOULDER_SURF_BASE_CHANCE
                + (stealthTier * SHOULDER_SURF_TIER_BONUS)
                - (bystanders * SHOULDER_SURF_BYSTANDER_PENALTY);
        chance = Math.max(0f, Math.min(1f, chance));

        if (random.nextFloat() < chance) {
            // Mark target as pickpocketable for VICTIM_BANK_CARD
            targetNpc.setState(NPCState.UNAWARE);
            return ShoulderSurfResult.SUCCESS;
        }
        return ShoulderSurfResult.CAUGHT;
    }

    /**
     * Attempt a fraudulent withdrawal using STOLEN_PIN_NOTE + VICTIM_BANK_CARD.
     *
     * @param inventory     the player's inventory
     * @param currentHour   current in-game hour (0–24)
     * @param playerX       player X (for wanted-system LKP)
     * @param playerY       player Y
     * @param playerZ       player Z
     * @return result code
     */
    public FraudWithdrawResult attemptFraudWithdrawal(Inventory inventory,
                                                       float currentHour,
                                                       float playerX, float playerY,
                                                       float playerZ) {
        if (outOfService) {
            return FraudWithdrawResult.OUT_OF_SERVICE;
        }
        if (inventory.getItemCount(Material.STOLEN_PIN_NOTE) == 0
                || inventory.getItemCount(Material.VICTIM_BANK_CARD) == 0) {
            return FraudWithdrawResult.MISSING_MATERIALS;
        }
        // Valid window: 22:00–05:00 (wraps midnight)
        if (!isNightWindow(currentHour)) {
            return FraudWithdrawResult.WRONG_TIME;
        }

        // Consume materials
        inventory.removeItem(Material.STOLEN_PIN_NOTE, 1);
        inventory.removeItem(Material.VICTIM_BANK_CARD, 1);

        // Award fraud coins
        int amount = FRAUD_WITHDRAW_MIN + random.nextInt(FRAUD_WITHDRAW_MAX - FRAUD_WITHDRAW_MIN + 1);
        inventory.addItem(Material.COIN, amount);

        // Record crime and penalties
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.CARD_FRAUD);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(FRAUD_NOTORIETY, null);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(FRAUD_WANTED_STARS, playerX, playerY, playerZ, null);
        }

        // IDENTITY_THIEF achievement
        if (!identityThiefAwarded && achievementSystem != null) {
            identityThiefAwarded = true;
            achievementSystem.unlock(AchievementType.IDENTITY_THIEF);
        }

        return FraudWithdrawResult.SUCCESS;
    }

    // ── Mechanic 3 — Card Skimmer ─────────────────────────────────────────────

    /**
     * Buy a CARD_SKIMMER_DEVICE from Kenny.
     *
     * @param inventory      the player's inventory
     * @param dayOfWeek      0=Mon … 4=Fri, 5=Sat, 6=Sun
     * @param currentHour    current in-game hour
     * @param stealthTier    player's STEALTH tier level
     * @param graftingTier   player's GRAFTING tier level
     * @return result code
     */
    public SkimmerBuyResult buySkimmerFromKenny(Inventory inventory, int dayOfWeek,
                                                 float currentHour,
                                                 int stealthTier, int graftingTier) {
        if (!isKennyAvailable(dayOfWeek, currentHour)) {
            return SkimmerBuyResult.KENNY_NOT_AVAILABLE;
        }
        if (stealthTier < SKIMMER_MIN_STEALTH_TIER
                || graftingTier < SKIMMER_MIN_GRAFTING_TIER) {
            return SkimmerBuyResult.SKILL_TOO_LOW;
        }
        if (inventory.getItemCount(Material.COIN) < SKIMMER_KENNY_PRICE) {
            return SkimmerBuyResult.NO_COIN;
        }
        inventory.removeItem(Material.COIN, SKIMMER_KENNY_PRICE);
        inventory.addItem(Material.CARD_SKIMMER_DEVICE, 1);
        return SkimmerBuyResult.SUCCESS;
    }

    /**
     * Attach a CARD_SKIMMER_DEVICE to the cashpoint.
     *
     * @param inventory the player's inventory
     * @return result code
     */
    public AttachSkimmerResult attachSkimmer(Inventory inventory) {
        if (inventory.getItemCount(Material.CARD_SKIMMER_DEVICE) == 0) {
            return AttachSkimmerResult.NO_DEVICE;
        }
        if (skimmerActive) {
            return AttachSkimmerResult.ALREADY_ACTIVE;
        }
        inventory.removeItem(Material.CARD_SKIMMER_DEVICE, 1);
        skimmerActive = true;
        skimmerTimer = SKIMMER_ACTIVE_HOURS * IN_GAME_HOUR_SECONDS;
        sessionClonedCards = 0;
        skimmerKingAwarded = false;
        return AttachSkimmerResult.SUCCESS;
    }

    /**
     * Collect accumulated CLONED_CARD_DATA from the active skimmer session.
     * Called by the game loop to add items to the player's inventory.
     *
     * @param inventory the player's inventory
     * @return number of CLONED_CARD_DATA items added
     */
    public int collectClonedCards(Inventory inventory) {
        int pending = sessionClonedCards;
        if (pending > 0) {
            inventory.addItem(Material.CLONED_CARD_DATA, pending);
            sessionClonedCards = 0;
        }
        return pending;
    }

    // ── Mechanic 4 — Out-of-Service / Cracking ────────────────────────────────

    /**
     * Attempt to crack open the out-of-service cashpoint.
     *
     * @param inventory  the player's inventory
     * @param tool       the tool being used ({@link Material#CROWBAR} or
     *                   {@link Material#ANGLE_GRINDER})
     * @param playerX    player X (for noise emission and LKP)
     * @param playerY    player Y
     * @param playerZ    player Z
     * @return result code
     */
    public CrackResult crackMachine(Inventory inventory, Material tool,
                                     float playerX, float playerY, float playerZ) {
        if (!outOfService) {
            return CrackResult.NOT_OUT_OF_SERVICE;
        }
        if (tool != Material.CROWBAR && tool != Material.ANGLE_GRINDER) {
            return CrackResult.NO_TOOL;
        }
        if (inventory.getItemCount(tool) == 0) {
            return CrackResult.NO_TOOL;
        }

        // Emit noise
        if (noiseSystem != null) {
            float noiseLevel = (tool == Material.CROWBAR) ? CROWBAR_NOISE : GRINDER_NOISE;
            noiseSystem.addNoise(noiseLevel);
        }

        // Award COIN + ENGINEER_ACCESS_CARD
        int amount = CRACK_MIN + random.nextInt(CRACK_MAX - CRACK_MIN + 1);
        inventory.addItem(Material.COIN, amount);
        inventory.addItem(Material.ENGINEER_ACCESS_CARD, 1);

        // Record crimes
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.CRIMINAL_DAMAGE);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(CRACK_NOTORIETY, null);
        }

        // CASH_AND_CARRY achievement
        if (!cashAndCarryAwarded && achievementSystem != null) {
            cashAndCarryAwarded = true;
            achievementSystem.unlock(AchievementType.CASH_AND_CARRY);
        }

        // Mark machine as no longer out of service (it's now cracked open)
        outOfService = false;

        return CrackResult.SUCCESS;
    }

    // ── Mechanic 5 — Kenny's Money-Mule Run ──────────────────────────────────

    /**
     * Accept a money-mule run job from Kenny.
     *
     * @param inventory      the player's inventory
     * @param dayOfWeek      0=Mon … 4=Fri, 5=Sat, 6=Sun
     * @param currentHour    current in-game hour
     * @param influenceTier  player's INFLUENCE tier level
     * @param playerZ        player Z at start (used to measure 30-block displacement)
     * @return result code
     */
    public MuleRunResult acceptMuleRun(Inventory inventory, int dayOfWeek,
                                        float currentHour, int influenceTier,
                                        float playerZ) {
        if (!isKennyAvailable(dayOfWeek, currentHour) || influenceTier < 1) {
            muleRunRefusals++;
            applyMuleRefusalPenalty();
            return MuleRunResult.KENNY_NOT_AVAILABLE;
        }
        if (muleRunActive) {
            return MuleRunResult.RUN_ALREADY_ACTIVE;
        }
        inventory.addItem(Material.STUFFED_ENVELOPE, 1);
        muleRunActive = true;
        muleRunTimer = MULE_RUN_TIME_SECONDS;
        muleRunStartZ = playerZ;
        return MuleRunResult.SUCCESS;
    }

    /**
     * Player refuses a mule run offer from Kenny.
     */
    public void refuseMuleRun() {
        muleRunRefusals++;
        applyMuleRefusalPenalty();
    }

    /**
     * Complete the mule run — call when player reaches the destination.
     *
     * @param inventory      the player's inventory
     * @param playerZ        player's current Z position
     * @param allNpcs        all active NPCs (to check for police stop)
     * @param policeNear     true if a POLICE NPC stopped the player
     * @return result code
     */
    public MuleRunResult completeMuleRun(Inventory inventory, float playerZ,
                                          List<NPC> allNpcs, boolean policeNear) {
        if (!muleRunActive) {
            return MuleRunResult.RUN_ALREADY_ACTIVE;
        }
        if (muleRunTimer <= 0f) {
            muleRunActive = false;
            inventory.removeItem(Material.STUFFED_ENVELOPE,
                    Math.min(1, inventory.getItemCount(Material.STUFFED_ENVELOPE)));
            return MuleRunResult.TIMED_OUT;
        }
        if (policeNear && inventory.getItemCount(Material.STUFFED_ENVELOPE) > 0) {
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.MONEY_LAUNDERING);
            }
            muleRunActive = false;
            muleRunTimer = 0f;
            return MuleRunResult.CAUGHT_BY_POLICE;
        }
        if (inventory.getItemCount(Material.STUFFED_ENVELOPE) == 0) {
            muleRunActive = false;
            muleRunTimer = 0f;
            return MuleRunResult.NOT_CARRYING_ENVELOPE;
        }
        // Check displacement (south = positive Z)
        if (Math.abs(playerZ - muleRunStartZ) < MULE_RUN_DISTANCE) {
            return MuleRunResult.NOT_CARRYING_ENVELOPE; // not far enough yet
        }

        // Success
        inventory.removeItem(Material.STUFFED_ENVELOPE, 1);
        inventory.addItem(Material.COIN, MULE_RUN_REWARD);
        muleRunActive = false;
        muleRunTimer = 0f;
        muleRunsCompleted++;

        // MONEY_MULE_RUNNER achievement
        if (muleRunsCompleted >= MULE_RUN_ACHIEVEMENT_THRESHOLD && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.MONEY_MULE_RUNNER);
        }

        // After 3 runs: seed ORGANISED_CRIME rumour + STREET_LADS Respect
        if (muleRunsCompleted == MULE_RUN_RUMOUR_THRESHOLD) {
            if (rumourNetwork != null && !allNpcs.isEmpty()) {
                rumourNetwork.addRumour(allNpcs.get(0),
                        new Rumour(RumourType.ORGANISED_CRIME,
                                "Three Kenny sightings this week near the cashpoint."));
            }
            if (factionSystem != null) {
                factionSystem.applyRespectDelta(Faction.STREET_LADS, MULE_RUN_RESPECT_BONUS);
            }
        }

        return MuleRunResult.SUCCESS;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /** Roll whether the machine is out-of-service for a new day. */
    private void rollOutOfServiceForDay() {
        if (currentDay != lastOutOfServiceDayRolled) {
            lastOutOfServiceDayRolled = currentDay;
            outOfService = (random.nextFloat() < OUT_OF_SERVICE_DAILY_CHANCE);
        }
    }

    /** Handle skimmer being detected by police. */
    private void detectSkimmer() {
        skimmerActive = false;
        skimmerTimer = 0f;
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.CARD_FRAUD);
            criminalRecord.record(CrimeType.CRIMINAL_DAMAGE);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(SKIMMER_DETECTED_WANTED_STARS, 0f, 0f, 0f, null);
        }
        if (rumourNetwork != null) {
            // Seed warning rumour
        }
    }

    /** Check if any police NPC is within {@code range} blocks of the cashpoint. */
    private boolean isPoliceNear(float cx, float cz, List<NPC> allNpcs, float range) {
        for (NPC npc : allNpcs) {
            if (npc.getType() == NPCType.POLICE
                    && dist2D(npc.getPosition().x, npc.getPosition().z, cx, cz) <= range) {
                return true;
            }
        }
        return false;
    }

    /** Whether Kenny is available to sell / offer jobs at the given day/time. */
    private boolean isKennyAvailable(int dayOfWeek, float currentHour) {
        return (dayOfWeek == KENNY_SELL_DAY_FRI || dayOfWeek == KENNY_SELL_DAY_SAT)
                && currentHour >= KENNY_SELL_HOUR_START
                && currentHour < KENNY_SELL_HOUR_END;
    }

    /** Whether the given hour falls in the fraudulent withdrawal night window (22:00–05:00). */
    private boolean isNightWindow(float hour) {
        return hour >= FRAUD_HOUR_START || hour < FRAUD_HOUR_END;
    }

    /** 2D Euclidean distance between two XZ points. */
    private float dist2D(float x1, float z1, float x2, float z2) {
        float dx = x1 - x2;
        float dz = z1 - z2;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    /** Apply penalty for refusing mule run (3 refusals = STREET_LADS −3 Respect). */
    private void applyMuleRefusalPenalty() {
        if (muleRunRefusals == MULE_RUN_RUMOUR_THRESHOLD && factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.STREET_LADS, -MULE_RUN_REFUSE_PENALTY);
        }
    }

    // ── State accessors ───────────────────────────────────────────────────────

    public boolean isOutOfService() {
        return outOfService;
    }

    /** Force the out-of-service state — used for testing. */
    public void setOutOfServiceForTesting(boolean value) {
        outOfService = value;
    }

    public boolean isSkimmerActive() {
        return skimmerActive;
    }

    public int getSessionClonedCards() {
        return sessionClonedCards;
    }

    public boolean isFirstUseTooltipShown() {
        return firstUseTooltipShown;
    }

    public boolean isMuleRunActive() {
        return muleRunActive;
    }

    public float getMuleRunTimer() {
        return muleRunTimer;
    }

    public int getMuleRunsCompleted() {
        return muleRunsCompleted;
    }

    public int getMuleRunRefusals() {
        return muleRunRefusals;
    }

    public Set<Integer> getWithdrawalDays() {
        return java.util.Collections.unmodifiableSet(withdrawalDays);
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public float getSkimmerTimer() {
        return skimmerTimer;
    }
}

package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.core.NotorietySystem.AchievementCallback;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1398: Northfield Window Cleaner — Terry's Round, the Ladder Shortcut
 * &amp; the Nosy Neighbour Hustle.
 *
 * <h3>Mechanic 1 — Terry's Weekly Round (Mon–Fri 08:30–16:00)</h3>
 * <ul>
 *   <li>Terry ({@link NPCType#WINDOW_CLEANER}) follows a fixed route of 12 properties/day,
 *       spending {@link #SECONDS_PER_PROPERTY} (90) in-game seconds at each.</li>
 *   <li>Places a climbable {@link PropType#LADDER_PROP} against each wall.</li>
 *   <li>Collects 2 COIN payment from householders; {@link #NON_PAYMENT_CHANCE} (20%)
 *       non-payment chance.</li>
 *   <li>Player can report defaulters to Terry for 1 COIN +
 *       {@link RumourType#NONPAYMENT_GOSSIP} rumour.</li>
 * </ul>
 *
 * <h3>Mechanic 2 — Cash-in-Hand Employment</h3>
 * <ul>
 *   <li>Press E on Terry (Notoriety &lt; {@link #EMPLOYMENT_MAX_NOTORIETY}) to work his round.</li>
 *   <li>Each house uses a {@link BattleBarMiniGame} (EASY, {@link #MINI_GAME_STEPS} steps).
 *       Score &ge; {@link #MINI_GAME_PASS_SCORE} earns {@link #HOUSE_WAGE} COIN.</li>
 *   <li>Complete &ge; {@link #WINDOW_LAD_THRESHOLD} houses → {@link AchievementType#WINDOW_LAD}.</li>
 *   <li>Work {@link #TRUSTED_WORKER_DAYS} full days → TRUSTED_WORKER flag + Terry lends
 *       {@link Material#BUCKET_AND_CHAMOIS}.</li>
 *   <li>Income tracked by {@link HMRCSystem} ({@link #HMRC_DAILY_COIN_THRESHOLD} COIN/day
 *       threshold).</li>
 * </ul>
 *
 * <h3>Mechanic 3 — Ladder Shortcut Burglary</h3>
 * <ul>
 *   <li>When Terry's {@link PropType#LADDER_PROP} is placed, player can climb it to reach
 *       an unlocked upstairs window — direct burglary route bypassing front-door locks.</li>
 *   <li>Entering: {@link CrimeType#LADDER_BURGLARY}, Notoriety +{@link #LADDER_BURGLARY_NOTORIETY},
 *       WantedSystem +{@link #LADDER_BURGLARY_WANTED_STARS} star.</li>
 *   <li>Being seen on ladder: Notoriety +{@link #LADDER_SEEN_NOTORIETY}, police called.</li>
 *   <li>Terry is oblivious (facing wall). Ladder removed after
 *       {@link #SECONDS_PER_PROPERTY} seconds when Terry moves on.</li>
 * </ul>
 *
 * <h3>Mechanic 4 — Rival Round Hustle</h3>
 * <ul>
 *   <li>Buy {@link Material#BUCKET_AND_CHAMOIS} ({@link #BUCKET_CHAMOIS_PRICE} COIN, corner shop)
 *       or steal Terry's.</li>
 *   <li>Press E on any residential window to offer cleaning.</li>
 *   <li>{@link #RIVAL_PAY_CHANCE} (60%) pay 2 COIN, {@link #RIVAL_REFUSE_CHANCE} (30%) refuse,
 *       {@link #RIVAL_GRASS_CHANCE} (10%) grass to Terry → Terry HOSTILE for 24h +
 *       {@link RumourType#TURF_WAR} rumour.</li>
 *   <li>Clean {@link #UNDERCUTTING_TERRY_THRESHOLD} houses without Terry spotting →
 *       {@link AchievementType#UNDERCUTTING_TERRY}.</li>
 *   <li>If Terry spots player within {@link #TERRY_SPOT_RADIUS} blocks: instant HOSTILE.</li>
 * </ul>
 *
 * <h3>Mechanic 5 — Nosy Neighbour Intel</h3>
 * <ul>
 *   <li>Player within {@link #GOSSIP_OVERHEAR_RADIUS} blocks of Terry's payment exchange
 *       (Notoriety &lt; {@link #GOSSIP_MAX_NOTORIETY}) overhears
 *       {@link RumourType#NEIGHBOURHOOD_GOSSIP} rumour added to {@link RumourNetwork}.</li>
 *   <li>Achievement {@link AchievementType#CURTAIN_TWITCHER} on
 *       {@link #CURTAIN_TWITCHER_THRESHOLD} overheard rumours.</li>
 * </ul>
 */
public class WindowCleanerSystem {

    // ── Round schedule ────────────────────────────────────────────────────────

    /** Hour Terry starts his round (08:30). */
    public static final float ROUND_START_HOUR = 8.5f;

    /** Hour Terry ends his round (16:00). */
    public static final float ROUND_END_HOUR = 16.0f;

    /** Number of properties Terry visits per day. */
    public static final int PROPERTIES_PER_DAY = 12;

    /** In-game seconds Terry spends at each property. */
    public static final float SECONDS_PER_PROPERTY = 90.0f;

    /** Payment Terry collects per house (COIN). */
    public static final int PAYMENT_PER_HOUSE = 2;

    /** Chance (0–1) a householder refuses to pay. */
    public static final float NON_PAYMENT_CHANCE = 0.20f;

    // ── Employment ────────────────────────────────────────────────────────────

    /** Maximum notoriety for the player to work Terry's round. */
    public static final int EMPLOYMENT_MAX_NOTORIETY = 40;

    /** Mini-game steps per house (BattleBarMiniGame rounds). */
    public static final int MINI_GAME_STEPS = 6;

    /** Mini-game score needed to pass a house (out of MINI_GAME_STEPS). */
    public static final int MINI_GAME_PASS_SCORE = 4;

    /** COIN earned per successfully cleaned house during employment. */
    public static final int HOUSE_WAGE = 3;

    /** Houses to complete in one shift to earn WINDOW_LAD achievement. */
    public static final int WINDOW_LAD_THRESHOLD = 8;

    /** Full days worked before gaining TRUSTED_WORKER status. */
    public static final int TRUSTED_WORKER_DAYS = 5;

    /** Daily COIN threshold above which HMRCSystem is notified (15% grass chance). */
    public static final int HMRC_DAILY_COIN_THRESHOLD = 30;

    // ── Ladder burglary ───────────────────────────────────────────────────────

    /** Notoriety added when player enters via LADDER_PROP (burglary). */
    public static final int LADDER_BURGLARY_NOTORIETY = 6;

    /** WantedSystem stars added on ladder burglary. */
    public static final int LADDER_BURGLARY_WANTED_STARS = 1;

    /** Notoriety added when player is seen on LADDER_PROP by a witness. */
    public static final int LADDER_SEEN_NOTORIETY = 3;

    // ── Rival round ───────────────────────────────────────────────────────────

    /** Corner shop price for BUCKET_AND_CHAMOIS (COIN). */
    public static final int BUCKET_CHAMOIS_PRICE = 8;

    /** Chance (0–1) a householder pays the rival cleaner. */
    public static final float RIVAL_PAY_CHANCE = 0.60f;

    /** Chance (0–1) a householder refuses the rival cleaner (after pay-chance fails). */
    public static final float RIVAL_REFUSE_CHANCE = 0.30f;

    /** Chance (0–1) a householder grasses to Terry (remainder after pay + refuse). */
    public static final float RIVAL_GRASS_CHANCE = 0.10f;

    /** COIN paid by householder for rival cleaning. */
    public static final int RIVAL_CLEAN_WAGE = 2;

    /** Houses cleaned without Terry spotting to earn UNDERCUTTING_TERRY. */
    public static final int UNDERCUTTING_TERRY_THRESHOLD = 5;

    /** Radius (blocks) within which Terry spots the rival cleaner. */
    public static final float TERRY_SPOT_RADIUS = 12.0f;

    // ── Nosy neighbour ────────────────────────────────────────────────────────

    /** Radius (blocks) within which player overhears payment exchange gossip. */
    public static final float GOSSIP_OVERHEAR_RADIUS = 4.0f;

    /** Maximum notoriety for player to overhear gossip. */
    public static final int GOSSIP_MAX_NOTORIETY = 25;

    /** Rumours overheard to earn CURTAIN_TWITCHER achievement. */
    public static final int CURTAIN_TWITCHER_THRESHOLD = 5;

    // ── Speech lines ──────────────────────────────────────────────────────────

    public static final String TERRY_GREETING        = "All right. You want to help on the round?";
    public static final String TERRY_NOTORIETY_HIGH  = "Nah, I can't have you near my customers. Not with your reputation.";
    public static final String TERRY_TRUSTED         = "You've done right by me. Here — have me spare kit.";
    public static final String TERRY_HOSTILE         = "You've been doing my round behind my back. We're done.";
    public static final String TERRY_DEBT_REPORT     = "Cheers for letting me know. Here's your cut.";

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Result of pressing E on Terry to request employment. */
    public enum EmploymentResult {
        /** Terry agrees; shift starts. */
        STARTED,
        /** Player's notoriety is too high. */
        NOTORIETY_TOO_HIGH,
        /** Terry is currently hostile. */
        TERRY_HOSTILE,
        /** Round is not currently active. */
        ROUND_NOT_ACTIVE,
        /** A shift is already in progress. */
        ALREADY_ON_SHIFT
    }

    /** Result of the BattleBar mini-game for one house. */
    public enum HouseCleanResult {
        /** Score >= MINI_GAME_PASS_SCORE: COIN awarded. */
        PASSED,
        /** Score < MINI_GAME_PASS_SCORE: no COIN. */
        FAILED,
        /** No active shift. */
        NO_SHIFT
    }

    /** Result of attempting a rival window clean on a residential window. */
    public enum RivalCleanResult {
        /** Householder pays RIVAL_CLEAN_WAGE. */
        PAID,
        /** Householder refuses. */
        REFUSED,
        /** Householder grasses to Terry; Terry becomes hostile; TURF_WAR rumour seeded. */
        GRASSED,
        /** Player doesn't have BUCKET_AND_CHAMOIS. */
        NO_KIT,
        /** Terry spotted the player (within TERRY_SPOT_RADIUS). */
        SPOTTED_BY_TERRY
    }

    /** Result of reporting a defaulter to Terry. */
    public enum DefaulterReportResult {
        /** Terry thanks player, awards 1 COIN, seeds NONPAYMENT_GOSSIP. */
        REPORTED,
        /** No active defaulter at current property position. */
        NO_DEFAULTER,
        /** Terry is hostile; won't listen. */
        TERRY_HOSTILE
    }

    /** Result of climbing the LADDER_PROP. */
    public enum LadderClimbResult {
        /** Ladder successfully climbed; LADDER_BURGLARY recorded. */
        CLIMBED,
        /** Witnessed by an NPC; Notoriety +LADDER_SEEN_NOTORIETY, police called. */
        WITNESSED,
        /** No ladder is currently placed. */
        NO_LADDER
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Whether today's round is active (Mon–Fri, 08:30–16:00). */
    private boolean roundActive = false;

    /** Current property index in the route (0 = first house, 11 = last). */
    private int currentPropertyIndex = 0;

    /** Seconds spent at the current property. */
    private float propertyTimer = 0f;

    /** Whether a LADDER_PROP is currently placed at the active property. */
    private boolean ladderPlaced = false;

    /** Whether the player is currently on an employment shift. */
    private boolean shiftActive = false;

    /** Number of houses cleaned this shift. */
    private int housesCleanedThisShift = 0;

    /** Total COIN earned today from employment. */
    private int dailyCoinEarned = 0;

    /** Full days the player has worked for Terry. */
    private int fullDaysWorked = 0;

    /** Whether the player has TRUSTED_WORKER status. */
    private boolean trustedWorker = false;

    /** Number of rival houses cleaned without Terry spotting. */
    private int rivalHousesCleaned = 0;

    /** Whether Terry is currently hostile toward the player. */
    private boolean terryHostile = false;

    /** Hours remaining on Terry's HOSTILE state (0 = not hostile). */
    private float terryHostileTimer = 0f;

    /** Number of payment-exchange gossip rumours overheard. */
    private int gossipOverheardCount = 0;

    /** List of property indices that have defaulted this round. */
    private final List<Integer> defaulterProperties = new ArrayList<>();

    /** Whether WINDOW_LAD achievement has been awarded this session. */
    private boolean windowLadAwarded = false;

    /** Whether UNDERCUTTING_TERRY has been awarded this session. */
    private boolean underCuttingTerryAwarded = false;

    /** Whether CURTAIN_TWITCHER has been awarded this session. */
    private boolean curtainTwitcherAwarded = false;

    /** Whether UP_THE_LADDER has been awarded this session. */
    private boolean upTheLadderAwarded = false;

    /** Whether BUCKET_AND_SPADE has been awarded (stolen kit + rival round completed). */
    private boolean bucketAndSpadeAwarded = false;

    /** Whether the bucket was stolen from Terry (for BUCKET_AND_SPADE achievement). */
    private boolean kitWasStolen = false;

    // ── Optional integrations ─────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private HMRCSystem hmrcSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public WindowCleanerSystem(Random random) {
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
     * Update the window cleaner system each frame.
     *
     * <p>Advances the property timer; when {@link #SECONDS_PER_PROPERTY} elapses, Terry
     * moves to the next property (ladder removed). Manages Terry's hostile timer.
     *
     * @param delta      seconds since last frame
     * @param hour       current in-game hour
     * @param dayOfWeek  0=Sunday, 1=Monday … 6=Saturday
     * @param playerX    player X position (for proximity checks)
     * @param playerY    player Y position
     * @param playerZ    player Z position
     * @param playerNotoriety  raw player notoriety score
     * @param nearbyNpc  nearest NPC witness (or null); used for gossip rumour seeding
     * @param cb         achievement callback (may be null)
     */
    public void update(float delta, float hour, int dayOfWeek,
                       float playerX, float playerY, float playerZ,
                       int playerNotoriety,
                       NPC nearbyNpc, AchievementCallback cb) {

        // Manage hostile timer
        if (terryHostile && terryHostileTimer > 0f) {
            terryHostileTimer -= delta;
            if (terryHostileTimer <= 0f) {
                terryHostileTimer = 0f;
                terryHostile = false;
            }
        }

        // Determine if round should be active (Mon–Fri, 08:30–16:00)
        boolean shouldBeActive = isWeekday(dayOfWeek) && hour >= ROUND_START_HOUR && hour < ROUND_END_HOUR;
        if (!shouldBeActive) {
            if (roundActive) {
                endRound(cb);
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
            ladderPlaced = false;
            currentPropertyIndex++;
            if (currentPropertyIndex >= PROPERTIES_PER_DAY) {
                endRound(cb);
                return;
            }
            // Place ladder at new property and resolve payment
            placeLadderAtCurrentProperty();
            resolvePayment();
        }

        // Check gossip overhear (near payment exchange — first 10 seconds at property)
        if (propertyTimer < 10f && playerNotoriety < GOSSIP_MAX_NOTORIETY) {
            float dx = playerX;
            float dy = playerY;
            float dz = playerZ;
            // Distance from property (simplified: use propertyIndex as a proxy offset)
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist <= GOSSIP_OVERHEAR_RADIUS && nearbyNpc != null) {
                overhearGossip(nearbyNpc, cb);
            }
        }
    }

    private boolean isWeekday(int dayOfWeek) {
        return dayOfWeek >= 1 && dayOfWeek <= 5; // Mon=1 … Fri=5
    }

    private void startRound() {
        roundActive = true;
        currentPropertyIndex = 0;
        propertyTimer = 0f;
        defaulterProperties.clear();
        placeLadderAtCurrentProperty();
        resolvePayment();
    }

    private void endRound(AchievementCallback cb) {
        roundActive = false;
        ladderPlaced = false;
        if (shiftActive) {
            finaliseShift(cb);
        }
    }

    private void placeLadderAtCurrentProperty() {
        ladderPlaced = currentPropertyIndex < PROPERTIES_PER_DAY;
    }

    private void resolvePayment() {
        if (random.nextFloat() < NON_PAYMENT_CHANCE) {
            defaulterProperties.add(currentPropertyIndex);
        }
    }

    // ── Employment (Mechanic 2) ────────────────────────────────────────────────

    /**
     * Player presses E on Terry to start a cash-in-hand shift.
     *
     * @param playerNotoriety raw player notoriety score
     */
    public EmploymentResult startShift(int playerNotoriety) {
        if (!roundActive) return EmploymentResult.ROUND_NOT_ACTIVE;
        if (terryHostile) return EmploymentResult.TERRY_HOSTILE;
        if (playerNotoriety >= EMPLOYMENT_MAX_NOTORIETY) return EmploymentResult.NOTORIETY_TOO_HIGH;
        if (shiftActive) return EmploymentResult.ALREADY_ON_SHIFT;

        shiftActive = true;
        housesCleanedThisShift = 0;
        dailyCoinEarned = 0;
        return EmploymentResult.STARTED;
    }

    /**
     * Resolve one house during the player's employment shift using BattleBarMiniGame.
     *
     * <p>Simulates {@link #MINI_GAME_STEPS} presses with EASY difficulty. Each press
     * that lands in the hit zone counts as a hit. Score &ge; {@link #MINI_GAME_PASS_SCORE}
     * awards {@link #HOUSE_WAGE} COIN.
     *
     * @param inventory the player's inventory (COIN added on pass)
     * @param cb        achievement callback
     * @return result of cleaning this house
     */
    public HouseCleanResult cleanHouse(Inventory inventory, AchievementCallback cb) {
        if (!shiftActive) return HouseCleanResult.NO_SHIFT;

        BattleBarMiniGame bar = BattleBarMiniGame.easy(random);
        int score = 0;
        for (int i = 0; i < MINI_GAME_STEPS; i++) {
            // Simulate cursor moving to the centre of the bar then pressing
            bar.update(BattleBarMiniGame.ROUND_TIMEOUT_SECONDS * 0.3f);
            if (bar.press()) {
                score++;
            }
            // Reset for next step by creating a new bar (each step is independent)
            if (i < MINI_GAME_STEPS - 1) {
                bar = BattleBarMiniGame.easy(random);
            }
        }

        if (score >= MINI_GAME_PASS_SCORE) {
            inventory.addItem(Material.COIN, HOUSE_WAGE);
            dailyCoinEarned += HOUSE_WAGE;
            housesCleanedThisShift++;
            checkWindowLad(cb);
            return HouseCleanResult.PASSED;
        }

        housesCleanedThisShift++;
        return HouseCleanResult.FAILED;
    }

    private void checkWindowLad(AchievementCallback cb) {
        if (!windowLadAwarded && housesCleanedThisShift >= WINDOW_LAD_THRESHOLD && cb != null) {
            windowLadAwarded = true;
            cb.award(AchievementType.WINDOW_LAD);
        }
    }

    private void finaliseShift(AchievementCallback cb) {
        shiftActive = false;

        // HMRC notification
        if (hmrcSystem != null && dailyCoinEarned > HMRC_DAILY_COIN_THRESHOLD) {
            hmrcSystem.onUntaxedEarning(dailyCoinEarned, 0);
        }

        // Trusted worker progression
        if (housesCleanedThisShift >= WINDOW_LAD_THRESHOLD) {
            fullDaysWorked++;
            if (!trustedWorker && fullDaysWorked >= TRUSTED_WORKER_DAYS) {
                trustedWorker = true;
            }
        }
    }

    /**
     * Terry lends BUCKET_AND_CHAMOIS to a trusted worker.
     *
     * @param inventory player's inventory
     * @return true if kit was awarded (player is trusted and doesn't already have it)
     */
    public boolean receiveTrustedWorkerKit(Inventory inventory) {
        if (trustedWorker && !inventory.hasItem(Material.BUCKET_AND_CHAMOIS)) {
            inventory.addItem(Material.BUCKET_AND_CHAMOIS, 1);
            return true;
        }
        return false;
    }

    // ── Ladder burglary (Mechanic 3) ──────────────────────────────────────────

    /**
     * Player attempts to climb the current LADDER_PROP.
     *
     * @param witnessNpc  NPC that may witness the climb (null = no witness)
     * @param cb          achievement callback
     */
    public LadderClimbResult climbLadder(NPC witnessNpc, AchievementCallback cb) {
        if (!ladderPlaced) return LadderClimbResult.NO_LADDER;

        if (witnessNpc != null) {
            // Seen on ladder
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(LADDER_SEEN_NOTORIETY, cb);
            }
            if (rumourNetwork != null) {
                rumourNetwork.addRumour(witnessNpc,
                        new Rumour(RumourType.LADDER_INCIDENT,
                                "Someone was up Terry's ladder when he wasn't looking. Bold as brass."));
            }
            return LadderClimbResult.WITNESSED;
        }

        // Unwitnessed burglary
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.LADDER_BURGLARY);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(LADDER_BURGLARY_NOTORIETY, cb);
        }
        if (wantedSystem != null) {
            wantedSystem.increaseWantedStars(LADDER_BURGLARY_WANTED_STARS);
        }
        if (!upTheLadderAwarded && cb != null) {
            upTheLadderAwarded = true;
            cb.award(AchievementType.UP_THE_LADDER);
        }
        return LadderClimbResult.CLIMBED;
    }

    // ── Rival round (Mechanic 4) ──────────────────────────────────────────────

    /**
     * Player offers window cleaning at a residential window.
     *
     * @param inventory       player's inventory (BUCKET_AND_CHAMOIS required)
     * @param terryNpc        Terry's NPC (for proximity check; null if not in range)
     * @param terryX          Terry's X position (for distance check)
     * @param terryZ          Terry's Z position
     * @param playerX         player X position
     * @param playerZ         player Z position
     * @param witnessNpc      NPC that may seed TURF_WAR rumour (null = none)
     * @param cb              achievement callback
     */
    public RivalCleanResult doRivalClean(Inventory inventory,
                                         NPC terryNpc,
                                         float terryX, float terryZ,
                                         float playerX, float playerZ,
                                         NPC witnessNpc, AchievementCallback cb) {
        if (!inventory.hasItem(Material.BUCKET_AND_CHAMOIS)) {
            return RivalCleanResult.NO_KIT;
        }

        // Check if Terry spots the player
        if (terryNpc != null && !terryHostile) {
            float dx = terryX - playerX;
            float dz = terryZ - playerZ;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist <= TERRY_SPOT_RADIUS) {
                makeTerryHostile();
                return RivalCleanResult.SPOTTED_BY_TERRY;
            }
        }

        float roll = random.nextFloat();
        if (roll < RIVAL_PAY_CHANCE) {
            inventory.addItem(Material.COIN, RIVAL_CLEAN_WAGE);
            rivalHousesCleaned++;

            // Check BUCKET_AND_SPADE (stolen kit + rival completion)
            if (!bucketAndSpadeAwarded && kitWasStolen && cb != null) {
                bucketAndSpadeAwarded = true;
                cb.award(AchievementType.BUCKET_AND_SPADE);
            }

            // Check UNDERCUTTING_TERRY
            if (!underCuttingTerryAwarded && rivalHousesCleaned >= UNDERCUTTING_TERRY_THRESHOLD && cb != null) {
                underCuttingTerryAwarded = true;
                cb.award(AchievementType.UNDERCUTTING_TERRY);
            }

            return RivalCleanResult.PAID;
        } else if (roll < RIVAL_PAY_CHANCE + RIVAL_REFUSE_CHANCE) {
            return RivalCleanResult.REFUSED;
        } else {
            // Grassed to Terry
            makeTerryHostile();
            if (rumourNetwork != null && witnessNpc != null) {
                rumourNetwork.addRumour(witnessNpc,
                        new Rumour(RumourType.TURF_WAR,
                                "Terry's gone mad. Some lad's been doing his round on the sly."));
            }
            return RivalCleanResult.GRASSED;
        }
    }

    /** Mark Terry's stolen kit so BUCKET_AND_SPADE can be tracked. */
    public void markKitStolen() {
        kitWasStolen = true;
    }

    private void makeTerryHostile() {
        terryHostile = true;
        terryHostileTimer = 24f * 3600f / 60f; // 24 in-game hours in seconds (approx)
    }

    // ── Nosy neighbour (Mechanic 5) ───────────────────────────────────────────

    /**
     * Player overhears a payment-exchange gossip rumour near Terry.
     *
     * @param nearestNpc NPC to seed the rumour from
     * @param cb         achievement callback
     */
    private void overhearGossip(NPC nearestNpc, AchievementCallback cb) {
        if (rumourNetwork == null) return;
        rumourNetwork.addRumour(nearestNpc,
                new Rumour(RumourType.NEIGHBOURHOOD_GOSSIP,
                        "Terry was chatting to Dot about number twelve — apparently they've been at it again."));
        gossipOverheardCount++;

        if (!curtainTwitcherAwarded && gossipOverheardCount >= CURTAIN_TWITCHER_THRESHOLD && cb != null) {
            curtainTwitcherAwarded = true;
            cb.award(AchievementType.CURTAIN_TWITCHER);
        }
    }

    // ── Defaulter report (Mechanic 1) ─────────────────────────────────────────

    /**
     * Player reports a defaulter to Terry.
     *
     * @param inventory      player's inventory (1 COIN reward added)
     * @param propertyIndex  index of the property to report
     * @param witnessNpc     NPC to seed NONPAYMENT_GOSSIP (null = none)
     */
    public DefaulterReportResult reportDefaulter(Inventory inventory, int propertyIndex, NPC witnessNpc) {
        if (terryHostile) return DefaulterReportResult.TERRY_HOSTILE;
        if (!defaulterProperties.contains(propertyIndex)) return DefaulterReportResult.NO_DEFAULTER;

        inventory.addItem(Material.COIN, 1);
        defaulterProperties.remove(Integer.valueOf(propertyIndex));

        if (rumourNetwork != null && witnessNpc != null) {
            rumourNetwork.addRumour(witnessNpc,
                    new Rumour(RumourType.NONPAYMENT_GOSSIP,
                            "Someone on this street hasn't paid Terry in three weeks. He's proper fuming."));
        }
        return DefaulterReportResult.REPORTED;
    }

    // ── Route generation ──────────────────────────────────────────────────────

    /**
     * Generate a deterministic daily route of {@link #PROPERTIES_PER_DAY} property IDs.
     *
     * <p>Property IDs are sequential integers 0–11 shuffled with the seeded RNG. The
     * route is stable for the same seed (used in tests to verify determinism).
     *
     * @return list of {@link #PROPERTIES_PER_DAY} property indices in visit order
     */
    public List<Integer> generateDailyRoute() {
        List<Integer> route = new ArrayList<>();
        for (int i = 0; i < PROPERTIES_PER_DAY; i++) {
            route.add(i);
        }
        // Fisher-Yates shuffle with seeded random
        for (int i = PROPERTIES_PER_DAY - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = route.get(i);
            route.set(i, route.get(j));
            route.set(j, tmp);
        }
        return route;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Returns whether the daily round is currently active. */
    public boolean isRoundActive() { return roundActive; }

    /** Returns current property index (0-based). */
    public int getCurrentPropertyIndex() { return currentPropertyIndex; }

    /** Returns seconds elapsed at current property. */
    public float getPropertyTimer() { return propertyTimer; }

    /** Returns whether LADDER_PROP is placed at the current property. */
    public boolean isLadderPlaced() { return ladderPlaced; }

    /** Returns whether an employment shift is active. */
    public boolean isShiftActive() { return shiftActive; }

    /** Returns houses cleaned this shift. */
    public int getHousesCleanedThisShift() { return housesCleanedThisShift; }

    /** Returns daily COIN earned from employment. */
    public int getDailyCoinEarned() { return dailyCoinEarned; }

    /** Returns full days worked for Terry. */
    public int getFullDaysWorked() { return fullDaysWorked; }

    /** Returns whether the player has TRUSTED_WORKER status. */
    public boolean isTrustedWorker() { return trustedWorker; }

    /** Returns whether Terry is currently hostile. */
    public boolean isTerryHostile() { return terryHostile; }

    /** Returns rival houses cleaned without being spotted. */
    public int getRivalHousesCleaned() { return rivalHousesCleaned; }

    /** Returns gossip rumours overheard. */
    public int getGossipOverheardCount() { return gossipOverheardCount; }

    /** Returns the list of defaulter property indices. */
    public List<Integer> getDefaulterProperties() { return defaulterProperties; }

    // ── Test helpers ──────────────────────────────────────────────────────────

    /** Force-set round active state for testing. */
    public void setRoundActiveForTesting(boolean active) { this.roundActive = active; }

    /** Force-set Terry hostile state for testing. */
    public void setTerryHostileForTesting(boolean hostile) {
        this.terryHostile = hostile;
        this.terryHostileTimer = hostile ? 86400f : 0f;
    }

    /** Force-set trusted worker status for testing. */
    public void setTrustedWorkerForTesting(boolean trusted) { this.trustedWorker = trusted; }

    /** Force-set full days worked for testing. */
    public void setFullDaysWorkedForTesting(int days) { this.fullDaysWorked = days; }

    /** Force-set daily coin earned for testing. */
    public void setDailyCoinEarnedForTesting(int coin) { this.dailyCoinEarned = coin; }

    /** Force-set shift active for testing. */
    public void setShiftActiveForTesting(boolean active) { this.shiftActive = active; }

    /** Force-set houses cleaned for testing. */
    public void setHousesCleanedForTesting(int count) { this.housesCleanedThisShift = count; }

    /** Force-set rival houses cleaned for testing. */
    public void setRivalHousesCleanedForTesting(int count) { this.rivalHousesCleaned = count; }

    /** Force-set gossip overheard for testing. */
    public void setGossipOverheardForTesting(int count) { this.gossipOverheardCount = count; }

    /** Force-set ladder placed for testing. */
    public void setLadderPlacedForTesting(boolean placed) { this.ladderPlaced = placed; }

    /** Force-set kit stolen flag for testing. */
    public void setKitStolenForTesting(boolean stolen) { this.kitWasStolen = stolen; }

    /** Add a defaulter property index directly for testing. */
    public void addDefaulterForTesting(int propertyIndex) {
        if (!defaulterProperties.contains(propertyIndex)) {
            defaulterProperties.add(propertyIndex);
        }
    }
}

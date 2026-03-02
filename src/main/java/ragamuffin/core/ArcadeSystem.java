package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1309: Northfield Ace Amusements Arcade — Tokens, Penny Falls, Claw Machine
 * &amp; Machine Tampering.
 *
 * <h3>Token Economy</h3>
 * <ul>
 *   <li>Kevin ({@link NPCType#ARCADE_ATTENDANT}) exchanges 1 {@link Material#COIN} →
 *       1 {@link Material#ARCADE_TOKEN}.</li>
 *   <li>Banned players are refused ("You're barred, mate.").</li>
 *   <li>Surplus tokens can be fenced at 2 {@link Material#COIN} each.</li>
 * </ul>
 *
 * <h3>Penny Falls Mini-Game</h3>
 * <ul>
 *   <li>Costs 1 {@link Material#ARCADE_TOKEN} per push (press E on
 *       {@link ragamuffin.world.PropType#PENNY_FALLS_PROP}).</li>
 *   <li>Timing bar: green zone (20%) wins 3 tokens, yellow zone (40%) wins 1,
 *       red zone returns nothing.</li>
 *   <li>Jackpot: threshold set per machine; hitting it awards {@link AchievementType#PENNY_KING}
 *       and seeds a {@link RumourType#LOCAL_EVENT} rumour.</li>
 * </ul>
 *
 * <h3>Claw Machine Mini-Game</h3>
 * <ul>
 *   <li>Costs 1 {@link Material#ARCADE_TOKEN}. Player uses WASD + E to position and drop
 *       the claw over a hidden prize at a seeded column.</li>
 *   <li>15% base success chance yields {@link Material#STUFFED_ANIMAL}.</li>
 *   <li>40% early-drop chance for authentic disappointment.</li>
 *   <li>First-attempt win awards {@link AchievementType#CLAW_MASTER}.</li>
 * </ul>
 *
 * <h3>Arcade Shooter Mini-Game</h3>
 * <ul>
 *   <li>Costs 1 {@link Material#ARCADE_TOKEN}. Reaction-time game.</li>
 *   <li>10 kills without 3 misses wins 5 {@link Material#PRIZE_TICKET} and seeds a
 *       {@link RumourType#SHOP_NEWS} rumour. Awards {@link AchievementType#ARCADE_LEGEND}.</li>
 * </ul>
 *
 * <h3>Redemption Counter</h3>
 * <ul>
 *   <li>5 {@link Material#PRIZE_TICKET} → {@link Material#PENNY_SWEETS} ×3</li>
 *   <li>15 tickets → {@link Material#STUFFED_ANIMAL}</li>
 *   <li>40 tickets → {@link Material#PLASTIC_TROPHY}</li>
 *   <li>100 tickets → {@link Material#ARCADE_CHAMPION_BADGE} (grants tamper immunity).</li>
 *   <li>Awards {@link AchievementType#REDEMPTION_ARC} on first ARCADE_CHAMPION_BADGE claim.</li>
 * </ul>
 *
 * <h3>Machine Tampering</h3>
 * <ul>
 *   <li>Hold E for {@link #TAMPER_HOLD_SECONDS} seconds while Kevin is &gt;
 *       {@link #KEVIN_DETECTION_RADIUS} blocks away.</li>
 *   <li>Tamper actions: lower penny-falls threshold, free play, extract 2 COIN.</li>
 *   <li>Kevin catching the player: 30-minute ban, {@link CrimeType#CRIMINAL_DAMAGE},
 *       +{@link #TAMPER_CAUGHT_NOTORIETY} Notoriety.</li>
 *   <li>Wearing {@link Material#BUILDER_OVERALLS} halves Kevin's detection radius to
 *       {@link #KEVIN_DETECTION_RADIUS_DISGUISED}.</li>
 *   <li>Possessing {@link Material#ARCADE_CHAMPION_BADGE} grants full tamper immunity.</li>
 *   <li>Awards {@link AchievementType#DODGY_ENGINEER} on first successful tamper.</li>
 * </ul>
 *
 * <h3>ARCADE_KID Distraction</h3>
 * <ul>
 *   <li>Each {@link NPCType#ARCADE_KID} present has a {@link #KID_DISTRACT_CHANCE_PER_MIN}
 *       chance per in-game minute of distracting Kevin for {@link #KID_DISTRACT_DURATION_SECONDS}
 *       seconds.</li>
 * </ul>
 */
public class ArcadeSystem {

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Hour the arcade opens (10:00). */
    public static final float OPEN_HOUR = 10.0f;

    /** Hour the arcade closes (22:00). */
    public static final float CLOSE_HOUR = 22.0f;

    // ── Token economy ─────────────────────────────────────────────────────────

    /** Coins required to buy 1 ARCADE_TOKEN from Kevin. */
    public static final int TOKEN_COIN_COST = 1;

    /** Fence resale value per surplus ARCADE_TOKEN (COIN). */
    public static final int TOKEN_FENCE_VALUE = 2;

    // ── Penny Falls ───────────────────────────────────────────────────────────

    /** Token cost per penny-falls push. */
    public static final int PENNY_FALLS_COST = 1;

    /** Green zone fraction of timing bar (0–1). Pays {@link #PENNY_FALLS_GREEN_WIN}. */
    public static final float PENNY_FALLS_GREEN_ZONE = 0.20f;

    /** Yellow zone fraction of timing bar, cumulative (0–1). Pays {@link #PENNY_FALLS_YELLOW_WIN}. */
    public static final float PENNY_FALLS_YELLOW_ZONE = 0.60f; // green+yellow combined upper edge

    /** Tokens won in the green zone. */
    public static final int PENNY_FALLS_GREEN_WIN = 3;

    /** Tokens won in the yellow zone. */
    public static final int PENNY_FALLS_YELLOW_WIN = 1;

    /** Minimum jackpot threshold (pushes). */
    public static final int PENNY_FALLS_JACKPOT_THRESHOLD_MIN = 50;

    /** Maximum jackpot threshold (pushes). */
    public static final int PENNY_FALLS_JACKPOT_THRESHOLD_MAX = 200;

    /** Tokens awarded on jackpot. */
    public static final int PENNY_FALLS_JACKPOT_WIN = 10;

    // ── Claw Machine ──────────────────────────────────────────────────────────

    /** Token cost per claw machine attempt. */
    public static final int CLAW_MACHINE_COST = 1;

    /** Base probability (0–1) of a successful claw grab. */
    public static final float CLAW_SUCCESS_CHANCE = 0.15f;

    /** Probability (0–1) of an early drop (authentic disappointment). */
    public static final float CLAW_EARLY_DROP_CHANCE = 0.40f;

    // ── Arcade Shooter ────────────────────────────────────────────────────────

    /** Token cost per arcade shooter game. */
    public static final int SHOOTER_COST = 1;

    /** Kills needed to win the arcade shooter. */
    public static final int SHOOTER_KILLS_TO_WIN = 10;

    /** Maximum misses before the game ends without winning. */
    public static final int SHOOTER_MAX_MISSES = 3;

    /** Prize tickets awarded on winning the shooter. */
    public static final int SHOOTER_TICKET_REWARD = 5;

    // ── Redemption counter ────────────────────────────────────────────────────

    /** Tickets needed for PENNY_SWEETS ×3. */
    public static final int REDEEM_PENNY_SWEETS_COST = 5;

    /** PENNY_SWEETS quantity per redemption. */
    public static final int REDEEM_PENNY_SWEETS_QTY = 3;

    /** Tickets needed for STUFFED_ANIMAL. */
    public static final int REDEEM_STUFFED_ANIMAL_COST = 15;

    /** Tickets needed for PLASTIC_TROPHY. */
    public static final int REDEEM_PLASTIC_TROPHY_COST = 40;

    /** Tickets needed for ARCADE_CHAMPION_BADGE. */
    public static final int REDEEM_CHAMPION_BADGE_COST = 100;

    // ── Machine tampering ─────────────────────────────────────────────────────

    /** Seconds the player must hold E to initiate a tamper. */
    public static final float TAMPER_HOLD_SECONDS = 3.0f;

    /** Kevin's detection radius in blocks (normal). */
    public static final float KEVIN_DETECTION_RADIUS = 6.0f;

    /** Kevin's detection radius in blocks when player wears BUILDER_OVERALLS. */
    public static final float KEVIN_DETECTION_RADIUS_DISGUISED = 3.0f;

    /** Notoriety penalty when Kevin catches the player tampering. */
    public static final int TAMPER_CAUGHT_NOTORIETY = 3;

    /** Duration of the ban imposed by Kevin (in in-game minutes). */
    public static final float BAN_DURATION_MINUTES = 30.0f;

    /** Coin extracted per coin-box tamper on a penny-falls machine. */
    public static final int TAMPER_COIN_EXTRACT = 2;

    /** Noise level spiked on a tamper action (2–3). */
    public static final int TAMPER_NOISE_MIN = 2;
    public static final int TAMPER_NOISE_MAX = 3;

    /** Amount by which penny-falls jackpot threshold is lowered on tamper. */
    public static final int TAMPER_THRESHOLD_REDUCTION = 10;

    // ── ARCADE_KID distraction ────────────────────────────────────────────────

    /** Chance (0–1) per in-game minute that each ARCADE_KID distracts Kevin. */
    public static final float KID_DISTRACT_CHANCE_PER_MIN = 0.15f;

    /** Duration (seconds) Kevin is distracted per ARCADE_KID event. */
    public static final float KID_DISTRACT_DURATION_SECONDS = 30.0f;

    // ── Speech lines ──────────────────────────────────────────────────────────

    public static final String KEVIN_GREETING       = "You wanna exchange them tokens?";
    public static final String KEVIN_BARRED         = "You're barred, mate.";
    public static final String KEVIN_WATCHING       = "I'm watching you.";
    public static final String KEVIN_TILT_WARNING   = "Oi! Don't shake the machines!";
    public static final String KEVIN_CAUGHT_TAMPER  = "You breakin' that machine? You're out. Come back when you've grown up.";
    public static final String KEVIN_CLOSED         = "We're shut. Come back at ten.";
    public static final String CHANGE_NO_COIN       = "No coins, mate.";

    // ── Inner enums ───────────────────────────────────────────────────────────

    /** Result of attempting to buy a token from Kevin. */
    public enum BuyTokenResult {
        /** Token purchased successfully. */
        SUCCESS,
        /** Player is banned from the arcade. */
        BANNED,
        /** Player has no COIN. */
        NO_COIN,
        /** Arcade is closed. */
        CLOSED
    }

    /** Result of playing the penny falls machine. */
    public enum PennyFallsResult {
        /** Timing bar landed in green zone — won {@link ArcadeSystem#PENNY_FALLS_GREEN_WIN} tokens. */
        GREEN_WIN,
        /** Timing bar landed in yellow zone — won {@link ArcadeSystem#PENNY_FALLS_YELLOW_WIN} token. */
        YELLOW_WIN,
        /** Timing bar landed in red zone — no win. */
        RED_LOSS,
        /** Jackpot! Hit the threshold — won {@link ArcadeSystem#PENNY_FALLS_JACKPOT_WIN} tokens. */
        JACKPOT,
        /** Player has no ARCADE_TOKEN. */
        NO_TOKEN,
        /** Player is ejected from the arcade. */
        EJECTED
    }

    /** Result of a claw machine attempt. */
    public enum ClawResult {
        /** Successful grab — STUFFED_ANIMAL added to inventory. */
        WIN,
        /** Early drop — machine drops before reaching prize. */
        EARLY_DROP,
        /** Claw missed. */
        MISS,
        /** No token available. */
        NO_TOKEN,
        /** Player is ejected. */
        EJECTED
    }

    /** Result of an arcade shooter game. */
    public enum ShooterResult {
        /** Scored 10 kills without 3 misses — won tickets. */
        WIN,
        /** Game over — too many misses. */
        GAME_OVER,
        /** No token available. */
        NO_TOKEN,
        /** Player is ejected. */
        EJECTED
    }

    /** Result of a redemption counter exchange. */
    public enum RedemptionResult {
        /** Exchange successful. */
        SUCCESS,
        /** Not enough tickets. */
        NOT_ENOUGH_TICKETS,
        /** Unknown reward tier. */
        INVALID_TIER
    }

    /** Tamper action type on a machine. */
    public enum TamperAction {
        /** Lower penny-falls jackpot threshold by {@link ArcadeSystem#TAMPER_THRESHOLD_REDUCTION}. */
        LOWER_THRESHOLD,
        /** Unlock a free play on this machine. */
        FREE_PLAY,
        /** Extract {@link ArcadeSystem#TAMPER_COIN_EXTRACT} COIN from coin box. */
        EXTRACT_COIN
    }

    /** Result of attempting to tamper with a machine. */
    public enum TamperResult {
        /** Tamper succeeded. */
        SUCCESS,
        /** Kevin is too close — blocked. */
        KEVIN_TOO_CLOSE,
        /** Player does not have a SCREWDRIVER. */
        NO_SCREWDRIVER,
        /** Player has ARCADE_CHAMPION_BADGE — immunity active (no need to sneak). */
        IMMUNE,
        /** Player is ejected. */
        EJECTED
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Whether the player is currently banned from the arcade. */
    private boolean playerBanned = false;

    /** Time remaining on the current ban (in seconds). */
    private float banTimeRemaining = 0f;

    /** Per-machine jackpot threshold (set via forceJackpotThreshold for testing). */
    private int jackpotThreshold;

    /** Push count on the penny-falls machine. */
    private int pennyFallsPushes = 0;

    /** Number of machine tilts this visit. */
    private int tiltCountThisVisit = 0;

    /** Whether Kevin is currently distracted by an ARCADE_KID. */
    private boolean kevinDistracted = false;

    /** Time remaining on Kevin's distraction (seconds). */
    private float kevinDistractRemaining = 0f;

    /** Whether the jackpot rumour has already been seeded this session. */
    private boolean jackpotRumourSeeded = false;

    /** Whether the player has won the claw machine on their very first attempt ever. */
    private boolean clawFirstAttempt = true;

    /** Whether the DODGY_ENGINEER achievement has been awarded. */
    private boolean dodgyEngineerAwarded = false;

    /** Whether the REDEMPTION_ARC achievement has been awarded. */
    private boolean redemptionArcAwarded = false;

    /** Remaining tamper hold time progress (seconds). Resets if E released before threshold. */
    private float tamperHoldProgress = 0f;

    /** Kevin NPC reference for distance checks (may be null). */
    private NPC kevin = null;

    // ── Optional system references ────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private NoiseSystem noiseSystem;
    private DisguiseSystem disguiseSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public ArcadeSystem(Random random) {
        this.random = random;
        this.jackpotThreshold = PENNY_FALLS_JACKPOT_THRESHOLD_MIN
                + random.nextInt(PENNY_FALLS_JACKPOT_THRESHOLD_MAX - PENNY_FALLS_JACKPOT_THRESHOLD_MIN + 1);
    }

    // ── Dependency injection ──────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setNoiseSystem(NoiseSystem noiseSystem) {
        this.noiseSystem = noiseSystem;
    }

    public void setDisguiseSystem(DisguiseSystem disguiseSystem) {
        this.disguiseSystem = disguiseSystem;
    }

    // ── NPC management ────────────────────────────────────────────────────────

    /**
     * Register Kevin (the ARCADE_ATTENDANT NPC). Used for distance checks.
     *
     * @param kevinNpc the Kevin NPC instance
     */
    public void setKevin(NPC kevinNpc) {
        this.kevin = kevinNpc;
    }

    /** Returns Kevin NPC reference (may be null). */
    public NPC getKevin() {
        return kevin;
    }

    /** Spawn Kevin for testing. */
    public void forceSpawnKevin(float x, float y, float z) {
        kevin = new NPC(NPCType.ARCADE_ATTENDANT, "Kevin", x, y, z);
    }

    // ── Opening hours ─────────────────────────────────────────────────────────

    /**
     * Returns true if the arcade is open at the given hour.
     *
     * @param hour current in-game hour (0–24)
     */
    public boolean isOpen(float hour) {
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    // ── Token economy ─────────────────────────────────────────────────────────

    /**
     * Player presses E on Kevin to buy an ARCADE_TOKEN.
     *
     * @param inventory player's inventory
     * @param hour      current in-game hour
     * @return the result of the purchase attempt
     */
    public BuyTokenResult buyToken(Inventory inventory, float hour) {
        if (!isOpen(hour)) return BuyTokenResult.CLOSED;
        if (playerBanned)  return BuyTokenResult.BANNED;
        if (inventory.getItemCount(Material.COIN) < TOKEN_COIN_COST) return BuyTokenResult.NO_COIN;

        inventory.removeItem(Material.COIN, TOKEN_COIN_COST);
        inventory.addItem(Material.ARCADE_TOKEN, 1);
        return BuyTokenResult.SUCCESS;
    }

    // ── Penny Falls ───────────────────────────────────────────────────────────

    /**
     * Player pushes the penny-falls machine (E press).
     *
     * <p>The {@code timingPosition} parameter represents where the player stopped the
     * timing bar (0.0 = start, 1.0 = end of bar):
     * <ul>
     *   <li>0.0–{@link #PENNY_FALLS_GREEN_ZONE}: green zone (wins 3 tokens).</li>
     *   <li>{@link #PENNY_FALLS_GREEN_ZONE}–{@link #PENNY_FALLS_YELLOW_ZONE}: yellow zone
     *       (wins 1 token).</li>
     *   <li>{@link #PENNY_FALLS_YELLOW_ZONE}–1.0: red zone (no win).</li>
     * </ul>
     *
     * @param inventory      player inventory
     * @param timingPosition timing bar stop position in [0, 1]
     * @param npcs           all NPCs (for rumour seeding)
     * @return result of the push
     */
    public PennyFallsResult pushPennyFalls(Inventory inventory, float timingPosition, List<NPC> npcs) {
        if (playerBanned) return PennyFallsResult.EJECTED;
        if (inventory.getItemCount(Material.ARCADE_TOKEN) < PENNY_FALLS_COST) {
            return PennyFallsResult.NO_TOKEN;
        }

        inventory.removeItem(Material.ARCADE_TOKEN, PENNY_FALLS_COST);
        pennyFallsPushes++;

        // Check jackpot first
        if (pennyFallsPushes >= jackpotThreshold) {
            inventory.addItem(Material.ARCADE_TOKEN, PENNY_FALLS_JACKPOT_WIN);
            pennyFallsPushes = 0; // reset after jackpot
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.PENNY_KING);
            }
            seedJackpotRumour(npcs);
            return PennyFallsResult.JACKPOT;
        }

        // Evaluate timing bar position
        if (timingPosition < PENNY_FALLS_GREEN_ZONE) {
            inventory.addItem(Material.ARCADE_TOKEN, PENNY_FALLS_GREEN_WIN);
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.PENNY_CASCADE);
            }
            return PennyFallsResult.GREEN_WIN;
        } else if (timingPosition < PENNY_FALLS_YELLOW_ZONE) {
            inventory.addItem(Material.ARCADE_TOKEN, PENNY_FALLS_YELLOW_WIN);
            return PennyFallsResult.YELLOW_WIN;
        } else {
            return PennyFallsResult.RED_LOSS;
        }
    }

    private void seedJackpotRumour(List<NPC> npcs) {
        if (jackpotRumourSeeded || rumourNetwork == null || npcs == null || npcs.isEmpty()) return;
        jackpotRumourSeeded = true;
        NPC target = findAnyNPC(npcs);
        if (target != null) {
            rumourNetwork.addRumour(target, new Rumour(RumourType.LOCAL_EVENT,
                    "Someone won the penny falls at Ace Amusements. Didn't believe it meself."));
        }
    }

    // ── Claw Machine ──────────────────────────────────────────────────────────

    /**
     * Player attempts the claw machine (WASD + E).
     *
     * <p>The {@code playerColumn} and {@code prizeColumn} represent horizontal positions
     * on the claw machine grid. Success if {@code |playerColumn - prizeColumn| <= 1}
     * AND random roll &lt; {@link #CLAW_SUCCESS_CHANCE}.
     *
     * <p>If not within range: 40% chance of early drop (disappointment), else clean miss.
     *
     * @param inventory    player inventory
     * @param playerColumn claw column chosen by the player (0–9)
     * @param prizeColumn  hidden prize column (0–9)
     * @return result of the claw attempt
     */
    public ClawResult attemptClaw(Inventory inventory, int playerColumn, int prizeColumn) {
        if (playerBanned) return ClawResult.EJECTED;
        if (inventory.getItemCount(Material.ARCADE_TOKEN) < CLAW_MACHINE_COST) {
            return ClawResult.NO_TOKEN;
        }

        inventory.removeItem(Material.ARCADE_TOKEN, CLAW_MACHINE_COST);
        boolean firstAttempt = clawFirstAttempt;
        clawFirstAttempt = false;

        boolean inRange = Math.abs(playerColumn - prizeColumn) <= 1;
        if (inRange && random.nextFloat() < CLAW_SUCCESS_CHANCE) {
            inventory.addItem(Material.STUFFED_ANIMAL, 1);
            if (firstAttempt && achievementSystem != null) {
                achievementSystem.unlock(AchievementType.CLAW_MASTER);
            }
            return ClawResult.WIN;
        }

        // Early drop check
        if (random.nextFloat() < CLAW_EARLY_DROP_CHANCE) {
            return ClawResult.EARLY_DROP;
        }

        return ClawResult.MISS;
    }

    // ── Arcade Shooter ────────────────────────────────────────────────────────

    /**
     * Simulate a full arcade shooter game.
     *
     * <p>The caller passes the final kill and miss counts from the reaction-time session.
     * If kills &ge; {@link #SHOOTER_KILLS_TO_WIN} and misses &lt; {@link #SHOOTER_MAX_MISSES},
     * the player wins {@link #SHOOTER_TICKET_REWARD} {@link Material#PRIZE_TICKET}s and the
     * {@link AchievementType#ARCADE_LEGEND} achievement.
     *
     * @param inventory player inventory
     * @param kills     number of targets hit in the session
     * @param misses    number of targets missed
     * @param npcs      all NPCs (for rumour seeding on win)
     * @return result of the shooter game
     */
    public ShooterResult playShooter(Inventory inventory, int kills, int misses, List<NPC> npcs) {
        if (playerBanned) return ShooterResult.EJECTED;
        if (inventory.getItemCount(Material.ARCADE_TOKEN) < SHOOTER_COST) {
            return ShooterResult.NO_TOKEN;
        }

        inventory.removeItem(Material.ARCADE_TOKEN, SHOOTER_COST);

        if (kills >= SHOOTER_KILLS_TO_WIN && misses < SHOOTER_MAX_MISSES) {
            inventory.addItem(Material.PRIZE_TICKET, SHOOTER_TICKET_REWARD);
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.ARCADE_LEGEND);
            }
            // Seed a SHOP_NEWS rumour about the high score
            if (rumourNetwork != null && npcs != null && !npcs.isEmpty()) {
                NPC target = findAnyNPC(npcs);
                if (target != null) {
                    rumourNetwork.addRumour(target, new Rumour(RumourType.SHOP_NEWS,
                            "Someone smashed the high score on the shooter at Ace Amusements."));
                }
            }
            return ShooterResult.WIN;
        }
        return ShooterResult.GAME_OVER;
    }

    // ── Redemption Counter ────────────────────────────────────────────────────

    /**
     * Player exchanges prize tickets at the redemption counter.
     *
     * @param inventory player inventory
     * @param ticketCost the tier cost to redeem (use one of the REDEEM_* constants)
     * @return the result of the redemption
     */
    public RedemptionResult redeemTickets(Inventory inventory, int ticketCost) {
        int held = inventory.getItemCount(Material.PRIZE_TICKET);
        if (held < ticketCost) return RedemptionResult.NOT_ENOUGH_TICKETS;

        inventory.removeItem(Material.PRIZE_TICKET, ticketCost);

        if (ticketCost == REDEEM_PENNY_SWEETS_COST) {
            inventory.addItem(Material.PENNY_SWEETS, REDEEM_PENNY_SWEETS_QTY);
        } else if (ticketCost == REDEEM_STUFFED_ANIMAL_COST) {
            inventory.addItem(Material.STUFFED_ANIMAL, 1);
        } else if (ticketCost == REDEEM_PLASTIC_TROPHY_COST) {
            inventory.addItem(Material.PLASTIC_TROPHY, 1);
        } else if (ticketCost == REDEEM_CHAMPION_BADGE_COST) {
            inventory.addItem(Material.ARCADE_CHAMPION_BADGE, 1);
            if (!redemptionArcAwarded && achievementSystem != null) {
                redemptionArcAwarded = true;
                achievementSystem.unlock(AchievementType.REDEMPTION_ARC);
            }
        } else {
            // Put tickets back and signal invalid tier
            inventory.addItem(Material.PRIZE_TICKET, ticketCost);
            return RedemptionResult.INVALID_TIER;
        }

        return RedemptionResult.SUCCESS;
    }

    // ── Machine Tampering ─────────────────────────────────────────────────────

    /**
     * Player attempts to tamper with a machine (hold E for {@link #TAMPER_HOLD_SECONDS} seconds).
     *
     * <p>Kevin's detection radius is:
     * <ul>
     *   <li>{@link #KEVIN_DETECTION_RADIUS} normally.</li>
     *   <li>{@link #KEVIN_DETECTION_RADIUS_DISGUISED} if player carries
     *       {@link Material#BUILDER_OVERALLS}.</li>
     *   <li>Ignored entirely if player carries {@link Material#ARCADE_CHAMPION_BADGE}
     *       (full tamper immunity).</li>
     *   <li>Treated as max range if Kevin is distracted.</li>
     * </ul>
     *
     * @param inventory     player inventory
     * @param action        the tamper action to perform
     * @param kevinDistance current distance of Kevin from the player in blocks
     * @return result of the tamper attempt
     */
    public TamperResult attemptTamper(Inventory inventory, TamperAction action, float kevinDistance) {
        if (playerBanned) return TamperResult.EJECTED;

        // ARCADE_CHAMPION_BADGE grants full immunity
        if (inventory.getItemCount(Material.ARCADE_CHAMPION_BADGE) > 0) {
            return performTamper(inventory, action);
        }

        if (inventory.getItemCount(Material.SCREWDRIVER) < 1) {
            return TamperResult.NO_SCREWDRIVER;
        }

        // Determine effective detection radius
        float radius = KEVIN_DETECTION_RADIUS;
        if (inventory.getItemCount(Material.BUILDER_OVERALLS) > 0) {
            radius = KEVIN_DETECTION_RADIUS_DISGUISED;
        }

        // Kevin is distracted — treat as if very far away
        if (kevinDistracted) {
            kevinDistance = Float.MAX_VALUE;
        }

        if (kevin != null && kevinDistance <= radius) {
            // Kevin catches the player
            banPlayer();
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.CRIMINAL_DAMAGE);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(TAMPER_CAUGHT_NOTORIETY, null);
            }
            if (kevin != null) {
                kevin.setSpeechText(KEVIN_CAUGHT_TAMPER, 5f);
            }
            return TamperResult.KEVIN_TOO_CLOSE;
        }

        return performTamper(inventory, action);
    }

    private TamperResult performTamper(Inventory inventory, TamperAction action) {
        switch (action) {
            case LOWER_THRESHOLD:
                jackpotThreshold = Math.max(1, jackpotThreshold - TAMPER_THRESHOLD_REDUCTION);
                break;
            case FREE_PLAY:
                // Grant one free token
                inventory.addItem(Material.ARCADE_TOKEN, 1);
                break;
            case EXTRACT_COIN:
                inventory.addItem(Material.COIN, TAMPER_COIN_EXTRACT);
                break;
            default:
                break;
        }

        // Add tamper noise
        if (noiseSystem != null) {
            int noiseAmount = TAMPER_NOISE_MIN + random.nextInt(TAMPER_NOISE_MAX - TAMPER_NOISE_MIN + 1);
            noiseSystem.addNoise(noiseAmount * 0.1f);
        }

        // Award achievement on first tamper
        if (!dodgyEngineerAwarded && achievementSystem != null) {
            dodgyEngineerAwarded = true;
            achievementSystem.unlock(AchievementType.DODGY_ENGINEER);
        }

        return TamperResult.SUCCESS;
    }

    // ── Ban management ────────────────────────────────────────────────────────

    /**
     * Ban the player from the arcade for {@link #BAN_DURATION_MINUTES} in-game minutes.
     */
    public void banPlayer() {
        playerBanned = true;
        banTimeRemaining = BAN_DURATION_MINUTES * 60f; // convert minutes to seconds
    }

    /**
     * Update the ban timer. Call once per frame.
     *
     * @param delta seconds since last frame
     */
    public void updateBan(float delta) {
        if (playerBanned && banTimeRemaining > 0) {
            banTimeRemaining -= delta;
            if (banTimeRemaining <= 0) {
                playerBanned = false;
                banTimeRemaining = 0f;
            }
        }
    }

    // ── ARCADE_KID distraction ────────────────────────────────────────────────

    /**
     * Update Kevin's distraction state. Call once per frame.
     * ARCADE_KID NPCs in the list have a {@link #KID_DISTRACT_CHANCE_PER_MIN} per-minute
     * chance to distract Kevin.
     *
     * @param delta    seconds since last frame
     * @param arcadeKids list of ARCADE_KID NPCs currently in the arcade
     */
    public void updateKidDistraction(float delta, List<NPC> arcadeKids) {
        if (kevinDistracted) {
            kevinDistractRemaining -= delta;
            if (kevinDistractRemaining <= 0) {
                kevinDistracted = false;
                kevinDistractRemaining = 0f;
            }
        } else if (arcadeKids != null) {
            // Per-second probability derived from per-minute chance
            float chancePerSecond = KID_DISTRACT_CHANCE_PER_MIN / 60f;
            for (NPC kid : arcadeKids) {
                if (kid == null) continue;
                float roll = random.nextFloat();
                if (roll < chancePerSecond * delta * arcadeKids.size()) {
                    kevinDistracted = true;
                    kevinDistractRemaining = KID_DISTRACT_DURATION_SECONDS;
                    break;
                }
            }
        }
    }

    // ── Visit state reset ─────────────────────────────────────────────────────

    /**
     * Reset per-visit state when the player leaves the arcade (e.g. on tilt ejection).
     */
    public void resetVisitState() {
        tiltCountThisVisit = 0;
        pennyFallsPushes = 0;
    }

    // ── Utility helpers ───────────────────────────────────────────────────────

    private NPC findAnyNPC(List<NPC> npcs) {
        if (npcs == null) return null;
        for (NPC npc : npcs) {
            if (npc != null && npc.isAlive()) return npc;
        }
        return null;
    }

    // ── Accessors for testing ─────────────────────────────────────────────────

    /** Returns whether the player is currently banned. */
    public boolean isPlayerBanned() {
        return playerBanned;
    }

    /** Returns the remaining ban time in seconds. */
    public float getBanTimeRemaining() {
        return banTimeRemaining;
    }

    /** Returns the current penny-falls push count. */
    public int getPennyFallsPushes() {
        return pennyFallsPushes;
    }

    /** Returns the current jackpot threshold. */
    public int getJackpotThreshold() {
        return jackpotThreshold;
    }

    /**
     * Force-set the jackpot threshold for deterministic testing.
     *
     * @param threshold new jackpot threshold in pushes
     */
    public void forceJackpotThreshold(int threshold) {
        this.jackpotThreshold = threshold;
    }

    /** Returns the number of machine tilts this visit. */
    public int getTiltCountThisVisit() {
        return tiltCountThisVisit;
    }

    /** Returns whether Kevin is currently distracted. */
    public boolean isKevinDistracted() {
        return kevinDistracted;
    }

    /** Force-set Kevin distracted state for testing. */
    public void forceKevinDistracted(boolean distracted) {
        this.kevinDistracted = distracted;
        if (distracted) {
            kevinDistractRemaining = KID_DISTRACT_DURATION_SECONDS;
        }
    }

    /** Returns whether the DODGY_ENGINEER achievement has been awarded. */
    public boolean isDodgyEngineerAwarded() {
        return dodgyEngineerAwarded;
    }

    /** Reset claw first-attempt flag for testing. */
    public void resetClawFirstAttempt() {
        clawFirstAttempt = true;
    }
}

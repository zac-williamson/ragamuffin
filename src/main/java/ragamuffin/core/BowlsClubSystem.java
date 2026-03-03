package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1483: Northfield Crown Green Bowls Club — Reg's Green, the Grudge Match
 * &amp; the Bowl Racket.
 *
 * <p>Crown green bowls is a quintessentially British working-class recreation.
 * The club occupies a fenced-off 20×20 GRASS green with a crown raise at the edge
 * of the park, behind a privet hedge and a chipped white gate.
 *
 * <h3>Mechanic 1 — Getting On the Green</h3>
 * <ul>
 *   <li>Reg (BOWLS_CLUB_SECRETARY) sells BOWLS_CLUB_MEMBERSHIP for 3 COIN.</li>
 *   <li>Non-members who trespass twice get the PCSO called.</li>
 *   <li>BOWLS_SET costs 5 COIN to purchase or 1 COIN to borrow per session.</li>
 *   <li>A STOLEN_BOWLS_SET can be pickpocketed from a BOWLS_PLAYER NPC (50% success).</li>
 * </ul>
 *
 * <h3>Mechanic 2 — The Mini-Game</h3>
 * <ul>
 *   <li>{@code playEnd()} uses BattleBarMiniGame timing bar.</li>
 *   <li>Each bowl curves by {@link #BIAS_CURVE_FACTOR} = 0.3f blocks.</li>
 *   <li>Green zone (≤1 block from jack) = 2 pts; yellow (≤2 blocks) = 1 pt; outside = 0.</li>
 *   <li>Full game = {@link #ENDS_PER_GAME} ends.</li>
 *   <li>Three NPC difficulty tiers: BOWLS_NOVICE, BOWLS_CLUB_PLAYER, Reg (hardest).</li>
 * </ul>
 *
 * <h3>Mechanic 3 — Grudge Match &amp; Side Betting</h3>
 * <ul>
 *   <li>Every Saturday 14:00, Reg vs Arthur (BOWLS_RIVAL).</li>
 *   <li>BOWLS_SPECTATOR NPCs accept 1–{@link #MAX_WAGER} COIN wagers (1.5× payout).</li>
 *   <li>Player can nobble Arthur's bowls with WEIGHTED_BOWL during a
 *       {@link #PCSO_DISTRACTION_WINDOW_SECONDS}-second window.
 *       If caught: CrimeType.CHEATING_AT_BOWLS, Notoriety +{@link #NOBBLE_NOTORIETY},
 *       banned {@link #NOBBLE_BAN_DAYS} days.</li>
 * </ul>
 *
 * <h3>Mechanic 4 — The Jack Racket</h3>
 * <ul>
 *   <li>Steal JACK → fence (4 COIN) or ransom to Reg (3 COIN + Respect).</li>
 *   <li>CHAMPIONSHIP_JACK (during Tournament): fence (12 COIN) but cancels tournament,
 *       Notoriety +{@link #CHAMPIONSHIP_JACK_NOTORIETY}, Vibes −5.</li>
 * </ul>
 *
 * <h3>Mechanic 5 — Annual Tournament</h3>
 * <ul>
 *   <li>Every {@link #TOURNAMENT_INTERVAL_DAYS} in-game days at 14:00.</li>
 *   <li>Entry 2 COIN; prize {@link #TOURNAMENT_PRIZE_COINS} COIN + BOWLS_TROPHY.</li>
 *   <li>Single-elimination 8-player bracket.</li>
 *   <li>Win 3 consecutive tournaments: THE_DYNASTY achievement + Vice-Secretary title.</li>
 * </ul>
 *
 * <h3>Integrations</h3>
 * <ul>
 *   <li>{@link NotorietySystem} — notoriety gains for cheating, championship sabotage.</li>
 *   <li>{@link CriminalRecord} — CHEATING_AT_BOWLS.</li>
 *   <li>{@link RumourNetwork} — BOWLS_GRUDGE_MATCH, MISSING_JACK, CHAMPIONSHIP_SABOTAGE,
 *       BOWLS_DYNASTY.</li>
 * </ul>
 */
public class BowlsClubSystem {

    // ── Schedule constants ────────────────────────────────────────────────────

    /** Club opening hour, Tue–Sun (10:00). */
    public static final float OPEN_HOUR = 10.0f;

    /** Club closing hour, Tue–Sun (17:00). */
    public static final float CLOSE_HOUR = 17.0f;

    /** Hour of the Saturday Grudge Match (14:00). */
    public static final float GRUDGE_MATCH_HOUR = 14.0f;

    /** In-game hour when Grudge Match rumour is seeded (13:30). */
    public static final float GRUDGE_MATCH_RUMOUR_HOUR = 13.5f;

    /** Hour of the Annual Tournament (14:00). */
    public static final float TOURNAMENT_HOUR = 14.0f;

    /** Days between Annual Tournaments. */
    public static final int TOURNAMENT_INTERVAL_DAYS = 14;

    // ── Membership &amp; equipment costs ─────────────────────────────────────────

    /** Cost of BOWLS_CLUB_MEMBERSHIP in COIN. */
    public static final int MEMBERSHIP_COST = 3;

    /** Cost to buy a BOWLS_SET outright in COIN. */
    public static final int BOWLS_SET_BUY_COST = 5;

    /** Cost to borrow a BOWLS_SET for one session in COIN. */
    public static final int BOWLS_SET_BORROW_COST = 1;

    /** Number of trespass warnings before PCSO is called on a non-member. */
    public static final int TRESPASS_PCSO_THRESHOLD = 2;

    // ── Mini-game constants ───────────────────────────────────────────────────

    /**
     * Bias curve factor: how many blocks off-centre each bowl drifts if the
     * player does not compensate with directional input.
     */
    public static final float BIAS_CURVE_FACTOR = 0.3f;

    /** Distance from jack (in blocks) for the green zone (2 pts). */
    public static final float GREEN_ZONE_DISTANCE = 1.0f;

    /** Distance from jack (in blocks) for the yellow zone (1 pt). */
    public static final float YELLOW_ZONE_DISTANCE = 2.0f;

    /** Points awarded for a green-zone delivery. */
    public static final int GREEN_ZONE_POINTS = 2;

    /** Points awarded for a yellow-zone delivery. */
    public static final int YELLOW_ZONE_POINTS = 1;

    /** Number of ends in a full game. */
    public static final int ENDS_PER_GAME = 8;

    // ── Difficulty — NPC hit-zone widths &amp; cursor speeds ──────────────────────

    /** Hit-zone width for BOWLS_NOVICE difficulty (easy). */
    public static final float NOVICE_HIT_ZONE_WIDTH = 0.30f;

    /** Cursor speed for BOWLS_NOVICE difficulty (easy). */
    public static final float NOVICE_CURSOR_SPEED = 0.42f;

    /** Hit-zone width for BOWLS_CLUB_PLAYER difficulty (medium). */
    public static final float PLAYER_HIT_ZONE_WIDTH = 0.20f;

    /** Cursor speed for BOWLS_CLUB_PLAYER difficulty (medium). */
    public static final float PLAYER_CURSOR_SPEED = 0.62f;

    /** Hit-zone width for Reg (hardest) difficulty. */
    public static final float REG_HIT_ZONE_WIDTH = 0.12f;

    /** Cursor speed for Reg (hardest) difficulty. */
    public static final float REG_CURSOR_SPEED = 0.90f;

    // ── Grudge Match betting constants ────────────────────────────────────────

    /** Minimum wager on the Grudge Match in COIN. */
    public static final int MIN_WAGER = 1;

    /** Maximum wager on the Grudge Match in COIN. */
    public static final int MAX_WAGER = 10;

    /** Payout multiplier for a successful Grudge Match wager. */
    public static final float WAGER_PAYOUT_MULTIPLIER = 1.5f;

    /** Seconds the PCSO is distracted — the nobble window. */
    public static final float PCSO_DISTRACTION_WINDOW_SECONDS = 30.0f;

    /** Notoriety gained if caught nobbling Arthur's bowl. */
    public static final int NOBBLE_NOTORIETY = 6;

    /** Days banned from the green if caught nobbling. */
    public static final int NOBBLE_BAN_DAYS = 5;

    // ── Jack racket constants ─────────────────────────────────────────────────

    /** COIN value of the JACK when sold to the fence. */
    public static final int JACK_FENCE_VALUE = 4;

    /** COIN payout when ransoming the JACK back to Reg. */
    public static final int JACK_RANSOM_COINS = 3;

    /** COIN value of the CHAMPIONSHIP_JACK when sold to the fence. */
    public static final int CHAMPIONSHIP_JACK_FENCE_VALUE = 12;

    /** Notoriety gained for stealing the CHAMPIONSHIP_JACK. */
    public static final int CHAMPIONSHIP_JACK_NOTORIETY = 10;

    /** Vibes penalty for stealing the CHAMPIONSHIP_JACK. */
    public static final int CHAMPIONSHIP_JACK_VIBES_PENALTY = 5;

    // ── Tournament constants ──────────────────────────────────────────────────

    /** Entry fee for the Annual Tournament in COIN. */
    public static final int TOURNAMENT_ENTRY_FEE = 2;

    /** Prize money for winning the Annual Tournament in COIN. */
    public static final int TOURNAMENT_PRIZE_COINS = 20;

    /** Number of players in the Annual Tournament bracket. */
    public static final int TOURNAMENT_BRACKET_SIZE = 8;

    /** Consecutive tournament wins needed for THE_DYNASTY achievement. */
    public static final int DYNASTY_WIN_COUNT = 3;

    // ── Probability constants ─────────────────────────────────────────────────

    /** Chance of successfully pickpocketing a STOLEN_BOWLS_SET from a BOWLS_CLUB_PLAYER. */
    public static final float PICKPOCKET_SUCCESS_CHANCE = 0.50f;

    // ── Inner state classes ───────────────────────────────────────────────────

    /** Possible difficulty tiers for a bowls opponent. */
    public enum Difficulty {
        NOVICE, CLUB_PLAYER, REG
    }

    /** Result codes for {@link #buyMembership}. */
    public enum MembershipResult {
        /** Membership purchased successfully. */
        PURCHASED,
        /** Player already holds a membership. */
        ALREADY_MEMBER,
        /** Player does not have enough COIN. */
        INSUFFICIENT_FUNDS
    }

    /** Result codes for {@link #acquireBowlsSet}. */
    public enum BowlsSetResult {
        /** Set purchased outright. */
        PURCHASED,
        /** Set borrowed for this session. */
        BORROWED,
        /** Player does not have enough COIN. */
        INSUFFICIENT_FUNDS
    }

    /** Result codes for {@link #playEnd}. */
    public enum EndResult {
        /** Player scored in the green zone (2 pts). */
        GREEN_ZONE,
        /** Player scored in the yellow zone (1 pt). */
        YELLOW_ZONE,
        /** Player's bowl was outside the scoring zone. */
        OUTSIDE,
        /** Player does not have a bowls set in inventory. */
        NO_BOWLS_SET
    }

    /** Result codes for {@link #placeWager}. */
    public enum WagerResult {
        /** Wager placed successfully. */
        PLACED,
        /** The Grudge Match is not currently active. */
        NO_MATCH_ACTIVE,
        /** Wager amount is outside the 1–10 COIN range. */
        INVALID_AMOUNT,
        /** Player does not have enough COIN. */
        INSUFFICIENT_FUNDS
    }

    /** Result codes for {@link #attemptNobble}. */
    public enum NobbleResult {
        /** Nobble succeeded — Arthur's bowl swapped. */
        SUCCESS,
        /** PCSO distraction window is not open. */
        WINDOW_NOT_OPEN,
        /** Player does not have a WEIGHTED_BOWL in inventory. */
        NO_WEIGHTED_BOWL,
        /** Player was caught — CHEATING_AT_BOWLS recorded. */
        CAUGHT
    }

    /** Result codes for {@link #stealJack}. */
    public enum JackResult {
        /** Jack stolen successfully. */
        STOLEN,
        /** No jack is on the green. */
        NO_JACK_PRESENT,
        /** Player was witnessed — Notoriety added. */
        WITNESSED
    }

    /** Result codes for {@link #enterTournament}. */
    public enum TournamentResult {
        /** Player entered the tournament. */
        ENTERED,
        /** The tournament is not currently active. */
        NOT_ACTIVE,
        /** Player does not have enough COIN. */
        INSUFFICIENT_FUNDS,
        /** Player is banned from the green. */
        BANNED
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random rng;

    /** Whether the player holds a BOWLS_CLUB_MEMBERSHIP. */
    private boolean hasMembership;

    /** How many times the player has trespassed without a membership. */
    private int trespassCount;

    /** Whether the PCSO has been called for trespassing. */
    private boolean pcsoCalledForTrespass;

    /** Whether the player currently has a borrowed bowls set for this session. */
    private boolean setCurrentlyBorrowed;

    /** Player's accumulated score in the current game. */
    private int playerScore;

    /** Opponent's accumulated score in the current game. */
    private int opponentScore;

    /** Number of ends played so far in the current game. */
    private int endsPlayed;

    /** Whether a Grudge Match is currently active. */
    private boolean grudgeMatchActive;

    /** Current wager on the Grudge Match (0 = no wager). */
    private int activeWager;

    /** Whether the player has bet on Reg (true) or Arthur (false). */
    private boolean wageredOnReg;

    /** Whether the PCSO distraction window is currently open. */
    private boolean pcsoDistractionWindowOpen;

    /** Remaining seconds in the PCSO distraction window. */
    private float pcsoDistractionTimeRemaining;

    /** Whether Arthur's bowl has been nobbled in the current Grudge Match. */
    private boolean arthurNobbled;

    /** Whether the JACK is currently on the green. */
    private boolean jackOnGreen;

    /** Whether the CHAMPIONSHIP_JACK is currently on the green. */
    private boolean championshipJackOnGreen;

    /** Whether the Annual Tournament is currently active. */
    private boolean tournamentActive;

    /** Whether the tournament has been cancelled (championship jack stolen). */
    private boolean tournamentCancelled;

    /** Number of consecutive tournaments the player has won. */
    private int consecutiveTournamentWins;

    /** Whether the player has earned the THE_DYNASTY achievement. */
    private boolean dynastyAchieved;

    /** Whether the player is currently banned from the green. */
    private boolean banned;

    /** Remaining in-game days of a ban (0 = not banned). */
    private int banDaysRemaining;

    /** Whether the Grudge Match rumour has been seeded today. */
    private boolean grudgeMatchRumourSeeded;

    // ── Injected dependencies ─────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Construct a new BowlsClubSystem with the given random number generator.
     *
     * @param rng seeded Random for deterministic tests
     */
    public BowlsClubSystem(Random rng) {
        this.rng = rng;
        this.jackOnGreen = true; // jack starts on the green
    }

    // ── Dependency injection setters ──────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem system) {
        this.notorietySystem = system;
    }

    public void setCriminalRecord(CriminalRecord record) {
        this.criminalRecord = record;
    }

    public void setRumourNetwork(RumourNetwork network) {
        this.rumourNetwork = network;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Advance the system by one frame.
     *
     * @param delta           seconds since last frame
     * @param timeSystem      provides current in-game time
     * @param allNpcs         all active NPCs (for rumour seeding)
     * @param achievementCallback callback for unlocking achievements
     */
    public void update(float delta, TimeSystem timeSystem,
                       List<NPC> allNpcs,
                       NotorietySystem.AchievementCallback achievementCallback) {

        float hour = timeSystem.getTime();
        int dayOfWeek = (timeSystem.getDayCount() + 4) % 7; // 0=Mon … 6=Sun; day 1 = Sat = 5

        // Tick PCSO distraction window
        if (pcsoDistractionWindowOpen) {
            pcsoDistractionTimeRemaining -= delta;
            if (pcsoDistractionTimeRemaining <= 0f) {
                pcsoDistractionWindowOpen = false;
            }
        }

        // Seed Grudge Match rumour at 13:30 on Saturdays
        if (dayOfWeek == 5 && hour >= GRUDGE_MATCH_RUMOUR_HOUR && !grudgeMatchRumourSeeded) {
            grudgeMatchRumourSeeded = true;
            if (rumourNetwork != null && allNpcs != null) {
                NPC source = findNpcOfType(allNpcs, NPCType.BOWLS_SPECTATOR);
                if (source != null) {
                    rumourNetwork.addRumour(source,
                            new Rumour(RumourType.BOWLS_GRUDGE_MATCH,
                                    "Reg versus Arthur today at two. Same old same old."));
                }
            }
        }

        // Start Grudge Match at 14:00 on Saturdays
        if (dayOfWeek == 5 && hour >= GRUDGE_MATCH_HOUR && !grudgeMatchActive) {
            grudgeMatchActive = true;
            arthurNobbled = false;
        }

        // Reset daily state at midnight
        if (hour < 0.1f) {
            grudgeMatchRumourSeeded = false;
            if (grudgeMatchActive) {
                grudgeMatchActive = false;
                resolveGrudgeMatchWager(achievementCallback);
            }
        }
    }

    // ── Mechanic 1: Getting On the Green ─────────────────────────────────────

    /**
     * Attempt to purchase a BOWLS_CLUB_MEMBERSHIP from Reg.
     *
     * @param inventory player inventory
     * @param coin      amount of COIN player has
     * @return result code
     */
    public MembershipResult buyMembership(Inventory inventory, int coin,
                                          NotorietySystem.AchievementCallback achievementCallback) {
        if (hasMembership) return MembershipResult.ALREADY_MEMBER;
        if (coin < MEMBERSHIP_COST) return MembershipResult.INSUFFICIENT_FUNDS;
        inventory.removeItem(Material.COIN, MEMBERSHIP_COST);
        inventory.addItem(Material.BOWLS_CLUB_MEMBERSHIP, 1);
        hasMembership = true;
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.CARD_CARRYING_MEMBER);
        }
        return MembershipResult.PURCHASED;
    }

    /**
     * Attempt to acquire a BOWLS_SET by purchasing or borrowing.
     *
     * @param inventory  player inventory
     * @param coin       amount of COIN player has
     * @param wantToBuy  {@code true} to purchase outright; {@code false} to borrow
     * @return result code
     */
    public BowlsSetResult acquireBowlsSet(Inventory inventory, int coin, boolean wantToBuy) {
        if (wantToBuy) {
            if (coin < BOWLS_SET_BUY_COST) return BowlsSetResult.INSUFFICIENT_FUNDS;
            inventory.removeItem(Material.COIN, BOWLS_SET_BUY_COST);
            inventory.addItem(Material.BOWLS_SET, 1);
            return BowlsSetResult.PURCHASED;
        } else {
            if (coin < BOWLS_SET_BORROW_COST) return BowlsSetResult.INSUFFICIENT_FUNDS;
            inventory.removeItem(Material.COIN, BOWLS_SET_BORROW_COST);
            inventory.addItem(Material.BOWLS_SET, 1);
            setCurrentlyBorrowed = true;
            return BowlsSetResult.BORROWED;
        }
    }

    /**
     * Record a trespass by a non-member.
     * On the {@link #TRESPASS_PCSO_THRESHOLD}-th trespass the PCSO is called.
     *
     * @return {@code true} if the PCSO has now been called
     */
    public boolean recordTrespass() {
        if (hasMembership) return false;
        trespassCount++;
        if (trespassCount >= TRESPASS_PCSO_THRESHOLD) {
            pcsoCalledForTrespass = true;
            return true;
        }
        return false;
    }

    /**
     * Attempt to pickpocket a STOLEN_BOWLS_SET from the given NPC.
     * 50% chance of success with the seeded RNG.
     *
     * @param npc       target NPC (should be BOWLS_CLUB_PLAYER)
     * @param inventory player inventory
     * @return {@code true} if successful
     */
    public boolean attemptPickpocket(NPC npc, Inventory inventory) {
        if (npc.getType() != NPCType.BOWLS_CLUB_PLAYER) return false;
        if (rng.nextFloat() < PICKPOCKET_SUCCESS_CHANCE) {
            inventory.addItem(Material.STOLEN_BOWLS_SET, 1);
            return true;
        }
        return false;
    }

    // ── Mechanic 2: The Mini-Game ─────────────────────────────────────────────

    /**
     * Play one end of crown green bowls using the BattleBarMiniGame timing bar.
     *
     * <p>The caller must press the mini-game's action at the appropriate moment;
     * this method resolves the end based on the mini-game result and applies
     * the bias curve to determine the delivery distance from the jack.
     *
     * @param inventory   player inventory (must hold BOWLS_SET or STOLEN_BOWLS_SET)
     * @param miniGame    a pre-constructed BattleBarMiniGame at the desired difficulty
     * @param pressResult {@code true} if the player pressed at the right moment
     * @param difficulty  opponent difficulty tier (affects opponent's automatic score)
     * @return result indicating zone scored (or NO_BOWLS_SET)
     */
    public EndResult playEnd(Inventory inventory, BattleBarMiniGame miniGame,
                             boolean pressResult, Difficulty difficulty) {
        boolean hasBowls = inventory.getItemCount(Material.BOWLS_SET) > 0
                || inventory.getItemCount(Material.STOLEN_BOWLS_SET) > 0;
        if (!hasBowls) return EndResult.NO_BOWLS_SET;

        endsPlayed++;

        // Resolve delivery distance: a perfect press = 0 bias; a miss = full bias
        float distance;
        if (pressResult) {
            // Hit — ball lands close, with only a small residual curve
            distance = BIAS_CURVE_FACTOR * rng.nextFloat();
        } else {
            // Miss — ball curves away; distance is 1.0f–3.5f blocks from jack
            distance = 1.0f + rng.nextFloat() * 2.5f + BIAS_CURVE_FACTOR;
        }

        EndResult result;
        if (distance <= GREEN_ZONE_DISTANCE) {
            playerScore += GREEN_ZONE_POINTS;
            result = EndResult.GREEN_ZONE;
        } else if (distance <= YELLOW_ZONE_DISTANCE) {
            playerScore += YELLOW_ZONE_POINTS;
            result = EndResult.YELLOW_ZONE;
        } else {
            result = EndResult.OUTSIDE;
        }

        // Opponent scores automatically based on difficulty
        opponentScore += resolveOpponentScore(difficulty);

        return result;
    }

    /**
     * Reset the current game state (player and opponent scores, ends played).
     * Call before starting a new game.
     */
    public void resetGame() {
        playerScore = 0;
        opponentScore = 0;
        endsPlayed = 0;
    }

    /** @return player's current score in the active game. */
    public int getPlayerScore() { return playerScore; }

    /** @return opponent's current score in the active game. */
    public int getOpponentScore() { return opponentScore; }

    /** @return number of ends played so far in the active game. */
    public int getEndsPlayed() { return endsPlayed; }

    /** @return {@code true} if the current game has reached {@link #ENDS_PER_GAME}. */
    public boolean isGameComplete() { return endsPlayed >= ENDS_PER_GAME; }

    // ── Mechanic 3: Grudge Match &amp; Side Betting ─────────────────────────────

    /**
     * Place a wager on the current Grudge Match.
     *
     * @param inventory  player inventory
     * @param coin       amount of COIN player currently has
     * @param amount     wager amount (1–10 COIN)
     * @param onReg      {@code true} to bet on Reg; {@code false} for Arthur
     * @param achievementCallback callback for achievements
     * @return result code
     */
    public WagerResult placeWager(Inventory inventory, int coin, int amount, boolean onReg,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        if (!grudgeMatchActive) return WagerResult.NO_MATCH_ACTIVE;
        if (amount < MIN_WAGER || amount > MAX_WAGER) return WagerResult.INVALID_AMOUNT;
        if (coin < amount) return WagerResult.INSUFFICIENT_FUNDS;

        inventory.removeItem(Material.COIN, amount);
        activeWager = amount;
        wageredOnReg = onReg;

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.PARKS_DEPARTMENT_BOOKIE);
        }
        return WagerResult.PLACED;
    }

    /**
     * Open the PCSO distraction window, allowing the nobble action.
     * Called by the game engine when the player successfully distracts the PCSO.
     */
    public void openPcsoDistractionWindow() {
        pcsoDistractionWindowOpen = true;
        pcsoDistractionTimeRemaining = PCSO_DISTRACTION_WINDOW_SECONDS;
    }

    /**
     * Attempt to nobble Arthur's bowl by swapping it with a WEIGHTED_BOWL.
     *
     * @param inventory player inventory (must contain WEIGHTED_BOWL)
     * @param caught    whether the player was seen (caller determines witness logic)
     * @param achievementCallback callback for achievements
     * @return result code
     */
    public NobbleResult attemptNobble(Inventory inventory, boolean caught,
                                      NotorietySystem.AchievementCallback achievementCallback) {
        if (!pcsoDistractionWindowOpen) return NobbleResult.WINDOW_NOT_OPEN;
        if (inventory.getItemCount(Material.WEIGHTED_BOWL) < 1) return NobbleResult.NO_WEIGHTED_BOWL;

        if (caught) {
            inventory.removeItem(Material.WEIGHTED_BOWL, 1);
            pcsoDistractionWindowOpen = false;
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(NOBBLE_NOTORIETY, achievementCallback);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.CHEATING_AT_BOWLS);
            }
            banned = true;
            banDaysRemaining = NOBBLE_BAN_DAYS;
            return NobbleResult.CAUGHT;
        }

        // Success
        inventory.removeItem(Material.WEIGHTED_BOWL, 1);
        arthurNobbled = true;
        pcsoDistractionWindowOpen = false;
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.LOADED_JACK);
        }
        return NobbleResult.SUCCESS;
    }

    // ── Mechanic 4: The Jack Racket ───────────────────────────────────────────

    /**
     * Attempt to steal the JACK from the green.
     *
     * @param inventory  player inventory
     * @param witnessed  whether an NPC witnessed the theft
     * @param achievementCallback callback for achievements
     * @return result code
     */
    public JackResult stealJack(Inventory inventory, boolean witnessed,
                                NotorietySystem.AchievementCallback achievementCallback) {
        if (!jackOnGreen) return JackResult.NO_JACK_PRESENT;

        if (witnessed) {
            return JackResult.WITNESSED;
        }

        // Determine whether this is the championship jack (during an active tournament)
        boolean isChampionship = tournamentActive && championshipJackOnGreen;
        jackOnGreen = false;
        if (isChampionship) {
            championshipJackOnGreen = false;
            inventory.addItem(Material.CHAMPIONSHIP_JACK, 1);
            tournamentCancelled = true;
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(CHAMPIONSHIP_JACK_NOTORIETY, achievementCallback);
            }
            if (rumourNetwork != null) {
                rumourNetwork.addRumour(null,
                        new Rumour(RumourType.CHAMPIONSHIP_SABOTAGE,
                                "Someone nicked the gold jack. Tournament's off."));
            }
        } else {
            inventory.addItem(Material.JACK, 1);
            if (rumourNetwork != null) {
                rumourNetwork.addRumour(null,
                        new Rumour(RumourType.MISSING_JACK,
                                "Someone's nicked the jack off the crown green."));
            }
        }

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.FINDERS_KEEPERS);
        }
        return JackResult.STOLEN;
    }

    /**
     * Ransom the JACK back to Reg for 3 COIN + Respect.
     *
     * @param inventory player inventory (must contain JACK)
     * @return COIN amount received, or 0 if no JACK in inventory
     */
    public int ransomJack(Inventory inventory) {
        if (inventory.getItemCount(Material.JACK) < 1) return 0;
        inventory.removeItem(Material.JACK, 1);
        inventory.addItem(Material.COIN, JACK_RANSOM_COINS);
        jackOnGreen = true;
        return JACK_RANSOM_COINS;
    }

    // ── Mechanic 5: Annual Tournament ─────────────────────────────────────────

    /**
     * Attempt to enter the Annual Tournament.
     *
     * @param inventory player inventory
     * @param coin      player's current COIN count
     * @return result code
     */
    public TournamentResult enterTournament(Inventory inventory, int coin) {
        if (banned) return TournamentResult.BANNED;
        if (!tournamentActive) return TournamentResult.NOT_ACTIVE;
        if (coin < TOURNAMENT_ENTRY_FEE) return TournamentResult.INSUFFICIENT_FUNDS;
        inventory.removeItem(Material.COIN, TOURNAMENT_ENTRY_FEE);
        return TournamentResult.ENTERED;
    }

    /**
     * Start the Annual Tournament.
     * Spawns the CHAMPIONSHIP_JACK on the green.
     */
    public void startTournament() {
        tournamentActive = true;
        tournamentCancelled = false;
        championshipJackOnGreen = true;
        jackOnGreen = true;
    }

    /**
     * Award the tournament victory to the player.
     *
     * @param inventory           player inventory
     * @param achievementCallback callback for achievements
     */
    public void awardTournamentVictory(Inventory inventory,
                                       NotorietySystem.AchievementCallback achievementCallback) {
        tournamentActive = false;
        inventory.addItem(Material.COIN, TOURNAMENT_PRIZE_COINS);
        inventory.addItem(Material.BOWLS_TROPHY, 1);

        consecutiveTournamentWins++;
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.NORTHFIELD_BOWLS_CHAMPION);
        }

        if (consecutiveTournamentWins >= DYNASTY_WIN_COUNT && !dynastyAchieved) {
            dynastyAchieved = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.THE_DYNASTY);
            }
            if (rumourNetwork != null) {
                rumourNetwork.addRumour(null,
                        new Rumour(RumourType.BOWLS_DYNASTY,
                                "Some lad's won the bowls tournament three years running. "
                                        + "Reg called him Vice-Secretary."));
            }
        }
    }

    /**
     * Reset consecutive tournament win counter (called if the player loses a tournament
     * in between victories).
     */
    public void resetConsecutiveWins() {
        consecutiveTournamentWins = 0;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** @return {@code true} if the player holds a BOWLS_CLUB_MEMBERSHIP. */
    public boolean hasMembership() { return hasMembership; }

    /** @return the number of times the player has trespassed. */
    public int getTrespassCount() { return trespassCount; }

    /** @return {@code true} if the PCSO has been called for trespassing. */
    public boolean isPcsoCalledForTrespass() { return pcsoCalledForTrespass; }

    /** @return {@code true} if the player currently has a borrowed bowls set. */
    public boolean isSetBorrowed() { return setCurrentlyBorrowed; }

    /** @return {@code true} if the Saturday Grudge Match is currently active. */
    public boolean isGrudgeMatchActive() { return grudgeMatchActive; }

    /** @return current active wager amount (0 if no wager). */
    public int getActiveWager() { return activeWager; }

    /** @return {@code true} if the wager is on Reg winning. */
    public boolean isWageredOnReg() { return wageredOnReg; }

    /** @return {@code true} if the PCSO distraction window is currently open. */
    public boolean isPcsoDistractionWindowOpen() { return pcsoDistractionWindowOpen; }

    /** @return remaining seconds in the PCSO distraction window. */
    public float getPcsoDistractionTimeRemaining() { return pcsoDistractionTimeRemaining; }

    /** @return {@code true} if Arthur's bowl has been nobbled this Grudge Match. */
    public boolean isArthurNobbled() { return arthurNobbled; }

    /** @return {@code true} if the JACK is currently on the green. */
    public boolean isJackOnGreen() { return jackOnGreen; }

    /** @return {@code true} if the CHAMPIONSHIP_JACK is on the green. */
    public boolean isChampionshipJackOnGreen() { return championshipJackOnGreen; }

    /** @return {@code true} if the Annual Tournament is currently active. */
    public boolean isTournamentActive() { return tournamentActive; }

    /** @return {@code true} if the tournament was cancelled by a jack theft. */
    public boolean isTournamentCancelled() { return tournamentCancelled; }

    /** @return the player's count of consecutive tournament victories. */
    public int getConsecutiveTournamentWins() { return consecutiveTournamentWins; }

    /** @return {@code true} if the player has achieved THE_DYNASTY. */
    public boolean isDynastyAchieved() { return dynastyAchieved; }

    /** @return {@code true} if the player is currently banned from the green. */
    public boolean isBanned() { return banned; }

    /** @return days remaining on the current ban. */
    public int getBanDaysRemaining() { return banDaysRemaining; }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Determine the opponent's automatic score for one end based on difficulty tier.
     * Higher difficulty = better opponent.
     */
    private int resolveOpponentScore(Difficulty difficulty) {
        float roll = rng.nextFloat();
        switch (difficulty) {
            case NOVICE:
                // Novice: mostly misses, rare yellow
                if (roll < 0.15f) return GREEN_ZONE_POINTS;
                if (roll < 0.45f) return YELLOW_ZONE_POINTS;
                return 0;
            case CLUB_PLAYER:
                // Club player: moderate accuracy
                if (roll < 0.30f) return GREEN_ZONE_POINTS;
                if (roll < 0.65f) return YELLOW_ZONE_POINTS;
                return 0;
            case REG:
            default:
                // Reg: very accurate
                if (roll < 0.55f) return GREEN_ZONE_POINTS;
                if (roll < 0.85f) return YELLOW_ZONE_POINTS;
                return 0;
        }
    }

    /**
     * Resolve any pending Grudge Match wager.
     * Reg wins unless Arthur was nobbled.
     */
    private void resolveGrudgeMatchWager(NotorietySystem.AchievementCallback achievementCallback) {
        if (activeWager <= 0) return;
        // Reg normally wins (he is the better player); nobbling gives Arthur a boost
        boolean regWins = !arthurNobbled || (rng.nextFloat() < 0.35f); // nobbling still not certain
        boolean playerWon = (wageredOnReg && regWins) || (!wageredOnReg && !regWins);
        activeWager = 0;
        // Note: the calling code should add COIN to inventory based on playerWon flag.
        // We expose isLastWagerWon() so callers can query the result.
        lastWagerWon = playerWon;
    }

    /** Whether the last resolved wager was won by the player. */
    private boolean lastWagerWon;

    /** @return {@code true} if the most recently resolved wager was won. */
    public boolean wasLastWagerWon() { return lastWagerWon; }

    /** Find the first NPC of the given type in the list, or null. */
    private NPC findNpcOfType(List<NPC> npcs, NPCType type) {
        for (NPC npc : npcs) {
            if (npc.getType() == type) return npc;
        }
        return null;
    }
}

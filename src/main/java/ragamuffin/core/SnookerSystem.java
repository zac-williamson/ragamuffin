package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.List;
import java.util.Random;

/**
 * Issue #965: Northfield Snooker Hall — The Reds, Hustling &amp; Smoky Back Rooms.
 *
 * <p>Manages Cue Zone, a dingy upstairs snooker hall above a row of shops on the
 * high street. Open 10:00–23:00 daily.
 *
 * <h3>Snooker Mini-Game</h3>
 * A turn-based snooker frame against NPC opponents using a timing bar
 * (green/yellow/red zones). A frame runs for {@link #FRAME_SCORING_EVENTS} scoring
 * events; the player with more points wins.
 *
 * <h3>Difficulty Tiers</h3>
 * <ul>
 *   <li>{@link NpcDifficulty#NOVICE} — low-accuracy NPC, low point totals.</li>
 *   <li>{@link NpcDifficulty#HUSTLER} — Frank, played at medium accuracy.</li>
 *   <li>{@link NpcDifficulty#LEGEND} — One-Armed Carl, unlocked at STREET_LADS Respect ≥ 75.</li>
 * </ul>
 *
 * <h3>Economics</h3>
 * <ul>
 *   <li>Table rental: {@link #RENTAL_COST} COIN; win returns rental + {@link #WIN_PRIZE} COIN.</li>
 *   <li>Hustle betting: 1–{@link #HUSTLE_BET_MAX} COIN; deliberately miss
 *       {@link #HUSTLE_MISS_COUNT} shots to set up the hustle (doubles bet, drops NPC tier);
 *       {@link #HUSTLE_DETECT_CHANCE}% chance of detection.</li>
 * </ul>
 *
 * <h3>Back Room (Pontoon)</h3>
 * Accessible at MARCHETTI_CREW Respect ≥ {@link #BACK_ROOM_RESPECT_THRESHOLD}.
 * Entry costs {@link #BACK_ROOM_ENTRY_COST} COIN; maximum payout {@link #BACK_ROOM_MAX_PAYOUT} COIN.
 * One game per in-game day. Cheat using STOLEN_PHONE:
 * {@link #CHEAT_CATCH_CHANCE}% caught → Notoriety +{@link #CHEAT_CAUGHT_NOTORIETY},
 * Respect −{@link #CHEAT_CAUGHT_RESPECT_LOSS}.
 *
 * <h3>Brawl consequences</h3>
 * Dennis bans the player for {@link #BAN_DURATION_DAYS} in-game day if brawling occurs.
 * MARCHETTI_CREW Respect −{@link #BRAWL_MARCHETTI_RESPECT_LOSS};
 * THE_COUNCIL Respect +{@link #BRAWL_COUNCIL_RESPECT_GAIN}.
 *
 * <h3>Faction &amp; Rumour Integration</h3>
 * <ul>
 *   <li>Beat Frank twice → STREET_LADS Respect +{@link #BEAT_FRANK_RESPECT_GAIN},
 *       seeds {@link RumourType#SHOP_NEWS}.</li>
 *   <li>Win back-room card game → MARCHETTI_CREW Respect +{@link #BACK_ROOM_WIN_RESPECT_GAIN},
 *       seeds {@link RumourType#GANG_ACTIVITY}.</li>
 *   <li>Caught cheating in card game → MARCHETTI_CREW Respect −{@link #CHEAT_CAUGHT_RESPECT_LOSS}.</li>
 * </ul>
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@link AchievementType#FIRST_FRAME} — first snooker frame played.</li>
 *   <li>{@link AchievementType#SNOOKER_HUSTLER} — successfully hustled Frank.</li>
 *   <li>{@link AchievementType#SNOOKER_LEGEND} — defeated One-Armed Carl.</li>
 *   <li>{@link AchievementType#BACK_ROOM_WINNER} — won back-room pontoon.</li>
 *   <li>{@link AchievementType#CHALK_AND_TALK} — bought chalk from Dennis.</li>
 * </ul>
 *
 * <h3>CUE weapon</h3>
 * {@link Material#CUE} is craftable (WOOD×2). Melee weapon: 2 damage per hit, breaks
 * after 3 hits, drops WOOD×1.
 */
public class SnookerSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Shop opening hour (10:00). */
    public static final float OPEN_HOUR = 10.0f;

    /** Shop closing hour (23:00). */
    public static final float CLOSE_HOUR = 23.0f;

    /** COIN cost to rent a table for one frame. */
    public static final int RENTAL_COST = 2;

    /** COIN prize awarded to the winner (on top of rental refund). */
    public static final int WIN_PRIZE = 3;

    /** Number of scoring events per frame. */
    public static final int FRAME_SCORING_EVENTS = 15;

    /** Green zone threshold (0.0–0.35): best shot quality, 3 points. */
    public static final float TIMING_GREEN_MAX = 0.35f;

    /** Yellow zone threshold (0.35–0.70): decent shot, 1 point. */
    public static final float TIMING_YELLOW_MAX = 0.70f;

    /** Red zone (0.70–1.0): miss — 0 points. */
    public static final float TIMING_RED_MIN = 0.70f;

    /** Points for a green-zone shot. */
    public static final int POINTS_GREEN = 3;

    /** Points for a yellow-zone shot. */
    public static final int POINTS_YELLOW = 1;

    /** Points for a red-zone (miss) shot. */
    public static final int POINTS_RED = 0;

    /** Maximum COIN the player can bet in a hustle. */
    public static final int HUSTLE_BET_MAX = 10;

    /** Number of deliberate misses required to set up a hustle. */
    public static final int HUSTLE_MISS_COUNT = 3;

    /** Percent chance of hustle being detected (0–100). */
    public static final int HUSTLE_DETECT_CHANCE = 25;

    /** Hustle detection multiplies the bet payout for the player. */
    public static final int HUSTLE_WIN_MULTIPLIER = 2;

    /** MARCHETTI_CREW Respect required to access the back room. */
    public static final int BACK_ROOM_RESPECT_THRESHOLD = 60;

    /** COIN entry cost for the back-room card game. */
    public static final int BACK_ROOM_ENTRY_COST = 5;

    /** Maximum COIN payout from the back-room card game. */
    public static final int BACK_ROOM_MAX_PAYOUT = 20;

    /** Percent chance of being caught cheating in the card game (0–100). */
    public static final int CHEAT_CATCH_CHANCE = 25;

    /** Notoriety added when caught cheating. */
    public static final int CHEAT_CAUGHT_NOTORIETY = 8;

    /** MARCHETTI_CREW Respect lost when caught cheating. */
    public static final int CHEAT_CAUGHT_RESPECT_LOSS = 20;

    /** In-game days the player is banned by Dennis for brawling. */
    public static final int BAN_DURATION_DAYS = 1;

    /** MARCHETTI_CREW Respect lost for a brawl in the hall. */
    public static final int BRAWL_MARCHETTI_RESPECT_LOSS = 10;

    /** THE_COUNCIL Respect gained for a brawl in the hall. */
    public static final int BRAWL_COUNCIL_RESPECT_GAIN = 3;

    /** STREET_LADS Respect gained for beating Frank twice. */
    public static final int BEAT_FRANK_RESPECT_GAIN = 5;

    /** MARCHETTI_CREW Respect gained for winning the back-room card game. */
    public static final int BACK_ROOM_WIN_RESPECT_GAIN = 5;

    /** STREET_LADS Respect threshold to unlock One-Armed Carl. */
    public static final int CARL_UNLOCK_RESPECT = 75;

    /** Times player must beat Frank to trigger STREET_LADS Respect bonus. */
    public static final int FRANK_WIN_THRESHOLD = 2;

    /** CUE weapon damage per hit. */
    public static final int CUE_DAMAGE = 2;

    /** Number of combat hits before CUE breaks. */
    public static final int CUE_BREAK_HITS = 3;

    /** WOOD dropped when CUE breaks. */
    public static final int CUE_BREAK_WOOD_DROP = 1;

    // ── NPC difficulty tier ────────────────────────────────────────────────────

    /**
     * Difficulty tier for NPC opponents in the snooker mini-game.
     */
    public enum NpcDifficulty {
        /**
         * Novice — generic beginner NPC. Low accuracy; scoring chance 40%.
         * Not a named NPC.
         */
        NOVICE,

        /**
         * Hustler — Frank the Hustler ({@link NPCType#SNOOKER_HUSTLER}).
         * Medium accuracy; scoring chance 60%.
         * Wanders Cue Zone 12:00–22:00.
         */
        HUSTLER,

        /**
         * Legend — One-Armed Carl ({@link NPCType#PUBLIC}, named "one_armed_carl").
         * High accuracy; scoring chance 80%.
         * Unlocked when STREET_LADS Respect ≥ {@link #CARL_UNLOCK_RESPECT}.
         */
        LEGEND
    }

    // ── Frame result ──────────────────────────────────────────────────────────

    /**
     * Result of a completed snooker frame.
     */
    public enum FrameResult {
        /** Player scored more points — player wins. */
        PLAYER_WIN,
        /** NPC scored more points — player loses. */
        NPC_WIN,
        /** Equal points — tie (rental refunded, no prize). */
        TIE,
        /** Player cannot afford the rental cost. */
        INSUFFICIENT_FUNDS,
        /** Hall is closed outside opening hours. */
        CLOSED,
        /** Player is currently banned by Dennis. */
        BANNED
    }

    // ── Hustle result ─────────────────────────────────────────────────────────

    /**
     * Result of a hustle attempt.
     */
    public enum HustleResult {
        /** Hustle not yet set up — still deliberate-missing. */
        SETUP_IN_PROGRESS,
        /** Hustle completed successfully — player wins multiplied bet. */
        HUSTLE_WIN,
        /** Hustle detected — player loses bet, NPC becomes hostile. */
        HUSTLE_DETECTED,
        /** Player cannot afford to place the bet. */
        INSUFFICIENT_FUNDS,
        /** Hustle already detected or not active. */
        NOT_AVAILABLE
    }

    // ── Card game result ──────────────────────────────────────────────────────

    /**
     * Result of a back-room pontoon game.
     */
    public enum CardGameResult {
        /** Player won — receives payout up to {@link #BACK_ROOM_MAX_PAYOUT} COIN. */
        WIN,
        /** Player lost — entry fee forfeited. */
        LOSS,
        /** Player cheated and was caught — Notoriety/Respect penalties applied. */
        CAUGHT_CHEATING,
        /** Player cannot afford the entry cost. */
        INSUFFICIENT_FUNDS,
        /** MARCHETTI_CREW Respect below {@link #BACK_ROOM_RESPECT_THRESHOLD}. */
        ACCESS_DENIED,
        /** Already played the card game today. */
        ALREADY_PLAYED_TODAY,
        /** Hall is closed. */
        CLOSED
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Dennis the Proprietor NPC, or null if not spawned. */
    private NPC dennisNpc;

    /** Frank the Hustler NPC, or null if not spawned. */
    private NPC frankNpc;

    /** One-Armed Carl NPC, or null if not spawned. */
    private NPC carlNpc;

    /** Whether the player is currently banned. */
    private boolean playerBanned;

    /** In-game day the ban expires (player re-admitted next day after banDay). */
    private int banDay = -1;

    /** Number of times the player has beaten Frank this session. */
    private int frankWinCount;

    /** Whether the BEAT_FRANK_RESPECT boost has been given this session. */
    private boolean frankRespectAwarded;

    /** Number of deliberate misses the player has accumulated toward the hustle. */
    private int hustleMissCount;

    /** Current hustle bet amount (0 = no active hustle). */
    private int hustleBet;

    /** Whether the hustle is currently active (setup complete, bet placed). */
    private boolean hustleActive;

    /** Whether the back-room card game has been played today. */
    private boolean cardGamePlayedToday;

    /** In-game day when cardGamePlayedToday was last set. */
    private int cardGameLastDay = -1;

    /** Whether FIRST_FRAME achievement has been awarded. */
    private boolean firstFrameAwarded;

    /** Whether CHALK_AND_TALK achievement has been awarded. */
    private boolean chalkAwardedThisSession;

    /** Current CUE weapon hit counter (remaining hits before break). */
    private int cueHitsRemaining = CUE_BREAK_HITS;

    // ── System references ─────────────────────────────────────────────────────

    private FactionSystem factionSystem;
    private NotorietySystem notorietySystem;
    private RumourNetwork rumourNetwork;

    /** Random instance for all probabilistic mechanics. */
    private final Random random;

    // ── Constructor ───────────────────────────────────────────────────────────

    public SnookerSystem(Random random) {
        this.random = random;
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    // ── NPC management ────────────────────────────────────────────────────────

    public void setDennisNpc(NPC npc) {
        this.dennisNpc = npc;
    }

    public NPC getDennisNpc() {
        return dennisNpc;
    }

    public void setFrankNpc(NPC npc) {
        this.frankNpc = npc;
    }

    public NPC getFrankNpc() {
        return frankNpc;
    }

    public void setCarlNpc(NPC npc) {
        this.carlNpc = npc;
    }

    public NPC getCarlNpc() {
        return carlNpc;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Call once per frame. Resets the daily card game flag on a new day.
     *
     * @param currentDay  current in-game day index
     * @param currentHour current in-game hour (0–24)
     */
    public void update(int currentDay, float currentHour) {
        // Reset card game flag on a new day
        if (cardGameLastDay >= 0 && currentDay > cardGameLastDay) {
            cardGamePlayedToday = false;
        }

        // Remove ban when the day after banDay has arrived
        if (playerBanned && banDay >= 0 && currentDay > banDay) {
            playerBanned = false;
            banDay = -1;
        }

        // Frank active 12:00–22:00; hide him outside those hours
        if (frankNpc != null) {
            boolean frankActive = currentHour >= 12.0f && currentHour < 22.0f;
            if (!frankActive && frankNpc.getState() != NPCState.FLEEING) {
                frankNpc.setState(NPCState.IDLE);
            }
        }
    }

    // ── Open hours ────────────────────────────────────────────────────────────

    /**
     * Returns true if Cue Zone is open for business.
     *
     * @param currentHour hour of day (0–24)
     */
    public boolean isOpen(float currentHour) {
        return currentHour >= OPEN_HOUR && currentHour < CLOSE_HOUR;
    }

    // ── Snooker frame ─────────────────────────────────────────────────────────

    /**
     * Play a full snooker frame against an NPC opponent using simulated timing-bar shots.
     *
     * <p>Each of the {@link #FRAME_SCORING_EVENTS} scoring events is resolved by drawing
     * a random float [0, 1) for both the player and the NPC. The drawn value is compared
     * against the timing zones. Points are accumulated; the player with more points wins.
     *
     * @param timingValues   pre-determined player timing values [0,1) for each event;
     *                       length must equal {@link #FRAME_SCORING_EVENTS}.
     *                       Pass {@code null} to generate values randomly.
     * @param difficulty     opponent difficulty tier
     * @param inventory      player's inventory (for rental cost / prize coins)
     * @param currentHour    current in-game hour
     * @param currentDay     current in-game day
     * @param achievementCallback for awarding achievements (may be null)
     * @return result of the frame
     */
    public FrameResult playFrame(float[] timingValues,
                                 NpcDifficulty difficulty,
                                 Inventory inventory,
                                 float currentHour,
                                 int currentDay,
                                 NotorietySystem.AchievementCallback achievementCallback) {

        if (!isOpen(currentHour)) return FrameResult.CLOSED;
        if (playerBanned && currentDay <= banDay) return FrameResult.BANNED;
        if (inventory.getItemCount(Material.COIN) < RENTAL_COST) return FrameResult.INSUFFICIENT_FUNDS;

        // Deduct rental
        inventory.removeItem(Material.COIN, RENTAL_COST);

        // Award FIRST_FRAME achievement
        if (!firstFrameAwarded && achievementCallback != null) {
            firstFrameAwarded = true;
            achievementCallback.award(AchievementType.FIRST_FRAME);
        }

        // Simulate frame
        int playerPoints = 0;
        int npcPoints    = 0;

        float npcAccuracy = npcAccuracyFor(difficulty);

        for (int i = 0; i < FRAME_SCORING_EVENTS; i++) {
            // Player shot
            float playerTiming = (timingValues != null && i < timingValues.length)
                    ? timingValues[i]
                    : random.nextFloat();
            playerPoints += pointsForTiming(playerTiming);

            // NPC shot — NPC accuracy determines chance of hitting green zone
            float npcRoll = random.nextFloat();
            playerPoints += 0; // NPC points tracked separately
            npcPoints += npcPointsFor(npcRoll, npcAccuracy);
        }

        // Determine result
        FrameResult result;
        if (playerPoints > npcPoints) {
            result = FrameResult.PLAYER_WIN;
            inventory.addItem(Material.COIN, RENTAL_COST + WIN_PRIZE);
            onPlayerBeatOpponent(difficulty, currentDay, achievementCallback);
        } else if (npcPoints > playerPoints) {
            result = FrameResult.NPC_WIN;
            // Rental already deducted; no refund
        } else {
            result = FrameResult.TIE;
            // Refund rental on tie
            inventory.addItem(Material.COIN, RENTAL_COST);
        }

        return result;
    }

    /**
     * Simplified frame where the caller supplies only the player's total points
     * and the NPC's total points (e.g. computed externally or for testing).
     *
     * @param playerPoints player's total points for the frame
     * @param npcPoints    NPC's total points for the frame
     * @param difficulty   opponent difficulty (for post-win hooks)
     * @param inventory    player's inventory
     * @param currentHour  current in-game hour
     * @param currentDay   current in-game day
     * @param achievementCallback for awarding achievements (may be null)
     * @return result of the frame
     */
    public FrameResult resolveFrame(int playerPoints,
                                    int npcPoints,
                                    NpcDifficulty difficulty,
                                    Inventory inventory,
                                    float currentHour,
                                    int currentDay,
                                    NotorietySystem.AchievementCallback achievementCallback) {

        if (!isOpen(currentHour)) return FrameResult.CLOSED;
        if (playerBanned && currentDay <= banDay) return FrameResult.BANNED;
        if (inventory.getItemCount(Material.COIN) < RENTAL_COST) return FrameResult.INSUFFICIENT_FUNDS;

        inventory.removeItem(Material.COIN, RENTAL_COST);

        if (!firstFrameAwarded && achievementCallback != null) {
            firstFrameAwarded = true;
            achievementCallback.award(AchievementType.FIRST_FRAME);
        }

        FrameResult result;
        if (playerPoints > npcPoints) {
            result = FrameResult.PLAYER_WIN;
            inventory.addItem(Material.COIN, RENTAL_COST + WIN_PRIZE);
            onPlayerBeatOpponent(difficulty, currentDay, achievementCallback);
        } else if (npcPoints > playerPoints) {
            result = FrameResult.NPC_WIN;
        } else {
            result = FrameResult.TIE;
            inventory.addItem(Material.COIN, RENTAL_COST);
        }

        return result;
    }

    // ── Hustle mechanic ───────────────────────────────────────────────────────

    /**
     * Record a deliberate miss toward setting up a hustle.
     * Once {@link #HUSTLE_MISS_COUNT} misses have been made, the hustle is ready.
     *
     * @return true if hustle is now ready to activate (missCount reached threshold)
     */
    public boolean recordDeliberateMiss() {
        if (hustleMissCount < HUSTLE_MISS_COUNT) {
            hustleMissCount++;
        }
        return hustleMissCount >= HUSTLE_MISS_COUNT;
    }

    /**
     * Attempt to place a hustle bet and activate the hustle.
     *
     * @param betAmount  COIN to bet (1–{@link #HUSTLE_BET_MAX})
     * @param inventory  player's inventory
     * @return {@link HustleResult#SETUP_IN_PROGRESS} if misses not yet complete;
     *         {@link HustleResult#INSUFFICIENT_FUNDS} if player cannot cover the bet;
     *         {@link HustleResult#HUSTLE_WIN} or {@link HustleResult#HUSTLE_DETECTED}
     *         once the hustle resolves;
     *         {@link HustleResult#NOT_AVAILABLE} if hustle is already active
     */
    public HustleResult activateHustle(int betAmount, Inventory inventory,
                                        NotorietySystem.AchievementCallback achievementCallback) {
        if (hustleActive) return HustleResult.NOT_AVAILABLE;
        if (hustleMissCount < HUSTLE_MISS_COUNT) return HustleResult.SETUP_IN_PROGRESS;

        int clampedBet = Math.max(1, Math.min(HUSTLE_BET_MAX, betAmount));
        if (inventory.getItemCount(Material.COIN) < clampedBet) return HustleResult.INSUFFICIENT_FUNDS;

        inventory.removeItem(Material.COIN, clampedBet);
        hustleBet = clampedBet;
        hustleActive = true;

        // 25% chance of detection
        boolean detected = random.nextInt(100) < HUSTLE_DETECT_CHANCE;
        if (detected) {
            hustleActive = false;
            hustleMissCount = 0;
            hustleBet = 0;
            if (frankNpc != null) {
                frankNpc.setState(NPCState.FLEEING);
                frankNpc.setSpeechText("Oi — you've been throwing shots! Get out!", 6f);
            }
            return HustleResult.HUSTLE_DETECTED;
        }

        // Hustle succeeds — pay out double the bet
        int payout = clampedBet * HUSTLE_WIN_MULTIPLIER;
        inventory.addItem(Material.COIN, payout);

        hustleActive = false;
        hustleMissCount = 0;
        hustleBet = 0;

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.SNOOKER_HUSTLER);
        }

        return HustleResult.HUSTLE_WIN;
    }

    // ── Back-room card game ───────────────────────────────────────────────────

    /**
     * Attempt to play the back-room pontoon card game.
     *
     * @param useStolenPhone  true if the player attempts to cheat using STOLEN_PHONE
     * @param inventory       player's inventory
     * @param currentHour     current in-game hour
     * @param currentDay      current in-game day
     * @param achievementCallback for awarding achievements (may be null)
     * @return result of the card game attempt
     */
    public CardGameResult playCardGame(boolean useStolenPhone,
                                        Inventory inventory,
                                        float currentHour,
                                        int currentDay,
                                        NotorietySystem.AchievementCallback achievementCallback) {

        if (!isOpen(currentHour)) return CardGameResult.CLOSED;

        // Access gating: MARCHETTI_CREW Respect
        if (factionSystem != null
                && factionSystem.getRespect(Faction.MARCHETTI_CREW) < BACK_ROOM_RESPECT_THRESHOLD) {
            return CardGameResult.ACCESS_DENIED;
        }

        // Daily limit
        if (cardGamePlayedToday && currentDay == cardGameLastDay) {
            return CardGameResult.ALREADY_PLAYED_TODAY;
        }

        if (inventory.getItemCount(Material.COIN) < BACK_ROOM_ENTRY_COST) {
            return CardGameResult.INSUFFICIENT_FUNDS;
        }

        inventory.removeItem(Material.COIN, BACK_ROOM_ENTRY_COST);
        cardGamePlayedToday = true;
        cardGameLastDay = currentDay;

        // Cheat mechanic
        if (useStolenPhone && inventory.getItemCount(Material.STOLEN_PHONE) > 0) {
            boolean caught = random.nextInt(100) < CHEAT_CATCH_CHANCE;
            if (caught) {
                if (notorietySystem != null) {
                    notorietySystem.addNotoriety(CHEAT_CAUGHT_NOTORIETY, achievementCallback);
                }
                if (factionSystem != null) {
                    factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, -CHEAT_CAUGHT_RESPECT_LOSS);
                }
                return CardGameResult.CAUGHT_CHEATING;
            }
            // Cheat succeeds — guaranteed win
            int payout = BACK_ROOM_MAX_PAYOUT;
            inventory.addItem(Material.COIN, payout);
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.BACK_ROOM_WINNER);
            }
            onBackRoomWin();
            return CardGameResult.WIN;
        }

        // Normal play — 50% win chance
        boolean win = random.nextBoolean();
        if (win) {
            int payout = BACK_ROOM_ENTRY_COST + random.nextInt(BACK_ROOM_MAX_PAYOUT - BACK_ROOM_ENTRY_COST + 1);
            inventory.addItem(Material.COIN, payout);
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.BACK_ROOM_WINNER);
            }
            onBackRoomWin();
            return CardGameResult.WIN;
        }

        return CardGameResult.LOSS;
    }

    // ── Brawl response ────────────────────────────────────────────────────────

    /**
     * Called when a brawl occurs inside the hall.
     * Dennis bans the player for {@link #BAN_DURATION_DAYS} day.
     * Applies Respect changes.
     *
     * @param currentDay current in-game day
     */
    public void onBrawlInHall(int currentDay) {
        playerBanned = true;
        banDay = currentDay + BAN_DURATION_DAYS - 1;
        if (dennisNpc != null) {
            dennisNpc.setState(NPCState.FLEEING);
            dennisNpc.setSpeechText("Right, you're barred! Get out — now!", 5f);
        }
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, -BRAWL_MARCHETTI_RESPECT_LOSS);
            factionSystem.applyRespectDelta(Faction.THE_COUNCIL, BRAWL_COUNCIL_RESPECT_GAIN);
        }
    }

    // ── Chalk purchase ────────────────────────────────────────────────────────

    /**
     * Player buys CHALK_CUBE from Dennis for 1 COIN.
     *
     * @param inventory  player's inventory
     * @param achievementCallback for awarding achievements (may be null)
     * @return true if purchase succeeded
     */
    public boolean buyChalk(Inventory inventory,
                            NotorietySystem.AchievementCallback achievementCallback) {
        if (inventory.getItemCount(Material.COIN) < 1) return false;
        inventory.removeItem(Material.COIN, 1);
        inventory.addItem(Material.CHALK_CUBE, 1);
        if (!chalkAwardedThisSession && achievementCallback != null) {
            chalkAwardedThisSession = true;
            achievementCallback.award(AchievementType.CHALK_AND_TALK);
        }
        return true;
    }

    // ── CUE weapon mechanic ───────────────────────────────────────────────────

    /**
     * Resolve a CUE weapon hit in combat.
     * Deals {@link #CUE_DAMAGE} damage. After {@link #CUE_BREAK_HITS} hits,
     * the CUE breaks and drops {@link #CUE_BREAK_WOOD_DROP} WOOD.
     *
     * @param inventory player's inventory (CUE must be present; WOOD added on break)
     * @return true if the CUE is still intact after this hit; false if it broke
     */
    public boolean onCueHit(Inventory inventory) {
        if (inventory.getItemCount(Material.CUE) < 1) return false;
        cueHitsRemaining--;
        if (cueHitsRemaining <= 0) {
            inventory.removeItem(Material.CUE, 1);
            inventory.addItem(Material.WOOD, CUE_BREAK_WOOD_DROP);
            cueHitsRemaining = CUE_BREAK_HITS; // reset for next CUE
            return false; // broke
        }
        return true;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Whether the player is currently banned from Cue Zone. */
    public boolean isPlayerBanned() {
        return playerBanned;
    }

    /** In-game day on which the ban expires (-1 if not banned). */
    public int getBanDay() {
        return banDay;
    }

    /** Number of times the player has beaten Frank. */
    public int getFrankWinCount() {
        return frankWinCount;
    }

    /** Whether the back-room card game has been played today. */
    public boolean isCardGamePlayedToday() {
        return cardGamePlayedToday;
    }

    /** Number of deliberate misses accumulated toward the current hustle. */
    public int getHustleMissCount() {
        return hustleMissCount;
    }

    /** Whether the hustle is currently active. */
    public boolean isHustleActive() {
        return hustleActive;
    }

    /** Remaining hits before the currently held CUE breaks. */
    public int getCueHitsRemaining() {
        return cueHitsRemaining;
    }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /** Force-set the player ban state (for testing). */
    void setBannedForTesting(boolean banned, int day) {
        this.playerBanned = banned;
        this.banDay = day;
    }

    /** Force-set Frank win count (for testing). */
    void setFrankWinCountForTesting(int count) {
        this.frankWinCount = count;
    }

    /** Force-set hustle miss count (for testing). */
    void setHustleMissCountForTesting(int count) {
        this.hustleMissCount = count;
    }

    /** Force-set card game played today (for testing). */
    void setCardGamePlayedTodayForTesting(boolean played, int day) {
        this.cardGamePlayedToday = played;
        this.cardGameLastDay = day;
    }

    /** Force-set CUE hits remaining (for testing). */
    void setCueHitsRemainingForTesting(int hits) {
        this.cueHitsRemaining = hits;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void onPlayerBeatOpponent(NpcDifficulty difficulty,
                                       int currentDay,
                                       NotorietySystem.AchievementCallback achievementCallback) {
        if (difficulty == NpcDifficulty.HUSTLER) {
            frankWinCount++;
            if (!frankRespectAwarded && frankWinCount >= FRANK_WIN_THRESHOLD) {
                frankRespectAwarded = true;
                if (factionSystem != null) {
                    factionSystem.applyRespectDelta(Faction.STREET_LADS, BEAT_FRANK_RESPECT_GAIN);
                }
                seedShopNewsRumour();
            }
        }

        if (difficulty == NpcDifficulty.LEGEND && achievementCallback != null) {
            achievementCallback.award(AchievementType.SNOOKER_LEGEND);
        }
    }

    private void onBackRoomWin() {
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, BACK_ROOM_WIN_RESPECT_GAIN);
        }
        seedGangActivityRumour();
    }

    private void seedShopNewsRumour() {
        if (rumourNetwork == null) return;
        seedRumour(RumourType.SHOP_NEWS,
                "Word is some lad's been giving Frank a proper hiding up at the snooker hall.");
    }

    private void seedGangActivityRumour() {
        if (rumourNetwork == null) return;
        seedRumour(RumourType.GANG_ACTIVITY,
                "Someone walked out of the back room at Cue Zone with a serious amount of coin.");
    }

    private void seedRumour(RumourType type, String text) {
        if (rumourNetwork == null) return;
        // Seed to Dennis if available, otherwise no spreading NPC
        if (dennisNpc != null && dennisNpc.isAlive()) {
            rumourNetwork.addRumour(dennisNpc, new Rumour(type, text));
        }
    }

    private float npcAccuracyFor(NpcDifficulty difficulty) {
        switch (difficulty) {
            case NOVICE:  return 0.40f;
            case HUSTLER: return 0.60f;
            case LEGEND:  return 0.80f;
            default:      return 0.40f;
        }
    }

    private int npcPointsFor(float roll, float accuracy) {
        if (roll < accuracy * TIMING_GREEN_MAX / TIMING_GREEN_MAX) {
            // Simplified: if roll < accuracy → score green or yellow
            if (roll < accuracy * 0.5f) return POINTS_GREEN;
            if (roll < accuracy)        return POINTS_YELLOW;
        }
        return POINTS_RED;
    }

    /**
     * Points awarded for a timing value in [0, 1).
     *
     * @param timing float in [0, 1) — lower is better
     * @return {@link #POINTS_GREEN}, {@link #POINTS_YELLOW}, or {@link #POINTS_RED}
     */
    public static int pointsForTiming(float timing) {
        if (timing < TIMING_GREEN_MAX)  return POINTS_GREEN;
        if (timing < TIMING_YELLOW_MAX) return POINTS_YELLOW;
        return POINTS_RED;
    }
}

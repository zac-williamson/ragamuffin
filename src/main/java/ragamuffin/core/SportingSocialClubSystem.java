package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.Faction;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1288: Northfield Sporting &amp; Social Club — Darts Night, Thursday Quiz,
 * Back-Room Pontoon &amp; the Protection Handover.
 *
 * <p>The Northfield Sporting &amp; Social Club is a quintessential British members' club
 * — sticky carpet, faded trophies, cheap bitter, and decades of barely-suppressed
 * resentment simmering under a veneer of committee civility.
 *
 * <h3>Activities</h3>
 * <ul>
 *   <li><b>Darts Night</b> (Fri/Sat 18:00–23:00) — 501 mini-game vs Brian; 5 COIN wager;
 *       DARTS_SET on first win; DARTS XP +1/game, +2/win.</li>
 *   <li><b>Thursday Quiz Night</b> (Thu 19:30–22:00) — 5 rounds, 15 questions, 10 COIN prize;
 *       cheat via stolen QUIZ_ANSWER_SHEET (30% detection risk).</li>
 *   <li><b>Back-Room Pontoon</b> (Fri/Sat 22:00–01:00) — Mick cheats; detectable with FENCE ≥ 10;
 *       seeds CARD_CHEAT rumour; reduces MARCHETTI_CREW Respect −5.</li>
 *   <li><b>Protection Envelope</b> (Sun 20:00–21:30) — Marchetti Enforcer collects 20 COIN from
 *       Terry; player can steal (Wanted +2), plant decoy, or grass to police (SNITCH rumour).</li>
 * </ul>
 *
 * <h3>Membership</h3>
 * <ul>
 *   <li>Ron ({@link NPCType#SOCIAL_CLUB_STEWARD}) sells {@link Material#CLUB_MEMBERSHIP_CARD}
 *       for 5 COIN. Card required for bar, darts, quiz, and pontoon.</li>
 * </ul>
 *
 * <h3>Integration</h3>
 * <ul>
 *   <li>{@link FactionSystem} — MARCHETTI_CREW Respect ±5/−20/−30; STREET_LADS Respect ≥ 50
 *       unlocks Ron insider dialogue.</li>
 *   <li>{@link RumourNetwork} — CARD_CHEAT, COMMITTEE_CONSPIRACY, POLICE_SNITCH.</li>
 *   <li>{@link NotorietySystem} — quiz cheat caught +2; envelope theft +3; brawl +5.</li>
 *   <li>{@link WantedSystem} — envelope theft +2; enforcer assault +3.</li>
 *   <li>{@link CriminalRecord} — THEFT on envelope stolen; CHEATING_AT_PUB_QUIZ if quiz cheat caught.</li>
 *   <li>{@link StreetSkillSystem} — DARTS XP +1/game, +2/win; FENCE XP +1 on detecting cheat.</li>
 *   <li>{@link NeighbourhoodSystem} — COMMITTEE_CONSPIRACY rumour vibes +3; protection exposed +5;
 *       enforcer brawl −4.</li>
 *   <li>{@link NewspaperSystem} — headline when conspiracy spreads to 5+ NPCs.</li>
 *   <li>{@link HealingSystem} — pint purchase restores +2 HP.</li>
 * </ul>
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@link AchievementType#DARTS_HUSTLER_CLUB} — win 5 darts matches at the social club.</li>
 *   <li>{@link AchievementType#PONTOON_KING} — win 20 COIN net at pontoon over career.</li>
 *   <li>{@link AchievementType#BACK_ROOM_WINNER} — detect Mick cheating and survive.</li>
 *   <li>{@link AchievementType#AGM_TROUBLEMAKER} — successfully move "Investigate the accounts".</li>
 *   <li>{@link AchievementType#QUIZ_CHAMPION} — win quiz with 12+ points.</li>
 *   <li>{@link AchievementType#FULL_MEMBER} — buy Club Membership Card.</li>
 * </ul>
 */
public class SportingSocialClubSystem {

    // ── Day-of-week constants (0=Mon … 6=Sun) ─────────────────────────────────

    /** Thursday — Quiz Night. */
    public static final int DAY_THURSDAY = 3;

    /** Friday — Darts Night + Back-Room Pontoon (late). */
    public static final int DAY_FRIDAY = 4;

    /** Saturday — Darts Night + Back-Room Pontoon. */
    public static final int DAY_SATURDAY = 5;

    /** Sunday — Protection Envelope handover. */
    public static final int DAY_SUNDAY = 6;

    // ── Hours ─────────────────────────────────────────────────────────────────

    /** Darts Night start hour. */
    public static final float DARTS_NIGHT_START = 18.0f;

    /** Darts Night end hour. */
    public static final float DARTS_NIGHT_END = 23.0f;

    /** Quiz Night start hour. */
    public static final float QUIZ_START_HOUR = 19.5f;  // 19:30

    /** Quiz Night end hour. */
    public static final float QUIZ_END_HOUR = 22.0f;

    /** Quiz answer sheet available from noticeboard (setup window). */
    public static final float QUIZ_SHEET_WINDOW_START = 19.0f;

    /** Quiz answer sheet window closes when Maureen collects it. */
    public static final float QUIZ_SHEET_WINDOW_END = 19.5f;

    /** Back-Room Pontoon start hour. */
    public static final float PONTOON_START_HOUR = 22.0f;

    /** Back-Room Pontoon end hour (next day 01:00). */
    public static final float PONTOON_END_HOUR = 1.0f;

    /** Protection Envelope: Terry places envelope. */
    public static final float ENVELOPE_PLACED_HOUR = 20.0f;

    /** Protection Envelope: Enforcer arrives. */
    public static final float ENFORCER_ARRIVE_HOUR = 20.25f;  // 20:15

    /** Protection Envelope: Enforcer leaves by this hour. */
    public static final float ENFORCER_LEAVE_HOUR = 21.5f;

    // ── Prices ────────────────────────────────────────────────────────────────

    /** Membership card cost (COIN). */
    public static final int MEMBERSHIP_COST = 5;

    /** Darts wager (COIN). */
    public static final int DARTS_WAGER = 5;

    /** Quiz entry fee (COIN). */
    public static final int QUIZ_ENTRY_FEE = 2;

    /** Quiz win prize (COIN). */
    public static final int QUIZ_PRIZE = 10;

    /** Quiz split prize when tied (COIN). */
    public static final int QUIZ_SPLIT_PRIZE = 5;

    /** Protection envelope amount (COIN). */
    public static final int PROTECTION_ENVELOPE_COIN = 20;

    /** Minimum pontoon bet (COIN). */
    public static final int PONTOON_MIN_BET = 2;

    /** Maximum pontoon bet (COIN). */
    public static final int PONTOON_MAX_BET = 10;

    /** Bitter price (COIN). */
    public static final int DRINK_PRICE = 1;

    /** HP restored by a pint (HealingSystem integration). */
    public static final float PINT_HEAL_HP = 2.0f;

    // ── Quiz settings ─────────────────────────────────────────────────────────

    /** Total quiz questions (5 rounds × 3 questions). */
    public static final int QUIZ_TOTAL_QUESTIONS = 15;

    /** Score needed to win the quiz outright. */
    public static final int QUIZ_WIN_SCORE = 12;

    /** Chance Maureen detects quiz cheat (30%). */
    public static final float QUIZ_CHEAT_DETECTION_CHANCE = 0.30f;

    /** Notoriety penalty if caught cheating at quiz. */
    public static final int QUIZ_CHEAT_NOTORIETY = 2;

    /** Days the player is banned from quiz after being caught cheating. */
    public static final int QUIZ_CHEAT_BAN_DAYS = 7;

    /** Fine (COIN) for being caught cheating at quiz. */
    public static final int QUIZ_CHEAT_FINE = 5;

    // ── Darts XP ─────────────────────────────────────────────────────────────

    /** DARTS XP awarded per game played. */
    public static final int DARTS_XP_PER_GAME = 1;

    /** DARTS XP bonus per win. */
    public static final int DARTS_XP_PER_WIN = 2;

    /** Darts wins required for DARTS_HUSTLER_CLUB achievement. */
    public static final int DARTS_WINS_FOR_ACHIEVEMENT = 5;

    // ── Pontoon settings ──────────────────────────────────────────────────────

    /** FENCE skill level required to detect Mick cheating. */
    public static final int FENCE_SKILL_DETECT_CHEAT = 10;

    /** MARCHETTI_CREW Respect penalty for detecting Mick. */
    public static final int RESPECT_PENALTY_DETECT_MICK = 5;

    /** Cumulative pontoon net-win threshold for PONTOON_KING achievement. */
    public static final int PONTOON_KING_WIN_THRESHOLD = 20;

    // ── Protection Envelope ───────────────────────────────────────────────────

    /** Notoriety gained from stealing envelope. */
    public static final int ENVELOPE_THEFT_NOTORIETY = 3;

    /** Wanted stars from stealing envelope. */
    public static final int ENVELOPE_THEFT_WANTED = 2;

    /** MARCHETTI_CREW Respect penalty for stealing envelope. */
    public static final int RESPECT_PENALTY_ENVELOPE_THEFT = 20;

    /** MARCHETTI_CREW Respect penalty for grassing on protection handover. */
    public static final int RESPECT_PENALTY_GRASS = 30;

    /** Days player is banned from social club after grassing. */
    public static final int GRASS_BAN_DAYS = 14;

    // ── AGM settings ──────────────────────────────────────────────────────────

    /** AGM bribe offer (COIN). */
    public static final int AGM_BRIBE_COIN = 10;

    /** MARCHETTI_CREW Respect bonus for accepting AGM bribe. */
    public static final int RESPECT_BONUS_ACCEPT_BRIBE = 5;

    /** NeighbourhoodSystem vibes boost when committee conspiracy exposed. */
    public static final int VIBES_CONSPIRACY_EXPOSED = 3;

    /** NeighbourhoodSystem vibes boost when protection payment exposed. */
    public static final int VIBES_PROTECTION_EXPOSED = 5;

    /** NeighbourhoodSystem vibes penalty when enforcer brawl occurs. */
    public static final int VIBES_ENFORCER_BRAWL = -4;

    // ── WarmthSystem ──────────────────────────────────────────────────────────

    /** Warmth added per in-game minute while inside the social club. */
    public static final float WARMTH_RATE_PER_MINUTE = 1.0f;

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Result of attempting to buy a Club Membership Card. */
    public enum MembershipResult {
        SUCCESS,
        ALREADY_MEMBER,
        INSUFFICIENT_FUNDS,
        RON_NOT_PRESENT
    }

    /** Result of attempting to buy a drink from Keith. */
    public enum DrinkResult {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        REFUSED_WANTED,
        REFUSED_TRACKSUIT,
        BAR_CLOSED
    }

    /** Result of a darts game. */
    public enum DartsResult {
        PLAYER_WIN,
        PLAYER_LOSE,
        GAME_NOT_ACTIVE,
        NOT_DARTS_NIGHT
    }

    /** Result of answering a quiz question. */
    public enum QuizAnswerResult {
        CORRECT,
        INCORRECT,
        EJECTED,
        QUIZ_NOT_ACTIVE,
        ALREADY_ANSWERED
    }

    /** Result of stealing the protection envelope. */
    public enum EnvelopeResult {
        STOLEN,
        ENFORCER_ALREADY_COLLECTED,
        ENVELOPE_NOT_PLACED,
        NOT_PROTECTION_WINDOW
    }

    /** Result of grassing on the protection handover. */
    public enum GrassResult {
        SUCCESS,
        TOO_LATE,
        NOT_PROTECTION_DAY
    }

    /** Result of detecting Mick's cheating at pontoon. */
    public enum CheatDetectResult {
        CHEAT_DETECTED,
        SKILL_TOO_LOW,
        NOT_IN_BACK_ROOM,
        MICK_NOT_PRESENT
    }

    /** AGM motion choices for the "Investigate the accounts" scenario. */
    public enum AGMMotionResult {
        MOTION_PASSED,
        MOTION_REJECTED,
        BRIBE_ACCEPTED,
        BRIBE_REFUSED,
        AGM_NOT_ACTIVE
    }

    // ── Internal state ────────────────────────────────────────────────────────

    private final Random rng;

    // Membership
    private boolean playerIsMember = false;
    private int clubBanDaysRemaining = 0;  // Days banned from club (grassing)
    private int quizBanDaysRemaining = 0;  // Days banned from quiz (cheating)

    // Darts
    private boolean dartsNightActive = false;
    private boolean dartsGameActive = false;
    private int dartsWinsAtClub = 0;
    private boolean dartsSetAwarded = false;
    private int dartsPlayerScore = 501;
    private int dartsNpcScore = 501;
    private boolean dartsPlayerTurn = true;

    // Quiz
    private boolean quizNightActive = false;
    private int quizScore = 0;
    private int questionsAnswered = 0;
    private boolean quizComplete = false;
    private boolean playerEjectedFromQuiz = false;
    private String quizLog = "";

    // Pontoon / back room
    private boolean backRoomPontoonActive = false;
    private boolean mickCheating = true;  // Mick always cheats
    private int pontoonNetWin = 0;        // Career net win

    // Protection envelope
    private boolean envelopePlaced = false;
    private boolean envelopeStolen = false;
    private boolean enforcerPresent = false;
    private boolean enforcerArrested = false;
    private boolean playerGrassed = false;

    // AGM
    private boolean agmActive = false;
    private boolean agmMotionPassed = false;
    private boolean bribeOffered = false;

    // Pending newspaper headline (polled by game loop)
    private String pendingHeadline = null;

    // Injected systems
    private FactionSystem factionSystem;
    private RumourNetwork rumourNetwork;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private StreetSkillSystem streetSkillSystem;
    private NeighbourhoodSystem neighbourhoodSystem;
    private NewspaperSystem newspaperSystem;
    private AchievementSystem achievementSystem;
    private NotorietySystem.AchievementCallback achievementCallback;

    // ── Constructors ──────────────────────────────────────────────────────────

    public SportingSocialClubSystem() {
        this(new Random());
    }

    public SportingSocialClubSystem(Random rng) {
        this.rng = rng;
    }

    // ── Dependency injection ──────────────────────────────────────────────────

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setStreetSkillSystem(StreetSkillSystem streetSkillSystem) {
        this.streetSkillSystem = streetSkillSystem;
    }

    public void setNeighbourhoodSystem(NeighbourhoodSystem neighbourhoodSystem) {
        this.neighbourhoodSystem = neighbourhoodSystem;
    }

    public void setNewspaperSystem(NewspaperSystem newspaperSystem) {
        this.newspaperSystem = newspaperSystem;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
        this.achievementCallback = type -> achievementSystem.unlock(type);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Per-frame update. Call once per game loop with current time and relevant state.
     *
     * @param delta      frame delta (seconds)
     * @param hour       current in-game hour (0.0–24.0)
     * @param dayOfWeek  0=Mon … 6=Sun
     */
    public void update(float delta, float hour, int dayOfWeek) {
        // Darts Night: Fri/Sat 18:00–23:00
        dartsNightActive = (dayOfWeek == DAY_FRIDAY || dayOfWeek == DAY_SATURDAY)
                && hour >= DARTS_NIGHT_START && hour < DARTS_NIGHT_END;

        // Quiz Night: Thu 19:30–22:00
        quizNightActive = (dayOfWeek == DAY_THURSDAY)
                && hour >= QUIZ_START_HOUR && hour < QUIZ_END_HOUR;

        // Pontoon: Fri/Sat 22:00 onwards (wraps past midnight at 01:00)
        backRoomPontoonActive = (dayOfWeek == DAY_FRIDAY || dayOfWeek == DAY_SATURDAY)
                && (hour >= PONTOON_START_HOUR || hour < PONTOON_END_HOUR);

        // Protection envelope events: Sunday
        if (dayOfWeek == DAY_SUNDAY) {
            if (hour >= ENVELOPE_PLACED_HOUR && !envelopePlaced && !envelopeStolen) {
                envelopePlaced = true;
            }
            if (hour >= ENFORCER_ARRIVE_HOUR && envelopePlaced && !envelopeStolen
                    && !enforcerPresent && !playerGrassed) {
                enforcerPresent = true;
            }
        }
    }

    // ── Membership ────────────────────────────────────────────────────────────

    /**
     * Returns whether the player can enter the main bar (has membership card and is not banned).
     */
    public boolean canEnterBar(Inventory playerInventory) {
        if (clubBanDaysRemaining > 0) return false;
        return playerInventory != null && playerInventory.hasItem(Material.CLUB_MEMBERSHIP_CARD);
    }

    /**
     * Attempt to buy a Club Membership Card from Ron.
     *
     * @param playerInventory player's inventory
     * @param ron             Ron NPC (must be SOCIAL_CLUB_STEWARD)
     * @return result of the purchase
     */
    public MembershipResult buyMembership(Inventory playerInventory, NPC ron) {
        if (ron == null || ron.getType() != NPCType.SOCIAL_CLUB_STEWARD) {
            return MembershipResult.RON_NOT_PRESENT;
        }
        if (playerInventory.hasItem(Material.CLUB_MEMBERSHIP_CARD)) {
            return MembershipResult.ALREADY_MEMBER;
        }
        if (playerInventory.getItemCount(Material.COIN) < MEMBERSHIP_COST) {
            return MembershipResult.INSUFFICIENT_FUNDS;
        }
        playerInventory.removeItem(Material.COIN, MEMBERSHIP_COST);
        playerInventory.addItem(Material.CLUB_MEMBERSHIP_CARD, 1);
        playerIsMember = true;

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.FULL_MEMBER);
        }
        return MembershipResult.SUCCESS;
    }

    /** Returns whether the player is flagged as a member internally. */
    public boolean isMember() {
        return playerIsMember;
    }

    /**
     * Returns whether the player can enter the social club (not banned).
     */
    public boolean canEnterSocialClub(Inventory playerInventory) {
        if (clubBanDaysRemaining > 0) return false;
        return playerInventory != null && playerInventory.hasItem(Material.CLUB_MEMBERSHIP_CARD);
    }

    // ── Bar service ───────────────────────────────────────────────────────────

    /**
     * Attempt to buy a drink from Keith.
     *
     * @param playerInventory player's inventory
     * @param wantedStars     current wanted stars (Tier 2+ refused)
     * @param wearingTracksuit true if player wears KNOCK_OFF_TRACKSUIT
     * @param currentHour     in-game hour (bar open 12:00–23:00)
     * @return result of the purchase
     */
    public DrinkResult tryBuyDrink(Inventory playerInventory, int wantedStars,
                                   boolean wearingTracksuit, float currentHour) {
        if (currentHour < 12.0f || currentHour >= 23.0f) {
            return DrinkResult.BAR_CLOSED;
        }
        if (wantedStars >= 2) {
            return DrinkResult.REFUSED_WANTED;
        }
        if (wearingTracksuit) {
            return DrinkResult.REFUSED_TRACKSUIT;
        }
        if (playerInventory.getItemCount(Material.COIN) < DRINK_PRICE) {
            return DrinkResult.INSUFFICIENT_FUNDS;
        }
        playerInventory.removeItem(Material.COIN, DRINK_PRICE);
        return DrinkResult.SUCCESS;
    }

    // ── Darts Night ───────────────────────────────────────────────────────────

    /**
     * Checks whether Darts Night is currently active.
     */
    public boolean isDartsNightActive() {
        return dartsNightActive;
    }

    /**
     * Force Darts Night active (for testing).
     */
    public void forceDartsNightActive(boolean active) {
        this.dartsNightActive = active;
    }

    /**
     * Start a darts game against Brian. Player must have CLUB_MEMBERSHIP_CARD
     * and must have sufficient COIN for the wager.
     *
     * @param playerInventory player's inventory
     * @param brian           the Brian NPC (CLUB_REGULAR)
     * @return true if game started successfully
     */
    public boolean startDartsGame(Inventory playerInventory, NPC brian) {
        if (!canEnterBar(playerInventory)) return false;
        if (playerInventory.getItemCount(Material.COIN) < DARTS_WAGER) return false;
        playerInventory.removeItem(Material.COIN, DARTS_WAGER);
        dartsGameActive = true;
        dartsPlayerScore = 501;
        dartsNpcScore = 501;
        dartsPlayerTurn = true;

        // Award DARTS XP per game
        if (streetSkillSystem != null) {
            streetSkillSystem.awardXP(StreetSkillSystem.Skill.DARTS, DARTS_XP_PER_GAME);
        }
        return true;
    }

    /**
     * Play a darts game against Brian (simplified: single-turn resolution using RNG).
     * Player wins if they have higher RNG roll, modified by DARTS skill bonus.
     *
     * @param playerInventory player's inventory
     * @param brian           the Brian NPC (CLUB_REGULAR)
     * @return result of the darts game
     */
    public DartsResult playDarts(Inventory playerInventory, NPC brian) {
        if (!dartsNightActive && !dartsGameActive) {
            // Try to start if not started
            if (!startDartsGame(playerInventory, brian)) {
                return DartsResult.NOT_DARTS_NIGHT;
            }
        }

        if (!dartsGameActive && !startDartsGame(playerInventory, brian)) {
            return DartsResult.GAME_NOT_ACTIVE;
        }
        dartsGameActive = false;

        int dartsBonus = 0;
        if (streetSkillSystem != null) {
            dartsBonus = streetSkillSystem.getDartsAccuracyBonus();
        }
        boolean hasSet = playerInventory != null
                && playerInventory.hasItem(Material.DARTS_SET);
        if (hasSet) dartsBonus += 1;

        // Resolve: player wins if roll + bonus > threshold
        int playerRoll = rng.nextInt(10) + dartsBonus;
        int npcRoll = rng.nextInt(10);

        if (playerRoll >= npcRoll) {
            // Player wins
            playerInventory.addItem(Material.COIN, DARTS_WAGER * 2); // get wager back + Brian's stake
            dartsWinsAtClub++;

            // Award DARTS_SET on first win
            if (!dartsSetAwarded) {
                dartsSetAwarded = true;
                playerInventory.addItem(Material.DARTS_SET, 1);
            }

            // DARTS XP per win
            if (streetSkillSystem != null) {
                streetSkillSystem.awardXP(StreetSkillSystem.Skill.DARTS, DARTS_XP_PER_WIN);
            }

            // DARTS_HUSTLER_CLUB achievement after 5 wins
            if (dartsWinsAtClub >= DARTS_WINS_FOR_ACHIEVEMENT && achievementSystem != null) {
                achievementSystem.unlock(AchievementType.DARTS_HUSTLER_CLUB);
            }
            return DartsResult.PLAYER_WIN;
        } else {
            // NPC wins
            return DartsResult.PLAYER_LOSE;
        }
    }

    /** Returns total darts wins at the social club. */
    public int getDartsWinsAtClub() {
        return dartsWinsAtClub;
    }

    /** Returns whether the DARTS_SET has been awarded to the player. */
    public boolean isDartsSetAwarded() {
        return dartsSetAwarded;
    }

    // ── Thursday Quiz Night ───────────────────────────────────────────────────

    /**
     * Returns whether Quiz Night is currently active.
     */
    public boolean isQuizNightActive() {
        return quizNightActive;
    }

    /**
     * Force Quiz Night active (for testing).
     */
    public void forceQuizNightActive(boolean active) {
        this.quizNightActive = active;
        if (active) {
            quizScore = 0;
            questionsAnswered = 0;
            quizComplete = false;
            playerEjectedFromQuiz = false;
            quizLog = "";
        }
    }

    /**
     * Start a quiz night session. Player must have CLUB_MEMBERSHIP_CARD and pay 2 COIN entry.
     *
     * @param playerInventory player's inventory
     * @param maureen         the Maureen NPC (QUIZ_HOST)
     * @return true if quiz started
     */
    public boolean startQuizNight(Inventory playerInventory, NPC maureen) {
        if (quizBanDaysRemaining > 0) return false;
        if (!canEnterBar(playerInventory)) return false;
        if (playerInventory.getItemCount(Material.COIN) < QUIZ_ENTRY_FEE) return false;
        playerInventory.removeItem(Material.COIN, QUIZ_ENTRY_FEE);
        quizNightActive = true;
        quizScore = 0;
        questionsAnswered = 0;
        quizComplete = false;
        playerEjectedFromQuiz = false;
        quizLog = "";
        return true;
    }

    /**
     * Answer a quiz question. If player has QUIZ_ANSWER_SHEET, answer is guaranteed correct
     * but 30% chance of detection.
     *
     * @param playerInventory player's inventory
     * @param answerIndex     player's chosen answer index (0-based)
     * @param correctIndex    correct answer index (0-based)
     * @return result of the answer
     */
    public QuizAnswerResult answerQuestion(Inventory playerInventory,
                                           int answerIndex, int correctIndex) {
        if (!quizNightActive) return QuizAnswerResult.QUIZ_NOT_ACTIVE;
        if (playerEjectedFromQuiz) return QuizAnswerResult.EJECTED;
        if (questionsAnswered >= QUIZ_TOTAL_QUESTIONS) return QuizAnswerResult.ALREADY_ANSWERED;

        boolean hasSheet = playerInventory != null
                && playerInventory.hasItem(Material.QUIZ_ANSWER_SHEET);

        if (hasSheet) {
            // Using cheat sheet — check detection
            float detectionRoll = rng.nextFloat();
            if (detectionRoll < QUIZ_CHEAT_DETECTION_CHANCE) {
                // Caught cheating
                playerEjectedFromQuiz = true;
                quizBanDaysRemaining = QUIZ_CHEAT_BAN_DAYS;
                // Fine
                int fine = Math.min(QUIZ_CHEAT_FINE,
                        playerInventory.getItemCount(Material.COIN));
                if (fine > 0) playerInventory.removeItem(Material.COIN, fine);
                // Notoriety
                if (notorietySystem != null) {
                    notorietySystem.addNotoriety(QUIZ_CHEAT_NOTORIETY, achievementCallback);
                }
                // Criminal record
                if (criminalRecord != null) {
                    criminalRecord.record(CriminalRecord.CrimeType.CHEATING_AT_PUB_QUIZ);
                }
                quizLog = "ejected";
                return QuizAnswerResult.EJECTED;
            }
            // Not detected — guaranteed correct
            quizScore++;
            questionsAnswered++;
            quizLog = "correct (cheat)";
            checkQuizComplete(playerInventory);
            return QuizAnswerResult.CORRECT;
        }

        // Normal answer
        questionsAnswered++;
        if (answerIndex == correctIndex) {
            quizScore++;
            quizLog = "correct";
            checkQuizComplete(playerInventory);
            return QuizAnswerResult.CORRECT;
        } else {
            quizLog = "incorrect";
            checkQuizComplete(playerInventory);
            return QuizAnswerResult.INCORRECT;
        }
    }

    private void checkQuizComplete(Inventory playerInventory) {
        if (questionsAnswered >= QUIZ_TOTAL_QUESTIONS) {
            quizComplete = true;
            quizNightActive = false;
            if (quizScore >= QUIZ_WIN_SCORE) {
                // Player wins
                playerInventory.addItem(Material.COIN, QUIZ_PRIZE);
                quizLog = "winner announced";
                if (achievementSystem != null) {
                    achievementSystem.unlock(AchievementType.QUIZ_CHAMPION);
                }
                if (rumourNetwork != null) {
                    NPC fakeHost = new NPC(NPCType.QUIZ_HOST, 0, 0, 0);
                    rumourNetwork.addRumour(fakeHost,
                            new Rumour(RumourType.LOCAL_EVENT,
                                    "Quiz Night at the social club — someone actually won."));
                }
            }
        }
    }

    /** Returns the current quiz score. */
    public int getQuizScore() {
        return quizScore;
    }

    /** Returns whether the quiz is complete. */
    public boolean isQuizComplete() {
        return quizComplete;
    }

    /** Returns the quiz event log (last notable event). */
    public String getQuizLog() {
        return quizLog;
    }

    /** Returns whether the player was ejected from the quiz. */
    public boolean isPlayerEjectedFromQuiz() {
        return playerEjectedFromQuiz;
    }

    /** Returns whether the player is banned from the quiz. */
    public boolean isQuizBanned() {
        return quizBanDaysRemaining > 0;
    }

    // ── Back-Room Pontoon ─────────────────────────────────────────────────────

    /**
     * Returns whether the back-room pontoon is currently active.
     */
    public boolean isBackRoomPontoonActive() {
        return backRoomPontoonActive;
    }

    /**
     * Detect Mick's cheating at pontoon. Requires FENCE skill ≥ 10.
     *
     * @param playerInventory player's inventory (unused directly but required for context)
     * @param mick            Mick NPC (CARD_DEALER)
     * @param npcs            all NPCs in scene (to find Enforcer)
     * @return detection result
     */
    public CheatDetectResult detectCheating(Inventory playerInventory, NPC mick, List<NPC> npcs) {
        if (mick == null || mick.getType() != NPCType.CARD_DEALER) {
            return CheatDetectResult.MICK_NOT_PRESENT;
        }
        if (!backRoomPontoonActive) {
            return CheatDetectResult.NOT_IN_BACK_ROOM;
        }
        int fenceLevel = 0;
        if (streetSkillSystem != null) {
            fenceLevel = streetSkillSystem.getSkillLevel(StreetSkillSystem.Skill.TRADING);
        }
        if (fenceLevel < FENCE_SKILL_DETECT_CHEAT) {
            return CheatDetectResult.SKILL_TOO_LOW;
        }

        // Cheat detected!
        if (streetSkillSystem != null) {
            streetSkillSystem.awardXP(StreetSkillSystem.Skill.TRADING, 1);
        }

        // Seed CARD_CHEAT rumour
        if (rumourNetwork != null) {
            rumourNetwork.addRumour(mick,
                    new Rumour(RumourType.CARD_CHEAT,
                            "Mick at the social club back room was dealing from the bottom of the deck."));
        }

        // Reduce MARCHETTI_CREW Respect
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW,
                    -RESPECT_PENALTY_DETECT_MICK);
        }

        // Mick becomes hostile
        mick.setState(NPCState.ATTACKING_PLAYER);

        // If Enforcer present, they also attack
        if (npcs != null) {
            for (NPC npc : npcs) {
                if (npc.getType() == NPCType.MARCHETTI_ENFORCER) {
                    npc.setState(NPCState.ATTACKING_PLAYER);
                }
            }
        }

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.BACK_ROOM_WINNER);
        }

        return CheatDetectResult.CHEAT_DETECTED;
    }

    /**
     * Play a pontoon hand. Simple twist/stick — player draws two cards (random 1–10 each).
     * If total > 21 it's a bust. Otherwise player wins if total > 15 (simplified house rules).
     * Mick's cheating applies a −15% outcome adjustment when active.
     *
     * @param playerInventory player's inventory
     * @param bet             bet amount (clamped to PONTOON_MIN_BET – PONTOON_MAX_BET)
     * @return net COIN change (positive = win, negative = loss)
     */
    public int playPontoonHand(Inventory playerInventory, int bet) {
        bet = Math.max(PONTOON_MIN_BET, Math.min(PONTOON_MAX_BET, bet));
        if (playerInventory.getItemCount(Material.COIN) < bet) return 0;

        playerInventory.removeItem(Material.COIN, bet);

        int card1 = 1 + rng.nextInt(10);
        int card2 = 1 + rng.nextInt(10);
        int total = card1 + card2;

        // Optionally twist
        if (total < 16) {
            int card3 = 1 + rng.nextInt(10);
            total += card3;
        }

        boolean bust = total > 21;

        // Mick's cheating: house wins more
        if (mickCheating && !bust) {
            // 15% extra chance to lose
            if (rng.nextFloat() < 0.15f) {
                bust = true;
            }
        }

        if (bust) {
            pontoonNetWin -= bet;
            return -bet;
        } else {
            // Win
            int payout;
            if (total == 21 && (card1 == 10 || card2 == 10)) {
                // Pontoon (21 with face card) — 2:1
                payout = bet * 2;
            } else {
                payout = bet;
            }
            playerInventory.addItem(Material.COIN, bet + payout);
            pontoonNetWin += payout;

            // PONTOON_KING achievement
            if (pontoonNetWin >= PONTOON_KING_WIN_THRESHOLD && achievementSystem != null) {
                achievementSystem.unlock(AchievementType.PONTOON_KING);
            }
            return payout;
        }
    }

    /** Returns whether Mick is (flagged as) cheating. */
    public boolean isMickCheating() {
        return mickCheating;
    }

    /** Returns the career net pontoon win total. */
    public int getPontoonNetWin() {
        return pontoonNetWin;
    }

    // ── Protection Envelope ───────────────────────────────────────────────────

    /**
     * Returns whether the protection envelope has been placed on the bar.
     */
    public boolean isEnvelopePlaced() {
        return envelopePlaced;
    }

    /**
     * Force the protection payment event (for testing).
     */
    public void triggerProtectionPayment() {
        envelopePlaced = true;
        enforcerPresent = false;
        envelopeStolen = false;
        enforcerArrested = false;
        playerGrassed = false;
    }

    /**
     * Player steals the protection envelope from the bar before the Enforcer arrives.
     *
     * @param playerInventory player's inventory
     * @param currentHour     in-game hour (must be between ENVELOPE_PLACED and ENFORCER_ARRIVE)
     * @param enforcer        the Marchetti Enforcer NPC (may be null if not yet spawned)
     * @return result of the theft attempt
     */
    public EnvelopeResult stealEnvelope(Inventory playerInventory, float currentHour, NPC enforcer) {
        if (!envelopePlaced) return EnvelopeResult.ENVELOPE_NOT_PLACED;
        if (envelopeStolen) return EnvelopeResult.ENFORCER_ALREADY_COLLECTED;
        if (enforcerPresent && currentHour >= ENFORCER_ARRIVE_HOUR) {
            return EnvelopeResult.ENFORCER_ALREADY_COLLECTED;
        }

        envelopeStolen = true;
        playerInventory.addItem(Material.COIN, PROTECTION_ENVELOPE_COIN);

        // Notoriety
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(ENVELOPE_THEFT_NOTORIETY, achievementCallback);
        }

        // Wanted stars
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(ENVELOPE_THEFT_WANTED, 0, 0, 0, null);
        }

        // MARCHETTI_CREW Respect penalty
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW,
                    -RESPECT_PENALTY_ENVELOPE_THEFT);
        }

        // Criminal record
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.THEFT);
        }

        // Enforcer becomes hostile if present
        if (enforcer != null) {
            enforcer.setState(NPCState.ATTACKING_PLAYER);
        }

        return EnvelopeResult.STOLEN;
    }

    /**
     * Player calls the police before 20:00 to grass on the protection handover.
     * Police arrive at 20:10; Enforcer and Terry are arrested.
     *
     * @param currentHour    in-game hour (must be before ENVELOPE_PLACED_HOUR)
     * @param dayOfWeek      current day (must be Sunday)
     * @param enforcer       Marchetti Enforcer NPC
     * @param npcs           all NPCs (to find enforcer if not passed directly)
     * @return result of the grass attempt
     */
    public GrassResult reportToPolice(float currentHour, int dayOfWeek, NPC enforcer, List<NPC> npcs) {
        if (dayOfWeek != DAY_SUNDAY) return GrassResult.NOT_PROTECTION_DAY;
        if (currentHour >= ENVELOPE_PLACED_HOUR) return GrassResult.TOO_LATE;

        playerGrassed = true;
        enforcerArrested = true;
        clubBanDaysRemaining = GRASS_BAN_DAYS;

        // Enforcer arrested (fleeing)
        if (enforcer != null) {
            enforcer.setState(NPCState.FLEEING);
        }
        if (npcs != null) {
            for (NPC npc : npcs) {
                if (npc.getType() == NPCType.MARCHETTI_ENFORCER) {
                    npc.setState(NPCState.FLEEING);
                }
            }
        }

        // MARCHETTI_CREW Respect penalty
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW,
                    -RESPECT_PENALTY_GRASS);
        }

        // Seed POLICE_SNITCH rumour
        if (rumourNetwork != null) {
            NPC fakeNpc = new NPC(NPCType.CLUB_REGULAR, 0, 0, 0);
            rumourNetwork.addRumour(fakeNpc,
                    new Rumour(RumourType.POLICE_SNITCH,
                            "Someone grassed on the collection at the social club — Tommy's not happy."));
        }

        return GrassResult.SUCCESS;
    }

    /** Returns whether the player has grassed on the protection handover. */
    public boolean hasPlayerGrassed() {
        return playerGrassed;
    }

    /** Returns whether the Enforcer has been arrested. */
    public boolean isEnforcerArrested() {
        return enforcerArrested;
    }

    /** Returns whether the Enforcer is currently present. */
    public boolean isEnforcerPresent() {
        return enforcerPresent;
    }

    /** Returns whether the envelope was stolen by the player. */
    public boolean isEnvelopeStolen() {
        return envelopeStolen;
    }

    // ── AGM ───────────────────────────────────────────────────────────────────

    /**
     * Table the "Investigate the accounts" motion at the AGM.
     * Requires 3 CLUB_REGULAR NPC votes to pass.
     *
     * @param playerInventory player's inventory
     * @param npcs            all NPCs in scene (counts CLUB_REGULAR vote)
     * @return result of the motion
     */
    public AGMMotionResult tableInvestigateMotion(Inventory playerInventory, List<NPC> npcs) {
        if (!agmActive) return AGMMotionResult.AGM_NOT_ACTIVE;

        int votes = 0;
        if (npcs != null) {
            for (NPC npc : npcs) {
                if (npc.getType() == NPCType.CLUB_REGULAR
                        || npc.getType() == NPCType.SOCIAL_CLUB_STEWARD) {
                    // Each CLUB_REGULAR has 60% chance to vote yes
                    if (rng.nextFloat() < 0.6f) votes++;
                }
            }
        }

        if (votes >= 3) {
            agmMotionPassed = true;
            bribeOffered = true;

            // Achievement
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.AGM_TROUBLEMAKER);
            }
            return AGMMotionResult.MOTION_PASSED;
        }
        return AGMMotionResult.MOTION_REJECTED;
    }

    /**
     * Force the AGM active (for testing).
     */
    public void forceAGMActive(boolean active) {
        this.agmActive = active;
    }

    /**
     * Respond to Terry's bribe offer after the AGM motion passes.
     *
     * @param acceptBribe     whether the player accepts the 10 COIN bribe
     * @param playerInventory player's inventory
     * @return result
     */
    public AGMMotionResult respondToBribe(boolean acceptBribe, Inventory playerInventory) {
        if (!bribeOffered) return AGMMotionResult.AGM_NOT_ACTIVE;
        bribeOffered = false;

        if (acceptBribe) {
            playerInventory.addItem(Material.COIN, AGM_BRIBE_COIN);
            if (factionSystem != null) {
                factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW,
                        RESPECT_BONUS_ACCEPT_BRIBE);
            }
            return AGMMotionResult.BRIBE_ACCEPTED;
        } else {
            // Bribe refused — seed COMMITTEE_CONSPIRACY rumour
            if (rumourNetwork != null) {
                NPC fakeNpc = new NPC(NPCType.CLUB_REGULAR, 0, 0, 0);
                rumourNetwork.addRumour(fakeNpc,
                        new Rumour(RumourType.COMMITTEE_CONSPIRACY,
                                "Something dodgy going on with the social club accounts — Terry's sweating."));
            }
            // Neighbourhood vibes boost
            if (neighbourhoodSystem != null) {
                int current = neighbourhoodSystem.getVibes();
                neighbourhoodSystem.setVibes(current + VIBES_CONSPIRACY_EXPOSED);
            }
            return AGMMotionResult.BRIBE_REFUSED;
        }
    }

    /** Returns whether the AGM motion was passed. */
    public boolean isAGMMotionPassed() {
        return agmMotionPassed;
    }

    /** Returns whether a bribe has been offered. */
    public boolean isBribeOffered() {
        return bribeOffered;
    }

    // ── Newspaper ─────────────────────────────────────────────────────────────

    /**
     * Called when the COMMITTEE_CONSPIRACY rumour has spread to 5+ NPCs.
     * Generates a newspaper headline and boosts neighbourhood vibes.
     */
    public void onConspiracyRumourSpreadWide() {
        pendingHeadline = "Social Club Chairman Exposed in Protection Racket — Terry Denies All";
        if (newspaperSystem != null) {
            newspaperSystem.publishHeadline(pendingHeadline);
        }
        if (neighbourhoodSystem != null) {
            int current = neighbourhoodSystem.getVibes();
            neighbourhoodSystem.setVibes(current + VIBES_PROTECTION_EXPOSED);
        }
    }

    /**
     * Poll the pending newspaper headline (consumed on read).
     *
     * @return headline string, or null if none pending
     */
    public String pollPendingHeadline() {
        String h = pendingHeadline;
        pendingHeadline = null;
        return h;
    }

    // ── Day-advance tick ──────────────────────────────────────────────────────

    /**
     * Advance one in-game day — decrements ban counters and resets daily state.
     */
    public void advanceDay() {
        if (clubBanDaysRemaining > 0) clubBanDaysRemaining--;
        if (quizBanDaysRemaining > 0) quizBanDaysRemaining--;

        // Reset daily protection state
        envelopePlaced = false;
        envelopeStolen = false;
        enforcerPresent = false;
        enforcerArrested = false;
        playerGrassed = false;
        agmActive = false;
        agmMotionPassed = false;
        bribeOffered = false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the landmark type for this system. */
    public LandmarkType getLandmarkType() {
        return LandmarkType.SPORTING_SOCIAL_CLUB;
    }

    /** Returns the display name of this landmark. */
    public String getDisplayName() {
        return LandmarkType.SPORTING_SOCIAL_CLUB.getDisplayName();
    }

    /** Returns days remaining on the social club ban. */
    public int getClubBanDaysRemaining() {
        return clubBanDaysRemaining;
    }

    /** Returns days remaining on the quiz ban. */
    public int getQuizBanDaysRemaining() {
        return quizBanDaysRemaining;
    }

    /** Force-set club ban days (for testing). */
    public void setClubBanDays(int days) {
        this.clubBanDaysRemaining = days;
    }

    /** Force-set quiz ban days (for testing). */
    public void setQuizBanDays(int days) {
        this.quizBanDaysRemaining = days;
    }

    /** Force-set player membership flag (for testing without inventory). */
    public void setPlayerIsMember(boolean member) {
        this.playerIsMember = member;
    }

    /** Force pontoon net win (for testing). */
    public void setPontoonNetWin(int value) {
        this.pontoonNetWin = value;
    }

    /** Force darts wins count (for testing). */
    public void setDartsWinsAtClub(int wins) {
        this.dartsWinsAtClub = wins;
    }

    /** Force darts set awarded flag (for testing). */
    public void setDartsSetAwarded(boolean awarded) {
        this.dartsSetAwarded = awarded;
    }

    /** Force the Enforcer present flag (for testing). */
    public void setEnforcerPresent(boolean present) {
        this.enforcerPresent = present;
    }

    /** Force the envelope placed flag (for testing). */
    public void setEnvelopePlaced(boolean placed) {
        this.envelopePlaced = placed;
    }
}

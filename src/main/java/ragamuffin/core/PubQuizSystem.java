package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1259: Northfield Pub Quiz Night — PubQuizSystem, Derek's Wednesday Grilling
 * &amp; the Cheat Sheet Hustle.
 *
 * <p>Every Wednesday 20:00–22:00, The Rusty Anchor hosts a pub quiz run by
 * Derek ({@link NPCType#QUIZ_MASTER}). Players register at the
 * {@link ragamuffin.world.PropType#QUIZ_PODIUM_PROP} (1 COIN entry, open 19:30–20:00),
 * receive a {@link Material#QUIZ_SHEET}, then compete across 6 rounds × 5 questions
 * against 2–4 rival {@link NPCType#QUIZ_TEAM} teams.
 *
 * <h3>Rounds &amp; Scoring</h3>
 * Categories: Sport, Geography, TV &amp; Film, Music, Science &amp; Nature, Local Knowledge.
 * Each question shows a 20-second timer via {@link TooltipSystem} and accepts answer
 * keys 1–4. NPC team-mates auto-answer at 55% accuracy per question.
 * Rival teams score Normal(μ=3, σ=1) per round, clamped [0, 5].
 *
 * <h3>The Cheat Sheet Hustle</h3>
 * Press F during any question to use a {@link Material#CHEAT_SHEET}:
 * +2 points but 40% chance Derek catches you — disqualified, ejected,
 * Notoriety +3, {@link CriminalRecord.CrimeType#CHEATING_AT_PUB_QUIZ}.
 * The player can also plant a cheat sheet on a rival table; 15%/round chance Derek
 * spots it and disqualifies that team instead.
 *
 * <h3>Prizes</h3>
 * Winner receives {@link Material#QUIZ_PRIZE_ENVELOPE} (10 COIN) +
 * {@link AchievementType#QUIZ_CHAMPION} achievement + {@link RumourType#QUIZ_CHAMPION_RUMOUR}.
 * {@link AchievementType#HAT_TRICK_QUIZZER} for 3 consecutive wins.
 * ≥20/30 consolation prize: 2 COIN.
 * Gary grants 1 free pint to the winning team.
 *
 * <h3>Entry Guard</h3>
 * High-notoriety players (Notoriety ≥ {@value #DEREK_BLOCK_NOTORIETY} / Tier ≥ 4)
 * are refused entry by Derek.
 */
public class PubQuizSystem {

    // ── Time constants ────────────────────────────────────────────────────────

    /** Registration window opens (19:30). */
    public static final float REGISTRATION_OPEN_HOUR  = 19.5f;

    /** Registration window closes / quiz starts (20:00). */
    public static final float QUIZ_START_HOUR = 20.0f;

    /** Quiz ends (22:00). */
    public static final float QUIZ_END_HOUR = 22.0f;

    /** Day-of-week index for Wednesday (0=Monday). */
    public static final int WEDNESDAY_INDEX = 2;

    // ── Quiz structure ────────────────────────────────────────────────────────

    /** Number of rounds per quiz night. */
    public static final int ROUNDS_TOTAL = 6;

    /** Questions per round. */
    public static final int QUESTIONS_PER_ROUND = 5;

    /** Maximum score per round (one point per question). */
    public static final int MAX_SCORE_PER_ROUND = QUESTIONS_PER_ROUND;

    /** Total questions in the quiz. */
    public static final int TOTAL_QUESTIONS = ROUNDS_TOTAL * QUESTIONS_PER_ROUND;

    /** Total maximum player score. */
    public static final int MAX_TOTAL_SCORE = ROUNDS_TOTAL * MAX_SCORE_PER_ROUND;

    /** Seconds per question before it times out (unanswered = miss). */
    public static final float QUESTION_TIMER_SECONDS = 20.0f;

    // ── Team settings ─────────────────────────────────────────────────────────

    /** Minimum number of rival NPC teams. */
    public static final int RIVAL_TEAMS_MIN = 2;

    /** Maximum number of rival NPC teams. */
    public static final int RIVAL_TEAMS_MAX = 4;

    /** NPC team-mate answer accuracy (0–1). */
    public static final float TEAMMATE_ACCURACY = 0.55f;

    /** Mean rival-team score per round (Normal distribution μ). */
    public static final float RIVAL_SCORE_MEAN = 3.0f;

    /** Std-dev rival-team score per round (Normal distribution σ). */
    public static final float RIVAL_SCORE_STDDEV = 1.0f;

    // ── Cheat sheet ───────────────────────────────────────────────────────────

    /** Points awarded for using a CHEAT_SHEET. */
    public static final int CHEAT_SHEET_BONUS = 2;

    /** Probability Derek catches a player using a CHEAT_SHEET (0–1). */
    public static final float CHEAT_CATCH_CHANCE = 0.40f;

    /** Notoriety added on cheat-sheet catch. */
    public static final int CHEAT_CATCH_NOTORIETY = 3;

    /** Per-round probability Derek spots a cheat sheet planted on a rival table (0–1). */
    public static final float PLANTED_CHEAT_CATCH_CHANCE = 0.15f;

    // ── Entry guard ───────────────────────────────────────────────────────────

    /** Notoriety at which Derek refuses player entry (Tier 4 threshold = 750). */
    public static final int DEREK_BLOCK_NOTORIETY = NotorietySystem.TIER_4_THRESHOLD;

    // ── Prizes ────────────────────────────────────────────────────────────────

    /** COIN value inside the prize envelope. */
    public static final int PRIZE_COIN_VALUE = 10;

    /** Consolation prize COIN for scoring ≥ this many points but not winning. */
    public static final int CONSOLATION_SCORE_THRESHOLD = 20;

    /** COIN value of the consolation prize. */
    public static final int CONSOLATION_COIN_VALUE = 2;

    /** Entry fee in COIN. */
    public static final int ENTRY_FEE_COIN = 1;

    // ── Registration results ──────────────────────────────────────────────────

    public enum RegistrationResult {
        /** Player successfully registered; receives QUIZ_SHEET. */
        REGISTERED,
        /** Not Wednesday or outside 19:30–20:00 window. */
        WRONG_TIME,
        /** Player already registered for this session. */
        ALREADY_REGISTERED,
        /** Player cannot afford the 1 COIN entry fee. */
        INSUFFICIENT_FUNDS,
        /** Derek refuses entry: Notoriety ≥ DEREK_BLOCK_NOTORIETY. */
        BLOCKED_NOTORIETY,
        /** Quiz is not active (no session running). */
        NOT_ACTIVE
    }

    // ── Answer results ────────────────────────────────────────────────────────

    public enum AnswerResult {
        /** Answer accepted (correct or not — response is the same). */
        ANSWERED,
        /** No question is currently active. */
        NO_QUESTION_ACTIVE,
        /** Player is not registered. */
        NOT_REGISTERED,
        /** Player has been disqualified. */
        DISQUALIFIED
    }

    // ── Cheat sheet results ───────────────────────────────────────────────────

    public enum CheatSheetResult {
        /** Cheat sheet used successfully; +2 points awarded. */
        USED,
        /** Derek caught the player; disqualified. */
        CAUGHT,
        /** Player has no CHEAT_SHEET in inventory. */
        NO_CHEAT_SHEET,
        /** No question is currently active. */
        NO_QUESTION_ACTIVE,
        /** Player is not registered or is disqualified. */
        NOT_ELIGIBLE
    }

    // ── Plant cheat results ───────────────────────────────────────────────────

    public enum PlantCheatResult {
        /** Cheat sheet planted on a rival table. */
        PLANTED,
        /** No CHEAT_SHEET in inventory to plant. */
        NO_CHEAT_SHEET,
        /** No rival teams to plant on. */
        NO_RIVALS,
        /** Player is not registered. */
        NOT_ELIGIBLE
    }

    // ── Session outcome ───────────────────────────────────────────────────────

    public enum SessionOutcome {
        /** Player's team won the quiz. */
        WINNER,
        /** Player's team scored consolation threshold but did not win. */
        CONSOLATION,
        /** Player's team lost and scored below consolation threshold. */
        LOSER,
        /** Player was disqualified during the session. */
        DISQUALIFIED
    }

    // ── Round categories ──────────────────────────────────────────────────────

    public enum QuizCategory {
        SPORT,
        GEOGRAPHY,
        TV_AND_FILM,
        MUSIC,
        SCIENCE_AND_NATURE,
        LOCAL_KNOWLEDGE
    }

    // ── Internal state ────────────────────────────────────────────────────────

    /** Whether a quiz session is currently running. */
    private boolean sessionActive = false;

    /** Whether the player is registered for the current session. */
    private boolean playerRegistered = false;

    /** Whether the player has been disqualified this session. */
    private boolean playerDisqualified = false;

    /** Current round index (0-based). */
    private int currentRound = 0;

    /** Current question index within the round (0-based). */
    private int currentQuestion = 0;

    /** Whether a question is currently active (timer running). */
    private boolean questionActive = false;

    /** Remaining seconds on the current question timer. */
    private float questionTimerRemaining = 0f;

    /** Player's total score this session. */
    private int playerScore = 0;

    /** Number of rival teams this session. */
    private int rivalTeamCount = 0;

    /** Per-round scores for each rival team (index by team index). */
    private int[] rivalTeamRoundScores;

    /** Total accumulated scores for each rival team. */
    private int[] rivalTotalScores;

    /** Whether each rival team has been disqualified (planted cheat). */
    private boolean[] rivalDisqualified;

    /** Number of consecutive quiz wins (for HAT_TRICK_QUIZZER). */
    private int consecutiveWins = 0;

    /** Whether the player has planted a cheat sheet this session. */
    private boolean cheatSheetPlanted = false;

    /** The rival team index on which the cheat sheet was planted (-1 = none). */
    private int plantedOnTeam = -1;

    /** Seconds since session was last updated (used for per-frame timer). */
    private float sessionTimer = 0f;

    /** Derek NPC for this session. */
    private NPC derek = null;

    /** Rival team NPCs (one NPC per team as representative). */
    private final List<NPC> rivalTeams = new ArrayList<>();

    /** Position of the podium (for NPC placement). */
    private float podiumX = 0f;
    private float podiumY = 0f;
    private float podiumZ = 0f;

    // ── Dependencies ──────────────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private RumourNetwork rumourNetwork;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;

    private final Random random;

    // ── Constructor ───────────────────────────────────────────────────────────

    public PubQuizSystem() {
        this(new Random());
    }

    public PubQuizSystem(Random random) {
        this.random = random;
    }

    // ── Dependency setters ────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    // ── Session lifecycle ─────────────────────────────────────────────────────

    /**
     * Called by {@link WetherspoonsSystem} when the QUIZ_NIGHT atmospheric event fires
     * on a Wednesday in the 19:30–22:00 window. Initialises Derek and rival teams.
     *
     * @param pubX  pub X position (Derek spawns at podium)
     * @param pubY  pub Y position
     * @param pubZ  pub Z position
     */
    public void startSession(float pubX, float pubY, float pubZ) {
        if (sessionActive) return;

        sessionActive = true;
        playerRegistered = false;
        playerDisqualified = false;
        currentRound = 0;
        currentQuestion = 0;
        questionActive = false;
        questionTimerRemaining = 0f;
        playerScore = 0;
        cheatSheetPlanted = false;
        plantedOnTeam = -1;
        sessionTimer = 0f;

        this.podiumX = pubX;
        this.podiumY = pubY;
        this.podiumZ = pubZ;

        // Spawn Derek at the podium
        derek = new NPC(NPCType.QUIZ_MASTER, pubX, pubY, pubZ + 2);
        derek.setName("Derek");
        derek.setSpeechText("Right then — pens at the ready. Quiz starts at eight o'clock sharp.", 0f);
        derek.setState(NPCState.IDLE);

        // Spawn 2–4 rival teams
        rivalTeamCount = RIVAL_TEAMS_MIN + random.nextInt(RIVAL_TEAMS_MAX - RIVAL_TEAMS_MIN + 1);
        rivalTeamRoundScores = new int[rivalTeamCount];
        rivalTotalScores = new int[rivalTeamCount];
        rivalDisqualified = new boolean[rivalTeamCount];
        rivalTeams.clear();

        for (int i = 0; i < rivalTeamCount; i++) {
            NPC team = new NPC(NPCType.QUIZ_TEAM, pubX + (i * 2) - rivalTeamCount, pubY, pubZ + 4);
            team.setState(NPCState.IDLE);
            rivalTeams.add(team);
            rivalTotalScores[i] = 0;
            rivalDisqualified[i] = false;
        }
    }

    /**
     * End the current session and clean up NPCs.
     */
    public void endSession() {
        sessionActive = false;
        derek = null;
        rivalTeams.clear();
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update. Advances question timers and handles round/session
     * progression.
     *
     * @param delta       frame time in seconds
     * @param timeSystem  current game time
     * @param inventory   player inventory (for prize delivery)
     * @param gary        Gary the BARMAN NPC (for free pint delivery)
     */
    public void update(float delta, TimeSystem timeSystem, Inventory inventory, NPC gary) {
        if (!sessionActive) return;

        float hour = timeSystem.getHours() + timeSystem.getMinutes() / 60f;

        // End session at 22:00
        if (hour >= QUIZ_END_HOUR) {
            resolveSession(inventory, gary);
            endSession();
            return;
        }

        // Advance question timer if a question is active
        if (questionActive) {
            questionTimerRemaining -= delta;
            if (questionTimerRemaining <= 0f) {
                // Time expired — no points awarded for this question
                advanceQuestion(inventory, gary);
            }
        }
    }

    // ── Player registration ───────────────────────────────────────────────────

    /**
     * Player presses E on the QUIZ_PODIUM_PROP to register.
     *
     * @param inventory   player inventory (must contain ≥1 COIN)
     * @param timeSystem  current game time
     * @param dayOfWeek   current day of week (0=Monday)
     * @return            result of registration attempt
     */
    public RegistrationResult register(Inventory inventory, TimeSystem timeSystem, int dayOfWeek) {
        if (!sessionActive) return RegistrationResult.NOT_ACTIVE;

        if (dayOfWeek != WEDNESDAY_INDEX) return RegistrationResult.WRONG_TIME;

        float hour = timeSystem.getHours() + timeSystem.getMinutes() / 60f;
        if (hour < REGISTRATION_OPEN_HOUR || hour >= QUIZ_START_HOUR) {
            return RegistrationResult.WRONG_TIME;
        }

        if (playerRegistered) return RegistrationResult.ALREADY_REGISTERED;

        // Check notoriety
        if (notorietySystem != null
                && notorietySystem.getNotoriety() >= DEREK_BLOCK_NOTORIETY) {
            if (derek != null) {
                derek.setSpeechText("Not for you, sunshine. We know your type.", 5f);
            }
            return RegistrationResult.BLOCKED_NOTORIETY;
        }

        // Check entry fee
        if (inventory.count(Material.COIN) < ENTRY_FEE_COIN) {
            if (derek != null) {
                derek.setSpeechText("It's a quid to enter, love. Come back when you've got change.", 5f);
            }
            return RegistrationResult.INSUFFICIENT_FUNDS;
        }

        inventory.remove(Material.COIN, ENTRY_FEE_COIN);
        inventory.add(Material.QUIZ_SHEET, 1);
        playerRegistered = true;

        if (derek != null) {
            derek.setSpeechText("You're in. Table four. Don't cheat — I will notice.", 5f);
        }

        return RegistrationResult.REGISTERED;
    }

    // ── Question answering ────────────────────────────────────────────────────

    /**
     * Player presses key 1–4 to answer the current question.
     * Scoring is probabilistic: 55% chance answer is correct (team-mate accuracy).
     *
     * @param answerKey  1–4 key pressed
     * @param inventory  player inventory
     * @param gary       Gary NPC (for session end)
     * @return           result of the answer attempt
     */
    public AnswerResult answerQuestion(int answerKey, Inventory inventory, NPC gary) {
        if (!playerRegistered) return AnswerResult.NOT_REGISTERED;
        if (playerDisqualified) return AnswerResult.DISQUALIFIED;
        if (!questionActive) return AnswerResult.NO_QUESTION_ACTIVE;

        // 55% accuracy for team-mates answering
        if (random.nextFloat() < TEAMMATE_ACCURACY) {
            playerScore++;
        }

        advanceQuestion(inventory, gary);
        return AnswerResult.ANSWERED;
    }

    // ── Cheat sheet mechanics ─────────────────────────────────────────────────

    /**
     * Player presses F during a question to use a CHEAT_SHEET from their inventory.
     *
     * @param inventory  player inventory
     * @param gary       Gary NPC (for session end)
     * @return           result of cheat attempt
     */
    public CheatSheetResult useCheatSheet(Inventory inventory, NPC gary) {
        if (!playerRegistered || playerDisqualified) return CheatSheetResult.NOT_ELIGIBLE;
        if (!questionActive) return CheatSheetResult.NO_QUESTION_ACTIVE;
        if (inventory.count(Material.CHEAT_SHEET) < 1) return CheatSheetResult.NO_CHEAT_SHEET;

        inventory.remove(Material.CHEAT_SHEET, 1);

        // 40% chance Derek catches the player
        if (random.nextFloat() < CHEAT_CATCH_CHANCE) {
            disqualifyPlayer();
            if (derek != null) {
                derek.setSpeechText("Oi! You've got a cheat sheet! OUT. NOW.", 8f);
            }
            return CheatSheetResult.CAUGHT;
        }

        // Successfully used — +2 points, advance question
        playerScore += CHEAT_SHEET_BONUS;
        advanceQuestion(inventory, gary);
        return CheatSheetResult.USED;
    }

    /**
     * Player plants a CHEAT_SHEET on a rival team's table (passive sabotage).
     * Each round there is a 15% chance Derek spots it and disqualifies that team.
     *
     * @param inventory  player inventory
     * @return           result of plant attempt
     */
    public PlantCheatResult plantCheatSheet(Inventory inventory) {
        if (!playerRegistered || playerDisqualified) return PlantCheatResult.NOT_ELIGIBLE;
        if (inventory.count(Material.CHEAT_SHEET) < 1) return PlantCheatResult.NO_CHEAT_SHEET;
        if (rivalTeamCount == 0) return PlantCheatResult.NO_RIVALS;

        inventory.remove(Material.CHEAT_SHEET, 1);
        cheatSheetPlanted = true;
        plantedOnTeam = random.nextInt(rivalTeamCount);

        return PlantCheatResult.PLANTED;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Disqualify the player: record crime, add notoriety, award shame achievement.
     */
    private void disqualifyPlayer() {
        playerDisqualified = true;
        questionActive = false;
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.CHEATING_AT_PUB_QUIZ);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(CHEAT_CATCH_NOTORIETY);
        }
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.CHEATS_NEVER_PROSPER);
        }
        consecutiveWins = 0;
    }

    /**
     * Advance to the next question; if end of round, score rival teams and check
     * planted cheat sheet. If end of session questions, resolve final scores.
     */
    private void advanceQuestion(Inventory inventory, NPC gary) {
        questionActive = false;
        currentQuestion++;

        if (currentQuestion >= QUESTIONS_PER_ROUND) {
            // End of round — score rival teams for this round
            scoreRivalTeams();
            checkPlantedCheat();

            currentQuestion = 0;
            currentRound++;

            if (currentRound >= ROUNDS_TOTAL) {
                // All rounds complete — session resolves at 22:00, but also trigger here
                resolveSession(inventory, gary);
                endSession();
                return;
            }
        }

        // Start next question
        startNextQuestion();
    }

    /**
     * Activate the next question timer.
     */
    private void startNextQuestion() {
        questionActive = true;
        questionTimerRemaining = QUESTION_TIMER_SECONDS;
    }

    /**
     * Score rival teams for the current round using Normal(μ=3, σ=1), clamped [0,5].
     */
    private void scoreRivalTeams() {
        for (int i = 0; i < rivalTeamCount; i++) {
            if (rivalDisqualified[i]) continue;
            // Box-Muller transform for Normal distribution
            double u1 = random.nextDouble();
            double u2 = random.nextDouble();
            // Avoid log(0)
            if (u1 == 0.0) u1 = 1e-9;
            double z = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
            int score = (int) Math.round(RIVAL_SCORE_MEAN + z * RIVAL_SCORE_STDDEV);
            score = Math.max(0, Math.min(MAX_SCORE_PER_ROUND, score));
            rivalTeamRoundScores[i] = score;
            rivalTotalScores[i] += score;
        }
    }

    /**
     * Check whether Derek spots the planted cheat sheet this round (15% per round).
     */
    private void checkPlantedCheat() {
        if (!cheatSheetPlanted || plantedOnTeam < 0) return;
        if (rivalDisqualified[plantedOnTeam]) return;

        if (random.nextFloat() < PLANTED_CHEAT_CATCH_CHANCE) {
            rivalDisqualified[plantedOnTeam] = true;
            if (derek != null) {
                derek.setSpeechText("Team " + (plantedOnTeam + 1) + " — you're OUT. Cheating won't be tolerated.", 8f);
            }
        }
    }

    /**
     * Resolve the final session outcome: award prizes, update achievements and rumours.
     */
    private void resolveSession(Inventory inventory, NPC gary) {
        if (!sessionActive) return;
        if (playerDisqualified) return;
        if (!playerRegistered) return;

        // Find the highest rival score
        int highestRival = 0;
        for (int i = 0; i < rivalTeamCount; i++) {
            if (!rivalDisqualified[i] && rivalTotalScores[i] > highestRival) {
                highestRival = rivalTotalScores[i];
            }
        }

        SessionOutcome outcome;
        if (playerScore > highestRival) {
            outcome = SessionOutcome.WINNER;
        } else if (playerScore >= CONSOLATION_SCORE_THRESHOLD) {
            outcome = SessionOutcome.CONSOLATION;
        } else {
            outcome = SessionOutcome.LOSER;
        }

        if (outcome == SessionOutcome.WINNER) {
            // Award prize envelope (contains 10 COIN)
            inventory.add(Material.QUIZ_PRIZE_ENVELOPE, 1);
            inventory.add(Material.COIN, PRIZE_COIN_VALUE);

            // Achievement: QUIZ_CHAMPION
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.QUIZ_CHAMPION);
            }

            // Track consecutive wins for HAT_TRICK_QUIZZER
            consecutiveWins++;
            if (achievementSystem != null && consecutiveWins >= 3) {
                achievementSystem.unlock(AchievementType.HAT_TRICK_QUIZZER);
            }

            // Seed QUIZ_CHAMPION_RUMOUR
            if (rumourNetwork != null && derek != null) {
                rumourNetwork.addRumour(derek,
                    new Rumour(RumourType.QUIZ_CHAMPION_RUMOUR,
                        "Someone cleaned up at quiz night — answered every question right. Derek was fuming."));
            }

            // Gary grants 1 free pint to the winner
            if (gary != null) {
                gary.setSpeechText("Nice one — that one's on me.", 6f);
                inventory.add(Material.PINT, 1);
            }

            if (derek != null) {
                derek.setSpeechText("And the winner is... well done. Don't let it go to your head.", 8f);
            }

        } else if (outcome == SessionOutcome.CONSOLATION) {
            inventory.add(Material.COIN, CONSOLATION_COIN_VALUE);
            consecutiveWins = 0;
            if (derek != null) {
                derek.setSpeechText("Not bad — here's a couple of quid. Buy yourself a drink.", 6f);
            }
        } else {
            consecutiveWins = 0;
        }
    }

    // ── Session start (called per-frame by WetherspoonsSystem) ───────────────

    /**
     * Called by {@link WetherspoonsSystem} from the QUIZ_NIGHT event on Wednesday
     * evenings. Starts a session if one is not already running.
     *
     * @param pubX  X-coordinate of the pub (podium will be placed here)
     * @param pubY  Y-coordinate
     * @param pubZ  Z-coordinate
     * @param gary  Gary the BARMAN NPC (for prize delivery later)
     */
    public void startSessionFromWetherspoons(float pubX, float pubY, float pubZ, NPC gary) {
        if (!sessionActive) {
            startSession(pubX, pubY, pubZ);
            // Begin first question immediately
            startNextQuestion();
        }
    }

    // ── State accessors ───────────────────────────────────────────────────────

    /** @return {@code true} if a quiz session is currently running. */
    public boolean isSessionActive() {
        return sessionActive;
    }

    /** @return {@code true} if the player is registered for the current session. */
    public boolean isPlayerRegistered() {
        return playerRegistered;
    }

    /** @return {@code true} if the player has been disqualified. */
    public boolean isPlayerDisqualified() {
        return playerDisqualified;
    }

    /** @return the player's current score this session. */
    public int getPlayerScore() {
        return playerScore;
    }

    /** @return the current round index (0-based). */
    public int getCurrentRound() {
        return currentRound;
    }

    /** @return {@code true} if a question is currently active (timer running). */
    public boolean isQuestionActive() {
        return questionActive;
    }

    /** @return remaining seconds on the current question timer. */
    public float getQuestionTimerRemaining() {
        return questionTimerRemaining;
    }

    /** @return the number of rival teams in the current session. */
    public int getRivalTeamCount() {
        return rivalTeamCount;
    }

    /** @return total score for rival team at given index. */
    public int getRivalTotalScore(int teamIndex) {
        if (teamIndex < 0 || teamIndex >= rivalTeamCount) return 0;
        return rivalTotalScores[teamIndex];
    }

    /** @return {@code true} if rival team at given index has been disqualified. */
    public boolean isRivalDisqualified(int teamIndex) {
        if (teamIndex < 0 || teamIndex >= rivalTeamCount) return false;
        return rivalDisqualified[teamIndex];
    }

    /** @return Derek the Quiz Master NPC, or {@code null} if no session is running. */
    public NPC getDerek() {
        return derek;
    }

    /** @return the number of consecutive quiz wins. */
    public int getConsecutiveWins() {
        return consecutiveWins;
    }

    // ── Test helpers ──────────────────────────────────────────────────────────

    /** For testing: force the player score. */
    public void setPlayerScoreForTesting(int score) {
        this.playerScore = score;
    }

    /** For testing: force question active state. */
    public void setQuestionActiveForTesting(boolean active) {
        this.questionActive = active;
        if (active) this.questionTimerRemaining = QUESTION_TIMER_SECONDS;
    }

    /** For testing: force rival team scores. */
    public void setRivalTotalScoreForTesting(int teamIndex, int score) {
        if (teamIndex >= 0 && teamIndex < rivalTeamCount) {
            rivalTotalScores[teamIndex] = score;
        }
    }

    /** For testing: force consecutive wins count. */
    public void setConsecutiveWinsForTesting(int wins) {
        this.consecutiveWins = wins;
    }

    /** For testing: force the current round. */
    public void setCurrentRoundForTesting(int round) {
        this.currentRound = round;
    }

    /** For testing: force player registered state without coin payment. */
    public void setPlayerRegisteredForTesting(boolean registered) {
        this.playerRegistered = registered;
        if (registered && !questionActive) {
            startNextQuestion();
        }
    }
}

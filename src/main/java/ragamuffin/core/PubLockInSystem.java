package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #920: Pub Lock-In — After-Hours Drinking, Darts &amp; Pub Quiz.
 *
 * <p>At 23:00, Terry the Landlord ({@link NPCType#LANDLORD}) locks the front door of
 * The Ragamuffin Arms for an illegal after-hours session with up to
 * {@link #MAX_GUESTS} guests. Terry sells drinks at half price and cycles through
 * {@link #TERRY_SPEECH_LINES} flavourful speech lines.
 *
 * <h3>Activities</h3>
 * <ul>
 *   <li>{@link DartsMinigame} — 301 countdown with bust/double-out rules, optional coin
 *       stake vs a {@link NPCType#PUBLIC} opponent.</li>
 *   <li>{@link PubQuizSystem} — runs every Thursday 20:00–22:00 with 40 hardcoded
 *       British general-knowledge questions, answers on keys 1–4, NPC teams at 60%
 *       accuracy, and a COIN prize pot.</li>
 * </ul>
 *
 * <h3>Police Raid</h3>
 * <ul>
 *   <li>20% chance per 10-in-game-minute cycle that a raid occurs.</li>
 *   <li>Player has 5 seconds to hide behind the bar counter to avoid a
 *       {@link CriminalRecord.CrimeType#DRUNK_AND_DISORDERLY} record entry.</li>
 * </ul>
 *
 * <h3>Tip-Off Mechanic</h3>
 * <ul>
 *   <li>Press E on the locked door to 'Tip off the police' — guarantees a raid.</li>
 *   <li>Unlocks {@link AchievementType#GRASS} and adds +3 Notoriety.</li>
 *   <li>Terry remembers and permanently ejects the player.</li>
 * </ul>
 *
 * <h3>Integration</h3>
 * <ul>
 *   <li>{@link WarmthSystem} — pub counts as shelter (handled externally by ShelterDetector)</li>
 *   <li>{@link RumourNetwork} — raid seeds a PLAYER_SPOTTED rumour</li>
 *   <li>{@link NewspaperSystem} — raid generates a morning headline via
 *       {@link #pollPendingHeadline()}</li>
 *   <li>{@link WeatherSystem} — Terry gives free tea during FROST</li>
 *   <li>{@link BusSystem} — Night Bus DRUNK-NPC probability boosted after a
 *       successful lock-in via {@link #isLockInSuccessful()}</li>
 * </ul>
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@link AchievementType#LOCK_IN_REGULAR} — attended 5 lock-ins</li>
 *   <li>{@link AchievementType#STAYED_BEHIND_THE_BAR} — hid during a raid</li>
 *   <li>{@link AchievementType#QUIZ_NIGHT_CHAMPION} — won the pub quiz</li>
 *   <li>{@link AchievementType#DARTS_HUSTLER} — beat an NPC at darts with a stake</li>
 *   <li>{@link AchievementType#GRASS} — tipped off the police</li>
 *   <li>{@link AchievementType#LOCK_IN_LEGEND} — survived 10 lock-ins clean</li>
 * </ul>
 */
public class PubLockInSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** In-game hour the lock-in starts (23:00). */
    public static final float LOCK_IN_START_HOUR = 23.0f;

    /** In-game hour the lock-in ends (02:00). */
    public static final float LOCK_IN_END_HOUR = 2.0f;

    /** Maximum number of guest NPCs (excluding Terry) during a lock-in. */
    public static final int MAX_GUESTS = 8;

    /** Half-price drink cost in COIN. */
    public static final int DRINK_PRICE = 1;

    /** Number of Terry's speech lines. */
    public static final int TERRY_SPEECH_LINE_COUNT = 10;

    /** Seconds between Terry speech cycles. */
    public static final float TERRY_SPEECH_INTERVAL = 45.0f;

    /** In-game minutes between police raid checks. */
    public static final float RAID_CHECK_INTERVAL_MINUTES = 10.0f;

    /** Probability of a raid occurring each check cycle. */
    public static final float RAID_PROBABILITY = 0.20f;

    /** Seconds the player has to hide when a raid starts. */
    public static final float HIDE_WINDOW_SECONDS = 5.0f;

    /** Notoriety added when tipping off the police. */
    public static final int TIP_OFF_NOTORIETY_GAIN = 3;

    /** In-game hour (Thursday) pub quiz starts. */
    public static final float QUIZ_START_HOUR = 20.0f;

    /** In-game hour pub quiz ends. */
    public static final float QUIZ_END_HOUR = 22.0f;

    /** In-game day-of-week index for Thursday (0=Monday). */
    public static final int THURSDAY_INDEX = 3;

    /** Number of pub quiz questions. */
    public static final int QUIZ_QUESTION_COUNT = 40;

    /** NPC team quiz accuracy (60%). */
    public static final float NPC_QUIZ_ACCURACY = 0.60f;

    /** Prize pot for winning the pub quiz in COIN. */
    public static final int QUIZ_PRIZE_POT = 20;

    /** Seconds (game-time) per quiz question before moving to next. */
    public static final float QUIZ_QUESTION_DURATION = 20.0f;

    /** Time in real seconds each raid check cycle corresponds to. */
    public static final float RAID_CHECK_REAL_SECONDS = 60.0f;

    // ── Terry's flavourful speech lines ───────────────────────────────────────

    public static final String[] TERRY_SPEECH_LINES = {
        "Doors are locked — we're officially closed. Wink.",
        "Same again, love? Half price after hours.",
        "You never saw this, right? Right.",
        "Last one to leave washes up. Probably won't happen.",
        "I've been doing this fifteen years. Never had a problem. Touch wood.",
        "The rozzer's van was parked outside earlier. Probably nothing.",
        "Round's on me if you win the darts. And that's a first.",
        "Mind the wet floor. Somebody knocked over a Guinness. Could've been anyone.",
        "Right, order up before I change my mind.",
        "Lovely bunch tonight. Lovely. Don't tell anyone."
    };

    // ── Pub quiz questions ─────────────────────────────────────────────────────

    /** Hardcoded British general-knowledge quiz questions [question, a1, a2, a3, a4, correctIndex(1-4)]. */
    public static final String[][] QUIZ_QUESTIONS = {
        {"What is the capital of England?", "London", "Manchester", "Birmingham", "Leeds", "1"},
        {"Which team won the 1966 FIFA World Cup?", "Germany", "England", "Brazil", "Argentina", "2"},
        {"What is the UK's national flower?", "Rose", "Thistle", "Daffodil", "Shamrock", "1"},
        {"How many pence in a pound?", "10", "50", "100", "200", "3"},
        {"Who wrote Oliver Twist?", "Thomas Hardy", "Charles Dickens", "Jane Austen", "William Shakespeare", "2"},
        {"What colour is the Queen's Guard uniform coat?", "Black", "Blue", "Red", "Green", "3"},
        {"Which river runs through London?", "Severn", "Mersey", "Avon", "Thames", "4"},
        {"What is the currency of the UK?", "Euro", "Dollar", "Pound Sterling", "Franc", "3"},
        {"Who is the patron saint of England?", "St Andrew", "St Patrick", "St David", "St George", "4"},
        {"What is the largest city in Scotland?", "Edinburgh", "Glasgow", "Aberdeen", "Dundee", "2"},
        {"Which TV show is set in Walford?", "Coronation Street", "Emmerdale", "EastEnders", "Hollyoaks", "3"},
        {"How many sides does a 50p coin have?", "5", "6", "7", "8", "3"},
        {"What is the tallest mountain in the UK?", "Scafell Pike", "Ben Nevis", "Snowdon", "Slieve Donard", "2"},
        {"Which city is known as the Steel City?", "Leeds", "Manchester", "Sheffield", "Birmingham", "3"},
        {"What year did the NHS begin?", "1945", "1948", "1952", "1960", "2"},
        {"What sport is played at Wimbledon?", "Cricket", "Football", "Golf", "Tennis", "4"},
        {"Which is the longest river in Britain?", "Thames", "Severn", "Trent", "Mersey", "2"},
        {"Who sang 'Don't You Want Me'?", "Duran Duran", "Human League", "Depeche Mode", "Spandau Ballet", "2"},
        {"What is a bap in British English?", "Shoe", "Hat", "Bread roll", "Snack bar", "3"},
        {"What does 'ASBO' stand for?", "Anti-Social Behaviour Order", "Authorised Social Behaviour Order", "Anti-Squad Border Order", "Approved Social Business Ordinance", "1"},
        {"Which city hosted the 2012 Olympics?", "Manchester", "Birmingham", "London", "Edinburgh", "3"},
        {"What is the informal name for a British police officer?", "Bobby", "Bill", "Barry", "Bob", "1"},
        {"Which biscuit is dunked most in Britain?", "Digestive", "Hobnob", "Bourbon", "Custard Cream", "1"},
        {"What is 'bangers and mash'?", "Fish and chips", "Sausages and mashed potato", "Bacon and eggs", "Steak and kidney pie", "2"},
        {"Which UK city is famous for its Cavern Club?", "London", "Manchester", "Liverpool", "Birmingham", "3"},
        {"Who wrote the Harry Potter series?", "Roald Dahl", "Terry Pratchett", "J.K. Rowling", "Philip Pullman", "3"},
        {"What colour is a double-decker London bus?", "Blue", "Green", "Yellow", "Red", "4"},
        {"How many floors does a standard terrace house have?", "1", "2", "3", "4", "2"},
        {"What is spotted dick?", "A rash", "A pudding", "A card game", "A dog breed", "2"},
        {"Which city is associated with Coronation Street?", "Sheffield", "Manchester", "Liverpool", "Bradford", "2"},
        {"What does 'ta' mean in British slang?", "Goodbye", "Yes please", "Thank you", "Help me", "3"},
        {"Which show features a pub called The Woolpack?", "EastEnders", "Emmerdale", "Coronation Street", "Hollyoaks", "2"},
        {"What is a 'quid'?", "Five pounds", "Ten pounds", "One pound", "Twenty pounds", "3"},
        {"What is the name of the bear in Paddington?", "Rupert", "Pooh", "Paddington", "Fozzie", "3"},
        {"Which county is known for cream teas?", "Yorkshire", "Devon", "Lancashire", "Kent", "2"},
        {"What is an 'off-licence' in British English?", "A place selling takeaway alcohol", "A police station", "A discount shop", "A car park", "1"},
        {"What city is Buckingham Palace in?", "Birmingham", "London", "Oxford", "Windsor", "2"},
        {"How often is the Queen's (King's) speech broadcast?", "Daily", "Weekly", "Monthly", "Annually", "4"},
        {"Which UK motorway connects London to Scotland?", "M1", "M4", "M6", "M25", "1"},
        {"What is 'mushy peas'?", "Crushed avocado", "A dessert made from mint", "Cooked and crushed marrowfat peas", "Peas in cream sauce", "3"},
    };

    // ── Inner classes ──────────────────────────────────────────────────────────

    /**
     * Result of a player pub-quiz answer.
     */
    public enum QuizAnswerResult {
        CORRECT,
        INCORRECT,
        QUIZ_NOT_ACTIVE,
        ALREADY_ANSWERED
    }

    /**
     * Result of a darts game completion.
     */
    public enum DartsResult {
        PLAYER_WIN,
        PLAYER_BUST,
        NPC_WIN,
        GAME_NOT_ACTIVE
    }

    /**
     * Result of attempting to hide during a raid.
     */
    public enum HideResult {
        SUCCESS,
        TOO_LATE,
        NO_RAID_IN_PROGRESS
    }

    /**
     * Result of a tip-off action.
     */
    public enum TipOffResult {
        SUCCESS,
        LOCK_IN_NOT_ACTIVE,
        ALREADY_GRASSED
    }

    // ── DartsMinigame inner class ──────────────────────────────────────────────

    /**
     * 301 countdown darts minigame with bust/double-out rules.
     *
     * <p>Both the player and an NPC opponent start at 301. They alternate throws.
     * A throw scores between 1 and 60 points. To win, a player must reach exactly 0
     * and the finishing throw must be a double (score divisible by 2).
     * Going below 0 or hitting 1 with no double available is a bust (score reset
     * to value before the throw).
     */
    public static class DartsMinigame {

        /** Starting score for both player and NPC. */
        public static final int STARTING_SCORE = 301;

        /** Minimum single-throw score. */
        public static final int MIN_THROW_SCORE = 1;

        /** Maximum single-throw score (treble twenty). */
        public static final int MAX_THROW_SCORE = 60;

        /** Optional coin stake (0 = no stake). */
        private final int coinStake;

        private int playerScore;
        private int npcScore;
        private boolean playerTurn;
        private boolean active;
        private DartsResult result;

        private final Random rng;

        /**
         * Create a new darts game.
         *
         * @param coinStake optional COIN stake (0 = no stake)
         * @param rng       random number generator
         */
        public DartsMinigame(int coinStake, Random rng) {
            this.coinStake = coinStake;
            this.rng = rng;
            this.playerScore = STARTING_SCORE;
            this.npcScore = STARTING_SCORE;
            this.playerTurn = true;
            this.active = true;
            this.result = null;
        }

        /**
         * Player throws a dart. Scores a random value between MIN and MAX.
         * Applies bust rule: if remaining would go below 0 or land on 1,
         * the score is reset to pre-throw value.
         *
         * @return the score achieved (0 = bust)
         */
        public int playerThrow() {
            if (!active || !playerTurn) return 0;

            int score = MIN_THROW_SCORE + rng.nextInt(MAX_THROW_SCORE - MIN_THROW_SCORE + 1);
            int newScore = playerScore - score;

            if (newScore < 0 || newScore == 1) {
                // Bust — no change
                playerTurn = false;
                npcTakeTurn();
                return 0;
            }

            playerScore = newScore;

            if (playerScore == 0 && score % 2 == 0) {
                // Double-out — player wins
                active = false;
                result = DartsResult.PLAYER_WIN;
                return score;
            }

            playerTurn = false;
            npcTakeTurn();
            return score;
        }

        /**
         * NPC takes their turn automatically (average player — roughly 50% success rate).
         */
        private void npcTakeTurn() {
            if (!active) return;

            int score = MIN_THROW_SCORE + rng.nextInt(MAX_THROW_SCORE - MIN_THROW_SCORE + 1);
            int newScore = npcScore - score;

            if (newScore < 0 || newScore == 1) {
                // NPC busts
                playerTurn = true;
                return;
            }

            npcScore = newScore;

            if (npcScore == 0 && score % 2 == 0) {
                // NPC wins
                active = false;
                result = DartsResult.NPC_WIN;
                return;
            }

            playerTurn = true;
        }

        public int getPlayerScore() { return playerScore; }
        public int getNpcScore()    { return npcScore; }
        public boolean isPlayerTurn() { return playerTurn; }
        public boolean isActive()   { return active; }
        public DartsResult getResult() { return result; }
        public int getCoinStake()   { return coinStake; }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random rng;

    /** Whether a lock-in is currently active. */
    private boolean lockInActive = false;

    /** Terry the Landlord NPC (null when lock-in is inactive). */
    private NPC terry = null;

    /** Guest NPCs currently in the lock-in (max MAX_GUESTS). */
    private final List<NPC> guests = new ArrayList<>();

    /** Whether the player is currently participating in the lock-in. */
    private boolean playerInLockIn = false;

    /** Whether the player has been permanently ejected (grassed). */
    private boolean playerEjected = false;

    /** Number of lock-ins the player has attended. */
    private int lockInsAttended = 0;

    /** Number of lock-ins completed without a D&amp;D charge. */
    private int cleanLockIns = 0;

    /** Timer for Terry's speech cycling (seconds). */
    private float terrySpeechTimer = 0f;

    /** Current index in Terry's speech cycle. */
    private int terrySpeechIndex = 0;

    /** Timer counting real seconds toward the next raid check. */
    private float raidCheckTimer = 0f;

    /** Whether a police raid is currently in progress. */
    private boolean raidInProgress = false;

    /** Remaining seconds in the hide window during a raid. */
    private float hideWindowTimer = 0f;

    /** Whether the player successfully hid during the current raid. */
    private boolean playerHidSuccessfully = false;

    /** Whether a raid is pending (from tip-off). */
    private boolean raidGuaranteed = false;

    /** Whether the player has tipped off the police this lock-in. */
    private boolean playerGrassed = false;

    /** Active darts minigame (null when not playing). */
    private DartsMinigame activeDartsGame = null;

    /** Whether the pub quiz is currently active. */
    private boolean quizActive = false;

    /** Current question index in the quiz (0-based). */
    private int quizQuestionIndex = 0;

    /** Player's current quiz score. */
    private int playerQuizScore = 0;

    /** NPC team's current quiz score. */
    private int npcTeamQuizScore = 0;

    /** Timer for the current question (seconds). */
    private float quizQuestionTimer = 0f;

    /** Whether the player has answered the current question. */
    private boolean playerAnsweredQuestion = false;

    /** Whether the quiz has been completed this session. */
    private boolean quizCompletedToday = false;

    /** Last in-game day when the quiz was run (to prevent double-running). */
    private int lastQuizDay = -1;

    /** Whether a raid headline is pending for the newspaper system. */
    private boolean pendingRaidHeadline = false;

    /** Whether a successful lock-in ended (for BusSystem DRUNK boost). */
    private boolean lockInSuccessful = false;

    // ── Optional system references ────────────────────────────────────────────

    private RumourNetwork rumourNetwork;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public PubLockInSystem() {
        this(new Random());
    }

    public PubLockInSystem(Random rng) {
        this.rng = rng;
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setRumourNetwork(RumourNetwork r)     { this.rumourNetwork = r; }
    public void setNotorietySystem(NotorietySystem n) { this.notorietySystem = n; }
    public void setCriminalRecord(CriminalRecord c)   { this.criminalRecord = c; }
    public void setAchievementSystem(AchievementSystem a) { this.achievementSystem = a; }

    // ── Main update ───────────────────────────────────────────────────────────

    /**
     * Update the pub lock-in system each frame.
     *
     * @param delta         seconds since last frame (real-time)
     * @param hour          current in-game hour (0.0–24.0)
     * @param dayOfWeek     current day of week index (0=Monday … 6=Sunday)
     * @param day           current in-game day count (for quiz deduplication)
     * @param weather       current weather (for frost tea mechanic)
     * @param player        the player (may be null)
     * @param playerInventory the player's inventory (may be null)
     */
    public void update(float delta,
                       float hour,
                       int dayOfWeek,
                       int day,
                       Weather weather,
                       Player player,
                       Inventory playerInventory) {

        float normHour = hour % 24.0f;
        boolean shouldBeActive = isLockInHour(normHour);

        // Pub quiz: runs Thursday 20:00–22:00 (independent of lock-in)
        updatePubQuiz(delta, normHour, dayOfWeek, day);

        // Spawn / despawn
        if (shouldBeActive && !lockInActive) {
            startLockIn();
        } else if (!shouldBeActive && lockInActive) {
            endLockIn();
            return;
        }

        if (!lockInActive) return;

        // Terry speech cycling
        terrySpeechTimer += delta;
        if (terrySpeechTimer >= TERRY_SPEECH_INTERVAL) {
            terrySpeechTimer = 0f;
            if (terry != null) {
                terry.say(TERRY_SPEECH_LINES[terrySpeechIndex % TERRY_SPEECH_LINE_COUNT], 6f);
                terrySpeechIndex = (terrySpeechIndex + 1) % TERRY_SPEECH_LINE_COUNT;
            }
        }

        // Frost: Terry gives free tea
        if (weather == Weather.FROST && terry != null && playerInLockIn && playerInventory != null) {
            // Give tea once per lock-in during frost (check by stock)
            if (!playerInventory.hasItem(Material.FLASK_OF_TEA)) {
                playerInventory.addItem(Material.FLASK_OF_TEA, 1);
                if (terry != null) {
                    terry.say("Brass out there tonight. Here, have a tea on me.", 5f);
                }
            }
        }

        // Raid logic
        updateRaidLogic(delta, player, playerInventory);
    }

    // ── Lock-in spawn/despawn ─────────────────────────────────────────────────

    private boolean isLockInHour(float normHour) {
        return normHour >= LOCK_IN_START_HOUR || normHour < LOCK_IN_END_HOUR;
    }

    private void startLockIn() {
        lockInActive = true;
        lockInSuccessful = false;
        terry = new NPC(NPCType.LANDLORD, 0f, 1f, 0f);
        terry.setName("Terry");
        terrySpeechTimer = 0f;
        terrySpeechIndex = 0;
        raidCheckTimer = 0f;
        raidInProgress = false;
        hideWindowTimer = 0f;
        playerHidSuccessfully = false;
        raidGuaranteed = false;
        playerGrassed = false;
        pendingRaidHeadline = false;

        // Spawn guest NPCs
        guests.clear();
        int guestCount = 3 + rng.nextInt(MAX_GUESTS - 2);
        for (int i = 0; i < guestCount; i++) {
            NPC guest = new NPC(NPCType.PUBLIC, i * 2f, 1f, 2f);
            guest.setState(NPCState.IDLE);
            guests.add(guest);
        }

        if (terry != null) {
            terry.say(TERRY_SPEECH_LINES[0], 6f);
        }
    }

    private void endLockIn() {
        lockInActive = false;
        lockInSuccessful = !raidInProgress && !playerGrassed && playerInLockIn;

        if (terry != null) {
            terry.kill();
            terry = null;
        }
        for (NPC guest : guests) {
            guest.setState(NPCState.WANDERING);
        }
        guests.clear();

        if (playerInLockIn) {
            lockInsAttended++;
            if (lockInSuccessful) {
                cleanLockIns++;
            }
            playerInLockIn = false;

            if (achievementSystem != null) {
                achievementSystem.increment(AchievementType.LOCK_IN_REGULAR);
                if (cleanLockIns >= 10) {
                    achievementSystem.unlock(AchievementType.LOCK_IN_LEGEND);
                }
            }
        }

        activeDartsGame = null;
        raidInProgress = false;
        hideWindowTimer = 0f;
    }

    // ── Raid logic ────────────────────────────────────────────────────────────

    private void updateRaidLogic(float delta, Player player, Inventory playerInventory) {
        if (raidInProgress) {
            // Count down the hide window
            if (hideWindowTimer > 0f) {
                hideWindowTimer = Math.max(0f, hideWindowTimer - delta);
                if (hideWindowTimer <= 0f && !playerHidSuccessfully && playerInLockIn) {
                    // Player failed to hide — record D&D charge
                    if (criminalRecord != null) {
                        criminalRecord.record(CriminalRecord.CrimeType.DRUNK_AND_DISORDERLY);
                    }
                    // Seed PLAYER_SPOTTED rumour
                    seedRaidRumour();
                    pendingRaidHeadline = true;
                    raidInProgress = false;
                }
            }
            return;
        }

        // Advance raid check timer
        raidCheckTimer += delta;
        if (raidCheckTimer >= RAID_CHECK_REAL_SECONDS) {
            raidCheckTimer = 0f;
            boolean raid = raidGuaranteed || (rng.nextFloat() < RAID_PROBABILITY);
            raidGuaranteed = false;
            if (raid) {
                triggerRaid();
            }
        }
    }

    private void triggerRaid() {
        raidInProgress = true;
        hideWindowTimer = HIDE_WINDOW_SECONDS;
        playerHidSuccessfully = false;

        if (terry != null) {
            terry.say("RAID! HIDE! GO GO GO!", 4f);
        }
    }

    private void seedRaidRumour() {
        if (rumourNetwork == null || terry == null) return;
        Rumour r = new Rumour(RumourType.PLAYER_SPOTTED,
                "Old Bill raided the lock-in at The Ragamuffin Arms — someone was nicked!");
        rumourNetwork.addRumour(terry, r);
    }

    // ── Pub quiz update ───────────────────────────────────────────────────────

    private void updatePubQuiz(float delta, float normHour, int dayOfWeek, int day) {
        boolean isThursdayQuizTime = (dayOfWeek == THURSDAY_INDEX)
                && normHour >= QUIZ_START_HOUR && normHour < QUIZ_END_HOUR;

        if (isThursdayQuizTime && !quizActive && (lastQuizDay != day)) {
            startQuiz(day);
        } else if (!isThursdayQuizTime && quizActive) {
            endQuiz();
        }

        if (!quizActive) return;

        quizQuestionTimer += delta;
        if (quizQuestionTimer >= QUIZ_QUESTION_DURATION) {
            quizQuestionTimer = 0f;
            // NPC team answers at 60% accuracy
            if (!playerAnsweredQuestion) {
                // Player didn't answer; NPC team takes a shot
            }
            if (rng.nextFloat() < NPC_QUIZ_ACCURACY) {
                npcTeamQuizScore++;
            }
            playerAnsweredQuestion = false;
            quizQuestionIndex++;
            if (quizQuestionIndex >= QUIZ_QUESTION_COUNT) {
                endQuiz();
            }
        }
    }

    private void startQuiz(int day) {
        quizActive = true;
        quizQuestionIndex = 0;
        playerQuizScore = 0;
        npcTeamQuizScore = 0;
        quizQuestionTimer = 0f;
        playerAnsweredQuestion = false;
        quizCompletedToday = false;
        lastQuizDay = day;
    }

    private void endQuiz() {
        quizActive = false;
        quizCompletedToday = true;

        if (playerInLockIn && playerQuizScore > npcTeamQuizScore) {
            // Player wins the quiz
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.QUIZ_NIGHT_CHAMPION);
            }
        }
    }

    // ── Player actions ────────────────────────────────────────────────────────

    /**
     * Player enters the lock-in (called when they are inside the pub at 23:00+).
     * Does nothing if the player has been ejected.
     *
     * @return true if the player successfully joined the lock-in
     */
    public boolean playerJoinLockIn() {
        if (!lockInActive) return false;
        if (playerEjected) {
            if (terry != null) terry.say("Not you. Get out.", 4f);
            return false;
        }
        if (!playerInLockIn) {
            playerInLockIn = true;
        }
        return true;
    }

    /**
     * Player buys a drink from Terry (half price).
     *
     * @param playerInventory the player's inventory
     * @return true if the drink was purchased
     */
    public boolean buyDrink(Inventory playerInventory) {
        if (!lockInActive || !playerInLockIn || terry == null) return false;
        if (playerInventory == null) return false;
        if (playerInventory.getItemCount(Material.COIN) < DRINK_PRICE) return false;

        playerInventory.removeItem(Material.COIN, DRINK_PRICE);
        playerInventory.addItem(Material.CAN_OF_LAGER, 1);
        terry.say("There you go. Lovely.", 3f);
        return true;
    }

    /**
     * Player presses E on the locked door — tips off the police.
     * Guarantees the next raid, unlocks GRASS achievement, adds Notoriety.
     * Terry remembers and permanently ejects the player.
     *
     * @return result of the tip-off
     */
    public TipOffResult tipOffPolice() {
        if (!lockInActive) return TipOffResult.LOCK_IN_NOT_ACTIVE;
        if (playerGrassed) return TipOffResult.ALREADY_GRASSED;

        playerGrassed = true;
        playerEjected = true;
        raidGuaranteed = true;

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.GRASS);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(TIP_OFF_NOTORIETY_GAIN,
                    achievementSystem != null ? type -> achievementSystem.unlock(type) : null);
        }

        return TipOffResult.SUCCESS;
    }

    /**
     * Player hides behind the bar counter during a raid.
     *
     * @return result of the hide attempt
     */
    public HideResult hideBehindBar() {
        if (!raidInProgress) return HideResult.NO_RAID_IN_PROGRESS;
        if (hideWindowTimer <= 0f) return HideResult.TOO_LATE;

        playerHidSuccessfully = true;
        raidInProgress = false;
        hideWindowTimer = 0f;

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.STAYED_BEHIND_THE_BAR);
        }

        return HideResult.SUCCESS;
    }

    /**
     * Start a darts game against an NPC opponent.
     *
     * @param coinStake optional COIN stake (0 = no stake)
     * @param playerInventory the player's inventory (to verify stake funds)
     * @return true if the game was started
     */
    public boolean startDartsGame(int coinStake, Inventory playerInventory) {
        if (!lockInActive || !playerInLockIn) return false;
        if (activeDartsGame != null && activeDartsGame.isActive()) return false;

        if (coinStake > 0 && playerInventory != null) {
            if (playerInventory.getItemCount(Material.COIN) < coinStake) return false;
        }

        activeDartsGame = new DartsMinigame(coinStake, rng);
        return true;
    }

    /**
     * Player throws a dart in the active darts game.
     * If the game ends and the player wins, awards COIN stake and achievement.
     *
     * @param playerInventory the player's inventory (for stake payout)
     * @return the score achieved, or 0 on bust / game not active
     */
    public int throwDart(Inventory playerInventory) {
        if (activeDartsGame == null || !activeDartsGame.isActive()) return 0;

        int score = activeDartsGame.playerThrow();

        if (!activeDartsGame.isActive()) {
            // Game over
            DartsResult result = activeDartsGame.getResult();
            int stake = activeDartsGame.getCoinStake();

            if (result == DartsResult.PLAYER_WIN) {
                if (stake > 0 && playerInventory != null) {
                    playerInventory.addItem(Material.COIN, stake * 2); // win double the stake
                }
                if (stake > 0 && achievementSystem != null) {
                    achievementSystem.unlock(AchievementType.DARTS_HUSTLER);
                }
            } else if (result == DartsResult.NPC_WIN) {
                // Deduct stake from player if NPC wins
                if (stake > 0 && playerInventory != null) {
                    playerInventory.removeItem(Material.COIN, Math.min(stake,
                            playerInventory.getItemCount(Material.COIN)));
                }
            }
        }

        return score;
    }

    /**
     * Player answers a pub quiz question (keys 1–4 map to answer index 1–4).
     *
     * @param answerIndex answer chosen (1–4)
     * @return result of the answer attempt
     */
    public QuizAnswerResult answerQuizQuestion(int answerIndex) {
        if (!quizActive) return QuizAnswerResult.QUIZ_NOT_ACTIVE;
        if (playerAnsweredQuestion) return QuizAnswerResult.ALREADY_ANSWERED;
        if (answerIndex < 1 || answerIndex > 4) return QuizAnswerResult.INCORRECT;

        playerAnsweredQuestion = true;

        if (quizQuestionIndex < QUIZ_QUESTION_COUNT) {
            String[] q = QUIZ_QUESTIONS[quizQuestionIndex];
            int correctIndex = Integer.parseInt(q[5]);
            if (answerIndex == correctIndex) {
                playerQuizScore++;
                return QuizAnswerResult.CORRECT;
            }
        }
        return QuizAnswerResult.INCORRECT;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Returns true if the pub lock-in is currently active (23:00–02:00). */
    public boolean isLockInActive() { return lockInActive; }

    /** Returns true if the player is currently participating in the lock-in. */
    public boolean isPlayerInLockIn() { return playerInLockIn; }

    /** Returns true if the player has been permanently ejected. */
    public boolean isPlayerEjected() { return playerEjected; }

    /** Returns Terry the Landlord NPC, or null if not active. */
    public NPC getTerry() { return terry; }

    /** Returns the list of guest NPCs during the lock-in. */
    public List<NPC> getGuests() { return guests; }

    /** Returns true if a police raid is currently in progress. */
    public boolean isRaidInProgress() { return raidInProgress; }

    /** Returns the remaining hide-window time in seconds. */
    public float getHideWindowTimer() { return hideWindowTimer; }

    /** Returns true if a raid headline is pending for NewspaperSystem. */
    public boolean pollPendingHeadline() {
        if (pendingRaidHeadline) {
            pendingRaidHeadline = false;
            return true;
        }
        return false;
    }

    /**
     * Returns true if the most recent lock-in ended successfully
     * (player attended and was not raided/grassed).
     * Used by BusSystem to boost DRUNK NPC probability on Night Bus.
     */
    public boolean isLockInSuccessful() { return lockInSuccessful; }

    /** Returns the active darts minigame, or null if none. */
    public DartsMinigame getActiveDartsGame() { return activeDartsGame; }

    /** Returns true if the pub quiz is currently active. */
    public boolean isQuizActive() { return quizActive; }

    /** Returns the current quiz question index (0-based). */
    public int getQuizQuestionIndex() { return quizQuestionIndex; }

    /** Returns the player's current quiz score. */
    public int getPlayerQuizScore() { return playerQuizScore; }

    /** Returns the NPC team's current quiz score. */
    public int getNpcTeamQuizScore() { return npcTeamQuizScore; }

    /** Returns the number of lock-ins the player has attended. */
    public int getLockInsAttended() { return lockInsAttended; }

    /** Returns the number of lock-ins completed without a D&amp;D charge. */
    public int getCleanLockIns() { return cleanLockIns; }

    /**
     * Force a lock-in to start immediately (for testing).
     */
    public void forceStartLockIn() {
        if (!lockInActive) {
            startLockIn();
        }
    }

    /**
     * Force a raid (for testing).
     */
    public void forceRaid() {
        if (lockInActive) {
            triggerRaid();
        }
    }
}

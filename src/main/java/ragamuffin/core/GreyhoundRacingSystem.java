package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Issue #983: Northfield Dog Track — Greyhound Racing System.
 *
 * <p>The Marchetti Crew run the Northfield Dog Track on the industrial fringe.
 * Open evenings (18:00–23:00) and Saturday afternoons (13:00–19:00).
 * Six named greyhounds race every 8 in-game minutes; player places fixed-odds
 * bets (1.5×–8×) at a Tote window via BettingUI.
 *
 * <h3>Core mechanics</h3>
 * <ul>
 *   <li>Races every {@link #RACE_INTERVAL_HOURS} in-game hours during open hours.</li>
 *   <li>Stake: 1–{@link #MAX_STAKE} coins.</li>
 *   <li>On bet placement a {@link Material#BET_SLIP} is added to inventory.</li>
 *   <li>Payout = stake × odds multiplier on win; BET_SLIP consumed on resolution.</li>
 * </ul>
 *
 * <h3>Illegal activities</h3>
 * <ul>
 *   <li><b>Bribe kennel hand:</b> 10 COIN, guarantees a dog loses next race.</li>
 *   <li><b>DODGY_PIE:</b> crafted from COLD_PASTRY + SUSPICIOUS_MEAT; reduces
 *       dog speed 30% for the next race.</li>
 *   <li><b>Night kennel heist:</b> LOCKPICK steals a GREYHOUND item, fenceable
 *       at Pawn Shop for 25–40 COIN. Adds ANIMAL_THEFT to CriminalRecord.</li>
 * </ul>
 *
 * <h3>Integrations</h3>
 * <ul>
 *   <li>{@link FactionSystem} — Marchetti run the track; Respect &lt; 30 = refused entry;
 *       Respect &ge; 60 = one insider tip per session.</li>
 *   <li>{@link NotorietySystem} — +10 for witnessed fixing; +3 for 3 consecutive wins.</li>
 *   <li>{@link NoiseSystem} — crowd noise level 3 masks stealth actions.</li>
 *   <li>{@link WeatherSystem} — rain reduces attendance; fog dims scoreboard.</li>
 *   <li>{@link CriminalRecord} — ANIMAL_THEFT, RACE_FIXING.</li>
 *   <li>{@link RumourNetwork} — DOG_HEIST and RACE_FIXED rumours.</li>
 *   <li>{@link PawnShopSystem} — greyhound valuation 25–40 COIN.</li>
 *   <li>{@link StreetEconomySystem} — RACE_CARD as BORED satisfier.</li>
 * </ul>
 */
public class GreyhoundRacingSystem {

    // ── Schedule constants ────────────────────────────────────────────────────

    /** Evening opening hour (18:00). */
    public static final float EVENING_OPEN_HOUR  = 18.0f;

    /** Evening closing hour (23:00). */
    public static final float EVENING_CLOSE_HOUR = 23.0f;

    /** Saturday afternoon opening hour (13:00). */
    public static final float SAT_OPEN_HOUR  = 13.0f;

    /** Saturday afternoon closing hour (19:00). */
    public static final float SAT_CLOSE_HOUR = 19.0f;

    /** Races run every 8 in-game minutes = 8/60 hours. */
    public static final float RACE_INTERVAL_HOURS = 8f / 60f;

    /** Number of greyhounds per race. */
    public static final int DOGS_PER_RACE = 6;

    // ── Stake limits ─────────────────────────────────────────────────────────

    /** Minimum stake in coins. */
    public static final int MIN_STAKE = 1;

    /** Maximum stake in coins. */
    public static final int MAX_STAKE = 20;

    // ── Odds pool (multipliers ×1.5 to ×8) ──────────────────────────────────

    /**
     * Odds multipliers assigned to the 6 traps.
     * One favourite (1.5×), two mid (2×, 3×), two longer (5×), one longest (8×).
     */
    private static final float[] ODDS_POOL = { 1.5f, 2.0f, 3.0f, 3.0f, 5.0f, 8.0f };

    // ── Bribery / fixing constants ────────────────────────────────────────────

    /** Cost to bribe the kennel hand (guarantees a dog loses). */
    public static final int BRIBE_COST = 10;

    /** Speed reduction when a DODGY_PIE is slipped to a dog. */
    public static final float DODGY_PIE_SPEED_PENALTY = 0.30f;

    /** Notoriety gained when race fixing is witnessed. */
    public static final int FIXING_NOTORIETY = 10;

    /** Notoriety gained after 3 consecutive wins. */
    public static final int CONSECUTIVE_WIN_NOTORIETY = 3;

    /** Consecutive wins required for the BROKE_THE_TOTE achievement and notoriety hit. */
    public static final int CONSECUTIVE_WINS_THRESHOLD = 3;

    // ── Greyhound heist constants ─────────────────────────────────────────────

    /** Minimum COIN value when fencing a GREYHOUND at the Pawn Shop. */
    public static final int GREYHOUND_FENCE_MIN = 25;

    /** Maximum COIN value when fencing a GREYHOUND at the Pawn Shop. */
    public static final int GREYHOUND_FENCE_MAX = 40;

    // ── Faction integration ───────────────────────────────────────────────────

    /** Marchetti Crew Respect required for entry. */
    public static final int ENTRY_RESPECT_MINIMUM = 30;

    /** Marchetti Crew Respect required for insider tip. */
    public static final int INSIDER_TIP_RESPECT = 60;

    // ── Greyhound names (British greyhound naming conventions) ────────────────

    private static final String[] GREYHOUND_NAMES = {
        "Northfield Nelly",   "Trap Six Terror",      "Dodgy Pasty",
        "Council Runner",     "Bin Night Special",    "Chip Shop Flyer",
        "Marchetti's Pride",  "Fence Jumper",         "Flat Cap Lightning",
        "Tracksuit Terrier",  "Last Pint Larry",      "Skip Dash",
        "Industrial Streak",  "Lager Legged Larry",   "Benefit Day Betty",
        "Estate Agent",       "Grey Skies Runner",    "Kebab Chaser",
        "Drizzle Dasher",     "Pavement Princess",    "Corner Shop Charlie",
        "Broken Biscuit",     "Soggy Bottom",         "Pigeon Fancier",
    };

    // ── Ambient dialogue for TRACK_PUNTER NPCs ────────────────────────────────

    static final String[] TRACK_PUNTER_DIALOGUE = {
        "Come on, Trap 4!",
        "Useless mutt.",
        "I had the winner last week.",
        "Rigged, that is.",
        "One more and I'm done.",
        "She's got good form, that one.",
        "My mate says Trap 3 is a cert.",
        "Twenty years I've been coming here.",
        "That dog's been got at, I reckon.",
        "Marchetti boys won't have it fixed. Probably."
    };

    // ── Inner types ───────────────────────────────────────────────────────────

    /** A single greyhound entry in a race. */
    public static class Greyhound {
        private final String name;
        private final int trapNumber;    // 1-6
        private final float oddsMultiplier;
        private boolean fixed;           // true if bribed/dodgy-pied to lose
        private float speedPenalty;      // 0.0–1.0 fraction of speed removed

        public Greyhound(String name, int trapNumber, float oddsMultiplier) {
            this.name = name;
            this.trapNumber = trapNumber;
            this.oddsMultiplier = oddsMultiplier;
            this.fixed = false;
            this.speedPenalty = 0.0f;
        }

        public String getName()          { return name; }
        public int getTrapNumber()       { return trapNumber; }
        public float getOddsMultiplier() { return oddsMultiplier; }
        public boolean isFixed()         { return fixed; }
        public float getSpeedPenalty()   { return speedPenalty; }

        void setFixed(boolean fixed)             { this.fixed = fixed; }
        void setSpeedPenalty(float speedPenalty) { this.speedPenalty = speedPenalty; }

        /** Effective win probability — lower for fixed/penalised dogs. */
        public float getEffectiveWinProbability() {
            // base probability inversely proportional to odds (higher odds = lower prob)
            float base = 1.0f / oddsMultiplier;
            if (fixed) {
                base *= 0.01f; // virtually no chance when fixed
            } else {
                base *= (1.0f - speedPenalty);
            }
            return base;
        }

        /** Formatted odds string, e.g. "3.0x". */
        public String getOddsString() {
            if (oddsMultiplier == (int) oddsMultiplier) {
                return (int) oddsMultiplier + "x";
            }
            return oddsMultiplier + "x";
        }
    }

    /** A scheduled greyhound race. */
    public static class Race {
        private final int raceIndex;
        private final float scheduledHour;
        private final List<Greyhound> dogs;
        private boolean resolved;
        private int winnerTrap; // -1 until resolved

        public Race(int raceIndex, float scheduledHour, List<Greyhound> dogs) {
            this.raceIndex = raceIndex;
            this.scheduledHour = scheduledHour;
            this.dogs = Collections.unmodifiableList(new ArrayList<>(dogs));
            this.resolved = false;
            this.winnerTrap = -1;
        }

        public int getRaceIndex()        { return raceIndex; }
        public float getScheduledHour()  { return scheduledHour; }
        public List<Greyhound> getDogs() { return dogs; }
        public boolean isResolved()      { return resolved; }
        public int getWinnerTrap()       { return winnerTrap; }

        public Greyhound getWinner() {
            if (!resolved) return null;
            for (Greyhound g : dogs) {
                if (g.getTrapNumber() == winnerTrap) return g;
            }
            return null;
        }

        void resolve(int winnerTrap) {
            this.winnerTrap = winnerTrap;
            this.resolved = true;
        }
    }

    /** An active bet placed by the player. */
    public static class Bet {
        private final int raceIndex;
        private final int trapNumber;
        private final int stake;

        public Bet(int raceIndex, int trapNumber, int stake) {
            this.raceIndex = raceIndex;
            this.trapNumber = trapNumber;
            this.stake = stake;
        }

        public int getRaceIndex()  { return raceIndex; }
        public int getTrapNumber() { return trapNumber; }
        public int getStake()      { return stake; }
    }

    /** Result codes for bet placement. */
    public enum BetResult {
        SUCCESS,
        ALREADY_BET,
        INVALID_RACE,
        RACE_ALREADY_RESOLVED,
        INVALID_TRAP,
        INVALID_STAKE,
        INSUFFICIENT_FUNDS,
        TRACK_CLOSED,
        ENTRY_REFUSED  // Marchetti Crew Respect < 30
    }

    /** Result codes for kennel hand bribery. */
    public enum BribeResult {
        SUCCESS,
        ALREADY_FIXED,
        INSUFFICIENT_FUNDS,
        KENNEL_HAND_ABSENT,
        WITNESSED  // security guard or kennel hand caught the player fixing
    }

    /** Result codes for the kennel heist. */
    public enum HeistResult {
        SUCCESS,
        NO_LOCKPICK,
        WITNESSED,
        TRACK_CLOSED
    }

    // ── System state ─────────────────────────────────────────────────────────

    private final Random random;

    /** Races for the current session. Rebuilt each session open. */
    private final List<Race> sessionRaces = new ArrayList<>();

    /** Current in-game day index (from TimeSystem.getDayCount()). */
    private int currentDay = -1;

    /** Current session open hour (may differ between eve/sat). */
    private float sessionOpenHour = -1f;

    /** Current session close hour. */
    private float sessionCloseHour = -1f;

    /** Active bet, or null. One bet allowed per race. */
    private Bet activeBet = null;

    /** Consecutive wins in the current session. */
    private int consecutiveWins = 0;

    /** Whether insider tip has been given this session. */
    private boolean insiderTipGivenThisSession = false;

    /** Whether the heist has been attempted this session. */
    private boolean heistAttemptedThisSession = false;

    /** Whether PUNTER achievement has been awarded. */
    private boolean punterAwarded = false;

    /** Whether LUCKY_DOG achievement has been awarded. */
    private boolean luckyDogAwarded = false;

    /** Whether TRACK_FIXER achievement has been awarded. */
    private boolean trackFixerAwarded = false;

    /** Whether NICKED_THE_GREYHOUND achievement has been awarded. */
    private boolean nickedGreyhoundAwarded = false;

    /** Whether BROKE_THE_TOTE achievement has been awarded. */
    private boolean brokeToteAwarded = false;

    // ── Construction ──────────────────────────────────────────────────────────

    public GreyhoundRacingSystem() {
        this(new Random());
    }

    public GreyhoundRacingSystem(Random random) {
        this.random = random;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Update the greyhound racing system. Call once per frame.
     *
     * @param delta              seconds since last frame
     * @param timeSystem         the game time system
     * @param playerInventory    player's inventory (for payouts and BET_SLIP management)
     * @param rumourNetwork      may be null; used to seed DOG_HEIST / RACE_FIXED
     * @param allNpcs            all NPCs in the world (may be null)
     * @param achievementCallback for unlocking achievements (may be null)
     * @param notorietySystem    for applying notoriety gains (may be null)
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       Inventory playerInventory,
                       RumourNetwork rumourNetwork,
                       List<NPC> allNpcs,
                       NotorietySystem.AchievementCallback achievementCallback,
                       NotorietySystem notorietySystem) {

        float currentHour = timeSystem.getTime();
        int day = timeSystem.getDayCount();

        // Check if we should open/rebuild the session schedule
        if (isTrackOpen(timeSystem)) {
            float openHour  = getSessionOpenHour(timeSystem);
            float closeHour = getSessionCloseHour(timeSystem);
            if (day != currentDay || sessionOpenHour != openHour) {
                buildSessionSchedule(day, openHour, closeHour);
            }
        } else {
            // Track is closed — reset session state when closing
            if (!sessionRaces.isEmpty() && !isTrackOpen(timeSystem)) {
                // Only reset when we transition from open to closed
                float prevClose = sessionCloseHour;
                if (prevClose > 0 && currentHour > prevClose) {
                    sessionRaces.clear();
                    insiderTipGivenThisSession = false;
                    heistAttemptedThisSession = false;
                    consecutiveWins = 0;
                    sessionOpenHour = -1f;
                }
            }
            return;
        }

        // Resolve races whose time has passed
        for (Race race : sessionRaces) {
            if (!race.isResolved() && currentHour >= race.getScheduledHour()) {
                resolveRace(race, playerInventory, rumourNetwork, allNpcs,
                        achievementCallback, notorietySystem);
            }
        }
    }

    // ── Track open/close logic ─────────────────────────────────────────────────

    /**
     * Returns true if the track is open at the current time.
     *
     * <p>Open: evenings 18:00–23:00 daily; Saturday 13:00–19:00.
     * Saturday is computed as (dayCount % 7 == 6).
     */
    public boolean isTrackOpen(TimeSystem timeSystem) {
        float hour = timeSystem.getTime();
        boolean satAfternoon = isSaturday(timeSystem)
                && hour >= SAT_OPEN_HOUR && hour < SAT_CLOSE_HOUR;
        if (satAfternoon) return true;
        return hour >= EVENING_OPEN_HOUR && hour < EVENING_CLOSE_HOUR;
    }

    /** Returns true if the current in-game day is Saturday (dayCount % 7 == 6). */
    public boolean isSaturday(TimeSystem timeSystem) {
        return timeSystem.getDayCount() % 7 == 6;
    }

    private float getSessionOpenHour(TimeSystem timeSystem) {
        float hour = timeSystem.getTime();
        if (isSaturday(timeSystem) && hour >= SAT_OPEN_HOUR && hour < SAT_CLOSE_HOUR) {
            return SAT_OPEN_HOUR;
        }
        return EVENING_OPEN_HOUR;
    }

    private float getSessionCloseHour(TimeSystem timeSystem) {
        float hour = timeSystem.getTime();
        if (isSaturday(timeSystem) && hour >= SAT_OPEN_HOUR && hour < SAT_CLOSE_HOUR) {
            return SAT_CLOSE_HOUR;
        }
        return EVENING_CLOSE_HOUR;
    }

    // ── Session schedule generation ────────────────────────────────────────────

    /**
     * Build the race schedule for a session.
     */
    private void buildSessionSchedule(int day, float openHour, float closeHour) {
        currentDay = day;
        sessionOpenHour = openHour;
        sessionCloseHour = closeHour;
        sessionRaces.clear();

        Random dayRandom = new Random((long) day * 77777L + (long)(openHour * 100));

        // Pick unique dog names for this session
        List<String> namePool = new ArrayList<>();
        for (String n : GREYHOUND_NAMES) namePool.add(n);
        while (namePool.size() < 60) { // enough for many races
            for (String n : GREYHOUND_NAMES) {
                namePool.add(n + " II");
                if (namePool.size() >= 60) break;
            }
        }
        Collections.shuffle(namePool, dayRandom);

        // Build races from openHour to closeHour - RACE_INTERVAL_HOURS
        int raceIndex = 0;
        int nameIdx = 0;
        float raceHour = openHour + RACE_INTERVAL_HOURS; // first race shortly after opening
        while (raceHour < closeHour - 0.01f) {
            List<Greyhound> dogs = buildRaceDogs(dayRandom, raceIndex, namePool, nameIdx);
            nameIdx += DOGS_PER_RACE;
            sessionRaces.add(new Race(raceIndex, raceHour, dogs));
            raceIndex++;
            raceHour += RACE_INTERVAL_HOURS;
        }
    }

    /**
     * Build the 6 greyhounds for a race with shuffled odds.
     */
    private List<Greyhound> buildRaceDogs(Random dayRandom, int raceIndex,
                                           List<String> namePool, int nameOffset) {
        // Shuffle odds across trap numbers
        float[] shuffledOdds = ODDS_POOL.clone();
        // Fisher-Yates
        for (int i = shuffledOdds.length - 1; i > 0; i--) {
            int j = dayRandom.nextInt(i + 1);
            float tmp = shuffledOdds[i];
            shuffledOdds[i] = shuffledOdds[j];
            shuffledOdds[j] = tmp;
        }

        List<Greyhound> dogs = new ArrayList<>();
        for (int i = 0; i < DOGS_PER_RACE; i++) {
            String name = namePool.get(nameOffset + i);
            dogs.add(new Greyhound(name, i + 1, shuffledOdds[i]));
        }
        return dogs;
    }

    // ── Race resolution ────────────────────────────────────────────────────────

    /**
     * Resolve a race: pick winner using effective probabilities, settle any active bet.
     */
    private void resolveRace(Race race,
                              Inventory playerInventory,
                              RumourNetwork rumourNetwork,
                              List<NPC> allNpcs,
                              NotorietySystem.AchievementCallback achievementCallback,
                              NotorietySystem notorietySystem) {

        // Pick winner weighted by effective probability
        int winnerTrap = pickWinner(race.getDogs());
        race.resolve(winnerTrap);

        // Settle active bet if it's on this race
        if (activeBet != null && activeBet.getRaceIndex() == race.getRaceIndex()) {
            settleBet(activeBet, race, playerInventory, rumourNetwork, allNpcs,
                    achievementCallback, notorietySystem);
            activeBet = null;
        }
    }

    /**
     * Pick a winner using weighted effective probabilities.
     */
    int pickWinner(List<Greyhound> dogs) {
        float total = 0f;
        float[] probs = new float[dogs.size()];
        for (int i = 0; i < dogs.size(); i++) {
            probs[i] = dogs.get(i).getEffectiveWinProbability();
            total += probs[i];
        }
        if (total <= 0f) {
            // Fallback: uniform
            return dogs.get(random.nextInt(dogs.size())).getTrapNumber();
        }

        float roll = random.nextFloat() * total;
        float cumulative = 0f;
        for (int i = 0; i < dogs.size(); i++) {
            cumulative += probs[i];
            if (roll < cumulative) {
                return dogs.get(i).getTrapNumber();
            }
        }
        return dogs.get(dogs.size() - 1).getTrapNumber();
    }

    /**
     * Settle a bet: pay out or record loss.
     */
    private void settleBet(Bet bet, Race race,
                            Inventory playerInventory,
                            RumourNetwork rumourNetwork,
                            List<NPC> allNpcs,
                            NotorietySystem.AchievementCallback achievementCallback,
                            NotorietySystem notorietySystem) {

        // Remove BET_SLIP
        if (playerInventory != null) {
            playerInventory.removeItem(Material.BET_SLIP, 1);
        }

        // Find the bet dog
        Greyhound betDog = null;
        for (Greyhound g : race.getDogs()) {
            if (g.getTrapNumber() == bet.getTrapNumber()) {
                betDog = g;
                break;
            }
        }
        if (betDog == null) return;

        boolean won = (bet.getTrapNumber() == race.getWinnerTrap());

        if (won) {
            int payout = (int)(bet.getStake() * betDog.getOddsMultiplier());
            if (playerInventory != null) {
                playerInventory.addItem(Material.COIN, payout + bet.getStake());
            }
            consecutiveWins++;

            // PUNTER achievement (first win)
            if (achievementCallback != null && !punterAwarded) {
                achievementCallback.award(AchievementType.PUNTER);
                punterAwarded = true;
            }

            // LUCKY_DOG achievement (8x or better)
            if (achievementCallback != null && !luckyDogAwarded
                    && betDog.getOddsMultiplier() >= 8.0f) {
                achievementCallback.award(AchievementType.LUCKY_DOG);
                luckyDogAwarded = true;
            }

            // BROKE_THE_TOTE achievement + notoriety for 3 consecutive wins
            if (consecutiveWins >= CONSECUTIVE_WINS_THRESHOLD) {
                if (achievementCallback != null && !brokeToteAwarded) {
                    achievementCallback.award(AchievementType.BROKE_THE_TOTE);
                    brokeToteAwarded = true;
                }
                if (notorietySystem != null) {
                    notorietySystem.addNotoriety(CONSECUTIVE_WIN_NOTORIETY, achievementCallback);
                }
            }
        } else {
            consecutiveWins = 0;
        }
    }

    // ── Bet placement ─────────────────────────────────────────────────────────

    /**
     * Place a bet on a greyhound (by trap number) in the given race.
     *
     * @param raceIndex       which race in the session (0-based)
     * @param trapNumber      trap number (1–6)
     * @param stake           coin amount (1–{@link #MAX_STAKE})
     * @param inventory       player's inventory
     * @param factionSystem   may be null; used to check Marchetti entry Respect
     * @param notorietySystem may be null
     * @param achievementCallback may be null
     * @param timeSystem      used to check track open status
     * @return BetResult describing outcome
     */
    public BetResult placeBet(int raceIndex, int trapNumber, int stake,
                               Inventory inventory,
                               FactionSystem factionSystem,
                               NotorietySystem notorietySystem,
                               NotorietySystem.AchievementCallback achievementCallback,
                               TimeSystem timeSystem) {

        // Check track open
        if (timeSystem != null && !isTrackOpen(timeSystem)) {
            return BetResult.TRACK_CLOSED;
        }

        // Check Marchetti entry respect
        if (factionSystem != null) {
            int respect = factionSystem.getRespect(Faction.MARCHETTI_CREW);
            if (respect < ENTRY_RESPECT_MINIMUM) {
                return BetResult.ENTRY_REFUSED;
            }
        }

        if (activeBet != null) return BetResult.ALREADY_BET;
        if (raceIndex < 0 || raceIndex >= sessionRaces.size()) return BetResult.INVALID_RACE;

        Race race = sessionRaces.get(raceIndex);
        if (race.isResolved()) return BetResult.RACE_ALREADY_RESOLVED;
        if (trapNumber < 1 || trapNumber > DOGS_PER_RACE) return BetResult.INVALID_TRAP;
        if (stake < MIN_STAKE || stake > MAX_STAKE) return BetResult.INVALID_STAKE;

        if (inventory.getItemCount(Material.COIN) < stake) return BetResult.INSUFFICIENT_FUNDS;

        inventory.removeItem(Material.COIN, stake);
        inventory.addItem(Material.BET_SLIP, 1);
        activeBet = new Bet(raceIndex, trapNumber, stake);

        // First bet ever: PUNTER achievement for just placing a bet
        if (achievementCallback != null && !punterAwarded) {
            achievementCallback.award(AchievementType.PUNTER);
            punterAwarded = true;
        }

        return BetResult.SUCCESS;
    }

    // ── Kennel hand bribery ───────────────────────────────────────────────────

    /**
     * Attempt to bribe the kennel hand to fix a dog to lose the next race.
     *
     * @param trapToFix         the trap number of the dog to fix (1–6)
     * @param raceIndex         the race to fix (must be unresolved)
     * @param inventory         player's inventory
     * @param kennelHandNearby  true if KENNEL_HAND NPC is within interaction range
     * @param witnessed         true if SECURITY_GUARD or another NPC witnessed the bribe
     * @param criminalRecord    may be null; records RACE_FIXING if witnessed
     * @param notorietySystem   may be null; adds FIXING_NOTORIETY if witnessed
     * @param achievementCallback may be null
     * @param rumourNetwork     may be null; seeds RACE_FIXED if witnessed
     * @param allNpcs           all NPCs (for seeding rumours)
     * @return BribeResult describing outcome
     */
    public BribeResult bribeKennelHand(int trapToFix, int raceIndex,
                                        Inventory inventory,
                                        boolean kennelHandNearby,
                                        boolean witnessed,
                                        CriminalRecord criminalRecord,
                                        NotorietySystem notorietySystem,
                                        NotorietySystem.AchievementCallback achievementCallback,
                                        RumourNetwork rumourNetwork,
                                        List<NPC> allNpcs) {
        if (!kennelHandNearby) return BribeResult.KENNEL_HAND_ABSENT;
        if (inventory.getItemCount(Material.COIN) < BRIBE_COST) return BribeResult.INSUFFICIENT_FUNDS;

        if (raceIndex < 0 || raceIndex >= sessionRaces.size()) return BribeResult.KENNEL_HAND_ABSENT;
        Race race = sessionRaces.get(raceIndex);
        if (race.isResolved()) return BribeResult.KENNEL_HAND_ABSENT;

        // Find the target dog
        Greyhound target = null;
        for (Greyhound g : race.getDogs()) {
            if (g.getTrapNumber() == trapToFix) { target = g; break; }
        }
        if (target == null) return BribeResult.KENNEL_HAND_ABSENT;
        if (target.isFixed()) return BribeResult.ALREADY_FIXED;

        inventory.removeItem(Material.COIN, BRIBE_COST);
        target.setFixed(true);

        if (witnessed) {
            if (criminalRecord != null) criminalRecord.record(CriminalRecord.CrimeType.RACE_FIXING);
            if (notorietySystem != null) notorietySystem.addNotoriety(FIXING_NOTORIETY, achievementCallback);
            seedRumour(rumourNetwork, allNpcs, RumourType.RACE_FIXED,
                    "Someone fixed the dogs down the track — bribed the kennel hand.");
            return BribeResult.WITNESSED;
        }

        // Award TRACK_FIXER achievement
        if (achievementCallback != null && !trackFixerAwarded) {
            achievementCallback.award(AchievementType.TRACK_FIXER);
            trackFixerAwarded = true;
        }

        return BribeResult.SUCCESS;
    }

    // ── Dodgy pie ─────────────────────────────────────────────────────────────

    /**
     * Slip a DODGY_PIE to a greyhound, reducing its speed by 30% for the next race.
     *
     * @param trapTarget     the trap number of the target dog (1–6)
     * @param raceIndex      the race index (must be unresolved)
     * @param inventory      player's inventory (must contain DODGY_PIE)
     * @param witnessed      true if SECURITY_GUARD or KENNEL_HAND witnessed the act
     * @param criminalRecord may be null; records RACE_FIXING if witnessed
     * @param notorietySystem may be null; adds FIXING_NOTORIETY if witnessed
     * @param achievementCallback may be null
     * @param rumourNetwork  may be null
     * @param allNpcs        all NPCs
     * @return true if the pie was successfully slipped
     */
    public boolean slipDodgyPie(int trapTarget, int raceIndex,
                                  Inventory inventory,
                                  boolean witnessed,
                                  CriminalRecord criminalRecord,
                                  NotorietySystem notorietySystem,
                                  NotorietySystem.AchievementCallback achievementCallback,
                                  RumourNetwork rumourNetwork,
                                  List<NPC> allNpcs) {

        if (inventory.getItemCount(Material.DODGY_PIE) < 1) return false;
        if (raceIndex < 0 || raceIndex >= sessionRaces.size()) return false;

        Race race = sessionRaces.get(raceIndex);
        if (race.isResolved()) return false;

        Greyhound target = null;
        for (Greyhound g : race.getDogs()) {
            if (g.getTrapNumber() == trapTarget) { target = g; break; }
        }
        if (target == null) return false;

        inventory.removeItem(Material.DODGY_PIE, 1);
        target.setSpeedPenalty(DODGY_PIE_SPEED_PENALTY);

        if (witnessed) {
            if (criminalRecord != null) criminalRecord.record(CriminalRecord.CrimeType.RACE_FIXING);
            if (notorietySystem != null) notorietySystem.addNotoriety(FIXING_NOTORIETY, achievementCallback);
            seedRumour(rumourNetwork, allNpcs, RumourType.RACE_FIXED,
                    "Someone slipped something to one of the dogs at the track. Scandalous.");
        } else {
            // Award TRACK_FIXER achievement
            if (achievementCallback != null && !trackFixerAwarded) {
                achievementCallback.award(AchievementType.TRACK_FIXER);
                trackFixerAwarded = true;
            }
        }

        return true;
    }

    // ── Night kennel heist ────────────────────────────────────────────────────

    /**
     * Attempt the night kennel heist — steal a greyhound using a LOCKPICK.
     *
     * @param inventory      player's inventory (must contain LOCKPICK)
     * @param witnessed      true if SECURITY_GUARD or KENNEL_HAND witnessed the act
     * @param timeSystem     used to check night access (track should be closed)
     * @param criminalRecord may be null
     * @param notorietySystem may be null
     * @param achievementCallback may be null
     * @param rumourNetwork  may be null
     * @param allNpcs        all NPCs
     * @return HeistResult describing outcome
     */
    public HeistResult attemptKennelHeist(Inventory inventory,
                                           boolean witnessed,
                                           TimeSystem timeSystem,
                                           CriminalRecord criminalRecord,
                                           NotorietySystem notorietySystem,
                                           NotorietySystem.AchievementCallback achievementCallback,
                                           RumourNetwork rumourNetwork,
                                           List<NPC> allNpcs) {

        if (inventory.getItemCount(Material.LOCKPICK) < 1) return HeistResult.NO_LOCKPICK;

        // Track should be closed for the heist (night access)
        if (timeSystem != null && isTrackOpen(timeSystem)) return HeistResult.TRACK_CLOSED;

        if (witnessed) {
            if (criminalRecord != null) criminalRecord.record(CriminalRecord.CrimeType.ANIMAL_THEFT);
            if (notorietySystem != null) notorietySystem.addNotoriety(FIXING_NOTORIETY, achievementCallback);
            seedRumour(rumourNetwork, allNpcs, RumourType.DOG_HEIST,
                    "Heard a dog went missing from the track kennels last night — Marchetti's not happy.");
            return HeistResult.WITNESSED;
        }

        // Consume LOCKPICK
        inventory.removeItem(Material.LOCKPICK, 1);

        // Grant GREYHOUND item (with random fence value baked into display name)
        inventory.addItem(Material.GREYHOUND, 1);

        if (criminalRecord != null) criminalRecord.record(CriminalRecord.CrimeType.ANIMAL_THEFT);

        seedRumour(rumourNetwork, allNpcs, RumourType.DOG_HEIST,
                "Heard a dog went missing from the track kennels last night — Marchetti's not happy.");

        if (achievementCallback != null && !nickedGreyhoundAwarded) {
            achievementCallback.award(AchievementType.NICKED_THE_GREYHOUND);
            nickedGreyhoundAwarded = true;
        }

        heistAttemptedThisSession = true;
        return HeistResult.SUCCESS;
    }

    // ── Insider tip ───────────────────────────────────────────────────────────

    /**
     * Request an insider tip from the TOTE_CLERK (requires Marchetti Respect >= 60).
     *
     * <p>Returns the trap number of the dog least likely to win in the next unresolved race,
     * or -1 if no tip is available (wrong respect level, or tip already given this session).
     *
     * @param factionSystem the faction system to check Marchetti Respect
     * @return trap number of the weakest dog (1–6), or -1 if unavailable
     */
    public int requestInsiderTip(FactionSystem factionSystem) {
        if (factionSystem == null) return -1;
        if (factionSystem.getRespect(Faction.MARCHETTI_CREW) < INSIDER_TIP_RESPECT) return -1;
        if (insiderTipGivenThisSession) return -1;

        Race nextRace = getNextUnresolvedRace();
        if (nextRace == null) return -1;

        // Find the dog with the lowest win probability (highest odds = longest shot)
        Greyhound weakest = null;
        float weakestProb = Float.MAX_VALUE;
        for (Greyhound g : nextRace.getDogs()) {
            float prob = g.getEffectiveWinProbability();
            if (prob < weakestProb) {
                weakestProb = prob;
                weakest = g;
            }
        }

        if (weakest == null) return -1;

        insiderTipGivenThisSession = true;
        return weakest.getTrapNumber();
    }

    // ── Greyhound fence valuation ─────────────────────────────────────────────

    /**
     * Returns the Pawn Shop valuation for a stolen GREYHOUND item.
     * Range: 25–40 COIN (seeded by this system's random).
     */
    public int getGreyhoundFenceValue() {
        return GREYHOUND_FENCE_MIN + random.nextInt(GREYHOUND_FENCE_MAX - GREYHOUND_FENCE_MIN + 1);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public List<Race> getSessionRaces() {
        return Collections.unmodifiableList(sessionRaces);
    }

    public Race getRace(int index) {
        if (index < 0 || index >= sessionRaces.size()) return null;
        return sessionRaces.get(index);
    }

    public Race getNextUnresolvedRace() {
        for (Race r : sessionRaces) {
            if (!r.isResolved()) return r;
        }
        return null;
    }

    public Bet getActiveBet() {
        return activeBet;
    }

    public int getConsecutiveWins() {
        return consecutiveWins;
    }

    public boolean isInsiderTipGivenThisSession() {
        return insiderTipGivenThisSession;
    }

    public boolean hasTodaysSchedule() {
        return !sessionRaces.isEmpty();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void seedRumour(RumourNetwork rumourNetwork,
                             List<NPC> allNpcs,
                             RumourType type,
                             String text) {
        if (rumourNetwork == null || allNpcs == null) return;
        Rumour rumour = new Rumour(type, text);
        for (NPC npc : allNpcs) {
            if (npc.isAlive()) {
                rumourNetwork.addRumour(npc, rumour);
                break;
            }
        }
    }
}

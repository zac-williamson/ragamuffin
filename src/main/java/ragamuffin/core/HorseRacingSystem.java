package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #908: Bookies Horse Racing System — 'The Daily Flutter'
 *
 * <p>8 races per in-game day (11:00–21:00, roughly 1.25 hr spacing), each with
 * 6 named horses and seeded odds (2/1 favourite through 33/1 rank outsider).
 *
 * <h3>Core mechanics</h3>
 * <ul>
 *   <li>Player interacts with a TV_SCREEN prop (E key) inside the bookies to open BettingUI.</li>
 *   <li>Stake is 1–50 coins (100 on BENEFIT_DAY).</li>
 *   <li>On bet placement a {@link Material#BET_SLIP} is added to inventory.</li>
 *   <li>Race resolves at its scheduled time via a {@link TimeSystem} tick.</li>
 *   <li>Payout = stake × odds numerator on win; BET_SLIP consumed on resolution.</li>
 * </ul>
 *
 * <h3>Debt &amp; tension</h3>
 * <ul>
 *   <li>{@link NPCType#LOAN_SHARK} spawns when cumulative net loss hits 50 coins.</li>
 *   <li>Loan: 20 coins, repay 30 within 3 in-game days or LOAN_SHARK turns hostile.</li>
 *   <li>Hostile: 2 STREET_LAD enforcers spawn and pursue; WantedSystem +1 star.</li>
 * </ul>
 *
 * <h3>Integrations</h3>
 * <ul>
 *   <li>{@link TimeSystem} — call {@link #update(float, TimeSystem, Inventory,
 *       RumourNetwork, List, NotorietySystem.AchievementCallback)} each frame.</li>
 *   <li>{@link StreetEconomySystem} — BENEFIT_DAY doubles max stake to 100.</li>
 *   <li>{@link NotorietySystem} — +5 if using COUNTERFEIT_NOTE to pay.</li>
 *   <li>{@link RumourNetwork} — BIG_WIN_AT_BOOKIES rumour seeded on 33/1 win.</li>
 * </ul>
 */
public class HorseRacingSystem {

    // ── Race schedule constants ───────────────────────────────────────────────

    /** Number of races per day. */
    public static final int RACES_PER_DAY = 8;

    /** First race time (11:00). */
    public static final float FIRST_RACE_HOUR = 11.0f;

    /** Last race time (21:00). */
    public static final float LAST_RACE_HOUR = 21.0f;

    /** Spacing between races in hours. */
    public static final float RACE_SPACING_HOURS =
            (LAST_RACE_HOUR - FIRST_RACE_HOUR) / (RACES_PER_DAY - 1);

    /** Number of horses per race. */
    public static final int HORSES_PER_RACE = 6;

    // ── Stake limits ─────────────────────────────────────────────────────────

    /** Minimum stake in coins. */
    public static final int MIN_STAKE = 1;

    /** Normal maximum stake in coins. */
    public static final int MAX_STAKE_NORMAL = 50;

    /** Maximum stake on BENEFIT_DAY. */
    public static final int MAX_STAKE_BENEFIT_DAY = 100;

    // ── Debt mechanics ───────────────────────────────────────────────────────

    /** Cumulative net-loss threshold that triggers LOAN_SHARK spawn. */
    public static final int LOAN_SHARK_TRIGGER_LOSS = 50;

    /** Loan amount offered by LOAN_SHARK. */
    public static final int LOAN_AMOUNT = 20;

    /** Amount player must repay (interest included). */
    public static final int LOAN_REPAY_AMOUNT = 30;

    /** In-game days before repayment is overdue. */
    public static final int LOAN_REPAY_DAYS = 3;

    // ── NotorietySystem integration ──────────────────────────────────────────

    /** Notoriety gain for placing a bet with COUNTERFEIT_NOTE. */
    public static final int COUNTERFEIT_BET_NOTORIETY_GAIN = 5;

    // ── Horse name pool (30 British-themed names) ─────────────────────────────

    private static final String[] HORSE_NAMES = {
        "Broken Biscuit",    "Council Flat",        "Missed the Bus",
        "Soggy Bottom",      "Flat Cap Fred",        "Greggs Galloper",
        "Wet Weekend",       "Benefits Day",         "Dodgy Motor",
        "Corner Shop Dash",  "Chip Shop Charlie",    "Grey Skies Runner",
        "Last Orders",       "Bin Night Special",    "Pavement Prince",
        "Lager Lout",        "Charity Shop Find",    "Estate Agent",
        "Tea and Sympathy",  "Drizzle Dash",         "Pound Shop Punter",
        "Pigeon Fancier",    "Wheelie Bin Williams", "Tracksuit Terror",
        "Skip Diver",        "Newspaper Parcel",     "Bus Stop Brian",
        "Allotment Ace",     "Rotary Club Runner",   "Crown Green Carol"
    };

    /**
     * Odds structures: [numerator, denominator].
     * Probability ≈ denominator/(numerator+denominator).
     * One favourite (2/1), two second-favourites (4/1), two mid-odds (6/1),
     * one 10/1, one 16/1, one 33/1 — but we have 6 horses per race:
     * 1 at 2/1 fav, 2 at 4/1, 2 at 6/1, 1 at 10/1.
     * For 33/1 wins, a special "rank outsider" slot is occasionally used.
     */
    private static final int[][] ODDS_POOL = {
        {2,  1},  // favourite (prob ~33%)
        {4,  1},  // second fav A (prob ~20%)
        {4,  1},  // second fav B (prob ~20%)
        {6,  1},  // mid-odds A (prob ~14%)
        {6,  1},  // mid-odds B (prob ~14%)
        {10, 1},  // long shot (prob ~9%)
    };

    /**
     * When the rank outsider slot replaces one of the mid-odds slots.
     * Probability ~3%.
     */
    private static final int[] OUTSIDER_ODDS = {33, 1};

    // ── Flavour dialogue for ambient NPCs at bookies ──────────────────────────

    static final String[] PUNTER_DIALOGUE = {
        "Come on my son!",
        "Useless horse.",
        "Just one more.",
        "She's a certainty, that one.",
        "Oi, who picked that nag?",
        "My mate says it's a sure thing.",
        "Right, last bet, promise.",
        "Form means nothing on a wet pitch.",
        "I've been coming 'ere twenty years.",
        "The favourite's been pulled. I knew it."
    };

    // ── Inner types ───────────────────────────────────────────────────────────

    /** Represents one horse in a race. */
    public static class Horse {
        private final String name;
        private final int oddsNumerator;
        private final int oddsDenominator;

        public Horse(String name, int oddsNumerator, int oddsDenominator) {
            this.name = name;
            this.oddsNumerator = oddsNumerator;
            this.oddsDenominator = oddsDenominator;
        }

        public String getName() { return name; }
        public int getOddsNumerator() { return oddsNumerator; }
        public int getOddsDenominator() { return oddsDenominator; }

        /** Formatted odds string e.g. "4/1". */
        public String getOddsString() {
            return oddsNumerator + "/" + oddsDenominator;
        }

        /** Win probability implied by the odds. */
        public float getWinProbability() {
            return (float) oddsDenominator / (oddsNumerator + oddsDenominator);
        }
    }

    /** A scheduled race with its horses and result. */
    public static class Race {
        private final int raceIndex; // 0-7
        private final float scheduledHour;
        private final List<Horse> horses;
        private boolean resolved;
        private int winnerIndex; // -1 until resolved

        public Race(int raceIndex, float scheduledHour, List<Horse> horses) {
            this.raceIndex = raceIndex;
            this.scheduledHour = scheduledHour;
            this.horses = horses;
            this.resolved = false;
            this.winnerIndex = -1;
        }

        public int getRaceIndex() { return raceIndex; }
        public float getScheduledHour() { return scheduledHour; }
        public List<Horse> getHorses() { return horses; }
        public boolean isResolved() { return resolved; }
        public int getWinnerIndex() { return winnerIndex; }

        public Horse getWinner() {
            return resolved ? horses.get(winnerIndex) : null;
        }

        void resolve(int winnerIndex) {
            this.winnerIndex = winnerIndex;
            this.resolved = true;
        }
    }

    /** An active bet placed by the player. */
    public static class Bet {
        private final int raceIndex;
        private final int horseIndex;
        private final int stake;
        private final boolean paidWithCounterfeit;

        public Bet(int raceIndex, int horseIndex, int stake, boolean paidWithCounterfeit) {
            this.raceIndex = raceIndex;
            this.horseIndex = horseIndex;
            this.stake = stake;
            this.paidWithCounterfeit = paidWithCounterfeit;
        }

        public int getRaceIndex() { return raceIndex; }
        public int getHorseIndex() { return horseIndex; }
        public int getStake() { return stake; }
        public boolean isPaidWithCounterfeit() { return paidWithCounterfeit; }
    }

    /** Outcome of a bet resolution. */
    public enum BetOutcome { WIN, LOSS }

    /** Loan state. */
    public enum LoanState { NONE, OFFERED, TAKEN, REPAID, DEFAULTED }

    // ── System state ─────────────────────────────────────────────────────────

    private final Random random;

    /** Today's races. Rebuilt each day. */
    private List<Race> todaysRaces = new ArrayList<>();

    /** Current in-game day index (from TimeSystem.getDayCount()). */
    private int currentDay = -1;

    /** Active bet, or null. Only one bet allowed per race. */
    private Bet activeBet = null;

    /** Cumulative net loss (losses minus wins). Triggers loan shark at 50. */
    private int cumulativeNetLoss = 0;

    /** Total winnings ever. */
    private int totalWinnings = 0;

    /** Total stakes ever lost. */
    private int totalLosses = 0;

    /** Number of races bet on today (for DAILY_PUNTER achievement). */
    private int racesTodayBetOn = 0;
    private int raceDayTracker = -1;

    // Loan state
    private LoanState loanState = LoanState.NONE;
    private int loanTakenDay = -1;

    // Achievement flags
    private boolean luckyPuntAwarded = false;
    private boolean outsiderAwarded = false;
    private boolean rankOutsiderAwarded = false;
    private boolean losingStreakAwarded = false;
    private boolean debtFreeAwarded = false;
    private boolean dailyPunterAwarded = false;

    // ── Construction ──────────────────────────────────────────────────────────

    public HorseRacingSystem() {
        this(new Random());
    }

    public HorseRacingSystem(Random random) {
        this.random = random;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update the horse racing system. Call once per frame.
     *
     * @param delta              seconds since last frame
     * @param timeSystem         the game time system
     * @param playerInventory    player's inventory (for payouts and BET_SLIP management)
     * @param rumourNetwork      may be null; used to seed BIG_WIN_AT_BOOKIES on 33/1 win
     * @param allNpcs            all NPCs in the world (for BIG_WIN crowd draw)
     * @param achievementCallback for unlocking achievements (may be null)
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       Inventory playerInventory,
                       RumourNetwork rumourNetwork,
                       List<NPC> allNpcs,
                       NotorietySystem.AchievementCallback achievementCallback) {

        int day = timeSystem.getDayCount();

        // Rebuild schedule each new day
        if (day != currentDay) {
            buildDaySchedule(day, timeSystem);
        }

        // Check if any unresolved race has passed its time
        float currentHour = timeSystem.getTime();
        for (Race race : todaysRaces) {
            if (!race.isResolved() && currentHour >= race.getScheduledHour()) {
                resolveRace(race, playerInventory, rumourNetwork, allNpcs, achievementCallback);
            }
        }
    }

    // ── Schedule generation ───────────────────────────────────────────────────

    /**
     * Build today's race schedule. Called once per day.
     */
    private void buildDaySchedule(int day, TimeSystem timeSystem) {
        currentDay = day;
        todaysRaces = new ArrayList<>();
        // Use day as seed for reproducibility within a day
        Random dayRandom = new Random((long) day * 31337L);

        // Pick 48 unique horse names for the day (8 races × 6 horses)
        List<String> namePool = new ArrayList<>();
        for (String name : HORSE_NAMES) {
            namePool.add(name);
        }
        // If pool is small, repeat with suffix
        while (namePool.size() < RACES_PER_DAY * HORSES_PER_RACE) {
            for (String name : HORSE_NAMES) {
                namePool.add(name + " II");
                if (namePool.size() >= RACES_PER_DAY * HORSES_PER_RACE) break;
            }
        }
        // Shuffle the name pool
        java.util.Collections.shuffle(namePool, dayRandom);

        int nameIdx = 0;
        for (int i = 0; i < RACES_PER_DAY; i++) {
            float hour = FIRST_RACE_HOUR + i * RACE_SPACING_HOURS;
            List<Horse> horses = new ArrayList<>();

            // Build odds for this race — one of the ODDS_POOL slots may be replaced by 33/1
            int[][] raceOdds = buildRaceOdds(dayRandom, i);

            for (int h = 0; h < HORSES_PER_RACE; h++) {
                String horseName = namePool.get(nameIdx++);
                int oddsNum = raceOdds[h][0];
                int oddsDen = raceOdds[h][1];
                horses.add(new Horse(horseName, oddsNum, oddsDen));
            }

            todaysRaces.add(new Race(i, hour, horses));
        }

        // Reset daily bet tracker if it's a new day
        if (raceDayTracker != day) {
            racesTodayBetOn = 0;
            raceDayTracker = day;
        }
    }

    /**
     * Build odds for a race. One in every RACES_PER_DAY races includes a 33/1 horse
     * (replacing the 10/1 long shot).
     */
    private int[][] buildRaceOdds(Random dayRandom, int raceIndex) {
        int[][] odds = new int[HORSES_PER_RACE][2];
        // Shuffle base odds pool
        int[] shuffleIdx = {0, 1, 2, 3, 4, 5};
        // Fisher-Yates
        for (int i = 5; i > 0; i--) {
            int j = dayRandom.nextInt(i + 1);
            int tmp = shuffleIdx[i];
            shuffleIdx[i] = shuffleIdx[j];
            shuffleIdx[j] = tmp;
        }
        for (int h = 0; h < HORSES_PER_RACE; h++) {
            int poolIdx = shuffleIdx[h];
            odds[h][0] = ODDS_POOL[poolIdx][0];
            odds[h][1] = ODDS_POOL[poolIdx][1];
        }

        // One designated race per day has a 33/1 outsider (replaces the 10/1 long shot)
        // We pick race at index 0 for simplicity (seeded with day)
        int outsiderRace = dayRandom.nextInt(RACES_PER_DAY);
        if (raceIndex == outsiderRace) {
            // Find the 10/1 slot and replace it
            for (int h = 0; h < HORSES_PER_RACE; h++) {
                if (odds[h][0] == 10) {
                    odds[h][0] = OUTSIDER_ODDS[0];
                    odds[h][1] = OUTSIDER_ODDS[1];
                    break;
                }
            }
        }

        return odds;
    }

    // ── Race resolution ───────────────────────────────────────────────────────

    /**
     * Resolve a race using weighted RNG matching stated odds.
     * If the player has an active bet on this race, calculate payout or loss.
     */
    private void resolveRace(Race race,
                             Inventory playerInventory,
                             RumourNetwork rumourNetwork,
                             List<NPC> allNpcs,
                             NotorietySystem.AchievementCallback achievementCallback) {

        // Pick winner using weighted probability
        int winner = pickWinner(race.getHorses());
        race.resolve(winner);

        // If player had a bet on this race, settle it
        if (activeBet != null && activeBet.getRaceIndex() == race.getRaceIndex()) {
            settleBet(activeBet, race, playerInventory, rumourNetwork, allNpcs, achievementCallback);
            activeBet = null;
        }
    }

    /**
     * Pick a winner using weighted probability based on horse odds.
     */
    int pickWinner(List<Horse> horses) {
        // Build cumulative probability array
        float total = 0f;
        float[] probs = new float[horses.size()];
        for (int i = 0; i < horses.size(); i++) {
            probs[i] = horses.get(i).getWinProbability();
            total += probs[i];
        }

        float roll = random.nextFloat() * total;
        float cumulative = 0f;
        for (int i = 0; i < horses.size(); i++) {
            cumulative += probs[i];
            if (roll < cumulative) {
                return i;
            }
        }
        return horses.size() - 1; // fallback
    }

    /**
     * Settle a bet after race resolution.
     */
    private void settleBet(Bet bet, Race race,
                           Inventory playerInventory,
                           RumourNetwork rumourNetwork,
                           List<NPC> allNpcs,
                           NotorietySystem.AchievementCallback achievementCallback) {

        // Remove BET_SLIP from inventory
        if (playerInventory != null) {
            playerInventory.removeItem(Material.BET_SLIP, 1);
        }

        boolean won = (bet.getHorseIndex() == race.getWinnerIndex());
        Horse pickedHorse = race.getHorses().get(bet.getHorseIndex());

        if (won) {
            int payout = bet.getStake() * pickedHorse.getOddsNumerator();
            if (playerInventory != null) {
                playerInventory.addItem(Material.COIN, payout + bet.getStake()); // stake returned + winnings
            }
            totalWinnings += payout;
            cumulativeNetLoss -= (payout + bet.getStake()); // net improve

            // Achievements
            if (achievementCallback != null) {
                if (!luckyPuntAwarded) {
                    achievementCallback.award(AchievementType.LUCKY_PUNT);
                    luckyPuntAwarded = true;
                }
                if (!outsiderAwarded && pickedHorse.getOddsNumerator() >= 10) {
                    achievementCallback.award(AchievementType.OUTSIDER);
                    outsiderAwarded = true;
                }
                if (!rankOutsiderAwarded && pickedHorse.getOddsNumerator() >= 33) {
                    achievementCallback.award(AchievementType.RANK_OUTSIDER);
                    rankOutsiderAwarded = true;
                }
            }

            // 33/1 win: seed BIG_WIN_AT_BOOKIES rumour
            if (pickedHorse.getOddsNumerator() >= 33 && rumourNetwork != null && allNpcs != null) {
                Rumour rumour = new Rumour(RumourType.BIG_WIN_AT_BOOKIES,
                    "Someone just cleaned up at the bookies on a 33/1 shot — absolute madness!");
                for (NPC npc : allNpcs) {
                    if (npc.isAlive()) {
                        rumourNetwork.addRumour(npc, rumour);
                        break; // seed one NPC; it will spread
                    }
                }
            }
        } else {
            // Loss: stake already deducted when bet was placed
            cumulativeNetLoss += bet.getStake();
            totalLosses += bet.getStake();

            // LOSING_STREAK achievement
            if (achievementCallback != null && !losingStreakAwarded && cumulativeNetLoss >= 50) {
                achievementCallback.award(AchievementType.LOSING_STREAK);
                losingStreakAwarded = true;
            }
        }
    }

    // ── Bet placement ─────────────────────────────────────────────────────────

    /**
     * Place a bet on a horse in the given race.
     *
     * <p>Deducts the stake from the player's inventory, adds a BET_SLIP.
     * Applies Notoriety penalty if paid with COUNTERFEIT_NOTE.
     *
     * @param raceIndex    which race (0–7)
     * @param horseIndex   which horse in that race (0–5)
     * @param stake        coin amount to stake (1–50, or 1–100 on BENEFIT_DAY)
     * @param inventory    player's inventory
     * @param notorietySystem may be null
     * @param achievementCallback may be null
     * @param isBenefitDay true if BENEFIT_DAY market event is active
     * @return BetResult describing outcome
     */
    public BetResult placeBet(int raceIndex, int horseIndex, int stake,
                              Inventory inventory,
                              NotorietySystem notorietySystem,
                              NotorietySystem.AchievementCallback achievementCallback,
                              boolean isBenefitDay) {

        // Validation
        if (activeBet != null) {
            return BetResult.ALREADY_BET;
        }
        if (raceIndex < 0 || raceIndex >= todaysRaces.size()) {
            return BetResult.INVALID_RACE;
        }
        Race race = todaysRaces.get(raceIndex);
        if (race.isResolved()) {
            return BetResult.RACE_ALREADY_RESOLVED;
        }
        if (horseIndex < 0 || horseIndex >= race.getHorses().size()) {
            return BetResult.INVALID_HORSE;
        }

        int maxStake = isBenefitDay ? MAX_STAKE_BENEFIT_DAY : MAX_STAKE_NORMAL;
        if (stake < MIN_STAKE || stake > maxStake) {
            return BetResult.INVALID_STAKE;
        }

        // Check payment
        boolean usingCounterfeit = false;
        int coinCount = inventory.getItemCount(Material.COIN);
        int counterfeitCount = inventory.getItemCount(Material.COUNTERFEIT_NOTE);

        if (coinCount >= stake) {
            inventory.removeItem(Material.COIN, stake);
        } else if (counterfeitCount > 0 && coinCount + counterfeitCount >= stake) {
            // Use coins first, then counterfeit notes
            int coinUsed = Math.min(coinCount, stake);
            int counterfeitUsed = stake - coinUsed;
            if (coinUsed > 0) inventory.removeItem(Material.COIN, coinUsed);
            inventory.removeItem(Material.COUNTERFEIT_NOTE, counterfeitUsed);
            usingCounterfeit = true;
        } else {
            return BetResult.INSUFFICIENT_FUNDS;
        }

        // Apply notoriety penalty for counterfeit payment
        if (usingCounterfeit && notorietySystem != null) {
            notorietySystem.addNotoriety(COUNTERFEIT_BET_NOTORIETY_GAIN, achievementCallback);
        }

        // Place bet
        activeBet = new Bet(raceIndex, horseIndex, stake, usingCounterfeit);
        inventory.addItem(Material.BET_SLIP, 1);

        // Track daily bet count
        if (raceDayTracker == currentDay) {
            racesTodayBetOn++;
        } else {
            racesTodayBetOn = 1;
            raceDayTracker = currentDay;
        }

        // DAILY_PUNTER achievement
        if (achievementCallback != null && !dailyPunterAwarded && racesTodayBetOn >= RACES_PER_DAY) {
            achievementCallback.award(AchievementType.DAILY_PUNTER);
            dailyPunterAwarded = true;
        }

        return BetResult.SUCCESS;
    }

    /** Result codes for bet placement. */
    public enum BetResult {
        SUCCESS,
        ALREADY_BET,
        INVALID_RACE,
        RACE_ALREADY_RESOLVED,
        INVALID_HORSE,
        INVALID_STAKE,
        INSUFFICIENT_FUNDS
    }

    // ── Loan shark mechanics ──────────────────────────────────────────────────

    /**
     * Check if a LOAN_SHARK should spawn. Returns true if the threshold has been
     * crossed and no loan is currently active/defaulted.
     */
    public boolean shouldSpawnLoanShark() {
        return cumulativeNetLoss >= LOAN_SHARK_TRIGGER_LOSS
                && loanState == LoanState.NONE;
    }

    /**
     * Record that the loan shark has offered a loan (spawned and approached player).
     */
    public void offerLoan(int day) {
        if (loanState == LoanState.NONE) {
            loanState = LoanState.OFFERED;
        }
    }

    /**
     * Player accepts the loan: add 20 coins to inventory, record day taken.
     *
     * @return true if accepted successfully
     */
    public boolean acceptLoan(Inventory inventory, int currentDay) {
        if (loanState != LoanState.OFFERED) return false;
        inventory.addItem(Material.COIN, LOAN_AMOUNT);
        loanState = LoanState.TAKEN;
        loanTakenDay = currentDay;
        // Reduce net loss tracking (they got money)
        cumulativeNetLoss -= LOAN_AMOUNT;
        return true;
    }

    /**
     * Player repays the loan: deduct 30 coins.
     *
     * @return true if repaid successfully
     */
    public boolean repayLoan(Inventory inventory, NotorietySystem.AchievementCallback achievementCallback) {
        if (loanState != LoanState.TAKEN) return false;
        if (inventory.getItemCount(Material.COIN) < LOAN_REPAY_AMOUNT) return false;
        inventory.removeItem(Material.COIN, LOAN_REPAY_AMOUNT);
        loanState = LoanState.REPAID;
        if (achievementCallback != null && !debtFreeAwarded) {
            achievementCallback.award(AchievementType.DEBT_FREE);
            debtFreeAwarded = true;
        }
        return true;
    }

    /**
     * Check if the loan has expired (past due date without repayment).
     * Returns true if the loan should now default (LOAN_SHARK goes hostile).
     */
    public boolean isLoanOverdue(int currentDay) {
        return loanState == LoanState.TAKEN
                && (currentDay - loanTakenDay) >= LOAN_REPAY_DAYS;
    }

    /**
     * Mark the loan as defaulted (LOAN_SHARK turned hostile).
     */
    public void defaultLoan() {
        loanState = LoanState.DEFAULTED;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public List<Race> getTodaysRaces() {
        return todaysRaces;
    }

    public Bet getActiveBet() {
        return activeBet;
    }

    public int getCumulativeNetLoss() {
        return cumulativeNetLoss;
    }

    public int getTotalWinnings() {
        return totalWinnings;
    }

    public int getTotalLosses() {
        return totalLosses;
    }

    public LoanState getLoanState() {
        return loanState;
    }

    public int getLoanTakenDay() {
        return loanTakenDay;
    }

    public int getRacesTodayBetOn() {
        return racesTodayBetOn;
    }

    /** Returns the next unresolved race, or null if all are done. */
    public Race getNextRace() {
        for (Race r : todaysRaces) {
            if (!r.isResolved()) return r;
        }
        return null;
    }

    /** Returns the race at the given index, or null if out of range. */
    public Race getRace(int index) {
        if (index < 0 || index >= todaysRaces.size()) return null;
        return todaysRaces.get(index);
    }

    /** Whether today's schedule has been built. */
    public boolean hasTodaysSchedule() {
        return !todaysRaces.isEmpty();
    }

    /**
     * Maximum stake for the current context.
     *
     * @param isBenefitDay true if BENEFIT_DAY market event is active
     */
    public int getMaxStake(boolean isBenefitDay) {
        return isBenefitDay ? MAX_STAKE_BENEFIT_DAY : MAX_STAKE_NORMAL;
    }
}

package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1278: Northfield Travelling Fairground — Dodgems, Waltzers &amp; the Strongman Hustle.
 *
 * <p>Once a fortnight (biweekly) the fair sets up on the park's east end:
 * <strong>Friday 18:00 – Sunday 22:00</strong>. It is managed by Big Lenny
 * ({@link NPCType#FAIRGROUND_BOSS}) and staffed by three {@link NPCType#FAIRGROUND_WORKER}
 * NPCs (Shaz, Wayne, Donna).
 *
 * <h3>Attractions</h3>
 * <ul>
 *   <li><b>Dodgems</b> — 2 COIN, screen-shake session, Notoriety risk near police.</li>
 *   <li><b>Waltzers</b> — 2 COIN, DIZZY debuff, tip Wayne 1 COIN to spin faster.</li>
 *   <li><b>Strongman High-Striker</b> — timing-bar mechanic reusing {@link BattleBarMiniGame}
 *       (MEDIUM difficulty). Win = {@link Material#FAIRGROUND_PRIZE}.</li>
 *   <li><b>Hook-a-Duck</b> — free attraction, reuses existing prop.</li>
 *   <li><b>Candy Floss Stall</b> — {@link Material#CANDY_FLOSS} / {@link Material#TOFFEE_APPLE}
 *       items (2 COIN each). Bulk bag 5× CANDY_FLOSS into
 *       {@link Material#FAIRGROUND_CANDYFLOSS_BAG} for resale hustle.</li>
 *   <li><b>Ring Toss</b> — always rigged: 15% real success vs stated 25%.
 *       Win = FAIRGROUND_PRIZE.</li>
 * </ul>
 *
 * <h3>Employment</h3>
 * Big Lenny offers cash-in-hand Strongman shifts (3 COIN/hr). Skimming over
 * {@link #SKIM_THRESHOLD} of takings triggers a Notoriety gain and ends the shift.
 *
 * <h3>Night mechanic</h3>
 * Stripping the unattended {@code DIESEL_GENERATOR_PROP} on Sunday after
 * {@link #GENERATOR_STRIP_HOUR} yields {@link Material#SCRAP_METAL}.
 *
 * <h3>Pickpocket window</h3>
 * When 6+ NPCs are near the rides, a crowd pickpocket window opens.
 *
 * <h3>Cancellation</h3>
 * A {@link Weather#THUNDERSTORM} cancels the fair entirely.
 *
 * <h3>Integrations</h3>
 * {@link CriminalRecord}, {@link NotorietySystem}, {@link RumourNetwork},
 * {@link WeatherSystem}, {@link BattleBarMiniGame}.
 */
public class FairgroundSystem {

    // ── Schedule constants ────────────────────────────────────────────────────

    /** The fair opens on Friday at this hour. Day-of-week: 5 = Friday (day % 7). */
    public static final float OPEN_HOUR_FRIDAY = 18.0f;

    /** The fair closes on Sunday at this hour. */
    public static final float CLOSE_HOUR_SUNDAY = 22.0f;

    /** Hour on Sunday when the generator can be stripped (fair is closed). */
    public static final float GENERATOR_STRIP_HOUR = 23.0f;

    /** The fair runs every 14 days (biweekly). */
    public static final int FAIR_PERIOD_DAYS = 14;

    // ── Ride costs ────────────────────────────────────────────────────────────

    /** Cost of a Dodgems session in COIN. */
    public static final int DODGEMS_COST = 2;

    /** Cost of a Waltzers ride in COIN. */
    public static final int WALTZERS_COST = 2;

    /** Cost of a Strongman attempt in COIN (ticket required). */
    public static final int STRONGMAN_COST = 1;

    /** Cost of a Ring Toss attempt in COIN. */
    public static final int RING_TOSS_COST = 1;

    /** Cost of Candy Floss or Toffee Apple in COIN. */
    public static final int CANDY_STALL_COST = 2;

    /** Tip to Wayne to spin the Waltzers faster. */
    public static final int WAYNE_TIP_COST = 1;

    // ── Ring Toss odds ────────────────────────────────────────────────────────

    /** Stated (advertised) win probability for Ring Toss. */
    public static final float RING_TOSS_STATED_WIN_CHANCE = 0.25f;

    /** Actual win probability for Ring Toss (it's rigged). */
    public static final float RING_TOSS_REAL_WIN_CHANCE = 0.15f;

    // ── Employment constants ──────────────────────────────────────────────────

    /** Coin earned per in-game hour running the Strongman shift. */
    public static final int SHIFT_COIN_PER_HOUR = 3;

    /**
     * Maximum fraction of takings the player can skim before being caught.
     * Exceeding this triggers Notoriety gain and ends the shift.
     */
    public static final float SKIM_THRESHOLD = 0.20f;

    // ── Notoriety gains ───────────────────────────────────────────────────────

    /** Notoriety gained when Dodgems are played near a POLICE NPC. */
    public static final int DODGEMS_NOTORIETY_NEAR_POLICE = 5;

    /** Notoriety gained for FAIRGROUND_TROUBLEMAKER crime. */
    public static final int TROUBLEMAKER_NOTORIETY = 10;

    /** Notoriety gained for RIGGED_GAME crime. */
    public static final int RIGGED_GAME_NOTORIETY = 8;

    // ── Crowd pickpocket threshold ────────────────────────────────────────────

    /** Minimum NPCs near rides for the pickpocket crowd window to open. */
    public static final int PICKPOCKET_CROWD_THRESHOLD = 6;

    // ── Waltzer dizzy rides ───────────────────────────────────────────────────

    /** Number of Waltzer rides in a single visit that earns DIZZY_RASCAL achievement. */
    public static final int DIZZY_RASCAL_RIDES = 3;

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Result of attempting to ride the Dodgems. */
    public enum DodgemsResult {
        SUCCESS,
        NOT_ENOUGH_COIN,
        FAIR_CLOSED,
        FAIR_CANCELLED
    }

    /** Result of riding the Waltzers. */
    public enum WaltzersResult {
        SUCCESS,
        SUCCESS_FAST,       // Tipped Wayne
        NOT_ENOUGH_COIN,
        FAIR_CLOSED,
        FAIR_CANCELLED
    }

    /** Result of attempting the Strongman. */
    public enum StrongmanResult {
        HIT_THE_BELL,       // BattleBarMiniGame hit
        MISSED,             // BattleBarMiniGame miss or timeout
        NOT_ENOUGH_COIN,
        FAIR_CLOSED,
        FAIR_CANCELLED
    }

    /** Result of a Ring Toss attempt. */
    public enum RingTossResult {
        WIN,
        LOSS,
        NOT_ENOUGH_COIN,
        FAIR_CLOSED,
        FAIR_CANCELLED
    }

    /** Result of buying from the Candy Floss stall. */
    public enum CandyStallResult {
        BOUGHT_CANDY_FLOSS,
        BOUGHT_TOFFEE_APPLE,
        NOT_ENOUGH_COIN,
        FAIR_CLOSED,
        FAIR_CANCELLED
    }

    /** Result of signing on for a Strongman shift. */
    public enum ShiftResult {
        SIGNED_ON,
        ALREADY_ON_SHIFT,
        FAIR_CLOSED,
        FAIR_CANCELLED,
        BANNED
    }

    /** Result of stripping the generator. */
    public enum GeneratorStripResult {
        SUCCESS,
        FAIR_STILL_OPEN,
        ALREADY_STRIPPED,
        WRONG_TIME
    }

    // ── State ────────────────────────────────────────────────────────────────

    private final Random rng;

    /** Whether the player is currently on a Strongman shift for Big Lenny. */
    private boolean onShift = false;

    /** Accumulated shift time in in-game hours. */
    private float shiftHoursAccumulated = 0f;

    /** Whether the player has been banned from working for Big Lenny (skim caught). */
    private boolean bannedFromShifts = false;

    /** Total coins collected on the current shift (for skim detection). */
    private int shiftTakings = 0;

    /** Coins pocketed by the player (skim) on current shift. */
    private int shiftSkim = 0;

    /** Whether the generator has been stripped this fair visit. */
    private boolean generatorStripped = false;

    /** Waltzer rides taken in this visit (for DIZZY_RASCAL achievement). */
    private int waltzerRidesThisVisit = 0;

    /** Whether Dodgems have been ridden this visit. */
    private boolean dodgemsRiddenThisVisit = false;

    /** Whether Strongman has been attempted this visit. */
    private boolean strongmanAttemptedThisVisit = false;

    /** Whether Ring Toss has been played this visit. */
    private boolean ringTossPlayedThisVisit = false;

    /** Whether Candy Floss stall was visited this visit. */
    private boolean candyStallVisitedThisVisit = false;

    /** Whether Hook-a-Duck was visited this visit. */
    private boolean hookaDuckVisitedThisVisit = false;

    /** Day number of the last fair opening, to detect a new fair period. */
    private int lastFairOpenDay = -1;

    // ── Optional systems injected via setters ─────────────────────────────────

    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private WeatherSystem weatherSystem;

    // ── Constructor ───────────────────────────────────────────────────────────

    public FairgroundSystem(Random rng) {
        this.rng = rng;
    }

    // ── Dependency setters ────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setWeatherSystem(WeatherSystem weatherSystem) {
        this.weatherSystem = weatherSystem;
    }

    // ── Schedule helpers ──────────────────────────────────────────────────────

    /**
     * Returns whether the fair is active (open) at the given time.
     *
     * <p>The fair runs on fortnightly Friday–Sunday windows:
     * <ul>
     *   <li>Friday (dayOfWeek == 5): open from {@link #OPEN_HOUR_FRIDAY}</li>
     *   <li>Saturday (dayOfWeek == 6): open all day (0–24)</li>
     *   <li>Sunday  (dayOfWeek == 0): open until {@link #CLOSE_HOUR_SUNDAY}</li>
     * </ul>
     * Day 0 of the game is treated as a Monday; dayOfWeek = dayCount % 7 where
     * 0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun.
     *
     * <p>Biweekly: the fair is only active in even-numbered 14-day periods
     * ({@code (dayCount / FAIR_PERIOD_DAYS) % 2 == 0}).
     *
     * <p>If a THUNDERSTORM is active the fair is cancelled.
     *
     * @param timeSystem the current time system
     * @return {@code true} if the fair is currently open
     */
    public boolean isFairOpen(TimeSystem timeSystem) {
        if (isThunderstorm()) return false;

        int dayCount = timeSystem.getDayCount();
        // Biweekly check: active only in alternating 14-day windows
        if ((dayCount / FAIR_PERIOD_DAYS) % 2 != 0) return false;

        float hour = timeSystem.getTime();
        // dayOfWeek: 0=Mon…4=Fri, 5=Sat, 6=Sun
        int dayOfWeek = dayCount % 7;

        switch (dayOfWeek) {
            case 4: // Friday
                return hour >= OPEN_HOUR_FRIDAY;
            case 5: // Saturday
                return true;
            case 6: // Sunday
                return hour < CLOSE_HOUR_SUNDAY;
            default:
                return false;
        }
    }

    /**
     * Returns whether the fair is currently cancelled due to a thunderstorm.
     */
    public boolean isCancelledByThunderstorm() {
        return isThunderstorm();
    }

    private boolean isThunderstorm() {
        if (weatherSystem == null) return false;
        Weather w = weatherSystem.getCurrentWeather();
        return w != null && w.causesEvacuation();
    }

    /**
     * Returns whether it is Sunday after {@link #GENERATOR_STRIP_HOUR} (23:00).
     * At this point the fair is closed and the generator is unattended.
     */
    public boolean isGeneratorStrippable(TimeSystem timeSystem) {
        if (generatorStripped) return false;

        int dayCount = timeSystem.getDayCount();
        // Must be in a fair period (so the generator was actually set up)
        if ((dayCount / FAIR_PERIOD_DAYS) % 2 != 0) return false;

        int dayOfWeek = dayCount % 7;
        float hour = timeSystem.getTime();
        // Sunday (6) after 23:00
        return dayOfWeek == 6 && hour >= GENERATOR_STRIP_HOUR;
    }

    // ── Ride interactions ─────────────────────────────────────────────────────

    /**
     * Player attempts to ride the Dodgems (2 COIN).
     *
     * @param timeSystem the current time
     * @param inventory  player inventory
     * @param nearPolice whether a POLICE NPC is within detection range
     * @param achievementCallback for unlocking DODGEMS_ACE
     * @return result of the attempt
     */
    public DodgemsResult rideDodgems(TimeSystem timeSystem,
                                     Inventory inventory,
                                     boolean nearPolice,
                                     NotorietySystem.AchievementCallback achievementCallback) {
        if (isThunderstorm()) return DodgemsResult.FAIR_CANCELLED;
        if (!isFairOpen(timeSystem)) return DodgemsResult.FAIR_CLOSED;
        if (!inventory.hasItem(Material.COIN, DODGEMS_COST)) return DodgemsResult.NOT_ENOUGH_COIN;

        inventory.removeItem(Material.COIN, DODGEMS_COST);

        if (nearPolice && notorietySystem != null) {
            notorietySystem.addNotoriety(DODGEMS_NOTORIETY_NEAR_POLICE, achievementCallback);
        }

        if (!nearPolice) {
            dodgemsRiddenThisVisit = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.DODGEMS_ACE);
            }
        }

        checkAllTheFunOfTheFair(achievementCallback);
        return DodgemsResult.SUCCESS;
    }

    /**
     * Player attempts to ride the Waltzers (2 COIN, optional 1-COIN tip to Wayne).
     *
     * @param timeSystem  the current time
     * @param inventory   player inventory
     * @param tipWayne    whether to tip Wayne 1 COIN extra for a faster spin
     * @param achievementCallback for unlocking DIZZY_RASCAL
     * @return result of the attempt
     */
    public WaltzersResult rideWaltzers(TimeSystem timeSystem,
                                       Inventory inventory,
                                       boolean tipWayne,
                                       NotorietySystem.AchievementCallback achievementCallback) {
        if (isThunderstorm()) return WaltzersResult.FAIR_CANCELLED;
        if (!isFairOpen(timeSystem)) return WaltzersResult.FAIR_CLOSED;

        int totalCost = WALTZERS_COST + (tipWayne ? WAYNE_TIP_COST : 0);
        if (!inventory.hasItem(Material.COIN, totalCost)) return WaltzersResult.NOT_ENOUGH_COIN;

        inventory.removeItem(Material.COIN, totalCost);
        waltzerRidesThisVisit++;

        if (waltzerRidesThisVisit >= DIZZY_RASCAL_RIDES && achievementCallback != null) {
            achievementCallback.award(AchievementType.DIZZY_RASCAL);
        }

        checkAllTheFunOfTheFair(achievementCallback);
        return tipWayne ? WaltzersResult.SUCCESS_FAST : WaltzersResult.SUCCESS;
    }

    /**
     * Player attempts the Strongman High-Striker (1 COIN per attempt).
     * Uses BattleBarMiniGame at MEDIUM difficulty.
     *
     * @param timeSystem  the current time
     * @param inventory   player inventory
     * @param achievementCallback for unlocking BELLRINGER
     * @return result of the attempt
     */
    public StrongmanResult attemptStrongman(TimeSystem timeSystem,
                                            Inventory inventory,
                                            NotorietySystem.AchievementCallback achievementCallback) {
        if (isThunderstorm()) return StrongmanResult.FAIR_CANCELLED;
        if (!isFairOpen(timeSystem)) return StrongmanResult.FAIR_CLOSED;
        if (!inventory.hasItem(Material.COIN, STRONGMAN_COST)) return StrongmanResult.NOT_ENOUGH_COIN;

        inventory.removeItem(Material.COIN, STRONGMAN_COST);
        strongmanAttemptedThisVisit = true;

        // Run BattleBarMiniGame at MEDIUM difficulty, then immediately press
        BattleBarMiniGame miniGame = BattleBarMiniGame.medium(rng);
        // Simulate the player pressing at a random point in the bar
        float simulatedProgress = rng.nextFloat() * BattleBarMiniGame.ROUND_TIMEOUT_SECONDS;
        miniGame.update(simulatedProgress);
        boolean hit = miniGame.press();

        if (hit) {
            inventory.addItem(Material.FAIRGROUND_PRIZE, 1);
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.BELLRINGER);
            }
            checkAllTheFunOfTheFair(achievementCallback);
            return StrongmanResult.HIT_THE_BELL;
        }

        checkAllTheFunOfTheFair(achievementCallback);
        return StrongmanResult.MISSED;
    }

    /**
     * Runs a Strongman BattleBarMiniGame and returns whether the player pressed
     * during the hit zone. Call {@link BattleBarMiniGame#update(float)} then
     * {@link BattleBarMiniGame#press()} externally for UI-driven gameplay;
     * this overload is for programmatic / test use.
     *
     * @param miniGame an already-constructed BattleBarMiniGame to resolve
     * @param timeSystem current time
     * @param inventory  player inventory
     * @param achievementCallback for BELLRINGER
     * @return result of the attempt
     */
    public StrongmanResult resolveStrongman(BattleBarMiniGame miniGame,
                                            TimeSystem timeSystem,
                                            Inventory inventory,
                                            NotorietySystem.AchievementCallback achievementCallback) {
        if (isThunderstorm()) return StrongmanResult.FAIR_CANCELLED;
        if (!isFairOpen(timeSystem)) return StrongmanResult.FAIR_CLOSED;
        if (!inventory.hasItem(Material.COIN, STRONGMAN_COST)) return StrongmanResult.NOT_ENOUGH_COIN;

        inventory.removeItem(Material.COIN, STRONGMAN_COST);
        strongmanAttemptedThisVisit = true;

        boolean hit = miniGame.wasHit();
        if (hit) {
            inventory.addItem(Material.FAIRGROUND_PRIZE, 1);
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.BELLRINGER);
            }
            checkAllTheFunOfTheFair(achievementCallback);
            return StrongmanResult.HIT_THE_BELL;
        }

        checkAllTheFunOfTheFair(achievementCallback);
        return StrongmanResult.MISSED;
    }

    /**
     * Player attempts Ring Toss (1 COIN). Always rigged:
     * actual win probability is {@link #RING_TOSS_REAL_WIN_CHANCE} (15%),
     * not the stated {@link #RING_TOSS_STATED_WIN_CHANCE} (25%).
     *
     * @param timeSystem  the current time
     * @param inventory   player inventory
     * @param achievementCallback for ALL_THE_FUN_OF_THE_FAIR
     * @return result of the attempt
     */
    public RingTossResult playRingToss(TimeSystem timeSystem,
                                       Inventory inventory,
                                       NotorietySystem.AchievementCallback achievementCallback) {
        if (isThunderstorm()) return RingTossResult.FAIR_CANCELLED;
        if (!isFairOpen(timeSystem)) return RingTossResult.FAIR_CLOSED;
        if (!inventory.hasItem(Material.COIN, RING_TOSS_COST)) return RingTossResult.NOT_ENOUGH_COIN;

        inventory.removeItem(Material.COIN, RING_TOSS_COST);
        ringTossPlayedThisVisit = true;

        boolean win = rng.nextFloat() < RING_TOSS_REAL_WIN_CHANCE;
        if (win) {
            inventory.addItem(Material.FAIRGROUND_PRIZE, 1);
            checkAllTheFunOfTheFair(achievementCallback);
            return RingTossResult.WIN;
        }

        checkAllTheFunOfTheFair(achievementCallback);
        return RingTossResult.LOSS;
    }

    /**
     * Player buys from the Candy Floss stall.
     *
     * @param timeSystem      the current time
     * @param inventory       player inventory
     * @param wantCandyFloss  {@code true} for CANDY_FLOSS, {@code false} for TOFFEE_APPLE
     * @param achievementCallback for ALL_THE_FUN_OF_THE_FAIR
     * @return result of the purchase
     */
    public CandyStallResult buyCandyStall(TimeSystem timeSystem,
                                          Inventory inventory,
                                          boolean wantCandyFloss,
                                          NotorietySystem.AchievementCallback achievementCallback) {
        if (isThunderstorm()) return CandyStallResult.FAIR_CANCELLED;
        if (!isFairOpen(timeSystem)) return CandyStallResult.FAIR_CLOSED;
        if (!inventory.hasItem(Material.COIN, CANDY_STALL_COST)) return CandyStallResult.NOT_ENOUGH_COIN;

        inventory.removeItem(Material.COIN, CANDY_STALL_COST);
        candyStallVisitedThisVisit = true;

        if (wantCandyFloss) {
            inventory.addItem(Material.CANDY_FLOSS, 1);
            checkAllTheFunOfTheFair(achievementCallback);
            return CandyStallResult.BOUGHT_CANDY_FLOSS;
        } else {
            inventory.addItem(Material.TOFFEE_APPLE, 1);
            checkAllTheFunOfTheFair(achievementCallback);
            return CandyStallResult.BOUGHT_TOFFEE_APPLE;
        }
    }

    // ── Employment ────────────────────────────────────────────────────────────

    /**
     * Player signs on for a Strongman shift with Big Lenny.
     *
     * @param timeSystem the current time
     * @return result of signing on
     */
    public ShiftResult signOnForShift(TimeSystem timeSystem) {
        if (isThunderstorm()) return ShiftResult.FAIR_CANCELLED;
        if (!isFairOpen(timeSystem)) return ShiftResult.FAIR_CLOSED;
        if (bannedFromShifts) return ShiftResult.BANNED;
        if (onShift) return ShiftResult.ALREADY_ON_SHIFT;

        onShift = true;
        shiftHoursAccumulated = 0f;
        shiftTakings = 0;
        shiftSkim = 0;
        return ShiftResult.SIGNED_ON;
    }

    /**
     * Sign off from the current shift and collect wages.
     * If the player skimmed more than {@link #SKIM_THRESHOLD} of takings they are
     * caught: +5 Notoriety, shift wages forfeited, banned from future shifts.
     *
     * @param inventory           player inventory to receive wages
     * @param achievementCallback for FAIRGROUND_WORKER_BADGE
     * @return wages paid (0 if caught skimming)
     */
    public int signOffShift(Inventory inventory,
                            NotorietySystem.AchievementCallback achievementCallback) {
        if (!onShift) return 0;

        onShift = false;
        float skimRatio = (shiftTakings > 0) ? (float) shiftSkim / shiftTakings : 0f;
        int wagesEarned = (int) (shiftHoursAccumulated * SHIFT_COIN_PER_HOUR);

        if (skimRatio > SKIM_THRESHOLD) {
            // Caught skimming
            bannedFromShifts = true;
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(5, achievementCallback);
            }
            return 0;
        }

        if (wagesEarned > 0) {
            inventory.addItem(Material.COIN, wagesEarned);
        }
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.FAIRGROUND_WORKER_BADGE);
        }
        return wagesEarned;
    }

    /**
     * Called each frame while the player is on shift to accumulate hours and record
     * takings. {@code customersServed} represents the number of punters who paid for
     * the Strongman during this frame.
     *
     * @param delta            frame delta in in-game hours
     * @param customersServed  coin collected from punters during this frame
     * @param coinSkimmed      coin the player pocketed (skim) during this frame
     */
    public void updateShift(float delta, int customersServed, int coinSkimmed) {
        if (!onShift) return;
        shiftHoursAccumulated += delta;
        shiftTakings += customersServed;
        shiftSkim += coinSkimmed;
    }

    // ── Generator strip ───────────────────────────────────────────────────────

    /**
     * Player attempts to strip the diesel generator for scrap.
     *
     * @param timeSystem the current time
     * @param inventory  player inventory to receive SCRAP_METAL
     * @param allNpcs    all NPCs (witnesses check)
     * @param rumourNetwork rumour network to seed GENERATOR_STRIPPED
     * @return result of the attempt
     */
    public GeneratorStripResult stripGenerator(TimeSystem timeSystem,
                                               Inventory inventory,
                                               List<NPC> allNpcs,
                                               RumourNetwork rumourNetwork) {
        if (!isGeneratorStrippable(timeSystem)) {
            if (generatorStripped) return GeneratorStripResult.ALREADY_STRIPPED;
            if (isFairOpen(timeSystem)) return GeneratorStripResult.FAIR_STILL_OPEN;
            return GeneratorStripResult.WRONG_TIME;
        }

        generatorStripped = true;
        inventory.addItem(Material.SCRAP_METAL, 3);

        // Seed rumour
        seedRumour(rumourNetwork, allNpcs, RumourType.GENERATOR_STRIPPED,
                "Someone stripped the generator at the fair Sunday night — Big Lenny's fuming.");

        return GeneratorStripResult.SUCCESS;
    }

    // ── Pickpocket crowd window ───────────────────────────────────────────────

    /**
     * Returns whether the pickpocket crowd window is open.
     * Requires the fair to be open and at least {@link #PICKPOCKET_CROWD_THRESHOLD}
     * fairground-adjacent NPCs to be present.
     *
     * @param timeSystem the current time
     * @param allNpcs    all active NPCs in the scene
     * @return {@code true} if the crowd is large enough for pickpocketing
     */
    public boolean isPickpocketWindowOpen(TimeSystem timeSystem, List<NPC> allNpcs) {
        if (!isFairOpen(timeSystem)) return false;
        if (allNpcs == null) return false;
        int count = 0;
        for (NPC npc : allNpcs) {
            if (npc != null && npc.isAlive()) {
                count++;
            }
        }
        return count >= PICKPOCKET_CROWD_THRESHOLD;
    }

    // ── Crime recording ───────────────────────────────────────────────────────

    /**
     * Record that the player operated the rigged Ring Toss stall and was caught.
     *
     * @param achievementCallback for notoriety tier achievements
     */
    public void recordRiggedGameCaught(NotorietySystem.AchievementCallback achievementCallback,
                                       RumourNetwork rumourNetwork, List<NPC> allNpcs) {
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.RIGGED_GAME);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(RIGGED_GAME_NOTORIETY, achievementCallback);
        }
        seedRumour(rumourNetwork, allNpcs, RumourType.RIGGED_GAME_EXPOSED,
                "Someone got nicked at the fair for rigging the ring toss — always knew it was bent.");
    }

    /**
     * Record that the player caused trouble at the fairground.
     *
     * @param achievementCallback for notoriety tier achievements
     */
    public void recordFairgroundTrouble(NotorietySystem.AchievementCallback achievementCallback) {
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.FAIRGROUND_TROUBLEMAKER);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(TROUBLEMAKER_NOTORIETY, achievementCallback);
        }
    }

    // ── Visit state management ────────────────────────────────────────────────

    /**
     * Mark Hook-a-Duck as visited this fair session.
     * (Hook-a-Duck reuses existing props; this just tracks visit state for achievements.)
     *
     * @param achievementCallback for ALL_THE_FUN_OF_THE_FAIR
     */
    public void visitHookaDuck(NotorietySystem.AchievementCallback achievementCallback) {
        hookaDuckVisitedThisVisit = true;
        checkAllTheFunOfTheFair(achievementCallback);
    }

    /** Reset per-visit state (called when the fair closes or a new visit begins). */
    public void resetVisitState() {
        waltzerRidesThisVisit = 0;
        dodgemsRiddenThisVisit = false;
        strongmanAttemptedThisVisit = false;
        ringTossPlayedThisVisit = false;
        candyStallVisitedThisVisit = false;
        hookaDuckVisitedThisVisit = false;
    }

    /** Reset generator strip state for a new fair period. */
    public void resetForNewFairPeriod() {
        generatorStripped = false;
        resetVisitState();
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Per-frame update. Handles:
     * <ul>
     *   <li>Seeding FAIR_IN_TOWN rumour when the fair opens.</li>
     *   <li>Resetting visit/period state when a new fair period begins.</li>
     *   <li>Shift accumulation (delegates to callers via {@link #updateShift}).</li>
     * </ul>
     *
     * @param delta               frame delta (in-game hours)
     * @param timeSystem          current time
     * @param inventory           player inventory
     * @param rumourNetwork       rumour network
     * @param allNpcs             all active NPCs
     * @param achievementCallback for achievements
     * @param notorietySystem     notoriety system (may be null)
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       Inventory inventory,
                       RumourNetwork rumourNetwork,
                       List<NPC> allNpcs,
                       NotorietySystem.AchievementCallback achievementCallback,
                       NotorietySystem notorietySystem) {
        int dayCount = timeSystem.getDayCount();

        // Detect new fair period — reset generator strip state
        int fairPeriod = dayCount / FAIR_PERIOD_DAYS;
        if (lastFairOpenDay < 0 || (dayCount / FAIR_PERIOD_DAYS) != (lastFairOpenDay / FAIR_PERIOD_DAYS)) {
            if (isFairOpen(timeSystem) && lastFairOpenDay != dayCount) {
                lastFairOpenDay = dayCount;
                resetForNewFairPeriod();
                // Seed the FAIR_IN_TOWN rumour
                seedRumour(rumourNetwork, allNpcs, RumourType.FAIR_IN_TOWN,
                        "The fair's in town this weekend — park's east end. Dodgems, waltzers, the lot.");
            }
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** @return whether the player is currently on a Strongman shift */
    public boolean isOnShift() { return onShift; }

    /** @return whether the player has been banned from Big Lenny's shifts */
    public boolean isBannedFromShifts() { return bannedFromShifts; }

    /** @return whether the generator has been stripped this fair period */
    public boolean isGeneratorStripped() { return generatorStripped; }

    /** @return number of Waltzer rides taken this visit */
    public int getWaltzerRidesThisVisit() { return waltzerRidesThisVisit; }

    /** @return whether Dodgems have been ridden this visit */
    public boolean hasDodgemsRiddenThisVisit() { return dodgemsRiddenThisVisit; }

    /** @return whether Ring Toss has been played this visit */
    public boolean hasRingTossPlayedThisVisit() { return ringTossPlayedThisVisit; }

    /** @return whether the Candy Floss stall was visited this visit */
    public boolean hasCandyStallVisitedThisVisit() { return candyStallVisitedThisVisit; }

    /** @return whether Hook-a-Duck was visited this visit */
    public boolean hasHookaDuckVisitedThisVisit() { return hookaDuckVisitedThisVisit; }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Check whether all attractions have been visited and award ALL_THE_FUN_OF_THE_FAIR.
     */
    private void checkAllTheFunOfTheFair(NotorietySystem.AchievementCallback achievementCallback) {
        if (achievementCallback == null) return;
        if (dodgemsRiddenThisVisit
                && waltzerRidesThisVisit >= 1
                && strongmanAttemptedThisVisit
                && ringTossPlayedThisVisit
                && candyStallVisitedThisVisit
                && hookaDuckVisitedThisVisit) {
            achievementCallback.award(AchievementType.ALL_THE_FUN_OF_THE_FAIR);
        }
    }

    /**
     * Seed a rumour to the first alive NPC in {@code allNpcs}.
     */
    private void seedRumour(RumourNetwork rumourNetwork,
                            List<NPC> allNpcs,
                            RumourType type,
                            String text) {
        if (rumourNetwork == null || allNpcs == null) return;
        Rumour rumour = new Rumour(type, text);
        for (NPC npc : allNpcs) {
            if (npc != null && npc.isAlive()) {
                rumourNetwork.addRumour(npc, rumour);
                break;
            }
        }
    }
}

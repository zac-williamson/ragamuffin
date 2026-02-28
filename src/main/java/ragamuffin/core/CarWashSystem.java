package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
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
 * Issue #948: Hand Car Wash — Cash in Hand, Notoriety Laundering &amp; Cash Box Robbery.
 *
 * <p>Manages the Sparkle Hand Car Wash landmark near the industrial estate.
 *
 * <h3>Shift Work</h3>
 * <ul>
 *   <li>Press E near {@code HOSE_PROP} between 08:00–18:00 to start a shift.</li>
 *   <li>Shift lasts 3 in-game minutes; awards 3 COIN/min.</li>
 *   <li>Completing the full shift reduces Notoriety by 5, adds 1 tip COIN, and records
 *       a {@code LEGITIMATE_WORK} entry in {@link CriminalRecord}.</li>
 *   <li>Shift is blocked when the player has ≥1 wanted star.</li>
 *   <li>Maximum 4 completed shifts per in-game day (Notoriety reduction capped at −20/day).</li>
 *   <li>Equipping a {@code SQUEEGEE} gives +1 bonus COIN/min during the shift.</li>
 * </ul>
 *
 * <h3>Cash Box Robbery</h3>
 * <ul>
 *   <li>Press E on {@code CASH_BOX_PROP} to rob it (Tier 2+ or CROWBAR required).</li>
 *   <li>Yields 3–9 COIN. +15 Notoriety. Workers flee. Car wash closed 1 day.</li>
 *   <li>Seeds a {@code GANG_ACTIVITY} rumour about the robbery.</li>
 * </ul>
 *
 * <h3>Faction Integration</h3>
 * <ul>
 *   <li>Marchetti tip-off: press E on a Marchetti NPC (Respect ≥ 30) to earn +5 Respect.</li>
 *   <li>Operational car wash: +3 Neighbourhood Vibes/min. Robbed: −2/min.</li>
 *   <li>Completing a shift awards 1 TRADING XP and satisfies the BROKE need.</li>
 * </ul>
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@code HONEST_DAYS_WORK} — complete 4 shifts total.</li>
 *   <li>{@code SOAPY_BANDIT} — rob the cash box on the same day as completing a shift.</li>
 * </ul>
 */
public class CarWashSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Opening hour (08:00). */
    public static final float OPEN_HOUR = 8.0f;

    /** Closing hour (18:00). */
    public static final float CLOSE_HOUR = 18.0f;

    /** Duration of one shift in in-game minutes. */
    public static final int SHIFT_DURATION_MINUTES = 3;

    /** COIN earned per in-game minute during a shift (base). */
    public static final int COIN_PER_MINUTE = 3;

    /** Bonus COIN/min when the player holds a SQUEEGEE. */
    public static final int SQUEEGEE_BONUS_PER_MINUTE = 1;

    /** Bonus COIN tip awarded on completing a full shift. */
    public static final int COMPLETION_TIP = 1;

    /** Notoriety reduction per completed shift. */
    public static final int NOTORIETY_REDUCTION_PER_SHIFT = 5;

    /** Maximum total Notoriety reduction per in-game day (4 shifts × 5). */
    public static final int MAX_NOTORIETY_REDUCTION_PER_DAY = 20;

    /** Maximum shifts per in-game day. */
    public static final int MAX_SHIFTS_PER_DAY = 4;

    /** Notoriety gain when robbing the cash box. */
    public static final int ROBBERY_NOTORIETY_GAIN = 15;

    /** Minimum coins in the cash box. */
    public static final int CASH_BOX_MIN_COINS = 3;

    /** Maximum coins in the cash box. */
    public static final int CASH_BOX_MAX_COINS = 9;

    /** Number of in-game days the car wash is closed after a robbery. */
    public static final int CLOSURE_DAYS = 1;

    /** Notoriety tier required to rob without a CROWBAR (Tier 2 = 250+). */
    public static final int ROBBERY_NO_TOOL_TIER = 2;

    /** Marchetti respect required to tip off about the owner's income. */
    public static final int MARCHETTI_TIP_OFF_RESPECT = 30;

    /** Marchetti respect gained for tipping off. */
    public static final int MARCHETTI_TIP_OFF_RESPECT_GAIN = 5;

    /** Neighbourhood Vibes contribution per in-game minute when operational. */
    public static final int VIBES_PER_MINUTE_OPERATIONAL = 3;

    /** Neighbourhood Vibes contribution per in-game minute when closed (negative). */
    public static final int VIBES_PER_MINUTE_CLOSED = -2;

    /** Notoriety above which the boss gives the "I know your face" flavour line. */
    public static final int BOSS_RECOGNITION_NOTORIETY = 40;

    /** Trading XP awarded per completed shift. */
    public static final int TRADING_XP_PER_SHIFT = 1;

    /** Number of LEGITIMATE_WORK records that reduce arrest fines. */
    public static final int LEGITIMATE_WORK_FINE_REDUCTION_THRESHOLD = 3;

    /** Fine reduction multiplier when LEGITIMATE_WORK threshold is met. */
    public static final float FINE_REDUCTION_MULTIPLIER = 0.80f;

    // ── Shift result enum ──────────────────────────────────────────────────────

    /** Result of attempting to start a shift. */
    public enum ShiftStartResult {
        /** Shift started successfully. */
        SUCCESS,
        /** Car wash is currently closed (outside hours, or boss absent). */
        CLOSED,
        /** Player has wanted stars — boss refuses. */
        PLAYER_WANTED,
        /** Maximum 4 shifts for today already completed. */
        DAILY_LIMIT_REACHED,
        /** Car wash is closed due to robbery. */
        ROBBERY_CLOSURE
    }

    /** Result of completing or abandoning a shift. */
    public enum ShiftEndResult {
        /** Shift completed in full — full reward. */
        COMPLETED,
        /** Shift abandoned (player walked away) — no Notoriety reduction, no tip. */
        ABANDONED
    }

    /** Result of attempting to rob the cash box. */
    public enum RobberyResult {
        /** Robbery succeeded. */
        SUCCESS,
        /** Cash box already robbed today. */
        ALREADY_ROBBED,
        /** Player needs a CROWBAR (Notoriety below Tier 2). */
        NEEDS_CROWBAR,
        /** Car wash is closed (already robbed). */
        CLOSED
    }

    // ── State ──────────────────────────────────────────────────────────────────

    /** Whether a shift is currently in progress. */
    private boolean shiftActive = false;

    /** In-game minutes elapsed in the current shift. */
    private int shiftMinutesElapsed = 0;

    /** Number of completed shifts today. */
    private int shiftsCompletedToday = 0;

    /** Total Notoriety reduction applied today via shifts. */
    private int notorietyReducedToday = 0;

    /** Last in-game day on which stats were reset. */
    private int lastResetDay = -1;

    /** In-game day on which the cash box was robbed (−1 if not robbed). */
    private int robbedDay = -1;

    /** Whether the cash box coins have been randomised today. */
    private int cashBoxCoins = -1;

    /** Whether the Marchetti tip-off has been triggered (once per session). */
    private boolean marchettiTipOffDone = false;

    /** Whether the first-shift tooltip has been shown. */
    private boolean firstShiftTooltipShown = false;

    /** Total number of shifts ever completed (for HONEST_DAYS_WORK). */
    private int totalShiftsCompleted = 0;

    /** Whether a shift was completed today (for SOAPY_BANDIT). */
    private boolean shiftCompletedToday = false;

    /** The CAR_WASH_BOSS NPC. */
    private NPC boss;

    /** The two WORKER NPCs. */
    private final List<NPC> workers = new ArrayList<>();

    private final Random random;
    private AchievementSystem achievementSystem;
    private NotorietySystem notorietySystem;
    private FactionSystem factionSystem;
    private NeighbourhoodSystem neighbourhoodSystem;
    private StreetSkillSystem streetSkillSystem;
    private RumourNetwork rumourNetwork;
    private CriminalRecord criminalRecord;
    private WantedSystem wantedSystem;

    // ── Construction ───────────────────────────────────────────────────────────

    public CarWashSystem() {
        this(new Random());
    }

    public CarWashSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ───────────────────────────────────────────────────

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setNeighbourhoodSystem(NeighbourhoodSystem neighbourhoodSystem) {
        this.neighbourhoodSystem = neighbourhoodSystem;
    }

    public void setStreetSkillSystem(StreetSkillSystem streetSkillSystem) {
        this.streetSkillSystem = streetSkillSystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    // ── NPC management ─────────────────────────────────────────────────────────

    /**
     * Spawn the CAR_WASH_BOSS and two WORKER NPCs at the forecourt.
     *
     * @param bossX  X position of the SHED_PROP (where the boss stands)
     * @param bossY  Y position
     * @param bossZ  Z position
     */
    public void spawnNPCs(int bossX, int bossY, int bossZ) {
        workers.clear();
        boss = new NPC(NPCType.CAR_WASH_BOSS, bossX, bossY, bossZ);
        boss.setSpeechText("Full valet, five pounds. Interior extra.", 0f);

        NPC worker1 = new NPC(NPCType.PUBLIC, bossX + 2, bossY, bossZ + 1);
        worker1.setSpeechText("Lovely job mate, come back Tuesday.", 0f);
        workers.add(worker1);

        NPC worker2 = new NPC(NPCType.PUBLIC, bossX + 4, bossY, bossZ + 1);
        worker2.setSpeechText("We do tyres an' all.", 0f);
        workers.add(worker2);
    }

    // ── Opening hours ──────────────────────────────────────────────────────────

    /**
     * Returns true if the car wash is open at the given in-game hour.
     *
     * @param hour in-game hour (0–24)
     */
    public boolean isOpen(float hour) {
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Returns true if the car wash is closed due to a robbery on the current day.
     *
     * @param currentDay current in-game day number
     */
    public boolean isClosed(int currentDay) {
        if (robbedDay < 0) return false;
        return currentDay - robbedDay < CLOSURE_DAYS;
    }

    // ── Daily reset ────────────────────────────────────────────────────────────

    /**
     * Called at the start of each in-game day to reset per-day counters.
     *
     * @param currentDay current in-game day number
     */
    public void onNewDay(int currentDay) {
        if (lastResetDay == currentDay) return;
        lastResetDay = currentDay;
        shiftsCompletedToday = 0;
        notorietyReducedToday = 0;
        shiftCompletedToday = false;
        // Randomise cash box contents for the new day
        cashBoxCoins = CASH_BOX_MIN_COINS + random.nextInt(CASH_BOX_MAX_COINS - CASH_BOX_MIN_COINS + 1);
        // Reopen car wash if closure period has expired
        if (robbedDay >= 0 && currentDay - robbedDay >= CLOSURE_DAYS) {
            robbedDay = -1;
            if (boss != null) {
                boss.setSpeechText("Full valet, five pounds. Interior extra.", 0f);
            }
        }
    }

    // ── Shift mechanics ────────────────────────────────────────────────────────

    /**
     * Check whether the player can start a new shift right now.
     *
     * @param hour       current in-game hour
     * @param currentDay current in-game day
     * @return the result explaining why a shift can or cannot be started
     */
    public ShiftStartResult canStartShift(float hour, int currentDay) {
        if (!isOpen(hour)) {
            return ShiftStartResult.CLOSED;
        }
        if (isClosed(currentDay)) {
            return ShiftStartResult.ROBBERY_CLOSURE;
        }
        if (wantedSystem != null && wantedSystem.getWantedStars() >= 1) {
            return ShiftStartResult.PLAYER_WANTED;
        }
        if (shiftsCompletedToday >= MAX_SHIFTS_PER_DAY) {
            return ShiftStartResult.DAILY_LIMIT_REACHED;
        }
        return ShiftStartResult.SUCCESS;
    }

    /**
     * Start a new shift. Returns {@code false} if the shift could not be started.
     * The caller should have checked {@link #canStartShift} first.
     *
     * @param inventory  player inventory (for tooltip tracking)
     * @param currentDay current in-game day
     */
    public boolean startShift(Inventory inventory, int currentDay) {
        if (shiftActive) return false;
        shiftActive = true;
        shiftMinutesElapsed = 0;
        onNewDay(currentDay); // ensure day counters are initialised

        if (!firstShiftTooltipShown) {
            firstShiftTooltipShown = true;
            // Tooltip "Honest work. Almost." — caller reads getFirstShiftTooltip()
        }

        // Boss flavour on high notoriety
        if (notorietySystem != null && notorietySystem.getNotoriety() > BOSS_RECOGNITION_NOTORIETY && boss != null) {
            boss.setSpeechText("I know your face from somewhere. You're not in the papers, are ya?", 0f);
        }

        return true;
    }

    /**
     * Advance the shift by one in-game minute. Awards mid-shift COIN.
     * Call this once per in-game minute while a shift is active.
     *
     * @param inventory      player inventory (receives COIN)
     * @param hasSqueegeee   true if the player has a SQUEEGEE equipped
     * @return true if the shift is now complete (3 minutes elapsed)
     */
    public boolean advanceShiftMinute(Inventory inventory, boolean hasSqueegeee) {
        if (!shiftActive) return false;

        int coinEarned = COIN_PER_MINUTE + (hasSqueegeee ? SQUEEGEE_BONUS_PER_MINUTE : 0);
        inventory.addItem(Material.COIN, coinEarned);
        shiftMinutesElapsed++;

        return shiftMinutesElapsed >= SHIFT_DURATION_MINUTES;
    }

    /**
     * End the current shift, awarding completion rewards if it was a full shift.
     *
     * @param completed  true if the player worked the full {@link #SHIFT_DURATION_MINUTES}
     * @param inventory  player inventory
     * @param currentDay current in-game day
     * @return the result of ending the shift
     */
    public ShiftEndResult endShift(boolean completed, Inventory inventory, int currentDay) {
        if (!shiftActive) return ShiftEndResult.ABANDONED;
        shiftActive = false;

        if (!completed) {
            shiftMinutesElapsed = 0;
            return ShiftEndResult.ABANDONED;
        }

        // Full completion bonuses
        inventory.addItem(Material.COIN, COMPLETION_TIP);

        // Notoriety reduction (capped per day)
        if (notorietySystem != null && notorietyReducedToday < MAX_NOTORIETY_REDUCTION_PER_DAY) {
            int reduction = Math.min(NOTORIETY_REDUCTION_PER_SHIFT,
                                     MAX_NOTORIETY_REDUCTION_PER_DAY - notorietyReducedToday);
            notorietySystem.reduceNotoriety(reduction, null);
            notorietyReducedToday += reduction;
        }

        // CriminalRecord
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.LEGITIMATE_WORK);
        }

        // Street skill XP
        if (streetSkillSystem != null) {
            streetSkillSystem.awardXP(StreetSkillSystem.Skill.TRADING, TRADING_XP_PER_SHIFT);
        }

        // Increment counters
        shiftsCompletedToday++;
        totalShiftsCompleted++;
        shiftCompletedToday = true;
        shiftMinutesElapsed = 0;

        // Achievements
        if (achievementSystem != null) {
            achievementSystem.increment(AchievementType.HONEST_DAYS_WORK);
        }

        // Boss speech if limit reached
        if (shiftsCompletedToday >= MAX_SHIFTS_PER_DAY && boss != null) {
            boss.setSpeechText("You'll do. Come back tomorrow.", 0f);
        } else if (boss != null) {
            boss.setSpeechText("You did a proper job. The boss is impressed. He didn't ask where you live.", 0f);
        }

        return ShiftEndResult.COMPLETED;
    }

    // ── Cash box robbery ───────────────────────────────────────────────────────

    /**
     * Attempt to rob the cash box.
     *
     * @param inventory      player inventory (receives coins; CROWBAR consumed if required)
     * @param currentDay     current in-game day
     * @param notorietyTier  player's current Notoriety tier (0–5)
     * @param hasCrowbar     true if the player is holding a CROWBAR
     * @param allNpcs        all NPCs in the world (workers set to FLEEING)
     * @return the result of the robbery attempt
     */
    public RobberyResult robCashBox(Inventory inventory, int currentDay,
                                     int notorietyTier, boolean hasCrowbar,
                                     List<NPC> allNpcs) {
        if (isClosed(currentDay)) {
            return RobberyResult.CLOSED;
        }
        if (robbedDay == currentDay) {
            return RobberyResult.ALREADY_ROBBED;
        }
        if (notorietyTier < ROBBERY_NO_TOOL_TIER && !hasCrowbar) {
            return RobberyResult.NEEDS_CROWBAR;
        }

        // Ensure cash box has been randomised for today
        onNewDay(currentDay);
        if (cashBoxCoins < 0) {
            cashBoxCoins = CASH_BOX_MIN_COINS + random.nextInt(CASH_BOX_MAX_COINS - CASH_BOX_MIN_COINS + 1);
        }

        // Award coins
        inventory.addItem(Material.COIN, cashBoxCoins);
        cashBoxCoins = 0;

        // Notoriety gain
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(ROBBERY_NOTORIETY_GAIN, null);
        }

        // Workers flee
        for (NPC npc : workers) {
            npc.setState(NPCState.FLEEING);
        }
        // Also check allNpcs for any workers near the car wash
        if (allNpcs != null) {
            for (NPC npc : allNpcs) {
                if (npc.getType() == NPCType.PUBLIC) {
                    npc.setState(NPCState.FLEEING);
                }
            }
        }

        // Seed GANG_ACTIVITY rumour
        if (rumourNetwork != null && allNpcs != null) {
            Rumour robberyRumour = new Rumour(
                RumourType.GANG_ACTIVITY,
                "Someone's turned over the car wash. Bold move."
            );
            for (NPC npc : allNpcs) {
                rumourNetwork.addRumour(npc, robberyRumour);
                break; // seed into first available NPC
            }
        }

        // Close for 1 day
        robbedDay = currentDay;
        if (boss != null) {
            boss.setSpeechText("Oi! What you playing at?!", 0f);
        }

        // Check SOAPY_BANDIT achievement
        if (shiftCompletedToday && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.SOAPY_BANDIT);
        }

        return RobberyResult.SUCCESS;
    }

    // ── Neighbourhood Vibes tick ───────────────────────────────────────────────

    /**
     * Called once per in-game minute to apply Neighbourhood Vibes contribution.
     *
     * @param currentDay current in-game day
     */
    public void onMinuteTick(int currentDay) {
        if (neighbourhoodSystem == null) return;
        int vibesDelta = isClosed(currentDay) ? VIBES_PER_MINUTE_CLOSED : VIBES_PER_MINUTE_OPERATIONAL;
        int current = neighbourhoodSystem.getVibes();
        neighbourhoodSystem.setVibes(current + vibesDelta);
    }

    // ── Marchetti tip-off ──────────────────────────────────────────────────────

    /**
     * Trigger the Marchetti tip-off mechanic (press E on a Marchetti NPC).
     * Awards +5 Marchetti Respect if Respect ≥ 30.
     *
     * @return true if the tip-off succeeded
     */
    public boolean triggerMarchettiTipOff() {
        if (marchettiTipOffDone) return false;
        if (factionSystem == null) return false;
        int respect = factionSystem.getRespect(Faction.MARCHETTI_CREW);
        if (respect < MARCHETTI_TIP_OFF_RESPECT) return false;

        factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, MARCHETTI_TIP_OFF_RESPECT_GAIN);
        marchettiTipOffDone = true;
        return true;
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    /** Whether a shift is currently active. */
    public boolean isShiftActive() {
        return shiftActive;
    }

    /** In-game minutes elapsed in the current shift (0–3). */
    public int getShiftMinutesElapsed() {
        return shiftMinutesElapsed;
    }

    /** Number of shifts completed today. */
    public int getShiftsCompletedToday() {
        return shiftsCompletedToday;
    }

    /** Total Notoriety reduction applied today via shifts. */
    public int getNotorietyReducedToday() {
        return notorietyReducedToday;
    }

    /** Total shifts ever completed (across all days). */
    public int getTotalShiftsCompleted() {
        return totalShiftsCompleted;
    }

    /** Current cash box coin amount (3–9, or 0 if already robbed today). */
    public int getCashBoxCoins() {
        return cashBoxCoins;
    }

    /** Returns the CAR_WASH_BOSS NPC, or null if not spawned. */
    public NPC getBoss() {
        return boss;
    }

    /** Returns the list of WORKER NPCs. */
    public List<NPC> getWorkers() {
        return workers;
    }

    /** Whether the first-shift tooltip should be shown this session. */
    public boolean isFirstShiftTooltipShown() {
        return firstShiftTooltipShown;
    }

    // ── Testing helpers ────────────────────────────────────────────────────────

    /** Force-set shiftsCompletedToday (for testing). */
    public void setShiftsCompletedTodayForTesting(int count) {
        this.shiftsCompletedToday = count;
    }

    /** Force-set robbedDay (for testing). */
    public void setRobbedDayForTesting(int day) {
        this.robbedDay = day;
    }

    /** Force-set cashBoxCoins (for testing). */
    public void setCashBoxCoinsForTesting(int coins) {
        this.cashBoxCoins = coins;
    }

    /** Force-set shiftCompletedToday (for testing). */
    public void setShiftCompletedTodayForTesting(boolean value) {
        this.shiftCompletedToday = value;
    }

    /** Force-set lastResetDay (for testing). */
    public void setLastResetDayForTesting(int day) {
        this.lastResetDay = day;
    }
}

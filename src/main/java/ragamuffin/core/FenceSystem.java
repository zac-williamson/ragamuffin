package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Manages the Fence NPC and the underground economy system.
 *
 * <p>Features:
 * <ul>
 *   <li>Police avoidance: Fence retreats to IDLE if police within 15 blocks</li>
 *   <li>Rep gate: rep &lt; 10 = ignored; 10–29 = buy/sell only; 30+ = contraband runs unlocked</li>
 *   <li>Daily rotating stock (3 items, re-rolled each in-game day)</li>
 *   <li>Contraband runs: timed DELIVER quests with failure penalising rep by 5</li>
 *   <li>Fence locked for one in-game day after a failed contraband run</li>
 * </ul>
 */
public class FenceSystem {

    /** Distance within which a police NPC causes the Fence to abort trade. */
    public static final float POLICE_FLEE_DISTANCE = 15f;

    /** Duration of police-scared idle state (seconds). */
    public static final float POLICE_SCARE_DURATION = 8f;

    /** Rep penalty for a failed contraband run. */
    public static final int FAILED_RUN_REP_PENALTY = 5;

    /** Seconds per in-game day (used for lock duration). */
    public static final float IN_GAME_DAY_SECONDS = 1200f; // 20 real-minutes = 1 day

    /** Available contraband run definitions. */
    public static final String[] CONTRABAND_RUN_NAMES = {
        "The Parcel",
        "Diamond Geezer",
        "Office Clearance",
        "Biscuit Run"
    };

    /** Time limit in seconds for each contraband run (same order as CONTRABAND_RUN_NAMES). */
    public static final float[] CONTRABAND_RUN_TIME_LIMITS = { 120f, 90f, 150f, 60f };

    /** Required material for each contraband run. */
    public static final Material[] CONTRABAND_RUN_MATERIALS = {
        Material.SCRAP_METAL,
        Material.DIAMOND,
        Material.COMPUTER,
        Material.STAPLER
    };

    /** Required count for each contraband run. */
    public static final int[] CONTRABAND_RUN_COUNTS = { 3, 1, 2, 5 };

    /** Reward in FOOD units for completing each contraband run. */
    public static final int[] CONTRABAND_RUN_REWARDS = { 8, 20, 12, 5 };

    /** All items the Fence can stock (daily rotation pool). */
    private static final Material[] STOCK_POOL = {
        Material.HIGH_VIS_JACKET,
        Material.CROWBAR,
        Material.BALACLAVA,
        Material.BOLT_CUTTERS,
        Material.DODGY_PASTY
    };

    /** FOOD cost for each stock item (same order as STOCK_POOL). */
    private static final int[] STOCK_COSTS = { 5, 8, 6, 10, 2 };

    /** How many stock items the Fence sells per day. */
    public static final int DAILY_STOCK_SIZE = 3;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final FenceValuationTable valuationTable = new FenceValuationTable();
    private final Random random;

    /** Phase 8e: notoriety system for pricing bonuses and bribery (may be null). */
    private NotorietySystem notorietySystem;

    /** The Fence NPC, or null if not yet spawned. */
    private NPC fenceNpc;

    /** Whether the trade UI is open. */
    private boolean tradeUIOpen;

    /** Current day (for detecting day rollover and refreshing stock). */
    private int currentDay = -1;

    /** Daily stock: up to DAILY_STOCK_SIZE items available today. */
    private final List<Material> todayStock = new ArrayList<>();

    /** Cost in FOOD units for each item in todayStock (parallel list). */
    private final List<Integer> todayStockCosts = new ArrayList<>();

    /** Whether the Fence is locked (after a failed contraband run). */
    private boolean locked = false;

    /** Countdown until the Fence becomes available again (seconds). */
    private float lockTimer = 0f;

    /** Whether a contraband run is currently active. */
    private boolean contrabandRunActive = false;

    /** Index into CONTRABAND_RUN_NAMES for the current run, or -1. */
    private int currentRunIndex = -1;

    /** Countdown timer for the current contraband run. */
    private float runTimer = 0f;

    /** Whether the Fence was scared off by police this interaction (trade UI closed). */
    private boolean scaredByPolice = false;

    /** Timer tracking how long the Fence has been hiding from police. */
    private float policeScareTimer = 0f;

    public FenceSystem() {
        this(new Random());
    }

    public FenceSystem(Random random) {
        this.random = random;
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    /**
     * Call once per frame. Updates run timers, lock countdown, and police avoidance.
     *
     * @param delta       seconds since last frame
     * @param player      the player (for rep checks)
     * @param allNpcs     all active NPCs (used to find police)
     * @param currentDayInt current in-game day (0-based integer, advances daily)
     */
    public void update(float delta, Player player, List<NPC> allNpcs, int currentDayInt) {
        // Day rollover — refresh daily stock
        if (currentDayInt != currentDay) {
            currentDay = currentDayInt;
            rollDailyStock();
        }

        // Lock countdown
        if (locked && lockTimer > 0f) {
            lockTimer -= delta;
            if (lockTimer <= 0f) {
                locked = false;
                lockTimer = 0f;
            }
        }

        // Police scare
        if (policeScareTimer > 0f) {
            policeScareTimer -= delta;
            if (policeScareTimer <= 0f) {
                scaredByPolice = false;
                policeScareTimer = 0f;
            }
        }

        // Check if fence NPC is near police — if so, retreat to IDLE
        if (fenceNpc != null && fenceNpc.isAlive()) {
            boolean policeNear = isPoliceNear(fenceNpc.getPosition(), allNpcs);
            if (policeNear && tradeUIOpen) {
                // Abort trade
                tradeUIOpen = false;
                scaredByPolice = true;
                policeScareTimer = POLICE_SCARE_DURATION;
                fenceNpc.setState(NPCState.IDLE);
                fenceNpc.setSpeechText("Not now. Bill's watching.", 4.0f);
            } else if (policeNear) {
                fenceNpc.setState(NPCState.IDLE);
            }
        }

        // Contraband run countdown
        if (contrabandRunActive && currentRunIndex >= 0) {
            runTimer -= delta;
            if (runTimer <= 0f) {
                // Run failed — time expired
                failContrabandRun(player);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Player interaction
    // -------------------------------------------------------------------------

    /**
     * Called when the player presses E near the Fence NPC.
     *
     * @param player    the player
     * @param inventory the player's inventory
     * @param allNpcs   all active NPCs (used to check for police)
     * @return dialogue text to display (may be null if Fence ignores player)
     */
    public String onPlayerInteract(Player player, Inventory inventory, List<NPC> allNpcs) {
        if (fenceNpc == null || !fenceNpc.isAlive()) {
            return null;
        }

        // Police nearby — refuse interaction
        if (isPoliceNear(fenceNpc.getPosition(), allNpcs)) {
            scaredByPolice = true;
            policeScareTimer = POLICE_SCARE_DURATION;
            fenceNpc.setState(NPCState.IDLE);
            String line = "Not now. Bill's watching.";
            fenceNpc.setSpeechText(line, 4.0f);
            return line;
        }

        // Locked after failed run
        if (locked) {
            String line = "Come back tomorrow.";
            fenceNpc.setSpeechText(line, 3.0f);
            return line;
        }

        int rep = player.getStreetReputation().getPoints();

        // Rep < 10: Fence ignores player
        if (rep < StreetReputation.KNOWN_THRESHOLD) {
            return null; // silently ignored
        }

        // Rep 10+: open trade UI
        tradeUIOpen = true;
        return null; // Trade UI will render; no speech bubble needed
    }

    /**
     * Close the Fence trade UI (e.g. player pressed ESC or moved away).
     */
    public void closeTradeUI() {
        tradeUIOpen = false;
    }

    /**
     * Attempt to sell one unit of the given material to the Fence.
     *
     * @param material  the item to sell
     * @param inventory player inventory
     * @return FOOD units received, or 0 if the sale failed
     */
    public int sellToFence(Material material, Inventory inventory) {
        if (!valuationTable.accepts(material)) return 0;
        if (inventory.getItemCount(material) < 1) return 0;
        int payment = valuationTable.getValueFor(material);
        // Phase 8e: apply notoriety tier bonus (Tier 1+ = 10% better prices)
        if (notorietySystem != null) {
            payment = notorietySystem.applyFencePriceBonus(payment);
        }
        inventory.removeItem(material, 1);
        inventory.addItem(Material.FOOD, payment);
        return payment;
    }

    /**
     * Attempt to buy a stock item at the given index from todayStock.
     *
     * @param stockIndex  index into todayStock (0-based)
     * @param inventory   player inventory
     * @return true if purchased, false if insufficient FOOD or invalid index
     */
    public boolean buyFromStock(int stockIndex, Inventory inventory) {
        if (stockIndex < 0 || stockIndex >= todayStock.size()) return false;
        Material item = todayStock.get(stockIndex);
        int cost = todayStockCosts.get(stockIndex);
        if (inventory.getItemCount(Material.FOOD) < cost) return false;
        inventory.removeItem(Material.FOOD, cost);
        inventory.addItem(item, 1);
        return true;
    }

    /**
     * Offer a contraband run to the player (only if rep >= 30).
     * Picks a random run from CONTRABAND_RUN_NAMES.
     *
     * @param player the player
     * @return the run name and instructions as a string, or a refusal if rep too low
     */
    public String offerContrabandRun(Player player) {
        if (player.getStreetReputation().getPoints() < StreetReputation.NOTORIOUS_THRESHOLD) {
            return "You're not ready for that yet, mate. Build your rep first.";
        }
        if (contrabandRunActive) {
            return "You've already got a job on. Finish that first.";
        }
        if (locked) {
            return "We're done 'til tomorrow. You know why.";
        }

        // Pick a random run
        currentRunIndex = random.nextInt(CONTRABAND_RUN_NAMES.length);
        runTimer = CONTRABAND_RUN_TIME_LIMITS[currentRunIndex];
        contrabandRunActive = true;

        String runName = CONTRABAND_RUN_NAMES[currentRunIndex];
        Material mat = CONTRABAND_RUN_MATERIALS[currentRunIndex];
        int count = CONTRABAND_RUN_COUNTS[currentRunIndex];
        int reward = CONTRABAND_RUN_REWARDS[currentRunIndex];
        int timeLimit = (int) CONTRABAND_RUN_TIME_LIMITS[currentRunIndex];

        return String.format("Job: \"%s\". Bring me %d x %s in %d seconds. Pay: %d food. Don't be late.",
                runName, count, mat.getDisplayName(), timeLimit, reward);
    }

    /**
     * Attempt to complete the active contraband run.
     * Must have the required items. Removes them and pays FOOD.
     *
     * @param player    the player
     * @param inventory the player's inventory
     * @return completion message, or null if run cannot be completed
     */
    public String completeContrabandRun(Player player, Inventory inventory) {
        if (!contrabandRunActive || currentRunIndex < 0) {
            return null;
        }
        Material mat = CONTRABAND_RUN_MATERIALS[currentRunIndex];
        int count = CONTRABAND_RUN_COUNTS[currentRunIndex];

        if (inventory.getItemCount(mat) < count) {
            return "You ain't got the goods yet.";
        }

        int reward = CONTRABAND_RUN_REWARDS[currentRunIndex];
        String runName = CONTRABAND_RUN_NAMES[currentRunIndex];

        inventory.removeItem(mat, count);
        inventory.addItem(Material.FOOD, reward);
        player.getStreetReputation().addPoints(3); // bonus rep for completing a run

        contrabandRunActive = false;
        currentRunIndex = -1;
        runTimer = 0f;

        return String.format("Nice work on \"%s\". Here's your %d food. Don't spend it all at once.", runName, reward);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void rollDailyStock() {
        todayStock.clear();
        todayStockCosts.clear();

        // Shuffle the pool and take DAILY_STOCK_SIZE items
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < STOCK_POOL.length; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, random);

        int count = Math.min(DAILY_STOCK_SIZE, indices.size());
        for (int i = 0; i < count; i++) {
            int idx = indices.get(i);
            todayStock.add(STOCK_POOL[idx]);
            todayStockCosts.add(STOCK_COSTS[idx]);
        }
    }

    private boolean isPoliceNear(Vector3 fencePos, List<NPC> allNpcs) {
        for (NPC npc : allNpcs) {
            if (npc.getType() == NPCType.POLICE && npc.isAlive()) {
                float dist = npc.getPosition().dst(fencePos);
                if (dist <= POLICE_FLEE_DISTANCE) {
                    return true;
                }
            }
        }
        return false;
    }

    private void failContrabandRun(Player player) {
        contrabandRunActive = false;
        String runName = CONTRABAND_RUN_NAMES[currentRunIndex];
        currentRunIndex = -1;
        runTimer = 0f;

        player.getStreetReputation().removePoints(FAILED_RUN_REP_PENALTY);

        locked = true;
        lockTimer = IN_GAME_DAY_SECONDS;

        if (fenceNpc != null) {
            fenceNpc.setSpeechText("You bottled it. Come back tomorrow — if you dare.", 5.0f);
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Set the Fence NPC instance (call after spawning). */
    public void setFenceNpc(NPC npc) {
        this.fenceNpc = npc;
    }

    /** Get the Fence NPC, or null. */
    public NPC getFenceNpc() {
        return fenceNpc;
    }

    /** Whether the trade UI is currently open. */
    public boolean isTradeUIOpen() {
        return tradeUIOpen;
    }

    /** Whether the Fence was scared by police and is hiding. */
    public boolean isScarecByPolice() {
        return scaredByPolice;
    }

    /** Whether the Fence is locked after a failed run. */
    public boolean isLocked() {
        return locked;
    }

    /** How many seconds until the Fence unlocks. */
    public float getLockTimer() {
        return lockTimer;
    }

    /** Whether a contraband run is active. */
    public boolean isContrabandRunActive() {
        return contrabandRunActive;
    }

    /** Remaining time in seconds for the current contraband run. */
    public float getRunTimer() {
        return runTimer;
    }

    /** Index of the active run (into CONTRABAND_RUN_NAMES), or -1. */
    public int getCurrentRunIndex() {
        return currentRunIndex;
    }

    /** Today's stock items. */
    public List<Material> getTodayStock() {
        return Collections.unmodifiableList(todayStock);
    }

    /** Today's stock costs (parallel to todayStock). */
    public List<Integer> getTodayStockCosts() {
        return Collections.unmodifiableList(todayStockCosts);
    }

    /** The valuation table. */
    public FenceValuationTable getValuationTable() {
        return valuationTable;
    }

    /**
     * Attach the NotorietySystem for Phase 8e pricing bonuses and bribery.
     * Call once after the notoriety system is initialised.
     */
    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    /** Returns the attached NotorietySystem, or null. */
    public NotorietySystem getNotorietySystem() {
        return notorietySystem;
    }

    /**
     * Bribe the fence to reduce notoriety by {@link NotorietySystem#BRIBE_REDUCTION} points.
     * Costs {@link NotorietySystem#BRIBE_COST_COIN} COIN. Requires notoriety system attached.
     *
     * @param inventory         the player's inventory
     * @param achievementCallback callback for achievements (may be null)
     * @return true if the bribe succeeded
     */
    public boolean bribeFence(Inventory inventory, NotorietySystem.AchievementCallback achievementCallback) {
        if (notorietySystem == null) return false;
        return notorietySystem.bribeFence(inventory, achievementCallback);
    }

    /**
     * Force-set the lock state (for testing).
     */
    void setLockedForTesting(boolean locked, float lockTimer) {
        this.locked = locked;
        this.lockTimer = lockTimer;
    }

    /**
     * Force-set the current day (for testing daily stock roll).
     */
    void setCurrentDayForTesting(int day) {
        this.currentDay = day;
    }

    /**
     * Force-set contraband run state (for testing).
     */
    void setContrabandRunForTesting(boolean active, int runIndex, float timer) {
        this.contrabandRunActive = active;
        this.currentRunIndex = runIndex;
        this.runTimer = timer;
    }
}

package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Issue #914: Allotment System — Grow Your Own, Mind Your Own.
 *
 * <p>Manages allotment plot ownership, crop growth, weather effects, plot-neighbour
 * events, council repossession, and the annual Giant Vegetable Show.
 *
 * <h3>Plot Claiming</h3>
 * <ul>
 *   <li>6 individual plots (5×5 DIRT) at the ALLOTMENTS landmark.</li>
 *   <li>Free claim from {@code ALLOTMENT_WARDEN} NPC, or buy out an occupied plot
 *       for 30 coins. Only one plot per player.</li>
 *   <li>Warden open hours: 07:00–19:00. Trespassing outside adds a criminal record entry.</li>
 *   <li>{@code PLOT_DEED} item added to inventory on claim; removed on repossession.</li>
 * </ul>
 *
 * <h3>Crop Growing</h3>
 * <ul>
 *   <li>POTATO (15 min), CARROT (10 min), CABBAGE (20 min), SUNFLOWER (8 min).</li>
 *   <li>3 growth stages: 0 = freshly planted, 1 = sprout (≥50%), 2 = fully grown (100%).</li>
 *   <li>Watering: reduces remaining grow time by 30%. Bucket not consumed.</li>
 *   <li>HEATWAVE: growth rate ×1.5 slower unless watered each in-game day.</li>
 *   <li>RAIN/DRIZZLE: auto-waters — no bucket needed that day.</li>
 *   <li>FROST: kills crops at stage 0 (yields nothing).</li>
 * </ul>
 *
 * <h3>Plot Neighbour Events</h3>
 * Every 5 in-game minutes while player is on site, one of:
 * <ol>
 *   <li>COMPLIMENT — faction respect +1</li>
 *   <li>COMPLAINT  — harvest within 60s or lose 2 coins</li>
 *   <li>GIFT       — free POTATO or CARROT drop</li>
 *   <li>RIVALRY    — triggers Veg Show vote bonus</li>
 * </ol>
 *
 * <h3>Repossession</h3>
 * 3 consecutive fallow days → REPOSSESSION_NOTICE prop; 1 day grace to plant.
 * If ignored, plot is repossessed and PLOT_DEED removed.
 *
 * <h3>Annual Giant Vegetable Show</h3>
 * Day 7/14/21 at 12:00; 1-hour event. Win: 15 coins + CHAMPION_GROWER + headline.
 *
 * <h3>Integration</h3>
 * <ul>
 *   <li>{@link WeatherSystem} — growth rate modifiers</li>
 *   <li>{@link TimeSystem}   — growth ticks, show schedule</li>
 *   <li>{@link StreetSkillSystem} — GRAFTING XP per harvest, INFLUENCE XP on show win</li>
 *   <li>{@link NewspaperSystem}  — show win headline</li>
 *   <li>{@link NotorietySystem}  — trespassing outside hours</li>
 *   <li>{@link AchievementSystem} — GREEN_FINGERS, CHAMPION_GROWER, SELF_SUFFICIENT, GOOD_NEIGHBOUR</li>
 * </ul>
 */
public class AllotmentSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Number of available plots at the ALLOTMENTS landmark. */
    public static final int TOTAL_PLOTS = 6;

    /** Coins required to buy out an occupied plot. */
    public static final int BUYOUT_COST = 30;

    /** Size of each plot in blocks (one side of the 5×5 grid). */
    public static final int PLOT_SIZE = 5;

    /** Warden open hour (07:00). */
    public static final float WARDEN_OPEN_HOUR = 7.0f;

    /** Warden close hour (19:00). */
    public static final float WARDEN_CLOSE_HOUR = 19.0f;

    /** Grow-time reduction from a single watering (30%). */
    public static final float WATERING_REDUCTION = 0.30f;

    /** HEATWAVE growth-time multiplier (×1.5 slower = ×1.5 duration). */
    public static final float HEATWAVE_SLOW_MULTIPLIER = 1.5f;

    /** In-game seconds between neighbour events (5 in-game minutes). */
    public static final float NEIGHBOUR_EVENT_INTERVAL = 5.0f * 60.0f;

    /** Seconds the player has to respond to a COMPLAINT event before losing coins. */
    public static final float COMPLAINT_WINDOW_SECONDS = 60.0f;

    /** Coins deducted if a COMPLAINT goes unanswered. */
    public static final int COMPLAINT_FINE = 2;

    /** Fallow days before REPOSSESSION_NOTICE is placed. */
    public static final int FALLOW_DAYS_THRESHOLD = 3;

    /** Coins awarded for winning the Veg Show. */
    public static final int VEG_SHOW_WIN_COINS = 15;

    /** GRAFTING XP awarded per successful harvest. */
    public static final int XP_HARVEST = 1;

    /** INFLUENCE XP awarded for entering the Veg Show. */
    public static final int XP_SHOW_ENTRY = 5;

    /** Veg Show headline text. */
    public static final String VEG_SHOW_HEADLINE = "LOCAL HERO TAKES TOP PRIZE AT VEG SHOW";

    /** Compliments needed for GOOD_NEIGHBOUR achievement. */
    public static final int GOOD_NEIGHBOUR_COMPLIMENTS = 3;

    /** Crops harvested needed for SELF_SUFFICIENT achievement. */
    public static final int SELF_SUFFICIENT_TARGET = 20;

    // ── Crop definitions ──────────────────────────────────────────────────────

    /**
     * Defines a growable crop type.
     */
    public enum CropType {
        /** Potato: 15-min grow, yields 2–4. */
        POTATO(Material.POTATO_SEED, Material.POTATO, 15.0f * 60f, 2, 4),
        /** Carrot: 10-min grow, yields 2–3. */
        CARROT(Material.CARROT_SEED, Material.CARROT, 10.0f * 60f, 2, 3),
        /** Cabbage: 20-min grow, yields 1–2. */
        CABBAGE(Material.CABBAGE_SEED, Material.CABBAGE, 20.0f * 60f, 1, 2),
        /** Sunflower: 8-min grow, yields 1. */
        SUNFLOWER(Material.SUNFLOWER_SEED, Material.SUNFLOWER, 8.0f * 60f, 1, 1);

        private final Material seedMaterial;
        private final Material produceMaterial;
        private final float growTimeSec;   // base grow time in game-seconds
        private final int minYield;
        private final int maxYield;

        CropType(Material seedMaterial, Material produceMaterial, float growTimeSec,
                 int minYield, int maxYield) {
            this.seedMaterial   = seedMaterial;
            this.produceMaterial = produceMaterial;
            this.growTimeSec    = growTimeSec;
            this.minYield       = minYield;
            this.maxYield       = maxYield;
        }

        public Material getSeedMaterial()    { return seedMaterial; }
        public Material getProduceMaterial() { return produceMaterial; }
        public float getGrowTimeSec()        { return growTimeSec; }
        public int getMinYield()             { return minYield; }
        public int getMaxYield()             { return maxYield; }

        /** Resolve CropType from a seed material, or null if not a seed. */
        public static CropType fromSeed(Material seed) {
            for (CropType t : values()) {
                if (t.seedMaterial == seed) return t;
            }
            return null;
        }
    }

    /**
     * Neighbour event types fired every 5 in-game minutes.
     */
    public enum NeighbourEvent {
        COMPLIMENT, COMPLAINT, GIFT, RIVALRY
    }

    // ── Result enums ──────────────────────────────────────────────────────────

    public enum ClaimResult {
        /** Plot claimed successfully. */
        SUCCESS,
        /** Player already owns a plot. */
        ALREADY_HAS_PLOT,
        /** No free plots and player cannot afford buy-out. */
        NO_PLOTS_AVAILABLE,
        /** Warden is closed (outside open hours). */
        WARDEN_CLOSED,
        /** Warden NPC type is wrong. */
        WRONG_NPC
    }

    public enum PlantResult {
        SUCCESS,
        /** Block is not in the player's plot area. */
        WRONG_BLOCK,
        /** Block already has a crop. */
        ALREADY_PLANTED,
        /** Player doesn't hold the seed item. */
        NO_SEED,
        /** Player has no plot. */
        NO_PLOT
    }

    public enum HarvestResult {
        /** Harvest successful; produce added to inventory. */
        SUCCESS,
        /** Crop killed by frost; nothing returned, block reset. */
        KILLED_BY_FROST,
        /** Crop not yet fully grown. */
        NOT_READY,
        /** No crop at this position. */
        NO_CROP,
        /** Player has no plot. */
        NO_PLOT
    }

    // ── Inner state: CropState ────────────────────────────────────────────────

    /**
     * State for a planted crop at a specific block position.
     * Position encoded as {@code x * 10000 + z} for map keys.
     */
    public static class CropState {
        /** The crop type. */
        public final CropType type;
        /** Remaining grow time in game-seconds (counts down). */
        public float remainingSeconds;
        /** Current growth stage: 0 = sprout, 1 = mid, 2 = fully grown. */
        public int stage;
        /** True if FROST killed this crop before it reached stage 1. */
        public boolean killedByFrost;
        /** True if this crop has been watered today. */
        public boolean wateredToday;

        public CropState(CropType type) {
            this.type             = type;
            this.remainingSeconds = type.getGrowTimeSec();
            this.stage            = 0;
            this.killedByFrost    = false;
            this.wateredToday     = false;
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Whether the player currently holds a plot (-1 = no plot). */
    private int playerPlotIndex = -1;

    /** Ownership of each plot slot (true = occupied by player or NPC). */
    private final boolean[] plotOccupied = new boolean[TOTAL_PLOTS];

    /**
     * Active crops by encoded block position (x * 10000 + z).
     */
    private final Map<Integer, CropState> crops = new HashMap<>();

    /** Consecutive fallow days (no harvest). */
    private int fallowDays = 0;

    /** Whether a repossession notice is pending. */
    private boolean repossessionNoticePending = false;

    /** Whether the plot is in the grace period (1 day after notice). */
    private boolean inGracePeriod = false;

    /** In-game day of last harvest (for fallow tracking). */
    private int lastHarvestDay = -1;

    /** Total crops harvested (for SELF_SUFFICIENT achievement). */
    private int totalHarvested = 0;

    /** Harvests this week (for Veg Show weight). */
    private int weeklyHarvestCount = 0;

    /** Timer for next neighbour event (game-seconds). */
    private float neighbourEventTimer = NEIGHBOUR_EVENT_INTERVAL;

    /** Active complaint timer (>0 = complaint pending). */
    private float complaintTimer = 0f;

    /** Consecutive compliments without a complaint (for GOOD_NEIGHBOUR). */
    private int complimentsWithoutComplaint = 0;

    /** Whether a rivalry event is active (affects Veg Show scoring). */
    private boolean rivalryActive = false;

    /** Whether player has entered the current Veg Show. */
    private boolean showEnteredThisWeek = false;

    /** Last Veg Show day processed. */
    private int lastShowDay = -1;

    /** Whether the last show was won. */
    private boolean lastShowWon = false;

    /** Last newspaper headline from show (for test verification). */
    private String lastShowHeadline = null;

    private final Random random;
    private AchievementSystem achievementSystem;
    private StreetSkillSystem skillSystem;
    private NewspaperSystem newspaperSystem;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private FactionSystem factionSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public AllotmentSystem() {
        this(new Random());
    }

    public AllotmentSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ──────────────────────────────────────────────────

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setSkillSystem(StreetSkillSystem skillSystem) {
        this.skillSystem = skillSystem;
    }

    public void setNewspaperSystem(NewspaperSystem newspaperSystem) {
        this.newspaperSystem = newspaperSystem;
    }

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    // ── Plot claiming ──────────────────────────────────────────────────────────

    /**
     * Attempt to claim an allotment plot from the warden NPC.
     *
     * @param player    the player
     * @param warden    the ALLOTMENT_WARDEN NPC
     * @param inventory the player's inventory
     * @param currentHour current in-game hour (for open hours check)
     * @return ClaimResult
     */
    public ClaimResult claimPlot(Player player, NPC warden, Inventory inventory,
                                  float currentHour) {
        if (warden == null || warden.getType() != NPCType.ALLOTMENT_WARDEN) {
            return ClaimResult.WRONG_NPC;
        }
        if (currentHour < WARDEN_OPEN_HOUR || currentHour >= WARDEN_CLOSE_HOUR) {
            return ClaimResult.WARDEN_CLOSED;
        }
        if (playerPlotIndex >= 0) {
            return ClaimResult.ALREADY_HAS_PLOT;
        }

        // Find a free plot
        for (int i = 0; i < TOTAL_PLOTS; i++) {
            if (!plotOccupied[i]) {
                plotOccupied[i] = true;
                playerPlotIndex = i;
                if (inventory != null) {
                    inventory.addItem(Material.PLOT_DEED, 1);
                }
                fallowDays = 0;
                lastHarvestDay = -1;
                repossessionNoticePending = false;
                inGracePeriod = false;
                warden.setSpeechText("Right then, Plot " + (i + 1) + " is yours. Use it or lose it, pal.", 5f);
                return ClaimResult.SUCCESS;
            }
        }

        // All occupied — buy out for 30 coins
        if (inventory == null || inventory.getItemCount(Material.COIN) < BUYOUT_COST) {
            return ClaimResult.NO_PLOTS_AVAILABLE;
        }
        inventory.removeItem(Material.COIN, BUYOUT_COST);
        // Take plot 0 (first occupied)
        playerPlotIndex = 0;
        if (inventory != null) {
            inventory.addItem(Material.PLOT_DEED, 1);
        }
        fallowDays = 0;
        lastHarvestDay = -1;
        repossessionNoticePending = false;
        inGracePeriod = false;
        warden.setSpeechText("Thirty coins. Plot 1's yours now. Don't waste it.", 5f);
        return ClaimResult.SUCCESS;
    }

    /**
     * Convenience overload for tests using default open hours.
     */
    public ClaimResult claimPlot(Player player, NPC warden) {
        // Create a stub inventory-free claim (used in integration tests that
        // pass inventory separately via method chaining or direct inventory access).
        // Use open hours by default.
        return claimPlot(player, warden, null, 12.0f);
    }

    /**
     * Claim plot with explicit inventory for testing.
     */
    public ClaimResult claimPlot(Player player, NPC warden, Inventory inventory) {
        return claimPlot(player, warden, inventory, 12.0f);
    }

    // ── Crop planting ──────────────────────────────────────────────────────────

    /**
     * Encode a block position as an integer key.
     */
    private int encodePos(int x, int z) {
        return x * 100000 + z;
    }

    /**
     * Attempt to plant a seed at the given block position.
     *
     * @param player    the player (must have a plot and hold the seed)
     * @param blockX    X coordinate of the DIRT block
     * @param blockZ    Z coordinate of the DIRT block
     * @param seedItem  the seed material to plant
     * @param inventory the player's inventory
     * @return PlantResult
     */
    public PlantResult plantCrop(Player player, int blockX, int blockZ,
                                  Material seedItem, Inventory inventory) {
        if (playerPlotIndex < 0) {
            return PlantResult.NO_PLOT;
        }
        CropType cropType = CropType.fromSeed(seedItem);
        if (cropType == null) {
            return PlantResult.NO_SEED;
        }
        if (inventory != null && inventory.getItemCount(seedItem) < 1) {
            return PlantResult.NO_SEED;
        }
        int key = encodePos(blockX, blockZ);
        if (crops.containsKey(key)) {
            return PlantResult.ALREADY_PLANTED;
        }
        // Consume seed
        if (inventory != null) {
            inventory.removeItem(seedItem, 1);
        }
        CropState state = new CropState(cropType);
        crops.put(key, state);
        // Reset fallow tracking since something was planted
        fallowDays = 0;
        repossessionNoticePending = false;
        inGracePeriod = false;
        return PlantResult.SUCCESS;
    }

    /**
     * Convenience: plant without explicit inventory (e.g. in tests that set inventory separately).
     */
    public PlantResult plantCrop(Player player, int blockX, int blockZ, Material seedItem) {
        return plantCrop(player, blockX, blockZ, seedItem, null);
    }

    // ── Watering ──────────────────────────────────────────────────────────────

    /**
     * Water a crop at the given position (right-click with BUCKET).
     * Reduces remaining grow time by 30%. Bucket is not consumed.
     *
     * @param player    the player
     * @param blockX    X coordinate
     * @param blockZ    Z coordinate
     * @param inventory the player's inventory
     * @return true if watered successfully
     */
    public boolean waterCrop(Player player, int blockX, int blockZ, Inventory inventory) {
        int key = encodePos(blockX, blockZ);
        CropState state = crops.get(key);
        if (state == null || state.killedByFrost) return false;
        // Bucket is assumed held by the player (not consumed); no inventory item check needed
        if (!state.wateredToday) {
            state.remainingSeconds *= (1.0f - WATERING_REDUCTION);
            state.wateredToday = true;
        }
        return true;
    }

    /**
     * Convenience overload (no inventory check — used in tests).
     */
    public boolean waterCrop(Player player, int blockX, int blockZ) {
        return waterCrop(player, blockX, blockZ, null);
    }

    // ── Harvesting ────────────────────────────────────────────────────────────

    /**
     * Attempt to harvest a fully grown crop at the given position.
     *
     * @param player    the player
     * @param blockX    X coordinate
     * @param blockZ    Z coordinate
     * @param inventory the player's inventory
     * @param currentDay current in-game day (for fallow reset)
     * @return HarvestResult
     */
    public HarvestResult harvestCrop(Player player, int blockX, int blockZ,
                                      Inventory inventory, int currentDay) {
        if (playerPlotIndex < 0) return HarvestResult.NO_PLOT;
        int key = encodePos(blockX, blockZ);
        CropState state = crops.get(key);
        if (state == null) return HarvestResult.NO_CROP;

        if (state.killedByFrost) {
            crops.remove(key);
            return HarvestResult.KILLED_BY_FROST;
        }
        if (state.stage < 2) {
            return HarvestResult.NOT_READY;
        }

        // Harvest success
        int yield = state.type.getMinYield() +
                random.nextInt(state.type.getMaxYield() - state.type.getMinYield() + 1);
        if (inventory != null) {
            inventory.addItem(state.type.getProduceMaterial(), yield);
        }
        crops.remove(key);

        // Update fallow tracking
        lastHarvestDay = currentDay;
        fallowDays = 0;
        repossessionNoticePending = false;
        inGracePeriod = false;
        totalHarvested++;
        weeklyHarvestCount++;

        // Cancel complaint if one was pending
        if (complaintTimer > 0f) {
            complaintTimer = 0f;
        }

        // Skills
        if (skillSystem != null) {
            skillSystem.awardXP(StreetSkillSystem.Skill.GRAFTING, XP_HARVEST);
        }

        // Achievements
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.GREEN_FINGERS);
            if (totalHarvested >= SELF_SUFFICIENT_TARGET) {
                achievementSystem.unlock(AchievementType.SELF_SUFFICIENT);
            }
        }

        return HarvestResult.SUCCESS;
    }

    /**
     * Convenience overload (day defaults to 0; used in basic tests).
     */
    public HarvestResult harvestCrop(Player player, int blockX, int blockZ,
                                      Inventory inventory) {
        return harvestCrop(player, blockX, blockZ, inventory, 0);
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Update all crop growth, neighbour events, complaint timers, and
     * daily fallow tracking.
     *
     * @param delta         game-seconds since last frame
     * @param currentHour   current in-game hour
     * @param currentDay    current in-game day
     * @param weather       current weather (may be null)
     * @param playerOnSite  true if the player is near the allotments
     * @param inventory     player's inventory (for complaint fine deduction)
     * @param allNpcs       all NPCs (for neighbour speech; may be null)
     */
    public void update(float delta, float currentHour, int currentDay,
                       Weather weather, boolean playerOnSite, Inventory inventory,
                       java.util.List<NPC> allNpcs) {

        if (playerPlotIndex < 0) return;

        // ── Crop growth ──
        updateCropGrowth(delta, weather);

        // ── Daily fallow tracking ──
        updateFallowTracking(currentDay);

        // ── Complaint timer ──
        if (complaintTimer > 0f) {
            complaintTimer -= delta;
            if (complaintTimer <= 0f) {
                complaintTimer = 0f;
                // Deduct fine
                if (inventory != null && inventory.getItemCount(Material.COIN) >= COMPLAINT_FINE) {
                    inventory.removeItem(Material.COIN, COMPLAINT_FINE);
                }
            }
        }

        // ── Neighbour events (only while player is on site) ──
        if (playerOnSite) {
            neighbourEventTimer -= delta;
            if (neighbourEventTimer <= 0f) {
                neighbourEventTimer = NEIGHBOUR_EVENT_INTERVAL;
                fireNeighbourEvent(inventory, allNpcs);
            }
        }
    }

    private void updateCropGrowth(float delta, Weather weather) {
        for (CropState state : crops.values()) {
            if (state.killedByFrost || state.stage >= 2) continue;

            // FROST kills stage-0 crops
            if (weather == Weather.FROST && state.stage == 0) {
                state.killedByFrost = true;
                continue;
            }

            // RAIN/DRIZZLE auto-waters
            if (!state.wateredToday &&
                    (weather == Weather.RAIN || weather == Weather.DRIZZLE)) {
                state.wateredToday = true;
            }

            // HEATWAVE slows growth (×1.5 duration unless watered today)
            float effectiveDelta = delta;
            if (weather == Weather.HEATWAVE && !state.wateredToday) {
                effectiveDelta = delta / HEATWAVE_SLOW_MULTIPLIER;
            }

            state.remainingSeconds -= effectiveDelta;
            if (state.remainingSeconds < 0f) state.remainingSeconds = 0f;

            // Update stage based on proportion grown
            float total = state.type.getGrowTimeSec();
            float grown = total - state.remainingSeconds;
            float fraction = grown / total;
            if (fraction >= 1.0f) {
                state.stage = 2;
            } else if (fraction >= 0.5f) {
                state.stage = 1;
            }
        }
    }

    private void updateFallowTracking(int currentDay) {
        if (lastHarvestDay < 0) {
            // Never harvested — day counter runs from start
            // This is managed externally via advanceDay()
        }
    }

    /**
     * Called once per in-game day to advance fallow tracking.
     *
     * @param currentDay the current in-game day number
     */
    public void advanceDay(int currentDay) {
        if (playerPlotIndex < 0) return;

        // Reset watered-today flags
        for (CropState state : crops.values()) {
            state.wateredToday = false;
        }

        // Check if anything was harvested today
        boolean harvestedToday = (lastHarvestDay == currentDay);
        if (!harvestedToday && crops.isEmpty()) {
            // No crops growing and nothing harvested today
            fallowDays++;
        } else {
            fallowDays = 0;
        }

        // Repossession logic
        if (fallowDays >= FALLOW_DAYS_THRESHOLD && !repossessionNoticePending) {
            repossessionNoticePending = true;
            inGracePeriod = true;
        } else if (repossessionNoticePending && inGracePeriod) {
            // Grace period expired — repossess
            repossessPlot(null);
        }
    }

    /**
     * Called once per in-game day from external game loop.
     * Advances fallow tracking and repossession.
     *
     * @param currentDay  current in-game day
     * @param inventory   player's inventory (to remove PLOT_DEED on repossession)
     */
    public void advanceDay(int currentDay, Inventory inventory) {
        if (playerPlotIndex < 0) return;

        // Reset watered-today flags
        for (CropState state : crops.values()) {
            state.wateredToday = false;
        }

        // Check if anything was harvested on this day
        boolean harvestedToday = (lastHarvestDay == currentDay);
        // Also consider: if crops exist and are in progress, plot is not fallow
        boolean plotActive = !crops.isEmpty() || harvestedToday;

        if (!plotActive) {
            fallowDays++;
        } else {
            fallowDays = 0;
        }

        // Repossession logic
        if (!repossessionNoticePending && fallowDays >= FALLOW_DAYS_THRESHOLD) {
            repossessionNoticePending = true;
            inGracePeriod = true;
        } else if (repossessionNoticePending && inGracePeriod && !plotActive) {
            // Still fallow during grace period — repossess
            repossessPlot(inventory);
        } else if (repossessionNoticePending && plotActive) {
            // Player planted something — reset
            repossessionNoticePending = false;
            inGracePeriod = false;
            fallowDays = 0;
        }
    }

    private void repossessPlot(Inventory inventory) {
        if (playerPlotIndex < 0) return;
        plotOccupied[playerPlotIndex] = false;
        playerPlotIndex = -1;
        crops.clear();
        fallowDays = 0;
        repossessionNoticePending = false;
        inGracePeriod = false;
        if (inventory != null) {
            inventory.removeItem(Material.PLOT_DEED, 1);
        }
        // Council respect penalty
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.THE_COUNCIL, -5);
        }
    }

    // ── Neighbour events ──────────────────────────────────────────────────────

    private void fireNeighbourEvent(Inventory inventory, java.util.List<NPC> allNpcs) {
        NeighbourEvent event = NeighbourEvent.values()[random.nextInt(NeighbourEvent.values().length)];

        // Find a PLOT_NEIGHBOUR NPC to deliver speech
        NPC neighbour = findNeighbourNpc(allNpcs);

        switch (event) {
            case COMPLIMENT:
                if (neighbour != null) {
                    neighbour.setSpeechText("Them carrots are coming on lovely.", 5f);
                }
                // Street Lads respect +1
                if (factionSystem != null) {
                    factionSystem.applyRespectDelta(Faction.STREET_LADS, 1);
                }
                complimentsWithoutComplaint++;
                if (achievementSystem != null &&
                        complimentsWithoutComplaint >= GOOD_NEIGHBOUR_COMPLIMENTS) {
                    achievementSystem.unlock(AchievementType.GOOD_NEIGHBOUR);
                }
                break;

            case COMPLAINT:
                if (neighbour != null) {
                    neighbour.setSpeechText("Your weeds are blowing onto my patch.", 5f);
                }
                complaintTimer = COMPLAINT_WINDOW_SECONDS;
                complimentsWithoutComplaint = 0; // break streak
                break;

            case GIFT:
                if (inventory != null) {
                    Material gift = random.nextBoolean() ? Material.POTATO : Material.CARROT;
                    inventory.addItem(gift, 1);
                }
                if (neighbour != null) {
                    neighbour.setSpeechText("Here, take some of these — got more than I need.", 5f);
                }
                break;

            case RIVALRY:
                rivalryActive = true;
                if (neighbour != null) {
                    neighbour.setSpeechText("Mine'll be bigger than yours at the show.", 5f);
                }
                break;
        }
    }

    private NPC findNeighbourNpc(java.util.List<NPC> allNpcs) {
        if (allNpcs == null) return null;
        for (NPC npc : allNpcs) {
            if (npc != null && npc.isAlive() && npc.getType() == NPCType.PLOT_NEIGHBOUR) {
                return npc;
            }
        }
        return null;
    }

    // ── Giant Vegetable Show ──────────────────────────────────────────────────

    /**
     * Check whether the Veg Show should run (day 7/14/21 at 12:00).
     *
     * @param currentDay  current in-game day
     * @param currentHour current in-game hour
     * @return true if a show should fire now
     */
    public boolean isShowTime(int currentDay, float currentHour) {
        return currentDay > 0
            && currentDay % 7 == 0
            && currentDay != lastShowDay
            && currentHour >= 12.0f && currentHour < 13.0f;
    }

    /**
     * Run the Giant Vegetable Show.
     *
     * @param player              the player
     * @param inventory           the player's inventory
     * @param currentDay          the current in-game day
     * @param deterministicRng    if non-null, overrides internal random for testing
     *                            (value in [0,1); value &lt; 0.5 = player wins)
     * @return true if the player won
     */
    public boolean runShow(Player player, Inventory inventory, int currentDay,
                           Float deterministicRng) {
        if (playerPlotIndex < 0) return false;

        lastShowDay = currentDay;
        showEnteredThisWeek = true;

        // INFLUENCE XP for entering
        if (skillSystem != null) {
            skillSystem.awardXP(StreetSkillSystem.Skill.INFLUENCE, XP_SHOW_ENTRY);
        }

        // Determine winner
        // Player weight: cumulative weekly harvest count (minimum 1)
        int playerWeight = Math.max(1, weeklyHarvestCount);
        // Neighbour weight: fixed random 2–5
        int neighbourWeight = 2 + random.nextInt(4);
        // Rivalry bonus for neighbour
        if (rivalryActive) {
            neighbourWeight += 2;
            rivalryActive = false;
        }

        boolean playerWins;
        if (deterministicRng != null) {
            // Use deterministic value: fraction of total weight for player
            float playerFraction = (float) playerWeight / (playerWeight + neighbourWeight);
            playerWins = deterministicRng < playerFraction;
        } else {
            // Weighted RNG
            int total = playerWeight + neighbourWeight;
            playerWins = random.nextInt(total) < playerWeight;
        }

        weeklyHarvestCount = 0; // reset for next week

        if (playerWins) {
            lastShowWon = true;
            // Award 15 coins
            if (inventory != null) {
                inventory.addItem(Material.COIN, VEG_SHOW_WIN_COINS);
            }
            // Achievement
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.CHAMPION_GROWER);
            }
            // Newspaper headline
            lastShowHeadline = VEG_SHOW_HEADLINE;
            if (newspaperSystem != null) {
                newspaperSystem.recordEvent(new NewspaperSystem.InfamyEvent(
                    "VEG_SHOW_WIN",
                    "Allotments",
                    null,
                    null,
                    0,
                    null,
                    null,
                    1
                ));
            }
        } else {
            lastShowWon = false;
        }

        return playerWins;
    }

    /**
     * Convenience overload for tests (uses internal RNG).
     */
    public boolean runShow(Player player, Inventory inventory, int currentDay) {
        return runShow(player, inventory, currentDay, null);
    }

    // ── Trespassing ───────────────────────────────────────────────────────────

    /**
     * Called when the player attempts to enter the allotments outside warden hours.
     * Records a trespassing criminal record entry.
     *
     * @param currentHour current in-game hour
     */
    public void onTrespassAttempt(float currentHour) {
        if (currentHour < WARDEN_OPEN_HOUR || currentHour >= WARDEN_CLOSE_HOUR) {
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.TRESPASSING);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(NotorietySystem.GAIN_BLOCK_BREAK, null);
            }
        }
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    /** True if the player currently owns a plot. */
    public boolean hasPlot(Player player) {
        return playerPlotIndex >= 0;
    }

    /** Returns the player's current plot index (0-based), or -1 if none. */
    public int getPlayerPlotIndex() {
        return playerPlotIndex;
    }

    /** Returns the number of fallow days. */
    public int getFallowDays() {
        return fallowDays;
    }

    /** True if a repossession notice is currently pending. */
    public boolean isRepossessionNoticePending() {
        return repossessionNoticePending;
    }

    /** Returns the CropState at the given position, or null. */
    public CropState getCropState(int blockX, int blockZ) {
        return crops.get(encodePos(blockX, blockZ));
    }

    /** True if the crop at the given position has been killed by frost. */
    public boolean isCropKilled(int blockX, int blockZ) {
        CropState state = getCropState(blockX, blockZ);
        return state != null && state.killedByFrost;
    }

    /** Returns total crops harvested. */
    public int getTotalHarvested() {
        return totalHarvested;
    }

    /** Returns weekly harvest count (for Veg Show). */
    public int getWeeklyHarvestCount() {
        return weeklyHarvestCount;
    }

    /** Returns the complaint timer remaining (0 = no active complaint). */
    public float getComplaintTimer() {
        return complaintTimer;
    }

    /** True if rivalry event is active (affects Veg Show scoring). */
    public boolean isRivalryActive() {
        return rivalryActive;
    }

    /** Returns the last show headline (for testing). */
    public String getLastShowHeadline() {
        return lastShowHeadline;
    }

    /** True if the last show was won. */
    public boolean wasLastShowWon() {
        return lastShowWon;
    }

    /** Returns current number of consecutive compliments without complaint. */
    public int getComplimentsWithoutComplaint() {
        return complimentsWithoutComplaint;
    }

    // ── Test helpers ──────────────────────────────────────────────────────────

    /** Set the neighbour event timer directly (for testing). */
    public void setNeighbourEventTimerForTesting(float timer) {
        this.neighbourEventTimer = timer;
    }

    /** Set the fallow days directly (for testing). */
    public void setFallowDaysForTesting(int days) {
        this.fallowDays = days;
    }

    /** Set the last harvest day (for testing). */
    public void setLastHarvestDayForTesting(int day) {
        this.lastHarvestDay = day;
    }

    /** Set repossession notice pending (for testing). */
    public void setRepossessionNoticePendingForTesting(boolean pending) {
        this.repossessionNoticePending = pending;
    }

    /** Set in grace period (for testing). */
    public void setInGracePeriodForTesting(boolean grace) {
        this.inGracePeriod = grace;
    }

    /** Set player plot index directly (for testing without going through claimPlot). */
    public void setPlayerPlotIndexForTesting(int index) {
        this.playerPlotIndex = index;
        if (index >= 0 && index < TOTAL_PLOTS) {
            this.plotOccupied[index] = true;
        }
    }

    /** Set weekly harvest count (for testing Veg Show). */
    public void setWeeklyHarvestCountForTesting(int count) {
        this.weeklyHarvestCount = count;
    }
}

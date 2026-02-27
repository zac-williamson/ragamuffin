package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.BlockType;
import ragamuffin.world.Landmark;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The Living Neighbourhood — Dynamic Gentrification, Decay &amp; Reclamation (Issue #793).
 *
 * <h3>Overview</h3>
 * Buildings decay over time with visible block-level changes. The Council rolls out
 * gentrification waves replacing crumbling structures with sterile CONCRETE_PANEL
 * luxury flats. Street Lads reclaim abandoned sites with graffiti. Marchetti Crew
 * fortify their territory with METAL_SHUTTER blocks.
 *
 * <h3>Neighbourhood Vibes</h3>
 * A 0–100 score aggregated each in-game minute from building conditions, faction
 * scores, graffiti count, luxury flat count, active raves, and Notoriety. Five
 * threshold states cause cascading gameplay effects.
 *
 * <h3>Condition Decay States</h3>
 * <ul>
 *   <li>≥70: Normal</li>
 *   <li>50–69: Crumbling — CRUMBLED_BRICK appears on exterior</li>
 *   <li>30–49: Derelict — windows shatter, CONDEMNED_NOTICE prop, COUNCIL_NOTICE rumour</li>
 *   <li>10–29: Ruin — roof blocks removed, BOARDED_WOOD appears</li>
 *   <li>&lt;10: Demolition Ready — council builders arrive in 2 in-game hours</li>
 * </ul>
 *
 * <h3>Vibes Thresholds</h3>
 * <ul>
 *   <li>≥80: Thriving — bonus coins, ambient music on street</li>
 *   <li>50–79: Normal</li>
 *   <li>30–49: Tense — police patrol rate +25%</li>
 *   <li>10–29: Hostile — NPCs flee on sight, fence prices −15%</li>
 *   <li>&lt;10: Dystopia — ambient sound silenced, fog halved, overnight block crumbling</li>
 * </ul>
 */
public class NeighbourhoodSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Default condition for a building when first tracked. */
    public static final int DEFAULT_CONDITION = 80;

    /** Maximum building condition. */
    public static final int MAX_CONDITION = 100;

    /** Condition threshold below which CRUMBLED_BRICK appears (Crumbling state). */
    public static final int CONDITION_CRUMBLING   = 70;
    /** Condition threshold below which building becomes Derelict. */
    public static final int CONDITION_DERELICT    = 50;
    /** Condition threshold below which building is a Ruin. */
    public static final int CONDITION_RUIN        = 30;
    /** Condition threshold below which building is Demolition Ready. */
    public static final int CONDITION_DEMOLITION  = 10;

    /** Condition lost per in-game minute when a building is unoccupied. */
    public static final int DECAY_PER_MINUTE  = 1;
    /** Condition lost when the player breaks a block in the building. */
    public static final int DECAY_PER_BLOCK_BREAK = 3;
    /** Condition lost when graffiti is placed on the building. */
    public static final int DECAY_PER_GRAFFITI    = 2;

    /** Condition gain when player tears down a condemned notice. */
    public static final int CONDITION_NOTICE_TEARDOWN = 10;
    /** Condition gain when a pirate radio broadcast happens from inside a derelict building. */
    public static final int CONDITION_RADIO_BOOST  = 5;
    /** Condition gain when a community meeting is organised. */
    public static final int CONDITION_COMMUNITY_MEETING = 30;

    /** Condition cap for Street Lads graffiti campaign (when ≥4 faces tagged). */
    public static final int STREET_LADS_GRAFFITI_CAP = 60;

    /** Council territory fraction threshold that triggers gentrification waves. */
    public static final float COUNCIL_GENTRIFY_THRESHOLD = 0.50f;
    /** In-game seconds between gentrification waves (3 in-game hours). */
    public static final float GENTRIFY_INTERVAL_SECONDS  = 10800f;
    /** In-game seconds before council builders arrive for demolition-ready buildings (2 in-game hours). */
    public static final float DEMOLITION_ARRIVAL_SECONDS = 7200f;

    /** Street Lads graffiti response delay in in-game seconds (1 in-game hour). */
    public static final float STREET_LADS_RESPONSE_SECONDS = 3600f;
    /** Duration Street Lads remain hostile after a Luxury Flat appears. */
    public static final float STREET_LADS_HOSTILE_SECONDS  = 300f; // 5 in-game minutes

    /** Marchetti territory fraction above which shutters are placed. */
    public static final float MARCHETTI_SHUTTER_THRESHOLD = 0.40f;

    /** Vibes threshold for Thriving state. */
    public static final int VIBES_THRIVING  = 80;
    /** Vibes threshold for Normal state (lower bound). */
    public static final int VIBES_NORMAL    = 50;
    /** Vibes threshold for Tense state (lower bound). */
    public static final int VIBES_TENSE     = 30;
    /** Vibes threshold for Hostile state (lower bound). */
    public static final int VIBES_HOSTILE   = 10;
    // Below VIBES_HOSTILE = Dystopia

    /** Police patrol rate multiplier in Tense state. */
    public static final float TENSE_POLICE_MULT    = 1.25f;
    /** Fence price multiplier reduction in Hostile state. */
    public static final float HOSTILE_FENCE_DISCOUNT = 0.85f;

    /** Number of coins from luxury flat sale to developers. */
    public static final int SELL_TO_DEVELOPERS_COINS = 30;
    /** Notoriety increase when selling to developers. */
    public static final int SELL_TO_DEVELOPERS_NOTORIETY = 20;
    /** Minimum nearby NPCs needed for a community meeting. */
    public static final int COMMUNITY_MEETING_MIN_NPCS = 5;

    /** Vibes boost from an active rave in the area. */
    public static final int VIBES_RAVE_BOOST = 8;
    /** Vibes boost from pirate radio broadcast from derelict building. */
    public static final int VIBES_RADIO_BOOST = 8;
    /** BootSale extra buyers in high-Vibes areas. */
    public static final int BOOT_SALE_EXTRA_BUYERS = 2;
    /** BootSale auction price multiplier in high-Vibes areas. */
    public static final float BOOT_SALE_VIBES_PRICE_MULT = 1.15f;

    /** In-game seconds per minute (for decay timing). */
    public static final float SECONDS_PER_IN_GAME_MINUTE = 1f; // 1 real second = 1 in-game minute

    // ── Building Condition State enum ─────────────────────────────────────────

    public enum ConditionState {
        NORMAL,
        CRUMBLING,
        DERELICT,
        RUIN,
        DEMOLITION_READY
    }

    // ── Vibes State enum ──────────────────────────────────────────────────────

    public enum VibesState {
        THRIVING,
        NORMAL,
        TENSE,
        HOSTILE,
        DYSTOPIA
    }

    // ── Inner class: BuildingRecord ───────────────────────────────────────────

    /**
     * Tracks the dynamic condition of a single building.
     */
    public static class BuildingRecord {
        private final LandmarkType type;
        private final int worldX;
        private final int worldZ;
        private int condition;
        private ConditionState state;
        private boolean squatted;
        private boolean condemned;
        private boolean luxuryFlat;
        private boolean graffitiCapped;
        private int graffitiTagCount;
        private float demolitionTimer; // seconds until council builders arrive
        private float decayAccumulator; // sub-minute decay accumulator

        public BuildingRecord(LandmarkType type, int worldX, int worldZ) {
            this.type              = type;
            this.worldX            = worldX;
            this.worldZ            = worldZ;
            this.condition         = DEFAULT_CONDITION;
            this.state             = ConditionState.NORMAL;
            this.squatted          = false;
            this.condemned         = false;
            this.luxuryFlat        = false;
            this.graffitiCapped    = false;
            this.graffitiTagCount  = 0;
            this.demolitionTimer   = -1f;
            this.decayAccumulator  = 0f;
        }

        public LandmarkType getType()    { return type; }
        public int getWorldX()           { return worldX; }
        public int getWorldZ()           { return worldZ; }

        public int getCondition()        { return condition; }
        public void setCondition(int v)  { condition = Math.max(0, Math.min(MAX_CONDITION, v)); }
        public void applyConditionDelta(int delta) { setCondition(condition + delta); }

        public ConditionState getState() { return state; }
        public void setState(ConditionState s) { state = s; }

        public boolean isSquatted()       { return squatted; }
        public void setSquatted(boolean v) { squatted = v; }

        public boolean isCondemned()      { return condemned; }
        public void setCondemned(boolean v) { condemned = v; }

        public boolean isLuxuryFlat()     { return luxuryFlat; }
        public void setLuxuryFlat(boolean v) { luxuryFlat = v; }

        public boolean isGraffitiCapped() { return graffitiCapped; }
        public void setGraffitiCapped(boolean v) { graffitiCapped = v; }

        public int getGraffitiTagCount()  { return graffitiTagCount; }
        public void incrementGraffitiTagCount() { graffitiTagCount++; }

        public float getDemolitionTimer() { return demolitionTimer; }
        public void setDemolitionTimer(float v) { demolitionTimer = v; }

        public float getDecayAccumulator() { return decayAccumulator; }
        public void setDecayAccumulator(float v) { decayAccumulator = v; }

        /** Returns true if the building is derelict (condition below DERELICT threshold). */
        public boolean isDerelict() { return condition < CONDITION_DERELICT; }

        /** Returns true if this building can be demolished (not squatted, demolition ready). */
        public boolean canDemolish() {
            return !squatted && condition < CONDITION_DEMOLITION;
        }

        /** Effective condition considering graffiti cap. */
        public int getEffectiveCondition() {
            if (graffitiCapped) return Math.min(condition, STREET_LADS_GRAFFITI_CAP);
            return condition;
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** All tracked buildings, keyed by "worldX,worldZ". */
    private final Map<String, BuildingRecord> buildings = new HashMap<>();

    /** Current Neighbourhood Vibes score (0–100). */
    private int vibes = 50;

    /** Previous Vibes state (to detect threshold crossings). */
    private VibesState previousVibesState = VibesState.NORMAL;

    /** Elapsed seconds since last gentrification wave check. */
    private float gentrifyTimer = 0f;

    /** Elapsed seconds since last Vibes recalculation. */
    private float vibesTimer = 0f;

    /** Number of active luxury flats. */
    private int luxuryFlatCount = 0;

    /** Number of graffiti tags across the neighbourhood. */
    private int graffitiCount = 0;

    /** Number of active raves. */
    private int activeRaveCount = 0;

    /** Pending tooltip message (polled by game HUD). */
    private String pendingTooltip = null;

    /** Whether the Dystopia state has been entered this session (for achievement). */
    private boolean dystopiaAchievementUnlocked = false;

    /** Street Lads hostile timer (seconds remaining). */
    private float streetLadsHostileTimer = 0f;

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final FactionSystem factionSystem;
    private final TurfMap turfMap;
    private final RumourNetwork rumourNetwork;
    private final AchievementSystem achievementSystem;
    private final Random random;

    // ── Constructor ───────────────────────────────────────────────────────────

    public NeighbourhoodSystem(FactionSystem factionSystem,
                               TurfMap turfMap,
                               RumourNetwork rumourNetwork,
                               AchievementSystem achievementSystem,
                               Random random) {
        this.factionSystem     = factionSystem;
        this.turfMap           = turfMap;
        this.rumourNetwork     = rumourNetwork;
        this.achievementSystem = achievementSystem;
        this.random            = random;
    }

    // ── Building registration ─────────────────────────────────────────────────

    /**
     * Register a building for tracking. Should be called at world-gen time for
     * each landmark.
     */
    public void registerBuilding(LandmarkType type, int worldX, int worldZ) {
        String key = key(worldX, worldZ);
        if (!buildings.containsKey(key)) {
            buildings.put(key, new BuildingRecord(type, worldX, worldZ));
        }
    }

    /**
     * Register a building from a Landmark object.
     */
    public void registerBuilding(Landmark landmark) {
        registerBuilding(
            landmark.getType(),
            (int) landmark.getPosition().x,
            (int) landmark.getPosition().z
        );
    }

    /** Returns the BuildingRecord for the building at (worldX, worldZ), or null. */
    public BuildingRecord getBuilding(int worldX, int worldZ) {
        return buildings.get(key(worldX, worldZ));
    }

    /** Returns all registered BuildingRecords (unmodifiable). */
    public Map<String, BuildingRecord> getBuildings() {
        return Collections.unmodifiableMap(buildings);
    }

    // ── Update loop ───────────────────────────────────────────────────────────

    /**
     * Called each frame with the time delta in real seconds.
     *
     * @param delta       real-seconds since last frame
     * @param world       the voxel world (for block mutations)
     * @param allNpcs     all living NPCs
     * @param notoriety   player notoriety score (0–100)
     * @param playerX     player world X
     * @param playerZ     player world Z
     */
    public void update(float delta, World world, List<NPC> allNpcs,
                       int notoriety, float playerX, float playerZ) {
        // Tick building decay every in-game minute
        for (BuildingRecord rec : buildings.values()) {
            tickBuildingDecay(rec, delta, world, allNpcs);
        }

        // Recalculate Vibes every in-game minute
        vibesTimer += delta;
        if (vibesTimer >= SECONDS_PER_IN_GAME_MINUTE) {
            vibesTimer = 0f;
            recalculateVibes(notoriety, allNpcs);
        }

        // Gentrification wave check
        gentrifyTimer += delta;
        if (gentrifyTimer >= GENTRIFY_INTERVAL_SECONDS) {
            gentrifyTimer = 0f;
            tryGentrificationWave(world, allNpcs);
        }

        // Street Lads hostile timer
        if (streetLadsHostileTimer > 0f) {
            streetLadsHostileTimer -= delta;
        }

        // Dystopia overnight crumbling (in Dystopia state, crumble blocks)
        if (getCurrentVibesState() == VibesState.DYSTOPIA) {
            applyDystopiaEffects(delta, world);
        }
    }

    // ── Per-building decay ────────────────────────────────────────────────────

    private void tickBuildingDecay(BuildingRecord rec, float delta,
                                   World world, List<NPC> allNpcs) {
        if (rec.isLuxuryFlat()) return; // Luxury flats don't decay normally

        rec.setDecayAccumulator(rec.getDecayAccumulator() + delta);
        if (rec.getDecayAccumulator() < SECONDS_PER_IN_GAME_MINUTE) {
            return; // Haven't reached a full minute yet
        }
        rec.setDecayAccumulator(rec.getDecayAccumulator() - SECONDS_PER_IN_GAME_MINUTE);

        // Passive decay for unoccupied buildings
        if (!rec.isSquatted()) {
            rec.applyConditionDelta(-DECAY_PER_MINUTE);
        }

        // Apply graffiti cap
        if (rec.isGraffitiCapped()) {
            if (rec.getCondition() > STREET_LADS_GRAFFITI_CAP) {
                rec.setCondition(STREET_LADS_GRAFFITI_CAP);
            }
        }

        // Transition to new condition states based on thresholds
        updateConditionState(rec, world, allNpcs);

        // Demolition timer countdown
        if (rec.getDemolitionTimer() > 0f) {
            rec.setDemolitionTimer(rec.getDemolitionTimer() - SECONDS_PER_IN_GAME_MINUTE);
            if (rec.getDemolitionTimer() <= 0f && rec.canDemolish()) {
                // Council builders have arrived — building is demolition-ready
                rec.setDemolitionTimer(-1f);
                pendingTooltip = "The council has sent builders to demolish a derelict building.";
            }
        }
    }

    private void updateConditionState(BuildingRecord rec, World world, List<NPC> allNpcs) {
        int cond = rec.getCondition();
        ConditionState current = rec.getState();
        ConditionState newState = stateForCondition(cond);

        if (newState == current) return;

        rec.setState(newState);

        switch (newState) {
            case CRUMBLING:
                onEnterCrumbling(rec, world);
                break;
            case DERELICT:
                onEnterDerelict(rec, world, allNpcs);
                break;
            case RUIN:
                onEnterRuin(rec, world);
                break;
            case DEMOLITION_READY:
                onEnterDemolitionReady(rec);
                break;
            default:
                break;
        }
    }

    private ConditionState stateForCondition(int cond) {
        if (cond >= CONDITION_CRUMBLING) return ConditionState.NORMAL;
        if (cond >= CONDITION_DERELICT)  return ConditionState.CRUMBLING;
        if (cond >= CONDITION_RUIN)      return ConditionState.DERELICT;
        if (cond >= CONDITION_DEMOLITION) return ConditionState.RUIN;
        return ConditionState.DEMOLITION_READY;
    }

    private void onEnterCrumbling(BuildingRecord rec, World world) {
        // Place CRUMBLED_BRICK on some exterior walls
        if (world != null) {
            placeCrumbledBrick(world, rec.getWorldX(), rec.getWorldZ());
        }
        pendingTooltip = "A building is starting to crumble.";
    }

    private void onEnterDerelict(BuildingRecord rec, World world, List<NPC> allNpcs) {
        // Shatter windows (replace GLASS with AIR)
        if (world != null) {
            shatterWindows(world, rec.getWorldX(), rec.getWorldZ());
        }
        rec.setCondemned(true);
        // Seed COUNCIL_NOTICE rumour
        if (rumourNetwork != null && allNpcs != null) {
            seedRumour(allNpcs, RumourType.COUNCIL_NOTICE,
                "The council's been putting notices on that derelict building on the high street");
        }
        pendingTooltip = "A building has been condemned. Look for the notice.";
    }

    private void onEnterRuin(BuildingRecord rec, World world) {
        // Remove roof blocks and place BOARDED_WOOD
        if (world != null) {
            applyRuinBlocks(world, rec.getWorldX(), rec.getWorldZ());
        }
        pendingTooltip = "A building has become a ruin.";
    }

    private void onEnterDemolitionReady(BuildingRecord rec) {
        if (!rec.isSquatted()) {
            rec.setDemolitionTimer(DEMOLITION_ARRIVAL_SECONDS);
            pendingTooltip = "A building is demolition-ready. Council builders will arrive soon.";
        }
    }

    // ── Block-level mutations ─────────────────────────────────────────────────

    private void placeCrumbledBrick(World world, int cx, int cz) {
        // Place a few CRUMBLED_BRICK blocks near the building exterior (deterministic)
        for (int y = 1; y <= 3; y++) {
            for (int dx = -1; dx <= 1; dx += 2) {
                int bx = cx + dx * 4;
                BlockType existing = world.getBlock(bx, y, cz);
                if (existing == BlockType.BRICK || existing == BlockType.YELLOW_BRICK) {
                    world.setBlock(bx, y, cz, BlockType.CRUMBLED_BRICK);
                }
            }
        }
    }

    private void shatterWindows(World world, int cx, int cz) {
        // Replace GLASS blocks within a small radius with AIR
        int radius = 6;
        for (int y = 1; y <= 5; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (world.getBlock(cx + dx, y, cz + dz) == BlockType.GLASS) {
                        world.setBlock(cx + dx, y, cz + dz, BlockType.AIR);
                    }
                }
            }
        }
    }

    private void applyRuinBlocks(World world, int cx, int cz) {
        int radius = 5;
        for (int y = 4; y <= 6; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockType b = world.getBlock(cx + dx, y, cz + dz);
                    if (b == BlockType.ROOF_TILE || b == BlockType.SLATE) {
                        world.setBlock(cx + dx, y, cz + dz, BlockType.AIR);
                    }
                }
            }
        }
        // Place BOARDED_WOOD on doorways (y=1, in front of building)
        for (int dx = -2; dx <= 2; dx++) {
            BlockType b = world.getBlock(cx + dx, 1, cz - 3);
            if (b == BlockType.DOOR_LOWER || b == BlockType.DOOR_WOOD || b == BlockType.AIR) {
                world.setBlock(cx + dx, 1, cz - 3, BlockType.BOARDED_WOOD);
            }
        }
    }

    private void buildLuxuryFlat(World world, int cx, int cz, int width, int depth, int height) {
        // Demolish existing blocks and build CONCRETE_PANEL + GLASS structure
        for (int y = 0; y <= height; y++) {
            for (int dx = -width / 2; dx <= width / 2; dx++) {
                for (int dz = -depth / 2; dz <= depth / 2; dz++) {
                    world.setBlock(cx + dx, y, cz + dz, BlockType.AIR);
                }
            }
        }
        // Build walls
        for (int y = 1; y <= height; y++) {
            for (int dx = -width / 2; dx <= width / 2; dx++) {
                for (int dz = -depth / 2; dz <= depth / 2; dz++) {
                    boolean isEdge = (Math.abs(dx) == width / 2 || Math.abs(dz) == depth / 2);
                    if (isEdge) {
                        // Alternate CONCRETE_PANEL and GLASS for windows
                        boolean isWindow = (y >= 2 && y <= height - 1 && dx != -width / 2 && dx != width / 2);
                        world.setBlock(cx + dx, y, cz + dz,
                            isWindow ? BlockType.GLASS : BlockType.CONCRETE_PANEL);
                    }
                }
            }
        }
        // Roof
        for (int dx = -width / 2; dx <= width / 2; dx++) {
            for (int dz = -depth / 2; dz <= depth / 2; dz++) {
                world.setBlock(cx + dx, height + 1, cz + dz, BlockType.CONCRETE_PANEL);
            }
        }
    }

    private void placeMarchettShutters(World world, int cx, int cz) {
        // Cover doors and windows with METAL_SHUTTER
        int radius = 6;
        for (int y = 1; y <= 3; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockType b = world.getBlock(cx + dx, y, cz + dz);
                    if (b == BlockType.DOOR_LOWER || b == BlockType.DOOR_WOOD
                            || b == BlockType.GLASS) {
                        world.setBlock(cx + dx, y, cz + dz, BlockType.METAL_SHUTTER);
                    }
                }
            }
        }
    }

    // ── Gentrification wave ───────────────────────────────────────────────────

    private void tryGentrificationWave(World world, List<NPC> allNpcs) {
        if (turfMap == null) return;
        float councilFraction = turfMap.ownershipFraction(Faction.THE_COUNCIL);
        if (councilFraction <= COUNCIL_GENTRIFY_THRESHOLD) return;

        // Find most-decayed building in Council territory
        BuildingRecord target = null;
        int lowestCondition = MAX_CONDITION + 1;
        for (BuildingRecord rec : buildings.values()) {
            if (rec.isLuxuryFlat()) continue;
            if (rec.isSquatted()) continue;
            // Check if in Council territory
            Faction owner = turfMap.getOwner(rec.getWorldX(), rec.getWorldZ());
            if (owner != Faction.THE_COUNCIL) continue;
            if (rec.getCondition() < lowestCondition) {
                lowestCondition = rec.getCondition();
                target = rec;
            }
        }

        if (target == null) return;

        // Demolish and build Luxury Flat
        if (world != null) {
            buildLuxuryFlat(world, target.getWorldX(), target.getWorldZ(), 8, 8, 6);
        }
        target.setLuxuryFlat(true);
        target.setCondition(MAX_CONDITION);
        target.setState(ConditionState.NORMAL);
        luxuryFlatCount++;

        // Seed GENTRIFICATION rumour
        if (rumourNetwork != null && allNpcs != null) {
            seedRumour(allNpcs, RumourType.GENTRIFICATION,
                "Developers are moving in — prices are going up round here");
        }

        // Street Lads become hostile for 5 in-game minutes
        streetLadsHostileTimer = STREET_LADS_HOSTILE_SECONDS;
        if (factionSystem != null) {
            // They're angry — apply a small respect hit to represent their hostility
            factionSystem.applyRespectDelta(Faction.STREET_LADS, -5);
        }

        // Schedule Street Lads reclamation graffiti
        scheduleStreetLadsReclamation(target, allNpcs);

        pendingTooltip = "The Council has built a Luxury Flat — 'Prestige Living — From £850/pcm'.";
    }

    private void scheduleStreetLadsReclamation(BuildingRecord rec, List<NPC> allNpcs) {
        // Immediately trigger graffiti effect (simplified — no timer threading)
        // In a full game this would be queued; here we apply it if Street Lads have territory
        if (turfMap == null) return;
        float streetLadsFrac = turfMap.ownershipFraction(Faction.STREET_LADS);
        if (streetLadsFrac > 0f && allNpcs != null) {
            // Tag the building (simulating 2–4 NPCs arriving)
            int tags = 2 + random.nextInt(3); // 2 to 4
            for (int i = 0; i < tags; i++) {
                rec.incrementGraffitiTagCount();
            }
            if (rec.getGraffitiTagCount() >= 4) {
                rec.setGraffitiCapped(true);
            }
        }
    }

    // ── Faction reactions ─────────────────────────────────────────────────────

    /**
     * Called when a METAL_SHUTTER block is broken. Seeds GANG_ACTIVITY rumour
     * and penalises Marchetti Respect.
     *
     * @param allNpcs all living NPCs
     * @return tooltip message
     */
    public String onMarchettiShutterBroken(List<NPC> allNpcs) {
        if (rumourNetwork != null && allNpcs != null) {
            seedRumour(allNpcs, RumourType.GANG_ACTIVITY,
                "Someone's been messing with the Marchetti Crew's shutters — asking for trouble");
        }
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, -20);
        }
        return "You've broken a Marchetti shutter. They won't be happy about that.";
    }

    /**
     * Called each frame update to check if Marchetti Crew should fortify buildings.
     * Places METAL_SHUTTER over doors/windows when they control enough territory.
     */
    public void checkMarchettiShutters(World world) {
        if (turfMap == null || world == null) return;
        float march = turfMap.ownershipFraction(Faction.MARCHETTI_CREW);
        if (march <= MARCHETTI_SHUTTER_THRESHOLD) return;

        for (BuildingRecord rec : buildings.values()) {
            Faction owner = turfMap.getOwner(rec.getWorldX(), rec.getWorldZ());
            if (owner == Faction.MARCHETTI_CREW && !rec.isLuxuryFlat()) {
                placeMarchettShutters(world, rec.getWorldX(), rec.getWorldZ());
            }
        }
    }

    // ── Player interactions ───────────────────────────────────────────────────

    /**
     * Player squats a derelict building — prevents demolition.
     *
     * @param worldX  building location
     * @param worldZ  building location
     * @return result message, or null if preconditions not met
     */
    public String squatBuilding(int worldX, int worldZ) {
        BuildingRecord rec = findNearest(worldX, worldZ, 10);
        if (rec == null) return null;
        if (!rec.isDerelict()) {
            return "This building isn't derelict. Only abandoned buildings can be squatted.";
        }
        if (rec.isSquatted()) {
            return "Someone's already squatting here.";
        }
        rec.setSquatted(true);
        rec.setDemolitionTimer(-1f); // Cancel demolition
        return "You squat the building. The council can't touch it now. Probably.";
    }

    /**
     * Player tears down a condemned notice. Raises condition, lowers Council respect,
     * raises Street Lads respect.
     *
     * @param worldX    building location
     * @param worldZ    building location
     * @param allNpcs   living NPCs
     * @return result message, or null if no condemned building nearby
     */
    public String tearDownCondemnedNotice(int worldX, int worldZ, List<NPC> allNpcs) {
        BuildingRecord rec = findNearest(worldX, worldZ, 10);
        if (rec == null || !rec.isCondemned()) {
            return null;
        }

        rec.applyConditionDelta(CONDITION_NOTICE_TEARDOWN);
        rec.setCondemned(false);

        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.THE_COUNCIL, -10);
            factionSystem.applyRespectDelta(Faction.STREET_LADS, +10);
        }

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.COMMUNITY_HERO);
        }

        return "You tear down the condemned notice. The council won't like that.";
    }

    /**
     * Player tips off the council about a rival squat.
     *
     * @param worldX  squat location
     * @param worldZ  squat location
     * @param allNpcs living NPCs
     * @return result message, or null if no squat nearby
     */
    public String tipOffCouncilAboutSquat(int worldX, int worldZ, List<NPC> allNpcs) {
        BuildingRecord rec = findNearest(worldX, worldZ, 10);
        if (rec == null || !rec.isSquatted()) {
            return null;
        }
        if (rumourNetwork != null && allNpcs != null) {
            seedRumour(allNpcs, RumourType.COUNCIL_NOTICE,
                "There's a squat on the high street — somebody should tip off the council");
        }
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.THE_COUNCIL, +5);
            factionSystem.applyRespectDelta(Faction.STREET_LADS, -10);
        }
        return "You tip off the council. The squatters won't be pleased when the bailiffs arrive.";
    }

    /**
     * Player sells a building to developers.
     *
     * @param worldX    building location
     * @param worldZ    building location
     * @param inventory player inventory (receives coins)
     * @return result message, or null if no building nearby
     */
    public String sellToDevelopers(int worldX, int worldZ, Inventory inventory) {
        BuildingRecord rec = findNearest(worldX, worldZ, 10);
        if (rec == null) return null;

        if (inventory != null) {
            inventory.addItem(Material.COIN, SELL_TO_DEVELOPERS_COINS);
        }

        rec.setLuxuryFlat(true);
        luxuryFlatCount++;

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.PROPERTY_DEVELOPER);
        }

        return "Sold to developers for " + SELL_TO_DEVELOPERS_COINS + " coins. " +
               "You feel slightly guilty. Just slightly.";
    }

    /**
     * Player organises a community meeting.
     *
     * @param worldX            building location
     * @param worldZ            building location
     * @param allNpcs           all living NPCs (must have ≥5 nearby)
     * @param playerHasFlyer    whether the player has a FLYER item
     * @param influenceTier3    whether the player has INFLUENCE Tier 3 (no FLYER needed)
     * @return result message, or null if preconditions not met
     */
    public String organiseCommunityMeeting(int worldX, int worldZ, List<NPC> allNpcs,
                                            boolean playerHasFlyer, boolean influenceTier3) {
        BuildingRecord rec = findNearest(worldX, worldZ, 10);
        if (rec == null || !rec.isCondemned()) {
            return null;
        }

        if (!playerHasFlyer && !influenceTier3) {
            return "You need a FLYER to organise a community meeting.";
        }

        // Count nearby NPCs
        int nearbyNpcs = 0;
        if (allNpcs != null) {
            for (NPC npc : allNpcs) {
                if (!npc.isAlive()) continue;
                float dx = npc.getPosition().x - worldX;
                float dz = npc.getPosition().z - worldZ;
                if (dx * dx + dz * dz < 100f) { // within 10 blocks
                    nearbyNpcs++;
                }
            }
        }
        if (nearbyNpcs < COMMUNITY_MEETING_MIN_NPCS) {
            return "You need at least " + COMMUNITY_MEETING_MIN_NPCS +
                   " people nearby for a community meeting. " + nearbyNpcs + " are here.";
        }

        rec.applyConditionDelta(CONDITION_COMMUNITY_MEETING);
        rec.setCondemned(false);

        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.THE_COUNCIL, -15);
        }

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.LAST_OF_THE_LOCALS);
        }

        return "Community meeting organised! The building's condition has improved. " +
               "The council's fuming.";
    }

    // ── Block-break and graffiti hooks ────────────────────────────────────────

    /**
     * Called when the player breaks a block at (worldX, y, worldZ).
     * Applies −3 condition to the nearest building.
     */
    public void onBlockBroken(int worldX, int worldZ) {
        BuildingRecord rec = findNearest(worldX, worldZ, 8);
        if (rec != null && !rec.isLuxuryFlat()) {
            rec.applyConditionDelta(-DECAY_PER_BLOCK_BREAK);
        }
    }

    /**
     * Called when graffiti is placed at (worldX, worldZ).
     * Applies −2 condition to the nearest building and increments global graffiti count.
     */
    public void onGraffitiPlaced(int worldX, int worldZ) {
        BuildingRecord rec = findNearest(worldX, worldZ, 8);
        if (rec != null && !rec.isLuxuryFlat()) {
            rec.applyConditionDelta(-DECAY_PER_GRAFFITI);
            rec.incrementGraffitiTagCount();
            if (rec.getGraffitiTagCount() >= 4) {
                rec.setGraffitiCapped(true);
            }
        }
        graffitiCount++;
    }

    // ── PirateRadioSystem integration ─────────────────────────────────────────

    /**
     * Called when a pirate radio broadcast occurs from inside a derelict building.
     * Raises building condition +5 and Vibes +8.
     *
     * @param buildingX  building world X
     * @param buildingZ  building world Z
     */
    public void onPirateRadioBroadcast(int buildingX, int buildingZ) {
        BuildingRecord rec = findNearest(buildingX, buildingZ, 10);
        if (rec != null && rec.isDerelict()) {
            rec.applyConditionDelta(CONDITION_RADIO_BOOST);
            vibes = Math.min(100, vibes + VIBES_RADIO_BOOST);
        }
    }

    /**
     * Called when a rave is active or ends.
     *
     * @param active true if rave just started, false if ended
     */
    public void setRaveActive(boolean active) {
        if (active) {
            activeRaveCount++;
        } else {
            activeRaveCount = Math.max(0, activeRaveCount - 1);
        }
    }

    // ── Vibes calculation ─────────────────────────────────────────────────────

    /**
     * Recalculate Neighbourhood Vibes from all contributing factors.
     *
     * @param notoriety   player notoriety (0–100)
     * @param allNpcs     all living NPCs (unused in base calculation, reserved)
     */
    public void recalculateVibes(int notoriety, List<NPC> allNpcs) {
        // Average condition across all non-luxury buildings
        int totalCondition = 0;
        int buildingCount = 0;
        for (BuildingRecord rec : buildings.values()) {
            if (!rec.isLuxuryFlat()) {
                totalCondition += rec.getEffectiveCondition();
                buildingCount++;
            }
        }
        int avgCondition = buildingCount > 0 ? totalCondition / buildingCount : DEFAULT_CONDITION;

        // Faction scores (average of all three factions)
        int factionContrib = 0;
        if (factionSystem != null) {
            int total = 0;
            for (Faction f : Faction.values()) {
                total += factionSystem.getRespect(f);
            }
            factionContrib = total / Faction.values().length;
        } else {
            factionContrib = 50;
        }

        // Penalties
        int graffitiPenalty = Math.min(20, graffitiCount / 5); // max 20 penalty
        int luxuryPenalty   = Math.min(15, luxuryFlatCount * 5); // 5 per luxury flat, max 15
        int notorietyPenalty = notoriety / 5; // 0–20

        // Bonuses
        int raveBonusTotal = Math.min(16, activeRaveCount * VIBES_RAVE_BOOST);

        // Weighted formula: conditions 40%, faction 30%, bonuses 10%, penalties subtracted
        int rawVibes = (int) (avgCondition * 0.4f + factionContrib * 0.3f + raveBonusTotal * 0.1f)
                      - graffitiPenalty - luxuryPenalty - notorietyPenalty
                      + 10; // baseline bonus

        vibes = Math.max(0, Math.min(100, rawVibes));

        // Check for threshold crossings
        VibesState newState = getCurrentVibesState();
        if (newState != previousVibesState) {
            onVibesStateChange(previousVibesState, newState);
            previousVibesState = newState;
        }
    }

    private void onVibesStateChange(VibesState oldState, VibesState newState) {
        switch (newState) {
            case DYSTOPIA:
                if (!dystopiaAchievementUnlocked && achievementSystem != null) {
                    achievementSystem.unlock(AchievementType.DYSTOPIA_NOW);
                    dystopiaAchievementUnlocked = true;
                }
                pendingTooltip = "Zone of Deprivation. The fog thickens. The silence is deafening.";
                break;
            case HOSTILE:
                pendingTooltip = "The neighbourhood has turned hostile. NPCs flee on sight.";
                break;
            case THRIVING:
                pendingTooltip = "The neighbourhood is thriving! Bonus coins incoming.";
                break;
            default:
                break;
        }
    }

    private void applyDystopiaEffects(float delta, World world) {
        // In Dystopia state, random blocks crumble overnight (small random chance per frame)
        if (random.nextFloat() < 0.001f && world != null) {
            // Pick a random building and crumble a block
            List<BuildingRecord> list = new ArrayList<>(buildings.values());
            if (!list.isEmpty()) {
                BuildingRecord rec = list.get(random.nextInt(list.size()));
                if (!rec.isLuxuryFlat()) {
                    placeCrumbledBrick(world, rec.getWorldX(), rec.getWorldZ());
                }
            }
        }
    }

    // ── BootSaleSystem integration ────────────────────────────────────────────

    /**
     * Returns the auction price multiplier for the current Vibes state.
     * High-Vibes areas attract 15% better prices.
     */
    public float getBootSalePriceMultiplier() {
        return vibes >= VIBES_THRIVING ? BOOT_SALE_VIBES_PRICE_MULT : 1.0f;
    }

    /**
     * Returns extra buyers for the BootSale in high-Vibes areas.
     */
    public int getBootSaleExtraBuyers() {
        return vibes >= VIBES_THRIVING ? BOOT_SALE_EXTRA_BUYERS : 0;
    }

    // ── Vibes state accessors ─────────────────────────────────────────────────

    /** Returns the current Vibes score (0–100). */
    public int getVibes() { return vibes; }

    /** Directly set Vibes (used in tests). */
    public void setVibes(int v) {
        this.vibes = Math.max(0, Math.min(100, v));
        this.previousVibesState = getCurrentVibesState();
    }

    /** Returns the current VibesState based on the Vibes score. */
    public VibesState getCurrentVibesState() {
        if (vibes >= VIBES_THRIVING) return VibesState.THRIVING;
        if (vibes >= VIBES_NORMAL)   return VibesState.NORMAL;
        if (vibes >= VIBES_TENSE)    return VibesState.TENSE;
        if (vibes >= VIBES_HOSTILE)  return VibesState.HOSTILE;
        return VibesState.DYSTOPIA;
    }

    /** Whether police patrol rate should be increased (Tense or worse). */
    public boolean isPoliceRateIncreased() {
        return vibes < VIBES_NORMAL;
    }

    /** Fence price multiplier based on Vibes state. */
    public float getFencePriceMultiplier() {
        if (vibes < VIBES_HOSTILE) return HOSTILE_FENCE_DISCOUNT;
        return 1.0f;
    }

    /** Whether ambient sound should be silenced (Dystopia). */
    public boolean isAmbientSilenced() {
        return getCurrentVibesState() == VibesState.DYSTOPIA;
    }

    /** Whether Street Lads are currently hostile due to gentrification. */
    public boolean areStreetLadsHostile() {
        return streetLadsHostileTimer > 0f;
    }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    /** Returns and clears the pending tooltip message, or null. */
    public String pollTooltip() {
        String tip = pendingTooltip;
        pendingTooltip = null;
        return tip;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public int getLuxuryFlatCount()  { return luxuryFlatCount; }
    public int getGraffitiCount()    { return graffitiCount; }
    public int getActiveRaveCount()  { return activeRaveCount; }
    public float getGentrifyTimer()  { return gentrifyTimer; }

    // ── Private helpers ───────────────────────────────────────────────────────

    private BuildingRecord findNearest(int worldX, int worldZ, int maxDist) {
        BuildingRecord best = null;
        int bestDist = Integer.MAX_VALUE;
        for (BuildingRecord rec : buildings.values()) {
            int dx = rec.getWorldX() - worldX;
            int dz = rec.getWorldZ() - worldZ;
            int dist = Math.abs(dx) + Math.abs(dz);
            if (dist < bestDist && dist <= maxDist) {
                bestDist = dist;
                best = rec;
            }
        }
        return best;
    }

    private void seedRumour(List<NPC> allNpcs, RumourType type, String text) {
        Rumour rumour = new Rumour(type, text);
        // Seed to first available NPC
        for (NPC npc : allNpcs) {
            if (npc.isAlive()) {
                rumourNetwork.addRumour(npc, rumour);
                return;
            }
        }
    }

    private static String key(int x, int z) {
        return x + "," + z;
    }
}

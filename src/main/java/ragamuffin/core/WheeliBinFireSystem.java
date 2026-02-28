package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.PropPosition;
import ragamuffin.world.PropType;
import ragamuffin.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Issue #940: Wheelie Bin Fire System — Saturday Night Spectacle, Warmth &amp; Disorder.
 *
 * <p>Manages all active wheelie bin fires in the world. When a player or YOUTH_GANG NPC
 * ignites a {@link PropType#WHEELIE_BIN}, this system:
 * <ul>
 *   <li>Replaces the WHEELIE_BIN prop with a {@link PropType#BURNING_BIN}</li>
 *   <li>Emits persistent noise at level {@value #BURN_NOISE_LEVEL}</li>
 *   <li>Applies campfire-level warmth to nearby players each frame</li>
 *   <li>Attracts nearby PUBLIC / PENSIONER / YOUTH_GANG NPCs to gather and stare</li>
 *   <li>Increases {@link NeighbourhoodWatchSystem} anger (15 for player, 8 for gang)</li>
 *   <li>Spawns a {@link NPCType#FIRE_ENGINE} after {@value #FIRE_ENGINE_SPAWN_DELAY_SECONDS}s</li>
 *   <li>Halves remaining lifetime each in-game minute during rain</li>
 *   <li>Generates a newspaper headline after 60 seconds of burning</li>
 *   <li>Awards {@link AchievementType#PYROMANIAC} (3 fires in one night) and
 *       {@link AchievementType#FIRE_WARDEN} (extinguish before fire engine arrives)</li>
 * </ul>
 */
public class WheeliBinFireSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Noise level emitted persistently by a burning bin. */
    public static final float BURN_NOISE_LEVEL = 0.8f;

    /**
     * Minimum random lifetime (seconds) of a burning bin before it burns out.
     * Randomised per bin between {@value #MIN_BURN_LIFETIME} and
     * {@value #MAX_BURN_LIFETIME}.
     */
    public static final float MIN_BURN_LIFETIME = 90f;

    /** Maximum random lifetime (seconds) of a burning bin before it burns out. */
    public static final float MAX_BURN_LIFETIME = 180f;

    /** Seconds after ignition before a FIRE_ENGINE NPC is spawned. */
    public static final float FIRE_ENGINE_SPAWN_DELAY_SECONDS = 30f;

    /** Seconds after which a newspaper arson headline is generated. */
    public static final float HEADLINE_THRESHOLD_SECONDS = 60f;

    /** How many bin fires must be set in one night for PYROMANIAC achievement. */
    public static final int PYROMANIAC_FIRE_COUNT = 3;

    /** Radius within which NPCs are attracted toward a burning bin. */
    public static final float NPC_ATTRACTION_RADIUS = 20f;

    /** Radius within which NPCs gather around the fire. */
    public static final float NPC_GATHER_RADIUS = 5f;

    /** Maximum number of NPCs that gather around a single fire. */
    public static final int MAX_GATHERED_NPCS = 6;

    /** Watch anger added when the player ignites a bin. */
    public static final int WATCH_ANGER_PLAYER_IGNITION = 15;

    /** Watch anger added when a YOUTH_GANG NPC ignites a bin. */
    public static final int WATCH_ANGER_GANG_IGNITION = 8;

    /** Watch anger change when fire is extinguished by the fire engine (reduction). */
    public static final int WATCH_ANGER_FIRE_ENGINE = -5;

    /** Watch anger change when player extinguishes with fire extinguisher (reduction). */
    public static final int WATCH_ANGER_PLAYER_EXTINGUISH = -10;

    /** Street reputation gained when the player ignites a bin. */
    public static final int STREET_REP_IGNITION = 3;

    /** Faction respect delta for Street Lads when they (or the player) light a fire. */
    public static final int STREET_LADS_RESPECT_FIRE = 3;

    /** Faction respect delta for The Council when a fire is lit. */
    public static final int THE_COUNCIL_RESPECT_FIRE = -2;

    /** In-game minutes between rain lifetime halving. */
    public static final float RAIN_HALVING_INTERVAL_MINUTES = 1.0f;

    // ── Inner class: active fire record ───────────────────────────────────────

    /**
     * Tracks a single active bin fire.
     */
    public static class BinFire {

        /** World position of the fire (centre of the bin). */
        public final Vector3 position;

        /** Whether the fire was started by the player (vs YOUTH_GANG). */
        public final boolean playerIgnited;

        /** Remaining lifetime in real seconds before the fire burns out. */
        public float remainingLifetime;

        /** Total lifetime assigned on ignition (for rain-halving calculations). */
        public final float totalLifetime;

        /** Elapsed burn time in seconds (used for headline and spawn thresholds). */
        public float elapsedSeconds;

        /** Whether the FIRE_ENGINE has already been spawned for this fire. */
        public boolean fireEngineSpawned;

        /** Whether the newspaper headline has been generated for this fire. */
        public boolean headlineGenerated;

        /** Whether this fire was extinguished by the player (for FIRE_WARDEN achievement). */
        public boolean extinguishedByPlayer;

        /** Timer for rain-lifetime halving (counts in-game minutes). */
        public float rainMinuteTimer;

        /** Index of the BURNING_BIN prop in the world's prop list. */
        public int propIndex;

        BinFire(Vector3 position, boolean playerIgnited, float lifetime, int propIndex) {
            this.position = new Vector3(position);
            this.playerIgnited = playerIgnited;
            this.remainingLifetime = lifetime;
            this.totalLifetime = lifetime;
            this.elapsedSeconds = 0f;
            this.fireEngineSpawned = false;
            this.headlineGenerated = false;
            this.extinguishedByPlayer = false;
            this.rainMinuteTimer = 0f;
            this.propIndex = propIndex;
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;
    private final List<BinFire> activeFires = new ArrayList<>();

    /** Fires started in the current in-game night (reset at dawn). */
    private int nightFireCount = 0;

    /** Whether PYROMANIAC has been awarded this session. */
    private boolean pyromaniacAwarded = false;

    /** Whether FIRE_WARDEN has been awarded this session. */
    private boolean fireWardanAwarded = false;

    /** Whether the first-ignition tooltip has been shown this session. */
    private boolean firstIgnitionTooltipShown = false;

    // ── Construction ──────────────────────────────────────────────────────────

    public WheeliBinFireSystem() {
        this(new Random());
    }

    public WheeliBinFireSystem(Random random) {
        this.random = random;
    }

    // ── Ignition ──────────────────────────────────────────────────────────────

    /**
     * Attempt to ignite a wheelie bin at the given world position.
     *
     * <p>Fails (returns false) if:
     * <ul>
     *   <li>The weather is RAIN, DRIZZLE, or THUNDERSTORM</li>
     *   <li>A fire is already active at that position</li>
     *   <li>No WHEELIE_BIN prop exists at that position in the world</li>
     * </ul>
     *
     * <p>On success:
     * <ul>
     *   <li>Replaces the WHEELIE_BIN prop with a BURNING_BIN prop</li>
     *   <li>Consumes one PETROL_CAN from player inventory (if player-ignited)</li>
     *   <li>Adds +{@value #STREET_REP_IGNITION} street reputation (player only)</li>
     *   <li>Adds watch anger ({@value #WATCH_ANGER_PLAYER_IGNITION} or
     *       {@value #WATCH_ANGER_GANG_IGNITION})</li>
     *   <li>Emits noise at level {@value #BURN_NOISE_LEVEL}</li>
     *   <li>Shows first-ignition tooltip if not yet shown</li>
     * </ul>
     *
     * @param binPosition          world position of the WHEELIE_BIN prop
     * @param playerIgnited        true if the player started the fire, false for YOUTH_GANG
     * @param weather              current weather
     * @param world                the game world (for prop replacement)
     * @param player               the player (for inventory and reputation; may be null if gang)
     * @param inventory            player's inventory (may be null if gang)
     * @param noiseSystem          for emitting noise
     * @param watchSystem          for anger changes
     * @param factionSystem        for faction respect changes (may be null)
     * @param tooltipSystem        for showing tooltips (may be null)
     * @param currentHour          current in-game hour (for night fire count)
     * @param achievementCallback  for awarding achievements (may be null)
     * @return true if the bin was successfully ignited
     */
    public boolean ignite(Vector3 binPosition,
                          boolean playerIgnited,
                          Weather weather,
                          World world,
                          Player player,
                          Inventory inventory,
                          NoiseSystem noiseSystem,
                          NeighbourhoodWatchSystem watchSystem,
                          FactionSystem factionSystem,
                          TooltipSystem tooltipSystem,
                          float currentHour,
                          NotorietySystem.AchievementCallback achievementCallback) {

        // Cannot ignite in wet weather
        if (weather.isRaining()) {
            if (tooltipSystem != null) {
                tooltipSystem.showMessage("It's too wet to catch.", 3.0f);
            }
            return false;
        }

        // Cannot ignite a bin that is already burning
        if (isAlreadyBurning(binPosition)) {
            if (tooltipSystem != null) {
                tooltipSystem.showMessage("It's too wet to catch.", 3.0f);
            }
            return false;
        }

        // Find the WHEELIE_BIN prop at this position
        int propIdx = findPropIndex(world, binPosition, PropType.WHEELIE_BIN);
        if (propIdx < 0) {
            return false;
        }

        // Consume PETROL_CAN from player inventory (player ignition only)
        if (playerIgnited && inventory != null) {
            if (!inventory.hasItem(Material.PETROL_CAN)) {
                return false;
            }
            inventory.removeItem(Material.PETROL_CAN, 1);
        }

        // Replace WHEELIE_BIN prop with BURNING_BIN
        PropPosition oldProp = world.getPropPositions().get(propIdx);
        PropPosition burningProp = new PropPosition(
                oldProp.getWorldX(), oldProp.getWorldY(), oldProp.getWorldZ(),
                PropType.BURNING_BIN, oldProp.getRotationY());
        world.removeProp(propIdx);
        world.addPropPosition(burningProp);
        int newPropIdx = world.getPropPositions().size() - 1;

        // Record the fire
        float lifetime = MIN_BURN_LIFETIME
                + random.nextFloat() * (MAX_BURN_LIFETIME - MIN_BURN_LIFETIME);
        BinFire fire = new BinFire(binPosition, playerIgnited, lifetime, newPropIdx);
        activeFires.add(fire);

        // Emit noise
        noiseSystem.emitNoise(binPosition, BURN_NOISE_LEVEL);

        // Player reputation
        if (playerIgnited && player != null) {
            player.getStreetReputation().addPoints(STREET_REP_IGNITION);
        }

        // Watch anger
        if (watchSystem != null) {
            watchSystem.addAnger(playerIgnited ? WATCH_ANGER_PLAYER_IGNITION : WATCH_ANGER_GANG_IGNITION);
        }

        // Faction respect
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.STREET_LADS, STREET_LADS_RESPECT_FIRE);
            factionSystem.applyRespectDelta(Faction.THE_COUNCIL, THE_COUNCIL_RESPECT_FIRE);
        }

        // Night fire count for PYROMANIAC achievement
        boolean isNight = (currentHour >= 22.0f || currentHour < 6.0f);
        if (isNight) {
            nightFireCount++;
            if (!pyromaniacAwarded && nightFireCount >= PYROMANIAC_FIRE_COUNT
                    && achievementCallback != null) {
                pyromaniacAwarded = true;
                achievementCallback.award(AchievementType.PYROMANIAC);
            }
        }

        // First-ignition tooltip
        if (tooltipSystem != null && !firstIgnitionTooltipShown) {
            firstIgnitionTooltipShown = true;
            tooltipSystem.showMessage("The bins are on fire. As is tradition.", 4.0f);
        }

        return true;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update all active fires each frame.
     *
     * <p>For each active fire:
     * <ul>
     *   <li>Apply warmth to the player if within campfire radius</li>
     *   <li>Attract nearby NPCs to gather and stare</li>
     *   <li>Halve lifetime each in-game minute if it's raining</li>
     *   <li>Spawn FIRE_ENGINE after {@value #FIRE_ENGINE_SPAWN_DELAY_SECONDS}s</li>
     *   <li>Generate newspaper headline after {@value #HEADLINE_THRESHOLD_SECONDS}s</li>
     *   <li>Extinguish when lifetime expires (replace with BURNT_BIN)</li>
     * </ul>
     *
     * @param delta               seconds since last frame
     * @param weather             current weather
     * @param world               the game world
     * @param player              the player
     * @param warmthSystem        for applying warmth
     * @param noiseSystem         for maintaining noise sources
     * @param npcManager          for spawning fire engine and managing NPC states
     * @param watchSystem         for anger changes on fire engine arrival
     * @param newspaperSystem     for generating arson headlines
     * @param achievementCallback for awarding achievements (may be null)
     * @param currentHour         current in-game hour (for dawn reset)
     */
    public void update(float delta,
                       Weather weather,
                       World world,
                       Player player,
                       WarmthSystem warmthSystem,
                       NoiseSystem noiseSystem,
                       NPCManager npcManager,
                       NeighbourhoodWatchSystem watchSystem,
                       NewspaperSystem newspaperSystem,
                       NotorietySystem.AchievementCallback achievementCallback,
                       float currentHour) {

        // Reset night fire count at dawn
        if (currentHour >= 6.0f && currentHour < 6.1f) {
            nightFireCount = 0;
        }

        Iterator<BinFire> iter = activeFires.iterator();
        while (iter.hasNext()) {
            BinFire fire = iter.next();

            // Advance elapsed time
            fire.elapsedSeconds += delta;

            // Rain: halve lifetime each in-game minute
            if (weather.isRaining()) {
                fire.rainMinuteTimer += delta;
                float rainIntervalSeconds = RAIN_HALVING_INTERVAL_MINUTES * 60f;
                while (fire.rainMinuteTimer >= rainIntervalSeconds) {
                    fire.rainMinuteTimer -= rainIntervalSeconds;
                    fire.remainingLifetime /= 2f;
                }
            }

            // Drain lifetime
            fire.remainingLifetime -= delta;

            // Apply warmth to nearby player
            if (warmthSystem != null && player != null) {
                warmthSystem.applyExternalWarmthSource(player, fire.position, player.getPosition(), delta);
            }

            // Attract nearby NPCs
            if (npcManager != null) {
                attractNPCs(fire, npcManager.getNPCs());
            }

            // Spawn fire engine after 30 seconds
            if (!fire.fireEngineSpawned && fire.elapsedSeconds >= FIRE_ENGINE_SPAWN_DELAY_SECONDS) {
                fire.fireEngineSpawned = true;
                if (npcManager != null) {
                    spawnFireEngine(fire, npcManager, world);
                }
            }

            // Generate newspaper headline after 60 seconds
            if (!fire.headlineGenerated && fire.elapsedSeconds >= HEADLINE_THRESHOLD_SECONDS) {
                fire.headlineGenerated = true;
                if (newspaperSystem != null) {
                    newspaperSystem.generateArsonHeadline(random);
                }
            }

            // Burn out when lifetime exhausted
            if (fire.remainingLifetime <= 0f) {
                extinguish(fire, world, noiseSystem, watchSystem, false);
                iter.remove();
            }
        }

        // Check if any fire engine has arrived at a fire
        if (npcManager != null) {
            checkFireEngineArrivals(world, noiseSystem, watchSystem, npcManager);
        }
    }

    // ── Extinguishing ──────────────────────────────────────────────────────────

    /**
     * Attempt to extinguish a burning bin at the given position using a fire extinguisher.
     * Consumes one FIRE_EXTINGUISHER from the player's inventory.
     *
     * @param binPosition         world position of the burning bin
     * @param player              the player
     * @param inventory           the player's inventory
     * @param world               the game world
     * @param noiseSystem         for removing the noise source
     * @param watchSystem         for anger changes
     * @param tooltipSystem       for showing the extinguish tooltip
     * @param achievementCallback for awarding FIRE_WARDEN achievement
     * @return true if successfully extinguished
     */
    public boolean extinguishWithFireExtinguisher(Vector3 binPosition,
                                                   Player player,
                                                   Inventory inventory,
                                                   World world,
                                                   NoiseSystem noiseSystem,
                                                   NeighbourhoodWatchSystem watchSystem,
                                                   TooltipSystem tooltipSystem,
                                                   NotorietySystem.AchievementCallback achievementCallback) {
        if (inventory == null || !inventory.hasItem(Material.FIRE_EXTINGUISHER)) {
            return false;
        }

        BinFire fire = findFireAt(binPosition);
        if (fire == null) {
            return false;
        }

        inventory.removeItem(Material.FIRE_EXTINGUISHER, 1);
        fire.extinguishedByPlayer = true;

        // Watch anger reduction
        if (watchSystem != null) {
            watchSystem.addAnger(WATCH_ANGER_PLAYER_EXTINGUISH);
        }

        // FIRE_WARDEN achievement — only if fire engine hasn't arrived yet
        if (!fire.fireEngineSpawned && !fireWardanAwarded && achievementCallback != null) {
            fireWardanAwarded = true;
            achievementCallback.award(AchievementType.FIRE_WARDEN);
        }

        // Show tooltip
        if (tooltipSystem != null) {
            tooltipSystem.showMessage("Hero. Or grass. Depending on who's watching.", 4.0f);
        }

        // Extinguish
        extinguish(fire, world, noiseSystem, watchSystem, true);
        activeFires.remove(fire);

        return true;
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    /**
     * Extinguish a fire: replace the BURNING_BIN prop with BURNT_BIN and remove noise.
     *
     * @param fire        the fire to extinguish
     * @param world       the game world
     * @param noiseSystem for removing the noise source
     * @param watchSystem for anger changes on fire engine arrival
     * @param byPlayer    true if extinguished by the player (not natural burnout)
     */
    private void extinguish(BinFire fire, World world, NoiseSystem noiseSystem,
                             NeighbourhoodWatchSystem watchSystem, boolean byPlayer) {
        // Replace BURNING_BIN with BURNT_BIN
        int burningIdx = findPropIndex(world, fire.position, PropType.BURNING_BIN);
        if (burningIdx >= 0) {
            PropPosition oldProp = world.getPropPositions().get(burningIdx);
            PropPosition burntProp = new PropPosition(
                    oldProp.getWorldX(), oldProp.getWorldY(), oldProp.getWorldZ(),
                    PropType.BURNT_BIN, oldProp.getRotationY());
            world.removeProp(burningIdx);
            world.addPropPosition(burntProp);
        }

        // Remove persistent noise source
        noiseSystem.removeNoiseAt(fire.position);
    }

    /**
     * Find the index of a prop of the given type near the given position (within 1 block).
     *
     * @return the prop index, or -1 if not found
     */
    private int findPropIndex(World world, Vector3 position, PropType type) {
        List<PropPosition> props = world.getPropPositions();
        for (int i = 0; i < props.size(); i++) {
            PropPosition p = props.get(i);
            if (p.getType() == type) {
                float dx = p.getWorldX() - position.x;
                float dy = p.getWorldY() - position.y;
                float dz = p.getWorldZ() - position.z;
                float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist < 1.5f) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Check if a fire is already active at the given position (within 1.5 blocks).
     */
    private boolean isAlreadyBurning(Vector3 position) {
        for (BinFire f : activeFires) {
            if (f.position.dst(position) < 1.5f) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find the active fire at the given position, or null.
     */
    private BinFire findFireAt(Vector3 position) {
        for (BinFire f : activeFires) {
            if (f.position.dst(position) < 1.5f) {
                return f;
            }
        }
        return null;
    }

    /**
     * Attract nearby PUBLIC, PENSIONER, and YOUTH_GANG NPCs toward the fire.
     * Up to {@value #MAX_GATHERED_NPCS} NPCs gather. Gathered NPCs enter STARING state.
     */
    private void attractNPCs(BinFire fire, List<NPC> npcs) {
        int gatheredCount = 0;

        // Count already gathered
        for (NPC npc : npcs) {
            if (npc.getPosition().dst(fire.position) < NPC_GATHER_RADIUS
                    && npc.getState() == NPCState.STARING) {
                gatheredCount++;
            }
        }

        for (NPC npc : npcs) {
            if (gatheredCount >= MAX_GATHERED_NPCS) break;

            NPCType type = npc.getType();
            if (type != NPCType.PUBLIC && type != NPCType.PENSIONER
                    && type != NPCType.YOUTH_GANG) {
                continue;
            }

            float dist = npc.getPosition().dst(fire.position);
            if (dist > NPC_ATTRACTION_RADIUS) continue;

            // If NPC is not yet gathered or staring, send them toward the fire
            if (dist > NPC_GATHER_RADIUS) {
                npc.setTargetPosition(new Vector3(fire.position));
                if (npc.getState() == NPCState.IDLE || npc.getState() == NPCState.WANDERING) {
                    npc.setState(NPCState.PATROLLING);
                }
            } else {
                // Close enough — have them stare at the fire
                npc.setState(NPCState.STARING);
                gatheredCount++;
            }
        }
    }

    /**
     * Spawn a FIRE_ENGINE NPC heading toward the fire.
     */
    private void spawnFireEngine(BinFire fire, NPCManager npcManager, World world) {
        // Spawn at a position offset from the fire (simulating arriving from outside)
        float spawnX = fire.position.x + 60f;
        float spawnY = 1f;
        float spawnZ = fire.position.z;
        NPC fireEngine = npcManager.spawnNPC(NPCType.FIRE_ENGINE, spawnX, spawnY, spawnZ);
        if (fireEngine != null) {
            fireEngine.setTargetPosition(new Vector3(fire.position));
            fireEngine.setState(NPCState.PATROLLING);
        }
    }

    /**
     * Check if any FIRE_ENGINE NPC has arrived within 2 blocks of a fire and
     * extinguish the fire if so.
     */
    private void checkFireEngineArrivals(World world, NoiseSystem noiseSystem,
                                          NeighbourhoodWatchSystem watchSystem,
                                          NPCManager npcManager) {
        List<NPC> npcs = npcManager.getNPCs();
        Iterator<BinFire> iter = activeFires.iterator();
        while (iter.hasNext()) {
            BinFire fire = iter.next();
            for (NPC npc : npcs) {
                if (npc.getType() == NPCType.FIRE_ENGINE
                        && npc.getPosition().dst(fire.position) <= 2.0f) {
                    // Fire engine has arrived — extinguish
                    if (watchSystem != null) {
                        watchSystem.addAnger(WATCH_ANGER_FIRE_ENGINE);
                    }
                    extinguish(fire, world, noiseSystem, watchSystem, false);
                    iter.remove();
                    break;
                }
            }
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /**
     * Get all currently active bin fires.
     */
    public List<BinFire> getActiveFires() {
        return activeFires;
    }

    /**
     * Returns true if there is an active fire at the given position (within 1.5 blocks).
     */
    public boolean isFireAt(float x, float y, float z) {
        Vector3 pos = new Vector3(x, y, z);
        return isAlreadyBurning(pos);
    }

    /**
     * Get the number of fires started in the current in-game night.
     */
    public int getNightFireCount() {
        return nightFireCount;
    }

    /**
     * Reset the night fire count (for testing or at dawn).
     */
    public void resetNightFireCount() {
        nightFireCount = 0;
    }

    /**
     * Whether the first-ignition tooltip has been shown.
     */
    public boolean isFirstIgnitionTooltipShown() {
        return firstIgnitionTooltipShown;
    }
}

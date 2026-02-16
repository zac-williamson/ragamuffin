package ragamuffin.ai;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.building.StructureTracker;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.ui.TooltipTrigger;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import java.util.*;

/**
 * Manages all NPCs in the game - spawning, updating, AI behavior.
 */
public class NPCManager {

    private final List<NPC> npcs;
    private final Pathfinder pathfinder;
    private final Random random;
    private float gameTime; // Game time in hours (0-24)

    // Park boundaries (assumed centered at 0,0)
    private static final float PARK_MIN_X = -20;
    private static final float PARK_MAX_X = 20;
    private static final float PARK_MIN_Z = -20;
    private static final float PARK_MAX_Z = 20;

    // Structure detection
    private Map<Vector3, Integer> playerStructures; // Position -> block count

    // Police system
    private boolean policeSpawned; // Track if police are currently spawned
    private Map<NPC, Float> policeWarningTimers; // Track warning duration for each police
    private Map<NPC, Vector3> policeTargetStructures; // Track which structure each police is investigating

    // Council builder system (Phase 7)
    private StructureTracker structureTracker;
    private Map<StructureTracker.Structure, Integer> structureBuilderCount; // Track builders per structure
    private Map<NPC, StructureTracker.Structure> builderTargets; // Track which structure each builder is targeting
    private Map<NPC, Float> builderKnockbackTimers; // Track knockback delay per builder
    private Map<NPC, Float> builderDemolishTimers; // Track demolition cooldown per builder
    private float structureScanTimer; // Periodic structure scanning
    private Set<Vector3> notifiedStructures; // Track which structure positions already have notices

    public NPCManager() {
        this.npcs = new ArrayList<>();
        this.pathfinder = new Pathfinder();
        this.random = new Random();
        this.gameTime = 8.0f; // Start at 8:00 AM
        this.playerStructures = new HashMap<>();
        this.policeSpawned = false;
        this.policeWarningTimers = new HashMap<>();
        this.policeTargetStructures = new HashMap<>();

        // Phase 7: Council builder system
        this.structureTracker = new StructureTracker();
        this.structureBuilderCount = new HashMap<>();
        this.builderTargets = new HashMap<>();
        this.builderKnockbackTimers = new HashMap<>();
        this.builderDemolishTimers = new HashMap<>();
        this.structureScanTimer = 0;
        this.notifiedStructures = new HashSet<>();
    }

    /**
     * Spawn an NPC at a specific position.
     */
    public NPC spawnNPC(NPCType type, float x, float y, float z) {
        NPC npc = new NPC(type, x, y, z);

        // Set initial state based on type
        switch (type) {
            case DOG:
                npc.setState(NPCState.WANDERING);
                break;
            case PUBLIC:
            case COUNCIL_MEMBER:
                npc.setState(NPCState.IDLE);
                updateDailyRoutine(npc); // Set based on time
                break;
            case YOUTH_GANG:
                npc.setState(NPCState.WANDERING);
                break;
            default:
                npc.setState(NPCState.IDLE);
        }

        npcs.add(npc);
        return npc;
    }

    /**
     * Get all NPCs.
     */
    public List<NPC> getNPCs() {
        return npcs;
    }

    /**
     * Remove an NPC.
     */
    public void removeNPC(NPC npc) {
        npcs.remove(npc);
    }

    /**
     * Set the current game time in hours (0-24).
     */
    public void setGameTime(float hours) {
        this.gameTime = hours % 24;

        // Update daily routines for all NPCs
        for (NPC npc : npcs) {
            if (npc.getType() == NPCType.PUBLIC || npc.getType() == NPCType.COUNCIL_MEMBER) {
                updateDailyRoutine(npc);
            }
        }
    }

    public float getGameTime() {
        return gameTime;
    }

    /**
     * Update daily routine based on time.
     */
    private void updateDailyRoutine(NPC npc) {
        if (gameTime >= 8 && gameTime < 17) {
            // Work hours (8:00 - 17:00)
            if (npc.getState() != NPCState.GOING_TO_WORK && npc.getState() != NPCState.STARING
                && npc.getState() != NPCState.PHOTOGRAPHING && npc.getState() != NPCState.COMPLAINING) {
                npc.setState(NPCState.GOING_TO_WORK);
            }
        } else if (gameTime >= 17 && gameTime < 20) {
            // Evening (17:00 - 20:00)
            if (npc.getState() != NPCState.GOING_HOME && npc.getState() != NPCState.STARING
                && npc.getState() != NPCState.PHOTOGRAPHING && npc.getState() != NPCState.COMPLAINING) {
                npc.setState(NPCState.GOING_HOME);
            }
        } else {
            // Night (20:00+) or early morning
            if (npc.getState() != NPCState.AT_PUB && npc.getState() != NPCState.AT_HOME
                && npc.getState() != NPCState.STARING && npc.getState() != NPCState.PHOTOGRAPHING
                && npc.getState() != NPCState.COMPLAINING) {
                // Randomly choose pub or home
                npc.setState(random.nextBoolean() ? NPCState.AT_PUB : NPCState.AT_HOME);
            }
        }
    }

    /**
     * Update all NPCs.
     */
    public void update(float delta, World world, Player player, Inventory inventory, TooltipSystem tooltipSystem) {
        // Update structure tracking (Phase 7)
        structureScanTimer += delta;
        if (structureScanTimer >= 2.0f) { // Scan every 2 seconds
            structureTracker.scanForStructures(world);
            updateCouncilBuilders(world, tooltipSystem);
            structureScanTimer = 0;
        }

        // Use indexed loop to avoid ConcurrentModificationException when spawning new NPCs
        for (int i = 0; i < npcs.size(); i++) {
            NPC npc = npcs.get(i);
            updateNPC(npc, delta, world, player, inventory, tooltipSystem);
        }
    }

    /**
     * Update a single NPC's behavior.
     */
    private void updateNPC(NPC npc, float delta, World world, Player player, Inventory inventory, TooltipSystem tooltipSystem) {
        npc.update(delta);

        // Phase 11: PUBLIC NPCs randomly speak when near player
        if (npc.getType() == NPCType.PUBLIC && !npc.isSpeaking() && npc.isNear(player.getPosition(), 10.0f)) {
            // Random chance to speak (about 1% per frame = ~every 1-2 seconds near player)
            if (random.nextFloat() < 0.01f) {
                String[] publicSpeech = {
                    "Is that... legal?",
                    "My council tax pays for this?",
                    "I'm calling the council.",
                    "Bit rough, innit?",
                    "You alright, love?"
                };
                npc.setSpeechText(publicSpeech[random.nextInt(publicSpeech.length)], 3.0f);
            }
        }

        // Handle police separately
        if (npc.getType() == NPCType.POLICE) {
            updatePolice(npc, delta, world, player, tooltipSystem);
        } else if (npc.getType() == NPCType.COUNCIL_BUILDER) {
            updateCouncilBuilder(npc, delta, world, tooltipSystem);
        } else {
            switch (npc.getState()) {
                case WANDERING:
                    updateWandering(npc, delta, world);
                    break;
                case GOING_TO_WORK:
                case GOING_HOME:
                case AT_PUB:
                case AT_HOME:
                    updateDailyRoutine(npc, delta, world);
                    break;
                case STARING:
                case PHOTOGRAPHING:
                case COMPLAINING:
                    updateReactingToStructure(npc, delta, world);
                    break;
                case STEALING:
                    updateStealing(npc, delta, player, inventory, tooltipSystem);
                    break;
            }
        }

        // Check for player structures nearby
        checkForPlayerStructures(npc, world);

        // Youth gangs try to steal
        if (npc.getType() == NPCType.YOUTH_GANG && npc.getState() != NPCState.STEALING) {
            if (npc.isNear(player.getPosition(), 2.0f)) {
                npc.setState(NPCState.STEALING);
            } else if (npc.isNear(player.getPosition(), 20.0f)) {
                // Move toward player
                setNPCTarget(npc, player.getPosition(), world);
            }
        }

        // Dogs stay in park
        if (npc.getType() == NPCType.DOG) {
            enforceParKBoundaries(npc);
        }

        // Follow path if one exists
        if (npc.getPath() != null && !npc.getPath().isEmpty()) {
            followPath(npc, delta);
        } else if (npc.getTargetPosition() != null) {
            // No path, but has target - move directly toward it
            moveTowardTarget(npc, npc.getTargetPosition(), delta);
        } else {
            // No target or path - stop moving
            npc.setVelocity(0, 0, 0);
        }
    }

    /**
     * Update wandering behavior.
     */
    private void updateWandering(NPC npc, float delta, World world) {
        // If no target, no path, or reached target, pick a new random target
        // Dogs wander more aggressively with a larger radius and minimum distance
        float wanderRadius = (npc.getType() == NPCType.DOG) ? 15.0f : 10.0f;
        float minWanderDistance = (npc.getType() == NPCType.DOG) ? 5.0f : 0.0f;

        boolean needsNewTarget = npc.getTargetPosition() == null ||
                                 npc.getPath() == null ||
                                 npc.getPath().isEmpty() ||
                                 npc.isNear(npc.getTargetPosition(), 1.0f);

        if (needsNewTarget) {
            Vector3 randomTarget;
            int attempts = 0;
            do {
                randomTarget = getRandomWalkablePosition(npc.getPosition(), world, wanderRadius);
                attempts++;
            } while (minWanderDistance > 0 && randomTarget.dst(npc.getPosition()) < minWanderDistance && attempts < 10);
            setNPCTarget(npc, randomTarget, world);
        }
    }

    /**
     * Update daily routine movement.
     */
    private void updateDailyRoutine(NPC npc, float delta, World world) {
        // For now, NPCs just wander during their routine
        // In a full implementation, they'd path to specific locations (work, home, pub)
        updateWandering(npc, delta, world);
    }

    /**
     * Update NPC reacting to a structure.
     */
    private void updateReactingToStructure(NPC npc, float delta, World world) {
        // Find nearest player structure
        Vector3 nearestStructure = findNearestPlayerStructure(npc.getPosition());

        if (nearestStructure != null && !npc.isNear(nearestStructure, 3.0f)) {
            // Move toward structure
            setNPCTarget(npc, nearestStructure, world);
        } else {
            // At structure, just idle
            npc.setVelocity(0, 0, 0);
        }
    }

    /**
     * Update youth gang stealing.
     */
    private void updateStealing(NPC npc, float delta, Player player, Inventory inventory, TooltipSystem tooltipSystem) {
        if (npc.isNear(player.getPosition(), 1.5f)) {
            // Adjacent to player - steal!
            attemptTheft(inventory, tooltipSystem);
            npc.setState(NPCState.WANDERING); // Go back to wandering after theft
        }
    }

    /**
     * Attempt to steal from player inventory.
     */
    private void attemptTheft(Inventory inventory, TooltipSystem tooltipSystem) {
        // Try to steal wood first
        if (inventory.getItemCount(Material.WOOD) > 0) {
            inventory.removeItem(Material.WOOD, 1);
            if (tooltipSystem != null) {
                tooltipSystem.trigger(TooltipTrigger.YOUTH_THEFT);
            }
        }
    }

    /**
     * Check for player-built structures nearby.
     */
    private void checkForPlayerStructures(NPC npc, World world) {
        if (npc.getType() != NPCType.PUBLIC && npc.getType() != NPCType.COUNCIL_MEMBER) {
            return;
        }

        // Scan for placed blocks nearby (simplified - in real implementation would track placed blocks)
        int scanRadius = 20;
        int playerBlockCount = 0;
        Vector3 structureCenter = null;

        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                int x = (int) (npc.getPosition().x + dx);
                int z = (int) (npc.getPosition().z + dz);

                for (int y = 0; y < 10; y++) {
                    BlockType block = world.getBlock(x, y, z);
                    // Check for placed blocks (planks, etc.)
                    if (block == BlockType.WOOD || block == BlockType.BRICK) {
                        playerBlockCount++;
                        if (structureCenter == null) {
                            structureCenter = new Vector3(x, y, z);
                        }
                    }
                }
            }
        }

        // React if substantial structure detected
        if (playerBlockCount >= 10 && structureCenter != null) {
            if (npc.getState() != NPCState.STARING && npc.getState() != NPCState.PHOTOGRAPHING
                && npc.getState() != NPCState.COMPLAINING) {
                // Randomly choose reaction
                int reaction = random.nextInt(3);
                switch (reaction) {
                    case 0: npc.setState(NPCState.STARING); break;
                    case 1: npc.setState(NPCState.PHOTOGRAPHING); break;
                    case 2: npc.setState(NPCState.COMPLAINING); break;
                }

                // Track this structure
                playerStructures.put(structureCenter, playerBlockCount);
            }
        }
    }

    /**
     * Find nearest player structure.
     */
    private Vector3 findNearestPlayerStructure(Vector3 position) {
        Vector3 nearest = null;
        float nearestDist = Float.MAX_VALUE;

        for (Vector3 structurePos : playerStructures.keySet()) {
            float dist = position.dst(structurePos);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = structurePos;
            }
        }

        return nearest;
    }

    /**
     * Set NPC target and find path.
     */
    private void setNPCTarget(NPC npc, Vector3 target, World world) {
        npc.setTargetPosition(target);

        // Find path
        List<Vector3> path = pathfinder.findPath(world, npc.getPosition(), target);

        // If pathfinding fails and this is a wandering NPC (dog/youth), use direct movement
        // by setting an empty path - the NPC will use moveTowardTarget instead
        if (path == null && (npc.getType() == NPCType.DOG || npc.getType() == NPCType.YOUTH_GANG)) {
            npc.setPath(new ArrayList<>()); // Empty path triggers direct movement
        } else {
            npc.setPath(path);
        }
    }

    /**
     * Move NPC directly toward a target position.
     */
    private void moveTowardTarget(NPC npc, Vector3 target, float delta) {
        if (npc.isNear(target, 0.5f)) {
            npc.setVelocity(0, 0, 0);
            return;
        }

        Vector3 direction = target.cpy().sub(npc.getPosition()).nor();
        float speed = (npc.getType() == NPCType.DOG) ? NPC.DOG_SPEED : NPC.MOVE_SPEED;
        npc.setVelocity(direction.x * speed, 0, direction.z * speed);
    }

    /**
     * Make NPC follow their current path.
     */
    private void followPath(NPC npc, float delta) {
        List<Vector3> path = npc.getPath();
        if (path == null || path.isEmpty()) {
            return;
        }

        int currentIndex = npc.getCurrentPathIndex();
        if (currentIndex >= path.size()) {
            npc.setPath(null); // Path complete
            npc.setVelocity(0, 0, 0);
            return;
        }

        Vector3 waypoint = path.get(currentIndex);

        // Check if reached current waypoint
        if (npc.isNear(waypoint, 0.5f)) {
            npc.advancePathIndex();
            if (npc.getCurrentPathIndex() >= path.size()) {
                npc.setPath(null);
                npc.setVelocity(0, 0, 0);
                return;
            }
            waypoint = path.get(npc.getCurrentPathIndex());
        }

        // Move toward waypoint
        Vector3 direction = waypoint.cpy().sub(npc.getPosition()).nor();
        float speed = (npc.getType() == NPCType.DOG) ? NPC.DOG_SPEED : NPC.MOVE_SPEED;
        npc.setVelocity(direction.x * speed, 0, direction.z * speed);
    }

    /**
     * Get random position near a point.
     */
    private Vector3 getRandomNearbyPosition(Vector3 center, float radius) {
        float angle = random.nextFloat() * (float) Math.PI * 2;
        float distance = random.nextFloat() * radius;

        float x = center.x + (float) Math.cos(angle) * distance;
        float z = center.z + (float) Math.sin(angle) * distance;

        return new Vector3(x, center.y, z);
    }

    /**
     * Get random walkable position near a point.
     * Tries multiple times to find a valid walkable position.
     */
    private Vector3 getRandomWalkablePosition(Vector3 center, World world, float radius) {
        // Try up to 10 times to find a walkable position
        for (int attempt = 0; attempt < 10; attempt++) {
            float angle = random.nextFloat() * (float) Math.PI * 2;
            float distance = random.nextFloat() * radius;

            float x = center.x + (float) Math.cos(angle) * distance;
            float z = center.z + (float) Math.sin(angle) * distance;

            int blockX = (int) Math.floor(x);
            int blockY = (int) Math.floor(center.y);
            int blockZ = (int) Math.floor(z);

            // Check if position is walkable (air above solid ground)
            BlockType below = world.getBlock(blockX, blockY - 1, blockZ);
            BlockType atPos = world.getBlock(blockX, blockY, blockZ);
            BlockType above = world.getBlock(blockX, blockY + 1, blockZ);

            if (below.isSolid() && !atPos.isSolid() && !above.isSolid()) {
                return new Vector3(x, center.y, z);
            }
        }

        // Fallback to nearby position if no walkable position found
        return getRandomNearbyPosition(center, radius);
    }

    /**
     * Keep dogs within park boundaries.
     */
    private void enforceParKBoundaries(NPC npc) {
        if (!npc.isWithinBounds(PARK_MIN_X, PARK_MIN_Z, PARK_MAX_X, PARK_MAX_Z)) {
            // Push back toward center
            Vector3 parkCenter = new Vector3(0, npc.getPosition().y, 0);
            Vector3 direction = parkCenter.cpy().sub(npc.getPosition()).nor();
            npc.getPosition().add(direction.scl(0.1f));
        }
    }

    /**
     * Handle punching an NPC - applies knockback.
     */
    public void punchNPC(NPC npc, Vector3 punchDirection) {
        npc.applyKnockback(punchDirection, 2.0f); // 2 blocks of knockback

        // Special handling for council builders
        if (npc.getType() == NPCType.COUNCIL_BUILDER) {
            punchCouncilBuilder(npc);
        }
    }

    /**
     * Notify manager of a placed block (for structure detection).
     */
    public void notifyBlockPlaced(Vector3 position) {
        // Track placed blocks for structure detection
        // This is a simplified version - real implementation would maintain a set
    }

    /**
     * Update police spawning based on time of day.
     * Police spawn at night (20:00-06:00) and despawn during day.
     */
    public void updatePoliceSpawning(float time, World world, Player player) {
        boolean isNight = time >= 20.0f || time < 6.0f;

        if (isNight && !policeSpawned) {
            // Spawn police at night
            spawnPolice(player, world);
            policeSpawned = true;
        } else if (!isNight && policeSpawned) {
            // Despawn police during day
            despawnPolice();
            policeSpawned = false;
        }
    }

    /**
     * Spawn police NPCs around the player.
     */
    private void spawnPolice(Player player, World world) {
        // Spawn 2-3 police around the player
        int policeCount = 2 + random.nextInt(2);

        for (int i = 0; i < policeCount; i++) {
            // Spawn police 15-25 blocks away from player
            float angle = random.nextFloat() * (float) Math.PI * 2;
            float distance = 15 + random.nextFloat() * 10;

            float x = player.getPosition().x + (float) Math.cos(angle) * distance;
            float z = player.getPosition().z + (float) Math.sin(angle) * distance;

            NPC police = spawnNPC(NPCType.POLICE, x, 1, z);
            police.setState(NPCState.PATROLLING);
        }
    }

    /**
     * Despawn all police NPCs.
     */
    private void despawnPolice() {
        List<NPC> policeToRemove = new ArrayList<>();

        for (NPC npc : npcs) {
            if (npc.getType() == NPCType.POLICE) {
                policeToRemove.add(npc);
            }
        }

        for (NPC police : policeToRemove) {
            npcs.remove(police);
            policeWarningTimers.remove(police);
            policeTargetStructures.remove(police);
        }
    }

    /**
     * Update police behavior.
     */
    private void updatePolice(NPC police, float delta, World world, Player player, TooltipSystem tooltipSystem) {
        switch (police.getState()) {
            case PATROLLING:
                updatePolicePatrolling(police, delta, world, player, tooltipSystem);
                break;
            case WARNING:
                updatePoliceWarning(police, delta, world, player);
                break;
            case AGGRESSIVE:
            case ARRESTING:
                updatePoliceAggressive(police, delta, world, player);
                break;
        }
    }

    /**
     * Update police patrolling behavior.
     */
    private void updatePolicePatrolling(NPC police, float delta, World world, Player player, TooltipSystem tooltipSystem) {
        // Scan for player structures around police
        Vector3 structure = scanForStructures(world, police.getPosition(), 40);

        if (structure != null) {
            // Found a structure - investigate
            policeTargetStructures.put(police, structure);
            setNPCTarget(police, structure, world);

            // If police reaches the structure, apply tape immediately
            if (police.isNear(structure, 3.0f)) {
                applyPoliceTapeToStructure(world, structure);
            }
        } else {
            // Approach player
            setNPCTarget(police, player.getPosition(), world);
        }

        // Check if adjacent to player - issue warning
        if (police.isNear(player.getPosition(), 2.0f)) {
            police.setState(NPCState.WARNING);
            police.setSpeechText("Move along, nothing to see here.", 3.0f);
            policeWarningTimers.put(police, 0.0f);

            // Record structure near player if any
            if (structure != null) {
                policeTargetStructures.put(police, structure);
            }

            // Trigger first police encounter tooltip
            if (tooltipSystem != null) {
                tooltipSystem.trigger(TooltipTrigger.FIRST_POLICE_ENCOUNTER);
            }
        }
    }

    /**
     * Update police warning behavior.
     */
    private void updatePoliceWarning(NPC police, float delta, World world, Player player) {
        // Increment warning timer
        float timer = policeWarningTimers.getOrDefault(police, 0.0f);
        timer += delta;
        policeWarningTimers.put(police, timer);

        // Check if player is near a structure
        Vector3 targetStructure = policeTargetStructures.get(police);

        // If no target structure, scan for one near the police/player
        if (targetStructure == null) {
            targetStructure = scanForStructures(world, police.getPosition(), 20);
            if (targetStructure == null) {
                targetStructure = scanForStructures(world, player.getPosition(), 20);
            }
            if (targetStructure != null) {
                policeTargetStructures.put(police, targetStructure);
            }
        }

        boolean playerNearStructure = false;
        if (targetStructure != null) {
            playerNearStructure = player.getPosition().dst(targetStructure) < 10.0f;
        }

        // Escalate after 2 seconds if player stays near structure
        if (timer >= 2.0f && playerNearStructure) {
            police.setState(NPCState.AGGRESSIVE);
            police.setSpeechText("Right, you're nicked!", 2.0f);

            // Apply police tape to structure
            if (targetStructure != null) {
                applyPoliceTapeToStructure(world, targetStructure);
            }

            // Spawn additional police
            spawnNPC(NPCType.POLICE, police.getPosition().x + 3, police.getPosition().y, police.getPosition().z);
        } else if (timer >= 3.0f) {
            // Go back to patrolling after warning expires
            police.setState(NPCState.PATROLLING);
            policeWarningTimers.remove(police);
        }
    }

    /**
     * Update police aggressive/arresting behavior.
     */
    private void updatePoliceAggressive(NPC police, float delta, World world, Player player) {
        // Move toward player
        setNPCTarget(police, player.getPosition(), world);

        // If very close, teleport player away
        if (police.isNear(player.getPosition(), 1.5f)) {
            // Teleport player 50 blocks away
            Vector3 escapePos = player.getPosition().cpy();
            escapePos.x += (random.nextBoolean() ? 50 : -50);
            escapePos.z += (random.nextBoolean() ? 50 : -50);
            player.getPosition().set(escapePos);

            // Police goes back to patrolling
            police.setState(NPCState.PATROLLING);
        }
    }

    /**
     * Scan for player-built structures around a position.
     * @return position of structure center, or null if none found
     */
    private Vector3 scanForStructures(World world, Vector3 center, int radius) {
        int playerBlockCount = 0;
        Vector3 structureCenter = null;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = (int) (center.x + dx);
                int z = (int) (center.z + dz);

                for (int y = 1; y < 10; y++) {
                    BlockType block = world.getBlock(x, y, z);
                    // Check for placed blocks (wood, brick)
                    if (block == BlockType.WOOD || block == BlockType.BRICK) {
                        playerBlockCount++;
                        if (structureCenter == null) {
                            structureCenter = new Vector3(x, y, z);
                        }
                    }
                }
            }
        }

        // Need at least 5 blocks to be considered a structure
        if (playerBlockCount >= 5 && structureCenter != null) {
            return structureCenter;
        }

        return null;
    }

    /**
     * Apply police tape to blocks in a structure.
     */
    private void applyPoliceTapeToStructure(World world, Vector3 structureCenter) {
        // Tape a few blocks around the structure center
        int tapeRadius = 2;
        int tapedCount = 0;

        for (int dx = -tapeRadius; dx <= tapeRadius && tapedCount < 5; dx++) {
            for (int dz = -tapeRadius; dz <= tapeRadius && tapedCount < 5; dz++) {
                for (int dy = 0; dy < 3 && tapedCount < 5; dy++) {
                    int x = (int) structureCenter.x + dx;
                    int y = (int) structureCenter.y + dy;
                    int z = (int) structureCenter.z + dz;

                    BlockType block = world.getBlock(x, y, z);
                    if (block == BlockType.WOOD || block == BlockType.BRICK) {
                        world.addPoliceTape(x, y, z);
                        tapedCount++;
                    }
                }
            }
        }
    }

    // ========== Phase 7: Council Builder System ==========

    /**
     * Update council builders based on detected structures.
     */
    private void updateCouncilBuilders(World world, TooltipSystem tooltipSystem) {
        List<StructureTracker.Structure> largeStructures = structureTracker.getLargeStructures();

        for (StructureTracker.Structure structure : largeStructures) {
            int requiredBuilders = structureTracker.calculateBuilderCount(structure);
            Vector3 structureCenter = structure.getCenter();

            // Check if this structure already has a notice (by position)
            boolean alreadyNotified = notifiedStructures.contains(structureCenter);

            // Add planning notice after structure is detected (first time only)
            if (!alreadyNotified && requiredBuilders > 0) {
                applyPlanningNotice(world, structure);
                notifiedStructures.add(structureCenter);
                structure.setHasNotice(true);
            } else if (alreadyNotified) {
                structure.setHasNotice(true); // Mark as having notice
            }

            int currentBuilders = structureBuilderCount.getOrDefault(structure, 0);

            // Spawn builders after planning notice has been up for a bit
            // Only spawn if structure has notice
            if (structure.hasNotice() && currentBuilders < requiredBuilders) {
                spawnCouncilBuilder(structure);
                structureBuilderCount.put(structure, currentBuilders + 1);
            }
        }
    }

    /**
     * Spawn a council builder to demolish a structure.
     */
    private void spawnCouncilBuilder(StructureTracker.Structure structure) {
        Vector3 center = structure.getCenter();

        // Spawn builder 10-20 blocks away from structure
        float angle = random.nextFloat() * (float) Math.PI * 2;
        float distance = 10 + random.nextFloat() * 10;

        float x = center.x + (float) Math.cos(angle) * distance;
        float z = center.z + (float) Math.sin(angle) * distance;

        NPC builder = spawnNPC(NPCType.COUNCIL_BUILDER, x, 1, z);
        builder.setState(NPCState.IDLE);
        builderTargets.put(builder, structure);
        builderDemolishTimers.put(builder, 0.0f);
    }

    /**
     * Apply planning notice to a structure.
     */
    private void applyPlanningNotice(World world, StructureTracker.Structure structure) {
        // Add planning notice to a few blocks on the structure
        Set<Vector3> blocks = structure.getBlocks();
        int noticeCount = 0;

        for (Vector3 block : blocks) {
            if (noticeCount >= 3) {
                break; // Only add notices to 3 blocks
            }
            world.addPlanningNotice((int)block.x, (int)block.y, (int)block.z);
            noticeCount++;
        }
    }

    /**
     * Update a council builder's behavior.
     */
    private void updateCouncilBuilder(NPC builder, float delta, World world, TooltipSystem tooltipSystem) {
        // Update knockback timer
        Float knockbackTimer = builderKnockbackTimers.get(builder);
        if (knockbackTimer != null && knockbackTimer > 0) {
            builderKnockbackTimers.put(builder, knockbackTimer - delta);
            if (knockbackTimer - delta <= 0) {
                builder.setState(NPCState.IDLE); // Return to normal
            }
            builder.setVelocity(0, 0, 0);
            return; // Don't demolish while knocked back
        }

        StructureTracker.Structure target = builderTargets.get(builder);
        if (target == null || target.isEmpty()) {
            // No target or structure demolished - remove builder
            npcs.remove(builder);
            builderTargets.remove(builder);
            builderKnockbackTimers.remove(builder);
            builderDemolishTimers.remove(builder);
            return;
        }

        Vector3 targetPos = target.getCenter();

        // Move toward structure
        if (!builder.isNear(targetPos, 3.0f)) {
            setNPCTarget(builder, targetPos, world);
        } else {
            // Adjacent to structure - start demolishing
            builder.setState(NPCState.DEMOLISHING);
            builder.setVelocity(0, 0, 0);

            // Demolish blocks periodically
            float demolishTimer = builderDemolishTimers.getOrDefault(builder, 0.0f);
            demolishTimer += delta;
            builderDemolishTimers.put(builder, demolishTimer);

            if (demolishTimer >= 1.0f) { // Demolish one block per second
                demolishBlock(world, target, tooltipSystem);
                builderDemolishTimers.put(builder, 0.0f);
            }
        }
    }

    /**
     * Demolish a block from a structure.
     */
    private void demolishBlock(World world, StructureTracker.Structure structure, TooltipSystem tooltipSystem) {
        if (structure.getBlocks().isEmpty()) {
            return;
        }

        // Pick a random block from the structure
        List<Vector3> blockList = new ArrayList<>(structure.getBlocks());
        Vector3 blockToRemove = blockList.get(random.nextInt(blockList.size()));

        int x = (int) blockToRemove.x;
        int y = (int) blockToRemove.y;
        int z = (int) blockToRemove.z;

        // Remove the block
        world.setBlock(x, y, z, BlockType.AIR);
        structure.removeBlock(blockToRemove);
        structureTracker.removeBlock(x, y, z);

        // Remove planning notice if present
        world.removePlanningNotice(x, y, z);

        // Trigger tooltip on first demolition
        if (tooltipSystem != null) {
            tooltipSystem.trigger(TooltipTrigger.FIRST_COUNCIL_ENCOUNTER);
        }
    }

    /**
     * Handle punching a council builder - applies knockback and delays demolition.
     */
    public void punchCouncilBuilder(NPC builder) {
        if (builder.getType() != NPCType.COUNCIL_BUILDER) {
            return;
        }

        // Set knockback timer (delays demolition for 1 second)
        builderKnockbackTimers.put(builder, 1.0f);
        builder.setState(NPCState.KNOCKED_BACK);
    }

    /**
     * Get structure tracker (for testing).
     */
    public StructureTracker getStructureTracker() {
        return structureTracker;
    }
}

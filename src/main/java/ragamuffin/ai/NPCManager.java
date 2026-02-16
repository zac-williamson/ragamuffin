package ragamuffin.ai;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
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

    public NPCManager() {
        this.npcs = new ArrayList<>();
        this.pathfinder = new Pathfinder();
        this.random = new Random();
        this.gameTime = 8.0f; // Start at 8:00 AM
        this.playerStructures = new HashMap<>();
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
        for (NPC npc : npcs) {
            updateNPC(npc, delta, world, player, inventory, tooltipSystem);
        }
    }

    /**
     * Update a single NPC's behavior.
     */
    private void updateNPC(NPC npc, float delta, World world, Player player, Inventory inventory, TooltipSystem tooltipSystem) {
        npc.update(delta);

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
        // If no target or reached target, pick a new random target
        if (npc.getTargetPosition() == null || npc.isNear(npc.getTargetPosition(), 1.0f)) {
            Vector3 randomTarget = getRandomNearbyPosition(npc.getPosition(), 10.0f);
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
        npc.setPath(path);
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
        npc.setVelocity(direction.x * NPC.MOVE_SPEED, 0, direction.z * NPC.MOVE_SPEED);
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
        npc.setVelocity(direction.x * NPC.MOVE_SPEED, 0, direction.z * NPC.MOVE_SPEED);
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
    }

    /**
     * Notify manager of a placed block (for structure detection).
     */
    public void notifyBlockPlaced(Vector3 position) {
        // Track placed blocks for structure detection
        // This is a simplified version - real implementation would maintain a set
    }
}

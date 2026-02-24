package ragamuffin.building;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.world.BlockType;
import ragamuffin.world.Raycast;
import ragamuffin.world.RaycastResult;
import ragamuffin.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Handles block breaking mechanics - blocks require varying hits to break based on hardness and tool.
 */
public class BlockBreaker {

    /** Seconds of inactivity before a partially-damaged block fully regenerates. */
    static final float BLOCK_REGEN_SECONDS = 10f;

    /** Holds hit count and the real-time millisecond timestamp of the last hit. */
    private static final class HitRecord {
        int hits;
        long lastHitTime;

        HitRecord(int hits, long lastHitTime) {
            this.hits = hits;
            this.lastHitTime = lastHitTime;
        }
    }

    private final Map<String, HitRecord> blockHits;

    public BlockBreaker() {
        this.blockHits = new HashMap<>();
    }

    /**
     * Remove stale hit entries for blocks that have not been punched within
     * {@link #BLOCK_REGEN_SECONDS}. Call this once per frame from the game loop.
     *
     * @param delta frame delta time in seconds (unused — decay is wall-clock based)
     */
    public void tickDecay(@SuppressWarnings("unused") float delta) {
        long now = System.currentTimeMillis();
        long thresholdMs = (long) (BLOCK_REGEN_SECONDS * 1000L);
        Iterator<Map.Entry<String, HitRecord>> it = blockHits.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, HitRecord> entry = it.next();
            if (now - entry.getValue().lastHitTime > thresholdMs) {
                it.remove();
            }
        }
    }

    /**
     * Get the base hit count required to break a block type.
     */
    private int getBlockHardness(BlockType blockType) {
        switch (blockType) {
            case TREE_TRUNK:
            case LEAVES:
            case GRASS:
            case STAIRS:
                return 5; // soft blocks
            case BRICK:
            case STONE:
            case PAVEMENT:
                return 8; // hard blocks
            case GLASS:
            case DOOR_LOWER:
            case DOOR_UPPER:
                return 2; // fragile blocks
            default:
                return 5; // default
        }
    }

    /**
     * Calculate the actual hits needed to break a block with the given tool.
     */
    private int getHitsToBreak(BlockType blockType, Material tool) {
        int baseHardness = getBlockHardness(blockType);
        float toolMultiplier = Tool.getHitsMultiplier(tool);
        return Math.max(1, Math.round(baseHardness * toolMultiplier));
    }

    /**
     * Punch a block at the given position with bare fist.
     * @return true if the block was broken on this punch
     */
    public boolean punchBlock(World world, int x, int y, int z) {
        return punchBlock(world, x, y, z, null);
    }

    /**
     * Punch a block at the given position with a tool.
     * @param tool the tool material being used, or null for bare fist
     * @return true if the block was broken on this punch
     */
    public boolean punchBlock(World world, int x, int y, int z, Material tool) {
        BlockType blockType = world.getBlock(x, y, z);

        // Can't punch air or bedrock, or police-taped (protected) blocks.
        // DOOR_LOWER and DOOR_UPPER are non-solid (passable) but must still be breakable.
        boolean isDoorBlock = blockType == BlockType.DOOR_LOWER || blockType == BlockType.DOOR_UPPER;
        if (blockType == BlockType.AIR || (!blockType.isSolid() && !isDoorBlock) || blockType == BlockType.BEDROCK
                || world.isProtected(x, y, z)) {
            return false;
        }

        String key = getBlockKey(x, y, z);
        HitRecord record = blockHits.get(key);
        int hits = (record != null ? record.hits : 0) + 1;
        int hitsToBreak = getHitsToBreak(blockType, tool);

        if (hits >= hitsToBreak) {
            // Break the block
            world.setBlock(x, y, z, BlockType.AIR);
            blockHits.remove(key);
            return true;
        } else {
            // Increment hit counter, refresh timestamp
            blockHits.put(key, new HitRecord(hits, System.currentTimeMillis()));
            return false;
        }
    }

    /**
     * Get the current hit count for a block.
     */
    public int getHitCount(int x, int y, int z) {
        String key = getBlockKey(x, y, z);
        HitRecord record = blockHits.get(key);
        return record != null ? record.hits : 0;
    }

    /**
     * Get the break progress for a block as a fraction (0.0 to 1.0).
     * @param tool the tool being used, or null for bare fist
     */
    public float getBreakProgress(World world, int x, int y, int z, Material tool) {
        BlockType blockType = world.getBlock(x, y, z);
        if (blockType == BlockType.AIR) {
            return 0.0f;
        }
        int hits = getHitCount(x, y, z);
        int hitsToBreak = getHitsToBreak(blockType, tool);
        return (float) hits / (float) hitsToBreak;
    }

    /**
     * Clear the hit counter for a specific block position.
     * Call this when a block is removed externally (e.g. by an NPC) so stale
     * hit counts don't carry over to a newly-placed block at the same position.
     */
    public void clearHits(int x, int y, int z) {
        blockHits.remove(getBlockKey(x, y, z));
    }

    /**
     * Reset all hit counters.
     */
    public void resetHits() {
        blockHits.clear();
    }

    /**
     * Get the block the player is currently looking at using raycasting.
     * @return RaycastResult if looking at a block, null otherwise
     */
    public RaycastResult getTargetBlock(World world, Vector3 origin, Vector3 direction, float maxDistance) {
        return Raycast.cast(world, origin, direction, maxDistance);
    }

    /**
     * Return the set of block positions (as "x,y,z" keys) that have at least one hit recorded.
     * Used by the renderer to draw crack overlays on all partially-damaged blocks.
     */
    public Set<String> getDamagedBlockKeys() {
        return new HashSet<>(blockHits.keySet());
    }

    /**
     * Parse a block key produced by {@link #getBlockKey} back into integer coordinates.
     * Returns an int[3] of {x, y, z}.
     */
    public static int[] parseBlockKey(String key) {
        String[] parts = key.split(",");
        return new int[]{ Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]) };
    }

    /**
     * Generate a unique key for a block position.
     */
    private String getBlockKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    /**
     * Test-only helper: subtract {@code ageMs} milliseconds from the last-hit timestamp
     * of the block at (x, y, z), making it appear older to {@link #tickDecay}.
     * Only has an effect if a HitRecord exists for that block.
     */
    void backdateHitsForTesting(int x, int y, int z, long ageMs) {
        String key = getBlockKey(x, y, z);
        HitRecord record = blockHits.get(key);
        if (record != null) {
            record.lastHitTime -= ageMs;
        }
    }
}

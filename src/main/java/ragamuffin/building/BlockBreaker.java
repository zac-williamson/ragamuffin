package ragamuffin.building;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.world.BlockType;
import ragamuffin.world.Raycast;
import ragamuffin.world.RaycastResult;
import ragamuffin.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles block breaking mechanics - blocks require 5 hits to break.
 */
public class BlockBreaker {
    private static final int HITS_TO_BREAK = 5;

    private final Map<String, Integer> blockHits;

    public BlockBreaker() {
        this.blockHits = new HashMap<>();
    }

    /**
     * Punch a block at the given position.
     * @return true if the block was broken on this punch
     */
    public boolean punchBlock(World world, int x, int y, int z) {
        BlockType blockType = world.getBlock(x, y, z);

        // Can't punch air
        if (blockType == BlockType.AIR || !blockType.isSolid()) {
            return false;
        }

        String key = getBlockKey(x, y, z);
        int hits = blockHits.getOrDefault(key, 0) + 1;

        if (hits >= HITS_TO_BREAK) {
            // Break the block
            world.setBlock(x, y, z, BlockType.AIR);
            blockHits.remove(key);
            return true;
        } else {
            // Increment hit counter
            blockHits.put(key, hits);
            return false;
        }
    }

    /**
     * Get the current hit count for a block.
     */
    public int getHitCount(int x, int y, int z) {
        String key = getBlockKey(x, y, z);
        return blockHits.getOrDefault(key, 0);
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
     * Generate a unique key for a block position.
     */
    private String getBlockKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }
}

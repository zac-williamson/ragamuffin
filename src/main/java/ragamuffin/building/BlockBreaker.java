package ragamuffin.building;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.world.BlockType;
import ragamuffin.world.Raycast;
import ragamuffin.world.RaycastResult;
import ragamuffin.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles block breaking mechanics - blocks require varying hits to break based on hardness and tool.
 */
public class BlockBreaker {
    private final Map<String, Integer> blockHits;

    public BlockBreaker() {
        this.blockHits = new HashMap<>();
    }

    /**
     * Get the base hit count required to break a block type.
     */
    private int getBlockHardness(BlockType blockType) {
        switch (blockType) {
            case TREE_TRUNK:
            case LEAVES:
            case GRASS:
                return 5; // soft blocks
            case BRICK:
            case STONE:
            case PAVEMENT:
                return 8; // hard blocks
            case GLASS:
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

        // Can't punch air or bedrock
        if (blockType == BlockType.AIR || !blockType.isSolid() || blockType == BlockType.BEDROCK) {
            return false;
        }

        String key = getBlockKey(x, y, z);
        int hits = blockHits.getOrDefault(key, 0) + 1;
        int hitsToBreak = getHitsToBreak(blockType, tool);

        if (hits >= hitsToBreak) {
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

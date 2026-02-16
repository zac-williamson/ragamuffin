package ragamuffin.world;

import com.badlogic.gdx.math.Vector3;

/**
 * Result of a raycast against the voxel world.
 */
public class RaycastResult {
    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private final BlockType blockType;
    private final Vector3 hitPosition;
    private final float distance;

    public RaycastResult(int blockX, int blockY, int blockZ, BlockType blockType, Vector3 hitPosition, float distance) {
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.blockType = blockType;
        this.hitPosition = hitPosition;
        this.distance = distance;
    }

    public int getBlockX() {
        return blockX;
    }

    public int getBlockY() {
        return blockY;
    }

    public int getBlockZ() {
        return blockZ;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public Vector3 getHitPosition() {
        return hitPosition;
    }

    public float getDistance() {
        return distance;
    }
}

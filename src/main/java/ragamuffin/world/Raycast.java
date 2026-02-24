package ragamuffin.world;

import com.badlogic.gdx.math.Vector3;

/**
 * Voxel raycast utility using DDA algorithm.
 * Finds the first solid block intersected by a ray.
 */
public class Raycast {

    /**
     * Cast a ray through the voxel world and return the first solid block hit.
     *
     * @param world The world to raycast in
     * @param origin Starting position of the ray
     * @param direction Direction vector (should be normalized)
     * @param maxDistance Maximum distance to check
     * @return RaycastResult if a block was hit, null otherwise
     */
    public static RaycastResult cast(World world, Vector3 origin, Vector3 direction, float maxDistance) {
        // DDA voxel traversal algorithm
        Vector3 pos = new Vector3(origin);
        Vector3 dir = new Vector3(direction).nor();

        // Current voxel position
        int x = (int) Math.floor(pos.x);
        int y = (int) Math.floor(pos.y);
        int z = (int) Math.floor(pos.z);

        // Step direction for each axis
        int stepX = dir.x > 0 ? 1 : (dir.x < 0 ? -1 : 0);
        int stepY = dir.y > 0 ? 1 : (dir.y < 0 ? -1 : 0);
        int stepZ = dir.z > 0 ? 1 : (dir.z < 0 ? -1 : 0);

        // Distance along the ray to cross one voxel on each axis
        float tDeltaX = stepX != 0 ? Math.abs(1.0f / dir.x) : Float.MAX_VALUE;
        float tDeltaY = stepY != 0 ? Math.abs(1.0f / dir.y) : Float.MAX_VALUE;
        float tDeltaZ = stepZ != 0 ? Math.abs(1.0f / dir.z) : Float.MAX_VALUE;

        // Distance along ray to next voxel boundary on each axis
        float tMaxX = stepX > 0 ? (x + 1 - pos.x) / dir.x : (stepX < 0 ? (pos.x - x) / -dir.x : Float.MAX_VALUE);
        float tMaxY = stepY > 0 ? (y + 1 - pos.y) / dir.y : (stepY < 0 ? (pos.y - y) / -dir.y : Float.MAX_VALUE);
        float tMaxZ = stepZ > 0 ? (z + 1 - pos.z) / dir.z : (stepZ < 0 ? (pos.z - z) / -dir.z : Float.MAX_VALUE);

        float distance = 0;

        // Traverse voxels along the ray
        while (distance < maxDistance) {
            // Check if current voxel is solid or a door block (non-solid but targetable/breakable)
            BlockType block = world.getBlock(x, y, z);
            boolean isDoorBlock = block == BlockType.DOOR_LOWER || block == BlockType.DOOR_UPPER;
            if (block != BlockType.AIR && (block.isSolid() || isDoorBlock)) {
                Vector3 hitPos = new Vector3(origin).add(new Vector3(dir).scl(distance));
                return new RaycastResult(x, y, z, block, hitPos, distance);
            }

            // Step to next voxel
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    distance = tMaxX;
                    tMaxX += tDeltaX;
                } else {
                    z += stepZ;
                    distance = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    distance = tMaxY;
                    tMaxY += tDeltaY;
                } else {
                    z += stepZ;
                    distance = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            }
        }

        return null; // No hit within max distance
    }
}

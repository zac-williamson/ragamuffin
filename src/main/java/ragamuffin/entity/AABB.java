package ragamuffin.entity;

import com.badlogic.gdx.math.Vector3;

/**
 * Axis-Aligned Bounding Box for collision detection.
 */
public class AABB {
    private float minX, minY, minZ;
    private float maxX, maxY, maxZ;

    public AABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public AABB(Vector3 center, float width, float height, float depth) {
        float halfWidth = width / 2f;
        float halfDepth = depth / 2f;
        this.minX = center.x - halfWidth;
        this.maxX = center.x + halfWidth;
        this.minY = center.y;
        this.maxY = center.y + height;
        this.minZ = center.z - halfDepth;
        this.maxZ = center.z + halfDepth;
    }

    public boolean intersects(AABB other) {
        return this.minX < other.maxX && this.maxX > other.minX &&
               this.minY < other.maxY && this.maxY > other.minY &&
               this.minZ < other.maxZ && this.maxZ > other.minZ;
    }

    /**
     * Returns true only when the overlap on every axis exceeds {@code tolerance}
     * blocks.  Use this for player-car collisions to avoid triggering damage
     * on a bare edge-graze — the player must genuinely penetrate the box by
     * at least {@code tolerance} on each axis.
     *
     * @param other     the other AABB to test against
     * @param tolerance minimum required overlap (blocks) on each axis
     */
    public boolean intersectsWithTolerance(AABB other, float tolerance) {
        return this.minX + tolerance < other.maxX && this.maxX - tolerance > other.minX &&
               this.minY + tolerance < other.maxY && this.maxY - tolerance > other.minY &&
               this.minZ + tolerance < other.maxZ && this.maxZ - tolerance > other.minZ;
    }

    public float getMinX() { return minX; }
    public float getMinY() { return minY; }
    public float getMinZ() { return minZ; }
    public float getMaxX() { return maxX; }
    public float getMaxY() { return maxY; }
    public float getMaxZ() { return maxZ; }

    public float getWidth() { return maxX - minX; }
    public float getHeight() { return maxY - minY; }
    public float getDepth() { return maxZ - minZ; }

    public void setPosition(Vector3 center, float width, float height, float depth) {
        float halfWidth = width / 2f;
        float halfDepth = depth / 2f;
        this.minX = center.x - halfWidth;
        this.maxX = center.x + halfWidth;
        this.minY = center.y;
        this.maxY = center.y + height;
        this.minZ = center.z - halfDepth;
        this.maxZ = center.z + halfDepth;
    }
}

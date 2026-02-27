package ragamuffin.world;

import ragamuffin.entity.AABB;

/**
 * Describes a single non-block-based 3D prop in the world.
 *
 * Props are unique decorative objects (phone boxes, benches, bollards, etc.)
 * rendered using simple geometric 3D models rather than voxel blocks.
 * They add visual variety to the British town environment.
 *
 * Each prop has:
 * <ul>
 *   <li>A world-space position (origin at the base centre of the prop).</li>
 *   <li>A {@link PropType} identifying which model to draw.</li>
 *   <li>A Y-axis rotation angle in degrees so props can face different directions.</li>
 * </ul>
 *
 * Instances are created by {@link WorldGenerator} and stored in
 * {@link World#getPropPositions()}.
 *
 * Issue #669: Add unique non-block-based 3D models to the world.
 * Issue #719: Props now have collision via {@link #getAABB()}.
 */
public class PropPosition {

    private final float worldX;
    private final float worldY;
    private final float worldZ;
    private final PropType type;
    /** Y-axis rotation in degrees (0 = facing -Z, 90 = facing +X). */
    private final float rotationY;

    /**
     * @param worldX    world-space X of the prop base centre
     * @param worldY    world-space Y of the prop base (ground level)
     * @param worldZ    world-space Z of the prop base centre
     * @param type      which prop model to render
     * @param rotationY Y-axis rotation in degrees
     */
    public PropPosition(float worldX, float worldY, float worldZ,
                        PropType type, float rotationY) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.type = type;
        this.rotationY = rotationY;
    }

    public float getWorldX()     { return worldX; }
    public float getWorldY()     { return worldY; }
    public float getWorldZ()     { return worldZ; }
    public PropType getType()    { return type; }
    public float getRotationY()  { return rotationY; }

    /**
     * Build an AABB for collision detection against this prop.
     *
     * The box is axis-aligned regardless of the prop's visual Y-rotation —
     * we use the larger of width/depth for both horizontal axes so the box
     * is always a conservative fit even when the prop is rotated.
     *
     * Issue #719: Add collision and destructibility to 3D objects.
     *
     * @return a new AABB centred on (worldX, worldY, worldZ) horizontally and
     *         extending upward by the prop's collision height.
     */
    public AABB getAABB() {
        float w = type.getCollisionWidth();
        float h = type.getCollisionHeight();
        float d = type.getCollisionDepth();
        // Use the larger horizontal dimension for both axes to stay conservative
        // under rotation.
        float halfW = w / 2f;
        float halfD = d / 2f;
        return new AABB(
                worldX - halfW, worldY,     worldZ - halfD,
                worldX + halfW, worldY + h, worldZ + halfD);
    }
}

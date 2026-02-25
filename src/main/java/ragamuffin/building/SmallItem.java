package ragamuffin.building;

import com.badlogic.gdx.math.Vector3;

/**
 * Represents a small item placed in the world without grid snapping.
 * Unlike full blocks, small items use precise float coordinates so they can be
 * positioned freely on the surface of a block (e.g. a can on a table, a book
 * on a shelf) without being locked to the voxel grid.
 */
public class SmallItem {

    private final Material material;
    private final Vector3 position; // Exact world-space position (not grid-snapped)

    public SmallItem(Material material, Vector3 position) {
        this.material = material;
        this.position = new Vector3(position);
    }

    public Material getMaterial() {
        return material;
    }

    /**
     * Returns the exact world-space position of this small item.
     * The position represents the base centre of the item.
     */
    public Vector3 getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "SmallItem{" + material + " at " + position + "}";
    }
}

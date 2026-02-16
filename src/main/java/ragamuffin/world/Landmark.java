package ragamuffin.world;

import com.badlogic.gdx.math.Vector3;

/**
 * Represents a landmark in the world - a significant building or area.
 */
public class Landmark {
    private final LandmarkType type;
    private final Vector3 position;
    private final int width;
    private final int height;
    private final int depth;

    public Landmark(LandmarkType type, Vector3 position, int width, int height, int depth) {
        this.type = type;
        this.position = position;
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public Landmark(LandmarkType type, int x, int y, int z, int width, int height, int depth) {
        this(type, new Vector3(x, y, z), width, height, depth);
    }

    public LandmarkType getType() {
        return type;
    }

    public Vector3 getPosition() {
        return position;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    /**
     * Check if a world position is within this landmark's bounds.
     */
    public boolean contains(int x, int y, int z) {
        return x >= position.x && x < position.x + width &&
               y >= position.y && y < position.y + height &&
               z >= position.z && z < position.z + depth;
    }
}

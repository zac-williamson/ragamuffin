package ragamuffin.world;

import com.badlogic.gdx.graphics.Color;

/**
 * All block types in the Ragamuffin world.
 * Each block has an ID, solidity state, and eventually textures.
 */
public enum BlockType {
    AIR(0, false),
    GRASS(1, true),
    DIRT(2, true),
    STONE(3, true),
    PAVEMENT(4, true),
    ROAD(5, true),
    BRICK(6, true),
    GLASS(7, true),
    WOOD(8, true),
    WATER(9, false),
    TREE_TRUNK(10, true),
    LEAVES(11, true),
    CARDBOARD(12, true),
    IRON_FENCE(13, true);

    private final int id;
    private final boolean solid;

    BlockType(int id, boolean solid) {
        this.id = id;
        this.solid = solid;
    }

    public int getId() {
        return id;
    }

    public boolean isSolid() {
        return solid;
    }

    public static BlockType fromId(int id) {
        for (BlockType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return AIR;
    }

    /**
     * Get the color for this block type.
     */
    public Color getColor() {
        switch (this) {
            case AIR: return new Color(0, 0, 0, 0); // Transparent
            case GRASS: return new Color(0.3f, 0.7f, 0.2f, 1f); // Green
            case DIRT: return new Color(0.6f, 0.4f, 0.2f, 1f); // Brown
            case STONE: return new Color(0.6f, 0.6f, 0.6f, 1f); // Grey
            case PAVEMENT: return new Color(0.7f, 0.7f, 0.7f, 1f); // Light grey
            case ROAD: return new Color(0.3f, 0.3f, 0.3f, 1f); // Dark grey
            case BRICK: return new Color(0.7f, 0.3f, 0.2f, 1f); // Red-brown
            case GLASS: return new Color(0.6f, 0.8f, 0.95f, 0.5f); // Light blue, semi-transparent
            case WOOD: return new Color(0.7f, 0.5f, 0.3f, 1f); // Light brown
            case WATER: return new Color(0.2f, 0.3f, 0.8f, 0.7f); // Blue
            case TREE_TRUNK: return new Color(0.4f, 0.25f, 0.1f, 1f); // Dark brown
            case LEAVES: return new Color(0.2f, 0.5f, 0.1f, 1f); // Dark green
            case CARDBOARD: return new Color(0.7f, 0.6f, 0.4f, 1f); // Tan
            case IRON_FENCE: return new Color(0.2f, 0.2f, 0.2f, 1f); // Dark iron grey
            default: return new Color(1f, 1f, 1f, 1f); // White fallback
        }
    }
}

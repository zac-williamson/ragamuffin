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
    IRON_FENCE(13, true),
    SIGN_WHITE(14, true),
    SIGN_RED(15, true),
    SIGN_BLUE(16, true),
    SIGN_GREEN(17, true),
    SIGN_YELLOW(18, true),
    GARDEN_WALL(19, true),
    CONCRETE(20, true),
    ROOF_TILE(21, true),
    TARMAC(22, true),
    CORRUGATED_METAL(23, true),
    RENDER_WHITE(24, true),
    RENDER_CREAM(25, true),
    SLATE(26, true),
    PEBBLEDASH(27, true),
    DOOR_WOOD(28, true),
    LINOLEUM(29, true),
    YELLOW_BRICK(30, true),
    TILE_WHITE(31, true),
    TILE_BLACK(32, true),
    RENDER_PINK(33, true),
    METAL_RED(34, true);

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
            case SIGN_WHITE: return new Color(0.95f, 0.95f, 0.95f, 1f); // White sign
            case SIGN_RED: return new Color(0.9f, 0.1f, 0.1f, 1f); // Red sign
            case SIGN_BLUE: return new Color(0.1f, 0.2f, 0.7f, 1f); // Blue sign
            case SIGN_GREEN: return new Color(0.1f, 0.6f, 0.2f, 1f); // Green sign
            case SIGN_YELLOW: return new Color(0.9f, 0.8f, 0.1f, 1f); // Yellow sign
            case GARDEN_WALL: return new Color(0.5f, 0.45f, 0.4f, 1f); // Grey-brown garden wall
            case CONCRETE: return new Color(0.75f, 0.75f, 0.73f, 1f); // Pale concrete
            case ROOF_TILE: return new Color(0.55f, 0.2f, 0.15f, 1f); // Terracotta roof
            case TARMAC: return new Color(0.22f, 0.22f, 0.22f, 1f); // Near-black tarmac
            case CORRUGATED_METAL: return new Color(0.55f, 0.58f, 0.6f, 1f); // Silver-grey metal
            case RENDER_WHITE: return new Color(0.92f, 0.9f, 0.88f, 1f); // Off-white render
            case RENDER_CREAM: return new Color(0.9f, 0.85f, 0.7f, 1f); // Cream render
            case SLATE: return new Color(0.35f, 0.38f, 0.42f, 1f); // Dark blue-grey slate
            case PEBBLEDASH: return new Color(0.78f, 0.75f, 0.7f, 1f); // Greyish pebbledash
            case DOOR_WOOD: return new Color(0.45f, 0.28f, 0.12f, 1f); // Dark brown door
            case LINOLEUM: return new Color(0.6f, 0.55f, 0.45f, 1f); // Beige lino floor
            case YELLOW_BRICK: return new Color(0.85f, 0.75f, 0.4f, 1f); // London stock yellow brick
            case TILE_WHITE: return new Color(0.95f, 0.95f, 0.95f, 1f); // Clean white tile
            case TILE_BLACK: return new Color(0.15f, 0.15f, 0.15f, 1f); // Black tile
            case RENDER_PINK: return new Color(0.9f, 0.7f, 0.75f, 1f); // Pink rendered wall
            case METAL_RED: return new Color(0.8f, 0.15f, 0.1f, 1f); // Red painted metal
            default: return new Color(1f, 1f, 1f, 1f); // White fallback
        }
    }
}

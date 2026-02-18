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
    METAL_RED(34, true),
    COUNTER(35, true),     // Shop counter
    SHELF(36, true),       // Shop shelf
    TABLE(37, true),       // Pub/cafe table
    CARPET(38, true),      // Floor carpet
    LINO_GREEN(39, true),  // Cheap lino flooring
    BOOKSHELF(40, true);   // Library bookshelf

    private final int id;
    private final boolean solid;
    private Color cachedColor;

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
     * Get the color for this block type. Cached to avoid allocation.
     */
    public Color getColor() {
        if (cachedColor != null) return cachedColor;
        switch (this) {
            case AIR: cachedColor = new Color(0, 0, 0, 0); break;
            case GRASS: cachedColor = new Color(0.3f, 0.7f, 0.2f, 1f); break;
            case DIRT: cachedColor = new Color(0.6f, 0.4f, 0.2f, 1f); break;
            case STONE: cachedColor = new Color(0.6f, 0.6f, 0.6f, 1f); break;
            case PAVEMENT: cachedColor = new Color(0.7f, 0.7f, 0.7f, 1f); break;
            case ROAD: cachedColor = new Color(0.3f, 0.3f, 0.3f, 1f); break;
            case BRICK: cachedColor = new Color(0.7f, 0.3f, 0.2f, 1f); break;
            case GLASS: cachedColor = new Color(0.6f, 0.8f, 0.95f, 0.5f); break;
            case WOOD: cachedColor = new Color(0.7f, 0.5f, 0.3f, 1f); break;
            case WATER: cachedColor = new Color(0.2f, 0.3f, 0.8f, 0.7f); break;
            case TREE_TRUNK: cachedColor = new Color(0.4f, 0.25f, 0.1f, 1f); break;
            case LEAVES: cachedColor = new Color(0.2f, 0.5f, 0.1f, 1f); break;
            case CARDBOARD: cachedColor = new Color(0.7f, 0.6f, 0.4f, 1f); break;
            case IRON_FENCE: cachedColor = new Color(0.2f, 0.2f, 0.2f, 1f); break;
            case SIGN_WHITE: cachedColor = new Color(0.95f, 0.95f, 0.95f, 1f); break;
            case SIGN_RED: cachedColor = new Color(0.9f, 0.1f, 0.1f, 1f); break;
            case SIGN_BLUE: cachedColor = new Color(0.1f, 0.2f, 0.7f, 1f); break;
            case SIGN_GREEN: cachedColor = new Color(0.1f, 0.6f, 0.2f, 1f); break;
            case SIGN_YELLOW: cachedColor = new Color(0.9f, 0.8f, 0.1f, 1f); break;
            case GARDEN_WALL: cachedColor = new Color(0.5f, 0.45f, 0.4f, 1f); break;
            case CONCRETE: cachedColor = new Color(0.75f, 0.75f, 0.73f, 1f); break;
            case ROOF_TILE: cachedColor = new Color(0.55f, 0.2f, 0.15f, 1f); break;
            case TARMAC: cachedColor = new Color(0.22f, 0.22f, 0.22f, 1f); break;
            case CORRUGATED_METAL: cachedColor = new Color(0.55f, 0.58f, 0.6f, 1f); break;
            case RENDER_WHITE: cachedColor = new Color(0.92f, 0.9f, 0.88f, 1f); break;
            case RENDER_CREAM: cachedColor = new Color(0.9f, 0.85f, 0.7f, 1f); break;
            case SLATE: cachedColor = new Color(0.35f, 0.38f, 0.42f, 1f); break;
            case PEBBLEDASH: cachedColor = new Color(0.78f, 0.75f, 0.7f, 1f); break;
            case DOOR_WOOD: cachedColor = new Color(0.45f, 0.28f, 0.12f, 1f); break;
            case LINOLEUM: cachedColor = new Color(0.6f, 0.55f, 0.45f, 1f); break;
            case YELLOW_BRICK: cachedColor = new Color(0.85f, 0.75f, 0.4f, 1f); break;
            case TILE_WHITE: cachedColor = new Color(0.95f, 0.95f, 0.95f, 1f); break;
            case TILE_BLACK: cachedColor = new Color(0.15f, 0.15f, 0.15f, 1f); break;
            case RENDER_PINK: cachedColor = new Color(0.9f, 0.7f, 0.75f, 1f); break;
            case METAL_RED: cachedColor = new Color(0.8f, 0.15f, 0.1f, 1f); break;
            case COUNTER: cachedColor = new Color(0.65f, 0.55f, 0.4f, 1f); break;
            case SHELF: cachedColor = new Color(0.55f, 0.4f, 0.25f, 1f); break;
            case TABLE: cachedColor = new Color(0.5f, 0.35f, 0.2f, 1f); break;
            case CARPET: cachedColor = new Color(0.5f, 0.2f, 0.2f, 1f); break;
            case LINO_GREEN: cachedColor = new Color(0.4f, 0.55f, 0.35f, 1f); break;
            case BOOKSHELF: cachedColor = new Color(0.4f, 0.3f, 0.15f, 1f); break;
            default: cachedColor = new Color(1f, 1f, 1f, 1f); break;
        }
        return cachedColor;
    }
}

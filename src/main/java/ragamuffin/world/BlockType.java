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
    BOOKSHELF(40, true),   // Library bookshelf
    BEDROCK(41, true),     // Indestructible bottom layer
    WOOD_FENCE(42, true),  // World-generated wooden fence (not player-placeable)
    WOOD_WALL(43, true);   // World-generated wooden wall/shed (not player-placeable)

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
     * Get the color for this block type (side faces). Cached to avoid allocation.
     */
    public Color getColor() {
        if (cachedColor != null) return cachedColor;
        cachedColor = buildSideColor();
        return cachedColor;
    }

    private Color cachedTopColor;
    private Color cachedBottomColor;

    /**
     * Get the top face colour (may differ from sides for visual variety).
     */
    public Color getTopColor() {
        if (cachedTopColor != null) return cachedTopColor;
        cachedTopColor = buildTopColor();
        return cachedTopColor;
    }

    /**
     * Get the bottom face colour.
     */
    public Color getBottomColor() {
        if (cachedBottomColor != null) return cachedBottomColor;
        cachedBottomColor = buildBottomColor();
        return cachedBottomColor;
    }

    /**
     * Whether this block type needs per-block colour variation (skips greedy merge).
     * Used for blocks like brick that need mortar lines / individual texture.
     */
    public boolean hasTextureDetail() {
        switch (this) {
            case BRICK:
            case YELLOW_BRICK:
            case STONE:
            case PEBBLEDASH:
            case ROOF_TILE:
            case SLATE:
            case WOOD:
            case WOOD_FENCE:
            case WOOD_WALL:
            case TREE_TRUNK:
            case BOOKSHELF:
            case CARDBOARD:
            case GARDEN_WALL:
            case TILE_WHITE:
            case TILE_BLACK:
                return true;
            default:
                return false;
        }
    }

    /**
     * Get a position-dependent colour variation for textured blocks.
     * Creates mortar lines, grain, speckle patterns based on world position.
     */
    public Color getTexturedColor(int worldX, int worldY, int worldZ, boolean isTop) {
        Color base = isTop ? getTopColor() : getColor();

        // Use position hash for deterministic variation
        int hash = worldX * 73856093 ^ worldY * 19349663 ^ worldZ * 83492791;
        float variation = ((hash & 0xFF) / 255.0f - 0.5f) * 0.08f; // ±4% variation

        switch (this) {
            case BRICK:
            case YELLOW_BRICK:
                // Mortar lines: slightly lighter colour at y-boundaries and every 4th x
                boolean isMortarY = (worldY % 3 == 0);
                boolean isMortarX = ((worldX + (worldY % 2 == 0 ? 0 : 2)) % 4 == 0);
                if (isMortarY || isMortarX) {
                    return new Color(
                        Math.min(1, base.r + 0.15f),
                        Math.min(1, base.g + 0.12f),
                        Math.min(1, base.b + 0.08f), base.a);
                }
                return new Color(base.r + variation, base.g + variation * 0.8f, base.b + variation * 0.6f, base.a);

            case STONE:
            case GARDEN_WALL:
                // Speckled stone
                return new Color(
                    clamp01(base.r + variation * 1.5f),
                    clamp01(base.g + variation * 1.5f),
                    clamp01(base.b + variation * 1.2f), base.a);

            case PEBBLEDASH:
                // Rough speckle
                float pebbleVar = ((hash & 0xFFF) / 4095.0f - 0.5f) * 0.12f;
                return new Color(
                    clamp01(base.r + pebbleVar),
                    clamp01(base.g + pebbleVar * 0.9f),
                    clamp01(base.b + pebbleVar * 0.7f), base.a);

            case ROOF_TILE:
            case SLATE:
                // Subtle tile variation
                return new Color(
                    clamp01(base.r + variation),
                    clamp01(base.g + variation * 0.7f),
                    clamp01(base.b + variation * 0.5f), base.a);

            case WOOD:
            case WOOD_FENCE:
            case WOOD_WALL:
            case TREE_TRUNK:
                // Wood grain — darken every other row
                boolean isGrainLine = (worldY % 2 == 0);
                float grainDarken = isGrainLine ? -0.05f : 0.02f;
                return new Color(
                    clamp01(base.r + grainDarken + variation),
                    clamp01(base.g + grainDarken * 0.8f + variation * 0.6f),
                    clamp01(base.b + grainDarken * 0.5f + variation * 0.3f), base.a);

            case TILE_WHITE:
            case TILE_BLACK:
                // Grid lines (grout)
                boolean isGrout = (worldX % 2 == 0) || (worldZ % 2 == 0);
                if (isGrout && isTop) {
                    return new Color(
                        clamp01(base.r + 0.08f),
                        clamp01(base.g + 0.08f),
                        clamp01(base.b + 0.08f), base.a);
                }
                return base;

            case BOOKSHELF:
                // Individual book colours
                float bookVar = ((hash & 0x1FF) / 511.0f - 0.5f) * 0.2f;
                return new Color(
                    clamp01(base.r + bookVar),
                    clamp01(base.g + bookVar * 0.5f),
                    clamp01(base.b - bookVar * 0.3f), base.a);

            case CARDBOARD:
                // Corrugation lines
                boolean isFold = (worldY % 2 == 0);
                float foldVar = isFold ? -0.04f : 0.03f;
                return new Color(
                    clamp01(base.r + foldVar + variation),
                    clamp01(base.g + foldVar * 0.8f + variation * 0.7f),
                    clamp01(base.b + foldVar * 0.5f + variation * 0.4f), base.a);

            default:
                return base;
        }
    }

    private static float clamp01(float v) {
        return Math.max(0, Math.min(1, v));
    }

    private Color buildSideColor() {
        switch (this) {
            case AIR: return new Color(0, 0, 0, 0);
            case GRASS: return new Color(0.45f, 0.35f, 0.2f, 1f);       // Brown dirt sides
            case DIRT: return new Color(0.55f, 0.38f, 0.18f, 1f);       // Warm brown
            case STONE: return new Color(0.52f, 0.52f, 0.50f, 1f);      // Cool mid-grey
            case PAVEMENT: return new Color(0.68f, 0.68f, 0.65f, 1f);   // Light warm grey
            case ROAD: return new Color(0.25f, 0.25f, 0.27f, 1f);       // Near-black asphalt
            case BRICK: return new Color(0.72f, 0.28f, 0.18f, 1f);      // Strong terracotta red
            case GLASS: return new Color(0.55f, 0.78f, 0.92f, 0.45f);   // Light blue tint
            case WOOD: return new Color(0.72f, 0.52f, 0.28f, 1f);       // Warm pine
            case WATER: return new Color(0.15f, 0.30f, 0.75f, 0.7f);    // Deep blue
            case TREE_TRUNK: return new Color(0.35f, 0.22f, 0.08f, 1f); // Dark bark brown
            case LEAVES: return new Color(0.18f, 0.48f, 0.12f, 1f);     // Forest green
            case CARDBOARD: return new Color(0.75f, 0.62f, 0.38f, 1f);  // Tan corrugated
            case IRON_FENCE: return new Color(0.18f, 0.18f, 0.20f, 1f); // Almost black iron
            case SIGN_WHITE: return new Color(0.95f, 0.95f, 0.92f, 1f);
            case SIGN_RED: return new Color(0.88f, 0.10f, 0.10f, 1f);
            case SIGN_BLUE: return new Color(0.10f, 0.18f, 0.68f, 1f);
            case SIGN_GREEN: return new Color(0.10f, 0.58f, 0.18f, 1f);
            case SIGN_YELLOW: return new Color(0.92f, 0.82f, 0.12f, 1f);
            case GARDEN_WALL: return new Color(0.58f, 0.50f, 0.42f, 1f); // Sandstone warm
            case CONCRETE: return new Color(0.62f, 0.62f, 0.58f, 1f);    // Cool grey, greenish tint
            case ROOF_TILE: return new Color(0.60f, 0.18f, 0.12f, 1f);   // Deep terracotta
            case TARMAC: return new Color(0.18f, 0.18f, 0.18f, 1f);      // Very dark
            case CORRUGATED_METAL: return new Color(0.48f, 0.52f, 0.55f, 1f); // Blue-grey steel
            case RENDER_WHITE: return new Color(0.92f, 0.90f, 0.86f, 1f);
            case RENDER_CREAM: return new Color(0.88f, 0.82f, 0.62f, 1f); // Warmer cream
            case SLATE: return new Color(0.30f, 0.32f, 0.38f, 1f);       // Dark blue-grey
            case PEBBLEDASH: return new Color(0.72f, 0.70f, 0.62f, 1f);  // Sandy/speckled
            case DOOR_WOOD: return new Color(0.42f, 0.25f, 0.10f, 1f);   // Dark oak
            case LINOLEUM: return new Color(0.58f, 0.52f, 0.42f, 1f);    // Beige floor
            case YELLOW_BRICK: return new Color(0.82f, 0.72f, 0.35f, 1f); // London stock brick
            case TILE_WHITE: return new Color(0.92f, 0.92f, 0.90f, 1f);  // Clean white
            case TILE_BLACK: return new Color(0.12f, 0.12f, 0.12f, 1f);  // Jet black
            case RENDER_PINK: return new Color(0.88f, 0.65f, 0.70f, 1f); // Rosy pink
            case METAL_RED: return new Color(0.78f, 0.12f, 0.08f, 1f);   // Pillar box red
            case COUNTER: return new Color(0.62f, 0.52f, 0.38f, 1f);     // Laminate brown
            case SHELF: return new Color(0.52f, 0.38f, 0.22f, 1f);       // Plywood brown
            case TABLE: return new Color(0.48f, 0.32f, 0.18f, 1f);       // Dark stained
            case CARPET: return new Color(0.52f, 0.18f, 0.18f, 1f);      // Deep burgundy
            case LINO_GREEN: return new Color(0.35f, 0.52f, 0.30f, 1f);  // Hospital green
            case BOOKSHELF: return new Color(0.38f, 0.28f, 0.12f, 1f);   // Dark walnut
            case BEDROCK: return new Color(0.15f, 0.15f, 0.15f, 1f);   // Very dark grey
            case WOOD_FENCE: return new Color(0.72f, 0.52f, 0.28f, 1f); // Same as WOOD
            case WOOD_WALL: return new Color(0.72f, 0.52f, 0.28f, 1f);  // Same as WOOD
            default: return new Color(1f, 1f, 1f, 1f);
        }
    }

    private Color buildTopColor() {
        switch (this) {
            case GRASS: return new Color(0.28f, 0.68f, 0.18f, 1f);       // Bright green top
            case DIRT: return new Color(0.52f, 0.35f, 0.15f, 1f);        // Dry earth
            case TREE_TRUNK: return new Color(0.48f, 0.32f, 0.12f, 1f);  // Wood rings
            case BRICK: return new Color(0.62f, 0.52f, 0.42f, 1f);       // Mortar top
            case YELLOW_BRICK: return new Color(0.78f, 0.70f, 0.45f, 1f);// Mortar top
            case STONE: return new Color(0.55f, 0.55f, 0.52f, 1f);       // Lighter stone top
            case CONCRETE: return new Color(0.65f, 0.65f, 0.60f, 1f);    // Lighter concrete
            case COUNTER: return new Color(0.72f, 0.68f, 0.58f, 1f);     // Lighter worktop
            case TABLE: return new Color(0.55f, 0.40f, 0.22f, 1f);       // Polished top
            case BOOKSHELF: return new Color(0.32f, 0.22f, 0.08f, 1f);   // Darker top
            case SHELF: return new Color(0.58f, 0.42f, 0.28f, 1f);       // Lighter shelf top
            case ROAD: return new Color(0.22f, 0.22f, 0.24f, 1f);        // Slightly lighter
            case PAVEMENT: return new Color(0.70f, 0.70f, 0.68f, 1f);    // Slightly lighter
            case ROOF_TILE: return new Color(0.55f, 0.15f, 0.10f, 1f);   // Darker ridge
            case SLATE: return new Color(0.28f, 0.30f, 0.35f, 1f);       // Darker slate top
            case CORRUGATED_METAL: return new Color(0.45f, 0.48f, 0.52f, 1f); // Weathered top
            case GARDEN_WALL: return new Color(0.55f, 0.48f, 0.38f, 1f); // Coping stone
            case CARPET: return new Color(0.48f, 0.15f, 0.15f, 1f);      // Slightly different pile
            case WOOD: return new Color(0.68f, 0.50f, 0.25f, 1f);        // End grain
            case CARDBOARD: return new Color(0.72f, 0.60f, 0.35f, 1f);   // Flap edge
            case WOOD_FENCE: return new Color(0.68f, 0.50f, 0.25f, 1f);  // End grain
            case WOOD_WALL: return new Color(0.68f, 0.50f, 0.25f, 1f);   // End grain
            default: return buildSideColor();
        }
    }

    private Color buildBottomColor() {
        switch (this) {
            case GRASS: return new Color(0.5f, 0.35f, 0.18f, 1f); // Earth
            case DIRT: return new Color(0.5f, 0.35f, 0.18f, 1f); // Darker earth
            default: return buildSideColor(); // Same as side
        }
    }
}

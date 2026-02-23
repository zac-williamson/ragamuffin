package ragamuffin.building;

import com.badlogic.gdx.graphics.Color;

/**
 * Material types that can be collected from breaking blocks.
 * These are items in the player's inventory.
 */
public enum Material {
    WOOD("Wood"),
    BRICK("Brick"),
    GLASS("Glass"),
    STONE("Stone"),
    DIAMOND("Diamond"),
    COMPUTER("Computer"),
    OFFICE_CHAIR("Office Chair"),
    STAPLER("Stapler"),
    GRASS_TURF("Grass Turf"),
    DIRT("Dirt"),
    PAVEMENT_SLAB("Pavement Slab"),
    ROAD_ASPHALT("Road Asphalt"),
    PLANKS("Planks"),
    SHELTER_WALL("Shelter Wall"),
    SHELTER_FLOOR("Shelter Floor"),
    SHELTER_ROOF("Shelter Roof"),
    BRICK_WALL("Brick Wall"),
    WINDOW("Window"),
    SAUSAGE_ROLL("Sausage Roll"),
    STEAK_BAKE("Steak Bake"),
    CARDBOARD("Cardboard"),
    IMPROVISED_TOOL("Improvised Tool"),
    STONE_TOOL("Stone Tool"),
    CHIPS("Chips"),
    KEBAB("Kebab"),
    ENERGY_DRINK("Energy Drink"),
    CRISPS("Crisps"),
    TIN_OF_BEANS("Tin of Beans"),
    CONCRETE("Concrete"),
    ROOF_TILE("Roof Tile"),
    TARMAC("Tarmac"),
    SCRAP_METAL("Scrap Metal"),
    RENDER("Render"),
    RENDER_CREAM("Cream Render"),
    RENDER_PINK("Pink Render"),
    SLATE("Slate"),
    PEBBLEDASH("Pebbledash"),
    DOOR("Door"),
    LINOLEUM("Linoleum"),
    LINO_GREEN("Green Lino"),
    YELLOW_BRICK("Yellow Brick"),
    TILE("Tile"),
    TILE_BLACK("Black Tile"),
    COUNTER("Counter"),
    SHELF("Shelf"),
    TABLE("Table"),
    CARPET("Carpet"),
    FENCE("Fence"),
    SIGN("Sign"),
    SIGN_RED("Red Sign"),
    SIGN_BLUE("Blue Sign"),
    SIGN_GREEN("Green Sign"),
    SIGN_YELLOW("Yellow Sign"),
    GARDEN_WALL("Garden Wall"),
    BOOKSHELF("Bookshelf"),
    METAL_RED("Red Metal"),
    PINT("Pint"),
    PERI_PERI_CHICKEN("Peri-Peri Chicken"),
    SCRATCH_CARD("Scratch Card"),
    NEWSPAPER("Newspaper"),
    WASHING_POWDER("Washing Powder"),
    PARACETAMOL("Paracetamol"),
    TEXTBOOK("Textbook"),
    HYMN_BOOK("Hymn Book"),
    PETROL_CAN("Petrol Can"),
    HAIR_CLIPPERS("Hair Clippers"),
    NAIL_POLISH("Nail Polish"),
    BROKEN_PHONE("Broken Phone"),
    DODGY_DVD("Dodgy DVD"),
    FIRE_EXTINGUISHER("Fire Extinguisher"),
    PLYWOOD("Plywood"),
    PIPE("Pipe"),
    CARDBOARD_BOX("Cardboard Box"),
    ANTIDEPRESSANTS("Antidepressants");

    private final String displayName;

    Material(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the icon color(s) for this material in the inventory UI.
     * Returns an array of 1 or 2 colors. A single color fills the whole slot;
     * two colors split the slot diagonally to represent materials with distinct
     * top/side appearance (e.g. grass has green top, brown side).
     */
    public Color[] getIconColors() {
        switch (this) {
            // Block materials — colors match BlockType
            case WOOD:           return c(0.72f, 0.52f, 0.28f);   // Warm pine
            case BRICK:          return c(0.72f, 0.28f, 0.18f);   // Terracotta red
            case BRICK_WALL:     return c(0.72f, 0.28f, 0.18f);
            case GLASS:          return c(0.55f, 0.78f, 0.92f, 0.7f); // Light blue
            case WINDOW:         return c(0.55f, 0.78f, 0.92f, 0.7f);
            case STONE:          return c(0.52f, 0.52f, 0.50f);   // Cool grey
            case GRASS_TURF:     return cs(0.28f, 0.68f, 0.18f,   // Green top
                                           0.45f, 0.35f, 0.20f);  // Brown side
            case DIRT:           return c(0.55f, 0.38f, 0.18f);   // Warm brown
            case PAVEMENT_SLAB:  return c(0.68f, 0.68f, 0.65f);   // Light grey
            case ROAD_ASPHALT:   return c(0.25f, 0.25f, 0.27f);   // Dark asphalt
            case PLANKS:         return c(0.72f, 0.52f, 0.28f);   // Same as wood
            case CARDBOARD:      return c(0.75f, 0.62f, 0.38f);   // Tan
            case CARDBOARD_BOX:  return c(0.75f, 0.62f, 0.38f);
            case CONCRETE:       return c(0.62f, 0.62f, 0.58f);   // Cool grey
            case ROOF_TILE:      return c(0.60f, 0.18f, 0.12f);   // Deep terracotta
            case TARMAC:         return c(0.18f, 0.18f, 0.18f);   // Very dark
            case SCRAP_METAL:    return c(0.48f, 0.52f, 0.55f);   // Blue-grey steel
            case RENDER:         return c(0.92f, 0.90f, 0.86f);   // White render
            case RENDER_CREAM:   return c(0.88f, 0.82f, 0.62f);   // Cream
            case RENDER_PINK:    return c(0.88f, 0.65f, 0.70f);   // Pink
            case SLATE:          return c(0.30f, 0.32f, 0.38f);   // Dark blue-grey
            case PEBBLEDASH:     return c(0.72f, 0.70f, 0.62f);   // Sandy
            case DOOR:           return c(0.42f, 0.25f, 0.10f);   // Dark oak
            case LINOLEUM:       return c(0.58f, 0.52f, 0.42f);   // Beige
            case LINO_GREEN:     return c(0.35f, 0.52f, 0.30f);   // Hospital green
            case YELLOW_BRICK:   return c(0.82f, 0.72f, 0.35f);   // London stock
            case TILE:           return c(0.92f, 0.92f, 0.90f);   // White tile
            case TILE_BLACK:     return c(0.12f, 0.12f, 0.12f);   // Black tile
            case COUNTER:        return c(0.62f, 0.52f, 0.38f);   // Laminate
            case SHELF:          return c(0.52f, 0.38f, 0.22f);   // Plywood
            case TABLE:          return c(0.48f, 0.32f, 0.18f);   // Dark stained
            case CARPET:         return c(0.52f, 0.18f, 0.18f);   // Burgundy
            case FENCE:          return c(0.18f, 0.18f, 0.20f);   // Iron black
            case SIGN:           return c(0.95f, 0.95f, 0.92f);
            case SIGN_RED:       return c(0.88f, 0.10f, 0.10f);
            case SIGN_BLUE:      return c(0.10f, 0.18f, 0.68f);
            case SIGN_GREEN:     return c(0.10f, 0.58f, 0.18f);
            case SIGN_YELLOW:    return c(0.92f, 0.82f, 0.12f);
            case GARDEN_WALL:    return c(0.58f, 0.50f, 0.42f);   // Sandstone
            case BOOKSHELF:      return c(0.38f, 0.28f, 0.12f);   // Dark walnut
            case METAL_RED:      return c(0.78f, 0.12f, 0.08f);   // Pillar box red
            case SHELTER_WALL:   return c(0.52f, 0.52f, 0.50f);
            case SHELTER_FLOOR:  return c(0.68f, 0.68f, 0.65f);
            case SHELTER_ROOF:   return c(0.48f, 0.52f, 0.55f);
            case PLYWOOD:        return c(0.80f, 0.68f, 0.40f);   // Light plywood
            case PIPE:           return c(0.55f, 0.55f, 0.60f);   // Metal pipe

            // Diamond — cyan/white sparkle
            case DIAMOND:        return cs(0.65f, 0.95f, 1.00f,
                                           0.40f, 0.85f, 0.90f);

            // Office items
            case COMPUTER:       return c(0.22f, 0.22f, 0.28f);   // Dark grey monitor
            case OFFICE_CHAIR:   return c(0.15f, 0.15f, 0.15f);   // Black chair
            case STAPLER:        return c(0.20f, 0.20f, 0.60f);   // Blue stapler

            // Food items
            case SAUSAGE_ROLL:   return c(0.80f, 0.55f, 0.20f);   // Golden pastry
            case STEAK_BAKE:     return c(0.75f, 0.42f, 0.15f);   // Darker pastry
            case CHIPS:          return c(0.90f, 0.78f, 0.30f);   // Golden chips
            case KEBAB:          return c(0.72f, 0.32f, 0.15f);   // Meat brown
            case CRISPS:         return c(0.88f, 0.78f, 0.20f);   // Yellow packet
            case TIN_OF_BEANS:   return cs(0.85f, 0.18f, 0.18f,   // Red tin
                                            0.92f, 0.60f, 0.15f); // Orange beans
            case ENERGY_DRINK:   return c(0.15f, 0.85f, 0.30f);   // Bright green can
            case PINT:           return c(0.78f, 0.62f, 0.18f);   // Golden ale
            case PERI_PERI_CHICKEN: return c(0.85f, 0.38f, 0.12f); // Spicy orange

            // Shop goods
            case SCRATCH_CARD:   return c(0.88f, 0.82f, 0.15f);   // Gold card
            case NEWSPAPER:      return c(0.88f, 0.84f, 0.72f);   // Newsprint grey
            case WASHING_POWDER: return c(0.70f, 0.78f, 0.95f);   // Blue box
            case PARACETAMOL:    return c(0.95f, 0.95f, 0.95f);   // White packet
            case TEXTBOOK:       return c(0.20f, 0.38f, 0.68f);   // Blue textbook
            case HYMN_BOOK:      return c(0.18f, 0.18f, 0.52f);   // Dark blue
            case PETROL_CAN:     return c(0.82f, 0.30f, 0.15f);   // Red can
            case HAIR_CLIPPERS:  return c(0.35f, 0.35f, 0.38f);   // Silver-grey
            case NAIL_POLISH:    return c(0.92f, 0.18f, 0.55f);   // Hot pink
            case BROKEN_PHONE:   return c(0.18f, 0.18f, 0.22f);   // Dark screen
            case DODGY_DVD:      return c(0.62f, 0.62f, 0.72f);   // Silver disc
            case FIRE_EXTINGUISHER: return c(0.82f, 0.12f, 0.10f); // Red cylinder
            case ANTIDEPRESSANTS: return c(0.85f, 0.85f, 0.92f);  // White/pale blue

            // Tools
            case IMPROVISED_TOOL: return cs(0.55f, 0.38f, 0.18f,  // Wood handle
                                             0.52f, 0.52f, 0.50f); // Stone head
            case STONE_TOOL:     return cs(0.52f, 0.52f, 0.50f,   // Stone head
                                            0.45f, 0.35f, 0.20f); // Wood handle

            default:             return c(0.5f, 0.5f, 0.5f);
        }
    }

    /** Make a single-color icon (opaque). */
    private static Color[] c(float r, float g, float b) {
        return new Color[]{new Color(r, g, b, 1f)};
    }

    /** Make a single-color icon with alpha. */
    private static Color[] c(float r, float g, float b, float a) {
        return new Color[]{new Color(r, g, b, a)};
    }

    /** Make a two-color icon (top-left and bottom-right halves). */
    private static Color[] cs(float r1, float g1, float b1, float r2, float g2, float b2) {
        return new Color[]{new Color(r1, g1, b1, 1f), new Color(r2, g2, b2, 1f)};
    }
}

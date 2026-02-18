package ragamuffin.building;

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
    SLATE("Slate"),
    PEBBLEDASH("Pebbledash"),
    DOOR("Door"),
    LINOLEUM("Linoleum"),
    YELLOW_BRICK("Yellow Brick"),
    TILE("Tile"),
    COUNTER("Counter"),
    SHELF("Shelf"),
    TABLE("Table"),
    CARPET("Carpet"),
    FENCE("Fence"),
    SIGN("Sign"),
    GARDEN_WALL("Garden Wall"),
    BOOKSHELF("Bookshelf");

    private final String displayName;

    Material(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

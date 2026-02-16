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
    WINDOW("Window");

    private final String displayName;

    Material(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

package ragamuffin.building;

import ragamuffin.world.BlockType;
import ragamuffin.world.LandmarkType;

/**
 * Defines what materials are dropped when blocks are broken.
 */
public class BlockDropTable {

    /**
     * Get the material dropped when a block is broken.
     * @param blockType The type of block broken
     * @param landmark The landmark this block belongs to (null if not part of a landmark)
     * @return The material dropped, or null if nothing is dropped
     */
    public Material getDrop(BlockType blockType, LandmarkType landmark) {
        // Special drops for landmark blocks
        if (landmark != null) {
            return getLandmarkDrop(blockType, landmark);
        }

        // Standard block drops
        switch (blockType) {
            case TREE_TRUNK:
                return Material.WOOD;
            case BRICK:
                return Material.BRICK;
            case GLASS:
                return Material.GLASS;
            case STONE:
                return Material.STONE;
            case GRASS:
                return Material.GRASS_TURF;
            case DIRT:
                return Material.DIRT;
            case PAVEMENT:
                return Material.PAVEMENT_SLAB;
            case ROAD:
                return Material.ROAD_ASPHALT;
            case WOOD:
                return Material.WOOD;
            case CARDBOARD:
                return Material.CARDBOARD;
            case CONCRETE:
                return Material.CONCRETE;
            case ROOF_TILE:
                return Material.ROOF_TILE;
            case TARMAC:
                return Material.TARMAC;
            case CORRUGATED_METAL:
            case METAL_RED:
                return Material.SCRAP_METAL;
            case RENDER_WHITE:
            case RENDER_CREAM:
            case RENDER_PINK:
                return Material.RENDER;
            case SLATE:
                return Material.SLATE;
            case PEBBLEDASH:
                return Material.PEBBLEDASH;
            case DOOR_WOOD:
                return Material.DOOR;
            case LINOLEUM:
            case LINO_GREEN:
                return Material.LINOLEUM;
            case YELLOW_BRICK:
                return Material.YELLOW_BRICK;
            case TILE_WHITE:
            case TILE_BLACK:
                return Material.TILE;
            case COUNTER:
                return Material.COUNTER;
            case SHELF:
            case BOOKSHELF:
                return Material.SHELF;
            case TABLE:
                return Material.TABLE;
            case CARPET:
                return Material.CARPET;
            case IRON_FENCE:
                return Material.FENCE;
            case SIGN_WHITE:
            case SIGN_RED:
            case SIGN_BLUE:
            case SIGN_GREEN:
            case SIGN_YELLOW:
                return Material.SIGN;
            case GARDEN_WALL:
                return Material.GARDEN_WALL;
            case AIR:
            case WATER:
            case LEAVES:
            default:
                return null; // No drop
        }
    }

    /**
     * Get special drops for blocks that are part of landmarks.
     */
    private Material getLandmarkDrop(BlockType blockType, LandmarkType landmark) {
        if (landmark == LandmarkType.JEWELLER) {
            // Jeweller blocks drop diamond
            if (blockType == BlockType.GLASS || blockType == BlockType.BRICK) {
                return Material.DIAMOND;
            }
        } else if (landmark == LandmarkType.OFFICE_BUILDING) {
            // Office building blocks drop office materials
            if (blockType == BlockType.BRICK) {
                return Material.COMPUTER;
            } else if (blockType == BlockType.GLASS) {
                return Material.OFFICE_CHAIR;
            }
        } else if (landmark == LandmarkType.GREGGS) {
            // Greggs blocks drop food (50/50 chance between sausage roll and steak bake)
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Math.random() < 0.5 ? Material.SAUSAGE_ROLL : Material.STEAK_BAKE;
            }
        } else if (landmark == LandmarkType.CHIPPY) {
            // Chippy drops chips
            if (blockType == BlockType.STONE || blockType == BlockType.BRICK) {
                return Material.CHIPS;
            }
        } else if (landmark == LandmarkType.KEBAB_SHOP) {
            // Kebab shop drops kebabs
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Material.KEBAB;
            }
        } else if (landmark == LandmarkType.OFF_LICENCE) {
            // Off-licence drops energy drinks and crisps
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                return Math.random() < 0.5 ? Material.ENERGY_DRINK : Material.CRISPS;
            }
        } else if (landmark == LandmarkType.TESCO_EXPRESS || landmark == LandmarkType.CORNER_SHOP) {
            // Supermarket/corner shop drops tinned food
            if (blockType == BlockType.BRICK || blockType == BlockType.GLASS) {
                double roll = Math.random();
                if (roll < 0.3) return Material.TIN_OF_BEANS;
                else if (roll < 0.6) return Material.CRISPS;
                else return Material.ENERGY_DRINK;
            }
        } else if (landmark == LandmarkType.CHARITY_SHOP) {
            // Charity shop drops cardboard
            if (blockType == BlockType.BRICK || blockType == BlockType.WOOD) {
                return Material.CARDBOARD;
            }
        }

        // If no special landmark drop, fall back to standard drop
        return getDrop(blockType, null);
    }
}

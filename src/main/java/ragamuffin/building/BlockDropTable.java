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
        } else if (landmark == LandmarkType.OFF_LICENCE ||
                   landmark == LandmarkType.CHARITY_SHOP) {
            // Shop blocks near shops drop cardboard
            if (blockType == BlockType.BRICK || blockType == BlockType.WOOD) {
                return Material.CARDBOARD;
            }
        }

        // If no special landmark drop, fall back to standard drop
        return getDrop(blockType, null);
    }
}

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
        }

        // If no special landmark drop, fall back to standard drop
        return getDrop(blockType, null);
    }
}

package ragamuffin.building;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.world.BlockType;
import ragamuffin.world.Raycast;
import ragamuffin.world.RaycastResult;
import ragamuffin.world.World;

/**
 * Handles placing blocks in the world.
 */
public class BlockPlacer {

    /**
     * Get the position where a block should be placed based on raycasting.
     * Returns the position adjacent to the hit block face.
     * @return the placement position, or null if no valid placement
     */
    public Vector3 getPlacementPosition(World world, Vector3 origin, Vector3 direction, float maxDistance) {
        RaycastResult result = Raycast.cast(world, origin, direction, maxDistance);
        if (result == null) {
            return null;
        }

        // Place on the face that was hit (adjacent to the block)
        int x = result.getBlockX();
        int y = result.getBlockY();
        int z = result.getBlockZ();

        // Determine which face was hit based on hit position
        Vector3 hitPos = result.getHitPosition();
        Vector3 blockCenter = new Vector3(x + 0.5f, y + 0.5f, z + 0.5f);
        Vector3 toHit = new Vector3(hitPos).sub(blockCenter);

        // Find the dominant axis
        int placeX = x;
        int placeY = y;
        int placeZ = z;

        float absX = Math.abs(toHit.x);
        float absY = Math.abs(toHit.y);
        float absZ = Math.abs(toHit.z);

        if (absX > absY && absX > absZ) {
            // X face
            placeX += (toHit.x > 0) ? 1 : -1;
        } else if (absY > absZ) {
            // Y face
            placeY += (toHit.y > 0) ? 1 : -1;
        } else {
            // Z face
            placeZ += (toHit.z > 0) ? 1 : -1;
        }

        // Check if placement position is air
        if (world.getBlock(placeX, placeY, placeZ) != BlockType.AIR) {
            return null;
        }

        return new Vector3(placeX, placeY, placeZ);
    }

    /**
     * Attempt to place a block from the inventory.
     * @return true if the block was placed successfully
     */
    public boolean placeBlock(World world, Inventory inventory, Material material, Vector3 origin, Vector3 direction, float maxDistance) {
        if (material == null) {
            return false;
        }

        if (!inventory.hasItem(material)) {
            return false;
        }

        Vector3 placement = getPlacementPosition(world, origin, direction, maxDistance);
        if (placement == null) {
            return false;
        }

        // Convert material to block type
        BlockType blockType = materialToBlockType(material);

        // Place the block
        world.setBlock((int) placement.x, (int) placement.y, (int) placement.z, blockType);

        // Remove from inventory
        inventory.removeItem(material, 1);

        return true;
    }

    /**
     * Convert a material to a block type for placement.
     */
    public BlockType materialToBlockType(Material material) {
        switch (material) {
            case PLANKS:
            case SHELTER_FLOOR:
            case SHELTER_ROOF:
                return BlockType.WOOD;
            case SHELTER_WALL:
            case BRICK_WALL:
            case BRICK:
                return BlockType.BRICK;
            case WINDOW:
            case GLASS:
                return BlockType.GLASS;
            case STONE:
                return BlockType.STONE;
            case WOOD:
                return BlockType.TREE_TRUNK;
            case DIRT:
                return BlockType.DIRT;
            case GRASS_TURF:
                return BlockType.GRASS;
            case PAVEMENT_SLAB:
                return BlockType.PAVEMENT;
            case ROAD_ASPHALT:
                return BlockType.ROAD;
            default:
                return BlockType.STONE; // Default fallback
        }
    }
}

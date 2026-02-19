package ragamuffin.building;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.entity.AABB;
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
        return placeBlock(world, inventory, material, origin, direction, maxDistance, null);
    }

    /**
     * Attempt to place a block from the inventory, checking against player AABB.
     * @param playerAABB if non-null, prevents placement inside the player's bounding box
     * @return true if the block was placed successfully
     */
    public boolean placeBlock(World world, Inventory inventory, Material material, Vector3 origin, Vector3 direction, float maxDistance, AABB playerAABB) {
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

        // Prevent placing a block inside the player
        if (playerAABB != null) {
            int bx = (int) Math.floor(placement.x);
            int by = (int) Math.floor(placement.y);
            int bz = (int) Math.floor(placement.z);
            AABB blockAABB = new AABB(bx, by, bz, bx + 1, by + 1, bz + 1);
            if (playerAABB.intersects(blockAABB)) {
                return false;
            }
            // Extra safety: check if block overlaps any grid cell the player occupies
            int pMinX = (int) Math.floor(playerAABB.getMinX());
            int pMaxX = (int) Math.floor(playerAABB.getMaxX());
            int pMinY = (int) Math.floor(playerAABB.getMinY());
            int pMaxY = (int) Math.ceil(playerAABB.getMaxY());
            int pMinZ = (int) Math.floor(playerAABB.getMinZ());
            int pMaxZ = (int) Math.floor(playerAABB.getMaxZ());
            if (bx >= pMinX && bx <= pMaxX && by >= pMinY && by <= pMaxY && bz >= pMinZ && bz <= pMaxZ) {
                return false;
            }
        }

        // Special case: CARDBOARD_BOX auto-builds a 2x2x2 shelter structure
        if (material == Material.CARDBOARD_BOX) {
            int ox = (int) Math.floor(placement.x);
            int oy = (int) Math.floor(placement.y);
            int oz = (int) Math.floor(placement.z);
            buildCardboardShelter(world, ox, oy, oz);
            inventory.removeItem(material, 1);
            return true;
        }

        // Convert material to block type — non-placeable items return null
        BlockType blockType = materialToBlockType(material);
        if (blockType == null) {
            return false;
        }

        // Place the block
        world.setBlock((int) Math.floor(placement.x), (int) Math.floor(placement.y), (int) Math.floor(placement.z), blockType);

        // Remove from inventory
        inventory.removeItem(material, 1);

        return true;
    }

    /**
     * Build a 2x2x2 cardboard shelter structure at the given origin position.
     * Structure layout (2 wide, 2 tall, 2 deep):
     *  - Floor: 2x2 cardboard at y=oy
     *  - Walls: 4 sides at y=oy and y=oy+1
     *  - Roof: 2x2 cardboard at y=oy+2
     * The front face (z+1) is left open as an entrance.
     */
    public void buildCardboardShelter(World world, int ox, int oy, int oz) {
        // Floor (2x2 at ground level)
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                setIfAir(world, ox + dx, oy, oz + dz, BlockType.CARDBOARD);
            }
        }

        // Walls — two height levels
        for (int dy = 1; dy <= 2; dy++) {
            // Back wall (z side, dz=0)
            setIfAir(world, ox,     oy + dy, oz,     BlockType.CARDBOARD);
            setIfAir(world, ox + 1, oy + dy, oz,     BlockType.CARDBOARD);
            // Left wall (x side, dx=0)
            setIfAir(world, ox,     oy + dy, oz,     BlockType.CARDBOARD);
            setIfAir(world, ox,     oy + dy, oz + 1, BlockType.CARDBOARD);
            // Right wall (x side, dx=1)
            setIfAir(world, ox + 1, oy + dy, oz,     BlockType.CARDBOARD);
            setIfAir(world, ox + 1, oy + dy, oz + 1, BlockType.CARDBOARD);
            // Front wall lower half only (dx=0 and dx=1 at dz=1) — leave open as entrance at dy=1
            if (dy == 2) {
                setIfAir(world, ox,     oy + dy, oz + 1, BlockType.CARDBOARD);
                setIfAir(world, ox + 1, oy + dy, oz + 1, BlockType.CARDBOARD);
            }
        }

        // Roof (2x2 at y=oy+3)
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                setIfAir(world, ox + dx, oy + 3, oz + dz, BlockType.CARDBOARD);
            }
        }
    }

    private void setIfAir(World world, int x, int y, int z, BlockType type) {
        if (world.getBlock(x, y, z) == BlockType.AIR) {
            world.setBlock(x, y, z, type);
        }
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
                return BlockType.WOOD;
            case DIRT:
                return BlockType.DIRT;
            case GRASS_TURF:
                return BlockType.GRASS;
            case PAVEMENT_SLAB:
                return BlockType.PAVEMENT;
            case ROAD_ASPHALT:
                return BlockType.ROAD;
            case CARDBOARD:
                return BlockType.CARDBOARD;
            case CONCRETE:
                return BlockType.CONCRETE;
            case ROOF_TILE:
                return BlockType.ROOF_TILE;
            case TARMAC:
                return BlockType.TARMAC;
            case SCRAP_METAL:
                return BlockType.CORRUGATED_METAL;
            case RENDER:
                return BlockType.RENDER_WHITE;
            case RENDER_CREAM:
                return BlockType.RENDER_CREAM;
            case RENDER_PINK:
                return BlockType.RENDER_PINK;
            case SLATE:
                return BlockType.SLATE;
            case PEBBLEDASH:
                return BlockType.PEBBLEDASH;
            case DOOR:
                return BlockType.DOOR_WOOD;
            case LINOLEUM:
                return BlockType.LINOLEUM;
            case LINO_GREEN:
                return BlockType.LINO_GREEN;
            case YELLOW_BRICK:
                return BlockType.YELLOW_BRICK;
            case TILE:
                return BlockType.TILE_WHITE;
            case TILE_BLACK:
                return BlockType.TILE_BLACK;
            case COUNTER:
                return BlockType.COUNTER;
            case SHELF:
                return BlockType.SHELF;
            case BOOKSHELF:
                return BlockType.BOOKSHELF;
            case TABLE:
                return BlockType.TABLE;
            case CARPET:
                return BlockType.CARPET;
            case FENCE:
                return BlockType.IRON_FENCE;
            case SIGN:
                return BlockType.SIGN_WHITE;
            case SIGN_RED:
                return BlockType.SIGN_RED;
            case SIGN_BLUE:
                return BlockType.SIGN_BLUE;
            case SIGN_GREEN:
                return BlockType.SIGN_GREEN;
            case SIGN_YELLOW:
                return BlockType.SIGN_YELLOW;
            case METAL_RED:
                return BlockType.METAL_RED;
            case GARDEN_WALL:
                return BlockType.GARDEN_WALL;
            default:
                return null; // Non-placeable items (food, tools, etc.)
        }
    }
}

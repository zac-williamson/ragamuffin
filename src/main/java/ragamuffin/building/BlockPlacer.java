package ragamuffin.building;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.entity.AABB;
import ragamuffin.world.BlockType;
import ragamuffin.world.PropPosition;
import ragamuffin.world.PropType;
import ragamuffin.world.Raycast;
import ragamuffin.world.RaycastResult;
import ragamuffin.world.World;

/**
 * Handles placing blocks in the world.
 */
public class BlockPlacer {

    private BlockBreaker blockBreaker;

    public BlockPlacer() {
    }

    public BlockPlacer(BlockBreaker blockBreaker) {
        this.blockBreaker = blockBreaker;
    }

    public void setBlockBreaker(BlockBreaker blockBreaker) {
        this.blockBreaker = blockBreaker;
    }

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

        // Special case: DOOR places a two-block-tall door (DOOR_LOWER + DOOR_UPPER)
        if (material == Material.DOOR) {
            int dx = (int) Math.floor(placement.x);
            int dy = (int) Math.floor(placement.y);
            int dz = (int) Math.floor(placement.z);
            // Require the block above to also be AIR
            if (world.getBlock(dx, dy + 1, dz) != BlockType.AIR) {
                return false;
            }
            world.setPlayerBlock(dx, dy, dz, BlockType.DOOR_LOWER);
            world.setPlayerBlock(dx, dy + 1, dz, BlockType.DOOR_UPPER);
            if (blockBreaker != null) {
                blockBreaker.clearHits(dx, dy, dz);
                blockBreaker.clearHits(dx, dy + 1, dz);
            }
            inventory.removeItem(material, 1);
            return true;
        }

        // Convert material to block type — non-placeable items return null
        BlockType blockType = materialToBlockType(material);
        if (blockType == null) {
            return false;
        }

        // Place the block, recording it as player-placed for council enforcement
        int bx = (int) Math.floor(placement.x);
        int by = (int) Math.floor(placement.y);
        int bz = (int) Math.floor(placement.z);
        world.setPlayerBlock(bx, by, bz, blockType);
        if (blockBreaker != null) {
            blockBreaker.clearHits(bx, by, bz);
        }

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
        // Floor (3x3 at ground level for stability, but interior is 1x1)
        for (int dx = 0; dx <= 2; dx++) {
            for (int dz = 0; dz <= 2; dz++) {
                setIfAir(world, ox + dx, oy, oz + dz, BlockType.CARDBOARD);
            }
        }

        // Walls — two height levels (3x3 perimeter, leaving 1x1 interior at center)
        for (int dy = 1; dy <= 2; dy++) {
            // Back wall (z=oz)
            setIfAir(world, ox,     oy + dy, oz,     BlockType.CARDBOARD);
            setIfAir(world, ox + 1, oy + dy, oz,     BlockType.CARDBOARD);
            setIfAir(world, ox + 2, oy + dy, oz,     BlockType.CARDBOARD);
            // Left wall (x=ox)
            setIfAir(world, ox,     oy + dy, oz,     BlockType.CARDBOARD);
            setIfAir(world, ox,     oy + dy, oz + 1, BlockType.CARDBOARD);
            setIfAir(world, ox,     oy + dy, oz + 2, BlockType.CARDBOARD);
            // Right wall (x=ox+2)
            setIfAir(world, ox + 2, oy + dy, oz,     BlockType.CARDBOARD);
            setIfAir(world, ox + 2, oy + dy, oz + 1, BlockType.CARDBOARD);
            setIfAir(world, ox + 2, oy + dy, oz + 2, BlockType.CARDBOARD);
            // Front wall (z=oz+2) - corners only at both heights, leaving ox+1 open for 2-block entrance
            setIfAir(world, ox,     oy + dy, oz + 2, BlockType.CARDBOARD);
            // ox+1, oy+dy, oz+2 intentionally left as AIR (entrance — 2 blocks tall)
            setIfAir(world, ox + 2, oy + dy, oz + 2, BlockType.CARDBOARD);
        }

        // Roof (3x3 at y=oy+3)
        for (int dx = 0; dx <= 2; dx++) {
            for (int dz = 0; dz <= 2; dz++) {
                setIfAir(world, ox + dx, oy + 3, oz + dz, BlockType.CARDBOARD);
            }
        }
    }

    private void setIfAir(World world, int x, int y, int z, BlockType type) {
        if (world.getBlock(x, y, z) == BlockType.AIR) {
            world.setPlayerBlock(x, y, z, type);
            if (blockBreaker != null) {
                blockBreaker.clearHits(x, y, z);
            }
        }
    }

    /**
     * Place a small item on the surface of a block at the exact hit position,
     * without snapping to the voxel grid. The item lands on top of whichever
     * face of the block the ray hits.
     *
     * @param playerAABB if non-null, prevents placement within the player's bounding box
     * @return true if the small item was successfully placed
     */
    public boolean placeSmallItem(World world, Inventory inventory, Material material,
                                   Vector3 origin, Vector3 direction, float maxDistance,
                                   AABB playerAABB) {
        if (material == null || !material.isSmallItem()) {
            return false;
        }

        if (!inventory.hasItem(material)) {
            return false;
        }

        RaycastResult result = ragamuffin.world.Raycast.cast(world, origin, direction, maxDistance);
        if (result == null) {
            return false;
        }

        // Use the exact hit position on the block surface (no grid snapping).
        // Snap the Y axis to the top face of the block so the item sits flat on it.
        int blockY = result.getBlockY();
        Vector3 hitPos = result.getHitPosition();

        // Determine which face was hit
        Vector3 blockCenter = new Vector3(result.getBlockX() + 0.5f, blockY + 0.5f, result.getBlockZ() + 0.5f);
        Vector3 toHit = new Vector3(hitPos).sub(blockCenter);

        float absX = Math.abs(toHit.x);
        float absY = Math.abs(toHit.y);
        float absZ = Math.abs(toHit.z);

        // Only allow placement on the top face (Y dominant and positive)
        if (!(absY >= absX && absY >= absZ && toHit.y > 0)) {
            return false;
        }

        // Place item at the exact X/Z hit position, on top of the block's surface (Y = blockY + 1)
        float itemX = hitPos.x;
        float itemY = blockY + 1.0f;
        float itemZ = hitPos.z;

        // Clamp X/Z within the block boundaries to prevent floating off the edge
        itemX = Math.max(result.getBlockX() + 0.05f, Math.min(result.getBlockX() + 0.95f, itemX));
        itemZ = Math.max(result.getBlockZ() + 0.05f, Math.min(result.getBlockZ() + 0.95f, itemZ));

        // Prevent placing inside the player's bounding box
        if (playerAABB != null &&
                itemX >= playerAABB.getMinX() && itemX <= playerAABB.getMaxX() &&
                itemY >= playerAABB.getMinY() && itemY <= playerAABB.getMaxY() &&
                itemZ >= playerAABB.getMinZ() && itemZ <= playerAABB.getMaxZ()) {
            return false;
        }

        world.placeSmallItem(new SmallItem(material, new Vector3(itemX, itemY, itemZ)));
        inventory.removeItem(material, 1);
        return true;
    }

    /**
     * Convert a PROP_* material to its corresponding PropType for placement.
     * Returns null for materials that are not placeable props.
     *
     * Fix #887: Disco ball and other craftable props could not be placed.
     */
    public PropType materialToPropType(Material material) {
        switch (material) {
            case PROP_BED:          return PropType.BED;
            case PROP_WORKBENCH:    return PropType.WORKBENCH;
            case PROP_DARTBOARD:    return PropType.SQUAT_DARTBOARD;
            case PROP_SPEAKER_STACK: return PropType.SPEAKER_STACK;
            case PROP_DISCO_BALL:   return PropType.DISCO_BALL;
            case PROP_DJ_DECKS:     return PropType.DJ_DECKS;
            default:                return null;
        }
    }

    /**
     * Attempt to place a prop item from the inventory at the targeted surface.
     * The prop is positioned on top of the hit block face.
     *
     * @param playerAABB if non-null, prevents placement inside the player's bounding box
     * @param playerYaw  the player's current Y-axis rotation (degrees) so the prop faces the player
     * @return the PropType placed, or null if placement failed
     *
     * Fix #887: Disco ball and other craftable props could not be placed.
     */
    public PropType placeProp(World world, Inventory inventory, Material material,
                              Vector3 origin, Vector3 direction, float maxDistance,
                              AABB playerAABB, float playerYaw) {
        if (material == null) return null;

        PropType propType = materialToPropType(material);
        if (propType == null) return null;

        if (!inventory.hasItem(material)) return null;

        RaycastResult result = Raycast.cast(world, origin, direction, maxDistance);
        if (result == null) return null;

        // Determine which face was hit — only allow placement on the top face
        Vector3 hitPos = result.getHitPosition();
        int blockX = result.getBlockX();
        int blockY = result.getBlockY();
        int blockZ = result.getBlockZ();
        Vector3 blockCenter = new Vector3(blockX + 0.5f, blockY + 0.5f, blockZ + 0.5f);
        Vector3 toHit = new Vector3(hitPos).sub(blockCenter);

        float absX = Math.abs(toHit.x);
        float absY = Math.abs(toHit.y);
        float absZ = Math.abs(toHit.z);

        // Place on top face only
        if (!(absY >= absX && absY >= absZ && toHit.y > 0)) return null;

        // Prop base sits on top of the block surface
        float propX = blockX + 0.5f;
        float propY = blockY + 1.0f;
        float propZ = blockZ + 0.5f;

        // Prevent placing inside the player's bounding box
        if (playerAABB != null) {
            PropPosition candidate = new PropPosition(propX, propY, propZ, propType, playerYaw);
            if (playerAABB.intersects(candidate.getAABB())) {
                return null;
            }
        }

        world.addPropPosition(new PropPosition(propX, propY, propZ, propType, playerYaw));
        inventory.removeItem(material, 1);
        return propType;
    }

    /**
     * Convert a material to a block type for placement.
     */
    public BlockType materialToBlockType(Material material) {
        switch (material) {
            case PLANKS:
                return BlockType.WOOD_PLANKS;
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
            case STAIRS:
                return BlockType.STAIRS;
            case HALF_BLOCK:
                return BlockType.HALF_BLOCK;
            case LADDER:
                return BlockType.LADDER;
            default:
                return null; // Non-placeable items (food, tools, etc.)
        }
    }
}

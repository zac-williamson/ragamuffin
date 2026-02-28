package ragamuffin.world;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.building.SmallItem;
import ragamuffin.entity.AABB;
import ragamuffin.entity.DamageReason;
import ragamuffin.entity.Player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application;
import java.util.*;

/**
 * Manages the voxel world - chunk loading/unloading and world data access.
 */
public class World {
    private static final int RENDER_DISTANCE_DESKTOP = 8;
    private static final int RENDER_DISTANCE_WEB = 6;
    private static final int RENDER_DISTANCE = detectRenderDistance();
    private static final int WORLD_CHUNK_RADIUS = 15; // Half world size in chunks (480/2/16=15)

    private static int detectRenderDistance() {
        try {
            if (Gdx.app != null && Gdx.app.getType() == Application.ApplicationType.WebGL) {
                return RENDER_DISTANCE_WEB;
            }
        } catch (Exception ignored) {}
        return RENDER_DISTANCE_DESKTOP;
    }

    private final long seed;
    private final Map<String, Chunk> loadedChunks;
    private final Map<LandmarkType, Landmark> landmarks;
    private WorldGenerator generator;
    private final Set<String> policeTapedBlocks; // Blocks with police tape
    private final Set<String> protectedBlocks; // Blocks protected from breaking
    private final Set<String> planningNoticeBlocks; // Blocks with planning notices (Phase 7)
    private final Set<String> dirtyChunks; // Chunks needing mesh rebuild
    private final Set<String> openDoors; // Open door positions (world coords of DOOR_LOWER)
    private final Set<String> playerPlacedBlocks; // Positions where player has placed blocks
    private final List<SmallItem> smallItems; // Small items placed without grid snapping
    private final List<FlagPosition> flagPositions; // Issue #658: animated flag poles
    private final List<PropPosition> propPositions; // Issue #669: non-block 3D props

    public World(long seed) {
        this.seed = seed;
        this.loadedChunks = new HashMap<>();
        this.landmarks = new HashMap<>();
        this.policeTapedBlocks = new HashSet<>();
        this.protectedBlocks = new HashSet<>();
        this.planningNoticeBlocks = new HashSet<>();
        this.dirtyChunks = new HashSet<>();
        this.openDoors = new HashSet<>();
        this.playerPlacedBlocks = new HashSet<>();
        this.smallItems = new ArrayList<>();
        this.flagPositions = new ArrayList<>();
        this.propPositions = new ArrayList<>();
    }

    /**
     * Generate the world using the world generator.
     */
    public void generate() {
        generator = new WorldGenerator(seed);
        generator.generateWorld(this);
        // Mark all generated chunks dirty so the mesh builder processes them on startup
        dirtyChunks.addAll(loadedChunks.keySet());
    }

    /**
     * Get a block at world coordinates.
     */
    public BlockType getBlock(int x, int y, int z) {
        int chunkX = Math.floorDiv(x, Chunk.SIZE);
        int chunkZ = Math.floorDiv(z, Chunk.SIZE);
        int chunkY = Math.floorDiv(y, Chunk.HEIGHT);

        Chunk chunk = getChunk(chunkX, chunkY, chunkZ);
        if (chunk == null) {
            return BlockType.AIR;
        }

        int localX = Math.floorMod(x, Chunk.SIZE);
        int localY = Math.floorMod(y, Chunk.HEIGHT);
        int localZ = Math.floorMod(z, Chunk.SIZE);

        return chunk.getBlock(localX, localY, localZ);
    }

    /**
     * Set a block at world coordinates.
     */
    public void setBlock(int x, int y, int z, BlockType type) {
        int chunkX = Math.floorDiv(x, Chunk.SIZE);
        int chunkZ = Math.floorDiv(z, Chunk.SIZE);
        int chunkY = Math.floorDiv(y, Chunk.HEIGHT);

        Chunk chunk = getOrCreateChunk(chunkX, chunkY, chunkZ);

        int localX = Math.floorMod(x, Chunk.SIZE);
        int localY = Math.floorMod(y, Chunk.HEIGHT);
        int localZ = Math.floorMod(z, Chunk.SIZE);

        chunk.setBlock(localX, localY, localZ, type);
    }

    /**
     * Set a block at world coordinates, recording it as player-placed.
     * Player-placed blocks are tracked so the council enforcement system can
     * distinguish between player shelters and world-generated buildings.
     * If type is AIR, the block is removed from the player-placed set.
     */
    public void setPlayerBlock(int x, int y, int z, BlockType type) {
        setBlock(x, y, z, type);
        String key = x + "," + y + "," + z;
        if (type == BlockType.AIR) {
            playerPlacedBlocks.remove(key);
        } else {
            playerPlacedBlocks.add(key);
        }
    }

    /**
     * Check if a block position was placed by the player (not world-generated).
     */
    public boolean isPlayerPlaced(int x, int y, int z) {
        return playerPlacedBlocks.contains(x + "," + y + "," + z);
    }

    /**
     * Get the set of all player-placed block positions (as "x,y,z" keys).
     */
    public Set<String> getPlayerPlacedBlocks() {
        return playerPlacedBlocks;
    }

    /**
     * Place a small item at an exact world-space position (no grid snapping).
     * Small items sit on the surface of blocks rather than occupying a full voxel.
     */
    public void placeSmallItem(SmallItem item) {
        smallItems.add(item);
    }

    /**
     * Get all small items placed in the world.
     */
    public List<SmallItem> getSmallItems() {
        return Collections.unmodifiableList(smallItems);
    }

    /**
     * Remove the small item at the given index from the world.
     * Called when the player loots a small item from the ground.
     *
     * Issue #872: Make small 3D objects lootable.
     *
     * @param itemIndex index into {@link #getSmallItems()}
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public void removeSmallItem(int itemIndex) {
        smallItems.remove(itemIndex);
    }

    /**
     * Register an animated flag pole position.
     * Called by WorldGenerator when placing flag poles on buildings.
     */
    public void addFlagPosition(FlagPosition flag) {
        flagPositions.add(flag);
    }

    /**
     * Get all animated flag positions in the world.
     */
    public List<FlagPosition> getFlagPositions() {
        return Collections.unmodifiableList(flagPositions);
    }

    /**
     * Register a non-block-based 3D prop position.
     * Called by WorldGenerator when placing props in the world.
     *
     * Issue #669: Add unique non-block-based 3D models to the world.
     */
    public void addPropPosition(PropPosition prop) {
        propPositions.add(prop);
    }

    /**
     * Get all non-block-based 3D prop positions in the world.
     *
     * Issue #669: Add unique non-block-based 3D models to the world.
     */
    public List<PropPosition> getPropPositions() {
        return Collections.unmodifiableList(propPositions);
    }

    /**
     * Remove the prop at the given index from the world.
     * Called by {@link ragamuffin.building.PropBreaker} when a prop is destroyed.
     *
     * Issue #719: Add collision and destructibility to 3D objects.
     *
     * @param propIndex index into {@link #getPropPositions()}
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public void removeProp(int propIndex) {
        propPositions.remove(propIndex);
    }

    /**
     * Get a chunk at chunk coordinates, or null if not loaded.
     */
    public Chunk getChunk(int chunkX, int chunkY, int chunkZ) {
        String key = getChunkKey(chunkX, chunkY, chunkZ);
        return loadedChunks.get(key);
    }

    /**
     * Get or create a chunk at chunk coordinates.
     */
    public Chunk getOrCreateChunk(int chunkX, int chunkY, int chunkZ) {
        String key = getChunkKey(chunkX, chunkY, chunkZ);
        return loadedChunks.computeIfAbsent(key, k -> new Chunk(chunkX, chunkY, chunkZ));
    }

    /**
     * Check if a chunk is currently loaded.
     */
    public boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ) {
        String key = getChunkKey(chunkX, chunkY, chunkZ);
        return loadedChunks.containsKey(key);
    }

    /**
     * Update which chunks are loaded based on player position.
     * Returns the set of chunk keys that were unloaded, so callers can
     * dispose any associated renderer resources (e.g. GPU mesh models).
     */
    public Set<String> updateLoadedChunks(Vector3 playerPos) {
        int playerChunkX = Math.floorDiv((int) playerPos.x, Chunk.SIZE);
        int playerChunkZ = Math.floorDiv((int) playerPos.z, Chunk.SIZE);

        Set<String> chunksToKeep = new HashSet<>();

        // Load chunks within render distance (both ground-level and underground)
        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;

                // Load ground-level chunk (chunkY = 0) and underground chunk (chunkY = -1)
                for (int cy = -1; cy <= 0; cy++) {
                    String key = getChunkKey(chunkX, cy, chunkZ);
                    chunksToKeep.add(key);

                    // Generate chunk if not already loaded
                    if (!loadedChunks.containsKey(key)) {
                        Chunk chunk = new Chunk(chunkX, cy, chunkZ);
                        if (generator != null) {
                            generator.generateChunk(chunk, this);
                        }
                        loadedChunks.put(key, chunk);
                        dirtyChunks.add(key);
                    }
                }
            }
        }

        // Never unload chunks within the generated world bounds (they contain buildings)
        // World is 480 blocks = 30 chunks across, from chunk -15 to +14
        int worldChunkMin = -WORLD_CHUNK_RADIUS;
        int worldChunkMax = WORLD_CHUNK_RADIUS - 1;
        Set<String> unloadedKeys = new HashSet<>();
        loadedChunks.keySet().removeIf(key -> {
            if (!chunksToKeep.contains(key)) {
                // Parse chunk coords from key
                String[] parts = key.split(",");
                int cx = Integer.parseInt(parts[0]);
                int cy = Integer.parseInt(parts[1]);
                int cz = Integer.parseInt(parts[2]);
                // Keep chunks within the generated world (surface and underground)
                boolean inWorldBounds = cx >= worldChunkMin && cx <= worldChunkMax &&
                       cz >= worldChunkMin && cz <= worldChunkMax &&
                       cy >= -1 && cy <= 0;
                if (!inWorldBounds) {
                    unloadedKeys.add(key);
                    return true;
                }
            }
            return false;
        });
        // Also remove from dirtyChunks so we don't try to build meshes for unloaded chunks
        dirtyChunks.removeAll(unloadedKeys);
        return unloadedKeys;
    }

    /**
     * Get all currently loaded chunk keys.
     */
    public List<String> getLoadedChunkKeys() {
        return new ArrayList<>(loadedChunks.keySet());
    }

    /**
     * Get all loaded chunks.
     */
    public Collection<Chunk> getLoadedChunks() {
        return loadedChunks.values();
    }

    /**
     * Get chunks that have been loaded or modified since last cleared.
     */
    public List<Chunk> getDirtyChunks() {
        List<Chunk> dirty = new ArrayList<>();
        for (String key : dirtyChunks) {
            Chunk chunk = loadedChunks.get(key);
            if (chunk != null) {
                dirty.add(chunk);
            }
        }
        return dirty;
    }

    /**
     * Clear the dirty chunks set (after meshes have been rebuilt).
     */
    public void clearDirtyChunks() {
        dirtyChunks.clear();
    }

    /**
     * Mark a single chunk as clean (mesh rebuilt).
     */
    public void markChunkClean(Chunk chunk) {
        String key = getChunkKey(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
        dirtyChunks.remove(key);
    }

    /**
     * Mark a specific chunk dirty by chunk coordinates (needs mesh rebuild).
     */
    public void markChunkDirty(int chunkX, int chunkY, int chunkZ) {
        String key = getChunkKey(chunkX, chunkY, chunkZ);
        dirtyChunks.add(key);
    }

    /**
     * Mark the chunk containing world block (x, y, z) dirty, plus any neighbouring
     * chunks if the block sits on a chunk boundary. Use this when a block is changed
     * by a system that does not have access to RagamuffinGame.rebuildChunkAt().
     */
    public void markBlockDirty(int x, int y, int z) {
        int chunkX = Math.floorDiv(x, Chunk.SIZE);
        int chunkY = Math.floorDiv(y, Chunk.HEIGHT);
        int chunkZ = Math.floorDiv(z, Chunk.SIZE);
        markChunkDirty(chunkX, chunkY, chunkZ);
        int localX = Math.floorMod(x, Chunk.SIZE);
        int localY = Math.floorMod(y, Chunk.HEIGHT);
        int localZ = Math.floorMod(z, Chunk.SIZE);
        if (localX == 0) markChunkDirty(chunkX - 1, chunkY, chunkZ);
        if (localX == Chunk.SIZE - 1) markChunkDirty(chunkX + 1, chunkY, chunkZ);
        if (localY == 0) markChunkDirty(chunkX, chunkY - 1, chunkZ);
        if (localY == Chunk.HEIGHT - 1) markChunkDirty(chunkX, chunkY + 1, chunkZ);
        if (localZ == 0) markChunkDirty(chunkX, chunkY, chunkZ - 1);
        if (localZ == Chunk.SIZE - 1) markChunkDirty(chunkX, chunkY, chunkZ + 1);
    }

    /**
     * Register a landmark in the world.
     */
    public void addLandmark(Landmark landmark) {
        landmarks.put(landmark.getType(), landmark);
    }

    /**
     * Get a landmark by type.
     */
    public Landmark getLandmark(LandmarkType type) {
        return landmarks.get(type);
    }

    /**
     * Get all landmarks.
     */
    public Collection<Landmark> getAllLandmarks() {
        return landmarks.values();
    }

    /**
     * Get the landmark at a specific block position, if any.
     * @return the landmark, or null if the block is not part of a landmark
     */
    public LandmarkType getLandmarkAt(int x, int y, int z) {
        for (Landmark landmark : landmarks.values()) {
            if (landmark.contains(x, y, z)) {
                return landmark.getType();
            }
        }
        return null;
    }

    /**
     * Get the render distance in chunks.
     */
    public int getRenderDistance() {
        return RENDER_DISTANCE;
    }

    /**
     * Get chunk key for storage.
     */
    private String getChunkKey(int chunkX, int chunkY, int chunkZ) {
        return chunkX + "," + chunkY + "," + chunkZ;
    }

    /**
     * Move player with collision detection against the world.
     * This is a helper method that checks collision across multiple chunks.
     * Includes gravity and Y-axis collision handling.
     */
    /**
     * Check if the player is standing on solid ground.
     */
    public boolean isOnGround(Player player) {
        AABB aabb = player.getAABB();
        int minX = (int) Math.floor(aabb.getMinX());
        int maxX = (int) Math.floor(aabb.getMaxX());
        int minZ = (int) Math.floor(aabb.getMinZ());
        int maxZ = (int) Math.floor(aabb.getMaxZ());
        int belowFeetY = (int) Math.floor(aabb.getMinY()) - 1;

        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                if (isBlockSolid(bx, belowFeetY, bz)) {
                    float blockTop = belowFeetY + 1.0f;
                    if (Math.abs(aabb.getMinY() - blockTop) < 0.05f) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Get the block type directly under the player's feet for footstep sounds.
     */
    public BlockType getBlockUnderPlayer(Player player) {
        Vector3 pos = player.getPosition();
        int x = (int) Math.floor(pos.x);
        int z = (int) Math.floor(pos.z);
        int y = (int) Math.floor(pos.y) - 1; // Block below feet
        return getBlock(x, y, z);
    }

    // Reusable vectors to avoid per-frame allocation in physics
    private final Vector3 tmpOriginalPos = new Vector3();
    private final Vector3 tmpDesiredMove = new Vector3();
    private final Vector3 tmpZOnlyPos = new Vector3();
    private final Vector3 tmpResult = new Vector3();

    public Vector3 moveWithCollision(Player player, float dx, float dy, float dz, float delta) {
        return moveWithCollision(player, dx, dy, dz, delta, Player.MOVE_SPEED);
    }

    /**
     * Fix #202: Apply gravity and vertical collision independently of horizontal input.
     * Called unconditionally every frame from updatePlayingSimulation() so that gravity
     * continues to act even when a UI overlay (inventory/help/crafting) is open.
     */
    public void applyGravityAndVerticalCollision(Player player, float delta) {
        // Only apply gravity if not standing on solid ground
        boolean onGround = isOnGround(player);
        if (onGround && player.getVerticalVelocity() <= 0) {
            // Landing — check for fall damage
            float fallDamage = player.landAndGetFallDamage();
            if (fallDamage > 0) {
                player.damage(fallDamage, DamageReason.FALL);
            }
            player.resetVerticalVelocity();
        } else {
            // Airborne — track fall start
            if (player.getVerticalVelocity() < 0) {
                player.startFalling();
            }
            player.applyGravity(delta);
        }

        // Vertical movement (gravity)
        float verticalMove = player.getVerticalVelocity() * delta;
        player.getPosition().y += verticalMove;
        player.getAABB().setPosition(player.getPosition(), Player.WIDTH, Player.HEIGHT, Player.DEPTH);

        // Check for vertical collision
        if (checkWorldCollision(player)) {
            // Landed on ground or hit ceiling
            if (verticalMove < 0) {
                // Falling down - find ground below player's feet within XZ footprint
                AABB aabb = player.getAABB();
                int minX = (int) Math.floor(aabb.getMinX());
                int maxX = (int) Math.floor(aabb.getMaxX());
                int minZ = (int) Math.floor(aabb.getMinZ());
                int maxZ = (int) Math.floor(aabb.getMaxZ());
                int feetY = (int) Math.floor(aabb.getMinY());

                int highestSolidY = -64;
                for (int bx = minX; bx <= maxX; bx++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        for (int by = feetY; by >= feetY - 3; by--) {
                            if (isBlockSolid(bx, by, bz)) {
                                highestSolidY = Math.max(highestSolidY, by);
                                break;
                            }
                        }
                    }
                }
                if (highestSolidY > -64) {
                    player.getPosition().y = highestSolidY + 1.0f;
                }
                // Fall damage on landing
                float fallDmg = player.landAndGetFallDamage();
                if (fallDmg > 0) {
                    player.damage(fallDmg, DamageReason.FALL);
                }
                player.resetVerticalVelocity();
            } else {
                // Moving up - hit ceiling
                player.getPosition().y = (float) Math.floor(player.getPosition().y + Player.HEIGHT) - Player.HEIGHT;
                player.resetVerticalVelocity();
            }
            player.getAABB().setPosition(player.getPosition(), Player.WIDTH, Player.HEIGHT, Player.DEPTH);
        }
    }

    public Vector3 moveWithCollision(Player player, float dx, float dy, float dz, float delta, float speed) {
        tmpOriginalPos.set(player.getPosition());

        // Horizontal movement (X and Z) only — gravity and vertical collision are
        // applied separately via applyGravityAndVerticalCollision() every frame.
        tmpDesiredMove.set(0, 0, 0);
        if (dx != 0 || dz != 0) {
            tmpDesiredMove.set(dx, 0, dz).nor().scl(speed * delta);
        }

        // Try horizontal movement with sliding
        player.getPosition().add(tmpDesiredMove.x, 0, tmpDesiredMove.z);
        player.getAABB().setPosition(player.getPosition(), Player.WIDTH, Player.HEIGHT, Player.DEPTH);

        if (checkWorldCollision(player)) {
            // Collision - revert and try sliding
            player.getPosition().set(tmpOriginalPos);

            // Try X only
            player.getPosition().add(tmpDesiredMove.x, 0, 0);
            player.getAABB().setPosition(player.getPosition(), Player.WIDTH, Player.HEIGHT, Player.DEPTH);
            if (checkWorldCollision(player)) {
                player.getPosition().set(tmpOriginalPos);
            }

            // Try Z only
            tmpZOnlyPos.set(player.getPosition());
            player.getPosition().add(0, 0, tmpDesiredMove.z);
            player.getAABB().setPosition(player.getPosition(), Player.WIDTH, Player.HEIGHT, Player.DEPTH);
            if (checkWorldCollision(player)) {
                player.getPosition().set(tmpZOnlyPos);
            }

            player.getAABB().setPosition(player.getPosition(), Player.WIDTH, Player.HEIGHT, Player.DEPTH);
        }

        return tmpResult.set(player.getPosition()).sub(tmpOriginalPos);
    }

    /**
     * Check if the player's AABB collides with any solid blocks or props in the world.
     * Inlines intersection test to avoid per-block AABB allocation.
     *
     * Issue #719: prop collision added.
     */
    private boolean checkWorldCollision(Player player) {
        AABB aabb = player.getAABB();
        float aMinX = aabb.getMinX(), aMaxX = aabb.getMaxX();
        float aMinY = aabb.getMinY(), aMaxY = aabb.getMaxY();
        float aMinZ = aabb.getMinZ(), aMaxZ = aabb.getMaxZ();

        // ── Block collision ──────────────────────────────────────────────────
        int minX = (int) Math.floor(aMinX);
        int maxX = (int) Math.ceil(aMaxX);
        int minY = (int) Math.floor(aMinY);
        int maxY = (int) Math.ceil(aMaxY);
        int minZ = (int) Math.floor(aMinZ);
        int maxZ = (int) Math.ceil(aMaxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (isBlockSolid(x, y, z)) {
                        // Inline AABB intersection: block occupies [x, x+1] x [y, y+1] x [z, z+1]
                        if (aMinX < x + 1 && aMaxX > x &&
                            aMinY < y + 1 && aMaxY > y &&
                            aMinZ < z + 1 && aMaxZ > z) {
                            return true;
                        }
                    }
                }
            }
        }

        // ── Prop collision (Issue #719) ──────────────────────────────────────
        for (PropPosition prop : propPositions) {
            AABB propBox = prop.getAABB();
            if (aabb.intersects(propBox)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if an AABB collides with any solid blocks in the world.
     * Public version for NPC collision.
     */
    public boolean checkAABBCollision(AABB aabb) {
        float aMinX = aabb.getMinX(), aMaxX = aabb.getMaxX();
        float aMinY = aabb.getMinY(), aMaxY = aabb.getMaxY();
        float aMinZ = aabb.getMinZ(), aMaxZ = aabb.getMaxZ();

        int minX = (int) Math.floor(aMinX);
        int maxX = (int) Math.ceil(aMaxX);
        int minY = (int) Math.floor(aMinY);
        int maxY = (int) Math.ceil(aMaxY);
        int minZ = (int) Math.floor(aMinZ);
        int maxZ = (int) Math.ceil(aMaxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (isBlockSolid(x, y, z)) {
                        if (aMinX < x + 1 && aMaxX > x &&
                            aMinY < y + 1 && aMaxY > y &&
                            aMinZ < z + 1 && aMaxZ > z) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Add police tape to a block.
     */
    public void addPoliceTape(int x, int y, int z) {
        String key = getBlockKey(x, y, z);
        policeTapedBlocks.add(key);
        protectedBlocks.add(key); // Taped blocks are also protected
    }

    /**
     * Check if a block has police tape.
     */
    public boolean hasPoliceTape(int x, int y, int z) {
        String key = getBlockKey(x, y, z);
        return policeTapedBlocks.contains(key);
    }

    /**
     * Check if a block is protected from breaking.
     */
    public boolean isProtected(int x, int y, int z) {
        String key = getBlockKey(x, y, z);
        return protectedBlocks.contains(key);
    }

    /**
     * Remove police tape from a block.
     */
    public void removePoliceTape(int x, int y, int z) {
        String key = getBlockKey(x, y, z);
        policeTapedBlocks.remove(key);
        protectedBlocks.remove(key);
    }

    /**
     * Add planning notice to a block (Phase 7).
     */
    public void addPlanningNotice(int x, int y, int z) {
        String key = getBlockKey(x, y, z);
        planningNoticeBlocks.add(key);
    }

    /**
     * Check if a block has a planning notice (Phase 7).
     */
    public boolean hasPlanningNotice(int x, int y, int z) {
        String key = getBlockKey(x, y, z);
        return planningNoticeBlocks.contains(key);
    }

    /**
     * Remove planning notice from a block (Phase 7).
     */
    public void removePlanningNotice(int x, int y, int z) {
        String key = getBlockKey(x, y, z);
        planningNoticeBlocks.remove(key);
    }

    /**
     * Get block key for storage.
     */
    private String getBlockKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    /**
     * Toggle a 2-block tall door at the given position.
     * The position should be the DOOR_LOWER block.
     * Toggling swaps DOOR_LOWER/DOOR_UPPER with AIR (open) or restores them (close).
     * Marks the affected chunk(s) dirty for mesh rebuild.
     *
     * @param x world X of the DOOR_LOWER block
     * @param y world Y of the DOOR_LOWER block
     * @param z world Z of the DOOR_LOWER block
     */
    public void toggleDoor(int x, int y, int z) {
        String key = getBlockKey(x, y, z);
        if (openDoors.contains(key)) {
            // Close: mark door as closed (blocks remain; collision restored)
            openDoors.remove(key);
        } else {
            // Open: mark door as open (blocks remain as thin panels; collision bypassed)
            openDoors.add(key);
        }
        markBlockDirty(x, y, z);
        markBlockDirty(x, y + 1, z);
    }

    /**
     * Check whether the door at the given DOOR_LOWER position is currently open.
     */
    public boolean isDoorOpen(int x, int y, int z) {
        return openDoors.contains(getBlockKey(x, y, z));
    }

    /**
     * Check whether the block at the given position is solid for collision purposes.
     * Open door blocks (DOOR_LOWER / DOOR_UPPER) are passable even though the block
     * type itself is marked solid — the door panel has swung aside.
     */
    public boolean isBlockSolid(int x, int y, int z) {
        BlockType type = getBlock(x, y, z);
        if (!type.isSolid()) return false;
        if (type == BlockType.DOOR_LOWER) {
            return !openDoors.contains(getBlockKey(x, y, z));
        }
        if (type == BlockType.DOOR_UPPER) {
            // Resolve to DOOR_LOWER position (one block below)
            return !openDoors.contains(getBlockKey(x, y - 1, z));
        }
        return true;
    }

    /**
     * Check whether the block at the given position is part of a 2-block door
     * (either DOOR_LOWER or DOOR_UPPER).
     */
    public boolean isDoorBlock(int x, int y, int z) {
        BlockType type = getBlock(x, y, z);
        return type == BlockType.DOOR_LOWER || type == BlockType.DOOR_UPPER;
    }
}

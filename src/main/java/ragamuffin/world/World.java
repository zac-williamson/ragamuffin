package ragamuffin.world;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.entity.AABB;
import ragamuffin.entity.Player;

import java.util.*;

/**
 * Manages the voxel world - chunk loading/unloading and world data access.
 */
public class World {
    private static final int RENDER_DISTANCE = 6; // Chunks to load around player

    private final long seed;
    private final Map<String, Chunk> loadedChunks;
    private final Map<LandmarkType, Landmark> landmarks;
    private WorldGenerator generator;
    private final Set<String> policeTapedBlocks; // Blocks with police tape
    private final Set<String> protectedBlocks; // Blocks protected from breaking
    private final Set<String> planningNoticeBlocks; // Blocks with planning notices (Phase 7)
    private final Set<String> dirtyChunks; // Chunks needing mesh rebuild

    public World(long seed) {
        this.seed = seed;
        this.loadedChunks = new HashMap<>();
        this.landmarks = new HashMap<>();
        this.policeTapedBlocks = new HashSet<>();
        this.protectedBlocks = new HashSet<>();
        this.planningNoticeBlocks = new HashSet<>();
        this.dirtyChunks = new HashSet<>();
    }

    /**
     * Generate the world using the world generator.
     */
    public void generate() {
        generator = new WorldGenerator(seed);
        generator.generateWorld(this);
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
     */
    public void updateLoadedChunks(Vector3 playerPos) {
        int playerChunkX = Math.floorDiv((int) playerPos.x, Chunk.SIZE);
        int playerChunkZ = Math.floorDiv((int) playerPos.z, Chunk.SIZE);

        Set<String> chunksToKeep = new HashSet<>();

        // Load chunks within render distance
        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;

                // Only load ground-level chunks for now (chunkY = 0)
                String key = getChunkKey(chunkX, 0, chunkZ);
                chunksToKeep.add(key);

                // Generate chunk if not already loaded
                if (!loadedChunks.containsKey(key)) {
                    Chunk chunk = new Chunk(chunkX, 0, chunkZ);
                    if (generator != null) {
                        generator.generateChunk(chunk, this);
                    }
                    loadedChunks.put(key, chunk);
                    dirtyChunks.add(key);
                }
            }
        }

        // Unload chunks beyond render distance
        loadedChunks.keySet().removeIf(key -> !chunksToKeep.contains(key));
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
                if (getBlock(bx, belowFeetY, bz).isSolid()) {
                    float blockTop = belowFeetY + 1.0f;
                    if (Math.abs(aabb.getMinY() - blockTop) < 0.05f) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Reusable vectors to avoid per-frame allocation in physics
    private final Vector3 tmpOriginalPos = new Vector3();
    private final Vector3 tmpDesiredMove = new Vector3();
    private final Vector3 tmpZOnlyPos = new Vector3();
    private final Vector3 tmpResult = new Vector3();

    public Vector3 moveWithCollision(Player player, float dx, float dy, float dz, float delta) {
        tmpOriginalPos.set(player.getPosition());

        // Only apply gravity if not standing on solid ground
        boolean onGround = isOnGround(player);
        if (onGround && player.getVerticalVelocity() <= 0) {
            player.resetVerticalVelocity();
        } else {
            player.applyGravity(delta);
        }

        // Horizontal movement (X and Z)
        tmpDesiredMove.set(0, 0, 0);
        if (dx != 0 || dz != 0) {
            tmpDesiredMove.set(dx, 0, dz).nor().scl(Player.MOVE_SPEED * delta);
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
                            if (getBlock(bx, by, bz).isSolid()) {
                                highestSolidY = Math.max(highestSolidY, by);
                                break;
                            }
                        }
                    }
                }
                if (highestSolidY > -64) {
                    player.getPosition().y = highestSolidY + 1.0f;
                }
                player.resetVerticalVelocity();
            } else {
                // Moving up - hit ceiling
                player.getPosition().y = (float) Math.floor(player.getPosition().y + Player.HEIGHT) - Player.HEIGHT;
                player.resetVerticalVelocity();
            }
            player.getAABB().setPosition(player.getPosition(), Player.WIDTH, Player.HEIGHT, Player.DEPTH);
        }

        return tmpResult.set(player.getPosition()).sub(tmpOriginalPos);
    }

    /**
     * Check if the player's AABB collides with any solid blocks in the world.
     * Inlines intersection test to avoid per-block AABB allocation.
     */
    private boolean checkWorldCollision(Player player) {
        AABB aabb = player.getAABB();
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
                    if (getBlock(x, y, z).isSolid()) {
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
}

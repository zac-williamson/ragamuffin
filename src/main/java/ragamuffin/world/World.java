package ragamuffin.world;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.entity.AABB;
import ragamuffin.entity.Player;

import java.util.*;

/**
 * Manages the voxel world - chunk loading/unloading and world data access.
 */
public class World {
    private static final int RENDER_DISTANCE = 4; // Chunks to load around player

    private final long seed;
    private final Map<String, Chunk> loadedChunks;
    private final Map<LandmarkType, Landmark> landmarks;
    private WorldGenerator generator;

    public World(long seed) {
        this.seed = seed;
        this.loadedChunks = new HashMap<>();
        this.landmarks = new HashMap<>();
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
     */
    public Vector3 moveWithCollision(Player player, float dx, float dy, float dz, float delta) {
        Vector3 desiredMove = new Vector3(dx, dy, dz).nor().scl(Player.MOVE_SPEED * delta);
        Vector3 originalPos = new Vector3(player.getPosition());

        // Try full movement
        player.getPosition().add(desiredMove);
        player.getAABB().setPosition(player.getPosition(), Player.WIDTH, Player.HEIGHT, Player.DEPTH);

        if (checkWorldCollision(player)) {
            // Collision - revert and try sliding
            player.getPosition().set(originalPos);

            // Try X only
            player.getPosition().add(desiredMove.x, 0, 0);
            player.getAABB().setPosition(player.getPosition(), Player.WIDTH, Player.HEIGHT, Player.DEPTH);
            if (checkWorldCollision(player)) {
                player.getPosition().set(originalPos);
            }

            // Try Z only
            Vector3 zOnlyPos = new Vector3(player.getPosition());
            player.getPosition().add(0, 0, desiredMove.z);
            player.getAABB().setPosition(player.getPosition(), Player.WIDTH, Player.HEIGHT, Player.DEPTH);
            if (checkWorldCollision(player)) {
                player.getPosition().set(zOnlyPos);
            }

            player.getAABB().setPosition(player.getPosition(), Player.WIDTH, Player.HEIGHT, Player.DEPTH);
        }

        return new Vector3(player.getPosition()).sub(originalPos);
    }

    /**
     * Check if the player's AABB collides with any solid blocks in the world.
     */
    private boolean checkWorldCollision(Player player) {
        AABB aabb = player.getAABB();

        int minX = (int) Math.floor(aabb.getMinX());
        int maxX = (int) Math.ceil(aabb.getMaxX());
        int minY = (int) Math.floor(aabb.getMinY());
        int maxY = (int) Math.ceil(aabb.getMaxY());
        int minZ = (int) Math.floor(aabb.getMinZ());
        int maxZ = (int) Math.ceil(aabb.getMaxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockType block = getBlock(x, y, z);
                    if (block.isSolid()) {
                        AABB blockBox = new AABB(x, y, z, x + 1, y + 1, z + 1);
                        if (aabb.intersects(blockBox)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}

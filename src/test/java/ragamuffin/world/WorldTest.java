package ragamuffin.world;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.test.HeadlessTestHelper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for World chunk management.
 */
public class WorldTest {

    private World world;

    @BeforeEach
    public void setUp() {
        HeadlessTestHelper.initHeadless();
        world = new World(0);
    }

    @Test
    public void testGetAndSetBlock() {
        world.setBlock(5, 10, 7, BlockType.BRICK);
        assertEquals(BlockType.BRICK, world.getBlock(5, 10, 7));
    }

    @Test
    public void testGetBlockAcrossChunkBoundary() {
        // Set blocks in different chunks
        world.setBlock(15, 0, 15, BlockType.GRASS);
        world.setBlock(16, 0, 16, BlockType.STONE);

        assertEquals(BlockType.GRASS, world.getBlock(15, 0, 15));
        assertEquals(BlockType.STONE, world.getBlock(16, 0, 16));
    }

    @Test
    public void testNegativeCoordinates() {
        world.setBlock(-5, 0, -7, BlockType.DIRT);
        assertEquals(BlockType.DIRT, world.getBlock(-5, 0, -7));
    }

    @Test
    public void testChunkLoading() {
        Vector3 playerPos = new Vector3(0, 0, 0);
        world.updateLoadedChunks(playerPos);

        // Chunk at origin should be loaded
        assertTrue(world.isChunkLoaded(0, 0, 0));

        // Chunks within render distance should be loaded
        int renderDist = world.getRenderDistance();
        assertTrue(world.isChunkLoaded(renderDist, 0, 0));
        assertTrue(world.isChunkLoaded(0, 0, renderDist));
    }

    @Test
    public void testChunkUnloading() {
        // Load chunks far outside the generated world bounds
        int farChunk = 20; // Well beyond WORLD_CHUNK_RADIUS of 15
        int farPos = farChunk * Chunk.SIZE;
        world.updateLoadedChunks(new Vector3(farPos, 0, 0));
        assertTrue(world.isChunkLoaded(farChunk, 0, 0));

        // Move back to origin — the far chunk is outside world bounds and render distance
        world.updateLoadedChunks(new Vector3(0, 0, 0));

        // Far chunk should be unloaded (it's outside the world bounds)
        assertFalse(world.isChunkLoaded(farChunk, 0, 0));
    }

    @Test
    public void testAddAndGetLandmark() {
        Landmark landmark = new Landmark(LandmarkType.PARK, 10, 0, 10, 20, 5, 20);
        world.addLandmark(landmark);

        Landmark retrieved = world.getLandmark(LandmarkType.PARK);
        assertNotNull(retrieved);
        assertEquals(LandmarkType.PARK, retrieved.getType());
        assertEquals(10, retrieved.getPosition().x);
    }

    @Test
    public void testGetBlockReturnsAirWhenChunkNotLoaded() {
        // Don't load any chunks
        BlockType block = world.getBlock(1000, 0, 1000);
        assertEquals(BlockType.AIR, block);
    }
}

package ragamuffin.world;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RaycastTest {

    @Test
    void testRaycastHitsBlock() {
        World world = new World(12345);
        world.setBlock(0, 0, 0, BlockType.STONE);

        // Raycast from above, looking down
        Vector3 origin = new Vector3(0.5f, 5f, 0.5f);
        Vector3 direction = new Vector3(0, -1, 0);

        RaycastResult result = Raycast.cast(world, origin, direction, 10f);

        assertNotNull(result);
        assertEquals(0, result.getBlockX());
        assertEquals(0, result.getBlockY());
        assertEquals(0, result.getBlockZ());
        assertEquals(BlockType.STONE, result.getBlockType());
    }

    @Test
    void testRaycastMissesBlocks() {
        World world = new World(12345);
        // No blocks in the world

        Vector3 origin = new Vector3(0, 5, 0);
        Vector3 direction = new Vector3(0, -1, 0);

        RaycastResult result = Raycast.cast(world, origin, direction, 10f);

        assertNull(result); // No hit
    }

    @Test
    void testRaycastMaxDistance() {
        World world = new World(12345);
        world.setBlock(0, 0, 0, BlockType.BRICK);

        Vector3 origin = new Vector3(0.5f, 20f, 0.5f);
        Vector3 direction = new Vector3(0, -1, 0);

        // Max distance too short
        RaycastResult result = Raycast.cast(world, origin, direction, 5f);
        assertNull(result);

        // Max distance long enough
        result = Raycast.cast(world, origin, direction, 25f);
        assertNotNull(result);
        assertEquals(BlockType.BRICK, result.getBlockType());
    }

    @Test
    void testRaycastIgnoresAir() {
        World world = new World(12345);
        world.setBlock(0, 1, 0, BlockType.AIR);
        world.setBlock(0, 0, 0, BlockType.GRASS);

        Vector3 origin = new Vector3(0.5f, 5f, 0.5f);
        Vector3 direction = new Vector3(0, -1, 0);

        RaycastResult result = Raycast.cast(world, origin, direction, 10f);

        assertNotNull(result);
        assertEquals(0, result.getBlockY()); // Should hit GRASS at y=0, not AIR at y=1
        assertEquals(BlockType.GRASS, result.getBlockType());
    }

    @Test
    void testRaycastHorizontal() {
        World world = new World(12345);
        world.setBlock(5, 1, 0, BlockType.TREE_TRUNK);

        Vector3 origin = new Vector3(0f, 1.5f, 0.5f);
        Vector3 direction = new Vector3(1, 0, 0); // Look east

        RaycastResult result = Raycast.cast(world, origin, direction, 10f);

        assertNotNull(result);
        assertEquals(5, result.getBlockX());
        assertEquals(1, result.getBlockY());
        assertEquals(0, result.getBlockZ());
        assertEquals(BlockType.TREE_TRUNK, result.getBlockType());
    }

    @Test
    void testRaycastDiagonal() {
        World world = new World(12345);
        world.setBlock(3, 3, 3, BlockType.BRICK);

        Vector3 origin = new Vector3(0f, 0f, 0f);
        Vector3 direction = new Vector3(1, 1, 1).nor(); // Diagonal

        RaycastResult result = Raycast.cast(world, origin, direction, 10f);

        assertNotNull(result);
        assertEquals(3, result.getBlockX());
        assertEquals(3, result.getBlockY());
        assertEquals(3, result.getBlockZ());
    }

    @Test
    void testRaycastReturnsNearestBlock() {
        World world = new World(12345);
        world.setBlock(0, 1, 0, BlockType.GRASS);
        world.setBlock(0, 2, 0, BlockType.BRICK);
        world.setBlock(0, 3, 0, BlockType.STONE);

        Vector3 origin = new Vector3(0.5f, 0f, 0.5f);
        Vector3 direction = new Vector3(0, 1, 0); // Look up

        RaycastResult result = Raycast.cast(world, origin, direction, 10f);

        assertNotNull(result);
        assertEquals(1, result.getBlockY()); // Should hit nearest block first
        assertEquals(BlockType.GRASS, result.getBlockType());
    }
}

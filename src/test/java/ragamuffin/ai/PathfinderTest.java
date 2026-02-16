package ragamuffin.ai;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Pathfinder.
 */
class PathfinderTest {

    private World world;
    private Pathfinder pathfinder;

    @BeforeEach
    void setUp() {
        world = new World(12345);
        pathfinder = new Pathfinder();

        // Create a flat ground
        for (int x = 0; x < 50; x++) {
            for (int z = 0; z < 50; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    @Test
    void testStraightPath() {
        Vector3 start = new Vector3(10, 1, 10);
        Vector3 end = new Vector3(15, 1, 10);

        List<Vector3> path = pathfinder.findPath(world, start, end);

        assertNotNull(path);
        assertTrue(path.size() >= 2);
        // Path should start near start position
        assertTrue(path.get(0).dst(start) < 2.0f);
        // Path should end near end position
        assertTrue(path.get(path.size() - 1).dst(end) < 2.0f);
    }

    @Test
    void testPathAroundWall() {
        // Create a wall
        for (int z = 5; z <= 15; z++) {
            world.setBlock(15, 1, z, BlockType.BRICK);
            world.setBlock(15, 2, z, BlockType.BRICK);
        }

        Vector3 start = new Vector3(10, 1, 10);
        Vector3 end = new Vector3(20, 1, 10);

        List<Vector3> path = pathfinder.findPath(world, start, end);

        assertNotNull(path);
        assertTrue(path.size() > 2); // Should go around, not straight

        // Verify path doesn't go through the wall
        for (Vector3 waypoint : path) {
            int x = (int) Math.floor(waypoint.x);
            int y = (int) Math.floor(waypoint.y);
            int z = (int) Math.floor(waypoint.z);

            BlockType block = world.getBlock(x, y, z);
            assertFalse(block.isSolid(), "Path goes through solid block at " + x + "," + y + "," + z);
        }
    }

    @Test
    void testNoPathWhenBlocked() {
        // Create a complete enclosure
        for (int x = 14; x <= 16; x++) {
            for (int z = 14; z <= 16; z++) {
                for (int y = 1; y <= 3; y++) {
                    world.setBlock(x, y, z, BlockType.BRICK);
                }
            }
        }

        Vector3 start = new Vector3(10, 1, 10);
        Vector3 end = new Vector3(15, 1, 15); // Inside the enclosure

        List<Vector3> path = pathfinder.findPath(world, start, end);

        assertNull(path); // No path should be found
    }

    @Test
    void testPathUpHill() {
        // Create a hill
        for (int x = 12; x <= 18; x++) {
            world.setBlock(x, 1, 10, BlockType.GRASS);
        }
        for (int x = 13; x <= 17; x++) {
            world.setBlock(x, 2, 10, BlockType.GRASS);
        }
        world.setBlock(15, 3, 10, BlockType.GRASS);

        Vector3 start = new Vector3(10, 1, 10);
        Vector3 end = new Vector3(20, 1, 10);

        List<Vector3> path = pathfinder.findPath(world, start, end);

        assertNotNull(path);
        // Path should climb the hill
        boolean foundHigherY = false;
        for (Vector3 waypoint : path) {
            if (waypoint.y >= 2) {
                foundHigherY = true;
                break;
            }
        }
        assertTrue(foundHigherY, "Path should climb the hill");
    }

    @Test
    void testSameStartAndEnd() {
        Vector3 start = new Vector3(10, 1, 10);
        Vector3 end = new Vector3(10, 1, 10);

        List<Vector3> path = pathfinder.findPath(world, start, end);

        assertNotNull(path);
        assertEquals(1, path.size()); // Just the starting point
    }

    @Test
    void testDiagonalPath() {
        Vector3 start = new Vector3(10, 1, 10);
        Vector3 end = new Vector3(20, 1, 20);

        List<Vector3> path = pathfinder.findPath(world, start, end);

        assertNotNull(path);
        assertTrue(path.size() >= 2);
    }
}

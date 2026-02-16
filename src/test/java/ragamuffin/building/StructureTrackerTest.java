package ragamuffin.building;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StructureTracker.
 */
class StructureTrackerTest {

    private World world;
    private StructureTracker tracker;

    @BeforeEach
    void setUp() {
        world = new World(12345);
        tracker = new StructureTracker();

        // Create ground
        for (int x = -50; x < 50; x++) {
            for (int z = -50; z < 50; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    @Test
    void testSmallStructureNotDetected() {
        // Build 2x2x2 (8 blocks) - below threshold
        for (int x = 10; x < 12; x++) {
            for (int y = 1; y < 3; y++) {
                for (int z = 10; z < 12; z++) {
                    world.setBlock(x, y, z, BlockType.WOOD);
                }
            }
        }

        tracker.scanForStructures(world);

        assertEquals(0, tracker.getLargeStructures().size(),
                "Small structure should not be detected");
    }

    @Test
    void testLargeStructureDetected() {
        // Build 5x5x5 (125 blocks) - above threshold
        for (int x = 20; x < 25; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 20; z < 25; z++) {
                    world.setBlock(x, y, z, BlockType.WOOD);
                }
            }
        }

        tracker.scanForStructures(world);

        assertTrue(tracker.getLargeStructures().size() >= 1,
                "Large structure should be detected");
        assertEquals(125, tracker.getLargeStructures().get(0).getComplexity(),
                "Structure should have 125 blocks");
    }

    @Test
    void testBuilderCountScaling() {
        // Build small structure (5x5x5 = 125 blocks)
        for (int x = 30; x < 35; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 30; z < 35; z++) {
                    world.setBlock(x, y, z, BlockType.WOOD);
                }
            }
        }

        tracker.scanForStructures(world);
        assertEquals(1, tracker.getLargeStructures().size(),
                "Should find exactly 1 structure");
        StructureTracker.Structure smallStructure = tracker.getLargeStructures().get(0);
        int smallBuilderCount = tracker.calculateBuilderCount(smallStructure);

        // Clear the small structure
        for (int x = 30; x < 35; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 30; z < 35; z++) {
                    world.setBlock(x, y, z, BlockType.AIR);
                }
            }
        }

        // Build large structure (10x5x5 = 250 blocks)
        for (int x = 40; x < 50; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 40; z < 45; z++) {
                    world.setBlock(x, y, z, BlockType.WOOD);
                }
            }
        }

        tracker = new StructureTracker();
        tracker.scanForStructures(world);
        assertEquals(1, tracker.getLargeStructures().size(),
                "Should find exactly 1 structure");
        StructureTracker.Structure largeStructure = tracker.getLargeStructures().get(0);
        int largeBuilderCount = tracker.calculateBuilderCount(largeStructure);

        assertTrue(largeBuilderCount > smallBuilderCount,
                "Larger structure should spawn more builders. Small: " + smallBuilderCount +
                        " (125 blocks), Large: " + largeBuilderCount + " (250 blocks)");
    }
}

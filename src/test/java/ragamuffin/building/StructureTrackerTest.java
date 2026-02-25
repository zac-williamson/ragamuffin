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

        // Create ground (world-generated, not player-placed)
        for (int x = -100; x < 100; x++) {
            for (int z = -100; z < 100; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    @Test
    void testBrickStructureDetected() {
        // Build 5x5x5 (125 blocks) from BRICK - should be detected as player-placed
        for (int x = 20; x < 25; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 20; z < 25; z++) {
                    world.setPlayerBlock(x, y, z, BlockType.BRICK);
                }
            }
        }

        tracker.scanForStructures(world);

        assertTrue(tracker.getLargeStructures().size() >= 1,
                "BRICK structure should be detected");
        assertEquals(125, tracker.getLargeStructures().get(0).getComplexity(),
                "BRICK structure should have 125 blocks");
    }

    @Test
    void testStoneStructureDetected() {
        // Build 5x5x5 (125 blocks) from STONE - should be detected as player-placed
        for (int x = 30; x < 35; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 30; z < 35; z++) {
                    world.setPlayerBlock(x, y, z, BlockType.STONE);
                }
            }
        }

        tracker.scanForStructures(world);

        assertTrue(tracker.getLargeStructures().size() >= 1,
                "STONE structure should be detected");
    }

    @Test
    void testMixedMaterialStructureDetected() {
        // Build a mixed WOOD + GLASS + BRICK structure (125 blocks total)
        // WOOD frame + GLASS windows + BRICK base - all connected
        for (int x = 40; x < 45; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 40; z < 45; z++) {
                    BlockType material;
                    if (y == 1) {
                        material = BlockType.BRICK; // base
                    } else if (x == 40 || x == 44 || z == 40 || z == 44) {
                        material = BlockType.WOOD; // frame walls
                    } else {
                        material = BlockType.GLASS; // windows/interior
                    }
                    world.setPlayerBlock(x, y, z, material);
                }
            }
        }

        tracker.scanForStructures(world);

        assertTrue(tracker.getLargeStructures().size() >= 1,
                "Mixed-material structure should be detected");
        assertEquals(125, tracker.getLargeStructures().get(0).getComplexity(),
                "Mixed-material structure should count all 125 blocks");
    }

    @Test
    void testNaturalBlocksNotTriggerStructure() {
        // Place many natural blocks using setBlock (world-gen, not player-placed)
        // They should NOT trigger structure detection
        for (int x = -50; x < 50; x++) {
            for (int z = -50; z < 50; z++) {
                world.setBlock(x, 1, z, BlockType.PAVEMENT);
                world.setBlock(x, 2, z, BlockType.TREE_TRUNK);
                world.setBlock(x, 3, z, BlockType.LEAVES);
            }
        }

        tracker.scanForStructures(world);

        assertEquals(0, tracker.getLargeStructures().size(),
                "Natural blocks (PAVEMENT, TREE_TRUNK, LEAVES) should not trigger structure detection");
    }

    @Test
    void testWorldGenBrickDoesNotTriggerStructure() {
        // Place BRICK blocks via setBlock() to simulate world-generated buildings
        // These should NOT be detected as player structures
        for (int x = 10; x < 15; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 10; z < 15; z++) {
                    world.setBlock(x, y, z, BlockType.BRICK); // world-gen, not player-placed
                }
            }
        }

        tracker.scanForStructures(world);

        assertEquals(0, tracker.getLargeStructures().size(),
                "World-generated BRICK blocks should NOT trigger structure detection");
    }

    @Test
    void testCardboardStructureDetected() {
        // CARDBOARD shelter (10x2x7 = 140 blocks) - above threshold
        for (int x = 60; x < 70; x++) {
            for (int y = 1; y < 3; y++) {
                for (int z = 60; z < 67; z++) {
                    world.setPlayerBlock(x, y, z, BlockType.CARDBOARD);
                }
            }
        }

        tracker.scanForStructures(world);

        assertTrue(tracker.getLargeStructures().size() >= 1,
                "CARDBOARD structure should be detected");
    }

    @Test
    void testConcreteStructureDetected() {
        // CONCRETE structure (5x5x5 = 125 blocks)
        for (int x = 70; x < 75; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 70; z < 75; z++) {
                    world.setPlayerBlock(x, y, z, BlockType.CONCRETE);
                }
            }
        }

        tracker.scanForStructures(world);

        assertTrue(tracker.getLargeStructures().size() >= 1,
                "CONCRETE structure should be detected");
    }

    @Test
    void testSmallStructureNotDetected() {
        // Build 2x2x2 (8 blocks) - below threshold
        for (int x = 10; x < 12; x++) {
            for (int y = 1; y < 3; y++) {
                for (int z = 10; z < 12; z++) {
                    world.setPlayerBlock(x, y, z, BlockType.WOOD);
                }
            }
        }

        tracker.scanForStructures(world);

        assertEquals(0, tracker.getLargeStructures().size(),
                "Small structure should not be detected");
    }

    /**
     * Regression test for Issue #646: getLargeStructures() must use LARGE_STRUCTURE_THRESHOLD (50),
     * not SMALL_STRUCTURE_THRESHOLD (10). Structures with 10–49 blocks must be excluded.
     */
    @Test
    void testGetLargeStructuresExcludesMediumSizedStructures() {
        // Build a medium structure: 3x2x3 = 18 blocks (>= SMALL_STRUCTURE_THRESHOLD=10, < LARGE_STRUCTURE_THRESHOLD=50)
        // scanForStructures will add it (>=10 blocks), but getLargeStructures() must exclude it (<50 blocks)
        for (int x = 10; x < 13; x++) {
            for (int y = 1; y < 3; y++) {
                for (int z = 10; z < 13; z++) {
                    world.setPlayerBlock(x, y, z, BlockType.WOOD);
                }
            }
        }

        tracker.scanForStructures(world);

        // getStructures() should include the medium structure (>=10 blocks)
        assertEquals(1, tracker.getStructures().size(),
                "Medium structure (18 blocks) should appear in getStructures()");

        // getLargeStructures() must NOT include structures below LARGE_STRUCTURE_THRESHOLD (50)
        assertEquals(0, tracker.getLargeStructures().size(),
                "Medium structure (18 blocks) must NOT appear in getLargeStructures() — only structures >=50 blocks qualify");
    }

    @Test
    void testGetLargeStructuresIncludesStructuresAtExactThreshold() {
        // Build exactly 50 blocks (5x2x5 = 50) — should appear in getLargeStructures()
        for (int x = 50; x < 55; x++) {
            for (int y = 1; y < 3; y++) {
                for (int z = 50; z < 55; z++) {
                    world.setPlayerBlock(x, y, z, BlockType.BRICK);
                }
            }
        }

        tracker.scanForStructures(world);

        assertEquals(1, tracker.getLargeStructures().size(),
                "Structure with exactly 50 blocks should appear in getLargeStructures()");
        assertEquals(50, tracker.getLargeStructures().get(0).getComplexity(),
                "Structure at threshold should have 50 blocks");
    }

    @Test
    void testLargeStructureDetected() {
        // Build 5x5x5 (125 blocks) - above threshold
        for (int x = 20; x < 25; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 20; z < 25; z++) {
                    world.setPlayerBlock(x, y, z, BlockType.WOOD);
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
                    world.setPlayerBlock(x, y, z, BlockType.WOOD);
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
                    world.setPlayerBlock(x, y, z, BlockType.AIR);
                }
            }
        }

        // Build large structure (10x5x5 = 250 blocks)
        for (int x = 40; x < 50; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 40; z < 45; z++) {
                    world.setPlayerBlock(x, y, z, BlockType.WOOD);
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

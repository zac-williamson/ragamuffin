package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #657: Add statue 3D object.
 * Verifies that a stone statue exists in the park at world centre.
 */
public class Issue657StatueTest {

    private World world;

    @BeforeEach
    public void setUp() {
        HeadlessTestHelper.initHeadless();
        world = new World(0);
        world.generate();
    }

    /**
     * Verify that a STATUE landmark exists in the world.
     */
    @Test
    public void testStatueLandmarkExists() {
        Landmark statue = world.getLandmark(LandmarkType.STATUE);
        assertNotNull(statue, "A STATUE landmark should exist in the generated world");
    }

    /**
     * Verify that STATUE blocks are present near the park centre.
     * The statue plinth is placed at (4,1,4) to (6,1,6).
     */
    @Test
    public void testStatueBlocksExistInPark() {
        // The plinth base occupies a 3x3 area at y=1 starting from (4,4)
        boolean foundStatueBlock = false;
        for (int x = 4; x <= 6; x++) {
            for (int z = 4; z <= 6; z++) {
                if (world.getBlock(x, 1, z) == BlockType.STATUE) {
                    foundStatueBlock = true;
                    break;
                }
            }
            if (foundStatueBlock) break;
        }
        assertTrue(foundStatueBlock, "STATUE blocks should be present in the park plinth area (y=1)");
    }

    /**
     * Verify the statue is multi-block tall (has figure blocks above the plinth).
     * The figure (centre column) reaches from y=2 up to y=5.
     */
    @Test
    public void testStatueIsMultiBlockTall() {
        // Centre column of the statue at (5, y, 5)
        int statueBlockCount = 0;
        for (int y = 1; y <= 5; y++) {
            if (world.getBlock(5, y, 5) == BlockType.STATUE) {
                statueBlockCount++;
            }
        }
        assertTrue(statueBlockCount >= 4,
                "Statue centre column should have at least 4 STATUE blocks (pedestal + figure), found " + statueBlockCount);
    }

    /**
     * Verify the STATUE block type is solid (so it has collision).
     */
    @Test
    public void testStatueBlockIsSolid() {
        assertTrue(BlockType.STATUE.isSolid(), "STATUE block should be solid");
    }

    /**
     * Verify the STATUE block type is opaque (so adjacent faces are culled in mesh).
     */
    @Test
    public void testStatueBlockIsOpaque() {
        assertTrue(BlockType.STATUE.isOpaque(), "STATUE block should be opaque");
    }
}

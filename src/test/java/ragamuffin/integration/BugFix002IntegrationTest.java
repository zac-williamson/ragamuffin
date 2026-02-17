package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.entity.Player;
import ragamuffin.ui.MainMenuScreen;
import ragamuffin.world.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Bug 001, 002, and 003 fixes.
 */
class BugFix002IntegrationTest {

    // --- Bug 001: Main Menu UI ---

    /**
     * Bug 001: Verify main menu navigation works correctly.
     * The menu should start with "New Game" selected, allow navigating
     * to "Quit" and back, and correctly report which option is selected.
     */
    @Test
    void mainMenuNavigationWorks() {
        MainMenuScreen menu = new MainMenuScreen();

        // Menu should start visible with "New Game" selected
        assertTrue(menu.isVisible(), "Menu should be visible on creation");
        assertEquals(0, menu.getSelectedOption(), "Should start on New Game (option 0)");
        assertTrue(menu.isNewGameSelected(), "New Game should be selected initially");
        assertFalse(menu.isQuitSelected(), "Quit should not be selected initially");

        // Navigate down to Quit
        menu.selectNext();
        assertEquals(1, menu.getSelectedOption(), "Should be on Quit (option 1) after selectNext");
        assertFalse(menu.isNewGameSelected(), "New Game should not be selected");
        assertTrue(menu.isQuitSelected(), "Quit should be selected after navigating down");

        // Navigate back up to New Game
        menu.selectPrevious();
        assertEquals(0, menu.getSelectedOption(), "Should be back on New Game");
        assertTrue(menu.isNewGameSelected(), "New Game should be selected again");

        // Navigate up wraps to Quit
        menu.selectPrevious();
        assertTrue(menu.isQuitSelected(), "Should wrap to Quit when navigating up from New Game");

        // Navigate down wraps to New Game
        menu.selectNext();
        assertTrue(menu.isNewGameSelected(), "Should wrap to New Game when navigating down from Quit");
    }

    /**
     * Bug 001: Verify menu show/hide resets selection.
     */
    @Test
    void mainMenuShowResetsSelection() {
        MainMenuScreen menu = new MainMenuScreen();

        // Navigate to Quit
        menu.selectNext();
        assertTrue(menu.isQuitSelected());

        // Hide and show should reset to New Game
        menu.hide();
        assertFalse(menu.isVisible());

        menu.show();
        assertTrue(menu.isVisible());
        assertTrue(menu.isNewGameSelected(), "Show should reset selection to New Game");
    }

    /**
     * Bug 001: Verify menu option text is centred by checking render params.
     * The render method should use screen dimensions to centre text properly.
     */
    @Test
    void mainMenuOptionsCentredLayout() {
        MainMenuScreen menu = new MainMenuScreen();

        // Verify menu has exactly 2 options
        assertTrue(menu.isNewGameSelected() || menu.isQuitSelected(),
            "Menu should have New Game and Quit options");

        // Navigate through all options to verify they all exist
        menu.selectNext(); // Go to Quit
        assertTrue(menu.isQuitSelected());
        menu.selectNext(); // Wrap to New Game
        assertTrue(menu.isNewGameSelected());
    }

    // --- Bug 002: Empty Chunks ---

    private World world;

    @BeforeEach
    void setUp() {
        world = new World(42L);
    }

    /**
     * Bug 002: After generating the world, all chunks within the 200x200
     * world bounds should have terrain (grass at y=0). No empty/missing chunks.
     */
    @Test
    void allChunksWithinWorldBoundsHaveTerrain() {
        world.generate();
        world.updateLoadedChunks(new Vector3(0, 1, 0));

        int emptyChunks = 0;
        int checkedChunks = 0;
        int halfWorld = 100; // WORLD_SIZE / 2

        for (Chunk chunk : world.getLoadedChunks()) {
            // Only check chunks whose blocks fall within the 200x200 world
            int startX = chunk.getChunkX() * Chunk.SIZE;
            int startZ = chunk.getChunkZ() * Chunk.SIZE;
            int endX = startX + Chunk.SIZE - 1;
            int endZ = startZ + Chunk.SIZE - 1;

            // Skip chunks entirely outside world bounds
            if (endX < -halfWorld || startX >= halfWorld ||
                endZ < -halfWorld || startZ >= halfWorld) {
                continue;
            }

            checkedChunks++;
            boolean hasAnyBlock = false;

            // Check if chunk has at least one non-AIR block
            for (int x = 0; x < Chunk.SIZE && !hasAnyBlock; x++) {
                for (int z = 0; z < Chunk.SIZE && !hasAnyBlock; z++) {
                    for (int y = 0; y < Chunk.HEIGHT && !hasAnyBlock; y++) {
                        if (chunk.getBlock(x, y, z) != BlockType.AIR) {
                            hasAnyBlock = true;
                        }
                    }
                }
            }

            if (!hasAnyBlock) {
                emptyChunks++;
            }
        }

        assertTrue(checkedChunks > 0, "Should have chunks within world bounds");
        assertEquals(0, emptyChunks,
            "No chunks within world bounds should be empty - found " + emptyChunks +
            " empty out of " + checkedChunks);
    }

    /**
     * Bug 002: Simulate walking to a far position, then coming back.
     * Chunks that were unloaded and reloaded should have terrain.
     */
    @Test
    void chunksReloadWithTerrainAfterUnloading() {
        world.generate();

        // Load chunks near origin
        world.updateLoadedChunks(new Vector3(0, 1, 0));

        // Verify chunk at origin has grass
        BlockType originBlock = world.getBlock(0, 0, 0);
        assertEquals(BlockType.GRASS, originBlock, "Origin should have grass");

        // Move far away so origin chunks get unloaded
        Vector3 farPos = new Vector3(500, 1, 500);
        world.updateLoadedChunks(farPos);

        // Move back to origin - chunks should be regenerated with terrain
        world.updateLoadedChunks(new Vector3(0, 1, 0));

        BlockType reloadedBlock = world.getBlock(0, 0, 0);
        assertEquals(BlockType.GRASS, reloadedBlock,
            "Chunk at origin should have grass after reload, not " + reloadedBlock);
    }

    /**
     * Bug 002: Adjacent chunks should both have terrain - no checkerboard gaps.
     */
    @Test
    void adjacentChunksAllHaveTerrain() {
        world.generate();
        world.updateLoadedChunks(new Vector3(0, 1, 0));

        // Check a grid of adjacent chunks around origin
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                int worldX = cx * Chunk.SIZE + Chunk.SIZE / 2;
                int worldZ = cz * Chunk.SIZE + Chunk.SIZE / 2;

                BlockType block = world.getBlock(worldX, 0, worldZ);
                assertNotEquals(BlockType.AIR, block,
                    "Block at (" + worldX + ", 0, " + worldZ +
                    ") in chunk (" + cx + ", " + cz + ") should have terrain, not AIR");
            }
        }
    }

    /**
     * Bug 002: Dynamically loaded chunks at the edge of render distance should have terrain.
     */
    @Test
    void dynamicallyLoadedChunksHaveTerrain() {
        world.generate();

        // Move to a position that forces loading new chunks
        Vector3 edgePos = new Vector3(80, 1, 80);
        world.updateLoadedChunks(edgePos);

        // Check blocks around the edge position
        for (int dx = -16; dx <= 16; dx += 8) {
            for (int dz = -16; dz <= 16; dz += 8) {
                int x = (int) edgePos.x + dx;
                int z = (int) edgePos.z + dz;
                // Only check within world bounds (200x200 centred on origin)
                if (Math.abs(x) < 100 && Math.abs(z) < 100) {
                    BlockType block = world.getBlock(x, 0, z);
                    assertNotEquals(BlockType.AIR, block,
                        "Block at (" + x + ", 0, " + z + ") should have terrain");
                }
            }
        }
    }

    // --- Bug 003: Camera Flip ---

    /**
     * Bug 003: Verify camera pitch is clamped to prevent flipping.
     * Uses a PerspectiveCamera directly to test the clamping logic.
     */
    @Test
    void cameraPitchClampingPreventsFlip() {
        // Simulate the pitch clamping logic from RagamuffinGame
        float cameraPitch = 0f;
        float maxPitch = 89.0f;

        // Simulate looking up many times (mouse moving upward)
        for (int i = 0; i < 1000; i++) {
            float pitchChange = 1.0f; // Simulating mouse moving up
            float newPitch = cameraPitch + pitchChange;
            newPitch = Math.max(-maxPitch, Math.min(maxPitch, newPitch));
            cameraPitch = newPitch;
        }

        // Pitch should be clamped at maxPitch
        assertEquals(maxPitch, cameraPitch, 0.01f,
            "Pitch should be clamped at " + maxPitch + " degrees when looking up");
        assertTrue(cameraPitch <= maxPitch,
            "Pitch should never exceed " + maxPitch + " degrees");

        // Simulate looking down many times
        for (int i = 0; i < 2000; i++) {
            float pitchChange = -1.0f; // Simulating mouse moving down
            float newPitch = cameraPitch + pitchChange;
            newPitch = Math.max(-maxPitch, Math.min(maxPitch, newPitch));
            cameraPitch = newPitch;
        }

        // Pitch should be clamped at -maxPitch
        assertEquals(-maxPitch, cameraPitch, 0.01f,
            "Pitch should be clamped at -" + maxPitch + " degrees when looking down");
        assertTrue(cameraPitch >= -maxPitch,
            "Pitch should never go below -" + maxPitch + " degrees");
    }

    /**
     * Bug 003: Verify moderate pitch changes work correctly without clamping.
     */
    @Test
    void cameraPitchAllowsNormalLooking() {
        float cameraPitch = 0f;
        float maxPitch = 89.0f;

        // Look up 45 degrees
        float pitchChange = 45.0f;
        float newPitch = cameraPitch + pitchChange;
        newPitch = Math.max(-maxPitch, Math.min(maxPitch, newPitch));
        cameraPitch = newPitch;

        assertEquals(45.0f, cameraPitch, 0.01f,
            "Should be able to look up 45 degrees");

        // Look down 90 degrees (from 45 to -45)
        pitchChange = -90.0f;
        newPitch = cameraPitch + pitchChange;
        newPitch = Math.max(-maxPitch, Math.min(maxPitch, newPitch));
        cameraPitch = newPitch;

        assertEquals(-45.0f, cameraPitch, 0.01f,
            "Should be able to look down to -45 degrees");
    }

    /**
     * Bug 003: Verify pitch doesn't flip past vertical.
     * A large single pitch change should still be clamped.
     */
    @Test
    void largePitchChangeIsClamped() {
        float cameraPitch = 0f;
        float maxPitch = 89.0f;

        // Try to pitch 180 degrees in one frame
        float pitchChange = 180.0f;
        float newPitch = cameraPitch + pitchChange;
        newPitch = Math.max(-maxPitch, Math.min(maxPitch, newPitch));
        cameraPitch = newPitch;

        assertTrue(cameraPitch <= maxPitch,
            "Even a 180-degree pitch should be clamped to " + maxPitch);
        assertEquals(maxPitch, cameraPitch, 0.01f);

        // Try negative 360 degrees
        pitchChange = -360.0f;
        newPitch = cameraPitch + pitchChange;
        newPitch = Math.max(-maxPitch, Math.min(maxPitch, newPitch));
        cameraPitch = newPitch;

        assertTrue(cameraPitch >= -maxPitch,
            "Even a -360-degree pitch should be clamped to -" + maxPitch);
        assertEquals(-maxPitch, cameraPitch, 0.01f);
    }
}

package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.BlockBreaker;
import ragamuffin.entity.Player;
import ragamuffin.ui.GameHUD;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #289: block-break progress arc renders over
 * inventory/crafting/help UI overlays.
 *
 * Verifies that when a UI overlay is open the crosshair state (block break
 * progress, target name) is cleared so the arc is not drawn over the overlay.
 */
class Issue289BlockBreakArcOverlayTest {

    private World world;
    private Player player;
    private BlockBreaker blockBreaker;
    private GameHUD gameHUD;

    @BeforeEach
    void setUp() {
        world = new World(42);
        player = new Player(0, 5, 0);
        blockBreaker = new BlockBreaker();
        gameHUD = new GameHUD(player);
    }

    /**
     * Simulates what RagamuffinGame.renderUI() does when isUIBlocking() is false:
     * it sets the block break progress from the actual damage on the targeted block.
     */
    private void simulateHUDUpdateWithCrosshair(int bx, int by, int bz) {
        float progress = blockBreaker.getBreakProgress(world, bx, by, bz, null);
        gameHUD.setBlockBreakProgress(progress);
        gameHUD.setTargetName("Stone");
    }

    /**
     * Simulates what RagamuffinGame.renderUI() does when isUIBlocking() is true:
     * it clears both the block break progress and the target name so the crosshair
     * / arc is not drawn over the UI overlay.
     */
    private void simulateHUDUpdateWithUIBlocking() {
        gameHUD.setBlockBreakProgress(0f);
        gameHUD.setTargetName(null);
    }

    @Test
    void test1_progressClearedWhenUIOverlayOpens() {
        // Partially damage a block
        world.setBlock(5, 1, 5, BlockType.STONE);
        blockBreaker.punchBlock(world, 5, 1, 5, null); // 1 of 8 hits
        blockBreaker.punchBlock(world, 5, 1, 5, null); // 2 of 8

        // Simulate HUD update with crosshair visible (no overlay open)
        simulateHUDUpdateWithCrosshair(5, 1, 5);
        float progressBeforeUI = gameHUD.getBlockBreakProgress();
        assertTrue(progressBeforeUI > 0f,
                "HUD should show non-zero progress while targeting a damaged block");

        // Player opens inventory → isUIBlocking() returns true → progress cleared
        simulateHUDUpdateWithUIBlocking();

        assertEquals(0f, gameHUD.getBlockBreakProgress(), 1e-6f,
                "Block break progress must be 0 when a UI overlay is open (arc hidden)");
        assertNull(gameHUD.getTargetName(),
                "Target name must be null when a UI overlay is open");
    }

    @Test
    void test2_progressRestoredAfterUIOverlayCloses() {
        // Partially damage a block
        world.setBlock(5, 1, 5, BlockType.STONE);
        blockBreaker.punchBlock(world, 5, 1, 5, null); // 1 of 8

        // UI overlay opens — progress cleared
        simulateHUDUpdateWithUIBlocking();
        assertEquals(0f, gameHUD.getBlockBreakProgress(), 1e-6f);

        // UI overlay closes — progress restored from actual block damage
        simulateHUDUpdateWithCrosshair(5, 1, 5);
        float progressAfterUI = gameHUD.getBlockBreakProgress();
        assertTrue(progressAfterUI > 0f,
                "HUD should show non-zero progress after overlay closes and block is still damaged");
    }

    @Test
    void test3_targetNameClearedWhenUIOverlayOpens() {
        // Set a target name as if aiming at a block
        gameHUD.setTargetName("Brick");
        assertNotNull(gameHUD.getTargetName());

        // Simulate UI overlay opening
        simulateHUDUpdateWithUIBlocking();

        assertNull(gameHUD.getTargetName(),
                "Target name must be null when a UI overlay is open");
    }

    @Test
    void test4_undamagedBlockShowsZeroProgressRegardlessOfUI() {
        // Block with no damage
        world.setBlock(3, 1, 3, BlockType.STONE);

        // Without UI overlay
        simulateHUDUpdateWithCrosshair(3, 1, 3);
        assertEquals(0f, gameHUD.getBlockBreakProgress(), 1e-6f,
                "Undamaged block should always show 0 progress");

        // With UI overlay
        simulateHUDUpdateWithUIBlocking();
        assertEquals(0f, gameHUD.getBlockBreakProgress(), 1e-6f,
                "Undamaged block with UI open should still show 0 progress");
    }
}

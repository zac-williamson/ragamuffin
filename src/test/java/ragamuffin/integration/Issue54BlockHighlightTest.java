package ragamuffin.integration;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ragamuffin.building.BlockBreaker;
import ragamuffin.building.BlockPlacer;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.BlockType;
import ragamuffin.world.RaycastResult;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #54 Integration Test — Block targeting outline and placement ghost block.
 *
 * Verifies that:
 * 1. getTargetBlock() returns non-null when the player looks at a solid block
 *    within punch range, confirming the outline will be rendered.
 * 2. getTargetBlock() returns null when looking at air, confirming no outline
 *    is drawn.
 * 3. materialToBlockType() returns null for non-placeable materials (food, tools),
 *    confirming the ghost block is not rendered for held non-placeable items.
 * 4. materialToBlockType() returns a non-null BlockType for placeable materials,
 *    and the returned BlockType has a valid colour (non-null) for ghost rendering.
 * 5. The camera's combined (perspective) matrix, not an orthographic matrix,
 *    correctly transforms world-space block coordinates to NDC, confirming that
 *    rendering the outline with camera.combined will place it in the 3D scene.
 * 6. getPlacementPosition() returns a valid adjacent position when looking at a
 *    block, confirming the ghost block can be positioned.
 */
class Issue54BlockHighlightTest {

    @BeforeAll
    static void initGdx() {
        HeadlessTestHelper.initHeadless();
    }

    /**
     * Test 1: getTargetBlock() returns non-null when a solid block is directly
     * in front of the player within punch range.
     * This confirms the targeting outline will be rendered.
     */
    @Test
    void test1_TargetBlockNonNullWhenLookingAtSolidBlock() {
        World world = new World(42L);
        BlockBreaker blockBreaker = new BlockBreaker();

        // Place a stone block directly in front of the camera
        world.setBlock(10, 5, 8, BlockType.STONE);

        Vector3 origin = new Vector3(10, 5, 12);  // Player at z=12
        Vector3 direction = new Vector3(0, 0, -1); // Looking -Z toward the block

        RaycastResult result = blockBreaker.getTargetBlock(world, origin, direction, 5.0f);

        assertNotNull(result,
            "getTargetBlock() must return non-null when looking at a solid block — " +
            "without this the targeting outline will never be rendered");
        assertEquals(10, result.getBlockX());
        assertEquals(5, result.getBlockY());
        assertEquals(8, result.getBlockZ());
    }

    /**
     * Test 2: getTargetBlock() returns null when looking at empty air.
     * This confirms the outline is correctly suppressed when not targeting a block.
     */
    @Test
    void test2_TargetBlockNullWhenLookingAtAir() {
        World world = new World(42L);
        BlockBreaker blockBreaker = new BlockBreaker();

        // No blocks placed — world is empty
        Vector3 origin = new Vector3(50, 50, 50);
        Vector3 direction = new Vector3(0, 1, 0); // Looking straight up into empty air

        RaycastResult result = blockBreaker.getTargetBlock(world, origin, direction, 5.0f);

        assertNull(result,
            "getTargetBlock() must return null when looking at air — " +
            "the outline must only render when a valid target exists");
    }

    /**
     * Test 3: materialToBlockType() returns null for non-placeable items.
     * When the player holds food or a tool, the ghost block must not render.
     */
    @Test
    void test3_GhostBlockNotRenderedForNonPlaceableMaterials() {
        BlockPlacer blockPlacer = new BlockPlacer();

        // Food items should not produce a block type
        assertNull(blockPlacer.materialToBlockType(Material.SAUSAGE_ROLL),
            "Food (SAUSAGE_ROLL) should return null from materialToBlockType — ghost must not render");
        assertNull(blockPlacer.materialToBlockType(Material.ENERGY_DRINK),
            "Food (ENERGY_DRINK) should return null from materialToBlockType — ghost must not render");
    }

    /**
     * Test 4: materialToBlockType() returns a valid BlockType with a non-null colour
     * for placeable materials. This confirms the ghost block can be coloured correctly.
     */
    @Test
    void test4_GhostBlockHasColourForPlaceableMaterials() {
        BlockPlacer blockPlacer = new BlockPlacer();

        // PLANKS is a core placeable material
        BlockType woodType = blockPlacer.materialToBlockType(Material.PLANKS);
        assertNotNull(woodType,
            "PLANKS should map to a non-null BlockType for ghost rendering");
        Color woodColor = woodType.getColor();
        assertNotNull(woodColor,
            "BlockType.WOOD must return a non-null Color for the ghost block face colour");
        // Alpha should be > 0 at the block level (ghost rendering sets its own alpha)
        assertTrue(woodColor.r >= 0f && woodColor.r <= 1f,
            "Block colour R channel must be in [0, 1] for valid rendering");

        // BRICK is another key material
        BlockType brickType = blockPlacer.materialToBlockType(Material.BRICK);
        assertNotNull(brickType,
            "BRICK material should map to a non-null BlockType for ghost rendering");
        assertNotNull(brickType.getColor(),
            "BlockType.BRICK must return a non-null Color for the ghost block");
    }

    /**
     * Test 5: camera.combined (perspective) matrix maps a world-space block coordinate
     * differently from an orthographic matrix, confirming that renderBlockHighlight()
     * using camera.combined renders in 3D scene space (not flat screen space).
     *
     * The block outline is rendered with shapeRenderer.setProjectionMatrix(camera.combined).
     * This test verifies that camera.combined correctly transforms near-camera world
     * positions into valid NDC, confirming the rendering approach is correct.
     */
    @Test
    void test5_CameraCombinedMatrixMapsWorldCoordsToCameraSpace() {
        PerspectiveCamera camera = new PerspectiveCamera(67, 1280, 720);
        camera.position.set(10, 5, 12);
        camera.lookAt(10, 5, 8);  // Looking toward block at z=8
        camera.near = 0.1f;
        camera.far = 300f;
        camera.update();

        // Block directly in front of the camera at (10, 5, 8)
        Vector3 blockCorner = new Vector3(10, 5, 8);

        // With camera.combined, the block corner should project to valid NDC
        Matrix4 combined = camera.combined;
        Vector3 projectedCombined = new Vector3(blockCorner);
        projectedCombined.prj(combined);

        // The block is within camera frustum — NDC x and y should be in [-1, 1]
        assertTrue(projectedCombined.x >= -1f && projectedCombined.x <= 1f,
            "Block corner X should project to valid NDC with camera.combined, got " + projectedCombined.x);
        assertTrue(projectedCombined.y >= -1f && projectedCombined.y <= 1f,
            "Block corner Y should project to valid NDC with camera.combined, got " + projectedCombined.y);

        // An ortho 2D matrix (wrong choice for 3D block outline) maps this coordinate very differently
        Matrix4 ortho = new Matrix4();
        ortho.setToOrtho2D(0, 0, 1280, 720);
        Vector3 projectedOrtho = new Vector3(blockCorner);
        projectedOrtho.prj(ortho);

        // Under ortho 2D, small world coords (10, 5) map to far outside NDC
        // or nowhere near where the block visually appears — confirming camera.combined is correct
        assertNotEquals(projectedCombined.x, projectedOrtho.x, 0.1f,
            "camera.combined and ortho2D must produce different NDC values for world-space coordinates");
    }

    /**
     * Test 6: getPlacementPosition() returns a valid adjacent-block position
     * when the camera is looking at a solid block.
     * This confirms the ghost block can be placed at a meaningful location.
     */
    @Test
    void test6_PlacementPositionAdjacentToTargetBlock() {
        World world = new World(42L);
        BlockPlacer blockPlacer = new BlockPlacer();

        // Place a ground block
        world.setBlock(10, 5, 8, BlockType.STONE);

        Vector3 origin = new Vector3(10, 5, 12);
        Vector3 direction = new Vector3(0, 0, -1); // Looking -Z toward z=8

        Vector3 placement = blockPlacer.getPlacementPosition(world, origin, direction, 5.0f);

        assertNotNull(placement,
            "getPlacementPosition() must return non-null when looking at a solid block face — " +
            "the ghost block needs a position to render at");

        // Placement must be adjacent to the block (differ by 1 on exactly one axis)
        int dx = Math.abs((int) Math.floor(placement.x) - 10);
        int dy = Math.abs((int) Math.floor(placement.y) - 5);
        int dz = Math.abs((int) Math.floor(placement.z) - 8);
        int totalDiff = dx + dy + dz;
        assertEquals(1, totalDiff,
            "Placement position must be exactly 1 block adjacent to the target block, " +
            "got displacement (" + dx + ", " + dy + ", " + dz + ")");
    }
}

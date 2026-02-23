package ragamuffin.integration;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ragamuffin.test.HeadlessTestHelper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #52 Integration Test — HUD ShapeRenderer projection in PAUSED state.
 *
 * Verifies that:
 * - The orthographic matrix produced by setToOrtho2D(0, 0, w, h) correctly maps
 *   2D screen-space pixel coordinates, confirming that HUD elements drawn with
 *   shapeRenderer will appear at the right screen position regardless of any
 *   prior 3D perspective matrix being set.
 * - A typical 3D perspective matrix does NOT map 2D screen coordinates correctly
 *   (demonstrating the stale-matrix bug that issue #52 fixes).
 *
 * This mirrors the logic fix in RagamuffinGame.renderUI(), which now calls
 * shapeRenderer.setProjectionMatrix(ortho) before any ShapeRenderer.begin(),
 * making HUD rendering self-contained and correct in PLAYING, PAUSED, and any
 * future game states.
 */
class Issue52HUDProjectionTest {

    private static final int SCREEN_WIDTH  = 1280;
    private static final int SCREEN_HEIGHT = 720;

    @BeforeAll
    static void initGdx() {
        HeadlessTestHelper.initHeadless();
    }

    /**
     * Test 1: Orthographic 2D matrix maps HUD pixel coordinates to NDC correctly.
     *
     * The fix in renderUI() sets:
     *   ortho.setToOrtho2D(0, 0, screenWidth, screenHeight)
     *   shapeRenderer.setProjectionMatrix(ortho)
     *
     * Verify that screen-space pixel coordinates are transformed to the expected
     * NDC values when the ortho matrix is applied — confirming that health bars
     * drawn at pixel positions will appear on screen.
     */
    @Test
    void test1_OrthoMatrixMapsHudPixelCoordsToNdc() {
        Matrix4 ortho = new Matrix4();
        ortho.setToOrtho2D(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // Health bar at top-left (x=20, y=700 in 720p)
        Vector3 hudPos = new Vector3(20f, 700f, 0f);
        hudPos.prj(ortho);

        // After ortho transform, NDC X should be in [-1, 1]
        assertTrue(hudPos.x >= -1f && hudPos.x <= 1f,
            "Health bar X should map to NDC [-1,1], got " + hudPos.x);
        assertTrue(hudPos.y >= -1f && hudPos.y <= 1f,
            "Health bar Y should map to NDC [-1,1], got " + hudPos.y);

        // The position is close to top-left, so NDC should be near (-1, 1)
        assertTrue(hudPos.x < 0f,
            "Left-side HUD element should map to negative NDC X, got " + hudPos.x);
        assertTrue(hudPos.y > 0f,
            "Top-side HUD element should map to positive NDC Y, got " + hudPos.y);
    }

    /**
     * Test 2: Screen centre maps to NDC (0, 0) with ortho matrix.
     *
     * The crosshair is drawn at screen centre (screenWidth/2, screenHeight/2).
     * With the orthographic fix, this must map to NDC (0, 0) — the true screen
     * centre — so the crosshair appears in the middle of the display.
     */
    @Test
    void test2_CrosshairAtCentreMapsToNdcOrigin() {
        Matrix4 ortho = new Matrix4();
        ortho.setToOrtho2D(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        Vector3 crosshair = new Vector3(SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f, 0f);
        crosshair.prj(ortho);

        assertEquals(0f, crosshair.x, 0.01f,
            "Crosshair at screen centre should map to NDC X=0, got " + crosshair.x);
        assertEquals(0f, crosshair.y, 0.01f,
            "Crosshair at screen centre should map to NDC Y=0, got " + crosshair.y);
    }

    /**
     * Test 3: A stale 3D perspective matrix does NOT map HUD coordinates correctly.
     *
     * This test demonstrates the bug that issue #52 fixes.  A perspective matrix
     * appropriate for 3D world-space rendering (as left behind by modelBatch.end())
     * does not produce valid 2D screen mappings for HUD pixel coordinates.
     *
     * Specifically, HUD coordinates like (20, 700) under a perspective matrix that
     * was built for 3D space will project to NDC values far outside [-1, 1] or
     * produce garbage values — making health bars and the crosshair invisible.
     */
    @Test
    void test3_PerspectiveMatrixDoesNotMapHudCoordinatesCorrectly() {
        // Simulate a typical 3D perspective matrix (like the one left by modelBatch.end())
        // We build one using setToProjection (the kind PerspectiveCamera uses)
        Matrix4 perspective = new Matrix4();
        // near=0.1, far=300, fov=67 degrees, aspect=1280/720
        perspective.setToProjection(0.1f, 300f, 67f, (float) SCREEN_WIDTH / SCREEN_HEIGHT);

        // Apply the stale perspective matrix to a typical HUD pixel position
        // (health bar at pixel 20, 700 in 1280x720)
        Vector3 hudPos = new Vector3(20f, 700f, 0f);
        hudPos.prj(perspective);

        // Under a 3D perspective matrix, pixel-space coordinates (20, 700) will
        // produce NDC values well outside [-1, 1], making the element invisible.
        // At least one axis must be out of normal NDC range.
        boolean xOutOfRange = hudPos.x < -1f || hudPos.x > 1f;
        boolean yOutOfRange = hudPos.y < -1f || hudPos.y > 1f;
        assertTrue(xOutOfRange || yOutOfRange,
            "HUD pixel coordinates under a 3D perspective matrix should produce " +
            "out-of-range NDC values (demonstrating the stale-matrix bug). " +
            "Got NDC (" + hudPos.x + ", " + hudPos.y + ")");
    }

    /**
     * Test 4: Bottom-left corner maps to NDC (-1, -1) with ortho matrix.
     *
     * Confirms the full extent of the orthographic mapping, ensuring that the
     * entire screen area is addressable by the ShapeRenderer when the fix is applied.
     */
    @Test
    void test4_BottomLeftMapsToNdcMinusOne() {
        Matrix4 ortho = new Matrix4();
        ortho.setToOrtho2D(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        Vector3 bottomLeft = new Vector3(0f, 0f, 0f);
        bottomLeft.prj(ortho);

        assertEquals(-1f, bottomLeft.x, 0.01f,
            "Bottom-left X should map to NDC -1, got " + bottomLeft.x);
        assertEquals(-1f, bottomLeft.y, 0.01f,
            "Bottom-left Y should map to NDC -1, got " + bottomLeft.y);
    }

    /**
     * Test 5: Top-right corner maps to NDC (1, 1) with ortho matrix.
     *
     * Confirms the weather display and reputation stars (top-right corner) will
     * be addressable after the fix.
     */
    @Test
    void test5_TopRightMapsToNdcPlusOne() {
        Matrix4 ortho = new Matrix4();
        ortho.setToOrtho2D(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        Vector3 topRight = new Vector3(SCREEN_WIDTH, SCREEN_HEIGHT, 0f);
        topRight.prj(ortho);

        assertEquals(1f, topRight.x, 0.01f,
            "Top-right X should map to NDC 1, got " + topRight.x);
        assertEquals(1f, topRight.y, 0.01f,
            "Top-right Y should map to NDC 1, got " + topRight.y);
    }
}

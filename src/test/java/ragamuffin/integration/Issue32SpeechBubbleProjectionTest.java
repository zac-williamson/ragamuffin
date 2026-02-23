package ragamuffin.integration;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.test.HeadlessTestHelper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #32 Integration Test — Speech bubble orthographic projection.
 *
 * Verifies that:
 * - When camera.project() is used with screen dimensions, the resulting pixel
 *   coordinates for an NPC within the camera view are within screen bounds.
 * - The orthographic matrix produced by setToOrtho2D(0, 0, w, h) maps those
 *   pixel coordinates correctly (i.e., is identity in pixel space), confirming
 *   shapeRenderer/spriteBatch will draw at the right screen position.
 *
 * This test mirrors the logic in RagamuffinGame.renderSpeechBubbles() and
 * confirms that the fix (adding setProjectionMatrix(ortho) before drawing)
 * resolves the bug where speech bubbles were invisible due to the stale 3D
 * perspective matrix inherited from modelBatch.end().
 */
class Issue32SpeechBubbleProjectionTest {

    private static final int SCREEN_WIDTH  = 800;
    private static final int SCREEN_HEIGHT = 600;

    @BeforeAll
    static void initGdx() {
        HeadlessTestHelper.initHeadless();
    }

    /**
     * Test 1: NPC directly in front of the camera projects to screen centre.
     *
     * Place camera at (0, 2, 10) looking toward -Z. Place NPC at (0, 0, 0),
     * so the NPC head (y+2.2 = 2.2) is roughly in front of the camera.
     * Project the head position. Verify the resulting screen X is within
     * [0, SCREEN_WIDTH] and screen Y is within [0, SCREEN_HEIGHT].
     */
    @Test
    void test1_NpcInViewProjectsToScreenBounds() {
        PerspectiveCamera camera = new PerspectiveCamera(67f, SCREEN_WIDTH, SCREEN_HEIGHT);
        camera.position.set(0f, 2f, 10f);
        camera.lookAt(0f, 2f, 0f);
        camera.near = 0.1f;
        camera.far  = 300f;
        camera.update();

        // Simulate the NPC head position computation from renderSpeechBubbles()
        NPC npc = new NPC(NPCType.PUBLIC, 0f, 0f, 0f);
        Vector3 headPos = new Vector3(npc.getPosition());
        headPos.y += 2.2f;

        // Project to screen coordinates, exactly as renderSpeechBubbles() does
        camera.project(headPos, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        float sx = headPos.x;
        float sy = headPos.y;
        float sz = headPos.z; // depth: 0=near, 1=far

        // The NPC is in front of the camera — must not be clipped
        assertTrue(sz >= 0f && sz <= 1.0f,
            "NPC should be within camera depth range, got z=" + sz);

        // The projected pixel coordinates must be within screen bounds
        assertTrue(sx >= 0f && sx <= SCREEN_WIDTH,
            "Projected X should be within [0, " + SCREEN_WIDTH + "], got " + sx);
        assertTrue(sy >= 0f && sy <= SCREEN_HEIGHT,
            "Projected Y should be within [0, " + SCREEN_HEIGHT + "], got " + sy);
    }

    /**
     * Test 2: Orthographic matrix maps pixel coordinates to themselves.
     *
     * The fix sets ortho = new Matrix4().setToOrtho2D(0, 0, w, h) and passes
     * this to shapeRenderer and spriteBatch. This test verifies that the ortho
     * matrix, when applied to a projected screen coordinate vector (sx, sy, 0),
     * produces NDC values in [-1, 1] — confirming the renderer will draw at the
     * correct screen position.
     *
     * Specifically: a pixel at (SCREEN_WIDTH/2, SCREEN_HEIGHT/2) should map to
     * NDC (0, 0) (centre of screen), and a pixel at (0, 0) should map to NDC
     * (-1, -1) (bottom-left).
     */
    @Test
    void test2_OrthoMatrixMapsPixelCoordsToNdc() {
        Matrix4 ortho = new Matrix4();
        ortho.setToOrtho2D(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // Centre pixel → NDC (0, 0)
        Vector3 centre = new Vector3(SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f, 0f);
        centre.prj(ortho);
        assertEquals(0f, centre.x, 0.01f,
            "Centre X should map to NDC 0, got " + centre.x);
        assertEquals(0f, centre.y, 0.01f,
            "Centre Y should map to NDC 0, got " + centre.y);

        // Bottom-left pixel → NDC (-1, -1)
        Vector3 bottomLeft = new Vector3(0f, 0f, 0f);
        bottomLeft.prj(ortho);
        assertEquals(-1f, bottomLeft.x, 0.01f,
            "Bottom-left X should map to NDC -1, got " + bottomLeft.x);
        assertEquals(-1f, bottomLeft.y, 0.01f,
            "Bottom-left Y should map to NDC -1, got " + bottomLeft.y);
    }

    /**
     * Test 3: NPC behind the camera is skipped (depth check).
     *
     * Place the NPC behind the camera. Verify that sz > 1.0f or sz < 0f,
     * matching the guard condition in renderSpeechBubbles():
     *   if (tmpCameraPos.z > 1.0f || tmpCameraPos.z < 0f) continue;
     */
    @Test
    void test3_NpcBehindCameraIsSkipped() {
        PerspectiveCamera camera = new PerspectiveCamera(67f, SCREEN_WIDTH, SCREEN_HEIGHT);
        camera.position.set(0f, 2f, -10f);
        camera.lookAt(0f, 2f, -20f); // Looking away from Z=0
        camera.near = 0.1f;
        camera.far  = 300f;
        camera.update();

        // NPC at Z=0 is behind the camera
        Vector3 headPos = new Vector3(0f, 2.2f, 0f);
        camera.project(headPos, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        float sz = headPos.z;

        // Should be clipped by the behind-camera guard
        assertTrue(sz > 1.0f || sz < 0f,
            "NPC behind camera should have sz > 1 or sz < 0, got " + sz);
    }

    /**
     * Test 4: Bubble screen X is derived correctly from the NPC world position.
     *
     * Place the NPC off-centre to the right. Verify the projected screen X is
     * greater than SCREEN_WIDTH/2 (right half of screen), confirming that the
     * projection faithfully maps world-space position to screen-space position.
     */
    @Test
    void test4_NpcOffCentreProjectsToCorrectSide() {
        PerspectiveCamera camera = new PerspectiveCamera(67f, SCREEN_WIDTH, SCREEN_HEIGHT);
        camera.position.set(0f, 2f, 10f);
        camera.lookAt(0f, 2f, 0f);
        camera.near = 0.1f;
        camera.far  = 300f;
        camera.update();

        // NPC placed to the right of the camera centre line
        Vector3 headPos = new Vector3(5f, 2.2f, 0f);
        camera.project(headPos, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        float sx = headPos.x;
        float sz = headPos.z;

        // Must be visible
        assertTrue(sz >= 0f && sz <= 1.0f, "NPC should be within depth range");

        // Right-of-centre NPC should project to right half of screen
        assertTrue(sx > SCREEN_WIDTH / 2f,
            "NPC offset right should project to right half of screen (x > " +
            (SCREEN_WIDTH / 2f) + "), got " + sx);
    }
}

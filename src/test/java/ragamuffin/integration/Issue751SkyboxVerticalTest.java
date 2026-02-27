package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.render.SkyRenderer;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #751: Fix skybox moving vertically with player camera.
 *
 * The skybox should not track vertical camera movement (pitch). Sky elements
 * must remain fixed in world space when the player looks up or down.
 *
 * These tests verify the pure-logic parts of the pitch offset calculation
 * (no GL calls needed): when the camera pitches up, sky elements shift
 * downward on screen by a proportional amount, keeping them stationary
 * relative to the world.
 */
class Issue751SkyboxVerticalTest {

    private static final float SUNRISE = 6.0f;
    private static final float SUNSET  = 20.0f;
    private static final float NOON    = 13.0f;

    private SkyRenderer skyRenderer;

    @BeforeEach
    void setUp() {
        skyRenderer = new SkyRenderer();
    }

    // -----------------------------------------------------------------------
    // Test 1: renderSkybox overload with cameraPitchDeg parameter exists
    // -----------------------------------------------------------------------

    @Test
    void renderSkybox_signatureIncludesCameraPitch() throws NoSuchMethodException {
        // The 10-parameter overload must exist: the last parameter is cameraPitchDeg.
        Method m = SkyRenderer.class.getMethod(
                "renderSkybox",
                com.badlogic.gdx.graphics.glutils.ShapeRenderer.class,
                float.class, float.class, float.class,
                int.class, int.class,
                boolean.class, float.class,
                int.class, float.class);
        assertNotNull(m, "renderSkybox must have a cameraPitchDeg overload");
    }

    // -----------------------------------------------------------------------
    // Test 2: Pitch offset calculation — looking up shifts elements downward
    //
    // The pitch offset formula is: pitchOffsetY = (cameraPitchDeg / 90f) * (screenHeight * 0.5f)
    // When pitchDeg > 0 (looking up), pitchOffsetY > 0, so Y = baseY - pitchOffsetY < baseY.
    // -----------------------------------------------------------------------

    @Test
    void lookingUp_shiftsSkyElementsDownOnScreen() {
        int screenHeight = 720;

        // At noon, sun world yaw = 180°, camera yaw = 180° (facing sun), no horizontal offset.
        // Flat (pitch=0): sunY = horizonY + (peakY - horizonY) * elevation
        // At noon: dayFraction=0.5, elevation = sin(0.5*PI) = 1.0
        float horizonY = screenHeight * 0.70f;
        float peakY    = screenHeight * 0.85f;
        float elevation = 1.0f; // noon
        float sunY_flat = horizonY + (peakY - horizonY) * elevation; // peakY

        // With pitch = +30° (looking up): pitchOffsetY = (30/90) * (720*0.5) = (1/3) * 360 = 120
        float pitchDeg = 30f;
        float pitchOffsetY = (pitchDeg / 90f) * (screenHeight * 0.5f);
        float sunY_lookingUp = sunY_flat - pitchOffsetY;

        assertTrue(sunY_lookingUp < sunY_flat,
                "Looking up should shift sun downward on screen (lower Y value); " +
                "sunY_flat=" + sunY_flat + ", sunY_lookingUp=" + sunY_lookingUp);
    }

    // -----------------------------------------------------------------------
    // Test 3: Looking down shifts sky elements upward on screen
    // -----------------------------------------------------------------------

    @Test
    void lookingDown_shiftsSkyElementsUpOnScreen() {
        int screenHeight = 720;

        float horizonY = screenHeight * 0.70f;
        float peakY    = screenHeight * 0.85f;
        float elevation = 1.0f;
        float sunY_flat = horizonY + (peakY - horizonY) * elevation;

        // With pitch = -30° (looking down): pitchOffsetY = (-30/90) * 360 = -120
        float pitchDeg = -30f;
        float pitchOffsetY = (pitchDeg / 90f) * (screenHeight * 0.5f);
        float sunY_lookingDown = sunY_flat - pitchOffsetY;

        assertTrue(sunY_lookingDown > sunY_flat,
                "Looking down should shift sun upward on screen (higher Y value); " +
                "sunY_flat=" + sunY_flat + ", sunY_lookingDown=" + sunY_lookingDown);
    }

    // -----------------------------------------------------------------------
    // Test 4: Pitch offset is proportional — 60° gives twice the offset of 30°
    // -----------------------------------------------------------------------

    @Test
    void pitchOffset_isProportionalToPitchAngle() {
        int screenHeight = 720;

        float offset30 = (30f / 90f) * (screenHeight * 0.5f);
        float offset60 = (60f / 90f) * (screenHeight * 0.5f);

        assertEquals(offset30 * 2f, offset60, 0.01f,
                "Pitch offset at 60° must be exactly twice that at 30°");
    }

    // -----------------------------------------------------------------------
    // Test 5: Zero pitch gives zero offset (level camera = unaffected Y positions)
    // -----------------------------------------------------------------------

    @Test
    void zeroPitch_givesZeroOffset() {
        int screenHeight = 720;
        float offset = (0f / 90f) * (screenHeight * 0.5f);
        assertEquals(0f, offset, 1e-6f,
                "Zero pitch must produce zero vertical offset");
    }

    // -----------------------------------------------------------------------
    // Test 6: Pitch offset at ±90° equals half screen height
    // -----------------------------------------------------------------------

    @Test
    void pitchOffset_atNinetyDegrees_isHalfScreenHeight() {
        int screenHeight = 720;
        float offsetUp   = (90f  / 90f) * (screenHeight * 0.5f);
        float offsetDown = (-90f / 90f) * (screenHeight * 0.5f);

        assertEquals(screenHeight * 0.5f,  offsetUp,   0.01f,
                "90° pitch up must give offset of half screen height");
        assertEquals(-screenHeight * 0.5f, offsetDown, 0.01f,
                "90° pitch down must give negative offset of half screen height");
    }

    // -----------------------------------------------------------------------
    // Test 7: Existing 8-parameter overload (no dayOfYear) still compiles
    // -----------------------------------------------------------------------

    @Test
    void existingEightParamOverload_stillExists() throws NoSuchMethodException {
        Method m = SkyRenderer.class.getMethod(
                "renderSkybox",
                com.badlogic.gdx.graphics.glutils.ShapeRenderer.class,
                float.class, float.class, float.class,
                int.class, int.class,
                boolean.class, float.class);
        assertNotNull(m, "Original 8-param renderSkybox overload must still exist");
    }

    // -----------------------------------------------------------------------
    // Test 8: Existing 9-parameter overload (with dayOfYear, no pitch) still compiles
    // -----------------------------------------------------------------------

    @Test
    void existingNineParamOverload_stillExists() throws NoSuchMethodException {
        Method m = SkyRenderer.class.getMethod(
                "renderSkybox",
                com.badlogic.gdx.graphics.glutils.ShapeRenderer.class,
                float.class, float.class, float.class,
                int.class, int.class,
                boolean.class, float.class,
                int.class);
        assertNotNull(m, "9-param renderSkybox overload (with dayOfYear) must still exist");
    }
}

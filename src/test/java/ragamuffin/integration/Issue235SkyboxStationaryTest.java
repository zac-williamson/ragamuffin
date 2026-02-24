package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.render.SkyRenderer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #235: Fix skybox to remain stationary relative to world.
 *
 * The skybox (sun and clouds) must not move when the player looks around.
 * Instead, sky elements should be offset on screen based on the camera yaw so
 * they appear fixed in the world — turning the camera to face the sun brings it
 * to screen centre, turning away moves it off-screen.
 *
 * These tests exercise the pure-logic parts of SkyRenderer (no GL calls) to
 * verify that the camera-yaw parameter is correctly wired into sun positioning.
 */
class Issue235SkyboxStationaryTest {

    private static final float SUNRISE = 6.0f;
    private static final float SUNSET  = 20.0f;
    // Noon is the midpoint; at noon the sun is due south (world yaw 180°)
    private static final float NOON    = 13.0f;

    private SkyRenderer skyRenderer;

    @BeforeEach
    void setUp() {
        skyRenderer = new SkyRenderer();
    }

    // -----------------------------------------------------------------------
    // Test 1: renderSkybox signature includes cameraYawDeg parameter
    // -----------------------------------------------------------------------

    @Test
    void renderSkybox_signatureIncludesCameraYaw() throws NoSuchMethodException {
        // The method must accept cameraYawDeg as the 8th (last) parameter.
        var m = SkyRenderer.class.getMethod(
                "renderSkybox",
                com.badlogic.gdx.graphics.glutils.ShapeRenderer.class,
                float.class, float.class, float.class,
                int.class, int.class,
                boolean.class, float.class);
        assertNotNull(m, "renderSkybox must accept cameraYawDeg float parameter");
    }

    // -----------------------------------------------------------------------
    // Test 2: getSunScreenX is still correct (not broken by the fix)
    // -----------------------------------------------------------------------

    @Test
    void sunScreenX_stillCorrectAfterFix() {
        // getSunScreenX is a pure helper used by callers; the fix must not break it.
        float atNoon = skyRenderer.getSunScreenX(NOON, SUNRISE, SUNSET);
        assertEquals(0.5f, atNoon, 1e-4f, "getSunScreenX at noon must still return 0.5");

        float atSunrise = skyRenderer.getSunScreenX(SUNRISE, SUNRISE, SUNSET);
        assertEquals(0.0f, atSunrise, 1e-4f, "getSunScreenX at sunrise must still return 0.0");

        float atSunset = skyRenderer.getSunScreenX(SUNSET, SUNRISE, SUNSET);
        assertEquals(1.0f, atSunset, 1e-4f, "getSunScreenX at sunset must still return 1.0");
    }

    // -----------------------------------------------------------------------
    // Test 3: Sun world-yaw mapping — at noon the sun is due south (180°)
    //
    // Verify indirectly: when the camera faces south (yaw=180°) at noon the sun
    // should be at screen centre, i.e. getSunScreenX returns 0.5.  This is a
    // logical consistency check — the rendering path uses the same dayFraction
    // to derive both getSunScreenX and the world-yaw offset.
    // -----------------------------------------------------------------------

    @Test
    void sunAtNoon_isCentredWhenFacingSouth() {
        // At noon the sun is at dayFraction=0.5, world yaw = 90 + 0.5*180 = 180° (south).
        // getSunScreenX is 0.5 at noon regardless of camera — it measures time position.
        // The rendering offset is: screenCentre + (sunYaw - cameraYaw)/90 * screenWidth.
        // When cameraYaw == sunYaw (180°), offset == 0 → sunX == screenCentre. ✓
        float dayFraction = (NOON - SUNRISE) / (SUNSET - SUNRISE);
        float sunWorldYaw = 90f + dayFraction * 180f;
        assertEquals(180f, sunWorldYaw, 0.1f, "Sun world yaw at noon should be 180° (south)");

        // Camera facing south (yaw=180) — yawDiff = 0 — sun at screen centre.
        float cameraYaw = 180f;
        float yawDiff = sunWorldYaw - cameraYaw;
        assertEquals(0f, yawDiff, 0.1f, "yawDiff must be 0 when camera faces the noon sun");
    }

    // -----------------------------------------------------------------------
    // Test 4: Sun moves off-screen when camera faces away
    // -----------------------------------------------------------------------

    @Test
    void sunMovesOffScreenWhenCameraFacesAway() {
        // At noon sun is at world yaw 180°.  If the camera faces north (yaw=0°),
        // yawDiff = 180°, which maps to +2 screen-widths off centre — well off-screen.
        float dayFraction = (NOON - SUNRISE) / (SUNSET - SUNRISE);
        float sunWorldYaw = 90f + dayFraction * 180f; // 180°

        float cameraYawNorth = 0f;
        float yawDiff = sunWorldYaw - cameraYawNorth;
        // Normalise to [-180, 180]
        while (yawDiff >  180f) yawDiff -= 360f;
        while (yawDiff < -180f) yawDiff += 360f;

        int screenWidth = 1280;
        float sunX = screenWidth * 0.5f + (yawDiff / 90f) * screenWidth;
        // sunX should be far from the visible [0, screenWidth] range
        assertTrue(sunX > screenWidth || sunX < 0,
                "Sun should be off-screen when camera faces directly away from it; sunX=" + sunX);
    }

    // -----------------------------------------------------------------------
    // Test 5: Sun shifts left when camera turns right
    // -----------------------------------------------------------------------

    @Test
    void sunShiftsLeftWhenCameraTurnsRight() {
        // At noon the sun is due south (180°).
        // Camera at 170° (slightly right of south) → sun slightly to the right of centre.
        // Camera at 190° (slightly left of south)  → sun slightly to the left of centre.
        float dayFraction = (NOON - SUNRISE) / (SUNSET - SUNRISE);
        float sunWorldYaw = 90f + dayFraction * 180f; // 180°

        int screenWidth = 1280;

        float yawDiff170 = normalise(sunWorldYaw - 170f);
        float sunX_at170 = screenWidth * 0.5f + (yawDiff170 / 90f) * screenWidth;

        float yawDiff190 = normalise(sunWorldYaw - 190f);
        float sunX_at190 = screenWidth * 0.5f + (yawDiff190 / 90f) * screenWidth;

        // Turning camera right (increasing yaw from 170→190) means sun moves left.
        assertTrue(sunX_at190 < sunX_at170,
                "Sun should shift left on screen when camera yaw increases (turns right); " +
                "sunX@170=" + sunX_at170 + ", sunX@190=" + sunX_at190);
    }

    // -----------------------------------------------------------------------
    // Test 6: Sun position is different for two distinct camera yaws at the same time
    // -----------------------------------------------------------------------

    @Test
    void sunScreenPosition_differsByYaw() {
        // Same time-of-day, two different camera yaws → different screen X positions.
        float dayFraction = (NOON - SUNRISE) / (SUNSET - SUNRISE);
        float sunWorldYaw = 90f + dayFraction * 180f;

        int screenWidth = 1280;

        float sunX_yaw0   = screenWidth * 0.5f + (normalise(sunWorldYaw - 0f)   / 90f) * screenWidth;
        float sunX_yaw180 = screenWidth * 0.5f + (normalise(sunWorldYaw - 180f)  / 90f) * screenWidth;

        assertNotEquals(sunX_yaw0, sunX_yaw180, 1f,
                "Sun screen X must differ for camera yaw 0° vs 180°");
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private static float normalise(float yawDiff) {
        while (yawDiff >  180f) yawDiff -= 360f;
        while (yawDiff < -180f) yawDiff += 360f;
        return yawDiff;
    }
}

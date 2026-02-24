package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.core.TimeSystem;
import ragamuffin.render.SkyRenderer;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #227: Move sun and clouds from player HUD to skybox.
 *
 * These tests verify that sun and cloud rendering is performed via the skybox
 * path (renderSkybox) rather than as a 2D HUD overlay, and that the SkyRenderer
 * API reflects the correct rendering order contract.
 */
class Issue227SkyboxRenderOrderTest {

    // -----------------------------------------------------------------------
    // Test 1: renderSkybox method exists (replaces the old render() overlay)
    // -----------------------------------------------------------------------

    @Test
    void skyRenderer_exposesRenderSkyboxMethod() throws NoSuchMethodException {
        // renderSkybox must exist with the correct parameter types.
        // Fix #235 added cameraYawDeg (float) as the last parameter so the skybox
        // remains stationary relative to the world when the player looks around.
        Method m = SkyRenderer.class.getMethod(
                "renderSkybox",
                com.badlogic.gdx.graphics.glutils.ShapeRenderer.class,
                float.class, float.class, float.class,
                int.class, int.class,
                boolean.class, float.class);
        assertNotNull(m, "SkyRenderer must have a public renderSkybox() method");
    }

    // -----------------------------------------------------------------------
    // Test 2: old render() overlay method must no longer exist
    // -----------------------------------------------------------------------

    @Test
    void skyRenderer_doesNotExposeOldRenderMethod() {
        // The old render() HUD-overlay method must have been removed/renamed.
        boolean found = false;
        for (Method m : SkyRenderer.class.getMethods()) {
            if (m.getName().equals("render") &&
                    m.getParameterCount() == 7) {
                found = true;
                break;
            }
        }
        assertFalse(found,
                "SkyRenderer must not expose the old render() method — " +
                "sun/clouds are now rendered via renderSkybox() as part of the skybox layer");
    }

    // -----------------------------------------------------------------------
    // Test 3: renderSkybox contract — Javadoc asserts it must be called
    //         BEFORE modelBatch; verify via update/logic path (no GL needed)
    // -----------------------------------------------------------------------

    @Test
    void skyRenderer_updateAndLogicWorkBeforeRenderSkyboxCall() {
        // Verify that the SkyRenderer can be updated (simulating pre-frame
        // housekeeping) and that the sun/cloud logic methods return correct
        // values which are what renderSkybox would consume.
        SkyRenderer skyRenderer = new SkyRenderer();
        TimeSystem timeSystem = new TimeSystem(12.0f);

        float sunrise = timeSystem.getSunriseTime();
        float sunset  = timeSystem.getSunsetTime();

        // Simulate several frames of update (cloud animation)
        for (int i = 0; i < 60; i++) {
            skyRenderer.update(1f / 60f);
        }

        // Sun position and elevation must still be valid — renderSkybox reads these
        float sunX = skyRenderer.getSunScreenX(12.0f, sunrise, sunset);
        assertTrue(sunX >= 0f && sunX <= 1f,
                "Sun screen-X must be valid at noon for skybox rendering");

        float elevation = skyRenderer.getSunElevation(12.0f, sunrise, sunset);
        assertTrue(elevation > 0f,
                "Sun elevation must be positive at noon for skybox rendering");

        float[] colour = skyRenderer.getSunColour(12.0f, sunrise, sunset);
        assertEquals(3, colour.length, "Sun colour must have 3 components for skybox rendering");
    }

    // -----------------------------------------------------------------------
    // Test 4: Sun and cloud data valid across the full day — skybox path
    // -----------------------------------------------------------------------

    @Test
    void skyboxSunData_validAcrossFullDay() {
        SkyRenderer skyRenderer = new SkyRenderer();
        TimeSystem timeSystem = new TimeSystem(6.0f);

        float sunrise = timeSystem.getSunriseTime();
        float sunset  = timeSystem.getSunsetTime();

        // Step through every half-hour during daylight hours
        for (float t = sunrise; t <= sunset; t += 0.5f) {
            float x    = skyRenderer.getSunScreenX(t, sunrise, sunset);
            float elev = skyRenderer.getSunElevation(t, sunrise, sunset);
            float[] col = skyRenderer.getSunColour(t, sunrise, sunset);

            assertTrue(x >= 0f && x <= 1f,
                    "Sun screen-X out of range at t=" + t + ": " + x);
            assertTrue(elev >= 0f && elev <= 1f,
                    "Sun elevation out of range at t=" + t + ": " + elev);
            for (int i = 0; i < 3; i++) {
                assertTrue(col[i] >= 0f && col[i] <= 1f,
                        "Colour component " + i + " out of range at t=" + t + ": " + col[i]);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Test 5: Cloud count is unaffected by the skybox migration
    // -----------------------------------------------------------------------

    @Test
    void cloudCount_unaffectedBySkyboxMigration() {
        SkyRenderer skyRenderer = new SkyRenderer();
        int count = skyRenderer.getCloudCount();
        assertTrue(count >= 4 && count <= 20,
                "Cloud count should remain between 4 and 20 after skybox migration, got: " + count);
    }
}

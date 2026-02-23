package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.core.TimeSystem;
import ragamuffin.render.SkyRenderer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #209: Add sun and clouds to sky.
 *
 * These tests verify the SkyRenderer works correctly with the TimeSystem
 * to produce the expected visual behaviour through the full day/night cycle.
 * No LibGDX rendering backend is required — only logic methods are exercised.
 */
class Issue209SkyRendererTest {

    // -----------------------------------------------------------------------
    // Test 1: Sun exists and is visible only during the day
    // -----------------------------------------------------------------------

    @Test
    void sun_isVisibleDuringDay_andHiddenAtNight() {
        SkyRenderer skyRenderer = new SkyRenderer();
        TimeSystem timeSystem = new TimeSystem(12.0f); // Start at noon

        float sunrise = timeSystem.getSunriseTime();
        float sunset  = timeSystem.getSunsetTime();

        // Midday: sun should be visible (positive screen-X position)
        float noonX = skyRenderer.getSunScreenX(12.0f, sunrise, sunset);
        assertTrue(noonX >= 0f && noonX <= 1f,
                "Sun should be visible at noon with a valid screen-X fraction");

        // Midnight: sun should be below the horizon (-1 sentinel)
        float midnightX = skyRenderer.getSunScreenX(0.0f, sunrise, sunset);
        assertEquals(-1f, midnightX,
                "Sun should return -1 (below horizon) at midnight");

        // 2 AM: sun should also be below horizon
        float earlyMorningX = skyRenderer.getSunScreenX(2.0f, sunrise, sunset);
        assertEquals(-1f, earlyMorningX,
                "Sun should return -1 (below horizon) at 2 AM");
    }

    // -----------------------------------------------------------------------
    // Test 2: Sun moves across sky from east (left) to west (right)
    // -----------------------------------------------------------------------

    @Test
    void sun_movesEastToWestAcrossTheSky() {
        SkyRenderer skyRenderer = new SkyRenderer();
        TimeSystem timeSystem = new TimeSystem(8.0f);

        float sunrise = timeSystem.getSunriseTime();
        float sunset  = timeSystem.getSunsetTime();

        // Sample sun position at three times: morning, noon, afternoon
        float xMorning   = skyRenderer.getSunScreenX(9.0f,  sunrise, sunset);
        float xNoon      = skyRenderer.getSunScreenX(13.0f, sunrise, sunset);
        float xAfternoon = skyRenderer.getSunScreenX(17.0f, sunrise, sunset);

        assertTrue(xMorning < xNoon,
                "Sun should move right (west) from morning to noon; got " + xMorning + " -> " + xNoon);
        assertTrue(xNoon < xAfternoon,
                "Sun should move right (west) from noon to afternoon; got " + xNoon + " -> " + xAfternoon);
    }

    // -----------------------------------------------------------------------
    // Test 3: Sun is highest at noon and lower at dawn/dusk
    // -----------------------------------------------------------------------

    @Test
    void sun_isHighestAtNoonAndLowerAtHorizon() {
        SkyRenderer skyRenderer = new SkyRenderer();
        TimeSystem timeSystem = new TimeSystem(12.0f);

        float sunrise = timeSystem.getSunriseTime();
        float sunset  = timeSystem.getSunsetTime();
        float noon    = (sunrise + sunset) / 2f;

        float elevNoon  = skyRenderer.getSunElevation(noon,       sunrise, sunset);
        float elevEarly = skyRenderer.getSunElevation(sunrise + 1f, sunrise, sunset);
        float elevLate  = skyRenderer.getSunElevation(sunset  - 1f, sunrise, sunset);

        assertTrue(elevNoon > elevEarly,
                "Sun should be higher at noon than in the early morning");
        assertTrue(elevNoon > elevLate,
                "Sun should be higher at noon than in the late afternoon");
        assertEquals(1.0f, elevNoon, 0.02f,
                "Sun elevation should peak at approximately 1.0 at noon");
    }

    // -----------------------------------------------------------------------
    // Test 4: Sun colour transitions from orange at dawn to white at noon
    // -----------------------------------------------------------------------

    @Test
    void sunColour_transitionsFromOrangeAtDawnToWhiteAtNoon() {
        SkyRenderer skyRenderer = new SkyRenderer();
        TimeSystem timeSystem = new TimeSystem(8.0f);

        float sunrise = timeSystem.getSunriseTime();
        float sunset  = timeSystem.getSunsetTime();
        float noon    = (sunrise + sunset) / 2f;

        float[] colourDawn = skyRenderer.getSunColour(sunrise + 0.5f, sunrise, sunset);
        float[] colourNoon = skyRenderer.getSunColour(noon, sunrise, sunset);

        // At dawn: green and blue channels should be noticeably lower than at noon
        assertTrue(colourDawn[1] < colourNoon[1],
                "Green should be lower at dawn than at noon (more orange)");
        assertTrue(colourDawn[2] < colourNoon[2],
                "Blue should be lower at dawn than at noon (more orange)");

        // At noon: red and green should both be high (white-yellow)
        assertTrue(colourNoon[0] >= 0.95f, "Red should be high at noon");
        assertTrue(colourNoon[1] >= 0.90f, "Green should be high at noon");
    }

    // -----------------------------------------------------------------------
    // Test 5: Clouds are always present (day and night)
    // -----------------------------------------------------------------------

    @Test
    void clouds_alwaysPresentWithPositiveCount() {
        SkyRenderer skyRenderer = new SkyRenderer();
        int cloudCount = skyRenderer.getCloudCount();
        assertTrue(cloudCount > 0,
                "Sky must always have at least one cloud cluster visible");
    }

    // -----------------------------------------------------------------------
    // Test 6: SkyRenderer integrates correctly with TimeSystem seasonal data
    // -----------------------------------------------------------------------

    @Test
    void skyRenderer_worksWithSeasonalTimeSystem() {
        SkyRenderer skyRenderer = new SkyRenderer();

        // Test with summer conditions (long days, early sunrise / late sunset)
        TimeSystem summerTime = new TimeSystem(12.0f); // June 1st start — summer
        float summerSunrise = summerTime.getSunriseTime();
        float summerSunset  = summerTime.getSunsetTime();

        // Summer: sunrise should be before 7 AM, sunset after 8 PM
        assertTrue(summerSunrise < 7.0f,
                "Summer sunrise should be before 07:00, got: " + summerSunrise);
        assertTrue(summerSunset > 20.0f,
                "Summer sunset should be after 20:00, got: " + summerSunset);

        // Sun should be visible at noon in summer
        float noonX = skyRenderer.getSunScreenX(12.0f, summerSunrise, summerSunset);
        assertTrue(noonX >= 0f && noonX <= 1f,
                "Sun should be visible at noon in summer");

        float noonElev = skyRenderer.getSunElevation(12.0f, summerSunrise, summerSunset);
        assertTrue(noonElev > 0.5f,
                "Sun elevation at noon in summer should be well above horizon");
    }

    // -----------------------------------------------------------------------
    // Test 7: Cloud count stays stable across multiple update calls
    // -----------------------------------------------------------------------

    @Test
    void cloudCount_stableAcrossMultipleUpdates() {
        SkyRenderer skyRenderer = new SkyRenderer();
        int initial = skyRenderer.getCloudCount();

        for (int i = 0; i < 100; i++) {
            skyRenderer.update(1.0f / 60f); // simulate 100 frames
        }

        assertEquals(initial, skyRenderer.getCloudCount(),
                "Cloud count should remain constant regardless of elapsed time");
    }
}

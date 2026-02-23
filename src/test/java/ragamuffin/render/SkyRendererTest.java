package ragamuffin.render;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SkyRenderer — sun position / colour calculation and cloud count.
 *
 * These tests exercise the pure-logic methods of SkyRenderer (no LibGDX
 * rendering calls needed, so no headless backend required).
 */
class SkyRendererTest {

    private static final float SUNRISE = 6.0f;   // 06:00 for easy arithmetic
    private static final float SUNSET  = 20.0f;  // 20:00
    private static final float NOON    = 13.0f;  // midpoint

    private SkyRenderer skyRenderer;

    @BeforeEach
    void setUp() {
        skyRenderer = new SkyRenderer();
    }

    // -----------------------------------------------------------------------
    // getSunScreenX
    // -----------------------------------------------------------------------

    @Test
    void sunScreenX_atSunrise_isZero() {
        float x = skyRenderer.getSunScreenX(SUNRISE, SUNRISE, SUNSET);
        assertEquals(0.0f, x, 1e-5f, "Sun screen-X should be 0.0 at sunrise");
    }

    @Test
    void sunScreenX_atNoon_isHalf() {
        float x = skyRenderer.getSunScreenX(NOON, SUNRISE, SUNSET);
        assertEquals(0.5f, x, 1e-5f, "Sun screen-X should be 0.5 at noon");
    }

    @Test
    void sunScreenX_atSunset_isOne() {
        float x = skyRenderer.getSunScreenX(SUNSET, SUNRISE, SUNSET);
        assertEquals(1.0f, x, 1e-5f, "Sun screen-X should be 1.0 at sunset");
    }

    @Test
    void sunScreenX_atNight_isNegative() {
        // Midnight is not between sunrise and sunset
        float x = skyRenderer.getSunScreenX(0.0f, SUNRISE, SUNSET);
        assertEquals(-1f, x, "Sun screen-X should be -1 at night");
    }

    @Test
    void sunScreenX_movesEastToWestDuringDay() {
        float x1 = skyRenderer.getSunScreenX(8.0f, SUNRISE, SUNSET);
        float x2 = skyRenderer.getSunScreenX(12.0f, SUNRISE, SUNSET);
        float x3 = skyRenderer.getSunScreenX(18.0f, SUNRISE, SUNSET);
        assertTrue(x1 < x2, "Sun should move right (east to west) through the morning");
        assertTrue(x2 < x3, "Sun should move right (east to west) through the afternoon");
    }

    // -----------------------------------------------------------------------
    // getSunElevation
    // -----------------------------------------------------------------------

    @Test
    void sunElevation_atNoon_isOne() {
        float elev = skyRenderer.getSunElevation(NOON, SUNRISE, SUNSET);
        assertEquals(1.0f, elev, 0.01f, "Sun elevation should peak at 1.0 at noon");
    }

    @Test
    void sunElevation_atSunrise_isNearZero() {
        float elev = skyRenderer.getSunElevation(SUNRISE, SUNRISE, SUNSET);
        assertEquals(0.0f, elev, 0.01f, "Sun elevation should be ~0 at sunrise");
    }

    @Test
    void sunElevation_atSunset_isNearZero() {
        float elev = skyRenderer.getSunElevation(SUNSET, SUNRISE, SUNSET);
        assertEquals(0.0f, elev, 0.01f, "Sun elevation should be ~0 at sunset");
    }

    @Test
    void sunElevation_atNight_isZero() {
        float elev = skyRenderer.getSunElevation(2.0f, SUNRISE, SUNSET);
        assertEquals(0.0f, elev, "Sun elevation should be 0 at night");
    }

    @Test
    void sunElevation_isNonNegativeThroughoutDay() {
        for (float t = SUNRISE; t <= SUNSET; t += 0.5f) {
            float elev = skyRenderer.getSunElevation(t, SUNRISE, SUNSET);
            assertTrue(elev >= 0.0f, "Sun elevation should never be negative during the day (t=" + t + ")");
        }
    }

    // -----------------------------------------------------------------------
    // getSunColour
    // -----------------------------------------------------------------------

    @Test
    void sunColour_hasThreeComponents() {
        float[] colour = skyRenderer.getSunColour(NOON, SUNRISE, SUNSET);
        assertNotNull(colour);
        assertEquals(3, colour.length, "Sun colour should have exactly 3 components (R, G, B)");
    }

    @Test
    void sunColour_atNoon_isYellowishWhite() {
        float[] colour = skyRenderer.getSunColour(NOON, SUNRISE, SUNSET);
        // At noon the sun should be mostly white/yellow — red and green both near 1
        assertTrue(colour[0] >= 0.95f, "Red channel at noon should be near 1.0");
        assertTrue(colour[1] >= 0.90f, "Green channel at noon should be high");
    }

    @Test
    void sunColour_atHorizon_isMoreOrange() {
        // Just after sunrise — should have a noticeably lower green/blue than noon
        float[] colourNoon = skyRenderer.getSunColour(NOON, SUNRISE, SUNSET);
        float[] colourHorizon = skyRenderer.getSunColour(SUNRISE + 0.1f, SUNRISE, SUNSET);
        assertTrue(colourHorizon[1] < colourNoon[1],
                "Green channel should be lower at horizon than at noon");
        assertTrue(colourHorizon[2] < colourNoon[2],
                "Blue channel should be lower at horizon than at noon");
    }

    @Test
    void sunColour_allComponentsAreInRange() {
        for (float t = SUNRISE; t <= SUNSET; t += 0.25f) {
            float[] colour = skyRenderer.getSunColour(t, SUNRISE, SUNSET);
            for (int i = 0; i < 3; i++) {
                assertTrue(colour[i] >= 0f && colour[i] <= 1f,
                        "Colour component " + i + " out of range at t=" + t + ": " + colour[i]);
            }
        }
    }

    // -----------------------------------------------------------------------
    // getCloudCount
    // -----------------------------------------------------------------------

    @Test
    void cloudCount_isPositive() {
        assertTrue(skyRenderer.getCloudCount() > 0, "There must be at least one cloud cluster");
    }

    @Test
    void cloudCount_isReasonable() {
        int count = skyRenderer.getCloudCount();
        assertTrue(count >= 4 && count <= 20,
                "Cloud count should be between 4 and 20, got: " + count);
    }

    // -----------------------------------------------------------------------
    // update
    // -----------------------------------------------------------------------

    @Test
    void update_doesNotThrow() {
        // Just verify no exception is thrown when updating the renderer
        assertDoesNotThrow(() -> skyRenderer.update(1.0f / 60f));
    }

    @Test
    void update_accumulatesTime() {
        // getSunScreenX and getSunElevation are deterministic on game time, not
        // cloudTime, so we just verify update doesn't corrupt state.
        skyRenderer.update(10f);
        float elev = skyRenderer.getSunElevation(NOON, SUNRISE, SUNSET);
        assertEquals(1.0f, elev, 0.01f, "Elevation still correct after time advance");
    }
}

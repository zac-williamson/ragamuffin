package ragamuffin.render;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

/**
 * Renders sky elements — sun disk and clouds — as part of the skybox.
 *
 * The sun is drawn as a filled circle near the top of the screen, positioned
 * horizontally based on the time of day and vertically based on the sun's
 * elevation above the horizon.  Its colour transitions through dawn/day/dusk
 * colours matching the sky colour system.
 *
 * Clouds are drawn as clusters of overlapping filled circles that scroll slowly
 * over time, giving a sense of atmospheric depth without expensive 3D geometry.
 *
 * Both elements are rendered into the skybox layer (before the 3D world geometry)
 * so that 3D objects naturally occlude them.  Use {@link #renderSkybox} each frame
 * immediately after clearing the colour buffer and before starting the 3D
 * model batch.  The caller must clear the depth buffer after this call so that
 * 3D geometry renders correctly on top.
 *
 * Both elements are rendered using ShapeRenderer so no extra textures are needed.
 */
public class SkyRenderer {

    // Number of cloud clusters in the sky
    private static final int CLOUD_COUNT = 8;

    // Horizontal scroll speed (fraction of screen width per second)
    private static final float CLOUD_SCROLL_SPEED = 0.004f;

    private float cloudTime = 0f;

    /**
     * Create a SkyRenderer with staggered initial cloud positions.
     */
    public SkyRenderer() {
        // Nothing heavy to initialise — ShapeRenderer is supplied per-frame.
    }

    /**
     * Update the internal time counter (call once per frame with delta seconds).
     */
    public void update(float delta) {
        cloudTime += delta;
    }

    /**
     * Render sun and clouds as part of the skybox background.
     *
     * Must be called BEFORE the 3D world is rendered (before modelBatch.begin())
     * so that 3D geometry naturally appears in front of sky elements.  The caller
     * is responsible for clearing the depth buffer after this call (but before
     * the model batch) so that 3D objects render correctly on top of the sky.
     *
     * @param shapeRenderer shared ShapeRenderer (not currently between begin/end)
     * @param time          current game time in hours (0–24)
     * @param sunrise       today's sunrise time in hours
     * @param sunset        today's sunset time in hours
     * @param screenWidth   current screen width in pixels
     * @param screenHeight  current screen height in pixels
     * @param isNight       true when it is currently night-time
     */
    public void renderSkybox(ShapeRenderer shapeRenderer,
                       float time, float sunrise, float sunset,
                       int screenWidth, int screenHeight,
                       boolean isNight) {

        Matrix4 ortho = new Matrix4();
        ortho.setToOrtho2D(0, 0, screenWidth, screenHeight);
        shapeRenderer.setProjectionMatrix(ortho);

        // Enable alpha blending so the semi-transparent clouds look soft.
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(
                com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Draw sun only during the day
        if (!isNight) {
            renderSun(shapeRenderer, time, sunrise, sunset, screenWidth, screenHeight);
        }

        // Draw clouds (visible day and night, but faded at night)
        renderClouds(shapeRenderer, time, sunrise, sunset, screenWidth, screenHeight, isNight);

        com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
    }

    // -----------------------------------------------------------------------
    // Sun
    // -----------------------------------------------------------------------

    private void renderSun(ShapeRenderer shapeRenderer,
                           float time, float sunrise, float sunset,
                           int screenWidth, int screenHeight) {

        float noon = (sunrise + sunset) / 2f;
        float halfDay = (sunset - sunrise) / 2f;

        // Normalised position along the day arc (0 at sunrise, 0.5 at noon, 1 at sunset)
        float dayFraction = (time - sunrise) / (sunset - sunrise);
        dayFraction = clamp(dayFraction, 0f, 1f);

        // Horizontal: arc left (east) to right (west) across the upper screen
        float sunX = dayFraction * screenWidth;

        // Vertical: parabolic arc — highest at noon, lower near horizon at dawn/dusk.
        // sunElevation goes 0→1→0 over the day.
        float sunElevation = (float) Math.sin(dayFraction * Math.PI);
        // Map to screen Y: horizon is ~30 % from top, peak is ~80 % from bottom = 20 % from top
        float horizonY = screenHeight * 0.70f; // where sun rises/sets
        float peakY    = screenHeight * 0.85f; // highest point (noon)
        float sunY = horizonY + (peakY - horizonY) * sunElevation;

        // Sun colour: orange at dawn/dusk, bright yellow-white at midday
        float[] sunColour = getSunColour(time, sunrise, sunset);
        float sr = sunColour[0], sg = sunColour[1], sb = sunColour[2];

        float radius = 22f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Soft glow — large translucent halo
        shapeRenderer.setColor(sr, sg, sb, 0.15f);
        shapeRenderer.circle(sunX, sunY, radius * 2.5f, 32);

        // Mid glow
        shapeRenderer.setColor(sr, sg, sb, 0.30f);
        shapeRenderer.circle(sunX, sunY, radius * 1.5f, 32);

        // Main sun disk
        shapeRenderer.setColor(sr, sg, sb, 1.0f);
        shapeRenderer.circle(sunX, sunY, radius, 32);

        shapeRenderer.end();
    }

    /**
     * Returns [r, g, b] for the sun at the given time.
     * Deep orange at sunrise/sunset, bright yellow-white at noon.
     */
    public float[] getSunColour(float time, float sunrise, float sunset) {
        float noon = (sunrise + sunset) / 2f;

        // Distance from noon as a fraction of half-day (0 = noon, 1 = horizon)
        float distFromNoon = Math.abs(time - noon) / ((sunset - sunrise) / 2f);
        distFromNoon = clamp(distFromNoon, 0f, 1f);

        // Noon: bright yellow-white (1.0, 0.98, 0.85)
        // Horizon: deep orange-red (1.0, 0.55, 0.20)
        float r = lerp(1.00f, 1.00f, distFromNoon);
        float g = lerp(0.98f, 0.55f, distFromNoon);
        float b = lerp(0.85f, 0.20f, distFromNoon);

        return new float[]{r, g, b};
    }

    // -----------------------------------------------------------------------
    // Clouds
    // -----------------------------------------------------------------------

    private void renderClouds(ShapeRenderer shapeRenderer,
                              float time, float sunrise, float sunset,
                              int screenWidth, int screenHeight,
                              boolean isNight) {

        // Cloud brightness: full white during day, faint grey at night
        float brightness = isNight ? 0.45f : 0.95f;

        // Cloud alpha: more opaque during overcast-looking times, lighter at noon
        // Small variation by time to make them feel alive
        float baseAlpha = 0.72f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < CLOUD_COUNT; i++) {
            // Each cloud has a unique starting offset spread across the screen
            float seedX = (i * 137.508f) % 1.0f; // golden ratio spread
            float seedY = (i * 73.111f) % 1.0f;

            // Clouds occupy the upper 35 % of the screen
            float cloudBaseX = ((seedX + cloudTime * CLOUD_SCROLL_SPEED) % 1.0f) * screenWidth;
            // Wrap around so clouds re-enter from the left
            if (cloudBaseX < 0) cloudBaseX += screenWidth;
            float cloudBaseY = screenHeight * (0.78f + seedY * 0.17f); // 78 %–95 % up

            // Each cloud is made of 5–7 overlapping circles
            int puffs = 5 + (i % 3);
            float cloudWidth = screenWidth * (0.06f + seedX * 0.06f); // 6 %–12 % of screen
            float puffRadius = cloudWidth * 0.28f;

            for (int p = 0; p < puffs; p++) {
                float puffSeed = (p * 53.7f + i * 19.3f) % 1.0f;
                float px = cloudBaseX + (p / (float) puffs - 0.5f) * cloudWidth;
                float py = cloudBaseY + (puffSeed - 0.5f) * puffRadius * 0.8f;
                float pr = puffRadius * (0.75f + puffSeed * 0.5f);

                // Slight shadow on bottom puffs for depth
                float shadowFactor = 1.0f - puffSeed * 0.15f;
                float cr = brightness * shadowFactor;
                float cg = brightness * shadowFactor;
                float cb = brightness * shadowFactor;
                float ca = baseAlpha * (0.8f + puffSeed * 0.2f);

                shapeRenderer.setColor(cr, cg, cb, ca);
                shapeRenderer.circle(px, py, pr, 20);
            }
        }

        shapeRenderer.end();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Get the sun's horizontal position as a fraction of the screen width (0–1).
     * Returns 0 when the sun is at the east horizon, 1 when at the west horizon.
     * Returns -1 if the sun is below the horizon (night time).
     */
    public float getSunScreenX(float time, float sunrise, float sunset) {
        if (time < sunrise || time > sunset) {
            return -1f;
        }
        return clamp((time - sunrise) / (sunset - sunrise), 0f, 1f);
    }

    /**
     * Get the sun's elevation factor (0 = on horizon, 1 = at noon peak).
     * Returns 0 at night.
     */
    public float getSunElevation(float time, float sunrise, float sunset) {
        if (time < sunrise || time > sunset) {
            return 0f;
        }
        float dayFraction = (time - sunrise) / (sunset - sunrise);
        return (float) Math.sin(dayFraction * Math.PI);
    }

    /**
     * Returns how many cloud clusters are rendered.
     */
    public int getCloudCount() {
        return CLOUD_COUNT;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }
}

package ragamuffin.render;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

/**
 * Renders sky elements — sun disk, clouds, and at night: moon, stars, and
 * planets — as part of the skybox.
 *
 * The sun is drawn as a filled circle near the top of the screen, positioned
 * horizontally based on the time of day and vertically based on the sun's
 * elevation above the horizon.  Its colour transitions through dawn/day/dusk
 * colours matching the sky colour system.
 *
 * Clouds are drawn as clusters of overlapping filled circles that scroll slowly
 * over time, giving a sense of atmospheric depth without expensive 3D geometry.
 *
 * At night, stars are rendered as small dots distributed across the sky dome,
 * the moon is drawn with the correct phase for the day of year, and the five
 * naked-eye planets are shown as brighter points whose positions shift with
 * the season.  Star and planet positions change with the day of year so that
 * the sky looks different across seasons.
 *
 * All elements are rendered into the skybox layer (before the 3D world geometry)
 * so that 3D objects naturally occlude them.  Use {@link #renderSkybox} each frame
 * immediately after clearing the colour buffer and before starting the 3D
 * model batch.  The caller must clear the depth buffer after this call so that
 * 3D geometry renders correctly on top.
 *
 * All elements are rendered using ShapeRenderer so no extra textures are needed.
 */
public class SkyRenderer {

    // Number of cloud clusters in the sky
    private static final int CLOUD_COUNT = 8;

    // Horizontal scroll speed (fraction of screen width per second)
    private static final float CLOUD_SCROLL_SPEED = 0.004f;

    // Number of stars in the night sky
    private static final int STAR_COUNT = 200;

    // Synodic period of the moon in days (new moon → new moon)
    private static final float LUNAR_CYCLE_DAYS = 29.53059f;

    // Reference new moon at day 0 of the year (day offset chosen so phases
    // look plausible across seasons — not an exact ephemeris anchor).
    private static final float NEW_MOON_REFERENCE_DAY = 5.0f;

    // Names of the five naked-eye planets (for documentation; order matches
    // the planet data arrays below).
    public static final String[] PLANET_NAMES = {"Mercury", "Venus", "Mars", "Jupiter", "Saturn"};

    // Orbital periods in Earth days (approximate, for visual position cycling)
    private static final float[] PLANET_PERIODS = {87.97f, 224.70f, 686.97f, 4332.59f, 10759.22f};

    // Base sky-position angles (degrees, 0-360) at day 0, spreading planets
    // across the ecliptic so they appear at different parts of the sky.
    private static final float[] PLANET_BASE_ANGLES = {30f, 110f, 200f, 290f, 60f};

    // Visual sizes (relative) — Venus > Jupiter > Mars > Saturn > Mercury
    private static final float[] PLANET_SIZES = {1.5f, 3.5f, 2.5f, 3.0f, 2.0f};

    // Colours [r, g, b] — rough naked-eye hues
    private static final float[][] PLANET_COLOURS = {
        {0.9f, 0.85f, 0.75f},  // Mercury — pale yellowish
        {1.0f, 1.0f, 0.80f},   // Venus   — brilliant white-yellow
        {1.0f, 0.55f, 0.35f},  // Mars    — reddish-orange
        {0.9f, 0.85f, 0.70f},  // Jupiter — cream/white
        {0.9f, 0.85f, 0.60f},  // Saturn  — pale gold
    };

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
     * The camera yaw is used to keep the skybox stationary relative to the world:
     * the sun and clouds are offset on screen based on which direction the player
     * is facing, so they do not appear to move when the player looks around.
     *
     * @param shapeRenderer  shared ShapeRenderer (not currently between begin/end)
     * @param time           current game time in hours (0–24)
     * @param sunrise        today's sunrise time in hours
     * @param sunset         today's sunset time in hours
     * @param screenWidth    current screen width in pixels
     * @param screenHeight   current screen height in pixels
     * @param isNight        true when it is currently night-time
     * @param cameraYawDeg   camera yaw in degrees (0 = facing -Z/north, 90 = east, 180 = south)
     */
    public void renderSkybox(ShapeRenderer shapeRenderer,
                       float time, float sunrise, float sunset,
                       int screenWidth, int screenHeight,
                       boolean isNight, float cameraYawDeg) {
        renderSkybox(shapeRenderer, time, sunrise, sunset, screenWidth, screenHeight,
                     isNight, cameraYawDeg, 0);
    }

    /**
     * Render sun, clouds, and (at night) stars, moon, and planets.
     *
     * This overload accepts the day of year so that star/planet positions and
     * moon phase are updated correctly as the in-game calendar advances.
     *
     * @param shapeRenderer  shared ShapeRenderer (not currently between begin/end)
     * @param time           current game time in hours (0–24)
     * @param sunrise        today's sunrise time in hours
     * @param sunset         today's sunset time in hours
     * @param screenWidth    current screen width in pixels
     * @param screenHeight   current screen height in pixels
     * @param isNight        true when it is currently night-time
     * @param cameraYawDeg   camera yaw in degrees (0 = facing -Z/north, 90 = east, 180 = south)
     * @param dayOfYear      day of the year (0–364) used for seasonal sky positions
     */
    public void renderSkybox(ShapeRenderer shapeRenderer,
                       float time, float sunrise, float sunset,
                       int screenWidth, int screenHeight,
                       boolean isNight, float cameraYawDeg,
                       int dayOfYear) {

        Matrix4 ortho = new Matrix4();
        ortho.setToOrtho2D(0, 0, screenWidth, screenHeight);
        shapeRenderer.setProjectionMatrix(ortho);

        // Enable alpha blending so the semi-transparent clouds look soft.
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(
                com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        if (isNight) {
            // Night sky: stars first (furthest back), then planets, then moon
            renderStars(shapeRenderer, screenWidth, screenHeight, cameraYawDeg, dayOfYear);
            renderPlanets(shapeRenderer, screenWidth, screenHeight, cameraYawDeg, dayOfYear);
            renderMoon(shapeRenderer, time, sunrise, sunset, screenWidth, screenHeight,
                       cameraYawDeg, dayOfYear);
        } else {
            // Draw sun only during the day
            renderSun(shapeRenderer, time, sunrise, sunset, screenWidth, screenHeight, cameraYawDeg);
        }

        // Draw clouds (visible day and night, but faded at night)
        renderClouds(shapeRenderer, time, sunrise, sunset, screenWidth, screenHeight, isNight, cameraYawDeg);

        com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
    }

    // -----------------------------------------------------------------------
    // Sun
    // -----------------------------------------------------------------------

    private void renderSun(ShapeRenderer shapeRenderer,
                           float time, float sunrise, float sunset,
                           int screenWidth, int screenHeight,
                           float cameraYawDeg) {

        // Normalised position along the day arc (0 at sunrise, 0.5 at noon, 1 at sunset)
        float dayFraction = (time - sunrise) / (sunset - sunrise);
        dayFraction = clamp(dayFraction, 0f, 1f);

        // The sun's world yaw: rises in the east (90°), crosses south (180°) at noon,
        // sets in the west (270°).
        float sunWorldYaw = 90f + dayFraction * 180f;

        // Angular difference between camera yaw and sun yaw, in degrees.
        // Positive offset = sun is to the right of screen centre.
        float yawDiff = sunWorldYaw - cameraYawDeg;
        // Normalise to [-180, 180]
        while (yawDiff >  180f) yawDiff -= 360f;
        while (yawDiff < -180f) yawDiff += 360f;

        // Map yaw difference to screen offset.  A 90° difference puts the sun
        // exactly one screen-width off centre (so it disappears off the edge).
        float sunX = screenWidth * 0.5f + (yawDiff / 90f) * screenWidth;

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
                              boolean isNight, float cameraYawDeg) {

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

            // Clouds have a fixed world yaw position (0–360°) that scrolls slowly over time
            float cloudWorldYaw = (seedX * 360f + cloudTime * CLOUD_SCROLL_SPEED * 360f) % 360f;

            // Angular difference between camera yaw and cloud yaw — same projection as sun
            float yawDiff = cloudWorldYaw - cameraYawDeg;
            while (yawDiff >  180f) yawDiff -= 360f;
            while (yawDiff < -180f) yawDiff += 360f;

            // Clouds occupy the upper 35 % of the screen
            float cloudBaseX = screenWidth * 0.5f + (yawDiff / 90f) * screenWidth;
            // Wrap around so clouds tile across the sky
            cloudBaseX = ((cloudBaseX % screenWidth) + screenWidth) % screenWidth;
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
    // Night sky: stars
    // -----------------------------------------------------------------------

    private void renderStars(ShapeRenderer shapeRenderer,
                             int screenWidth, int screenHeight,
                             float cameraYawDeg, int dayOfYear) {

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < STAR_COUNT; i++) {
            // Each star has a fixed celestial longitude (world yaw 0–360°).
            // The longitude drifts slightly with day of year — the ecliptic
            // shifts ~1°/day — so the star field rotates slowly across seasons,
            // placing different constellations overhead at different times of year.
            float baseLon = (i * 137.508f) % 360f;           // golden-ratio spread
            float seasonalShift = (dayOfYear * 0.9856f) % 360f; // ~1°/day sidereal drift
            float starWorldYaw = (baseLon - seasonalShift + 360f) % 360f;

            float yawDiff = starWorldYaw - cameraYawDeg;
            while (yawDiff >  180f) yawDiff -= 360f;
            while (yawDiff < -180f) yawDiff += 360f;

            // Only draw stars within roughly 90° of the camera direction
            if (Math.abs(yawDiff) > 95f) continue;

            float starX = screenWidth * 0.5f + (yawDiff / 90f) * screenWidth;

            // Distribute stars across the upper 60 % of the screen (the sky dome)
            float seedY  = ((i * 53.7f + 17.3f) % 1000f) / 1000f;
            float starY  = screenHeight * (0.40f + seedY * 0.55f);

            // Twinkle: vary brightness slightly with a pseudo-random phase
            float phase  = (i * 0.41f + cloudTime * (0.3f + (i % 7) * 0.05f)) % (2f * (float)Math.PI);
            float twinkle = 0.75f + 0.25f * (float)Math.cos(phase);

            // Vary star brightness: most are dim, a few are bright
            float baseBrightness = ((i * 29.3f + 3.7f) % 10f < 1f) ? 0.95f :
                                   ((i * 29.3f + 3.7f) % 10f < 3f) ? 0.75f : 0.55f;
            float brightness = baseBrightness * twinkle;

            // Slightly warm/cool tints for variety
            float tintR = 1.0f, tintG = 1.0f, tintB = 1.0f;
            int tintBucket = i % 6;
            if (tintBucket == 0) { tintB = 1.0f; tintR = 0.80f; tintG = 0.85f; } // blue-white
            else if (tintBucket == 1) { tintR = 1.0f; tintG = 0.85f; tintB = 0.75f; } // warm orange

            float radius = (baseBrightness > 0.9f) ? 2.5f : (baseBrightness > 0.7f) ? 1.8f : 1.2f;

            shapeRenderer.setColor(brightness * tintR, brightness * tintG, brightness * tintB, brightness);
            shapeRenderer.circle(starX, starY, radius, 6);
        }

        shapeRenderer.end();
    }

    // -----------------------------------------------------------------------
    // Night sky: moon
    // -----------------------------------------------------------------------

    private void renderMoon(ShapeRenderer shapeRenderer,
                            float time, float sunrise, float sunset,
                            int screenWidth, int screenHeight,
                            float cameraYawDeg, int dayOfYear) {

        // Moon rises in the east and sets in the west, roughly opposite the sun.
        // During the night it moves from east (rise at sunset) to west (set at sunrise).
        // We approximate: moon rises at sunset and sets at the next sunrise.

        // Night fraction: 0 when the sun sets, 1 when the sun rises again.
        float nightLength = (24f - sunset) + sunrise; // hours of darkness
        float timeIntoNight;
        if (time >= sunset) {
            timeIntoNight = time - sunset;
        } else {
            // After midnight, before sunrise
            timeIntoNight = (24f - sunset) + time;
        }
        float nightFraction = clamp(timeIntoNight / nightLength, 0f, 1f);

        // Moon world yaw: rises east (90°), transits south (180°), sets west (270°)
        float moonWorldYaw = 90f + nightFraction * 180f;

        float yawDiff = moonWorldYaw - cameraYawDeg;
        while (yawDiff >  180f) yawDiff -= 360f;
        while (yawDiff < -180f) yawDiff += 360f;

        float moonX = screenWidth * 0.5f + (yawDiff / 90f) * screenWidth;

        // Vertical: arc across the sky
        float moonElevation = (float) Math.sin(nightFraction * Math.PI);
        float horizonY = screenHeight * 0.70f;
        float peakY    = screenHeight * 0.85f;
        float moonY = horizonY + (peakY - horizonY) * moonElevation;

        float moonRadius = 14f;

        // ---- Moon phase ----
        // Phase angle in [0, 2π]: 0 = new moon, π = full moon
        float daysSinceNewMoon = (dayOfYear - NEW_MOON_REFERENCE_DAY + LUNAR_CYCLE_DAYS * 10)
                                  % LUNAR_CYCLE_DAYS;
        float phaseAngle = (daysSinceNewMoon / LUNAR_CYCLE_DAYS) * 2f * (float) Math.PI;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw the full lit moon disk (white-grey)
        shapeRenderer.setColor(0.92f, 0.92f, 0.88f, 0.95f);
        shapeRenderer.circle(moonX, moonY, moonRadius, 32);

        // Draw the shadow overlay to create the phase illusion.
        // The shadow is a semi-circular overlay: we approximate it by drawing
        // a dark circle offset from the moon centre.  The amount of lit surface
        // visible (illuminated fraction) is: (1 - cos(phaseAngle)) / 2.
        float illuminatedFraction = (1f - (float) Math.cos(phaseAngle)) / 2f;

        // Shadow coverage: 1 = fully dark (new moon), 0 = fully lit (full moon)
        float shadowCoverage = 1f - illuminatedFraction;

        if (shadowCoverage > 0.02f) {
            // Shadow offset: negative (left/waxing) or positive (right/waning)
            // Phase 0→π: waxing, shadow on left; phase π→2π: waning, shadow on right
            float offsetSign = (phaseAngle < Math.PI) ? -1f : 1f;
            // Shadow offset scales from moonRadius (new) to 0 (full) and back
            float shadowOffset = offsetSign * (float) Math.cos(phaseAngle) * moonRadius;

            shapeRenderer.setColor(0.05f, 0.05f, 0.12f, 0.95f);
            shapeRenderer.circle(moonX + shadowOffset, moonY, moonRadius, 32);
        }

        shapeRenderer.end();
    }

    /**
     * Returns the moon's illuminated fraction (0 = new moon, 1 = full moon)
     * for the given day of the year.
     */
    public float getMoonPhase(int dayOfYear) {
        float daysSinceNewMoon = (dayOfYear - NEW_MOON_REFERENCE_DAY + LUNAR_CYCLE_DAYS * 10)
                                  % LUNAR_CYCLE_DAYS;
        float phaseAngle = (daysSinceNewMoon / LUNAR_CYCLE_DAYS) * 2f * (float) Math.PI;
        return (1f - (float) Math.cos(phaseAngle)) / 2f;
    }

    /**
     * Returns the moon phase name for the given day of the year.
     * One of: "new", "waxing crescent", "first quarter", "waxing gibbous",
     * "full", "waning gibbous", "last quarter", "waning crescent".
     */
    public String getMoonPhaseName(int dayOfYear) {
        float daysSinceNewMoon = (dayOfYear - NEW_MOON_REFERENCE_DAY + LUNAR_CYCLE_DAYS * 10)
                                  % LUNAR_CYCLE_DAYS;
        float fraction = daysSinceNewMoon / LUNAR_CYCLE_DAYS; // 0–1
        if (fraction < 0.0625f || fraction >= 0.9375f) return "new";
        if (fraction < 0.1875f)  return "waxing crescent";
        if (fraction < 0.3125f)  return "first quarter";
        if (fraction < 0.4375f)  return "waxing gibbous";
        if (fraction < 0.5625f)  return "full";
        if (fraction < 0.6875f)  return "waning gibbous";
        if (fraction < 0.8125f)  return "last quarter";
        return "waning crescent";
    }

    // -----------------------------------------------------------------------
    // Night sky: planets
    // -----------------------------------------------------------------------

    private void renderPlanets(ShapeRenderer shapeRenderer,
                               int screenWidth, int screenHeight,
                               float cameraYawDeg, int dayOfYear) {

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < PLANET_NAMES.length; i++) {
            float planetWorldYaw = getPlanetWorldYaw(i, dayOfYear);

            float yawDiff = planetWorldYaw - cameraYawDeg;
            while (yawDiff >  180f) yawDiff -= 360f;
            while (yawDiff < -180f) yawDiff += 360f;

            if (Math.abs(yawDiff) > 95f) continue;

            float px = screenWidth * 0.5f + (yawDiff / 90f) * screenWidth;

            // Planets sit along the ecliptic band: upper-middle part of the sky
            float eclipticSeed = ((i * 73.1f + 41.3f) % 100f) / 100f;
            float py = screenHeight * (0.60f + eclipticSeed * 0.25f);

            float[] col = PLANET_COLOURS[i];
            float size  = PLANET_SIZES[i];

            // Soft glow
            shapeRenderer.setColor(col[0], col[1], col[2], 0.25f);
            shapeRenderer.circle(px, py, size * 2.2f, 12);

            // Main disk
            shapeRenderer.setColor(col[0], col[1], col[2], 0.95f);
            shapeRenderer.circle(px, py, size, 12);
        }

        shapeRenderer.end();
    }

    /**
     * Returns the world yaw (0–360°) of planet {@code planetIndex} on the
     * given day of the year.  Planets cycle around the sky with their
     * approximate orbital period, starting from the base angle at day 0.
     *
     * @param planetIndex  index into PLANET_NAMES (0=Mercury … 4=Saturn)
     * @param dayOfYear    0–364
     * @return yaw in degrees [0, 360)
     */
    public float getPlanetWorldYaw(int planetIndex, int dayOfYear) {
        float degreesPerDay = 360f / PLANET_PERIODS[planetIndex];
        return (PLANET_BASE_ANGLES[planetIndex] + dayOfYear * degreesPerDay) % 360f;
    }

    /**
     * Returns the number of planets rendered in the night sky.
     */
    public int getPlanetCount() {
        return PLANET_NAMES.length;
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

package ragamuffin.ui;

import com.badlogic.gdx.math.Vector3;

/**
 * Cinematic camera for the city fly-through cut-scene played when a new game starts.
 *
 * The camera follows a pre-defined path through the British town, sweeping over key
 * landmarks before descending to the player's spawn position at the park centre.
 *
 * Issue #373: Add opening cinematic cut-scene with city fly-through.
 */
public class CinematicCamera {

    /** Total duration of the fly-through in seconds. */
    public static final float DURATION = 8.0f;

    /** Waypoints for the camera path: (x, y, z, lookAtX, lookAtY, lookAtZ, arrivalTime). */
    private static final float[][] WAYPOINTS = {
        // Start: high above the industrial estate, looking south over the town
        { -70f, 40f, -60f,   0f, 5f,  0f,  0.0f },
        // Sweep east over the job centre and high street
        { -55f, 25f,  30f,  40f, 5f, 20f,  2.5f },
        // Glide over Greggs / shops area
        {  40f, 18f,  25f,   0f, 5f, -5f,  5.0f },
        // Descend toward the park centre (spawn)
        {   5f,  9f,   8f,   0f, 3f,  0f,  7.0f },
        // Final position: eye height at spawn
        {   0f,  4f,   0f,   0f, 3f, -1f,  8.0f },
    };

    private boolean active;
    private boolean completed;
    private float timer;

    // Current interpolated camera position and look-at target
    private final Vector3 position = new Vector3();
    private final Vector3 lookAt = new Vector3();

    public CinematicCamera() {
        this.active = false;
        this.completed = false;
        this.timer = 0f;
    }

    /** Start the cinematic fly-through. */
    public void start() {
        active = true;
        completed = false;
        timer = 0f;
        // Initialise position to the first waypoint immediately
        interpolate(0f);
    }

    /** Skip the cinematic — jump straight to the final waypoint. */
    public void skip() {
        active = false;
        completed = true;
        interpolate(DURATION);
    }

    /**
     * Update the cinematic timer and interpolate the camera position.
     *
     * @param delta seconds elapsed since last frame
     */
    public void update(float delta) {
        if (!active) {
            return;
        }
        timer += delta;
        if (timer >= DURATION) {
            timer = DURATION;
            active = false;
            completed = true;
        }
        interpolate(timer);
    }

    /**
     * Compute the interpolated camera position and look-at target for the given time.
     * Uses linear interpolation between adjacent waypoints.
     */
    private void interpolate(float t) {
        // Find the two waypoints that bracket time t
        int fromIdx = 0;
        int toIdx = 1;
        for (int i = 0; i < WAYPOINTS.length - 1; i++) {
            if (t >= WAYPOINTS[i][6] && t <= WAYPOINTS[i + 1][6]) {
                fromIdx = i;
                toIdx = i + 1;
                break;
            }
            if (t > WAYPOINTS[i + 1][6]) {
                fromIdx = i + 1;
                toIdx = i + 1; // clamp to last
            }
        }

        float[] from = WAYPOINTS[fromIdx];
        float[] to   = WAYPOINTS[toIdx];

        float segStart = from[6];
        float segEnd   = to[6];
        float alpha = (segEnd > segStart) ? (t - segStart) / (segEnd - segStart) : 1f;
        alpha = Math.max(0f, Math.min(1f, alpha));

        // Smooth step for easing
        alpha = alpha * alpha * (3f - 2f * alpha);

        position.set(
            lerp(from[0], to[0], alpha),
            lerp(from[1], to[1], alpha),
            lerp(from[2], to[2], alpha)
        );
        lookAt.set(
            lerp(from[3], to[3], alpha),
            lerp(from[4], to[4], alpha),
            lerp(from[5], to[5], alpha)
        );
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** @return true while the cinematic is playing. */
    public boolean isActive() {
        return active;
    }

    /** @return true once the cinematic has finished (or been skipped). */
    public boolean isCompleted() {
        return completed;
    }

    /** Current elapsed time in seconds. */
    public float getTimer() {
        return timer;
    }

    /** Interpolated world-space position for the camera origin. */
    public Vector3 getPosition() {
        return position;
    }

    /** Interpolated world-space look-at target for the camera. */
    public Vector3 getLookAt() {
        return lookAt;
    }

    /**
     * Render a black letterbox overlay with a "Press any key to skip" hint.
     * Called each frame while the cinematic is active.
     *
     * @param spriteBatch  LibGDX SpriteBatch (already has ortho projection set)
     * @param shapeRenderer LibGDX ShapeRenderer
     * @param font         BitmapFont for the skip hint
     * @param screenWidth  viewport width in pixels
     * @param screenHeight viewport height in pixels
     */
    public void render(com.badlogic.gdx.graphics.g2d.SpriteBatch spriteBatch,
                       com.badlogic.gdx.graphics.glutils.ShapeRenderer shapeRenderer,
                       com.badlogic.gdx.graphics.g2d.BitmapFont font,
                       int screenWidth, int screenHeight) {
        if (!active) {
            return;
        }

        int barHeight = screenHeight / 8;

        // Letterbox bars (top and bottom)
        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 1f);
        shapeRenderer.rect(0, screenHeight - barHeight, screenWidth, barHeight); // top
        shapeRenderer.rect(0, 0, screenWidth, barHeight);                         // bottom
        shapeRenderer.end();

        // Fade in / fade out overlay
        float fadeIn  = Math.min(1f, timer / 1.0f);
        float fadeOut = (timer > DURATION - 1.0f) ? Math.max(0f, 1f - (DURATION - timer)) : 0f;
        float fade    = Math.max(fadeIn == 1f ? 0f : (1f - fadeIn), fadeOut);
        if (fade > 0f) {
            shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, fade);
            shapeRenderer.rect(0, 0, screenWidth, screenHeight);
            shapeRenderer.end();
        }

        // Skip hint (bottom bar)
        spriteBatch.begin();
        font.getData().setScale(0.9f);
        font.setColor(0.6f, 0.6f, 0.6f, Math.min(1f, timer * 2f));
        String hint = "Press any key to skip";
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout =
                new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, hint);
        font.draw(spriteBatch, hint,
                (screenWidth - layout.width) / 2f,
                barHeight / 2f + layout.height / 2f);
        font.getData().setScale(1.2f);
        font.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        spriteBatch.end();
    }
}

package ragamuffin.render;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.List;
import ragamuffin.world.FlagPosition;

/**
 * Renders animated flags in the 3D world.
 *
 * Each flag is mounted on a pole and rendered as a coloured rectangular panel
 * with a rippling wave animation driven by elapsed game time.  The flag is
 * drawn in screen-space (via projection) so it always faces the camera and
 * remains legible from any angle.
 *
 * Flags are placed atop IRON_FENCE poles by WorldGenerator at key civic
 * buildings (council offices, police station, fire station, school, etc.).
 */
public class FlagRenderer {

    /** Maximum render distance for flags (blocks). */
    private static final float MAX_RENDER_DISTANCE = 60f;

    /** Number of horizontal segments used to draw the waving flag. */
    private static final int WAVE_SEGMENTS = 6;

    /** Base flag width in screen pixels at full scale (1 block away). */
    private static final float BASE_FLAG_WIDTH = 32f;
    /** Base flag height in screen pixels at full scale. */
    private static final float BASE_FLAG_HEIGHT = 20f;

    /** Wave amplitude as a fraction of flag height. */
    private static final float WAVE_AMPLITUDE = 0.25f;
    /** Wave frequency (full cycles per flag width). */
    private static final float WAVE_FREQUENCY = 1.5f;
    /** Wave travel speed (cycles per second). */
    private static final float WAVE_SPEED = 1.2f;

    private float time = 0f;

    private final List<FlagPosition> flags = new ArrayList<>();

    // Scratch vector for screen-space projection
    private final Vector3 tmpScreen = new Vector3();

    /** Register all flag positions.  Call once after world generation. */
    public void setFlags(List<FlagPosition> flags) {
        this.flags.clear();
        this.flags.addAll(flags);
    }

    /** Get the current list of flag positions (for testing). */
    public List<FlagPosition> getFlags() {
        return java.util.Collections.unmodifiableList(flags);
    }

    /**
     * Advance the flag animation timer.
     * @param delta elapsed time in seconds since the last frame
     */
    public void update(float delta) {
        time += delta;
    }

    /**
     * Render all visible flags.
     *
     * Must be called after modelBatch.end() but before the main 2D UI pass.
     * The ShapeRenderer must not already be active when this method is called.
     *
     * @param shapeRenderer the shape renderer used to draw the flag panels
     * @param camera        the current perspective camera (used for world→screen projection)
     * @param screenWidth   current screen width in pixels
     * @param screenHeight  current screen height in pixels
     */
    public void render(ShapeRenderer shapeRenderer,
                       PerspectiveCamera camera,
                       int screenWidth,
                       int screenHeight) {

        for (FlagPosition flag : flags) {
            tmpScreen.set(flag.getWorldX(), flag.getWorldY(), flag.getWorldZ());

            // Distance cull — skip flags that are too far away
            float dist = camera.position.dst(tmpScreen);
            if (dist > MAX_RENDER_DISTANCE) continue;

            // Project to screen space
            camera.project(tmpScreen, 0, 0, screenWidth, screenHeight);

            // Skip flags that are behind the camera
            if (tmpScreen.z > 1.0f || tmpScreen.z < 0f) continue;

            float baseX = tmpScreen.x;
            float baseY = tmpScreen.y;

            // Scale by distance so near flags appear larger
            float scale = Math.max(0.3f, 1.0f - dist / MAX_RENDER_DISTANCE);
            float fw = BASE_FLAG_WIDTH * scale;
            float fh = BASE_FLAG_HEIGHT * scale;

            // Skip if entirely off-screen
            if (baseX + fw < 0 || baseX > screenWidth) continue;
            if (baseY - fh > screenHeight || baseY < 0) continue;

            drawAnimatedFlag(shapeRenderer, flag, baseX, baseY, fw, fh, scale);
        }
    }

    /**
     * Draw a single animated flag panel at the given screen coordinates.
     *
     * The flag is divided into WAVE_SEGMENTS vertical strips.  Each strip is
     * offset vertically by a sine wave that advances with the flag's phase
     * offset and the current time, creating the illusion of fabric rippling
     * in the wind.
     */
    private void drawAnimatedFlag(ShapeRenderer shapeRenderer,
                                  FlagPosition flag,
                                  float baseX, float baseY,
                                  float fw, float fh,
                                  float scale) {

        float segW = fw / WAVE_SEGMENTS;
        float amp = fh * WAVE_AMPLITUDE;
        float phaseOffset = flag.getPhaseOffset();

        float r1 = flag.getColorR1(), g1 = flag.getColorG1(), b1 = flag.getColorB1();
        float r2 = flag.getColorR2(), g2 = flag.getColorG2(), b2 = flag.getColorB2();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < WAVE_SEGMENTS; i++) {
            float tLeft  = (float) i       / WAVE_SEGMENTS;
            float tRight = (float)(i + 1)  / WAVE_SEGMENTS;

            // Compute wave Y offsets for left and right edges of this strip
            float waveLeft  = waveOffset(tLeft,  phaseOffset, amp);
            float waveRight = waveOffset(tRight, phaseOffset, amp);

            float x0 = baseX + i * segW;
            float x1 = baseX + (i + 1) * segW;

            // Top-left, bottom-left of strip (flag hangs downward from pole top)
            float y0tl = baseY + waveLeft;
            float y0bl = baseY + waveLeft  - fh;
            float y0tr = baseY + waveRight;
            float y0br = baseY + waveRight - fh;

            // Blend between the two flag colours horizontally
            float t = (tLeft + tRight) * 0.5f;
            float cr = r1 + (r2 - r1) * t;
            float cg = g1 + (g2 - g1) * t;
            float cb = b1 + (b2 - b1) * t;

            // Shade the bottom edge slightly darker for depth
            float topShade   = 1.0f;
            float btmShade   = 0.75f;

            // Draw quad as two triangles (LibGDX ShapeRenderer.triangle)
            shapeRenderer.setColor(cr * topShade, cg * topShade, cb * topShade, 1f);
            shapeRenderer.triangle(x0, y0tl, x1, y0tr, x1, y0br);
            shapeRenderer.setColor(cr * btmShade, cg * btmShade, cb * btmShade, 1f);
            shapeRenderer.triangle(x0, y0tl, x1, y0br, x0, y0bl);
        }

        shapeRenderer.end();

        // Draw a thin dark outline for the pole attachment / flag edge
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 0.8f);
        // Left (hoist) edge
        float waveL0 = waveOffset(0f, phaseOffset, amp);
        float waveL1 = waveOffset(1f, phaseOffset, amp);
        shapeRenderer.line(baseX, baseY + waveL0, baseX, baseY + waveL0 - fh);
        shapeRenderer.end();
    }

    /**
     * Compute the vertical wave offset for a point at normalised horizontal
     * position {@code t} (0 = hoist/pole side, 1 = fly/free end).
     *
     * The wave is anchored at the pole (t=0, offset=0) and grows in amplitude
     * toward the free end, giving the correct physics feel of fabric streaming
     * from a fixed point.
     */
    private float waveOffset(float t, float phaseOffset, float amp) {
        float angle = t * WAVE_FREQUENCY * MathUtils.PI2
                      - time * WAVE_SPEED * MathUtils.PI2
                      + phaseOffset;
        // Amplitude increases from 0 at the hoist to full at the fly end
        return MathUtils.sin(angle) * amp * t;
    }
}

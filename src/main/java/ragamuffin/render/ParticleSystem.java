package ragamuffin.render;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Simple CPU particle system for visual feedback on combat and movement actions.
 *
 * Particles are rendered as 2D screen-space dots using ShapeRenderer.  This keeps
 * the implementation lightweight (no extra textures or 3D geometry) while still
 * giving the player satisfying visual feedback.
 *
 * Supported emitter types:
 *   - COMBAT_HIT   : small red/orange sparks when the player punches a block or NPC
 *   - BLOCK_BREAK  : block-coloured debris when a block is destroyed
 *   - FOOTSTEP_DUST: tiny grey puffs emitted at the player's feet while moving
 *   - DODGE_TRAIL  : blue-white streaks emitted behind the player during a dodge roll
 */
public class ParticleSystem {

    /** How many particles the pool can hold before oldest are evicted. */
    private static final int MAX_PARTICLES = 512;

    // -----------------------------------------------------------------------
    // Particle data (struct-of-arrays style for cache friendliness)
    // -----------------------------------------------------------------------

    private static class Particle {
        // World-space position
        float x, y, z;
        // Velocity (world-space units per second)
        float vx, vy, vz;
        // Colour
        float r, g, b, a;
        // Total lifetime and remaining life (seconds)
        float lifetime;
        float life;
        // Screen-space radius at spawn (pixels)
        float radius;
        // Whether this slot is active
        boolean active;
    }

    private final List<Particle> particles = new ArrayList<>();

    // Scratch vector for screen projection
    private final Vector3 tmpScreen = new Vector3();

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Emit a burst of combat-hit sparks at the given world-space position.
     * Typically called when the player punches a block or NPC.
     */
    public void emitCombatHit(float wx, float wy, float wz) {
        int count = 8;
        for (int i = 0; i < count; i++) {
            Particle p = acquire();
            p.x = wx; p.y = wy; p.z = wz;
            p.vx = MathUtils.random(-3f, 3f);
            p.vy = MathUtils.random(1f, 5f);
            p.vz = MathUtils.random(-3f, 3f);
            // Red-orange palette
            p.r = MathUtils.random(0.8f, 1.0f);
            p.g = MathUtils.random(0.2f, 0.5f);
            p.b = 0f;
            p.a = 1.0f;
            p.lifetime = MathUtils.random(0.25f, 0.55f);
            p.life = p.lifetime;
            p.radius = MathUtils.random(3f, 6f);
            p.active = true;
        }
    }

    /**
     * Emit block-break debris at the given world-space position, using the
     * supplied colour (matching the block type's colour).
     */
    public void emitBlockBreak(float wx, float wy, float wz, float r, float g, float b) {
        int count = 12;
        for (int i = 0; i < count; i++) {
            Particle p = acquire();
            p.x = wx + MathUtils.random(-0.3f, 0.3f);
            p.y = wy + MathUtils.random(0f, 0.5f);
            p.z = wz + MathUtils.random(-0.3f, 0.3f);
            p.vx = MathUtils.random(-4f, 4f);
            p.vy = MathUtils.random(2f, 6f);
            p.vz = MathUtils.random(-4f, 4f);
            p.r = r; p.g = g; p.b = b;
            p.a = 1.0f;
            p.lifetime = MathUtils.random(0.3f, 0.7f);
            p.life = p.lifetime;
            p.radius = MathUtils.random(2f, 5f);
            p.active = true;
        }
    }

    /**
     * Emit a small dust puff at the player's feet while walking.
     * Should be called periodically (e.g. every 0.3 s of movement) rather than every frame.
     */
    public void emitFootstepDust(float wx, float wy, float wz) {
        int count = 4;
        for (int i = 0; i < count; i++) {
            Particle p = acquire();
            p.x = wx + MathUtils.random(-0.2f, 0.2f);
            p.y = wy;
            p.z = wz + MathUtils.random(-0.2f, 0.2f);
            p.vx = MathUtils.random(-0.8f, 0.8f);
            p.vy = MathUtils.random(0.3f, 1.2f);
            p.vz = MathUtils.random(-0.8f, 0.8f);
            // Grey dust
            float grey = MathUtils.random(0.55f, 0.75f);
            p.r = grey; p.g = grey; p.b = grey;
            p.a = 0.7f;
            p.lifetime = MathUtils.random(0.4f, 0.8f);
            p.life = p.lifetime;
            p.radius = MathUtils.random(2f, 4f);
            p.active = true;
        }
    }

    /**
     * Emit a dodge-trail streak at the player's current world position.
     * Should be called each frame while the player is dodging.
     */
    public void emitDodgeTrail(float wx, float wy, float wz) {
        int count = 3;
        for (int i = 0; i < count; i++) {
            Particle p = acquire();
            p.x = wx + MathUtils.random(-0.1f, 0.1f);
            p.y = wy + MathUtils.random(0f, 1f);
            p.z = wz + MathUtils.random(-0.1f, 0.1f);
            p.vx = MathUtils.random(-0.5f, 0.5f);
            p.vy = MathUtils.random(0f, 0.5f);
            p.vz = MathUtils.random(-0.5f, 0.5f);
            // Blue-white trail
            p.r = MathUtils.random(0.5f, 1.0f);
            p.g = MathUtils.random(0.7f, 1.0f);
            p.b = 1.0f;
            p.a = 0.8f;
            p.lifetime = MathUtils.random(0.15f, 0.35f);
            p.life = p.lifetime;
            p.radius = MathUtils.random(2f, 5f);
            p.active = true;
        }
    }

    /**
     * Advance particle simulation by {@code delta} seconds.
     * Must be called once per frame before {@link #render}.
     */
    public void update(float delta) {
        for (Particle p : particles) {
            if (!p.active) continue;
            p.life -= delta;
            if (p.life <= 0f) {
                p.active = false;
                continue;
            }
            // Simple physics: gravity pull
            p.vy -= 9.8f * delta;
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.z += p.vz * delta;
            // Fade alpha linearly
            p.a = p.life / p.lifetime;
        }
    }

    /**
     * Render all active particles as 2D screen-space dots.
     *
     * @param shapeRenderer a ShapeRenderer already configured with an orthographic
     *                      (screen-space) projection matrix — must NOT be in a begin/end block
     * @param camera        the active perspective camera used to project world coordinates
     * @param screenWidth   current screen width in pixels
     * @param screenHeight  current screen height in pixels
     */
    public void render(ShapeRenderer shapeRenderer,
                       com.badlogic.gdx.graphics.PerspectiveCamera camera,
                       int screenWidth, int screenHeight) {
        if (particles.isEmpty()) return;

        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(
            com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
            com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Particle p : particles) {
            if (!p.active) continue;

            // Project world → screen
            tmpScreen.set(p.x, p.y, p.z);
            camera.project(tmpScreen, 0, 0, screenWidth, screenHeight);

            // Skip if behind the camera
            if (tmpScreen.z > 1.0f || tmpScreen.z < 0f) continue;

            float sx = tmpScreen.x;
            float sy = tmpScreen.y;

            // Scale radius by remaining life so particles shrink as they die
            float lifeFrac = p.a; // already normalised 0-1
            float drawRadius = p.radius * (0.5f + 0.5f * lifeFrac);

            shapeRenderer.setColor(p.r, p.g, p.b, p.a);
            shapeRenderer.circle(sx, sy, drawRadius, 6);
        }

        shapeRenderer.end();

        com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
    }

    /** Return the number of currently active particles (for testing / debugging). */
    public int getActiveCount() {
        int count = 0;
        for (Particle p : particles) {
            if (p.active) count++;
        }
        return count;
    }

    /** Remove all active particles (e.g. on state transitions). */
    public void clear() {
        for (Particle p : particles) {
            p.active = false;
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Acquire a free particle slot, creating a new one if under the pool limit,
     * or recycling the oldest active particle when the pool is full.
     */
    private Particle acquire() {
        // Try to reuse a dead slot first
        for (Particle p : particles) {
            if (!p.active) return p;
        }
        // Pool not full — create a new slot
        if (particles.size() < MAX_PARTICLES) {
            Particle p = new Particle();
            particles.add(p);
            return p;
        }
        // Pool full — evict the oldest (first active) particle
        for (Particle p : particles) {
            if (p.active) {
                p.active = false;
                return p;
            }
        }
        // Fallback (should not happen): add new beyond limit
        Particle p = new Particle();
        particles.add(p);
        return p;
    }
}

package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tracks the player's noise level (0.0–1.0) based on movement state and actions.
 *
 * Noise sources:
 *   - Walking upright: 0.6 base
 *   - Crouching:       0.2 base
 *   - Standing still:  0.05 ambient
 *   - Block break:     spike to 1.0, decay at 0.5/s
 *   - Block place:     spike to 0.7, decay at 0.5/s
 *
 * Decay rate toward baseline: 0.5/s linearly.
 *
 * Used by NPCManager for hearing-based detection (360° range = 5 + noise×15 blocks).
 */
public class NoiseSystem {

    // Baseline noise levels by movement state
    public static final float NOISE_STILL    = 0.05f;
    public static final float NOISE_CROUCH   = 0.2f;
    public static final float NOISE_WALK     = 0.6f;

    // Spike values for actions
    public static final float NOISE_BLOCK_BREAK = 1.0f;
    public static final float NOISE_BLOCK_PLACE = 0.7f;

    /** Issue #781: Noise added per outdoor graffiti tag placed. */
    public static final float NOISE_GRAFFITI = 0.1f;

    // Decay rate per second (toward baseline)
    private static final float DECAY_RATE = 0.5f;

    // Block-break spike duration in seconds (at full 1.0 spike, 2s to fully decay if no movement)
    public static final float BLOCK_BREAK_SPIKE_DURATION = 2.0f;

    private float noiseLevel;
    private float spikeTimer;  // remaining spike duration; 0 = no spike active

    // Current spike level (saved so we can decay from it over spikeTimer)
    private float spikeLevel;

    // ── Issue #940: World-position noise sources (e.g. burning bins) ─────────

    /**
     * A persistent noise source at a world position with a given level.
     * Used by WheeliBinFireSystem and similar environmental noise emitters.
     */
    public static class NoiseSource {
        public final Vector3 position;
        public float level;
        public float duration; // remaining lifetime; -1 = permanent (until removed)

        public NoiseSource(Vector3 position, float level, float duration) {
            this.position = new Vector3(position);
            this.level = level;
            this.duration = duration;
        }
    }

    /** Active world-position noise sources (e.g. burning bins). */
    private final List<NoiseSource> noiseSources = new ArrayList<>();

    public NoiseSystem() {
        this.noiseLevel = NOISE_STILL;
        this.spikeTimer = 0f;
        this.spikeLevel = 0f;
    }

    /**
     * Update noise each frame.
     *
     * @param delta      frame delta seconds
     * @param isMoving   whether the player is moving horizontally this frame
     * @param isCrouching whether the player is crouching this frame
     */
    public void update(float delta, boolean isMoving, boolean isCrouching) {
        float baseline;
        if (isMoving) {
            baseline = isCrouching ? NOISE_CROUCH : NOISE_WALK;
        } else {
            baseline = NOISE_STILL;
        }

        // If a spike is active, decay the spike toward baseline over spikeTimer
        if (spikeTimer > 0) {
            spikeTimer = Math.max(0f, spikeTimer - delta);
            // Linear decay from spikeLevel down to baseline over BLOCK_BREAK_SPIKE_DURATION
            float spikePct = spikeTimer / BLOCK_BREAK_SPIKE_DURATION;
            float spikeContrib = (spikeLevel - baseline) * spikePct;
            noiseLevel = baseline + Math.max(0f, spikeContrib);
        } else {
            // No spike — move toward baseline at DECAY_RATE
            if (noiseLevel > baseline) {
                noiseLevel = Math.max(baseline, noiseLevel - DECAY_RATE * delta);
            } else {
                noiseLevel = Math.min(baseline, noiseLevel + DECAY_RATE * delta);
            }
        }

        // Clamp to [0,1]
        noiseLevel = Math.max(0f, Math.min(1f, noiseLevel));
    }

    /**
     * Spike noise to 1.0 immediately (block break event).
     * The spike decays over BLOCK_BREAK_SPIKE_DURATION seconds.
     */
    public void spikeBlockBreak() {
        if (NOISE_BLOCK_BREAK > noiseLevel) {
            noiseLevel = NOISE_BLOCK_BREAK;
        }
        spikeLevel = NOISE_BLOCK_BREAK;
        spikeTimer = BLOCK_BREAK_SPIKE_DURATION;
    }

    /**
     * Spike noise to 0.7 immediately (block place event).
     * Decays over BLOCK_BREAK_SPIKE_DURATION seconds.
     */
    public void spikeBlockPlace() {
        if (NOISE_BLOCK_PLACE > noiseLevel) {
            noiseLevel = NOISE_BLOCK_PLACE;
        }
        // Only override the spike timer if this produces a louder spike
        if (NOISE_BLOCK_PLACE >= spikeLevel) {
            spikeLevel = NOISE_BLOCK_PLACE;
            spikeTimer = BLOCK_BREAK_SPIKE_DURATION;
        }
    }

    /**
     * Add a small noise delta (e.g. for graffiti placement: NOISE_GRAFFITI = 0.1).
     * Does not create a spike; noise decays normally after the call.
     *
     * @param amount amount to add (clamped to [0, 1])
     */
    public void addNoise(float amount) {
        noiseLevel = Math.min(1f, noiseLevel + amount);
    }

    /**
     * Get the current noise level (0.0 = silent, 1.0 = maximum).
     */
    public float getNoiseLevel() {
        return noiseLevel;
    }

    /**
     * Set the noise level directly (for testing).
     */
    public void setNoiseLevel(float level) {
        this.noiseLevel = Math.max(0f, Math.min(1f, level));
        this.spikeTimer = 0f;
        this.spikeLevel = 0f;
    }

    /**
     * Get the hearing detection range for a given noise level.
     * Base range: 5 + (noise × 15) blocks.
     */
    public static float getHearingRange(float noiseLevel) {
        return 5f + noiseLevel * 15f;
    }

    // ── Issue #940: World-position noise emission ──────────────────────────

    /**
     * Emit a persistent noise at a world position (e.g. a burning bin).
     * The noise source persists until removed via {@link #removeNoiseAt(Vector3)}.
     * Also raises the player noise level immediately so NPCManager can hear it.
     *
     * @param position world position of the noise source
     * @param level    noise level (0.0–1.0)
     */
    public void emitNoise(Vector3 position, float level) {
        noiseSources.add(new NoiseSource(position, level, -1f));
        // Also spike the player's perceived noise level so NPC hearing works
        addNoise(level);
    }

    /**
     * Remove the nearest noise source to the given position (within 1 block).
     * Called when a burning bin is extinguished or burned out.
     *
     * @param position world position of the noise source to remove
     */
    public void removeNoiseAt(Vector3 position) {
        noiseSources.removeIf(src -> src.position.dst(position) < 1.0f);
    }

    /**
     * Get the maximum noise level at a given world position.
     * Returns the level of any noise source within 1 block, or 0 if none.
     *
     * @param x world X coordinate
     * @param y world Y coordinate
     * @param z world Z coordinate
     * @return noise level at the position (0.0–1.0)
     */
    public float getNoiseLevel(float x, float y, float z) {
        float maxLevel = 0f;
        Vector3 query = new Vector3(x, y, z);
        for (NoiseSource src : noiseSources) {
            if (src.position.dst(query) < 1.0f) {
                maxLevel = Math.max(maxLevel, src.level);
            }
        }
        return maxLevel;
    }

    /**
     * Get all active world-position noise sources.
     */
    public List<NoiseSource> getNoiseSources() {
        return noiseSources;
    }
}

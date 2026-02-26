package ragamuffin.core;

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

    // Decay rate per second (toward baseline)
    private static final float DECAY_RATE = 0.5f;

    // Block-break spike duration in seconds (at full 1.0 spike, 2s to fully decay if no movement)
    public static final float BLOCK_BREAK_SPIKE_DURATION = 2.0f;

    private float noiseLevel;
    private float spikeTimer;  // remaining spike duration; 0 = no spike active

    // Current spike level (saved so we can decay from it over spikeTimer)
    private float spikeLevel;

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
}

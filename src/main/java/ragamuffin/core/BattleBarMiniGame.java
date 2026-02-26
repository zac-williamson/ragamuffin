package ragamuffin.core;

import java.util.Random;

/**
 * Issue #716: Underground Music Scene — BattleBar Mini-game.
 *
 * <h3>Overview</h3>
 * A timing-based mini-game used during MC Battles. A cursor slides back and forth
 * across a bar; the player must press the action key when the cursor overlaps the
 * "hit zone" — a highlighted region of the bar. Each round of the battle runs one
 * BattleBar attempt.
 *
 * <h3>Bar Layout</h3>
 * <pre>
 *   |──────────────[ HIT ZONE ]──────────────|
 *   0.0                                      1.0   (normalised position)
 * </pre>
 *
 * <ul>
 *   <li>Cursor starts at 0.0 and oscillates at {@link #cursorSpeed} units/sec.</li>
 *   <li>Hit zone is a random window of width {@link #hitZoneWidth} centred somewhere
 *       between 0.15 and 0.85.</li>
 *   <li>Pressing action when cursor is inside the hit zone registers a <em>hit</em>.</li>
 *   <li>Missing (pressing outside, or letting the bar time out) registers a
 *       <em>miss</em>.</li>
 *   <li>The bar automatically times out after {@link #ROUND_TIMEOUT_SECONDS} seconds
 *       if the player does not press anything.</li>
 * </ul>
 *
 * <h3>Difficulty Scaling</h3>
 * <ul>
 *   <li>Marchetti MC (off-licence): Easy   — wide hit zone, slow cursor</li>
 *   <li>Street Lads MC (park):      Medium — medium hit zone and speed</li>
 *   <li>Council MC (JobCentre):     Hard   — narrow hit zone, fast cursor</li>
 * </ul>
 */
public class BattleBarMiniGame {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Total normalised width of the bar (0.0 – 1.0). */
    public static final float BAR_WIDTH = 1.0f;

    /** Timeout in real seconds before the round automatically counts as a miss. */
    public static final float ROUND_TIMEOUT_SECONDS = 4.0f;

    // ── Difficulty presets ────────────────────────────────────────────────────

    /** Easy preset: wide hit zone, slow cursor. */
    public static final float EASY_HIT_ZONE_WIDTH  = 0.35f;
    public static final float EASY_CURSOR_SPEED    = 0.40f;

    /** Medium preset: standard width and speed. */
    public static final float MEDIUM_HIT_ZONE_WIDTH = 0.22f;
    public static final float MEDIUM_CURSOR_SPEED   = 0.60f;

    /** Hard preset: narrow hit zone, fast cursor. */
    public static final float HARD_HIT_ZONE_WIDTH  = 0.14f;
    public static final float HARD_CURSOR_SPEED    = 0.85f;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Normalised cursor position [0.0, 1.0]. */
    private float cursorPos;

    /** Direction the cursor is moving (+1 = right, −1 = left). */
    private int cursorDir;

    /** Units per second the cursor travels across the bar. */
    private final float cursorSpeed;

    /** Start of the hit zone (normalised). */
    private final float hitZoneStart;

    /** Width of the hit zone (normalised). */
    private final float hitZoneWidth;

    /** Elapsed time for this round (seconds). */
    private float elapsed;

    /** Whether this round has been resolved (hit, miss, or timeout). */
    private boolean resolved;

    /** Result of this round (only valid when {@link #resolved} is true). */
    private boolean wasHit;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Create a new BattleBar round with explicit parameters.
     *
     * @param hitZoneWidth width of the hit zone (0.0–1.0)
     * @param cursorSpeed  cursor travel speed (units/second)
     * @param rng          random number generator for hit-zone placement
     */
    public BattleBarMiniGame(float hitZoneWidth, float cursorSpeed, Random rng) {
        this.hitZoneWidth = hitZoneWidth;
        this.cursorSpeed  = cursorSpeed;

        // Place hit zone centre randomly between 0.15 and 0.85
        float margin     = hitZoneWidth / 2f;
        float minCentre  = 0.15f + margin;
        float maxCentre  = 0.85f - margin;
        float centre     = minCentre + rng.nextFloat() * (maxCentre - minCentre);
        this.hitZoneStart = centre - margin;

        this.cursorPos = 0f;
        this.cursorDir = 1;
        this.elapsed   = 0f;
        this.resolved  = false;
        this.wasHit    = false;
    }

    // ── Factory helpers ───────────────────────────────────────────────────────

    /** Create an Easy-difficulty round (Marchetti MC). */
    public static BattleBarMiniGame easy(Random rng) {
        return new BattleBarMiniGame(EASY_HIT_ZONE_WIDTH, EASY_CURSOR_SPEED, rng);
    }

    /** Create a Medium-difficulty round (Street Lads MC). */
    public static BattleBarMiniGame medium(Random rng) {
        return new BattleBarMiniGame(MEDIUM_HIT_ZONE_WIDTH, MEDIUM_CURSOR_SPEED, rng);
    }

    /** Create a Hard-difficulty round (Council MC). */
    public static BattleBarMiniGame hard(Random rng) {
        return new BattleBarMiniGame(HARD_HIT_ZONE_WIDTH, HARD_CURSOR_SPEED, rng);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Advance the mini-game by {@code delta} seconds.
     * Call this each frame while the mini-game is active.
     *
     * @param delta seconds since last frame
     */
    public void update(float delta) {
        if (resolved) return;

        elapsed += delta;
        if (elapsed >= ROUND_TIMEOUT_SECONDS) {
            // Timed out — automatic miss
            resolved = true;
            wasHit   = false;
            return;
        }

        // Move cursor
        cursorPos += cursorDir * cursorSpeed * delta;
        if (cursorPos >= BAR_WIDTH) {
            cursorPos = BAR_WIDTH;
            cursorDir = -1;
        } else if (cursorPos <= 0f) {
            cursorPos = 0f;
            cursorDir = 1;
        }
    }

    /**
     * The player presses the action key — resolve this round.
     * Has no effect if the round is already resolved.
     *
     * @return {@code true} if the cursor was inside the hit zone (hit), {@code false} otherwise (miss)
     */
    public boolean press() {
        if (resolved) return wasHit;
        resolved = true;
        wasHit   = cursorPos >= hitZoneStart && cursorPos <= (hitZoneStart + hitZoneWidth);
        return wasHit;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** @return current cursor position [0.0, 1.0] */
    public float getCursorPos() { return cursorPos; }

    /** @return normalised start of the hit zone */
    public float getHitZoneStart() { return hitZoneStart; }

    /** @return normalised width of the hit zone */
    public float getHitZoneWidth() { return hitZoneWidth; }

    /** @return elapsed time for this round (seconds) */
    public float getElapsed() { return elapsed; }

    /** @return {@code true} once this round has been resolved */
    public boolean isResolved() { return resolved; }

    /** @return {@code true} if the player hit the zone (only valid when {@link #isResolved()}) */
    public boolean wasHit() { return wasHit; }

    /** @return {@code true} if the round timed out without a player press */
    public boolean isTimedOut() { return resolved && !wasHit && elapsed >= ROUND_TIMEOUT_SECONDS; }
}

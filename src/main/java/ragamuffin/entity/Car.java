package ragamuffin.entity;

import com.badlogic.gdx.math.Vector3;

/**
 * A car that drives along road blocks in the British town.
 * Cars move at a fixed speed along a straight road segment.  When they reach a
 * road intersection they may turn left or right instead of continuing straight.
 * They slow to a stop when another obstacle is detected ahead (collision
 * avoidance) and resume once the path is clear.
 *
 * Heading convention: 0° = +Z, 90° = +X, 180° = −Z, 270° = −X.
 */
public class Car {
    /** Width of the car hitbox (fits within a 2-block-wide ROAD lane). */
    public static final float WIDTH  = 1.6f;
    /** Height of the car body. */
    public static final float HEIGHT = 1.4f;
    /** Depth (length) of the car along its direction of travel. */
    public static final float DEPTH  = 3.2f;
    /** Default driving speed in blocks/sec. */
    public static final float DEFAULT_SPEED = 6.0f;
    /** Damage dealt to the player per collision. */
    public static final float COLLISION_DAMAGE = 15.0f;

    /** How far ahead (blocks) to look for obstacles when doing collision avoidance. */
    public static final float LOOK_AHEAD_DISTANCE = 4.0f;

    /** Road grid spacing — must match WorldGenerator / CarManager. */
    public static final int ROAD_GRID_SPACING = 20;

    /** Colour variants for car body colours — chosen deterministically from spawn position. */
    public enum CarColour {
        RED, BLUE, WHITE, SILVER, BLACK, YELLOW
    }

    /**
     * Heading of the car in degrees.
     * 0 = travelling in +Z, 90 = +X, 180 = -Z, 270 = -X.
     */
    private float heading; // degrees

    private final Vector3 position;
    private final Vector3 velocity;
    private final AABB aabb;

    /** End-point A of the current road segment (lower coordinate on travel axis). */
    private float segmentMin;
    /** End-point B of the current road segment (higher coordinate on travel axis). */
    private float segmentMax;

    private float speed;
    private final CarColour colour;

    /** When true the car is waiting for the path ahead to clear. */
    private boolean stopped = false;

    /** Cooldown in seconds before this car can damage the player again. */
    private float damageCooldown = 0f;
    private static final float DAMAGE_COOLDOWN_DURATION = 1.5f;

    /**
     * Create a car on a road segment.
     *
     * @param x            Initial X position (centre of car)
     * @param y            Initial Y position (bottom of car)
     * @param z            Initial Z position (centre of car)
     * @param travelAlongX true = car starts moving in +X direction; false = +Z direction
     * @param segmentMin   Lower bound of the road segment on the travel axis
     * @param segmentMax   Upper bound of the road segment on the travel axis
     * @param colour       Body colour
     */
    public Car(float x, float y, float z,
               boolean travelAlongX,
               float segmentMin, float segmentMax,
               CarColour colour) {
        this.position   = new Vector3(x, y, z);
        this.velocity   = new Vector3();
        this.segmentMin = segmentMin;
        this.segmentMax = segmentMax;
        this.colour     = colour;
        this.speed      = DEFAULT_SPEED;
        this.aabb       = new AABB(position, WIDTH, HEIGHT, DEPTH);

        // Start moving in the positive direction on the chosen axis
        this.heading = travelAlongX ? 90f : 0f;
        applyHeadingToVelocity();
    }

    // ── Heading helpers ───────────────────────────────────────────────────────

    /** Apply the current heading angle to the velocity vector at full speed. */
    private void applyHeadingToVelocity() {
        double rad = Math.toRadians(heading);
        velocity.set((float) Math.sin(rad) * speed, 0f, (float) Math.cos(rad) * speed);
    }

    /**
     * True if the car is currently aligned to the X axis (heading ≈ 90° or 270°).
     * Used by the renderer for model orientation and by tests.
     */
    public boolean isTravellingAlongX() {
        float h = ((heading % 360f) + 360f) % 360f;
        return h > 45f && h <= 135f || h > 225f && h <= 315f;
    }

    /**
     * Returns the car's current heading in degrees.
     * 0 = +Z, 90 = +X, 180 = -Z, 270 = -X.
     */
    public float getHeading() {
        return heading;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Update car position and direction each frame.
     * Cars bounce between segmentMin and segmentMax on their travel axis.
     * If {@code stopped} is true the car does not move.
     *
     * @param delta seconds since last frame
     */
    public void update(float delta) {
        if (!stopped) {
            // Move car along heading
            position.add(velocity.x * delta, 0f, velocity.z * delta);

            // Bounce at segment endpoints on the travel axis
            float coord = isTravellingAlongX() ? position.x : position.z;
            if (coord >= segmentMax) {
                if (isTravellingAlongX()) {
                    position.x = segmentMax;
                } else {
                    position.z = segmentMax;
                }
                reverseDirection();
            } else if (coord <= segmentMin) {
                if (isTravellingAlongX()) {
                    position.x = segmentMin;
                } else {
                    position.z = segmentMin;
                }
                reverseDirection();
            }
        }

        // Update AABB
        aabb.setPosition(position, WIDTH, HEIGHT, DEPTH);

        // Tick damage cooldown
        if (damageCooldown > 0f) {
            damageCooldown -= delta;
        }
    }

    /**
     * Attempt to turn the car onto a perpendicular road at an intersection.
     * The intersection coordinates are the X and Z positions of the crossing.
     * Returns true if the turn was accepted and the car's heading/segment changed.
     *
     * @param newHeading   the new heading (degrees) — must be perpendicular to current
     * @param intersectX   X world-coord of the intersection centre
     * @param intersectZ   Z world-coord of the intersection centre
     * @param newSegMin    lower bound on the new travel axis
     * @param newSegMax    upper bound on the new travel axis
     */
    public boolean turn(float newHeading, float intersectX, float intersectZ,
                        float newSegMin, float newSegMax) {
        // Snap position to the intersection centre
        position.x = intersectX;
        position.z = intersectZ;
        heading = newHeading;
        segmentMin = newSegMin;
        segmentMax = newSegMax;
        stopped = false;
        applyHeadingToVelocity();
        aabb.setPosition(position, WIDTH, HEIGHT, DEPTH);
        return true;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Vector3 getPosition() { return position; }
    public Vector3 getVelocity() { return velocity; }
    public AABB    getAABB()     { return aabb; }
    public CarColour getColour() { return colour; }

    public float getSegmentMin() { return segmentMin; }
    public float getSegmentMax() { return segmentMax; }

    /** True when the car is moving in the positive direction on its travel axis. */
    public boolean isMovingPositive() {
        return isTravellingAlongX() ? velocity.x > 0 : velocity.z > 0;
    }

    /** True when the car is stopped (collision avoidance hold). */
    public boolean isStopped() { return stopped; }

    /** Set the stopped state (used by CarManager for collision avoidance). */
    public void setStopped(boolean stopped) {
        this.stopped = stopped;
        if (stopped) {
            velocity.set(0f, 0f, 0f);
        } else {
            applyHeadingToVelocity();
        }
    }

    /** True when the car can currently deal damage to the player. */
    public boolean canDamagePlayer() { return damageCooldown <= 0f; }

    /** Reset the damage cooldown after a player hit. */
    public void resetDamageCooldown() {
        damageCooldown = DAMAGE_COOLDOWN_DURATION;
    }

    /**
     * Reverse the car's direction of travel (heading flips 180°).
     * Called when the car collides with a solid block or reaches a segment endpoint.
     */
    public void reverseDirection() {
        heading = (heading + 180f) % 360f;
        applyHeadingToVelocity();
    }
}

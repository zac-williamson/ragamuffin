package ragamuffin.entity;

import com.badlogic.gdx.math.Vector3;

/**
 * A car that drives along road blocks in the British town.
 * Cars move at a fixed speed along a straight road segment, reversing direction
 * at the segment endpoints. They damage the player on collision.
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

    /** Colour variants for car body colours — chosen deterministically from spawn position. */
    public enum CarColour {
        RED, BLUE, WHITE, SILVER, BLACK, YELLOW
    }

    /** Whether the car is travelling along the X axis (true) or Z axis (false). */
    private final boolean travelAlongX;

    private final Vector3 position;
    private final Vector3 velocity;
    private final AABB aabb;

    /** End-point A of the road segment (lower coordinate on travel axis). */
    private final float segmentMin;
    /** End-point B of the road segment (higher coordinate on travel axis). */
    private final float segmentMax;

    private float speed;
    private final CarColour colour;

    /** Cooldown in seconds before this car can damage the player again. */
    private float damageCooldown = 0f;
    private static final float DAMAGE_COOLDOWN_DURATION = 1.5f;

    /**
     * Create a car on a road segment.
     *
     * @param x            Initial X position (centre of car)
     * @param y            Initial Y position (bottom of car)
     * @param z            Initial Z position (centre of car)
     * @param travelAlongX true = car moves in X direction; false = Z direction
     * @param segmentMin   Lower bound of the road segment on the travel axis
     * @param segmentMax   Upper bound of the road segment on the travel axis
     * @param colour       Body colour
     */
    public Car(float x, float y, float z,
               boolean travelAlongX,
               float segmentMin, float segmentMax,
               CarColour colour) {
        this.position      = new Vector3(x, y, z);
        this.velocity      = new Vector3();
        this.travelAlongX  = travelAlongX;
        this.segmentMin    = segmentMin;
        this.segmentMax    = segmentMax;
        this.colour        = colour;
        this.speed         = DEFAULT_SPEED;
        this.aabb          = new AABB(position, WIDTH, HEIGHT, DEPTH);

        // Start moving in the positive direction
        if (travelAlongX) {
            velocity.set(speed, 0, 0);
        } else {
            velocity.set(0, 0, speed);
        }
    }

    /**
     * Update car position and direction each frame.
     * Cars bounce between segmentMin and segmentMax on their travel axis.
     *
     * @param delta seconds since last frame
     */
    public void update(float delta) {
        // Move car
        position.add(velocity.x * delta, 0, velocity.z * delta);

        // Bounce at segment endpoints
        float coord = travelAlongX ? position.x : position.z;
        if (coord >= segmentMax) {
            if (travelAlongX) {
                position.x = segmentMax;
                velocity.set(-speed, 0, 0);
            } else {
                position.z = segmentMax;
                velocity.set(0, 0, -speed);
            }
        } else if (coord <= segmentMin) {
            if (travelAlongX) {
                position.x = segmentMin;
                velocity.set(speed, 0, 0);
            } else {
                position.z = segmentMin;
                velocity.set(0, 0, speed);
            }
        }

        // Update AABB
        aabb.setPosition(position, WIDTH, HEIGHT, DEPTH);

        // Tick damage cooldown
        if (damageCooldown > 0f) {
            damageCooldown -= delta;
        }
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public Vector3 getPosition() {
        return position;
    }

    public Vector3 getVelocity() {
        return velocity;
    }

    public AABB getAABB() {
        return aabb;
    }

    public CarColour getColour() {
        return colour;
    }

    public boolean isTravellingAlongX() {
        return travelAlongX;
    }

    public float getSegmentMin() {
        return segmentMin;
    }

    public float getSegmentMax() {
        return segmentMax;
    }

    /**
     * Returns true if this car can currently deal damage to the player
     * (i.e. the per-hit cooldown has expired).
     */
    public boolean canDamagePlayer() {
        return damageCooldown <= 0f;
    }

    /**
     * Called when the car hits the player — resets the damage cooldown so the
     * same car cannot deal damage again for {@value #DAMAGE_COOLDOWN_DURATION} seconds.
     */
    public void resetDamageCooldown() {
        damageCooldown = DAMAGE_COOLDOWN_DURATION;
    }

    /** Returns true when the car is moving in the positive direction on its axis. */
    public boolean isMovingPositive() {
        return travelAlongX ? velocity.x > 0 : velocity.z > 0;
    }

    /**
     * Reverse the car's direction of travel.
     * Called when the car collides with a solid block so it bounces back rather
     * than driving through the obstacle.
     */
    public void reverseDirection() {
        if (travelAlongX) {
            velocity.set(-velocity.x, 0, 0);
        } else {
            velocity.set(0, 0, -velocity.z);
        }
    }
}

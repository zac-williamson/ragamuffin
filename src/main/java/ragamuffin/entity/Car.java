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

    // ── Van dimensions (taller, longer, wider) ─────────────────────────────
    public static final float VAN_WIDTH  = 2.0f;
    public static final float VAN_HEIGHT = 2.2f;
    public static final float VAN_DEPTH  = 4.5f;
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
     * Condition rating of the car, visible during inspection at Wheelwright Motors.
     * Determines haggling thresholds and resale price in CarDealershipSystem (Issue #1227).
     * <ul>
     *   <li>MINT — showroom condition, full price; haggle threshold 80%.</li>
     *   <li>TIDY — minor wear; haggle threshold 80%.</li>
     *   <li>ROUGH — visible wear, some issues; haggle threshold 60%. Can be clocked.</li>
     *   <li>BANGER — poor condition, MOT borderline; haggle threshold 60%. Can be clocked.</li>
     * </ul>
     */
    public enum CarCondition {
        MINT, TIDY, ROUGH, BANGER
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

    /** True if this vehicle is a van (larger dimensions, different model). */
    private final boolean van;

    /** When true the car is waiting for the path ahead to clear. */
    private boolean stopped = false;

    /** Vertical velocity (blocks/sec, positive = up) used for gravity simulation. */
    private float verticalVelocity = 0f;

    /** Gravitational acceleration applied to vehicles (blocks/sec²). */
    public static final float GRAVITY = 9.8f;

    /** Cooldown in seconds before this car can damage the player again. */
    private float damageCooldown = 0f;
    private static final float DAMAGE_COOLDOWN_DURATION = 1.5f;

    /** True when the player is currently driving this car. */
    private boolean drivenByPlayer = false;

    /**
     * True when this car is parked (no AI driver — the engine is off).
     * Parked cars can be stolen by the player without a driver getting out.
     * Moving (non-parked) cars have an NPC driver who will exit when the
     * player interacts with the vehicle (Issue #991).
     */
    private boolean parked = false;

    /** Maximum speed when driven by the player (blocks/sec). */
    public static final float PLAYER_MAX_SPEED = 12.0f;

    /** Acceleration rate when the player accelerates (blocks/sec²). */
    public static final float PLAYER_ACCELERATION = 8.0f;

    /** Deceleration / braking rate (blocks/sec²). */
    public static final float PLAYER_DECELERATION = 10.0f;

    /** Turning rate when driven by the player (degrees/sec). */
    public static final float PLAYER_TURN_SPEED = 60.0f;

    /** Current speed magnitude when driven by the player. */
    private float driverSpeed = 0f;

    // ── Issue #1146: GarageSystem roadworthiness & stolen flag ────────────────
    // (Note: stolen flag and roadworthiness declared below; Issue #1227 flags
    //  vinSwapped, clocked, condition, vinSwapDaysElapsed also declared below)

    /**
     * Roadworthiness score (0–100).  Fresh NPC cars default to 70.
     * Stolen cars spawned on the forecourt start at 25–55.
     * Falls to 0 on cut-and-shut failure; restored by Advisory Repairs (+15–20 per fix).
     */
    private int roadworthiness = 70;

    /**
     * True if this car has been flagged as stolen by the WantedSystem.
     * WantedSystem plate-checks can seize the car when this is true.
     * Cleared by Car Ringing at Mick's garage (BLANK_LOGBOOK + 25 COIN).
     */
    private boolean stolen = false;

    // ── Issue #1227: CarDealershipSystem — dodgy car economy ─────────────────

    /**
     * True if the VIN plates on this car have been swapped with a donor car from
     * the Scrapyard (Issue #1227). The car appears unregistered to ANPR checks for
     * 3 in-game days; after that it is flagged. 10% daily chance of ANPR detection
     * by a patrolling POLICE NPC.
     */
    private boolean vinSwapped = false;

    /**
     * True if this car's odometer has been tampered with by Bez at Wheelwright Motors
     * (MILEAGE_CORRECTOR_PROP + 5 COIN bribe). Sets condition to TIDY; resale price
     * increases by 15 COIN. Selling while TRADING_STANDARDS NPC is nearby records
     * CONSUMER_FRAUD; Notoriety +8.
     */
    private boolean clocked = false;

    /**
     * The car's condition rating, used for haggling thresholds and resale pricing at
     * Wheelwright Motors (Issue #1227). Defaults to TIDY for fresh NPC cars.
     * Can be upgraded by clocking (ROUGH/BANGER → TIDY).
     */
    private CarCondition condition = CarCondition.TIDY;

    /**
     * Number of in-game days since VIN plate was swapped. Once this reaches 3,
     * ANPR detection becomes active (no longer a 3-day grace). Ticked by
     * CarDealershipSystem.update() each in-game day.
     */
    private int vinSwapDaysElapsed = 0;

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
        this(x, y, z, travelAlongX, segmentMin, segmentMax, colour, false);
    }

    public Car(float x, float y, float z,
               boolean travelAlongX,
               float segmentMin, float segmentMax,
               CarColour colour, boolean van) {
        this.position   = new Vector3(x, y, z);
        this.velocity   = new Vector3();
        this.segmentMin = segmentMin;
        this.segmentMax = segmentMax;
        this.colour     = colour;
        this.van        = van;
        this.speed      = van ? DEFAULT_SPEED * 0.75f : DEFAULT_SPEED;
        float w = van ? VAN_WIDTH : WIDTH;
        float h = van ? VAN_HEIGHT : HEIGHT;
        float d = van ? VAN_DEPTH : DEPTH;
        this.aabb       = new AABB(position, w, h, d);

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
        if (!stopped && !parked) {
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
        aabb.setPosition(position, getWidth(), getHeight(), getDepth());

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
        aabb.setPosition(position, getWidth(), getHeight(), getDepth());
        return true;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Vector3 getPosition() { return position; }
    public Vector3 getVelocity() { return velocity; }
    public AABB    getAABB()     { return aabb; }
    public CarColour getColour() { return colour; }
    public boolean isVan() { return van; }
    public float getWidth() { return van ? VAN_WIDTH : WIDTH; }
    public float getHeight() { return van ? VAN_HEIGHT : HEIGHT; }
    public float getDepth() { return van ? VAN_DEPTH : DEPTH; }

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

    /**
     * How far (blocks) to push the car back on a block collision so it no
     * longer overlaps the solid voxel.  This gives the visual "bounce".
     */
    public static final float BOUNCE_PUSHBACK = 0.3f;

    /**
     * Minimum overlap (blocks) required on each axis before a car-player
     * collision is registered.  Increasing this value makes the collision
     * less strict, so the player must genuinely intersect the car body rather
     * than merely graze its edge.  Addresses player feedback about overly
     * sensitive car collisions (Issue #882).
     */
    public static final float PLAYER_COLLISION_TOLERANCE = 0.25f;

    /**
     * Reverse direction and nudge the car back along the new (reversed)
     * heading so that it no longer overlaps the block it just hit.
     * This is the "bounce" response for block collisions.
     */
    public void bounceOffBlock() {
        reverseDirection();
        // Push the car a little in the new direction so it clears the block
        double rad = Math.toRadians(heading);
        position.x += (float) Math.sin(rad) * BOUNCE_PUSHBACK;
        position.z += (float) Math.cos(rad) * BOUNCE_PUSHBACK;
        aabb.setPosition(position, getWidth(), getHeight(), getDepth());
    }

    // ── Gravity ───────────────────────────────────────────────────────────────

    /** Returns the current vertical velocity (blocks/sec). */
    public float getVerticalVelocity() {
        return verticalVelocity;
    }

    /** Sets the vertical velocity directly (used by gravity/landing resolution). */
    public void setVerticalVelocity(float v) {
        verticalVelocity = v;
    }

    /** Accelerates the car downward by gravity for one frame. */
    public void applyGravity(float delta) {
        verticalVelocity -= GRAVITY * delta;
    }

    /** Zeroes the vertical velocity (called on landing). */
    public void resetVerticalVelocity() {
        verticalVelocity = 0f;
    }

    // ── Player driving ────────────────────────────────────────────────────────

    /**
     * Whether this car is currently being driven by the player.
     */
    public boolean isDrivenByPlayer() {
        return drivenByPlayer;
    }

    /**
     * Whether this car is parked (engine off, no AI driver).
     * Parked cars can be stolen silently; moving cars eject their driver
     * when the player interacts with them (Issue #991).
     */
    public boolean isParked() {
        return parked;
    }

    /**
     * Set the parked state of this car.
     * When parking the car its velocity is zeroed and it is stopped.
     * When un-parking (driving away) the car resumes from rest.
     */
    public void setParked(boolean parked) {
        this.parked = parked;
        if (parked) {
            velocity.set(0f, 0f, 0f);
            stopped = true;
        } else {
            stopped = false;
            applyHeadingToVelocity();
        }
    }

    /**
     * Mark this car as being driven by (or released from) the player.
     * When released the car speed is zeroed so it does not fly away.
     */
    public void setDrivenByPlayer(boolean driven) {
        this.drivenByPlayer = driven;
        if (!driven) {
            driverSpeed = 0f;
            // Apply a gentle stop so the AI can resume from rest
            velocity.set(0f, 0f, 0f);
            stopped = false;
        }
    }

    /**
     * Get the current player-controlled speed (blocks/sec, signed positive = forward).
     */
    public float getDriverSpeed() {
        return driverSpeed;
    }

    // ── Issue #1146 accessors: roadworthiness & stolen ────────────────────────

    /** Returns the roadworthiness score (0–100). */
    public int getRoadworthiness() { return roadworthiness; }

    /** Sets the roadworthiness score (0–100). Clamped to [0, 100]. */
    public void setRoadworthiness(int roadworthiness) {
        this.roadworthiness = Math.max(0, Math.min(100, roadworthiness));
    }

    /** Returns true if this car is flagged as stolen. */
    public boolean isStolen() { return stolen; }

    /** Sets the stolen flag on this car. */
    public void setStolen(boolean stolen) { this.stolen = stolen; }

    // ── Issue #1227 accessors: VIN swap, clocking, condition ─────────────────

    /** Returns true if the VIN plates on this car have been swapped. */
    public boolean isVinSwapped() { return vinSwapped; }

    /** Sets the VIN-swapped flag. */
    public void setVinSwapped(boolean vinSwapped) { this.vinSwapped = vinSwapped; }

    /** Returns true if this car's odometer has been clocked. */
    public boolean isClocked() { return clocked; }

    /** Sets the clocked flag on this car. */
    public void setClocked(boolean clocked) { this.clocked = clocked; }

    /** Returns the condition rating of this car. */
    public CarCondition getCondition() { return condition; }

    /** Sets the condition rating of this car. */
    public void setCondition(CarCondition condition) { this.condition = condition; }

    /** Returns the number of in-game days elapsed since VIN plate swap. */
    public int getVinSwapDaysElapsed() { return vinSwapDaysElapsed; }

    /** Increments the VIN swap day counter by one. */
    public void incrementVinSwapDays() { this.vinSwapDaysElapsed++; }

    /**
     * Update the car when driven by the player.
     *
     * @param delta      seconds since last frame
     * @param accelerate true if the player is pressing the accelerate key
     * @param braking    true if the player is pressing the brake/reverse key
     * @param turnLeft   true if the player is steering left
     * @param turnRight  true if the player is steering right
     */
    public void updatePlayerDriven(float delta, boolean accelerate, boolean braking,
                                   boolean turnLeft, boolean turnRight) {
        // Handle turning (only effective while moving)
        if (Math.abs(driverSpeed) > 0.5f) {
            float turnDir = 0f;
            if (turnLeft)  turnDir += 1f;
            if (turnRight) turnDir -= 1f;
            if (turnDir != 0f) {
                // Steer in the direction the player intends; reverse turns flip when reversing
                float sign = driverSpeed > 0 ? 1f : -1f;
                heading = (heading + turnDir * sign * PLAYER_TURN_SPEED * delta % 360f + 360f) % 360f;
            }
        }

        // Handle acceleration / braking
        if (accelerate && !braking) {
            driverSpeed = Math.min(driverSpeed + PLAYER_ACCELERATION * delta, PLAYER_MAX_SPEED);
        } else if (braking && !accelerate) {
            if (driverSpeed > 0.1f) {
                // Brake while moving forward
                driverSpeed = Math.max(0f, driverSpeed - PLAYER_DECELERATION * delta);
            } else {
                // Reverse
                driverSpeed = Math.max(-PLAYER_MAX_SPEED / 2f, driverSpeed - PLAYER_ACCELERATION * delta);
            }
        } else {
            // Natural deceleration
            if (driverSpeed > 0f) {
                driverSpeed = Math.max(0f, driverSpeed - PLAYER_DECELERATION * delta * 0.5f);
            } else if (driverSpeed < 0f) {
                driverSpeed = Math.min(0f, driverSpeed + PLAYER_DECELERATION * delta * 0.5f);
            }
        }

        // Apply velocity based on current heading and speed
        double rad = Math.toRadians(heading);
        velocity.set((float)(Math.sin(rad) * driverSpeed), 0f, (float)(Math.cos(rad) * driverSpeed));

        // Move the car
        position.add(velocity.x * delta, 0f, velocity.z * delta);
        aabb.setPosition(position, getWidth(), getHeight(), getDepth());

        // Tick damage cooldown
        if (damageCooldown > 0f) {
            damageCooldown -= delta;
        }
    }
}

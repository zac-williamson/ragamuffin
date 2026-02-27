package ragamuffin.ai;

import ragamuffin.entity.Car;
import ragamuffin.entity.Car.CarColour;
import ragamuffin.entity.AABB;
import ragamuffin.entity.DamageReason;
import ragamuffin.entity.Player;
import ragamuffin.world.World;

import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Manages all cars in the game world.
 *
 * Cars are spawned on ROAD blocks along the street grid and drive back and forth
 * along their road segment.  At road intersections they may turn 90° left or
 * right onto a crossing road.  Cars also perform simple look-ahead collision
 * avoidance: they stop when a solid block or another car is detected within
 * {@link Car#LOOK_AHEAD_DISTANCE} blocks ahead, and resume once the path clears.
 *
 * Street grid: roads run every 20 blocks in X and Z, from -170 to +170.
 * Each road is 4 blocks wide (ROAD blocks in the middle of a 6-block-wide street).
 */
public class CarManager {

    /** Maximum number of cars in the world at any time. */
    private static final int MAX_CARS = 40;

    /** Number of road segments to seed cars onto at startup. */
    private static final int INITIAL_CAR_COUNT = 20;

    /** Road grid spacing — matches WorldGenerator.STREET_WIDTH spacing. */
    private static final int STREET_SPACING = 20;

    /** Road extends ±170 blocks from centre (matching WorldGenerator). */
    private static final int STREET_EXTENT = 170;

    /** Park exclusion zone — no cars near the park centre (±18 blocks). */
    private static final int PARK_EXCLUSION = 18;

    /**
     * How close (blocks) a car must be to an intersection centre before we
     * consider it "at" the intersection for turning purposes.
     */
    private static final float INTERSECTION_SNAP_DIST = 1.5f;

    /**
     * Probability [0,1] that a car turns at an intersection rather than going
     * straight.  0.3 = turns about 30% of the time.
     */
    private static final float TURN_PROBABILITY = 0.30f;

    private final List<Car> cars;
    private final Random random;
    private World world;

    public CarManager() {
        this.cars   = new ArrayList<>();
        this.random = new Random();
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Spawn the initial set of cars spread across road segments.
     * Requires a World reference to verify ROAD blocks exist at spawn positions.
     * Stores the world for use in block collision checks during update().
     *
     * @param world the game world (used for ROAD block verification and block collision)
     */
    public void spawnInitialCars(World world) {
        this.world = world;
        List<int[]> hSegments = new ArrayList<>();
        List<int[]> vSegments = new ArrayList<>();

        // Horizontal streets: fixed Z, cars travel along Z axis
        for (int z = -STREET_EXTENT; z <= STREET_EXTENT; z += STREET_SPACING) {
            if (Math.abs(z) < PARK_EXCLUSION) continue;
            int roadZ = z + 2;
            hSegments.add(new int[]{roadZ, -STREET_EXTENT, STREET_EXTENT});
        }

        // Vertical streets: fixed X, cars travel along X axis
        for (int x = -STREET_EXTENT; x <= STREET_EXTENT; x += STREET_SPACING) {
            if (Math.abs(x) < PARK_EXCLUSION) continue;
            int roadX = x + 2;
            vSegments.add(new int[]{roadX, -STREET_EXTENT, STREET_EXTENT});
        }

        Collections.shuffle(hSegments, random);
        Collections.shuffle(vSegments, random);

        CarColour[] colours = CarColour.values();
        int spawned = 0;

        for (int[] seg : hSegments) {
            if (spawned >= INITIAL_CAR_COUNT) break;
            int roadZ  = seg[0];
            int minX   = seg[1];
            int maxX   = seg[2];
            float startX = minX + random.nextInt(maxX - minX);
            if (Math.abs(startX) < PARK_EXCLUSION) startX = PARK_EXCLUSION + 2;
            CarColour colour = colours[Math.abs(roadZ * 31 + (int) startX * 17) % colours.length];
            Car car = new Car(startX, 1.0f, roadZ, false, minX, maxX, colour);
            cars.add(car);
            spawned++;
        }

        for (int[] seg : vSegments) {
            if (spawned >= INITIAL_CAR_COUNT) break;
            int roadX = seg[0];
            int minZ  = seg[1];
            int maxZ  = seg[2];
            float startZ = minZ + random.nextInt(maxZ - minZ);
            if (Math.abs(startZ) < PARK_EXCLUSION) startZ = PARK_EXCLUSION + 2;
            CarColour colour = colours[Math.abs(roadX * 31 + (int) startZ * 17) % colours.length];
            Car car = new Car(roadX, 1.0f, startZ, true, minZ, maxZ, colour);
            cars.add(car);
            spawned++;
        }
    }

    /**
     * Spawn a single car at the given road position (for testing or scripted events).
     *
     * @return the spawned Car, or null if the cap is reached
     */
    public Car spawnCar(float x, float y, float z,
                        boolean travelAlongX,
                        float segmentMin, float segmentMax,
                        CarColour colour) {
        if (cars.size() >= MAX_CARS) return null;
        Car car = new Car(x, y, z, travelAlongX, segmentMin, segmentMax, colour);
        cars.add(car);
        return car;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Update all cars and check for player collisions.
     *
     * Each frame:
     * 1. Look-ahead collision avoidance — stop the car if a solid block or
     *    another car is within {@link Car#LOOK_AHEAD_DISTANCE} blocks ahead.
     * 2. Resume stopped cars when the path ahead is clear.
     * 3. Advance each car's physics via {@link Car#update(float)}.
     * 4. After moving, handle intersection turning.
     * 5. Reverse the car if it overlaps a solid block (fallback for any missed
     *    obstacle).
     * 6. Deal damage + knockback to the player on collision.
     *
     * @param delta  seconds since last frame
     * @param player the player entity
     */
    public void update(float delta, Player player) {
        for (int i = 0; i < cars.size(); i++) {
            Car car = cars.get(i);

            // Issue #773: player-driven cars are controlled by CarDrivingSystem — skip AI
            if (car.isDrivenByPlayer()) {
                continue;
            }

            // --- 1 & 2: look-ahead collision avoidance ---
            boolean pathBlocked = isPathAhead(car, i);
            if (pathBlocked && !car.isStopped()) {
                car.setStopped(true);
            } else if (!pathBlocked && car.isStopped()) {
                car.setStopped(false);
            }

            // --- 3: physics step ---
            car.update(delta);

            // --- 4: intersection turning ---
            if (!car.isStopped()) {
                maybeTurnAtIntersection(car);
            }

            // --- 5: solid-block collision fallback (reverse) ---
            if (world != null && world.checkAABBCollision(car.getAABB())) {
                car.reverseDirection();
            }

            // --- 6: player collision ---
            if (car.canDamagePlayer() && car.getAABB().intersects(player.getAABB())) {
                player.damage(Car.COLLISION_DAMAGE, DamageReason.CAR_HIT);
                car.resetDamageCooldown();
                float vx = car.getVelocity().x;
                float vz = car.getVelocity().z;
                float len = (float) Math.sqrt(vx * vx + vz * vz);
                if (len > 0.001f) {
                    player.setVelocity(vx / len * 8.0f, 4.0f, vz / len * 8.0f);
                }
            }
        }
    }

    // ── Collision avoidance ───────────────────────────────────────────────────

    /**
     * Returns true if the car's forward path is blocked within
     * {@link Car#LOOK_AHEAD_DISTANCE} blocks.
     * Checks both solid world blocks and the AABBs of other cars.
     */
    private boolean isPathAhead(Car car, int carIndex) {
        Vector3 pos = car.getPosition();
        Vector3 vel = car.getVelocity();

        // Determine forward direction unit vector
        float dx, dz;
        if (!car.isStopped()) {
            float len = (float) Math.sqrt(vel.x * vel.x + vel.z * vel.z);
            if (len < 0.001f) return false;
            dx = vel.x / len;
            dz = vel.z / len;
        } else {
            // Use heading when stopped
            double rad = Math.toRadians(car.getHeading());
            dx = (float) Math.sin(rad);
            dz = (float) Math.cos(rad);
        }

        // Build a probe AABB slightly ahead of the car
        float probeX = pos.x + dx * (Car.DEPTH / 2f + Car.LOOK_AHEAD_DISTANCE / 2f);
        float probeZ = pos.z + dz * (Car.DEPTH / 2f + Car.LOOK_AHEAD_DISTANCE / 2f);
        AABB probe = new AABB(
            new Vector3(probeX, pos.y, probeZ),
            Car.WIDTH * 0.9f,
            Car.HEIGHT,
            Car.LOOK_AHEAD_DISTANCE
        );

        // Rotate probe to align with heading
        if (car.isTravellingAlongX()) {
            // Width and depth are swapped for X-travelling cars
            probe = new AABB(
                new Vector3(probeX, pos.y, probeZ),
                Car.LOOK_AHEAD_DISTANCE,
                Car.HEIGHT,
                Car.WIDTH * 0.9f
            );
        }

        // Check world blocks
        if (world != null && world.checkAABBCollision(probe)) {
            return true;
        }

        // Check other cars
        for (int j = 0; j < cars.size(); j++) {
            if (j == carIndex) continue;
            Car other = cars.get(j);
            if (probe.intersects(other.getAABB())) {
                return true;
            }
        }
        return false;
    }

    // ── Intersection turning ──────────────────────────────────────────────────

    /**
     * If the car is at (or very near) a road intersection, randomly decide
     * whether to turn 90° left or right onto the crossing road.
     *
     * Road intersections occur wherever an X-grid line and a Z-grid line cross:
     * X positions: ..., -168, -148, ..., (multiples of STREET_SPACING offset by +2)
     * Z positions: same pattern.
     */
    private void maybeTurnAtIntersection(Car car) {
        Vector3 pos = car.getPosition();

        // Find the nearest intersection grid point
        int nearestGridX = nearestRoadCoord((int) Math.round(pos.x));
        int nearestGridZ = nearestRoadCoord((int) Math.round(pos.z));

        if (nearestGridX == Integer.MIN_VALUE || nearestGridZ == Integer.MIN_VALUE) return;

        float distToX = Math.abs(pos.x - nearestGridX);
        float distToZ = Math.abs(pos.z - nearestGridZ);

        // Only trigger when the car is close to the intersection on BOTH axes
        if (distToX > INTERSECTION_SNAP_DIST || distToZ > INTERSECTION_SNAP_DIST) return;

        // Don't turn in the park exclusion zone
        if (Math.abs(nearestGridX) < PARK_EXCLUSION || Math.abs(nearestGridZ) < PARK_EXCLUSION) return;

        // Decide whether to turn
        if (random.nextFloat() >= TURN_PROBABILITY) return;

        // Choose a perpendicular direction (left or right turn)
        boolean currentlyAlongX = car.isTravellingAlongX();
        float currentHeading = car.getHeading();
        float newHeading;
        float newSegMin, newSegMax;

        if (currentlyAlongX) {
            // Currently moving along X — turn onto Z axis
            // Left turn from +X (heading=90): heading becomes 0 (+Z)
            // Right turn from +X (heading=90): heading becomes 180 (-Z)
            // Left turn from -X (heading=270): heading becomes 180 (-Z)
            // Right turn from -X (heading=270): heading becomes 0 (+Z)
            boolean turnLeft = random.nextBoolean();
            if (turnLeft) {
                newHeading = (currentHeading - 90f + 360f) % 360f;
            } else {
                newHeading = (currentHeading + 90f) % 360f;
            }
            newSegMin = -STREET_EXTENT;
            newSegMax = STREET_EXTENT;
            car.turn(newHeading, nearestGridX, nearestGridZ, newSegMin, newSegMax);
        } else {
            // Currently moving along Z — turn onto X axis
            boolean turnLeft = random.nextBoolean();
            if (turnLeft) {
                newHeading = (currentHeading - 90f + 360f) % 360f;
            } else {
                newHeading = (currentHeading + 90f) % 360f;
            }
            newSegMin = -STREET_EXTENT;
            newSegMax = STREET_EXTENT;
            car.turn(newHeading, nearestGridX, nearestGridZ, newSegMin, newSegMax);
        }
    }

    /**
     * Given any world coordinate, return the nearest road-lane centre coordinate
     * on the road grid (offset +2 from every STREET_SPACING multiple).
     * Returns Integer.MIN_VALUE if the coordinate is not within snap distance
     * of any road lane.
     */
    private int nearestRoadCoord(int worldCoord) {
        // Road lanes sit at: n * STREET_SPACING + 2, where n is any integer,
        // within [-STREET_EXTENT, STREET_EXTENT].
        int snapped = (int) Math.round((worldCoord - 2.0) / STREET_SPACING) * STREET_SPACING + 2;
        if (snapped < -STREET_EXTENT || snapped > STREET_EXTENT) return Integer.MIN_VALUE;
        return snapped;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Returns an unmodifiable view of all active cars. */
    public List<Car> getCars() {
        return Collections.unmodifiableList(cars);
    }

    /** Returns the number of cars currently managed. */
    public int getCarCount() {
        return cars.size();
    }

    /** Remove all cars (used for testing / world reset). */
    public void clearCars() {
        cars.clear();
    }
}

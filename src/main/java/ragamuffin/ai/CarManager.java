package ragamuffin.ai;

import ragamuffin.entity.Car;
import ragamuffin.entity.Car.CarColour;
import ragamuffin.entity.DamageReason;
import ragamuffin.entity.Player;
import ragamuffin.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Manages all cars in the game world.
 *
 * Cars are spawned on ROAD blocks along the street grid and drive back and forth
 * along their road segment. They deal damage to the player on collision.
 *
 * Street grid: roads run every 20 blocks in X and Z, from -170 to +170.
 * Each road is 2 blocks wide (ROAD blocks in the middle of a 4-block-wide street).
 * Cars drive on one of these two lanes.
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

    private final List<Car> cars;
    private final Random random;

    public CarManager() {
        this.cars   = new ArrayList<>();
        this.random = new Random();
    }

    /**
     * Spawn the initial set of cars spread across road segments.
     * Requires a World reference to verify ROAD blocks exist at spawn positions.
     *
     * @param world the game world (used for ROAD block verification)
     */
    public void spawnInitialCars(World world) {
        // Collect all valid road segment centres
        List<int[]> hSegments = new ArrayList<>(); // Horizontal segments (Z-axis streets, cars move in Z)
        List<int[]> vSegments = new ArrayList<>(); // Vertical segments   (X-axis streets, cars move in X)

        // Horizontal streets: fixed Z, varying X
        for (int z = -STREET_EXTENT; z <= STREET_EXTENT; z += STREET_SPACING) {
            if (Math.abs(z) < PARK_EXCLUSION) continue; // skip park area
            // Road blocks are at z+1 and z+2 (inner 2 of the 4-block-wide street at z,z+1,z+2,z+3)
            int roadZ = z + 1; // first ROAD lane
            hSegments.add(new int[]{roadZ, -STREET_EXTENT, STREET_EXTENT});
        }

        // Vertical streets: fixed X, varying Z
        for (int x = -STREET_EXTENT; x <= STREET_EXTENT; x += STREET_SPACING) {
            if (Math.abs(x) < PARK_EXCLUSION) continue;
            int roadX = x + 1; // first ROAD lane
            vSegments.add(new int[]{roadX, -STREET_EXTENT, STREET_EXTENT});
        }

        // Shuffle and pick segments to populate
        Collections.shuffle(hSegments, random);
        Collections.shuffle(vSegments, random);

        CarColour[] colours = CarColour.values();
        int spawned = 0;

        // Spawn cars on horizontal (Z-travel) streets
        for (int[] seg : hSegments) {
            if (spawned >= INITIAL_CAR_COUNT) break;
            int roadZ  = seg[0];
            int minX   = seg[1];
            int maxX   = seg[2];
            // Start at a random X position along the segment
            float startX = minX + random.nextInt(maxX - minX);
            if (Math.abs(startX) < PARK_EXCLUSION) startX = PARK_EXCLUSION + 2;
            CarColour colour = colours[Math.abs(roadZ * 31 + (int) startX * 17) % colours.length];
            Car car = new Car(startX, 1.0f, roadZ, false, minX, maxX, colour);
            cars.add(car);
            spawned++;
        }

        // Spawn cars on vertical (X-travel) streets
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
     * Spawn a single car at the given road position for testing purposes.
     *
     * @param x            Centre X of the car
     * @param y            Bottom Y of the car
     * @param z            Centre Z of the car
     * @param travelAlongX true = moves in X; false = moves in Z
     * @param segmentMin   Lower bound on travel axis
     * @param segmentMax   Upper bound on travel axis
     * @param colour       Car colour
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

    /**
     * Update all cars and check for player collisions.
     *
     * @param delta  seconds since last frame
     * @param player the player entity
     */
    public void update(float delta, Player player) {
        for (Car car : cars) {
            car.update(delta);
            // Check collision with player
            if (car.canDamagePlayer() && car.getAABB().intersects(player.getAABB())) {
                player.damage(Car.COLLISION_DAMAGE, DamageReason.CAR_HIT);
                car.resetDamageCooldown();
                // Knock the player in the direction the car is travelling
                float vx = car.getVelocity().x;
                float vz = car.getVelocity().z;
                float len = (float) Math.sqrt(vx * vx + vz * vz);
                if (len > 0.001f) {
                    player.setVelocity(vx / len * 8.0f, 4.0f, vz / len * 8.0f);
                }
            }
        }
    }

    /**
     * Returns an unmodifiable view of all active cars.
     */
    public List<Car> getCars() {
        return Collections.unmodifiableList(cars);
    }

    /**
     * Returns the number of cars currently managed.
     */
    public int getCarCount() {
        return cars.size();
    }

    /**
     * Remove all cars (used for testing / world reset).
     */
    public void clearCars() {
        cars.clear();
    }
}

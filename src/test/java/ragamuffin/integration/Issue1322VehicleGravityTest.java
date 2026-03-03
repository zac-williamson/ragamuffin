package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.CarManager;
import ragamuffin.entity.Car;
import ragamuffin.entity.Car.CarColour;
import ragamuffin.entity.Player;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1322 — vehicles don't experience gravity.
 *
 * <p>Before the fix, {@link Car} had no vertical velocity and its {@code update(float)}
 * method only moved the car horizontally.  Vehicles spawned mid-air would simply
 * float indefinitely.
 *
 * <p>The fix:
 * <ul>
 *   <li>Adds {@code verticalVelocity} and {@code GRAVITY} to {@link Car}.</li>
 *   <li>Adds {@link World#applyGravityAndVerticalCollision(Car, float)} to apply
 *       gravity and land the car on solid blocks.</li>
 *   <li>Calls that method from {@link CarManager#update} for every car each frame.</li>
 * </ul>
 */
class Issue1322VehicleGravityTest {

    private static final float DELTA = 1f / 60f; // one frame at 60 fps

    private World world;
    private CarManager carManager;
    private Player dummyPlayer; // needed by CarManager.update()

    @BeforeEach
    void setUp() {
        // Empty world — all blocks are AIR so cars fall freely.
        world = new World(0L);
        carManager = new CarManager();
        // CarManager needs a world reference for gravity; inject via spawnInitialCars
        // would require a full world setup. Instead we call applyGravityAndVerticalCollision
        // directly on the world, and test CarManager integration separately.
        dummyPlayer = new Player(1000f, 1f, 1000f); // far away — won't interfere
    }

    // -----------------------------------------------------------------------
    // Car.GRAVITY constant must exist and be positive
    // -----------------------------------------------------------------------

    @Test
    void carGravityConstantExistsAndIsPositive() {
        assertTrue(Car.GRAVITY > 0f,
                "Car.GRAVITY must be a positive constant");
    }

    // -----------------------------------------------------------------------
    // Car.applyGravity() — vertical velocity accumulates downward
    // -----------------------------------------------------------------------

    @Test
    void applyGravityDecreasesVerticalVelocity() {
        Car car = new Car(0f, 10f, 0f, true, -100f, 100f, CarColour.RED);

        float vBefore = car.getVerticalVelocity();
        car.applyGravity(DELTA);
        float vAfter = car.getVerticalVelocity();

        assertTrue(vAfter < vBefore,
                "applyGravity() must decrease (make more negative) the vertical velocity; "
                        + "was=" + vBefore + " now=" + vAfter);
    }

    // -----------------------------------------------------------------------
    // World.applyGravityAndVerticalCollision(Car) — car falls in empty world
    // -----------------------------------------------------------------------

    @Test
    void carFallsInEmptyWorld() {
        // Spawn car high above the ground in an empty (all-AIR) world.
        Car car = new Car(0f, 10f, 0f, true, -100f, 100f, CarColour.BLUE);

        float yBefore = car.getPosition().y;

        // Simulate 60 frames (1 second) of gravity.
        for (int i = 0; i < 60; i++) {
            world.applyGravityAndVerticalCollision(car, DELTA);
        }

        float yAfter = car.getPosition().y;
        assertTrue(yAfter < yBefore,
                "Car Y must decrease after gravity is applied (was=" + yBefore + " now=" + yAfter + ")");
    }

    // -----------------------------------------------------------------------
    // Car lands on a solid block
    // -----------------------------------------------------------------------

    @Test
    void carLandsOnSolidBlock() {
        // Place a solid floor at y=0.
        world.setBlock(0, 0, 0, BlockType.ROAD);

        // Spawn car above it at y=5.
        Car car = new Car(0f, 5f, 0f, true, -100f, 100f, CarColour.WHITE);

        // Simulate up to 5 seconds of gravity (300 frames) — car should land.
        for (int i = 0; i < 300; i++) {
            world.applyGravityAndVerticalCollision(car, DELTA);
            if (car.getVerticalVelocity() == 0f) {
                break;
            }
        }

        // After landing, vertical velocity must be reset to zero.
        assertEquals(0f, car.getVerticalVelocity(), 0.001f,
                "Vertical velocity must be 0 after landing on a solid block");

        // Car must be resting on top of the block (y == 1.0, the top of the block at y=0).
        assertEquals(1.0f, car.getPosition().y, 0.1f,
                "Car must rest on top of the solid block");
    }

    // -----------------------------------------------------------------------
    // Vertical velocity resets on landing
    // -----------------------------------------------------------------------

    @Test
    void verticalVelocityResetsOnLanding() {
        world.setBlock(0, 0, 0, BlockType.PAVEMENT);
        Car car = new Car(0f, 8f, 0f, false, -100f, 100f, CarColour.SILVER);

        // Let it fall and land.
        for (int i = 0; i < 300; i++) {
            world.applyGravityAndVerticalCollision(car, DELTA);
            if (car.getVerticalVelocity() == 0f) break;
        }

        assertEquals(0f, car.getVerticalVelocity(), 0.001f,
                "Vertical velocity must be reset to 0 after landing");
    }

    // -----------------------------------------------------------------------
    // CarManager applies gravity via World (integration)
    // -----------------------------------------------------------------------

    @Test
    void carManagerAppliesGravityViaWorld() {
        // Spawn the initial cars into the world so CarManager gets the World reference.
        // Use a world with a flat ROAD floor at y=0 over a wide area.
        World flatWorld = new World(42L);
        // Lay a road floor at y=0 from -200..200 on both axes.
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                flatWorld.setBlock(x, 0, z, BlockType.ROAD);
            }
        }

        CarManager mgr = new CarManager();
        // spawnInitialCars gives the CarManager its world reference.
        mgr.spawnInitialCars(flatWorld);

        // Directly spawn a car high above the floor near the test area.
        Car car = mgr.spawnCar(0f, 10f, 0f, true, -20f, 20f, CarColour.BLACK);
        assertNotNull(car, "Should be able to spawn a test car");

        float yBefore = car.getPosition().y;

        // Simulate 60 frames — gravity should pull it down.
        for (int i = 0; i < 60; i++) {
            mgr.update(DELTA, dummyPlayer);
        }

        float yAfter = car.getPosition().y;
        assertTrue(yAfter < yBefore,
                "Car Y must decrease after CarManager applies gravity (was=" + yBefore + " now=" + yAfter + ")");
    }

    // -----------------------------------------------------------------------
    // Parked car also experiences gravity
    // -----------------------------------------------------------------------

    @Test
    void parkedCarExperiencesGravity() {
        // Parked car spawned high above in empty world — must fall.
        Car car = new Car(0f, 15f, 0f, true, -100f, 100f, CarColour.YELLOW);

        float yBefore = car.getPosition().y;

        for (int i = 0; i < 60; i++) {
            world.applyGravityAndVerticalCollision(car, DELTA);
        }

        assertTrue(car.getPosition().y < yBefore,
                "Parked car must fall under gravity (was=" + yBefore + " now=" + car.getPosition().y + ")");
    }

    // -----------------------------------------------------------------------
    // Van also experiences gravity
    // -----------------------------------------------------------------------

    @Test
    void vanExperiencesGravity() {
        Car van = new Car(0f, 10f, 0f, true, -100f, 100f, CarColour.RED, true);
        assertTrue(van.isVan(), "Should be a van");

        float yBefore = van.getPosition().y;

        for (int i = 0; i < 60; i++) {
            world.applyGravityAndVerticalCollision(van, DELTA);
        }

        assertTrue(van.getPosition().y < yBefore,
                "Van must fall under gravity (was=" + yBefore + " now=" + van.getPosition().y + ")");
    }
}

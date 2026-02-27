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
 * Integration tests for Issue #723 — Add block collision to cars.
 *
 * Verifies that cars interact properly with the voxel environment:
 *   - A car that drives into a solid block reverses direction
 *   - A car with no obstacles continues moving in the same direction
 *   - reverseDirection() toggles the car's travel direction
 */
class Issue723CarBlockCollisionTest {

    private World world;
    private CarManager carManager;
    private Player player;

    @BeforeEach
    void setUp() {
        world      = new World(42L);
        carManager = new CarManager();
        // Register the world so block collision is active during update()
        carManager.spawnInitialCars(world);
        carManager.clearCars(); // remove world-generated cars; we spawn our own
        player = new Player(1000f, 1f, 1000f); // far away — won't interfere
    }

    // -------------------------------------------------------------------------
    // reverseDirection() — unit-level check on the Car method
    // -------------------------------------------------------------------------

    @Test
    void reverseDirectionTogglesXTravelCar() {
        Car car = new Car(0f, 1f, 0f, true, -100f, 100f, CarColour.RED);
        assertTrue(car.isMovingPositive(), "Car should start moving positive");

        car.reverseDirection();

        assertFalse(car.isMovingPositive(), "Car should move negative after reverseDirection()");
    }

    @Test
    void reverseDirectionTogglesZTravelCar() {
        Car car = new Car(0f, 1f, 0f, false, -100f, 100f, CarColour.BLUE);
        assertTrue(car.isMovingPositive(), "Car should start moving positive");

        car.reverseDirection();

        assertFalse(car.isMovingPositive(), "Car should move negative after reverseDirection()");
    }

    @Test
    void reverseDirectionTwiceRestoresOriginalDirection() {
        Car car = new Car(0f, 1f, 0f, true, -100f, 100f, CarColour.WHITE);
        boolean initialPositive = car.isMovingPositive();

        car.reverseDirection();
        car.reverseDirection();

        assertEquals(initialPositive, car.isMovingPositive(),
            "Two reversals should restore the original direction");
    }

    // -------------------------------------------------------------------------
    // Block collision — car reverses when it hits a solid block
    // -------------------------------------------------------------------------

    @Test
    void carReversesWhenDrivingIntoSolidBlock() {
        // Spawn car at x=5, y=1, z=20, travelling in +X direction
        // Place a BRICK wall at x=8, y=1, z=20 — directly in the car's path
        world.setBlock(8, 1, 20, BlockType.BRICK);

        Car car = carManager.spawnCar(5f, 1f, 20f, true, -100f, 100f, CarColour.RED);
        assertNotNull(car);
        assertTrue(car.isMovingPositive(), "Car should start moving positive (+X)");

        // Simulate frames until the car reaches the wall
        // Car speed = 6 blocks/s, gap = ~3 blocks, needs ~0.5 s of frames
        for (int i = 0; i < 60; i++) {
            carManager.update(1f / 60f, player);
        }

        // Car should have reversed after colliding with the BRICK block
        assertFalse(car.isMovingPositive(),
            "Car should be moving in negative X after hitting BRICK wall");
    }

    @Test
    void carWithNoObstaclesKeepsTravellingPositive() {
        // No obstacles — car should still be moving positive after 60 frames
        // Place car far from segment ends and any blocks
        Car car = carManager.spawnCar(0f, 1f, 50f, true, -200f, 200f, CarColour.SILVER);
        assertNotNull(car);
        assertTrue(car.isMovingPositive(), "Car should start moving positive");

        // Drive for a short time — should not hit anything
        for (int i = 0; i < 10; i++) {
            carManager.update(1f / 60f, player);
        }

        assertTrue(car.isMovingPositive(),
            "Car with no obstacles should still be moving positive");
    }

    @Test
    void carDoesNotPassThroughSolidBlock() {
        // Place a solid block 2 blocks ahead of the car
        world.setBlock(7, 1, 30, BlockType.BRICK);

        Car car = carManager.spawnCar(5f, 1f, 30f, true, -100f, 100f, CarColour.BLACK);
        assertNotNull(car);

        // Run for enough time that the car would have passed through the block if
        // there were no block collision (6 blocks/s × 1s = 6 blocks)
        for (int i = 0; i < 60; i++) {
            carManager.update(1f / 60f, player);
        }

        // Car must not have gone past x=7 (the block position)
        float carMaxX = car.getAABB().getMaxX();
        assertTrue(carMaxX <= 8.0f,
            "Car AABB should not extend past the solid BRICK block at x=7 (maxX=" + carMaxX + ")");
    }
}

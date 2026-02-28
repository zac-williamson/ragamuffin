package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.CarManager;
import ragamuffin.core.CarDrivingSystem;
import ragamuffin.entity.Car;
import ragamuffin.entity.Car.CarColour;
import ragamuffin.entity.Player;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #804 — Cars should bounce off blocks rather
 * than passing through them.
 *
 * Verifies:
 *   - bounceOffBlock() reverses direction AND nudges the car clear of the block
 *   - AI-driven cars do not pass through solid blocks
 *   - AI-driven cars reverse direction after hitting a solid block
 *   - Player-driven cars do not pass through solid blocks
 *   - Player-driven cars reverse after hitting a solid block
 */
class Issue804CarBounceOffBlocksTest {

    private World world;
    private CarManager carManager;
    private CarDrivingSystem drivingSystem;
    private Player player;

    @BeforeEach
    void setUp() {
        world         = new World(42L);
        carManager    = new CarManager();
        carManager.spawnInitialCars(world);
        carManager.clearCars(); // remove world-generated cars; use our own
        drivingSystem = new CarDrivingSystem(carManager);
        drivingSystem.setWorld(world);
        player = new Player(1000f, 1f, 1000f); // far away — won't interfere
    }

    // ─────────────────────────────────────────────────────────────────────────
    // bounceOffBlock() unit-level checks
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void bounceOffBlockReversesDirection() {
        Car car = new Car(0f, 1f, 0f, true, -100f, 100f, CarColour.RED);
        assertTrue(car.isMovingPositive(), "Car should start moving in +X");

        car.bounceOffBlock();

        assertFalse(car.isMovingPositive(),
            "bounceOffBlock() should reverse direction (now moving in -X)");
    }

    @Test
    void bounceOffBlockPushesCarBackAlongNewDirection() {
        // Car travelling in +X; after bounce it should move in -X, so X decreases
        Car car = new Car(5f, 1f, 0f, true, -100f, 100f, CarColour.BLUE);
        float xBefore = car.getPosition().x;

        car.bounceOffBlock();

        float xAfter = car.getPosition().x;
        assertTrue(xAfter < xBefore,
            "After bouncing in +X direction, car should be pushed back (X decreases). "
            + "Before=" + xBefore + " After=" + xAfter);
    }

    @Test
    void bounceOffBlockPushesCarBackPositiveAmount() {
        Car car = new Car(0f, 1f, 0f, false, -100f, 100f, CarColour.WHITE);
        // Travelling +Z; bounce reverses to -Z and nudges in -Z direction
        float zBefore = car.getPosition().z;

        car.bounceOffBlock();

        float zAfter = car.getPosition().z;
        assertTrue(zAfter < zBefore,
            "After bouncing in +Z direction, car Z should decrease. "
            + "Before=" + zBefore + " After=" + zAfter);
    }

    @Test
    void bouncePushbackConstantIsPositive() {
        assertTrue(Car.BOUNCE_PUSHBACK > 0f,
            "BOUNCE_PUSHBACK should be a positive distance");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI-driven cars — do not pass through solid blocks
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void aiCarDoesNotPassThroughSolidBlockOnXAxis() {
        // Place a BRICK wall at x=10, y=1, z=50 — in the car's path (+X)
        world.setBlock(10, 1, 50, BlockType.BRICK);

        Car car = carManager.spawnCar(5f, 1f, 50f, true, -200f, 200f, CarColour.RED);
        assertNotNull(car);
        assertTrue(car.isMovingPositive(), "Car starts moving in +X");

        // Run 2 seconds: at 6 blocks/s the car would cover 12 blocks without collision
        for (int i = 0; i < 120; i++) {
            carManager.update(1f / 60f, player);
        }

        // The car's maximum X extent should not have passed through the brick at x=10
        float maxX = car.getAABB().getMaxX();
        assertTrue(maxX <= 11.0f,
            "AI car AABB should not extend beyond the BRICK block at x=10. maxX=" + maxX);
    }

    @Test
    void aiCarReversesDirectionAfterHittingBlock() {
        // Place a block close ahead so the car hits it within the first second
        world.setBlock(8, 1, 60, BlockType.BRICK);

        Car car = carManager.spawnCar(5f, 1f, 60f, true, -200f, 200f, CarColour.SILVER);
        assertNotNull(car);
        assertTrue(car.isMovingPositive(), "Car starts moving in +X");

        // Run enough frames to reach the block (speed 6, gap ~3 blocks → ~0.5s)
        for (int i = 0; i < 60; i++) {
            carManager.update(1f / 60f, player);
        }

        assertFalse(car.isMovingPositive(),
            "AI car should be moving in -X after bouncing off the BRICK block");
    }

    @Test
    void aiCarDoesNotPassThroughSolidBlockOnZAxis() {
        // Place a BRICK wall at z=10, y=1, x=70 — in the car's path (+Z)
        world.setBlock(70, 1, 10, BlockType.BRICK);

        Car car = carManager.spawnCar(70f, 1f, 5f, false, -200f, 200f, CarColour.BLACK);
        assertNotNull(car);
        assertTrue(car.isMovingPositive(), "Car starts moving in +Z");

        for (int i = 0; i < 120; i++) {
            carManager.update(1f / 60f, player);
        }

        float maxZ = car.getAABB().getMaxZ();
        assertTrue(maxZ <= 11.0f,
            "AI car AABB should not extend beyond the BRICK block at z=10. maxZ=" + maxZ);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Player-driven cars — do not pass through solid blocks
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void playerDrivenCarDoesNotPassThroughSolidBlock() {
        // Place a BRICK wall directly ahead of the car (in +Z direction, heading=0)
        world.setBlock(80, 1, 8, BlockType.BRICK);

        Car car = carManager.spawnCar(80f, 1f, 0f, false, -200f, 200f, CarColour.YELLOW);
        assertNotNull(car);

        // Player enters the car
        player.teleport(80f, 1f, 0f);
        boolean entered = drivingSystem.tryEnterCar(player);
        assertTrue(entered, "Player should be able to enter the car");

        // Accelerate for 2 seconds (at max speed 12 blocks/s it would cover 24 blocks
        // without collision — but there's a wall at z=8)
        for (int i = 0; i < 120; i++) {
            drivingSystem.update(1f / 60f, player,
                /* accelerate */ true,
                /* braking    */ false,
                /* turnLeft   */ false,
                /* turnRight  */ false);
        }

        float maxZ = car.getAABB().getMaxZ();
        assertTrue(maxZ <= 9.5f,
            "Player-driven car should not pass through BRICK block at z=8. maxZ=" + maxZ);
    }

    @Test
    void playerDrivenCarBouncesBackFromBlock() {
        // Place a BRICK wall close ahead so the bounce is clearly visible
        world.setBlock(80, 1, 6, BlockType.BRICK);

        Car car = carManager.spawnCar(80f, 1f, 0f, false, -200f, 200f, CarColour.RED);
        assertNotNull(car);

        player.teleport(80f, 1f, 0f);
        drivingSystem.tryEnterCar(player);

        float initialZ = car.getPosition().z;

        // Accelerate 2 seconds toward the block
        for (int i = 0; i < 120; i++) {
            drivingSystem.update(1f / 60f, player, true, false, false, false);
        }

        // After the bounce the car should have reversed: check it ended up closer to
        // (or back at) the initial position, i.e. max Z is less than the block position
        float finalZ = car.getPosition().z;
        assertTrue(finalZ < 6f,
            "After bouncing off the block at z=6, car centre should be behind the block. "
            + "initialZ=" + initialZ + " finalZ=" + finalZ);
    }
}

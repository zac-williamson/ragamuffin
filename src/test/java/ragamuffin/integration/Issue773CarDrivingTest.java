package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.CarManager;
import ragamuffin.core.CarDrivingSystem;
import ragamuffin.entity.Car;
import ragamuffin.entity.Car.CarColour;
import ragamuffin.entity.Player;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #773 — Car interaction and driving mechanics.
 *
 * Verifies:
 *   - Player can enter a nearby car (E interaction)
 *   - Player cannot enter a car that is out of range
 *   - Player position tracks the car while driving
 *   - W key (accelerate) causes the car to move forward
 *   - S key (brake/reverse) slows or reverses the car
 *   - A/D keys steer the car (heading changes while moving)
 *   - Player can exit the car (E or ESC)
 *   - After exit the car stops and AI can resume
 *   - CarManager skips AI for player-driven cars
 *   - Player-driven car does NOT collide-damage the player inside it
 *   - Car constants for player driving are sensible
 */
class Issue773CarDrivingTest {

    private CarManager carManager;
    private CarDrivingSystem drivingSystem;
    private Player player;

    @BeforeEach
    void setUp() {
        carManager    = new CarManager();
        drivingSystem = new CarDrivingSystem(carManager);
        player        = new Player(0f, 1f, 0f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Entering a car
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void playerCanEnterNearbycar() {
        // Spawn a car within enter range of the player
        carManager.spawnCar(0f, 1f, 2f, false, -100f, 100f, CarColour.RED);

        boolean entered = drivingSystem.tryEnterCar(player);

        assertTrue(entered, "Player should be able to enter a nearby car");
        assertTrue(drivingSystem.isInCar(), "isInCar() should return true after entering");
        assertNotNull(drivingSystem.getCurrentCar(), "getCurrentCar() should not be null while driving");
    }

    @Test
    void playerCannotEnterCarTooFarAway() {
        // Spawn a car far outside enter range
        carManager.spawnCar(100f, 1f, 100f, false, -200f, 200f, CarColour.BLUE);

        boolean entered = drivingSystem.tryEnterCar(player);

        assertFalse(entered, "Player should not be able to enter a car that is out of range");
        assertFalse(drivingSystem.isInCar(), "isInCar() should remain false when no car is in range");
    }

    @Test
    void enterCarMarksCraAsDrivenByPlayer() {
        Car car = carManager.spawnCar(0f, 1f, 2f, false, -100f, 100f, CarColour.WHITE);
        assertNotNull(car);

        drivingSystem.tryEnterCar(player);

        assertTrue(car.isDrivenByPlayer(), "Car should be marked as driven by player after entry");
    }

    @Test
    void enterCarProducesMessage() {
        carManager.spawnCar(0f, 1f, 2f, false, -100f, 100f, CarColour.SILVER);
        drivingSystem.tryEnterCar(player);

        String msg = drivingSystem.pollLastMessage();
        assertNotNull(msg, "Entering a car should produce a feedback message");
        assertFalse(msg.isEmpty(), "The entry message should not be empty");
    }

    @Test
    void playerCannotEnterCarTwice() {
        carManager.spawnCar(0f, 1f, 2f, false, -100f, 100f, CarColour.BLACK);
        drivingSystem.tryEnterCar(player);

        // Spawn a second car also in range
        carManager.spawnCar(1f, 1f, 2f, false, -100f, 100f, CarColour.YELLOW);
        boolean enteredAgain = drivingSystem.tryEnterCar(player);

        assertFalse(enteredAgain, "Player should not be able to enter a second car while already driving");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Driving — movement
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void acceleratingMovesCar() {
        Car car = carManager.spawnCar(0f, 1f, 0f, false, -200f, 200f, CarColour.RED);
        assertNotNull(car);
        drivingSystem.tryEnterCar(player);

        float initialZ = car.getPosition().z;

        // Simulate 1 second of W-key held
        for (int i = 0; i < 60; i++) {
            drivingSystem.update(1f / 60f, player,
                /* accelerate */ true,
                /* braking    */ false,
                /* turnLeft   */ false,
                /* turnRight  */ false);
        }

        assertNotEquals(initialZ, car.getPosition().z,
            "Car should have moved after 1 second of acceleration");
    }

    @Test
    void playerPositionTracksCarWhileDriving() {
        Car car = carManager.spawnCar(0f, 1f, 0f, false, -200f, 200f, CarColour.BLUE);
        assertNotNull(car);
        drivingSystem.tryEnterCar(player);

        // Accelerate for a few frames
        for (int i = 0; i < 30; i++) {
            drivingSystem.update(1f / 60f, player, true, false, false, false);
        }

        // Player X and Z must match the car
        assertEquals(car.getPosition().x, player.getPosition().x, 0.01f,
            "Player X should match car X while driving");
        assertEquals(car.getPosition().z, player.getPosition().z, 0.01f,
            "Player Z should match car Z while driving");
    }

    @Test
    void brakingStopsAcceleratingCar() {
        Car car = carManager.spawnCar(0f, 1f, 0f, false, -200f, 200f, CarColour.WHITE);
        assertNotNull(car);
        drivingSystem.tryEnterCar(player);

        // Accelerate for half a second
        for (int i = 0; i < 30; i++) {
            drivingSystem.update(1f / 60f, player, true, false, false, false);
        }
        float speedBeforeBrake = car.getDriverSpeed();
        assertTrue(speedBeforeBrake > 0f, "Car should have positive speed after accelerating");

        // Brake for 2 seconds
        for (int i = 0; i < 120; i++) {
            drivingSystem.update(1f / 60f, player, false, true, false, false);
        }

        // After braking the speed should be less than before
        assertTrue(car.getDriverSpeed() < speedBeforeBrake,
            "Car speed should decrease while braking");
    }

    @Test
    void steeringChangesHeadingWhileMoving() {
        Car car = carManager.spawnCar(0f, 1f, 0f, false, -200f, 200f, CarColour.SILVER);
        assertNotNull(car);
        drivingSystem.tryEnterCar(player);

        float initialHeading = car.getHeading();

        // Accelerate briefly first so the car is moving, then steer right
        for (int i = 0; i < 10; i++) {
            drivingSystem.update(1f / 60f, player, true, false, false, false);
        }
        for (int i = 0; i < 60; i++) {
            drivingSystem.update(1f / 60f, player, true, false, false, true);
        }

        assertNotEquals(initialHeading, car.getHeading(),
            "Car heading should change when steering right while moving");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exiting a car
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void playerCanExitCar() {
        carManager.spawnCar(0f, 1f, 2f, false, -100f, 100f, CarColour.BLACK);
        drivingSystem.tryEnterCar(player);

        boolean exited = drivingSystem.exitCar(player);

        assertTrue(exited, "exitCar() should return true when the player is in a car");
        assertFalse(drivingSystem.isInCar(), "isInCar() should be false after exiting");
        assertNull(drivingSystem.getCurrentCar(), "getCurrentCar() should be null after exiting");
    }

    @Test
    void exitCarMarksCrAsNotDrivenByPlayer() {
        Car car = carManager.spawnCar(0f, 1f, 2f, false, -100f, 100f, CarColour.YELLOW);
        assertNotNull(car);
        drivingSystem.tryEnterCar(player);
        drivingSystem.exitCar(player);

        assertFalse(car.isDrivenByPlayer(), "Car should not be marked as driven by player after exit");
    }

    @Test
    void exitCarProducesMessage() {
        carManager.spawnCar(0f, 1f, 2f, false, -100f, 100f, CarColour.RED);
        drivingSystem.tryEnterCar(player);
        drivingSystem.pollLastMessage(); // consume entry message
        drivingSystem.exitCar(player);

        String msg = drivingSystem.pollLastMessage();
        assertNotNull(msg, "Exiting a car should produce a feedback message");
        assertFalse(msg.isEmpty(), "The exit message should not be empty");
    }

    @Test
    void exitCarWhenNotInCarReturnsFalse() {
        boolean result = drivingSystem.exitCar(player);
        assertFalse(result, "exitCar() should return false when the player is not in a car");
    }

    @Test
    void exitCarStopsVehicle() {
        Car car = carManager.spawnCar(0f, 1f, 0f, false, -200f, 200f, CarColour.BLUE);
        assertNotNull(car);
        drivingSystem.tryEnterCar(player);

        // Accelerate for a moment
        for (int i = 0; i < 30; i++) {
            drivingSystem.update(1f / 60f, player, true, false, false, false);
        }

        drivingSystem.exitCar(player);

        assertEquals(0f, car.getDriverSpeed(), 0.001f,
            "Driver speed should be reset to 0 after exiting the car");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CarManager AI bypass for player-driven cars
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void carManagerSkipsAIForPlayerDrivenCar() {
        Car car = carManager.spawnCar(0f, 1f, 0f, false, -200f, 200f, CarColour.WHITE);
        assertNotNull(car);
        drivingSystem.tryEnterCar(player);

        float initialZ = car.getPosition().z;

        // CarManager.update should not move the player-driven car
        carManager.update(1.0f, player);

        assertEquals(initialZ, car.getPosition().z, 0.01f,
            "CarManager should not apply AI movement to a player-driven car");
    }

    @Test
    void carManagerDoesNotDamagePlayerInsideOwnCar() {
        Car car = carManager.spawnCar(0f, 1f, 0f, false, -200f, 200f, CarColour.RED);
        assertNotNull(car);
        drivingSystem.tryEnterCar(player);

        float healthBefore = player.getHealth();

        // Even though the car AABB overlaps the player, AI collision is skipped
        carManager.update(0.016f, player);

        assertEquals(healthBefore, player.getHealth(), 0.001f,
            "Player should not take damage from their own car while driving it");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Car driving constants
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void playerMaxSpeedIsReasonable() {
        assertTrue(Car.PLAYER_MAX_SPEED > Car.DEFAULT_SPEED,
            "Player max driving speed should be faster than AI speed");
        assertTrue(Car.PLAYER_MAX_SPEED <= 30f,
            "Player max speed should not be unrealistically high");
    }

    @Test
    void enterRangeIsReasonable() {
        assertTrue(CarDrivingSystem.ENTER_RANGE >= 2f,
            "Enter range should be at least 2 blocks");
        assertTrue(CarDrivingSystem.ENTER_RANGE <= 6f,
            "Enter range should not be excessively large");
    }

    @Test
    void playerAccelerationAndDecelerationArePositive() {
        assertTrue(Car.PLAYER_ACCELERATION > 0f,
            "Player acceleration should be positive");
        assertTrue(Car.PLAYER_DECELERATION > 0f,
            "Player deceleration should be positive");
    }

    @Test
    void playerTurnSpeedIsReasonable() {
        assertTrue(Car.PLAYER_TURN_SPEED >= 45f,
            "Turn speed should be at least 45 degrees/sec");
        assertTrue(Car.PLAYER_TURN_SPEED <= 180f,
            "Turn speed should not be unrealistically fast");
    }
}

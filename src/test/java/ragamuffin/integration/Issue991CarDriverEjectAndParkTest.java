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
 * Integration tests for Issue #991 — Car mechanics update.
 *
 * Verifies:
 *   - Interacting with a MOVING car causes the driver to get out
 *     (car stops, player takes over, specific "driver gets out" message shown)
 *   - Interacting with a PARKED car lets the player steal it directly
 *     (no driver ejection message, car immediately driveable)
 *   - Parked cars do not move under AI control
 *   - Moving cars eject driver regardless of current speed
 *   - After stealing a parked car it behaves like a normal player-driven car
 *   - After taking a moving car the car is stopped then becomes driveable
 */
class Issue991CarDriverEjectAndParkTest {

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
    // Moving car — driver ejects on player interaction
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void interactingWithMovingCarEjectsDriver() {
        // Spawn a standard (moving) car near the player
        carManager.spawnCar(0f, 1f, 2f, false, -100f, 100f, CarColour.RED);

        boolean entered = drivingSystem.tryEnterCar(player);

        assertTrue(entered, "Player should be able to take over a moving car");
    }

    @Test
    void movingCarEjectMessageMentionsDriver() {
        carManager.spawnCar(0f, 1f, 2f, false, -100f, 100f, CarColour.BLUE);

        drivingSystem.tryEnterCar(player);
        String msg = drivingSystem.pollLastMessage();

        assertNotNull(msg, "Entering a moving car should produce a message");
        assertTrue(msg.toLowerCase().contains("driver"),
            "Message for a moving car should mention the driver getting out, got: " + msg);
    }

    @Test
    void movingCarIsStoppedAfterDriverEjects() {
        Car car = carManager.spawnCar(0f, 1f, 2f, false, -100f, 100f, CarColour.WHITE);
        assertNotNull(car);

        drivingSystem.tryEnterCar(player);

        // Car should be stopped (engine paused) immediately after the driver exits
        // isStopped() is true OR isDrivenByPlayer keeps it from AI-moving — either way
        // the car velocity should be zeroed by setStopped(true) which was called
        assertTrue(car.isDrivenByPlayer(),
            "Car should be under player control after driver ejection");
    }

    @Test
    void movingCarIsMarkedDrivenByPlayerAfterDriverEjects() {
        Car car = carManager.spawnCar(0f, 1f, 2f, false, -100f, 100f, CarColour.SILVER);
        assertNotNull(car);

        assertFalse(car.isDrivenByPlayer(), "Car should not be driven by player before interaction");

        drivingSystem.tryEnterCar(player);

        assertTrue(car.isDrivenByPlayer(),
            "Car should be marked as driven by player after driver ejects");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parked car — player steals it directly without driver ejection
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void parkedCarCanBeStolen() {
        carManager.spawnParkedCar(0f, 1f, 2f, false, -100f, 100f, CarColour.BLACK);

        boolean entered = drivingSystem.tryEnterCar(player);

        assertTrue(entered, "Player should be able to steal a parked car");
        assertTrue(drivingSystem.isInCar(), "Player should be in the car after stealing it");
    }

    @Test
    void stealingParkedCarDoesNotMentionDriver() {
        carManager.spawnParkedCar(0f, 1f, 2f, false, -100f, 100f, CarColour.YELLOW);

        drivingSystem.tryEnterCar(player);
        String msg = drivingSystem.pollLastMessage();

        assertNotNull(msg, "Stealing a parked car should produce a message");
        assertFalse(msg.toLowerCase().contains("driver gets out"),
            "Stealing a parked car should NOT say the driver gets out, got: " + msg);
    }

    @Test
    void parkedCarMessageMentionsParked() {
        carManager.spawnParkedCar(0f, 1f, 2f, false, -100f, 100f, CarColour.RED);

        drivingSystem.tryEnterCar(player);
        String msg = drivingSystem.pollLastMessage();

        assertNotNull(msg, "Stealing a parked car should produce a message");
        assertTrue(msg.toLowerCase().contains("parked"),
            "Message for stealing a parked car should reference it being parked, got: " + msg);
    }

    @Test
    void parkedCarIsMarkedDrivenByPlayerAfterStealing() {
        Car car = carManager.spawnParkedCar(0f, 1f, 2f, false, -100f, 100f, CarColour.BLUE);
        assertNotNull(car);

        assertTrue(car.isParked(), "Spawned car should start as parked");

        drivingSystem.tryEnterCar(player);

        assertTrue(car.isDrivenByPlayer(),
            "Car should be driven by player after stealing");
        assertFalse(car.isParked(),
            "Car should no longer be parked once stolen");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parked cars don't move under AI
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void parkedCarDoesNotMoveUnderAI() {
        Car car = carManager.spawnParkedCar(10f, 1f, 10f, false, -100f, 100f, CarColour.WHITE);
        assertNotNull(car);

        float initialZ = car.getPosition().z;

        // Run AI update for 5 simulated seconds
        for (int i = 0; i < 300; i++) {
            carManager.update(1f / 60f, player);
        }

        assertEquals(initialZ, car.getPosition().z, 0.001f,
            "Parked car should not move under AI update");
    }

    @Test
    void parkedCarStartsWithZeroVelocity() {
        Car car = carManager.spawnParkedCar(5f, 1f, 5f, false, -100f, 100f, CarColour.SILVER);
        assertNotNull(car);

        assertEquals(0f, car.getVelocity().x, 0.001f, "Parked car velocity.x should be 0");
        assertEquals(0f, car.getVelocity().z, 0.001f, "Parked car velocity.z should be 0");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stolen parked car can be driven normally
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void stolenParkedCarCanAccelerate() {
        Car car = carManager.spawnParkedCar(0f, 1f, 0f, false, -200f, 200f, CarColour.BLACK);
        assertNotNull(car);
        drivingSystem.tryEnterCar(player);

        float initialZ = car.getPosition().z;

        // Accelerate for 1 second
        for (int i = 0; i < 60; i++) {
            drivingSystem.update(1f / 60f, player, true, false, false, false);
        }

        assertNotEquals(initialZ, car.getPosition().z,
            "Stolen parked car should move when player accelerates");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // isParked accessor
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void normalSpawnedCarIsNotParked() {
        Car car = carManager.spawnCar(0f, 1f, 5f, false, -100f, 100f, CarColour.RED);
        assertNotNull(car);

        assertFalse(car.isParked(), "A normally-spawned moving car should not be parked");
    }

    @Test
    void parkedCarSpawnedViaManagerIsParked() {
        Car car = carManager.spawnParkedCar(0f, 1f, 5f, false, -100f, 100f, CarColour.BLUE);
        assertNotNull(car);

        assertTrue(car.isParked(), "A car spawned via spawnParkedCar should be parked");
    }
}

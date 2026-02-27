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
 * Integration tests for Issue #753 — Vehicle turning and collision avoidance.
 *
 * Verifies:
 *   - Car heading is tracked correctly (getHeading())
 *   - Cars can turn 90° via turn()
 *   - isTravellingAlongX() reflects the current heading
 *   - reverseDirection() flips heading by 180°
 *   - Cars stop when an obstacle is detected ahead (collision avoidance)
 *   - Cars resume when the path ahead becomes clear
 *   - Car-to-car collision avoidance: a stopped car prevents the following car
 *     from driving into it
 *   - Turning does not break player-damage or speed invariants
 */
class Issue753CarTurningAndAvoidanceTest {

    private CarManager carManager;
    private Player player;
    private World world;

    @BeforeEach
    void setUp() {
        carManager = new CarManager();
        player = new Player(1000f, 1f, 1000f); // far away — won't interfere
        world  = new World(99L);
        // Register world so block collision is active; then clear spawned cars
        carManager.spawnInitialCars(world);
        carManager.clearCars();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Heading tracking
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void newCarTravellingAlongZHasHeadingZero() {
        Car car = new Car(0f, 1f, 0f, false, -100f, 100f, CarColour.RED);
        assertEquals(0f, car.getHeading(), 0.01f,
            "Car travelling along +Z should start with heading 0°");
    }

    @Test
    void newCarTravellingAlongXHasHeadingNinety() {
        Car car = new Car(0f, 1f, 0f, true, -100f, 100f, CarColour.BLUE);
        assertEquals(90f, car.getHeading(), 0.01f,
            "Car travelling along +X should start with heading 90°");
    }

    @Test
    void reverseDirectionFlipsHeadingBy180() {
        Car car = new Car(0f, 1f, 0f, false, -100f, 100f, CarColour.WHITE);
        float initialHeading = car.getHeading();

        car.reverseDirection();

        float expected = (initialHeading + 180f) % 360f;
        assertEquals(expected, car.getHeading(), 0.01f,
            "reverseDirection() should flip heading by 180°");
    }

    @Test
    void reverseDirectionTwiceRestoresHeading() {
        Car car = new Car(0f, 1f, 0f, true, -100f, 100f, CarColour.SILVER);
        float initial = car.getHeading();
        car.reverseDirection();
        car.reverseDirection();
        assertEquals(initial, car.getHeading(), 0.01f,
            "Two reversals should restore the original heading");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Turning
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void turnChangesHeadingToNewValue() {
        Car car = new Car(0f, 1f, 0f, true, -100f, 100f, CarColour.BLACK);
        // Car was heading +X (90°), turn it to +Z (0°)
        car.turn(0f, 0f, 0f, -100f, 100f);
        assertEquals(0f, car.getHeading(), 0.01f,
            "turn() should update the heading to the new value");
    }

    @Test
    void turnSnapsPositionToIntersection() {
        Car car = new Car(10f, 1f, 3f, true, -100f, 100f, CarColour.YELLOW);
        // Turn at intersection (2, ?, 2)
        car.turn(0f, 2f, 2f, -100f, 100f);
        assertEquals(2f, car.getPosition().x, 0.01f, "turn() should snap X to intersection X");
        assertEquals(2f, car.getPosition().z, 0.01f, "turn() should snap Z to intersection Z");
    }

    @Test
    void turnFromXAxisToZAxisUpdatesisTravellingAlongX() {
        Car car = new Car(0f, 1f, 0f, true, -100f, 100f, CarColour.RED);
        assertTrue(car.isTravellingAlongX(), "Should be travelling along X before turn");

        car.turn(0f, 0f, 0f, -100f, 100f); // new heading = 0° = +Z

        assertFalse(car.isTravellingAlongX(),
            "After turning to +Z heading, isTravellingAlongX() should be false");
    }

    @Test
    void turnFromZAxisToXAxisUpdatesisTravellingAlongX() {
        Car car = new Car(0f, 1f, 0f, false, -100f, 100f, CarColour.BLUE);
        assertFalse(car.isTravellingAlongX(), "Should not be travelling along X before turn");

        car.turn(90f, 0f, 0f, -100f, 100f); // new heading = 90° = +X

        assertTrue(car.isTravellingAlongX(),
            "After turning to +X heading, isTravellingAlongX() should be true");
    }

    @Test
    void turnUnsetsStoppedFlag() {
        Car car = new Car(0f, 1f, 0f, false, -100f, 100f, CarColour.WHITE);
        car.setStopped(true);
        assertTrue(car.isStopped(), "Car should be stopped before turn");

        car.turn(90f, 0f, 0f, -100f, 100f);

        assertFalse(car.isStopped(), "turn() should clear the stopped flag");
    }

    @Test
    void afterTurnVelocityAlignedWithNewHeading() {
        Car car = new Car(0f, 1f, 0f, false, -100f, 100f, CarColour.SILVER);
        // Heading 0° = +Z: velocity.z should be positive, velocity.x ~0
        assertEquals(0f, car.getVelocity().x, 0.01f, "Before turn: vx should be ~0");
        assertTrue(car.getVelocity().z > 0f, "Before turn: vz should be positive");

        car.turn(90f, 0f, 0f, -100f, 100f); // heading 90° = +X

        assertTrue(car.getVelocity().x > 0f, "After +X turn: vx should be positive");
        assertEquals(0f, car.getVelocity().z, 0.01f, "After +X turn: vz should be ~0");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stopped flag (collision avoidance)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void setStoppedTrueHaltsMovement() {
        Car car = carManager.spawnCar(0f, 1f, 20f, false, -200f, 200f, CarColour.RED);
        assertNotNull(car);
        float initialZ = car.getPosition().z;

        car.setStopped(true);
        car.update(1.0f);

        assertEquals(initialZ, car.getPosition().z, 0.01f,
            "Stopped car should not move");
    }

    @Test
    void setStoppedFalseResumesMovement() {
        Car car = carManager.spawnCar(0f, 1f, 20f, false, -200f, 200f, CarColour.BLUE);
        assertNotNull(car);
        car.setStopped(true);
        float zAfterStop = car.getPosition().z;

        car.setStopped(false);
        car.update(1.0f);

        assertNotEquals(zAfterStop, car.getPosition().z,
            "Car should resume movement after setStopped(false)");
    }

    @Test
    void setStoppedTrueSetsVelocityToZero() {
        Car car = new Car(0f, 1f, 0f, false, -100f, 100f, CarColour.WHITE);
        assertTrue(Math.abs(car.getVelocity().z) > 0.1f, "Car should be moving before stop");

        car.setStopped(true);

        assertEquals(0f, car.getVelocity().x, 0.001f, "vx should be 0 when stopped");
        assertEquals(0f, car.getVelocity().z, 0.001f, "vz should be 0 when stopped");
    }

    @Test
    void setStoppedFalseRestoresSpeedAtCurrentHeading() {
        Car car = new Car(0f, 1f, 0f, false, -100f, 100f, CarColour.BLACK);
        car.setStopped(true);
        car.setStopped(false);

        float speed = (float) Math.sqrt(
            car.getVelocity().x * car.getVelocity().x +
            car.getVelocity().z * car.getVelocity().z
        );
        assertEquals(Car.DEFAULT_SPEED, speed, 0.1f,
            "Resumed car should travel at DEFAULT_SPEED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Collision avoidance via CarManager
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void carStopsBeforeSolidBlockAhead() {
        // Place a BRICK wall 3 blocks ahead of the car
        world.setBlock(4, 1, 20, BlockType.BRICK);
        world.setBlock(4, 2, 20, BlockType.BRICK);

        // Car at x=0, travelling +X
        Car car = carManager.spawnCar(0f, 1f, 20f, true, -200f, 200f, CarColour.RED);
        assertNotNull(car);

        // Simulate frames — car should stop before reaching x=4
        for (int i = 0; i < 120; i++) {
            carManager.update(1f / 60f, player);
        }

        // Car should be stopped somewhere before the wall
        assertTrue(car.getPosition().x < 4.0f,
            "Car should stop before the solid block (x=" + car.getPosition().x + ")");
    }

    @Test
    void carAvoidsDrivingIntoAnotherCar() {
        // Car A is stopped at x=10, car B is behind it at x=0 heading +X
        Car carA = carManager.spawnCar(10f, 1f, 20f, true, -200f, 200f, CarColour.RED);
        assertNotNull(carA);
        carA.setStopped(true);

        Car carB = carManager.spawnCar(0f, 1f, 20f, true, -200f, 200f, CarColour.BLUE);
        assertNotNull(carB);

        // Run frames — carB should slow and stop before hitting carA
        for (int i = 0; i < 120; i++) {
            carManager.update(1f / 60f, player);
        }

        // carB's front (x + DEPTH/2) should not overlap carA's back (x - DEPTH/2)
        float carBFront = carB.getPosition().x + Car.DEPTH / 2f;
        float carABack  = carA.getPosition().x - Car.DEPTH / 2f;
        assertTrue(carBFront <= carABack + 0.5f,
            "Car B should stop before hitting Car A (carBFront=" + carBFront +
            ", carABack=" + carABack + ")");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Invariants after turning
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void carSpeedUnchangedAfterTurn() {
        Car car = new Car(0f, 1f, 0f, false, -100f, 100f, CarColour.YELLOW);
        car.turn(90f, 0f, 0f, -100f, 100f);

        float speed = (float) Math.sqrt(
            car.getVelocity().x * car.getVelocity().x +
            car.getVelocity().z * car.getVelocity().z
        );
        assertEquals(Car.DEFAULT_SPEED, speed, 0.1f,
            "Car speed should not change after a turn");
    }

    @Test
    void carCollisionDamageStillWorksAfterTurn() {
        // Place player at origin, spawn car at origin heading +Z, then turn it +X
        Player localPlayer = new Player(0f, 1f, 0f);
        Car car = carManager.spawnCar(0f, 1f, 0f, false, -100f, 100f, CarColour.RED);
        assertNotNull(car);
        car.turn(90f, 0f, 0f, -100f, 100f);

        float initialHealth = localPlayer.getHealth();
        carManager.update(0.016f, localPlayer);

        assertTrue(localPlayer.getHealth() < initialHealth,
            "Car should still deal collision damage after a turn");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getHeading() — boundary values
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void headingAfterMultipleReverseStaysInRange() {
        Car car = new Car(0f, 1f, 0f, false, -100f, 100f, CarColour.WHITE);
        for (int i = 0; i < 10; i++) {
            car.reverseDirection();
        }
        float h = car.getHeading();
        assertTrue(h >= 0f && h < 360f,
            "Heading should stay in [0, 360) after multiple reversals (got " + h + ")");
    }

    @Test
    void headingAfterTurnStaysInRange() {
        Car car = new Car(0f, 1f, 0f, false, -100f, 100f, CarColour.BLACK);
        car.turn(270f, 0f, 0f, -100f, 100f);
        float h = car.getHeading();
        assertTrue(h >= 0f && h < 360f,
            "Heading should stay in [0, 360) after turn (got " + h + ")");
    }
}

package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.CarManager;
import ragamuffin.entity.Car;
import ragamuffin.entity.Car.CarColour;
import ragamuffin.entity.DamageReason;
import ragamuffin.entity.Player;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #662 — cars and roads.
 *
 * Verifies:
 *   - Car entity is created with correct properties
 *   - Cars move along their road segment
 *   - Cars reverse direction at segment endpoints
 *   - CarManager spawns and manages cars
 *   - Cars deal damage to the player on collision
 *   - Damage cooldown prevents repeated hits
 *   - CAR_HIT DamageReason exists in enum
 */
class Issue662CarsAndRoadsTest {

    private CarManager carManager;

    @BeforeEach
    void setUp() {
        carManager = new CarManager();
    }

    // -----------------------------------------------------------------------
    // DamageReason enum — CAR_HIT must exist
    // -----------------------------------------------------------------------

    @Test
    void carHitDamageReasonExists() {
        DamageReason reason = DamageReason.CAR_HIT;
        assertNotNull(reason, "CAR_HIT must exist in DamageReason enum");
        assertEquals("Hit by car", reason.getDisplayName(),
            "CAR_HIT display name should be 'Hit by car'");
    }

    // -----------------------------------------------------------------------
    // Car entity — basic properties
    // -----------------------------------------------------------------------

    @Test
    void carIsCreatedWithCorrectColour() {
        Car car = carManager.spawnCar(10f, 1f, 20f, false, -100f, 100f, CarColour.RED);
        assertNotNull(car, "Car should be spawned successfully");
        assertEquals(CarColour.RED, car.getColour(), "Car colour should be RED");
    }

    @Test
    void carStartsAtGivenPosition() {
        Car car = carManager.spawnCar(50f, 1f, 30f, true, -50f, 100f, CarColour.BLUE);
        assertNotNull(car);
        assertEquals(50f, car.getPosition().x, 0.01f, "Car X should be at spawn X");
        assertEquals(1f,  car.getPosition().y, 0.01f, "Car Y should be at spawn Y");
        assertEquals(30f, car.getPosition().z, 0.01f, "Car Z should be at spawn Z");
    }

    @Test
    void carMovesAlongXAxis() {
        // travelAlongX = true → car moves in X direction
        Car car = carManager.spawnCar(0f, 1f, 20f, true, -170f, 170f, CarColour.WHITE);
        assertNotNull(car);
        float initialX = car.getPosition().x;
        // Update for 1 second
        car.update(1.0f);
        assertNotEquals(initialX, car.getPosition().x, "Car should have moved along X axis");
        assertEquals(20f, car.getPosition().z, 0.01f, "Car Z should not change when travelling along X");
    }

    @Test
    void carMovesAlongZAxis() {
        // travelAlongX = false → car moves in Z direction
        Car car = carManager.spawnCar(20f, 1f, 0f, false, -170f, 170f, CarColour.SILVER);
        assertNotNull(car);
        float initialZ = car.getPosition().z;
        car.update(1.0f);
        assertNotEquals(initialZ, car.getPosition().z, "Car should have moved along Z axis");
        assertEquals(20f, car.getPosition().x, 0.01f, "Car X should not change when travelling along Z");
    }

    @Test
    void carMovesAtDefaultSpeed() {
        Car car = carManager.spawnCar(0f, 1f, 20f, true, -200f, 200f, CarColour.BLACK);
        assertNotNull(car);
        float initialX = car.getPosition().x;
        car.update(1.0f);
        float distanceMoved = Math.abs(car.getPosition().x - initialX);
        assertEquals(Car.DEFAULT_SPEED, distanceMoved, 0.1f,
            "Car should move DEFAULT_SPEED blocks per second");
    }

    // -----------------------------------------------------------------------
    // Car bouncing — reverses direction at segment endpoints
    // -----------------------------------------------------------------------

    @Test
    void carReversesDirectionAtSegmentMax() {
        // Place car near segment max so it hits the boundary quickly
        float max = 50f;
        Car car = carManager.spawnCar(max - 1f, 1f, 20f, true, -50f, max, CarColour.YELLOW);
        assertNotNull(car);
        assertTrue(car.isMovingPositive(), "Car should start moving positive");

        // Update enough to go past the boundary
        car.update(1.0f);

        assertFalse(car.isMovingPositive(),
            "Car should be moving negative after hitting segment max");
        assertTrue(car.getPosition().x <= max,
            "Car should not exceed segment max");
    }

    @Test
    void carReversesDirectionAtSegmentMin() {
        // Place car near segment min
        float min = -50f;
        // Create car moving in negative direction by starting near max and letting it bounce
        Car car = carManager.spawnCar(min + 1f, 1f, 20f, true, min, 50f, CarColour.RED);
        assertNotNull(car);
        // Force it toward min — update enough to trigger the first positive bounce at max,
        // then it needs to travel the full segment back. Instead, just start near min:
        // The car starts moving positive, so advance it to max first…
        // A simpler test: car at just above min, moving negative (we cannot directly
        // set velocity, but we can bounce it through max first)
        // Re-approach: test by placing car at segment max and letting it bounce twice
        Car car2 = carManager.spawnCar(50f - 0.1f, 1f, 20f, true, min, 50f, CarColour.BLUE);
        // After a very small update it will hit max, reverse, travel to min
        car2.update(0.02f); // hits max, now going negative
        assertFalse(car2.isMovingPositive(), "After bouncing at max, car should move negative");

        // Drive all the way to the min boundary in one big step
        car2.update(200f / Car.DEFAULT_SPEED + 1f);
        assertTrue(car2.isMovingPositive(),
            "Car should be moving positive after hitting segment min");
        assertTrue(car2.getPosition().x >= min,
            "Car should not go below segment min");
    }

    // -----------------------------------------------------------------------
    // Car–player collision and damage
    // -----------------------------------------------------------------------

    @Test
    void carDamagesPlayerOnCollision() {
        // Place player at origin
        Player player = new Player(0f, 1f, 0f);
        float initialHealth = player.getHealth();

        // Spawn a car directly on top of the player (same position) — guaranteed overlap
        Car car = carManager.spawnCar(0f, 1f, 0f, false, -100f, 100f, CarColour.RED);
        assertNotNull(car);

        // Update CarManager (which checks collisions)
        carManager.update(0.016f, player);

        assertTrue(player.getHealth() < initialHealth,
            "Player should have taken damage from car collision");
    }

    @Test
    void carDealCorrectDamageAmount() {
        Player player = new Player(0f, 1f, 0f);
        float initialHealth = player.getHealth();

        Car car = carManager.spawnCar(0f, 1f, 0f, false, -100f, 100f, CarColour.WHITE);
        assertNotNull(car);

        carManager.update(0.016f, player);

        float expectedHealth = initialHealth - Car.COLLISION_DAMAGE;
        assertEquals(expectedHealth, player.getHealth(), 0.01f,
            "Car should deal exactly COLLISION_DAMAGE to the player");
    }

    @Test
    void carSetsCarHitDamageReason() {
        Player player = new Player(0f, 1f, 0f);

        Car car = carManager.spawnCar(0f, 1f, 0f, false, -100f, 100f, CarColour.SILVER);
        assertNotNull(car);

        carManager.update(0.016f, player);

        assertEquals(DamageReason.CAR_HIT, player.getLastDamageReason(),
            "Player's last damage reason should be CAR_HIT after being hit by a car");
    }

    @Test
    void carDoesNotDamageFarAwayPlayer() {
        // Place player far from the car
        Player player = new Player(100f, 1f, 100f);
        float initialHealth = player.getHealth();

        // Car near origin
        Car car = carManager.spawnCar(0f, 1f, 0f, false, -100f, 100f, CarColour.BLACK);
        assertNotNull(car);

        carManager.update(0.016f, player);

        assertEquals(initialHealth, player.getHealth(), 0.001f,
            "Player should not take damage when far from the car");
    }

    // -----------------------------------------------------------------------
    // Damage cooldown — prevents repeated hits within same collision
    // -----------------------------------------------------------------------

    @Test
    void carDamageCooldownPreventsRepeatedHits() {
        Player player = new Player(0f, 1f, 0f);
        float initialHealth = player.getHealth();

        Car car = carManager.spawnCar(0f, 1f, 0f, false, -100f, 100f, CarColour.YELLOW);
        assertNotNull(car);

        // First hit
        carManager.update(0.016f, player);
        float healthAfterFirstHit = player.getHealth();
        assertTrue(healthAfterFirstHit < initialHealth, "First hit should deal damage");

        // Immediate second frame — cooldown is active, no further damage
        carManager.update(0.016f, player);
        assertEquals(healthAfterFirstHit, player.getHealth(), 0.001f,
            "Car should not deal damage again within the damage cooldown window");
    }

    @Test
    void carCanDamagePlayerAfterCooldownExpires() {
        Player player = new Player(0f, 1f, 0f);

        Car car = carManager.spawnCar(0f, 1f, 0f, false, -100f, 100f, CarColour.RED);
        assertNotNull(car);

        // First hit
        carManager.update(0.016f, player);
        float healthAfterFirstHit = player.getHealth();

        // Wait for cooldown to expire (>1.5 s) — advance the car by 2 seconds
        car.update(2.0f);
        // Also move player back into collision range for a clean test
        // (we can't move player, but the car bounces back — just test canDamagePlayer)
        assertTrue(car.canDamagePlayer(),
            "Car should be able to damage again once the cooldown has expired");
    }

    // -----------------------------------------------------------------------
    // CarManager — spawning and management
    // -----------------------------------------------------------------------

    @Test
    void carManagerStartsEmpty() {
        CarManager fresh = new CarManager();
        assertEquals(0, fresh.getCarCount(), "New CarManager should have no cars");
    }

    @Test
    void carManagerSpawnCarAddsToList() {
        carManager.spawnCar(0f, 1f, 0f, true, -100f, 100f, CarColour.RED);
        assertEquals(1, carManager.getCarCount(), "Car count should be 1 after spawning one car");
    }

    @Test
    void carManagerClearCarsRemovesAll() {
        carManager.spawnCar(0f, 1f, 0f, true, -100f, 100f, CarColour.RED);
        carManager.spawnCar(10f, 1f, 0f, false, -100f, 100f, CarColour.BLUE);
        assertEquals(2, carManager.getCarCount());

        carManager.clearCars();
        assertEquals(0, carManager.getCarCount(), "clearCars() should remove all cars");
    }

    @Test
    void carManagerGetCarsIsUnmodifiable() {
        carManager.spawnCar(0f, 1f, 0f, true, -100f, 100f, CarColour.WHITE);
        assertThrows(UnsupportedOperationException.class, () -> {
            carManager.getCars().add(new Car(0, 0, 0, true, 0, 1, CarColour.RED));
        }, "getCars() should return an unmodifiable list");
    }

    @Test
    void carManagerEnforcesCap() {
        // Fill up to just below MAX_CARS (40) with minimal work — just use spawnCar
        // We only need to prove the 41st returns null when the cap (40) is full.
        for (int i = 0; i < 40; i++) {
            Car c = carManager.spawnCar(i, 1f, 0f, true, -100f, 100f, CarColour.RED);
            assertNotNull(c, "Should be able to spawn car " + (i + 1));
        }
        Car overflow = carManager.spawnCar(999f, 1f, 0f, true, -100f, 100f, CarColour.BLUE);
        assertNull(overflow, "Car beyond the cap (40) should return null");
    }

    // -----------------------------------------------------------------------
    // CarColour enum — all colours exist
    // -----------------------------------------------------------------------

    @Test
    void allCarColoursExist() {
        assertNotNull(CarColour.RED);
        assertNotNull(CarColour.BLUE);
        assertNotNull(CarColour.WHITE);
        assertNotNull(CarColour.SILVER);
        assertNotNull(CarColour.BLACK);
        assertNotNull(CarColour.YELLOW);
    }

    // -----------------------------------------------------------------------
    // Car geometry constants — sensible values
    // -----------------------------------------------------------------------

    @Test
    void carDimensionsAreReasonable() {
        assertTrue(Car.WIDTH  > 0f && Car.WIDTH  < 3f, "Car width should be between 0 and 3 blocks");
        assertTrue(Car.HEIGHT > 0f && Car.HEIGHT < 3f, "Car height should be between 0 and 3 blocks");
        assertTrue(Car.DEPTH  > 0f && Car.DEPTH  < 5f, "Car depth should be between 0 and 5 blocks");
    }

    @Test
    void carDefaultSpeedIsReasonable() {
        assertTrue(Car.DEFAULT_SPEED >= 4f && Car.DEFAULT_SPEED <= 15f,
            "Car speed should be in a realistic range (4-15 blocks/sec)");
    }

    @Test
    void carCollisionDamageIsSignificant() {
        assertTrue(Car.COLLISION_DAMAGE >= 10f && Car.COLLISION_DAMAGE <= 50f,
            "Car collision damage should be significant but not one-shot (10-50 HP)");
    }
}

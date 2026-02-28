package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.CarManager;
import ragamuffin.entity.Car;
import ragamuffin.entity.Car.CarColour;
import ragamuffin.entity.AABB;
import ragamuffin.entity.Player;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #882 — Increase car collision tolerance.
 *
 * Verifies that:
 *   - A player who merely grazes the edge of a car does NOT take damage
 *   - A player who genuinely overlaps a car DOES take damage
 *   - PLAYER_COLLISION_TOLERANCE constant is positive (non-zero tolerance)
 *   - intersectsWithTolerance() behaves correctly at boundary conditions
 */
class Issue882CarCollisionToleranceTest {

    private World world;
    private CarManager carManager;
    private Player player;

    @BeforeEach
    void setUp() {
        world      = new World(42L);
        carManager = new CarManager();
        carManager.spawnInitialCars(world);
        carManager.clearCars(); // remove world-generated cars; we spawn our own
        player = new Player(1000f, 1f, 1000f); // start far away
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Constant checks
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void playerCollisionToleranceIsPositive() {
        assertTrue(Car.PLAYER_COLLISION_TOLERANCE > 0f,
            "PLAYER_COLLISION_TOLERANCE should be a positive value so edge grazes are ignored");
    }

    @Test
    void playerCollisionToleranceIsReasonable() {
        // Tolerance should be meaningful but not so large that real collisions are missed.
        // The car half-width is Car.WIDTH / 2 = 0.8 blocks; tolerance must be well below that.
        assertTrue(Car.PLAYER_COLLISION_TOLERANCE < Car.WIDTH / 2f,
            "PLAYER_COLLISION_TOLERANCE (" + Car.PLAYER_COLLISION_TOLERANCE
            + ") should be less than half the car width (" + (Car.WIDTH / 2f) + ")");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AABB.intersectsWithTolerance() unit-level checks
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void intersectsWithToleranceReturnsFalseForBareGraze() {
        // Two AABBs that just touch (share an edge) — with tolerance they should NOT intersect
        AABB a = new AABB(0f, 0f, 0f, 1f, 1f, 1f);
        // b starts exactly at a.maxX (touching, zero overlap on X)
        AABB b = new AABB(1f, 0f, 0f, 2f, 1f, 1f);

        assertFalse(a.intersectsWithTolerance(b, 0.1f),
            "Touching (zero-overlap) AABBs should not intersect when tolerance > 0");
    }

    @Test
    void intersectsWithToleranceReturnsTrueForSignificantOverlap() {
        // Two AABBs with significant overlap on all axes
        AABB a = new AABB(0f, 0f, 0f, 2f, 2f, 2f);
        AABB b = new AABB(0.5f, 0.5f, 0.5f, 2.5f, 2.5f, 2.5f);
        // overlap = 1.5 blocks on each axis, well above any reasonable tolerance

        assertTrue(a.intersectsWithTolerance(b, 0.25f),
            "AABBs with 1.5-block overlap should intersect even with 0.25 tolerance");
    }

    @Test
    void intersectsWithToleranceMatchesIntersectsWhenToleranceIsZero() {
        AABB a = new AABB(0f, 0f, 0f, 1.5f, 1.5f, 1.5f);
        AABB b = new AABB(1.0f, 0.5f, 0.5f, 2.5f, 1.5f, 1.5f);
        // 0.5 block overlap on X, clear overlap on Y and Z

        assertEquals(a.intersects(b), a.intersectsWithTolerance(b, 0f),
            "intersectsWithTolerance(other, 0) should match intersects() for overlapping boxes");
    }

    @Test
    void intersectsWithToleranceRefusesSmallOverlap() {
        // Overlap is 0.1 blocks on X, but tolerance is 0.25 — should NOT trigger
        AABB a = new AABB(0f, 0f, 0f, 1.1f, 2f, 2f);
        AABB b = new AABB(1.0f, 0f, 0f, 3f, 2f, 2f);
        // overlap on X = 0.1, Y and Z fully overlap

        assertFalse(a.intersectsWithTolerance(b, 0.25f),
            "Overlap of 0.1 on one axis should not register with tolerance=0.25");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Integration: player grazing a stationary car does not take damage
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void playerGrazingCarEdgeDoesNotTakeDamage() {
        // Spawn a stationary car (stopped) near the player but just outside tolerance
        // Car centre at (50, 1, 50); car half-width = Car.WIDTH/2 = 0.8 blocks
        // Player AABB width ≈ 0.6 (player half-width = 0.3); place player so overlap
        // is less than PLAYER_COLLISION_TOLERANCE on X
        Car car = carManager.spawnCar(50f, 1f, 50f, true, -200f, 200f, CarColour.RED);
        assertNotNull(car);
        car.setStopped(true); // keep the car still

        float initialHealth = player.getHealth();

        // Position the player so its AABB barely grazes the car edge
        // Car minX = 50 - 0.8 = 49.2; player maxX needs to be just 49.2 + (tolerance/2)
        // Use tolerance / 2 overlap so it's below the threshold
        float graze = Car.PLAYER_COLLISION_TOLERANCE / 2f;
        float playerX = car.getAABB().getMinX() - 0.3f + graze; // player centre
        player.teleport(playerX, 1f, 50f);

        // Run a frame so CarManager checks player collision
        carManager.update(1f / 60f, player);

        assertEquals(initialHealth, player.getHealth(), 0.01f,
            "Player grazing the car edge (overlap < tolerance) should NOT take damage");
    }

    @Test
    void playerSignificantlyOverlappingCarTakesDamage() {
        // Spawn a moving car directly onto the player — overlap will be large
        float initialHealth = player.getHealth();

        // Place the player at (60, 1, 60)
        player.teleport(60f, 1f, 60f);

        // Spawn the car exactly at the player's position — guaranteed full overlap
        Car car = carManager.spawnCar(60f, 1f, 60f, true, -200f, 200f, CarColour.BLUE);
        assertNotNull(car);

        // Run one frame — the car AABB fully covers the player, well above tolerance
        carManager.update(1f / 60f, player);

        assertTrue(player.getHealth() < initialHealth,
            "Player fully inside the car AABB should take damage. Health was "
            + initialHealth + " now " + player.getHealth());
    }
}

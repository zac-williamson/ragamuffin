package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.CarManager;
import ragamuffin.entity.Car;
import ragamuffin.entity.Car.CarColour;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.world.World;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #884 — NPCs should be knocked back and damaged
 * when hit by vehicles.
 *
 * Verifies that:
 *   - An NPC fully overlapping a moving car takes damage
 *   - The NPC is transitioned to KNOCKED_OUT when the damage kills it
 *   - A knocked-back NPC has non-zero velocity after being hit
 *   - An NPC that merely grazes a car edge (below tolerance) does NOT take damage
 *   - A player-driven car also knocks back and damages NPCs
 *   - NPC_COLLISION_DAMAGE constant is positive
 */
class Issue884NPCCarKnockbackTest {

    private World world;
    private CarManager carManager;
    private Player player;

    @BeforeEach
    void setUp() {
        world = new World(42L);
        carManager = new CarManager();
        carManager.spawnInitialCars(world);
        carManager.clearCars(); // remove world-generated cars; we spawn our own
        // Place player well away from the NPCs being tested
        player = new Player(1000f, 1f, 1000f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Constant checks
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void npcCollisionDamageConstantIsPositive() {
        assertTrue(CarManager.NPC_COLLISION_DAMAGE > 0f,
            "NPC_COLLISION_DAMAGE should be a positive value");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NPC hit by AI-driven car
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void npcFullyOverlappingCarTakesDamage() {
        // Place the NPC at (60, 1, 60)
        NPC npc = new NPC(NPCType.PUBLIC, 60f, 1f, 60f);
        float initialHealth = npc.getHealth();
        List<NPC> npcs = Arrays.asList(npc);

        // Spawn a car exactly at the NPC's position — guaranteed full overlap
        Car car = carManager.spawnCar(60f, 1f, 60f, true, -200f, 200f, CarColour.RED);
        assertNotNull(car);

        // Run one frame
        carManager.update(1f / 60f, player, npcs);

        assertTrue(npc.getHealth() < initialHealth,
            "NPC fully inside the car AABB should take damage. Health was "
            + initialHealth + " now " + npc.getHealth());
    }

    @Test
    void npcKilledByCarTransitionsToKnockedOut() {
        // Use a PENSIONER (10 HP) so a single collision kills it
        NPC npc = new NPC(NPCType.PENSIONER, 70f, 1f, 70f);
        assertEquals(10f, npc.getHealth(), 0.01f, "Pensioner should start with 10 HP");
        List<NPC> npcs = Arrays.asList(npc);

        // Spawn a car directly on the NPC
        Car car = carManager.spawnCar(70f, 1f, 70f, true, -200f, 200f, CarColour.BLUE);
        assertNotNull(car);

        // NPC_COLLISION_DAMAGE (20) > PENSIONER maxHealth (10), so one hit kills it
        carManager.update(1f / 60f, player, npcs);

        assertFalse(npc.isAlive(), "NPC should be dead after lethal car hit");
        assertEquals(NPCState.KNOCKED_OUT, npc.getState(),
            "Dead NPC should be in KNOCKED_OUT state");
    }

    @Test
    void npcReceivesKnockbackVelocityAfterCarHit() {
        // Place an NPC and a moving car at the same position
        NPC npc = new NPC(NPCType.PUBLIC, 80f, 1f, 80f);
        List<NPC> npcs = Arrays.asList(npc);

        // Car travelling along X — velocity is non-zero
        Car car = carManager.spawnCar(80f, 1f, 80f, true, -200f, 200f, CarColour.WHITE);
        assertNotNull(car);
        assertFalse(car.isStopped(), "Car should be moving to apply directional knockback");

        carManager.update(1f / 60f, player, npcs);

        // After the hit the NPC should have non-zero velocity (knocked back)
        float speed = npc.getVelocity().len();
        assertTrue(speed > 0.1f,
            "NPC should have non-zero velocity after being hit by a car, but speed was " + speed);
        assertTrue(npc.isKnockedBack(),
            "NPC should report isKnockedBack() == true immediately after a car hit");
    }

    @Test
    void npcGrazingCarEdgeDoesNotTakeDamage() {
        // Position NPC so its AABB barely grazes the car edge — below tolerance threshold
        NPC npc = new NPC(NPCType.PUBLIC, 1f, 1f, 1f);
        float initialHealth = npc.getHealth();
        List<NPC> npcs = Arrays.asList(npc);

        // Spawn car at (50, 1, 50); car minX = 50 - Car.WIDTH/2 = 49.2
        Car car = carManager.spawnCar(50f, 1f, 50f, true, -200f, 200f, CarColour.SILVER);
        assertNotNull(car);
        car.setStopped(true);

        // Place NPC so it just barely grazes the car's edge (overlap < tolerance)
        float graze = Car.PLAYER_COLLISION_TOLERANCE / 2f;
        float npcX = car.getAABB().getMinX() - NPC.WIDTH / 2f + graze;
        npc.getPosition().set(npcX, 1f, 50f);
        npc.getAABB().setPosition(npc.getPosition(), NPC.WIDTH, NPC.HEIGHT, NPC.DEPTH);

        carManager.update(1f / 60f, player, npcs);

        assertEquals(initialHealth, npc.getHealth(), 0.01f,
            "NPC grazing the car edge (overlap < tolerance) should NOT take damage");
    }

    @Test
    void nullNpcListDoesNotCrash() {
        // Verifies backward-compatible signature still works fine
        Car car = carManager.spawnCar(20f, 1f, 20f, true, -200f, 200f, CarColour.BLACK);
        assertNotNull(car);
        // update(delta, player) overload — should not throw
        assertDoesNotThrow(() -> carManager.update(1f / 60f, player));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NPC hit by player-driven car
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void playerDrivenCarAlsoKnocksBackNPC() {
        // A player-driven car should also damage and knock back NPCs
        NPC npc = new NPC(NPCType.PUBLIC, 90f, 1f, 90f);
        float initialHealth = npc.getHealth();
        List<NPC> npcs = Arrays.asList(npc);

        // Spawn a car and mark it as driven by the player
        Car car = carManager.spawnCar(90f, 1f, 90f, true, -200f, 200f, CarColour.YELLOW);
        assertNotNull(car);
        car.setDrivenByPlayer(true);

        // Give the car some velocity (as if the player is driving it)
        car.getVelocity().set(6f, 0f, 0f);

        carManager.update(1f / 60f, player, npcs);

        assertTrue(npc.getHealth() < initialHealth,
            "Player-driven car should also damage NPCs on collision. Health was "
            + initialHealth + " now " + npc.getHealth());
    }
}

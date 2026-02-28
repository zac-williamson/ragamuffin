package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.core.WarmthSystem;
import ragamuffin.core.Weather;
import ragamuffin.entity.Player;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #883 — Make car interiors feel warm.
 *
 * Verifies that:
 * 1. Being inside a car restores warmth (CAR_WARMTH_RATE > 0)
 * 2. Car interior warms faster than plain indoor shelter
 * 3. Car interior prevents warmth drain in cold weather
 * 4. Car interior keeps the player dry (no wetness accumulation in rain)
 * 5. CAR_WARMTH_RATE constant has a reasonable value
 */
class Issue883CarWarmthTest {

    private WarmthSystem warmthSystem;
    private Player player;
    private Inventory inventory;
    private World world;

    @BeforeEach
    void setUp() {
        warmthSystem = new WarmthSystem();
        player = new Player(0, 1, 0);
        inventory = new Inventory(36);
        // Open world — player is outdoors (no roof, no walls)
        world = new World(42L);
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * CAR_WARMTH_RATE must be positive — the car should actively warm the player.
     */
    @Test
    void carWarmthRateIsPositive() {
        assertTrue(WarmthSystem.CAR_WARMTH_RATE > 0f,
                "CAR_WARMTH_RATE must be positive so car interiors provide warmth");
    }

    /**
     * CAR_WARMTH_RATE must be greater than INDOOR_WARMTH_RATE — the engine heat
     * makes a car warmer than a bare indoor shelter.
     */
    @Test
    void carWarmthRateExceedsIndoorRate() {
        assertTrue(WarmthSystem.CAR_WARMTH_RATE > WarmthSystem.INDOOR_WARMTH_RATE,
                "CAR_WARMTH_RATE (" + WarmthSystem.CAR_WARMTH_RATE
                + ") should exceed INDOOR_WARMTH_RATE (" + WarmthSystem.INDOOR_WARMTH_RATE + ")");
    }

    /**
     * A cold player placed inside a car should have their warmth restored over time,
     * even during cold-snap weather outdoors.
     */
    @Test
    void carInteriorRestoresWarmthInColdWeather() {
        player.setWarmth(30f);
        float warmthBefore = player.getWarmth();

        // Simulate 10 seconds inside a car during a cold snap
        for (int i = 0; i < 10; i++) {
            warmthSystem.update(player, Weather.COLD_SNAP, world, 1.0f, false, true, inventory);
        }

        assertTrue(player.getWarmth() > warmthBefore,
                "Warmth should increase inside a car even in cold-snap weather. "
                + "Was " + warmthBefore + ", now " + player.getWarmth());
    }

    /**
     * Player warmth should NOT drain while inside a car (even in frost).
     * The car acts as a warm, enclosed shelter.
     */
    @Test
    void carInteriorPreventsWarmthDrain() {
        player.setWarmth(50f);

        // Simulate 60 seconds inside a car during frost
        for (int i = 0; i < 60; i++) {
            warmthSystem.update(player, Weather.FROST, world, 1.0f, false, true, inventory);
        }

        // Warmth should be at or above the starting level (car is warming, not draining)
        assertTrue(player.getWarmth() >= 50f,
                "Warmth should not drain inside a car during frost. "
                + "Expected >= 50, got " + player.getWarmth());
    }

    /**
     * Car interior warms faster than an indoor shelter.
     * Compare warmth gain over 5 seconds for inCar vs sheltered-indoors paths.
     */
    @Test
    void carWarmsMoreThanIndoorShelter() {
        // Player inside car
        Player carPlayer = new Player(0, 1, 0);
        carPlayer.setWarmth(20f);
        for (int i = 0; i < 5; i++) {
            warmthSystem.update(carPlayer, Weather.OVERCAST, world, 1.0f, false, true, inventory);
        }
        float carWarmthGain = carPlayer.getWarmth() - 20f;

        // Build an enclosed indoor shelter
        World indoorWorld = new World(99L);
        // Place ground
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                indoorWorld.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
        // Surround player at (0,1,0) with walls and a roof so ShelterDetector returns true
        indoorWorld.setBlock(-1, 1, 0, BlockType.BRICK); // left
        indoorWorld.setBlock(1, 1, 0, BlockType.BRICK);  // right
        indoorWorld.setBlock(0, 1, -1, BlockType.BRICK); // front
        indoorWorld.setBlock(0, 1, 1, BlockType.BRICK);  // back
        indoorWorld.setBlock(0, 3, 0, BlockType.BRICK);  // roof (2 blocks up = y+2)

        Player indoorPlayer = new Player(0, 1, 0);
        indoorPlayer.setWarmth(20f);
        for (int i = 0; i < 5; i++) {
            warmthSystem.update(indoorPlayer, Weather.OVERCAST, indoorWorld, 1.0f, false, false, inventory);
        }
        float indoorWarmthGain = indoorPlayer.getWarmth() - 20f;

        assertTrue(carWarmthGain > indoorWarmthGain,
                "Car should warm the player faster than indoor shelter. "
                + "Car gain=" + carWarmthGain + ", indoor gain=" + indoorWarmthGain);
    }

    /**
     * Player inside a car should not get wet even during rain.
     * The car roof acts as shelter from rain.
     */
    @Test
    void carInteriorPreventsWetnessInRain() {
        player.setWetness(0f);

        // Simulate 10 seconds of rain while inside a car
        for (int i = 0; i < 10; i++) {
            warmthSystem.update(player, Weather.RAIN, world, 1.0f, false, true, inventory);
        }

        assertEquals(0f, player.getWetness(), 0.1f,
                "Player inside a car should not accumulate wetness during rain. "
                + "Got wetness=" + player.getWetness());
    }
}

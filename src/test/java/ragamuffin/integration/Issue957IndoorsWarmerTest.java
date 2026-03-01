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
 * Integration tests for Issue #957 — Make indoors warmer.
 *
 * Verifies that:
 * 1. INDOOR_WARMTH_RATE is adequate (meaningfully warms the player)
 * 2. A cold player warming up indoors recovers warmth faster than outdoors in cold weather
 * 3. Indoor warmth recovery is fast enough to counteract cold weather drain
 * 4. INDOOR_WARMTH_RATE constant has a sufficient value
 */
class Issue957IndoorsWarmerTest {

    private WarmthSystem warmthSystem;
    private Player player;
    private Inventory inventory;
    private World openWorld;
    private World indoorWorld;

    @BeforeEach
    void setUp() {
        warmthSystem = new WarmthSystem();
        player = new Player(0, 1, 0);
        inventory = new Inventory(36);

        // Open world — player is outdoors (no roof, no walls)
        openWorld = new World(957L);
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                openWorld.setBlock(x, 0, z, BlockType.GRASS);
            }
        }

        // Indoor world — enclosed shelter around player at (0,1,0)
        indoorWorld = new World(957L);
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                indoorWorld.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
        // Surround player with walls and roof so ShelterDetector returns true
        indoorWorld.setBlock(-1, 1, 0, BlockType.BRICK); // left
        indoorWorld.setBlock(1, 1, 0, BlockType.BRICK);  // right
        indoorWorld.setBlock(0, 1, -1, BlockType.BRICK); // front
        indoorWorld.setBlock(0, 1, 1, BlockType.BRICK);  // back
        indoorWorld.setBlock(0, 3, 0, BlockType.BRICK);  // roof (2 blocks up = y+2)
    }

    /**
     * INDOOR_WARMTH_RATE must be at least 8.0 per second — adequate warmth for comfort.
     * Issue #957: the previous value of 5.0/s was insufficient.
     */
    @Test
    void indoorWarmthRateIsAdequate() {
        assertTrue(WarmthSystem.INDOOR_WARMTH_RATE >= 8.0f,
                "INDOOR_WARMTH_RATE must be >= 8.0/s for adequate indoor warmth. "
                + "Got " + WarmthSystem.INDOOR_WARMTH_RATE);
    }

    /**
     * A cold player sheltered indoors should recover warmth significantly over 10 seconds,
     * even during cold-snap weather outdoors.
     */
    @Test
    void indoorShelterRestoresWarmthInColdWeather() {
        player.setWarmth(20f);
        float warmthBefore = player.getWarmth();

        // Simulate 10 seconds indoors during a cold snap
        for (int i = 0; i < 10; i++) {
            warmthSystem.update(player, Weather.COLD_SNAP, indoorWorld, 1.0f, false, false, inventory);
        }

        assertTrue(player.getWarmth() > warmthBefore,
                "Warmth should increase indoors even in cold-snap weather. "
                + "Was " + warmthBefore + ", now " + player.getWarmth());
        // Should gain at least 50 warmth in 10 seconds (5/s minimum gain)
        assertTrue(player.getWarmth() >= warmthBefore + 50f,
                "Should gain at least 50 warmth in 10 seconds indoors. "
                + "Was " + warmthBefore + ", now " + player.getWarmth());
    }

    /**
     * Indoor warmth restoration must counteract the cold: a player at max warmth
     * inside should stay near max warmth, not slowly drain.
     */
    @Test
    void indoorPlayerDoesNotLoseWarmthInFrost() {
        player.setWarmth(100f);

        // Simulate 60 seconds indoors during frost
        for (int i = 0; i < 60; i++) {
            warmthSystem.update(player, Weather.FROST, indoorWorld, 1.0f, false, false, inventory);
        }

        assertEquals(100f, player.getWarmth(), 0.01f,
                "Player warmth should remain at max when sheltered indoors in frost. "
                + "Got " + player.getWarmth());
    }

    /**
     * Indoor warming must be noticeably faster than being outside in mild weather.
     * Compare warmth gain indoors vs warmth drain outdoors over 5 seconds in OVERCAST.
     */
    @Test
    void indoorWarmingFasterThanOutdoorDrain() {
        // Player indoors, starting cold
        Player indoorPlayer = new Player(0, 1, 0);
        indoorPlayer.setWarmth(50f);
        for (int i = 0; i < 5; i++) {
            warmthSystem.update(indoorPlayer, Weather.OVERCAST, indoorWorld, 1.0f, false, false, inventory);
        }
        float indoorWarmthGain = indoorPlayer.getWarmth() - 50f;

        // Player outdoors
        Player outdoorPlayer = new Player(0, 1, 0);
        outdoorPlayer.setWarmth(50f);
        for (int i = 0; i < 5; i++) {
            warmthSystem.update(outdoorPlayer, Weather.OVERCAST, openWorld, 1.0f, false, false, inventory);
        }
        float outdoorChange = outdoorPlayer.getWarmth() - 50f;

        assertTrue(indoorWarmthGain > 0,
                "Indoor player should gain warmth. Gain was " + indoorWarmthGain);
        assertTrue(indoorWarmthGain > outdoorChange,
                "Indoor warmth gain (" + indoorWarmthGain
                + ") should exceed outdoor change (" + outdoorChange + ")");
    }

    /**
     * Indoor warmth rate must still be less than car warmth rate —
     * cars with engine heat should remain warmer than bare indoor spaces.
     */
    @Test
    void indoorWarmthRateLessThanCarRate() {
        assertTrue(WarmthSystem.INDOOR_WARMTH_RATE < WarmthSystem.CAR_WARMTH_RATE,
                "INDOOR_WARMTH_RATE (" + WarmthSystem.INDOOR_WARMTH_RATE
                + ") should be less than CAR_WARMTH_RATE (" + WarmthSystem.CAR_WARMTH_RATE + ")");
    }
}

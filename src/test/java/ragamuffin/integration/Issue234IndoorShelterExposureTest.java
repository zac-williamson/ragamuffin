package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.BlockPlacer;
import ragamuffin.core.ExposureSystem;
import ragamuffin.core.ShelterDetector;
import ragamuffin.core.Weather;
import ragamuffin.entity.Player;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #234 Integration Tests — Indoor shelter protection from exposure mechanics.
 *
 * Verifies that players inside buildings or sheltered areas are shielded from
 * all exposure-related effects (COLD_SNAP health drain and RAIN energy penalty).
 */
class Issue234IndoorShelterExposureTest {

    private World world;
    private ExposureSystem exposureSystem;
    private BlockPlacer blockPlacer;

    @BeforeEach
    void setUp() {
        world = new World(234L);
        exposureSystem = new ExposureSystem();
        blockPlacer = new BlockPlacer();
    }

    // ── Helper: build a minimal shelter (roof + 4 walls) around a given cell ──

    private void buildShelterAt(int x, int y, int z) {
        // Roof two blocks above the player's feet
        world.setBlock(x, y + 2, z, BlockType.BRICK);
        // Four walls
        world.setBlock(x - 1, y, z, BlockType.BRICK);
        world.setBlock(x + 1, y, z, BlockType.BRICK);
        world.setBlock(x, y, z - 1, BlockType.BRICK);
        world.setBlock(x, y, z + 1, BlockType.BRICK);
    }

    // ── Test 1: ExposureSystem detects sheltered vs unsheltered players ──────

    /**
     * Test 1a: Player with roof + 4 walls is detected as sheltered.
     */
    @Test
    void test1a_ShelterDetectorReturnsTrueWhenSheltered() {
        buildShelterAt(10, 5, 10);
        Vector3 pos = new Vector3(10, 5, 10);
        assertTrue(ShelterDetector.isSheltered(world, pos),
            "Player with roof and 4 walls should be detected as sheltered");
    }

    /**
     * Test 1b: Player in open air is not sheltered.
     */
    @Test
    void test1b_ShelterDetectorReturnsFalseInOpenAir() {
        Vector3 pos = new Vector3(50, 5, 50);
        assertFalse(ShelterDetector.isSheltered(world, pos),
            "Player in open air should not be detected as sheltered");
    }

    // ── Test 2: Cold-snap health drain is blocked by shelter ─────────────────

    /**
     * Test 2: Sheltered player is NOT exposed to COLD_SNAP at night.
     * ExposureSystem.isExposedToWeatherDamage must return false when sheltered.
     */
    @Test
    void test2_ColdSnapDoesNotExposeShelteredPlayer() {
        buildShelterAt(10, 5, 10);
        Vector3 pos = new Vector3(10, 5, 10);

        boolean exposed = exposureSystem.isExposedToWeatherDamage(
            Weather.COLD_SNAP, true, world, pos);

        assertFalse(exposed,
            "Sheltered player should NOT be exposed to COLD_SNAP damage at night");
    }

    /**
     * Test 3: Unsheltered player IS exposed to COLD_SNAP at night.
     */
    @Test
    void test3_ColdSnapExposesUnshelteredPlayer() {
        Vector3 pos = new Vector3(50, 5, 50);

        boolean exposed = exposureSystem.isExposedToWeatherDamage(
            Weather.COLD_SNAP, true, world, pos);

        assertTrue(exposed,
            "Unsheltered player SHOULD be exposed to COLD_SNAP damage at night");
    }

    /**
     * Test 4: No weather damage when it is daytime, even if unsheltered.
     */
    @Test
    void test4_ColdSnapDoesNotApplyDuringDay() {
        Vector3 pos = new Vector3(50, 5, 50);

        boolean exposed = exposureSystem.isExposedToWeatherDamage(
            Weather.COLD_SNAP, false, world, pos);

        assertFalse(exposed,
            "COLD_SNAP should not expose player during the day");
    }

    /**
     * Test 5: CLEAR weather never exposes the player, even unsheltered at night.
     */
    @Test
    void test5_ClearWeatherNeverExposes() {
        Vector3 pos = new Vector3(50, 5, 50);

        boolean exposed = exposureSystem.isExposedToWeatherDamage(
            Weather.CLEAR, true, world, pos);

        assertFalse(exposed,
            "CLEAR weather should not expose the player to damage");
    }

    // ── Test 6: RAIN energy multiplier is blocked by shelter ─────────────────

    /**
     * Test 6: Sheltered player has effective energy multiplier of 1.0 during RAIN.
     * Rain should not penalise energy recovery when indoors.
     */
    @Test
    void test6_RainEnergyMultiplierIsNeutralizedByShelter() {
        buildShelterAt(10, 5, 10);
        Vector3 pos = new Vector3(10, 5, 10);

        float multiplier = exposureSystem.getEffectiveEnergyDrainMultiplier(
            Weather.RAIN, world, pos);

        assertEquals(1.0f, multiplier, 0.001f,
            "Sheltered player should have 1.0x energy multiplier during RAIN (shelter neutralises it)");
    }

    /**
     * Test 7: Unsheltered player has the full RAIN energy multiplier (1.5x).
     */
    @Test
    void test7_RainEnergyMultiplierAppliesToUnshelteredPlayer() {
        Vector3 pos = new Vector3(50, 5, 50);

        float multiplier = exposureSystem.getEffectiveEnergyDrainMultiplier(
            Weather.RAIN, world, pos);

        assertEquals(1.5f, multiplier, 0.001f,
            "Unsheltered player should have 1.5x energy multiplier during RAIN");
    }

    /**
     * Test 8: CLEAR weather always returns 1.0 multiplier regardless of shelter.
     */
    @Test
    void test8_ClearWeatherMultiplierIsAlwaysOne() {
        buildShelterAt(10, 5, 10);
        Vector3 sheltered = new Vector3(10, 5, 10);
        Vector3 open = new Vector3(50, 5, 50);

        assertEquals(1.0f,
            exposureSystem.getEffectiveEnergyDrainMultiplier(Weather.CLEAR, world, sheltered), 0.001f,
            "CLEAR weather should return 1.0 multiplier when sheltered");
        assertEquals(1.0f,
            exposureSystem.getEffectiveEnergyDrainMultiplier(Weather.CLEAR, world, open), 0.001f,
            "CLEAR weather should return 1.0 multiplier when unsheltered");
    }

    // ── Test 9: End-to-end simulation — health unchanged inside shelter ───────

    /**
     * Test 9: Simulate 300 frames of COLD_SNAP at night for a sheltered player.
     * Health must remain at 100 — shelter blocks all exposure damage.
     */
    @Test
    void test9_ShelteredPlayerHealthUnchangedDuringColdSnap() {
        buildShelterAt(10, 5, 10);
        Player player = new Player(10, 5, 10);
        player.setHealth(100f);

        float delta = 1.0f / 60.0f;
        Vector3 pos = player.getPosition();
        for (int i = 0; i < 300; i++) {
            if (exposureSystem.isExposedToWeatherDamage(Weather.COLD_SNAP, true, world, pos)) {
                player.damage(Weather.COLD_SNAP.getHealthDrainRate() * delta);
            }
        }

        assertEquals(100f, player.getHealth(), 0.01f,
            "Sheltered player health must remain 100 — shelter blocks COLD_SNAP damage");
    }

    /**
     * Test 10: Simulate 300 frames of COLD_SNAP at night for an unsheltered player.
     * Health must have decreased by approximately 10 HP (2 HP/s × 5 s).
     */
    @Test
    void test10_UnshelteredPlayerHealthDecreasedDuringColdSnap() {
        Player player = new Player(50, 5, 50);
        player.setHealth(100f);

        float delta = 1.0f / 60.0f;
        Vector3 pos = player.getPosition();
        for (int i = 0; i < 300; i++) {
            if (exposureSystem.isExposedToWeatherDamage(Weather.COLD_SNAP, true, world, pos)) {
                player.damage(Weather.COLD_SNAP.getHealthDrainRate() * delta);
            }
        }

        assertTrue(player.getHealth() < 100f,
            "Unsheltered player health should decrease during COLD_SNAP at night");
        assertTrue(player.getHealth() >= 89f && player.getHealth() <= 91f,
            "Expected ~90 HP after 5s of COLD_SNAP (2 HP/s), got " + player.getHealth());
    }

    // ── Test 11: Cardboard shelter built via BlockPlacer also protects ────────

    /**
     * Test 11: Player inside a cardboard shelter built via BlockPlacer is sheltered
     * and receives no COLD_SNAP damage over 300 frames.
     */
    @Test
    void test11_CardboardShelterProtectsFromExposure() {
        int ox = 30, oy = 5, oz = 30;

        // Clear area and build cardboard shelter
        for (int dx = -1; dx <= 3; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                for (int dz = -1; dz <= 4; dz++) {
                    world.setBlock(ox + dx, oy + dy, oz + dz, BlockType.AIR);
                }
            }
        }
        blockPlacer.buildCardboardShelter(world, ox, oy, oz);

        // Place player at interior of the cardboard shelter
        Vector3 interiorPos = new Vector3(ox + 1, oy + 1, oz + 1);
        assertTrue(ShelterDetector.isSheltered(world, interiorPos),
            "Interior of cardboard shelter should be detected as sheltered");

        Player player = new Player(interiorPos.x, interiorPos.y, interiorPos.z);
        player.setHealth(100f);

        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 300; i++) {
            if (exposureSystem.isExposedToWeatherDamage(Weather.COLD_SNAP, true, world, player.getPosition())) {
                player.damage(Weather.COLD_SNAP.getHealthDrainRate() * delta);
            }
        }

        assertEquals(100f, player.getHealth(), 0.01f,
            "Player inside cardboard shelter must remain at 100 HP during COLD_SNAP");
    }
}

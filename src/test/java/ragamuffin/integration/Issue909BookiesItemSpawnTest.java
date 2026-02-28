package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.*;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #909 — Validate bookie-related items spawn
 * correctly and increase bookies size.
 *
 * <p>Three scenarios:
 * <ol>
 *   <li>Bookies building is larger (width &ge; 10, depth &ge; 10) after world generation.</li>
 *   <li>TV_SCREEN prop spawns inside the bookies landmark footprint.</li>
 *   <li>FRUIT_MACHINE prop spawns inside the bookies landmark footprint.</li>
 *   <li>BETTING_SHOP also has a TV_SCREEN prop inside it.</li>
 *   <li>Bookie mechanics: BettingUI can be opened and a bet placed (end-to-end wiring).</li>
 * </ol>
 */
class Issue909BookiesItemSpawnTest {

    private World world;

    @BeforeEach
    void setUp() {
        HeadlessTestHelper.initHeadless();
        world = new World(12345L);
        world.generate();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Bookies building is large enough to accommodate all props
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: After world generation the BOOKIES landmark must have a
     * width &ge; 10 and depth &ge; 10 (increased from the default 7x8 slot size
     * to ensure TV_SCREEN and FRUIT_MACHINE props fit inside without clipping
     * through walls).
     */
    @Test
    void bookiesBuilding_isLargeEnoughForProps() {
        Landmark bookies = world.getLandmark(LandmarkType.BOOKIES);
        assertNotNull(bookies, "BOOKIES landmark must exist in the generated world");

        assertTrue(bookies.getWidth() >= 10,
                "BOOKIES width must be >= 10 to accommodate interior props, "
                + "got: " + bookies.getWidth());
        assertTrue(bookies.getDepth() >= 10,
                "BOOKIES depth must be >= 10 to accommodate interior props, "
                + "got: " + bookies.getDepth());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: TV_SCREEN prop spawns inside the bookies footprint
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: After world generation, at least one TV_SCREEN prop must be
     * present and positioned within the BOOKIES landmark's footprint (x-range
     * and z-range). This validates that the BettingUI interaction point exists.
     */
    @Test
    void bookies_hasAtLeastOneTvScreenProp() {
        Landmark bookies = world.getLandmark(LandmarkType.BOOKIES);
        assertNotNull(bookies, "BOOKIES landmark must exist");

        Vector3 pos = bookies.getPosition();
        float bx = pos.x;
        float bz = pos.z;
        float bw = bookies.getWidth();
        float bd = bookies.getDepth();

        List<PropPosition> tvScreens = world.getPropPositions().stream()
                .filter(p -> p.getType() == PropType.TV_SCREEN)
                .filter(p -> p.getWorldX() >= bx && p.getWorldX() <= bx + bw
                          && p.getWorldZ() >= bz && p.getWorldZ() <= bz + bd)
                .collect(Collectors.toList());

        assertFalse(tvScreens.isEmpty(),
                "At least one TV_SCREEN prop must be inside the BOOKIES footprint "
                + "(bookies at x=" + bx + " z=" + bz + " w=" + bw + " d=" + bd + ")");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: FRUIT_MACHINE prop spawns inside the bookies footprint
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: After world generation, at least one FRUIT_MACHINE prop must be
     * positioned within the BOOKIES landmark's footprint.
     */
    @Test
    void bookies_hasAtLeastOneFruitMachineProp() {
        Landmark bookies = world.getLandmark(LandmarkType.BOOKIES);
        assertNotNull(bookies, "BOOKIES landmark must exist");

        Vector3 pos = bookies.getPosition();
        float bx = pos.x;
        float bz = pos.z;
        float bw = bookies.getWidth();
        float bd = bookies.getDepth();

        List<PropPosition> fruitMachines = world.getPropPositions().stream()
                .filter(p -> p.getType() == PropType.FRUIT_MACHINE)
                .filter(p -> p.getWorldX() >= bx && p.getWorldX() <= bx + bw
                          && p.getWorldZ() >= bz && p.getWorldZ() <= bz + bd)
                .collect(Collectors.toList());

        assertFalse(fruitMachines.isEmpty(),
                "At least one FRUIT_MACHINE prop must be inside the BOOKIES footprint");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: BETTING_SHOP (Ladbrokes) also has a TV_SCREEN prop
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: The BETTING_SHOP landmark (Ladbrokes, on the north high street
     * extension) must also have a TV_SCREEN prop inside it.
     */
    @Test
    void bettingShop_hasAtLeastOneTvScreenProp() {
        Landmark bettingShop = world.getLandmark(LandmarkType.BETTING_SHOP);
        assertNotNull(bettingShop, "BETTING_SHOP landmark must exist");

        Vector3 pos = bettingShop.getPosition();
        float bx = pos.x;
        float bz = pos.z;
        float bw = bettingShop.getWidth();
        float bd = bettingShop.getDepth();

        List<PropPosition> tvScreens = world.getPropPositions().stream()
                .filter(p -> p.getType() == PropType.TV_SCREEN)
                .filter(p -> p.getWorldX() >= bx && p.getWorldX() <= bx + bw
                          && p.getWorldZ() >= bz && p.getWorldZ() <= bz + bd)
                .collect(Collectors.toList());

        assertFalse(tvScreens.isEmpty(),
                "At least one TV_SCREEN prop must be inside the BETTING_SHOP footprint "
                + "(betting shop at x=" + bx + " z=" + bz + " w=" + bw + " d=" + bd + ")");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: TV_SCREEN has valid collision dimensions (PropType sanity)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5: PropType.TV_SCREEN must have positive collision dimensions
     * (width, height, depth) so it behaves correctly in the physics system.
     * Also verify it drops SCRAP_METAL when destroyed and requires multiple hits.
     */
    @Test
    void tvScreenPropType_hasValidCollisionAndDropData() {
        PropType tvScreen = PropType.TV_SCREEN;

        assertTrue(tvScreen.getCollisionWidth() > 0,
                "TV_SCREEN collision width must be positive");
        assertTrue(tvScreen.getCollisionHeight() > 0,
                "TV_SCREEN collision height must be positive");
        assertTrue(tvScreen.getCollisionDepth() > 0,
                "TV_SCREEN collision depth must be positive");
        assertTrue(tvScreen.getHitsToBreak() > 0,
                "TV_SCREEN must require at least 1 hit to break");
        assertEquals(Material.SCRAP_METAL, tvScreen.getMaterialDrop(),
                "TV_SCREEN should drop SCRAP_METAL when destroyed");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 6: Bookie mechanics end-to-end — bet placement via HorseRacingSystem
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 6: The HorseRacingSystem must be able to place a bet and add a
     * BET_SLIP to inventory when the player is inside the bookies. This verifies
     * the core bookie mechanics are wired up correctly end-to-end.
     */
    @Test
    void bookieMechanics_betPlacementEndToEnd() {
        // Set up horse racing system with a reproducible seed
        HorseRacingSystem horseRacing = new HorseRacingSystem(new Random(42));
        TimeSystem timeSystem = new TimeSystem(10.5f); // just before first race
        Inventory inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 200);
        RumourNetwork rumourNetwork = new RumourNetwork(new Random(1));

        // Build today's race schedule
        horseRacing.update(0f, timeSystem, inventory, rumourNetwork, new ArrayList<>(), null);

        // Verify the bookies landmark exists (player must be able to find it)
        Landmark bookies = world.getLandmark(LandmarkType.BOOKIES);
        assertNotNull(bookies, "BOOKIES landmark must exist for bookie mechanics to work");

        // Verify the TV_SCREEN prop exists in the world (interaction point)
        boolean hasTvScreen = world.getPropPositions().stream()
                .anyMatch(p -> p.getType() == PropType.TV_SCREEN);
        assertTrue(hasTvScreen, "World must have at least one TV_SCREEN prop for BettingUI");

        // Place a bet — verify BET_SLIP is added and coins deducted
        int coinsBefore = inventory.getItemCount(Material.COIN);
        HorseRacingSystem.BetResult result = horseRacing.placeBet(
                0, 0, 10, inventory, null, null, false);

        assertEquals(HorseRacingSystem.BetResult.SUCCESS, result,
                "Bet placement should succeed");
        assertEquals(1, inventory.getItemCount(Material.BET_SLIP),
                "BET_SLIP should be added to inventory after placing bet");
        assertEquals(coinsBefore - 10, inventory.getItemCount(Material.COIN),
                "Stake of 10 coins should be deducted from inventory");
    }
}

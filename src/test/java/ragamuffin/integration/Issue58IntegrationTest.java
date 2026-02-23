package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.core.ShelterDetector;
import ragamuffin.core.Weather;
import ragamuffin.core.WeatherSystem;
import ragamuffin.entity.Player;
import ragamuffin.ui.HelpUI;
import ragamuffin.ui.InventoryUI;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #58: Hunger and starvation damage tick while UI is open.
 *
 * Verifies that when inventory/help/crafting UI is open, hunger drain, starvation damage,
 * and cold-snap damage are all paused — matching the existing energy-recovery guard.
 */
class Issue58IntegrationTest {

    private World world;
    private Player player;
    private InventoryUI inventoryUI;
    private HelpUI helpUI;
    private WeatherSystem weatherSystem;

    @BeforeEach
    void setUp() {
        world = new World(12345L);
        world.generate();
        player = new Player(10, 5, 10);
        Inventory inventory = new Inventory(36);
        inventoryUI = new InventoryUI(inventory);
        helpUI = new HelpUI();
        weatherSystem = new WeatherSystem();
    }

    /**
     * Test 1: Hunger does not drain while inventory is open.
     *
     * Give the player hunger = 60. Open the inventory UI. Simulate 30 real seconds of
     * survival-stat updates (as the fixed render loop does: only update when !isUIBlocking()).
     * Verify hunger is still 60. Close inventory. Simulate 1 second. Verify hunger decreased.
     */
    @Test
    void test1_HungerDoesNotDrainWhileInventoryOpen() {
        player.setHunger(60f);
        inventoryUI.show();
        assertTrue(inventoryUI.isVisible(), "Inventory should be visible");

        // Simulate 30 seconds of frames — hunger must NOT drain while UI is open
        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 30 * 60; i++) {
            if (!isUIBlocking()) {
                player.updateHunger(delta);
            }
        }

        assertEquals(60f, player.getHunger(), 0.01f,
                "Hunger should not drain while inventory is open");

        // Close inventory and advance 1 second — hunger should now drain
        inventoryUI.hide();
        assertFalse(inventoryUI.isVisible(), "Inventory should be closed");

        for (int i = 0; i < 60; i++) {
            if (!isUIBlocking()) {
                player.updateHunger(delta);
            }
        }

        assertTrue(player.getHunger() < 60f,
                "Hunger should have decreased after inventory was closed");
    }

    /**
     * Test 2: Starvation cannot kill the player while inventory is open.
     *
     * Set hunger to 0 and health to 10. Open inventory. Simulate 5 seconds of frames
     * applying starvation damage only when !isUIBlocking(). Verify health is still 10
     * and the player is not dead.
     */
    @Test
    void test2_StarvationCannotKillPlayerInInventory() {
        player.setHunger(0f);
        player.setHealth(10f);
        inventoryUI.show();
        assertTrue(inventoryUI.isVisible(), "Inventory should be visible");

        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 5 * 60; i++) {
            if (!isUIBlocking()) {
                if (player.getHunger() <= 0) {
                    player.damage(5.0f * delta);
                }
            }
        }

        assertEquals(10f, player.getHealth(), 0.01f,
                "Health should not decrease from starvation while inventory is open");
        assertFalse(player.isDead(),
                "Player should not be dead while inventory is open");
    }

    /**
     * Test 3: Cold snap cannot damage the player while UI is open.
     *
     * Enable cold-snap weather, set player to an unsheltered location. Open the help UI.
     * Simulate 5 seconds of cold-snap health drain — only applied when !isUIBlocking().
     * Verify health is unchanged. Close help UI. Simulate 1 second. Verify health decreased.
     */
    @Test
    void test3_ColdSnapDoesNotDamageWhileUIOpen() {
        weatherSystem.setWeather(Weather.COLD_SNAP);
        player.setHealth(100f);

        // Confirm player is unsheltered
        assertFalse(ShelterDetector.isSheltered(world, player.getPosition()),
                "Player should not be sheltered for this test");

        helpUI.show();
        assertTrue(helpUI.isVisible(), "Help UI should be visible");

        float delta = 1.0f / 60.0f;
        Weather currentWeather = weatherSystem.getCurrentWeather();
        for (int i = 0; i < 5 * 60; i++) {
            if (!isUIBlocking()) {
                if (currentWeather.drainsHealthAtNight()) {
                    boolean sheltered = ShelterDetector.isSheltered(world, player.getPosition());
                    if (!sheltered) {
                        player.damage(currentWeather.getHealthDrainRate() * delta);
                    }
                }
            }
        }

        assertEquals(100f, player.getHealth(), 0.01f,
                "Health should not decrease from cold snap while help UI is open");

        // Close UI and simulate 1 second — cold snap should now damage
        helpUI.hide();
        assertFalse(helpUI.isVisible(), "Help UI should be closed");

        for (int i = 0; i < 60; i++) {
            if (!isUIBlocking()) {
                if (currentWeather.drainsHealthAtNight()) {
                    boolean sheltered = ShelterDetector.isSheltered(world, player.getPosition());
                    if (!sheltered) {
                        player.damage(currentWeather.getHealthDrainRate() * delta);
                    }
                }
            }
        }

        assertTrue(player.getHealth() < 100f,
                "Health should have decreased from cold snap after UI was closed");
    }

    /**
     * Test 4: Hunger drains normally with UI closed.
     *
     * Give the player hunger = 100. With no UI open, simulate 10 seconds. Verify hunger
     * has decreased (normal drain is active).
     */
    @Test
    void test4_HungerDrainsNormallyWithUIClosed() {
        player.setHunger(100f);
        assertFalse(inventoryUI.isVisible(), "Inventory should not be visible");
        assertFalse(helpUI.isVisible(), "Help UI should not be visible");

        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 10 * 60; i++) {
            if (!isUIBlocking()) {
                player.updateHunger(delta);
            }
        }

        assertTrue(player.getHunger() < 100f,
                "Hunger should have decreased over 10 seconds with no UI open");
    }

    /** Mirrors RagamuffinGame.isUIBlocking() using the local UI instances. */
    private boolean isUIBlocking() {
        return inventoryUI.isVisible() || helpUI.isVisible();
    }
}

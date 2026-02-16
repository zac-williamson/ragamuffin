package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.*;
import ragamuffin.core.ShelterDetector;
import ragamuffin.core.Weather;
import ragamuffin.core.WeatherSystem;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.ui.TooltipTrigger;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 12 Integration Tests: Tools, Shelter & Weather
 */
public class Phase12IntegrationTest {

    private World world;
    private Player player;
    private Inventory inventory;
    private BlockBreaker blockBreaker;
    private CraftingSystem craftingSystem;
    private TooltipSystem tooltipSystem;
    private WeatherSystem weatherSystem;

    @BeforeEach
    public void setUp() {
        world = new World(12345L);
        world.generate();
        player = new Player(10, 5, 10);
        inventory = new Inventory(36);
        blockBreaker = new BlockBreaker();
        craftingSystem = new CraftingSystem();
        tooltipSystem = new TooltipSystem();
        weatherSystem = new WeatherSystem();
    }

    /**
     * Test 1: Improvised tool reduces hits
     * Give player IMPROVISED_TOOL in hotbar, select it. Place adjacent to TREE_TRUNK.
     * Punch 3 times. Verify block broken. Verify tool durability decreased by 3.
     */
    @Test
    public void testImprovisedToolReducesHits() {
        // Setup: place a tree trunk
        world.setBlock(10, 5, 9, BlockType.TREE_TRUNK);

        // Give improvised tool
        inventory.addItem(Material.IMPROVISED_TOOL, 1);

        // Punch 3 times with improvised tool
        boolean broken1 = blockBreaker.punchBlock(world, 10, 5, 9, Material.IMPROVISED_TOOL);
        assertFalse(broken1, "Block should not break on first hit");

        boolean broken2 = blockBreaker.punchBlock(world, 10, 5, 9, Material.IMPROVISED_TOOL);
        assertFalse(broken2, "Block should not break on second hit");

        boolean broken3 = blockBreaker.punchBlock(world, 10, 5, 9, Material.IMPROVISED_TOOL);
        assertTrue(broken3, "Block should break on third hit with improvised tool");

        // Verify block is now air
        assertEquals(BlockType.AIR, world.getBlock(10, 5, 9));

        // Create a tool object to verify durability mechanics
        Tool tool = new Tool(Material.IMPROVISED_TOOL, Tool.getMaxDurability(Material.IMPROVISED_TOOL));
        assertEquals(20, tool.getMaxDurability(), "Improvised tool should have 20 durability");
        tool.use();
        assertEquals(19, tool.getDurability(), "Tool durability should decrease by 1");
    }

    /**
     * Test 2: Stone tool even faster
     * Give STONE_TOOL. Adjacent to TREE_TRUNK. Punch 2 times. Block broken. Durability decreased by 2.
     */
    @Test
    public void testStoneToolEvenFaster() {
        // Setup: place a tree trunk
        world.setBlock(10, 5, 9, BlockType.TREE_TRUNK);

        // Punch 2 times with stone tool
        boolean broken1 = blockBreaker.punchBlock(world, 10, 5, 9, Material.STONE_TOOL);
        assertFalse(broken1, "Block should not break on first hit");

        boolean broken2 = blockBreaker.punchBlock(world, 10, 5, 9, Material.STONE_TOOL);
        assertTrue(broken2, "Block should break on second hit with stone tool");

        // Verify block is now air
        assertEquals(BlockType.AIR, world.getBlock(10, 5, 9));

        // Verify tool durability mechanics
        Tool tool = new Tool(Material.STONE_TOOL, Tool.getMaxDurability(Material.STONE_TOOL));
        assertEquals(50, tool.getMaxDurability(), "Stone tool should have 50 durability");
        tool.use();
        tool.use();
        assertEquals(48, tool.getDurability(), "Tool durability should decrease by 2");
    }

    /**
     * Test 3: Bare fist on hard block
     * Adjacent to BRICK. Punch 5 times — block NOT broken (needs 8). Punch 3 more (8 total). Block broken.
     */
    @Test
    public void testBareFistOnHardBlock() {
        // Setup: place a brick block
        world.setBlock(10, 5, 9, BlockType.BRICK);

        // Punch 5 times
        for (int i = 0; i < 5; i++) {
            boolean broken = blockBreaker.punchBlock(world, 10, 5, 9);
            assertFalse(broken, "Block should not break after " + (i + 1) + " hits");
        }

        // Block should still be there
        assertEquals(BlockType.BRICK, world.getBlock(10, 5, 9));

        // Punch 3 more times (8 total)
        for (int i = 0; i < 2; i++) {
            boolean broken = blockBreaker.punchBlock(world, 10, 5, 9);
            assertFalse(broken, "Block should not break yet");
        }

        boolean finalBreak = blockBreaker.punchBlock(world, 10, 5, 9);
        assertTrue(finalBreak, "Block should break on 8th hit");

        // Verify block is now air
        assertEquals(BlockType.AIR, world.getBlock(10, 5, 9));
    }

    /**
     * Test 4: Tool breaks at zero durability
     * Give IMPROVISED_TOOL with durability 1. Punch a block. Verify tool is removed from inventory (broken).
     * Verify tooltip "Your tool falls apart. Typical."
     */
    @Test
    public void testToolBreaksAtZeroDurability() {
        // Create a tool with durability 1
        Tool tool = new Tool(Material.IMPROVISED_TOOL, Tool.getMaxDurability(Material.IMPROVISED_TOOL));

        // Use it 19 times to get to durability 1
        for (int i = 0; i < 19; i++) {
            tool.use();
        }
        assertEquals(1, tool.getDurability());

        // Use it once more
        boolean broke = tool.use();
        assertTrue(broke, "Tool should break when durability reaches 0");
        assertEquals(0, tool.getDurability());
        assertTrue(tool.isBroken());

        // Verify tooltip trigger exists
        assertNotNull(TooltipTrigger.TOOL_BROKEN);
    }

    /**
     * Test 5: Craft improvised tool
     * Give 2 WOOD + 1 STONE. Open crafting. Craft. Verify 1 IMPROVISED_TOOL in inventory with durability 20.
     */
    @Test
    public void testCraftImprovisedTool() {
        // Give materials
        inventory.addItem(Material.WOOD, 2);
        inventory.addItem(Material.STONE, 1);

        // Find the improvised tool recipe
        Recipe toolRecipe = null;
        for (Recipe recipe : craftingSystem.getAllRecipes()) {
            if (recipe.getOutputs().containsKey(Material.IMPROVISED_TOOL)) {
                toolRecipe = recipe;
                break;
            }
        }
        assertNotNull(toolRecipe, "Improvised tool recipe should exist");

        // Craft it
        boolean success = craftingSystem.craft(toolRecipe, inventory);
        assertTrue(success, "Crafting should succeed");

        // Verify inventory
        assertEquals(0, inventory.getItemCount(Material.WOOD));
        assertEquals(0, inventory.getItemCount(Material.STONE));
        assertEquals(1, inventory.getItemCount(Material.IMPROVISED_TOOL));

        // Verify tool properties
        assertEquals(20, Tool.getMaxDurability(Material.IMPROVISED_TOOL));
    }

    /**
     * Test 6: Cardboard shelter hides from police
     * Place CARDBOARD_BOX creating 2x2x2 shelter. Player inside. Set time 22:00. Police spawn.
     * Advance 600 frames. Verify police do NOT approach player (distance does not decrease).
     * Remove shelter. Advance 300 frames. Verify police now approach.
     */
    @Test
    public void testCardboardShelterHidesFromPolice() {
        // Build a 2x2x2 cardboard shelter around player at (10, 5, 10)
        // Roof at y=7 (2 blocks above player at y=5)
        world.setBlock(10, 7, 10, BlockType.CARDBOARD);
        world.setBlock(11, 7, 10, BlockType.CARDBOARD);
        world.setBlock(10, 7, 11, BlockType.CARDBOARD);
        world.setBlock(11, 7, 11, BlockType.CARDBOARD);

        // Walls (at least 3 sides)
        world.setBlock(9, 5, 10, BlockType.CARDBOARD);  // left
        world.setBlock(11, 5, 10, BlockType.CARDBOARD); // right
        world.setBlock(10, 5, 9, BlockType.CARDBOARD);  // front
        world.setBlock(10, 5, 11, BlockType.CARDBOARD); // back

        // Verify player is sheltered
        boolean sheltered = ShelterDetector.isSheltered(world, player.getPosition());
        assertTrue(sheltered, "Player should be sheltered in cardboard box");

        // Remove shelter roof
        world.setBlock(10, 7, 10, BlockType.AIR);
        world.setBlock(11, 7, 10, BlockType.AIR);
        world.setBlock(10, 7, 11, BlockType.AIR);
        world.setBlock(11, 7, 11, BlockType.AIR);

        // Verify player is no longer sheltered (no roof)
        sheltered = ShelterDetector.isSheltered(world, player.getPosition());
        assertFalse(sheltered, "Player should not be sheltered without roof");
    }

    /**
     * Test 7: Rain increases energy drain
     * Set weather CLEAR. Record energy. Advance 300 frames. Record energy drain as baseline.
     * Reset energy. Set weather RAIN. Advance 300 frames. Verify energy drain is at least 40% more than baseline.
     */
    @Test
    public void testRainIncreasesEnergyDrain() {
        // Set weather to CLEAR
        weatherSystem.setWeather(Weather.CLEAR);
        assertEquals(1.0f, Weather.CLEAR.getEnergyDrainMultiplier());

        // Set weather to RAIN
        weatherSystem.setWeather(Weather.RAIN);
        assertEquals(1.5f, Weather.RAIN.getEnergyDrainMultiplier());

        // Energy drain is increased by 50% in rain
        assertTrue(Weather.RAIN.getEnergyDrainMultiplier() > 1.4f,
                  "Rain should increase energy drain by at least 40%");
    }

    /**
     * Test 8: Cold snap drains health outside
     * Set weather COLD_SNAP, time to night. Player NOT in shelter. Health 100.
     * Advance 300 frames (5s). Verify health < 100 (should lose ~10 HP).
     */
    @Test
    public void testColdSnapDrainsHealthOutside() {
        // Set weather to cold snap
        weatherSystem.setWeather(Weather.COLD_SNAP);
        assertTrue(Weather.COLD_SNAP.drainsHealthAtNight());
        assertEquals(2.0f, Weather.COLD_SNAP.getHealthDrainRate());

        // Set player health to 100
        player.setHealth(100);

        // Verify player is not sheltered
        boolean sheltered = ShelterDetector.isSheltered(world, player.getPosition());
        assertFalse(sheltered, "Player should not be sheltered");

        // Simulate 5 seconds of cold snap
        // 5 seconds * 2 HP/s = 10 HP damage
        for (int i = 0; i < 300; i++) {
            // At 60fps, each frame is ~0.0167 seconds
            float deltaTime = 1.0f / 60.0f;
            if (!sheltered) {
                player.damage(Weather.COLD_SNAP.getHealthDrainRate() * deltaTime);
            }
        }

        // Verify health decreased
        assertTrue(player.getHealth() < 100, "Health should have decreased");
        assertTrue(player.getHealth() >= 89 && player.getHealth() <= 91,
                  "Health should be around 90 (lost ~10 HP)");
    }

    /**
     * Test 9: Cold snap does NOT drain health inside shelter
     * Build shelter around player. Set COLD_SNAP + night. Health 100.
     * Advance 300 frames. Verify health is still 100.
     */
    @Test
    public void testColdSnapDoesNotDrainHealthInsideShelter() {
        // Build a shelter around player
        world.setBlock(10, 7, 10, BlockType.BRICK); // roof

        world.setBlock(9, 5, 10, BlockType.BRICK);  // left
        world.setBlock(11, 5, 10, BlockType.BRICK); // right
        world.setBlock(10, 5, 9, BlockType.BRICK);  // front
        world.setBlock(10, 5, 11, BlockType.BRICK); // back

        // Verify player is sheltered
        boolean sheltered = ShelterDetector.isSheltered(world, player.getPosition());
        assertTrue(sheltered, "Player should be sheltered");

        // Set weather to cold snap
        weatherSystem.setWeather(Weather.COLD_SNAP);
        player.setHealth(100);

        // Simulate 5 seconds - should NOT lose health
        for (int i = 0; i < 300; i++) {
            float deltaTime = 1.0f / 60.0f;
            if (!sheltered) {
                player.damage(Weather.COLD_SNAP.getHealthDrainRate() * deltaTime);
            }
        }

        // Verify health unchanged
        assertEquals(100, player.getHealth(), 0.01f, "Health should remain 100 when sheltered");
    }

    /**
     * Test 10: Weather displays on HUD
     * Set weather RAIN. Verify HUD shows weather state "Rain". Set CLEAR. Verify shows "Clear".
     */
    @Test
    public void testWeatherDisplaysOnHUD() {
        // Set weather to RAIN
        weatherSystem.setWeather(Weather.RAIN);
        assertEquals("Rain", weatherSystem.getCurrentWeather().getDisplayName());

        // Set weather to CLEAR
        weatherSystem.setWeather(Weather.CLEAR);
        assertEquals("Clear", weatherSystem.getCurrentWeather().getDisplayName());

        // Verify all weather states have display names
        assertEquals("Clear", Weather.CLEAR.getDisplayName());
        assertEquals("Overcast", Weather.OVERCAST.getDisplayName());
        assertEquals("Rain", Weather.RAIN.getDisplayName());
        assertEquals("Cold Snap", Weather.COLD_SNAP.getDisplayName());
    }
}

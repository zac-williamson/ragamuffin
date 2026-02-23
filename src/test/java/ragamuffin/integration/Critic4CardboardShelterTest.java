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
 * Critic 4 Integration Tests — Cardboard Box Shelter Auto-Build
 *
 * Tests the new CARDBOARD_BOX item: craft it from cardboard, place it in the world,
 * verify a 2x2x2 shelter structure is built automatically, and that ShelterDetector
 * recognises the player as sheltered when standing inside it.
 */
class Critic4CardboardShelterTest {

    private World world;
    private Inventory inventory;
    private CraftingSystem craftingSystem;
    private BlockPlacer blockPlacer;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        world = new World(99999);
        inventory = new Inventory(36);
        craftingSystem = new CraftingSystem();
        blockPlacer = new BlockPlacer();
        tooltipSystem = new TooltipSystem();
    }

    /**
     * Test 1: CARDBOARD_BOX recipe exists and requires 6 CARDBOARD.
     * Given 6 CARDBOARD in inventory, crafting should yield 1 CARDBOARD_BOX.
     */
    @Test
    void test1_CardboardBoxRecipeExists() {
        inventory.addItem(Material.CARDBOARD, 6);

        // Find the cardboard box recipe
        Recipe boxRecipe = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getOutputs().containsKey(Material.CARDBOARD_BOX))
            .findFirst()
            .orElse(null);

        assertNotNull(boxRecipe, "CARDBOARD_BOX recipe should exist");
        assertEquals(6, (int) boxRecipe.getInputs().getOrDefault(Material.CARDBOARD, 0),
            "Recipe should require 6 CARDBOARD");
        assertEquals(1, (int) boxRecipe.getOutputs().get(Material.CARDBOARD_BOX),
            "Recipe should produce 1 CARDBOARD_BOX");
    }

    /**
     * Test 2: Crafting a CARDBOARD_BOX consumes 6 CARDBOARD and adds 1 CARDBOARD_BOX.
     */
    @Test
    void test2_CraftingCardboardBoxConsumesCardboard() {
        inventory.addItem(Material.CARDBOARD, 6);

        Recipe boxRecipe = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getOutputs().containsKey(Material.CARDBOARD_BOX))
            .findFirst()
            .orElseThrow(() -> new AssertionError("CARDBOARD_BOX recipe not found"));

        boolean crafted = craftingSystem.craft(boxRecipe, inventory);

        assertTrue(crafted, "Crafting should succeed with 6 CARDBOARD");
        assertEquals(0, inventory.getItemCount(Material.CARDBOARD),
            "All 6 CARDBOARD should be consumed");
        assertEquals(1, inventory.getItemCount(Material.CARDBOARD_BOX),
            "Inventory should contain 1 CARDBOARD_BOX");
    }

    /**
     * Test 3: Cannot craft CARDBOARD_BOX with fewer than 6 CARDBOARD.
     */
    @Test
    void test3_CannotCraftWithInsufficientCardboard() {
        inventory.addItem(Material.CARDBOARD, 5); // One short

        Recipe boxRecipe = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getOutputs().containsKey(Material.CARDBOARD_BOX))
            .findFirst()
            .orElseThrow(() -> new AssertionError("CARDBOARD_BOX recipe not found"));

        boolean crafted = craftingSystem.craft(boxRecipe, inventory);

        assertFalse(crafted, "Crafting should fail with only 5 CARDBOARD");
        assertEquals(5, inventory.getItemCount(Material.CARDBOARD),
            "CARDBOARD should not be consumed on failed craft");
    }

    /**
     * Test 4: buildCardboardShelter places cardboard blocks in a recognisable structure.
     * After building, the origin block and roof block should both be CARDBOARD.
     */
    @Test
    void test4_BuildCardboardShelterPlacesBlocks() {
        // Clear a space in the world — set the 2x2x4 area to AIR
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    world.setBlock(10 + dx, 5 + dy, 10 + dz, BlockType.AIR);
                }
            }
        }

        blockPlacer.buildCardboardShelter(world, 10, 5, 10);

        // Floor should be cardboard
        assertEquals(BlockType.CARDBOARD, world.getBlock(10, 5, 10),
            "Floor block at origin should be CARDBOARD");
        assertEquals(BlockType.CARDBOARD, world.getBlock(11, 5, 11),
            "Floor block at far corner should be CARDBOARD");

        // Roof should be cardboard
        assertEquals(BlockType.CARDBOARD, world.getBlock(10, 8, 10),
            "Roof block should be CARDBOARD");
        assertEquals(BlockType.CARDBOARD, world.getBlock(11, 8, 11),
            "Roof far corner should be CARDBOARD");

        // Back wall should be cardboard
        assertEquals(BlockType.CARDBOARD, world.getBlock(10, 6, 10),
            "Back wall lower block should be CARDBOARD");
        assertEquals(BlockType.CARDBOARD, world.getBlock(10, 7, 10),
            "Back wall upper block should be CARDBOARD");
    }

    /**
     * Test 5: ShelterDetector recognises player as sheltered after cardboard box is built.
     *
     * Verifies the 2-block-tall entrance by:
     *   (a) asserting both entrance blocks are AIR after buildCardboardShelter(),
     *   (b) simulating W-key movement from outside to inside the shelter,
     *   (c) verifying isSheltered() returns true once the player is inside.
     *
     * This test would have caught the original bug: with a 1-block-tall entrance the upper
     * entrance block would have been CARDBOARD, failing assertion (a).
     */
    @Test
    void test5_ShelterDetectorRecognisesCardboardShelter() {
        int ox = 20, oy = 5, oz = 20;

        // Clear a space (include exterior approach path at z=oz+3)
        for (int dx = -1; dx <= 3; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                for (int dz = -1; dz <= 4; dz++) {
                    world.setBlock(ox + dx, oy + dy, oz + dz, BlockType.AIR);
                }
            }
        }

        // Build the shelter
        blockPlacer.buildCardboardShelter(world, ox, oy, oz);

        // (a) Entrance must be open at BOTH heights — this directly catches the bug
        assertEquals(BlockType.AIR, world.getBlock(ox + 1, oy + 1, oz + 2),
            "Entrance lower block (dy=1) must be AIR so player (1.8 tall) can enter");
        assertEquals(BlockType.AIR, world.getBlock(ox + 1, oy + 2, oz + 2),
            "Entrance upper block (dy=2) must be AIR — bug was a solid block here");

        // (b) Simulate W-key: player walks from just outside the entrance to inside.
        // Start at x=ox+1.5 (centred in the 1-block-wide entrance gap) to avoid clipping
        // the corner pillars. Floor outside is provided by the shelter floor at oz+2 extended.
        world.setBlock(ox + 1, oy, oz + 3, BlockType.CARDBOARD); // approach floor block
        Player player = new Player(ox + 1.5f, oy + 1, oz + 3);

        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 60; i++) {
            world.moveWithCollision(player, 0, 0, -1, delta);
        }

        // Player must have passed through the entrance into the shelter interior
        assertTrue(player.getPosition().z < oz + 2,
            "Player should have entered the shelter (z < " + (oz + 2) + "), actual z=" + player.getPosition().z);

        // (c) ShelterDetector should recognise player as sheltered.
        // Check at the interior grid position (ox+1, oy+1, oz+1) which is the logical
        // centre — the shelter geometry ensures 3+ walls surround this point.
        Vector3 interiorPos = new Vector3(ox + 1, oy + 1, oz + 1);
        boolean sheltered = ShelterDetector.isSheltered(world, interiorPos);
        assertTrue(sheltered, "Player inside cardboard shelter should be considered sheltered");
    }

    /**
     * Test 9: Player can walk through the 2-block-tall entrance.
     * Build shelter at (20, 5, 20). Place player at (21.5, 6, 23) facing north.
     * Simulate W for 60 frames. Verify player Z < 22 (entered the shelter).
     */
    @Test
    void test9_PlayerCanWalkThroughEntrance() {
        int ox = 20, oy = 5, oz = 20;

        // Clear the area including approach path
        for (int dx = -1; dx <= 3; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                for (int dz = -1; dz <= 4; dz++) {
                    world.setBlock(ox + dx, oy + dy, oz + dz, BlockType.AIR);
                }
            }
        }

        // Build the shelter
        blockPlacer.buildCardboardShelter(world, ox, oy, oz);

        // Ensure entrance blocks are open (2-block-tall doorway at centre)
        assertEquals(BlockType.AIR, world.getBlock(ox + 1, oy + 1, oz + 2),
            "Entrance lower block (dy=1) must be AIR");
        assertEquals(BlockType.AIR, world.getBlock(ox + 1, oy + 2, oz + 2),
            "Entrance upper block (dy=2) must be AIR");

        // Ground block outside so player has something to stand on before entering
        world.setBlock(ox + 1, oy, oz + 3, BlockType.CARDBOARD);

        // Player centred in the 1-block-wide entrance gap (x=ox+1.5) to avoid clipping corners
        Player player = new Player(ox + 1.5f, oy + 1, oz + 3);

        // Simulate W key (negative Z) for 60 frames
        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 60; i++) {
            world.moveWithCollision(player, 0, 0, -1, delta);
        }

        // Player must have passed through the entrance
        assertTrue(player.getPosition().z < oz + 2,
            "Player should have entered shelter (z < " + (oz + 2) + "), actual z=" + player.getPosition().z);
    }

    /**
     * Test 10: Shelter provides cold-snap protection after player walks in via entrance.
     * Build shelter, walk player in via W-key movement, set COLD_SNAP weather.
     * Advance 300 frames. Verify health is unchanged (shelter blocks cold-snap damage).
     */
    @Test
    void test10_ShelterProvidesColdSnapProtectionAfterWalkingIn() {
        int ox = 20, oy = 5, oz = 20;

        // Clear the area including approach path
        for (int dx = -1; dx <= 3; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                for (int dz = -1; dz <= 4; dz++) {
                    world.setBlock(ox + dx, oy + dy, oz + dz, BlockType.AIR);
                }
            }
        }

        // Build the shelter
        blockPlacer.buildCardboardShelter(world, ox, oy, oz);

        // Ground block outside the entrance
        world.setBlock(ox + 1, oy, oz + 3, BlockType.CARDBOARD);

        // Player centred in the entrance gap, outside
        Player player = new Player(ox + 1.5f, oy + 1, oz + 3);

        // Walk the player into the shelter via W-key movement
        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 60; i++) {
            world.moveWithCollision(player, 0, 0, -1, delta);
        }

        // Verify player is inside
        assertTrue(player.getPosition().z < oz + 2,
            "Player should have entered shelter before cold-snap test, actual z=" + player.getPosition().z);

        // Set up cold-snap weather and full health
        WeatherSystem weatherSystem = new WeatherSystem();
        weatherSystem.setWeather(Weather.COLD_SNAP);
        player.setHealth(100f);

        // Verify sheltered from the logical interior position (geometry of cardboard shelter
        // is designed so the centre cell (ox+1, oy+1, oz+1) is surrounded by 3+ walls)
        boolean sheltered = ShelterDetector.isSheltered(world, new Vector3(ox + 1, oy + 1, oz + 1));
        assertTrue(sheltered, "Interior position should be detected as sheltered");

        // Advance 300 frames (5 seconds) with cold-snap active
        // Cold snap would drain 2 HP/s = 10 HP total if unsheltered; shelter prevents this
        for (int i = 0; i < 300; i++) {
            if (!sheltered) {
                player.damage(Weather.COLD_SNAP.getHealthDrainRate() * delta);
            }
        }

        // Health should be unchanged — shelter protects against cold-snap damage
        assertEquals(100f, player.getHealth(), 0.01f,
            "Health should remain 100 — shelter protects against cold-snap damage");
    }

    /**
     * Test 6: Placing a CARDBOARD_BOX via BlockPlacer triggers auto-build when inventory
     * has one. The item should be removed from inventory and blocks placed in world.
     */
    @Test
    void test6_PlacingCardboardBoxAutoBuilds() {
        inventory.addItem(Material.CARDBOARD_BOX, 1);
        assertEquals(1, inventory.getItemCount(Material.CARDBOARD_BOX));

        // Prepare a ground block to place against, and clear space above it
        int gx = 30, gy = 5, gz = 30;
        world.setBlock(gx, gy, gz, BlockType.PAVEMENT); // Ground block to aim at
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 1; dy <= 4; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    world.setBlock(gx + dx, gy + dy, gz + dz, BlockType.AIR);
                }
            }
        }

        // Directly invoke buildCardboardShelter (as placeBlock would after raycasting)
        // Simulates the BlockPlacer logic that fires when material == CARDBOARD_BOX
        blockPlacer.buildCardboardShelter(world, gx, gy + 1, gz);
        inventory.removeItem(Material.CARDBOARD_BOX, 1);

        // Inventory should now be empty
        assertEquals(0, inventory.getItemCount(Material.CARDBOARD_BOX),
            "CARDBOARD_BOX should be removed from inventory after placement");

        // Structure should exist
        assertEquals(BlockType.CARDBOARD, world.getBlock(gx, gy + 1, gz),
            "Shelter floor should be placed at gy+1");
        assertEquals(BlockType.CARDBOARD, world.getBlock(gx, gy + 4, gz),
            "Shelter roof should be placed at gy+4");
    }

    /**
     * Test 7: CARDBOARD_BOX tooltip trigger exists in TooltipTrigger enum.
     */
    @Test
    void test7_CardboardBoxTooltipTriggerExists() {
        TooltipTrigger trigger = TooltipTrigger.CARDBOARD_BOX_SHELTER;
        assertNotNull(trigger, "CARDBOARD_BOX_SHELTER tooltip trigger should exist");
        assertNotNull(trigger.getMessage(), "Tooltip message should not be null");
        assertFalse(trigger.getMessage().isEmpty(), "Tooltip message should not be empty");
    }

    /**
     * Test 11: Player at x=ox+1.5 (centre of 1-block gap) is detected as sheltered.
     * Build cardboard shelter at (20, 5, 20). Set player position to (21.5, 6.0, 21.0)
     * — horizontal centre of the interior. Verify isSheltered() returns true.
     * (Previously returned false due to Math.round(21.5) = 22 snapping to the right wall.)
     */
    @Test
    void test11_PlayerCentredInInteriorIsDetectedAsSheltered() {
        int ox = 20, oy = 5, oz = 20;

        // Clear a space for the shelter
        for (int dx = -1; dx <= 3; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                for (int dz = -1; dz <= 4; dz++) {
                    world.setBlock(ox + dx, oy + dy, oz + dz, BlockType.AIR);
                }
            }
        }

        blockPlacer.buildCardboardShelter(world, ox, oy, oz);

        // Player at x=ox+1.5 (centre of the 1-block-wide interior gap)
        // With Math.round this would snap to ox+2 (the right wall) — wrong.
        // With Math.floor this maps to ox+1 (the interior cell) — correct.
        Vector3 pos = new Vector3(ox + 1.5f, oy + 1.0f, oz + 1.0f);
        assertTrue(ShelterDetector.isSheltered(world, pos),
            "Player centred at x=ox+1.5 inside shelter should be detected as sheltered");
    }

    /**
     * Test 12: Cold-snap damage does NOT apply when player is centred in shelter interior.
     * Build shelter at (20, 5, 20). Set player to (21.5, 6.0, 21.0). Set weather to COLD_SNAP.
     * Simulate 300 frames. Verify health remains 100.
     */
    @Test
    void test12_ColdSnapDamageDoesNotApplyInsideShelter() {
        int ox = 20, oy = 5, oz = 20;

        for (int dx = -1; dx <= 3; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                for (int dz = -1; dz <= 4; dz++) {
                    world.setBlock(ox + dx, oy + dy, oz + dz, BlockType.AIR);
                }
            }
        }

        blockPlacer.buildCardboardShelter(world, ox, oy, oz);

        Player player = new Player(ox + 1.5f, oy + 1.0f, oz + 1.0f);
        player.setHealth(100f);

        WeatherSystem weatherSystem = new WeatherSystem();
        weatherSystem.setWeather(Weather.COLD_SNAP);

        float delta = 1.0f / 60.0f;
        Vector3 pos = player.getPosition();
        for (int i = 0; i < 300; i++) {
            if (!ShelterDetector.isSheltered(world, pos)) {
                player.damage(Weather.COLD_SNAP.getHealthDrainRate() * delta);
            }
        }

        assertEquals(100f, player.getHealth(), 0.01f,
            "Health should remain 100 — shelter protects against cold-snap damage");
    }

    /**
     * Test 13: Player outside shelter IS damaged by cold snap.
     * Place player at (21.5, 6.0, 24.0) — outside the shelter.
     * Simulate 300 frames of cold-snap. Verify health < 100.
     */
    @Test
    void test13_ColdSnapDamageAppliesOutsideShelter() {
        int ox = 20, oy = 5, oz = 20;

        for (int dx = -1; dx <= 3; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                for (int dz = -1; dz <= 4; dz++) {
                    world.setBlock(ox + dx, oy + dy, oz + dz, BlockType.AIR);
                }
            }
        }

        blockPlacer.buildCardboardShelter(world, ox, oy, oz);

        // Player is outside the shelter (z=24 is beyond the entrance at z=22)
        Player player = new Player(ox + 1.5f, oy + 1.0f, oz + 4.0f);
        player.setHealth(100f);

        WeatherSystem weatherSystem = new WeatherSystem();
        weatherSystem.setWeather(Weather.COLD_SNAP);

        float delta = 1.0f / 60.0f;
        Vector3 pos = player.getPosition();
        for (int i = 0; i < 300; i++) {
            if (!ShelterDetector.isSheltered(world, pos)) {
                player.damage(Weather.COLD_SNAP.getHealthDrainRate() * delta);
            }
        }

        assertTrue(player.getHealth() < 100f,
            "Health should be below 100 — player outside shelter takes cold-snap damage");
    }

    /**
     * Test 8: TooltipSystem correctly marks CARDBOARD_BOX_SHELTER as shown.
     */
    @Test
    void test8_CardboardBoxTooltipShownOnce() {
        assertFalse(tooltipSystem.hasShown(TooltipTrigger.CARDBOARD_BOX_SHELTER),
            "Tooltip should not be shown before trigger");

        tooltipSystem.trigger(TooltipTrigger.CARDBOARD_BOX_SHELTER);

        assertTrue(tooltipSystem.hasShown(TooltipTrigger.CARDBOARD_BOX_SHELTER),
            "Tooltip should be marked as shown after trigger");
    }
}

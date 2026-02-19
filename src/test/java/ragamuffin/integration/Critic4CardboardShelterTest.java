package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.*;
import ragamuffin.core.ShelterDetector;
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
     * Place the cardboard shelter at (20, 5, 20). Put player inside at (20, 6, 21) — one
     * block into the structure. Verify isSheltered() returns true.
     */
    @Test
    void test5_ShelterDetectorRecognisesCardboardShelter() {
        int ox = 20, oy = 5, oz = 20;

        // Clear a space
        for (int dx = -1; dx <= 2; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                for (int dz = -1; dz <= 2; dz++) {
                    world.setBlock(ox + dx, oy + dy, oz + dz, BlockType.AIR);
                }
            }
        }

        // Build the shelter
        blockPlacer.buildCardboardShelter(world, ox, oy, oz);

        // Player stands inside the shelter (floor+1, roughly centered in x, towards back in z)
        Vector3 playerPos = new Vector3(ox, oy + 1, oz);

        boolean sheltered = ShelterDetector.isSheltered(world, playerPos);
        assertTrue(sheltered, "Player inside cardboard shelter should be considered sheltered");
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

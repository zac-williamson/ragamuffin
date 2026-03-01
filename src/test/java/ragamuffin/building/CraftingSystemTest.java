package ragamuffin.building;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CraftingSystem.
 */
class CraftingSystemTest {

    private CraftingSystem craftingSystem;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        craftingSystem = new CraftingSystem();
        inventory = new Inventory(36);
    }

    @Test
    void testGetAllRecipes() {
        List<Recipe> recipes = craftingSystem.getAllRecipes();
        assertNotNull(recipes);
        assertFalse(recipes.isEmpty());

        // Should have at least the basic wood->planks recipe
        boolean hasWoodToPlanks = recipes.stream()
            .anyMatch(r -> r.getInputs().containsKey(Material.WOOD) &&
                          r.getOutputs().containsKey(Material.PLANKS));
        assertTrue(hasWoodToPlanks, "Should have WOOD->PLANKS recipe");
    }

    @Test
    void testCanCraft_WithSufficientMaterials() {
        inventory.addItem(Material.WOOD, 4);

        Recipe woodToPlanks = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getInputs().containsKey(Material.WOOD) &&
                        r.getOutputs().containsKey(Material.PLANKS))
            .findFirst()
            .orElseThrow();

        assertTrue(craftingSystem.canCraft(woodToPlanks, inventory));
    }

    @Test
    void testCanCraft_WithInsufficientMaterials() {
        inventory.addItem(Material.WOOD, 3);

        Recipe woodToPlanks = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getInputs().containsKey(Material.WOOD) &&
                        r.getOutputs().containsKey(Material.PLANKS))
            .findFirst()
            .orElseThrow();

        assertFalse(craftingSystem.canCraft(woodToPlanks, inventory));
    }

    @Test
    void testCanCraft_WithNoMaterials() {
        Recipe woodToPlanks = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getInputs().containsKey(Material.WOOD) &&
                        r.getOutputs().containsKey(Material.PLANKS))
            .findFirst()
            .orElseThrow();

        assertFalse(craftingSystem.canCraft(woodToPlanks, inventory));
    }

    @Test
    void testCraft_Success() {
        inventory.addItem(Material.WOOD, 4);

        Recipe woodToPlanks = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getInputs().containsKey(Material.WOOD) &&
                        r.getOutputs().containsKey(Material.PLANKS))
            .findFirst()
            .orElseThrow();

        assertTrue(craftingSystem.craft(woodToPlanks, inventory));

        assertEquals(0, inventory.getItemCount(Material.WOOD));
        assertEquals(8, inventory.getItemCount(Material.PLANKS));
    }

    @Test
    void testCraft_Failure_InsufficientMaterials() {
        inventory.addItem(Material.WOOD, 3);

        Recipe woodToPlanks = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getInputs().containsKey(Material.WOOD) &&
                        r.getOutputs().containsKey(Material.PLANKS))
            .findFirst()
            .orElseThrow();

        assertFalse(craftingSystem.craft(woodToPlanks, inventory));

        assertEquals(3, inventory.getItemCount(Material.WOOD));
        assertEquals(0, inventory.getItemCount(Material.PLANKS));
    }

    @Test
    void testCraft_WithMultipleInputs() {
        inventory.addItem(Material.PLANKS, 6);

        Recipe planksToWall = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getOutputs().containsKey(Material.SHELTER_WALL))
            .findFirst()
            .orElseThrow();

        assertTrue(craftingSystem.craft(planksToWall, inventory));

        assertEquals(0, inventory.getItemCount(Material.PLANKS));
        assertEquals(1, inventory.getItemCount(Material.SHELTER_WALL));
    }

    // ── Issue #870: Tool economics tests ─────────────────────────────────────

    @Test
    void testStoneTool_ReducedCost_RequiresOnly3StoneAnd1Wood() {
        // STONE_TOOL recipe should now cost STONE×3 + WOOD×1 (reduced from STONE×4 + WOOD×2)
        Recipe stoneToolRecipe = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getOutputs().containsKey(Material.STONE_TOOL) &&
                        r.getInputs().containsKey(Material.STONE) &&
                        r.getInputs().containsKey(Material.WOOD) &&
                        r.getInputs().size() == 2)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected direct STONE+WOOD->STONE_TOOL recipe"));

        assertEquals(3, stoneToolRecipe.getInputs().get(Material.STONE),
            "Stone tool should require 3 stone (reduced cost)");
        assertEquals(1, stoneToolRecipe.getInputs().get(Material.WOOD),
            "Stone tool should require 1 wood (reduced cost)");
    }

    @Test
    void testStoneTool_CanCraftWithReducedCost() {
        inventory.addItem(Material.STONE, 3);
        inventory.addItem(Material.WOOD, 1);

        Recipe stoneToolRecipe = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getOutputs().containsKey(Material.STONE_TOOL) &&
                        r.getInputs().containsKey(Material.STONE) &&
                        r.getInputs().containsKey(Material.WOOD) &&
                        r.getInputs().size() == 2)
            .findFirst()
            .orElseThrow();

        assertTrue(craftingSystem.canCraft(stoneToolRecipe, inventory),
            "Should be able to craft stone tool with 3 stone and 1 wood");
        assertTrue(craftingSystem.craft(stoneToolRecipe, inventory));
        assertEquals(1, inventory.getItemCount(Material.STONE_TOOL));
    }

    @Test
    void testSkeletonKey_RecipeExists() {
        boolean hasSkeletonKey = craftingSystem.getAllRecipes().stream()
            .anyMatch(r -> r.getOutputs().containsKey(Material.SKELETON_KEY));
        assertTrue(hasSkeletonKey, "Should have SKELETON_KEY recipe");
    }

    @Test
    void testSkeletonKey_CanCraft() {
        inventory.addItem(Material.WIRE, 3);
        inventory.addItem(Material.BRICK, 1);

        Recipe skeletonKeyRecipe = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getOutputs().containsKey(Material.SKELETON_KEY))
            .findFirst()
            .orElseThrow();

        assertTrue(craftingSystem.canCraft(skeletonKeyRecipe, inventory));
        assertTrue(craftingSystem.craft(skeletonKeyRecipe, inventory));
        assertEquals(1, inventory.getItemCount(Material.SKELETON_KEY));
        assertEquals(0, inventory.getItemCount(Material.WIRE));
        assertEquals(0, inventory.getItemCount(Material.BRICK));
    }

    @Test
    void testBoltCutters_RecipeExists() {
        boolean hasBoltCutters = craftingSystem.getAllRecipes().stream()
            .anyMatch(r -> r.getOutputs().containsKey(Material.BOLT_CUTTERS));
        assertTrue(hasBoltCutters, "Should have BOLT_CUTTERS recipe");
    }

    @Test
    void testBoltCutters_CanCraft() {
        inventory.addItem(Material.SCRAP_METAL, 3);
        inventory.addItem(Material.IRON, 1);

        Recipe boltCuttersRecipe = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getOutputs().containsKey(Material.BOLT_CUTTERS))
            .findFirst()
            .orElseThrow();

        assertTrue(craftingSystem.canCraft(boltCuttersRecipe, inventory));
        assertTrue(craftingSystem.craft(boltCuttersRecipe, inventory));
        assertEquals(1, inventory.getItemCount(Material.BOLT_CUTTERS));
    }

    @Test
    void testMouthGuard_RecipeExists() {
        boolean hasMouthGuard = craftingSystem.getAllRecipes().stream()
            .anyMatch(r -> r.getOutputs().containsKey(Material.MOUTH_GUARD));
        assertTrue(hasMouthGuard, "Should have MOUTH_GUARD recipe");
    }

    @Test
    void testMouthGuard_CanCraft() {
        inventory.addItem(Material.RUBBER, 2);

        Recipe mouthGuardRecipe = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getOutputs().containsKey(Material.MOUTH_GUARD))
            .findFirst()
            .orElseThrow();

        assertTrue(craftingSystem.canCraft(mouthGuardRecipe, inventory));
        assertTrue(craftingSystem.craft(mouthGuardRecipe, inventory));
        assertEquals(1, inventory.getItemCount(Material.MOUTH_GUARD));
        assertEquals(0, inventory.getItemCount(Material.RUBBER));
    }

    // ── Issue #988: Further tools ─────────────────────────────────────────────

    @Test
    void testFlaskOfTea_RecipeExists() {
        boolean hasFlaskOfTea = craftingSystem.getAllRecipes().stream()
            .anyMatch(r -> r.getOutputs().containsKey(Material.FLASK_OF_TEA));
        assertTrue(hasFlaskOfTea, "Should have FLASK_OF_TEA recipe");
    }

    @Test
    void testFlaskOfTea_CanCraft() {
        inventory.addItem(Material.WOOD, 1);
        inventory.addItem(Material.COIN, 1);

        Recipe recipe = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getOutputs().containsKey(Material.FLASK_OF_TEA))
            .findFirst()
            .orElseThrow();

        assertTrue(craftingSystem.canCraft(recipe, inventory));
        assertTrue(craftingSystem.craft(recipe, inventory));
        assertEquals(1, inventory.getItemCount(Material.FLASK_OF_TEA));
        assertEquals(0, inventory.getItemCount(Material.WOOD));
        assertEquals(0, inventory.getItemCount(Material.COIN));
    }

    @Test
    void testBusPass_RecipeExists() {
        boolean hasBusPass = craftingSystem.getAllRecipes().stream()
            .anyMatch(r -> r.getOutputs().containsKey(Material.BUS_PASS));
        assertTrue(hasBusPass, "Should have BUS_PASS recipe");
    }

    @Test
    void testBusPass_CanCraft() {
        inventory.addItem(Material.COIN, 3);
        inventory.addItem(Material.NEWSPAPER, 1);

        Recipe recipe = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getOutputs().containsKey(Material.BUS_PASS))
            .findFirst()
            .orElseThrow();

        assertTrue(craftingSystem.canCraft(recipe, inventory));
        assertTrue(craftingSystem.craft(recipe, inventory));
        assertEquals(1, inventory.getItemCount(Material.BUS_PASS));
        assertEquals(0, inventory.getItemCount(Material.COIN));
        assertEquals(0, inventory.getItemCount(Material.NEWSPAPER));
    }

    @Test
    void testBusPass_CannotCraftWithInsufficientCoins() {
        inventory.addItem(Material.COIN, 2);
        inventory.addItem(Material.NEWSPAPER, 1);

        Recipe recipe = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getOutputs().containsKey(Material.BUS_PASS))
            .findFirst()
            .orElseThrow();

        assertFalse(craftingSystem.canCraft(recipe, inventory));
    }

    @Test
    void testSkateboard_RecipeExists() {
        boolean hasSkateboard = craftingSystem.getAllRecipes().stream()
            .anyMatch(r -> r.getOutputs().containsKey(Material.SKATEBOARD));
        assertTrue(hasSkateboard, "Should have SKATEBOARD recipe");
    }

    @Test
    void testSkateboard_CanCraft() {
        inventory.addItem(Material.WOOD, 2);
        inventory.addItem(Material.PLANKS, 1);

        Recipe recipe = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getOutputs().containsKey(Material.SKATEBOARD))
            .findFirst()
            .orElseThrow();

        assertTrue(craftingSystem.canCraft(recipe, inventory));
        assertTrue(craftingSystem.craft(recipe, inventory));
        assertEquals(1, inventory.getItemCount(Material.SKATEBOARD));
        assertEquals(0, inventory.getItemCount(Material.WOOD));
        assertEquals(0, inventory.getItemCount(Material.PLANKS));
    }

    @Test
    void testBreadCrust_RecipeExists() {
        boolean hasBreadCrust = craftingSystem.getAllRecipes().stream()
            .anyMatch(r -> r.getOutputs().containsKey(Material.BREAD_CRUST));
        assertTrue(hasBreadCrust, "Should have BREAD_CRUST recipe");
    }

    @Test
    void testBreadCrust_CanCraft() {
        inventory.addItem(Material.GREGGS_PASTRY, 1);

        Recipe recipe = craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getOutputs().containsKey(Material.BREAD_CRUST))
            .findFirst()
            .orElseThrow();

        assertTrue(craftingSystem.canCraft(recipe, inventory));
        assertTrue(craftingSystem.craft(recipe, inventory));
        assertEquals(1, inventory.getItemCount(Material.BREAD_CRUST));
        assertEquals(0, inventory.getItemCount(Material.GREGGS_PASTRY));
    }

    @Test
    void testGetAvailableRecipes() {
        inventory.addItem(Material.WOOD, 4);

        List<Recipe> available = craftingSystem.getAvailableRecipes(inventory);
        assertFalse(available.isEmpty());

        // Should include wood->planks
        boolean hasWoodToPlanks = available.stream()
            .anyMatch(r -> r.getInputs().containsKey(Material.WOOD) &&
                          r.getOutputs().containsKey(Material.PLANKS));
        assertTrue(hasWoodToPlanks);

        // Should NOT include recipes we can't afford
        boolean hasPlanksRecipes = available.stream()
            .anyMatch(r -> r.getInputs().containsKey(Material.PLANKS));
        assertFalse(hasPlanksRecipes);
    }
}

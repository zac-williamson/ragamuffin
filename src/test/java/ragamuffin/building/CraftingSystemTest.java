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

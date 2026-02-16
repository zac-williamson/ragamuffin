package ragamuffin.building;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Recipe class.
 */
class RecipeTest {

    @Test
    void testRecipeWithSingleInput() {
        Recipe recipe = new Recipe(
            Map.of(Material.WOOD, 4),
            Map.of(Material.PLANKS, 8)
        );

        Map<Material, Integer> inputs = recipe.getInputs();
        assertEquals(1, inputs.size());
        assertEquals(4, inputs.get(Material.WOOD));

        Map<Material, Integer> outputs = recipe.getOutputs();
        assertEquals(1, outputs.size());
        assertEquals(8, outputs.get(Material.PLANKS));
    }

    @Test
    void testRecipeWithMultipleInputs() {
        Recipe recipe = new Recipe(
            Map.of(Material.PLANKS, 6, Material.WOOD, 2),
            Map.of(Material.SHELTER_WALL, 1)
        );

        Map<Material, Integer> inputs = recipe.getInputs();
        assertEquals(2, inputs.size());
        assertEquals(6, inputs.get(Material.PLANKS));
        assertEquals(2, inputs.get(Material.WOOD));
    }

    @Test
    void testRecipeEquality() {
        Recipe recipe1 = new Recipe(
            Map.of(Material.WOOD, 4),
            Map.of(Material.PLANKS, 8)
        );

        Recipe recipe2 = new Recipe(
            Map.of(Material.WOOD, 4),
            Map.of(Material.PLANKS, 8)
        );

        assertEquals(recipe1, recipe2);
        assertEquals(recipe1.hashCode(), recipe2.hashCode());
    }
}

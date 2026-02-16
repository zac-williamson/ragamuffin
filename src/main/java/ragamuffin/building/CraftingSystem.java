package ragamuffin.building;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages crafting recipes and crafting logic.
 */
public class CraftingSystem {
    private final List<Recipe> recipes;

    public CraftingSystem() {
        this.recipes = new ArrayList<>();
        registerDefaultRecipes();
    }

    private void registerDefaultRecipes() {
        // Basic materials
        recipes.add(new Recipe(
            Map.of(Material.WOOD, 4),
            Map.of(Material.PLANKS, 8)
        ));

        // Shelter components
        recipes.add(new Recipe(
            Map.of(Material.PLANKS, 6),
            Map.of(Material.SHELTER_WALL, 1)
        ));

        recipes.add(new Recipe(
            Map.of(Material.PLANKS, 3),
            Map.of(Material.SHELTER_FLOOR, 1)
        ));

        recipes.add(new Recipe(
            Map.of(Material.PLANKS, 4),
            Map.of(Material.SHELTER_ROOF, 1)
        ));

        // Advanced structures
        recipes.add(new Recipe(
            Map.of(Material.BRICK, 8),
            Map.of(Material.BRICK_WALL, 1)
        ));

        recipes.add(new Recipe(
            Map.of(Material.GLASS, 4),
            Map.of(Material.WINDOW, 1)
        ));
    }

    /**
     * Get all registered recipes.
     */
    public List<Recipe> getAllRecipes() {
        return new ArrayList<>(recipes);
    }

    /**
     * Get recipes that the player can currently craft.
     */
    public List<Recipe> getAvailableRecipes(Inventory inventory) {
        List<Recipe> available = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (canCraft(recipe, inventory)) {
                available.add(recipe);
            }
        }
        return available;
    }

    /**
     * Check if a recipe can be crafted with the given inventory.
     */
    public boolean canCraft(Recipe recipe, Inventory inventory) {
        for (Map.Entry<Material, Integer> input : recipe.getInputs().entrySet()) {
            Material material = input.getKey();
            int required = input.getValue();

            if (inventory.getItemCount(material) < required) {
                return false;
            }
        }
        return true;
    }

    /**
     * Attempt to craft a recipe.
     * @return true if successful, false if insufficient materials
     */
    public boolean craft(Recipe recipe, Inventory inventory) {
        if (!canCraft(recipe, inventory)) {
            return false;
        }

        // Remove inputs
        for (Map.Entry<Material, Integer> input : recipe.getInputs().entrySet()) {
            Material material = input.getKey();
            int required = input.getValue();
            inventory.removeItem(material, required);
        }

        // Add outputs
        for (Map.Entry<Material, Integer> output : recipe.getOutputs().entrySet()) {
            Material material = output.getKey();
            int produced = output.getValue();
            inventory.addItem(material, produced);
        }

        return true;
    }
}

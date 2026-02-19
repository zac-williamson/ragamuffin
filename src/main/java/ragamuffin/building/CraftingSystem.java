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

        // Tools
        recipes.add(new Recipe(
            Map.of(Material.WOOD, 2, Material.STONE, 1),
            Map.of(Material.IMPROVISED_TOOL, 1)
        ));

        recipes.add(new Recipe(
            Map.of(Material.STONE, 4, Material.WOOD, 2),
            Map.of(Material.STONE_TOOL, 1)
        ));

        // Cardboard shelter: 4 cardboard → 6 shelter walls (enough for a basic shelter)
        recipes.add(new Recipe(
            Map.of(Material.CARDBOARD, 4),
            Map.of(Material.SHELTER_WALL, 3, Material.SHELTER_ROOF, 1)
        ));

        // Cardboard box: 6 cardboard → 1 cardboard box (place to auto-build 2x2x2 shelter)
        recipes.add(new Recipe(
            Map.of(Material.CARDBOARD, 6),
            Map.of(Material.CARDBOARD_BOX, 1)
        ));

        // Scrap metal recipes — loot from industrial estate / builders merchant
        recipes.add(new Recipe(
            Map.of(Material.SCRAP_METAL, 3, Material.PLYWOOD, 2),
            Map.of(Material.SHELTER_WALL, 2, Material.SHELTER_ROOF, 2)
        ));

        // Pipe + scrap metal → improvised tool (alternative recipe)
        recipes.add(new Recipe(
            Map.of(Material.PIPE, 1, Material.SCRAP_METAL, 1),
            Map.of(Material.IMPROVISED_TOOL, 1)
        ));

        // Plywood boards → planks (alternative to punching trees)
        recipes.add(new Recipe(
            Map.of(Material.PLYWOOD, 2),
            Map.of(Material.PLANKS, 6)
        ));

        // Broken phone + computer → stone tool (electronics scavenging)
        recipes.add(new Recipe(
            Map.of(Material.BROKEN_PHONE, 2, Material.COMPUTER, 1),
            Map.of(Material.STONE_TOOL, 1)
        ));

        // Newspaper → cardboard (recycle the press)
        recipes.add(new Recipe(
            Map.of(Material.NEWSPAPER, 4),
            Map.of(Material.CARDBOARD, 2)
        ));

        // Petrol can + wood → improvised tool (better quality)
        recipes.add(new Recipe(
            Map.of(Material.PETROL_CAN, 1, Material.WOOD, 3),
            Map.of(Material.STONE_TOOL, 1)
        ));

        // Dodgy DVDs + broken phone → diamond (fence the goods)
        recipes.add(new Recipe(
            Map.of(Material.DODGY_DVD, 2, Material.BROKEN_PHONE, 1),
            Map.of(Material.DIAMOND, 1)
        ));

        // Textbooks + newspaper → cardboard (academic recycling)
        recipes.add(new Recipe(
            Map.of(Material.TEXTBOOK, 2, Material.NEWSPAPER, 2),
            Map.of(Material.CARDBOARD, 4)
        ));

        // Hymn book + wood → shelter wall + door (spiritual construction)
        recipes.add(new Recipe(
            Map.of(Material.HYMN_BOOK, 1, Material.WOOD, 3),
            Map.of(Material.SHELTER_WALL, 1, Material.DOOR, 1)
        ));

        // Hair clippers + nail polish + scrap metal → stone tool (salon armoury)
        recipes.add(new Recipe(
            Map.of(Material.HAIR_CLIPPERS, 1, Material.NAIL_POLISH, 1, Material.SCRAP_METAL, 1),
            Map.of(Material.STONE_TOOL, 1)
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

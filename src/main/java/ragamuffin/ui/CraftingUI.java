package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.building.CraftingSystem;
import ragamuffin.building.Inventory;
import ragamuffin.building.Recipe;

import java.util.List;

/**
 * Crafting menu UI overlay.
 */
public class CraftingUI {
    private final CraftingSystem craftingSystem;
    private final Inventory inventory;
    private boolean visible;
    private int selectedRecipeIndex = -1;

    public CraftingUI(CraftingSystem craftingSystem, Inventory inventory) {
        this.craftingSystem = craftingSystem;
        this.inventory = inventory;
        this.visible = false;
    }

    public void toggle() {
        visible = !visible;
        if (!visible) {
            selectedRecipeIndex = -1;
        }
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
        selectedRecipeIndex = -1;
    }

    public boolean isVisible() {
        return visible;
    }

    public void selectRecipe(int index) {
        selectedRecipeIndex = index;
    }

    public int getSelectedRecipeIndex() {
        return selectedRecipeIndex;
    }

    /**
     * Attempt to craft the currently selected recipe.
     * @return true if crafting succeeded
     */
    public boolean craftSelected() {
        if (selectedRecipeIndex < 0) {
            return false;
        }

        List<Recipe> recipes = craftingSystem.getAllRecipes();
        if (selectedRecipeIndex >= recipes.size()) {
            return false;
        }

        Recipe recipe = recipes.get(selectedRecipeIndex);
        return craftingSystem.craft(recipe, inventory);
    }

    /**
     * Get all recipes for display purposes.
     */
    public List<Recipe> getDisplayedRecipes() {
        return craftingSystem.getAllRecipes();
    }

    /**
     * Render the crafting UI.
     */
    public void render(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font, int screenWidth, int screenHeight) {
        if (!visible) {
            return;
        }

        // Draw semi-transparent background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.7f);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();

        // Draw crafting panel
        int panelWidth = 600;
        int panelHeight = 500;
        int panelX = (screenWidth - panelWidth) / 2;
        int panelY = (screenHeight - panelHeight) / 2;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1.0f);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        // Draw text
        spriteBatch.begin();

        font.setColor(Color.WHITE);
        font.draw(spriteBatch, "Crafting Menu", panelX + 20, panelY + panelHeight - 20);
        font.draw(spriteBatch, "Press C to close", panelX + 20, panelY + panelHeight - 50);

        // Draw recipes
        List<Recipe> recipes = craftingSystem.getAllRecipes();
        int y = panelY + panelHeight - 100;

        for (int i = 0; i < recipes.size(); i++) {
            Recipe recipe = recipes.get(i);
            boolean canCraft = craftingSystem.canCraft(recipe, inventory);
            boolean isSelected = (i == selectedRecipeIndex);

            if (isSelected) {
                font.setColor(Color.YELLOW);
            } else if (canCraft) {
                font.setColor(Color.GREEN);
            } else {
                font.setColor(Color.GRAY);
            }

            String prefix = isSelected ? "> " : "  ";
            String recipeText = prefix + (i + 1) + ". " + recipe.getDisplayName();
            font.draw(spriteBatch, recipeText, panelX + 40, y);

            y -= 30;
        }

        // Draw instructions
        font.setColor(Color.WHITE);
        font.draw(spriteBatch, "Use number keys (1-9) to select recipe", panelX + 20, panelY + 60);
        font.draw(spriteBatch, "Press ENTER to craft selected recipe", panelX + 20, panelY + 30);

        spriteBatch.end();
    }
}

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
 * Crafting menu UI overlay with mouse click support and scrolling.
 */
public class CraftingUI {
    private static final int RECIPE_ROW_HEIGHT = 35;
    private static final int HEADER_HEIGHT = 80;
    private static final int FOOTER_HEIGHT = 90;

    private final CraftingSystem craftingSystem;
    private final Inventory inventory;
    private boolean visible;
    private int selectedRecipeIndex = -1;
    private int scrollOffset = 0;

    // Cached layout
    private int panelX, panelY, panelWidth, panelHeight;
    private int recipeStartY;
    private int visibleRecipeCount = 9; // default; updated each render

    public CraftingUI(CraftingSystem craftingSystem, Inventory inventory) {
        this.craftingSystem = craftingSystem;
        this.inventory = inventory;
        this.visible = false;
    }

    public void toggle() {
        visible = !visible;
        if (!visible) {
            selectedRecipeIndex = -1;
            scrollOffset = 0;
        }
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
        selectedRecipeIndex = -1;
        scrollOffset = 0;
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
     * Scroll the recipe list up (towards lower indices).
     */
    public void scrollUp() {
        if (scrollOffset > 0) scrollOffset--;
    }

    /**
     * Scroll the recipe list down (towards higher indices).
     */
    public void scrollDown() {
        int totalRecipes = craftingSystem.getAllRecipes().size();
        if (scrollOffset < totalRecipes - visibleRecipeCount) scrollOffset++;
    }

    /**
     * Handle mouse scroll. Returns true if consumed.
     */
    public boolean handleScroll(float amountY) {
        if (!visible) return false;
        if (amountY > 0) scrollDown();
        else scrollUp();
        return true;
    }

    /**
     * Handle mouse click. Returns true if consumed.
     */
    public boolean handleClick(int screenX, int screenY, int screenHeight) {
        if (!visible) return false;

        int uiY = screenHeight - screenY;
        List<Recipe> recipes = craftingSystem.getAllRecipes();

        // Check if click is within the panel
        if (screenX < panelX || screenX > panelX + panelWidth) return false;

        // Check recipe rows (only visible ones)
        int endIndex = Math.min(scrollOffset + visibleRecipeCount, recipes.size());
        for (int i = scrollOffset; i < endIndex; i++) {
            int visibleRow = i - scrollOffset;
            int rowY = recipeStartY - (visibleRow * RECIPE_ROW_HEIGHT);
            if (uiY >= rowY - RECIPE_ROW_HEIGHT && uiY <= rowY) {
                if (selectedRecipeIndex == i) {
                    // Double-click to craft
                    craftSelected();
                } else {
                    selectedRecipeIndex = i;
                }
                return true;
            }
        }

        // Check "Craft" button area (bottom of panel)
        int craftBtnY = panelY + 55;
        int craftBtnH = 30;
        if (uiY >= craftBtnY && uiY <= craftBtnY + craftBtnH &&
            screenX >= panelX + 20 && screenX <= panelX + 200) {
            craftSelected();
            return true;
        }

        return true; // Consume click if within panel
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
        render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight, null);
    }

    /**
     * Render the crafting UI and register hover tooltip zones.
     */
    public void render(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font, int screenWidth, int screenHeight, HoverTooltipSystem hoverTooltips) {
        if (!visible) {
            return;
        }

        // Draw semi-transparent background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.7f);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();

        // Draw crafting panel — sized to fit within the screen
        panelWidth = Math.min(600, screenWidth - 40);
        panelHeight = Math.min(500, screenHeight - 40);
        panelX = (screenWidth - panelWidth) / 2;
        panelY = (screenHeight - panelHeight) / 2;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1.0f);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        // Draw recipes with clickable rows and scroll support
        List<Recipe> recipes = craftingSystem.getAllRecipes();
        int recipeAreaHeight = panelHeight - HEADER_HEIGHT - FOOTER_HEIGHT;
        visibleRecipeCount = Math.max(1, recipeAreaHeight / RECIPE_ROW_HEIGHT);
        recipeStartY = panelY + panelHeight - HEADER_HEIGHT;

        // Clamp scroll offset so we never scroll past the end
        int maxScroll = Math.max(0, recipes.size() - visibleRecipeCount);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        // Highlight selected recipe row (if it's visible)
        if (selectedRecipeIndex >= scrollOffset && selectedRecipeIndex < scrollOffset + visibleRecipeCount
                && selectedRecipeIndex < recipes.size()) {
            int visibleRow = selectedRecipeIndex - scrollOffset;
            int rowY = recipeStartY - (visibleRow * RECIPE_ROW_HEIGHT);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.3f, 0.3f, 0.1f, 0.5f);
            shapeRenderer.rect(panelX + 20, rowY - RECIPE_ROW_HEIGHT + 5, panelWidth - 40, RECIPE_ROW_HEIGHT);
            shapeRenderer.end();
        }

        // Draw text
        spriteBatch.begin();

        font.setColor(Color.WHITE);
        font.draw(spriteBatch, "Crafting Menu (click recipe, then click Craft)", panelX + 20, panelY + panelHeight - 20);
        font.setColor(Color.GRAY);
        font.draw(spriteBatch, "Press C to close | Scroll to see more recipes", panelX + 20, panelY + panelHeight - 50);

        // Draw visible recipes
        int endIndex = Math.min(scrollOffset + visibleRecipeCount, recipes.size());
        int y = recipeStartY;
        for (int i = scrollOffset; i < endIndex; i++) {
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

            if (hoverTooltips != null) {
                String status = canCraft ? "Click to select, then Craft" : "Missing materials";
                hoverTooltips.addZone(panelX + 40, y - 20, panelWidth - 80, 25, recipe.getDisplayName() + " - " + status);
            }

            y -= RECIPE_ROW_HEIGHT;
        }

        // Draw scroll indicators if needed
        if (scrollOffset > 0) {
            font.setColor(Color.LIGHT_GRAY);
            font.draw(spriteBatch, "^ scroll up ^", panelX + panelWidth / 2 - 40, recipeStartY + 15);
        }
        if (scrollOffset < maxScroll) {
            font.setColor(Color.LIGHT_GRAY);
            font.draw(spriteBatch, "v scroll down v", panelX + panelWidth / 2 - 50, panelY + FOOTER_HEIGHT + 5);
        }

        // Draw Craft button
        boolean canCraftSelected = selectedRecipeIndex >= 0 && selectedRecipeIndex < recipes.size() &&
            craftingSystem.canCraft(recipes.get(selectedRecipeIndex), inventory);

        font.setColor(canCraftSelected ? Color.GREEN : Color.DARK_GRAY);
        font.draw(spriteBatch, "[CRAFT]", panelX + 40, panelY + 75);

        // Draw instructions
        font.setColor(Color.WHITE);
        font.draw(spriteBatch, "Click a recipe then click [CRAFT] or press ENTER", panelX + 20, panelY + 30);

        spriteBatch.end();
    }
}

package ragamuffin.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.CraftingSystem;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.building.Recipe;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CraftingUI.
 */
class CraftingUITest {

    private CraftingUI craftingUI;
    private CraftingSystem craftingSystem;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        craftingSystem = new CraftingSystem();
        inventory = new Inventory(36);
        craftingUI = new CraftingUI(craftingSystem, inventory);
    }

    @Test
    void testInitialState() {
        assertFalse(craftingUI.isVisible());
        assertEquals(-1, craftingUI.getSelectedRecipeIndex());
    }

    @Test
    void testToggle() {
        assertFalse(craftingUI.isVisible());
        craftingUI.toggle();
        assertTrue(craftingUI.isVisible());
        craftingUI.toggle();
        assertFalse(craftingUI.isVisible());
    }

    @Test
    void testShowHide() {
        craftingUI.show();
        assertTrue(craftingUI.isVisible());
        craftingUI.hide();
        assertFalse(craftingUI.isVisible());
    }

    @Test
    void testSelectRecipe() {
        craftingUI.show();
        craftingUI.selectRecipe(0);
        assertEquals(0, craftingUI.getSelectedRecipeIndex());

        craftingUI.selectRecipe(2);
        assertEquals(2, craftingUI.getSelectedRecipeIndex());
    }

    @Test
    void testCraftSelected_NoSelection() {
        craftingUI.show();
        inventory.addItem(Material.WOOD, 4);

        boolean result = craftingUI.craftSelected();
        assertFalse(result);
    }

    @Test
    void testCraftSelected_Success() {
        craftingUI.show();
        inventory.addItem(Material.WOOD, 4);

        // Select the wood->planks recipe (assuming it's first)
        craftingUI.selectRecipe(0);

        boolean result = craftingUI.craftSelected();
        assertTrue(result);

        assertEquals(0, inventory.getItemCount(Material.WOOD));
        assertEquals(8, inventory.getItemCount(Material.PLANKS));
    }

    @Test
    void testCraftSelected_InsufficientMaterials() {
        craftingUI.show();
        inventory.addItem(Material.WOOD, 3);

        craftingUI.selectRecipe(0);

        boolean result = craftingUI.craftSelected();
        assertFalse(result);

        assertEquals(3, inventory.getItemCount(Material.WOOD));
    }

    @Test
    void testGetDisplayedRecipes() {
        craftingUI.show();
        inventory.addItem(Material.WOOD, 4);

        // Should display all recipes, but mark availability
        assertTrue(craftingUI.getDisplayedRecipes().size() > 0);
    }
}

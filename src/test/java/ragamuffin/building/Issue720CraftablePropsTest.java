package ragamuffin.building;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #720: 3D objects (props) are craftable items.
 *
 * Verifies that each squat-furnishing prop and rave-equipment prop has a
 * corresponding Material entry, a crafting recipe in CraftingSystem, and
 * correct icon metadata.
 */
class Issue720CraftablePropsTest {

    private CraftingSystem craftingSystem;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        craftingSystem = new CraftingSystem();
        inventory = new Inventory(36);
    }

    // ── Recipe existence ───────────────────────────────────────────────────

    @Test
    void craftingSystem_hasBedRecipe() {
        assertTrue(hasRecipeFor(Material.PROP_BED),
            "CraftingSystem must have a recipe that produces PROP_BED");
    }

    @Test
    void craftingSystem_hasWorkbenchRecipe() {
        assertTrue(hasRecipeFor(Material.PROP_WORKBENCH),
            "CraftingSystem must have a recipe that produces PROP_WORKBENCH");
    }

    @Test
    void craftingSystem_hasDartboardRecipe() {
        assertTrue(hasRecipeFor(Material.PROP_DARTBOARD),
            "CraftingSystem must have a recipe that produces PROP_DARTBOARD");
    }

    @Test
    void craftingSystem_hasSpeakerStackRecipe() {
        assertTrue(hasRecipeFor(Material.PROP_SPEAKER_STACK),
            "CraftingSystem must have a recipe that produces PROP_SPEAKER_STACK");
    }

    @Test
    void craftingSystem_hasDiscoBallRecipe() {
        assertTrue(hasRecipeFor(Material.PROP_DISCO_BALL),
            "CraftingSystem must have a recipe that produces PROP_DISCO_BALL");
    }

    @Test
    void craftingSystem_hasDjDecksRecipe() {
        assertTrue(hasRecipeFor(Material.PROP_DJ_DECKS),
            "CraftingSystem must have a recipe that produces PROP_DJ_DECKS");
    }

    // ── Can actually craft ─────────────────────────────────────────────────

    @Test
    void canCraftBed_withWoodAndPlanks() {
        inventory.addItem(Material.WOOD, 4);
        inventory.addItem(Material.PLANKS, 2);

        Recipe recipe = getRecipeFor(Material.PROP_BED);
        assertNotNull(recipe);
        assertTrue(craftingSystem.canCraft(recipe, inventory));

        assertTrue(craftingSystem.craft(recipe, inventory));
        assertEquals(1, inventory.getItemCount(Material.PROP_BED));
        assertEquals(0, inventory.getItemCount(Material.WOOD));
        assertEquals(0, inventory.getItemCount(Material.PLANKS));
    }

    @Test
    void canCraftWorkbench_withPlanksAndScrapMetal() {
        inventory.addItem(Material.PLANKS, 6);
        inventory.addItem(Material.SCRAP_METAL, 2);

        Recipe recipe = getRecipeFor(Material.PROP_WORKBENCH);
        assertNotNull(recipe);
        assertTrue(craftingSystem.craft(recipe, inventory));
        assertEquals(1, inventory.getItemCount(Material.PROP_WORKBENCH));
    }

    @Test
    void canCraftDartboard_withWoodAndScrapMetal() {
        inventory.addItem(Material.WOOD, 3);
        inventory.addItem(Material.SCRAP_METAL, 1);

        Recipe recipe = getRecipeFor(Material.PROP_DARTBOARD);
        assertNotNull(recipe);
        assertTrue(craftingSystem.craft(recipe, inventory));
        assertEquals(1, inventory.getItemCount(Material.PROP_DARTBOARD));
    }

    @Test
    void canCraftSpeakerStack_withScrapMetalAndWood() {
        inventory.addItem(Material.SCRAP_METAL, 4);
        inventory.addItem(Material.WOOD, 2);

        Recipe recipe = getRecipeFor(Material.PROP_SPEAKER_STACK);
        assertNotNull(recipe);
        assertTrue(craftingSystem.craft(recipe, inventory));
        assertEquals(1, inventory.getItemCount(Material.PROP_SPEAKER_STACK));
    }

    @Test
    void canCraftDiscoBall_withGlassAndScrapMetal() {
        inventory.addItem(Material.GLASS, 2);
        inventory.addItem(Material.SCRAP_METAL, 1);

        Recipe recipe = getRecipeFor(Material.PROP_DISCO_BALL);
        assertNotNull(recipe);
        assertTrue(craftingSystem.craft(recipe, inventory));
        assertEquals(1, inventory.getItemCount(Material.PROP_DISCO_BALL));
    }

    @Test
    void canCraftDjDecks_withScrapMetalAndPipe() {
        inventory.addItem(Material.SCRAP_METAL, 3);
        inventory.addItem(Material.PIPE, 2);

        Recipe recipe = getRecipeFor(Material.PROP_DJ_DECKS);
        assertNotNull(recipe);
        assertTrue(craftingSystem.craft(recipe, inventory));
        assertEquals(1, inventory.getItemCount(Material.PROP_DJ_DECKS));
    }

    // ── Cannot craft without materials ────────────────────────────────────

    @Test
    void cannotCraftBed_withoutMaterials() {
        Recipe recipe = getRecipeFor(Material.PROP_BED);
        assertNotNull(recipe);
        assertFalse(craftingSystem.canCraft(recipe, inventory));
    }

    @Test
    void cannotCraftSpeakerStack_withInsufficientScrapMetal() {
        inventory.addItem(Material.SCRAP_METAL, 2); // needs 4
        inventory.addItem(Material.WOOD, 2);

        Recipe recipe = getRecipeFor(Material.PROP_SPEAKER_STACK);
        assertNotNull(recipe);
        assertFalse(craftingSystem.canCraft(recipe, inventory));
    }

    // ── Material icon metadata ────────────────────────────────────────────

    @Test
    void propMaterials_areNotBlockItems() {
        assertFalse(Material.PROP_BED.isBlockItem());
        assertFalse(Material.PROP_WORKBENCH.isBlockItem());
        assertFalse(Material.PROP_DARTBOARD.isBlockItem());
        assertFalse(Material.PROP_SPEAKER_STACK.isBlockItem());
        assertFalse(Material.PROP_DISCO_BALL.isBlockItem());
        assertFalse(Material.PROP_DJ_DECKS.isBlockItem());
    }

    @Test
    void propMaterials_haveIconColors() {
        for (Material m : new Material[]{
                Material.PROP_BED, Material.PROP_WORKBENCH, Material.PROP_DARTBOARD,
                Material.PROP_SPEAKER_STACK, Material.PROP_DISCO_BALL, Material.PROP_DJ_DECKS}) {
            assertNotNull(m.getIconColors(), "getIconColors() must not be null for " + m);
            assertTrue(m.getIconColors().length >= 1,
                "getIconColors() must return at least one color for " + m);
        }
    }

    @Test
    void propMaterials_haveIconShapes() {
        for (Material m : new Material[]{
                Material.PROP_BED, Material.PROP_WORKBENCH, Material.PROP_DARTBOARD,
                Material.PROP_SPEAKER_STACK, Material.PROP_DISCO_BALL, Material.PROP_DJ_DECKS}) {
            assertNotNull(m.getIconShape(), "getIconShape() must not be null for " + m);
        }
    }

    @Test
    void propMaterials_haveDisplayNames() {
        assertEquals("Bed", Material.PROP_BED.getDisplayName());
        assertEquals("Workbench", Material.PROP_WORKBENCH.getDisplayName());
        assertEquals("Dartboard", Material.PROP_DARTBOARD.getDisplayName());
        assertEquals("Speaker Stack", Material.PROP_SPEAKER_STACK.getDisplayName());
        assertEquals("Disco Ball", Material.PROP_DISCO_BALL.getDisplayName());
        assertEquals("DJ Decks", Material.PROP_DJ_DECKS.getDisplayName());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private boolean hasRecipeFor(Material output) {
        return craftingSystem.getAllRecipes().stream()
            .anyMatch(r -> r.getOutputs().containsKey(output));
    }

    private Recipe getRecipeFor(Material output) {
        return craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getOutputs().containsKey(output))
            .findFirst()
            .orElse(null);
    }
}

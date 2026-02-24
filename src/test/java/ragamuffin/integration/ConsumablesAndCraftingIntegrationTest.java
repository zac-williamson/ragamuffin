package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.*;
import ragamuffin.core.InteractionSystem;
import ragamuffin.entity.Player;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for consumable item fixes and new crafting recipes.
 * Verifies that PINT, PERI_PERI_CHICKEN, and PARACETAMOL are properly
 * consumable, and that new scavenging recipes work correctly.
 */
class ConsumablesAndCraftingIntegrationTest {

    private Player player;
    private Inventory inventory;
    private InteractionSystem interactionSystem;
    private CraftingSystem craftingSystem;

    @BeforeEach
    void setUp() {
        player = new Player(0, 1, 0);
        inventory = new Inventory(36);
        interactionSystem = new InteractionSystem();
        craftingSystem = new CraftingSystem();
    }

    // === Consumable item bug fixes ===

    @Test
    void testPintIsRecognisedAsFood() {
        assertTrue(interactionSystem.isFood(Material.PINT),
            "PINT should be recognised as a consumable item");
    }

    @Test
    void testPeriPeriChickenIsRecognisedAsFood() {
        assertTrue(interactionSystem.isFood(Material.PERI_PERI_CHICKEN),
            "PERI_PERI_CHICKEN should be recognised as a consumable item");
    }

    @Test
    void testParacetamolIsRecognisedAsFood() {
        assertTrue(interactionSystem.isFood(Material.PARACETAMOL),
            "PARACETAMOL should be recognised as a consumable item");
    }

    @Test
    void testConsumePintRestoresHungerAndEnergy() {
        player.setHunger(50);
        float energyBefore = player.getEnergy();
        inventory.addItem(Material.PINT, 1);

        boolean consumed = interactionSystem.consumeFood(Material.PINT, player, inventory);

        assertTrue(consumed, "PINT should be consumed successfully");
        assertEquals(65, player.getHunger(), 0.01, "PINT should restore 15 hunger");
        assertEquals(0, inventory.getItemCount(Material.PINT), "PINT should be removed from inventory");
    }

    @Test
    void testConsumePeriPeriChickenRestoresHunger() {
        player.setHunger(30);
        inventory.addItem(Material.PERI_PERI_CHICKEN, 1);

        boolean consumed = interactionSystem.consumeFood(Material.PERI_PERI_CHICKEN, player, inventory);

        assertTrue(consumed, "PERI_PERI_CHICKEN should be consumed successfully");
        assertEquals(75, player.getHunger(), 0.01, "PERI_PERI_CHICKEN should restore 45 hunger");
        assertEquals(0, inventory.getItemCount(Material.PERI_PERI_CHICKEN));
    }

    @Test
    void testConsumeParacetamolHealsDirectly() {
        player.setHealth(50);
        player.setHunger(80); // hunger should NOT change
        inventory.addItem(Material.PARACETAMOL, 1);

        boolean consumed = interactionSystem.consumeFood(Material.PARACETAMOL, player, inventory);

        assertTrue(consumed, "PARACETAMOL should be consumed successfully");
        assertEquals(75, player.getHealth(), 0.01, "PARACETAMOL should heal 25 HP directly");
        assertEquals(80, player.getHunger(), 0.01, "PARACETAMOL should not affect hunger");
        assertEquals(0, inventory.getItemCount(Material.PARACETAMOL));
    }

    @Test
    void testNonFoodItemsNotConsumable() {
        assertFalse(interactionSystem.isFood(Material.WOOD));
        assertFalse(interactionSystem.isFood(Material.DIAMOND));
        assertFalse(interactionSystem.isFood(Material.BROKEN_PHONE));
        assertFalse(interactionSystem.isFood(Material.NEWSPAPER));
    }

    @Test
    void testAntidepressantsIsRecognisedAsConsumable() {
        assertTrue(interactionSystem.isFood(Material.ANTIDEPRESSANTS),
            "ANTIDEPRESSANTS should be recognised as a consumable item");
    }

    @Test
    void testConsumeAntidepressantsRestoresEnergyAndShowsMessage() {
        // Set up player with low stats to verify effects
        player.setHealth(50);
        player.setHunger(50);
        player.setEnergy(50);
        float energyBefore = player.getEnergy();
        inventory.addItem(Material.ANTIDEPRESSANTS, 1);

        boolean consumed = interactionSystem.consumeFood(Material.ANTIDEPRESSANTS, player, inventory);

        assertTrue(consumed, "ANTIDEPRESSANTS should be consumed successfully");
        // Verify item was consumed
        assertEquals(0, inventory.getItemCount(Material.ANTIDEPRESSANTS),
            "ANTIDEPRESSANTS should be removed from inventory");
        // Verify no health or hunger effect
        assertEquals(50, player.getHealth(), 0.01,
            "ANTIDEPRESSANTS should not affect health");
        assertEquals(50, player.getHunger(), 0.01,
            "ANTIDEPRESSANTS should not affect hunger");
        // Verify energy is restored (meaningful gameplay effect)
        assertTrue(player.getEnergy() > energyBefore,
            "ANTIDEPRESSANTS should restore energy");
        // Verify a flavour message is set
        assertNotNull(interactionSystem.getLastConsumeMessage(),
            "ANTIDEPRESSANTS should produce a feedback message");
        assertFalse(interactionSystem.getLastConsumeMessage().isEmpty(),
            "ANTIDEPRESSANTS feedback message should not be empty");
    }

    // === New crafting recipes ===

    @Test
    void testScrapMetalAndPlywoodToShelter() {
        inventory.addItem(Material.SCRAP_METAL, 3);
        inventory.addItem(Material.PLYWOOD, 2);

        Recipe recipe = findRecipe(Material.SCRAP_METAL, Material.SHELTER_WALL);
        assertNotNull(recipe, "Scrap metal + plywood -> shelter recipe should exist");

        boolean crafted = craftingSystem.craft(recipe, inventory);
        assertTrue(crafted, "Should be able to craft shelter from scrap metal and plywood");
        assertEquals(0, inventory.getItemCount(Material.SCRAP_METAL));
        assertEquals(0, inventory.getItemCount(Material.PLYWOOD));
        assertEquals(2, inventory.getItemCount(Material.SHELTER_WALL));
        assertEquals(2, inventory.getItemCount(Material.SHELTER_ROOF));
    }

    @Test
    void testPipeAndScrapMetalToImprovisedTool() {
        inventory.addItem(Material.PIPE, 1);
        inventory.addItem(Material.SCRAP_METAL, 1);

        Recipe recipe = findRecipe(Material.PIPE, Material.IMPROVISED_TOOL);
        assertNotNull(recipe, "Pipe + scrap metal -> improvised tool recipe should exist");

        boolean crafted = craftingSystem.craft(recipe, inventory);
        assertTrue(crafted);
        assertEquals(1, inventory.getItemCount(Material.IMPROVISED_TOOL));
        assertEquals(0, inventory.getItemCount(Material.PIPE));
        assertEquals(0, inventory.getItemCount(Material.SCRAP_METAL));
    }

    @Test
    void testPlywoodToPlanks() {
        inventory.addItem(Material.PLYWOOD, 2);

        Recipe recipe = findRecipe(Material.PLYWOOD, Material.PLANKS);
        assertNotNull(recipe, "Plywood -> planks recipe should exist");

        boolean crafted = craftingSystem.craft(recipe, inventory);
        assertTrue(crafted);
        assertEquals(6, inventory.getItemCount(Material.PLANKS));
        assertEquals(0, inventory.getItemCount(Material.PLYWOOD));
    }

    @Test
    void testBrokenPhoneAndComputerToStoneTool() {
        inventory.addItem(Material.BROKEN_PHONE, 2);
        inventory.addItem(Material.COMPUTER, 1);

        Recipe recipe = findRecipe(Material.BROKEN_PHONE, Material.STONE_TOOL);
        assertNotNull(recipe, "Broken phone + computer -> stone tool recipe should exist");

        boolean crafted = craftingSystem.craft(recipe, inventory);
        assertTrue(crafted);
        assertEquals(1, inventory.getItemCount(Material.STONE_TOOL));
        assertEquals(0, inventory.getItemCount(Material.BROKEN_PHONE));
        assertEquals(0, inventory.getItemCount(Material.COMPUTER));
    }

    @Test
    void testNewspaperToCardboard() {
        inventory.addItem(Material.NEWSPAPER, 4);

        Recipe recipe = findRecipe(Material.NEWSPAPER, Material.CARDBOARD);
        assertNotNull(recipe, "Newspaper -> cardboard recipe should exist");

        boolean crafted = craftingSystem.craft(recipe, inventory);
        assertTrue(crafted);
        assertEquals(2, inventory.getItemCount(Material.CARDBOARD));
        assertEquals(0, inventory.getItemCount(Material.NEWSPAPER));
    }

    @Test
    void testPetrolCanAndWoodToStoneTool() {
        inventory.addItem(Material.PETROL_CAN, 1);
        inventory.addItem(Material.WOOD, 3);

        Recipe recipe = findRecipe(Material.PETROL_CAN, Material.STONE_TOOL);
        assertNotNull(recipe, "Petrol can + wood -> stone tool recipe should exist");

        boolean crafted = craftingSystem.craft(recipe, inventory);
        assertTrue(crafted);
        assertEquals(1, inventory.getItemCount(Material.STONE_TOOL));
        assertEquals(0, inventory.getItemCount(Material.PETROL_CAN));
        assertEquals(0, inventory.getItemCount(Material.WOOD));
    }

    @Test
    void testCannotCraftWithInsufficientScrapMetal() {
        inventory.addItem(Material.SCRAP_METAL, 1); // Need 3
        inventory.addItem(Material.PLYWOOD, 2);

        Recipe recipe = findRecipe(Material.SCRAP_METAL, Material.SHELTER_WALL);
        assertNotNull(recipe);
        assertFalse(craftingSystem.canCraft(recipe, inventory));
    }

    @Test
    void testFullScavengingPipeline() {
        // Simulate looting an industrial estate and builders merchant
        // then crafting useful items from the scrap
        inventory.addItem(Material.SCRAP_METAL, 4);
        inventory.addItem(Material.PLYWOOD, 4);
        inventory.addItem(Material.PIPE, 1);

        // First: craft plywood into planks
        Recipe plywoodToPlanks = findRecipe(Material.PLYWOOD, Material.PLANKS);
        craftingSystem.craft(plywoodToPlanks, inventory);
        assertEquals(6, inventory.getItemCount(Material.PLANKS));
        assertEquals(2, inventory.getItemCount(Material.PLYWOOD)); // 2 remaining

        // Then: craft scrap + remaining plywood into shelter
        Recipe scrapToShelter = findRecipe(Material.SCRAP_METAL, Material.SHELTER_WALL);
        craftingSystem.craft(scrapToShelter, inventory);
        assertEquals(2, inventory.getItemCount(Material.SHELTER_WALL));
        assertEquals(2, inventory.getItemCount(Material.SHELTER_ROOF));

        // Then: craft pipe + remaining scrap into improvised tool
        Recipe pipeToTool = findRecipe(Material.PIPE, Material.IMPROVISED_TOOL);
        craftingSystem.craft(pipeToTool, inventory);
        assertEquals(1, inventory.getItemCount(Material.IMPROVISED_TOOL));

        // Verify we used up the industrial loot productively
        assertEquals(0, inventory.getItemCount(Material.PIPE));
        assertEquals(0, inventory.getItemCount(Material.PLYWOOD));
    }

    /**
     * Find a recipe that uses the given input material and produces the given output material.
     */
    private Recipe findRecipe(Material input, Material output) {
        return craftingSystem.getAllRecipes().stream()
            .filter(r -> r.getInputs().containsKey(input) &&
                         r.getOutputs().containsKey(output))
            .findFirst()
            .orElse(null);
    }
}

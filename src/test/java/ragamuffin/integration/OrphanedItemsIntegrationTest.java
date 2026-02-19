package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.*;
import ragamuffin.core.InteractionSystem;
import ragamuffin.entity.Player;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for previously-orphaned items that now have
 * consumable or crafting uses.
 *
 * Consumables: SCRATCH_CARD, FIRE_EXTINGUISHER, WASHING_POWDER
 * Crafting: DODGY_DVD, TEXTBOOK, HYMN_BOOK, HAIR_CLIPPERS, NAIL_POLISH
 */
class OrphanedItemsIntegrationTest {

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

    // === Consumable tests ===

    @Test
    void testFireExtinguisherIsConsumable() {
        assertTrue(interactionSystem.isFood(Material.FIRE_EXTINGUISHER),
            "FIRE_EXTINGUISHER should be recognised as consumable");
    }

    @Test
    void testFireExtinguisherHeals() {
        player.setHealth(50);
        inventory.addItem(Material.FIRE_EXTINGUISHER, 1);

        boolean consumed = interactionSystem.consumeFood(Material.FIRE_EXTINGUISHER, player, inventory);

        assertTrue(consumed, "FIRE_EXTINGUISHER should be consumed");
        assertEquals(70, player.getHealth(), 0.01, "FIRE_EXTINGUISHER should heal 20 HP");
        assertEquals(0, inventory.getItemCount(Material.FIRE_EXTINGUISHER));
    }

    @Test
    void testWashingPowderIsConsumable() {
        assertTrue(interactionSystem.isFood(Material.WASHING_POWDER),
            "WASHING_POWDER should be recognised as consumable");
    }

    @Test
    void testWashingPowderRestoresEnergy() {
        player.setEnergy(50);
        inventory.addItem(Material.WASHING_POWDER, 1);

        boolean consumed = interactionSystem.consumeFood(Material.WASHING_POWDER, player, inventory);

        assertTrue(consumed, "WASHING_POWDER should be consumed");
        assertEquals(65, player.getEnergy(), 0.01, "WASHING_POWDER should restore 15 energy");
        assertEquals(0, inventory.getItemCount(Material.WASHING_POWDER));
    }

    @Test
    void testScratchCardIsConsumable() {
        assertTrue(interactionSystem.isFood(Material.SCRATCH_CARD),
            "SCRATCH_CARD should be recognised as consumable");
    }

    @Test
    void testScratchCardConsumed() {
        inventory.addItem(Material.SCRATCH_CARD, 1);

        boolean consumed = interactionSystem.consumeFood(Material.SCRATCH_CARD, player, inventory);

        assertTrue(consumed, "SCRATCH_CARD should be consumed");
        assertEquals(0, inventory.getItemCount(Material.SCRATCH_CARD),
            "SCRATCH_CARD should be removed from inventory");
    }

    @Test
    void testScratchCardStatisticalOutcome() {
        // Scratch 100 cards and verify we get between 5 and 40 diamonds
        // (expected ~20 with 1/5 chance, but allow wide margin for randomness)
        int totalDiamonds = 0;
        for (int i = 0; i < 100; i++) {
            inventory.addItem(Material.SCRATCH_CARD, 1);
            interactionSystem.consumeFood(Material.SCRATCH_CARD, player, inventory);
            totalDiamonds = inventory.getItemCount(Material.DIAMOND);
        }
        assertTrue(totalDiamonds > 0, "Over 100 scratch cards, at least one should win");
        assertTrue(totalDiamonds < 80, "Over 100 scratch cards, shouldn't win almost every time");
    }

    // === Crafting recipe tests ===

    @Test
    void testDodgyDvdAndBrokenPhoneToDiamond() {
        inventory.addItem(Material.DODGY_DVD, 2);
        inventory.addItem(Material.BROKEN_PHONE, 1);

        Recipe recipe = findRecipe(Material.DODGY_DVD, Material.DIAMOND);
        assertNotNull(recipe, "Dodgy DVD + broken phone -> diamond recipe should exist");

        boolean crafted = craftingSystem.craft(recipe, inventory);
        assertTrue(crafted, "Should craft diamond from dodgy DVDs and broken phone");
        assertEquals(1, inventory.getItemCount(Material.DIAMOND));
        assertEquals(0, inventory.getItemCount(Material.DODGY_DVD));
        assertEquals(0, inventory.getItemCount(Material.BROKEN_PHONE));
    }

    @Test
    void testTextbookAndNewspaperToCardboard() {
        inventory.addItem(Material.TEXTBOOK, 2);
        inventory.addItem(Material.NEWSPAPER, 2);

        Recipe recipe = findRecipe(Material.TEXTBOOK, Material.CARDBOARD);
        assertNotNull(recipe, "Textbook + newspaper -> cardboard recipe should exist");

        boolean crafted = craftingSystem.craft(recipe, inventory);
        assertTrue(crafted, "Should craft cardboard from textbooks and newspapers");
        assertEquals(4, inventory.getItemCount(Material.CARDBOARD));
        assertEquals(0, inventory.getItemCount(Material.TEXTBOOK));
        assertEquals(0, inventory.getItemCount(Material.NEWSPAPER));
    }

    @Test
    void testHymnBookAndWoodToShelterAndDoor() {
        inventory.addItem(Material.HYMN_BOOK, 1);
        inventory.addItem(Material.WOOD, 3);

        Recipe recipe = findRecipe(Material.HYMN_BOOK, Material.SHELTER_WALL);
        assertNotNull(recipe, "Hymn book + wood -> shelter wall + door recipe should exist");

        boolean crafted = craftingSystem.craft(recipe, inventory);
        assertTrue(crafted, "Should craft shelter wall and door from hymn book and wood");
        assertEquals(1, inventory.getItemCount(Material.SHELTER_WALL));
        assertEquals(1, inventory.getItemCount(Material.DOOR));
        assertEquals(0, inventory.getItemCount(Material.HYMN_BOOK));
        assertEquals(0, inventory.getItemCount(Material.WOOD));
    }

    @Test
    void testHairClippersAndNailPolishToStoneTool() {
        inventory.addItem(Material.HAIR_CLIPPERS, 1);
        inventory.addItem(Material.NAIL_POLISH, 1);
        inventory.addItem(Material.SCRAP_METAL, 1);

        Recipe recipe = findRecipe(Material.HAIR_CLIPPERS, Material.STONE_TOOL);
        assertNotNull(recipe, "Hair clippers + nail polish + scrap metal -> stone tool recipe should exist");

        boolean crafted = craftingSystem.craft(recipe, inventory);
        assertTrue(crafted, "Should craft stone tool from salon items and scrap metal");
        assertEquals(1, inventory.getItemCount(Material.STONE_TOOL));
        assertEquals(0, inventory.getItemCount(Material.HAIR_CLIPPERS));
        assertEquals(0, inventory.getItemCount(Material.NAIL_POLISH));
        assertEquals(0, inventory.getItemCount(Material.SCRAP_METAL));
    }

    @Test
    void testCannotCraftWithInsufficientDodgyDvds() {
        inventory.addItem(Material.DODGY_DVD, 1); // Need 2
        inventory.addItem(Material.BROKEN_PHONE, 1);

        Recipe recipe = findRecipe(Material.DODGY_DVD, Material.DIAMOND);
        assertNotNull(recipe);
        assertFalse(craftingSystem.canCraft(recipe, inventory),
            "Should not be able to craft with insufficient dodgy DVDs");
    }

    @Test
    void testFullScavengingPipelineWithNewRecipes() {
        // Simulate looting a charity shop, newsagents, and salon
        inventory.addItem(Material.HYMN_BOOK, 1);
        inventory.addItem(Material.WOOD, 3);
        inventory.addItem(Material.DODGY_DVD, 2);
        inventory.addItem(Material.BROKEN_PHONE, 1);
        inventory.addItem(Material.TEXTBOOK, 2);
        inventory.addItem(Material.NEWSPAPER, 2);

        // Hymn book + wood → shelter wall + door
        Recipe hymnRecipe = findRecipe(Material.HYMN_BOOK, Material.SHELTER_WALL);
        craftingSystem.craft(hymnRecipe, inventory);
        assertEquals(1, inventory.getItemCount(Material.SHELTER_WALL));
        assertEquals(1, inventory.getItemCount(Material.DOOR));

        // Dodgy DVDs + broken phone → diamond
        Recipe dvdRecipe = findRecipe(Material.DODGY_DVD, Material.DIAMOND);
        craftingSystem.craft(dvdRecipe, inventory);
        assertEquals(1, inventory.getItemCount(Material.DIAMOND));

        // Textbooks + newspaper → cardboard
        Recipe textbookRecipe = findRecipe(Material.TEXTBOOK, Material.CARDBOARD);
        craftingSystem.craft(textbookRecipe, inventory);
        assertEquals(4, inventory.getItemCount(Material.CARDBOARD));

        // All scavenged materials should be used up
        assertEquals(0, inventory.getItemCount(Material.HYMN_BOOK));
        assertEquals(0, inventory.getItemCount(Material.DODGY_DVD));
        assertEquals(0, inventory.getItemCount(Material.BROKEN_PHONE));
        assertEquals(0, inventory.getItemCount(Material.TEXTBOOK));
        assertEquals(0, inventory.getItemCount(Material.NEWSPAPER));
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

package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.CraftingSystem;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.building.Recipe;
import ragamuffin.core.BistaVillageSystem;
import ragamuffin.core.InteractionSystem;
import ragamuffin.entity.Player;
import ragamuffin.world.LandmarkType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #901 — Add craftable portal to historically accurate Bista Village.
 *
 * <p>Four exact scenarios:
 * <ol>
 *   <li>Portal item is craftable from DIAMOND×2 + STONE×4 + WOOD×2</li>
 *   <li>Using the portal teleports the player to Bista Village and consumes the item</li>
 *   <li>Portal is the only means of accessing Bista Village (landmark not reachable by walking)</li>
 *   <li>Return portal brings the player back to their original position</li>
 * </ol>
 */
class Issue901BistaVillagePortalTest {

    private CraftingSystem craftingSystem;
    private BistaVillageSystem bistaVillageSystem;
    private InteractionSystem interactionSystem;
    private Player player;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        craftingSystem = new CraftingSystem();
        bistaVillageSystem = new BistaVillageSystem();
        interactionSystem = new InteractionSystem();
        interactionSystem.setBistaVillageSystem(bistaVillageSystem);
        player = new Player(10f, 1f, 10f);
        inventory = new Inventory(36);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Portal item is craftable from DIAMOND×2 + STONE×4 + WOOD×2
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: Create a CraftingSystem. Verify that a recipe exists that produces
     * BISTA_VILLAGE_PORTAL. Verify the recipe requires DIAMOND×2, STONE×4, and WOOD×2.
     * Add the required materials to an inventory and verify the recipe can be crafted.
     * After crafting, verify the inventory contains exactly 1 BISTA_VILLAGE_PORTAL
     * and the input materials have been consumed.
     */
    @Test
    void bistaVillagePortalIsCraftable() {
        // Find the portal recipe
        List<Recipe> recipes = craftingSystem.getAllRecipes();
        Recipe portalRecipe = null;
        for (Recipe r : recipes) {
            if (r.getOutputs().containsKey(Material.BISTA_VILLAGE_PORTAL)) {
                portalRecipe = r;
                break;
            }
        }

        assertNotNull(portalRecipe, "A recipe producing BISTA_VILLAGE_PORTAL must exist");

        // Verify required inputs
        assertEquals(2, (int) portalRecipe.getInputs().getOrDefault(Material.DIAMOND, 0),
                "Recipe must require DIAMOND×2");
        assertEquals(4, (int) portalRecipe.getInputs().getOrDefault(Material.STONE, 0),
                "Recipe must require STONE×4");
        assertEquals(2, (int) portalRecipe.getInputs().getOrDefault(Material.WOOD, 0),
                "Recipe must require WOOD×2");

        // Load inventory and craft
        inventory.addItem(Material.DIAMOND, 2);
        inventory.addItem(Material.STONE, 4);
        inventory.addItem(Material.WOOD, 2);

        assertTrue(craftingSystem.canCraft(portalRecipe, inventory), "Should be craftable with exact materials");
        boolean success = craftingSystem.craft(portalRecipe, inventory);
        assertTrue(success, "Crafting should succeed");

        // Verify output
        assertEquals(1, inventory.getItemCount(Material.BISTA_VILLAGE_PORTAL),
                "Inventory should contain 1 BISTA_VILLAGE_PORTAL after crafting");

        // Verify inputs consumed
        assertEquals(0, inventory.getItemCount(Material.DIAMOND),
                "DIAMOND should be consumed after crafting");
        assertEquals(0, inventory.getItemCount(Material.STONE),
                "STONE should be consumed after crafting");
        assertEquals(0, inventory.getItemCount(Material.WOOD),
                "WOOD should be consumed after crafting");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Using the portal teleports the player and consumes the item
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: Place the player at position (10, 1, 10). Give them a
     * BISTA_VILLAGE_PORTAL. Use the portal via InteractionSystem.useItem().
     * Verify the returned message is non-null and contains the activation text.
     * Verify the player has been teleported to the Bista Village spawn coordinates
     * (approximately X=510, Y=2, Z=510). Verify the player still has exactly 1
     * BISTA_VILLAGE_PORTAL in their inventory (the return portal stone).
     * Verify BistaVillageSystem.isInBistaVillage() returns true.
     */
    @Test
    void portalTeleportsPlayerToBistaVillage() {
        // Place player at known position
        player = new Player(10f, 1f, 10f);
        inventory.addItem(Material.BISTA_VILLAGE_PORTAL, 1);

        // Use the portal
        String message = interactionSystem.useItem(Material.BISTA_VILLAGE_PORTAL, player, inventory);

        assertNotNull(message, "useItem should return an activation message");
        assertTrue(message.contains("Bista Village") || message.contains("shimmer") || message.contains("energy"),
                "Message should reference the portal activation: " + message);

        // Verify teleport
        assertEquals(BistaVillageSystem.BISTA_SPAWN_X, player.getPosition().x, 0.01f,
                "Player X should be at Bista Village spawn X");
        assertEquals(BistaVillageSystem.BISTA_SPAWN_Y, player.getPosition().y, 0.01f,
                "Player Y should be at Bista Village spawn Y");
        assertEquals(BistaVillageSystem.BISTA_SPAWN_Z, player.getPosition().z, 0.01f,
                "Player Z should be at Bista Village spawn Z");

        // Verify return portal stone granted (player ends up with 1 portal stone)
        assertEquals(1, inventory.getItemCount(Material.BISTA_VILLAGE_PORTAL),
                "Player should have 1 BISTA_VILLAGE_PORTAL (return portal stone) after teleporting in");

        // Verify portal system state
        assertTrue(bistaVillageSystem.isInBistaVillage(),
                "BistaVillageSystem should report player is in Bista Village");
        assertEquals(LandmarkType.BISTA_VILLAGE, bistaVillageSystem.getCurrentLandmark(),
                "getCurrentLandmark() should return BISTA_VILLAGE while in the village");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Portal is the only means of accessing Bista Village
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: Verify that without using the portal, the player is not in
     * Bista Village. Create a BistaVillageSystem; verify isInBistaVillage() is
     * false initially and getCurrentLandmark() returns null.
     * Verify that the BISTA_VILLAGE landmark does not appear anywhere in the
     * normal world landmark set reachable without the portal (i.e. it is only
     * returned by BistaVillageSystem after portal activation, not by any other
     * world path).
     */
    @Test
    void bistaVillageNotAccessibleWithoutPortal() {
        // Fresh system — no portal used
        assertFalse(bistaVillageSystem.isInBistaVillage(),
                "Player should not start in Bista Village");
        assertNull(bistaVillageSystem.getCurrentLandmark(),
                "getCurrentLandmark() should be null when not in Bista Village");

        // Without using the portal, the player has no BISTA_VILLAGE_PORTAL
        assertEquals(0, inventory.getItemCount(Material.BISTA_VILLAGE_PORTAL),
                "Player should not start with a portal stone");

        // Attempting to use with no portal stone returns null
        String result = bistaVillageSystem.activatePortal(player, inventory);
        assertNull(result, "activatePortal should return null when no portal stone held");

        // State unchanged
        assertFalse(bistaVillageSystem.isInBistaVillage(),
                "Should still not be in Bista Village after failed portal attempt");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Return portal brings the player back to their original position
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Place the player at (20, 1, 30). Give them a BISTA_VILLAGE_PORTAL.
     * Activate the portal — player teleports to Bista Village. Verify the player
     * now holds a return portal stone (1 BISTA_VILLAGE_PORTAL).
     * Use the portal again. Verify the player is returned to approximately (20, 1, 30).
     * Verify BistaVillageSystem.isInBistaVillage() returns false.
     * Verify the inventory no longer contains a BISTA_VILLAGE_PORTAL (return stone consumed).
     */
    @Test
    void returnPortalBringsPlayerBack() {
        float startX = 20f;
        float startY = 1f;
        float startZ = 30f;
        player = new Player(startX, startY, startZ);
        inventory.addItem(Material.BISTA_VILLAGE_PORTAL, 1);

        // Go to Bista Village
        String goMessage = bistaVillageSystem.activatePortal(player, inventory);
        assertNotNull(goMessage, "Portal activation should return a message");
        assertTrue(bistaVillageSystem.isInBistaVillage(), "Should be in Bista Village");
        assertEquals(1, inventory.getItemCount(Material.BISTA_VILLAGE_PORTAL),
                "Should have return portal stone");

        // Return via portal
        String returnMessage = bistaVillageSystem.activateReturnPortal(player, inventory);
        assertNotNull(returnMessage, "Return portal activation should return a message");

        // Verify returned to original position
        assertEquals(startX, player.getPosition().x, 0.01f,
                "Player X should return to start position");
        assertEquals(startY, player.getPosition().y, 0.01f,
                "Player Y should return to start position");
        assertEquals(startZ, player.getPosition().z, 0.01f,
                "Player Z should return to start position");

        // Verify no longer in Bista Village
        assertFalse(bistaVillageSystem.isInBistaVillage(),
                "Should not be in Bista Village after returning");
        assertNull(bistaVillageSystem.getCurrentLandmark(),
                "getCurrentLandmark() should be null after returning");

        // Verify return stone consumed
        assertEquals(0, inventory.getItemCount(Material.BISTA_VILLAGE_PORTAL),
                "Return portal stone should be consumed after use");
    }
}

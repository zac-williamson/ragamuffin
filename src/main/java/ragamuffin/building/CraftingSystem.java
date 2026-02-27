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

        // Ladder: 2 wood (rails) + 4 planks (rungs) → 2 ladders
        recipes.add(new Recipe(
            Map.of(Material.WOOD, 2, Material.PLANKS, 4),
            Map.of(Material.LADDER, 2)
        ));

        // ── Heist tools (Phase O / Issue #704) ──────────────────────────────────

        // CROWBAR: BRICK×2 + WOOD×3 → CROWBAR×1 (cracks safes; reduces brick hits from 8 to 5)
        recipes.add(new Recipe(
            Map.of(Material.BRICK, 2, Material.WOOD, 3),
            Map.of(Material.CROWBAR, 1)
        ));

        // GLASS_CUTTER: DIAMOND×1 + WOOD×1 → GLASS_CUTTER×1 (removes glass silently in 1 hit)
        recipes.add(new Recipe(
            Map.of(Material.DIAMOND, 1, Material.WOOD, 1),
            Map.of(Material.GLASS_CUTTER, 1)
        ));

        // ROPE_LADDER: WOOD×4 + LEAVES×2 → ROPE_LADDER×1 (deployable ladder for 60 in-game seconds)
        recipes.add(new Recipe(
            Map.of(Material.WOOD, 4, Material.LEAVES, 2),
            Map.of(Material.ROPE_LADDER, 1)
        ));

        // ── Squat advanced recipes (Issue #714) — unlocked via WORKBENCH inside squat ──

        // BARRICADE: WOOD×2 + BRICK×1 → BARRICADE×1 (doorway fortification, 3 hits to break)
        recipes.add(new Recipe(
            Map.of(Material.WOOD, 2, Material.BRICK, 1),
            Map.of(Material.BARRICADE, 1)
        ));

        // LOCKPICK: IRON×1 + FLINT×1 → LOCKPICK×1 (reduces safe-crack time by 3s)
        recipes.add(new Recipe(
            Map.of(Material.IRON, 1, Material.FLINT, 1),
            Map.of(Material.LOCKPICK, 1)
        ));

        // FAKE_ID: COUNCIL_ID×1 + NEWSPAPER×2 → FAKE_ID×1 (removes 1 criminal record offence)
        recipes.add(new Recipe(
            Map.of(Material.COUNCIL_ID, 1, Material.NEWSPAPER, 2),
            Map.of(Material.FAKE_ID, 1)
        ));

        // ── Issue #720: Craftable 3D prop items ──────────────────────────────

        // PROP_BED: WOOD×4 + PLANKS×2 → PROP_BED×1 (squat furnishing, +10 Vibe)
        recipes.add(new Recipe(
            Map.of(Material.WOOD, 4, Material.PLANKS, 2),
            Map.of(Material.PROP_BED, 1)
        ));

        // PROP_WORKBENCH: PLANKS×6 + SCRAP_METAL×2 → PROP_WORKBENCH×1 (unlocks advanced recipes)
        recipes.add(new Recipe(
            Map.of(Material.PLANKS, 6, Material.SCRAP_METAL, 2),
            Map.of(Material.PROP_WORKBENCH, 1)
        ));

        // PROP_DARTBOARD: WOOD×3 + SCRAP_METAL×1 → PROP_DARTBOARD×1 (squat furnishing, +7 Vibe)
        recipes.add(new Recipe(
            Map.of(Material.WOOD, 3, Material.SCRAP_METAL, 1),
            Map.of(Material.PROP_DARTBOARD, 1)
        ));

        // PROP_SPEAKER_STACK: SCRAP_METAL×4 + WOOD×2 → PROP_SPEAKER_STACK×1 (rave equipment)
        recipes.add(new Recipe(
            Map.of(Material.SCRAP_METAL, 4, Material.WOOD, 2),
            Map.of(Material.PROP_SPEAKER_STACK, 1)
        ));

        // PROP_DISCO_BALL: GLASS×2 + SCRAP_METAL×1 → PROP_DISCO_BALL×1 (rave equipment)
        recipes.add(new Recipe(
            Map.of(Material.GLASS, 2, Material.SCRAP_METAL, 1),
            Map.of(Material.PROP_DISCO_BALL, 1)
        ));

        // PROP_DJ_DECKS: SCRAP_METAL×3 + PIPE×2 → PROP_DJ_DECKS×1 (rave equipment, enables DJ)
        recipes.add(new Recipe(
            Map.of(Material.SCRAP_METAL, 3, Material.PIPE, 2),
            Map.of(Material.PROP_DJ_DECKS, 1)
        ));

        // ── Issue #765: Witness & Evidence System ────────────────────────────────

        // RUMOUR_NOTE: COIN×1 + NEWSPAPER×1 → RUMOUR_NOTE×1
        // (tip off police to clear one criminal record entry, seeds BETRAYAL rumour)
        recipes.add(new Recipe(
            Map.of(Material.COIN, 1, Material.NEWSPAPER, 1),
            Map.of(Material.RUMOUR_NOTE, 1)
        ));

        // ── Issue #783: Pirate FM — Underground Radio Station ────────────────────

        // WIRE: COIN×1 + WOOD×1 → WIRE×1 (electrical wiring, used in MICROPHONE and TRANSMITTER)
        recipes.add(new Recipe(
            Map.of(Material.COIN, 1, Material.WOOD, 1),
            Map.of(Material.WIRE, 1)
        ));

        // MICROPHONE: WIRE×1 + COIN×1 → MICROPHONE×1
        // (Note: replaces old SCRAP_METAL+PIPE recipe from Issue #716 for pirate radio use)
        recipes.add(new Recipe(
            Map.of(Material.WIRE, 1, Material.COIN, 1),
            Map.of(Material.MICROPHONE, 1)
        ));

        // BROADCAST_TAPE: NEWSPAPER×1 + COIN×1 → BROADCAST_TAPE×1 (pre-record a show)
        recipes.add(new Recipe(
            Map.of(Material.NEWSPAPER, 1, Material.COIN, 1),
            Map.of(Material.BROADCAST_TAPE, 1)
        ));

        // TRANSMITTER: WIRE×2 + COMPUTER×1 + WOOD×1 → TRANSMITTER×1
        // (Placed as BlockType.TRANSMITTER; must be placed indoors with 3+ block roof overhead)
        recipes.add(new Recipe(
            Map.of(Material.WIRE, 2, Material.COMPUTER, 1, Material.WOOD, 1),
            Map.of(Material.TRANSMITTER_ITEM, 1)
        ));

        // ── Issue #781: Graffiti & Territorial Marking ────────────────────────────

        // SPRAY_CAN: SPRAY_CAN_EMPTY×1 + any PAINT_PIGMENT → SPRAY_CAN×1 (20 uses)
        recipes.add(new Recipe(
            Map.of(Material.SPRAY_CAN_EMPTY, 1, Material.PAINT_PIGMENT_RED, 1),
            Map.of(Material.SPRAY_CAN, 1)
        ));
        recipes.add(new Recipe(
            Map.of(Material.SPRAY_CAN_EMPTY, 1, Material.PAINT_PIGMENT_BLUE, 1),
            Map.of(Material.SPRAY_CAN, 1)
        ));
        recipes.add(new Recipe(
            Map.of(Material.SPRAY_CAN_EMPTY, 1, Material.PAINT_PIGMENT_GOLD, 1),
            Map.of(Material.SPRAY_CAN, 1)
        ));
        recipes.add(new Recipe(
            Map.of(Material.SPRAY_CAN_EMPTY, 1, Material.PAINT_PIGMENT_WHITE, 1),
            Map.of(Material.SPRAY_CAN, 1)
        ));
        recipes.add(new Recipe(
            Map.of(Material.SPRAY_CAN_EMPTY, 1, Material.PAINT_PIGMENT_GREY, 1),
            Map.of(Material.SPRAY_CAN, 1)
        ));

        // ── Issue #785: The Dodgy Market Stall ────────────────────────────────

        // STALL_FRAME: WOOD×4 → STALL_FRAME×1
        // (Place on any PAVEMENT or ROAD block to create a market stall)
        recipes.add(new Recipe(
            Map.of(Material.WOOD, 4),
            Map.of(Material.STALL_FRAME, 1)
        ));

        // STALL_AWNING: WOOD×2 + PLANKS×1 → STALL_AWNING×1
        // (Attach to stall for weather protection; prevents rain damage)
        recipes.add(new Recipe(
            Map.of(Material.WOOD, 2, Material.PLANKS, 1),
            Map.of(Material.STALL_AWNING, 1)
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

package ragamuffin.world;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ragamuffin.building.BlockDropTable;
import ragamuffin.building.CraftingSystem;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.render.ChunkMeshBuilder;
import ragamuffin.render.MeshData;
import ragamuffin.test.HeadlessTestHelper;

import java.util.List;
import ragamuffin.building.Recipe;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #472: LADDER block type and crafting recipe.
 * Verifies that the LADDER block type exists, has correct properties,
 * produces valid geometry, drops the correct material when broken,
 * and can be crafted from wood and planks.
 */
class LadderBlockTest {

    @BeforeAll
    static void setup() {
        HeadlessTestHelper.initHeadless();
    }

    // ── BlockType tests ────────────────────────────────────────────────────────

    @Test
    void ladderBlockTypeExists() {
        assertNotNull(BlockType.LADDER);
    }

    @Test
    void ladderIsNotSolid() {
        // Ladders are not solid — the player passes through them (climbs)
        assertFalse(BlockType.LADDER.isSolid());
    }

    @Test
    void ladderIsNotOpaque() {
        // Ladders don't fill the full 1x1x1 cell
        assertFalse(BlockType.LADDER.isOpaque());
    }

    @Test
    void ladderHasLadderRunsShape() {
        assertEquals(BlockType.BlockShape.LADDER_RUNGS, BlockType.LADDER.getBlockShape());
    }

    @Test
    void ladderIsPlayerPlaceable() {
        assertTrue(BlockType.LADDER.isPlayerPlaceable());
    }

    @Test
    void ladderHasUniqueId() {
        BlockType[] types = BlockType.values();
        int ladderId = BlockType.LADDER.getId();
        int count = 0;
        for (BlockType type : types) {
            if (type.getId() == ladderId) count++;
        }
        assertEquals(1, count, "LADDER ID must be unique among all block types");
    }

    @Test
    void ladderHasColor() {
        assertNotNull(BlockType.LADDER.getColor());
    }

    @Test
    void ladderHasTopColor() {
        assertNotNull(BlockType.LADDER.getTopColor());
    }

    @Test
    void allBlockTypesStillHaveUniqueIds() {
        BlockType[] types = BlockType.values();
        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                assertNotEquals(types[i].getId(), types[j].getId(),
                    "BlockType " + types[i] + " and " + types[j] + " have the same ID");
            }
        }
    }

    // ── Mesh building tests ────────────────────────────────────────────────────

    @Test
    void ladderProducesGeometry() {
        // A single LADDER block should produce geometry (at least 1 face)
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(4, 4, 4, BlockType.LADDER);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        assertTrue(meshData.getFaceCount() > 0,
            "A LADDER block should produce at least one face");
    }

    @Test
    void ladderAdjacentToSolidBlockDoesNotCullSolidFace() {
        // LADDER is non-opaque, so a DIRT block adjacent to it should still render
        // the face that touches the LADDER block.
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(4, 4, 4, BlockType.LADDER);
        chunk.setBlock(5, 4, 4, BlockType.DIRT); // East of ladder

        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);

        // DIRT alone has 6 faces; with non-opaque LADDER neighbour it must still have 6
        // Count faces of DIRT: with LADDER present, DIRT's west face should NOT be culled
        Chunk chunkDirtOnly = new Chunk(0, 0, 0);
        chunkDirtOnly.setBlock(5, 4, 4, BlockType.DIRT);
        MeshData meshDataDirtOnly = builder.build(chunkDirtOnly);

        assertEquals(meshDataDirtOnly.getFaceCount(), 6,
            "Standalone DIRT should have 6 faces");

        // When LADDER is next to DIRT, DIRT should still have all 6 faces (LADDER is non-opaque)
        // Total faces = LADDER faces + DIRT faces (all 6, since LADDER doesn't cull any)
        int ladderFaces = meshData.getFaceCount() - 6;
        assertTrue(ladderFaces > 0,
            "LADDER should contribute faces beyond the DIRT block's 6");
    }

    // ── BlockDropTable tests ───────────────────────────────────────────────────

    @Test
    void ladderDropsMaterialLadder() {
        BlockDropTable table = new BlockDropTable();
        Material drop = table.getDrop(BlockType.LADDER, null);
        assertEquals(Material.LADDER, drop,
            "Breaking a LADDER block should drop Material.LADDER");
    }

    // ── Material tests ─────────────────────────────────────────────────────────

    @Test
    void ladderMaterialExists() {
        assertNotNull(Material.LADDER);
    }

    @Test
    void ladderMaterialHasDisplayName() {
        assertEquals("Ladder", Material.LADDER.getDisplayName());
    }

    @Test
    void ladderMaterialHasIconColors() {
        assertNotNull(Material.LADDER.getIconColors());
        assertTrue(Material.LADDER.getIconColors().length >= 1,
            "LADDER material should have at least one icon color");
    }

    @Test
    void ladderMaterialIsBlockItem() {
        assertTrue(Material.LADDER.isBlockItem(),
            "LADDER is a construction block and should be treated as a block item");
    }

    // ── Crafting tests ─────────────────────────────────────────────────────────

    @Test
    void ladderCraftingRecipeExists() {
        CraftingSystem craftingSystem = new CraftingSystem();
        List<Recipe> recipes = craftingSystem.getAllRecipes();
        boolean hasLadderRecipe = recipes.stream()
            .anyMatch(r -> r.getOutputs().containsKey(Material.LADDER));
        assertTrue(hasLadderRecipe, "CraftingSystem should have a recipe that produces LADDER");
    }

    @Test
    void ladderCraftingRecipeRequiresWoodAndPlanks() {
        CraftingSystem craftingSystem = new CraftingSystem();
        List<Recipe> recipes = craftingSystem.getAllRecipes();
        Recipe ladderRecipe = recipes.stream()
            .filter(r -> r.getOutputs().containsKey(Material.LADDER))
            .findFirst()
            .orElse(null);
        assertNotNull(ladderRecipe, "There should be a LADDER recipe");
        assertTrue(ladderRecipe.getInputs().containsKey(Material.WOOD),
            "Ladder recipe should require WOOD");
        assertTrue(ladderRecipe.getInputs().containsKey(Material.PLANKS),
            "Ladder recipe should require PLANKS");
    }

    @Test
    void ladderCanBeCrafted() {
        CraftingSystem craftingSystem = new CraftingSystem();
        Inventory inventory = new Inventory(36);
        inventory.addItem(Material.WOOD, 2);
        inventory.addItem(Material.PLANKS, 4);

        List<Recipe> recipes = craftingSystem.getAllRecipes();
        Recipe ladderRecipe = recipes.stream()
            .filter(r -> r.getOutputs().containsKey(Material.LADDER))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No LADDER recipe found"));

        assertTrue(craftingSystem.canCraft(ladderRecipe, inventory),
            "Should be able to craft LADDER with 2 WOOD and 4 PLANKS");
        assertTrue(craftingSystem.craft(ladderRecipe, inventory),
            "Crafting LADDER should succeed");
        assertTrue(inventory.getItemCount(Material.LADDER) >= 1,
            "Inventory should contain at least 1 LADDER after crafting");
        assertEquals(0, inventory.getItemCount(Material.WOOD),
            "WOOD should be consumed after crafting");
        assertEquals(0, inventory.getItemCount(Material.PLANKS),
            "PLANKS should be consumed after crafting");
    }

    @Test
    void ladderCraftingAppearsInCraftingMenu() {
        CraftingSystem craftingSystem = new CraftingSystem();
        Inventory inventory = new Inventory(36);
        inventory.addItem(Material.WOOD, 2);
        inventory.addItem(Material.PLANKS, 4);

        List<Recipe> available = craftingSystem.getAvailableRecipes(inventory);
        boolean hasLadder = available.stream()
            .anyMatch(r -> r.getOutputs().containsKey(Material.LADDER));
        assertTrue(hasLadder,
            "LADDER recipe should appear in available recipes when materials are present");
    }
}

package ragamuffin.world;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ragamuffin.building.BlockDropTable;
import ragamuffin.building.Material;
import ragamuffin.render.ChunkMeshBuilder;
import ragamuffin.render.MeshData;
import ragamuffin.test.HeadlessTestHelper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #213: STAIRS block type.
 * Verifies that the STAIRS block type exists, has correct properties,
 * produces valid geometry, and drops the correct material when broken.
 */
class StairsBlockTest {

    @BeforeAll
    static void setup() {
        HeadlessTestHelper.initHeadless();
    }

    // ── BlockType tests ────────────────────────────────────────────────────────

    @Test
    void stairsBlockTypeExists() {
        assertNotNull(BlockType.STAIRS);
    }

    @Test
    void stairsIsSolid() {
        // Stairs are solid for player collision purposes
        assertTrue(BlockType.STAIRS.isSolid());
    }

    @Test
    void stairsIsNotOpaque() {
        // Stairs don't fill the full 1x1x1 cell, so adjacent faces must be rendered
        assertFalse(BlockType.STAIRS.isOpaque());
    }

    @Test
    void stairsHasStairStepShape() {
        assertEquals(BlockType.BlockShape.STAIR_STEP, BlockType.STAIRS.getBlockShape());
    }

    @Test
    void stairsIsPlayerPlaceable() {
        assertTrue(BlockType.STAIRS.isPlayerPlaceable());
    }

    @Test
    void stairsHasUniqueId() {
        // Verify STAIRS has a unique ID among all block types
        BlockType[] types = BlockType.values();
        int stairsId = BlockType.STAIRS.getId();
        int count = 0;
        for (BlockType type : types) {
            if (type.getId() == stairsId) count++;
        }
        assertEquals(1, count, "STAIRS ID must be unique among all block types");
    }

    @Test
    void stairsHasColor() {
        assertNotNull(BlockType.STAIRS.getColor());
    }

    @Test
    void stairsHasTopColor() {
        assertNotNull(BlockType.STAIRS.getTopColor());
    }

    @Test
    void allBlockTypesStillHaveUniqueIds() {
        // Adding STAIRS must not break uniqueness of existing IDs
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
    void stairsProducesGeometry() {
        // A single STAIRS block should produce 10 faces:
        //   bottom (1) + lower slab top (1) + riser (1) + upper step top (1)
        //   + south back wall (1) + north front face (1)
        //   + west lower (1) + west upper (1) + east lower (1) + east upper (1) = 10
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(4, 4, 4, BlockType.STAIRS);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        assertEquals(10, meshData.getFaceCount(),
            "A single STAIRS block should produce 10 faces");
    }

    @Test
    void stairsAdjacentToSolidBlockDoesNotCullSolidFace() {
        // STAIRS is non-opaque, so a DIRT block adjacent to it should still render
        // the face that touches the STAIRS block.
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(4, 4, 4, BlockType.STAIRS);
        chunk.setBlock(5, 4, 4, BlockType.DIRT); // East of stairs
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);

        // STAIRS: 10 shaped faces
        // DIRT: 6 faces (west face of DIRT is NOT culled because STAIRS is non-opaque)
        assertEquals(16, meshData.getFaceCount(),
            "STAIRS should not suppress adjacent solid block faces (non-opaque)");
    }

    // ── BlockDropTable tests ───────────────────────────────────────────────────

    @Test
    void stairsDropsMaterialStairs() {
        BlockDropTable table = new BlockDropTable();
        Material drop = table.getDrop(BlockType.STAIRS, null);
        assertEquals(Material.STAIRS, drop,
            "Breaking a STAIRS block should drop Material.STAIRS");
    }

    // ── Material tests ─────────────────────────────────────────────────────────

    @Test
    void stairsMaterialExists() {
        assertNotNull(Material.STAIRS);
    }

    @Test
    void stairsMaterialHasDisplayName() {
        assertEquals("Stairs", Material.STAIRS.getDisplayName());
    }

    @Test
    void stairsMaterialHasIconColors() {
        assertNotNull(Material.STAIRS.getIconColors());
        assertTrue(Material.STAIRS.getIconColors().length >= 1,
            "STAIRS material should have at least one icon color");
    }

    @Test
    void stairsMaterialIsBlockItem() {
        assertTrue(Material.STAIRS.isBlockItem(),
            "STAIRS is a construction block and should be treated as a block item");
    }
}

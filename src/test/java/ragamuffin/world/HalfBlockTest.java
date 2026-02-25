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
 * Tests for Issue #654: HALF_BLOCK block type.
 * Verifies that the HALF_BLOCK type exists, has correct properties,
 * produces valid geometry (6 faces), and drops the correct material when broken.
 */
class HalfBlockTest {

    @BeforeAll
    static void setup() {
        HeadlessTestHelper.initHeadless();
    }

    // ── BlockType tests ────────────────────────────────────────────────────────

    @Test
    void halfBlockTypeExists() {
        assertNotNull(BlockType.HALF_BLOCK);
    }

    @Test
    void halfBlockIsSolid() {
        assertTrue(BlockType.HALF_BLOCK.isSolid());
    }

    @Test
    void halfBlockIsNotOpaque() {
        // Half-block doesn't fill the full 1x1x1 cell, so adjacent faces must be rendered
        assertFalse(BlockType.HALF_BLOCK.isOpaque());
    }

    @Test
    void halfBlockHasHalfSlabShape() {
        assertEquals(BlockType.BlockShape.HALF_SLAB, BlockType.HALF_BLOCK.getBlockShape());
    }

    @Test
    void halfBlockIsPlayerPlaceable() {
        assertTrue(BlockType.HALF_BLOCK.isPlayerPlaceable());
    }

    @Test
    void halfBlockHasUniqueId() {
        BlockType[] types = BlockType.values();
        int halfBlockId = BlockType.HALF_BLOCK.getId();
        int count = 0;
        for (BlockType type : types) {
            if (type.getId() == halfBlockId) count++;
        }
        assertEquals(1, count, "HALF_BLOCK ID must be unique among all block types");
    }

    @Test
    void halfBlockHasColor() {
        assertNotNull(BlockType.HALF_BLOCK.getColor());
    }

    @Test
    void halfBlockHasTopColor() {
        assertNotNull(BlockType.HALF_BLOCK.getTopColor());
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
    void halfBlockProduces6Faces() {
        // A single HALF_BLOCK slab has exactly 6 faces:
        // bottom (1) + top at y+0.5 (1) + north (1) + south (1) + west (1) + east (1) = 6
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(4, 4, 4, BlockType.HALF_BLOCK);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        assertEquals(6, meshData.getFaceCount(),
            "A single HALF_BLOCK should produce exactly 6 faces");
    }

    @Test
    void halfBlockAdjacentToSolidBlockDoesNotCullSolidFace() {
        // HALF_BLOCK is non-opaque, so a DIRT block adjacent to it should still
        // render the face touching the HALF_BLOCK.
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(4, 4, 4, BlockType.HALF_BLOCK);
        chunk.setBlock(5, 4, 4, BlockType.DIRT); // East of half-block
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);

        // HALF_BLOCK: 6 shaped faces
        // DIRT: 6 faces (west face of DIRT is NOT culled because HALF_BLOCK is non-opaque)
        assertEquals(12, meshData.getFaceCount(),
            "HALF_BLOCK should not suppress adjacent solid block faces (non-opaque)");
    }

    // ── BlockDropTable tests ───────────────────────────────────────────────────

    @Test
    void halfBlockDropsMaterialHalfBlock() {
        BlockDropTable table = new BlockDropTable();
        Material drop = table.getDrop(BlockType.HALF_BLOCK, null);
        assertEquals(Material.HALF_BLOCK, drop,
            "Breaking a HALF_BLOCK should drop Material.HALF_BLOCK");
    }

    // ── Material tests ─────────────────────────────────────────────────────────

    @Test
    void halfBlockMaterialExists() {
        assertNotNull(Material.HALF_BLOCK);
    }

    @Test
    void halfBlockMaterialHasDisplayName() {
        assertEquals("Half Block", Material.HALF_BLOCK.getDisplayName());
    }

    @Test
    void halfBlockMaterialHasIconColors() {
        assertNotNull(Material.HALF_BLOCK.getIconColors());
        assertTrue(Material.HALF_BLOCK.getIconColors().length >= 1,
            "HALF_BLOCK material should have at least one icon color");
    }

    @Test
    void halfBlockMaterialIsBlockItem() {
        assertTrue(Material.HALF_BLOCK.isBlockItem(),
            "HALF_BLOCK is a construction block and should be treated as a block item");
    }
}

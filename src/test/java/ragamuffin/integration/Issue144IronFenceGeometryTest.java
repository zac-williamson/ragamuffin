package ragamuffin.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ragamuffin.render.ChunkMeshBuilder;
import ragamuffin.render.MeshData;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.BlockType;
import ragamuffin.world.Chunk;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #144: Iron fence blocks should have proper 3D fence-post geometry
 * and be non-opaque (transparent gaps visible through them).
 */
class Issue144IronFenceGeometryTest {

    @BeforeAll
    static void setup() {
        HeadlessTestHelper.initHeadless();
    }

    @Test
    void ironFenceHasFencePostShape() {
        assertEquals(BlockType.BlockShape.FENCE_POST, BlockType.IRON_FENCE.getBlockShape(),
            "IRON_FENCE should use FENCE_POST shape for proper 3D geometry");
    }

    @Test
    void ironFenceIsNotOpaque() {
        assertFalse(BlockType.IRON_FENCE.isOpaque(),
            "IRON_FENCE should be non-opaque so adjacent faces show through");
    }

    @Test
    void ironFenceIsSolidForCollision() {
        assertTrue(BlockType.IRON_FENCE.isSolid(),
            "IRON_FENCE should still be solid for player collision");
    }

    @Test
    void ironFenceProducesPostGeometry() {
        // A single IRON_FENCE block should produce 6 faces (4 sides + top + bottom of the post)
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(4, 4, 4, BlockType.IRON_FENCE);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        assertEquals(6, meshData.getFaceCount(),
            "A single IRON_FENCE block should produce 6 faces as a thin fence post");
    }

    @Test
    void ironFenceDoesNotCullAdjacentSolidFaces() {
        // IRON_FENCE is non-opaque — adjacent solid blocks should still render their facing face.
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(4, 4, 4, BlockType.IRON_FENCE);
        chunk.setBlock(5, 4, 4, BlockType.DIRT);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        // IRON_FENCE: 6 shaped faces; DIRT: 6 faces (west face rendered since fence is non-opaque)
        assertEquals(12, meshData.getFaceCount(),
            "IRON_FENCE should not suppress adjacent solid block faces");
    }

    @Test
    void ironFenceHasColor() {
        assertNotNull(BlockType.IRON_FENCE.getColor(),
            "IRON_FENCE should have a colour defined");
    }

    @Test
    void ironFenceHasTopColor() {
        assertNotNull(BlockType.IRON_FENCE.getTopColor(),
            "IRON_FENCE should have a top colour defined");
    }

    @Test
    void twoAdjacentIronFencesProduceTwelveFaces() {
        // Two adjacent IRON_FENCE posts: each produces its own shaped geometry (not merged).
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(4, 4, 4, BlockType.IRON_FENCE);
        chunk.setBlock(5, 4, 4, BlockType.IRON_FENCE);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        // Each IRON_FENCE post = 6 faces; shaped blocks are not greedy-merged.
        assertEquals(12, meshData.getFaceCount(),
            "Two adjacent IRON_FENCE posts should each produce their own 6 faces (12 total)");
    }
}

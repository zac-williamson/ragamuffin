package ragamuffin.render;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.BlockType;
import ragamuffin.world.Chunk;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

class ChunkMeshBuilderTest {

    @BeforeAll
    static void setup() {
        HeadlessTestHelper.initHeadless();
    }

    @Test
    void emptyChunkHasNoFaces() {
        Chunk chunk = new Chunk(0, 0, 0);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        assertEquals(0, meshData.getFaceCount());
    }

    @Test
    void singleBlockHasSixFaces() {
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(8, 8, 8, BlockType.GRASS);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        assertEquals(6, meshData.getFaceCount(), "A single block surrounded by air should have 6 faces");
    }

    @Test
    void twoAdjacentBlocksMergedFaces() {
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(8, 0, 8, BlockType.GRASS);
        chunk.setBlock(8, 1, 8, BlockType.GRASS); // Stacked vertically
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        // Greedy meshing merges same-type adjacent faces:
        // 4 side faces merge from 2 quads each to 1 quad each = 4
        // 1 top face + 1 bottom face = 2
        // Total: 6 faces
        assertEquals(6, meshData.getFaceCount(), "Two stacked same-type blocks should have 6 greedy-merged faces");
    }

    @Test
    void airBlocksAreNotRendered() {
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(5, 5, 5, BlockType.AIR);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        assertEquals(0, meshData.getFaceCount());
    }

    @Test
    void hiddenFacesAreNotRendered() {
        Chunk chunk = new Chunk(0, 0, 0);
        // Create a 2x2x2 cube - only outer faces should be visible
        // Use DIRT (not STONE) because STONE has hasTextureDetail=true which prevents greedy merge
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    chunk.setBlock(x, y, z, BlockType.DIRT);
                }
            }
        }
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        // With greedy meshing, each of the 6 cube faces merges into 1 quad
        // Internal faces between adjacent blocks are hidden (not rendered)
        assertEquals(6, meshData.getFaceCount(),
            "A 2x2x2 same-type cube should have 6 greedy-merged faces (one per cube face)");
    }

    @Test
    void glassBlockHasSixFaces() {
        // Glass is non-opaque but transparent, so its faces should be emitted.
        // Glass faces go to the transparent sub-mesh (alpha blended), not the opaque mesh.
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(8, 8, 8, BlockType.GLASS);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        int totalFaces = meshData.getFaceCount() + meshData.getTransparentMeshData().getFaceCount();
        assertEquals(6, totalFaces, "A single glass block surrounded by air should have 6 faces total");
        assertEquals(6, meshData.getTransparentMeshData().getFaceCount(),
            "All 6 glass faces should be in the transparent sub-mesh");
    }

    @Test
    void solidBlockFaceVisibleThroughGlass() {
        // Place a GRASS block with a GLASS block adjacent — the face between them
        // should be rendered (glass doesn't hide the grass face behind it).
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(8, 8, 8, BlockType.GRASS);
        chunk.setBlock(9, 8, 8, BlockType.GLASS);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        // GRASS: 6 faces (east face not culled since GLASS is non-opaque)
        // GLASS: 5 faces (west face toward GRASS is culled since GRASS is opaque)
        // GRASS goes to opaque mesh, GLASS goes to transparent mesh
        int totalFaces = meshData.getFaceCount() + meshData.getTransparentMeshData().getFaceCount();
        assertEquals(11, totalFaces,
            "GRASS(6 opaque faces) + GLASS(5 transparent faces, west culled by opaque GRASS) = 11 total faces");
    }

    @Test
    void glassFacesGoToTransparentSubMesh() {
        // Glass faces must be stored in the transparent sub-mesh, not the opaque mesh,
        // so they can be rendered with alpha blending after all opaque geometry.
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(8, 8, 8, BlockType.GLASS);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);

        assertEquals(0, meshData.getFaceCount(),
            "Opaque mesh should have 0 faces when only a glass block is present");
        assertEquals(6, meshData.getTransparentMeshData().getFaceCount(),
            "Transparent mesh should have 6 faces for a single glass block surrounded by air");
    }

    @Test
    void opaqueFacesGoToOpaqueMesh() {
        // Opaque block faces must stay in the opaque mesh (no alpha blending).
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(8, 8, 8, BlockType.GRASS);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);

        assertEquals(6, meshData.getFaceCount(),
            "Opaque mesh should have 6 faces for a single grass block");
        assertEquals(0, meshData.getTransparentMeshData().getFaceCount(),
            "Transparent mesh should have 0 faces when only an opaque block is present");
    }

    @Test
    void mixedSceneCorrectlyRoutesOpaqueAndTransparentFaces() {
        // Place a GRASS block and a GLASS block adjacent to each other.
        // GRASS faces should be in the opaque mesh, GLASS faces in the transparent mesh.
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(8, 8, 8, BlockType.GRASS);
        chunk.setBlock(9, 8, 8, BlockType.GLASS);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);

        // GRASS: 6 faces (east face NOT culled because GLASS is non-opaque)
        assertEquals(6, meshData.getFaceCount(),
            "Opaque mesh should have 6 GRASS faces (east face visible through non-opaque glass)");
        // GLASS: 5 faces (west face toward opaque GRASS is culled)
        assertEquals(5, meshData.getTransparentMeshData().getFaceCount(),
            "Transparent mesh should have 5 GLASS faces (west face toward opaque GRASS is culled)");
    }

    @Test
    void crossChunkBoundaryFaceIsCulledWhenNeighbourIsSolid() {
        // Place a DIRT block at the east edge of chunk (0,0,0) at local x=15
        // and a DIRT block at the west edge of chunk (1,0,0) at local x=0.
        // The shared face between them should NOT be rendered by either chunk.
        World world = new World(42L);
        world.setBlock(15, 0, 0, BlockType.DIRT); // east edge of chunk 0
        world.setBlock(16, 0, 0, BlockType.DIRT); // west edge of chunk 1

        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        builder.setWorld(world);

        Chunk chunk0 = world.getChunk(0, 0, 0);
        assertNotNull(chunk0, "Chunk (0,0,0) should exist after setBlock");

        MeshData meshData = builder.build(chunk0);

        // The block at local (15,0,0) has a solid neighbour in chunk 1 on its east side.
        // Without cross-chunk culling: 6 faces (all sides of the isolated boundary block
        // appear exposed because the neighbour returns AIR).
        // With correct cross-chunk culling: 5 faces (east face toward solid neighbour is hidden).
        assertEquals(5, meshData.getFaceCount(),
            "East face of boundary block should be culled because neighbouring chunk has a solid block flush against it");
    }
}

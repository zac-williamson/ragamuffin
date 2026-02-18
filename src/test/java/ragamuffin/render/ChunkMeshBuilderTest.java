package ragamuffin.render;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.BlockType;
import ragamuffin.world.Chunk;

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
}

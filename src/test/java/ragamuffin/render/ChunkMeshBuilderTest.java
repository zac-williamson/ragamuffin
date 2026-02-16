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
    void twoAdjacentBlocksHaveTenFaces() {
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(8, 0, 8, BlockType.GRASS);
        chunk.setBlock(8, 1, 8, BlockType.GRASS); // Stacked vertically
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        // Bottom block: 5 faces (top is hidden)
        // Top block: 5 faces (bottom is hidden)
        // Total: 10 faces
        assertEquals(10, meshData.getFaceCount(), "Two stacked blocks should have 10 faces (6+6-2)");
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
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    chunk.setBlock(x, y, z, BlockType.STONE);
                }
            }
        }
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        // A 2x2x2 cube has 24 outer faces
        // Each face of the cube: 2x2 = 4 quads per face, 6 faces = 24
        assertEquals(24, meshData.getFaceCount());
    }
}

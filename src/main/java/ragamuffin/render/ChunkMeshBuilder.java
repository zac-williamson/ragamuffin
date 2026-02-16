package ragamuffin.render;

import com.badlogic.gdx.graphics.Color;
import ragamuffin.world.BlockType;
import ragamuffin.world.Chunk;

/**
 * Builds 3D meshes from chunk data.
 * Only renders exposed faces (greedy meshing can be added later).
 */
public class ChunkMeshBuilder {

    private static final float BLOCK_SIZE = 1.0f;

    public MeshData build(Chunk chunk) {
        MeshData meshData = new MeshData();
        short vertexIndex = 0;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    BlockType block = chunk.getBlock(x, y, z);
                    if (block == BlockType.AIR) {
                        continue;
                    }

                    float worldX = chunk.getChunkX() * Chunk.SIZE + x;
                    float worldY = y;
                    float worldZ = chunk.getChunkZ() * Chunk.SIZE + z;

                    Color color = block.getColor();

                    // Check each face and add if exposed
                    // Top face (+Y)
                    if (!chunk.getBlock(x, y + 1, z).isSolid()) {
                        vertexIndex = addTopFace(meshData, worldX, worldY, worldZ, color, vertexIndex);
                    }

                    // Bottom face (-Y)
                    if (!chunk.getBlock(x, y - 1, z).isSolid()) {
                        vertexIndex = addBottomFace(meshData, worldX, worldY, worldZ, color, vertexIndex);
                    }

                    // North face (-Z)
                    if (!chunk.getBlock(x, y, z - 1).isSolid()) {
                        vertexIndex = addNorthFace(meshData, worldX, worldY, worldZ, color, vertexIndex);
                    }

                    // South face (+Z)
                    if (!chunk.getBlock(x, y, z + 1).isSolid()) {
                        vertexIndex = addSouthFace(meshData, worldX, worldY, worldZ, color, vertexIndex);
                    }

                    // West face (-X)
                    if (!chunk.getBlock(x - 1, y, z).isSolid()) {
                        vertexIndex = addWestFace(meshData, worldX, worldY, worldZ, color, vertexIndex);
                    }

                    // East face (+X)
                    if (!chunk.getBlock(x + 1, y, z).isSolid()) {
                        vertexIndex = addEastFace(meshData, worldX, worldY, worldZ, color, vertexIndex);
                    }
                }
            }
        }

        return meshData;
    }

    private short addTopFace(MeshData meshData, float x, float y, float z, Color color, short baseIndex) {
        float[] vertices = {
            // Position (3), Normal (3), UV (2), Color (4) - total 12 floats per vertex
            x, y + BLOCK_SIZE, z,               0, 1, 0,    0, 0,   color.r, color.g, color.b, color.a,
            x + BLOCK_SIZE, y + BLOCK_SIZE, z,  0, 1, 0,    1, 0,   color.r, color.g, color.b, color.a,
            x + BLOCK_SIZE, y + BLOCK_SIZE, z + BLOCK_SIZE,  0, 1, 0,    1, 1,   color.r, color.g, color.b, color.a,
            x, y + BLOCK_SIZE, z + BLOCK_SIZE,  0, 1, 0,    0, 1,   color.r, color.g, color.b, color.a
        };
        short[] indices = {0, 1, 2, 2, 3, 0};
        meshData.addQuad(vertices, indices, baseIndex);
        return (short)(baseIndex + 4);
    }

    private short addBottomFace(MeshData meshData, float x, float y, float z, Color color, short baseIndex) {
        float[] vertices = {
            x, y, z,               0, -1, 0,   0, 0,   color.r, color.g, color.b, color.a,
            x, y, z + BLOCK_SIZE,  0, -1, 0,   0, 1,   color.r, color.g, color.b, color.a,
            x + BLOCK_SIZE, y, z + BLOCK_SIZE,  0, -1, 0,   1, 1,   color.r, color.g, color.b, color.a,
            x + BLOCK_SIZE, y, z,  0, -1, 0,   1, 0,   color.r, color.g, color.b, color.a
        };
        short[] indices = {0, 1, 2, 2, 3, 0};
        meshData.addQuad(vertices, indices, baseIndex);
        return (short)(baseIndex + 4);
    }

    private short addNorthFace(MeshData meshData, float x, float y, float z, Color color, short baseIndex) {
        float[] vertices = {
            x, y, z,               0, 0, -1,   0, 0,   color.r, color.g, color.b, color.a,
            x + BLOCK_SIZE, y, z,  0, 0, -1,   1, 0,   color.r, color.g, color.b, color.a,
            x + BLOCK_SIZE, y + BLOCK_SIZE, z,  0, 0, -1,   1, 1,   color.r, color.g, color.b, color.a,
            x, y + BLOCK_SIZE, z,  0, 0, -1,   0, 1,   color.r, color.g, color.b, color.a
        };
        short[] indices = {0, 1, 2, 2, 3, 0};
        meshData.addQuad(vertices, indices, baseIndex);
        return (short)(baseIndex + 4);
    }

    private short addSouthFace(MeshData meshData, float x, float y, float z, Color color, short baseIndex) {
        float[] vertices = {
            x + BLOCK_SIZE, y, z + BLOCK_SIZE,  0, 0, 1,   0, 0,   color.r, color.g, color.b, color.a,
            x, y, z + BLOCK_SIZE,               0, 0, 1,   1, 0,   color.r, color.g, color.b, color.a,
            x, y + BLOCK_SIZE, z + BLOCK_SIZE,  0, 0, 1,   1, 1,   color.r, color.g, color.b, color.a,
            x + BLOCK_SIZE, y + BLOCK_SIZE, z + BLOCK_SIZE,  0, 0, 1,   0, 1,   color.r, color.g, color.b, color.a
        };
        short[] indices = {0, 1, 2, 2, 3, 0};
        meshData.addQuad(vertices, indices, baseIndex);
        return (short)(baseIndex + 4);
    }

    private short addWestFace(MeshData meshData, float x, float y, float z, Color color, short baseIndex) {
        float[] vertices = {
            x, y, z + BLOCK_SIZE,  -1, 0, 0,   0, 0,   color.r, color.g, color.b, color.a,
            x, y, z,               -1, 0, 0,   1, 0,   color.r, color.g, color.b, color.a,
            x, y + BLOCK_SIZE, z,  -1, 0, 0,   1, 1,   color.r, color.g, color.b, color.a,
            x, y + BLOCK_SIZE, z + BLOCK_SIZE,  -1, 0, 0,   0, 1,   color.r, color.g, color.b, color.a
        };
        short[] indices = {0, 1, 2, 2, 3, 0};
        meshData.addQuad(vertices, indices, baseIndex);
        return (short)(baseIndex + 4);
    }

    private short addEastFace(MeshData meshData, float x, float y, float z, Color color, short baseIndex) {
        float[] vertices = {
            x + BLOCK_SIZE, y, z,  1, 0, 0,   0, 0,   color.r, color.g, color.b, color.a,
            x + BLOCK_SIZE, y, z + BLOCK_SIZE,  1, 0, 0,   1, 0,   color.r, color.g, color.b, color.a,
            x + BLOCK_SIZE, y + BLOCK_SIZE, z + BLOCK_SIZE,  1, 0, 0,   1, 1,   color.r, color.g, color.b, color.a,
            x + BLOCK_SIZE, y + BLOCK_SIZE, z,  1, 0, 0,   0, 1,   color.r, color.g, color.b, color.a
        };
        short[] indices = {0, 1, 2, 2, 3, 0};
        meshData.addQuad(vertices, indices, baseIndex);
        return (short)(baseIndex + 4);
    }
}

package ragamuffin.render;

import com.badlogic.gdx.graphics.Color;
import ragamuffin.world.BlockType;
import ragamuffin.world.Chunk;
import ragamuffin.world.World;

/**
 * Builds 3D meshes from chunk data using greedy meshing.
 * Merges adjacent coplanar faces of the same block type into larger quads
 * to dramatically reduce vertex/triangle count.
 * Optionally uses World reference for cross-chunk neighbour queries to eliminate
 * visible seams at chunk boundaries.
 */
public class ChunkMeshBuilder {

    private static final float BLOCK_SIZE = 1.0f;

    // Reusable mask arrays to avoid per-build allocation
    // Max slice is SIZE x HEIGHT (16 x 64 = 1024)
    private static final int MAX_SLICE = Chunk.SIZE * Chunk.HEIGHT;
    private final BlockType[] mask = new BlockType[MAX_SLICE];
    private final boolean[] merged = new boolean[MAX_SLICE];

    // Optional world reference for cross-chunk queries
    private World world;

    /**
     * Set the world reference for cross-chunk neighbour queries.
     * When set, faces at chunk boundaries are only emitted if the adjacent
     * block in the neighbouring chunk is non-solid.
     */
    public void setWorld(World world) {
        this.world = world;
    }

    /**
     * Get the block at a local position within the chunk.
     * Returns AIR for out-of-bounds positions (assumed to be neighbouring chunks).
     * PERFORMANCE: Removed cross-chunk World queries to prevent browser freeze.
     */
    private BlockType getWorldBlock(Chunk chunk, int localX, int localY, int localZ) {
        // Within chunk bounds — use chunk directly
        if (localX >= 0 && localX < Chunk.SIZE &&
            localY >= 0 && localY < Chunk.HEIGHT &&
            localZ >= 0 && localZ < Chunk.SIZE) {
            return chunk.getBlock(localX, localY, localZ);
        }
        // Outside chunk bounds — treat as AIR to avoid expensive World lookups
        // This may cause minor visual seams at chunk boundaries, but prevents freezing
        return BlockType.AIR;
    }

    public MeshData build(Chunk chunk) {
        MeshData meshData = new MeshData();
        int vertexIndex = 0;

        // Greedy mesh each axis/direction
        // X-axis faces (West -X and East +X)
        vertexIndex = greedyMeshX(chunk, meshData, vertexIndex, false); // West
        vertexIndex = greedyMeshX(chunk, meshData, vertexIndex, true);  // East

        // Y-axis faces (Bottom -Y and Top +Y)
        vertexIndex = greedyMeshY(chunk, meshData, vertexIndex, false); // Bottom
        vertexIndex = greedyMeshY(chunk, meshData, vertexIndex, true);  // Top

        // Z-axis faces (North -Z and South +Z)
        vertexIndex = greedyMeshZ(chunk, meshData, vertexIndex, false); // North
        vertexIndex = greedyMeshZ(chunk, meshData, vertexIndex, true);  // South

        return meshData;
    }

    /**
     * Greedy mesh for X-normal faces (West/East walls).
     * Slices along X; each slice is a YZ plane of size HEIGHT x SIZE.
     */
    private int greedyMeshX(Chunk chunk, MeshData meshData, int vertexIndex, boolean positive) {
        int sliceW = Chunk.SIZE;  // z dimension
        int sliceH = Chunk.HEIGHT; // y dimension

        for (int x = 0; x <= Chunk.SIZE; x++) {
            // Build mask for this slice
            int maskSize = 0;
            for (int y = 0; y < sliceH; y++) {
                for (int z = 0; z < sliceW; z++) {
                    int idx = y * sliceW + z;
                    BlockType current;
                    BlockType neighbour;

                    if (positive) {
                        // East face: block at x-1 with no solid block at x
                        current = getWorldBlock(chunk, x - 1, y, z);
                        neighbour = getWorldBlock(chunk, x, y, z);
                    } else {
                        // West face: block at x with no solid block at x-1
                        current = getWorldBlock(chunk, x, y, z);
                        neighbour = getWorldBlock(chunk, x - 1, y, z);
                    }

                    if (current != BlockType.AIR && current.isSolid() && !neighbour.isSolid()) {
                        mask[idx] = current;
                    } else {
                        mask[idx] = null;
                    }
                }
            }

            // Greedy merge the mask
            java.util.Arrays.fill(merged, 0, sliceW * sliceH, false);

            for (int y = 0; y < sliceH; y++) {
                for (int z = 0; z < sliceW; z++) {
                    int idx = y * sliceW + z;
                    if (mask[idx] == null || merged[idx]) continue;

                    BlockType type = mask[idx];
                    boolean textured = type.hasTextureDetail();

                    // Don't merge textured blocks — each gets unique colour
                    int w = 1;
                    int h = 1;
                    if (!textured) {
                        // Expand width (z direction)
                        while (z + w < sliceW && mask[y * sliceW + z + w] == type && !merged[y * sliceW + z + w]) {
                            w++;
                        }
                        // Expand height (y direction)
                        outer:
                        while (y + h < sliceH) {
                            for (int dz = 0; dz < w; dz++) {
                                int checkIdx = (y + h) * sliceW + z + dz;
                                if (mask[checkIdx] != type || merged[checkIdx]) break outer;
                            }
                            h++;
                        }
                    }

                    // Mark merged
                    for (int dy = 0; dy < h; dy++) {
                        for (int dz = 0; dz < w; dz++) {
                            merged[(y + dy) * sliceW + z + dz] = true;
                        }
                    }

                    // Get colour — textured blocks use position-dependent colour
                    int worldX = chunk.getChunkX() * Chunk.SIZE + (positive ? x - 1 : x);
                    int worldY = chunk.getChunkY() * Chunk.HEIGHT + y;
                    int worldZ = chunk.getChunkZ() * Chunk.SIZE + z;
                    Color color = textured ? type.getTexturedColor(worldX, worldY, worldZ, false) : type.getColor();

                    float fx = x;
                    float fy = y;
                    float fz = z;
                    float fh = h;
                    float fw = w;

                    if (positive) {
                        vertexIndex = addFace(meshData, vertexIndex, color,
                            fx, fy, fz + fw,
                            fx, fy, fz,
                            fx, fy + fh, fz,
                            fx, fy + fh, fz + fw,
                            1, 0, 0, fw, fh);
                    } else {
                        vertexIndex = addFace(meshData, vertexIndex, color,
                            fx, fy, fz,
                            fx, fy, fz + fw,
                            fx, fy + fh, fz + fw,
                            fx, fy + fh, fz,
                            -1, 0, 0, fw, fh);
                    }
                }
            }
        }
        return vertexIndex;
    }

    /**
     * Greedy mesh for Y-normal faces (Top/Bottom).
     * Slices along Y; each slice is an XZ plane of size SIZE x SIZE.
     */
    private int greedyMeshY(Chunk chunk, MeshData meshData, int vertexIndex, boolean positive) {
        int sliceW = Chunk.SIZE;  // z dimension
        int sliceH = Chunk.SIZE;  // x dimension

        for (int y = 0; y <= Chunk.HEIGHT; y++) {
            // Build mask
            for (int x = 0; x < sliceH; x++) {
                for (int z = 0; z < sliceW; z++) {
                    int idx = x * sliceW + z;
                    BlockType current;
                    BlockType neighbour;

                    if (positive) {
                        // Top face: block at y-1 with no solid block at y
                        current = getWorldBlock(chunk, x, y - 1, z);
                        neighbour = getWorldBlock(chunk, x, y, z);
                    } else {
                        // Bottom face: block at y with no solid block at y-1
                        current = getWorldBlock(chunk, x, y, z);
                        neighbour = getWorldBlock(chunk, x, y - 1, z);
                    }

                    if (current != BlockType.AIR && current.isSolid() && !neighbour.isSolid()) {
                        mask[idx] = current;
                    } else {
                        mask[idx] = null;
                    }
                }
            }

            // Greedy merge
            java.util.Arrays.fill(merged, 0, sliceW * sliceH, false);

            for (int x = 0; x < sliceH; x++) {
                for (int z = 0; z < sliceW; z++) {
                    int idx = x * sliceW + z;
                    if (mask[idx] == null || merged[idx]) continue;

                    BlockType type = mask[idx];
                    boolean textured = type.hasTextureDetail();

                    int w = 1;
                    int h = 1;
                    if (!textured) {
                        // Expand width (z direction)
                        while (z + w < sliceW && mask[x * sliceW + z + w] == type && !merged[x * sliceW + z + w]) {
                            w++;
                        }
                        // Expand height (x direction)
                        outer:
                        while (x + h < sliceH) {
                            for (int dz = 0; dz < w; dz++) {
                                int checkIdx = (x + h) * sliceW + z + dz;
                                if (mask[checkIdx] != type || merged[checkIdx]) break outer;
                            }
                            h++;
                        }
                    }

                    // Mark merged
                    for (int dx = 0; dx < h; dx++) {
                        for (int dz = 0; dz < w; dz++) {
                            merged[(x + dx) * sliceW + z + dz] = true;
                        }
                    }

                    // Get colour — textured blocks use position-dependent colour
                    int worldX = chunk.getChunkX() * Chunk.SIZE + x;
                    int worldY = chunk.getChunkY() * Chunk.HEIGHT + (positive ? y - 1 : y);
                    int worldZ = chunk.getChunkZ() * Chunk.SIZE + z;

                    float fx = x;
                    float fy = y;
                    float fz = z;
                    float fh = h;
                    float fw = w;

                    if (positive) {
                        Color topColor = textured ? type.getTexturedColor(worldX, worldY, worldZ, true) : type.getTopColor();
                        vertexIndex = addFace(meshData, vertexIndex, topColor,
                            fx, fy, fz + fw,
                            fx + fh, fy, fz + fw,
                            fx + fh, fy, fz,
                            fx, fy, fz,
                            0, 1, 0, fh, fw);
                    } else {
                        Color bottomColor = textured ? type.getTexturedColor(worldX, worldY, worldZ, false) : type.getBottomColor();
                        vertexIndex = addFace(meshData, vertexIndex, bottomColor,
                            fx, fy, fz,
                            fx + fh, fy, fz,
                            fx + fh, fy, fz + fw,
                            fx, fy, fz + fw,
                            0, -1, 0, fh, fw);
                    }
                }
            }
        }
        return vertexIndex;
    }

    /**
     * Greedy mesh for Z-normal faces (North/South walls).
     * Slices along Z; each slice is an XY plane of size SIZE x HEIGHT.
     */
    private int greedyMeshZ(Chunk chunk, MeshData meshData, int vertexIndex, boolean positive) {
        int sliceW = Chunk.SIZE;   // x dimension
        int sliceH = Chunk.HEIGHT; // y dimension

        for (int z = 0; z <= Chunk.SIZE; z++) {
            // Build mask
            for (int y = 0; y < sliceH; y++) {
                for (int x = 0; x < sliceW; x++) {
                    int idx = y * sliceW + x;
                    BlockType current;
                    BlockType neighbour;

                    if (positive) {
                        // South face: block at z-1 with no solid at z
                        current = getWorldBlock(chunk, x, y, z - 1);
                        neighbour = getWorldBlock(chunk, x, y, z);
                    } else {
                        // North face: block at z with no solid at z-1
                        current = getWorldBlock(chunk, x, y, z);
                        neighbour = getWorldBlock(chunk, x, y, z - 1);
                    }

                    if (current != BlockType.AIR && current.isSolid() && !neighbour.isSolid()) {
                        mask[idx] = current;
                    } else {
                        mask[idx] = null;
                    }
                }
            }

            // Greedy merge
            java.util.Arrays.fill(merged, 0, sliceW * sliceH, false);

            for (int y = 0; y < sliceH; y++) {
                for (int x = 0; x < sliceW; x++) {
                    int idx = y * sliceW + x;
                    if (mask[idx] == null || merged[idx]) continue;

                    BlockType type = mask[idx];
                    boolean textured = type.hasTextureDetail();

                    int w = 1;
                    int h = 1;
                    if (!textured) {
                        // Expand width (x direction)
                        while (x + w < sliceW && mask[y * sliceW + x + w] == type && !merged[y * sliceW + x + w]) {
                            w++;
                        }
                        // Expand height (y direction)
                        outer:
                        while (y + h < sliceH) {
                            for (int dx = 0; dx < w; dx++) {
                                int checkIdx = (y + h) * sliceW + x + dx;
                                if (mask[checkIdx] != type || merged[checkIdx]) break outer;
                            }
                            h++;
                        }
                    }

                    // Mark merged
                    for (int dy = 0; dy < h; dy++) {
                        for (int dx = 0; dx < w; dx++) {
                            merged[(y + dy) * sliceW + x + dx] = true;
                        }
                    }

                    // Get colour — textured blocks use position-dependent colour
                    int worldX = chunk.getChunkX() * Chunk.SIZE + x;
                    int worldY = chunk.getChunkY() * Chunk.HEIGHT + y;
                    int worldZ = chunk.getChunkZ() * Chunk.SIZE + (positive ? z - 1 : z);
                    Color color = textured ? type.getTexturedColor(worldX, worldY, worldZ, false) : type.getColor();

                    float fx = x;
                    float fy = y;
                    float fz = z;
                    float fh = h;
                    float fw = w;

                    if (positive) {
                        vertexIndex = addFace(meshData, vertexIndex, color,
                            fx, fy, fz,
                            fx + fw, fy, fz,
                            fx + fw, fy + fh, fz,
                            fx, fy + fh, fz,
                            0, 0, 1, fw, fh);
                    } else {
                        vertexIndex = addFace(meshData, vertexIndex, color,
                            fx + fw, fy, fz,
                            fx, fy, fz,
                            fx, fy + fh, fz,
                            fx + fw, fy + fh, fz,
                            0, 0, -1, fw, fh);
                    }
                }
            }
        }
        return vertexIndex;
    }

    /**
     * Add a single quad face to the mesh data.
     */
    private int addFace(MeshData meshData, int baseIndex, Color color,
                        float x0, float y0, float z0,
                        float x1, float y1, float z1,
                        float x2, float y2, float z2,
                        float x3, float y3, float z3,
                        float nx, float ny, float nz,
                        float uScale, float vScale) {
        float[] vertices = {
            x0, y0, z0,  nx, ny, nz,  0, 0,       color.r, color.g, color.b, color.a,
            x1, y1, z1,  nx, ny, nz,  uScale, 0,  color.r, color.g, color.b, color.a,
            x2, y2, z2,  nx, ny, nz,  uScale, vScale, color.r, color.g, color.b, color.a,
            x3, y3, z3,  nx, ny, nz,  0, vScale,  color.r, color.g, color.b, color.a
        };
        short[] indices = {0, 1, 2, 2, 3, 0};
        meshData.addQuad(vertices, indices, baseIndex);
        return baseIndex + 4;
    }
}

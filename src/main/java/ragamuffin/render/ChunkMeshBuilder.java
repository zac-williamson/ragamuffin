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
     * For positions within chunk bounds, queries the chunk directly.
     * For out-of-bounds positions (chunk boundary faces), delegates to the World
     * so that cross-chunk neighbour blocks are correctly considered for face culling.
     * Falls back to AIR if no World is set (e.g. unit tests).
     */
    private BlockType getWorldBlock(Chunk chunk, int localX, int localY, int localZ) {
        // Within chunk bounds — use chunk directly (fast path)
        if (localX >= 0 && localX < Chunk.SIZE &&
            localY >= 0 && localY < Chunk.HEIGHT &&
            localZ >= 0 && localZ < Chunk.SIZE) {
            return chunk.getBlock(localX, localY, localZ);
        }
        // Outside chunk bounds — query the neighbouring chunk via World to avoid seams.
        // Only performed for boundary faces (one axis out-of-bounds at a time), so this
        // adds at most one World lookup per boundary voxel rather than for interior voxels.
        if (world != null) {
            int worldX = chunk.getChunkX() * Chunk.SIZE + localX;
            int worldY = chunk.getChunkY() * Chunk.HEIGHT + localY;
            int worldZ = chunk.getChunkZ() * Chunk.SIZE + localZ;
            return world.getBlock(worldX, worldY, worldZ);
        }
        return BlockType.AIR;
    }

    public MeshData build(Chunk chunk) {
        MeshData meshData = new MeshData();
        int vertexIndex = 0;

        // Greedy mesh each axis/direction (full-cube blocks only)
        // X-axis faces (West -X and East +X)
        vertexIndex = greedyMeshX(chunk, meshData, vertexIndex, false); // West
        vertexIndex = greedyMeshX(chunk, meshData, vertexIndex, true);  // East

        // Y-axis faces (Bottom -Y and Top +Y)
        vertexIndex = greedyMeshY(chunk, meshData, vertexIndex, false); // Bottom
        vertexIndex = greedyMeshY(chunk, meshData, vertexIndex, true);  // Top

        // Z-axis faces (North -Z and South +Z)
        vertexIndex = greedyMeshZ(chunk, meshData, vertexIndex, false); // North
        vertexIndex = greedyMeshZ(chunk, meshData, vertexIndex, true);  // South

        // Shaped blocks: thin fences, doors — emit custom geometry
        vertexIndex = buildShapedBlocks(chunk, meshData, vertexIndex);

        return meshData;
    }

    /**
     * Emit custom geometry for blocks that are not full cubes (FENCE_POST, DOOR_LOWER/UPPER).
     * These bypass greedy meshing and are rendered as thin quads.
     */
    private int buildShapedBlocks(Chunk chunk, MeshData meshData, int vertexIndex) {
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                for (int x = 0; x < Chunk.SIZE; x++) {
                    BlockType type = chunk.getBlock(x, y, z);
                    if (type == BlockType.AIR) continue;
                    BlockType.BlockShape shape = type.getBlockShape();
                    if (shape == BlockType.BlockShape.FULL_CUBE) continue;

                    int worldX = chunk.getChunkX() * Chunk.SIZE + x;
                    int worldY = chunk.getChunkY() * Chunk.HEIGHT + y;
                    int worldZ = chunk.getChunkZ() * Chunk.SIZE + z;

                    switch (shape) {
                        case FENCE_POST:
                            vertexIndex = buildFencePost(meshData, vertexIndex, type, x, y, z, worldX, worldY, worldZ);
                            break;
                        case DOOR_LOWER:
                        case DOOR_UPPER:
                            vertexIndex = buildDoorPanel(meshData, vertexIndex, type, x, y, z, worldX, worldY, worldZ);
                            break;
                        case STAIR_STEP:
                            vertexIndex = buildStairStep(meshData, vertexIndex, type, x, y, z, worldX, worldY, worldZ);
                            break;
                        case LADDER_RUNGS:
                            vertexIndex = buildLadder(meshData, vertexIndex, type, x, y, z, worldX, worldY, worldZ);
                            break;
                        case HALF_SLAB:
                            vertexIndex = buildHalfSlab(meshData, vertexIndex, type, x, y, z, worldX, worldY, worldZ);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        return vertexIndex;
    }

    /**
     * Build a thin vertical fence post centred in the block cell.
     * The post is FENCE_THICKNESS wide on both X and Z axes, full height (1 block).
     * Rendered as a cross (+) shape when viewed from above, like standard fence posts.
     */
    private static final float FENCE_THICKNESS = 0.125f; // 1/8 block
    private static final float FENCE_HALF = FENCE_THICKNESS / 2.0f;

    private int buildFencePost(MeshData meshData, int vertexIndex, BlockType type,
                               int lx, int ly, int lz,
                               int worldX, int worldY, int worldZ) {
        Color color = type.getColor();
        float x = lx, y = ly, z = lz;
        float cx = x + 0.5f, cz = z + 0.5f; // centre of block cell
        float x0 = cx - FENCE_HALF, x1 = cx + FENCE_HALF;
        float z0 = cz - FENCE_HALF, z1 = cz + FENCE_HALF;
        float y0 = y, y1 = y + 1.0f;

        // North face (z0, facing -Z)
        vertexIndex = addFace(meshData, vertexIndex, color,
            x1, y0, z0,  x0, y0, z0,  x0, y1, z0,  x1, y1, z0,
            0, 0, -1, FENCE_THICKNESS, 1.0f);
        // South face (z1, facing +Z)
        vertexIndex = addFace(meshData, vertexIndex, color,
            x0, y0, z1,  x1, y0, z1,  x1, y1, z1,  x0, y1, z1,
            0, 0, 1, FENCE_THICKNESS, 1.0f);
        // West face (x0, facing -X)
        vertexIndex = addFace(meshData, vertexIndex, color,
            x0, y0, z0,  x0, y0, z1,  x0, y1, z1,  x0, y1, z0,
            -1, 0, 0, FENCE_THICKNESS, 1.0f);
        // East face (x1, facing +X)
        vertexIndex = addFace(meshData, vertexIndex, color,
            x1, y0, z1,  x1, y0, z0,  x1, y1, z0,  x1, y1, z1,
            1, 0, 0, FENCE_THICKNESS, 1.0f);
        // Top face
        Color topColor = type.getTopColor();
        vertexIndex = addFace(meshData, vertexIndex, topColor,
            x0, y1, z1,  x1, y1, z1,  x1, y1, z0,  x0, y1, z0,
            0, 1, 0, FENCE_THICKNESS, FENCE_THICKNESS);
        // Bottom face
        vertexIndex = addFace(meshData, vertexIndex, color,
            x0, y0, z0,  x1, y0, z0,  x1, y0, z1,  x0, y0, z1,
            0, -1, 0, FENCE_THICKNESS, FENCE_THICKNESS);

        return vertexIndex;
    }

    /**
     * Build a thin door panel.
     *
     * Closed: panel is 0.125 blocks thick along Z, full width (1 block) on X,
     *         sitting flush against the north face of the block (z = lz).
     * Open:   panel is swung 90° — 0.125 blocks thick along X, full depth (1 block) on Z,
     *         sitting flush against the west face of the block (x = lx).
     *
     * DOOR_LOWER covers y to y+1, DOOR_UPPER covers y to y+1 (together they form a 2-block door).
     */
    private static final float DOOR_THICKNESS = 0.125f;

    private int buildDoorPanel(MeshData meshData, int vertexIndex, BlockType type,
                               int lx, int ly, int lz,
                               int worldX, int worldY, int worldZ) {
        Color color = type.getColor();
        Color topColor = type.getTopColor();
        float y0 = ly, y1 = ly + 1.0f;

        // Determine if this door is open: check the DOOR_LOWER position in the world
        boolean open = false;
        if (world != null) {
            int lowerWorldY = (type == BlockType.DOOR_UPPER) ? worldY - 1 : worldY;
            open = world.isDoorOpen(worldX, lowerWorldY, worldZ);
        }

        if (!open) {
            // Closed: panel along Z (north face)
            float x0 = lx, x1 = lx + 1.0f;
            float z0 = lz, z1 = lz + DOOR_THICKNESS;

            // South face (z1, facing +Z)
            vertexIndex = addFace(meshData, vertexIndex, color,
                x0, y0, z1,  x1, y0, z1,  x1, y1, z1,  x0, y1, z1,
                0, 0, 1, 1.0f, 1.0f);
            // North face (z0, facing -Z)
            vertexIndex = addFace(meshData, vertexIndex, color,
                x1, y0, z0,  x0, y0, z0,  x0, y1, z0,  x1, y1, z0,
                0, 0, -1, 1.0f, 1.0f);
            // East face (x1, facing +X)
            vertexIndex = addFace(meshData, vertexIndex, color,
                x1, y0, z1,  x1, y0, z0,  x1, y1, z0,  x1, y1, z1,
                1, 0, 0, DOOR_THICKNESS, 1.0f);
            // West face (x0, facing -X)
            vertexIndex = addFace(meshData, vertexIndex, color,
                x0, y0, z0,  x0, y0, z1,  x0, y1, z1,  x0, y1, z0,
                -1, 0, 0, DOOR_THICKNESS, 1.0f);
            // Top face
            vertexIndex = addFace(meshData, vertexIndex, topColor,
                x0, y1, z1,  x1, y1, z1,  x1, y1, z0,  x0, y1, z0,
                0, 1, 0, 1.0f, DOOR_THICKNESS);
            // Bottom face
            vertexIndex = addFace(meshData, vertexIndex, color,
                x0, y0, z0,  x1, y0, z0,  x1, y0, z1,  x0, y0, z1,
                0, -1, 0, 1.0f, DOOR_THICKNESS);
        } else {
            // Open: panel swung 90° along X (west face), spanning full Z depth
            float x0 = lx, x1 = lx + DOOR_THICKNESS;
            float z0 = lz, z1 = lz + 1.0f;

            // South face (z1, facing +Z)
            vertexIndex = addFace(meshData, vertexIndex, color,
                x0, y0, z1,  x1, y0, z1,  x1, y1, z1,  x0, y1, z1,
                0, 0, 1, DOOR_THICKNESS, 1.0f);
            // North face (z0, facing -Z)
            vertexIndex = addFace(meshData, vertexIndex, color,
                x1, y0, z0,  x0, y0, z0,  x0, y1, z0,  x1, y1, z0,
                0, 0, -1, DOOR_THICKNESS, 1.0f);
            // East face (x1, facing +X)
            vertexIndex = addFace(meshData, vertexIndex, color,
                x1, y0, z1,  x1, y0, z0,  x1, y1, z0,  x1, y1, z1,
                1, 0, 0, 1.0f, 1.0f);
            // West face (x0, facing -X)
            vertexIndex = addFace(meshData, vertexIndex, color,
                x0, y0, z0,  x0, y0, z1,  x0, y1, z1,  x0, y1, z0,
                -1, 0, 0, 1.0f, 1.0f);
            // Top face
            vertexIndex = addFace(meshData, vertexIndex, topColor,
                x0, y1, z1,  x1, y1, z1,  x1, y1, z0,  x0, y1, z0,
                0, 1, 0, DOOR_THICKNESS, 1.0f);
            // Bottom face
            vertexIndex = addFace(meshData, vertexIndex, color,
                x0, y0, z0,  x1, y0, z0,  x1, y0, z1,  x0, y0, z1,
                0, -1, 0, DOOR_THICKNESS, 1.0f);
        }

        return vertexIndex;
    }

    /**
     * Build an L-shaped stair step geometry.
     * The stair occupies the full block footprint (x to x+1, z to z+1) and consists of:
     *   - Lower slab: full width (x to x+1), full depth (z to z+1), height y to y+0.5
     *   - Upper step: full width (x to x+1), back half (z+0.5 to z+1), height y+0.5 to y+1.0
     *
     * This produces an ascending step when approached from the south (+Z direction).
     * 10 faces total: slab bottom (1) + step riser front (1) + step top (1) + slab top (1) +
     *                 west (2) + east (2) + north (1) + south (1) = 10
     */
    private int buildStairStep(MeshData meshData, int vertexIndex, BlockType type,
                               int lx, int ly, int lz,
                               int worldX, int worldY, int worldZ) {
        Color color = type.getColor();
        Color topColor = type.getTopColor();
        float x0 = lx,        x1 = lx + 1.0f;
        float y0 = ly,        yMid = ly + 0.5f, y1 = ly + 1.0f;
        float z0 = lz,        zMid = lz + 0.5f, z1 = lz + 1.0f;

        // ── Bottom face of lower slab ──────────────────────────────────────────
        vertexIndex = addFace(meshData, vertexIndex, color,
            x0, y0, z0,  x1, y0, z0,  x1, y0, z1,  x0, y0, z1,
            0, -1, 0, 1.0f, 1.0f);

        // ── Top face of lower slab (forward half — in front of the step riser) ─
        vertexIndex = addFace(meshData, vertexIndex, topColor,
            x0, yMid, z0,  x1, yMid, z0,  x1, yMid, zMid,  x0, yMid, zMid,
            0, 1, 0, 1.0f, 0.5f);

        // ── Step riser face (facing south, +Z) ────────────────────────────────
        vertexIndex = addFace(meshData, vertexIndex, color,
            x0, yMid, zMid,  x1, yMid, zMid,  x1, y1, zMid,  x0, y1, zMid,
            0, 0, 1, 1.0f, 0.5f);

        // ── Top face of upper step ─────────────────────────────────────────────
        vertexIndex = addFace(meshData, vertexIndex, topColor,
            x0, y1, zMid,  x1, y1, zMid,  x1, y1, z1,  x0, y1, z1,
            0, 1, 0, 1.0f, 0.5f);

        // ── South face of upper step (full height of upper step, back wall) ────
        vertexIndex = addFace(meshData, vertexIndex, color,
            x1, y0, z1,  x0, y0, z1,  x0, y1, z1,  x1, y1, z1,
            0, 0, -1, 1.0f, 1.0f);

        // ── North face (front of lower slab) ──────────────────────────────────
        vertexIndex = addFace(meshData, vertexIndex, color,
            x0, y0, z0,  x1, y0, z0,  x1, yMid, z0,  x0, yMid, z0,
            0, 0, -1, 1.0f, 0.5f);

        // ── West face (full L-shape profile) ──────────────────────────────────
        // Lower slab portion
        vertexIndex = addFace(meshData, vertexIndex, color,
            x0, y0, z1,  x0, y0, z0,  x0, yMid, z0,  x0, yMid, z1,
            -1, 0, 0, 1.0f, 0.5f);
        // Upper step portion
        vertexIndex = addFace(meshData, vertexIndex, color,
            x0, yMid, z1,  x0, yMid, zMid,  x0, y1, zMid,  x0, y1, z1,
            -1, 0, 0, 0.5f, 0.5f);

        // ── East face (full L-shape profile) ──────────────────────────────────
        // Lower slab portion
        vertexIndex = addFace(meshData, vertexIndex, color,
            x1, y0, z0,  x1, y0, z1,  x1, yMid, z1,  x1, yMid, z0,
            1, 0, 0, 1.0f, 0.5f);
        // Upper step portion
        vertexIndex = addFace(meshData, vertexIndex, color,
            x1, yMid, zMid,  x1, yMid, z1,  x1, y1, z1,  x1, y1, zMid,
            1, 0, 0, 0.5f, 0.5f);

        return vertexIndex;
    }

    /**
     * Build a ladder geometry: two vertical side rails plus four horizontal rungs,
     * rendered as a flat panel flush against the north face of the block (z = lz).
     *
     * Structure (viewed from the front, facing +Z):
     *   Left rail:  x0..x0+RAIL_WIDTH, y0..y1
     *   Right rail: x1-RAIL_WIDTH..x1, y0..y1
     *   4 rungs evenly spaced across x0+RAIL_WIDTH to x1-RAIL_WIDTH
     *
     * Face count: 2 rails × 2 faces (front+back) = 4
     *           + 4 rungs × 2 faces (front+back) = 8
     *           Total = 12 faces
     */
    private static final float LADDER_THICKNESS = 0.075f;
    private static final float LADDER_RAIL_WIDTH = 0.1f;
    private static final float LADDER_RUNG_HEIGHT = 0.075f;
    private static final int   LADDER_RUNG_COUNT  = 4;

    private int buildLadder(MeshData meshData, int vertexIndex, BlockType type,
                            int lx, int ly, int lz,
                            int worldX, int worldY, int worldZ) {
        Color color = type.getColor();
        float x0 = lx, x1 = lx + 1.0f;
        float y0 = ly, y1 = ly + 1.0f;
        float z0 = lz, z1 = lz + LADDER_THICKNESS;

        float lRailX0 = x0;
        float lRailX1 = x0 + LADDER_RAIL_WIDTH;
        float rRailX0 = x1 - LADDER_RAIL_WIDTH;
        float rRailX1 = x1;

        // ── Left vertical rail ─────────────────────────────────────────────────
        // Front face (+Z)
        vertexIndex = addFace(meshData, vertexIndex, color,
            lRailX0, y0, z1,  lRailX1, y0, z1,  lRailX1, y1, z1,  lRailX0, y1, z1,
            0, 0, 1, LADDER_RAIL_WIDTH, 1.0f);
        // Back face (-Z)
        vertexIndex = addFace(meshData, vertexIndex, color,
            lRailX1, y0, z0,  lRailX0, y0, z0,  lRailX0, y1, z0,  lRailX1, y1, z0,
            0, 0, -1, LADDER_RAIL_WIDTH, 1.0f);

        // ── Right vertical rail ────────────────────────────────────────────────
        // Front face (+Z)
        vertexIndex = addFace(meshData, vertexIndex, color,
            rRailX0, y0, z1,  rRailX1, y0, z1,  rRailX1, y1, z1,  rRailX0, y1, z1,
            0, 0, 1, LADDER_RAIL_WIDTH, 1.0f);
        // Back face (-Z)
        vertexIndex = addFace(meshData, vertexIndex, color,
            rRailX1, y0, z0,  rRailX0, y0, z0,  rRailX0, y1, z0,  rRailX1, y1, z0,
            0, 0, -1, LADDER_RAIL_WIDTH, 1.0f);

        // ── Horizontal rungs ───────────────────────────────────────────────────
        float rungX0 = lRailX1;
        float rungX1 = rRailX0;
        float rungSpacing = 1.0f / (LADDER_RUNG_COUNT + 1);
        for (int i = 1; i <= LADDER_RUNG_COUNT; i++) {
            float rungBaseY = y0 + i * rungSpacing;
            float rungTopY  = rungBaseY + LADDER_RUNG_HEIGHT;
            // Front face (+Z)
            vertexIndex = addFace(meshData, vertexIndex, color,
                rungX0, rungBaseY, z1,  rungX1, rungBaseY, z1,  rungX1, rungTopY, z1,  rungX0, rungTopY, z1,
                0, 0, 1, rungX1 - rungX0, LADDER_RUNG_HEIGHT);
            // Back face (-Z)
            vertexIndex = addFace(meshData, vertexIndex, color,
                rungX1, rungBaseY, z0,  rungX0, rungBaseY, z0,  rungX0, rungTopY, z0,  rungX1, rungTopY, z0,
                0, 0, -1, rungX1 - rungX0, LADDER_RUNG_HEIGHT);
        }

        return vertexIndex;
    }

    /**
     * Build a half-slab geometry.
     * The slab occupies the full block footprint (x to x+1, z to z+1) at the lower
     * half of the cell (y to y+0.5). It has a flat top at mid-height, a bottom face,
     * and four side faces of half height.
     *
     * Face count: 6 (bottom + top + north + south + west + east)
     */
    private int buildHalfSlab(MeshData meshData, int vertexIndex, BlockType type,
                              int lx, int ly, int lz,
                              int worldX, int worldY, int worldZ) {
        Color color = type.getColor();
        Color topColor = type.getTopColor();
        float x0 = lx, x1 = lx + 1.0f;
        float y0 = ly, yTop = ly + 0.5f;
        float z0 = lz, z1 = lz + 1.0f;

        // ── Bottom face ────────────────────────────────────────────────────────
        vertexIndex = addFace(meshData, vertexIndex, color,
            x0, y0, z0,  x1, y0, z0,  x1, y0, z1,  x0, y0, z1,
            0, -1, 0, 1.0f, 1.0f);

        // ── Top face (at half height) ──────────────────────────────────────────
        vertexIndex = addFace(meshData, vertexIndex, topColor,
            x0, yTop, z1,  x1, yTop, z1,  x1, yTop, z0,  x0, yTop, z0,
            0, 1, 0, 1.0f, 1.0f);

        // ── North face (z0, facing -Z) ─────────────────────────────────────────
        vertexIndex = addFace(meshData, vertexIndex, color,
            x1, y0, z0,  x0, y0, z0,  x0, yTop, z0,  x1, yTop, z0,
            0, 0, -1, 1.0f, 0.5f);

        // ── South face (z1, facing +Z) ─────────────────────────────────────────
        vertexIndex = addFace(meshData, vertexIndex, color,
            x0, y0, z1,  x1, y0, z1,  x1, yTop, z1,  x0, yTop, z1,
            0, 0, 1, 1.0f, 0.5f);

        // ── West face (x0, facing -X) ──────────────────────────────────────────
        vertexIndex = addFace(meshData, vertexIndex, color,
            x0, y0, z1,  x0, y0, z0,  x0, yTop, z0,  x0, yTop, z1,
            -1, 0, 0, 1.0f, 0.5f);

        // ── East face (x1, facing +X) ──────────────────────────────────────────
        vertexIndex = addFace(meshData, vertexIndex, color,
            x1, y0, z0,  x1, y0, z1,  x1, yTop, z1,  x1, yTop, z0,
            1, 0, 0, 1.0f, 0.5f);

        return vertexIndex;
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

                    if (current != BlockType.AIR && (current.isOpaque() || current.isTransparent()) && !neighbour.isOpaque()) {
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
                    boolean isTransparent = type.isTransparent();

                    float fx = x;
                    float fy = y;
                    float fz = z;
                    float fh = h;
                    float fw = w;

                    if (positive) {
                        vertexIndex = addFace(meshData, vertexIndex, color, isTransparent,
                            fx, fy, fz + fw,
                            fx, fy, fz,
                            fx, fy + fh, fz,
                            fx, fy + fh, fz + fw,
                            1, 0, 0, fw, fh);
                    } else {
                        vertexIndex = addFace(meshData, vertexIndex, color, isTransparent,
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

                    if (current != BlockType.AIR && (current.isOpaque() || current.isTransparent()) && !neighbour.isOpaque()) {
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
                    boolean isTransparent = type.isTransparent();

                    float fx = x;
                    float fy = y;
                    float fz = z;
                    float fh = h;
                    float fw = w;

                    if (positive) {
                        Color topColor = textured ? type.getTexturedColor(worldX, worldY, worldZ, true) : type.getTopColor();
                        vertexIndex = addFace(meshData, vertexIndex, topColor, isTransparent,
                            fx, fy, fz + fw,
                            fx + fh, fy, fz + fw,
                            fx + fh, fy, fz,
                            fx, fy, fz,
                            0, 1, 0, fh, fw);
                    } else {
                        Color bottomColor = textured ? type.getTexturedColor(worldX, worldY, worldZ, false) : type.getBottomColor();
                        vertexIndex = addFace(meshData, vertexIndex, bottomColor, isTransparent,
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

                    if (current != BlockType.AIR && (current.isOpaque() || current.isTransparent()) && !neighbour.isOpaque()) {
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
                    boolean isTransparent = type.isTransparent();

                    float fx = x;
                    float fy = y;
                    float fz = z;
                    float fh = h;
                    float fw = w;

                    if (positive) {
                        vertexIndex = addFace(meshData, vertexIndex, color, isTransparent,
                            fx, fy, fz,
                            fx + fw, fy, fz,
                            fx + fw, fy + fh, fz,
                            fx, fy + fh, fz,
                            0, 0, 1, fw, fh);
                    } else {
                        vertexIndex = addFace(meshData, vertexIndex, color, isTransparent,
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
     * If the block type is transparent (alpha < 1), the face is added to the
     * transparent sub-mesh so it can be rendered with alpha blending after
     * all opaque geometry, preventing see-through artifacts.
     */
    private int addFace(MeshData meshData, int baseIndex, Color color,
                        float x0, float y0, float z0,
                        float x1, float y1, float z1,
                        float x2, float y2, float z2,
                        float x3, float y3, float z3,
                        float nx, float ny, float nz,
                        float uScale, float vScale) {
        return addFace(meshData, baseIndex, color, false,
            x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3,
            nx, ny, nz, uScale, vScale);
    }

    /**
     * Add a single quad face to the mesh data, with explicit transparency routing.
     * Transparent faces (transparent=true) go to the alpha-blended sub-mesh.
     */
    private int addFace(MeshData meshData, int baseIndex, Color color, boolean transparent,
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
        if (transparent) {
            meshData.addQuadTransparent(vertices, indices, baseIndex);
        } else {
            meshData.addQuad(vertices, indices, baseIndex);
        }
        return baseIndex + 4;
    }
}

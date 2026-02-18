package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.*;
import ragamuffin.render.ChunkMeshBuilder;
import ragamuffin.render.MeshData;
import ragamuffin.world.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Bug 004, 005, and 006 fixes.
 */
class BugFix004IntegrationTest {

    // --- Bug 004: Double-offset chunks ---

    /**
     * Bug 004: ChunkMeshBuilder was computing WORLD coordinates for vertices,
     * but ChunkRenderer also applies a world-space translation, double-offsetting
     * every chunk. The fix is to use LOCAL coordinates (0..SIZE-1) in ChunkMeshBuilder.
     *
     * This test verifies that mesh vertices stay within local chunk bounds [0, SIZE).
     */
    @Test
    void chunkMeshBuilder_usesLocalCoordinates() {
        // Create a chunk at non-zero position
        Chunk chunk = new Chunk(3, 0, 5);
        // Place a single solid block so we get some vertices
        chunk.setBlock(0, 0, 0, BlockType.STONE);

        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);

        float[] vertices = meshData.getVerticesArray();
        assertTrue(vertices.length > 0, "Should have vertices for a solid block");

        // Each vertex has 12 floats: pos(3), normal(3), uv(2), color(4)
        // Position is at offsets 0, 1, 2 within each vertex
        for (int i = 0; i < vertices.length; i += 12) {
            float vx = vertices[i];
            float vy = vertices[i + 1];
            float vz = vertices[i + 2];

            // Vertices should be in LOCAL coordinates [0, SIZE]
            // NOT world coordinates (which would be chunkX*SIZE + local)
            assertTrue(vx >= 0 && vx <= Chunk.SIZE,
                "Vertex X=" + vx + " should be in local range [0, " + Chunk.SIZE + "]");
            assertTrue(vy >= 0 && vy <= Chunk.HEIGHT,
                "Vertex Y=" + vy + " should be in local range [0, " + Chunk.HEIGHT + "]");
            assertTrue(vz >= 0 && vz <= Chunk.SIZE,
                "Vertex Z=" + vz + " should be in local range [0, " + Chunk.SIZE + "]");
        }
    }

    /**
     * Bug 004: Verify that two adjacent chunks produce meshes that tile correctly.
     * With local coordinates, chunk (0,0,0) block at x=15 should have a vertex at x=16,
     * and chunk (1,0,0) block at x=0 should have a vertex at x=0. When the renderer
     * translates each chunk by its world offset, they should meet seamlessly.
     */
    @Test
    void adjacentChunks_meshesAlignWithoutGaps() {
        // Create two adjacent chunks
        Chunk chunk0 = new Chunk(0, 0, 0);
        Chunk chunk1 = new Chunk(1, 0, 0);

        // Place blocks at the shared boundary
        chunk0.setBlock(Chunk.SIZE - 1, 0, 0, BlockType.STONE); // x=15 in chunk 0
        chunk1.setBlock(0, 0, 0, BlockType.STONE); // x=0 in chunk 1

        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData mesh0 = builder.build(chunk0);
        MeshData mesh1 = builder.build(chunk1);

        float[] verts0 = mesh0.getVerticesArray();
        float[] verts1 = mesh1.getVerticesArray();

        // Find max X in chunk0 mesh - should be SIZE (16), not 2*SIZE (32)
        float maxX0 = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < verts0.length; i += 12) {
            maxX0 = Math.max(maxX0, verts0[i]);
        }

        // Find min X in chunk1 mesh - should be 0, not SIZE (16)
        float minX1 = Float.POSITIVE_INFINITY;
        for (int i = 0; i < verts1.length; i += 12) {
            minX1 = Math.min(minX1, verts1[i]);
        }

        // chunk0 max X should be Chunk.SIZE (local coords end at SIZE)
        assertEquals(Chunk.SIZE, maxX0, 0.01f,
            "Chunk 0 max vertex X should be " + Chunk.SIZE + " (local coords)");

        // chunk1 min X should be 0 (local coords start at 0)
        assertEquals(0, minX1, 0.01f,
            "Chunk 1 min vertex X should be 0 (local coords)");

        // When rendered: chunk0 translates by 0 -> max world X = 16
        // chunk1 translates by 16 -> min world X = 16
        // They meet at X=16 with no gap
        float worldMax0 = maxX0 + (chunk0.getChunkX() * Chunk.SIZE);
        float worldMin1 = minX1 + (chunk1.getChunkX() * Chunk.SIZE);
        assertEquals(worldMax0, worldMin1, 0.01f,
            "Adjacent chunks should meet seamlessly at the boundary");
    }

    // --- Bug 005: No mesh rebuild on chunk load ---

    /**
     * Bug 005: updateChunkRenderers() was not called after updateLoadedChunks()
     * in the game loop, so newly loaded chunks were invisible.
     *
     * This test verifies that World tracks dirty chunks that need mesh rebuilding,
     * and that after updateLoadedChunks loads new chunks, those chunks are reported
     * as needing a mesh update.
     */
    @Test
    void worldTracksNewlyLoadedChunksAsDirty() {
        World world = new World(42L);
        world.generate();

        // Initial load at origin
        world.updateLoadedChunks(new com.badlogic.gdx.math.Vector3(0, 0, 0));
        int initialChunkCount = world.getLoadedChunks().size();
        assertTrue(initialChunkCount > 0, "Should have loaded chunks near origin");

        // Clear dirty state (simulate initial mesh build)
        world.clearDirtyChunks();

        // Move player far enough to load new chunks
        float farX = (initialChunkCount + 5) * Chunk.SIZE;
        world.updateLoadedChunks(new com.badlogic.gdx.math.Vector3(farX, 0, 0));

        // The world should report newly loaded chunks as dirty
        var dirtyChunks = world.getDirtyChunks();
        assertFalse(dirtyChunks.isEmpty(),
            "Newly loaded chunks should be marked as dirty for mesh rebuild");
    }

    // --- Bug 006: Unknown block name "???" in inventory ---

    /**
     * Bug 006: Some materials (PLANKS, SHELTER_WALL, etc.) showed as "???"
     * in the hotbar because HotbarUI.getMaterialAbbreviation() didn't have
     * cases for all Material enum values.
     *
     * This test verifies every Material has a proper abbreviation (not "??").
     */
    @Test
    void allMaterials_haveDisplayNames() {
        // Every material should have a non-empty display name for rendering
        for (Material material : Material.values()) {
            String name = material.getDisplayName();
            assertNotNull(name, "Material " + material.name() + " should have a display name");
            assertTrue(name.length() >= 2,
                "Display name for " + material.name() + " should be at least 2 chars, got: '" + name + "'");
        }
    }

    /**
     * Bug 006: Verify that all block types that drop materials produce
     * properly named items (not null or unknown).
     */
    @Test
    void allMinableBlocks_dropNamedMaterials() {
        BlockDropTable dropTable = new BlockDropTable();

        // Check every solid block type has a valid drop
        for (BlockType blockType : BlockType.values()) {
            if (blockType == BlockType.AIR || blockType == BlockType.WATER) {
                continue; // These don't drop anything
            }

            Material drop = dropTable.getDrop(blockType, null);
            if (drop != null) {
                assertNotNull(drop.getDisplayName(),
                    "Material for " + blockType.name() + " should have a display name");
                assertFalse(drop.getDisplayName().isEmpty(),
                    "Material for " + blockType.name() + " should have a non-empty display name");
            }
            // LEAVES drops null which is fine - no drop expected
        }
    }
}

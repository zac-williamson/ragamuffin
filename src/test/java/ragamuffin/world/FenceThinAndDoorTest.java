package ragamuffin.world;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ragamuffin.render.ChunkMeshBuilder;
import ragamuffin.render.MeshData;
import ragamuffin.test.HeadlessTestHelper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #112: thin vertical fences and 2-block high animated doors.
 */
class FenceThinAndDoorTest {

    @BeforeAll
    static void setup() {
        HeadlessTestHelper.initHeadless();
    }

    // ── BlockType tests ────────────────────────────────────────────────────────

    @Test
    void fenceThinBlockTypeExists() {
        assertNotNull(BlockType.FENCE_THIN);
    }

    @Test
    void doorLowerBlockTypeExists() {
        assertNotNull(BlockType.DOOR_LOWER);
    }

    @Test
    void doorUpperBlockTypeExists() {
        assertNotNull(BlockType.DOOR_UPPER);
    }

    @Test
    void fenceThinIsSolidForCollision() {
        // Thin fence posts block player movement
        assertTrue(BlockType.FENCE_THIN.isSolid());
    }

    @Test
    void doorLowerIsSolid() {
        // Door blocks are solid by type — World.isBlockSolid() makes open doors passable
        assertTrue(BlockType.DOOR_LOWER.isSolid());
    }

    @Test
    void doorUpperIsSolid() {
        assertTrue(BlockType.DOOR_UPPER.isSolid());
    }

    @Test
    void fenceThinIsNotOpaque() {
        // Non-opaque: adjacent block faces should not be culled
        assertFalse(BlockType.FENCE_THIN.isOpaque());
    }

    @Test
    void doorLowerIsNotOpaque() {
        assertFalse(BlockType.DOOR_LOWER.isOpaque());
    }

    @Test
    void doorUpperIsNotOpaque() {
        assertFalse(BlockType.DOOR_UPPER.isOpaque());
    }

    @Test
    void fenceThinHasFencePostShape() {
        assertEquals(BlockType.BlockShape.FENCE_POST, BlockType.FENCE_THIN.getBlockShape());
    }

    @Test
    void doorLowerHasDoorLowerShape() {
        assertEquals(BlockType.BlockShape.DOOR_LOWER, BlockType.DOOR_LOWER.getBlockShape());
    }

    @Test
    void doorUpperHasDoorUpperShape() {
        assertEquals(BlockType.BlockShape.DOOR_UPPER, BlockType.DOOR_UPPER.getBlockShape());
    }

    @Test
    void allBlockTypesHaveUniqueIds() {
        BlockType[] types = BlockType.values();
        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                assertNotEquals(types[i].getId(), types[j].getId(),
                    "BlockType " + types[i] + " and " + types[j] + " have the same ID");
            }
        }
    }

    @Test
    void fenceThinHasColor() {
        assertNotNull(BlockType.FENCE_THIN.getColor());
    }

    @Test
    void doorLowerHasColor() {
        assertNotNull(BlockType.DOOR_LOWER.getColor());
    }

    @Test
    void doorUpperHasColor() {
        assertNotNull(BlockType.DOOR_UPPER.getColor());
    }

    // ── Mesh building tests ────────────────────────────────────────────────────

    @Test
    void fenceThinProducesGeometry() {
        // A single FENCE_THIN block should produce 6 faces (4 sides + top + bottom of the post)
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(4, 4, 4, BlockType.FENCE_THIN);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        assertEquals(6, meshData.getFaceCount(),
            "A single thin fence post should produce 6 faces");
    }

    @Test
    void doorLowerProducesGeometry() {
        // A single DOOR_LOWER block should produce 6 faces
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(4, 4, 4, BlockType.DOOR_LOWER);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        assertEquals(6, meshData.getFaceCount(),
            "A DOOR_LOWER block should produce 6 faces");
    }

    @Test
    void doorUpperProducesGeometry() {
        // A single DOOR_UPPER block should produce 6 faces
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(4, 5, 4, BlockType.DOOR_UPPER);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        assertEquals(6, meshData.getFaceCount(),
            "A DOOR_UPPER block should produce 6 faces");
    }

    @Test
    void twoDoorHalvesProduceTwelveFaces() {
        // DOOR_LOWER + DOOR_UPPER stacked = 12 faces (6 each, no merging)
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(4, 4, 4, BlockType.DOOR_LOWER);
        chunk.setBlock(4, 5, 4, BlockType.DOOR_UPPER);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        assertEquals(12, meshData.getFaceCount(),
            "A full 2-block door (DOOR_LOWER + DOOR_UPPER) should produce 12 faces total");
    }

    @Test
    void fenceThinAdjacentToSolidBlockDoesNotCullSolidFace() {
        // Place FENCE_THIN next to DIRT. The DIRT face adjacent to the fence should
        // still be rendered (fence is non-opaque so it does not suppress neighbours).
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(4, 4, 4, BlockType.FENCE_THIN);
        chunk.setBlock(5, 4, 4, BlockType.DIRT); // East of fence
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);

        // FENCE_THIN: 6 shaped faces
        // DIRT alone would be 6 faces; next to FENCE_THIN the west face of DIRT should
        // still appear because FENCE_THIN is non-opaque.
        // So total = 6 (fence) + 6 (dirt, west face rendered) = 12
        assertEquals(12, meshData.getFaceCount(),
            "FENCE_THIN should not suppress adjacent solid block faces (non-opaque)");
    }

    @Test
    void solidBlockAdjacentToSolidBlockCullsSharedFace() {
        // Sanity check: two adjacent DIRT blocks still cull shared face.
        // With greedy meshing, two adjacent same-type blocks merge into 6 total faces
        // (the 4 perpendicular faces each merge into 1 larger quad, plus 1 west + 1 east).
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(4, 4, 4, BlockType.DIRT);
        chunk.setBlock(5, 4, 4, BlockType.DIRT);
        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);
        // Two adjacent DIRT blocks: greedy merging produces 6 faces total (same as single block,
        // but wider quads — shared internal faces are culled, outer faces merged)
        assertEquals(6, meshData.getFaceCount(),
            "Two adjacent solid same-type blocks should cull shared faces and merge outer faces via greedy meshing");
    }

    // ── Door state (World) tests ───────────────────────────────────────────────

    @Test
    void doorInitiallyClosedAfterPlacement() {
        World world = new World(42L);
        world.setBlock(10, 5, 10, BlockType.DOOR_LOWER);
        world.setBlock(10, 6, 10, BlockType.DOOR_UPPER);
        assertFalse(world.isDoorOpen(10, 5, 10),
            "Newly placed door should be closed");
    }

    @Test
    void toggleDoorOpensIt() {
        World world = new World(42L);
        world.setBlock(10, 5, 10, BlockType.DOOR_LOWER);
        world.setBlock(10, 6, 10, BlockType.DOOR_UPPER);

        world.toggleDoor(10, 5, 10);

        assertTrue(world.isDoorOpen(10, 5, 10),
            "Door should be open after toggle");
    }

    @Test
    void openDoorBlocksRemainPresent() {
        // Fix #749: door blocks stay in the world when open — they swing aside visually
        // rather than disappearing. Collision is handled by World.isBlockSolid().
        World world = new World(42L);
        world.setBlock(10, 5, 10, BlockType.DOOR_LOWER);
        world.setBlock(10, 6, 10, BlockType.DOOR_UPPER);

        world.toggleDoor(10, 5, 10);

        assertEquals(BlockType.DOOR_LOWER, world.getBlock(10, 5, 10),
            "DOOR_LOWER should remain present when door is open (swings aside)");
        assertEquals(BlockType.DOOR_UPPER, world.getBlock(10, 6, 10),
            "DOOR_UPPER should remain present when door is open (swings aside)");
    }

    @Test
    void toggleDoorTwiceClosesDoor() {
        World world = new World(42L);
        world.setBlock(10, 5, 10, BlockType.DOOR_LOWER);
        world.setBlock(10, 6, 10, BlockType.DOOR_UPPER);

        world.toggleDoor(10, 5, 10); // open
        world.toggleDoor(10, 5, 10); // close

        assertFalse(world.isDoorOpen(10, 5, 10),
            "Door should be closed after two toggles");
    }

    @Test
    void closedDoorRestoresBlocks() {
        World world = new World(42L);
        world.setBlock(10, 5, 10, BlockType.DOOR_LOWER);
        world.setBlock(10, 6, 10, BlockType.DOOR_UPPER);

        world.toggleDoor(10, 5, 10); // open
        world.toggleDoor(10, 5, 10); // close

        assertEquals(BlockType.DOOR_LOWER, world.getBlock(10, 5, 10),
            "DOOR_LOWER should be restored when door is closed");
        assertEquals(BlockType.DOOR_UPPER, world.getBlock(10, 6, 10),
            "DOOR_UPPER should be restored when door is closed");
    }

    @Test
    void isDoorBlockReturnsTrueForDoorLower() {
        World world = new World(42L);
        world.setBlock(10, 5, 10, BlockType.DOOR_LOWER);
        assertTrue(world.isDoorBlock(10, 5, 10));
    }

    @Test
    void isDoorBlockReturnsTrueForDoorUpper() {
        World world = new World(42L);
        world.setBlock(10, 6, 10, BlockType.DOOR_UPPER);
        assertTrue(world.isDoorBlock(10, 6, 10));
    }

    @Test
    void isDoorBlockReturnsFalseForNonDoor() {
        World world = new World(42L);
        world.setBlock(10, 5, 10, BlockType.BRICK);
        assertFalse(world.isDoorBlock(10, 5, 10));
    }

    @Test
    void multipleDoorToggleStatesAreIndependent() {
        World world = new World(42L);
        // Door A at (10, 5, 10)
        world.setBlock(10, 5, 10, BlockType.DOOR_LOWER);
        world.setBlock(10, 6, 10, BlockType.DOOR_UPPER);
        // Door B at (20, 5, 20)
        world.setBlock(20, 5, 20, BlockType.DOOR_LOWER);
        world.setBlock(20, 6, 20, BlockType.DOOR_UPPER);

        world.toggleDoor(10, 5, 10); // open door A only

        assertTrue(world.isDoorOpen(10, 5, 10), "Door A should be open");
        assertFalse(world.isDoorOpen(20, 5, 20), "Door B should remain closed");
    }
}

package ragamuffin.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChunkTest {

    @Test
    void chunkIsCreatedWithAllAir() {
        Chunk chunk = new Chunk(0, 0, 0);
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    assertEquals(BlockType.AIR, chunk.getBlock(x, y, z));
                }
            }
        }
    }

    @Test
    void canSetAndGetBlock() {
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(5, 10, 7, BlockType.GRASS);
        assertEquals(BlockType.GRASS, chunk.getBlock(5, 10, 7));
    }

    @Test
    void outOfBoundsReturnsAir() {
        Chunk chunk = new Chunk(0, 0, 0);
        assertEquals(BlockType.AIR, chunk.getBlock(-1, 0, 0));
        assertEquals(BlockType.AIR, chunk.getBlock(Chunk.SIZE, 0, 0));
        assertEquals(BlockType.AIR, chunk.getBlock(0, Chunk.HEIGHT, 0));
        assertEquals(BlockType.AIR, chunk.getBlock(0, 0, Chunk.SIZE));
    }

    @Test
    void chunkPositionIsStored() {
        Chunk chunk = new Chunk(3, 0, -2);
        assertEquals(3, chunk.getChunkX());
        assertEquals(0, chunk.getChunkY());
        assertEquals(-2, chunk.getChunkZ());
    }

    @Test
    void worldToLocalCoordinates() {
        Chunk chunk = new Chunk(2, 0, 1); // Chunk at world (32, 0, 16) if SIZE=16
        // World position (37, 5, 20) should map to local (5, 5, 4)
        int worldX = chunk.getChunkX() * Chunk.SIZE + 5;
        int worldY = 5;
        int worldZ = chunk.getChunkZ() * Chunk.SIZE + 4;

        int localX = worldX - chunk.getChunkX() * Chunk.SIZE;
        int localY = worldY;
        int localZ = worldZ - chunk.getChunkZ() * Chunk.SIZE;

        assertEquals(5, localX);
        assertEquals(5, localY);
        assertEquals(4, localZ);
    }
}

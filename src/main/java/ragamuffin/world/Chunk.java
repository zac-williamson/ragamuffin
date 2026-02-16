package ragamuffin.world;

/**
 * A chunk is a 16x64x16 section of the voxel world.
 * Stores block data and generates meshes for rendering.
 */
public class Chunk {
    public static final int SIZE = 16;    // X and Z dimensions
    public static final int HEIGHT = 64;  // Y dimension

    private final int chunkX, chunkY, chunkZ;  // Chunk grid position
    private final BlockType[][][] blocks;

    public Chunk(int chunkX, int chunkY, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        this.blocks = new BlockType[SIZE][HEIGHT][SIZE];

        // Initialize with air
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIZE; z++) {
                    blocks[x][y][z] = BlockType.AIR;
                }
            }
        }
    }

    public BlockType getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) {
            return BlockType.AIR;
        }
        return blocks[x][y][z];
    }

    public void setBlock(int x, int y, int z, BlockType type) {
        if (x >= 0 && x < SIZE && y >= 0 && y < HEIGHT && z >= 0 && z < SIZE) {
            blocks[x][y][z] = type;
        }
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkY() {
        return chunkY;
    }

    public int getChunkZ() {
        return chunkZ;
    }
}

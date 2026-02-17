package ragamuffin.world;

import com.badlogic.gdx.math.Vector3;
import java.util.Random;

/**
 * Procedurally generates a dense British town with landmarks.
 * The town is centered on a park and has a proper grid of streets
 * with buildings on both sides — terraced houses, shops, an industrial estate,
 * office buildings, and all the amenities of a deprived British high street.
 */
public class WorldGenerator {
    private static final int WORLD_SIZE = 200; // World is 200x200 blocks
    private static final int PARK_SIZE = 30;   // Park is 30x30 blocks
    private static final int STREET_WIDTH = 4;

    private final Random random;
    private final long seed;

    public WorldGenerator(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    /**
     * Generate the entire world - landmarks and structure.
     */
    public void generateWorld(World world) {
        // Clear random state
        random.setSeed(seed);

        // FIRST: Fill entire world with base terrain layer to prevent gaps
        int halfWorld = WORLD_SIZE / 2;
        for (int x = -halfWorld; x < halfWorld; x++) {
            for (int z = -halfWorld; z < halfWorld; z++) {
                world.setBlock(x, -1, z, BlockType.STONE); // Bedrock layer
                world.setBlock(x, 0, z, BlockType.GRASS);  // Surface layer
            }
        }

        // Generate the park at the center (will overwrite base layer)
        generatePark(world);

        // Generate dense street grid
        generateStreets(world);

        // ===== HIGH STREET (along positive X from park, z=20 street) =====
        // South side of high street (z=25, buildings face north toward street)
        generateShopWithSign(world, 20, 25, 7, 8, 4, BlockType.BRICK, BlockType.SIGN_YELLOW, LandmarkType.GREGGS);
        generateShopWithSign(world, 28, 25, 6, 8, 4, BlockType.BRICK, BlockType.SIGN_RED, LandmarkType.OFF_LICENCE);
        generateShopWithSign(world, 35, 25, 7, 8, 4, BlockType.BRICK, BlockType.SIGN_GREEN, LandmarkType.CHARITY_SHOP);
        generateShopWithSign(world, 43, 25, 6, 8, 4, BlockType.GLASS, BlockType.SIGN_WHITE, LandmarkType.JEWELLER);
        generateShopWithSign(world, 50, 25, 7, 8, 4, BlockType.BRICK, BlockType.SIGN_GREEN, LandmarkType.BOOKIES);
        generateShopWithSign(world, 58, 25, 7, 8, 4, BlockType.BRICK, BlockType.SIGN_RED, LandmarkType.KEBAB_SHOP);

        // North side of high street (z=8, buildings face south toward street)
        generateShopWithSign(world, 20, 8, 7, 8, 4, BlockType.BRICK, BlockType.SIGN_BLUE, LandmarkType.TESCO_EXPRESS);
        generateShopWithSign(world, 28, 8, 8, 8, 4, BlockType.BRICK, BlockType.SIGN_WHITE, LandmarkType.LAUNDERETTE);
        generateShopWithSign(world, 37, 8, 8, 8, 5, BlockType.BRICK, BlockType.SIGN_RED, LandmarkType.PUB);
        generateShopWithSign(world, 46, 8, 7, 8, 4, BlockType.BRICK, BlockType.SIGN_YELLOW, LandmarkType.PAWN_SHOP);
        generateShopWithSign(world, 54, 8, 8, 8, 4, BlockType.BRICK, BlockType.SIGN_YELLOW, LandmarkType.BUILDERS_MERCHANT);

        // ===== OFFICE BUILDING (tall, near high street) =====
        generateOfficeBuilding(world, 70, 20, 15, 15, 12);
        world.addLandmark(new Landmark(LandmarkType.OFFICE_BUILDING, 70, 0, 20, 15, 12, 15));

        // ===== JOBCENTRE (west of park) =====
        generateJobCentre(world, -60, 25, 12, 12, 5);
        world.addLandmark(new Landmark(LandmarkType.JOB_CENTRE, -60, 0, 25, 12, 5, 12));

        // ===== TERRACED HOUSES — multiple rows =====
        // Row 1: south of park, south side
        generateTerracedRow(world, -70, -25, 8, 8, 6, 10);
        // Row 2: south of park, north side (across the street)
        generateTerracedRow(world, -70, -41, 8, 8, 6, 10);
        // Row 3: further south
        generateTerracedRow(world, -70, -55, 8, 8, 6, 10);
        // Row 4: east of park
        generateTerracedRow(world, 20, -25, 8, 8, 6, 8);
        // Row 5: west residential area
        generateTerracedRow(world, -70, 30, 8, 8, 6, 8);
        // Row 6: another row facing opposite way
        generateTerracedRow(world, -70, 46, 8, 8, 6, 8);

        // Garden walls between terraced rows
        generateGardenWalls(world, -70, -33, 80, 1);
        generateGardenWalls(world, -70, -49, 80, 1);
        generateGardenWalls(world, -70, 38, 64, 1);

        // ===== INDUSTRIAL ESTATE (northeast corner) =====
        generateWarehouse(world, 60, -40, 20, 15, 8);
        world.addLandmark(new Landmark(LandmarkType.WAREHOUSE, 60, 0, -40, 20, 8, 15));
        generateWarehouse(world, 60, -60, 18, 12, 7);
        generateWarehouse(world, 82, -40, 16, 14, 8);
        // Industrial fence
        generateGardenWalls(world, 58, -65, 44, 2);

        // ===== FILL GAPS — garden walls along streets =====
        fillGapsBetweenBuildings(world, 20, 25, 46);
        fillGapsBetweenBuildings(world, 20, 8, 42);

        // Street-side garden walls in residential areas
        generateGardenWalls(world, -70, -20, 80, 1);
        generateGardenWalls(world, 20, -20, 40, 1);

        // Load initial chunks around origin
        world.updateLoadedChunks(new Vector3(0, 0, 0));
    }

    /**
     * Generate a single chunk (called when chunk is loaded dynamically).
     */
    public void generateChunk(Chunk chunk, World world) {
        int halfWorld = WORLD_SIZE / 2;
        int startX = chunk.getChunkX() * Chunk.SIZE;
        int startZ = chunk.getChunkZ() * Chunk.SIZE;
        int startY = chunk.getChunkY() * Chunk.HEIGHT;

        for (int localX = 0; localX < Chunk.SIZE; localX++) {
            for (int localZ = 0; localZ < Chunk.SIZE; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;

                if (worldX >= -halfWorld && worldX < halfWorld &&
                    worldZ >= -halfWorld && worldZ < halfWorld) {
                    int stoneLocalY = -1 - startY;
                    if (stoneLocalY >= 0 && stoneLocalY < Chunk.HEIGHT) {
                        chunk.setBlock(localX, stoneLocalY, localZ, BlockType.STONE);
                    }
                    int grassLocalY = 0 - startY;
                    if (grassLocalY >= 0 && grassLocalY < Chunk.HEIGHT) {
                        chunk.setBlock(localX, grassLocalY, localZ, BlockType.GRASS);
                    }
                }
            }
        }
    }

    // ==================== PARK ====================

    private void generatePark(World world) {
        int parkStart = -PARK_SIZE / 2;
        int parkEnd = PARK_SIZE / 2;

        for (int x = parkStart; x < parkEnd; x++) {
            for (int z = parkStart; z < parkEnd; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
                world.setBlock(x, -1, z, BlockType.DIRT);
            }
        }

        // Plant trees
        int treeCount = 0;
        for (int i = 0; i < 8 && treeCount < 5; i++) {
            int treeX = parkStart + random.nextInt(PARK_SIZE - 4) + 2;
            int treeZ = parkStart + random.nextInt(PARK_SIZE - 4) + 2;

            for (int y = 1; y <= 4; y++) {
                world.setBlock(treeX, y, treeZ, BlockType.TREE_TRUNK);
            }
            for (int y = 4; y <= 6; y++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0 && y < 6) continue;
                        world.setBlock(treeX + dx, y, treeZ + dz, BlockType.LEAVES);
                    }
                }
            }
            treeCount++;
        }

        // Sad pond
        int pondX = parkStart + PARK_SIZE / 3;
        int pondZ = parkStart + PARK_SIZE / 3;
        for (int x = pondX; x < pondX + 4; x++) {
            for (int z = pondZ; z < pondZ + 4; z++) {
                world.setBlock(x, 0, z, BlockType.WATER);
            }
        }

        // 2-block high iron fence around park perimeter
        for (int x = parkStart; x < parkEnd; x++) {
            for (int y = 1; y <= 2; y++) {
                world.setBlock(x, y, parkStart, BlockType.IRON_FENCE);
                world.setBlock(x, y, parkEnd - 1, BlockType.IRON_FENCE);
            }
        }
        for (int z = parkStart; z < parkEnd; z++) {
            for (int y = 1; y <= 2; y++) {
                world.setBlock(parkStart, y, z, BlockType.IRON_FENCE);
                world.setBlock(parkEnd - 1, y, z, BlockType.IRON_FENCE);
            }
        }
        // Entry gaps
        for (int y = 1; y <= 2; y++) {
            world.setBlock(0, y, parkStart, BlockType.AIR);
            world.setBlock(-1, y, parkStart, BlockType.AIR);
            world.setBlock(0, y, parkEnd - 1, BlockType.AIR);
            world.setBlock(-1, y, parkEnd - 1, BlockType.AIR);
            world.setBlock(parkStart, y, 0, BlockType.AIR);
            world.setBlock(parkStart, y, -1, BlockType.AIR);
            world.setBlock(parkEnd - 1, y, 0, BlockType.AIR);
            world.setBlock(parkEnd - 1, y, -1, BlockType.AIR);
        }

        world.addLandmark(new Landmark(LandmarkType.PARK, parkStart, 0, parkStart, PARK_SIZE, 1, PARK_SIZE));
    }

    // ==================== STREETS ====================

    private void generateStreets(World world) {
        int halfWorld = WORLD_SIZE / 2;
        int streetSpacing = 20;

        // Horizontal streets
        for (int z = -halfWorld; z <= halfWorld; z += streetSpacing) {
            for (int x = -halfWorld; x <= halfWorld; x++) {
                if (Math.abs(x) < PARK_SIZE / 2 && Math.abs(z) < PARK_SIZE / 2) {
                    continue;
                }
                for (int w = 0; w < STREET_WIDTH; w++) {
                    int streetZ = z + w;
                    if (w == 0 || w == STREET_WIDTH - 1) {
                        world.setBlock(x, 0, streetZ, BlockType.PAVEMENT);
                    } else {
                        world.setBlock(x, 0, streetZ, BlockType.ROAD);
                    }
                    world.setBlock(x, -1, streetZ, BlockType.STONE);
                }
            }
        }

        // Vertical streets
        for (int x = -halfWorld; x <= halfWorld; x += streetSpacing) {
            for (int z = -halfWorld; z <= halfWorld; z++) {
                if (Math.abs(x) < PARK_SIZE / 2 && Math.abs(z) < PARK_SIZE / 2) {
                    continue;
                }
                for (int w = 0; w < STREET_WIDTH; w++) {
                    int streetX = x + w;
                    if (w == 0 || w == STREET_WIDTH - 1) {
                        world.setBlock(streetX, 0, z, BlockType.PAVEMENT);
                    } else {
                        world.setBlock(streetX, 0, z, BlockType.ROAD);
                    }
                    world.setBlock(streetX, -1, z, BlockType.STONE);
                }
            }
        }
    }

    // ==================== SHOPS WITH SIGNS ====================

    private void generateShopWithSign(World world, int x, int z, int width, int depth,
                                       int height, BlockType wallType, BlockType signColor,
                                       LandmarkType landmarkType) {
        buildShop(world, x, z, width, depth, height, wallType);

        // Sign strip across front face at top of building
        for (int dx = 0; dx < width; dx++) {
            world.setBlock(x + dx, height, z, signColor);
        }

        world.addLandmark(new Landmark(landmarkType, x, 0, z, width, height, depth));
    }

    // ==================== TERRACED HOUSES ====================

    private void generateTerracedRow(World world, int startX, int startZ,
                                      int houseWidth, int houseDepth, int houseHeight, int count) {
        for (int i = 0; i < count; i++) {
            buildHouse(world, startX + i * houseWidth, startZ, houseWidth, houseDepth, houseHeight);
        }
    }

    // ==================== OFFICE BUILDING ====================

    private void generateOfficeBuilding(World world, int x, int z, int width, int depth, int height) {
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (isWall) {
                        if (y % 2 == 0) {
                            world.setBlock(x + dx, y, z + dz, BlockType.GLASS);
                        } else {
                            world.setBlock(x + dx, y, z + dz, BlockType.STONE);
                        }
                    }
                }
            }
        }
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.STONE);
                world.setBlock(x + dx, 0, z + dz, BlockType.STONE);
            }
        }
        // Sign
        for (int dx = 0; dx < width; dx++) {
            world.setBlock(x + dx, height, z, BlockType.SIGN_BLUE);
        }
    }

    // ==================== JOBCENTRE ====================

    private void generateJobCentre(World world, int x, int z, int width, int depth, int height) {
        buildBuilding(world, x, z, width, depth, height, BlockType.BRICK, BlockType.BRICK);
        for (int dx = 0; dx < width; dx++) {
            world.setBlock(x + dx, height, z, BlockType.SIGN_BLUE);
        }
    }

    // ==================== WAREHOUSES ====================

    private void generateWarehouse(World world, int x, int z, int width, int depth, int height) {
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (isWall) {
                        world.setBlock(x + dx, y, z + dz, BlockType.STONE);
                    }
                }
            }
        }
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.STONE);
                world.setBlock(x + dx, 0, z + dz, BlockType.PAVEMENT);
            }
        }
        // Roller door
        for (int dx = 1; dx < Math.min(5, width - 1); dx++) {
            for (int y = 1; y <= 3; y++) {
                world.setBlock(x + dx, y, z, BlockType.AIR);
            }
        }
    }

    // ==================== GARDEN WALLS ====================

    private void generateGardenWalls(World world, int startX, int z, int length, int wallHeight) {
        for (int dx = 0; dx < length; dx++) {
            for (int y = 1; y <= wallHeight; y++) {
                world.setBlock(startX + dx, y, z, BlockType.GARDEN_WALL);
            }
        }
    }

    private void fillGapsBetweenBuildings(World world, int startX, int z, int length) {
        for (int dx = 0; dx < length; dx++) {
            int x = startX + dx;
            BlockType atPos = world.getBlock(x, 1, z);
            if (atPos == BlockType.AIR) {
                world.setBlock(x, 1, z, BlockType.GARDEN_WALL);
            }
        }
    }

    // ==================== BUILDING HELPERS ====================

    private void buildShop(World world, int x, int z, int width, int depth, int height, BlockType wallType) {
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (isWall) {
                        world.setBlock(x + dx, y, z + dz, wallType);
                    }
                }
            }
        }
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.BRICK);
                world.setBlock(x + dx, 0, z + dz, BlockType.PAVEMENT);
            }
        }
        // Door
        world.setBlock(x + width / 2, 1, z, BlockType.AIR);
        world.setBlock(x + width / 2, 2, z, BlockType.AIR);
    }

    private void buildBuilding(World world, int x, int z, int width, int depth, int height,
                                BlockType wallType, BlockType floorType) {
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (isWall) {
                        if (wallType == BlockType.GLASS && y % 2 == 0) {
                            world.setBlock(x + dx, y, z + dz, BlockType.GLASS);
                        } else {
                            world.setBlock(x + dx, y, z + dz, wallType);
                        }
                    }
                }
            }
        }
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.STONE);
                world.setBlock(x + dx, 0, z + dz, floorType);
            }
        }
    }

    private void buildHouse(World world, int x, int z, int width, int depth, int height) {
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (isWall) {
                        if (y == 2 || y == 4) {
                            world.setBlock(x + dx, y, z + dz, BlockType.GLASS);
                        } else {
                            world.setBlock(x + dx, y, z + dz, BlockType.BRICK);
                        }
                    }
                }
            }
        }
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.BRICK);
                world.setBlock(x + dx, 0, z + dz, BlockType.WOOD);
            }
        }
        // Front door
        world.setBlock(x + width / 2, 1, z, BlockType.AIR);
        world.setBlock(x + width / 2, 2, z, BlockType.AIR);
    }
}

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
    private static final int WORLD_SIZE = 480; // World is 480x480 blocks
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
        generateWarehouse(world, 82, -58, 14, 12, 7);
        // Industrial fence
        generateGardenWalls(world, 58, -65, 44, 2);

        // ===== CHIPPY (south side of high street extension) =====
        generateShopWithSign(world, 66, 25, 7, 8, 4, BlockType.STONE, BlockType.SIGN_WHITE, LandmarkType.CHIPPY);

        // ===== NEWSAGENT (north side of high street extension) =====
        generateShopWithSign(world, 63, 8, 7, 8, 4, BlockType.BRICK, BlockType.SIGN_GREEN, LandmarkType.NEWSAGENT);

        // ===== GP SURGERY (west of park, near JobCentre) =====
        buildBuilding(world, -60, 10, 14, 10, 5, BlockType.BRICK, BlockType.PAVEMENT);
        for (int dx = 0; dx < 14; dx++) {
            world.setBlock(-60 + dx, 5, 10, BlockType.SIGN_BLUE);
        }
        world.addLandmark(new Landmark(LandmarkType.GP_SURGERY, -60, 0, 10, 14, 5, 10));

        // ===== PRIMARY SCHOOL (south of industrial estate) =====
        buildBuilding(world, 60, -80, 20, 16, 6, BlockType.BRICK, BlockType.PAVEMENT);
        for (int dx = 0; dx < 20; dx++) {
            world.setBlock(60 + dx, 6, -80, BlockType.SIGN_BLUE);
        }
        // Playground (fenced grass area next to school)
        for (int x = 82; x < 96; x++) {
            for (int z = -80; z < -68; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
        for (int x = 82; x < 96; x++) {
            for (int y = 1; y <= 2; y++) {
                world.setBlock(x, y, -80, BlockType.IRON_FENCE);
                world.setBlock(x, y, -68, BlockType.IRON_FENCE);
            }
        }
        for (int z = -80; z < -68; z++) {
            for (int y = 1; y <= 2; y++) {
                world.setBlock(82, y, z, BlockType.IRON_FENCE);
                world.setBlock(95, y, z, BlockType.IRON_FENCE);
            }
        }
        world.addLandmark(new Landmark(LandmarkType.PRIMARY_SCHOOL, 60, 0, -80, 20, 6, 16));

        // ===== COMMUNITY CENTRE (west residential area) =====
        buildBuilding(world, -90, -25, 18, 14, 5, BlockType.BRICK, BlockType.PAVEMENT);
        for (int dx = 0; dx < 18; dx++) {
            world.setBlock(-90 + dx, 5, -25, BlockType.SIGN_RED);
        }
        // Front doors (double width)
        world.setBlock(-90 + 8, 1, -25, BlockType.AIR);
        world.setBlock(-90 + 9, 1, -25, BlockType.AIR);
        world.setBlock(-90 + 8, 2, -25, BlockType.AIR);
        world.setBlock(-90 + 9, 2, -25, BlockType.AIR);
        world.addLandmark(new Landmark(LandmarkType.COMMUNITY_CENTRE, -90, 0, -25, 18, 5, 14));

        // ===== CHURCH (northeast residential area) =====
        generateChurch(world, 30, -50, 12, 18, 10);
        world.addLandmark(new Landmark(LandmarkType.CHURCH, 30, 0, -50, 12, 10, 18));

        // ===== TAXI RANK (near high street) =====
        buildBuilding(world, 74, 25, 6, 6, 3, BlockType.BRICK, BlockType.PAVEMENT);
        for (int dx = 0; dx < 6; dx++) {
            world.setBlock(74 + dx, 3, 25, BlockType.SIGN_YELLOW);
        }
        // Forecourt (paved area in front)
        for (int x = 74; x < 80; x++) {
            for (int z = 20; z < 25; z++) {
                world.setBlock(x, 0, z, BlockType.PAVEMENT);
            }
        }
        world.addLandmark(new Landmark(LandmarkType.TAXI_RANK, 74, 0, 25, 6, 3, 6));

        // ===== CAR WASH (near industrial estate) =====
        generateCarWash(world, 100, -40, 10, 8, 5);
        world.addLandmark(new Landmark(LandmarkType.CAR_WASH, 100, 0, -40, 10, 5, 8));

        // ===== COUNCIL FLATS — tower block (west side) =====
        generateCouncilFlats(world, -95, 50, 12, 12, 18);
        world.addLandmark(new Landmark(LandmarkType.COUNCIL_FLATS, -95, 0, 50, 12, 18, 12));

        // ===== SECOND TOWER BLOCK (further west) =====
        generateCouncilFlats(world, -110, 50, 12, 12, 15);
        world.addLandmark(new Landmark(LandmarkType.COUNCIL_FLATS, -110, 0, 50, 12, 15, 12));

        // ===== PETROL STATION (east side) =====
        generatePetrolStation(world, 100, 20, 14, 10, 4);
        world.addLandmark(new Landmark(LandmarkType.PETROL_STATION, 100, 0, 20, 14, 4, 10));

        // ===== NEW HIGH STREET EXTENSION (south side, further east) =====
        // Nando's — yellow brick with red sign
        generateShopWithSign(world, 90, 25, 8, 10, 5, BlockType.YELLOW_BRICK, BlockType.SIGN_RED, LandmarkType.NANDOS);
        // Barber — white tile front
        generateShopWithSign(world, 99, 25, 6, 8, 4, BlockType.TILE_WHITE, BlockType.SIGN_BLUE, LandmarkType.BARBER);
        // Nail salon — pink rendered walls
        generateShopWithSign(world, 106, 25, 7, 8, 4, BlockType.RENDER_PINK, BlockType.SIGN_WHITE, LandmarkType.NAIL_SALON);

        // North side extension
        // Corner shop — yellow brick
        generateShopWithSign(world, 72, 8, 7, 8, 4, BlockType.YELLOW_BRICK, BlockType.SIGN_GREEN, LandmarkType.CORNER_SHOP);
        // Betting shop — red sign
        generateShopWithSign(world, 80, 8, 7, 8, 4, BlockType.BRICK, BlockType.SIGN_RED, LandmarkType.BETTING_SHOP);
        // Phone repair — white render
        generateShopWithSign(world, 88, 8, 6, 8, 4, BlockType.RENDER_WHITE, BlockType.SIGN_YELLOW, LandmarkType.PHONE_REPAIR);
        // Cash Converter — yellow sign
        generateShopWithSign(world, 95, 8, 8, 8, 4, BlockType.BRICK, BlockType.SIGN_YELLOW, LandmarkType.CASH_CONVERTER);

        // ===== WETHERSPOONS — large pub (south of high street, new area) =====
        generateWetherspoons(world, 115, 25, 16, 14, 6);
        world.addLandmark(new Landmark(LandmarkType.WETHERSPOONS, 115, 0, 25, 16, 6, 14));

        // ===== LIBRARY — west side civic area =====
        generateLibrary(world, -80, 10, 16, 12, 6);
        world.addLandmark(new Landmark(LandmarkType.LIBRARY, -80, 0, 10, 16, 6, 12));

        // ===== FIRE STATION — east side, near industrial estate =====
        generateFireStation(world, 100, -65, 16, 14, 7);
        world.addLandmark(new Landmark(LandmarkType.FIRE_STATION, 100, 0, -65, 16, 7, 14));

        // ===== ADDITIONAL TERRACED ROWS for bigger world =====
        generateTerracedRow(world, -110, -25, 8, 8, 6, 5);
        generateTerracedRow(world, -110, -41, 8, 8, 6, 5);
        generateTerracedRow(world, -110, 30, 8, 8, 6, 5);
        generateTerracedRow(world, 90, -25, 8, 8, 6, 4);
        generateTerracedRow(world, 90, -41, 8, 8, 6, 4);

        // ===== FAR TERRACED ROWS (new world edges) =====
        generateTerracedRow(world, -150, -25, 8, 8, 6, 5);
        generateTerracedRow(world, -150, -41, 8, 8, 6, 5);
        generateTerracedRow(world, -150, 30, 8, 8, 6, 5);
        generateTerracedRow(world, -150, 46, 8, 8, 6, 5);
        generateTerracedRow(world, 130, -25, 8, 8, 6, 5);
        generateTerracedRow(world, 130, -41, 8, 8, 6, 5);
        generateTerracedRow(world, 130, 30, 8, 8, 6, 4);
        generateTerracedRow(world, -150, -60, 8, 8, 6, 5);
        generateTerracedRow(world, -150, -76, 8, 8, 6, 5);

        // ===== ADDITIONAL COUNCIL FLATS — outer edges =====
        generateCouncilFlats(world, -150, 60, 12, 12, 14);
        world.addLandmark(new Landmark(LandmarkType.COUNCIL_FLATS, -150, 0, 60, 12, 14, 12));
        generateCouncilFlats(world, 140, -70, 12, 12, 16);

        // Additional garden walls for new rows
        generateGardenWalls(world, -110, -33, 40, 1);
        generateGardenWalls(world, 90, -33, 32, 1);
        generateGardenWalls(world, -150, -33, 40, 1);
        generateGardenWalls(world, -150, -68, 40, 1);
        generateGardenWalls(world, 130, -33, 40, 1);

        // ===== FILL GAPS — garden walls along streets =====
        fillGapsBetweenBuildings(world, 20, 25, 60);
        fillGapsBetweenBuildings(world, 20, 8, 52);

        // Street-side garden walls in residential areas
        generateGardenWalls(world, -150, -20, 170, 1);
        generateGardenWalls(world, 20, -20, 120, 1);

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

    // ==================== CHURCH ====================

    private void generateChurch(World world, int x, int z, int width, int depth, int height) {
        // Main nave — stone walls with slate roof
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (isWall) {
                        // Stained glass windows at even rows, stone otherwise
                        if (y == 3 || y == 5 || y == 7) {
                            world.setBlock(x + dx, y, z + dz, BlockType.GLASS);
                        } else {
                            world.setBlock(x + dx, y, z + dz, BlockType.STONE);
                        }
                    }
                }
            }
        }
        // Slate roof
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.SLATE);
            }
        }
        // Stone floor
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, 0, z + dz, BlockType.STONE);
            }
        }
        // Bell tower — narrow tall section at one end
        int towerX = x + width / 2 - 1;
        int towerZ = z;
        for (int y = height + 1; y <= height + 5; y++) {
            for (int dx = 0; dx < 3; dx++) {
                for (int dz = 0; dz < 3; dz++) {
                    boolean isWall = dx == 0 || dx == 2 || dz == 0 || dz == 2;
                    if (isWall) {
                        world.setBlock(towerX + dx, y, towerZ + dz, BlockType.STONE);
                    }
                }
            }
        }
        // Tower cap
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                world.setBlock(towerX + dx, height + 6, towerZ + dz, BlockType.SLATE);
            }
        }
        // Arched doorway
        world.setBlock(x + width / 2, 1, z, BlockType.AIR);
        world.setBlock(x + width / 2, 2, z, BlockType.AIR);
        world.setBlock(x + width / 2, 3, z, BlockType.AIR);
    }

    // ==================== CAR WASH ====================

    private void generateCarWash(World world, int x, int z, int width, int depth, int height) {
        // Open-sided structure — corrugated metal roof on pillars
        // Concrete floor
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, 0, z + dz, BlockType.CONCRETE);
            }
        }
        // Corner pillars
        for (int y = 1; y <= height; y++) {
            world.setBlock(x, y, z, BlockType.STONE);
            world.setBlock(x + width - 1, y, z, BlockType.STONE);
            world.setBlock(x, y, z + depth - 1, BlockType.STONE);
            world.setBlock(x + width - 1, y, z + depth - 1, BlockType.STONE);
        }
        // Corrugated metal roof
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.CORRUGATED_METAL);
            }
        }
        // Back wall
        for (int dx = 0; dx < width; dx++) {
            for (int y = 1; y <= height; y++) {
                world.setBlock(x + dx, y, z + depth - 1, BlockType.CONCRETE);
            }
        }
        // Sign
        for (int dx = 0; dx < width; dx++) {
            world.setBlock(x + dx, height, z, BlockType.SIGN_BLUE);
        }
    }

    // ==================== COUNCIL FLATS ====================

    private void generateCouncilFlats(World world, int x, int z, int width, int depth, int height) {
        // Brutalist concrete tower block with pebbledash
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (isWall) {
                        // Windows every other row, concrete pillars at corners
                        boolean isCorner = (dx == 0 || dx == width - 1) && (dz == 0 || dz == depth - 1);
                        if (isCorner) {
                            world.setBlock(x + dx, y, z + dz, BlockType.CONCRETE);
                        } else if (y % 3 == 2) {
                            world.setBlock(x + dx, y, z + dz, BlockType.GLASS);
                        } else {
                            world.setBlock(x + dx, y, z + dz, BlockType.PEBBLEDASH);
                        }
                    }
                    // Floor slabs every 3 blocks
                    if (!isWall && y % 3 == 0) {
                        world.setBlock(x + dx, y, z + dz, BlockType.CONCRETE);
                    }
                }
            }
        }
        // Flat roof
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.CONCRETE);
            }
        }
        // Ground floor — concrete
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, 0, z + dz, BlockType.CONCRETE);
            }
        }
        // Front door
        world.setBlock(x + width / 2, 1, z, BlockType.AIR);
        world.setBlock(x + width / 2, 2, z, BlockType.AIR);
    }

    // ==================== PETROL STATION ====================

    private void generatePetrolStation(World world, int x, int z, int width, int depth, int height) {
        // Forecourt — tarmac surface
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, 0, z + dz, BlockType.TARMAC);
            }
        }
        // Small kiosk building at the back
        int kioskW = 6;
        int kioskD = 4;
        int kioskX = x + width / 2 - kioskW / 2;
        int kioskZ = z + depth - kioskD;
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < kioskW; dx++) {
                for (int dz = 0; dz < kioskD; dz++) {
                    boolean isWall = dx == 0 || dx == kioskW - 1 || dz == 0 || dz == kioskD - 1;
                    if (isWall) {
                        if (y <= 2 && dz == 0) {
                            world.setBlock(kioskX + dx, y, kioskZ + dz, BlockType.GLASS);
                        } else {
                            world.setBlock(kioskX + dx, y, kioskZ + dz, BlockType.RENDER_WHITE);
                        }
                    }
                }
            }
        }
        // Kiosk roof
        for (int dx = 0; dx < kioskW; dx++) {
            for (int dz = 0; dz < kioskD; dz++) {
                world.setBlock(kioskX + dx, height + 1, kioskZ + dz, BlockType.CONCRETE);
            }
        }
        // Canopy over pumps — corrugated metal on pillars
        int canopyZ = z + 1;
        for (int dx = 2; dx < width - 2; dx++) {
            world.setBlock(x + dx, height + 1, canopyZ, BlockType.CORRUGATED_METAL);
            world.setBlock(x + dx, height + 1, canopyZ + 2, BlockType.CORRUGATED_METAL);
            world.setBlock(x + dx, height + 1, canopyZ + 1, BlockType.CORRUGATED_METAL);
        }
        // Canopy pillars
        for (int y = 1; y <= height; y++) {
            world.setBlock(x + 2, y, canopyZ, BlockType.STONE);
            world.setBlock(x + width - 3, y, canopyZ, BlockType.STONE);
            world.setBlock(x + 2, y, canopyZ + 2, BlockType.STONE);
            world.setBlock(x + width - 3, y, canopyZ + 2, BlockType.STONE);
        }
        // Sign
        for (int dx = 0; dx < kioskW; dx++) {
            world.setBlock(kioskX + dx, height, kioskZ, BlockType.SIGN_GREEN);
        }
        // Door
        world.setBlock(kioskX + kioskW / 2, 1, kioskZ, BlockType.AIR);
        world.setBlock(kioskX + kioskW / 2, 2, kioskZ, BlockType.AIR);
    }

    // ==================== WETHERSPOONS ====================

    private void generateWetherspoons(World world, int x, int z, int width, int depth, int height) {
        // Large pub — yellow brick exterior with big windows, slate roof
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (isWall) {
                        if (y <= 3 && dz == 0 && dx > 1 && dx < width - 2) {
                            // Large front windows
                            world.setBlock(x + dx, y, z + dz, BlockType.GLASS);
                        } else {
                            world.setBlock(x + dx, y, z + dz, BlockType.YELLOW_BRICK);
                        }
                    }
                }
            }
        }
        // Slate roof
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.SLATE);
                world.setBlock(x + dx, 0, z + dz, BlockType.PAVEMENT);
            }
        }
        // Sign strip
        for (int dx = 0; dx < width; dx++) {
            world.setBlock(x + dx, height, z, BlockType.SIGN_BLUE);
        }
        // Double doors
        world.setBlock(x + width / 2, 1, z, BlockType.AIR);
        world.setBlock(x + width / 2 + 1, 1, z, BlockType.AIR);
        world.setBlock(x + width / 2, 2, z, BlockType.AIR);
        world.setBlock(x + width / 2 + 1, 2, z, BlockType.AIR);
    }

    // ==================== LIBRARY ====================

    private void generateLibrary(World world, int x, int z, int width, int depth, int height) {
        // Civic building — concrete and glass, modern style
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (isWall) {
                        boolean isCorner = (dx == 0 || dx == width - 1) && (dz == 0 || dz == depth - 1);
                        if (isCorner) {
                            world.setBlock(x + dx, y, z + dz, BlockType.CONCRETE);
                        } else if (y >= 2 && y <= height - 1) {
                            world.setBlock(x + dx, y, z + dz, BlockType.GLASS);
                        } else {
                            world.setBlock(x + dx, y, z + dz, BlockType.CONCRETE);
                        }
                    }
                }
            }
        }
        // Flat concrete roof
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.CONCRETE);
                world.setBlock(x + dx, 0, z + dz, BlockType.CONCRETE);
            }
        }
        // Blue sign
        for (int dx = 0; dx < width; dx++) {
            world.setBlock(x + dx, height, z, BlockType.SIGN_BLUE);
        }
        // Glass double doors
        world.setBlock(x + width / 2, 1, z, BlockType.AIR);
        world.setBlock(x + width / 2 + 1, 1, z, BlockType.AIR);
        world.setBlock(x + width / 2, 2, z, BlockType.AIR);
        world.setBlock(x + width / 2 + 1, 2, z, BlockType.AIR);
    }

    // ==================== FIRE STATION ====================

    private void generateFireStation(World world, int x, int z, int width, int depth, int height) {
        // Red metal and brick, with tall roller doors
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (isWall) {
                        if (dz == 0 && y <= 4 && (dx >= 1 && dx <= 5)) {
                            // Bay 1 roller door
                            world.setBlock(x + dx, y, z + dz, BlockType.AIR);
                        } else if (dz == 0 && y <= 4 && (dx >= 8 && dx <= 12)) {
                            // Bay 2 roller door
                            world.setBlock(x + dx, y, z + dz, BlockType.AIR);
                        } else {
                            world.setBlock(x + dx, y, z + dz, BlockType.METAL_RED);
                        }
                    }
                }
            }
        }
        // Concrete floor and flat roof
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.CONCRETE);
                world.setBlock(x + dx, 0, z + dz, BlockType.CONCRETE);
            }
        }
        // Sign
        for (int dx = 0; dx < width; dx++) {
            world.setBlock(x + dx, height, z, BlockType.SIGN_RED);
        }
        // Drill yard (tarmac forecourt)
        for (int dx = 0; dx < width; dx++) {
            for (int dz = -5; dz < 0; dz++) {
                world.setBlock(x + dx, 0, z + dz, BlockType.TARMAC);
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

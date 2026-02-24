package ragamuffin.world;

import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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

    // Terrain height parameters
    private static final int BASE_HEIGHT = 0;  // Sea level / flat area height
    private static final int MAX_TERRAIN_HEIGHT = 8; // Maximum hill height above base

    private final Random random;
    private final long seed;

    // Heightmap cache for performance — maps (x,z) to terrain height
    private final Map<Long, Integer> heightCache = new HashMap<>();

    // Flat zones — areas that must stay at BASE_HEIGHT (buildings, roads)
    private final Set<Long> flatZones = new HashSet<>();

    // Near-building zones — areas within BUILDING_BLEND_RADIUS blocks of a flat zone.
    // Value is the Chebyshev distance to the nearest flat zone edge (1 = adjacent).
    private final Map<Long, Integer> nearBuildingZones = new HashMap<>();

    // Number of blocks beyond the flat zone over which terrain blends back to natural height.
    private static final int BUILDING_BLEND_RADIUS = 8;

    public WorldGenerator(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    /**
     * Get terrain height at a given (x, z) position.
     * Uses value noise with multiple octaves for natural-looking hills.
     * Areas marked as flat zones return BASE_HEIGHT.
     * Areas near buildings are blended smoothly from BASE_HEIGHT to natural height.
     */
    public int getTerrainHeight(int x, int z) {
        long key = packCoord(x, z);

        // Check flat zone first
        if (flatZones.contains(key)) {
            return BASE_HEIGHT;
        }

        Integer cached = heightCache.get(key);
        if (cached != null) {
            return cached;
        }

        // Multi-octave value noise for natural terrain
        float height = 0;
        height += valueNoise(x * 0.015f, z * 0.015f) * 5.0f;  // Large rolling hills
        height += valueNoise(x * 0.04f, z * 0.04f) * 2.5f;     // Medium bumps
        height += valueNoise(x * 0.1f, z * 0.1f) * 1.0f;       // Small detail

        // Distance from origin — flatten toward centre where the town is
        float distFromCentre = (float) Math.sqrt(x * x + z * z);
        float flatteningFactor = Math.min(1.0f, distFromCentre / 80.0f); // Fully flat within 80 blocks
        height *= flatteningFactor;

        // Blend terrain down near buildings for smooth visual integration
        Integer nearDist = nearBuildingZones.get(key);
        if (nearDist != null) {
            // Scale height linearly from 0 (at flat zone edge) to natural (at BUILDING_BLEND_RADIUS)
            float blendFactor = (float) nearDist / BUILDING_BLEND_RADIUS;
            height *= blendFactor;
        }

        int terrainY = BASE_HEIGHT + Math.max(0, Math.round(height));
        terrainY = Math.min(terrainY, BASE_HEIGHT + MAX_TERRAIN_HEIGHT);

        heightCache.put(key, terrainY);
        return terrainY;
    }

    /**
     * Simple value noise function (deterministic from seed).
     */
    private float valueNoise(float x, float z) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        float fx = x - ix;
        float fz = z - iz;

        // Smooth interpolation curve
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);

        float v00 = hashFloat(ix, iz);
        float v10 = hashFloat(ix + 1, iz);
        float v01 = hashFloat(ix, iz + 1);
        float v11 = hashFloat(ix + 1, iz + 1);

        float v0 = v00 + fx * (v10 - v00);
        float v1 = v01 + fx * (v11 - v01);
        return v0 + fz * (v1 - v0);
    }

    /**
     * Deterministic hash function that returns a float in [-1, 1].
     */
    private float hashFloat(int x, int z) {
        long h = seed * 6364136223846793005L + 1442695040888963407L;
        h ^= x * 2654435761L;
        h ^= z * 2246822519L;
        h ^= (h >>> 16);
        h *= 2246822519L;
        h ^= (h >>> 13);
        return ((h & 0xFFFFFFL) / (float) 0xFFFFFFL) * 2.0f - 1.0f;
    }

    /**
     * Mark a rectangular area as a flat zone (must stay at BASE_HEIGHT).
     * Also marks a wider transition zone around the building where terrain
     * height is gradually blended back to natural height.
     */
    private void markFlatZone(int x, int z, int width, int depth) {
        // Core flat zone: building footprint plus a 2-block hard margin
        for (int dx = -2; dx < width + 2; dx++) {
            for (int dz = -2; dz < depth + 2; dz++) {
                flatZones.add(packCoord(x + dx, z + dz));
            }
        }

        // Transition zone: BUILDING_BLEND_RADIUS blocks beyond the hard margin,
        // terrain height is scaled by (dist / BUILDING_BLEND_RADIUS) so it rises
        // smoothly from zero at the building edge to natural height at the limit.
        int outerMargin = 2 + BUILDING_BLEND_RADIUS;
        for (int dx = -outerMargin; dx < width + outerMargin; dx++) {
            for (int dz = -outerMargin; dz < depth + outerMargin; dz++) {
                long key = packCoord(x + dx, z + dz);
                if (!flatZones.contains(key)) {
                    // Chebyshev distance to the nearest hard-flat boundary
                    int distX = Math.max(0, Math.max(-dx - 2, dx - (width + 1)));
                    int distZ = Math.max(0, Math.max(-dz - 2, dz - (depth + 1)));
                    int dist = Math.max(distX, distZ); // Chebyshev distance
                    if (dist > 0 && dist <= BUILDING_BLEND_RADIUS) {
                        // Keep the minimum distance (closest building wins)
                        Integer existing = nearBuildingZones.get(key);
                        if (existing == null || dist < existing) {
                            nearBuildingZones.put(key, dist);
                        }
                    }
                }
            }
        }
    }

    private long packCoord(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * Pre-mark all areas that must remain flat (buildings, roads, park, streets).
     */
    private void markAllFlatZones() {
        int halfWorld = WORLD_SIZE / 2;

        // Park area
        int parkStart = -PARK_SIZE / 2;
        markFlatZone(parkStart, parkStart, PARK_SIZE, PARK_SIZE);

        // Streets — horizontal and vertical every 20 blocks (built-up area only)
        int streetSpacing = 20;
        int streetExtent = 170;
        for (int z = -streetExtent; z <= streetExtent; z += streetSpacing) {
            for (int x = -streetExtent; x <= streetExtent; x++) {
                for (int w = 0; w < STREET_WIDTH; w++) {
                    flatZones.add(packCoord(x, z + w));
                }
            }
        }
        for (int x = -streetExtent; x <= streetExtent; x += streetSpacing) {
            for (int z = -streetExtent; z <= streetExtent; z++) {
                for (int w = 0; w < STREET_WIDTH; w++) {
                    flatZones.add(packCoord(x + w, z));
                }
            }
        }

        // High street buildings — south side
        markFlatZone(20, 25, 7, 8);   // Greggs
        markFlatZone(28, 25, 6, 8);   // Off-licence
        markFlatZone(35, 25, 7, 8);   // Charity shop
        markFlatZone(43, 25, 6, 8);   // Jeweller
        markFlatZone(50, 25, 7, 8);   // Bookies
        markFlatZone(58, 25, 7, 8);   // Kebab shop

        // High street — north side
        markFlatZone(20, 8, 7, 8);    // Tesco Express
        markFlatZone(28, 8, 8, 8);    // Launderette
        markFlatZone(37, 8, 8, 8);    // Pub
        markFlatZone(46, 8, 7, 8);    // Pawn shop
        markFlatZone(54, 8, 8, 8);    // Builders merchant

        // Office building
        markFlatZone(70, 20, 15, 15);

        // JobCentre
        markFlatZone(-60, 25, 12, 12);

        // Terraced rows
        markFlatZone(-70, -25, 80, 8);
        markFlatZone(-70, -41, 80, 8);
        markFlatZone(-70, -55, 80, 8);
        markFlatZone(20, -25, 64, 8);
        markFlatZone(-70, 30, 64, 8);
        markFlatZone(-70, 46, 64, 8);

        // Industrial estate
        markFlatZone(60, -40, 20, 15);
        markFlatZone(60, -60, 18, 12);
        markFlatZone(82, -40, 16, 14);
        markFlatZone(82, -58, 14, 12);

        // Extended shops
        markFlatZone(66, 25, 7, 8);   // Chippy
        markFlatZone(63, 8, 7, 8);    // Newsagent
        markFlatZone(-60, 10, 14, 10); // GP Surgery
        markFlatZone(60, -80, 36, 16); // Primary school + playground

        // Community centre, church, taxi rank, car wash
        markFlatZone(-90, -25, 18, 14);
        markFlatZone(30, -50, 12, 18);
        markFlatZone(74, 20, 6, 11);
        markFlatZone(100, -40, 10, 8);

        // Council flats
        markFlatZone(-95, 50, 12, 12);
        markFlatZone(-110, 50, 12, 12);

        // Petrol station
        markFlatZone(100, 20, 14, 10);

        // Extended high street
        markFlatZone(90, 25, 8, 10);   // Nando's
        markFlatZone(99, 25, 6, 8);    // Barber
        markFlatZone(106, 25, 7, 8);   // Nail salon
        markFlatZone(72, 8, 7, 8);     // Corner shop
        markFlatZone(80, 8, 7, 8);     // Betting shop
        markFlatZone(88, 8, 6, 8);     // Phone repair
        markFlatZone(95, 8, 8, 8);     // Cash converter

        // Wetherspoons, library, fire station
        markFlatZone(115, 25, 16, 14);
        markFlatZone(-80, 10, 16, 12);
        markFlatZone(100, -65, 16, 14);

        // Additional terraced rows
        markFlatZone(-110, -25, 40, 8);
        markFlatZone(-110, -41, 40, 8);
        markFlatZone(-110, 30, 40, 8);
        markFlatZone(90, -25, 32, 8);
        markFlatZone(90, -41, 32, 8);
        markFlatZone(-150, -25, 40, 8);
        markFlatZone(-150, -41, 40, 8);
        markFlatZone(-150, 30, 40, 8);
        markFlatZone(-150, 46, 40, 8);
        markFlatZone(130, -25, 40, 8);
        markFlatZone(130, -41, 40, 8);
        markFlatZone(130, 30, 32, 8);
        markFlatZone(-150, -60, 40, 8);
        markFlatZone(-150, -76, 40, 8);

        // Additional council flats
        markFlatZone(-150, 60, 12, 12);
        markFlatZone(140, -70, 12, 12);

        // Allotments (south-east, between residential and industrial)
        markFlatZone(60, -100, 30, 20);

        // Canal (runs east-west along southern edge of town)
        markFlatZone(-120, -90, 240, 8);

        // Skate park (near park, south-west)
        markFlatZone(-50, -65, 18, 14);

        // Cemetery (near church, south-east)
        markFlatZone(45, -72, 20, 18);
    }

    /**
     * Generate the entire world - landmarks and structure.
     */
    public void generateWorld(World world) {
        // Clear random state
        random.setSeed(seed);

        // Mark flat zones BEFORE generating terrain, so buildings sit on flat ground
        markAllFlatZones();

        // Fill entire world with terrain using heightmap
        // Deep terrain: bedrock at y=-6, stone from y=-5 to y=-1, dirt/grass on surface
        int halfWorld = WORLD_SIZE / 2;
        for (int x = -halfWorld; x < halfWorld; x++) {
            for (int z = -halfWorld; z < halfWorld; z++) {
                int terrainHeight = getTerrainHeight(x, z);
                // Bedrock (indestructible bottom)
                world.setBlock(x, -6, z, BlockType.BEDROCK);
                // Stone layers
                for (int y = -5; y <= -1; y++) {
                    world.setBlock(x, y, z, BlockType.STONE);
                }
                // Fill dirt from y=0 up to terrainHeight-1, grass on top
                for (int y = 0; y < terrainHeight; y++) {
                    world.setBlock(x, y, z, BlockType.DIRT);
                }
                world.setBlock(x, terrainHeight, z, BlockType.GRASS); // Surface layer
            }
        }

        // Generate the park at the center (will overwrite base layer)
        generatePark(world);

        // Generate dense street grid
        generateStreets(world);

        // ===== HIGH STREET (along positive X from park, z=20 street) =====
        // Shop type assignments are shuffled per seed for layout variety.
        generateHighStreet(world);

        // ===== OFFICE BUILDING (tall, near high street) =====
        generateOfficeBuilding(world, 70, 20, 15, 15, 12);
        world.addLandmark(new Landmark(LandmarkType.OFFICE_BUILDING, 70, 0, 20, 15, 13, 15)); // roof at y=13

        // ===== JOBCENTRE (west of park) =====
        generateJobCentre(world, -60, 25, 12, 12, 5);
        world.addLandmark(new Landmark(LandmarkType.JOB_CENTRE, -60, 0, 25, 12, 6, 12)); // roof at y=6

        // ===== TERRACED HOUSES — multiple rows =====
        // Heights vary per seed for layout variety (base 6, varies by ±1).
        Random rowRng = new Random(seed ^ 0xBEEF1234L);
        int[] rowHeights = {
            5 + rowRng.nextInt(3),  // Row 1: 5-7
            5 + rowRng.nextInt(3),  // Row 2: 5-7
            5 + rowRng.nextInt(3),  // Row 3: 5-7
            5 + rowRng.nextInt(3),  // Row 4: 5-7
            5 + rowRng.nextInt(3),  // Row 5: 5-7
            5 + rowRng.nextInt(3),  // Row 6: 5-7
        };
        // Row 1: south of park, south side
        generateTerracedRow(world, -70, -25, 8, 8, rowHeights[0], 10);
        // Row 2: south of park, north side (across the street)
        generateTerracedRow(world, -70, -41, 8, 8, rowHeights[1], 10);
        // Row 3: further south
        generateTerracedRow(world, -70, -55, 8, 8, rowHeights[2], 10);
        // Row 4: east of park
        generateTerracedRow(world, 20, -25, 8, 8, rowHeights[3], 8);
        // Row 5: west residential area
        generateTerracedRow(world, -70, 30, 8, 8, rowHeights[4], 8);
        // Row 6: another row facing opposite way
        generateTerracedRow(world, -70, 46, 8, 8, rowHeights[5], 8);

        // Garden walls between terraced rows
        generateGardenWalls(world, -70, -33, 80, 1);
        generateGardenWalls(world, -70, -49, 80, 1);
        generateGardenWalls(world, -70, 38, 64, 1);

        // ===== INDUSTRIAL ESTATE (northeast corner) =====
        // Warehouse heights vary per seed.
        Random warehouseRng = new Random(seed ^ 0xCAFE5678L);
        int wh1 = 7 + warehouseRng.nextInt(3); // 7-9
        int wh2 = 6 + warehouseRng.nextInt(3); // 6-8
        int wh3 = 7 + warehouseRng.nextInt(3); // 7-9
        int wh4 = 6 + warehouseRng.nextInt(3); // 6-8
        generateWarehouse(world, 60, -40, 20, 15, wh1);
        world.addLandmark(new Landmark(LandmarkType.WAREHOUSE, 60, 0, -40, 20, wh1 + 1, 15));
        generateWarehouse(world, 60, -60, 18, 12, wh2);
        generateWarehouse(world, 82, -40, 16, 14, wh3);
        generateWarehouse(world, 82, -58, 14, 12, wh4);
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
        world.addLandmark(new Landmark(LandmarkType.GP_SURGERY, -60, 0, 10, 14, 6, 10)); // roof at y=6

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
        world.addLandmark(new Landmark(LandmarkType.PRIMARY_SCHOOL, 60, 0, -80, 20, 7, 16)); // roof at y=7

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
        world.addLandmark(new Landmark(LandmarkType.COMMUNITY_CENTRE, -90, 0, -25, 18, 6, 14)); // roof at y=6

        // ===== CHURCH (northeast residential area) =====
        generateChurch(world, 30, -50, 12, 18, 10);
        world.addLandmark(new Landmark(LandmarkType.CHURCH, 30, 0, -50, 12, 17, 18)); // bell tower cap at y=16

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
        world.addLandmark(new Landmark(LandmarkType.TAXI_RANK, 74, 0, 25, 6, 4, 6)); // roof at y=4

        // ===== CAR WASH (near industrial estate) =====
        generateCarWash(world, 100, -40, 10, 8, 5);
        world.addLandmark(new Landmark(LandmarkType.CAR_WASH, 100, 0, -40, 10, 6, 8)); // roof at y=6

        // ===== COUNCIL FLATS — tower block (west side) =====
        generateCouncilFlats(world, -95, 50, 12, 12, 18);
        world.addLandmark(new Landmark(LandmarkType.COUNCIL_FLATS, -95, 0, 50, 12, 19, 12)); // roof at y=19

        // ===== SECOND TOWER BLOCK (further west) =====
        generateCouncilFlats(world, -110, 50, 12, 12, 15);
        world.addLandmark(new Landmark(LandmarkType.COUNCIL_FLATS, -110, 0, 50, 12, 16, 12)); // roof at y=16

        // ===== PETROL STATION (east side) =====
        generatePetrolStation(world, 100, 20, 14, 10, 4);
        world.addLandmark(new Landmark(LandmarkType.PETROL_STATION, 100, 0, 20, 14, 5, 10)); // roof/canopy at y=5

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
        world.addLandmark(new Landmark(LandmarkType.WETHERSPOONS, 115, 0, 25, 16, 7, 14)); // roof at y=7

        // ===== LIBRARY — west side civic area =====
        generateLibrary(world, -80, 10, 16, 12, 6);
        world.addLandmark(new Landmark(LandmarkType.LIBRARY, -80, 0, 10, 16, 7, 12)); // roof at y=7

        // ===== FIRE STATION — east side, near industrial estate =====
        generateFireStation(world, 100, -65, 16, 14, 7);
        world.addLandmark(new Landmark(LandmarkType.FIRE_STATION, 100, 0, -65, 16, 8, 14)); // roof at y=8

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
        world.addLandmark(new Landmark(LandmarkType.COUNCIL_FLATS, -150, 0, 60, 12, 15, 12)); // roof at y=15
        generateCouncilFlats(world, 140, -70, 12, 12, 16);

        // ===== ALLOTMENTS (south-east) =====
        generateAllotments(world, 60, -100, 30, 20);
        world.addLandmark(new Landmark(LandmarkType.ALLOTMENTS, 60, 0, -100, 30, 3, 20));

        // ===== CANAL (east-west along southern edge) =====
        generateCanal(world, -120, -90, 240, 8);
        world.addLandmark(new Landmark(LandmarkType.CANAL, -120, 0, -90, 240, 1, 8));

        // ===== SKATE PARK (south-west, near park) =====
        generateSkatePark(world, -50, -65, 18, 14);
        world.addLandmark(new Landmark(LandmarkType.SKATE_PARK, -50, 0, -65, 18, 3, 14));

        // ===== CEMETERY (near church, south-east) =====
        generateCemetery(world, 45, -72, 20, 18);
        world.addLandmark(new Landmark(LandmarkType.CEMETERY, 45, 0, -72, 20, 3, 18));

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

        // ===== BUILDING INTERIORS =====
        generateBuildingInteriors(world);

        // ===== STREET FURNITURE =====
        generateStreetFurniture(world);

        // ===== PARK FURNITURE =====
        generateParkFurniture(world);

        // ===== SCATTERED TREES on hills outside town =====
        generateOutskirtsVegetation(world);

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
                    int terrainHeight = getTerrainHeight(worldX, worldZ);

                    // Bedrock at y=-6
                    int bedrockLocalY = -6 - startY;
                    if (bedrockLocalY >= 0 && bedrockLocalY < Chunk.HEIGHT) {
                        chunk.setBlock(localX, bedrockLocalY, localZ, BlockType.BEDROCK);
                    }

                    // Stone from y=-5 to y=-1
                    for (int y = -5; y <= -1; y++) {
                        int localY = y - startY;
                        if (localY >= 0 && localY < Chunk.HEIGHT) {
                            chunk.setBlock(localX, localY, localZ, BlockType.STONE);
                        }
                    }

                    // Fill dirt from y=0 to terrainHeight-1, grass on top
                    for (int y = 0; y < terrainHeight; y++) {
                        int localY = y - startY;
                        if (localY >= 0 && localY < Chunk.HEIGHT) {
                            chunk.setBlock(localX, localY, localZ, BlockType.DIRT);
                        }
                    }
                    int grassLocalY = terrainHeight - startY;
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
        // Only generate streets within the built-up area, not the whole world
        int streetExtent = 170; // Streets cover -170 to +170 (where buildings are)
        int streetSpacing = 20;

        // Horizontal streets
        for (int z = -streetExtent; z <= streetExtent; z += streetSpacing) {
            for (int x = -streetExtent; x <= streetExtent; x++) {
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
        for (int x = -streetExtent; x <= streetExtent; x += streetSpacing) {
            for (int z = -streetExtent; z <= streetExtent; z++) {
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

    // ==================== HIGH STREET ====================

    /**
     * Generate the high street with seed-shuffled shop assignments.
     * Building footprints and positions are fixed (required for terrain/collision tests),
     * but which shop occupies each slot varies by seed to increase replayability.
     */
    private void generateHighStreet(World world) {
        Random layoutRng = new Random(seed ^ 0xDEADBEEFL);

        // --- South side slots (z=25) ---
        // Each slot: {x, width, depth}
        int[][] southSlots = {
            {20, 7, 8},
            {28, 6, 8},
            {35, 7, 8},
            {43, 6, 8},
            {50, 7, 8},
            {58, 7, 8},
        };
        // Shop assignments for south side (shuffled per seed)
        List<LandmarkType> southShops = new ArrayList<>(Arrays.asList(
            LandmarkType.GREGGS,
            LandmarkType.OFF_LICENCE,
            LandmarkType.CHARITY_SHOP,
            LandmarkType.JEWELLER,
            LandmarkType.BOOKIES,
            LandmarkType.KEBAB_SHOP
        ));
        Collections.shuffle(southShops, layoutRng);

        // Wall materials and sign colours mapped to landmark type for thematic consistency
        for (int i = 0; i < southSlots.length; i++) {
            int[] slot = southSlots[i];
            LandmarkType type = southShops.get(i);
            BlockType wall = wallForShop(type);
            BlockType sign = signForShop(type);
            generateShopWithSign(world, slot[0], 25, slot[1], slot[2], 4, wall, sign, type);
        }

        // --- North side slots (z=8) ---
        int[][] northSlots = {
            {20, 7, 8},
            {28, 8, 8},
            {37, 8, 8},
            {46, 7, 8},
            {54, 8, 8},
        };
        List<LandmarkType> northShops = new ArrayList<>(Arrays.asList(
            LandmarkType.TESCO_EXPRESS,
            LandmarkType.LAUNDERETTE,
            LandmarkType.PUB,
            LandmarkType.PAWN_SHOP,
            LandmarkType.BUILDERS_MERCHANT
        ));
        Collections.shuffle(northShops, layoutRng);

        int[] northHeights = {4, 4, 5, 4, 4};
        for (int i = 0; i < northSlots.length; i++) {
            int[] slot = northSlots[i];
            LandmarkType type = northShops.get(i);
            BlockType wall = wallForShop(type);
            BlockType sign = signForShop(type);
            generateShopWithSign(world, slot[0], 8, slot[1], slot[2], northHeights[i], wall, sign, type);
        }
    }

    /** Returns the wall block type appropriate for a given shop landmark. */
    private BlockType wallForShop(LandmarkType type) {
        switch (type) {
            case JEWELLER: return BlockType.GLASS;
            case TESCO_EXPRESS: return BlockType.BRICK;
            case PUB: return BlockType.BRICK;
            case LAUNDERETTE: return BlockType.BRICK;
            default: return BlockType.BRICK;
        }
    }

    /** Returns the sign colour appropriate for a given shop landmark. */
    private BlockType signForShop(LandmarkType type) {
        switch (type) {
            case GREGGS: return BlockType.SIGN_YELLOW;
            case OFF_LICENCE: return BlockType.SIGN_RED;
            case CHARITY_SHOP: return BlockType.SIGN_GREEN;
            case JEWELLER: return BlockType.SIGN_WHITE;
            case BOOKIES: return BlockType.SIGN_GREEN;
            case KEBAB_SHOP: return BlockType.SIGN_RED;
            case TESCO_EXPRESS: return BlockType.SIGN_BLUE;
            case LAUNDERETTE: return BlockType.SIGN_WHITE;
            case PUB: return BlockType.SIGN_RED;
            case PAWN_SHOP: return BlockType.SIGN_YELLOW;
            case BUILDERS_MERCHANT: return BlockType.SIGN_YELLOW;
            default: return BlockType.SIGN_BLUE;
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

        // height + 2: walls go up to y=height inclusive, roof sits at y=height+1
        world.addLandmark(new Landmark(landmarkType, x, 0, z, width, height + 2, depth));
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
                world.setBlock(x + dx, height + 1, z + dz, BlockType.CONCRETE); // Flat office roof
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
                        world.setBlock(x + dx, y, z + dz, BlockType.CORRUGATED_METAL);
                    }
                }
            }
        }
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.CORRUGATED_METAL); // Metal warehouse roof
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

    // ==================== ALLOTMENTS ====================

    private void generateAllotments(World world, int x, int z, int width, int depth) {
        // Council allotments — fenced plots with sheds, dirt beds, and paths
        // Grass base
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, 0, z + dz, BlockType.GRASS);
            }
        }

        // Perimeter fence (wooden, 2 blocks high)
        for (int dx = 0; dx < width; dx++) {
            for (int y = 1; y <= 2; y++) {
                world.setBlock(x + dx, y, z, BlockType.WOOD_FENCE);
                world.setBlock(x + dx, y, z + depth - 1, BlockType.WOOD_FENCE);
            }
        }
        for (int dz = 0; dz < depth; dz++) {
            for (int y = 1; y <= 2; y++) {
                world.setBlock(x, y, z + dz, BlockType.WOOD_FENCE);
                world.setBlock(x + width - 1, y, z + dz, BlockType.WOOD_FENCE);
            }
        }

        // Gate entrance
        world.setBlock(x + width / 2, 1, z, BlockType.AIR);
        world.setBlock(x + width / 2, 2, z, BlockType.AIR);

        // Central path (gravel/pavement strip down the middle)
        int pathZ = z + depth / 2;
        for (int dx = 1; dx < width - 1; dx++) {
            world.setBlock(x + dx, 0, pathZ, BlockType.PAVEMENT);
        }

        // Allotment plots — alternating dirt beds and grass paths on each side
        for (int plotX = 2; plotX < width - 4; plotX += 6) {
            // North plots (above path)
            for (int dz = 2; dz < depth / 2 - 1; dz++) {
                for (int dx = plotX; dx < plotX + 4 && (x + dx) < x + width - 2; dx++) {
                    world.setBlock(x + dx, 0, z + dz, BlockType.DIRT);
                }
            }
            // South plots (below path)
            for (int dz = depth / 2 + 1; dz < depth - 2; dz++) {
                for (int dx = plotX; dx < plotX + 4 && (x + dx) < x + width - 2; dx++) {
                    world.setBlock(x + dx, 0, z + dz, BlockType.DIRT);
                }
            }
        }

        // Small wooden sheds (2x2, 2 tall) — one per side
        buildShed(world, x + 2, z + 2, 2, 2, 2);
        buildShed(world, x + width - 5, z + depth - 5, 2, 2, 2);

        // Water butt (stone block next to shed)
        world.setBlock(x + 4, 1, z + 2, BlockType.STONE);
    }

    private void buildShed(World world, int x, int z, int width, int depth, int height) {
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (isWall) {
                        world.setBlock(x + dx, y, z + dz, BlockType.WOOD_WALL);
                    }
                }
            }
        }
        // Corrugated roof
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.CORRUGATED_METAL);
            }
        }
        // Door
        world.setBlock(x, 1, z, BlockType.AIR);
    }

    // ==================== CANAL ====================

    private void generateCanal(World world, int x, int z, int width, int depth) {
        // Industrial canal running east-west: towpath, water channel, stone walls
        for (int dx = 0; dx < width; dx++) {
            int wx = x + dx;
            // South towpath (3 blocks wide, pavement)
            for (int dz = 0; dz < 2; dz++) {
                world.setBlock(wx, 0, z + dz, BlockType.PAVEMENT);
            }
            // South canal wall (stone, drops down 2 blocks)
            world.setBlock(wx, 0, z + 2, BlockType.STONE);
            world.setBlock(wx, -1, z + 2, BlockType.STONE);
            // Water channel (4 blocks wide, 2 blocks deep)
            for (int dz = 3; dz < depth - 2; dz++) {
                world.setBlock(wx, -1, z + dz, BlockType.STONE);  // Canal bed
                world.setBlock(wx, 0, z + dz, BlockType.WATER);   // Water surface
            }
            // North canal wall
            world.setBlock(wx, 0, z + depth - 2, BlockType.STONE);
            world.setBlock(wx, -1, z + depth - 2, BlockType.STONE);
            // North towpath
            world.setBlock(wx, 0, z + depth - 1, BlockType.PAVEMENT);
        }

        // Iron railings along towpath edges (every 3 blocks for visual variety)
        for (int dx = 0; dx < width; dx += 3) {
            world.setBlock(x + dx, 1, z + 2, BlockType.IRON_FENCE);
            world.setBlock(x + dx, 1, z + depth - 2, BlockType.IRON_FENCE);
        }

        // A footbridge across the canal (stone arch at midpoint)
        int bridgeX = x + width / 2;
        for (int dz = 2; dz < depth - 1; dz++) {
            world.setBlock(bridgeX, 1, z + dz, BlockType.STONE);
            world.setBlock(bridgeX + 1, 1, z + dz, BlockType.STONE);
        }
        // Bridge railings
        for (int dz = 2; dz < depth - 1; dz++) {
            world.setBlock(bridgeX, 2, z + dz, BlockType.IRON_FENCE);
            world.setBlock(bridgeX + 1, 2, z + dz, BlockType.IRON_FENCE);
        }

        // Benches along south towpath
        generateBench(world, x + 20, z);
        generateBench(world, x + 80, z);
        generateBench(world, x + 150, z);
    }

    // ==================== SKATE PARK ====================

    private void generateSkatePark(World world, int x, int z, int width, int depth) {
        // Concrete surface
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, 0, z + dz, BlockType.CONCRETE);
            }
        }

        // Low perimeter wall (1 block)
        for (int dx = 0; dx < width; dx++) {
            world.setBlock(x + dx, 1, z, BlockType.CONCRETE);
            world.setBlock(x + dx, 1, z + depth - 1, BlockType.CONCRETE);
        }
        for (int dz = 0; dz < depth; dz++) {
            world.setBlock(x, 1, z + dz, BlockType.CONCRETE);
            world.setBlock(x + width - 1, 1, z + dz, BlockType.CONCRETE);
        }

        // Entrance gap
        world.setBlock(x + width / 2, 1, z, BlockType.AIR);
        world.setBlock(x + width / 2 + 1, 1, z, BlockType.AIR);

        // Half-pipe (raised concrete walls, 3 blocks tall, at the back)
        for (int dx = 3; dx < width - 3; dx++) {
            for (int y = 1; y <= 3; y++) {
                world.setBlock(x + dx, y, z + depth - 2, BlockType.CONCRETE);
            }
        }

        // Quarter-pipe (left side, 2 blocks tall)
        for (int dz = 3; dz < depth - 4; dz++) {
            for (int y = 1; y <= 2; y++) {
                world.setBlock(x + 1, y, z + dz, BlockType.CONCRETE);
            }
        }

        // Grind rail (iron fence, centre of park)
        for (int dx = 4; dx < width - 4; dx++) {
            world.setBlock(x + dx, 1, z + depth / 2, BlockType.IRON_FENCE);
        }

        // Flat box / ledge (2 wide, raised concrete in middle area)
        for (int dx = 7; dx < 11 && dx < width - 2; dx++) {
            world.setBlock(x + dx, 1, z + 4, BlockType.CONCRETE);
            world.setBlock(x + dx, 1, z + 5, BlockType.CONCRETE);
        }

        // Bin near entrance
        generateBin(world, x + width / 2 + 3, z + 1);
    }

    // ==================== CEMETERY ====================

    private void generateCemetery(World world, int x, int z, int width, int depth) {
        // Grass base
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, 0, z + dz, BlockType.GRASS);
            }
        }

        // Iron fence perimeter (2 blocks high)
        for (int dx = 0; dx < width; dx++) {
            for (int y = 1; y <= 2; y++) {
                world.setBlock(x + dx, y, z, BlockType.IRON_FENCE);
                world.setBlock(x + dx, y, z + depth - 1, BlockType.IRON_FENCE);
            }
        }
        for (int dz = 0; dz < depth; dz++) {
            for (int y = 1; y <= 2; y++) {
                world.setBlock(x, y, z + dz, BlockType.IRON_FENCE);
                world.setBlock(x + width - 1, y, z + dz, BlockType.IRON_FENCE);
            }
        }

        // Gate entrance
        for (int y = 1; y <= 2; y++) {
            world.setBlock(x + width / 2, y, z, BlockType.AIR);
            world.setBlock(x + width / 2 + 1, y, z, BlockType.AIR);
        }

        // Central gravel path
        for (int dz = 1; dz < depth - 1; dz++) {
            world.setBlock(x + width / 2, 0, z + dz, BlockType.PAVEMENT);
            world.setBlock(x + width / 2 + 1, 0, z + dz, BlockType.PAVEMENT);
        }

        // Headstones — rows of stone blocks on each side of the path
        for (int row = 3; row < depth - 3; row += 3) {
            // West side headstones
            for (int col = 2; col < width / 2 - 1; col += 3) {
                world.setBlock(x + col, 1, z + row, BlockType.STONE);
            }
            // East side headstones
            for (int col = width / 2 + 3; col < width - 2; col += 3) {
                world.setBlock(x + col, 1, z + row, BlockType.STONE);
            }
        }

        // A few larger monuments (2 blocks tall)
        world.setBlock(x + 3, 1, z + 3, BlockType.STONE);
        world.setBlock(x + 3, 2, z + 3, BlockType.STONE);
        world.setBlock(x + width - 4, 1, z + depth - 4, BlockType.STONE);
        world.setBlock(x + width - 4, 2, z + depth - 4, BlockType.STONE);

        // Old yew tree near the back
        int treeX = x + width / 2;
        int treeZ = z + depth - 5;
        for (int y = 1; y <= 4; y++) {
            world.setBlock(treeX, y, treeZ, BlockType.TREE_TRUNK);
        }
        for (int y = 3; y <= 5; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0 && y < 5) continue;
                    world.setBlock(treeX + dx, y, treeZ + dz, BlockType.LEAVES);
                }
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
                world.setBlock(x + dx, height + 1, z + dz, BlockType.CONCRETE); // Flat shop roof
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
                world.setBlock(x + dx, height + 1, z + dz, BlockType.CONCRETE); // Flat concrete roof
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
                world.setBlock(x + dx, height + 1, z + dz, BlockType.SLATE); // Slate house roof
                world.setBlock(x + dx, 0, z + dz, BlockType.WOOD);
            }
        }
        // Front door
        world.setBlock(x + width / 2, 1, z, BlockType.AIR);
        world.setBlock(x + width / 2, 2, z, BlockType.AIR);
    }

    // ==================== BUILDING INTERIORS ====================

    private void generateBuildingInteriors(World world) {
        // GREGGS — counter with display case
        generateShopInterior(world, 20, 25, 7, 8, BlockType.LINO_GREEN, BlockType.COUNTER);

        // OFF-LICENCE — shelves along walls
        generateShopInterior(world, 28, 25, 6, 8, BlockType.LINO_GREEN, BlockType.SHELF);

        // CHARITY SHOP — cluttered shelves
        generateShopInterior(world, 35, 25, 7, 8, BlockType.CARPET, BlockType.SHELF);

        // JEWELLER — glass counter
        generateShopInterior(world, 43, 25, 6, 8, BlockType.CARPET, BlockType.COUNTER);

        // BOOKIES — counter at back
        generateShopInterior(world, 50, 25, 7, 8, BlockType.CARPET, BlockType.COUNTER);

        // KEBAB SHOP — counter
        generateShopInterior(world, 58, 25, 7, 8, BlockType.LINO_GREEN, BlockType.COUNTER);

        // TESCO EXPRESS — shelves
        generateShopInterior(world, 20, 8, 7, 8, BlockType.LINO_GREEN, BlockType.SHELF);

        // LAUNDERETTE — machines (stone blocks as washers)
        generateShopInterior(world, 28, 8, 8, 8, BlockType.LINO_GREEN, BlockType.STONE);

        // PUB — tables
        generatePubInterior(world, 37, 8, 8, 8);

        // PAWN SHOP — shelves
        generateShopInterior(world, 46, 8, 7, 8, BlockType.CARPET, BlockType.SHELF);

        // CHIPPY
        generateShopInterior(world, 66, 25, 7, 8, BlockType.LINO_GREEN, BlockType.COUNTER);

        // NEWSAGENT — shelves
        generateShopInterior(world, 63, 8, 7, 8, BlockType.LINO_GREEN, BlockType.SHELF);

        // Nando's — tables
        generatePubInterior(world, 90, 25, 8, 10);

        // Corner shop — shelves
        generateShopInterior(world, 72, 8, 7, 8, BlockType.LINO_GREEN, BlockType.SHELF);

        // WETHERSPOONS — pub interior
        generatePubInterior(world, 115, 25, 16, 14);

        // LIBRARY — bookshelves
        generateLibraryInterior(world, -80, 10, 16, 12);

        // JOBCENTRE — desks
        generateOfficeInterior(world, -60, 25, 12, 12);

        // GP SURGERY — waiting room
        generateOfficeInterior(world, -60, 10, 14, 10);
    }

    private void generateShopInterior(World world, int x, int z, int width, int depth,
                                       BlockType floorType, BlockType furnitureType) {
        // Floor
        for (int dx = 1; dx < width - 1; dx++) {
            for (int dz = 1; dz < depth - 1; dz++) {
                world.setBlock(x + dx, 0, z + dz, floorType);
            }
        }

        // Counter at the back (2 blocks from back wall)
        int counterZ = z + depth - 3;
        for (int dx = 1; dx < width - 1; dx++) {
            world.setBlock(x + dx, 1, counterZ, furnitureType);
        }

        // Shelves along side walls (every other block for variety)
        for (int dz = 2; dz < depth - 3; dz += 2) {
            world.setBlock(x + 1, 1, z + dz, BlockType.SHELF);
            world.setBlock(x + 1, 2, z + dz, BlockType.SHELF);
            world.setBlock(x + width - 2, 1, z + dz, BlockType.SHELF);
            world.setBlock(x + width - 2, 2, z + dz, BlockType.SHELF);
        }
    }

    private void generatePubInterior(World world, int x, int z, int width, int depth) {
        // Carpet floor
        for (int dx = 1; dx < width - 1; dx++) {
            for (int dz = 1; dz < depth - 1; dz++) {
                world.setBlock(x + dx, 0, z + dz, BlockType.CARPET);
            }
        }

        // Bar counter along back wall
        int barZ = z + depth - 3;
        for (int dx = 1; dx < width - 1; dx++) {
            world.setBlock(x + dx, 1, barZ, BlockType.COUNTER);
        }
        // Shelves behind bar
        for (int dx = 1; dx < width - 1; dx++) {
            world.setBlock(x + dx, 1, z + depth - 2, BlockType.SHELF);
            world.setBlock(x + dx, 2, z + depth - 2, BlockType.SHELF);
        }

        // Tables scattered in the middle
        for (int dx = 3; dx < width - 3; dx += 4) {
            for (int dz = 2; dz < depth - 4; dz += 4) {
                world.setBlock(x + dx, 1, z + dz, BlockType.TABLE);
            }
        }
    }

    private void generateLibraryInterior(World world, int x, int z, int width, int depth) {
        // Floor
        for (int dx = 1; dx < width - 1; dx++) {
            for (int dz = 1; dz < depth - 1; dz++) {
                world.setBlock(x + dx, 0, z + dz, BlockType.CARPET);
            }
        }

        // Rows of bookshelves
        for (int dx = 2; dx < width - 2; dx += 3) {
            for (int dz = 2; dz < depth - 2; dz++) {
                world.setBlock(x + dx, 1, z + dz, BlockType.BOOKSHELF);
                world.setBlock(x + dx, 2, z + dz, BlockType.BOOKSHELF);
            }
        }

        // Front desk
        world.setBlock(x + width / 2, 1, z + 1, BlockType.COUNTER);
        world.setBlock(x + width / 2 + 1, 1, z + 1, BlockType.COUNTER);
    }

    private void generateOfficeInterior(World world, int x, int z, int width, int depth) {
        // Lino floor
        for (int dx = 1; dx < width - 1; dx++) {
            for (int dz = 1; dz < depth - 1; dz++) {
                world.setBlock(x + dx, 0, z + dz, BlockType.LINO_GREEN);
            }
        }

        // Rows of desks
        for (int dx = 2; dx < width - 2; dx += 3) {
            for (int dz = 3; dz < depth - 2; dz += 3) {
                world.setBlock(x + dx, 1, z + dz, BlockType.TABLE);
            }
        }

        // Reception counter at front
        for (int dx = 2; dx < width - 2; dx++) {
            world.setBlock(x + dx, 1, z + 1, BlockType.COUNTER);
        }
    }

    // ==================== STREET FURNITURE ====================

    private void generateStreetFurniture(World world) {
        // Lamp posts along the main high street (every 10 blocks on pavement)
        for (int x = 20; x < 130; x += 10) {
            generateLampPost(world, x, 24); // South pavement
            generateLampPost(world, x, 13); // North pavement
        }

        // Bins outside shops
        generateBin(world, 22, 24);
        generateBin(world, 36, 24);
        generateBin(world, 52, 24);
        generateBin(world, 70, 24);
        generateBin(world, 95, 24);
        generateBin(world, 115, 24);

        // Benches along high street
        generateBench(world, 30, 13);
        generateBench(world, 50, 13);
        generateBench(world, 75, 13);
        generateBench(world, 100, 13);

        // Bus shelter near the taxi rank area
        generateBusShelter(world, 85, 24);

        // Bollards at street junctions
        for (int z = 20; z <= 23; z++) {
            generateBollard(world, 19, z);
        }
        for (int z = 20; z <= 23; z++) {
            generateBollard(world, 65, z);
        }

        // Phone box near the park entrance
        generatePhoneBox(world, 16, -16);

        // Post box outside newsagent
        generatePostBox(world, 64, 7);
    }

    private void generateParkFurniture(World world) {
        int parkStart = -PARK_SIZE / 2;

        // Park benches
        generateBench(world, parkStart + 5, parkStart + 5);
        generateBench(world, parkStart + 5, parkStart + PARK_SIZE - 7);
        generateBench(world, parkStart + PARK_SIZE - 7, parkStart + 5);
        generateBench(world, parkStart + PARK_SIZE - 7, parkStart + PARK_SIZE - 7);

        // Park bins
        generateBin(world, parkStart + 3, parkStart + 3);
        generateBin(world, parkStart + PARK_SIZE - 5, parkStart + PARK_SIZE - 5);
    }

    private void generateLampPost(World world, int x, int z) {
        // 4-block tall iron pole with a stone cap
        for (int y = 1; y <= 4; y++) {
            world.setBlock(x, y, z, BlockType.IRON_FENCE);
        }
        world.setBlock(x, 5, z, BlockType.SIGN_YELLOW); // Light at top
    }

    private void generateBin(World world, int x, int z) {
        // Single block bin — dark stone
        world.setBlock(x, 1, z, BlockType.STONE);
    }

    private void generateBench(World world, int x, int z) {
        // 3-block wide wooden bench, 1 block tall
        world.setBlock(x, 1, z, BlockType.WOOD);
        world.setBlock(x + 1, 1, z, BlockType.WOOD);
        world.setBlock(x + 2, 1, z, BlockType.WOOD);
    }

    private void generateBusShelter(World world, int x, int z) {
        // Glass and metal shelter: 4 wide, 2 deep, 3 tall
        // Back wall (glass)
        for (int dx = 0; dx < 4; dx++) {
            for (int y = 1; y <= 3; y++) {
                world.setBlock(x + dx, y, z + 1, BlockType.GLASS);
            }
        }
        // Side walls
        for (int y = 1; y <= 3; y++) {
            world.setBlock(x, y, z, BlockType.GLASS);
            world.setBlock(x + 3, y, z, BlockType.GLASS);
        }
        // Roof
        for (int dx = 0; dx < 4; dx++) {
            world.setBlock(x + dx, 4, z, BlockType.CORRUGATED_METAL);
            world.setBlock(x + dx, 4, z + 1, BlockType.CORRUGATED_METAL);
        }
        // Bench inside
        world.setBlock(x + 1, 1, z + 1, BlockType.WOOD);
        world.setBlock(x + 2, 1, z + 1, BlockType.WOOD);
        // Concrete floor
        for (int dx = 0; dx < 4; dx++) {
            world.setBlock(x + dx, 0, z, BlockType.CONCRETE);
            world.setBlock(x + dx, 0, z + 1, BlockType.CONCRETE);
        }
    }

    private void generateBollard(World world, int x, int z) {
        world.setBlock(x, 1, z, BlockType.CONCRETE);
    }

    private void generatePhoneBox(World world, int x, int z) {
        // Classic red phone box: 1x1, 3 tall, red metal with glass
        world.setBlock(x, 1, z, BlockType.METAL_RED);
        world.setBlock(x, 2, z, BlockType.GLASS);
        world.setBlock(x, 3, z, BlockType.METAL_RED);
    }

    private void generatePostBox(World world, int x, int z) {
        // Royal Mail post box: 1x1, 2 tall, red metal
        world.setBlock(x, 1, z, BlockType.METAL_RED);
        world.setBlock(x, 2, z, BlockType.METAL_RED);
    }

    // ==================== OUTSKIRTS VEGETATION ====================

    private void generateOutskirtsVegetation(World world) {
        int halfWorld = WORLD_SIZE / 2;
        Random treeRng = new Random(seed + 7777);

        // Scatter trees in areas outside the town centre that have terrain height > 0
        for (int attempt = 0; attempt < 200; attempt++) {
            int x = treeRng.nextInt(WORLD_SIZE) - halfWorld;
            int z = treeRng.nextInt(WORLD_SIZE) - halfWorld;

            int terrainHeight = getTerrainHeight(x, z);

            // Only place trees on elevated terrain (hills)
            if (terrainHeight <= BASE_HEIGHT) continue;

            // Check there's nothing already here (no building on top)
            if (world.getBlock(x, terrainHeight + 1, z) != BlockType.AIR) continue;
            if (world.getBlock(x, terrainHeight, z) != BlockType.GRASS) continue;

            // Place a tree on the hill
            int treeHeight = 3 + treeRng.nextInt(3); // 3-5 block trunk
            for (int y = terrainHeight + 1; y <= terrainHeight + treeHeight; y++) {
                world.setBlock(x, y, z, BlockType.TREE_TRUNK);
            }
            // Leaf canopy
            int canopyBase = terrainHeight + treeHeight - 1;
            for (int y = canopyBase; y <= canopyBase + 2; y++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0 && y < canopyBase + 2) continue;
                        world.setBlock(x + dx, y, z + dz, BlockType.LEAVES);
                    }
                }
            }
        }
    }
}

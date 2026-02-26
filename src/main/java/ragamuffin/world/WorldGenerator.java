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
 * The town is centered on a park — the park's position is the only location
 * that remains fixed across all playthroughs.  All other building zones
 * (high street, office, JobCentre, industrial estate, residential rows) are
 * offset by a seed-derived amount so each world feels unique.
 */
public class WorldGenerator {
    private static final int WORLD_SIZE = 480; // World is 480x480 blocks
    private static final int PARK_SIZE = 30;   // Park is 30x30 blocks
    private static final int STREET_WIDTH = 6;

    // Terrain height parameters
    private static final int BASE_HEIGHT = 0;  // Sea level / flat area height
    private static final int MAX_TERRAIN_HEIGHT = 8; // Maximum hill height above base
    /** Y coordinate of the bedrock layer (increased depth for underground exploration). */
    public static final int BEDROCK_DEPTH = -32;
    /** Y coordinate of the top of the sewer tunnels. */
    public static final int SEWER_CEILING_Y = -3;
    /** Y coordinate of the floor of the sewer tunnels. */
    public static final int SEWER_FLOOR_Y = -5;
    /** Y coordinate of the top of the underground bunker. */
    public static final int BUNKER_TOP_Y = -8;
    /** Y coordinate of the floor of the underground bunker. */
    public static final int BUNKER_FLOOR_Y = -14;

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

    // ── Seed-derived zone offsets ──────────────────────────────────────────────
    // These give each world a distinct layout while keeping the park at centre.
    // Offsets are clamped to multiples of the street grid to keep buildings aligned.

    /** X start of the high street (south-side row, base 20). */
    private final int hsStartX;
    /** Z of the high street south-side row (base 25). */
    private final int hsSouthZ;
    /** Z of the high street north-side row (base 8). */
    private final int hsNorthZ;
    /** Offset applied to the office building position. */
    private final int officeOffX;
    private final int officeOffZ;
    /** Offset applied to the JobCentre position. */
    private final int jobOffX;
    private final int jobOffZ;
    /** Offset applied to the industrial estate cluster. */
    private final int indOffX;
    private final int indOffZ;
    /** Z offset for the primary residential terraced rows south of the park. */
    private final int resOffZ;
    /** X start offset for the western residential rows. */
    private final int resOffX;

    public WorldGenerator(long seed) {
        this.seed = seed;
        this.random = new Random(seed);

        // Derive independent zone offsets from the seed using distinct multipliers.
        // Each offset is in the range [-8, +8] blocks and snapped to multiples of 4
        // so buildings remain on the street grid.
        Random zoneRng = new Random(seed ^ 0xF00D_CAFE_DEAD_BEEFL);
        hsStartX  = snapToGrid(zoneRng.nextInt(17) - 8);    // high-street X start
        hsSouthZ  = 25 + snapToGrid(zoneRng.nextInt(9) - 4); // high-street south Z
        hsNorthZ  = 8  + snapToGrid(zoneRng.nextInt(9) - 4); // high-street north Z
        officeOffX = snapToGrid(zoneRng.nextInt(17) - 8);
        officeOffZ = snapToGrid(zoneRng.nextInt(17) - 8);
        jobOffX    = snapToGrid(zoneRng.nextInt(17) - 8);
        jobOffZ    = snapToGrid(zoneRng.nextInt(9)  - 4);
        indOffX    = snapToGrid(zoneRng.nextInt(17) - 8);
        indOffZ    = snapToGrid(zoneRng.nextInt(17) - 8);
        resOffZ    = snapToGrid(zoneRng.nextInt(9)  - 4);
        resOffX    = snapToGrid(zoneRng.nextInt(9)  - 4);
    }

    /** Round {@code v} to the nearest multiple of 4 (street-grid unit). */
    private static int snapToGrid(int v) {
        return Math.round(v / 4.0f) * 4;
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
     * Building positions incorporate the seed-derived zone offsets so the town
     * layout varies across playthroughs while the park stays fixed at the centre.
     */
    private void markAllFlatZones() {
        // Park area — ALWAYS at the world centre, never offset.
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

        // High street south-side slots — positions shifted by hsStartX / hsSouthZ
        int sx = 20 + hsStartX;
        int sz = hsSouthZ;
        markFlatZone(sx,      sz, 7, 8);   // south slot 0
        markFlatZone(sx + 8,  sz, 6, 8);   // south slot 1
        markFlatZone(sx + 15, sz, 7, 8);   // south slot 2
        markFlatZone(sx + 23, sz, 6, 8);   // south slot 3
        markFlatZone(sx + 30, sz, 7, 8);   // south slot 4
        markFlatZone(sx + 38, sz, 7, 8);   // south slot 5

        // High street north-side slots
        int nx = 20 + hsStartX;
        int nz = hsNorthZ;
        markFlatZone(nx,      nz, 7, 8);   // north slot 0
        markFlatZone(nx + 8,  nz, 8, 8);   // north slot 1
        markFlatZone(nx + 17, nz, 8, 8);   // north slot 2
        markFlatZone(nx + 26, nz, 7, 8);   // north slot 3
        markFlatZone(nx + 34, nz, 8, 8);   // north slot 4

        // Office building
        int offX = 70 + officeOffX;
        int offZ = 20 + officeOffZ;
        markFlatZone(offX, offZ, 15, 15);

        // JobCentre
        int jobX = -60 + jobOffX;
        int jobZ = 25 + jobOffZ;
        markFlatZone(jobX, jobZ, 12, 12);

        // Terraced rows (south of park)
        int rz1 = -25 + resOffZ;
        int rz2 = rz1 - 16;
        int rz3 = rz2 - 14;
        int rx  = -70 + resOffX;
        markFlatZone(rx,      rz1, 80, 8);
        markFlatZone(rx,      rz2, 80, 8);
        markFlatZone(rx,      rz3, 80, 8);
        markFlatZone(20 + hsStartX, rz1, 64, 8);
        // North of park
        int nrz1 = 30 - resOffZ;
        int nrz2 = nrz1 + 16;
        markFlatZone(rx,      nrz1, 64, 8);
        markFlatZone(rx,      nrz2, 64, 8);

        // Industrial estate
        int indX = 60 + indOffX;
        int indZ = -40 + indOffZ;
        markFlatZone(indX,      indZ,      20, 15);
        markFlatZone(indX,      indZ - 20, 18, 12);
        markFlatZone(indX + 22, indZ,      16, 14);
        markFlatZone(indX + 22, indZ - 18, 14, 12);

        // Extended high street (south side)
        int esx = sx + 46;
        int enz = nx + 43;
        markFlatZone(esx,      sz, 7, 8);   // Chippy
        markFlatZone(enz,      nz, 7, 8);   // Newsagent

        // GP Surgery (west, near JobCentre)
        markFlatZone(jobX, jobZ - 15, 14, 10);

        // Primary school
        markFlatZone(indX, indZ - 40, 36, 16);

        // Community centre, church, taxi rank, car wash
        markFlatZone(rx - 20, rz1, 18, 14);
        markFlatZone(30,      rz2, 12, 18);
        markFlatZone(offX + 16, offZ, 6, 11);
        markFlatZone(indX + 40, indZ, 10, 8);

        // Council flats (west side)
        markFlatZone(rx - 25, nrz2 + 4, 12, 12);
        markFlatZone(rx - 40, nrz2 + 4, 12, 12);

        // Petrol station
        markFlatZone(indX + 40, offZ, 14, 10);

        // Extended high street (further east, south side)
        markFlatZone(esx + 24, sz, 8, 10);  // Nando's
        markFlatZone(esx + 33, sz, 6, 8);   // Barber
        markFlatZone(esx + 40, sz, 7, 8);   // Nail salon
        // North side extension
        markFlatZone(enz + 9,  nz, 7, 8);   // Corner shop
        markFlatZone(enz + 17, nz, 7, 8);   // Betting shop
        markFlatZone(enz + 25, nz, 6, 8);   // Phone repair
        markFlatZone(enz + 32, nz, 8, 8);   // Cash converter

        // Wetherspoons, library, fire station
        markFlatZone(esx + 49, sz, 16, 14);
        markFlatZone(jobX - 20, jobZ - 15, 16, 12);
        markFlatZone(indX + 40, indZ - 25, 16, 14);

        // Additional terraced rows
        markFlatZone(rx - 40, rz1, 40, 8);
        markFlatZone(rx - 40, rz2, 40, 8);
        markFlatZone(rx - 40, nrz1, 40, 8);
        markFlatZone(offX + 20, rz1, 32, 8);
        markFlatZone(offX + 20, rz2, 32, 8);
        markFlatZone(rx - 80, rz1, 40, 8);
        markFlatZone(rx - 80, rz2, 40, 8);
        markFlatZone(rx - 80, nrz1, 40, 8);
        markFlatZone(rx - 80, nrz2, 40, 8);
        markFlatZone(offX + 60, rz1, 40, 8);
        markFlatZone(offX + 60, rz2, 40, 8);
        markFlatZone(offX + 60, nrz1, 32, 8);
        markFlatZone(rx - 80, rz3, 40, 8);
        markFlatZone(rx - 80, rz3 - 16, 40, 8);

        // Additional council flats (outer edges)
        markFlatZone(rx - 80, nrz2 + 4, 12, 12);
        markFlatZone(offX + 70, rz3 - 4, 12, 12);

        // Allotments
        markFlatZone(indX, indZ - 60, 30, 20);

        // Canal
        markFlatZone(-120, rz3 - 30, 240, 8);

        // Skate park
        markFlatZone(rx - 5, rz3 + 10, 18, 14);

        // Cemetery
        markFlatZone(45, rz2 - 22, 20, 18);

        // Leisure centre
        markFlatZone(indX - 30, indZ + 20, 22, 14);

        // Mosque
        markFlatZone(rx - 20, nrz2 + 20, 14, 12);

        // Estate agent
        markFlatZone(nx + 43 + 40, hsNorthZ, 8, 8);

        // Supermarket
        markFlatZone(indX - 30, indZ - 60, 24, 16);

        // Police station
        markFlatZone(-60 + jobOffX - 20, 25 + jobOffZ + 16, 14, 12);

        // Food bank
        markFlatZone(rx - 20, rz3 - 20, 12, 10);
    }

    /**
     * Generate the entire world - landmarks and structure.
     * The park is always at the world centre.  Every other zone position is
     * offset by the seed-derived values computed in the constructor.
     */
    public void generateWorld(World world) {
        // Clear random state
        random.setSeed(seed);

        // Mark flat zones BEFORE generating terrain, so buildings sit on flat ground
        markAllFlatZones();

        // Fill entire world with terrain using heightmap
        // Deep terrain: bedrock at y=-32, stone from y=-31 to y=-1, dirt/grass on surface
        int halfWorld = WORLD_SIZE / 2;
        for (int x = -halfWorld; x < halfWorld; x++) {
            for (int z = -halfWorld; z < halfWorld; z++) {
                int terrainHeight = getTerrainHeight(x, z);
                // Bedrock (indestructible bottom layer at increased depth)
                world.setBlock(x, BEDROCK_DEPTH, z, BlockType.BEDROCK);
                // Stone layers from bedrock+1 up to -1
                for (int y = BEDROCK_DEPTH + 1; y <= -1; y++) {
                    world.setBlock(x, y, z, BlockType.STONE);
                }
                // Fill dirt from y=0 up to terrainHeight-1, grass on top
                for (int y = 0; y < terrainHeight; y++) {
                    world.setBlock(x, y, z, BlockType.DIRT);
                }
                world.setBlock(x, terrainHeight, z, BlockType.GRASS); // Surface layer
            }
        }

        // Generate dense street grid
        generateStreets(world);

        // ===== HIGH STREET — positions derived from seed =====
        generateHighStreet(world);

        // ===== Derived zone anchor points (mirror markAllFlatZones) =====
        int sx   = 20 + hsStartX;
        int sz   = hsSouthZ;
        int nx   = 20 + hsStartX;
        int nz   = hsNorthZ;
        int offX = 70 + officeOffX;
        int offZ = 20 + officeOffZ;
        int jobX = -60 + jobOffX;
        int jobZ = 25 + jobOffZ;
        int indX = 60 + indOffX;
        int indZ = -40 + indOffZ;
        int rz1  = -25 + resOffZ;
        int rz2  = rz1 - 16;
        int rz3  = rz2 - 14;
        int rx   = -70 + resOffX;
        int nrz1 = 30 - resOffZ;
        int nrz2 = nrz1 + 16;

        // ===== OFFICE BUILDING =====
        generateOfficeBuilding(world, offX, offZ, 15, 15, 12);
        world.addLandmark(new Landmark(LandmarkType.OFFICE_BUILDING, offX, 0, offZ, 15, 13, 15));

        // ===== JOBCENTRE =====
        generateJobCentre(world, jobX, jobZ, 12, 12, 5);
        world.addLandmark(new Landmark(LandmarkType.JOB_CENTRE, jobX, 0, jobZ, 12, 6, 12));

        // ===== TERRACED HOUSES — multiple rows =====
        Random rowRng = new Random(seed ^ 0xBEEF1234L);
        int[] rowHeights = {
            5 + rowRng.nextInt(3),
            5 + rowRng.nextInt(3),
            5 + rowRng.nextInt(3),
            5 + rowRng.nextInt(3),
            5 + rowRng.nextInt(3),
            5 + rowRng.nextInt(3),
        };
        generateTerracedRow(world, rx,              rz1, 8, 8, rowHeights[0], 10);
        generateTerracedRow(world, rx,              rz2, 8, 8, rowHeights[1], 10);
        generateTerracedRow(world, rx,              rz3, 8, 8, rowHeights[2], 10);
        generateTerracedRow(world, 20 + hsStartX,   rz1, 8, 8, rowHeights[3], 8);
        generateTerracedRow(world, rx,             nrz1, 8, 8, rowHeights[4], 8);
        generateTerracedRow(world, rx,             nrz2, 8, 8, rowHeights[5], 8);

        generateGardenWalls(world, rx, rz1 + 8, 80, 1);
        generateGardenWalls(world, rx, rz2 + 8, 80, 1);
        generateGardenWalls(world, rx, nrz1 + 8, 64, 1);

        // ===== INDUSTRIAL ESTATE =====
        Random warehouseRng = new Random(seed ^ 0xCAFE5678L);
        int wh1 = 7 + warehouseRng.nextInt(3);
        int wh2 = 6 + warehouseRng.nextInt(3);
        int wh3 = 7 + warehouseRng.nextInt(3);
        int wh4 = 6 + warehouseRng.nextInt(3);
        generateWarehouse(world, indX,      indZ,      20, 15, wh1);
        world.addLandmark(new Landmark(LandmarkType.WAREHOUSE, indX, 0, indZ, 20, wh1 + 1, 15));
        generateWarehouse(world, indX,      indZ - 20, 18, 12, wh2);
        generateWarehouse(world, indX + 22, indZ,      16, 14, wh3);
        generateWarehouse(world, indX + 22, indZ - 18, 14, 12, wh4);
        generateGardenWalls(world, indX - 2, indZ - 25, 44, 2);

        // ===== EXTENDED HIGH STREET (south side) =====
        int esx = sx + 46;
        int enz = nx + 43;
        generateShopWithSign(world, esx, sz, 7, 8, 4, BlockType.STONE, BlockType.SIGN_WHITE, LandmarkType.CHIPPY);
        generateShopWithSign(world, enz, nz, 7, 8, 4, BlockType.BRICK, BlockType.SIGN_GREEN, LandmarkType.NEWSAGENT);

        // ===== GP SURGERY =====
        int gpX = jobX;
        int gpZ = jobZ - 15;
        buildBuilding(world, gpX, gpZ, 14, 10, 5, BlockType.BRICK, BlockType.PAVEMENT);
        for (int dx = 0; dx < 14; dx++) {
            world.setBlock(gpX + dx, 5, gpZ, BlockType.SIGN_BLUE);
        }
        world.addLandmark(new Landmark(LandmarkType.GP_SURGERY, gpX, 0, gpZ, 14, 6, 10));

        // ===== PRIMARY SCHOOL =====
        int schoolX = indX;
        int schoolZ = indZ - 40;
        buildBuilding(world, schoolX, schoolZ, 20, 16, 6, BlockType.BRICK, BlockType.PAVEMENT);
        for (int dx = 0; dx < 20; dx++) {
            world.setBlock(schoolX + dx, 6, schoolZ, BlockType.SIGN_BLUE);
        }
        for (int x = schoolX + 20; x < schoolX + 34; x++) {
            for (int z = schoolZ; z < schoolZ + 12; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
        for (int x = schoolX + 20; x < schoolX + 34; x++) {
            for (int y = 1; y <= 2; y++) {
                world.setBlock(x, y, schoolZ,       BlockType.IRON_FENCE);
                world.setBlock(x, y, schoolZ + 12,  BlockType.IRON_FENCE);
            }
        }
        for (int z = schoolZ; z < schoolZ + 12; z++) {
            for (int y = 1; y <= 2; y++) {
                world.setBlock(schoolX + 20, y, z, BlockType.IRON_FENCE);
                world.setBlock(schoolX + 33, y, z, BlockType.IRON_FENCE);
            }
        }
        world.addLandmark(new Landmark(LandmarkType.PRIMARY_SCHOOL, schoolX, 0, schoolZ, 20, 7, 16));

        // ===== COMMUNITY CENTRE =====
        int ccX = rx - 20;
        int ccZ = rz1;
        buildBuilding(world, ccX, ccZ, 18, 14, 5, BlockType.BRICK, BlockType.PAVEMENT);
        for (int dx = 0; dx < 18; dx++) {
            world.setBlock(ccX + dx, 5, ccZ, BlockType.SIGN_RED);
        }
        world.setBlock(ccX + 8, 1, ccZ, BlockType.AIR);
        world.setBlock(ccX + 9, 1, ccZ, BlockType.AIR);
        world.setBlock(ccX + 8, 2, ccZ, BlockType.AIR);
        world.setBlock(ccX + 9, 2, ccZ, BlockType.AIR);
        world.addLandmark(new Landmark(LandmarkType.COMMUNITY_CENTRE, ccX, 0, ccZ, 18, 6, 14));

        // ===== CHURCH =====
        int churchX = 30;
        int churchZ = rz2;
        generateChurch(world, churchX, churchZ, 12, 18, 10);
        world.addLandmark(new Landmark(LandmarkType.CHURCH, churchX, 0, churchZ, 12, 17, 18));

        // ===== TAXI RANK =====
        int taxiX = offX + 16;
        int taxiZ = offZ;
        buildBuilding(world, taxiX, taxiZ + 5, 6, 6, 3, BlockType.BRICK, BlockType.PAVEMENT);
        for (int dx = 0; dx < 6; dx++) {
            world.setBlock(taxiX + dx, 3, taxiZ + 5, BlockType.SIGN_YELLOW);
        }
        for (int x = taxiX; x < taxiX + 6; x++) {
            for (int z = taxiZ; z < taxiZ + 5; z++) {
                world.setBlock(x, 0, z, BlockType.PAVEMENT);
            }
        }
        world.addLandmark(new Landmark(LandmarkType.TAXI_RANK, taxiX, 0, taxiZ + 5, 6, 4, 6));

        // ===== CAR WASH =====
        int cwX = indX + 40;
        int cwZ = indZ;
        generateCarWash(world, cwX, cwZ, 10, 8, 5);
        world.addLandmark(new Landmark(LandmarkType.CAR_WASH, cwX, 0, cwZ, 10, 6, 8));

        // ===== COUNCIL FLATS =====
        int cf1X = rx - 25;
        int cf1Z = nrz2 + 4;
        generateCouncilFlats(world, cf1X, cf1Z, 12, 12, 18);
        world.addLandmark(new Landmark(LandmarkType.COUNCIL_FLATS, cf1X, 0, cf1Z, 12, 19, 12));
        int cf2X = rx - 40;
        generateCouncilFlats(world, cf2X, cf1Z, 12, 12, 15);
        world.addLandmark(new Landmark(LandmarkType.COUNCIL_FLATS, cf2X, 0, cf1Z, 12, 16, 12));

        // ===== PETROL STATION =====
        int psX = cwX;
        int psZ = offZ;
        generatePetrolStation(world, psX, psZ, 14, 10, 4);
        world.addLandmark(new Landmark(LandmarkType.PETROL_STATION, psX, 0, psZ, 14, 5, 10));

        // ===== FURTHER HIGH STREET EXTENSION (south side) =====
        generateShopWithSign(world, esx + 24, sz, 8, 10, 5, BlockType.YELLOW_BRICK, BlockType.SIGN_RED,   LandmarkType.NANDOS);
        generateShopWithSign(world, esx + 33, sz, 6, 8,  4, BlockType.TILE_WHITE,   BlockType.SIGN_BLUE,  LandmarkType.BARBER);
        generateShopWithSign(world, esx + 40, sz, 7, 8,  4, BlockType.RENDER_PINK,  BlockType.SIGN_WHITE, LandmarkType.NAIL_SALON);
        // North side
        generateShopWithSign(world, enz + 9,  nz, 7, 8,  4, BlockType.YELLOW_BRICK, BlockType.SIGN_GREEN, LandmarkType.CORNER_SHOP);
        generateShopWithSign(world, enz + 17, nz, 7, 8,  4, BlockType.BRICK,         BlockType.SIGN_RED,   LandmarkType.BETTING_SHOP);
        generateShopWithSign(world, enz + 25, nz, 6, 8,  4, BlockType.RENDER_WHITE,  BlockType.SIGN_YELLOW,LandmarkType.PHONE_REPAIR);
        generateShopWithSign(world, enz + 32, nz, 8, 8,  4, BlockType.BRICK,         BlockType.SIGN_YELLOW,LandmarkType.CASH_CONVERTER);

        // ===== WETHERSPOONS =====
        int wsX = esx + 49;
        generateWetherspoons(world, wsX, sz, 16, 14, 6);
        world.addLandmark(new Landmark(LandmarkType.WETHERSPOONS, wsX, 0, sz, 16, 7, 14));

        // ===== LIBRARY =====
        int libX = jobX - 20;
        int libZ = gpZ;
        generateLibrary(world, libX, libZ, 16, 12, 6);
        world.addLandmark(new Landmark(LandmarkType.LIBRARY, libX, 0, libZ, 16, 7, 12));

        // ===== FIRE STATION =====
        int fsX = cwX + 40;
        int fsZ = indZ - 25;
        generateFireStation(world, fsX, fsZ, 16, 14, 7);
        world.addLandmark(new Landmark(LandmarkType.FIRE_STATION, fsX, 0, fsZ, 16, 8, 14));

        // ===== ADDITIONAL TERRACED ROWS =====
        generateTerracedRow(world, rx - 40, rz1,  8, 8, 6, 5);
        generateTerracedRow(world, rx - 40, rz2,  8, 8, 6, 5);
        generateTerracedRow(world, rx - 40, nrz1, 8, 8, 6, 5);
        generateTerracedRow(world, offX + 20, rz1, 8, 8, 6, 4);
        generateTerracedRow(world, offX + 20, rz2, 8, 8, 6, 4);
        generateTerracedRow(world, rx - 80, rz1,  8, 8, 6, 5);
        generateTerracedRow(world, rx - 80, rz2,  8, 8, 6, 5);
        generateTerracedRow(world, rx - 80, nrz1, 8, 8, 6, 5);
        generateTerracedRow(world, rx - 80, nrz2, 8, 8, 6, 5);
        generateTerracedRow(world, offX + 60, rz1, 8, 8, 6, 5);
        generateTerracedRow(world, offX + 60, rz2, 8, 8, 6, 5);
        generateTerracedRow(world, offX + 60, nrz1,8, 8, 6, 4);
        generateTerracedRow(world, rx - 80, rz3,  8, 8, 6, 5);
        generateTerracedRow(world, rx - 80, rz3 - 16, 8, 8, 6, 5);

        // ===== ADDITIONAL COUNCIL FLATS =====
        generateCouncilFlats(world, rx - 80, nrz2 + 4, 12, 12, 14);
        world.addLandmark(new Landmark(LandmarkType.COUNCIL_FLATS, rx - 80, 0, nrz2 + 4, 12, 15, 12));
        generateCouncilFlats(world, offX + 70, rz3 - 4, 12, 12, 16);

        // ===== ALLOTMENTS =====
        int alX = indX;
        int alZ = indZ - 60;
        generateAllotments(world, alX, alZ, 30, 20);
        world.addLandmark(new Landmark(LandmarkType.ALLOTMENTS, alX, 0, alZ, 30, 3, 20));

        // ===== CANAL =====
        int canalZ = rz3 - 30;
        generateCanal(world, -120, canalZ, 240, 8);
        world.addLandmark(new Landmark(LandmarkType.CANAL, -120, 0, canalZ, 240, 1, 8));

        // ===== SKATE PARK =====
        int skX = rx - 5;
        int skZ = rz3 + 10;
        generateSkatePark(world, skX, skZ, 18, 14);
        world.addLandmark(new Landmark(LandmarkType.SKATE_PARK, skX, 0, skZ, 18, 3, 14));

        // ===== CEMETERY =====
        int cemX = 45;
        int cemZ = rz2 - 22;
        generateCemetery(world, cemX, cemZ, 20, 18);
        world.addLandmark(new Landmark(LandmarkType.CEMETERY, cemX, 0, cemZ, 20, 3, 18));

        // ===== LEISURE CENTRE =====
        int lcX = indX - 30;
        int lcZ = indZ + 20;
        generateLeisureCentre(world, lcX, lcZ, 22, 14, 6);
        world.addLandmark(new Landmark(LandmarkType.LEISURE_CENTRE, lcX, 0, lcZ, 22, 8, 14));

        // ===== MOSQUE =====
        int mosqueX = rx - 20;
        int mosqueZ = nrz2 + 20;
        generateMosque(world, mosqueX, mosqueZ, 14, 12, 6);
        world.addLandmark(new Landmark(LandmarkType.MOSQUE, mosqueX, 0, mosqueZ, 14, 14, 12));

        // ===== ESTATE AGENT =====
        int eaX = enz + 40;
        int eaZ = nz;
        generateEstateAgent(world, eaX, eaZ, 8, 8, 4);
        world.addLandmark(new Landmark(LandmarkType.ESTATE_AGENT, eaX, 0, eaZ, 8, 6, 8));

        // ===== SUPERMARKET =====
        int smX = indX - 30;
        int smZ = indZ - 60;
        generateSupermarket(world, smX, smZ, 24, 16, 5);
        world.addLandmark(new Landmark(LandmarkType.SUPERMARKET, smX, 0, smZ, 24, 7, 16));

        // ===== POLICE STATION =====
        int pstnX = jobX - 20;
        int pstnZ = jobZ + 16;
        generatePoliceStation(world, pstnX, pstnZ, 14, 12, 7);
        world.addLandmark(new Landmark(LandmarkType.POLICE_STATION, pstnX, 0, pstnZ, 14, 9, 12));

        // ===== FOOD BANK =====
        int fbX = rx - 20;
        int fbZ = rz3 - 20;
        generateFoodBank(world, fbX, fbZ, 12, 10, 4);
        world.addLandmark(new Landmark(LandmarkType.FOOD_BANK, fbX, 0, fbZ, 12, 6, 10));

        // Garden walls for extra rows
        generateGardenWalls(world, rx - 40, rz2 + 8, 40, 1);
        generateGardenWalls(world, offX + 20, rz2 + 8, 32, 1);
        generateGardenWalls(world, rx - 80, rz2 + 8, 40, 1);
        generateGardenWalls(world, rx - 80, rz3 - 8, 40, 1);
        generateGardenWalls(world, offX + 60, rz2 + 8, 40, 1);

        // Fill gaps between buildings along the high street
        fillGapsBetweenBuildings(world, sx, sz, 60);
        fillGapsBetweenBuildings(world, nx, nz, 52);

        // Street-side garden walls in residential areas
        generateGardenWalls(world, rx - 80, rz1 - 5, 170, 1);
        generateGardenWalls(world, sx, rz1 - 5, 120, 1);

        // ===== BUILDING INTERIORS =====
        generateBuildingInteriors(world, sx, sz, nx, nz, esx, enz, jobX, jobZ, gpX, gpZ, libX, libZ);

        // ===== STREET FURNITURE =====
        generateStreetFurniture(world, sx, sz, nx, nz);

        // ===== PARK — generated LAST so it always wins over any accidental overlap =====
        // The park is the only location guaranteed to be at the same position in every world.
        generatePark(world);

        // ===== PARK FURNITURE =====
        generateParkFurniture(world);

        // ===== SCATTERED TREES on hills outside town =====
        generateOutskirtsVegetation(world);

        // ===== UNDERGROUND STRUCTURES =====
        generateSewerTunnels(world, sx, sz, nx, nz);
        generateUndergroundBunker(world);

        // ===== UNDERGROUND MINERALS — Issue #691 =====
        generateMineralVeins(world);

        // ===== ANIMATED FLAG POLES — Issue #658 =====
        // Place flags on roofs of key civic buildings.  Each flag pole is a
        // 3-block IRON_FENCE pillar; the top block is where the flag renders.
        generateFlagPoles(world, offX, offZ, 12,
                          jobX, jobZ, 5,
                          pstnX, pstnZ, 7,
                          fsX, fsZ, 7,
                          schoolX, schoolZ, 6,
                          cf1X, cf1Z, 14);

        // ===== NON-BLOCK 3D PROPS — Issue #669 =====
        // Place unique decorative objects throughout the world to add visual
        // variety: phone boxes, post boxes, benches, bus shelters, bollards,
        // street lamps, litter bins, market stalls, picnic tables, bike racks,
        // shopping trolleys, and a park statue.
        generateProps(world, sx, sz, nx, nz, offX, offZ, jobX, jobZ, pstnX, pstnZ,
                      rx, rz1, rz2, indX, indZ, smX, smZ);

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

                    // Bedrock at BEDROCK_DEPTH (increased depth)
                    int bedrockLocalY = BEDROCK_DEPTH - startY;
                    if (bedrockLocalY >= 0 && bedrockLocalY < Chunk.HEIGHT) {
                        chunk.setBlock(localX, bedrockLocalY, localZ, BlockType.BEDROCK);
                    }

                    // Stone from BEDROCK_DEPTH+1 to y=-1
                    for (int y = BEDROCK_DEPTH + 1; y <= -1; y++) {
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

        // Sad pond — dig a shallow depression and fill with water
        int pondX = parkStart + PARK_SIZE / 3;
        int pondZ = parkStart + PARK_SIZE / 3;
        for (int x = pondX; x < pondX + 4; x++) {
            for (int z = pondZ; z < pondZ + 4; z++) {
                world.setBlock(x, -1, z, BlockType.WATER);
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
     * Generate the high street with seed-derived positions and shuffled shop assignments.
     * Both the X start of the street and the Z row positions vary per seed (via hsStartX,
     * hsSouthZ, hsNorthZ), so each world has a distinct high-street location.
     * Which shop occupies each slot also varies by seed.
     */
    private void generateHighStreet(World world) {
        Random layoutRng = new Random(seed ^ 0xDEADBEEFL);

        // Anchor X and Z from seed-derived fields
        int sx = 20 + hsStartX;
        int sz = hsSouthZ;
        int nx = 20 + hsStartX;
        int nz = hsNorthZ;

        // --- South side slots ---
        int[][] southSlots = {
            {sx,      7, 8},
            {sx + 8,  6, 8},
            {sx + 15, 7, 8},
            {sx + 23, 6, 8},
            {sx + 30, 7, 8},
            {sx + 38, 7, 8},
        };
        List<LandmarkType> southShops = new ArrayList<>(Arrays.asList(
            LandmarkType.GREGGS,
            LandmarkType.OFF_LICENCE,
            LandmarkType.CHARITY_SHOP,
            LandmarkType.JEWELLER,
            LandmarkType.BOOKIES,
            LandmarkType.KEBAB_SHOP
        ));
        Collections.shuffle(southShops, layoutRng);

        for (int i = 0; i < southSlots.length; i++) {
            int[] slot = southSlots[i];
            LandmarkType type = southShops.get(i);
            generateShopWithSign(world, slot[0], sz, slot[1], slot[2], 4, wallForShop(type), signForShop(type), type);
        }

        // --- North side slots ---
        int[][] northSlots = {
            {nx,      7, 8},
            {nx + 8,  8, 8},
            {nx + 17, 8, 8},
            {nx + 26, 7, 8},
            {nx + 34, 8, 8},
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
            generateShopWithSign(world, slot[0], nz, slot[1], slot[2], northHeights[i], wallForShop(type), signForShop(type), type);
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

    /**
     * Facade material palettes for terraced houses.
     * Each array is a set of block types that can appear as the main wall material.
     */
    private static final BlockType[] HOUSE_WALL_PALETTE = {
        BlockType.BRICK,
        BlockType.YELLOW_BRICK,
        BlockType.PEBBLEDASH,
        BlockType.RENDER_WHITE,
        BlockType.RENDER_CREAM,
    };

    /** Roof materials matched to wall palette entries. */
    private static final BlockType[] HOUSE_ROOF_PALETTE = {
        BlockType.SLATE,
        BlockType.SLATE,
        BlockType.ROOF_TILE,
        BlockType.ROOF_TILE,
        BlockType.SLATE,
    };

    private void generateTerracedRow(World world, int startX, int startZ,
                                      int houseWidth, int houseDepth, int houseHeight, int count) {
        // Use a deterministic RNG seeded from position so the same row always looks the same
        // across generation passes, but differs from other rows.
        Random rowRng = new Random(seed ^ ((long) startX * 0x9E3779B97F4A7C15L + startZ * 0x6C62272E07BB0142L));
        int cursorX = startX;
        for (int i = 0; i < count; i++) {
            // Vary width slightly: ±1 block, keeping minimum of 5
            int w = houseWidth + (rowRng.nextInt(3) - 1); // -1, 0, +1
            if (w < 5) w = 5;
            // Vary height slightly: ±1 block
            int h = houseHeight + (rowRng.nextInt(3) - 1);
            if (h < 4) h = 4;
            buildHouseVariant(world, cursorX, startZ, w, houseDepth, h, rowRng);
            // Register each house as a TERRACED_HOUSE landmark (height includes roof + chimney allowance)
            world.addLandmark(new Landmark(LandmarkType.TERRACED_HOUSE, cursorX, 0, startZ, w, h + 3, houseDepth));
            cursorX += w;
        }
    }

    /**
     * Build a terraced house with varied facade, windows, roof and optional chimney.
     * Each house picks from the palette deterministically based on the provided RNG.
     */
    private void buildHouseVariant(World world, int x, int z, int width, int depth,
                                    int height, Random rng) {
        // Pick facade style
        int styleIdx = rng.nextInt(HOUSE_WALL_PALETTE.length);
        BlockType wallType = HOUSE_WALL_PALETTE[styleIdx];
        BlockType roofType = HOUSE_ROOF_PALETTE[styleIdx];

        // Determine window row heights (glass rows)
        // Standard British terrace: windows at y=2 (ground floor) and y=4 (first floor)
        // Some variants skip y=2 (e.g. pebbledash often has smaller windows)
        boolean doubleGlazed = rng.nextBoolean(); // wider window opening
        int windowRowA = 2;
        int windowRowB = (height >= 5) ? 4 : -1; // no second row for shorter houses

        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (!isWall) continue;
                    if (y == windowRowA || y == windowRowB) {
                        // Front and back walls get windows; side walls stay solid
                        boolean isFrontOrBack = (dz == 0 || dz == depth - 1);
                        boolean inWindowZone;
                        if (doubleGlazed) {
                            // Window spans from dx=1 to dx=width-2
                            inWindowZone = isFrontOrBack && dx >= 1 && dx <= width - 2;
                        } else {
                            // Narrower window in the centre
                            inWindowZone = isFrontOrBack && dx >= 1 && dx <= width - 2
                                && dx != width / 2; // leave a centre mullion
                        }
                        world.setBlock(x + dx, y, z + dz,
                            inWindowZone ? BlockType.GLASS : wallType);
                    } else {
                        world.setBlock(x + dx, y, z + dz, wallType);
                    }
                }
            }
        }

        // Roof
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, roofType);
                world.setBlock(x + dx, 0, z + dz, BlockType.WOOD);
            }
        }

        // Front door (centred, cleared to height 2)
        int doorX = x + width / 2;
        world.setBlock(doorX, 1, z, BlockType.AIR);
        world.setBlock(doorX, 2, z, BlockType.AIR);

        // Occasional chimney stack (30% chance) on the ridge
        if (rng.nextInt(10) < 3 && width >= 6) {
            int chimneyDx = rng.nextBoolean() ? 1 : width - 2;
            world.setBlock(x + chimneyDx, height + 2, z + depth / 2, BlockType.BRICK);
            world.setBlock(x + chimneyDx, height + 3, z + depth / 2, BlockType.BRICK);
        }
    }

    // ==================== OFFICE BUILDING ====================

    private void generateOfficeBuilding(World world, int x, int z, int width, int depth, int height) {
        // Vary the facade style: curtain-wall glass, stone-and-glass bands, or concrete-and-glass
        Random offRng = new Random(seed ^ 0xABCD_EF01_2345_6789L);
        int officeStyle = offRng.nextInt(3); // 0=full curtain wall, 1=stone bands, 2=concrete frame

        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (!isWall) continue;
                    boolean isCorner = (dx == 0 || dx == width - 1) && (dz == 0 || dz == depth - 1);
                    BlockType block;
                    switch (officeStyle) {
                        case 0:
                            // Full curtain-wall: glass everywhere except corners
                            block = isCorner ? BlockType.CONCRETE : BlockType.GLASS;
                            break;
                        case 1:
                            // Stone-and-glass bands: alternating full-height stone and glass rows
                            block = (y % 3 == 1) ? BlockType.STONE : BlockType.GLASS;
                            break;
                        default:
                            // Concrete frame with glass infill: concrete at corners + every 4th col
                            boolean isFrame = isCorner || (dx % 4 == 0) || (y == 1) || (y == height);
                            block = isFrame ? BlockType.CONCRETE : BlockType.GLASS;
                            break;
                    }
                    world.setBlock(x + dx, y, z + dz, block);
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
        // Rooftop plant room / lift shaft (small concrete block on roof)
        int plantW = Math.max(3, width / 4);
        int plantD = Math.max(3, depth / 4);
        int plantX = x + width / 2 - plantW / 2;
        int plantZ = z + depth / 2 - plantD / 2;
        for (int dx = 0; dx < plantW; dx++) {
            for (int dz = 0; dz < plantD; dz++) {
                world.setBlock(plantX + dx, height + 2, plantZ + dz, BlockType.CONCRETE);
            }
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
        // Vary warehouse style: all-metal, brick-base + metal upper, or concrete block
        int whStyle = Math.abs((x * 379 + z * 613) ^ (int)(seed & 0xFFFF)) % 3;
        BlockType lowerWall = (whStyle == 1) ? BlockType.BRICK : BlockType.CORRUGATED_METAL;
        BlockType upperWall = BlockType.CORRUGATED_METAL;
        // Some warehouses have a concrete base course (bottom 2 rows)
        boolean hasBrickBase = (whStyle == 2);

        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (!isWall) continue;
                    BlockType block;
                    if (hasBrickBase && y <= 2) {
                        block = BlockType.CONCRETE;
                    } else if (y <= height / 2) {
                        block = lowerWall;
                    } else {
                        block = upperWall;
                    }
                    world.setBlock(x + dx, y, z + dz, block);
                }
            }
        }
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.CORRUGATED_METAL); // Metal warehouse roof
                world.setBlock(x + dx, 0, z + dz, BlockType.PAVEMENT);
            }
        }
        // Roller door (variable width based on building size)
        int doorWidth = Math.min(4, width / 3);
        for (int dx = 1; dx <= doorWidth && dx < width - 1; dx++) {
            for (int y = 1; y <= 3; y++) {
                world.setBlock(x + dx, y, z, BlockType.AIR);
            }
        }
        // Some warehouses have a loading bay sign
        if (whStyle == 0) {
            for (int dx = 0; dx < width; dx++) {
                world.setBlock(x + dx, height, z, BlockType.SIGN_WHITE);
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

    // ==================== LEISURE CENTRE ====================

    /**
     * Generate a leisure centre — a large flat-roofed brick and concrete building with
     * wide glass frontage and an attached car park.
     */
    private void generateLeisureCentre(World world, int x, int z, int width, int depth, int height) {
        // Main block: brick lower half, concrete upper half
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (!isWall) continue;
                    BlockType block;
                    if (y <= height / 2) {
                        block = BlockType.BRICK;
                    } else {
                        block = BlockType.CONCRETE;
                    }
                    // Wide glass frontage on ground and first floor
                    if (dz == 0 && (y == 1 || y == 2) && dx >= 1 && dx <= width - 2) {
                        block = BlockType.GLASS;
                    }
                    // Clerestory windows near top on side walls
                    if ((dx == 0 || dx == width - 1) && y == height - 1) {
                        block = BlockType.GLASS;
                    }
                    world.setBlock(x + dx, y, z + dz, block);
                }
            }
        }
        // Flat concrete roof
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.CONCRETE);
                world.setBlock(x + dx, 0, z + dz, BlockType.PAVEMENT);
            }
        }
        // Sign strip across front
        for (int dx = 0; dx < width; dx++) {
            world.setBlock(x + dx, height, z, BlockType.SIGN_BLUE);
        }
        // Main entrance doors (cleared)
        world.setBlock(x + width / 2 - 1, 1, z, BlockType.AIR);
        world.setBlock(x + width / 2,     1, z, BlockType.AIR);
        world.setBlock(x + width / 2 - 1, 2, z, BlockType.AIR);
        world.setBlock(x + width / 2,     2, z, BlockType.AIR);
    }

    // ==================== MOSQUE ====================

    /**
     * Generate a mosque — rendered white walls with a central dome (stone hemisphere)
     * and a minaret tower at one corner.
     */
    private void generateMosque(World world, int x, int z, int width, int depth, int height) {
        // Main prayer hall: render white walls
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (!isWall) continue;
                    BlockType block;
                    // Arched windows at y=2, y=4
                    if (y == 2 || y == 4) {
                        boolean isFrontOrBack = (dz == 0 || dz == depth - 1);
                        boolean isWindowPos = dx >= 2 && dx <= width - 3 && (dx % 3 == 2);
                        block = (isFrontOrBack && isWindowPos) ? BlockType.GLASS : BlockType.RENDER_WHITE;
                    } else {
                        block = BlockType.RENDER_WHITE;
                    }
                    world.setBlock(x + dx, y, z + dz, block);
                }
            }
        }
        // Flat roof base
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.RENDER_WHITE);
                world.setBlock(x + dx, 0, z + dz, BlockType.STONE);
            }
        }
        // Central dome — 3x3 stone hemisphere above roof centre
        int domeX = x + width / 2 - 1;
        int domeZ = z + depth / 2 - 1;
        int domeBase = height + 2;
        // Bottom ring of dome
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                world.setBlock(domeX + dx, domeBase, domeZ + dz, BlockType.STONE);
            }
        }
        // Top of dome
        world.setBlock(domeX + 1, domeBase + 1, domeZ + 1, BlockType.STONE);
        // Sign strip across front
        for (int dx = 0; dx < width; dx++) {
            world.setBlock(x + dx, height, z, BlockType.SIGN_GREEN);
        }
        // Minaret at front-left corner (narrow tower)
        int minaretX = x;
        int minaretZ = z;
        for (int y = 1; y <= height + 6; y++) {
            world.setBlock(minaretX, y, minaretZ, BlockType.RENDER_WHITE);
        }
        // Minaret cap
        world.setBlock(minaretX, height + 7, minaretZ, BlockType.STONE);
        // Entrance door
        world.setBlock(x + width / 2, 1, z, BlockType.AIR);
        world.setBlock(x + width / 2, 2, z, BlockType.AIR);
    }

    // ==================== ESTATE AGENT ====================

    /**
     * Generate an estate agent — a neat shop-front with a tile-white or render facade,
     * large display windows showing property listings.
     */
    private void generateEstateAgent(World world, int x, int z, int width, int depth, int height) {
        // Tile white facade
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (!isWall) continue;
                    BlockType block;
                    // Full-width display window on ground floor front
                    if (dz == 0 && y == 1 && dx >= 1 && dx <= width - 2) {
                        block = BlockType.GLASS;
                    } else if (dz == 0 && y == 2 && dx >= 1 && dx <= width - 2) {
                        block = BlockType.GLASS;
                    } else {
                        block = BlockType.TILE_WHITE;
                    }
                    world.setBlock(x + dx, y, z + dz, block);
                }
            }
        }
        // Flat concrete roof
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.CONCRETE);
                world.setBlock(x + dx, 0, z + dz, BlockType.PAVEMENT);
            }
        }
        // Sign strip
        for (int dx = 0; dx < width; dx++) {
            world.setBlock(x + dx, height, z, BlockType.SIGN_RED);
        }
        // Door (cleared from display window)
        world.setBlock(x + width / 2, 1, z, BlockType.AIR);
        world.setBlock(x + width / 2, 2, z, BlockType.AIR);
    }

    // ==================== SUPERMARKET ====================

    /**
     * Generate a supermarket — a large single-storey building with a flat corrugated-metal
     * roof and wide glass frontage, surrounded by a concrete car park.
     */
    private void generateSupermarket(World world, int x, int z, int width, int depth, int height) {
        // Walls: concrete lower, brick upper
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (!isWall) continue;
                    BlockType block;
                    // Large glass frontage spanning most of the front wall
                    if (dz == 0 && y <= 2 && dx >= 2 && dx <= width - 3) {
                        block = BlockType.GLASS;
                    } else {
                        block = (y <= 2) ? BlockType.CONCRETE : BlockType.BRICK;
                    }
                    world.setBlock(x + dx, y, z + dz, block);
                }
            }
        }
        // Corrugated metal roof
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.CORRUGATED_METAL);
                // Concrete car park floor
                world.setBlock(x + dx, 0, z + dz, BlockType.PAVEMENT);
            }
        }
        // Extended concrete car park in front
        for (int dx = -2; dx < width + 2; dx++) {
            for (int dz = -4; dz < 0; dz++) {
                world.setBlock(x + dx, 0, z + dz, BlockType.PAVEMENT);
            }
        }
        // Sign strip
        for (int dx = 0; dx < width; dx++) {
            world.setBlock(x + dx, height, z, BlockType.SIGN_RED);
        }
        // Entrance doors (wide double doors)
        for (int dx = width / 2 - 1; dx <= width / 2 + 1; dx++) {
            world.setBlock(x + dx, 1, z, BlockType.AIR);
            world.setBlock(x + dx, 2, z, BlockType.AIR);
        }
    }

    // ==================== POLICE STATION ====================

    /**
     * Generate a police station — an imposing stone-and-brick building with barred
     * windows and a distinctive blue sign.
     */
    private void generatePoliceStation(World world, int x, int z, int width, int depth, int height) {
        // Stone facade — authoritative look
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (!isWall) continue;
                    BlockType block;
                    // Small windows (narrow, at y=2 and y=4) — police stations have restricted windows
                    boolean isFrontOrBack = (dz == 0 || dz == depth - 1);
                    boolean isNarrowWindow = isFrontOrBack && (y == 2 || y == 4)
                        && dx >= 2 && dx <= width - 3 && (dx % 4 == 2);
                    block = isNarrowWindow ? BlockType.GLASS : BlockType.STONE;
                    world.setBlock(x + dx, y, z + dz, block);
                }
            }
        }
        // Flat roof
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.CONCRETE);
                world.setBlock(x + dx, 0, z + dz, BlockType.PAVEMENT);
            }
        }
        // Blue sign strip
        for (int dx = 0; dx < width; dx++) {
            world.setBlock(x + dx, height, z, BlockType.SIGN_BLUE);
        }
        // Imposing entrance with wider door opening
        world.setBlock(x + width / 2 - 1, 1, z, BlockType.AIR);
        world.setBlock(x + width / 2,     1, z, BlockType.AIR);
        world.setBlock(x + width / 2 - 1, 2, z, BlockType.AIR);
        world.setBlock(x + width / 2,     2, z, BlockType.AIR);
        world.setBlock(x + width / 2 - 1, 3, z, BlockType.AIR);
        world.setBlock(x + width / 2,     3, z, BlockType.AIR);
    }

    // ==================== FOOD BANK ====================

    /**
     * Generate a food bank — a modest converted warehouse or community hall,
     * brick-built with a flat roof and a prominent sign.
     */
    private void generateFoodBank(World world, int x, int z, int width, int depth, int height) {
        // Simple brick construction — a converted building
        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (!isWall) continue;
                    BlockType block;
                    // Basic windows at y=2 on front
                    boolean isFront = (dz == 0);
                    if (isFront && y == 2 && dx >= 1 && dx <= width - 2) {
                        block = BlockType.GLASS;
                    } else {
                        block = BlockType.BRICK;
                    }
                    world.setBlock(x + dx, y, z + dz, block);
                }
            }
        }
        // Flat concrete roof
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.CONCRETE);
                world.setBlock(x + dx, 0, z + dz, BlockType.PAVEMENT);
            }
        }
        // Green sign strip
        for (int dx = 0; dx < width; dx++) {
            world.setBlock(x + dx, height, z, BlockType.SIGN_GREEN);
        }
        // Entrance
        world.setBlock(x + width / 2, 1, z, BlockType.AIR);
        world.setBlock(x + width / 2, 2, z, BlockType.AIR);
    }

    // ==================== BUILDING HELPERS ====================

    private void buildShop(World world, int x, int z, int width, int depth, int height, BlockType wallType) {
        // Use a positional hash for deterministic per-shop variation
        int shopHash = (x * 1049 + z * 757) ^ (int)(seed & 0xFFFFFFL);
        // Shop style: 0 = large display window, 1 = narrow display window, 2 = solid lower half
        int shopStyle = Math.abs(shopHash) % 3;
        // Upper floor material may differ: some shops have render or yellow brick upper floors
        BlockType upperWall = ((Math.abs(shopHash) >> 4) % 3 == 0 && wallType == BlockType.BRICK)
            ? BlockType.RENDER_WHITE : wallType;

        for (int y = 1; y <= height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    boolean isWall = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
                    if (!isWall) continue;

                    boolean isFront = (dz == 0);
                    BlockType block;

                    if (isFront && (y == 1 || y == 2)) {
                        // Ground floor front — display window area
                        boolean isCornerPillar = (dx == 0 || dx == width - 1);
                        if (isCornerPillar) {
                            block = wallType; // structural pillars always solid
                        } else if (shopStyle == 0) {
                            // Large display window: glass across nearly the whole front
                            block = BlockType.GLASS;
                        } else if (shopStyle == 1) {
                            // Narrow: glass only in centre half
                            boolean inCentre = dx >= width / 4 && dx < (width * 3 / 4);
                            block = inCentre ? BlockType.GLASS : wallType;
                        } else {
                            // Solid lower half (e.g. a bookies or pawn shop with opaque frontage)
                            block = wallType;
                        }
                    } else if (y > 2) {
                        // Upper floors: use potentially varied material
                        block = (y == height && !isFront) ? upperWall : wallType;
                        // Occasional upper-floor window on front
                        if (isFront && y == 3 && width >= 6) {
                            boolean inWindowBand = dx >= 1 && dx <= width - 2;
                            if (inWindowBand) block = BlockType.GLASS;
                        }
                    } else {
                        block = wallType;
                    }
                    world.setBlock(x + dx, y, z + dz, block);
                }
            }
        }
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                world.setBlock(x + dx, height + 1, z + dz, BlockType.CONCRETE); // Flat shop roof
                world.setBlock(x + dx, 0, z + dz, BlockType.PAVEMENT);
            }
        }
        // Door (cleared into display window area)
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

    private void generateBuildingInteriors(World world,
                                            int sx, int sz, int nx, int nz,
                                            int esx, int enz,
                                            int jobX, int jobZ,
                                            int gpX, int gpZ,
                                            int libX, int libZ) {
        // South-side high street slots
        generateShopInterior(world, sx,      sz, 7, 8, BlockType.LINO_GREEN, BlockType.COUNTER);
        generateShopInterior(world, sx + 8,  sz, 6, 8, BlockType.LINO_GREEN, BlockType.SHELF);
        generateShopInterior(world, sx + 15, sz, 7, 8, BlockType.CARPET,     BlockType.SHELF);
        generateShopInterior(world, sx + 23, sz, 6, 8, BlockType.CARPET,     BlockType.COUNTER);
        generateShopInterior(world, sx + 30, sz, 7, 8, BlockType.CARPET,     BlockType.COUNTER);
        generateShopInterior(world, sx + 38, sz, 7, 8, BlockType.LINO_GREEN, BlockType.COUNTER);

        // North-side high street slots
        generateShopInterior(world, nx,      nz, 7, 8, BlockType.LINO_GREEN, BlockType.SHELF);
        generateShopInterior(world, nx + 8,  nz, 8, 8, BlockType.LINO_GREEN, BlockType.STONE);
        generatePubInterior(world,  nx + 17, nz, 8, 8);
        generateShopInterior(world, nx + 26, nz, 7, 8, BlockType.CARPET,     BlockType.SHELF);

        // Extended south / north
        generateShopInterior(world, esx, sz, 7, 8, BlockType.LINO_GREEN, BlockType.COUNTER);
        generateShopInterior(world, enz, nz, 7, 8, BlockType.LINO_GREEN, BlockType.SHELF);

        // Nando's
        generatePubInterior(world, esx + 24, sz, 8, 10);

        // Corner shop
        generateShopInterior(world, enz + 9, nz, 7, 8, BlockType.LINO_GREEN, BlockType.SHELF);

        // Wetherspoons
        generatePubInterior(world, esx + 49, sz, 16, 14);

        // Library
        generateLibraryInterior(world, libX, libZ, 16, 12);

        // JobCentre
        generateOfficeInterior(world, jobX, jobZ, 12, 12);

        // GP Surgery
        generateOfficeInterior(world, gpX, gpZ, 14, 10);
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

    private void generateStreetFurniture(World world, int sx, int sz, int nx, int nz) {
        // Lamp posts along the main high street (every 10 blocks on pavement)
        for (int x = sx; x < sx + 110; x += 10) {
            generateLampPost(world, x, sz - 1); // South pavement
            generateLampPost(world, x, nz + 7); // North pavement
        }

        // Bins outside shops
        generateBin(world, sx + 2,  sz - 1);
        generateBin(world, sx + 16, sz - 1);
        generateBin(world, sx + 32, sz - 1);
        generateBin(world, sx + 50, sz - 1);
        generateBin(world, sx + 75, sz - 1);
        generateBin(world, sx + 95, sz - 1);

        // Benches along high street (north pavement)
        generateBench(world, nx + 10, nz + 7);
        generateBench(world, nx + 30, nz + 7);
        generateBench(world, nx + 55, nz + 7);
        generateBench(world, nx + 80, nz + 7);

        // Bus shelter
        generateBusShelter(world, sx + 65, sz - 1);

        // Bollards at street junctions
        for (int z = sz - 5; z <= sz - 2; z++) {
            generateBollard(world, sx - 1, z);
        }
        for (int z = sz - 5; z <= sz - 2; z++) {
            generateBollard(world, sx + 45, z);
        }

        // Phone box near the park entrance
        generatePhoneBox(world, 16, -16);

        // Post box outside newsagent
        generatePostBox(world, nx + 43, nz - 1);
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

        // Statue near the centre of the park (offset slightly from 0,0 to avoid conflicts)
        generateStatue(world, 4, 4);
        world.addLandmark(new Landmark(LandmarkType.STATUE, 4, 0, 4, 3, 5, 3));
    }

    /**
     * Generate a stone statue — a 3x3 plinth topped by a human-like figure.
     * The statue is 5 blocks tall: a 1-block-tall plinth base (3x3), a 1-block
     * pedestal (1x1), and a 3-block-tall stylised figure (1x1 legs, 1x1 torso,
     * 1x1 head) all made of STATUE blocks.
     *
     * @param x western edge of the 3x3 plinth base
     * @param z northern edge of the 3x3 plinth base
     */
    private void generateStatue(World world, int x, int z) {
        // Plinth base: 3x3, 1 block tall at ground level (y=1)
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                world.setBlock(x + dx, 1, z + dz, BlockType.STATUE);
            }
        }
        // Pedestal: 1x1, 1 block tall at y=2 (centre of plinth)
        world.setBlock(x + 1, 2, z + 1, BlockType.STATUE);
        // Figure legs: y=3
        world.setBlock(x + 1, 3, z + 1, BlockType.STATUE);
        // Figure torso: y=4
        world.setBlock(x + 1, 4, z + 1, BlockType.STATUE);
        // Figure head: y=5
        world.setBlock(x + 1, 5, z + 1, BlockType.STATUE);
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

    // ==================== UNDERGROUND STRUCTURES ====================

    /**
     * Generate sewer tunnels running beneath the main streets.
     * Tunnels are 2 blocks tall (SEWER_FLOOR_Y to SEWER_CEILING_Y) carved from stone,
     * with concrete floors and brick walls. They run along the major street grid.
     */
    private void generateSewerTunnels(World world, int sx, int sz, int nx, int nz) {
        int streetSpacing = 20;
        int streetExtent = 120; // Sewers only under the built-up area

        // Tunnels running east-west (below z-direction streets)
        for (int z = -streetExtent; z <= streetExtent; z += streetSpacing) {
            for (int x = -streetExtent; x <= streetExtent; x++) {
                carveTunnelSegment(world, x, z);
            }
        }

        // Tunnels running north-south (below x-direction streets)
        for (int x = -streetExtent; x <= streetExtent; x += streetSpacing) {
            for (int z = -streetExtent; z <= streetExtent; z++) {
                carveTunnelSegment(world, x, z);
            }
        }

        // Register sewer tunnel as a landmark (approximate centre of tunnel network)
        world.addLandmark(new Landmark(LandmarkType.SEWER_TUNNEL, -streetExtent, SEWER_FLOOR_Y,
                -streetExtent, streetExtent * 2, SEWER_CEILING_Y - SEWER_FLOOR_Y + 1, streetExtent * 2));
    }

    /**
     * Carve a single segment of sewer tunnel at (x, z) from SEWER_FLOOR_Y to SEWER_CEILING_Y.
     * Floor is concrete, walls are brick, ceiling and interior are air.
     */
    private void carveTunnelSegment(World world, int x, int z) {
        // Floor
        world.setBlock(x, SEWER_FLOOR_Y, z, BlockType.CONCRETE);
        // Carved interior (air)
        for (int y = SEWER_FLOOR_Y + 1; y <= SEWER_CEILING_Y; y++) {
            world.setBlock(x, y, z, BlockType.AIR);
        }
        // Brick ceiling
        world.setBlock(x, SEWER_CEILING_Y + 1, z, BlockType.BRICK);
    }

    /**
     * Generate an underground bunker beneath the park.
     * The bunker is a large concrete-walled room with brick floors and interior rooms,
     * located at BUNKER_FLOOR_Y to BUNKER_TOP_Y directly beneath the park.
     * Access is via ladders from the surface.
     */
    private void generateUndergroundBunker(World world) {
        // Bunker extends from -20 to +20 in X and Z (beneath and around the park)
        int bunkerHalfWidth = 20;
        int bunkerStartX = -bunkerHalfWidth;
        int bunkerEndX   = bunkerHalfWidth;
        int bunkerStartZ = -bunkerHalfWidth;
        int bunkerEndZ   = bunkerHalfWidth;
        int bunkerWidth  = bunkerEndX - bunkerStartX;
        int bunkerDepth  = bunkerEndZ - bunkerStartZ;
        int bunkerHeight = BUNKER_TOP_Y - BUNKER_FLOOR_Y + 1;

        // Carve out the bunker interior (air)
        for (int x = bunkerStartX; x < bunkerEndX; x++) {
            for (int z = bunkerStartZ; z < bunkerEndZ; z++) {
                for (int y = BUNKER_FLOOR_Y; y <= BUNKER_TOP_Y; y++) {
                    world.setBlock(x, y, z, BlockType.AIR);
                }
                // Concrete floor
                world.setBlock(x, BUNKER_FLOOR_Y - 1, z, BlockType.CONCRETE);
            }
        }

        // Build bunker walls (concrete outer shell, brick inner lining)
        for (int x = bunkerStartX - 1; x <= bunkerEndX; x++) {
            for (int z = bunkerStartZ - 1; z <= bunkerEndZ; z++) {
                for (int y = BUNKER_FLOOR_Y - 1; y <= BUNKER_TOP_Y + 1; y++) {
                    // Only set wall blocks on the perimeter
                    boolean isEdgeX = (x == bunkerStartX - 1 || x == bunkerEndX);
                    boolean isEdgeZ = (z == bunkerStartZ - 1 || z == bunkerEndZ);
                    boolean isEdgeY = (y == BUNKER_FLOOR_Y - 1 || y == BUNKER_TOP_Y + 1);
                    if (isEdgeX || isEdgeZ || isEdgeY) {
                        world.setBlock(x, y, z, BlockType.CONCRETE);
                    }
                }
            }
        }

        // Internal dividing walls to create rooms
        // Main corridor running east-west
        int corridorZ = 0;
        // North room divider
        for (int x = bunkerStartX; x < bunkerEndX; x++) {
            for (int y = BUNKER_FLOOR_Y; y <= BUNKER_TOP_Y; y++) {
                world.setBlock(x, y, -8, BlockType.BRICK);
            }
        }
        // South room divider
        for (int x = bunkerStartX; x < bunkerEndX; x++) {
            for (int y = BUNKER_FLOOR_Y; y <= BUNKER_TOP_Y; y++) {
                world.setBlock(x, y, 8, BlockType.BRICK);
            }
        }
        // Door gaps in north divider
        world.setBlock(-2, BUNKER_FLOOR_Y, -8, BlockType.AIR);
        world.setBlock(-1, BUNKER_FLOOR_Y, -8, BlockType.AIR);
        world.setBlock(-2, BUNKER_FLOOR_Y + 1, -8, BlockType.AIR);
        world.setBlock(-1, BUNKER_FLOOR_Y + 1, -8, BlockType.AIR);
        // Door gaps in south divider
        world.setBlock(-2, BUNKER_FLOOR_Y, 8, BlockType.AIR);
        world.setBlock(-1, BUNKER_FLOOR_Y, 8, BlockType.AIR);
        world.setBlock(-2, BUNKER_FLOOR_Y + 1, 8, BlockType.AIR);
        world.setBlock(-1, BUNKER_FLOOR_Y + 1, 8, BlockType.AIR);

        // West room divider
        for (int z = bunkerStartZ; z < bunkerEndZ; z++) {
            for (int y = BUNKER_FLOOR_Y; y <= BUNKER_TOP_Y; y++) {
                world.setBlock(-10, y, z, BlockType.BRICK);
            }
        }
        // Door gap in west divider
        world.setBlock(-10, BUNKER_FLOOR_Y, -2, BlockType.AIR);
        world.setBlock(-10, BUNKER_FLOOR_Y, -1, BlockType.AIR);
        world.setBlock(-10, BUNKER_FLOOR_Y + 1, -2, BlockType.AIR);
        world.setBlock(-10, BUNKER_FLOOR_Y + 1, -1, BlockType.AIR);

        // Bunker floor furniture: tables, shelves, counters
        // Central room tables
        for (int x = -8; x <= -3; x += 3) {
            world.setBlock(x, BUNKER_FLOOR_Y, -5, BlockType.TABLE);
            world.setBlock(x, BUNKER_FLOOR_Y, -4, BlockType.TABLE);
        }
        // West room shelves
        for (int z = bunkerStartZ + 2; z < -8; z += 3) {
            world.setBlock(bunkerStartX + 1, BUNKER_FLOOR_Y, z, BlockType.SHELF);
            world.setBlock(bunkerStartX + 2, BUNKER_FLOOR_Y, z, BlockType.SHELF);
        }
        // Lino floor in central corridor
        for (int x = -9; x < bunkerEndX; x++) {
            for (int z = -7; z <= 7; z++) {
                if (world.getBlock(x, BUNKER_FLOOR_Y - 1, z) == BlockType.CONCRETE) {
                    world.setBlock(x, BUNKER_FLOOR_Y - 1, z, BlockType.LINOLEUM);
                }
            }
        }

        // Ladder access shafts from surface to bunker
        // Shaft 1: at (5, 0) — inside the park fence
        generateBunkerAccessShaft(world, 5, 5);
        // Shaft 2: at (-5, 0) — inside the park fence
        generateBunkerAccessShaft(world, -5, 5);

        // Register bunker as a landmark
        world.addLandmark(new Landmark(LandmarkType.UNDERGROUND_BUNKER,
                bunkerStartX, BUNKER_FLOOR_Y - 1, bunkerStartZ,
                bunkerWidth, bunkerHeight + 2, bunkerDepth));
    }

    /**
     * Generate a vertical access shaft from the surface (y=0) down to the bunker ceiling.
     * The shaft is lined with brick and has ladders.
     */
    private void generateBunkerAccessShaft(World world, int x, int z) {
        // Carve the shaft from surface down to bunker top
        for (int y = BUNKER_TOP_Y; y <= 0; y++) {
            world.setBlock(x, y, z, BlockType.AIR);
        }
        // Brick lining on sides (not all sides, just marker blocks)
        for (int y = BUNKER_TOP_Y + 1; y <= -1; y++) {
            world.setBlock(x - 1, y, z, BlockType.BRICK);
            world.setBlock(x + 1, y, z, BlockType.BRICK);
            world.setBlock(x, y, z - 1, BlockType.BRICK);
            world.setBlock(x, y, z + 1, BlockType.BRICK);
        }
        // Ladders down the shaft
        for (int y = BUNKER_TOP_Y + 1; y <= -1; y++) {
            world.setBlock(x, y, z, BlockType.LADDER);
        }
    }

    // ==================== UNDERGROUND MINERALS — Issue #691 ====================

    /**
     * Generate mineral veins in the underground stone layers.
     *
     * Three mineral types are distributed through the underground:
     *   - COAL_ORE: common, found from y=-4 down to y=-20 (relatively shallow)
     *   - IRON_ORE: uncommon, found from y=-10 down to y=-28 (deeper)
     *   - FLINT:    rare, found from y=-15 down to BEDROCK_DEPTH+2 (deepest)
     *
     * Each vein is a small cluster of ore blocks scattered in the stone layer.
     * Veins only replace STONE blocks (not bedrock, sewer tunnels, or bunker blocks).
     */
    private void generateMineralVeins(World world) {
        Random mineralRng = new Random(seed ^ 0x4D494E455F56454EL); // "MINE_VEN"

        int halfWorld = WORLD_SIZE / 2;
        // Limit vein placement to built-up area for performance (-200 to +200)
        int extent = 200;

        // Coal veins — common, shallow underground (below sewer but above bedrock)
        int coalVeinCount = 800;
        for (int i = 0; i < coalVeinCount; i++) {
            int vx = mineralRng.nextInt(extent * 2) - extent;
            int vz = mineralRng.nextInt(extent * 2) - extent;
            int vy = -(4 + mineralRng.nextInt(17)); // y=-4 to y=-20
            placeOreVein(world, vx, vy, vz, BlockType.COAL_ORE, 3 + mineralRng.nextInt(3), mineralRng);
        }

        // Iron ore veins — uncommon, deeper
        int ironVeinCount = 400;
        for (int i = 0; i < ironVeinCount; i++) {
            int vx = mineralRng.nextInt(extent * 2) - extent;
            int vz = mineralRng.nextInt(extent * 2) - extent;
            int vy = -(10 + mineralRng.nextInt(19)); // y=-10 to y=-28
            placeOreVein(world, vx, vy, vz, BlockType.IRON_ORE, 2 + mineralRng.nextInt(3), mineralRng);
        }

        // Flint nodules — rare, deepest layer
        int flintVeinCount = 200;
        for (int i = 0; i < flintVeinCount; i++) {
            int vx = mineralRng.nextInt(extent * 2) - extent;
            int vz = mineralRng.nextInt(extent * 2) - extent;
            int vy = -(15 + mineralRng.nextInt(BEDROCK_DEPTH + 15 - 2)); // y=-15 down to BEDROCK_DEPTH+2
            if (vy <= BEDROCK_DEPTH + 1) vy = BEDROCK_DEPTH + 2; // clamp above bedrock
            placeOreVein(world, vx, vy, vz, BlockType.FLINT, 2 + mineralRng.nextInt(2), mineralRng);
        }
    }

    /**
     * Place a small cluster of ore blocks centred at (cx, cy, cz).
     * Only replaces existing STONE blocks to avoid disturbing tunnels, the bunker,
     * or other underground structures. Vein size controls cluster radius.
     */
    private void placeOreVein(World world, int cx, int cy, int cz, BlockType oreType, int size, Random rng) {
        for (int i = 0; i < size * 3; i++) {
            int dx = rng.nextInt(size * 2 + 1) - size;
            int dy = rng.nextInt(3) - 1; // slight vertical spread
            int dz = rng.nextInt(size * 2 + 1) - size;
            int bx = cx + dx;
            int by = cy + dy;
            int bz = cz + dz;
            // Only replace stone — preserve bedrock, air (tunnel/bunker interiors), etc.
            if (world.getBlock(bx, by, bz) == BlockType.STONE) {
                world.setBlock(bx, by, bz, oreType);
            }
        }
    }

    // ==================== FLAG POLES — Issue #658 ====================

    /**
     * Place animated flag poles on the roofs of the six key civic buildings.
     *
     * Parameters come in (x, z, roofHeight) triples for each building.
     * A 3-block IRON_FENCE pillar is placed on the roof, and the flag position
     * is registered at the top of the pillar so FlagRenderer can animate it.
     *
     * Flag colour schemes chosen to suggest British civic identity:
     *   Office     — navy/white (corporate colours)
     *   JobCentre  — government blue/white
     *   Police     — police blue/silver
     *   Fire       — fire-engine red/yellow
     *   School     — royal blue/sunshine yellow
     *   Council    — deep red/white
     */
    private void generateFlagPoles(World world,
                                   int offX,    int offZ,    int offH,
                                   int jobX,    int jobZ,    int jobH,
                                   int pstnX,   int pstnZ,   int pstnH,
                                   int fsX,     int fsZ,     int fsH,
                                   int schoolX, int schoolZ, int schoolH,
                                   int cf1X,    int cf1Z,    int cf1H) {
        // Office building — navy / white
        generateFlagPole(world,
            offX + 2,  offH, offZ + 2,
            0.05f, 0.10f, 0.45f,   1.00f, 1.00f, 1.00f,
            0.0f);

        // JobCentre — government blue / white
        generateFlagPole(world,
            jobX + 2,  jobH, jobZ + 2,
            0.00f, 0.30f, 0.60f,   0.95f, 0.95f, 0.95f,
            1.05f);

        // Police station — police blue / silver
        generateFlagPole(world,
            pstnX + 2, pstnH, pstnZ + 2,
            0.08f, 0.22f, 0.58f,   0.78f, 0.80f, 0.85f,
            2.10f);

        // Fire station — red / yellow
        generateFlagPole(world,
            fsX + 2,   fsH, fsZ + 2,
            0.88f, 0.10f, 0.08f,   0.98f, 0.82f, 0.10f,
            3.14f);

        // Primary school — royal blue / yellow
        generateFlagPole(world,
            schoolX + 2, schoolH, schoolZ + 2,
            0.10f, 0.30f, 0.75f,   0.95f, 0.80f, 0.05f,
            4.19f);

        // Council flats — deep red / white
        generateFlagPole(world,
            cf1X + 2,  cf1H, cf1Z + 2,
            0.70f, 0.05f, 0.05f,   0.95f, 0.95f, 0.95f,
            5.24f);
    }

    /**
     * Place a single flag pole: a 3-block IRON_FENCE pillar on top of a building,
     * and register the flag attachment position with the world.
     *
     * @param world  the world to place blocks in
     * @param x      X of the pole base (on the building roof)
     * @param roofY  Y of the building roof surface
     * @param z      Z of the pole base
     * @param r1     red component of hoist colour (0–1)
     * @param g1     green component of hoist colour
     * @param b1     blue component of hoist colour
     * @param r2     red component of fly colour (0–1)
     * @param g2     green component of fly colour
     * @param b2     blue component of fly colour
     * @param phase  phase offset in radians for the wave animation
     */
    private void generateFlagPole(World world,
                                  int x, int roofY, int z,
                                  float r1, float g1, float b1,
                                  float r2, float g2, float b2,
                                  float phase) {
        // 3-block tall IRON_FENCE pole above the roof
        int poleHeight = 3;
        for (int dy = 1; dy <= poleHeight; dy++) {
            world.setBlock(x, roofY + dy, z, BlockType.IRON_FENCE);
        }
        // Register the flag at the top of the pole
        float flagY = roofY + poleHeight + 0.5f;
        world.addFlagPosition(new FlagPosition(
            x + 0.5f, flagY, z + 0.5f,
            r1, g1, b1,
            r2, g2, b2,
            phase));
    }

    // ==================== NON-BLOCK 3D PROPS — Issue #669 ====================

    /**
     * Place all unique non-block-based 3D decorative props throughout the world.
     *
     * Props are registered with the world so {@link PropRenderer} can render them.
     * They are positioned at ground level (y = 1) unless the building has raised the
     * ground slightly — in those cases y = 0 is fine because the prop sits on the
     * pavement surface.
     *
     * @param world   the world to register props in
     * @param sx      high-street south X start
     * @param sz      high-street south Z
     * @param nx      high-street north X start
     * @param nz      high-street north Z
     * @param offX    office building X
     * @param offZ    office building Z
     * @param jobX    JobCentre X
     * @param jobZ    JobCentre Z
     * @param pstnX   police station X
     * @param pstnZ   police station Z
     * @param rx      residential row X start
     * @param rz1     residential row 1 Z
     * @param rz2     residential row 2 Z
     * @param indX    industrial estate X
     * @param indZ    industrial estate Z
     * @param smX     supermarket X
     * @param smZ     supermarket Z
     */
    private void generateProps(World world,
                               int sx, int sz, int nx, int nz,
                               int offX, int offZ,
                               int jobX, int jobZ,
                               int pstnX, int pstnZ,
                               int rx, int rz1, int rz2,
                               int indX, int indZ,
                               int smX, int smZ) {
        float y = 1.0f; // props sit at ground / pavement level

        // ── Phone boxes ──────────────────────────────────────────────────────
        // High street south — outside the off-licence / Greggs end
        addProp(world, sx + 3,  y, sz - 1,  PropType.PHONE_BOX, 0f);
        // High street north — near the newsagent
        addProp(world, nx + 44, y, nz - 1,  PropType.PHONE_BOX, 180f);
        // Near JobCentre entrance
        addProp(world, jobX - 2, y, jobZ + 5, PropType.PHONE_BOX, 90f);
        // Near police station
        addProp(world, pstnX - 2, y, pstnZ + 4, PropType.PHONE_BOX, 270f);

        // ── Post boxes ───────────────────────────────────────────────────────
        // High street south pavement
        addProp(world, sx + 12, y, sz - 1,  PropType.POST_BOX, 0f);
        // High street north pavement
        addProp(world, nx + 20, y, nz - 1,  PropType.POST_BOX, 0f);
        // Residential row 1 — pavement end
        addProp(world, rx + 5,  y, rz1 - 1, PropType.POST_BOX, 180f);
        // Outside supermarket
        addProp(world, smX + 2, y, smZ - 1, PropType.POST_BOX, 0f);

        // ── Park benches ─────────────────────────────────────────────────────
        // Two benches on the south side of the park path
        addProp(world,  5f, y,  8f, PropType.PARK_BENCH, 0f);
        addProp(world, -5f, y,  8f, PropType.PARK_BENCH, 0f);
        // Two benches on the north side
        addProp(world,  5f, y, -8f, PropType.PARK_BENCH, 180f);
        addProp(world, -5f, y, -8f, PropType.PARK_BENCH, 180f);
        // One bench near the pond (east side)
        addProp(world, 10f, y,  2f, PropType.PARK_BENCH, 90f);

        // ── Bus shelters ─────────────────────────────────────────────────────
        // High street south bus stop
        addProp(world, sx + 20, y, sz - 2, PropType.BUS_SHELTER, 180f);
        // High street north bus stop
        addProp(world, nx + 30, y, nz + 9, PropType.BUS_SHELTER, 0f);
        // Near JobCentre road
        addProp(world, jobX + 5, y, jobZ - 3, PropType.BUS_SHELTER, 180f);
        // Near residential row
        addProp(world, rx + 20, y, rz1 - 2, PropType.BUS_SHELTER, 180f);

        // ── Bollards ─────────────────────────────────────────────────────────
        // Office building entrance — a row of 3 bollards
        for (int i = 0; i < 3; i++) {
            addProp(world, offX + 2 + i * 2, y, offZ - 1, PropType.BOLLARD, 0f);
        }
        // JobCentre entrance
        for (int i = 0; i < 3; i++) {
            addProp(world, jobX + 2 + i * 2, y, jobZ - 1, PropType.BOLLARD, 0f);
        }
        // Police station front
        for (int i = 0; i < 3; i++) {
            addProp(world, pstnX + 1 + i * 2, y, pstnZ - 1, PropType.BOLLARD, 0f);
        }
        // High street pedestrian zone — scattered bollards
        addProp(world, sx + 2,  y, sz + 8,  PropType.BOLLARD, 0f);
        addProp(world, sx + 14, y, sz + 8,  PropType.BOLLARD, 0f);

        // ── Street lamps ─────────────────────────────────────────────────────
        // High street south — every ~10 blocks
        for (int i = 0; i < 5; i++) {
            addProp(world, sx + 2 + i * 10, y, sz - 1, PropType.STREET_LAMP, 0f);
        }
        // High street north — every ~10 blocks
        for (int i = 0; i < 4; i++) {
            addProp(world, nx + 2 + i * 10, y, nz + 8, PropType.STREET_LAMP, 180f);
        }
        // Park perimeter — 4 lamps at corners
        addProp(world, -12f, y,  12f, PropType.STREET_LAMP, 0f);
        addProp(world,  12f, y,  12f, PropType.STREET_LAMP, 0f);
        addProp(world, -12f, y, -12f, PropType.STREET_LAMP, 0f);
        addProp(world,  12f, y, -12f, PropType.STREET_LAMP, 0f);

        // ── Litter bins ──────────────────────────────────────────────────────
        // High street — pair near each bus stop
        addProp(world, sx + 18, y, sz - 1, PropType.LITTER_BIN, 0f);
        addProp(world, sx + 35, y, sz - 1, PropType.LITTER_BIN, 0f);
        addProp(world, nx + 25, y, nz + 8, PropType.LITTER_BIN, 180f);
        // Park entrance
        addProp(world, -14f, y, 0f,  PropType.LITTER_BIN, 90f);
        addProp(world,  14f, y, 0f,  PropType.LITTER_BIN, 270f);
        // Near supermarket entrance
        addProp(world, smX + 5, y, smZ - 1, PropType.LITTER_BIN, 0f);

        // ── Market stalls ────────────────────────────────────────────────────
        // Market area between the high street rows — a cluster of 4 stalls
        addProp(world, sx + 5,  y + 0.1f, sz + 12, PropType.MARKET_STALL, 0f);
        addProp(world, sx + 12, y + 0.1f, sz + 12, PropType.MARKET_STALL, 0f);
        addProp(world, sx + 19, y + 0.1f, sz + 12, PropType.MARKET_STALL, 0f);
        addProp(world, sx + 26, y + 0.1f, sz + 12, PropType.MARKET_STALL, 180f);

        // ── Picnic tables ────────────────────────────────────────────────────
        // Park area — 2 picnic tables near the pond
        addProp(world, -3f, y, -3f, PropType.PICNIC_TABLE, 0f);
        addProp(world,  4f, y, -4f, PropType.PICNIC_TABLE, 45f);
        // Near the community centre
        addProp(world, rx - 12f, y, rz1 + 14f, PropType.PICNIC_TABLE, 0f);

        // ── Bike racks ───────────────────────────────────────────────────────
        // Outside the library
        addProp(world, jobX - 18f, y, jobZ - 15f, PropType.BIKE_RACK, 0f);
        addProp(world, jobX - 14f, y, jobZ - 15f, PropType.BIKE_RACK, 0f);
        // Outside the supermarket
        addProp(world, smX + 8f, y, smZ - 1f, PropType.BIKE_RACK, 90f);
        addProp(world, smX + 8f, y, smZ + 3f, PropType.BIKE_RACK, 90f);
        // Outside the primary school
        addProp(world, indX + 2f, y, indZ - 41f, PropType.BIKE_RACK, 0f);
        addProp(world, indX + 6f, y, indZ - 41f, PropType.BIKE_RACK, 0f);

        // ── Shopping trolleys ────────────────────────────────────────────────
        // Abandoned near the supermarket car park
        addProp(world, smX + 18f, y, smZ + 5f,  PropType.SHOPPING_TROLLEY, 15f);
        addProp(world, smX + 20f, y, smZ + 2f,  PropType.SHOPPING_TROLLEY, 195f);
        // One dumped in the canal-adjacent area (very British)
        addProp(world, -8f, y, rz2 - 30f,       PropType.SHOPPING_TROLLEY, 42f);

        // ── Statue ───────────────────────────────────────────────────────────
        // Heroic monument at the centre of the park
        addProp(world, 0f, y, 0f, PropType.STATUE, 0f);
    }

    /**
     * Helper: register a single prop placement with the world.
     *
     * @param world      the world to register the prop in
     * @param x          world-space X position of prop base
     * @param y          world-space Y position of prop base (ground level)
     * @param z          world-space Z position of prop base
     * @param type       which prop type to place
     * @param rotationY  Y-axis rotation in degrees
     */
    private void addProp(World world, float x, float y, float z,
                         PropType type, float rotationY) {
        world.addPropPosition(new PropPosition(x, y, z, type, rotationY));
    }
}

package ragamuffin.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.test.HeadlessTestHelper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for underground world depth and underground structures (Issue #503).
 */
public class UndergroundTest {

    private World world;
    private WorldGenerator generator;

    @BeforeEach
    public void setUp() {
        HeadlessTestHelper.initHeadless();
        world = new World(12345);
        generator = new WorldGenerator(12345);
    }

    /**
     * Bedrock should be at BEDROCK_DEPTH (y=-32), not the old y=-6.
     * Checks a position well outside the bunker and sewer tunnel areas.
     */
    @Test
    public void testIncreasedWorldDepth() {
        generator.generateWorld(world);

        // Use a position outside the bunker (x=50, z=50) and not on a street grid
        // so no sewer tunnel is carved there (streets are at multiples of 20).
        int testX = 50;
        int testZ = 51; // Avoid street at z=40 and z=60 (multiples of 20)

        // Bedrock should be at the new deeper level
        BlockType bedrockBlock = world.getBlock(testX, WorldGenerator.BEDROCK_DEPTH, testZ);
        assertEquals(BlockType.BEDROCK, bedrockBlock,
                "Bedrock should be at y=" + WorldGenerator.BEDROCK_DEPTH);

        // Old bedrock position should now be stone
        BlockType oldBedrockPos = world.getBlock(testX, -6, testZ);
        assertEquals(BlockType.STONE, oldBedrockPos,
                "Old bedrock position (y=-6) should now be STONE");

        // There should be stone all the way from bedrock+1 to below sewer level
        // (Sewer tunnels only run at street grid positions, not here)
        for (int y = WorldGenerator.BEDROCK_DEPTH + 1; y < WorldGenerator.SEWER_FLOOR_Y; y++) {
            BlockType block = world.getBlock(testX, y, testZ);
            assertEquals(BlockType.STONE, block,
                    "Stone should exist at y=" + y + " (underground layer at x=" + testX + ",z=" + testZ + ")");
        }
    }

    /**
     * No air should exist at positions deeper than y=-2 that are not part of
     * underground structures (sewers or bunker). The stone/bedrock should be intact.
     */
    @Test
    public void testUndergroundSolidExceptStructures() {
        generator.generateWorld(world);

        // Check a location far from streets and the bunker (far corner of map)
        // Should be completely solid stone from surface down to bedrock
        int testX = 200;
        int testZ = 200;

        // Deep stone check (far below sewer level)
        for (int y = WorldGenerator.BEDROCK_DEPTH + 1; y < WorldGenerator.SEWER_FLOOR_Y; y++) {
            BlockType block = world.getBlock(testX, y, testZ);
            assertEquals(BlockType.STONE, block,
                    "Stone should exist at (" + testX + "," + y + "," + testZ + ")");
        }
    }

    /**
     * Sewer tunnels should exist below the main streets.
     * They run at SEWER_FLOOR_Y to SEWER_CEILING_Y.
     */
    @Test
    public void testSewerTunnelsExist() {
        generator.generateWorld(world);

        // Check that a sewer tunnel exists below street grid
        // Streets run every 20 blocks; check z=0 (a street axis) at x=0
        // The sewer should be carved out between SEWER_FLOOR_Y+1 and SEWER_CEILING_Y
        boolean foundAirInSewer = false;
        for (int x = -100; x <= 100; x++) {
            for (int y = WorldGenerator.SEWER_FLOOR_Y + 1; y <= WorldGenerator.SEWER_CEILING_Y; y++) {
                BlockType block = world.getBlock(x, y, 0);
                if (block == BlockType.AIR) {
                    foundAirInSewer = true;
                    break;
                }
            }
            if (foundAirInSewer) break;
        }
        assertTrue(foundAirInSewer,
                "Sewer tunnels should exist below streets (air at sewer height)");

        // Also check sewer floor is concrete
        boolean foundConcreteFloor = false;
        for (int x = -100; x <= 100; x++) {
            BlockType floorBlock = world.getBlock(x, WorldGenerator.SEWER_FLOOR_Y, 0);
            if (floorBlock == BlockType.CONCRETE) {
                foundConcreteFloor = true;
                break;
            }
        }
        assertTrue(foundConcreteFloor,
                "Sewer floor should be concrete");
    }

    /**
     * The underground bunker should exist below the park.
     * It should be carved out (air interior) with concrete/brick walls.
     */
    @Test
    public void testUndergroundBunkerExists() {
        generator.generateWorld(world);

        // Verify the bunker landmark is registered
        Landmark bunker = world.getLandmark(LandmarkType.UNDERGROUND_BUNKER);
        assertNotNull(bunker, "Underground bunker landmark should exist");

        // Bunker is beneath the park (0,0) area
        // Check for air at bunker interior height below the park
        boolean foundAirInBunker = false;
        for (int x = -15; x <= 15; x++) {
            for (int z = -15; z <= 15; z++) {
                for (int y = WorldGenerator.BUNKER_FLOOR_Y; y <= WorldGenerator.BUNKER_TOP_Y; y++) {
                    BlockType block = world.getBlock(x, y, z);
                    if (block == BlockType.AIR) {
                        foundAirInBunker = true;
                        break;
                    }
                }
                if (foundAirInBunker) break;
            }
            if (foundAirInBunker) break;
        }
        assertTrue(foundAirInBunker,
                "Underground bunker should have air interior at bunker height");
    }

    /**
     * The sewer tunnel landmark should be registered in the world.
     */
    @Test
    public void testSewerTunnelLandmarkRegistered() {
        generator.generateWorld(world);

        Landmark sewer = world.getLandmark(LandmarkType.SEWER_TUNNEL);
        assertNotNull(sewer, "Sewer tunnel landmark should be registered");
    }

    /**
     * Bunker access shaft: there should be ladders going from surface down to the bunker.
     */
    @Test
    public void testBunkerAccessShaftHasLadders() {
        generator.generateWorld(world);

        // Access shafts are at (5,5) and (-5,5)
        boolean foundLadder = false;
        // Check shaft at (5,5)
        for (int y = WorldGenerator.BUNKER_TOP_Y + 1; y <= -1; y++) {
            BlockType block = world.getBlock(5, y, 5);
            if (block == BlockType.LADDER) {
                foundLadder = true;
                break;
            }
        }
        assertTrue(foundLadder,
                "Bunker access shaft should contain ladders");
    }

    /**
     * LandmarkType enum should contain the new underground types.
     */
    @Test
    public void testUndergroundLandmarkTypesExist() {
        // Just verify the enum values exist without NPE
        assertNotNull(LandmarkType.UNDERGROUND_BUNKER);
        assertNotNull(LandmarkType.SEWER_TUNNEL);
    }
}

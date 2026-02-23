package ragamuffin.world;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.test.HeadlessTestHelper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WorldGenerator.
 */
public class WorldGeneratorTest {

    private World world;
    private WorldGenerator generator;

    @BeforeEach
    public void setUp() {
        HeadlessTestHelper.initHeadless();
        world = new World(12345);
        generator = new WorldGenerator(12345);
    }

    @Test
    public void testWorldGeneratorCreatesLandmarks() {
        generator.generateWorld(world);

        // Verify all required landmarks exist
        assertNotNull(world.getLandmark(LandmarkType.PARK));
        assertNotNull(world.getLandmark(LandmarkType.GREGGS));
        assertNotNull(world.getLandmark(LandmarkType.OFF_LICENCE));
        assertNotNull(world.getLandmark(LandmarkType.CHARITY_SHOP));
        assertNotNull(world.getLandmark(LandmarkType.JEWELLER));
        assertNotNull(world.getLandmark(LandmarkType.OFFICE_BUILDING));
        assertNotNull(world.getLandmark(LandmarkType.JOB_CENTRE));
    }

    @Test
    public void testParkIsAtWorldCenter() {
        generator.generateWorld(world);

        Landmark park = world.getLandmark(LandmarkType.PARK);
        assertNotNull(park);

        Vector3 parkPos = park.getPosition();
        // Park should be centered near origin
        assertTrue(Math.abs(parkPos.x) < 20);
        assertTrue(Math.abs(parkPos.z) < 20);
    }

    @Test
    public void testBuildingsHaveCorrectDimensions() {
        generator.generateWorld(world);

        // Office building should be taller than shops
        Landmark office = world.getLandmark(LandmarkType.OFFICE_BUILDING);
        Landmark greggs = world.getLandmark(LandmarkType.GREGGS);

        assertNotNull(office);
        assertNotNull(greggs);

        assertTrue(office.getHeight() > greggs.getHeight(),
            "Office building should be taller than shops");

        // Office should be at least 10 blocks tall
        assertTrue(office.getHeight() >= 10,
            "Office building should be at least 10 blocks tall");

        // Shops should be shorter (landmark height = buildingHeight + 2, shops are height 4 so 6)
        assertTrue(greggs.getHeight() <= 8,
            "Shops should be 8 blocks or less (landmark height includes roof row)");
    }

    @Test
    public void testBlockTypesAreCorrect() {
        generator.generateWorld(world);

        // Park should have grass
        Landmark park = world.getLandmark(LandmarkType.PARK);
        Vector3 parkPos = park.getPosition();
        BlockType groundBlock = world.getBlock((int) parkPos.x, 0, (int) parkPos.z);
        assertEquals(BlockType.GRASS, groundBlock, "Park ground should be grass");

        // Office building should have glass or stone walls
        Landmark office = world.getLandmark(LandmarkType.OFFICE_BUILDING);
        Vector3 officePos = office.getPosition();
        BlockType wallBlock = world.getBlock((int) officePos.x, 1, (int) officePos.z);
        assertTrue(wallBlock == BlockType.GLASS || wallBlock == BlockType.STONE,
            "Office walls should be glass or stone");
    }

    @Test
    public void testLandmarksDoNotOverlap() {
        generator.generateWorld(world);

        Landmark[] landmarks = {
            world.getLandmark(LandmarkType.PARK),
            world.getLandmark(LandmarkType.GREGGS),
            world.getLandmark(LandmarkType.OFF_LICENCE),
            world.getLandmark(LandmarkType.CHARITY_SHOP),
            world.getLandmark(LandmarkType.JEWELLER),
            world.getLandmark(LandmarkType.OFFICE_BUILDING),
            world.getLandmark(LandmarkType.JOB_CENTRE)
        };

        // Check each pair for overlap
        for (int i = 0; i < landmarks.length; i++) {
            for (int j = i + 1; j < landmarks.length; j++) {
                assertFalse(landmarksOverlap(landmarks[i], landmarks[j]),
                    landmarks[i].getType() + " overlaps with " + landmarks[j].getType());
            }
        }
    }

    @Test
    public void testParkHasFence() {
        generator.generateWorld(world);

        int parkStart = -15; // -PARK_SIZE/2
        int parkEnd = 15;    // PARK_SIZE/2

        // Check that fence blocks exist at the park boundary (2 blocks high)
        int fenceCount = 0;
        for (int x = parkStart; x < parkEnd; x++) {
            for (int y = 1; y <= 2; y++) {
                if (world.getBlock(x, y, parkStart) == BlockType.IRON_FENCE) fenceCount++;
                if (world.getBlock(x, y, parkEnd - 1) == BlockType.IRON_FENCE) fenceCount++;
            }
        }
        for (int z = parkStart; z < parkEnd; z++) {
            for (int y = 1; y <= 2; y++) {
                if (world.getBlock(parkStart, y, z) == BlockType.IRON_FENCE) fenceCount++;
                if (world.getBlock(parkEnd - 1, y, z) == BlockType.IRON_FENCE) fenceCount++;
            }
        }

        // Should have many fence blocks (minus the entry gaps)
        assertTrue(fenceCount > 200,
            "Park should have a substantial iron fence around its perimeter, found " + fenceCount);

        // Check entry gaps exist (at centre of each side)
        assertEquals(BlockType.AIR, world.getBlock(0, 1, parkStart),
            "Should have entry gap at south side of park");
        assertEquals(BlockType.AIR, world.getBlock(parkStart, 1, 0),
            "Should have entry gap at west side of park");
    }

    @Test
    public void testDenseTownHasNewShopTypes() {
        generator.generateWorld(world);

        // Verify the new shop types from bug 008 exist
        assertNotNull(world.getLandmark(LandmarkType.BOOKIES), "Town should have a bookies");
        assertNotNull(world.getLandmark(LandmarkType.KEBAB_SHOP), "Town should have a kebab shop");
        assertNotNull(world.getLandmark(LandmarkType.LAUNDERETTE), "Town should have a launderette");
        assertNotNull(world.getLandmark(LandmarkType.TESCO_EXPRESS), "Town should have a Tesco Express");
        assertNotNull(world.getLandmark(LandmarkType.PUB), "Town should have a pub");
        assertNotNull(world.getLandmark(LandmarkType.PAWN_SHOP), "Town should have a pawn shop");
        assertNotNull(world.getLandmark(LandmarkType.BUILDERS_MERCHANT), "Town should have a builders merchant");
        assertNotNull(world.getLandmark(LandmarkType.WAREHOUSE), "Town should have a warehouse");

        // Verify new building types
        assertNotNull(world.getLandmark(LandmarkType.NANDOS), "Town should have a Nando's");
        assertNotNull(world.getLandmark(LandmarkType.BARBER), "Town should have a barber");
        assertNotNull(world.getLandmark(LandmarkType.NAIL_SALON), "Town should have a nail salon");
        assertNotNull(world.getLandmark(LandmarkType.WETHERSPOONS), "Town should have a Wetherspoons");
        assertNotNull(world.getLandmark(LandmarkType.CORNER_SHOP), "Town should have a corner shop");
        assertNotNull(world.getLandmark(LandmarkType.BETTING_SHOP), "Town should have a betting shop");
        assertNotNull(world.getLandmark(LandmarkType.PHONE_REPAIR), "Town should have a phone repair shop");
        assertNotNull(world.getLandmark(LandmarkType.CASH_CONVERTER), "Town should have a Cash Converter");
        assertNotNull(world.getLandmark(LandmarkType.LIBRARY), "Town should have a library");
        assertNotNull(world.getLandmark(LandmarkType.FIRE_STATION), "Town should have a fire station");
    }

    @Test
    public void testBuildingsHaveSignBlocks() {
        generator.generateWorld(world);

        // Check that the Greggs has a sign (yellow sign blocks at top of building front)
        Landmark greggs = world.getLandmark(LandmarkType.GREGGS);
        assertNotNull(greggs);
        Vector3 pos = greggs.getPosition();
        // Sign is placed at y = buildingHeight; landmark height is buildingHeight + 2
        // so scan the front face up to landmark height to find the sign
        boolean foundSign = false;
        for (int y = 1; y < greggs.getHeight(); y++) {
            BlockType block = world.getBlock((int) pos.x, y, (int) pos.z);
            if (block == BlockType.SIGN_YELLOW || block == BlockType.SIGN_RED ||
                    block == BlockType.SIGN_BLUE || block == BlockType.SIGN_WHITE ||
                    block == BlockType.SIGN_GREEN) {
                foundSign = true;
                break;
            }
        }
        assertTrue(foundSign, "Greggs should have a coloured sign block on its front face");
    }

    @Test
    public void testMultipleTerracedHouseRows() {
        generator.generateWorld(world);

        // Count terraced house buildings by scanning for brick walls
        // Check that there are houses in at least 3 different Z-row areas
        int rowsWithHouses = 0;
        int[] zPositions = {-25, -41, -55, -30, 30, 46}; // Expected terraced row Z positions
        for (int rowZ : zPositions) {
            int houseCount = 0;
            for (int x = -70; x < 20; x++) {
                BlockType block = world.getBlock(x, 1, rowZ);
                if (block == BlockType.BRICK || block == BlockType.GLASS) {
                    houseCount++;
                }
            }
            if (houseCount > 20) { // At least some wall blocks
                rowsWithHouses++;
            }
        }
        assertTrue(rowsWithHouses >= 3,
            "Should have at least 3 rows of terraced houses, found " + rowsWithHouses);
    }

    private boolean landmarksOverlap(Landmark a, Landmark b) {
        Vector3 posA = a.getPosition();
        Vector3 posB = b.getPosition();

        // AABB overlap check
        boolean xOverlap = posA.x < posB.x + b.getWidth() && posA.x + a.getWidth() > posB.x;
        boolean zOverlap = posA.z < posB.z + b.getDepth() && posA.z + a.getDepth() > posB.z;

        return xOverlap && zOverlap;
    }
}

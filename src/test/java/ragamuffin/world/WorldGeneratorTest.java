package ragamuffin.world;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.test.HeadlessTestHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        // Office building should have glass, stone or concrete walls
        Landmark office = world.getLandmark(LandmarkType.OFFICE_BUILDING);
        Vector3 officePos = office.getPosition();
        BlockType wallBlock = world.getBlock((int) officePos.x, 1, (int) officePos.z);
        assertTrue(wallBlock == BlockType.GLASS || wallBlock == BlockType.STONE
                || wallBlock == BlockType.CONCRETE,
            "Office walls should be glass, stone or concrete");
    }

    @Test
    public void testLandmarksDoNotOverlap() {
        generator.generateWorld(world);

        // Include all named landmarks to ensure no building is placed inside another
        LandmarkType[] types = {
            LandmarkType.PARK,
            LandmarkType.GREGGS,
            LandmarkType.OFF_LICENCE,
            LandmarkType.CHARITY_SHOP,
            LandmarkType.JEWELLER,
            LandmarkType.OFFICE_BUILDING,
            LandmarkType.JOB_CENTRE,
            LandmarkType.WAREHOUSE,
            LandmarkType.GP_SURGERY,
            LandmarkType.PRIMARY_SCHOOL,
            LandmarkType.COMMUNITY_CENTRE,
            LandmarkType.CHURCH,
            LandmarkType.TAXI_RANK,
            LandmarkType.CAR_WASH,
            LandmarkType.PETROL_STATION,
            LandmarkType.LIBRARY,
            LandmarkType.FIRE_STATION,
            LandmarkType.WETHERSPOONS,
        };

        List<Landmark> landmarks = new ArrayList<>();
        for (LandmarkType type : types) {
            Landmark lm = world.getLandmark(type);
            if (lm != null) {
                landmarks.add(lm);
            }
        }

        // Check each pair for overlap
        for (int i = 0; i < landmarks.size(); i++) {
            for (int j = i + 1; j < landmarks.size(); j++) {
                assertFalse(landmarksOverlap(landmarks.get(i), landmarks.get(j)),
                    landmarks.get(i).getType() + " overlaps with " + landmarks.get(j).getType());
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
    public void testNewBuildingTypesExist() {
        generator.generateWorld(world);

        // Verify the 6 new building types added in issue #638
        assertNotNull(world.getLandmark(LandmarkType.LEISURE_CENTRE), "Town should have a leisure centre");
        assertNotNull(world.getLandmark(LandmarkType.MOSQUE), "Town should have a mosque");
        assertNotNull(world.getLandmark(LandmarkType.ESTATE_AGENT), "Town should have an estate agent");
        assertNotNull(world.getLandmark(LandmarkType.SUPERMARKET), "Town should have a supermarket");
        assertNotNull(world.getLandmark(LandmarkType.POLICE_STATION), "Town should have a police station");
        assertNotNull(world.getLandmark(LandmarkType.FOOD_BANK), "Town should have a food bank");
    }

    @Test
    public void testNewBuildingTypesHaveDisplayNames() {
        assertEquals("Northfield Leisure Centre", LandmarkType.LEISURE_CENTRE.getDisplayName());
        assertEquals("Al-Noor Mosque", LandmarkType.MOSQUE.getDisplayName());
        assertEquals("Baxter's Estate Agents", LandmarkType.ESTATE_AGENT.getDisplayName());
        assertEquals("Aldi", LandmarkType.SUPERMARKET.getDisplayName());
        assertEquals("Northfield Police Station", LandmarkType.POLICE_STATION.getDisplayName());
        assertEquals("Northfield Food Bank", LandmarkType.FOOD_BANK.getDisplayName());
    }

    @Test
    public void testNewBuildingsHaveSignBlocks() {
        generator.generateWorld(world);

        // Leisure centre has a blue sign
        Landmark lc = world.getLandmark(LandmarkType.LEISURE_CENTRE);
        assertNotNull(lc);
        Vector3 lcPos = lc.getPosition();
        boolean lcHasSign = false;
        for (int y = 1; y < lc.getHeight(); y++) {
            BlockType block = world.getBlock((int) lcPos.x, y, (int) lcPos.z);
            if (block == BlockType.SIGN_BLUE || block == BlockType.SIGN_RED ||
                    block == BlockType.SIGN_GREEN || block == BlockType.SIGN_WHITE ||
                    block == BlockType.SIGN_YELLOW) {
                lcHasSign = true;
                break;
            }
        }
        assertTrue(lcHasSign, "Leisure centre should have a sign block on its front face");

        // Police station has a blue sign
        Landmark ps = world.getLandmark(LandmarkType.POLICE_STATION);
        assertNotNull(ps);
        Vector3 psPos = ps.getPosition();
        boolean psHasSign = false;
        for (int y = 1; y < ps.getHeight(); y++) {
            BlockType block = world.getBlock((int) psPos.x, y, (int) psPos.z);
            if (block == BlockType.SIGN_BLUE || block == BlockType.SIGN_RED ||
                    block == BlockType.SIGN_GREEN || block == BlockType.SIGN_WHITE ||
                    block == BlockType.SIGN_YELLOW) {
                psHasSign = true;
                break;
            }
        }
        assertTrue(psHasSign, "Police station should have a sign block on its front face");
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
    public void testTerracedHousesRegisteredAsLandmarks() {
        generator.generateWorld(world);

        // Verify that terraced houses are registered as landmarks (issue #661)
        Landmark house = world.getLandmark(LandmarkType.TERRACED_HOUSE);
        assertNotNull(house, "Town should have at least one TERRACED_HOUSE landmark registered");

        // The house landmark should have reasonable dimensions
        assertTrue(house.getWidth() >= 4, "House width should be at least 4 blocks");
        assertTrue(house.getHeight() >= 4, "House height should be at least 4 blocks");
        assertTrue(house.getDepth() >= 6, "House depth should be at least 6 blocks");
    }

    @Test
    public void testMultipleTerracedHouseRows() {
        generator.generateWorld(world);

        // Count terraced house rows by scanning a wide area south and north of the park.
        // Building positions are seed-derived, so we search a broad range of Z values
        // rather than checking hardcoded coordinates.
        int rowsWithHouses = 0;
        // Scan Z from -120 to +80 in 4-block steps, covering the full residential belt.
        // Count any residential wall material (houses now use varied facades).
        for (int rowZ = -120; rowZ <= 80; rowZ += 4) {
            int houseCount = 0;
            for (int x = -160; x < 140; x++) {
                BlockType block = world.getBlock(x, 1, rowZ);
                if (block == BlockType.BRICK || block == BlockType.GLASS
                        || block == BlockType.YELLOW_BRICK || block == BlockType.PEBBLEDASH
                        || block == BlockType.RENDER_WHITE || block == BlockType.RENDER_CREAM) {
                    houseCount++;
                }
            }
            if (houseCount > 20) {
                rowsWithHouses++;
            }
        }
        assertTrue(rowsWithHouses >= 3,
            "Should have at least 3 rows of terraced houses, found " + rowsWithHouses);
    }

    /**
     * Issue #210: Terrain near buildings should be flatter than terrain far away.
     * Uses the Greggs landmark position (which is seed-derived) rather than hardcoded
     * coordinates, so the test remains valid regardless of the world layout.
     */
    @Test
    public void testTerrainIsFlatterNearBuildings() {
        generator.generateWorld(world);

        // Find the Greggs landmark — its position is procedurally placed per seed.
        Landmark greggs = world.getLandmark(LandmarkType.GREGGS);
        assertNotNull(greggs, "Greggs must exist before terrain test");
        int gx = (int) greggs.getPosition().x;
        int gz = (int) greggs.getPosition().z;
        int gw = greggs.getWidth();
        int gd = greggs.getDepth();

        // The flat zone margin extends 2 blocks beyond the footprint on every side.
        // A point 1 block inside the hard-flat boundary (i.e. the building footprint itself)
        // must be at base terrain height (y=0).
        assertEquals(0, generator.getTerrainHeight(gx, gz),
            "Terrain at Greggs position must be at base height (inside flat zone)");
        assertEquals(0, generator.getTerrainHeight(gx - 1, gz),
            "Terrain 1 block west of Greggs (inside hard-flat margin) must be at base height");

        // Blocks well beyond the building (past the blend zone) may be elevated.
        // Verify the gradient: a point just past the flat margin should be <= a point
        // further out (natural terrain rises with distance from town centre).
        // Check along the north side: flatEdge = gz + gd + 2, blend ends 8 blocks out.
        int flatEdgeZ = gz + gd + 2;
        int heightNearEdge = generator.getTerrainHeight(gx, flatEdgeZ + 3);
        int heightFarEdge  = generator.getTerrainHeight(gx, flatEdgeZ + 11);

        assertTrue(heightNearEdge <= heightFarEdge,
            "Terrain in blend zone should be <= natural terrain further out (near=" + heightNearEdge + ", far=" + heightFarEdge + ")");
    }

    // ── Issue #693: Staged world generation ──────────────────────────────────

    @Test
    public void testGenerationStageIsCompleteAfterGenerateWorld() {
        generator.generateWorld(world);

        assertEquals(WorldGenerator.GenerationStage.COMPLETE, generator.getCurrentStage(),
            "Generation stage should be COMPLETE after generateWorld() returns");
    }

    @Test
    public void testStageIsNotStartedBeforeGenerateWorld() {
        WorldGenerator freshGen = new WorldGenerator(12345);
        assertEquals(WorldGenerator.GenerationStage.NOT_STARTED, freshGen.getCurrentStage(),
            "Generation stage should be NOT_STARTED before generateWorld() is called");
    }

    @Test
    public void testBuildingPlotsPopulatedAfterGeneration() {
        generator.generateWorld(world);

        List<WorldGenerator.BuildingPlot> plots = generator.getBuildingPlots();
        assertFalse(plots.isEmpty(),
            "Building plots list should be non-empty after generation");

        boolean hasAboveGround = plots.stream().anyMatch(p -> !p.isUnderground());
        boolean hasUnderground  = plots.stream().anyMatch(WorldGenerator.BuildingPlot::isUnderground);

        assertTrue(hasAboveGround, "Should have at least one above-ground building plot");
        assertTrue(hasUnderground, "Should have at least one underground building plot");
    }

    @Test
    public void testBuildingPlotsHaveValidDimensions() {
        generator.generateWorld(world);

        for (WorldGenerator.BuildingPlot plot : generator.getBuildingPlots()) {
            assertTrue(plot.getWidth() > 0,
                "Plot width must be positive: " + plot);
            assertTrue(plot.getDepth() > 0,
                "Plot depth must be positive: " + plot);
        }
    }

    @Test
    public void testNpcSpawnPointsPopulatedAfterGeneration() {
        generator.generateWorld(world);

        List<WorldGenerator.NpcSpawnPoint> spawnPoints = generator.getNpcSpawnPoints();
        assertFalse(spawnPoints.isEmpty(),
            "NPC spawn points should be non-empty after generation (shops need shopkeepers)");
    }

    @Test
    public void testNpcSpawnPointsHaveValidLandmarkTypes() {
        generator.generateWorld(world);

        for (WorldGenerator.NpcSpawnPoint sp : generator.getNpcSpawnPoints()) {
            assertNotNull(sp.getLandmarkType(),
                "NPC spawn point landmark type must not be null");
        }
    }

    @Test
    public void testNpcSpawnPointsIncludeGreggs() {
        generator.generateWorld(world);

        boolean hasGreggs = generator.getNpcSpawnPoints().stream()
            .anyMatch(sp -> sp.getLandmarkType() == LandmarkType.GREGGS);
        assertTrue(hasGreggs, "NPC spawn points should include a shopkeeper for Greggs");
    }

    @Test
    public void testBuildingPlotsListIsUnmodifiable() {
        generator.generateWorld(world);

        List<WorldGenerator.BuildingPlot> plots = generator.getBuildingPlots();
        assertThrows(UnsupportedOperationException.class,
            () -> plots.add(new WorldGenerator.BuildingPlot(0, 0, 1, 1, false)),
            "getBuildingPlots() should return an unmodifiable view");
    }

    @Test
    public void testNpcSpawnPointsListIsUnmodifiable() {
        generator.generateWorld(world);

        List<WorldGenerator.NpcSpawnPoint> points = generator.getNpcSpawnPoints();
        assertThrows(UnsupportedOperationException.class,
            () -> points.add(new WorldGenerator.NpcSpawnPoint(LandmarkType.GREGGS, 0, 0, 0)),
            "getNpcSpawnPoints() should return an unmodifiable view");
    }

    // ── Issue #732: Buildings populated with content and NPCs ────────────────

    @Test
    public void testOfficeBuildingHasInteriorFurniture() {
        generator.generateWorld(world);

        Landmark office = world.getLandmark(LandmarkType.OFFICE_BUILDING);
        assertNotNull(office, "Office building must exist");

        int ox = (int) office.getPosition().x;
        int oz = (int) office.getPosition().z;
        int width = office.getWidth();
        int depth = office.getDepth();

        // Interior should have TABLE or COUNTER blocks
        boolean hasFurniture = false;
        for (int dx = 1; dx < width - 1 && !hasFurniture; dx++) {
            for (int dz = 1; dz < depth - 1 && !hasFurniture; dz++) {
                BlockType block = world.getBlock(ox + dx, 1, oz + dz);
                if (block == BlockType.TABLE || block == BlockType.COUNTER
                        || block == BlockType.BOOKSHELF) {
                    hasFurniture = true;
                }
            }
        }
        assertTrue(hasFurniture, "Office building interior should contain desks, counters or shelves");
    }

    @Test
    public void testChurchHasInteriorFurniture() {
        generator.generateWorld(world);

        Landmark church = world.getLandmark(LandmarkType.CHURCH);
        assertNotNull(church, "Church must exist");

        int cx = (int) church.getPosition().x;
        int cz = (int) church.getPosition().z;
        int width = church.getWidth();
        int depth = church.getDepth();

        // Church interior should have wooden pews (WOOD blocks) and an altar (TABLE)
        boolean hasPew = false;
        boolean hasAltar = false;
        for (int dx = 1; dx < width - 1; dx++) {
            for (int dz = 1; dz < depth - 1; dz++) {
                BlockType block = world.getBlock(cx + dx, 1, cz + dz);
                if (block == BlockType.WOOD) hasPew = true;
                if (block == BlockType.TABLE) hasAltar = true;
            }
        }
        assertTrue(hasPew, "Church interior should have wooden pews");
        assertTrue(hasAltar, "Church interior should have an altar table");
    }

    @Test
    public void testWarehouseHasInteriorFurniture() {
        generator.generateWorld(world);

        Landmark warehouse = world.getLandmark(LandmarkType.WAREHOUSE);
        assertNotNull(warehouse, "Warehouse must exist");

        int wx = (int) warehouse.getPosition().x;
        int wz = (int) warehouse.getPosition().z;
        int width = warehouse.getWidth();
        int depth = warehouse.getDepth();

        // Warehouse interior should have SHELF blocks (shelving racks)
        boolean hasShelf = false;
        for (int dx = 1; dx < width - 1 && !hasShelf; dx++) {
            for (int dz = 1; dz < depth - 1 && !hasShelf; dz++) {
                BlockType block = world.getBlock(wx + dx, 1, wz + dz);
                if (block == BlockType.SHELF) {
                    hasShelf = true;
                }
            }
        }
        assertTrue(hasShelf, "Warehouse interior should contain shelving racks");
    }

    @Test
    public void testPoliceStationHasInteriorFurniture() {
        generator.generateWorld(world);

        Landmark ps = world.getLandmark(LandmarkType.POLICE_STATION);
        assertNotNull(ps, "Police station must exist");

        int px = (int) ps.getPosition().x;
        int pz = (int) ps.getPosition().z;
        int width = ps.getWidth();
        int depth = ps.getDepth();

        // Police station interior should have a custody counter and desks
        boolean hasCounter = false;
        boolean hasDesk = false;
        for (int dx = 1; dx < width - 1; dx++) {
            for (int dz = 1; dz < depth - 1; dz++) {
                BlockType block = world.getBlock(px + dx, 1, pz + dz);
                if (block == BlockType.COUNTER) hasCounter = true;
                if (block == BlockType.TABLE) hasDesk = true;
            }
        }
        assertTrue(hasCounter, "Police station interior should have a custody counter");
        assertTrue(hasDesk, "Police station interior should have officer desks");
    }

    @Test
    public void testFireStationHasInteriorFurniture() {
        generator.generateWorld(world);

        Landmark fs = world.getLandmark(LandmarkType.FIRE_STATION);
        assertNotNull(fs, "Fire station must exist");

        int fx = (int) fs.getPosition().x;
        int fz = (int) fs.getPosition().z;
        int width = fs.getWidth();
        int depth = fs.getDepth();

        // Fire station interior should have tables (crew room) or shelves (equipment)
        boolean hasFurniture = false;
        for (int dx = 1; dx < width - 1 && !hasFurniture; dx++) {
            for (int dz = 1; dz < depth - 1 && !hasFurniture; dz++) {
                BlockType block = world.getBlock(fx + dx, 1, fz + dz);
                if (block == BlockType.TABLE || block == BlockType.COUNTER
                        || block == BlockType.SHELF) {
                    hasFurniture = true;
                }
            }
        }
        assertTrue(hasFurniture, "Fire station interior should contain crew room furniture");
    }

    @Test
    public void testSupermarketHasInteriorFurniture() {
        generator.generateWorld(world);

        Landmark sm = world.getLandmark(LandmarkType.SUPERMARKET);
        assertNotNull(sm, "Supermarket must exist");

        int sx = (int) sm.getPosition().x;
        int sz = (int) sm.getPosition().z;
        int width = sm.getWidth();
        int depth = sm.getDepth();

        // Supermarket should have SHELF aisles and COUNTER checkouts
        int shelfCount = 0;
        boolean hasCheckout = false;
        for (int dx = 1; dx < width - 1; dx++) {
            for (int dz = 1; dz < depth - 1; dz++) {
                BlockType block = world.getBlock(sx + dx, 1, sz + dz);
                if (block == BlockType.SHELF) shelfCount++;
                if (block == BlockType.COUNTER) hasCheckout = true;
            }
        }
        assertTrue(shelfCount > 5,
            "Supermarket interior should have multiple shelf blocks for aisles, found " + shelfCount);
        assertTrue(hasCheckout, "Supermarket interior should have checkout counters");
    }

    @Test
    public void testNpcSpawnPointsIncludeOfficeBuilding() {
        generator.generateWorld(world);

        boolean hasOffice = generator.getNpcSpawnPoints().stream()
            .anyMatch(sp -> sp.getLandmarkType() == LandmarkType.OFFICE_BUILDING);
        assertTrue(hasOffice,
            "NPC spawn points should include office workers for the office building (#732)");
    }

    @Test
    public void testNpcSpawnPointsIncludeChurch() {
        generator.generateWorld(world);

        boolean hasChurch = generator.getNpcSpawnPoints().stream()
            .anyMatch(sp -> sp.getLandmarkType() == LandmarkType.CHURCH);
        assertTrue(hasChurch,
            "NPC spawn points should include NPCs for the church (#732)");
    }

    @Test
    public void testNpcSpawnPointsIncludePoliceStation() {
        generator.generateWorld(world);

        boolean hasPoliceStation = generator.getNpcSpawnPoints().stream()
            .anyMatch(sp -> sp.getLandmarkType() == LandmarkType.POLICE_STATION);
        assertTrue(hasPoliceStation,
            "NPC spawn points should include police officers for the police station (#732)");
    }

    @Test
    public void testNpcSpawnPointsIncludeFireStation() {
        generator.generateWorld(world);

        boolean hasFireStation = generator.getNpcSpawnPoints().stream()
            .anyMatch(sp -> sp.getLandmarkType() == LandmarkType.FIRE_STATION);
        assertTrue(hasFireStation,
            "NPC spawn points should include crew for the fire station (#732)");
    }

    @Test
    public void testNpcSpawnPointsIncludePrimarySchool() {
        generator.generateWorld(world);

        boolean hasPrimarySchool = generator.getNpcSpawnPoints().stream()
            .anyMatch(sp -> sp.getLandmarkType() == LandmarkType.PRIMARY_SCHOOL);
        assertTrue(hasPrimarySchool,
            "NPC spawn points should include pupils/staff for the primary school (#732)");
    }

    @Test
    public void testNpcSpawnPointsIncludeWarehouse() {
        generator.generateWorld(world);

        boolean hasWarehouse = generator.getNpcSpawnPoints().stream()
            .anyMatch(sp -> sp.getLandmarkType() == LandmarkType.WAREHOUSE);
        assertTrue(hasWarehouse,
            "NPC spawn points should include a guard for the warehouse (#732)");
    }

    @Test
    public void testNpcSpawnPointsMoreThanBeforeIssue732() {
        generator.generateWorld(world);

        // The original implementation had exactly one spawn point per shop (27 shop types).
        // After #732 we have additional NPCs in offices, church, police, fire, school,
        // warehouse, community centre, leisure centre, and extra staff in pubs/supermarkets.
        // So total spawn points should exceed 35.
        int count = generator.getNpcSpawnPoints().size();
        assertTrue(count > 35,
            "After #732, total NPC spawn points should exceed 35 (found " + count + ")");
    }

    @Test
    public void testCommunityCentreHasInteriorFurniture() {
        generator.generateWorld(world);

        Landmark cc = world.getLandmark(LandmarkType.COMMUNITY_CENTRE);
        assertNotNull(cc, "Community centre must exist");

        int cx = (int) cc.getPosition().x;
        int cz = (int) cc.getPosition().z;
        int width = cc.getWidth();
        int depth = cc.getDepth();

        // Community centre should have TABLE blocks for the hall
        boolean hasTable = false;
        for (int dx = 1; dx < width - 1 && !hasTable; dx++) {
            for (int dz = 1; dz < depth - 1 && !hasTable; dz++) {
                if (world.getBlock(cx + dx, 1, cz + dz) == BlockType.TABLE) {
                    hasTable = true;
                }
            }
        }
        assertTrue(hasTable, "Community centre interior should contain tables for the hall");
    }

    @Test
    public void testPubPositionDebug() {
        generator.generateWorld(world);

        Landmark pub = world.getLandmark(LandmarkType.PUB);
        assertNotNull(pub, "PUB must exist");

        int px = (int) pub.getPosition().x;
        int pz = (int) pub.getPosition().z;
        int width = pub.getWidth();

        System.out.println("PUB position: (" + px + ", " + pz + ") width=" + width + " depth=" + pub.getDepth());
        System.out.println("Door expected at: (" + (px + width/2) + ", 1-2, " + pz + ")");
        System.out.println("Block at door y=1: " + world.getBlock(px + width/2, 1, pz));
        System.out.println("Block at door y=2: " + world.getBlock(px + width/2, 2, pz));

        // Check all landmarks and their positions for overlap with PUB
        for (Landmark lm : world.getAllLandmarks()) {
            if (lm == pub) continue;
            if (landmarksOverlap(pub, lm)) {
                System.out.println("PUB OVERLAPS WITH: " + lm.getType() + " at " + lm.getPosition() + " w=" + lm.getWidth() + " d=" + lm.getDepth());
            }
        }
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

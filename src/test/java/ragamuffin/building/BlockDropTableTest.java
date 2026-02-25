package ragamuffin.building;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.world.BlockType;
import ragamuffin.world.LandmarkType;

import static org.junit.jupiter.api.Assertions.*;

class BlockDropTableTest {

    private BlockDropTable dropTable;

    @BeforeEach
    void setUp() {
        dropTable = new BlockDropTable();
    }

    @Test
    void testTreeTrunkDropsWood() {
        Material drop = dropTable.getDrop(BlockType.TREE_TRUNK, null);
        assertEquals(Material.WOOD, drop);
    }

    @Test
    void testBrickDropsBrick() {
        Material drop = dropTable.getDrop(BlockType.BRICK, null);
        assertEquals(Material.BRICK, drop);
    }

    @Test
    void testStoneDropsStone() {
        Material drop = dropTable.getDrop(BlockType.STONE, null);
        assertEquals(Material.STONE, drop);
    }

    @Test
    void testGrassDropsGrassTurf() {
        Material drop = dropTable.getDrop(BlockType.GRASS, null);
        assertEquals(Material.GRASS_TURF, drop);
    }

    @Test
    void testDirtDropsDirt() {
        Material drop = dropTable.getDrop(BlockType.DIRT, null);
        assertEquals(Material.DIRT, drop);
    }

    @Test
    void testGlassDropsGlass() {
        Material drop = dropTable.getDrop(BlockType.GLASS, null);
        assertEquals(Material.GLASS, drop);
    }

    @Test
    void testPavementDropsPavementSlabOrCurrency() {
        // PAVEMENT has a 5% chance to drop a coin (PENNY or SHILLING); otherwise PAVEMENT_SLAB.
        // Run enough trials to confirm the result is always one of the three valid materials.
        boolean sawSlab = false;
        boolean sawCurrency = false;
        for (int i = 0; i < 300; i++) {
            Material drop = dropTable.getDrop(BlockType.PAVEMENT, null);
            assertTrue(drop == Material.PAVEMENT_SLAB || drop == Material.PENNY || drop == Material.SHILLING,
                    "PAVEMENT should drop PAVEMENT_SLAB, PENNY, or SHILLING, but got: " + drop);
            if (drop == Material.PAVEMENT_SLAB) sawSlab = true;
            if (drop == Material.PENNY || drop == Material.SHILLING) sawCurrency = true;
        }
        assertTrue(sawSlab, "PAVEMENT should usually drop PAVEMENT_SLAB");
        assertTrue(sawCurrency, "PAVEMENT should occasionally drop a coin (PENNY or SHILLING)");
    }

    @Test
    void testRoadDropsRoadAsphaltOrCurrency() {
        // ROAD has a 5% chance to drop a coin (PENNY or SHILLING); otherwise ROAD_ASPHALT.
        // Run enough trials to confirm the result is always one of the three valid materials.
        boolean sawAsphalt = false;
        boolean sawCurrency = false;
        for (int i = 0; i < 300; i++) {
            Material drop = dropTable.getDrop(BlockType.ROAD, null);
            assertTrue(drop == Material.ROAD_ASPHALT || drop == Material.PENNY || drop == Material.SHILLING,
                    "ROAD should drop ROAD_ASPHALT, PENNY, or SHILLING, but got: " + drop);
            if (drop == Material.ROAD_ASPHALT) sawAsphalt = true;
            if (drop == Material.PENNY || drop == Material.SHILLING) sawCurrency = true;
        }
        assertTrue(sawAsphalt, "ROAD should usually drop ROAD_ASPHALT");
        assertTrue(sawCurrency, "ROAD should occasionally drop a coin (PENNY or SHILLING)");
    }

    @Test
    void testAirDropsNothing() {
        Material drop = dropTable.getDrop(BlockType.AIR, null);
        assertNull(drop);
    }

    @Test
    void testLeavesDropWoodOrNothing() {
        // LEAVES have a 30% chance to drop WOOD (twigs/branches); run enough trials
        // to ensure the result is always either WOOD or null, never anything else.
        boolean sawWood = false;
        boolean sawNull = false;
        for (int i = 0; i < 200; i++) {
            Material drop = dropTable.getDrop(BlockType.LEAVES, null);
            assertTrue(drop == null || drop == Material.WOOD,
                    "LEAVES should drop WOOD or null, but got: " + drop);
            if (drop == Material.WOOD) sawWood = true;
            if (drop == null) sawNull = true;
        }
        // With 200 trials and 30% probability, the chance of never seeing WOOD is ~1e-31
        assertTrue(sawWood, "LEAVES should occasionally drop WOOD");
        assertTrue(sawNull, "LEAVES should sometimes drop nothing");
    }

    @Test
    void testWaterDropsNothing() {
        Material drop = dropTable.getDrop(BlockType.WATER, null);
        assertNull(drop);
    }

    @Test
    void testJewellerBlockDropsDiamond() {
        // A GLASS block from the jeweller should drop DIAMOND
        Material drop = dropTable.getDrop(BlockType.GLASS, LandmarkType.JEWELLER);
        assertEquals(Material.DIAMOND, drop);
    }

    @Test
    void testOfficeBlockDropsComputer() {
        // BRICK block from office building
        Material drop = dropTable.getDrop(BlockType.BRICK, LandmarkType.OFFICE_BUILDING);
        assertEquals(Material.COMPUTER, drop);
    }

    @Test
    void testOfficeGlassDropsOfficeChair() {
        Material drop = dropTable.getDrop(BlockType.GLASS, LandmarkType.OFFICE_BUILDING);
        assertEquals(Material.OFFICE_CHAIR, drop);
    }

    @Test
    void testRegularGlassNotFromJeweller() {
        // Regular GLASS block not from a landmark
        Material drop = dropTable.getDrop(BlockType.GLASS, null);
        assertEquals(Material.GLASS, drop);
    }

    @Test
    void testRegularBrickNotFromOffice() {
        // Regular BRICK not from office
        Material drop = dropTable.getDrop(BlockType.BRICK, null);
        assertEquals(Material.BRICK, drop);
    }

    // --- Regression tests for Issue #211: wood planks infinite loop ---

    @Test
    void testWoodPlanksBlockDropsPlanks() {
        // WOOD_PLANKS (player-placed plank block) must drop PLANKS, not WOOD.
        // If it dropped WOOD, players could craft WOOD->PLANKS, place, break, get WOOD again
        // — an infinite duplication loop.
        Material drop = dropTable.getDrop(BlockType.WOOD_PLANKS, null);
        assertEquals(Material.PLANKS, drop, "WOOD_PLANKS block must drop PLANKS to prevent infinite loop");
    }

    @Test
    void testWoodBlockDropsWood() {
        // World-generated WOOD blocks (e.g. from logs) still drop WOOD as before
        Material drop = dropTable.getDrop(BlockType.WOOD, null);
        assertEquals(Material.WOOD, drop, "World-generated WOOD block should still drop WOOD");
    }
}

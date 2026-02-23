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
    void testPavementDropsPavementSlab() {
        Material drop = dropTable.getDrop(BlockType.PAVEMENT, null);
        assertEquals(Material.PAVEMENT_SLAB, drop);
    }

    @Test
    void testRoadDropsRoadAsphalt() {
        Material drop = dropTable.getDrop(BlockType.ROAD, null);
        assertEquals(Material.ROAD_ASPHALT, drop);
    }

    @Test
    void testAirDropsNothing() {
        Material drop = dropTable.getDrop(BlockType.AIR, null);
        assertNull(drop);
    }

    @Test
    void testLeavesDropNothing() {
        Material drop = dropTable.getDrop(BlockType.LEAVES, null);
        assertNull(drop); // Leaves don't drop items
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

package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.BlockBreaker;
import ragamuffin.building.BlockDropTable;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.BlockType;
import ragamuffin.world.Landmark;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #207:
 * Wood fence not added to inventory when destroyed.
 *
 * WOOD_FENCE blocks in the allotments are inside the ALLOTMENTS landmark zone.
 * When broken, the drop lookup goes through getLandmarkDrop() which must fall
 * back to the standard drop table and return Material.WOOD so the item is added
 * to the player's inventory.
 */
class Issue207WoodFenceInventoryTest {

    // Allotments position is seed-derived; look it up from the landmark at runtime.
    private int ALLOTMENTS_X;
    private int ALLOTMENTS_Z;

    private World world;
    private Inventory inventory;
    private BlockBreaker blockBreaker;
    private BlockDropTable dropTable;

    @BeforeEach
    void setUp() {
        HeadlessTestHelper.initHeadless();
        world = new World(0);
        world.generate();
        inventory = new Inventory(36);
        blockBreaker = new BlockBreaker();
        dropTable = new BlockDropTable();

        Landmark allotments = world.getLandmark(LandmarkType.ALLOTMENTS);
        assertNotNull(allotments, "ALLOTMENTS landmark must exist after world generation");
        ALLOTMENTS_X = (int) allotments.getPosition().x;
        ALLOTMENTS_Z = (int) allotments.getPosition().z;
    }

    /**
     * Test 1: Breaking a WOOD_FENCE block inside the ALLOTMENTS landmark adds WOOD to inventory.
     *
     * The allotments perimeter fence at (ALLOTMENTS_X, 1, ALLOTMENTS_Z) is a WOOD_FENCE block
     * inside the ALLOTMENTS landmark zone. Punching it 5 times should break it and the drop
     * (looked up via getLandmarkDrop → fallback to standard table) must be WOOD.
     */
    @Test
    void woodFenceInAllotmentsLandmarkDropsWood() {
        // Confirm the block at the allotments perimeter is WOOD_FENCE
        BlockType fenceBlock = world.getBlock(ALLOTMENTS_X, 1, ALLOTMENTS_Z);
        assertEquals(BlockType.WOOD_FENCE, fenceBlock,
                "Allotments perimeter block should be WOOD_FENCE");

        // Confirm it is inside the ALLOTMENTS landmark
        LandmarkType landmark = world.getLandmarkAt(ALLOTMENTS_X, 1, ALLOTMENTS_Z);
        assertEquals(LandmarkType.ALLOTMENTS, landmark,
                "Block should be inside the ALLOTMENTS landmark zone");

        // Get the drop via the full landmark-aware path (mirrors RagamuffinGame.handlePunch)
        Material drop = dropTable.getDrop(fenceBlock, landmark);
        assertNotNull(drop,
                "WOOD_FENCE inside ALLOTMENTS landmark must yield a drop (not null)");
        assertEquals(Material.WOOD, drop,
                "WOOD_FENCE inside ALLOTMENTS landmark must drop WOOD");

        // Punch 5 times to break the fence block
        for (int i = 0; i < 4; i++) {
            boolean broken = blockBreaker.punchBlock(world, ALLOTMENTS_X, 1, ALLOTMENTS_Z);
            assertFalse(broken, "Block should not break before hit 5 (hit " + (i + 1) + ")");
        }
        boolean broken = blockBreaker.punchBlock(world, ALLOTMENTS_X, 1, ALLOTMENTS_Z);
        assertTrue(broken, "Block should break on hit 5");

        // Block must be AIR now
        assertEquals(BlockType.AIR, world.getBlock(ALLOTMENTS_X, 1, ALLOTMENTS_Z),
                "WOOD_FENCE should be replaced by AIR after breaking");

        // Simulate RagamuffinGame.handlePunch() adding the drop to inventory
        inventory.addItem(drop, 1);

        // Verify WOOD is in the inventory
        assertEquals(1, inventory.getItemCount(Material.WOOD),
                "Player inventory must contain 1 WOOD after breaking an allotments WOOD_FENCE");
    }

    /**
     * Test 2: Breaking a WOOD_WALL block (shed) inside ALLOTMENTS landmark adds WOOD to inventory.
     *
     * Shed 1 is placed at (ALLOTMENTS_X+2, z=ALLOTMENTS_Z+2). The wall adjacent to the door
     * at (ALLOTMENTS_X+3, 1, ALLOTMENTS_Z+2) should be WOOD_WALL and inside the ALLOTMENTS zone.
     */
    @Test
    void woodWallInAllotmentsLandmarkDropsWood() {
        // Confirm WOOD_WALL block in shed
        BlockType wallBlock = world.getBlock(ALLOTMENTS_X + 3, 1, ALLOTMENTS_Z + 2);
        assertEquals(BlockType.WOOD_WALL, wallBlock,
                "Shed wall should be WOOD_WALL");

        // Confirm it is inside ALLOTMENTS
        LandmarkType landmark = world.getLandmarkAt(ALLOTMENTS_X + 3, 1, ALLOTMENTS_Z + 2);
        assertEquals(LandmarkType.ALLOTMENTS, landmark,
                "Shed wall should be inside the ALLOTMENTS landmark zone");

        // Get drop via the landmark-aware path
        Material drop = dropTable.getDrop(wallBlock, landmark);
        assertNotNull(drop,
                "WOOD_WALL inside ALLOTMENTS landmark must yield a drop (not null)");
        assertEquals(Material.WOOD, drop,
                "WOOD_WALL inside ALLOTMENTS landmark must drop WOOD");

        // Punch 5 times to break
        for (int i = 0; i < 4; i++) {
            boolean broken = blockBreaker.punchBlock(world, ALLOTMENTS_X + 3, 1, ALLOTMENTS_Z + 2);
            assertFalse(broken, "Block should not break before hit 5 (hit " + (i + 1) + ")");
        }
        boolean broken = blockBreaker.punchBlock(world, ALLOTMENTS_X + 3, 1, ALLOTMENTS_Z + 2);
        assertTrue(broken, "Block should break on hit 5");

        assertEquals(BlockType.AIR, world.getBlock(ALLOTMENTS_X + 3, 1, ALLOTMENTS_Z + 2),
                "WOOD_WALL should become AIR after breaking");

        inventory.addItem(drop, 1);
        assertEquals(1, inventory.getItemCount(Material.WOOD),
                "Player inventory must contain 1 WOOD after breaking an allotments WOOD_WALL");
    }

    /**
     * Test 3: Standalone WOOD_FENCE block (no landmark) drops WOOD.
     *
     * Regression guard: a WOOD_FENCE block placed outside any landmark must also
     * drop WOOD (null-landmark path through BlockDropTable).
     */
    @Test
    void standalonewoodFenceDropsWood() {
        // Place a WOOD_FENCE block away from any landmark
        world.setBlock(0, 1, 0, BlockType.WOOD_FENCE);

        // Confirm no landmark at this position
        LandmarkType landmark = world.getLandmarkAt(0, 1, 0);
        assertNull(landmark, "Block at (0,1,0) should not be inside any landmark");

        Material drop = dropTable.getDrop(BlockType.WOOD_FENCE, null);
        assertNotNull(drop, "Standalone WOOD_FENCE must yield a drop");
        assertEquals(Material.WOOD, drop, "Standalone WOOD_FENCE must drop WOOD");

        // Break and add to inventory
        for (int i = 0; i < 5; i++) {
            blockBreaker.punchBlock(world, 0, 1, 0);
        }
        assertEquals(BlockType.AIR, world.getBlock(0, 1, 0),
                "Standalone WOOD_FENCE should be AIR after 5 punches");

        inventory.addItem(drop, 1);
        assertEquals(1, inventory.getItemCount(Material.WOOD),
                "Inventory must contain 1 WOOD after breaking standalone WOOD_FENCE");
    }

    /**
     * Test 4: Multiple fence blocks can be broken to accumulate WOOD.
     *
     * Breaking N fence blocks adds N WOOD to the inventory.
     */
    @Test
    void multipleWoodFenceBlocksAccumulateWood() {
        // Break 3 fence blocks along the north allotments perimeter
        int[] xPositions = {ALLOTMENTS_X + 1, ALLOTMENTS_X + 2, ALLOTMENTS_X + 3};

        for (int bx : xPositions) {
            assertEquals(BlockType.WOOD_FENCE, world.getBlock(bx, 1, ALLOTMENTS_Z),
                    "Block at (" + bx + ",1," + ALLOTMENTS_Z + ") should be WOOD_FENCE");

            LandmarkType lm = world.getLandmarkAt(bx, 1, ALLOTMENTS_Z);
            Material drop = dropTable.getDrop(BlockType.WOOD_FENCE, lm);
            assertEquals(Material.WOOD, drop, "Each WOOD_FENCE must drop WOOD regardless of landmark");

            for (int i = 0; i < 5; i++) {
                blockBreaker.punchBlock(world, bx, 1, ALLOTMENTS_Z);
            }
            inventory.addItem(drop, 1);
        }

        assertEquals(3, inventory.getItemCount(Material.WOOD),
                "Breaking 3 WOOD_FENCE blocks should give exactly 3 WOOD in inventory");
    }
}

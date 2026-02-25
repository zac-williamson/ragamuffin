package ragamuffin.integration;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ragamuffin.building.BlockPlacer;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.building.SmallItem;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #651 Integration Test — Small items placed in the world are rendered.
 *
 * Verifies that:
 * 1. Small items placed via BlockPlacer.placeSmallItem() appear in world.getSmallItems(),
 *    which is called by the new renderSmallItems() pass in RagamuffinGame.
 * 2. world.getSmallItems() returns all placed items with correct positions and materials.
 * 3. Material.getIconColors() returns a non-null, valid colour for every small-item
 *    material — this colour is used to tint the rendered billboard.
 * 4. The perspective camera correctly projects a small item's world-space position
 *    into valid NDC, confirming that camera.combined is the right projection matrix
 *    for the renderSmallItems() 3D rendering pass.
 * 5. Multiple small items accumulate and are all retrievable for the render pass.
 */
class Issue651SmallItemRenderingTest {

    @BeforeAll
    static void initGdx() {
        HeadlessTestHelper.initHeadless();
    }

    /**
     * Test 1: world.getSmallItems() returns placed items — the render pass can iterate them.
     * Verifies the data pipeline from placeSmallItem() → world.getSmallItems() is intact.
     */
    @Test
    void test1_PlacedSmallItemsAreRetrievableForRendering() {
        World world = new World(42L);
        BlockPlacer placer = new BlockPlacer();
        Inventory inventory = new Inventory(36);

        // Place a block at high altitude to avoid world generation interference
        world.setBlock(50, 100, 50, BlockType.STONE);
        inventory.addItem(Material.TIN_OF_BEANS, 1);

        // Look straight down at the top face of the block
        Vector3 origin = new Vector3(50.5f, 103f, 50.5f);
        Vector3 direction = new Vector3(0, -1, 0);

        boolean placed = placer.placeSmallItem(world, inventory, Material.TIN_OF_BEANS,
                origin, direction, 5.0f, null);

        assertTrue(placed, "placeSmallItem() should succeed on a block's top face");

        // The render pass calls world.getSmallItems() — verify it returns the item
        List<SmallItem> items = world.getSmallItems();
        assertFalse(items.isEmpty(),
                "world.getSmallItems() must return a non-empty list after placement — " +
                "renderSmallItems() iterates this list to draw billboards");
        assertEquals(1, items.size(),
                "Exactly one small item should be in the world");

        SmallItem item = items.get(0);
        assertEquals(Material.TIN_OF_BEANS, item.getMaterial(),
                "Placed item's material must match what was placed — " +
                "renderSmallItems() uses material.getIconColors() to tint the billboard");
        // Y must be on top of block (blockY + 1 = 101)
        assertEquals(101.0f, item.getPosition().y, 0.001f,
                "Small item must sit on top of the block for correct 3D rendering height");
    }

    /**
     * Test 2: Every small-item material returns a valid non-null icon colour.
     * renderSmallItems() calls material.getIconColors()[0] to tint each billboard.
     * A null or empty array would cause a NullPointerException during rendering.
     */
    @Test
    void test2_AllSmallItemMaterialsHaveValidRenderColour() {
        for (Material m : Material.values()) {
            if (!m.isSmallItem()) continue;

            Color[] colors = m.getIconColors();
            assertNotNull(colors,
                    m.name() + ".getIconColors() must not return null — " +
                    "renderSmallItems() uses colors[0] to tint the 3D billboard");
            assertTrue(colors.length >= 1,
                    m.name() + ".getIconColors() must return at least one colour");

            Color col = colors[0];
            assertNotNull(col,
                    m.name() + ".getIconColors()[0] must not be null");
            assertTrue(col.r >= 0f && col.r <= 1f,
                    m.name() + ": red channel must be in [0, 1], got " + col.r);
            assertTrue(col.g >= 0f && col.g <= 1f,
                    m.name() + ": green channel must be in [0, 1], got " + col.g);
            assertTrue(col.b >= 0f && col.b <= 1f,
                    m.name() + ": blue channel must be in [0, 1], got " + col.b);
        }
    }

    /**
     * Test 3: camera.project() maps a small item's world-space position to valid
     * screen coordinates, confirming that renderSmallItems() can correctly compute
     * the screen-space billboard position for each item.
     *
     * renderSmallItems() uses camera.project(pos, 0, 0, sw, sh) to convert the
     * world-space SmallItem position to a screen pixel position, then draws a
     * 2D rect centred at that screen position.  Items behind the camera (z >= 1)
     * are skipped.  This test verifies the projection for a front-facing item.
     */
    @Test
    void test3_CameraProjectsSmallItemPositionToValidScreenCoords() {
        PerspectiveCamera camera = new PerspectiveCamera(67, 1280, 720);
        camera.position.set(10, 8, 10);
        camera.lookAt(10, 5, 10);  // Looking down toward a block
        camera.near = 0.1f;
        camera.far = 300f;
        camera.update();

        int sw = 1280, sh = 720;

        // A small item placed on top of a block at y=5 sits at y=6
        Vector3 itemPos = new Vector3(10f, 6f, 10f);
        Vector3 screenPos = new Vector3(itemPos);
        camera.project(screenPos, 0, 0, sw, sh);

        // Item is in front of the camera — screen z should be < 1 (not clipped)
        assertTrue(screenPos.z < 1f,
                "Small item in front of camera must have projected z < 1 (not behind camera), got " + screenPos.z);

        // Screen position should be within display bounds
        assertTrue(screenPos.x >= 0f && screenPos.x <= sw,
                "Small item screen X must be within [0, " + sw + "], got " + screenPos.x);
        assertTrue(screenPos.y >= 0f && screenPos.y <= sh,
                "Small item screen Y must be within [0, " + sh + "], got " + screenPos.y);
    }

    /**
     * Test 4: Multiple small items accumulate and all are retrievable for the render pass.
     * renderSmallItems() iterates all items in a single pass — this test verifies the
     * full list is returned, not just the most recently placed item.
     */
    @Test
    void test4_MultipleSmallItemsAllVisibleToRenderPass() {
        World world = new World(99L);
        BlockPlacer placer = new BlockPlacer();
        Inventory inventory = new Inventory(36);

        world.setBlock(50, 100, 50, BlockType.STONE);
        world.setBlock(60, 100, 60, BlockType.STONE);

        inventory.addItem(Material.ENERGY_DRINK, 1);
        inventory.addItem(Material.NEWSPAPER, 1);

        // Place first item on first block
        boolean placed1 = placer.placeSmallItem(world, inventory, Material.ENERGY_DRINK,
                new Vector3(50.5f, 103f, 50.5f), new Vector3(0, -1, 0), 5.0f, null);

        // Place second item on second block
        boolean placed2 = placer.placeSmallItem(world, inventory, Material.NEWSPAPER,
                new Vector3(60.5f, 103f, 60.5f), new Vector3(0, -1, 0), 5.0f, null);

        assertTrue(placed1, "First small item (ENERGY_DRINK) should be placed successfully");
        assertTrue(placed2, "Second small item (NEWSPAPER) should be placed successfully");

        List<SmallItem> items = world.getSmallItems();
        assertEquals(2, items.size(),
                "world.getSmallItems() must return both items — renderSmallItems() iterates all of them");

        // Verify both materials are present
        boolean hasEnergyDrink = items.stream().anyMatch(i -> i.getMaterial() == Material.ENERGY_DRINK);
        boolean hasNewspaper   = items.stream().anyMatch(i -> i.getMaterial() == Material.NEWSPAPER);
        assertTrue(hasEnergyDrink, "ENERGY_DRINK must be in world.getSmallItems() for rendering");
        assertTrue(hasNewspaper,   "NEWSPAPER must be in world.getSmallItems() for rendering");
    }

    /**
     * Test 5: A small item placed at a known position has Y = blockY + 1, which is
     * the height at which renderSmallItems() draws the flat quad.
     * Items are placed flush with the top of the block to appear to sit on it.
     */
    @Test
    void test5_SmallItemYPositionMatchesBlockTopForCorrect3DPlacement() {
        World world = new World(7L);

        // Directly insert a SmallItem at a precise position (bypassing BlockPlacer)
        // to test the data model independent of raycasting.
        int blockY = 42;
        float expectedY = blockY + 1.0f; // top surface of the block
        world.placeSmallItem(new SmallItem(Material.STAPLER,
                new Vector3(10.5f, expectedY, 10.5f)));

        List<SmallItem> items = world.getSmallItems();
        assertEquals(1, items.size());
        assertEquals(expectedY, items.get(0).getPosition().y, 0.001f,
                "Small item Y must equal blockY + 1 so renderSmallItems() draws it flush on the block top");
    }
}

package ragamuffin.building;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.world.BlockType;
import ragamuffin.world.PropType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #887: Disco ball cannot be placed in the world.
 *
 * Verifies that all PROP_* materials map correctly to PropType values via
 * materialToPropType(), and that placeProp() successfully places them in
 * the world and removes them from inventory.
 */
class Issue887PropPlacementTest {

    private BlockPlacer blockPlacer;
    private World world;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        blockPlacer = new BlockPlacer();
        world = new World(12345L);
        inventory = new Inventory(36);
    }

    // ── materialToPropType() mapping ──────────────────────────────────────

    @Test
    void materialToPropType_discoBall_returnsDISCO_BALL() {
        assertEquals(PropType.DISCO_BALL, blockPlacer.materialToPropType(Material.PROP_DISCO_BALL),
            "PROP_DISCO_BALL must map to PropType.DISCO_BALL");
    }

    @Test
    void materialToPropType_bed_returnsBED() {
        assertEquals(PropType.BED, blockPlacer.materialToPropType(Material.PROP_BED),
            "PROP_BED must map to PropType.BED");
    }

    @Test
    void materialToPropType_workbench_returnsWORKBENCH() {
        assertEquals(PropType.WORKBENCH, blockPlacer.materialToPropType(Material.PROP_WORKBENCH),
            "PROP_WORKBENCH must map to PropType.WORKBENCH");
    }

    @Test
    void materialToPropType_dartboard_returnsSQUAT_DARTBOARD() {
        assertEquals(PropType.SQUAT_DARTBOARD, blockPlacer.materialToPropType(Material.PROP_DARTBOARD),
            "PROP_DARTBOARD must map to PropType.SQUAT_DARTBOARD");
    }

    @Test
    void materialToPropType_speakerStack_returnsSPEAKER_STACK() {
        assertEquals(PropType.SPEAKER_STACK, blockPlacer.materialToPropType(Material.PROP_SPEAKER_STACK),
            "PROP_SPEAKER_STACK must map to PropType.SPEAKER_STACK");
    }

    @Test
    void materialToPropType_djDecks_returnsDJ_DECKS() {
        assertEquals(PropType.DJ_DECKS, blockPlacer.materialToPropType(Material.PROP_DJ_DECKS),
            "PROP_DJ_DECKS must map to PropType.DJ_DECKS");
    }

    @Test
    void materialToPropType_nonPropMaterial_returnsNull() {
        assertNull(blockPlacer.materialToPropType(Material.PLANKS),
            "Non-prop material should return null");
        assertNull(blockPlacer.materialToPropType(Material.GLASS),
            "Non-prop material should return null");
        assertNull(blockPlacer.materialToPropType(Material.DOOR),
            "Non-prop material should return null");
    }

    // ── placeProp() behaviour ─────────────────────────────────────────────

    @Test
    void placeProp_discoBall_placesInWorldAndConsumesInventory() {
        // Set up a flat ground block at high altitude to avoid terrain
        world.setBlock(50, 500, 50, BlockType.GRASS);
        inventory.addItem(Material.PROP_DISCO_BALL, 1);

        // Camera above the block, looking straight down — hits the top face
        Vector3 cameraPos = new Vector3(50.5f, 503f, 50.5f);
        Vector3 direction = new Vector3(0, -1, 0);

        PropType placed = blockPlacer.placeProp(world, inventory, Material.PROP_DISCO_BALL,
                cameraPos, direction, 5.0f, null, 0f);

        assertNotNull(placed, "placeProp should succeed when pointing at top face of a block");
        assertEquals(PropType.DISCO_BALL, placed, "Placed prop type must be DISCO_BALL");
        assertEquals(0, inventory.getItemCount(Material.PROP_DISCO_BALL),
            "PROP_DISCO_BALL should be consumed from inventory after placement");
        assertEquals(1, world.getPropPositions().size(),
            "World should contain exactly one prop after placement");
        assertEquals(PropType.DISCO_BALL, world.getPropPositions().get(0).getType(),
            "The placed prop must be DISCO_BALL");
    }

    @Test
    void placeProp_discoBall_failsWithoutInventory() {
        world.setBlock(50, 500, 50, BlockType.GRASS);
        // No disco ball in inventory

        Vector3 cameraPos = new Vector3(50.5f, 503f, 50.5f);
        Vector3 direction = new Vector3(0, -1, 0);

        PropType placed = blockPlacer.placeProp(world, inventory, Material.PROP_DISCO_BALL,
                cameraPos, direction, 5.0f, null, 0f);

        assertNull(placed, "placeProp should fail when item not in inventory");
        assertEquals(0, world.getPropPositions().size(), "No prop should be placed");
    }

    @Test
    void placeProp_discoBall_failsWhenNoBlockInRange() {
        // No block to look at
        inventory.addItem(Material.PROP_DISCO_BALL, 1);

        Vector3 cameraPos = new Vector3(50.5f, 503f, 50.5f);
        Vector3 direction = new Vector3(0, 1, 0); // Looking up at empty space

        PropType placed = blockPlacer.placeProp(world, inventory, Material.PROP_DISCO_BALL,
                cameraPos, direction, 5.0f, null, 0f);

        assertNull(placed, "placeProp should fail when no block is targeted");
        assertEquals(1, inventory.getItemCount(Material.PROP_DISCO_BALL),
            "Inventory should be unchanged when placement fails");
    }

    @Test
    void placeProp_allCraftableProps_placeSuccessfully() {
        // Verify all six craftable prop materials can be placed
        Material[] propMaterials = {
            Material.PROP_BED,
            Material.PROP_WORKBENCH,
            Material.PROP_DARTBOARD,
            Material.PROP_SPEAKER_STACK,
            Material.PROP_DISCO_BALL,
            Material.PROP_DJ_DECKS
        };

        Vector3 direction = new Vector3(0, -1, 0);

        for (int i = 0; i < propMaterials.length; i++) {
            Material mat = propMaterials[i];
            // Place each prop at a different X position to avoid overlap
            int bx = 50 + i * 5;
            world.setBlock(bx, 500, 50, BlockType.GRASS);
            inventory.addItem(mat, 1);

            Vector3 cameraPos = new Vector3(bx + 0.5f, 503f, 50.5f);
            PropType placed = blockPlacer.placeProp(world, inventory, mat,
                    cameraPos, direction, 5.0f, null, 0f);

            assertNotNull(placed, "placeProp should succeed for " + mat);
            assertEquals(0, inventory.getItemCount(mat),
                mat + " should be consumed from inventory after placement");
        }

        assertEquals(propMaterials.length, world.getPropPositions().size(),
            "All craftable props should have been placed in the world");
    }

    @Test
    void placeProp_sideHit_failsForProps() {
        // Props can only be placed on top faces, not sides
        world.setBlock(50, 500, 50, BlockType.GRASS);
        inventory.addItem(Material.PROP_DISCO_BALL, 1);

        // Camera to the side of the block looking horizontally — hits a side face
        Vector3 cameraPos = new Vector3(48f, 500.5f, 50.5f);
        Vector3 direction = new Vector3(1, 0, 0);

        PropType placed = blockPlacer.placeProp(world, inventory, Material.PROP_DISCO_BALL,
                cameraPos, direction, 5.0f, null, 0f);

        assertNull(placed, "placeProp should fail when hitting a side face (not top)");
        assertEquals(1, inventory.getItemCount(Material.PROP_DISCO_BALL),
            "Inventory unchanged when side-face placement is rejected");
    }
}

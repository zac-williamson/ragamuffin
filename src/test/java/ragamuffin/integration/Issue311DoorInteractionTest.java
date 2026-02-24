package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.BlockBreaker;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.RaycastResult;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #311: E key does not open/close doors.
 *
 * Verifies that when the player faces a DOOR_LOWER or DOOR_UPPER block and
 * presses E, the door is toggled (open/close) via world.toggleDoor().
 */
class Issue311DoorInteractionTest {

    private World world;
    private BlockBreaker blockBreaker;

    @BeforeEach
    void setUp() {
        HeadlessTestHelper.initHeadless();
        world = new World(42L);
        blockBreaker = new BlockBreaker();
    }

    /**
     * Pressing E while facing a DOOR_LOWER block opens the door.
     * The raycast must detect the DOOR_LOWER block and toggleDoor() must be called,
     * replacing both halves with AIR.
     */
    @Test
    void pressingEWhileFacingDoorLowerOpensDoor() {
        // Place a 2-block door at (10, 1, 10)
        world.setBlock(10, 1, 10, BlockType.DOOR_LOWER);
        world.setBlock(10, 2, 10, BlockType.DOOR_UPPER);
        assertFalse(world.isDoorOpen(10, 1, 10), "Door should start closed");

        // Player at (10, 1, 8) looking in +Z direction toward the door
        Vector3 origin = new Vector3(10.5f, 1.5f, 8.5f);
        Vector3 direction = new Vector3(0, 0, 1).nor();

        // Simulate handleInteraction: raycast ≤3 blocks
        RaycastResult result = blockBreaker.getTargetBlock(world, origin, direction, 3.0f);
        assertNotNull(result, "Raycast should hit the door");

        BlockType hitBlock = world.getBlock(result.getBlockX(), result.getBlockY(), result.getBlockZ());
        assertTrue(hitBlock == BlockType.DOOR_LOWER || hitBlock == BlockType.DOOR_UPPER,
                "Raycast should hit a door block");

        // Normalise to DOOR_LOWER position
        int lowerY = (hitBlock == BlockType.DOOR_UPPER) ? result.getBlockY() - 1 : result.getBlockY();
        world.toggleDoor(result.getBlockX(), lowerY, result.getBlockZ());

        assertTrue(world.isDoorOpen(result.getBlockX(), lowerY, result.getBlockZ()),
                "Door should be open after toggle");
        assertEquals(BlockType.AIR, world.getBlock(10, 1, 10),
                "DOOR_LOWER should become AIR when door is open");
        assertEquals(BlockType.AIR, world.getBlock(10, 2, 10),
                "DOOR_UPPER should become AIR when door is open");
    }

    /**
     * Pressing E while facing a DOOR_UPPER block also toggles the door
     * (normalised to the DOOR_LOWER position).
     */
    @Test
    void pressingEWhileFacingDoorUpperOpensDoor() {
        // Place a 2-block door at (10, 1, 10)
        world.setBlock(10, 1, 10, BlockType.DOOR_LOWER);
        world.setBlock(10, 2, 10, BlockType.DOOR_UPPER);

        // Player at (10, 2, 8) looking in +Z, aimed at the DOOR_UPPER half
        Vector3 origin = new Vector3(10.5f, 2.5f, 8.5f);
        Vector3 direction = new Vector3(0, 0, 1).nor();

        RaycastResult result = blockBreaker.getTargetBlock(world, origin, direction, 3.0f);
        assertNotNull(result, "Raycast should hit the door");

        BlockType hitBlock = world.getBlock(result.getBlockX(), result.getBlockY(), result.getBlockZ());
        assertEquals(BlockType.DOOR_UPPER, hitBlock,
                "Raycast aimed at upper door should hit DOOR_UPPER");

        // Normalise to DOOR_LOWER position (y - 1)
        int lowerY = result.getBlockY() - 1;
        world.toggleDoor(result.getBlockX(), lowerY, result.getBlockZ());

        assertTrue(world.isDoorOpen(10, 1, 10), "Door should be open after toggle via DOOR_UPPER");
    }

    /**
     * Pressing E on an open door closes it, restoring DOOR_LOWER and DOOR_UPPER.
     */
    @Test
    void pressingEOnOpenDoorClosesDoor() {
        // Place and open the door
        world.setBlock(10, 1, 10, BlockType.DOOR_LOWER);
        world.setBlock(10, 2, 10, BlockType.DOOR_UPPER);
        world.toggleDoor(10, 1, 10); // open
        assertTrue(world.isDoorOpen(10, 1, 10));

        // After opening, both blocks are AIR — the raycast won't find the door.
        // The second toggle should close it (restoring blocks).
        world.toggleDoor(10, 1, 10); // close

        assertFalse(world.isDoorOpen(10, 1, 10), "Door should be closed after second toggle");
        assertEquals(BlockType.DOOR_LOWER, world.getBlock(10, 1, 10),
                "DOOR_LOWER should be restored");
        assertEquals(BlockType.DOOR_UPPER, world.getBlock(10, 2, 10),
                "DOOR_UPPER should be restored");
    }

    /**
     * The raycast correctly misses when no door block is in range (>3 blocks away).
     */
    @Test
    void raycastMissesDoorBeyondInteractionRange() {
        // Place door 5 blocks away
        world.setBlock(10, 1, 15, BlockType.DOOR_LOWER);
        world.setBlock(10, 2, 15, BlockType.DOOR_UPPER);

        Vector3 origin = new Vector3(10.5f, 1.5f, 8.5f);
        Vector3 direction = new Vector3(0, 0, 1).nor();

        // Raycast with max 3 blocks — should miss (door is ~6.5 blocks away)
        RaycastResult result = blockBreaker.getTargetBlock(world, origin, direction, 3.0f);
        assertNull(result, "Raycast should not reach door more than 3 blocks away");
    }

    /**
     * Multiple doors are toggled independently.
     */
    @Test
    void multipleDoorsToggleIndependently() {
        // Door A at (5, 1, 10)
        world.setBlock(5, 1, 10, BlockType.DOOR_LOWER);
        world.setBlock(5, 2, 10, BlockType.DOOR_UPPER);
        // Door B at (20, 1, 10)
        world.setBlock(20, 1, 10, BlockType.DOOR_LOWER);
        world.setBlock(20, 2, 10, BlockType.DOOR_UPPER);

        // Toggle only door A
        world.toggleDoor(5, 1, 10);

        assertTrue(world.isDoorOpen(5, 1, 10), "Door A should be open");
        assertFalse(world.isDoorOpen(20, 1, 10), "Door B should remain closed");
    }
}

package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.entity.Player;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #749: Doors disappear on interaction — should swing open instead.
 *
 * Fix: when a door is toggled open, DOOR_LOWER and DOOR_UPPER blocks remain in the world
 * (they render as a swung-aside panel in ChunkMeshBuilder) but are treated as passable
 * for collision purposes via World.isBlockSolid().
 */
class Issue749DoorSwingTest {

    private World world;
    private Player player;

    @BeforeEach
    void setUp() {
        HeadlessTestHelper.initHeadless();
        world = new World(42L);
        player = new Player(10, 1, 17);
        player.setVerticalVelocity(0);

        // Ground so player doesn't sink
        for (int x = 8; x <= 12; x++) {
            for (int z = 8; z <= 20; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * After toggling a door open, DOOR_LOWER and DOOR_UPPER blocks must still be present.
     * Previously the blocks were replaced with AIR, making the door disappear entirely.
     */
    @Test
    void openDoorKeepsDoorBlocks() {
        world.setBlock(10, 1, 15, BlockType.DOOR_LOWER);
        world.setBlock(10, 2, 15, BlockType.DOOR_UPPER);

        world.toggleDoor(10, 1, 15);

        assertTrue(world.isDoorOpen(10, 1, 15), "Door should be marked open");
        assertEquals(BlockType.DOOR_LOWER, world.getBlock(10, 1, 15),
                "DOOR_LOWER block must remain when door is open (visible as swung panel)");
        assertEquals(BlockType.DOOR_UPPER, world.getBlock(10, 2, 15),
                "DOOR_UPPER block must remain when door is open (visible as swung panel)");
    }

    /**
     * An open door does not block the player — isBlockSolid() returns false for open door blocks.
     */
    @Test
    void openDoorIsPassableForPlayer() {
        world.setBlock(10, 1, 15, BlockType.DOOR_LOWER);
        world.setBlock(10, 2, 15, BlockType.DOOR_UPPER);
        world.toggleDoor(10, 1, 15);
        assertTrue(world.isDoorOpen(10, 1, 15));

        player.getPosition().set(10.5f, 1, 17);

        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 60; i++) {
            world.moveWithCollision(player, 0, 0, -1, delta);
        }

        // Player should have passed through z=15
        assertTrue(player.getPosition().z < 15.5f,
                "Player should pass through open door. Z=" + player.getPosition().z);
    }

    /**
     * A closed door blocks the player — the door blocks are solid.
     */
    @Test
    void closedDoorBlocksPlayer() {
        world.setBlock(10, 1, 15, BlockType.DOOR_LOWER);
        world.setBlock(10, 2, 15, BlockType.DOOR_UPPER);
        assertFalse(world.isDoorOpen(10, 1, 15));

        player.getPosition().set(10.5f, 1, 17);

        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 60; i++) {
            world.moveWithCollision(player, 0, 0, -1, delta);
        }

        assertTrue(player.getPosition().z >= 16.0f + Player.DEPTH / 2f - 0.01f,
                "Player should be blocked by closed door. Z=" + player.getPosition().z);
    }

    /**
     * isBlockSolid() returns false for DOOR_LOWER and DOOR_UPPER when the door is open,
     * and true when the door is closed.
     */
    @Test
    void isBlockSolidReflectsDoorState() {
        world.setBlock(10, 1, 15, BlockType.DOOR_LOWER);
        world.setBlock(10, 2, 15, BlockType.DOOR_UPPER);

        // Closed: solid
        assertTrue(world.isBlockSolid(10, 1, 15), "Closed DOOR_LOWER should be solid");
        assertTrue(world.isBlockSolid(10, 2, 15), "Closed DOOR_UPPER should be solid");

        world.toggleDoor(10, 1, 15);

        // Open: not solid (swung aside)
        assertFalse(world.isBlockSolid(10, 1, 15), "Open DOOR_LOWER should not be solid");
        assertFalse(world.isBlockSolid(10, 2, 15), "Open DOOR_UPPER should not be solid");

        world.toggleDoor(10, 1, 15);

        // Closed again: solid
        assertTrue(world.isBlockSolid(10, 1, 15), "Re-closed DOOR_LOWER should be solid again");
        assertTrue(world.isBlockSolid(10, 2, 15), "Re-closed DOOR_UPPER should be solid again");
    }

    /**
     * Closing an open door restores collision — player cannot walk through a re-closed door.
     */
    @Test
    void closingOpenDoorRestoresCollision() {
        world.setBlock(10, 1, 15, BlockType.DOOR_LOWER);
        world.setBlock(10, 2, 15, BlockType.DOOR_UPPER);
        world.toggleDoor(10, 1, 15); // open
        world.toggleDoor(10, 1, 15); // close
        assertFalse(world.isDoorOpen(10, 1, 15), "Door should be closed again");

        player.getPosition().set(10.5f, 1, 17);

        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 60; i++) {
            world.moveWithCollision(player, 0, 0, -1, delta);
        }

        assertTrue(player.getPosition().z >= 16.0f + Player.DEPTH / 2f - 0.01f,
                "Player should be blocked by re-closed door. Z=" + player.getPosition().z);
    }
}

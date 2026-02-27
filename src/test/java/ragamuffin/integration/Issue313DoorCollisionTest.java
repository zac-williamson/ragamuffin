package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.entity.Player;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #313: Closed doors have no collision — player walks through them.
 *
 * Verifies that DOOR_LOWER and DOOR_UPPER are solid (blocking player movement when closed),
 * and that open doors (swing aside via toggleDoor()) are passable.
 * Fix #749: open doors keep their block type but are treated as non-solid for collision.
 */
class Issue313DoorCollisionTest {

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
     * A closed door (DOOR_LOWER + DOOR_UPPER) blocks the player.
     * Player at (10.5, 1, 17), door at z=15. Simulating W (negative Z) for 60 frames
     * should leave the player stopped at the door boundary.
     */
    @Test
    void closedDoorBlocksPlayerMovement() {
        // Place a closed 2-block door at x=10, z=15
        world.setBlock(10, 1, 15, BlockType.DOOR_LOWER);
        world.setBlock(10, 2, 15, BlockType.DOOR_UPPER);
        assertFalse(world.isDoorOpen(10, 1, 15), "Door should start closed");

        player.getPosition().set(10.5f, 1, 17);

        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 60; i++) {
            world.moveWithCollision(player, 0, 0, -1, delta);
        }

        // Door lower block is at z=15; player (depth ~0.6, half ~0.3) must stay at z >= 15+1+0.3 = 16.3
        // But the front face of the door block is at z=16 (block occupies [15,16]).
        // So player Z should be >= 16.0 + Player.DEPTH/2
        assertTrue(player.getPosition().z >= 16.0f + Player.DEPTH / 2f - 0.01f,
                "Player should be blocked by closed door. Z=" + player.getPosition().z
                        + " expected >= " + (16.0f + Player.DEPTH / 2f));

        // Door blocks remain
        assertEquals(BlockType.DOOR_LOWER, world.getBlock(10, 1, 15), "DOOR_LOWER should still be present");
        assertEquals(BlockType.DOOR_UPPER, world.getBlock(10, 2, 15), "DOOR_UPPER should still be present");
    }

    /**
     * An open door (replaced with AIR by toggleDoor()) does not block the player.
     * Same setup as above, but the door is opened first — player should pass through.
     */
    @Test
    void openDoorAllowsPlayerMovement() {
        // Place and immediately open the door
        world.setBlock(10, 1, 15, BlockType.DOOR_LOWER);
        world.setBlock(10, 2, 15, BlockType.DOOR_UPPER);
        world.toggleDoor(10, 1, 15); // opens door — panel swings aside
        assertTrue(world.isDoorOpen(10, 1, 15), "Door should be open");
        // Fix #749: blocks remain present but are passable (swung aside visually)
        assertEquals(BlockType.DOOR_LOWER, world.getBlock(10, 1, 15), "DOOR_LOWER block remains when open");
        assertEquals(BlockType.DOOR_UPPER, world.getBlock(10, 2, 15), "DOOR_UPPER block remains when open");

        player.getPosition().set(10.5f, 1, 17);

        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 60; i++) {
            world.moveWithCollision(player, 0, 0, -1, delta);
        }

        // Player should have passed through z=15 (door opening)
        assertTrue(player.getPosition().z < 15.5f,
                "Player should pass through open door. Z=" + player.getPosition().z);
    }

    /**
     * Toggling a door closed restores collision — player cannot walk through.
     */
    @Test
    void closingDoorRestoresCollision() {
        // Open then close
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

package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.entity.Player;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #885: Ladders cannot be climbed.
 *
 * <p>Verifies that when a player's AABB overlaps a LADDER block:
 * <ul>
 *   <li>Gravity is suppressed (player does not fall through the ladder).</li>
 *   <li>A positive {@code verticalVelocity} moves the player upward (climbing).</li>
 *   <li>A negative {@code verticalVelocity} moves the player downward.</li>
 *   <li>With zero vertical velocity the player hovers in place.</li>
 *   <li>A mid-air fall is cancelled on grabbing a ladder (no fall damage).</li>
 *   <li>Normal gravity resumes once the player leaves the ladder column.</li>
 * </ul>
 */
class Issue885LadderClimbingTest {

    private static final float DELTA = 1f / 60f; // one frame at 60 fps

    private World world;
    private Player player;

    @BeforeEach
    void setUp() {
        world = new World(0L);
        // Place a solid floor so the player has somewhere to land when not on a ladder
        world.setBlock(0, 0, 0, BlockType.GRASS);
        // Place a ladder at y=1 (directly above the floor)
        world.setBlock(0, 1, 0, BlockType.LADDER);
        // Spawn the player overlapping the ladder block
        player = new Player(0, 1, 0);
    }

    // -----------------------------------------------------------------------
    // isOnLadder detection
    // -----------------------------------------------------------------------

    @Test
    void playerOnLadderIsDetected() {
        assertTrue(world.isOnLadder(player),
                "Player overlapping a LADDER block should be detected as on a ladder");
    }

    @Test
    void playerNotOnLadderWhenNoLadderPresent() {
        // Move player away from the ladder block
        player = new Player(5, 2, 5);
        assertFalse(world.isOnLadder(player),
                "Player with no LADDER in AABB should not be detected as on a ladder");
    }

    // -----------------------------------------------------------------------
    // Gravity suppression
    // -----------------------------------------------------------------------

    @Test
    void gravityIsSuppressedOnLadder() {
        // Player is on the ladder; gravity must not pull them down
        float yBefore = player.getPosition().y;

        for (int i = 0; i < 60; i++) {
            world.applyGravityAndVerticalCollision(player, DELTA);
        }

        float yAfter = player.getPosition().y;
        // With zero vertical velocity and gravity suppressed, Y must not decrease
        assertEquals(yBefore, yAfter, 0.01f,
                "Player Y must not decrease while on a ladder with no climb input "
                        + "(was=" + yBefore + " now=" + yAfter + ")");
    }

    // -----------------------------------------------------------------------
    // Climbing upward
    // -----------------------------------------------------------------------

    @Test
    void playerClimbsUpWhenPositiveVerticalVelocitySet() {
        // Add more ladder blocks above so the player can climb
        world.setBlock(0, 2, 0, BlockType.LADDER);
        world.setBlock(0, 3, 0, BlockType.LADDER);
        world.setBlock(0, 4, 0, BlockType.LADDER);

        float yBefore = player.getPosition().y;

        // Simulate forward (W) key: set climb velocity each frame before gravity runs
        for (int i = 0; i < 60; i++) {
            player.setVerticalVelocity(Player.CLIMB_SPEED);
            world.applyGravityAndVerticalCollision(player, DELTA);
        }

        float yAfter = player.getPosition().y;
        assertTrue(yAfter > yBefore,
                "Player Y must increase when climbing up a ladder "
                        + "(was=" + yBefore + " now=" + yAfter + ")");
    }

    // -----------------------------------------------------------------------
    // Climbing downward
    // -----------------------------------------------------------------------

    @Test
    void playerClimbsDownWhenNegativeVerticalVelocitySet() {
        // Add ladder blocks below to climb down into
        world.setBlock(0, 1, 0, BlockType.LADDER);
        world.setBlock(0, 2, 0, BlockType.LADDER);
        world.setBlock(0, 3, 0, BlockType.LADDER);
        // Spawn player higher up on the ladder
        player = new Player(0, 3, 0);

        float yBefore = player.getPosition().y;

        // Simulate backward (S) key: set negative climb velocity each frame
        for (int i = 0; i < 30; i++) {
            player.setVerticalVelocity(-Player.CLIMB_SPEED);
            world.applyGravityAndVerticalCollision(player, DELTA);
        }

        float yAfter = player.getPosition().y;
        assertTrue(yAfter < yBefore,
                "Player Y must decrease when climbing down a ladder "
                        + "(was=" + yBefore + " now=" + yAfter + ")");
    }

    // -----------------------------------------------------------------------
    // No fall damage when grabbing ladder mid-fall
    // -----------------------------------------------------------------------

    @Test
    void fallingIntoLadderCancelsFallNoDamage() {
        // Player spawned high above the ladder and falls into it
        player = new Player(0, 20, 0);
        // Place ladders from y=1 to y=18 so the player intercepts them mid-fall
        for (int y = 1; y <= 18; y++) {
            world.setBlock(0, y, 0, BlockType.LADDER);
        }

        float healthBefore = player.getHealth();

        // Simulate fall — gravity runs each frame; player eventually hits the ladder column
        for (int i = 0; i < 300; i++) {
            world.applyGravityAndVerticalCollision(player, DELTA);
            if (world.isOnLadder(player)) break;
        }

        // Once on the ladder, gravity must be suppressed and no fall damage dealt
        assertTrue(world.isOnLadder(player),
                "Player should have grabbed the ladder while falling");
        assertEquals(healthBefore, player.getHealth(), 0.001f,
                "No fall damage should be applied when the player grabs a ladder mid-fall");
    }

    // -----------------------------------------------------------------------
    // Gravity resumes after leaving ladder
    // -----------------------------------------------------------------------

    @Test
    void gravityResumesAfterLeavingLadder() {
        // Player is NOT on any ladder (no ladder in setUp touches this new position)
        player = new Player(5, 10, 5);

        float yBefore = player.getPosition().y;

        // Simulate 30 frames — gravity should pull the player down
        for (int i = 0; i < 30; i++) {
            world.applyGravityAndVerticalCollision(player, DELTA);
        }

        float yAfter = player.getPosition().y;
        assertTrue(yAfter < yBefore,
                "Gravity must apply when the player is not on a ladder "
                        + "(was=" + yBefore + " now=" + yAfter + ")");
    }
}

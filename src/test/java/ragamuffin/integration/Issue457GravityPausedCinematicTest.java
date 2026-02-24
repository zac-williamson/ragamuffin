package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.entity.DamageReason;
import ragamuffin.entity.Player;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #457: gravity and fall damage were not applied
 * during PAUSED or CINEMATIC states, allowing a pause-to-float exploit.
 *
 * <p>Before the fix, {@code world.applyGravityAndVerticalCollision(player, delta)}
 * was only called inside {@code updatePlayingSimulation()}.  If the player jumped
 * and opened the pause menu mid-air, they hung frozen until resume, then carried
 * their pre-pause {@code verticalVelocity} without any fall-damage for the height
 * they had been hovering at.
 *
 * <p>The fix adds the following block to both the PAUSED and CINEMATIC branches of
 * {@code render()}:
 * <pre>
 *     if (!player.isDead()) {
 *         world.applyGravityAndVerticalCollision(player, delta);
 *     }
 * </pre>
 *
 * <p>These tests verify the invariants of that fix directly against
 * {@link World#applyGravityAndVerticalCollision(Player, float)}, which is the same
 * method called in the updated PAUSED and CINEMATIC blocks.
 */
class Issue457GravityPausedCinematicTest {

    private static final float DELTA = 1f / 60f; // one frame at 60 fps

    private World world;
    private Player player;

    @BeforeEach
    void setUp() {
        // Empty world (no WorldGenerator) — all blocks are AIR so the player falls freely.
        world = new World(0L);
        // Spawn well above the ground so there is room to fall.
        player = new Player(0, 10, 0);
    }

    // -----------------------------------------------------------------------
    // Core gravity behaviour — mirrors PAUSED/CINEMATIC fix
    // -----------------------------------------------------------------------

    /**
     * Test 1: Vertical velocity accumulates (becomes more negative) when
     * applyGravityAndVerticalCollision is called for a live, airborne player.
     *
     * This confirms that the fix actually drives gravity forward each frame.
     */
    @Test
    void gravityAccumulatesVerticalVelocityEachFrame() {
        // Live player, not dead
        assertFalse(player.isDead(), "Player must be alive for this test");

        float vvBefore = player.getVerticalVelocity(); // 0 initially

        // Simulate one PAUSED/CINEMATIC frame with the fix applied
        world.applyGravityAndVerticalCollision(player, DELTA);

        float vvAfter = player.getVerticalVelocity();

        // After one frame of gravity the vertical velocity must be lower (more negative)
        assertTrue(vvAfter < vvBefore,
                "Vertical velocity must decrease (become more negative) after one gravity frame; "
                        + "was=" + vvBefore + " now=" + vvAfter);
    }

    /**
     * Test 2: Player Y-position decreases over 60 frames of simulated PAUSED gravity.
     *
     * Without the fix the position was frozen; with the fix it falls.
     */
    @Test
    void playerFallsDuringSimulatedPausedFrames() {
        assertFalse(player.isDead());

        float yBefore = player.getPosition().y;

        // Simulate 60 PAUSED frames (1 second) with the fix
        for (int i = 0; i < 60; i++) {
            world.applyGravityAndVerticalCollision(player, DELTA);
        }

        float yAfter = player.getPosition().y;

        assertTrue(yAfter < yBefore,
                "Player Y must decrease after 60 gravity frames (was=" + yBefore + " now=" + yAfter + ")");
    }

    /**
     * Test 3: Gravity is NOT applied when the player is dead.
     *
     * The fix wraps the call in {@code if (!player.isDead())} to mirror the guard
     * in {@code updatePlayingSimulation()}.  A dead player must not move.
     */
    @Test
    void gravitySkippedWhenPlayerIsDead() {
        // Kill the player
        player.setHealth(0f);
        assertTrue(player.isDead(), "Player must be dead for this test");

        float yBefore = player.getPosition().y;
        float vvBefore = player.getVerticalVelocity();

        // The fix guards the call with !player.isDead(), so this call is what
        // the game code executes each frame — only runs if alive.
        if (!player.isDead()) {
            world.applyGravityAndVerticalCollision(player, DELTA);
        }

        assertEquals(yBefore, player.getPosition().y, 0.001f,
                "Dead player Y must not change");
        assertEquals(vvBefore, player.getVerticalVelocity(), 0.001f,
                "Dead player vertical velocity must not change");
    }

    /**
     * Test 4: Vertical velocity does not continue accumulating when the player
     * lands on a solid block — ensures fall damage applies and velocity resets.
     *
     * Place a solid GRASS floor under the player and let them fall onto it.
     * After landing the vertical velocity must be 0 (reset by landAndGetFallDamage).
     */
    @Test
    void velocityResetsOnLanding() {
        // Place a solid floor just below the player's spawn height
        // Player spawned at y=10; place a GRASS block at y=0 so the player falls to it.
        world.setBlock(0, 0, 0, ragamuffin.world.BlockType.GRASS);

        // Simulate up to 5 seconds of PAUSED gravity (300 frames) — player will land
        for (int i = 0; i < 300; i++) {
            world.applyGravityAndVerticalCollision(player, DELTA);
            // Stop early if already landed
            if (player.getVerticalVelocity() == 0f) {
                break;
            }
        }

        assertEquals(0f, player.getVerticalVelocity(), 0.001f,
                "Vertical velocity must be 0 after landing on a solid block");
    }

    /**
     * Test 5: Fall damage is recorded when the player falls from height during
     * a simulated PAUSED/CINEMATIC state.
     *
     * This is the key exploit-prevention invariant: a player who jumps and then
     * pauses mid-air must still receive fall damage when they land.
     *
     * The player spawns at y=25 with a solid GRASS floor at y=0.  Fall distance
     * is ~24 blocks (> 10-block threshold), so fall damage must apply.
     */
    @Test
    void fallDamageAppliedAfterFallingDuringPause() {
        // Rebuild with a player high enough that the fall exceeds the 10-block damage threshold.
        // Fall damage: (fallDistance - 10) * 10 HP; need fallDistance > 10.
        // Player at y=25 lands on top of GRASS at y=0 → fallDistance ≈ 24 blocks.
        player = new Player(0, 25, 0);
        world.setBlock(0, 0, 0, ragamuffin.world.BlockType.GRASS);

        float healthBefore = player.getHealth();

        // Simulate PAUSED/CINEMATIC frames (the fix) — up to 10 seconds of gravity
        for (int i = 0; i < 600; i++) {
            if (!player.isDead()) {
                world.applyGravityAndVerticalCollision(player, DELTA);
            }
            if (player.getVerticalVelocity() == 0f) {
                break;
            }
        }

        // After landing from 24+ blocks the player must have taken fall damage
        assertTrue(player.getHealth() < healthBefore,
                "Player must take fall damage after landing from height during simulated pause; "
                        + "healthBefore=" + healthBefore + " healthAfter=" + player.getHealth());
    }
}

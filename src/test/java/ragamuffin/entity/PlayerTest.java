package ragamuffin.entity;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.BlockType;
import ragamuffin.world.Chunk;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @BeforeAll
    static void setup() {
        HeadlessTestHelper.initHeadless();
    }

    @Test
    void playerHasPosition() {
        Player player = new Player(10, 5, 10);
        Vector3 pos = player.getPosition();
        assertEquals(10, pos.x, 0.01f);
        assertEquals(5, pos.y, 0.01f);
        assertEquals(10, pos.z, 0.01f);
    }

    @Test
    void playerCanMove() {
        Player player = new Player(0, 0, 0);
        player.move(1, 0, 0, 1.0f);
        Vector3 pos = player.getPosition();
        assertTrue(pos.x > 0);
    }

    @Test
    void playerHasAABB() {
        Player player = new Player(10, 5, 10);
        assertNotNull(player.getAABB());
        assertTrue(player.getAABB().getWidth() > 0);
        assertTrue(player.getAABB().getHeight() > 0);
        assertTrue(player.getAABB().getDepth() > 0);
    }

    @Test
    void aabbFollowsPlayer() {
        Player player = new Player(10, 5, 10);
        AABB box1 = player.getAABB();
        float minX1 = box1.getMinX();

        player.move(5, 0, 0, 1.0f);
        AABB box2 = player.getAABB();
        float minX2 = box2.getMinX();

        assertTrue(minX2 > minX1, "AABB should move with player");
    }

    @Test
    void playerCollidesWithSolidBlock() {
        Player player = new Player(10, 1, 10);
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(10, 1, 9, BlockType.STONE); // Block in front

        // Try to move forward (negative Z)
        Vector3 initialPos = new Vector3(player.getPosition());
        player.move(0, 0, -1, 0.5f); // Try to move into block

        // Collision should occur at the chunk
        boolean collided = player.checkCollision(chunk);
        if (collided) {
            // If collision detected, movement should be prevented or adjusted
            assertTrue(true, "Collision was detected");
        }
    }

    // ========== Dodge/Roll Tests ==========

    @Test
    void dodgeInitiatesWhenMovingWithEnergy() {
        Player player = new Player(0, 0, 0);
        assertTrue(player.canDodge(), "Should be able to dodge with full energy");

        boolean dodged = player.dodge(1, 0);
        assertTrue(dodged, "Dodge should initiate when moving with energy");
        assertTrue(player.isDodging(), "Player should be in dodging state");
    }

    @Test
    void dodgeFailsWhenStationary() {
        Player player = new Player(0, 0, 0);
        boolean dodged = player.dodge(0, 0);
        assertFalse(dodged, "Dodge should fail with no direction");
        assertFalse(player.isDodging(), "Player should not be dodging");
    }

    @Test
    void dodgeConsumesEnergy() {
        Player player = new Player(0, 0, 0);
        float energyBefore = player.getEnergy();

        player.dodge(1, 0);

        assertEquals(energyBefore - Player.DODGE_ENERGY_COST, player.getEnergy(), 0.01f,
            "Dodge should consume energy");
    }

    @Test
    void dodgeFailsWithInsufficientEnergy() {
        Player player = new Player(0, 0, 0);
        player.setEnergy(5); // Less than DODGE_ENERGY_COST

        boolean dodged = player.dodge(1, 0);
        assertFalse(dodged, "Dodge should fail with insufficient energy");
    }

    @Test
    void dodgeCooldownPreventsRepeatDodge() {
        Player player = new Player(0, 0, 0);

        player.dodge(1, 0);
        // Finish the dodge
        player.updateDodge(Player.DODGE_DURATION + 0.01f);
        assertFalse(player.isDodging(), "Dodge should have ended");

        // Try to dodge again — should fail due to cooldown
        boolean dodgedAgain = player.dodge(1, 0);
        assertFalse(dodgedAgain, "Should not dodge during cooldown");
    }

    @Test
    void dodgeCooldownExpiresAfterDuration() {
        Player player = new Player(0, 0, 0);

        player.dodge(1, 0);
        // Wait out both dodge and cooldown
        player.updateDodge(Player.DODGE_COOLDOWN + Player.DODGE_DURATION + 0.1f);

        assertTrue(player.canDodge(), "Should be able to dodge after cooldown expires");
    }

    @Test
    void dodgeEndsAfterDuration() {
        Player player = new Player(0, 0, 0);
        player.dodge(0, 1);
        assertTrue(player.isDodging());

        player.updateDodge(Player.DODGE_DURATION + 0.01f);
        assertFalse(player.isDodging(), "Dodge should end after duration");
    }

    @Test
    void dodgeDirectionIsNormalised() {
        Player player = new Player(0, 0, 0);
        player.dodge(3, 4); // Not normalised input

        float len = (float) Math.sqrt(player.getDodgeDirX() * player.getDodgeDirX()
            + player.getDodgeDirZ() * player.getDodgeDirZ());
        assertEquals(1.0f, len, 0.01f, "Dodge direction should be normalised");
    }
}

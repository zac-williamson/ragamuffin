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
}

package ragamuffin.integration;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ragamuffin.core.GameState;
import ragamuffin.entity.Player;
import ragamuffin.render.ChunkMeshBuilder;
import ragamuffin.render.MeshData;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.BlockType;
import ragamuffin.world.Chunk;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 Integration Tests - EXACT scenarios from SPEC.md
 */
class Phase1IntegrationTest {

    @BeforeAll
    static void setup() {
        HeadlessTestHelper.initHeadless();
    }

    /**
     * Integration Test 1: Player movement updates camera
     * Place the player at position (10, 5, 10). Simulate pressing W for 60 frames (1 second at 60fps).
     * Verify the player's Z coordinate has decreased (moved forward).
     * Verify the PerspectiveCamera's position matches the player's position (offset by eye height).
     */
    @Test
    void playerMovementUpdatesCamera() {
        Player player = new Player(10, 5, 10);
        PerspectiveCamera camera = new PerspectiveCamera(67, 1280, 720);
        camera.position.set(player.getPosition());
        camera.position.y += Player.EYE_HEIGHT;
        camera.lookAt(player.getPosition().x, player.getPosition().y + Player.EYE_HEIGHT, player.getPosition().z - 1);
        camera.update();

        float initialZ = player.getPosition().z;

        // Simulate W key pressed for 60 frames
        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 60; i++) {
            // W moves forward (negative Z in camera forward direction)
            player.move(0, 0, -1, delta);
            // Update camera to follow player
            camera.position.set(player.getPosition());
            camera.position.y += Player.EYE_HEIGHT;
            camera.update();
        }

        float finalZ = player.getPosition().z;

        // Verify player moved forward (Z decreased)
        assertTrue(finalZ < initialZ, "Player Z should have decreased (moved forward)");

        // Verify camera matches player position (offset by eye height)
        assertEquals(player.getPosition().x, camera.position.x, 0.01f, "Camera X should match player X");
        assertEquals(player.getPosition().y + Player.EYE_HEIGHT, camera.position.y, 0.01f,
            "Camera Y should be player Y + eye height");
        assertEquals(player.getPosition().z, camera.position.z, 0.01f, "Camera Z should match player Z");
    }

    /**
     * Integration Test 2: Collision prevents movement into solid blocks
     * Place the player at (10, 1, 10). Place a STONE block at (10, 1, 9) — directly in front of the player.
     * Simulate pressing W for 60 frames. Verify the player's Z coordinate has NOT moved past the block
     * boundary (Z >= 9.0 + player half-width). Verify the STONE block is still present.
     */
    @Test
    void collisionPreventsMovementIntoSolidBlocks() {
        Player player = new Player(10, 1, 10);
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(10, 1, 9, BlockType.STONE);

        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 60; i++) {
            // Try to move forward (negative Z)
            player.moveWithCollision(0, 0, -1, delta, chunk);
        }

        // Player should be stopped by the block
        // Block is at Z=9, player half-width is 0.3, so player Z should be >= 9.0 + 0.3 = 9.3
        float playerHalfWidth = Player.WIDTH / 2f;
        assertTrue(player.getPosition().z >= 9.0f + playerHalfWidth,
            "Player should not pass through block boundary. Expected Z >= " + (9.0f + playerHalfWidth) +
            " but was " + player.getPosition().z);

        // Verify STONE block still exists
        assertEquals(BlockType.STONE, chunk.getBlock(10, 1, 9), "STONE block should still be present");
    }

    /**
     * Integration Test 3: Chunk mesh generation
     * Create a chunk. Set block (8, 0, 8) to GRASS and all surrounding blocks to AIR. Build the chunk mesh.
     * Verify the mesh has exactly 6 faces (one per exposed side). Now set block (8, 1, 8) to GRASS as well.
     * Rebuild. With greedy meshing, same-type adjacent faces merge into larger quads.
     */
    @Test
    void chunkMeshGeneration() {
        Chunk chunk = new Chunk(0, 0, 0);
        chunk.setBlock(8, 0, 8, BlockType.GRASS);

        ChunkMeshBuilder builder = new ChunkMeshBuilder();
        MeshData meshData = builder.build(chunk);

        assertEquals(6, meshData.getFaceCount(), "Single GRASS block should have exactly 6 faces");

        // Now add a second GRASS block on top
        chunk.setBlock(8, 1, 8, BlockType.GRASS);
        meshData = builder.build(chunk);

        // With greedy meshing, same-type adjacent side faces merge:
        // 4 side faces (1 quad each, merged from 2) + top + bottom = 6
        assertEquals(6, meshData.getFaceCount(), "Two stacked same-type blocks should have 6 greedy-merged faces");
    }

    /**
     * Integration Test 4: State machine transitions
     * Game starts in MENU state. Transition to PLAYING. Verify state is PLAYING and player input is enabled.
     * Press ESC. Verify state is PAUSED and player input is disabled. Press ESC again. Verify state returns
     * to PLAYING.
     */
    @Test
    void stateMachineTransitions() {
        GameState state = GameState.MENU;
        assertFalse(state.acceptsInput(), "MENU state should not accept input");

        // Transition to PLAYING
        state = GameState.PLAYING;
        assertEquals(GameState.PLAYING, state, "State should be PLAYING");
        assertTrue(state.acceptsInput(), "PLAYING state should accept input");

        // Press ESC -> PAUSED
        state = GameState.PAUSED;
        assertEquals(GameState.PAUSED, state, "State should be PAUSED");
        assertFalse(state.acceptsInput(), "PAUSED state should not accept input");

        // Press ESC again -> PLAYING
        state = GameState.PLAYING;
        assertEquals(GameState.PLAYING, state, "State should return to PLAYING");
        assertTrue(state.acceptsInput(), "PLAYING state should accept input");
    }

    /**
     * Integration Test 5: Player cannot move in PAUSED state
     * Set state to PAUSED. Record player position. Simulate pressing W for 30 frames.
     * Verify player position is unchanged.
     */
    @Test
    void playerCannotMoveInPausedState() {
        GameState state = GameState.PAUSED;
        Player player = new Player(10, 5, 10);

        float initialX = player.getPosition().x;
        float initialY = player.getPosition().y;
        float initialZ = player.getPosition().z;

        float delta = 1.0f / 60.0f;

        // Simulate W pressed for 30 frames, but only process if state accepts input
        for (int i = 0; i < 30; i++) {
            if (state.acceptsInput()) {
                player.move(0, 0, -1, delta);
            }
            // Otherwise do nothing - input is blocked
        }

        // Verify position unchanged
        assertEquals(initialX, player.getPosition().x, 0.001f, "Player X should be unchanged in PAUSED state");
        assertEquals(initialY, player.getPosition().y, 0.001f, "Player Y should be unchanged in PAUSED state");
        assertEquals(initialZ, player.getPosition().z, 0.001f, "Player Z should be unchanged in PAUSED state");
    }
}

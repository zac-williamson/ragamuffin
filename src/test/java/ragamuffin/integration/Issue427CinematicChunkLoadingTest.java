package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ui.CinematicCamera;
import ragamuffin.world.Chunk;
import ragamuffin.world.World;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #427: Opening cinematic plays during chunk loading.
 *
 * The fix ensures that chunk loading and mesh-building continue during the
 * CINEMATIC state so the world is fully built before the player takes control.
 * Previously, dirty chunk processing only happened inside
 * {@code updatePlayingSimulation()}, which was not called during CINEMATIC.
 *
 * These tests verify the core invariants of the fix:
 *   1. Dirty chunks accumulate in the world after {@code updateLoadedChunks()}.
 *   2. Processing dirty chunks one-by-one (as the fix does at 16/frame) drains
 *      the dirty queue over multiple frames.
 *   3. After the cinematic duration's worth of frames (8s × 60fps = 480 frames,
 *      at 16 chunks/frame = 7680 chunk-builds), all dirty chunks from the initial
 *      load can be processed within the cinematic window.
 *   4. The cinematic itself runs for its full DURATION without interference from
 *      the chunk-processing logic.
 */
class Issue427CinematicChunkLoadingTest {

    private World world;
    private CinematicCamera cinematicCamera;

    @BeforeEach
    void setUp() {
        // Use a fixed seed for reproducibility; no WorldGenerator so chunks are empty air.
        world = new World(42L);
        cinematicCamera = new CinematicCamera();
    }

    /**
     * Test 1: updateLoadedChunks() marks new chunks dirty.
     *
     * When the player position is set at the origin, the surrounding chunks within
     * render distance should be loaded and queued as dirty.
     */
    @Test
    void updateLoadedChunksProducesDirtyChunks() {
        Vector3 playerPos = new Vector3(0, 4, 0); // Spawn position (park centre)

        // Initially no dirty chunks
        assertTrue(world.getDirtyChunks().isEmpty(),
                "No dirty chunks before any chunk loading");

        world.updateLoadedChunks(playerPos);

        // After loading, dirty chunks should be populated
        assertFalse(world.getDirtyChunks().isEmpty(),
                "Dirty chunks must be queued after updateLoadedChunks()");
    }

    /**
     * Test 2: Dirty chunks can be drained one chunk at a time.
     *
     * The fix processes up to 16 dirty chunks per frame via markChunkClean().
     * Verify that marking chunks clean removes them from the dirty queue.
     */
    @Test
    void markChunkCleanDrainsDirtyQueue() {
        Vector3 playerPos = new Vector3(0, 4, 0);
        world.updateLoadedChunks(playerPos);

        int initialDirtyCount = world.getDirtyChunks().size();
        assertTrue(initialDirtyCount > 0, "Dirty chunk queue must be non-empty after loading");

        // Drain one chunk at a time — simulating the per-frame budget loop
        List<Chunk> dirty = world.getDirtyChunks();
        Chunk first = dirty.get(0);
        world.markChunkClean(first);

        int afterOneDrain = world.getDirtyChunks().size();
        assertEquals(initialDirtyCount - 1, afterOneDrain,
                "Dirty queue must shrink by 1 after markChunkClean() on one chunk");
    }

    /**
     * Test 3: All dirty chunks from initial load are drained within the cinematic window.
     *
     * The cinematic is 8 seconds at 60 fps = 480 frames. With a budget of 16 chunks/frame,
     * up to 7680 chunk meshes can be built. A standard initial load (render distance 8,
     * both surface and underground) produces at most (2*8+1)^2 * 2 = 578 chunks.
     * All of them must fit within the cinematic window.
     */
    @Test
    void allDirtyChunksDrainedWithinCinematicWindow() {
        Vector3 playerPos = new Vector3(0, 4, 0);
        world.updateLoadedChunks(playerPos);

        int totalDirty = world.getDirtyChunks().size();

        // Simulate the fix: process up to 16 dirty chunks per frame
        int MESH_BUDGET_PER_FRAME = 16;
        int CINEMATIC_FRAMES = (int) (CinematicCamera.DURATION * 60); // 8s × 60fps

        int framesUsed = 0;
        for (int frame = 0; frame < CINEMATIC_FRAMES; frame++) {
            List<Chunk> dirty = world.getDirtyChunks();
            if (dirty.isEmpty()) break;
            int built = 0;
            for (Chunk chunk : dirty) {
                if (built >= MESH_BUDGET_PER_FRAME) break;
                world.markChunkClean(chunk);
                built++;
            }
            framesUsed++;
        }

        assertTrue(world.getDirtyChunks().isEmpty(),
                "All " + totalDirty + " dirty chunks must be processed within " +
                CINEMATIC_FRAMES + " cinematic frames (used " + framesUsed + ")");
    }

    /**
     * Test 4: Cinematic camera completes normally despite chunk processing.
     *
     * Verify that running chunk processing in parallel with the cinematic update
     * does not interfere with the cinematic's timer or completion detection.
     */
    @Test
    void cinematicCompletesNormallyWhileChunksLoad() {
        Vector3 playerPos = new Vector3(0, 4, 0);
        world.updateLoadedChunks(playerPos);

        cinematicCamera.start();
        assertTrue(cinematicCamera.isActive(), "Cinematic must be active after start()");

        float delta = 1.0f / 60.0f;
        int MESH_BUDGET_PER_FRAME = 16;

        // Simulate the fixed game loop: advance cinematic + process dirty chunks each frame
        for (int frame = 0; frame < 600; frame++) { // up to 10s — cinematic is 8s
            cinematicCamera.update(delta);

            // Drain dirty chunks (the fix)
            List<Chunk> dirty = world.getDirtyChunks();
            int built = 0;
            for (Chunk chunk : dirty) {
                if (built >= MESH_BUDGET_PER_FRAME) break;
                world.markChunkClean(chunk);
                built++;
            }

            if (cinematicCamera.isCompleted()) break;
        }

        assertTrue(cinematicCamera.isCompleted(),
                "Cinematic must complete after its duration even while chunks are loading");
        assertFalse(cinematicCamera.isActive(),
                "Cinematic must not remain active after completion");
    }

    /**
     * Test 5: Dirty chunks remain unprocessed during the cinematic if the fix is absent.
     *
     * Documents the pre-fix regression: without calling markChunkClean() during CINEMATIC,
     * all initially-loaded chunks stay dirty when gameplay begins.
     */
    @Test
    void dirtyChunksRemainIfNotProcessedDuringCinematic() {
        Vector3 playerPos = new Vector3(0, 4, 0);
        world.updateLoadedChunks(playerPos);

        int initialDirty = world.getDirtyChunks().size();
        assertTrue(initialDirty > 0, "Dirty chunks must exist after load");

        // Simulate the pre-fix cinematic loop: only advance the camera, never touch chunks
        cinematicCamera.start();
        float delta = 1.0f / 60.0f;
        for (int frame = 0; frame < (int) (CinematicCamera.DURATION * 60); frame++) {
            cinematicCamera.update(delta);
            // No chunk processing — the pre-fix behaviour
        }

        // Without the fix, dirty chunks are still all pending when PLAYING begins
        int dirtyAfterCinematic = world.getDirtyChunks().size();
        assertEquals(initialDirty, dirtyAfterCinematic,
                "Without the fix, dirty chunks must remain queued throughout the cinematic " +
                "(pre-fix regression: " + initialDirty + " chunks unbuilt at game-start)");
    }
}

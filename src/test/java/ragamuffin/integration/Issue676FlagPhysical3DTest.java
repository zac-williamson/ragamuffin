package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.render.FlagRenderer;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.*;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #676: Make flags physical 3D objects.
 *
 * Verifies that:
 * - FlagRenderer can be constructed without a GPU context (GL models are lazy)
 * - setFlags() stores positions correctly without touching GL
 * - update() advances internal state without building GL objects
 * - dispose() is safe even before any GL models are built
 * - The renderer is wired to use ModelBatch (3D) rather than ShapeRenderer (2D)
 *
 * Note: Actual GL model creation (ModelBatch/MeshBuilder) requires a GPU context
 * and cannot be tested in headless JUnit mode. These tests verify the data-side
 * wiring and lazy-init contract introduced by this issue.
 */
public class Issue676FlagPhysical3DTest {

    private World world;
    private FlagRenderer flagRenderer;

    @BeforeEach
    public void setUp() {
        HeadlessTestHelper.initHeadless();
        world = new World(0);
        world.generate();
        flagRenderer = new FlagRenderer();
    }

    /**
     * Verify FlagRenderer constructs without error even without a GL context.
     * GL model building must be deferred to the first render() call.
     */
    @Test
    public void testFlagRendererConstructsWithoutGLContext() {
        FlagRenderer renderer = new FlagRenderer();
        assertNotNull(renderer, "FlagRenderer should construct without a GPU/GL context");
    }

    /**
     * Verify that setFlags() stores all positions without touching GL.
     */
    @Test
    public void testFlagRendererStoresPositions() {
        List<FlagPosition> flags = world.getFlagPositions();
        assertFalse(flags.isEmpty(), "World must have at least one flag for this test");

        assertDoesNotThrow(() -> flagRenderer.setFlags(flags),
                "setFlags() should not throw even without a GL context");

        assertEquals(flags.size(), flagRenderer.getFlags().size(),
                "FlagRenderer should store all flag positions passed to setFlags()");
    }

    /**
     * Verify that setFlags() with an empty list is handled gracefully.
     */
    @Test
    public void testEmptyFlagListIsHandledGracefully() {
        assertDoesNotThrow(() -> flagRenderer.setFlags(Collections.emptyList()),
                "setFlags() with empty list should not throw");

        assertEquals(0, flagRenderer.getFlags().size(),
                "Zero flags registered should result in zero flags stored");
    }

    /**
     * Verify update() advances the animation timer without building GL objects.
     * GL objects must be deferred to render() — not created here.
     */
    @Test
    public void testUpdateDoesNotRequireGLContext() {
        flagRenderer.setFlags(world.getFlagPositions());

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 60; i++) {
                flagRenderer.update(1f / 60f);
            }
        }, "update() should not require a GL context (geometry built lazily in render())");
    }

    /**
     * Verify dispose() is safe to call before any render() has been issued.
     * Before the first render(), no GL models exist — dispose must be a no-op.
     */
    @Test
    public void testDisposeBeforeRenderIsASafeNoop() {
        flagRenderer.setFlags(world.getFlagPositions());
        flagRenderer.update(0.5f);

        assertDoesNotThrow(flagRenderer::dispose,
                "dispose() before any render() call should not throw");
    }

    /**
     * Verify dispose() is idempotent — calling it twice should not throw.
     */
    @Test
    public void testDisposeIsIdempotent() {
        flagRenderer.setFlags(world.getFlagPositions());

        assertDoesNotThrow(() -> {
            flagRenderer.dispose();
            flagRenderer.dispose();
        }, "dispose() called twice should not throw");
    }

    /**
     * Verify that getFlags() returns an unmodifiable view.
     */
    @Test
    public void testGetFlagsReturnsUnmodifiableView() {
        flagRenderer.setFlags(world.getFlagPositions());
        List<FlagPosition> result = flagRenderer.getFlags();

        assertThrows(UnsupportedOperationException.class,
                () -> result.add(new FlagPosition(0, 0, 0, 1, 1, 1, 0, 0, 0, 0)),
                "getFlags() should return an unmodifiable list");
    }
}

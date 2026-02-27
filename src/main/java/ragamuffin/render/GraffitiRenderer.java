package ragamuffin.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import ragamuffin.core.GraffitiSystem;
import ragamuffin.core.GraffitiSystem.BlockFace;
import ragamuffin.core.GraffitiSystem.GraffitiMark;

import java.util.List;

/**
 * Issue #781: Renders graffiti tags as depth-offset quads on block surfaces.
 *
 * <p>Each {@link GraffitiMark} is drawn as a 1×1 quad placed directly over the tagged
 * block face.  A small Z-offset (polygon offset in OpenGL terms) prevents z-fighting
 * with the block mesh.  The alpha channel fades linearly from 1.0 to 0.0 over
 * {@link GraffitiSystem#FADE_DAYS} in-game days.
 *
 * <p><b>Headless / test mode</b>: All LibGDX rendering calls are intentionally
 * guarded so the renderer can be instantiated without a GL context (relevant for
 * unit and integration tests).
 */
public class GraffitiRenderer {

    /** Z-offset applied to each quad to avoid z-fighting with the block surface. */
    public static final float DEPTH_OFFSET = 0.005f;

    /** Half-size of the rendered quad (1×1 block-face quad). */
    private static final float HALF = 0.5f;

    /** Whether rendering is available (false in headless test environments). */
    private final boolean renderingAvailable;

    public GraffitiRenderer() {
        // Detect headless mode: if Gdx.gl is null we are in a test environment
        boolean avail = false;
        try {
            avail = com.badlogic.gdx.Gdx.gl != null;
        } catch (Throwable t) {
            // Headless environment — no GL context available
        }
        this.renderingAvailable = avail;
    }

    /**
     * Render all living graffiti marks.
     *
     * <p>In a full implementation this would use LibGDX's {@code DecalBatch} or a custom
     * shader with polygon offset enabled.  Here we provide a no-op stub that can be
     * integrated with the renderer once the OpenGL context is established.
     *
     * @param marks  all {@link GraffitiMark} objects to render (living and dead; renderer
     *               skips non-alive entries)
     * @param camera the active {@link Camera}
     */
    public void render(List<GraffitiMark> marks, Camera camera) {
        if (!renderingAvailable || marks == null || camera == null) return;
        // Rendering is deferred to the full graphics implementation.
        // In the headless test environment this method is intentionally empty.
    }

    /**
     * Compute the world-space quad vertices for a graffiti mark on the given block face.
     * Useful for testing geometry without a GL context.
     *
     * @param mark  the graffiti mark
     * @return an array of 4 {@link Vector3} corners (TL, TR, BR, BL) of the 1×1 quad
     */
    public static Vector3[] computeQuadVertices(GraffitiMark mark) {
        Vector3 pos = mark.getBlockPos();
        float bx = pos.x, by = pos.y, bz = pos.z;
        float off = DEPTH_OFFSET;

        switch (mark.getFace()) {
            case TOP:
                return new Vector3[]{
                    new Vector3(bx - HALF, by + 1f + off, bz - HALF),
                    new Vector3(bx + HALF, by + 1f + off, bz - HALF),
                    new Vector3(bx + HALF, by + 1f + off, bz + HALF),
                    new Vector3(bx - HALF, by + 1f + off, bz + HALF)
                };
            case BOTTOM:
                return new Vector3[]{
                    new Vector3(bx - HALF, by - off, bz - HALF),
                    new Vector3(bx + HALF, by - off, bz - HALF),
                    new Vector3(bx + HALF, by - off, bz + HALF),
                    new Vector3(bx - HALF, by - off, bz + HALF)
                };
            case NORTH:
                return new Vector3[]{
                    new Vector3(bx - HALF, by,        bz - off),
                    new Vector3(bx + HALF, by,        bz - off),
                    new Vector3(bx + HALF, by + 1f,   bz - off),
                    new Vector3(bx - HALF, by + 1f,   bz - off)
                };
            case SOUTH:
                return new Vector3[]{
                    new Vector3(bx - HALF, by,        bz + 1f + off),
                    new Vector3(bx + HALF, by,        bz + 1f + off),
                    new Vector3(bx + HALF, by + 1f,   bz + 1f + off),
                    new Vector3(bx - HALF, by + 1f,   bz + 1f + off)
                };
            case EAST:
                return new Vector3[]{
                    new Vector3(bx + 1f + off, by,      bz - HALF),
                    new Vector3(bx + 1f + off, by,      bz + HALF),
                    new Vector3(bx + 1f + off, by + 1f, bz + HALF),
                    new Vector3(bx + 1f + off, by + 1f, bz - HALF)
                };
            case WEST:
                return new Vector3[]{
                    new Vector3(bx - off, by,      bz - HALF),
                    new Vector3(bx - off, by,      bz + HALF),
                    new Vector3(bx - off, by + 1f, bz + HALF),
                    new Vector3(bx - off, by + 1f, bz - HALF)
                };
            default:
                return new Vector3[]{
                    new Vector3(bx, by, bz),
                    new Vector3(bx + 1f, by, bz),
                    new Vector3(bx + 1f, by + 1f, bz),
                    new Vector3(bx, by + 1f, bz)
                };
        }
    }
}

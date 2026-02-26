package ragamuffin.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ragamuffin.world.FlagPosition;

/**
 * Renders animated flags in the 3D world as physical 3D objects.
 *
 * Issue #676: Flags are now physical 3D objects rather than 2D screen-space sprites.
 *
 * Each flag is a flat rectangular 3D mesh attached to a thin pole. The flag panel
 * is represented as a series of horizontal strips whose vertices are displaced in
 * the Z axis by a sine wave to simulate fabric waving in the wind. Both front and
 * back faces are rendered so the flag is visible from either side.
 *
 * Rendering uses ModelBatch inside the main 3D pass (same as PropRenderer and
 * SmallItemRenderer), so flags properly occlude and are occluded by world geometry
 * and benefit from the same scene lighting.
 *
 * GL model construction is deferred to the first render() call so that the class
 * can be instantiated and configured in headless/test contexts without a GPU.
 */
public class FlagRenderer {

    private static final long ATTRS =
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

    /** Maximum render distance for flags (blocks). */
    private static final float MAX_RENDER_DIST_SQ = 60f * 60f;

    /** Flag dimensions in world units (blocks). */
    private static final float FLAG_WIDTH  = 1.2f;
    private static final float FLAG_HEIGHT = 0.75f;

    /** Number of horizontal strips used to approximate the wave shape. */
    private static final int WAVE_SEGMENTS = 6;

    /** Wave amplitude as a fraction of flag width, applied in the Z axis. */
    private static final float WAVE_AMPLITUDE = 0.08f;
    /** Wave frequency (full cycles per flag width). */
    private static final float WAVE_FREQUENCY = 1.5f;
    /** Wave travel speed (cycles per second). */
    private static final float WAVE_SPEED = 1.2f;

    private float time = 0f;

    private final List<FlagPosition> flags = new ArrayList<>();

    /**
     * One ModelInstance per flag — rebuilt each frame for animation.
     * {@code null} until the first render() call (lazy GL initialisation).
     */
    private List<ModelInstance> instances = null;

    private final ModelBuilder mb = new ModelBuilder();
    private final Vector3 tmpVec = new Vector3();

    /**
     * Mark instances as stale whenever time or flag list changes.
     * Actual GL rebuild is deferred to render().
     */
    private boolean dirty = false;

    /** Register all flag positions. Call once after world generation. */
    public void setFlags(List<FlagPosition> flags) {
        this.flags.clear();
        this.flags.addAll(flags);
        dirty = true;
    }

    /** Get the current list of flag positions (for testing). */
    public List<FlagPosition> getFlags() {
        return Collections.unmodifiableList(flags);
    }

    /**
     * Advance the flag animation timer. Marks geometry as dirty for rebuild
     * on the next render() call.
     *
     * @param delta elapsed time in seconds since the last frame
     */
    public void update(float delta) {
        time += delta;
        dirty = true;
    }

    /**
     * Render all visible flags inside the main 3D model batch.
     *
     * Must be called inside a modelBatch.begin()/end() block.
     * GL models are built lazily on the first call (or after any dirty update).
     *
     * @param modelBatch  the 3D model batch
     * @param environment the environment (lighting)
     */
    public void render(ModelBatch modelBatch, Environment environment) {
        if (dirty || instances == null) {
            rebuildAllInstances();
            dirty = false;
        }

        Vector3 camPos = modelBatch.getCamera() != null
                ? modelBatch.getCamera().position : null;

        for (int i = 0; i < instances.size(); i++) {
            FlagPosition flag = flags.get(i);

            if (camPos != null) {
                tmpVec.set(flag.getWorldX(), flag.getWorldY(), flag.getWorldZ());
                if (camPos.dst2(tmpVec) > MAX_RENDER_DIST_SQ) continue;
            }

            modelBatch.render(instances.get(i), environment);
        }
    }

    /** Dispose all models (call on game shutdown). */
    public void dispose() {
        if (instances != null) {
            for (ModelInstance inst : instances) {
                inst.model.dispose();
            }
            instances.clear();
            instances = null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Rebuild 3D model instances for all flags based on the current animation time.
     * Requires an active GL context — must only be called from render().
     */
    private void rebuildAllInstances() {
        // Dispose previous models
        if (instances != null) {
            for (ModelInstance inst : instances) {
                inst.model.dispose();
            }
            instances.clear();
        } else {
            instances = new ArrayList<>();
        }

        for (FlagPosition flag : flags) {
            Model model = buildFlagModel(flag);
            ModelInstance instance = new ModelInstance(model);
            // Position the flag at the top of the pole in world space
            instance.transform.setToTranslation(
                    flag.getWorldX(), flag.getWorldY(), flag.getWorldZ());
            instances.add(instance);
        }
    }

    /**
     * Build a 3D model for a single flag at the current animation time.
     *
     * The model is centred on the origin; the caller translates it to world space.
     * The model consists of:
     * <ul>
     *   <li>A thin vertical pole stub (dark grey box below the attachment point)</li>
     *   <li>A waving flag panel made of {@code WAVE_SEGMENTS} horizontal strips,
     *       each strip a quad whose vertices are displaced in the Z axis by a sine
     *       wave to simulate fabric movement.</li>
     * </ul>
     */
    private Model buildFlagModel(FlagPosition flag) {
        mb.begin();

        // ── Pole stub ─────────────────────────────────────────────────────────
        Color poleColor = new Color(0.55f, 0.55f, 0.60f, 1f);
        Material poleMat = new Material(ColorAttribute.createDiffuse(poleColor));
        MeshPartBuilder pole = mb.part("pole", GL20.GL_TRIANGLES, ATTRS, poleMat);
        pole.setVertexTransform(new Matrix4().setToTranslation(0f, -0.25f, 0f));
        pole.box(0.05f, 0.5f, 0.05f);

        // ── Waving flag panel ─────────────────────────────────────────────────
        float r1 = flag.getColorR1(), g1 = flag.getColorG1(), b1 = flag.getColorB1();
        float r2 = flag.getColorR2(), g2 = flag.getColorG2(), b2 = flag.getColorB2();
        float phase = flag.getPhaseOffset();

        for (int seg = 0; seg < WAVE_SEGMENTS; seg++) {
            float tLeft  = (float) seg      / WAVE_SEGMENTS;
            float tRight = (float)(seg + 1) / WAVE_SEGMENTS;

            // X coordinates: flag extends along +X from the pole
            float x0 = tLeft  * FLAG_WIDTH;
            float x1 = tRight * FLAG_WIDTH;

            // Z wave offsets (displacement toward/away from camera)
            float zLeft  = waveZ(tLeft,  phase);
            float zRight = waveZ(tRight, phase);

            // Blend colour horizontally across the flag
            float t = (tLeft + tRight) * 0.5f;
            float cr = r1 + (r2 - r1) * t;
            float cg = g1 + (g2 - g1) * t;
            float cb = b1 + (b2 - b1) * t;

            Color stripColor = new Color(cr, cg, cb, 1f);
            Material stripMat = new Material(ColorAttribute.createDiffuse(stripColor));

            // Four corners of this strip (flag hangs downward from Y=0)
            float yTop = 0f;
            float yBot = -FLAG_HEIGHT;

            MeshPartBuilder strip = mb.part("strip" + seg, GL20.GL_TRIANGLES, ATTRS, stripMat);

            // Front face (normal pointing -Z)
            MeshPartBuilder.VertexInfo tlv = new MeshPartBuilder.VertexInfo()
                    .setPos(x0, yTop, zLeft).setNor(0, 0, -1);
            MeshPartBuilder.VertexInfo trv = new MeshPartBuilder.VertexInfo()
                    .setPos(x1, yTop, zRight).setNor(0, 0, -1);
            MeshPartBuilder.VertexInfo brv = new MeshPartBuilder.VertexInfo()
                    .setPos(x1, yBot, zRight).setNor(0, 0, -1);
            MeshPartBuilder.VertexInfo blv = new MeshPartBuilder.VertexInfo()
                    .setPos(x0, yBot, zLeft).setNor(0, 0, -1);

            strip.triangle(tlv, trv, brv);
            strip.triangle(tlv, brv, blv);

            // Back face (reversed winding, normal pointing +Z)
            MeshPartBuilder.VertexInfo tlvB = new MeshPartBuilder.VertexInfo()
                    .setPos(x0, yTop, zLeft).setNor(0, 0, 1);
            MeshPartBuilder.VertexInfo trvB = new MeshPartBuilder.VertexInfo()
                    .setPos(x1, yTop, zRight).setNor(0, 0, 1);
            MeshPartBuilder.VertexInfo brvB = new MeshPartBuilder.VertexInfo()
                    .setPos(x1, yBot, zRight).setNor(0, 0, 1);
            MeshPartBuilder.VertexInfo blvB = new MeshPartBuilder.VertexInfo()
                    .setPos(x0, yBot, zLeft).setNor(0, 0, 1);

            strip.triangle(brvB, trvB, tlvB);
            strip.triangle(blvB, brvB, tlvB);
        }

        return mb.end();
    }

    /**
     * Compute the Z-axis wave displacement for a point at normalised horizontal
     * position {@code t} (0 = hoist/pole, 1 = free end).
     *
     * The wave is anchored at t=0 and grows in amplitude toward the free end,
     * giving the correct physics feel of fabric streaming from a fixed point.
     */
    private float waveZ(float t, float phaseOffset) {
        float angle = t * WAVE_FREQUENCY * MathUtils.PI2
                      - time * WAVE_SPEED * MathUtils.PI2
                      + phaseOffset;
        return MathUtils.sin(angle) * WAVE_AMPLITUDE * t;
    }
}

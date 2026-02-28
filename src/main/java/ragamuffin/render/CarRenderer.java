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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import ragamuffin.entity.Car;
import ragamuffin.entity.Car.CarColour;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Renders car entities in the 3D world.
 *
 * Each car colour has a shared {@link Model} built from geometric primitives.
 * Per-car {@link ModelInstance} objects hold the individual world-space transforms.
 * Cars are rendered inside the main 3D ModelBatch pass, benefiting from the same
 * environment lighting as chunk geometry.
 *
 * Issue #672: Cars are invisible in-game — this renderer makes them visible.
 */
public class CarRenderer {

    private static final long ATTRS = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

    /** Render distance: don't render cars more than this many blocks away (squared). */
    private static final float MAX_RENDER_DIST_SQ = 80f * 80f;

    private final ModelBuilder mb;

    /** One shared Model per car colour. */
    private final Map<CarColour, Model> carModels;
    /** One shared Model per van colour. */
    private final Map<CarColour, Model> vanModels;

    /** Reusable transform matrix to avoid per-frame allocation. */
    private final Matrix4 tmpMatrix = new Matrix4();
    /** Reusable vector for distance culling. */
    private final Vector3 tmpVec = new Vector3();

    public CarRenderer() {
        mb = new ModelBuilder();
        carModels = new EnumMap<>(CarColour.class);
        vanModels = new EnumMap<>(CarColour.class);
        buildAllModels();
    }

    /**
     * Render all cars.
     *
     * Must be called inside a {@code modelBatch.begin()} / {@code modelBatch.end()} block.
     *
     * @param modelBatch  the active 3D model batch
     * @param environment the lighting environment
     * @param cars        the live car list from {@link ragamuffin.ai.CarManager}
     */
    public void render(ModelBatch modelBatch, Environment environment, List<Car> cars) {
        Vector3 camPos = modelBatch.getCamera() != null
                ? modelBatch.getCamera().position : null;

        for (Car car : cars) {
            Vector3 pos = car.getPosition();

            // Distance cull
            if (camPos != null) {
                tmpVec.set(pos.x, pos.y, pos.z);
                if (camPos.dst2(tmpVec) > MAX_RENDER_DIST_SQ) continue;
            }

            Model model = car.isVan()
                    ? vanModels.get(car.getColour())
                    : carModels.get(car.getColour());
            if (model == null) model = carModels.get(car.getColour());
            if (model == null) continue;

            ModelInstance instance = new ModelInstance(model);

            // Position: car.position is the bottom-centre of the car body.
            // Rotate the model around Y so the car's DEPTH axis aligns with its
            // heading.  The model is built along +Z; heading 0 = +Z needs no
            // rotation.  R_y(+90°) takes +Z → +X, matching heading 90 = +X.
            tmpMatrix.idt();
            tmpMatrix.setToTranslation(pos.x, pos.y, pos.z);
            // Car heading 0 = +Z (model forward), so rotation = +heading.
            float rotDeg = car.getHeading();
            tmpMatrix.rotate(Vector3.Y, rotDeg);
            instance.transform.set(tmpMatrix);

            modelBatch.render(instance, environment);
        }
    }

    /** Dispose all shared models. */
    public void dispose() {
        for (Model model : carModels.values()) {
            model.dispose();
        }
        carModels.clear();
        for (Model model : vanModels.values()) {
            model.dispose();
        }
        vanModels.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Model construction
    // ─────────────────────────────────────────────────────────────────────────

    private void buildAllModels() {
        carModels.put(CarColour.RED,    buildCarModel(new Color(0.80f, 0.10f, 0.10f, 1f)));
        carModels.put(CarColour.BLUE,   buildCarModel(new Color(0.15f, 0.25f, 0.75f, 1f)));
        carModels.put(CarColour.WHITE,  buildCarModel(new Color(0.92f, 0.92f, 0.92f, 1f)));
        carModels.put(CarColour.SILVER, buildCarModel(new Color(0.65f, 0.65f, 0.68f, 1f)));
        carModels.put(CarColour.BLACK,  buildCarModel(new Color(0.10f, 0.10f, 0.10f, 1f)));
        carModels.put(CarColour.YELLOW, buildCarModel(new Color(0.90f, 0.80f, 0.05f, 1f)));

        vanModels.put(CarColour.WHITE,  buildVanModel(new Color(0.92f, 0.92f, 0.92f, 1f)));
        vanModels.put(CarColour.BLUE,   buildVanModel(new Color(0.15f, 0.25f, 0.75f, 1f)));
        vanModels.put(CarColour.RED,    buildVanModel(new Color(0.80f, 0.10f, 0.10f, 1f)));
        vanModels.put(CarColour.SILVER, buildVanModel(new Color(0.65f, 0.65f, 0.68f, 1f)));
        vanModels.put(CarColour.BLACK,  buildVanModel(new Color(0.10f, 0.10f, 0.10f, 1f)));
        vanModels.put(CarColour.YELLOW, buildVanModel(new Color(0.90f, 0.80f, 0.05f, 1f)));
    }

    /**
     * Build a simple car model in the given body colour.
     *
     * The model is centred at the origin and sits above y=0 (bottom of body at y=0).
     * Car dimensions: WIDTH=1.6, HEIGHT=1.4, DEPTH=3.2.
     *
     * Layout (Z is the car's forward axis before rotation):
     *   - Lower body / chassis
     *   - Upper cabin
     *   - Four wheels
     *   - Windscreen and rear window (light grey)
     *   - Headlights (yellow-white) and tail lights (dark red)
     */
    private Model buildCarModel(Color bodyColour) {
        mb.begin();

        Material bodyMat    = new Material(ColorAttribute.createDiffuse(bodyColour));
        Material darkMat    = new Material(ColorAttribute.createDiffuse(new Color(0.12f, 0.12f, 0.12f, 1f)));
        Material glassMat   = new Material(ColorAttribute.createDiffuse(new Color(0.55f, 0.70f, 0.80f, 1f)));
        Material headMat    = new Material(ColorAttribute.createDiffuse(new Color(1.00f, 0.95f, 0.70f, 1f)));
        Material tailMat    = new Material(ColorAttribute.createDiffuse(new Color(0.60f, 0.05f, 0.05f, 1f)));

        // ── Lower body / chassis ─────────────────────────────────────────────
        // Full-width lower section — centred at y = 0.4 (bottom at 0, top at 0.8)
        MeshPartBuilder lower = mb.part("lower", GL20.GL_TRIANGLES, ATTRS, bodyMat);
        lower.setVertexTransform(new Matrix4().setToTranslation(0f, 0.40f, 0f));
        lower.box(Car.WIDTH, 0.80f, Car.DEPTH);

        // ── Cabin (upper body) ───────────────────────────────────────────────
        // Narrower and shorter cabin sits on top of the lower body
        // cabin: y centre = 1.0 (from 0.8 to 1.2 in Y), Z-length = ~60% of total
        MeshPartBuilder cabin = mb.part("cabin", GL20.GL_TRIANGLES, ATTRS, bodyMat);
        cabin.setVertexTransform(new Matrix4().setToTranslation(0f, 1.10f, 0f));
        cabin.box(Car.WIDTH * 0.85f, 0.60f, Car.DEPTH * 0.60f);

        // ── Windscreen (front glass) ─────────────────────────────────────────
        MeshPartBuilder windscreen = mb.part("windscreen", GL20.GL_TRIANGLES, ATTRS, glassMat);
        windscreen.setVertexTransform(new Matrix4().setToTranslation(0f, 1.05f, Car.DEPTH * 0.30f));
        windscreen.box(Car.WIDTH * 0.78f, 0.42f, 0.05f);

        // ── Rear window ──────────────────────────────────────────────────────
        MeshPartBuilder rearWindow = mb.part("rearWindow", GL20.GL_TRIANGLES, ATTRS, glassMat);
        rearWindow.setVertexTransform(new Matrix4().setToTranslation(0f, 1.05f, -Car.DEPTH * 0.30f));
        rearWindow.box(Car.WIDTH * 0.78f, 0.42f, 0.05f);

        // ── Wheels (4 corners) ───────────────────────────────────────────────
        float wheelR = 0.28f;   // radius (half-height & half-width of wheel box)
        float wheelW = 0.12f;   // wheel thickness
        float wheelY = wheelR;  // centre Y — wheel sits on the ground
        float wheelZ = Car.DEPTH * 0.35f;
        float wheelX = Car.WIDTH * 0.50f + wheelW * 0.5f;

        // Front-left
        MeshPartBuilder wfl = mb.part("wFL", GL20.GL_TRIANGLES, ATTRS, darkMat);
        wfl.setVertexTransform(new Matrix4().setToTranslation(-wheelX, wheelY, wheelZ));
        wfl.box(wheelW, wheelR * 2f, wheelR * 2f);

        // Front-right
        MeshPartBuilder wfr = mb.part("wFR", GL20.GL_TRIANGLES, ATTRS, darkMat);
        wfr.setVertexTransform(new Matrix4().setToTranslation(wheelX, wheelY, wheelZ));
        wfr.box(wheelW, wheelR * 2f, wheelR * 2f);

        // Rear-left
        MeshPartBuilder wrl = mb.part("wRL", GL20.GL_TRIANGLES, ATTRS, darkMat);
        wrl.setVertexTransform(new Matrix4().setToTranslation(-wheelX, wheelY, -wheelZ));
        wrl.box(wheelW, wheelR * 2f, wheelR * 2f);

        // Rear-right
        MeshPartBuilder wrr = mb.part("wRR", GL20.GL_TRIANGLES, ATTRS, darkMat);
        wrr.setVertexTransform(new Matrix4().setToTranslation(wheelX, wheelY, -wheelZ));
        wrr.box(wheelW, wheelR * 2f, wheelR * 2f);

        // ── Headlights (front, Z-positive) ───────────────────────────────────
        MeshPartBuilder headL = mb.part("headL", GL20.GL_TRIANGLES, ATTRS, headMat);
        headL.setVertexTransform(new Matrix4().setToTranslation(-0.38f, 0.52f, Car.DEPTH * 0.501f));
        headL.box(0.28f, 0.16f, 0.04f);

        MeshPartBuilder headR = mb.part("headR", GL20.GL_TRIANGLES, ATTRS, headMat);
        headR.setVertexTransform(new Matrix4().setToTranslation(0.38f, 0.52f, Car.DEPTH * 0.501f));
        headR.box(0.28f, 0.16f, 0.04f);

        // ── Tail lights (rear, Z-negative) ────────────────────────────────────
        MeshPartBuilder tailL = mb.part("tailL", GL20.GL_TRIANGLES, ATTRS, tailMat);
        tailL.setVertexTransform(new Matrix4().setToTranslation(-0.38f, 0.52f, -Car.DEPTH * 0.501f));
        tailL.box(0.28f, 0.16f, 0.04f);

        MeshPartBuilder tailR = mb.part("tailR", GL20.GL_TRIANGLES, ATTRS, tailMat);
        tailR.setVertexTransform(new Matrix4().setToTranslation(0.38f, 0.52f, -Car.DEPTH * 0.501f));
        tailR.box(0.28f, 0.16f, 0.04f);

        return mb.end();
    }

    /**
     * Build a van model — taller, longer than a car with a flat-topped cargo body
     * and a small cab at the front. Visually distinct from a car.
     *
     * Dimensions: VAN_WIDTH=2.0, VAN_HEIGHT=2.2, VAN_DEPTH=4.5.
     */
    private Model buildVanModel(Color bodyColour) {
        mb.begin();

        float W = Car.VAN_WIDTH;
        float H = Car.VAN_HEIGHT;
        float D = Car.VAN_DEPTH;

        Material bodyMat    = new Material(ColorAttribute.createDiffuse(bodyColour));
        Material darkMat    = new Material(ColorAttribute.createDiffuse(new Color(0.12f, 0.12f, 0.12f, 1f)));
        Material glassMat   = new Material(ColorAttribute.createDiffuse(new Color(0.55f, 0.70f, 0.80f, 1f)));
        Material headMat    = new Material(ColorAttribute.createDiffuse(new Color(1.00f, 0.95f, 0.70f, 1f)));
        Material tailMat    = new Material(ColorAttribute.createDiffuse(new Color(0.60f, 0.05f, 0.05f, 1f)));
        Material stripeMat  = new Material(ColorAttribute.createDiffuse(new Color(0.95f, 0.65f, 0.0f, 1f)));

        // ── Cargo body — full-height box from ground to roof ──────────────────
        MeshPartBuilder cargo = mb.part("cargo", GL20.GL_TRIANGLES, ATTRS, bodyMat);
        cargo.setVertexTransform(new Matrix4().setToTranslation(0f, H * 0.5f, -D * 0.1f));
        cargo.box(W, H, D * 0.7f);

        // ── Cab — shorter front section ───────────────────────────────────────
        float cabH = H * 0.7f;
        MeshPartBuilder cab = mb.part("cab", GL20.GL_TRIANGLES, ATTRS, bodyMat);
        cab.setVertexTransform(new Matrix4().setToTranslation(0f, cabH * 0.5f, D * 0.35f));
        cab.box(W, cabH, D * 0.3f);

        // ── Windscreen ────────────────────────────────────────────────────────
        MeshPartBuilder windscreen = mb.part("windscreen", GL20.GL_TRIANGLES, ATTRS, glassMat);
        windscreen.setVertexTransform(new Matrix4().setToTranslation(0f, cabH * 0.65f, D * 0.501f));
        windscreen.box(W * 0.8f, cabH * 0.45f, 0.05f);

        // ── Side stripe (council hi-vis band) ─────────────────────────────────
        MeshPartBuilder stripeL = mb.part("stripeL", GL20.GL_TRIANGLES, ATTRS, stripeMat);
        stripeL.setVertexTransform(new Matrix4().setToTranslation(-W * 0.501f, H * 0.35f, -D * 0.1f));
        stripeL.box(0.04f, 0.2f, D * 0.68f);

        MeshPartBuilder stripeR = mb.part("stripeR", GL20.GL_TRIANGLES, ATTRS, stripeMat);
        stripeR.setVertexTransform(new Matrix4().setToTranslation(W * 0.501f, H * 0.35f, -D * 0.1f));
        stripeR.box(0.04f, 0.2f, D * 0.68f);

        // ── Wheels (4 corners, larger than car) ───────────────────────────────
        float wheelR = 0.35f;
        float wheelW = 0.14f;
        float wheelY = wheelR;
        float wheelZ = D * 0.38f;
        float wheelX = W * 0.50f + wheelW * 0.5f;

        MeshPartBuilder wfl = mb.part("wFL", GL20.GL_TRIANGLES, ATTRS, darkMat);
        wfl.setVertexTransform(new Matrix4().setToTranslation(-wheelX, wheelY, wheelZ));
        wfl.box(wheelW, wheelR * 2f, wheelR * 2f);

        MeshPartBuilder wfr = mb.part("wFR", GL20.GL_TRIANGLES, ATTRS, darkMat);
        wfr.setVertexTransform(new Matrix4().setToTranslation(wheelX, wheelY, wheelZ));
        wfr.box(wheelW, wheelR * 2f, wheelR * 2f);

        MeshPartBuilder wrl = mb.part("wRL", GL20.GL_TRIANGLES, ATTRS, darkMat);
        wrl.setVertexTransform(new Matrix4().setToTranslation(-wheelX, wheelY, -wheelZ));
        wrl.box(wheelW, wheelR * 2f, wheelR * 2f);

        MeshPartBuilder wrr = mb.part("wRR", GL20.GL_TRIANGLES, ATTRS, darkMat);
        wrr.setVertexTransform(new Matrix4().setToTranslation(wheelX, wheelY, -wheelZ));
        wrr.box(wheelW, wheelR * 2f, wheelR * 2f);

        // ── Headlights ────────────────────────────────────────────────────────
        MeshPartBuilder headL = mb.part("headL", GL20.GL_TRIANGLES, ATTRS, headMat);
        headL.setVertexTransform(new Matrix4().setToTranslation(-0.5f, 0.55f, D * 0.501f));
        headL.box(0.32f, 0.18f, 0.04f);

        MeshPartBuilder headR = mb.part("headR", GL20.GL_TRIANGLES, ATTRS, headMat);
        headR.setVertexTransform(new Matrix4().setToTranslation(0.5f, 0.55f, D * 0.501f));
        headR.box(0.32f, 0.18f, 0.04f);

        // ── Tail lights ───────────────────────────────────────────────────────
        MeshPartBuilder tailL = mb.part("tailL", GL20.GL_TRIANGLES, ATTRS, tailMat);
        tailL.setVertexTransform(new Matrix4().setToTranslation(-0.5f, 0.55f, -D * 0.45f - 0.02f));
        tailL.box(0.32f, 0.18f, 0.04f);

        MeshPartBuilder tailR = mb.part("tailR", GL20.GL_TRIANGLES, ATTRS, tailMat);
        tailR.setVertexTransform(new Matrix4().setToTranslation(0.5f, 0.55f, -D * 0.45f - 0.02f));
        tailR.box(0.32f, 0.18f, 0.04f);

        return mb.end();
    }
}

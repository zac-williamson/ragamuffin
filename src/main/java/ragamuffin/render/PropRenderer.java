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
import ragamuffin.world.PropPosition;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Renders unique non-block-based 3D props in the world.
 *
 * Each prop type has a shared {@link Model} built from geometric primitives
 * (boxes, etc.) using {@link MeshPartBuilder}.  Individual {@link ModelInstance}
 * objects are created per prop placement so each can have its own world-space
 * transform (position + rotation).
 *
 * Props are rendered inside the main 3D ModelBatch pass, benefiting from the
 * same environment lighting as chunk geometry.
 *
 * Issue #669: Add unique non-block-based 3D models to the world.
 */
public class PropRenderer {

    private static final long ATTRS = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
    /** Render distance: don't bother rendering props more than this many blocks away. */
    private static final float MAX_RENDER_DIST_SQ = 80f * 80f;

    private final ModelBuilder mb;
    /** One shared Model per prop type. */
    private final Map<PropType, Model> propModels;
    /** One ModelInstance per placed prop. */
    private final List<ModelInstance> instances;
    private final List<PropPosition> propPositions;

    // Reusable transform to avoid GC pressure per frame
    private final Vector3 tmpVec = new Vector3();

    public PropRenderer() {
        mb = new ModelBuilder();
        propModels = new EnumMap<>(PropType.class);
        instances = new ArrayList<>();
        propPositions = new ArrayList<>();
    }

    /**
     * Register all prop positions and build the model instances.
     * Call once after world generation.
     *
     * @param props list of prop placements from the world generator
     */
    public void setProps(List<PropPosition> props) {
        // Dispose old instances (models are shared, disposed separately)
        instances.clear();
        propPositions.clear();
        propPositions.addAll(props);

        // Ensure all required models are built
        buildAllModels();

        // Create a ModelInstance per prop
        for (PropPosition prop : props) {
            Model model = propModels.get(prop.getType());
            if (model == null) continue;

            ModelInstance instance = new ModelInstance(model);
            instance.transform.setToTranslation(prop.getWorldX(), prop.getWorldY(), prop.getWorldZ());
            instance.transform.rotate(Vector3.Y, prop.getRotationY());
            instances.add(instance);
        }
    }

    /** Get the current list of prop positions (for testing). */
    public List<PropPosition> getProps() {
        return Collections.unmodifiableList(propPositions);
    }

    /**
     * Render all visible props.
     *
     * Must be called inside a modelBatch.begin()/end() block.
     *
     * @param modelBatch  the 3D model batch
     * @param environment the environment (lighting)
     */
    public void render(ModelBatch modelBatch, Environment environment) {
        Vector3 camPos = modelBatch.getCamera() != null
                ? modelBatch.getCamera().position : null;

        for (int i = 0; i < instances.size(); i++) {
            ModelInstance instance = instances.get(i);
            PropPosition prop = propPositions.get(i);

            // Distance cull — skip props far from the camera
            if (camPos != null) {
                tmpVec.set(prop.getWorldX(), prop.getWorldY(), prop.getWorldZ());
                float distSq = camPos.dst2(tmpVec);
                if (distSq > MAX_RENDER_DIST_SQ) continue;
            }

            modelBatch.render(instance, environment);
        }
    }

    /** Dispose all shared models. */
    public void dispose() {
        for (Model model : propModels.values()) {
            model.dispose();
        }
        propModels.clear();
        instances.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Model construction
    // ─────────────────────────────────────────────────────────────────────────

    private void buildAllModels() {
        if (!propModels.containsKey(PropType.PHONE_BOX))     propModels.put(PropType.PHONE_BOX,     buildPhoneBox());
        if (!propModels.containsKey(PropType.POST_BOX))      propModels.put(PropType.POST_BOX,      buildPostBox());
        if (!propModels.containsKey(PropType.PARK_BENCH))    propModels.put(PropType.PARK_BENCH,    buildParkBench());
        if (!propModels.containsKey(PropType.BUS_SHELTER))   propModels.put(PropType.BUS_SHELTER,   buildBusShelter());
        if (!propModels.containsKey(PropType.BOLLARD))       propModels.put(PropType.BOLLARD,       buildBollard());
        if (!propModels.containsKey(PropType.STREET_LAMP))   propModels.put(PropType.STREET_LAMP,   buildStreetLamp());
        if (!propModels.containsKey(PropType.LITTER_BIN))    propModels.put(PropType.LITTER_BIN,    buildLitterBin());
        if (!propModels.containsKey(PropType.MARKET_STALL))  propModels.put(PropType.MARKET_STALL,  buildMarketStall());
        if (!propModels.containsKey(PropType.PICNIC_TABLE))  propModels.put(PropType.PICNIC_TABLE,  buildPicnicTable());
        if (!propModels.containsKey(PropType.BIKE_RACK))     propModels.put(PropType.BIKE_RACK,     buildBikeRack());
        if (!propModels.containsKey(PropType.SHOPPING_TROLLEY)) propModels.put(PropType.SHOPPING_TROLLEY, buildShoppingTrolley());
        if (!propModels.containsKey(PropType.STATUE))        propModels.put(PropType.STATUE,        buildStatue());

        // ── Issue #721: Small 3D objects on shop shelves ──────────────────────
        if (!propModels.containsKey(PropType.SHELF_CAN))    propModels.put(PropType.SHELF_CAN,    buildShelfCan());
        if (!propModels.containsKey(PropType.SHELF_BOTTLE)) propModels.put(PropType.SHELF_BOTTLE, buildShelfBottle());
        if (!propModels.containsKey(PropType.SHELF_BOX))    propModels.put(PropType.SHELF_BOX,    buildShelfBox());
    }

    /**
     * Classic British red telephone box.
     * Composed of a base plinth, a rectangular red body, glass panels, and a
     * domed roof with a small ventilation crown.
     */
    private Model buildPhoneBox() {
        mb.begin();
        Color red = new Color(0.80f, 0.05f, 0.05f, 1f);
        Color glass = new Color(0.65f, 0.85f, 0.90f, 1f);
        Color crown = new Color(0.70f, 0.04f, 0.04f, 1f);

        Material redMat   = new Material(ColorAttribute.createDiffuse(red));
        Material glassMat = new Material(ColorAttribute.createDiffuse(glass));
        Material crownMat = new Material(ColorAttribute.createDiffuse(crown));

        // Stone plinth at base
        MeshPartBuilder plinth = mb.part("plinth", GL20.GL_TRIANGLES, ATTRS,
                new Material(ColorAttribute.createDiffuse(new Color(0.60f, 0.60f, 0.60f, 1f))));
        plinth.setVertexTransform(new Matrix4().setToTranslation(0f, 0.04f, 0f));
        plinth.box(0.98f, 0.08f, 0.98f);

        // Main red body
        MeshPartBuilder body = mb.part("body", GL20.GL_TRIANGLES, ATTRS, redMat);
        body.setVertexTransform(new Matrix4().setToTranslation(0f, 1.20f, 0f));
        body.box(0.90f, 2.20f, 0.90f);

        // Four glass panes (inset, slightly thinner)
        MeshPartBuilder g1 = mb.part("glass_front",  GL20.GL_TRIANGLES, ATTRS, glassMat);
        g1.setVertexTransform(new Matrix4().setToTranslation(0f, 1.10f, 0.44f));
        g1.box(0.70f, 1.60f, 0.04f);

        MeshPartBuilder g2 = mb.part("glass_back",   GL20.GL_TRIANGLES, ATTRS, glassMat);
        g2.setVertexTransform(new Matrix4().setToTranslation(0f, 1.10f, -0.44f));
        g2.box(0.70f, 1.60f, 0.04f);

        MeshPartBuilder g3 = mb.part("glass_left",   GL20.GL_TRIANGLES, ATTRS, glassMat);
        g3.setVertexTransform(new Matrix4().setToTranslation(-0.44f, 1.10f, 0f));
        g3.box(0.04f, 1.60f, 0.70f);

        MeshPartBuilder g4 = mb.part("glass_right",  GL20.GL_TRIANGLES, ATTRS, glassMat);
        g4.setVertexTransform(new Matrix4().setToTranslation(0.44f, 1.10f, 0f));
        g4.box(0.04f, 1.60f, 0.70f);

        // Domed roof
        MeshPartBuilder roof = mb.part("roof", GL20.GL_TRIANGLES, ATTRS, redMat);
        roof.setVertexTransform(new Matrix4().setToTranslation(0f, 2.42f, 0f));
        roof.box(0.94f, 0.16f, 0.94f);

        // Crown ventilation finial
        MeshPartBuilder fin = mb.part("crown", GL20.GL_TRIANGLES, ATTRS, crownMat);
        fin.setVertexTransform(new Matrix4().setToTranslation(0f, 2.58f, 0f));
        fin.box(0.28f, 0.12f, 0.28f);

        return mb.end();
    }

    /**
     * Classic Royal Mail post box — a short red cylinder approximated by stacked boxes.
     */
    private Model buildPostBox() {
        mb.begin();
        Color postRed = new Color(0.82f, 0.05f, 0.05f, 1f);
        Color dark    = new Color(0.12f, 0.12f, 0.12f, 1f);
        Material redMat  = new Material(ColorAttribute.createDiffuse(postRed));
        Material darkMat = new Material(ColorAttribute.createDiffuse(dark));

        // Cylindrical body approximation: 3 slightly tapered boxes
        MeshPartBuilder base = mb.part("base", GL20.GL_TRIANGLES, ATTRS, redMat);
        base.setVertexTransform(new Matrix4().setToTranslation(0f, 0.35f, 0f));
        base.box(0.50f, 0.70f, 0.50f);

        // Neck
        MeshPartBuilder neck = mb.part("neck", GL20.GL_TRIANGLES, ATTRS, redMat);
        neck.setVertexTransform(new Matrix4().setToTranslation(0f, 0.78f, 0f));
        neck.box(0.44f, 0.08f, 0.44f);

        // Cap (domed top)
        MeshPartBuilder cap = mb.part("cap", GL20.GL_TRIANGLES, ATTRS, redMat);
        cap.setVertexTransform(new Matrix4().setToTranslation(0f, 0.88f, 0f));
        cap.box(0.46f, 0.12f, 0.46f);

        // Slot (dark inset)
        MeshPartBuilder slot = mb.part("slot", GL20.GL_TRIANGLES, ATTRS, darkMat);
        slot.setVertexTransform(new Matrix4().setToTranslation(0f, 0.60f, 0.26f));
        slot.box(0.16f, 0.04f, 0.04f);

        // Royal cipher: small crown detail
        MeshPartBuilder cipher = mb.part("cipher", GL20.GL_TRIANGLES, ATTRS,
                new Material(ColorAttribute.createDiffuse(new Color(0.85f, 0.70f, 0.05f, 1f))));
        cipher.setVertexTransform(new Matrix4().setToTranslation(0f, 0.45f, 0.26f));
        cipher.box(0.10f, 0.10f, 0.02f);

        return mb.end();
    }

    /**
     * A wooden park bench with two legs on each side and a slatted seat + back.
     */
    private Model buildParkBench() {
        mb.begin();
        Color wood  = new Color(0.55f, 0.35f, 0.18f, 1f);
        Color metal = new Color(0.30f, 0.30f, 0.35f, 1f);
        Material woodMat  = new Material(ColorAttribute.createDiffuse(wood));
        Material metalMat = new Material(ColorAttribute.createDiffuse(metal));

        // Seat slats (3 horizontal boards)
        for (int i = 0; i < 3; i++) {
            MeshPartBuilder slat = mb.part("slat" + i, GL20.GL_TRIANGLES, ATTRS, woodMat);
            slat.setVertexTransform(new Matrix4().setToTranslation((i - 1) * 0.10f, 0.52f, 0f));
            slat.box(0.08f, 0.04f, 1.40f);
        }

        // Backrest slats
        for (int i = 0; i < 3; i++) {
            MeshPartBuilder slat = mb.part("back" + i, GL20.GL_TRIANGLES, ATTRS, woodMat);
            slat.setVertexTransform(new Matrix4().setToTranslation((i - 1) * 0.10f, 0.90f, -0.28f));
            slat.box(0.08f, 0.04f, 1.40f);
        }

        // Left rear leg support
        MeshPartBuilder lr = mb.part("leftRear", GL20.GL_TRIANGLES, ATTRS, metalMat);
        lr.setVertexTransform(new Matrix4().setToTranslation(-0.58f, 0.46f, -0.24f));
        lr.box(0.06f, 0.92f, 0.06f);

        // Right rear leg support
        MeshPartBuilder rr = mb.part("rightRear", GL20.GL_TRIANGLES, ATTRS, metalMat);
        rr.setVertexTransform(new Matrix4().setToTranslation(0.58f, 0.46f, -0.24f));
        rr.box(0.06f, 0.92f, 0.06f);

        // Left front leg support
        MeshPartBuilder lf = mb.part("leftFront", GL20.GL_TRIANGLES, ATTRS, metalMat);
        lf.setVertexTransform(new Matrix4().setToTranslation(-0.58f, 0.30f, 0.24f));
        lf.box(0.06f, 0.60f, 0.06f);

        // Right front leg support
        MeshPartBuilder rf = mb.part("rightFront", GL20.GL_TRIANGLES, ATTRS, metalMat);
        rf.setVertexTransform(new Matrix4().setToTranslation(0.58f, 0.30f, 0.24f));
        rf.box(0.06f, 0.60f, 0.06f);

        return mb.end();
    }

    /**
     * A covered bus shelter: a rectangular metal frame with a back wall and roof.
     */
    private Model buildBusShelter() {
        mb.begin();
        Color frame  = new Color(0.35f, 0.35f, 0.40f, 1f);
        Color glass  = new Color(0.70f, 0.85f, 0.90f, 1f);
        Color roof   = new Color(0.25f, 0.25f, 0.30f, 1f);
        Material frameMat = new Material(ColorAttribute.createDiffuse(frame));
        Material glassMat = new Material(ColorAttribute.createDiffuse(glass));
        Material roofMat  = new Material(ColorAttribute.createDiffuse(roof));

        // Back wall — glass panel
        MeshPartBuilder back = mb.part("back", GL20.GL_TRIANGLES, ATTRS, glassMat);
        back.setVertexTransform(new Matrix4().setToTranslation(0f, 1.10f, -0.90f));
        back.box(2.40f, 2.00f, 0.06f);

        // Left side wall
        MeshPartBuilder left = mb.part("left", GL20.GL_TRIANGLES, ATTRS, glassMat);
        left.setVertexTransform(new Matrix4().setToTranslation(-1.20f, 1.10f, -0.40f));
        left.box(0.06f, 2.00f, 1.00f);

        // Right side wall
        MeshPartBuilder right = mb.part("right", GL20.GL_TRIANGLES, ATTRS, glassMat);
        right.setVertexTransform(new Matrix4().setToTranslation(1.20f, 1.10f, -0.40f));
        right.box(0.06f, 2.00f, 1.00f);

        // Front frame top rail
        MeshPartBuilder topRail = mb.part("topRail", GL20.GL_TRIANGLES, ATTRS, frameMat);
        topRail.setVertexTransform(new Matrix4().setToTranslation(0f, 2.12f, 0.10f));
        topRail.box(2.50f, 0.08f, 0.10f);

        // Roof
        MeshPartBuilder roofPart = mb.part("roof", GL20.GL_TRIANGLES, ATTRS, roofMat);
        roofPart.setVertexTransform(new Matrix4().setToTranslation(0f, 2.25f, -0.40f));
        roofPart.box(2.60f, 0.10f, 1.20f);

        // Bench inside
        MeshPartBuilder bench = mb.part("bench", GL20.GL_TRIANGLES, ATTRS, frameMat);
        bench.setVertexTransform(new Matrix4().setToTranslation(0f, 0.45f, -0.70f));
        bench.box(2.20f, 0.06f, 0.30f);

        return mb.end();
    }

    /**
     * A short concrete traffic bollard.
     */
    private Model buildBollard() {
        mb.begin();
        Color concrete = new Color(0.60f, 0.60f, 0.60f, 1f);
        Color stripe   = new Color(0.92f, 0.80f, 0.10f, 1f);
        Material concMat   = new Material(ColorAttribute.createDiffuse(concrete));
        Material stripeMat = new Material(ColorAttribute.createDiffuse(stripe));

        // Main body
        MeshPartBuilder body = mb.part("body", GL20.GL_TRIANGLES, ATTRS, concMat);
        body.setVertexTransform(new Matrix4().setToTranslation(0f, 0.45f, 0f));
        body.box(0.25f, 0.90f, 0.25f);

        // Reflective top band
        MeshPartBuilder band = mb.part("band", GL20.GL_TRIANGLES, ATTRS, stripeMat);
        band.setVertexTransform(new Matrix4().setToTranslation(0f, 0.87f, 0f));
        band.box(0.28f, 0.06f, 0.28f);

        // Rounded cap
        MeshPartBuilder cap = mb.part("cap", GL20.GL_TRIANGLES, ATTRS, concMat);
        cap.setVertexTransform(new Matrix4().setToTranslation(0f, 0.97f, 0f));
        cap.box(0.20f, 0.08f, 0.20f);

        return mb.end();
    }

    /**
     * A street lamp with a vertical post and a curving arm holding a light box.
     */
    private Model buildStreetLamp() {
        mb.begin();
        Color post  = new Color(0.28f, 0.28f, 0.32f, 1f);
        Color light = new Color(0.98f, 0.94f, 0.70f, 1f);
        Color arm   = new Color(0.25f, 0.25f, 0.30f, 1f);
        Material postMat  = new Material(ColorAttribute.createDiffuse(post));
        Material lightMat = new Material(ColorAttribute.createDiffuse(light));
        Material armMat   = new Material(ColorAttribute.createDiffuse(arm));

        // Base plinth
        MeshPartBuilder base = mb.part("base", GL20.GL_TRIANGLES, ATTRS, postMat);
        base.setVertexTransform(new Matrix4().setToTranslation(0f, 0.06f, 0f));
        base.box(0.28f, 0.12f, 0.28f);

        // Vertical post
        MeshPartBuilder pole = mb.part("pole", GL20.GL_TRIANGLES, ATTRS, postMat);
        pole.setVertexTransform(new Matrix4().setToTranslation(0f, 2.10f, 0f));
        pole.box(0.10f, 4.00f, 0.10f);

        // Arm (horizontal reach at top)
        MeshPartBuilder armPart = mb.part("arm", GL20.GL_TRIANGLES, ATTRS, armMat);
        armPart.setVertexTransform(new Matrix4().setToTranslation(0.30f, 4.14f, 0f));
        armPart.box(0.60f, 0.08f, 0.08f);

        // Light housing
        MeshPartBuilder housing = mb.part("housing", GL20.GL_TRIANGLES, ATTRS, armMat);
        housing.setVertexTransform(new Matrix4().setToTranslation(0.60f, 4.02f, 0f));
        housing.box(0.24f, 0.14f, 0.18f);

        // Light globe
        MeshPartBuilder globe = mb.part("globe", GL20.GL_TRIANGLES, ATTRS, lightMat);
        globe.setVertexTransform(new Matrix4().setToTranslation(0.60f, 3.92f, 0f));
        globe.box(0.18f, 0.10f, 0.14f);

        return mb.end();
    }

    /**
     * A municipal litter bin — a rectangular dark metal bin with a lid.
     */
    private Model buildLitterBin() {
        mb.begin();
        Color bin = new Color(0.20f, 0.20f, 0.22f, 1f);
        Color lid = new Color(0.30f, 0.30f, 0.35f, 1f);
        Material binMat = new Material(ColorAttribute.createDiffuse(bin));
        Material lidMat = new Material(ColorAttribute.createDiffuse(lid));

        // Post
        MeshPartBuilder post = mb.part("post", GL20.GL_TRIANGLES, ATTRS, binMat);
        post.setVertexTransform(new Matrix4().setToTranslation(0f, 0.35f, 0f));
        post.box(0.10f, 0.70f, 0.10f);

        // Bin body
        MeshPartBuilder body = mb.part("body", GL20.GL_TRIANGLES, ATTRS, binMat);
        body.setVertexTransform(new Matrix4().setToTranslation(0f, 0.90f, 0f));
        body.box(0.42f, 0.60f, 0.42f);

        // Lid
        MeshPartBuilder lidPart = mb.part("lid", GL20.GL_TRIANGLES, ATTRS, lidMat);
        lidPart.setVertexTransform(new Matrix4().setToTranslation(0f, 1.24f, 0f));
        lidPart.box(0.46f, 0.08f, 0.46f);

        // Opening slot
        MeshPartBuilder slot = mb.part("slot", GL20.GL_TRIANGLES, ATTRS,
                new Material(ColorAttribute.createDiffuse(new Color(0.05f, 0.05f, 0.05f, 1f))));
        slot.setVertexTransform(new Matrix4().setToTranslation(0f, 1.15f, 0.22f));
        slot.box(0.20f, 0.06f, 0.04f);

        return mb.end();
    }

    /**
     * A market stall with a striped canvas awning and a simple trestle table.
     */
    private Model buildMarketStall() {
        mb.begin();
        Color awning1 = new Color(0.85f, 0.10f, 0.10f, 1f);   // red stripe
        Color awning2 = new Color(0.95f, 0.95f, 0.85f, 1f);   // cream stripe
        Color wood    = new Color(0.50f, 0.32f, 0.15f, 1f);
        Material awn1Mat  = new Material(ColorAttribute.createDiffuse(awning1));
        Material awn2Mat  = new Material(ColorAttribute.createDiffuse(awning2));
        Material woodMat  = new Material(ColorAttribute.createDiffuse(wood));

        // Four corner posts
        for (int px = -1; px <= 1; px += 2) {
            for (int pz = -1; pz <= 1; pz += 2) {
                MeshPartBuilder post = mb.part("post_" + px + "_" + pz, GL20.GL_TRIANGLES, ATTRS, woodMat);
                post.setVertexTransform(new Matrix4().setToTranslation(px * 0.80f, 1.0f, pz * 0.55f));
                post.box(0.06f, 2.00f, 0.06f);
            }
        }

        // Trestle table top
        MeshPartBuilder table = mb.part("table", GL20.GL_TRIANGLES, ATTRS, woodMat);
        table.setVertexTransform(new Matrix4().setToTranslation(0f, 1.10f, 0f));
        table.box(1.60f, 0.06f, 1.10f);

        // Awning panels — alternating stripes
        for (int stripe = 0; stripe < 6; stripe++) {
            Material mat = (stripe % 2 == 0) ? awn1Mat : awn2Mat;
            MeshPartBuilder panel = mb.part("awn" + stripe, GL20.GL_TRIANGLES, ATTRS, mat);
            float stripeX = (stripe - 2.5f) * 0.28f;
            panel.setVertexTransform(new Matrix4().setToTranslation(stripeX, 2.16f, 0f));
            panel.box(0.26f, 0.06f, 1.30f);
        }

        return mb.end();
    }

    /**
     * A park picnic table: two bench seats flanking a central table top, all on a shared frame.
     */
    private Model buildPicnicTable() {
        mb.begin();
        Color wood = new Color(0.52f, 0.34f, 0.16f, 1f);
        Material woodMat = new Material(ColorAttribute.createDiffuse(wood));

        // Table top
        MeshPartBuilder top = mb.part("top", GL20.GL_TRIANGLES, ATTRS, woodMat);
        top.setVertexTransform(new Matrix4().setToTranslation(0f, 0.78f, 0f));
        top.box(1.60f, 0.06f, 0.80f);

        // Left bench
        MeshPartBuilder lb = mb.part("leftBench", GL20.GL_TRIANGLES, ATTRS, woodMat);
        lb.setVertexTransform(new Matrix4().setToTranslation(-0.70f, 0.48f, 0f));
        lb.box(1.50f, 0.05f, 0.30f);

        // Right bench
        MeshPartBuilder rb = mb.part("rightBench", GL20.GL_TRIANGLES, ATTRS, woodMat);
        rb.setVertexTransform(new Matrix4().setToTranslation(0.70f, 0.48f, 0f));
        rb.box(1.50f, 0.05f, 0.30f);

        // Left A-frame legs
        MeshPartBuilder ll = mb.part("leftLeg", GL20.GL_TRIANGLES, ATTRS, woodMat);
        ll.setVertexTransform(new Matrix4().setToTranslation(-0.55f, 0.38f, 0f));
        ll.box(0.06f, 0.76f, 1.30f);

        // Right A-frame legs
        MeshPartBuilder rl = mb.part("rightLeg", GL20.GL_TRIANGLES, ATTRS, woodMat);
        rl.setVertexTransform(new Matrix4().setToTranslation(0.55f, 0.38f, 0f));
        rl.box(0.06f, 0.76f, 1.30f);

        return mb.end();
    }

    /**
     * A simple metal bicycle rack (an inverted U-bar).
     */
    private Model buildBikeRack() {
        mb.begin();
        Color steel = new Color(0.55f, 0.55f, 0.60f, 1f);
        Material steelMat = new Material(ColorAttribute.createDiffuse(steel));

        // Left vertical
        MeshPartBuilder lv = mb.part("leftVert", GL20.GL_TRIANGLES, ATTRS, steelMat);
        lv.setVertexTransform(new Matrix4().setToTranslation(-0.40f, 0.45f, 0f));
        lv.box(0.06f, 0.90f, 0.06f);

        // Right vertical
        MeshPartBuilder rv = mb.part("rightVert", GL20.GL_TRIANGLES, ATTRS, steelMat);
        rv.setVertexTransform(new Matrix4().setToTranslation(0.40f, 0.45f, 0f));
        rv.box(0.06f, 0.90f, 0.06f);

        // Top horizontal bar
        MeshPartBuilder horiz = mb.part("horiz", GL20.GL_TRIANGLES, ATTRS, steelMat);
        horiz.setVertexTransform(new Matrix4().setToTranslation(0f, 0.90f, 0f));
        horiz.box(0.86f, 0.06f, 0.06f);

        return mb.end();
    }

    /**
     * An abandoned supermarket shopping trolley.
     */
    private Model buildShoppingTrolley() {
        mb.begin();
        Color chrome   = new Color(0.72f, 0.72f, 0.78f, 1f);
        Color handle   = new Color(0.20f, 0.20f, 0.22f, 1f);
        Material chrMat = new Material(ColorAttribute.createDiffuse(chrome));
        Material hanMat = new Material(ColorAttribute.createDiffuse(handle));

        // Basket body (wireframe approximated as thin panels)
        MeshPartBuilder front = mb.part("front", GL20.GL_TRIANGLES, ATTRS, chrMat);
        front.setVertexTransform(new Matrix4().setToTranslation(0f, 0.60f, 0.38f));
        front.box(0.80f, 0.60f, 0.04f);

        MeshPartBuilder back = mb.part("back", GL20.GL_TRIANGLES, ATTRS, chrMat);
        back.setVertexTransform(new Matrix4().setToTranslation(0f, 0.60f, -0.38f));
        back.box(0.80f, 0.60f, 0.04f);

        MeshPartBuilder left = mb.part("left", GL20.GL_TRIANGLES, ATTRS, chrMat);
        left.setVertexTransform(new Matrix4().setToTranslation(-0.40f, 0.60f, 0f));
        left.box(0.04f, 0.60f, 0.80f);

        MeshPartBuilder right = mb.part("right", GL20.GL_TRIANGLES, ATTRS, chrMat);
        right.setVertexTransform(new Matrix4().setToTranslation(0.40f, 0.60f, 0f));
        right.box(0.04f, 0.60f, 0.80f);

        MeshPartBuilder bottom = mb.part("bottom", GL20.GL_TRIANGLES, ATTRS, chrMat);
        bottom.setVertexTransform(new Matrix4().setToTranslation(0f, 0.32f, 0f));
        bottom.box(0.80f, 0.04f, 0.80f);

        // Handle
        MeshPartBuilder handlePart = mb.part("handle", GL20.GL_TRIANGLES, ATTRS, hanMat);
        handlePart.setVertexTransform(new Matrix4().setToTranslation(0f, 0.94f, 0.38f));
        handlePart.box(0.78f, 0.06f, 0.06f);

        // Legs / wheels (simple short posts)
        for (int lx = -1; lx <= 1; lx += 2) {
            for (int lz = -1; lz <= 1; lz += 2) {
                MeshPartBuilder leg = mb.part("leg" + lx + lz, GL20.GL_TRIANGLES, ATTRS, chrMat);
                leg.setVertexTransform(new Matrix4().setToTranslation(lx * 0.35f, 0.14f, lz * 0.35f));
                leg.box(0.06f, 0.28f, 0.06f);
            }
        }

        return mb.end();
    }

    /**
     * A simple park statue: a humanoid figure on a stone plinth.
     */
    private Model buildStatue() {
        mb.begin();
        Color stone  = new Color(0.72f, 0.72f, 0.68f, 1f);
        Color bronze = new Color(0.55f, 0.40f, 0.20f, 1f);
        Material stoneMat  = new Material(ColorAttribute.createDiffuse(stone));
        Material bronzeMat = new Material(ColorAttribute.createDiffuse(bronze));

        // Plinth steps (3 tiers)
        MeshPartBuilder tier1 = mb.part("tier1", GL20.GL_TRIANGLES, ATTRS, stoneMat);
        tier1.setVertexTransform(new Matrix4().setToTranslation(0f, 0.10f, 0f));
        tier1.box(1.20f, 0.20f, 1.20f);

        MeshPartBuilder tier2 = mb.part("tier2", GL20.GL_TRIANGLES, ATTRS, stoneMat);
        tier2.setVertexTransform(new Matrix4().setToTranslation(0f, 0.40f, 0f));
        tier2.box(0.90f, 0.40f, 0.90f);

        MeshPartBuilder pedestal = mb.part("pedestal", GL20.GL_TRIANGLES, ATTRS, stoneMat);
        pedestal.setVertexTransform(new Matrix4().setToTranslation(0f, 0.90f, 0f));
        pedestal.box(0.55f, 0.60f, 0.55f);

        // Figurine: legs
        MeshPartBuilder legs = mb.part("legs", GL20.GL_TRIANGLES, ATTRS, bronzeMat);
        legs.setVertexTransform(new Matrix4().setToTranslation(0f, 1.52f, 0f));
        legs.box(0.22f, 0.44f, 0.20f);

        // Figurine: torso
        MeshPartBuilder torso = mb.part("torso", GL20.GL_TRIANGLES, ATTRS, bronzeMat);
        torso.setVertexTransform(new Matrix4().setToTranslation(0f, 1.96f, 0f));
        torso.box(0.26f, 0.38f, 0.18f);

        // Figurine: head
        MeshPartBuilder head = mb.part("head", GL20.GL_TRIANGLES, ATTRS, bronzeMat);
        head.setVertexTransform(new Matrix4().setToTranslation(0f, 2.32f, 0f));
        head.box(0.20f, 0.22f, 0.20f);

        // Outstretched arm (pointing heroically)
        MeshPartBuilder arm = mb.part("arm", GL20.GL_TRIANGLES, ATTRS, bronzeMat);
        arm.setVertexTransform(new Matrix4().setToTranslation(0.34f, 2.02f, 0f));
        arm.box(0.38f, 0.10f, 0.10f);

        return mb.end();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Issue #721: Small 3D objects on shop shelves
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A small tin can sitting on a shop shelf — e.g. baked beans, soup, etc.
     * Rendered as a short cylinder approximated by stacked boxes.
     */
    private Model buildShelfCan() {
        mb.begin();
        Color body  = new Color(0.75f, 0.72f, 0.68f, 1f); // tin silver
        Color label = new Color(0.80f, 0.15f, 0.10f, 1f); // red label band
        Color top   = new Color(0.85f, 0.82f, 0.78f, 1f); // lighter lid
        Material bodyMat  = new Material(ColorAttribute.createDiffuse(body));
        Material labelMat = new Material(ColorAttribute.createDiffuse(label));
        Material topMat   = new Material(ColorAttribute.createDiffuse(top));

        // Main cylindrical body
        MeshPartBuilder canBody = mb.part("can_body", GL20.GL_TRIANGLES, ATTRS, bodyMat);
        canBody.setVertexTransform(new Matrix4().setToTranslation(0f, 0.08f, 0f));
        canBody.box(0.13f, 0.16f, 0.13f);

        // Coloured label band
        MeshPartBuilder labelPart = mb.part("can_label", GL20.GL_TRIANGLES, ATTRS, labelMat);
        labelPart.setVertexTransform(new Matrix4().setToTranslation(0f, 0.09f, 0f));
        labelPart.box(0.14f, 0.08f, 0.14f);

        // Lid
        MeshPartBuilder lid = mb.part("can_lid", GL20.GL_TRIANGLES, ATTRS, topMat);
        lid.setVertexTransform(new Matrix4().setToTranslation(0f, 0.17f, 0f));
        lid.box(0.12f, 0.02f, 0.12f);

        return mb.end();
    }

    /**
     * A small glass bottle on a shop shelf — e.g. wine, sauce, or a fizzy drink.
     * Rendered as a narrow tall box with a bottle-neck cap.
     */
    private Model buildShelfBottle() {
        mb.begin();
        Color glass = new Color(0.40f, 0.65f, 0.35f, 1f); // green glass
        Color cap   = new Color(0.15f, 0.14f, 0.12f, 1f); // dark metal cap
        Material glassMat = new Material(ColorAttribute.createDiffuse(glass));
        Material capMat   = new Material(ColorAttribute.createDiffuse(cap));

        // Bottle body
        MeshPartBuilder body = mb.part("bottle_body", GL20.GL_TRIANGLES, ATTRS, glassMat);
        body.setVertexTransform(new Matrix4().setToTranslation(0f, 0.10f, 0f));
        body.box(0.09f, 0.20f, 0.09f);

        // Neck
        MeshPartBuilder neck = mb.part("bottle_neck", GL20.GL_TRIANGLES, ATTRS, glassMat);
        neck.setVertexTransform(new Matrix4().setToTranslation(0f, 0.23f, 0f));
        neck.box(0.05f, 0.06f, 0.05f);

        // Cap
        MeshPartBuilder capPart = mb.part("bottle_cap", GL20.GL_TRIANGLES, ATTRS, capMat);
        capPart.setVertexTransform(new Matrix4().setToTranslation(0f, 0.27f, 0f));
        capPart.box(0.06f, 0.03f, 0.06f);

        return mb.end();
    }

    /**
     * A small cardboard box sitting on a shop shelf — generic merchandise packaging.
     * Rendered as a flat rectangular box with a slightly darker top.
     */
    private Model buildShelfBox() {
        mb.begin();
        Color cardboard = new Color(0.78f, 0.62f, 0.38f, 1f); // cardboard brown
        Color top       = new Color(0.68f, 0.54f, 0.30f, 1f); // slightly darker top
        Color print     = new Color(0.30f, 0.45f, 0.70f, 1f); // blue print/logo stripe
        Material cardMat  = new Material(ColorAttribute.createDiffuse(cardboard));
        Material topMat   = new Material(ColorAttribute.createDiffuse(top));
        Material printMat = new Material(ColorAttribute.createDiffuse(print));

        // Box body
        MeshPartBuilder body = mb.part("box_body", GL20.GL_TRIANGLES, ATTRS, cardMat);
        body.setVertexTransform(new Matrix4().setToTranslation(0f, 0.07f, 0f));
        body.box(0.17f, 0.14f, 0.13f);

        // Top flap (slightly darker)
        MeshPartBuilder topPart = mb.part("box_top", GL20.GL_TRIANGLES, ATTRS, topMat);
        topPart.setVertexTransform(new Matrix4().setToTranslation(0f, 0.145f, 0f));
        topPart.box(0.18f, 0.01f, 0.14f);

        // Front label stripe
        MeshPartBuilder label = mb.part("box_label", GL20.GL_TRIANGLES, ATTRS, printMat);
        label.setVertexTransform(new Matrix4().setToTranslation(0f, 0.07f, 0.065f));
        label.box(0.12f, 0.06f, 0.01f);

        return mb.end();
    }
}

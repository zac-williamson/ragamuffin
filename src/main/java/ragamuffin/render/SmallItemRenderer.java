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
import ragamuffin.building.SmallItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Renders small items placed in the world as 3D models.
 *
 * Small items (tins, bottles, newspapers, etc.) are placed on block surfaces
 * without grid snapping. Each item type has a shared {@link Model} built from
 * geometric primitives. Individual {@link ModelInstance} objects are created
 * per placed item to hold their unique world-space positions.
 *
 * Issue #675: Add support for small 3D objects (non-block items).
 */
public class SmallItemRenderer {

    private static final long ATTRS = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
    /** Render distance: don't render items more than this many blocks away. */
    private static final float MAX_RENDER_DIST_SQ = 32f * 32f;

    /** Shape categories used internally to drive model construction. */
    private enum ItemShape {
        CYLINDER,   // Cans, bottles, extinguisher
        BOX,        // Tin, computer, stapler, box-shaped items
        FLAT,       // Flat paper items (newspaper, textbook)
        FOOD,       // Flat irregular food shapes
        CARD        // Thin flat card-like items (scratch card, phone, DVD)
    }

    private final ModelBuilder mb;
    /** One shared Model per item shape. */
    private final Map<ItemShape, Model> shapeModels;

    // Per-placed-item data
    private final List<ModelInstance> instances;
    private final List<SmallItem> items;

    // Reusable transform/vector to avoid GC pressure per frame
    private final Vector3 tmpVec = new Vector3();

    public SmallItemRenderer() {
        mb = new ModelBuilder();
        shapeModels = new EnumMap<>(ItemShape.class);
        instances = new ArrayList<>();
        items = new ArrayList<>();
    }

    /**
     * Register the current list of small items and rebuild model instances.
     * Call whenever the world's small item list changes (item placed or removed).
     *
     * @param smallItems the current list of small items in the world
     */
    public void setItems(List<SmallItem> smallItems) {
        instances.clear();
        items.clear();
        items.addAll(smallItems);

        buildAllModels();

        for (SmallItem item : smallItems) {
            ItemShape shape = shapeFor(item.getMaterial());
            Model model = shapeModels.get(shape);
            if (model == null) continue;

            ModelInstance instance = new ModelInstance(model);
            Vector3 pos = item.getPosition();
            instance.transform.setToTranslation(pos.x, pos.y, pos.z);
            instances.add(instance);
        }
    }

    /**
     * Render all visible small items.
     * Must be called inside a modelBatch.begin()/end() block.
     *
     * @param modelBatch  the 3D model batch
     * @param environment the environment (lighting)
     */
    public void render(ModelBatch modelBatch, Environment environment) {
        Vector3 camPos = modelBatch.getCamera() != null
                ? modelBatch.getCamera().position : null;

        for (int i = 0; i < instances.size(); i++) {
            // Distance cull — skip items far from the camera
            if (camPos != null) {
                SmallItem item = items.get(i);
                Vector3 pos = item.getPosition();
                tmpVec.set(pos.x, pos.y, pos.z);
                if (camPos.dst2(tmpVec) > MAX_RENDER_DIST_SQ) continue;
            }
            modelBatch.render(instances.get(i), environment);
        }
    }

    /**
     * Get the number of item instances registered (for testing).
     */
    public int getInstanceCount() {
        return instances.size();
    }

    /** Dispose all shared models. */
    public void dispose() {
        for (Model model : shapeModels.values()) {
            model.dispose();
        }
        shapeModels.clear();
        instances.clear();
        items.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shape selection
    // ─────────────────────────────────────────────────────────────────────────

    private ItemShape shapeFor(ragamuffin.building.Material material) {
        switch (material) {
            // Cylindrical / bottle-shaped
            case ENERGY_DRINK:
            case PINT:
            case NAIL_POLISH:
            case PETROL_CAN:
            case FIRE_EXTINGUISHER:
                return ItemShape.CYLINDER;

            // Flat paper / book items
            case NEWSPAPER:
            case TEXTBOOK:
            case HYMN_BOOK:
            case PARACETAMOL:
            case ANTIDEPRESSANTS:
            case WASHING_POWDER:
                return ItemShape.FLAT;

            // Card / phone / disc
            case SCRATCH_CARD:
            case BROKEN_PHONE:
            case DODGY_DVD:
                return ItemShape.CARD;

            // Food items (flat ovals/plates)
            case SAUSAGE_ROLL:
            case STEAK_BAKE:
            case CHIPS:
            case KEBAB:
            case PERI_PERI_CHICKEN:
            case CRISPS:
                return ItemShape.FOOD;

            // Box / can shaped — default for remaining small items
            case TIN_OF_BEANS:
            case STAPLER:
            default:
                return ItemShape.BOX;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Model construction — one model per shape category
    // ─────────────────────────────────────────────────────────────────────────

    private void buildAllModels() {
        if (!shapeModels.containsKey(ItemShape.CYLINDER))
            shapeModels.put(ItemShape.CYLINDER, buildCylinder());
        if (!shapeModels.containsKey(ItemShape.BOX))
            shapeModels.put(ItemShape.BOX, buildBox());
        if (!shapeModels.containsKey(ItemShape.FLAT))
            shapeModels.put(ItemShape.FLAT, buildFlat());
        if (!shapeModels.containsKey(ItemShape.FOOD))
            shapeModels.put(ItemShape.FOOD, buildFood());
        if (!shapeModels.containsKey(ItemShape.CARD))
            shapeModels.put(ItemShape.CARD, buildCard());
    }

    /**
     * A tall thin cylinder approximated as a stack of tapered boxes —
     * used for cans, bottles, extinguishers.
     * The base of the model is at Y=0 so it sits on the block surface.
     */
    private Model buildCylinder() {
        mb.begin();
        Color body  = new Color(0.35f, 0.70f, 0.30f, 1f); // generic green can colour
        Color cap   = new Color(0.55f, 0.55f, 0.60f, 1f); // silver cap

        Material bodyMat = new Material(ColorAttribute.createDiffuse(body));
        Material capMat  = new Material(ColorAttribute.createDiffuse(cap));

        // Main cylindrical body (approximated as a box)
        MeshPartBuilder bodyPart = mb.part("body", GL20.GL_TRIANGLES, ATTRS, bodyMat);
        bodyPart.setVertexTransform(new Matrix4().setToTranslation(0f, 0.10f, 0f));
        bodyPart.box(0.14f, 0.20f, 0.14f);

        // Neck
        MeshPartBuilder neck = mb.part("neck", GL20.GL_TRIANGLES, ATTRS, bodyMat);
        neck.setVertexTransform(new Matrix4().setToTranslation(0f, 0.22f, 0f));
        neck.box(0.10f, 0.04f, 0.10f);

        // Cap
        MeshPartBuilder capPart = mb.part("cap", GL20.GL_TRIANGLES, ATTRS, capMat);
        capPart.setVertexTransform(new Matrix4().setToTranslation(0f, 0.26f, 0f));
        capPart.box(0.09f, 0.03f, 0.09f);

        return mb.end();
    }

    /**
     * A small box / tin — used for tin cans, staplers, boxy items.
     * The base sits at Y=0.
     */
    private Model buildBox() {
        mb.begin();
        Color col = new Color(0.82f, 0.18f, 0.18f, 1f); // red tin
        Material mat = new Material(ColorAttribute.createDiffuse(col));

        MeshPartBuilder body = mb.part("box", GL20.GL_TRIANGLES, ATTRS, mat);
        body.setVertexTransform(new Matrix4().setToTranslation(0f, 0.075f, 0f));
        body.box(0.20f, 0.15f, 0.16f);

        return mb.end();
    }

    /**
     * A thin flat rectangle — used for newspapers, textbooks, packets.
     * Lies flat on the surface.
     */
    private Model buildFlat() {
        mb.begin();
        Color col = new Color(0.88f, 0.84f, 0.72f, 1f); // newsprint
        Material mat = new Material(ColorAttribute.createDiffuse(col));

        MeshPartBuilder body = mb.part("paper", GL20.GL_TRIANGLES, ATTRS, mat);
        body.setVertexTransform(new Matrix4().setToTranslation(0f, 0.015f, 0f));
        body.box(0.28f, 0.03f, 0.20f);

        return mb.end();
    }

    /**
     * A flat oval / plate shape — used for food items sitting on a surface.
     */
    private Model buildFood() {
        mb.begin();
        Color col = new Color(0.80f, 0.55f, 0.20f, 1f); // golden pastry
        Material mat = new Material(ColorAttribute.createDiffuse(col));

        // Main food body
        MeshPartBuilder body = mb.part("food", GL20.GL_TRIANGLES, ATTRS, mat);
        body.setVertexTransform(new Matrix4().setToTranslation(0f, 0.025f, 0f));
        body.box(0.22f, 0.05f, 0.14f);

        // Slight crust ridge
        MeshPartBuilder crust = mb.part("crust", GL20.GL_TRIANGLES, ATTRS,
                new Material(ColorAttribute.createDiffuse(new Color(0.65f, 0.38f, 0.12f, 1f))));
        crust.setVertexTransform(new Matrix4().setToTranslation(0f, 0.054f, 0f));
        crust.box(0.20f, 0.01f, 0.12f);

        return mb.end();
    }

    /**
     * A very thin flat rectangle — used for scratch cards, phones, DVDs.
     */
    private Model buildCard() {
        mb.begin();
        Color col = new Color(0.88f, 0.82f, 0.15f, 1f); // gold card
        Material mat = new Material(ColorAttribute.createDiffuse(col));

        MeshPartBuilder body = mb.part("card", GL20.GL_TRIANGLES, ATTRS, mat);
        body.setVertexTransform(new Matrix4().setToTranslation(0f, 0.008f, 0f));
        body.box(0.18f, 0.016f, 0.10f);

        return mb.end();
    }
}

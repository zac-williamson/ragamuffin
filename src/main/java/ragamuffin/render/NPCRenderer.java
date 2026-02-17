package ragamuffin.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders NPCs as coloured box models with heads and simple faces.
 * Each NPC type has a distinct colour so they're easily identifiable.
 */
public class NPCRenderer {

    private final Map<NPCType, Model> npcModels;
    private final ModelBuilder modelBuilder;

    public NPCRenderer() {
        this.modelBuilder = new ModelBuilder();
        this.npcModels = new HashMap<>();
        buildNPCModels();
    }

    private void buildNPCModels() {
        long attributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        // PUBLIC - Blue civilians
        npcModels.put(NPCType.PUBLIC, buildHumanoidModel(attributes,
            new Color(0.3f, 0.3f, 0.8f, 1f),   // body
            new Color(0.85f, 0.7f, 0.6f, 1f),   // head (skin tone)
            new Color(0.1f, 0.1f, 0.1f, 1f)));  // eyes

        // DOG - Brown, shorter and longer
        npcModels.put(NPCType.DOG, buildDogModel(attributes,
            new Color(0.6f, 0.4f, 0.2f, 1f),    // body
            new Color(0.5f, 0.3f, 0.15f, 1f),   // head (darker brown)
            new Color(0.05f, 0.05f, 0.05f, 1f)));// eyes

        // YOUTH_GANG - Red hoodies
        npcModels.put(NPCType.YOUTH_GANG, buildHumanoidModel(attributes,
            new Color(0.8f, 0.2f, 0.2f, 1f),
            new Color(0.85f, 0.7f, 0.6f, 1f),
            new Color(0.1f, 0.1f, 0.1f, 1f)));

        // COUNCIL_MEMBER - Grey suits
        npcModels.put(NPCType.COUNCIL_MEMBER, buildHumanoidModel(attributes,
            new Color(0.5f, 0.5f, 0.5f, 1f),
            new Color(0.85f, 0.7f, 0.6f, 1f),
            new Color(0.1f, 0.1f, 0.1f, 1f)));

        // POLICE - Dark blue uniform
        npcModels.put(NPCType.POLICE, buildHumanoidModel(attributes,
            new Color(0.1f, 0.1f, 0.4f, 1f),
            new Color(0.85f, 0.7f, 0.6f, 1f),
            new Color(0.1f, 0.1f, 0.1f, 1f)));

        // COUNCIL_BUILDER - Orange hi-vis
        npcModels.put(NPCType.COUNCIL_BUILDER, buildHumanoidModel(attributes,
            new Color(0.9f, 0.6f, 0.1f, 1f),
            new Color(0.85f, 0.7f, 0.6f, 1f),
            new Color(0.1f, 0.1f, 0.1f, 1f)));
    }

    /**
     * Build a humanoid model: body box + head box + two eye dots.
     * The model origin is at the feet centre.
     */
    private Model buildHumanoidModel(long attributes, Color bodyColor, Color headColor, Color eyeColor) {
        float bodyW = NPC.WIDTH;    // 0.6
        float bodyD = NPC.DEPTH;    // 0.6
        float bodyH = 1.2f;        // body is lower 1.2 of 1.8 total
        float headSize = 0.4f;     // head is a 0.4 cube
        float headY = bodyH + headSize / 2f;  // head sits on top of body

        modelBuilder.begin();

        // Body part
        Material bodyMat = new Material(ColorAttribute.createDiffuse(bodyColor));
        MeshPartBuilder body = modelBuilder.part("body", GL20.GL_TRIANGLES, attributes, bodyMat);
        // box() creates around origin, so translate to centre of body
        body.setVertexTransform(new Matrix4().setToTranslation(0, bodyH / 2f, 0));
        body.box(bodyW, bodyH, bodyD);

        // Head part (skin colour)
        Material headMat = new Material(ColorAttribute.createDiffuse(headColor));
        MeshPartBuilder head = modelBuilder.part("head", GL20.GL_TRIANGLES, attributes, headMat);
        head.setVertexTransform(new Matrix4().setToTranslation(0, headY, 0));
        head.box(headSize, headSize, headSize);

        // Left eye
        Material eyeMat = new Material(ColorAttribute.createDiffuse(eyeColor));
        MeshPartBuilder leftEye = modelBuilder.part("leftEye", GL20.GL_TRIANGLES, attributes, eyeMat);
        leftEye.setVertexTransform(new Matrix4().setToTranslation(-0.08f, headY + 0.04f, -headSize / 2f - 0.01f));
        leftEye.box(0.06f, 0.06f, 0.02f);

        // Right eye
        MeshPartBuilder rightEye = modelBuilder.part("rightEye", GL20.GL_TRIANGLES, attributes, eyeMat);
        rightEye.setVertexTransform(new Matrix4().setToTranslation(0.08f, headY + 0.04f, -headSize / 2f - 0.01f));
        rightEye.box(0.06f, 0.06f, 0.02f);

        // Mouth (dark line below eyes)
        MeshPartBuilder mouth = modelBuilder.part("mouth", GL20.GL_TRIANGLES, attributes, eyeMat);
        mouth.setVertexTransform(new Matrix4().setToTranslation(0f, headY - 0.08f, -headSize / 2f - 0.01f));
        mouth.box(0.14f, 0.03f, 0.02f);

        return modelBuilder.end();
    }

    /**
     * Build a dog model: longer body + smaller head + snout + eyes.
     */
    private Model buildDogModel(long attributes, Color bodyColor, Color headColor, Color eyeColor) {
        float bodyW = 0.5f;
        float bodyH = 0.4f;
        float bodyD = 0.8f;
        float headSize = 0.3f;
        float headY = bodyH + headSize / 2f - 0.05f;

        modelBuilder.begin();

        // Body
        Material bodyMat = new Material(ColorAttribute.createDiffuse(bodyColor));
        MeshPartBuilder body = modelBuilder.part("body", GL20.GL_TRIANGLES, attributes, bodyMat);
        body.setVertexTransform(new Matrix4().setToTranslation(0, bodyH / 2f, 0));
        body.box(bodyW, bodyH, bodyD);

        // Head
        Material headMat = new Material(ColorAttribute.createDiffuse(headColor));
        MeshPartBuilder head = modelBuilder.part("head", GL20.GL_TRIANGLES, attributes, headMat);
        head.setVertexTransform(new Matrix4().setToTranslation(0, headY, -bodyD / 2f - headSize / 2f + 0.05f));
        head.box(headSize, headSize, headSize);

        // Snout (lighter nose area)
        Material snotMat = new Material(ColorAttribute.createDiffuse(new Color(0.3f, 0.2f, 0.1f, 1f)));
        MeshPartBuilder snout = modelBuilder.part("snout", GL20.GL_TRIANGLES, attributes, snotMat);
        snout.setVertexTransform(new Matrix4().setToTranslation(0, headY - 0.05f, -bodyD / 2f - headSize - 0.01f));
        snout.box(0.12f, 0.1f, 0.08f);

        // Eyes
        Material eyeMat = new Material(ColorAttribute.createDiffuse(eyeColor));
        MeshPartBuilder leftEye = modelBuilder.part("leftEye", GL20.GL_TRIANGLES, attributes, eyeMat);
        leftEye.setVertexTransform(new Matrix4().setToTranslation(-0.06f, headY + 0.05f, -bodyD / 2f - headSize / 2f - 0.01f));
        leftEye.box(0.04f, 0.04f, 0.02f);

        MeshPartBuilder rightEye = modelBuilder.part("rightEye", GL20.GL_TRIANGLES, attributes, eyeMat);
        rightEye.setVertexTransform(new Matrix4().setToTranslation(0.06f, headY + 0.05f, -bodyD / 2f - headSize / 2f - 0.01f));
        rightEye.box(0.04f, 0.04f, 0.02f);

        // Nose (dark dot on snout)
        MeshPartBuilder nose = modelBuilder.part("nose", GL20.GL_TRIANGLES, attributes, eyeMat);
        nose.setVertexTransform(new Matrix4().setToTranslation(0f, headY - 0.02f, -bodyD / 2f - headSize - 0.05f));
        nose.box(0.05f, 0.04f, 0.02f);

        return modelBuilder.end();
    }

    /**
     * Render all NPCs using the model batch.
     */
    public void render(ModelBatch modelBatch, Environment environment, List<NPC> npcs) {
        for (NPC npc : npcs) {
            Model model = npcModels.get(npc.getType());
            if (model == null) continue;

            ModelInstance instance = new ModelInstance(model);
            Vector3 pos = npc.getPosition();
            // Model origin is at feet, so just translate to NPC position
            instance.transform.setToTranslation(pos.x, pos.y, pos.z);
            modelBatch.render(instance, environment);
        }
    }

    public void dispose() {
        for (Model model : npcModels.values()) {
            model.dispose();
        }
        npcModels.clear();
    }
}

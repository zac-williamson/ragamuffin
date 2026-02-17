package ragamuffin.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders NPCs as simple colored box models in the 3D world.
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
        // Create a box model for each NPC type with distinctive colours
        npcModels.put(NPCType.PUBLIC, createBoxModel(
            new Color(0.3f, 0.3f, 0.8f, 1f), // Blue - members of the public
            NPC.WIDTH, NPC.HEIGHT, NPC.DEPTH));
        npcModels.put(NPCType.DOG, createBoxModel(
            new Color(0.6f, 0.4f, 0.2f, 1f), // Brown - dogs
            0.5f, 0.6f, 0.8f)); // Smaller, longer body
        npcModels.put(NPCType.YOUTH_GANG, createBoxModel(
            new Color(0.8f, 0.2f, 0.2f, 1f), // Red - gangs of youths
            NPC.WIDTH, NPC.HEIGHT, NPC.DEPTH));
        npcModels.put(NPCType.COUNCIL_MEMBER, createBoxModel(
            new Color(0.5f, 0.5f, 0.5f, 1f), // Grey - council members
            NPC.WIDTH, NPC.HEIGHT, NPC.DEPTH));
        npcModels.put(NPCType.POLICE, createBoxModel(
            new Color(0.1f, 0.1f, 0.4f, 1f), // Dark blue - police
            NPC.WIDTH, NPC.HEIGHT, NPC.DEPTH));
        npcModels.put(NPCType.COUNCIL_BUILDER, createBoxModel(
            new Color(0.9f, 0.6f, 0.1f, 1f), // Orange hi-vis - council builders
            NPC.WIDTH, NPC.HEIGHT, NPC.DEPTH));
    }

    private Model createBoxModel(Color color, float width, float height, float depth) {
        long attributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Material material = new Material(ColorAttribute.createDiffuse(color));

        modelBuilder.begin();
        MeshPartBuilder builder = modelBuilder.part("box", GL20.GL_TRIANGLES, attributes, material);
        builder.box(width, height, depth);
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
            // Position at NPC feet, offset Y by half height so the model center is at the right spot
            float halfHeight = (npc.getType() == NPCType.DOG) ? 0.3f : NPC.HEIGHT / 2f;
            instance.transform.setToTranslation(pos.x, pos.y + halfHeight, pos.z);
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

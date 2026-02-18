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
 * Renders NPCs as Minecraft-style humanoid characters with separate articulated
 * body parts: head, torso, left/right arms, left/right legs.
 * Each part is rendered as an individual model instance so limbs can be
 * rotated independently for walk-cycle animation.
 *
 * Dogs get a quadruped model with body, head, snout, tail, and four legs.
 * Police get a custodian helmet (traditional British bobby helmet).
 *
 * ModelInstances are cached per-model to avoid GC pressure.
 */
public class NPCRenderer {

    private static final long ATTRS = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
    private static final float WALK_SPEED = 6.0f; // animation cycle speed
    private static final float WALK_AMPLITUDE = 45f; // max limb swing in degrees

    // Humanoid proportions (Minecraft-style, slightly less blocky)
    private static final float HEAD_W = 0.4f, HEAD_H = 0.4f, HEAD_D = 0.4f;
    private static final float TORSO_W = 0.5f, TORSO_H = 0.6f, TORSO_D = 0.25f;
    private static final float ARM_W = 0.2f, ARM_H = 0.6f, ARM_D = 0.2f;
    private static final float LEG_W = 0.2f, LEG_H = 0.6f, LEG_D = 0.2f;

    // Per-type part models and cached instances
    // Humanoid: [head, torso, leftArm, rightArm, leftLeg, rightLeg, face, helmet(opt)]
    private final Map<NPCType, Model[]> humanoidParts;
    private final Map<NPCType, ModelInstance[]> humanoidInstances;
    private final Map<NPCType, Model[]> dogParts;
    private final ModelInstance[] dogInstances;
    private final ModelBuilder mb;

    // Reusable transform to avoid GC pressure
    private final Matrix4 tmpTransform = new Matrix4();

    public NPCRenderer() {
        mb = new ModelBuilder();
        humanoidParts = new HashMap<>();
        humanoidInstances = new HashMap<>();
        dogParts = new HashMap<>();
        dogInstances = new ModelInstance[9];
        buildAllModels();
    }

    private void buildAllModels() {
        // PUBLIC - Blue civilians
        buildAndCacheHumanoid(NPCType.PUBLIC,
            new Color(0.3f, 0.3f, 0.8f, 1f),
            new Color(0.2f, 0.2f, 0.5f, 1f),
            new Color(0.85f, 0.70f, 0.60f, 1f),
            new Color(0.10f, 0.10f, 0.10f, 1f),
            false);

        // YOUTH_GANG - Red hoodies
        buildAndCacheHumanoid(NPCType.YOUTH_GANG,
            new Color(0.8f, 0.15f, 0.15f, 1f),
            new Color(0.15f, 0.15f, 0.15f, 1f),
            new Color(0.85f, 0.70f, 0.60f, 1f),
            new Color(0.10f, 0.10f, 0.10f, 1f),
            false);

        // COUNCIL_MEMBER - Grey suits
        buildAndCacheHumanoid(NPCType.COUNCIL_MEMBER,
            new Color(0.45f, 0.45f, 0.50f, 1f),
            new Color(0.35f, 0.35f, 0.40f, 1f),
            new Color(0.85f, 0.70f, 0.60f, 1f),
            new Color(0.10f, 0.10f, 0.10f, 1f),
            false);

        // POLICE - Dark blue uniform with custodian helmet
        buildAndCacheHumanoid(NPCType.POLICE,
            new Color(0.10f, 0.10f, 0.35f, 1f),
            new Color(0.10f, 0.10f, 0.30f, 1f),
            new Color(0.85f, 0.70f, 0.60f, 1f),
            new Color(0.10f, 0.10f, 0.10f, 1f),
            true);

        // COUNCIL_BUILDER - Orange hi-vis
        buildAndCacheHumanoid(NPCType.COUNCIL_BUILDER,
            new Color(0.95f, 0.55f, 0.05f, 1f),
            new Color(0.30f, 0.30f, 0.35f, 1f),
            new Color(0.85f, 0.70f, 0.60f, 1f),
            new Color(0.10f, 0.10f, 0.10f, 1f),
            false);

        // DOG - Brown quadruped
        Model[] dParts = buildDogParts(
            new Color(0.55f, 0.35f, 0.18f, 1f),
            new Color(0.45f, 0.28f, 0.12f, 1f),
            new Color(0.05f, 0.05f, 0.05f, 1f));
        dogParts.put(NPCType.DOG, dParts);
        for (int i = 0; i < dParts.length; i++) {
            dogInstances[i] = new ModelInstance(dParts[i]);
        }
    }

    private void buildAndCacheHumanoid(NPCType type, Color shirtColor, Color trouserColor,
                                        Color skinColor, Color eyeColor, boolean hasHelmet) {
        Model[] parts = buildHumanoidParts(shirtColor, trouserColor, skinColor, eyeColor, hasHelmet);
        humanoidParts.put(type, parts);
        ModelInstance[] instances = new ModelInstance[parts.length];
        for (int i = 0; i < parts.length; i++) {
            instances[i] = new ModelInstance(parts[i]);
        }
        humanoidInstances.put(type, instances);
    }

    /**
     * Build the separate models for a humanoid NPC.
     * Index: 0=head, 1=torso, 2=leftArm, 3=rightArm, 4=leftLeg, 5=rightLeg, 6=face
     * If hasHelmet: 7=helmet
     */
    private Model[] buildHumanoidParts(Color shirtColor, Color trouserColor,
                                        Color skinColor, Color eyeColor, boolean hasHelmet) {
        int count = hasHelmet ? 8 : 7;
        Model[] parts = new Model[count];

        parts[0] = buildBox(HEAD_W, HEAD_H, HEAD_D, skinColor);
        parts[1] = buildBox(TORSO_W, TORSO_H, TORSO_D, shirtColor);
        parts[2] = buildBox(ARM_W, ARM_H, ARM_D, skinColor);
        parts[3] = buildBox(ARM_W, ARM_H, ARM_D, skinColor);
        parts[4] = buildBox(LEG_W, LEG_H, LEG_D, trouserColor);
        parts[5] = buildBox(LEG_W, LEG_H, LEG_D, trouserColor);
        parts[6] = buildFace(eyeColor);

        if (hasHelmet) {
            parts[7] = buildCustodianHelmet();
        }

        return parts;
    }

    /**
     * Build the separate models for a dog.
     */
    private Model[] buildDogParts(Color bodyColor, Color headColor, Color eyeColor) {
        Model[] parts = new Model[9];
        parts[0] = buildBox(0.35f, 0.30f, 0.65f, bodyColor);
        parts[1] = buildBox(0.28f, 0.26f, 0.26f, headColor);
        parts[2] = buildBox(0.12f, 0.10f, 0.12f, new Color(0.30f, 0.20f, 0.10f, 1f));
        parts[3] = buildBox(0.06f, 0.22f, 0.06f, bodyColor);
        Color legColor = new Color(bodyColor).lerp(Color.BLACK, 0.1f);
        parts[4] = buildBox(0.10f, 0.30f, 0.10f, legColor);
        parts[5] = buildBox(0.10f, 0.30f, 0.10f, legColor);
        parts[6] = buildBox(0.10f, 0.30f, 0.10f, legColor);
        parts[7] = buildBox(0.10f, 0.30f, 0.10f, legColor);
        parts[8] = buildDogEyes(eyeColor);
        return parts;
    }

    private Model buildBox(float w, float h, float d, Color color) {
        mb.begin();
        Material mat = new Material(ColorAttribute.createDiffuse(color));
        MeshPartBuilder mpb = mb.part("box", GL20.GL_TRIANGLES, ATTRS, mat);
        mpb.box(w, h, d);
        return mb.end();
    }

    /**
     * Build a traditional British custodian helmet (bobby helmet).
     * Composed of a dome (wide brim base) and a tall pointed crown.
     */
    private Model buildCustodianHelmet() {
        mb.begin();
        Color helmetBlue = new Color(0.08f, 0.08f, 0.28f, 1f);
        Material mat = new Material(ColorAttribute.createDiffuse(helmetBlue));

        // Brim — wide flat box at the base of the helmet
        MeshPartBuilder brim = mb.part("brim", GL20.GL_TRIANGLES, ATTRS, mat);
        brim.setVertexTransform(new Matrix4().setToTranslation(0f, -0.02f, 0f));
        brim.box(0.48f, 0.04f, 0.48f);

        // Main dome — slightly tapered (wider at bottom, narrower at top)
        MeshPartBuilder dome = mb.part("dome", GL20.GL_TRIANGLES, ATTRS, mat);
        dome.setVertexTransform(new Matrix4().setToTranslation(0f, 0.10f, 0f));
        dome.box(0.38f, 0.20f, 0.38f);

        // Crown/point — the distinctive tall pointed top
        MeshPartBuilder crown = mb.part("crown", GL20.GL_TRIANGLES, ATTRS, mat);
        crown.setVertexTransform(new Matrix4().setToTranslation(0f, 0.28f, 0f));
        crown.box(0.22f, 0.16f, 0.22f);

        // Tip — very top point
        MeshPartBuilder tip = mb.part("tip", GL20.GL_TRIANGLES, ATTRS, mat);
        tip.setVertexTransform(new Matrix4().setToTranslation(0f, 0.40f, 0f));
        tip.box(0.10f, 0.08f, 0.10f);

        // Badge — small silver box on the front
        Material badgeMat = new Material(ColorAttribute.createDiffuse(new Color(0.75f, 0.75f, 0.80f, 1f)));
        MeshPartBuilder badge = mb.part("badge", GL20.GL_TRIANGLES, ATTRS, badgeMat);
        badge.setVertexTransform(new Matrix4().setToTranslation(0f, 0.10f, 0.20f));
        badge.box(0.08f, 0.08f, 0.02f);

        return mb.end();
    }

    private Model buildFace(Color eyeColor) {
        mb.begin();
        Material mat = new Material(ColorAttribute.createDiffuse(eyeColor));

        MeshPartBuilder le = mb.part("leftEye", GL20.GL_TRIANGLES, ATTRS, mat);
        le.setVertexTransform(new Matrix4().setToTranslation(-0.09f, 0.04f, 0f));
        le.box(0.07f, 0.07f, 0.02f);

        MeshPartBuilder re = mb.part("rightEye", GL20.GL_TRIANGLES, ATTRS, mat);
        re.setVertexTransform(new Matrix4().setToTranslation(0.09f, 0.04f, 0f));
        re.box(0.07f, 0.07f, 0.02f);

        MeshPartBuilder mo = mb.part("mouth", GL20.GL_TRIANGLES, ATTRS, mat);
        mo.setVertexTransform(new Matrix4().setToTranslation(0f, -0.08f, 0f));
        mo.box(0.14f, 0.04f, 0.02f);

        return mb.end();
    }

    private Model buildDogEyes(Color eyeColor) {
        mb.begin();
        Material mat = new Material(ColorAttribute.createDiffuse(eyeColor));

        MeshPartBuilder le = mb.part("leftEye", GL20.GL_TRIANGLES, ATTRS, mat);
        le.setVertexTransform(new Matrix4().setToTranslation(-0.06f, 0.04f, 0f));
        le.box(0.04f, 0.04f, 0.02f);

        MeshPartBuilder re = mb.part("rightEye", GL20.GL_TRIANGLES, ATTRS, mat);
        re.setVertexTransform(new Matrix4().setToTranslation(0.06f, 0.04f, 0f));
        re.box(0.04f, 0.04f, 0.02f);

        MeshPartBuilder no = mb.part("nose", GL20.GL_TRIANGLES, ATTRS, mat);
        no.setVertexTransform(new Matrix4().setToTranslation(0f, -0.05f, 0f));
        no.box(0.05f, 0.04f, 0.02f);

        return mb.end();
    }

    /**
     * Render all NPCs.
     */
    public void render(ModelBatch modelBatch, Environment environment, List<NPC> npcs) {
        for (NPC npc : npcs) {
            if (npc.getType() == NPCType.DOG) {
                renderDog(modelBatch, environment, npc);
            } else {
                renderHumanoid(modelBatch, environment, npc);
            }
        }
    }

    private void renderHumanoid(ModelBatch modelBatch, Environment environment, NPC npc) {
        ModelInstance[] instances = humanoidInstances.get(npc.getType());
        if (instances == null) return;

        Vector3 pos = npc.getPosition();
        float yaw = npc.getFacingAngle();
        float animT = npc.getAnimTime();

        float swing = (float) Math.sin(animT * WALK_SPEED) * WALK_AMPLITUDE;
        float speed = npc.getVelocity().len();
        if (speed < 0.01f) {
            swing = 0;
        }
        float swingRad = (float) Math.toRadians(swing);

        float legTop = LEG_H;
        float torsoBottom = legTop;
        float torsoTop = torsoBottom + TORSO_H;
        float headCentre = torsoTop + HEAD_H / 2f;
        float torsoCentre = torsoBottom + TORSO_H / 2f;
        float armPivotY = torsoTop - 0.05f;
        float legPivotY = torsoBottom;

        float yawRad = (float) Math.toRadians(yaw);

        // 1 - Torso
        setPartTransform(instances[1], pos, yawRad, 0f, torsoCentre, 0f);
        modelBatch.render(instances[1], environment);

        // 0 - Head
        setPartTransform(instances[0], pos, yawRad, 0f, headCentre, 0f);
        modelBatch.render(instances[0], environment);

        // 6 - Face (on the +Z side of the head, which is the front at yaw 0)
        setPartTransform(instances[6], pos, yawRad, 0f, headCentre, (HEAD_D / 2f + 0.011f));
        modelBatch.render(instances[6], environment);

        // 7 - Helmet (police only)
        if (instances.length > 7) {
            float helmetY = torsoTop + HEAD_H + 0.02f; // sits on top of head
            setPartTransform(instances[7], pos, yawRad, 0f, helmetY, 0f);
            modelBatch.render(instances[7], environment);
        }

        // 2 - Left arm (swings opposite to left leg for natural gait)
        setLimbTransform(instances[2], pos, yawRad,
            -(TORSO_W / 2f + ARM_W / 2f), armPivotY, 0f, swingRad, ARM_H);
        modelBatch.render(instances[2], environment);

        // 3 - Right arm
        setLimbTransform(instances[3], pos, yawRad,
            (TORSO_W / 2f + ARM_W / 2f), armPivotY, 0f, -swingRad, ARM_H);
        modelBatch.render(instances[3], environment);

        // 4 - Left leg
        setLimbTransform(instances[4], pos, yawRad,
            -(TORSO_W / 2f - LEG_W / 2f), legPivotY, 0f, -swingRad, LEG_H);
        modelBatch.render(instances[4], environment);

        // 5 - Right leg
        setLimbTransform(instances[5], pos, yawRad,
            (TORSO_W / 2f - LEG_W / 2f), legPivotY, 0f, swingRad, LEG_H);
        modelBatch.render(instances[5], environment);
    }

    /**
     * Set transform for a static body part.
     */
    private void setPartTransform(ModelInstance instance, Vector3 npcPos, float yawRad,
                                   float localX, float localY, float localZ) {
        float cosY = (float) Math.cos(yawRad);
        float sinY = (float) Math.sin(yawRad);

        float worldX = npcPos.x + localX * cosY + localZ * sinY;
        float worldY = npcPos.y + localY;
        float worldZ = npcPos.z - localX * sinY + localZ * cosY;

        tmpTransform.idt();
        tmpTransform.setToTranslation(worldX, worldY, worldZ);
        tmpTransform.rotate(Vector3.Y, (float) Math.toDegrees(yawRad));

        instance.transform.set(tmpTransform);
    }

    /**
     * Set transform for an animated limb.
     */
    private void setLimbTransform(ModelInstance instance, Vector3 npcPos, float yawRad,
                                   float localX, float pivotY, float localZ,
                                   float swingRad, float limbHeight) {
        float cosY = (float) Math.cos(yawRad);
        float sinY = (float) Math.sin(yawRad);

        float pivotWorldX = npcPos.x + localX * cosY + localZ * sinY;
        float pivotWorldY = npcPos.y + pivotY;
        float pivotWorldZ = npcPos.z - localX * sinY + localZ * cosY;

        tmpTransform.idt();
        tmpTransform.setToTranslation(pivotWorldX, pivotWorldY, pivotWorldZ);
        tmpTransform.rotate(Vector3.Y, (float) Math.toDegrees(yawRad));
        tmpTransform.rotate(Vector3.X, (float) Math.toDegrees(swingRad));
        tmpTransform.translate(0, -limbHeight / 2f, 0);

        instance.transform.set(tmpTransform);
    }

    private void renderDog(ModelBatch modelBatch, Environment environment, NPC npc) {
        Model[] parts = dogParts.get(NPCType.DOG);
        if (parts == null) return;

        Vector3 pos = npc.getPosition();
        float yaw = npc.getFacingAngle();
        float animT = npc.getAnimTime();

        float speed = npc.getVelocity().len();
        float swing = (speed < 0.01f) ? 0f
            : (float) Math.sin(animT * WALK_SPEED * 1.5f) * 30f;
        float swingRad = (float) Math.toRadians(swing);

        float yawRad = (float) Math.toRadians(yaw);

        float bodyH = 0.30f;
        float legH = 0.30f;
        float bodyCentreY = legH + bodyH / 2f;

        // 0 - Body
        setPartTransform(dogInstances[0], pos, yawRad, 0f, bodyCentreY, 0f);
        modelBatch.render(dogInstances[0], environment);

        // 1 - Head (at +Z, the front of the dog)
        float headZ = (0.65f / 2f + 0.26f / 2f - 0.04f);
        setPartTransform(dogInstances[1], pos, yawRad, 0f, bodyCentreY + 0.06f, headZ);
        modelBatch.render(dogInstances[1], environment);

        // 2 - Snout
        setPartTransform(dogInstances[2], pos, yawRad,
            0f, bodyCentreY, headZ + 0.26f / 2f + 0.06f);
        modelBatch.render(dogInstances[2], environment);

        // 8 - Eyes
        setPartTransform(dogInstances[8], pos, yawRad,
            0f, bodyCentreY + 0.10f, headZ + 0.26f / 2f + 0.011f);
        modelBatch.render(dogInstances[8], environment);

        // 3 - Tail (at -Z, the back of the dog)
        float tailZ = -(0.65f / 2f + 0.03f);
        setPartTransform(dogInstances[3], pos, yawRad, 0f, bodyCentreY + 0.15f, tailZ);
        modelBatch.render(dogInstances[3], environment);

        // 4-7: Legs with walk animation (front at +Z, back at -Z)
        float frontLegZ = 0.65f / 2f - 0.10f;
        float backLegZ = -(0.65f / 2f - 0.10f);
        float legPivotY = bodyCentreY - bodyH / 2f;
        float legSpreadX = 0.12f;

        setLimbTransform(dogInstances[4], pos, yawRad, -legSpreadX, legPivotY, frontLegZ, swingRad, legH);
        modelBatch.render(dogInstances[4], environment);
        setLimbTransform(dogInstances[5], pos, yawRad, legSpreadX, legPivotY, frontLegZ, -swingRad, legH);
        modelBatch.render(dogInstances[5], environment);
        setLimbTransform(dogInstances[6], pos, yawRad, -legSpreadX, legPivotY, backLegZ, -swingRad, legH);
        modelBatch.render(dogInstances[6], environment);
        setLimbTransform(dogInstances[7], pos, yawRad, legSpreadX, legPivotY, backLegZ, swingRad, legH);
        modelBatch.render(dogInstances[7], environment);
    }

    public void dispose() {
        for (Model[] parts : humanoidParts.values()) {
            for (Model m : parts) {
                if (m != null) m.dispose();
            }
        }
        humanoidParts.clear();
        humanoidInstances.clear();

        for (Model[] parts : dogParts.values()) {
            for (Model m : parts) {
                if (m != null) m.dispose();
            }
        }
        dogParts.clear();
    }
}

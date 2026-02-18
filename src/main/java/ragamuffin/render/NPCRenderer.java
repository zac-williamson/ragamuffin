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

    // Per-type part models: [head, torso, leftArm, rightArm, leftLeg, rightLeg, face(eyes+mouth)]
    private final Map<NPCType, Model[]> humanoidParts;
    private final Map<NPCType, Model[]> dogParts;
    private final ModelBuilder mb;

    // Reusable transforms to avoid GC pressure
    private final Matrix4 tmpTransform = new Matrix4();

    public NPCRenderer() {
        mb = new ModelBuilder();
        humanoidParts = new HashMap<>();
        dogParts = new HashMap<>();
        buildAllModels();
    }

    private void buildAllModels() {
        // PUBLIC - Blue civilians
        humanoidParts.put(NPCType.PUBLIC, buildHumanoidParts(
            new Color(0.3f, 0.3f, 0.8f, 1f),       // shirt
            new Color(0.2f, 0.2f, 0.5f, 1f),        // trousers
            new Color(0.85f, 0.70f, 0.60f, 1f),     // skin
            new Color(0.10f, 0.10f, 0.10f, 1f)));   // eyes

        // YOUTH_GANG - Red hoodies
        humanoidParts.put(NPCType.YOUTH_GANG, buildHumanoidParts(
            new Color(0.8f, 0.15f, 0.15f, 1f),
            new Color(0.15f, 0.15f, 0.15f, 1f),     // dark tracksuit bottoms
            new Color(0.85f, 0.70f, 0.60f, 1f),
            new Color(0.10f, 0.10f, 0.10f, 1f)));

        // COUNCIL_MEMBER - Grey suits
        humanoidParts.put(NPCType.COUNCIL_MEMBER, buildHumanoidParts(
            new Color(0.45f, 0.45f, 0.50f, 1f),
            new Color(0.35f, 0.35f, 0.40f, 1f),
            new Color(0.85f, 0.70f, 0.60f, 1f),
            new Color(0.10f, 0.10f, 0.10f, 1f)));

        // POLICE - Dark blue uniform
        humanoidParts.put(NPCType.POLICE, buildHumanoidParts(
            new Color(0.10f, 0.10f, 0.35f, 1f),
            new Color(0.10f, 0.10f, 0.30f, 1f),
            new Color(0.85f, 0.70f, 0.60f, 1f),
            new Color(0.10f, 0.10f, 0.10f, 1f)));

        // COUNCIL_BUILDER - Orange hi-vis
        humanoidParts.put(NPCType.COUNCIL_BUILDER, buildHumanoidParts(
            new Color(0.95f, 0.55f, 0.05f, 1f),
            new Color(0.30f, 0.30f, 0.35f, 1f),
            new Color(0.85f, 0.70f, 0.60f, 1f),
            new Color(0.10f, 0.10f, 0.10f, 1f)));

        // DOG - Brown quadruped
        dogParts.put(NPCType.DOG, buildDogParts(
            new Color(0.55f, 0.35f, 0.18f, 1f),     // body
            new Color(0.45f, 0.28f, 0.12f, 1f),     // head
            new Color(0.05f, 0.05f, 0.05f, 1f)));   // eyes/nose
    }

    /**
     * Build the 7 separate models for a humanoid NPC.
     * Index: 0=head, 1=torso, 2=leftArm, 3=rightArm, 4=leftLeg, 5=rightLeg, 6=face
     * All parts are built centred at origin so we can position them with transforms.
     */
    private Model[] buildHumanoidParts(Color shirtColor, Color trouserColor, Color skinColor, Color eyeColor) {
        Model[] parts = new Model[7];

        // 0 - Head (skin coloured cube)
        parts[0] = buildBox(HEAD_W, HEAD_H, HEAD_D, skinColor);

        // 1 - Torso (shirt coloured)
        parts[1] = buildBox(TORSO_W, TORSO_H, TORSO_D, shirtColor);

        // 2 - Left arm (skin-tone with shirt sleeve effect: just use shirt colour)
        parts[2] = buildBox(ARM_W, ARM_H, ARM_D, skinColor);

        // 3 - Right arm
        parts[3] = buildBox(ARM_W, ARM_H, ARM_D, skinColor);

        // 4 - Left leg (trouser coloured)
        parts[4] = buildBox(LEG_W, LEG_H, LEG_D, trouserColor);

        // 5 - Right leg
        parts[5] = buildBox(LEG_W, LEG_H, LEG_D, trouserColor);

        // 6 - Face (eyes + mouth, rendered as a flat composite on front of head)
        parts[6] = buildFace(eyeColor);

        return parts;
    }

    /**
     * Build the separate models for a dog.
     * Index: 0=body, 1=head, 2=snout, 3=tail, 4=frontLeftLeg, 5=frontRightLeg,
     *        6=backLeftLeg, 7=backRightLeg, 8=eyes
     */
    private Model[] buildDogParts(Color bodyColor, Color headColor, Color eyeColor) {
        Model[] parts = new Model[9];

        // 0 - Body (long horizontal box)
        parts[0] = buildBox(0.35f, 0.30f, 0.65f, bodyColor);

        // 1 - Head
        parts[1] = buildBox(0.28f, 0.26f, 0.26f, headColor);

        // 2 - Snout
        parts[2] = buildBox(0.12f, 0.10f, 0.12f, new Color(0.30f, 0.20f, 0.10f, 1f));

        // 3 - Tail (thin vertical box, angled later)
        parts[3] = buildBox(0.06f, 0.22f, 0.06f, bodyColor);

        // 4-7: Four legs
        Color legColor = new Color(bodyColor).lerp(Color.BLACK, 0.1f);
        parts[4] = buildBox(0.10f, 0.30f, 0.10f, legColor);
        parts[5] = buildBox(0.10f, 0.30f, 0.10f, legColor);
        parts[6] = buildBox(0.10f, 0.30f, 0.10f, legColor);
        parts[7] = buildBox(0.10f, 0.30f, 0.10f, legColor);

        // 8 - Eyes (two dots as a single mesh part)
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
     * Build a face model: two eyes and a mouth, all as small boxes
     * positioned relative to centre of head. Faces -Z direction.
     */
    private Model buildFace(Color eyeColor) {
        mb.begin();
        Material mat = new Material(ColorAttribute.createDiffuse(eyeColor));

        // Left eye
        MeshPartBuilder le = mb.part("leftEye", GL20.GL_TRIANGLES, ATTRS, mat);
        le.setVertexTransform(new Matrix4().setToTranslation(-0.09f, 0.04f, 0f));
        le.box(0.07f, 0.07f, 0.02f);

        // Right eye
        MeshPartBuilder re = mb.part("rightEye", GL20.GL_TRIANGLES, ATTRS, mat);
        re.setVertexTransform(new Matrix4().setToTranslation(0.09f, 0.04f, 0f));
        re.box(0.07f, 0.07f, 0.02f);

        // Mouth
        MeshPartBuilder mo = mb.part("mouth", GL20.GL_TRIANGLES, ATTRS, mat);
        mo.setVertexTransform(new Matrix4().setToTranslation(0f, -0.08f, 0f));
        mo.box(0.14f, 0.04f, 0.02f);

        return mb.end();
    }

    private Model buildDogEyes(Color eyeColor) {
        mb.begin();
        Material mat = new Material(ColorAttribute.createDiffuse(eyeColor));

        // Left eye
        MeshPartBuilder le = mb.part("leftEye", GL20.GL_TRIANGLES, ATTRS, mat);
        le.setVertexTransform(new Matrix4().setToTranslation(-0.06f, 0.04f, 0f));
        le.box(0.04f, 0.04f, 0.02f);

        // Right eye
        MeshPartBuilder re = mb.part("rightEye", GL20.GL_TRIANGLES, ATTRS, mat);
        re.setVertexTransform(new Matrix4().setToTranslation(0.06f, 0.04f, 0f));
        re.box(0.04f, 0.04f, 0.02f);

        // Nose (on front of snout conceptually, but we'll position relative to head)
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
        Model[] parts = humanoidParts.get(npc.getType());
        if (parts == null) return;

        Vector3 pos = npc.getPosition();
        float yaw = npc.getFacingAngle();
        float animT = npc.getAnimTime();

        // Calculate walk cycle: sinusoidal swing
        float swing = (float) Math.sin(animT * WALK_SPEED) * WALK_AMPLITUDE;
        // Check if NPC is actually moving (has velocity)
        float speed = npc.getVelocity().len();
        if (speed < 0.01f) {
            swing = 0; // No animation when standing still
        }
        float swingRad = (float) Math.toRadians(swing);

        // Y positions (origin = feet)
        float legBottom = 0f;
        float legTop = LEG_H;
        float torsoBottom = legTop;
        float torsoTop = torsoBottom + TORSO_H;
        float headBottom = torsoTop;
        float headCentre = headBottom + HEAD_H / 2f;
        float torsoCentre = torsoBottom + TORSO_H / 2f;
        float armPivotY = torsoTop - 0.05f; // arms pivot from top of torso
        float legPivotY = torsoBottom;       // legs pivot from bottom of torso

        // Base transform: translate to NPC position and rotate to facing direction
        // yaw: 0 = +Z, we rotate around Y axis
        float yawRad = (float) Math.toRadians(yaw);

        // 1 - Torso (static, just positioned)
        renderPart(modelBatch, environment, parts[1], pos, yawRad,
            0f, torsoCentre, 0f, 0f, 0f);

        // 0 - Head (static on top of torso)
        renderPart(modelBatch, environment, parts[0], pos, yawRad,
            0f, headCentre, 0f, 0f, 0f);

        // 6 - Face (positioned on the front of the head, facing forward)
        renderPart(modelBatch, environment, parts[6], pos, yawRad,
            0f, headCentre, -(HEAD_D / 2f + 0.011f), 0f, 0f);

        // 2 - Left arm: pivots from shoulder, swings opposite to right leg
        renderLimb(modelBatch, environment, parts[2], pos, yawRad,
            -(TORSO_W / 2f + ARM_W / 2f), armPivotY, 0f,
            -swingRad, ARM_H);

        // 3 - Right arm: swings opposite to left leg
        renderLimb(modelBatch, environment, parts[3], pos, yawRad,
            (TORSO_W / 2f + ARM_W / 2f), armPivotY, 0f,
            swingRad, ARM_H);

        // 4 - Left leg: swings forward when right arm swings forward
        renderLimb(modelBatch, environment, parts[4], pos, yawRad,
            -(TORSO_W / 2f - LEG_W / 2f), legPivotY, 0f,
            swingRad, LEG_H);

        // 5 - Right leg
        renderLimb(modelBatch, environment, parts[5], pos, yawRad,
            (TORSO_W / 2f - LEG_W / 2f), legPivotY, 0f,
            -swingRad, LEG_H);
    }

    /**
     * Render a static body part at a position relative to the NPC origin.
     */
    private void renderPart(ModelBatch modelBatch, Environment environment, Model model,
                            Vector3 npcPos, float yawRad,
                            float localX, float localY, float localZ,
                            float pitchRad, float rollRad) {
        ModelInstance instance = new ModelInstance(model);

        // Build transform: first rotate the local offset by the NPC's yaw, then translate
        float cosY = (float) Math.cos(yawRad);
        float sinY = (float) Math.sin(yawRad);

        float worldX = npcPos.x + localX * cosY + localZ * sinY;
        float worldY = npcPos.y + localY;
        float worldZ = npcPos.z - localX * sinY + localZ * cosY;

        tmpTransform.idt();
        tmpTransform.setToTranslation(worldX, worldY, worldZ);
        tmpTransform.rotate(Vector3.Y, (float) Math.toDegrees(yawRad));

        instance.transform.set(tmpTransform);
        modelBatch.render(instance, environment);
    }

    /**
     * Render a limb (arm or leg) that swings from a pivot point.
     * The limb rotates around its top edge (pivot) on the X axis (pitch).
     */
    private void renderLimb(ModelBatch modelBatch, Environment environment, Model model,
                            Vector3 npcPos, float yawRad,
                            float localX, float pivotY, float localZ,
                            float swingRad, float limbHeight) {
        ModelInstance instance = new ModelInstance(model);

        // The limb's pivot is at (localX, pivotY, localZ) in NPC-local space.
        // The limb hangs down from the pivot. When swing=0, it points straight down.
        // The box model is centred at origin, so the top of the limb is at +limbHeight/2.
        // We need to: translate so top of limb is at pivot, then rotate around pivot.

        // 1. Start at origin
        // 2. Translate so the top of the limb (centre + h/2) is at pivot point
        // 3. Rotate around the pivot (X-axis rotation in local space)

        float cosY = (float) Math.cos(yawRad);
        float sinY = (float) Math.sin(yawRad);

        // World position of pivot
        float pivotWorldX = npcPos.x + localX * cosY + localZ * sinY;
        float pivotWorldY = npcPos.y + pivotY;
        float pivotWorldZ = npcPos.z - localX * sinY + localZ * cosY;

        tmpTransform.idt();
        // Translate to pivot point
        tmpTransform.setToTranslation(pivotWorldX, pivotWorldY, pivotWorldZ);
        // Apply NPC facing rotation
        tmpTransform.rotate(Vector3.Y, (float) Math.toDegrees(yawRad));
        // Apply limb swing (rotation around local X axis at pivot)
        tmpTransform.rotate(Vector3.X, (float) Math.toDegrees(swingRad));
        // Offset the limb so its top is at the pivot (move down by half height)
        tmpTransform.translate(0, -limbHeight / 2f, 0);

        instance.transform.set(tmpTransform);
        modelBatch.render(instance, environment);
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
        renderPart(modelBatch, environment, parts[0], pos, yawRad,
            0f, bodyCentreY, 0f, 0f, 0f);

        // 1 - Head (front of body, slightly higher)
        float headZ = -(0.65f / 2f + 0.26f / 2f - 0.04f);
        renderPart(modelBatch, environment, parts[1], pos, yawRad,
            0f, bodyCentreY + 0.06f, headZ, 0f, 0f);

        // 2 - Snout (in front of head)
        renderPart(modelBatch, environment, parts[2], pos, yawRad,
            0f, bodyCentreY, headZ - 0.26f / 2f - 0.06f, 0f, 0f);

        // 8 - Eyes (on front of head)
        renderPart(modelBatch, environment, parts[8], pos, yawRad,
            0f, bodyCentreY + 0.10f, headZ - 0.26f / 2f - 0.011f, 0f, 0f);

        // 3 - Tail (back of body, angled up)
        float tailZ = 0.65f / 2f + 0.03f;
        renderPart(modelBatch, environment, parts[3], pos, yawRad,
            0f, bodyCentreY + 0.15f, tailZ, 0f, 0f);

        // 4-7: Legs with walk animation
        // Front legs: opposite swing to back legs
        float frontLegZ = -0.65f / 2f + 0.10f;
        float backLegZ = 0.65f / 2f - 0.10f;
        float legPivotY = bodyCentreY - bodyH / 2f;
        float legSpreadX = 0.12f;

        // Front left
        renderLimb(modelBatch, environment, parts[4], pos, yawRad,
            -legSpreadX, legPivotY, frontLegZ, swingRad, legH);
        // Front right
        renderLimb(modelBatch, environment, parts[5], pos, yawRad,
            legSpreadX, legPivotY, frontLegZ, -swingRad, legH);
        // Back left
        renderLimb(modelBatch, environment, parts[6], pos, yawRad,
            -legSpreadX, legPivotY, backLegZ, -swingRad, legH);
        // Back right
        renderLimb(modelBatch, environment, parts[7], pos, yawRad,
            legSpreadX, legPivotY, backLegZ, swingRad, legH);
    }

    public void dispose() {
        for (Model[] parts : humanoidParts.values()) {
            for (Model m : parts) {
                if (m != null) m.dispose();
            }
        }
        humanoidParts.clear();

        for (Model[] parts : dogParts.values()) {
            for (Model m : parts) {
                if (m != null) m.dispose();
            }
        }
        dogParts.clear();
    }
}

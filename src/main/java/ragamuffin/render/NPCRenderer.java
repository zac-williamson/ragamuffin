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
import ragamuffin.entity.FacialExpression;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;

import java.util.HashMap;
import java.util.IdentityHashMap;
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
 * ModelInstances are allocated per-NPC (keyed by NPC identity) so that
 * multiple NPCs of the same type each have their own transform state.
 * The underlying Model objects (geometry/materials) are still shared per type.
 */
public class NPCRenderer {

    private static final long ATTRS = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
    private static final float WALK_SPEED = 6.0f; // animation cycle speed
    private static final float WALK_AMPLITUDE = 45f; // max limb swing in degrees

    // Humanoid proportions — detailed multi-polygon parts
    private static final float HEAD_W = 0.38f, HEAD_H = 0.38f, HEAD_D = 0.38f;
    private static final float NECK_W = 0.15f, NECK_H = 0.08f, NECK_D = 0.15f;
    private static final float SHOULDER_W = 0.60f, SHOULDER_H = 0.08f, SHOULDER_D = 0.25f;
    private static final float TORSO_W = 0.48f, TORSO_H = 0.52f, TORSO_D = 0.24f;
    private static final float UPPER_ARM_W = 0.16f, UPPER_ARM_H = 0.32f, UPPER_ARM_D = 0.16f;
    private static final float FOREARM_W = 0.14f, FOREARM_H = 0.28f, FOREARM_D = 0.14f;
    private static final float HAND_W = 0.12f, HAND_H = 0.10f, HAND_D = 0.08f;
    private static final float UPPER_LEG_W = 0.20f, UPPER_LEG_H = 0.32f, UPPER_LEG_D = 0.20f;
    private static final float LOWER_LEG_W = 0.18f, LOWER_LEG_H = 0.30f, LOWER_LEG_D = 0.18f;
    private static final float FOOT_W = 0.20f, FOOT_H = 0.08f, FOOT_D = 0.28f;

    // Per-type part models and cached instances
    // Humanoid parts: head, neck, shoulders, torso, leftUpperArm, rightUpperArm,
    //   leftForearm, rightForearm, leftHand, rightHand, leftUpperLeg, rightUpperLeg,
    //   leftLowerLeg, rightLowerLeg, leftFoot, rightFoot, face, helmet(opt)
    // Indices: 0=head, 1=neck, 2=shoulders, 3=torso, 4=LUA, 5=RUA, 6=LFA, 7=RFA,
    //          8=LH, 9=RH, 10=LUL, 11=RUL, 12=LLL, 13=RLL, 14=LF, 15=RF, 16=face, 17=helmet
    private static final int PART_HEAD=0, PART_NECK=1, PART_SHOULDERS=2, PART_TORSO=3;
    private static final int PART_L_UPPER_ARM=4, PART_R_UPPER_ARM=5;
    private static final int PART_L_FOREARM=6, PART_R_FOREARM=7;
    private static final int PART_L_HAND=8, PART_R_HAND=9;
    private static final int PART_L_UPPER_LEG=10, PART_R_UPPER_LEG=11;
    private static final int PART_L_LOWER_LEG=12, PART_R_LOWER_LEG=13;
    private static final int PART_L_FOOT=14, PART_R_FOOT=15;
    private static final int PART_FACE=16, PART_HELMET=17;
    private static final int NUM_PARTS_NO_HELMET = 17;
    private static final int NUM_PARTS_WITH_HELMET = 18;

    // Shared geometry/material models per type (not per NPC)
    private final Map<NPCType, Model[]> humanoidParts;
    private final Map<NPCType, Model[]> dogParts;

    // Expression face models per NPC type: index = FacialExpression.ordinal()
    private final Map<NPCType, Model[]> expressionFaceParts;

    // Per-NPC ModelInstance arrays — keyed by NPC object identity so each NPC
    // has its own transform state even when multiple NPCs share the same type.
    private final Map<NPC, ModelInstance[]> humanoidInstances;
    private final Map<NPC, ModelInstance[]> dogInstances;

    // Per-NPC expression face instances (one per FacialExpression)
    private final Map<NPC, ModelInstance[]> expressionFaceInstances;

    private final ModelBuilder mb;

    // Reusable transform to avoid GC pressure
    private final Matrix4 tmpTransform = new Matrix4();

    public NPCRenderer() {
        mb = new ModelBuilder();
        humanoidParts = new HashMap<>();
        humanoidInstances = new IdentityHashMap<>();
        dogParts = new HashMap<>();
        dogInstances = new IdentityHashMap<>();
        expressionFaceParts = new HashMap<>();
        expressionFaceInstances = new IdentityHashMap<>();
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

        // SHOPKEEPER - Apron brown over white
        buildAndCacheHumanoid(NPCType.SHOPKEEPER,
            new Color(0.55f, 0.35f, 0.18f, 1f),
            new Color(0.25f, 0.25f, 0.30f, 1f),
            new Color(0.85f, 0.70f, 0.60f, 1f),
            new Color(0.10f, 0.10f, 0.10f, 1f),
            false);

        // POSTMAN - Royal Mail red
        buildAndCacheHumanoid(NPCType.POSTMAN,
            new Color(0.85f, 0.10f, 0.10f, 1f),
            new Color(0.20f, 0.20f, 0.25f, 1f),
            new Color(0.85f, 0.70f, 0.60f, 1f),
            new Color(0.10f, 0.10f, 0.10f, 1f),
            false);

        // JOGGER - Bright neon green activewear
        buildAndCacheHumanoid(NPCType.JOGGER,
            new Color(0.20f, 0.90f, 0.20f, 1f),
            new Color(0.15f, 0.15f, 0.15f, 1f),
            new Color(0.85f, 0.70f, 0.60f, 1f),
            new Color(0.10f, 0.10f, 0.10f, 1f),
            false);

        // DRUNK - Tatty brown jacket
        buildAndCacheHumanoid(NPCType.DRUNK,
            new Color(0.40f, 0.30f, 0.20f, 1f),
            new Color(0.30f, 0.30f, 0.25f, 1f),
            new Color(0.80f, 0.60f, 0.55f, 1f),
            new Color(0.10f, 0.10f, 0.10f, 1f),
            false);

        // BUSKER - Purple/patchwork bohemian
        buildAndCacheHumanoid(NPCType.BUSKER,
            new Color(0.50f, 0.20f, 0.60f, 1f),
            new Color(0.25f, 0.25f, 0.30f, 1f),
            new Color(0.85f, 0.70f, 0.60f, 1f),
            new Color(0.10f, 0.10f, 0.10f, 1f),
            false);

        // DELIVERY_DRIVER - Black/grey with hi-vis vest
        buildAndCacheHumanoid(NPCType.DELIVERY_DRIVER,
            new Color(0.15f, 0.15f, 0.15f, 1f),  // Dark uniform
            new Color(0.10f, 0.10f, 0.10f, 1f),   // Dark trousers
            new Color(0.85f, 0.70f, 0.60f, 1f),
            new Color(0.10f, 0.10f, 0.10f, 1f),
            false);

        // PENSIONER - Beige/cream cardigan
        buildAndCacheHumanoid(NPCType.PENSIONER,
            new Color(0.82f, 0.76f, 0.65f, 1f),  // Beige cardy
            new Color(0.50f, 0.48f, 0.42f, 1f),   // Brown trousers
            new Color(0.80f, 0.68f, 0.58f, 1f),   // Slightly paler skin
            new Color(0.10f, 0.10f, 0.10f, 1f),
            false);

        // SCHOOL_KID - Black blazer, grey trousers
        buildAndCacheHumanoid(NPCType.SCHOOL_KID,
            new Color(0.10f, 0.10f, 0.12f, 1f),  // Black blazer
            new Color(0.40f, 0.40f, 0.42f, 1f),   // Grey trousers
            new Color(0.85f, 0.70f, 0.60f, 1f),
            new Color(0.10f, 0.10f, 0.10f, 1f),
            false);

        // DOG - Brown quadruped (models only; instances created per-NPC on demand)
        Model[] dParts = buildDogParts(
            new Color(0.55f, 0.35f, 0.18f, 1f),
            new Color(0.45f, 0.28f, 0.12f, 1f),
            new Color(0.05f, 0.05f, 0.05f, 1f));
        dogParts.put(NPCType.DOG, dParts);
    }

    private void buildAndCacheHumanoid(NPCType type, Color shirtColor, Color trouserColor,
                                        Color skinColor, Color eyeColor, boolean hasHelmet) {
        Model[] parts = buildHumanoidParts(shirtColor, trouserColor, skinColor, eyeColor, hasHelmet);
        humanoidParts.put(type, parts);
        // Build one face model per FacialExpression for this type
        FacialExpression[] expressions = FacialExpression.values();
        Model[] exprFaces = new Model[expressions.length];
        for (FacialExpression expr : expressions) {
            exprFaces[expr.ordinal()] = buildExpressionFace(eyeColor, expr);
        }
        expressionFaceParts.put(type, exprFaces);
        // ModelInstances are created per-NPC on demand in getOrCreateHumanoidInstances()
    }

    /** Returns the per-NPC ModelInstance array for a humanoid NPC, creating it if needed. */
    private ModelInstance[] getOrCreateHumanoidInstances(NPC npc) {
        ModelInstance[] inst = humanoidInstances.get(npc);
        if (inst != null) return inst;
        Model[] parts = humanoidParts.get(npc.getType());
        if (parts == null) return null;
        inst = new ModelInstance[parts.length];
        for (int i = 0; i < parts.length; i++) {
            inst[i] = new ModelInstance(parts[i]);
        }
        humanoidInstances.put(npc, inst);
        return inst;
    }

    /** Returns the per-NPC ModelInstance array for a dog NPC, creating it if needed. */
    private ModelInstance[] getOrCreateDogInstances(NPC npc) {
        ModelInstance[] inst = dogInstances.get(npc);
        if (inst != null) return inst;
        Model[] parts = dogParts.get(NPCType.DOG);
        if (parts == null) return null;
        inst = new ModelInstance[parts.length];
        for (int i = 0; i < parts.length; i++) {
            inst[i] = new ModelInstance(parts[i]);
        }
        dogInstances.put(npc, inst);
        return inst;
    }

    /**
     * Returns the per-NPC expression face ModelInstance array (one per FacialExpression),
     * creating it if needed.
     */
    private ModelInstance[] getOrCreateExpressionFaceInstances(NPC npc) {
        ModelInstance[] inst = expressionFaceInstances.get(npc);
        if (inst != null) return inst;
        Model[] exprFaces = expressionFaceParts.get(npc.getType());
        if (exprFaces == null) return null;
        inst = new ModelInstance[exprFaces.length];
        for (int i = 0; i < exprFaces.length; i++) {
            inst[i] = new ModelInstance(exprFaces[i]);
        }
        expressionFaceInstances.put(npc, inst);
        return inst;
    }

    /**
     * Build a face model for the given expression. Each expression has distinct
     * eye and mouth geometry to convey emotion on the voxel character's face.
     *
     * <ul>
     *   <li>NEUTRAL  — standard square eyes, thin straight mouth</li>
     *   <li>ANGRY    — narrowed (shorter) eyes, pressed thin mouth shifted down</li>
     *   <li>SCARED   — tall wide eyes, open round mouth</li>
     *   <li>HAPPY    — normal eyes, wider smile mouth</li>
     *   <li>SURPRISED — taller eyes, larger open mouth</li>
     * </ul>
     */
    private Model buildExpressionFace(Color eyeColor, FacialExpression expression) {
        mb.begin();
        Material mat = new Material(ColorAttribute.createDiffuse(eyeColor));

        switch (expression) {
            case ANGRY: {
                // Narrowed eyes (shorter height), mouth pressed into thin line
                MeshPartBuilder le = mb.part("leftEye", GL20.GL_TRIANGLES, ATTRS, mat);
                le.setVertexTransform(new Matrix4().setToTranslation(-0.09f, 0.04f, 0f));
                le.box(0.07f, 0.03f, 0.02f); // narrowed
                MeshPartBuilder re = mb.part("rightEye", GL20.GL_TRIANGLES, ATTRS, mat);
                re.setVertexTransform(new Matrix4().setToTranslation(0.09f, 0.04f, 0f));
                re.box(0.07f, 0.03f, 0.02f); // narrowed
                MeshPartBuilder mo = mb.part("mouth", GL20.GL_TRIANGLES, ATTRS, mat);
                mo.setVertexTransform(new Matrix4().setToTranslation(0f, -0.10f, 0f));
                mo.box(0.12f, 0.02f, 0.02f); // thin, lower
                break;
            }
            case SCARED: {
                // Wide tall eyes, open wider mouth
                MeshPartBuilder le = mb.part("leftEye", GL20.GL_TRIANGLES, ATTRS, mat);
                le.setVertexTransform(new Matrix4().setToTranslation(-0.09f, 0.04f, 0f));
                le.box(0.08f, 0.10f, 0.02f); // taller
                MeshPartBuilder re = mb.part("rightEye", GL20.GL_TRIANGLES, ATTRS, mat);
                re.setVertexTransform(new Matrix4().setToTranslation(0.09f, 0.04f, 0f));
                re.box(0.08f, 0.10f, 0.02f); // taller
                MeshPartBuilder mo = mb.part("mouth", GL20.GL_TRIANGLES, ATTRS, mat);
                mo.setVertexTransform(new Matrix4().setToTranslation(0f, -0.09f, 0f));
                mo.box(0.10f, 0.07f, 0.02f); // open mouth
                break;
            }
            case HAPPY: {
                // Normal eyes, wider mouth
                MeshPartBuilder le = mb.part("leftEye", GL20.GL_TRIANGLES, ATTRS, mat);
                le.setVertexTransform(new Matrix4().setToTranslation(-0.09f, 0.04f, 0f));
                le.box(0.07f, 0.07f, 0.02f);
                MeshPartBuilder re = mb.part("rightEye", GL20.GL_TRIANGLES, ATTRS, mat);
                re.setVertexTransform(new Matrix4().setToTranslation(0.09f, 0.04f, 0f));
                re.box(0.07f, 0.07f, 0.02f);
                MeshPartBuilder mo = mb.part("mouth", GL20.GL_TRIANGLES, ATTRS, mat);
                mo.setVertexTransform(new Matrix4().setToTranslation(0f, -0.08f, 0f));
                mo.box(0.18f, 0.04f, 0.02f); // wider smile
                break;
            }
            case SURPRISED: {
                // Raised, taller eyes; larger round mouth
                MeshPartBuilder le = mb.part("leftEye", GL20.GL_TRIANGLES, ATTRS, mat);
                le.setVertexTransform(new Matrix4().setToTranslation(-0.09f, 0.06f, 0f));
                le.box(0.08f, 0.09f, 0.02f); // taller, raised
                MeshPartBuilder re = mb.part("rightEye", GL20.GL_TRIANGLES, ATTRS, mat);
                re.setVertexTransform(new Matrix4().setToTranslation(0.09f, 0.06f, 0f));
                re.box(0.08f, 0.09f, 0.02f); // taller, raised
                MeshPartBuilder mo = mb.part("mouth", GL20.GL_TRIANGLES, ATTRS, mat);
                mo.setVertexTransform(new Matrix4().setToTranslation(0f, -0.09f, 0f));
                mo.box(0.09f, 0.08f, 0.02f); // larger open circle
                break;
            }
            default: {
                // NEUTRAL — same as original buildFace
                MeshPartBuilder le = mb.part("leftEye", GL20.GL_TRIANGLES, ATTRS, mat);
                le.setVertexTransform(new Matrix4().setToTranslation(-0.09f, 0.04f, 0f));
                le.box(0.07f, 0.07f, 0.02f);
                MeshPartBuilder re = mb.part("rightEye", GL20.GL_TRIANGLES, ATTRS, mat);
                re.setVertexTransform(new Matrix4().setToTranslation(0.09f, 0.04f, 0f));
                re.box(0.07f, 0.07f, 0.02f);
                MeshPartBuilder mo = mb.part("mouth", GL20.GL_TRIANGLES, ATTRS, mat);
                mo.setVertexTransform(new Matrix4().setToTranslation(0f, -0.08f, 0f));
                mo.box(0.14f, 0.04f, 0.02f);
                break;
            }
        }

        return mb.end();
    }

    /**
     * Build the separate models for a humanoid NPC with detailed body parts.
     */
    private Model[] buildHumanoidParts(Color shirtColor, Color trouserColor,
                                        Color skinColor, Color eyeColor, boolean hasHelmet) {
        int count = hasHelmet ? NUM_PARTS_WITH_HELMET : NUM_PARTS_NO_HELMET;
        Model[] parts = new Model[count];

        // Head and neck
        parts[PART_HEAD] = buildBox(HEAD_W, HEAD_H, HEAD_D, skinColor);
        parts[PART_NECK] = buildBox(NECK_W, NECK_H, NECK_D, skinColor);

        // Shoulders and torso (shirt coloured)
        parts[PART_SHOULDERS] = buildBox(SHOULDER_W, SHOULDER_H, SHOULDER_D, shirtColor);
        parts[PART_TORSO] = buildBox(TORSO_W, TORSO_H, TORSO_D, shirtColor);

        // Arms: upper arms in shirt colour, forearms in skin, hands in skin
        parts[PART_L_UPPER_ARM] = buildBox(UPPER_ARM_W, UPPER_ARM_H, UPPER_ARM_D, shirtColor);
        parts[PART_R_UPPER_ARM] = buildBox(UPPER_ARM_W, UPPER_ARM_H, UPPER_ARM_D, shirtColor);
        parts[PART_L_FOREARM] = buildBox(FOREARM_W, FOREARM_H, FOREARM_D, skinColor);
        parts[PART_R_FOREARM] = buildBox(FOREARM_W, FOREARM_H, FOREARM_D, skinColor);
        parts[PART_L_HAND] = buildBox(HAND_W, HAND_H, HAND_D, skinColor);
        parts[PART_R_HAND] = buildBox(HAND_W, HAND_H, HAND_D, skinColor);

        // Legs: upper legs and lower legs in trouser colour, feet darker
        parts[PART_L_UPPER_LEG] = buildBox(UPPER_LEG_W, UPPER_LEG_H, UPPER_LEG_D, trouserColor);
        parts[PART_R_UPPER_LEG] = buildBox(UPPER_LEG_W, UPPER_LEG_H, UPPER_LEG_D, trouserColor);
        Color shoeColor = new Color(trouserColor).lerp(Color.BLACK, 0.4f);
        parts[PART_L_LOWER_LEG] = buildBox(LOWER_LEG_W, LOWER_LEG_H, LOWER_LEG_D, trouserColor);
        parts[PART_R_LOWER_LEG] = buildBox(LOWER_LEG_W, LOWER_LEG_H, LOWER_LEG_D, trouserColor);
        parts[PART_L_FOOT] = buildBox(FOOT_W, FOOT_H, FOOT_D, shoeColor);
        parts[PART_R_FOOT] = buildBox(FOOT_W, FOOT_H, FOOT_D, shoeColor);

        // Face
        parts[PART_FACE] = buildFace(eyeColor);

        if (hasHelmet) {
            parts[PART_HELMET] = buildCustodianHelmet();
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
        ModelInstance[] inst = getOrCreateHumanoidInstances(npc);
        if (inst == null) return;

        // Knocked out: render NPC lying flat on the ground
        if (npc.getState() == NPCState.KNOCKED_OUT) {
            renderHumanoidKnockedOut(modelBatch, environment, npc, inst);
            return;
        }

        Vector3 pos = npc.getPosition();
        float yaw = npc.getFacingAngle();
        float animT = npc.getAnimTime();

        float swing = (float) Math.sin(animT * WALK_SPEED) * WALK_AMPLITUDE;
        float speed = npc.getVelocity().len();
        if (speed < 0.01f) swing = 0;
        float swingRad = (float) Math.toRadians(swing);
        float halfSwingRad = swingRad * 0.5f; // Forearms bend less

        // Vertical layout from ground up
        float footH = FOOT_H;
        float lowerLegTop = footH + LOWER_LEG_H;
        float upperLegTop = lowerLegTop + UPPER_LEG_H;
        float torsoBottom = upperLegTop;
        float torsoTop = torsoBottom + TORSO_H;
        float shoulderY = torsoTop;
        float neckY = shoulderY + SHOULDER_H;
        float headCentre = neckY + NECK_H + HEAD_H / 2f;
        float torsoCentre = torsoBottom + TORSO_H / 2f;

        float yawRad = (float) Math.toRadians(yaw);

        // Torso (static)
        setPartTransform(inst[PART_TORSO], pos, yawRad, 0f, torsoCentre, 0f);
        modelBatch.render(inst[PART_TORSO], environment);

        // Shoulders (static, wider than torso)
        setPartTransform(inst[PART_SHOULDERS], pos, yawRad, 0f, shoulderY, 0f);
        modelBatch.render(inst[PART_SHOULDERS], environment);

        // Neck (static)
        setPartTransform(inst[PART_NECK], pos, yawRad, 0f, neckY + NECK_H / 2f, 0f);
        modelBatch.render(inst[PART_NECK], environment);

        // Head (static)
        setPartTransform(inst[PART_HEAD], pos, yawRad, 0f, headCentre, 0f);
        modelBatch.render(inst[PART_HEAD], environment);

        // Face (front of head at +Z) — select model based on current expression
        ModelInstance[] exprInst = getOrCreateExpressionFaceInstances(npc);
        if (exprInst != null) {
            int exprIdx = npc.getFacialExpression().ordinal();
            setPartTransform(exprInst[exprIdx], pos, yawRad, 0f, headCentre, (HEAD_D / 2f + 0.011f));
            modelBatch.render(exprInst[exprIdx], environment);
        } else {
            setPartTransform(inst[PART_FACE], pos, yawRad, 0f, headCentre, (HEAD_D / 2f + 0.011f));
            modelBatch.render(inst[PART_FACE], environment);
        }

        // Helmet (police only)
        if (inst.length > PART_HELMET) {
            float helmetY = headCentre + HEAD_H / 2f + 0.02f;
            setPartTransform(inst[PART_HELMET], pos, yawRad, 0f, helmetY, 0f);
            modelBatch.render(inst[PART_HELMET], environment);
        }

        // Arms — upper arms swing from shoulder, forearms and hands follow
        float armOffsetX = SHOULDER_W / 2f;
        float armPivotY = shoulderY;

        // Left arm chain (swings with +swingRad for natural gait: left arm forward when right leg forward)
        setLimbTransform(inst[PART_L_UPPER_ARM], pos, yawRad,
            -armOffsetX, armPivotY, 0f, swingRad, UPPER_ARM_H);
        modelBatch.render(inst[PART_L_UPPER_ARM], environment);
        setLimbChainTransform(inst[PART_L_FOREARM], pos, yawRad,
            -armOffsetX, armPivotY, 0f, swingRad, UPPER_ARM_H, halfSwingRad, FOREARM_H);
        modelBatch.render(inst[PART_L_FOREARM], environment);
        setLimb3ChainTransform(inst[PART_L_HAND], pos, yawRad,
            -armOffsetX, armPivotY, 0f, swingRad, UPPER_ARM_H, halfSwingRad, FOREARM_H, -halfSwingRad, HAND_H);
        modelBatch.render(inst[PART_L_HAND], environment);

        // Right arm chain
        setLimbTransform(inst[PART_R_UPPER_ARM], pos, yawRad,
            armOffsetX, armPivotY, 0f, -swingRad, UPPER_ARM_H);
        modelBatch.render(inst[PART_R_UPPER_ARM], environment);
        setLimbChainTransform(inst[PART_R_FOREARM], pos, yawRad,
            armOffsetX, armPivotY, 0f, -swingRad, UPPER_ARM_H, -halfSwingRad, FOREARM_H);
        modelBatch.render(inst[PART_R_FOREARM], environment);
        setLimb3ChainTransform(inst[PART_R_HAND], pos, yawRad,
            armOffsetX, armPivotY, 0f, -swingRad, UPPER_ARM_H, -halfSwingRad, FOREARM_H, halfSwingRad, HAND_H);
        modelBatch.render(inst[PART_R_HAND], environment);

        // Legs — upper legs swing from hip, lower legs and feet follow
        float legOffsetX = TORSO_W / 2f - UPPER_LEG_W / 2f;
        float legPivotY = torsoBottom;

        // Left leg chain
        setLimbTransform(inst[PART_L_UPPER_LEG], pos, yawRad,
            -legOffsetX, legPivotY, 0f, -swingRad, UPPER_LEG_H);
        modelBatch.render(inst[PART_L_UPPER_LEG], environment);
        setLimbChainTransform(inst[PART_L_LOWER_LEG], pos, yawRad,
            -legOffsetX, legPivotY, 0f, -swingRad, UPPER_LEG_H, -halfSwingRad, LOWER_LEG_H);
        modelBatch.render(inst[PART_L_LOWER_LEG], environment);
        setLimb3ChainTransform(inst[PART_L_FOOT], pos, yawRad,
            -legOffsetX, legPivotY, 0f, -swingRad, UPPER_LEG_H, -halfSwingRad, LOWER_LEG_H, halfSwingRad, FOOT_H);
        modelBatch.render(inst[PART_L_FOOT], environment);

        // Right leg chain
        setLimbTransform(inst[PART_R_UPPER_LEG], pos, yawRad,
            legOffsetX, legPivotY, 0f, swingRad, UPPER_LEG_H);
        modelBatch.render(inst[PART_R_UPPER_LEG], environment);
        setLimbChainTransform(inst[PART_R_LOWER_LEG], pos, yawRad,
            legOffsetX, legPivotY, 0f, swingRad, UPPER_LEG_H, halfSwingRad, LOWER_LEG_H);
        modelBatch.render(inst[PART_R_LOWER_LEG], environment);
        setLimb3ChainTransform(inst[PART_R_FOOT], pos, yawRad,
            legOffsetX, legPivotY, 0f, swingRad, UPPER_LEG_H, halfSwingRad, LOWER_LEG_H, -halfSwingRad, FOOT_H);
        modelBatch.render(inst[PART_R_FOOT], environment);
    }

    /**
     * Render a humanoid NPC in the knocked out (lying flat on ground) pose.
     * All body parts are rotated 90 degrees around the X axis so the NPC
     * appears to have fallen forward flat on the ground.
     */
    private void renderHumanoidKnockedOut(ModelBatch modelBatch, Environment environment,
                                           NPC npc, ModelInstance[] inst) {
        Vector3 pos = npc.getPosition();
        float yaw = npc.getFacingAngle();
        float yawRad = (float) Math.toRadians(yaw);

        // Lie flat: pitch the NPC 90 degrees forward around X axis.
        // The body pivots around the hip so the torso/head extend forward along Z.
        float pitchRad = (float) Math.toRadians(90f);

        // Centre of the lying body — slightly above ground to avoid z-fighting
        float groundY = 0.05f;

        // Torso lies flat; centre is at half-torso-height along Z (now horizontal)
        setKnockedOutPartTransform(inst[PART_TORSO],    pos, yawRad, pitchRad, 0f,   groundY + TORSO_W / 2f,   TORSO_H / 2f);
        modelBatch.render(inst[PART_TORSO], environment);

        setKnockedOutPartTransform(inst[PART_SHOULDERS], pos, yawRad, pitchRad, 0f,  groundY + SHOULDER_W / 2f, TORSO_H + SHOULDER_H / 2f);
        modelBatch.render(inst[PART_SHOULDERS], environment);

        setKnockedOutPartTransform(inst[PART_NECK],     pos, yawRad, pitchRad, 0f,   groundY + NECK_W / 2f,   TORSO_H + SHOULDER_H + NECK_H / 2f);
        modelBatch.render(inst[PART_NECK], environment);

        setKnockedOutPartTransform(inst[PART_HEAD],     pos, yawRad, pitchRad, 0f,   groundY + HEAD_W / 2f,   TORSO_H + SHOULDER_H + NECK_H + HEAD_H / 2f);
        modelBatch.render(inst[PART_HEAD], environment);

        ModelInstance[] exprInstKO = getOrCreateExpressionFaceInstances(npc);
        if (exprInstKO != null) {
            int exprIdxKO = npc.getFacialExpression().ordinal();
            setKnockedOutPartTransform(exprInstKO[exprIdxKO], pos, yawRad, pitchRad, 0f, groundY + HEAD_D / 2f + 0.011f, TORSO_H + SHOULDER_H + NECK_H + HEAD_H / 2f);
            modelBatch.render(exprInstKO[exprIdxKO], environment);
        } else {
            setKnockedOutPartTransform(inst[PART_FACE], pos, yawRad, pitchRad, 0f, groundY + HEAD_D / 2f + 0.011f, TORSO_H + SHOULDER_H + NECK_H + HEAD_H / 2f);
            modelBatch.render(inst[PART_FACE], environment);
        }

        if (inst.length > PART_HELMET) {
            setKnockedOutPartTransform(inst[PART_HELMET], pos, yawRad, pitchRad, 0f, groundY + HEAD_W / 2f + 0.02f, TORSO_H + SHOULDER_H + NECK_H + HEAD_H + 0.02f);
            modelBatch.render(inst[PART_HELMET], environment);
        }

        // Arms hang flat beside the torso
        float armOffsetX = SHOULDER_W / 2f;
        setKnockedOutPartTransform(inst[PART_L_UPPER_ARM], pos, yawRad, pitchRad, -armOffsetX, groundY + UPPER_ARM_W / 2f, TORSO_H + UPPER_ARM_H / 2f);
        modelBatch.render(inst[PART_L_UPPER_ARM], environment);
        setKnockedOutPartTransform(inst[PART_L_FOREARM],   pos, yawRad, pitchRad, -armOffsetX, groundY + FOREARM_W / 2f,   TORSO_H + UPPER_ARM_H + FOREARM_H / 2f);
        modelBatch.render(inst[PART_L_FOREARM], environment);
        setKnockedOutPartTransform(inst[PART_L_HAND],      pos, yawRad, pitchRad, -armOffsetX, groundY + HAND_W / 2f,      TORSO_H + UPPER_ARM_H + FOREARM_H + HAND_H / 2f);
        modelBatch.render(inst[PART_L_HAND], environment);

        setKnockedOutPartTransform(inst[PART_R_UPPER_ARM], pos, yawRad, pitchRad,  armOffsetX, groundY + UPPER_ARM_W / 2f, TORSO_H + UPPER_ARM_H / 2f);
        modelBatch.render(inst[PART_R_UPPER_ARM], environment);
        setKnockedOutPartTransform(inst[PART_R_FOREARM],   pos, yawRad, pitchRad,  armOffsetX, groundY + FOREARM_W / 2f,   TORSO_H + UPPER_ARM_H + FOREARM_H / 2f);
        modelBatch.render(inst[PART_R_FOREARM], environment);
        setKnockedOutPartTransform(inst[PART_R_HAND],      pos, yawRad, pitchRad,  armOffsetX, groundY + HAND_W / 2f,      TORSO_H + UPPER_ARM_H + FOREARM_H + HAND_H / 2f);
        modelBatch.render(inst[PART_R_HAND], environment);

        // Legs extend behind (toward -Z in local space, now upward when lying flat)
        float legOffsetX = TORSO_W / 2f - UPPER_LEG_W / 2f;
        setKnockedOutPartTransform(inst[PART_L_UPPER_LEG], pos, yawRad, pitchRad, -legOffsetX, groundY + UPPER_LEG_W / 2f, -(UPPER_LEG_H / 2f));
        modelBatch.render(inst[PART_L_UPPER_LEG], environment);
        setKnockedOutPartTransform(inst[PART_L_LOWER_LEG], pos, yawRad, pitchRad, -legOffsetX, groundY + LOWER_LEG_W / 2f, -(UPPER_LEG_H + LOWER_LEG_H / 2f));
        modelBatch.render(inst[PART_L_LOWER_LEG], environment);
        setKnockedOutPartTransform(inst[PART_L_FOOT],      pos, yawRad, pitchRad, -legOffsetX, groundY + FOOT_H / 2f,      -(UPPER_LEG_H + LOWER_LEG_H + FOOT_H / 2f));
        modelBatch.render(inst[PART_L_FOOT], environment);

        setKnockedOutPartTransform(inst[PART_R_UPPER_LEG], pos, yawRad, pitchRad,  legOffsetX, groundY + UPPER_LEG_W / 2f, -(UPPER_LEG_H / 2f));
        modelBatch.render(inst[PART_R_UPPER_LEG], environment);
        setKnockedOutPartTransform(inst[PART_R_LOWER_LEG], pos, yawRad, pitchRad,  legOffsetX, groundY + LOWER_LEG_W / 2f, -(UPPER_LEG_H + LOWER_LEG_H / 2f));
        modelBatch.render(inst[PART_R_LOWER_LEG], environment);
        setKnockedOutPartTransform(inst[PART_R_FOOT],      pos, yawRad, pitchRad,  legOffsetX, groundY + FOOT_H / 2f,      -(UPPER_LEG_H + LOWER_LEG_H + FOOT_H / 2f));
        modelBatch.render(inst[PART_R_FOOT], environment);
    }

    /**
     * Set transform for a body part in the knocked out (lying flat) pose.
     * The NPC is rotated 90 degrees around the X axis relative to its yaw.
     * localX/localY/localZ are in the NPC's lying-flat local space:
     *   X = left/right, Y = up from ground, Z = along body (+ = head direction)
     */
    private void setKnockedOutPartTransform(ModelInstance instance, Vector3 npcPos, float yawRad,
                                             float pitchRad, float localX, float localY, float localZ) {
        float cosY = (float) Math.cos(yawRad);
        float sinY = (float) Math.sin(yawRad);

        // Rotate local position by yaw around Y axis
        float rotX = localX * cosY + localZ * sinY;
        float rotZ = -localX * sinY + localZ * cosY;

        float worldX = npcPos.x + rotX;
        float worldY = npcPos.y + localY;
        float worldZ = npcPos.z + rotZ;

        tmpTransform.idt();
        tmpTransform.setToTranslation(worldX, worldY, worldZ);
        tmpTransform.rotate(Vector3.Y, (float) Math.toDegrees(yawRad));
        tmpTransform.rotate(Vector3.X, (float) Math.toDegrees(pitchRad));

        instance.transform.set(tmpTransform);
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
     * Set transform for a child limb segment chained to a parent limb.
     * The child hangs from the end of the parent, adding its own swing rotation.
     */
    private void setLimbChainTransform(ModelInstance instance, Vector3 npcPos, float yawRad,
                                        float localX, float pivotY, float localZ,
                                        float parentSwingRad, float parentLength,
                                        float childSwingRad, float childLength) {
        float cosY = (float) Math.cos(yawRad);
        float sinY = (float) Math.sin(yawRad);

        float pivotWorldX = npcPos.x + localX * cosY + localZ * sinY;
        float pivotWorldY = npcPos.y + pivotY;
        float pivotWorldZ = npcPos.z - localX * sinY + localZ * cosY;

        tmpTransform.idt();
        tmpTransform.setToTranslation(pivotWorldX, pivotWorldY, pivotWorldZ);
        tmpTransform.rotate(Vector3.Y, (float) Math.toDegrees(yawRad));
        // Apply parent swing first to reach end of parent limb
        tmpTransform.rotate(Vector3.X, (float) Math.toDegrees(parentSwingRad));
        tmpTransform.translate(0, -parentLength, 0);
        // Apply child's own swing
        tmpTransform.rotate(Vector3.X, (float) Math.toDegrees(childSwingRad));
        tmpTransform.translate(0, -childLength / 2f, 0);

        instance.transform.set(tmpTransform);
    }

    /**
     * Set transform for a 3-segment limb chain (e.g. upper arm → forearm → hand).
     * Properly applies each joint's rotation in sequence so the third segment is
     * positioned at the true end of the second segment after both rotations.
     */
    private void setLimb3ChainTransform(ModelInstance instance, Vector3 npcPos, float yawRad,
                                         float localX, float pivotY, float localZ,
                                         float seg1SwingRad, float seg1Length,
                                         float seg2SwingRad, float seg2Length,
                                         float seg3SwingRad, float seg3Length) {
        float cosY = (float) Math.cos(yawRad);
        float sinY = (float) Math.sin(yawRad);

        float pivotWorldX = npcPos.x + localX * cosY + localZ * sinY;
        float pivotWorldY = npcPos.y + pivotY;
        float pivotWorldZ = npcPos.z - localX * sinY + localZ * cosY;

        tmpTransform.idt();
        tmpTransform.setToTranslation(pivotWorldX, pivotWorldY, pivotWorldZ);
        tmpTransform.rotate(Vector3.Y, (float) Math.toDegrees(yawRad));
        // Segment 1 (e.g. upper arm) swings from root pivot
        tmpTransform.rotate(Vector3.X, (float) Math.toDegrees(seg1SwingRad));
        tmpTransform.translate(0, -seg1Length, 0);
        // Segment 2 (e.g. forearm) swings from end of segment 1
        tmpTransform.rotate(Vector3.X, (float) Math.toDegrees(seg2SwingRad));
        tmpTransform.translate(0, -seg2Length, 0);
        // Segment 3 (e.g. hand) swings from end of segment 2
        tmpTransform.rotate(Vector3.X, (float) Math.toDegrees(seg3SwingRad));
        tmpTransform.translate(0, -seg3Length / 2f, 0);

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

    /**
     * Render a dog NPC in the knocked out (lying on side) pose.
     */
    private void renderDogKnockedOut(ModelBatch modelBatch, Environment environment,
                                      NPC npc, ModelInstance[] inst) {
        Vector3 pos = npc.getPosition();
        float yaw = npc.getFacingAngle();
        float yawRad = (float) Math.toRadians(yaw);

        float bodyH = 0.30f;
        float legH = 0.30f;

        // Roll 90 degrees to the side — dog lies on its left side
        float rollRad = (float) Math.toRadians(90f);

        // Body centre: body lies on its side at ground level
        float groundY = bodyH / 2f + 0.05f;

        // Body
        setKnockedOutPartTransform(inst[0], pos, yawRad, rollRad, 0f, groundY, 0f);
        modelBatch.render(inst[0], environment);

        // Head (front of body)
        float headZ = (0.65f / 2f + 0.26f / 2f - 0.04f);
        setKnockedOutPartTransform(inst[1], pos, yawRad, rollRad, 0f, groundY + 0.06f, headZ);
        modelBatch.render(inst[1], environment);

        // Snout
        setKnockedOutPartTransform(inst[2], pos, yawRad, rollRad, 0f, groundY, headZ + 0.26f / 2f + 0.06f);
        modelBatch.render(inst[2], environment);

        // Eyes
        setKnockedOutPartTransform(inst[8], pos, yawRad, rollRad, 0f, groundY + 0.10f, headZ + 0.26f / 2f + 0.011f);
        modelBatch.render(inst[8], environment);

        // Tail (back of body)
        float tailZ = -(0.65f / 2f + 0.03f);
        setKnockedOutPartTransform(inst[3], pos, yawRad, rollRad, 0f, groundY + 0.15f, tailZ);
        modelBatch.render(inst[3], environment);

        // Legs (all pointing up/sideways when lying)
        float frontLegZ = 0.65f / 2f - 0.10f;
        float backLegZ = -(0.65f / 2f - 0.10f);
        float legSpreadX = 0.12f;
        setKnockedOutPartTransform(inst[4], pos, yawRad, rollRad, -legSpreadX, groundY, frontLegZ);
        modelBatch.render(inst[4], environment);
        setKnockedOutPartTransform(inst[5], pos, yawRad, rollRad,  legSpreadX, groundY, frontLegZ);
        modelBatch.render(inst[5], environment);
        setKnockedOutPartTransform(inst[6], pos, yawRad, rollRad, -legSpreadX, groundY, backLegZ);
        modelBatch.render(inst[6], environment);
        setKnockedOutPartTransform(inst[7], pos, yawRad, rollRad,  legSpreadX, groundY, backLegZ);
        modelBatch.render(inst[7], environment);
    }

    private void renderDog(ModelBatch modelBatch, Environment environment, NPC npc) {
        ModelInstance[] inst = getOrCreateDogInstances(npc);
        if (inst == null) return;

        // Knocked out: dog lies on its side
        if (npc.getState() == NPCState.KNOCKED_OUT) {
            renderDogKnockedOut(modelBatch, environment, npc, inst);
            return;
        }

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
        setPartTransform(inst[0], pos, yawRad, 0f, bodyCentreY, 0f);
        modelBatch.render(inst[0], environment);

        // 1 - Head (at +Z, the front of the dog)
        float headZ = (0.65f / 2f + 0.26f / 2f - 0.04f);
        setPartTransform(inst[1], pos, yawRad, 0f, bodyCentreY + 0.06f, headZ);
        modelBatch.render(inst[1], environment);

        // 2 - Snout
        setPartTransform(inst[2], pos, yawRad,
            0f, bodyCentreY, headZ + 0.26f / 2f + 0.06f);
        modelBatch.render(inst[2], environment);

        // 8 - Eyes
        setPartTransform(inst[8], pos, yawRad,
            0f, bodyCentreY + 0.10f, headZ + 0.26f / 2f + 0.011f);
        modelBatch.render(inst[8], environment);

        // 3 - Tail (at -Z, the back of the dog)
        float tailZ = -(0.65f / 2f + 0.03f);
        setPartTransform(inst[3], pos, yawRad, 0f, bodyCentreY + 0.15f, tailZ);
        modelBatch.render(inst[3], environment);

        // 4-7: Legs with walk animation (front at +Z, back at -Z)
        float frontLegZ = 0.65f / 2f - 0.10f;
        float backLegZ = -(0.65f / 2f - 0.10f);
        float legPivotY = bodyCentreY - bodyH / 2f;
        float legSpreadX = 0.12f;

        setLimbTransform(inst[4], pos, yawRad, -legSpreadX, legPivotY, frontLegZ, swingRad, legH);
        modelBatch.render(inst[4], environment);
        setLimbTransform(inst[5], pos, yawRad, legSpreadX, legPivotY, frontLegZ, -swingRad, legH);
        modelBatch.render(inst[5], environment);
        setLimbTransform(inst[6], pos, yawRad, -legSpreadX, legPivotY, backLegZ, -swingRad, legH);
        modelBatch.render(inst[6], environment);
        setLimbTransform(inst[7], pos, yawRad, legSpreadX, legPivotY, backLegZ, swingRad, legH);
        modelBatch.render(inst[7], environment);
    }

    public void dispose() {
        for (Model[] parts : humanoidParts.values()) {
            for (Model m : parts) {
                if (m != null) m.dispose();
            }
        }
        humanoidParts.clear();
        humanoidInstances.clear();

        for (Model[] faces : expressionFaceParts.values()) {
            for (Model m : faces) {
                if (m != null) m.dispose();
            }
        }
        expressionFaceParts.clear();
        expressionFaceInstances.clear();

        for (Model[] parts : dogParts.values()) {
            for (Model m : parts) {
                if (m != null) m.dispose();
            }
        }
        dogParts.clear();
        dogInstances.clear();
    }
}

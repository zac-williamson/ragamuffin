package ragamuffin.entity;

/**
 * Physical appearance variants for NPC 3D models.
 * Each variant defines scale factors that the renderer applies to the
 * body-part transforms, giving NPCs different statures and visual features.
 *
 * heightScale — multiplier applied to all vertical offsets (leg/torso/neck/head heights).
 * widthScale  — multiplier applied to all horizontal offsets (arm/leg separation).
 * headScale   — multiplier applied to head size (rendered via transform scale).
 * hasLongHair — if true the renderer adds a hair-block behind the head.
 */
public enum NPCModelVariant {
    /** Standard proportions — the baseline for all NPC types. */
    DEFAULT(1.0f, 1.0f, 1.0f, false),

    /** Taller than average — longer legs and torso. */
    TALL(1.25f, 1.0f, 1.0f, false),

    /** Shorter than average — compressed legs and torso. */
    SHORT(0.78f, 1.0f, 0.92f, false),

    /** Broad, stocky build — wider stance and thicker torso. */
    STOCKY(0.90f, 1.20f, 1.05f, false),

    /** Slim, slender build — narrower stance. */
    SLIM(1.05f, 0.82f, 0.95f, false),

    /** Long flowing hair rendered as a block behind the head. */
    LONG_HAIR(1.0f, 1.0f, 1.0f, true);

    private final float heightScale;
    private final float widthScale;
    private final float headScale;
    private final boolean hasLongHair;

    NPCModelVariant(float heightScale, float widthScale, float headScale, boolean hasLongHair) {
        this.heightScale = heightScale;
        this.widthScale = widthScale;
        this.headScale = headScale;
        this.hasLongHair = hasLongHair;
    }

    /** Vertical scale factor applied to leg, torso, neck, and head heights. */
    public float getHeightScale() {
        return heightScale;
    }

    /** Horizontal scale factor applied to shoulder width and leg separation. */
    public float getWidthScale() {
        return widthScale;
    }

    /** Scale factor applied to head size. */
    public float getHeadScale() {
        return headScale;
    }

    /** Whether this variant includes a long-hair block behind the head. */
    public boolean hasLongHair() {
        return hasLongHair;
    }
}

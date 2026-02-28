package ragamuffin.entity;

/**
 * Facial hair variants for NPC visual variety (Issue #875).
 * Each value describes the style of facial hair rendered on the NPC's face.
 * The renderer uses this to add the appropriate facial hair geometry.
 */
public enum FacialHairType {
    /** No facial hair. */
    NONE,

    /** Light stubble — a thin dark rectangle across the lower face. */
    STUBBLE,

    /** Moustache — a block above the upper lip. */
    MOUSTACHE,

    /** Full beard — a wider, deeper block covering the chin and jaw. */
    BEARD,

    /** Goatee — a small neat tuft on the chin only. */
    GOATEE;
}

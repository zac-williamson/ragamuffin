package ragamuffin.entity;

/**
 * Hairstyle variants for NPC visual variety (Issue #875).
 * Each value describes the shape/length of hair rendered on the NPC's head.
 * The renderer uses this to pick or build the appropriate hair geometry.
 */
public enum HairstyleType {
    /** No hair rendered — bald NPC. */
    NONE,

    /** Short, neat cut — close to the scalp on sides and back, slightly longer on top. */
    SHORT,

    /** Long flowing hair — a wide block hanging behind and below the head. */
    LONG,

    /** Mohawk — a tall narrow strip of hair running front-to-back along the centre of the head. */
    MOHAWK,

    /** Curly / afro — a wide rounded puff of hair sitting atop the head. */
    CURLY,

    /** Buzzcut — very short, uniform length all over. */
    BUZZCUT;
}

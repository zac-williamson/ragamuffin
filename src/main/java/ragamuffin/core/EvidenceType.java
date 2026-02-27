package ragamuffin.core;

/**
 * Types of physical evidence props that can spawn at crime scenes.
 *
 * <p>Evidence props are placed in the world when a crime occurs. Each prop has a
 * decay timer after which it disappears. Police NPCs who discover an evidence prop
 * automatically add an entry to the player's {@link CriminalRecord}.
 *
 * <p>Issue #765: Witness &amp; Evidence System — 'The Word on the Street'
 */
public enum EvidenceType {

    /**
     * Broken glass — spawns when a GLASS block is destroyed.
     * Decay: 120 seconds.
     */
    SMASHED_GLASS("Smashed Glass", 120f),

    /**
     * Crowbar marks — spawns when a CROWBAR is used to break a block or safe.
     * Decay: 180 seconds.
     */
    CROWBAR_MARKS("Crowbar Marks", 180f),

    /**
     * Blood spatter — spawns when an NPC is knocked out or killed.
     * Decay: 90 seconds.
     */
    BLOOD_SPATTER("Blood Spatter", 90f),

    /**
     * Dropped loot — spawns when the player drops stolen items near a crime scene.
     * Decay: 60 seconds.
     */
    DROPPED_LOOT("Dropped Loot", 60f),

    /**
     * CCTV tape — a stealable prop found in office blocks and off-licences.
     * If a crime happens within 8 blocks and the tape is not stolen within
     * 180 seconds (3 in-game minutes), police automatically gain a WITNESSED_CRIME
     * record entry. Stealing the tape cancels this.
     * Decay: 180 seconds (the activation window).
     */
    CCTV_TAPE("CCTV Tape", 180f);

    private final String displayName;
    /** Seconds until this evidence prop decays and is removed from the world. */
    private final float decaySeconds;

    EvidenceType(String displayName, float decaySeconds) {
        this.displayName = displayName;
        this.decaySeconds = decaySeconds;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getDecaySeconds() {
        return decaySeconds;
    }
}

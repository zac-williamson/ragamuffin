package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;

/**
 * A physical evidence prop spawned at a crime scene.
 *
 * <p>Evidence props are placed in the world when crimes occur (block breaking,
 * NPC assaults, heist activity). Each prop decays over time according to its
 * {@link EvidenceType#getDecaySeconds()} value. Police NPCs who walk within
 * {@link #POLICE_DETECT_RADIUS} of an undiscovered prop will discover it and
 * add a {@link CriminalRecord.CrimeType#WITNESSED_CRIMES} entry to the player's
 * criminal record.
 *
 * <p>CCTV_TAPE props have special behaviour: if a crime occurs within
 * {@link #CCTV_ACTIVATION_RADIUS} blocks and the tape is not stolen by the player
 * within the decay window, police gain a WITNESSED_CRIMES entry automatically.
 *
 * <p>Issue #765: Witness &amp; Evidence System — 'The Word on the Street'
 */
public class EvidenceProp {

    /** Radius in blocks within which police NPCs detect this evidence. */
    public static final float POLICE_DETECT_RADIUS = 3f;

    /** Radius in blocks within which a crime event activates a CCTV_TAPE prop. */
    public static final float CCTV_ACTIVATION_RADIUS = 8f;

    private final EvidenceType type;
    private final Vector3 position;
    private float decayTimer;    // counts DOWN to 0
    private boolean discovered;  // true once police have detected it
    private boolean stolen;      // true if a CCTV_TAPE has been taken by the player
    private boolean active;      // CCTV: true once a nearby crime has occurred
    private boolean alive;       // false when decayed or collected

    /**
     * Create a new evidence prop at the given world position.
     *
     * @param type     the category of evidence
     * @param x        world X coordinate
     * @param y        world Y coordinate
     * @param z        world Z coordinate
     */
    public EvidenceProp(EvidenceType type, float x, float y, float z) {
        this.type = type;
        this.position = new Vector3(x, y, z);
        this.decayTimer = type.getDecaySeconds();
        this.discovered = false;
        this.stolen = false;
        this.alive = true;
        // CCTV tapes start inactive — they only begin their countdown when a
        // crime occurs nearby (flagged via activateCctv()).
        this.active = (type != EvidenceType.CCTV_TAPE);
    }

    /**
     * Advance the decay timer by delta seconds.
     * When the timer reaches zero the prop is marked dead.
     *
     * @param delta seconds since last update
     */
    public void update(float delta) {
        if (!alive) return;
        if (!active) return; // CCTV tape waiting for a nearby crime — don't decay yet

        decayTimer -= delta;
        if (decayTimer <= 0f) {
            alive = false;
        }
    }

    /**
     * Activate a CCTV_TAPE prop after a crime occurs nearby.
     * This starts the 3-minute steal window.
     */
    public void activateCctv() {
        if (type == EvidenceType.CCTV_TAPE && !active) {
            active = true;
        }
    }

    /**
     * Mark this prop as discovered by police.
     * The prop is then considered processed and will not trigger further entries.
     */
    public void discover() {
        this.discovered = true;
    }

    /**
     * Mark a CCTV_TAPE prop as stolen by the player.
     * This cancels any pending WITNESSED_CRIMES entry and removes the prop.
     */
    public void steal() {
        this.stolen = true;
        this.alive = false;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public EvidenceType getType() {
        return type;
    }

    public Vector3 getPosition() {
        return position;
    }

    /** Remaining seconds before this prop decays (0 if already dead). */
    public float getDecayTimer() {
        return Math.max(0f, decayTimer);
    }

    /** True if police have already processed/discovered this prop. */
    public boolean isDiscovered() {
        return discovered;
    }

    /** True if this was a CCTV_TAPE that the player has stolen. */
    public boolean isStolen() {
        return stolen;
    }

    /** True if this prop is active (decaying) and in the world. */
    public boolean isActive() {
        return active;
    }

    /** True if this prop is still in the world (not decayed or collected). */
    public boolean isAlive() {
        return alive;
    }
}

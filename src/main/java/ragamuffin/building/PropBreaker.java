package ragamuffin.building;

import ragamuffin.world.PropPosition;
import ragamuffin.world.PropType;
import ragamuffin.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Handles punching / breaking of non-block 3D props.
 *
 * Each prop is identified by its index in {@link World#getPropPositions()}.
 * Hit counts decay after {@link #PROP_REGEN_SECONDS} of inactivity, just like
 * {@link BlockBreaker} does for voxel blocks.
 *
 * Issue #719: Add collision and destructibility to 3D objects.
 */
public class PropBreaker {

    /** Seconds of inactivity before a partially-damaged prop fully regenerates. */
    static final float PROP_REGEN_SECONDS = 10f;

    /** Per-prop hit state. */
    private static final class HitRecord {
        int hits;
        long lastHitTime;

        HitRecord(int hits, long lastHitTime) {
            this.hits        = hits;
            this.lastHitTime = lastHitTime;
        }
    }

    /** Map from prop index (as String) → hit record. */
    private final Map<String, HitRecord> propHits;

    public PropBreaker() {
        this.propHits = new HashMap<>();
    }

    /**
     * Decay stale hit entries. Call once per frame.
     *
     * @param delta frame delta time in seconds (unused — decay is wall-clock based)
     */
    public void tickDecay(@SuppressWarnings("unused") float delta) {
        long now          = System.currentTimeMillis();
        long thresholdMs  = (long) (PROP_REGEN_SECONDS * 1000L);
        Iterator<Map.Entry<String, HitRecord>> it = propHits.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().lastHitTime > thresholdMs) {
                it.remove();
            }
        }
    }

    /**
     * Punch the prop at the given index.
     *
     * @param world     the game world (used to look up and remove props)
     * @param propIndex index into {@link World#getPropPositions()}
     * @return the material drop if the prop was broken on this punch, or
     *         {@code null} if it was not yet broken (or drops nothing)
     */
    public Material punchProp(World world, int propIndex) {
        if (propIndex < 0 || propIndex >= world.getPropPositions().size()) {
            return null;
        }

        PropPosition prop = world.getPropPositions().get(propIndex);
        PropType type     = prop.getType();

        String key         = String.valueOf(propIndex);
        HitRecord record   = propHits.get(key);
        int hits           = (record != null ? record.hits : 0) + 1;
        int hitsToBreak    = type.getHitsToBreak();

        if (hits >= hitsToBreak) {
            // Prop is broken — remove it from the world
            world.removeProp(propIndex);
            propHits.remove(key);
            // Shift indices: all entries with index > propIndex must be decremented
            shiftIndicesDown(propIndex);
            return type.getMaterialDrop();
        } else {
            propHits.put(key, new HitRecord(hits, System.currentTimeMillis()));
            return null;
        }
    }

    /**
     * Get the break progress for a prop as a fraction (0.0 to 1.0).
     *
     * @param world     the game world
     * @param propIndex index into {@link World#getPropPositions()}
     */
    public float getBreakProgress(World world, int propIndex) {
        if (propIndex < 0 || propIndex >= world.getPropPositions().size()) {
            return 0f;
        }
        PropPosition prop  = world.getPropPositions().get(propIndex);
        int hitsToBreak    = prop.getType().getHitsToBreak();
        String key         = String.valueOf(propIndex);
        HitRecord record   = propHits.get(key);
        int hits           = record != null ? record.hits : 0;
        return (float) hits / hitsToBreak;
    }

    /**
     * Get the current hit count for a prop.
     */
    public int getHitCount(int propIndex) {
        HitRecord record = propHits.get(String.valueOf(propIndex));
        return record != null ? record.hits : 0;
    }

    /**
     * Clear all hit state (for testing / world reset).
     */
    public void resetHits() {
        propHits.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * When a prop at {@code removedIndex} is deleted, all stored indices greater
     * than it must be decremented by 1 so they still point at the correct props.
     */
    private void shiftIndicesDown(int removedIndex) {
        Map<String, HitRecord> shifted = new HashMap<>();
        for (Map.Entry<String, HitRecord> entry : propHits.entrySet()) {
            int idx = Integer.parseInt(entry.getKey());
            if (idx > removedIndex) {
                shifted.put(String.valueOf(idx - 1), entry.getValue());
            } else {
                shifted.put(entry.getKey(), entry.getValue());
            }
        }
        propHits.clear();
        propHits.putAll(shifted);
    }
}

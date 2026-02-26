package ragamuffin.core;

import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Tracks furnishing props and blocks placed inside the player's squat.
 *
 * <p>Maintains counts of each prop type and a list of CARPET block placements
 * (each +2 Vibe). Also maintains a CAMPFIRE counter (+8 Vibe each).
 *
 * <p>Used by {@link SquatSystem} to manage Vibe changes when furnishings
 * are added or removed, and to resolve which prop is destroyed during a raid.
 */
public class SquatFurnishingTracker {

    /** Count of each prop type currently placed in the squat. */
    private final Map<PropType, Integer> propCounts = new EnumMap<>(PropType.class);

    /** Number of CARPET blocks placed. */
    private int carpetCount;

    /** Number of CAMPFIRE blocks placed. */
    private int campfireCount;

    /**
     * Add a prop to the tracker.
     *
     * @param prop the prop type placed
     */
    public void addProp(PropType prop) {
        propCounts.merge(prop, 1, Integer::sum);
    }

    /**
     * Remove a prop from the tracker. No-op if count is already 0.
     *
     * @param prop the prop type removed
     * @return true if removed, false if the prop was not tracked
     */
    public boolean removeProp(PropType prop) {
        Integer count = propCounts.get(prop);
        if (count == null || count <= 0) return false;
        if (count == 1) {
            propCounts.remove(prop);
        } else {
            propCounts.put(prop, count - 1);
        }
        return true;
    }

    /** Increment the CARPET block count. */
    public void addCarpet() {
        carpetCount++;
    }

    /** Remove a CARPET block. */
    public boolean removeCarpet() {
        if (carpetCount <= 0) return false;
        carpetCount--;
        return true;
    }

    /** Increment the CAMPFIRE block count. */
    public void addCampfire() {
        campfireCount++;
    }

    /** Remove a CAMPFIRE block. */
    public boolean removeCampfire() {
        if (campfireCount <= 0) return false;
        campfireCount--;
        return true;
    }

    /**
     * Returns the count of the given prop type in the squat.
     */
    public int getCount(PropType prop) {
        return propCounts.getOrDefault(prop, 0);
    }

    /** Returns the number of CARPET blocks placed. */
    public int getCarpetCount() {
        return carpetCount;
    }

    /** Returns the number of CAMPFIRE blocks placed. */
    public int getCampfireCount() {
        return campfireCount;
    }

    /** Returns true if any furnishings have been placed. */
    public boolean hasFurnishings() {
        return !propCounts.isEmpty() || carpetCount > 0 || campfireCount > 0;
    }

    /**
     * Destroy a random prop from the squat (simulating raid damage).
     * Priority: highest-Vibe prop is destroyed first.
     *
     * @param random for tie-breaking
     * @return the destroyed prop type, or null if no props to destroy
     */
    public PropType destroyRandomProp(Random random) {
        // Build a priority list: higher Vibe prop first
        // BED(10) > CAMPFIRE_BLOCK(8) > SQUAT_DARTBOARD(7) > CARPET(2) > WORKBENCH(0)
        List<PropType> candidates = new ArrayList<>();

        for (Map.Entry<PropType, Integer> entry : propCounts.entrySet()) {
            if (entry.getValue() > 0) {
                candidates.add(entry.getKey());
            }
        }

        // Also consider carpet and campfire
        if (campfireCount > 0) candidates.add(null); // represent campfire as null for special handling

        if (candidates.isEmpty() && carpetCount == 0) {
            return null; // nothing to destroy
        }

        // Prefer highest-Vibe prop
        // We'll use a simple priority: BED, SQUAT_DARTBOARD, then others
        for (PropType priority : new PropType[]{PropType.BED, PropType.SQUAT_DARTBOARD}) {
            if (propCounts.getOrDefault(priority, 0) > 0) {
                removeProp(priority);
                return priority;
            }
        }

        // Campfire block (represented specially)
        if (campfireCount > 0) {
            campfireCount--;
            return null; // caller interprets null as "campfire"
        }

        // Carpet
        if (carpetCount > 0) {
            carpetCount--;
            return null;
        }

        // Any remaining prop
        if (!candidates.isEmpty()) {
            PropType chosen = candidates.get(random.nextInt(candidates.size()));
            if (chosen != null) removeProp(chosen);
            return chosen;
        }

        return null;
    }

    /** Returns the full prop count map (unmodifiable view). */
    public Map<PropType, Integer> getPropCounts() {
        return java.util.Collections.unmodifiableMap(propCounts);
    }
}

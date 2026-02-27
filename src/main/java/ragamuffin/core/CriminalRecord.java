package ragamuffin.core;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Tracks the player's criminal activity statistics across a session.
 *
 * <p>Records specific crime counts (e.g. pensioners punched, blocks destroyed,
 * shops raided) that the player has committed.  Displayed via {@link ragamuffin.ui.CriminalRecordUI}.
 */
public class CriminalRecord {

    /**
     * The categories of crime tracked by the record.
     * Add new entries here to extend the system without touching UI code.
     */
    public enum CrimeType {
        PENSIONERS_PUNCHED("Pensioners punched"),
        MEMBERS_OF_PUBLIC_PUNCHED("Members of public punched"),
        BLOCKS_DESTROYED("Blocks destroyed"),
        TIMES_ARRESTED("Times arrested"),
        SHOPS_RAIDED("Shops raided"),
        NPCS_KILLED("NPCs killed"),
        /** Issue #765: Added by police who find evidence or receive a WITNESS_SIGHTING rumour. */
        WITNESSED_CRIMES("Witnessed crimes on record"),

        /** Issue #774: Each time the player appears on the front page of The Daily Ragamuffin. */
        PRESS_INFAMY("Front-page appearances");

        private final String displayName;

        CrimeType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final Map<CrimeType, Integer> counts;

    public CriminalRecord() {
        counts = new EnumMap<>(CrimeType.class);
        for (CrimeType type : CrimeType.values()) {
            counts.put(type, 0);
        }
    }

    /**
     * Record that one instance of the given crime was committed.
     *
     * @param type  the category of crime
     */
    public void record(CrimeType type) {
        counts.put(type, counts.get(type) + 1);
    }

    /**
     * Get the count for a specific crime category.
     *
     * @param type  the crime category
     * @return number of times that crime has been committed (>= 0)
     */
    public int getCount(CrimeType type) {
        return counts.getOrDefault(type, 0);
    }

    /**
     * Total crimes committed across all categories.
     */
    public int getTotalCrimes() {
        int total = 0;
        for (int v : counts.values()) {
            total += v;
        }
        return total;
    }

    /**
     * Read-only view of the crime counts map.
     */
    public Map<CrimeType, Integer> getCounts() {
        return Collections.unmodifiableMap(counts);
    }

    /**
     * Decrement the count for a specific crime category by 1 (minimum 0).
     * Used by the informant mechanic (grassing) to clear one witnessed crime entry.
     *
     * @param type the crime category to decrement
     */
    public void clearOne(CrimeType type) {
        int current = counts.getOrDefault(type, 0);
        if (current > 0) {
            counts.put(type, current - 1);
        }
    }

    /**
     * Reset all crime counts — called on new game start.
     */
    public void reset() {
        for (CrimeType type : CrimeType.values()) {
            counts.put(type, 0);
        }
    }
}

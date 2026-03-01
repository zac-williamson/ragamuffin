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
        PRESS_INFAMY("Front-page appearances"),

        /**
         * Issue #781: Logged after 3 graffiti arrests. Triggers a solicitor quest
         * to secure a not-guilty plea.
         */
        CRIMINAL_DAMAGE("Criminal damage charges"),

        /**
         * Issue #797: Recorded by Watch Members during a soft citizen's arrest.
         * Two counts added per citizen's arrest at Tier 3+.
         */
        ANTISOCIAL_BEHAVIOUR("Antisocial behaviour charges"),

        /**
         * Issue #906: Recorded when police confiscate the player's BUCKET_DRUM
         * during a busking licence check.
         */
        UNLICENSED_BUSKING("Unlicensed busking"),

        /**
         * Issue #914: Recorded when the player enters the allotments outside warden
         * open hours (07:00–19:00).
         */
        TRESPASSING("Trespassing charges"),

        /**
         * Issue #918: Recorded when the player boards The Number 47 without paying
         * the fare and without a valid BUS_PASS.
         */
        FARE_EVASION("Fare evasion"),

        /**
         * Issue #920: Recorded when the player is caught by a police raid during
         * the pub lock-in after-hours session and fails to hide behind the bar counter.
         */
        DRUNK_AND_DISORDERLY("Drunk and disorderly charges"),

        /**
         * Issue #946: Recorded when a POLICE NPC inspects the player's off-lead dog
         * with Notoriety ≥ 50, or when the dog is used for intimidation and a police
         * NPC is within 15 blocks.
         */
        DANGEROUS_DOG("Dangerous dog offences"),

        /**
         * Issue #948: Recorded each time the player completes a full 3-minute shift
         * at the Sparkle Hand Car Wash. 3+ entries reduce the arrest fine by 20%
         * (arresting officer gives benefit of the doubt for legitimate employment).
         */
        LEGITIMATE_WORK("Legitimate employment (car wash shifts)"),

        /**
         * Issue #954: Recorded when the player is caught using a RIGGED_BINGO_CARD
         * at Lucky Stars Bingo Hall and ejected by the CALLER NPC.
         */
        BINGO_CHEATING("Bingo cheating offences"),

        /**
         * Issue #961: Recorded by WitnessSystem when the player sells a stolen item
         * at the pawn shop while a POLICE NPC is within 8 blocks.
         */
        HANDLING_STOLEN_GOODS("Handling stolen goods"),

        /**
         * Issue #969: Recorded when the GROUNDSKEEPER NPC witnesses the player
         * digging a grave plot in Northfield Cemetery (+2 Notoriety per witnessed dig).
         */
        GRAVE_ROBBING("Grave robbing"),

        /**
         * Issue #969: Recorded when the player attacks a MOURNER or FUNERAL_DIRECTOR
         * during a funeral procession.
         */
        DISTURBING_THE_PEACE("Disturbing the peace");

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

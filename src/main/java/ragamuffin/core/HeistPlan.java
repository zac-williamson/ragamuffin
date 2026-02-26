package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores casing data gathered during the Casing phase of a heist (Phase O / Issue #704).
 *
 * <p>Created when the player presses F inside a heistable building and persists
 * until the heist is executed or the in-game day resets at 06:00.
 *
 * <p>Contains:
 * <ul>
 *   <li>The target landmark type</li>
 *   <li>Positions of observed alarm boxes, CCTV cameras, and safes</li>
 *   <li>Whether each guard's patrol route has been observed (stood within 6 blocks
 *       for ≥5 seconds)</li>
 * </ul>
 */
public class HeistPlan {

    /** The landmark being targeted. */
    private final LandmarkType target;

    /** World positions of alarm boxes on this building. */
    private final List<Vector3> alarmBoxPositions = new ArrayList<>();

    /** Silenced state for each alarm box (parallel list to alarmBoxPositions). */
    private final List<Boolean> alarmBoxSilenced = new ArrayList<>();

    /** World positions of CCTV cameras. */
    private final List<Vector3> cctvPositions = new ArrayList<>();

    /** World positions of safes. */
    private final List<Vector3> safePositions = new ArrayList<>();

    /** Whether the safe at each position has been cracked (parallel to safePositions). */
    private final List<Boolean> safeCracked = new ArrayList<>();

    /** Whether each patrol guard's route has been observed (true = revealed, false = "?"). */
    private final List<Boolean> guardRoutesObserved = new ArrayList<>();

    /** Number of patrol guards that patrol this building. */
    private final int guardCount;

    /** Accumulated observation time per guard (seconds). Reaches 5s to reveal route. */
    private final List<Float> guardObservationTime = new ArrayList<>();

    /**
     * Construct a heist plan for the given target.
     *
     * @param target     the landmark to rob
     * @param guardCount number of patrol guards assigned to the building
     */
    public HeistPlan(LandmarkType target, int guardCount) {
        this.target = target;
        this.guardCount = guardCount;
        for (int i = 0; i < guardCount; i++) {
            guardRoutesObserved.add(false);
            guardObservationTime.add(0f);
        }
    }

    // ── Alarm boxes ───────────────────────────────────────────────────────────

    /**
     * Register an alarm box at the given world position.
     */
    public void addAlarmBox(Vector3 position) {
        alarmBoxPositions.add(new Vector3(position));
        alarmBoxSilenced.add(false);
    }

    /** Number of alarm boxes on this building. */
    public int getAlarmBoxCount() {
        return alarmBoxPositions.size();
    }

    /** Position of alarm box {@code i}. */
    public Vector3 getAlarmBoxPosition(int i) {
        return alarmBoxPositions.get(i);
    }

    /** Whether alarm box {@code i} has been silenced. */
    public boolean isAlarmBoxSilenced(int i) {
        return alarmBoxSilenced.get(i);
    }

    /** Mark alarm box {@code i} as silenced (or re-armed). */
    public void setAlarmBoxSilenced(int i, boolean silenced) {
        alarmBoxSilenced.set(i, silenced);
    }

    // ── CCTV ──────────────────────────────────────────────────────────────────

    /**
     * Register a CCTV camera at the given world position.
     */
    public void addCCTV(Vector3 position) {
        cctvPositions.add(new Vector3(position));
    }

    /** Number of CCTV cameras on this building. */
    public int getCCTVCount() {
        return cctvPositions.size();
    }

    /** Position of CCTV camera {@code i}. */
    public Vector3 getCCTVPosition(int i) {
        return cctvPositions.get(i);
    }

    // ── Safes ─────────────────────────────────────────────────────────────────

    /**
     * Register a safe at the given world position.
     */
    public void addSafe(Vector3 position) {
        safePositions.add(new Vector3(position));
        safeCracked.add(false);
    }

    /** Number of safes on this building. */
    public int getSafeCount() {
        return safePositions.size();
    }

    /** Position of safe {@code i}. */
    public Vector3 getSafePosition(int i) {
        return safePositions.get(i);
    }

    /** Whether safe {@code i} has been cracked. */
    public boolean isSafeCracked(int i) {
        return safeCracked.get(i);
    }

    /** Mark safe {@code i} as cracked. */
    public void setSafeCracked(int i, boolean cracked) {
        safeCracked.set(i, cracked);
    }

    // ── Guard observation ─────────────────────────────────────────────────────

    /** Number of patrol guards assigned to this building. */
    public int getGuardCount() {
        return guardCount;
    }

    /**
     * Accumulate observation time for guard {@code guardIndex}.
     * After 5 seconds, the guard's route is considered observed.
     *
     * @param guardIndex index of the guard (0-based)
     * @param seconds    seconds observed this frame
     */
    public void observeGuard(int guardIndex, float seconds) {
        if (guardIndex < 0 || guardIndex >= guardCount) return;
        if (guardRoutesObserved.get(guardIndex)) return; // already observed
        float total = guardObservationTime.get(guardIndex) + seconds;
        guardObservationTime.set(guardIndex, total);
        if (total >= 5f) {
            guardRoutesObserved.set(guardIndex, true);
        }
    }

    /** Whether guard {@code guardIndex}'s route has been fully observed. */
    public boolean isGuardRouteObserved(int guardIndex) {
        if (guardIndex < 0 || guardIndex >= guardCount) return false;
        return guardRoutesObserved.get(guardIndex);
    }

    // ── Planning recommendations ──────────────────────────────────────────────

    /**
     * Returns a list of recommended items based on what was observed during casing.
     */
    public List<String> getRecommendations() {
        List<String> recs = new ArrayList<>();
        if (!alarmBoxPositions.isEmpty()) {
            recs.add("You'll need BOLT_CUTTERS or GLASS_CUTTER.");
        }
        if (!safePositions.isEmpty()) {
            recs.add("You'll need a CROWBAR.");
        }
        if (!cctvPositions.isEmpty()) {
            recs.add("You'll need a BALACLAVA (at night).");
        }
        return recs;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** The target landmark. */
    public LandmarkType getTarget() {
        return target;
    }

    /** Whether all alarm boxes have been silenced. */
    public boolean allAlarmBoxesSilenced() {
        for (boolean silenced : alarmBoxSilenced) {
            if (!silenced) return false;
        }
        return true;
    }
}

package ragamuffin.core;

/**
 * A single procedural faction mission offered by a lieutenant (Phase 8d / Issue #702).
 *
 * <p>Missions expire after {@link FactionSystem#MISSION_DURATION_SECONDS} real-time seconds.
 * On completion the owning faction gains {@link #respectReward} and rival factions lose
 * {@link #rivalRespectPenalty}.  On expiry (timeout) the faction loses
 * {@link FactionSystem#MISSION_FAIL_RESPECT_PENALTY}.
 */
public class FactionMission {

    public enum MissionType {
        DELIVERY_RUN,
        EVICTION_NOTICE,
        QUIET_THE_WITNESS,
        CORNER_DEFENCE,
        OFFICE_JOB,
        TAG_THE_TURF,
        VOLUNTARY_COMPLIANCE,
        REPORT_A_NUISANCE,
        CLEAR_THE_ENCAMPMENT
    }

    private final Faction faction;
    private final MissionType type;
    private final String title;
    private final String description;
    private final int coinReward;
    private final int respectReward;
    private final int rivalRespectPenalty;

    /** Remaining time in seconds; decrements each update tick. */
    private float timeRemaining;

    private boolean completed = false;
    private boolean failed    = false;

    public FactionMission(Faction faction, MissionType type,
                          String title, String description,
                          int coinReward, int respectReward, int rivalRespectPenalty,
                          float durationSeconds) {
        this.faction             = faction;
        this.type                = type;
        this.title               = title;
        this.description         = description;
        this.coinReward          = coinReward;
        this.respectReward       = respectReward;
        this.rivalRespectPenalty = rivalRespectPenalty;
        this.timeRemaining       = durationSeconds;
    }

    /** Advance the mission timer. Returns true if the mission has now expired. */
    public boolean tick(float delta) {
        if (completed || failed) return false;
        timeRemaining -= delta;
        if (timeRemaining <= 0f) {
            timeRemaining = 0f;
            failed = true;
            return true;
        }
        return false;
    }

    public void markCompleted() {
        this.completed = true;
    }

    public void markFailed() {
        this.failed = true;
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    public Faction getFaction()              { return faction; }
    public MissionType getType()             { return type; }
    public String getTitle()                 { return title; }
    public String getDescription()           { return description; }
    public int getCoinReward()               { return coinReward; }
    public int getRespectReward()            { return respectReward; }
    public int getRivalRespectPenalty()      { return rivalRespectPenalty; }
    public float getTimeRemaining()          { return timeRemaining; }
    public boolean isCompleted()             { return completed; }
    public boolean isFailed()               { return failed; }
    public boolean isActive()               { return !completed && !failed; }
}

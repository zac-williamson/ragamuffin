package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1339 — Northfield Council Enforcement Day: The Coordinated Sweep,
 * Lying Low &amp; the Chaos Window.
 *
 * <p>Manages a recurring multi-agency enforcement sweep that fires every 14
 * in-game days (days 14, 28, 42…). The sweep runs 08:00–17:00. The evening
 * before (day 13, 27, 41…) at 19:00 a COUNCIL_NOTICE_PROP appears on the
 * community centre noticeboard and an ENFORCEMENT_SWEEP rumour is seeded,
 * giving the player time to pre-empt citations.
 *
 * <h3>Teams deployed during the sweep</h3>
 * <ul>
 *   <li><b>Derek (LICENCE_OFFICER) + COUNCIL_OFFICER</b> — TV licensing does
 *       double rounds. Integrates with {@link TvLicensingSystem}.</li>
 *   <li><b>Clive (TRAFFIC_WARDEN) + WARDEN_TRAINEE</b> — Trainee has 50% error
 *       rate; mistakes create a {@value #FREE_PARKING_WINDOW_SECONDS}-second
 *       free-parking window. Integrates with {@link TrafficWardenSystem}.</li>
 *   <li><b>Karen (DVLA_OFFICER)</b> — Checks plates at road junctions. No
 *       DRIVING_LICENCE = car towed + NO_INSURANCE_DRIVING crime.</li>
 *   <li><b>Phil (BENEFITS_INVESTIGATOR)</b> — Cross-references {@link DWPSystem}
 *       with {@link EmploymentSystem}. Working while claiming = BENEFIT_FRAUD +
 *       WantedSystem +2.</li>
 *   <li><b>Janet (ENVIRONMENTAL_HEALTH_OFFICER)</b> — Inspects all food venues
 *       in one day (normally 1/day). CCTV unplugged during inspection at 2 venues,
 *       creating a till-robbing opportunity.</li>
 * </ul>
 *
 * <h3>Chaos Window (08:30–16:00)</h3>
 * <ol>
 *   <li>CCTV unplugged at 2 inspected venues — rob tills with no camera evidence.</li>
 *   <li>Warden trainee mistake creates a {@value #FREE_PARKING_WINDOW_SECONDS}s
 *       free-parking window while Clive is distracted.</li>
 * </ol>
 *
 * <h3>Penalties</h3>
 * All Enforcement Day citations carry a ×{@value #PENALTY_MULTIPLIER} multiplier.
 * Each team is escorted by a POLICE_PATROL who can arrest immediately.
 *
 * <h3>Newspaper</h3>
 * The morning after the sweep, {@link NewspaperSystem} carries a headline
 * reflecting the outcome (clean sweep vs. catches).
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@link ragamuffin.ui.AchievementType#FOREWARNED} — player hears the
 *       ENFORCEMENT_SWEEP rumour before the sweep begins.</li>
 *   <li>{@link ragamuffin.ui.AchievementType#LAY_LOW} — player receives zero
 *       citations during the entire sweep window.</li>
 *   <li>{@link ragamuffin.ui.AchievementType#WARDEN_CHAOS} — warden trainee
 *       error fires at least once.</li>
 *   <li>{@link ragamuffin.ui.AchievementType#CHAOS_WINDOW} — player exploits
 *       a chaos window (unplugged CCTV or free-parking window).</li>
 * </ul>
 */
public class CouncilEnforcementSystem {

    // ── Sweep schedule ─────────────────────────────────────────────────────────

    /** Number of in-game days between enforcement sweeps. */
    public static final int SWEEP_INTERVAL_DAYS = 14;

    /** Hour the enforcement sweep begins. */
    public static final float SWEEP_START_HOUR = 8.0f;

    /** Hour the enforcement sweep ends. */
    public static final float SWEEP_END_HOUR = 17.0f;

    /** Hour the evening-before warning notice is posted. */
    public static final float WARNING_HOUR = 19.0f;

    // ── Chaos window ───────────────────────────────────────────────────────────

    /** Hour the chaos window opens (CCTV unplugged, warden distraction). */
    public static final float CHAOS_START_HOUR = 8.5f;

    /** Hour the chaos window closes. */
    public static final float CHAOS_END_HOUR = 16.0f;

    /** Number of food venues whose CCTV is unplugged during Janet's inspection. */
    public static final int CCTV_UNPLUGGED_VENUES = 2;

    /**
     * Duration (in seconds) of the free-parking window created by the warden
     * trainee's ticketing error.
     */
    public static final float FREE_PARKING_WINDOW_SECONDS = 300f; // 5 minutes

    /**
     * Probability (0–1) that the WARDEN_TRAINEE issues a ticket to the wrong
     * vehicle on any given inspection attempt.
     */
    public static final float TRAINEE_ERROR_RATE = 0.50f;

    // ── Penalty multiplier ─────────────────────────────────────────────────────

    /**
     * Multiplier applied to all citations issued on Enforcement Day.
     * e.g. a fine of 8 COIN becomes 12 COIN (×1.5).
     */
    public static final float PENALTY_MULTIPLIER = 1.5f;

    // ── WantedSystem addition for benefit fraud ────────────────────────────────

    /** Wanted-star penalty added when Phil catches working-while-claiming. */
    public static final int BENEFIT_FRAUD_WANTED_STARS = 2;

    // ── State ──────────────────────────────────────────────────────────────────

    private final Random random;

    /** Day on which the last warning notice was posted (0 = never posted). */
    private int lastWarningDay = 0;

    /** Day on which the last sweep was run (0 = never run). */
    private int lastSweepDay = 0;

    /** Whether the current sweep is active (true between 08:00 and 17:00 on a sweep day). */
    private boolean sweepActive = false;

    /** Whether the chaos window is currently open. */
    private boolean chaosWindowOpen = false;

    /**
     * Whether the COUNCIL_NOTICE_PROP is currently posted on the noticeboard.
     * Set to true at 19:00 the evening before; cleared at 08:00 on sweep day.
     */
    private boolean noticePropPosted = false;

    /** Whether the free-parking window is currently active (trainee mistake). */
    private boolean freeParkingWindowActive = false;

    /** Remaining seconds of the free-parking window (counts down when active). */
    private float freeParkingWindowRemaining = 0f;

    /** Whether the warden trainee error has already fired this sweep. */
    private boolean traineeErrorFiredThisSweep = false;

    /** Number of citations issued to the player during the current sweep. */
    private int citationsThisSweep = 0;

    /**
     * Number of chaos window exploitations by the player during the current
     * sweep (till robs at unplugged-CCTV venues or free-parking uses).
     */
    private int chaosExploitsThisSweep = 0;

    /** Whether LAY_LOW has been awarded for this sweep. */
    private boolean layLowAwardedThisSweep = false;

    // ── Injected dependencies ──────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private NewspaperSystem newspaperSystem;
    private DWPSystem dwpSystem;
    private EmploymentSystem employmentSystem;
    private TrafficWardenSystem trafficWardenSystem;
    private TvLicensingSystem tvLicensingSystem;
    private EnvironmentalHealthSystem environmentalHealthSystem;

    // ── Constructor ────────────────────────────────────────────────────────────

    /**
     * Constructs a new CouncilEnforcementSystem.
     *
     * @param random seeded random used for trainee error rolls and chaos outcomes
     */
    public CouncilEnforcementSystem(Random random) {
        this.random = random;
    }

    // ── Dependency setters ─────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setNewspaperSystem(NewspaperSystem newspaperSystem) {
        this.newspaperSystem = newspaperSystem;
    }

    public void setDwpSystem(DWPSystem dwpSystem) {
        this.dwpSystem = dwpSystem;
    }

    public void setEmploymentSystem(EmploymentSystem employmentSystem) {
        this.employmentSystem = employmentSystem;
    }

    public void setTrafficWardenSystem(TrafficWardenSystem trafficWardenSystem) {
        this.trafficWardenSystem = trafficWardenSystem;
    }

    public void setTvLicensingSystem(TvLicensingSystem tvLicensingSystem) {
        this.tvLicensingSystem = tvLicensingSystem;
    }

    public void setEnvironmentalHealthSystem(EnvironmentalHealthSystem environmentalHealthSystem) {
        this.environmentalHealthSystem = environmentalHealthSystem;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Per-frame update. Called once per game loop tick.
     *
     * @param delta      seconds since last frame
     * @param currentDay current in-game day (1-based)
     * @param currentHour current in-game hour (0.0–24.0)
     * @param npcs       all active NPCs (used for rumour seeding)
     * @param playerInventory player's inventory (for DRIVING_LICENCE check)
     * @param achievementCb callback to unlock achievements
     */
    public void update(float delta, int currentDay, float currentHour,
                       List<NPC> npcs,
                       Inventory playerInventory,
                       NotorietySystem.AchievementCallback achievementCb) {

        // ── 1. Evening-before warning (19:00 on day before each sweep day) ────
        boolean isWarningDay = isSweepDay(currentDay + 1);
        if (isWarningDay && currentHour >= WARNING_HOUR && lastWarningDay != currentDay) {
            lastWarningDay = currentDay;
            postEveningWarning(npcs, achievementCb);
        }

        // ── 2. Sweep activation (08:00 on sweep day) ───────────────────────────
        boolean isTodaySweepDay = isSweepDay(currentDay);
        if (isTodaySweepDay) {
            // Remove notice prop at sweep start
            if (currentHour >= SWEEP_START_HOUR && noticePropPosted) {
                noticePropPosted = false;
            }

            // Activate sweep
            if (currentHour >= SWEEP_START_HOUR && currentHour < SWEEP_END_HOUR
                    && lastSweepDay != currentDay) {
                if (!sweepActive) {
                    sweepActive = true;
                    citationsThisSweep = 0;
                    chaosExploitsThisSweep = 0;
                    layLowAwardedThisSweep = false;
                    traineeErrorFiredThisSweep = false;
                }
            }

            // Open chaos window (08:30–16:00)
            if (sweepActive && currentHour >= CHAOS_START_HOUR && currentHour < CHAOS_END_HOUR) {
                chaosWindowOpen = true;
            } else {
                chaosWindowOpen = false;
            }

            // Deactivate sweep at end of day
            if (currentHour >= SWEEP_END_HOUR && sweepActive) {
                sweepActive = false;
                chaosWindowOpen = false;
                lastSweepDay = currentDay;
                onSweepComplete(achievementCb);
            }
        } else {
            // Not a sweep day — ensure sweep is inactive
            if (sweepActive) {
                sweepActive = false;
                chaosWindowOpen = false;
            }
        }

        // ── 3. Warden free-parking window countdown ────────────────────────────
        if (freeParkingWindowActive) {
            freeParkingWindowRemaining -= delta;
            if (freeParkingWindowRemaining <= 0f) {
                freeParkingWindowActive = false;
                freeParkingWindowRemaining = 0f;
            }
        }

        // ── 4. Phil's benefit-fraud check (once per sweep, during chaos window) ─
        if (sweepActive && chaosWindowOpen && lastSweepDay != currentDay) {
            runBenefitsFraudCheck(playerInventory, npcs, achievementCb);
        }
    }

    // ── Evening warning ────────────────────────────────────────────────────────

    /**
     * Posts the COUNCIL_NOTICE_PROP on the community centre noticeboard and
     * seeds the ENFORCEMENT_SWEEP rumour at 19:00 the evening before a sweep.
     */
    private void postEveningWarning(List<NPC> npcs,
                                    NotorietySystem.AchievementCallback achievementCb) {
        noticePropPosted = true;

        if (rumourNetwork != null && !npcs.isEmpty()) {
            NPC seedNpc = npcs.get(0);
            rumourNetwork.addRumour(seedNpc,
                new Rumour(RumourType.ENFORCEMENT_SWEEP,
                    "Council's doing a big sweep tomorrow — TV licensing, DVLA, " +
                    "traffic wardens, the lot. Notice is up on the community centre board."));
        }
    }

    // ── DVLA plate check ───────────────────────────────────────────────────────

    /**
     * Karen (DVLA_OFFICER) checks whether the player has a DRIVING_LICENCE.
     * If not, the vehicle is towed and NO_INSURANCE_DRIVING is recorded.
     * Only valid while the sweep is active.
     *
     * @param playerInventory player's inventory
     * @param playerX         player world X (for wanted-star positioning)
     * @param playerY         player world Y
     * @param playerZ         player world Z
     * @param achievementCb   achievement callback
     * @return result of the plate check
     */
    public DvlaCheckResult checkPlates(Inventory playerInventory,
                                       float playerX, float playerY, float playerZ,
                                       NotorietySystem.AchievementCallback achievementCb) {
        if (!sweepActive) {
            return DvlaCheckResult.NOT_ON_DUTY;
        }
        boolean hasLicence = playerInventory.getItemCount(Material.DRIVING_LICENCE) > 0
                || playerInventory.getItemCount(Material.FULL_DRIVING_LICENCE) > 0;
        if (hasLicence) {
            return DvlaCheckResult.LICENCE_VALID;
        }

        // No licence — tow the vehicle and record the offence
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.NO_INSURANCE_DRIVING);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(10, achievementCb);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(2, playerX, playerY, playerZ, achievementCb);
        }
        citationsThisSweep++;
        return DvlaCheckResult.VEHICLE_TOWED;
    }

    // ── Warden trainee interaction ─────────────────────────────────────────────

    /**
     * Simulates the WARDEN_TRAINEE attempting to issue a PCN. There is a
     * {@value #TRAINEE_ERROR_RATE} chance the trainee tickets the wrong vehicle,
     * creating a free-parking window and awarding WARDEN_CHAOS.
     * Only fires once per sweep.
     *
     * @param achievementCb achievement callback
     * @return result of the trainee's inspection attempt
     */
    public TraineeInspectResult traineeInspect(
            NotorietySystem.AchievementCallback achievementCb) {
        if (!sweepActive) {
            return TraineeInspectResult.NOT_ON_DUTY;
        }
        if (traineeErrorFiredThisSweep) {
            return TraineeInspectResult.NO_ERROR;
        }
        if (random.nextFloat() < TRAINEE_ERROR_RATE) {
            // Trainee errors — wrong car ticketed
            traineeErrorFiredThisSweep = true;
            freeParkingWindowActive = true;
            freeParkingWindowRemaining = FREE_PARKING_WINDOW_SECONDS;
            achievementCb.onAchievement(AchievementType.WARDEN_CHAOS);
            return TraineeInspectResult.WRONG_CAR_TICKETED;
        }
        return TraineeInspectResult.NO_ERROR;
    }

    // ── Player exploits chaos window ───────────────────────────────────────────

    /**
     * Called when the player exploits a chaos window opportunity — either robbing
     * a till at a venue where CCTV has been unplugged by Janet, or parking for
     * free during the warden-trainee distraction window.
     *
     * <p>Awards {@link AchievementType#CHAOS_WINDOW} on first exploitation.
     *
     * @param achievementCb achievement callback
     * @return true if the chaos window was actually open and exploitable
     */
    public boolean exploitChaosWindow(NotorietySystem.AchievementCallback achievementCb) {
        if (!chaosWindowOpen) {
            return false;
        }
        chaosExploitsThisSweep++;
        if (chaosExploitsThisSweep == 1) {
            achievementCb.onAchievement(AchievementType.CHAOS_WINDOW);
        }
        return true;
    }

    // ── Player notices noticeboard ─────────────────────────────────────────────

    /**
     * Called when the player reads the COUNCIL_NOTICE_PROP on the community
     * centre noticeboard before the sweep begins. Awards FOREWARNED on first
     * read.
     *
     * @param achievementCb achievement callback
     * @return true if the notice was present and the player was forewarned
     */
    public boolean readNotice(NotorietySystem.AchievementCallback achievementCb) {
        if (!noticePropPosted) {
            return false;
        }
        achievementCb.onAchievement(AchievementType.FOREWARNED);
        return true;
    }

    // ── Benefit-fraud check ────────────────────────────────────────────────────

    /**
     * Phil (BENEFITS_INVESTIGATOR) cross-references DWPSystem with
     * EmploymentSystem. If the player is claiming UC while employed, BENEFIT_FRAUD
     * is recorded and WantedSystem +{@value #BENEFIT_FRAUD_WANTED_STARS} stars
     * are added. Only fires once per sweep (governed by lastSweepDay check in
     * update()).
     */
    private void runBenefitsFraudCheck(Inventory playerInventory,
                                       List<NPC> npcs,
                                       NotorietySystem.AchievementCallback achievementCb) {
        if (dwpSystem == null || employmentSystem == null) return;
        boolean claimingUC = dwpSystem.isUCClaimActive();
        boolean employed = employmentSystem.isEmployed();
        if (claimingUC && employed) {
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.BENEFIT_FRAUD);
            }
            if (wantedSystem != null && !npcs.isEmpty()) {
                NPC firstNpc = npcs.get(0);
                wantedSystem.addWantedStars(BENEFIT_FRAUD_WANTED_STARS,
                    firstNpc.getPosition().x, firstNpc.getPosition().y,
                    firstNpc.getPosition().z, achievementCb);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(15, achievementCb);
            }
            citationsThisSweep++;
        }
    }

    // ── Sweep complete ─────────────────────────────────────────────────────────

    /**
     * Called at 17:00 when the sweep ends. Awards LAY_LOW if the player received
     * no citations. Publishes a newspaper headline reflecting the outcome.
     */
    private void onSweepComplete(NotorietySystem.AchievementCallback achievementCb) {
        if (citationsThisSweep == 0 && !layLowAwardedThisSweep) {
            layLowAwardedThisSweep = true;
            achievementCb.onAchievement(AchievementType.LAY_LOW);
        }

        if (newspaperSystem != null) {
            String headline;
            if (citationsThisSweep == 0) {
                headline = "COUNCIL ENFORCEMENT SWEEP DRAWS BLANK — 'WE'LL BE BACK,' VOWS DEREK";
            } else {
                headline = "COUNCIL BLITZ NETS " + citationsThisSweep
                    + " OFFENDER" + (citationsThisSweep == 1 ? "" : "S")
                    + " IN NORTHFIELD SWEEP";
            }
            newspaperSystem.publishHeadline(headline);
        }
    }

    // ── Helper: is this day a sweep day? ──────────────────────────────────────

    /**
     * Returns true if {@code day} is a sweep day (i.e. day % 14 == 0 and day > 0).
     *
     * @param day in-game day number (1-based)
     */
    public boolean isSweepDay(int day) {
        return day > 0 && day % SWEEP_INTERVAL_DAYS == 0;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Returns true if the enforcement sweep is currently active. */
    public boolean isSweepActive() {
        return sweepActive;
    }

    /** Returns true if the chaos window is currently open. */
    public boolean isChaosWindowOpen() {
        return chaosWindowOpen;
    }

    /** Returns true if the COUNCIL_NOTICE_PROP is currently posted. */
    public boolean isNoticePropPosted() {
        return noticePropPosted;
    }

    /** Returns true if the free-parking window is currently active. */
    public boolean isFreeParkingWindowActive() {
        return freeParkingWindowActive;
    }

    /** Returns the remaining seconds of the free-parking window. */
    public float getFreeParkingWindowRemaining() {
        return freeParkingWindowRemaining;
    }

    /** Returns the number of citations issued to the player during the current sweep. */
    public int getCitationsThisSweep() {
        return citationsThisSweep;
    }

    // ── Result enums ───────────────────────────────────────────────────────────

    /** Result of Karen's DVLA plate check. */
    public enum DvlaCheckResult {
        /** Karen is not on duty (no sweep active). */
        NOT_ON_DUTY,
        /** Player has a valid DRIVING_LICENCE — no action taken. */
        LICENCE_VALID,
        /** Player has no licence — vehicle towed, crime recorded. */
        VEHICLE_TOWED
    }

    /** Result of the WARDEN_TRAINEE's inspection attempt. */
    public enum TraineeInspectResult {
        /** Sweep is not currently active. */
        NOT_ON_DUTY,
        /** Trainee issued ticket correctly (or error already fired this sweep). */
        NO_ERROR,
        /** Trainee ticketed the wrong car — free-parking window opened. */
        WRONG_CAR_TICKETED
    }
}

package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1473: Northfield Community Litter Pick — Janet's Tidy Streets,
 * the Quota Dodge &amp; the Crack Pipe Incident.
 *
 * <p>Janet from number 42 runs Northfield Tidy Streets every second Saturday
 * 09:00–11:00 outside the park gates.
 *
 * <ul>
 *   <li><b>Mechanic 1 — Sign-Up</b>: Press E on Janet before 09:15 to receive
 *       LITTER_PICKER_STICK and COUNCIL_RUBBISH_BAG. Late arrivals and
 *       high-notoriety players (≥ {@value #NOTORIETY_REFUSAL_THRESHOLD}) refused.</li>
 *   <li><b>Mechanic 2 — Litter Collection</b>: 10–18 LITTER_PROP items scatter the
 *       park. Player presses E with stick equipped to collect; LITTER_TARGET = 8.
 *       Returning a full bag gives Notoriety −5 and a 120-second Good Citizen buff.
 *       3–5 VOLUNTEER_PICKER NPCs compete, each collecting one item every 45 s.</li>
 *   <li><b>Mechanic 3 — Volunteer Pickpocketing</b>: 60% pickpocket success while
 *       volunteer is bent down collecting. Alternatively steal their bag (3-second
 *       hold-E) to claim up to 5 items toward the player's quota.</li>
 *   <li><b>Mechanic 4 — The Crack Pipe Incident</b>: Player can slip a CRACK_PIPE
 *       into their COUNCIL_RUBBISH_BAG before handing in. Janet opens the bag,
 *       screams, police called (WantedSystem +2, CrimeType.POSSESSION, Notoriety +12,
 *       event ends). Variant: plant the pipe in a volunteer's bag instead — if
 *       undetected the volunteer gets blamed. Achievement: JANET_S_MORNING / FIT_UP.</li>
 *   <li><b>Mechanic 5 — Fly-Tip Sabotage</b>: Drop BIN_BAG items within 15 blocks of
 *       the event to respawn 4 litter props each time. After 2 fly-tips Janet suspects
 *       the player and assigns a volunteer watcher (auto-fails pickpocket).
 *       EnvironmentalHealthSystem notified if fly-tip occurs in park bounds.
 *       Achievement: UNDOING_ALL_THE_GOOD if player fly-tipped more than they
 *       collected.</li>
 * </ul>
 *
 * <h3>Integration points:</h3>
 * <ul>
 *   <li>{@link NotorietySystem} — notoriety gains/reductions.</li>
 *   <li>{@link WantedSystem} — +2 stars on crack pipe incident.</li>
 *   <li>{@link CriminalRecord} — POSSESSION on crack pipe.</li>
 *   <li>{@link RumourNetwork} — PICKPOCKET_SPOTTED on detected pickpocket;
 *       SCANDAL_RUMOUR on crack pipe incident.</li>
 *   <li>{@link EnvironmentalHealthSystem} — notifyParkFlyTip() on fly-tip in park.</li>
 * </ul>
 */
public class LitterPickSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Hour the litter pick event begins (09:00). */
    public static final float EVENT_START_HOUR = 9.0f;

    /** Hour the litter pick event ends (11:00). */
    public static final float EVENT_END_HOUR = 11.0f;

    /** Last hour at which a player may sign up (09:15). */
    public static final float SIGN_UP_DEADLINE_HOUR = 9.25f;

    /** Notoriety at or above which Janet refuses the player. */
    public static final int NOTORIETY_REFUSAL_THRESHOLD = 400;

    /** Number of litter items required to meet the player's quota. */
    public static final int LITTER_TARGET = 8;

    /** Minimum number of LITTER_PROP items spawned at event start. */
    public static final int LITTER_SPAWN_MIN = 10;

    /** Maximum number of LITTER_PROP items spawned at event start. */
    public static final int LITTER_SPAWN_MAX = 18;

    /** Minimum number of VOLUNTEER_PICKER NPCs at the event. */
    public static final int VOLUNTEER_COUNT_MIN = 3;

    /** Maximum number of VOLUNTEER_PICKER NPCs at the event. */
    public static final int VOLUNTEER_COUNT_MAX = 5;

    /** In-game seconds between each volunteer pick action. */
    public static final float VOLUNTEER_PICK_INTERVAL_SECONDS = 45.0f;

    /** Notoriety reduction for handing in a quota-met bag. */
    public static final int GOOD_CITIZEN_NOTORIETY_REDUCTION = 5;

    /** Duration of the Good Citizen buff in real seconds. */
    public static final float GOOD_CITIZEN_BUFF_DURATION_SECONDS = 120.0f;

    /** Probability of a successful pickpocket on a distracted volunteer (0–1). */
    public static final float DISTRACTED_PICKPOCKET_CHANCE = 0.60f;

    /** Notoriety gain for a caught (failed) pickpocket attempt. */
    public static final int PICKPOCKET_CAUGHT_NOTORIETY = 4;

    /** Seconds the player must hold E to steal a volunteer's bag. */
    public static final float BAG_STEAL_DURATION = 3.0f;

    /** Quota items contributed by a stolen volunteer bag. */
    public static final int STOLEN_BAG_QUOTA_CONTRIBUTION = 5;

    /** Notoriety gain from the crack pipe incident. */
    public static final int CRACK_PIPE_NOTORIETY = 12;

    /** Seconds the player must hold E to plant a crack pipe in a volunteer's bag. */
    public static final float PLANT_DURATION = 4.0f;

    /** Probability that the volunteer detects the planting attempt (0–1). */
    public static final float PLANT_DETECTION_CHANCE = 0.35f;

    /** Blocks from Janet within which a BIN_BAG drop is treated as a fly-tip. */
    public static final float FLY_TIP_RADIUS = 15.0f;

    /** Number of LITTER_PROP items respawned per fly-tip. */
    public static final int FLY_TIP_LITTER_SPAWN = 4;

    /** Number of fly-tips before Janet assigns a volunteer watcher. */
    public static final int FLY_TIP_BEFORE_SUSPECTED = 2;

    /** WantedSystem stars added on the crack pipe incident. */
    public static final int CRACK_PIPE_WANTED_STARS = 2;

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Result of attempting to sign up with Janet. */
    public enum SignUpResult {
        /** Player successfully signed up and received equipment. */
        SIGNED_UP,
        /** Sign-up deadline passed (after 09:15). */
        TOO_LATE,
        /** Player's notoriety is too high (≥ {@value #NOTORIETY_REFUSAL_THRESHOLD}). */
        REFUSED_NOTORIETY
    }

    /** Result of handing in the COUNCIL_RUBBISH_BAG to Janet. */
    public enum HandInResult {
        /** Player collected at least {@value #LITTER_TARGET} items. */
        QUOTA_MET,
        /** Player collected fewer than {@value #LITTER_TARGET} items. */
        QUOTA_NOT_MET,
        /** Bag contained a CRACK_PIPE — incident triggered, event ended. */
        CRACK_PIPE_INCIDENT
    }

    /** Result of attempting to pickpocket a distracted volunteer. */
    public enum PickpocketResult {
        /** Pickpocket succeeded — COIN added to player inventory. */
        SUCCESS,
        /** Pickpocket failed — notoriety gained and PICKPOCKET_SPOTTED rumour seeded. */
        FAILURE,
        /** Player is being watched by a watcher-volunteer — auto-fail, no notoriety. */
        WATCHED_AUTO_FAIL
    }

    /** Result of attempting to steal a volunteer's bag. */
    public enum BagStealResult {
        /** Bag successfully stolen — quota items added. */
        STOLEN,
        /** Volunteer was not in the distracted/bent-down state. */
        NOT_DISTRACTED
    }

    /** Result of attempting to plant a CRACK_PIPE in a volunteer's bag. */
    public enum PlantResult {
        /** Pipe planted undetected — volunteer will be blamed at hand-in. */
        PLANTED,
        /** Volunteer detected the planting — notoriety gained and event alerted. */
        DETECTED
    }

    /** Reason the litter pick event ended. */
    public enum EventEndReason {
        TIME_EXPIRED,
        CRACK_PIPE_INCIDENT,
        PLAYER_QUIT
    }

    // ── Inner classes ─────────────────────────────────────────────────────────

    /**
     * Tracks per-volunteer state during the event.
     */
    public static class VolunteerState {
        public final NPC npc;
        /** Seconds since this volunteer last picked an item. */
        public float pickTimer;
        /** True while the volunteer is bent down and distracted (pickpocketable). */
        public boolean isBentDown;
        /** True if this volunteer has been assigned to watch the player. */
        public boolean isWatcher;
        /** True if this volunteer's bag has been planted with a crack pipe. */
        public boolean bagPlanted;

        public VolunteerState(NPC npc) {
            this.npc = npc;
            this.pickTimer = 0f;
            this.isBentDown = false;
            this.isWatcher = false;
            this.bagPlanted = false;
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    private boolean eventActive = false;
    private boolean playerSignedUp = false;
    private int playerPickedCount = 0;
    private int litterCount = 0;   // remaining LITTER_PROP items in the world
    private int flyTipCount = 0;
    private boolean playerSuspected = false;  // Janet suspects after FLY_TIP_BEFORE_SUSPECTED fly-tips
    private boolean goodCitizenBuffActive = false;
    private float goodCitizenBuffTimer = 0f;

    private final List<VolunteerState> volunteers = new ArrayList<>();

    /** Number of items fly-tipped by the player during this event. */
    private int flyTippedItemCount = 0;

    /** True if the crack pipe incident has been triggered this event. */
    private boolean crackPipeIncidentFired = false;

    // ── Injected systems ──────────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private EnvironmentalHealthSystem environmentalHealthSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public LitterPickSystem() {
        this(new Random());
    }

    public LitterPickSystem(Random random) {
        this.random = random;
    }

    // ── Setters for injected systems ──────────────────────────────────────────

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

    public void setEnvironmentalHealthSystem(EnvironmentalHealthSystem ehs) {
        this.environmentalHealthSystem = ehs;
    }

    // ── Event lifecycle ───────────────────────────────────────────────────────

    /**
     * Start the event: spawn litter and volunteers.
     *
     * @param volunteerNpcs list of pre-created VOLUNTEER_PICKER NPCs (3–5)
     */
    public void startEvent(List<NPC> volunteerNpcs) {
        eventActive = true;
        playerSignedUp = false;
        playerPickedCount = 0;
        flyTipCount = 0;
        flyTippedItemCount = 0;
        playerSuspected = false;
        goodCitizenBuffActive = false;
        goodCitizenBuffTimer = 0f;
        crackPipeIncidentFired = false;
        volunteers.clear();

        // Spawn between LITTER_SPAWN_MIN and LITTER_SPAWN_MAX litter props
        litterCount = LITTER_SPAWN_MIN + random.nextInt(LITTER_SPAWN_MAX - LITTER_SPAWN_MIN + 1);

        for (NPC npc : volunteerNpcs) {
            volunteers.add(new VolunteerState(npc));
        }
    }

    /**
     * End the event for the given reason.
     */
    public void endEvent(EventEndReason reason) {
        eventActive = false;
    }

    // ── Mechanic 1: Sign-Up ───────────────────────────────────────────────────

    /**
     * Attempt to sign the player up at Janet's table.
     *
     * @param inventory      player's inventory (receives stick and bag on success)
     * @param currentHour    current in-game hour (e.g. 9.1f for 09:06)
     * @param notoriety      player's current notoriety score
     * @param achievementCallback callback for achievement unlocks (may be null)
     * @return the result of the sign-up attempt
     */
    public SignUpResult signUp(Inventory inventory, float currentHour, int notoriety,
                               NotorietySystem.AchievementCallback achievementCallback) {
        if (currentHour > SIGN_UP_DEADLINE_HOUR) {
            return SignUpResult.TOO_LATE;
        }
        if (notoriety >= NOTORIETY_REFUSAL_THRESHOLD) {
            return SignUpResult.REFUSED_NOTORIETY;
        }
        playerSignedUp = true;
        inventory.addItem(Material.LITTER_PICKER_STICK, 1);
        inventory.addItem(Material.COUNCIL_RUBBISH_BAG, 1);
        return SignUpResult.SIGNED_UP;
    }

    // ── Mechanic 2: Litter Collection ─────────────────────────────────────────

    /**
     * Player presses E with the litter picker stick equipped to collect one LITTER_PROP.
     * Increments the player's picked count and decrements remaining litter.
     *
     * @return true if a piece of litter was collected (litter remains in world), false otherwise
     */
    public boolean collectLitterItem() {
        if (!playerSignedUp || !eventActive || litterCount <= 0) {
            return false;
        }
        playerPickedCount++;
        litterCount--;
        return true;
    }

    /**
     * Player hands in their COUNCIL_RUBBISH_BAG to Janet.
     *
     * <p>If the bag contains a CRACK_PIPE, the incident is triggered: WantedSystem +2
     * stars, CrimeType.POSSESSION added to criminal record, Notoriety +{@value #CRACK_PIPE_NOTORIETY},
     * SCANDAL_RUMOUR seeded, and the event ends.
     *
     * <p>If quota is met (≥ {@value #LITTER_TARGET} items), Notoriety −{@value #GOOD_CITIZEN_NOTORIETY_REDUCTION}
     * and a {@value #GOOD_CITIZEN_BUFF_DURATION_SECONDS}-second Good Citizen buff is granted.
     *
     * @param inventory           player's inventory (bag and stick consumed on success)
     * @param achievementCallback callback for achievement unlocks (may be null)
     * @return the result of the hand-in
     */
    public HandInResult handInBag(Inventory inventory,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        // Check for crack pipe in bag
        if (inventory.getItemCount(Material.CRACK_PIPE) > 0) {
            return triggerCrackPipeIncident(inventory, achievementCallback, false);
        }

        // Remove equipment
        inventory.removeItem(Material.LITTER_PICKER_STICK, 1);
        inventory.removeItem(Material.COUNCIL_RUBBISH_BAG, 1);

        if (playerPickedCount >= LITTER_TARGET) {
            // Reduce notoriety
            if (notorietySystem != null) {
                notorietySystem.reduceNotoriety(GOOD_CITIZEN_NOTORIETY_REDUCTION, achievementCallback);
            }
            goodCitizenBuffActive = true;
            goodCitizenBuffTimer = GOOD_CITIZEN_BUFF_DURATION_SECONDS;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.TIDY_STREETS);
            }
            // Award UNDOING_ALL_THE_GOOD if player sabotaged more than they collected
            if (flyTippedItemCount > playerPickedCount && achievementCallback != null) {
                achievementCallback.award(AchievementType.UNDOING_ALL_THE_GOOD);
            }
            endEvent(EventEndReason.PLAYER_QUIT);
            return HandInResult.QUOTA_MET;
        } else {
            // Award UNDOING_ALL_THE_GOOD if applicable
            if (flyTippedItemCount > playerPickedCount && achievementCallback != null) {
                achievementCallback.award(AchievementType.UNDOING_ALL_THE_GOOD);
            }
            endEvent(EventEndReason.PLAYER_QUIT);
            return HandInResult.QUOTA_NOT_MET;
        }
    }

    // ── Mechanic 3: Volunteer Pickpocketing ───────────────────────────────────

    /**
     * Attempt to pickpocket a distracted volunteer.
     *
     * <p>Auto-fails silently if a watcher-volunteer has been assigned.
     * Success rate: {@value #DISTRACTED_PICKPOCKET_CHANCE} while volunteer is bent down.
     * On failure: Notoriety +{@value #PICKPOCKET_CAUGHT_NOTORIETY} and PICKPOCKET_SPOTTED rumour.
     *
     * @param inventory           player's inventory (receives COIN on success)
     * @param volunteerState      the volunteer being targeted
     * @param achievementCallback callback for achievement unlocks (may be null)
     * @return the result of the pickpocket attempt
     */
    public PickpocketResult attemptPickpocket(Inventory inventory,
                                              VolunteerState volunteerState,
                                              NotorietySystem.AchievementCallback achievementCallback) {
        // Check if player is being watched
        for (VolunteerState v : volunteers) {
            if (v.isWatcher) {
                return PickpocketResult.WATCHED_AUTO_FAIL;
            }
        }

        if (!volunteerState.isBentDown) {
            return PickpocketResult.FAILURE;
        }

        if (random.nextFloat() < DISTRACTED_PICKPOCKET_CHANCE) {
            // Success: grant 1–3 COIN
            int coinAmount = 1 + random.nextInt(3);
            inventory.addItem(Material.COIN, coinAmount);
            return PickpocketResult.SUCCESS;
        } else {
            // Failure: notoriety and rumour
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(PICKPOCKET_CAUGHT_NOTORIETY, achievementCallback);
            }
            if (rumourNetwork != null) {
                rumourNetwork.addRumour(volunteerState.npc,
                        new Rumour(RumourType.PICKPOCKET_SPOTTED,
                                "Someone got caught going through the pockets of a litter pick volunteer — broad daylight."));
            }
            return PickpocketResult.FAILURE;
        }
    }

    /**
     * Attempt to steal a volunteer's entire bag (hold-E for {@value #BAG_STEAL_DURATION}s).
     * The caller is responsible for tracking the hold duration; this method is called
     * when the duration is complete.
     *
     * @param volunteerState the volunteer being targeted
     * @return the result of the bag-steal attempt
     */
    public BagStealResult stealBag(VolunteerState volunteerState) {
        if (!volunteerState.isBentDown) {
            return BagStealResult.NOT_DISTRACTED;
        }
        playerPickedCount = Math.min(playerPickedCount + STOLEN_BAG_QUOTA_CONTRIBUTION, LITTER_TARGET);
        volunteerState.isBentDown = false;
        return BagStealResult.STOLEN;
    }

    // ── Mechanic 4: Crack Pipe Incident ───────────────────────────────────────

    /**
     * Attempt to plant a CRACK_PIPE into a volunteer's COUNCIL_RUBBISH_BAG.
     * Hold-E for {@value #PLANT_DURATION}s; the caller is responsible for tracking
     * the hold duration and calling this when complete.
     *
     * <p>There is a {@value #PLANT_DETECTION_CHANCE} chance the volunteer notices.
     * If detected, Notoriety +{@value #PICKPOCKET_CAUGHT_NOTORIETY} and the player
     * is flagged as suspected. If undetected, the volunteer's bag is marked as planted
     * and the volunteer will be blamed when Janet opens the bag.
     *
     * @param inventory           player's inventory (CRACK_PIPE consumed on success)
     * @param volunteerState      the targeted volunteer
     * @param achievementCallback callback for achievement unlocks (may be null)
     * @return the result of the planting attempt
     */
    public PlantResult plantCrackPipe(Inventory inventory,
                                      VolunteerState volunteerState,
                                      NotorietySystem.AchievementCallback achievementCallback) {
        if (random.nextFloat() < PLANT_DETECTION_CHANCE) {
            // Detected
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(PICKPOCKET_CAUGHT_NOTORIETY, achievementCallback);
            }
            playerSuspected = true;
            return PlantResult.DETECTED;
        }
        // Undetected — pipe consumed, volunteer marked
        inventory.removeItem(Material.CRACK_PIPE, 1);
        volunteerState.bagPlanted = true;
        return PlantResult.PLANTED;
    }

    /**
     * Triggers the crack pipe incident internally. Called when Janet opens a bag
     * (either the player's or a planted volunteer's bag).
     *
     * @param inventory           player's inventory
     * @param achievementCallback callback for achievement unlocks
     * @param isVolunteerBag      true if a volunteer's bag triggered the incident
     * @return HandInResult.CRACK_PIPE_INCIDENT always
     */
    private HandInResult triggerCrackPipeIncident(Inventory inventory,
                                                   NotorietySystem.AchievementCallback achievementCallback,
                                                   boolean isVolunteerBag) {
        crackPipeIncidentFired = true;

        if (!isVolunteerBag) {
            // Remove the crack pipe from the player's bag
            inventory.removeItem(Material.CRACK_PIPE, 1);

            // Police called — wanted stars + POSSESSION
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(CRACK_PIPE_WANTED_STARS, 0f, 0f, 0f, null);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.POSSESSION);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(CRACK_PIPE_NOTORIETY, achievementCallback);
            }
            if (rumourNetwork != null) {
                rumourNetwork.addRumour(null,
                        new Rumour(RumourType.SCANDAL_RUMOUR,
                                "That litter pick on Saturday — apparently someone hid something nasty in their bag. Police were called."));
            }
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.JANET_S_MORNING);
            }
        } else {
            // Volunteer blamed — no direct police call on player
            if (rumourNetwork != null) {
                rumourNetwork.addRumour(null,
                        new Rumour(RumourType.SCANDAL_RUMOUR,
                                "One of the litter pick volunteers had something very dodgy in their bag. The police were called."));
            }
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.FIT_UP);
            }
        }

        endEvent(EventEndReason.CRACK_PIPE_INCIDENT);
        return HandInResult.CRACK_PIPE_INCIDENT;
    }

    // ── Mechanic 5: Fly-Tip Sabotage ──────────────────────────────────────────

    /**
     * Called when the player drops a BIN_BAG item within {@value #FLY_TIP_RADIUS} blocks
     * of the event (in park bounds).
     *
     * <p>Each fly-tip respawns {@value #FLY_TIP_LITTER_SPAWN} LITTER_PROP items.
     * After {@value #FLY_TIP_BEFORE_SUSPECTED} fly-tips, Janet suspects the player and
     * assigns a volunteer watcher (auto-fails pickpocket).
     * EnvironmentalHealthSystem is notified.
     *
     * @param achievementCallback callback for achievement unlocks (may be null)
     */
    public void dropBinBag(NotorietySystem.AchievementCallback achievementCallback) {
        if (!eventActive) return;

        flyTipCount++;
        flyTippedItemCount++;

        // Respawn litter props
        litterCount += FLY_TIP_LITTER_SPAWN;

        // Notify environmental health
        if (environmentalHealthSystem != null) {
            environmentalHealthSystem.notifyParkFlyTip();
        }

        // After threshold, assign a watcher
        if (flyTipCount >= FLY_TIP_BEFORE_SUSPECTED && !playerSuspected) {
            playerSuspected = true;
            assignVolunteerWatcher();
        }
    }

    /**
     * Assigns the first non-watcher volunteer as the player's watcher.
     */
    private void assignVolunteerWatcher() {
        for (VolunteerState v : volunteers) {
            if (!v.isWatcher) {
                v.isWatcher = true;
                break;
            }
        }
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update. Advances volunteer pick timers and Good Citizen buff.
     *
     * @param delta               time delta in real seconds
     * @param currentHour         current in-game hour
     * @param achievementCallback callback for achievement unlocks (may be null)
     */
    public void update(float delta, float currentHour,
                       NotorietySystem.AchievementCallback achievementCallback) {
        if (!eventActive) return;

        // End event at EVENT_END_HOUR
        if (currentHour >= EVENT_END_HOUR) {
            endEvent(EventEndReason.TIME_EXPIRED);
            return;
        }

        // Advance Good Citizen buff timer
        if (goodCitizenBuffActive) {
            goodCitizenBuffTimer -= delta;
            if (goodCitizenBuffTimer <= 0f) {
                goodCitizenBuffActive = false;
            }
        }

        // Advance volunteer pick timers
        for (VolunteerState v : volunteers) {
            v.isBentDown = false;  // reset each frame; set true briefly during pick action
            v.pickTimer += delta;
            if (v.pickTimer >= VOLUNTEER_PICK_INTERVAL_SECONDS && litterCount > 0) {
                v.pickTimer = 0f;
                v.isBentDown = true;
                litterCount--;
            }
        }
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    /** Returns true if the litter pick event is currently active. */
    public boolean isEventActive() { return eventActive; }

    /** Returns true if the player has signed up for the event. */
    public boolean isPlayerSignedUp() { return playerSignedUp; }

    /** Returns the number of litter items the player has collected this event. */
    public int getPlayerPickedCount() { return playerPickedCount; }

    /** Returns the number of litter props remaining in the world. */
    public int getLitterCount() { return litterCount; }

    /** Returns the number of fly-tips performed by the player this event. */
    public int getFlyTipCount() { return flyTipCount; }

    /** Returns true if Janet suspects the player (triggers volunteer watcher). */
    public boolean isPlayerSuspected() { return playerSuspected; }

    /** Returns true if the Good Citizen buff is currently active. */
    public boolean isGoodCitizenBuffActive() { return goodCitizenBuffActive; }

    /** Returns the remaining duration of the Good Citizen buff in seconds. */
    public float getGoodCitizenBuffTimer() { return goodCitizenBuffTimer; }

    /** Returns true if the crack pipe incident has been triggered this event. */
    public boolean isCrackPipeIncidentFired() { return crackPipeIncidentFired; }

    /** Returns the number of fly-tipped items during this event. */
    public int getFlyTippedItemCount() { return flyTippedItemCount; }

    /** Returns an unmodifiable view of the current volunteer states. */
    public List<VolunteerState> getVolunteers() { return volunteers; }

    /**
     * Returns true if a volunteer watcher has been assigned (auto-fails pickpocket).
     * Delegates to checking the volunteer list for any watcher.
     */
    public boolean isPlayerBeingWatched() {
        for (VolunteerState v : volunteers) {
            if (v.isWatcher) return true;
        }
        return false;
    }
}

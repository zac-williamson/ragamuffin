package ragamuffin.core;

import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pirate FM — Underground Radio Station &amp; Neighbourhood Propaganda Machine.
 *
 * <p>Issue #783. The player builds a TRANSMITTER block indoors and broadcasts
 * using a MICROPHONE. Every 10 in-game seconds the player chooses one of four
 * broadcast actions:
 * <ol>
 *   <li>Big Up the Area — boosts all faction respect, reduces NPC boredom</li>
 *   <li>Slag Off a Faction — tanks one faction's respect, seeds rumours</li>
 *   <li>Black Market Shout-Out — spawns LISTENER NPCs carrying loot</li>
 *   <li>Council Diss Track — tanks Council respect, triggers newspaper headline</li>
 * </ol>
 *
 * <p>While broadcasting a triangulation bar fills; at 100% a Council Signal Van
 * spawns and drives to confiscate the transmitter. Stopping the broadcast resets
 * triangulation. The transmitter upgrades through 4 power levels (ranges 30→60→100→160)
 * by right-clicking with COMPUTER and STOLEN_PHONE components.
 *
 * <p>A BROADCAST_TAPE lets the player pre-record a show that auto-broadcasts at
 * half effectiveness while unattended.
 */
public class PirateRadioSystem {

    // ── Constants ────────────────────────────────────────────────────────────

    /** In-game seconds between broadcast action choices. */
    public static final float ACTION_INTERVAL_SECONDS = 10f;

    /** Maximum LISTENER NPCs active simultaneously. */
    public static final int MAX_LISTENERS = 6;

    /** Broadcast range (blocks) per power level (1-indexed; index 0 unused). */
    public static final int[] POWER_RANGES = {0, 30, 60, 100, 160};

    /** Triangulation progress per second per power level (1-indexed; index 0 unused). */
    public static final float[] TRIANGULATION_RATE = {0f, 0.3f, 0.5f, 0.8f, 1.5f};

    /** Triangulation threshold to spawn Signal Van. */
    public static final float TRIANGULATION_MAX = 100f;

    /** Triangulation warning threshold. */
    public static final float TRIANGULATION_WARN = 80f;

    /** Interval (in-game seconds) for BROADCAST_TAPE auto-broadcast. */
    public static final float TAPE_AUTO_BROADCAST_INTERVAL = 30f;

    /** Effectiveness multiplier for BROADCAST_TAPE auto-broadcasts. */
    public static final float TAPE_EFFECTIVENESS = 0.5f;

    /** Triangulation rate multiplier when using BROADCAST_TAPE (double speed). */
    public static final float TAPE_TRIANGULATION_MULTIPLIER = 2.0f;

    /** Notoriety gain on broadcast session start. */
    public static final int NOTORIETY_ON_START = 5;

    /** Notoriety gain for Council Diss Track action. */
    public static final int NOTORIETY_COUNCIL_DISS = 10;

    /** Notoriety gain for destroying Signal Van. */
    public static final int NOTORIETY_VAN_DESTROYED = 20;

    /** Respect delta applied to all factions for Big Up action. */
    public static final int RESPECT_BIG_UP_ALL = 3;

    /** Respect delta for targeted faction in Slag Off action. */
    public static final int RESPECT_SLAG_OFF_TARGET = -10;

    /** Respect delta for rival factions in Slag Off action. */
    public static final int RESPECT_SLAG_OFF_RIVALS = 5;

    /** Respect delta for Council in Council Diss Track action. */
    public static final int RESPECT_COUNCIL_DISS = -15;

    /** Cumulative broadcast minutes target for PIRATE_FM achievement. */
    public static final int PIRATE_FM_MINUTES_TARGET = 10;

    /** BORED need reduction for NPCs in range on Big Up action (0-100 scale). */
    public static final int BORED_NEED_REDUCTION = 20;

    /** Police detection percentage added per Black Market Shout-Out. */
    public static final float POLICE_DETECTION_PER_SHOUTOUT = 5f;

    /** Blocks within which police NPCs trigger instant wanted level increase. */
    public static final float POLICE_DETECTION_RANGE = 10f;

    /** Listener loot table — items a LISTENER NPC may carry. */
    public static final ragamuffin.building.Material[] LISTENER_LOOT_TABLE = {
        ragamuffin.building.Material.CAN_OF_LAGER,
        ragamuffin.building.Material.CIGARETTE,
        ragamuffin.building.Material.COIN,
        ragamuffin.building.Material.TOBACCO_POUCH,
        ragamuffin.building.Material.SCRATCH_CARD,
        ragamuffin.building.Material.STOLEN_PHONE,
        ragamuffin.building.Material.NEWSPAPER,
        ragamuffin.building.Material.WOOLLY_HAT_ECONOMY,
        ragamuffin.building.Material.PRESCRIPTION_MEDS
    };

    // ── Broadcast action enum ─────────────────────────────────────────────────

    public enum BroadcastAction {
        BIG_UP_THE_AREA(1,
            "Big Up the Area",
            "Nothing unites a community like a good tune."),
        SLAG_OFF_FACTION(2,
            "Slag Off a Faction",
            "Somebody's going to be very unhappy with you."),
        BLACK_MARKET_SHOUTOUT(3,
            "Black Market Shout-Out",
            "They're coming. With stuff."),
        COUNCIL_DISS_TRACK(4,
            "Council Diss Track",
            "They'll know your name by morning. Unfortunately.");

        private final int number;
        private final String displayName;
        private final String tooltip;

        BroadcastAction(int number, String displayName, String tooltip) {
            this.number = number;
            this.displayName = displayName;
            this.tooltip = tooltip;
        }

        public int getNumber() { return number; }
        public String getDisplayName() { return displayName; }
        public String getTooltip() { return tooltip; }

        public static BroadcastAction fromNumber(int n) {
            for (BroadcastAction a : values()) {
                if (a.number == n) return a;
            }
            return null;
        }
    }

    // ── Achievement callback interface ────────────────────────────────────────

    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private boolean broadcasting;
    private boolean tapeActive;          // BROADCAST_TAPE auto-broadcasting
    private int powerLevel;              // 1-4
    private float triangulation;         // 0 to 100
    private float actionTimer;           // countdown to next action choice
    private float tapeTimer;             // countdown to tape auto-broadcast
    private float cumulativeBroadcastMinutes; // for PIRATE_FM achievement
    private boolean signalVanSpawned;
    private boolean signalVanConfiscated;
    private boolean actionPending;       // player must choose an action
    private BroadcastAction lastAction;
    private float policeDetection;       // cumulative % from Shout-Outs (resets at broadcast end)

    // Active LISTENER NPCs (tracked externally by NPCManager; count stored here)
    private int listenerCount;

    // Attached systems (may be null)
    private FactionSystem factionSystem;
    private RumourNetwork rumourNetwork;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private AchievementCallback achievementCallback;

    private final Random random;

    // Transmitter world position (set when transmitter placed)
    private float transmitterX;
    private float transmitterY;
    private float transmitterZ;
    private boolean transmitterPlaced;

    // Pulse timer for ON AIR indicator (2 Hz blink = 0.5s period)
    private float pulseTimer;
    private boolean pulseVisible;

    // Faction cycle for Slag Off (cycles through factions)
    private int slagOffFactionIndex;

    // ── Constructor ───────────────────────────────────────────────────────────

    public PirateRadioSystem(Random random) {
        this.random = random;
        this.broadcasting = false;
        this.tapeActive = false;
        this.powerLevel = 1;
        this.triangulation = 0f;
        this.actionTimer = ACTION_INTERVAL_SECONDS;
        this.tapeTimer = TAPE_AUTO_BROADCAST_INTERVAL;
        this.cumulativeBroadcastMinutes = 0f;
        this.signalVanSpawned = false;
        this.signalVanConfiscated = false;
        this.actionPending = false;
        this.lastAction = null;
        this.policeDetection = 0f;
        this.listenerCount = 0;
        this.transmitterPlaced = false;
        this.pulseTimer = 0f;
        this.pulseVisible = false;
        this.slagOffFactionIndex = 0;
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setAchievementCallback(AchievementCallback callback) {
        this.achievementCallback = callback;
    }

    // ── Transmitter placement ─────────────────────────────────────────────────

    /** Called when the player places a TRANSMITTER block in the world. */
    public void onTransmitterPlaced(float x, float y, float z) {
        this.transmitterX = x;
        this.transmitterY = y;
        this.transmitterZ = z;
        this.transmitterPlaced = true;
        this.powerLevel = 1;
    }

    /** Called when the transmitter is confiscated or destroyed. */
    public void onTransmitterRemoved() {
        stopBroadcast();
        deactivateTape();
        this.transmitterPlaced = false;
        this.powerLevel = 1;
    }

    /** Whether a transmitter has been placed in the world. */
    public boolean isTransmitterPlaced() {
        return transmitterPlaced;
    }

    // ── Broadcast control ─────────────────────────────────────────────────────

    /**
     * Start broadcasting. Player must be within 2 blocks of the transmitter
     * and holding a MICROPHONE. Call this when B is pressed.
     *
     * @param playerX player world X (for LKP used by WantedSystem)
     * @param playerY player world Y
     * @param playerZ player world Z
     * @return true if broadcast started, false if prerequisites not met
     */
    public boolean startBroadcast(float playerX, float playerY, float playerZ) {
        if (!transmitterPlaced || broadcasting) return false;
        broadcasting = true;
        actionTimer = ACTION_INTERVAL_SECONDS;
        actionPending = false;
        policeDetection = 0f;

        // Notoriety +5 on session start
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(NOTORIETY_ON_START, achievementCallback != null
                ? achievementCallback::award : null);
        }

        // Achievement: ON_AIR on first broadcast
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.ON_AIR);
        }

        return true;
    }

    /**
     * Start broadcasting without specific player position (for testing).
     */
    public boolean startBroadcast() {
        return startBroadcast(transmitterX, transmitterY, transmitterZ);
    }

    /**
     * Stop broadcasting. Resets triangulation to 0. Call this when B is pressed
     * again or the player moves away from the transmitter.
     */
    public void stopBroadcast() {
        if (!broadcasting) return;
        broadcasting = false;
        triangulation = 0f;
        actionPending = false;
        policeDetection = 0f;
    }

    /** Whether the player is currently broadcasting. */
    public boolean isBroadcasting() {
        return broadcasting;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Update the pirate radio system. Call once per frame.
     *
     * @param delta        seconds since last frame
     * @param nearbyNpcs   all NPCs currently in range of the transmitter
     * @param policeNearby whether a police NPC is within {@link #POLICE_DETECTION_RANGE} blocks
     * @param playerX      player X for WantedSystem LKP
     * @param playerY      player Y
     * @param playerZ      player Z
     */
    public void update(float delta, List<NPC> nearbyNpcs, boolean policeNearby,
                       float playerX, float playerY, float playerZ) {
        // Pulse timer: 2 Hz blink = toggle every 0.25 seconds
        pulseTimer += delta;
        if (pulseTimer >= 0.25f) {
            pulseTimer -= 0.25f;
            pulseVisible = !pulseVisible;
        }

        if (!broadcasting && !tapeActive) return;

        // Police nearby → instant wanted level +1
        if (policeNearby && wantedSystem != null) {
            wantedSystem.addWantedStars(1, playerX, playerY, playerZ, achievementCallback != null
                ? achievementCallback::award : null);
        }

        if (broadcasting) {
            updateBroadcasting(delta, nearbyNpcs);
        } else if (tapeActive) {
            updateTapeBroadcast(delta, nearbyNpcs);
        }
    }

    /**
     * Overload without player position (for backward compatibility and testing).
     */
    public void update(float delta, List<NPC> nearbyNpcs, boolean policeNearby) {
        update(delta, nearbyNpcs, policeNearby, transmitterX, transmitterY, transmitterZ);
    }

    private void updateBroadcasting(float delta, List<NPC> nearbyNpcs) {
        // Accumulate broadcast minutes for PIRATE_FM achievement
        cumulativeBroadcastMinutes += delta / 60f;
        if (achievementCallback != null
                && cumulativeBroadcastMinutes >= PIRATE_FM_MINUTES_TARGET) {
            achievementCallback.award(AchievementType.PIRATE_FM);
        }

        // Advance triangulation
        float rate = TRIANGULATION_RATE[powerLevel];
        triangulation += rate * delta;
        if (triangulation > TRIANGULATION_MAX) {
            triangulation = TRIANGULATION_MAX;
        }

        // Signal Van spawn at 100%
        if (triangulation >= TRIANGULATION_MAX && !signalVanSpawned) {
            signalVanSpawned = true;
            // Actual van spawning is handled by the game loop observing isSignalVanSpawned()
        }

        // Action timer
        actionTimer -= delta;
        if (actionTimer <= 0f && !actionPending) {
            actionPending = true;
            actionTimer = ACTION_INTERVAL_SECONDS;
        }
    }

    private void updateTapeBroadcast(float delta, List<NPC> nearbyNpcs) {
        // Double triangulation rate for unattended tape
        float rate = TRIANGULATION_RATE[powerLevel] * TAPE_TRIANGULATION_MULTIPLIER;
        triangulation += rate * delta;
        if (triangulation > TRIANGULATION_MAX) {
            triangulation = TRIANGULATION_MAX;
        }

        if (triangulation >= TRIANGULATION_MAX && !signalVanSpawned) {
            signalVanSpawned = true;
        }

        tapeTimer -= delta;
        if (tapeTimer <= 0f) {
            tapeTimer = TAPE_AUTO_BROADCAST_INTERVAL;
            if (lastAction != null) {
                executeAction(lastAction, nearbyNpcs, TAPE_EFFECTIVENESS);
            }
        }
    }

    // ── Broadcast action execution ────────────────────────────────────────────

    /**
     * Execute a broadcast action chosen by the player.
     *
     * @param action     the action to perform
     * @param nearbyNpcs NPCs in transmitter range
     * @return tooltip text for the chosen action
     */
    public String executePlayerAction(BroadcastAction action, List<NPC> nearbyNpcs) {
        actionPending = false;
        lastAction = action;
        executeAction(action, nearbyNpcs, 1.0f);
        return action.getTooltip();
    }

    private void executeAction(BroadcastAction action, List<NPC> nearbyNpcs, float effectiveness) {
        switch (action) {
            case BIG_UP_THE_AREA:
                executeBigUpTheArea(nearbyNpcs, effectiveness);
                break;
            case SLAG_OFF_FACTION:
                executeSlagOffFaction(nearbyNpcs, effectiveness);
                break;
            case BLACK_MARKET_SHOUTOUT:
                executeBlackMarketShoutout(nearbyNpcs, effectiveness);
                break;
            case COUNCIL_DISS_TRACK:
                executeCouncilDissTrack(nearbyNpcs, effectiveness);
                break;
        }

        // Seed rumour into nearby NPCs on every action
        if (rumourNetwork != null && !nearbyNpcs.isEmpty()) {
            Rumour rumour = new Rumour(RumourType.GANG_ACTIVITY,
                "Pirate FM is broadcasting — " + action.getDisplayName());
            int seedCount = Math.min(3, nearbyNpcs.size());
            for (int i = 0; i < seedCount; i++) {
                rumourNetwork.addRumour(nearbyNpcs.get(i), rumour);
            }
        }
    }

    private void executeBigUpTheArea(List<NPC> nearbyNpcs, float effectiveness) {
        // All factions: Respect +3 (scaled by effectiveness)
        if (factionSystem != null) {
            int delta = Math.round(RESPECT_BIG_UP_ALL * effectiveness);
            for (Faction f : Faction.values()) {
                factionSystem.applyRespectDelta(f, delta);
            }
        }
        // All NPCs in range enter IDLE / relaxed state (BORED need satisfaction
        // is handled by the NPC need system externally via the nearbyNpcs list)
        // We signal this by setting them to IDLE state briefly
        for (NPC npc : nearbyNpcs) {
            if (npc.isAlive() && npc.getState() == NPCState.WANDERING) {
                npc.setState(NPCState.IDLE);
            }
        }
    }

    private void executeSlagOffFaction(List<NPC> nearbyNpcs, float effectiveness) {
        // Cycle through factions to determine target
        Faction target = getNextSlagOffTarget();
        if (factionSystem != null) {
            int targetDelta = Math.round(RESPECT_SLAG_OFF_TARGET * effectiveness);
            int rivalDelta  = Math.round(RESPECT_SLAG_OFF_RIVALS * effectiveness);
            for (Faction f : Faction.values()) {
                if (f == target) {
                    factionSystem.applyRespectDelta(f, targetDelta);
                } else {
                    factionSystem.applyRespectDelta(f, rivalDelta);
                }
            }
        }
        // Targeted faction's NPCs in range flee
        for (NPC npc : nearbyNpcs) {
            if (npc.isAlive() && isFactionNpc(npc, target)) {
                npc.setState(NPCState.FLEEING);
            }
        }
    }

    private Faction getNextSlagOffTarget() {
        Faction[] factions = Faction.values();
        if (factions.length == 0) return null;
        Faction target = factions[slagOffFactionIndex % factions.length];
        slagOffFactionIndex++;
        return target;
    }

    /**
     * Rough heuristic: map faction to NPC types.
     * MARCHETTI_CREW → FACTION_LIEUTENANT (Marchetti), STREET_LADS → STREET_LAD, THE_COUNCIL → COUNCIL_MEMBER.
     */
    private boolean isFactionNpc(NPC npc, Faction faction) {
        if (faction == null) return false;
        switch (faction) {
            case MARCHETTI_CREW:
                return npc.getType() == NPCType.FACTION_LIEUTENANT;
            case STREET_LADS:
                return npc.getType() == NPCType.STREET_LAD
                    || npc.getType() == NPCType.YOUTH_GANG;
            case THE_COUNCIL:
                return npc.getType() == NPCType.COUNCIL_MEMBER
                    || npc.getType() == NPCType.COUNCIL_BUILDER;
            default:
                return false;
        }
    }

    private void executeBlackMarketShoutout(List<NPC> nearbyNpcs, float effectiveness) {
        // Spawn request: game loop will create LISTENER NPCs on seeing spawnListenerRequest > 0
        int toSpawn = random.nextInt(3) + 1; // 1-3
        int available = MAX_LISTENERS - listenerCount;
        toSpawn = Math.min(toSpawn, available);
        int actualSpawned = (int) Math.round(toSpawn * effectiveness);
        if (actualSpawned > 0) {
            listenerSpawnRequest = actualSpawned;
        }

        // Police detection +5% cumulative (resets at broadcast end)
        policeDetection += POLICE_DETECTION_PER_SHOUTOUT;

        // Check THE_PEOPLE_S_DJ achievement: 6 listeners simultaneously
        if (listenerCount >= MAX_LISTENERS && achievementCallback != null) {
            achievementCallback.award(AchievementType.THE_PEOPLE_S_DJ);
        }
    }

    private void executeCouncilDissTrack(List<NPC> nearbyNpcs, float effectiveness) {
        // Council Respect -15
        if (factionSystem != null) {
            int delta = Math.round(RESPECT_COUNCIL_DISS * effectiveness);
            factionSystem.applyRespectDelta(Faction.THE_COUNCIL, delta);
        }
        // Notoriety +10
        if (notorietySystem != null) {
            int gain = Math.round(NOTORIETY_COUNCIL_DISS * effectiveness);
            notorietySystem.addNotoriety(gain, achievementCallback != null
                ? achievementCallback::award : null);
        }
        // Council Cleaners in range move faster — flagged for game loop to apply
        councilCleanerSpeedBoost = true;
    }

    // ── LISTENER NPC spawn request ────────────────────────────────────────────

    /** Number of LISTENER NPCs the game loop should spawn on next update check. */
    private int listenerSpawnRequest = 0;

    /** Whether Council Cleaners in range should have doubled speed applied. */
    private boolean councilCleanerSpeedBoost = false;

    /**
     * Number of LISTENER NPCs the game loop should spawn on this update.
     * Clears after reading.
     */
    public int consumeListenerSpawnRequest() {
        int req = listenerSpawnRequest;
        listenerSpawnRequest = 0;
        return req;
    }

    /**
     * Whether the game loop should apply 2x speed to Council Cleaners in range.
     * Clears after reading.
     */
    public boolean consumeCouncilCleanerSpeedBoost() {
        boolean boost = councilCleanerSpeedBoost;
        councilCleanerSpeedBoost = false;
        return boost;
    }

    /** Update the active listener count (called by NPCManager each frame). */
    public void setListenerCount(int count) {
        this.listenerCount = Math.min(count, MAX_LISTENERS);
    }

    /** Current listener count (as reported by NPCManager). */
    public int getListenerCount() {
        return listenerCount;
    }

    // ── Signal Van ────────────────────────────────────────────────────────────

    /**
     * Called when the Signal Van reaches within 6 blocks of the transmitter.
     * Confiscates the transmitter and awards OFF_AIR achievement.
     */
    public void onSignalVanArrived() {
        signalVanConfiscated = true;
        stopBroadcast();
        tapeActive = false;
        transmitterPlaced = false;
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.OFF_AIR);
        }
    }

    /**
     * Called when the player destroys the Signal Van.
     * Resets triangulation, awards SIGNAL_JAM, adds +20 Notoriety.
     */
    public void onSignalVanDestroyed() {
        triangulation = 0f;
        signalVanSpawned = false;
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(NOTORIETY_VAN_DESTROYED, achievementCallback != null
                ? achievementCallback::award : null);
        }
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.SIGNAL_JAM);
        }
    }

    /** Whether a Signal Van has been spawned (triangulation = 100%). */
    public boolean isSignalVanSpawned() {
        return signalVanSpawned;
    }

    /** Whether the transmitter was confiscated by the Signal Van. */
    public boolean isSignalVanConfiscated() {
        return signalVanConfiscated;
    }

    /** Reset signal van spawned state (e.g. after van is destroyed). */
    public void resetSignalVan() {
        this.signalVanSpawned = false;
    }

    // ── Transmitter upgrades ──────────────────────────────────────────────────

    /**
     * Attempt to upgrade the transmitter's power level by consuming components.
     *
     * <p>Upgrade requirements:
     * <ul>
     *   <li>Level 1 → 2: 1 COMPUTER consumed</li>
     *   <li>Level 2 → 3: 1 STOLEN_PHONE + 1 COMPUTER consumed</li>
     *   <li>Level 3 → 4: 2 COMPUTER + 1 PETROL_CAN consumed</li>
     * </ul>
     *
     * @param availableComputers  number of COMPUTER items player is contributing
     * @param hasStolenPhone      true if player is contributing a STOLEN_PHONE
     * @param hasPetrolCan        true if player is contributing a PETROL_CAN
     * @return new power level after upgrade attempt (unchanged if requirements not met)
     */
    public int tryUpgrade(int availableComputers, boolean hasStolenPhone, boolean hasPetrolCan) {
        switch (powerLevel) {
            case 1:
                if (availableComputers >= 1) {
                    powerLevel = 2;
                }
                break;
            case 2:
                if (hasStolenPhone && availableComputers >= 1) {
                    powerLevel = 3;
                }
                break;
            case 3:
                if (availableComputers >= 2 && hasPetrolCan) {
                    powerLevel = 4;
                }
                break;
            default:
                // Level 4 is max; no upgrade possible
                break;
        }
        return powerLevel;
    }

    /** Current power level (1–4). */
    public int getPowerLevel() {
        return powerLevel;
    }

    /** Set power level directly (for testing). */
    public void setPowerLevel(int level) {
        if (level >= 1 && level <= 4) {
            this.powerLevel = level;
        }
    }

    /** Broadcast range in blocks for the current power level. */
    public int getBroadcastRange() {
        return POWER_RANGES[powerLevel];
    }

    // ── BROADCAST_TAPE ────────────────────────────────────────────────────────

    /**
     * Activate the BROADCAST_TAPE auto-broadcast mode with a pre-recorded action.
     * Deactivates live broadcasting; the tape broadcasts at half effectiveness.
     *
     * @param recordedAction the action to repeat (must not be null)
     * @return true if the tape was activated
     */
    public boolean activateTape(BroadcastAction recordedAction) {
        if (!transmitterPlaced || recordedAction == null) return false;
        this.lastAction = recordedAction;
        this.tapeActive = true;
        this.tapeTimer = TAPE_AUTO_BROADCAST_INTERVAL;
        // Tape replaces live broadcast
        if (broadcasting) {
            broadcasting = false;
            triangulation = 0f;
        }
        return true;
    }

    /** Deactivate the BROADCAST_TAPE auto-broadcast. Resets triangulation. */
    public void deactivateTape() {
        if (!tapeActive) return;
        tapeActive = false;
        triangulation = 0f;
        tapeTimer = TAPE_AUTO_BROADCAST_INTERVAL;
    }

    /** Whether the BROADCAST_TAPE auto-broadcast is running. */
    public boolean isTapeActive() {
        return tapeActive;
    }

    // ── Newspaper / infamy integration ────────────────────────────────────────

    /**
     * Called by the NewspaperSystem when a "RAGAMUFFIN FM: ENEMY OF THE STATE?"
     * headline runs. Unlocks the ENEMY_OF_THE_STATE achievement.
     */
    public void onEnemyOfStateHeadline() {
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.ENEMY_OF_THE_STATE);
        }
    }

    // ── HUD data ──────────────────────────────────────────────────────────────

    /**
     * Whether the ON AIR indicator should currently be shown.
     * Pulses at 2 Hz (alternates every 0.25 seconds) while broadcasting or tape is active.
     */
    public boolean isOnAirVisible() {
        return (broadcasting || tapeActive) && pulseVisible;
    }

    /** Whether broadcasting or tape is active at all (for HUD show/hide). */
    public boolean isActive() {
        return broadcasting || tapeActive;
    }

    /** Triangulation progress (0.0 to 100.0). */
    public float getTriangulation() {
        return triangulation;
    }

    /** Whether a triangulation warning should be shown (≥ 80%). */
    public boolean isTriangulationWarning() {
        return triangulation >= TRIANGULATION_WARN;
    }

    /** Whether a broadcast action choice is pending this cycle. */
    public boolean isActionPending() {
        return actionPending;
    }

    /** The last action performed (may be null on first broadcast). */
    public BroadcastAction getLastAction() {
        return lastAction;
    }

    /** Cumulative broadcast minutes (for PIRATE_FM achievement tracking). */
    public float getCumulativeBroadcastMinutes() {
        return cumulativeBroadcastMinutes;
    }

    /** Set cumulative broadcast minutes (for testing). */
    public void setCumulativeBroadcastMinutes(float minutes) {
        this.cumulativeBroadcastMinutes = minutes;
    }

    /** Whether the pulse is currently in the visible phase (for ON AIR blink). */
    public boolean isPulseVisible() {
        return pulseVisible;
    }

    /** Current cumulative police detection from Shout-Outs (resets on broadcast end). */
    public float getPoliceDetection() {
        return policeDetection;
    }

    /** Force-set triangulation for testing. */
    public void setTriangulation(float value) {
        this.triangulation = Math.max(0f, Math.min(TRIANGULATION_MAX, value));
    }

    /** Transmitter world X coordinate. */
    public float getTransmitterX() { return transmitterX; }

    /** Transmitter world Y coordinate. */
    public float getTransmitterY() { return transmitterY; }

    /** Transmitter world Z coordinate. */
    public float getTransmitterZ() { return transmitterZ; }
}

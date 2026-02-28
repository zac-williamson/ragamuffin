package ragamuffin.core;

import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.PropPosition;
import ragamuffin.world.PropType;
import ragamuffin.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #924: Launderette System — Wash Clothes, Scrub Notoriety &amp; Launderette Drama.
 *
 * <p>Gives the {@link LandmarkType#LAUNDERETTE} landmark actual gameplay:
 * <ul>
 *   <li>Player interacts (E) with a {@link PropType#WASHING_MACHINE} to start a
 *       90-second wash cycle costing 2 COIN. On completion, receives
 *       {@link Material#CLEAN_CLOTHES}.</li>
 *   <li>If the player's current disguise (via {@link DisguiseSystem}) is a
 *       {@link Material#BLOODY_HOODIE} or {@link Material#STOLEN_JACKET}, the wash
 *       also deducts 2 Notoriety from {@link NotorietySystem} and clears the
 *       COVERED_IN_BLOOD debuff.</li>
 *   <li>Waiting NPCs (1–3 {@code PUBLIC}) inside share rumours freely without
 *       requiring {@code drunkTimer > 0} — the launderette is a loose-tongue zone.
 *       These NPCs start with {@link NeedType#BORED} at 60.</li>
 *   <li>Random events (30% chance every 5 in-game minutes):
 *       {@code MACHINE_STOLEN}, {@code SOAP_SPILL}, {@code POWER_CUT},
 *       {@code SUSPICIOUS_LOAD}.</li>
 *   <li>Using the {@link PropType#CHANGING_CUBICLE} prop after collecting
 *       {@link Material#CLEAN_CLOTHES} grants the FRESHLY_LAUNDERED buff.</li>
 *   <li>Opening hours: 07:00–22:00. A {@link PropType#CLOSED_SIGN} blocks the
 *       door outside hours; entering after-hours triggers
 *       {@link NeighbourhoodWatchSystem#ANGER_VISIBLE_CRIME}.</li>
 * </ul>
 */
public class LaunderetteSystem {

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Hour at which the launderette opens. */
    public static final float OPEN_HOUR  = 7.0f;

    /** Hour at which the launderette closes (exclusive). */
    public static final float CLOSE_HOUR = 22.0f;

    // ── Wash cycle ────────────────────────────────────────────────────────────

    /** Real-seconds for a full wash cycle (90 in-game seconds at standard time speed). */
    public static final float WASH_CYCLE_DURATION = 90.0f;

    /** Cost in COIN to start a wash cycle. */
    public static final int WASH_CYCLE_COST = 2;

    /** Notoriety reduction when washing a BLOODY_HOODIE or STOLEN_JACKET. */
    public static final int NOTORIETY_SCRUB_AMOUNT = 2;

    // ── FRESHLY_LAUNDERED buff ────────────────────────────────────────────────

    /**
     * Duration of the FRESHLY_LAUNDERED buff in real seconds.
     * (3 in-game minutes × ~50 real-seconds/in-game-minute ≈ 150 real seconds.)
     */
    public static final float FRESHLY_LAUNDERED_DURATION = 150.0f;

    /** NPC recognition chance multiplier while FRESHLY_LAUNDERED buff is active. */
    public static final float FRESHLY_LAUNDERED_RECOGNITION_MULT = 0.80f; // −20%

    // ── Random event ──────────────────────────────────────────────────────────

    /** In-game minutes between random event rolls. */
    public static final float RANDOM_EVENT_INTERVAL_MINUTES = 5.0f;

    /** Probability of a random event each interval (0–1). */
    public static final float RANDOM_EVENT_CHANCE = 0.30f;

    /** Duration of the SOAP_SPILL movement debuff in real seconds. */
    public static final float SOAP_SPILL_DURATION = 60.0f;

    /** Player movement speed multiplier during SOAP_SPILL. */
    public static final float SOAP_SPILL_SPEED_MULT = 0.70f; // 70%

    /** Seconds the wash cycle timer is paused during POWER_CUT. */
    public static final float POWER_CUT_PAUSE_DURATION = 30.0f;

    /** Cost in COIN for the FENCE to sell a STOLEN_JACKET during SUSPICIOUS_LOAD. */
    public static final int SUSPICIOUS_LOAD_JACKET_COST = 5;

    /** WatchAnger reduction when brokering peace during MACHINE_STOLEN. */
    public static final int MACHINE_STOLEN_PEACE_ANGER_REDUCTION = 5;

    // ── NPC management ────────────────────────────────────────────────────────

    /** Minimum PUBLIC NPCs waiting inside the launderette. */
    public static final int WAITING_NPC_MIN = 1;

    /** Maximum PUBLIC NPCs waiting inside the launderette. */
    public static final int WAITING_NPC_MAX = 3;

    /** Starting BORED need value for launderette waiting NPCs. */
    public static final int WAITING_NPC_BORED_START = 60;

    // ── Achievement thresholds ────────────────────────────────────────────────

    /** Number of notoriety scrubs needed for SMELLS_LIKE_CLEAN_SPIRIT achievement. */
    public static final int NOTORIETY_SCRUBS_FOR_ACHIEVEMENT = 3;

    // ── In-game time conversion ───────────────────────────────────────────────

    /** Real seconds per in-game minute (TimeSystem uses 0.1 hours/real-second → 6 min/s). */
    private static final float IN_GAME_MINUTES_PER_REAL_SECOND = 6.0f;

    // ── Random event types ────────────────────────────────────────────────────

    /**
     * The types of random events that can occur in the launderette.
     */
    public enum RandomEvent {
        /** An NPC turns AGGRESSIVE over a stolen machine; player can broker peace. */
        MACHINE_STOLEN,
        /** Floor becomes slippery; player movement reduced to 70% for 60s. */
        SOAP_SPILL,
        /** Wash cycle timer pauses for 30s. */
        POWER_CUT,
        /** A FENCE NPC briefly appears selling a STOLEN_JACKET for 5 COIN. */
        SUSPICIOUS_LOAD
    }

    /**
     * Result of interacting with the WASHING_MACHINE prop.
     */
    public enum WashStartResult {
        /** Wash cycle started successfully. */
        SUCCESS,
        /** Launderette is closed (outside 07:00–22:00). */
        CLOSED,
        /** Player does not have enough COIN. */
        NOT_ENOUGH_COIN,
        /** A wash cycle is already in progress. */
        ALREADY_RUNNING,
        /** Wash cycle is paused by POWER_CUT. */
        POWER_CUT_ACTIVE
    }

    /**
     * Result of interacting with the CHANGING_CUBICLE prop.
     */
    public enum ChangingCubicleResult {
        /** FRESHLY_LAUNDERED buff applied. */
        BUFF_APPLIED,
        /** Player does not have CLEAN_CLOTHES. */
        NO_CLEAN_CLOTHES,
        /** Launderette is closed. */
        CLOSED
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Whether a wash cycle is currently in progress. */
    private boolean washCycleActive = false;

    /** Remaining real seconds in the current wash cycle. */
    private float washCycleTimer = 0f;

    /** Whether the wash cycle is currently paused by POWER_CUT. */
    private boolean washCyclePaused = false;

    /** Remaining real seconds of POWER_CUT pause. */
    private float powerCutTimer = 0f;

    /** Remaining real seconds of the FRESHLY_LAUNDERED buff (0 = not active). */
    private float freshlyLaunderedTimer = 0f;

    /** Remaining real seconds of the SOAP_SPILL debuff (0 = not active). */
    private float soapSpillTimer = 0f;

    /** Accumulated real seconds toward next random event check. */
    private float randomEventAccumulator = 0f;

    /** The current active random event, or null if none. */
    private RandomEvent activeRandomEvent = null;

    /** The AGGRESSIVE NPC spawned during MACHINE_STOLEN event (null if none). */
    private NPC machineStolenNpc = null;

    /** The FENCE NPC spawned during SUSPICIOUS_LOAD event (null if none). */
    private NPC suspiciousLoadFenceNpc = null;

    /** Remaining real seconds the FENCE NPC lingers during SUSPICIOUS_LOAD. */
    private float suspiciousLoadTimer = 0f;

    /** Duration (real seconds) the FENCE NPC stays during SUSPICIOUS_LOAD. */
    private static final float SUSPICIOUS_LOAD_FENCE_DURATION = 120.0f;

    /** Whether the closed-sign prop is currently placed at the door. */
    private boolean closedSignActive = false;

    /** Index into world's prop list of the active CLOSED_SIGN prop (-1 = none). */
    private int closedSignPropIndex = -1;

    /** NPCs currently managed by this system as waiting launderette patrons. */
    private final List<NPC> waitingNpcs = new ArrayList<>();

    /** Whether waiting NPCs are currently spawned. */
    private boolean npcsSpawned = false;

    // ── Achievement tracking ──────────────────────────────────────────────────

    /** Number of notoriety scrubs performed (for SMELLS_LIKE_CLEAN_SPIRIT). */
    private int notorityScrubCount = 0;

    /** Whether FRESH_START has been awarded. */
    private boolean freshStartAwarded = false;

    /** Number of peace-brokered events (for PEACEKEEPER_OF_SUDWORTH). */
    private int peacekeeperCount = 0;

    // ── Wash result tracking ──────────────────────────────────────────────────

    /** Whether the last completed wash scrubbed notoriety. */
    private boolean lastWashScrubbedNotoriety = false;

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final Random random;

    // ── Constructor ───────────────────────────────────────────────────────────

    public LaunderetteSystem() {
        this(new Random());
    }

    public LaunderetteSystem(Random random) {
        this.random = random;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Update the launderette system. Call once per frame.
     *
     * @param delta           seconds since last frame
     * @param timeSystem      game time (for opening hours and event scheduling)
     * @param world           the world (for prop management)
     * @param npcManager      NPC manager (for spawning)
     * @param inventory       player inventory (for wash completion)
     * @param notorietySystem notoriety system (for scrub on wash)
     * @param watchSystem     neighbourhood watch system (for after-hours entry events)
     * @param disguiseSystem  disguise system (for checking active disguise)
     * @param achievementSystem achievement callback
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       World world,
                       NPCManager npcManager,
                       Inventory inventory,
                       NotorietySystem notorietySystem,
                       NeighbourhoodWatchSystem watchSystem,
                       DisguiseSystem disguiseSystem,
                       NotorietySystem.AchievementCallback achievementSystem) {

        float hour = (timeSystem != null) ? timeSystem.getTime() : 12.0f; // default midday when null (tests)
        boolean isOpen = isOpen(hour);

        // Manage CLOSED_SIGN prop
        updateClosedSign(isOpen, world);

        // Manage waiting NPC spawns (only while open)
        if (isOpen && !npcsSpawned) {
            spawnWaitingNpcs(world, npcManager);
        } else if (!isOpen && npcsSpawned) {
            despawnWaitingNpcs();
        }

        // Advance POWER_CUT timer
        if (powerCutTimer > 0f) {
            powerCutTimer = Math.max(0f, powerCutTimer - delta);
            if (powerCutTimer <= 0f) {
                washCyclePaused = false;
            }
        }

        // Advance wash cycle timer (only if active and not paused)
        if (washCycleActive && !washCyclePaused) {
            washCycleTimer -= delta;
            if (washCycleTimer <= 0f) {
                washCycleTimer = 0f;
                completWashCycle(inventory, notorietySystem, disguiseSystem, achievementSystem);
            }
        }

        // Advance FRESHLY_LAUNDERED buff timer
        if (freshlyLaunderedTimer > 0f) {
            freshlyLaunderedTimer = Math.max(0f, freshlyLaunderedTimer - delta);
        }

        // Advance SOAP_SPILL debuff timer
        if (soapSpillTimer > 0f) {
            soapSpillTimer = Math.max(0f, soapSpillTimer - delta);
        }

        // Advance random event accumulator (only while open)
        if (isOpen) {
            randomEventAccumulator += delta * IN_GAME_MINUTES_PER_REAL_SECOND;
            float intervalMinutes = RANDOM_EVENT_INTERVAL_MINUTES;
            if (randomEventAccumulator >= intervalMinutes) {
                randomEventAccumulator -= intervalMinutes;
                if (activeRandomEvent == null && random.nextFloat() < RANDOM_EVENT_CHANCE) {
                    triggerRandomEvent(world, npcManager);
                }
            }
        }

        // Advance SUSPICIOUS_LOAD fence timer
        if (suspiciousLoadTimer > 0f) {
            suspiciousLoadTimer = Math.max(0f, suspiciousLoadTimer - delta);
            if (suspiciousLoadTimer <= 0f && suspiciousLoadFenceNpc != null) {
                suspiciousLoadFenceNpc.setState(NPCState.FLEEING);
                suspiciousLoadFenceNpc = null;
                if (activeRandomEvent == RandomEvent.SUSPICIOUS_LOAD) {
                    activeRandomEvent = null;
                }
            }
        }
    }

    // ── Wash cycle ────────────────────────────────────────────────────────────

    /**
     * Called when the player interacts (E) with the {@link PropType#WASHING_MACHINE} prop.
     *
     * @param hour      current in-game hour (for open/closed check)
     * @param inventory player inventory (must have ≥ 2 COIN)
     * @return the result of the attempt to start the wash cycle
     */
    public WashStartResult interactWithWashingMachine(float hour, Inventory inventory) {
        if (!isOpen(hour)) {
            return WashStartResult.CLOSED;
        }
        if (washCycleActive) {
            if (washCyclePaused) {
                return WashStartResult.POWER_CUT_ACTIVE;
            }
            return WashStartResult.ALREADY_RUNNING;
        }
        if (inventory.getItemCount(Material.COIN) < WASH_CYCLE_COST) {
            return WashStartResult.NOT_ENOUGH_COIN;
        }

        inventory.removeItem(Material.COIN, WASH_CYCLE_COST);
        washCycleActive = true;
        washCycleTimer = WASH_CYCLE_DURATION;
        washCyclePaused = false;
        lastWashScrubbedNotoriety = false;
        return WashStartResult.SUCCESS;
    }

    /**
     * Called when the wash cycle completes. Adds CLEAN_CLOTHES to inventory.
     * If the player's active disguise is BLOODY_HOODIE or STOLEN_JACKET, deducts
     * 2 Notoriety and clears the COVERED_IN_BLOOD debuff.
     */
    private void completWashCycle(Inventory inventory,
                                   NotorietySystem notorietySystem,
                                   DisguiseSystem disguiseSystem,
                                   NotorietySystem.AchievementCallback achievementSystem) {
        washCycleActive = false;
        washCycleTimer = 0f;

        if (inventory != null) {
            inventory.addItem(Material.CLEAN_CLOTHES, 1);
        }

        // Notoriety scrub for dirty disguises
        if (disguiseSystem != null && notorietySystem != null) {
            Material active = disguiseSystem.getActiveDisguise();
            if (active == Material.BLOODY_HOODIE || active == Material.STOLEN_JACKET) {
                notorietySystem.reduceNotoriety(NOTORIETY_SCRUB_AMOUNT, achievementSystem);
                lastWashScrubbedNotoriety = true;
                notorityScrubCount++;

                if (achievementSystem != null
                        && notorityScrubCount >= NOTORIETY_SCRUBS_FOR_ACHIEVEMENT) {
                    achievementSystem.award(AchievementType.SMELLS_LIKE_CLEAN_SPIRIT);
                }
            }
        }

        // FRESH_START achievement on first wash
        if (!freshStartAwarded && achievementSystem != null) {
            freshStartAwarded = true;
            achievementSystem.award(AchievementType.FRESH_START);
        }
    }

    // ── Changing cubicle ──────────────────────────────────────────────────────

    /**
     * Called when the player interacts (E) with the {@link PropType#CHANGING_CUBICLE} prop.
     *
     * @param hour      current in-game hour (for open/closed check)
     * @param inventory player inventory (must contain CLEAN_CLOTHES)
     * @return the result of the interaction
     */
    public ChangingCubicleResult interactWithChangingCubicle(float hour, Inventory inventory) {
        if (!isOpen(hour)) {
            return ChangingCubicleResult.CLOSED;
        }
        if (!inventory.hasItem(Material.CLEAN_CLOTHES, 1)) {
            return ChangingCubicleResult.NO_CLEAN_CLOTHES;
        }

        inventory.removeItem(Material.CLEAN_CLOTHES, 1);
        freshlyLaunderedTimer = FRESHLY_LAUNDERED_DURATION;
        return ChangingCubicleResult.BUFF_APPLIED;
    }

    // ── After-hours entry ─────────────────────────────────────────────────────

    /**
     * Called when the player attempts to enter the launderette outside opening hours.
     * Triggers {@link NeighbourhoodWatchSystem#ANGER_VISIBLE_CRIME} in the watch system.
     *
     * @param hour        current in-game hour
     * @param watchSystem neighbourhood watch system
     */
    public void onAfterHoursEntry(float hour, NeighbourhoodWatchSystem watchSystem) {
        if (!isOpen(hour) && watchSystem != null) {
            watchSystem.onVisibleCrime();
        }
    }

    // ── Random events ─────────────────────────────────────────────────────────

    /**
     * Trigger a random event. One of the four event types is chosen at random.
     *
     * @param world      the world
     * @param npcManager NPC manager
     */
    public void triggerRandomEvent(World world, NPCManager npcManager) {
        RandomEvent[] events = RandomEvent.values();
        RandomEvent chosen = events[random.nextInt(events.length)];
        activeRandomEvent = chosen;

        switch (chosen) {
            case MACHINE_STOLEN:
                handleMachineStolen(npcManager);
                break;
            case SOAP_SPILL:
                soapSpillTimer = SOAP_SPILL_DURATION;
                break;
            case POWER_CUT:
                if (washCycleActive) {
                    washCyclePaused = true;
                    powerCutTimer = POWER_CUT_PAUSE_DURATION;
                } else {
                    // No active cycle to pause; event still triggers atmosphere
                    powerCutTimer = POWER_CUT_PAUSE_DURATION;
                }
                break;
            case SUSPICIOUS_LOAD:
                handleSuspiciousLoad(npcManager);
                break;
        }
    }

    private void handleMachineStolen(NPCManager npcManager) {
        if (npcManager == null) return;
        // Pick a random waiting NPC to turn AGGRESSIVE, or spawn a new one
        NPC aggressor = null;
        for (NPC npc : waitingNpcs) {
            if (npc.isAlive() && npc.getType() == NPCType.PUBLIC) {
                aggressor = npc;
                break;
            }
        }
        if (aggressor == null) {
            // Spawn a new aggressor
            aggressor = npcManager.spawnNPC(NPCType.PUBLIC, 0f, 1f, 0f);
        }
        if (aggressor != null) {
            aggressor.setState(NPCState.AGGRESSIVE);
            aggressor.setSpeechText("Someone nicked my machine! I had forty minutes left!", 6.0f);
            machineStolenNpc = aggressor;
        }
    }

    private void handleSuspiciousLoad(NPCManager npcManager) {
        if (npcManager == null) return;
        NPC fence = npcManager.spawnNPC(NPCType.FENCE, 0f, 1f, 0f);
        if (fence != null) {
            fence.setSpeechText("Nice jacket. Barely worn. Five coins, no questions.", 6.0f);
            suspiciousLoadFenceNpc = fence;
            suspiciousLoadTimer = SUSPICIOUS_LOAD_FENCE_DURATION;
        }
    }

    /**
     * Called when the player brokers peace during a MACHINE_STOLEN event.
     * The aggressor NPC returns to WANDERING and WatchAnger is reduced by 5.
     *
     * @param watchSystem       neighbourhood watch system
     * @param achievementSystem achievement callback
     * @return true if peace was successfully brokered
     */
    public boolean brokerPeace(NeighbourhoodWatchSystem watchSystem,
                                NotorietySystem.AchievementCallback achievementSystem) {
        if (activeRandomEvent != RandomEvent.MACHINE_STOLEN) return false;
        if (machineStolenNpc == null || !machineStolenNpc.isAlive()) return false;

        machineStolenNpc.setState(NPCState.WANDERING);
        machineStolenNpc.setSpeechText("Alright, alright... no need for all that.", 4.0f);
        machineStolenNpc = null;
        activeRandomEvent = null;

        if (watchSystem != null) {
            watchSystem.addAnger(-MACHINE_STOLEN_PEACE_ANGER_REDUCTION);
        }

        peacekeeperCount++;
        if (achievementSystem != null) {
            achievementSystem.award(AchievementType.PEACEKEEPER_OF_SUDWORTH);
        }

        return true;
    }

    /**
     * Called when the player buys the STOLEN_JACKET from the FENCE during
     * a SUSPICIOUS_LOAD event.
     *
     * @param inventory         player inventory (must have ≥ 5 COIN)
     * @param achievementSystem achievement callback
     * @return true if the purchase succeeded
     */
    public boolean buyFromSuspiciousLoad(Inventory inventory,
                                          NotorietySystem.AchievementCallback achievementSystem) {
        if (activeRandomEvent != RandomEvent.SUSPICIOUS_LOAD) return false;
        if (suspiciousLoadFenceNpc == null || !suspiciousLoadFenceNpc.isAlive()) return false;
        if (inventory.getItemCount(Material.COIN) < SUSPICIOUS_LOAD_JACKET_COST) return false;

        inventory.removeItem(Material.COIN, SUSPICIOUS_LOAD_JACKET_COST);
        inventory.addItem(Material.STOLEN_JACKET, 1);

        if (achievementSystem != null) {
            achievementSystem.award(AchievementType.LAUNDERING);
        }

        // Fence leaves after the transaction
        suspiciousLoadFenceNpc.setState(NPCState.FLEEING);
        suspiciousLoadFenceNpc = null;
        suspiciousLoadTimer = 0f;
        activeRandomEvent = null;

        return true;
    }

    // ── Rumour network integration ─────────────────────────────────────────────

    /**
     * Returns true if the given NPC is a waiting launderette patron who may share
     * rumours freely (without requiring drunkTimer > 0).
     *
     * @param npc the NPC to check
     * @return true if the NPC is a launderette waiting patron
     */
    public boolean isLooseTongueNpc(NPC npc) {
        return waitingNpcs.contains(npc);
    }

    // ── Closed sign management ────────────────────────────────────────────────

    private void updateClosedSign(boolean isOpen, World world) {
        if (!isOpen && !closedSignActive) {
            // Place closed sign
            closedSignActive = true;
            if (world != null) {
                PropPosition sign = new PropPosition(0f, 1.5f, 0f, PropType.CLOSED_SIGN, 0f);
                world.addPropPosition(sign);
                closedSignPropIndex = world.getPropPositions().size() - 1;
            }
        } else if (isOpen && closedSignActive) {
            // Remove closed sign
            closedSignActive = false;
            if (world != null && closedSignPropIndex >= 0
                    && closedSignPropIndex < world.getPropPositions().size()) {
                world.removeProp(closedSignPropIndex);
            }
            closedSignPropIndex = -1;
        }
    }

    // ── Waiting NPC management ─────────────────────────────────────────────────

    private void spawnWaitingNpcs(World world, NPCManager npcManager) {
        if (npcsSpawned || npcManager == null) return;

        npcsSpawned = true;
        int count = WAITING_NPC_MIN + random.nextInt(WAITING_NPC_MAX - WAITING_NPC_MIN + 1);
        for (int i = 0; i < count; i++) {
            float x = (i - 1) * 1.5f; // spread them out horizontally
            NPC npc = npcManager.spawnNPC(NPCType.PUBLIC, x, 1f, 0f);
            if (npc != null) {
                npc.setSpeechText("Nothing to do round here, is there.", 3.0f);
                waitingNpcs.add(npc);
            }
        }
    }

    private void despawnWaitingNpcs() {
        for (NPC npc : waitingNpcs) {
            if (npc.isAlive()) {
                npc.setState(NPCState.FLEEING);
            }
        }
        waitingNpcs.clear();
        npcsSpawned = false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true if the launderette is open at the given in-game hour.
     *
     * @param hour in-game hour (0–24)
     * @return true if open
     */
    public boolean isOpen(float hour) {
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Whether a wash cycle is currently in progress. */
    public boolean isWashCycleActive() { return washCycleActive; }

    /** Remaining real seconds in the current wash cycle (0 if not active). */
    public float getWashCycleTimer() { return washCycleTimer; }

    /** Whether the wash cycle is currently paused by a POWER_CUT. */
    public boolean isWashCyclePaused() { return washCyclePaused; }

    /** Remaining real seconds of the FRESHLY_LAUNDERED buff (0 = not active). */
    public float getFreshlyLaunderedTimer() { return freshlyLaunderedTimer; }

    /** Whether the FRESHLY_LAUNDERED buff is currently active. */
    public boolean isFreshlyLaundered() { return freshlyLaunderedTimer > 0f; }

    /** Whether the SOAP_SPILL movement debuff is currently active. */
    public boolean isSoapSpillActive() { return soapSpillTimer > 0f; }

    /** Remaining real seconds of the SOAP_SPILL debuff. */
    public float getSoapSpillTimer() { return soapSpillTimer; }

    /** Player movement speed multiplier (1.0 normally; reduced during SOAP_SPILL). */
    public float getMovementSpeedMultiplier() {
        return isSoapSpillActive() ? SOAP_SPILL_SPEED_MULT : 1.0f;
    }

    /** The current active random event, or null if none. */
    public RandomEvent getActiveRandomEvent() { return activeRandomEvent; }

    /** The AGGRESSIVE NPC during MACHINE_STOLEN (null if not active). */
    public NPC getMachineStolenNpc() { return machineStolenNpc; }

    /** The FENCE NPC during SUSPICIOUS_LOAD (null if not active). */
    public NPC getSuspiciousLoadFenceNpc() { return suspiciousLoadFenceNpc; }

    /** Whether the CLOSED_SIGN prop is currently active. */
    public boolean isClosedSignActive() { return closedSignActive; }

    /** NPCs currently waiting inside the launderette. */
    public List<NPC> getWaitingNpcs() { return waitingNpcs; }

    /** Whether NPC patrons are currently spawned. */
    public boolean isNpcsSpawned() { return npcsSpawned; }

    /** Whether the last completed wash scrubbed notoriety. */
    public boolean isLastWashScrubbedNotoriety() { return lastWashScrubbedNotoriety; }

    /** Total notoriety scrubs performed. */
    public int getNotorityScrubCount() { return notorityScrubCount; }

    /** Total peace-brokering events. */
    public int getPeacekeeperCount() { return peacekeeperCount; }

    // ── Force-set methods for testing ─────────────────────────────────────────

    /** Force-set wash cycle state (for testing). */
    public void setWashCycleActiveForTesting(boolean active, float timer) {
        this.washCycleActive = active;
        this.washCycleTimer = timer;
    }

    /** Force-set random event (for testing). */
    public void setActiveRandomEventForTesting(RandomEvent event) {
        this.activeRandomEvent = event;
    }

    /** Force-set the machine stolen NPC (for testing). */
    public void setMachineStolenNpcForTesting(NPC npc) {
        this.machineStolenNpc = npc;
    }

    /** Force-set the suspicious load fence NPC (for testing). */
    public void setSuspiciousLoadFenceNpcForTesting(NPC npc) {
        this.suspiciousLoadFenceNpc = npc;
        this.suspiciousLoadTimer = SUSPICIOUS_LOAD_FENCE_DURATION;
    }

    /** Force-set the random event accumulator (for testing). */
    public void setRandomEventAccumulatorForTesting(float minutes) {
        this.randomEventAccumulator = minutes;
    }

    /** Force-set fresh start awarded state (for testing). */
    public void setFreshStartAwardedForTesting(boolean awarded) {
        this.freshStartAwarded = awarded;
    }

    /** Force-set notoriety scrub count (for testing). */
    public void setNotorityScrubCountForTesting(int count) {
        this.notorityScrubCount = count;
    }

    /** Force-set the FRESHLY_LAUNDERED buff timer (for testing). */
    public void setFreshlyLaunderedTimerForTesting(float seconds) {
        this.freshlyLaunderedTimer = seconds;
    }

    /** Force-set SOAP_SPILL timer (for testing). */
    public void setSoapSpillTimerForTesting(float seconds) {
        this.soapSpillTimer = seconds;
    }
}

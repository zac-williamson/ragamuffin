package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.BlockType;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.PropType;
import ragamuffin.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Orchestrates the four-phase heist mechanic (Phase O / Issue #704).
 *
 * <h3>Phases</h3>
 * <ol>
 *   <li><b>Casing</b> — player presses F inside a building to gather intel,
 *       stored as a {@link HeistPlan}.</li>
 *   <li><b>Planning</b> — player acquires tools and optionally recruits an accomplice
 *       (costs 10 COIN, warns with "Leg it!" speech bubble).</li>
 *   <li><b>Execution</b> — player presses G to start the countdown timer. Breaking
 *       blocks near armed alarm boxes spikes noise to 1.0 and flags the player as
 *       wanted. Safes require CROWBAR + 8 seconds of hold-E. Timer expiry floods
 *       the area with 4 POLICE NPCs.</li>
 *   <li><b>Fence</b> — hot loot is worth 100% within 5 in-game minutes, 50% up to
 *       60 minutes, then 25% permanently.</li>
 * </ol>
 *
 * <h3>Heist targets</h3>
 * <table>
 *   <tr><th>Target</th><th>Alarms</th><th>Safes</th><th>Key Loot</th><th>Time Limit</th><th>Faction Impact</th></tr>
 *   <tr><td>Jeweller</td><td>2</td><td>1</td><td>DIAMOND×3-6, GOLD_RING×2</td><td>90s</td><td>Marchetti –15</td></tr>
 *   <tr><td>Off-licence</td><td>1</td><td>1</td><td>COIN×20-40, PETROL_CAN×1</td><td>60s</td><td>Marchetti –20</td></tr>
 *   <tr><td>Greggs</td><td>0</td><td>0</td><td>PASTY×10, COIN×8</td><td>45s</td><td>Street Lads +5</td></tr>
 *   <tr><td>JobCentre</td><td>2</td><td>0</td><td>COUNCIL_ID×1, COIN×15</td><td>75s</td><td>The Council –25</td></tr>
 * </table>
 */
public class HeistSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Seconds of in-game time per game minute (in-game day = 1200s = 24 in-game hours). */
    public static final float SECONDS_PER_INGAME_MINUTE = 1200f / (24f * 60f); // ~0.833s real = 1 in-game minute

    /** Alarm box detection range in blocks. */
    public static final float ALARM_BOX_RANGE = 8f;

    /** Alarm box re-arm delay in real seconds (3 in-game minutes). */
    public static final float ALARM_REARM_DELAY = 3f * 60f * SECONDS_PER_INGAME_MINUTE;

    /** Distance within which player must stand to observe a guard (blocks). */
    public static final float GUARD_OBSERVE_RANGE = 6f;

    /** Seconds of observation required to reveal a guard's patrol route. */
    public static final float GUARD_OBSERVE_TIME_REQUIRED = 5f;

    /** Safe cracking time in seconds. */
    public static final float SAFE_CRACK_TIME = 8f;

    /** CCTV detection cone half-angle in degrees. */
    public static final float CCTV_CONE_HALF_ANGLE = 22.5f; // 45° total

    /** CCTV detection range in blocks. */
    public static final float CCTV_RANGE = 6f;

    /** Criminal record points per second of CCTV exposure. */
    public static final int CCTV_RECORD_POINTS_PER_SECOND = 1;

    /** Accomplice cost in COIN. */
    public static final int ACCOMPLICE_COST_COIN = 10;

    /** Accomplice cut of COIN loot on success (fraction). */
    public static final float ACCOMPLICE_LOOT_CUT = 0.25f;

    /** Range at which the accomplice detects incoming patrol (blocks). */
    public static final float ACCOMPLICE_WATCH_RANGE = 8f;

    /** Noise set by accomplice "Leg it!" warning. */
    public static final float ACCOMPLICE_WARNING_NOISE = 0.8f;

    /** Hot loot full-price window in real seconds (5 in-game minutes). */
    public static final float HOT_LOOT_FULL_PRICE_WINDOW = 5f * 60f * SECONDS_PER_INGAME_MINUTE;

    /** Hot loot half-price window in real seconds (60 in-game minutes). */
    public static final float HOT_LOOT_HALF_PRICE_WINDOW = 60f * 60f * SECONDS_PER_INGAME_MINUTE;

    /** Police spawn count when timer expires. */
    public static final int POLICE_FLOOD_COUNT = 4;

    /** Radius in blocks around the building for police flood spawning. */
    public static final float POLICE_SPAWN_RADIUS = 5f;

    /** Alarm override timer when alarm is tripped during execution (seconds). */
    public static final float ALARM_OVERRIDE_TIMER = 30f;

    /** Criminal record points gained on heist failure. */
    public static final int FAILURE_CRIMINAL_RECORD = 2;

    /** Street reputation gain on heist success. */
    public static final int SUCCESS_STREET_REP = 3;

    /** Number of NPCs to seed rumour into on heist success. */
    public static final int RUMOUR_SEED_COUNT = 5;

    // ── Heist target definitions ──────────────────────────────────────────────

    /** Time limits in seconds for each heist target. */
    public static float getTimeLimit(LandmarkType target) {
        if (target == LandmarkType.JEWELLER)     return 90f;
        if (target == LandmarkType.OFF_LICENCE)  return 60f;
        if (target == LandmarkType.GREGGS)       return 45f;
        if (target == LandmarkType.JOB_CENTRE)  return 75f;
        return 60f;
    }

    /** Faction respect delta (may be positive or negative) for a given target on success. */
    public static int getSuccessRespectDelta(LandmarkType target) {
        if (target == LandmarkType.JEWELLER)    return -15;
        if (target == LandmarkType.OFF_LICENCE) return -20;
        if (target == LandmarkType.GREGGS)      return  5;
        if (target == LandmarkType.JOB_CENTRE)  return -25;
        return 0;
    }

    /** Which faction is affected by the respect delta on success. */
    public static Faction getSuccessFaction(LandmarkType target) {
        if (target == LandmarkType.GREGGS)      return Faction.STREET_LADS;
        if (target == LandmarkType.JOB_CENTRE)  return Faction.THE_COUNCIL;
        return Faction.MARCHETTI_CREW; // Jeweller + Off-Licence
    }

    // ── Phase state ────────────────────────────────────────────────────────────

    public enum HeistPhase {
        NONE,       // No heist active
        CASING,     // Player is inside, casing the building
        PLANNING,   // Heist plan ready; player acquires tools / accomplice
        EXECUTION,  // Timer running
        COMPLETE,   // Heist succeeded or failed
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private HeistPhase phase = HeistPhase.NONE;
    private HeistPlan activePlan = null;

    /** The building position (centre) used to spawn police and judge escape. */
    private Vector3 buildingCentre = null;

    /** Execution countdown timer (seconds). */
    private float executionTimer = 0f;

    /** Initial time limit for the current heist. */
    private float initialTimeLimit = 0f;

    /** Whether the player succeeded (escaped in time). */
    private boolean heistSucceeded = false;

    /** Whether this heist was done without an accomplice. */
    private boolean soloJob = true;

    /** Whether this heist was done without triggering any alarm. */
    private boolean noAlarmTripped = true;

    /** Elapsed real-time since heist completion (for hot-loot valuation). */
    private float timeSinceHeistComplete = 0f;

    /** Whether hot loot has been fenced. */
    private boolean hotLootFenced = false;

    /** Accomplice NPC (null if no accomplice recruited). */
    private NPC accomplice = null;

    /** Per-alarm-box re-arm countdown (seconds). Negative = armed, positive = counting down. */
    private final List<Float> alarmRearmTimers = new ArrayList<>();

    /** Whether each alarm box is currently silenced. */
    private final List<Boolean> alarmSilenced = new ArrayList<>();

    /** Whether each alarm box has been tripped this session. */
    private final List<Boolean> alarmTripped = new ArrayList<>();

    /** Safe cracking progress (seconds held-E). */
    private float safeCrackProgress = 0f;

    /** Whether safe cracking is currently in progress. */
    private boolean crackingInProgress = false;

    /** Index of the safe being cracked. */
    private int crackingSafeIndex = -1;

    /** CCTV exposure accumulator (fractional seconds for criminal record). */
    private float cctvExposureAccumulator = 0f;

    /** Whether today's heist has already been completed (reset at 06:00). */
    private boolean heistDoneToday = false;

    private final Random random;

    // ── Construction ──────────────────────────────────────────────────────────

    public HeistSystem() {
        this(new Random());
    }

    public HeistSystem(Random random) {
        this.random = random;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Call once per frame.
     *
     * @param delta       seconds since last frame
     * @param player      the player
     * @param noiseSystem the player's noise system
     * @param npcManager  the NPC manager (for spawning police, accomplice lookout)
     * @param factionSystem the faction system (for respect adjustments)
     * @param rumourNetwork the rumour network (for heist gossip)
     * @param allNpcs     all living NPCs
     * @param world       the world
     * @param isNight     whether it is currently night
     */
    public void update(float delta,
                       Player player,
                       NoiseSystem noiseSystem,
                       NPCManager npcManager,
                       FactionSystem factionSystem,
                       RumourNetwork rumourNetwork,
                       List<NPC> allNpcs,
                       World world,
                       boolean isNight) {

        // Alarm re-arm timers
        for (int i = 0; i < alarmRearmTimers.size(); i++) {
            float t = alarmRearmTimers.get(i);
            if (t > 0f) {
                t -= delta;
                alarmRearmTimers.set(i, t);
                if (t <= 0f) {
                    // Re-arm
                    alarmSilenced.set(i, false);
                    if (activePlan != null) activePlan.setAlarmBoxSilenced(i, false);
                }
            }
        }

        if (phase == HeistPhase.EXECUTION) {
            updateExecution(delta, player, noiseSystem, npcManager, factionSystem, rumourNetwork, allNpcs, world);
        }

        // CCTV detection (active whenever there is an active plan)
        if (activePlan != null && phase != HeistPhase.NONE) {
            updateCCTV(delta, player, noiseSystem, rumourNetwork, allNpcs, isNight);
        }

        // Accomplice lookout
        if (accomplice != null && accomplice.isAlive() && phase == HeistPhase.EXECUTION) {
            updateAccomplice(delta, player, noiseSystem, allNpcs);
        }

        // Hot loot ageing
        if (phase == HeistPhase.COMPLETE && heistSucceeded) {
            timeSinceHeistComplete += delta;
        }
    }

    private void updateExecution(float delta,
                                  Player player,
                                  NoiseSystem noiseSystem,
                                  NPCManager npcManager,
                                  FactionSystem factionSystem,
                                  RumourNetwork rumourNetwork,
                                  List<NPC> allNpcs,
                                  World world) {
        executionTimer -= delta;
        if (executionTimer <= 0f) {
            // Timer expired — police flood
            triggerPoliceFlood(player, npcManager, world);
            failHeist(player, factionSystem, rumourNetwork, allNpcs);
        }
    }

    private void updateCCTV(float delta,
                             Player player,
                             NoiseSystem noiseSystem,
                             RumourNetwork rumourNetwork,
                             List<NPC> allNpcs,
                             boolean isNight) {
        if (activePlan == null) return;

        boolean exposed = false;
        for (int i = 0; i < activePlan.getCCTVCount(); i++) {
            Vector3 cctvPos = activePlan.getCCTVPosition(i);
            if (isPlayerInCCTVCone(player.getPosition(), cctvPos)) {
                // BALACLAVA at night nullifies
                if (isNight && player.isBalaclavWorn()) {
                    continue;
                }
                exposed = true;
                break;
            }
        }

        if (exposed) {
            cctvExposureAccumulator += delta;
            // Add 1 criminal record point per second
            int pointsToAdd = (int) cctvExposureAccumulator;
            if (pointsToAdd >= 1) {
                cctvExposureAccumulator -= pointsToAdd;
                for (int i = 0; i < pointsToAdd; i++) {
                    player.getCriminalRecord().record(CriminalRecord.CrimeType.SHOPS_RAIDED);
                }
                // Seed CCTV rumour
                String rumourText = "Someone had their face all over the jeweller's CCTV.";
                seedRumour(rumourNetwork, allNpcs, rumourText, 1);
            }
        } else {
            cctvExposureAccumulator = 0f;
        }
    }

    private boolean isPlayerInCCTVCone(Vector3 playerPos, Vector3 cctvPos) {
        float dx = playerPos.x - cctvPos.x;
        float dz = playerPos.z - cctvPos.z;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (dist > CCTV_RANGE) return false;

        // Camera faces -Z by default (facing the building interior)
        // The cone check: angle between camera-forward and player direction
        // Using camera forward = (0, 0, -1)
        float forwardX = 0f;
        float forwardZ = -1f;
        float dot = (dx * forwardX + dz * forwardZ) / Math.max(dist, 0.001f);
        float angle = (float) Math.toDegrees(Math.acos(Math.max(-1f, Math.min(1f, dot))));
        return angle <= CCTV_CONE_HALF_ANGLE;
    }

    private void updateAccomplice(float delta, Player player, NoiseSystem noiseSystem, List<NPC> allNpcs) {
        if (accomplice == null) return;

        // Check if any patrol NPC is within watch range of the accomplice
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            if (npc == accomplice) continue;
            if (npc.getType() == NPCType.POLICE || npc.getState() == NPCState.PATROLLING) {
                float dist = npc.getPosition().dst(accomplice.getPosition());
                if (dist <= ACCOMPLICE_WATCH_RANGE) {
                    // Trigger "Leg it!" warning
                    accomplice.setSpeechText("Leg it!", 3.0f);
                    noiseSystem.setNoiseLevel(Math.max(noiseSystem.getNoiseLevel(), ACCOMPLICE_WARNING_NOISE));
                    break;
                }
            }
        }
    }

    // ── Phase transitions ─────────────────────────────────────────────────────

    /**
     * Called when the player presses F inside a building. Begins casing.
     * Returns a tooltip message, or null if casing is not possible here.
     *
     * @param target         the landmark the player is inside
     * @param buildingCentre the centre of the building in world coordinates
     * @param npcManager     NPC manager (to count patrol guards)
     * @return tooltip text, or null
     */
    public String startCasing(LandmarkType target, Vector3 buildingCentre, NPCManager npcManager) {
        if (heistDoneToday) {
            return "Already done one today. Leave it 'til tomorrow.";
        }
        if (!isHeistableTarget(target)) {
            return null; // Not a valid heist target
        }

        // Count patrol guards near the building
        int guardCount = 0;
        if (npcManager != null) {
            for (NPC npc : npcManager.getNPCs()) {
                if (!npc.isAlive()) continue;
                if (npc.getState() == NPCState.PATROLLING) {
                    float dist = npc.getPosition().dst(buildingCentre);
                    if (dist < 20f) {
                        guardCount++;
                    }
                }
            }
        }

        this.activePlan = new HeistPlan(target, guardCount);
        this.buildingCentre = new Vector3(buildingCentre);
        this.phase = HeistPhase.CASING;

        // Populate alarm boxes, CCTVs, safes based on target
        populateHeistProps(target, buildingCentre);

        return "Knowledge is power. Or at least it's a start.";
    }

    private void populateHeistProps(LandmarkType target, Vector3 centre) {
        alarmRearmTimers.clear();
        alarmSilenced.clear();
        alarmTripped.clear();

        if (target == LandmarkType.JEWELLER) {
            // 2 alarm boxes, 1 safe, CCTV
            activePlan.addAlarmBox(new Vector3(centre.x - 3, centre.y + 1, centre.z - 4));
            activePlan.addAlarmBox(new Vector3(centre.x + 3, centre.y + 1, centre.z - 4));
            activePlan.addSafe(new Vector3(centre.x, centre.y, centre.z + 2));
            activePlan.addCCTV(new Vector3(centre.x, centre.y + 2, centre.z - 5));
        } else if (target == LandmarkType.OFF_LICENCE) {
            // 1 alarm box, 1 safe
            activePlan.addAlarmBox(new Vector3(centre.x, centre.y + 1, centre.z - 3));
            activePlan.addSafe(new Vector3(centre.x + 2, centre.y, centre.z + 1));
        } else if (target == LandmarkType.GREGGS) {
            // No alarms or safes — just a cash drawer raid
        } else if (target == LandmarkType.JOB_CENTRE) {
            // 2 alarm boxes, no safe
            activePlan.addAlarmBox(new Vector3(centre.x - 4, centre.y + 1, centre.z - 5));
            activePlan.addAlarmBox(new Vector3(centre.x + 4, centre.y + 1, centre.z - 5));
            activePlan.addCCTV(new Vector3(centre.x, centre.y + 2, centre.z - 5));
        }

        // Initialise alarm state
        for (int i = 0; i < activePlan.getAlarmBoxCount(); i++) {
            alarmRearmTimers.add(-1f);  // -1 = armed (not counting down)
            alarmSilenced.add(false);
            alarmTripped.add(false);
        }
    }

    /**
     * Called when the player presses G to begin the execution timer.
     *
     * @return true if the heist execution has started
     */
    public boolean startExecution() {
        if (phase != HeistPhase.CASING && phase != HeistPhase.PLANNING) return false;
        if (activePlan == null) return false;

        phase = HeistPhase.EXECUTION;
        executionTimer = getTimeLimit(activePlan.getTarget());
        initialTimeLimit = executionTimer;
        heistSucceeded = false;
        noAlarmTripped = true;
        safeCrackProgress = 0f;
        crackingInProgress = false;
        crackingSafeIndex = -1;
        return true;
    }

    /**
     * Called when a block is broken near the building.
     * If an armed alarm box is within {@link #ALARM_BOX_RANGE} blocks, triggers the alarm.
     *
     * @param breakPos    the position where the block was broken
     * @param noiseSystem the player's noise system
     * @param npcManager  NPC manager (for police flagging)
     * @param player      the player
     * @param world       the world
     * @return true if an alarm was triggered
     */
    public boolean onBlockBreak(Vector3 breakPos,
                                 NoiseSystem noiseSystem,
                                 NPCManager npcManager,
                                 Player player,
                                 World world) {
        if (activePlan == null) return false;

        boolean triggered = false;
        for (int i = 0; i < activePlan.getAlarmBoxCount(); i++) {
            if (alarmSilenced.get(i)) continue; // already silenced
            Vector3 alarmPos = activePlan.getAlarmBoxPosition(i);
            float dist = alarmPos.dst(breakPos);
            if (dist <= ALARM_BOX_RANGE) {
                // Trigger alarm
                noiseSystem.setNoiseLevel(1.0f);
                noAlarmTripped = false;
                alarmTripped.set(i, true);

                // Override execution timer if currently running
                if (phase == HeistPhase.EXECUTION && executionTimer > ALARM_OVERRIDE_TIMER) {
                    executionTimer = ALARM_OVERRIDE_TIMER;
                }

                // Spawn police immediately for unsilenced alarm
                if (npcManager != null && player != null && world != null) {
                    spawnPoliceFlood(player, npcManager, world);
                }

                triggered = true;
            }
        }
        return triggered;
    }

    /**
     * Begin silencing an alarm box at the given index (requires player to hold E for 1 second).
     * This is a simplified call — the hold-E timing is managed externally.
     *
     * @param alarmIndex index of the alarm box
     * @param noiseSystem noise system (silencing makes 0.1 noise)
     * @return true if the alarm box was successfully silenced
     */
    public boolean silenceAlarmBox(int alarmIndex, NoiseSystem noiseSystem) {
        if (activePlan == null) return false;
        if (alarmIndex < 0 || alarmIndex >= activePlan.getAlarmBoxCount()) return false;
        if (alarmSilenced.get(alarmIndex)) return true; // already silenced

        alarmSilenced.set(alarmIndex, true);
        activePlan.setAlarmBoxSilenced(alarmIndex, true);
        alarmRearmTimers.set(alarmIndex, ALARM_REARM_DELAY);

        if (noiseSystem != null) {
            noiseSystem.setNoiseLevel(0.1f);
        }
        return true;
    }

    /**
     * Returns true if alarm box at {@code alarmIndex} is currently silenced.
     */
    public boolean isAlarmBoxSilenced(int alarmIndex) {
        if (alarmIndex < 0 || alarmIndex >= alarmSilenced.size()) return false;
        return alarmSilenced.get(alarmIndex);
    }

    /**
     * Advance safe cracking progress.
     * Call once per frame while the player holds E adjacent to the safe.
     *
     * @param delta       seconds this frame
     * @param safeIndex   which safe the player is cracking
     * @param hasCrowbar  whether the player has a CROWBAR in their inventory
     * @param noiseSystem the noise system (interrupted → spike to 1.0)
     * @param inventory   the player's inventory
     * @param world       the world (for dropping loot)
     * @return true if the safe was successfully cracked this frame
     */
    public boolean updateSafeCracking(float delta,
                                       int safeIndex,
                                       boolean hasCrowbar,
                                       NoiseSystem noiseSystem,
                                       Inventory inventory,
                                       World world) {
        if (activePlan == null) return false;
        if (safeIndex < 0 || safeIndex >= activePlan.getSafeCount()) return false;
        if (activePlan.isSafeCracked(safeIndex)) return false;
        if (!hasCrowbar) return false;

        crackingInProgress = true;
        crackingSafeIndex = safeIndex;
        safeCrackProgress += delta;

        if (safeCrackProgress >= SAFE_CRACK_TIME) {
            // Safe cracked!
            activePlan.setSafeCracked(safeIndex, true);
            crackingInProgress = false;
            crackingSafeIndex = -1;
            safeCrackProgress = 0f;

            // Spawn loot
            spawnSafeLoot(safeIndex, inventory);
            return true;
        }
        return false;
    }

    /**
     * Interrupt safe cracking (player detected or moved away).
     *
     * @param noiseSystem the noise system
     */
    public void interruptSafeCracking(NoiseSystem noiseSystem) {
        if (crackingInProgress) {
            crackingInProgress = false;
            crackingSafeIndex = -1;
            safeCrackProgress = 0f;
            if (noiseSystem != null) {
                noiseSystem.setNoiseLevel(1.0f);
            }
        }
    }

    private void spawnSafeLoot(int safeIndex, Inventory inventory) {
        if (activePlan == null || inventory == null) return;
        LandmarkType target = activePlan.getTarget();
        if (target == LandmarkType.JEWELLER) {
            int diamonds = 3 + random.nextInt(4); // 3–6
            inventory.addItem(Material.DIAMOND, diamonds);
            inventory.addItem(Material.GOLD_RING, 2);
        } else if (target == LandmarkType.OFF_LICENCE) {
            int coins = 20 + random.nextInt(21); // 20–40
            inventory.addItem(Material.COIN, coins);
            inventory.addItem(Material.PETROL_CAN, 1);
        }
    }

    /**
     * Called when the player escapes (exits the building exclusion zone) before
     * the timer expires. Completes the heist successfully.
     *
     * @param player        the player
     * @param inventory     the player's inventory
     * @param factionSystem the faction system
     * @param rumourNetwork the rumour network
     * @param allNpcs       all living NPCs
     * @param achievementSystem the achievement callback (may be null)
     */
    public void completeHeist(Player player,
                               Inventory inventory,
                               FactionSystem factionSystem,
                               RumourNetwork rumourNetwork,
                               List<NPC> allNpcs,
                               AchievementCallback achievementSystem) {
        completeHeist(player, inventory, factionSystem, rumourNetwork, allNpcs, achievementSystem, null);
    }

    /**
     * Called when the player escapes (exits the building exclusion zone) before
     * the timer expires. Completes the heist successfully.
     *
     * @param player          the player
     * @param inventory       the player's inventory
     * @param factionSystem   the faction system
     * @param rumourNetwork   the rumour network
     * @param allNpcs         all living NPCs
     * @param achievementSystem the achievement callback (may be null)
     * @param notorietySystem the notoriety system for Phase 8e notoriety gain (may be null)
     */
    public void completeHeist(Player player,
                               Inventory inventory,
                               FactionSystem factionSystem,
                               RumourNetwork rumourNetwork,
                               List<NPC> allNpcs,
                               AchievementCallback achievementSystem,
                               NotorietySystem notorietySystem) {
        if (phase != HeistPhase.EXECUTION) return;

        heistSucceeded = true;
        phase = HeistPhase.COMPLETE;
        timeSinceHeistComplete = 0f;
        heistDoneToday = true;

        // Apply faction respect
        LandmarkType target = activePlan.getTarget();
        Faction faction = getSuccessFaction(target);
        int delta = getSuccessRespectDelta(target);
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(faction, delta);
        }

        // Greggs loot (no safe) — drop directly
        if (target == LandmarkType.GREGGS) {
            inventory.addItem(Material.PASTY, 10);
            inventory.addItem(Material.COIN, 8);
        } else if (target == LandmarkType.JOB_CENTRE) {
            inventory.addItem(Material.COUNCIL_ID, 1);
            inventory.addItem(Material.COIN, 15);
        }

        // Accomplice coin cut
        if (accomplice != null) {
            int coinCount = inventory.getItemCount(Material.COIN);
            int cut = (int) (coinCount * ACCOMPLICE_LOOT_CUT);
            if (cut > 0) {
                inventory.removeItem(Material.COIN, cut);
            }
        }

        // Street rep
        player.getStreetReputation().addPoints(SUCCESS_STREET_REP);

        // Phase 8e: notoriety gain
        if (notorietySystem != null) {
            boolean isJeweller = (target == LandmarkType.JEWELLER);
            // Adapt HeistSystem.AchievementCallback to NotorietySystem.AchievementCallback
            NotorietySystem.AchievementCallback notorietyCallback =
                    (achievementSystem != null) ? achievementSystem::award : null;
            notorietySystem.onHeistComplete(isJeweller, notorietyCallback);
            // Award THE_CREW achievement if accomplice was present
            if (accomplice != null) {
                notorietySystem.onHeistSuccessWithAccomplice(notorietyCallback);
            }
        }

        // Seed rumour
        String targetName = target.getDisplayName();
        seedRumour(rumourNetwork, allNpcs, targetName + " got done over last night. Proper job.", RUMOUR_SEED_COUNT);

        // Achievements
        if (achievementSystem != null) {
            achievementSystem.award(AchievementType.MASTER_CRIMINAL);
            if (noAlarmTripped) {
                achievementSystem.award(AchievementType.SMOOTH_OPERATOR);
            }
            if (soloJob) {
                achievementSystem.award(AchievementType.SOLO_JOB);
            }
        }
    }

    /**
     * Called when the heist fails (timer expires or player arrested).
     */
    private void failHeist(Player player,
                            FactionSystem factionSystem,
                            RumourNetwork rumourNetwork,
                            List<NPC> allNpcs) {
        heistSucceeded = false;
        phase = HeistPhase.COMPLETE;
        heistDoneToday = true;

        // Criminal record penalty
        for (int i = 0; i < FAILURE_CRIMINAL_RECORD; i++) {
            player.getCriminalRecord().record(CriminalRecord.CrimeType.SHOPS_RAIDED);
        }

        // Building faction gains +10 respect (player failed their territory)
        if (activePlan != null && factionSystem != null) {
            Faction faction = getSuccessFaction(activePlan.getTarget());
            factionSystem.applyRespectDelta(faction, 10);
        }

        // Seed failure rumour
        if (activePlan != null) {
            String targetName = activePlan.getTarget().getDisplayName();
            seedRumour(rumourNetwork, allNpcs,
                    "That one tried to do " + targetName + " over. Pathetic.", RUMOUR_SEED_COUNT);
        }
    }

    /**
     * Trigger a police flood — spawn {@link #POLICE_FLOOD_COUNT} police around the building.
     */
    public void triggerPoliceFlood(Player player, NPCManager npcManager, World world) {
        if (npcManager == null || buildingCentre == null) return;
        spawnPoliceFlood(player, npcManager, world);
    }

    private void spawnPoliceFlood(Player player, NPCManager npcManager, World world) {
        int spawned = 0;
        for (int i = 0; i < POLICE_FLOOD_COUNT && spawned < POLICE_FLOOD_COUNT; i++) {
            float angle = (float) (random.nextFloat() * Math.PI * 2);
            float dist = POLICE_SPAWN_RADIUS + random.nextFloat() * 3f;
            float x = (buildingCentre != null ? buildingCentre.x : player.getPosition().x) + (float) Math.cos(angle) * dist;
            float z = (buildingCentre != null ? buildingCentre.z : player.getPosition().z) + (float) Math.sin(angle) * dist;
            float y = player.getPosition().y; // approximate ground height
            NPC police = npcManager.spawnNPC(NPCType.POLICE, x, y, z);
            if (police != null) {
                police.setState(NPCState.AGGRESSIVE);
                police.setSpeechText("Oi! Stop right there!", 3.0f);
                spawned++;
            }
        }
    }

    // ── Accomplice ────────────────────────────────────────────────────────────

    /**
     * Recruit an NPC as an accomplice for this heist.
     * Costs {@link #ACCOMPLICE_COST_COIN} COIN.
     *
     * @param npc       the NPC to recruit
     * @param inventory the player's inventory
     * @return true if recruitment succeeded
     */
    public boolean recruitAccomplice(NPC npc, Inventory inventory) {
        if (inventory.getItemCount(Material.COIN) < ACCOMPLICE_COST_COIN) return false;
        inventory.removeItem(Material.COIN, ACCOMPLICE_COST_COIN);
        this.accomplice = npc;
        this.soloJob = false;
        npc.setState(NPCState.FOLLOWING);
        npc.setSpeechText("All clear.", 3.0f);
        return true;
    }

    /** Whether the player has an accomplice for this heist. */
    public boolean hasAccomplice() {
        return accomplice != null;
    }

    // ── Hot loot valuation ────────────────────────────────────────────────────

    /**
     * Returns the multiplier for hot loot value based on time since the heist completed.
     *
     * <ul>
     *   <li>100% within 5 in-game minutes ("still warm, son")</li>
     *   <li>50% from 5 min to 60 min</li>
     *   <li>25% after 60 in-game minutes</li>
     * </ul>
     *
     * @return value multiplier (0.25f, 0.5f, or 1.0f)
     */
    public float getHotLootMultiplier() {
        if (!heistSucceeded) return 1.0f;
        if (timeSinceHeistComplete <= HOT_LOOT_FULL_PRICE_WINDOW) return 1.0f;
        if (timeSinceHeistComplete <= HOT_LOOT_HALF_PRICE_WINDOW) return 0.5f;
        return 0.25f;
    }

    /**
     * Returns a descriptive string for the current hot loot state.
     */
    public String getHotLootDescription() {
        float mult = getHotLootMultiplier();
        if (mult >= 1.0f) return "still warm, son";
        if (mult >= 0.5f) return "getting cold";
        return "stone cold — take it or leave it";
    }

    /**
     * Mark hot loot as fenced and award the FENCE_FRESH achievement if applicable.
     */
    public void markHotLootFenced(AchievementCallback achievementSystem) {
        if (!hotLootFenced && heistSucceeded && timeSinceHeistComplete <= HOT_LOOT_FULL_PRICE_WINDOW) {
            if (achievementSystem != null) {
                achievementSystem.award(AchievementType.FENCE_FRESH);
            }
        }
        hotLootFenced = true;
    }

    // ── Crafting tools ────────────────────────────────────────────────────────

    /**
     * Called when the player removes a GLASS block with a GLASS_CUTTER equipped.
     * Removes the block silently (no noise spike).
     *
     * @param noiseSystem the noise system (noise stays unchanged)
     * @return true (operation always succeeds when GLASS_CUTTER is held)
     */
    public boolean removeGlassSilently(NoiseSystem noiseSystem) {
        // Don't spike noise — just return true
        // The actual block removal is handled by the block-breaking system
        return true;
    }

    /**
     * Deploy a ROPE_LADDER at the given position. Places a LADDER block and
     * schedules it for removal after 60 in-game seconds.
     *
     * @param world    the world
     * @param x        block X
     * @param y        block Y
     * @param z        block Z
     */
    public void deployRopeLadder(World world, int x, int y, int z) {
        if (world == null) return;
        world.setBlock(x, y, z, BlockType.LADDER);
        pendingLadderX = x;
        pendingLadderY = y;
        pendingLadderZ = z;
        ropeLadderTimer = 60f * SECONDS_PER_INGAME_MINUTE; // 60 in-game seconds
        ropeLadderActive = true;
    }

    /** Pending rope ladder position. */
    private int pendingLadderX, pendingLadderY, pendingLadderZ;
    private float ropeLadderTimer = 0f;
    private boolean ropeLadderActive = false;

    /**
     * Update the rope ladder timer. Call from the main update loop.
     *
     * @param delta seconds since last frame
     * @param world the world
     */
    public void updateRopeLadder(float delta, World world) {
        if (!ropeLadderActive) return;
        ropeLadderTimer -= delta;
        if (ropeLadderTimer <= 0f) {
            ropeLadderActive = false;
            if (world != null) {
                world.setBlock(pendingLadderX, pendingLadderY, pendingLadderZ, BlockType.AIR);
            }
        }
    }

    // ── Day reset ─────────────────────────────────────────────────────────────

    /**
     * Reset the daily heist flag (call at 06:00 each in-game day).
     */
    public void resetDaily() {
        heistDoneToday = false;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private void seedRumour(RumourNetwork rumourNetwork, List<NPC> allNpcs, String text, int count) {
        if (rumourNetwork == null || allNpcs == null) return;
        int seeded = 0;
        List<NPC> candidates = new ArrayList<>(allNpcs);
        java.util.Collections.shuffle(candidates, random);
        for (NPC npc : candidates) {
            if (!npc.isAlive()) continue;
            if (npc.getType() == NPCType.FENCE) continue; // fence doesn't spread rumours
            rumourNetwork.addRumour(npc, new Rumour(RumourType.GANG_ACTIVITY, text));
            seeded++;
            if (seeded >= count) break;
        }
    }

    private static boolean isHeistableTarget(LandmarkType target) {
        return target == LandmarkType.JEWELLER
            || target == LandmarkType.OFF_LICENCE
            || target == LandmarkType.GREGGS
            || target == LandmarkType.JOB_CENTRE;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Current heist phase. */
    public HeistPhase getPhase() {
        return phase;
    }

    /** The active heist plan, or null if none. */
    public HeistPlan getActivePlan() {
        return activePlan;
    }

    /** Remaining execution timer in seconds. */
    public float getExecutionTimer() {
        return executionTimer;
    }

    /** Whether the last completed heist was a success. */
    public boolean isHeistSucceeded() {
        return heistSucceeded;
    }

    /** Safe cracking progress (0–8 seconds). */
    public float getSafeCrackProgress() {
        return safeCrackProgress;
    }

    /** Whether safe cracking is currently in progress. */
    public boolean isCrackingInProgress() {
        return crackingInProgress;
    }

    /** Whether today's heist has been completed (prevents repeating same day). */
    public boolean isHeistDoneToday() {
        return heistDoneToday;
    }

    /** Time since the heist completed (in real seconds). */
    public float getTimeSinceHeistComplete() {
        return timeSinceHeistComplete;
    }

    /** Whether the rope ladder is active. */
    public boolean isRopeLadderActive() {
        return ropeLadderActive;
    }

    /** Remaining time on the rope ladder (real seconds). */
    public float getRopeLadderTimer() {
        return ropeLadderTimer;
    }

    // ── Force-set for testing ─────────────────────────────────────────────────

    /** Force the heist phase (for testing). */
    public void setPhaseForTesting(HeistPhase phase) {
        this.phase = phase;
    }

    /** Force execution timer (for testing). */
    public void setExecutionTimerForTesting(float timer) {
        this.executionTimer = timer;
    }

    /** Force time since heist complete (for testing hot loot valuation). */
    public void setTimeSinceHeistCompleteForTesting(float t) {
        this.timeSinceHeistComplete = t;
        this.heistSucceeded = true;
        this.phase = HeistPhase.COMPLETE;
    }

    /** Force heist succeeded state (for testing). */
    public void setHeistSucceededForTesting(boolean succeeded) {
        this.heistSucceeded = succeeded;
    }

    /** Force accomplice (for testing). */
    public void setAccompliceForTesting(NPC npc) {
        this.accomplice = npc;
        this.soloJob = false;
    }

    /**
     * Callback interface for awarding achievements without coupling to the full game.
     */
    public interface AchievementCallback {
        void award(AchievementType type);
    }
}

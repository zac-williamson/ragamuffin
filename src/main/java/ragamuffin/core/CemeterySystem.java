package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1120: Northfield Cemetery — Funeral Processions, Grave-Robbing &amp; the Night Shift.
 *
 * <p>Manages:
 * <ol>
 *   <li><b>Funeral processions</b> — every 3 in-game days, 10:00–11:00: VICAR + 2–5 MOURNER NPCs
 *       route from FUNERAL_PARLOUR → CHURCH → CEMETERY, cluster at GRAVE_PLOT_PROP, then
 *       disperse. Seeds a {@link RumourType#FRESH_GRAVE} rumour into RumourNetwork on
 *       completion.</li>
 *   <li><b>Grave-robbing mechanic</b> — 8 SPADE hits on GRAVE_PLOT_PROP converts it to
 *       OPEN_GRAVE_PROP and yields loot (WEDDING_RING/GOLD_RING/OLD_COIN/POCKET_WATCH/empty)
 *       from a weighted table. Unwitnessed: Notoriety +5, GRAVE_ROBBER achievement.
 *       Witnessed by VICAR or GRAVEDIGGER: Notoriety +15, WantedSystem Tier 2,
 *       CriminalRecord GRAVE_ROBBING, NoiseSystem +30.</li>
 *   <li><b>GRAVEDIGGER NPC (Reg)</b> — weekday mornings 06:00–09:00, hostile if witnesses
 *       digging.</li>
 *   <li><b>Night gate lock</b> — GATE_PROP locks at 23:00. Lockpickable (4 hits): Notoriety +2,
 *       TRESPASS on CriminalRecord. CEMETERY_NIGHT_OWL achievement fires when player is inside
 *       after midnight.</li>
 *   <li><b>FUNERAL_FLOWERS interactions</b> — place on headstone (Notoriety −1), steal from
 *       headstone (Notoriety +3).</li>
 * </ol>
 */
public class CemeterySystem {

    // ── Day-of-week constants (dayCount % 7) ──────────────────────────────────
    // Game start = day 1 → day%7: 0=Mon,1=Tue,2=Wed,3=Thu,4=Fri,5=Sat,6=Sun
    private static final int MONDAY    = 0;
    private static final int TUESDAY   = 1;
    private static final int WEDNESDAY = 2;
    private static final int THURSDAY  = 3;
    private static final int FRIDAY    = 4;

    // ── Funeral procession constants ──────────────────────────────────────────

    /** In-game days between funeral processions. */
    public static final int FUNERAL_INTERVAL_DAYS = 3;

    /** Hour at which a funeral procession starts. */
    public static final float FUNERAL_START_HOUR = 10.0f;

    /** Hour at which the procession window ends (any active procession wraps up). */
    public static final float FUNERAL_END_HOUR = 11.0f;

    /** Duration of a funeral procession (seconds). */
    public static final float FUNERAL_PROCESSION_DURATION = 90.0f;

    /** Minimum number of MOURNER NPCs per procession. */
    public static final int MOURNER_MIN = 2;

    /** Maximum number of MOURNER NPCs per procession. */
    public static final int MOURNER_MAX = 5;

    // ── Grave-robbing constants ───────────────────────────────────────────────

    /** Number of SPADE hits required to open a GRAVE_PLOT_PROP. */
    public static final int GRAVE_HITS_REQUIRED = 8;

    /** Notoriety gained for unwitnessed grave robbing. */
    public static final int GRAVE_ROB_UNWITNESSED_NOTORIETY = 5;

    /** Notoriety gained for witnessed grave robbing. */
    public static final int GRAVE_ROB_WITNESSED_NOTORIETY = 15;

    /** WantedSystem severity points added when grave robbing is witnessed (results in Tier 2). */
    public static final int GRAVE_ROB_WITNESSED_SEVERITY = 6;

    /** NoiseSystem level spike when grave robbing is witnessed (0–100 scale, mapped to 0–1). */
    public static final float GRAVE_ROB_WITNESSED_NOISE = 0.30f;

    /** Detection range (blocks) within which VICAR or GRAVEDIGGER witnesses digging. */
    public static final float WITNESS_DETECT_RANGE = 12.0f;

    // ── Grave loot weights ────────────────────────────────────────────────────
    // Cumulative weights: WEDDING_RING(25), GOLD_RING(45), OLD_COIN(65), POCKET_WATCH(80), empty(100)

    private static final int LOOT_WEIGHT_WEDDING_RING  = 25;
    private static final int LOOT_WEIGHT_GOLD_RING     = 45;
    private static final int LOOT_WEIGHT_OLD_COIN      = 65;
    private static final int LOOT_WEIGHT_POCKET_WATCH  = 80;
    // > 80 = empty

    /** In-game days before an OPEN_GRAVE_PROP resets back to GRAVE_PLOT_PROP. */
    public static final int GRAVE_RESET_DAYS = 2;

    // ── Gravedigger (Reg) constants ───────────────────────────────────────────

    /** Gravedigger starts work at this hour (weekday mornings). */
    public static final float GRAVEDIGGER_START_HOUR = 6.0f;

    /** Gravedigger leaves at this hour. */
    public static final float GRAVEDIGGER_END_HOUR = 9.0f;

    // ── Night gate constants ──────────────────────────────────────────────────

    /** Hour at which the cemetery gate locks. */
    public static final float GATE_LOCK_HOUR = 23.0f;

    /** Hour at which the gate unlocks (morning). */
    public static final float GATE_UNLOCK_HOUR = 7.0f;

    /** Hits required to lockpick the gate. */
    public static final int GATE_LOCKPICK_HITS = 4;

    /** Notoriety gained for lockpicking the gate. */
    public static final int GATE_LOCKPICK_NOTORIETY = 2;

    // ── FUNERAL_FLOWERS constants ─────────────────────────────────────────────

    /** Notoriety change when player places FUNERAL_FLOWERS on a headstone. */
    public static final int FLOWER_PLACE_NOTORIETY = -1;

    /** Notoriety change when player steals FUNERAL_FLOWERS from a headstone. */
    public static final int FLOWER_STEAL_NOTORIETY = 3;

    // ── Cemetery bounds ───────────────────────────────────────────────────────

    private float cemeteryX     = 60f;
    private float cemeteryZ     = 80f;
    private float cemeteryWidth = 30f;
    private float cemeteryDepth = 30f;

    // ── Funeral procession state ──────────────────────────────────────────────

    /** Day of the last funeral procession. */
    private int lastFuneralDay = -99;

    /** Whether a funeral procession is currently active. */
    private boolean processionActive = false;

    /** Remaining seconds of the current procession. */
    private float processionTimer = 0f;

    /** The VICAR NPC leading the procession. */
    private NPC vicarNpc = null;

    /** The MOURNER NPCs in the current procession. */
    private final List<NPC> mournerNpcs = new ArrayList<>();

    // ── Gravedigger (Reg) state ───────────────────────────────────────────────

    private NPC gravediggerNpc = null;
    private boolean gravediggerSpawned = false;

    // ── Grave hit tracking ────────────────────────────────────────────────────

    /** Current SPADE hit count on the active grave target. */
    private int graveHitCount = 0;

    // ── Night gate state ──────────────────────────────────────────────────────

    /** Whether the cemetery gate is currently locked. */
    private boolean gateLocked = false;

    /** Current lockpick hit count on the gate. */
    private int gateLockpickHits = 0;

    // ── Night Owl achievement state ───────────────────────────────────────────

    /** Whether CEMETERY_NIGHT_OWL has been awarded this session. */
    private boolean nightOwlAwarded = false;

    /** Whether the player is currently inside the cemetery. */
    private boolean playerInsideCemetery = false;

    // ── Injected systems ──────────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private NoiseSystem noiseSystem;
    private RumourNetwork rumourNetwork;

    // ── Callback ──────────────────────────────────────────────────────────────

    /**
     * Callback for awarding achievements.
     */
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── Construction ──────────────────────────────────────────────────────────

    private final Random random;

    public CemeterySystem() {
        this(new Random());
    }

    public CemeterySystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ──────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setNoiseSystem(NoiseSystem noiseSystem) {
        this.noiseSystem = noiseSystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    // ── Cemetery bounds ───────────────────────────────────────────────────────

    /**
     * Set the world position and dimensions of the cemetery.
     */
    public void setCemeteryBounds(float x, float z, float width, float depth) {
        this.cemeteryX     = x;
        this.cemeteryZ     = z;
        this.cemeteryWidth = width;
        this.cemeteryDepth = depth;
    }

    /**
     * Returns true if the given world position is inside the cemetery bounds.
     */
    public boolean isInsideCemetery(float x, float z) {
        return x >= cemeteryX && x <= cemeteryX + cemeteryWidth
            && z >= cemeteryZ && z <= cemeteryZ + cemeteryDepth;
    }

    // ── Schedule queries ──────────────────────────────────────────────────────

    /**
     * Returns true if a funeral procession should start on this day and hour.
     * Processions happen every {@link #FUNERAL_INTERVAL_DAYS} days at
     * {@link #FUNERAL_START_HOUR}.
     */
    public boolean isFuneralDue(int dayCount, float hour) {
        if (dayCount - lastFuneralDay < FUNERAL_INTERVAL_DAYS) return false;
        return hour >= FUNERAL_START_HOUR && hour < FUNERAL_END_HOUR;
    }

    /**
     * Returns true if the gravedigger (Reg) should be present.
     * Present weekday mornings 06:00–09:00.
     */
    public boolean isGravediggerShift(int dayCount, float hour) {
        int dow = dayCount % 7;
        boolean weekday = (dow == MONDAY || dow == TUESDAY || dow == WEDNESDAY
                        || dow == THURSDAY || dow == FRIDAY);
        return weekday && hour >= GRAVEDIGGER_START_HOUR && hour < GRAVEDIGGER_END_HOUR;
    }

    /**
     * Returns true if the cemetery gate should be locked (23:00–07:00).
     */
    public boolean isGateLocked(float hour) {
        return hour >= GATE_LOCK_HOUR || hour < GATE_UNLOCK_HOUR;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Update the cemetery system each frame.
     *
     * @param delta               seconds since last frame
     * @param timeSystem          the TimeSystem
     * @param npcs                all living NPCs
     * @param achievementCallback callback for awarding achievements (may be null)
     */
    public void update(float delta, TimeSystem timeSystem, List<NPC> npcs,
                       AchievementCallback achievementCallback) {
        float hour     = timeSystem.getTime();
        int   dayCount = timeSystem.getDayCount();

        // Update gate lock state
        gateLocked = isGateLocked(hour);

        // Night Owl achievement: player inside cemetery after midnight
        if (playerInsideCemetery && hour >= 0.0f && hour < GATE_UNLOCK_HOUR && !nightOwlAwarded) {
            nightOwlAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.CEMETERY_NIGHT_OWL);
            }
        }

        // Manage gravedigger (Reg)
        manageGravedigger(dayCount, hour, npcs);

        // Start funeral procession if due
        if (!processionActive && isFuneralDue(dayCount, hour)) {
            startFuneralProcession(dayCount, npcs);
        }

        // Update active procession
        if (processionActive) {
            processionTimer -= delta;
            if (processionTimer <= 0f) {
                endFuneralProcession(npcs, achievementCallback);
            }
        }
    }

    // ── Gravedigger management ────────────────────────────────────────────────

    private void manageGravedigger(int dayCount, float hour, List<NPC> npcs) {
        boolean shouldBePresent = isGravediggerShift(dayCount, hour);
        if (shouldBePresent && !gravediggerSpawned) {
            gravediggerNpc = new NPC(NPCType.GRAVEDIGGER, "Reg",
                    cemeteryX + cemeteryWidth / 2f, 0f, cemeteryZ + cemeteryDepth / 2f);
            gravediggerNpc.setState(NPCState.WANDERING);
            gravediggerSpawned = true;
            npcs.add(gravediggerNpc);
        } else if (!shouldBePresent && gravediggerSpawned) {
            if (gravediggerNpc != null) {
                npcs.remove(gravediggerNpc);
            }
            gravediggerNpc = null;
            gravediggerSpawned = false;
        }
    }

    // ── Funeral procession ────────────────────────────────────────────────────

    /**
     * Start a funeral procession. Spawns a VICAR + 2–5 MOURNER NPCs.
     */
    private void startFuneralProcession(int dayCount, List<NPC> npcs) {
        lastFuneralDay = dayCount;

        // Spawn VICAR leading the procession
        vicarNpc = new NPC(NPCType.VICAR, "Reverend Collins",
                cemeteryX + cemeteryWidth / 2f, 0f, cemeteryZ + 4f);
        vicarNpc.setState(NPCState.IDLE);
        npcs.add(vicarNpc);

        // Spawn 2–5 MOURNER NPCs
        int mournerCount = MOURNER_MIN + random.nextInt(MOURNER_MAX - MOURNER_MIN + 1);
        for (int i = 0; i < mournerCount; i++) {
            NPC mourner = new NPC(NPCType.MOURNER,
                    cemeteryX + 2f + i * 2f, 0f, cemeteryZ + 6f);
            mourner.setState(NPCState.IDLE);
            mournerNpcs.add(mourner);
            npcs.add(mourner);
        }

        processionActive = true;
        processionTimer  = FUNERAL_PROCESSION_DURATION;
    }

    /**
     * End the funeral procession: despawn NPCs, seed FRESH_GRAVE rumour.
     */
    private void endFuneralProcession(List<NPC> npcs, AchievementCallback achievementCallback) {
        // Despawn mourners
        npcs.removeAll(mournerNpcs);
        mournerNpcs.clear();

        // Despawn VICAR
        if (vicarNpc != null) {
            npcs.remove(vicarNpc);
            vicarNpc = null;
        }

        processionActive = false;
        processionTimer  = 0f;

        // Seed FRESH_GRAVE rumour into the rumour network via a nearby NPC
        if (rumourNetwork != null && !npcs.isEmpty()) {
            Rumour rumour = new Rumour(RumourType.FRESH_GRAVE,
                    "Did you see the funeral today? Poor sod.");
            // Seed into the first available NPC near the cemetery
            for (NPC npc : npcs) {
                if (npc.getType() != NPCType.POLICE) {
                    rumourNetwork.addRumour(npc, rumour);
                    break;
                }
            }
        }
    }

    // ── Player actions ────────────────────────────────────────────────────────

    /**
     * Record that the player entered or left the cemetery.
     */
    public void setPlayerInsideCemetery(boolean inside) {
        this.playerInsideCemetery = inside;
    }

    /**
     * Called when the player hits a GRAVE_PLOT_PROP with a SPADE.
     *
     * <p>After {@link #GRAVE_HITS_REQUIRED} hits, the grave is opened and loot is
     * yielded. If witnessed by a VICAR or GRAVEDIGGER within
     * {@link #WITNESS_DETECT_RANGE} blocks, applies heavy crime consequences.
     * Otherwise, applies lighter unwitnessed consequences.
     *
     * @param playerX             player world X
     * @param playerY             player world Y
     * @param playerZ             player world Z
     * @param inventory           player's inventory (loot added here on success)
     * @param npcs                all living NPCs (for witness check)
     * @param achievementCallback callback for achievements
     * @return true if the grave was fully opened (this hit was the final hit)
     */
    public boolean onGraveHit(float playerX, float playerY, float playerZ,
                               Inventory inventory, List<NPC> npcs,
                               AchievementCallback achievementCallback) {
        graveHitCount++;
        if (graveHitCount < GRAVE_HITS_REQUIRED) {
            return false;
        }

        // Grave fully opened
        graveHitCount = 0;

        boolean witnessed = isWitnessedByVicarOrGravedigger(playerX, playerY, playerZ, npcs);

        if (witnessed) {
            // Witnessed: heavy consequences
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(GRAVE_ROB_WITNESSED_NOTORIETY,
                        achievementCallback != null ? achievementCallback::award : null);
            }
            if (wantedSystem != null) {
                wantedSystem.onCrimeWitnessed(GRAVE_ROB_WITNESSED_SEVERITY,
                        playerX, playerY, playerZ, null, null);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.GRAVE_ROBBING);
            }
            if (noiseSystem != null) {
                noiseSystem.addNoise(GRAVE_ROB_WITNESSED_NOISE);
            }
        } else {
            // Unwitnessed: lighter consequences + achievement
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(GRAVE_ROB_UNWITNESSED_NOTORIETY,
                        achievementCallback != null ? achievementCallback::award : null);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.GRAVE_ROBBING);
            }
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.GRAVE_ROBBER);
            }
        }

        // Roll loot from weighted table
        if (inventory != null) {
            Material loot = rollGraveLoot();
            if (loot != null) {
                inventory.addItem(loot, 1);
            }
        }

        return true;
    }

    /**
     * Reset the grave hit counter (e.g. player walks away).
     */
    public void resetGraveHits() {
        graveHitCount = 0;
    }

    /**
     * Called when the player hits the cemetery gate while it is locked.
     *
     * <p>After {@link #GATE_LOCKPICK_HITS} hits the gate opens. Grants Notoriety +2
     * and records TRESPASSING.
     *
     * @param achievementCallback callback for achievements
     * @return true if the gate was successfully lockpicked on this hit
     */
    public boolean onGateLockpickHit(AchievementCallback achievementCallback) {
        if (!gateLocked) return false;
        gateLockpickHits++;
        if (gateLockpickHits < GATE_LOCKPICK_HITS) {
            return false;
        }

        // Gate unlocked
        gateLockpickHits = 0;
        gateLocked = false;

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(GATE_LOCKPICK_NOTORIETY,
                    achievementCallback != null ? achievementCallback::award : null);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.TRESPASSING);
        }

        return true;
    }

    /**
     * Reset the gate lockpick counter (e.g. player walks away).
     */
    public void resetGateLockpickHits() {
        gateLockpickHits = 0;
    }

    /**
     * Player places FUNERAL_FLOWERS on a headstone.
     *
     * @param inventory           player's inventory
     * @param achievementCallback callback for achievements
     * @return true if flowers were placed
     */
    public boolean placeFlowersOnHeadstone(Inventory inventory,
                                            AchievementCallback achievementCallback) {
        if (inventory == null) return false;
        if (inventory.getItemCount(Material.FUNERAL_FLOWERS) <= 0) return false;
        inventory.removeItem(Material.FUNERAL_FLOWERS, 1);

        if (notorietySystem != null) {
            // Negative notoriety = reduction
            notorietySystem.reduceNotoriety(-FLOWER_PLACE_NOTORIETY,
                    achievementCallback != null ? achievementCallback::award : null);
        }
        return true;
    }

    /**
     * Player steals FUNERAL_FLOWERS from a headstone.
     *
     * @param inventory           player's inventory
     * @param achievementCallback callback for achievements
     * @return true if flowers were stolen
     */
    public boolean stealFlowersFromHeadstone(Inventory inventory,
                                              AchievementCallback achievementCallback) {
        if (inventory == null) return false;
        inventory.addItem(Material.FUNERAL_FLOWERS, 1);

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(FLOWER_STEAL_NOTORIETY,
                    achievementCallback != null ? achievementCallback::award : null);
        }
        return true;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Returns true if a VICAR or GRAVEDIGGER NPC is within
     * {@link #WITNESS_DETECT_RANGE} of the player.
     */
    private boolean isWitnessedByVicarOrGravedigger(float px, float py, float pz,
                                                      List<NPC> npcs) {
        for (NPC npc : npcs) {
            if (npc.getType() != NPCType.VICAR && npc.getType() != NPCType.GRAVEDIGGER) continue;
            float dx = npc.getPosition().x - px;
            float dy = npc.getPosition().y - py;
            float dz = npc.getPosition().z - pz;
            float distSq = dx * dx + dy * dy + dz * dz;
            if (distSq <= WITNESS_DETECT_RANGE * WITNESS_DETECT_RANGE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Roll the grave loot table and return the dropped material, or null for no loot.
     */
    Material rollGraveLoot() {
        int roll = random.nextInt(100);
        if (roll < LOOT_WEIGHT_WEDDING_RING) {
            return Material.WEDDING_RING;
        } else if (roll < LOOT_WEIGHT_GOLD_RING) {
            return Material.GOLD_RING;
        } else if (roll < LOOT_WEIGHT_OLD_COIN) {
            return Material.OLD_COIN;
        } else if (roll < LOOT_WEIGHT_POCKET_WATCH) {
            return Material.POCKET_WATCH;
        }
        return null; // empty grave
    }

    // ── Getters (for testing) ─────────────────────────────────────────────────

    public boolean isProcessionActive() {
        return processionActive;
    }

    public float getProcessionTimer() {
        return processionTimer;
    }

    public List<NPC> getMournerNpcs() {
        return mournerNpcs;
    }

    public NPC getVicarNpc() {
        return vicarNpc;
    }

    public NPC getGravediggerNpc() {
        return gravediggerNpc;
    }

    public boolean isGravediggerSpawned() {
        return gravediggerSpawned;
    }

    public boolean isGateLocked() {
        return gateLocked;
    }

    public int getGraveHitCount() {
        return graveHitCount;
    }

    public int getLastFuneralDay() {
        return lastFuneralDay;
    }

    public boolean isNightOwlAwarded() {
        return nightOwlAwarded;
    }

    // ── Setters for testing ───────────────────────────────────────────────────

    void setLastFuneralDayForTesting(int day) {
        this.lastFuneralDay = day;
    }

    void setProcessionForTesting(boolean active, float timer) {
        this.processionActive = active;
        this.processionTimer  = timer;
    }

    void setGateLockpickHitsForTesting(int hits) {
        this.gateLockpickHits = hits;
    }

    void setGraveHitCountForTesting(int count) {
        this.graveHitCount = count;
    }

    void setPlayerInsideCemeteryForTesting(boolean inside) {
        this.playerInsideCemetery = inside;
    }

    void setNightOwlAwardedForTesting(boolean awarded) {
        this.nightOwlAwarded = awarded;
    }
}

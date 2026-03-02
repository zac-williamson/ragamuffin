package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.PropPosition;
import ragamuffin.world.PropType;
import ragamuffin.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Issue #1273: Northfield Fly-Tipping Ring — Dodgy Waste Disposal, Enforcement Vans
 * &amp; the White Van Hustle.
 *
 * <p>Manages the unlicensed waste-clearance operation:
 * <ul>
 *   <li><b>White Van Hustle</b> — player accepts clearance jobs from {@link PropType#CLEARANCE_JOB_BOARD_PROP}
 *       or residential NPCs; job types: House Clearance (4–8 JUNK_ITEM, 6–12 COIN),
 *       Garden Rubbish (2–4 GARDEN_WASTE_BAG, 3–6 COIN), Builder's Rubble (3 RUBBLE_SACK, 8 COIN).</li>
 *   <li><b>Disposal choices</b> — fly-tip (illegal, spawns FLY_TIP_PILE_PROP, +2 Notoriety, FLY_TIPPING crime,
 *       COUNCIL_VAN_OFFICER after 120 s); legitimate disposal at Recycling Centre (costs DISPOSAL_FEE_COIN
 *       per item, grants COMMUNITY_WIN rumour); burn near CAMPFIRE/WHEELIE_BIN_FIRE (NoiseSystem level 6,
 *       30% FIRE_BRIGADE chance, ARSON crime if fire spreads).</li>
 *   <li><b>Council Enforcement</b> — Gary (COUNCIL_VAN_OFFICER) spawns near pile after
 *       {@value #FLY_TIP_RESPONSE_SECONDS}s; active 07:00–20:00 only;
 *       confronts player within 8 blocks; issues FIXED_PENALTY_NOTICE (15 COIN fine).</li>
 *   <li><b>Evidence Mechanic</b> — CCTV within 6 blocks records the dump; tape must be stolen
 *       within 60 s; else CAUGHT_ON_CAMERA crime + WantedSystem +1 star.</li>
 *   <li><b>Neighbourhood Impact</b> — Vibes −1/day per active pile; 3+ simultaneous piles triggers
 *       newspaper headline "Fly-Tipping Crisis Hits Northfield"; pile within 10 blocks of food
 *       venue adds RAT_PENALTY to EnvironmentalHealthSystem inspection.</li>
 * </ul>
 *
 * <h3>Integration points:</h3>
 * <ul>
 *   <li>{@link EnvironmentalHealthSystem} — rat penalty from piles near food venues.</li>
 *   <li>{@link NeighbourhoodSystem} — Vibes −1/day per uncleared pile.</li>
 *   <li>{@link WantedSystem} — +1 star on evasion or CCTV capture.</li>
 *   <li>{@link CriminalRecord} — FLY_TIPPING, CAUGHT_ON_CAMERA, EVADING_ENFORCEMENT.</li>
 *   <li>{@link NewspaperSystem} — headline at 3+ simultaneous piles.</li>
 *   <li>{@link RumourNetwork} — BONFIRE_BEHIND_GARAGES after burning load.</li>
 *   <li>{@link NotorietySystem} — +2 Notoriety on fly-tip; −1 on paid fine.</li>
 *   <li>{@link NoiseSystem} — level 6 noise on burning.</li>
 * </ul>
 */
public class FlyTippingSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Seconds after fly-tip before the COUNCIL_VAN_OFFICER spawns. */
    public static final float FLY_TIP_RESPONSE_SECONDS = 120f;

    /** Seconds the player has to steal the CCTV tape before CAUGHT_ON_CAMERA is recorded. */
    public static final float CCTV_TAPE_WINDOW_SECONDS = 60f;

    /** Seconds after paying the fine before the pile is cleared. */
    public static final float PILE_CLEAR_AFTER_FINE_SECONDS = 60f;

    /** Maximum number of items the fly-tip load can hold (base, without van). */
    public static final int MAX_LOAD_BASE = 10;

    /** In-game days before an uncleared pile decays naturally. */
    public static final int PILE_DECAY_DAYS = 2;

    /** Notoriety added when fly-tipping a load. */
    public static final int FLY_TIP_NOTORIETY_GAIN = 2;

    /** Notoriety removed when player pays the fixed penalty notice. */
    public static final int FINE_NOTORIETY_REDUCTION = 1;

    /** Cost in COIN per item for legitimate Recycling Centre disposal. */
    public static final int DISPOSAL_FEE_COIN = 3;

    /** COIN deducted when the player accepts (pays) the fixed penalty notice. */
    public static final int FIXED_PENALTY_COIN = 15;

    /** Blocks within which the COUNCIL_VAN_OFFICER confronts the player. */
    public static final float OFFICER_CONFRONTATION_RANGE = 8f;

    /** Blocks within which CCTV records the fly-tip event. */
    public static final float CCTV_DETECTION_RANGE = 6f;

    /** Blocks within which an active pile penalises a nearby food venue. */
    public static final float RAT_PENALTY_RANGE = 10f;

    /** Number of simultaneous piles required to trigger the crisis newspaper headline. */
    public static final int HEADLINE_PILE_THRESHOLD = 3;

    /** WantedSystem stars added on CCTV evidence. */
    public static final int CCTV_WANTED_STARS = 1;

    /** WantedSystem stars added when evading the council officer. */
    public static final int EVASION_WANTED_STARS = 1;

    /** Noise level generated when burning a fly-tip load. */
    public static final float BURN_NOISE_LEVEL = 6f;

    /** Probability (0–1) that burning a load spawns the FIRE_BRIGADE_NPC. */
    public static final float FIRE_BRIGADE_CHANCE = 0.30f;

    /** Earliest hour the council officer will be active. */
    public static final float OFFICER_START_HOUR = 7f;

    /** Latest hour the council officer will be active. */
    public static final float OFFICER_END_HOUR = 20f;

    /** Vibes reduction per in-game day per active pile. */
    public static final int VIBES_PENALTY_PER_DAY = 1;

    /** Vibes restored when a pile is cleared. */
    public static final int VIBES_RESTORED_ON_CLEAR = 1;

    // ── Job types ─────────────────────────────────────────────────────────────

    /** Clearance job types offered at the job board or by residential NPCs. */
    public enum JobType {
        /** Collect 4–8 JUNK_ITEM from a house. Pay: 6–12 COIN. */
        HOUSE_CLEARANCE,
        /** Collect 2–4 GARDEN_WASTE_BAG items. Pay: 3–6 COIN. */
        GARDEN_RUBBISH,
        /** Collect 3 RUBBLE_SACK from a build site. Pay: 8 COIN fixed. */
        BUILDERS_RUBBLE
    }

    /** Disposal method chosen by the player after completing a job. */
    public enum DisposalMethod {
        FLY_TIP,
        RECYCLING_CENTRE,
        BURN
    }

    /** Result returned when attempting to load an item into the fly-tip load. */
    public enum LoadResult {
        LOADED,
        LOAD_FULL,
        NO_ACTIVE_JOB
    }

    // ── Inner classes ─────────────────────────────────────────────────────────

    /**
     * Represents a single active clearance job.
     */
    public static class ClearanceJob {
        public final JobType type;
        public final int required;
        public final int pay;
        public int loaded;

        public ClearanceJob(JobType type, int required, int pay) {
            this.type = type;
            this.required = required;
            this.pay = pay;
            this.loaded = 0;
        }

        public boolean isComplete() {
            return loaded >= required;
        }
    }

    /**
     * Tracks a single active fly-tip pile in the world.
     */
    public static class FlyTipPile {
        public final Vector3 position;
        public float officerSpawnTimer;   // counts up; officer spawns at FLY_TIP_RESPONSE_SECONDS
        public boolean officerSpawned;
        public boolean cctvEvidence;      // true if CCTV recorded and tape not stolen
        public float cctvTapeTimer;       // counts down from CCTV_TAPE_WINDOW_SECONDS
        public boolean cctvActive;        // true while tape-steal window is open
        public float dayAgeSeconds;       // accumulates in-game seconds (simplification)
        public int daysOld;
        public boolean fineAccepted;
        public float fineClearTimer;      // counts up after fine paid; pile removed at threshold
        public int propIndex;             // index in World.getPropPositions()
        public boolean headlineTriggered;

        public FlyTipPile(Vector3 position, int propIndex) {
            this.position = new Vector3(position);
            this.officerSpawnTimer = 0f;
            this.officerSpawned = false;
            this.cctvEvidence = false;
            this.cctvTapeTimer = 0f;
            this.cctvActive = false;
            this.dayAgeSeconds = 0f;
            this.daysOld = 0;
            this.fineAccepted = false;
            this.fineClearTimer = 0f;
            this.propIndex = propIndex;
            this.headlineTriggered = false;
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;
    private final List<FlyTipPile> activePiles = new ArrayList<>();
    private ClearanceJob activeJob = null;
    private int loadCount = 0;

    /** Number of loads fly-tipped without getting caught (for THE_ENVIRONMENT_THOUGH). */
    private int uncaughtFlyTips = 0;

    /** Number of loads disposed legitimately (for CIVIC_PRIDE). */
    private int legitimateDisposals = 0;

    /** Whether the HEADLINE_SHAME achievement has been awarded this session. */
    private boolean headlineShameAwarded = false;

    /** Whether the GRIM_REAPER achievement has been awarded this session. */
    private boolean grimReaperAwarded = false;

    /** Whether the first-dump tooltip has been shown. */
    private boolean firstDumpTooltipShown = false;

    // ── Injected systems ──────────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private NeighbourhoodSystem neighbourhoodSystem;
    private NewspaperSystem newspaperSystem;
    private RumourNetwork rumourNetwork;
    private NoiseSystem noiseSystem;
    private EnvironmentalHealthSystem environmentalHealthSystem;
    private TooltipSystem tooltipSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public FlyTippingSystem() {
        this(new Random());
    }

    public FlyTippingSystem(Random random) {
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

    public void setNeighbourhoodSystem(NeighbourhoodSystem neighbourhoodSystem) {
        this.neighbourhoodSystem = neighbourhoodSystem;
    }

    public void setNewspaperSystem(NewspaperSystem newspaperSystem) {
        this.newspaperSystem = newspaperSystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setNoiseSystem(NoiseSystem noiseSystem) {
        this.noiseSystem = noiseSystem;
    }

    public void setEnvironmentalHealthSystem(EnvironmentalHealthSystem ehs) {
        this.environmentalHealthSystem = ehs;
    }

    public void setTooltipSystem(TooltipSystem tooltipSystem) {
        this.tooltipSystem = tooltipSystem;
    }

    // ── Job system ────────────────────────────────────────────────────────────

    /**
     * Generates and returns a random clearance job. Replaces any existing active job.
     *
     * @param random optional override RNG; uses system random if null
     * @return the new ClearanceJob
     */
    public ClearanceJob generateJob(Random random) {
        Random rng = (random != null) ? random : this.random;
        JobType type = JobType.values()[rng.nextInt(JobType.values().length)];
        int required;
        int pay;
        switch (type) {
            case HOUSE_CLEARANCE:
                required = 4 + rng.nextInt(5); // 4–8
                pay = 6 + rng.nextInt(7);       // 6–12
                break;
            case GARDEN_RUBBISH:
                required = 2 + rng.nextInt(3); // 2–4
                pay = 3 + rng.nextInt(4);       // 3–6
                break;
            case BUILDERS_RUBBLE:
            default:
                required = 3;
                pay = 8;
                break;
        }
        activeJob = new ClearanceJob(type, required, pay);
        loadCount = 0;
        return activeJob;
    }

    /**
     * Attempt to load one waste item into the fly-tip load for the active job.
     *
     * @return {@link LoadResult#LOADED} on success, {@link LoadResult#LOAD_FULL} if at capacity,
     *         {@link LoadResult#NO_ACTIVE_JOB} if no job is active.
     */
    public LoadResult loadItem() {
        if (activeJob == null) {
            return LoadResult.NO_ACTIVE_JOB;
        }
        if (loadCount >= MAX_LOAD_BASE) {
            return LoadResult.LOAD_FULL;
        }
        loadCount++;
        activeJob.loaded = loadCount;
        return LoadResult.LOADED;
    }

    /**
     * Mark the active job as complete (called when {@code loadCount == jobRequired}).
     * Pays the player the job's pay in COIN.
     *
     * @param inventory player inventory to receive COIN
     * @param achievementCallback for GRIM_REAPER achievement
     * @return the pay amount, or 0 if no active job
     */
    public int completeJob(Inventory inventory, NotorietySystem.AchievementCallback achievementCallback) {
        if (activeJob == null || !activeJob.isComplete()) {
            return 0;
        }
        int pay = activeJob.pay;
        if (inventory != null) {
            for (int i = 0; i < pay; i++) {
                inventory.addItem(Material.COIN, 1);
            }
        }
        if (!grimReaperAwarded && achievementCallback != null) {
            grimReaperAwarded = true;
            achievementCallback.award(AchievementType.GRIM_REAPER);
        }
        activeJob = null;
        return pay;
    }

    // ── Disposal ──────────────────────────────────────────────────────────────

    /**
     * Fly-tip the current load at the given position.
     *
     * <p>Effects:
     * <ul>
     *   <li>Spawns a {@link PropType#FLY_TIP_PILE_PROP} in the world.</li>
     *   <li>Records {@link CrimeType#FLY_TIPPING} and adds {@value #FLY_TIP_NOTORIETY_GAIN} Notoriety.</li>
     *   <li>Starts a timer; COUNCIL_VAN_OFFICER spawns after {@value #FLY_TIP_RESPONSE_SECONDS} s.</li>
     *   <li>If CCTV is within {@value #CCTV_DETECTION_RANGE} blocks, opens a 60-second tape-steal window.</li>
     * </ul>
     *
     * @param dumpPosition       world position of the dump
     * @param cctvNearby         true if a CCTV_PROP is within {@value #CCTV_DETECTION_RANGE} blocks
     * @param world              game world for prop placement
     * @param achievementCallback for achievement tracking
     * @return true if the load was fly-tipped successfully
     */
    public boolean flyTip(Vector3 dumpPosition, boolean cctvNearby, World world,
                          NotorietySystem.AchievementCallback achievementCallback) {
        if (loadCount <= 0) {
            return false;
        }

        // Spawn FLY_TIP_PILE_PROP
        int propIdx = -1;
        if (world != null) {
            PropPosition pile = new PropPosition(
                    dumpPosition.x, dumpPosition.y, dumpPosition.z,
                    PropType.FLY_TIP_PILE_PROP, 0f);
            world.addPropPosition(pile);
            propIdx = world.getPropPositions().size() - 1;
        }

        FlyTipPile pileRecord = new FlyTipPile(dumpPosition, propIdx);

        // CCTV evidence window
        if (cctvNearby) {
            pileRecord.cctvActive = true;
            pileRecord.cctvTapeTimer = CCTV_TAPE_WINDOW_SECONDS;
        }

        activePiles.add(pileRecord);

        // Crime and notoriety
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.FLY_TIPPING);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(FLY_TIP_NOTORIETY_GAIN, achievementCallback);
        }

        // Tooltip
        if (tooltipSystem != null && !firstDumpTooltipShown) {
            firstDumpTooltipShown = true;
            tooltipSystem.showMessage("Out of sight, out of mind. Mostly.", 4.0f);
        }

        // Check for crisis headline trigger
        checkHeadlineTrigger(achievementCallback);

        loadCount = 0;
        return true;
    }

    /**
     * Dispose of the current load legitimately at the Recycling Centre.
     * Costs {@link #DISPOSAL_FEE_COIN} per item loaded.
     *
     * @param inventory          player inventory to deduct COIN from
     * @param player             NPC source for rumour seeding (may be null)
     * @param achievementCallback for CIVIC_PRIDE achievement
     * @return coin cost deducted, or −1 if not enough COIN
     */
    public int disposeLegitimately(Inventory inventory, NPC player,
                                   NotorietySystem.AchievementCallback achievementCallback) {
        if (loadCount <= 0) {
            return 0;
        }
        int cost = loadCount * DISPOSAL_FEE_COIN;
        if (inventory != null && inventory.getItemCount(Material.COIN) < cost) {
            return -1; // not enough coin
        }
        if (inventory != null) {
            inventory.removeItem(Material.COIN, cost);
        }

        // Seed COMMUNITY_WIN rumour
        if (rumourNetwork != null) {
            rumourNetwork.addRumour(player, new Rumour(RumourType.COMMUNITY_WIN,
                    "Someone took their rubbish to the tip properly — nice to see."));
        }

        // Neighbourhood vibes bonus (pile cleared = +1)
        if (neighbourhoodSystem != null) {
            neighbourhoodSystem.setVibes(neighbourhoodSystem.getVibes() + VIBES_RESTORED_ON_CLEAR);
        }

        // Achievement tracking
        legitimateDisposals++;
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.CIVIC_PRIDE);
        }

        loadCount = 0;
        activeJob = null;
        return cost;
    }

    /**
     * Burn the current load near a campfire or wheelie bin fire.
     *
     * <p>Effects:
     * <ul>
     *   <li>Emits noise at level {@value #BURN_NOISE_LEVEL}.</li>
     *   <li>{@value #FIRE_BRIGADE_CHANCE}×100% chance FIRE_BRIGADE_NPC is added to the NPC list.</li>
     *   <li>Seeds {@link RumourType#BONFIRE_BEHIND_GARAGES} rumour.</li>
     *   <li>Awards {@link AchievementType#BURN_IT_ALL} if fire brigade triggered.</li>
     * </ul>
     *
     * @param burnPosition       world position of the burn
     * @param npcs               active NPC list (FIRE_BRIGADE appended if triggered)
     * @param nearestNpc         NPC near the fire for rumour seeding (may be null)
     * @param achievementCallback for BURN_IT_ALL achievement
     * @return true if the fire brigade was triggered
     */
    public boolean burnLoad(Vector3 burnPosition, List<NPC> npcs, NPC nearestNpc,
                            NotorietySystem.AchievementCallback achievementCallback) {
        if (loadCount <= 0) {
            return false;
        }

        // Emit noise
        if (noiseSystem != null) {
            noiseSystem.emitNoise(burnPosition, BURN_NOISE_LEVEL);
        }

        // Seed rumour
        if (rumourNetwork != null) {
            rumourNetwork.addRumour(nearestNpc, new Rumour(RumourType.BONFIRE_BEHIND_GARAGES,
                    "Someone's been having a bonfire behind the garages again."));
        }

        // Tooltip
        if (tooltipSystem != null) {
            tooltipSystem.showMessage("What the nose doesn't know...", 3.0f);
        }

        boolean fireBrigadeSpawned = random.nextFloat() < FIRE_BRIGADE_CHANCE;
        if (fireBrigadeSpawned) {
            // Spawn FIRE_BRIGADE_NPC
            if (npcs != null) {
                NPC fireCrew = new NPC(NPCType.FIRE_ENGINE,
                        burnPosition.x + 5f, burnPosition.y, burnPosition.z);
                npcs.add(fireCrew);
            }
            // Award achievement
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.BURN_IT_ALL);
            }
            if (tooltipSystem != null) {
                tooltipSystem.showMessage("Plan B was a bit much.", 4.0f);
            }
        }

        loadCount = 0;
        activeJob = null;
        return fireBrigadeSpawned;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update all active fly-tip piles each frame.
     *
     * <p>For each active pile:
     * <ul>
     *   <li>Count up the officer spawn timer; spawn COUNCIL_VAN_OFFICER at threshold.</li>
     *   <li>Count down the CCTV tape-steal window; lock in CAUGHT_ON_CAMERA if expired.</li>
     *   <li>Count up fine-clear timer; remove pile when threshold reached.</li>
     *   <li>Accumulate day age; apply Vibes −1/day per pile.</li>
     *   <li>Remove piles after {@value #PILE_DECAY_DAYS} days.</li>
     * </ul>
     *
     * @param delta                real-seconds since last frame
     * @param currentHour          current in-game hour (for officer active-hours check)
     * @param inGameSecondsPerDay  how many real-seconds constitute one in-game day
     * @param npcs                 active NPC list (COUNCIL_VAN_OFFICER appended if triggered)
     * @param player               player reference (for distance checks)
     * @param achievementCallback  for achievement tracking
     */
    public void update(float delta, float currentHour, float inGameSecondsPerDay,
                       List<NPC> npcs, Player player,
                       NotorietySystem.AchievementCallback achievementCallback) {

        Iterator<FlyTipPile> iter = activePiles.iterator();
        while (iter.hasNext()) {
            FlyTipPile pile = iter.next();

            // ── CCTV tape window ───────────────────────────────────────────────
            if (pile.cctvActive) {
                pile.cctvTapeTimer -= delta;
                if (pile.cctvTapeTimer <= 0f) {
                    pile.cctvActive = false;
                    pile.cctvEvidence = true;
                    // Record CAUGHT_ON_CAMERA
                    if (criminalRecord != null) {
                        criminalRecord.record(CrimeType.CAUGHT_ON_CAMERA);
                    }
                    if (wantedSystem != null) {
                        float px = player != null ? player.getPosition().x : pile.position.x;
                        float py = player != null ? player.getPosition().y : pile.position.y;
                        float pz = player != null ? player.getPosition().z : pile.position.z;
                        wantedSystem.addWantedStars(CCTV_WANTED_STARS, px, py, pz, null);
                    }
                }
            }

            // ── Officer spawn timer ────────────────────────────────────────────
            if (!pile.officerSpawned) {
                boolean officerHours = (currentHour >= OFFICER_START_HOUR
                        && currentHour < OFFICER_END_HOUR);
                if (officerHours) {
                    pile.officerSpawnTimer += delta;
                    if (pile.officerSpawnTimer >= FLY_TIP_RESPONSE_SECONDS) {
                        pile.officerSpawned = true;
                        if (npcs != null) {
                            NPC officer = new NPC(NPCType.COUNCIL_VAN_OFFICER,
                                    pile.position.x + 2f, pile.position.y, pile.position.z + 2f);
                            npcs.add(officer);
                        }
                        if (tooltipSystem != null) {
                            tooltipSystem.showMessage(
                                    "That's yours, mate. I can see the receipt.", 4.0f);
                        }
                    }
                }
            }

            // ── Fine clear timer ───────────────────────────────────────────────
            if (pile.fineAccepted) {
                pile.fineClearTimer += delta;
                if (pile.fineClearTimer >= PILE_CLEAR_AFTER_FINE_SECONDS) {
                    removePile(pile, iter);
                    continue;
                }
            }

            // ── Day age tracking ───────────────────────────────────────────────
            if (inGameSecondsPerDay > 0f) {
                pile.dayAgeSeconds += delta;
                int newDays = (int)(pile.dayAgeSeconds / inGameSecondsPerDay);
                if (newDays > pile.daysOld) {
                    int daysPassed = newDays - pile.daysOld;
                    pile.daysOld = newDays;
                    // Vibes penalty per day
                    if (neighbourhoodSystem != null) {
                        neighbourhoodSystem.setVibes(
                                neighbourhoodSystem.getVibes() - daysPassed * VIBES_PENALTY_PER_DAY);
                    }
                    // Remove after PILE_DECAY_DAYS
                    if (pile.daysOld >= PILE_DECAY_DAYS) {
                        removePile(pile, iter);
                        continue;
                    }
                }
            }
        }
    }

    // ── Officer confrontation ─────────────────────────────────────────────────

    /**
     * Called when a COUNCIL_VAN_OFFICER confronts the player.
     *
     * @param player             player entity
     * @param inventory          player inventory (COIN deducted on fine payment)
     * @param achievementCallback for notoriety changes
     * @return true if the player was within range and confrontation happened
     */
    public boolean officerConfront(Player player, Inventory inventory,
                                   NotorietySystem.AchievementCallback achievementCallback) {
        if (activePiles.isEmpty()) {
            return false;
        }
        // Find the pile whose officer is spawned and player is nearby
        for (FlyTipPile pile : activePiles) {
            if (!pile.officerSpawned) continue;
            float dx = player.getPosition().x - pile.position.x;
            float dz = player.getPosition().z - pile.position.z;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist <= OFFICER_CONFRONTATION_RANGE) {
                // Issue fine
                if (inventory != null) {
                    inventory.addItem(Material.FIXED_PENALTY_NOTICE, 1);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Called when the player pays the fixed penalty notice (or it is auto-deducted).
     * Reduces Notoriety by {@value #FINE_NOTORIETY_REDUCTION} and starts the pile-clear timer.
     *
     * @param pile              the pile being cleared
     * @param inventory         player inventory (COIN deducted)
     * @param achievementCallback for notoriety changes
     * @return true if payment succeeded
     */
    public boolean payFine(FlyTipPile pile, Inventory inventory,
                           NotorietySystem.AchievementCallback achievementCallback) {
        if (inventory != null && inventory.getItemCount(Material.COIN) < FIXED_PENALTY_COIN) {
            return false;
        }
        if (inventory != null) {
            inventory.removeItem(Material.COIN, FIXED_PENALTY_COIN);
        }
        pile.fineAccepted = true;
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(-FINE_NOTORIETY_REDUCTION, achievementCallback);
        }
        // Track uncaught fly-tips for THE_ENVIRONMENT_THOUGH (paying fine = caught, reset)
        uncaughtFlyTips = 0;
        return true;
    }

    /**
     * Called when the player evades the council officer (moves &gt;8 blocks away without paying).
     *
     * @param player             player entity for location
     * @param achievementCallback for notoriety changes
     */
    public void evadeOfficer(Player player, NotorietySystem.AchievementCallback achievementCallback) {
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.EVADING_ENFORCEMENT);
        }
        if (wantedSystem != null) {
            float px = player != null ? player.getPosition().x : 0f;
            float py = player != null ? player.getPosition().y : 0f;
            float pz = player != null ? player.getPosition().z : 0f;
            wantedSystem.addWantedStars(EVASION_WANTED_STARS, px, py, pz, null);
        }

        // Track uncaught fly-tips
        uncaughtFlyTips++;
        if (uncaughtFlyTips >= 5 && achievementCallback != null) {
            achievementCallback.award(AchievementType.THE_ENVIRONMENT_THOUGH);
        }
    }

    // ── CCTV tape steal ───────────────────────────────────────────────────────

    /**
     * Called when the player steals the CCTV tape within the evidence window.
     * Clears the CCTV evidence flag for the nearest active pile.
     *
     * @param playerPosition player position
     * @return true if a pile had its tape window cleared
     */
    public boolean stealCctvTape(Vector3 playerPosition) {
        for (FlyTipPile pile : activePiles) {
            if (pile.cctvActive) {
                float dx = playerPosition.x - pile.position.x;
                float dz = playerPosition.z - pile.position.z;
                float dist = (float) Math.sqrt(dx * dx + dz * dz);
                if (dist <= CCTV_DETECTION_RANGE * 2f) {
                    pile.cctvActive = false;
                    pile.cctvEvidence = false;
                    return true;
                }
            }
        }
        return false;
    }

    // ── Headline trigger ──────────────────────────────────────────────────────

    /**
     * Check if 3+ piles are active and trigger the crisis headline.
     */
    private void checkHeadlineTrigger(NotorietySystem.AchievementCallback achievementCallback) {
        if (activePiles.size() >= HEADLINE_PILE_THRESHOLD) {
            if (newspaperSystem != null) {
                newspaperSystem.publishHeadline("FLY-TIPPING CRISIS HITS NORTHFIELD");
            }
            if (!headlineShameAwarded && achievementCallback != null) {
                headlineShameAwarded = true;
                achievementCallback.award(AchievementType.HEADLINE_SHAME);
            }
        }
    }

    // ── EnvironmentalHealthSystem integration ─────────────────────────────────

    /**
     * Returns true if any active pile is within {@value #RAT_PENALTY_RANGE} blocks
     * of the given food venue position. Used by EnvironmentalHealthSystem to apply
     * the rat penalty during inspections.
     *
     * @param venueX X world coordinate of the food venue
     * @param venueZ Z world coordinate of the food venue
     * @return true if a pile is within range
     */
    public boolean hasPileNearVenue(float venueX, float venueZ) {
        for (FlyTipPile pile : activePiles) {
            float dx = pile.position.x - venueX;
            float dz = pile.position.z - venueZ;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist <= RAT_PENALTY_RANGE) {
                return true;
            }
        }
        return false;
    }

    // ── Utility / helpers ─────────────────────────────────────────────────────

    /**
     * Remove a pile from the active list, restore Vibes, and remove the prop from the world.
     */
    private void removePile(FlyTipPile pile, Iterator<FlyTipPile> iter) {
        iter.remove();
        if (neighbourhoodSystem != null) {
            neighbourhoodSystem.setVibes(
                    neighbourhoodSystem.getVibes() + VIBES_RESTORED_ON_CLEAR);
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Returns the current number of items loaded onto the fly-tip load. */
    public int getLoadCount() {
        return loadCount;
    }

    /** Returns the currently active clearance job, or null. */
    public ClearanceJob getActiveJob() {
        return activeJob;
    }

    /** Returns the number of active fly-tip piles in the world. */
    public int getActivePileCount() {
        return activePiles.size();
    }

    /** Returns a read-only view of active piles (for integration queries). */
    public List<FlyTipPile> getActivePiles() {
        return java.util.Collections.unmodifiableList(activePiles);
    }

    /** Direct access for testing — adds a pre-constructed pile. */
    public void addPile(FlyTipPile pile) {
        activePiles.add(pile);
    }

    /** Direct access for testing — sets load count. */
    public void setLoadCount(int count) {
        this.loadCount = count;
    }

    /** Returns the number of loads disposed legitimately. */
    public int getLegitimateDisposals() {
        return legitimateDisposals;
    }

    /** Returns the number of consecutive uncaught fly-tips. */
    public int getUncaughtFlyTips() {
        return uncaughtFlyTips;
    }
}

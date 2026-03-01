package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1043: Northfield Fire Station — Watch Commander, Engine Heist &amp; the Arson Callout.
 *
 * <p>Manages the fire station landmark: callout response, engine heist, fire extinguisher
 * interaction, firefighter disguise, and the antidepressants delivery quest.
 *
 * <h3>Core mechanics</h3>
 * <ul>
 *   <li><b>Callout</b> — {@link #onFireCallout(float, float, float)} opens the BAY_DOOR,
 *       deploys 2 FIREFIGHTERs with the engine, and returns them after
 *       {@link #CALLOUT_RETURN_SECONDS} seconds. {@link #isEngineDeployed()} tracks state.</li>
 *   <li><b>Engine Heist</b> — player presses E on FIRE_ENGINE_PROP while no FIREFIGHTER is
 *       within 6 blocks. 8-second ignition, then engine is driveable via
 *       {@link CarDrivingSystem}. Sets {@link #isEngineStolen()}, awards +3 wanted stars,
 *       records {@link CriminalRecord.CrimeType#FIRE_ENGINE_STOLEN}, seeds
 *       {@link RumourType#MAJOR_THEFT} to 5 NPCs, and unlocks
 *       {@link AchievementType#GREAT_ENGINE_HEIST}.</li>
 *   <li><b>Crusher fence</b> — park stolen engine at SCRAPYARD 20:00–07:00, dwell 60 s →
 *       40 COIN, record cleared.</li>
 *   <li><b>Fire extinguisher</b> — pick up from HOSE_REEL (60 % detection chance if a
 *       FIREFIGHTER is within 6 blocks).</li>
 *   <li><b>Disguise</b> — FIREFIGHTER_HELMET grants {@code isDisguised(NPCType.FIREFIGHTER)};
 *       WATCH_COMMANDER sees through at ≤ 3 blocks.</li>
 *   <li><b>Antidepressants quest</b> — deliver ANTIDEPRESSANTS to WATCH_COMMANDER for
 *       5 COIN + 10 neighbourhood vibes + {@link RumourType#LOCAL_EVENT}.</li>
 * </ul>
 *
 * <h3>Open hours</h3>
 * 24/7. Staffed by at least 2 FIREFIGHTERs + 1 WATCH_COMMANDER; 4 FIREFIGHTERs 08:00–18:00.
 */
public class FireStationSystem {

    // ── Staffing constants ────────────────────────────────────────────────────

    /** Number of FIREFIGHTER NPCs active during daytime (08:00–18:00). */
    public static final int   FIREFIGHTER_DAYTIME_COUNT   = 4;
    /** Number of FIREFIGHTER NPCs active outside daytime hours. */
    public static final int   FIREFIGHTER_NIGHTTIME_COUNT  = 2;

    // ── Callout constants ─────────────────────────────────────────────────────

    /** Seconds until the engine and crew return from a callout. */
    public static final float CALLOUT_RETURN_SECONDS       = 120f;

    // ── Engine Heist constants ────────────────────────────────────────────────

    /** Ignition time in seconds before the engine becomes driveable. */
    public static final float ENGINE_IGNITION_SECONDS      = 8f;
    /** Wanted stars awarded when the engine is driven out of the bay. */
    public static final int   ENGINE_HEIST_WANTED_STARS    = 3;
    /** Notoriety added on engine heist. */
    public static final int   ENGINE_HEIST_NOTORIETY       = 25;

    // ── Firefighter detection constants ──────────────────────────────────────

    /** Probability that a FIREFIGHTER within 6 blocks detects extinguisher theft. */
    public static final float EXTINGUISHER_DETECT_CHANCE   = 0.60f;
    /** Probability that a FIREFIGHTER within 6 blocks confronts player on heist attempt. */
    public static final float FIREFIGHTER_DETECT_CHANCE    = 0.40f;
    /** Detection radius (blocks) for extinguisher theft / heist blocking. */
    public static final float FIREFIGHTER_DETECT_RADIUS    = 6f;

    // ── WATCH_COMMANDER constants ─────────────────────────────────────────────

    /** Seconds the WATCH_COMMANDER is distracted after player uses a FIRE_EXTINGUISHER on them. */
    public static final float WATCH_COMMANDER_DISTRACT_S   = 8f;
    /** Radius at which WATCH_COMMANDER sees through the FIREFIGHTER_HELMET disguise. */
    public static final float DISGUISE_DETECTION_RADIUS    = 3f;

    // ── Loitering / disguise constants ───────────────────────────────────────

    /** Seconds a player can loiter inside the station before FIREFIGHTER calls WATCH_COMMANDER. */
    public static final float LOITER_ALERT_SECONDS         = 60f;
    /** Radius within which a FIREFIGHTER ignores minor crimes (disguise active). */
    public static final float FIREFIGHTER_BLIND_RADIUS     = 4f;

    // ── Crusher fence constants ───────────────────────────────────────────────

    /** COIN paid out when the stolen engine is crushed at the scrapyard. */
    public static final int   CRUSHER_PAYOUT_COIN          = 40;
    /** Seconds the player must dwell at the scrapyard to complete the crusher event. */
    public static final float CRUSHER_DWELL_SECONDS        = 60f;
    /** Siren slowdown factor applied to police cars while siren is active. */
    public static final float SIREN_POLICE_SLOW_FACTOR     = 0.80f;

    // ── Antidepressants quest constants ──────────────────────────────────────

    /** COIN reward for delivering ANTIDEPRESSANTS to the WATCH_COMMANDER. */
    public static final int   ANTIDEPRESSANTS_QUEST_COIN   = 5;
    /** NeighbourhoodSystem vibes boost for completing the antidepressants quest. */
    public static final int   ANTIDEPRESSANTS_VIBES_BONUS  = 10;
    /** Probability that a FIREFIGHTER_HELMET is found in any given locker. */
    public static final float LOCKER_HELMET_CHANCE         = 0.50f;

    // ── Scrapyard crush time window ───────────────────────────────────────────

    /** Hour at which the scrapyard crusher becomes active (20:00). */
    private static final float CRUSH_WINDOW_START = 20f;
    /** Hour at which the scrapyard crusher becomes inactive (07:00). */
    private static final float CRUSH_WINDOW_END   = 7f;

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Result codes for an attempted engine heist. */
    public enum HeistResult {
        /** The engine was not present (deployed on callout or already stolen). */
        ENGINE_NOT_PRESENT,
        /** A FIREFIGHTER was within 6 blocks — 40 % chance of confrontation. */
        BLOCKED_BY_FIREFIGHTER,
        /** Ignition sequence started — advance {@link #ENGINE_IGNITION_SECONDS} to complete. */
        IGNITION_STARTED
    }

    /** Result codes for attempting to pick up the fire extinguisher. */
    public enum ExtinguisherPickupResult {
        /** No FIREFIGHTER detected; extinguisher added to inventory. */
        PICKED_UP,
        /** FIREFIGHTER detected the theft; THEFT crime recorded, +1 wanted star. */
        DETECTED,
        /** There was no extinguisher to pick up (already taken). */
        NONE_AVAILABLE
    }

    /** Result codes for locker search. */
    public enum LockerResult {
        /** Found a FIREFIGHTER_HELMET. */
        FOUND_HELMET,
        /** Locker was empty. */
        EMPTY
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** True while the engine is out on a fire callout. */
    private boolean engineDeployed;
    /** Elapsed seconds since the last callout began; used to track return time. */
    private float   calloutTimer;

    /** True after the player has successfully stolen the engine from the bay. */
    private boolean engineStolen;

    /** True while the ignition sequence is running. */
    private boolean ignitionActive;
    /** Elapsed seconds into the ignition sequence. */
    private float   ignitionTimer;

    /** True while the BAY_DOOR is open. */
    private boolean bayDoorOpen;

    /** Whether the WATCH_COMMANDER is currently distracted. */
    private boolean watchCommanderDistracted;
    /** Remaining distraction time in seconds. */
    private float   watchCommanderDistractTimer;

    /** Elapsed seconds the player has been loitering inside the station without interaction. */
    private float   loiterTimer;

    /** True if the extinguisher on the HOSE_REEL is still available for pickup. */
    private boolean extinguisherAvailable;

    /** Accumulated dwell seconds at the scrapyard with the stolen engine. */
    private float   scrapyardDwellAccum;

    /** True if the siren is currently active (player pressing E while driving). */
    private boolean sirenActive;

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final Random random;

    private NotorietySystem      notorietySystem;
    private WantedSystem         wantedSystem;
    private CriminalRecord       criminalRecord;
    private RumourNetwork        rumourNetwork;
    private AchievementSystem    achievementSystem;
    private NeighbourhoodSystem  neighbourhoodSystem;
    private WheeliBinFireSystem  wheeliBinFireSystem;
    private DisguiseSystem       disguiseSystem;
    private CarDrivingSystem     carDrivingSystem;

    // ── Constructors ──────────────────────────────────────────────────────────

    public FireStationSystem() {
        this(new Random());
    }

    public FireStationSystem(Random random) {
        this.random               = random;
        this.engineDeployed       = false;
        this.calloutTimer         = 0f;
        this.engineStolen         = false;
        this.ignitionActive       = false;
        this.ignitionTimer        = 0f;
        this.bayDoorOpen          = false;
        this.watchCommanderDistracted    = false;
        this.watchCommanderDistractTimer = 0f;
        this.loiterTimer          = 0f;
        this.extinguisherAvailable = true;
        this.scrapyardDwellAccum  = 0f;
        this.sirenActive          = false;
    }

    // ── Dependency setters ────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem s)     { this.notorietySystem     = s; }
    public void setWantedSystem(WantedSystem s)           { this.wantedSystem        = s; }
    public void setCriminalRecord(CriminalRecord s)       { this.criminalRecord      = s; }
    public void setRumourNetwork(RumourNetwork s)         { this.rumourNetwork       = s; }
    public void setAchievementSystem(AchievementSystem s) { this.achievementSystem   = s; }
    public void setNeighbourhoodSystem(NeighbourhoodSystem s) { this.neighbourhoodSystem = s; }
    public void setWheeliBinFireSystem(WheeliBinFireSystem s) { this.wheeliBinFireSystem = s; }
    public void setDisguiseSystem(DisguiseSystem s)       { this.disguiseSystem      = s; }
    public void setCarDrivingSystem(CarDrivingSystem s)   { this.carDrivingSystem    = s; }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update. Advances callout timer, ignition timer, WATCH_COMMANDER
     * distraction, and loiter timer.
     *
     * @param delta   frame delta in seconds
     * @param player  the player entity (may be null in tests)
     * @param npcs    all active NPCs in the world (may be null)
     */
    public void update(float delta, Player player, List<NPC> npcs) {
        // ── Callout return ────────────────────────────────────────────────────
        if (engineDeployed) {
            calloutTimer += delta;
            if (calloutTimer >= CALLOUT_RETURN_SECONDS) {
                engineDeployed = false;
                calloutTimer   = 0f;
                bayDoorOpen    = false;
            }
        }

        // ── Ignition sequence ─────────────────────────────────────────────────
        if (ignitionActive) {
            ignitionTimer += delta;
            if (ignitionTimer >= ENGINE_IGNITION_SECONDS) {
                ignitionActive = false;
                ignitionTimer  = 0f;
                // Engine is now driveable — CarDrivingSystem integration
                // (real integration sets up the driveable entity; tested via isDriving())
            }
        }

        // ── WATCH_COMMANDER distraction timer ─────────────────────────────────
        if (watchCommanderDistracted) {
            watchCommanderDistractTimer -= delta;
            if (watchCommanderDistractTimer <= 0f) {
                watchCommanderDistracted    = false;
                watchCommanderDistractTimer = 0f;
            }
        }

        // ── Disguise: WATCH_COMMANDER sees through at ≤ DISGUISE_DETECTION_RADIUS ──
        if (disguiseSystem != null && disguiseSystem.isDisguised() && npcs != null && player != null) {
            for (NPC npc : npcs) {
                if (npc.getType() == NPCType.WATCH_COMMANDER) {
                    float dist = npc.getPosition().dst(player.getPosition());
                    if (dist <= DISGUISE_DETECTION_RADIUS) {
                        disguiseSystem.removeDisguise();
                    }
                }
            }
        }

        // ── Loiter timer ──────────────────────────────────────────────────────
        if (player != null) {
            boolean wearingHelmet = player.getInventory() != null
                    && player.getInventory().hasItem(Material.FIREFIGHTER_HELMET, 1);
            if (!wearingHelmet) {
                loiterTimer += delta;
                if (loiterTimer >= LOITER_ALERT_SECONDS && npcs != null) {
                    // Signal WATCH_COMMANDER to come out
                    alertWatchCommander(npcs);
                    loiterTimer = 0f;
                }
            } else {
                loiterTimer = 0f;
            }
        }
    }

    // ── 1. Callout response ───────────────────────────────────────────────────

    /**
     * Called by {@link WheeliBinFireSystem} when a fire engine NPC spawns.
     * Opens the BAY_DOOR, marks the engine as deployed, and starts the callout timer.
     *
     * @param fireX X world coordinate of the fire
     * @param fireY Y world coordinate of the fire
     * @param fireZ Z world coordinate of the fire
     */
    public void onFireCallout(float fireX, float fireY, float fireZ) {
        if (engineStolen) {
            // Engine not present — can't deploy
            return;
        }
        engineDeployed = true;
        bayDoorOpen    = true;
        calloutTimer   = 0f;
    }

    /**
     * Returns true while the engine is out on a callout.
     */
    public boolean isEngineDeployed() {
        return engineDeployed;
    }

    /**
     * Returns true if the bay door is currently open.
     */
    public boolean isBayDoorOpen() {
        return bayDoorOpen;
    }

    // ── 2. Engine Heist ───────────────────────────────────────────────────────

    /**
     * Attempt to start the engine heist. Called when the player presses E on
     * {@link PropType#FIRE_ENGINE_PROP}.
     *
     * <p>Requirements:
     * <ul>
     *   <li>Engine must not be deployed ({@link #isEngineDeployed()} == false).</li>
     *   <li>Engine must not be already stolen ({@link #isEngineStolen()} == false).</li>
     *   <li>No FIREFIGHTER within {@value #FIREFIGHTER_DETECT_RADIUS} blocks (otherwise
     *       40 % confrontation chance).</li>
     * </ul>
     *
     * @param player      the player attempting the heist
     * @param npcs        all active NPCs (used to check for nearby FIREFIGHTERs)
     * @return the heist result
     */
    public HeistResult attemptHeist(Player player, List<NPC> npcs) {
        // Cannot heist if engine is out or already stolen
        if (engineDeployed || engineStolen) {
            return HeistResult.ENGINE_NOT_PRESENT;
        }

        // Check for nearby FIREFIGHTERs
        if (npcs != null) {
            for (NPC npc : npcs) {
                if (npc.getType() == NPCType.FIREFIGHTER || npc.getType() == NPCType.WATCH_COMMANDER) {
                    float dist = npc.getPosition().dst(player.getPosition());
                    if (dist <= FIREFIGHTER_DETECT_RADIUS) {
                        // 40% chance of confrontation
                        if (random.nextFloat() < FIREFIGHTER_DETECT_CHANCE) {
                            if (wantedSystem != null) {
                                wantedSystem.addWantedStars(1,
                                        player.getPosition().x,
                                        player.getPosition().y,
                                        player.getPosition().z, null);
                            }
                        }
                        return HeistResult.BLOCKED_BY_FIREFIGHTER;
                    }
                }
            }
        }

        // Start ignition sequence
        ignitionActive = true;
        ignitionTimer  = 0f;
        return HeistResult.IGNITION_STARTED;
    }

    /**
     * Complete the engine heist — called once ignition finishes (after
     * {@link #ENGINE_IGNITION_SECONDS} seconds have elapsed since
     * {@link #attemptHeist} returned {@link HeistResult#IGNITION_STARTED}).
     *
     * <p>Marks the engine as stolen, seeds rumours, records the crime,
     * awards wanted stars and notoriety, and unlocks the achievement.
     *
     * @param player       the player
     * @param npcs         all active NPCs (WATCH_COMMANDER calls police)
     * @param achievementCallback optional notoriety system callback
     */
    public void completeHeist(Player player, List<NPC> npcs,
                              NotorietySystem.AchievementCallback achievementCallback) {
        engineStolen = true;
        bayDoorOpen  = false;

        // Record the crime
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.FIRE_ENGINE_STOLEN);
        }

        // Wanted stars
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(ENGINE_HEIST_WANTED_STARS,
                    player.getPosition().x,
                    player.getPosition().y,
                    player.getPosition().z, null);
        }

        // Notoriety
        if (notorietySystem != null && achievementCallback != null) {
            notorietySystem.addNotoriety(ENGINE_HEIST_NOTORIETY, achievementCallback);
        }

        // Seed MAJOR_THEFT rumour to up to 5 NPCs
        if (rumourNetwork != null && npcs != null) {
            int seeded = 0;
            for (NPC npc : npcs) {
                if (seeded >= 5) break;
                if (npc.getType() != NPCType.FIREFIGHTER
                        && npc.getType() != NPCType.WATCH_COMMANDER
                        && npc.getType() != NPCType.FIRE_ENGINE) {
                    rumourNetwork.addRumour(npc, new Rumour(RumourType.MAJOR_THEFT,
                            "Someone nicked the fire engine from Northfield station!"));
                    seeded++;
                }
            }
        }

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.GREAT_ENGINE_HEIST);
        }

        // WATCH_COMMANDER calls police (+1 extra POLICE NPC)
        if (npcs != null) {
            for (NPC npc : npcs) {
                if (npc.getType() == NPCType.WATCH_COMMANDER) {
                    npc.setState(NPCState.ALERT);
                    break;
                }
            }
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(1,
                    player.getPosition().x,
                    player.getPosition().y,
                    player.getPosition().z, null);
        }
    }

    /**
     * Returns true if the engine has been stolen by the player.
     */
    public boolean isEngineStolen() {
        return engineStolen;
    }

    /**
     * Returns true while the ignition sequence is active.
     */
    public boolean isIgnitionActive() {
        return ignitionActive;
    }

    /**
     * Returns elapsed ignition time in seconds.
     */
    public float getIgnitionTimer() {
        return ignitionTimer;
    }

    // ── 3. Crusher fence ─────────────────────────────────────────────────────

    /**
     * Called each second while the player is dwelling at the SCRAPYARD with the
     * stolen engine. When dwell reaches {@link #CRUSHER_DWELL_SECONDS} and the
     * current hour is within the crush window (20:00–07:00), the engine is crushed
     * for {@link #CRUSHER_PAYOUT_COIN} COIN and the crime record is cleared.
     *
     * @param player       the player (receives COIN payout)
     * @param currentHour  current in-game hour (0–24)
     * @return true if the crusher event completed this tick
     */
    public boolean onScrapyardDwellTick(Player player, float currentHour) {
        if (!engineStolen) return false;

        boolean inWindow = currentHour >= CRUSH_WINDOW_START || currentHour < CRUSH_WINDOW_END;
        if (!inWindow) {
            scrapyardDwellAccum = 0f;
            return false;
        }

        scrapyardDwellAccum += 1f;
        if (scrapyardDwellAccum >= CRUSHER_DWELL_SECONDS) {
            scrapyardDwellAccum = 0f;
            engineStolen        = false;

            // Pay out COIN
            if (player != null && player.getInventory() != null) {
                player.getInventory().addItem(Material.COIN, CRUSHER_PAYOUT_COIN);
            }

            // Clear the crime record
            if (criminalRecord != null) {
                criminalRecord.clearOne(CriminalRecord.CrimeType.FIRE_ENGINE_STOLEN);
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the accumulated scrapyard dwell seconds for testing.
     */
    public float getScrapyardDwellAccum() {
        return scrapyardDwellAccum;
    }

    // ── 4. Fire extinguisher interaction ─────────────────────────────────────

    /**
     * Player presses E on the HOSE_REEL_PROP to pick up a FIRE_EXTINGUISHER.
     *
     * <p>If a FIREFIGHTER is within {@value #FIREFIGHTER_DETECT_RADIUS} blocks,
     * there is a {@value #EXTINGUISHER_DETECT_CHANCE} chance of detection, which
     * adds {@link CriminalRecord.CrimeType#THEFT} and +1 wanted star.
     *
     * @param player    the player
     * @param npcs      active NPCs (checked for nearby FIREFIGHTERs)
     * @return pickup result
     */
    public ExtinguisherPickupResult attemptPickupExtinguisher(Player player, List<NPC> npcs) {
        if (!extinguisherAvailable) {
            return ExtinguisherPickupResult.NONE_AVAILABLE;
        }

        boolean detected = false;
        if (npcs != null) {
            for (NPC npc : npcs) {
                if (npc.getType() == NPCType.FIREFIGHTER) {
                    float dist = npc.getPosition().dst(player.getPosition());
                    if (dist <= FIREFIGHTER_DETECT_RADIUS) {
                        if (random.nextFloat() < EXTINGUISHER_DETECT_CHANCE) {
                            detected = true;
                        }
                        break;
                    }
                }
            }
        }

        if (detected) {
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.THEFT);
            }
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1,
                        player.getPosition().x,
                        player.getPosition().y,
                        player.getPosition().z, null);
            }
            return ExtinguisherPickupResult.DETECTED;
        }

        // Undetected — give extinguisher to player
        extinguisherAvailable = false;
        if (player.getInventory() != null) {
            player.getInventory().addItem(Material.FIRE_EXTINGUISHER, 1);
        }
        return ExtinguisherPickupResult.PICKED_UP;
    }

    /**
     * Player uses a FIRE_EXTINGUISHER near an active wheelie bin fire.
     * Extinguishes the fire immediately; grants −2 Notoriety.
     *
     * @param player              the player
     * @param firePos             the fire's world position
     * @param achievementCallback notoriety system callback
     * @return true if a fire was extinguished
     */
    public boolean useFireExtinguisher(Player player,
                                       com.badlogic.gdx.math.Vector3 firePos,
                                       NotorietySystem.AchievementCallback achievementCallback) {
        if (player.getInventory() == null
                || !player.getInventory().hasItem(Material.FIRE_EXTINGUISHER, 1)) {
            return false;
        }
        if (wheeliBinFireSystem == null) return false;

        boolean extinguished = wheeliBinFireSystem.extinguishWithFireExtinguisher(
                firePos, player.getInventory(), notorietySystem, achievementCallback, null, null);

        if (extinguished) {
            // −2 Notoriety for civic action
            if (notorietySystem != null && achievementCallback != null) {
                notorietySystem.reduceNotoriety(2, achievementCallback);
            }
        }
        return extinguished;
    }

    // ── 5. Firefighter disguise ───────────────────────────────────────────────

    /**
     * Called when the player equips a FIREFIGHTER_HELMET (e.g. from a locker).
     * Activates the FIREFIGHTER disguise via {@link DisguiseSystem}.
     *
     * @param inventory the player's inventory
     * @return true if the disguise was successfully applied
     */
    public boolean equipFirefighterDisguise(Inventory inventory) {
        if (disguiseSystem == null) return false;
        return disguiseSystem.equipDisguise(Material.FIREFIGHTER_HELMET, inventory);
    }

    /**
     * Returns true if the player is currently disguised as a FIREFIGHTER.
     */
    public boolean isDisguisedAsFirefighter() {
        if (disguiseSystem == null) return false;
        return disguiseSystem.isDisguised()
                && disguiseSystem.getActiveDisguise() == Material.FIREFIGHTER_HELMET;
    }

    // ── 6. Locker search ─────────────────────────────────────────────────────

    /**
     * Player presses E on a LOCKER_PROP.
     * 50 % chance of finding a FIREFIGHTER_HELMET.
     *
     * @param inventory the player's inventory
     * @return locker search result
     */
    public LockerResult searchLocker(Inventory inventory) {
        if (random.nextFloat() < LOCKER_HELMET_CHANCE) {
            inventory.addItem(Material.FIREFIGHTER_HELMET, 1);
            return LockerResult.FOUND_HELMET;
        }
        return LockerResult.EMPTY;
    }

    // ── 7. Antidepressants quest ──────────────────────────────────────────────

    /**
     * Player delivers ANTIDEPRESSANTS to the WATCH_COMMANDER.
     * Grants {@link #ANTIDEPRESSANTS_QUEST_COIN} COIN,
     * +{@link #ANTIDEPRESSANTS_VIBES_BONUS} neighbourhood vibes,
     * and seeds a {@link RumourType#LOCAL_EVENT} rumour.
     *
     * @param player  the player delivering the item
     * @param npcs    active NPCs (used to seed the LOCAL_EVENT rumour)
     * @return true if delivery was accepted (player had ANTIDEPRESSANTS and
     *         a WATCH_COMMANDER was present)
     */
    public boolean deliverAntidepressants(Player player, List<NPC> npcs) {
        if (player.getInventory() == null
                || !player.getInventory().hasItem(Material.ANTIDEPRESSANTS, 1)) {
            return false;
        }

        // Find the WATCH_COMMANDER
        NPC watchCommander = null;
        if (npcs != null) {
            for (NPC npc : npcs) {
                if (npc.getType() == NPCType.WATCH_COMMANDER) {
                    watchCommander = npc;
                    break;
                }
            }
        }
        if (watchCommander == null) return false;

        // Accept delivery
        player.getInventory().removeItem(Material.ANTIDEPRESSANTS, 1);
        player.getInventory().addItem(Material.COIN, ANTIDEPRESSANTS_QUEST_COIN);

        // Neighbourhood vibes boost
        if (neighbourhoodSystem != null) {
            int current = neighbourhoodSystem.getVibes();
            neighbourhoodSystem.setVibes(current + ANTIDEPRESSANTS_VIBES_BONUS);
        }

        // Seed LOCAL_EVENT rumour
        if (rumourNetwork != null) {
            rumourNetwork.addRumour(watchCommander, new Rumour(RumourType.LOCAL_EVENT,
                    "The fire station crew are in good spirits today."));
        }

        return true;
    }

    // ── 8. Siren toggle ──────────────────────────────────────────────────────

    /**
     * Toggle the siren on/off while driving the stolen engine.
     * When active, nearby civilian NPCs scatter and police cars are slowed
     * by {@link #SIREN_POLICE_SLOW_FACTOR}.
     */
    public void toggleSiren() {
        sirenActive = !sirenActive;
    }

    /**
     * Returns true if the siren is currently active.
     */
    public boolean isSirenActive() {
        return sirenActive;
    }

    // ── 9. WATCH_COMMANDER distraction ────────────────────────────────────────

    /**
     * Distract the WATCH_COMMANDER by pressing E on them while holding a
     * FIRE_EXTINGUISHER. They spend {@link #WATCH_COMMANDER_DISTRACT_S} seconds
     * resetting the extinguisher before returning to the watch room.
     *
     * @param player the player (must have FIRE_EXTINGUISHER in inventory)
     * @return true if the distraction was accepted
     */
    public boolean distractWatchCommander(Player player) {
        if (player.getInventory() == null
                || !player.getInventory().hasItem(Material.FIRE_EXTINGUISHER, 1)) {
            return false;
        }
        watchCommanderDistracted    = true;
        watchCommanderDistractTimer = WATCH_COMMANDER_DISTRACT_S;
        return true;
    }

    /**
     * Returns true if the WATCH_COMMANDER is currently distracted.
     */
    public boolean isWatchCommanderDistracted() {
        return watchCommanderDistracted;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Returns true if the fire station is open (always — 24/7).
     *
     * @param hour current in-game hour (0–24, ignored)
     */
    public boolean isOpen(float hour) {
        return true;
    }

    /**
     * Returns the expected FIREFIGHTER count for the given hour.
     *
     * @param hour current in-game hour (0–24)
     */
    public int expectedFirefighterCount(float hour) {
        boolean daytime = hour >= 8f && hour < 18f;
        return daytime ? FIREFIGHTER_DAYTIME_COUNT : FIREFIGHTER_NIGHTTIME_COUNT;
    }

    /**
     * Reset engine stolen flag (for new game / testing).
     */
    public void resetForNewGame() {
        engineStolen         = false;
        engineDeployed       = false;
        calloutTimer         = 0f;
        ignitionActive       = false;
        ignitionTimer        = 0f;
        bayDoorOpen          = false;
        loiterTimer          = 0f;
        extinguisherAvailable = true;
        scrapyardDwellAccum  = 0f;
        sirenActive          = false;
        watchCommanderDistracted    = false;
        watchCommanderDistractTimer = 0f;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void alertWatchCommander(List<NPC> npcs) {
        for (NPC npc : npcs) {
            if (npc.getType() == NPCType.WATCH_COMMANDER) {
                npc.setState(NPCState.ALERT);
                return;
            }
        }
    }
}

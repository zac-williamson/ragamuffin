package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Issue #1169 / Phase 39: The Status Dog — Staffy Companion, Intimidation &amp; Park Walks.
 *
 * <p>Manages the player's Staffordshire Bull Terrier companion. The dog can be adopted
 * from a {@code STRAY_DOG} NPC in the park by pressing E while holding a
 * {@code SAUSAGE_ROLL} or {@code STEAK_BAKE}. Once adopted the dog follows the player,
 * must be fed, can learn tricks, and can be used to intimidate NPCs.
 *
 * <h3>Bond Level (0–100)</h3>
 * Increases through park walks (+{@link #PARK_WALK_BOND_PER_MINUTE}/min),
 * feeding (+{@link #FEED_BOND_BONUS}), and teaching tricks (+{@link #TRICK_BOND_BONUS}).
 *
 * <h3>Hunger (0–100)</h3>
 * Drains at {@link #DOG_HUNGER_DRAIN_PER_MINUTE} per in-game minute.
 * Doubled in {@code FROST} or {@code COLD_SNAP} weather.
 * When hunger falls below {@link #WHIMPER_HUNGER_THRESHOLD} the dog stops following
 * the player until it is fed.
 *
 * <h3>Tricks</h3>
 * Four tricks gated by bond level:
 * SIT ≥ {@link #BOND_TRICK_UNLOCK_SIT}, STAY ≥ {@link #BOND_TRICK_UNLOCK_STAY},
 * FETCH ≥ {@link #BOND_TRICK_UNLOCK_FETCH}, GUARD ≥ {@link #BOND_TRICK_UNLOCK_GUARD}.
 * Teaching all four unlocks the {@code GOOD_BOY_GOOD_BOY} achievement.
 *
 * <h3>Intimidation</h3>
 * Requires {@link #isOffLead} and bond ≥ 30. Adds Notoriety, Watch anger, and may
 * record a {@code DANGEROUS_DOG} offence. The fifth use triggers the
 * {@code DANGEROUS_DOG} achievement.
 *
 * <h3>Park Walk</h3>
 * While the current landmark is {@code PARK} and the dog is following, bond accrues
 * and Watch anger decays. The first park walk seeds a positive rumour.
 *
 * <h3>WarmthSystem</h3>
 * When bond ≥ 50 and the player has been stationary for ≥ 5 seconds,
 * the dog provides +5 warmth/minute (+{@link #WARMTH_BONUS_PER_MINUTE}).
 */
public class DogCompanionSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Hunger restored by feeding once. */
    public static final float FEED_HUNGER_RESTORE = 30f;

    /** Bond gained by feeding. */
    public static final int FEED_BOND_BONUS = 5;

    /** Bond points gained per in-game minute during a park walk. */
    public static final int PARK_WALK_BOND_PER_MINUTE = 3;

    /** Bond gained each time a trick is successfully taught. */
    public static final int TRICK_BOND_BONUS = 2;

    /** Notoriety added per intimidation use. */
    public static final int INTIMIDATION_NOTORIETY_GAIN = 3;

    /** Watch anger added per intimidation use. */
    public static final int INTIMIDATION_WATCH_ANGER_GAIN = 8;

    /** Watch anger reduced per in-game minute of park walking. */
    public static final int PARK_WALK_WATCH_ANGER_REDUCTION = 5;

    /** Range within which a police NPC causes extra suspicion when dog is off-lead. */
    public static final float POLICE_SUSPICION_RANGE = 10f;

    /** Dog hunger drain per in-game minute (base rate). */
    public static final float DOG_HUNGER_DRAIN_PER_MINUTE = 1f;

    /** Hunger threshold below which the dog stops following the player. */
    public static final float WHIMPER_HUNGER_THRESHOLD = 15f;

    /** Minimum bond level to unlock the SIT trick. */
    public static final int BOND_TRICK_UNLOCK_SIT = 10;

    /** Minimum bond level to unlock the STAY trick. */
    public static final int BOND_TRICK_UNLOCK_STAY = 25;

    /** Minimum bond level to unlock the FETCH trick. */
    public static final int BOND_TRICK_UNLOCK_FETCH = 40;

    /** Minimum bond level to unlock the GUARD trick. */
    public static final int BOND_TRICK_UNLOCK_GUARD = 60;

    /** Maximum range (blocks) at which the dog stays near the player when off-lead. */
    public static final float OFF_LEAD_RANGE = 12f;

    /** Warmth restored per in-game minute when bond ≥ 50 and player is stationary. */
    public static final float WARMTH_BONUS_PER_MINUTE = 5f;

    /** Stationary time (seconds) before warmth bonus activates. */
    public static final float WARMTH_STATIONARY_THRESHOLD = 5f;

    /** Minimum bond required to use the dog for intimidation. */
    private static final int INTIMIDATION_MIN_BOND = 30;

    /** Number of intimidations before the DANGEROUS_DOG achievement is awarded. */
    private static final int DANGEROUS_DOG_ACHIEVEMENT_THRESHOLD = 5;

    /** Hunger drain multiplier in FROST or COLD_SNAP weather. */
    private static final float COLD_WEATHER_HUNGER_MULTIPLIER = 2f;

    /** Street Lads respect bonus when intimidating in their territory. */
    private static final int STREET_LADS_INTIMIDATION_RESPECT = 5;

    /** Council respect penalty when intimidating in their territory. */
    private static final int COUNCIL_INTIMIDATION_RESPECT_PENALTY = -10;

    /** In-game minutes per real second at default time speed (1s real = 6 in-game minutes). */
    private static final float IN_GAME_MINUTES_PER_SECOND = 6f;

    // ── State ─────────────────────────────────────────────────────────────────

    private boolean hasDog = false;
    private float dogHunger = 100f;
    private int dogBondLevel = 0;
    private final Set<DogTrick> tricksLearned = EnumSet.noneOf(DogTrick.class);
    private boolean isOffLead = false;

    /** Number of times the dog has been used for intimidation. */
    private int intimidationCount = 0;

    /** Whether the GOOD_BOY_GOOD_BOY achievement has been awarded. */
    private boolean goodBoyAchieved = false;

    /** Whether the DANGEROUS_DOG achievement has been awarded. */
    private boolean dangerousDogAchieved = false;

    /** Whether the first park walk rumour has been seeded. */
    private boolean firstParkWalkRumourSeeded = false;

    /**
     * Accumulated fractional in-game minutes spent in the park this walk,
     * used to award bond and anger reduction at whole-minute intervals.
     */
    private float parkWalkMinuteAccumulator = 0f;

    /** Accumulated stationary time for warmth bonus. */
    private float stationaryTimer = 0f;

    /** Player position from previous frame (for stationary detection). */
    private float lastPlayerX = Float.NaN;
    private float lastPlayerZ = Float.NaN;

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final NotorietySystem notorietySystem;
    private final NeighbourhoodWatchSystem watchSystem;
    private final FactionSystem factionSystem;
    private final WantedSystem wantedSystem;
    private final WarmthSystem warmthSystem;
    private final RumourNetwork rumourNetwork;
    private final CriminalRecord criminalRecord;
    private final Random random;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Full constructor.
     *
     * @param notorietySystem  for adding notoriety on intimidation
     * @param watchSystem      for anger changes on park walk / intimidation
     * @param factionSystem    for territory-based respect changes on intimidation
     * @param wantedSystem     for recording DANGEROUS_DOG offences on intimidation
     * @param warmthSystem     (stored but warmth is applied directly to the Player)
     * @param rumourNetwork    for seeding park-walk and intimidation rumours
     * @param criminalRecord   for recording DANGEROUS_DOG offences
     * @param random           random instance for probabilistic elements
     */
    public DogCompanionSystem(NotorietySystem notorietySystem,
                               NeighbourhoodWatchSystem watchSystem,
                               FactionSystem factionSystem,
                               WantedSystem wantedSystem,
                               WarmthSystem warmthSystem,
                               RumourNetwork rumourNetwork,
                               CriminalRecord criminalRecord,
                               Random random) {
        this.notorietySystem = notorietySystem;
        this.watchSystem     = watchSystem;
        this.factionSystem   = factionSystem;
        this.wantedSystem    = wantedSystem;
        this.warmthSystem    = warmthSystem;
        this.rumourNetwork   = rumourNetwork;
        this.criminalRecord  = criminalRecord;
        this.random          = random;
    }

    /** Convenience constructor for tests (no warmth/criminal record integration). */
    public DogCompanionSystem(NotorietySystem notorietySystem,
                               NeighbourhoodWatchSystem watchSystem,
                               FactionSystem factionSystem,
                               WantedSystem wantedSystem,
                               Random random) {
        this(notorietySystem, watchSystem, factionSystem, wantedSystem,
             new WarmthSystem(), new RumourNetwork(random), new CriminalRecord(), random);
    }

    /** Minimal no-arg constructor for simple tests. */
    public DogCompanionSystem() {
        this(new NotorietySystem(), new NeighbourhoodWatchSystem(),
             new FactionSystem(), new WantedSystem(), new Random());
    }

    // ── Adoption ──────────────────────────────────────────────────────────────

    /**
     * Attempt to adopt a stray dog. Consumes one {@code SAUSAGE_ROLL} or
     * {@code STEAK_BAKE} from the inventory. Sets the NPC state to
     * {@code FOLLOWING_PLAYER} and awards {@code MANS_BEST_FRIEND}.
     *
     * @param strayDog  the {@code STRAY_DOG} NPC to adopt
     * @param inventory player's inventory
     * @param callback  achievement callback (may be null)
     * @return true if adoption succeeded
     */
    public boolean adoptDog(NPC strayDog, Inventory inventory,
                            NotorietySystem.AchievementCallback callback) {
        if (hasDog) return false;
        if (strayDog == null || !strayDog.isAlive()) return false;

        boolean hasSausageRoll = inventory.hasItem(Material.SAUSAGE_ROLL);
        boolean hasSteakBake   = inventory.hasItem(Material.STEAK_BAKE);
        if (!hasSausageRoll && !hasSteakBake) return false;

        // Consume the food
        if (hasSausageRoll) {
            inventory.removeItem(Material.SAUSAGE_ROLL, 1);
        } else {
            inventory.removeItem(Material.STEAK_BAKE, 1);
        }

        hasDog = true;
        dogHunger = 100f;
        dogBondLevel = 0;
        tricksLearned.clear();
        isOffLead = false;
        intimidationCount = 0;

        strayDog.setState(NPCState.FOLLOWING_PLAYER);

        if (callback != null) {
            callback.award(AchievementType.MANS_BEST_FRIEND);
        }
        return true;
    }

    // ── Feeding ───────────────────────────────────────────────────────────────

    /**
     * Feed the dog using a food item from the player's inventory.
     * Accepts {@code SAUSAGE_ROLL} or {@code STEAK_BAKE}.
     * Restores {@link #FEED_HUNGER_RESTORE} hunger and adds {@link #FEED_BOND_BONUS} bond.
     *
     * @param food      the food material to feed (must be SAUSAGE_ROLL or STEAK_BAKE)
     * @param inventory player's inventory (item is consumed)
     * @return true if the dog was fed
     */
    public boolean feedDog(Material food, Inventory inventory) {
        if (!hasDog) return false;
        if (food != Material.SAUSAGE_ROLL && food != Material.STEAK_BAKE) return false;
        if (!inventory.hasItem(food)) return false;

        inventory.removeItem(food, 1);
        dogHunger = Math.min(100f, dogHunger + FEED_HUNGER_RESTORE);
        addBond(FEED_BOND_BONUS);
        return true;
    }

    // ── Trick training ────────────────────────────────────────────────────────

    /**
     * Teach the dog a trick if the required bond threshold is met and the trick
     * has not already been learned.
     *
     * <p>On success, adds {@link #TRICK_BOND_BONUS} to bond. Once all four tricks
     * have been learned, awards {@code GOOD_BOY_GOOD_BOY}.
     *
     * @param trick    the trick to teach
     * @param callback achievement callback (may be null)
     * @return true if the trick was newly learned
     */
    public boolean teachTrick(DogTrick trick, NotorietySystem.AchievementCallback callback) {
        if (!hasDog) return false;
        if (tricksLearned.contains(trick)) return false;

        int required = bondThresholdFor(trick);
        if (dogBondLevel < required) return false;

        tricksLearned.add(trick);
        addBond(TRICK_BOND_BONUS);

        if (!goodBoyAchieved && tricksLearned.size() == DogTrick.values().length) {
            goodBoyAchieved = true;
            if (callback != null) {
                callback.award(AchievementType.GOOD_BOY_GOOD_BOY);
            }
        }
        return true;
    }

    // ── Intimidation ──────────────────────────────────────────────────────────

    /**
     * Use the dog to intimidate a nearby NPC.
     *
     * <p>Requirements: {@link #hasDog}, {@link #isOffLead}, bond ≥ {@link #INTIMIDATION_MIN_BOND}.
     *
     * <p>Effects:
     * <ul>
     *   <li>Notoriety +{@link #INTIMIDATION_NOTORIETY_GAIN}</li>
     *   <li>Watch anger +{@link #INTIMIDATION_WATCH_ANGER_GAIN}</li>
     *   <li>Target NPC → {@code FLEEING} state</li>
     *   <li>FactionSystem: +5 Street Lads respect in their territory,
     *       −10 Council respect in their territory</li>
     *   <li>Criminal record: {@code DANGEROUS_DOG} offence recorded</li>
     *   <li>Rumour: {@code PLAYER_SPOTTED} seeded to the target NPC</li>
     *   <li>On 5th use: {@code DANGEROUS_DOG} achievement</li>
     * </ul>
     *
     * @param target     the NPC to intimidate
     * @param currentLandmarkType the zone the player is currently in (for faction territory)
     * @param allNpcs    all NPCs (for faction interaction context)
     * @param callback   achievement callback (may be null)
     * @return true if intimidation was performed
     */
    public boolean useForIntimidation(NPC target, LandmarkType currentLandmarkType,
                                       List<NPC> allNpcs,
                                       NotorietySystem.AchievementCallback callback) {
        if (!hasDog) return false;
        if (!isOffLead) return false;
        if (dogBondLevel < INTIMIDATION_MIN_BOND) return false;
        if (target == null || !target.isAlive()) return false;

        // Put target to flight
        target.setState(NPCState.FLEEING);

        // Notoriety
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(INTIMIDATION_NOTORIETY_GAIN, callback);
        }

        // Watch anger
        if (watchSystem != null) {
            watchSystem.addAnger(INTIMIDATION_WATCH_ANGER_GAIN);
        }

        // Faction territory effects
        if (factionSystem != null) {
            if (currentLandmarkType == LandmarkType.PARK) {
                // Park is Street Lads territory
                factionSystem.applyRespectDelta(Faction.STREET_LADS,
                        STREET_LADS_INTIMIDATION_RESPECT);
            } else if (currentLandmarkType == LandmarkType.OFFICE_BUILDING
                    || currentLandmarkType == LandmarkType.JOB_CENTRE) {
                // Council territory
                factionSystem.applyRespectDelta(Faction.THE_COUNCIL,
                        COUNCIL_INTIMIDATION_RESPECT_PENALTY);
            }
        }

        // Criminal record
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.DANGEROUS_DOG);
        }

        // Seed intimidation rumour to the target NPC
        if (rumourNetwork != null) {
            rumourNetwork.addRumour(target,
                    new Rumour(RumourType.PLAYER_SPOTTED,
                            "Someone's dog went for a bloke round here — proper scary"));
        }

        intimidationCount++;

        // Award achievement on 5th use
        if (!dangerousDogAchieved && intimidationCount >= DANGEROUS_DOG_ACHIEVEMENT_THRESHOLD) {
            dangerousDogAchieved = true;
            if (callback != null) {
                callback.award(AchievementType.DANGEROUS_DOG);
            }
        }

        return true;
    }

    /** Overload without faction-territory context (uses null landmark). */
    public boolean useForIntimidation(NPC target, NotorietySystem.AchievementCallback callback) {
        return useForIntimidation(target, null, null, callback);
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Call once per frame to advance the dog companion simulation.
     *
     * <p>Responsibilities:
     * <ol>
     *   <li>Drain hunger (doubled in FROST/COLD_SNAP)</li>
     *   <li>If in PARK and dog is following: accrue park-walk bond + Watch anger reduction</li>
     *   <li>Seed first park-walk rumour when applicable</li>
     *   <li>Apply warmth bonus to player when bond ≥ 50 and stationary ≥ 5s</li>
     * </ol>
     *
     * @param delta              seconds since last frame
     * @param timeSystem         for converting real-time delta to in-game minutes
     * @param player             the player (for warmth and stationary detection)
     * @param currentLandmark    the landmark zone the player is currently in (may be null)
     * @param weather            current weather
     * @param nearestNpc         an NPC to receive the park-walk rumour (may be null)
     * @param callback           achievement callback (may be null)
     */
    public void update(float delta, TimeSystem timeSystem, Player player,
                       LandmarkType currentLandmark, Weather weather,
                       NPC nearestNpc, NotorietySystem.AchievementCallback callback) {
        if (!hasDog) return;

        // Convert real-time delta to in-game minutes
        float inGameMinutes = delta * IN_GAME_MINUTES_PER_SECOND;

        // ── Hunger drain ──────────────────────────────────────────────────────
        float drainRate = DOG_HUNGER_DRAIN_PER_MINUTE;
        if (weather == Weather.FROST || weather == Weather.COLD_SNAP) {
            drainRate *= COLD_WEATHER_HUNGER_MULTIPLIER;
        }
        dogHunger = Math.max(0f, dogHunger - drainRate * inGameMinutes);

        // ── Park walk ────────────────────────────────────────────────────────
        boolean isFollowingNow = isFollowing();
        if (currentLandmark == LandmarkType.PARK && isFollowingNow) {
            parkWalkMinuteAccumulator += inGameMinutes;

            // Seed first park-walk rumour
            if (!firstParkWalkRumourSeeded) {
                firstParkWalkRumourSeeded = true;
                if (rumourNetwork != null && nearestNpc != null) {
                    rumourNetwork.addRumour(nearestNpc,
                            new Rumour(RumourType.COMMUNITY_WIN,
                                    "Someone's got a gorgeous dog down the park — proper friendly Staffy"));
                }
            }

            // Award bond + anger reduction at whole-minute intervals
            while (parkWalkMinuteAccumulator >= 1f) {
                parkWalkMinuteAccumulator -= 1f;
                addBond(PARK_WALK_BOND_PER_MINUTE);
                if (watchSystem != null) {
                    watchSystem.addAnger(-PARK_WALK_WATCH_ANGER_REDUCTION);
                }
            }
        } else {
            // Reset accumulator when not in park
            parkWalkMinuteAccumulator = 0f;
        }

        // ── Warmth bonus ─────────────────────────────────────────────────────
        if (player != null && dogBondLevel >= 50) {
            float px = player.getPosition().x;
            float pz = player.getPosition().z;
            boolean stationary = (!Float.isNaN(lastPlayerX))
                    && Math.abs(px - lastPlayerX) < 0.01f
                    && Math.abs(pz - lastPlayerZ) < 0.01f;

            if (stationary) {
                stationaryTimer += delta;
                if (stationaryTimer >= WARMTH_STATIONARY_THRESHOLD) {
                    // +5 warmth/minute → per-frame amount
                    float warmthPerSecond = WARMTH_BONUS_PER_MINUTE / 60f;
                    player.restoreWarmth(warmthPerSecond * delta);
                }
            } else {
                stationaryTimer = 0f;
            }

            lastPlayerX = px;
            lastPlayerZ = pz;
        } else {
            stationaryTimer = 0f;
        }
    }

    /**
     * Simplified update overload for tests that do not have a Player reference.
     */
    public void update(float delta, TimeSystem timeSystem,
                       LandmarkType currentLandmark, Weather weather,
                       NPC nearestNpc, NotorietySystem.AchievementCallback callback) {
        update(delta, timeSystem, null, currentLandmark, weather, nearestNpc, callback);
    }

    // ── Off-lead toggle ───────────────────────────────────────────────────────

    /**
     * Put the dog on or off lead.
     *
     * @param offLead true to release the dog off lead; false to put on lead
     */
    public void setOffLead(boolean offLead) {
        this.isOffLead = offLead;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** @return true if the player has adopted a dog. */
    public boolean hasDog() {
        return hasDog;
    }

    /**
     * @return true if the dog is currently following the player
     *         (has dog AND hunger is above whimper threshold).
     */
    public boolean isFollowing() {
        return hasDog && dogHunger >= WHIMPER_HUNGER_THRESHOLD;
    }

    /** @return current hunger level (0–100). */
    public float getDogHunger() {
        return dogHunger;
    }

    /** @return current bond level (0–100). */
    public int getDogBondLevel() {
        return dogBondLevel;
    }

    /** @return true if the dog is currently off lead. */
    public boolean isOffLead() {
        return isOffLead;
    }

    /** @return an unmodifiable view of the tricks the dog has learned. */
    public Set<DogTrick> getTricksLearned() {
        return java.util.Collections.unmodifiableSet(tricksLearned);
    }

    /** @return true if the given trick has been learned. */
    public boolean knowsTrick(DogTrick trick) {
        return tricksLearned.contains(trick);
    }

    /** @return number of times the dog has been used for intimidation. */
    public int getIntimidationCount() {
        return intimidationCount;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void addBond(int amount) {
        dogBondLevel = Math.min(100, dogBondLevel + amount);
    }

    private int bondThresholdFor(DogTrick trick) {
        switch (trick) {
            case SIT:   return BOND_TRICK_UNLOCK_SIT;
            case STAY:  return BOND_TRICK_UNLOCK_STAY;
            case FETCH: return BOND_TRICK_UNLOCK_FETCH;
            case GUARD: return BOND_TRICK_UNLOCK_GUARD;
            default:    return 100;
        }
    }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /** Force-adopt a dog for testing (skips inventory check). */
    public void adoptDogForTesting() {
        hasDog = true;
        dogHunger = 100f;
        dogBondLevel = 0;
        isOffLead = false;
    }

    /** Force-set the dog bond level for testing. */
    public void setDogBondForTesting(int bond) {
        dogBondLevel = Math.max(0, Math.min(100, bond));
    }

    /** Force-set the off-lead status for testing. */
    public void setOffLeadForTesting(boolean offLead) {
        isOffLead = offLead;
    }
}

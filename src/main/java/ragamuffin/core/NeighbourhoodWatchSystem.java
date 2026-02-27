package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Issue #797: The Neighbourhood Watch — Vigilante Mob Justice &amp; Community Uprising.
 *
 * <p>Tracks a persistent {@code WatchAnger} score (0–100). Visible crimes,
 * exterior wall smashing, and punching civilians cause ordinary residents to
 * escalate from passive muttering all the way to a full neighbourhood uprising.
 *
 * <h3>Escalation Tiers</h3>
 * <ol>
 *   <li>Tier 1 — Mutterings (Anger 1–24): NPCs stare and mutter disapprovingly.</li>
 *   <li>Tier 2 — Petitions (Anger 25–49): A {@code PETITION_BOARD} prop spawns;
 *       1–2 WATCH_MEMBER NPCs patrol and follow the player.</li>
 *   <li>Tier 3 — Vigilante Patrol (Anger 50–74): 3–5 Watch Members perform soft
 *       citizen's arrests (immobilise + speech + 2 offence records).</li>
 *   <li>Tier 4 — Organised Mob (Anger 75–94): 6–10 Watch Members coordinate,
 *       equip clipboards, and brawl with gang NPCs.</li>
 *   <li>Tier 5 — Full Uprising (Anger 95–100): every PUBLIC NPC converts to a
 *       tabard-wearing vigilante; the pub locks; police are proactively called;
 *       a newspaper front-page is generated. After 5 minutes the Anger resets
 *       to 60.</li>
 * </ol>
 *
 * <h3>New Mechanic: Grovel</h3>
 * Player holds G for 2 seconds (tracked externally via {@link #grovelling(float)}).
 * On completion, reduces Watch Member engagement and decreases Anger by −5.
 *
 * <h3>Crafting Items</h3>
 * <ul>
 *   <li>{@code NEIGHBOURHOOD_NEWSLETTER} (2 NEWSPAPER + 1 COIN) — removes a
 *       PETITION_BOARD and reduces Anger by −8.</li>
 *   <li>{@code PEACE_OFFERING} (1 SAUSAGE_ROLL + 1 COIN) — converts a Watch
 *       Member to neutral patrol mode; Anger −5.</li>
 * </ul>
 *
 * <h3>Faction Hooks</h3>
 * <ul>
 *   <li>The Council secretly funds the Watch when turf &gt; 40 % (doubles spawn
 *       rate, gives clipboards that record offences).</li>
 *   <li>Street Lads mock Watch Members on their turf (−3 Anger via banter).</li>
 *   <li>Marchetti Crew exploit the Uprising commotion for a free mission reset.</li>
 * </ul>
 *
 * <h3>Anger Decay</h3>
 * Anger decays in rain/fog (weather doubles decay rate), from pub rounds, and
 * on Council mission completion.
 */
public class NeighbourhoodWatchSystem {

    // ── Anger tier thresholds ─────────────────────────────────────────────────

    public static final int TIER_1_MUTTERINGS     = 1;
    public static final int TIER_2_PETITIONS      = 25;
    public static final int TIER_3_VIGILANTE      = 50;
    public static final int TIER_4_ORGANISED_MOB  = 75;
    public static final int TIER_5_UPRISING       = 95;

    public static final int MAX_ANGER = 100;
    public static final int MIN_ANGER = 0;

    // ── Anger gain values ─────────────────────────────────────────────────────

    /** Punching a PUBLIC or passive NPC in view of other NPCs. */
    public static final int ANGER_PUNCH_CIVILIAN     = 10;

    /** Smashing an exterior wall block (BRICK/STONE/GLASS) of a building. */
    public static final int ANGER_SMASH_EXTERIOR     = 5;

    /** Committing a visible crime (theft, block break in a public area). */
    public static final int ANGER_VISIBLE_CRIME      = 3;

    /** Punching a WATCH_MEMBER NPC. */
    public static final int ANGER_PUNCH_WATCH_MEMBER = 15;

    // ── Anger decay values ────────────────────────────────────────────────────

    /** Passive Anger decay per real second (standard). */
    public static final float ANGER_DECAY_PER_SECOND          = 0.5f;

    /** Multiplier applied during rain or fog weather. */
    public static final float ANGER_DECAY_WEATHER_MULTIPLIER  = 2.0f;

    /** Decay granted on completing a Council faction mission. */
    public static final int ANGER_DECAY_COUNCIL_MISSION       = 8;

    /** Decay granted by buying a round at the pub. */
    public static final int ANGER_DECAY_PUB_ROUND             = 3;

    // ── Grovel mechanic ───────────────────────────────────────────────────────

    /** Seconds the player must hold G to complete a grovel. */
    public static final float GROVEL_HOLD_DURATION = 2.0f;

    /** Anger reduction on successful grovel. */
    public static final int ANGER_GROVEL_REDUCTION = 5;

    // ── Newsletter / Peace Offering mechanics ─────────────────────────────────

    /** Anger reduction from using a NEIGHBOURHOOD_NEWSLETTER on a PETITION_BOARD. */
    public static final int ANGER_NEWSLETTER_REDUCTION = 8;

    /** Anger reduction from using a PEACE_OFFERING on a WATCH_MEMBER. */
    public static final int ANGER_PEACE_OFFERING_REDUCTION = 5;

    // ── Tier 5 Uprising reset ──────────────────────────────────────────────────

    /** Duration (seconds) of the Full Uprising before Anger resets. */
    public static final float UPRISING_DURATION_SECONDS = 300f; // 5 in-game minutes

    /** Anger level after Full Uprising auto-reset. */
    public static final int ANGER_AFTER_UPRISING_RESET = 60;

    // ── Spawn count ranges per tier ────────────────────────────────────────────

    public static final int TIER_2_SPAWN_MIN = 1;
    public static final int TIER_2_SPAWN_MAX = 2;
    public static final int TIER_3_SPAWN_MIN = 3;
    public static final int TIER_3_SPAWN_MAX = 5;
    public static final int TIER_4_SPAWN_MIN = 6;
    public static final int TIER_4_SPAWN_MAX = 10;

    // ── Citizen's arrest parameters ────────────────────────────────────────────

    /** Number of offence records added per soft citizen's arrest (Tier 3). */
    public static final int CITIZENS_ARREST_OFFENCES = 2;

    // ── Faction hooks ──────────────────────────────────────────────────────────

    /** Council turf fraction above which the Watch spawn rate doubles. */
    public static final float COUNCIL_FUNDING_TURF_THRESHOLD = 0.40f;

    /** Anger reduction when Street Lads mock Watch Members on their turf. */
    public static final int ANGER_STREET_LADS_BANTER = 3;

    // ── State ─────────────────────────────────────────────────────────────────

    private int watchAnger = 0;
    private int currentTier = 0;

    /** Active Watch Member NPCs spawned by this system. */
    private final List<NPC> watchMembers = new ArrayList<>();

    /** Whether a Petition Board has been spawned at Tier 2. */
    private boolean petitionBoardActive = false;

    /** Whether the Full Uprising (Tier 5) is currently active. */
    private boolean uprisingActive = false;

    /** Countdown timer for the Full Uprising auto-reset (seconds). */
    private float uprisingTimer = 0f;

    /** Whether a first-tier achievement has been awarded. */
    private boolean watchedAchieved = false;

    /** Whether the petition achievement has been awarded. */
    private boolean petitionAchieved = false;

    /** Whether the uprising-survived achievement has been awarded. */
    private boolean uprisingSurvivedAchieved = false;

    /** Grovel progress timer (seconds held). Reset on release before completion. */
    private float groveltimer = 0f;

    /** Whether the player is currently grovelling (G held). */
    private boolean isGrovelling = false;

    /** Grovel achievement awarded flag. */
    private boolean grovellAchieved = false;

    private final Random random;

    // ── Construction ──────────────────────────────────────────────────────────

    public NeighbourhoodWatchSystem() {
        this(new Random());
    }

    public NeighbourhoodWatchSystem(Random random) {
        this.random = random;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update the system each frame.
     *
     * @param delta           seconds since last frame
     * @param weatherRaining  true if current weather is RAIN or DRIZZLE or THUNDERSTORM
     * @param weatherFoggy    true if current weather is FOG
     * @param callback        achievement callback (may be null)
     */
    public void update(float delta, boolean weatherRaining, boolean weatherFoggy,
                       NotorietySystem.AchievementCallback callback) {

        // Grovel decay clears if not currently grovelling
        if (!isGrovelling) {
            groveltimer = 0f;
        }
        isGrovelling = false; // reset; caller must call grovelling() every frame to maintain

        // Anger passive decay
        float decayRate = ANGER_DECAY_PER_SECOND;
        if (weatherRaining || weatherFoggy) {
            decayRate *= ANGER_DECAY_WEATHER_MULTIPLIER;
        }
        decreaseAnger(decayRate * delta);

        // Tier 5 uprising timer
        if (uprisingActive) {
            uprisingTimer -= delta;
            if (uprisingTimer <= 0f) {
                endUprising(callback);
            }
        }

        // Recompute tier
        recalculateTier(callback);
    }

    // ── Grovel mechanic ────────────────────────────────────────────────────────

    /**
     * Call every frame while the player is holding G.
     * Returns {@code true} when the grovel completes (full 2-second hold).
     *
     * @param delta   seconds since last frame
     * @param callback achievement callback (may be null)
     * @return true if grovel just completed this frame
     */
    public boolean grovelling(float delta, NotorietySystem.AchievementCallback callback) {
        isGrovelling = true;
        groveltimer += delta;
        if (groveltimer >= GROVEL_HOLD_DURATION) {
            groveltimer = 0f;
            isGrovelling = false;
            // Reduce anger and dismiss nearby Watch Members (set to WANDERING)
            addAnger(-ANGER_GROVEL_REDUCTION);
            for (NPC wm : watchMembers) {
                if (wm.getState() == NPCState.CHASING_PLAYER || wm.getState() == NPCState.PATROLLING) {
                    wm.setState(NPCState.WANDERING);
                }
            }
            if (!grovellAchieved && callback != null) {
                grovellAchieved = true;
                callback.award(AchievementType.GROVELLED);
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the current grovel progress as a value between 0.0 and 1.0.
     */
    public float getGroveltProgress() {
        return Math.min(1.0f, groveltimer / GROVEL_HOLD_DURATION);
    }

    // ── Anger mutations ────────────────────────────────────────────────────────

    /**
     * Add (or subtract) anger. Clamped to [0, 100].
     * Positive values increase anger; negative values reduce it.
     */
    public void addAnger(int delta) {
        watchAnger = Math.max(MIN_ANGER, Math.min(MAX_ANGER, watchAnger + delta));
    }

    /**
     * Decrease anger by a fractional amount (used for per-second decay).
     * Fractional accumulation is handled internally.
     */
    private float angerDecayRemainder = 0f;

    private void decreaseAnger(float amount) {
        angerDecayRemainder += amount;
        int wholeDecay = (int) angerDecayRemainder;
        if (wholeDecay > 0) {
            angerDecayRemainder -= wholeDecay;
            watchAnger = Math.max(MIN_ANGER, watchAnger - wholeDecay);
        }
    }

    // ── Trigger methods (called by other systems) ─────────────────────────────

    /**
     * Call when the player punches a PUBLIC or passive NPC visibly.
     */
    public void onPlayerPunchedCivilian() {
        addAnger(ANGER_PUNCH_CIVILIAN);
    }

    /**
     * Call when the player smashes an exterior wall block of a building.
     */
    public void onPlayerSmashedExteriorWall() {
        addAnger(ANGER_SMASH_EXTERIOR);
    }

    /**
     * Call when the player commits a visible crime (theft, vandalism, etc.)
     * in a public area.
     */
    public void onVisibleCrime() {
        addAnger(ANGER_VISIBLE_CRIME);
    }

    /**
     * Call when the player punches a WATCH_MEMBER NPC.
     */
    public void onPlayerPunchedWatchMember() {
        addAnger(ANGER_PUNCH_WATCH_MEMBER);
    }

    /**
     * Call when the player buys a round at the pub.
     */
    public void onPubRound() {
        addAnger(-ANGER_DECAY_PUB_ROUND);
    }

    /**
     * Call when the player completes a Council faction mission.
     */
    public void onCouncilMissionComplete() {
        addAnger(-ANGER_DECAY_COUNCIL_MISSION);
    }

    /**
     * Call when Street Lads mock Watch Members on their turf (reduces anger by banter).
     */
    public void onStreetLadsBanter() {
        addAnger(-ANGER_STREET_LADS_BANTER);
    }

    /**
     * Call when the Marchetti Crew exploit the Uprising for a free mission reset.
     * Returns {@code true} if the uprising is currently active (and the crew benefit applies).
     */
    public boolean onMarchettiMissionReset() {
        return uprisingActive;
    }

    // ── Crafting item interactions ─────────────────────────────────────────────

    /**
     * Use a NEIGHBOURHOOD_NEWSLETTER to remove the active Petition Board.
     * Consumes one NEIGHBOURHOOD_NEWSLETTER from inventory.
     * Returns {@code true} if successful.
     *
     * @param inventory  player inventory
     * @param callback   achievement callback (may be null)
     */
    public boolean useNeighbourhoodNewsletter(Inventory inventory,
                                               NotorietySystem.AchievementCallback callback) {
        if (!petitionBoardActive) {
            return false;
        }
        if (inventory.getItemCount(Material.NEIGHBOURHOOD_NEWSLETTER) < 1) {
            return false;
        }
        inventory.removeItem(Material.NEIGHBOURHOOD_NEWSLETTER, 1);
        petitionBoardActive = false;
        addAnger(-ANGER_NEWSLETTER_REDUCTION);
        if (callback != null) {
            callback.award(AchievementType.NEWSLETTER_PUBLISHED);
        }
        return true;
    }

    /**
     * Use a PEACE_OFFERING on a Watch Member NPC.
     * Consumes one PEACE_OFFERING from inventory. Converts the NPC to PATROLLING.
     * Returns {@code true} if successful.
     *
     * @param watchMember the WATCH_MEMBER NPC being offered to
     * @param inventory   player inventory
     * @param callback    achievement callback (may be null)
     */
    public boolean usePeaceOffering(NPC watchMember, Inventory inventory,
                                     NotorietySystem.AchievementCallback callback) {
        if (watchMember == null || watchMember.getType() != NPCType.WATCH_MEMBER) {
            return false;
        }
        if (inventory.getItemCount(Material.PEACE_OFFERING) < 1) {
            return false;
        }
        inventory.removeItem(Material.PEACE_OFFERING, 1);
        addAnger(-ANGER_PEACE_OFFERING_REDUCTION);
        watchMember.setState(NPCState.PATROLLING);
        if (callback != null) {
            callback.award(AchievementType.PEACEMAKER);
        }
        return true;
    }

    // ── Tier recalculation ─────────────────────────────────────────────────────

    /**
     * Recompute the current tier from watchAnger, firing events and achievements
     * as appropriate.
     */
    private void recalculateTier(NotorietySystem.AchievementCallback callback) {
        int newTier;
        if (watchAnger >= TIER_5_UPRISING) {
            newTier = 5;
        } else if (watchAnger >= TIER_4_ORGANISED_MOB) {
            newTier = 4;
        } else if (watchAnger >= TIER_3_VIGILANTE) {
            newTier = 3;
        } else if (watchAnger >= TIER_2_PETITIONS) {
            newTier = 2;
        } else if (watchAnger >= TIER_1_MUTTERINGS) {
            newTier = 1;
        } else {
            newTier = 0;
        }

        if (newTier != currentTier) {
            onTierChanged(currentTier, newTier, callback);
            currentTier = newTier;
        }
    }

    private void onTierChanged(int oldTier, int newTier, NotorietySystem.AchievementCallback callback) {
        if (newTier > oldTier) {
            // Escalating
            if (newTier >= 1 && !watchedAchieved) {
                watchedAchieved = true;
                if (callback != null) callback.award(AchievementType.WATCHED);
            }
            if (newTier == 2 && !petitionBoardActive) {
                petitionBoardActive = true;
                if (!petitionAchieved) {
                    petitionAchieved = true;
                    if (callback != null) callback.award(AchievementType.PETITION_RECEIVED);
                }
            }
            if (newTier == 5 && !uprisingActive) {
                uprisingActive = true;
                uprisingTimer = UPRISING_DURATION_SECONDS;
            }
        }
        // When dropping below Tier 2, the petition board disappears naturally
        if (newTier < 2 && oldTier >= 2) {
            petitionBoardActive = false;
        }
    }

    private void endUprising(NotorietySystem.AchievementCallback callback) {
        uprisingActive = false;
        watchAnger = ANGER_AFTER_UPRISING_RESET;
        currentTier = 4; // reset tier to match new anger level
        // Dismiss all Watch Members to WANDERING
        for (NPC wm : watchMembers) {
            wm.setState(NPCState.WANDERING);
        }
        if (!uprisingSurvivedAchieved) {
            uprisingSurvivedAchieved = true;
            if (callback != null) callback.award(AchievementType.UPRISING_SURVIVED);
        }
    }

    // ── NPC management ────────────────────────────────────────────────────────

    /**
     * Spawn Watch Member NPCs according to the current tier.
     * The returned list contains the newly created NPCs; callers should add them
     * to the game world via the NPCManager.
     *
     * @param councilTurfFraction fraction (0–1) of world turf controlled by The Council
     * @param playerX             player X position (spawn point reference)
     * @param playerZ             player Z position (spawn point reference)
     * @return list of new WATCH_MEMBER NPCs to add to the world
     */
    public List<NPC> spawnWatchMembersForTier(float councilTurfFraction,
                                               float playerX, float playerZ) {
        List<NPC> spawned = new ArrayList<>();
        int baseCount = 0;
        boolean isTier2 = currentTier == 2;
        boolean isTier3 = currentTier == 3;
        boolean isTier4Plus = currentTier >= 4;

        if (isTier2) {
            baseCount = TIER_2_SPAWN_MIN + random.nextInt(TIER_2_SPAWN_MAX - TIER_2_SPAWN_MIN + 1);
        } else if (isTier3) {
            baseCount = TIER_3_SPAWN_MIN + random.nextInt(TIER_3_SPAWN_MAX - TIER_3_SPAWN_MIN + 1);
        } else if (isTier4Plus) {
            baseCount = TIER_4_SPAWN_MIN + random.nextInt(TIER_4_SPAWN_MAX - TIER_4_SPAWN_MIN + 1);
        }

        // Council funding doubles spawn rate when they hold > 40% turf
        if (councilTurfFraction > COUNCIL_FUNDING_TURF_THRESHOLD) {
            baseCount *= 2;
        }

        for (int i = 0; i < baseCount; i++) {
            float spawnOffsetX = (random.nextFloat() - 0.5f) * 20f;
            float spawnOffsetZ = (random.nextFloat() - 0.5f) * 20f;
            NPC wm = new NPC(NPCType.WATCH_MEMBER, playerX + spawnOffsetX, 1f,
                             playerZ + spawnOffsetZ);
            wm.setState(isTier2 ? NPCState.PATROLLING : NPCState.AGGRESSIVE);
            spawned.add(wm);
            watchMembers.add(wm);
        }
        return spawned;
    }

    /**
     * Remove defeated or out-of-range Watch Members from tracking.
     * Call periodically to keep the list clean.
     */
    public void pruneWatchMembers() {
        Iterator<NPC> it = watchMembers.iterator();
        while (it.hasNext()) {
            NPC wm = it.next();
            if (wm.getHealth() <= 0 || wm.getState() == NPCState.KNOCKED_OUT) {
                it.remove();
            }
        }
    }

    // ── Soft citizen's arrest ─────────────────────────────────────────────────

    /**
     * Perform a soft citizen's arrest on the player (Tier 3 mechanic).
     * Immobilises the player briefly (caller must enforce), adds speech to
     * the Watch Member, and records offences in the criminal record.
     *
     * @param watchMember    the WATCH_MEMBER performing the arrest
     * @param criminalRecord the player's criminal record (may be null)
     */
    public void performSoftCitizensArrest(NPC watchMember, CriminalRecord criminalRecord) {
        if (watchMember == null || watchMember.getType() != NPCType.WATCH_MEMBER) {
            return;
        }
        watchMember.setSpeechText("I'm placing you under citizen's arrest! " +
                                   "Your behaviour has been noted. TWICE.", 5.0f);
        watchMember.setState(NPCState.WARNING);
        if (criminalRecord != null) {
            for (int i = 0; i < CITIZENS_ARREST_OFFENCES; i++) {
                criminalRecord.record(CriminalRecord.CrimeType.ANTISOCIAL_BEHAVIOUR);
            }
        }
    }

    // ── HUD / accessors ───────────────────────────────────────────────────────

    /** Returns the current WatchAnger score (0–100). */
    public int getWatchAnger() {
        return watchAnger;
    }

    /**
     * Sets the WatchAnger score directly (for testing and serialisation).
     * Clamped to [0, 100]. Does NOT fire achievement callbacks.
     */
    public void setWatchAnger(int anger) {
        watchAnger = Math.max(MIN_ANGER, Math.min(MAX_ANGER, anger));
    }

    /** Returns the current escalation tier (0–5). */
    public int getCurrentTier() {
        return currentTier;
    }

    /** Returns true if a Petition Board is currently active in the world. */
    public boolean isPetitionBoardActive() {
        return petitionBoardActive;
    }

    /** Returns true if the Full Uprising is currently active. */
    public boolean isUprisingActive() {
        return uprisingActive;
    }

    /** Returns the remaining time (seconds) of the current uprising, or 0 if none. */
    public float getUprisingTimer() {
        return uprisingTimer;
    }

    /** Returns the list of currently tracked Watch Member NPCs. */
    public List<NPC> getWatchMembers() {
        return watchMembers;
    }

    /**
     * Returns the Watch Anger fill fraction (0.0–1.0) for use in the HUD bar.
     */
    public float getAngerFraction() {
        return watchAnger / (float) MAX_ANGER;
    }
}

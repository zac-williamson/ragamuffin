package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.NotorietySystem.AchievementCallback;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1202: Northfield Karaoke Night — Wetherspoons Friday Special,
 * Crowd Reaction &amp; the Mic Drop Hustle.
 *
 * <p>Every Friday 20:00–23:00, Wetherspoons hosts Karaoke Night. A
 * {@link PropType#KARAOKE_BOOTH_PROP} appears at the back of the pub. The
 * compère is <b>Bev</b> ({@link NPCType#PUBLIC} fallback), a chain-smoking
 * woman in her 50s with a laminated song sheet and a grudge against silence.
 *
 * <h3>Song Queue Flow</h3>
 * <ol>
 *   <li>Press E on {@link PropType#KARAOKE_BOOTH_PROP} to queue.</li>
 *   <li>Bev calls the player up after a random 0–3 NPC turns (see
 *       {@link #tryQueueForStage}).</li>
 *   <li>Player performs using {@link BattleBarMiniGame} (3 bars).</li>
 *   <li>Score 0–1 hits: TERRIBLE — crowd jeers, pint glass thrown, Notoriety +2.</li>
 *   <li>Score 2 hits: ADEQUATE — polite applause.</li>
 *   <li>Score 3 hits: GREAT — crowd celebrates, Notoriety +5, 5 COIN tip from Bev.</li>
 * </ol>
 *
 * <h3>Sabotage</h3>
 * <ul>
 *   <li><b>Cut the PA</b>: Interact with {@link PropType#FUSE_BOX_PROP} while
 *       holding {@link Material#SCREWDRIVER}. Rival NPC fails; Notoriety +3 if
 *       witnessed (NPC within {@link #WITNESS_RADIUS} blocks).</li>
 *   <li><b>Steal the mic</b>: Grab {@link PropType#MICROPHONE_PROP} while Bev
 *       is distracted (10-second window after each song). Yields
 *       {@link Material#MICROPHONE} item; sells to {@link FenceSystem} for
 *       {@link #MIC_FENCE_VALUE} COIN or used with BuskingSystem at double XP.
 *       {@link AchievementType#MIC_DROP} awarded.</li>
 * </ul>
 *
 * <h3>Integrations</h3>
 * <ul>
 *   <li>{@link WetherspoonsSystem}: round-buying spike on GREAT via
 *       {@link WetherspoonsSystem#serveRound}.</li>
 *   <li>{@link NotorietySystem}: TERRIBLE +2, GREAT +5, sabotage witnessed +3.</li>
 *   <li>{@link RumourNetwork}: LOCAL_EVENT rumour seeded on TERRIBLE/GREAT.</li>
 *   <li>{@link StreetSkillSystem}: INFLUENCE XP +1 per performance; +3 on GREAT.</li>
 *   <li>{@link WantedSystem}: mic theft = +1 star; fuse box sabotage witnessed = +1 star.</li>
 *   <li>{@link CriminalRecord}: CRIMINAL_DAMAGE on fuse cut; THEFT on mic steal.</li>
 *   <li>{@link NeighbourhoodSystem}: GREAT = +2 Vibes; glassing = −3 Vibes.</li>
 *   <li>{@link NoiseSystem}: PA on = noise level 7; PA cut = noise drops to 1.</li>
 *   <li>{@link DogCompanionSystem}: dog present = +1 score modifier (20% chance
 *       dog barks = −1 score modifier instead).</li>
 *   <li>{@link AchievementSystem}: KARAOKE_KING, BOTTLED_IT, FULL_HOUSE, MIC_DROP.</li>
 * </ul>
 */
public class KaraokeSystem {

    // ── Constants ──────────────────────────────────────────────────────────────

    /** Day-of-week index for Friday (0 = Monday). */
    public static final int FRIDAY_INDEX = 4;

    /** Karaoke night start hour. */
    public static final float KARAOKE_START_HOUR = 20.0f;

    /** Karaoke night end hour. */
    public static final float KARAOKE_END_HOUR = 23.0f;

    /** Notoriety threshold above which Bev refuses the player. */
    public static final int BEV_REFUSE_NOTORIETY = 50;

    /** Notoriety gained on TERRIBLE performance. */
    public static final int NOTORIETY_TERRIBLE = 2;

    /** Notoriety gained on GREAT performance. */
    public static final int NOTORIETY_GREAT = 5;

    /** Notoriety gained if fuse-box sabotage is witnessed. */
    public static final int NOTORIETY_SABOTAGE_WITNESSED = 3;

    /** COIN tip Bev awards on a GREAT performance. */
    public static final int BEV_TIP_GREAT = 5;

    /** Coin value when selling stolen mic to the Fence. */
    public static final int MIC_FENCE_VALUE = 8;

    /** Number of bars in the karaoke performance. */
    public static final int PERFORMANCE_BARS = 3;

    /** Minimum NPC turns to wait before Bev calls the player (inclusive). */
    public static final int QUEUE_TURNS_MIN = 0;

    /** Maximum NPC turns to wait before Bev calls the player (inclusive). */
    public static final int QUEUE_TURNS_MAX = 3;

    /** Number of PUBLIC/DRUNK NPCs that start jeering on a TERRIBLE result. */
    public static final int JEER_NPC_MIN = 2;

    /** Maximum NPCs that start jeering. */
    public static final int JEER_NPC_MAX = 4;

    /** Duration (seconds) NPCs celebrate or jeer. */
    public static final float CROWD_REACTION_DURATION = 30.0f;

    /** Duration (seconds) of the GLASSED debuff (−10 Alertness). */
    public static final float GLASSED_DEBUFF_DURATION = 60.0f;

    /** Damage dealt to the player by a thrown pint glass. */
    public static final int GLASS_THROW_DAMAGE = 3;

    /** Duration (seconds) of Bev's distracted window after each song (mic-steal window). */
    public static final float BEV_DISTRACTED_DURATION = 10.0f;

    /** Radius (blocks) within which an NPC counts as a witness for sabotage. */
    public static final float WITNESS_RADIUS = 4.0f;

    /** Noise level when PA system is active. */
    public static final float PA_NOISE_LEVEL = 7.0f;

    /** Noise level when PA system is cut. */
    public static final float PA_NOISE_CUT_LEVEL = 1.0f;

    /** INFLUENCE XP awarded per performance (regardless of score). */
    public static final int XP_PER_PERFORMANCE = 1;

    /** Additional INFLUENCE XP awarded on a GREAT performance. */
    public static final int XP_GREAT_BONUS = 3;

    /** Chance that the dog barks during performance (reducing score by 1). */
    public static final float DOG_BARK_CHANCE = 0.20f;

    /** Neighbourhood vibes change on GREAT performance. */
    public static final int VIBES_GREAT = 2;

    /** Neighbourhood vibes change on a glassing incident. */
    public static final int VIBES_GLASSING = -3;

    /** Song pool. */
    public static final String[] SONG_POOL = {
        "Angels",
        "Don't Look Back in Anger",
        "Mr. Brightside",
        "Livin' on a Prayer",
        "Valerie"
    };

    // ── Queue & performance state ──────────────────────────────────────────────

    /** Whether the player is currently queued for the stage. */
    private boolean playerQueued = false;

    /** Remaining NPC turns before Bev calls the player up. */
    private int queueTurnsRemaining = 0;

    /** Whether the PA system is currently active. */
    private boolean paActive = true;

    /** Remaining seconds of Bev's distracted window (mic-steal opportunity). */
    private float bevDistractedTimer = 0f;

    /** Whether the player has been glassed this session. */
    private boolean playerGlassed = false;

    /** Remaining seconds of GLASSED debuff. */
    private float glassedDebuffTimer = 0f;

    /** Whether a pint glass projectile has been spawned (cleared each performance). */
    private boolean pintGlassSpawned = false;

    /** NPCs currently in JEERING state (set by resolvePerformance). */
    private final List<NPC> jeeringNpcs = new ArrayList<>();

    /** The song selected for the current/last performance. */
    private String currentSong = null;

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final Random random;

    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private RumourNetwork rumourNetwork;
    private StreetSkillSystem streetSkillSystem;
    private WantedSystem wantedSystem;
    private NeighbourhoodSystem neighbourhoodSystem;
    private NoiseSystem noiseSystem;
    private DogCompanionSystem dogCompanionSystem;
    private WetherspoonsSystem wetherspoonsSystem;

    /** Bev the compère NPC — spawned at KARAOKE_BOOTH_PROP. */
    private NPC bev;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Create a new KaraokeSystem.
     *
     * @param random seeded RNG for song selection, queue turns, and crowd reactions
     */
    public KaraokeSystem(Random random) {
        this.random = random;
    }

    // ── Dependency setters ────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem ns)         { this.notorietySystem = ns; }
    public void setCriminalRecord(CriminalRecord cr)           { this.criminalRecord = cr; }
    public void setAchievementSystem(AchievementSystem as)     { this.achievementSystem = as; }
    public void setRumourNetwork(RumourNetwork rn)             { this.rumourNetwork = rn; }
    public void setStreetSkillSystem(StreetSkillSystem ss)     { this.streetSkillSystem = ss; }
    public void setWantedSystem(WantedSystem ws)               { this.wantedSystem = ws; }
    public void setNeighbourhoodSystem(NeighbourhoodSystem ns) { this.neighbourhoodSystem = ns; }
    public void setNoiseSystem(NoiseSystem ns)                 { this.noiseSystem = ns; }
    public void setDogCompanionSystem(DogCompanionSystem dcs)  { this.dogCompanionSystem = dcs; }
    public void setWetherspoonsSystem(WetherspoonsSystem ws)   { this.wetherspoonsSystem = ws; }
    public void setBev(NPC bev)                                { this.bev = bev; }

    // ── Time predicate ────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if karaoke night is currently active.
     *
     * @param gameHour   current game hour (0–23.99)
     * @param dayOfWeek  current day (0 = Monday, 4 = Friday, etc.)
     */
    public boolean isKaraokeActive(float gameHour, int dayOfWeek) {
        float normHour = gameHour % 24.0f;
        return dayOfWeek == FRIDAY_INDEX
                && normHour >= KARAOKE_START_HOUR
                && normHour < KARAOKE_END_HOUR;
    }

    // ── Queue for stage ───────────────────────────────────────────────────────

    /**
     * Player presses E on the {@link PropType#KARAOKE_BOOTH_PROP}.
     *
     * @param playerNotoriety current player Notoriety score
     * @param gameHour        current game hour
     * @param dayOfWeek       current day of week
     * @return result of the queue attempt
     */
    public QueueResult tryQueueForStage(int playerNotoriety, float gameHour, int dayOfWeek) {
        if (!isKaraokeActive(gameHour, dayOfWeek)) {
            if (bev != null) {
                bev.setSpeechText("Karaoke's not on tonight, love.", 5f);
            }
            return QueueResult.KARAOKE_INACTIVE;
        }
        if (playerNotoriety >= BEV_REFUSE_NOTORIETY) {
            if (bev != null) {
                bev.setSpeechText("You're not touching my mic, love. I know what you did.", 5f);
            }
            return QueueResult.REFUSED_HIGH_NOTORIETY;
        }
        if (playerQueued) {
            return QueueResult.ALREADY_QUEUED;
        }
        playerQueued = true;
        queueTurnsRemaining = QUEUE_TURNS_MIN + random.nextInt(QUEUE_TURNS_MAX - QUEUE_TURNS_MIN + 1);
        return QueueResult.QUEUED;
    }

    /**
     * Advance the queue by one NPC turn.
     *
     * @return {@code true} if it is now the player's turn to perform
     */
    public boolean advanceQueue() {
        if (!playerQueued) return false;
        if (queueTurnsRemaining <= 0) {
            return true;
        }
        queueTurnsRemaining--;
        return queueTurnsRemaining <= 0;
    }

    /**
     * Called when it's the player's turn — selects a song and sets up the PA.
     *
     * @return the song title selected for this performance
     */
    public String beginPerformance() {
        currentSong = SONG_POOL[random.nextInt(SONG_POOL.length)];
        if (noiseSystem != null) {
            noiseSystem.setNoiseLevel(PA_NOISE_LEVEL);
        }
        if (bev != null) {
            bev.setSpeechText("Up next — give it up for the legend themselves!", 5f);
        }
        bevDistractedTimer = 0f;
        pintGlassSpawned = false;
        jeeringNpcs.clear();
        return currentSong;
    }

    // ── Resolve performance ───────────────────────────────────────────────────

    /**
     * Resolve the result of a karaoke performance.
     *
     * <p>Applies Notoriety, XP, rumours, achievements, crowd reactions, and
     * Wetherspoons round-buying as appropriate.
     *
     * @param hitsScored   number of BattleBar hits out of 3 (adjusted for dog)
     * @param pubNpcs      all NPC patrons currently in the pub
     * @param playerInv    player's inventory (for Bev's tip)
     * @param playerX      player world X (for pint glass spawn / witness check)
     * @param playerZ      player world Z
     * @return performance result
     */
    public PerformanceResult resolvePerformance(int hitsScored, List<NPC> pubNpcs,
                                                 Inventory playerInv,
                                                 float playerX, float playerZ) {
        // Dog modifier
        int adjustedScore = hitsScored;
        if (dogCompanionSystem != null && dogCompanionSystem.isFollowing()) {
            if (random.nextFloat() < DOG_BARK_CHANCE) {
                adjustedScore = Math.max(0, adjustedScore - 1);
            } else {
                adjustedScore = Math.min(PERFORMANCE_BARS, adjustedScore + 1);
            }
        }

        // Award base INFLUENCE XP
        if (streetSkillSystem != null) {
            streetSkillSystem.awardXP(StreetSkillSystem.Skill.INFLUENCE, XP_PER_PERFORMANCE);
        }

        PerformanceResult result;

        if (adjustedScore >= PERFORMANCE_BARS) {
            result = PerformanceResult.GREAT;
            applyGreatOutcome(pubNpcs, playerInv, playerX, playerZ);
        } else if (adjustedScore >= 2) {
            result = PerformanceResult.ADEQUATE;
            applyAdequateOutcome(pubNpcs);
        } else {
            result = PerformanceResult.TERRIBLE;
            applyTerribleOutcome(pubNpcs, playerInv, playerX, playerZ);
        }

        // Check FULL_HOUSE
        if (achievementSystem != null && pubNpcs != null && pubNpcs.size() >= 10) {
            achievementSystem.unlock(AchievementType.FULL_HOUSE);
        }

        // Bev distracted window opens after performance
        bevDistractedTimer = BEV_DISTRACTED_DURATION;
        playerQueued = false;

        return result;
    }

    private void applyGreatOutcome(List<NPC> pubNpcs, Inventory playerInv,
                                    float playerX, float playerZ) {
        // Notoriety +5
        AchievementCallback cb = achievementCallback();
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(NOTORIETY_GREAT, cb);
        }

        // 5 COIN tip from Bev
        if (playerInv != null) {
            playerInv.addItem(Material.COIN, BEV_TIP_GREAT);
        }

        // Bev speech
        if (bev != null) {
            bev.setSpeechText("Get in! Give it up!", 8f);
        }

        // All NPCs celebrate
        if (pubNpcs != null) {
            for (NPC npc : pubNpcs) {
                if (npc != null) {
                    npc.setState(NPCState.CELEBRATING);
                }
            }
        }

        // Wetherspoons round-buying spike
        if (wetherspoonsSystem != null) {
            wetherspoonsSystem.serveRound(pubNpcs);
        }

        // Rumour
        seedRumour("Did you see that karaoke? Absolute legend.");

        // Street skill bonus XP
        if (streetSkillSystem != null) {
            streetSkillSystem.awardXP(StreetSkillSystem.Skill.INFLUENCE, XP_GREAT_BONUS);
        }

        // Neighbourhood vibes
        if (neighbourhoodSystem != null) {
            neighbourhoodSystem.setVibes(
                    Math.min(100, neighbourhoodSystem.getVibes() + VIBES_GREAT));
        }

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.KARAOKE_KING);
        }
    }

    private void applyAdequateOutcome(List<NPC> pubNpcs) {
        // Polite applause — some NPCs wave
        if (pubNpcs != null) {
            for (NPC npc : pubNpcs) {
                if (npc != null) {
                    npc.setSpeechText("Wasn't the worst I've heard.", 4f);
                }
            }
        }
    }

    private void applyTerribleOutcome(List<NPC> pubNpcs, Inventory playerInv,
                                       float playerX, float playerZ) {
        AchievementCallback cb = achievementCallback();

        // Notoriety +2
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(NOTORIETY_TERRIBLE, cb);
        }

        // 2–4 NPCs jeer
        int jeerCount = JEER_NPC_MIN + random.nextInt(JEER_NPC_MAX - JEER_NPC_MIN + 1);
        jeeringNpcs.clear();
        if (pubNpcs != null) {
            int added = 0;
            for (NPC npc : pubNpcs) {
                if (npc != null && added < jeerCount
                        && (npc.getType() == NPCType.PUBLIC || npc.getType() == NPCType.DRUNK)) {
                    npc.setState(NPCState.JEERING);
                    jeeringNpcs.add(npc);
                    added++;
                }
            }
        }

        // Pint glass projectile (flags that one has been thrown)
        pintGlassSpawned = true;
        // Apply damage + debuff directly (in integration would deal 3 hp damage)
        playerGlassed = true;
        glassedDebuffTimer = GLASSED_DEBUFF_DURATION;

        // Achievement for being glassed
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.BOTTLED_IT);
        }

        // Neighbourhood vibes
        if (neighbourhoodSystem != null) {
            neighbourhoodSystem.setVibes(
                    Math.max(0, neighbourhoodSystem.getVibes() + VIBES_GLASSING));
        }

        // Rumour
        seedRumour("Someone got bottled at karaoke last night.");
    }

    // ── Sabotage ──────────────────────────────────────────────────────────────

    /**
     * Player interacts with {@link PropType#FUSE_BOX_PROP}.
     *
     * @param playerInv   player's inventory (must contain SCREWDRIVER)
     * @param nearbyNpcs  NPCs nearby; if any are within {@link #WITNESS_RADIUS},
     *                    Notoriety +3 is applied
     * @param playerX     player world X (for witness distance check)
     * @param playerZ     player world Z
     * @return result of the sabotage attempt
     */
    public SabotageResult interactFuseBox(Inventory playerInv, List<NPC> nearbyNpcs,
                                           float playerX, float playerZ) {
        if (playerInv == null || playerInv.getItemCount(Material.SCREWDRIVER) < 1) {
            return SabotageResult.NO_TOOL;
        }

        // Cut the PA
        paActive = false;
        if (noiseSystem != null) {
            noiseSystem.setNoiseLevel(PA_NOISE_CUT_LEVEL);
        }

        // Log crime
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.CRIMINAL_DAMAGE);
        }

        // Witness check
        boolean witnessed = false;
        if (nearbyNpcs != null) {
            for (NPC npc : nearbyNpcs) {
                if (npc == null) continue;
                float dx = npc.getPosition().x - playerX;
                float dz = npc.getPosition().z - playerZ;
                float dist = (float) Math.sqrt(dx * dx + dz * dz);
                if (dist <= WITNESS_RADIUS) {
                    witnessed = true;
                    break;
                }
            }
        }

        if (witnessed) {
            AchievementCallback cb = achievementCallback();
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(NOTORIETY_SABOTAGE_WITNESSED, cb);
            }
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, playerX, 0f, playerZ, null);
            }
            return SabotageResult.CUT_WITNESSED;
        }

        return SabotageResult.CUT_UNWITNESSED;
    }

    /**
     * Player interacts with {@link PropType#MICROPHONE_PROP} during the Bev
     * distracted window.
     *
     * @param playerInv player's inventory (MICROPHONE item is added)
     * @param playerX   player world X (for wanted star LKP)
     * @param playerZ   player world Z
     * @return result of the mic-steal attempt
     */
    public MicStealResult interactMicrophoneProp(Inventory playerInv,
                                                  float playerX, float playerZ) {
        if (bevDistractedTimer <= 0f) {
            return MicStealResult.BEV_WATCHING;
        }

        if (playerInv != null) {
            playerInv.addItem(Material.MICROPHONE, 1);
        }

        // Log crime
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.THEFT);
        }

        // Wanted star
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(1, playerX, 0f, playerZ, null);
        }

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.MIC_DROP);
        }

        // Mic prop is gone — distracted window closes
        bevDistractedTimer = 0f;

        return MicStealResult.STOLEN;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Advance timers. Call each frame while karaoke night is active.
     *
     * @param delta seconds since last frame
     */
    public void update(float delta) {
        if (bevDistractedTimer > 0f) {
            bevDistractedTimer -= delta;
            if (bevDistractedTimer < 0f) bevDistractedTimer = 0f;
        }
        if (glassedDebuffTimer > 0f) {
            glassedDebuffTimer -= delta;
            if (glassedDebuffTimer < 0f) {
                glassedDebuffTimer = 0f;
                playerGlassed = false;
            }
        }
    }

    // ── Song selection helper ─────────────────────────────────────────────────

    /**
     * Pick a song at random from the pool (exposed for unit testing distribution).
     *
     * @return song title
     */
    public String pickSong() {
        return SONG_POOL[random.nextInt(SONG_POOL.length)];
    }

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Result of pressing E on the KARAOKE_BOOTH_PROP. */
    public enum QueueResult {
        /** Player successfully joined the queue. */
        QUEUED,
        /** Karaoke night is not currently active. */
        KARAOKE_INACTIVE,
        /** Bev refused — player Notoriety ≥ 50. */
        REFUSED_HIGH_NOTORIETY,
        /** Player is already in the queue. */
        ALREADY_QUEUED
    }

    /** Result of a complete karaoke performance. */
    public enum PerformanceResult {
        /** 0–1 hits: crowd boos, pint glass thrown, Notoriety +2. */
        TERRIBLE,
        /** 2 hits: polite applause. */
        ADEQUATE,
        /** 3 hits: crowd celebrates, Notoriety +5, 5 COIN tip. */
        GREAT
    }

    /** Result of interacting with FUSE_BOX_PROP. */
    public enum SabotageResult {
        /** PA system cut; no witness. */
        CUT_UNWITNESSED,
        /** PA system cut; NPC witness within {@link #WITNESS_RADIUS} blocks. */
        CUT_WITNESSED,
        /** Player does not have SCREWDRIVER. */
        NO_TOOL
    }

    /** Result of interacting with MICROPHONE_PROP. */
    public enum MicStealResult {
        /** Mic stolen successfully; MICROPHONE added to inventory. */
        STOLEN,
        /** Bev is not distracted; steal window not open. */
        BEV_WATCHING
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** @return {@code true} if karaoke is active and PA is on. */
    public boolean isPaActive() { return paActive; }

    /** @return {@code true} if the player is currently queued. */
    public boolean isPlayerQueued() { return playerQueued; }

    /** @return remaining NPC turns before player is called up. */
    public int getQueueTurnsRemaining() { return queueTurnsRemaining; }

    /** @return remaining seconds of Bev's distracted window. */
    public float getBevDistractedTimer() { return bevDistractedTimer; }

    /** @return {@code true} if the player currently has the GLASSED debuff. */
    public boolean isPlayerGlassed() { return playerGlassed; }

    /** @return remaining seconds of the GLASSED debuff. */
    public float getGlassedDebuffTimer() { return glassedDebuffTimer; }

    /** @return {@code true} if a pint glass projectile was spawned in the last performance. */
    public boolean isPintGlassSpawned() { return pintGlassSpawned; }

    /** @return NPCs currently in JEERING state (from last performance). */
    public List<NPC> getJeeringNpcs() { return jeeringNpcs; }

    /** @return the song title for the current/last performance. */
    public String getCurrentSong() { return currentSong; }

    /** @return the Bev compère NPC, or {@code null} if not spawned. */
    public NPC getBev() { return bev; }

    // ── Test helpers ──────────────────────────────────────────────────────────

    /** Force-spawn Bev (for testing). */
    public void forceSpawnBev() {
        this.bev = new NPC(NPCType.PUBLIC, 0f, 0f, 0f);
    }

    /** Directly set the PA active state (for testing). */
    public void setPaActiveForTesting(boolean active) { this.paActive = active; }

    /** Directly set the player queued state (for testing). */
    public void setPlayerQueuedForTesting(boolean queued) { this.playerQueued = queued; }

    /** Directly set the queue turns remaining (for testing). */
    public void setQueueTurnsRemainingForTesting(int turns) { this.queueTurnsRemaining = turns; }

    /** Directly set the Bev distracted timer (for testing). */
    public void setBevDistractedTimerForTesting(float seconds) { this.bevDistractedTimer = seconds; }

    /** Directly set glassed state (for testing). */
    public void setGlassedForTesting(boolean glassed) {
        this.playerGlassed = glassed;
        this.glassedDebuffTimer = glassed ? GLASSED_DEBUFF_DURATION : 0f;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void seedRumour(String text) {
        if (rumourNetwork == null) return;
        NPC source = bev != null ? bev : new NPC(NPCType.PUBLIC, 0f, 0f, 0f);
        rumourNetwork.addRumour(source, new Rumour(RumourType.LOCAL_EVENT, text));
    }

    private AchievementCallback achievementCallback() {
        if (achievementSystem == null) return null;
        return type -> achievementSystem.unlock(type);
    }
}

package ragamuffin.core;

import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.BlockType;
import ragamuffin.world.Landmark;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.PropPosition;
import ragamuffin.world.PropType;
import ragamuffin.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #922: Skate Park System — Tricks, Street Rep &amp; Council Crackdown.
 *
 * <p>Gives the {@link LandmarkType#SKATE_PARK} landmark actual gameplay:
 * <ul>
 *   <li>Player can press T while on CONCRETE inside the park to perform tricks.</li>
 *   <li>Five trick ranks unlock progressively with successful attempts.</li>
 *   <li>Tricks earn session score → coins (every 50 score = 1 COIN) and Street
 *       Lads Respect (every 100 score = +5).</li>
 *   <li>Council Enforcement Events: every 8 in-game hours the Council has a 40%
 *       chance to post a CLOSURE_NOTICE; if ignored for 10 minutes, the park
 *       entrance is blocked for 30 minutes.</li>
 *   <li>ASBO mechanic: tricking near POLICE/PCSO has a 30% chance of earning an
 *       ASBO (Notoriety +5, ANTISOCIAL_BEHAVIOUR criminal record, 1-day park ban).</li>
 *   <li>Skater NPC spawning window: 14:00–22:00.</li>
 * </ul>
 */
public class SkateParkSystem {

    // ── Trick definitions ─────────────────────────────────────────────────────

    /** Trick names in rank order. */
    public static final String[] TRICK_NAMES = {
        "Kickflip", "Heelflip", "50-50 Grind", "720 Spin", "McTwist"
    };

    /** Score awarded for each trick rank. */
    public static final int[] TRICK_SCORES = { 10, 20, 35, 60, 100 };

    /** Number of successful tricks required to unlock each rank. */
    public static final int[] RANK_UNLOCK_THRESHOLDS = { 0, 3, 8, 18, 35 };

    /** Maximum trick rank (0-indexed). */
    public static final int MAX_RANK = 4;

    // ── Thresholds ────────────────────────────────────────────────────────────

    /** Minimum speed (blocks/s) to attempt a trick. */
    public static final float MIN_SPEED_TO_TRY  = 0.5f;

    /** Minimum speed (blocks/s) for a trick to succeed. */
    public static final float MIN_SPEED_TO_LAND = 1.0f;

    /** Energy cost per trick attempt (success or failure). */
    public static final int TRICK_ENERGY_COST = 5;

    /** Cooldown in seconds between trick attempts. */
    public static final float TRICK_COOLDOWN_SECONDS = 2.0f;

    /** Score per COIN conversion. */
    public static final int SCORE_PER_COIN = 50;

    /** Score per Street Lads Respect conversion. */
    public static final int SCORE_PER_RESPECT = 100;

    /** Street Lads Respect awarded per conversion. */
    public static final int RESPECT_AWARD = 5;

    /** Skateboard score multiplier (when holding SKATEBOARD). */
    public static final float SKATEBOARD_MULTIPLIER = 1.15f;

    // ── Council enforcement ───────────────────────────────────────────────────

    /** In-game hours between enforcement event chances. */
    public static final float ENFORCEMENT_INTERVAL_HOURS = 8.0f;

    /** Probability of enforcement event each interval (0–1). */
    public static final float ENFORCEMENT_CHANCE = 0.40f;

    /** In-game minutes before lockout if notice not torn down. */
    public static final float NOTICE_TIMEOUT_MINUTES = 10.0f;

    /** In-game minutes the park stays locked after enforcement. */
    public static final float LOCKOUT_DURATION_MINUTES = 30.0f;

    /** Bribe cost (COIN) to cancel an enforcement event. */
    public static final int BRIBE_COST = 8;

    /** Street Lads Respect gained for tearing down a notice. */
    public static final int TEAR_DOWN_STREET_LADS_RESPECT = 10;

    /** Council Respect change for tearing down a notice. */
    public static final int TEAR_DOWN_COUNCIL_RESPECT = -5;

    /** Street Lads Respect gained for bribing the council member. */
    public static final int BRIBE_STREET_LADS_RESPECT = 5;

    /** Council Respect gained for bribing the council member. */
    public static final int BRIBE_COUNCIL_RESPECT = 5;

    // ── ASBO mechanic ─────────────────────────────────────────────────────────

    /** Radius within which a POLICE/PCSO triggers ASBO check. */
    public static final float POLICE_ASBO_RADIUS = 8.0f;

    /** Probability of ASBO when police are within range. Overridable for tests. */
    public static float ASBO_CHANCE = 0.30f;

    /** Notoriety gained on ASBO. */
    public static final int ASBO_NOTORIETY_GAIN = 5;

    /** In-game hours of park ban after ASBO. */
    public static final float ASBO_BAN_HOURS = 24.0f;

    // ── NPC spawning ──────────────────────────────────────────────────────────

    /** Earliest hour to spawn skater NPCs. */
    public static final float SKATER_SPAWN_START = 14.0f;

    /** Latest hour for skater NPCs to remain. */
    public static final float SKATER_SPAWN_END = 22.0f;

    /** Min SCHOOL_KID NPCs in the park during window. */
    public static final int SCHOOL_KID_MIN = 3;

    /** Max SCHOOL_KID NPCs in the park during window. */
    public static final int SCHOOL_KID_MAX = 6;

    /** Min YOUTH_GANG NPCs at the perimeter during window. */
    public static final int YOUTH_GANG_MIN = 1;

    /** Max YOUTH_GANG NPCs at the perimeter during window. */
    public static final int YOUTH_GANG_MAX = 2;

    // ── In-game time conversion ───────────────────────────────────────────────

    /** Real seconds per in-game minute (TimeSystem uses 0.1 hours/real-second → 6 min/s). */
    private static final float IN_GAME_MINUTES_PER_REAL_SECOND = 6.0f;

    /** Real seconds per in-game hour. */
    private static final float REAL_SECONDS_PER_IN_GAME_HOUR = 60.0f / IN_GAME_MINUTES_PER_REAL_SECOND;

    // ── Result enum ───────────────────────────────────────────────────────────

    /** Result of a trick attempt. */
    public enum TrickResult {
        /** Trick succeeded. */
        SUCCESS,
        /** Player not in skate park or not on concrete. */
        FAIL_NOT_IN_PARK,
        /** Player moving too slowly to land the trick. */
        FAIL_TOO_SLOW,
        /** Not enough energy. */
        FAIL_NO_ENERGY,
        /** Cooldown between tricks not yet expired. */
        FAIL_COOLDOWN
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Current trick rank (0–4). */
    private int skillRank = 0;

    /** Total successful tricks performed this session. */
    private int successfulTricks = 0;

    /** Accumulated session score (used for coin / respect conversion). */
    private int sessionScore = 0;

    /** Score already converted to coins. */
    private int coinsConverted = 0;

    /** Score already converted to Street Lads Respect. */
    private int respectConverted = 0;

    /** Cooldown timer until next trick is allowed (real seconds). */
    private float trickCooldownTimer = 0.0f;

    /** Result of the last trick attempt. */
    private TrickResult lastTrickResult = null;

    /** Whether the first trick tooltip has been shown. */
    private boolean firstTrickShown = false;

    /** Number of closure notices torn down (for achievement / newspaper). */
    private int closureNoticesTornDown = 0;

    /** Number of ASBOs received. */
    private int asboCount = 0;

    // ── Council enforcement state ─────────────────────────────────────────────

    /** Hours accumulated since last enforcement check. */
    private float hoursSinceLastEnforcementCheck = 0.0f;

    /** Whether a closure event is currently active. */
    private boolean closureEventActive = false;

    /** Real-second timer counting how long the notice has been up. */
    private float noticeTimer = 0.0f;

    /** Whether the park is currently locked out. */
    private boolean parkLocked = false;

    /** Real-second timer counting the lockout duration. */
    private float lockedOutTimer = 0.0f;

    /** Index into world's prop list of the active closure notice (-1 = none). */
    private int closureNoticePropIndex = -1;

    /** The COUNCIL_MEMBER NPC for the active enforcement event. */
    private NPC councilMemberNpc = null;

    /** Position where the entrance block was placed (for removal on lockout end). */
    private int entranceBlockX = -1;
    private int entranceBlockY = -1;
    private int entranceBlockZ = -1;

    // ── ASBO / ban state ──────────────────────────────────────────────────────

    /** Whether the player is currently park-banned. */
    private boolean playerBanned = false;

    /** Remaining real-seconds of park ban. */
    private float banTimer = 0.0f;

    // ── Skater NPC tracking ───────────────────────────────────────────────────

    /** NPCs currently managed by this system as skater NPCs. */
    private final List<NPC> skaterNpcs = new ArrayList<>();

    /** Whether skater NPCs are currently spawned. */
    private boolean skatersSpawned = false;

    // ── Session tracking ──────────────────────────────────────────────────────

    /** Tricks completed this session (for KICKFLIP_KING achievement: 10 in one session). */
    private int tricksThisSession = 0;

    /** Whether KICKFLIP_KING has been awarded this session. */
    private boolean kickflipKingThisSession = false;

    /** Whether first ASBO tooltip has been shown. */
    private boolean firstAsboShown = false;

    /** In-game hour at start of today (for daily CLOSURE_NOTICES_TORN_DOWN counter). */
    private int todayDayIndex = -1;

    /** Closure notices torn down today. */
    private int closuresTornDownToday = 0;

    /** Whether the newspaper headline for 3+ notices has been triggered today. */
    private boolean newspaperTriggeredToday = false;

    private final Random random;

    // ── Constructor ───────────────────────────────────────────────────────────

    public SkateParkSystem() {
        this(new Random());
    }

    public SkateParkSystem(Random random) {
        this.random = random;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Update the skate park system. Call once per frame.
     *
     * @param delta       seconds since last frame
     * @param timeSystem  game time
     * @param npcManager  NPC manager for spawning
     * @param world       the world
     */
    public void update(float delta, TimeSystem timeSystem, NPCManager npcManager, World world) {
        // Advance trick cooldown
        if (trickCooldownTimer > 0f) {
            trickCooldownTimer = Math.max(0f, trickCooldownTimer - delta);
        }

        // Advance ban timer
        if (banTimer > 0f) {
            banTimer = Math.max(0f, banTimer - delta);
            if (banTimer <= 0f) {
                playerBanned = false;
            }
        }

        // Advance enforcement check timer (in hours using game time speed)
        // TimeSystem.DEFAULT_TIME_SPEED = 0.1 hours/real-second
        hoursSinceLastEnforcementCheck += delta * 0.1f; // 0.1 = timeSpeed in hours/s

        // Enforcement event timer
        if (closureEventActive) {
            noticeTimer += delta;
            float timeoutSeconds = NOTICE_TIMEOUT_MINUTES * REAL_SECONDS_PER_IN_GAME_HOUR / 60.0f
                    * IN_GAME_MINUTES_PER_REAL_SECOND;
            if (noticeTimer >= timeoutSeconds && !parkLocked) {
                activateLockout(world);
            }
        }

        // Lockout timer
        if (parkLocked) {
            lockedOutTimer = Math.max(0f, lockedOutTimer - delta);
            if (lockedOutTimer <= 0f) {
                deactivateLockout(world);
            }
        }

        // Check enforcement interval
        if (hoursSinceLastEnforcementCheck >= ENFORCEMENT_INTERVAL_HOURS && !closureEventActive) {
            hoursSinceLastEnforcementCheck = 0f;
            // High notoriety (Tier 3+) halves interval — but here we just check probability
            if (random.nextFloat() < ENFORCEMENT_CHANCE) {
                triggerEnforcementEvent(world, npcManager);
            }
        }

        // Skater NPC spawn window management
        float hour = timeSystem.getTime();
        boolean inWindow = hour >= SKATER_SPAWN_START && hour < SKATER_SPAWN_END;
        if (inWindow && !skatersSpawned) {
            spawnSkaterNpcs(world, npcManager);
        } else if (!inWindow && skatersSpawned) {
            despawnSkaterNpcs();
        }

        // Daily counter reset
        int dayIndex = timeSystem.getDayIndex();
        if (dayIndex != todayDayIndex) {
            todayDayIndex = dayIndex;
            closuresTornDownToday = 0;
            newspaperTriggeredToday = false;
        }
    }

    // ── Trick mechanic ────────────────────────────────────────────────────────

    /**
     * Attempt to perform a skate trick. The player must be inside the SKATE_PARK
     * AABB, on or above a CONCRETE block, and moving fast enough.
     *
     * @param player          the player
     * @param world           the world
     * @param npcManager      NPC manager (for ASBO checks)
     * @param inventory       player inventory (coins added here)
     * @param hotbarSlot      currently selected hotbar slot (for skateboard check)
     * @param factionSystem   for awarding Street Lads Respect
     * @param notorietySystem for ASBO notoriety gain
     * @param criminalRecord  for ASBO criminal record
     * @param rumourNetwork   for McTwist rumour seeding
     * @param achievementSystem for achievements
     * @return the result of this attempt
     */
    public TrickResult attemptTrick(
            Player player,
            World world,
            NPCManager npcManager,
            Inventory inventory,
            int hotbarSlot,
            FactionSystem factionSystem,
            NotorietySystem notorietySystem,
            CriminalRecord criminalRecord,
            RumourNetwork rumourNetwork,
            NotorietySystem.AchievementCallback achievementSystem) {

        // Check in park
        Landmark park = (world != null) ? world.getLandmark(LandmarkType.SKATE_PARK) : null;
        if (park == null || !isInPark(player, park)) {
            lastTrickResult = TrickResult.FAIL_NOT_IN_PARK;
            return lastTrickResult;
        }

        // Check cooldown
        if (trickCooldownTimer > 0f) {
            lastTrickResult = TrickResult.FAIL_COOLDOWN;
            return lastTrickResult;
        }

        // Check energy (consume regardless)
        if (player.getEnergy() < TRICK_ENERGY_COST) {
            lastTrickResult = TrickResult.FAIL_NO_ENERGY;
            return lastTrickResult;
        }
        player.consumeEnergy(TRICK_ENERGY_COST);

        // Start cooldown (applies to both success and fail)
        trickCooldownTimer = TRICK_COOLDOWN_SECONDS;

        // Check speed
        float speed = getPlayerSpeed(player);
        if (speed < MIN_SPEED_TO_LAND) {
            lastTrickResult = TrickResult.FAIL_TOO_SLOW;
            return lastTrickResult;
        }

        // ── Success ───────────────────────────────────────────────────────────
        int baseScore = TRICK_SCORES[skillRank];

        // Skateboard multiplier
        boolean holdingSkateboard = isHoldingSkateboard(inventory, hotbarSlot);
        float multiplier = holdingSkateboard ? SKATEBOARD_MULTIPLIER : 1.0f;
        int score = Math.round(baseScore * multiplier);

        sessionScore += score;
        successfulTricks++;
        tricksThisSession++;

        // Rank progression
        updateRank();

        // Convert score to coins
        int newCoins = (sessionScore / SCORE_PER_COIN) - coinsConverted;
        if (newCoins > 0 && inventory != null) {
            inventory.addItem(Material.COIN, newCoins);
            coinsConverted += newCoins;
        }

        // Convert score to Street Lads Respect
        int newRespectBlocks = (sessionScore / SCORE_PER_RESPECT) - respectConverted;
        if (newRespectBlocks > 0 && factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.STREET_LADS, newRespectBlocks * RESPECT_AWARD);
            respectConverted += newRespectBlocks;
        }

        // NPC reactions for big tricks (rank 3 = 720 Spin, rank 4 = McTwist)
        if (skillRank >= 3 && npcManager != null) {
            reactNpcsToTrick(player, npcManager, true);
        }

        // McTwist special handling
        if (skillRank == MAX_RANK) {
            handleMcTwist(player, npcManager, rumourNetwork, achievementSystem);
        }

        // ASBO check
        checkAsbo(player, npcManager, notorietySystem, criminalRecord, achievementSystem);

        // Achievements
        if (achievementSystem != null) {
            if (tricksThisSession >= 10 && !kickflipKingThisSession) {
                kickflipKingThisSession = true;
                achievementSystem.award(AchievementType.KICKFLIP_KING);
            }
        }

        lastTrickResult = TrickResult.SUCCESS;
        return lastTrickResult;
    }

    /**
     * Convenience overload: uses a simplified signature for integration tests
     * where fewer systems are wired.
     */
    public TrickResult attemptTrick(Player player, World world, TimeSystem timeSystem,
                                    NPCManager npcManager) {
        return attemptTrick(player, world, npcManager, null, 0, null, null, null, null, null);
    }

    // ── Council enforcement ────────────────────────────────────────────────────

    /**
     * Trigger a Park Closure Attempt: spawn a COUNCIL_MEMBER and place a
     * CLOSURE_NOTICE prop on the park perimeter.
     *
     * @param world      the world
     * @param npcManager NPC manager
     */
    public void triggerEnforcementEvent(World world, NPCManager npcManager) {
        if (closureEventActive) return;
        if (world == null) return;

        Landmark park = world.getLandmark(LandmarkType.SKATE_PARK);
        if (park == null) return;

        closureEventActive = true;
        noticeTimer = 0f;

        // Place CLOSURE_NOTICE on perimeter: at (parkX + width/2, 1, parkZ) — south edge
        float noticeX = park.getPosition().x + park.getWidth() / 2f;
        float noticeZ = park.getPosition().z;
        PropPosition noticeProp = new PropPosition(noticeX, 1f, noticeZ, PropType.CLOSURE_NOTICE, 0f);
        world.addPropPosition(noticeProp);
        closureNoticePropIndex = world.getPropPositions().size() - 1;

        // Spawn COUNCIL_MEMBER NPC nearby
        if (npcManager != null) {
            float councilX = park.getPosition().x + park.getWidth() / 2f + 2f;
            float councilZ = park.getPosition().z - 2f;
            councilMemberNpc = npcManager.spawnNPC(NPCType.COUNCIL_MEMBER, councilX, 1f, councilZ);
            if (councilMemberNpc != null) {
                councilMemberNpc.setSpeechText(
                        "This facility is to be closed pending a risk assessment and licensing review.",
                        8.0f);
            }
        }
    }

    /**
     * Called when the player interacts (E) with the CLOSURE_NOTICE prop.
     * Tears down the notice, awards Street Lads Respect, deducts Council Respect.
     *
     * @param player          the player
     * @param factionSystem   faction system
     * @param notorietySystem notoriety (for high-notoriety ASBO check)
     * @param world           the world
     * @param achievementSystem for achievements
     */
    public void onPlayerInteractClosureNotice(
            Player player,
            FactionSystem factionSystem,
            NotorietySystem notorietySystem,
            World world,
            NotorietySystem.AchievementCallback achievementSystem) {

        if (!closureEventActive) return;

        // Remove the notice prop
        if (world != null && closureNoticePropIndex >= 0
                && closureNoticePropIndex < world.getPropPositions().size()) {
            world.removeProp(closureNoticePropIndex);
            closureNoticePropIndex = -1;
        }

        // Faction effects
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.STREET_LADS, TEAR_DOWN_STREET_LADS_RESPECT);
            factionSystem.applyRespectDelta(Faction.THE_COUNCIL, TEAR_DOWN_COUNCIL_RESPECT);
        }

        closureNoticesTornDown++;
        closuresTornDownToday++;
        closureEventActive = false;
        noticeTimer = 0f;

        // Despawn council member
        if (councilMemberNpc != null) {
            councilMemberNpc.setState(NPCState.FLEEING);
            councilMemberNpc = null;
        }

        // Achievement
        if (achievementSystem != null && closureNoticesTornDown >= 3) {
            achievementSystem.award(AchievementType.COUNCIL_SABOTEUR);
        }
    }

    /**
     * Called when the player bribes the COUNCIL_MEMBER NPC to cancel the event.
     *
     * @param inventory     player inventory (8 COIN deducted)
     * @param factionSystem faction system
     * @param world         the world
     * @return true if bribe succeeded (player had enough coins)
     */
    public boolean bribeCouncilMember(
            Inventory inventory,
            FactionSystem factionSystem,
            World world) {

        if (!closureEventActive) return false;
        if (inventory == null) return false;
        if (inventory.getItemCount(Material.COIN) < BRIBE_COST) return false;

        inventory.removeItem(Material.COIN, BRIBE_COST);

        // Remove notice
        if (world != null && closureNoticePropIndex >= 0
                && closureNoticePropIndex < world.getPropPositions().size()) {
            world.removeProp(closureNoticePropIndex);
            closureNoticePropIndex = -1;
        }

        // Faction effects
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.STREET_LADS, BRIBE_STREET_LADS_RESPECT);
            factionSystem.applyRespectDelta(Faction.THE_COUNCIL, BRIBE_COUNCIL_RESPECT);
        }

        closureEventActive = false;
        noticeTimer = 0f;

        if (councilMemberNpc != null) {
            councilMemberNpc.setState(NPCState.FLEEING);
            councilMemberNpc = null;
        }

        return true;
    }

    // ── Lockout ───────────────────────────────────────────────────────────────

    private void activateLockout(World world) {
        parkLocked = true;
        float lockoutSeconds = LOCKOUT_DURATION_MINUTES * REAL_SECONDS_PER_IN_GAME_HOUR / 60.0f
                * IN_GAME_MINUTES_PER_REAL_SECOND;
        lockedOutTimer = lockoutSeconds;
        closureEventActive = false;
        noticeTimer = 0f;

        // Block the park entrance with CONCRETE
        if (world != null) {
            Landmark park = world.getLandmark(LandmarkType.SKATE_PARK);
            if (park != null) {
                entranceBlockX = (int) (park.getPosition().x + park.getWidth() / 2);
                entranceBlockY = 1;
                entranceBlockZ = (int) park.getPosition().z;
                world.setBlock(entranceBlockX, entranceBlockY, entranceBlockZ, BlockType.CONCRETE);
            }
        }
    }

    private void deactivateLockout(World world) {
        parkLocked = false;
        lockedOutTimer = 0f;

        // Remove the entrance block
        if (world != null && entranceBlockX >= 0) {
            world.setBlock(entranceBlockX, entranceBlockY, entranceBlockZ, BlockType.AIR);
            entranceBlockX = -1;
        }
    }

    // ── ASBO mechanic ─────────────────────────────────────────────────────────

    private void checkAsbo(Player player, NPCManager npcManager,
                           NotorietySystem notorietySystem,
                           CriminalRecord criminalRecord,
                           NotorietySystem.AchievementCallback achievementSystem) {

        if (npcManager == null) return;

        // Check for POLICE/PCSO within range
        boolean policeNearby = false;
        for (NPC npc : npcManager.getNPCs()) {
            if (!npc.isAlive()) continue;
            if (npc.getType() == NPCType.POLICE || npc.getType() == NPCType.PCSO) {
                float dist = npc.getPosition().dst(player.getPosition());
                if (dist <= POLICE_ASBO_RADIUS) {
                    policeNearby = true;
                    break;
                }
            }
        }

        if (!policeNearby) return;

        // Roll ASBO chance
        if (random.nextFloat() >= ASBO_CHANCE) return;

        // Apply ASBO
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(ASBO_NOTORIETY_GAIN, achievementSystem);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.ANTISOCIAL_BEHAVIOUR);
        }

        // Apply ban
        float banSeconds = ASBO_BAN_HOURS * REAL_SECONDS_PER_IN_GAME_HOUR;
        playerBanned = true;
        banTimer = banSeconds;

        asboCount++;

        // Achievement
        if (achievementSystem != null && asboCount >= 3) {
            achievementSystem.award(AchievementType.ASBO_MAGNET);
        }
    }

    // ── McTwist handling ──────────────────────────────────────────────────────

    private void handleMcTwist(Player player, NPCManager npcManager,
                               RumourNetwork rumourNetwork,
                               NotorietySystem.AchievementCallback achievementSystem) {

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.award(AchievementType.PARK_LEGEND);
        }

        // Seed rumour in nearest NPC within 15 blocks
        if (npcManager != null && rumourNetwork != null) {
            NPC nearest = null;
            float nearestDist = Float.MAX_VALUE;
            for (NPC npc : npcManager.getNPCs()) {
                if (!npc.isAlive()) continue;
                float dist = npc.getPosition().dst(player.getPosition());
                if (dist < nearestDist && dist <= 15f) {
                    nearestDist = dist;
                    nearest = npc;
                }
            }
            if (nearest != null) {
                rumourNetwork.addRumour(nearest, new Rumour(RumourType.PLAYER_SPOTTED,
                        "Someone just pulled a McTwist down the skate park. Actual legend."));
            }
        }

        // NPC speech reactions
        if (npcManager != null) {
            reactNpcsToTrick(player, npcManager, true);
        }
    }

    // ── NPC reactions ─────────────────────────────────────────────────────────

    private void reactNpcsToTrick(Player player, NPCManager npcManager, boolean success) {
        String[] successLines = { "BRUUUH", "Mad ting" };
        String[] failLines = { "Get rekt", "Unlucky, son." };
        String[] lines = success ? successLines : failLines;

        for (NPC npc : npcManager.getNPCs()) {
            if (!npc.isAlive()) continue;
            if (npc.getType() != NPCType.SCHOOL_KID && npc.getType() != NPCType.YOUTH_GANG) continue;
            float dist = npc.getPosition().dst(player.getPosition());
            if (dist <= 15f) {
                String line = lines[random.nextInt(lines.length)];
                npc.setSpeechText(line, 3.0f);
            }
        }
    }

    // ── Skater NPC management ─────────────────────────────────────────────────

    private void spawnSkaterNpcs(World world, NPCManager npcManager) {
        if (skatersSpawned || npcManager == null || world == null) return;
        Landmark park = world.getLandmark(LandmarkType.SKATE_PARK);
        if (park == null) return;

        skatersSpawned = true;
        float px = park.getPosition().x;
        float pz = park.getPosition().z;
        int pw = park.getWidth();
        int pd = park.getDepth();

        // Spawn 3–6 SCHOOL_KID NPCs inside park
        int kidCount = SCHOOL_KID_MIN + random.nextInt(SCHOOL_KID_MAX - SCHOOL_KID_MIN + 1);
        for (int i = 0; i < kidCount; i++) {
            float x = px + 1 + random.nextInt(Math.max(1, pw - 2));
            float z = pz + 1 + random.nextInt(Math.max(1, pd - 2));
            NPC kid = npcManager.spawnNPC(NPCType.SCHOOL_KID, x, 1f, z);
            if (kid != null) {
                skaterNpcs.add(kid);
            }
        }

        // Spawn 1–2 YOUTH_GANG NPCs at perimeter
        int gangCount = YOUTH_GANG_MIN + random.nextInt(YOUTH_GANG_MAX - YOUTH_GANG_MIN + 1);
        for (int i = 0; i < gangCount; i++) {
            float x = px + pw / 2f + (i == 0 ? -1 : 1);
            float z = pz - 1f;
            NPC gang = npcManager.spawnNPC(NPCType.YOUTH_GANG, x, 1f, z);
            if (gang != null) {
                skaterNpcs.add(gang);
            }
        }
    }

    private void despawnSkaterNpcs() {
        for (NPC npc : skaterNpcs) {
            if (npc.isAlive()) {
                npc.setState(NPCState.FLEEING);
            }
        }
        skaterNpcs.clear();
        skatersSpawned = false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isInPark(Player player, Landmark park) {
        float px = player.getPosition().x;
        float pz = player.getPosition().z;
        float lx = park.getPosition().x;
        float lz = park.getPosition().z;
        return px >= lx && px < lx + park.getWidth()
                && pz >= lz && pz < lz + park.getDepth();
    }

    private float getPlayerSpeed(Player player) {
        float vx = player.getVelocity().x;
        float vz = player.getVelocity().z;
        return (float) Math.sqrt(vx * vx + vz * vz);
    }

    private boolean isHoldingSkateboard(Inventory inventory, int hotbarSlot) {
        if (inventory == null) return false;
        Material held = inventory.getItemInSlot(hotbarSlot);
        return held == Material.SKATEBOARD;
    }

    private void updateRank() {
        for (int r = MAX_RANK; r >= 0; r--) {
            if (successfulTricks >= RANK_UNLOCK_THRESHOLDS[r]) {
                skillRank = r;
                return;
            }
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Current skill rank (0–4). */
    public int getSkillRank() { return skillRank; }

    /** Total successful tricks. */
    public int getSuccessfulTricks() { return successfulTricks; }

    /** Accumulated session score. */
    public int getSessionScore() { return sessionScore; }

    /** Result of the last trick attempt. */
    public TrickResult getLastTrickResult() { return lastTrickResult; }

    /** Whether a closure event is currently active. */
    public boolean isClosureEventActive() { return closureEventActive; }

    /** Whether the park is currently locked out. */
    public boolean isParkLocked() { return parkLocked; }

    /** Remaining lockout timer (real seconds). */
    public float getLockedOutTimer() { return lockedOutTimer; }

    /** Whether the player is currently park-banned (ASBO). */
    public boolean isPlayerBanned() { return playerBanned; }

    /** Remaining ban timer (real seconds). */
    public float getBanTimer() { return banTimer; }

    /** Total closure notices torn down. */
    public int getClosureNoticesTornDown() { return closureNoticesTornDown; }

    /** Total ASBOs received. */
    public int getAsboCount() { return asboCount; }

    /** Tricks completed in this session. */
    public int getTricksThisSession() { return tricksThisSession; }

    /** Whether skater NPCs are currently spawned. */
    public boolean isSkatersSpawned() { return skatersSpawned; }

    /** Current skater NPCs managed by this system. */
    public List<NPC> getSkaterNpcs() { return skaterNpcs; }

    /** The active closure notice prop index (-1 if none). */
    public int getClosureNoticePropIndex() { return closureNoticePropIndex; }

    /** The active COUNCIL_MEMBER NPC for the enforcement event (null if none). */
    public NPC getCouncilMemberNpc() { return councilMemberNpc; }

    /** Hours accumulated since last enforcement check. */
    public float getHoursSinceLastEnforcementCheck() { return hoursSinceLastEnforcementCheck; }

    // ── Force-set methods for testing ─────────────────────────────────────────

    /** Force-set skill rank (for testing). */
    public void setSkillRankForTesting(int rank) {
        this.skillRank = Math.max(0, Math.min(MAX_RANK, rank));
        // Also set successfulTricks to the threshold for that rank
        this.successfulTricks = RANK_UNLOCK_THRESHOLDS[this.skillRank];
    }

    /** Force-set trick cooldown timer (for testing). */
    public void setTrickCooldownForTesting(float seconds) {
        this.trickCooldownTimer = seconds;
    }

    /** Force-set player speed by modifying a speed override (tests call setVelocity). */
    public void setHoursSinceLastEnforcementCheckForTesting(float hours) {
        this.hoursSinceLastEnforcementCheck = hours;
    }

    /** Force-reset session state for testing. */
    public void resetSessionForTesting() {
        sessionScore = 0;
        coinsConverted = 0;
        respectConverted = 0;
        successfulTricks = 0;
        tricksThisSession = 0;
        kickflipKingThisSession = false;
        skillRank = 0;
        trickCooldownTimer = 0f;
        lastTrickResult = null;
    }
}

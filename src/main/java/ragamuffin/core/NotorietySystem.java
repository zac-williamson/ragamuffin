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
import ragamuffin.world.LandmarkType;
import ragamuffin.world.PropType;
import ragamuffin.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Phase 8e: Street Legend — Notoriety, Criminal Career Progression &
 * Escalating Police Response.
 *
 * <p>Tracks a persistent 0–1000 notoriety score that maps to 5 tiers:
 * <ul>
 *   <li>Tier 0 (Nobody):             0–99   — PCSOs ignore you</li>
 *   <li>Tier 1 (Local Nuisance):     100–249 — PCSOs warn; Police patrol last crime location</li>
 *   <li>Tier 2 (Neighbourhood Villain): 250–499 — Armed Response after heists</li>
 *   <li>Tier 3 (Area Menace):        500–749 — ARU on patrol; helicopter searchlight</li>
 *   <li>Tier 4 (Urban Legend):       750–999 — Surveillance van; can recruit accomplice</li>
 *   <li>Tier 5 (The Ragamuffin):     1000    — Wanted posters; doubled police</li>
 * </ul>
 *
 * <p>Notoriety only rises (one-way ratchet). It can be reduced by −5 per tier via
 * bribery (20 coins to the Fence) or by spending a night in the cells (arrest).
 */
public class NotorietySystem {

    // ── Tier thresholds ──────────────────────────────────────────────────────

    public static final int TIER_1_THRESHOLD = 100;
    public static final int TIER_2_THRESHOLD = 250;
    public static final int TIER_3_THRESHOLD = 500;
    public static final int TIER_4_THRESHOLD = 750;
    public static final int TIER_5_THRESHOLD = 1000;
    public static final int MAX_NOTORIETY    = 1000;

    // ── Gain values ──────────────────────────────────────────────────────────

    /** Breaking a block inside someone's building. */
    public static final int GAIN_BLOCK_BREAK = 2;

    /** Successfully pickpocketing an NPC. */
    public static final int GAIN_PICKPOCKET_SUCCESS = 5;

    /** Pickpocket attempt that fails (you still tried). */
    public static final int GAIN_PICKPOCKET_FAILURE = 2;

    /** Completing any heist. */
    public static final int GAIN_HEIST_COMPLETE = 30;

    /** Completing the jeweller heist (on top of GAIN_HEIST_COMPLETE). */
    public static final int GAIN_JEWELLER_HEIST_BONUS = 20; // 30+20 = 50 total

    /** Hitting a POLICE NPC. */
    public static final int GAIN_HIT_POLICE = 20;

    /** Escaping from arrest (not currently triggered — placeholder for future). */
    public static final int GAIN_ARREST_ESCAPE = 40;

    /** Completing a faction mission. */
    public static final int GAIN_FACTION_MISSION = 15;

    /** Reaching a faction Respect threshold ≥ 75. */
    public static final int GAIN_FACTION_THRESHOLD = 10;

    /** Being mentioned in a rumour that reaches the Barman (once per rumour). */
    public static final int GAIN_RUMOUR_MENTION = 5;

    // ── Reduction values ─────────────────────────────────────────────────────

    /** Bribery cost in COIN per reduction event. */
    public static final int BRIBE_COST_COIN = 20;

    /** Notoriety reduction per bribe. */
    public static final int BRIBE_REDUCTION = 5;

    // ── Pickpocket constants ─────────────────────────────────────────────────

    /** Base pickpocket success chance (0–1). */
    public static final float PICKPOCKET_BASE_CHANCE = 0.70f;

    /** Penalty when noise > 0.3. */
    public static final float PICKPOCKET_NOISE_PENALTY = 0.20f;

    /** Penalty when a witness NPC is within 6 blocks. */
    public static final float PICKPOCKET_WITNESS_PENALTY = 0.30f;

    /** Bonus in fog weather. */
    public static final float PICKPOCKET_FOG_BONUS = 0.20f;

    /** Noise threshold above which the noise penalty applies. */
    public static final float PICKPOCKET_NOISE_THRESHOLD = 0.3f;

    /** Distance within which an NPC acts as a witness. */
    public static final float PICKPOCKET_WITNESS_RANGE = 6f;

    /** Max distance behind NPC to initiate pickpocket. */
    public static final float PICKPOCKET_RANGE = 1.5f;

    /** Pickpocket per-NPC cooldown in real seconds (1 in-game hour). */
    public static final float PICKPOCKET_COOLDOWN_SECONDS = 50f; // ~1 in-game hour

    // ── Accomplice constants ─────────────────────────────────────────────────

    /** Tier required to recruit an accomplice. */
    public static final int ACCOMPLICE_TIER_REQUIRED = 4;

    /** Coin cost to recruit an accomplice. */
    public static final int ACCOMPLICE_COST_COIN = 10;

    /** Reduced safe-crack time when accomplice is present. */
    public static final float ACCOMPLICE_SAFE_CRACK_TIME = 5f;

    /** Number of police hits before accomplice flees. */
    public static final int ACCOMPLICE_MAX_POLICE_HITS = 3;

    // ── Helicopter constants ─────────────────────────────────────────────────

    /** Interval between helicopter passes in real seconds (5 in-game minutes). */
    public static final float HELICOPTER_INTERVAL_SECONDS = 5f * 50f; // ~250s real
    // (1 in-game minute ≈ 50s real at standard time speed)

    /** Duration of helicopter searchlight sweep in real seconds. */
    public static final float HELICOPTER_SWEEP_DURATION = 20f;

    // ── PCSO constants ───────────────────────────────────────────────────────

    /** How long (real seconds) the PCSO warning is active before calling police. */
    public static final float PCSO_WARNING_WINDOW = 60f;

    // ── State ─────────────────────────────────────────────────────────────────

    private int notoriety = 0;
    private int tier = 0;

    /** Whether a tier-up animation flash is pending. */
    private boolean tierUpPending = false;

    /** Timer for tier-up flash animation (counts down). */
    private float tierUpFlashTimer = 0f;

    /** Duration of the tier-up flash. */
    private static final float TIER_UP_FLASH_DURATION = 2.0f;

    /** Accomplice NPC reference (null if none recruited). */
    private NPC accomplice = null;

    /** Number of times police have hit the accomplice. */
    private int accomplicePoliceHits = 0;

    /** Number of times the fence has been bribed. */
    private int briberCount = 0;

    /** How many ARU escapes the player has survived consecutively. */
    private int consecutiveAruEscapes = 0;

    /** Whether the helicopter searchlight is currently active. */
    private boolean helicopterActive = false;

    /** Countdown until next helicopter pass (real seconds). */
    private float helicopterTimer = 0f;

    /** How long the current helicopter sweep has been running. */
    private float helicopterSweepTimer = 0f;

    /** Position of the helicopter searchlight (2D centre at ground level). */
    private float helicopterLightX = 0f;
    private float helicopterLightZ = 0f;

    /** Whether Tier 5 RAGAMUFFIN achievement has been awarded yet. */
    private boolean ragamuffinAchieved = false;

    /** Whether the wanted poster for Tier 5 has been placed. */
    private boolean wantedPosterPlaced = false;

    private final Random random;

    // ── Construction ──────────────────────────────────────────────────────────

    public NotorietySystem() {
        this(new Random());
    }

    public NotorietySystem(Random random) {
        this.random = random;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update the notoriety system. Call once per frame.
     *
     * @param delta   seconds since last frame
     * @param player  the player
     * @param achievementSystem callback for awarding achievements (may be null)
     */
    public void update(float delta, Player player, AchievementCallback achievementSystem) {
        // Tier-up flash
        if (tierUpFlashTimer > 0f) {
            tierUpFlashTimer = Math.max(0f, tierUpFlashTimer - delta);
            if (tierUpFlashTimer <= 0f) {
                tierUpPending = false;
            }
        }

        // Helicopter timer (only at Tier 3+)
        if (tier >= 3) {
            if (!helicopterActive) {
                helicopterTimer -= delta;
                if (helicopterTimer <= 0f) {
                    helicopterActive = true;
                    helicopterSweepTimer = HELICOPTER_SWEEP_DURATION;
                }
            } else {
                helicopterSweepTimer -= delta;
                if (helicopterSweepTimer <= 0f) {
                    helicopterActive = false;
                    helicopterTimer = HELICOPTER_INTERVAL_SECONDS;
                }
            }
        }
    }

    // ── Notoriety mutations ───────────────────────────────────────────────────

    /**
     * Add notoriety points. Capped at MAX_NOTORIETY. Fires tier-up if threshold crossed.
     *
     * @param amount amount to add (positive integer)
     * @param achievementSystem callback for achievements (may be null)
     * @return true if a tier-up occurred
     */
    public boolean addNotoriety(int amount, AchievementCallback achievementSystem) {
        if (amount <= 0) return false;
        int oldTier = tier;
        notoriety = Math.min(MAX_NOTORIETY, notoriety + amount);
        updateTier(achievementSystem);
        return tier > oldTier;
    }

    /**
     * Record a block-break crime.
     */
    public boolean onBlockBreak(AchievementCallback achievementSystem) {
        return addNotoriety(GAIN_BLOCK_BREAK, achievementSystem);
    }

    /**
     * Record hitting a POLICE NPC.
     */
    public boolean onHitPolice(AchievementCallback achievementSystem) {
        return addNotoriety(GAIN_HIT_POLICE, achievementSystem);
    }

    /**
     * Record a completed heist.
     *
     * @param isJeweller true if the jeweller was the target (+50 total)
     */
    public boolean onHeistComplete(boolean isJeweller, AchievementCallback achievementSystem) {
        int gain = GAIN_HEIST_COMPLETE + (isJeweller ? GAIN_JEWELLER_HEIST_BONUS : 0);
        return addNotoriety(gain, achievementSystem);
    }

    /**
     * Record completing a faction mission.
     */
    public boolean onFactionMission(AchievementCallback achievementSystem) {
        return addNotoriety(GAIN_FACTION_MISSION, achievementSystem);
    }

    /**
     * Record reaching a faction respect threshold ≥ 75.
     */
    public boolean onFactionThreshold(AchievementCallback achievementSystem) {
        return addNotoriety(GAIN_FACTION_THRESHOLD, achievementSystem);
    }

    /**
     * Record a rumour mention that reached the Barman.
     */
    public boolean onRumourMention(AchievementCallback achievementSystem) {
        return addNotoriety(GAIN_RUMOUR_MENTION, achievementSystem);
    }

    /**
     * Attempt to bribe the Fence to reduce notoriety. Costs 20 COIN.
     *
     * @param inventory the player's inventory
     * @param achievementSystem callback for achievements (may be null)
     * @return true if the bribe succeeded
     */
    public boolean bribeFence(Inventory inventory, AchievementCallback achievementSystem) {
        if (inventory.getItemCount(Material.COIN) < BRIBE_COST_COIN) return false;
        inventory.removeItem(Material.COIN, BRIBE_COST_COIN);
        notoriety = Math.max(0, notoriety - BRIBE_REDUCTION);
        updateTier(achievementSystem); // tier may go down
        briberCount++;
        if (achievementSystem != null && briberCount >= 3) {
            achievementSystem.award(AchievementType.KEEPING_IT_QUIET);
        }
        return true;
    }

    // ── Pickpocketing ────────────────────────────────────────────────────────

    /**
     * Attempt to pickpocket the given NPC. Handles success/failure logic,
     * notoriety, cooldown, and tooltips.
     *
     * @param target          the NPC to pickpocket
     * @param player          the player
     * @param playerInventory the player's inventory
     * @param noiseLevel      current noise level (0–1)
     * @param allNpcs         all nearby NPCs (for witness check)
     * @param isFog           whether current weather is fog
     * @param tooltipSystem   tooltip system for first-success message (may be null)
     * @param criminalRecord  the player's criminal record
     * @param achievementSystem callback for achievements (may be null)
     * @return PickpocketResult describing what happened
     */
    public PickpocketResult attemptPickpocket(
            NPC target,
            Player player,
            Inventory playerInventory,
            float noiseLevel,
            List<NPC> allNpcs,
            boolean isFog,
            CriminalRecord criminalRecord,
            AchievementCallback achievementSystem) {

        // Validate position: must be behind the NPC
        if (!isPlayerBehindNpc(player, target)) {
            return PickpocketResult.NOT_BEHIND;
        }

        // Check cooldown
        if (target.isPickpocketCooldown()) {
            return PickpocketResult.ON_COOLDOWN;
        }

        // Calculate success chance
        float chance = PICKPOCKET_BASE_CHANCE;
        if (noiseLevel > PICKPOCKET_NOISE_THRESHOLD) {
            chance -= PICKPOCKET_NOISE_PENALTY;
        }
        if (hasWitness(player, target, allNpcs)) {
            chance -= PICKPOCKET_WITNESS_PENALTY;
        }
        if (isFog) {
            chance += PICKPOCKET_FOG_BONUS;
        }
        chance = Math.max(0f, Math.min(1f, chance));

        boolean success = random.nextFloat() < chance;

        if (success) {
            // Yield 1–4 coins
            int coins = 1 + random.nextInt(4);
            playerInventory.addItem(Material.COIN, coins);

            // Set cooldown on target NPC
            target.setPickpocketCooldown(PICKPOCKET_COOLDOWN_SECONDS);

            // Notoriety gain
            addNotoriety(GAIN_PICKPOCKET_SUCCESS, achievementSystem);

            // Achievement
            if (achievementSystem != null) {
                achievementSystem.award(AchievementType.FIRST_PICKPOCKET);
            }

            return new PickpocketResult(true, coins, false, false);
        } else {
            // Failure — NPC turns and flees to police
            target.setState(NPCState.FLEEING);
            target.setSpeechText("Oi!", 3.0f);

            // Criminal record offence
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.ASSAULT); // closest existing type
            }

            // Notoriety gain (you tried)
            addNotoriety(GAIN_PICKPOCKET_FAILURE, achievementSystem);

            return new PickpocketResult(false, 0, true, false);
        }
    }

    /**
     * Checks whether the player is behind the NPC (not in the NPC's forward arc).
     * Uses dot product: player-to-NPC direction vs NPC facing direction.
     * If the dot product is positive, the player is in the NPC's forward arc.
     */
    private boolean isPlayerBehindNpc(Player player, NPC target) {
        // Distance check
        float dx = player.getPosition().x - target.getPosition().x;
        float dz = player.getPosition().z - target.getPosition().z;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (dist > PICKPOCKET_RANGE) {
            return false;
        }

        // NPC facing direction from facingAngle (degrees, 0=+Z, 90=+X)
        float angleRad = (float) Math.toRadians(target.getFacingAngle());
        float npcForwardX = (float) Math.sin(angleRad);
        float npcForwardZ = (float) Math.cos(angleRad);

        // Vector from NPC to player, normalised
        if (dist < 0.001f) return true; // same position — treat as behind
        float toPlayerX = dx / dist;
        float toPlayerZ = dz / dist;

        // Dot product: positive means player is in front of NPC, negative = behind
        float dot = toPlayerX * npcForwardX + toPlayerZ * npcForwardZ;
        return dot <= 0f; // player is in NPC's rear arc
    }

    /**
     * Returns true if any NPC (other than target) is within PICKPOCKET_WITNESS_RANGE
     * of the player.
     */
    private boolean hasWitness(Player player, NPC target, List<NPC> allNpcs) {
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            if (npc == target) continue;
            float dist = npc.getPosition().dst(player.getPosition());
            if (dist <= PICKPOCKET_WITNESS_RANGE) {
                return true;
            }
        }
        return false;
    }

    // ── Accomplice ────────────────────────────────────────────────────────────

    /**
     * Attempt to recruit an NPC as a permanent accomplice.
     * Requires Tier 4+, ≥ 10 coins, and the NPC must be a STREET_LAD or YOUTH_GANG type.
     *
     * @param npc             the NPC to recruit
     * @param inventory       the player's inventory
     * @param achievementSystem callback for achievements (may be null)
     * @return true if recruitment succeeded
     */
    public boolean recruitAccomplice(NPC npc, Inventory inventory, AchievementCallback achievementSystem) {
        if (tier < ACCOMPLICE_TIER_REQUIRED) return false;
        if (accomplice != null && accomplice.isAlive()) return false; // already have one
        if (inventory.getItemCount(Material.COIN) < ACCOMPLICE_COST_COIN) return false;
        if (npc.getType() != NPCType.YOUTH_GANG && npc.getType() != NPCType.STREET_LAD) return false;

        inventory.removeItem(Material.COIN, ACCOMPLICE_COST_COIN);
        this.accomplice = npc;
        this.accomplicePoliceHits = 0;
        npc.setState(NPCState.FOLLOWING);
        npc.setSpeechText("Don't get attached.", 3.0f);
        return true;
    }

    /**
     * Called when the accomplice is hit by police.
     * After ACCOMPLICE_MAX_POLICE_HITS hits, the accomplice flees and is lost.
     *
     * @return true if the accomplice was lost
     */
    public boolean onAccompliceHitByPolice() {
        if (accomplice == null) return false;
        accomplicePoliceHits++;
        if (accomplicePoliceHits >= ACCOMPLICE_MAX_POLICE_HITS) {
            accomplice.setState(NPCState.FLEEING);
            accomplice.setSpeechText("Every man for himself, mate!", 4.0f);
            accomplice = null;
            accomplicePoliceHits = 0;
            return true;
        }
        return false;
    }

    /**
     * Called when a heist succeeds with an accomplice present.
     */
    public void onHeistSuccessWithAccomplice(AchievementCallback achievementSystem) {
        if (achievementSystem != null && accomplice != null) {
            achievementSystem.award(AchievementType.THE_CREW);
        }
    }

    // ── PCSO spawning ─────────────────────────────────────────────────────────

    /**
     * Spawn PCSOs in response to a crime. At Tier 0–1, PCSOs issue warnings.
     * Returns the PCSO NPC that was spawned (or null if none was spawned).
     *
     * @param crimeX       X coordinate of the crime
     * @param crimeZ       Z coordinate of the crime
     * @param playerY      Y coordinate of the player (for spawn height)
     * @param npcManager   the NPC manager (for spawning)
     * @return list of spawned PCSO NPCs
     */
    public List<NPC> spawnPcsosForCrime(float crimeX, float crimeZ, float playerY, NPCManager npcManager) {
        List<NPC> spawned = new ArrayList<>();
        if (tier > 1) return spawned; // above Tier 1, regular police handle it
        if (npcManager == null) return spawned;

        int count = 1 + random.nextInt(2); // 1–2 PCSOs
        for (int i = 0; i < count; i++) {
            float angle = (float) (random.nextFloat() * Math.PI * 2);
            float dist = 5f + random.nextFloat() * 5f;
            float x = crimeX + (float) Math.cos(angle) * dist;
            float z = crimeZ + (float) Math.sin(angle) * dist;
            NPC pcso = npcManager.spawnNPC(NPCType.PCSO, x, playerY, z);
            if (pcso != null) {
                pcso.setState(NPCState.WARNING);
                pcso.setSpeechText("Oi! Pack it in!", 4.0f);
                spawned.add(pcso);
            }
        }
        return spawned;
    }

    // ── ARU spawning ──────────────────────────────────────────────────────────

    /**
     * Spawn ARU units in response to a crime (Tier 3+).
     *
     * @param crimeX     X coordinate of the crime
     * @param crimeZ     Z coordinate of the crime
     * @param playerY    Y coordinate of the player
     * @param npcManager the NPC manager
     * @return list of spawned ARU NPCs
     */
    public List<NPC> spawnAruForCrime(float crimeX, float crimeZ, float playerY, NPCManager npcManager) {
        List<NPC> spawned = new ArrayList<>();
        if (tier < 3) return spawned;
        if (npcManager == null) return spawned;

        int count = 1 + random.nextInt(2); // 1–2 ARU
        for (int i = 0; i < count; i++) {
            float angle = (float) (random.nextFloat() * Math.PI * 2);
            float dist = 8f + random.nextFloat() * 5f;
            float x = crimeX + (float) Math.cos(angle) * dist;
            float z = crimeZ + (float) Math.sin(angle) * dist;
            NPC aru = npcManager.spawnNPC(NPCType.ARMED_RESPONSE, x, playerY, z);
            if (aru != null) {
                aru.setState(NPCState.AGGRESSIVE);
                aru.setSpeechText("Armed police! On the ground!", 4.0f);
                spawned.add(aru);
            }
        }
        return spawned;
    }

    // ── Wanted poster placement ───────────────────────────────────────────────

    /**
     * At Tier 5, check if wanted posters should be placed and seed rumours.
     * Called by the game loop when notoriety reaches 1000.
     *
     * @param npcManager    the NPC manager
     * @param rumourNetwork the rumour network
     * @param allNpcs       all NPCs
     */
    public void checkWantedPosters(NPCManager npcManager, RumourNetwork rumourNetwork, List<NPC> allNpcs) {
        if (tier < 5 || wantedPosterPlaced) return;
        wantedPosterPlaced = true;

        // Seed rumour about wanted poster
        if (rumourNetwork != null && allNpcs != null) {
            for (NPC npc : allNpcs) {
                if (!npc.isAlive()) continue;
                if (npc.getType() == NPCType.PUBLIC || npc.getType() == NPCType.BARMAN) {
                    rumourNetwork.addRumour(npc, new Rumour(RumourType.PLAYER_SPOTTED,
                            "Have you seen the wanted poster by the off-licence? Proper scary."));
                }
            }
        }
    }

    // ── Safe-crack time ───────────────────────────────────────────────────────

    /**
     * Returns the safe-crack hold time in seconds, reduced if an accomplice is present.
     *
     * @param baseCrackTime the default safe-crack time (from HeistSystem)
     * @return effective safe-crack time
     */
    public float getEffectiveSafeCrackTime(float baseCrackTime) {
        if (accomplice != null && accomplice.isAlive()) {
            return ACCOMPLICE_SAFE_CRACK_TIME;
        }
        return baseCrackTime;
    }

    // ── Tier computation ──────────────────────────────────────────────────────

    private void updateTier(AchievementCallback achievementSystem) {
        int oldTier = tier;
        tier = computeTier(notoriety);
        if (tier > oldTier) {
            tierUpPending = true;
            tierUpFlashTimer = TIER_UP_FLASH_DURATION;

            // Awards
            if (achievementSystem != null) {
                if (tier == 1) {
                    achievementSystem.award(AchievementType.LOCAL_NUISANCE);
                }
                if (tier == 5 && !ragamuffinAchieved) {
                    ragamuffinAchieved = true;
                    achievementSystem.award(AchievementType.RAGAMUFFIN);
                }
            }

            // Reset helicopter timer when entering Tier 3
            if (tier == 3 && oldTier < 3) {
                helicopterTimer = HELICOPTER_INTERVAL_SECONDS;
            }
        }
    }

    /**
     * Compute tier from a notoriety value (pure function, no side effects).
     */
    public static int computeTier(int notoriety) {
        if (notoriety >= TIER_5_THRESHOLD) return 5;
        if (notoriety >= TIER_4_THRESHOLD) return 4;
        if (notoriety >= TIER_3_THRESHOLD) return 3;
        if (notoriety >= TIER_2_THRESHOLD) return 2;
        if (notoriety >= TIER_1_THRESHOLD) return 1;
        return 0;
    }

    /**
     * Title text for a given tier.
     */
    public static String getTierTitle(int tier) {
        switch (tier) {
            case 0: return "Nobody";
            case 1: return "Local Nuisance";
            case 2: return "Neighbourhood Villain";
            case 3: return "Area Menace";
            case 4: return "Urban Legend";
            case 5: return "The Ragamuffin";
            default: return "Nobody";
        }
    }

    // ── Fence pricing ────────────────────────────────────────────────────────

    /**
     * Apply the notoriety-tier fence price multiplier to a base value.
     * Tier 1+ gives 10% better prices at the fence.
     *
     * @param baseValue the base sell value
     * @return adjusted value (integer, rounded down)
     */
    public int applyFencePriceBonus(int baseValue) {
        if (tier >= 1) {
            return (int) (baseValue * 1.10f);
        }
        return baseValue;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Current notoriety score (0–1000). */
    public int getNotoriety() {
        return notoriety;
    }

    /** Current Street Legend tier (0–5). */
    public int getTier() {
        return tier;
    }

    /** Current tier title string. */
    public String getTierTitle() {
        return getTierTitle(tier);
    }

    /** Whether a tier-up flash is pending. */
    public boolean isTierUpPending() {
        return tierUpPending;
    }

    /** Remaining tier-up flash timer. */
    public float getTierUpFlashTimer() {
        return tierUpFlashTimer;
    }

    /** Whether the helicopter searchlight is currently active. */
    public boolean isHelicopterActive() {
        return helicopterActive;
    }

    /** Remaining helicopter sweep time. */
    public float getHelicopterSweepTimer() {
        return helicopterSweepTimer;
    }

    /** Helicopter searchlight X position. */
    public float getHelicopterLightX() {
        return helicopterLightX;
    }

    /** Helicopter searchlight Z position. */
    public float getHelicopterLightZ() {
        return helicopterLightZ;
    }

    /** Set helicopter searchlight position. */
    public void setHelicopterLightPosition(float x, float z) {
        this.helicopterLightX = x;
        this.helicopterLightZ = z;
    }

    /** The current accomplice NPC, or null. */
    public NPC getAccomplice() {
        return accomplice;
    }

    /** Whether the player has an active accomplice. */
    public boolean hasAccomplice() {
        return accomplice != null && accomplice.isAlive();
    }

    /** Whether the wanted poster for Tier 5 has been placed. */
    public boolean isWantedPosterPlaced() {
        return wantedPosterPlaced;
    }

    /** Number of times bribed (for KEEPING_IT_QUIET achievement tracking). */
    public int getBriberCount() {
        return briberCount;
    }

    /** Consecutive ARU escapes (for SURVIVED_ARU achievement). */
    public int getConsecutiveAruEscapes() {
        return consecutiveAruEscapes;
    }

    /** Increment ARU escape counter. Awards SURVIVED_ARU at 3. */
    public void onEscapeFromAru(AchievementCallback achievementSystem) {
        consecutiveAruEscapes++;
        if (consecutiveAruEscapes >= 3 && achievementSystem != null) {
            achievementSystem.award(AchievementType.SURVIVED_ARU);
        }
    }

    /** Reset ARU escape counter (called when player is caught). */
    public void resetAruEscapes() {
        consecutiveAruEscapes = 0;
    }

    // ── Force-set for testing ──────────────────────────────────────────────────

    /** Force-set notoriety (for testing). Also recomputes tier. */
    public void setNotorietyForTesting(int value) {
        this.notoriety = Math.max(0, Math.min(MAX_NOTORIETY, value));
        this.tier = computeTier(this.notoriety);
    }

    /** Force-set the accomplice (for testing). */
    public void setAccompliceForTesting(NPC npc) {
        this.accomplice = npc;
        this.accomplicePoliceHits = 0;
    }

    // ── Nested types ──────────────────────────────────────────────────────────

    /**
     * Result of a pickpocket attempt.
     */
    public static class PickpocketResult {

        public static final PickpocketResult NOT_BEHIND = new PickpocketResult(false, 0, false, true);
        public static final PickpocketResult ON_COOLDOWN = new PickpocketResult(false, 0, false, false);

        public final boolean success;
        public final int coinsGained;
        public final boolean npcAlerted;
        public final boolean notBehind;

        public PickpocketResult(boolean success, int coinsGained, boolean npcAlerted, boolean notBehind) {
            this.success = success;
            this.coinsGained = coinsGained;
            this.npcAlerted = npcAlerted;
            this.notBehind = notBehind;
        }
    }

    /**
     * Callback interface for awarding achievements.
     */
    public interface AchievementCallback {
        void award(AchievementType type);
    }
}

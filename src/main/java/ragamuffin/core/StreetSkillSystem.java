package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Issue #787: Street Skills &amp; Character Progression — 'Learning the Hard Way'
 *
 * <p>A persistent six-skill system that rewards the player for specialising in a
 * playstyle. Skills are earned automatically from existing game actions. Each skill
 * has five tiers with concrete perks that feed back into gameplay.
 *
 * <h3>Skills</h3>
 * <ul>
 *   <li><b>BRAWLING</b>   — combat: punching NPCs, surviving fights</li>
 *   <li><b>GRAFTING</b>   — scavenging: breaking blocks, collecting resources</li>
 *   <li><b>TRADING</b>    — economy: stall sales, fence deals, street economy</li>
 *   <li><b>STEALTH</b>    — evasion: crouching, escaping police, pick-pocketing</li>
 *   <li><b>INFLUENCE</b>  — social: talking to NPCs, rumours, MC battles</li>
 *   <li><b>SURVIVAL</b>   — endurance: staying alive, managing stats</li>
 * </ul>
 *
 * <h3>Tiers</h3>
 * Novice (0) → Apprentice (1) → Journeyman (2) → Expert (3) → Legend (4)
 *
 * <h3>Perks by Tier</h3>
 *
 * <b>BRAWLING</b>
 * <ul>
 *   <li>Novice      — baseline</li>
 *   <li>Apprentice  — +10% punch damage</li>
 *   <li>Journeyman  — enemies stagger on 3rd consecutive hit (0.5s pause)</li>
 *   <li>Expert      — unlock combo counter: 5-hit chain deals 2× final blow</li>
 *   <li>Legend      — crowd-stagger: every punch staggers all hostiles within 3 blocks</li>
 * </ul>
 *
 * <b>GRAFTING</b>
 * <ul>
 *   <li>Novice      — baseline</li>
 *   <li>Apprentice  — +1 extra drop from block breaks (50% chance)</li>
 *   <li>Journeyman  — LEAVES blocks can be eaten as BITTER_GREENS emergency food</li>
 *   <li>Expert      — Soft blocks break in 3 hits instead of 5</li>
 *   <li>Legend      — 25% chance to not consume tool durability on block break</li>
 * </ul>
 *
 * <b>TRADING</b>
 * <ul>
 *   <li>Novice      — baseline</li>
 *   <li>Apprentice  — +10% stall sale income</li>
 *   <li>Journeyman  — Fence buy price +15%</li>
 *   <li>Expert      — Black market price-drop immunity (ignores COUNCIL_CRACKDOWN)</li>
 *   <li>Legend      — Halves faction protection cut on market stall</li>
 * </ul>
 *
 * <b>STEALTH</b>
 * <ul>
 *   <li>Novice      — baseline</li>
 *   <li>Apprentice  — Noise while crouching reduced by 50%</li>
 *   <li>Journeyman  — Police sight range reduced by 20% while crouching</li>
 *   <li>Expert      — Unlock pick-pocketing (press E behind unaware NPC)</li>
 *   <li>Legend      — Become invisible to cameras while crouching</li>
 * </ul>
 *
 * <b>INFLUENCE</b>
 * <ul>
 *   <li>Novice      — baseline</li>
 *   <li>Apprentice  — Rumour spread speed +25%</li>
 *   <li>Journeyman  — NPC bribery costs -20%</li>
 *   <li>Expert      — Unlock RALLY (press G to assemble PUBLIC followers)</li>
 *   <li>Legend      — RALLY mob also deters GANG members (not just police)</li>
 * </ul>
 *
 * <b>SURVIVAL</b>
 * <ul>
 *   <li>Novice      — baseline</li>
 *   <li>Apprentice  — Hunger drain -20%</li>
 *   <li>Journeyman  — Fall damage -25%</li>
 *   <li>Expert      — Start respawn with 50% health instead of 25%</li>
 *   <li>Legend      — Trigger NEIGHBOURHOOD_EVENT at any location</li>
 * </ul>
 */
public class StreetSkillSystem {

    // ── Skill enum ────────────────────────────────────────────────────────────

    public enum Skill {
        BRAWLING, GRAFTING, TRADING, STEALTH, INFLUENCE, SURVIVAL
    }

    // ── Tier enum ─────────────────────────────────────────────────────────────

    public enum Tier {
        NOVICE(0, "Novice"),
        APPRENTICE(1, "Apprentice"),
        JOURNEYMAN(2, "Journeyman"),
        EXPERT(3, "Expert"),
        LEGEND(4, "Legend");

        private final int level;
        private final String label;

        Tier(int level, String label) {
            this.level = level;
            this.label = label;
        }

        public int getLevel() { return level; }
        public String getLabel() { return label; }

        /** Get the next tier, or null if already Legend. */
        public Tier next() {
            if (this == LEGEND) return null;
            return values()[level + 1];
        }
    }

    // ── XP thresholds for each tier ───────────────────────────────────────────

    /** XP required to enter each tier (indexed by Tier.level). */
    public static final int[] TIER_XP_THRESHOLDS = { 0, 100, 300, 700, 1500 };

    // ── XP awards for actions ─────────────────────────────────────────────────

    public static final int XP_PUNCH_HIT         = 2;    // BRAWLING: hit landed on NPC
    public static final int XP_FIGHT_WIN         = 20;   // BRAWLING: NPC defeated
    public static final int XP_TAKE_DAMAGE       = 1;    // BRAWLING: survive damage
    public static final int XP_BLOCK_BREAK       = 1;    // GRAFTING: break a block
    public static final int XP_COLLECT_RESOURCE  = 2;    // GRAFTING: pick up dropped item
    public static final int XP_STALL_SALE        = 5;    // TRADING: complete stall sale
    public static final int XP_FENCE_DEAL        = 8;    // TRADING: fence a stolen item
    public static final int XP_STREET_DEAL       = 3;    // TRADING: street economy sale
    public static final int XP_CROUCH_ESCAPE     = 5;    // STEALTH: escape while crouching
    public static final int XP_EVADE_POLICE      = 10;   // STEALTH: lose police pursuit
    public static final int XP_PICKPOCKET        = 15;   // STEALTH: successful pick-pocket
    public static final int XP_TALK_NPC          = 2;    // INFLUENCE: talk to NPC
    public static final int XP_RUMOUR_SPREAD     = 3;    // INFLUENCE: rumour propagated
    public static final int XP_MC_BATTLE_WIN     = 25;   // INFLUENCE: win an MC battle
    public static final int XP_SURVIVE_DAMAGE    = 3;    // SURVIVAL: survive dangerous health
    public static final int XP_SURVIVE_HUNGER    = 2;    // SURVIVAL: recover from near-starvation
    public static final int XP_DAY_SURVIVED      = 20;   // SURVIVAL: survive a full in-game day

    // ── Pick-pocket constants ─────────────────────────────────────────────────

    /** Base success probability for pick-pocketing. */
    public static final float PICKPOCKET_BASE_CHANCE   = 0.35f;
    /** Bonus probability per STEALTH tier above EXPERT. */
    public static final float PICKPOCKET_TIER_BONUS    = 0.10f;
    /** Bonus probability when crouching during pick-pocket. */
    public static final float PICKPOCKET_CROUCH_BONUS  = 0.20f;
    /** Penalty per witness NPC within 6 blocks. */
    public static final float PICKPOCKET_WITNESS_PENALTY = 0.15f;
    /** Maximum range to attempt pick-pocketing (blocks). */
    public static final float PICKPOCKET_RANGE         = 2.5f;
    /** NPC must not have seen the player recently (seconds). */
    public static final float PICKPOCKET_UNAWARE_WINDOW = 3.0f;

    // ── RALLY constants ───────────────────────────────────────────────────────

    /** Radius to recruit PUBLIC followers when RALLY is used. */
    public static final float RALLY_RECRUIT_RADIUS     = 15f;
    /** Duration of RALLY effect in seconds. */
    public static final float RALLY_DURATION           = 120f;
    /** Maximum number of followers. */
    public static final int   RALLY_MAX_FOLLOWERS      = 6;
    /** Cooldown between RALLY uses in seconds. */
    public static final float RALLY_COOLDOWN           = 300f;
    /** Police deter radius per follower. */
    public static final float RALLY_DETER_RADIUS       = 5f;

    // ── NEIGHBOURHOOD_EVENT constants ────────────────────────────────────────

    public enum NeighbourhoodEventType { FLASH_RAVE, FLASH_MARKET, MASS_BRAWL }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Map<Skill, Integer> xp;
    private final Map<Skill, Tier> tiers;

    private float rallyTimer    = 0f;   // remaining RALLY duration (>0 = active)
    private float rallyCooldown = 0f;   // cooldown until next RALLY
    private final List<NPC> followers  = new ArrayList<>();

    private final Random random;
    private AchievementSystem achievementSystem;
    private RaveSystem raveSystem;
    private StallSystem stallSystem;
    private FactionSystem factionSystem;
    private WantedSystem wantedSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public StreetSkillSystem() {
        this(new Random());
    }

    public StreetSkillSystem(Random random) {
        this.random = random;
        this.xp    = new EnumMap<>(Skill.class);
        this.tiers = new EnumMap<>(Skill.class);
        for (Skill s : Skill.values()) {
            xp.put(s, 0);
            tiers.put(s, Tier.NOVICE);
        }
    }

    // ── Dependency injection ──────────────────────────────────────────────────

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setRaveSystem(RaveSystem raveSystem) {
        this.raveSystem = raveSystem;
    }

    public void setStallSystem(StallSystem stallSystem) {
        this.stallSystem = stallSystem;
    }

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    // ── XP & tier management ─────────────────────────────────────────────────

    /**
     * Award XP to a skill. May trigger one or more tier-ups.
     *
     * @param skill the skill to award XP to
     * @param amount the amount of XP to add
     */
    public void awardXP(Skill skill, int amount) {
        int current = xp.get(skill) + amount;
        xp.put(skill, current);
        checkTierUp(skill);
    }

    private void checkTierUp(Skill skill) {
        Tier current = tiers.get(skill);
        Tier next = current.next();
        while (next != null && xp.get(skill) >= TIER_XP_THRESHOLDS[next.getLevel()]) {
            tiers.put(skill, next);
            onTierUp(skill, next);
            current = next;
            next = current.next();
        }
    }

    private void onTierUp(Skill skill, Tier newTier) {
        if (achievementSystem == null) return;
        if (newTier == Tier.LEGEND) {
            switch (skill) {
                case BRAWLING:  achievementSystem.unlock(AchievementType.PROPER_HARD);        break;
                case GRAFTING:  achievementSystem.unlock(AchievementType.GRAFTER);            break;
                case TRADING:   achievementSystem.unlock(AchievementType.WHEELERDEALER);      break;
                case STEALTH:   achievementSystem.unlock(AchievementType.GHOST);              break;
                case INFLUENCE: achievementSystem.unlock(AchievementType.WORDS_ON_THE_STREET); break;
                case SURVIVAL:  achievementSystem.unlock(AchievementType.LEGEND_OF_THE_MANOR); break;
            }
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int getXP(Skill skill) { return xp.get(skill); }

    public Tier getTier(Skill skill) { return tiers.get(skill); }

    public int getTierLevel(Skill skill) { return tiers.get(skill).getLevel(); }

    /**
     * XP required to reach the next tier, or 0 if already Legend.
     */
    public int getXPToNextTier(Skill skill) {
        Tier current = tiers.get(skill);
        Tier next = current.next();
        if (next == null) return 0;
        return TIER_XP_THRESHOLDS[next.getLevel()] - xp.get(skill);
    }

    // ── Perk queries ─────────────────────────────────────────────────────────

    /** BRAWLING Apprentice+: +10% punch damage. */
    public float getPunchDamageMultiplier() {
        return getTierLevel(Skill.BRAWLING) >= Tier.APPRENTICE.getLevel() ? 1.10f : 1.0f;
    }

    /** BRAWLING Journeyman+: enemies stagger on 3rd consecutive hit. */
    public boolean canStaggerOnCombo() {
        return getTierLevel(Skill.BRAWLING) >= Tier.JOURNEYMAN.getLevel();
    }

    /** BRAWLING Legend: crowd-stagger on every punch. */
    public boolean hasCrowdStagger() {
        return getTierLevel(Skill.BRAWLING) >= Tier.LEGEND.getLevel();
    }

    /** GRAFTING Journeyman+: can eat LEAVES as BITTER_GREENS. */
    public boolean canEatBitterGreens() {
        return getTierLevel(Skill.GRAFTING) >= Tier.JOURNEYMAN.getLevel();
    }

    /** GRAFTING Expert+: soft blocks break in 3 hits instead of 5. */
    public boolean hasSoftBlockReduction() {
        return getTierLevel(Skill.GRAFTING) >= Tier.EXPERT.getLevel();
    }

    /** TRADING Apprentice+: +10% stall income multiplier. */
    public float getStallIncomeMultiplier() {
        return getTierLevel(Skill.TRADING) >= Tier.APPRENTICE.getLevel() ? 1.10f : 1.0f;
    }

    /** TRADING Legend: halves faction protection cut on market stall. */
    public float getFactionProtectionCutMultiplier() {
        return getTierLevel(Skill.TRADING) >= Tier.LEGEND.getLevel() ? 0.5f : 1.0f;
    }

    /** STEALTH Apprentice+: 50% crouch noise reduction. */
    public float getCrouchNoiseMultiplier() {
        return getTierLevel(Skill.STEALTH) >= Tier.APPRENTICE.getLevel() ? 0.5f : 1.0f;
    }

    /** STEALTH Expert+: pick-pocketing unlocked. */
    public boolean canPickpocket() {
        return getTierLevel(Skill.STEALTH) >= Tier.EXPERT.getLevel();
    }

    /** INFLUENCE Expert+: RALLY mechanic unlocked. */
    public boolean canRally() {
        return getTierLevel(Skill.INFLUENCE) >= Tier.EXPERT.getLevel();
    }

    /** INFLUENCE Legend: RALLY also deters gang members. */
    public boolean rallyDetersGangs() {
        return getTierLevel(Skill.INFLUENCE) >= Tier.LEGEND.getLevel();
    }

    /** SURVIVAL Journeyman+: fall damage -25%. */
    public float getFallDamageMultiplier() {
        return getTierLevel(Skill.SURVIVAL) >= Tier.JOURNEYMAN.getLevel() ? 0.75f : 1.0f;
    }

    /** SURVIVAL Expert+: respawn at 50% health instead of 25%. */
    public boolean hasImprovedRespawn() {
        return getTierLevel(Skill.SURVIVAL) >= Tier.EXPERT.getLevel();
    }

    /** SURVIVAL Legend: NEIGHBOURHOOD_EVENT unlocked. */
    public boolean canTriggerNeighbourhoodEvent() {
        return getTierLevel(Skill.SURVIVAL) >= Tier.LEGEND.getLevel();
    }

    // ── Pick-pocketing ────────────────────────────────────────────────────────

    /** Result of a pick-pocket attempt. */
    public enum PickpocketResult {
        SUCCESS,
        FAILED_CAUGHT,     // NPC noticed; may become hostile
        FAILED_UNAWARE,    // Not behind unaware NPC
        SKILL_LOCKED,      // STEALTH not at Expert tier
        NPC_EMPTY,         // NPC has nothing to steal
        OUT_OF_RANGE       // Player not close enough
    }

    /**
     * Attempt to pick-pocket a target NPC.
     *
     * <p>Requirements:
     * <ul>
     *   <li>STEALTH Expert or above</li>
     *   <li>Player within {@link #PICKPOCKET_RANGE} blocks</li>
     *   <li>Target NPC not in WITNESS, AGGRESSIVE, ATTACKING, or POLICE states</li>
     * </ul>
     *
     * @param target      the NPC to pick-pocket
     * @param player      the player
     * @param inventory   the player's inventory
     * @param witnesses   all NPCs in the world (used to count witnesses)
     * @return a PickpocketResult
     */
    public PickpocketResult tryPickpocket(NPC target, Player player, Inventory inventory,
                                          List<NPC> witnesses) {
        if (!canPickpocket()) {
            return PickpocketResult.SKILL_LOCKED;
        }

        float dist = target.getPosition().dst(player.getPosition());
        if (dist > PICKPOCKET_RANGE) {
            return PickpocketResult.OUT_OF_RANGE;
        }

        // Target must be unaware (WANDERING or IDLE)
        NPCState state = target.getState();
        if (state != NPCState.WANDERING && state != NPCState.IDLE) {
            return PickpocketResult.FAILED_UNAWARE;
        }

        // NPC must have loot (use COIN as a proxy for "has something")
        // Any NPC that drops COIN on death is considered to have pickpocket loot
        if (!hasPickpocketLoot(target)) {
            return PickpocketResult.NPC_EMPTY;
        }

        // Calculate success probability
        float chance = PICKPOCKET_BASE_CHANCE;

        // STEALTH tier bonus (each tier above Expert adds 10%)
        int stealthTier = getTierLevel(Skill.STEALTH);
        if (stealthTier > Tier.EXPERT.getLevel()) {
            chance += PICKPOCKET_TIER_BONUS * (stealthTier - Tier.EXPERT.getLevel());
        }

        // Crouching bonus
        if (player.isCrouching()) {
            chance += PICKPOCKET_CROUCH_BONUS;
        }

        // Witness penalty
        int witnessCount = countWitnessesNearby(player, witnesses);
        chance -= witnessCount * PICKPOCKET_WITNESS_PENALTY;

        chance = Math.max(0.05f, Math.min(0.95f, chance));

        if (random.nextFloat() < chance) {
            // Success — steal one COIN from the NPC
            inventory.addItem(Material.COIN, 1);
            awardXP(Skill.STEALTH, XP_PICKPOCKET);
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.PICKPOCKET);
            }
            return PickpocketResult.SUCCESS;
        } else {
            // Caught — NPC becomes hostile
            target.setState(NPCState.AGGRESSIVE);
            target.setSpeechText("Oi! Get your hands out me pockets!", 4f);
            return PickpocketResult.FAILED_CAUGHT;
        }
    }

    private boolean hasPickpocketLoot(NPC npc) {
        // PUBLIC, DRUNK, PENSIONER, SCHOOL_KID, BUSKER all carry COIN
        NPCType type = npc.getType();
        return type == NPCType.PUBLIC
            || type == NPCType.DRUNK
            || type == NPCType.PENSIONER
            || type == NPCType.SCHOOL_KID
            || type == NPCType.BUSKER
            || type == NPCType.POSTMAN
            || type == NPCType.DELIVERY_DRIVER
            || type == NPCType.JOGGER;
    }

    private int countWitnessesNearby(Player player, List<NPC> npcs) {
        int count = 0;
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            if (npc.getState() == NPCState.WANDERING || npc.getState() == NPCState.IDLE) {
                float dist = npc.getPosition().dst(player.getPosition());
                if (dist <= 6f) {
                    count++;
                }
            }
        }
        return count;
    }

    // ── RALLY ─────────────────────────────────────────────────────────────────

    /** Result of a RALLY attempt. */
    public enum RallyResult {
        SUCCESS,
        SKILL_LOCKED,   // INFLUENCE not at Expert tier
        ON_COOLDOWN,    // RALLY is on cooldown
        NO_TARGETS      // No PUBLIC NPCs nearby
    }

    /**
     * Attempt to RALLY nearby PUBLIC NPCs as followers.
     *
     * @param player    the player
     * @param allNPCs   all NPCs in the world
     * @return a RallyResult
     */
    public RallyResult tryRally(Player player, List<NPC> allNPCs) {
        if (!canRally()) {
            return RallyResult.SKILL_LOCKED;
        }
        if (rallyCooldown > 0f) {
            return RallyResult.ON_COOLDOWN;
        }

        int recruited = 0;
        for (NPC npc : allNPCs) {
            if (!npc.isAlive()) continue;
            if (npc.getType() != NPCType.PUBLIC) continue;
            if (npc.getState() == NPCState.WITNESS
                    || npc.getState() == NPCState.AGGRESSIVE) continue;

            float dist = npc.getPosition().dst(player.getPosition());
            if (dist > RALLY_RECRUIT_RADIUS) continue;

            if (recruited < RALLY_MAX_FOLLOWERS) {
                // Convert to FOLLOWER type via state (we track them in the list)
                npc.setState(NPCState.FOLLOWING_PLAYER);
                followers.add(npc);
                npc.setSpeechText("Yeah, come on then!", 3f);
                recruited++;
            }
        }

        if (recruited == 0) {
            return RallyResult.NO_TARGETS;
        }

        rallyTimer = RALLY_DURATION;
        rallyCooldown = RALLY_COOLDOWN;

        awardXP(Skill.INFLUENCE, 10);

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.RALLY_CRY);
        }

        return RallyResult.SUCCESS;
    }

    /**
     * Update RALLY state each frame. Disperses followers when timer expires.
     * Also checks if followers are deterring police/gang NPCs nearby.
     *
     * @param delta   seconds since last frame
     * @param player  the player
     * @param allNPCs all NPCs in the world
     */
    public void update(float delta, Player player, List<NPC> allNPCs) {
        // Cooldown
        if (rallyCooldown > 0f) {
            rallyCooldown = Math.max(0f, rallyCooldown - delta);
        }

        // RALLY timer
        if (rallyTimer > 0f) {
            rallyTimer -= delta;
            if (rallyTimer <= 0f) {
                disperseFollowers();
            } else {
                // Deter hostile NPCs near followers
                applyFollowerDeterrence(allNPCs);
                // Remove dead/dispersed followers
                pruneFollowers();
            }
        }
    }

    private void disperseFollowers() {
        for (NPC npc : followers) {
            if (npc.isAlive() && npc.getState() == NPCState.FOLLOWING_PLAYER) {
                npc.setState(NPCState.WANDERING);
                npc.setSpeechText("Alright, I'm off then.", 3f);
            }
        }
        followers.clear();
        rallyTimer = 0f;
    }

    private void applyFollowerDeterrence(List<NPC> allNPCs) {
        for (NPC npc : allNPCs) {
            if (!npc.isAlive()) continue;
            boolean isPolice = (npc.getType() == NPCType.POLICE
                    || npc.getType() == NPCType.PCSO
                    || npc.getType() == NPCType.ARMED_RESPONSE);
            boolean isGang = (npc.getType() == NPCType.YOUTH_GANG
                    || npc.getType() == NPCType.STREET_LAD
                    || npc.getType() == NPCType.THUG);

            if (!isPolice && !(isGang && rallyDetersGangs())) continue;

            // Check if any follower is within deter radius
            for (NPC follower : followers) {
                if (!follower.isAlive()) continue;
                float dist = npc.getPosition().dst(follower.getPosition());
                if (dist <= RALLY_DETER_RADIUS) {
                    // Deter: make hostile NPC back off
                    if (npc.getState() == NPCState.AGGRESSIVE
                            || npc.getState() == NPCState.CHASING_PLAYER) {
                        npc.setState(NPCState.WANDERING);
                        npc.setSpeechText("Forget it, there's too many of them.", 3f);
                    }
                    break;
                }
            }
        }
    }

    private void pruneFollowers() {
        Iterator<NPC> it = followers.iterator();
        while (it.hasNext()) {
            NPC npc = it.next();
            if (!npc.isAlive() || npc.getState() != NPCState.FOLLOWING_PLAYER) {
                it.remove();
            }
        }
    }

    // ── NEIGHBOURHOOD_EVENT ───────────────────────────────────────────────────

    /**
     * Trigger a neighbourhood event at the player's current position.
     * Requires SURVIVAL Legend tier.
     *
     * @param type    the type of event to trigger
     * @param player  the player
     * @return true if the event was triggered successfully
     */
    public boolean triggerNeighbourhoodEvent(NeighbourhoodEventType type, Player player) {
        if (!canTriggerNeighbourhoodEvent()) return false;

        switch (type) {
            case FLASH_RAVE:
                if (raveSystem != null) {
                    // Seed a flash rave at the player's location
                    raveSystem.setFlashRaveActive(true);
                }
                break;
            case FLASH_MARKET:
                if (stallSystem != null) {
                    stallSystem.setFlashMarketActive(true);
                }
                break;
            case MASS_BRAWL:
                // Award combat XP spike as the brawl begins
                awardXP(Skill.BRAWLING, 10);
                break;
        }
        return true;
    }

    // ── Stall income modification ─────────────────────────────────────────────

    /**
     * Apply TRADING skill modifiers to a stall sale amount.
     *
     * @param baseAmount the base COIN amount from the sale
     * @return modified amount after TRADING perks
     */
    public int applyTradingPerkToSale(int baseAmount) {
        float mult = getStallIncomeMultiplier();
        return (int) (baseAmount * mult);
    }

    /**
     * Apply TRADING Legend perk to a faction protection cut.
     *
     * @param baseProtectionCut the base protection cut fraction (0–1)
     * @return modified protection cut
     */
    public float applyFactionProtectionCut(float baseProtectionCut) {
        return baseProtectionCut * getFactionProtectionCutMultiplier();
    }

    // ── Getters for UI ────────────────────────────────────────────────────────

    public boolean isRallyActive() { return rallyTimer > 0f; }
    public float getRallyTimer()   { return rallyTimer; }
    public float getRallyCooldown() { return rallyCooldown; }
    public List<NPC> getFollowers() { return followers; }
    public int getFollowerCount()  { return followers.size(); }

    /**
     * Set XP directly for a skill (for testing).
     */
    public void setXP(Skill skill, int amount) {
        xp.put(skill, Math.max(0, amount));
        // Recompute tier from scratch
        tiers.put(skill, Tier.NOVICE);
        checkTierUp(skill);
    }

    /**
     * Set the rally timer directly (for testing).
     */
    public void setRallyTimer(float timer) {
        this.rallyTimer = timer;
    }

    /**
     * Set the rally cooldown directly (for testing).
     */
    public void setRallyCooldown(float cooldown) {
        this.rallyCooldown = cooldown;
    }
}

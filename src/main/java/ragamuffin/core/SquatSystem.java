package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Phase Q: Player Squat — Base Building, Defence &amp; Social Prestige (Issue #714).
 *
 * <h3>Overview</h3>
 * The player can claim any derelict building (Condition ≤ 10) as a personal squat
 * by pressing <b>E</b> inside while holding 5 WOOD. No coin is required — this is
 * squatting, not buying. Only one squat may be active at a time.
 *
 * <h3>Vibe score (0–100)</h3>
 * Interior prestige score, analogous to Condition in {@link PropertySystem}.
 * Raised by placing furnishing props/blocks; decays 2 points/day without maintenance.
 *
 * <table border="1">
 *   <tr><th>Vibe</th><th>Label</th><th>Effects</th></tr>
 *   <tr><td>0–19</td><td>Dump</td><td>None</td></tr>
 *   <tr><td>20–39</td><td>Habitable</td><td>Passive health regen +1/min inside</td></tr>
 *   <tr><td>40–59</td><td>Decent Gaff</td><td>2× sleep energy recovery; barman rumour seeded</td></tr>
 *   <tr><td>60–79</td><td>Proper Nice</td><td>Lodger system unlocked; one faction +5 respect</td></tr>
 *   <tr><td>80–100</td><td>Legendary Squat</td><td>Notoriety +10; achievement; NPC street comments</td></tr>
 * </table>
 *
 * <h3>Lodger system</h3>
 * At Vibe ≥ 60, press E on a PUBLIC or STREET_LAD NPC to offer lodgings.
 * Acceptance: 70% base (+15% if bought them a drink). Max = floor(Vibe/20), capped at 4.
 * Each lodger earns 2 coins/day. Lodgers flee if Vibe drops below 20.
 *
 * <h3>Defence (raids)</h3>
 * At Notoriety Tier 2+, raids occur every 3 in-game days (25% base chance +10%/tier).
 * 1–3 THUGs spawn; destroy props (−5 Vibe each). BARRICADE blocks absorb 3 hits.
 * Lodgers fight back (50% chance). Pub BOUNCER can be hired for 5 coins/day.
 *
 * <h3>Crafting hub</h3>
 * Placing a WORKBENCH prop unlocks BARRICADE, LOCKPICK, and FAKE_ID recipes.
 *
 * <h3>New block types</h3>
 * CARPET (+2 Vibe/block), BARRICADE (3 hits to destroy, crafted from 2 WOOD + 1 BRICK).
 *
 * <h3>New achievements</h3>
 * SQUATTER, LEGENDARY_SQUAT, RUNNING_A_HOUSE, BOUNCER_ON_DOOR, BARRICADED_IN.
 */
public class SquatSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Number of WOOD required to claim a squat. */
    public static final int CLAIM_WOOD_COST = 5;

    /** Maximum condition of a building that can be claimed as a squat. */
    public static final int MAX_CLAIMABLE_CONDITION = 10;

    /** Maximum Vibe score. */
    public static final int MAX_VIBE = 100;

    /** Minimum Vibe score. */
    public static final int MIN_VIBE = 0;

    /** Vibe decay per in-game day without maintenance. */
    public static final int VIBE_DECAY_PER_DAY = 2;

    // Vibe tier thresholds
    public static final int VIBE_TIER_HABITABLE = 20;
    public static final int VIBE_TIER_DECENT     = 40;
    public static final int VIBE_TIER_PROPER     = 60;
    public static final int VIBE_TIER_LEGENDARY  = 80;

    /** Vibe below which lodgers flee. */
    public static final int LODGER_FLEE_VIBE = 20;

    /** Max number of lodgers (absolute cap). */
    public static final int MAX_LODGERS = 4;

    /** Coins earned per lodger per in-game day. */
    public static final int LODGER_DAILY_INCOME = 2;

    /** Base lodger acceptance chance (0–100). */
    public static final int LODGER_ACCEPT_BASE_CHANCE = 70;

    /** Bonus to acceptance if player bought them a drink. */
    public static final int LODGER_DRINK_BONUS = 15;

    /** Vibe lost per prop destroyed during a raid. */
    public static final int VIBE_LOSS_PER_PROP = 5;

    /** Base raid chance per eligible day (0–100). */
    public static final int RAID_BASE_CHANCE = 25;

    /** Additional raid chance per notoriety tier above 1. */
    public static final int RAID_CHANCE_PER_TIER = 10;

    /** Days between raid checks. */
    public static final int RAID_INTERVAL_DAYS = 3;

    /** Number of hits a BARRICADE block absorbs before breaking. */
    public static final int BARRICADE_DURABILITY = 3;

    /** Cost in coins/day to hire the bouncer. */
    public static final int BOUNCER_DAILY_COST = 5;

    /** Notoriety bonus for reaching Legendary Squat (Vibe 80+). */
    public static final int LEGENDARY_NOTORIETY_BONUS = 10;

    // Vibe gains per furnishing
    private static final Map<PropType, Integer> PROP_VIBE_GAIN;
    static {
        Map<PropType, Integer> m = new EnumMap<>(PropType.class);
        m.put(PropType.BED,             10);
        m.put(PropType.SQUAT_DARTBOARD,  7);
        m.put(PropType.WORKBENCH,        0);
        PROP_VIBE_GAIN = Collections.unmodifiableMap(m);
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** The landmark type of the claimed squat, null if none. */
    private LandmarkType squatLocation;

    /** World-space centre of the squat (approximate). */
    private int squatWorldX;
    private int squatWorldZ;

    /** Current Vibe score (0–100). */
    private int vibe;

    /** NPCs currently living in the squat as lodgers. */
    private final List<NPC> lodgers = new ArrayList<>();

    /** Whether a WORKBENCH prop has been placed inside. */
    private boolean hasWorkbench;

    /** Whether the bouncer has been hired. */
    private boolean bouncerHired;

    /** The bouncer NPC, if hired. */
    private NPC bouncerNpc;

    /** Furnishing tracker (prop type → count placed). */
    private final SquatFurnishingTracker furnishings = new SquatFurnishingTracker();

    /** Day counter used to process daily ticks. */
    private int lastProcessedDay;

    /** Whether the Legendary Squat notoriety bonus has already been awarded. */
    private boolean legendaryBonusAwarded;

    /** Whether the Proper Nice faction respect bonus has already been awarded. */
    private boolean properNiceFactionBonusAwarded;

    /** Number of raids survived without Vibe dropping below 60 (for BARRICADED_IN achievement). */
    private int raidsWithVibeAbove60;

    /** Day counter for tracking raid intervals. */
    private int lastRaidDay;

    /** Accumulated fractional lodger income (in case of partial-day calcs). */
    private float dayIncomeAccumulator;

    /** Tooltip message pending display (polled by the HUD). */
    private String pendingTooltip;

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final NotorietySystem notorietySystem;
    private final FactionSystem factionSystem;
    private final AchievementSystem achievementSystem;
    private final RumourNetwork rumourNetwork;
    private final Random random;

    // ── Constructor ───────────────────────────────────────────────────────────

    public SquatSystem(NotorietySystem notorietySystem,
                       FactionSystem factionSystem,
                       AchievementSystem achievementSystem,
                       RumourNetwork rumourNetwork,
                       Random random) {
        this.notorietySystem  = notorietySystem;
        this.factionSystem    = factionSystem;
        this.achievementSystem = achievementSystem;
        this.rumourNetwork    = rumourNetwork;
        this.random           = random;
    }

    // ── Claiming ──────────────────────────────────────────────────────────────

    /**
     * Attempt to claim a derelict building as the player's squat.
     *
     * <p>Preconditions:
     * <ol>
     *   <li>Player has no current squat (only one at a time).</li>
     *   <li>The target building has Condition ≤ {@link #MAX_CLAIMABLE_CONDITION}.</li>
     *   <li>Player has at least {@link #CLAIM_WOOD_COST} WOOD in their inventory.</li>
     * </ol>
     *
     * @param building       which landmark type is being claimed
     * @param buildingX      world-space X centre of the building
     * @param buildingZ      world-space Z centre of the building
     * @param buildingCondition current Condition of the building (0–100)
     * @param inventory      player inventory (WOOD will be consumed)
     * @param allNpcs        all living NPCs (for rumour seeding)
     * @return descriptive result message, or null if already squatting
     */
    public String claimSquat(LandmarkType building, int buildingX, int buildingZ,
                             int buildingCondition, Inventory inventory, List<NPC> allNpcs) {
        if (squatLocation != null) {
            return "You've already got a squat. You can only have one at a time.";
        }

        if (buildingCondition > MAX_CLAIMABLE_CONDITION) {
            return "This place is in too good a nick to squat. Find somewhere more derelict.";
        }

        if (inventory.getItemCount(Material.WOOD) < CLAIM_WOOD_COST) {
            return "You need " + CLAIM_WOOD_COST + " WOOD to board the place up and make it yours.";
        }

        inventory.removeItem(Material.WOOD, CLAIM_WOOD_COST);
        squatLocation = building;
        squatWorldX   = buildingX;
        squatWorldZ   = buildingZ;
        vibe          = 0;

        achievementSystem.unlock(AchievementType.SQUATTER);

        seedBarmanRumour("Someone's moved into that derelict place on the estate. Squatters.", allNpcs);

        pendingTooltip = "Claimed! This is your gaff now. Make it liveable.";
        return "You've squatted it. Board it up, make it cosy, watch your back.";
    }

    // ── Furnishing ────────────────────────────────────────────────────────────

    /**
     * Register a furnishing prop being placed inside the squat.
     * Increases Vibe by the prop's contribution and checks for tier crossings.
     *
     * @param prop    the prop type placed
     * @param allNpcs for barman rumour seeding on tier unlock
     * @return the Vibe gained, or 0 if unknown prop
     */
    public int furnish(PropType prop, List<NPC> allNpcs) {
        if (squatLocation == null) return 0;

        furnishings.addProp(prop);

        if (prop == PropType.WORKBENCH) {
            hasWorkbench = true;
        }

        int gain = PROP_VIBE_GAIN.getOrDefault(prop, 0);
        if (gain > 0) {
            setVibe(vibe + gain, allNpcs);
        }

        return gain;
    }

    /**
     * Register a CARPET block being placed inside the squat (+2 Vibe per block).
     *
     * @param allNpcs for barman rumour seeding on tier unlock
     * @return Vibe gained (2)
     */
    public int furnishCarpet(List<NPC> allNpcs) {
        if (squatLocation == null) return 0;
        furnishings.addCarpet();
        setVibe(vibe + 2, allNpcs);
        return 2;
    }

    /**
     * Register a CAMPFIRE block placed inside the squat (+8 Vibe).
     *
     * @param allNpcs for barman rumour seeding on tier unlock
     * @return Vibe gained (8)
     */
    public int furnishCampfire(List<NPC> allNpcs) {
        if (squatLocation == null) return 0;
        furnishings.addCampfire();
        setVibe(vibe + 8, allNpcs);
        return 8;
    }

    /**
     * Remove a furnishing prop, reducing Vibe by its contribution.
     *
     * @param prop    the prop type removed
     * @param allNpcs not used for rumours here, but kept consistent
     * @return the Vibe lost (positive number), or 0 if prop not tracked
     */
    public int removeFurnishing(PropType prop, List<NPC> allNpcs) {
        if (squatLocation == null) return 0;

        boolean removed = furnishings.removeProp(prop);
        if (!removed) return 0;

        if (prop == PropType.WORKBENCH && furnishings.getCount(PropType.WORKBENCH) == 0) {
            hasWorkbench = false;
        }

        int loss = PROP_VIBE_GAIN.getOrDefault(prop, 0);
        if (loss > 0) {
            setVibe(vibe - loss, allNpcs);
        }
        return loss;
    }

    // ── Lodger system ─────────────────────────────────────────────────────────

    /**
     * Attempt to invite an NPC to live in the squat as a lodger.
     *
     * <p>Requirements:
     * <ul>
     *   <li>Vibe ≥ 60</li>
     *   <li>NPC is PUBLIC or STREET_LAD</li>
     *   <li>Lodger count < floor(Vibe/20), capped at 4</li>
     *   <li>NPC is not already a lodger</li>
     * </ul>
     *
     * @param npc            the NPC to invite
     * @param boughtDrink    whether the player previously bought this NPC a drink
     * @return descriptive result message
     */
    public String inviteLodger(NPC npc, boolean boughtDrink) {
        if (squatLocation == null) return "You don't have a squat to invite anyone to.";
        if (vibe < VIBE_TIER_PROPER) {
            return "Your place isn't nice enough to have lodgers. Raise the Vibe to 60 first.";
        }

        if (npc.getType() != NPCType.PUBLIC && npc.getType() != NPCType.STREET_LAD) {
            return "That sort of person wouldn't want to doss at yours.";
        }

        if (lodgers.contains(npc)) {
            return "They're already living here.";
        }

        int maxLodgers = Math.min(MAX_LODGERS, vibe / 20);
        if (lodgers.size() >= maxLodgers) {
            if (lodgers.size() >= MAX_LODGERS) {
                return "It's a squat, not a hotel.";
            }
            return "You can't fit any more lodgers until you raise the Vibe more.";
        }

        int chance = LODGER_ACCEPT_BASE_CHANCE + (boughtDrink ? LODGER_DRINK_BONUS : 0);
        if (random.nextInt(100) >= chance) {
            return "They reckon your gaff looks a bit grim. Maybe later.";
        }

        lodgers.add(npc);
        npc.setState(NPCState.FOLLOWING);
        npc.setSpeechText("Yeah alright, I'll kip here for a bit.", 5f);

        if (lodgers.size() == MAX_LODGERS) {
            achievementSystem.unlock(AchievementType.RUNNING_A_HOUSE);
        }

        return "They move in. Lodger count: " + lodgers.size() + ". Each earns you 2 coins a day.";
    }

    /**
     * Evict a lodger from the squat.
     *
     * @param npc the lodger NPC to evict
     * @return result message, or null if NPC is not a lodger
     */
    public String evictLodger(NPC npc) {
        if (!lodgers.remove(npc)) {
            return null;
        }
        npc.setState(NPCState.WANDERING);
        npc.setSpeechText("Fair enough. Cheers for having me.", 5f);
        return "They gather their stuff and leave. Still friendly about it, all things considered.";
    }

    // ── Raid system ───────────────────────────────────────────────────────────

    /**
     * Determine if a raid should trigger on the given day, based on notoriety tier.
     *
     * @param currentDay current in-game day count
     * @param notorietyTier current notoriety tier (0–5)
     * @return true if a raid should occur
     */
    public boolean shouldRaid(int currentDay, int notorietyTier) {
        if (squatLocation == null) return false;
        if (notorietyTier < 2) return false;
        if (currentDay - lastRaidDay < RAID_INTERVAL_DAYS) return false;

        int chance = RAID_BASE_CHANCE + (notorietyTier - 1) * RAID_CHANCE_PER_TIER;
        return random.nextInt(100) < chance;
    }

    /**
     * Execute a raid: spawn 1–3 THUG NPCs at the squat entrance, destroy props,
     * allow lodgers to fight back, and apply Vibe penalties.
     *
     * @param currentDay     current in-game day
     * @param notorietyTier  current notoriety tier
     * @param allNpcs        the full NPC list (THUGs will be added)
     * @return list of event messages from the raid
     */
    public List<String> executeRaid(int currentDay, int notorietyTier, List<NPC> allNpcs) {
        List<String> messages = new ArrayList<>();
        if (squatLocation == null) return messages;

        lastRaidDay = currentDay;

        int vibeBeforeRaid = vibe;

        // Spawn 1–3 THUGs
        int thugsCount = 1 + random.nextInt(3);
        for (int i = 0; i < thugsCount; i++) {
            NPC thug = new NPC(NPCType.THUG, "raider_" + currentDay + "_" + i,
                    squatWorldX + i * 2f, 1f, squatWorldZ + 1f);
            thug.setState(NPCState.AGGRESSIVE);
            allNpcs.add(thug);
        }
        messages.add(thugsCount + " thugs are trying to ransack your gaff!");

        // If bouncer is hired and alive, they try to block
        if (bouncerHired && bouncerNpc != null && bouncerNpc.isAlive()) {
            bouncerNpc.setState(NPCState.ATTACKING_PLAYER); // repurposed: bouncer attacks thug
            messages.add("Your bouncer steps in to deal with them.");
            // Bouncer absorbs 1 thug (reduce effective count)
            thugsCount = Math.max(0, thugsCount - 1);
        }

        // Lodgers fight back (50% chance each)
        for (NPC lodger : lodgers) {
            if (lodger.isAlive() && random.nextBoolean()) {
                lodger.setState(NPCState.ATTACKING_PLAYER); // repurposed: lodger attacks thug
                lodger.setSpeechText("Oi! This is our gaff!", 5f);
                messages.add(lodger.getName() != null ? lodger.getName() : "A lodger" +
                        " fights back: \"Oi! This is our gaff!\"");
                thugsCount = Math.max(0, thugsCount - 1);
            }
        }

        // Each remaining thug destroys a prop
        for (int i = 0; i < thugsCount; i++) {
            PropType destroyed = furnishings.destroyRandomProp(random);
            if (destroyed != null) {
                setVibe(vibe - VIBE_LOSS_PER_PROP, allNpcs);
                messages.add("A thug smashes your " + destroyed.name().toLowerCase().replace("_", " ") +
                        ". Vibe −5.");
            }
        }

        // Track BARRICADED_IN achievement
        if (vibe >= VIBE_TIER_PROPER) {
            raidsWithVibeAbove60++;
            if (raidsWithVibeAbove60 >= 3) {
                achievementSystem.unlock(AchievementType.BARRICADED_IN);
            }
        }

        if (vibe >= vibeBeforeRaid - VIBE_LOSS_PER_PROP || vibeBeforeRaid == vibe) {
            messages.add("You repelled the raid! Your gaff stands.");
        } else {
            messages.add("The raid took its toll. Vibe is now " + vibe + ".");
        }

        return messages;
    }

    // ── Bouncer hiring ────────────────────────────────────────────────────────

    /**
     * Hire the pub BOUNCER NPC to guard the squat door.
     *
     * @param bouncerNpc the BOUNCER NPC (from the pub)
     * @param inventory  player inventory (5 coins/day cost checked here for first day)
     * @return result message, or null if preconditions not met
     */
    public String hireBouncer(NPC bouncerNpc, Inventory inventory) {
        if (squatLocation == null) return "You don't have a squat for them to guard.";
        if (bouncerNpc.getType() != NPCType.BOUNCER) {
            return "That's not the bouncer.";
        }
        if (inventory.getItemCount(Material.COIN) < BOUNCER_DAILY_COST) {
            return "You need at least " + BOUNCER_DAILY_COST + " coins to hire the bouncer.";
        }

        inventory.removeItem(Material.COIN, BOUNCER_DAILY_COST);
        this.bouncerNpc = bouncerNpc;
        this.bouncerHired = true;
        bouncerNpc.setState(NPCState.IDLE);

        achievementSystem.unlock(AchievementType.BOUNCER_ON_DOOR);
        pendingTooltip = "Bouncer hired. Professional.";
        return "The bouncer cracks their knuckles and takes up position at the door.";
    }

    // ── Daily tick ────────────────────────────────────────────────────────────

    /**
     * Called once per in-game day to apply Vibe decay, collect lodger income,
     * pay bouncer, and check for raid triggers.
     *
     * @param currentDay    the current in-game day count
     * @param notorietyTier the player's current notoriety tier
     * @param inventory     player's inventory (income deposited here)
     * @param allNpcs       all living NPCs (for raid and rumour logic)
     * @return list of event messages (may be empty, never null)
     */
    public List<String> onDayTick(int currentDay, int notorietyTier,
                                   Inventory inventory, List<NPC> allNpcs) {
        if (squatLocation == null) return Collections.emptyList();
        if (currentDay <= lastProcessedDay) return Collections.emptyList();

        List<String> messages = new ArrayList<>();

        // 1. Vibe decay
        setVibe(vibe - VIBE_DECAY_PER_DAY, allNpcs);
        if (vibe < VIBE_TIER_HABITABLE) {
            messages.add("Your squat is getting grim. Raise the Vibe before it falls apart.");
        }

        // 2. Lodger fleeing at Vibe < 20
        if (vibe < LODGER_FLEE_VIBE && !lodgers.isEmpty()) {
            for (NPC lodger : lodgers) {
                lodger.setState(NPCState.WANDERING);
                lodger.setSpeechText("This place has gone proper grim. I'm off.", 5f);
            }
            int fled = lodgers.size();
            lodgers.clear();
            messages.add(fled + " lodger(s) left. They said: \"This place has gone proper grim.\"");
        }

        // 3. Lodger income
        int totalLodgerIncome = lodgers.size() * LODGER_DAILY_INCOME;
        if (totalLodgerIncome > 0) {
            inventory.addItem(Material.COIN, totalLodgerIncome);
            messages.add("Lodger income: +" + totalLodgerIncome + " coins.");
        }

        // 4. Bouncer daily cost
        if (bouncerHired && bouncerNpc != null && bouncerNpc.isAlive()) {
            if (inventory.getItemCount(Material.COIN) >= BOUNCER_DAILY_COST) {
                inventory.removeItem(Material.COIN, BOUNCER_DAILY_COST);
            } else {
                bouncerHired = false;
                bouncerNpc.setState(NPCState.WANDERING);
                bouncerNpc.setSpeechText("Oi, where's my money?", 5f);
                messages.add("Bouncer walked off — you couldn't pay them.");
            }
        }

        // 5. Raid check
        if (shouldRaid(currentDay, notorietyTier)) {
            messages.addAll(executeRaid(currentDay, notorietyTier, allNpcs));
        }

        lastProcessedDay = currentDay;
        return messages;
    }

    // ── Vibe management ───────────────────────────────────────────────────────

    /**
     * Set the Vibe score, clamped to 0–100.
     * Fires tier-crossing logic (rumours, achievements, notoriety bonuses).
     *
     * @param newVibe the new Vibe score (will be clamped)
     * @param allNpcs for barman rumour seeding
     */
    private void setVibe(int newVibe, List<NPC> allNpcs) {
        int oldTier = vibeTier(vibe);
        vibe = Math.max(MIN_VIBE, Math.min(MAX_VIBE, newVibe));
        int newTier = vibeTier(vibe);

        if (newTier > oldTier) {
            onVibeTierUp(newTier, allNpcs);
        }
    }

    private void onVibeTierUp(int tier, List<NPC> allNpcs) {
        switch (tier) {
            case 2: // Decent Gaff (40+)
                seedBarmanRumour("They've got a proper little setup going at that squat.", allNpcs);
                break;
            case 3: // Proper Nice (60+)
                if (!properNiceFactionBonusAwarded) {
                    properNiceFactionBonusAwarded = true;
                    // Award respect bonus to one faction (first one, conceptually the closest)
                    if (factionSystem != null) {
                        factionSystem.applyRespectDelta(ragamuffin.core.Faction.STREET_LADS, 5);
                    }
                }
                seedBarmanRumour("Heard their gaff is proper nice now. Lodgers and everything.", allNpcs);
                break;
            case 4: // Legendary (80+)
                if (!legendaryBonusAwarded) {
                    legendaryBonusAwarded = true;
                    if (notorietySystem != null) {
                        notorietySystem.addNotoriety(LEGENDARY_NOTORIETY_BONUS,
                                achievementSystem::unlock);
                    }
                    achievementSystem.unlock(AchievementType.LEGENDARY_SQUAT);
                    seedBarmanRumour("Heard your gaff is well posh. Proper Legendary Squat, mate.", allNpcs);
                    pendingTooltip = "Legendary Squat! NPCs on the street are talking about your gaff.";
                }
                break;
            default:
                break;
        }
    }

    /**
     * Convert a Vibe score to a tier (0–4).
     */
    public static int vibeTier(int vibeScore) {
        if (vibeScore >= VIBE_TIER_LEGENDARY) return 4;
        if (vibeScore >= VIBE_TIER_PROPER)    return 3;
        if (vibeScore >= VIBE_TIER_DECENT)    return 2;
        if (vibeScore >= VIBE_TIER_HABITABLE) return 1;
        return 0;
    }

    /**
     * Returns a label for the current Vibe tier.
     */
    public static String getVibeTierLabel(int vibeScore) {
        switch (vibeTier(vibeScore)) {
            case 4: return "Legendary Squat";
            case 3: return "Proper Nice";
            case 2: return "Decent Gaff";
            case 1: return "Habitable";
            default: return "Dump";
        }
    }

    // ── Health regen query ────────────────────────────────────────────────────

    /**
     * Returns true if the player is inside the squat and Vibe is high enough
     * to grant passive health regen (Vibe ≥ 20).
     *
     * @param playerX player's world X
     * @param playerZ player's world Z
     * @param radius  proximity radius to consider "inside"
     * @return true if passive health regen is active
     */
    public boolean isRegenActive(float playerX, float playerZ, float radius) {
        if (squatLocation == null) return false;
        if (vibe < VIBE_TIER_HABITABLE) return false;

        float dx = Math.abs(playerX - squatWorldX);
        float dz = Math.abs(playerZ - squatWorldZ);
        return dx <= radius && dz <= radius;
    }

    // ── Workbench query ───────────────────────────────────────────────────────

    /**
     * Returns true if a WORKBENCH prop has been placed inside the squat,
     * unlocking advanced crafting recipes.
     */
    public boolean hasWorkbench() {
        return hasWorkbench;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Returns the currently claimed squat's landmark type, or null if none. */
    public LandmarkType getSquatLocation() {
        return squatLocation;
    }

    /** Returns true if the player has an active squat. */
    public boolean hasSquat() {
        return squatLocation != null;
    }

    /** Returns the current Vibe score (0–100). */
    public int getVibe() {
        return vibe;
    }

    /**
     * Directly set the Vibe score (test helper / cheat). Fires tier logic.
     *
     * @param newVibe the desired Vibe (0–100, clamped)
     * @param allNpcs for barman rumours (pass empty list in tests)
     */
    public void setVibeDirectly(int newVibe, List<NPC> allNpcs) {
        setVibe(newVibe, allNpcs);
    }

    /** Returns the current number of lodgers. */
    public int getLodgerCount() {
        return lodgers.size();
    }

    /** Returns an unmodifiable view of the lodger list. */
    public List<NPC> getLodgers() {
        return Collections.unmodifiableList(lodgers);
    }

    /** Returns the world X centre of the squat. */
    public int getSquatWorldX() {
        return squatWorldX;
    }

    /** Returns the world Z centre of the squat. */
    public int getSquatWorldZ() {
        return squatWorldZ;
    }

    /** Returns true if the bouncer has been hired. */
    public boolean isBouncerHired() {
        return bouncerHired;
    }

    /** Returns the bouncer NPC, or null if not hired. */
    public NPC getBouncerNpc() {
        return bouncerNpc;
    }

    /** Returns the furnishings tracker. */
    public SquatFurnishingTracker getFurnishings() {
        return furnishings;
    }

    /** Returns and clears the pending tooltip, or null if none. */
    public String pollTooltip() {
        String tip = pendingTooltip;
        pendingTooltip = null;
        return tip;
    }

    /** Returns the maximum lodger capacity given current Vibe. */
    public int getMaxLodgers() {
        return Math.min(MAX_LODGERS, vibe / 20);
    }

    /** Returns the number of raids survived with Vibe above 60. */
    public int getRaidsWithVibeAbove60() {
        return raidsWithVibeAbove60;
    }

    /**
     * Daily tick — convenience wrapper used by the game loop.
     * Delegates to {@link #onDayTick(int, int, Inventory, List)}.
     *
     * @param dayIndex      current in-game day index (from {@code TimeSystem.getDayIndex()})
     * @param notorietyTier the player's current notoriety tier
     * @param allNpcs       all living NPCs
     * @param inventory     player's inventory (income deposited here)
     */
    public void tickDay(int dayIndex, int notorietyTier, List<NPC> allNpcs, Inventory inventory) {
        onDayTick(dayIndex, notorietyTier, inventory, allNpcs);
    }

    /**
     * Count the number of NPCs currently inside the squat (used by RaveSystem).
     * Returns 0 if no squat has been claimed.
     * Uses a simple bounding-box check: any NPC within 15 blocks of the squat centre.
     *
     * @param allNpcs all living NPCs
     * @return number of NPCs near the squat entrance
     */
    public int countAttendeesInSquat(List<NPC> allNpcs) {
        if (squatLocation == null || allNpcs == null) return 0;
        int count = 0;
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            float dx = npc.getPosition().x - squatWorldX;
            float dz = npc.getPosition().z - squatWorldZ;
            if (dx * dx + dz * dz <= 15f * 15f) {
                count++;
            }
        }
        return count;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void seedBarmanRumour(String text, List<NPC> allNpcs) {
        if (rumourNetwork == null || allNpcs == null) return;
        Rumour rumour = new Rumour(RumourType.GANG_ACTIVITY, text);
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            if (npc.getType() == NPCType.BARMAN) {
                rumourNetwork.addRumour(npc, rumour);
                return;
            }
        }
        // No barman found — seed first available NPC
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            rumourNetwork.addRumour(npc, rumour);
            return;
        }
    }
}

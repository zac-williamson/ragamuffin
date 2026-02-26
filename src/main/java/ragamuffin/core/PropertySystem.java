package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Slumlord property economy (Issue #712 / Phase P).
 *
 * <h3>Overview</h3>
 * The player can buy buildings from the weekday-only {@link NPCType#ESTATE_AGENT} NPC,
 * repair them with BRICK or WOOD to raise a Condition score (0–100), and collect
 * passive coin income scaled to Condition.  Owned properties decay daily without
 * maintenance, losing tenants and eventually reverting to derelict if neglected.
 *
 * <h3>Rival faction takeovers</h3>
 * Rival factions send {@link NPCType#THUG} NPCs to stand outside player buildings.
 * The player must punch them off or use an {@link Material#EVICTION_NOTICE} item.
 *
 * <h3>Council rates</h3>
 * The council charges weekly rates per owned building; non-payment triggers Planning
 * Notices and, after two missed weeks, compulsory purchase (building reverts).
 *
 * <h3>Property cap</h3>
 * The player may own at most {@link #MAX_PROPERTIES} buildings.  On reaching the cap,
 * a flavour tooltip is shown: "You're not Thatcher. Five properties is enough."
 *
 * <h3>Faction integration</h3>
 * Buying a faction-owned building drops that faction's Respect by
 * {@link #FACTION_PURCHASE_RESPECT_PENALTY}.
 */
public class PropertySystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Maximum number of buildings the player may own simultaneously. */
    public static final int MAX_PROPERTIES = 5;

    /** Condition score for a freshly purchased building. */
    public static final int INITIAL_CONDITION = 30;

    /** Condition score for a fully repaired building. */
    public static final int MAX_CONDITION = 100;

    /** Minimum condition before the building is considered derelict. */
    public static final int DERELICT_THRESHOLD = 10;

    /**
     * Passive coin income per in-game day, per owned building, when Condition = 100.
     * Scales linearly with Condition.
     */
    public static final int MAX_DAILY_INCOME = 10;

    /**
     * Condition lost per in-game day without maintenance repairs.
     * 1 repair action (BRICK or WOOD) restores {@link #CONDITION_PER_REPAIR} points.
     */
    public static final int DECAY_PER_DAY = 5;

    /** Condition restored by one repair action (using BRICK or WOOD). */
    public static final int CONDITION_PER_REPAIR = 10;

    /** Respect penalty applied to the faction that owned the building when purchased. */
    public static final int FACTION_PURCHASE_RESPECT_PENALTY = 15;

    /** Weekly council rates cost per owned building (in COINs). */
    public static final int WEEKLY_RATES_COST = 20;

    /** Days between council rate demands (7 in-game days = 1 week). */
    public static final int RATES_PERIOD_DAYS = 7;

    /** Number of missed rate periods before compulsory purchase. */
    public static final int COMPULSORY_PURCHASE_MISSED_PERIODS = 2;

    /** Base purchase price for a building (COINs). */
    public static final int BASE_PURCHASE_PRICE = 50;

    /** Weekday hours during which the ESTATE_AGENT NPC is available (9–17). */
    public static final float ESTATE_AGENT_OPEN_HOUR  = 9f;
    public static final float ESTATE_AGENT_CLOSE_HOUR = 17f;

    // ── Inner record ─────────────────────────────────────────────────────────

    /**
     * Represents a single owned property.
     */
    public static class OwnedProperty {

        private final LandmarkType type;
        /** World-space X/Z centre of the building (approximate). */
        private final int worldX;
        private final int worldZ;

        /** Condition 0–100. */
        private int condition;

        /** Number of in-game days this property has been owned without repair. */
        private int daysSinceRepair;

        /** Number of consecutive rate periods the player has not paid. */
        private int missedRatePeriods;

        /** Whether a THUG NPC is currently stationed outside this building. */
        private boolean underTakeover;

        public OwnedProperty(LandmarkType type, int worldX, int worldZ) {
            this.type             = type;
            this.worldX           = worldX;
            this.worldZ           = worldZ;
            this.condition        = INITIAL_CONDITION;
            this.daysSinceRepair  = 0;
            this.missedRatePeriods = 0;
            this.underTakeover    = false;
        }

        // Accessors

        public LandmarkType getType() { return type; }
        public int getWorldX()        { return worldX; }
        public int getWorldZ()        { return worldZ; }

        public int getCondition()     { return condition; }
        public void setCondition(int v) { this.condition = Math.max(0, Math.min(MAX_CONDITION, v)); }

        public boolean isDerelict()   { return condition <= DERELICT_THRESHOLD; }

        public int getDaysSinceRepair()      { return daysSinceRepair; }
        public void resetRepairClock()        { this.daysSinceRepair = 0; }
        public void incrementRepairClock()    { this.daysSinceRepair++; }

        public int getMissedRatePeriods()     { return missedRatePeriods; }
        public void clearMissedRates()        { this.missedRatePeriods = 0; }
        public void incrementMissedRates()    { this.missedRatePeriods++; }

        public boolean isUnderTakeover()      { return underTakeover; }
        public void setUnderTakeover(boolean v) { this.underTakeover = v; }

        /**
         * Calculate the daily passive income for this property (scales with condition).
         */
        public int getDailyIncome() {
            if (isDerelict()) return 0;
            return Math.round(MAX_DAILY_INCOME * (condition / (float) MAX_CONDITION));
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final List<OwnedProperty> properties = new ArrayList<>();

    /** Day counter used to trigger daily decay and weekly rates. */
    private int lastProcessedDay = 0;

    /** Whether the player has been warned about the cap tooltip this session. */
    private boolean capTooltipShown = false;

    /** Accumulated coins owed to the player from passive income (collected on day-tick). */
    private int pendingIncome = 0;

    /** Tooltip message displayed by the game HUD, cleared after display. */
    private String pendingTooltip = null;

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final FactionSystem factionSystem;
    private final AchievementSystem achievementSystem;
    private final RumourNetwork rumourNetwork;

    // ── Constructor ───────────────────────────────────────────────────────────

    public PropertySystem(FactionSystem factionSystem,
                          AchievementSystem achievementSystem,
                          RumourNetwork rumourNetwork) {
        this.factionSystem      = factionSystem;
        this.achievementSystem  = achievementSystem;
        this.rumourNetwork      = rumourNetwork;
    }

    // ── ESTATE_AGENT schedule ─────────────────────────────────────────────────

    /**
     * Returns true if the ESTATE_AGENT NPC is currently open for business.
     * Open weekdays (Mon–Fri) from {@link #ESTATE_AGENT_OPEN_HOUR} to
     * {@link #ESTATE_AGENT_CLOSE_HOUR}.
     *
     * @param timeSystem the current game time
     * @return true if the estate agent is open
     */
    public boolean isEstateAgentOpen(TimeSystem timeSystem) {
        float hour = timeSystem.getTime();
        if (hour < ESTATE_AGENT_OPEN_HOUR || hour >= ESTATE_AGENT_CLOSE_HOUR) {
            return false;
        }
        // Use dayCount to determine weekday. Game starts on day 1 = Wednesday (0=Mon, 2=Wed).
        // dayCount mod 7: 0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun
        int weekday = (timeSystem.getDayCount() + 2) % 7; // +2 so day 1 maps to Wed=2
        return weekday < 5; // Mon–Fri
    }

    // ── Property purchase ─────────────────────────────────────────────────────

    /**
     * Attempt to purchase the building at the given location.
     *
     * @param type      the landmark type of the building
     * @param worldX    world-space X position
     * @param worldZ    world-space Z position
     * @param inventory the player's inventory (coins deducted)
     * @param allNpcs   all living NPCs (for barman rumour seeding)
     * @param owningFaction the faction that currently owns the building, or null
     * @return a result message, or null if purchase failed
     */
    public String purchaseProperty(LandmarkType type, int worldX, int worldZ,
                                   Inventory inventory, List<NPC> allNpcs,
                                   Faction owningFaction) {
        // Cap check
        if (properties.size() >= MAX_PROPERTIES) {
            pendingTooltip = "You're not Thatcher. Five properties is enough.";
            achievementSystem.unlock(AchievementType.THATCHER_WOULDNT);
            return "You already own " + MAX_PROPERTIES + " properties. That's quite enough.";
        }

        // Already owned
        for (OwnedProperty p : properties) {
            if (p.getWorldX() == worldX && p.getWorldZ() == worldZ) {
                return "You already own this building.";
            }
        }

        int price = BASE_PURCHASE_PRICE;
        if (inventory.getItemCount(Material.COIN) < price) {
            return "You can't afford that. You need " + price + " coins.";
        }

        inventory.removeItem(Material.COIN, price);
        inventory.addItem(Material.DEED, 1);

        OwnedProperty prop = new OwnedProperty(type, worldX, worldZ);
        properties.add(prop);

        // Faction respect penalty
        if (owningFaction != null) {
            factionSystem.applyRespectDelta(owningFaction, -FACTION_PURCHASE_RESPECT_PENALTY);
        }

        // Achievements
        if (properties.size() == 1) {
            achievementSystem.unlock(AchievementType.FIRST_PROPERTY);
        }
        if (properties.size() == MAX_PROPERTIES) {
            achievementSystem.unlock(AchievementType.ARMCHAIR_LANDLORD);
        }

        // Seed barman rumour
        seedBarmanRumour("Someone's gone and bought a building on the high street", allNpcs);

        return "Property purchased. Here's your deed. Try not to let it fall apart.";
    }

    // ── Repair ────────────────────────────────────────────────────────────────

    /**
     * Repair the property nearest to (worldX, worldZ) using BRICK or WOOD from the
     * player's inventory.
     *
     * @param worldX    player's world X
     * @param worldZ    player's world Z
     * @param inventory player's inventory
     * @return a message describing the result, or null if no nearby property
     */
    public String repairProperty(int worldX, int worldZ, Inventory inventory) {
        OwnedProperty prop = findNearest(worldX, worldZ, 10);
        if (prop == null) {
            return null;
        }

        Material mat = null;
        if (inventory.getItemCount(Material.BRICK) > 0) {
            mat = Material.BRICK;
        } else if (inventory.getItemCount(Material.WOOD) > 0) {
            mat = Material.WOOD;
        } else if (inventory.getItemCount(Material.PAINT_TIN) > 0) {
            mat = Material.PAINT_TIN;
        }

        if (mat == null) {
            return "You need BRICK, WOOD, or a PAINT_TIN to repair this building.";
        }

        inventory.removeItem(mat, 1);
        prop.setCondition(prop.getCondition() + CONDITION_PER_REPAIR);
        prop.resetRepairClock();

        if (prop.getCondition() >= MAX_CONDITION) {
            achievementSystem.unlock(AchievementType.SLUM_CLEARANCE);
            return "Building fully repaired. The tenants are cautiously optimistic.";
        }

        return "Building repaired. Condition: " + prop.getCondition() + "/100.";
    }

    // ── Takeover / eviction ───────────────────────────────────────────────────

    /**
     * Called when a THUG NPC is punched off a player-owned building.
     * Marks the building as no longer under takeover.
     *
     * @param worldX  location of the building
     * @param worldZ  location of the building
     */
    public void onThugsDefeated(int worldX, int worldZ) {
        OwnedProperty prop = findNearest(worldX, worldZ, 10);
        if (prop != null) {
            prop.setUnderTakeover(false);
        }
    }

    /**
     * Use an EVICTION_NOTICE item to remove THUG NPCs from a player-owned building.
     *
     * @param worldX    building location
     * @param worldZ    building location
     * @param inventory player's inventory
     * @param allNpcs   living NPCs (THUGs will be set to leave)
     * @return result message, or null if preconditions not met
     */
    public String useEvictionNotice(int worldX, int worldZ,
                                    Inventory inventory, List<NPC> allNpcs) {
        OwnedProperty prop = findNearest(worldX, worldZ, 10);
        if (prop == null) {
            return null;
        }
        if (!prop.isUnderTakeover()) {
            return "There's no one outside your building that needs evicting.";
        }
        if (inventory.getItemCount(Material.EVICTION_NOTICE) <= 0) {
            return "You need an Eviction Notice to do that.";
        }

        inventory.removeItem(Material.EVICTION_NOTICE, 1);
        prop.setUnderTakeover(false);

        // Remove nearby THUG NPCs
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            if (npc.getType() == NPCType.THUG) {
                float dx = npc.getPosition().x - worldX;
                float dz = npc.getPosition().z - worldZ;
                if (dx * dx + dz * dz < 100f) { // within 10 blocks
                    npc.setState(NPCState.FLEEING);
                }
            }
        }

        return "Eviction notice served. They won't like it, but they'll have to lump it.";
    }

    /**
     * Spawn a THUG takeover attempt at the property nearest to (worldX, worldZ).
     * Called by the game loop when a rival faction decides to make a move.
     *
     * @param worldX approximate building X
     * @param worldZ approximate building Z
     */
    public void triggerTakeoverAttempt(int worldX, int worldZ) {
        OwnedProperty prop = findNearest(worldX, worldZ, 20);
        if (prop != null && !prop.isUnderTakeover()) {
            prop.setUnderTakeover(true);
        }
    }

    // ── Council rates ─────────────────────────────────────────────────────────

    /**
     * Attempt to pay council rates for all owned properties.
     *
     * @param inventory player's inventory
     * @return true if rates were paid; false if insufficient funds
     */
    public boolean payRates(Inventory inventory) {
        int totalCost = properties.size() * WEEKLY_RATES_COST;
        if (inventory.getItemCount(Material.COIN) < totalCost) {
            return false;
        }
        inventory.removeItem(Material.COIN, totalCost);
        for (OwnedProperty prop : properties) {
            prop.clearMissedRates();
        }
        return true;
    }

    // ── Daily / weekly tick ───────────────────────────────────────────────────

    /**
     * Called once per in-game day to apply decay, collect income, and check rates.
     *
     * @param currentDay     the current in-game day count
     * @param inventory      player's inventory (income deposited here)
     * @param turfMap        the faction turf map (player buildings marked neutral)
     * @param allNpcs        all living NPCs (for barman gossip)
     * @return a list of notification messages to display (may be empty, never null)
     */
    public List<String> onDayTick(int currentDay, Inventory inventory,
                                   TurfMap turfMap, List<NPC> allNpcs) {
        if (currentDay <= lastProcessedDay) {
            return Collections.emptyList();
        }

        List<String> messages = new ArrayList<>();
        List<OwnedProperty> toRevert = new ArrayList<>();

        for (OwnedProperty prop : properties) {
            // Passive income
            int income = prop.getDailyIncome();
            if (income > 0) {
                pendingIncome += income;
            }

            // Daily decay
            prop.incrementRepairClock();
            if (prop.getDaysSinceRepair() >= 1) {
                prop.setCondition(prop.getCondition() - DECAY_PER_DAY);
            }

            if (prop.isDerelict()) {
                messages.add(prop.getType().getDisplayName() != null
                        ? prop.getType().getDisplayName() + " has fallen into disrepair."
                        : "One of your buildings has fallen into disrepair.");
            }

            // Weekly rates check
            if (currentDay % RATES_PERIOD_DAYS == 0) {
                // Check if rates were paid this period
                // Rates are paid manually; if missedRatePeriods has been incremented
                // then they haven't paid this cycle
                prop.incrementMissedRates();
                if (prop.getMissedRatePeriods() == 1) {
                    messages.add("Planning Notice: rates due for your " +
                            (prop.getType().getDisplayName() != null
                                    ? prop.getType().getDisplayName()
                                    : "building") + ". Pay up.");
                } else if (prop.getMissedRatePeriods() >= COMPULSORY_PURCHASE_MISSED_PERIODS) {
                    toRevert.add(prop);
                    messages.add("Compulsory purchase order! The council has reclaimed " +
                            (prop.getType().getDisplayName() != null
                                    ? prop.getType().getDisplayName()
                                    : "your building") + ".");
                    achievementSystem.unlock(AchievementType.RATES_DODGER);
                }
            }
        }

        // Deposit pending income
        if (pendingIncome > 0) {
            inventory.addItem(Material.COIN, pendingIncome);
            pendingIncome = 0;
        }

        // Revert compulsorily purchased buildings
        properties.removeAll(toRevert);

        // Seed barman rumour if any reversion happened
        if (!toRevert.isEmpty()) {
            seedBarmanRumour("The council's been seizing buildings off people round here", allNpcs);
        }

        // Mark all owned properties as neutral on TurfMap
        for (OwnedProperty prop : properties) {
            if (turfMap != null) {
                turfMap.setOwner(prop.getWorldX(), prop.getWorldZ(), null);
            }
        }

        lastProcessedDay = currentDay;
        return messages;
    }

    // ── Bookies fruit machine hook ────────────────────────────────────────────

    /**
     * Returns true if the player owns the bookies, which flips the fruit machine odds.
     */
    public boolean ownsBookies() {
        for (OwnedProperty p : properties) {
            if (p.getType() == LandmarkType.BOOKIES || p.getType() == LandmarkType.BETTING_SHOP) {
                return true;
            }
        }
        return false;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Returns an unmodifiable view of all owned properties. */
    public List<OwnedProperty> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    /** Returns the number of owned properties. */
    public int getPropertyCount() {
        return properties.size();
    }

    /**
     * Returns the pending tooltip message (shown once then cleared), or null.
     */
    public String pollTooltip() {
        String tip = pendingTooltip;
        pendingTooltip = null;
        return tip;
    }

    /** Returns the property at the given location, or null. */
    public OwnedProperty getPropertyAt(int worldX, int worldZ) {
        return findNearest(worldX, worldZ, 5);
    }

    /** Returns whether the player owns the property at (worldX, worldZ). */
    public boolean ownsAt(int worldX, int worldZ) {
        return findNearest(worldX, worldZ, 5) != null;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private OwnedProperty findNearest(int worldX, int worldZ, int maxDist) {
        OwnedProperty best = null;
        int bestDist = Integer.MAX_VALUE;
        for (OwnedProperty p : properties) {
            int dx = p.getWorldX() - worldX;
            int dz = p.getWorldZ() - worldZ;
            int dist = Math.abs(dx) + Math.abs(dz);
            if (dist < bestDist && dist <= maxDist) {
                bestDist = dist;
                best = p;
            }
        }
        return best;
    }

    private void seedBarmanRumour(String text, List<NPC> allNpcs) {
        if (rumourNetwork == null || allNpcs == null) return;
        Rumour rumour = new Rumour(RumourType.GANG_ACTIVITY, text);
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            if (npc.getType() == ragamuffin.entity.NPCType.BARMAN) {
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

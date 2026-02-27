package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #785: The Dodgy Market Stall — Underground Street Trade Empire.
 *
 * <p>A passive income engine and faction-pressure sandbox. The player crafts a
 * {@link Material#STALL_FRAME} (4 WOOD) and places it on PAVEMENT or ROAD.
 * When set to OPEN, the stall automatically attracts NPCs whose need scores
 * are elevated — they pathfind to the stall, buy one item, and their need
 * is zeroed.
 *
 * <h3>Stall Placement</h3>
 * <ul>
 *   <li>Must be placed on PAVEMENT or ROAD block.</li>
 *   <li>{@link #placeStall} validates placement and returns true on success.</li>
 * </ul>
 *
 * <h3>Stock</h3>
 * <ul>
 *   <li>6 stock slots; player sets per-item asking prices.</li>
 *   <li>NPCs with elevated needs pathfind to the stall and purchase one item.</li>
 *   <li>Customer queue limited to 3 simultaneous NPCs.</li>
 * </ul>
 *
 * <h3>Licencing</h3>
 * <ul>
 *   <li>{@link Material#MARKET_LICENCE} purchased from Council NPC for 20 COIN.</li>
 *   <li>Unlicensed trading for 3+ in-game minutes spawns a {@link NPCType#MARKET_INSPECTOR}.</li>
 *   <li>Inspector can be bribed for 10 COIN (+5 Notoriety) or confiscates all stock.</li>
 *   <li>POLICE within 8 blocks: 50% chance of Stall Fine (stock confiscated, offence added,
 *       stall closes) if unlicensed.</li>
 * </ul>
 *
 * <h3>Faction Integration</h3>
 * <ul>
 *   <li>MARCHETTI territory: 20% protection cut on income.</li>
 *   <li>STREET_LADS territory: free protection, but rival gangs may trash stock.</li>
 *   <li>THE_COUNCIL territory: planning notices → eventual demolition.</li>
 *   <li>All factions at Respect ≥ 75: 1.5× income multiplier.</li>
 * </ul>
 *
 * <h3>Weather</h3>
 * <ul>
 *   <li>RAIN without {@link Material#STALL_AWNING}: closes stall and destroys stock.</li>
 *   <li>FROST: halves customer rate.</li>
 *   <li>HEATWAVE: doubles customer rate.</li>
 * </ul>
 *
 * <h3>Upgrade Tiers</h3>
 * <ul>
 *   <li>Tier 0 (0 COIN lifetime): 3 stock slots, queue of 1.</li>
 *   <li>Tier 1 (50 COIN lifetime): 5 stock slots, queue of 2.</li>
 *   <li>Tier 2 (200 COIN lifetime): 6 stock slots, queue of 3, BuildingSign, TOURIST attraction.</li>
 * </ul>
 */
public class StallSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** In-game seconds before a MARKET_INSPECTOR spawns for unlicensed trading. */
    public static final float UNLICENSED_INSPECTOR_DELAY = 180f; // 3 in-game minutes

    /** Cost to bribe the Market Inspector (COIN). */
    public static final int INSPECTOR_BRIBE_COST = 10;

    /** Notoriety gain from bribing the Market Inspector. */
    public static final int INSPECTOR_BRIBE_NOTORIETY = 5;

    /** POLICE detection range for stall licence check (blocks). */
    public static final float POLICE_DETECTION_RANGE = 8f;

    /** Probability of a police fine for unlicensed stall when police are in range. */
    public static final float POLICE_FINE_PROBABILITY = 0.5f;

    /** How many seconds between customer attraction scans. */
    public static final float CUSTOMER_SCAN_INTERVAL = 30f;

    /** NPC need score threshold above which an NPC becomes a customer. */
    public static final float CUSTOMER_NEED_THRESHOLD = 50f;

    /** Maximum customer queue size (at max tier). */
    public static final int MAX_QUEUE_SIZE = 3;

    /** Need score threshold below which a customer will not approach. */
    public static final float MIN_CUSTOMER_NEED = CUSTOMER_NEED_THRESHOLD;

    /** Maximum range (blocks) within which NPCs are attracted as customers. */
    public static final float CUSTOMER_ATTRACTION_RANGE = 20f;

    /** Distance in blocks at which an NPC is "at the stall" and makes a purchase. */
    public static final float STALL_PURCHASE_RANGE = 2f;

    /** Lifetime sales threshold (COIN) for Tier 1 upgrade. */
    public static final int TIER_1_SALES_THRESHOLD = 50;

    /** Lifetime sales threshold (COIN) for Tier 2 upgrade. */
    public static final int TIER_2_SALES_THRESHOLD = 200;

    /** Income multiplier when all factions are at Respect ≥ 75. */
    public static final float ALL_FACTION_RESPECT_MULTIPLIER = 1.5f;

    /** Faction Respect threshold for the all-faction multiplier. */
    public static final int ALL_FACTION_RESPECT_THRESHOLD = 75;

    /** Marchetti protection cut fraction. */
    public static final float MARCHETTI_PROTECTION_CUT = 0.20f;

    /** Cost to buy a Market Licence from the Council NPC. */
    public static final int MARKET_LICENCE_COST = 20;

    // ── Stall state ───────────────────────────────────────────────────────────

    /** Whether the player has placed a stall in the world. */
    private boolean stallPlaced = false;

    /** Whether the stall is currently open for business. */
    private boolean stallOpen = false;

    /** Whether the stall has a {@link Material#STALL_AWNING} attached. */
    private boolean hasAwning = false;

    /** Whether the stall has a {@link Material#MARKET_LICENCE}. */
    private boolean hasLicence = false;

    /** Stall position (grid X). */
    private int stallX = 0;

    /** Stall position (grid Y). */
    private int stallY = 0;

    /** Stall position (grid Z). */
    private int stallZ = 0;

    /**
     * The 6-slot stock for the stall. Each entry is [material, quantity, askingPrice].
     * null means empty slot.
     */
    private final StallSlot[] stock = new StallSlot[6];

    /** Total coin accumulated in the stall's running coin total. */
    private int stallCoinTotal = 0;

    /** Lifetime sales in COIN (for upgrade tracking). */
    private int lifetimeSales = 0;

    /** Current upgrade tier (0, 1, 2). */
    private int stallTier = 0;

    /** Current faction territory the stall is in (null = none). */
    private Faction stallTerritory = null;

    // ── Inspector state ───────────────────────────────────────────────────────

    /** Timer counting unlicensed trading time (seconds). */
    private float unlicensedTimer = 0f;

    /** Whether a MARKET_INSPECTOR is currently active. */
    private boolean inspectorActive = false;

    /** The active Market Inspector NPC (null if none). */
    private NPC activeInspector = null;

    // ── Customer queue ────────────────────────────────────────────────────────

    /** Active customer NPCs heading to the stall. */
    private final List<NPC> customerQueue = new ArrayList<>();

    /** Timer between customer attraction scans. */
    private float customerScanTimer = 0f;

    // ── Achievement tracking ──────────────────────────────────────────────────

    private boolean achievementMarketTrader = false;
    private boolean achievementLicensedToSell = false;
    private boolean achievementBribedInspector = false;
    private boolean achievementShutItDown = false;
    private boolean achievementEmpireBuilder = false;
    private boolean achievementTurfVendor = false;

    // ── Random ────────────────────────────────────────────────────────────────

    private final Random random;

    // ── Construction ──────────────────────────────────────────────────────────

    public StallSystem() {
        this(new Random());
    }

    public StallSystem(Random random) {
        this.random = random;
    }

    // ── Stall placement ────────────────────────────────────────────────────────

    /**
     * Attempt to place a stall at the given grid position.
     *
     * @param x                grid X coordinate
     * @param y                grid Y coordinate
     * @param z                grid Z coordinate
     * @param groundBlockType  the block type at this position (must be PAVEMENT or ROAD)
     * @param playerInventory  the player's inventory (STALL_FRAME consumed on success)
     * @param territory        the faction territory at this position (may be null)
     * @return true if placement succeeded
     */
    public boolean placeStall(int x, int y, int z, String groundBlockType,
                              Inventory playerInventory, Faction territory) {
        if (stallPlaced) {
            return false; // already placed
        }
        if (!"PAVEMENT".equals(groundBlockType) && !"ROAD".equals(groundBlockType)) {
            return false; // invalid surface
        }
        if (playerInventory == null || playerInventory.getItemCount(Material.STALL_FRAME) < 1) {
            return false; // need a STALL_FRAME
        }

        playerInventory.removeItem(Material.STALL_FRAME, 1);
        stallPlaced = true;
        stallX = x;
        stallY = y;
        stallZ = z;
        stallTerritory = territory;
        stallOpen = false;
        hasAwning = false;
        hasLicence = false;
        unlicensedTimer = 0f;
        stallCoinTotal = 0;

        return true;
    }

    /**
     * Remove (demolish) the stall. Drops any remaining stock into the player inventory.
     *
     * @param playerInventory player's inventory to receive leftover stock
     */
    public void removeStall(Inventory playerInventory) {
        if (!stallPlaced) return;
        if (playerInventory != null) {
            for (StallSlot slot : stock) {
                if (slot != null) {
                    playerInventory.addItem(slot.material, slot.quantity);
                }
            }
        }
        stallPlaced = false;
        stallOpen = false;
        clearStock();
        customerQueue.clear();
        inspectorActive = false;
        activeInspector = null;
    }

    // ── Stock management ───────────────────────────────────────────────────────

    /**
     * Set a stock slot with a material, quantity, and asking price.
     *
     * @param slotIndex   0–5
     * @param material    material to stock
     * @param quantity    quantity to stock
     * @param askingPrice price in COIN per unit
     * @return true if set successfully
     */
    public boolean setStockSlot(int slotIndex, Material material, int quantity, int askingPrice) {
        if (!stallPlaced) return false;
        int maxSlots = getMaxStockSlots();
        if (slotIndex < 0 || slotIndex >= maxSlots) return false;
        if (material == null || quantity <= 0 || askingPrice <= 0) return false;
        stock[slotIndex] = new StallSlot(material, quantity, askingPrice);
        return true;
    }

    /**
     * Clear a stock slot.
     */
    public boolean clearStockSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= stock.length) return false;
        stock[slotIndex] = null;
        return true;
    }

    private void clearStock() {
        for (int i = 0; i < stock.length; i++) {
            stock[i] = null;
        }
    }

    /**
     * Maximum number of active stock slots at the current tier.
     */
    public int getMaxStockSlots() {
        if (stallTier >= 2) return 6;
        if (stallTier >= 1) return 5;
        return 3;
    }

    // ── Open / close ───────────────────────────────────────────────────────────

    /**
     * Open the stall for business.
     *
     * @return true if the stall was opened
     */
    public boolean openStall() {
        if (!stallPlaced) return false;
        stallOpen = true;
        return true;
    }

    /**
     * Close the stall.
     */
    public void closeStall() {
        stallOpen = false;
        customerQueue.clear();
    }

    // ── Awning / licence ───────────────────────────────────────────────────────

    /**
     * Attach a stall awning (consumes one {@link Material#STALL_AWNING} from inventory).
     */
    public boolean attachAwning(Inventory playerInventory) {
        if (!stallPlaced || hasAwning) return false;
        if (playerInventory == null || playerInventory.getItemCount(Material.STALL_AWNING) < 1) {
            return false;
        }
        playerInventory.removeItem(Material.STALL_AWNING, 1);
        hasAwning = true;
        return true;
    }

    /**
     * Buy a market licence from the Council NPC.
     * Costs {@link #MARKET_LICENCE_COST} COIN from the player's inventory.
     */
    public boolean buyLicence(Inventory playerInventory,
                              NotorietySystem.AchievementCallback achievementCallback) {
        if (!stallPlaced || hasLicence) return false;
        if (playerInventory == null || playerInventory.getItemCount(Material.COIN) < MARKET_LICENCE_COST) {
            return false;
        }
        playerInventory.removeItem(Material.COIN, MARKET_LICENCE_COST);
        hasLicence = true;
        unlicensedTimer = 0f;

        if (!achievementLicensedToSell) {
            achievementLicensedToSell = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.LICENSED_TO_SELL);
            }
        }

        return true;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update the stall system.
     *
     * @param delta               seconds since last frame
     * @param npcs                all NPCs in the world
     * @param player              the player entity
     * @param weather             current weather
     * @param playerInventory     player's inventory (for coin drops)
     * @param factionSystem       for Respect lookups and territory checks
     * @param notorietySystem     for Notoriety additions
     * @param criminalRecord      for recording stall offences
     * @param achievementCallback for unlocking achievements
     * @param streetEconomySystem for NPC need score lookups
     */
    public void update(float delta,
                       List<NPC> npcs,
                       Player player,
                       Weather weather,
                       Inventory playerInventory,
                       FactionSystem factionSystem,
                       NotorietySystem notorietySystem,
                       CriminalRecord criminalRecord,
                       NotorietySystem.AchievementCallback achievementCallback,
                       StreetEconomySystem streetEconomySystem) {

        if (!stallPlaced) return;

        // ── Weather damage ─────────────────────────────────────────────────────
        if (stallOpen && weather != null) {
            if (weather == Weather.RAIN && !hasAwning) {
                closeStall();
                clearStock();
                return;
            }
        }

        if (!stallOpen) return;

        // ── Unlicensed trading timer ───────────────────────────────────────────
        if (!hasLicence) {
            unlicensedTimer += delta;

            // Check for POLICE within range
            if (npcs != null) {
                for (NPC npc : npcs) {
                    if (!npc.isAlive() || npc.getType() != NPCType.POLICE) continue;
                    float dist = distanceToStall(npc.getPosition().x, npc.getPosition().y, npc.getPosition().z);
                    if (dist <= POLICE_DETECTION_RANGE) {
                        if (random.nextFloat() < POLICE_FINE_PROBABILITY) {
                            applyStallFine(playerInventory, criminalRecord, achievementCallback);
                            return;
                        }
                    }
                }
            }

            // Spawn inspector after 3 minutes unlicensed
            if (unlicensedTimer >= UNLICENSED_INSPECTOR_DELAY && !inspectorActive) {
                spawnInspector(npcs);
            }
        }

        // ── Inspector movement / interaction ───────────────────────────────────
        if (inspectorActive && activeInspector != null) {
            if (!activeInspector.isAlive()) {
                inspectorActive = false;
                activeInspector = null;
            } else {
                float inspDist = distanceToStall(
                    activeInspector.getPosition().x, activeInspector.getPosition().y, activeInspector.getPosition().z);
                if (inspDist <= STALL_PURCHASE_RANGE) {
                    // Inspector has arrived — confiscate stock
                    confiscateStock(criminalRecord);
                }
            }
        }

        // ── Customer scan ──────────────────────────────────────────────────────
        customerScanTimer += delta;
        if (customerScanTimer >= CUSTOMER_SCAN_INTERVAL) {
            customerScanTimer = 0f;
            attractCustomers(npcs, weather, streetEconomySystem);
        }

        // ── Customer purchases ─────────────────────────────────────────────────
        processCustomerPurchases(playerInventory, factionSystem, notorietySystem,
            achievementCallback, weather);

        // ── Upgrade check ──────────────────────────────────────────────────────
        checkUpgrade(achievementCallback);
    }

    // ── Customer attraction ────────────────────────────────────────────────────

    private void attractCustomers(List<NPC> npcs, Weather weather,
                                  StreetEconomySystem streetEconomySystem) {
        if (npcs == null) return;

        int maxQueue = getMaxQueueSize();
        if (customerQueue.size() >= maxQueue) return;

        float customerRateMultiplier = 1.0f;
        if (weather == Weather.FROST) {
            customerRateMultiplier = 0.5f;
        } else if (weather == Weather.HEATWAVE) {
            customerRateMultiplier = 2.0f;
        }

        // Probabilistic: only scan effectively at reduced rate in frost
        if (random.nextFloat() > customerRateMultiplier) return;

        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            if (customerQueue.contains(npc)) continue;
            if (customerQueue.size() >= maxQueue) break;

            // Only attract PUBLIC-type civilians (not inspectors, police, etc.)
            if (npc.getType() != NPCType.PUBLIC) continue;

            float dist = distanceToStall(npc.getPosition().x, npc.getPosition().y, npc.getPosition().z);
            if (dist > CUSTOMER_ATTRACTION_RANGE) continue;

            // Check if NPC has elevated needs
            boolean hasElevatedNeed = false;
            if (streetEconomySystem != null) {
                for (NeedType need : NeedType.values()) {
                    if (streetEconomySystem.getNeedScore(npc, need) >= MIN_CUSTOMER_NEED) {
                        hasElevatedNeed = true;
                        break;
                    }
                }
            } else {
                // Without economy system, attract all nearby NPCs with some probability
                hasElevatedNeed = random.nextFloat() < 0.3f;
            }

            if (hasElevatedNeed && hasStockAvailable()) {
                customerQueue.add(npc);
                npc.setState(NPCState.WANDERING);
            }
        }
    }

    private boolean hasStockAvailable() {
        for (StallSlot slot : stock) {
            if (slot != null && slot.quantity > 0) return true;
        }
        return false;
    }

    // ── Customer purchase processing ──────────────────────────────────────────

    private void processCustomerPurchases(Inventory playerInventory,
                                          FactionSystem factionSystem,
                                          NotorietySystem notorietySystem,
                                          NotorietySystem.AchievementCallback achievementCallback,
                                          Weather weather) {
        List<NPC> purchasedNpcs = new ArrayList<>();

        for (NPC customer : customerQueue) {
            if (!customer.isAlive()) {
                purchasedNpcs.add(customer);
                continue;
            }

            float dist = distanceToStall(customer.getPosition().x, customer.getPosition().y, customer.getPosition().z);
            if (dist <= STALL_PURCHASE_RANGE) {
                // NPC is at the stall — make a purchase
                StallSlot slot = findFirstAvailableSlot();
                if (slot != null) {
                    int baseRevenue = slot.askingPrice;
                    int revenue = applyFactionModifiers(baseRevenue, factionSystem,
                        playerInventory, achievementCallback);
                    if (playerInventory != null && revenue > 0) {
                        playerInventory.addItem(Material.COIN, revenue);
                    }
                    stallCoinTotal += revenue;
                    lifetimeSales += revenue;
                    slot.quantity--;
                    if (slot.quantity <= 0) {
                        // Remove slot from stock
                        for (int i = 0; i < stock.length; i++) {
                            if (stock[i] == slot) {
                                stock[i] = null;
                                break;
                            }
                        }
                    }
                }
                purchasedNpcs.add(customer);
                customer.setState(NPCState.WANDERING);
            }
        }

        customerQueue.removeAll(purchasedNpcs);
    }

    /**
     * Apply faction modifiers to income and return adjusted revenue.
     * Also handles protection cuts and turf vendor achievement.
     */
    private int applyFactionModifiers(int baseRevenue, FactionSystem factionSystem,
                                      Inventory playerInventory,
                                      NotorietySystem.AchievementCallback achievementCallback) {
        if (factionSystem == null) return baseRevenue;

        float multiplier = 1.0f;

        // All factions at Respect ≥ 75 → 1.5× multiplier
        boolean allFriendly = true;
        for (Faction f : Faction.values()) {
            if (factionSystem.getRespect(f) < ALL_FACTION_RESPECT_THRESHOLD) {
                allFriendly = false;
                break;
            }
        }
        if (allFriendly) {
            multiplier *= ALL_FACTION_RESPECT_MULTIPLIER;
        }

        int revenue = Math.round(baseRevenue * multiplier);

        // Marchetti territory: 20% protection cut
        if (stallTerritory == Faction.MARCHETTI_CREW) {
            int cut = Math.max(1, Math.round(revenue * MARCHETTI_PROTECTION_CUT));
            revenue -= cut;
            if (!achievementTurfVendor) {
                achievementTurfVendor = true;
                if (achievementCallback != null) {
                    achievementCallback.award(AchievementType.TURF_VENDOR);
                }
            }
        } else if (stallTerritory == Faction.STREET_LADS || stallTerritory == Faction.THE_COUNCIL) {
            if (!achievementTurfVendor) {
                achievementTurfVendor = true;
                if (achievementCallback != null) {
                    achievementCallback.award(AchievementType.TURF_VENDOR);
                }
            }
        }

        return Math.max(0, revenue);
    }

    // ── Police fine ────────────────────────────────────────────────────────────

    private void applyStallFine(Inventory playerInventory, CriminalRecord criminalRecord,
                                NotorietySystem.AchievementCallback achievementCallback) {
        // Confiscate all stock, add offence, close stall
        clearStock();
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.WITNESSED_CRIMES);
        }
        closeStall();

        if (!achievementShutItDown) {
            achievementShutItDown = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.SHUTIT_DOWN);
            }
        }
    }

    // ── Stock confiscation by inspector ───────────────────────────────────────

    private void confiscateStock(CriminalRecord criminalRecord) {
        clearStock();
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.WITNESSED_CRIMES);
        }
        closeStall();
        inspectorActive = false;
        activeInspector = null;
    }

    // ── Inspector bribe ────────────────────────────────────────────────────────

    /**
     * Attempt to bribe the Market Inspector.
     * Costs {@link #INSPECTOR_BRIBE_COST} COIN and adds {@link #INSPECTOR_BRIBE_NOTORIETY}
     * to the Notoriety score.
     *
     * @return true if the bribe succeeded
     */
    public boolean bribeInspector(Inventory playerInventory,
                                  NotorietySystem notorietySystem,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        if (!inspectorActive || activeInspector == null) return false;
        if (playerInventory == null || playerInventory.getItemCount(Material.COIN) < INSPECTOR_BRIBE_COST) {
            return false;
        }

        playerInventory.removeItem(Material.COIN, INSPECTOR_BRIBE_COST);
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(INSPECTOR_BRIBE_NOTORIETY, achievementCallback);
        }
        inspectorActive = false;
        activeInspector = null;
        unlicensedTimer = 0f; // Reset timer after bribe

        if (!achievementBribedInspector) {
            achievementBribedInspector = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.BRIBED_THE_INSPECTOR);
            }
        }

        return true;
    }

    // ── Inspector spawning ─────────────────────────────────────────────────────

    private void spawnInspector(List<NPC> allNpcs) {
        NPC inspector = new NPC(NPCType.MARKET_INSPECTOR,
            stallX + 5f, stallY, stallZ + 5f);
        if (allNpcs != null) {
            allNpcs.add(inspector);
        }
        activeInspector = inspector;
        inspectorActive = true;
    }

    // ── Upgrade checks ─────────────────────────────────────────────────────────

    private void checkUpgrade(NotorietySystem.AchievementCallback achievementCallback) {
        int oldTier = stallTier;
        if (lifetimeSales >= TIER_2_SALES_THRESHOLD) {
            stallTier = 2;
        } else if (lifetimeSales >= TIER_1_SALES_THRESHOLD) {
            stallTier = 1;
        }

        if (stallTier == 2 && !achievementEmpireBuilder) {
            achievementEmpireBuilder = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.EMPIRE_BUILDER);
            }
        }
    }

    // ── Open stall / first placement achievement ───────────────────────────────

    /**
     * Should be called when the player first opens the stall for business.
     */
    public boolean openStallWithAchievement(NotorietySystem.AchievementCallback achievementCallback) {
        boolean opened = openStall();
        if (opened && !achievementMarketTrader) {
            achievementMarketTrader = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.MARKET_TRADER);
            }
        }
        return opened;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private float distanceToStall(float x, float y, float z) {
        float dx = x - stallX;
        float dy = y - stallY;
        float dz = z - stallZ;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private StallSlot findFirstAvailableSlot() {
        for (StallSlot slot : stock) {
            if (slot != null && slot.quantity > 0) return slot;
        }
        return null;
    }

    private int getMaxQueueSize() {
        if (stallTier >= 2) return MAX_QUEUE_SIZE;
        if (stallTier >= 1) return 2;
        return 1;
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public boolean isStallPlaced() { return stallPlaced; }
    public boolean isStallOpen() { return stallOpen; }
    public boolean isHasAwning() { return hasAwning; }
    public boolean isHasLicence() { return hasLicence; }
    public int getStallCoinTotal() { return stallCoinTotal; }
    public int getLifetimeSales() { return lifetimeSales; }
    public int getStallTier() { return stallTier; }
    public int getStallX() { return stallX; }
    public int getStallY() { return stallY; }
    public int getStallZ() { return stallZ; }
    public boolean isInspectorActive() { return inspectorActive; }
    public NPC getActiveInspector() { return activeInspector; }
    public float getUnlicensedTimer() { return unlicensedTimer; }
    public List<NPC> getCustomerQueue() { return new ArrayList<>(customerQueue); }
    public Faction getStallTerritory() { return stallTerritory; }

    /** Get the stock slot at index (0–5). May be null if empty. */
    public StallSlot getStockSlot(int index) {
        if (index < 0 || index >= stock.length) return null;
        return stock[index];
    }

    /** Set stall territory (for testing). */
    public void setStallTerritory(Faction territory) {
        this.stallTerritory = territory;
    }

    /** Force the unlicensed timer to a specific value (for testing). */
    public void setUnlicensedTimer(float value) {
        this.unlicensedTimer = value;
    }

    /** Force the inspector active state (for testing). */
    public void setInspectorActive(boolean active, NPC inspector) {
        this.inspectorActive = active;
        this.activeInspector = inspector;
    }

    /**
     * Issue #787: Trigger a flash market event (SURVIVAL Legend perk).
     * Opens the stall immediately and resets the customer scan timer so
     * NPCs are attracted immediately.
     */
    public void setFlashMarketActive(boolean active) {
        if (active) {
            this.stallOpen = true;
            this.customerScanTimer = 0f; // force immediate customer scan
        }
    }

    // ── Inner class: StallSlot ─────────────────────────────────────────────────

    /**
     * A single stock slot in the stall.
     */
    public static class StallSlot {
        public Material material;
        public int quantity;
        public int askingPrice;

        public StallSlot(Material material, int quantity, int askingPrice) {
            this.material = material;
            this.quantity = quantity;
            this.askingPrice = askingPrice;
        }
    }
}

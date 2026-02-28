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
import java.util.Map;
import java.util.Random;

/**
 * Issue #799: The Corner Shop Economy — Dynamic Shopkeeping, Price Wars &amp; Neighbourhood Hustle.
 *
 * <p>The player claims a derelict shop unit (press E on a door with Condition ≤ 49, or use a
 * {@link Material#SHOP_KEY}) and runs a grey-to-black-market enterprise that competes directly
 * with Marchetti's off-licence.
 *
 * <h3>Shop Claiming</h3>
 * <ul>
 *   <li>Call {@link #tryClaimShop(Inventory)} to claim via SHOP_KEY, or
 *       {@link #claimShopByInteraction(int)} to claim a derelict unit by door interaction.</li>
 *   <li>Only one shop at a time.</li>
 * </ul>
 *
 * <h3>Customer Traffic</h3>
 * <ul>
 *   <li>PUBLIC, WORKER, and PENSIONER NPCs have a 15% chance per minute to buy a stocked item.</li>
 *   <li>Pricing vs fence-valuation: undercut (+30% traffic, Marchetti Respect −5/day),
 *       fair (baseline), overpriced (−50% traffic).</li>
 * </ul>
 *
 * <h3>Runner NPC</h3>
 * <ul>
 *   <li>Hire any PUBLIC/YOUTH NPC for 5 coins/day wages.</li>
 *   <li>Runner autonomously restocks empty slots and makes player-queued deliveries.</li>
 *   <li>Each delivery earns +2 coin premium; +3 Notoriety per delivery.</li>
 * </ul>
 *
 * <h3>Heat System</h3>
 * <ul>
 *   <li>Heat 0–100 from dodgy sales, runner spotted by police, newspaper mentions.</li>
 *   <li>30 = Inspection Notice prop spawns.</li>
 *   <li>60 = UNDERCOVER_POLICE NPC stakeout begins.</li>
 *   <li>80 = Raid Warning SHOP_NEWS rumour seeded.</li>
 *   <li>100 = Police Raid: stock confiscated, shop closed, Notoriety +25.</li>
 * </ul>
 *
 * <h3>Marchetti Rivalry</h3>
 * <ul>
 *   <li>Undercutting triggers enforcer visit at Marchetti Respect 35 (warning speech).</li>
 *   <li>Shop vandalism if Marchetti Respect &lt; 25.</li>
 *   <li>Player can pay protection (10 coins/day).</li>
 * </ul>
 *
 * <h3>Street Lads Alliance</h3>
 * <ul>
 *   <li>Selling CIDER/TOBACCO/ENERGY_DRINK at fair price gives Street Lads Respect +2–5/day.</li>
 *   <li>Street Lads defend shop against Marchetti enforcers when Respect ≥ 70.</li>
 * </ul>
 *
 * <h3>Council Pressure</h3>
 * <ul>
 *   <li>Daily revenue &gt; 50 coins triggers BUSINESS_RATES_NOTICE prop.</li>
 *   <li>Council Victory spawns a subsidised competitor stall.</li>
 * </ul>
 *
 * <h3>Integrations</h3>
 * <ul>
 *   <li>StreetSkillSystem TRADING perk boosts traffic.</li>
 *   <li>BootSaleSystem bulk-transfer of unsold stock.</li>
 *   <li>NewspaperSystem headlines for revenue milestones.</li>
 *   <li>PirateRadio boosts traffic for 1 day.</li>
 * </ul>
 */
public class CornerShopSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Fence valuation for each base material type (used for pricing comparison). */
    public static final int FENCE_VALUE_DEFAULT = 5;

    /** Customer purchase chance per minute (0–1). */
    public static final float CUSTOMER_PURCHASE_CHANCE_PER_MINUTE = 0.15f;

    /** Traffic bonus multiplier when shop prices undercut fence value. */
    public static final float UNDERCUT_TRAFFIC_MULTIPLIER = 1.30f;

    /** Traffic penalty multiplier when shop prices are overpriced vs fence value. */
    public static final float OVERPRICED_TRAFFIC_MULTIPLIER = 0.50f;

    /** Marchetti Respect lost per day when undercutting. */
    public static final int UNDERCUT_MARCHETTI_RESPECT_PER_DAY = -5;

    /** Runner daily wage in COIN. */
    public static final int RUNNER_DAILY_WAGE = 5;

    /** Coin premium earned per delivery item. */
    public static final int RUNNER_DELIVERY_PREMIUM = 2;

    /** Notoriety gained per runner delivery. */
    public static final int RUNNER_DELIVERY_NOTORIETY = 3;

    /** Heat threshold for Inspection Notice prop. */
    public static final int HEAT_INSPECTION_NOTICE = 30;

    /** Heat threshold for Undercover Police stakeout. */
    public static final int HEAT_UNDERCOVER_STAKEOUT = 60;

    /** Heat threshold for Raid Warning rumour. */
    public static final int HEAT_RAID_WARNING = 80;

    /** Heat threshold triggering Police Raid. */
    public static final int HEAT_RAID = 100;

    /** Notoriety gained from a Police Raid. */
    public static final int RAID_NOTORIETY_GAIN = 25;

    /** Marchetti Respect threshold for enforcer warning visit. */
    public static final int MARCHETTI_ENFORCER_VISIT_RESPECT = 35;

    /** Marchetti Respect threshold below which vandalism occurs. */
    public static final int MARCHETTI_VANDALISM_RESPECT = 25;

    /** Protection payment in COIN per day. */
    public static final int PROTECTION_COST_PER_DAY = 10;

    /** Street Lads Respect gained per day for selling grey-market goods at fair price. */
    public static final int STREET_LADS_FAIR_PRICE_RESPECT_MIN = 2;
    public static final int STREET_LADS_FAIR_PRICE_RESPECT_MAX = 5;

    /** Street Lads Respect threshold for defending the shop. */
    public static final int STREET_LADS_DEFEND_RESPECT = 70;

    /** Daily revenue threshold triggering BUSINESS_RATES_NOTICE. */
    public static final int BUSINESS_RATES_THRESHOLD = 50;

    /** Daily revenue milestone that triggers a newspaper headline. */
    public static final int NEWSPAPER_HEADLINE_REVENUE = 100;

    /** Maximum number of simultaneous customer NPCs. */
    public static final int MAX_CUSTOMER_QUEUE = 3;

    /** Range within which NPCs are attracted as customers (blocks). */
    public static final float CUSTOMER_ATTRACTION_RANGE = 20f;

    /** Range within which an NPC completes a purchase (blocks). */
    public static final float PURCHASE_RANGE = 2f;

    /** Seconds between customer attraction scans (one in-game minute = 60s). */
    public static final float CUSTOMER_SCAN_INTERVAL = 60f;

    /** Heat gained per dodgy sale (CIDER, TOBACCO, ENERGY_DRINK). */
    public static final int HEAT_PER_DODGY_SALE = 2;

    /** Heat gained when runner is spotted by police. */
    public static final int HEAT_RUNNER_SPOTTED = 5;

    /** Building condition threshold for claiming a derelict unit by interaction. */
    public static final int DERELICT_CONDITION_THRESHOLD = 49;

    /** Pirate radio traffic boost duration (in-game seconds ≈ 1 in-game day). */
    public static final float PIRATE_RADIO_BOOST_DURATION = 1200f;

    // ── State ─────────────────────────────────────────────────────────────────

    /** The current shop unit, or null if the player has no shop. */
    private ShopUnit shopUnit = null;

    /** Timer accumulating time for customer scans (seconds). */
    private float customerScanTimer = 0f;

    /** Timer accumulating daily elapsed time for faction Respect and wage handling. */
    private float dailyTimer = 0f;

    /** Whether the player has paid protection today. */
    private boolean paidProtectionToday = false;

    /** Whether a BUSINESS_RATES_NOTICE has been triggered this session. */
    private boolean businessRatesNoticeTriggered = false;

    /** Remaining duration of pirate radio traffic boost (seconds). 0 = inactive. */
    private float pirateRadioBoostTimer = 0f;

    /** Whether the OPEN_FOR_BUSINESS achievement has been unlocked. */
    private boolean achievementOpenForBusiness = false;

    /** Whether the KERCHING achievement has been unlocked. */
    private boolean achievementKerching = false;

    /** Whether the PROTECTION_MONEY achievement has been unlocked. */
    private boolean achievementProtectionMoney = false;

    /** Whether the THE_NEIGHBOURHOOD_SHOP achievement has been unlocked. */
    private boolean achievementNeighbourhoodShop = false;

    /** Whether the RAIDED achievement has been unlocked. */
    private boolean achievementRaided = false;

    /** Whether the PRICE_WAR achievement has been unlocked. */
    private boolean achievementPriceWar = false;

    /** Whether a Marchetti enforcer visit has been triggered this session. */
    private boolean marchettiEnforcerVisitTriggered = false;

    /** Customer NPCs currently heading to the shop. */
    private final List<NPC> customerQueue = new ArrayList<>();

    private final Random random;

    // ── Construction ──────────────────────────────────────────────────────────

    public CornerShopSystem() {
        this(new Random());
    }

    public CornerShopSystem(Random random) {
        this.random = random;
    }

    // ── Shop claiming ─────────────────────────────────────────────────────────

    /**
     * Try to claim a shop using a {@link Material#SHOP_KEY} from the player's inventory.
     *
     * @param playerInventory the player's inventory
     * @return true if the shop was successfully claimed
     */
    public boolean tryClaimShop(Inventory playerInventory) {
        if (shopUnit != null && shopUnit.isClaimed()) {
            return false; // already own a shop
        }
        if (!playerInventory.hasItem(Material.SHOP_KEY)) {
            return false;
        }
        playerInventory.removeItem(Material.SHOP_KEY, 1);
        claimNewShop();
        return true;
    }

    /**
     * Claim a derelict shop unit by pressing E on its door.
     *
     * @param buildingCondition the condition value of the target building (0–100)
     * @return true if the shop was successfully claimed
     */
    public boolean claimShopByInteraction(int buildingCondition) {
        if (shopUnit != null && shopUnit.isClaimed()) {
            return false; // already own a shop
        }
        if (buildingCondition > DERELICT_CONDITION_THRESHOLD) {
            return false;
        }
        claimNewShop();
        return true;
    }

    /**
     * Internal helper: creates and initialises the ShopUnit.
     */
    private void claimNewShop() {
        shopUnit = new ShopUnit();
        shopUnit.setClaimed(true);
    }

    // ── Shop open/close ───────────────────────────────────────────────────────

    /**
     * Open the shop for business.
     *
     * @param achievementCallback called on {@link AchievementType#OPEN_FOR_BUSINESS} unlock
     */
    public void openShop(NotorietySystem.AchievementCallback achievementCallback) {
        if (shopUnit == null || !shopUnit.isClaimed()) return;
        shopUnit.setOpen(true);
        if (!achievementOpenForBusiness && achievementCallback != null) {
            achievementOpenForBusiness = true;
            achievementCallback.award(AchievementType.OPEN_FOR_BUSINESS);
        }
    }

    /**
     * Close the shop.
     */
    public void closeShop() {
        if (shopUnit == null) return;
        shopUnit.setOpen(false);
    }

    // ── Stock management ──────────────────────────────────────────────────────

    /**
     * Stock the shop with items from the player's inventory.
     *
     * @param playerInventory the player's inventory
     * @param material        material to stock
     * @param quantity        quantity to transfer
     * @param askingPrice     price per unit in COIN
     * @return true if items were transferred successfully
     */
    public boolean stockShop(Inventory playerInventory, Material material, int quantity, int askingPrice) {
        if (shopUnit == null || !shopUnit.isClaimed()) return false;
        if (!playerInventory.hasItem(material) || playerInventory.getItemCount(material) < quantity) {
            return false;
        }
        playerInventory.removeItem(material, quantity);
        shopUnit.addStock(material, quantity);
        shopUnit.setPrice(material, askingPrice);
        return true;
    }

    /**
     * Collect all accumulated cash from the till into the player's inventory.
     *
     * @param playerInventory the player's inventory
     * @return coins collected
     */
    public int collectCash(Inventory playerInventory) {
        if (shopUnit == null) return 0;
        int coins = shopUnit.collectCash();
        if (coins > 0) {
            playerInventory.addItem(Material.COIN, coins);
        }
        return coins;
    }

    // ── Runner ────────────────────────────────────────────────────────────────

    /**
     * Hire an NPC as the shop runner. The NPC must be PUBLIC or YOUTH type.
     *
     * @param npc             the NPC to hire
     * @param playerInventory player inventory (RUNNER_DAILY_WAGE deducted)
     * @return true if the runner was hired successfully
     */
    public boolean hireRunner(NPC npc, Inventory playerInventory) {
        if (shopUnit == null || !shopUnit.isClaimed()) return false;
        if (shopUnit.hasRunner()) return false;
        if (npc.getType() != NPCType.PUBLIC && npc.getType() != NPCType.YOUTH_GANG) return false;
        if (playerInventory.getItemCount(Material.COIN) < RUNNER_DAILY_WAGE) return false;

        playerInventory.removeItem(Material.COIN, RUNNER_DAILY_WAGE);
        shopUnit.setRunnerNpc(npc);
        return true;
    }

    /**
     * Fire the runner NPC.
     */
    public void fireRunner() {
        if (shopUnit == null) return;
        shopUnit.setRunnerNpc(null);
    }

    // ── Protection payment ────────────────────────────────────────────────────

    /**
     * Pay Marchetti protection money (10 COIN/day).
     *
     * @param playerInventory  player's inventory
     * @param factionSystem    faction system for Respect update
     * @param achievementCallback called on {@link AchievementType#PROTECTION_MONEY} unlock
     * @return true if the payment succeeded
     */
    public boolean payProtection(Inventory playerInventory, FactionSystem factionSystem,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        if (playerInventory.getItemCount(Material.COIN) < PROTECTION_COST_PER_DAY) return false;
        playerInventory.removeItem(Material.COIN, PROTECTION_COST_PER_DAY);
        paidProtectionToday = true;

        if (!achievementProtectionMoney && achievementCallback != null) {
            achievementProtectionMoney = true;
            achievementCallback.award(AchievementType.PROTECTION_MONEY);
        }
        return true;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Main update method — call once per frame.
     *
     * @param delta            seconds since last frame
     * @param npcs             all living NPCs in the world
     * @param player           the player entity
     * @param playerInventory  player's inventory
     * @param factionSystem    faction system (for Respect changes)
     * @param notorietySystem  notoriety system (for heat-based gains)
     * @param rumourNetwork    rumour network (for SHOP_NEWS seeding)
     * @param streetSkillSystem street skills (for TRADING perk check)
     * @param achievementCallback called when achievements are unlocked
     */
    public void update(float delta,
                       List<NPC> npcs,
                       Player player,
                       Inventory playerInventory,
                       FactionSystem factionSystem,
                       NotorietySystem notorietySystem,
                       RumourNetwork rumourNetwork,
                       StreetSkillSystem streetSkillSystem,
                       NotorietySystem.AchievementCallback achievementCallback) {

        if (shopUnit == null || !shopUnit.isClaimed() || !shopUnit.isOpen()) return;

        // Advance daily timer (1 in-game day = 1200 real seconds)
        dailyTimer += delta;
        if (dailyTimer >= FenceSystem.IN_GAME_DAY_SECONDS) {
            dailyTimer = 0f;
            onNewDay(playerInventory, factionSystem, npcs, rumourNetwork, achievementCallback);
        }

        // Pirate radio boost
        if (pirateRadioBoostTimer > 0f) {
            pirateRadioBoostTimer -= delta;
        }

        // Customer scan
        customerScanTimer += delta;
        if (customerScanTimer >= CUSTOMER_SCAN_INTERVAL) {
            customerScanTimer = 0f;
            attractCustomers(npcs, streetSkillSystem);
        }

        // Process customer purchases
        processCustomerPurchases(playerInventory, factionSystem, notorietySystem,
                rumourNetwork, achievementCallback);

        // Heat checks
        processHeatThresholds(npcs, player, playerInventory, notorietySystem,
                rumourNetwork, achievementCallback);

        // Marchetti rivalry checks
        processMarchettiRivalry(npcs, factionSystem, achievementCallback);

        // Street Lads alliance
        processStreetLadsAlliance(factionSystem, achievementCallback);
    }

    // ── Daily cycle ───────────────────────────────────────────────────────────

    private void onNewDay(Inventory playerInventory,
                          FactionSystem factionSystem,
                          List<NPC> npcs,
                          RumourNetwork rumourNetwork,
                          NotorietySystem.AchievementCallback achievementCallback) {

        // Reset daily flags
        paidProtectionToday = false;
        shopUnit.resetDailyRevenue();

        // Deduct runner wage
        if (shopUnit.hasRunner() && shopUnit.getRunnerNpc() != null
                && shopUnit.getRunnerNpc().isAlive()) {
            if (playerInventory.getItemCount(Material.COIN) >= RUNNER_DAILY_WAGE) {
                playerInventory.removeItem(Material.COIN, RUNNER_DAILY_WAGE);
            } else {
                // Can't pay runner — fire them
                shopUnit.setRunnerNpc(null);
            }
        }

        // Business rates notice
        if (shopUnit.getDailyRevenue() > BUSINESS_RATES_THRESHOLD && !businessRatesNoticeTriggered) {
            businessRatesNoticeTriggered = true;
            // Seed SHOP_NEWS rumour about council scrutiny
            if (rumourNetwork != null && npcs != null) {
                for (NPC npc : npcs) {
                    if (npc.isAlive() && npc.getType() == NPCType.PUBLIC) {
                        rumourNetwork.addRumour(npc, new Rumour(RumourType.SHOP_NEWS,
                                "There's a dodgy shop raking it in — council's got to know about this."));
                        break;
                    }
                }
            }
        }
    }

    // ── Customer attraction ────────────────────────────────────────────────────

    private void attractCustomers(List<NPC> npcs, StreetSkillSystem streetSkillSystem) {
        if (npcs == null || shopUnit.getStockMap().isEmpty()) return;

        // Check for TRADING skill bonus
        float trafficMultiplier = 1.0f;
        if (streetSkillSystem != null) {
            StreetSkillSystem.Tier tier = streetSkillSystem.getTier(StreetSkillSystem.Skill.TRADING);
            if (tier.getLevel() >= 1) {
                trafficMultiplier *= 1.10f; // Apprentice: +10% stall income
            }
        }

        // Pirate radio boost
        if (pirateRadioBoostTimer > 0f) {
            trafficMultiplier *= 1.25f;
        }

        // Pricing modifier — sample first stocked item vs fence value
        float priceModifier = getPriceModifier();
        trafficMultiplier *= priceModifier;

        if (customerQueue.size() >= MAX_CUSTOMER_QUEUE) return;

        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            if (customerQueue.contains(npc)) continue;
            if (customerQueue.size() >= MAX_CUSTOMER_QUEUE) break;

            // Only attract PUBLIC, PENSIONER-type civilians
            if (npc.getType() != NPCType.PUBLIC && npc.getType() != NPCType.PENSIONER) continue;

            // Distance check (shop has no fixed position; use simple probability model)
            float purchaseChance = CUSTOMER_PURCHASE_CHANCE_PER_MINUTE * trafficMultiplier;
            if (random.nextFloat() < purchaseChance) {
                customerQueue.add(npc);
            }
        }
    }

    /**
     * Returns the traffic modifier based on pricing: undercut = +30%, overpriced = -50%.
     */
    float getPriceModifier() {
        if (shopUnit == null || shopUnit.getPriceMap().isEmpty()) return 1.0f;
        // Sample first priced item
        for (Map.Entry<Material, Integer> entry : shopUnit.getPriceMap().entrySet()) {
            int askingPrice = entry.getValue();
            int fenceValue = getFenceValue(entry.getKey());
            if (askingPrice < fenceValue) {
                return UNDERCUT_TRAFFIC_MULTIPLIER;
            } else if (askingPrice > fenceValue * 2) {
                return OVERPRICED_TRAFFIC_MULTIPLIER;
            } else {
                return 1.0f;
            }
        }
        return 1.0f;
    }

    /**
     * Returns the fence valuation for a material.
     */
    private int getFenceValue(Material material) {
        // Standard fence values for common shop materials
        switch (material) {
            case CIDER:           return 3;
            case TOBACCO:         return 4;
            case ENERGY_DRINK:    return 3;
            case COIN:            return 1;
            default:              return FENCE_VALUE_DEFAULT;
        }
    }

    // ── Customer purchases ─────────────────────────────────────────────────────

    private void processCustomerPurchases(Inventory playerInventory,
                                          FactionSystem factionSystem,
                                          NotorietySystem notorietySystem,
                                          RumourNetwork rumourNetwork,
                                          NotorietySystem.AchievementCallback achievementCallback) {
        List<NPC> completedCustomers = new ArrayList<>();
        for (NPC customer : customerQueue) {
            if (!customer.isAlive()) {
                completedCustomers.add(customer);
                continue;
            }

            // Find something to buy
            Material itemToBuy = null;
            for (Material material : shopUnit.getStockMap().keySet()) {
                if (shopUnit.getStock(material) > 0) {
                    itemToBuy = material;
                    break;
                }
            }

            if (itemToBuy == null) {
                completedCustomers.add(customer);
                continue;
            }

            int price = shopUnit.getPrice(itemToBuy);
            if (price <= 0) price = getFenceValue(itemToBuy);

            shopUnit.removeStock(itemToBuy, 1);
            shopUnit.addCash(price);

            // Add heat for dodgy goods
            if (itemToBuy == Material.CIDER || itemToBuy == Material.TOBACCO
                    || itemToBuy == Material.ENERGY_DRINK) {
                shopUnit.addHeat(HEAT_PER_DODGY_SALE);
            }

            completedCustomers.add(customer);
        }
        customerQueue.removeAll(completedCustomers);

        // Check KERCHING achievement (100 coins in a single day)
        if (!achievementKerching && shopUnit.getDailyRevenue() >= NEWSPAPER_HEADLINE_REVENUE
                && achievementCallback != null) {
            achievementKerching = true;
            achievementCallback.award(AchievementType.KERCHING);
        }
    }

    // ── Heat system ───────────────────────────────────────────────────────────

    private void processHeatThresholds(List<NPC> npcs,
                                       Player player,
                                       Inventory playerInventory,
                                       NotorietySystem notorietySystem,
                                       RumourNetwork rumourNetwork,
                                       NotorietySystem.AchievementCallback achievementCallback) {
        int heat = shopUnit.getHeatLevel();

        if (heat >= HEAT_RAID) {
            executeRaid(playerInventory, notorietySystem, achievementCallback);
            return;
        }

        if (heat >= HEAT_RAID_WARNING) {
            // Seed SHOP_NEWS rumour as raid warning
            if (npcs != null) {
                for (NPC npc : npcs) {
                    if (npc.isAlive() && npc.getType() == NPCType.PUBLIC) {
                        npc.getRumours().add(new Rumour(RumourType.SHOP_NEWS, "A shop got raided!"));
                        break;
                    }
                }
            }
        }

        // Check if runner is spotted by police (adds heat)
        if (shopUnit.hasRunner() && shopUnit.getRunnerNpc() != null
                && shopUnit.getRunnerNpc().isAlive() && npcs != null) {
            for (NPC npc : npcs) {
                if (!npc.isAlive()) continue;
                if (npc.getType() != NPCType.POLICE && npc.getType() != NPCType.UNDERCOVER_POLICE) continue;
                float dist = npc.getPosition().dst(shopUnit.getRunnerNpc().getPosition());
                if (dist <= 8f) {
                    shopUnit.addHeat(HEAT_RUNNER_SPOTTED);
                    break;
                }
            }
        }
    }

    /**
     * Execute a Police Raid: confiscate all stock, close the shop, add Notoriety.
     */
    private void executeRaid(Inventory playerInventory,
                             NotorietySystem notorietySystem,
                             NotorietySystem.AchievementCallback achievementCallback) {
        // Clear all stock
        shopUnit.getStockMap().keySet().forEach(m ->
            shopUnit.removeStock(m, shopUnit.getStock(m)));

        // Empty the till (confiscated)
        shopUnit.collectCash();

        // Close shop
        shopUnit.setOpen(false);
        shopUnit.setClaimed(false);

        // Notoriety gain
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(RAID_NOTORIETY_GAIN, achievementCallback);
        }

        // Fire runner
        shopUnit.setRunnerNpc(null);

        // Reset heat
        shopUnit.reduceHeat(HEAT_RAID);

        // Achievement
        if (!achievementRaided && achievementCallback != null) {
            achievementRaided = true;
            achievementCallback.award(AchievementType.RAIDED);
        }

        shopUnit = null;
    }

    // ── Marchetti rivalry ─────────────────────────────────────────────────────

    private void processMarchettiRivalry(List<NPC> npcs,
                                         FactionSystem factionSystem,
                                         NotorietySystem.AchievementCallback achievementCallback) {
        if (factionSystem == null) return;

        int marchettiRespect = factionSystem.getRespect(Faction.MARCHETTI_CREW);
        boolean isUndercutting = getPriceModifier() > 1.0f; // undercut = traffic bonus

        if (isUndercutting) {
            if (marchettiRespect <= MARCHETTI_ENFORCER_VISIT_RESPECT && !marchettiEnforcerVisitTriggered) {
                marchettiEnforcerVisitTriggered = true;
                if (!achievementPriceWar && achievementCallback != null) {
                    achievementPriceWar = true;
                    achievementCallback.award(AchievementType.PRICE_WAR);
                }
            }
        }
    }

    // ── Street Lads alliance ──────────────────────────────────────────────────

    private void processStreetLadsAlliance(FactionSystem factionSystem,
                                            NotorietySystem.AchievementCallback achievementCallback) {
        if (factionSystem == null || shopUnit.getStockMap().isEmpty()) return;

        boolean hasGreyMarketGoods = shopUnit.getStock(Material.CIDER) > 0
                || shopUnit.getStock(Material.TOBACCO) > 0
                || shopUnit.getStock(Material.ENERGY_DRINK) > 0;

        if (!hasGreyMarketGoods) return;

        // Fair pricing check (not overpriced)
        float priceModifier = getPriceModifier();
        if (priceModifier == OVERPRICED_TRAFFIC_MULTIPLIER) return;

        // Check THE_NEIGHBOURHOOD_SHOP achievement
        int streetLadsRespect = factionSystem.getRespect(Faction.STREET_LADS);
        if (streetLadsRespect >= STREET_LADS_DEFEND_RESPECT && !achievementNeighbourhoodShop
                && achievementCallback != null) {
            achievementNeighbourhoodShop = true;
            achievementCallback.award(AchievementType.THE_NEIGHBOURHOOD_SHOP);
        }
    }

    // ── Pirate radio boost ─────────────────────────────────────────────────────

    /**
     * Activate a pirate radio traffic boost for one in-game day.
     */
    public void activatePirateRadioBoost() {
        pirateRadioBoostTimer = PIRATE_RADIO_BOOST_DURATION;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /**
     * Returns the current shop unit, or null if no shop is owned.
     */
    public ShopUnit getShopUnit() {
        return shopUnit;
    }

    /**
     * Returns true if the player currently owns a shop.
     */
    public boolean hasShop() {
        return shopUnit != null && shopUnit.isClaimed();
    }

    /**
     * Returns true if the pirate radio boost is active.
     */
    public boolean isPirateRadioBoostActive() {
        return pirateRadioBoostTimer > 0f;
    }

    /** Returns true if the shop is claimed and currently open for business. */
    public boolean isShopOpen() {
        return shopUnit != null && shopUnit.isClaimed() && shopUnit.isOpen();
    }

    /** Returns the daily revenue of the current shop, or 0 if no shop. */
    public int getDailyRevenue() {
        return shopUnit != null ? shopUnit.getDailyRevenue() : 0;
    }

    /** Returns the heat level of the current shop, or 0 if no shop. */
    public int getHeat() {
        return shopUnit != null ? shopUnit.getHeatLevel() : 0;
    }

    /**
     * Record a sale manually — adds heat for dodgy goods.
     * Used by integration tests and direct sale flows.
     *
     * @param material        the material sold
     * @param notorietySystem not used directly here but kept for API consistency
     */
    public void recordSale(Material material, NotorietySystem notorietySystem) {
        if (shopUnit == null) return;
        if (material == Material.CIDER || material == Material.TOBACCO
                || material == Material.ENERGY_DRINK) {
            shopUnit.addHeat(HEAT_PER_DODGY_SALE);
        }
    }

    /** For testing: force the heat level of the current shop. */
    public void setHeatForTesting(int heat) {
        if (shopUnit == null) {
            shopUnit = new ShopUnit();
            shopUnit.setClaimed(true);
            shopUnit.setOpen(true);
        }
        // Set heat by reducing to 0 first then adding the desired amount
        shopUnit.reduceHeat(100);
        shopUnit.addHeat(heat);
    }

    /** For testing: expose the customer queue size. */
    public int getCustomerQueueSize() {
        return customerQueue.size();
    }

    /** For testing: override shopUnit. */
    void setShopUnitForTesting(ShopUnit shopUnit) {
        this.shopUnit = shopUnit;
    }
}

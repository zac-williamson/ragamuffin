package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.Faction;
import ragamuffin.core.FactionSystem;
import ragamuffin.core.PropertySystem;
import ragamuffin.core.RumourNetwork;
import ragamuffin.core.TimeSystem;
import ragamuffin.core.TurfMap;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #712 Integration Tests — Slumlord Property Ownership System
 *
 * 8 integration tests covering:
 * 1. Property purchase via ESTATE_AGENT costs COINs, yields DEED, records ownership
 * 2. Daily decay reduces Condition; neglected properties become derelict
 * 3. Repair with BRICK or PAINT_TIN raises Condition; SLUM_CLEARANCE achievement fires at 100
 * 4. Faction takeover: THUG NPC set to FLEEING when EVICTION_NOTICE used
 * 5. Council rates: missed payments trigger compulsory purchase after 2 periods
 * 6. ESTATE_AGENT schedule: closed on weekends and outside 09:00–17:00
 * 7. Property cap: buying a 6th building denied; THATCHER_WOULDNT achievement fires
 * 8. Owning the bookies flips fruit-machine odds flag; faction Respect drops on purchase
 */
class Issue712SlumlordPropertyTest {

    private PropertySystem propertySystem;
    private FactionSystem factionSystem;
    private AchievementSystem achievementSystem;
    private RumourNetwork rumourNetwork;
    private TurfMap turfMap;
    private Inventory inventory;
    private TimeSystem timeSystem;
    private List<NPC> npcs;

    @BeforeEach
    void setUp() {
        turfMap         = new TurfMap(200, 200);
        rumourNetwork   = new RumourNetwork(new Random(42));
        factionSystem   = new FactionSystem(turfMap, rumourNetwork, new Random(42));
        achievementSystem = new AchievementSystem();
        propertySystem  = new PropertySystem(factionSystem, achievementSystem, rumourNetwork);
        inventory       = new Inventory(36);
        timeSystem      = new TimeSystem(10f); // 10:00 AM, day 1 (Wednesday)
        npcs            = new ArrayList<>();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Test 1: Purchase a building
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Buying a building costs {@link PropertySystem#BASE_PURCHASE_PRICE} COINs,
     * adds a DEED to the player's inventory, records the property, and fires the
     * FIRST_PROPERTY achievement.
     */
    @Test
    void test1_PurchasePropertyCostsCoinAndYieldsDeed() {
        // Give player enough coins
        inventory.addItem(Material.COIN, PropertySystem.BASE_PURCHASE_PRICE + 5);

        String result = propertySystem.purchaseProperty(
                LandmarkType.TERRACED_HOUSE, 50, 50,
                inventory, npcs, null);

        assertNotNull(result, "Purchase should return a result message");
        assertFalse(result.contains("afford"), "Should not return an 'afford' failure");

        // Coins deducted
        assertEquals(5, inventory.getItemCount(Material.COIN),
                "Coin cost should be deducted from inventory");

        // DEED added
        assertEquals(1, inventory.getItemCount(Material.DEED),
                "A DEED should be added to inventory on purchase");

        // Property recorded
        assertEquals(1, propertySystem.getPropertyCount(),
                "Property count should be 1 after purchase");

        // FIRST_PROPERTY achievement unlocked
        assertTrue(achievementSystem.isUnlocked(AchievementType.FIRST_PROPERTY),
                "FIRST_PROPERTY achievement should unlock on first purchase");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Test 2: Daily decay — property becomes derelict without maintenance
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Each in-game day tick reduces Condition by {@link PropertySystem#DECAY_PER_DAY}.
     * After enough ticks without repair the building crosses
     * {@link PropertySystem#DERELICT_THRESHOLD} and isDerelict() returns true.
     */
    @Test
    void test2_DailyDecayEventuallyMakesPropertyDerelict() {
        inventory.addItem(Material.COIN, PropertySystem.BASE_PURCHASE_PRICE);
        propertySystem.purchaseProperty(LandmarkType.TERRACED_HOUSE, 50, 50,
                inventory, npcs, null);

        PropertySystem.OwnedProperty prop = propertySystem.getProperties().get(0);
        int initialCondition = prop.getCondition();
        assertTrue(initialCondition > PropertySystem.DERELICT_THRESHOLD,
                "Freshly purchased building should not start derelict");

        // Simulate days until derelict
        int daysNeeded = (int) Math.ceil(
                (initialCondition - PropertySystem.DERELICT_THRESHOLD) /
                (float) PropertySystem.DECAY_PER_DAY) + 1;

        for (int day = 1; day <= daysNeeded; day++) {
            propertySystem.onDayTick(day, inventory, turfMap, npcs);
        }

        assertTrue(prop.isDerelict(),
                "Property should become derelict after enough days without repair");
        assertEquals(0, prop.getDailyIncome(),
                "Derelict property should produce zero income");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Test 3: Repair raises Condition; SLUM_CLEARANCE achievement at 100
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Using BRICK or PAINT_TIN on a building raises Condition by
     * {@link PropertySystem#CONDITION_PER_REPAIR}.  Reaching 100 triggers the
     * SLUM_CLEARANCE achievement.
     */
    @Test
    void test3_RepairRaisesConditionAndUnlocksAchievement() {
        inventory.addItem(Material.COIN, PropertySystem.BASE_PURCHASE_PRICE);
        propertySystem.purchaseProperty(LandmarkType.TERRACED_HOUSE, 50, 50,
                inventory, npcs, null);

        PropertySystem.OwnedProperty prop = propertySystem.getProperties().get(0);

        // Force condition low so we can verify increments
        prop.setCondition(50);

        // Repair with PAINT_TIN
        inventory.addItem(Material.PAINT_TIN, 10);

        // Repair until full
        while (prop.getCondition() < PropertySystem.MAX_CONDITION) {
            String msg = propertySystem.repairProperty(50, 50, inventory);
            assertNotNull(msg, "Repair message should not be null");
        }

        assertEquals(PropertySystem.MAX_CONDITION, prop.getCondition(),
                "Condition should be at MAX after full repair");
        assertTrue(achievementSystem.isUnlocked(AchievementType.SLUM_CLEARANCE),
                "SLUM_CLEARANCE achievement should unlock when building reaches full condition");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Test 4: Faction takeover — EVICTION_NOTICE removes THUG NPCs
    // ────────────────────────────────────────────────────────────────────────

    /**
     * When a takeover is triggered, isUnderTakeover() returns true.
     * Using an EVICTION_NOTICE item clears the takeover flag and sets nearby
     * THUG NPCs to FLEEING.
     */
    @Test
    void test4_EvictionNoticeRemovesThugFromTakeover() {
        inventory.addItem(Material.COIN, PropertySystem.BASE_PURCHASE_PRICE);
        propertySystem.purchaseProperty(LandmarkType.TERRACED_HOUSE, 50, 50,
                inventory, npcs, null);

        // Trigger takeover
        propertySystem.triggerTakeoverAttempt(50, 50);

        PropertySystem.OwnedProperty prop = propertySystem.getProperties().get(0);
        assertTrue(prop.isUnderTakeover(), "Property should be under takeover after attempt");

        // Spawn a THUG near the property
        NPC thug = new NPC(NPCType.THUG, "thug_1", 51f, 1f, 51f);
        thug.setState(NPCState.IDLE);
        npcs.add(thug);

        // Give player an eviction notice and use it
        inventory.addItem(Material.EVICTION_NOTICE, 1);
        String result = propertySystem.useEvictionNotice(50, 50, inventory, npcs);

        assertNotNull(result, "Eviction notice use should return a message");
        assertFalse(result.contains("need"), "Should not return a 'need' failure");

        assertFalse(prop.isUnderTakeover(),
                "Property should no longer be under takeover after eviction notice");
        assertEquals(NPCState.FLEEING, thug.getState(),
                "Nearby THUG NPC should be set to FLEEING after eviction");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Test 5: Council rates — missed payments cause compulsory purchase
    // ────────────────────────────────────────────────────────────────────────

    /**
     * If the player never pays rates, after 2 × {@link PropertySystem#RATES_PERIOD_DAYS}
     * the property is compulsorily purchased and removed from the player's portfolio.
     * The RATES_DODGER achievement fires on the compulsory purchase.
     */
    @Test
    void test5_MissedRatesTriggerCompulsoryPurchase() {
        inventory.addItem(Material.COIN, PropertySystem.BASE_PURCHASE_PRICE);
        propertySystem.purchaseProperty(LandmarkType.TERRACED_HOUSE, 50, 50,
                inventory, npcs, null);

        assertEquals(1, propertySystem.getPropertyCount(),
                "Player should own 1 property before rates defaulting");

        // Simulate days covering 2 complete rate periods without paying
        int totalDays = PropertySystem.RATES_PERIOD_DAYS *
                (PropertySystem.COMPULSORY_PURCHASE_MISSED_PERIODS + 1);

        for (int day = 1; day <= totalDays; day++) {
            propertySystem.onDayTick(day, inventory, turfMap, npcs);
        }

        assertEquals(0, propertySystem.getPropertyCount(),
                "Property should be compulsorily purchased after 2 missed rate periods");
        assertTrue(achievementSystem.isUnlocked(AchievementType.RATES_DODGER),
                "RATES_DODGER achievement should unlock on compulsory purchase");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Test 6: ESTATE_AGENT schedule
    // ────────────────────────────────────────────────────────────────────────

    /**
     * The ESTATE_AGENT NPC is open Mon–Fri, 09:00–17:00 only.
     * On Saturday/Sunday, or outside business hours, isEstateAgentOpen() returns false.
     *
     * Schedule rule: weekday = (dayCount + 2) % 7; 0=Mon..4=Fri are open, 5=Sat/6=Sun closed.
     * Day 1 => (1+2)%7 = 3 = Thursday (open).
     */
    @Test
    void test6_EstateAgentScheduleWeekdaysOnly() {
        // timeSystem starts at 10:00 on day 1 (Thursday by our formula) — should be open
        assertTrue(propertySystem.isEstateAgentOpen(timeSystem),
                "Estate agent should be open on a weekday at 10:00");

        // Day 3 => (3+2)%7 = 5 = Saturday — closed
        TimeSystem saturdayMorning = new TimeSystem(10f);
        // Advance to day 3 by simulating two day transitions
        // (dayCount starts at 1, update() increments it when time wraps 24h)
        saturdayMorning.update(24f / saturdayMorning.getTimeSpeed()); // advance 1 day
        saturdayMorning.update(24f / saturdayMorning.getTimeSpeed()); // advance to day 3
        assertFalse(propertySystem.isEstateAgentOpen(saturdayMorning),
                "Estate agent should be closed on Saturday (day 3)");

        // Day 1, time 08:30 — weekday but before opening
        TimeSystem earlyMorning = new TimeSystem(8.5f);
        assertFalse(propertySystem.isEstateAgentOpen(earlyMorning),
                "Estate agent should be closed before 09:00");

        // Day 1, time 17:00 — weekday but at closing time (exclusive upper bound)
        TimeSystem closingTime = new TimeSystem(17.0f);
        assertFalse(propertySystem.isEstateAgentOpen(closingTime),
                "Estate agent should be closed at exactly 17:00");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Test 7: Property cap — 6th purchase denied; THATCHER_WOULDNT achievement
    // ────────────────────────────────────────────────────────────────────────

    /**
     * The player may own at most {@link PropertySystem#MAX_PROPERTIES} buildings.
     * Attempting a 6th purchase returns a denial message and fires the
     * THATCHER_WOULDNT achievement.
     */
    @Test
    void test7_PropertyCapDeniesExcessPurchase() {
        // Buy up to the cap
        for (int i = 0; i < PropertySystem.MAX_PROPERTIES; i++) {
            inventory.addItem(Material.COIN, PropertySystem.BASE_PURCHASE_PRICE);
            String msg = propertySystem.purchaseProperty(
                    LandmarkType.TERRACED_HOUSE, i * 20, 0,
                    inventory, npcs, null);
            assertFalse(msg.contains("afford"), "Purchase " + (i + 1) + " should succeed");
        }

        assertEquals(PropertySystem.MAX_PROPERTIES, propertySystem.getPropertyCount(),
                "Should own exactly MAX_PROPERTIES after buying up to the cap");

        // ARMCHAIR_LANDLORD fires at the cap
        assertTrue(achievementSystem.isUnlocked(AchievementType.ARMCHAIR_LANDLORD),
                "ARMCHAIR_LANDLORD achievement should unlock when cap is reached");

        // Attempt one more purchase
        inventory.addItem(Material.COIN, PropertySystem.BASE_PURCHASE_PRICE);
        String result = propertySystem.purchaseProperty(
                LandmarkType.TERRACED_HOUSE, 200, 200,
                inventory, npcs, null);

        assertNotNull(result, "Purchase attempt beyond cap should return a message");
        assertTrue(result.contains("enough") || result.contains("already") || result.contains("5"),
                "Denial message should reference the cap limit: " + result);

        assertEquals(PropertySystem.MAX_PROPERTIES, propertySystem.getPropertyCount(),
                "Property count should still be at MAX after denied purchase");

        // THATCHER_WOULDNT fires when cap is hit
        assertTrue(achievementSystem.isUnlocked(AchievementType.THATCHER_WOULDNT),
                "THATCHER_WOULDNT achievement should unlock when cap is hit");

        // Tooltip queued
        String tooltip = propertySystem.pollTooltip();
        assertNotNull(tooltip, "A cap tooltip should be queued");
        assertTrue(tooltip.contains("Thatcher") || tooltip.contains("five") || tooltip.contains("Five"),
                "Cap tooltip should reference Thatcher: " + tooltip);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Test 8: Buying faction-owned building drops faction Respect
    // ────────────────────────────────────────────────────────────────────────

    /**
     * When the player buys a building that belongs to a faction, that faction's
     * Respect drops by {@link PropertySystem#FACTION_PURCHASE_RESPECT_PENALTY}.
     * Owning the bookies returns true from ownsBookies().
     */
    @Test
    void test8_FactionRespectDropsOnPurchaseAndBookiesFlag() {
        int respectBefore = factionSystem.getRespect(Faction.MARCHETTI_CREW);

        inventory.addItem(Material.COIN, PropertySystem.BASE_PURCHASE_PRICE);
        propertySystem.purchaseProperty(
                LandmarkType.BOOKIES, 100, 100,
                inventory, npcs, Faction.MARCHETTI_CREW);

        int respectAfter = factionSystem.getRespect(Faction.MARCHETTI_CREW);
        assertEquals(respectBefore - PropertySystem.FACTION_PURCHASE_RESPECT_PENALTY,
                respectAfter,
                "Marchetti Crew Respect should drop by FACTION_PURCHASE_RESPECT_PENALTY " +
                "when the player buys their building");

        assertTrue(propertySystem.ownsBookies(),
                "ownsBookies() should return true after purchasing BOOKIES");
    }
}

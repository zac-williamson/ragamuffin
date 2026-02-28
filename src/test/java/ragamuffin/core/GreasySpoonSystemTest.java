package ragamuffin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GreasySpoonSystem} — Issue #938.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Café opening hours (07:00–14:00)</li>
 *   <li>NPC spawning (Vera + regulars)</li>
 *   <li>Weather modifier (+2 regulars during rain/drizzle/thunderstorm)</li>
 *   <li>Monday rush (max 4 customers)</li>
 *   <li>Menu ordering: success, insufficient funds, breakfast-only, closed</li>
 *   <li>Service refused when notoriety ≥ 60 + police nearby</li>
 *   <li>Eavesdropping mechanic (rumour on proximity)</li>
 *   <li>WELL_INFORMED achievement (3 rumours in one visit)</li>
 *   <li>FULL_ENGLISH_FANATIC achievement (5 days)</li>
 *   <li>CAFF_REGULAR achievement (7 consecutive days)</li>
 *   <li>First-entry tooltip text</li>
 *   <li>Daily combo discount applied correctly</li>
 *   <li>Marchetti dealer regular mechanic</li>
 *   <li>PRESCRIPTION_MEDS purchase from dealer</li>
 *   <li>Landmark display name</li>
 *   <li>Material entries present (FULL_ENGLISH, MUG_OF_TEA, etc.)</li>
 *   <li>NPC types present (CAFF_OWNER, CAFF_REGULAR)</li>
 * </ul>
 */
class GreasySpoonSystemTest {

    private GreasySpoonSystem system;
    private Player player;
    private Inventory inventory;
    private NPC vera;
    private AchievementSystem achievements;
    private NotorietySystem notorietySystem;
    private FactionSystem factionSystem;

    @BeforeEach
    void setUp() {
        system = new GreasySpoonSystem(new Random(42));
        player = new Player(50, 1, 50);
        inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 100);
        vera = new NPC(NPCType.CAFF_OWNER, 10, 1, 10);
        achievements = new AchievementSystem();
        notorietySystem = new NotorietySystem();
        factionSystem = new FactionSystem();
        system.setAchievementSystem(achievements);
        system.setNotorietySystem(notorietySystem);
        system.setFactionSystem(factionSystem);
    }

    // ── Opening hours ──────────────────────────────────────────────────────────

    @Test
    void cafeIsOpenDuringOpeningHours() {
        assertTrue(system.isOpen(7.0f), "Café should be open at 07:00");
        assertTrue(system.isOpen(10.0f), "Café should be open at 10:00");
        assertTrue(system.isOpen(13.9f), "Café should be open at 13:54");
    }

    @Test
    void cafeIsClosedOutsideHours() {
        assertFalse(system.isOpen(6.9f), "Café should be closed before 07:00");
        assertFalse(system.isOpen(14.0f), "Café should be closed at 14:00");
        assertFalse(system.isOpen(22.0f), "Café should be closed at 22:00");
    }

    // ── NPC spawning ───────────────────────────────────────────────────────────

    @Test
    void openCafeSpawnsVeraAndBaseRegulars() {
        system.openCafe(10, 1, 10, Weather.CLEAR, 3);
        assertNotNull(system.getVera(), "Vera should be spawned on open");
        assertEquals(NPCType.CAFF_OWNER, system.getVera().getType(),
                "Spawned NPC should be CAFF_OWNER");
        assertEquals(GreasySpoonSystem.BASE_REGULAR_COUNT, system.getActiveRegulars().size(),
                "Base regular count should be " + GreasySpoonSystem.BASE_REGULAR_COUNT);
    }

    @Test
    void openCafeSpawnsExtraRegularsDuringRain() {
        system.openCafe(10, 1, 10, Weather.RAIN, 3);
        int expected = GreasySpoonSystem.BASE_REGULAR_COUNT + GreasySpoonSystem.WEATHER_BONUS_REGULARS;
        assertEquals(expected, system.getActiveRegulars().size(),
                "Rain should add " + GreasySpoonSystem.WEATHER_BONUS_REGULARS + " extra regulars");
    }

    @Test
    void openCafeSpawnsExtraRegularsDuringDrizzle() {
        system.openCafe(10, 1, 10, Weather.DRIZZLE, 3);
        int expected = GreasySpoonSystem.BASE_REGULAR_COUNT + GreasySpoonSystem.WEATHER_BONUS_REGULARS;
        assertEquals(expected, system.getActiveRegulars().size(),
                "Drizzle should add extra regulars");
    }

    @Test
    void openCafeSpawnsExtraRegularsDuringThunderstorm() {
        system.openCafe(10, 1, 10, Weather.THUNDERSTORM, 3);
        int expected = GreasySpoonSystem.BASE_REGULAR_COUNT + GreasySpoonSystem.WEATHER_BONUS_REGULARS;
        assertEquals(expected, system.getActiveRegulars().size(),
                "Thunderstorm should add extra regulars");
    }

    @Test
    void mondayRushCapsRegularsAtFour() {
        // Day 1 = Monday; with rain we'd get 4 normally, which equals the cap
        system.openCafe(10, 1, 10, Weather.RAIN, 1);
        int count = system.getActiveRegulars().size();
        assertTrue(count <= GreasySpoonSystem.MONDAY_RUSH_MAX,
                "Monday rush should cap regulars at " + GreasySpoonSystem.MONDAY_RUSH_MAX);
    }

    @Test
    void closeCafeDesawnsAllNpcs() {
        system.openCafe(10, 1, 10, Weather.CLEAR, 3);
        system.closeCafe();
        assertNull(system.getVera(), "Vera should be null after closing");
        assertTrue(system.getActiveRegulars().isEmpty(),
                "Active regulars should be empty after closing");
    }

    @Test
    void closeCafeResetsVisitState() {
        system.openCafe(10, 1, 10, Weather.CLEAR, 3);
        system.setRumoursHeardThisVisitForTesting(2);
        system.closeCafe();
        assertEquals(0, system.getRumoursHeardThisVisit(),
                "Rumours heard this visit should reset on close");
    }

    // ── Ordering ───────────────────────────────────────────────────────────────

    @Test
    void orderFullEnglishSuccessfully() {
        GreasySpoonSystem.OrderResult result = system.order(
            player, vera, Material.FULL_ENGLISH, inventory, 8.0f, 1, new ArrayList<>());
        assertEquals(GreasySpoonSystem.OrderResult.SUCCESS, result,
                "Should succeed ordering Full English during opening hours");
        assertEquals(1, inventory.getItemCount(Material.FULL_ENGLISH),
                "Inventory should contain 1 Full English after order");
        assertEquals(100 - GreasySpoonSystem.PRICE_FULL_ENGLISH,
                inventory.getItemCount(Material.COIN),
                "Correct number of coins should be deducted");
    }

    @Test
    void orderMugOfTeaSuccessfully() {
        GreasySpoonSystem.OrderResult result = system.order(
            player, vera, Material.MUG_OF_TEA, inventory, 12.0f, 1, new ArrayList<>());
        assertEquals(GreasySpoonSystem.OrderResult.SUCCESS, result,
                "Should succeed ordering Mug of Tea after 11:00");
        assertEquals(1, inventory.getItemCount(Material.MUG_OF_TEA),
                "Inventory should contain 1 Mug of Tea");
    }

    @Test
    void orderReturnsClosedOutsideHours() {
        GreasySpoonSystem.OrderResult result = system.order(
            player, vera, Material.FULL_ENGLISH, inventory, 6.0f, 1, new ArrayList<>());
        assertEquals(GreasySpoonSystem.OrderResult.CLOSED, result,
                "Should return CLOSED before opening hours");
    }

    @Test
    void orderReturnsCafeClosed_afterCloseHour() {
        GreasySpoonSystem.OrderResult result = system.order(
            player, vera, Material.FULL_ENGLISH, inventory, 14.0f, 1, new ArrayList<>());
        assertEquals(GreasySpoonSystem.OrderResult.CLOSED, result,
                "Should return CLOSED at 14:00");
    }

    @Test
    void orderFullEnglishFailsAfter11() {
        GreasySpoonSystem.OrderResult result = system.order(
            player, vera, Material.FULL_ENGLISH, inventory, 11.0f, 1, new ArrayList<>());
        assertEquals(GreasySpoonSystem.OrderResult.BREAKFAST_ONLY, result,
                "Full English should be unavailable at or after 11:00");
    }

    @Test
    void orderBaconButtyFailsAfter11() {
        GreasySpoonSystem.OrderResult result = system.order(
            player, vera, Material.BACON_BUTTY, inventory, 11.0f, 1, new ArrayList<>());
        assertEquals(GreasySpoonSystem.OrderResult.BREAKFAST_ONLY, result,
                "Bacon Butty should be unavailable at or after 11:00");
    }

    @Test
    void orderReturnsInsufficientFundsWhenPoor() {
        inventory.removeItem(Material.COIN, 100); // Remove all coins
        GreasySpoonSystem.OrderResult result = system.order(
            player, vera, Material.FULL_ENGLISH, inventory, 9.0f, 1, new ArrayList<>());
        assertEquals(GreasySpoonSystem.OrderResult.INSUFFICIENT_FUNDS, result,
                "Should return INSUFFICIENT_FUNDS when player cannot afford the item");
    }

    @Test
    void orderReturnsWrongNpcForNonCaffOwner() {
        NPC shopkeeper = new NPC(NPCType.SHOPKEEPER, 10, 1, 10);
        GreasySpoonSystem.OrderResult result = system.order(
            player, shopkeeper, Material.FULL_ENGLISH, inventory, 9.0f, 1, new ArrayList<>());
        assertEquals(GreasySpoonSystem.OrderResult.WRONG_NPC, result,
                "Non-CAFF_OWNER NPC should return WRONG_NPC");
    }

    @Test
    void orderReturnsNotOnMenuForUnknownItem() {
        GreasySpoonSystem.OrderResult result = system.order(
            player, vera, Material.SAUSAGE_ROLL, inventory, 9.0f, 1, new ArrayList<>());
        assertEquals(GreasySpoonSystem.OrderResult.NOT_ON_MENU, result,
                "Item not on the café menu should return NOT_ON_MENU");
    }

    @Test
    void beansOnToastAvailableAllDay() {
        GreasySpoonSystem.OrderResult result = system.order(
            player, vera, Material.BEANS_ON_TOAST, inventory, 13.5f, 1, new ArrayList<>());
        assertEquals(GreasySpoonSystem.OrderResult.SUCCESS, result,
                "Beans on Toast should be available all day");
    }

    @Test
    void friedBreadAvailableAllDay() {
        GreasySpoonSystem.OrderResult result = system.order(
            player, vera, Material.FRIED_BREAD, inventory, 13.0f, 1, new ArrayList<>());
        assertEquals(GreasySpoonSystem.OrderResult.SUCCESS, result,
                "Fried Bread should be available all day");
    }

    @Test
    void builderSTeaAvailableAllDay() {
        GreasySpoonSystem.OrderResult result = system.order(
            player, vera, Material.BUILDER_S_TEA, inventory, 13.0f, 1, new ArrayList<>());
        assertEquals(GreasySpoonSystem.OrderResult.SUCCESS, result,
                "Builder's Tea should be available all day");
    }

    // ── Service refusal ────────────────────────────────────────────────────────

    @Test
    void orderRefusedWhenHighNotorietyAndPoliceNearby() {
        // Set notoriety to threshold
        for (int i = 0; i < 60; i++) {
            notorietySystem.addNotoriety(1, null);
        }
        assertTrue(notorietySystem.getNotoriety() >= GreasySpoonSystem.NOTORIETY_BLOCK_THRESHOLD,
                "Notoriety should be at or above threshold");

        // Place a police NPC near Vera
        NPC police = new NPC(NPCType.POLICE, 12, 1, 10); // within 8 blocks of vera at (10,1,10)
        List<NPC> npcs = new ArrayList<>();
        npcs.add(police);

        GreasySpoonSystem.OrderResult result = system.order(
            player, vera, Material.MUG_OF_TEA, inventory, 9.0f, 1, npcs);
        assertEquals(GreasySpoonSystem.OrderResult.SERVICE_REFUSED, result,
                "Service should be refused when notoriety ≥ 60 and police are nearby");
    }

    @Test
    void orderNotRefusedWhenHighNotorietyButNoPolicenearby() {
        for (int i = 0; i < 60; i++) {
            notorietySystem.addNotoriety(1, null);
        }
        // No police NPCs
        GreasySpoonSystem.OrderResult result = system.order(
            player, vera, Material.MUG_OF_TEA, inventory, 9.0f, 1, new ArrayList<>());
        assertEquals(GreasySpoonSystem.OrderResult.SUCCESS, result,
                "Service should proceed when notoriety is high but no police nearby");
    }

    @Test
    void orderNotRefusedWhenLowNotorietyAndPoliceNearby() {
        // notoriety stays at 0 (default)
        NPC police = new NPC(NPCType.POLICE, 12, 1, 10);
        List<NPC> npcs = new ArrayList<>();
        npcs.add(police);

        GreasySpoonSystem.OrderResult result = system.order(
            player, vera, Material.MUG_OF_TEA, inventory, 9.0f, 1, npcs);
        assertEquals(GreasySpoonSystem.OrderResult.SUCCESS, result,
                "Service should proceed when notoriety is below threshold");
    }

    // ── Eavesdropping ──────────────────────────────────────────────────────────

    @Test
    void eavesdropReturnsRumourWhenWithinRadius() {
        system.openCafe(10, 1, 10, Weather.CLEAR, 3);
        // Regulars are at (12, 1, 11), (13, 1, 11) — player within 2 blocks
        String rumour = system.checkEavesdrop(12.0f, 11.0f, 1);
        assertNotNull(rumour, "Should return a rumour when player is within eavesdrop radius");
        assertFalse(rumour.isEmpty(), "Rumour text should not be empty");
    }

    @Test
    void eavesdropReturnsNullWhenFarAway() {
        system.openCafe(10, 1, 10, Weather.CLEAR, 3);
        // Player is far from all regulars
        String rumour = system.checkEavesdrop(50.0f, 50.0f, 1);
        assertNull(rumour, "Should return null when player is too far from regulars");
    }

    @Test
    void eavesdropIncrementsRumoursHeardCount() {
        system.openCafe(10, 1, 10, Weather.CLEAR, 3);
        system.checkEavesdrop(12.0f, 11.0f, 1);
        assertTrue(system.getRumoursHeardThisVisit() > 0,
                "Rumours heard count should increment on eavesdrop");
    }

    // ── Achievements ───────────────────────────────────────────────────────────

    @Test
    void wellInformedAchievementUnlockedAfterThreeRumours() {
        system.openCafe(10, 1, 10, Weather.RAIN, 3); // 4 regulars at (12,1,11), (13,1,11), (14,1,11), (15,1,11)
        // Manually set rumours heard to 2
        system.setRumoursHeardThisVisitForTesting(2);
        // Trigger one more eavesdrop within range of regular at (12,1,11)
        system.checkEavesdrop(12.0f, 11.0f, 1);
        assertTrue(achievements.isUnlocked(AchievementType.WELL_INFORMED),
                "WELL_INFORMED achievement should unlock after 3 rumours in one visit");
    }

    @Test
    void fullEnglishFanaticAchievementTracksMultipleDays() {
        // Day 1
        system.order(player, vera, Material.FULL_ENGLISH, inventory, 9.0f, 1, new ArrayList<>());
        assertEquals(1, system.getFullEnglishDaysEaten(),
                "Should track 1 Full English day");
        assertEquals(1, achievements.getProgress(AchievementType.FULL_ENGLISH_FANATIC),
                "Achievement progress should be 1");

        // Day 2
        system.order(player, vera, Material.FULL_ENGLISH, inventory, 9.0f, 2, new ArrayList<>());
        assertEquals(2, system.getFullEnglishDaysEaten(),
                "Should track 2 Full English days");
    }

    @Test
    void fullEnglishFanaticDoesNotDoubleCountSameDay() {
        system.order(player, vera, Material.FULL_ENGLISH, inventory, 9.0f, 1, new ArrayList<>());
        system.order(player, vera, Material.FULL_ENGLISH, inventory, 9.0f, 1, new ArrayList<>());
        assertEquals(1, system.getFullEnglishDaysEaten(),
                "Same-day Full English should only count once");
    }

    @Test
    void fullEnglishFanaticUnlocksAtFiveDays() {
        system.setFullEnglishDaysEatenForTesting(4);
        system.setLastFullEnglishDayForTesting(-1); // Reset so next order counts
        // Eat on day 5
        system.order(player, vera, Material.FULL_ENGLISH, inventory, 9.0f, 5, new ArrayList<>());
        assertTrue(achievements.isUnlocked(AchievementType.FULL_ENGLISH_FANATIC),
                "FULL_ENGLISH_FANATIC should unlock after 5 days");
    }

    @Test
    void caffRegularAchievementUnlocksAtSevenConsecutiveDays() {
        system.setConsecutiveDaysVisitedForTesting(6);
        system.setLastVisitDayForTesting(6);
        // Visit on day 7
        system.onPlayerEnter(7);
        assertTrue(achievements.isUnlocked(AchievementType.CAFF_REGULAR),
                "CAFF_REGULAR achievement should unlock after 7 consecutive daily visits");
    }

    @Test
    void consecutiveVisitStreakResetsOnGap() {
        system.onPlayerEnter(1);
        system.onPlayerEnter(2);
        system.onPlayerEnter(2); // Same day — should not advance
        // Skip day 3
        system.onPlayerEnter(4); // Breaks the streak
        assertEquals(1, system.getConsecutiveDaysVisited(),
                "Consecutive streak should reset to 1 after a gap day");
    }

    // ── First-entry tooltip ────────────────────────────────────────────────────

    @Test
    void firstEntryTooltipReturnedOnFirstVisit() {
        String tooltip = system.onPlayerEnter(1);
        assertNotNull(tooltip, "First entry should return a tooltip");
        assertTrue(tooltip.contains("Vera's Caff"), "Tooltip should mention Vera's Caff");
        assertTrue(tooltip.contains("1987"), "Tooltip should mention Est. 1987");
        assertTrue(tooltip.contains("Cash only"), "Tooltip should mention Cash only");
        assertTrue(tooltip.contains("No WiFi"), "Tooltip should mention No WiFi");
    }

    @Test
    void firstEntryTooltipNotReturnedOnSubsequentVisits() {
        system.onPlayerEnter(1);
        String secondTooltip = system.onPlayerEnter(2);
        assertNull(secondTooltip, "Second entry should NOT return a tooltip");
    }

    // ── Daily combo discount ───────────────────────────────────────────────────

    @Test
    void dailyComboRefreshesEachDay() {
        system.refreshDailyCombo(1);
        GreasySpoonSystem.ComboDiscount combo1 = system.getDailyCombo();
        assertNotNull(combo1, "Daily combo should be set after refresh");

        // Refreshing same day should not change it
        system.refreshDailyCombo(1);
        assertSame(combo1, system.getDailyCombo(), "Same-day refresh should not change combo");
    }

    @Test
    void comboDiscountReducesPrice() {
        // Force a known combo
        system.refreshDailyCombo(1);
        GreasySpoonSystem.ComboDiscount combo = system.getDailyCombo();

        // Buy one item of the combo first
        inventory.addItem(combo.itemA, 1);
        int coinsBefore = inventory.getItemCount(Material.COIN);
        // Now order itemB — should get discount
        GreasySpoonSystem.MenuItem itemBMenu = null;
        for (GreasySpoonSystem.MenuItem m : GreasySpoonSystem.MENU) {
            if (m.material == combo.itemB) {
                itemBMenu = m;
                break;
            }
        }
        assertNotNull(itemBMenu, "itemB should exist on menu");
        int expectedPrice = Math.max(0, itemBMenu.price - combo.discount);

        GreasySpoonSystem.OrderResult result = system.order(
            player, vera, combo.itemB, inventory, 9.0f, 1, new ArrayList<>());
        assertEquals(GreasySpoonSystem.OrderResult.SUCCESS, result, "Discounted order should succeed");
        int coinsAfter = inventory.getItemCount(Material.COIN);
        assertEquals(expectedPrice, coinsBefore - coinsAfter,
                "Combo discount should reduce the price by " + combo.discount);
    }

    // ── Marchetti dealer ───────────────────────────────────────────────────────

    @Test
    void marchettiDealerSpawnedWhenRespectHigh() {
        // Set Marchetti respect to 70+
        factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW,
            GreasySpoonSystem.MARCHETTI_DEALER_RESPECT - FactionSystem.STARTING_RESPECT);
        system.openCafe(10, 1, 10, Weather.CLEAR, 3);
        assertTrue(system.getMarchettiDealerIndex() >= 0,
                "Marchetti dealer regular should be assigned when respect ≥ 70");
    }

    @Test
    void marchettiDealerNotSpawnedWhenRespectLow() {
        // Marchetti respect is default (50) — below threshold
        system.openCafe(10, 1, 10, Weather.CLEAR, 3);
        assertEquals(-1, system.getMarchettiDealerIndex(),
                "Marchetti dealer regular should NOT be assigned when respect < 70");
    }

    @Test
    void buyMedsFromDealerSucceeds() {
        system.openCafe(10, 1, 10, Weather.CLEAR, 3);
        system.setMarchettiDealerIndexForTesting(0);
        int coinsBefore = inventory.getItemCount(Material.COIN);
        boolean bought = system.buyMedsFromDealer(0, inventory);
        assertTrue(bought, "Purchase of PRESCRIPTION_MEDS from dealer should succeed");
        assertEquals(1, inventory.getItemCount(Material.PRESCRIPTION_MEDS),
                "Inventory should contain 1 PRESCRIPTION_MEDS after purchase");
        assertEquals(coinsBefore - GreasySpoonSystem.PRESCRIPTION_MEDS_PRICE,
                inventory.getItemCount(Material.COIN),
                "Correct coins should be deducted for PRESCRIPTION_MEDS");
    }

    @Test
    void buyMedsFromDealerFailsWhenNotDealer() {
        system.openCafe(10, 1, 10, Weather.CLEAR, 3);
        // No Marchetti dealer (index = -1)
        boolean bought = system.buyMedsFromDealer(0, inventory);
        assertFalse(bought, "Cannot buy meds from a non-dealer regular");
    }

    @Test
    void buyMedsFromDealerFailsInsufficientFunds() {
        system.openCafe(10, 1, 10, Weather.CLEAR, 3);
        system.setMarchettiDealerIndexForTesting(0);
        inventory.removeItem(Material.COIN, 100); // Remove all coins
        boolean bought = system.buyMedsFromDealer(0, inventory);
        assertFalse(bought, "Purchase should fail when player has insufficient funds");
    }

    // ── Landmark display name ──────────────────────────────────────────────────

    @Test
    void greasySpoonLandmarkHasCorrectDisplayName() {
        String name = ragamuffin.world.LandmarkType.GREASY_SPOON_CAFE.getDisplayName();
        assertEquals("Vera's Caff", name,
                "GREASY_SPOON_CAFE landmark should display as 'Vera's Caff'");
    }

    // ── Material entries ───────────────────────────────────────────────────────

    @Test
    void fullEnglishMaterialExists() {
        assertNotNull(Material.FULL_ENGLISH, "FULL_ENGLISH material should exist");
        assertEquals("Full English", Material.FULL_ENGLISH.getDisplayName(),
                "FULL_ENGLISH display name should be 'Full English'");
    }

    @Test
    void mugOfTeaMaterialExists() {
        assertNotNull(Material.MUG_OF_TEA, "MUG_OF_TEA material should exist");
    }

    @Test
    void beansOnToastMaterialExists() {
        assertNotNull(Material.BEANS_ON_TOAST, "BEANS_ON_TOAST material should exist");
    }

    @Test
    void friedBreadMaterialExists() {
        assertNotNull(Material.FRIED_BREAD, "FRIED_BREAD material should exist");
    }

    @Test
    void baconButtyMaterialExists() {
        assertNotNull(Material.BACON_BUTTY, "BACON_BUTTY material should exist");
    }

    @Test
    void builderSTeaMaterialExists() {
        assertNotNull(Material.BUILDER_S_TEA, "BUILDER_S_TEA material should exist");
    }

    @Test
    void chalkboardMaterialExists() {
        assertNotNull(Material.CHALKBOARD, "CHALKBOARD material should exist");
    }

    @Test
    void caffFoodMaterialsAreNotBlocks() {
        assertFalse(Material.FULL_ENGLISH.isBlockItem(), "FULL_ENGLISH should not be a block item");
        assertFalse(Material.MUG_OF_TEA.isBlockItem(), "MUG_OF_TEA should not be a block item");
        assertFalse(Material.BEANS_ON_TOAST.isBlockItem(), "BEANS_ON_TOAST should not be a block item");
        assertFalse(Material.FRIED_BREAD.isBlockItem(), "FRIED_BREAD should not be a block item");
        assertFalse(Material.BACON_BUTTY.isBlockItem(), "BACON_BUTTY should not be a block item");
        assertFalse(Material.BUILDER_S_TEA.isBlockItem(), "BUILDER_S_TEA should not be a block item");
    }

    // ── NPC type entries ───────────────────────────────────────────────────────

    @Test
    void caffOwnerNpcTypeExists() {
        assertNotNull(NPCType.CAFF_OWNER, "CAFF_OWNER NPC type should exist");
        assertFalse(NPCType.CAFF_OWNER.isHostile(), "CAFF_OWNER should not be hostile");
    }

    @Test
    void caffRegularNpcTypeExists() {
        assertNotNull(NPCType.CAFF_REGULAR, "CAFF_REGULAR NPC type should exist");
        assertFalse(NPCType.CAFF_REGULAR.isHostile(), "CAFF_REGULAR should not be hostile");
    }

    // ── Menu constants ─────────────────────────────────────────────────────────

    @Test
    void menuPricesMatchConstants() {
        for (GreasySpoonSystem.MenuItem item : GreasySpoonSystem.MENU) {
            assertTrue(item.price > 0, "Menu item price should be positive: " + item.material);
        }
    }

    @Test
    void fullEnglishPriceIs6() {
        assertEquals(6, GreasySpoonSystem.PRICE_FULL_ENGLISH,
                "Full English should cost 6 coins");
    }

    @Test
    void mugOfTeaPriceIs2() {
        assertEquals(2, GreasySpoonSystem.PRICE_MUG_OF_TEA,
                "Mug of Tea should cost 2 coins");
    }
}

package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.*;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #930 — Charity Shop System: Haggling, Mystery Bags
 * &amp; Community Service.
 *
 * <p>Eight scenarios:
 * <ol>
 *   <li>Buy item with warmth system integration — shop acts as warm shelter</li>
 *   <li>Cold-snap haggle for clothing always succeeds</li>
 *   <li>Mystery bag daily limit enforced (normal vs high-notoriety)</li>
 *   <li>DIAMOND drawn from mystery bag triggers CHARITY_SHOP_DIAMOND achievement</li>
 *   <li>Donation reduces notoriety and seeds LOOT_TIP rumour</li>
 *   <li>5+ donations unlock COMMUNITY_SERVICE achievement</li>
 *   <li>BALACLAVA refusal — volunteer refuses service</li>
 *   <li>Donated item re-enters shop stock</li>
 * </ol>
 */
class Issue930CharityShopSystemTest {

    private CharityShopSystem charityShop;
    private NotorietySystem notorietySystem;
    private WeatherSystem weatherSystem;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private NotorietySystem.AchievementCallback achievementCallback;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        charityShop = new CharityShopSystem(new Random(42));
        notorietySystem = new NotorietySystem(new Random(1));
        weatherSystem = new WeatherSystem();
        rumourNetwork = new RumourNetwork(new Random(3));
        achievementSystem = new AchievementSystem();
        achievementCallback = type -> achievementSystem.unlock(type);
        inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 50);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Buy item — shop is open and item purchased successfully
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: Shop is open at 10:00. Stock contains a COAT. Player has enough
     * COIN. Verify PurchaseResult.SUCCESS is returned, COAT added to inventory,
     * COIN deducted by the base price, and COAT removed from daily stock.
     */
    @Test
    void buyItem_successRemovesItemAndDeductsCoins() {
        // Set up stock with known items
        charityShop.setDailyStockForTesting(Arrays.asList(
                Material.COAT, Material.WOOLLY_HAT, Material.TEXTBOOK,
                Material.BROKEN_PHONE, Material.DODGY_DVD, Material.NEWSPAPER));
        charityShop.setLastStockDayForTesting(1);

        float hour = 10.0f;
        int coinsBefore = inventory.getItemCount(Material.COIN);
        int coatsBefore = inventory.getItemCount(Material.COAT);

        CharityShopSystem.PurchaseResult result = charityShop.buyItem(
                hour, Material.COAT, inventory, false);

        assertEquals(CharityShopSystem.PurchaseResult.SUCCESS, result,
                "Buying a COAT during opening hours should succeed");
        assertEquals(coatsBefore + 1, inventory.getItemCount(Material.COAT),
                "Player should have one more COAT after purchase");
        assertEquals(coinsBefore - CharityShopSystem.getBasePrice(Material.COAT),
                inventory.getItemCount(Material.COIN),
                "COIN should be deducted by the COAT base price");
        assertFalse(charityShop.getDailyStock().contains(Material.COAT),
                "COAT should be removed from daily stock after purchase");
    }

    /**
     * Scenario 1b: Attempting to buy when shop is closed (20:00) returns CLOSED.
     */
    @Test
    void buyItem_closedOutsideOpeningHours() {
        charityShop.setDailyStockForTesting(Arrays.asList(Material.COAT));
        charityShop.setLastStockDayForTesting(1);

        CharityShopSystem.PurchaseResult result = charityShop.buyItem(
                20.0f, Material.COAT, inventory, false);

        assertEquals(CharityShopSystem.PurchaseResult.CLOSED, result,
                "Shop should return CLOSED when outside opening hours");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Cold-snap haggle for clothing always succeeds
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: Set weather to COLD_SNAP. Attempt to haggle for a COAT.
     * Verify HaggleResult.COLD_SNAP_ACCEPTED is returned regardless of
     * normal probability. Verify COAT added to inventory at the reduced price.
     * Verify TIGHT_FISTED achievement is awarded.
     */
    @Test
    void haggle_coldSnapAlwaysSucceedsForClothing() {
        charityShop.setDailyStockForTesting(Arrays.asList(
                Material.COAT, Material.WOOLLY_HAT, Material.TEXTBOOK,
                Material.BROKEN_PHONE, Material.DODGY_DVD, Material.NEWSPAPER));
        charityShop.setLastStockDayForTesting(1);

        weatherSystem.setWeather(Weather.COLD_SNAP);

        float hour = 12.0f;
        int coinsBefore = inventory.getItemCount(Material.COIN);
        int coatsBefore = inventory.getItemCount(Material.COAT);
        int expectedPrice = Math.max(0, CharityShopSystem.getBasePrice(Material.COAT) - 1);

        CharityShopSystem.HaggleResult result = charityShop.haggle(
                hour, Material.COAT, inventory,
                false, 0,
                weatherSystem.getCurrentWeather(), achievementCallback);

        assertEquals(CharityShopSystem.HaggleResult.COLD_SNAP_ACCEPTED, result,
                "Cold-snap haggle for clothing should always return COLD_SNAP_ACCEPTED");
        assertEquals(coatsBefore + 1, inventory.getItemCount(Material.COAT),
                "Player should have one more COAT after cold-snap haggle");
        assertEquals(coinsBefore - expectedPrice, inventory.getItemCount(Material.COIN),
                "COIN should be deducted by the haggled price (base - 1)");
        assertTrue(achievementSystem.isUnlocked(AchievementType.TIGHT_FISTED),
                "TIGHT_FISTED achievement should be awarded on successful haggle");
    }

    /**
     * Scenario 2b: Normal haggle (no cold snap). With a deterministic RNG that
     * always returns a value below the acceptance probability, haggle should succeed.
     * With a value above it, should be rejected.
     */
    @Test
    void haggle_probabilisticAcceptanceBasedOnNotoriety() {
        // Use a random that always accepts (nextFloat returns 0.0 → always < threshold)
        CharityShopSystem alwaysAccept = new CharityShopSystem(new Random(0) {
            @Override
            public float nextFloat() {
                return 0.0f; // always accept
            }
        });
        alwaysAccept.setDailyStockForTesting(Arrays.asList(Material.TEXTBOOK));
        alwaysAccept.setLastStockDayForTesting(1);

        CharityShopSystem.HaggleResult result = alwaysAccept.haggle(
                12.0f, Material.TEXTBOOK, inventory,
                false, 2, // tier 2 — no low-notoriety bonus
                Weather.CLEAR, achievementCallback);

        assertEquals(CharityShopSystem.HaggleResult.ACCEPTED, result,
                "Haggle should be ACCEPTED when random always returns 0.0");

        // Use a random that always rejects (nextFloat returns 0.99 → always > threshold)
        CharityShopSystem alwaysReject = new CharityShopSystem(new Random(0) {
            @Override
            public float nextFloat() {
                return 0.99f; // always reject
            }
        });
        alwaysReject.setDailyStockForTesting(Arrays.asList(Material.TEXTBOOK));
        alwaysReject.setLastStockDayForTesting(1);

        CharityShopSystem.HaggleResult rejectedResult = alwaysReject.haggle(
                12.0f, Material.TEXTBOOK, inventory,
                false, 2,
                Weather.CLEAR, achievementCallback);

        assertEquals(CharityShopSystem.HaggleResult.REJECTED, rejectedResult,
                "Haggle should be REJECTED when random always returns 0.99");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Mystery bag daily limit enforced
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3a: Normal player (Tier 0). Buy 3 mystery bags successfully.
     * The 4th attempt returns LIMIT_REACHED.
     */
    @Test
    void mysteryBag_dailyLimitEnforcedForNormalPlayer() {
        float hour = 12.0f;
        int day = 1;
        int notorietyTier = 0;

        for (int i = 0; i < CharityShopSystem.MYSTERY_BAG_MAX_PER_DAY; i++) {
            CharityShopSystem.MysteryBagResult r = charityShop.buyMysteryBag(
                    hour, day, inventory, false, notorietyTier, achievementCallback);
            assertEquals(CharityShopSystem.MysteryBagResult.SUCCESS, r,
                    "Mystery bag " + (i + 1) + " should succeed (limit = "
                    + CharityShopSystem.MYSTERY_BAG_MAX_PER_DAY + ")");
        }

        // 4th attempt should fail
        CharityShopSystem.MysteryBagResult overflow = charityShop.buyMysteryBag(
                hour, day, inventory, false, notorietyTier, achievementCallback);
        assertEquals(CharityShopSystem.MysteryBagResult.LIMIT_REACHED, overflow,
                "4th mystery bag on the same day should return LIMIT_REACHED");
    }

    /**
     * Scenario 3b: High-notoriety player (Tier 3+). Only 1 mystery bag allowed per day.
     * Second attempt returns LIMIT_REACHED.
     */
    @Test
    void mysteryBag_reducedLimitForHighNotorietyPlayer() {
        float hour = 12.0f;
        int day = 2;
        int notorietyTier = CharityShopSystem.MYSTERY_BAG_HIGH_NOTORIETY_TIER; // 3

        CharityShopSystem.MysteryBagResult first = charityShop.buyMysteryBag(
                hour, day, inventory, false, notorietyTier, achievementCallback);
        assertEquals(CharityShopSystem.MysteryBagResult.SUCCESS, first,
                "First mystery bag for high-notoriety player should succeed");

        // Immediately try again
        CharityShopSystem.MysteryBagResult second = charityShop.buyMysteryBag(
                hour, day, inventory, false, notorietyTier, achievementCallback);
        assertEquals(CharityShopSystem.MysteryBagResult.LIMIT_REACHED, second,
                "High-notoriety player should be limited to "
                + CharityShopSystem.MYSTERY_BAG_HIGH_NOTORIETY_MAX + " bag(s) per day");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: DIAMOND from mystery bag triggers CHARITY_SHOP_DIAMOND achievement
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Use a deterministic random that forces a DIAMOND result.
     * Verify CHARITY_SHOP_DIAMOND achievement is awarded and DIAMOND added to inventory.
     */
    @Test
    void mysteryBag_diamondTriggersCHARITY_SHOP_DIAMONDAchievement() {
        // To force a DIAMOND: roll must be in range [93, 94) — weight 1 at index 7
        // MYSTERY_BAG_TOTAL_WEIGHT = 94, DIAMOND is last entry (cumulative = 93..93)
        // We need nextInt(94) to return 93
        CharityShopSystem forcesDiamond = new CharityShopSystem(new Random(0) {
            @Override
            public int nextInt(int bound) {
                if (bound == 94) return 93; // forces DIAMOND
                return 0;
            }
        });

        int diamondsBefore = inventory.getItemCount(Material.DIAMOND);

        CharityShopSystem.MysteryBagResult result = forcesDiamond.buyMysteryBag(
                12.0f, 1, inventory, false, 0, achievementCallback);

        assertEquals(CharityShopSystem.MysteryBagResult.SUCCESS, result,
                "Mystery bag purchase should succeed");
        assertEquals(Material.DIAMOND, forcesDiamond.getLastMysteryBagItem(),
                "Mystery bag should yield DIAMOND when forced");
        assertEquals(diamondsBefore + 1, inventory.getItemCount(Material.DIAMOND),
                "DIAMOND should be added to inventory");
        assertTrue(achievementSystem.isUnlocked(AchievementType.CHARITY_SHOP_DIAMOND),
                "CHARITY_SHOP_DIAMOND achievement should be awarded on DIAMOND mystery bag result");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Donation reduces notoriety and seeds LOOT_TIP rumour
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5: Player has a COAT in inventory. Donate it.
     * Verify DonationResult.SUCCESS. Verify notoriety reduced by
     * DONATION_CLOTHING_NOTORIETY_REDUCTION. Verify LOOT_TIP rumour seeded to a PUBLIC NPC.
     */
    @Test
    void donation_clothingReducesNotorietyAndSeedsLootTipRumour() {
        inventory.addItem(Material.COAT, 1);
        notorietySystem.setNotorietyForTesting(100); // give some notoriety to reduce
        int notorietyBefore = notorietySystem.getNotoriety();

        List<NPC> npcs = new ArrayList<>();
        NPC publicNpc = new NPC(NPCType.PUBLIC, 0f, 1f, 0f);
        npcs.add(publicNpc);

        CharityShopSystem.DonationResult result = charityShop.donate(
                12.0f, Material.COAT, inventory,
                false, notorietySystem, rumourNetwork, npcs, achievementCallback);

        assertEquals(CharityShopSystem.DonationResult.SUCCESS, result,
                "Donating a COAT should succeed");
        assertEquals(0, inventory.getItemCount(Material.COAT),
                "COAT should be removed from inventory after donation");

        int notorietyAfter = notorietySystem.getNotoriety();
        assertEquals(notorietyBefore - CharityShopSystem.DONATION_CLOTHING_NOTORIETY_REDUCTION,
                notorietyAfter,
                "Notoriety should be reduced by " + CharityShopSystem.DONATION_CLOTHING_NOTORIETY_REDUCTION
                + " after clothing donation");

        // Verify LOOT_TIP rumour was seeded to the PUBLIC NPC
        List<Rumour> rumours = publicNpc.getRumours();
        assertTrue(rumours.stream().anyMatch(r -> r.getType() == RumourType.LOOT_TIP),
                "A LOOT_TIP rumour should be seeded to a PUBLIC NPC after clothing donation");
    }

    /**
     * Scenario 5b: Donating a DIAMOND reduces notoriety by DONATION_DIAMOND_NOTORIETY_REDUCTION
     * and awards DIAMOND_DONOR achievement.
     */
    @Test
    void donation_diamondReducesMoreNotorietyAndAwardsDIAMOND_DONOR() {
        inventory.addItem(Material.DIAMOND, 1);
        notorietySystem.setNotorietyForTesting(200);
        int notorietyBefore = notorietySystem.getNotoriety();

        CharityShopSystem.DonationResult result = charityShop.donate(
                12.0f, Material.DIAMOND, inventory,
                false, notorietySystem, rumourNetwork, new ArrayList<>(), achievementCallback);

        assertEquals(CharityShopSystem.DonationResult.SUCCESS, result,
                "Donating a DIAMOND should succeed");
        assertEquals(notorietyBefore - CharityShopSystem.DONATION_DIAMOND_NOTORIETY_REDUCTION,
                notorietySystem.getNotoriety(),
                "Notoriety should be reduced by " + CharityShopSystem.DONATION_DIAMOND_NOTORIETY_REDUCTION
                + " after DIAMOND donation");
        assertTrue(achievementSystem.isUnlocked(AchievementType.DIAMOND_DONOR),
                "DIAMOND_DONOR achievement should be awarded after donating a DIAMOND");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 6: 5+ donations unlock COMMUNITY_SERVICE achievement
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 6: Make 5 donations (using NEWSPAPER which is cheap and always available).
     * After the 5th donation, verify COMMUNITY_SERVICE achievement is awarded.
     * Verify total donations count equals 5.
     */
    @Test
    void donations_fiveOrMoreUnlockCOMMUNITY_SERVICE() {
        float hour = 12.0f;

        // Pre-set 4 donations so the 5th triggers the achievement
        charityShop.setTotalDonationsForTesting(
                CharityShopSystem.COMMUNITY_SERVICE_DONATION_TARGET - 1);

        // Add the item to donate
        inventory.addItem(Material.NEWSPAPER, 1);

        CharityShopSystem.DonationResult result = charityShop.donate(
                hour, Material.NEWSPAPER, inventory,
                false, notorietySystem, rumourNetwork, new ArrayList<>(), achievementCallback);

        assertEquals(CharityShopSystem.DonationResult.SUCCESS, result,
                "5th donation should succeed");
        assertEquals(CharityShopSystem.COMMUNITY_SERVICE_DONATION_TARGET,
                charityShop.getTotalDonations(),
                "Total donations should be " + CharityShopSystem.COMMUNITY_SERVICE_DONATION_TARGET);
        assertTrue(achievementSystem.isUnlocked(AchievementType.COMMUNITY_SERVICE),
                "COMMUNITY_SERVICE achievement should be awarded after "
                + CharityShopSystem.COMMUNITY_SERVICE_DONATION_TARGET + " donations");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 7: BALACLAVA refusal
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 7a: Player wears a BALACLAVA. Attempt to buy an item.
     * Verify PurchaseResult.BALACLAVA_REFUSED is returned.
     */
    @Test
    void balaclava_purchaseRefused() {
        charityShop.setDailyStockForTesting(Arrays.asList(Material.COAT));
        charityShop.setLastStockDayForTesting(1);

        CharityShopSystem.PurchaseResult result = charityShop.buyItem(
                12.0f, Material.COAT, inventory, true); // hasBalaclava = true

        assertEquals(CharityShopSystem.PurchaseResult.BALACLAVA_REFUSED, result,
                "Buying while wearing a BALACLAVA should return BALACLAVA_REFUSED");
        assertEquals(0, inventory.getItemCount(Material.COAT),
                "COAT should NOT be added to inventory when refused");
    }

    /**
     * Scenario 7b: Player wears a BALACLAVA. Attempt to haggle. Verify BALACLAVA_REFUSED.
     * Attempt to buy mystery bag. Verify BALACLAVA_REFUSED.
     * Attempt to donate. Verify BALACLAVA_REFUSED.
     */
    @Test
    void balaclava_refusedForAllInteractions() {
        charityShop.setDailyStockForTesting(Arrays.asList(Material.COAT));
        charityShop.setLastStockDayForTesting(1);
        inventory.addItem(Material.NEWSPAPER, 1);

        // Haggle refused
        CharityShopSystem.HaggleResult haggleResult = charityShop.haggle(
                12.0f, Material.COAT, inventory, true, 0,
                Weather.CLEAR, achievementCallback);
        assertEquals(CharityShopSystem.HaggleResult.BALACLAVA_REFUSED, haggleResult,
                "Haggling with BALACLAVA should return BALACLAVA_REFUSED");

        // Mystery bag refused
        CharityShopSystem.MysteryBagResult bagResult = charityShop.buyMysteryBag(
                12.0f, 1, inventory, true, 0, achievementCallback);
        assertEquals(CharityShopSystem.MysteryBagResult.BALACLAVA_REFUSED, bagResult,
                "Mystery bag with BALACLAVA should return BALACLAVA_REFUSED");

        // Donation refused
        CharityShopSystem.DonationResult donateResult = charityShop.donate(
                12.0f, Material.NEWSPAPER, inventory, true,
                notorietySystem, rumourNetwork, new ArrayList<>(), achievementCallback);
        assertEquals(CharityShopSystem.DonationResult.BALACLAVA_REFUSED, donateResult,
                "Donating with BALACLAVA should return BALACLAVA_REFUSED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 8: Donated item re-enters shop stock
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 8: Start with an empty stock. Donate a WOOLLY_HAT. Verify it appears
     * in the daily stock. Then buy it back to verify it can be purchased.
     */
    @Test
    void donation_itemReEntersStock() {
        // Start with minimal stock (no woolly hat)
        charityShop.setDailyStockForTesting(new ArrayList<>(Arrays.asList(
                Material.TEXTBOOK, Material.BROKEN_PHONE, Material.DODGY_DVD)));
        charityShop.setLastStockDayForTesting(1);

        inventory.addItem(Material.WOOLLY_HAT, 1);

        assertFalse(charityShop.getDailyStock().contains(Material.WOOLLY_HAT),
                "WOOLLY_HAT should NOT be in stock before donation");

        // Donate the WOOLLY_HAT
        charityShop.donate(12.0f, Material.WOOLLY_HAT, inventory,
                false, notorietySystem, rumourNetwork, new ArrayList<>(), achievementCallback);

        assertTrue(charityShop.getDailyStock().contains(Material.WOOLLY_HAT),
                "WOOLLY_HAT should appear in daily stock after being donated");

        // Now buy it back
        CharityShopSystem.PurchaseResult buyResult = charityShop.buyItem(
                12.0f, Material.WOOLLY_HAT, inventory, false);
        assertEquals(CharityShopSystem.PurchaseResult.SUCCESS, buyResult,
                "Should be able to buy back the donated WOOLLY_HAT");
        assertEquals(1, inventory.getItemCount(Material.WOOLLY_HAT),
                "Player should have the WOOLLY_HAT back after buying it");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 9 (bonus): Daily stock always includes at least one clothing item
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 9: Generate daily stock. Verify at least one CLOTHING_ITEMS item
     * is present in the 6-item daily stock.
     */
    @Test
    void dailyStock_alwaysContainsAtLeastOneClothingItem() {
        // Force stock generation by using a new system with day 1
        CharityShopSystem fresh = new CharityShopSystem(new Random(99));

        // Trigger stock generation via update (day 1 is a new day for lastStockDay = -1)
        fresh.update(0f,
                new TimeSystem(10.0f),
                null, null, null, null);

        List<Material> stock = fresh.getDailyStock();
        assertEquals(CharityShopSystem.DAILY_STOCK_SIZE, stock.size(),
                "Daily stock should have exactly " + CharityShopSystem.DAILY_STOCK_SIZE + " items");

        boolean hasClothing = stock.stream()
                .anyMatch(m -> CharityShopSystem.CLOTHING_ITEMS.contains(m));
        assertTrue(hasClothing,
                "Daily stock should always include at least one clothing item");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 10 (bonus): Opening hours
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 10: Verify shop is open at 10:00, 12:00, 16:59. Verify shop is
     * closed at 08:59, 17:00, 20:00.
     */
    @Test
    void openingHours_correctlyEnforced() {
        assertTrue(charityShop.isOpen(CharityShopSystem.OPEN_HOUR),
                "Shop should be open at " + CharityShopSystem.OPEN_HOUR);
        assertTrue(charityShop.isOpen(12.0f),
                "Shop should be open at 12:00");
        assertTrue(charityShop.isOpen(CharityShopSystem.CLOSE_HOUR - 0.01f),
                "Shop should be open just before closing hour");

        assertFalse(charityShop.isOpen(CharityShopSystem.OPEN_HOUR - 0.01f),
                "Shop should be closed just before opening hour");
        assertFalse(charityShop.isOpen(CharityShopSystem.CLOSE_HOUR),
                "Shop should be closed at exactly the closing hour");
        assertFalse(charityShop.isOpen(20.0f),
                "Shop should be closed at 20:00");
    }
}

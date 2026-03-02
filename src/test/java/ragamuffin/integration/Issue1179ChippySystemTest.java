package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.ChippySystem;
import ragamuffin.core.NotorietySystem;
import ragamuffin.core.Rumour;
import ragamuffin.core.RumourNetwork;
import ragamuffin.core.RumourType;
import ragamuffin.core.Weather;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;
import ragamuffin.ui.ChippyOrderUI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1179: Tony's Chip Shop — Late-Night Queue,
 * Biscuit the Cat &amp; the Frying-Pan Heist.
 */
class Issue1179ChippySystemTest {

    private ChippySystem chippy;
    private Inventory inventory;
    private NotorietySystem notorietySystem;
    private RumourNetwork rumourNetwork;
    private List<AchievementType> awarded;
    private NotorietySystem.AchievementCallback achievementCb;

    @BeforeEach
    void setUp() {
        chippy = new ChippySystem(new Random(42L));
        inventory = new Inventory();
        notorietySystem = new NotorietySystem();
        rumourNetwork = new RumourNetwork();
        awarded = new ArrayList<>();
        achievementCb = type -> awarded.add(type);

        chippy.setNotorietySystem(notorietySystem);
        chippy.setRumourNetwork(rumourNetwork);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Opening hours
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void isOpen_atOpenHour_returnsTrue() {
        assertTrue(chippy.isOpen(11.0f, Weather.CLEAR));
    }

    @Test
    void isOpen_beforeOpenHour_returnsFalse() {
        assertFalse(chippy.isOpen(10.5f, Weather.CLEAR));
    }

    @Test
    void isOpen_atMidnight_returnsFalse() {
        // 24.0 = midnight closing
        assertFalse(chippy.isOpen(24.0f, Weather.CLEAR));
    }

    @Test
    void isOpen_eveningHour_returnsTrue() {
        assertTrue(chippy.isOpen(22.0f, Weather.OVERCAST));
    }

    @Test
    void isOpen_frost_delaysToHalfEleven() {
        // 11:00 is too early when frost
        assertFalse(chippy.isOpen(11.0f, Weather.FROST));
        // 11:30 is fine
        assertTrue(chippy.isOpen(11.5f, Weather.FROST));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fish supper availability
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void fishSupper_availableWhenDayNumberModThreeNotZero() {
        // Day 1: 1 % 3 = 1 ≠ 0 → available
        assertTrue(chippy.isFishSupperAvailable(1));
        // Day 2: 2 % 3 = 2 ≠ 0 → available
        assertTrue(chippy.isFishSupperAvailable(2));
        // Day 3: 3 % 3 = 0 → not available
        assertFalse(chippy.isFishSupperAvailable(3));
        // Day 6: 6 % 3 = 0 → not available
        assertFalse(chippy.isFishSupperAvailable(6));
        // Day 4: 4 % 3 = 1 → available
        assertTrue(chippy.isFishSupperAvailable(4));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ordering — success path
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void placeOrder_chips_deductsCoinAndAddsItem() {
        inventory.addItem(Material.COIN, 5);

        ChippySystem.OrderResult result = chippy.placeOrder(
                Material.CHIPS, 2, inventory, 0,
                13.0f, 1, Weather.CLEAR, null, achievementCb);

        assertEquals(ChippySystem.OrderResult.SUCCESS, result);
        assertEquals(3, inventory.getItemCount(Material.COIN));
        assertEquals(1, inventory.getItemCount(Material.CHIPS));
    }

    @Test
    void placeOrder_fishSupper_availableDay_succeeds() {
        inventory.addItem(Material.COIN, 10);

        // Day 1 (% 3 != 0) → fish supper available
        ChippySystem.OrderResult result = chippy.placeOrder(
                Material.FISH_SUPPER, 4, inventory, 0,
                13.0f, 1, Weather.CLEAR, null, achievementCb);

        assertEquals(ChippySystem.OrderResult.SUCCESS, result);
        assertEquals(1, inventory.getItemCount(Material.FISH_SUPPER));
    }

    @Test
    void placeOrder_fishSupper_unavailableDay_returnsItemUnavailable() {
        inventory.addItem(Material.COIN, 10);

        // Day 3 (% 3 == 0) → fish supper not available
        ChippySystem.OrderResult result = chippy.placeOrder(
                Material.FISH_SUPPER, 4, inventory, 0,
                13.0f, 3, Weather.CLEAR, null, achievementCb);

        assertEquals(ChippySystem.OrderResult.ITEM_UNAVAILABLE, result);
        assertEquals(0, inventory.getItemCount(Material.FISH_SUPPER));
    }

    @Test
    void placeOrder_chipButty_withBread_succeeds() {
        inventory.addItem(Material.COIN, 5);
        inventory.addItem(Material.BREAD, 1);

        ChippySystem.OrderResult result = chippy.placeOrder(
                Material.CHIP_BUTTY, 3, inventory, 0,
                13.0f, 1, Weather.CLEAR, null, achievementCb);

        assertEquals(ChippySystem.OrderResult.SUCCESS, result);
        assertEquals(1, inventory.getItemCount(Material.CHIP_BUTTY));
        // BREAD should be consumed
        assertFalse(inventory.hasItem(Material.BREAD), "BREAD should be consumed by CHIP_BUTTY");
    }

    @Test
    void placeOrder_chipButty_noBread_returnsMissingBread() {
        inventory.addItem(Material.COIN, 5);

        ChippySystem.OrderResult result = chippy.placeOrder(
                Material.CHIP_BUTTY, 3, inventory, 0,
                13.0f, 1, Weather.CLEAR, null, achievementCb);

        assertEquals(ChippySystem.OrderResult.MISSING_BREAD, result);
        assertEquals(0, inventory.getItemCount(Material.CHIP_BUTTY));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ordering — failure paths
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void placeOrder_shopClosed_returnsShopClosed() {
        inventory.addItem(Material.COIN, 5);

        ChippySystem.OrderResult result = chippy.placeOrder(
                Material.CHIPS, 2, inventory, 0,
                8.0f, 1, Weather.CLEAR, null, achievementCb);

        assertEquals(ChippySystem.OrderResult.SHOP_CLOSED, result);
        assertEquals(0, inventory.getItemCount(Material.CHIPS));
    }

    @Test
    void placeOrder_highNotoriety_returnsServiceRefused() {
        inventory.addItem(Material.COIN, 5);

        ChippySystem.OrderResult result = chippy.placeOrder(
                Material.CHIPS, 2, inventory, ChippySystem.SERVICE_REFUSED_NOTORIETY,
                13.0f, 1, Weather.CLEAR, null, achievementCb);

        assertEquals(ChippySystem.OrderResult.SERVICE_REFUSED, result);
    }

    @Test
    void placeOrder_insufficientFunds_returnsInsufficientFunds() {
        inventory.addItem(Material.COIN, 1);

        ChippySystem.OrderResult result = chippy.placeOrder(
                Material.CHIPS, 2, inventory, 0,
                13.0f, 1, Weather.CLEAR, null, achievementCb);

        assertEquals(ChippySystem.OrderResult.INSUFFICIENT_FUNDS, result);
        assertEquals(0, inventory.getItemCount(Material.CHIPS));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Queue jumping
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void placeOrder_queuePresent_returnsQueueJumpRefused() {
        inventory.addItem(Material.COIN, 5);
        chippy.setQueueSizeForTesting(3); // 3 NPCs in queue

        ChippySystem.OrderResult result = chippy.placeOrder(
                Material.CHIPS, 2, inventory, 0,
                23.5f, 1, Weather.CLEAR, null, achievementCb);

        assertEquals(ChippySystem.OrderResult.QUEUE_JUMP_REFUSED, result);
        assertEquals(0, inventory.getItemCount(Material.CHIPS));
    }

    @Test
    void placeOrder_queuePresent_seedsQueueJumpRumour() {
        inventory.addItem(Material.COIN, 5);
        chippy.setQueueSizeForTesting(2);
        NPC tonyNpc = new NPC(NPCType.CHIPPY_OWNER, 0f, 1f, 0f);

        chippy.placeOrder(Material.CHIPS, 2, inventory, 0,
                23.5f, 1, Weather.CLEAR, tonyNpc, achievementCb);

        // ChippySystem seeds QUEUE_JUMP rumour into tonyNpc via rumourNetwork.addRumour
        List<Rumour> tonyRumours = tonyNpc.getRumours();
        assertFalse(tonyRumours.isEmpty(), "QUEUE_JUMP rumour should be seeded into tonyNpc");
        assertTrue(tonyRumours.stream().anyMatch(r -> r.getType() == RumourType.QUEUE_JUMP),
                "Tony should have a QUEUE_JUMP rumour");
    }

    @Test
    void placeOrder_queuePresent_awardsQueueJumperAchievement() {
        inventory.addItem(Material.COIN, 5);
        chippy.setQueueSizeForTesting(2);

        chippy.placeOrder(Material.CHIPS, 2, inventory, 0,
                23.5f, 1, Weather.CLEAR, null, achievementCb);

        assertTrue(awarded.contains(AchievementType.QUEUE_JUMPER));
    }

    @Test
    void placeOrder_emptyQueue_servesWithoutQueueJump() {
        inventory.addItem(Material.COIN, 5);
        // No queue (default after construction, empty queue)

        ChippySystem.OrderResult result = chippy.placeOrder(
                Material.CHIPS, 2, inventory, 0,
                13.0f, 1, Weather.CLEAR, null, achievementCb);

        assertEquals(ChippySystem.OrderResult.SUCCESS, result);
        assertFalse(awarded.contains(AchievementType.QUEUE_JUMPER));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Last orders achievement
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void placeOrder_lastOrders_awardsLastOrdersChippyAchievement() {
        inventory.addItem(Material.COIN, 5);

        // 23:55 is within last orders window (23:50–00:00)
        chippy.placeOrder(Material.CHIPS, 2, inventory, 0,
                23.0f + 55.0f / 60.0f, 1, Weather.CLEAR, null, achievementCb);

        assertTrue(awarded.contains(AchievementType.LAST_ORDERS_CHIPPY),
                "LAST_ORDERS_CHIPPY should be awarded during last orders window");
    }

    @Test
    void placeOrder_notLastOrders_noLastOrdersAchievement() {
        inventory.addItem(Material.COIN, 5);

        chippy.placeOrder(Material.CHIPS, 2, inventory, 0,
                13.0f, 1, Weather.CLEAR, null, achievementCb);

        assertFalse(awarded.contains(AchievementType.LAST_ORDERS_CHIPPY));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHIPPY_REGULAR achievement (5 purchase days)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void placeOrder_fivePurchaseDays_awardsChippyRegular() {
        // Set 4 purchase days already
        Set<Integer> priorDays = new HashSet<>();
        for (int i = 1; i <= 4; i++) priorDays.add(i);
        chippy.setPurchaseDaysForTesting(priorDays);

        inventory.addItem(Material.COIN, 5);
        // Day 5 = 5th purchase day
        chippy.placeOrder(Material.CHIPS, 2, inventory, 0,
                13.0f, 5, Weather.CLEAR, null, achievementCb);

        assertTrue(awarded.contains(AchievementType.CHIPPY_REGULAR),
                "CHIPPY_REGULAR should be awarded after 5 purchase days");
    }

    @Test
    void purchaseDayCount_incrementsOnFirstPurchaseOfDay() {
        inventory.addItem(Material.COIN, 10);

        chippy.placeOrder(Material.CHIPS, 2, inventory, 0,
                13.0f, 1, Weather.CLEAR, null, achievementCb);
        chippy.placeOrder(Material.CHIPS, 2, inventory, 0,
                14.0f, 1, Weather.CLEAR, null, achievementCb); // same day

        assertEquals(1, chippy.getPurchaseDayCount(),
                "Multiple purchases on the same day should count as one");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Heatwave price surcharge
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getEffectivePrice_heatwave_addsOneCoin() {
        assertEquals(3, chippy.getEffectivePrice(2, Weather.HEATWAVE));
    }

    @Test
    void getEffectivePrice_clearWeather_noSurcharge() {
        assertEquals(2, chippy.getEffectivePrice(2, Weather.CLEAR));
    }

    @Test
    void placeOrder_heatwave_deductsExtraCoin() {
        inventory.addItem(Material.COIN, 3); // enough for 2+1 surcharge

        ChippySystem.OrderResult result = chippy.placeOrder(
                Material.CHIPS, 2, inventory, 0,
                13.0f, 1, Weather.HEATWAVE, null, achievementCb);

        assertEquals(ChippySystem.OrderResult.SUCCESS, result);
        assertEquals(0, inventory.getItemCount(Material.COIN));
    }

    @Test
    void placeOrder_heatwave_insufficientForSurcharge_returnsFail() {
        inventory.addItem(Material.COIN, 2); // base price but not +1 surcharge

        ChippySystem.OrderResult result = chippy.placeOrder(
                Material.CHIPS, 2, inventory, 0,
                13.0f, 1, Weather.HEATWAVE, null, achievementCb);

        assertEquals(ChippySystem.OrderResult.INSUFFICIENT_FUNDS, result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Biscuit the cat — punching
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void onBiscuitPunched_biscuitFlees() {
        NPC biscuit = new NPC(NPCType.STRAY_CAT, 5f, 1f, 5f);
        NPC tony = new NPC(NPCType.CHIPPY_OWNER, 0f, 1f, 0f);

        chippy.onBiscuitPunched(biscuit, tony, achievementCb);

        assertEquals(NPCState.FLEEING, biscuit.getState(),
                "Biscuit should flee after being punched");
    }

    @Test
    void onBiscuitPunched_tonyGoesAggressive() {
        NPC biscuit = new NPC(NPCType.STRAY_CAT, 5f, 1f, 5f);
        NPC tony = new NPC(NPCType.CHIPPY_OWNER, 0f, 1f, 0f);

        chippy.onBiscuitPunched(biscuit, tony, achievementCb);

        assertEquals(NPCState.AGGRESSIVE, tony.getState(),
                "Tony should go AGGRESSIVE after Biscuit is punched");
    }

    @Test
    void onBiscuitPunched_seedsCatPunchRumour() {
        NPC biscuit = new NPC(NPCType.STRAY_CAT, 5f, 1f, 5f);
        NPC tony = new NPC(NPCType.CHIPPY_OWNER, 0f, 1f, 0f);

        chippy.onBiscuitPunched(biscuit, tony, achievementCb);

        // ChippySystem seeds CAT_PUNCH rumour into tony via rumourNetwork.addRumour
        List<Rumour> rumours = tony.getRumours();
        assertFalse(rumours.isEmpty(), "CAT_PUNCH rumour should be seeded into tony");
        assertTrue(rumours.stream().anyMatch(r -> r.getType() == RumourType.CAT_PUNCH),
                "Tony should have a CAT_PUNCH rumour");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Biscuit the cat — feeding
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void feedBiscuit_withChips_succeeds() {
        NPC biscuit = new NPC(NPCType.STRAY_CAT, 5f, 1f, 5f);
        inventory.addItem(Material.CHIPS, 1);

        ChippySystem.FeedBiscuitResult result = chippy.feedBiscuit(
                Material.CHIPS, inventory, biscuit, achievementCb);

        assertEquals(ChippySystem.FeedBiscuitResult.SUCCESS, result);
        assertFalse(inventory.hasItem(Material.CHIPS), "CHIPS should be consumed");
    }

    @Test
    void feedBiscuit_withFishSupper_succeeds() {
        NPC biscuit = new NPC(NPCType.STRAY_CAT, 5f, 1f, 5f);
        inventory.addItem(Material.FISH_SUPPER, 1);

        ChippySystem.FeedBiscuitResult result = chippy.feedBiscuit(
                Material.FISH_SUPPER, inventory, biscuit, achievementCb);

        assertEquals(ChippySystem.FeedBiscuitResult.SUCCESS, result);
    }

    @Test
    void feedBiscuit_wrongFood_returnsWrongFood() {
        NPC biscuit = new NPC(NPCType.STRAY_CAT, 5f, 1f, 5f);
        inventory.addItem(Material.PICKLED_EGG, 1);

        ChippySystem.FeedBiscuitResult result = chippy.feedBiscuit(
                Material.PICKLED_EGG, inventory, biscuit, achievementCb);

        assertEquals(ChippySystem.FeedBiscuitResult.WRONG_FOOD, result);
    }

    @Test
    void feedBiscuit_threeFeeds_awardsFedTheCat() {
        NPC biscuit = new NPC(NPCType.STRAY_CAT, 5f, 1f, 5f);
        chippy.setBiscuitFeedCountForTesting(2); // already fed twice
        inventory.addItem(Material.CHIPS, 1);

        chippy.feedBiscuit(Material.CHIPS, inventory, biscuit, achievementCb);

        assertTrue(awarded.contains(AchievementType.FED_THE_CAT),
                "FED_THE_CAT should be awarded after 3 feeds");
    }

    @Test
    void feedBiscuit_noBiscuitPresent_returnsBiscuitNotPresent() {
        inventory.addItem(Material.CHIPS, 1);

        ChippySystem.FeedBiscuitResult result = chippy.feedBiscuit(
                Material.CHIPS, inventory, null, achievementCb);

        assertEquals(ChippySystem.FeedBiscuitResult.BISCUIT_NOT_PRESENT, result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Frying-pan heist
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void heist_outsideWindow_returnsWindowNotOpen() {
        // 14:00 is outside the 11:00 opening window
        ChippySystem.HeistResult result = chippy.attemptFryingPanHeist(
                14.0f, Weather.CLEAR, inventory, 0f);

        assertEquals(ChippySystem.HeistResult.WINDOW_NOT_OPEN, result);
    }

    @Test
    void heist_alreadyLooted_returnsAlreadyLooted() {
        chippy.setHeistLootedForTesting(true);
        ChippySystem.HeistResult result = chippy.attemptFryingPanHeist(
                11.0f, Weather.CLEAR, inventory, 0f);

        assertEquals(ChippySystem.HeistResult.ALREADY_LOOTED, result);
    }

    @Test
    void heist_withDisguise_reducesDetection() {
        // Use a seeded RNG for determinism. With full disguise (1.0 modifier),
        // detection = 0.35 - 1.0 = -0.65 → clamped to 0 → always succeeds
        ChippySystem deterministicChippy = new ChippySystem(new Random(0L));
        deterministicChippy.setQueueSizeForTesting(0);

        ChippySystem.HeistResult result = deterministicChippy.attemptFryingPanHeist(
                11.0f, Weather.CLEAR, inventory, 1.0f);

        assertEquals(ChippySystem.HeistResult.SUCCESS, result,
                "Full disguise should eliminate detection chance");
        assertTrue(inventory.getItemCount(Material.CHIPS) > 0
                        || inventory.getItemCount(Material.BATTERED_SAUSAGE) > 0
                        || inventory.getItemCount(Material.MUSHY_PEAS) > 0,
                "Heist should yield at least one food item");
    }

    @Test
    void heist_success_marksLootedFlag() {
        ChippySystem deterministicChippy = new ChippySystem(new Random(0L));
        deterministicChippy.setQueueSizeForTesting(0);

        deterministicChippy.attemptFryingPanHeist(11.0f, Weather.CLEAR, inventory, 1.0f);

        assertTrue(deterministicChippy.isHeistLooted());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Queue system
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void initQueue_normalHours_queueWithinBounds() {
        chippy.initQueue(13.0f, Weather.CLEAR);
        int size = chippy.getQueueSize();
        assertTrue(size >= ChippySystem.QUEUE_MIN_NORMAL
                        && size <= ChippySystem.QUEUE_MAX_NORMAL,
                "Queue size should be within normal bounds, got: " + size);
    }

    @Test
    void initQueue_postPubRush_largerQueue() {
        chippy.initQueue(23.5f, Weather.CLEAR);
        int size = chippy.getQueueSize();
        assertTrue(size >= ChippySystem.QUEUE_MIN_POSTPUB,
                "Post-pub rush queue should be at least " + ChippySystem.QUEUE_MIN_POSTPUB + ", got: " + size);
    }

    @Test
    void initQueue_rainWeather_thinsQueue() {
        // With RAIN, max is halved. Initialise several times with fixed seed and check.
        // At minimum it should be 1.
        ChippySystem rainChippy = new ChippySystem(new Random(1L));
        rainChippy.initQueue(13.0f, Weather.RAIN);
        int size = rainChippy.getQueueSize();
        assertTrue(size >= 1, "Rain queue should have at least 1 NPC, got: " + size);
        assertTrue(size <= ChippySystem.QUEUE_MAX_NORMAL / 2 + 1,
                "Rain should thin the queue");
    }

    @Test
    void joinQueueDrunk_addsDrunkAtFront() {
        chippy.setQueueSizeForTesting(2);
        chippy.joinQueueDrunk();

        assertEquals(3, chippy.getQueueSize());
        assertEquals(NPCType.DRUNK, chippy.getQueue().get(0));
    }

    @Test
    void updateQueue_afterAdvanceInterval_removesOnNPC() {
        chippy.setQueueSizeForTesting(3);
        int initialSize = chippy.getQueueSize();

        // Advance queue past the 90-second interval
        chippy.updateQueue(ChippySystem.QUEUE_ADVANCE_INTERVAL + 1f);

        assertEquals(initialSize - 1, chippy.getQueueSize(),
                "One NPC should have been removed after advance interval");
    }

    @Test
    void isPlayerAtFront_emptyQueue_returnsTrue() {
        assertTrue(chippy.isPlayerAtFront());
    }

    @Test
    void isPlayerAtFront_queueNotEmpty_returnsFalse() {
        chippy.setQueueSizeForTesting(1);
        assertFalse(chippy.isPlayerAtFront());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Last orders window
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void isLastOrders_before2350_returnsFalse() {
        assertFalse(chippy.isLastOrders(23.0f));
    }

    @Test
    void isLastOrders_at2350_returnsTrue() {
        assertTrue(chippy.isLastOrders(23.0f + 50.0f / 60.0f));
    }

    @Test
    void isLastOrders_at2359_returnsTrue() {
        assertTrue(chippy.isLastOrders(23.0f + 59.0f / 60.0f));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Post-pub rush detection
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void isPostPubRush_at2300_returnsTrue() {
        assertTrue(chippy.isPostPubRush(23.0f));
    }

    @Test
    void isPostPubRush_before2300_returnsFalse() {
        assertFalse(chippy.isPostPubRush(22.5f));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ChippyOrderUI integration (UI state-machine consistency)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void chippyOrderUI_fishSupperAvailability_reflectsSystemRule() {
        ChippyOrderUI ui = new ChippyOrderUI();

        // Day 1: available
        ui.setFishSupperAvailable(chippy.isFishSupperAvailable(1));
        assertTrue(ui.isFishSupperAvailable(), "Fish supper should be available on day 1");

        // Day 3: unavailable
        ui.setFishSupperAvailable(chippy.isFishSupperAvailable(3));
        assertFalse(ui.isFishSupperAvailable(), "Fish supper should not be available on day 3");
    }

    @Test
    void chippyOrderUI_buildMenuText_containsTonyHeader() {
        ChippyOrderUI ui = new ChippyOrderUI();
        String text = ui.buildMenuText();
        assertTrue(text.contains("Tony's Chip Shop"), "Menu text should contain shop name");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Enum and constant sanity checks
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void achievementTypes_allChippyAchievementsExist() {
        assertNotNull(AchievementType.LAST_ORDERS_CHIPPY);
        assertNotNull(AchievementType.QUEUE_JUMPER);
        assertNotNull(AchievementType.CHIPPY_REGULAR);
        assertNotNull(AchievementType.FED_THE_CAT);
        assertNotNull(AchievementType.CAT_PUNCHER);
    }

    @Test
    void rumourTypes_chippyRumourTypesExist() {
        assertNotNull(RumourType.QUEUE_JUMP);
        assertNotNull(RumourType.CAT_PUNCH);
    }

    @Test
    void npcTypes_chippyNpcTypesExist() {
        assertNotNull(NPCType.CHIPPY_OWNER);
        assertNotNull(NPCType.STRAY_CAT);
    }
}

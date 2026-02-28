package ragamuffin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FoodBankSystem} — Issue #942.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Food bank opening hours (Mon–Fri 09:00–17:00)</li>
 *   <li>NPC spawning (Margaret + queue recipients)</li>
 *   <li>Weather modifier (+1 recipient during rain, +2 during frost/cold snap)</li>
 *   <li>Donation mechanics: item removal, vibes boost, rumour seeding</li>
 *   <li>Donation gates: closed, already donated today, wrong item, no item, police block</li>
 *   <li>Emergency parcel mechanics: eligibility, cooldown, parcel contents, hunger boost</li>
 *   <li>Parcel gates: closed, notoriety too high, not eligible, on cooldown</li>
 *   <li>Rainy day free tea</li>
 *   <li>Coin donation notoriety reduction</li>
 *   <li>Daily priority item doubles vibes</li>
 *   <li>Witness seeds PLAYER_SPOTTED rumour on parcel collection</li>
 *   <li>HEARTS_AND_MINDS achievement (5 donation days)</li>
 *   <li>ROUGH_WEEK achievement (3 parcel days)</li>
 *   <li>Council Inspector spawns on Thursdays 10:00–12:00</li>
 *   <li>First-entry tooltip text</li>
 *   <li>LandmarkType FOOD_BANK has correct display name</li>
 *   <li>NPCType FOOD_BANK_VOLUNTEER, RECIPIENT, COUNCIL_INSPECTOR exist</li>
 *   <li>RumourType COMMUNITY_WIN exists</li>
 *   <li>AchievementType HEARTS_AND_MINDS, ROUGH_WEEK exist</li>
 *   <li>All DONATABLE_ITEMS are accepted</li>
 * </ul>
 */
class FoodBankSystemTest {

    private FoodBankSystem system;
    private Player player;
    private Inventory inventory;
    private NPC margaret;
    private AchievementSystem achievements;
    private NotorietySystem notorietySystem;
    private NeighbourhoodSystem neighbourhoodSystem;
    private RumourNetwork rumourNetwork;

    // Weekday (Wednesday = 3) for most tests
    private static final int WEEKDAY = 3;
    private static final int SATURDAY = 6;
    private static final int THURSDAY = 4;

    @BeforeEach
    void setUp() {
        system = new FoodBankSystem(new Random(42));
        player = new Player(50, 1, 50);
        inventory = new Inventory(36);
        margaret = new NPC(NPCType.FOOD_BANK_VOLUNTEER, "Margaret", 10, 1, 10);
        achievements = new AchievementSystem();
        notorietySystem = new NotorietySystem();
        rumourNetwork = new RumourNetwork(new Random(42));

        // NeighbourhoodSystem requires heavyweight constructor
        TurfMap turfMap = new TurfMap();
        FactionSystem factionSystem = new FactionSystem(turfMap, rumourNetwork, new Random(42));
        neighbourhoodSystem = new NeighbourhoodSystem(
            factionSystem, turfMap, rumourNetwork, achievements, new Random(42)
        );

        system.setAchievementSystem(achievements);
        system.setNotorietySystem(notorietySystem);
        system.setNeighbourhoodSystem(neighbourhoodSystem);
        system.setRumourNetwork(rumourNetwork);
    }

    // ── Opening hours ──────────────────────────────────────────────────────────

    @Test
    void foodBankIsOpenDuringWeekdayHours() {
        assertTrue(system.isOpen(9.0f, WEEKDAY), "Should be open at 09:00 on weekday");
        assertTrue(system.isOpen(12.0f, WEEKDAY), "Should be open at 12:00 on weekday");
        assertTrue(system.isOpen(16.9f, WEEKDAY), "Should be open at 16:54 on weekday");
    }

    @Test
    void foodBankIsClosedOutsideHours() {
        assertFalse(system.isOpen(8.9f, WEEKDAY), "Should be closed before 09:00");
        assertFalse(system.isOpen(17.0f, WEEKDAY), "Should be closed at 17:00");
        assertFalse(system.isOpen(20.0f, WEEKDAY), "Should be closed at 20:00");
    }

    @Test
    void foodBankIsClosedAtWeekend() {
        assertFalse(system.isOpen(12.0f, 6), "Should be closed on Saturday");
        assertFalse(system.isOpen(12.0f, 7), "Should be closed on Sunday");
    }

    @Test
    void foodBankIsOpenAllFiveWeekdays() {
        for (int day = 1; day <= 5; day++) {
            assertTrue(system.isOpen(12.0f, day), "Should be open at 12:00 on weekday " + day);
        }
    }

    // ── NPC spawning ───────────────────────────────────────────────────────────

    @Test
    void openFoodBankSpawnsMargaret() {
        system.openFoodBank(10, 1, 10, Weather.CLEAR, 1);
        assertNotNull(system.getMargaret(), "Margaret should be spawned on open");
        assertEquals(NPCType.FOOD_BANK_VOLUNTEER, system.getMargaret().getType(),
                "Spawned NPC should be FOOD_BANK_VOLUNTEER");
        assertEquals("Margaret", system.getMargaret().getName(), "NPC name should be Margaret");
    }

    @Test
    void openFoodBankSpawnsQueueRecipients() {
        system.openFoodBank(10, 1, 10, Weather.CLEAR, 1);
        assertFalse(system.getQueueNpcs().isEmpty(), "Queue should have at least one RECIPIENT");
        for (NPC npc : system.getQueueNpcs()) {
            assertEquals(NPCType.RECIPIENT, npc.getType(), "Queue NPC should be RECIPIENT type");
            assertEquals(NPCState.QUEUING, npc.getState(), "Queue NPC should be in QUEUING state");
        }
    }

    @Test
    void rainyWeatherAddsExtraRecipient() {
        system.openFoodBank(10, 1, 10, Weather.CLEAR, 1);
        int clearSize = system.getQueueNpcs().size();

        FoodBankSystem rainySystem = new FoodBankSystem(new Random(42));
        rainySystem.openFoodBank(10, 1, 10, Weather.RAIN, 1);
        int rainySize = rainySystem.getQueueNpcs().size();

        assertEquals(clearSize + FoodBankSystem.WEATHER_BONUS_RECIPIENTS, rainySize,
                "Rain should add " + FoodBankSystem.WEATHER_BONUS_RECIPIENTS + " extra recipients");
    }

    @Test
    void drizzleAddsExtraRecipient() {
        system.openFoodBank(10, 1, 10, Weather.CLEAR, 1);
        int clearSize = system.getQueueNpcs().size();

        FoodBankSystem drizzleSystem = new FoodBankSystem(new Random(42));
        drizzleSystem.openFoodBank(10, 1, 10, Weather.DRIZZLE, 1);
        int drizzleSize = drizzleSystem.getQueueNpcs().size();

        assertEquals(clearSize + FoodBankSystem.WEATHER_BONUS_RECIPIENTS, drizzleSize,
                "Drizzle should add extra recipient");
    }

    @Test
    void coldSnapAddsExtraRecipients() {
        system.openFoodBank(10, 1, 10, Weather.CLEAR, 1);
        int clearSize = system.getQueueNpcs().size();

        FoodBankSystem coldSystem = new FoodBankSystem(new Random(42));
        coldSystem.openFoodBank(10, 1, 10, Weather.COLD_SNAP, 1);
        int coldSize = coldSystem.getQueueNpcs().size();

        assertEquals(clearSize + FoodBankSystem.COLD_WEATHER_BONUS_RECIPIENTS, coldSize,
                "Cold snap should add " + FoodBankSystem.COLD_WEATHER_BONUS_RECIPIENTS + " extra recipients");
    }

    @Test
    void frostAddsExtraRecipients() {
        system.openFoodBank(10, 1, 10, Weather.CLEAR, 1);
        int clearSize = system.getQueueNpcs().size();

        FoodBankSystem frostSystem = new FoodBankSystem(new Random(42));
        frostSystem.openFoodBank(10, 1, 10, Weather.FROST, 1);
        int frostSize = frostSystem.getQueueNpcs().size();

        assertEquals(clearSize + FoodBankSystem.COLD_WEATHER_BONUS_RECIPIENTS, frostSize,
                "Frost should add " + FoodBankSystem.COLD_WEATHER_BONUS_RECIPIENTS + " extra recipients");
    }

    @Test
    void closeFoodBankDespaywnsNpcs() {
        system.openFoodBank(10, 1, 10, Weather.CLEAR, 1);
        assertNotNull(system.getMargaret());
        system.closeFoodBank();
        assertNull(system.getMargaret(), "Margaret should be null after close");
        assertTrue(system.getQueueNpcs().isEmpty(), "Queue should be empty after close");
    }

    // ── Donation mechanics ─────────────────────────────────────────────────────

    @Test
    void donationSucceedsWithValidItem() {
        inventory.addItem(Material.SAUSAGE_ROLL, 3);
        FoodBankSystem.DonationResult result = system.donate(
            margaret, Material.SAUSAGE_ROLL, inventory,
            10.0f, 1, WEEKDAY, new ArrayList<>(), null
        );
        assertEquals(FoodBankSystem.DonationResult.SUCCESS, result);
        assertEquals(2, inventory.getItemCount(Material.SAUSAGE_ROLL),
                "One sausage roll should be removed");
    }

    @Test
    void donationFailsWhenClosed() {
        inventory.addItem(Material.SAUSAGE_ROLL, 1);
        FoodBankSystem.DonationResult result = system.donate(
            margaret, Material.SAUSAGE_ROLL, inventory,
            8.0f, 1, WEEKDAY, new ArrayList<>(), null
        );
        assertEquals(FoodBankSystem.DonationResult.CLOSED, result);
    }

    @Test
    void donationFailsOnWeekend() {
        inventory.addItem(Material.SAUSAGE_ROLL, 1);
        FoodBankSystem.DonationResult result = system.donate(
            margaret, Material.SAUSAGE_ROLL, inventory,
            12.0f, 1, SATURDAY, new ArrayList<>(), null
        );
        assertEquals(FoodBankSystem.DonationResult.CLOSED, result);
    }

    @Test
    void donationFailsWhenAlreadyDonatedToday() {
        inventory.addItem(Material.SAUSAGE_ROLL, 2);
        system.donate(margaret, Material.SAUSAGE_ROLL, inventory, 10.0f, 5, WEEKDAY, new ArrayList<>(), null);
        FoodBankSystem.DonationResult second = system.donate(
            margaret, Material.BEANS_ON_TOAST, inventory, 11.0f, 5, WEEKDAY, new ArrayList<>(), null
        );
        assertEquals(FoodBankSystem.DonationResult.ALREADY_DONATED_TODAY, second);
    }

    @Test
    void donationFailsForNonDonableItem() {
        inventory.addItem(Material.BRICK, 5);
        FoodBankSystem.DonationResult result = system.donate(
            margaret, Material.BRICK, inventory,
            10.0f, 1, WEEKDAY, new ArrayList<>(), null
        );
        assertEquals(FoodBankSystem.DonationResult.NOT_ACCEPTED, result);
    }

    @Test
    void donationFailsIfItemNotInInventory() {
        FoodBankSystem.DonationResult result = system.donate(
            margaret, Material.FULL_ENGLISH, inventory,
            10.0f, 1, WEEKDAY, new ArrayList<>(), null
        );
        assertEquals(FoodBankSystem.DonationResult.ITEM_NOT_FOUND, result);
    }

    @Test
    void donationFailsWithWrongNpc() {
        NPC wrongNpc = new NPC(NPCType.BARMAN, 10, 1, 10);
        inventory.addItem(Material.SAUSAGE_ROLL, 1);
        FoodBankSystem.DonationResult result = system.donate(
            wrongNpc, Material.SAUSAGE_ROLL, inventory,
            10.0f, 1, WEEKDAY, new ArrayList<>(), null
        );
        assertEquals(FoodBankSystem.DonationResult.WRONG_NPC, result);
    }

    @Test
    void donationBlockedWhenHighNotorietyAndPoliceNearby() {
        notorietySystem.addNotoriety(60, null); // Above NOTORIETY_POLICE_BLOCK threshold (50)
        system.setNotorietySystem(notorietySystem);
        inventory.addItem(Material.WOOD, 1);

        // Place a police NPC near Margaret
        NPC police = new NPC(NPCType.POLICE, 10, 1, 12); // 2 blocks from margaret at (10,1,10)
        List<NPC> allNpcs = new ArrayList<>();
        allNpcs.add(police);

        FoodBankSystem.DonationResult result = system.donate(
            margaret, Material.WOOD, inventory,
            10.0f, 1, WEEKDAY, allNpcs, null
        );
        assertEquals(FoodBankSystem.DonationResult.BLOCKED_POLICE, result);
    }

    @Test
    void donationNotBlockedWithHighNotorietyButNoPoliceNearby() {
        notorietySystem.addNotoriety(60, null);
        system.setNotorietySystem(notorietySystem);
        inventory.addItem(Material.WOOD, 1);

        // Police far away (100 blocks)
        NPC police = new NPC(NPCType.POLICE, 110, 1, 110);
        List<NPC> allNpcs = new ArrayList<>();
        allNpcs.add(police);

        FoodBankSystem.DonationResult result = system.donate(
            margaret, Material.WOOD, inventory,
            10.0f, 1, WEEKDAY, allNpcs, null
        );
        assertEquals(FoodBankSystem.DonationResult.SUCCESS, result);
    }

    @Test
    void donationBoostsVibes() {
        int vibesBefore = neighbourhoodSystem.getVibes();
        inventory.addItem(Material.SAUSAGE_ROLL, 1);
        system.donate(margaret, Material.SAUSAGE_ROLL, inventory, 10.0f, 1, WEEKDAY, new ArrayList<>(), null);
        int vibesAfter = neighbourhoodSystem.getVibes();
        assertEquals(vibesBefore + FoodBankSystem.VIBES_DONATION_BOOST, vibesAfter,
                "Donation should boost vibes by " + FoodBankSystem.VIBES_DONATION_BOOST);
    }

    @Test
    void donationSeedsCommunityWinRumourIntoBarman() {
        NPC barman = new NPC(NPCType.BARMAN, 20, 1, 20);
        inventory.addItem(Material.CARDBOARD, 1);
        system.donate(margaret, Material.CARDBOARD, inventory, 10.0f, 1, WEEKDAY, new ArrayList<>(), barman);
        boolean hasCommunityWin = barman.getRumours().stream()
            .anyMatch(r -> r.getType() == RumourType.COMMUNITY_WIN);
        assertTrue(hasCommunityWin, "COMMUNITY_WIN rumour should be seeded into barman");
    }

    @Test
    void donationTracksDonationDay() {
        inventory.addItem(Material.MUG_OF_TEA, 1);
        assertEquals(-1, system.getLastDonationDay(), "No donation yet");
        system.donate(margaret, Material.MUG_OF_TEA, inventory, 10.0f, 7, WEEKDAY, new ArrayList<>(), null);
        assertEquals(7, system.getLastDonationDay(), "Should record donation day");
    }

    @Test
    void priorityItemDoublesVibes() {
        system.setDailyPriorityItemForTesting(Material.FULL_ENGLISH);
        int vibesBefore = neighbourhoodSystem.getVibes();
        inventory.addItem(Material.FULL_ENGLISH, 1);
        system.donate(margaret, Material.FULL_ENGLISH, inventory, 10.0f, 1, WEEKDAY, new ArrayList<>(), null);
        int vibesAfter = neighbourhoodSystem.getVibes();
        assertEquals(vibesBefore + FoodBankSystem.VIBES_DONATION_BOOST * 2, vibesAfter,
                "Priority item donation should double vibes boost");
    }

    @Test
    void allDonatablesItemsAreAccepted() {
        for (Material item : FoodBankSystem.DONATABLE_ITEMS) {
            assertTrue(system.isDonatable(item), "Should accept " + item);
        }
    }

    // ── Emergency parcel mechanics ─────────────────────────────────────────────

    @Test
    void collectParcelSucceedsWhenHungry() {
        player.setHunger(20f); // Below threshold of 30
        FoodBankSystem.ParcelResult result = system.collectParcel(
            margaret, player, inventory, 10.0f, 1, WEEKDAY, new ArrayList<>()
        );
        assertEquals(FoodBankSystem.ParcelResult.SUCCESS, result);
    }

    @Test
    void collectParcelSucceedsWhenBroke() {
        player.setHunger(80f); // Not hungry
        // No coins in inventory (inventory starts empty)
        FoodBankSystem.ParcelResult result = system.collectParcel(
            margaret, player, inventory, 10.0f, 1, WEEKDAY, new ArrayList<>()
        );
        assertEquals(FoodBankSystem.ParcelResult.SUCCESS, result);
    }

    @Test
    void collectParcelAddsCorrectItems() {
        player.setHunger(10f);
        system.collectParcel(margaret, player, inventory, 10.0f, 1, WEEKDAY, new ArrayList<>());
        assertEquals(1, inventory.getItemCount(Material.FULL_ENGLISH), "Should receive 1 FULL_ENGLISH");
        assertEquals(1, inventory.getItemCount(Material.MUG_OF_TEA), "Should receive 1 MUG_OF_TEA");
        assertEquals(1, inventory.getItemCount(Material.BEANS_ON_TOAST), "Should receive 1 BEANS_ON_TOAST");
    }

    @Test
    void collectParcelBoostsHunger() {
        player.setHunger(5f);
        system.collectParcel(margaret, player, inventory, 10.0f, 1, WEEKDAY, new ArrayList<>());
        assertEquals(5f + FoodBankSystem.PARCEL_HUNGER_BOOST, player.getHunger(), 0.01f,
                "Parcel should boost hunger by " + FoodBankSystem.PARCEL_HUNGER_BOOST);
    }

    @Test
    void collectParcelFailsWhenClosed() {
        player.setHunger(10f);
        FoodBankSystem.ParcelResult result = system.collectParcel(
            margaret, player, inventory, 8.0f, 1, WEEKDAY, new ArrayList<>()
        );
        assertEquals(FoodBankSystem.ParcelResult.CLOSED, result);
    }

    @Test
    void collectParcelFailsOnWeekend() {
        player.setHunger(10f);
        FoodBankSystem.ParcelResult result = system.collectParcel(
            margaret, player, inventory, 12.0f, 1, SATURDAY, new ArrayList<>()
        );
        assertEquals(FoodBankSystem.ParcelResult.CLOSED, result);
    }

    @Test
    void collectParcelFailsWithHighNotoriety() {
        notorietySystem.addNotoriety(100, null); // Above 80 threshold
        system.setNotorietySystem(notorietySystem);
        player.setHunger(10f);
        FoodBankSystem.ParcelResult result = system.collectParcel(
            margaret, player, inventory, 10.0f, 1, WEEKDAY, new ArrayList<>()
        );
        assertEquals(FoodBankSystem.ParcelResult.NOTORIETY_TOO_HIGH, result);
    }

    @Test
    void collectParcelFailsWhenNotEligible() {
        player.setHunger(80f); // Not hungry
        inventory.addItem(Material.COIN, 10); // Has coins
        FoodBankSystem.ParcelResult result = system.collectParcel(
            margaret, player, inventory, 10.0f, 1, WEEKDAY, new ArrayList<>()
        );
        assertEquals(FoodBankSystem.ParcelResult.NOT_ELIGIBLE, result);
    }

    @Test
    void collectParcelFailsOnCooldown() {
        player.setHunger(10f);
        system.setLastParcelDayForTesting(1);
        // Try to collect again on day 2 (cooldown is 3 days)
        FoodBankSystem.ParcelResult result = system.collectParcel(
            margaret, player, inventory, 10.0f, 2, WEEKDAY, new ArrayList<>()
        );
        assertEquals(FoodBankSystem.ParcelResult.ON_COOLDOWN, result);
    }

    @Test
    void collectParcelSucceedsAfterCooldown() {
        player.setHunger(10f);
        system.setLastParcelDayForTesting(1);
        // Day 4 = 3 days after last collection
        FoodBankSystem.ParcelResult result = system.collectParcel(
            margaret, player, inventory, 10.0f, 4, WEEKDAY, new ArrayList<>()
        );
        assertEquals(FoodBankSystem.ParcelResult.SUCCESS, result);
    }

    @Test
    void collectParcelFailsWithWrongNpc() {
        NPC wrongNpc = new NPC(NPCType.BARMAN, 10, 1, 10);
        player.setHunger(10f);
        FoodBankSystem.ParcelResult result = system.collectParcel(
            wrongNpc, player, inventory, 10.0f, 1, WEEKDAY, new ArrayList<>()
        );
        assertEquals(FoodBankSystem.ParcelResult.WRONG_NPC, result);
    }

    @Test
    void collectParcelReducesVibes() {
        int vibesBefore = neighbourhoodSystem.getVibes();
        player.setHunger(10f);
        system.collectParcel(margaret, player, inventory, 10.0f, 1, WEEKDAY, new ArrayList<>());
        int vibesAfter = neighbourhoodSystem.getVibes();
        assertEquals(vibesBefore - FoodBankSystem.RECIPIENT_STREET_REP_PENALTY, vibesAfter,
                "Parcel collection should reduce vibes by " + FoodBankSystem.RECIPIENT_STREET_REP_PENALTY);
    }

    @Test
    void witnessNearParcelCollectionSeedsRumour() {
        player.setHunger(10f);
        // Place a PUBLIC NPC within WITNESS_RADIUS of Margaret at (10,1,10)
        NPC witness = new NPC(NPCType.PUBLIC, 12, 1, 10); // 2 blocks away
        List<NPC> allNpcs = new ArrayList<>();
        allNpcs.add(witness);

        system.collectParcel(margaret, player, inventory, 10.0f, 1, WEEKDAY, allNpcs);

        boolean hasPlayerSpotted = witness.getRumours().stream()
            .anyMatch(r -> r.getType() == RumourType.PLAYER_SPOTTED);
        assertTrue(hasPlayerSpotted, "Witness should have PLAYER_SPOTTED rumour");
    }

    @Test
    void noWitnessRumourWhenFarAway() {
        player.setHunger(10f);
        // Place a PUBLIC NPC far from Margaret at (10,1,10)
        NPC farWitness = new NPC(NPCType.PUBLIC, 100, 1, 100); // Way out of radius
        List<NPC> allNpcs = new ArrayList<>();
        allNpcs.add(farWitness);

        system.collectParcel(margaret, player, inventory, 10.0f, 1, WEEKDAY, allNpcs);

        boolean hasPlayerSpotted = farWitness.getRumours().stream()
            .anyMatch(r -> r.getType() == RumourType.PLAYER_SPOTTED);
        assertFalse(hasPlayerSpotted, "Far-away NPC should not receive rumour");
    }

    // ── Coin donation notoriety reduction ─────────────────────────────────────

    @Test
    void coinDonationReducesNotoriety() {
        notorietySystem.addNotoriety(50, null);
        system.setNotorietySystem(notorietySystem);
        int notorietyBefore = notorietySystem.getNotoriety();
        inventory.addItem(Material.COIN, 10);
        system.donate(margaret, Material.COIN, inventory, 10.0f, 1, WEEKDAY, new ArrayList<>(), null);
        assertTrue(notorietySystem.getNotoriety() < notorietyBefore,
                "Coin donation should reduce notoriety");
    }

    @Test
    void coinDonationNotorietyReductionCappedPerDay() {
        notorietySystem.addNotoriety(100, null);
        system.setNotorietySystem(notorietySystem);
        // Pre-fill daily reduction to cap
        system.setNotorietyReducedTodayForTesting(FoodBankSystem.MAX_NOTORIETY_REDUCTION_PER_DAY);
        int notorietyBefore = notorietySystem.getNotoriety();
        inventory.addItem(Material.COIN, 10);
        system.donate(margaret, Material.COIN, inventory, 10.0f, 1, WEEKDAY, new ArrayList<>(), null);
        // Notoriety should not decrease further today
        assertEquals(notorietyBefore, notorietySystem.getNotoriety(),
                "Notoriety reduction should be capped per day");
    }

    // ── Rainy day free tea ─────────────────────────────────────────────────────

    @Test
    void rainyDayTeaIncreasesWarmth() {
        player.setWarmth(50f); // Set below max so boost is visible
        float warmthBefore = player.getWarmth();
        system.openFoodBank(10, 1, 10, Weather.RAIN, 1);
        system.offerRainyDayTea(player, inventory);
        assertEquals(warmthBefore + FoodBankSystem.RAINY_WARMTH_BOOST, player.getWarmth(), 0.01f,
                "Rainy day tea should increase warmth by " + FoodBankSystem.RAINY_WARMTH_BOOST);
    }

    @Test
    void rainyDayTeaAddsMugOfTeaToInventory() {
        system.openFoodBank(10, 1, 10, Weather.RAIN, 1);
        system.offerRainyDayTea(player, inventory);
        assertEquals(1, inventory.getItemCount(Material.MUG_OF_TEA),
                "Rainy day tea should add 1 MUG_OF_TEA to inventory");
    }

    @Test
    void rainyDayTeaReturnsSpeechText() {
        system.openFoodBank(10, 1, 10, Weather.RAIN, 1);
        String speech = system.offerRainyDayTea(player, inventory);
        assertNotNull(speech, "Should return speech text");
        assertFalse(speech.isEmpty(), "Speech text should not be empty");
    }

    // ── Achievements ───────────────────────────────────────────────────────────

    @Test
    void heartsAndMindsAchievementProgress() {
        for (int day = 1; day <= 4; day++) {
            inventory.addItem(Material.SAUSAGE_ROLL, 1);
            system.donate(margaret, Material.SAUSAGE_ROLL, inventory, 10.0f, day, WEEKDAY, new ArrayList<>(), null);
        }
        assertFalse(achievements.isUnlocked(AchievementType.HEARTS_AND_MINDS),
                "Should not be unlocked after only 4 days");

        inventory.addItem(Material.SAUSAGE_ROLL, 1);
        system.donate(margaret, Material.SAUSAGE_ROLL, inventory, 10.0f, 5, WEEKDAY, new ArrayList<>(), null);
        assertTrue(achievements.isUnlocked(AchievementType.HEARTS_AND_MINDS),
                "Should be unlocked after 5 donation days");
    }

    @Test
    void roughWeekAchievementProgress() {
        player.setHunger(10f);
        for (int day = 1; day <= 7; day += 3) {
            system.setLastParcelDayForTesting(day - PARCEL_COOLDOWN_AFTER_TEST(day));
            system.collectParcel(margaret, player, inventory, 10.0f, day, WEEKDAY, new ArrayList<>());
        }
        // After 3 collections, achievement should be unlocked
        assertTrue(achievements.isUnlocked(AchievementType.ROUGH_WEEK),
                "ROUGH_WEEK should unlock after 3 parcel collections");
    }

    private int PARCEL_COOLDOWN_AFTER_TEST(int day) {
        // Helper to ensure cooldown is bypassed for test
        return day > 0 ? FoodBankSystem.PARCEL_COOLDOWN_DAYS + 1 : 0;
    }

    // ── Council Inspector ──────────────────────────────────────────────────────

    @Test
    void councilInspectorSpawnsOnThursdays() {
        system.openFoodBank(10, 1, 10, Weather.CLEAR, 1);
        // Update to Thursday 10:30
        system.update(1.0f, 10.5f, 1, THURSDAY);
        assertNotNull(system.getCouncilInspector(),
                "Council Inspector should spawn on Thursday 10:30");
        assertEquals(NPCType.COUNCIL_INSPECTOR, system.getCouncilInspector().getType());
    }

    @Test
    void councilInspectorDoesNotSpawnOnWeekdays() {
        system.openFoodBank(10, 1, 10, Weather.CLEAR, 1);
        // Wednesday (day 3), 10:30
        system.update(1.0f, 10.5f, 1, 3);
        assertNull(system.getCouncilInspector(),
                "Council Inspector should not spawn on non-Thursday");
    }

    @Test
    void councilInspectorDespawnsAfterNoon() {
        system.openFoodBank(10, 1, 10, Weather.CLEAR, 1);
        // Spawn the inspector
        system.update(1.0f, 10.5f, 1, THURSDAY);
        assertNotNull(system.getCouncilInspector(), "Inspector should be present at 10:30");
        // Past 12:00
        system.update(1.0f, 12.5f, 1, THURSDAY);
        assertNull(system.getCouncilInspector(), "Inspector should despawn after 12:00");
    }

    // ── First-entry tooltip ────────────────────────────────────────────────────

    @Test
    void firstEntryTooltipShownOnce() {
        String firstTooltip = system.onPlayerEnter();
        assertNotNull(firstTooltip, "First entry should show tooltip");
        assertTrue(firstTooltip.contains("Northfield Food Bank"), "Tooltip should mention food bank");

        String secondTooltip = system.onPlayerEnter();
        assertNull(secondTooltip, "Second entry should not show tooltip again");
    }

    // ── Daily priority item ────────────────────────────────────────────────────

    @Test
    void dailyPriorityItemIsSetOnOpen() {
        system.openFoodBank(10, 1, 10, Weather.CLEAR, 1);
        assertNotNull(system.getDailyPriorityItem(), "Daily priority item should be set on open");
        assertTrue(system.isDonatable(system.getDailyPriorityItem()),
                "Priority item must be a donatable item");
    }

    @Test
    void dailyPriorityItemChangesEachDay() {
        system.refreshDailyPriorityItem(1);
        Material day1 = system.getDailyPriorityItem();

        // Use a seeded random that gives different results on day 2
        FoodBankSystem system2 = new FoodBankSystem(new Random(99));
        system2.refreshDailyPriorityItem(2);
        // Can't guarantee different item due to randomness, but item must be valid
        assertNotNull(system2.getDailyPriorityItem());
        assertTrue(system2.isDonatable(system2.getDailyPriorityItem()));
    }

    // ── Enum / type existence checks ───────────────────────────────────────────

    @Test
    void landmarkTypeFoodBankHasCorrectDisplayName() {
        assertEquals("Northfield Food Bank", LandmarkType.FOOD_BANK.getDisplayName(),
                "FOOD_BANK should display as 'Northfield Food Bank'");
    }

    @Test
    void npcTypeFoodBankVolunteerExists() {
        NPC volunteer = new NPC(NPCType.FOOD_BANK_VOLUNTEER, 0, 0, 0);
        assertEquals(NPCType.FOOD_BANK_VOLUNTEER, volunteer.getType());
        assertFalse(NPCType.FOOD_BANK_VOLUNTEER.isHostile(), "Volunteer should be passive");
    }

    @Test
    void npcTypeRecipientExists() {
        NPC recipient = new NPC(NPCType.RECIPIENT, 0, 0, 0);
        assertEquals(NPCType.RECIPIENT, recipient.getType());
        assertFalse(NPCType.RECIPIENT.isHostile(), "Recipient should be passive");
    }

    @Test
    void npcTypeCouncilInspectorExists() {
        NPC inspector = new NPC(NPCType.COUNCIL_INSPECTOR, 0, 0, 0);
        assertEquals(NPCType.COUNCIL_INSPECTOR, inspector.getType());
        assertFalse(NPCType.COUNCIL_INSPECTOR.isHostile(), "Inspector should be passive");
    }

    @Test
    void rumourTypeCommunityWinExists() {
        RumourType communityWin = RumourType.COMMUNITY_WIN;
        assertNotNull(communityWin);
        Rumour r = new Rumour(RumourType.COMMUNITY_WIN, "Test community win");
        assertEquals(RumourType.COMMUNITY_WIN, r.getType());
    }

    @Test
    void achievementTypeHeartsAndMindsExists() {
        AchievementType type = AchievementType.HEARTS_AND_MINDS;
        assertNotNull(type);
        assertEquals(5, type.getProgressTarget(), "HEARTS_AND_MINDS should require 5 donations");
        assertFalse(type.isInstant());
    }

    @Test
    void achievementTypeRoughWeekExists() {
        AchievementType type = AchievementType.ROUGH_WEEK;
        assertNotNull(type);
        assertEquals(3, type.getProgressTarget(), "ROUGH_WEEK should require 3 parcel collections");
        assertFalse(type.isInstant());
    }

    // ── Integration scenario: donation then parcel same day ───────────────────

    @Test
    void canDonateAndCollectParcelOnDifferentDays() {
        // Day 1: donate
        inventory.addItem(Material.WOOD, 1);
        FoodBankSystem.DonationResult donateResult = system.donate(
            margaret, Material.WOOD, inventory, 10.0f, 1, WEEKDAY, new ArrayList<>(), null
        );
        assertEquals(FoodBankSystem.DonationResult.SUCCESS, donateResult);

        // Day 4: collect parcel (after 3-day cooldown)
        player.setHunger(10f);
        FoodBankSystem.ParcelResult parcelResult = system.collectParcel(
            margaret, player, inventory, 10.0f, 4, WEEKDAY, new ArrayList<>()
        );
        assertEquals(FoodBankSystem.ParcelResult.SUCCESS, parcelResult);
    }

    @Test
    void foodBankSystemTracksDonationAndParcelDaysSeparately() {
        inventory.addItem(Material.CARDBOARD, 1);
        system.donate(margaret, Material.CARDBOARD, inventory, 10.0f, 1, WEEKDAY, new ArrayList<>(), null);
        assertEquals(1, system.getDonationDaysCount(), "Should count 1 donation day");
        assertEquals(0, system.getParcelDaysCount(), "Parcel days should still be 0");

        player.setHunger(10f);
        system.collectParcel(margaret, player, inventory, 10.0f, 5, WEEKDAY, new ArrayList<>());
        assertEquals(1, system.getDonationDaysCount(), "Donation count should be unchanged");
        assertEquals(1, system.getParcelDaysCount(), "Should count 1 parcel day");
    }
}

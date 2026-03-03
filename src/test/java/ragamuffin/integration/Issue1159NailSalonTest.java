package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord;
import ragamuffin.core.NailSalonSystem;
import ragamuffin.core.NotorietySystem;
import ragamuffin.core.Rumour;
import ragamuffin.core.RumourNetwork;
import ragamuffin.core.RumourType;
import ragamuffin.core.TimeSystem;
import ragamuffin.core.Weather;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1159: Angel Nails &amp; Beauty — Gossip Economy,
 * WAG Culture &amp; the Acrylic Hustle.
 */
class Issue1159NailSalonTest {

    private NailSalonSystem salon;
    private Player player;
    private Inventory inventory;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private TimeSystem timeSystem;
    private List<AchievementType> awarded;
    private NotorietySystem.AchievementCallback achievementCb;

    @BeforeEach
    void setUp() {
        salon = new NailSalonSystem(new Random(42L));
        player = new Player(0, 1, 0);
        inventory = new Inventory();
        notorietySystem = new NotorietySystem();
        criminalRecord = new CriminalRecord();
        timeSystem = new TimeSystem(10.0f);
        awarded = new ArrayList<>();
        achievementCb = type -> awarded.add(type);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Opening hours
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void isOpen_mondayMorning_returnsTrue() {
        assertTrue(salon.isOpen(9.0f, NailSalonSystem.MONDAY));
    }

    @Test
    void isOpen_mondayEveningAtClose_returnsFalse() {
        // 19:30 on Monday — after close
        assertFalse(salon.isOpen(19.5f, NailSalonSystem.MONDAY));
    }

    @Test
    void isOpen_sunday_returnsFalse() {
        // Closed all day Sunday
        assertFalse(salon.isOpen(12.0f, NailSalonSystem.SUNDAY));
    }

    @Test
    void isOpen_saturdayDuringHours_returnsTrue() {
        assertTrue(salon.isOpen(10.0f, NailSalonSystem.SATURDAY));
    }

    @Test
    void isOpen_beforeOpeningTime_returnsFalse() {
        assertFalse(salon.isOpen(8.5f, NailSalonSystem.MONDAY));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WAG Saturday window
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void isWagSaturdayWindow_saturdayMidWindow_returnsTrue() {
        assertTrue(salon.isWagSaturdayWindow(10.5f, NailSalonSystem.SATURDAY));
    }

    @Test
    void isWagSaturdayWindow_saturdayAfterWindow_returnsFalse() {
        // 12:30 is past the 12:00 end
        assertFalse(salon.isWagSaturdayWindow(12.5f, NailSalonSystem.SATURDAY));
    }

    @Test
    void isWagSaturdayWindow_weekday_returnsFalse() {
        assertFalse(salon.isWagSaturdayWindow(10.5f, NailSalonSystem.MONDAY));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Service pricing
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getServicePrice_manicure_weekday_returnsBase() {
        assertEquals(NailSalonSystem.PRICE_MANICURE,
                salon.getServicePrice(NailSalonSystem.ServiceType.MANICURE, false));
    }

    @Test
    void getServicePrice_gel_saturday_returnsMultipliedRoundedUp() {
        // 5 × 1.5 = 7.5 → rounded up to 8
        assertEquals(8, salon.getServicePrice(NailSalonSystem.ServiceType.GEL, true));
    }

    @Test
    void getServicePrice_acrylics_weekday_returnsBase() {
        assertEquals(NailSalonSystem.PRICE_ACRYLICS,
                salon.getServicePrice(NailSalonSystem.ServiceType.ACRYLICS, false));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Nail polish theft catch chances
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getNailPolishTheftCatchChance_kimNotWatching_returnsBase() {
        assertEquals(NailSalonSystem.THEFT_CATCH_CHANCE_BASE,
                salon.getNailPolishTheftCatchChance(false), 0.001f);
    }

    @Test
    void getNailPolishTheftCatchChance_kimWatching_returnsHigher() {
        assertEquals(NailSalonSystem.THEFT_CATCH_CHANCE_KIM,
                salon.getNailPolishTheftCatchChance(true), 0.001f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Back-room access
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void isBackRoomAccessible_atThreshold_returnsTrue() {
        assertTrue(salon.isBackRoomAccessible(NailSalonSystem.BACK_ROOM_REP_THRESHOLD));
    }

    @Test
    void isBackRoomAccessible_belowThreshold_returnsFalse() {
        assertFalse(salon.isBackRoomAccessible(NailSalonSystem.BACK_ROOM_REP_THRESHOLD - 1));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Client count
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getClientCount_rain_returnsBasesPlusOne() {
        // vibes=50 → normal, rain → base+1
        int normal = salon.getClientCount(Weather.OVERCAST, 50);
        int rainy  = salon.getClientCount(Weather.RAIN, 50);
        assertEquals(normal + NailSalonSystem.RAIN_EXTRA_CLIENT_COUNT, rainy);
    }

    @Test
    void getClientCount_lowVibes_capsAtOne() {
        int count = salon.getClientCount(Weather.OVERCAST, NailSalonSystem.BOARD_UP_VIBES_THRESHOLD - 1);
        assertEquals(NailSalonSystem.LOW_VIBES_CLIENT_COUNT, count);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Integration test 1: Manicure reduces displayed Notoriety
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void manicure_reducesDisplayNotoriety_andIncrementsVisitCount() {
        // Set player Notoriety to 20
        notorietySystem.addNotoriety(20, achievementCb);
        int notorietyBefore = notorietySystem.getNotoriety();

        // Give player enough coin
        inventory.addItem(Material.COIN, 5);

        // Purchase a manicure at 11:00 Monday
        NailSalonSystem.ServiceResult result = salon.purchaseService(
                NailSalonSystem.ServiceType.MANICURE,
                inventory,
                11.0f, NailSalonSystem.MONDAY,
                0 /* wanted stars */,
                notorietySystem, achievementCb);

        assertEquals(NailSalonSystem.ServiceResult.SUCCESS, result);
        // 2 COIN deducted
        assertEquals(3, inventory.getItemCount(Material.COIN));
        // Notoriety reduced by 1
        assertEquals(notorietyBefore - NailSalonSystem.MANICURE_NOTORIETY_REDUCTION,
                notorietySystem.getNotoriety());
        // Visit counter incremented
        assertEquals(1, salon.getTotalVisits());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Integration test 2: Gossip exchange seeds rumour
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void gossipExchange_seedsSalonGossipRumour_andIncrementsCounter() {
        RumourNetwork rumourNetwork = new RumourNetwork();
        NPC clientNpc = new NPC(NPCType.PUBLIC, 2, 1, 2);

        boolean exchanged = salon.performGossipExchange(clientNpc, rumourNetwork, achievementCb);

        assertTrue(exchanged);
        // SALON_GOSSIP rumour should now exist in network
        List<Rumour> rumours = rumourNetwork.getRumoursFrom(clientNpc);
        assertFalse(rumours.isEmpty(), "RumourNetwork should have at least one SALON_GOSSIP rumour");
        assertTrue(rumours.stream().anyMatch(r -> r.getType() == RumourType.SALON_GOSSIP),
                "At least one rumour should be of type SALON_GOSSIP");
        // Exchange counter incremented
        assertEquals(1, salon.getTotalGossipExchanges());
    }

    @Test
    void gossipExchange_tenExchanges_awardsGossipQueenAchievement() {
        RumourNetwork rumourNetwork = new RumourNetwork();
        NPC clientNpc = new NPC(NPCType.PUBLIC, 2, 1, 2);

        salon.setTotalGossipExchangesForTesting(NailSalonSystem.GOSSIP_QUEEN_EXCHANGE_COUNT - 1);
        salon.performGossipExchange(clientNpc, rumourNetwork, achievementCb);

        assertTrue(awarded.contains(AchievementType.GOSSIP_QUEEN));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Integration test 3: Back-door fence
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void collectFenceDrop_atThresholdRep_withCourierDrop_addsItem() {
        salon.setCourierDropForTesting(true);

        boolean collected = salon.collectFenceDrop(inventory, NailSalonSystem.BACK_ROOM_REP_THRESHOLD);

        assertTrue(collected);
        // One of the three possible drops should be in inventory
        int total = inventory.getItemCount(Material.NAIL_POLISH)
                + inventory.getItemCount(Material.STOLEN_JEWELLERY)
                + inventory.getItemCount(Material.COUNTERFEIT_PERFUME);
        assertTrue(total > 0, "Inventory should contain at least one fenced item");
    }

    @Test
    void collectFenceDrop_belowThresholdRep_returnsFalse() {
        salon.setCourierDropForTesting(true);
        boolean collected = salon.collectFenceDrop(inventory, NailSalonSystem.BACK_ROOM_REP_THRESHOLD - 1);
        assertFalse(collected);
    }

    @Test
    void collectFenceDrop_noDrop_returnsFalse() {
        salon.setCourierDropForTesting(false);
        boolean collected = salon.collectFenceDrop(inventory, NailSalonSystem.BACK_ROOM_REP_THRESHOLD);
        assertFalse(collected);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Integration test 4: Nail polish theft caught by Kim
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void nailPolishTheft_caught_appliesBanAndNotoriety() {
        // Use a deterministic RNG that returns below the catch threshold (0.60)
        NailSalonSystem caughtSalon = new NailSalonSystem(new Random(3L) {
            @Override public float nextFloat() { return 0.1f; }
        });

        int notorietyBefore = notorietySystem.getNotoriety();

        boolean success = caughtSalon.attemptNailPolishTheft(
                inventory, true /* kimWatching */,
                notorietySystem, criminalRecord, achievementCb);

        assertFalse(success, "Theft should be caught");
        // No nail polish added
        assertEquals(0, inventory.getItemCount(Material.NAIL_POLISH));
        // Banned from salon
        assertTrue(caughtSalon.isBannedFromSalon());
        // Notoriety increased
        assertTrue(notorietySystem.getNotoriety() > notorietyBefore);
        // Criminal record contains SHOPLIFTING
        assertTrue(criminalRecord.getCount(CriminalRecord.CrimeType.SHOPLIFTING) > 0);
    }

    @Test
    void nailPolishTheft_uncaught_addsPolishAndAwardsAchievement() {
        // Use seed that produces > 0.25 with kimNotWatching=false...
        // Random(42L) first nextFloat() ≈ 0.7288 > 0.25, so not caught
        // Seed 42L should NOT catch (kimWatching=false, 0.25 threshold)
        NailSalonSystem freeSalon = new NailSalonSystem(new Random(42L));

        boolean success = freeSalon.attemptNailPolishTheft(
                inventory, false /* kimNotWatching */,
                notorietySystem, criminalRecord, achievementCb);

        assertTrue(success, "Theft should succeed (uncaught)");
        assertEquals(1, inventory.getItemCount(Material.NAIL_POLISH));
        assertTrue(awarded.contains(AchievementType.FIVE_FINGER_DISCOUNT_DELUXE));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Integration test 5: WAG Saturday — Stacey Marchetti
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void wagSaturday_staceyAppears_seedsMarchettiRumour() {
        RumourNetwork rumourNetwork = new RumourNetwork();
        NPC staceyNpc = new NPC(NPCType.PUBLIC, 3, 1, 2);

        boolean triggered = salon.triggerStaceyIntel(
                10.5f, NailSalonSystem.SATURDAY,
                NailSalonSystem.STACEY_MARCHETTI_RESPECT_THRESHOLD,
                rumourNetwork, staceyNpc, achievementCb);

        assertTrue(triggered);
        // Achievement awarded
        assertTrue(awarded.contains(AchievementType.STACEY_INTEL));
        // CONTRABAND_SHIPMENT rumour seeded
        List<Rumour> rumours = rumourNetwork.getRumoursFrom(staceyNpc);
        assertFalse(rumours.isEmpty());
        assertTrue(rumours.stream().anyMatch(r -> r.getType() == RumourType.CONTRABAND_SHIPMENT));
    }

    @Test
    void wagSaturday_insufficientRespect_doesNotTrigger() {
        RumourNetwork rumourNetwork = new RumourNetwork();
        NPC staceyNpc = new NPC(NPCType.PUBLIC, 3, 1, 2);

        boolean triggered = salon.triggerStaceyIntel(
                10.5f, NailSalonSystem.SATURDAY,
                NailSalonSystem.STACEY_MARCHETTI_RESPECT_THRESHOLD - 1,
                rumourNetwork, staceyNpc, achievementCb);

        assertFalse(triggered);
        assertFalse(awarded.contains(AchievementType.STACEY_INTEL));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Integration test 6: Salon closes on Sundays
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void salon_closedOnSunday_serviceReturnsClosed() {
        inventory.addItem(Material.COIN, 10);

        NailSalonSystem.ServiceResult result = salon.purchaseService(
                NailSalonSystem.ServiceType.MANICURE,
                inventory,
                12.0f, NailSalonSystem.SUNDAY,
                0, notorietySystem, achievementCb);

        assertEquals(NailSalonSystem.ServiceResult.SHOP_CLOSED, result);
        // No coin deducted
        assertEquals(10, inventory.getItemCount(Material.COIN));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Additional unit-level tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void purchaseService_insufficientFunds_returnsInsufficientFunds() {
        inventory.addItem(Material.COIN, 1); // only 1 coin, manicure costs 2

        NailSalonSystem.ServiceResult result = salon.purchaseService(
                NailSalonSystem.ServiceType.MANICURE,
                inventory,
                11.0f, NailSalonSystem.MONDAY,
                0, notorietySystem, achievementCb);

        assertEquals(NailSalonSystem.ServiceResult.INSUFFICIENT_FUNDS, result);
        assertEquals(1, inventory.getItemCount(Material.COIN));
    }

    @Test
    void purchaseService_banned_returnsRefusedBanned() {
        salon.setBannedForTesting(true, 2.0f);
        inventory.addItem(Material.COIN, 10);

        NailSalonSystem.ServiceResult result = salon.purchaseService(
                NailSalonSystem.ServiceType.MANICURE,
                inventory,
                11.0f, NailSalonSystem.MONDAY,
                0, notorietySystem, achievementCb);

        assertEquals(NailSalonSystem.ServiceResult.REFUSED_BANNED, result);
    }

    @Test
    void purchaseService_tooManyWantedStars_returnsRefusedWanted() {
        inventory.addItem(Material.COIN, 10);

        NailSalonSystem.ServiceResult result = salon.purchaseService(
                NailSalonSystem.ServiceType.MANICURE,
                inventory,
                11.0f, NailSalonSystem.MONDAY,
                NailSalonSystem.REFUSE_WANTED_STARS,
                notorietySystem, achievementCb);

        assertEquals(NailSalonSystem.ServiceResult.REFUSED_WANTED, result);
    }

    @Test
    void purchaseAcrylics_setsAcrylicsDoneFlag_andAwardsFullSetAchievement() {
        inventory.addItem(Material.COIN, 10);

        NailSalonSystem.ServiceResult result = salon.purchaseService(
                NailSalonSystem.ServiceType.ACRYLICS,
                inventory,
                11.0f, NailSalonSystem.MONDAY,
                0, notorietySystem, achievementCb);

        assertEquals(NailSalonSystem.ServiceResult.SUCCESS, result);
        assertTrue(salon.isAcrylicsDone());
        assertTrue(awarded.contains(AchievementType.FULL_SET));
    }

    @Test
    void purchaseGel_activatesGelBuff() {
        inventory.addItem(Material.COIN, 10);

        NailSalonSystem.ServiceResult result = salon.purchaseService(
                NailSalonSystem.ServiceType.GEL,
                inventory,
                11.0f, NailSalonSystem.MONDAY,
                0, notorietySystem, achievementCb);

        assertEquals(NailSalonSystem.ServiceResult.SUCCESS, result);
        assertTrue(salon.isGelBuffActive());
        // Buff duration should be 12 hours × 60 min = 720 in-game minutes
        assertEquals(NailSalonSystem.GEL_BUFF_HOURS * 60f, salon.getGelBuffRemaining(), 0.01f);
    }

    @Test
    void salonRegularAchievement_awardedAfterFiveVisits() {
        inventory.addItem(Material.COIN, 50);
        salon.setTotalVisitsForTesting(NailSalonSystem.SALON_REGULAR_VISIT_COUNT - 1);

        salon.purchaseService(
                NailSalonSystem.ServiceType.PEDICURE,
                inventory,
                11.0f, NailSalonSystem.MONDAY,
                0, notorietySystem, achievementCb);

        assertTrue(awarded.contains(AchievementType.SALON_REGULAR));
    }

    @Test
    void stealEnvelope_addsCoinsAndRecordsCrime() {
        salon.setEnvelopePendingForTesting(true);
        int notorietyBefore = notorietySystem.getNotoriety();

        boolean stolen = salon.stealEnvelope(inventory, notorietySystem, criminalRecord, achievementCb);

        assertTrue(stolen);
        assertEquals(NailSalonSystem.ENVELOPE_COIN, inventory.getItemCount(Material.COIN));
        assertTrue(notorietySystem.getNotoriety() > notorietyBefore);
        assertTrue(criminalRecord.getCount(CriminalRecord.CrimeType.THEFT) > 0);
        assertTrue(salon.isBannedFromSalon());
    }

    @Test
    void stealEnvelope_whenNonePresent_returnsFalse() {
        boolean stolen = salon.stealEnvelope(inventory, notorietySystem, criminalRecord, achievementCb);
        assertFalse(stolen);
    }

    @Test
    void update_decreasesGelBuff() {
        salon.setGelBuffForTesting(60f); // 60 in-game minutes
        // 1 real second at timeSpeed 1.0 = 1 in-game minute
        TimeSystem ts = new TimeSystem(1.0f);
        salon.update(1.0f, ts);
        // Should have reduced by ~1 in-game minute
        assertTrue(salon.getGelBuffRemaining() < 60f);
    }
}

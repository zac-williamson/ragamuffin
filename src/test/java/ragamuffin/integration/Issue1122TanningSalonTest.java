package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.Faction;
import ragamuffin.core.FactionSystem;
import ragamuffin.core.NotorietySystem;
import ragamuffin.core.Rumour;
import ragamuffin.core.RumourNetwork;
import ragamuffin.core.RumourType;
import ragamuffin.core.TanningSalonSystem;
import ragamuffin.core.TimeSystem;
import ragamuffin.core.Weather;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1122: Sun Kissed Studio — Tanning Salon &amp; Marchetti Front.
 */
class Issue1122TanningSalonTest {

    private TanningSalonSystem salon;
    private Player player;
    private Inventory inventory;
    private FactionSystem factionSystem;
    private NotorietySystem notorietySystem;
    private TimeSystem timeSystem;
    private List<AchievementType> awarded;
    private NotorietySystem.AchievementCallback achievementCb;

    @BeforeEach
    void setUp() {
        salon = new TanningSalonSystem(new Random(42L));
        player = new Player(0, 1, 0);
        inventory = new Inventory();
        factionSystem = new FactionSystem();
        notorietySystem = new NotorietySystem();
        timeSystem = new TimeSystem(10.0f);
        awarded = new ArrayList<>();
        achievementCb = type -> awarded.add(type);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Opening hours
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Salon is open Mon–Sat at 10:00.
     */
    @Test
    void isOpen_weekday_midmorning_returnsTrue() {
        // dayOfWeek 0 = Monday
        assertTrue(salon.isOpen(10.0f, 0));
    }

    /**
     * Salon is closed before 09:00 on a weekday.
     */
    @Test
    void isOpen_weekday_beforeOpen_returnsFalse() {
        assertFalse(salon.isOpen(8.0f, 1));
    }

    /**
     * Salon is closed after 21:00 on a weekday.
     */
    @Test
    void isOpen_weekday_afterClose_returnsFalse() {
        assertFalse(salon.isOpen(21.5f, 2));
    }

    /**
     * Salon opens at 11:00 on Sunday.
     */
    @Test
    void isOpen_sunday_afterElevenAM_returnsTrue() {
        // dayOfWeek 6 = Sunday
        assertTrue(salon.isOpen(13.0f, 6));
    }

    /**
     * Salon is closed before 11:00 on Sunday.
     */
    @Test
    void isOpen_sunday_beforeElevenAM_returnsFalse() {
        assertFalse(salon.isOpen(10.0f, 6));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sunbed pricing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sunbed prices are normal on an overcast day.
     */
    @Test
    void getPrice_overcast_basePrice() {
        assertEquals(TanningSalonSystem.PRICE_SUNBED_6MIN,
                salon.getPrice(TanningSalonSystem.ServiceType.SUNBED_6MIN, Weather.OVERCAST));
        assertEquals(TanningSalonSystem.PRICE_SUNBED_12MIN,
                salon.getPrice(TanningSalonSystem.ServiceType.SUNBED_12MIN, Weather.OVERCAST));
    }

    /**
     * Sunny-day discount reduces sunbed prices by 1 COIN.
     */
    @Test
    void getPrice_clearWeather_sunnyDiscount() {
        assertEquals(TanningSalonSystem.PRICE_SUNBED_6MIN - 1,
                salon.getPrice(TanningSalonSystem.ServiceType.SUNBED_6MIN, Weather.CLEAR));
        assertEquals(TanningSalonSystem.PRICE_SUNBED_12MIN - 1,
                salon.getPrice(TanningSalonSystem.ServiceType.SUNBED_12MIN, Weather.CLEAR));
    }

    /**
     * Sunny-day discount also applies during HEATWAVE.
     */
    @Test
    void getPrice_heatwave_sunnyDiscount() {
        assertEquals(TanningSalonSystem.PRICE_SUNBED_6MIN - 1,
                salon.getPrice(TanningSalonSystem.ServiceType.SUNBED_6MIN, Weather.HEATWAVE));
    }

    /**
     * Massage prices are not affected by weather.
     */
    @Test
    void getPrice_massage_notAffectedByWeather() {
        assertEquals(TanningSalonSystem.PRICE_SWEDISH_MASSAGE,
                salon.getPrice(TanningSalonSystem.ServiceType.SWEDISH_MASSAGE, Weather.CLEAR));
        assertEquals(TanningSalonSystem.PRICE_HOT_STONE_MASSAGE,
                salon.getPrice(TanningSalonSystem.ServiceType.HOT_STONE_MASSAGE, Weather.CLEAR));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sunbed service — TANNED buff
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Purchasing a 6-min sunbed applies the TANNED buff and deducts COIN.
     */
    @Test
    void purchaseService_sunbed6min_appliesTannedBuff() {
        inventory.addItem(Material.COIN, 10);

        TanningSalonSystem.ServiceResult result = salon.purchaseService(
                TanningSalonSystem.ServiceType.SUNBED_6MIN,
                player, inventory,
                10.0f, 1, Weather.OVERCAST,
                notorietySystem, achievementCb,
                null, null,
                false, 0);

        assertEquals(TanningSalonSystem.ServiceResult.SUCCESS, result);
        assertTrue(salon.isTanned(), "TANNED buff should be active");
        assertFalse(salon.isDeeplyTanned(), "DEEPLY_TANNED should not be active");
        assertEquals(TanningSalonSystem.TANNED_NOTORIETY_REDUCTION, salon.getNotorietyDisplayReduction());
    }

    /**
     * Purchasing a 12-min sunbed applies the DEEPLY_TANNED buff.
     */
    @Test
    void purchaseService_sunbed12min_appliesDeeplyTannedBuff() {
        inventory.addItem(Material.COIN, 10);

        TanningSalonSystem.ServiceResult result = salon.purchaseService(
                TanningSalonSystem.ServiceType.SUNBED_12MIN,
                player, inventory,
                10.0f, 1, Weather.OVERCAST,
                notorietySystem, achievementCb,
                null, null,
                false, 0);

        assertEquals(TanningSalonSystem.ServiceResult.SUCCESS, result);
        assertFalse(salon.isTanned(), "TANNED buff should not be active after 12-min upgrade");
        assertTrue(salon.isDeeplyTanned(), "DEEPLY_TANNED buff should be active");
        assertEquals(TanningSalonSystem.DEEPLY_TANNED_NOTORIETY_REDUCTION, salon.getNotorietyDisplayReduction());
    }

    /**
     * BRONZED achievement is awarded on first sunbed use.
     */
    @Test
    void purchaseService_sunbed_awardsBronzedAchievement() {
        inventory.addItem(Material.COIN, 10);
        salon.purchaseService(TanningSalonSystem.ServiceType.SUNBED_6MIN,
                player, inventory, 10.0f, 1, Weather.OVERCAST,
                notorietySystem, achievementCb, null, null, false, 0);

        assertTrue(awarded.contains(AchievementType.BRONZED));
    }

    /**
     * SUN_KISSED achievement is awarded after 5 sunbed visits.
     */
    @Test
    void purchaseService_sunbed_fiveVisits_awardsSunKissedAchievement() {
        salon.setTotalSunbedVisitsForTesting(TanningSalonSystem.SUN_KISSED_VISIT_COUNT - 1);
        inventory.addItem(Material.COIN, 10);
        salon.purchaseService(TanningSalonSystem.ServiceType.SUNBED_6MIN,
                player, inventory, 10.0f, 1, Weather.OVERCAST,
                notorietySystem, achievementCb, null, null, false, 0);

        assertTrue(awarded.contains(AchievementType.SUN_KISSED));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Massage services
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Swedish massage restores player health.
     */
    @Test
    void purchaseService_swedishMassage_restoresHealth() {
        player.damage(50f, ragamuffin.entity.DamageReason.PUNCH);
        float healthBefore = player.getHealth();
        inventory.addItem(Material.COIN, 10);

        TanningSalonSystem.ServiceResult result = salon.purchaseService(
                TanningSalonSystem.ServiceType.SWEDISH_MASSAGE,
                player, inventory, 10.0f, 1, Weather.OVERCAST,
                notorietySystem, achievementCb, null, null, false, 0);

        assertEquals(TanningSalonSystem.ServiceResult.SUCCESS, result);
        assertEquals(healthBefore + TanningSalonSystem.SWEDISH_MASSAGE_HEALTH, player.getHealth(), 0.01f);
    }

    /**
     * Hot stone massage restores more health and applies RELAXED buff.
     */
    @Test
    void purchaseService_hotStoneMassage_restoresHealthAndRelaxedBuff() {
        player.damage(80f, ragamuffin.entity.DamageReason.PUNCH);
        float healthBefore = player.getHealth();
        inventory.addItem(Material.COIN, 10);

        TanningSalonSystem.ServiceResult result = salon.purchaseService(
                TanningSalonSystem.ServiceType.HOT_STONE_MASSAGE,
                player, inventory, 10.0f, 1, Weather.OVERCAST,
                notorietySystem, achievementCb, null, null, false, 0);

        assertEquals(TanningSalonSystem.ServiceResult.SUCCESS, result);
        assertEquals(healthBefore + TanningSalonSystem.HOT_STONE_MASSAGE_HEALTH, player.getHealth(), 0.01f);
        assertTrue(salon.isRelaxed(), "RELAXED buff should be active after hot stone massage");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Special Services
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Special Services require Street Rep ≥ 40; refused if below threshold.
     */
    @Test
    void purchaseService_specialServices_insufficientRep_returnsInsufficientRep() {
        inventory.addItem(Material.COIN, 20);

        TanningSalonSystem.ServiceResult result = salon.purchaseService(
                TanningSalonSystem.ServiceType.SPECIAL_SERVICES,
                player, inventory, 10.0f, 1, Weather.OVERCAST,
                notorietySystem, achievementCb, null, null, false,
                TanningSalonSystem.SPECIAL_SERVICES_REP_THRESHOLD - 1);

        assertEquals(TanningSalonSystem.ServiceResult.INSUFFICIENT_REP, result);
        // COIN should not have been deducted
        assertEquals(20, inventory.getItemCount(Material.COIN));
    }

    /**
     * Special Services removes Notoriety and suppresses WitnessSystem at sufficient rep.
     */
    @Test
    void purchaseService_specialServices_removesNotorietyAndSuppressesWitness() {
        notorietySystem.addNotoriety(25, achievementCb);
        int notorietyBefore = notorietySystem.getNotoriety();
        inventory.addItem(Material.COIN, 20);

        TanningSalonSystem.ServiceResult result = salon.purchaseService(
                TanningSalonSystem.ServiceType.SPECIAL_SERVICES,
                player, inventory, 10.0f, 1, Weather.OVERCAST,
                notorietySystem, achievementCb, null, null,
                false, TanningSalonSystem.SPECIAL_SERVICES_REP_THRESHOLD);

        assertEquals(TanningSalonSystem.ServiceResult.SUCCESS, result);
        assertEquals(notorietyBefore - TanningSalonSystem.SPECIAL_SERVICES_NOTORIETY_REMOVAL,
                notorietySystem.getNotoriety());
        assertTrue(salon.isWitnessSuppressed(), "WitnessSystem should be suppressed");
        assertEquals(TanningSalonSystem.SPECIAL_SERVICES_WITNESS_SUPPRESS_MINUTES,
                salon.getWitnessSuppressionRemaining(), 0.01f);
        assertTrue(awarded.contains(AchievementType.SPECIAL_APPOINTMENT));
    }

    /**
     * Grass player is refused service.
     */
    @Test
    void purchaseService_grassPlayer_refused() {
        inventory.addItem(Material.COIN, 20);
        TanningSalonSystem.ServiceResult result = salon.purchaseService(
                TanningSalonSystem.ServiceType.SUNBED_6MIN,
                player, inventory, 10.0f, 1, Weather.OVERCAST,
                notorietySystem, achievementCb, null, null,
                true /* isGrass */, 0);

        assertEquals(TanningSalonSystem.ServiceResult.REFUSED_GRASS, result);
    }

    /**
     * Service is refused when the salon is closed.
     */
    @Test
    void purchaseService_shopClosed_returnsShopClosed() {
        inventory.addItem(Material.COIN, 20);
        // 08:00 on a weekday — before opening
        TanningSalonSystem.ServiceResult result = salon.purchaseService(
                TanningSalonSystem.ServiceType.SUNBED_6MIN,
                player, inventory, 8.0f, 1, Weather.OVERCAST,
                notorietySystem, achievementCb, null, null, false, 0);

        assertEquals(TanningSalonSystem.ServiceResult.SHOP_CLOSED, result);
    }

    /**
     * Insufficient COIN returns the appropriate result without applying any effect.
     */
    @Test
    void purchaseService_insufficientFunds_returnsInsufficientFunds() {
        // Only 1 COIN but sunbed costs 2
        inventory.addItem(Material.COIN, 1);
        TanningSalonSystem.ServiceResult result = salon.purchaseService(
                TanningSalonSystem.ServiceType.SUNBED_6MIN,
                player, inventory, 10.0f, 1, Weather.OVERCAST,
                notorietySystem, achievementCb, null, null, false, 0);

        assertEquals(TanningSalonSystem.ServiceResult.INSUFFICIENT_FUNDS, result);
        assertFalse(salon.isTanned());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Marchetti cash-drop mechanics
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * onHourTick at 11:00 sets dropPending = true.
     */
    @Test
    void onHourTick_11h_setDropPending() {
        assertFalse(salon.isDropPending());
        salon.onHourTick(11);
        assertTrue(salon.isDropPending());
    }

    /**
     * onHourTick at 18:00 also sets dropPending = true.
     */
    @Test
    void onHourTick_18h_setDropPending() {
        salon.onHourTick(18);
        assertTrue(salon.isDropPending());
    }

    /**
     * Intercepting a drop gives COIN and penalises Marchetti Respect.
     */
    @Test
    void interceptDrop_givesCoinsAndPenalisesRespect() {
        salon.setDropPendingForTesting(true);
        int startRespect = factionSystem.getRespect(Faction.MARCHETTI_CREW);

        boolean intercepted = salon.interceptDrop(inventory, factionSystem);

        assertTrue(intercepted);
        assertFalse(salon.isDropPending(), "Drop should no longer be pending after intercept");
        int coin = inventory.getItemCount(Material.COIN);
        assertTrue(coin >= TanningSalonSystem.INTERCEPT_COIN_MIN
                && coin <= TanningSalonSystem.INTERCEPT_COIN_MAX,
                "COIN amount should be within intercept range, got: " + coin);
        assertEquals(startRespect - TanningSalonSystem.INTERCEPT_MARCHETTI_RESPECT_PENALTY,
                factionSystem.getRespect(Faction.MARCHETTI_CREW));
    }

    /**
     * Delivering a drop gives COIN, increases Marchetti Respect, and awards CLEAN_MONEY.
     */
    @Test
    void deliverDrop_givesCoinsAndGainsRespect() {
        salon.setDropPendingForTesting(true);
        int startRespect = factionSystem.getRespect(Faction.MARCHETTI_CREW);

        boolean delivered = salon.deliverDrop(inventory, factionSystem, achievementCb);

        assertTrue(delivered);
        assertFalse(salon.isDropPending());
        assertEquals(TanningSalonSystem.DELIVER_COIN_REWARD, inventory.getItemCount(Material.COIN));
        assertEquals(startRespect + TanningSalonSystem.DELIVER_MARCHETTI_RESPECT_GAIN,
                factionSystem.getRespect(Faction.MARCHETTI_CREW));
        assertTrue(awarded.contains(AchievementType.CLEAN_MONEY));
    }

    /**
     * Intercepting when no drop is pending returns false.
     */
    @Test
    void interceptDrop_noPending_returnsFalse() {
        assertFalse(salon.interceptDrop(inventory, factionSystem));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Marchetti ledger
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Stealing the ledger adds MARCHETTI_LEDGER to inventory and marks it removed.
     */
    @Test
    void stealLedger_addsToInventoryAndMarkRemoved() {
        boolean stolen = salon.stealLedger(inventory);

        assertTrue(stolen);
        assertTrue(salon.isLedgerRemoved());
        assertEquals(1, inventory.getItemCount(Material.MARCHETTI_LEDGER));
    }

    /**
     * Stealing the ledger a second time returns false (already removed).
     */
    @Test
    void stealLedger_alreadyRemoved_returnsFalse() {
        salon.stealLedger(inventory);
        // Remove from inventory to simulate delivery
        inventory.removeItem(Material.MARCHETTI_LEDGER, 1);
        // Try to steal again
        Inventory second = new Inventory();
        assertFalse(salon.stealLedger(second));
    }

    /**
     * Delivering ledger to police triggers raid closure, penalises Marchetti Respect,
     * seeds GRASSED_UP rumour, and awards LAUNDERED achievement.
     */
    @Test
    void deliverLedgerToPolice_triggersClosure() {
        inventory.addItem(Material.MARCHETTI_LEDGER, 1);
        int startRespect = factionSystem.getRespect(Faction.MARCHETTI_CREW);

        RumourNetwork rumourNetwork = new RumourNetwork();
        NPC policeNpc = new NPC(NPCType.POLICE, 0, 1, 0);

        boolean delivered = salon.deliverLedgerToPolice(
                inventory, factionSystem, rumourNetwork, policeNpc, achievementCb);

        assertTrue(delivered);
        assertTrue(salon.isRaidClosed(), "Salon should be raid-closed");
        assertFalse(inventory.hasItem(Material.MARCHETTI_LEDGER), "Ledger should be consumed");
        assertEquals(startRespect - TanningSalonSystem.GRASS_MARCHETTI_RESPECT_PENALTY,
                factionSystem.getRespect(Faction.MARCHETTI_CREW));
        assertTrue(awarded.contains(AchievementType.LAUNDERED));
    }

    /**
     * Salon is closed during a raid.
     */
    @Test
    void isOpen_duringRaid_returnsFalse() {
        salon.setRaidClosedForTesting(true, 3600f);
        assertFalse(salon.isOpen(10.0f, 1), "Salon should be closed during raid");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Buff tick-down and timed effects
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * After update(), TANNED buff ticks down over time.
     */
    @Test
    void update_tannedBuff_ticksDown() {
        salon.setTannedBuffForTesting(60f);
        // Advance time: 1 real second at default time speed
        salon.update(1.0f, timeSystem);

        float remaining = salon.getTannedBuffRemaining();
        assertTrue(remaining < 60f, "TANNED buff should have ticked down");
        assertTrue(remaining >= 0f, "TANNED buff should not go negative");
    }

    /**
     * TANNED buff expires correctly: isTanned() returns false after expiry.
     */
    @Test
    void update_tannedBuff_expiresCorrectly() {
        salon.setTannedBuffForTesting(0.001f);
        salon.update(1.0f, timeSystem);
        assertFalse(salon.isTanned(), "TANNED buff should have expired");
        assertEquals(0, salon.getNotorietyDisplayReduction());
    }

    /**
     * WitnessSystem suppression window ticks down.
     */
    @Test
    void update_witnessSuppressionWindowTicksDown() {
        salon.setWitnessSuppressionForTesting(60f);
        salon.update(1.0f, timeSystem);
        assertTrue(salon.getWitnessSuppressionRemaining() < 60f);
    }

    /**
     * Raid closure expires after the set duration.
     */
    @Test
    void update_raidClosure_expiresAfterDuration() {
        salon.setRaidClosedForTesting(true, 1.0f); // 1 real second
        salon.update(2.0f, timeSystem); // advance 2 seconds
        assertFalse(salon.isRaidClosed(), "Raid closure should have expired");
        assertTrue(salon.isOpen(10.0f, 1), "Salon should be open after raid expires");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Weather integration
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * COLD_SNAP provides extra warmth bonus inside the salon.
     */
    @Test
    void getColdSnapWarmthBonus_coldSnap_returnsBonus() {
        assertEquals(TanningSalonSystem.COLD_SNAP_WARMTH_RATE,
                salon.getColdSnapWarmthBonus(Weather.COLD_SNAP), 0.01f);
    }

    /**
     * No warmth bonus on a clear day.
     */
    @Test
    void getColdSnapWarmthBonus_clearWeather_returnsZero() {
        assertEquals(0f, salon.getColdSnapWarmthBonus(Weather.CLEAR), 0.01f);
    }

    /**
     * Jade seeds a LOCAL_EVENT rumour on rainy days.
     */
    @Test
    void purchaseService_rainyDay_jadeSedsGossipRumour() {
        inventory.addItem(Material.COIN, 10);
        NPC jadeNpc = new NPC(NPCType.MASSAGE_THERAPIST, 1, 1, 1);
        RumourNetwork rumourNetwork = new RumourNetwork();

        salon.purchaseService(TanningSalonSystem.ServiceType.SUNBED_6MIN,
                player, inventory, 10.0f, 1, Weather.RAIN,
                notorietySystem, achievementCb, jadeNpc, rumourNetwork, false, 0);

        List<Rumour> rumours = rumourNetwork.getRumoursFor(jadeNpc);
        assertFalse(rumours.isEmpty(), "Jade should seed a rumour on rainy days");
        assertEquals(RumourType.LOCAL_EVENT, rumours.get(0).getType());
    }

    /**
     * No gossip rumour is seeded on a clear day.
     */
    @Test
    void purchaseService_clearDay_noGossipRumour() {
        inventory.addItem(Material.COIN, 10);
        NPC jadeNpc = new NPC(NPCType.MASSAGE_THERAPIST, 1, 1, 1);
        RumourNetwork rumourNetwork = new RumourNetwork();

        salon.purchaseService(TanningSalonSystem.ServiceType.SUNBED_6MIN,
                player, inventory, 10.0f, 1, Weather.CLEAR,
                notorietySystem, achievementCb, jadeNpc, rumourNetwork, false, 0);

        List<Rumour> rumours = rumourNetwork.getRumoursFor(jadeNpc);
        assertTrue(rumours.isEmpty(), "No gossip rumour on clear days");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DisguiseSystem stacking
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * TANNED buff contributes a −20% recognition reduction when active.
     */
    @Test
    void getTannedDisguiseStackReduction_tannedActive_returnsReduction() {
        salon.setTannedBuffForTesting(100f);
        assertEquals(TanningSalonSystem.TANNED_DISGUISE_STACK_REDUCTION,
                salon.getTannedDisguiseStackReduction(), 0.001f);
    }

    /**
     * No stacking reduction when no buff is active.
     */
    @Test
    void getTannedDisguiseStackReduction_noBuffs_returnsZero() {
        assertEquals(0f, salon.getTannedDisguiseStackReduction(), 0.001f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Enum / constant sanity checks
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * SUN_KISSED_STUDIO landmark has the correct display name.
     */
    @Test
    void landmarkType_sunKissedStudio_hasCorrectDisplayName() {
        assertEquals("Sun Kissed Studio", LandmarkType.SUN_KISSED_STUDIO.getDisplayName());
    }

    /**
     * New prop types are present in the PropType enum.
     */
    @Test
    void propTypes_allNewPropsExist() {
        assertNotNull(PropType.TANNING_BED_PROP);
        assertNotNull(PropType.MASSAGE_TABLE_PROP);
        assertNotNull(PropType.RECEPTION_DESK_PROP);
        assertNotNull(PropType.LAUNDRY_BAG_PROP);
    }

    /**
     * New NPC types are present in the NPCType enum.
     */
    @Test
    void npcTypes_salonOwnerAndMassageTherapistExist() {
        assertNotNull(ragamuffin.entity.NPCType.SALON_OWNER);
        assertNotNull(ragamuffin.entity.NPCType.MASSAGE_THERAPIST);
    }

    /**
     * New materials are present in the Material enum.
     */
    @Test
    void materials_marchettiLedgerAndBrownEnvelopeExist() {
        assertNotNull(Material.MARCHETTI_LEDGER);
        assertNotNull(Material.BROWN_ENVELOPE);
    }

    /**
     * New achievements are present in the AchievementType enum.
     */
    @Test
    void achievementTypes_allNewAchievementsExist() {
        assertNotNull(AchievementType.BRONZED);
        assertNotNull(AchievementType.SUN_KISSED);
        assertNotNull(AchievementType.CLEAN_MONEY);
        assertNotNull(AchievementType.LAUNDERED);
        assertNotNull(AchievementType.SPECIAL_APPOINTMENT);
    }
}

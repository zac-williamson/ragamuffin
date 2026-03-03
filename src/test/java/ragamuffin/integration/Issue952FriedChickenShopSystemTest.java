package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord;
import ragamuffin.core.FriedChickenShopSystem;
import ragamuffin.core.NoiseSystem;
import ragamuffin.core.NotorietySystem;
import ragamuffin.core.Rumour;
import ragamuffin.core.RumourNetwork;
import ragamuffin.core.RumourType;
import ragamuffin.core.WantedSystem;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #952 / #1255: Clucky's Fried Chicken —
 * FriedChickenShopSystem, Wing Tax &amp; the Late-Night Lockout Hustle.
 */
class Issue952FriedChickenShopSystemTest {

    private FriedChickenShopSystem cluckys;
    private Inventory inventory;
    private NotorietySystem notorietySystem;
    private RumourNetwork rumourNetwork;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private NoiseSystem noiseSystem;
    private List<AchievementType> awarded;
    private NotorietySystem.AchievementCallback achievementCb;

    @BeforeEach
    void setUp() {
        cluckys = new FriedChickenShopSystem(new Random(42L));
        inventory = new Inventory();
        notorietySystem = new NotorietySystem();
        rumourNetwork = new RumourNetwork();
        wantedSystem = new WantedSystem();
        criminalRecord = new CriminalRecord();
        noiseSystem = new NoiseSystem();
        awarded = new ArrayList<>();
        achievementCb = type -> awarded.add(type);

        cluckys.setNotorietySystem(notorietySystem);
        cluckys.setRumourNetwork(rumourNetwork);
        cluckys.setWantedSystem(wantedSystem);
        cluckys.setCriminalRecord(criminalRecord);
        cluckys.setNoiseSystem(noiseSystem);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Opening hours
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void isOpen_atOpenHour_returnsTrue() {
        assertTrue(cluckys.isOpen(10.0f));
    }

    @Test
    void isOpen_beforeOpenHour_returnsFalse() {
        // 09:00 is before opening
        assertFalse(cluckys.isOpen(9.0f));
    }

    @Test
    void isOpen_atMidnight_returnsTrue() {
        // 00:00 is within 10:00–02:00 window
        assertTrue(cluckys.isOpen(0.0f));
    }

    @Test
    void isOpen_at1am_returnsTrue() {
        assertTrue(cluckys.isOpen(1.0f));
    }

    @Test
    void isOpen_at2am_returnsFalse() {
        // 02:00 is closing time
        assertFalse(cluckys.isOpen(2.0f));
    }

    @Test
    void isOpen_afternoonHour_returnsTrue() {
        assertTrue(cluckys.isOpen(15.0f));
    }

    @Test
    void isOpen_at3am_returnsFalse() {
        // Well past closing time
        assertFalse(cluckys.isOpen(3.0f));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Chips and Gravy availability
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void chipsAndGravy_notAvailableBefore20() {
        assertFalse(cluckys.isChipsAndGravyAvailable(15.0f));
    }

    @Test
    void chipsAndGravy_availableAfter20() {
        assertTrue(cluckys.isChipsAndGravyAvailable(21.0f));
    }

    @Test
    void chipsAndGravy_availableAtMidnight() {
        assertTrue(cluckys.isChipsAndGravyAvailable(0.0f));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Order success
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void order_chickenWings_success() {
        inventory.addItem(Material.COIN, 5);
        FriedChickenShopSystem.OrderResult result =
                cluckys.placeOrder(Material.CHICKEN_WINGS, 2, inventory, 14.0f, false, null, achievementCb);
        assertEquals(FriedChickenShopSystem.OrderResult.SUCCESS, result);
        assertTrue(inventory.hasItem(Material.CHICKEN_WINGS));
        assertEquals(3, inventory.getItemCount(Material.COIN));
    }

    @Test
    void order_chickenBox_success() {
        inventory.addItem(Material.COIN, 4);
        FriedChickenShopSystem.OrderResult result =
                cluckys.placeOrder(Material.CHICKEN_BOX, 4, inventory, 14.0f, false, null, achievementCb);
        assertEquals(FriedChickenShopSystem.OrderResult.SUCCESS, result);
        assertTrue(inventory.hasItem(Material.CHICKEN_BOX));
        assertEquals(0, inventory.getItemCount(Material.COIN));
    }

    @Test
    void order_flatCola_success() {
        inventory.addItem(Material.COIN, 3);
        FriedChickenShopSystem.OrderResult result =
                cluckys.placeOrder(Material.FLAT_COLA, 1, inventory, 14.0f, false, null, achievementCb);
        assertEquals(FriedChickenShopSystem.OrderResult.SUCCESS, result);
        assertTrue(inventory.hasItem(Material.FLAT_COLA));
    }

    @Test
    void order_chipsAndGravy_afterTwenty_success() {
        inventory.addItem(Material.COIN, 3);
        FriedChickenShopSystem.OrderResult result =
                cluckys.placeOrder(Material.CHIPS_AND_GRAVY, 1, inventory, 21.0f, false, null, achievementCb);
        assertEquals(FriedChickenShopSystem.OrderResult.SUCCESS, result);
        assertTrue(inventory.hasItem(Material.CHIPS_AND_GRAVY));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Order failures
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void order_shopClosed_returnsClosed() {
        inventory.addItem(Material.COIN, 5);
        FriedChickenShopSystem.OrderResult result =
                cluckys.placeOrder(Material.CHICKEN_WINGS, 2, inventory, 5.0f, false, null, achievementCb);
        assertEquals(FriedChickenShopSystem.OrderResult.SHOP_CLOSED, result);
        assertFalse(inventory.hasItem(Material.CHICKEN_WINGS));
    }

    @Test
    void order_balaclava_refused() {
        inventory.addItem(Material.COIN, 10);
        FriedChickenShopSystem.OrderResult result =
                cluckys.placeOrder(Material.CHICKEN_WINGS, 2, inventory, 14.0f, true, null, achievementCb);
        assertEquals(FriedChickenShopSystem.OrderResult.BALACLAVA_REFUSED, result);
        assertFalse(inventory.hasItem(Material.CHICKEN_WINGS));
        // Wearing balaclava triggers wanted star
        assertTrue(wantedSystem.getWantedStars() >= 1);
    }

    @Test
    void order_insufficientFunds() {
        inventory.addItem(Material.COIN, 1);
        FriedChickenShopSystem.OrderResult result =
                cluckys.placeOrder(Material.CHICKEN_BOX, 4, inventory, 14.0f, false, null, achievementCb);
        assertEquals(FriedChickenShopSystem.OrderResult.INSUFFICIENT_FUNDS, result);
        assertFalse(inventory.hasItem(Material.CHICKEN_BOX));
    }

    @Test
    void order_chipsAndGravy_beforeTwenty_itemUnavailable() {
        inventory.addItem(Material.COIN, 5);
        FriedChickenShopSystem.OrderResult result =
                cluckys.placeOrder(Material.CHIPS_AND_GRAVY, 1, inventory, 14.0f, false, null, achievementCb);
        assertEquals(FriedChickenShopSystem.OrderResult.ITEM_UNAVAILABLE, result);
        assertFalse(inventory.hasItem(Material.CHIPS_AND_GRAVY));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wing Tax mechanic
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void wingTax_noChicken_notApplicable() {
        // Player holds no chicken
        FriedChickenShopSystem.WingTaxResult result =
                cluckys.resolveWingTax(FriedChickenShopSystem.WingTaxAction.GIVE, inventory, 2, achievementCb, null);
        assertEquals(FriedChickenShopSystem.WingTaxResult.NOT_APPLICABLE, result);
    }

    @Test
    void wingTax_give_removesChickenFromInventory() {
        inventory.addItem(Material.CHICKEN_BOX, 1);
        FriedChickenShopSystem.WingTaxResult result =
                cluckys.resolveWingTax(FriedChickenShopSystem.WingTaxAction.GIVE, inventory, 2, achievementCb, null);
        assertEquals(FriedChickenShopSystem.WingTaxResult.GAVE, result);
        assertFalse(inventory.hasItem(Material.CHICKEN_BOX));
    }

    @Test
    void wingTax_give_preferChickenBoxOverWings() {
        inventory.addItem(Material.CHICKEN_BOX, 1);
        inventory.addItem(Material.CHICKEN_WINGS, 1);
        cluckys.resolveWingTax(FriedChickenShopSystem.WingTaxAction.GIVE, inventory, 2, achievementCb, null);
        // Chicken box should have been taken, wings remain
        assertFalse(inventory.hasItem(Material.CHICKEN_BOX));
        assertTrue(inventory.hasItem(Material.CHICKEN_WINGS));
    }

    @Test
    void wingTax_run_countsAsRefusal() {
        inventory.addItem(Material.CHICKEN_WINGS, 1);
        FriedChickenShopSystem.WingTaxResult result =
                cluckys.resolveWingTax(FriedChickenShopSystem.WingTaxAction.RUN, inventory, 0, achievementCb, null);
        assertEquals(FriedChickenShopSystem.WingTaxResult.RAN, result);
        assertEquals(1, cluckys.getWingTaxRefusalCount());
        // Wings not taken
        assertTrue(inventory.hasItem(Material.CHICKEN_WINGS));
    }

    @Test
    void wingTax_stareDown_withSufficientBrawling_succeeds() {
        inventory.addItem(Material.CHICKEN_WINGS, 1);
        // INTIMIDATE_TIER_REQUIRED = 3
        FriedChickenShopSystem.WingTaxResult result =
                cluckys.resolveWingTax(FriedChickenShopSystem.WingTaxAction.STARE_DOWN, inventory, 3, achievementCb, null);
        assertEquals(FriedChickenShopSystem.WingTaxResult.STARED_DOWN, result);
        assertEquals(1, cluckys.getWingTaxRefusalCount());
        assertTrue(inventory.hasItem(Material.CHICKEN_WINGS));
    }

    @Test
    void wingTax_stareDown_insufficientBrawling_fallsBackToRefuse() {
        inventory.addItem(Material.CHICKEN_WINGS, 1);
        // Tier 1 is below EXPERT (3) — should not stare-down; falls through to refuse
        FriedChickenShopSystem.WingTaxResult result =
                cluckys.resolveWingTax(FriedChickenShopSystem.WingTaxAction.STARE_DOWN, inventory, 1, achievementCb, null);
        // Result is either FIGHT_TRIGGERED or REFUSED_NO_FIGHT (not STARED_DOWN or GAVE)
        assertNotEquals(FriedChickenShopSystem.WingTaxResult.STARED_DOWN, result);
        assertNotEquals(FriedChickenShopSystem.WingTaxResult.GAVE, result);
        assertEquals(1, cluckys.getWingTaxRefusalCount());
    }

    @Test
    void wingTax_refuse_seedsFightNearbyRumourOnFight() {
        // Use seed known to trigger fight (60% chance — we'll use a fixed seed)
        // With seed 42 and a single call, check if either FIGHT_TRIGGERED or REFUSED_NO_FIGHT
        inventory.addItem(Material.CHICKEN_WINGS, 1);
        NPC gangNpc = new NPC(NPCType.YOUTH_GANG, 5f, 1f, 5f);
        FriedChickenShopSystem.WingTaxResult result =
                cluckys.resolveWingTax(FriedChickenShopSystem.WingTaxAction.REFUSE, inventory, 0, achievementCb, gangNpc);
        // If fight triggered, FIGHT_NEARBY rumour should be seeded into gangNpc
        if (result == FriedChickenShopSystem.WingTaxResult.FIGHT_TRIGGERED) {
            List<Rumour> gangRumours = gangNpc.getRumours();
            boolean hasFightRumour = gangRumours.stream()
                    .anyMatch(r -> r.getType() == RumourType.FIGHT_NEARBY);
            assertTrue(hasFightRumour, "FIGHT_NEARBY rumour should be seeded when fight triggered");
        }
        // Count as refusal regardless
        assertEquals(1, cluckys.getWingTaxRefusalCount());
    }

    @Test
    void wingTax_fiveRefusals_awardsWingDefender() {
        inventory.addItem(Material.CHICKEN_WINGS, 10);
        cluckys.setWingTaxRefusalsForTesting(4);
        cluckys.resolveWingTax(FriedChickenShopSystem.WingTaxAction.RUN, inventory, 0, achievementCb, null);
        assertTrue(awarded.contains(AchievementType.WING_DEFENDER),
                "WING_DEFENDER should be awarded after 5 refusals");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Late-Night Lockout Hustle
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void lockoutHustle_activeAt0145() {
        // 01:45 = 1.75 on 0-24 scale
        assertTrue(cluckys.isLockoutHustleActive(1.75f));
    }

    @Test
    void lockoutHustle_notActiveAt13() {
        assertFalse(cluckys.isLockoutHustleActive(13.0f));
    }

    @Test
    void lockoutHustle_noChickenBox_returnsNoBox() {
        FriedChickenShopSystem.LockoutHustleResult result =
                cluckys.resolveLockoutHustle(false, inventory, 0, achievementCb);
        assertEquals(FriedChickenShopSystem.LockoutHustleResult.NO_CHICKEN_BOX, result);
    }

    @Test
    void lockoutHustle_standardPrice_sells() {
        inventory.addItem(Material.CHICKEN_BOX, 1);
        FriedChickenShopSystem.LockoutHustleResult result =
                cluckys.resolveLockoutHustle(false, inventory, 0, achievementCb);
        assertEquals(FriedChickenShopSystem.LockoutHustleResult.SOLD_STANDARD, result);
        assertFalse(inventory.hasItem(Material.CHICKEN_BOX));
        assertEquals(FriedChickenShopSystem.HUSTLE_STANDARD_PRICE,
                inventory.getItemCount(Material.COIN));
    }

    @Test
    void lockoutHustle_markupWithoutNegotiate_getsStandardPrice() {
        inventory.addItem(Material.CHICKEN_BOX, 1);
        // Trading tier 0 < NEGOTIATE_TIER_REQUIRED (2) — can't mark up
        FriedChickenShopSystem.LockoutHustleResult result =
                cluckys.resolveLockoutHustle(true, inventory, 0, achievementCb);
        assertEquals(FriedChickenShopSystem.LockoutHustleResult.SOLD_STANDARD, result);
        assertEquals(FriedChickenShopSystem.HUSTLE_STANDARD_PRICE,
                inventory.getItemCount(Material.COIN));
    }

    @Test
    void lockoutHustle_markupWithNegotiate_getsMarkupPrice() {
        inventory.addItem(Material.CHICKEN_BOX, 1);
        // Trading tier 2 = JOURNEYMAN >= NEGOTIATE_TIER_REQUIRED
        FriedChickenShopSystem.LockoutHustleResult result =
                cluckys.resolveLockoutHustle(true, inventory, 2, achievementCb);
        assertEquals(FriedChickenShopSystem.LockoutHustleResult.SOLD_MARKUP, result);
        assertFalse(inventory.hasItem(Material.CHICKEN_BOX));
        assertEquals(FriedChickenShopSystem.HUSTLE_MARKUP_PRICE,
                inventory.getItemCount(Material.COIN));
    }

    @Test
    void lockoutHustle_threeMarkups_awardsLateNightEntrepreneur() {
        cluckys.setLockoutMarkupCountForTesting(2);
        inventory.addItem(Material.CHICKEN_BOX, 1);
        cluckys.resolveLockoutHustle(true, inventory, 3, achievementCb);
        assertTrue(awarded.contains(AchievementType.LATE_NIGHT_ENTREPRENEUR),
                "LATE_NIGHT_ENTREPRENEUR should be awarded after 3 mark-ups");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Youth Gang fighting
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void youthGangFight_emptyList_doesNotFight() {
        boolean fought = cluckys.checkYouthGangFight(new ArrayList<>(), null);
        assertFalse(fought);
    }

    @Test
    void youthGangFight_singleNpc_doesNotFight() {
        List<NPC> gang = new ArrayList<>();
        gang.add(new NPC(NPCType.YOUTH_GANG, 0f, 1f, 0f));
        // Single NPC can't fight each other
        // Even if RNG fires, size < 2 → no fight
        // (seed 42 may or may not fire — but with size 1 the check should return false)
        boolean fought = cluckys.checkYouthGangFight(gang, null);
        assertFalse(fought);
    }

    @Test
    void youthSpawnActive_after20() {
        assertTrue(cluckys.isYouthSpawnActive(21.0f));
    }

    @Test
    void youthSpawnActive_before20() {
        assertFalse(cluckys.isYouthSpawnActive(15.0f));
    }

    @Test
    void youthSpawnActive_atMidnight() {
        assertTrue(cluckys.isYouthSpawnActive(0.0f));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fryer smash
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void fryerSmash_recordsArsonAndAddsNotoriety() {
        int notorietyBefore = notorietySystem.getNotoriety();
        cluckys.onFryerSmashed(achievementCb);
        assertTrue(notorietySystem.getNotoriety() > notorietyBefore,
                "Notoriety should increase after fryer smash");
        assertEquals(notorietyBefore + FriedChickenShopSystem.FRYER_SMASH_NOTORIETY,
                notorietySystem.getNotoriety());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Till robbery
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void tillRob_shopClosed_returnsClosed() {
        FriedChickenShopSystem.TillRobResult result =
                cluckys.robTill(inventory, 5.0f, achievementCb);
        assertEquals(FriedChickenShopSystem.TillRobResult.SHOP_CLOSED, result);
    }

    @Test
    void tillRob_whileOpen_addsCoinsAndSetsFlag() {
        FriedChickenShopSystem.TillRobResult result =
                cluckys.robTill(inventory, 14.0f, achievementCb);
        assertEquals(FriedChickenShopSystem.TillRobResult.SUCCESS, result);
        int coins = inventory.getItemCount(Material.COIN);
        assertTrue(coins >= FriedChickenShopSystem.TILL_LOOT_MIN,
                "Should receive at least min till loot");
        assertTrue(coins <= FriedChickenShopSystem.TILL_LOOT_MAX,
                "Should receive at most max till loot");
        assertTrue(cluckys.isTillRobbed());
        assertTrue(wantedSystem.getWantedStars() >= 1,
                "Robbing till should add wanted star");
    }

    @Test
    void tillRob_twice_secondAttemptRefused() {
        cluckys.robTill(inventory, 14.0f, achievementCb);
        Inventory inventory2 = new Inventory();
        FriedChickenShopSystem.TillRobResult result2 =
                cluckys.robTill(inventory2, 14.0f, achievementCb);
        assertEquals(FriedChickenShopSystem.TillRobResult.ALREADY_ROBBED, result2);
        assertEquals(0, inventory2.getItemCount(Material.COIN));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Litter spawning
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void litter_spawnedAfterInterval() {
        // Use fast in-game time so litter spawns quickly in real seconds
        FriedChickenShopSystem fastCluckys = new FriedChickenShopSystem(new Random(1L), 60.0f);
        // Each in-game minute = 1 real second; 15 in-game minutes = 15 real seconds
        // Update with 15.1 seconds → should spawn 1 litter
        boolean spawned = false;
        for (int i = 0; i < 16; i++) {
            if (fastCluckys.updateLitter(1.0f)) spawned = true;
        }
        assertTrue(spawned, "At least one litter item should spawn after interval");
        assertTrue(fastCluckys.getLitterCount() >= 1,
            "At least one litter item should exist after interval");
    }

    @Test
    void litter_capNotExceeded() {
        FriedChickenShopSystem fastCluckys = new FriedChickenShopSystem(new Random(1L), 60.0f);
        // Run many updates to try to exceed cap
        for (int i = 0; i < 200; i++) {
            fastCluckys.updateLitter(1.0f);
        }
        assertTrue(fastCluckys.getLitterCount() <= FriedChickenShopSystem.LITTER_CAP,
                "Litter count should not exceed cap of " + FriedChickenShopSystem.LITTER_CAP);
    }

    @Test
    void litter_pickupDecreasesCount() {
        cluckys.setLitterCountForTesting(3);
        cluckys.onLitterPickedUp();
        assertEquals(2, cluckys.getLitterCount());
    }

    @Test
    void litter_pickupDoesNotGoNegative() {
        cluckys.setLitterCountForTesting(0);
        cluckys.onLitterPickedUp();
        assertEquals(0, cluckys.getLitterCount());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PropType and AchievementType enum sanity checks
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void propType_allFriedChickenPropsExist() {
        // Verify all Issue #952 prop types exist in the enum
        assertDoesNotThrow(() -> PropType.valueOf("FRYER_PROP"));
        assertDoesNotThrow(() -> PropType.valueOf("PLASTIC_TABLE_PROP"));
        assertDoesNotThrow(() -> PropType.valueOf("PLASTIC_CHAIR_PROP"));
        assertDoesNotThrow(() -> PropType.valueOf("SECURITY_GRILLE_PROP"));
        assertDoesNotThrow(() -> PropType.valueOf("CLUCKYS_SIGN_PROP"));
    }

    @Test
    void achievementType_wingDefenderExists() {
        assertDoesNotThrow(() -> AchievementType.valueOf("WING_DEFENDER"));
        AchievementType wd = AchievementType.WING_DEFENDER;
        assertEquals(5, wd.getProgressTarget(),
                "WING_DEFENDER should require 5 refusals");
    }

    @Test
    void achievementType_lateNightEntrepreneurExists() {
        assertDoesNotThrow(() -> AchievementType.valueOf("LATE_NIGHT_ENTREPRENEUR"));
        AchievementType lne = AchievementType.LATE_NIGHT_ENTREPRENEUR;
        assertEquals(3, lne.getProgressTarget(),
                "LATE_NIGHT_ENTREPRENEUR should require 3 mark-ups");
    }

    @Test
    void propType_fryerProp_hasCorrectHitsToBreak() {
        assertEquals(6, PropType.FRYER_PROP.getHitsToBreak());
    }

    @Test
    void propType_securityGrilleProp_hasZeroHitsToBreak() {
        // Grille cannot be broken by player
        assertEquals(0, PropType.SECURITY_GRILLE_PROP.getHitsToBreak());
    }

    @Test
    void propType_fryerProp_dropsMetal() {
        assertEquals(Material.SCRAP_METAL, PropType.FRYER_PROP.getMaterialDrop());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Material enum sanity checks (Issue #952 stubs must exist)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void material_chickenWingsExists() {
        assertDoesNotThrow(() -> Material.valueOf("CHICKEN_WINGS"));
    }

    @Test
    void material_chickenBoxExists() {
        assertDoesNotThrow(() -> Material.valueOf("CHICKEN_BOX"));
    }

    @Test
    void material_chipsAndGravyExists() {
        assertDoesNotThrow(() -> Material.valueOf("CHIPS_AND_GRAVY"));
    }

    @Test
    void material_flatColaExists() {
        assertDoesNotThrow(() -> Material.valueOf("FLAT_COLA"));
    }

    @Test
    void rumourType_fightNearbyExists() {
        assertDoesNotThrow(() -> RumourType.valueOf("FIGHT_NEARBY"));
    }

    @Test
    void npcType_devrajExists() {
        assertDoesNotThrow(() -> NPCType.valueOf("DEVRAJ"));
    }

    @Test
    void npcType_youthGangExists() {
        assertDoesNotThrow(() -> NPCType.valueOf("YOUTH_GANG"));
    }
}

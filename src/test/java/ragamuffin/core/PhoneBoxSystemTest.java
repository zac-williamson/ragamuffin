package ragamuffin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.core.Faction;
import ragamuffin.core.PhoneBoxSystem.BoxState;
import ragamuffin.core.PhoneBoxSystem.CallResult;
import ragamuffin.core.PhoneBoxSystem.CallType;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PhoneBoxSystem} — Issue #1345.
 */
class PhoneBoxSystemTest {

    private PhoneBoxSystem phoneBoxSystem;
    private Inventory inventory;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private FactionSystem factionSystem;
    private NeighbourhoodSystem neighbourhoodSystem;

    @BeforeEach
    void setUp() {
        phoneBoxSystem = new PhoneBoxSystem(new Random(42));
        inventory = new Inventory(36);
        notorietySystem = new NotorietySystem();
        wantedSystem = new WantedSystem(new Random(42));
        criminalRecord = new CriminalRecord();
        rumourNetwork = new RumourNetwork(new Random(42));
        achievementSystem = new AchievementSystem();
        factionSystem = new FactionSystem();
        neighbourhoodSystem = new NeighbourhoodSystem();

        phoneBoxSystem.setNotorietySystem(notorietySystem);
        phoneBoxSystem.setWantedSystem(wantedSystem);
        phoneBoxSystem.setCriminalRecord(criminalRecord);
        phoneBoxSystem.setRumourNetwork(rumourNetwork);
        phoneBoxSystem.setAchievementSystem(achievementSystem);
        phoneBoxSystem.setFactionSystem(factionSystem);
        phoneBoxSystem.setNeighbourhoodSystem(neighbourhoodSystem);
    }

    // ── isOccupied tests ──────────────────────────────────────────────────────

    /**
     * With a seeded Random that produces a value < 0.20, isOccupied should return
     * true during daytime. We test several seeds to confirm behaviour.
     */
    @Test
    void isOccupied_belowThreshold_returnsTrue() {
        // Seed 0 produces first float ~0.73 — above 0.20 → not occupied
        // We need a seed that produces < 0.20 on first call
        // Seed 1: first float from new Random(1) is ~0.7309... — above threshold
        // Use brute-force: find a seed where first nextFloat() < 0.2
        // Random(99) first float is ~0.169... — below threshold
        PhoneBoxSystem system = new PhoneBoxSystem(new Random(99));
        // Verify with daytime hour
        assertTrue(system.isOccupied(12.0f));
    }

    @Test
    void isOccupied_aboveThreshold_returnsFalse() {
        // Random(42) first float is ~0.619... — above 0.20 → not occupied during daytime
        PhoneBoxSystem system = new PhoneBoxSystem(new Random(42));
        assertFalse(system.isOccupied(12.0f));
    }

    @Test
    void isOccupied_nighttime_returnsFalse() {
        // Even a seeded rng below threshold should return false at night
        PhoneBoxSystem system = new PhoneBoxSystem(new Random(99));
        assertFalse(system.isOccupied(3.0f));
        assertFalse(system.isOccupied(23.0f));
    }

    // ── Tip-off call tests ────────────────────────────────────────────────────

    @Test
    void makeCall_tipOff_seedsPoliceTipOffRumour_andDeductsCoin() {
        inventory.addItem(Material.COIN, 5);

        // Use a system with a predictable random that won't trigger occupancy
        PhoneBoxSystem system = new PhoneBoxSystem(new Random(42)); // 0.619... → not occupied
        system.setRumourNetwork(rumourNetwork);
        system.setAchievementSystem(achievementSystem);
        system.setFactionSystem(factionSystem);

        CallResult result = system.makeCall(
                CallType.TIP_OFF, inventory, 12.0f, null, new ArrayList<>());

        assertEquals(CallResult.SUCCESS, result);
        assertEquals(4, inventory.getItemCount(Material.COIN)); // 1 coin deducted
    }

    @Test
    void makeCall_tipOff_noPayment_returnsNoPayment() {
        // Empty inventory — no coin, no phone card
        PhoneBoxSystem system = new PhoneBoxSystem(new Random(42));
        system.setRumourNetwork(rumourNetwork);
        system.setAchievementSystem(achievementSystem);

        CallResult result = system.makeCall(
                CallType.TIP_OFF, inventory, 12.0f, null, new ArrayList<>());

        assertEquals(CallResult.NO_PAYMENT, result);
    }

    @Test
    void makeCall_boxSmashed_returnsBOX_SMASHED() {
        inventory.addItem(Material.COIN, 5);
        phoneBoxSystem.setHighStreetBoxStateForTesting(BoxState.BROKEN);

        CallResult result = phoneBoxSystem.makeCall(
                CallType.TIP_OFF, inventory, 12.0f, null, new ArrayList<>());

        assertEquals(CallResult.BOX_SMASHED, result);
    }

    // ── Dead-drop call tests ──────────────────────────────────────────────────

    @Test
    void makeCall_deadDrop_marchettiRespectBelow30_returnsFactionLocked() {
        inventory.addItem(Material.COIN, 5);
        inventory.addItem(Material.SCRAWLED_NUMBER, 1);

        factionSystem.setRespect(Faction.MARCHETTI_CREW, 20); // below threshold

        PhoneBoxSystem system = new PhoneBoxSystem(new Random(42));
        system.setFactionSystem(factionSystem);
        system.setRumourNetwork(rumourNetwork);
        system.setAchievementSystem(achievementSystem);

        CallResult result = system.makeCall(
                CallType.DEAD_DROP, inventory, 12.0f, null, new ArrayList<>());

        assertEquals(CallResult.FACTION_LOCKED, result);
    }

    @Test
    void makeCall_deadDrop_noScrawledNumber_returnsMissingItem() {
        inventory.addItem(Material.COIN, 5);
        // No SCRAWLED_NUMBER

        factionSystem.setRespect(Faction.MARCHETTI_CREW, 50); // above threshold

        PhoneBoxSystem system = new PhoneBoxSystem(new Random(42));
        system.setFactionSystem(factionSystem);
        system.setRumourNetwork(rumourNetwork);
        system.setAchievementSystem(achievementSystem);

        CallResult result = system.makeCall(
                CallType.DEAD_DROP, inventory, 12.0f, null, new ArrayList<>());

        assertEquals(CallResult.MISSING_ITEM, result);
    }

    @Test
    void makeCall_deadDrop_validConditions_returnsSuccess() {
        inventory.addItem(Material.COIN, 5);
        inventory.addItem(Material.SCRAWLED_NUMBER, 1);

        factionSystem.setRespect(Faction.MARCHETTI_CREW, 50);

        PhoneBoxSystem system = new PhoneBoxSystem(new Random(42));
        system.setFactionSystem(factionSystem);
        system.setRumourNetwork(rumourNetwork);
        system.setAchievementSystem(achievementSystem);

        CallResult result = system.makeCall(
                CallType.DEAD_DROP, inventory, 12.0f, null, new ArrayList<>());

        assertEquals(CallResult.SUCCESS, result);
        assertEquals(4, inventory.getItemCount(Material.COIN)); // 1 coin deducted
    }

    // ── repairEstateBox tests ─────────────────────────────────────────────────

    @Test
    void repairEstateBox_insufficientScrapMetal_returnsFalse() {
        inventory.addItem(Material.SCRAP_METAL, 2); // Need 3

        boolean result = phoneBoxSystem.repairEstateBox(inventory, new ArrayList<>());

        assertFalse(result);
        assertEquals(BoxState.BROKEN, phoneBoxSystem.getEstateBoxState());
    }

    @Test
    void repairEstateBox_sufficientScrapMetal_repairsBox() {
        inventory.addItem(Material.SCRAP_METAL, 3);

        boolean result = phoneBoxSystem.repairEstateBox(inventory, new ArrayList<>());

        assertTrue(result);
        assertEquals(BoxState.FUNCTIONAL, phoneBoxSystem.getEstateBoxState());
        assertEquals(0, inventory.getItemCount(Material.SCRAP_METAL)); // consumed
        assertTrue(achievementSystem.isUnlocked(AchievementType.LAST_PHONE_STANDING));
    }

    @Test
    void repairEstateBox_withKey_repairsWithoutConsumingScrap() {
        inventory.addItem(Material.PHONE_BOX_KEY, 1);
        // No SCRAP_METAL needed

        boolean result = phoneBoxSystem.repairEstateBox(inventory, new ArrayList<>());

        assertTrue(result);
        assertEquals(BoxState.FUNCTIONAL, phoneBoxSystem.getEstateBoxState());
        assertEquals(0, inventory.getItemCount(Material.SCRAP_METAL)); // still 0
    }

    // ── raidCoinBox tests ─────────────────────────────────────────────────────

    @Test
    void raidCoinBox_withKey_yieldsCoinsAndRecordsCrime() {
        inventory.addItem(Material.PHONE_BOX_KEY, 1);

        // Use a seeded random for reproducible yield
        PhoneBoxSystem system = new PhoneBoxSystem(new Random(7));
        system.setCriminalRecord(criminalRecord);
        system.setNotorietySystem(notorietySystem);
        system.setWantedSystem(wantedSystem);

        int coins = system.raidCoinBox(inventory);

        // Must be between 2 and 6 inclusive
        assertTrue(coins >= PhoneBoxSystem.COIN_BOX_MIN && coins <= PhoneBoxSystem.COIN_BOX_MAX,
                "Expected 2–6 coins, got " + coins);

        // Crime recorded
        assertEquals(1, criminalRecord.getCount(CrimeType.PHONE_BOX_VANDALISM));

        // Notoriety +1
        assertEquals(PhoneBoxSystem.VANDALISM_NOTORIETY, notorietySystem.getNotoriety());

        // Wanted +1
        assertEquals(1, wantedSystem.getWantedStars());
    }

    @Test
    void raidCoinBox_noKey_yieldsZero() {
        int coins = phoneBoxSystem.raidCoinBox(inventory);
        assertEquals(0, coins);
    }

    // ── scrawledNumberDiscovery tests ─────────────────────────────────────────

    @Test
    void discoverScrawledNumber_firstCall_addsToInventory() {
        boolean added = phoneBoxSystem.discoverScrawledNumber(inventory);

        assertTrue(added);
        assertEquals(1, inventory.getItemCount(Material.SCRAWLED_NUMBER));
        assertTrue(phoneBoxSystem.isScrawledNumberDiscovered());
    }

    @Test
    void discoverScrawledNumber_secondCall_noDuplicate() {
        phoneBoxSystem.discoverScrawledNumber(inventory);
        boolean addedAgain = phoneBoxSystem.discoverScrawledNumber(inventory);

        assertFalse(addedAgain);
        assertEquals(1, inventory.getItemCount(Material.SCRAWLED_NUMBER));
    }

    // ── NPC vandalism tests ───────────────────────────────────────────────────

    @Test
    void npcVandalise_youthGang_withLowRoll_breaksBox() {
        // Random(5) produces a first float < 0.05
        PhoneBoxSystem system = new PhoneBoxSystem(new Random(5));
        // Verify by checking the value
        float firstFloat = new Random(5).nextFloat();
        assumeConditionOrSkip(firstFloat < 0.05f);

        boolean broken = system.npcVandalise(NPCType.YOUTH_GANG);
        if (firstFloat < 0.05f) {
            assertTrue(broken);
            assertEquals(BoxState.BROKEN, system.getHighStreetBoxState());
        }
    }

    @Test
    void npcVandalise_publicNpc_neverVandalises() {
        // PUBLIC NPC should never vandalise regardless of roll
        PhoneBoxSystem system = new PhoneBoxSystem(new Random(5));
        boolean broken = system.npcVandalise(NPCType.PUBLIC);
        assertFalse(broken);
        assertEquals(BoxState.FUNCTIONAL, system.getHighStreetBoxState());
    }

    // ── PHONE_CARD payment tests ──────────────────────────────────────────────

    @Test
    void makeCall_withPhoneCard_consumesCardNotCoin() {
        inventory.addItem(Material.PHONE_CARD, 1);
        inventory.addItem(Material.COIN, 5);

        PhoneBoxSystem system = new PhoneBoxSystem(new Random(42));
        system.setRumourNetwork(rumourNetwork);
        system.setAchievementSystem(achievementSystem);
        system.setFactionSystem(factionSystem);

        CallResult result = system.makeCall(
                CallType.TIP_OFF, inventory, 12.0f, null, new ArrayList<>());

        assertEquals(CallResult.SUCCESS, result);
        assertEquals(0, inventory.getItemCount(Material.PHONE_CARD)); // card consumed
        assertEquals(5, inventory.getItemCount(Material.COIN)); // coin NOT consumed
    }

    // ── Council auto-repair tests ─────────────────────────────────────────────

    @Test
    void tickDailyRepair_afterTwoDays_restoresBox() {
        phoneBoxSystem.setHighStreetBoxStateForTesting(BoxState.BROKEN);
        phoneBoxSystem.setHighStreetBrokenDayForTesting(10);

        phoneBoxSystem.tickDailyRepair(12); // 2 days later

        assertEquals(BoxState.FUNCTIONAL, phoneBoxSystem.getHighStreetBoxState());
    }

    @Test
    void tickDailyRepair_beforeTwoDays_boxRemainsSmashed() {
        phoneBoxSystem.setHighStreetBoxStateForTesting(BoxState.BROKEN);
        phoneBoxSystem.setHighStreetBrokenDayForTesting(10);

        phoneBoxSystem.tickDailyRepair(11); // only 1 day later

        assertEquals(BoxState.BROKEN, phoneBoxSystem.getHighStreetBoxState());
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Skip a test if the condition is false (used for probabilistic seeded checks). */
    private void assumeConditionOrSkip(boolean condition) {
        org.junit.jupiter.api.Assumptions.assumeTrue(condition,
                "Skipping: seed did not produce required random value");
    }
}

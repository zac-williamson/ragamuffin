package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.*;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.core.Faction;
import ragamuffin.core.PhoneBoxSystem.BoxState;
import ragamuffin.core.PhoneBoxSystem.CallResult;
import ragamuffin.core.PhoneBoxSystem.CallType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1345 — Northfield BT Phone Box:
 * The Scrawled Number, Anonymous Tip-Offs &amp; the Marchetti Dead-Drop.
 *
 * <ol>
 *   <li>Anonymous tip-off spawns police patrol</li>
 *   <li>Marchetti dead-drop full flow</li>
 *   <li>Estate box repair restores function</li>
 *   <li>Coin box raid yields coin and criminal record</li>
 *   <li>SCRAWLED_NUMBER discovered on first use</li>
 * </ol>
 */
class Issue1345PhoneBoxIntegrationTest {

    private PhoneBoxSystem phoneBoxSystem;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private FactionSystem factionSystem;
    private NeighbourhoodSystem neighbourhoodSystem;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        // Use seeded Random(42): first nextFloat() ~0.619 → above 0.20 occupancy
        // threshold, so box is never occupied in these tests.
        phoneBoxSystem = new PhoneBoxSystem(new Random(42));

        notorietySystem = new NotorietySystem();
        wantedSystem = new WantedSystem(new Random(42));
        criminalRecord = new CriminalRecord();
        rumourNetwork = new RumourNetwork(new Random(42));
        achievementSystem = new AchievementSystem();
        factionSystem = new FactionSystem();
        neighbourhoodSystem = new NeighbourhoodSystem();
        inventory = new Inventory(36);

        phoneBoxSystem.setNotorietySystem(notorietySystem);
        phoneBoxSystem.setWantedSystem(wantedSystem);
        phoneBoxSystem.setCriminalRecord(criminalRecord);
        phoneBoxSystem.setRumourNetwork(rumourNetwork);
        phoneBoxSystem.setAchievementSystem(achievementSystem);
        phoneBoxSystem.setFactionSystem(factionSystem);
        phoneBoxSystem.setNeighbourhoodSystem(neighbourhoodSystem);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Anonymous tip-off spawns police patrol
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: Call makeCall(TIP_OFF, ...) targeting SCRAPYARD.
     * Verify POLICE_TIP_OFF rumour is seeded in RumourNetwork.
     * Advance time 5 in-game minutes. Verify tip-off police are ready to spawn.
     * Verify player COIN decreased by 1. Verify OFF_THE_BOOKS progress incremented by 1.
     */
    @Test
    void tipOffCall_seedsRumourAndSchedulesPolice() {
        inventory.addItem(Material.COIN, 5);

        // Seed a nearby NPC for rumour propagation
        NPC witness = new NPC(NPCType.PUBLIC, 5f, 0f, 5f);
        List<NPC> npcs = new ArrayList<>();
        npcs.add(witness);

        // Make the tip-off call
        CallResult result = phoneBoxSystem.makeCall(
                CallType.TIP_OFF, inventory, 14.0f, witness, npcs);

        assertEquals(CallResult.SUCCESS, result, "Tip-off call should succeed");

        // Verify coin deducted
        assertEquals(4, inventory.getItemCount(Material.COIN),
                "1 COIN should be deducted for the call");

        // Verify POLICE_TIP_OFF rumour seeded
        List<Rumour> rumours = rumourNetwork.getRumoursFrom(witness);
        assertNotNull(rumours, "NPC should have rumours seeded");
        assertTrue(rumours.stream()
                .anyMatch(r -> r.getType() == RumourType.POLICE_TIP_OFF),
                "POLICE_TIP_OFF rumour should be seeded to a nearby NPC");

        // Verify OFF_THE_BOOKS progress incremented
        assertEquals(1, achievementSystem.getProgress(AchievementType.OFF_THE_BOOKS),
                "OFF_THE_BOOKS progress should be 1 after first call");

        // Advance 5 in-game minutes — police should now be ready
        phoneBoxSystem.update(5.0f);
        assertTrue(phoneBoxSystem.isTipOffPoliceReady(),
                "Police should be ready to spawn after 5 in-game minutes");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Marchetti dead-drop full flow
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: Set Marchetti Respect to 35. Add SCRAWLED_NUMBER to inventory.
     * Call makeCall(DEAD_DROP, ...). Verify SUCCESS. Verify MARCHETTI_CONTACT rumour.
     * Advance 10 in-game minutes. Verify runner is ready to spawn.
     * Simulate collecting the package. Verify DEAD_DROP achievement awarded.
     * Verify inventory contains one of {SCRAP_METAL, COIN, FAKE_ID}.
     * Verify Marchetti Respect increased to 40.
     */
    @Test
    void deadDropCall_fullFlow_succeeds() {
        factionSystem.setRespect(Faction.MARCHETTI_CREW, 35);
        inventory.addItem(Material.COIN, 5);
        inventory.addItem(Material.SCRAWLED_NUMBER, 1);

        NPC witness = new NPC(NPCType.PUBLIC, 5f, 0f, 5f);
        List<NPC> npcs = new ArrayList<>();
        npcs.add(witness);

        // Make dead-drop call
        CallResult result = phoneBoxSystem.makeCall(
                CallType.DEAD_DROP, inventory, 14.0f, witness, npcs);

        assertEquals(CallResult.SUCCESS, result, "Dead-drop call should succeed");

        // Verify COIN deducted
        assertEquals(4, inventory.getItemCount(Material.COIN));

        // Verify MARCHETTI_CONTACT rumour seeded
        List<Rumour> rumours = rumourNetwork.getRumoursFrom(witness);
        assertNotNull(rumours);
        assertTrue(rumours.stream()
                .anyMatch(r -> r.getType() == RumourType.MARCHETTI_CONTACT),
                "MARCHETTI_CONTACT rumour should be seeded");

        // Advance 10 in-game minutes
        phoneBoxSystem.update(10.0f);
        assertTrue(phoneBoxSystem.isDeadDropRunnerReady(),
                "MARCHETTI_RUNNER should be ready to spawn after 10 minutes");

        // Collect the package
        int coinsBefore = inventory.getItemCount(Material.COIN);
        Material loot = phoneBoxSystem.collectDeadDropPackage(inventory);
        assertNotNull(loot, "A loot item should be returned");

        // Verify loot is one of the expected types
        assertTrue(loot == Material.SCRAP_METAL || loot == Material.COIN || loot == Material.FAKE_ID,
                "Loot should be SCRAP_METAL, COIN, or FAKE_ID, got: " + loot);

        // Verify DEAD_DROP achievement
        assertTrue(achievementSystem.isUnlocked(AchievementType.DEAD_DROP),
                "DEAD_DROP achievement should be awarded");

        // Verify Marchetti Respect increased from 35 to 40
        assertEquals(40, factionSystem.getRespect(Faction.MARCHETTI_CREW),
                "Marchetti Respect should increase by 5 after dead-drop collection");
    }

    /**
     * Scenario 2b: Dead-drop call fails if Marchetti Respect < 30.
     */
    @Test
    void deadDropCall_lowRespect_returnsFactionLocked() {
        factionSystem.setRespect(Faction.MARCHETTI_CREW, 25);
        inventory.addItem(Material.COIN, 5);
        inventory.addItem(Material.SCRAWLED_NUMBER, 1);

        CallResult result = phoneBoxSystem.makeCall(
                CallType.DEAD_DROP, inventory, 14.0f, null, new ArrayList<>());

        assertEquals(CallResult.FACTION_LOCKED, result);
        assertEquals(5, inventory.getItemCount(Material.COIN), "No coin deducted on FACTION_LOCKED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Estate box repair restores function
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: Set PHONE_BOX_ESTATE state to BROKEN. Add 3× SCRAP_METAL.
     * Call repairEstateBox. Verify true. Verify 0 SCRAP_METAL in inventory.
     * Verify estate box FUNCTIONAL. Verify PHONE_BOX_REPAIR rumour seeded.
     * Verify LAST_PHONE_STANDING achievement. Verify Vibes +2.
     */
    @Test
    void repairEstateBox_endToEnd_restoresFunction() {
        // Estate box starts broken by default
        assertEquals(BoxState.BROKEN, phoneBoxSystem.getEstateBoxState(),
                "Estate box starts as BROKEN");

        int vibesBefore = neighbourhoodSystem.getVibes();

        inventory.addItem(Material.SCRAP_METAL, 3);

        NPC witness = new NPC(NPCType.PUBLIC, 5f, 0f, 5f);
        List<NPC> npcs = new ArrayList<>();
        npcs.add(witness);

        boolean repaired = phoneBoxSystem.repairEstateBox(inventory, npcs);

        assertTrue(repaired, "Repair should succeed with 3 SCRAP_METAL");
        assertEquals(BoxState.FUNCTIONAL, phoneBoxSystem.getEstateBoxState(),
                "Estate box should now be FUNCTIONAL");
        assertEquals(0, inventory.getItemCount(Material.SCRAP_METAL),
                "3 SCRAP_METAL should be consumed");

        // Verify achievement
        assertTrue(achievementSystem.isUnlocked(AchievementType.LAST_PHONE_STANDING),
                "LAST_PHONE_STANDING should be awarded");

        // Verify PHONE_BOX_REPAIR rumour
        List<Rumour> rumours = rumourNetwork.getRumoursFrom(witness);
        assertNotNull(rumours);
        assertTrue(rumours.stream()
                .anyMatch(r -> r.getType() == RumourType.PHONE_BOX_REPAIR),
                "PHONE_BOX_REPAIR rumour should be seeded");

        // Verify Neighbourhood Vibes +2
        assertEquals(vibesBefore + PhoneBoxSystem.REPAIR_VIBES_BONUS,
                neighbourhoodSystem.getVibes(),
                "Neighbourhood Vibes should increase by 2");
    }

    /**
     * Scenario 3b: Repair fails with fewer than 3 SCRAP_METAL.
     */
    @Test
    void repairEstateBox_insufficientMaterials_returnsFalse() {
        inventory.addItem(Material.SCRAP_METAL, 2);

        boolean repaired = phoneBoxSystem.repairEstateBox(inventory, new ArrayList<>());

        assertFalse(repaired, "Repair should fail with only 2 SCRAP_METAL");
        assertEquals(BoxState.BROKEN, phoneBoxSystem.getEstateBoxState());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Coin box raid yields coin and criminal record
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Give player PHONE_BOX_KEY. Call raidCoinBox(inventory).
     * Verify player gains between 2 and 6 COIN. Verify PHONE_BOX_VANDALISM in CriminalRecord.
     * Verify Notoriety +1. Verify WantedSystem star count +1.
     */
    @Test
    void raidCoinBox_withKey_yieldsCoinsAndRecordsCrime() {
        inventory.addItem(Material.PHONE_BOX_KEY, 1);

        PhoneBoxSystem system = new PhoneBoxSystem(new Random(7));
        system.setCriminalRecord(criminalRecord);
        system.setNotorietySystem(notorietySystem);
        system.setWantedSystem(wantedSystem);

        int notorietyBefore = notorietySystem.getNotoriety();
        int wantedBefore = wantedSystem.getWantedStars();

        int coins = system.raidCoinBox(inventory);

        // Verify coin yield in range
        assertTrue(coins >= PhoneBoxSystem.COIN_BOX_MIN && coins <= PhoneBoxSystem.COIN_BOX_MAX,
                "Expected 2–6 coins from raid, got: " + coins);

        // Verify coins added to inventory
        assertEquals(coins, inventory.getItemCount(Material.COIN));

        // Verify crime recorded
        assertEquals(1, criminalRecord.getCount(CrimeType.PHONE_BOX_VANDALISM),
                "PHONE_BOX_VANDALISM should be recorded");

        // Verify Notoriety +1
        assertEquals(notorietyBefore + PhoneBoxSystem.VANDALISM_NOTORIETY,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by 1");

        // Verify Wanted +1
        assertEquals(wantedBefore + 1, wantedSystem.getWantedStars(),
                "WantedSystem star count should increase by 1");
    }

    /**
     * Scenario 4b: Attempting to use the box after it's smashed returns BOX_SMASHED.
     */
    @Test
    void vandaliseBox_thenUse_returnsBOX_SMASHED() {
        inventory.addItem(Material.COIN, 5);

        // Vandalize the box
        phoneBoxSystem.vandaliseBox(inventory, 0);

        assertEquals(BoxState.BROKEN, phoneBoxSystem.getHighStreetBoxState(),
                "Box should be BROKEN after vandalism");

        // Try to use it
        inventory.addItem(Material.COIN, 5); // replenish
        CallResult result = phoneBoxSystem.makeCall(
                CallType.TIP_OFF, inventory, 14.0f, null, new ArrayList<>());

        assertEquals(CallResult.BOX_SMASHED, result,
                "Should return BOX_SMASHED for a broken box");
    }

    /**
     * Scenario 4c: Vandalism records crime, adds notoriety, and wanted star.
     */
    @Test
    void vandaliseBox_recordsCrimeAndPenalties() {
        int notorietyBefore = notorietySystem.getNotoriety();
        int wantedBefore = wantedSystem.getWantedStars();

        phoneBoxSystem.vandaliseBox(inventory, 5);

        assertEquals(1, inventory.getItemCount(Material.SCRAP_METAL),
                "Vandalism should yield 1 SCRAP_METAL");
        assertEquals(1, criminalRecord.getCount(CrimeType.PHONE_BOX_VANDALISM),
                "PHONE_BOX_VANDALISM crime should be recorded");
        assertEquals(notorietyBefore + PhoneBoxSystem.VANDALISM_NOTORIETY,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by 1");
        assertEquals(wantedBefore + 1, wantedSystem.getWantedStars(),
                "Wanted stars should increase by 1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: SCRAWLED_NUMBER discovered on first use
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5: Player has empty inventory. Press E on PHONE_BOX_HIGH_STREET
     * (box functional, not occupied). Verify SCRAWLED_NUMBER added to inventory.
     * Press E again (open call menu). Verify SCRAWLED_NUMBER count is still 1.
     * Verify Marchetti dead-drop option available if Respect ≥ 30.
     */
    @Test
    void scrawledNumberDiscovery_onFirstUse_addedOnce() {
        // Start with empty inventory (no coin yet — we test discovery separately)
        assertFalse(phoneBoxSystem.isScrawledNumberDiscovered());
        assertEquals(0, inventory.getItemCount(Material.SCRAWLED_NUMBER));

        // First E press — simulate by calling discoverScrawledNumber
        boolean discovered = phoneBoxSystem.discoverScrawledNumber(inventory);

        assertTrue(discovered, "SCRAWLED_NUMBER should be newly discovered on first press");
        assertEquals(1, inventory.getItemCount(Material.SCRAWLED_NUMBER),
                "Inventory should contain 1 SCRAWLED_NUMBER");

        // Second E press — no duplicate
        boolean discoveredAgain = phoneBoxSystem.discoverScrawledNumber(inventory);

        assertFalse(discoveredAgain, "Second press should not add another SCRAWLED_NUMBER");
        assertEquals(1, inventory.getItemCount(Material.SCRAWLED_NUMBER),
                "SCRAWLED_NUMBER count should still be 1");
    }

    /**
     * Scenario 5b: Marchetti dead-drop option is available when Respect ≥ 30
     * and SCRAWLED_NUMBER is in inventory.
     */
    @Test
    void scrawledNumberPresent_marchettiRespectMet_deadDropAvailable() {
        factionSystem.setRespect(Faction.MARCHETTI_CREW, 30);
        inventory.addItem(Material.SCRAWLED_NUMBER, 1);
        inventory.addItem(Material.COIN, 5);

        // Dead-drop call should succeed (not FACTION_LOCKED or MISSING_ITEM)
        CallResult result = phoneBoxSystem.makeCall(
                CallType.DEAD_DROP, inventory, 14.0f, null, new ArrayList<>());

        assertEquals(CallResult.SUCCESS, result,
                "Dead-drop should be available with Respect 30 and SCRAWLED_NUMBER");
    }

    /**
     * Scenario 5c: Auto-discovery of SCRAWLED_NUMBER happens when any call is made.
     */
    @Test
    void makeCall_autoDicoveriesScrawledNumber() {
        inventory.addItem(Material.COIN, 5);
        assertFalse(phoneBoxSystem.isScrawledNumberDiscovered());

        phoneBoxSystem.makeCall(CallType.TIP_OFF, inventory, 14.0f, null, new ArrayList<>());

        assertTrue(phoneBoxSystem.isScrawledNumberDiscovered(),
                "SCRAWLED_NUMBER should be auto-discovered on any call");
        assertEquals(1, inventory.getItemCount(Material.SCRAWLED_NUMBER));
    }
}

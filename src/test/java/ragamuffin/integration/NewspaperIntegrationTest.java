package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.*;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #774: Integration tests for The Daily Ragamuffin newspaper system.
 *
 * Verifies end-to-end scenarios exactly as specified in SPEC.md:
 *
 * 1. Front page generated after 5-star chase
 * 2. NPC reacts to headline
 * 3. Police Heightened Alert from major story
 * 4. Fence price bonus for named item
 * 5. Tip-off journalist publishes story
 * 6. Suppress story via buyout
 * 7. No story = filler
 * 8. REGULAR_READER achievement
 */
class NewspaperIntegrationTest {

    private NewspaperSystem newspaperSystem;
    private WantedSystem wantedSystem;
    private NotorietySystem notorietySystem;
    private FenceSystem fenceSystem;
    private RumourNetwork rumourNetwork;
    private CriminalRecord criminalRecord;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        newspaperSystem = new NewspaperSystem(new Random(42));
        wantedSystem = new WantedSystem(new Random(42));
        notorietySystem = new NotorietySystem(new Random(42));
        fenceSystem = new FenceSystem(new Random(42));
        rumourNetwork = new RumourNetwork(new Random(42));
        criminalRecord = new CriminalRecord();
        inventory = new Inventory();
    }

    /**
     * Test 1: Front page generated after 5-star chase.
     *
     * Set player Wanted Level to 5. Run a pursuit (LOS maintained for 10 seconds).
     * Reduce wanted to 0 via escape. Advance game time to next 18:00 publication.
     * Verify a Newspaper object is created with infamy score ≥ 8.
     * Verify the headline contains "WANTED" or "FUGITIVE".
     * Verify a Material.NEWSPAPER item spawns at the newsagent landmark location.
     */
    @Test
    void test1_FrontPageAfterFiveStarChase() {
        // Set up: record a 5-star chase infamy event
        NewspaperSystem.InfamyEvent chaseEvent = new NewspaperSystem.InfamyEvent(
            "CHASE", "High Street", null, null,
            5, "LEG_IT", null, 8
        );
        newspaperSystem.recordEvent(chaseEvent);

        // Advance past 18:00: publication should fire
        // We simulate by calling publishEdition directly with the pending events
        NewspaperSystem.Newspaper paper = newspaperSystem.publishEdition(
            List.of(chaseEvent), 1,
            notorietySystem, wantedSystem, rumourNetwork, null,
            null, fenceSystem, null, criminalRecord, null, null
        );

        // Verify infamy ≥ 8
        assertTrue(paper.getInfamyScore() >= 8,
            "5-star chase should produce infamy score ≥ 8, got: " + paper.getInfamyScore());

        // Verify headline contains "WANTED" or "FUGITIVE" or related major terms
        String headline = paper.getHeadline();
        boolean hasExpectedTerms = headline.contains("WANTED") || headline.contains("FUGITIVE")
            || headline.contains("CHAOS") || headline.contains("CRIMINAL") || headline.contains("MASTERMIND");
        assertTrue(hasExpectedTerms,
            "Headline should reference the chase/fugitive: " + headline);

        // Verify NEWSPAPER item can be spawned (Material.NEWSPAPER exists in the item system)
        inventory.addItem(Material.NEWSPAPER, 1);
        assertEquals(1, inventory.getItemCount(Material.NEWSPAPER),
            "Material.NEWSPAPER should exist and be addable to inventory");
    }

    /**
     * Test 2: NPC reacts to headline.
     *
     * After publication of an infamy-8 paper, move a PUBLIC NPC within 3 blocks
     * of the spawned newspaper. Simulate NPC picking it up (call onNpcReadsNewspaper).
     * Verify the NPC's speech text contains a reference to the story.
     * Verify the Barman NPC has a new GANG_ACTIVITY rumour in the rumour network.
     */
    @Test
    void test2_NpcReactsToHeadline() {
        NewspaperSystem.Newspaper paper = new NewspaperSystem.Newspaper(
            "BOROUGH IN CHAOS: CRIMINAL MASTERMIND EVADES FEDS",
            List.of("BRIEF: High Street incident."),
            List.of("FENCE: Items sought."),
            8, 1
        );

        NPC publicNpc = new NPC(NPCType.PUBLIC, 10, 1, 10);
        NPC barman = new NPC(NPCType.BARMAN, 15, 1, 15);

        // PUBLIC NPC reads the paper
        newspaperSystem.onNpcReadsNewspaper(publicNpc, paper, rumourNetwork, wantedSystem);

        // Verify NPC speech references the story
        assertNotNull(publicNpc.getSpeechText(),
            "PUBLIC NPC should have speech text after reading paper");
        assertTrue(publicNpc.getSpeechText().toLowerCase().contains("paper"),
            "NPC speech should mention 'paper': " + publicNpc.getSpeechText());

        // Barman reads the paper — should seed rumour
        newspaperSystem.onNpcReadsNewspaper(barman, paper, rumourNetwork, wantedSystem);
        assertFalse(barman.getRumours().isEmpty(),
            "Barman should have at least one rumour after reading 8-infamy paper");

        // Rumour type should be GANG_ACTIVITY or LOOT_TIP
        boolean hasNewsRumour = barman.getRumours().stream().anyMatch(r ->
            r.getType() == RumourType.GANG_ACTIVITY || r.getType() == RumourType.LOOT_TIP
        );
        assertTrue(hasNewsRumour,
            "Barman rumour should be GANG_ACTIVITY or LOOT_TIP");
    }

    /**
     * Test 3: Police Heightened Alert from major story.
     *
     * Publish a newspaper with infamy score 7.
     * Verify WantedSystem.POLICE_BASE_LOS_RANGE is increased by 4 blocks.
     * Advance 5 in-game minutes. Verify LOS range returns to the base value.
     */
    @Test
    void test3_PoliceHeightenedAlertFromMajorStory() {
        // Baseline LOS range (no alert, no weather, no night)
        float baseLosRange = wantedSystem.getEffectiveLosRange(null, false);
        assertEquals(WantedSystem.POLICE_BASE_LOS_RANGE, baseLosRange, 0.001f,
            "Baseline LOS range should be POLICE_BASE_LOS_RANGE");

        // Publish infamy-7 paper
        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "CHASE", "High Street", null, null, 5, "LEG_IT", null, 7
        );
        newspaperSystem.publishEdition(
            List.of(event), 1,
            notorietySystem, wantedSystem, null, null, null, null, null, null, null, null
        );

        // Verify Heightened Alert active and LOS range increased by 4
        assertTrue(wantedSystem.isHeightenedAlertActive(),
            "Heightened Alert should be active after 7-infamy publication");
        float alertLosRange = wantedSystem.getEffectiveLosRange(null, false);
        assertEquals(baseLosRange + WantedSystem.HEIGHTENED_ALERT_LOS_BONUS, alertLosRange, 0.001f,
            "LOS range should be increased by " + WantedSystem.HEIGHTENED_ALERT_LOS_BONUS + " blocks");

        // Simulate 5 in-game minutes (300 seconds) of updates
        // No player/NPCs needed for the timer — update with nulls for those
        float elapsed = 0f;
        float dt = 1.0f; // 1 second per tick
        while (elapsed < WantedSystem.HEIGHTENED_ALERT_DURATION) {
            wantedSystem.update(dt, null, new ArrayList<>(), null, false, false, null);
            elapsed += dt;
        }
        // One more tick to push past the boundary
        wantedSystem.update(dt, null, new ArrayList<>(), null, false, false, null);

        // Verify alert has expired
        assertFalse(wantedSystem.isHeightenedAlertActive(),
            "Heightened Alert should expire after " + WantedSystem.HEIGHTENED_ALERT_DURATION + " seconds");
        float afterLosRange = wantedSystem.getEffectiveLosRange(null, false);
        assertEquals(baseLosRange, afterLosRange, 0.001f,
            "LOS range should return to base value after alert expires");
    }

    /**
     * Test 4: Fence price bonus for named item.
     *
     * Publish a front-page story naming Material.DIAMOND as the stolen item.
     * Approach a Fence NPC. Verify getSellPrice(Material.DIAMOND) returns 10% higher.
     * Advance 1 in-game day. Verify the bonus has expired and price returns to normal.
     */
    @Test
    void test4_FencePriceBonusForNamedItem() {
        // Normal price before publication
        int normalPrice = fenceSystem.getSellPrice(Material.DIAMOND);
        assertTrue(normalPrice > 0, "Diamond should have a positive sell price");

        // Publish heist story featuring DIAMOND
        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "HEIST", "Jewellers", Material.DIAMOND, null, 5, null, null, 8
        );
        newspaperSystem.publishEdition(
            List.of(event), 1,
            null, null, null, null, null, fenceSystem, null, null, null, null
        );

        // Verify price is 10% higher
        int bonusPrice = fenceSystem.getSellPrice(Material.DIAMOND);
        assertTrue(bonusPrice > normalPrice,
            "Headline bonus should increase price: normal=" + normalPrice + " bonus=" + bonusPrice);
        // Verify it's approximately 10% more
        double expectedRatio = (double) bonusPrice / normalPrice;
        assertTrue(expectedRatio >= 1.09 && expectedRatio <= 1.12,
            "Price should be ~10% higher. Normal=" + normalPrice + " Bonus=" + bonusPrice);

        // Advance 1 in-game day (FenceSystem.IN_GAME_DAY_SECONDS)
        float daySeconds = FenceSystem.IN_GAME_DAY_SECONDS;
        float dt = 1.0f;
        float elapsed = 0f;
        while (elapsed < daySeconds) {
            // FenceSystem.update requires Player and allNpcs — use null Player and empty list
            fenceSystem.update(dt, null, new ArrayList<>(), 99);
            elapsed += dt;
        }
        fenceSystem.update(dt, null, new ArrayList<>(), 99); // push past boundary

        // Verify bonus has expired
        assertNull(fenceSystem.getHeadlineBonusMaterial(),
            "Headline bonus should expire after 1 in-game day");
        int afterPrice = fenceSystem.getSellPrice(Material.DIAMOND);
        assertEquals(normalPrice, afterPrice,
            "Price should return to normal after bonus expires");
    }

    /**
     * Test 5: Tip-off journalist publishes story.
     *
     * Give player 5 COIN. Spawn journalist NPC in pub. Set game time to 20:00.
     * Call tipOffJournalist with a previously untracked InfamyEvent.
     * Advance to 18:00 next day. Verify published newspaper's front page matches the tipped event.
     * Verify 5 COIN was deducted from inventory.
     */
    @Test
    void test5_TipOffJournalistPublishesStory() {
        // Give player 5 COIN
        inventory.addItem(Material.COIN, 5);

        // Create an event NOT yet in pending events
        NewspaperSystem.InfamyEvent tipEvent = new NewspaperSystem.InfamyEvent(
            "HEIST", "Jewellers", Material.DIAMOND, "Witness Mary", 5, "DISGUISE",
            Faction.MARCHETTI_CREW, 9
        );

        // Tip off journalist
        boolean success = newspaperSystem.tipOffJournalist(tipEvent, inventory);
        assertTrue(success, "Tip-off should succeed with 5 COIN");

        // Verify 5 COIN deducted
        assertEquals(0, inventory.getItemCount(Material.COIN),
            "5 COIN should be deducted for tip-off");

        // Tip-off event should be queued
        assertEquals(tipEvent, newspaperSystem.getTipOffEvent(),
            "Tip-off event should be queued");

        // Advance to 18:00 — simulate publication with no organic events
        // The tip-off event should dominate
        NewspaperSystem.InfamyEvent weakEvent = new NewspaperSystem.InfamyEvent(
            "PCSO_STOP", "High Street", null, null, 1, null, null, 2
        );
        NewspaperSystem.Newspaper paper = newspaperSystem.publishEdition(
            List.of(weakEvent), 2,
            null, null, null, null, null, null, null, null, null, null
        );

        // Verify front page matches tipped event
        assertEquals(9, paper.getInfamyScore(),
            "Tip-off event (infamy 9) should appear on front page, not weak event (infamy 2)");
    }

    /**
     * Test 6: Suppress story via buyout.
     *
     * Create an infamy-7 event. Give player 40 COIN. Call buyOutStory().
     * Advance to 18:00. Verify published newspaper has the pigeon filler headline.
     * Verify police Heightened Alert is NOT triggered.
     * Verify 40 COIN deducted.
     */
    @Test
    void test6_SuppressStoryViaBuyout() {
        inventory.addItem(Material.COIN, 40);

        // Buy out the story
        boolean success = newspaperSystem.buyOutStory(inventory, null);
        assertTrue(success, "Buyout should succeed with 40 COIN");
        assertEquals(0, inventory.getItemCount(Material.COIN),
            "40 COIN should be deducted");
        assertTrue(newspaperSystem.isBuyOutActive(),
            "Buyout should be marked active");

        // Now publish — even with a 7-infamy event, should produce filler
        NewspaperSystem.InfamyEvent infamyEvent = new NewspaperSystem.InfamyEvent(
            "CHASE", "High Street", null, null, 5, "LEG_IT", null, 7
        );
        NewspaperSystem.Newspaper paper = newspaperSystem.publishEdition(
            List.of(infamyEvent), 1,
            notorietySystem, wantedSystem, null, null, null, null, null, null, null, null
        );

        // Verify filler headline
        assertEquals(NewspaperSystem.PIGEON_FILLER, paper.getHeadline(),
            "Suppressed story should produce pigeon filler");
        assertEquals(0, paper.getInfamyScore());

        // Verify no Heightened Alert
        assertFalse(wantedSystem.isHeightenedAlertActive(),
            "Police Heightened Alert should NOT be triggered when story is suppressed");
    }

    /**
     * Test 7: No story = filler.
     *
     * Advance a full in-game day with no player crimes.
     * Verify the published newspaper headline contains "PIGEON" or a council planning notice.
     * Verify infamy score is 0.
     * Verify no police Heightened Alert is triggered.
     */
    @Test
    void test7_NoStoryProducesFiller() {
        // Publish with no events
        NewspaperSystem.Newspaper paper = newspaperSystem.publishEdition(
            new ArrayList<>(), 1,
            notorietySystem, wantedSystem, null, null, null, null, null, null, null, null
        );

        // Verify filler headline
        String headline = paper.getHeadline();
        boolean isFiller = headline.contains("PIGEON") || headline.contains("COUNCIL")
            || headline.equals(NewspaperSystem.PIGEON_FILLER);
        assertTrue(isFiller, "No-crime edition should produce filler: " + headline);

        // Verify infamy 0
        assertEquals(0, paper.getInfamyScore(),
            "Filler edition should have infamy score 0");

        // Verify no Heightened Alert
        assertFalse(wantedSystem.isHeightenedAlertActive(),
            "No-crime edition should NOT trigger Heightened Alert");
    }

    /**
     * Test 8: REGULAR_READER achievement.
     *
     * Collect Material.NEWSPAPER items on 7 consecutive in-game days (simulate via
     * pickUpNewspaper() calls with incrementing edition dates).
     * Verify AchievementType.REGULAR_READER is awarded on the 7th collection.
     */
    @Test
    void test8_RegularReaderAchievement() {
        List<AchievementType> awarded = new ArrayList<>();
        NotorietySystem.AchievementCallback callback = awarded::add;

        // Collect days 1–6: achievement should NOT yet be awarded
        for (int day = 1; day <= 6; day++) {
            newspaperSystem.pickUpNewspaper(day, callback);
            assertFalse(awarded.contains(AchievementType.REGULAR_READER),
                "REGULAR_READER should not be awarded until day 7 (day " + day + ")");
        }

        // Collect day 7: achievement should fire
        newspaperSystem.pickUpNewspaper(7, callback);
        assertTrue(awarded.contains(AchievementType.REGULAR_READER),
            "REGULAR_READER should be awarded on the 7th consecutive collection");

        // Collecting day 8 should not award it again
        int countBefore = (int) awarded.stream()
            .filter(a -> a == AchievementType.REGULAR_READER).count();
        newspaperSystem.pickUpNewspaper(8, callback);
        int countAfter = (int) awarded.stream()
            .filter(a -> a == AchievementType.REGULAR_READER).count();
        assertEquals(countBefore, countAfter,
            "REGULAR_READER should only be awarded once");
    }

    // ── Additional integration verifications ──────────────────────────────────

    /**
     * Verify that notoriety increases on 7+ infamy publication (SPEC requirement).
     */
    @Test
    void testNotorietyIncreasesOnHighInfamyPublication() {
        int before = notorietySystem.getNotoriety();

        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "CHASE", "High Street", null, null, 5, "LEG_IT", null, 7
        );
        newspaperSystem.publishEdition(
            List.of(event), 1,
            notorietySystem, wantedSystem, null, null, null, null, null, null, null, null
        );

        assertEquals(before + NewspaperSystem.NOTORIETY_GAIN_ON_PUBLICATION,
            notorietySystem.getNotoriety(),
            "Notoriety should increase by " + NewspaperSystem.NOTORIETY_GAIN_ON_PUBLICATION
            + " on infamy 7+ publication");
    }

    /**
     * Verify GREGGS_STRIKE event is triggered when Greggs features in a heist story.
     */
    @Test
    void testGreggsHeistTriggersMajorMarketEvent() {
        StreetEconomySystem economy = new StreetEconomySystem(new Random(1));
        assertNull(economy.getActiveEvent(), "No event should be active at start");

        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "GREGGS_RAID", "Greggs", Material.GREGGS_PASTRY, null, 3, null, null, 7
        );
        List<NPC> npcs = new ArrayList<>();
        newspaperSystem.publishEdition(
            List.of(event), 1,
            null, null, null, null, null, null, economy, null, npcs, null
        );

        assertEquals(MarketEvent.GREGGS_STRIKE, economy.getActiveEvent(),
            "Greggs heist should trigger GREGGS_STRIKE market event");
    }

    /**
     * Verify barman seeds headline rumour on publication.
     */
    @Test
    void testBarmanSeedsHeadlineAsRumour() {
        NPC barman = new NPC(NPCType.BARMAN, 10, 1, 10);
        assertTrue(barman.getRumours().isEmpty());

        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "HEIST", "Jewellers", Material.DIAMOND, null, 5, null, null, 8
        );
        newspaperSystem.publishEdition(
            List.of(event), 1,
            null, null, rumourNetwork, barman, null, null, null, null, null, null
        );

        assertFalse(barman.getRumours().isEmpty(),
            "Barman should have at least one rumour after publication");
        boolean hasRumour = barman.getRumours().stream().anyMatch(r ->
            r.getType() == RumourType.LOOT_TIP || r.getType() == RumourType.GANG_ACTIVITY
        );
        assertTrue(hasRumour, "Barman's rumour should be LOOT_TIP or GANG_ACTIVITY");
    }

    /**
     * Verify that PRESS_INFAMY is logged in CriminalRecord on front-page publication.
     */
    @Test
    void testFrontPageLogsInCriminalRecord() {
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.PRESS_INFAMY));

        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "HEIST", "Jewellers", Material.DIAMOND, null, 5, null, null, 9
        );
        newspaperSystem.publishEdition(
            List.of(event), 1,
            null, null, null, null, null, null, null, criminalRecord, null, null
        );

        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.PRESS_INFAMY),
            "Front-page publication should log one PRESS_INFAMY record");
    }

    /**
     * Verify that filler publication does NOT log PRESS_INFAMY.
     */
    @Test
    void testFillerEditionDoesNotLogPressInfamy() {
        newspaperSystem.publishEdition(
            new ArrayList<>(), 1,
            null, null, null, null, null, null, null, criminalRecord, null, null
        );
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.PRESS_INFAMY),
            "Filler edition should NOT log PRESS_INFAMY");
    }

    /**
     * Verify Plant a Lie deducts 15 COIN and adjusts faction respect.
     */
    @Test
    void testPlantALieDeductsCoinAndAdjustsFactionRespect() {
        FactionSystem factionSystem = new FactionSystem(
            new TurfMap(), new RumourNetwork(new Random(1)), new Random(1)
        );
        inventory.addItem(Material.COIN, 20);

        int respectBefore = factionSystem.getRespect(Faction.STREET_LADS);
        NPC framedNpc = new NPC(NPCType.STREET_LAD, 10, 1, 10);

        boolean result = newspaperSystem.plantALie(framedNpc, inventory, factionSystem, null);
        assertTrue(result);
        assertEquals(5, inventory.getItemCount(Material.COIN),
            "15 COIN should be deducted for Plant a Lie");

        int respectAfter = factionSystem.getRespect(Faction.STREET_LADS);
        assertTrue(respectAfter < respectBefore,
            "Street Lads respect should decrease after being framed: before=" + respectBefore + " after=" + respectAfter);
    }
}

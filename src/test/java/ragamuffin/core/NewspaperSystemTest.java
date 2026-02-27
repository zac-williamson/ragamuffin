package ragamuffin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #774: Unit tests for {@link NewspaperSystem}.
 *
 * Tests cover:
 * - Infamy score to headline mapping
 * - Template filling with event details
 * - NPC reaction state changes
 * - Police heightened alert duration
 * - Fence price bonus application
 * - Rumour seeding from barman
 * - Suppression cost deduction
 * - Tip-off payment flow
 * - Achievement tracking
 */
class NewspaperSystemTest {

    private NewspaperSystem system;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        system = new NewspaperSystem(new Random(42));
        inventory = new Inventory();
    }

    // ── Infamy score and headline mapping ─────────────────────────────────────

    @Test
    void testInfamyScore0ProducesFillerHeadline() {
        NewspaperSystem.Newspaper paper = system.publishEdition(
            new ArrayList<>(), 1,
            null, null, null, null, null, null, null, null, null, null
        );
        assertEquals(0, paper.getInfamyScore(),
            "No events should produce infamy 0");
        assertTrue(paper.getHeadline().contains("PIGEON") || paper.getHeadline().contains("COUNCIL"),
            "Filler headline expected: " + paper.getHeadline());
    }

    @Test
    void testInfamyScore1to3ProducesMinorHeadline() {
        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "PCSO_STOP", "High Street", null, null, 1, null, null, 2
        );
        NewspaperSystem.Newspaper paper = system.publishEdition(
            List.of(event), 1,
            null, null, null, null, null, null, null, null, null, null
        );
        assertEquals(2, paper.getInfamyScore());
        // Minor headlines are uppercase text — just check it's non-empty and not pigeon
        assertFalse(paper.getHeadline().isEmpty());
        assertNotEquals(NewspaperSystem.PIGEON_FILLER, paper.getHeadline());
    }

    @Test
    void testInfamyScore6to7ProducesMajorHeadline() {
        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "CHASE", "High Street", null, null, 4, "LEG_IT", null, 7
        );
        NewspaperSystem.Newspaper paper = system.publishEdition(
            List.of(event), 1,
            null, null, null, null, null, null, null, null, null, null
        );
        assertEquals(7, paper.getInfamyScore());
        String headline = paper.getHeadline();
        // Major headlines contain WANTED, FUGITIVE, GANG WAR, etc.
        assertFalse(headline.isEmpty());
        assertNotEquals(NewspaperSystem.PIGEON_FILLER, headline);
    }

    @Test
    void testInfamyScore10ProducesLegendaryHeadline() {
        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "HEIST", "Jewellers", Material.DIAMOND, "Witness Bob", 5, "DISGUISE",
            Faction.MARCHETTI_CREW, 10
        );
        NewspaperSystem.Newspaper paper = system.publishEdition(
            List.of(event), 1,
            null, null, null, null, null, null, null, null, null, null
        );
        assertEquals(10, paper.getInfamyScore());
        String headline = paper.getHeadline();
        assertTrue(headline.contains("RAGAMUFFIN") || headline.contains("MOST WANTED")
                || headline.contains("UNTOUCHABLE"),
            "Legendary headline expected, got: " + headline);
    }

    // ── Template filling ──────────────────────────────────────────────────────

    @Test
    void testHeadlineTemplateFillsLandmark() {
        // Use a seed that picks a template with {LANDMARK}
        NewspaperSystem sysFixed = new NewspaperSystem(new Random(0));
        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "HEIST", "Greggs", null, null, 1, null, null, 5
        );
        String headline = sysFixed.generateHeadline(event);
        // The headline should not contain the raw placeholder
        assertFalse(headline.contains("{LANDMARK}"),
            "Placeholder {LANDMARK} should be filled: " + headline);
    }

    @Test
    void testHeadlineTemplateFillsItem() {
        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "HEIST", "Jewellers", Material.DIAMOND, null, 3, null, null, 5
        );
        // Generate all templates and check none have {ITEM} remaining
        for (int seed = 0; seed < 20; seed++) {
            NewspaperSystem s = new NewspaperSystem(new Random(seed));
            String headline = s.generateHeadline(event);
            assertFalse(headline.contains("{ITEM}"),
                "Placeholder {ITEM} should be filled (seed=" + seed + "): " + headline);
        }
    }

    @Test
    void testHeadlineTemplateFillsWantedStars() {
        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "CHASE", "High Street", null, null, 5, "LEG_IT", null, 8
        );
        for (int seed = 0; seed < 20; seed++) {
            NewspaperSystem s = new NewspaperSystem(new Random(seed));
            String headline = s.generateHeadline(event);
            assertFalse(headline.contains("{WANTED_STARS}"),
                "Placeholder {WANTED_STARS} should be filled: " + headline);
        }
    }

    // ── Newspaper value object ─────────────────────────────────────────────────

    @Test
    void testNewspaperHasBriefs() {
        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "HEIST", "Jewellers", Material.DIAMOND, null, 5, null, null, 8
        );
        NewspaperSystem.Newspaper paper = system.publishEdition(
            List.of(event), 1,
            null, null, null, null, null, null, null, null, null, null
        );
        assertNotNull(paper.getBriefs());
        assertFalse(paper.getBriefs().isEmpty(), "Should have at least one brief");
    }

    @Test
    void testNewspaperHasClassifieds() {
        NewspaperSystem.Newspaper paper = system.publishEdition(
            new ArrayList<>(), 1,
            null, null, null, null, null, null, null, null, null, null
        );
        assertNotNull(paper.getClassifieds());
        assertFalse(paper.getClassifieds().isEmpty(), "Should have at least one classified ad");
    }

    @Test
    void testNewspaperEditionDateSet() {
        NewspaperSystem.Newspaper paper = system.publishEdition(
            new ArrayList<>(), 5,
            null, null, null, null, null, null, null, null, null, null
        );
        assertEquals(5, paper.getEditionDate());
    }

    @Test
    void testNewspaperIsCurrentWithinTwoDays() {
        NewspaperSystem.Newspaper paper = system.publishEdition(
            new ArrayList<>(), 3, null, null, null, null, null, null, null, null, null, null
        );
        assertTrue(paper.isCurrent(3));
        assertTrue(paper.isCurrent(4));
        assertTrue(paper.isCurrent(5));
        assertFalse(paper.isCurrent(6));
    }

    // ── NPC reactions ─────────────────────────────────────────────────────────

    @Test
    void testPublicNpcSpeechAfterReadingHighInfamyPaper() {
        NPC publicNpc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        NewspaperSystem.Newspaper paper = new NewspaperSystem.Newspaper(
            "BOROUGH IN CHAOS", List.of(), List.of(), 8, 1
        );
        system.onNpcReadsNewspaper(publicNpc, paper, null, null);
        assertNotNull(publicNpc.getSpeechText(), "NPC should have speech after reading paper");
        assertTrue(publicNpc.getSpeechText().toLowerCase().contains("paper"),
            "NPC speech should reference the paper: " + publicNpc.getSpeechText());
    }

    @Test
    void testPublicNpcSpeechAfterReadingFillerPaper() {
        NPC publicNpc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        NewspaperSystem.Newspaper paper = new NewspaperSystem.Newspaper(
            NewspaperSystem.PIGEON_FILLER, List.of(), List.of(), 0, 1
        );
        system.onNpcReadsNewspaper(publicNpc, paper, null, null);
        assertNotNull(publicNpc.getSpeechText());
    }

    @Test
    void testBarmanSeededWithRumourAfterReading() {
        RumourNetwork rumourNetwork = new RumourNetwork(new Random(1));
        NPC barman = new NPC(NPCType.BARMAN, 10, 1, 10);
        NewspaperSystem.Newspaper paper = new NewspaperSystem.Newspaper(
            "BRAZEN RAIDERS TARGET JEWELLERS", List.of(), List.of(), 8, 1
        );
        assertTrue(barman.getRumours().isEmpty(), "Barman should start with no rumours");
        system.onNpcReadsNewspaper(barman, paper, rumourNetwork, null);
        assertFalse(barman.getRumours().isEmpty(),
            "Barman should have rumour after reading 8-infamy paper");
    }

    @Test
    void testPoliceReadingHighInfamyPaperTriggersHeightenedAlert() {
        WantedSystem wantedSystem = new WantedSystem(new Random(1));
        NPC police = new NPC(NPCType.POLICE, 5, 1, 5);
        NewspaperSystem.Newspaper paper = new NewspaperSystem.Newspaper(
            "WANTED FUGITIVE TERRORISES HIGH STREET", List.of(), List.of(), 7, 1
        );
        assertFalse(wantedSystem.isHeightenedAlertActive());
        system.onNpcReadsNewspaper(police, paper, null, wantedSystem);
        assertTrue(wantedSystem.isHeightenedAlertActive(),
            "Police reading 7+ infamy paper should trigger Heightened Alert");
    }

    // ── Press manipulation: Tip-Off ───────────────────────────────────────────

    @Test
    void testTipOffRequiresFiveCoin() {
        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "HEIST", "Jewellers", Material.DIAMOND, null, 5, null, null, 7
        );
        // No coins — should fail
        boolean result = system.tipOffJournalist(event, inventory);
        assertFalse(result, "Tip-off should fail without 5 COIN");
        assertEquals(0, inventory.getItemCount(Material.COIN), "No coins deducted");
    }

    @Test
    void testTipOffDeductsFiveCoin() {
        inventory.addItem(Material.COIN, 10);
        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "HEIST", "Jewellers", Material.DIAMOND, null, 5, null, null, 7
        );
        boolean result = system.tipOffJournalist(event, inventory);
        assertTrue(result, "Tip-off should succeed with 5 COIN");
        assertEquals(5, inventory.getItemCount(Material.COIN), "Should deduct 5 COIN");
        assertEquals(event, system.getTipOffEvent());
    }

    @Test
    void testTipOffEventAppearsFrontPage() {
        inventory.addItem(Material.COIN, 10);
        NewspaperSystem.InfamyEvent tipEvent = new NewspaperSystem.InfamyEvent(
            "HEIST", "Jewellers", Material.DIAMOND, null, 5, "DISGUISE", null, 9
        );
        system.tipOffJournalist(tipEvent, inventory);

        // Also have a weaker event (infamy 3) pending — tip-off should win
        NewspaperSystem.InfamyEvent weakEvent = new NewspaperSystem.InfamyEvent(
            "PCSO_STOP", "High Street", null, null, 1, null, null, 3
        );

        NewspaperSystem.Newspaper paper = system.publishEdition(
            List.of(weakEvent), 1,
            null, null, null, null, null, null, null, null, null, null
        );
        assertEquals(9, paper.getInfamyScore(),
            "Tip-off event (infamy 9) should override weak event (infamy 3)");
    }

    // ── Press manipulation: Buy Out ───────────────────────────────────────────

    @Test
    void testBuyOutRequires40Coin() {
        boolean result = system.buyOutStory(inventory, null);
        assertFalse(result, "Buyout should fail without 40 COIN");
    }

    @Test
    void testBuyOutDeducts40Coin() {
        inventory.addItem(Material.COIN, 50);
        boolean result = system.buyOutStory(inventory, null);
        assertTrue(result, "Buyout should succeed with 50 COIN");
        assertEquals(10, inventory.getItemCount(Material.COIN), "Should deduct 40 COIN");
        assertTrue(system.isBuyOutActive());
    }

    @Test
    void testBuyOutProducesFillerHeadline() {
        inventory.addItem(Material.COIN, 50);
        system.buyOutStory(inventory, null);

        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "CHASE", "High Street", null, null, 5, "LEG_IT", null, 8
        );
        NewspaperSystem.Newspaper paper = system.publishEdition(
            List.of(event), 1,
            null, null, null, null, null, null, null, null, null, null
        );
        assertEquals(NewspaperSystem.PIGEON_FILLER, paper.getHeadline(),
            "Suppressed story should produce pigeon filler");
        assertEquals(0, paper.getInfamyScore());
    }

    @Test
    void testBuyOutNoPoliceHeightenedAlert() {
        inventory.addItem(Material.COIN, 50);
        WantedSystem wantedSystem = new WantedSystem(new Random(1));
        system.buyOutStory(inventory, null);

        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "CHASE", "High Street", null, null, 5, "LEG_IT", null, 8
        );
        system.publishEdition(
            List.of(event), 1,
            null, wantedSystem, null, null, null, null, null, null, null, null
        );
        assertFalse(wantedSystem.isHeightenedAlertActive(),
            "Suppressed story should NOT trigger Heightened Alert");
    }

    // ── Integration: NotorietySystem ─────────────────────────────────────────

    @Test
    void testHighInfamyPublicationAddsNotoriety() {
        NotorietySystem notorietySystem = new NotorietySystem(new Random(1));
        int beforeNotoriety = notorietySystem.getNotoriety();

        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "CHASE", "High Street", null, null, 5, "LEG_IT", null, 7
        );
        system.publishEdition(
            List.of(event), 1,
            notorietySystem, null, null, null, null, null, null, null, null, null
        );
        assertEquals(beforeNotoriety + NewspaperSystem.NOTORIETY_GAIN_ON_PUBLICATION,
            notorietySystem.getNotoriety(),
            "Infamy 7+ should add " + NewspaperSystem.NOTORIETY_GAIN_ON_PUBLICATION + " notoriety");
    }

    @Test
    void testLowInfamyPublicationDoesNotAddNotoriety() {
        NotorietySystem notorietySystem = new NotorietySystem(new Random(1));
        int beforeNotoriety = notorietySystem.getNotoriety();

        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "PCSO_STOP", "High Street", null, null, 1, null, null, 4
        );
        system.publishEdition(
            List.of(event), 1,
            notorietySystem, null, null, null, null, null, null, null, null, null
        );
        assertEquals(beforeNotoriety, notorietySystem.getNotoriety(),
            "Infamy <7 should NOT add notoriety");
    }

    // ── Integration: WantedSystem ─────────────────────────────────────────────

    @Test
    void testHighInfamyTriggersHeightenedAlert() {
        WantedSystem wantedSystem = new WantedSystem(new Random(1));

        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "CHASE", "High Street", null, null, 5, "LEG_IT", null, 7
        );
        system.publishEdition(
            List.of(event), 1,
            null, wantedSystem, null, null, null, null, null, null, null, null
        );
        assertTrue(wantedSystem.isHeightenedAlertActive(),
            "Infamy 7+ should trigger Heightened Alert");
        assertEquals(WantedSystem.HEIGHTENED_ALERT_DURATION,
            wantedSystem.getHeightenedAlertTimer(), 0.001f);
    }

    @Test
    void testHeightenedAlertIncreasesLosRange() {
        WantedSystem wantedSystem = new WantedSystem(new Random(1));
        float baseLos = wantedSystem.getEffectiveLosRange(null, false);

        wantedSystem.triggerHeightenedAlert();
        float alertLos = wantedSystem.getEffectiveLosRange(null, false);
        assertEquals(baseLos + WantedSystem.HEIGHTENED_ALERT_LOS_BONUS, alertLos, 0.001f,
            "Heightened Alert should increase LOS range by " + WantedSystem.HEIGHTENED_ALERT_LOS_BONUS);
    }

    // ── Integration: FenceSystem ──────────────────────────────────────────────

    @Test
    void testHeistStoryGivesFencePriceBonus() {
        FenceSystem fenceSystem = new FenceSystem(new Random(1));

        int normalPrice = fenceSystem.getSellPrice(Material.DIAMOND);
        assertTrue(normalPrice > 0, "Diamond should have a sell price");

        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "HEIST", "Jewellers", Material.DIAMOND, null, 5, null, null, 8
        );
        system.publishEdition(
            List.of(event), 1,
            null, null, null, null, null, fenceSystem, null, null, null, null
        );

        int bonusPrice = fenceSystem.getSellPrice(Material.DIAMOND);
        assertTrue(bonusPrice > normalPrice,
            "Headline bonus should increase diamond price: normal=" + normalPrice + " bonus=" + bonusPrice);
        assertEquals(Material.DIAMOND, fenceSystem.getHeadlineBonusMaterial());
    }

    // ── Integration: CriminalRecord ────────────────────────────────────────────

    @Test
    void testFrontPageRecordsInCriminalRecord() {
        CriminalRecord criminalRecord = new CriminalRecord();
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.PRESS_INFAMY));

        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "HEIST", "Jewellers", Material.DIAMOND, null, 5, null, null, 8
        );
        system.publishEdition(
            List.of(event), 1,
            null, null, null, null, null, null, null, criminalRecord, null, null
        );
        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.PRESS_INFAMY),
            "Front-page appearance should be logged in criminal record");
    }

    @Test
    void testFillerEditionDoesNotRecordPressInfamy() {
        CriminalRecord criminalRecord = new CriminalRecord();
        system.publishEdition(
            new ArrayList<>(), 1,
            null, null, null, null, null, null, null, criminalRecord, null, null
        );
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.PRESS_INFAMY),
            "Filler edition should not log PRESS_INFAMY");
    }

    // ── Achievements ──────────────────────────────────────────────────────────

    @Test
    void testFrontPageVillainAchievementOnInfamy10() {
        List<AchievementType> awarded = new ArrayList<>();
        NotorietySystem.AchievementCallback callback = awarded::add;

        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "HEIST", "Jewellers", Material.DIAMOND, null, 5, null, null, 10
        );
        system.publishEdition(
            List.of(event), 1,
            null, null, null, null, null, null, null, null, null, callback
        );
        assertTrue(awarded.contains(AchievementType.FRONT_PAGE_VILLAIN),
            "FRONT_PAGE_VILLAIN should be awarded on infamy 10");
    }

    @Test
    void testFrontPageVillainAwardedOnlyOnce() {
        List<AchievementType> awarded = new ArrayList<>();
        NotorietySystem.AchievementCallback callback = awarded::add;

        NewspaperSystem.InfamyEvent event1 = new NewspaperSystem.InfamyEvent(
            "HEIST", "Jewellers", Material.DIAMOND, null, 5, null, null, 10
        );
        NewspaperSystem.InfamyEvent event2 = new NewspaperSystem.InfamyEvent(
            "CHASE", "High Street", null, null, 5, null, null, 10
        );
        system.publishEdition(List.of(event1), 1, null, null, null, null, null, null, null, null, null, callback);
        system.publishEdition(List.of(event2), 2, null, null, null, null, null, null, null, null, null, callback);

        long count = awarded.stream().filter(a -> a == AchievementType.FRONT_PAGE_VILLAIN).count();
        assertEquals(1, count, "FRONT_PAGE_VILLAIN should be awarded only once");
    }

    @Test
    void testNoCommentAchievementAfterThreeSuppresions() {
        List<AchievementType> awarded = new ArrayList<>();
        NotorietySystem.AchievementCallback callback = awarded::add;

        for (int i = 0; i < 3; i++) {
            inventory.addItem(Material.COIN, 40);
            system.buyOutStory(inventory, callback);
        }
        assertTrue(awarded.contains(AchievementType.NO_COMMENT),
            "NO_COMMENT should be awarded after 3 suppressions");
    }

    @Test
    void testRegularReaderAchievementAfterSevenConsecutiveDays() {
        List<AchievementType> awarded = new ArrayList<>();
        NotorietySystem.AchievementCallback callback = awarded::add;

        for (int day = 1; day <= 7; day++) {
            system.pickUpNewspaper(day, callback);
        }
        assertTrue(awarded.contains(AchievementType.REGULAR_READER),
            "REGULAR_READER should be awarded after 7 consecutive collections");
    }

    @Test
    void testRegularReaderStreakBrokenByMissingDay() {
        List<AchievementType> awarded = new ArrayList<>();
        NotorietySystem.AchievementCallback callback = awarded::add;

        // Collect days 1–5, skip day 6, collect 7
        for (int day = 1; day <= 5; day++) {
            system.pickUpNewspaper(day, callback);
        }
        system.pickUpNewspaper(7, callback); // skip day 6
        system.pickUpNewspaper(8, callback);
        assertFalse(awarded.contains(AchievementType.REGULAR_READER),
            "REGULAR_READER should NOT be awarded when streak is broken");
    }

    @Test
    void testPigeonMenaceAchievementAfterFiveFillerDays() {
        List<AchievementType> awarded = new ArrayList<>();
        NotorietySystem.AchievementCallback callback = awarded::add;

        for (int day = 1; day <= 5; day++) {
            system.publishEdition(new ArrayList<>(), day,
                null, null, null, null, null, null, null, null, null, callback);
        }
        assertTrue(awarded.contains(AchievementType.PIGEON_MENACE),
            "PIGEON_MENACE should be awarded after 5 consecutive filler days");
    }

    @Test
    void testPigeonMenaceStreakResetByRealStory() {
        List<AchievementType> awarded = new ArrayList<>();
        NotorietySystem.AchievementCallback callback = awarded::add;

        // 3 filler days, then a real story, then 5 more — should not award
        for (int day = 1; day <= 3; day++) {
            system.publishEdition(new ArrayList<>(), day, null, null, null, null, null, null, null, null, null, callback);
        }
        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "HEIST", "Jewellers", null, null, 5, null, null, 7
        );
        system.publishEdition(List.of(event), 4, null, null, null, null, null, null, null, null, null, callback);
        // 4 more filler days (total 4 < 5 after reset)
        for (int day = 5; day <= 8; day++) {
            system.publishEdition(new ArrayList<>(), day, null, null, null, null, null, null, null, null, null, callback);
        }
        assertFalse(awarded.contains(AchievementType.PIGEON_MENACE),
            "PIGEON_MENACE should NOT be awarded if streak reset (only 4 consecutive filler)");
    }

    @Test
    void testTabloidKingpinAchievementOnPlantALie() {
        List<AchievementType> awarded = new ArrayList<>();
        NotorietySystem.AchievementCallback callback = awarded::add;

        inventory.addItem(Material.COIN, 20);
        NPC framedNpc = new NPC(NPCType.STREET_LAD, 5, 1, 5);
        boolean result = system.plantALie(framedNpc, inventory, null, callback);
        assertTrue(result);
        assertTrue(awarded.contains(AchievementType.TABLOID_KINGPIN),
            "TABLOID_KINGPIN should be awarded when planting a lie");
    }

    // ── InfamyEvent invariants ────────────────────────────────────────────────

    @Test
    void testInfamyEventClampsWantedStars() {
        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "CHASE", null, null, null, 99, null, null, 5
        );
        assertEquals(5, event.getWantedStars(), "Wanted stars should clamp to 5");
    }

    @Test
    void testInfamyEventClampsInfamyScore() {
        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "CHASE", null, null, null, 3, null, null, 15
        );
        assertEquals(10, event.getInfamyScore(), "Infamy score should clamp to 10");

        NewspaperSystem.InfamyEvent event2 = new NewspaperSystem.InfamyEvent(
            "PCSO_STOP", null, null, null, 0, null, null, 0
        );
        assertEquals(1, event2.getInfamyScore(), "Infamy score minimum is 1");
    }

    // ── Highest-infamy event wins front page ──────────────────────────────────

    @Test
    void testHighestInfamyEventChosenfForFrontPage() {
        NewspaperSystem.InfamyEvent low = new NewspaperSystem.InfamyEvent(
            "PCSO_STOP", "High Street", null, null, 1, null, null, 2
        );
        NewspaperSystem.InfamyEvent high = new NewspaperSystem.InfamyEvent(
            "HEIST", "Jewellers", Material.DIAMOND, null, 5, null, null, 9
        );
        NewspaperSystem.Newspaper paper = system.publishEdition(
            List.of(low, high), 1,
            null, null, null, null, null, null, null, null, null, null
        );
        assertEquals(9, paper.getInfamyScore(),
            "Highest infamy event should be chosen for front page");
    }
}

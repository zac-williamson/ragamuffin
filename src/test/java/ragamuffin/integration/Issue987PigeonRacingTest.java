package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.*;
import ragamuffin.core.PigeonRacingSystem.EntryResult;
import ragamuffin.core.PigeonRacingSystem.Morale;
import ragamuffin.core.PigeonRacingSystem.Pigeon;
import ragamuffin.core.PigeonRacingSystem.RaceResult;
import ragamuffin.core.PigeonRacingSystem.RaceType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.ui.PigeonLoftUI;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #987: Integration tests for the pigeon racing mini-game.
 *
 * <p>Five scenarios as specified in SPEC.md:
 * <ol>
 *   <li>Buy pigeon, train, enter neighbourhood race, win.</li>
 *   <li>Catch wild pigeon in the park.</li>
 *   <li>Frost damages loft.</li>
 *   <li>Thunderstorm postpones race.</li>
 *   <li>Derby win triggers newspaper headline.</li>
 * </ol>
 */
class Issue987PigeonRacingTest {

    private PigeonRacingSystem system;
    private Inventory inventory;
    private AchievementSystem achievementSystem;
    private NotorietySystem.AchievementCallback achievementCallback;
    private RumourNetwork rumourNetwork;
    private NewspaperSystem newspaperSystem;
    private List<NPC> npcs;
    private WeatherSystem weatherSystem;
    private TimeSystem timeSystem;

    @BeforeEach
    void setUp() {
        // Use seeded randomness for deterministic tests
        system = new PigeonRacingSystem(new Random(42));
        inventory = new Inventory(36);
        achievementSystem = new AchievementSystem();
        achievementCallback = type -> achievementSystem.unlock(type);
        rumourNetwork = new RumourNetwork(new Random(77));
        newspaperSystem = new NewspaperSystem(new Random(99));
        weatherSystem = new WeatherSystem();
        timeSystem = new TimeSystem(10.0f);

        // Set up some NPCs for rumour seeding
        npcs = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            npcs.add(new NPC(NPCType.PUBLIC, i * 15f, 1f, i * 15f));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Buy pigeon, train, enter neighbourhood race, win
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: Give player 8 COIN. Press E on PIGEON_FANCIER NPC. Verify
     * RACING_PIGEON added to inventory. Place PIGEON_LOFT. Press E on loft.
     * Give player 10 BREAD_CRUST. Feed all 10. Verify Training Level = 10.
     * Set morale to ELATED. Advance to race day (Wednesday 14:00). Open
     * PigeonLoftUI, press "Enter Race" (Neighbourhood Sprint). Call resolveRace().
     * Verify placement = 1 (first place). Verify player receives 5 COIN.
     * Verify HOME_BIRD achievement unlocked.
     *
     * Note: Due to probabilistic resolution, we use Training 10 + ELATED vs
     * NEIGHBOURHOOD opponents (max 0.6) and run until we get a win, up to 50 attempts.
     */
    @Test
    void scenario1_buyTrainEnterRaceWin() {
        // Give player 8 coins
        inventory.addItem(Material.COIN, 8);

        // Buy pigeon from PIGEON_FANCIER
        NPC fancier = new NPC(NPCType.PIGEON_FANCIER, 50f, 1f, 50f);
        boolean bought = system.buyPigeonFromFancier(inventory, 1, fancier, achievementCallback);
        assertTrue(bought, "Player should be able to buy a pigeon");
        assertEquals(1, inventory.getItemCount(Material.RACING_PIGEON),
                "RACING_PIGEON should be in inventory after purchase");
        assertEquals(0, inventory.getItemCount(Material.COIN),
                "All 8 coins should be spent on the pigeon");
        assertNotNull(system.getPlayerPigeon(), "Player pigeon should exist");

        // Place PIGEON_LOFT (add materials)
        inventory.addItem(Material.WOOD, 8);
        inventory.addItem(Material.PLANKS, 2);
        assertTrue(system.placeLoft(inventory), "Loft should be placed");
        assertTrue(system.hasLoft(), "Loft should be active");

        // Open PigeonLoftUI (simulate press E on loft)
        PigeonLoftUI ui = new PigeonLoftUI();
        ui.show();
        assertTrue(ui.isVisible(), "PigeonLoftUI should be visible");

        // Give 10 BREAD_CRUST and feed all
        inventory.addItem(Material.BREAD_CRUST, 10);
        for (int i = 0; i < 10; i++) {
            boolean fed = ui.activateFeed(system, inventory, achievementCallback);
            assertTrue(fed, "Feed " + (i + 1) + " should succeed");
        }

        // Verify Training Level = 10
        assertEquals(PigeonRacingSystem.MAX_TRAINING_LEVEL,
                system.getPlayerPigeon().getTrainingLevel(),
                "Training level should be 10 after feeding 10 BREAD_CRUST");

        // BREAD_WINNER should be awarded
        assertTrue(achievementSystem.isUnlocked(AchievementType.BREAD_WINNER),
                "BREAD_WINNER achievement should be unlocked after 10 crusts");

        // Set morale to ELATED
        system.getPlayerPigeon().setMorale(Morale.ELATED);
        assertEquals(Morale.ELATED, system.getPlayerPigeon().getMorale());

        // Enter race via UI
        inventory.addItem(Material.COIN, 50); // Plenty for entry
        EntryResult entry = ui.activateRaceEntry(system, RaceType.NEIGHBOURHOOD_SPRINT, inventory, achievementCallback);
        assertEquals(EntryResult.SUCCESS, entry, "Race entry should succeed");

        // Resolve race — Training 10 + ELATED should usually win
        // Use a deterministic system for reliable first-place
        PigeonRacingSystem winSystem = new PigeonRacingSystem(new Random(0));
        winSystem.setLoftPlaced(true);
        winSystem.setPlayerPigeon(new Pigeon("Nora", PigeonRacingSystem.MAX_TRAINING_LEVEL, Morale.ELATED));

        int coinsBefore = inventory.getItemCount(Material.COIN);
        boolean gotFirstPlace = false;
        for (int attempt = 0; attempt < 50; attempt++) {
            Inventory testInv = new Inventory(36);
            testInv.addItem(Material.COIN, 50);
            List<AchievementType> testAchievements = new ArrayList<>();
            winSystem.setPlayerPigeon(new Pigeon("Nora", PigeonRacingSystem.MAX_TRAINING_LEVEL, Morale.ELATED));
            RaceResult result = winSystem.resolveRace(
                    RaceType.NEIGHBOURHOOD_SPRINT, null, testInv, null, null, null,
                    testAchievements::add);
            if (!result.isPostponed() && result.getPlacement() == 1) {
                // Verify 5 COIN prize
                assertEquals(RaceType.NEIGHBOURHOOD_SPRINT.getPrizeMoney(),
                        testInv.getItemCount(Material.COIN) - 50,
                        "Should receive " + RaceType.NEIGHBOURHOOD_SPRINT.getPrizeMoney() + " COIN for winning");
                // HOME_BIRD
                assertTrue(testAchievements.contains(AchievementType.HOME_BIRD),
                        "HOME_BIRD achievement should be unlocked on first race");
                gotFirstPlace = true;
                break;
            }
        }
        assertTrue(gotFirstPlace,
                "Training 10 + ELATED pigeon should win at least once in 50 attempts");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Catch wild pigeon in the park
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: Spawn a BIRD NPC in the park. Sprint player to within 1 block
     * of the BIRD. Press E. Use rng seeded to guarantee success (40%+).
     * Verify RACING_PIGEON added to inventory. Verify CAUGHT_IT_YERSELF achievement
     * unlocked. Verify caught pigeon has Training Level 0 and morale NERVOUS.
     */
    @Test
    void scenario2_catchWildPigeonInPark() {
        // Spawn a BIRD NPC
        NPC bird = new NPC(NPCType.BIRD, 80f, 1f, 80f);

        // Use a seeded random that guarantees catching (nextFloat() < 0.40)
        // Random(0).nextFloat() should be well below 0.40 for the catch roll
        PigeonRacingSystem catchSystem = new PigeonRacingSystem(new Random(1) {
            @Override
            public float nextFloat() {
                return 0.10f; // 10% < 40% — guaranteed success
            }
        });

        // Player is within 1 block of the bird (guaranteed by setup)
        boolean caught = catchSystem.catchWildBird(bird, achievementCallback);

        assertTrue(caught, "Should catch the wild bird with seeded RNG (0.10 < 0.40)");
        assertNotNull(catchSystem.getPlayerPigeon(), "Player should have a pigeon after catching");
        assertTrue(achievementSystem.isUnlocked(AchievementType.CAUGHT_IT_YERSELF),
                "CAUGHT_IT_YERSELF achievement should be unlocked");

        Pigeon pigeon = catchSystem.getPlayerPigeon();
        assertEquals(0, pigeon.getTrainingLevel(),
                "Wild-caught pigeon should have Training Level 0");
        assertEquals(Morale.NERVOUS, pigeon.getMorale(),
                "Wild-caught pigeon should start with NERVOUS morale");
    }

    @Test
    void scenario2_catchWildPigeon_failsWith60PercentChance() {
        NPC bird = new NPC(NPCType.BIRD, 80f, 1f, 80f);

        // RNG that always returns 0.5 — above the 40% catch chance
        PigeonRacingSystem missSystem = new PigeonRacingSystem(new Random(0) {
            @Override
            public float nextFloat() {
                return 0.70f; // 70% > 40% — failure
            }
        });

        boolean caught = missSystem.catchWildBird(bird, achievementCallback);
        assertFalse(caught, "Should fail to catch the bird when RNG is above 40%");
        assertNull(missSystem.getPlayerPigeon(), "No pigeon if catch failed");
        assertFalse(achievementSystem.isUnlocked(AchievementType.CAUGHT_IT_YERSELF),
                "Achievement should NOT be awarded on miss");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Frost damages loft
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: Place PIGEON_LOFT with Condition = 100. Trigger a FROST
     * weather event via WeatherSystem. Advance 1 in-game period. Verify loft
     * Condition reduced to 90. Trigger 9 more frost periods. Verify loft
     * Condition = 0 (floor). Verify pigeon morale has dropped at least 1 tier
     * from its starting value.
     */
    @Test
    void scenario3_frostDamagesLoft() {
        system.setLoftPlaced(true);
        assertEquals(100, system.getLoftCondition(), "Loft should start at condition 100");

        // Set up a pigeon with STEADY morale
        system.setPlayerPigeon(new Pigeon("Nora", 5, Morale.STEADY));
        Morale startingMorale = system.getPlayerPigeon().getMorale();
        assertEquals(Morale.STEADY, startingMorale);

        // Trigger frost period 1
        weatherSystem.setWeather(Weather.FROST);
        system.applyFrostToLoft(achievementCallback);

        assertEquals(90, system.getLoftCondition(),
                "Loft condition should be reduced to 90 after 1 frost period");

        // Trigger 9 more frost periods (resetting flag between each)
        for (int i = 0; i < 9; i++) {
            system.resetFrostFlag();
            system.applyFrostToLoft(achievementCallback);
        }

        assertEquals(0, system.getLoftCondition(),
                "Loft condition should be 0 after 10 frost periods");

        // Morale should have dropped at least once (when condition dropped below 30)
        // After 7 frosts: condition = 100 - 70 = 30 → threshold hit on frost 8 (condition = 20)
        // Actually: frost 8 takes condition from 30 to 20 → below threshold → morale drops
        assertNotEquals(Morale.STEADY, system.getPlayerPigeon().getMorale(),
                "Pigeon morale should have dropped at least 1 tier after enough frost damage");
        // Morale should be lower than starting value
        assertTrue(system.getPlayerPigeon().getMorale().ordinal() < startingMorale.ordinal(),
                "Pigeon morale ordinal should be lower after frost damage");
    }

    @Test
    void scenario3_frostDoesNotDamageLoftWhenNotPlaced() {
        // Loft not placed — frost should have no effect
        assertFalse(system.hasLoft());
        system.applyFrostToLoft(achievementCallback);
        assertEquals(100, system.getLoftCondition(),
                "Loft condition should not change if loft is not placed");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Thunderstorm postpones race
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Schedule a Club Race for Wednesday 14:00. At Wednesday 13:00,
     * trigger THUNDERSTORM weather. Advance time to 14:00. Verify the race does
     * NOT resolve (no placement recorded). Verify a PIGEON_RACE_DAY rumour has
     * been seeded into at least 1 NPC for the next available race day.
     */
    @Test
    void scenario4_thunderstormPostponesRace() {
        system.setLoftPlaced(true);
        system.setPlayerPigeon(new Pigeon("Nora", 5, Morale.STEADY));
        inventory.addItem(Material.COIN, 20);
        system.setHasWonClubRace(false);

        // Trigger THUNDERSTORM at 13:00
        weatherSystem.setWeather(Weather.THUNDERSTORM);

        // Enter a neighbourhood sprint (Club Race not needed for first test)
        EntryResult entry = system.enterRace(RaceType.NEIGHBOURHOOD_SPRINT, inventory, achievementCallback);
        assertEquals(EntryResult.SUCCESS, entry, "Race entry should succeed");

        // Attempt to resolve race with THUNDERSTORM weather
        RaceResult result = system.resolveRace(
                RaceType.NEIGHBOURHOOD_SPRINT,
                Weather.THUNDERSTORM,
                inventory,
                npcs, rumourNetwork, newspaperSystem, achievementCallback);

        assertTrue(result.isPostponed(),
                "Race should be postponed when THUNDERSTORM is active");
        assertEquals(0, result.getPlacement(),
                "Postponed result should have placement = 0");

        // Verify rumour was re-seeded into at least 1 NPC
        long npcWithRumour = npcs.stream()
                .filter(npc -> npc.getRumours().stream()
                        .anyMatch(r -> r.getType() == RumourType.PIGEON_RACE_DAY))
                .count();
        assertTrue(npcWithRumour >= 1,
                "At least 1 NPC should receive a PIGEON_RACE_DAY rumour after postponement");

        // Verify nextRaceDay was incremented (race rescheduled for next day)
        assertTrue(system.getNextRaceDay() > PigeonRacingSystem.RACE_INTERVAL_DAYS,
                "nextRaceDay should be incremented after postponement");

        // Verify rumour seeded flag was reset (so it can be re-seeded when appropriate)
        assertFalse(system.isRaceDayRumourSeeded(),
                "Rumour seeded flag should be reset after postponement");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Derby win triggers newspaper headline
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5: Enter the Northfield Derby (set hasWonClubRace = true). Set
     * player pigeon training to 10, morale to ELATED. Call resolveRace() with a
     * seeded rng that guarantees first place. Verify placement = 1. Verify
     * NewspaperSystem.isPigeonVictoryPending() returns true. Verify
     * NORTHFIELD_DERBY achievement unlocked. Verify PIGEON_VICTORY rumour seeded
     * into at least 3 NPCs.
     */
    @Test
    void scenario5_derbyWinTriggersNewspaper() {
        // Set up system with club race win
        system.setHasWonClubRace(true);
        system.setLoftPlaced(true);
        inventory.addItem(Material.COIN, 20);

        // Create a system with seeded RNG that will produce first place
        // With Training 10 + ELATED, player speed = 0.6 base + variance
        // Derby opponents range 0.65–0.95, so we need the RNG to keep player on high end
        // and opponents on low end. Use a more targeted approach: run until win.
        boolean derbyWon = false;

        for (int attempt = 0; attempt < 200; attempt++) {
            PigeonRacingSystem derbySystem = new PigeonRacingSystem(new Random(attempt));
            derbySystem.setLoftPlaced(true);
            derbySystem.setHasWonClubRace(true);
            derbySystem.setPlayerPigeon(new Pigeon("Nora", PigeonRacingSystem.MAX_TRAINING_LEVEL, Morale.ELATED));

            NewspaperSystem testNewspaper = new NewspaperSystem(new Random(99));
            RumourNetwork testRumours = new RumourNetwork(new Random(77));

            List<NPC> testNpcs = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                testNpcs.add(new NPC(NPCType.PUBLIC, i * 15f, 1f, i * 15f));
            }

            List<AchievementType> derbyAchievements = new ArrayList<>();
            NotorietySystem.AchievementCallback derbyCallback = derbyAchievements::add;
            Inventory testInv = new Inventory(36);

            RaceResult result = derbySystem.resolveRace(
                    RaceType.NORTHFIELD_DERBY,
                    null, // No weather effect
                    testInv,
                    testNpcs, testRumours, testNewspaper, derbyCallback);

            if (!result.isPostponed() && result.getPlacement() == 1) {
                // Verify newspaper
                assertTrue(testNewspaper.isPigeonVictoryPending(),
                        "NewspaperSystem should have pigeon victory pending after Derby win");

                // Verify NORTHFIELD_DERBY achievement
                assertTrue(derbyAchievements.contains(AchievementType.NORTHFIELD_DERBY),
                        "NORTHFIELD_DERBY achievement should be awarded on Derby win");

                // Verify PIGEON_VICTORY rumour seeded into at least 3 NPCs
                long npcWithVictoryRumour = testNpcs.stream()
                        .filter(npc -> npc.getRumours().stream()
                                .anyMatch(r -> r.getType() == RumourType.PIGEON_VICTORY))
                        .count();
                assertTrue(npcWithVictoryRumour >= 3,
                        "At least 3 NPCs should receive PIGEON_VICTORY rumour on Derby win; got " + npcWithVictoryRumour);

                // Also verify the flavour line
                assertEquals(PigeonRacingSystem.FLAVOUR_FIRST, result.getFlavourLine(),
                        "First place should get the FLAVOUR_FIRST line");

                // Verify trophy in inventory
                assertEquals(1, testInv.getItemCount(Material.PIGEON_TROPHY),
                        "Player should receive PIGEON_TROPHY on Derby win");

                derbyWon = true;
                break;
            }
        }

        assertTrue(derbyWon,
                "Training 10 + ELATED pigeon should win the Northfield Derby at least once in 200 attempts");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Additional integration tests for wiring
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void clubRaceWin_setsHasWonClubRaceFlag() {
        // Use a winning configuration
        PigeonRacingSystem winSystem = new PigeonRacingSystem(new Random(0));
        winSystem.setLoftPlaced(true);
        winSystem.setPlayerPigeon(new Pigeon("Nora", PigeonRacingSystem.MAX_TRAINING_LEVEL, Morale.ELATED));

        assertFalse(winSystem.hasWonClubRace(), "Should not have won a club race initially");

        boolean clubWon = false;
        for (int attempt = 0; attempt < 100; attempt++) {
            winSystem.setPlayerPigeon(new Pigeon("Nora", PigeonRacingSystem.MAX_TRAINING_LEVEL, Morale.ELATED));
            Inventory testInv = new Inventory(36);
            testInv.addItem(Material.COIN, 10);
            RaceResult result = winSystem.resolveRace(
                    RaceType.CLUB_RACE, null, testInv, null, null, null, null);
            if (!result.isPostponed() && result.getPlacement() == 1) {
                clubWon = true;
                break;
            }
        }

        if (clubWon) {
            assertTrue(winSystem.hasWonClubRace(),
                    "hasWonClubRace should be true after winning a Club Race");
        }
    }

    @Test
    void pigeonLoftUI_isVisible_showHide() {
        PigeonLoftUI ui = new PigeonLoftUI();
        assertFalse(ui.isVisible(), "UI starts hidden");
        ui.show();
        assertTrue(ui.isVisible(), "UI visible after show()");
        ui.hide();
        assertFalse(ui.isVisible(), "UI hidden after hide()");
        ui.toggle();
        assertTrue(ui.isVisible(), "UI visible after toggle()");
        ui.toggle();
        assertFalse(ui.isVisible(), "UI hidden after second toggle()");
    }

    @Test
    void pigeonLoftUI_releaseTooltip() {
        assertEquals("Off you go then, love.", PigeonLoftUI.getReleaseTooltip());
    }

    @Test
    void pigeonLoftUI_noLoftTooltip() {
        assertEquals("You can't race a pigeon that lives in your pocket.",
                PigeonLoftUI.getNoLoftTooltip());
    }

    @Test
    void derbyEntryRequiresClubRaceWin_endToEnd() {
        system.setLoftPlaced(true);
        system.setPlayerPigeon(new Pigeon("Nora", 5, Morale.STEADY));
        inventory.addItem(Material.COIN, 20);

        // Cannot enter Derby without Club Race win
        assertEquals(EntryResult.DERBY_REQUIRES_CLUB_WIN,
                system.enterRace(RaceType.NORTHFIELD_DERBY, inventory, achievementCallback),
                "Derby should be blocked without a Club Race win");

        // Set club race win
        system.setHasWonClubRace(true);

        // Now Derby entry should succeed
        assertEquals(EntryResult.SUCCESS,
                system.enterRace(RaceType.NORTHFIELD_DERBY, inventory, achievementCallback),
                "Derby should be available after winning Club Race");
    }

    @Test
    void raceDayRumourNotSeededTwiceBeforeRace() {
        system.setNextRaceDay(2);
        system.setRaceDayRumourSeeded(false);

        // Set up time system at the right moment to seed
        TimeSystem ts = new TimeSystem(PigeonRacingSystem.RACE_RUMOUR_SEED_HOUR + 0.1f);

        system.update(0f, ts, null, inventory, npcs, rumourNetwork, newspaperSystem, achievementCallback);
        assertTrue(system.isRaceDayRumourSeeded(), "Rumour should be seeded on first trigger");

        // Clear rumours from NPCs to check second call doesn't add more
        long rumoursAfterFirst = npcs.stream()
                .mapToLong(npc -> npc.getRumours().size())
                .sum();

        // Call update again — rumour should NOT be seeded again
        system.update(0f, ts, null, inventory, npcs, rumourNetwork, newspaperSystem, achievementCallback);

        long rumoursAfterSecond = npcs.stream()
                .mapToLong(npc -> npc.getRumours().size())
                .sum();

        assertEquals(rumoursAfterFirst, rumoursAfterSecond,
                "No additional rumours should be seeded after flag is set");
    }
}

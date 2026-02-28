package ragamuffin.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.*;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 8k Integration Tests — Allotment System (Issue #914).
 *
 * <p>Implements the 5 exact scenarios from SPEC.md Phase 8k.
 */
class Phase8kIntegrationTest {

    @BeforeAll
    static void setup() {
        HeadlessTestHelper.initHeadless();
    }

    // ─── Test 1: Plot claim and crop growth cycle ─────────────────────────────

    /**
     * Plot claim and crop growth cycle:
     * Spawn ALLOTMENT_WARDEN NPC near the ALLOTMENTS landmark. Call
     * allotmentSystem.claimPlot(player, warden). Verify PLOT_DEED is in player
     * inventory. Set a DIRT block in the player's plot; call
     * allotmentSystem.plantCrop(player, blockPos, Material.CARROT_SEED). Advance
     * the TimeSystem by 10 in-game minutes. Call allotmentSystem.update() each frame.
     * Verify the crop block reaches stage 2 (fully grown). Call
     * allotmentSystem.harvestCrop(player, blockPos, inventory). Verify at least 2
     * CARROT items in player inventory.
     */
    @Test
    void plotClaimAndCropGrowthCycle() {
        // Setup
        Player player = new Player(50, 1, 50);
        Inventory inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 50);
        inventory.addItem(Material.CARROT_SEED, 5);

        NPC warden = new NPC(NPCType.ALLOTMENT_WARDEN, 50, 1, 50);
        AllotmentSystem allotmentSystem = new AllotmentSystem(new Random(42));

        // Claim plot
        AllotmentSystem.ClaimResult claimResult = allotmentSystem.claimPlot(
                player, warden, inventory, 12.0f);
        assertEquals(AllotmentSystem.ClaimResult.SUCCESS, claimResult,
                "Claim should succeed");
        assertTrue(inventory.getItemCount(Material.PLOT_DEED) >= 1,
                "PLOT_DEED should be in player inventory after claiming");

        // Plant carrot seed
        AllotmentSystem.PlantResult plantResult = allotmentSystem.plantCrop(
                player, 10, 10, Material.CARROT_SEED, inventory);
        assertEquals(AllotmentSystem.PlantResult.SUCCESS, plantResult,
                "Planting CARROT_SEED should succeed");

        // Advance 10 in-game minutes (CARROT grow time = 10 min = 600 game-seconds)
        // TimeSystem: 0.1 hours/sec = 6 min/sec real → 10 in-game min = 100 real seconds
        // But we use game-seconds directly in the allotment update
        float carrotGrowTimeSec = AllotmentSystem.CropType.CARROT.getGrowTimeSec(); // 600s
        // Simulate in small frames (60fps = 1/60s per frame)
        float deltaPerFrame = 1.0f / 60.0f;
        float totalTime = 0f;
        while (totalTime < carrotGrowTimeSec + 1f) {
            allotmentSystem.update(deltaPerFrame, 12f, 1, Weather.CLEAR, false, inventory, null);
            totalTime += deltaPerFrame;
        }

        // Verify crop reached stage 2
        AllotmentSystem.CropState cropState = allotmentSystem.getCropState(10, 10);
        assertNotNull(cropState, "Crop state should exist");
        assertEquals(2, cropState.stage, "Crop should be at stage 2 (fully grown)");

        // Harvest
        int carrotsBefore = inventory.getItemCount(Material.CARROT);
        AllotmentSystem.HarvestResult harvestResult = allotmentSystem.harvestCrop(
                player, 10, 10, inventory, 1);
        assertEquals(AllotmentSystem.HarvestResult.SUCCESS, harvestResult,
                "Harvest should succeed");

        int carrots = inventory.getItemCount(Material.CARROT);
        assertTrue(carrots - carrotsBefore >= 2,
                "Harvest of CARROT should yield at least 2 CARROT items (got " +
                (carrots - carrotsBefore) + ")");
    }

    // ─── Test 2: Watering reduces grow time ───────────────────────────────────

    /**
     * Watering reduces grow time:
     * Plant a POTATO_SEED (15-min grow time). Call allotmentSystem.waterCrop().
     * Verify the crop's remaining grow time has been reduced to ≤ 10.5 in-game minutes
     * (30% reduction of 15 = 4.5 min off). Advance time by 10.5 minutes.
     * Verify crop is at stage 2.
     */
    @Test
    void wateringReducesGrowTime() {
        Player player = new Player(50, 1, 50);
        Inventory inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 50);

        NPC warden = new NPC(NPCType.ALLOTMENT_WARDEN, 50, 1, 50);
        AllotmentSystem allotmentSystem = new AllotmentSystem(new Random(42));

        allotmentSystem.claimPlot(player, warden, inventory, 12.0f);
        allotmentSystem.plantCrop(player, 10, 10, Material.POTATO_SEED);

        // Verify initial remaining time
        AllotmentSystem.CropState stateBefore = allotmentSystem.getCropState(10, 10);
        float potatoGrowTime = AllotmentSystem.CropType.POTATO.getGrowTimeSec(); // 900s
        assertEquals(potatoGrowTime, stateBefore.remainingSeconds, 1f,
                "Initial remaining time should be 15 minutes (900 seconds)");

        // Water the crop
        allotmentSystem.waterCrop(player, 10, 10);

        AllotmentSystem.CropState stateAfter = allotmentSystem.getCropState(10, 10);
        float expectedMaxRemaining = potatoGrowTime * (1.0f - AllotmentSystem.WATERING_REDUCTION);
        // 15 min → 10.5 min = 630 seconds
        assertTrue(stateAfter.remainingSeconds <= expectedMaxRemaining + 0.01f,
                "After watering, remaining time should be ≤ 10.5 minutes (630 s), " +
                "got " + stateAfter.remainingSeconds);

        // Advance 10.5 in-game minutes (630 game-seconds + a bit extra)
        float advanceTime = expectedMaxRemaining + 1f;
        float deltaPerFrame = 1.0f / 60.0f;
        float totalTime = 0f;
        while (totalTime < advanceTime) {
            allotmentSystem.update(deltaPerFrame, 12f, 1, Weather.CLEAR, false, inventory, null);
            totalTime += deltaPerFrame;
        }

        AllotmentSystem.CropState finalState = allotmentSystem.getCropState(10, 10);
        assertNotNull(finalState, "Crop should still exist");
        assertEquals(2, finalState.stage,
                "After watering + 10.5 minutes, crop should be at stage 2");
    }

    // ─── Test 3: FROST kills newly planted crop ───────────────────────────────

    /**
     * FROST kills newly planted crop:
     * Set weather to FROST. Plant a CARROT_SEED (stage 0). Call
     * allotmentSystem.update(delta, weather=FROST) for 1 in-game minute.
     * Verify allotmentSystem.isCropKilled(blockPos) returns true. Attempt to
     * harvest — verify 0 CARROT items returned and block resets to DIRT.
     */
    @Test
    void frostKillsNewlyPlantedCrop() {
        Player player = new Player(50, 1, 50);
        Inventory inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 50);

        NPC warden = new NPC(NPCType.ALLOTMENT_WARDEN, 50, 1, 50);
        AllotmentSystem allotmentSystem = new AllotmentSystem(new Random(42));

        allotmentSystem.claimPlot(player, warden, inventory, 12.0f);
        allotmentSystem.plantCrop(player, 10, 10, Material.CARROT_SEED);

        // Verify stage 0
        assertEquals(0, allotmentSystem.getCropState(10, 10).stage,
                "Freshly planted crop should be at stage 0");

        // Update for 1 in-game minute with FROST
        float oneMinute = 60.0f;
        allotmentSystem.update(oneMinute, 12f, 1, Weather.FROST, true, inventory, null);

        // Verify killed
        assertTrue(allotmentSystem.isCropKilled(10, 10),
                "Crop should be killed by FROST at stage 0");

        // Attempt harvest — should return KILLED_BY_FROST with 0 carrots
        int carrotsBefore = inventory.getItemCount(Material.CARROT);
        AllotmentSystem.HarvestResult result = allotmentSystem.harvestCrop(
                player, 10, 10, inventory, 1);
        assertEquals(AllotmentSystem.HarvestResult.KILLED_BY_FROST, result,
                "Harvest result should be KILLED_BY_FROST");
        assertEquals(carrotsBefore, inventory.getItemCount(Material.CARROT),
                "No CARROT items should be added from frost-killed crop");
        assertNull(allotmentSystem.getCropState(10, 10),
                "Crop state should be cleared after harvest of frost-killed crop");
    }

    // ─── Test 4: Repossession after 3 fallow days ─────────────────────────────

    /**
     * Repossession after 3 fallow days:
     * Claim a plot. Advance TimeSystem by 3 full in-game days without planting.
     * Verify allotmentSystem.isRepossessionNoticePending() returns true after day 3.
     * Advance 1 more day. Verify allotmentSystem.hasPlot(player) returns false.
     * Verify PLOT_DEED is no longer in player inventory.
     */
    @Test
    void repossessionAfter3FallowDays() {
        Player player = new Player(50, 1, 50);
        Inventory inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 50);

        NPC warden = new NPC(NPCType.ALLOTMENT_WARDEN, 50, 1, 50);
        AllotmentSystem allotmentSystem = new AllotmentSystem(new Random(42));

        // Claim a plot
        AllotmentSystem.ClaimResult claimResult = allotmentSystem.claimPlot(
                player, warden, inventory, 12.0f);
        assertEquals(AllotmentSystem.ClaimResult.SUCCESS, claimResult);
        assertTrue(allotmentSystem.hasPlot(player));
        assertEquals(1, inventory.getItemCount(Material.PLOT_DEED));

        // Advance 3 in-game days without planting
        allotmentSystem.advanceDay(1, inventory);
        allotmentSystem.advanceDay(2, inventory);
        allotmentSystem.advanceDay(3, inventory);

        // Repossession notice should be pending
        assertTrue(allotmentSystem.isRepossessionNoticePending(),
                "Repossession notice should be pending after 3 fallow days");
        assertTrue(allotmentSystem.hasPlot(player),
                "Player should still have plot (grace period)");

        // Advance 1 more day (grace period expires)
        allotmentSystem.advanceDay(4, inventory);

        // Plot should be repossessed
        assertFalse(allotmentSystem.hasPlot(player),
                "Plot should be repossessed after grace period");
        assertEquals(0, inventory.getItemCount(Material.PLOT_DEED),
                "PLOT_DEED should be removed from inventory on repossession");
    }

    // ─── Test 5: Giant Vegetable Show win pays out ────────────────────────────

    /**
     * Giant Vegetable Show win pays out:
     * Claim plot. Harvest at least 3 POTATO crops. Advance TimeSystem to show time
     * (day 7 at 12:00). Force allotmentSystem.runShow(deterministicRng=playerWins).
     * Verify player receives 15 coins. Verify CHAMPION_GROWER achievement is unlocked.
     * Verify NewspaperSystem.getLastHeadline() contains "VEG SHOW".
     */
    @Test
    void vegShowWinPaysOut() {
        Player player = new Player(50, 1, 50);
        Inventory inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 50);

        NPC warden = new NPC(NPCType.ALLOTMENT_WARDEN, 50, 1, 50);
        AllotmentSystem allotmentSystem = new AllotmentSystem(new Random(42));

        // Set up achievement system to track unlocks
        AchievementSystem achievementSystem = new AchievementSystem();
        allotmentSystem.setAchievementSystem(achievementSystem);

        // Set up newspaper system
        NewspaperSystem newspaperSystem = new NewspaperSystem(new Random(42));
        allotmentSystem.setNewspaperSystem(newspaperSystem);

        // Claim plot
        allotmentSystem.claimPlot(player, warden, inventory, 12.0f);

        // Harvest 3 POTATO crops by planting, growing, and harvesting repeatedly
        float potatoGrowTime = AllotmentSystem.CropType.POTATO.getGrowTimeSec(); // 900s
        float deltaPerFrame = 1.0f / 60.0f;

        for (int i = 0; i < 3; i++) {
            // Plant a potato
            allotmentSystem.plantCrop(player, 10 + i, 10, Material.POTATO_SEED);

            // Grow fully
            float totalTime = 0f;
            while (totalTime < potatoGrowTime + 1f) {
                allotmentSystem.update(deltaPerFrame, 12f, 1, Weather.CLEAR, false,
                        inventory, null);
                totalTime += deltaPerFrame;
            }

            // Harvest
            AllotmentSystem.HarvestResult result = allotmentSystem.harvestCrop(
                    player, 10 + i, 10, inventory, 1);
            assertEquals(AllotmentSystem.HarvestResult.SUCCESS, result,
                    "Harvest " + (i + 1) + " should succeed");
        }

        assertTrue(allotmentSystem.getWeeklyHarvestCount() >= 3,
                "Weekly harvest count should be at least 3");

        // Advance TimeSystem to day 7 at 12:00 (show time)
        assertTrue(allotmentSystem.isShowTime(7, 12.0f), "Day 7 at 12:00 should be show time");

        // Record coins before
        int coinsBefore = inventory.getItemCount(Material.COIN);

        // Run show with RNG ensuring player wins (0.0f < playerFraction for playerWeight≥3)
        boolean won = allotmentSystem.runShow(player, inventory, 7, 0.0f);

        assertTrue(won, "Player should win the Veg Show with deterministic winning RNG");

        // Verify 15 coins awarded
        int coinsAfter = inventory.getItemCount(Material.COIN);
        assertEquals(coinsBefore + AllotmentSystem.VEG_SHOW_WIN_COINS, coinsAfter,
                "Player should receive " + AllotmentSystem.VEG_SHOW_WIN_COINS +
                " coins for winning the Veg Show");

        // Verify CHAMPION_GROWER achievement
        assertTrue(achievementSystem.isUnlocked(AchievementType.CHAMPION_GROWER),
                "CHAMPION_GROWER achievement should be unlocked after winning the Veg Show");

        // Verify newspaper headline contains "VEG SHOW"
        String headline = allotmentSystem.getLastShowHeadline();
        assertNotNull(headline, "Last show headline should be set");
        assertTrue(headline.contains("VEG SHOW"),
                "Headline should contain 'VEG SHOW', got: " + headline);
    }
}

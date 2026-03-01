package ragamuffin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Issue #1057 — CanalSystem.
 *
 * <p>Covers: fishing craft, bite-chance by hour, cast-and-catch, trolley outcome,
 * rod durability, evidence disposal (witnessed and unwitnessed), night-swim warmth
 * drain, dinghy boarding, Derek's sale, and Maureen's fish reward.
 */
class CanalSystemTest {

    private CanalSystem canalSystem;
    private Inventory inventory;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private CanalSystem.AchievementCallback achievementCallback;
    private NotorietySystem.AchievementCallback notorietyAchievementCallback;
    private TimeSystem timeSystem;
    private Player player;
    private RumourNetwork rumourNetwork;

    @BeforeEach
    void setUp() {
        canalSystem = new CanalSystem(new Random(42));
        inventory = new Inventory(36);
        notorietySystem = new NotorietySystem();
        wantedSystem = new WantedSystem(new Random(1));
        criminalRecord = new CriminalRecord();
        achievementSystem = new AchievementSystem();
        achievementCallback = type -> achievementSystem.unlock(type);
        notorietyAchievementCallback = type -> achievementSystem.unlock(type);
        timeSystem = new TimeSystem(10.0f);
        player = new Player(0f, 1f, 0f);
        rumourNetwork = new RumourNetwork(new Random(99));

        canalSystem.setNotorietySystem(notorietySystem);
        canalSystem.setWantedSystem(wantedSystem);
        canalSystem.setCriminalRecord(criminalRecord);
        canalSystem.setRumourNetwork(rumourNetwork);
    }

    // ── Fishing crafting ──────────────────────────────────────────────────────

    @Test
    void craftFishingRod_withCorrectMaterials_succeeds() {
        inventory.addItem(Material.WOOD, 2);
        inventory.addItem(Material.STRING_ITEM, 1);

        boolean result = canalSystem.craftFishingRod(inventory);

        assertTrue(result, "Should successfully craft a fishing rod");
        assertEquals(1, inventory.getItemCount(Material.FISHING_ROD));
        assertEquals(0, inventory.getItemCount(Material.WOOD));
        assertEquals(0, inventory.getItemCount(Material.STRING_ITEM));
    }

    @Test
    void craftFishingRod_withInsufficientWood_fails() {
        inventory.addItem(Material.WOOD, 1); // Only 1, needs 2
        inventory.addItem(Material.STRING_ITEM, 1);

        boolean result = canalSystem.craftFishingRod(inventory);

        assertFalse(result, "Should not craft with insufficient wood");
        assertEquals(0, inventory.getItemCount(Material.FISHING_ROD));
    }

    @Test
    void craftFishingRod_withNoString_fails() {
        inventory.addItem(Material.WOOD, 2);
        // No STRING_ITEM

        boolean result = canalSystem.craftFishingRod(inventory);

        assertFalse(result, "Should not craft without string");
        assertEquals(0, inventory.getItemCount(Material.FISHING_ROD));
    }

    // ── Bite chance by hour ────────────────────────────────────────────────────

    @Test
    void biteChance_atDawn_is60Percent() {
        float chance = canalSystem.getBiteChanceForHour(6.0f); // 06:00 — dawn
        assertEquals(CanalSystem.BITE_CHANCE_DAWN, chance, 0.001f,
                "Dawn bite chance should be 60%");
    }

    @Test
    void biteChance_atNoon_is25Percent() {
        float chance = canalSystem.getBiteChanceForHour(13.0f); // 13:00 — noon
        assertEquals(CanalSystem.BITE_CHANCE_NOON, chance, 0.001f,
                "Noon bite chance should be 25%");
    }

    @Test
    void biteChance_atNight_is10Percent() {
        float chance = canalSystem.getBiteChanceForHour(23.0f); // 23:00 — night
        assertEquals(CanalSystem.BITE_CHANCE_NIGHT, chance, 0.001f,
                "Night bite chance should be 10%");
    }

    @Test
    void biteChance_atMidnight_is10Percent() {
        float chance = canalSystem.getBiteChanceForHour(1.0f); // 01:00 — still night
        assertEquals(CanalSystem.BITE_CHANCE_NIGHT, chance, 0.001f,
                "Early morning bite chance should be 10%");
    }

    // ── Cast and catch ─────────────────────────────────────────────────────────

    @Test
    void castRod_withoutRod_returnsNoRod() {
        // No rod in inventory
        CanalSystem.CastResult result = canalSystem.castRod(timeSystem, inventory);
        assertEquals(CanalSystem.CastResult.NO_ROD, result);
    }

    @Test
    void castRod_withRod_returnsCastOk() {
        inventory.addItem(Material.FISHING_ROD, 1);
        CanalSystem.CastResult result = canalSystem.castRod(timeSystem, inventory);
        assertEquals(CanalSystem.CastResult.CAST_OK, result);
        assertTrue(canalSystem.isCasting());
    }

    @Test
    void castRod_whenAlreadyCasting_returnsAlreadyCasting() {
        inventory.addItem(Material.FISHING_ROD, 1);
        canalSystem.castRod(timeSystem, inventory); // First cast
        CanalSystem.CastResult second = canalSystem.castRod(timeSystem, inventory);
        assertEquals(CanalSystem.CastResult.ALREADY_CASTING, second);
    }

    @Test
    void reelIn_withBiteReady_yieldsCatch() {
        inventory.addItem(Material.FISHING_ROD, 1);
        canalSystem.castRod(timeSystem, inventory);
        canalSystem.setBiteReadyForTesting(true);

        CanalSystem.ReelResult result = canalSystem.reelIn(inventory, achievementCallback);

        assertTrue(result == CanalSystem.ReelResult.FISH
                        || result == CanalSystem.ReelResult.TROLLEY
                        || result == CanalSystem.ReelResult.FISH_ROD_BROKEN
                        || result == CanalSystem.ReelResult.TROLLEY_ROD_BROKEN,
                "Should yield a catch when bite is ready");
        // Should not still be casting after reeling in
        assertFalse(canalSystem.isCasting());
    }

    @Test
    void reelIn_withNoBite_returnsNoBite() {
        inventory.addItem(Material.FISHING_ROD, 1);
        canalSystem.castRod(timeSystem, inventory);
        // Don't set bite ready

        CanalSystem.ReelResult result = canalSystem.reelIn(inventory, achievementCallback);

        assertEquals(CanalSystem.ReelResult.NO_BITE, result);
    }

    @Test
    void reelIn_notCasting_returnsNotCasting() {
        CanalSystem.ReelResult result = canalSystem.reelIn(inventory, achievementCallback);
        assertEquals(CanalSystem.ReelResult.NOT_CASTING, result);
    }

    // ── Trolley outcome ────────────────────────────────────────────────────────

    @Test
    void reelIn_trolleyOutcome_grantsAchievement() {
        // Use seeded random that gives trolley: find a seed that produces < 0.15
        CanalSystem systemWithSeed = new CanalSystem(new Random(0)); // test with seed 0
        systemWithSeed.setRumourNetwork(rumourNetwork);
        // Manually force trolley by checking TROLLEY_FISHERMAN achievement across many reels
        int trolleyCount = 0;
        for (int seed = 0; seed < 100; seed++) {
            AchievementSystem as = new AchievementSystem();
            CanalSystem cs = new CanalSystem(new Random(seed));
            Inventory inv = new Inventory(36);
            inv.addItem(Material.FISHING_ROD, 1);
            cs.castRod(timeSystem, inv);
            cs.setBiteReadyForTesting(true);
            CanalSystem.ReelResult r = cs.reelIn(inv, type -> as.unlock(type));
            if (r == CanalSystem.ReelResult.TROLLEY || r == CanalSystem.ReelResult.TROLLEY_ROD_BROKEN) {
                trolleyCount++;
                assertTrue(as.isUnlocked(AchievementType.TROLLEY_FISHERMAN),
                        "TROLLEY_FISHERMAN achievement should be awarded on trolley catch");
            }
        }
        // With 15% chance and 100 trials, expect at least a few trolleys
        assertTrue(trolleyCount > 0, "Should have caught at least one trolley across 100 seeded trials");
    }

    @Test
    void reelIn_fishOutcome_grantsAchievement() {
        // With most seeds, a fish will be caught (85% probability)
        boolean gotFish = false;
        for (int seed = 0; seed < 20; seed++) {
            AchievementSystem as = new AchievementSystem();
            CanalSystem cs = new CanalSystem(new Random(seed));
            Inventory inv = new Inventory(36);
            inv.addItem(Material.FISHING_ROD, 1);
            cs.castRod(timeSystem, inv);
            cs.setBiteReadyForTesting(true);
            CanalSystem.ReelResult r = cs.reelIn(inv, type -> as.unlock(type));
            if (r == CanalSystem.ReelResult.FISH || r == CanalSystem.ReelResult.FISH_ROD_BROKEN) {
                gotFish = true;
                assertTrue(as.isUnlocked(AchievementType.CANAL_CATCH),
                        "CANAL_CATCH achievement should be awarded on fish catch");
                break;
            }
        }
        assertTrue(gotFish, "Should have caught at least one fish across 20 seeded trials");
    }

    // ── Rod durability ─────────────────────────────────────────────────────────

    @Test
    void rodDurability_decreasesOnReelIn() {
        inventory.addItem(Material.FISHING_ROD, 1);
        canalSystem.castRod(timeSystem, inventory);
        canalSystem.setBiteReadyForTesting(true);
        int durabilityBefore = canalSystem.getRodDurability();

        canalSystem.reelIn(inventory, achievementCallback);

        assertEquals(durabilityBefore - 1, canalSystem.getRodDurability(),
                "Durability should decrease by 1 per reel-in");
    }

    @Test
    void rod_breaksAtZeroDurability() {
        inventory.addItem(Material.FISHING_ROD, 1);
        canalSystem.setRodDurabilityForTesting(1); // Last use
        canalSystem.castRod(timeSystem, inventory);
        canalSystem.setBiteReadyForTesting(true);

        CanalSystem.ReelResult result = canalSystem.reelIn(inventory, achievementCallback);

        assertTrue(result == CanalSystem.ReelResult.FISH_ROD_BROKEN
                        || result == CanalSystem.ReelResult.TROLLEY_ROD_BROKEN,
                "Rod should break on last use");
        assertEquals(0, inventory.getItemCount(Material.FISHING_ROD),
                "Rod should be removed from inventory when broken");
        assertEquals(0, canalSystem.getRodDurability());
    }

    @Test
    void castRod_withBrokenRod_returnsRodBroken() {
        inventory.addItem(Material.FISHING_ROD, 1);
        canalSystem.setRodDurabilityForTesting(0);

        CanalSystem.CastResult result = canalSystem.castRod(timeSystem, inventory);

        assertEquals(CanalSystem.CastResult.ROD_BROKEN, result);
    }

    // ── Evidence disposal (unwitnessed) ────────────────────────────────────────

    @Test
    void disposeEvidence_unwitnessed_cctvTape_succeeds() {
        inventory.addItem(Material.CCTV_TAPE, 1);
        criminalRecord.record(CriminalRecord.CrimeType.WITNESSED_CRIMES);
        NPC rumourHolder = new NPC(NPCType.PUBLIC, 5f, 1f, 5f);

        CanalSystem.EvidenceDisposalResult result = canalSystem.disposeEvidence(
                inventory, false, 0f, 0f, 0f,
                rumourHolder, achievementCallback, notorietyAchievementCallback);

        assertEquals(CanalSystem.EvidenceDisposalResult.SUCCESS, result);
        assertEquals(0, inventory.getItemCount(Material.CCTV_TAPE),
                "CCTV tape should be consumed");
        assertTrue(achievementSystem.isUnlocked(AchievementType.EVIDENCE_IN_THE_CUT),
                "EVIDENCE_IN_THE_CUT achievement should be awarded");
        // Criminal record should have been cleared
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.WITNESSED_CRIMES),
                "Witnessed crime count should be reduced by clearOne");
    }

    @Test
    void disposeEvidence_unwitnessed_reducesNotoriety() {
        inventory.addItem(Material.BLOODY_HOODIE, 1);
        notorietySystem.addNotoriety(10, notorietyAchievementCallback);
        int notorietyBefore = notorietySystem.getNotoriety();

        canalSystem.disposeEvidence(inventory, false, 0f, 0f, 0f,
                null, achievementCallback, notorietyAchievementCallback);

        assertEquals(notorietyBefore - CanalSystem.EVIDENCE_DISPOSAL_NOTORIETY_REDUCTION,
                notorietySystem.getNotoriety(),
                "Notoriety should be reduced by 1 on successful disposal");
    }

    // ── Evidence disposal (witnessed) ──────────────────────────────────────────

    @Test
    void disposeEvidence_witnessed_addsWantedStar() {
        inventory.addItem(Material.CCTV_TAPE, 1);

        CanalSystem.EvidenceDisposalResult result = canalSystem.disposeEvidence(
                inventory, true, 0f, 0f, 0f,
                null, achievementCallback, notorietyAchievementCallback);

        assertEquals(CanalSystem.EvidenceDisposalResult.WITNESSED, result);
        assertEquals(1, wantedSystem.getWantedStars(),
                "Witnessed evidence disposal should add 1 wanted star");
        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.EVIDENCE_DESTRUCTION),
                "EVIDENCE_DESTRUCTION should be recorded when witnessed");
        // Item should NOT be consumed
        assertEquals(1, inventory.getItemCount(Material.CCTV_TAPE),
                "Evidence should not be consumed if witnessed");
    }

    @Test
    void disposeEvidence_noEvidence_returnsNoEvidence() {
        // Inventory has no disposable evidence
        CanalSystem.EvidenceDisposalResult result = canalSystem.disposeEvidence(
                inventory, false, 0f, 0f, 0f,
                null, achievementCallback, notorietyAchievementCallback);

        assertEquals(CanalSystem.EvidenceDisposalResult.NO_EVIDENCE, result);
    }

    // ── Night swim warmth drain ────────────────────────────────────────────────

    @Test
    void nightSwim_inWater_drainsWarmthAndHealth() {
        TimeSystem night = new TimeSystem(22.0f);
        Player swimmer = new Player(0f, 0f, 0f);
        float initialWarmth = swimmer.getWarmth();
        float initialHealth = swimmer.getHealth();

        // Simulate 1 second of swimming
        canalSystem.update(1.0f, night, swimmer, inventory, true, true,
                new ArrayList<>(), achievementCallback);

        assertTrue(swimmer.getWarmth() < initialWarmth,
                "Warmth should decrease while swimming");
        assertTrue(swimmer.getHealth() < initialHealth,
                "Health should decrease while swimming");

        // Check specific drain rates
        float expectedWarmthDrain = CanalSystem.SWIM_WARMTH_DRAIN_PER_SECOND * 1.0f;
        float expectedHealthDrain = CanalSystem.SWIM_HEALTH_DRAIN_PER_SECOND * 1.0f;

        assertEquals(initialWarmth - expectedWarmthDrain, swimmer.getWarmth(), 0.1f,
                "Warmth drain should match SWIM_WARMTH_DRAIN_PER_SECOND");
        assertEquals(initialHealth - expectedHealthDrain, swimmer.getHealth(), 0.1f,
                "Health drain should match SWIM_HEALTH_DRAIN_PER_SECOND");
    }

    @Test
    void nightSwim_afterThreshold_appliesGrimeDebuff() {
        TimeSystem night = new TimeSystem(22.0f);
        Player swimmer = new Player(0f, 0f, 0f);

        // Set swim timer past the grime threshold
        canalSystem.setSwimTimerForTesting(CanalSystem.SWIM_GRIME_THRESHOLD_SECONDS + 1f);

        // Simulate exiting water (playerInWater = false)
        canalSystem.update(0.016f, night, swimmer, inventory, true, false,
                new ArrayList<>(), achievementCallback);

        assertTrue(canalSystem.isGrimeDebuffActive(),
                "Grime debuff should be active after swimming over threshold");
        assertTrue(canalSystem.getGrimeDebuffTimer() > 0f,
                "Grime debuff timer should be set");
    }

    @Test
    void nightSwim_achievement_awardedAfter20() {
        TimeSystem night = new TimeSystem(21.0f); // 21:00
        Player swimmer = new Player(0f, 0f, 0f);

        // Enter water for the first time
        canalSystem.update(0.016f, night, swimmer, inventory, true, true,
                new ArrayList<>(), achievementCallback);

        assertTrue(achievementSystem.isUnlocked(AchievementType.NIGHT_SWIMMER),
                "NIGHT_SWIMMER achievement should be awarded when entering water after 20:00");
    }

    @Test
    void nightSwim_achievement_notAwardedBefore20() {
        TimeSystem day = new TimeSystem(14.0f); // 14:00
        Player swimmer = new Player(0f, 0f, 0f);

        canalSystem.update(0.016f, day, swimmer, inventory, true, true,
                new ArrayList<>(), achievementCallback);

        assertFalse(achievementSystem.isUnlocked(AchievementType.NIGHT_SWIMMER),
                "NIGHT_SWIMMER achievement should NOT be awarded before 20:00");
    }

    // ── Dinghy boarding ────────────────────────────────────────────────────────

    @Test
    void boardDinghy_withDinghyInInventory_succeeds() {
        inventory.addItem(Material.DINGHY, 1);
        boolean result = canalSystem.boardDinghy(inventory);

        assertTrue(result, "Should board dinghy when DINGHY is in inventory");
        assertTrue(canalSystem.isOnDinghy());
        assertTrue(canalSystem.getDinghyTimer() > 0f);
    }

    @Test
    void boardDinghy_withoutDinghy_fails() {
        boolean result = canalSystem.boardDinghy(inventory);

        assertFalse(result, "Should not board dinghy without DINGHY item");
        assertFalse(canalSystem.isOnDinghy());
    }

    @Test
    void exitDinghy_removesDinghyFromInventory() {
        inventory.addItem(Material.DINGHY, 1);
        canalSystem.boardDinghy(inventory);

        boolean result = canalSystem.exitDinghy(inventory);

        assertTrue(result, "Should successfully exit dinghy");
        assertFalse(canalSystem.isOnDinghy());
        assertEquals(0, inventory.getItemCount(Material.DINGHY),
                "DINGHY should be removed on deflation");
    }

    @Test
    void dinghy_deflatesAfterMaxDuration() {
        inventory.addItem(Material.DINGHY, 1);
        canalSystem.boardDinghy(inventory);

        // Simulate the dinghy duration expiring
        Player swimmer = new Player(0f, 0f, 0f);
        canalSystem.update(CanalSystem.DINGHY_MAX_DURATION_SECONDS + 1f,
                timeSystem, swimmer, inventory, true, true,
                new ArrayList<>(), achievementCallback);

        assertFalse(canalSystem.isOnDinghy(), "Dinghy should deflate after max duration");
    }

    @Test
    void dinghy_preventsSwimDrain() {
        inventory.addItem(Material.DINGHY, 1);
        canalSystem.boardDinghy(inventory);

        Player swimmer = new Player(0f, 0f, 0f);
        float initialWarmth = swimmer.getWarmth();
        float initialHealth = swimmer.getHealth();

        // Simulate being on dinghy in water for 1 second
        canalSystem.update(1.0f, timeSystem, swimmer, inventory, true, true,
                new ArrayList<>(), achievementCallback);

        assertEquals(initialWarmth, swimmer.getWarmth(), 0.001f,
                "Dinghy should prevent warmth drain while on water");
        assertEquals(initialHealth, swimmer.getHealth(), 0.001f,
                "Dinghy should prevent health drain while on water");
    }

    // ── Derek's sale ──────────────────────────────────────────────────────────

    @Test
    void buyDinghyFromDerek_duringActiveHours_withEnoughCoin_succeeds() {
        TimeSystem morning = new TimeSystem(10.0f); // Derek active 07:00–22:00
        inventory.addItem(Material.COIN, CanalSystem.DINGHY_COST_COIN);

        CanalSystem.DerekSaleResult result = canalSystem.buyDinghyFromDerek(inventory, morning);

        assertEquals(CanalSystem.DerekSaleResult.SUCCESS, result);
        assertEquals(1, inventory.getItemCount(Material.DINGHY));
        assertEquals(0, inventory.getItemCount(Material.COIN));
    }

    @Test
    void buyDinghyFromDerek_outsideActiveHours_returnsDerekNotHere() {
        TimeSystem night = new TimeSystem(23.0f); // Derek inactive after 22:00
        inventory.addItem(Material.COIN, CanalSystem.DINGHY_COST_COIN);

        CanalSystem.DerekSaleResult result = canalSystem.buyDinghyFromDerek(inventory, night);

        assertEquals(CanalSystem.DerekSaleResult.DEREK_NOT_HERE, result);
        assertEquals(0, inventory.getItemCount(Material.DINGHY));
    }

    @Test
    void buyDinghyFromDerek_withInsufficientCoin_returnsInsufficientFunds() {
        TimeSystem morning = new TimeSystem(10.0f);
        inventory.addItem(Material.COIN, CanalSystem.DINGHY_COST_COIN - 1);

        CanalSystem.DerekSaleResult result = canalSystem.buyDinghyFromDerek(inventory, morning);

        assertEquals(CanalSystem.DerekSaleResult.INSUFFICIENT_FUNDS, result);
        assertEquals(0, inventory.getItemCount(Material.DINGHY));
    }

    // ── Maureen's fish reward ──────────────────────────────────────────────────

    @Test
    void tradeFishToMaureen_duringActiveHours_withEnoughFish_awardsCoin() {
        TimeSystem midMorning = new TimeSystem(11.0f); // Maureen active 09:00–20:00
        inventory.addItem(Material.CANAL_FISH, CanalSystem.MAUREEN_FISH_BATCH);

        int coinsEarned = canalSystem.tradeFishToMaureen(inventory, midMorning);

        assertEquals(CanalSystem.MAUREEN_FISH_REWARD_COIN, coinsEarned,
                "Maureen should award 5 COIN for 3 fish");
        assertEquals(0, inventory.getItemCount(Material.CANAL_FISH),
                "Fish should be consumed in the trade");
        assertEquals(CanalSystem.MAUREEN_FISH_REWARD_COIN, inventory.getItemCount(Material.COIN));
    }

    @Test
    void tradeFishToMaureen_outsideActiveHours_returnsZero() {
        TimeSystem night = new TimeSystem(21.0f); // Maureen inactive after 20:00
        inventory.addItem(Material.CANAL_FISH, CanalSystem.MAUREEN_FISH_BATCH);

        int coinsEarned = canalSystem.tradeFishToMaureen(inventory, night);

        assertEquals(0, coinsEarned, "Maureen should not buy fish outside her active hours");
        assertEquals(CanalSystem.MAUREEN_FISH_BATCH, inventory.getItemCount(Material.CANAL_FISH),
                "Fish should not be consumed if Maureen is absent");
    }

    @Test
    void tradeFishToMaureen_withInsufficientFish_returnsZero() {
        TimeSystem morning = new TimeSystem(11.0f);
        inventory.addItem(Material.CANAL_FISH, CanalSystem.MAUREEN_FISH_BATCH - 1);

        int coinsEarned = canalSystem.tradeFishToMaureen(inventory, morning);

        assertEquals(0, coinsEarned, "Should not trade below minimum batch");
    }

    @Test
    void tradeFishToMaureen_multiBatch_awardsCorrectCoin() {
        TimeSystem morning = new TimeSystem(11.0f);
        int batchCount = 3;
        inventory.addItem(Material.CANAL_FISH, CanalSystem.MAUREEN_FISH_BATCH * batchCount);

        int coinsEarned = canalSystem.tradeFishToMaureen(inventory, morning);

        assertEquals(CanalSystem.MAUREEN_FISH_REWARD_COIN * batchCount, coinsEarned,
                "Should award coins proportionally for multiple batches");
    }

    // ── Time-of-day helpers ────────────────────────────────────────────────────

    @Test
    void pcsoPatrol_activeAt22() {
        assertTrue(canalSystem.isPcsoPatrolActive(22.0f));
    }

    @Test
    void pcsoPatrol_activeAt3() {
        assertTrue(canalSystem.isPcsoPatrolActive(3.0f));
    }

    @Test
    void pcsoPatrol_inactiveAt12() {
        assertFalse(canalSystem.isPcsoPatrolActive(12.0f));
    }

    @Test
    void anglers_presentAt9() {
        assertTrue(canalSystem.areAnglersPresent(9.0f));
    }

    @Test
    void anglers_absentAt22() {
        assertFalse(canalSystem.areAnglersPresent(22.0f));
    }

    @Test
    void drunk_presentAt23() {
        assertTrue(canalSystem.isDrunkPresent(23.0f));
    }

    @Test
    void drunk_absentAt12() {
        assertFalse(canalSystem.isDrunkPresent(12.0f));
    }
}

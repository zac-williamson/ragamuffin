package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.*;
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
 * Integration tests for Issue #1057 — Northfield Canal: Gone Fishing, Towpath Economy
 * &amp; The Cut at Night.
 *
 * <p>Five scenarios:
 * <ol>
 *   <li>Full fishing cycle at dawn — craft rod, cast at dawn (60% bite chance), reel in,
 *       verify CANAL_FISH in inventory, CANAL_CATCH achievement awarded.</li>
 *   <li>Evidence disposal pipeline — player has CCTV_TAPE and WITNESSED_CRIMES record;
 *       disposes in canal unwitnessed; verify item consumed, record cleared, notoriety
 *       reduced, EVIDENCE_IN_THE_CUT achievement awarded.</li>
 *   <li>Nighttime swim with PCSO response — player enters WATER blocks after 20:00;
 *       verify warmth &amp; health drain, NIGHT_SWIMMER achievement, PCSO within range
 *       triggers WantedSystem +1 star.</li>
 *   <li>Dinghy purchase and water traversal — buy DINGHY from Derek (07:00–22:00) for
 *       15 COIN; board dinghy at water edge; verify no warmth/health drain while on water
 *       for full dinghy duration; verify deflation after 120 seconds.</li>
 *   <li>Angler pickpocket economy — Maureen fish reward: trade 3 fish to Maureen
 *       during active hours (09:00–20:00), verify 5 COIN awarded; trade again outside
 *       hours, verify zero COIN.</li>
 * </ol>
 */
class Issue1057CanalSystemTest {

    private CanalSystem canalSystem;
    private TimeSystem timeSystem;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private CanalSystem.AchievementCallback achievementCallback;
    private NotorietySystem.AchievementCallback notorietyCallback;
    private Inventory inventory;
    private Player player;
    private RumourNetwork rumourNetwork;
    private List<NPC> npcs;

    @BeforeEach
    void setUp() {
        // Use fixed seeds for reproducibility
        canalSystem = new CanalSystem(new Random(12345));
        timeSystem = new TimeSystem(6.0f); // 06:00 — dawn
        notorietySystem = new NotorietySystem();
        wantedSystem = new WantedSystem(new Random(99));
        criminalRecord = new CriminalRecord();
        achievementSystem = new AchievementSystem();
        achievementCallback = type -> achievementSystem.unlock(type);
        notorietyCallback = type -> achievementSystem.unlock(type);
        rumourNetwork = new RumourNetwork(new Random(77));
        inventory = new Inventory(36);
        player = new Player(0f, 1f, 0f);
        npcs = new ArrayList<>();

        canalSystem.setNotorietySystem(notorietySystem);
        canalSystem.setWantedSystem(wantedSystem);
        canalSystem.setCriminalRecord(criminalRecord);
        canalSystem.setRumourNetwork(rumourNetwork);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Full fishing cycle at dawn
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: Full fishing cycle at dawn.
     *
     * <p>Craft a FISHING_ROD from 2 WOOD + 1 STRING_ITEM. Verify it appears in inventory.
     * At dawn (06:00), cast the rod (bite chance = 60%). Advance time past the bite window
     * (45 seconds max). Force bite for determinism. Reel in. Verify CANAL_FISH or
     * SHOPPING_TROLLEY_GOLD in inventory and the appropriate achievement is awarded.
     * Verify rod durability decreased by 1.
     */
    @Test
    void scenario1_fullFishingCycle_atDawn() {
        // Step 1: craft a fishing rod
        inventory.addItem(Material.WOOD, 2);
        inventory.addItem(Material.STRING_ITEM, 1);

        boolean crafted = canalSystem.craftFishingRod(inventory);

        assertTrue(crafted, "Should successfully craft a fishing rod");
        assertEquals(1, inventory.getItemCount(Material.FISHING_ROD),
                "FISHING_ROD should be in inventory after crafting");
        assertEquals(0, inventory.getItemCount(Material.WOOD),
                "WOOD should be consumed in crafting");
        assertEquals(0, inventory.getItemCount(Material.STRING_ITEM),
                "STRING_ITEM should be consumed in crafting");

        // Step 2: verify dawn bite chance
        float dawnBiteChance = canalSystem.getBiteChanceForHour(6.0f);
        assertEquals(CanalSystem.BITE_CHANCE_DAWN, dawnBiteChance, 0.001f,
                "Dawn bite chance should be 60%");

        // Step 3: cast the rod at dawn
        CanalSystem.CastResult castResult = canalSystem.castRod(timeSystem, inventory);
        assertEquals(CanalSystem.CastResult.CAST_OK, castResult,
                "Rod should cast successfully");
        assertTrue(canalSystem.isCasting(), "System should be in casting state");

        // Step 4: force bite ready (simulate waiting for bite)
        canalSystem.setBiteReadyForTesting(true);

        // Step 5: reel in
        int durabilityBefore = canalSystem.getRodDurability();
        CanalSystem.ReelResult reelResult = canalSystem.reelIn(inventory, achievementCallback);

        // Verify a catch occurred
        assertTrue(
                reelResult == CanalSystem.ReelResult.FISH
                        || reelResult == CanalSystem.ReelResult.TROLLEY
                        || reelResult == CanalSystem.ReelResult.FISH_ROD_BROKEN
                        || reelResult == CanalSystem.ReelResult.TROLLEY_ROD_BROKEN,
                "Should yield a catch when bite is ready");

        // Verify item in inventory
        int fishCount = inventory.getItemCount(Material.CANAL_FISH);
        int trolleyCount = inventory.getItemCount(Material.SHOPPING_TROLLEY_GOLD);
        assertTrue(fishCount > 0 || trolleyCount > 0,
                "Inventory should contain either CANAL_FISH or SHOPPING_TROLLEY_GOLD");

        // Verify achievement
        boolean canalCatch = achievementSystem.isUnlocked(AchievementType.CANAL_CATCH);
        boolean trolleyFisherman = achievementSystem.isUnlocked(AchievementType.TROLLEY_FISHERMAN);
        assertTrue(canalCatch || trolleyFisherman,
                "Either CANAL_CATCH or TROLLEY_FISHERMAN achievement should be awarded");

        // Verify durability decreased
        assertEquals(durabilityBefore - 1, canalSystem.getRodDurability(),
                "Rod durability should decrease by 1 after reeling in");

        // Verify not casting any more
        assertFalse(canalSystem.isCasting(),
                "Should no longer be casting after reeling in");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Evidence disposal pipeline
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: Evidence disposal pipeline.
     *
     * <p>Player holds CCTV_TAPE. Criminal record has 1 WITNESSED_CRIMES entry.
     * Notoriety starts at 10. Player is unwitnessed (no PCSO nearby). Press E near
     * water: item consumed, WITNESSED_CRIMES count → 0, notoriety → 9,
     * EVIDENCE_IN_THE_CUT achievement awarded, SUSPICIOUS_PERSON rumour seeded.
     *
     * <p>Then repeat with a PCSO nearby (witnessed = true): item NOT consumed, +1
     * wanted star, EVIDENCE_DESTRUCTION recorded.
     */
    @Test
    void scenario2_evidenceDisposalPipeline() {
        // ── Unwitnessed disposal ──────────────────────────────────────────────

        inventory.addItem(Material.CCTV_TAPE, 1);
        criminalRecord.record(CriminalRecord.CrimeType.WITNESSED_CRIMES);
        notorietySystem.addNotoriety(10, notorietyCallback);
        int notorietyBefore = notorietySystem.getNotoriety();

        NPC rumourHolder = new NPC(NPCType.PUBLIC, 5f, 1f, 5f);
        npcs.add(rumourHolder);

        CanalSystem.EvidenceDisposalResult result = canalSystem.disposeEvidence(
                inventory, false,
                0f, 0f, 0f,
                rumourHolder,
                achievementCallback, notorietyCallback);

        // Verify success
        assertEquals(CanalSystem.EvidenceDisposalResult.SUCCESS, result,
                "Unwitnessed evidence disposal should succeed");

        // Verify CCTV_TAPE consumed
        assertEquals(0, inventory.getItemCount(Material.CCTV_TAPE),
                "CCTV_TAPE should be consumed");

        // Verify criminal record cleared
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.WITNESSED_CRIMES),
                "WITNESSED_CRIMES should be cleared by evidence disposal");

        // Verify notoriety reduced
        assertEquals(notorietyBefore - CanalSystem.EVIDENCE_DISPOSAL_NOTORIETY_REDUCTION,
                notorietySystem.getNotoriety(),
                "Notoriety should be reduced by 1");

        // Verify achievement
        assertTrue(achievementSystem.isUnlocked(AchievementType.EVIDENCE_IN_THE_CUT),
                "EVIDENCE_IN_THE_CUT achievement should be awarded");

        // ── Witnessed disposal ────────────────────────────────────────────────

        inventory.addItem(Material.BLOODY_HOODIE, 1);
        int starsBefore = wantedSystem.getWantedStars();
        int evidenceDestructionBefore = criminalRecord.getCount(
                CriminalRecord.CrimeType.EVIDENCE_DESTRUCTION);

        CanalSystem.EvidenceDisposalResult witnessedResult = canalSystem.disposeEvidence(
                inventory, true,
                0f, 0f, 0f,
                null,
                achievementCallback, notorietyCallback);

        assertEquals(CanalSystem.EvidenceDisposalResult.WITNESSED, witnessedResult,
                "Witnessed disposal should return WITNESSED result");

        // Item NOT consumed
        assertEquals(1, inventory.getItemCount(Material.BLOODY_HOODIE),
                "BLOODY_HOODIE should NOT be consumed when witnessed");

        // +1 wanted star
        assertEquals(starsBefore + 1, wantedSystem.getWantedStars(),
                "Witnessed disposal should add 1 wanted star");

        // EVIDENCE_DESTRUCTION recorded
        assertEquals(evidenceDestructionBefore + 1,
                criminalRecord.getCount(CriminalRecord.CrimeType.EVIDENCE_DESTRUCTION),
                "EVIDENCE_DESTRUCTION should be recorded when witnessed");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Nighttime swim with PCSO response
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: Nighttime swim with PCSO response.
     *
     * <p>Set time to 21:00. Simulate player entering WATER blocks. Verify:
     * <ul>
     *   <li>NIGHT_SWIMMER achievement awarded on water entry.</li>
     *   <li>After 1 second in water: warmth reduced by 4, health reduced by 2.</li>
     *   <li>After 30+ seconds: exiting water applies COVERED_IN_GRIME debuff.</li>
     *   <li>PCSO within 20 blocks triggers WantedSystem +1 star.</li>
     * </ul>
     */
    @Test
    void scenario3_nighttimeSwim_withPcsoResponse() {
        TimeSystem night = new TimeSystem(21.0f);
        Player swimmer = new Player(10f, 0f, 10f);
        float initialWarmth = swimmer.getWarmth();
        float initialHealth = swimmer.getHealth();

        // Step 1: enter water at 21:00 — should award NIGHT_SWIMMER
        canalSystem.update(0.016f, night, swimmer, inventory, true, true,
                npcs, achievementCallback);

        assertTrue(achievementSystem.isUnlocked(AchievementType.NIGHT_SWIMMER),
                "NIGHT_SWIMMER achievement should be awarded on entry after 20:00");
        assertTrue(canalSystem.isInWater(), "System should track player as in water");

        // Step 2: simulate 1 full second of swimming
        canalSystem.update(1.0f, night, swimmer, inventory, true, true,
                npcs, achievementCallback);

        float warmthAfter1s = swimmer.getWarmth();
        float healthAfter1s = swimmer.getHealth();

        assertTrue(warmthAfter1s < initialWarmth,
                "Warmth should be lower after swimming");
        assertTrue(healthAfter1s < initialHealth,
                "Health should be lower after swimming");

        // Check drain rates (initial update added ~0.016s drain; total ~1.016s)
        assertTrue(initialWarmth - warmthAfter1s >= CanalSystem.SWIM_WARMTH_DRAIN_PER_SECOND * 0.9f,
                "Warmth drain should be close to SWIM_WARMTH_DRAIN_PER_SECOND per second");
        assertTrue(initialHealth - healthAfter1s >= CanalSystem.SWIM_HEALTH_DRAIN_PER_SECOND * 0.9f,
                "Health drain should be close to SWIM_HEALTH_DRAIN_PER_SECOND per second");

        // Step 3: force swim timer past grime threshold, then exit water
        canalSystem.setSwimTimerForTesting(CanalSystem.SWIM_GRIME_THRESHOLD_SECONDS + 1f);

        // Exit water
        canalSystem.update(0.016f, night, swimmer, inventory, true, false,
                npcs, achievementCallback);

        assertTrue(canalSystem.isGrimeDebuffActive(),
                "COVERED_IN_GRIME debuff should be active after prolonged swim");
        assertEquals(CanalSystem.GRIME_DEBUFF_DURATION_SECONDS,
                canalSystem.getGrimeDebuffTimer(), 1.0f,
                "Grime debuff timer should be set to full duration");

        // Step 4: PCSO nearby triggers wanted star
        // Re-enter water first
        canalSystem.update(0.016f, night, swimmer, inventory, true, true,
                npcs, achievementCallback);

        NPC pcso = new NPC(NPCType.PCSO, 10f, 0f, 15f); // 5 blocks away — within 20
        npcs.add(pcso);

        int starsBefore = wantedSystem.getWantedStars();
        canalSystem.update(0.016f, night, swimmer, inventory, true, true,
                npcs, achievementCallback);

        assertEquals(starsBefore + 1, wantedSystem.getWantedStars(),
                "PCSO within 20 blocks should trigger +1 wanted star for night swimming");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Dinghy purchase and water traversal
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Dinghy purchase and water traversal.
     *
     * <p>Set time to 10:00 (Derek active 07:00–22:00). Give player 15 COIN. Buy
     * DINGHY from Derek. Board dinghy at water edge. Simulate 5 seconds in water
     * while on dinghy — verify NO warmth or health drain. Simulate 120 seconds
     * pass — verify dinghy deflates.
     */
    @Test
    void scenario4_dinghyPurchaseAndWaterTraversal() {
        TimeSystem morning = new TimeSystem(10.0f); // Derek active
        inventory.addItem(Material.COIN, CanalSystem.DINGHY_COST_COIN);

        // Step 1: buy dinghy from Derek
        CanalSystem.DerekSaleResult saleResult =
                canalSystem.buyDinghyFromDerek(inventory, morning);

        assertEquals(CanalSystem.DerekSaleResult.SUCCESS, saleResult,
                "Should successfully buy DINGHY from Derek");
        assertEquals(1, inventory.getItemCount(Material.DINGHY),
                "DINGHY should be in inventory after purchase");
        assertEquals(0, inventory.getItemCount(Material.COIN),
                "Coins should be deducted");

        // Step 2: board the dinghy
        boolean boarded = canalSystem.boardDinghy(inventory);

        assertTrue(boarded, "Should board dinghy successfully");
        assertTrue(canalSystem.isOnDinghy(), "Player should be on dinghy");

        // Step 3: verify no warmth/health drain while on dinghy in water
        Player swimmer = new Player(0f, 0f, 0f);
        float warmthBefore = swimmer.getWarmth();
        float healthBefore = swimmer.getHealth();

        canalSystem.update(5.0f, morning, swimmer, inventory, true, true,
                npcs, achievementCallback);

        assertEquals(warmthBefore, swimmer.getWarmth(), 0.001f,
                "No warmth drain while on dinghy");
        assertEquals(healthBefore, swimmer.getHealth(), 0.001f,
                "No health drain while on dinghy");
        assertTrue(canalSystem.isOnDinghy(), "Should still be on dinghy after 5 seconds");

        // Step 4: advance time past dinghy duration — dinghy deflates
        canalSystem.update(CanalSystem.DINGHY_MAX_DURATION_SECONDS, morning,
                swimmer, inventory, true, true, npcs, achievementCallback);

        assertFalse(canalSystem.isOnDinghy(),
                "Dinghy should deflate after " + CanalSystem.DINGHY_MAX_DURATION_SECONDS + " seconds");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Towpath economy — Maureen fish reward
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5: Towpath economy — Maureen fish reward and angler pickpocket.
     *
     * <p>Maureen active 09:00–20:00. Trade 6 fish (2 batches of 3) at 11:00 →
     * verify 10 COIN awarded. Trade again at 21:00 → verify 0 COIN.
     *
     * <p>Also verifies the PCSO patrol schedule:
     * PCSO active at 22:00, inactive at 12:00.
     */
    @Test
    void scenario5_towpathEconomy_maureenFishReward() {
        // ── Maureen fish trade during active hours ────────────────────────────

        TimeSystem activeTime = new TimeSystem(11.0f); // Maureen active
        int numBatches = 2;
        inventory.addItem(Material.CANAL_FISH, CanalSystem.MAUREEN_FISH_BATCH * numBatches);

        int coinsEarned = canalSystem.tradeFishToMaureen(inventory, activeTime);

        assertEquals(CanalSystem.MAUREEN_FISH_REWARD_COIN * numBatches, coinsEarned,
                "Should earn 5 COIN per batch of 3 fish");
        assertEquals(CanalSystem.MAUREEN_FISH_REWARD_COIN * numBatches,
                inventory.getItemCount(Material.COIN),
                "Coins should be added to inventory");
        assertEquals(0, inventory.getItemCount(Material.CANAL_FISH),
                "All fish should be consumed in the trade");

        // ── Maureen fish trade outside active hours ───────────────────────────

        TimeSystem inactiveTime = new TimeSystem(21.0f); // Maureen inactive
        inventory.addItem(Material.CANAL_FISH, CanalSystem.MAUREEN_FISH_BATCH);
        int coinsBefore = inventory.getItemCount(Material.COIN);

        int coinsOutOfHours = canalSystem.tradeFishToMaureen(inventory, inactiveTime);

        assertEquals(0, coinsOutOfHours,
                "Maureen should not buy fish outside 09:00–20:00");
        assertEquals(coinsBefore, inventory.getItemCount(Material.COIN),
                "No coins should be added if Maureen is absent");
        assertEquals(CanalSystem.MAUREEN_FISH_BATCH, inventory.getItemCount(Material.CANAL_FISH),
                "Fish should not be consumed if Maureen is absent");

        // ── PCSO patrol schedule ──────────────────────────────────────────────

        assertTrue(canalSystem.isPcsoPatrolActive(22.0f),
                "PCSO patrol should be active at 22:00");
        assertTrue(canalSystem.isPcsoPatrolActive(2.0f),
                "PCSO patrol should be active at 02:00");
        assertFalse(canalSystem.isPcsoPatrolActive(12.0f),
                "PCSO patrol should not be active at 12:00");

        // ── Angler presence schedule ──────────────────────────────────────────

        assertTrue(canalSystem.areAnglersPresent(7.0f),
                "Anglers should be present at 07:00");
        assertFalse(canalSystem.areAnglersPresent(21.0f),
                "Anglers should not be present at 21:00");

        // ── Drunk night walker schedule ────────────────────────────────────────

        assertTrue(canalSystem.isDrunkPresent(23.0f),
                "Drunk walker should be present at 23:00");
        assertFalse(canalSystem.isDrunkPresent(14.0f),
                "Drunk walker should not be present at 14:00");
    }
}

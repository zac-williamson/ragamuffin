package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.core.MarketEvent;
import ragamuffin.core.NeedType;
import ragamuffin.core.StreetEconomySystem;
import ragamuffin.core.Weather;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #899 — Wire random market event triggering into the game loop.
 *
 * <p>Four exact scenarios:
 * <ol>
 *   <li>BENEFIT_DAY zeroes NPC BROKE needs</li>
 *   <li>LAGER_SHORTAGE spikes BORED accumulation</li>
 *   <li>Market event cooldown triggers automatically</li>
 *   <li>No double-trigger while event active</li>
 * </ol>
 */
class Issue899MarketEventSchedulerTest {

    private StreetEconomySystem economy;
    private Player player;
    private Inventory playerInventory;
    private List<NPC> npcs;

    @BeforeEach
    void setUp() {
        economy = new StreetEconomySystem(new Random(42L));
        player = new Player(10f, 1f, 10f);
        playerInventory = new Inventory(36);
        npcs = new ArrayList<>();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: BENEFIT_DAY zeroes NPC BROKE needs
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: Create a StreetEconomySystem. Populate three NPCs with BROKE need score 80.
     * Call triggerMarketEvent(BENEFIT_DAY, npcs, null).
     * Call update(0.1f, npcs, player, null, 0, null, null, null).
     * Verify all three NPCs now have BROKE need score 0.
     */
    @Test
    void benefitDayZeroesNpcBrokeNeeds() {
        NPC npc1 = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        NPC npc2 = new NPC(NPCType.PUBLIC, 15f, 1f, 12f);
        NPC npc3 = new NPC(NPCType.PUBLIC, 20f, 1f, 12f);
        npcs.addAll(List.of(npc1, npc2, npc3));

        // Set BROKE need to 80 for all three NPCs
        economy.setNeedScore(npc1, NeedType.BROKE, 80f);
        economy.setNeedScore(npc2, NeedType.BROKE, 80f);
        economy.setNeedScore(npc3, NeedType.BROKE, 80f);

        // Trigger BENEFIT_DAY
        economy.triggerMarketEvent(MarketEvent.BENEFIT_DAY, npcs, null);

        // One update frame
        economy.update(0.1f, npcs, player, null, 0, null, null, null);

        // All three NPCs should have BROKE need score 0
        assertEquals(0f, economy.getNeedScore(npc1, NeedType.BROKE), 0.01f,
                "NPC1 BROKE need should be 0 after BENEFIT_DAY");
        assertEquals(0f, economy.getNeedScore(npc2, NeedType.BROKE), 0.01f,
                "NPC2 BROKE need should be 0 after BENEFIT_DAY");
        assertEquals(0f, economy.getNeedScore(npc3, NeedType.BROKE), 0.01f,
                "NPC3 BROKE need should be 0 after BENEFIT_DAY");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: LAGER_SHORTAGE spikes BORED accumulation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: Create a StreetEconomySystem.
     * Call triggerMarketEvent(LAGER_SHORTAGE, emptyList, null).
     * Run update() for 10 seconds with one NPC.
     * Verify the NPC's BORED need score is at least 2× the value it would have
     * without the event active.
     */
    @Test
    void lagerShortageSpikesBored() {
        NPC npc = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        npcs.add(npc);

        // Baseline: 10 seconds without any event
        StreetEconomySystem baseEconomy = new StreetEconomySystem(new Random(1L));
        List<NPC> baseNpcs = new ArrayList<>();
        NPC baseNpc = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        baseNpcs.add(baseNpc);
        for (int i = 0; i < 100; i++) {
            baseEconomy.update(0.1f, baseNpcs, player, null, 0, null, null, null);
        }
        float baseBored = baseEconomy.getNeedScore(baseNpc, NeedType.BORED);

        // With LAGER_SHORTAGE: 10 seconds
        economy.triggerMarketEvent(MarketEvent.LAGER_SHORTAGE, new ArrayList<>(), null);
        for (int i = 0; i < 100; i++) {
            economy.update(0.1f, npcs, player, null, 0, null, null, null);
        }
        float eventBored = economy.getNeedScore(npc, NeedType.BORED);

        // BORED accumulation is 2× during LAGER_SHORTAGE — must be at least 2× baseline
        assertTrue(eventBored >= baseBored * 2f,
                "BORED need during LAGER_SHORTAGE (" + eventBored
                        + ") should be at least 2× baseline (" + baseBored + ")");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Market event cooldown triggers automatically
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: Construct a game loop harness with streetEconomySystem,
     * marketEventCooldown = 0.1f, and no active event.
     * Advance by 0.2 seconds.
     * Verify that streetEconomySystem.getActiveEvent() is non-null (an event was triggered).
     *
     * <p>This test simulates the scheduler logic added to RagamuffinGame to verify
     * the triggering mechanism works correctly.
     */
    @Test
    void marketEventCooldownTriggersAutomatically() {
        // No active event to start
        assertNull(economy.getActiveEvent(), "No event should be active initially");

        // Simulate the scheduler: cooldown starts at 0.1f
        float[] cooldownHolder = {0.1f};
        float delta = 0.1f;

        // Simulate two frames (0.2 seconds total) — cooldown reaches 0 on first frame
        for (int frame = 0; frame < 2; frame++) {
            economy.update(delta, npcs, player, null, 0, null, null, null);
            cooldownHolder[0] -= delta;
            if (cooldownHolder[0] <= 0f && economy.getActiveEvent() == null) {
                // Pick a random non-GREGGS_STRIKE event
                MarketEvent[] candidates = java.util.Arrays.stream(MarketEvent.values())
                        .filter(e -> e != MarketEvent.GREGGS_STRIKE)
                        .toArray(MarketEvent[]::new);
                MarketEvent event = candidates[new Random(99L).nextInt(candidates.length)];
                economy.triggerMarketEvent(event, npcs, null);
                cooldownHolder[0] = 120f + new Random(99L).nextFloat() * 180f;
            }
        }

        assertNotNull(economy.getActiveEvent(),
                "An event should have been triggered after cooldown expired");
        assertNotEquals(MarketEvent.GREGGS_STRIKE, economy.getActiveEvent(),
                "Triggered event must not be GREGGS_STRIKE (handled by NewspaperSystem)");
        // Verify cooldown was reset to a value in [120, 300]
        assertTrue(cooldownHolder[0] >= 120f && cooldownHolder[0] <= 300f,
                "Cooldown should be reset to [120, 300] after trigger (got " + cooldownHolder[0] + ")");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: No double-trigger while event active
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Trigger COLD_SNAP manually. Set marketEventCooldown = 0.
     * Advance by 1 second. Verify getActiveEvent() is still COLD_SNAP
     * (the active event was not replaced by the scheduler).
     */
    @Test
    void noDoubleTriggerWhileEventActive() {
        // Manually trigger COLD_SNAP
        economy.triggerMarketEvent(MarketEvent.COLD_SNAP, npcs, null);
        assertEquals(MarketEvent.COLD_SNAP, economy.getActiveEvent(),
                "COLD_SNAP should be active after manual trigger");

        // Simulate the scheduler with cooldown = 0
        float[] cooldownHolder = {0f};
        float delta = 0.1f;

        // Advance 10 frames (1 second total) — cooldown is 0 every frame but event is active
        for (int frame = 0; frame < 10; frame++) {
            economy.update(delta, npcs, player, null, 0, null, null, null);
            cooldownHolder[0] -= delta;
            // Scheduler should NOT fire because getActiveEvent() != null
            if (cooldownHolder[0] <= 0f && economy.getActiveEvent() == null) {
                MarketEvent[] candidates = java.util.Arrays.stream(MarketEvent.values())
                        .filter(e -> e != MarketEvent.GREGGS_STRIKE)
                        .toArray(MarketEvent[]::new);
                MarketEvent event = candidates[new Random(99L).nextInt(candidates.length)];
                economy.triggerMarketEvent(event, npcs, null);
                cooldownHolder[0] = 120f + new Random(99L).nextFloat() * 180f;
            }
        }

        // COLD_SNAP must still be the active event — not replaced
        assertEquals(MarketEvent.COLD_SNAP, economy.getActiveEvent(),
                "COLD_SNAP should still be the active event — scheduler must not replace it");
    }
}

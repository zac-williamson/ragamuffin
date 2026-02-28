package ragamuffin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #936: Unit and integration tests for {@link SkipDivingSystem}.
 *
 * <p>Covers:
 * <ol>
 *   <li>Bulky Item Day spawns on day 3 (event lifecycle).</li>
 *   <li>Player picks up item → inventory updated, WatchAnger +3.</li>
 *   <li>5+ items triggers PETITION_BOARD flag.</li>
 *   <li>Lorry removes remaining items at 10:00 (event closes).</li>
 *   <li>SKIP_DIVER NPC grabs unclaimed items on 30-second tick.</li>
 *   <li>ANTIQUE_CLOCK fence sale awards ANTIQUE_ROADSHOW achievement.</li>
 *   <li>PIGEON_FANCIER pre-claims BOX_OF_RECORDS at 07:55.</li>
 *   <li>Loot generation produces 8–14 items.</li>
 *   <li>FenceValuationTable recognises all skip materials.</li>
 *   <li>SKIP_KING achievement on 5+ items; EARLY_BIRD on first take.</li>
 * </ol>
 */
class SkipDivingSystemTest {

    private SkipDivingSystem skipSystem;
    private NeighbourhoodWatchSystem watchSystem;
    private Inventory playerInventory;
    private FenceValuationTable fenceTable;
    private List<AchievementType> awarded;
    private NotorietySystem.AchievementCallback callback;

    @BeforeEach
    void setUp() {
        skipSystem = new SkipDivingSystem(new Random(42));
        watchSystem = new NeighbourhoodWatchSystem(new Random(42));
        playerInventory = new Inventory();
        fenceTable = new FenceValuationTable();
        awarded = new ArrayList<>();
        callback = awarded::add;
    }

    // ── Test 1: Bulky Item Day spawns on day 3 ────────────────────────────────

    @Test
    void testEventNotActiveBeforeDay3() {
        assertFalse(skipSystem.isEventActive(), "Event should not be active initially");
        assertFalse(skipSystem.isBulkyItemDay(1), "Day 1 is not Bulky Item Day");
        assertFalse(skipSystem.isBulkyItemDay(2), "Day 2 is not Bulky Item Day");
    }

    @Test
    void testEventActivatesOnDay3At0800() {
        assertFalse(skipSystem.isEventActive());

        // Day 3 at 07:59 — not active yet
        skipSystem.update(0f, 3, 7.98f, watchSystem, playerInventory, callback);
        assertFalse(skipSystem.isEventActive(), "Event should not be active at 07:59");

        // Day 3 at 08:00 — should activate
        skipSystem.update(0f, 3, 8.0f, watchSystem, playerInventory, callback);
        assertTrue(skipSystem.isEventActive(), "Event should be active at 08:00 on day 3");
    }

    @Test
    void testEventActivatesOnDay6AsToo() {
        // Day 6 is also a Bulky Item Day (every 3 days)
        assertTrue(skipSystem.isBulkyItemDay(3), "Day 3 should be Bulky Item Day");
        assertTrue(skipSystem.isBulkyItemDay(6), "Day 6 should be Bulky Item Day");
        assertTrue(skipSystem.isBulkyItemDay(9), "Day 9 should be Bulky Item Day");
        assertFalse(skipSystem.isBulkyItemDay(4), "Day 4 should NOT be Bulky Item Day");
        assertFalse(skipSystem.isBulkyItemDay(5), "Day 5 should NOT be Bulky Item Day");
    }

    @Test
    void testEventContains8To14Items() {
        // Activate event on day 3 at 08:00
        skipSystem.update(0f, 3, 8.0f, watchSystem, playerInventory, callback);
        assertTrue(skipSystem.isEventActive());

        int itemCount = skipSystem.getSkipItems().size();
        assertTrue(itemCount >= SkipDivingSystem.LOOT_MIN,
            "Should have at least " + SkipDivingSystem.LOOT_MIN + " items, got " + itemCount);
        assertTrue(itemCount <= SkipDivingSystem.LOOT_MAX,
            "Should have at most " + SkipDivingSystem.LOOT_MAX + " items, got " + itemCount);
    }

    // ── Test 2: Player picks up item, WatchAnger +3 ───────────────────────────

    @Test
    void testPlayerPicksUpItemUpdatesInventoryAndWatchAnger() {
        List<Material> loot = Arrays.asList(
            Material.OLD_SOFA, Material.BROKEN_TELLY, Material.WONKY_CHAIR
        );
        skipSystem.openEventForTesting(loot);
        skipSystem.setSkipPosition(100f, 1f, 100f);

        watchSystem.setWatchAnger(10);
        int angerBefore = watchSystem.getWatchAnger();

        boolean result = skipSystem.onPlayerTakesItem(
            Material.OLD_SOFA, playerInventory, watchSystem, callback);

        assertTrue(result, "Should successfully pick up OLD_SOFA");
        assertEquals(1, playerInventory.getItemCount(Material.OLD_SOFA),
            "Player inventory should contain 1 OLD_SOFA");
        assertFalse(skipSystem.getSkipItems().contains(Material.OLD_SOFA),
            "OLD_SOFA should be removed from skip zone");
        assertEquals(angerBefore + SkipDivingSystem.WATCH_ANGER_PER_ITEM,
            watchSystem.getWatchAnger(),
            "WatchAnger should increase by " + SkipDivingSystem.WATCH_ANGER_PER_ITEM);
    }

    @Test
    void testPlayerCannotPickUpItemWhenEventNotActive() {
        // No event open
        List<Material> loot = Arrays.asList(Material.OLD_SOFA);
        // Don't open the event

        boolean result = skipSystem.onPlayerTakesItem(
            Material.OLD_SOFA, playerInventory, watchSystem, callback);

        assertFalse(result, "Should not pick up item when event is not active");
        assertEquals(0, playerInventory.getItemCount(Material.OLD_SOFA));
    }

    // ── Test 3: 5+ items triggers PETITION_BOARD ──────────────────────────────

    @Test
    void testFiveItemsTriggersPetitionBoard() {
        List<Material> loot = Arrays.asList(
            Material.OLD_SOFA, Material.BROKEN_TELLY, Material.WONKY_CHAIR,
            Material.CARPET_ROLL, Material.OLD_MATTRESS, Material.FILING_CABINET
        );
        skipSystem.openEventForTesting(loot);

        assertFalse(skipSystem.isPetitionBoardSpawned(),
            "Petition board should not be spawned initially");

        // Take 4 items — no petition board yet
        for (int i = 0; i < 4; i++) {
            skipSystem.onPlayerTakesItem(watchSystem, callback);
        }
        assertFalse(skipSystem.isPetitionBoardSpawned(),
            "Petition board should not spawn after only 4 items");

        // Take the 5th item — triggers petition board
        skipSystem.onPlayerTakesItem(watchSystem, callback);
        assertTrue(skipSystem.isPetitionBoardSpawned(),
            "Petition board should be spawned after 5 items");
        assertEquals(5, skipSystem.getPlayerItemsTakenThisEvent());
    }

    @Test
    void testSkipKingAchievementAwardedAt5Items() {
        List<Material> loot = Arrays.asList(
            Material.OLD_SOFA, Material.BROKEN_TELLY, Material.WONKY_CHAIR,
            Material.CARPET_ROLL, Material.OLD_MATTRESS, Material.FILING_CABINET
        );
        skipSystem.openEventForTesting(loot);

        // Take 5 items
        for (int i = 0; i < 5; i++) {
            skipSystem.onPlayerTakesItem(watchSystem, callback);
        }

        assertTrue(awarded.contains(AchievementType.SKIP_KING),
            "SKIP_KING achievement should be awarded after 5 items");
    }

    // ── Test 4: Lorry removes remaining items at 10:00 ────────────────────────

    @Test
    void testLorryRemovesItemsAt1000() {
        List<Material> loot = Arrays.asList(
            Material.OLD_SOFA, Material.BROKEN_TELLY, Material.WONKY_CHAIR, Material.CARPET_ROLL
        );
        skipSystem.openEventForTesting(loot);

        assertTrue(skipSystem.isEventActive(), "Event should be active");
        assertEquals(4, skipSystem.getSkipItems().size(), "Should have 4 unclaimed items");

        // Advance time to 10:00
        skipSystem.update(0f, 3, 10.0f, watchSystem, playerInventory, callback);

        assertFalse(skipSystem.isEventActive(), "Event should be closed at 10:00");
        assertTrue(skipSystem.getSkipItems().isEmpty(),
            "All unclaimed items should be removed by the lorry");
    }

    @Test
    void testEventStillActiveJustBefore1000() {
        skipSystem.openEventForTesting(List.of(Material.OLD_SOFA));
        skipSystem.update(0f, 3, 9.99f, watchSystem, playerInventory, callback);
        assertTrue(skipSystem.isEventActive(), "Event should still be active just before 10:00");
    }

    // ── Test 5: SKIP_DIVER grabs unclaimed items ──────────────────────────────

    @Test
    void testSkipDiverGrabsItemOnTick() {
        List<Material> loot = Arrays.asList(
            Material.OLD_SOFA, Material.BROKEN_TELLY, Material.WONKY_CHAIR
        );
        skipSystem.openEventForTesting(loot);
        skipSystem.setSkipPosition(50f, 1f, 50f);

        // Spawn 1 diver for simplicity
        List<NPC> divers = skipSystem.spawnSkipDivers(2);
        assertFalse(divers.isEmpty(), "Should spawn at least 2 SKIP_DIVER NPCs");

        int itemsBefore = skipSystem.getSkipItems().size();
        assertEquals(3, itemsBefore, "Should start with 3 items in skip zone");

        // Trigger a grab
        Material grabbed = skipSystem.triggerSkipDiverGrabForTesting();

        assertNotNull(grabbed, "SKIP_DIVER should grab an item");
        assertEquals(itemsBefore - 1, skipSystem.getSkipItems().size(),
            "One item should be removed from the skip zone");

        // Verify the diver holds the item
        NPC diver = divers.get(0);
        // The grabbed item is tracked in the diver inventories
        List<Material> diverInventory = skipSystem.getSkipDiverInventory(diver);
        // At least one diver has a grabbed item total (we don't guarantee which diver)
        boolean anyDiverHasItem = divers.stream()
            .anyMatch(d -> !skipSystem.getSkipDiverInventory(d).isEmpty());
        assertTrue(anyDiverHasItem, "At least one SKIP_DIVER should hold the grabbed item");
    }

    @Test
    void testSkipDiverCountInRange() {
        skipSystem.openEventForTesting(List.of(Material.OLD_SOFA));
        skipSystem.setSkipPosition(50f, 1f, 50f);

        List<NPC> divers = skipSystem.spawnSkipDivers(3);
        assertEquals(3, divers.size(), "Should spawn exactly 3 SKIP_DIVER NPCs");

        // All should be SKIP_DIVER type
        for (NPC diver : divers) {
            assertEquals(NPCType.SKIP_DIVER, diver.getType(),
                "Spawned NPC should be SKIP_DIVER type");
        }
    }

    // ── Test 6: ANTIQUE_CLOCK fence sale awards achievement ───────────────────

    @Test
    void testAntiqueCockFenceSaleAwardsAchievement() {
        // Give player an ANTIQUE_CLOCK
        playerInventory.addItem(Material.ANTIQUE_CLOCK, 1);

        // Sell to fence
        String dialogue = skipSystem.onFenceSale(Material.ANTIQUE_CLOCK, callback);

        // Verify special dialogue
        assertNotNull(dialogue, "Should return special Fence dialogue for ANTIQUE_CLOCK");
        assertEquals(SkipDivingSystem.ANTIQUE_CLOCK_FENCE_DIALOGUE, dialogue,
            "Fence dialogue should match the spec");

        // Verify achievement
        assertTrue(awarded.contains(AchievementType.ANTIQUE_ROADSHOW),
            "ANTIQUE_ROADSHOW achievement should be awarded");
    }

    @Test
    void testOtherMaterialsDoNotTriggerSpecialFenceDialogue() {
        String dialogue = skipSystem.onFenceSale(Material.OLD_SOFA, callback);
        assertNull(dialogue, "OLD_SOFA should not trigger special Fence dialogue");
        assertFalse(awarded.contains(AchievementType.ANTIQUE_ROADSHOW));
    }

    // ── Test 7: PIGEON_FANCIER pre-claims BOX_OF_RECORDS at 07:55 ─────────────

    @Test
    void testPigeonFancierPreclaimsBoxOfRecordsAt0755() {
        // Set up a pigeon fancier NPC
        NPC fancier = new NPC(NPCType.PIGEON_FANCIER, 100f, 1f, 100f);
        skipSystem.setPigeonFancier(fancier);

        assertFalse(skipSystem.isPigeonFancierHoldingRecords(),
            "Pigeon Fancier should not hold records initially");

        // Advance to day 3 at 07:55
        skipSystem.update(0f, 3, SkipDivingSystem.PIGEON_FANCIER_PRECLAIM_HOUR,
            watchSystem, playerInventory, callback);

        assertTrue(skipSystem.isPigeonFancierHoldingRecords(),
            "PIGEON_FANCIER should hold BOX_OF_RECORDS at 07:55");
    }

    @Test
    void testPigeonFancierDialogueText() {
        skipSystem.triggerPigeonFancierPreclaimForTesting();
        String dialogue = skipSystem.getPigeonFancierDialogue();
        assertNotNull(dialogue, "Should have negotiation dialogue");
        assertTrue(dialogue.contains("first dibs"), "Dialogue should mention 'first dibs'");
    }

    @Test
    void testPlayerCanBuyRecordsFromPigeonFancier() {
        skipSystem.triggerPigeonFancierPreclaimForTesting();
        assertTrue(skipSystem.isPigeonFancierHoldingRecords());

        // Not enough coins
        playerInventory.addItem(Material.COIN, 3);
        boolean result = skipSystem.buyRecordsFromPigeonFancier(playerInventory);
        assertFalse(result, "Should fail without enough COIN");
        assertTrue(skipSystem.isPigeonFancierHoldingRecords(),
            "Fancier should still hold records after failed purchase");

        // Enough coins
        playerInventory.addItem(Material.COIN, 2); // total: 5
        result = skipSystem.buyRecordsFromPigeonFancier(playerInventory);
        assertTrue(result, "Should succeed with 5 COIN");
        assertFalse(skipSystem.isPigeonFancierHoldingRecords(),
            "Pigeon Fancier should no longer hold records");
        assertEquals(1, playerInventory.getItemCount(Material.BOX_OF_RECORDS),
            "Player should receive BOX_OF_RECORDS");
        assertEquals(0, playerInventory.getItemCount(Material.COIN),
            "5 COIN should be deducted");
    }

    @Test
    void testBoxOfRecordsNotInSkipZoneWhenPigeonFancierHoldsIt() {
        // Set up event with loot that would normally include BOX_OF_RECORDS,
        // but pigeon fancier pre-claims it before the window opens
        skipSystem.setPigeonFancier(new NPC(NPCType.PIGEON_FANCIER, 10f, 1f, 10f));

        // Trigger pre-claim at 07:55 (day 3, before event opens)
        skipSystem.update(0f, 3, SkipDivingSystem.PIGEON_FANCIER_PRECLAIM_HOUR,
            watchSystem, playerInventory, callback);

        assertTrue(skipSystem.isPigeonFancierHoldingRecords(),
            "Pigeon Fancier should hold records at 07:55");

        // Event opens at 08:00
        skipSystem.update(0f, 3, 8.0f, watchSystem, playerInventory, callback);
        assertTrue(skipSystem.isEventActive(), "Event should now be active");

        // BOX_OF_RECORDS should not be in the skip zone (pigeon fancier has it)
        assertFalse(skipSystem.getSkipItems().contains(Material.BOX_OF_RECORDS),
            "BOX_OF_RECORDS should not be in the skip zone (pigeon fancier pre-claimed it)");
    }

    // ── Test 8: Loot generation range and rarity ──────────────────────────────

    @Test
    void testLootGenerationRange() {
        for (int i = 0; i < 20; i++) {
            List<Material> loot = skipSystem.generateLoot();
            assertTrue(loot.size() >= SkipDivingSystem.LOOT_MIN,
                "Loot size " + loot.size() + " below minimum " + SkipDivingSystem.LOOT_MIN);
            assertTrue(loot.size() <= SkipDivingSystem.LOOT_MAX,
                "Loot size " + loot.size() + " above maximum " + SkipDivingSystem.LOOT_MAX);
        }
    }

    @Test
    void testLootOnlyContainsSkipLotMaterials() {
        List<Material> loot = skipSystem.generateLoot();
        List<Material> validMaterials = new ArrayList<>();
        for (SkipDivingSystem.SkipLot lot : SkipDivingSystem.SkipLot.values()) {
            validMaterials.add(lot.getMaterial());
        }
        for (Material m : loot) {
            assertTrue(validMaterials.contains(m),
                "Loot should only contain SkipLot materials, found: " + m);
        }
    }

    // ── Test 9: FenceValuationTable recognises all skip materials ─────────────

    @Test
    void testFenceTableAcceptsAllSkipMaterials() {
        for (SkipDivingSystem.SkipLot lot : SkipDivingSystem.SkipLot.values()) {
            assertTrue(fenceTable.accepts(lot.getMaterial()),
                "FenceValuationTable should accept " + lot.getMaterial());
            assertTrue(fenceTable.getValueFor(lot.getMaterial()) > 0,
                "Fence value for " + lot.getMaterial() + " should be > 0");
            assertTrue(fenceTable.isSkipSalvage(lot.getMaterial()),
                lot.getMaterial() + " should be marked as skip salvage (no Notoriety)");
        }
    }

    @Test
    void testFenceValuesMatchSpec() {
        assertEquals(3,  fenceTable.getValueFor(Material.OLD_SOFA));
        assertEquals(4,  fenceTable.getValueFor(Material.BROKEN_TELLY));
        assertEquals(2,  fenceTable.getValueFor(Material.WONKY_CHAIR));
        assertEquals(3,  fenceTable.getValueFor(Material.CARPET_ROLL));
        assertEquals(5,  fenceTable.getValueFor(Material.OLD_MATTRESS));
        assertEquals(6,  fenceTable.getValueFor(Material.FILING_CABINET));
        assertEquals(7,  fenceTable.getValueFor(Material.EXERCISE_BIKE));
        assertEquals(8,  fenceTable.getValueFor(Material.BOX_OF_RECORDS));
        assertEquals(10, fenceTable.getValueFor(Material.MICROWAVE));
        assertEquals(12, fenceTable.getValueFor(Material.SHOPPING_TROLLEY_GOLD));
        assertEquals(20, fenceTable.getValueFor(Material.ANTIQUE_CLOCK));
    }

    // ── Test 10: EARLY_BIRD achievement ───────────────────────────────────────

    @Test
    void testEarlyBirdAwardedOnFirstItemTaken() {
        skipSystem.openEventForTesting(Arrays.asList(Material.OLD_SOFA, Material.BROKEN_TELLY));

        skipSystem.onPlayerTakesItem(watchSystem, callback);

        assertTrue(awarded.contains(AchievementType.EARLY_BIRD),
            "EARLY_BIRD should be awarded on first item taken");
    }

    @Test
    void testEarlyBirdNotAwardedTwice() {
        skipSystem.openEventForTesting(Arrays.asList(
            Material.OLD_SOFA, Material.BROKEN_TELLY, Material.WONKY_CHAIR
        ));

        skipSystem.onPlayerTakesItem(watchSystem, callback);
        skipSystem.onPlayerTakesItem(watchSystem, callback);

        long earlyBirdCount = awarded.stream()
            .filter(a -> a == AchievementType.EARLY_BIRD)
            .count();
        assertEquals(1, earlyBirdCount, "EARLY_BIRD should be awarded exactly once");
    }

    // ── Test 11: WatchAnger integration ───────────────────────────────────────

    @Test
    void testWatchAngerIncreasesBy3PerItem() {
        skipSystem.openEventForTesting(Arrays.asList(
            Material.OLD_SOFA, Material.BROKEN_TELLY, Material.WONKY_CHAIR
        ));
        watchSystem.setWatchAnger(0);

        skipSystem.onPlayerTakesItem(watchSystem, callback);
        assertEquals(3, watchSystem.getWatchAnger(), "WatchAnger should be 3 after 1 item");

        skipSystem.onPlayerTakesItem(watchSystem, callback);
        assertEquals(6, watchSystem.getWatchAnger(), "WatchAnger should be 6 after 2 items");

        skipSystem.onPlayerTakesItem(watchSystem, callback);
        assertEquals(9, watchSystem.getWatchAnger(), "WatchAnger should be 9 after 3 items");
    }

    // ── Test 12: First-interaction tooltip ────────────────────────────────────

    @Test
    void testFirstInteractionTooltip() {
        assertEquals(SkipDivingSystem.FIRST_INTERACTION_TOOLTIP,
            skipSystem.getFirstInteractionTooltip(),
            "Tooltip text should match spec");
        assertFalse(skipSystem.isFirstInteractionTooltipShown(),
            "Tooltip should not be shown initially");

        skipSystem.openEventForTesting(List.of(Material.OLD_SOFA));
        skipSystem.onPlayerTakesItem(watchSystem, callback);

        assertTrue(skipSystem.isFirstInteractionTooltipShown(),
            "Tooltip should be marked as shown after first interaction");
    }

    // ── Test 13: Crafting recipes exist in CraftingSystem ─────────────────────

    @Test
    void testCraftingRecipesExist() {
        ragamuffin.building.CraftingSystem crafting = new ragamuffin.building.CraftingSystem();
        List<ragamuffin.building.Recipe> recipes = crafting.getAllRecipes();

        // LUXURY_BED: OLD_MATTRESS + SLEEPING_BAG → LUXURY_BED
        boolean hasLuxuryBed = recipes.stream().anyMatch(r ->
            r.getInputs().containsKey(Material.OLD_MATTRESS)
            && r.getInputs().containsKey(Material.SLEEPING_BAG)
            && r.getOutputs().containsKey(Material.LUXURY_BED));
        assertTrue(hasLuxuryBed, "LUXURY_BED recipe should exist");

        // IMPROVISED_TOOL (skip variant): EXERCISE_BIKE + SCRAP_METAL → IMPROVISED_TOOL
        boolean hasImprovToolSkip = recipes.stream().anyMatch(r ->
            r.getInputs().containsKey(Material.EXERCISE_BIKE)
            && r.getInputs().containsKey(Material.SCRAP_METAL)
            && r.getOutputs().containsKey(Material.IMPROVISED_TOOL));
        assertTrue(hasImprovToolSkip, "Skip-variant IMPROVISED_TOOL recipe should exist");

        // HOT_PASTRY: GREGGS_PASTRY → HOT_PASTRY
        boolean hasHotPastry = recipes.stream().anyMatch(r ->
            r.getInputs().containsKey(Material.GREGGS_PASTRY)
            && r.getOutputs().containsKey(Material.HOT_PASTRY));
        assertTrue(hasHotPastry, "HOT_PASTRY recipe should exist");
    }
}

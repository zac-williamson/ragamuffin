package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.core.BuildingQuestRegistry;
import ragamuffin.core.InteractionSystem;
import ragamuffin.entity.NPC;
import ragamuffin.world.BlockType;
import ragamuffin.world.Landmark;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #509: spawnBuildingNPCs() must use the live
 * interactionSystem.getQuestRegistry() rather than a throwaway
 * BuildingQuestRegistry instance.
 */
class Issue509BuildingNPCLiveRegistryTest {

    private NPCManager npcManager;
    private InteractionSystem interactionSystem;
    private Inventory inventory;
    private World world;

    @BeforeEach
    void setUp() {
        npcManager = new NPCManager();
        interactionSystem = new InteractionSystem();
        inventory = new Inventory(36);

        world = new World(42);
        for (int x = -10; x < 120; x++) {
            for (int z = -10; z < 120; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * Test 1: NPCs spawned via the live registry correspond exactly to the
     * landmark types registered in interactionSystem.getQuestRegistry().
     *
     * Register several landmarks (some with quests, some without). Run the
     * spawnBuildingNPCs() logic using the live registry. Verify the set of
     * NPC buildingTypes matches exactly the set of landmark types that have
     * a registered quest.
     */
    @Test
    void test1_NPCBuildingTypesMatchLiveRegistry() {
        BuildingQuestRegistry liveRegistry = interactionSystem.getQuestRegistry();

        // Add landmarks — GREGGS and JEWELLER have quests; WAREHOUSE and PARK do not.
        world.addLandmark(new Landmark(LandmarkType.GREGGS, 10, 0, 10, 8, 5, 8));
        world.addLandmark(new Landmark(LandmarkType.JEWELLER, 30, 0, 30, 8, 5, 8));
        world.addLandmark(new Landmark(LandmarkType.WAREHOUSE, 50, 0, 50, 20, 8, 15));
        world.addLandmark(new Landmark(LandmarkType.PARK, 70, 0, 70, 30, 1, 30));

        int before = npcManager.getNPCs().size();

        // Simulate spawnBuildingNPCs() using the live registry (the fixed behaviour)
        for (Landmark lm : world.getAllLandmarks()) {
            LandmarkType type = lm.getType();
            if (liveRegistry.hasQuest(type)) {
                float x = lm.getPosition().x + lm.getWidth() / 2.0f;
                float z = lm.getPosition().z + lm.getDepth() / 2.0f;
                float y = lm.getPosition().y + 1.0f;
                npcManager.spawnBuildingNPC(type, x, y, z);
            }
        }

        List<NPC> allNPCs = npcManager.getNPCs();
        // Collect building types of all newly spawned NPCs
        Set<LandmarkType> spawnedTypes = new HashSet<>();
        for (int i = before; i < allNPCs.size(); i++) {
            LandmarkType bt = allNPCs.get(i).getBuildingType();
            assertNotNull(bt, "Every building NPC spawned by the live registry should have a non-null buildingType");
            spawnedTypes.add(bt);
        }

        // Exactly GREGGS and JEWELLER should have been spawned
        assertTrue(spawnedTypes.contains(LandmarkType.GREGGS),
                "GREGGS landmark should have produced a building NPC");
        assertTrue(spawnedTypes.contains(LandmarkType.JEWELLER),
                "JEWELLER landmark should have produced a building NPC");
        assertFalse(spawnedTypes.contains(LandmarkType.WAREHOUSE),
                "WAREHOUSE has no quest and should NOT produce a building NPC");
        assertFalse(spawnedTypes.contains(LandmarkType.PARK),
                "PARK has no quest and should NOT produce a building NPC");
        assertEquals(2, spawnedTypes.size(),
                "Exactly 2 building NPCs should be spawned (one per quest-registered landmark)");
    }

    /**
     * Test 2: Interacting with a building NPC offers quest dialogue from the
     * live registry, not from a fresh/reset copy.
     *
     * Spawn a JEWELLER NPC. Interact with it via the same InteractionSystem
     * that owns the live registry. Verify the returned dialogue is the quest
     * offer from the live registry (not blank, and mentions the jeweller quest).
     */
    @Test
    void test2_InteractionUsesLiveRegistryDialogue() {
        NPC npc = npcManager.spawnBuildingNPC(LandmarkType.JEWELLER, 50, 1, 50);
        assertNotNull(npc, "Jeweller building NPC should be spawned");

        String dialogue = interactionSystem.interactWithNPC(npc, inventory);

        assertNotNull(dialogue, "Interaction with quest NPC should return dialogue");
        assertFalse(dialogue.isBlank(), "Dialogue from live registry should not be blank");
        // The jeweller quest in the live registry mentions "diamond" or "nicked"
        assertTrue(dialogue.toLowerCase().contains("diamond")
                        || dialogue.toLowerCase().contains("bring")
                        || dialogue.toLowerCase().contains("nicked"),
                "Dialogue should be from the live JEWELLER quest, got: " + dialogue);
    }

    /**
     * Test 3: A throwaway BuildingQuestRegistry and the live registry both
     * have the same quest registrations — confirming the fix preserves behaviour
     * while eliminating the wasteful allocation.
     *
     * The live registry must return hasQuest()==true for every LandmarkType that
     * a fresh BuildingQuestRegistry also returns hasQuest()==true for.
     */
    @Test
    void test3_LiveRegistryHasSameQuestsAsThrowaway() {
        BuildingQuestRegistry liveRegistry = interactionSystem.getQuestRegistry();
        BuildingQuestRegistry throwaway = new BuildingQuestRegistry();

        for (LandmarkType type : LandmarkType.values()) {
            assertEquals(throwaway.hasQuest(type), liveRegistry.hasQuest(type),
                    "Live registry and throwaway registry should agree on hasQuest() for " + type);
        }
    }
}

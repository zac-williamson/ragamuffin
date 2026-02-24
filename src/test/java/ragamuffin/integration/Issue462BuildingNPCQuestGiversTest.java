package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.BuildingQuestRegistry;
import ragamuffin.core.InteractionSystem;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.world.BlockType;
import ragamuffin.world.Landmark;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #462: Building NPC quest givers must be spawned
 * inside their respective buildings when the world is generated.
 *
 * Issue #440 added the spawnBuildingNPC() infrastructure but never wired it to
 * world generation. This fix calls spawnBuildingNPCs() after spawnInitialNPCs(),
 * placing a static SHOPKEEPER quest-giver inside every labelled building that
 * has a registered quest.
 */
class Issue462BuildingNPCQuestGiversTest {

    private NPCManager npcManager;
    private BuildingQuestRegistry questRegistry;
    private Inventory inventory;
    private World world;

    @BeforeEach
    void setUp() {
        npcManager = new NPCManager();
        questRegistry = new BuildingQuestRegistry();
        inventory = new Inventory(36);

        world = new World(12345);
        // Flat ground for spawn height calculation
        for (int x = -10; x < 120; x++) {
            for (int z = -10; z < 120; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * Test 1: spawnBuildingNPC() creates a SHOPKEEPER with the correct buildingType set
     * and state IDLE.
     *
     * Spawn a building NPC for GREGGS at (10, 1, 10). Verify:
     * - Returned NPC is not null
     * - NPC type is SHOPKEEPER
     * - NPC buildingType is GREGGS
     * - NPC state is IDLE
     */
    @Test
    void test1_SpawnBuildingNPCCreatesCorrectNPC() {
        NPC npc = npcManager.spawnBuildingNPC(LandmarkType.GREGGS, 10, 1, 10);

        assertNotNull(npc, "spawnBuildingNPC should return a non-null NPC");
        assertEquals(NPCType.SHOPKEEPER, npc.getType(),
                "Building NPC should be of type SHOPKEEPER");
        assertEquals(LandmarkType.GREGGS, npc.getBuildingType(),
                "Building NPC should have buildingType GREGGS");
        assertEquals(NPCState.IDLE, npc.getState(),
                "Building NPC should start in IDLE state");
    }

    /**
     * Test 2: Building NPCs are positioned inside landmark bounds.
     *
     * Register a GREGGS landmark at (20, 0, 20) with width=8, depth=8.
     * Simulate spawnBuildingNPCs() by iterating landmarks and spawning NPCs
     * for those with quests. Verify the spawned NPC's X and Z are inside
     * the landmark bounds.
     */
    @Test
    void test2_BuildingNPCPositionedInsideLandmark() {
        // Register a GREGGS landmark
        world.addLandmark(new Landmark(LandmarkType.GREGGS, 20, 0, 20, 8, 5, 8));

        // Simulate what spawnBuildingNPCs() does
        NPC spawned = null;
        for (Landmark lm : world.getAllLandmarks()) {
            LandmarkType type = lm.getType();
            if (questRegistry.hasQuest(type)) {
                float x = lm.getPosition().x + lm.getWidth() / 2.0f;
                float z = lm.getPosition().z + lm.getDepth() / 2.0f;
                float y = 1.0f;
                spawned = npcManager.spawnBuildingNPC(type, x, y, z);
            }
        }

        assertNotNull(spawned, "A building NPC should have been spawned for GREGGS");

        // Verify the NPC is inside the landmark bounds (centre of 20..28 = 24)
        Landmark greggs = world.getLandmark(LandmarkType.GREGGS);
        assertNotNull(greggs);
        float npcX = spawned.getPosition().x;
        float npcZ = spawned.getPosition().z;

        assertTrue(npcX >= greggs.getPosition().x && npcX < greggs.getPosition().x + greggs.getWidth(),
                "NPC X (" + npcX + ") should be inside landmark X bounds ["
                        + greggs.getPosition().x + ", " + (greggs.getPosition().x + greggs.getWidth()) + ")");
        assertTrue(npcZ >= greggs.getPosition().z && npcZ < greggs.getPosition().z + greggs.getDepth(),
                "NPC Z (" + npcZ + ") should be inside landmark Z bounds ["
                        + greggs.getPosition().z + ", " + (greggs.getPosition().z + greggs.getDepth()) + ")");
    }

    /**
     * Test 3: Interacting with a building NPC offers the registered quest.
     *
     * Spawn a building NPC for JEWELLER. Create an InteractionSystem. Call
     * interactWithNPC(). Verify the returned dialogue contains text from the
     * JEWELLER quest description (it should offer the quest on first interaction).
     */
    @Test
    void test3_InteractingWithBuildingNPCOffersQuest() {
        NPC npc = npcManager.spawnBuildingNPC(LandmarkType.JEWELLER, 50, 1, 50);
        assertNotNull(npc, "Jeweller building NPC should be spawned");

        InteractionSystem interactionSystem = new InteractionSystem();
        String dialogue = interactionSystem.interactWithNPC(npc, inventory);

        assertNotNull(dialogue, "Interacting with a quest NPC should return dialogue");
        assertFalse(dialogue.isBlank(), "Dialogue should not be blank");
        // The jeweller quest description mentions "diamond" — verify this is the quest offer
        assertTrue(dialogue.toLowerCase().contains("diamond")
                        || dialogue.toLowerCase().contains("bring") || dialogue.toLowerCase().contains("nicked"),
                "Dialogue should be the quest offer for JEWELLER, got: " + dialogue);
    }

    /**
     * Test 4: BuildingQuestRegistry covers the expected landmark types.
     *
     * Verify that the key commercial/civic building types used in the world
     * each have a registered quest, ensuring quest-giver NPCs will be spawned
     * for them.
     */
    @Test
    void test4_BuildingQuestRegistryCoversKeyBuildings() {
        LandmarkType[] expectedQuestBuildings = {
            LandmarkType.GREGGS,
            LandmarkType.OFF_LICENCE,
            LandmarkType.CHARITY_SHOP,
            LandmarkType.JEWELLER,
            LandmarkType.JOB_CENTRE,
            LandmarkType.BOOKIES,
            LandmarkType.OFFICE_BUILDING,
            LandmarkType.GP_SURGERY,
            LandmarkType.PRIMARY_SCHOOL,
            LandmarkType.COMMUNITY_CENTRE,
            LandmarkType.LIBRARY,
        };

        for (LandmarkType type : expectedQuestBuildings) {
            assertTrue(questRegistry.hasQuest(type),
                    "BuildingQuestRegistry should have a quest for " + type);
        }
    }

    /**
     * Test 5: Only landmark types with quests get a building NPC spawned.
     *
     * Register two landmarks: GREGGS (has a quest) and WAREHOUSE (no quest).
     * Run the building NPC spawn loop. Verify exactly 1 NPC was spawned
     * (for GREGGS), and 0 for WAREHOUSE.
     */
    @Test
    void test5_OnlyQuestBuildingsGetNPCs() {
        world.addLandmark(new Landmark(LandmarkType.GREGGS, 30, 0, 30, 8, 5, 8));
        world.addLandmark(new Landmark(LandmarkType.WAREHOUSE, 50, 0, 50, 20, 8, 15));

        int before = npcManager.getNPCs().size();

        for (Landmark lm : world.getAllLandmarks()) {
            LandmarkType type = lm.getType();
            if (questRegistry.hasQuest(type)) {
                float x = lm.getPosition().x + lm.getWidth() / 2.0f;
                float z = lm.getPosition().z + lm.getDepth() / 2.0f;
                npcManager.spawnBuildingNPC(type, x, 1, z);
            }
        }

        int after = npcManager.getNPCs().size();
        assertEquals(before + 1, after,
                "Exactly 1 building NPC should be spawned (only GREGGS has a quest)");

        NPC buildingNPC = npcManager.getNPCs().get(npcManager.getNPCs().size() - 1);
        assertEquals(LandmarkType.GREGGS, buildingNPC.getBuildingType(),
                "The spawned building NPC should be associated with GREGGS");
    }
}

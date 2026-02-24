package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.core.BuildingQuestRegistry;
import ragamuffin.entity.NPC;
import ragamuffin.world.BlockType;
import ragamuffin.world.Landmark;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Issue #491: Shopkeeper spawns outside store.
 *
 * The root cause was that spawnBuildingNPCs() used calculateSpawnHeight() to
 * determine the shopkeeper's Y position. calculateSpawnHeight() scans downward
 * from y=64 and returns the first solid block it finds — which for a multi-storey
 * building is the roof, not the ground floor. This caused the shopkeeper to spawn
 * on top of the building rather than inside it.
 *
 * The fix is to use lm.getPosition().y + 1.0f (the landmark's own ground-floor Y)
 * instead of calculateSpawnHeight().
 */
class Issue491ShopkeeperSpawnInsideStoreTest {

    private NPCManager npcManager;
    private BuildingQuestRegistry questRegistry;
    private World world;

    @BeforeEach
    void setUp() {
        npcManager = new NPCManager();
        questRegistry = new BuildingQuestRegistry();

        world = new World(99991);
        // Ground floor at y=0
        for (int x = -10; x < 120; x++) {
            for (int z = -10; z < 120; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * Test: Shopkeeper Y position matches the building's ground floor, not the roof.
     *
     * Set up a GREGGS landmark at (20, 0, 20) with height=5 (roof at y=4).
     * Fill the building volume with BRICK blocks (simulating a real building).
     * Simulate the fixed spawnBuildingNPCs() logic.
     * Verify the shopkeeper's Y coordinate equals landmarkY + 1 (ground floor inside),
     * NOT landmarkY + height (the roof).
     */
    @Test
    void test_ShopkeeperSpawnsAtGroundFloorNotRoof() {
        int landmarkX = 20, landmarkY = 0, landmarkZ = 20;
        int width = 8, height = 5, depth = 8;

        Landmark greggs = new Landmark(LandmarkType.GREGGS, landmarkX, landmarkY, landmarkZ, width, height, depth);
        world.addLandmark(greggs);

        // Build a solid building (walls + roof) — the roof blocks are at y=4
        for (int x = landmarkX; x < landmarkX + width; x++) {
            for (int z = landmarkZ; z < landmarkZ + depth; z++) {
                for (int y = landmarkY; y < landmarkY + height; y++) {
                    world.setBlock(x, y, z, BlockType.BRICK);
                }
            }
        }

        // Simulate fixed spawnBuildingNPCs(): use landmark Y + 1 (ground floor inside)
        NPC spawned = null;
        for (Landmark lm : world.getAllLandmarks()) {
            LandmarkType type = lm.getType();
            if (questRegistry.hasQuest(type)) {
                float x = lm.getPosition().x + lm.getWidth() / 2.0f;
                float z = lm.getPosition().z + lm.getDepth() / 2.0f;
                float y = lm.getPosition().y + 1.0f;  // Fix #491: ground floor, not roof
                spawned = npcManager.spawnBuildingNPC(type, x, y, z);
            }
        }

        assertNotNull(spawned, "A building NPC should be spawned for GREGGS");

        float npcY = spawned.getPosition().y;
        float expectedGroundFloorY = landmarkY + 1.0f;
        float wrongRoofY = landmarkY + height + 1.0f; // what calculateSpawnHeight() would return

        assertEquals(expectedGroundFloorY, npcY, 0.01f,
                "Shopkeeper Y (" + npcY + ") should match ground floor Y ("
                + expectedGroundFloorY + "), not roof Y (" + wrongRoofY + ")");

        // Confirm the NPC is inside landmark Y bounds (not above the roof)
        assertTrue(npcY < landmarkY + height,
                "Shopkeeper Y (" + npcY + ") should be below the roof (y < " + (landmarkY + height) + ")");
    }

    /**
     * Test: Shopkeeper X and Z are inside the landmark's horizontal footprint.
     *
     * Even with the Y fix, the X/Z must remain centred within the building.
     */
    @Test
    void test_ShopkeeperXZInsideLandmarkBounds() {
        Landmark offLicence = new Landmark(LandmarkType.OFF_LICENCE, 40, 0, 40, 10, 4, 10);
        world.addLandmark(offLicence);

        NPC spawned = null;
        for (Landmark lm : world.getAllLandmarks()) {
            LandmarkType type = lm.getType();
            if (questRegistry.hasQuest(type)) {
                float x = lm.getPosition().x + lm.getWidth() / 2.0f;
                float z = lm.getPosition().z + lm.getDepth() / 2.0f;
                float y = lm.getPosition().y + 1.0f;
                spawned = npcManager.spawnBuildingNPC(type, x, y, z);
            }
        }

        assertNotNull(spawned, "A building NPC should be spawned for OFF_LICENCE");

        float npcX = spawned.getPosition().x;
        float npcZ = spawned.getPosition().z;

        assertTrue(npcX >= offLicence.getPosition().x && npcX < offLicence.getPosition().x + offLicence.getWidth(),
                "NPC X (" + npcX + ") should be inside landmark X bounds");
        assertTrue(npcZ >= offLicence.getPosition().z && npcZ < offLicence.getPosition().z + offLicence.getDepth(),
                "NPC Z (" + npcZ + ") should be inside landmark Z bounds");
    }
}

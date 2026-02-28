package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.entity.NPC;
import ragamuffin.world.LandmarkType;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Issue #874: Add unique identification to shopkeepers based on linked shop.
 *
 * Shopkeepers should have unique identification that corresponds to which shop they are
 * linked to. This is exposed via NPC.getShopId(), which returns a string derived from
 * the linked LandmarkType (e.g. "SHOPKEEPER_GREGGS" for the Greggs shopkeeper).
 */
class Issue874ShopkeeperShopIdTest {

    private NPCManager npcManager;

    @BeforeEach
    void setUp() {
        npcManager = new NPCManager();
    }

    /**
     * A shopkeeper spawned for GREGGS should have shopId "SHOPKEEPER_GREGGS".
     */
    @Test
    void test_ShopkeeperLinkedToGreggs_HasGregsShopId() {
        NPC npc = npcManager.spawnBuildingNPC(LandmarkType.GREGGS, 10f, 1f, 10f);
        assertNotNull(npc, "Shopkeeper should be spawned");
        assertEquals("SHOPKEEPER_GREGGS", npc.getShopId(),
                "Shopkeeper linked to GREGGS should have shopId SHOPKEEPER_GREGGS");
    }

    /**
     * A shopkeeper spawned for OFF_LICENCE should have shopId "SHOPKEEPER_OFF_LICENCE".
     */
    @Test
    void test_ShopkeeperLinkedToOffLicence_HasOffLicenceShopId() {
        NPC npc = npcManager.spawnBuildingNPC(LandmarkType.OFF_LICENCE, 20f, 1f, 20f);
        assertNotNull(npc, "Shopkeeper should be spawned");
        assertEquals("SHOPKEEPER_OFF_LICENCE", npc.getShopId(),
                "Shopkeeper linked to OFF_LICENCE should have shopId SHOPKEEPER_OFF_LICENCE");
    }

    /**
     * Each shopkeeper's shopId is unique per shop — two shopkeepers from different shops
     * must have different shopIds.
     */
    @Test
    void test_DifferentShops_ProduceDifferentShopIds() {
        NPC greggs = npcManager.spawnBuildingNPC(LandmarkType.GREGGS, 10f, 1f, 10f);
        NPC offLicence = npcManager.spawnBuildingNPC(LandmarkType.OFF_LICENCE, 20f, 1f, 20f);
        NPC jeweller = npcManager.spawnBuildingNPC(LandmarkType.JEWELLER, 30f, 1f, 30f);

        assertNotNull(greggs);
        assertNotNull(offLicence);
        assertNotNull(jeweller);

        Set<String> ids = new HashSet<>();
        ids.add(greggs.getShopId());
        ids.add(offLicence.getShopId());
        ids.add(jeweller.getShopId());

        assertEquals(3, ids.size(),
                "Each shopkeeper linked to a different shop should have a distinct shopId");
    }

    /**
     * An NPC that has not been linked to any building should return null for shopId.
     */
    @Test
    void test_UnlinkedShopkeeper_HasNullShopId() {
        NPC npc = npcManager.spawnBuildingNPC(LandmarkType.GREGGS, 10f, 1f, 10f);
        assertNotNull(npc);

        // Remove the building link
        npc.setBuildingType(null);
        assertNull(npc.getShopId(),
                "Shopkeeper with no linked building should have null shopId");
    }

    /**
     * The shopId reflects the LandmarkType name so it's stable and predictable.
     * Verify all common shop types produce the expected pattern.
     */
    @Test
    void test_ShopIdMatchesLandmarkTypeName() {
        LandmarkType[] shopTypes = {
            LandmarkType.GREGGS,
            LandmarkType.OFF_LICENCE,
            LandmarkType.CHARITY_SHOP,
            LandmarkType.JEWELLER,
            LandmarkType.BOOKIES
        };

        for (LandmarkType type : shopTypes) {
            NPC npc = npcManager.spawnBuildingNPC(type, 5f, 1f, 5f);
            assertNotNull(npc, "Shopkeeper should be spawned for " + type);
            String expectedId = "SHOPKEEPER_" + type.name();
            assertEquals(expectedId, npc.getShopId(),
                    "shopId for " + type + " should be " + expectedId);
        }
    }
}

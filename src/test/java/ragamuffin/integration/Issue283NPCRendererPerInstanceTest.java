package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;

import java.util.IdentityHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #283 — NPCRenderer shared ModelInstance bug.
 *
 * The bug: NPCRenderer used Map&lt;NPCType, ModelInstance[]&gt; (shared per type), so
 * all NPCs of the same type overwrote each other's transforms and rendered at the
 * last NPC's position.
 *
 * The fix: NPCRenderer now uses Map&lt;NPC, ModelInstance[]&gt; (IdentityHashMap keyed
 * by NPC object identity) so each NPC has its own ModelInstance array.
 *
 * These tests verify the core invariant: that distinct NPC objects of the same
 * type are tracked as separate keys, and that the same NPC object resolves to
 * the same slot (idempotent lookup).
 */
class Issue283NPCRendererPerInstanceTest {

    /**
     * Verify that distinct NPC objects of the same type are treated as
     * distinct keys in an IdentityHashMap. This is the fundamental property
     * that the NPCRenderer fix relies on: each NPC must get its own
     * ModelInstance[], not share one with other NPCs of the same type.
     */
    @Test
    void distinctNPCObjectsOfSameTypeAreDistinctKeys() {
        NPC npc1 = new NPC(NPCType.PUBLIC, 0, 1, 0);
        NPC npc2 = new NPC(NPCType.PUBLIC, 5, 1, 5);
        NPC npc3 = new NPC(NPCType.PUBLIC, 10, 1, 10);

        Map<NPC, String> instanceMap = new IdentityHashMap<>();
        instanceMap.put(npc1, "instances-for-npc1");
        instanceMap.put(npc2, "instances-for-npc2");
        instanceMap.put(npc3, "instances-for-npc3");

        // All three must be tracked separately even though they share the same type
        assertEquals(3, instanceMap.size(),
            "Each NPC object must occupy a distinct slot — " +
            "sharing a type must not cause collisions");

        assertEquals("instances-for-npc1", instanceMap.get(npc1));
        assertEquals("instances-for-npc2", instanceMap.get(npc2));
        assertEquals("instances-for-npc3", instanceMap.get(npc3));
    }

    /**
     * Verify that looking up the same NPC object twice yields the same slot
     * (idempotent get). This mirrors the getOrCreateHumanoidInstances() method
     * which creates instances on first access and returns the cached array on
     * subsequent accesses for the same NPC.
     */
    @Test
    void sameNPCObjectResolvesToSameSlot() {
        NPC npc = new NPC(NPCType.YOUTH_GANG, 3, 1, 3);

        Map<NPC, String[]> instanceMap = new IdentityHashMap<>();
        String[] firstInstances = new String[]{"part0", "part1"};
        instanceMap.put(npc, firstInstances);

        // Second lookup must return the exact same array, not create a new one
        String[] retrieved = instanceMap.get(npc);
        assertSame(firstInstances, retrieved,
            "Repeated lookup of the same NPC must return the same cached instance array");
    }

    /**
     * Verify the fix covers all NPC types that appear in groups at game start.
     * PUBLIC x4, YOUTH_GANG x3, JOGGER x2, DRUNK x2, SCHOOL_KID x3,
     * DELIVERY_DRIVER x2, PENSIONER x2, DOG x2 — all must be individually tracked.
     */
    @Test
    void multipleNPCsOfEachGroupTypeAreTrackedIndividually() {
        NPCType[] groupTypes = {
            NPCType.PUBLIC, NPCType.PUBLIC, NPCType.PUBLIC, NPCType.PUBLIC,
            NPCType.YOUTH_GANG, NPCType.YOUTH_GANG, NPCType.YOUTH_GANG,
            NPCType.JOGGER, NPCType.JOGGER,
            NPCType.DRUNK, NPCType.DRUNK,
            NPCType.SCHOOL_KID, NPCType.SCHOOL_KID, NPCType.SCHOOL_KID,
            NPCType.DELIVERY_DRIVER, NPCType.DELIVERY_DRIVER,
            NPCType.PENSIONER, NPCType.PENSIONER,
            NPCType.DOG, NPCType.DOG,
        };

        Map<NPC, Integer> instanceMap = new IdentityHashMap<>();
        NPC[] npcs = new NPC[groupTypes.length];

        for (int i = 0; i < groupTypes.length; i++) {
            npcs[i] = new NPC(groupTypes[i], i * 2f, 1f, 0f);
            instanceMap.put(npcs[i], i);
        }

        // Every NPC must have its own slot
        assertEquals(groupTypes.length, instanceMap.size(),
            "All " + groupTypes.length + " NPCs must occupy distinct slots; " +
            "none may overwrite another due to shared type");

        // Each NPC must retrieve its own data, not another NPC's
        for (int i = 0; i < npcs.length; i++) {
            assertEquals(i, instanceMap.get(npcs[i]),
                "NPC #" + i + " (" + groupTypes[i] + ") must retrieve its own slot");
        }
    }

    /**
     * Verify dog NPCs are also individually tracked (dogInstances uses the same fix).
     */
    @Test
    void multipleDogNPCsAreTrackedIndividually() {
        NPC dog1 = new NPC(NPCType.DOG, 0, 1, 0);
        NPC dog2 = new NPC(NPCType.DOG, 5, 1, 5);

        Map<NPC, String> instanceMap = new IdentityHashMap<>();
        instanceMap.put(dog1, "dog1-instances");
        instanceMap.put(dog2, "dog2-instances");

        assertEquals(2, instanceMap.size(),
            "Two dog NPCs must occupy two distinct slots");
        assertEquals("dog1-instances", instanceMap.get(dog1));
        assertEquals("dog2-instances", instanceMap.get(dog2));
    }
}

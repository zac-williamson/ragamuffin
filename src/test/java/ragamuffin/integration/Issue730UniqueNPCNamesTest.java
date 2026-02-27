package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #730 Integration Tests — NPCs should have unique names for immersion.
 *
 * Humanoid NPCs (PUBLIC, YOUTH_GANG, SHOPKEEPER, etc.) must receive a unique
 * British full name when spawned.  Non-humanoid and role-anonymous types
 * (DOG, BIRD, POLICE, PCSO, ARMED_RESPONSE, COUNCIL_BUILDER) must NOT receive
 * a personal name.
 *
 * The pre-existing named NPCs ("Brother Desmond", "Maureen") are spawned via
 * {@code spawnNamedNPC} and already satisfy the named-NPC requirement; they are
 * tested separately.  This test focuses on the general-purpose {@code spawnNPC}
 * path which now auto-assigns names.
 */
class Issue730UniqueNPCNamesTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: Humanoid NPC types receive a non-empty name on spawn
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Spawn one of each humanoid NPC type and verify every resulting NPC
     * has a non-empty name assigned.
     */
    @Test
    void test1_HumanoidNPCsHaveNamesAfterSpawn() {
        NPCManager manager = new NPCManager();

        NPCType[] humanoidTypes = {
            NPCType.PUBLIC,
            NPCType.YOUTH_GANG,
            NPCType.COUNCIL_MEMBER,
            NPCType.SHOPKEEPER,
            NPCType.POSTMAN,
            NPCType.JOGGER,
            NPCType.DRUNK,
            NPCType.BUSKER,
            NPCType.DELIVERY_DRIVER,
            NPCType.PENSIONER,
            NPCType.SCHOOL_KID,
            NPCType.FENCE,
            NPCType.TUNNEL_DWELLER,
            NPCType.BARMAN,
            NPCType.BOUNCER,
            NPCType.STREET_LAD,
            NPCType.ACCOMPLICE,
            NPCType.ESTATE_AGENT,
            NPCType.THUG,
            NPCType.RAVE_ATTENDEE,
        };

        for (NPCType type : humanoidTypes) {
            NPC npc = manager.spawnNPC(type, 0f, 1f, 0f);
            assertNotNull(npc, "spawnNPC returned null for type " + type);
            assertTrue(npc.isNamed(),
                "Expected a name for NPC of type " + type + " but got: " + npc.getName());
            assertFalse(npc.getName().isBlank(),
                "Name must not be blank for type " + type);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Non-humanoid / role-anonymous NPC types do NOT receive a name
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dog, Bird, Police, PCSO, Armed Response, and Council Builder should NOT
     * receive personal names — they are identified by role, not by name.
     */
    @Test
    void test2_RoleAnonymousNPCsHaveNoName() {
        NPCManager manager = new NPCManager();

        NPCType[] anonymousTypes = {
            NPCType.DOG,
            NPCType.BIRD,
            NPCType.POLICE,
            NPCType.PCSO,
            NPCType.ARMED_RESPONSE,
            NPCType.COUNCIL_BUILDER,
        };

        for (NPCType type : anonymousTypes) {
            NPC npc = manager.spawnNPC(type, 0f, 1f, 0f);
            assertNotNull(npc, "spawnNPC returned null for type " + type);
            assertFalse(npc.isNamed(),
                "Type " + type + " should NOT have a name, but has: " + npc.getName());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: All spawned humanoid NPCs have distinct names
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Spawn 30 PUBLIC NPCs and verify that every one of them has a unique name
     * — no two NPCs share the same full name.
     */
    @Test
    void test3_SpawnedNPCNamesAreUnique() {
        NPCManager manager = new NPCManager();

        int count = 30;
        Set<String> names = new HashSet<>();
        for (int i = 0; i < count; i++) {
            NPC npc = manager.spawnNPC(NPCType.PUBLIC, (float) i, 1f, 0f);
            assertNotNull(npc, "spawnNPC returned null at index " + i);
            assertTrue(npc.isNamed(), "NPC at index " + i + " has no name");
            boolean added = names.add(npc.getName());
            assertTrue(added, "Duplicate name detected: " + npc.getName());
        }
        assertEquals(count, names.size(), "Expected " + count + " unique names");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4: Names consist of a first name and a surname separated by a space
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Each auto-generated name must be in "First Surname" format — exactly one
     * space separating two non-empty parts.
     */
    @Test
    void test4_NamesHaveFirstAndSurnameFormat() {
        NPCManager manager = new NPCManager();

        for (int i = 0; i < 20; i++) {
            NPC npc = manager.spawnNPC(NPCType.PUBLIC, (float) i, 1f, (float) i);
            assertNotNull(npc);
            String name = npc.getName();
            assertNotNull(name, "Name must not be null");
            String[] parts = name.split(" ");
            assertTrue(parts.length >= 2,
                "Name '" + name + "' does not contain at least a first name and surname");
            assertFalse(parts[0].isEmpty(), "First name part must not be empty in: " + name);
            assertFalse(parts[1].isEmpty(), "Surname part must not be empty in: " + name);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5: spawnNamedNPC preserves the explicitly given name
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calling {@code spawnNamedNPC} with a specific name must result in the NPC
     * bearing exactly that name — the auto-name logic must not overwrite it.
     */
    @Test
    void test5_SpawnNamedNPCPreservesExplicitName() {
        NPCManager manager = new NPCManager();

        NPC desmond = manager.spawnNamedNPC(NPCType.STREET_PREACHER, "Brother Desmond", 12f, 1f, 22f);
        assertNotNull(desmond);
        assertEquals("Brother Desmond", desmond.getName(),
            "spawnNamedNPC must preserve the explicitly provided name");

        NPC maureen = manager.spawnNamedNPC(NPCType.LOLLIPOP_LADY, "Maureen", -26f, 1f, -50f);
        assertNotNull(maureen);
        assertEquals("Maureen", maureen.getName(),
            "spawnNamedNPC must preserve the explicitly provided name");
    }
}

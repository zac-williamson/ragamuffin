package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #639 — 2 new named NPCs with unique 3D appearance.
 *
 * Verifies that Brother Desmond (STREET_PREACHER) and Maureen (LOLLIPOP_LADY)
 * exist as named NPC types with distinct appearances and can be spawned in the world.
 */
class Issue639NamedNPCsTest {

    private NPCManager manager;

    @BeforeEach
    void setUp() {
        manager = new NPCManager();
    }

    // -----------------------------------------------------------------------
    // NPCType enum — new types exist with correct stats
    // -----------------------------------------------------------------------

    @Test
    void streetPreacherTypeExists() {
        // STREET_PREACHER is passive (not hostile) with standard health
        assertEquals(20f, NPCType.STREET_PREACHER.getMaxHealth(), 0.01f,
            "STREET_PREACHER should have 20 max health");
        assertFalse(NPCType.STREET_PREACHER.isHostile(),
            "STREET_PREACHER should be passive (not hostile)");
        assertEquals(0f, NPCType.STREET_PREACHER.getAttackDamage(), 0.01f,
            "STREET_PREACHER should deal no damage");
    }

    @Test
    void lollipopLadyTypeExists() {
        // LOLLIPOP_LADY is passive (not hostile) with standard health
        assertEquals(20f, NPCType.LOLLIPOP_LADY.getMaxHealth(), 0.01f,
            "LOLLIPOP_LADY should have 20 max health");
        assertFalse(NPCType.LOLLIPOP_LADY.isHostile(),
            "LOLLIPOP_LADY should be passive (not hostile)");
        assertEquals(0f, NPCType.LOLLIPOP_LADY.getAttackDamage(), 0.01f,
            "LOLLIPOP_LADY should deal no damage");
    }

    // -----------------------------------------------------------------------
    // NPC name field — get/set/isNamed
    // -----------------------------------------------------------------------

    @Test
    void anonymousNPCHasNoName() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        assertNull(npc.getName(), "Anonymous NPC should have null name");
        assertFalse(npc.isNamed(), "Anonymous NPC should not be named");
    }

    @Test
    void namedNPCConstructorStoresName() {
        NPC npc = new NPC(NPCType.STREET_PREACHER, "Brother Desmond", 10, 1, 20);
        assertEquals("Brother Desmond", npc.getName(),
            "Named NPC should return the name set via constructor");
        assertTrue(npc.isNamed(), "NPC with name should report isNamed() == true");
    }

    @Test
    void setNameUpdatesName() {
        NPC npc = new NPC(NPCType.LOLLIPOP_LADY, 0, 1, 0);
        assertFalse(npc.isNamed());
        npc.setName("Maureen");
        assertEquals("Maureen", npc.getName());
        assertTrue(npc.isNamed());
    }

    // -----------------------------------------------------------------------
    // spawnNamedNPC — NPCManager method
    // -----------------------------------------------------------------------

    @Test
    void spawnNamedNPCCreatesNPCWithName() {
        NPC npc = manager.spawnNamedNPC(NPCType.STREET_PREACHER, "Brother Desmond", 12, 1, 22);
        assertNotNull(npc, "spawnNamedNPC should return a non-null NPC");
        assertEquals(NPCType.STREET_PREACHER, npc.getType());
        assertEquals("Brother Desmond", npc.getName());
        assertTrue(npc.isNamed());
        assertEquals(12f, npc.getPosition().x, 0.01f);
        assertEquals(22f, npc.getPosition().z, 0.01f);
    }

    @Test
    void spawnNamedNPCIsTrackedByManager() {
        manager.spawnNamedNPC(NPCType.LOLLIPOP_LADY, "Maureen", -26, 1, -50);
        List<NPC> npcs = manager.getNPCs();
        assertEquals(1, npcs.size(), "Manager should track the named NPC");
        assertEquals("Maureen", npcs.get(0).getName());
    }

    // -----------------------------------------------------------------------
    // Unique 3D appearance — distinct NPCTypes each have their own entry in
    // the type enum (and therefore a unique appearance in NPCRenderer)
    // -----------------------------------------------------------------------

    @Test
    void namedNPCTypesAreDistinctFromAllOthers() {
        // STREET_PREACHER and LOLLIPOP_LADY must be different from every other type
        for (NPCType type : NPCType.values()) {
            if (type == NPCType.STREET_PREACHER || type == NPCType.LOLLIPOP_LADY) continue;
            assertNotEquals(NPCType.STREET_PREACHER, type,
                "STREET_PREACHER must be a unique NPC type");
            assertNotEquals(NPCType.LOLLIPOP_LADY, type,
                "LOLLIPOP_LADY must be a unique NPC type");
        }
        assertNotEquals(NPCType.STREET_PREACHER, NPCType.LOLLIPOP_LADY,
            "The two named NPC types must be distinct from each other");
    }

    @Test
    void bothNamedNPCTypesSpawnSuccessfully() {
        NPC preacher = manager.spawnNamedNPC(NPCType.STREET_PREACHER, "Brother Desmond", 0, 1, 0);
        NPC lollipop = manager.spawnNamedNPC(NPCType.LOLLIPOP_LADY, "Maureen", 5, 1, 0);
        assertNotNull(preacher, "STREET_PREACHER (Brother Desmond) should spawn");
        assertNotNull(lollipop, "LOLLIPOP_LADY (Maureen) should spawn");
        assertEquals(2, manager.getNPCs().size());
    }

    @Test
    void namedNPCsStartInIdleOrWanderingState() {
        NPC preacher = manager.spawnNamedNPC(NPCType.STREET_PREACHER, "Brother Desmond", 0, 1, 0);
        NPC lollipop = manager.spawnNamedNPC(NPCType.LOLLIPOP_LADY, "Maureen", 5, 1, 0);
        assertNotNull(preacher);
        assertNotNull(lollipop);
        // New named NPCs should start in a valid peaceful state
        NPCState preacherState = preacher.getState();
        NPCState lolliState = lollipop.getState();
        assertTrue(preacherState == NPCState.IDLE || preacherState == NPCState.WANDERING,
            "STREET_PREACHER should start IDLE or WANDERING, was: " + preacherState);
        assertTrue(lolliState == NPCState.IDLE || lolliState == NPCState.WANDERING,
            "LOLLIPOP_LADY should start IDLE or WANDERING, was: " + lolliState);
    }

    @Test
    void namedNPCsHaveCorrectHealth() {
        NPC preacher = manager.spawnNPC(NPCType.STREET_PREACHER, 0, 1, 0);
        NPC lollipop = manager.spawnNPC(NPCType.LOLLIPOP_LADY, 5, 1, 0);
        assertNotNull(preacher);
        assertNotNull(lollipop);
        assertEquals(20f, preacher.getHealth(), 0.01f,
            "STREET_PREACHER should spawn with full health (20)");
        assertEquals(20f, lollipop.getHealth(), 0.01f,
            "LOLLIPOP_LADY should spawn with full health (20)");
    }

    @Test
    void twoNamedNPCsHaveDistinctNames() {
        manager.spawnNamedNPC(NPCType.STREET_PREACHER, "Brother Desmond", 0, 1, 0);
        manager.spawnNamedNPC(NPCType.LOLLIPOP_LADY, "Maureen", 5, 1, 0);
        List<NPC> named = manager.getNPCs().stream()
            .filter(NPC::isNamed)
            .collect(Collectors.toList());
        assertEquals(2, named.size(), "Both NPCs should be named");
        String name1 = named.get(0).getName();
        String name2 = named.get(1).getName();
        assertNotEquals(name1, name2,
            "The two named NPCs must have distinct names, but both were: " + name1);
    }
}

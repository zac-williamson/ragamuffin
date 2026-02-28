package ragamuffin.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NPC hairstyle and facial hair systems (Issue #875).
 * Verifies that NPCs can be assigned hairstyle and facial hair options
 * and that the defaults, getters, setters, and null-safety all behave correctly.
 */
class NPCAppearanceTest {

    // ── HairstyleType enum ───────────────────────────────────────────────────

    @Test
    void allHairstyleValuesExist() {
        // Verify the expected hairstyle values are present
        HairstyleType[] values = HairstyleType.values();
        assertTrue(values.length >= 5, "Expected at least 5 HairstyleType values");

        // Spot-check key names
        assertNotNull(HairstyleType.NONE);
        assertNotNull(HairstyleType.SHORT);
        assertNotNull(HairstyleType.LONG);
        assertNotNull(HairstyleType.MOHAWK);
        assertNotNull(HairstyleType.CURLY);
        assertNotNull(HairstyleType.BUZZCUT);
    }

    @Test
    void hairstyleValuesAreDistinct() {
        HairstyleType[] values = HairstyleType.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals(values[i], values[j],
                    "HairstyleType values should be distinct: " + values[i] + " vs " + values[j]);
            }
        }
    }

    // ── FacialHairType enum ──────────────────────────────────────────────────

    @Test
    void allFacialHairValuesExist() {
        FacialHairType[] values = FacialHairType.values();
        assertTrue(values.length >= 4, "Expected at least 4 FacialHairType values");

        assertNotNull(FacialHairType.NONE);
        assertNotNull(FacialHairType.STUBBLE);
        assertNotNull(FacialHairType.MOUSTACHE);
        assertNotNull(FacialHairType.BEARD);
        assertNotNull(FacialHairType.GOATEE);
    }

    @Test
    void facialHairValuesAreDistinct() {
        FacialHairType[] values = FacialHairType.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals(values[i], values[j],
                    "FacialHairType values should be distinct: " + values[i] + " vs " + values[j]);
            }
        }
    }

    // ── NPC defaults ─────────────────────────────────────────────────────────

    @Test
    void npcDefaultsToShortHairstyle() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);
        assertEquals(HairstyleType.SHORT, npc.getHairstyle(),
            "NPC should default to SHORT hairstyle on construction");
    }

    @Test
    void npcDefaultsToNoFacialHair() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);
        assertEquals(FacialHairType.NONE, npc.getFacialHair(),
            "NPC should default to NONE facial hair on construction");
    }

    // ── HairstyleType getters/setters ────────────────────────────────────────

    @Test
    void npcHairstyleCanBeSetToLong() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);
        npc.setHairstyle(HairstyleType.LONG);
        assertEquals(HairstyleType.LONG, npc.getHairstyle());
    }

    @Test
    void npcHairstyleCanBeSetToMohawk() {
        NPC npc = new NPC(NPCType.YOUTH_GANG, 5, 1, 5);
        npc.setHairstyle(HairstyleType.MOHAWK);
        assertEquals(HairstyleType.MOHAWK, npc.getHairstyle());
    }

    @Test
    void npcHairstyleCanBeSetToNone() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);
        npc.setHairstyle(HairstyleType.NONE);
        assertEquals(HairstyleType.NONE, npc.getHairstyle());
    }

    @Test
    void npcHairstyleCanBeSetToCurly() {
        NPC npc = new NPC(NPCType.BUSKER, 0, 1, 0);
        npc.setHairstyle(HairstyleType.CURLY);
        assertEquals(HairstyleType.CURLY, npc.getHairstyle());
    }

    @Test
    void npcHairstyleCanBeSetToBuzzcut() {
        NPC npc = new NPC(NPCType.JOGGER, 0, 1, 0);
        npc.setHairstyle(HairstyleType.BUZZCUT);
        assertEquals(HairstyleType.BUZZCUT, npc.getHairstyle());
    }

    @Test
    void settingNullHairstyleFallsBackToShort() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);
        npc.setHairstyle(HairstyleType.LONG);
        npc.setHairstyle(null);
        assertEquals(HairstyleType.SHORT, npc.getHairstyle(),
            "Setting null hairstyle should fall back to SHORT");
    }

    // ── FacialHairType getters/setters ───────────────────────────────────────

    @Test
    void npcFacialHairCanBeSetToBeard() {
        NPC npc = new NPC(NPCType.DRUNK, 0, 0, 0);
        npc.setFacialHair(FacialHairType.BEARD);
        assertEquals(FacialHairType.BEARD, npc.getFacialHair());
    }

    @Test
    void npcFacialHairCanBeSetToStubble() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);
        npc.setFacialHair(FacialHairType.STUBBLE);
        assertEquals(FacialHairType.STUBBLE, npc.getFacialHair());
    }

    @Test
    void npcFacialHairCanBeSetToMoustache() {
        NPC npc = new NPC(NPCType.PENSIONER, 0, 1, 0);
        npc.setFacialHair(FacialHairType.MOUSTACHE);
        assertEquals(FacialHairType.MOUSTACHE, npc.getFacialHair());
    }

    @Test
    void npcFacialHairCanBeSetToGoatee() {
        NPC npc = new NPC(NPCType.COUNCIL_MEMBER, 0, 1, 0);
        npc.setFacialHair(FacialHairType.GOATEE);
        assertEquals(FacialHairType.GOATEE, npc.getFacialHair());
    }

    @Test
    void settingNullFacialHairFallsBackToNone() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);
        npc.setFacialHair(FacialHairType.BEARD);
        npc.setFacialHair(null);
        assertEquals(FacialHairType.NONE, npc.getFacialHair(),
            "Setting null facial hair should fall back to NONE");
    }

    // ── Independence from other NPC properties ───────────────────────────────

    @Test
    void hairstyleDoesNotAffectNPCType() {
        NPC npc = new NPC(NPCType.POLICE, 0, 1, 0);
        npc.setHairstyle(HairstyleType.MOHAWK);
        assertEquals(NPCType.POLICE, npc.getType(),
            "Changing hairstyle must not change the NPC type");
    }

    @Test
    void facialHairDoesNotAffectNPCHealth() {
        NPC npc = new NPC(NPCType.POLICE, 0, 1, 0);
        float fullHealth = npc.getHealth();
        npc.setFacialHair(FacialHairType.BEARD);
        assertEquals(fullHealth, npc.getHealth(), 0.001f,
            "Changing facial hair must not affect NPC health");
    }

    @Test
    void hairstyleAndFacialHairAreIndependent() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);
        npc.setHairstyle(HairstyleType.LONG);
        npc.setFacialHair(FacialHairType.BEARD);
        assertEquals(HairstyleType.LONG, npc.getHairstyle(),
            "Hairstyle should be unaffected by setting facial hair");
        assertEquals(FacialHairType.BEARD, npc.getFacialHair(),
            "Facial hair should be unaffected by setting hairstyle");
    }

    @Test
    void hairstyleDoesNotAffectModelVariant() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);
        npc.setModelVariant(NPCModelVariant.TALL);
        npc.setHairstyle(HairstyleType.MOHAWK);
        assertEquals(NPCModelVariant.TALL, npc.getModelVariant(),
            "Setting hairstyle must not change the model variant");
    }

    // ── All types accept all values ───────────────────────────────────────────

    @Test
    void allNPCTypesCanReceiveAnyHairstyle() {
        for (NPCType type : NPCType.values()) {
            for (HairstyleType style : HairstyleType.values()) {
                NPC npc = new NPC(type, 0, 1, 0);
                npc.setHairstyle(style);
                assertEquals(style, npc.getHairstyle(),
                    type + " should accept hairstyle " + style);
            }
        }
    }

    @Test
    void allNPCTypesCanReceiveAnyFacialHair() {
        for (NPCType type : NPCType.values()) {
            for (FacialHairType fh : FacialHairType.values()) {
                NPC npc = new NPC(type, 0, 1, 0);
                npc.setFacialHair(fh);
                assertEquals(fh, npc.getFacialHair(),
                    type + " should accept facial hair " + fh);
            }
        }
    }
}

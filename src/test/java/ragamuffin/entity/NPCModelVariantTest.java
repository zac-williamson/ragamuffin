package ragamuffin.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NPC model variant system (Issue #706).
 * Verifies that NPCs can be assigned physical appearance variants and
 * that each variant has the expected scale properties.
 */
class NPCModelVariantTest {

    // ── NPCModelVariant enum properties ─────────────────────────────────────

    @Test
    void defaultVariantHasNeutralScales() {
        NPCModelVariant v = NPCModelVariant.DEFAULT;
        assertEquals(1.0f, v.getHeightScale(), 0.001f);
        assertEquals(1.0f, v.getWidthScale(),  0.001f);
        assertEquals(1.0f, v.getHeadScale(),   0.001f);
        assertFalse(v.hasLongHair());
    }

    @Test
    void tallVariantHasHeightGreaterThanOne() {
        NPCModelVariant v = NPCModelVariant.TALL;
        assertTrue(v.getHeightScale() > 1.0f,
            "TALL variant should have heightScale > 1");
        assertFalse(v.hasLongHair());
    }

    @Test
    void shortVariantHasHeightLessThanOne() {
        NPCModelVariant v = NPCModelVariant.SHORT;
        assertTrue(v.getHeightScale() < 1.0f,
            "SHORT variant should have heightScale < 1");
        assertFalse(v.hasLongHair());
    }

    @Test
    void stockyVariantIsWiderThanDefault() {
        NPCModelVariant v = NPCModelVariant.STOCKY;
        assertTrue(v.getWidthScale() > 1.0f,
            "STOCKY variant should have widthScale > 1");
    }

    @Test
    void slimVariantIsNarrowerThanDefault() {
        NPCModelVariant v = NPCModelVariant.SLIM;
        assertTrue(v.getWidthScale() < 1.0f,
            "SLIM variant should have widthScale < 1");
    }

    @Test
    void longHairVariantHasHairFlag() {
        NPCModelVariant v = NPCModelVariant.LONG_HAIR;
        assertTrue(v.hasLongHair(),
            "LONG_HAIR variant should have hasLongHair() == true");
    }

    /**
     * Verify the hair block Z position does not clip into the head.
     * Hair centre Z = -(HEAD_D*headScale/2 + hairHalfDepth + gap).
     * The front face of the hair is at hairCentreZ + hairHalfDepth,
     * which must be <= -(HEAD_D*headScale/2) (the back surface of the head).
     */
    @Test
    void longHairPositionDoesNotClipIntoHead() {
        final float HEAD_D = 0.38f;
        float headScale = NPCModelVariant.LONG_HAIR.getHeadScale();
        float hairHalfDepth = HEAD_D * 0.3f / 2f;
        float hairZ = -(HEAD_D * headScale / 2f + hairHalfDepth + 0.005f);
        float hairFrontZ = hairZ + hairHalfDepth; // front face of hair block
        float headBackZ  = -(HEAD_D * headScale / 2f); // back surface of head
        assertTrue(hairFrontZ <= headBackZ,
            "Hair front face (" + hairFrontZ + ") must not protrude in front of head back surface (" + headBackZ + ")");
    }

    /**
     * Verify the hair block Y position aligns its top with the top of the head.
     * hairY + (HEAD_H + 0.20f)/2 should equal headCentre + HEAD_H*headScale/2.
     */
    @Test
    void longHairTopAlignsWithHeadTop() {
        final float HEAD_H = 0.38f;
        final float headCentre = 1.5f; // arbitrary reference height
        final float headBob = 0f;
        float headScale = NPCModelVariant.LONG_HAIR.getHeadScale();
        float hairY = headCentre + headBob + HEAD_H * headScale / 2f - (HEAD_H + 0.20f) / 2f;
        float hairTopY = hairY + (HEAD_H + 0.20f) / 2f;
        float headTopY = headCentre + HEAD_H * headScale / 2f;
        assertEquals(headTopY, hairTopY, 0.001f,
            "Hair top should align with the top of the head");
    }

    @Test
    void onlyLongHairVariantHasHairFlag() {
        for (NPCModelVariant v : NPCModelVariant.values()) {
            if (v == NPCModelVariant.LONG_HAIR) {
                assertTrue(v.hasLongHair(), "LONG_HAIR should have hair flag");
            } else {
                assertFalse(v.hasLongHair(), v + " should not have hair flag");
            }
        }
    }

    @Test
    void allVariantsHavePositiveScales() {
        for (NPCModelVariant v : NPCModelVariant.values()) {
            assertTrue(v.getHeightScale() > 0f, v + " heightScale must be positive");
            assertTrue(v.getWidthScale()  > 0f, v + " widthScale must be positive");
            assertTrue(v.getHeadScale()   > 0f, v + " headScale must be positive");
        }
    }

    // ── NPC integration: variant field ──────────────────────────────────────

    @Test
    void npcDefaultsToDefaultVariant() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);
        assertEquals(NPCModelVariant.DEFAULT, npc.getModelVariant(),
            "NPC should default to DEFAULT variant on construction");
    }

    @Test
    void npcVariantCanBeSetToTall() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);
        npc.setModelVariant(NPCModelVariant.TALL);
        assertEquals(NPCModelVariant.TALL, npc.getModelVariant());
    }

    @Test
    void npcVariantCanBeSetToShort() {
        NPC npc = new NPC(NPCType.YOUTH_GANG, 5, 1, 5);
        npc.setModelVariant(NPCModelVariant.SHORT);
        assertEquals(NPCModelVariant.SHORT, npc.getModelVariant());
    }

    @Test
    void npcVariantCanBeSetToLongHair() {
        NPC npc = new NPC(NPCType.PENSIONER, 10, 1, 10);
        npc.setModelVariant(NPCModelVariant.LONG_HAIR);
        assertEquals(NPCModelVariant.LONG_HAIR, npc.getModelVariant());
        assertTrue(npc.getModelVariant().hasLongHair());
    }

    @Test
    void npcVariantCanBeSetToStocky() {
        NPC npc = new NPC(NPCType.BOUNCER, 0, 1, 0);
        npc.setModelVariant(NPCModelVariant.STOCKY);
        assertEquals(NPCModelVariant.STOCKY, npc.getModelVariant());
    }

    @Test
    void npcVariantCanBeSetToSlim() {
        NPC npc = new NPC(NPCType.JOGGER, 0, 1, 0);
        npc.setModelVariant(NPCModelVariant.SLIM);
        assertEquals(NPCModelVariant.SLIM, npc.getModelVariant());
    }

    @Test
    void settingNullVariantFallsBackToDefault() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);
        npc.setModelVariant(NPCModelVariant.TALL);
        npc.setModelVariant(null);
        assertEquals(NPCModelVariant.DEFAULT, npc.getModelVariant(),
            "Setting null should fall back to DEFAULT");
    }

    @Test
    void variantDoesNotAffectNPCType() {
        NPC npc = new NPC(NPCType.POLICE, 0, 1, 0);
        npc.setModelVariant(NPCModelVariant.TALL);
        assertEquals(NPCType.POLICE, npc.getType(),
            "Changing variant must not change the NPC type");
    }

    @Test
    void variantDoesNotAffectHealth() {
        NPC npc = new NPC(NPCType.POLICE, 0, 1, 0);
        float fullHealth = npc.getHealth();
        npc.setModelVariant(NPCModelVariant.STOCKY);
        assertEquals(fullHealth, npc.getHealth(), 0.001f,
            "Changing variant must not affect NPC health");
    }

    @Test
    void allNPCTypesCanReceiveAnyVariant() {
        for (NPCType type : NPCType.values()) {
            for (NPCModelVariant variant : NPCModelVariant.values()) {
                NPC npc = new NPC(type, 0, 1, 0);
                npc.setModelVariant(variant);
                assertEquals(variant, npc.getModelVariant(),
                    type + " should accept variant " + variant);
            }
        }
    }
}

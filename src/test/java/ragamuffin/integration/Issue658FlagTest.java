package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.render.FlagRenderer;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #658: Add animated flags.
 *
 * Verifies that animated flag poles are placed on key civic buildings during
 * world generation, that the flag positions are correctly registered with the
 * world, and that the FlagRenderer animation timer advances properly.
 */
public class Issue658FlagTest {

    private World world;

    @BeforeEach
    public void setUp() {
        HeadlessTestHelper.initHeadless();
        world = new World(0);
        world.generate();
    }

    /**
     * Verify that the world registers at least one flag position after generation.
     */
    @Test
    public void testFlagPositionsExist() {
        List<FlagPosition> flags = world.getFlagPositions();
        assertNotNull(flags, "getFlagPositions() should never return null");
        assertFalse(flags.isEmpty(),
                "At least one flag pole should be registered after world generation");
    }

    /**
     * Verify that exactly 6 flag poles are registered (one per civic building).
     */
    @Test
    public void testSixFlagPolesRegistered() {
        List<FlagPosition> flags = world.getFlagPositions();
        assertEquals(6, flags.size(),
                "Exactly 6 flag poles should be registered (office, jobcentre, police, "
                + "fire station, school, council flats); found " + flags.size());
    }

    /**
     * Verify that all flag positions have valid world coordinates (not all zero).
     */
    @Test
    public void testFlagPositionsHaveValidCoordinates() {
        List<FlagPosition> flags = world.getFlagPositions();
        assertFalse(flags.isEmpty(), "Flag list must not be empty for this test to run");

        for (FlagPosition flag : flags) {
            // Y should be above ground level (poles are built on top of buildings)
            assertTrue(flag.getWorldY() > 1.0f,
                    "Flag Y coordinate should be well above ground (got " + flag.getWorldY() + ")");
        }
    }

    /**
     * Verify that all flag positions have distinct phase offsets so they do not
     * wave in perfect unison — visual variety is the core feature of this issue.
     */
    @Test
    public void testFlagPhasesAreDistinct() {
        List<FlagPosition> flags = world.getFlagPositions();
        if (flags.size() < 2) return; // Need at least 2 flags to check distinctness

        java.util.Set<Float> phases = new java.util.HashSet<>();
        for (FlagPosition flag : flags) {
            phases.add(flag.getPhaseOffset());
        }
        // All 6 flags should have different phase offsets
        assertEquals(flags.size(), phases.size(),
                "Each flag should have a unique phase offset to desynchronise animations");
    }

    /**
     * Verify that flag colour components are within the valid [0, 1] range.
     */
    @Test
    public void testFlagColourComponentsAreInRange() {
        for (FlagPosition flag : world.getFlagPositions()) {
            assertTrue(flag.getColorR1() >= 0f && flag.getColorR1() <= 1f, "R1 out of range");
            assertTrue(flag.getColorG1() >= 0f && flag.getColorG1() <= 1f, "G1 out of range");
            assertTrue(flag.getColorB1() >= 0f && flag.getColorB1() <= 1f, "B1 out of range");
            assertTrue(flag.getColorR2() >= 0f && flag.getColorR2() <= 1f, "R2 out of range");
            assertTrue(flag.getColorG2() >= 0f && flag.getColorG2() <= 1f, "G2 out of range");
            assertTrue(flag.getColorB2() >= 0f && flag.getColorB2() <= 1f, "B2 out of range");
        }
    }

    /**
     * Verify that the FlagRenderer accepts the world's flag positions and stores them.
     */
    @Test
    public void testFlagRendererAcceptsPositions() {
        FlagRenderer renderer = new FlagRenderer();
        renderer.setFlags(world.getFlagPositions());
        assertEquals(world.getFlagPositions().size(), renderer.getFlags().size(),
                "FlagRenderer should store all flag positions provided to it");
    }

    /**
     * Verify that the FlagRenderer animation timer advances when update() is called.
     * This is the core animation mechanism — if time does not advance, flags will not wave.
     *
     * We test this indirectly: the renderer must not throw an exception after advancing
     * time by several seconds (the animation path is exercised).
     */
    @Test
    public void testFlagRendererUpdateAdvancesTime() {
        FlagRenderer renderer = new FlagRenderer();
        renderer.setFlags(world.getFlagPositions());

        // Advance time by a full wave cycle; should not throw
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 60; i++) {
                renderer.update(1f / 60f); // 60 frames at 60 fps = 1 second
            }
        }, "FlagRenderer.update() should not throw over 60 frames");
    }

    /**
     * Verify that IRON_FENCE blocks (flag poles) are present above the office
     * building roof.  The office is placed at (offX + 2, roofHeight+1 .. +3)
     * in WorldGenerator, but the exact coordinates are seed-derived — we
     * instead verify the property by checking that at least 3 IRON_FENCE
     * blocks at any height above 12 exist somewhere in the world within the
     * flag pole region (pole bases are placed at building-height + 1 to +3).
     */
    @Test
    public void testFlagPoleBlocksExistInWorld() {
        // Search for IRON_FENCE blocks above y=12 (where flag poles would be)
        // within the general civic-building area of the world
        int poleCount = 0;
        for (int x = -80; x <= 130; x++) {
            for (int z = -60; z <= 50; z++) {
                for (int y = 13; y <= 22; y++) {
                    if (world.getBlock(x, y, z) == BlockType.IRON_FENCE) {
                        poleCount++;
                    }
                }
            }
        }
        assertTrue(poleCount >= 3,
                "At least 3 IRON_FENCE flag-pole blocks should exist above building roofs; found " + poleCount);
    }
}

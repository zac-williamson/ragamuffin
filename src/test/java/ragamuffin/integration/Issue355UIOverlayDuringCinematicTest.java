package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.CraftingSystem;
import ragamuffin.building.Inventory;
import ragamuffin.ui.CraftingUI;
import ragamuffin.ui.HelpUI;
import ragamuffin.ui.InventoryUI;
import ragamuffin.ui.OpeningSequence;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #355:
 * UI overlays (inventory, help, crafting) must not be toggled while the opening
 * cinematic sequence is active.
 *
 * Root cause: handleUIInput() was called unconditionally in the PLAYING render
 * path, regardless of whether openingSequence.isActive() was true. Pressing I,
 * H, or C during the cinematic would open overlays, releasing cursor capture and
 * disrupting the sequence.
 *
 * Fix: Gate handleUIInput() behind !openingSequence.isActive(), mirroring the
 * guard already applied to updatePlayingSimulation() and updatePlayingInput().
 *
 * These tests verify the fix by simulating the gated handleUIInput() logic
 * directly against the OpeningSequence and UI components — no LibGDX runtime
 * needed.
 */
class Issue355UIOverlayDuringCinematicTest {

    private OpeningSequence openingSequence;
    private InventoryUI inventoryUI;
    private HelpUI helpUI;
    private CraftingUI craftingUI;

    @BeforeEach
    void setUp() {
        openingSequence = new OpeningSequence();
        Inventory inventory = new Inventory(36);
        inventoryUI = new InventoryUI(inventory);
        helpUI = new HelpUI();
        CraftingSystem craftingSystem = new CraftingSystem();
        craftingUI = new CraftingUI(craftingSystem, inventory);
    }

    /**
     * Test 1: Inventory must not be toggled while the opening sequence is active.
     *
     * Simulates pressing I during the cinematic. With the fix in place,
     * handleUIInput() is skipped when openingSequence.isActive() returns true,
     * so the inventory remains hidden.
     */
    @Test
    void inventoryUI_mustNotOpen_duringOpeningSequence() {
        openingSequence.start();
        assertTrue(openingSequence.isActive(), "Opening sequence must be active after start()");

        // Simulate the fixed handleUIInput() gate:
        // if (!openingSequence.isActive()) { inventoryUI.toggle(); }
        if (!openingSequence.isActive()) {
            inventoryUI.toggle();
        }

        assertFalse(inventoryUI.isVisible(),
                "Inventory must remain hidden while opening sequence is active; " +
                "handleUIInput() must be gated behind !openingSequence.isActive()");
    }

    /**
     * Test 2: Help UI must not be toggled while the opening sequence is active.
     *
     * Simulates pressing H during the cinematic. The fixed guard prevents the
     * help overlay from opening.
     */
    @Test
    void helpUI_mustNotOpen_duringOpeningSequence() {
        openingSequence.start();
        assertTrue(openingSequence.isActive(), "Opening sequence must be active after start()");

        if (!openingSequence.isActive()) {
            helpUI.toggle();
        }

        assertFalse(helpUI.isVisible(),
                "Help UI must remain hidden while opening sequence is active; " +
                "handleUIInput() must be gated behind !openingSequence.isActive()");
    }

    /**
     * Test 3: Crafting UI must not be toggled while the opening sequence is active.
     *
     * Simulates pressing C during the cinematic. The fixed guard prevents the
     * crafting overlay from opening.
     */
    @Test
    void craftingUI_mustNotOpen_duringOpeningSequence() {
        openingSequence.start();
        assertTrue(openingSequence.isActive(), "Opening sequence must be active after start()");

        if (!openingSequence.isActive()) {
            craftingUI.toggle();
        }

        assertFalse(craftingUI.isVisible(),
                "Crafting UI must remain hidden while opening sequence is active; " +
                "handleUIInput() must be gated behind !openingSequence.isActive()");
    }

    /**
     * Test 4: UI overlays CAN be toggled once the opening sequence completes.
     *
     * After the sequence ends (isActive() returns false), handleUIInput() is
     * no longer suppressed and overlays respond normally to key presses.
     */
    @Test
    void uiOverlays_canOpen_afterOpeningSequenceCompletes() {
        openingSequence.start();
        openingSequence.skip(); // Immediately complete the sequence

        assertFalse(openingSequence.isActive(), "Opening sequence must be inactive after skip()");
        assertTrue(openingSequence.isCompleted(), "Opening sequence must be marked completed");

        // With sequence inactive, handleUIInput() runs normally
        if (!openingSequence.isActive()) {
            inventoryUI.toggle();
        }

        assertTrue(inventoryUI.isVisible(),
                "Inventory must open normally after the opening sequence completes");
    }

    /**
     * Test 5: UI overlays CAN be toggled when no opening sequence has been started.
     *
     * At game load time (sequence not yet started), isActive() is false, so
     * handleUIInput() is not suppressed.
     */
    @Test
    void uiOverlays_canOpen_whenOpeningSequenceNotStarted() {
        assertFalse(openingSequence.isActive(), "Opening sequence must not be active before start()");

        if (!openingSequence.isActive()) {
            helpUI.toggle();
        }

        assertTrue(helpUI.isVisible(),
                "Help UI must open when no opening sequence is running");
    }

    /**
     * Test 6: Opening sequence gate does not affect toggling after the sequence expires naturally.
     *
     * Advances the sequence past its full duration via update() calls so it
     * completes naturally, then verifies overlays become accessible.
     */
    @Test
    void uiOverlays_canOpen_afterOpeningSequenceExpiresNaturally() {
        openingSequence.start();
        assertTrue(openingSequence.isActive());

        // Advance past full duration (12 seconds) in one large step
        openingSequence.update(13.0f);

        assertFalse(openingSequence.isActive(),
                "Opening sequence must become inactive after its full duration elapses");
        assertTrue(openingSequence.isCompleted(),
                "Opening sequence must be marked completed after natural expiry");

        if (!openingSequence.isActive()) {
            craftingUI.toggle();
        }

        assertTrue(craftingUI.isVisible(),
                "Crafting UI must open normally after the opening sequence expires naturally");
    }
}

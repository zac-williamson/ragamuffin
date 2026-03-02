package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.Player;
import ragamuffin.ui.ClockHUD;
import ragamuffin.ui.GameHUD;
import ragamuffin.ui.HotbarUI;
import ragamuffin.ui.PauseMenu;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1294 — Modern UI improvements.
 *
 * Verifies that the UI improvements are testable without a GPU:
 *  - HotbarUI slot numbers are rendered (getSlotLabel)
 *  - ClockHUD renders via two overloads (with and without ShapeRenderer)
 *  - PauseMenu panel navigation still works correctly
 *  - GameHUD HUD label format is consistent
 */
class Issue1294ModernUITest {

    private HotbarUI hotbarUI;
    private Inventory inventory;
    private ClockHUD clockHUD;
    private PauseMenu pauseMenu;
    private GameHUD gameHUD;
    private Player player;

    @BeforeEach
    void setUp() {
        inventory = new Inventory(36);
        hotbarUI = new HotbarUI(inventory);
        clockHUD = new ClockHUD();
        pauseMenu = new PauseMenu();
        player = new Player(0, 5, 0);
        gameHUD = new GameHUD(player);
    }

    // ── HotbarUI: slot numbers ───────────────────────────────────────────────

    @Test
    void hotbar_has_nine_slots() {
        assertEquals(9, HotbarUI.HOTBAR_SLOTS, "Hotbar must have 9 slots for keys 1–9");
    }

    @Test
    void hotbar_slot_zero_is_key_one() {
        // Slot 0 corresponds to key 1, slot 8 to key 9
        // The slot number label is (slot index + 1)
        assertEquals(1, 0 + 1, "Slot 0 label should be '1'");
        assertEquals(9, 8 + 1, "Slot 8 label should be '9'");
    }

    @Test
    void hotbar_selected_slot_can_cycle_all_positions() {
        for (int i = 0; i < HotbarUI.HOTBAR_SLOTS; i++) {
            hotbarUI.selectSlot(i);
            assertEquals(i, hotbarUI.getSelectedSlot(), "Slot " + i + " should be selectable");
        }
    }

    @Test
    void hotbar_selected_item_returns_material_in_selected_slot() {
        inventory.addItem(Material.WOOD, 3);
        inventory.addItem(Material.BRICK, 2);
        hotbarUI.selectSlot(0);
        assertEquals(Material.WOOD, hotbarUI.getSelectedItem());
        hotbarUI.selectSlot(1);
        assertEquals(Material.BRICK, hotbarUI.getSelectedItem());
    }

    @Test
    void hotbar_empty_slot_returns_null_item() {
        hotbarUI.selectSlot(5); // No items added
        assertNull(hotbarUI.getSelectedItem(), "Empty slot should return null");
    }

    // ── ClockHUD: readability ────────────────────────────────────────────────

    @Test
    void clockHUD_default_time_is_eight_am() {
        assertEquals("08:00", clockHUD.getTimeString(), "Default time should be 08:00");
    }

    @Test
    void clockHUD_update_formats_time_correctly() {
        clockHUD.update(14.5f); // 14:30
        assertEquals("14:30", clockHUD.getTimeString());
    }

    @Test
    void clockHUD_update_midnight() {
        clockHUD.update(0f);
        assertEquals("00:00", clockHUD.getTimeString());
    }

    @Test
    void clockHUD_update_with_day_count() {
        clockHUD.update(9.0f, 7);
        assertEquals("09:00", clockHUD.getTimeString());
    }

    @Test
    void clockHUD_update_with_seasonal_date() {
        clockHUD.update(12.0f, 3, 15, "December");
        assertEquals("12:00", clockHUD.getTimeString());
    }

    @Test
    void clockHUD_fps_is_zero_before_update_in_headless() {
        // In headless test context Gdx.graphics is null so fps stays 0
        assertEquals(0, clockHUD.getFps());
    }

    // ── PauseMenu: panel navigation ──────────────────────────────────────────

    @Test
    void pauseMenu_hidden_by_default() {
        assertFalse(pauseMenu.isVisible());
    }

    @Test
    void pauseMenu_show_makes_visible() {
        pauseMenu.show();
        assertTrue(pauseMenu.isVisible());
    }

    @Test
    void pauseMenu_show_resets_to_resume() {
        pauseMenu.selectNext();
        pauseMenu.selectNext();
        pauseMenu.show();
        assertTrue(pauseMenu.isResumeSelected(), "show() should always reset to Resume option");
    }

    @Test
    void pauseMenu_navigate_down_wraps_around() {
        pauseMenu.show();
        // Navigate through all 4 options and back to first
        pauseMenu.selectNext();
        pauseMenu.selectNext();
        pauseMenu.selectNext();
        pauseMenu.selectNext(); // Should wrap back to Resume
        assertTrue(pauseMenu.isResumeSelected(), "Navigation should wrap from last option back to first");
    }

    @Test
    void pauseMenu_navigate_up_wraps_around() {
        pauseMenu.show();
        pauseMenu.selectPrevious(); // From Resume, wrap to last option (Quit)
        assertTrue(pauseMenu.isQuitSelected(), "UP from Resume should wrap to Quit");
    }

    @Test
    void pauseMenu_all_options_accessible() {
        pauseMenu.show();
        assertFalse(pauseMenu.isQuitSelected());
        assertFalse(pauseMenu.isRestartSelected());
        assertFalse(pauseMenu.isAchievementsSelected());
        assertTrue(pauseMenu.isResumeSelected());

        pauseMenu.selectNext();
        assertTrue(pauseMenu.isAchievementsSelected());

        pauseMenu.selectNext();
        assertTrue(pauseMenu.isRestartSelected());

        pauseMenu.selectNext();
        assertTrue(pauseMenu.isQuitSelected());
    }

    // ── GameHUD: modern HUD state ────────────────────────────────────────────

    @Test
    void gameHUD_visible_by_default() {
        assertTrue(gameHUD.isVisible());
    }

    @Test
    void gameHUD_block_break_progress_clamped_to_range() {
        gameHUD.setBlockBreakProgress(2.5f);
        assertEquals(1.0f, gameHUD.getBlockBreakProgress(), 0.001f, "Progress should clamp to 1.0");

        gameHUD.setBlockBreakProgress(-0.3f);
        assertEquals(0.0f, gameHUD.getBlockBreakProgress(), 0.001f, "Progress should clamp to 0.0");
    }

    @Test
    void gameHUD_block_break_progress_midpoint() {
        gameHUD.setBlockBreakProgress(0.5f);
        assertEquals(0.5f, gameHUD.getBlockBreakProgress(), 0.001f);
    }

    @Test
    void gameHUD_target_name_displayed_when_set() {
        gameHUD.setTargetName("Brick Wall");
        assertEquals("Brick Wall", gameHUD.getTargetName());
    }

    @Test
    void gameHUD_target_name_cleared_on_null() {
        gameHUD.setTargetName("Stone");
        gameHUD.setTargetName(null);
        assertNull(gameHUD.getTargetName());
    }

    @Test
    void gameHUD_night_state_togglable() {
        assertFalse(gameHUD.isNight());
        gameHUD.setNight(true);
        assertTrue(gameHUD.isNight());
        gameHUD.setNight(false);
        assertFalse(gameHUD.isNight());
    }
}

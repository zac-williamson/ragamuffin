package ragamuffin.ui;

import ragamuffin.building.Material;

/**
 * Issue #926: ChippyOrderUI — the ordering menu shown when the player presses E
 * on the {@code PropType#CHIPPY_COUNTER} inside Tony's Chip Shop (CHIPPY landmark).
 *
 * <p>Displays the chippy menu with prices and availability. Navigation is via
 * UP/DOWN arrow keys; ENTER (or E) confirms the order; ESC closes without ordering.
 * Rendering is intentionally headless-safe: all rendering calls are delegated to
 * the caller. This class manages UI state only.
 *
 * <h3>Menu Items</h3>
 * <ul>
 *   <li>CHIPS — 2 COIN, +40 hunger</li>
 *   <li>BATTERED_SAUSAGE — 2 COIN, +30 hunger +10 energy</li>
 *   <li>CHIP_BUTTY — 3 COIN, +50 hunger, requires BREAD in inventory</li>
 *   <li>MUSHY_PEAS — 1 COIN, +15 hunger +5 cold relief</li>
 *   <li>PICKLED_EGG — 1 COIN, 20% FOOD_POISONING chance</li>
 *   <li>FISH_SUPPER — 4 COIN, premium, only available 2/3 days</li>
 *   <li>SALT_AND_VINEGAR_PACKET — 1 COIN, condiment</li>
 *   <li>BOTTLE_OF_WATER — 1 COIN, +20 thirst</li>
 * </ul>
 */
public class ChippyOrderUI {

    // ── Layout constants ──────────────────────────────────────────────────────

    public static final float PANEL_WIDTH  = 340f;
    public static final float PANEL_HEIGHT = 380f;
    public static final float PADDING      = 14f;

    // ── Menu item definitions ─────────────────────────────────────────────────

    /** All items on Tony's menu, in display order. */
    public static final MenuItem[] MENU_ITEMS = {
        new MenuItem(Material.CHIPS,                  2, "Chips",               "+40 hunger"),
        new MenuItem(Material.BATTERED_SAUSAGE,       2, "Battered Sausage",    "+30 hunger +10 energy"),
        new MenuItem(Material.CHIP_BUTTY,             3, "Chip Butty",          "+50 hunger (needs BREAD)"),
        new MenuItem(Material.MUSHY_PEAS,             1, "Mushy Peas",          "+15 hunger +5 cold relief"),
        new MenuItem(Material.PICKLED_EGG,            1, "Pickled Egg",         "20% food poisoning risk"),
        new MenuItem(Material.FISH_SUPPER,            4, "Fish Supper",         "+60 hunger (premium)"),
        new MenuItem(Material.SALT_AND_VINEGAR_PACKET,1, "Salt & Vinegar",      "Seasons your chips"),
        new MenuItem(Material.BOTTLE_OF_WATER,        1, "Bottle of Water",     "+20 thirst"),
    };

    /** Total number of menu items. */
    public static final int MENU_SIZE = MENU_ITEMS.length;

    // ── State ─────────────────────────────────────────────────────────────────

    private boolean visible;

    /** Currently highlighted menu item index (0–MENU_SIZE-1). */
    private int selectedIndex;

    /** Last status message shown to the player (e.g. "Enjoy your chips!"). */
    private String statusMessage;

    /** Whether the FISH_SUPPER is available today (true 2 out of 3 days). */
    private boolean fishSupperAvailable;

    // ── Construction ──────────────────────────────────────────────────────────

    public ChippyOrderUI() {
        this.visible = false;
        this.selectedIndex = 0;
        this.statusMessage = null;
        this.fishSupperAvailable = true;
    }

    // ── Visibility ────────────────────────────────────────────────────────────

    public boolean isVisible() {
        return visible;
    }

    /** Open the ordering menu. */
    public void show() {
        visible = true;
        selectedIndex = 0;
        statusMessage = null;
    }

    /** Close the ordering menu. */
    public void hide() {
        visible = false;
        statusMessage = null;
    }

    /** Toggle visibility. */
    public void toggle() {
        if (visible) hide();
        else show();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    /** Returns the index of the currently highlighted menu item. */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /** Set the selected item index directly (clamped to valid range). */
    public void setSelectedIndex(int index) {
        this.selectedIndex = Math.max(0, Math.min(index, MENU_SIZE - 1));
    }

    /** Move selection down one row (wraps around). */
    public void selectNext() {
        selectedIndex = (selectedIndex + 1) % MENU_SIZE;
    }

    /** Move selection up one row (wraps around). */
    public void selectPrev() {
        selectedIndex = (selectedIndex + MENU_SIZE - 1) % MENU_SIZE;
    }

    /** Returns the currently highlighted {@link MenuItem}. */
    public MenuItem getSelectedItem() {
        return MENU_ITEMS[selectedIndex];
    }

    // ── Fish Supper availability ───────────────────────────────────────────────

    /**
     * Set whether the fish supper is available today.
     * Called by {@code ChippySystem} based on the in-game day number.
     *
     * @param available true if the fish supper is on today's menu
     */
    public void setFishSupperAvailable(boolean available) {
        this.fishSupperAvailable = available;
    }

    /** Returns true if the FISH_SUPPER is available today. */
    public boolean isFishSupperAvailable() {
        return fishSupperAvailable;
    }

    /**
     * Returns true if the item at the given index is currently available for
     * purchase. FISH_SUPPER is only available on 2 out of every 3 days.
     *
     * @param index the menu item index
     * @return true if available
     */
    public boolean isItemAvailable(int index) {
        if (index < 0 || index >= MENU_SIZE) return false;
        MenuItem item = MENU_ITEMS[index];
        if (item.material == Material.FISH_SUPPER) {
            return fishSupperAvailable;
        }
        return true;
    }

    // ── Status messages ───────────────────────────────────────────────────────

    /** Returns the last status message, or {@code null} if none. */
    public String getStatusMessage() {
        return statusMessage;
    }

    /** Set a status message to display to the player. */
    public void setStatusMessage(String message) {
        this.statusMessage = message;
    }

    /** Clear the status message. */
    public void clearStatusMessage() {
        this.statusMessage = null;
    }

    // ── Summary text (for tests and HUD) ──────────────────────────────────────

    /**
     * Build the full menu string as it would appear on screen.
     * Unavailable items are marked as "(not today)".
     *
     * @return multi-line menu text
     */
    public String buildMenuText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tony's Chip Shop\n");
        sb.append("Salt? Vinegar? Open till midnight.\n");
        sb.append("─────────────────────────────────\n");
        for (int i = 0; i < MENU_SIZE; i++) {
            MenuItem item = MENU_ITEMS[i];
            String prefix = (i == selectedIndex) ? "> " : "  ";
            String availability = isItemAvailable(i) ? "" : " (not today)";
            sb.append(String.format("%s%-26s %d COIN%s\n",
                    prefix, item.displayName, item.price, availability));
        }
        if (statusMessage != null) {
            sb.append("\n").append(statusMessage);
        }
        return sb.toString();
    }

    // ── Inner classes ─────────────────────────────────────────────────────────

    /**
     * A single item on Tony's menu.
     */
    public static class MenuItem {

        /** The Material awarded on purchase. */
        public final Material material;

        /** Price in COIN. */
        public final int price;

        /** Display name shown on the menu. */
        public final String displayName;

        /** Short description / effect summary. */
        public final String description;

        MenuItem(Material material, int price, String displayName, String description) {
            this.material    = material;
            this.price       = price;
            this.displayName = displayName;
            this.description = description;
        }
    }
}

package ragamuffin.ui;

import ragamuffin.building.Material;

/**
 * Issue #1041: ArgosOrderUI — the catalogue-browsing menu shown when the player
 * presses E on the {@code PropType#ARGOS_CATALOGUE_PROP} inside Argos
 * ({@code LandmarkType#ARGOS}).
 *
 * <p>Displays the catalogue with 12 items, each identified by a 4-digit item number.
 * Navigation is via UP/DOWN arrow keys; ENTER (or E) confirms the slip; ESC closes
 * without ordering. This class manages UI state only — rendering is delegated to the caller.
 *
 * <h3>Catalogue Items</h3>
 * <ul>
 *   <li>#1234 FOLDING_CHAIR — 8 COIN</li>
 *   <li>#2156 KETTLE — 6 COIN</li>
 *   <li>#3421 TOASTER — 5 COIN</li>
 *   <li>#4567 GOLD_CHAIN — 12 COIN</li>
 *   <li>#5102 PORTABLE_RADIO — 9 COIN</li>
 *   <li>#6234 SLEEPING_BAG — 7 COIN</li>
 *   <li>#7801 AIR_FRYER — 14 COIN</li>
 *   <li>#8045 DUVET — 10 COIN</li>
 *   <li>#8921 ELECTRIC_HEATER — 11 COIN</li>
 *   <li>#9012 ALARM_CLOCK — 4 COIN</li>
 *   <li>#9411 KIDS_BIKE — 13 COIN</li>
 *   <li>#9999 ??? — 0 COIN (Marchetti dead-drop, requires Respect ≥ 50)</li>
 * </ul>
 */
public class ArgosOrderUI {

    // ── Layout constants ──────────────────────────────────────────────────────

    public static final float PANEL_WIDTH  = 380f;
    public static final float PANEL_HEIGHT = 420f;
    public static final float PADDING      = 16f;

    // ── Catalogue item definitions ────────────────────────────────────────────

    /** All items in the Argos catalogue, in display order. */
    public static final CatalogueItem[] CATALOGUE_ITEMS = {
        new CatalogueItem(1234, Material.FOLDING_CHAIR,   8,  "Folding Chair",   "Folds flat. Perfect for cramped spaces."),
        new CatalogueItem(2156, Material.KETTLE,          6,  "Kettle",          "Boils water. Classic."),
        new CatalogueItem(3421, Material.TOASTER,         5,  "Toaster",         "2-slice. White. £5.99."),
        new CatalogueItem(4567, Material.GOLD_CHAIN,      12, "Gold Chain",      "18ct gold-effect. Very glam."),
        new CatalogueItem(5102, Material.PORTABLE_RADIO,  9,  "Portable Radio",  "AM/FM. Batteries not included."),
        new CatalogueItem(6234, Material.SLEEPING_BAG,    7,  "Sleeping Bag",    "3-season. Suitable for 0°C."),
        new CatalogueItem(7801, Material.AIR_FRYER,       14, "Air Fryer",       "2L capacity. Health food, apparently."),
        new CatalogueItem(8045, Material.DUVET,           10, "Duvet",           "10.5 tog. Winter warmth sorted."),
        new CatalogueItem(8921, Material.ELECTRIC_HEATER, 11, "Electric Heater", "2kW convector. Meter will spin."),
        new CatalogueItem(9012, Material.ALARM_CLOCK,     4,  "Alarm Clock",     "Never miss your giro again."),
        new CatalogueItem(9411, Material.KIDS_BIKE,       13, "Kids Bike",       "14-inch wheels. Red and silver."),
        new CatalogueItem(9999, null,                     0,  "???",             "Special order. Ask at counter."),
    };

    /** Total number of catalogue items. */
    public static final int CATALOGUE_SIZE = CATALOGUE_ITEMS.length;

    /** Item number for the Marchetti dead-drop slip. */
    public static final int MARCHETTI_ITEM_NUMBER = 9999;

    // ── State ─────────────────────────────────────────────────────────────────

    private boolean visible;

    /** Currently highlighted catalogue item index (0–CATALOGUE_SIZE-1). */
    private int selectedIndex;

    /** Last status message shown to the player (e.g. "Slip written!"). */
    private String statusMessage;

    /**
     * Whether item #9999 (Marchetti dead-drop) is currently visible.
     * Only shown when Marchetti Crew Respect ≥ 50 and the dead-drop is active.
     */
    private boolean marchettiSlipAvailable;

    // ── Construction ──────────────────────────────────────────────────────────

    public ArgosOrderUI() {
        this.visible = false;
        this.selectedIndex = 0;
        this.statusMessage = null;
        this.marchettiSlipAvailable = false;
    }

    // ── Visibility ────────────────────────────────────────────────────────────

    public boolean isVisible() {
        return visible;
    }

    /** Open the catalogue menu. */
    public void show() {
        visible = true;
        selectedIndex = 0;
        statusMessage = null;
    }

    /** Close the catalogue menu. */
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

    /** Returns the index of the currently highlighted catalogue item. */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /** Set the selected item index directly (clamped to valid range). */
    public void setSelectedIndex(int index) {
        this.selectedIndex = Math.max(0, Math.min(index, CATALOGUE_SIZE - 1));
    }

    /** Move selection down one row (wraps around). */
    public void selectNext() {
        selectedIndex = (selectedIndex + 1) % CATALOGUE_SIZE;
    }

    /** Move selection up one row (wraps around). */
    public void selectPrev() {
        selectedIndex = (selectedIndex + CATALOGUE_SIZE - 1) % CATALOGUE_SIZE;
    }

    /** Returns the currently highlighted {@link CatalogueItem}. */
    public CatalogueItem getSelectedItem() {
        return CATALOGUE_ITEMS[selectedIndex];
    }

    // ── Marchetti dead-drop availability ──────────────────────────────────────

    /**
     * Set whether the Marchetti dead-drop slip (item #9999) is available.
     * Called by {@code ArgosSystem} when Marchetti Crew Respect ≥ 50 and
     * the dead-drop has been seeded.
     *
     * @param available true if item 9999 should be shown and selectable
     */
    public void setMarchettiSlipAvailable(boolean available) {
        this.marchettiSlipAvailable = available;
    }

    /** Returns true if the Marchetti dead-drop slip is currently available. */
    public boolean isMarchettiSlipAvailable() {
        return marchettiSlipAvailable;
    }

    /**
     * Returns true if the item at the given index is available for ordering.
     * Item #9999 (Marchetti dead-drop) is only available when
     * {@link #marchettiSlipAvailable} is true.
     *
     * @param index the catalogue item index
     * @return true if the item can be ordered
     */
    public boolean isItemAvailable(int index) {
        if (index < 0 || index >= CATALOGUE_SIZE) return false;
        CatalogueItem item = CATALOGUE_ITEMS[index];
        if (item.itemNumber == MARCHETTI_ITEM_NUMBER) {
            return marchettiSlipAvailable;
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
     * Build the full catalogue text as it would appear on screen.
     * Unavailable items are marked as "(unavailable)".
     *
     * @return multi-line catalogue text
     */
    public String buildCatalogueText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Argos — Catalogue\n");
        sb.append("Write a slip. Hand it to the clerk. Wait.\n");
        sb.append("─────────────────────────────────────\n");
        for (int i = 0; i < CATALOGUE_SIZE; i++) {
            CatalogueItem item = CATALOGUE_ITEMS[i];
            String prefix = (i == selectedIndex) ? "> " : "  ";
            String availability = isItemAvailable(i) ? "" : " (unavailable)";
            if (item.itemNumber == MARCHETTI_ITEM_NUMBER && !marchettiSlipAvailable) {
                continue; // Hidden entirely until Marchetti unlocks
            }
            sb.append(String.format("%s#%04d %-24s %d COIN%s\n",
                    prefix, item.itemNumber, item.displayName, item.price, availability));
        }
        if (statusMessage != null) {
            sb.append("\n").append(statusMessage);
        }
        return sb.toString();
    }

    // ── Inner classes ─────────────────────────────────────────────────────────

    /**
     * A single item in the Argos catalogue.
     */
    public static class CatalogueItem {

        /** The 4-digit catalogue item number. */
        public final int itemNumber;

        /** The Material awarded on collection, or {@code null} for special items. */
        public final Material material;

        /** Price in COIN. */
        public final int price;

        /** Display name shown in the catalogue. */
        public final String displayName;

        /** Short description / flavour text. */
        public final String description;

        CatalogueItem(int itemNumber, Material material, int price,
                      String displayName, String description) {
            this.itemNumber  = itemNumber;
            this.material    = material;
            this.price       = price;
            this.displayName = displayName;
            this.description = description;
        }
    }
}

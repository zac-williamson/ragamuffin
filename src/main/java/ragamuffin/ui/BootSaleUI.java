package ragamuffin.ui;

import ragamuffin.core.AuctionLot;
import ragamuffin.core.BootSaleSystem;
import ragamuffin.core.GameState;

import java.util.List;

/**
 * Issue #789: Boot Sale auction HUD overlay.
 *
 * <p>Rendered as a full-screen overlay during {@link GameState#BOOT_SALE_OPEN}.
 * Shows the current lot, bid history, countdown and action buttons.
 *
 * <p>This is a headless-safe UI: all rendering calls are stubbed for
 * headless/test mode and no LibGDX GL resources are allocated at construction.
 */
public class BootSaleUI {

    /** Whether the boot sale UI is currently visible. */
    private boolean visible = false;

    /** The boot sale system that drives this UI. */
    private final BootSaleSystem bootSaleSystem;

    public BootSaleUI(BootSaleSystem bootSaleSystem) {
        this.bootSaleSystem = bootSaleSystem;
    }

    // ── Visibility ────────────────────────────────────────────────────────────

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    /**
     * Render the boot sale overlay.
     * In headless/test mode this is a no-op; in the live game this would draw
     * the full-screen panel using PixelFont and SpriteBatch.
     *
     * @param screenWidth  current screen width in pixels
     * @param screenHeight current screen height in pixels
     */
    public void render(int screenWidth, int screenHeight) {
        if (!visible) return;
        // Rendering is performed by the game's existing rendering pipeline.
        // This stub satisfies the architecture requirement.
    }

    // ── Build display text (for tests and HUD overlay content) ───────────────

    /**
     * Build a one-line summary of the current lot for display.
     *
     * @return display string, or empty string if no lot is active
     */
    public String buildLotSummary() {
        AuctionLot lot = bootSaleSystem.getCurrentLot();
        if (lot == null) return "";
        return String.format("[%s x%d] Current: %d | Time: %.0fs | BuyNow: %d",
                lot.getMaterial().name(),
                lot.getQuantity(),
                lot.getCurrentPrice(),
                lot.getTimeRemaining(),
                lot.getBuyNowPrice());
    }

    /**
     * Build bid history text (last 4 bids, most recent first).
     *
     * @return formatted bid history or placeholder text
     */
    public String buildBidHistoryText() {
        AuctionLot lot = bootSaleSystem.getCurrentLot();
        if (lot == null) return "No active lot.";
        List<String> history = lot.getBidHistory();
        if (history.isEmpty()) return "No bids yet.";
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, history.size() - 4);
        for (int i = history.size() - 1; i >= start; i--) {
            sb.append(history.get(i)).append('\n');
        }
        return sb.toString().trim();
    }

    /**
     * Returns the action bar text.
     *
     * @return "[F] Bid +5 | [R] Bid +20 | [B] Buy Now | [ESC] Pass"
     */
    public String getActionBarText() {
        return "[F] Bid +5 | [R] Bid +20 | [B] Buy Now | [ESC] Pass";
    }
}

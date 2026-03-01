package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.core.PirateRadioSystem;
import ragamuffin.core.PirateRadioSystem.BroadcastAction;

/**
 * Issue #990: RadioUI — overlay panel shown while a broadcast session is active.
 *
 * <p>When {@link PirateRadioSystem#isActionPending()} is true the panel expands
 * to show all four broadcast actions with their key number and a one-line
 * description so the player always knows what they can do. When no action is
 * pending the panel collapses to a slim status bar showing the ON AIR indicator,
 * power level, and triangulation percentage.
 *
 * <p>Input is handled externally (keys 1–4 in the game loop). This class
 * manages state and rendering only and is headless-safe.
 */
public class RadioUI {

    // ── Layout constants ──────────────────────────────────────────────────────

    public static final float PANEL_WIDTH    = 400f;
    public static final float PANEL_HEIGHT   = 240f;   // expanded (action pending)
    public static final float STATUS_HEIGHT  = 44f;    // collapsed (on-air only)
    public static final float PADDING        = 14f;
    public static final float ROW_HEIGHT     = 42f;

    // Colours
    private static final Color BG_COLOUR         = new Color(0.05f, 0.05f, 0.08f, 0.92f);
    private static final Color BORDER_COLOUR      = new Color(0.85f, 0.15f, 0.15f, 1.0f); // red
    private static final Color TITLE_COLOUR       = new Color(0.95f, 0.15f, 0.15f, 1.0f); // red
    private static final Color KEY_COLOUR         = new Color(0.95f, 0.80f, 0.20f, 1.0f); // amber
    private static final Color ACTION_COLOUR      = new Color(0.90f, 0.90f, 0.90f, 1.0f);
    private static final Color TOOLTIP_COLOUR     = new Color(0.60f, 0.60f, 0.65f, 1.0f);
    private static final Color TRIANGULATION_WARN = new Color(0.95f, 0.40f, 0.10f, 1.0f);
    private static final Color TRIANGULATION_OK   = new Color(0.30f, 0.85f, 0.30f, 1.0f);
    private static final Color STATUS_BG          = new Color(0.08f, 0.08f, 0.10f, 0.88f);
    private static final Color HINT_COLOUR        = new Color(0.50f, 0.50f, 0.55f, 1.0f);

    // Font scales
    private static final float TITLE_SCALE   = 0.90f;
    private static final float KEY_SCALE     = 0.85f;
    private static final float ACTION_SCALE  = 0.78f;
    private static final float TOOLTIP_SCALE = 0.68f;
    private static final float STATUS_SCALE  = 0.80f;

    // ── State ─────────────────────────────────────────────────────────────────

    private boolean visible;

    /** Currently highlighted action index (0-based, matching BroadcastAction ordinal). */
    private int highlightedAction;

    // ── Constructor ───────────────────────────────────────────────────────────

    public RadioUI() {
        this.visible = false;
        this.highlightedAction = 0;
    }

    // ── Visibility ────────────────────────────────────────────────────────────

    public boolean isVisible() { return visible; }

    public void show() {
        visible = true;
        highlightedAction = 0;
    }

    public void hide() {
        visible = false;
    }

    public void toggle() {
        if (visible) hide();
        else show();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    public int getHighlightedAction() { return highlightedAction; }

    /** Highlight action by 0-based index (0–3). */
    public void setHighlightedAction(int index) {
        this.highlightedAction = Math.max(0, Math.min(3, index));
    }

    public void nextAction() { highlightedAction = Math.min(3, highlightedAction + 1); }
    public void prevAction() { highlightedAction = Math.max(0, highlightedAction - 1); }

    // ── Summary text helpers (headless-safe, for tests) ───────────────────────

    /**
     * Build a text summary of the radio panel suitable for tests.
     *
     * @param system the pirate radio system
     * @return multi-line string describing visible panel contents
     */
    public String buildSummary(PirateRadioSystem system) {
        StringBuilder sb = new StringBuilder();

        if (!system.isActive()) {
            sb.append("[PIRATE FM — OFF AIR]");
            return sb.toString();
        }

        // Status line
        sb.append(String.format("[PIRATE FM] Power: %d  Triangulation: %.0f%%",
                system.getPowerLevel(), system.getTriangulation()));
        if (system.isTriangulationWarning()) {
            sb.append("  *** SIGNAL WARNING ***");
        }
        sb.append("\n");

        // Action list — always shown when visible, highlighted when pending
        if (system.isActionPending()) {
            sb.append("-- CHOOSE BROADCAST ACTION --\n");
        }

        for (BroadcastAction action : BroadcastAction.values()) {
            boolean isHighlighted = (action.ordinal() == highlightedAction);
            String prefix = isHighlighted ? "> " : "  ";
            sb.append(String.format("%s[%d] %-30s %s\n",
                    prefix,
                    action.getNumber(),
                    action.getDisplayName(),
                    action.getTooltip()));
        }

        if (system.isActionPending()) {
            sb.append("Press 1-4 to broadcast.");
        }

        return sb.toString();
    }

    /**
     * Returns true if the action list section (keys 1-4) is currently displayed.
     * This is always true when the panel is visible — the options are shown at all
     * times while on-air, not only when an action is pending, so the player always
     * knows what keys do what.
     */
    public boolean isActionListVisible() {
        return visible;
    }

    /**
     * Returns true when the panel is showing the "action pending" prompt,
     * meaning the player must choose one of the four broadcast actions.
     */
    public boolean isActionPromptActive(PirateRadioSystem system) {
        return visible && system != null && system.isActionPending();
    }

    // ── Rendering (LibGDX; no-op without context) ─────────────────────────────

    /**
     * Render the radio UI panel.
     *
     * <p>Centred horizontally, anchored to the lower third of the screen.
     * When an action is pending the panel expands to show all four options;
     * otherwise only the compact status bar is drawn.
     *
     * @param batch       sprite batch (already begun)
     * @param shape       shape renderer (NOT begun — this method manages begin/end)
     * @param font        bitmap font
     * @param system      pirate radio system
     * @param screenW     screen width
     * @param screenH     screen height
     */
    public void render(SpriteBatch batch, ShapeRenderer shape, BitmapFont font,
                       PirateRadioSystem system, int screenW, int screenH) {
        if (!visible || system == null) return;

        boolean pending = system.isActionPending();
        float panelH = pending ? PANEL_HEIGHT : STATUS_HEIGHT;
        float panelX = (screenW - PANEL_WIDTH) / 2f;
        float panelY = screenH * 0.25f;

        // ── Background ─────────────────────────────────────────────────────────
        batch.end();
        shape.begin(ShapeRenderer.ShapeType.Filled);
        Color bg = pending ? BG_COLOUR : STATUS_BG;
        shape.setColor(bg);
        shape.rect(panelX, panelY, PANEL_WIDTH, panelH);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(BORDER_COLOUR);
        shape.rect(panelX, panelY, PANEL_WIDTH, panelH);
        shape.end();

        batch.begin();

        float x = panelX + PADDING;
        float y = panelY + panelH - PADDING;

        // ── Title / status bar ─────────────────────────────────────────────────
        String onAirText = system.isOnAirVisible() ? "● ON AIR" : "  ON AIR";
        String triangWarn = system.isTriangulationWarning() ? "  ⚠ SIGNAL" : "";
        String statusLine = String.format("%s  PWR:%d  TRI:%.0f%%%s",
                onAirText, system.getPowerLevel(), system.getTriangulation(), triangWarn);

        font.getData().setScale(TITLE_SCALE);
        font.setColor(TITLE_COLOUR);
        font.draw(batch, statusLine, x, y);
        y -= ROW_HEIGHT * 0.7f;

        if (!pending) return; // compact mode — only show status line

        // ── Divider label ──────────────────────────────────────────────────────
        font.getData().setScale(ACTION_SCALE);
        font.setColor(TOOLTIP_COLOUR);
        font.draw(batch, "CHOOSE BROADCAST ACTION  (press 1-4)", x, y);
        y -= ROW_HEIGHT * 0.65f;

        // ── Action rows ────────────────────────────────────────────────────────
        for (BroadcastAction action : BroadcastAction.values()) {
            boolean isHighlighted = (action.ordinal() == highlightedAction);

            // Key badge
            font.getData().setScale(KEY_SCALE);
            font.setColor(KEY_COLOUR);
            font.draw(batch, "[" + action.getNumber() + "]", x, y);

            // Action name
            float nameX = x + 36f;
            font.getData().setScale(ACTION_SCALE);
            font.setColor(isHighlighted ? KEY_COLOUR : ACTION_COLOUR);
            font.draw(batch, action.getDisplayName(), nameX, y);

            // Tooltip (one line below)
            font.getData().setScale(TOOLTIP_SCALE);
            font.setColor(TOOLTIP_COLOUR);
            font.draw(batch, action.getTooltip(), nameX, y - 14f);

            y -= ROW_HEIGHT;
        }

        // ── Triangulation bar ──────────────────────────────────────────────────
        float barWidth  = PANEL_WIDTH - PADDING * 2f;
        float barHeight = 6f;
        float barX      = panelX + PADDING;
        float barY      = panelY + PADDING;

        batch.end();
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(new Color(0.20f, 0.20f, 0.22f, 1.0f));
        shape.rect(barX, barY, barWidth, barHeight);
        float fraction = system.getTriangulation() / PirateRadioSystem.TRIANGULATION_MAX;
        Color barColour = system.isTriangulationWarning() ? TRIANGULATION_WARN : TRIANGULATION_OK;
        shape.setColor(barColour);
        shape.rect(barX, barY, barWidth * fraction, barHeight);
        shape.end();
        batch.begin();

        // Restore font scale
        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);
    }
}

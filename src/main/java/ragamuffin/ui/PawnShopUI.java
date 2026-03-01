package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.PawnShopSystem;

import java.util.List;

/**
 * Issue #961: UI panel for Cash4Gold Pawnbrokers.
 *
 * <p>Two tabs:
 * <ul>
 *   <li><b>SELL</b> — shows accepted items held by the player with Gary's quote.
 *       Drag (or press number key) to select; confirm with Enter/S.</li>
 *   <li><b>PLEDGES</b> — shows the player's active pledge tickets with days remaining
 *       and redeem cost. Press R to redeem selected.</li>
 * </ul>
 *
 * <p>Input is handled externally. This class is purely for rendering.
 */
public class PawnShopUI {

    // ── Layout constants ──────────────────────────────────────────────────────

    private static final float PANEL_WIDTH  = 360f;
    private static final float PANEL_HEIGHT = 440f;
    private static final float PADDING      = 14f;
    private static final float TAB_HEIGHT   = 28f;

    private static final float TITLE_SCALE   = 1.0f;
    private static final float SECTION_SCALE = 0.85f;
    private static final float ITEM_SCALE    = 0.78f;
    private static final float HINT_SCALE    = 0.70f;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Which tab is active: 0 = SELL, 1 = PLEDGES. */
    private int activeTab = 0;

    /** Currently highlighted item index in the SELL tab. */
    private int selectedSellIndex = -1;

    /** Currently highlighted pledge index in the PLEDGES tab. */
    private int selectedPledgeIndex = -1;

    private boolean visible;

    public PawnShopUI() {
        this.visible = false;
    }

    // ── Visibility ────────────────────────────────────────────────────────────

    public boolean isVisible() { return visible; }
    public void show() { visible = true; activeTab = 0; selectedSellIndex = -1; }
    public void hide() { visible = false; }
    public void toggle() { if (visible) hide(); else show(); }

    // ── Tab navigation ────────────────────────────────────────────────────────

    /** Switch to the sell tab. */
    public void showSellTab() { activeTab = 0; selectedSellIndex = -1; }

    /** Switch to the pledges tab. */
    public void showPledgesTab() { activeTab = 1; selectedPledgeIndex = -1; }

    public int getActiveTab() { return activeTab; }

    // ── Selection helpers ─────────────────────────────────────────────────────

    /** Set the selected sell-item index. */
    public void setSelectedSellIndex(int index) { this.selectedSellIndex = index; }
    public int getSelectedSellIndex() { return selectedSellIndex; }

    /** Set the selected pledge index. */
    public void setSelectedPledgeIndex(int index) { this.selectedPledgeIndex = index; }
    public int getSelectedPledgeIndex() { return selectedPledgeIndex; }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Render the pawn shop panel.
     *
     * @param batch        sprite batch (orthographic projection already set)
     * @param shape        shape renderer
     * @param font         bitmap font
     * @param screenWidth  screen width in pixels
     * @param screenHeight screen height in pixels
     * @param system       the pawn shop system state
     * @param inventory    the player's inventory
     * @param currentDay   current in-game day index
     */
    public void render(SpriteBatch batch, ShapeRenderer shape, BitmapFont font,
                       int screenWidth, int screenHeight,
                       PawnShopSystem system, Inventory inventory, int currentDay) {
        if (!visible) return;

        float panelX = (screenWidth - PANEL_WIDTH) / 2f;
        float panelY = (screenHeight - PANEL_HEIGHT) / 2f;

        // Background
        com.badlogic.gdx.Gdx.gl.glEnable(GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.05f, 0.05f, 0.05f, 0.92f);
        shape.rect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        shape.end();
        com.badlogic.gdx.Gdx.gl.glDisable(GL20.GL_BLEND);

        // Gold left accent bar
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.85f, 0.65f, 0.10f, 1f);
        shape.rect(panelX, panelY, 4f, PANEL_HEIGHT);
        shape.end();

        // Border
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.65f, 0.50f, 0.08f, 1f);
        shape.rect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        shape.end();

        // Tab backgrounds
        float tabW = PANEL_WIDTH / 2f;
        for (int t = 0; t < 2; t++) {
            float tabX = panelX + t * tabW;
            float tabY = panelY + PANEL_HEIGHT - TAB_HEIGHT;
            shape.begin(ShapeRenderer.ShapeType.Filled);
            if (t == activeTab) {
                shape.setColor(0.25f, 0.20f, 0.03f, 1f);
            } else {
                shape.setColor(0.10f, 0.08f, 0.01f, 1f);
            }
            shape.rect(tabX, tabY, tabW, TAB_HEIGHT);
            shape.end();
        }

        batch.begin();

        float curY = panelY + PANEL_HEIGHT - PADDING / 2f;

        // Tab labels
        font.getData().setScale(SECTION_SCALE);
        font.setColor(activeTab == 0 ? new Color(1f, 0.85f, 0.3f, 1f) : new Color(0.5f, 0.5f, 0.5f, 1f));
        font.draw(batch, " SELL", panelX + PADDING, curY);

        font.setColor(activeTab == 1 ? new Color(1f, 0.85f, 0.3f, 1f) : new Color(0.5f, 0.5f, 0.5f, 1f));
        font.draw(batch, " PLEDGES", panelX + PANEL_WIDTH / 2f + PADDING, curY);

        curY -= TAB_HEIGHT;

        // Title
        font.getData().setScale(TITLE_SCALE);
        font.setColor(0.95f, 0.80f, 0.15f, 1f);
        font.draw(batch, "CASH4GOLD PAWNBROKERS", panelX + PADDING + 6f, curY);
        curY -= 22f;

        // Subhead
        font.getData().setScale(ITEM_SCALE);
        font.setColor(0.55f, 0.55f, 0.55f, 1f);
        font.draw(batch, "Gary says: \"What you got for me?\"", panelX + PADDING + 6f, curY);
        curY -= 18f;

        // Divider
        font.setColor(0.35f, 0.35f, 0.35f, 1f);
        font.draw(batch, "------------------------------------", panelX + PADDING + 6f, curY);
        curY -= 16f;

        if (activeTab == 0) {
            renderSellTab(batch, font, panelX, curY, system, inventory);
        } else {
            renderPledgesTab(batch, font, panelX, curY, system, inventory, currentDay);
        }

        // Controls hint
        font.getData().setScale(HINT_SCALE);
        font.setColor(0.45f, 0.45f, 0.45f, 1f);
        if (activeTab == 0) {
            font.draw(batch, "[1-9] select  [S] sell  [P] pledge  [Tab] pledges tab  [ESC] close",
                    panelX + PADDING + 4f, panelY + PADDING + 4f);
        } else {
            font.draw(batch, "[1-9] select  [R] redeem pledge  [Tab] sell tab  [ESC] close",
                    panelX + PADDING + 4f, panelY + PADDING + 4f);
        }

        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // ── Sell tab ──────────────────────────────────────────────────────────────

    private void renderSellTab(SpriteBatch batch, BitmapFont font,
                               float panelX, float startY,
                               PawnShopSystem system, Inventory inventory) {
        float curY = startY;

        font.getData().setScale(SECTION_SCALE);
        font.setColor(0.95f, 0.75f, 0.1f, 1f);
        font.draw(batch, "Items Gary buys:", panelX + PADDING + 6f, curY);
        curY -= 20f;

        int idx = 0;
        for (Material mat : PawnShopSystem.ACCEPTED_ITEMS) {
            int held = inventory.getItemCount(mat);
            int quote = system.getSellQuote(mat);
            int pledgeQuote = system.getPledgeQuote(mat);

            boolean hasItem = held > 0;
            boolean selected = (idx == selectedSellIndex);

            font.getData().setScale(ITEM_SCALE);
            if (selected) {
                font.setColor(1f, 1f, 0.4f, 1f);
            } else if (hasItem) {
                font.setColor(1f, 1f, 1f, 1f);
            } else {
                font.setColor(0.35f, 0.35f, 0.35f, 1f);
            }

            String line = String.format("[%d] %-20s sell:%d  pledge:%d  held:%d",
                    (idx + 1), mat.getDisplayName(), quote, pledgeQuote, held);
            font.draw(batch, line, panelX + PADDING + 6f, curY);
            curY -= 16f;
            idx++;
        }

        // Quote detail for selected item
        if (selectedSellIndex >= 0) {
            Material[] accepted = PawnShopSystem.ACCEPTED_ITEMS.toArray(new Material[0]);
            if (selectedSellIndex < accepted.length) {
                Material sel = accepted[selectedSellIndex];
                int held = inventory.getItemCount(sel);
                curY -= 4f;
                font.getData().setScale(ITEM_SCALE);
                if (held > 0) {
                    font.setColor(0.8f, 0.95f, 0.4f, 1f);
                    font.draw(batch, String.format(">> %s: Gary offers %d coin (pledge: %d coin)",
                            sel.getDisplayName(), system.getSellQuote(sel), system.getPledgeQuote(sel)),
                            panelX + PADDING + 6f, curY);
                } else {
                    font.setColor(0.6f, 0.3f, 0.3f, 1f);
                    font.draw(batch, ">> You don't have that item.", panelX + PADDING + 6f, curY);
                }
            }
        }
    }

    // ── Pledges tab ───────────────────────────────────────────────────────────

    private void renderPledgesTab(SpriteBatch batch, BitmapFont font,
                                  float panelX, float startY,
                                  PawnShopSystem system, Inventory inventory, int currentDay) {
        float curY = startY;

        List<PawnShopSystem.Pledge> active = system.getActivePledges();

        font.getData().setScale(SECTION_SCALE);
        font.setColor(0.95f, 0.75f, 0.1f, 1f);
        font.draw(batch, "Active pledges (" + active.size() + "):", panelX + PADDING + 6f, curY);
        curY -= 20f;

        if (active.isEmpty()) {
            font.getData().setScale(ITEM_SCALE);
            font.setColor(0.5f, 0.5f, 0.5f, 1f);
            font.draw(batch, "  Nothing in hock. Yet.", panelX + PADDING + 6f, curY);
            curY -= 16f;
        } else {
            for (int i = 0; i < active.size(); i++) {
                PawnShopSystem.Pledge pledge = active.get(i);
                int daysLeft = pledge.daysRemaining(currentDay);
                boolean selected = (i == selectedPledgeIndex);

                font.getData().setScale(ITEM_SCALE);
                if (selected) {
                    font.setColor(1f, 1f, 0.4f, 1f);
                } else if (daysLeft <= 1) {
                    font.setColor(1f, 0.35f, 0.20f, 1f); // urgent red
                } else {
                    font.setColor(0.85f, 0.85f, 0.85f, 1f);
                }

                String daysStr = daysLeft <= 0 ? "OVERDUE!" : daysLeft + " day(s) left";
                String line = String.format("[%d] %-18s redeem: %d coin  (%s)",
                        (i + 1), pledge.material.getDisplayName(),
                        pledge.redeemAmount, daysStr);
                font.draw(batch, line, panelX + PADDING + 6f, curY);
                curY -= 16f;
            }

            // Redemption detail for selected pledge
            if (selectedPledgeIndex >= 0 && selectedPledgeIndex < active.size()) {
                PawnShopSystem.Pledge sel = active.get(selectedPledgeIndex);
                int coinHeld = inventory.getItemCount(Material.COIN);
                boolean canAfford = coinHeld >= sel.redeemAmount;
                curY -= 4f;
                font.getData().setScale(ITEM_SCALE);
                if (canAfford) {
                    font.setColor(0.6f, 0.95f, 0.4f, 1f);
                    font.draw(batch, String.format(">> Redeem %s for %d coin? [R] to confirm.",
                            sel.material.getDisplayName(), sel.redeemAmount),
                            panelX + PADDING + 6f, curY);
                } else {
                    font.setColor(0.7f, 0.3f, 0.3f, 1f);
                    font.draw(batch, String.format(">> Need %d coin (you have %d).",
                            sel.redeemAmount, coinHeld),
                            panelX + PADDING + 6f, curY);
                }
            }
        }
    }
}

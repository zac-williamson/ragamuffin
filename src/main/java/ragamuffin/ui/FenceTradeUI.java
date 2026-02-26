package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.FenceSystem;
import ragamuffin.core.FenceValuationTable;
import ragamuffin.core.StreetReputation;
import ragamuffin.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Trade UI panel for the Fence NPC, opened with E.
 *
 * <p>Layout:
 * <ul>
 *   <li>Header: "FENCE — BLACK MARKET" with a dark red accent bar</li>
 *   <li>Rep indicator: current rep level and points</li>
 *   <li>SELL section: lists stolen goods the player has and what the Fence pays</li>
 *   <li>BUY section: today's rotating stock with prices in FOOD</li>
 *   <li>CONTRABAND RUN section (rep >= 30 only): available run + countdown if active</li>
 *   <li>Controls hint at the bottom</li>
 * </ul>
 *
 * <p>Input is handled externally (by the game loop / InputHandler). This class
 * is purely for rendering.
 */
public class FenceTradeUI {

    private static final float PANEL_WIDTH  = 320f;
    private static final float PANEL_HEIGHT = 400f;
    private static final float PADDING      = 12f;

    private static final float TITLE_SCALE   = 1.0f;
    private static final float SECTION_SCALE = 0.85f;
    private static final float ITEM_SCALE    = 0.78f;
    private static final float HINT_SCALE    = 0.72f;

    private boolean visible;

    public FenceTradeUI() {
        this.visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
    }

    public void toggle() {
        visible = !visible;
    }

    /**
     * Render the Fence trade panel.
     *
     * @param batch       sprite batch (orthographic projection already set)
     * @param shape       shape renderer
     * @param font        bitmap font
     * @param screenWidth  screen width in pixels
     * @param screenHeight screen height in pixels
     * @param fenceSystem  the fence system state
     * @param player       the player (for rep access)
     * @param inventory    the player's inventory
     */
    public void render(SpriteBatch batch, ShapeRenderer shape, BitmapFont font,
                       int screenWidth, int screenHeight,
                       FenceSystem fenceSystem, Player player, Inventory inventory) {
        if (!visible) return;

        float panelX = (screenWidth - PANEL_WIDTH) / 2f;
        float panelY = (screenHeight - PANEL_HEIGHT) / 2f;

        // Background
        com.badlogic.gdx.Gdx.gl.glEnable(GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0f, 0f, 0.88f);
        shape.rect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        shape.end();
        com.badlogic.gdx.Gdx.gl.glDisable(GL20.GL_BLEND);

        // Dark red left accent bar
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.7f, 0.05f, 0.05f, 1f);
        shape.rect(panelX, panelY, 4f, PANEL_HEIGHT);
        shape.end();

        // Border
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.5f, 0.1f, 0.1f, 1f);
        shape.rect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        shape.end();

        batch.begin();

        float curY = panelY + PANEL_HEIGHT - PADDING;

        // Title
        font.getData().setScale(TITLE_SCALE);
        font.setColor(0.9f, 0.1f, 0.1f, 1f);
        font.draw(batch, "FENCE — BLACK MARKET", panelX + PADDING + 6f, curY);
        curY -= 22f;

        // Rep line
        int repPoints = player.getStreetReputation().getPoints();
        StreetReputation.ReputationLevel repLevel = player.getStreetReputation().getLevel();
        font.getData().setScale(ITEM_SCALE);
        font.setColor(0.7f, 0.7f, 0.7f, 1f);
        String repLine = "Your rep: " + repPoints + " (" + repLevel.name() + ")";
        font.draw(batch, repLine, panelX + PADDING + 6f, curY);
        curY -= 18f;

        // Run countdown (if active)
        if (fenceSystem.isContrabandRunActive()) {
            int runIdx = fenceSystem.getCurrentRunIndex();
            String runName = (runIdx >= 0) ? FenceSystem.CONTRABAND_RUN_NAMES[runIdx] : "?";
            int remaining = (int) Math.ceil(fenceSystem.getRunTimer());
            font.getData().setScale(ITEM_SCALE);
            font.setColor(1f, 0.5f, 0.1f, 1f);
            font.draw(batch, String.format("RUN: \"%s\" — %ds left", runName, remaining),
                    panelX + PADDING + 6f, curY);
            curY -= 18f;
        }

        // Divider
        font.getData().setScale(ITEM_SCALE);
        font.setColor(0.4f, 0.4f, 0.4f, 1f);
        font.draw(batch, "----------------------------", panelX + PADDING + 6f, curY);
        curY -= 16f;

        // SELL section
        font.getData().setScale(SECTION_SCALE);
        font.setColor(0.95f, 0.75f, 0.1f, 1f);
        font.draw(batch, "SELL (stolen goods):", panelX + PADDING + 6f, curY);
        curY -= 18f;

        FenceValuationTable table = fenceSystem.getValuationTable();
        for (Map.Entry<Material, Integer> entry : table.getAllValuations().entrySet()) {
            Material mat = entry.getKey();
            int value = entry.getValue();
            int held = inventory.getItemCount(mat);
            font.getData().setScale(ITEM_SCALE);
            if (held > 0) {
                font.setColor(1f, 1f, 1f, 1f);
            } else {
                font.setColor(0.4f, 0.4f, 0.4f, 1f);
            }
            String line = String.format("  %s — %d food each  [held: %d]",
                    mat.getDisplayName(), value, held);
            font.draw(batch, line, panelX + PADDING + 6f, curY);
            curY -= 16f;
        }

        curY -= 4f;

        // BUY section (only if rep >= 10)
        if (repPoints >= StreetReputation.KNOWN_THRESHOLD) {
            font.getData().setScale(SECTION_SCALE);
            font.setColor(0.95f, 0.75f, 0.1f, 1f);
            font.draw(batch, "BUY (today's stock):", panelX + PADDING + 6f, curY);
            curY -= 18f;

            List<Material> stock = fenceSystem.getTodayStock();
            List<Integer> costs = fenceSystem.getTodayStockCosts();
            int foodHeld = inventory.getItemCount(Material.FOOD);

            for (int i = 0; i < stock.size(); i++) {
                Material item = stock.get(i);
                int cost = costs.get(i);
                boolean canAfford = foodHeld >= cost;
                font.getData().setScale(ITEM_SCALE);
                font.setColor(canAfford ? Color.WHITE : new Color(0.4f, 0.4f, 0.4f, 1f));
                String line = String.format("  [%d] %s — %d food", (i + 1), item.getDisplayName(), cost);
                font.draw(batch, line, panelX + PADDING + 6f, curY);
                curY -= 16f;
            }

            curY -= 4f;
        }

        // CONTRABAND RUNS section (rep >= 30 only)
        if (repPoints >= StreetReputation.NOTORIOUS_THRESHOLD) {
            font.getData().setScale(SECTION_SCALE);
            font.setColor(0.95f, 0.75f, 0.1f, 1f);
            font.draw(batch, "CONTRABAND RUNS:", panelX + PADDING + 6f, curY);
            curY -= 16f;

            font.getData().setScale(ITEM_SCALE);
            if (fenceSystem.isContrabandRunActive()) {
                font.setColor(1f, 0.5f, 0.1f, 1f);
                font.draw(batch, "  Run in progress — complete it first.", panelX + PADDING + 6f, curY);
            } else if (fenceSystem.isLocked()) {
                font.setColor(0.5f, 0.2f, 0.2f, 1f);
                font.draw(batch, "  Locked out. Come back tomorrow.", panelX + PADDING + 6f, curY);
            } else {
                font.setColor(0.7f, 0.7f, 0.7f, 1f);
                font.draw(batch, "  [R] Take a contraband run", panelX + PADDING + 6f, curY);
            }
            curY -= 16f;
            curY -= 4f;
        }

        // Controls hint
        font.getData().setScale(HINT_SCALE);
        font.setColor(0.5f, 0.5f, 0.5f, 1f);
        font.draw(batch, "S: sell item   B: buy selected   ESC: close", panelX + PADDING + 6f, panelY + PADDING + 4f);

        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);
        batch.end();
    }
}

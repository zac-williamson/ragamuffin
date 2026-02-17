package ragamuffin.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Hover tooltip system that shows tooltips when the cursor hovers over UI elements.
 * Separate from TooltipSystem which handles tutorial/event popups.
 *
 * Usage: Each frame, UI components call addZone() to register their hover zones.
 * After all UI is rendered, call render() to draw the tooltip on top.
 * Call clear() at the start of each frame to reset zones.
 */
public class HoverTooltipSystem {

    private static final float HOVER_DELAY = 0.3f;
    private static final float TOOLTIP_PADDING = 8f;
    private static final float CURSOR_OFFSET_X = 15f;
    private static final float CURSOR_OFFSET_Y = -20f;

    private final List<TooltipZone> zones = new ArrayList<>();
    private float hoverTime;
    private String activeTooltip;
    private int lastHoverZoneHash;

    /**
     * A rectangular screen region with associated tooltip text.
     */
    public static class TooltipZone {
        public final float x, y, width, height;
        public final String text;

        /**
         * @param x      left edge (LibGDX screen coords, origin bottom-left)
         * @param y      bottom edge
         * @param width  zone width
         * @param height zone height
         * @param text   tooltip text to display
         */
        public TooltipZone(float x, float y, float width, float height, String text) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
        }

        public boolean contains(float px, float py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }
    }

    /**
     * Register a tooltip zone for the current frame.
     */
    public void addZone(float x, float y, float width, float height, String text) {
        if (text != null && !text.isEmpty()) {
            zones.add(new TooltipZone(x, y, width, height, text));
        }
    }

    /**
     * Clear all zones. Call at the start of each frame before UI components re-register.
     */
    public void clear() {
        zones.clear();
    }

    /**
     * Update hover tracking. Call once per frame.
     */
    public void update(float delta) {
        // Get mouse position in LibGDX screen coords (flip Y)
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        // Find which zone the cursor is in
        TooltipZone hoveredZone = null;
        for (TooltipZone zone : zones) {
            if (zone.contains(mouseX, mouseY)) {
                hoveredZone = zone;
                break;
            }
        }

        if (hoveredZone != null) {
            int zoneHash = System.identityHashCode(hoveredZone);
            if (zoneHash != lastHoverZoneHash) {
                // Moved to a new zone — reset timer
                hoverTime = 0;
                lastHoverZoneHash = zoneHash;
                activeTooltip = null;
            }
            hoverTime += delta;
            if (hoverTime >= HOVER_DELAY) {
                activeTooltip = hoveredZone.text;
            }
        } else {
            hoverTime = 0;
            lastHoverZoneHash = 0;
            activeTooltip = null;
        }
    }

    /**
     * Render the hover tooltip if active. Call at the END of the UI render pass.
     */
    public void render(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font) {
        if (activeTooltip == null) {
            return;
        }

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        // Measure text
        float prevScaleX = font.getData().scaleX;
        float prevScaleY = font.getData().scaleY;
        font.getData().setScale(1.0f);

        GlyphLayout layout = new GlyphLayout(font, activeTooltip);
        float textWidth = layout.width;
        float textHeight = layout.height;

        float boxWidth = textWidth + TOOLTIP_PADDING * 2;
        float boxHeight = textHeight + TOOLTIP_PADDING * 2;

        // Position near cursor
        float tooltipX = mouseX + CURSOR_OFFSET_X;
        float tooltipY = mouseY + CURSOR_OFFSET_Y;

        // Clamp to screen bounds
        if (tooltipX + boxWidth > screenWidth) {
            tooltipX = mouseX - boxWidth - 5;
        }
        if (tooltipX < 0) {
            tooltipX = 0;
        }
        if (tooltipY - boxHeight < 0) {
            tooltipY = boxHeight + 5;
        }
        if (tooltipY > screenHeight) {
            tooltipY = screenHeight;
        }

        // Draw background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 0.9f);
        shapeRenderer.rect(tooltipX, tooltipY - boxHeight, boxWidth, boxHeight);
        shapeRenderer.end();

        // Draw border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.7f, 0.7f, 0.7f, 1f);
        shapeRenderer.rect(tooltipX, tooltipY - boxHeight, boxWidth, boxHeight);
        shapeRenderer.end();

        // Draw text
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, activeTooltip, tooltipX + TOOLTIP_PADDING, tooltipY - TOOLTIP_PADDING);
        batch.end();

        // Restore font scale
        font.getData().setScale(prevScaleX, prevScaleY);
    }

    /**
     * Get the currently active tooltip text (for testing).
     */
    public String getActiveTooltip() {
        return activeTooltip;
    }

    /**
     * Get the number of registered zones (for testing).
     */
    public int getZoneCount() {
        return zones.size();
    }
}

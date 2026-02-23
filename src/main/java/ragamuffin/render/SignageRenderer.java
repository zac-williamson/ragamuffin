package ragamuffin.render;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import ragamuffin.world.BuildingSign;
import ragamuffin.world.Landmark;
import ragamuffin.world.LandmarkType;

/**
 * Renders building signage as world-projected 2D overlays.
 *
 * Each sign is a coloured panel with the store name rendered in pixel-art style
 * (using {@link PixelFont}), positioned above the building entrance and rendered
 * in screen-space so the text is always legible regardless of viewing angle.
 * Signs are only shown when the camera is within a reasonable distance
 * (MAX_RENDER_DISTANCE) to avoid clutter.
 *
 * The sign panel is larger than the old text-based panel to better showcase the
 * pixel-art lettering: a minimum of 120 px wide and 36 px tall at full scale.
 */
public class SignageRenderer {

    /** Maximum distance (blocks) at which signs are visible. */
    private static final float MAX_RENDER_DISTANCE = 40f;

    // ---- Sign panel geometry (full-scale, screen pixels) ----
    /** Pixel size (screen px per font pixel) at full scale. */
    private static final float BASE_PIXEL_SIZE = 3f;
    /** Vertical padding above and below the pixel-art text inside the panel. */
    private static final float PANEL_V_PADDING = 8f;
    /** Horizontal padding left and right of the pixel-art text. */
    private static final float PANEL_H_PADDING = 12f;
    /** Minimum panel width in screen pixels at full scale. */
    private static final float MIN_PANEL_WIDTH = 120f;
    /** Border thickness in screen pixels. */
    private static final float BORDER = 2f;

    private final List<BuildingSign> signs = new ArrayList<>();
    private final Vector3 tmpPos = new Vector3();

    /**
     * Build the sign list from world landmarks.  Call once after world generation.
     *
     * For each landmark that has a display name, a sign is placed at the front of the
     * building, centred horizontally, just above the top of the wall (at worldY = height + 0.5).
     */
    public void buildFromLandmarks(Collection<Landmark> landmarks) {
        signs.clear();
        for (Landmark landmark : landmarks) {
            String name = landmark.getType().getDisplayName();
            if (name == null) continue;

            // Derive sign colours from the landmark type
            com.badlogic.gdx.graphics.Color bg = getSignBackground(landmark.getType());
            com.badlogic.gdx.graphics.Color fg = getSignForeground(landmark.getType());

            // Centre the sign horizontally over the entrance (front face = min Z of landmark)
            float signX = landmark.getPosition().x + landmark.getWidth() / 2f;
            float signY = landmark.getHeight() + 1.0f; // 1 block above top of wall
            float signZ = landmark.getPosition().z;     // Front face of building

            signs.add(new BuildingSign(name, signX, signY, signZ, bg, fg));
        }
    }

    /**
     * Render all visible signs using pixel-art lettering.
     *
     * Call after the 3D modelBatch.end() but before the main UI is drawn.
     *
     * @param camera        the perspective camera used for projection
     * @param spriteBatch   2D sprite batch (unused directly; kept for API compatibility)
     * @param shapeRenderer shape renderer (must NOT already be active)
     * @param font          bitmap font (unused — pixel art rendering replaces it)
     * @param screenWidth   current screen width in pixels
     * @param screenHeight  current screen height in pixels
     */
    public void render(PerspectiveCamera camera,
                       SpriteBatch spriteBatch,
                       ShapeRenderer shapeRenderer,
                       BitmapFont font,
                       int screenWidth,
                       int screenHeight) {

        for (BuildingSign sign : signs) {
            tmpPos.set(sign.getWorldX(), sign.getWorldY(), sign.getWorldZ());

            // Distance cull
            float dist = camera.position.dst(tmpPos);
            if (dist > MAX_RENDER_DISTANCE) continue;

            // Project to screen space
            camera.project(tmpPos, 0, 0, screenWidth, screenHeight);

            // Skip if behind the camera
            if (tmpPos.z > 1.0f || tmpPos.z < 0f) continue;

            float sx = tmpPos.x;
            float sy = tmpPos.y;

            // Scale everything by distance so distant signs appear smaller
            float scale = Math.max(0.4f, 1.0f - dist / MAX_RENDER_DISTANCE);
            float pixelSize = BASE_PIXEL_SIZE * scale;

            // Panel dimensions derived from pixel-art text size
            float textW = PixelFont.stringWidth(sign.getText(), pixelSize);
            float textH = PixelFont.glyphHeight(pixelSize);
            float panelW = Math.max(MIN_PANEL_WIDTH * scale, textW + PANEL_H_PADDING * 2 * scale);
            float panelH = textH + PANEL_V_PADDING * 2 * scale;

            float panelX = sx - panelW / 2f;
            float panelY = sy;

            // Skip if off-screen
            if (panelX + panelW < 0 || panelX > screenWidth) continue;
            if (panelY + panelH < 0 || panelY > screenHeight) continue;

            // ---- Draw sign background ----------------------------------------
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(sign.getBackgroundColor());
            shapeRenderer.rect(panelX, panelY, panelW, panelH);
            shapeRenderer.end();

            // ---- Draw border (two-tone: light top-left, dark bottom-right) -----
            com.badlogic.gdx.graphics.Color bg = sign.getBackgroundColor();
            float border = Math.max(1f, BORDER * scale);

            // Dark border (full rect outline)
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(0f, 0f, 0f, 0.9f);
            shapeRenderer.rect(panelX, panelY, panelW, panelH);
            shapeRenderer.end();

            // Bright inner highlight on top and left edges (retro sign feel)
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(
                Math.min(1f, bg.r + 0.35f),
                Math.min(1f, bg.g + 0.35f),
                Math.min(1f, bg.b + 0.35f),
                0.8f);
            // Top highlight strip
            shapeRenderer.rect(panelX, panelY + panelH - border, panelW, border);
            // Left highlight strip
            shapeRenderer.rect(panelX, panelY, border, panelH);
            shapeRenderer.end();

            // ---- Draw pixel-art text centred on the panel --------------------
            float textX = panelX + (panelW - textW) / 2f;
            float textY = panelY + PANEL_V_PADDING * scale;
            PixelFont.drawString(shapeRenderer, sign.getText(), textX, textY, pixelSize,
                                 sign.getTextColor());
        }
    }

    /** Returns all registered signs (for testing). */
    public List<BuildingSign> getSigns() {
        return java.util.Collections.unmodifiableList(signs);
    }

    // ---- Colour helpers -------------------------------------------------------

    private com.badlogic.gdx.graphics.Color getSignBackground(LandmarkType type) {
        switch (type) {
            case GREGGS:            return new com.badlogic.gdx.graphics.Color(0.0f, 0.33f, 0.71f, 1f); // Greggs blue
            case TESCO_EXPRESS:     return new com.badlogic.gdx.graphics.Color(0.0f, 0.29f, 0.57f, 1f); // Tesco blue
            case OFF_LICENCE:       return new com.badlogic.gdx.graphics.Color(0.55f, 0.0f, 0.0f, 1f);  // Dark red
            case CHARITY_SHOP:      return new com.badlogic.gdx.graphics.Color(0.0f, 0.50f, 0.15f, 1f); // Charity green
            case JEWELLER:          return new com.badlogic.gdx.graphics.Color(0.12f, 0.12f, 0.12f, 1f); // Near-black
            case JOB_CENTRE:        return new com.badlogic.gdx.graphics.Color(0.0f, 0.30f, 0.60f, 1f); // Govt blue
            case GP_SURGERY:        return new com.badlogic.gdx.graphics.Color(0.0f, 0.55f, 0.27f, 1f); // NHS green
            case PUB:
            case WETHERSPOONS:      return new com.badlogic.gdx.graphics.Color(0.30f, 0.15f, 0.05f, 1f); // Pub dark wood
            case NANDOS:            return new com.badlogic.gdx.graphics.Color(0.78f, 0.10f, 0.10f, 1f); // Nando's red
            case BOOKIES:
            case BETTING_SHOP:      return new com.badlogic.gdx.graphics.Color(0.0f, 0.45f, 0.10f, 1f); // Betting green
            case FIRE_STATION:      return new com.badlogic.gdx.graphics.Color(0.85f, 0.10f, 0.10f, 1f); // Red
            case LIBRARY:           return new com.badlogic.gdx.graphics.Color(0.15f, 0.28f, 0.55f, 1f); // Library blue
            case PRIMARY_SCHOOL:    return new com.badlogic.gdx.graphics.Color(0.0f, 0.38f, 0.65f, 1f); // School blue
            default:                return new com.badlogic.gdx.graphics.Color(0.15f, 0.15f, 0.15f, 1f); // Default dark
        }
    }

    private com.badlogic.gdx.graphics.Color getSignForeground(LandmarkType type) {
        switch (type) {
            case JEWELLER:          return new com.badlogic.gdx.graphics.Color(0.95f, 0.80f, 0.10f, 1f); // Gold text
            default:                return com.badlogic.gdx.graphics.Color.WHITE;
        }
    }
}

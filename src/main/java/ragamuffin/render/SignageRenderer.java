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
 * Each sign is a coloured panel with the store name, positioned above the building
 * entrance and rendered in screen-space so the text is always legible regardless of
 * viewing angle.  Signs are only shown when the camera is within a reasonable distance
 * (MAX_RENDER_DISTANCE) to avoid clutter.
 *
 * The sign panel is sized to a 2x3 block equivalent in screen pixels, matching the
 * spec of a "2×3 block area" sign above the entrance.
 */
public class SignageRenderer {

    /** Maximum distance (blocks) at which signs are visible. */
    private static final float MAX_RENDER_DISTANCE = 40f;
    /** Sign panel height in screen pixels (represents ~3 blocks). */
    private static final float PANEL_HEIGHT = 22f;
    /** Horizontal padding inside the sign panel. */
    private static final float PANEL_PADDING = 10f;
    /** Minimum panel width (represents ~2 blocks of width). */
    private static final float MIN_PANEL_WIDTH = 60f;

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
     * Render all visible signs.  Call after the 3D modelBatch.end() but before
     * the main UI is drawn.
     *
     * @param camera        the perspective camera used for projection
     * @param spriteBatch   2D sprite batch (must NOT already be active)
     * @param shapeRenderer shape renderer (must NOT already be active)
     * @param font          bitmap font to draw text with
     * @param screenWidth   current screen width in pixels
     * @param screenHeight  current screen height in pixels
     */
    public void render(PerspectiveCamera camera,
                       SpriteBatch spriteBatch,
                       ShapeRenderer shapeRenderer,
                       BitmapFont font,
                       int screenWidth,
                       int screenHeight) {

        com.badlogic.gdx.math.Matrix4 ortho = new com.badlogic.gdx.math.Matrix4();
        ortho.setToOrtho2D(0, 0, screenWidth, screenHeight);
        shapeRenderer.setProjectionMatrix(ortho);
        spriteBatch.setProjectionMatrix(ortho);

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

            // Scale panel size by distance so distant signs are smaller
            float scale = Math.max(0.4f, 1.0f - dist / MAX_RENDER_DISTANCE);
            float textWidth = sign.getText().length() * 7f * scale;
            float panelW = Math.max(MIN_PANEL_WIDTH * scale, textWidth + PANEL_PADDING * 2 * scale);
            float panelH = PANEL_HEIGHT * scale;
            float panelX = sx - panelW / 2f;
            float panelY = sy;

            // Skip if off-screen
            if (panelX + panelW < 0 || panelX > screenWidth) continue;
            if (panelY + panelH < 0 || panelY > screenHeight) continue;

            // Draw sign background
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(sign.getBackgroundColor());
            shapeRenderer.rect(panelX, panelY, panelW, panelH);
            shapeRenderer.end();

            // Draw a thin border
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(0f, 0f, 0f, 0.8f);
            shapeRenderer.rect(panelX, panelY, panelW, panelH);
            shapeRenderer.end();

            // Draw text centred on the panel
            spriteBatch.begin();
            font.getData().setScale(Math.max(0.6f, scale));
            font.setColor(sign.getTextColor());
            font.draw(spriteBatch, sign.getText(),
                      panelX + PANEL_PADDING * scale,
                      panelY + panelH - 4 * scale);
            spriteBatch.end();
        }
        // Reset font scale
        font.getData().setScale(1.2f);
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

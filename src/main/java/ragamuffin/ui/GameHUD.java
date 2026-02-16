package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.entity.Player;

/**
 * Game HUD displaying health, hunger, energy bars and crosshair.
 */
public class GameHUD {
    private static final float BAR_WIDTH = 200f;
    private static final float BAR_HEIGHT = 20f;
    private static final float BAR_SPACING = 30f;
    private static final float BAR_MARGIN = 20f;

    private static final float CROSSHAIR_SIZE = 10f;
    private static final float CROSSHAIR_THICKNESS = 2f;
    private static final float CROSSHAIR_GAP = 3f;

    private final Player player;
    private boolean visible;

    public GameHUD(Player player) {
        this.player = player;
        this.visible = true;
    }

    /**
     * Render the HUD overlay.
     */
    public void render(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                       int screenWidth, int screenHeight) {
        if (!visible) {
            return;
        }

        // Render status bars
        renderStatusBars(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);

        // Render crosshair
        renderCrosshair(shapeRenderer, screenWidth, screenHeight);
    }

    private void renderStatusBars(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                                  int screenWidth, int screenHeight) {
        float x = BAR_MARGIN;
        float y = screenHeight - BAR_MARGIN - BAR_HEIGHT;

        // Health bar
        renderBar(spriteBatch, shapeRenderer, font, "Health", player.getHealth() / Player.MAX_HEALTH,
                  Color.RED, x, y);

        // Hunger bar
        y -= BAR_SPACING;
        renderBar(spriteBatch, shapeRenderer, font, "Hunger", player.getHunger() / Player.MAX_HUNGER,
                  Color.ORANGE, x, y);

        // Energy bar
        y -= BAR_SPACING;
        renderBar(spriteBatch, shapeRenderer, font, "Energy", player.getEnergy() / Player.MAX_ENERGY,
                  Color.YELLOW, x, y);
    }

    private void renderBar(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                           String label, float percentage, Color color, float x, float y) {
        // Draw bar background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        shapeRenderer.rect(x, y, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.end();

        // Draw bar fill
        if (percentage > 0) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(color);
            shapeRenderer.rect(x, y, BAR_WIDTH * percentage, BAR_HEIGHT);
            shapeRenderer.end();
        }

        // Draw bar border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x, y, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.end();

        // Draw label and percentage
        spriteBatch.begin();
        font.setColor(Color.WHITE);
        font.draw(spriteBatch, label + ": " + (int)(percentage * 100) + "%", x + 5, y + BAR_HEIGHT - 5);
        spriteBatch.end();
    }

    private void renderCrosshair(ShapeRenderer shapeRenderer, int screenWidth, int screenHeight) {
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);

        // Horizontal line (left and right)
        shapeRenderer.rect(centerX - CROSSHAIR_SIZE - CROSSHAIR_GAP, centerY - CROSSHAIR_THICKNESS / 2,
                          CROSSHAIR_SIZE, CROSSHAIR_THICKNESS);
        shapeRenderer.rect(centerX + CROSSHAIR_GAP, centerY - CROSSHAIR_THICKNESS / 2,
                          CROSSHAIR_SIZE, CROSSHAIR_THICKNESS);

        // Vertical line (top and bottom)
        shapeRenderer.rect(centerX - CROSSHAIR_THICKNESS / 2, centerY - CROSSHAIR_SIZE - CROSSHAIR_GAP,
                          CROSSHAIR_THICKNESS, CROSSHAIR_SIZE);
        shapeRenderer.rect(centerX - CROSSHAIR_THICKNESS / 2, centerY + CROSSHAIR_GAP,
                          CROSSHAIR_THICKNESS, CROSSHAIR_SIZE);

        shapeRenderer.end();
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
    }
}

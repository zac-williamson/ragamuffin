package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.core.Weather;
import ragamuffin.entity.Player;

/**
 * Game HUD displaying health, hunger, energy bars, weather, and crosshair.
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
    private Weather currentWeather;

    public GameHUD(Player player) {
        this.player = player;
        this.visible = true;
        this.currentWeather = Weather.CLEAR;
    }

    /**
     * Render the HUD overlay.
     */
    public void render(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                       int screenWidth, int screenHeight) {
        render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight, null);
    }

    /**
     * Render the HUD overlay and register hover tooltip zones.
     */
    public void render(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                       int screenWidth, int screenHeight, HoverTooltipSystem hoverTooltips) {
        if (!visible) {
            return;
        }

        // Render status bars
        renderStatusBars(spriteBatch, shapeRenderer, font, screenWidth, screenHeight, hoverTooltips);

        // Render weather display
        renderWeather(spriteBatch, font, screenWidth, screenHeight);

        // Render crosshair
        renderCrosshair(shapeRenderer, screenWidth, screenHeight, hoverTooltips);
    }

    private void renderStatusBars(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                                  int screenWidth, int screenHeight, HoverTooltipSystem hoverTooltips) {
        float x = BAR_MARGIN;
        float y1 = screenHeight - BAR_MARGIN - BAR_HEIGHT;
        float y2 = y1 - BAR_SPACING;
        float y3 = y2 - BAR_SPACING;

        float healthPct = player.getHealth() / Player.MAX_HEALTH;
        float hungerPct = player.getHunger() / Player.MAX_HUNGER;
        float energyPct = player.getEnergy() / Player.MAX_ENERGY;

        // All filled shapes in one batch (backgrounds + fills)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        shapeRenderer.rect(x, y1, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.rect(x, y2, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.rect(x, y3, BAR_WIDTH, BAR_HEIGHT);
        if (healthPct > 0) { shapeRenderer.setColor(Color.RED); shapeRenderer.rect(x, y1, BAR_WIDTH * healthPct, BAR_HEIGHT); }
        if (hungerPct > 0) { shapeRenderer.setColor(Color.ORANGE); shapeRenderer.rect(x, y2, BAR_WIDTH * hungerPct, BAR_HEIGHT); }
        if (energyPct > 0) { shapeRenderer.setColor(Color.YELLOW); shapeRenderer.rect(x, y3, BAR_WIDTH * energyPct, BAR_HEIGHT); }
        shapeRenderer.end();

        // All borders in one batch
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x, y1, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.rect(x, y2, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.rect(x, y3, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.end();

        // All text in one batch
        spriteBatch.begin();
        font.setColor(Color.WHITE);
        font.draw(spriteBatch, "HP: " + (int)(healthPct * 100) + "%", x + 5, y1 + BAR_HEIGHT - 5);
        font.draw(spriteBatch, "Food: " + (int)(hungerPct * 100) + "%", x + 5, y2 + BAR_HEIGHT - 5);
        font.draw(spriteBatch, "Energy: " + (int)(energyPct * 100) + "%", x + 5, y3 + BAR_HEIGHT - 5);
        spriteBatch.end();

        if (hoverTooltips != null) {
            hoverTooltips.addZone(x, y1, BAR_WIDTH, BAR_HEIGHT, "Health: " + (int)(healthPct * 100) + "%");
            hoverTooltips.addZone(x, y2, BAR_WIDTH, BAR_HEIGHT, "Hunger: " + (int)(hungerPct * 100) + "%");
            hoverTooltips.addZone(x, y3, BAR_WIDTH, BAR_HEIGHT, "Energy: " + (int)(energyPct * 100) + "%");
        }
    }

    private void renderCrosshair(ShapeRenderer shapeRenderer, int screenWidth, int screenHeight,
                                 HoverTooltipSystem hoverTooltips) {
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

        // Register crosshair hover tooltip zone
        if (hoverTooltips != null) {
            float zoneSize = (CROSSHAIR_SIZE + CROSSHAIR_GAP) * 2;
            hoverTooltips.addZone(centerX - zoneSize / 2, centerY - zoneSize / 2,
                    zoneSize, zoneSize, "Aim here, mate");
        }
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

    /**
     * Update the current weather to display.
     */
    public void setWeather(Weather weather) {
        this.currentWeather = weather;
    }

    /**
     * Render the weather display in the top-right corner.
     */
    private void renderWeather(SpriteBatch spriteBatch, BitmapFont font,
                               int screenWidth, int screenHeight) {
        spriteBatch.begin();
        font.setColor(Color.WHITE);
        String weatherText = "Weather: " + currentWeather.getDisplayName();
        font.draw(spriteBatch, weatherText, screenWidth - 200, screenHeight - 20);
        spriteBatch.end();
    }
}

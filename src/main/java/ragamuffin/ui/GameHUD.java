package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.core.StreetReputation;
import ragamuffin.core.Weather;
import ragamuffin.entity.Player;

/**
 * Game HUD displaying health, hunger, energy bars, weather, crosshair,
 * dodge cooldown indicator, and night warning.
 */
public class GameHUD {
    private static final float BAR_WIDTH = 200f;
    private static final float BAR_HEIGHT = 20f;
    private static final float BAR_SPACING = 30f;
    private static final float BAR_MARGIN = 20f;

    private static final float CROSSHAIR_SIZE = 10f;
    private static final float CROSSHAIR_THICKNESS = 2f;
    private static final float CROSSHAIR_GAP = 3f;

    // Dodge bar is narrower — it's a readiness indicator not a resource
    private static final float DODGE_BAR_WIDTH = 100f;
    private static final float DODGE_BAR_HEIGHT = 12f;

    private final Player player;
    private boolean visible;
    private Weather currentWeather;
    private float blockBreakProgress; // 0.0 to 1.0
    private boolean isNight; // Whether it is currently night (police active)

    public GameHUD(Player player) {
        this.player = player;
        this.visible = true;
        this.currentWeather = Weather.CLEAR;
        this.blockBreakProgress = 0f;
        this.isNight = false;
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

        // Render street reputation
        renderReputation(spriteBatch, font, screenWidth, screenHeight);

        // Render night warning if applicable
        if (isNight) {
            renderNightWarning(spriteBatch, font, screenWidth, screenHeight);
        }

        // Render crosshair
        renderCrosshair(shapeRenderer, screenWidth, screenHeight, hoverTooltips);
    }

    private void renderStatusBars(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                                  int screenWidth, int screenHeight, HoverTooltipSystem hoverTooltips) {
        float x = BAR_MARGIN;
        float y1 = screenHeight - BAR_MARGIN - BAR_HEIGHT;
        float y2 = y1 - BAR_SPACING;
        float y3 = y2 - BAR_SPACING;
        float y4 = y3 - BAR_SPACING; // Dodge indicator row

        float healthPct = player.getHealth() / Player.MAX_HEALTH;
        float hungerPct = player.getHunger() / Player.MAX_HUNGER;
        float energyPct = player.getEnergy() / Player.MAX_ENERGY;

        // Dodge readiness: 1.0 when ready, filling as cooldown expires
        float dodgePct = player.canDodge() ? 1.0f : computeDodgePct();
        boolean dodgeReady = player.canDodge();

        // All filled shapes in one batch (backgrounds + fills)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        shapeRenderer.rect(x, y1, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.rect(x, y2, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.rect(x, y3, BAR_WIDTH, BAR_HEIGHT);
        // Dodge bar background (narrower)
        shapeRenderer.rect(x, y4, DODGE_BAR_WIDTH, DODGE_BAR_HEIGHT);
        if (healthPct > 0) { shapeRenderer.setColor(Color.RED); shapeRenderer.rect(x, y1, BAR_WIDTH * healthPct, BAR_HEIGHT); }
        if (hungerPct > 0) { shapeRenderer.setColor(Color.ORANGE); shapeRenderer.rect(x, y2, BAR_WIDTH * hungerPct, BAR_HEIGHT); }
        if (energyPct > 0) { shapeRenderer.setColor(Color.YELLOW); shapeRenderer.rect(x, y3, BAR_WIDTH * energyPct, BAR_HEIGHT); }
        // Dodge bar: cyan when ready, grey-blue when cooling down
        if (dodgePct > 0) {
            if (dodgeReady) {
                shapeRenderer.setColor(0f, 0.9f, 0.9f, 1f); // Cyan = ready
            } else {
                shapeRenderer.setColor(0.2f, 0.4f, 0.6f, 1f); // Blue-grey = cooling
            }
            shapeRenderer.rect(x, y4, DODGE_BAR_WIDTH * dodgePct, DODGE_BAR_HEIGHT);
        }
        shapeRenderer.end();

        // All borders in one batch
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x, y1, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.rect(x, y2, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.rect(x, y3, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.rect(x, y4, DODGE_BAR_WIDTH, DODGE_BAR_HEIGHT);
        shapeRenderer.end();

        // All text in one batch
        spriteBatch.begin();
        font.setColor(Color.WHITE);
        font.draw(spriteBatch, "HP: " + (int)(healthPct * 100) + "%", x + 5, y1 + BAR_HEIGHT - 5);
        font.draw(spriteBatch, "Food: " + (int)(hungerPct * 100) + "%", x + 5, y2 + BAR_HEIGHT - 5);
        font.draw(spriteBatch, "Energy: " + (int)(energyPct * 100) + "%", x + 5, y3 + BAR_HEIGHT - 5);
        // Dodge label beside the bar
        String dodgeLabel = dodgeReady ? "DODGE [Ctrl]" : "DODGE: wait";
        font.draw(spriteBatch, dodgeLabel, x + DODGE_BAR_WIDTH + 6, y4 + DODGE_BAR_HEIGHT - 1);
        spriteBatch.end();

        if (hoverTooltips != null) {
            hoverTooltips.addZone(x, y1, BAR_WIDTH, BAR_HEIGHT, "Health: " + (int)(healthPct * 100) + "%");
            hoverTooltips.addZone(x, y2, BAR_WIDTH, BAR_HEIGHT, "Hunger: " + (int)(hungerPct * 100) + "%");
            hoverTooltips.addZone(x, y3, BAR_WIDTH, BAR_HEIGHT, "Energy: " + (int)(energyPct * 100) + "%");
            hoverTooltips.addZone(x, y4, DODGE_BAR_WIDTH, DODGE_BAR_HEIGHT,
                    dodgeReady ? "Dodge ready — press Ctrl while moving" : "Dodge on cooldown");
        }
    }

    /**
     * Compute dodge fill percentage from cooldown timer.
     * 0.0 = just dodged (full cooldown), 1.0 = ready.
     */
    private float computeDodgePct() {
        float cooldown = player.getDodgeCooldownTimer();
        float maxCooldown = Player.DODGE_COOLDOWN;
        if (maxCooldown <= 0) return 1.0f;
        float remaining = Math.max(0f, cooldown);
        return 1.0f - (remaining / maxCooldown);
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

        // Block break progress indicator — arc around crosshair
        if (blockBreakProgress > 0f) {
            float arcRadius = CROSSHAIR_SIZE + CROSSHAIR_GAP + 4;
            float arcThickness = 3f;
            int segments = (int) (blockBreakProgress * 24); // 24 segments for full circle
            shapeRenderer.setColor(1f, 0.4f, 0.1f, 0.9f); // Orange glow
            for (int i = 0; i < segments; i++) {
                float angle1 = (float) (i * Math.PI * 2 / 24);
                float angle2 = (float) ((i + 1) * Math.PI * 2 / 24);
                float x1 = centerX + (float) Math.cos(angle1) * arcRadius;
                float y1 = centerY + (float) Math.sin(angle1) * arcRadius;
                float x2 = centerX + (float) Math.cos(angle2) * arcRadius;
                float y2 = centerY + (float) Math.sin(angle2) * arcRadius;
                shapeRenderer.rectLine(x1, y1, x2, y2, arcThickness);
            }
        }

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
     * Set whether it is currently night (triggers police warning banner).
     */
    public void setNight(boolean night) {
        this.isNight = night;
    }

    /**
     * Whether night mode is currently active.
     */
    public boolean isNight() {
        return isNight;
    }

    /**
     * Set block break progress for crosshair indicator.
     */
    public void setBlockBreakProgress(float progress) {
        this.blockBreakProgress = Math.max(0f, Math.min(1f, progress));
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

    /**
     * Render street reputation below the clock/weather display area.
     * Positioned at screenHeight - 105 to avoid overlapping the date and time lines.
     */
    private void renderReputation(SpriteBatch spriteBatch, BitmapFont font,
                                  int screenWidth, int screenHeight) {
        StreetReputation rep = player.getStreetReputation();
        spriteBatch.begin();

        // Choose color based on reputation level
        switch (rep.getLevel()) {
            case NOBODY:
                font.setColor(0.7f, 0.7f, 0.7f, 1f); // Grey
                break;
            case KNOWN:
                font.setColor(1f, 0.8f, 0.2f, 1f); // Yellow
                break;
            case NOTORIOUS:
                font.setColor(1f, 0.2f, 0.2f, 1f); // Red
                break;
        }

        String repText = "Rep: " + rep.getLevel().name() + " (" + rep.getPoints() + ")";
        font.draw(spriteBatch, repText, screenWidth - 200, screenHeight - 105);
        font.setColor(Color.WHITE);
        spriteBatch.end();
    }

    /**
     * Render a night warning banner at the bottom of the screen.
     * Helps players know police are active.
     */
    private void renderNightWarning(SpriteBatch spriteBatch, BitmapFont font,
                                    int screenWidth, int screenHeight) {
        spriteBatch.begin();
        font.setColor(0.9f, 0.3f, 0.3f, 1f); // Red warning text
        String nightText = "NIGHT — POLICE ACTIVE";
        font.draw(spriteBatch, nightText, screenWidth / 2f - 80, 40);
        font.setColor(Color.WHITE);
        spriteBatch.end();
    }
}

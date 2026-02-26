package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.core.Faction;
import ragamuffin.core.FactionSystem;
import ragamuffin.core.StreetReputation;
import ragamuffin.core.Weather;
import ragamuffin.entity.DamageReason;
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

    // Faction status strip (Phase 8d / Issue #702)
    private static final float FACTION_BAR_WIDTH  = 80f;
    private static final float FACTION_BAR_HEIGHT = 10f;
    private static final float FACTION_BAR_GAP    = 4f;

    private final Player player;
    private FactionSystem factionSystem; // may be null if not yet initialised
    private boolean visible;
    private Weather currentWeather;
    private float blockBreakProgress; // 0.0 to 1.0
    private boolean isNight; // Whether it is currently night (police active)
    private String targetName; // Name of the block or NPC currently targeted (null = nothing)

    // Damage reason display
    private float damageReasonTimer; // > 0 while reason banner is visible
    private static final float DAMAGE_REASON_DURATION = 2.5f;
    private String damageReasonText; // Text to display (null = nothing)

    public GameHUD(Player player) {
        this.player = player;
        this.visible = true;
        this.currentWeather = Weather.CLEAR;
        this.blockBreakProgress = 0f;
        this.isNight = false;
        this.targetName = null;
        this.damageReasonTimer = 0f;
        this.damageReasonText = null;
        this.factionSystem = null;
    }

    /**
     * Attach the FactionSystem so the HUD can render the faction status strip.
     * Call once after the faction system is initialised.
     */
    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    /** Returns the attached FactionSystem, or null. */
    public FactionSystem getFactionSystem() {
        return factionSystem;
    }

    /**
     * Update HUD timers. Call once per frame.
     */
    public void update(float delta) {
        if (damageReasonTimer > 0) {
            damageReasonTimer = Math.max(0, damageReasonTimer - delta);
        }
    }

    /**
     * Show a damage reason banner on screen for a fixed duration.
     * Call this whenever the player takes damage.
     */
    public void showDamageReason(DamageReason reason) {
        damageReasonText = reason.getDisplayName();
        damageReasonTimer = DAMAGE_REASON_DURATION;
    }

    /**
     * Get current damage reason text (null if not displaying).
     */
    public String getDamageReasonText() {
        return damageReasonTimer > 0 ? damageReasonText : null;
    }

    /**
     * Get the remaining time the damage reason banner will display.
     */
    public float getDamageReasonTimer() {
        return damageReasonTimer;
    }

    /**
     * Render the HUD overlay.
     */
    public void render(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                       int screenWidth, int screenHeight) {
        render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight, null, true);
    }

    /**
     * Render the HUD overlay and register hover tooltip zones.
     */
    public void render(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                       int screenWidth, int screenHeight, HoverTooltipSystem hoverTooltips) {
        render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight, hoverTooltips, true);
    }

    /**
     * Render the HUD overlay. When {@code showCrosshair} is false, the crosshair
     * and block-break progress arc are omitted (e.g. when an inventory/crafting/help
     * overlay is open) while status bars, weather, and reputation still render.
     */
    public void render(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                       int screenWidth, int screenHeight, HoverTooltipSystem hoverTooltips,
                       boolean showCrosshair) {
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

        // Render crosshair and target name only when no UI overlay is blocking
        if (showCrosshair) {
            renderCrosshair(spriteBatch, shapeRenderer, font, screenWidth, screenHeight, hoverTooltips);
        }

        // Render damage reason banner
        if (damageReasonTimer > 0 && damageReasonText != null) {
            renderDamageReason(spriteBatch, font, screenWidth, screenHeight);
        }

        // Render faction status strip (Phase 8d)
        if (factionSystem != null) {
            renderFactionStrip(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
        }
    }

    private void renderStatusBars(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                                  int screenWidth, int screenHeight, HoverTooltipSystem hoverTooltips) {
        float x = BAR_MARGIN;
        float y1 = screenHeight - BAR_MARGIN - BAR_HEIGHT;
        float y2 = y1 - BAR_SPACING;
        float y3 = y2 - BAR_SPACING;
        float y4 = y3 - BAR_SPACING; // Dodge indicator row
        float y5 = y4 - BAR_SPACING; // Warmth bar (Issue #698)
        float y6 = y5 - BAR_SPACING; // Wetness bar (Issue #698)

        float healthPct = player.getHealth() / Player.MAX_HEALTH;
        float hungerPct = player.getHunger() / Player.MAX_HUNGER;
        float energyPct = player.getEnergy() / Player.MAX_ENERGY;
        float warmthPct = player.getWarmth() / Player.MAX_WARMTH;   // Issue #698
        float wetnessPct = player.getWetness() / Player.MAX_WETNESS; // Issue #698

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
        // Warmth and Wetness bar backgrounds (Issue #698)
        shapeRenderer.rect(x, y5, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.rect(x, y6, BAR_WIDTH, BAR_HEIGHT);
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
        // Warmth bar: orange-red gradient; red when dangerous (Issue #698)
        if (warmthPct > 0) {
            if (player.isWarmthDangerous()) {
                shapeRenderer.setColor(0.9f, 0.1f, 0.1f, 1f); // Red = danger
            } else {
                shapeRenderer.setColor(1.0f, 0.5f, 0.1f, 1f); // Orange = ok
            }
            shapeRenderer.rect(x, y5, BAR_WIDTH * warmthPct, BAR_HEIGHT);
        }
        // Wetness bar: blue (Issue #698)
        if (wetnessPct > 0) {
            shapeRenderer.setColor(0.2f, 0.4f, 0.9f, 1f); // Blue
            shapeRenderer.rect(x, y6, BAR_WIDTH * wetnessPct, BAR_HEIGHT);
        }
        shapeRenderer.end();

        // All borders in one batch
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x, y1, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.rect(x, y2, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.rect(x, y3, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.rect(x, y4, DODGE_BAR_WIDTH, DODGE_BAR_HEIGHT);
        shapeRenderer.rect(x, y5, BAR_WIDTH, BAR_HEIGHT); // Warmth border
        shapeRenderer.rect(x, y6, BAR_WIDTH, BAR_HEIGHT); // Wetness border
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
        // Warmth and Wetness labels (Issue #698)
        String warmthLabel = player.isWarmthDangerous() ? "WARMTH: COLD!" : "Warmth: " + (int)(warmthPct * 100) + "%";
        font.draw(spriteBatch, warmthLabel, x + 5, y5 + BAR_HEIGHT - 5);
        font.draw(spriteBatch, "Wet: " + (int)(wetnessPct * 100) + "%", x + 5, y6 + BAR_HEIGHT - 5);
        spriteBatch.end();

        if (hoverTooltips != null) {
            hoverTooltips.addZone(x, y1, BAR_WIDTH, BAR_HEIGHT, "Health: " + (int)(healthPct * 100) + "%");
            hoverTooltips.addZone(x, y2, BAR_WIDTH, BAR_HEIGHT, "Hunger: " + (int)(hungerPct * 100) + "%");
            hoverTooltips.addZone(x, y3, BAR_WIDTH, BAR_HEIGHT, "Energy: " + (int)(energyPct * 100) + "%");
            hoverTooltips.addZone(x, y4, DODGE_BAR_WIDTH, DODGE_BAR_HEIGHT,
                    dodgeReady ? "Dodge ready — press Ctrl while moving" : "Dodge on cooldown");
            hoverTooltips.addZone(x, y5, BAR_WIDTH, BAR_HEIGHT,
                    "Warmth: " + (int)(warmthPct * 100) + "% — wear a coat or stand near a campfire");
            hoverTooltips.addZone(x, y6, BAR_WIDTH, BAR_HEIGHT,
                    "Wetness: " + (int)(wetnessPct * 100) + "% — use an umbrella to stay dry");
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

    private void renderCrosshair(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                                 int screenWidth, int screenHeight, HoverTooltipSystem hoverTooltips) {
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

        // Draw target name label below the crosshair
        if (targetName != null) {
            spriteBatch.begin();
            font.setColor(1f, 1f, 1f, 0.9f);
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout =
                    new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, targetName);
            float textX = centerX - layout.width / 2f;
            float textY = centerY - CROSSHAIR_SIZE - CROSSHAIR_GAP - 8f;
            font.draw(spriteBatch, layout, textX, textY);
            font.setColor(Color.WHITE);
            spriteBatch.end();
        }

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
     * Set the name of the object currently under the crosshair.
     * Pass null (or empty string) to clear the label.
     */
    public void setTargetName(String name) {
        this.targetName = (name != null && !name.isEmpty()) ? name : null;
    }

    /**
     * Get the current target name shown below the crosshair.
     */
    public String getTargetName() {
        return targetName;
    }

    /**
     * Set block break progress for crosshair indicator.
     */
    public void setBlockBreakProgress(float progress) {
        this.blockBreakProgress = Math.max(0f, Math.min(1f, progress));
    }

    /**
     * Get current block break progress (0.0 to 1.0).
     */
    public float getBlockBreakProgress() {
        return blockBreakProgress;
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
     * Render street reputation as a GTA-style star display.
     * Positioned at screenHeight - 105 to avoid overlapping the date and time lines.
     *
     * Filled stars (★) are shown for earned reputation; empty stars (☆) for remaining.
     * Colour scales from grey (0 stars) through yellow (1–2) to red (3–5).
     */
    private void renderReputation(SpriteBatch spriteBatch, BitmapFont font,
                                  int screenWidth, int screenHeight) {
        StreetReputation rep = player.getStreetReputation();
        int stars = rep.getStarCount();

        // Only render when the player has earned at least one star
        if (stars == 0) {
            return;
        }

        // Tint stars based on ReputationLevel: gold for KNOWN, red for NOTORIOUS
        if (rep.isNotorious()) {
            font.setColor(1f, 0.2f, 0.2f, 1f);   // Red — notorious
        } else {
            font.setColor(1f, 0.8f, 0.2f, 1f);   // Gold — known
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(i < stars ? "\u2605" : "\u2606"); // ★ / ☆
        }

        spriteBatch.begin();
        // Top-right star display (existing)
        font.draw(spriteBatch, sb.toString(), screenWidth - 200, screenHeight - 105);

        // Fix #687: Bottom-right rep indicator
        font.getData().setScale(0.8f);
        String repLabel = "REP: " + rep.getPoints() + " (" + rep.getLevel().name() + ")";
        font.draw(spriteBatch, repLabel, screenWidth - 200, BAR_MARGIN + BAR_HEIGHT);
        font.getData().setScale(1.0f);

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

    /**
     * Render the damage reason banner near the top-centre of the screen.
     * Fades out as the timer expires.
     */
    private void renderDamageReason(SpriteBatch spriteBatch, BitmapFont font,
                                    int screenWidth, int screenHeight) {
        float alpha = Math.min(1f, damageReasonTimer / (DAMAGE_REASON_DURATION * 0.3f));
        spriteBatch.begin();
        font.setColor(1f, 0.3f, 0.3f, alpha);
        GlyphLayout layout = new GlyphLayout(font, damageReasonText);
        float textX = screenWidth / 2f - layout.width / 2f;
        float textY = screenHeight * 0.72f;
        font.draw(spriteBatch, layout, textX, textY);
        font.setColor(Color.WHITE);
        spriteBatch.end();
    }

    /**
     * Render the three-faction Respect bars below the hotbar (Phase 8d / Issue #702).
     *
     * <p>Each bar shows the faction's colour at its current Respect percentage.
     * Bars pulse when Respect has just changed.
     */
    private void renderFactionStrip(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer,
                                    BitmapFont font, int screenWidth, int screenHeight) {
        Faction[] factions = Faction.values();
        // Position the strip just below the hotbar area (bottom of screen)
        float stripY = BAR_MARGIN + FACTION_BAR_HEIGHT + 2f;
        float startX = BAR_MARGIN;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < factions.length; i++) {
            Faction f = factions[i];
            float barX = startX + i * (FACTION_BAR_WIDTH + FACTION_BAR_GAP);

            // Background
            shapeRenderer.setColor(0.15f, 0.15f, 0.15f, 0.85f);
            shapeRenderer.rect(barX, stripY, FACTION_BAR_WIDTH, FACTION_BAR_HEIGHT);

            // Filled portion
            float pct = factionSystem.getRespect(f) / (float) FactionSystem.MAX_RESPECT;
            Color base = f.getHudColor();
            boolean pulsing = factionSystem.isHudPulsing(f);
            if (pulsing) {
                // Pulse: brighten the bar
                shapeRenderer.setColor(
                        Math.min(1f, base.r + 0.3f),
                        Math.min(1f, base.g + 0.3f),
                        Math.min(1f, base.b + 0.3f),
                        1f);
            } else {
                shapeRenderer.setColor(base.r, base.g, base.b, 1f);
            }
            if (pct > 0f) {
                shapeRenderer.rect(barX, stripY, FACTION_BAR_WIDTH * pct, FACTION_BAR_HEIGHT);
            }
        }
        shapeRenderer.end();

        // Borders + labels
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        for (int i = 0; i < factions.length; i++) {
            float barX = startX + i * (FACTION_BAR_WIDTH + FACTION_BAR_GAP);
            shapeRenderer.rect(barX, stripY, FACTION_BAR_WIDTH, FACTION_BAR_HEIGHT);
        }
        shapeRenderer.end();

        // Faction abbreviations
        spriteBatch.begin();
        font.getData().setScale(0.6f);
        font.setColor(Color.WHITE);
        for (int i = 0; i < factions.length; i++) {
            Faction f = factions[i];
            float barX = startX + i * (FACTION_BAR_WIDTH + FACTION_BAR_GAP);
            String label = f.getDisplayName().substring(0, Math.min(3, f.getDisplayName().length()))
                    + " " + factionSystem.getRespect(f);
            font.draw(spriteBatch, label, barX + 2, stripY + FACTION_BAR_HEIGHT - 1);
        }
        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);
        spriteBatch.end();
    }
}

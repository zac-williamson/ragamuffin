package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.core.Faction;
import ragamuffin.core.FactionSystem;
import ragamuffin.core.MCBattleSystem;
import ragamuffin.core.NotorietySystem;
import ragamuffin.core.RaveSystem;
import ragamuffin.core.StreetReputation;
import ragamuffin.core.Weather;
import ragamuffin.core.WitnessSystem;
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
    private NotorietySystem notorietySystem; // Phase 8e — may be null if not yet initialised
    private MCBattleSystem mcBattleSystem;   // Issue #716 — may be null if not yet initialised
    private RaveSystem raveSystem;           // Issue #716 — may be null if not yet initialised
    private WitnessSystem witnessSystem;     // Issue #765 — may be null if not yet initialised
    private ragamuffin.core.PirateRadioSystem pirateRadioSystem; // Issue #783 — may be null
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
        this.pirateRadioSystem = null;
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
     * Attach the NotorietySystem so the HUD can render the notoriety star cluster (Phase 8e).
     * Call once after the notoriety system is initialised.
     */
    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    /** Returns the attached NotorietySystem, or null. */
    public NotorietySystem getNotorietySystem() {
        return notorietySystem;
    }

    /**
     * Attach the MCBattleSystem so the HUD can render the MC Rank microphone icon (Issue #716).
     * Call once after the system is initialised.
     */
    public void setMcBattleSystem(MCBattleSystem mcBattleSystem) {
        this.mcBattleSystem = mcBattleSystem;
    }

    /** Returns the attached MCBattleSystem, or null. */
    public MCBattleSystem getMcBattleSystem() {
        return mcBattleSystem;
    }

    /**
     * Attach the RaveSystem so the HUD can render the rave-active indicator (Issue #716).
     * Call once after the system is initialised.
     */
    public void setRaveSystem(RaveSystem raveSystem) {
        this.raveSystem = raveSystem;
    }

    /** Returns the attached RaveSystem, or null. */
    public RaveSystem getRaveSystem() {
        return raveSystem;
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

        // Render notoriety star cluster (Phase 8e)
        if (notorietySystem != null) {
            renderNotoriety(spriteBatch, font, screenWidth, screenHeight);
        }

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

        // Render MC Rank microphone icon (Issue #716)
        if (mcBattleSystem != null) {
            renderMcRank(spriteBatch, font, screenWidth, screenHeight);
        }

        // Render rave-active indicator (Issue #716)
        if (raveSystem != null && raveSystem.isRaveActive()) {
            renderRaveIndicator(spriteBatch, font, screenWidth, screenHeight);
        }

        // Render evidence countdown counter and CCTV vignette (Issue #765)
        if (witnessSystem != null) {
            renderEvidenceHUD(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
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
     * Render the notoriety star cluster in the top-right corner (Phase 8e / Issue #709).
     *
     * <p>Shows 0–5 filled stars corresponding to the current Street Legend Tier.
     * Stars flash briefly when a tier-up occurs. At Tier 5 the stars pulse red permanently.
     * Below the stars the player's Street Legend Title is displayed in small text.
     */
    private void renderNotoriety(SpriteBatch spriteBatch, BitmapFont font,
                                  int screenWidth, int screenHeight) {
        if (notorietySystem == null) return;
        int tier = notorietySystem.getTier();

        // Always render the notoriety cluster (even at Tier 0 — shows empty stars)
        boolean isTierUp = notorietySystem.isTierUpPending();
        boolean isTier5  = tier == 5;

        // Choose star colour
        if (isTier5) {
            // Tier 5 pulses red — simple sine-based pulse
            float pulse = (float) Math.abs(Math.sin(notorietySystem.getTierUpFlashTimer() * Math.PI));
            font.setColor(1f, pulse * 0.3f, pulse * 0.3f, 1f);
        } else if (isTierUp) {
            font.setColor(1f, 1f, 0f, 1f); // Yellow flash on tier-up
        } else if (tier >= 3) {
            font.setColor(1f, 0.3f, 0.1f, 1f); // Orange-red for high tiers
        } else if (tier >= 1) {
            font.setColor(1f, 0.8f, 0.2f, 1f); // Gold for mid tiers
        } else {
            font.setColor(0.6f, 0.6f, 0.6f, 1f); // Grey for tier 0 (no stars)
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(i < tier ? "\u2605" : "\u2606"); // ★ / ☆
        }

        // Position: below weather display (screenHeight - 50)
        float starsY = screenHeight - 50f;
        float starsX = screenWidth - 200f;

        spriteBatch.begin();
        font.draw(spriteBatch, sb.toString(), starsX, starsY);

        // Street Legend title in smaller text below the stars
        String title = "WANTED: " + notorietySystem.getTierTitle();
        font.getData().setScale(0.7f);
        font.setColor(0.9f, 0.9f, 0.9f, 1f);
        font.draw(spriteBatch, title, starsX, starsY - 18f);
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

    /**
     * Render MC Rank microphone icon in the top-right corner below the notoriety display
     * (Issue #716). Shows 0–5 microphone symbols (🎤 or unicode fallback) for current rank.
     */
    private void renderMcRank(SpriteBatch spriteBatch, BitmapFont font,
                               int screenWidth, int screenHeight) {
        if (mcBattleSystem == null) return;
        int rank = mcBattleSystem.getMcRank();
        if (rank == 0) return; // Only show once any rank is earned

        // Position: top-right, below notoriety stars
        float x = screenWidth - 200f;
        float y = screenHeight - 75f;

        spriteBatch.begin();
        font.getData().setScale(0.8f);
        // Use "mic" unicode marker; mic symbols may not render in bitmap font so use text
        font.setColor(0.92f, 0.10f, 0.55f, 1f); // Hot pink — grime palette
        StringBuilder sb = new StringBuilder("MC: ");
        for (int i = 0; i < MCBattleSystem.MAX_MC_RANK; i++) {
            sb.append(i < rank ? "\u25CF" : "\u25CB"); // ● / ○
        }
        font.draw(spriteBatch, sb.toString(), x, y);
        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);
        spriteBatch.end();
    }

    /**
     * Render the rave-active indicator in the bottom-centre of the screen (Issue #716).
     * Shows attendee count, income rate, and a countdown timer until police arrive
     * (or "FEDS COMING" once alerted).
     *
     * @param attendeeCountForRave  number of current attendees (injected by caller via
     *                             {@link #setRaveAttendeeCount(int)})
     */
    private void renderRaveIndicator(SpriteBatch spriteBatch, BitmapFont font,
                                     int screenWidth, int screenHeight) {
        if (raveSystem == null || !raveSystem.isRaveActive()) return;

        spriteBatch.begin();
        font.getData().setScale(0.85f);

        // Timer or alert text
        String timerText;
        if (raveSystem.isPoliceAlerted()) {
            font.setColor(1f, 0.1f, 0.1f, 1f); // Red — feds alerted
            timerText = "FEDS COMING! Disperse now (E)";
        } else {
            float secs = raveSystem.getSecondsUntilPolice();
            int mins    = (int)(secs / 60f);
            int remSecs = (int)(secs % 60f);
            font.setColor(0.2f, 0.9f, 0.4f, 1f); // Green — safe
            timerText = String.format("RAVE ACTIVE | Attendees: %d | Police in: %d:%02d",
                    raveAttendeeCount, mins, remSecs);
        }

        GlyphLayout layout = new GlyphLayout(font, timerText);
        float textX = screenWidth / 2f - layout.width / 2f;
        float textY = 80f;
        font.draw(spriteBatch, layout, textX, textY);

        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);
        spriteBatch.end();
    }

    // ── Witness system (Issue #765) ────────────────────────────────────────────

    /**
     * Attach the WitnessSystem so the HUD can render the evidence countdown counter
     * and CCTV vignette flash (Issue #765).
     */
    public void setWitnessSystem(WitnessSystem witnessSystem) {
        this.witnessSystem = witnessSystem;
    }

    /** Returns the attached WitnessSystem, or null. */
    public WitnessSystem getWitnessSystem() {
        return witnessSystem;
    }

    // ── Pirate Radio system (Issue #783) ──────────────────────────────────────

    /**
     * Attach the PirateRadioSystem so the HUD can render the ON AIR indicator,
     * listener count, and triangulation bar (Issue #783).
     */
    public void setPirateRadioSystem(ragamuffin.core.PirateRadioSystem pirateRadioSystem) {
        this.pirateRadioSystem = pirateRadioSystem;
    }

    /** Returns the attached PirateRadioSystem, or null. */
    public ragamuffin.core.PirateRadioSystem getPirateRadioSystem() {
        return pirateRadioSystem;
    }

    /**
     * Render the evidence countdown counter in the bottom-right corner.
     * Shows the number of active evidence props that haven't been discovered yet.
     * Also flashes a red vignette when a CCTV tape is hot.
     */
    private void renderEvidenceHUD(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer,
                                   BitmapFont font, int screenWidth, int screenHeight) {
        if (witnessSystem == null) return;

        int evidenceCount = witnessSystem.getActiveEvidenceCount();
        boolean cctvHot = witnessSystem.isCctvHot();

        // Red vignette flash when CCTV tape is hot
        if (cctvHot) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            // Semi-transparent red border vignette
            shapeRenderer.setColor(0.9f, 0.05f, 0.05f, 0.25f);
            float vw = 30f; // vignette width
            shapeRenderer.rect(0, 0, screenWidth, vw);                          // bottom
            shapeRenderer.rect(0, screenHeight - vw, screenWidth, vw);          // top
            shapeRenderer.rect(0, 0, vw, screenHeight);                          // left
            shapeRenderer.rect(screenWidth - vw, 0, vw, screenHeight);           // right
            shapeRenderer.end();
        }

        // Evidence countdown counter — bottom-right
        if (evidenceCount > 0) {
            spriteBatch.begin();
            font.getData().setScale(0.85f);
            font.setColor(cctvHot ? new Color(1f, 0.2f, 0.2f, 1f) : new Color(1f, 0.85f, 0.2f, 1f));
            String label = "EVIDENCE: " + evidenceCount;
            GlyphLayout layout = new GlyphLayout(font, label);
            font.draw(spriteBatch, layout,
                    screenWidth - layout.width - BAR_MARGIN,
                    BAR_MARGIN + 60f);
            font.getData().setScale(1.0f);
            font.setColor(Color.WHITE);
            spriteBatch.end();
        }
    }

    // ── Rave attendee count (injected by game logic each frame) ───────────────

    private int raveAttendeeCount = 0;

    /**
     * Set the current rave attendee count for HUD display.
     * Call each frame while a rave is active.
     */
    public void setRaveAttendeeCount(int count) {
        this.raveAttendeeCount = count;
    }

    /** Returns the last set rave attendee count. */
    public int getRaveAttendeeCount() {
        return raveAttendeeCount;
    }
}

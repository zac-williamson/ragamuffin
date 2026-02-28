package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.core.StreetSkillSystem;
import ragamuffin.core.StreetSkillSystem.Skill;
import ragamuffin.core.StreetSkillSystem.Tier;

/**
 * Issue #787: Street Skills &amp; Character Progression — SkillsUI overlay.
 *
 * <p>Displays all six skills as progress bars with tier labels and locked/unlocked
 * perks. Toggled with the <b>K</b> key. Sits on top of the game world.
 *
 * <h3>Layout</h3>
 * A dark semi-transparent panel in the centre of the screen, 500×420 px.
 * Each skill occupies a row with:
 * <ul>
 *   <li>Skill name label (left)</li>
 *   <li>Tier label (e.g. "Journeyman") (centre-left)</li>
 *   <li>XP progress bar spanning 5 tier segments</li>
 *   <li>Next-tier XP remaining (right)</li>
 * </ul>
 * Below the bars, a brief perk description for the current tier is shown.
 */
public class SkillsUI {

    // ── Layout constants ─────────────────────────────────────────────────────

    private static final float PANEL_W     = 520f;
    private static final float PANEL_H     = 450f;
    private static final float ROW_H       = 60f;
    private static final float BAR_H       = 12f;
    private static final float PADDING     = 18f;
    private static final float BAR_W       = 360f;
    private static final float LABEL_SCALE = 1.0f;
    private static final float DESC_SCALE  = 0.85f;

    // ── Skill colours ────────────────────────────────────────────────────────

    private static final Color[] SKILL_COLORS = {
        new Color(0.85f, 0.15f, 0.10f, 1f),  // BRAWLING  — red
        new Color(0.20f, 0.72f, 0.25f, 1f),  // GRAFTING  — green
        new Color(0.95f, 0.75f, 0.10f, 1f),  // TRADING   — gold
        new Color(0.20f, 0.50f, 0.88f, 1f),  // STEALTH   — blue
        new Color(0.75f, 0.30f, 0.90f, 1f),  // INFLUENCE — purple
        new Color(0.90f, 0.55f, 0.10f, 1f),  // SURVIVAL  — orange
    };

    // ── Perk descriptions per tier (skill × tier) ───────────────────────────

    private static final String[][] PERKS = {
        { // BRAWLING
            "Novice: baseline brawler",
            "Apprentice: +10% punch damage",
            "Journeyman: stagger on 3-hit combo",
            "Expert: 5-hit chain — 2× final blow",
            "Legend: crowd-stagger every punch"
        },
        { // GRAFTING
            "Novice: baseline forager",
            "Apprentice: +1 extra drop (50% chance)",
            "Journeyman: LEAVES edible as Bitter Greens",
            "Expert: soft blocks break in 3 hits",
            "Legend: 25% chance tool durability saved"
        },
        { // TRADING
            "Novice: baseline trader",
            "Apprentice: +10% stall income",
            "Journeyman: fence prices +15%",
            "Expert: immune to COUNCIL_CRACKDOWN",
            "Legend: halves faction protection cut"
        },
        { // STEALTH
            "Novice: baseline ghost",
            "Apprentice: 50% less noise while crouching",
            "Journeyman: police sight range −20% crouched",
            "Expert: pick-pocketing unlocked (E behind NPC)",
            "Legend: invisible to CCTV while crouching"
        },
        { // INFLUENCE
            "Novice: baseline networker",
            "Apprentice: rumour spread +25% faster",
            "Journeyman: bribery costs −20%",
            "Expert: RALLY mechanic unlocked (G key)",
            "Legend: RALLY deters gang members too"
        },
        { // SURVIVAL
            "Novice: baseline survivor",
            "Apprentice: hunger drain −20%",
            "Journeyman: fall damage −25%",
            "Expert: respawn at 50% health",
            "Legend: NEIGHBOURHOOD_EVENT unlocked"
        }
    };

    // ── State ────────────────────────────────────────────────────────────────

    private final StreetSkillSystem skillSystem;
    private boolean visible;

    // ── Construction ─────────────────────────────────────────────────────────

    public SkillsUI(StreetSkillSystem skillSystem) {
        this.skillSystem = skillSystem;
        this.visible = false;
    }

    // ── Visibility ───────────────────────────────────────────────────────────

    public boolean isVisible() { return visible; }

    public void show() { visible = true; }

    public void hide() { visible = false; }

    public void toggle() {
        if (visible) hide();
        else show();
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    /**
     * Render the Skills UI overlay.
     *
     * @param batch         active SpriteBatch (already begun)
     * @param shapeRenderer active ShapeRenderer
     * @param font          BitmapFont for labels
     * @param screenW       screen width in pixels
     * @param screenH       screen height in pixels
     */
    public void render(SpriteBatch batch, ShapeRenderer shapeRenderer,
                       BitmapFont font, float screenW, float screenH) {
        if (!visible) return;

        float panelX = (screenW - PANEL_W) / 2f;
        float panelY = (screenH - PANEL_H) / 2f;

        // ── Background panel ─────────────────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.82f);
        shapeRenderer.rect(panelX, panelY, PANEL_W, PANEL_H);
        shapeRenderer.end();

        // ── Panel border ─────────────────────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.6f, 0.6f, 0.6f, 1f);
        shapeRenderer.rect(panelX, panelY, PANEL_W, PANEL_H);
        shapeRenderer.end();

        batch.begin();

        // ── Title ─────────────────────────────────────────────────────────────
        font.getData().setScale(1.2f);
        font.setColor(Color.WHITE);
        font.draw(batch, "STREET SKILLS", panelX + PADDING, panelY + PANEL_H - PADDING);

        // ── Close hint ───────────────────────────────────────────────────────
        font.getData().setScale(0.8f);
        font.setColor(0.7f, 0.7f, 0.7f, 1f);
        font.draw(batch, "[K] close", panelX + PANEL_W - 80f, panelY + PANEL_H - PADDING);

        batch.end();

        Skill[] skills = Skill.values();
        for (int i = 0; i < skills.length; i++) {
            Skill skill = skills[i];
            float rowY = panelY + PANEL_H - PADDING - 30f - (i * ROW_H) - ROW_H;

            renderSkillRow(batch, shapeRenderer, font, skill, i,
                           panelX + PADDING, rowY);
        }

    }

    private void renderSkillRow(SpriteBatch batch, ShapeRenderer shapeRenderer,
                                 BitmapFont font, Skill skill, int colorIndex,
                                 float x, float y) {
        Tier tier = skillSystem.getTier(skill);
        int currentXP = skillSystem.getXP(skill);
        int tierLevel = tier.getLevel();
        Color skillColor = SKILL_COLORS[colorIndex];

        // ── Skill name & tier label ───────────────────────────────────────────
        batch.begin();
        font.getData().setScale(LABEL_SCALE);
        font.setColor(skillColor);
        font.draw(batch, skill.name(), x, y + 40f);

        font.getData().setScale(DESC_SCALE);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, tier.getLabel(), x + 110f, y + 40f);

        // ── Perk description ─────────────────────────────────────────────────
        font.getData().setScale(0.78f);
        font.setColor(0.85f, 0.85f, 0.85f, 1f);
        String perkText = PERKS[colorIndex][tierLevel];
        font.draw(batch, perkText, x, y + 18f);

        // ── XP remaining ─────────────────────────────────────────────────────
        int toNext = skillSystem.getXPToNextTier(skill);
        font.getData().setScale(0.78f);
        font.setColor(0.65f, 0.65f, 0.65f, 1f);
        if (tier == Tier.LEGEND) {
            font.draw(batch, "MAX", x + BAR_W + 8f, y + 30f);
        } else {
            font.draw(batch, toNext + " XP", x + BAR_W + 8f, y + 30f);
        }
        batch.end();

        // ── XP progress bar (5 segments) ─────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float segW = BAR_W / 5f;
        for (int seg = 0; seg < 5; seg++) {
            float segX = x + seg * segW;
            float segY = y + 24f;

            int segStartXP = StreetSkillSystem.TIER_XP_THRESHOLDS[seg];
            int segEndXP   = (seg < 4) ? StreetSkillSystem.TIER_XP_THRESHOLDS[seg + 1]
                                        : StreetSkillSystem.TIER_XP_THRESHOLDS[4] + 1;

            float fill;
            if (currentXP >= segEndXP) {
                fill = 1f; // whole segment filled
            } else if (currentXP <= segStartXP) {
                fill = 0f;
            } else {
                fill = (float)(currentXP - segStartXP) / (segEndXP - segStartXP);
            }

            // Background
            shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f);
            shapeRenderer.rect(segX + 1f, segY, segW - 2f, BAR_H);

            // Fill
            if (fill > 0f) {
                shapeRenderer.setColor(skillColor.r, skillColor.g, skillColor.b, 0.9f);
                shapeRenderer.rect(segX + 1f, segY, (segW - 2f) * fill, BAR_H);
            }
        }

        // Tier dividers
        shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 1f);
        for (int seg = 1; seg < 5; seg++) {
            shapeRenderer.rect(x + seg * segW - 1f, y + 24f, 2f, BAR_H);
        }

        shapeRenderer.end();
    }
}

package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.core.BuildingQuestRegistry;
import ragamuffin.core.Quest;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact always-visible quest tracker displayed in the top-right corner during gameplay.
 *
 * <p>Shows up to {@value #MAX_VISIBLE} active quests with their giver name and objective.
 * Only visible when there is at least one active quest. Hidden during opening sequence.
 */
public class QuestTrackerUI {

    static final int MAX_VISIBLE = 3;

    private static final float PANEL_WIDTH   = 260f;
    private static final float ROW_HEIGHT    = 44f;
    private static final float HEADER_HEIGHT = 24f;
    private static final float PADDING       = 8f;
    private static final float MARGIN_RIGHT  = 14f;
    private static final float MARGIN_TOP    = 14f;

    private static final float TITLE_SCALE = 0.9f;
    private static final float NAME_SCALE  = 0.85f;
    private static final float DESC_SCALE  = 0.75f;

    private final BuildingQuestRegistry questRegistry;
    private boolean visible;

    public QuestTrackerUI(BuildingQuestRegistry questRegistry) {
        this.questRegistry = questRegistry;
        this.visible = true;
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

    /**
     * Returns a list of currently active (accepted, not completed) quests.
     */
    List<Quest> getActiveQuests() {
        List<Quest> active = new ArrayList<>();
        for (LandmarkType type : LandmarkType.values()) {
            Quest q = questRegistry.getQuest(type);
            if (q != null && q.isActive() && !q.isCompleted()) {
                active.add(q);
            }
        }
        return active;
    }

    /**
     * Render the quest tracker.  Must be called with an orthographic projection
     * already set on {@code spriteBatch} and {@code shapeRenderer}.
     */
    public void render(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                       int screenWidth, int screenHeight) {
        if (!visible) return;

        List<Quest> active = getActiveQuests();
        if (active.isEmpty()) return;

        int count = Math.min(active.size(), MAX_VISIBLE);
        float panelH = HEADER_HEIGHT + count * ROW_HEIGHT + PADDING;
        float panelX = screenWidth - PANEL_WIDTH - MARGIN_RIGHT;
        float panelY = screenHeight - panelH - MARGIN_TOP;

        // Semi-transparent dark background
        com.badlogic.gdx.Gdx.gl.glEnable(GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.72f);
        shapeRenderer.rect(panelX, panelY, PANEL_WIDTH, panelH);
        shapeRenderer.end();
        com.badlogic.gdx.Gdx.gl.glDisable(GL20.GL_BLEND);

        // Left accent bar (gold stripe)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 0.75f, 0.1f, 1f);
        shapeRenderer.rect(panelX, panelY, 3f, panelH);
        shapeRenderer.end();

        spriteBatch.begin();

        // Header: "QUESTS"
        font.getData().setScale(TITLE_SCALE);
        font.setColor(1f, 0.85f, 0.2f, 1f);
        GlyphLayout titleLayout = new GlyphLayout(font, "QUESTS");
        font.draw(spriteBatch, "QUESTS",
                panelX + PANEL_WIDTH - PADDING - titleLayout.width,
                panelY + panelH - PADDING * 0.5f);

        // Quest rows
        float rowTop = panelY + panelH - HEADER_HEIGHT;
        for (int i = 0; i < count; i++) {
            Quest quest = active.get(i);
            float rowY = rowTop - i * ROW_HEIGHT;

            // Quest giver name
            font.getData().setScale(NAME_SCALE);
            font.setColor(1f, 1f, 1f, 1f);
            String giverStr = "\u25CF " + quest.getGiver();
            font.draw(spriteBatch, giverStr, panelX + PADDING, rowY);

            // Objective (truncated if needed)
            font.getData().setScale(DESC_SCALE);
            font.setColor(0.78f, 0.78f, 0.78f, 1f);
            String desc = quest.getDescription();
            font.draw(spriteBatch, desc, panelX + PADDING + 6f, rowY - 18f);

            // Progress hint for COLLECT quests
            if (quest.getRequiredMaterial() != null) {
                font.getData().setScale(DESC_SCALE);
                font.setColor(1f, 0.65f, 0.2f, 1f);
                String mat = quest.getRequiredMaterial().name().toLowerCase().replace('_', ' ');
                String progress = quest.getRequiredCount() + "x " + mat;
                GlyphLayout progLayout = new GlyphLayout(font, progress);
                font.draw(spriteBatch, progress,
                        panelX + PANEL_WIDTH - PADDING - progLayout.width,
                        rowY - 18f);
            }
        }

        // Overflow indicator
        if (active.size() > MAX_VISIBLE) {
            font.getData().setScale(DESC_SCALE);
            font.setColor(0.55f, 0.55f, 0.55f, 1f);
            String more = "+" + (active.size() - MAX_VISIBLE) + " more  (Q)";
            GlyphLayout moreLayout = new GlyphLayout(font, more);
            font.draw(spriteBatch, more,
                    panelX + PANEL_WIDTH - PADDING - moreLayout.width,
                    panelY + PADDING * 1.5f);
        }

        font.getData().setScale(1.2f); // restore default
        font.setColor(Color.WHITE);
        spriteBatch.end();
    }
}

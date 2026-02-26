package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.building.Inventory;
import ragamuffin.core.BuildingQuestRegistry;
import ragamuffin.core.Quest;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.List;

/**
 * Overlay screen showing all available quests, their status (inactive/active/completed),
 * objectives, and rewards.  Toggled with the Q key.
 *
 * <p>Navigation: UP/DOWN arrow keys scroll the list; ESC or Q closes the screen.
 */
public class QuestLogUI {

    private static final float ROW_HEIGHT     = 72f;
    private static final float PADDING        = 16f;
    private static final float TITLE_SCALE    = 1.4f;
    private static final float NAME_SCALE     = 1.1f;
    private static final float DESC_SCALE     = 0.85f;
    private static final float HINT_SCALE     = 0.85f;
    private static final int   VISIBLE_ROWS   = 7;

    private final BuildingQuestRegistry questRegistry;
    private final Inventory inventory;
    private boolean visible;

    /** Top-most row index in the visible window. */
    private int scrollOffset;

    public QuestLogUI(BuildingQuestRegistry questRegistry, Inventory inventory) {
        this.questRegistry = questRegistry;
        this.inventory = inventory;
        this.visible = false;
        this.scrollOffset = 0;
    }

    /** Convenience constructor for tests or contexts without an inventory reference. */
    public QuestLogUI(BuildingQuestRegistry questRegistry) {
        this(questRegistry, null);
    }

    public boolean isVisible() {
        return visible;
    }

    public void show() {
        visible = true;
        scrollOffset = 0;
    }

    public void hide() {
        visible = false;
    }

    public void toggle() {
        if (visible) hide();
        else show();
    }

    /** Scroll the list up (show earlier quests). */
    public void scrollUp() {
        if (scrollOffset > 0) scrollOffset--;
    }

    /** Scroll the list down (show later quests). */
    public void scrollDown() {
        int total = getQuestList().size();
        int maxOffset = Math.max(0, total - VISIBLE_ROWS);
        if (scrollOffset < maxOffset) scrollOffset++;
    }

    /** Current scroll offset (for tests). */
    public int getScrollOffset() {
        return scrollOffset;
    }

    /**
     * Build an ordered list of started quests from the registry.
     * Only quests that have been started (active or completed) are shown.
     * Active and incomplete quests appear first, then completed quests.
     */
    List<Quest> getQuestList() {
        List<Quest> active = new ArrayList<>();
        List<Quest> completed = new ArrayList<>();

        for (LandmarkType type : LandmarkType.values()) {
            Quest q = questRegistry.getQuest(type);
            if (q == null) continue;
            if (q.isCompleted()) {
                completed.add(q);
            } else if (q.isActive()) {
                active.add(q);
            }
            // Not-yet-started quests (inactive and not completed) are hidden
        }

        List<Quest> all = new ArrayList<>();
        all.addAll(active);
        all.addAll(completed);
        return all;
    }

    /**
     * Render the quest log overlay.  Must be called with an orthographic
     * projection already set on {@code spriteBatch} and {@code shapeRenderer}.
     */
    public void render(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                       int screenWidth, int screenHeight) {
        if (!visible) return;

        List<Quest> quests = getQuestList();
        int total = quests.size();

        long activeCount    = quests.stream().filter(q -> q.isActive() && !q.isCompleted()).count();
        long completedCount = quests.stream().filter(Quest::isCompleted).count();

        float panelW = Math.min(740f, screenWidth - 60f);
        float panelH = Math.min(VISIBLE_ROWS * ROW_HEIGHT + 90f, screenHeight - 60f);
        float panelX = (screenWidth - panelW) / 2f;
        float panelY = (screenHeight - panelH) / 2f;

        // Semi-transparent dark background panel
        com.badlogic.gdx.Gdx.gl.glEnable(GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.88f);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.end();
        com.badlogic.gdx.Gdx.gl.glDisable(GL20.GL_BLEND);

        // Panel border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1f);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.end();

        spriteBatch.begin();

        // Title
        font.getData().setScale(TITLE_SCALE);
        font.setColor(Color.YELLOW);
        String title = "QUEST LOG  [" + activeCount + " active / " + completedCount + " completed]";
        GlyphLayout titleLayout = new GlyphLayout(font, title);
        font.draw(spriteBatch, title,
                  panelX + (panelW - titleLayout.width) / 2f,
                  panelY + panelH - PADDING);

        // Close hint
        font.getData().setScale(HINT_SCALE);
        font.setColor(0.6f, 0.6f, 0.6f, 1f);
        font.draw(spriteBatch, "ESC to close  |  UP/DOWN to scroll",
                  panelX + PADDING, panelY + PADDING * 1.8f);

        // Rows
        float rowAreaTop = panelY + panelH - 58f; // Below title
        int end = Math.min(scrollOffset + VISIBLE_ROWS, total);

        for (int i = scrollOffset; i < end; i++) {
            Quest quest = quests.get(i);
            float rowY = rowAreaTop - (i - scrollOffset) * ROW_HEIGHT;
            float rowX = panelX + PADDING;

            // Alternating row background
            if ((i % 2) == 0) {
                spriteBatch.end();
                com.badlogic.gdx.Gdx.gl.glEnable(GL20.GL_BLEND);
                com.badlogic.gdx.Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(1f, 1f, 1f, 0.04f);
                shapeRenderer.rect(panelX + 1, rowY - ROW_HEIGHT + 4f, panelW - 2, ROW_HEIGHT);
                shapeRenderer.end();
                com.badlogic.gdx.Gdx.gl.glDisable(GL20.GL_BLEND);
                spriteBatch.begin();
            }

            // Status indicator and colour
            String statusPrefix;
            if (quest.isCompleted()) {
                font.setColor(0.5f, 0.8f, 0.5f, 1f); // Green for completed
                statusPrefix = "\u2713 ";
            } else if (quest.isActive()) {
                font.setColor(1f, 0.85f, 0.2f, 1f); // Gold for active
                statusPrefix = "\u25CF ";
            } else {
                font.setColor(0.5f, 0.5f, 0.5f, 1f); // Grey for not yet started
                statusPrefix = "\u25CB ";
            }

            // Quest giver / name line
            font.getData().setScale(NAME_SCALE);
            String nameStr = statusPrefix + quest.getGiver();
            font.draw(spriteBatch, nameStr, rowX, rowY);

            // Description line
            font.getData().setScale(DESC_SCALE);
            Color descColor = quest.isCompleted()
                    ? new Color(0.6f, 0.8f, 0.6f, 1f)
                    : (quest.isActive() ? new Color(0.9f, 0.9f, 0.9f, 1f) : new Color(0.45f, 0.45f, 0.45f, 1f));
            font.setColor(descColor);
            font.draw(spriteBatch, quest.getDescription(), rowX + 14f, rowY - 18f);

            // Reward hint (right-aligned)
            if (quest.getReward() != null && !quest.isCompleted()) {
                font.getData().setScale(DESC_SCALE);
                String rewardMat = quest.getReward().name().toLowerCase().replace('_', ' ');
                String rewardStr = "Reward: " + quest.getRewardCount() + "x " + rewardMat;
                font.setColor(0.6f, 0.85f, 1f, 1f);
                GlyphLayout rewardLayout = new GlyphLayout(font, rewardStr);
                font.draw(spriteBatch, rewardStr,
                          panelX + panelW - PADDING - rewardLayout.width,
                          rowY);
            }

            // Objective summary (required items) — shown only for active incomplete quests
            if (quest.isActive() && !quest.isCompleted() && quest.getRequiredMaterial() != null) {
                font.getData().setScale(DESC_SCALE);
                String reqMat = quest.getRequiredMaterial().name().toLowerCase().replace('_', ' ');
                int required = quest.getRequiredCount();
                int current = inventory != null ? inventory.getItemCount(quest.getRequiredMaterial()) : 0;
                String objStr = "Have: " + current + "/" + required + "x " + reqMat;
                font.setColor(1f, 0.7f, 0.3f, 1f);
                GlyphLayout objLayout = new GlyphLayout(font, objStr);
                font.draw(spriteBatch, objStr,
                          panelX + panelW - PADDING - objLayout.width,
                          rowY - 18f);
            }
        }

        // Scroll indicators
        font.getData().setScale(1.0f);
        font.setColor(0.7f, 0.7f, 0.7f, 1f);
        if (scrollOffset > 0) {
            font.draw(spriteBatch, "\u25B2 more above",
                      panelX + PADDING, rowAreaTop + 14f);
        }
        if (end < total) {
            float moreY = rowAreaTop - (end - scrollOffset) * ROW_HEIGHT + ROW_HEIGHT - 4f;
            font.draw(spriteBatch, "\u25BC more below", panelX + PADDING, moreY);
        }

        font.getData().setScale(1.2f); // restore default
        font.setColor(Color.WHITE);
        spriteBatch.end();
    }
}

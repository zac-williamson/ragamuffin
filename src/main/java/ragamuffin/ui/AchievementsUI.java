package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Overlay screen showing all achievements, their descriptions, unlock status,
 * and progress counts.  Shown from the pause menu.
 *
 * <p>Navigation: UP/DOWN arrow keys scroll the list; ESC or the achievements
 * key closes the screen.
 */
public class AchievementsUI {

    private static final float ROW_HEIGHT     = 52f;
    private static final float PADDING        = 16f;
    private static final float NAME_SCALE     = 1.1f;
    private static final float DESC_SCALE     = 0.9f;
    private static final float PROGRESS_SCALE = 0.85f;

    private final AchievementSystem achievementSystem;
    private boolean visible;

    /** Top-most row index in the visible window. */
    private int scrollOffset;

    private static final int VISIBLE_ROWS = 8;

    public AchievementsUI(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
        this.visible = false;
        this.scrollOffset = 0;
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

    /** Scroll the list up (show earlier achievements). */
    public void scrollUp() {
        if (scrollOffset > 0) scrollOffset--;
    }

    /** Scroll the list down (show later achievements). */
    public void scrollDown() {
        int total = AchievementType.values().length;
        int maxOffset = Math.max(0, total - VISIBLE_ROWS);
        if (scrollOffset < maxOffset) scrollOffset++;
    }

    /**
     * Render the achievements overlay.  Must be called with an orthographic
     * projection already set on {@code spriteBatch} and {@code shapeRenderer}.
     */
    public void render(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                       int screenWidth, int screenHeight) {
        if (!visible) return;

        AchievementType[] types = AchievementType.values();
        int total = types.length;
        int unlocked = achievementSystem.getUnlockedCount();

        float panelW = Math.min(700f, screenWidth - 60f);
        float panelH = Math.min(VISIBLE_ROWS * ROW_HEIGHT + 80f, screenHeight - 60f);
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
        font.getData().setScale(1.4f);
        font.setColor(Color.YELLOW);
        String title = "ACHIEVEMENTS  [" + unlocked + "/" + total + "]";
        GlyphLayout titleLayout = new GlyphLayout(font, title);
        font.draw(spriteBatch, title,
                  panelX + (panelW - titleLayout.width) / 2f,
                  panelY + panelH - PADDING);

        // Close hint
        font.getData().setScale(0.85f);
        font.setColor(0.6f, 0.6f, 0.6f, 1f);
        font.draw(spriteBatch, "ESC to close  |  UP/DOWN to scroll",
                  panelX + PADDING, panelY + PADDING * 1.8f);

        // Rows
        float rowAreaTop = panelY + panelH - 55f; // Below title
        int end = Math.min(scrollOffset + VISIBLE_ROWS, total);

        for (int i = scrollOffset; i < end; i++) {
            AchievementType type = types[i];
            boolean isUnlocked = achievementSystem.isUnlocked(type);
            int prog = achievementSystem.getProgress(type);
            int target = type.getProgressTarget();

            float rowY = rowAreaTop - (i - scrollOffset) * ROW_HEIGHT;
            float rowX = panelX + PADDING;

            // Alternating row background
            if ((i % 2) == 0) {
                spriteBatch.end(); // Fix #667: must end spriteBatch before starting shapeRenderer
                com.badlogic.gdx.Gdx.gl.glEnable(GL20.GL_BLEND);
                com.badlogic.gdx.Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(1f, 1f, 1f, 0.04f);
                shapeRenderer.rect(panelX + 1, rowY - ROW_HEIGHT + 4f, panelW - 2, ROW_HEIGHT);
                shapeRenderer.end();
                com.badlogic.gdx.Gdx.gl.glDisable(GL20.GL_BLEND);
                // Re-begin spriteBatch after shape interruption
                spriteBatch.begin();
            }

            // Achievement name
            font.getData().setScale(NAME_SCALE);
            if (isUnlocked) {
                font.setColor(1f, 0.85f, 0.2f, 1f); // Gold for unlocked
            } else {
                font.setColor(0.6f, 0.6f, 0.6f, 1f); // Grey for locked
            }
            String nameStr = (isUnlocked ? "\u2713 " : "\u2610 ") + type.getName();
            font.draw(spriteBatch, nameStr, rowX, rowY);

            // Description
            font.getData().setScale(DESC_SCALE);
            font.setColor(isUnlocked ? new Color(0.9f, 0.9f, 0.9f, 1f) : new Color(0.45f, 0.45f, 0.45f, 1f));
            font.draw(spriteBatch, type.getDescription(), rowX + 16f, rowY - 16f);

            // Progress (only for multi-step achievements)
            if (target > 1) {
                font.getData().setScale(PROGRESS_SCALE);
                String progStr = prog + "/" + target;
                font.setColor(isUnlocked ? new Color(0.5f, 1f, 0.5f, 1f) : new Color(0.4f, 0.6f, 0.4f, 1f));
                GlyphLayout progLayout = new GlyphLayout(font, progStr);
                font.draw(spriteBatch, progStr,
                          panelX + panelW - PADDING - progLayout.width,
                          rowY);
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

    /** Current scroll offset (for tests). */
    public int getScrollOffset() {
        return scrollOffset;
    }
}

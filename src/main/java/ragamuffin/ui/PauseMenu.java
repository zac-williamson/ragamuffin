package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Pause menu displayed when the game is paused (ESC key).
 */
public class PauseMenu {
    private boolean visible;
    private int selectedOption;
    private static final int OPTION_RESUME = 0;
    private static final int OPTION_ACHIEVEMENTS = 1;
    private static final int OPTION_RESTART = 2;
    private static final int OPTION_QUIT = 3;
    private static final int NUM_OPTIONS = 4;

    private static final String[] OPTIONS = {
        "Resume",
        "Achievements",
        "Restart",
        "Quit"
    };

    public PauseMenu() {
        this.visible = false;
        this.selectedOption = OPTION_RESUME;
    }

    /**
     * Render the pause menu as an overlay.
     */
    public void render(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                       int screenWidth, int screenHeight) {
        if (!visible) {
            return;
        }

        // Draw semi-transparent background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.7f);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();

        // Draw title
        spriteBatch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(2.0f);
        String title = "PAUSED";
        GlyphLayout titleLayout = new GlyphLayout(font, title);
        font.draw(spriteBatch, title, screenWidth / 2f - titleLayout.width / 2, screenHeight * 0.7f);
        font.getData().setScale(1.2f);

        // Draw options
        float optionY = screenHeight * 0.5f;
        float optionSpacing = 50f;

        for (int i = 0; i < OPTIONS.length; i++) {
            String option = OPTIONS[i];
            if (i == selectedOption) {
                font.setColor(Color.YELLOW);
                option = "> " + option + " <";
            } else {
                font.setColor(Color.WHITE);
            }

            GlyphLayout optionLayout = new GlyphLayout(font, option);
            font.draw(spriteBatch, option, screenWidth / 2f - optionLayout.width / 2, optionY - i * optionSpacing);
        }

        spriteBatch.end();
    }

    public boolean isVisible() {
        return visible;
    }

    public void show() {
        visible = true;
        selectedOption = OPTION_RESUME;
    }

    public void hide() {
        visible = false;
    }

    public void toggle() {
        visible = !visible;
        if (visible) {
            selectedOption = OPTION_RESUME;
        }
    }

    /**
     * Move selection up.
     */
    public void selectPrevious() {
        selectedOption = (selectedOption - 1 + NUM_OPTIONS) % NUM_OPTIONS;
    }

    /**
     * Move selection down.
     */
    public void selectNext() {
        selectedOption = (selectedOption + 1) % NUM_OPTIONS;
    }

    /**
     * Get the currently selected option.
     */
    public int getSelectedOption() {
        return selectedOption;
    }

    public boolean isResumeSelected() {
        return selectedOption == OPTION_RESUME;
    }

    public boolean isAchievementsSelected() {
        return selectedOption == OPTION_ACHIEVEMENTS;
    }

    public boolean isRestartSelected() {
        return selectedOption == OPTION_RESTART;
    }

    public boolean isQuitSelected() {
        return selectedOption == OPTION_QUIT;
    }

    /**
     * Handle mouse click. Returns the selected option index (0=Resume, 1=Achievements, 2=Restart, 3=Quit)
     * or -1 if click was not on any option.
     * screenX/screenY are in LibGDX screen coords (0,0 = top-left).
     */
    public int handleClick(int screenX, int screenY, int screenWidth, int screenHeight) {
        if (!visible) return -1;

        float optionStartY = screenHeight * 0.5f;
        float optionSpacing = 50f;
        float optionHeight = 30f;
        float optionHalfWidth = 120f;
        float centerX = screenWidth / 2f;

        for (int i = 0; i < NUM_OPTIONS; i++) {
            // Option baseline Y in UI/OpenGL coords (0=bottom): optionStartY - i * optionSpacing
            // Convert baseline to screen coords (0=top): screenHeight - (optionStartY - i * optionSpacing)
            // Text renders above its baseline, so in screen coords the text occupies
            // [baselineScreen - optionHeight, baselineScreen] (smaller Y = higher on screen).
            float baselineScreen = screenHeight - (optionStartY - i * optionSpacing);
            float optionTopScreen = baselineScreen - optionHeight;
            float optionBottomScreen = baselineScreen;

            if (screenX >= centerX - optionHalfWidth && screenX <= centerX + optionHalfWidth
                && screenY >= optionTopScreen && screenY <= optionBottomScreen) {
                selectedOption = i;
                return i;
            }
        }
        return -1;
    }
}

package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Main menu screen with "New Game" and "Quit" options.
 */
public class MainMenuScreen {
    private boolean visible;
    private int selectedOption;
    private static final int OPTION_NEW_GAME = 0;
    private static final int OPTION_QUIT = 1;
    private static final int NUM_OPTIONS = 2;

    private static final String[] OPTIONS = {
        "New Game",
        "Quit"
    };

    public MainMenuScreen() {
        this.visible = true;
        this.selectedOption = OPTION_NEW_GAME;
    }

    /**
     * Render the main menu.
     */
    public void render(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                       int screenWidth, int screenHeight) {
        if (!visible) {
            return;
        }

        // Draw background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 1.0f);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();

        // Draw title
        spriteBatch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(3.0f);
        String title = "RAGAMUFFIN";
        float titleWidth = font.getRegion().getRegionWidth() * 3.0f * title.length() / 4;
        font.draw(spriteBatch, title, screenWidth / 2f - titleWidth / 2, screenHeight * 0.7f);
        font.getData().setScale(1.2f);

        // Subtitle
        font.getData().setScale(1.5f);
        String subtitle = "Survival in Modern Britain";
        float subtitleWidth = font.getRegion().getRegionWidth() * 1.5f * subtitle.length() / 4;
        font.draw(spriteBatch, subtitle, screenWidth / 2f - subtitleWidth / 2, screenHeight * 0.6f);
        font.getData().setScale(1.2f);

        // Draw options
        float optionY = screenHeight * 0.4f;
        float optionSpacing = 60f;

        for (int i = 0; i < OPTIONS.length; i++) {
            String option = OPTIONS[i];
            if (i == selectedOption) {
                font.setColor(Color.YELLOW);
                option = "> " + option + " <";
            } else {
                font.setColor(Color.WHITE);
            }

            font.getData().setScale(1.5f);
            float optionWidth = font.getRegion().getRegionWidth() * 1.5f * option.length() / 4;
            font.draw(spriteBatch, option, screenWidth / 2f - optionWidth / 2, optionY - i * optionSpacing);
            font.getData().setScale(1.2f);
        }

        spriteBatch.end();
    }

    public boolean isVisible() {
        return visible;
    }

    public void show() {
        visible = true;
        selectedOption = OPTION_NEW_GAME;
    }

    public void hide() {
        visible = false;
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

    public boolean isNewGameSelected() {
        return selectedOption == OPTION_NEW_GAME;
    }

    public boolean isQuitSelected() {
        return selectedOption == OPTION_QUIT;
    }
}

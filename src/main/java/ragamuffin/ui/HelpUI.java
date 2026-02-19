package ragamuffin.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Displays help/controls UI overlay.
 */
public class HelpUI {
    private boolean visible;

    private static final String HELP_TEXT =
            "=== RAGAMUFFIN CONTROLS ===\n\n" +
            "WASD - Move player\n" +
            "Mouse - Look around\n" +
            "Left click / Punch - Break block (5 hits)\n" +
            "Right click - Place block\n" +
            "Left Ctrl - Dodge roll (while moving)\n" +
            "Space - Jump\n" +
            "Left Shift - Sprint\n" +
            "I - Open/close Inventory\n" +
            "H - Open/close Help\n" +
            "C - Open/close Crafting menu\n" +
            "E - Interact with objects/NPCs\n" +
            "1-9 - Select hotbar slot\n" +
            "ESC - Pause menu\n\n" +
            "Press H to close this help screen.";

    public HelpUI() {
        this.visible = false;
    }

    /**
     * Toggle visibility of the help UI.
     */
    public void toggle() {
        visible = !visible;
    }

    /**
     * Show the help UI.
     */
    public void show() {
        visible = true;
    }

    /**
     * Hide the help UI.
     */
    public void hide() {
        visible = false;
    }

    /**
     * Check if the help UI is currently visible.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Get the help text.
     */
    public String getHelpText() {
        return HELP_TEXT;
    }

    /**
     * Render the help UI.
     */
    public void render(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font, int screenWidth, int screenHeight) {
        if (!visible) {
            return;
        }

        // Render semi-transparent background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 0.85f);
        shapeRenderer.rect(screenWidth / 4, screenHeight / 4, screenWidth / 2, screenHeight / 2);
        shapeRenderer.end();

        // Render help text
        batch.begin();
        int x = screenWidth / 4 + 20;
        int y = screenHeight * 3 / 4 - 20;

        String[] lines = HELP_TEXT.split("\n");
        for (String line : lines) {
            font.draw(batch, line, x, y);
            y -= 20; // Line spacing
        }
        batch.end();
    }
}

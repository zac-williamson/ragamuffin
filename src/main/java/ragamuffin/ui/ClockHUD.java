package ragamuffin.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Displays the current in-game time and FPS on the HUD.
 */
public class ClockHUD {

    private String timeString;
    private int fps;
    private int dayCount;
    private String dateString;

    public ClockHUD() {
        this.timeString = "08:00";
        this.fps = 0;
        this.dayCount = 1;
        this.dateString = "1 June";
    }

    /**
     * Update the clock with current game time.
     * @param time Current time in hours (0-24)
     */
    public void update(float time) {
        int hours = (int) time;
        int minutes = (int) ((time - hours) * 60);
        this.timeString = String.format("%02d:%02d", hours, minutes);
        // Only update FPS if Gdx is available (not in headless tests)
        if (Gdx.graphics != null) {
            this.fps = Gdx.graphics.getFramesPerSecond();
        }
    }

    /**
     * Update the clock with current game time, day count, and date info.
     */
    public void update(float time, int dayCount) {
        update(time);
        this.dayCount = dayCount;
    }

    /**
     * Update with full seasonal info.
     */
    public void update(float time, int dayCount, int dayOfMonth, String monthName) {
        update(time, dayCount);
        this.dateString = dayOfMonth + " " + monthName;
    }

    /**
     * Get the current time string.
     */
    public String getTimeString() {
        return timeString;
    }

    /**
     * Get the current FPS.
     */
    public int getFps() {
        return fps;
    }

    /**
     * Render the clock on screen with a subtle background panel for readability.
     */
    public void render(SpriteBatch batch, BitmapFont font, int screenWidth, int screenHeight) {
        render(batch, null, font, screenWidth, screenHeight);
    }

    /**
     * Render the clock on screen. If shapeRenderer is provided, draws a
     * semi-transparent backing panel behind the text for modern HUD readability.
     */
    public void render(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font,
                       int screenWidth, int screenHeight) {
        float x = screenWidth - 130;
        float y = screenHeight - 45;

        // Background pill for readability (rendered only when shapeRenderer is available)
        if (shapeRenderer != null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, 0.45f);
            shapeRenderer.rect(x - 4, y - 60, 126, 70);
            shapeRenderer.end();
        }

        batch.begin();
        font.getData().setScale(0.9f);
        font.setColor(Color.WHITE);

        // Draw date and day counter in top-right corner, below the weather text
        font.draw(batch, dateString + " (Day " + dayCount + ")", x, y);

        // Draw time in slightly larger text below date
        font.getData().setScale(1.1f);
        font.setColor(0.95f, 0.95f, 0.7f, 1f); // Warm white for clock
        font.draw(batch, timeString, x, y - 20);

        // Draw FPS below the time in smaller grey text
        font.getData().setScale(0.75f);
        font.setColor(0.5f, 0.5f, 0.5f, 1f);
        font.draw(batch, "FPS: " + fps, x, y - 40);

        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);
        batch.end();
    }
}

package ragamuffin.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Displays the current in-game time and FPS on the HUD.
 */
public class ClockHUD {

    private String timeString;
    private int fps;

    public ClockHUD() {
        this.timeString = "08:00";
        this.fps = 0;
    }

    /**
     * Update the clock with current game time.
     * @param time Current time in hours (0-24)
     */
    public void update(float time) {
        int hours = (int) time;
        int minutes = (int) ((time - hours) * 60);
        this.timeString = String.format("%02d:%02d", hours, minutes);
        this.fps = Gdx.graphics.getFramesPerSecond();
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
     * Render the clock on screen.
     */
    public void render(SpriteBatch batch, BitmapFont font, int screenWidth, int screenHeight) {
        batch.begin();

        // Draw time in top-right corner, below the weather text
        float x = screenWidth - 100;
        float y = screenHeight - 45;
        font.draw(batch, timeString, x, y);

        // Draw FPS below the time
        font.draw(batch, "FPS: " + fps, x, y - 25);

        batch.end();
    }
}

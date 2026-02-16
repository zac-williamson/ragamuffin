package ragamuffin.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Displays the current in-game time on the HUD.
 */
public class ClockHUD {

    private String timeString;

    public ClockHUD() {
        this.timeString = "08:00";
    }

    /**
     * Update the clock with current game time.
     * @param time Current time in hours (0-24)
     */
    public void update(float time) {
        int hours = (int) time;
        int minutes = (int) ((time - hours) * 60);
        this.timeString = String.format("%02d:%02d", hours, minutes);
    }

    /**
     * Get the current time string.
     */
    public String getTimeString() {
        return timeString;
    }

    /**
     * Render the clock on screen.
     */
    public void render(SpriteBatch batch, BitmapFont font, int screenWidth, int screenHeight) {
        batch.begin();

        // Draw time in top-right corner
        float x = screenWidth - 100;
        float y = screenHeight - 20;

        font.draw(batch, timeString, x, y);

        batch.end();
    }
}

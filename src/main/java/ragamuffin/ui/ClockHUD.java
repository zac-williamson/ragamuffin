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
     * Render the clock on screen.
     */
    public void render(SpriteBatch batch, BitmapFont font, int screenWidth, int screenHeight) {
        batch.begin();

        // Draw date and day counter in top-right corner, below the weather text
        float x = screenWidth - 130;
        float y = screenHeight - 45;
        font.draw(batch, dateString + " (Day " + dayCount + ")", x, y);

        // Draw time below date
        font.draw(batch, timeString, x, y - 20);

        // Draw FPS below the time
        font.draw(batch, "FPS: " + fps, x, y - 40);

        batch.end();
    }
}

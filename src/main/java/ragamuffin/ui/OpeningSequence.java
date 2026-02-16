package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Opening sequence displayed when a new game starts.
 * Shows "it's time to learn to survive on your own" for 3 seconds.
 */
public class OpeningSequence {
    private static final float DURATION = 3.0f; // 3 seconds = 180 frames at 60fps
    private static final String MESSAGE = "It's time to learn to survive on your own";

    private boolean active;
    private float timer;
    private boolean completed;

    public OpeningSequence() {
        this.active = false;
        this.timer = 0;
        this.completed = false;
    }

    /**
     * Start the opening sequence.
     */
    public void start() {
        active = true;
        timer = 0;
        completed = false;
    }

    /**
     * Update the sequence timer.
     */
    public void update(float delta) {
        if (!active) {
            return;
        }

        timer += delta;
        if (timer >= DURATION) {
            active = false;
            completed = true;
        }
    }

    /**
     * Render the opening sequence.
     */
    public void render(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                       int screenWidth, int screenHeight) {
        if (!active) {
            return;
        }

        // Draw semi-transparent background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.8f);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();

        // Calculate fade effect
        float alpha = 1.0f;
        if (timer < 0.5f) {
            // Fade in
            alpha = timer / 0.5f;
        } else if (timer > DURATION - 0.5f) {
            // Fade out
            alpha = (DURATION - timer) / 0.5f;
        }

        // Draw message
        spriteBatch.begin();
        font.setColor(1, 1, 1, alpha);
        font.getData().setScale(2.0f);
        float messageWidth = font.getRegion().getRegionWidth() * 2.0f * MESSAGE.length() / 4;
        font.draw(spriteBatch, MESSAGE, screenWidth / 2f - messageWidth / 2, screenHeight / 2f);
        font.getData().setScale(1.2f);
        font.setColor(Color.WHITE);
        spriteBatch.end();
    }

    public boolean isActive() {
        return active;
    }

    public boolean isCompleted() {
        return completed;
    }

    public float getTimer() {
        return timer;
    }

    /**
     * Skip the opening sequence.
     */
    public void skip() {
        active = false;
        completed = true;
    }
}

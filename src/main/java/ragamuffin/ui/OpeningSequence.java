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
    private static final float DURATION = 12.0f;
    private static final String[] LINES = {
        "\"You're eighteen now. Time to stand on your own two feet.\"",
        "Your parents lock the door behind you.",
        "No money. No phone. No plan.",
        "Just you and the streets of a town that doesn't care."
    };
    private static final float[] LINE_TIMES = {0.0f, 3.5f, 6.5f, 9.0f};

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

        // Draw black background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.9f);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();

        // Draw lines sequentially with fade-in
        spriteBatch.begin();
        font.getData().setScale(1.6f);

        float yOffset = screenHeight / 2f + 60;

        for (int i = 0; i < LINES.length; i++) {
            if (timer < LINE_TIMES[i]) continue;

            float lineAge = timer - LINE_TIMES[i];
            float alpha = Math.min(1.0f, lineAge / 0.8f); // Fade in over 0.8s

            // Fade out everything in the last second
            if (timer > DURATION - 1.0f) {
                alpha *= (DURATION - timer);
            }

            // First line in italic yellow (parent's voice), rest in white
            if (i == 0) {
                font.setColor(0.9f, 0.8f, 0.4f, alpha);
            } else {
                font.setColor(0.8f, 0.8f, 0.8f, alpha);
            }

            float textWidth = LINES[i].length() * 8f; // Approximate
            font.draw(spriteBatch, LINES[i], screenWidth / 2f - textWidth / 2f, yOffset - i * 40);
        }

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

package ragamuffin.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Renders a first-person block-style arm at the bottom-right of the screen.
 * When punching, the arm swings forward with a simple animation.
 */
public class FirstPersonArm {

    private static final Color SKIN_COLOR = new Color(0.85f, 0.7f, 0.55f, 1f);
    private static final Color SLEEVE_COLOR = new Color(0.3f, 0.3f, 0.7f, 1f);
    private static final float SWING_DURATION = 0.25f; // seconds for full punch swing

    private float swingTimer = 0f;
    private boolean swinging = false;

    /**
     * Trigger a punch swing animation.
     */
    public void punch() {
        swinging = true;
        swingTimer = SWING_DURATION;
    }

    /**
     * Update the swing animation.
     */
    public void update(float delta) {
        if (swinging) {
            swingTimer -= delta;
            if (swingTimer <= 0) {
                swinging = false;
                swingTimer = 0;
            }
        }
    }

    /**
     * Returns true if currently in a punch animation.
     */
    public boolean isSwinging() {
        return swinging;
    }

    /**
     * Get the current swing progress (0 = resting, 1 = fully extended).
     */
    public float getSwingProgress() {
        if (!swinging) return 0f;
        float halfDuration = SWING_DURATION / 2f;
        float elapsed = SWING_DURATION - swingTimer;
        // Swing out then back: triangle wave
        if (elapsed < halfDuration) {
            return elapsed / halfDuration; // 0 -> 1
        } else {
            return 1f - (elapsed - halfDuration) / halfDuration; // 1 -> 0
        }
    }

    /**
     * Render the arm using ShapeRenderer in screen space.
     * Call this after the 3D world render but before/during 2D UI overlay.
     */
    public void render(ShapeRenderer shapeRenderer, int screenWidth, int screenHeight) {
        float swingProgress = getSwingProgress();

        // Base position: bottom-right of screen
        float baseX = screenWidth * 0.7f;
        float baseY = screenHeight * 0.05f;

        // Arm dimensions (blocky, Minecraft-style)
        float armWidth = screenWidth * 0.08f;
        float armHeight = screenHeight * 0.35f;

        // Swing offset: arm moves up and left when punching
        float swingOffsetX = -swingProgress * screenWidth * 0.15f;
        float swingOffsetY = swingProgress * screenHeight * 0.2f;

        float armX = baseX + swingOffsetX;
        float armY = baseY + swingOffsetY;

        // Slight rotation effect via skewing the forearm position
        float forearmOffsetX = swingProgress * screenWidth * 0.05f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Upper arm (sleeve) - wider block at bottom
        shapeRenderer.setColor(SLEEVE_COLOR);
        shapeRenderer.rect(armX, armY, armWidth * 1.2f, armHeight * 0.4f);

        // Forearm (skin) - main arm block
        shapeRenderer.setColor(SKIN_COLOR);
        shapeRenderer.rect(armX + forearmOffsetX, armY + armHeight * 0.4f,
                          armWidth, armHeight * 0.4f);

        // Hand/fist (skin, slightly wider) - top block
        shapeRenderer.setColor(SKIN_COLOR.cpy().mul(0.9f));
        shapeRenderer.rect(armX + forearmOffsetX - armWidth * 0.1f,
                          armY + armHeight * 0.8f,
                          armWidth * 1.2f, armHeight * 0.2f);

        shapeRenderer.end();
    }
}

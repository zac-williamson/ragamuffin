package ragamuffin.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Renders a first-person block-style arm at the bottom-right of the screen.
 * When punching, the arm swings forward with a simple animation.
 * The arm features idle bob animation and improved visual detail.
 */
public class FirstPersonArm {

    private static final Color SKIN_COLOR        = new Color(0.85f, 0.70f, 0.55f, 1f);
    private static final Color SKIN_SHADOW_COLOR = new Color(0.65f, 0.52f, 0.38f, 1f);
    private static final Color SLEEVE_COLOR      = new Color(0.30f, 0.30f, 0.70f, 1f);
    private static final Color SLEEVE_DARK_COLOR = new Color(0.20f, 0.20f, 0.50f, 1f);
    private static final Color KNUCKLE_COLOR     = new Color(0.72f, 0.57f, 0.42f, 1f);
    private static final Color OUTLINE_COLOR     = new Color(0.10f, 0.07f, 0.05f, 0.55f);

    private static final float SWING_DURATION = 0.25f; // seconds for full punch swing
    private static final float BOB_SPEED      = 3.0f;  // idle bob frequency (radians/sec)
    private static final float BOB_AMPLITUDE  = 0.012f; // fraction of screen height

    private float swingTimer  = 0f;
    private boolean swinging  = false;
    private float idleTimer   = 0f;

    /**
     * Trigger a punch swing animation.
     */
    public void punch() {
        swinging   = true;
        swingTimer = SWING_DURATION;
    }

    /**
     * Update the swing and idle bob animations.
     */
    public void update(float delta) {
        if (swinging) {
            swingTimer -= delta;
            if (swingTimer <= 0) {
                swinging   = false;
                swingTimer = 0;
            }
        }
        idleTimer += delta;
    }

    /**
     * Returns true if currently in a punch animation.
     */
    public boolean isSwinging() {
        return swinging;
    }

    /**
     * Get the current swing progress (0 = resting, 1 = fully extended).
     * Uses a smooth ease-in/ease-out curve for more natural motion.
     */
    public float getSwingProgress() {
        if (!swinging) return 0f;
        float halfDuration = SWING_DURATION / 2f;
        float elapsed = SWING_DURATION - swingTimer;
        float t;
        if (elapsed < halfDuration) {
            t = elapsed / halfDuration; // 0 -> 1
        } else {
            t = 1f - (elapsed - halfDuration) / halfDuration; // 1 -> 0
        }
        // Smooth step: t*t*(3-2t)
        return t * t * (3f - 2f * t);
    }

    /**
     * Render the arm using ShapeRenderer in screen space.
     * Call this after the 3D world render but before/during 2D UI overlay.
     */
    public void render(ShapeRenderer shapeRenderer, int screenWidth, int screenHeight) {
        float swingProgress = getSwingProgress();

        // Idle bob (suppressed while swinging for cleaner punch feel)
        float bob = swinging ? 0f : (float) Math.sin(idleTimer * BOB_SPEED) * BOB_AMPLITUDE * screenHeight;

        // Base position: bottom-right of screen, anchored so arm emerges from corner
        float baseX = screenWidth  * 0.68f;
        float baseY = screenHeight * 0.03f + bob;

        // Arm dimensions
        float armWidth  = screenWidth  * 0.09f;
        float armHeight = screenHeight * 0.38f;

        // Swing offset: arm punches forward (up + slightly left) then retracts
        float swingOffsetX = -swingProgress * screenWidth  * 0.13f;
        float swingOffsetY =  swingProgress * screenHeight * 0.22f;

        float armX = baseX + swingOffsetX;
        float armY = baseY + swingOffsetY;

        // Forearm lean: tips slightly forward during the punch
        float leanX = swingProgress * screenWidth * 0.04f;

        // --- Segment heights ---
        float sleeveH   = armHeight * 0.38f;
        float forearmH  = armHeight * 0.38f;
        float handH     = armHeight * 0.24f;

        // Segment x positions (forearm & hand lean during punch)
        float sleeveX   = armX;
        float forearmX  = armX + leanX;
        float handX     = armX + leanX - armWidth * 0.05f;

        float sleeveY   = armY;
        float forearmY  = sleeveY  + sleeveH;
        float handY     = forearmY + forearmH;

        float sleeveW   = armWidth * 1.25f;
        float forearmW  = armWidth;
        float handW     = armWidth * 1.15f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // --- Shadow/outline pass (draw slightly larger shapes in near-black first) ---
        float pad = screenWidth * 0.004f;
        shapeRenderer.setColor(OUTLINE_COLOR);
        shapeRenderer.rect(sleeveX  - pad, sleeveY  - pad, sleeveW  + pad * 2, sleeveH  + pad * 2);
        shapeRenderer.rect(forearmX - pad, forearmY - pad, forearmW + pad * 2, forearmH + pad * 2);
        shapeRenderer.rect(handX    - pad, handY    - pad, handW    + pad * 2, handH    + pad * 2);

        // --- Sleeve (upper arm with fabric) ---
        shapeRenderer.setColor(SLEEVE_COLOR);
        shapeRenderer.rect(sleeveX, sleeveY, sleeveW, sleeveH);

        // Right-side shadow strip on sleeve for depth
        shapeRenderer.setColor(SLEEVE_DARK_COLOR);
        shapeRenderer.rect(sleeveX + sleeveW * 0.72f, sleeveY, sleeveW * 0.28f, sleeveH);

        // --- Forearm (skin) ---
        shapeRenderer.setColor(SKIN_COLOR);
        shapeRenderer.rect(forearmX, forearmY, forearmW, forearmH);

        // Right-side shadow strip on forearm
        shapeRenderer.setColor(SKIN_SHADOW_COLOR);
        shapeRenderer.rect(forearmX + forearmW * 0.70f, forearmY, forearmW * 0.30f, forearmH);

        // --- Hand / fist ---
        shapeRenderer.setColor(SKIN_COLOR);
        shapeRenderer.rect(handX, handY, handW, handH);

        // Right-side shadow strip on hand
        shapeRenderer.setColor(SKIN_SHADOW_COLOR);
        shapeRenderer.rect(handX + handW * 0.70f, handY, handW * 0.30f, handH);

        // Knuckle detail: three thin horizontal lines across the top third of the fist
        shapeRenderer.setColor(KNUCKLE_COLOR);
        float knuckleH = handH * 0.06f;
        float knuckleY1 = handY + handH * 0.62f;
        float knuckleY2 = handY + handH * 0.74f;
        float knuckleY3 = handY + handH * 0.86f;
        shapeRenderer.rect(handX + handW * 0.08f, knuckleY1, handW * 0.84f, knuckleH);
        shapeRenderer.rect(handX + handW * 0.08f, knuckleY2, handW * 0.84f, knuckleH);
        shapeRenderer.rect(handX + handW * 0.08f, knuckleY3, handW * 0.84f, knuckleH);

        // Sleeve cuff line (border between sleeve and forearm)
        shapeRenderer.setColor(SLEEVE_DARK_COLOR);
        float cuffH = sleeveH * 0.10f;
        shapeRenderer.rect(sleeveX, sleeveY + sleeveH - cuffH, sleeveW, cuffH);

        shapeRenderer.end();
    }
}

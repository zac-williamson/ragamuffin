package ragamuffin.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.building.Material;

/**
 * Renders a first-person block-style arm at the bottom-right of the screen.
 * When punching, the arm swings forward with a simple animation.
 * The arm features idle bob animation and improved visual detail.
 * When the player holds an item it is drawn above the fist.
 */
public class FirstPersonArm {

    private static final Color SKIN_COLOR        = new Color(0.85f, 0.70f, 0.55f, 1f);
    private static final Color SKIN_SHADOW_COLOR = new Color(0.65f, 0.52f, 0.38f, 1f);
    private static final Color SLEEVE_COLOR      = new Color(0.30f, 0.30f, 0.70f, 1f);
    private static final Color SLEEVE_DARK_COLOR = new Color(0.20f, 0.20f, 0.50f, 1f);
    private static final Color KNUCKLE_COLOR     = new Color(0.72f, 0.57f, 0.42f, 1f);
    private static final Color OUTLINE_COLOR     = new Color(0.10f, 0.07f, 0.05f, 0.55f);

    /** The item currently held in this hand, or null for empty hand. */
    private Material heldItem = null;

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
     * Returns the current idle bob timer value (seconds elapsed since construction
     * or last reset). Used by tests to verify the timer advances during all game
     * states (PLAYING, PAUSED, CINEMATIC).
     */
    public float getIdleTimer() {
        return idleTimer;
    }

    /**
     * Set the item currently held in the player's hand.
     * Pass null to show an empty hand.
     */
    public void setHeldItem(Material item) {
        this.heldItem = item;
    }

    /**
     * Returns the item currently held, or null if empty-handed.
     */
    public Material getHeldItem() {
        return heldItem;
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

        // Draw held item above the fist
        if (heldItem != null) {
            float itemSize = armWidth * 1.4f;
            float itemX = handX + (handW - itemSize) / 2f;
            float itemY = handY + handH + screenHeight * 0.01f;
            drawHeldItemIcon(shapeRenderer, heldItem, itemX, itemY, itemSize);
        }

        shapeRenderer.end();
    }

    /**
     * Draw a small item icon above the fist representing the held item.
     * Block items render as an isometric voxel cube; non-block items render
     * using a simplified shape based on the material's icon shape.
     */
    private void drawHeldItemIcon(ShapeRenderer shapeRenderer, Material material, float x, float y, float size) {
        Color[] colors = material.getIconColors();
        if (material.isBlockItem()) {
            drawIsometricBlock(shapeRenderer, colors, x, y, size);
        } else {
            drawSimpleItemShape(shapeRenderer, material, x, y, size, colors);
        }
    }

    /**
     * Draw a 3D isometric voxel cube for a block-type held item.
     */
    private void drawIsometricBlock(ShapeRenderer shapeRenderer, Color[] colors, float x, float y, float size) {
        Color topColor   = colors[0];
        Color leftColor  = colors.length > 1 ? colors[1] : darken(colors[0], 0.80f);
        Color rightColor = darken(leftColor, 0.70f);

        float cx    = x + size / 2f;
        float xLeft = x;
        float xRight= x + size;
        float yTop  = y + size;
        float yMid  = y + size * 0.40f;
        float yBot  = y;

        float ax = cx,      ay = yTop;
        float bx = xLeft,   by = yMid;
        float c2x= xRight,  c2y= yMid;
        float dx = xLeft,   dy = yBot;
        float ex = cx,      ey = yBot;
        float fx = xRight,  fy = yBot;

        // Top face (rhombus)
        shapeRenderer.setColor(topColor);
        shapeRenderer.triangle(ax, ay, bx, by, c2x, c2y);
        shapeRenderer.triangle(bx, by, ex, ey, c2x, c2y);

        // Left face
        shapeRenderer.setColor(leftColor);
        shapeRenderer.triangle(bx, by, dx, dy, ex, ey);

        // Right face (shadow side)
        shapeRenderer.setColor(rightColor);
        shapeRenderer.triangle(c2x, c2y, ex, ey, fx, fy);
    }

    /**
     * Draw a simple shape for non-block items (tool, gem, bottle, flat paper, etc.).
     */
    private void drawSimpleItemShape(ShapeRenderer shapeRenderer, Material material, float x, float y, float size, Color[] colors) {
        Color primary   = colors[0];
        Color secondary = colors.length > 1 ? colors[1] : primary;
        float cx = x + size / 2f;
        float cy = y + size / 2f;

        switch (material.getIconShape()) {
            case TOOL: {
                float handleW = size / 5f;
                float handleH = size * 0.65f;
                shapeRenderer.setColor(secondary);
                shapeRenderer.rect(x + size / 5f, y + size / 8f, handleW, handleH);
                float headSize = size / 3f;
                shapeRenderer.setColor(primary);
                shapeRenderer.rect(x + size / 2f, y + size / 2f, headSize, headSize);
                break;
            }
            case FLAT_PAPER: {
                float w = size * 0.75f;
                float h = size * 0.80f;
                float px = x + (size - w) / 2f;
                float py = y + (size - h) / 2f;
                shapeRenderer.setColor(primary);
                shapeRenderer.rect(px, py, w, h);
                shapeRenderer.setColor(secondary);
                shapeRenderer.rect(px + 2, py + h - h / 5f, w - 4, h / 5f - 1);
                break;
            }
            case BOTTLE: {
                float bodyW = size / 3f;
                float bodyH = size * 0.70f;
                float bx2 = cx - bodyW / 2f;
                float by2 = y + size / 8f;
                shapeRenderer.setColor(primary);
                shapeRenderer.rect(bx2, by2, bodyW, bodyH);
                float capW = bodyW - 4f;
                shapeRenderer.setColor(secondary);
                shapeRenderer.rect(bx2 + 2f, by2 + bodyH, capW, size / 8f);
                break;
            }
            case FOOD: {
                float foodW = size * 0.80f;
                float foodH = size * 0.50f;
                float fx2 = x + (size - foodW) / 2f;
                float fy2 = cy - foodH / 2f + size / 10f;
                shapeRenderer.setColor(primary);
                shapeRenderer.rect(fx2, fy2, foodW, foodH);
                shapeRenderer.setColor(secondary);
                shapeRenderer.rect(fx2 + 2f, y + size / 8f, foodW - 4f, size / 8f);
                break;
            }
            case CARD: {
                float cardW = size * 0.80f;
                float cardH = size * 0.55f;
                float kx = x + (size - cardW) / 2f;
                float ky = cy - cardH / 2f;
                shapeRenderer.setColor(primary);
                shapeRenderer.rect(kx, ky, cardW, cardH);
                shapeRenderer.setColor(secondary);
                shapeRenderer.rect(kx + 3f, ky + 3f, cardW - 6f, cardH - 6f);
                break;
            }
            case GEM: {
                shapeRenderer.setColor(primary);
                shapeRenderer.triangle(cx, y + size - 2f, x + 2f, cy, x + size - 2f, cy);
                shapeRenderer.setColor(secondary);
                shapeRenderer.triangle(x + 2f, cy, x + size - 2f, cy, cx, y + 2f);
                break;
            }
            case BOX: {
                float boxSize = size * 0.70f;
                float bx2 = x + (size - boxSize) / 2f;
                float by2 = y + size / 10f;
                shapeRenderer.setColor(primary);
                shapeRenderer.rect(bx2, by2, boxSize, boxSize);
                float topH = size / 6f;
                shapeRenderer.setColor(secondary);
                shapeRenderer.rect(bx2, by2 + boxSize, boxSize, topH);
                break;
            }
            case CYLINDER: {
                float cylW = size / 3f;
                float cylH = size * 0.72f;
                float cx2 = x + (size - cylW) / 2f;
                float cy2 = y + size / 10f;
                shapeRenderer.setColor(primary);
                shapeRenderer.rect(cx2, cy2, cylW, cylH);
                shapeRenderer.setColor(secondary);
                shapeRenderer.rect(cx2 - 2f, cy2 + cylH - size / 10f, cylW + 4f, size / 8f);
                shapeRenderer.rect(cx2 + cylW, cy2 + cylH / 2f, size / 6f, size / 8f);
                break;
            }
            default: {
                shapeRenderer.setColor(primary);
                shapeRenderer.rect(x, y, size, size);
                break;
            }
        }
    }

    /** Return a darker version of the given color (factor in 0..1). */
    private static Color darken(Color c, float factor) {
        return new Color(c.r * factor, c.g * factor, c.b * factor, c.a);
    }
}

package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.building.Inventory;
import ragamuffin.core.JobCentreRecord;
import ragamuffin.core.JobCentreRecord.JobSearchMissionType;
import ragamuffin.core.JobCentreSystem;
import ragamuffin.core.JobCentreSystem.SignOnResult;
import ragamuffin.entity.Player;

/**
 * Issue #795: JobCentre UI overlay (GameState.JOB_CENTRE_OPEN).
 *
 * <p>Renders the sign-on interaction panel: case worker dialogue, job search
 * mission description, sanction level, current UC payment, and controls hint.
 *
 * <p>Input is handled externally (by the game loop / InputHandler). This class
 * is purely for rendering.
 */
public class JobCentreUI {

    private static final float PANEL_WIDTH  = 380f;
    private static final float PANEL_HEIGHT = 320f;
    private static final float PADDING      = 14f;

    private static final float TITLE_SCALE   = 1.0f;
    private static final float SECTION_SCALE = 0.85f;
    private static final float ITEM_SCALE    = 0.78f;
    private static final float HINT_SCALE    = 0.70f;

    private boolean visible;

    /** The last sign-on result, used to display appropriate dialogue. */
    private SignOnResult lastResult = null;

    /** Whether the player is in the "suspicious" lie-or-admit sub-dialogue. */
    private boolean awaitingLieOrAdmit = false;

    public JobCentreUI() {
        this.visible = false;
    }

    public boolean isVisible() { return visible; }

    public void show() { visible = true; }

    public void hide() {
        visible = false;
        awaitingLieOrAdmit = false;
    }

    public void toggle() {
        if (visible) hide(); else show();
    }

    /**
     * Notify the UI of a sign-on result so it can display appropriate dialogue.
     *
     * @param result the sign-on result from {@link JobCentreSystem#trySignOn}
     */
    public void setLastSignOnResult(SignOnResult result) {
        this.lastResult = result;
        this.awaitingLieOrAdmit = (result == SignOnResult.SUSPICIOUS);
    }

    /** @return true if the player must choose LIE or ADMIT IT */
    public boolean isAwaitingLieOrAdmit() { return awaitingLieOrAdmit; }

    /** Clear the lie-or-admit state after the player makes their choice. */
    public void clearLieOrAdmit() { awaitingLieOrAdmit = false; }

    /**
     * Render the JobCentre UI panel.
     *
     * @param batch         sprite batch (orthographic projection already set)
     * @param shape         shape renderer
     * @param font          bitmap font
     * @param screenWidth   screen width in pixels
     * @param screenHeight  screen height in pixels
     * @param jobCentreSystem the job centre system state
     * @param player        the player
     * @param inventory     the player's inventory
     */
    public void render(SpriteBatch batch, ShapeRenderer shape, BitmapFont font,
                       int screenWidth, int screenHeight,
                       JobCentreSystem jobCentreSystem, Player player, Inventory inventory) {
        if (!visible) return;
        if (jobCentreSystem == null) return;

        float panelX = (screenWidth  - PANEL_WIDTH)  / 2f;
        float panelY = (screenHeight - PANEL_HEIGHT) / 2f;

        // ── Background ────────────────────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.05f, 0.10f, 0.20f, 0.92f);
        shape.rect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        // DWP grey-blue accent bar at the top
        shape.setColor(0.25f, 0.35f, 0.60f, 1.0f);
        shape.rect(panelX, panelY + PANEL_HEIGHT - 28f, PANEL_WIDTH, 28f);
        shape.end();

        // ── Border ────────────────────────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.40f, 0.55f, 0.85f, 1.0f);
        shape.rect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        shape.end();

        // ── Text ──────────────────────────────────────────────────────────────
        batch.begin();

        GlyphLayout layout = new GlyphLayout();
        float textX = panelX + PADDING;
        float y     = panelY + PANEL_HEIGHT - PADDING - 4f;

        // Title
        font.getData().setScale(TITLE_SCALE);
        font.setColor(Color.WHITE);
        font.draw(batch, "JOBCENTRE PLUS", textX, y);
        y -= 32f;

        // UC payment & sanction level
        JobCentreRecord record = jobCentreSystem.getRecord();
        font.getData().setScale(SECTION_SCALE);
        font.setColor(new Color(0.85f, 0.85f, 0.40f, 1f));
        String sanctionText = "Sanction Level: " + record.getSanctionLevel() + "/3";
        font.draw(batch, sanctionText, textX, y);
        y -= 20f;

        int ucPayment = jobCentreSystem.computeUCPayment();
        font.setColor(new Color(0.55f, 0.90f, 0.55f, 1f));
        String paymentText = "UC Payment: " + ucPayment + " COIN";
        font.draw(batch, paymentText, textX, y);
        y -= 24f;

        // Sign-on window status
        font.getData().setScale(ITEM_SCALE);
        font.setColor(Color.LIGHT_GRAY);
        if (jobCentreSystem.isSignOnWindowOpen()) {
            font.setColor(new Color(0.55f, 0.90f, 0.55f, 1f));
            font.draw(batch, "Sign-on window: OPEN", textX, y);
        } else {
            font.setColor(new Color(0.80f, 0.40f, 0.40f, 1f));
            font.draw(batch, "Sign-on window: CLOSED", textX, y);
        }
        y -= 20f;

        // Current mission
        JobSearchMissionType mission = record.getCurrentMission();
        if (mission != null) {
            font.getData().setScale(SECTION_SCALE);
            font.setColor(new Color(0.70f, 0.85f, 1.0f, 1f));
            font.draw(batch, "Current Mission:", textX, y);
            y -= 18f;

            font.getData().setScale(ITEM_SCALE);
            font.setColor(Color.WHITE);
            // Word-wrap the mission description manually
            String desc = mission.getDescription();
            font.draw(batch, desc, textX, y, PANEL_WIDTH - PADDING * 2, -1, true);
            y -= 44f;
        } else {
            y -= 10f;
        }

        // Debt collector warning
        if (record.isDebtCollectorActive()) {
            font.getData().setScale(SECTION_SCALE);
            font.setColor(new Color(1.0f, 0.35f, 0.35f, 1f));
            font.draw(batch, "! DEBT COLLECTOR ACTIVE — You owe 10 COIN !", textX, y);
            y -= 22f;
        }

        // Claim closed
        if (record.isClaimClosed()) {
            font.getData().setScale(SECTION_SCALE);
            font.setColor(new Color(1.0f, 0.20f, 0.20f, 1f));
            font.draw(batch, "CLAIM PERMANENTLY CLOSED", textX, y);
            y -= 22f;
        }

        // Lie-or-admit prompt
        if (awaitingLieOrAdmit) {
            font.getData().setScale(SECTION_SCALE);
            font.setColor(new Color(1.0f, 0.85f, 0.30f, 1f));
            font.draw(batch, "Case worker is suspicious. [L] Lie  [A] Admit It", textX, y);
            y -= 22f;
        }

        // Controls hint
        font.getData().setScale(HINT_SCALE);
        font.setColor(new Color(0.60f, 0.60f, 0.60f, 1f));
        font.draw(batch, "[E] Sign On   [ESC] Leave", textX, panelY + PADDING);

        font.getData().setScale(1.0f);
        batch.end();
    }
}

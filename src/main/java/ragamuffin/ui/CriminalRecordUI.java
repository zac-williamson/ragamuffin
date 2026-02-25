package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.core.CriminalRecord;
import ragamuffin.core.CriminalRecord.CrimeType;

/**
 * Overlay screen showing the player's criminal record log — a breakdown of
 * every category of crime committed this session with running totals.
 *
 * <p>Toggled with the R key (Criminal Record).
 */
public class CriminalRecordUI {

    private static final float ROW_HEIGHT  = 36f;
    private static final float PADDING     = 16f;
    private static final float TITLE_SCALE = 1.4f;
    private static final float ROW_SCALE   = 1.0f;
    private static final float HINT_SCALE  = 0.85f;

    private final CriminalRecord criminalRecord;
    private boolean visible;

    public CriminalRecordUI(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
        this.visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
    }

    public void toggle() {
        if (visible) hide();
        else show();
    }

    /**
     * Render the criminal record overlay.  Must be called with an orthographic
     * projection already set on {@code spriteBatch} and {@code shapeRenderer}.
     */
    public void render(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer, BitmapFont font,
                       int screenWidth, int screenHeight) {
        if (!visible) return;

        CrimeType[] types = CrimeType.values();
        int rows = types.length;

        float panelW = Math.min(520f, screenWidth - 60f);
        float panelH = rows * ROW_HEIGHT + 90f;
        float panelX = (screenWidth - panelW) / 2f;
        float panelY = (screenHeight - panelH) / 2f;

        // Semi-transparent dark background panel
        com.badlogic.gdx.Gdx.gl.glEnable(GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.88f);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.end();
        com.badlogic.gdx.Gdx.gl.glDisable(GL20.GL_BLEND);

        // Panel border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.6f, 0.2f, 0.2f, 1f); // red-ish — criminal record vibes
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.end();

        spriteBatch.begin();

        // Title
        font.getData().setScale(TITLE_SCALE);
        font.setColor(new Color(1f, 0.35f, 0.35f, 1f));
        String title = "CRIMINAL RECORD";
        GlyphLayout titleLayout = new GlyphLayout(font, title);
        font.draw(spriteBatch, title,
                  panelX + (panelW - titleLayout.width) / 2f,
                  panelY + panelH - PADDING);

        // Total crimes subtitle
        font.getData().setScale(HINT_SCALE);
        font.setColor(0.7f, 0.7f, 0.7f, 1f);
        String totalStr = "Total crimes committed: " + criminalRecord.getTotalCrimes();
        GlyphLayout totalLayout = new GlyphLayout(font, totalStr);
        font.draw(spriteBatch, totalStr,
                  panelX + (panelW - totalLayout.width) / 2f,
                  panelY + panelH - PADDING - 22f);

        // Close hint
        font.getData().setScale(HINT_SCALE);
        font.setColor(0.5f, 0.5f, 0.5f, 1f);
        font.draw(spriteBatch, "R or ESC to close",
                  panelX + PADDING, panelY + PADDING * 1.6f);

        // Crime rows
        float rowAreaTop = panelY + panelH - 60f;

        for (int i = 0; i < rows; i++) {
            CrimeType type = types[i];
            int count = criminalRecord.getCount(type);

            float rowY = rowAreaTop - i * ROW_HEIGHT;

            // Crime name (left)
            font.getData().setScale(ROW_SCALE);
            font.setColor(count > 0
                    ? new Color(1f, 0.75f, 0.75f, 1f)  // pinkish when non-zero
                    : new Color(0.5f, 0.5f, 0.5f, 1f)); // grey when zero
            font.draw(spriteBatch, type.getDisplayName(), panelX + PADDING, rowY);

            // Count (right-aligned)
            String countStr = String.valueOf(count);
            GlyphLayout countLayout = new GlyphLayout(font, countStr);
            font.setColor(count > 0
                    ? new Color(1f, 0.4f, 0.4f, 1f)  // red when non-zero
                    : new Color(0.4f, 0.4f, 0.4f, 1f));
            font.draw(spriteBatch, countStr,
                      panelX + panelW - PADDING - countLayout.width,
                      rowY);
        }

        font.getData().setScale(1.0f); // restore default
        font.setColor(Color.WHITE);
        spriteBatch.end();
    }
}

package ragamuffin.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.PigeonRacingSystem;
import ragamuffin.core.PigeonRacingSystem.Morale;
import ragamuffin.core.PigeonRacingSystem.Pigeon;
import ragamuffin.core.PigeonRacingSystem.RaceResult;
import ragamuffin.core.PigeonRacingSystem.RaceType;

/**
 * Issue #987: PigeonLoftUI — 2D overlay panel for managing the player's racing pigeon.
 *
 * <p>Opened by pressing E on a placed {@code PIGEON_LOFT} prop. Sections:
 * <ol>
 *   <li>Header: "PIGEON LOFT" title bar.</li>
 *   <li>Pigeon Stats: name, training bar (0–10), morale (coloured text), wins/races.</li>
 *   <li>Train: "Feed Bread Crust" button (requires BREAD_CRUST in inventory).</li>
 *   <li>Race Entry: lists upcoming races with entry fee and enabled/disabled state.</li>
 *   <li>Results: last race placement and flavour line.</li>
 *   <li>Release: release the pigeon with a flavour tooltip.</li>
 * </ol>
 *
 * <p>Input handling (key selection, button activation) is done externally.
 * This class is purely for state and rendering.
 */
public class PigeonLoftUI {

    // ── Layout constants ──────────────────────────────────────────────────────

    private static final float PANEL_WIDTH  = 340f;
    private static final float PANEL_HEIGHT = 480f;
    private static final float PADDING      = 14f;
    private static final float ROW_HEIGHT   = 28f;
    private static final float SECTION_GAP  = 10f;

    // Font scales
    private static final float TITLE_SCALE   = 1.0f;
    private static final float SECTION_SCALE = 0.85f;
    private static final float BODY_SCALE    = 0.78f;
    private static final float HINT_SCALE    = 0.70f;

    // Colours
    private static final Color PANEL_BG      = new Color(0.08f, 0.08f, 0.10f, 0.90f);
    private static final Color PANEL_BORDER  = new Color(0.55f, 0.40f, 0.20f, 1.0f); // warm brown
    private static final Color TITLE_COLOUR  = new Color(0.90f, 0.75f, 0.40f, 1.0f); // gold
    private static final Color SECTION_COLOUR = new Color(0.70f, 0.70f, 0.70f, 1.0f);
    private static final Color BODY_COLOUR   = new Color(0.90f, 0.90f, 0.90f, 1.0f);
    private static final Color DISABLED_COLOUR = new Color(0.45f, 0.45f, 0.45f, 1.0f);
    private static final Color MORALE_GOOD   = new Color(0.30f, 0.90f, 0.30f, 1.0f);
    private static final Color MORALE_MID    = new Color(0.90f, 0.80f, 0.20f, 1.0f);
    private static final Color MORALE_BAD    = new Color(0.90f, 0.30f, 0.20f, 1.0f);
    private static final Color BAR_FILLED    = new Color(0.40f, 0.80f, 0.40f, 1.0f);
    private static final Color BAR_EMPTY     = new Color(0.25f, 0.25f, 0.25f, 1.0f);
    private static final Color HINT_COLOUR   = new Color(0.55f, 0.55f, 0.60f, 1.0f);

    // ── State ─────────────────────────────────────────────────────────────────

    private boolean visible;

    /** Currently selected button (0=Feed, 1=Sprint, 2=Club, 3=Derby, 4=Release). */
    private int selectedButton;

    // ── Constructor ───────────────────────────────────────────────────────────

    public PigeonLoftUI() {
        this.visible = false;
        this.selectedButton = 0;
    }

    // ── Visibility ────────────────────────────────────────────────────────────

    public boolean isVisible() { return visible; }
    public void show() { visible = true; selectedButton = 0; }
    public void hide() { visible = false; }
    public void toggle() { if (visible) hide(); else show(); }

    // ── Button navigation ─────────────────────────────────────────────────────

    public int getSelectedButton() { return selectedButton; }
    public void selectButton(int index) { this.selectedButton = Math.max(0, Math.min(4, index)); }
    public void nextButton() { selectedButton = Math.min(4, selectedButton + 1); }
    public void prevButton() { selectedButton = Math.max(0, selectedButton - 1); }

    // ── Action handling ───────────────────────────────────────────────────────

    /**
     * Handle the "Feed Bread Crust" button activation.
     *
     * @return true if a crust was fed
     */
    public boolean activateFeed(PigeonRacingSystem system, Inventory inventory,
                                 ragamuffin.core.NotorietySystem.AchievementCallback callback) {
        return system.feedBreadCrust(inventory, callback);
    }

    /**
     * Handle race entry button activation.
     *
     * @param raceType the race to enter
     * @return the entry result
     */
    public PigeonRacingSystem.EntryResult activateRaceEntry(PigeonRacingSystem system,
                                                             RaceType raceType,
                                                             Inventory inventory,
                                                             ragamuffin.core.NotorietySystem.AchievementCallback callback) {
        return system.enterRace(raceType, inventory, callback);
    }

    /**
     * Handle the "Release" button activation.
     */
    public void activateRelease(PigeonRacingSystem system) {
        system.releasePigeon();
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Render the pigeon loft panel.
     *
     * @param batch        sprite batch (orthographic projection set)
     * @param shape        shape renderer
     * @param font         bitmap font
     * @param screenWidth  screen width
     * @param screenHeight screen height
     * @param system       the pigeon racing system
     * @param inventory    player inventory (for button enabled states)
     */
    public void render(SpriteBatch batch, ShapeRenderer shape, BitmapFont font,
                       float screenWidth, float screenHeight,
                       PigeonRacingSystem system, Inventory inventory) {
        if (!visible) return;

        float panelX = (screenWidth - PANEL_WIDTH) / 2f;
        float panelY = (screenHeight - PANEL_HEIGHT) / 2f;

        // ── Background ────────────────────────────────────────────────────────
        batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(PANEL_BG);
        shape.rect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(PANEL_BORDER);
        shape.rect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        shape.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();

        float x = panelX + PADDING;
        float y = panelY + PANEL_HEIGHT - PADDING;

        // ── Title ─────────────────────────────────────────────────────────────
        font.getData().setScale(TITLE_SCALE);
        font.setColor(TITLE_COLOUR);
        font.draw(batch, "PIGEON LOFT", x, y);
        y -= ROW_HEIGHT;

        Pigeon pigeon = system.getPlayerPigeon();

        if (pigeon == null) {
            // No pigeon
            font.getData().setScale(BODY_SCALE);
            font.setColor(DISABLED_COLOUR);
            font.draw(batch, "No pigeon. Buy one from the Pigeon Fancier", x, y);
            y -= ROW_HEIGHT;
            font.draw(batch, "or catch a wild bird in the park (press E).", x, y);
        } else {
            // ── Pigeon Stats ──────────────────────────────────────────────────
            font.getData().setScale(SECTION_SCALE);
            font.setColor(SECTION_COLOUR);
            font.draw(batch, "PIGEON", x, y);
            y -= ROW_HEIGHT;

            font.getData().setScale(BODY_SCALE);
            font.setColor(BODY_COLOUR);
            font.draw(batch, "Name: " + pigeon.getName(), x, y);
            y -= ROW_HEIGHT;

            // Training bar
            font.draw(batch, "Training: " + pigeon.getTrainingLevel() + "/" + PigeonRacingSystem.MAX_TRAINING_LEVEL, x, y);
            y -= ROW_HEIGHT - 4f;

            // Draw training bar (rendered in shape pass — skip here for simplicity,
            // indicate via text)
            // Wins/Races
            font.draw(batch, "Record: " + pigeon.getWins() + "W / " + pigeon.getRaces() + " races", x, y);
            y -= ROW_HEIGHT;

            // Morale (coloured)
            font.draw(batch, "Morale: ", x, y);
            font.setColor(moraleColour(pigeon.getMorale()));
            font.draw(batch, pigeon.getMorale().name(), x + 70f, y);
            font.setColor(BODY_COLOUR);
            y -= ROW_HEIGHT;

            // Loft condition
            font.setColor(loftConditionColour(system.getLoftCondition()));
            font.draw(batch, "Loft condition: " + system.getLoftCondition() + "/100", x, y);
            font.setColor(BODY_COLOUR);
            y -= SECTION_GAP + ROW_HEIGHT;

            // ── Train ─────────────────────────────────────────────────────────
            font.getData().setScale(SECTION_SCALE);
            font.setColor(SECTION_COLOUR);
            font.draw(batch, "TRAIN", x, y);
            y -= ROW_HEIGHT;

            boolean canFeed = inventory.getItemCount(Material.BREAD_CRUST) > 0
                    && pigeon.getTrainingLevel() < PigeonRacingSystem.MAX_TRAINING_LEVEL;
            renderButton(batch, font, x, y, "[0] Feed Bread Crust", 0, canFeed);
            y -= ROW_HEIGHT + SECTION_GAP;

            // ── Race Entry ────────────────────────────────────────────────────
            font.getData().setScale(SECTION_SCALE);
            font.setColor(SECTION_COLOUR);
            font.draw(batch, "ENTER RACE", x, y);
            y -= ROW_HEIGHT;

            boolean hasLoft = system.hasLoft();

            // Neighbourhood Sprint
            boolean sprintOk = hasLoft && inventory.getItemCount(Material.COIN) >= RaceType.NEIGHBOURHOOD_SPRINT.getEntryFee();
            renderButton(batch, font, x, y, "[1] Neighbourhood Sprint (free)", 1, sprintOk);
            y -= ROW_HEIGHT;

            // Club Race
            boolean clubOk = hasLoft && inventory.getItemCount(Material.COIN) >= RaceType.CLUB_RACE.getEntryFee();
            renderButton(batch, font, x, y, "[2] Club Race (2 coin)", 2, clubOk);
            y -= ROW_HEIGHT;

            // Derby
            boolean derbyOk = hasLoft && system.hasWonClubRace()
                    && inventory.getItemCount(Material.COIN) >= RaceType.NORTHFIELD_DERBY.getEntryFee();
            String derbyLabel = system.hasWonClubRace()
                    ? "[3] Northfield Derby (5 coin)"
                    : "[3] Northfield Derby (requires Club win)";
            renderButton(batch, font, x, y, derbyLabel, 3, derbyOk);
            y -= ROW_HEIGHT + SECTION_GAP;

            // ── Last Result ───────────────────────────────────────────────────
            RaceResult lastResult = system.getLastResult();
            if (lastResult != null) {
                font.getData().setScale(SECTION_SCALE);
                font.setColor(SECTION_COLOUR);
                font.draw(batch, "LAST RESULT", x, y);
                y -= ROW_HEIGHT;

                font.getData().setScale(BODY_SCALE);
                if (lastResult.isPostponed()) {
                    font.setColor(DISABLED_COLOUR);
                    font.draw(batch, "Race postponed due to weather.", x, y);
                } else {
                    Color resultColour = lastResult.isWin() ? MORALE_GOOD : BODY_COLOUR;
                    font.setColor(resultColour);
                    font.draw(batch, "Place: " + lastResult.getPlacement()
                            + "/" + lastResult.getTotalEntrants(), x, y);
                    y -= ROW_HEIGHT;
                    font.setColor(BODY_COLOUR);
                    // Wrap long flavour text
                    String flavour = lastResult.getFlavourLine();
                    font.draw(batch, flavour, x, y);
                }
                y -= ROW_HEIGHT + SECTION_GAP;
            }

            // ── Release ───────────────────────────────────────────────────────
            renderButton(batch, font, x, y, "[4] Release pigeon", 4, true);
        }

        // ── Controls hint ─────────────────────────────────────────────────────
        font.getData().setScale(HINT_SCALE);
        font.setColor(HINT_COLOUR);
        font.draw(batch, "ESC / E — close", x, panelY + PADDING + ROW_HEIGHT * 0.5f);

        // Reset font
        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void renderButton(SpriteBatch batch, BitmapFont font,
                              float x, float y, String label, int buttonIndex, boolean enabled) {
        font.getData().setScale(BODY_SCALE);
        if (!enabled) {
            font.setColor(DISABLED_COLOUR);
        } else if (selectedButton == buttonIndex) {
            font.setColor(TITLE_COLOUR); // highlight selected
        } else {
            font.setColor(BODY_COLOUR);
        }
        font.draw(batch, label, x, y);
    }

    private Color moraleColour(Morale morale) {
        switch (morale) {
            case ELATED:
            case CONFIDENT:
                return MORALE_GOOD;
            case STEADY:
                return MORALE_MID;
            case NERVOUS:
            case MISERABLE:
            default:
                return MORALE_BAD;
        }
    }

    private Color loftConditionColour(int condition) {
        if (condition >= 70) return MORALE_GOOD;
        if (condition >= 30) return MORALE_MID;
        return MORALE_BAD;
    }

    /**
     * Returns the tooltip text for the release button.
     */
    public static String getReleaseTooltip() {
        return "Off you go then, love.";
    }

    /**
     * Returns the tooltip text shown when racing without a loft.
     */
    public static String getNoLoftTooltip() {
        return "You can't race a pigeon that lives in your pocket.";
    }
}

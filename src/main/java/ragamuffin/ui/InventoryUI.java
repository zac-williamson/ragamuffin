package ragamuffin.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;

/**
 * Renders the inventory UI overlay.
 */
public class InventoryUI {
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 4;
    private static final int SLOT_SIZE = 50;
    private static final int SLOT_PADDING = 5;

    private final Inventory inventory;
    private boolean visible;

    public InventoryUI(Inventory inventory) {
        this.inventory = inventory;
        this.visible = false;
    }

    /**
     * Toggle visibility of the inventory UI.
     */
    public void toggle() {
        visible = !visible;
    }

    /**
     * Show the inventory UI.
     */
    public void show() {
        visible = true;
    }

    /**
     * Hide the inventory UI.
     */
    public void hide() {
        visible = false;
    }

    /**
     * Check if the inventory UI is currently visible.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Get the underlying inventory.
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Render the inventory UI.
     */
    public void render(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font, int screenWidth, int screenHeight) {
        if (!visible) {
            return;
        }

        // Calculate starting position (center of screen)
        int gridWidth = GRID_COLS * (SLOT_SIZE + SLOT_PADDING);
        int gridHeight = GRID_ROWS * (SLOT_SIZE + SLOT_PADDING);
        int startX = (screenWidth - gridWidth) / 2;
        int startY = (screenHeight - gridHeight) / 2;

        // Render background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.9f);
        shapeRenderer.rect(startX - 10, startY - 10, gridWidth + 20, gridHeight + 20);
        shapeRenderer.end();

        // Render slots
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.6f, 0.6f, 0.6f, 1f);

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int x = startX + col * (SLOT_SIZE + SLOT_PADDING);
                int y = startY + row * (SLOT_SIZE + SLOT_PADDING);
                shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);
            }
        }
        shapeRenderer.end();

        // Render item text
        batch.begin();
        for (int slot = 0; slot < Math.min(inventory.getSize(), GRID_COLS * GRID_ROWS); slot++) {
            Material material = inventory.getItemInSlot(slot);
            if (material != null) {
                int count = inventory.getCountInSlot(slot);
                int col = slot % GRID_COLS;
                int row = slot / GRID_COLS;
                int x = startX + col * (SLOT_SIZE + SLOT_PADDING) + 5;
                int y = startY + row * (SLOT_SIZE + SLOT_PADDING) + SLOT_SIZE - 10;

                // Draw material name (abbreviated)
                String name = getMaterialAbbreviation(material);
                font.draw(batch, name, x, y);

                // Draw count
                font.draw(batch, String.valueOf(count), x + 30, y - 15);
            }
        }
        batch.end();
    }

    /**
     * Get abbreviated material name for UI display.
     */
    private String getMaterialAbbreviation(Material material) {
        switch (material) {
            case WOOD: return "WD";
            case BRICK: return "BR";
            case GLASS: return "GL";
            case STONE: return "ST";
            case DIAMOND: return "DI";
            case COMPUTER: return "PC";
            case OFFICE_CHAIR: return "CH";
            case STAPLER: return "SP";
            case GRASS_TURF: return "GR";
            case DIRT: return "DT";
            case PAVEMENT_SLAB: return "PV";
            case ROAD_ASPHALT: return "RD";
            default: return "??";
        }
    }
}

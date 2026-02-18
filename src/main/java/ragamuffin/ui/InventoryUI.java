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
        render(batch, shapeRenderer, font, screenWidth, screenHeight, null);
    }

    /**
     * Render the inventory UI and register hover tooltip zones.
     */
    public void render(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font, int screenWidth, int screenHeight, HoverTooltipSystem hoverTooltips) {
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

        // Render item text and register tooltip zones
        batch.begin();
        for (int slot = 0; slot < Math.min(inventory.getSize(), GRID_COLS * GRID_ROWS); slot++) {
            int col = slot % GRID_COLS;
            int row = slot / GRID_COLS;
            int slotX = startX + col * (SLOT_SIZE + SLOT_PADDING);
            int slotY = startY + row * (SLOT_SIZE + SLOT_PADDING);
            Material material = inventory.getItemInSlot(slot);
            if (material != null) {
                int count = inventory.getCountInSlot(slot);
                int textX = slotX + 5;
                int textY = slotY + SLOT_SIZE - 10;

                // Draw material name (abbreviated)
                String name = getMaterialAbbreviation(material);
                font.draw(batch, name, textX, textY);

                // Draw count
                font.draw(batch, String.valueOf(count), textX + 30, textY - 15);

                // Register tooltip zone
                if (hoverTooltips != null) {
                    String tooltip = material.getDisplayName() + " x" + count;
                    hoverTooltips.addZone(slotX, slotY, SLOT_SIZE, SLOT_SIZE, tooltip);
                }
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
            case PLANKS: return "PL";
            case SHELTER_WALL: return "SW";
            case SHELTER_FLOOR: return "SF";
            case SHELTER_ROOF: return "SR";
            case BRICK_WALL: return "BW";
            case WINDOW: return "WN";
            case SAUSAGE_ROLL: return "SG";
            case STEAK_BAKE: return "SB";
            case CARDBOARD: return "CB";
            case IMPROVISED_TOOL: return "IT";
            case STONE_TOOL: return "TL";
            case CHIPS: return "CP";
            case KEBAB: return "KB";
            case ENERGY_DRINK: return "ED";
            case CRISPS: return "CR";
            case TIN_OF_BEANS: return "TB";
            case CONCRETE: return "CN";
            case ROOF_TILE: return "RT";
            case TARMAC: return "TM";
            case SCRAP_METAL: return "SM";
            case RENDER: return "RN";
            case SLATE: return "SL";
            case PEBBLEDASH: return "PB";
            case DOOR: return "DR";
            case LINOLEUM: return "LN";
            case YELLOW_BRICK: return "YB";
            case TILE: return "TI";
            case COUNTER: return "CT";
            case SHELF: return "SH";
            case TABLE: return "TA";
            case CARPET: return "CA";
            case FENCE: return "FN";
            case SIGN: return "SI";
            case GARDEN_WALL: return "GW";
            case BOOKSHELF: return "BK";
            default: return "??";
        }
    }
}

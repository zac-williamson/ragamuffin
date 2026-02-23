package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;

// Note: item icons are drawn as coloured block graphics using Material.getIconColors()

/**
 * Renders the inventory UI overlay with click and drag-and-drop support.
 */
public class InventoryUI {
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 4;
    private static final int SLOT_SIZE = 50;
    private static final int SLOT_PADDING = 5;

    private final Inventory inventory;
    private boolean visible;

    // Drag-and-drop state
    private int dragSourceSlot = -1;
    private boolean dragging = false;
    private int dragMouseX, dragMouseY;

    // Cached layout positions (updated each render)
    private int gridStartX, gridStartY;

    public InventoryUI(Inventory inventory) {
        this.inventory = inventory;
        this.visible = false;
    }

    public void toggle() {
        visible = !visible;
        if (!visible) {
            cancelDrag();
        }
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
        cancelDrag();
    }

    public boolean isVisible() {
        return visible;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public boolean isDragging() {
        return dragging;
    }

    public int getDragSourceSlot() {
        return dragSourceSlot;
    }

    public Material getDragMaterial() {
        if (dragSourceSlot >= 0) {
            return inventory.getItemInSlot(dragSourceSlot);
        }
        return null;
    }

    /**
     * Handle mouse click at the given screen coordinates.
     * screenY is in LibGDX screen coordinates (0 = top).
     * Returns true if the click was consumed by the inventory UI.
     */
    public boolean handleClick(int screenX, int screenY, int screenHeight) {
        if (!visible) return false;

        // Convert to UI coordinates (0 = bottom)
        int uiY = screenHeight - screenY;
        int slot = getSlotAt(screenX, uiY);
        if (slot >= 0 && slot < inventory.getSize()) {
            if (inventory.getItemInSlot(slot) != null) {
                dragSourceSlot = slot;
                dragging = true;
                dragMouseX = screenX;
                dragMouseY = screenY;
            }
            return true;
        }
        return false;
    }

    /**
     * Handle mouse release at the given screen coordinates.
     * Returns true if the release was consumed.
     */
    public boolean handleRelease(int screenX, int screenY, int screenHeight) {
        if (!visible || !dragging) return false;

        // Convert to UI coordinates (0 = bottom)
        int uiY = screenHeight - screenY;
        int targetSlot = getSlotAt(screenX, uiY);

        if (targetSlot >= 0 && targetSlot < inventory.getSize() && targetSlot != dragSourceSlot) {
            // Drop onto inventory slot — swap items
            inventory.swapSlots(dragSourceSlot, targetSlot);
        }

        cancelDrag();
        return true;
    }

    /**
     * Handle a drop onto the hotbar.
     * Returns the source slot index if we were dragging, or -1.
     */
    public int getDragSlotForHotbarDrop() {
        if (dragging) {
            int slot = dragSourceSlot;
            cancelDrag();
            return slot;
        }
        return -1;
    }

    /**
     * Update drag position for rendering the floating item.
     */
    public void updateDragPosition(int screenX, int screenY) {
        dragMouseX = screenX;
        dragMouseY = screenY;
    }

    private void cancelDrag() {
        dragging = false;
        dragSourceSlot = -1;
    }

    /**
     * Get the inventory slot index at the given UI coordinates (0 = bottom).
     * Returns -1 if not over any slot.
     */
    public int getSlotAt(int x, int y) {
        int relX = x - gridStartX;
        int relY = y - gridStartY;
        if (relX < 0 || relY < 0) return -1;

        int col = relX / (SLOT_SIZE + SLOT_PADDING);
        int row = relY / (SLOT_SIZE + SLOT_PADDING);

        if (col >= GRID_COLS || row >= GRID_ROWS) return -1;

        // Check we're actually inside the slot, not the padding
        int withinSlotX = relX % (SLOT_SIZE + SLOT_PADDING);
        int withinSlotY = relY % (SLOT_SIZE + SLOT_PADDING);
        if (withinSlotX > SLOT_SIZE || withinSlotY > SLOT_SIZE) return -1;

        int slot = row * GRID_COLS + col;
        if (slot >= inventory.getSize()) return -1;
        return slot;
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
        gridStartX = (screenWidth - gridWidth) / 2;
        gridStartY = (screenHeight - gridHeight) / 2;

        // Render background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.9f);
        shapeRenderer.rect(gridStartX - 10, gridStartY - 10, gridWidth + 20, gridHeight + 40);
        shapeRenderer.end();

        // Title
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Inventory (drag to rearrange)", gridStartX, gridStartY + gridHeight + 22);
        batch.end();

        // Render slot backgrounds
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int slot = row * GRID_COLS + col;
                int x = gridStartX + col * (SLOT_SIZE + SLOT_PADDING);
                int y = gridStartY + row * (SLOT_SIZE + SLOT_PADDING);

                if (dragging && slot == dragSourceSlot) {
                    shapeRenderer.setColor(0.4f, 0.4f, 0.1f, 0.8f); // Highlight source
                } else {
                    shapeRenderer.setColor(0.15f, 0.15f, 0.15f, 0.9f);
                }
                shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);
            }
        }
        shapeRenderer.end();

        // Render slot borders
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int x = gridStartX + col * (SLOT_SIZE + SLOT_PADDING);
                int y = gridStartY + row * (SLOT_SIZE + SLOT_PADDING);

                // Highlight hovered slot
                int mouseUiY = screenHeight - dragMouseY;
                int hoveredSlot = getSlotAt(dragMouseX, mouseUiY);
                if (row * GRID_COLS + col == hoveredSlot && !dragging) {
                    shapeRenderer.setColor(1f, 1f, 0f, 1f); // Yellow hover
                } else {
                    shapeRenderer.setColor(0.6f, 0.6f, 0.6f, 1f);
                }
                shapeRenderer.rect(x, y, SLOT_SIZE, SLOT_SIZE);
            }
        }
        shapeRenderer.end();

        // Render item icons (coloured block graphics) and register tooltip zones
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int slot = 0; slot < Math.min(inventory.getSize(), GRID_COLS * GRID_ROWS); slot++) {
            if (dragging && slot == dragSourceSlot) continue;

            int col = slot % GRID_COLS;
            int row = slot / GRID_COLS;
            int slotX = gridStartX + col * (SLOT_SIZE + SLOT_PADDING);
            int slotY = gridStartY + row * (SLOT_SIZE + SLOT_PADDING);
            Material material = inventory.getItemInSlot(slot);
            if (material != null) {
                drawItemIcon(shapeRenderer, material, slotX, slotY, SLOT_SIZE);
            }
        }
        shapeRenderer.end();

        // Render item count badges and register tooltip zones
        batch.begin();
        for (int slot = 0; slot < Math.min(inventory.getSize(), GRID_COLS * GRID_ROWS); slot++) {
            if (dragging && slot == dragSourceSlot) continue;

            int col = slot % GRID_COLS;
            int row = slot / GRID_COLS;
            int slotX = gridStartX + col * (SLOT_SIZE + SLOT_PADDING);
            int slotY = gridStartY + row * (SLOT_SIZE + SLOT_PADDING);
            Material material = inventory.getItemInSlot(slot);
            if (material != null) {
                int count = inventory.getCountInSlot(slot);
                font.setColor(Color.WHITE);
                font.draw(batch, String.valueOf(count), slotX + 3, slotY + 14);

                if (hoverTooltips != null) {
                    String tooltip = material.getDisplayName() + " x" + count;
                    hoverTooltips.addZone(slotX, slotY, SLOT_SIZE, SLOT_SIZE, tooltip);
                }
            }
        }
        batch.end();

        // Render dragged item at cursor position
        if (dragging && dragSourceSlot >= 0) {
            Material dragMat = inventory.getItemInSlot(dragSourceSlot);
            if (dragMat != null) {
                int count = inventory.getCountInSlot(dragSourceSlot);
                // Convert screen coords to UI coords
                int cursorUiX = dragMouseX - SLOT_SIZE / 2;
                int cursorUiY = (screenHeight - dragMouseY) - SLOT_SIZE / 2;

                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                drawItemIcon(shapeRenderer, dragMat, cursorUiX, cursorUiY, SLOT_SIZE);
                shapeRenderer.end();

                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(1f, 1f, 0f, 1f);
                shapeRenderer.rect(cursorUiX, cursorUiY, SLOT_SIZE, SLOT_SIZE);
                shapeRenderer.end();

                batch.begin();
                font.setColor(Color.WHITE);
                font.draw(batch, String.valueOf(count), cursorUiX + 3, cursorUiY + 14);
                batch.end();
            }
        }
    }

    /**
     * Draw a block-style icon for the given material inside the slot rectangle.
     * Must be called while a Filled ShapeRenderer is active.
     * For single-color materials the whole icon is filled.
     * For two-color materials the top half uses color[0] (representing the top face)
     * and the bottom half uses color[1] (representing the side face), mimicking how
     * blocks look when viewed from a slight angle.
     */
    private void drawItemIcon(ShapeRenderer shapeRenderer, Material material, int x, int y, int size) {
        int padding = 4;
        int iconX = x + padding;
        int iconY = y + padding;
        int iconSize = size - padding * 2;

        Color[] colors = material.getIconColors();
        if (colors.length == 1) {
            shapeRenderer.setColor(colors[0]);
            shapeRenderer.rect(iconX, iconY, iconSize, iconSize);
        } else {
            // Two-color: top half = colors[0] (top face), bottom half = colors[1] (side face)
            int half = iconSize / 2;
            shapeRenderer.setColor(colors[1]);
            shapeRenderer.rect(iconX, iconY, iconSize, half);
            shapeRenderer.setColor(colors[0]);
            shapeRenderer.rect(iconX, iconY + half, iconSize, iconSize - half);
        }
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
            case RENDER_CREAM: return "RC";
            case RENDER_PINK: return "RP";
            case SLATE: return "SL";
            case PEBBLEDASH: return "PB";
            case DOOR: return "DR";
            case LINOLEUM: return "LN";
            case LINO_GREEN: return "LG";
            case YELLOW_BRICK: return "YB";
            case TILE: return "TI";
            case TILE_BLACK: return "BT";
            case COUNTER: return "CT";
            case SHELF: return "SH";
            case TABLE: return "TA";
            case CARPET: return "CA";
            case FENCE: return "FN";
            case SIGN: return "SI";
            case SIGN_RED: return "rS";
            case SIGN_BLUE: return "bS";
            case SIGN_GREEN: return "gS";
            case SIGN_YELLOW: return "SY";
            case GARDEN_WALL: return "GW";
            case BOOKSHELF: return "BK";
            case METAL_RED: return "MR";
            default: return "??";
        }
    }
}

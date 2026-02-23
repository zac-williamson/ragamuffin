package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.building.Tool;

/**
 * Renders the hotbar at the bottom of the screen.
 * Shows the first 9 inventory slots and the selected slot.
 */
public class HotbarUI {
    public static final int HOTBAR_SLOTS = 9;
    private static final int SLOT_SIZE = 50;
    private static final int SLOT_PADDING = 5;

    private final Inventory inventory;
    private int selectedSlot;

    // Cached layout for click detection
    private int barStartX, barStartY;

    public HotbarUI(Inventory inventory) {
        this.inventory = inventory;
        this.selectedSlot = 0;
    }

    /**
     * Select a hotbar slot (0-8).
     */
    public void selectSlot(int slot) {
        this.selectedSlot = Math.max(0, Math.min(HOTBAR_SLOTS - 1, slot));
    }

    /**
     * Get the currently selected hotbar slot.
     */
    public int getSelectedSlot() {
        return selectedSlot;
    }

    /**
     * Get the material in the currently selected slot.
     * @return the material, or null if the slot is empty
     */
    public Material getSelectedItem() {
        return inventory.getItemInSlot(selectedSlot);
    }

    /**
     * Get the underlying inventory.
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Get the hotbar slot at the given UI coordinates (0 = bottom).
     * Returns -1 if not over any slot.
     */
    public int getSlotAt(int x, int y) {
        int relX = x - barStartX;
        int relY = y - barStartY;
        if (relX < 0 || relY < 0 || relY > SLOT_SIZE) return -1;

        int slot = relX / (SLOT_SIZE + SLOT_PADDING);
        if (slot >= HOTBAR_SLOTS) return -1;

        int withinSlotX = relX % (SLOT_SIZE + SLOT_PADDING);
        if (withinSlotX > SLOT_SIZE) return -1;

        return slot;
    }

    /**
     * Render the hotbar UI.
     */
    public void render(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font, int screenWidth, int screenHeight) {
        render(batch, shapeRenderer, font, screenWidth, screenHeight, null);
    }

    /**
     * Render the hotbar UI and register hover tooltip zones.
     */
    public void render(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font, int screenWidth, int screenHeight, HoverTooltipSystem hoverTooltips) {
        int hotbarWidth = HOTBAR_SLOTS * (SLOT_SIZE + SLOT_PADDING);
        int startX = (screenWidth - hotbarWidth) / 2;
        int startY = 20; // Bottom of screen
        barStartX = startX;
        barStartY = startY;

        // Render hotbar background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.7f);
        shapeRenderer.rect(startX - 5, startY - 5, hotbarWidth + 10, SLOT_SIZE + 10);
        shapeRenderer.end();

        // Render slots
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int x = startX + i * (SLOT_SIZE + SLOT_PADDING);

            // Highlight selected slot
            if (i == selectedSlot) {
                shapeRenderer.setColor(1f, 1f, 0f, 1f); // Yellow
            } else {
                shapeRenderer.setColor(0.6f, 0.6f, 0.6f, 1f); // Gray
            }

            shapeRenderer.rect(x, startY, SLOT_SIZE, SLOT_SIZE);
        }
        shapeRenderer.end();

        // Render item icons (coloured block graphics)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int x = startX + i * (SLOT_SIZE + SLOT_PADDING);
            Material material = inventory.getItemInSlot(i);
            if (material != null) {
                drawItemIcon(shapeRenderer, material, x, startY, SLOT_SIZE);
            }
        }
        shapeRenderer.end();

        // Render item count badges and register tooltip zones
        batch.begin();
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int x = startX + i * (SLOT_SIZE + SLOT_PADDING);
            Material material = inventory.getItemInSlot(i);
            if (material != null) {
                int count = inventory.getCountInSlot(i);
                font.setColor(com.badlogic.gdx.graphics.Color.WHITE);
                font.draw(batch, String.valueOf(count), x + 3, startY + 14);

                // Register tooltip zone
                if (hoverTooltips != null) {
                    Tool tool = inventory.getToolInSlot(i);
                    String tooltip;
                    if (tool != null) {
                        tooltip = material.getDisplayName() + " (" + tool.getDurability() + "/" + tool.getMaxDurability() + ")";
                    } else {
                        tooltip = material.getDisplayName() + " x" + count;
                    }
                    hoverTooltips.addZone(x, startY, SLOT_SIZE, SLOT_SIZE, tooltip);
                }
            } else if (hoverTooltips != null) {
                hoverTooltips.addZone(x, startY, SLOT_SIZE, SLOT_SIZE, "Empty slot " + (i + 1));
            }
        }
        batch.end();

        // Render tool durability bars (drawn after text so they're visible)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            Tool tool = inventory.getToolInSlot(i);
            if (tool != null && !tool.isBroken()) {
                int x = startX + i * (SLOT_SIZE + SLOT_PADDING);
                int barY = startY + 2;
                int barWidth = SLOT_SIZE - 4;
                int barHeight = 4;

                // Background (dark)
                shapeRenderer.setColor(0.15f, 0.15f, 0.15f, 1f);
                shapeRenderer.rect(x + 2, barY, barWidth, barHeight);

                // Durability fill: green > yellow > red
                float ratio = (float) tool.getDurability() / tool.getMaxDurability();
                if (ratio > 0.5f) {
                    shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 1f); // Green
                } else if (ratio > 0.25f) {
                    shapeRenderer.setColor(0.9f, 0.8f, 0.1f, 1f); // Yellow
                } else {
                    shapeRenderer.setColor(0.9f, 0.2f, 0.1f, 1f); // Red
                }
                shapeRenderer.rect(x + 2, barY, barWidth * ratio, barHeight);
            }
        }
        shapeRenderer.end();
    }

    /**
     * Draw an icon for the given material inside the slot rectangle.
     * Must be called while a Filled ShapeRenderer is active.
     *
     * Block items are drawn as coloured rectangles. Non-block items use custom shapes.
     */
    private void drawItemIcon(ShapeRenderer shapeRenderer, Material material, int x, int y, int size) {
        int padding = 4;
        int iconX = x + padding;
        int iconY = y + padding;
        int iconSize = size - padding * 2;

        Color[] colors = material.getIconColors();

        if (material.isBlockItem()) {
            if (colors.length == 1) {
                shapeRenderer.setColor(colors[0]);
                shapeRenderer.rect(iconX, iconY, iconSize, iconSize);
            } else {
                int half = iconSize / 2;
                shapeRenderer.setColor(colors[1]);
                shapeRenderer.rect(iconX, iconY, iconSize, half);
                shapeRenderer.setColor(colors[0]);
                shapeRenderer.rect(iconX, iconY + half, iconSize, iconSize - half);
            }
        } else {
            drawNonBlockIcon(shapeRenderer, material, iconX, iconY, iconSize, colors);
        }
    }

    /**
     * Draw a custom shaped icon for a non-block item.
     */
    private void drawNonBlockIcon(ShapeRenderer shapeRenderer, Material material, int x, int y, int size, Color[] colors) {
        Color primary = colors[0];
        Color secondary = colors.length > 1 ? colors[1] : primary;
        int cx = x + size / 2;
        int cy = y + size / 2;

        switch (material.getIconShape()) {
            case TOOL: {
                int handleW = size / 5;
                int handleH = (int)(size * 0.65f);
                shapeRenderer.setColor(secondary);
                shapeRenderer.rect(x + size / 5, y + size / 8, handleW, handleH);
                int headSize = size / 3;
                shapeRenderer.setColor(primary);
                shapeRenderer.rect(x + size / 2, y + size / 2, headSize, headSize);
                break;
            }
            case FLAT_PAPER: {
                int w = (int)(size * 0.75f);
                int h = (int)(size * 0.80f);
                int px = x + (size - w) / 2;
                int py = y + (size - h) / 2;
                shapeRenderer.setColor(primary);
                shapeRenderer.rect(px, py, w, h);
                shapeRenderer.setColor(secondary);
                shapeRenderer.rect(px + 2, py + h - h / 5, w - 4, h / 5 - 1);
                break;
            }
            case BOTTLE: {
                int bodyW = size / 3;
                int bodyH = (int)(size * 0.70f);
                int bx = cx - bodyW / 2;
                int by = y + size / 8;
                shapeRenderer.setColor(primary);
                shapeRenderer.rect(bx, by, bodyW, bodyH);
                int capW = bodyW - 4;
                shapeRenderer.setColor(secondary);
                shapeRenderer.rect(bx + 2, by + bodyH, capW, size / 8);
                break;
            }
            case FOOD: {
                int foodW = (int)(size * 0.80f);
                int foodH = (int)(size * 0.50f);
                int fx = x + (size - foodW) / 2;
                int fy = cy - foodH / 2 + size / 10;
                shapeRenderer.setColor(primary);
                shapeRenderer.rect(fx, fy, foodW, foodH);
                shapeRenderer.setColor(secondary);
                shapeRenderer.rect(fx + 2, y + size / 8, foodW - 4, size / 8);
                break;
            }
            case CARD: {
                int cardW = (int)(size * 0.80f);
                int cardH = (int)(size * 0.55f);
                int kx = x + (size - cardW) / 2;
                int ky = cy - cardH / 2;
                shapeRenderer.setColor(primary);
                shapeRenderer.rect(kx, ky, cardW, cardH);
                shapeRenderer.setColor(secondary);
                shapeRenderer.rect(kx + 3, ky + 3, cardW - 6, cardH - 6);
                break;
            }
            case GEM: {
                shapeRenderer.setColor(primary);
                shapeRenderer.triangle(
                    cx, y + size - 2,
                    x + 2, cy,
                    x + size - 2, cy
                );
                shapeRenderer.setColor(secondary);
                shapeRenderer.triangle(
                    x + 2, cy,
                    x + size - 2, cy,
                    cx, y + 2
                );
                break;
            }
            case BOX: {
                int boxSize = (int)(size * 0.70f);
                int bx = x + (size - boxSize) / 2;
                int by = y + size / 10;
                shapeRenderer.setColor(primary);
                shapeRenderer.rect(bx, by, boxSize, boxSize);
                int topH = size / 6;
                shapeRenderer.setColor(secondary);
                shapeRenderer.rect(bx, by + boxSize, boxSize, topH);
                break;
            }
            case CYLINDER: {
                int cylW = size / 3;
                int cylH = (int)(size * 0.72f);
                int cx2 = x + (size - cylW) / 2;
                int cy2 = y + size / 10;
                shapeRenderer.setColor(primary);
                shapeRenderer.rect(cx2, cy2, cylW, cylH);
                shapeRenderer.setColor(secondary);
                shapeRenderer.rect(cx2 - 2, cy2 + cylH - size / 10, cylW + 4, size / 8);
                shapeRenderer.rect(cx2 + cylW, cy2 + cylH / 2, size / 6, size / 8);
                break;
            }
            default: {
                shapeRenderer.setColor(primary);
                shapeRenderer.rect(x, y, size, size);
                break;
            }
        }
    }

    /**
     * Get abbreviated material name for UI display.
     */
    public String getMaterialAbbreviation(Material material) {
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

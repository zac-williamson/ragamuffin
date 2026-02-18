package ragamuffin.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;

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

        // Render item text and register tooltip zones
        batch.begin();
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int x = startX + i * (SLOT_SIZE + SLOT_PADDING);
            Material material = inventory.getItemInSlot(i);
            if (material != null) {
                int count = inventory.getCountInSlot(i);
                int textX = x + 5;
                int textY = startY + SLOT_SIZE - 10;

                // Draw material abbreviation
                String name = getMaterialAbbreviation(material);
                font.draw(batch, name, textX, textY);

                // Draw count
                font.draw(batch, String.valueOf(count), textX + 30, textY - 15);

                // Register tooltip zone
                if (hoverTooltips != null) {
                    String tooltip = material.getDisplayName() + " x" + count;
                    hoverTooltips.addZone(x, startY, SLOT_SIZE, SLOT_SIZE, tooltip);
                }
            } else if (hoverTooltips != null) {
                hoverTooltips.addZone(x, startY, SLOT_SIZE, SLOT_SIZE, "Empty slot " + (i + 1));
            }
        }
        batch.end();
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
            default: return "??";
        }
    }
}

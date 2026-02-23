package ragamuffin.world;

import com.badlogic.gdx.graphics.Color;

/**
 * Represents a named sign above a building entrance.
 *
 * The sign occupies a 2x3 block area in the world and displays the store name.
 * The sign position is the centre of the sign face (front of the building, above the door).
 * Signs are rendered as world-projected overlays so text is always legible.
 */
public class BuildingSign {

    private final String text;
    /** World-space X of the sign centre (horizontally centred over the entrance). */
    private final float worldX;
    /** World-space Y of the sign bottom edge (top of the building wall). */
    private final float worldY;
    /** World-space Z of the sign face (front wall of the building). */
    private final float worldZ;
    /** Background colour for the sign panel. */
    private final Color backgroundColor;
    /** Text colour. */
    private final Color textColor;

    public BuildingSign(String text, float worldX, float worldY, float worldZ,
                        Color backgroundColor, Color textColor) {
        this.text = text;
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
    }

    public String getText() {
        return text;
    }

    public float getWorldX() {
        return worldX;
    }

    public float getWorldY() {
        return worldY;
    }

    public float getWorldZ() {
        return worldZ;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getTextColor() {
        return textColor;
    }
}

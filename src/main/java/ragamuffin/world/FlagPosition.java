package ragamuffin.world;

/**
 * Describes a single animated flag in the world.
 *
 * Each flag has:
 * <ul>
 *   <li>A world-space position at the top of its pole (the flag attachment point).</li>
 *   <li>Two colours ({@code color1} and {@code color2}) blended horizontally across
 *       the flag panel to give it a simple stripe or gradient effect.</li>
 *   <li>A phase offset so that flags in different locations wave out of sync,
 *       preventing all flags from rippling in perfect unison.</li>
 * </ul>
 *
 * Instances are created by {@link WorldGenerator#generateFlagPole} and stored in
 * {@link World#getFlagPositions()}.
 */
public class FlagPosition {

    private final float worldX;
    private final float worldY;
    private final float worldZ;

    // First (hoist-side) colour
    private final float colorR1;
    private final float colorG1;
    private final float colorB1;

    // Second (fly-side) colour
    private final float colorR2;
    private final float colorG2;
    private final float colorB2;

    /** Per-flag phase offset (radians) to desynchronise waving animations. */
    private final float phaseOffset;

    /**
     * Create a flag position with two colours and a phase offset.
     *
     * @param worldX     world-space X of the top of the pole
     * @param worldY     world-space Y of the top of the pole
     * @param worldZ     world-space Z of the top of the pole
     * @param r1         red component of hoist-side colour (0–1)
     * @param g1         green component of hoist-side colour (0–1)
     * @param b1         blue component of hoist-side colour (0–1)
     * @param r2         red component of fly-side colour (0–1)
     * @param g2         green component of fly-side colour (0–1)
     * @param b2         blue component of fly-side colour (0–1)
     * @param phaseOffset phase offset in radians (use 0 for default)
     */
    public FlagPosition(float worldX, float worldY, float worldZ,
                        float r1, float g1, float b1,
                        float r2, float g2, float b2,
                        float phaseOffset) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.colorR1 = r1;
        this.colorG1 = g1;
        this.colorB1 = b1;
        this.colorR2 = r2;
        this.colorG2 = g2;
        this.colorB2 = b2;
        this.phaseOffset = phaseOffset;
    }

    public float getWorldX()      { return worldX; }
    public float getWorldY()      { return worldY; }
    public float getWorldZ()      { return worldZ; }

    public float getColorR1()     { return colorR1; }
    public float getColorG1()     { return colorG1; }
    public float getColorB1()     { return colorB1; }

    public float getColorR2()     { return colorR2; }
    public float getColorG2()     { return colorG2; }
    public float getColorB2()     { return colorB2; }

    public float getPhaseOffset() { return phaseOffset; }
}

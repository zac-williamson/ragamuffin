package ragamuffin.core;

import com.badlogic.gdx.graphics.Color;

/**
 * The three factions competing for turf control in Ragamuffin (Phase 8d / Issue #702).
 *
 * <ul>
 *   <li><b>MARCHETTI_CREW</b> – Organised crime; holds the industrial estate and off-licence.</li>
 *   <li><b>STREET_LADS</b>   – Chaotic youths; hold the park and housing estate.</li>
 *   <li><b>THE_COUNCIL</b>   – Bureaucratic authority; hold the town hall and office block.</li>
 * </ul>
 */
public enum Faction {

    MARCHETTI_CREW(
            "Marchetti Crew",
            "The Marchetti boys",
            new Color(0.85f, 0.20f, 0.10f, 1f),  // Blood red
            "Industrial estate + off-licence"
    ),

    STREET_LADS(
            "Street Lads",
            "The Street Lads",
            new Color(0.10f, 0.60f, 0.20f, 1f),  // Gang green
            "Park + housing estate"
    ),

    THE_COUNCIL(
            "The Council",
            "The Council",
            new Color(0.20f, 0.30f, 0.80f, 1f),  // Bureaucratic blue
            "Town hall + office block"
    );

    private final String displayName;
    private final String rumorName;   // Used in rumour text fragments
    private final Color hudColor;     // Colour of this faction's Respect bar in the HUD
    private final String territory;

    Faction(String displayName, String rumorName, Color hudColor, String territory) {
        this.displayName = displayName;
        this.rumorName   = rumorName;
        this.hudColor    = hudColor;
        this.territory   = territory;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRumorName() {
        return rumorName;
    }

    public Color getHudColor() {
        return hudColor;
    }

    public String getTerritory() {
        return territory;
    }

    /**
     * Return the other two factions (rivals).
     */
    public Faction[] rivals() {
        Faction[] all = values();
        Faction[] rivals = new Faction[2];
        int idx = 0;
        for (Faction f : all) {
            if (f != this) {
                rivals[idx++] = f;
            }
        }
        return rivals;
    }

    /** Numeric index (0, 1, 2) matching {@link #values()} order. */
    public int index() {
        return ordinal();
    }
}

package ragamuffin.entity;

/**
 * Reasons why the player takes damage. Used to display feedback on screen.
 */
public enum DamageReason {
    FALL("Fall damage"),
    NPC_ATTACK("Attacked"),
    STARVATION("Starving"),
    WEATHER("Exposure"),
    HYPOTHERMIA("Hypothermia"),
    CAR_HIT("Hit by car"),
    UNKNOWN("Damaged");

    private final String displayName;

    DamageReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

package ragamuffin.core;

/**
 * Tracks the player's street reputation.
 *
 * Reputation affects how NPCs react to the player:
 *   - Breaking blocks, fighting, and committing crimes increases your reputation
 *   - Higher reputation makes police more aggressive
 *   - Higher reputation makes civilians more scared
 *   - Getting arrested reduces your reputation
 *
 * This system adds depth to the ragamuffin fantasy: you build a reputation
 * as a hardened street dweller, and the world responds accordingly.
 */
public class StreetReputation {

    /** Points needed to reach KNOWN status. */
    public static final int KNOWN_THRESHOLD = 10;

    /** Points needed to reach NOTORIOUS status. */
    public static final int NOTORIOUS_THRESHOLD = 30;

    /** Reputation levels — how the streets see you. */
    public enum ReputationLevel {
        NOBODY,      // Just another dosser, no one cares
        KNOWN,       // Word's getting around about you
        NOTORIOUS    // Everyone knows your name (coppers especially)
    }

    private int points;
    private ReputationLevel level;

    public StreetReputation() {
        this.points = 0;
        this.level = ReputationLevel.NOBODY;
    }

    /**
     * Add reputation points (for crimes, fights, surviving police encounters).
     *
     * @param amount  how many points to add (typically 1-5 per event)
     */
    public void addPoints(int amount) {
        points += amount;
        updateLevel();
    }

    /**
     * Remove reputation points (for getting arrested, dying, or lying low).
     *
     * @param amount  how many points to subtract
     */
    public void removePoints(int amount) {
        points = Math.max(0, points - amount);
        updateLevel();
    }

    /**
     * Recalculate the reputation level based on current points.
     */
    private void updateLevel() {
        if (points >= NOTORIOUS_THRESHOLD) {
            level = ReputationLevel.NOTORIOUS;
        } else if (points >= KNOWN_THRESHOLD) {
            level = ReputationLevel.KNOWN;
        } else {
            level = ReputationLevel.NOBODY;
        }
    }

    /**
     * Get the current reputation level.
     */
    public ReputationLevel getLevel() {
        return level;
    }

    /**
     * Get the current reputation points.
     */
    public int getPoints() {
        return points;
    }

    /**
     * Whether the player is known on the streets (KNOWN or higher).
     */
    public boolean isKnown() {
        return level != ReputationLevel.NOBODY;
    }

    /**
     * Whether the player is notorious (NOTORIOUS level).
     */
    public boolean isNotorious() {
        return level == ReputationLevel.NOTORIOUS;
    }

    /**
     * Reset reputation to zero — called on arrest or player reset.
     * The streets forget quickly when you're locked up.
     */
    public void reset() {
        points = 0;
        level = ReputationLevel.NOBODY;
    }
}

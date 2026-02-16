package ragamuffin.ui;

/**
 * Enumeration of all tooltip triggers in the game.
 * Each trigger is shown only once (first-time only).
 */
public enum TooltipTrigger {
    FIRST_TREE_PUNCH("Punch a tree to get wood"),
    JEWELLER_DIAMOND("Jewellers can be a good source of diamond");

    private final String message;

    TooltipTrigger(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

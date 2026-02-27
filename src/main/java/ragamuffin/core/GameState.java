package ragamuffin.core;

/**
 * Game state machine states.
 */
public enum GameState {
    LOADING(false),
    MENU(false),
    CINEMATIC(false),
    PLAYING(true),
    PAUSED(false),
    /** Issue #789: Boot sale auction overlay is open. */
    BOOT_SALE_OPEN(true);

    private final boolean acceptsInput;

    GameState(boolean acceptsInput) {
        this.acceptsInput = acceptsInput;
    }

    public boolean acceptsInput() {
        return acceptsInput;
    }
}

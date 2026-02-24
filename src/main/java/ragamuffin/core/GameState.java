package ragamuffin.core;

/**
 * Game state machine states.
 */
public enum GameState {
    LOADING(false),
    MENU(false),
    CINEMATIC(false),
    PLAYING(true),
    PAUSED(false);

    private final boolean acceptsInput;

    GameState(boolean acceptsInput) {
        this.acceptsInput = acceptsInput;
    }

    public boolean acceptsInput() {
        return acceptsInput;
    }
}

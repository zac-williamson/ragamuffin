package ragamuffin.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    @Test
    void allStatesExist() {
        assertNotNull(GameState.MENU);
        assertNotNull(GameState.PLAYING);
        assertNotNull(GameState.PAUSED);
    }

    @Test
    void menuStateDoesNotAcceptInput() {
        assertFalse(GameState.MENU.acceptsInput());
    }

    @Test
    void playingStateAcceptsInput() {
        assertTrue(GameState.PLAYING.acceptsInput());
    }

    @Test
    void pausedStateDoesNotAcceptInput() {
        assertFalse(GameState.PAUSED.acceptsInput());
    }
}
